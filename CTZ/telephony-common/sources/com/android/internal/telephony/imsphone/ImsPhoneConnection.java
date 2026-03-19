package com.android.internal.telephony.imsphone;

import android.R;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.Registrant;
import android.os.SystemClock;
import android.telecom.Connection;
import android.telecom.VideoProfile;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.ims.ImsCallProfile;
import android.text.TextUtils;
import com.android.ims.ImsCall;
import com.android.ims.ImsException;
import com.android.ims.internal.ImsVideoCallProviderWrapper;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.UUSInfo;
import com.android.internal.telephony.imsphone.ImsRttTextHandler;
import java.util.Objects;

public class ImsPhoneConnection extends Connection implements ImsVideoCallProviderWrapper.ImsVideoProviderWrapperCallback {
    private static final boolean DBG = true;
    private static final int EVENT_DTMF_DELAY_DONE = 5;
    private static final int EVENT_DTMF_DONE = 1;
    private static final int EVENT_NEXT_POST_DIAL = 3;
    private static final int EVENT_PAUSE_DONE = 2;
    private static final int EVENT_WAKE_LOCK_TIMEOUT = 4;
    private static final String LOG_TAG = "ImsPhoneConnection";
    private static final int PAUSE_DELAY_MILLIS = 3000;
    private static final int WAKE_LOCK_TIMEOUT_MILLIS = 60000;
    private long mConferenceConnectTime;
    protected long mDisconnectTime;
    protected boolean mDisconnected;
    private int mDtmfToneDelay;
    private Bundle mExtras;
    protected Handler mHandler;
    private Messenger mHandlerMessenger;
    protected ImsCall mImsCall;
    private ImsVideoCallProviderWrapper mImsVideoCallProviderWrapper;
    protected boolean mIsEmergency;
    private boolean mIsMergeInProcess;
    private boolean mIsRttEnabledForCall;
    private boolean mIsVideoEnabled;
    protected ImsPhoneCallTracker mOwner;
    protected ImsPhoneCall mParent;
    protected PowerManager.WakeLock mPartialWakeLock;
    private int mPreciseDisconnectCause;
    public ImsRttTextHandler mRttTextHandler;
    public Connection.RttTextStream mRttTextStream;
    private boolean mShouldIgnoreVideoStateChanges;
    protected UUSInfo mUusInfo;

    protected class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
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

    public ImsPhoneConnection(Phone phone, ImsCall imsCall, ImsPhoneCallTracker imsPhoneCallTracker, ImsPhoneCall imsPhoneCall, boolean z) {
        super(5);
        this.mExtras = new Bundle();
        this.mConferenceConnectTime = 0L;
        this.mDtmfToneDelay = 0;
        this.mIsEmergency = false;
        this.mShouldIgnoreVideoStateChanges = false;
        this.mPreciseDisconnectCause = 0;
        this.mIsRttEnabledForCall = false;
        this.mIsMergeInProcess = false;
        this.mIsVideoEnabled = true;
        createWakeLock(phone.getContext());
        acquireWakeLock();
        this.mOwner = imsPhoneCallTracker;
        this.mHandler = new MyHandler(this.mOwner.getLooper());
        this.mHandlerMessenger = new Messenger(this.mHandler);
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
        this.mIsIncoming = !z;
        this.mCreateTime = System.currentTimeMillis();
        this.mUusInfo = null;
        updateExtras(imsCall);
        this.mParent = imsPhoneCall;
        this.mParent.attach(this, this.mIsIncoming ? Call.State.INCOMING : Call.State.DIALING);
        fetchDtmfToneDelay(phone);
        if (phone.getContext().getResources().getBoolean(R.^attr-private.pointerIconZoomIn)) {
            setAudioModeIsVoip(true);
        }
    }

