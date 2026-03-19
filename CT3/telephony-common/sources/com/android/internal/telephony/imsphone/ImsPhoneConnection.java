package com.android.internal.telephony.imsphone;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.Registrant;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.telecom.ConferenceParticipant;
import android.telecom.Log;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.text.TextUtils;
import com.android.ims.ImsCall;
import com.android.ims.ImsCallProfile;
import com.android.ims.ImsException;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.UUSInfo;
import com.mediatek.internal.telephony.ConferenceCallMessageHandler;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ImsPhoneConnection extends Connection {
    private static final boolean DBG = true;
    private static final int EVENT_DTMF_DELAY_DONE = 5;
    private static final int EVENT_DTMF_DONE = 1;
    private static final int EVENT_NEXT_POST_DIAL = 3;
    private static final int EVENT_PAUSE_DONE = 2;
    private static final int EVENT_WAKE_LOCK_TIMEOUT = 4;
    private static final String LOG_TAG = "ImsPhoneConnection";
    private static final int PAUSE_DELAY_MILLIS = 3000;
    private static final int WAKE_LOCK_TIMEOUT_MILLIS = 60000;
    private int mCallIdBeforeDisconnected;
    private ArrayList<String> mConfDialStrings;
    private long mConferenceConnectTime;
    private List<ConferenceParticipant> mConferenceParticipants;
    private long mDisconnectTime;
    private boolean mDisconnected;
    private int mDtmfToneDelay;
    private Bundle mExtras;
    private Handler mHandler;
    private ImsCall mImsCall;
    private boolean mIsEmergency;
    private boolean mIsWifiStateFromExtras;
    private ImsPhoneCallTracker mOwner;
    private ImsPhoneCall mParent;
    private PowerManager.WakeLock mPartialWakeLock;
    private UUSInfo mUusInfo;
    private String mVendorCause;

    class MyHandler extends Handler {
        MyHandler(Looper l) {
            super(l);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    ImsPhoneConnection.this.mHandler.sendMessageDelayed(ImsPhoneConnection.this.mHandler.obtainMessage(5), ImsPhoneConnection.this.mDtmfToneDelay);
                    break;
                case 2:
                case 3:
                case 5:
                    ImsPhoneConnection.this.processNextPostDialChar();
                    break;
                case 4:
                    ImsPhoneConnection.this.releaseWakeLock();
                    break;
            }
        }
    }

    public ImsPhoneConnection(Phone phone, ImsCall imsCall, ImsPhoneCallTracker ct, ImsPhoneCall parent, boolean isUnknown) {
        super(5);
        this.mExtras = new Bundle();
        this.mConferenceConnectTime = 0L;
        this.mDtmfToneDelay = 0;
        this.mIsEmergency = false;
        this.mIsWifiStateFromExtras = false;
        this.mConfDialStrings = null;
        this.mConferenceParticipants = null;
        this.mCallIdBeforeDisconnected = -1;
        this.mVendorCause = null;
        createWakeLock(phone.getContext());
        acquireWakeLock();
        this.mOwner = ct;
        this.mHandler = new MyHandler(this.mOwner.getLooper());
        this.mImsCall = imsCall;
        if (imsCall != null && imsCall.getCallProfile() != null) {
            this.mAddress = imsCall.getCallProfile().getCallExtra("oi");
            this.mCnapName = imsCall.getCallProfile().getCallExtra("cna");
            this.mNumberPresentation = ImsCallProfile.OIRToPresentation(imsCall.getCallProfile().getCallExtraInt("oir"));
            this.mCnapNamePresentation = ImsCallProfile.OIRToPresentation(imsCall.getCallProfile().getCallExtraInt("cnap"));
            updateMediaCapabilities(imsCall);
        } else {
            this.mNumberPresentation = 3;
            this.mCnapNamePresentation = 3;
        }
        this.mIsIncoming = isUnknown ? false : true;
        this.mCreateTime = System.currentTimeMillis();
        this.mUusInfo = null;
        updateWifiState();
        updateExtras(imsCall);
        this.mParent = parent;
        this.mParent.attach(this, this.mIsIncoming ? Call.State.INCOMING : Call.State.DIALING);
        fetchDtmfToneDelay(phone);
    }

    public ImsPhoneConnection(Phone phone, String dialString, ImsPhoneCallTracker ct, ImsPhoneCall parent, boolean isEmergency) {
        super(5);
        this.mExtras = new Bundle();
        this.mConferenceConnectTime = 0L;
        this.mDtmfToneDelay = 0;
        this.mIsEmergency = false;
        this.mIsWifiStateFromExtras = false;
        this.mConfDialStrings = null;
        this.mConferenceParticipants = null;
        this.mCallIdBeforeDisconnected = -1;
        this.mVendorCause = null;
        createWakeLock(phone.getContext());
        acquireWakeLock();
        this.mOwner = ct;
        this.mHandler = new MyHandler(this.mOwner.getLooper());
        this.mDialString = dialString;
        this.mAddress = dialString;
        this.mPostDialString = UsimPBMemInfo.STRING_NOT_SET;
        if (!PhoneNumberUtils.isUriNumber(dialString)) {
            this.mAddress = PhoneNumberUtils.extractNetworkPortionAlt(dialString);
            this.mPostDialString = PhoneNumberUtils.extractPostDialPortion(dialString);
        }
        this.mIsIncoming = false;
        this.mCnapName = null;
        this.mCnapNamePresentation = 1;
        this.mNumberPresentation = 1;
        this.mCreateTime = System.currentTimeMillis();
        this.mParent = parent;
        parent.attachFake(this, Call.State.DIALING);
        this.mIsEmergency = isEmergency;
        fetchDtmfToneDelay(phone);
    }

    public void dispose() {
    }

    static boolean equalsHandlesNulls(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    private static int applyLocalCallCapabilities(ImsCallProfile localProfile, int capabilities) {
        int capabilities2 = removeCapability(capabilities, 5);
        switch (localProfile.mCallType) {
            case 3:
            case 4:
                return addCapability(capabilities2, 5);
            default:
                return capabilities2;
        }
    }

    private static int applyRemoteCallCapabilities(ImsCallProfile remoteProfile, int capabilities) {
        int capabilities2 = removeCapability(capabilities, 10);
        switch (remoteProfile.mCallType) {
            case 3:
            case 4:
                return addCapability(capabilities2, 10);
            default:
                return capabilities2;
        }
    }

    @Override
    public String getOrigDialString() {
        return this.mDialString;
    }

    @Override
    public ImsPhoneCall getCall() {
        return this.mParent;
    }

    @Override
    public long getDisconnectTime() {
        return this.mDisconnectTime;
    }

    @Override
    public long getHoldingStartTime() {
        return this.mHoldingStartTime;
    }

    @Override
    public long getHoldDurationMillis() {
        if (getState() != Call.State.HOLDING) {
            return 0L;
        }
        return SystemClock.elapsedRealtime() - this.mHoldingStartTime;
    }

    public void setDisconnectCause(int cause) {
        this.mCause = cause;
    }

    @Override
    public String getVendorDisconnectCause() {
        return this.mVendorCause;
    }

    public ImsPhoneCallTracker getOwner() {
        return this.mOwner;
    }

    @Override
    public Call.State getState() {
        if (this.mDisconnected) {
            return Call.State.DISCONNECTED;
        }
        return super.getState();
    }

    @Override
    public void hangup() throws CallStateException {
        if (this.mOwner != null) {
            this.mOwner.logDebugMessagesWithOpFormat("CC", "Hangup", this, "ImsphoneConnection.hangup");
        }
        if (!this.mDisconnected) {
            this.mOwner.hangup(this);
            return;
        }
        throw new CallStateException(ConferenceCallMessageHandler.STATUS_DISCONNECTED);
    }

    @Override
    public void separate() throws CallStateException {
        throw new CallStateException("not supported");
    }

    @Override
    public void proceedAfterWaitChar() {
        if (this.mPostDialState != Connection.PostDialState.WAIT) {
            Rlog.w(LOG_TAG, "ImsPhoneConnection.proceedAfterWaitChar(): Expected getPostDialState() to be WAIT but was " + this.mPostDialState);
        } else {
            setPostDialState(Connection.PostDialState.STARTED);
            processNextPostDialChar();
        }
    }

    @Override
    public void proceedAfterWildChar(String str) {
        if (this.mPostDialState != Connection.PostDialState.WILD) {
            Rlog.w(LOG_TAG, "ImsPhoneConnection.proceedAfterWaitChar(): Expected getPostDialState() to be WILD but was " + this.mPostDialState);
            return;
        }
        setPostDialState(Connection.PostDialState.STARTED);
        this.mPostDialString = str + this.mPostDialString.substring(this.mNextPostDialChar);
        this.mNextPostDialChar = 0;
        Rlog.d(LOG_TAG, new StringBuilder().append("proceedAfterWildChar: new postDialString is ").append(this.mPostDialString).toString());
        processNextPostDialChar();
    }

    @Override
    public void cancelPostDial() {
        setPostDialState(Connection.PostDialState.CANCELLED);
    }

    void onHangupLocal() {
        this.mCause = 3;
    }

    @Override
    public boolean onDisconnect(int cause) {
        Rlog.d(LOG_TAG, "onDisconnect: cause=" + cause);
        if (this.mCause != 3) {
            this.mCause = cause;
        }
        String optr = SystemProperties.get("persist.operator.optr");
        if (optr != null && optr.equals("OP01") && isIncoming() && getConnectTime() == 0 && this.mCause == 3) {
            this.mCause = 16;
        }
        return onDisconnect();
    }

    public boolean onDisconnect() {
        boolean changed = false;
        if (!this.mDisconnected) {
            this.mDisconnectTime = System.currentTimeMillis();
            this.mDuration = SystemClock.elapsedRealtime() - this.mConnectTimeReal;
            this.mDisconnected = true;
            this.mOwner.mPhone.notifyDisconnect(this);
            if (this.mParent != null) {
                changed = this.mParent.connectionDisconnected(this);
            } else {
                Rlog.d(LOG_TAG, "onDisconnect: no parent");
            }
            this.mCallIdBeforeDisconnected = getCallId();
            if (this.mImsCall != null) {
                this.mImsCall.close();
            }
            this.mImsCall = null;
        }
        releaseWakeLock();
        return changed;
    }

    void onConnectedInOrOut() {
        this.mConnectTime = System.currentTimeMillis();
        this.mConnectTimeReal = SystemClock.elapsedRealtime();
        this.mDuration = 0L;
        Rlog.d(LOG_TAG, "onConnectedInOrOut: connectTime=" + this.mConnectTime);
        if (!this.mIsIncoming) {
            processNextPostDialChar();
        }
        releaseWakeLock();
    }

    void onStartedHolding() {
        this.mHoldingStartTime = SystemClock.elapsedRealtime();
    }

    private boolean processPostDialChar(char c) {
        if (PhoneNumberUtils.is12Key(c)) {
            this.mOwner.sendDtmf(c, this.mHandler.obtainMessage(1));
        } else if (c == ',') {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(2), 3000L);
        } else if (c == ';') {
            setPostDialState(Connection.PostDialState.WAIT);
        } else if (c == 'N') {
            setPostDialState(Connection.PostDialState.WILD);
        } else {
            return false;
        }
        return true;
    }

    protected void finalize() {
        releaseWakeLock();
    }

    private void processNextPostDialChar() {
        char c;
        Message notifyMessage;
        if (this.mPostDialState == Connection.PostDialState.CANCELLED) {
            return;
        }
        if (this.mPostDialString == null || this.mPostDialString.length() <= this.mNextPostDialChar) {
            setPostDialState(Connection.PostDialState.COMPLETE);
            c = 0;
        } else {
            setPostDialState(Connection.PostDialState.STARTED);
            String str = this.mPostDialString;
            int i = this.mNextPostDialChar;
            this.mNextPostDialChar = i + 1;
            c = str.charAt(i);
            boolean isValid = processPostDialChar(c);
            if (!isValid) {
                this.mHandler.obtainMessage(3).sendToTarget();
                Rlog.e(LOG_TAG, "processNextPostDialChar: c=" + c + " isn't valid!");
                return;
            }
        }
        notifyPostDialListenersNextChar(c);
        Registrant postDialHandler = this.mOwner.mPhone.getPostDialHandler();
        if (postDialHandler == null || (notifyMessage = postDialHandler.messageForRegistrant()) == null) {
            return;
        }
        Connection.PostDialState state = this.mPostDialState;
        AsyncResult ar = AsyncResult.forMessage(notifyMessage);
        ar.result = this;
        ar.userObj = state;
        notifyMessage.arg1 = c;
        notifyMessage.sendToTarget();
    }

    private void setPostDialState(Connection.PostDialState s) {
        if (this.mPostDialState != Connection.PostDialState.STARTED && s == Connection.PostDialState.STARTED) {
            acquireWakeLock();
            Message msg = this.mHandler.obtainMessage(4);
            this.mHandler.sendMessageDelayed(msg, 60000L);
        } else if (this.mPostDialState == Connection.PostDialState.STARTED && s != Connection.PostDialState.STARTED) {
            this.mHandler.removeMessages(4);
            releaseWakeLock();
        }
        this.mPostDialState = s;
        notifyPostDialListeners();
    }

    private void createWakeLock(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService("power");
        this.mPartialWakeLock = pm.newWakeLock(1, LOG_TAG);
    }

    private void acquireWakeLock() {
        Rlog.d(LOG_TAG, "acquireWakeLock");
        this.mPartialWakeLock.acquire();
    }

    void releaseWakeLock() {
        synchronized (this.mPartialWakeLock) {
            if (this.mPartialWakeLock.isHeld()) {
                Rlog.d(LOG_TAG, "releaseWakeLock");
                this.mPartialWakeLock.release();
            }
        }
    }

    private void fetchDtmfToneDelay(Phone phone) {
        CarrierConfigManager configMgr = (CarrierConfigManager) phone.getContext().getSystemService("carrier_config");
        PersistableBundle b = configMgr.getConfigForSubId(phone.getSubId());
        if (b == null) {
            return;
        }
        this.mDtmfToneDelay = b.getInt("ims_dtmf_tone_delay_int");
    }

    @Override
    public int getNumberPresentation() {
        return this.mNumberPresentation;
    }

    @Override
    public UUSInfo getUUSInfo() {
        return this.mUusInfo;
    }

    @Override
    public Connection getOrigConnection() {
        return null;
    }

    @Override
    public boolean isMultiparty() {
        if (this.mImsCall != null) {
            return this.mImsCall.isMultiparty();
        }
        return false;
    }

    @Override
    public boolean isConferenceHost() {
        if (this.mImsCall == null) {
            return false;
        }
        return this.mImsCall.isConferenceHost();
    }

    @Override
    public boolean isMemberOfPeerConference() {
        return !isConferenceHost();
    }

    public ImsCall getImsCall() {
        return this.mImsCall;
    }

    public void setImsCall(ImsCall imsCall) {
        this.mImsCall = imsCall;
    }

    public void changeParent(ImsPhoneCall parent) {
        this.mParent = parent;
    }

    public boolean update(ImsCall imsCall, Call.State state) {
        if (state == Call.State.ACTIVE) {
            if (imsCall.isPendingHold()) {
                Rlog.w(LOG_TAG, "update : state is ACTIVE, but call is pending hold, skipping");
                return false;
            }
            if (this.mParent.getState().isRinging() || this.mParent.getState().isDialing()) {
                onConnectedInOrOut();
            }
            if ((this.mParent.getState().isRinging() || this.mParent == this.mOwner.mBackgroundCall) && this.mParent != this.mOwner.mHandoverCall) {
                Rlog.d(LOG_TAG, "update() - Switch Connection to foreground call:" + this);
                this.mParent.detach(this);
                this.mParent = this.mOwner.mForegroundCall;
                this.mParent.attach(this);
            }
        } else if (state == Call.State.HOLDING) {
            if (this.mParent == this.mOwner.mForegroundCall) {
                Rlog.d(LOG_TAG, "update() - Switch Connection to background call:" + this);
                this.mParent.detach(this);
                this.mParent = this.mOwner.mBackgroundCall;
                this.mParent.attach(this);
            }
            onStartedHolding();
        }
        boolean updateParent = this.mParent.update(this, imsCall, state);
        boolean updateWifiState = updateWifiState();
        boolean updateAddressDisplay = updateAddressDisplay(imsCall);
        boolean updateMediaCapabilities = updateMediaCapabilities(imsCall);
        boolean updateExtras = updateExtras(imsCall);
        if (updateParent || updateWifiState || updateAddressDisplay || updateMediaCapabilities) {
            return true;
        }
        return updateExtras;
    }

    @Override
    public int getPreciseDisconnectCause() {
        return 0;
    }

    @Override
    public void onDisconnectConferenceParticipant(Uri endpoint) {
        if (this.mOwner != null) {
            this.mOwner.logDebugMessagesWithOpFormat("CC", "RemoveMember", this, " remove: " + endpoint);
        }
        ImsCall imsCall = getImsCall();
        if (imsCall == null) {
            return;
        }
        try {
            imsCall.removeParticipants(new String[]{endpoint.toString()});
        } catch (ImsException e) {
            Rlog.e(LOG_TAG, "onDisconnectConferenceParticipant: no session in place. Failed to disconnect endpoint = " + endpoint);
        }
    }

    public void setConferenceConnectTime(long conferenceConnectTime) {
        this.mConferenceConnectTime = conferenceConnectTime;
    }

    public long getConferenceConnectTime() {
        return this.mConferenceConnectTime;
    }

    public boolean updateAddressDisplay(ImsCall imsCall) {
        if (imsCall == null) {
            return false;
        }
        boolean changed = false;
        ImsCallProfile callProfile = imsCall.getCallProfile();
        if (callProfile != null) {
            String address = callProfile.getCallExtra("oi");
            String name = callProfile.getCallExtra("cna");
            int nump = ImsCallProfile.OIRToPresentation(callProfile.getCallExtraInt("oir"));
            if (!this.mIsIncoming) {
                nump = 1;
            }
            int namep = ImsCallProfile.OIRToPresentation(callProfile.getCallExtraInt("cnap"));
            Rlog.d(LOG_TAG, "address = " + address + " name = " + name + " nump = " + nump + " namep = " + namep + " mAddr = " + this.mAddress);
            if (!equalsHandlesNulls(this.mAddress, address)) {
                Rlog.d(LOG_TAG, "update address = " + address + " isMpty = " + isMultiparty());
                if (!isMultiparty() || !TextUtils.isEmpty(address)) {
                    this.mAddress = address;
                    changed = true;
                }
            }
            if (TextUtils.isEmpty(name)) {
                if (!TextUtils.isEmpty(this.mCnapName)) {
                    this.mCnapName = UsimPBMemInfo.STRING_NOT_SET;
                    changed = true;
                }
            } else if (!name.equals(this.mCnapName)) {
                this.mCnapName = name;
                changed = true;
            }
            if (this.mNumberPresentation != nump) {
                this.mNumberPresentation = nump;
                changed = true;
            }
            if (this.mCnapNamePresentation != namep) {
                this.mCnapNamePresentation = namep;
                changed = true;
            }
        }
        if (changed) {
            setConnectionAddressDisplay();
        }
        return changed;
    }

    public boolean updateMediaCapabilities(ImsCall imsCall) {
        if (imsCall == null) {
            return false;
        }
        boolean changed = false;
        try {
            ImsCallProfile negotiatedCallProfile = imsCall.getCallProfile();
            if (negotiatedCallProfile != null) {
                int oldVideoState = getVideoState();
                int newVideoState = ImsCallProfile.getVideoStateFromImsCallProfile(negotiatedCallProfile);
                if (oldVideoState != newVideoState) {
                    setVideoState(newVideoState);
                    changed = true;
                }
            }
            int capabilities = getConnectionCapabilities();
            ImsCallProfile localCallProfile = imsCall.getLocalCallProfile();
            Rlog.v(LOG_TAG, "update localCallProfile=" + localCallProfile);
            if (localCallProfile != null) {
                capabilities = applyLocalCallCapabilities(localCallProfile, capabilities);
            }
            ImsCallProfile remoteCallProfile = imsCall.getRemoteCallProfile();
            Rlog.v(LOG_TAG, "update remoteCallProfile=" + remoteCallProfile);
            if (remoteCallProfile != null) {
                capabilities = applyRemoteCallCapabilities(remoteCallProfile, capabilities);
            }
            if (getConnectionCapabilities() != capabilities) {
                setConnectionCapabilities(capabilities);
                changed = true;
            }
            int newAudioQuality = getAudioQualityFromCallProfile(localCallProfile, remoteCallProfile);
            if (getAudioQuality() != newAudioQuality) {
                setAudioQuality(newAudioQuality);
                return true;
            }
            return changed;
        } catch (ImsException e) {
            return changed;
        }
    }

    public boolean updateWifiState() {
        if (this.mIsWifiStateFromExtras) {
            return false;
        }
        Rlog.d(LOG_TAG, "updateWifiState: " + this.mOwner.isVowifiEnabled());
        if (isWifi() == this.mOwner.isVowifiEnabled()) {
            return false;
        }
        setWifi(this.mOwner.isVowifiEnabled());
        return true;
    }

    private void updateWifiStateFromExtras(Bundle extras) {
        int radioTechnology;
        if (!extras.containsKey("CallRadioTech")) {
            return;
        }
        try {
            radioTechnology = Integer.parseInt(extras.getString("CallRadioTech"));
        } catch (NumberFormatException e) {
            radioTechnology = 0;
        }
        this.mIsWifiStateFromExtras = true;
        boolean isWifi = radioTechnology == 18;
        if (isWifi() == isWifi) {
            return;
        }
        setWifi(isWifi);
    }

    boolean updateExtras(ImsCall imsCall) {
        if (imsCall == null) {
            return false;
        }
        ImsCallProfile callProfile = imsCall.getCallProfile();
        Bundle extras = callProfile != null ? callProfile.mCallExtras : null;
        if (extras == null) {
            Rlog.d(LOG_TAG, "Call profile extras are null.");
        }
        boolean changed = areBundlesEqual(extras, this.mExtras) ? false : true;
        if (changed) {
            updateWifiStateFromExtras(extras);
            this.mExtras.clear();
            this.mExtras.putAll(extras);
            setConnectionExtras(this.mExtras);
        }
        return changed;
    }

    private static boolean areBundlesEqual(Bundle extras, Bundle newExtras) {
        if (extras == null || newExtras == null) {
            return extras == newExtras;
        }
        if (extras.size() != newExtras.size()) {
            return false;
        }
        for (String key : extras.keySet()) {
            if (key != null) {
                Object value = extras.get(key);
                Object newValue = newExtras.get(key);
                if (!Objects.equals(value, newValue)) {
                    return false;
                }
            }
        }
        return true;
    }

    private int getAudioQualityFromCallProfile(ImsCallProfile localCallProfile, ImsCallProfile remoteCallProfile) {
        boolean isHighDef;
        if (localCallProfile == null || remoteCallProfile == null || localCallProfile.mMediaProfile == null) {
            return 1;
        }
        if (localCallProfile.mMediaProfile.mAudioQuality == 2 || localCallProfile.mMediaProfile.mAudioQuality == 6) {
            isHighDef = remoteCallProfile.mRestrictCause == 0;
        } else {
            isHighDef = false;
        }
        return isHighDef ? 2 : 1;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[ImsPhoneConnection objId: ");
        sb.append(System.identityHashCode(this));
        sb.append(" telecomCallID: ");
        sb.append(getTelecomCallId());
        sb.append(" address: ");
        sb.append(Log.pii(getAddress()));
        sb.append(" ImsCall: ");
        if (this.mImsCall == null) {
            sb.append("null");
        } else {
            sb.append(this.mImsCall);
        }
        sb.append("]");
        sb.append(" state:").append(getState());
        sb.append(" mParent:");
        sb.append(getParentCallName());
        return sb.toString();
    }

    protected boolean isEmergency() {
        return this.mIsEmergency;
    }

    public int getCallId() {
        ImsCall call = getImsCall();
        if (call == null || call.getCallSession() == null) {
            return -1;
        }
        String callId = call.getCallSession().getCallId();
        if (callId == null) {
            Rlog.d(LOG_TAG, "Abnormal! Call Id = null");
            return -1;
        }
        return Integer.parseInt(callId);
    }

    int getCallIdBeforeDisconnected() {
        return this.mCallIdBeforeDisconnected;
    }

    ArrayList<String> getConfDialStrings() {
        return this.mConfDialStrings;
    }

    public String getConferenceParticipantAddress(int index) {
        if (this.mConferenceParticipants == null) {
            Rlog.d(LOG_TAG, "getConferenceParticipantAddress(): no XML information");
            return UsimPBMemInfo.STRING_NOT_SET;
        }
        if (index < 0 || index + 1 >= this.mConferenceParticipants.size()) {
            Rlog.d(LOG_TAG, "getConferenceParticipantAddress(): invalid index");
            return UsimPBMemInfo.STRING_NOT_SET;
        }
        ConferenceParticipant participant = this.mConferenceParticipants.get(index + 1);
        if (participant == null) {
            Rlog.d(LOG_TAG, "getConferenceParticipantAddress(): empty participant info");
            return UsimPBMemInfo.STRING_NOT_SET;
        }
        Uri userEntity = participant.getHandle();
        Rlog.d(LOG_TAG, "getConferenceParticipantAddress(): ret=" + userEntity);
        return userEntity.toString();
    }

    String getParentCallName() {
        if (this.mOwner == null) {
            return "Unknown";
        }
        if (this.mParent == this.mOwner.mForegroundCall) {
            return "Foreground Call";
        }
        if (this.mParent == this.mOwner.mBackgroundCall) {
            return "Background Call";
        }
        if (this.mParent == this.mOwner.mRingingCall) {
            return "Ringing Call";
        }
        if (this.mParent == this.mOwner.mHandoverCall) {
            return "Handover Call";
        }
        return "Abnormal";
    }

    @Override
    public boolean isIncomingCallMultiparty() {
        if (this.mImsCall != null) {
            return this.mImsCall.isIncomingCallMultiparty();
        }
        return false;
    }

    public void inviteConferenceParticipants(List<String> numbers) {
        StringBuilder sb = new StringBuilder();
        for (String number : numbers) {
            sb.append(number);
            sb.append(", ");
        }
        if (this.mOwner != null) {
            this.mOwner.logDebugMessagesWithOpFormat("CC", "AddMember", this, " invite with " + sb.toString());
        }
        ImsCall imsCall = getImsCall();
        if (imsCall == null) {
            return;
        }
        ArrayList<String> list = new ArrayList<>();
        list.addAll(numbers);
        String[] participants = (String[]) list.toArray(new String[list.size()]);
        try {
            imsCall.inviteParticipants(participants);
        } catch (ImsException e) {
            Rlog.e(LOG_TAG, "inviteConferenceParticipants: no call session and fail to invite participants " + participants);
        }
    }

    void setConfDialStrings(ArrayList<String> dialStrings) {
        this.mConfDialStrings = dialStrings;
    }

    void setConferenceAsHost() {
        Rlog.d(LOG_TAG, "set is conference host connection: " + this);
        this.mIsIncoming = false;
    }

    void setVendorDisconnectCause(String cause) {
        this.mVendorCause = cause;
    }

    @Override
    public void updateConferenceParticipants(List<ConferenceParticipant> conferenceParticipants) {
        this.mConferenceParticipants = conferenceParticipants;
        super.updateConferenceParticipants(conferenceParticipants);
    }

    public void unhold() throws CallStateException {
        if (this.mOwner == null) {
            return;
        }
        this.mOwner.unhold(this);
    }
}
