package com.android.internal.telephony;

import android.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.telephony.CellLocation;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.EventLog;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallTracker;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.DriverCall;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneInternalInterface;
import com.android.internal.telephony.cdma.CdmaCallWaitingNotification;
import com.android.internal.telephony.gsm.GsmCallTrackerHelper;
import com.android.internal.telephony.imsphone.ImsPhoneConnection;
import com.mediatek.common.MPlugin;
import com.mediatek.common.telephony.cdma.ICdmaCallTrackerExt;
import com.mediatek.internal.telephony.gsm.GsmVTProvider;
import com.mediatek.internal.telephony.gsm.GsmVideoCallProviderWrapper;
import com.mediatek.internal.telephony.gsm.IGsmVideoCallProvider;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GsmCdmaCallTracker extends CallTracker {
    private static final boolean DBG_POLL = false;
    private static final String LOG_TAG = "GsmCdmaCallTracker";
    private static final int MAX_CONNECTIONS_CDMA = 8;
    public static final int MAX_CONNECTIONS_GSM = 19;
    private static final int MAX_CONNECTIONS_PER_CALL_CDMA = 1;
    private static final int MAX_CONNECTIONS_PER_CALL_GSM = 5;
    private static final String PROP_LOG_TAG = "GsmCdmaCallTkr";
    private static final boolean REPEAT_POLLING = false;
    private static final boolean VDBG;
    private int m3WayCallFlashDelay;
    private ICdmaCallTrackerExt mCdmaCallTrackerExt;
    public GsmCdmaConnection[] mConnections;
    private TelephonyEventLog mEventLog;
    public boolean mHangupPendingMO;
    GsmCallTrackerHelper mHelper;
    private boolean mIsEcmTimerCanceled;
    private boolean mIsInEmergencyCall;
    private int mPendingCallClirMode;
    private boolean mPendingCallInEcm;
    private GsmCdmaConnection mPendingMO;
    public GsmCdmaPhone mPhone;
    private RegistrantList mVoiceCallEndedRegistrants = new RegistrantList();
    private RegistrantList mVoiceCallStartedRegistrants = new RegistrantList();
    public RegistrantList mVoiceCallIncomingIndicationRegistrants = new RegistrantList();
    private ArrayList<GsmCdmaConnection> mDroppedDuringPoll = new ArrayList<>(19);
    public GsmCdmaCall mRingingCall = new GsmCdmaCall(this);
    public GsmCdmaCall mForegroundCall = new GsmCdmaCall(this);
    public GsmCdmaCall mBackgroundCall = new GsmCdmaCall(this);
    private boolean mDesiredMute = DBG_POLL;
    public PhoneConstants.State mState = PhoneConstants.State.IDLE;
    private RegistrantList mCallWaitingRegistrants = new RegistrantList();
    private CallTracker.RedialState mRedialState = CallTracker.RedialState.REDIAL_NONE;
    private boolean hasPendingReplaceRequest = DBG_POLL;
    boolean mHasPendingSwapRequest = DBG_POLL;
    WaitForHoldToRedial mWaitForHoldToRedialRequest = new WaitForHoldToRedial();
    WaitForHoldToHangup mWaitForHoldToHangupRequest = new WaitForHoldToHangup();
    private ArrayList<Connection> mImsConfParticipants = new ArrayList<>();
    private int[] mEconfSrvccConnectionIds = null;
    private BroadcastReceiver mEcmExitReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.getAction().equals("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED")) {
                return;
            }
            boolean isInEcm = intent.getBooleanExtra("phoneinECMState", GsmCdmaCallTracker.DBG_POLL);
            GsmCdmaCallTracker.this.log("Received ACTION_EMERGENCY_CALLBACK_MODE_CHANGED isInEcm = " + isInEcm);
            if (isInEcm) {
                return;
            }
            List<Connection> toNotify = new ArrayList<>();
            toNotify.addAll(GsmCdmaCallTracker.this.mRingingCall.getConnections());
            toNotify.addAll(GsmCdmaCallTracker.this.mForegroundCall.getConnections());
            toNotify.addAll(GsmCdmaCallTracker.this.mBackgroundCall.getConnections());
            if (GsmCdmaCallTracker.this.mPendingMO != null) {
                toNotify.add(GsmCdmaCallTracker.this.mPendingMO);
            }
            for (Connection connection : toNotify) {
                if (connection != null) {
                    connection.onExitedEcmMode();
                }
            }
        }
    };

    static {
        VDBG = SystemProperties.getInt("persist.log.tag.tel_dbg", 0) == 1;
    }

    class WaitForHoldToRedial {
        private boolean mWaitToRedial = GsmCdmaCallTracker.DBG_POLL;
        private String mDialString = null;
        private int mClirMode = 0;
        private UUSInfo mUUSInfo = null;

        WaitForHoldToRedial() {
            resetToRedial();
        }

        boolean isWaitToRedial() {
            return this.mWaitToRedial;
        }

        void setToRedial() {
            this.mWaitToRedial = true;
        }

        public void setToRedial(String dialSting, int clir, UUSInfo uusinfo) {
            this.mWaitToRedial = true;
            this.mDialString = dialSting;
            this.mClirMode = clir;
            this.mUUSInfo = uusinfo;
        }

        public void resetToRedial() {
            GsmCdmaCallTracker.this.proprietaryLog("Reset mWaitForHoldToRedialRequest variables");
            this.mWaitToRedial = GsmCdmaCallTracker.DBG_POLL;
            this.mDialString = null;
            this.mClirMode = 0;
            this.mUUSInfo = null;
        }

        private boolean resumeDialAfterHold() {
            GsmCdmaCallTracker.this.proprietaryLog("resumeDialAfterHold begin");
            if (this.mWaitToRedial) {
                if (PhoneNumberUtils.isEmergencyNumber(GsmCdmaCallTracker.this.mPhone.getSubId(), this.mDialString) && !PhoneNumberUtils.isSpecialEmergencyNumber(this.mDialString)) {
                    int serviceCategory = PhoneNumberUtils.getServiceCategoryFromEccBySubId(this.mDialString, GsmCdmaCallTracker.this.mPhone.getSubId());
                    GsmCdmaCallTracker.this.mCi.setEccServiceCategory(serviceCategory);
                    GsmCdmaCallTracker.this.mCi.emergencyDial(this.mDialString, this.mClirMode, this.mUUSInfo, GsmCdmaCallTracker.this.obtainCompleteMessage(1003));
                } else {
                    GsmCdmaCallTracker.this.mCi.dial(this.mDialString, this.mClirMode, this.mUUSInfo, GsmCdmaCallTracker.this.obtainCompleteMessage(1003));
                }
                resetToRedial();
                GsmCdmaCallTracker.this.proprietaryLog("resumeDialAfterHold end");
                return true;
            }
            return GsmCdmaCallTracker.DBG_POLL;
        }
    }

    class WaitForHoldToHangup {
        private boolean mWaitToHangup = GsmCdmaCallTracker.DBG_POLL;
        private boolean mHoldDone = GsmCdmaCallTracker.DBG_POLL;
        private GsmCdmaCall mCall = null;

        WaitForHoldToHangup() {
            resetToHangup();
        }

        boolean isWaitToHangup() {
            return this.mWaitToHangup;
        }

        boolean isHoldDone() {
            return this.mHoldDone;
        }

        void setHoldDone() {
            this.mHoldDone = true;
        }

        void setToHangup() {
            this.mWaitToHangup = true;
        }

        public void setToHangup(GsmCdmaCall call) {
            this.mWaitToHangup = true;
            this.mCall = call;
        }

        public void resetToHangup() {
            GsmCdmaCallTracker.this.proprietaryLog("Reset mWaitForHoldToHangupRequest variables");
            this.mWaitToHangup = GsmCdmaCallTracker.DBG_POLL;
            this.mHoldDone = GsmCdmaCallTracker.DBG_POLL;
            this.mCall = null;
        }

        private boolean resumeHangupAfterHold() {
            GsmCdmaCallTracker.this.proprietaryLog("resumeHangupAfterHold begin");
            if (this.mWaitToHangup && this.mCall != null) {
                GsmCdmaCallTracker.this.proprietaryLog("resumeHangupAfterHold to hangup call");
                this.mWaitToHangup = GsmCdmaCallTracker.DBG_POLL;
                this.mHoldDone = GsmCdmaCallTracker.DBG_POLL;
                try {
                    GsmCdmaCallTracker.this.hangup(this.mCall);
                } catch (CallStateException e) {
                    Rlog.e(GsmCdmaCallTracker.PROP_LOG_TAG, "unexpected error on hangup");
                }
                GsmCdmaCallTracker.this.proprietaryLog("resumeHangupAfterHold end");
                this.mCall = null;
                return true;
            }
            resetToHangup();
            return GsmCdmaCallTracker.DBG_POLL;
        }
    }

    public GsmCdmaCallTracker(GsmCdmaPhone phone) {
        this.mPhone = phone;
        this.mCi = phone.mCi;
        this.mCi.registerForCallStateChanged(this, 2, null);
        this.mCi.registerForOn(this, 9, null);
        this.mCi.registerForNotAvailable(this, 10, null);
        this.mCi.setOnIncomingCallIndication(this, 1000, null);
        this.mCi.registerForOffOrNotAvailable(this, 1001, null);
        this.mCi.registerForCallRedialState(this, 1006, null);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED");
        this.mPhone.getContext().registerReceiver(this.mEcmExitReceiver, filter);
        updatePhoneType(true);
        this.mEventLog = new TelephonyEventLog(this.mPhone.getPhoneId());
        this.mHelper = new GsmCallTrackerHelper(phone.getContext(), this);
        this.mCi.registerForEconfSrvcc(this, 1004, null);
        this.mCdmaCallTrackerExt = (ICdmaCallTrackerExt) MPlugin.createInstance(ICdmaCallTrackerExt.class.getName());
        proprietaryLog("mCdmaCallTrackerExt:" + this.mCdmaCallTrackerExt);
    }

    public void updatePhoneType() {
        updatePhoneType(DBG_POLL);
    }

    private void updatePhoneType(boolean duringInit) {
        if (!duringInit) {
            reset();
            if (this.mRedialState != CallTracker.RedialState.REDIAL_NONE) {
                pollCallsWhenSafe();
            }
        }
        if (this.mPhone.isPhoneTypeGsm()) {
            this.mConnections = new GsmCdmaConnection[19];
            this.mCi.unregisterForCallWaitingInfo(this);
            this.mCi.unregisterForCallAccepted(this);
            return;
        }
        this.mConnections = new GsmCdmaConnection[8];
        this.mPendingCallInEcm = DBG_POLL;
        this.mIsInEmergencyCall = DBG_POLL;
        this.mPendingCallClirMode = 0;
        this.mIsEcmTimerCanceled = DBG_POLL;
        this.m3WayCallFlashDelay = 0;
        this.mCi.registerForCallWaitingInfo(this, 15, null);
        this.mCi.registerForCallAccepted(this, 1005, null);
    }

    private void reset() {
        Rlog.d(LOG_TAG, "reset");
        if (this.mRedialState == CallTracker.RedialState.REDIAL_NONE) {
            handlePollCalls(new AsyncResult((Object) null, (Object) null, new CommandException(CommandException.Error.RADIO_NOT_AVAILABLE)));
        }
        clearDisconnected();
        for (GsmCdmaConnection gsmCdmaConnection : this.mConnections) {
            if (gsmCdmaConnection != null) {
                gsmCdmaConnection.dispose();
            }
        }
    }

    protected void finalize() {
        Rlog.d(LOG_TAG, "GsmCdmaCallTracker finalized");
    }

    @Override
    public void registerForVoiceCallStarted(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mVoiceCallStartedRegistrants.add(r);
        if (this.mState == PhoneConstants.State.IDLE) {
            return;
        }
        r.notifyRegistrant(new AsyncResult((Object) null, (Object) null, (Throwable) null));
    }

    @Override
    public void unregisterForVoiceCallStarted(Handler h) {
        this.mVoiceCallStartedRegistrants.remove(h);
    }

    @Override
    public void registerForVoiceCallEnded(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mVoiceCallEndedRegistrants.add(r);
    }

    @Override
    public void unregisterForVoiceCallEnded(Handler h) {
        this.mVoiceCallEndedRegistrants.remove(h);
    }

    public void registerForCallWaiting(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mCallWaitingRegistrants.add(r);
    }

    public void unregisterForCallWaiting(Handler h) {
        this.mCallWaitingRegistrants.remove(h);
    }

    private void fakeHoldForegroundBeforeDial() {
        List<Connection> connCopy = (List) this.mForegroundCall.mConnections.clone();
        int s = connCopy.size();
        for (int i = 0; i < s; i++) {
            GsmCdmaConnection conn = (GsmCdmaConnection) connCopy.get(i);
            conn.fakeHoldBeforeDial();
        }
    }

    public synchronized Connection dial(String dialString, int clirMode, UUSInfo uusInfo, Bundle intentExtras) throws CallStateException {
        clearDisconnected();
        if (!canDial()) {
            throw new CallStateException("cannot dial in current state");
        }
        String dialString2 = convertNumberIfNecessary(this.mPhone, dialString);
        if (this.mForegroundCall.getState() == Call.State.ACTIVE) {
            this.mWaitForHoldToRedialRequest.setToRedial();
            switchWaitingOrHoldingAndActive();
            try {
                Thread.sleep(500L);
            } catch (InterruptedException e) {
            }
            fakeHoldForegroundBeforeDial();
        }
        if (this.mForegroundCall.getState() != Call.State.IDLE) {
            throw new CallStateException("cannot dial in current state");
        }
        this.mPendingMO = new GsmCdmaConnection(this.mPhone, checkForTestEmergencyNumber(dialString2), this, this.mForegroundCall);
        this.mHangupPendingMO = DBG_POLL;
        if (this.mPendingMO.getAddress() == null || this.mPendingMO.getAddress().length() == 0 || this.mPendingMO.getAddress().indexOf(78) >= 0) {
            this.mPendingMO.mCause = 7;
            this.mWaitForHoldToRedialRequest.resetToRedial();
            pollCallsWhenSafe();
        } else {
            setMute(DBG_POLL);
            if (!this.mWaitForHoldToRedialRequest.isWaitToRedial()) {
                if (PhoneNumberUtils.isEmergencyNumber(this.mPhone.getSubId(), dialString2) && !PhoneNumberUtils.isSpecialEmergencyNumber(dialString2)) {
                    int serviceCategory = PhoneNumberUtils.getServiceCategoryFromEccBySubId(dialString2, this.mPhone.getSubId());
                    this.mCi.setEccServiceCategory(serviceCategory);
                    this.mCi.emergencyDial(this.mPendingMO.getAddress(), clirMode, uusInfo, obtainCompleteMessage(1003));
                } else {
                    this.mCi.dial(this.mPendingMO.getAddress(), clirMode, uusInfo, obtainCompleteMessage(1003));
                }
            } else {
                this.mWaitForHoldToRedialRequest.setToRedial(this.mPendingMO.getAddress(), clirMode, uusInfo);
            }
        }
        if (this.mNumberConverted) {
            this.mPendingMO.setConverted(dialString);
            this.mNumberConverted = DBG_POLL;
        }
        updatePhoneState();
        this.mPhone.notifyPreciseCallStateChanged();
        return this.mPendingMO;
    }

    private void handleEcmTimer(int action) {
        this.mPhone.handleTimerInEmergencyCallbackMode(action);
        switch (action) {
            case 0:
                this.mIsEcmTimerCanceled = DBG_POLL;
                break;
            case 1:
                this.mIsEcmTimerCanceled = true;
                break;
            default:
                Rlog.e(LOG_TAG, "handleEcmTimer, unsupported action " + action);
                break;
        }
    }

    private void disableDataCallInEmergencyCall(String dialString) {
        if (!PhoneNumberUtils.isLocalEmergencyNumber(this.mPhone.getContext(), this.mPhone.getSubId(), dialString)) {
            return;
        }
        log("disableDataCallInEmergencyCall");
        setIsInEmergencyCall();
    }

    public void setIsInEmergencyCall() {
        this.mIsInEmergencyCall = true;
        this.mPhone.mDcTracker.setInternalDataEnabled(DBG_POLL);
        this.mPhone.notifyEmergencyCallRegistrants(true);
        this.mPhone.sendEmergencyCallStateChange(true);
    }

    private Connection dial(String dialString, int clirMode) throws CallStateException {
        boolean internationalRoaming;
        clearDisconnected();
        if (!canDial()) {
            throw new CallStateException("cannot dial in current state");
        }
        TelephonyManager tm = (TelephonyManager) this.mPhone.getContext().getSystemService("phone");
        String operatorIsoContry = tm.getNetworkCountryIsoForPhone(this.mPhone.getPhoneId());
        String simIsoContry = tm.getSimCountryIsoForPhone(this.mPhone.getPhoneId());
        if (TextUtils.isEmpty(operatorIsoContry) || TextUtils.isEmpty(simIsoContry)) {
            internationalRoaming = DBG_POLL;
        } else {
            internationalRoaming = simIsoContry.equals(operatorIsoContry) ? DBG_POLL : true;
        }
        if (internationalRoaming) {
            if ("us".equals(simIsoContry)) {
                internationalRoaming = (!internationalRoaming || "vi".equals(operatorIsoContry)) ? DBG_POLL : true;
            } else if ("vi".equals(simIsoContry)) {
                internationalRoaming = (!internationalRoaming || "us".equals(operatorIsoContry)) ? DBG_POLL : true;
            }
        }
        if (internationalRoaming) {
            dialString = convertNumberIfNecessary(this.mPhone, dialString);
        }
        String inEcm = this.mPhone.getSystemProperty("ril.cdma.inecmmode", "false");
        boolean isPhoneInEcmMode = inEcm.equals("true");
        boolean isEmergencyCall = PhoneNumberUtils.isLocalEmergencyNumber(this.mPhone.getContext(), this.mPhone.getSubId(), dialString);
        if (isPhoneInEcmMode && isEmergencyCall) {
            handleEcmTimer(1);
        }
        if (this.mForegroundCall.getState() == Call.State.ACTIVE) {
            return dialThreeWay(dialString);
        }
        this.mPendingMO = new GsmCdmaConnection(this.mPhone, checkForTestEmergencyNumber(dialString), this, this.mForegroundCall);
        this.mHangupPendingMO = DBG_POLL;
        GsmCdmaConnection result = this.mPendingMO;
        if (this.mPendingMO.getAddress() == null || this.mPendingMO.getAddress().length() == 0 || this.mPendingMO.getAddress().indexOf(78) >= 0) {
            this.mPendingMO.mCause = 7;
            pollCallsWhenSafe();
        } else {
            setMute(DBG_POLL);
            disableDataCallInEmergencyCall(dialString);
            if (!isPhoneInEcmMode || (isPhoneInEcmMode && isEmergencyCall)) {
                String tmpStr = this.mPendingMO.getAddress() + "," + PhoneNumberUtils.extractNetworkPortionAlt(dialString);
                if (isEmergencyCall) {
                    this.mCi.emergencyDial(tmpStr, clirMode, null, obtainCompleteMessage());
                } else {
                    this.mCi.dial(tmpStr, clirMode, obtainCompleteMessage());
                }
                if (this.mCdmaCallTrackerExt != null) {
                    if (this.mCdmaCallTrackerExt.needToConvert(dialString, GsmCdmaConnection.formatDialString(dialString))) {
                        result.setConverted(PhoneNumberUtils.extractNetworkPortionAlt(dialString));
                    }
                }
            } else {
                this.mPhone.exitEmergencyCallbackMode();
                this.mPhone.setOnEcbModeExitResponse(this, 14, dialString);
                this.mPendingCallClirMode = clirMode;
                this.mPendingCallInEcm = true;
            }
        }
        if (this.mNumberConverted) {
            result.setConverted(dialString);
            this.mNumberConverted = DBG_POLL;
        }
        updatePhoneState();
        this.mPhone.notifyPreciseCallStateChanged();
        return result;
    }

    private Connection dialThreeWay(String dialString) {
        if (this.mForegroundCall.isIdle()) {
            return null;
        }
        disableDataCallInEmergencyCall(dialString);
        this.mPendingMO = new GsmCdmaConnection(this.mPhone, checkForTestEmergencyNumber(dialString), this, this.mForegroundCall);
        this.m3WayCallFlashDelay = this.mPhone.getContext().getResources().getInteger(R.integer.config_dreamCloseAnimationDuration);
        if (this.m3WayCallFlashDelay > 0) {
            this.mCi.sendCDMAFeatureCode(UsimPBMemInfo.STRING_NOT_SET, obtainMessage(20, dialString));
        } else {
            String tmpStr = this.mPendingMO.getAddress() + "," + PhoneNumberUtils.extractNetworkPortionAlt(dialString);
            this.mCi.sendCDMAFeatureCode(tmpStr, obtainMessage(16));
            if (this.mCdmaCallTrackerExt != null && this.mCdmaCallTrackerExt.needToConvert(dialString, GsmCdmaConnection.formatDialString(dialString))) {
                this.mPendingMO.setConverted(PhoneNumberUtils.extractNetworkPortionAlt(dialString));
            }
        }
        return this.mPendingMO;
    }

    public Connection dial(String dialString) throws CallStateException {
        if (isPhoneTypeGsm()) {
            return dial(dialString, 0, (Bundle) null);
        }
        return dial(dialString, 0);
    }

    public Connection dial(String dialString, UUSInfo uusInfo, Bundle intentExtras) throws CallStateException {
        return dial(dialString, 0, uusInfo, intentExtras);
    }

    private Connection dial(String dialString, int clirMode, Bundle intentExtras) throws CallStateException {
        return dial(dialString, clirMode, null, intentExtras);
    }

    public void acceptCall() throws CallStateException {
        if (this.mRingingCall.getState() == Call.State.INCOMING) {
            Rlog.i("phone", "acceptCall: incoming...");
            setMute(DBG_POLL);
            this.mCi.acceptCall(obtainCompleteMessage());
        } else {
            if (this.mRingingCall.getState() == Call.State.WAITING) {
                if (isPhoneTypeGsm()) {
                    setMute(DBG_POLL);
                } else {
                    GsmCdmaConnection cwConn = (GsmCdmaConnection) this.mRingingCall.getLatestConnection();
                    cwConn.updateParent(this.mRingingCall, this.mForegroundCall);
                    cwConn.onConnectedInOrOut();
                    updatePhoneState();
                }
                switchWaitingOrHoldingAndActive();
                return;
            }
            throw new CallStateException("phone not ringing");
        }
    }

    public void rejectCall() throws CallStateException {
        if (this.mRingingCall.getState().isRinging()) {
            this.mCi.rejectCall(obtainCompleteMessage());
            return;
        }
        throw new CallStateException("phone not ringing");
    }

    private void flashAndSetGenericTrue() {
        this.mCi.sendCDMAFeatureCode(UsimPBMemInfo.STRING_NOT_SET, obtainMessage(8));
        this.mPhone.notifyPreciseCallStateChanged();
    }

    public void switchWaitingOrHoldingAndActive() throws CallStateException {
        if (this.mRingingCall.getState() == Call.State.INCOMING) {
            throw new CallStateException("cannot be in the incoming state");
        }
        if (isPhoneTypeGsm()) {
            if (this.mHasPendingSwapRequest) {
                return;
            }
            this.mWaitForHoldToHangupRequest.setToHangup();
            this.mCi.switchWaitingOrHoldingAndActive(obtainCompleteMessage(8));
            this.mHasPendingSwapRequest = true;
            return;
        }
        if (this.mForegroundCall.getConnections().size() > 1) {
            flashAndSetGenericTrue();
        } else {
            this.mCi.sendCDMAFeatureCode(UsimPBMemInfo.STRING_NOT_SET, obtainMessage(8));
        }
    }

    public void conference() {
        if (isPhoneTypeGsm()) {
            this.mCi.conference(obtainCompleteMessage(11));
        } else {
            flashAndSetGenericTrue();
        }
    }

    public void explicitCallTransfer() {
        this.mCi.explicitCallTransfer(obtainCompleteMessage(13));
    }

    public void clearDisconnected() {
        internalClearDisconnected();
        updatePhoneState();
        this.mPhone.notifyPreciseCallStateChanged();
    }

    public boolean canConference() {
        if (this.mForegroundCall.getState() != Call.State.ACTIVE || this.mBackgroundCall.getState() != Call.State.HOLDING || this.mBackgroundCall.isFull() || this.mForegroundCall.isFull()) {
            return DBG_POLL;
        }
        return true;
    }

    private boolean canDial() {
        boolean ret;
        boolean z;
        boolean z2 = DBG_POLL;
        int serviceState = this.mPhone.getServiceState().getState();
        String disableCall = SystemProperties.get("ro.telephony.disable-call", "false");
        if (serviceState == 3 || this.mPendingMO != null || this.mRingingCall.isRinging() || disableCall.equals("true")) {
            ret = false;
        } else {
            if (this.mForegroundCall.getState().isAlive() && this.mBackgroundCall.getState().isAlive()) {
                z = !isPhoneTypeGsm() && this.mForegroundCall.getState() == Call.State.ACTIVE;
            } else {
                z = true;
            }
            ret = z;
        }
        if (!ret) {
            Object[] objArr = new Object[8];
            objArr[0] = Integer.valueOf(serviceState);
            objArr[1] = Boolean.valueOf(serviceState != 3);
            objArr[2] = Boolean.valueOf(this.mPendingMO == null);
            objArr[3] = Boolean.valueOf(!this.mRingingCall.isRinging());
            objArr[4] = Boolean.valueOf(!disableCall.equals("true"));
            objArr[5] = Boolean.valueOf(!this.mForegroundCall.getState().isAlive());
            objArr[6] = Boolean.valueOf(this.mForegroundCall.getState() == Call.State.ACTIVE);
            if (!this.mBackgroundCall.getState().isAlive()) {
                z2 = true;
            }
            objArr[7] = Boolean.valueOf(z2);
            log(String.format("canDial is false\n((serviceState=%d) != ServiceState.STATE_POWER_OFF)::=%s\n&& pendingMO == null::=%s\n&& !ringingCall.isRinging()::=%s\n&& !disableCall.equals(\"true\")::=%s\n&& (!foregroundCall.getState().isAlive()::=%s\n   || foregroundCall.getState() == GsmCdmaCall.State.ACTIVE::=%s\n   ||!backgroundCall.getState().isAlive())::=%s)", objArr));
        }
        return ret;
    }

    public boolean canTransfer() {
        if (isPhoneTypeGsm()) {
            if ((this.mForegroundCall.getState() == Call.State.ACTIVE || this.mForegroundCall.getState() == Call.State.ALERTING || this.mForegroundCall.getState() == Call.State.DIALING) && this.mBackgroundCall.getState() == Call.State.HOLDING) {
                return true;
            }
            return DBG_POLL;
        }
        Rlog.e(LOG_TAG, "canTransfer: not possible in CDMA");
        return DBG_POLL;
    }

    private void internalClearDisconnected() {
        this.mRingingCall.clearDisconnected();
        this.mForegroundCall.clearDisconnected();
        this.mBackgroundCall.clearDisconnected();
    }

    public Message obtainCompleteMessage() {
        return obtainCompleteMessage(4);
    }

    public Message obtainCompleteMessage(int what) {
        this.mPendingOperations++;
        this.mLastRelevantPoll = null;
        this.mNeedsPoll = true;
        return obtainMessage(what);
    }

    private void operationComplete() {
        this.mPendingOperations--;
        if (this.mPendingOperations == 0 && this.mNeedsPoll) {
            this.mLastRelevantPoll = obtainMessage(1);
            this.mCi.getCurrentCalls(this.mLastRelevantPoll);
        } else {
            if (this.mPendingOperations >= 0) {
                return;
            }
            Rlog.e(LOG_TAG, "GsmCdmaCallTracker.pendingOperations < 0");
            this.mPendingOperations = 0;
        }
    }

    private void updatePhoneState() {
        PhoneConstants.State oldState = this.mState;
        if (this.mRingingCall.isRinging()) {
            this.mState = PhoneConstants.State.RINGING;
        } else if (this.mPendingMO != null || !this.mForegroundCall.isIdle() || !this.mBackgroundCall.isIdle()) {
            this.mState = PhoneConstants.State.OFFHOOK;
        } else {
            Phone imsPhone = this.mPhone.getImsPhone();
            if (imsPhone != null) {
                imsPhone.callEndCleanupHandOverCallIfAny();
            }
            this.mState = PhoneConstants.State.IDLE;
        }
        if (this.mState == PhoneConstants.State.IDLE && oldState != this.mState) {
            this.mVoiceCallEndedRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        } else if (oldState == PhoneConstants.State.IDLE && oldState != this.mState) {
            this.mVoiceCallStartedRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        }
        log("update phone state, old=" + oldState + " new=" + this.mState);
        if (this.mState == oldState) {
            return;
        }
        this.mPhone.notifyPhoneStateChanged();
        this.mEventLog.writePhoneState(this.mState);
    }

    @Override
    protected synchronized void handlePollCalls(AsyncResult ar) {
        List polledCalls;
        Connection newRinging;
        ArrayList<Connection> newUnknownConnectionsGsm;
        Connection newUnknownConnectionCdma;
        boolean hasNonHangupStateChanged;
        boolean hasAnyCallDisconnected;
        boolean unknownConnectionAppeared;
        int cause;
        if (VDBG) {
            log("handlePollCalls");
        }
        if (ar.exception == null) {
            polledCalls = (List) ar.result;
        } else if (isCommandExceptionRadioNotAvailable(ar.exception)) {
            polledCalls = new ArrayList();
        } else if (this.mNeedWaitImsEConfSrvcc) {
            proprietaryLog("SRVCC: +ECONFSRVCC is still not arrival, skip this poll call.");
            return;
        } else {
            pollCallsAfterDelay();
            return;
        }
        newRinging = null;
        newUnknownConnectionsGsm = new ArrayList<>();
        newUnknownConnectionCdma = null;
        hasNonHangupStateChanged = DBG_POLL;
        hasAnyCallDisconnected = DBG_POLL;
        unknownConnectionAppeared = DBG_POLL;
        boolean noConnectionExists = true;
        int i = 0;
        int curDC = 0;
        int dcSize = polledCalls.size();
        while (true) {
            if (i >= this.mConnections.length) {
                break;
            }
            GsmCdmaConnection conn = this.mConnections[i];
            DriverCall dc = null;
            if (curDC < dcSize) {
                dc = (DriverCall) polledCalls.get(curDC);
                if (!isPhoneTypeGsm() && this.mCdmaCallTrackerExt != null) {
                    dc.number = this.mCdmaCallTrackerExt.processPlusCodeForDriverCall(dc.number, dc.isMT, dc.TOA);
                }
                if (dc.index == i + 1) {
                    curDC++;
                } else {
                    dc = null;
                }
            }
            if (conn != null || dc != null) {
                noConnectionExists = DBG_POLL;
            }
            if (conn == null && dc != null) {
                if (this.mPendingMO != null && this.mPendingMO.compareTo(dc)) {
                    if (SystemProperties.get("ro.mtk_vt3g324m_support").equals("1") && this.mForegroundCall.mVTProvider != null && dc.isVideo) {
                        this.mForegroundCall.mVTProvider.setId(i + 1);
                    }
                    this.mConnections[i] = this.mPendingMO;
                    this.mPendingMO.mIndex = i;
                    this.mPendingMO.update(dc);
                    this.mPendingMO = null;
                    if (this.mHangupPendingMO) {
                        break;
                    }
                } else {
                    log("pendingMo=" + this.mPendingMO + ", dc=" + dc);
                    if (this.mPendingMO != null && !this.mPendingMO.compareTo(dc)) {
                        proprietaryLog("MO/MT conflict! MO should be hangup by MD");
                    }
                    this.mConnections[i] = new GsmCdmaConnection(this.mPhone, dc, this, i);
                    if (isPhoneTypeGsm()) {
                        this.mHelper.setForwardingAddressToConnection(i, this.mConnections[i]);
                    }
                    Connection hoConnection = getHoConnection(dc);
                    if (hoConnection != null) {
                        if (hoConnection.isMultipartyBeforeHandover() && hoConnection.isConfHostBeforeHandover()) {
                            Rlog.i(LOG_TAG, "SRVCC: goes to conference case.");
                            this.mConnections[i].mOrigConnection = hoConnection;
                            this.mImsConfParticipants.add(this.mConnections[i]);
                        } else {
                            Rlog.i(LOG_TAG, "SRVCC: goes to normal call case.");
                            this.mConnections[i].migrateFrom(hoConnection);
                            if (hoConnection.mPreHandoverState != Call.State.ACTIVE && hoConnection.mPreHandoverState != Call.State.HOLDING && dc.state == DriverCall.State.ACTIVE) {
                                this.mConnections[i].onConnectedInOrOut();
                            }
                            this.mHandoverConnections.remove(hoConnection);
                            if (isPhoneTypeGsm()) {
                                Iterator<Connection> it = this.mHandoverConnections.iterator();
                                while (it.hasNext()) {
                                    Connection c = it.next();
                                    Rlog.i(LOG_TAG, "HO Conn state is " + c.mPreHandoverState);
                                    if (c.mPreHandoverState == this.mConnections[i].getState()) {
                                        Rlog.i(LOG_TAG, "Removing HO conn " + hoConnection + c.mPreHandoverState);
                                        it.remove();
                                    }
                                }
                            }
                            this.mPhone.notifyHandoverStateChanged(this.mConnections[i]);
                        }
                    } else {
                        newRinging = checkMtFindNewRinging(dc, i);
                        if (newRinging == null) {
                            unknownConnectionAppeared = true;
                            if (isPhoneTypeGsm()) {
                                newUnknownConnectionsGsm.add(this.mConnections[i]);
                            } else {
                                newUnknownConnectionCdma = this.mConnections[i];
                            }
                        }
                    }
                }
                hasNonHangupStateChanged = true;
            } else if (conn != null && dc == null) {
                if (isPhoneTypeGsm()) {
                    if (((conn.getCall() == this.mForegroundCall && this.mForegroundCall.mConnections.size() == 1 && this.mBackgroundCall.isIdle()) || (conn.getCall() == this.mBackgroundCall && this.mBackgroundCall.mConnections.size() == 1 && this.mForegroundCall.isIdle())) && this.mRingingCall.getState() == Call.State.WAITING) {
                        this.mRingingCall.mState = Call.State.INCOMING;
                    }
                    this.mDroppedDuringPoll.add(conn);
                    this.mConnections[i] = null;
                    this.mHelper.CallIndicationEnd();
                    this.mHelper.clearForwardingAddressVariables(i);
                } else {
                    int count = this.mForegroundCall.mConnections.size();
                    for (int n = 0; n < count; n++) {
                        log("adding fgCall cn " + n + " to droppedDuringPoll");
                        GsmCdmaConnection cn = (GsmCdmaConnection) this.mForegroundCall.mConnections.get(n);
                        this.mDroppedDuringPoll.add(cn);
                    }
                    int count2 = this.mRingingCall.mConnections.size();
                    for (int n2 = 0; n2 < count2; n2++) {
                        log("adding rgCall cn " + n2 + " to droppedDuringPoll");
                        GsmCdmaConnection cn2 = (GsmCdmaConnection) this.mRingingCall.mConnections.get(n2);
                        this.mDroppedDuringPoll.add(cn2);
                    }
                    if (this.mIsEcmTimerCanceled) {
                        handleEcmTimer(0);
                    }
                    checkAndEnableDataCallAfterEmergencyCallDropped();
                    this.mConnections[i] = null;
                }
            } else if (conn != null && dc != null && !conn.compareTo(dc) && isPhoneTypeGsm()) {
                this.mDroppedDuringPoll.add(conn);
                if (this.mPendingMO != null && this.mPendingMO.compareTo(dc)) {
                    proprietaryLog("ringing disc not updated yet & replaced by pendingMo");
                    if (SystemProperties.get("ro.mtk_vt3g324m_support").equals("1") && this.mForegroundCall.mVTProvider != null && dc.isVideo) {
                        this.mForegroundCall.mVTProvider.setId(i + 1);
                    }
                    this.mConnections[i] = this.mPendingMO;
                    this.mPendingMO.mIndex = i;
                    this.mPendingMO.update(dc);
                    this.mPendingMO = null;
                } else {
                    this.mConnections[i] = new GsmCdmaConnection(this.mPhone, dc, this, i);
                }
                if (this.mConnections[i].getCall() == this.mRingingCall) {
                    newRinging = this.mConnections[i];
                }
                hasNonHangupStateChanged = true;
            } else if (conn != null && dc != null) {
                if (!isPhoneTypeGsm() && conn.isIncoming() != dc.isMT) {
                    if (dc.isMT) {
                        this.mConnections[i] = new GsmCdmaConnection(this.mPhone, dc, this, i);
                        this.mDroppedDuringPoll.add(conn);
                        newRinging = checkMtFindNewRinging(dc, i);
                        if (newRinging == null) {
                            unknownConnectionAppeared = true;
                            newUnknownConnectionCdma = conn;
                        }
                        checkAndEnableDataCallAfterEmergencyCallDropped();
                    } else {
                        Rlog.e(LOG_TAG, "Error in RIL, Phantom call appeared " + dc);
                    }
                } else {
                    boolean changed = conn.update(dc);
                    hasNonHangupStateChanged = !hasNonHangupStateChanged ? changed : true;
                }
            }
            i++;
        }
        for (int i2 = this.mDroppedDuringPoll.size() - 1; i2 >= 0; i2--) {
            GsmCdmaConnection conn2 = this.mDroppedDuringPoll.get(i2);
            boolean wasDisconnected = DBG_POLL;
            if (isCommandExceptionRadioNotAvailable(ar.exception)) {
                this.mDroppedDuringPoll.remove(i2);
                hasAnyCallDisconnected |= conn2.onDisconnect(14);
                wasDisconnected = true;
            } else if (conn2.isIncoming() && conn2.getConnectTime() == 0) {
                if (conn2.mCause == 3) {
                    cause = 16;
                } else {
                    cause = 1;
                }
                log("missed/rejected call, conn.cause=" + conn2.mCause);
                log("setting cause to " + cause);
                this.mDroppedDuringPoll.remove(i2);
                hasAnyCallDisconnected |= conn2.onDisconnect(cause);
                wasDisconnected = true;
            } else if (conn2.mCause == 3 || conn2.mCause == 7) {
                this.mDroppedDuringPoll.remove(i2);
                hasAnyCallDisconnected |= conn2.onDisconnect(conn2.mCause);
                wasDisconnected = true;
            }
            if (!isPhoneTypeGsm() && wasDisconnected && unknownConnectionAppeared && conn2 == newUnknownConnectionCdma) {
                unknownConnectionAppeared = DBG_POLL;
                newUnknownConnectionCdma = null;
            }
        }
        if (this.mImsConfHostConnection != null) {
            ImsPhoneConnection hostConn = (ImsPhoneConnection) this.mImsConfHostConnection;
            if (this.mImsConfParticipants.size() >= 2) {
                restoreConferenceParticipantAddress();
                proprietaryLog("SRVCC: notify new participant connections");
                hostConn.notifyConferenceConnectionsConfigured(this.mImsConfParticipants);
            } else if (this.mImsConfParticipants.size() == 1) {
                GsmCdmaConnection participant = (GsmCdmaConnection) this.mImsConfParticipants.get(0);
                String address = hostConn.getConferenceParticipantAddress(0);
                proprietaryLog("SRVCC: restore participant connection with address: " + address);
                participant.updateConferenceParticipantAddress(address);
                proprietaryLog("SRVCC: only one connection, consider it as a normal call SRVCC");
                this.mPhone.notifyHandoverStateChanged(participant);
            } else {
                Rlog.e(PROP_LOG_TAG, "SRVCC: abnormal case, no participant connections.");
            }
            this.mImsConfParticipants.clear();
            this.mImsConfHostConnection = null;
            this.mEconfSrvccConnectionIds = null;
        }
        Iterator<Connection> it2 = this.mHandoverConnections.iterator();
        while (it2.hasNext()) {
            Connection hoConnection2 = it2.next();
            log("handlePollCalls - disconnect hoConn= " + hoConnection2 + " hoConn.State= " + hoConnection2.getState());
            if (hoConnection2.getState().isRinging()) {
                hoConnection2.onDisconnect(1);
            } else {
                hoConnection2.onDisconnect(-1);
            }
            it2.remove();
        }
        if (this.mDroppedDuringPoll.size() > 0 && !this.hasPendingReplaceRequest) {
            this.mCi.getLastCallFailCause(obtainNoPollCompleteMessage(5));
        }
        if (0 != 0) {
            pollCallsAfterDelay();
        }
        if ((newRinging != null || hasNonHangupStateChanged || hasAnyCallDisconnected) && !this.mHasPendingSwapRequest) {
            internalClearDisconnected();
        }
        if (VDBG) {
            log("handlePollCalls calling updatePhoneState()");
        }
        updatePhoneState();
        if (unknownConnectionAppeared) {
            if (isPhoneTypeGsm()) {
                for (Connection c2 : newUnknownConnectionsGsm) {
                    log("Notify unknown for " + c2);
                    this.mPhone.notifyUnknownConnection(c2);
                }
            } else {
                this.mPhone.notifyUnknownConnection(newUnknownConnectionCdma);
            }
        }
        if (hasNonHangupStateChanged || newRinging != null || hasAnyCallDisconnected) {
            this.mPhone.notifyPreciseCallStateChanged();
        }
        if (isPhoneTypeGsm() && this.mConnections != null && this.mConnections.length == 19 && this.mHelper.getCurrentTotalConnections() == 1 && this.mRingingCall.getState() == Call.State.WAITING) {
            this.mRingingCall.mState = Call.State.INCOMING;
        }
    }

    private void handleRadioNotAvailable() {
        pollCallsWhenSafe();
    }

    private void dumpState() {
        Rlog.i(LOG_TAG, "Phone State:" + this.mState);
        Rlog.i(LOG_TAG, "Ringing call: " + this.mRingingCall.toString());
        List<Connection> connections = this.mRingingCall.getConnections();
        int s = connections.size();
        for (int i = 0; i < s; i++) {
            Rlog.i(LOG_TAG, connections.get(i).toString());
        }
        Rlog.i(LOG_TAG, "Foreground call: " + this.mForegroundCall.toString());
        List<Connection> connections2 = this.mForegroundCall.getConnections();
        int s2 = connections2.size();
        for (int i2 = 0; i2 < s2; i2++) {
            Rlog.i(LOG_TAG, connections2.get(i2).toString());
        }
        Rlog.i(LOG_TAG, "Background call: " + this.mBackgroundCall.toString());
        List<Connection> connections3 = this.mBackgroundCall.getConnections();
        int s3 = connections3.size();
        for (int i3 = 0; i3 < s3; i3++) {
            Rlog.i(LOG_TAG, connections3.get(i3).toString());
        }
        if (!isPhoneTypeGsm()) {
            return;
        }
        this.mHelper.LogState();
    }

    public void hangup(GsmCdmaConnection conn) throws CallStateException {
        if (conn.mOwner != this) {
            throw new CallStateException("GsmCdmaConnection " + conn + "does not belong to GsmCdmaCallTracker " + this);
        }
        if (conn == this.mPendingMO) {
            log("hangup: set hangupPendingMO to true");
            this.mHangupPendingMO = true;
            this.mHelper.PendingHangupRequestReset();
        } else {
            if (!isPhoneTypeGsm() && conn.getCall() == this.mRingingCall && this.mRingingCall.getState() == Call.State.WAITING) {
                proprietaryLog("hangup waiting call");
                conn.onLocalDisconnect();
                updatePhoneState();
                this.mPhone.notifyPreciseCallStateChanged();
                return;
            }
            try {
                this.mCi.hangupConnection(conn.getGsmCdmaIndex(), obtainCompleteMessage(1002));
            } catch (CallStateException e) {
                this.mHelper.PendingHangupRequestReset();
                Rlog.w(LOG_TAG, "GsmCdmaCallTracker WARN: hangup() on absent connection " + conn);
            }
        }
        conn.onHangupLocal();
    }

    public void separate(GsmCdmaConnection conn) throws CallStateException {
        if (conn.mOwner != this) {
            throw new CallStateException("GsmCdmaConnection " + conn + "does not belong to GsmCdmaCallTracker " + this);
        }
        try {
            this.mCi.separateConnection(conn.getGsmCdmaIndex(), obtainCompleteMessage(12));
        } catch (CallStateException e) {
            Rlog.w(LOG_TAG, "GsmCdmaCallTracker WARN: separate() on absent connection " + conn);
        }
    }

    public void setMute(boolean mute) {
        this.mDesiredMute = mute;
        this.mCi.setMute(this.mDesiredMute, null);
    }

    public boolean getMute() {
        return this.mDesiredMute;
    }

    public void hangup(GsmCdmaCall call) throws CallStateException {
        if (call.getConnections().size() == 0) {
            throw new CallStateException("no connections in call");
        }
        if (this.mHelper.hasPendingHangupRequest()) {
            proprietaryLog("hangup(GsmCdmaCall) hasPendingHangupRequest = true");
            if (this.mHelper.ForceReleaseAllConnection(call)) {
                return;
            }
        }
        if (call == this.mRingingCall) {
            log("(ringing) hangup waiting or background");
            this.mHelper.PendingHangupRequestInc();
            hangup((GsmCdmaConnection) call.getConnections().get(0));
        } else if (call == this.mForegroundCall) {
            this.mHelper.PendingHangupRequestInc();
            if (call.isDialingOrAlerting()) {
                log("(foregnd) hangup dialing or alerting...");
                hangup((GsmCdmaConnection) call.getConnections().get(0));
            } else {
                log("(foregnd) hangup active");
                if (isPhoneTypeGsm()) {
                    GsmCdmaConnection cn = (GsmCdmaConnection) call.getConnections().get(0);
                    String address = cn.getAddress();
                    if (PhoneNumberUtils.isEmergencyNumber(this.mPhone.getSubId(), address) && !PhoneNumberUtils.isSpecialEmergencyNumber(address)) {
                        proprietaryLog("(foregnd) hangup active ECC call by connection index");
                        hangup((GsmCdmaConnection) call.getConnections().get(0));
                    } else if (!this.mWaitForHoldToHangupRequest.isWaitToHangup()) {
                        hangupForegroundResumeBackground();
                    } else {
                        this.mWaitForHoldToHangupRequest.setToHangup(call);
                    }
                } else {
                    hangupForegroundResumeBackground();
                }
            }
        } else if (call == this.mBackgroundCall) {
            if (this.mRingingCall.isRinging()) {
                log("hangup all conns in background call");
                hangupAllConnections(call);
            } else {
                this.mHelper.PendingHangupRequestInc();
                log("(backgnd) hangup waiting/background");
                if (!this.mWaitForHoldToHangupRequest.isWaitToHangup()) {
                    hangupWaitingOrBackground();
                } else {
                    this.mWaitForHoldToHangupRequest.setToHangup(call);
                }
            }
        } else {
            throw new RuntimeException("GsmCdmaCall " + call + "does not belong to GsmCdmaCallTracker " + this);
        }
        call.onHangupLocal();
        this.mPhone.notifyPreciseCallStateChanged();
    }

    public void hangupWaitingOrBackground() {
        log("hangupWaitingOrBackground");
        this.mCi.hangupWaitingOrBackground(obtainCompleteMessage(1002));
    }

    public void hangupForegroundResumeBackground() {
        log("hangupForegroundResumeBackground");
        this.mCi.hangupForegroundResumeBackground(obtainCompleteMessage(1002));
    }

    public void hangupConnectionByIndex(GsmCdmaCall call, int index) throws CallStateException {
        int count = call.mConnections.size();
        for (int i = 0; i < count; i++) {
            GsmCdmaConnection cn = (GsmCdmaConnection) call.mConnections.get(i);
            if (cn.getState() == Call.State.DISCONNECTED) {
                proprietaryLog("hangupConnectionByIndex: hangup a DISCONNECTED conn");
            } else if (cn.getGsmCdmaIndex() == index) {
                this.mCi.hangupConnection(index, obtainCompleteMessage());
                return;
            }
        }
        if (!this.mHelper.hangupBgConnectionByIndex(index) && !this.mHelper.hangupRingingConnectionByIndex(index)) {
            throw new CallStateException("no GsmCdma index found");
        }
    }

    public void hangupAllConnections(GsmCdmaCall call) {
        try {
            int count = call.mConnections.size();
            for (int i = 0; i < count; i++) {
                GsmCdmaConnection cn = (GsmCdmaConnection) call.mConnections.get(i);
                this.mCi.hangupConnection(cn.getGsmCdmaIndex(), obtainCompleteMessage());
            }
        } catch (CallStateException ex) {
            Rlog.e(LOG_TAG, "hangupConnectionByIndex caught " + ex);
        }
    }

    public GsmCdmaConnection getConnectionByIndex(GsmCdmaCall call, int index) throws CallStateException {
        int count = call.mConnections.size();
        for (int i = 0; i < count; i++) {
            GsmCdmaConnection cn = (GsmCdmaConnection) call.mConnections.get(i);
            if (cn.getGsmCdmaIndex() == index) {
                return cn;
            }
        }
        return null;
    }

    private void notifyCallWaitingInfo(CdmaCallWaitingNotification obj) {
        if (this.mCallWaitingRegistrants == null) {
            return;
        }
        this.mCallWaitingRegistrants.notifyRegistrants(new AsyncResult((Object) null, obj, (Throwable) null));
    }

    private void handleCallWaitingInfo(CdmaCallWaitingNotification cw) {
        processPlusCodeForWaitingCall(cw);
        if (!shouldNotifyWaitingCall(cw)) {
            return;
        }
        new GsmCdmaConnection(this.mPhone.getContext(), cw, this, this.mRingingCall);
        updatePhoneState();
        notifyCallWaitingInfo(cw);
    }

    private PhoneInternalInterface.SuppService getFailedService(int what) {
        switch (what) {
            case 8:
                return PhoneInternalInterface.SuppService.SWITCH;
            case 9:
            case 10:
            default:
                return PhoneInternalInterface.SuppService.UNKNOWN;
            case 11:
                return PhoneInternalInterface.SuppService.CONFERENCE;
            case 12:
                return PhoneInternalInterface.SuppService.SEPARATE;
            case 13:
                return PhoneInternalInterface.SuppService.TRANSFER;
        }
    }

    @Override
    public void handleMessage(Message msg) {
        int causeCode;
        Connection connection;
        this.mHelper.LogerMessage(msg.what);
        switch (msg.what) {
            case 1:
                Rlog.d(LOG_TAG, "Event EVENT_POLL_CALLS_RESULT Received");
                if (msg != this.mLastRelevantPoll) {
                    return;
                }
                this.mNeedsPoll = DBG_POLL;
                this.mLastRelevantPoll = null;
                handlePollCalls((AsyncResult) msg.obj);
                if (!this.mWaitForHoldToHangupRequest.isHoldDone()) {
                    return;
                }
                proprietaryLog("Switch ends, and poll call done, then resume hangup");
                this.mWaitForHoldToHangupRequest.resumeHangupAfterHold();
                return;
            case 2:
            case 3:
                pollCallsWhenSafe();
                return;
            case 4:
                operationComplete();
                if (!this.hasPendingReplaceRequest) {
                    return;
                }
                this.hasPendingReplaceRequest = DBG_POLL;
                return;
            case 5:
                String vendorCause = null;
                AsyncResult ar = (AsyncResult) msg.obj;
                operationComplete();
                if (ar.exception != null) {
                    causeCode = 16;
                    Rlog.i(LOG_TAG, "Exception during getLastCallFailCause, assuming normal disconnect");
                } else {
                    LastCallFailCause failCause = (LastCallFailCause) ar.result;
                    causeCode = failCause.causeCode;
                    vendorCause = failCause.vendorCause;
                }
                if (causeCode == 34 || causeCode == 41 || causeCode == 42 || causeCode == 44 || causeCode == 49 || causeCode == 58 || causeCode == 65535) {
                    CellLocation loc = this.mPhone.getCellLocation();
                    int cid = -1;
                    if (loc != null) {
                        if (isPhoneTypeGsm()) {
                            cid = ((GsmCellLocation) loc).getCid();
                        } else {
                            cid = ((CdmaCellLocation) loc).getBaseStationId();
                        }
                    }
                    EventLog.writeEvent(EventLogTags.CALL_DROP, Integer.valueOf(causeCode), Integer.valueOf(cid), Integer.valueOf(TelephonyManager.getDefault().getNetworkType(this.mPhone.getSubId())));
                }
                int s = this.mDroppedDuringPoll.size();
                for (int i = 0; i < s; i++) {
                    GsmCdmaConnection conn = this.mDroppedDuringPoll.get(i);
                    conn.onRemoteDisconnect(causeCode, vendorCause);
                }
                updatePhoneState();
                this.mPhone.notifyPreciseCallStateChanged();
                this.mDroppedDuringPoll.clear();
                return;
            case 8:
                if (!isPhoneTypeGsm()) {
                    return;
                }
                AsyncResult ar2 = (AsyncResult) msg.obj;
                if (ar2.exception != null) {
                    if (this.mWaitForHoldToRedialRequest.isWaitToRedial()) {
                        this.mPendingMO.mCause = 3;
                        this.mPendingMO.onDisconnect(3);
                        this.mPendingMO = null;
                        this.mHangupPendingMO = DBG_POLL;
                        updatePhoneState();
                        resumeBackgroundAfterDialFailed();
                        this.mWaitForHoldToRedialRequest.resetToRedial();
                    }
                    this.mPhone.notifySuppServiceFailed(getFailedService(msg.what));
                } else if (this.mWaitForHoldToRedialRequest.isWaitToRedial()) {
                    proprietaryLog("Switch success, then resume dial");
                    this.mWaitForHoldToRedialRequest.resumeDialAfterHold();
                }
                if (this.mWaitForHoldToHangupRequest.isWaitToHangup()) {
                    proprietaryLog("Switch ends, wait for poll call done to hangup");
                    this.mWaitForHoldToHangupRequest.setHoldDone();
                }
                this.mHasPendingSwapRequest = DBG_POLL;
                operationComplete();
                return;
            case 9:
                handleRadioAvailable();
                return;
            case 10:
                handleRadioNotAvailable();
                return;
            case 11:
                if (isPhoneTypeGsm() && (connection = this.mForegroundCall.getLatestConnection()) != null) {
                    connection.onConferenceMergeFailed();
                }
                break;
            case 12:
            case 13:
                break;
            case 14:
                if (!isPhoneTypeGsm()) {
                    if (this.mPendingCallInEcm) {
                        String dialString = (String) ((AsyncResult) msg.obj).userObj;
                        String tmpStr = this.mPendingMO.getAddress() + "," + PhoneNumberUtils.extractNetworkPortionAlt(dialString);
                        this.mCi.dial(tmpStr, this.mPendingCallClirMode, obtainCompleteMessage());
                        this.mPendingCallInEcm = DBG_POLL;
                        if (this.mCdmaCallTrackerExt != null && this.mCdmaCallTrackerExt.needToConvert(dialString, GsmCdmaConnection.formatDialString(dialString))) {
                            this.mPendingMO.setConverted(PhoneNumberUtils.extractNetworkPortionAlt(dialString));
                        }
                    }
                    this.mPhone.unsetOnEcbModeExitResponse(this);
                    return;
                }
                throw new RuntimeException("unexpected event " + msg.what + " not handled by phone type " + this.mPhone.getPhoneType());
            case 15:
                if (!isPhoneTypeGsm()) {
                    AsyncResult ar3 = (AsyncResult) msg.obj;
                    if (ar3.exception != null) {
                        return;
                    }
                    handleCallWaitingInfo((CdmaCallWaitingNotification) ar3.result);
                    Rlog.d(LOG_TAG, "Event EVENT_CALL_WAITING_INFO_CDMA Received");
                    return;
                }
                throw new RuntimeException("unexpected event " + msg.what + " not handled by phone type " + this.mPhone.getPhoneType());
            case 16:
                if (!isPhoneTypeGsm()) {
                    AsyncResult ar4 = (AsyncResult) msg.obj;
                    if (ar4.exception != null) {
                        return;
                    }
                    if (this.mPendingMO != null) {
                        this.mPendingMO.onConnectedInOrOut();
                    }
                    this.mPendingMO = null;
                    return;
                }
                throw new RuntimeException("unexpected event " + msg.what + " not handled by phone type " + this.mPhone.getPhoneType());
            case 20:
                if (!isPhoneTypeGsm()) {
                    AsyncResult ar5 = (AsyncResult) msg.obj;
                    if (ar5.exception == null) {
                        final String dialString2 = (String) ((AsyncResult) msg.obj).userObj;
                        postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (GsmCdmaCallTracker.this.mPendingMO == null) {
                                    return;
                                }
                                String tmpStr2 = GsmCdmaCallTracker.this.mPendingMO.getAddress() + "," + PhoneNumberUtils.extractNetworkPortionAlt(dialString2);
                                GsmCdmaCallTracker.this.mCi.sendCDMAFeatureCode(tmpStr2, GsmCdmaCallTracker.this.obtainMessage(16));
                                if (GsmCdmaCallTracker.this.mCdmaCallTrackerExt == null || !GsmCdmaCallTracker.this.mCdmaCallTrackerExt.needToConvert(dialString2, GsmCdmaConnection.formatDialString(dialString2))) {
                                    return;
                                }
                                GsmCdmaCallTracker.this.mPendingMO.setConverted(PhoneNumberUtils.extractNetworkPortionAlt(dialString2));
                            }
                        }, this.m3WayCallFlashDelay);
                        return;
                    } else {
                        this.mPendingMO = null;
                        Rlog.w(LOG_TAG, "exception happened on Blank Flash for 3-way call");
                        return;
                    }
                }
                throw new RuntimeException("unexpected event " + msg.what + " not handled by phone type " + this.mPhone.getPhoneType());
            case 1000:
                this.mHelper.CallIndicationProcess((AsyncResult) msg.obj);
                return;
            case 1001:
                proprietaryLog("Receives EVENT_RADIO_OFF_OR_NOT_AVAILABLE");
                handlePollCalls(new AsyncResult((Object) null, (Object) null, new CommandException(CommandException.Error.RADIO_NOT_AVAILABLE)));
                return;
            case 1002:
                this.mHelper.PendingHangupRequestDec();
                operationComplete();
                return;
            case 1003:
                AsyncResult ar6 = (AsyncResult) msg.obj;
                if (ar6.exception != null) {
                    proprietaryLog("dial call failed!!");
                    this.mHelper.PendingHangupRequestUpdate();
                }
                operationComplete();
                return;
            case 1004:
                proprietaryLog("Receives EVENT_ECONF_SRVCC_INDICATION");
                AsyncResult ar7 = (AsyncResult) msg.obj;
                this.mEconfSrvccConnectionIds = (int[]) ar7.result;
                this.mNeedWaitImsEConfSrvcc = DBG_POLL;
                pollCallsWhenSafe();
                return;
            case 1005:
                AsyncResult ar8 = (AsyncResult) msg.obj;
                if (ar8.exception != null) {
                    return;
                }
                handleCallAccepted();
                proprietaryLog("EVENT_CDMA_CALL_ACCEPTED");
                return;
            case 1006:
                proprietaryLog("Receives EVENT_CALL_REDIAL_STATE");
                AsyncResult ar9 = (AsyncResult) msg.obj;
                this.mRedialState = redialStateFromInt(((Integer) ar9.result).intValue());
                return;
            default:
                throw new RuntimeException("unexpected event " + msg.what + " not handled by phone type " + this.mPhone.getPhoneType());
        }
        if (isPhoneTypeGsm()) {
            AsyncResult ar10 = (AsyncResult) msg.obj;
            if (ar10.exception != null) {
                this.mHelper.PendingHangupRequestUpdate();
                this.mPhone.notifySuppServiceFailed(getFailedService(msg.what));
            }
            operationComplete();
            return;
        }
        throw new RuntimeException("unexpected event " + msg.what + " not handled by phone type " + this.mPhone.getPhoneType());
    }

    private void checkAndEnableDataCallAfterEmergencyCallDropped() {
        if (!this.mIsInEmergencyCall) {
            return;
        }
        this.mIsInEmergencyCall = DBG_POLL;
        String inEcm = this.mPhone.getSystemProperty("ril.cdma.inecmmode", "false");
        log("checkAndEnableDataCallAfterEmergencyCallDropped,inEcm=" + inEcm);
        if (inEcm.compareTo("false") == 0) {
            this.mPhone.mDcTracker.setInternalDataEnabled(true);
            this.mPhone.notifyEmergencyCallRegistrants(DBG_POLL);
        }
        this.mPhone.sendEmergencyCallStateChange(DBG_POLL);
    }

    private Connection checkMtFindNewRinging(DriverCall dc, int i) {
        if (this.mConnections[i].getCall() == this.mRingingCall) {
            Connection newRinging = this.mConnections[i];
            log("Notify new ring " + dc);
            return newRinging;
        }
        Rlog.e(LOG_TAG, "Phantom call appeared " + dc);
        if (dc.state == DriverCall.State.ALERTING || dc.state == DriverCall.State.DIALING) {
            return null;
        }
        this.mConnections[i].onConnectedInOrOut();
        if (dc.state != DriverCall.State.HOLDING) {
            return null;
        }
        this.mConnections[i].onStartedHolding();
        return null;
    }

    public boolean isInEmergencyCall() {
        return this.mIsInEmergencyCall;
    }

    private boolean isPhoneTypeGsm() {
        if (this.mPhone.getPhoneType() == 1) {
            return true;
        }
        return DBG_POLL;
    }

    public GsmCdmaPhone getPhone() {
        return this.mPhone;
    }

    @Override
    protected void log(String msg) {
        Rlog.d(LOG_TAG, "[GsmCdmaCallTracker] " + msg);
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("GsmCdmaCallTracker extends:");
        super.dump(fd, pw, args);
        pw.println("mConnections: length=" + this.mConnections.length);
        for (int i = 0; i < this.mConnections.length; i++) {
            pw.printf("  mConnections[%d]=%s\n", Integer.valueOf(i), this.mConnections[i]);
        }
        pw.println(" mVoiceCallEndedRegistrants=" + this.mVoiceCallEndedRegistrants);
        pw.println(" mVoiceCallStartedRegistrants=" + this.mVoiceCallStartedRegistrants);
        if (!isPhoneTypeGsm()) {
            pw.println(" mCallWaitingRegistrants=" + this.mCallWaitingRegistrants);
        }
        pw.println(" mDroppedDuringPoll: size=" + this.mDroppedDuringPoll.size());
        for (int i2 = 0; i2 < this.mDroppedDuringPoll.size(); i2++) {
            pw.printf("  mDroppedDuringPoll[%d]=%s\n", Integer.valueOf(i2), this.mDroppedDuringPoll.get(i2));
        }
        pw.println(" mRingingCall=" + this.mRingingCall);
        pw.println(" mForegroundCall=" + this.mForegroundCall);
        pw.println(" mBackgroundCall=" + this.mBackgroundCall);
        pw.println(" mPendingMO=" + this.mPendingMO);
        pw.println(" mHangupPendingMO=" + this.mHangupPendingMO);
        pw.println(" mPhone=" + this.mPhone);
        pw.println(" mDesiredMute=" + this.mDesiredMute);
        pw.println(" mState=" + this.mState);
        if (isPhoneTypeGsm()) {
            return;
        }
        pw.println(" mPendingCallInEcm=" + this.mPendingCallInEcm);
        pw.println(" mIsInEmergencyCall=" + this.mIsInEmergencyCall);
        pw.println(" mPendingCallClirMode=" + this.mPendingCallClirMode);
        pw.println(" mIsEcmTimerCanceled=" + this.mIsEcmTimerCanceled);
    }

    @Override
    public PhoneConstants.State getState() {
        return this.mState;
    }

    public int getMaxConnectionsPerCall() {
        if (this.mPhone.isPhoneTypeGsm()) {
            return 5;
        }
        return 1;
    }

    public void hangupAll() throws CallStateException {
        proprietaryLog("hangupAll");
        this.mCi.hangupAll(obtainCompleteMessage());
        if (!this.mRingingCall.isIdle()) {
            this.mRingingCall.onHangupLocal();
        }
        if (!this.mForegroundCall.isIdle()) {
            this.mForegroundCall.onHangupLocal();
        }
        if (this.mBackgroundCall.isIdle()) {
            return;
        }
        this.mBackgroundCall.onHangupLocal();
    }

    public void setIncomingCallIndicationResponse(boolean accept) {
        this.mHelper.CallIndicationResponse(accept);
    }

    public void registerForVoiceCallIncomingIndication(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mVoiceCallIncomingIndicationRegistrants.add(r);
    }

    public void unregisterForVoiceCallIncomingIndication(Handler h) {
        this.mVoiceCallIncomingIndicationRegistrants.remove(h);
    }

    private boolean canVtDial() {
        int networkType = this.mPhone.getServiceState().getVoiceNetworkType();
        proprietaryLog("networkType=" + TelephonyManager.getNetworkTypeName(networkType));
        if (networkType == 3 || networkType == 8 || networkType == 9 || networkType == 10 || networkType == 15 || networkType == 13) {
            return true;
        }
        return DBG_POLL;
    }

    public synchronized Connection vtDial(String dialString, int clirMode, UUSInfo uusInfo, Bundle intentExtras) throws CallStateException {
        clearDisconnected();
        if (!canDial()) {
            throw new CallStateException("cannot dial in current state");
        }
        if (!canVtDial()) {
            throw new CallStateException("cannot vtDial under non 3/4G network");
        }
        String dialString2 = convertNumberIfNecessary(this.mPhone, dialString);
        if (this.mForegroundCall.getState() != Call.State.IDLE || this.mRingingCall.getState().isRinging()) {
            throw new CallStateException("cannot vtDial since non-IDLE call already exists");
        }
        this.mPendingMO = new GsmCdmaConnection(this.mPhone, checkForTestEmergencyNumber(dialString2), this, this.mForegroundCall);
        this.mHangupPendingMO = DBG_POLL;
        this.mPendingMO.mIsVideo = true;
        if (this.mPendingMO.getAddress() == null || this.mPendingMO.getAddress().length() == 0 || this.mPendingMO.getAddress().indexOf(78) >= 0) {
            this.mPendingMO.mCause = 7;
            pollCallsWhenSafe();
        } else {
            setMute(DBG_POLL);
            this.mCi.vtDial(this.mPendingMO.getAddress(), clirMode, uusInfo, obtainCompleteMessage());
            this.mPendingMO.setVideoState(3);
            this.mForegroundCall.mVTProvider = new GsmVTProvider();
            proprietaryLog("vtDial new GsmVTProvider");
            try {
                IGsmVideoCallProvider gsmVideoCallProvider = this.mForegroundCall.mVTProvider.getInterface();
                if (gsmVideoCallProvider != null) {
                    GsmVideoCallProviderWrapper gsmVideoCallProviderWrapper = new GsmVideoCallProviderWrapper(gsmVideoCallProvider);
                    proprietaryLog("vtDial new GsmVideoCallProviderWrapper");
                    this.mPendingMO.setVideoProvider(gsmVideoCallProviderWrapper);
                }
            } catch (RemoteException e) {
                Rlog.e(PROP_LOG_TAG, "vtDial new GsmVideoCallProviderWrapper failed");
            }
        }
        if (this.mNumberConverted) {
            this.mPendingMO.setConverted(dialString);
            this.mNumberConverted = DBG_POLL;
        }
        updatePhoneState();
        this.mPhone.notifyPreciseCallStateChanged();
        return this.mPendingMO;
    }

    public Connection vtDial(String dialString, UUSInfo uusInfo, Bundle intentExtras) throws CallStateException {
        return vtDial(dialString, 0, uusInfo, intentExtras);
    }

    public void acceptCall(int videoState) throws CallStateException {
        GsmCdmaConnection fgCn;
        if (this.mRingingCall.getState() == Call.State.INCOMING) {
            Rlog.i("phone", "acceptCall: incoming...");
            setMute(DBG_POLL);
            GsmCdmaConnection cn = (GsmCdmaConnection) this.mRingingCall.mConnections.get(0);
            if (cn.isVideo() && videoState == 0) {
                this.mCi.acceptVtCallWithVoiceOnly(cn.getGsmCdmaIndex(), obtainCompleteMessage());
                cn.setVideoState(0);
                return;
            } else {
                this.mCi.acceptCall(obtainCompleteMessage());
                return;
            }
        }
        if (this.mRingingCall.getState() == Call.State.WAITING) {
            if (isPhoneTypeGsm()) {
                setMute(DBG_POLL);
                if (((GsmCdmaConnection) this.mRingingCall.mConnections.get(0)).isVideo() && (fgCn = (GsmCdmaConnection) this.mForegroundCall.mConnections.get(0)) != null && fgCn.isVideo()) {
                    this.hasPendingReplaceRequest = true;
                    this.mCi.replaceVtCall(fgCn.mIndex + 1, obtainCompleteMessage());
                    fgCn.onHangupLocal();
                    return;
                }
            } else {
                GsmCdmaConnection cwConn = (GsmCdmaConnection) this.mRingingCall.getLatestConnection();
                cwConn.updateParent(this.mRingingCall, this.mForegroundCall);
                cwConn.onConnectedInOrOut();
                updatePhoneState();
            }
            switchWaitingOrHoldingAndActive();
            return;
        }
        throw new CallStateException("phone not ringing");
    }

    private void resumeBackgroundAfterDialFailed() {
        List<Connection> connCopy = (List) this.mBackgroundCall.mConnections.clone();
        int s = connCopy.size();
        for (int i = 0; i < s; i++) {
            GsmCdmaConnection conn = (GsmCdmaConnection) connCopy.get(i);
            conn.resumeHoldAfterDialFailed();
        }
    }

    private synchronized boolean restoreConferenceParticipantAddress() {
        if (this.mEconfSrvccConnectionIds == null) {
            proprietaryLog("SRVCC: restoreConferenceParticipantAddress():ignore because mEconfSrvccConnectionIds is empty");
            return DBG_POLL;
        }
        boolean finishRestore = DBG_POLL;
        int numOfParticipants = this.mEconfSrvccConnectionIds[0];
        for (int index = 1; index <= numOfParticipants; index++) {
            int participantCallId = this.mEconfSrvccConnectionIds[index];
            GsmCdmaConnection participantConnection = this.mConnections[participantCallId - 1];
            if (participantConnection != null) {
                proprietaryLog("SRVCC: found conference connections!");
                if (participantConnection.mOrigConnection instanceof ImsPhoneConnection) {
                    ImsPhoneConnection hostConnection = (ImsPhoneConnection) participantConnection.mOrigConnection;
                    if (hostConnection == null) {
                        proprietaryLog("SRVCC: no host, ignore connection: " + participantConnection);
                    } else {
                        String address = hostConnection.getConferenceParticipantAddress(index - 1);
                        participantConnection.updateConferenceParticipantAddress(address);
                        finishRestore = true;
                        proprietaryLog("SRVCC: restore Connection=" + participantConnection + " with address:" + address);
                    }
                } else {
                    proprietaryLog("SRVCC: host is abnormal, ignore connection: " + participantConnection);
                }
            }
        }
        return finishRestore;
    }

    @Override
    protected Connection getHoConnection(DriverCall dc) {
        if (dc == null) {
            return null;
        }
        if (this.mEconfSrvccConnectionIds != null && dc != null) {
            int numOfParticipants = this.mEconfSrvccConnectionIds[0];
            int index = 1;
            while (true) {
                if (index > numOfParticipants) {
                    break;
                }
                if (dc.index != this.mEconfSrvccConnectionIds[index]) {
                    index++;
                } else {
                    proprietaryLog("SRVCC: getHoConnection for call-id:" + dc.index + " in a conference is found!");
                    if (this.mImsConfHostConnection == null) {
                        proprietaryLog("SRVCC: but mImsConfHostConnection is null, try to find by callState");
                    } else {
                        proprietaryLog("SRVCC: ret= " + this.mImsConfHostConnection);
                        return this.mImsConfHostConnection;
                    }
                }
            }
        }
        return super.getHoConnection(dc);
    }

    private void processPlusCodeForWaitingCall(CdmaCallWaitingNotification cw) {
        String address = cw.number;
        proprietaryLog("processPlusCodeForWaitingCall before format number:" + cw.number);
        if (address != null && address.length() > 0 && this.mCdmaCallTrackerExt != null) {
            cw.number = this.mCdmaCallTrackerExt.processPlusCodeForWaitingCall(address, cw.numberType);
        }
        proprietaryLog("processPlusCodeForWaitingCall after format number:" + cw.number);
    }

    private boolean shouldNotifyWaitingCall(CdmaCallWaitingNotification cw) {
        GsmCdmaConnection lastRingConn;
        String address = cw.number;
        proprietaryLog("shouldNotifyWaitingCall, address:" + address);
        if (address != null && address.length() > 0 && (lastRingConn = (GsmCdmaConnection) this.mRingingCall.getLatestConnection()) != null && address.equals(lastRingConn.getAddress())) {
            proprietaryLog("handleCallWaitingInfo, skip duplicate waiting call!");
            return DBG_POLL;
        }
        return true;
    }

    private void handleCallAccepted() {
        List<Connection> connections = this.mForegroundCall.getConnections();
        int count = connections.size();
        proprietaryLog("handleCallAccepted, fgcall count:" + count);
        if (count != 1) {
            return;
        }
        GsmCdmaConnection c = (GsmCdmaConnection) connections.get(0);
        if (!c.onCdmaCallAccept()) {
            return;
        }
        this.mPhone.notifyCdmaCallAccepted();
    }

    void proprietaryLog(String s) {
        Rlog.d(PROP_LOG_TAG, s);
    }
}