    public ImsPhoneConnection(Phone phone, String str, ImsPhoneCallTracker imsPhoneCallTracker, ImsPhoneCall imsPhoneCall, boolean z) {
        super(5);
        this.mExtras = new Bundle();
        this.mConferenceConnectTime = 0L;
        this.mDtmfToneDelay = 0;
        this.mIsEmergency = false;
        this.mShouldIgnoreVideoStateChanges = false;
        this.mPreciseDisconnectCause = 0;
        this.mIsRttEnabledForCall = false;
        this.mIsMergeInProcess = false;
        this.mIsVideoEnabled = true;
        createWakeLock(phone.getContext());
        acquireWakeLock();
        this.mOwner = imsPhoneCallTracker;
        this.mHandler = new MyHandler(this.mOwner.getLooper());
        this.mDialString = str;
        this.mAddress = PhoneNumberUtils.extractNetworkPortionAlt(str);
        this.mPostDialString = PhoneNumberUtils.extractPostDialPortion(str);
        this.mIsIncoming = false;
        this.mCnapName = null;
        this.mCnapNamePresentation = 1;
        this.mNumberPresentation = 1;
        this.mCreateTime = System.currentTimeMillis();
        this.mParent = imsPhoneCall;
        imsPhoneCall.attachFake(this, Call.State.DIALING);
        this.mIsEmergency = z;
        fetchDtmfToneDelay(phone);
        if (phone.getContext().getResources().getBoolean(R.^attr-private.pointerIconZoomIn)) {
            setAudioModeIsVoip(true);
        }
    }

    public void dispose() {
    }

    protected static boolean equalsHandlesNulls(Object obj, Object obj2) {
        return obj == null ? obj2 == null : obj.equals(obj2);
    }

    protected static boolean equalsBaseDialString(String str, String str2) {
        if (str == null) {
            if (str2 != null) {
                return false;
            }
        } else if (str2 == null || !str.startsWith(str2)) {
            return false;
        }
        return true;
    }

    private int applyLocalCallCapabilities(ImsCallProfile imsCallProfile, int i) {
        Rlog.i(LOG_TAG, "applyLocalCallCapabilities - localProfile = " + imsCallProfile);
        int iRemoveCapability = removeCapability(i, 4);
        if (!this.mIsVideoEnabled) {
            Rlog.i(LOG_TAG, "applyLocalCallCapabilities - disabling video (overidden)");
            return iRemoveCapability;
        }
        switch (imsCallProfile.mCallType) {
            case 3:
            case 4:
                return addCapability(iRemoveCapability, 4);
            default:
                return iRemoveCapability;
        }
    }

    private static int applyRemoteCallCapabilities(ImsCallProfile imsCallProfile, int i) {
        Rlog.w(LOG_TAG, "applyRemoteCallCapabilities - remoteProfile = " + imsCallProfile);
        int iRemoveCapability = removeCapability(i, 8);
        switch (imsCallProfile.mCallType) {
            case 3:
            case 4:
                return addCapability(iRemoveCapability, 8);
            default:
                return iRemoveCapability;
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

    public void setDisconnectCause(int i) {
        this.mCause = i;
    }

    @Override
    public String getVendorDisconnectCause() {
        return null;
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
    public void deflect(String str) throws CallStateException {
        if (this.mParent.getState().isRinging()) {
            try {
                if (this.mImsCall != null) {
                    this.mImsCall.deflect(str);
                    return;
                }
                throw new CallStateException("no valid ims call to deflect");
            } catch (ImsException e) {
                throw new CallStateException("cannot deflect call");
            }
        }
        throw new CallStateException("phone not ringing");
    }

    @Override
    public void hangup() throws CallStateException {
        if (!this.mDisconnected) {
            this.mOwner.hangup(this);
            return;
        }
        throw new CallStateException("disconnected");
    }

    @Override
    public void separate() throws CallStateException {
        throw new CallStateException("not supported");
    }

    @Override
    public void proceedAfterWaitChar() {
        if (this.mPostDialState != Connection.PostDialState.WAIT) {
            Rlog.w(LOG_TAG, "ImsPhoneConnection.proceedAfterWaitChar(): Expected getPostDialState() to be WAIT but was " + this.mPostDialState);
            return;
        }
        setPostDialState(Connection.PostDialState.STARTED);
        processNextPostDialChar();
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
        Rlog.d(LOG_TAG, "proceedAfterWildChar: new postDialString is " + this.mPostDialString);
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
    public boolean onDisconnect(int i) {
        Rlog.d(LOG_TAG, "onDisconnect: cause=" + i);
        if (this.mCause != 3 || i == 16) {
            this.mCause = i;
        }
        checkIncomingRejected(i);
        return onDisconnect();
    }

    public boolean onDisconnect() {
        boolean zConnectionDisconnected = false;
        if (!this.mDisconnected) {
            this.mDisconnectTime = System.currentTimeMillis();
            this.mDuration = SystemClock.elapsedRealtime() - this.mConnectTimeReal;
            this.mDisconnected = true;
            this.mOwner.mPhone.notifyDisconnect(this);
            notifyDisconnect(this.mCause);
            if (this.mParent != null) {
                zConnectionDisconnected = this.mParent.connectionDisconnected(this);
            } else {
                Rlog.d(LOG_TAG, "onDisconnect: no parent");
            }
            synchronized (this) {
                if (this.mImsCall != null) {
                    this.mImsCall.close();
                }
                this.mImsCall = null;
            }
        }
        releaseWakeLock();
        return zConnectionDisconnected;
    }

    protected void onConnectedInOrOut() {
        this.mConnectTime = System.currentTimeMillis();
        this.mConnectTimeReal = SystemClock.elapsedRealtime();
        this.mDuration = 0L;
        Rlog.d(LOG_TAG, "onConnectedInOrOut: connectTime=" + this.mConnectTime);
        if (!this.mIsIncoming) {
            processNextPostDialChar();
        }
        releaseWakeLock();
    }

    protected void onStartedHolding() {
        this.mHoldingStartTime = SystemClock.elapsedRealtime();
    }

    private boolean processPostDialChar(char c) {
        if (PhoneNumberUtils.is12Key(c)) {
            Message messageObtainMessage = this.mHandler.obtainMessage(1);
            messageObtainMessage.replyTo = this.mHandlerMessenger;
            this.mOwner.sendDtmf(c, messageObtainMessage);
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

    public void finalize() {
        releaseWakeLock();
    }

    private void processNextPostDialChar() {
        char cCharAt;
        Message messageMessageForRegistrant;
        if (this.mPostDialState == Connection.PostDialState.CANCELLED) {
            return;
        }
        if (this.mPostDialString == null || this.mPostDialString.length() <= this.mNextPostDialChar) {
            setPostDialState(Connection.PostDialState.COMPLETE);
            cCharAt = 0;
        } else {
            setPostDialState(Connection.PostDialState.STARTED);
            String str = this.mPostDialString;
            int i = this.mNextPostDialChar;
            this.mNextPostDialChar = i + 1;
            cCharAt = str.charAt(i);
            if (!processPostDialChar(cCharAt)) {
                this.mHandler.obtainMessage(3).sendToTarget();
                Rlog.e(LOG_TAG, "processNextPostDialChar: c=" + cCharAt + " isn't valid!");
                return;
            }
        }
        notifyPostDialListenersNextChar(cCharAt);
        Registrant postDialHandler = this.mOwner.mPhone.getPostDialHandler();
        if (postDialHandler != null && (messageMessageForRegistrant = postDialHandler.messageForRegistrant()) != null) {
            Connection.PostDialState postDialState = this.mPostDialState;
            AsyncResult asyncResultForMessage = AsyncResult.forMessage(messageMessageForRegistrant);
            asyncResultForMessage.result = this;
            asyncResultForMessage.userObj = postDialState;
            messageMessageForRegistrant.arg1 = cCharAt;
            messageMessageForRegistrant.sendToTarget();
        }
    }

    private void setPostDialState(Connection.PostDialState postDialState) {
        if (this.mPostDialState != Connection.PostDialState.STARTED && postDialState == Connection.PostDialState.STARTED) {
            acquireWakeLock();
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(4), 60000L);
        } else if (this.mPostDialState == Connection.PostDialState.STARTED && postDialState != Connection.PostDialState.STARTED) {
            this.mHandler.removeMessages(4);
            releaseWakeLock();
        }
        this.mPostDialState = postDialState;
        notifyPostDialListeners();
    }

    protected void createWakeLock(Context context) {
        this.mPartialWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, LOG_TAG);
    }

    protected void acquireWakeLock() {
        Rlog.d(LOG_TAG, "acquireWakeLock");
        this.mPartialWakeLock.acquire();
    }

    public void releaseWakeLock() {
        if (this.mPartialWakeLock != null) {
            synchronized (this.mPartialWakeLock) {
                if (this.mPartialWakeLock.isHeld()) {
                    Rlog.d(LOG_TAG, "releaseWakeLock");
                    this.mPartialWakeLock.release();
                }
            }
        }
    }

    protected void fetchDtmfToneDelay(Phone phone) {
        PersistableBundle configForSubId = ((CarrierConfigManager) phone.getContext().getSystemService("carrier_config")).getConfigForSubId(phone.getSubId());
        if (configForSubId != null) {
            this.mDtmfToneDelay = configForSubId.getInt("ims_dtmf_tone_delay_int");
        }
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
    public com.android.internal.telephony.Connection getOrigConnection() {
        return null;
    }

    @Override
    public synchronized boolean isMultiparty() {
        boolean z;
        if (this.mImsCall != null) {
            z = this.mImsCall.isMultiparty();
        }
        return z;
    }

    @Override
    public synchronized boolean isConferenceHost() {
        boolean z;
        if (this.mImsCall != null) {
            z = this.mImsCall.isConferenceHost();
        }
        return z;
    }

    @Override
    public boolean isMemberOfPeerConference() {
        return !isConferenceHost();
    }

    public synchronized ImsCall getImsCall() {
        return this.mImsCall;
    }

    public synchronized void setImsCall(ImsCall imsCall) {
        this.mImsCall = imsCall;
    }

    public void changeParent(ImsPhoneCall imsPhoneCall) {
        this.mParent = imsPhoneCall;
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
            if ((this.mParent.getState().isRinging() || this.mParent == this.mOwner.mBackgroundCall) && !skipSwitchingCallToForeground()) {
                this.mParent.detach(this);
                this.mParent = this.mOwner.mForegroundCall;
                this.mParent.attach(this);
            }
        } else if (state == Call.State.HOLDING) {
            switchCallToBackgroundIfNecessary();
            onStartedHolding();
        }
        return this.mParent.update(this, imsCall, state) || updateAddressDisplay(imsCall) || updateMediaCapabilities(imsCall) || updateExtras(imsCall);
    }

    @Override
    public int getPreciseDisconnectCause() {
        return this.mPreciseDisconnectCause;
    }

    public void setPreciseDisconnectCause(int i) {
        this.mPreciseDisconnectCause = i;
    }

    @Override
    public void onDisconnectConferenceParticipant(Uri uri) {
        ImsCall imsCall = getImsCall();
        if (imsCall == null) {
            return;
        }
        try {
            imsCall.removeParticipants(new String[]{uri.toString()});
        } catch (ImsException e) {
            Rlog.e(LOG_TAG, "onDisconnectConferenceParticipant: no session in place. Failed to disconnect endpoint = " + uri);
        }
    }

    public void setConferenceConnectTime(long j) {
        this.mConferenceConnectTime = j;
    }

    public long getConferenceConnectTime() {
        return this.mConferenceConnectTime;
    }

    public boolean updateAddressDisplay(ImsCall imsCall) {
        ImsCallProfile callProfile;
        boolean z = false;
        if (imsCall == null || (callProfile = imsCall.getCallProfile()) == null) {
            return false;
        }
        if (!isIncoming() && !allowedUpdateMOAddress()) {
            return false;
        }
        String callExtra = callProfile.getCallExtra("oi");
        String callExtra2 = callProfile.getCallExtra("cna");
        int iCalNumberPresentation = calNumberPresentation(callProfile);
        int iOIRToPresentation = ImsCallProfile.OIRToPresentation(callProfile.getCallExtraInt("cnap"));
        Rlog.d(LOG_TAG, "updateAddressDisplay: callId = " + getTelecomCallId() + " address = " + Rlog.pii(LOG_TAG, callExtra) + " name = " + Rlog.pii(LOG_TAG, callExtra2) + " nump = " + iCalNumberPresentation + " namep = " + iOIRToPresentation);
        if (this.mIsMergeInProcess) {
            return false;
        }
        if (needUpdateAddress(callExtra)) {
            this.mAddress = callExtra;
            z = true;
        }
        if (TextUtils.isEmpty(callExtra2)) {
            if (!TextUtils.isEmpty(this.mCnapName)) {
                this.mCnapName = "";
                z = true;
            }
        } else if (!callExtra2.equals(this.mCnapName)) {
            this.mCnapName = callExtra2;
            z = true;
        }
        if (this.mNumberPresentation != iCalNumberPresentation) {
            this.mNumberPresentation = iCalNumberPresentation;
            z = true;
        }
        if (this.mCnapNamePresentation == iOIRToPresentation) {
            return z;
        }
        this.mCnapNamePresentation = iOIRToPresentation;
        return true;
    }

    public boolean updateMediaCapabilities(ImsCall imsCall) {
        int iRemoveCapability;
        boolean z = false;
        if (imsCall == null) {
            return false;
        }
        try {
            ImsCallProfile callProfile = imsCall.getCallProfile();
            if (callProfile != null) {
                int videoState = getVideoState();
                int videoStateFromImsCallProfile = ImsCallProfile.getVideoStateFromImsCallProfile(callProfile);
                if (videoState != videoStateFromImsCallProfile) {
                    if (VideoProfile.isPaused(videoState) && !VideoProfile.isPaused(videoStateFromImsCallProfile)) {
                        this.mShouldIgnoreVideoStateChanges = false;
                    }
                    if (!this.mShouldIgnoreVideoStateChanges) {
                        updateVideoState(videoStateFromImsCallProfile);
                        z = true;
                    } else {
                        Rlog.d(LOG_TAG, "updateMediaCapabilities - ignoring video state change due to paused state.");
                    }
                    if (!VideoProfile.isPaused(videoState) && VideoProfile.isPaused(videoStateFromImsCallProfile)) {
                        this.mShouldIgnoreVideoStateChanges = true;
                    }
                }
                if (callProfile.mMediaProfile != null) {
                    this.mIsRttEnabledForCall = callProfile.mMediaProfile.isRttCall();
                    if (this.mIsRttEnabledForCall && this.mRttTextHandler == null) {
                        Rlog.d(LOG_TAG, "updateMediaCapabilities -- turning RTT on, profile=" + callProfile);
                        startRttTextProcessing();
                        onRttInitiated();
                    } else if (!this.mIsRttEnabledForCall && this.mRttTextHandler != null) {
                        Rlog.d(LOG_TAG, "updateMediaCapabilities -- turning RTT off, profile=" + callProfile);
                        this.mRttTextHandler.tearDown();
                        this.mRttTextHandler = null;
                        onRttTerminated();
                    }
                    z = true;
                }
            }
            int connectionCapabilities = getConnectionCapabilities();
            if (this.mOwner.isCarrierDowngradeOfVtCallSupported()) {
                iRemoveCapability = addCapability(connectionCapabilities, 3);
            } else {
                iRemoveCapability = removeCapability(connectionCapabilities, 3);
            }
            ImsCallProfile localCallProfile = imsCall.getLocalCallProfile();
            Rlog.v(LOG_TAG, "update localCallProfile=" + localCallProfile);
            if (localCallProfile != null) {
                iRemoveCapability = applyLocalCallCapabilities(localCallProfile, iRemoveCapability);
            }
            ImsCallProfile remoteCallProfile = imsCall.getRemoteCallProfile();
            Rlog.v(LOG_TAG, "update remoteCallProfile=" + remoteCallProfile);
            if (remoteCallProfile != null) {
                iRemoveCapability = applyVideoRingtoneCapabilities(remoteCallProfile, applyRemoteCallCapabilities(remoteCallProfile, iRemoveCapability));
            }
            if (getConnectionCapabilities() != iRemoveCapability) {
                setConnectionCapabilities(iRemoveCapability);
                z = true;
            }
            int audioQualityFromCallProfile = getAudioQualityFromCallProfile(localCallProfile, remoteCallProfile);
            if (getAudioQuality() != audioQualityFromCallProfile) {
                setAudioQuality(audioQualityFromCallProfile);
                return true;
            }
            return z;
        } catch (ImsException e) {
            return z;
        }
    }

    private void updateVideoState(int i) {
        if (this.mImsVideoCallProviderWrapper != null) {
            this.mImsVideoCallProviderWrapper.onVideoStateChanged(i);
        }
        setVideoState(i);
    }

    public void sendRttModifyRequest(Connection.RttTextStream rttTextStream) {
        getImsCall().sendRttModifyRequest();
        setCurrentRttTextStream(rttTextStream);
    }

    public void sendRttModifyResponse(Connection.RttTextStream rttTextStream) {
        boolean z = rttTextStream != null;
        getImsCall().sendRttModifyResponse(z);
        if (z) {
            setCurrentRttTextStream(rttTextStream);
        } else {
            Rlog.e(LOG_TAG, "sendRttModifyResponse: foreground call has no connections");
        }
    }

    public void onRttMessageReceived(String str) {
        synchronized (this) {
            if (this.mRttTextHandler == null) {
                Rlog.w(LOG_TAG, "onRttMessageReceived: RTT text handler not available. Attempting to create one.");
                if (this.mRttTextStream == null) {
                    Rlog.e(LOG_TAG, "onRttMessageReceived: Unable to process incoming message. No textstream available");
                    return;
                }
                createRttTextHandler();
            }
            this.mRttTextHandler.sendToInCall(str);
        }
    }

    public void setCurrentRttTextStream(Connection.RttTextStream rttTextStream) {
        synchronized (this) {
            this.mRttTextStream = rttTextStream;
            if (this.mRttTextHandler == null && this.mIsRttEnabledForCall) {
                Rlog.i(LOG_TAG, "setCurrentRttTextStream: Creating a text handler");
                createRttTextHandler();
            }
        }
    }

    public boolean hasRttTextStream() {
        return this.mRttTextStream != null;
    }

    public boolean isRttEnabledForCall() {
        return this.mIsRttEnabledForCall;
    }

    public void startRttTextProcessing() {
        synchronized (this) {
            if (this.mRttTextStream == null) {
                Rlog.w(LOG_TAG, "startRttTextProcessing: no RTT text stream. Ignoring.");
            } else if (this.mRttTextHandler != null) {
                Rlog.w(LOG_TAG, "startRttTextProcessing: RTT text handler already exists");
            } else {
                createRttTextHandler();
            }
        }
    }

    protected void createRttTextHandler() {
        this.mRttTextHandler = new ImsRttTextHandler(Looper.getMainLooper(), new ImsRttTextHandler.NetworkWriter() {
            @Override
            public final void write(String str) {
                this.f$0.getImsCall().sendRttMessage(str);
            }
        });
        this.mRttTextHandler.initialize(this.mRttTextStream);
    }

    private void updateWifiStateFromExtras(Bundle bundle) {
        if (bundle.containsKey("CallRadioTech") || bundle.containsKey("callRadioTech")) {
            ImsCall imsCall = getImsCall();
            boolean zIsWifiCall = false;
            if (imsCall != null) {
                zIsWifiCall = imsCall.isWifiCall();
            }
            if (isWifi() != zIsWifiCall) {
                setWifi(zIsWifiCall);
            }
        }
    }

    protected boolean updateExtras(ImsCall imsCall) {
        if (imsCall == null) {
            return false;
        }
        ImsCallProfile callProfile = imsCall.getCallProfile();
        Bundle bundle = callProfile != null ? callProfile.mCallExtras : null;
        if (bundle == null) {
            Rlog.d(LOG_TAG, "Call profile extras are null.");
        }
        boolean z = !areBundlesEqual(bundle, this.mExtras);
        if (z) {
            updateWifiStateFromExtras(bundle);
            synchronized (this.mExtras) {
                this.mExtras.clear();
                this.mExtras.putAll(bundle);
            }
            setConnectionExtras(this.mExtras);
        }
        return z;
    }

    private static boolean areBundlesEqual(Bundle bundle, Bundle bundle2) {
        if (bundle == null || bundle2 == null) {
            return bundle == bundle2;
        }
        if (bundle.size() != bundle2.size()) {
            return false;
        }
        for (String str : bundle.keySet()) {
            if (str != null && !Objects.equals(bundle.get(str), bundle2.get(str))) {
                return false;
            }
        }
        return true;
    }

    protected int getAudioQualityFromCallProfile(ImsCallProfile imsCallProfile, ImsCallProfile imsCallProfile2) {
        if (imsCallProfile == null || imsCallProfile2 == null || imsCallProfile.mMediaProfile == null) {
            return 1;
        }
        boolean z = false;
        boolean z2 = imsCallProfile.mMediaProfile.mAudioQuality == 18 || imsCallProfile.mMediaProfile.mAudioQuality == 19 || imsCallProfile.mMediaProfile.mAudioQuality == 20;
        if ((imsCallProfile.mMediaProfile.mAudioQuality == 2 || imsCallProfile.mMediaProfile.mAudioQuality == 6 || z2) && imsCallProfile2.mRestrictCause == 0) {
            z = true;
        }
        return z ? 2 : 1;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[ImsPhoneConnection objId: ");
        sb.append(System.identityHashCode(this));
        sb.append(" telecomCallID: ");
        sb.append(getTelecomCallId());
        sb.append(" address: ");
        sb.append(Rlog.pii(LOG_TAG, getAddress()));
        sb.append(" ImsCall: ");
        synchronized (this) {
            if (this.mImsCall == null) {
                sb.append("null");
            } else {
                sb.append(this.mImsCall);
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public void setVideoProvider(Connection.VideoProvider videoProvider) {
        super.setVideoProvider(videoProvider);
        if (videoProvider instanceof ImsVideoCallProviderWrapper) {
            this.mImsVideoCallProviderWrapper = (ImsVideoCallProviderWrapper) videoProvider;
        }
    }

    public boolean isEmergency() {
        return this.mIsEmergency;
    }

    public void onReceiveSessionModifyResponse(int i, VideoProfile videoProfile, VideoProfile videoProfile2) {
        if (i == 1 && this.mShouldIgnoreVideoStateChanges) {
            int videoState = getVideoState();
            int videoState2 = videoProfile2.getVideoState();
            int i2 = (videoState ^ videoState2) & 3;
            if (i2 == 0) {
                return;
            }
            int i3 = (videoState & (~(i2 & videoState))) | (videoState2 & i2);
            Rlog.d(LOG_TAG, "onReceiveSessionModifyResponse : received " + VideoProfile.videoStateToString(videoProfile.getVideoState()) + " / " + VideoProfile.videoStateToString(videoProfile2.getVideoState()) + " while paused ; sending new videoState = " + VideoProfile.videoStateToString(i3));
            setVideoState(i3);
        }
    }

    public void pauseVideo(int i) {
        if (this.mImsVideoCallProviderWrapper == null) {
            return;
        }
        this.mImsVideoCallProviderWrapper.pauseVideo(getVideoState(), i);
    }

    public void resumeVideo(int i) {
        if (this.mImsVideoCallProviderWrapper == null) {
            return;
        }
        this.mImsVideoCallProviderWrapper.resumeVideo(getVideoState(), i);
    }

    public boolean wasVideoPausedFromSource(int i) {
        if (this.mImsVideoCallProviderWrapper == null) {
            return false;
        }
        return this.mImsVideoCallProviderWrapper.wasVideoPausedFromSource(i);
    }

    public void handleMergeStart() {
        this.mIsMergeInProcess = true;
        onConnectionEvent("android.telecom.event.MERGE_START", null);
    }

    public void handleMergeComplete() {
        this.mIsMergeInProcess = false;
        onConnectionEvent("android.telecom.event.MERGE_COMPLETE", null);
    }

    public void changeToPausedState() {
        int videoState = getVideoState() | 4;
        Rlog.i(LOG_TAG, "ImsPhoneConnection: changeToPausedState - setting paused bit; newVideoState=" + VideoProfile.videoStateToString(videoState));
        updateVideoState(videoState);
        this.mShouldIgnoreVideoStateChanges = true;
    }

    public void changeToUnPausedState() {
        int videoState = getVideoState() & (-5);
        Rlog.i(LOG_TAG, "ImsPhoneConnection: changeToUnPausedState - unsetting paused bit; newVideoState=" + VideoProfile.videoStateToString(videoState));
        updateVideoState(videoState);
        this.mShouldIgnoreVideoStateChanges = false;
    }

    public void handleDataEnabledChange(boolean z) {
        this.mIsVideoEnabled = z;
        Rlog.i(LOG_TAG, "handleDataEnabledChange: isDataEnabled=" + z + "; updating local video availability.");
        updateMediaCapabilities(getImsCall());
        if (this.mImsVideoCallProviderWrapper != null) {
            this.mImsVideoCallProviderWrapper.setIsVideoEnabled(hasCapabilities(4));
        }
    }

    protected void checkIncomingRejected(int i) {
    }

    protected boolean skipSwitchingCallToForeground() {
        return false;
    }

    protected int applyVideoRingtoneCapabilities(ImsCallProfile imsCallProfile, int i) {
        return i;
    }

    protected void switchCallToBackgroundIfNecessary() {
    }

    protected int calNumberPresentation(ImsCallProfile imsCallProfile) {
        return ImsCallProfile.OIRToPresentation(imsCallProfile.getCallExtraInt("oir"));
    }

    protected boolean needUpdateAddress(String str) {
        return !equalsBaseDialString(this.mAddress, str);
    }

    protected boolean allowedUpdateMOAddress() {
        return false;
    }
}
