package com.android.internal.telephony;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.Registrant;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Vibrator;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.DriverCall;
import com.android.internal.telephony.cdma.CdmaCallWaitingNotification;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.mediatek.common.MPlugin;
import com.mediatek.common.telephony.IGsmConnectionExt;
import com.mediatek.internal.telephony.ConferenceCallMessageHandler;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;

public class GsmCdmaConnection extends Connection {

    private static final int[] f6comandroidinternaltelephonyDriverCall$StateSwitchesValues = null;
    private static final boolean DBG = true;
    static final int EVENT_DTMF_DELAY_DONE = 5;
    static final int EVENT_DTMF_DONE = 1;
    static final int EVENT_NEXT_POST_DIAL = 3;
    static final int EVENT_PAUSE_DONE = 2;
    static final int EVENT_WAKE_LOCK_TIMEOUT = 4;
    private static final String LOG_TAG = "GsmCdmaConnection";
    private static final int MO_CALL_VIBRATE_TIME = 200;
    static final int PAUSE_DELAY_FIRST_MILLIS_GSM = 500;
    static final int PAUSE_DELAY_MILLIS_CDMA = 2000;
    static final int PAUSE_DELAY_MILLIS_GSM = 3000;
    private static final String PROP_LOG_TAG = "GsmCdmaConn";
    private static final boolean VDBG;
    static final int WAKE_LOCK_TIMEOUT_MILLIS = 60000;
    long mDisconnectTime;
    boolean mDisconnected;
    private int mDtmfToneDelay;
    Handler mHandler;
    public int mIndex;
    private boolean mIsRealConnected;
    boolean mIsVideo;
    Connection mOrigConnection;
    GsmCdmaCallTracker mOwner;
    GsmCdmaCall mParent;
    private PowerManager.WakeLock mPartialWakeLock;
    int mPreciseCause;
    private boolean mReceivedAccepted;
    UUSInfo mUusInfo;
    String mVendorCause;

    private static int[] m15xd9c92f69() {
        if (f6comandroidinternaltelephonyDriverCall$StateSwitchesValues != null) {
            return f6comandroidinternaltelephonyDriverCall$StateSwitchesValues;
        }
        int[] iArr = new int[DriverCall.State.valuesCustom().length];
        try {
            iArr[DriverCall.State.ACTIVE.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[DriverCall.State.ALERTING.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[DriverCall.State.DIALING.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[DriverCall.State.HOLDING.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[DriverCall.State.INCOMING.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[DriverCall.State.WAITING.ordinal()] = 6;
        } catch (NoSuchFieldError e6) {
        }
        f6comandroidinternaltelephonyDriverCall$StateSwitchesValues = iArr;
        return iArr;
    }

    static {
        VDBG = SystemProperties.getInt("persist.log.tag.tel_dbg", 0) == 1;
    }

    class MyHandler extends Handler {
        MyHandler(Looper l) {
            super(l);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    GsmCdmaConnection.this.mHandler.sendMessageDelayed(GsmCdmaConnection.this.mHandler.obtainMessage(5), GsmCdmaConnection.this.mDtmfToneDelay);
                    break;
                case 2:
                case 3:
                case 5:
                    GsmCdmaConnection.this.processNextPostDialChar();
                    break;
                case 4:
                    GsmCdmaConnection.this.releaseWakeLock();
                    break;
            }
        }
    }

    public GsmCdmaConnection(GsmCdmaPhone phone, DriverCall dc, GsmCdmaCallTracker ct, int index) {
        super(phone.getPhoneType());
        this.mPreciseCause = 0;
        this.mDtmfToneDelay = 0;
        createWakeLock(phone.getContext());
        acquireWakeLock();
        this.mOwner = ct;
        this.mHandler = new MyHandler(this.mOwner.getLooper());
        this.mAddress = dc.number;
        this.mIsIncoming = dc.isMT;
        this.mCreateTime = System.currentTimeMillis();
        this.mCnapName = dc.name;
        this.mCnapNamePresentation = dc.namePresentation;
        this.mNumberPresentation = dc.numberPresentation;
        this.mUusInfo = dc.uusInfo;
        this.mIndex = index;
        this.mIsVideo = dc.isVideo;
        this.mParent = parentFromDCState(dc.state);
        this.mParent.attach(this, dc);
        fetchDtmfToneDelay(phone);
    }

    public GsmCdmaConnection(GsmCdmaPhone phone, String dialString, GsmCdmaCallTracker ct, GsmCdmaCall parent) {
        super(phone.getPhoneType());
        this.mPreciseCause = 0;
        this.mDtmfToneDelay = 0;
        createWakeLock(phone.getContext());
        acquireWakeLock();
        this.mOwner = ct;
        this.mHandler = new MyHandler(this.mOwner.getLooper());
        if (isPhoneTypeGsm()) {
            this.mDialString = dialString;
        } else {
            Rlog.d(LOG_TAG, "[GsmCdmaConn] GsmCdmaConnection: dialString=" + maskDialString(dialString));
            dialString = formatDialString(dialString);
            Rlog.d(LOG_TAG, "[GsmCdmaConn] GsmCdmaConnection:formated dialString=" + maskDialString(dialString));
        }
        this.mAddress = PhoneNumberUtils.extractNetworkPortionAlt(dialString);
        this.mPostDialString = PhoneNumberUtils.extractPostDialPortion(dialString);
        this.mIndex = -1;
        this.mIsIncoming = false;
        this.mCnapName = null;
        this.mCnapNamePresentation = 1;
        this.mNumberPresentation = 1;
        this.mCreateTime = System.currentTimeMillis();
        if (parent != null) {
            this.mParent = parent;
            if (!isPhoneTypeGsm() && parent.mState == Call.State.ACTIVE) {
                parent.attachFake(this, Call.State.ACTIVE);
            } else {
                parent.attachFake(this, Call.State.DIALING);
            }
        }
        fetchDtmfToneDelay(phone);
        this.mIsRealConnected = false;
        this.mReceivedAccepted = false;
    }

    public GsmCdmaConnection(Context context, CdmaCallWaitingNotification cw, GsmCdmaCallTracker ct, GsmCdmaCall parent) {
        super(parent.getPhone().getPhoneType());
        this.mPreciseCause = 0;
        this.mDtmfToneDelay = 0;
        createWakeLock(context);
        acquireWakeLock();
        this.mOwner = ct;
        this.mHandler = new MyHandler(this.mOwner.getLooper());
        this.mAddress = cw.number;
        this.mNumberPresentation = cw.numberPresentation;
        this.mCnapName = cw.name;
        this.mCnapNamePresentation = cw.namePresentation;
        this.mIndex = -1;
        this.mIsIncoming = true;
        this.mCreateTime = System.currentTimeMillis();
        this.mConnectTime = 0L;
        this.mParent = parent;
        parent.attachFake(this, Call.State.WAITING);
    }

    public void dispose() {
        clearPostDialListeners();
        releaseAllWakeLocks();
    }

    static boolean equalsHandlesNulls(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    public static String formatDialString(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        int length = phoneNumber.length();
        StringBuilder ret = new StringBuilder();
        int currIndex = 0;
        while (currIndex < length) {
            char c = phoneNumber.charAt(currIndex);
            if (isPause(c) || isWait(c)) {
                if (currIndex < length - 1) {
                    int nextIndex = findNextPCharOrNonPOrNonWCharIndex(phoneNumber, currIndex);
                    if (nextIndex < length) {
                        char pC = findPOrWCharToAppend(phoneNumber, currIndex, nextIndex);
                        ret.append(pC);
                        if (nextIndex > currIndex + 1) {
                            currIndex = nextIndex - 1;
                        }
                    } else if (nextIndex == length) {
                        currIndex = length - 1;
                    }
                }
            } else {
                ret.append(c);
            }
            currIndex++;
        }
        return PhoneNumberUtils.cdmaCheckAndProcessPlusCode(ret.toString());
    }

    boolean compareTo(DriverCall c) {
        if (!(!this.mIsIncoming ? c.isMT : true)) {
            return true;
        }
        if (isPhoneTypeGsm() && this.mOrigConnection != null) {
            return true;
        }
        String cAddress = PhoneNumberUtils.stringFromStringAndTOA(c.number, c.TOA);
        if (this.mIsIncoming == c.isMT) {
            return equalsHandlesNulls(this.mAddress, cAddress);
        }
        return false;
    }

    @Override
    public String getOrigDialString() {
        return this.mDialString;
    }

    @Override
    public GsmCdmaCall getCall() {
        return this.mParent;
    }

    @Override
    public long getDisconnectTime() {
        return this.mDisconnectTime;
    }

    @Override
    public long getHoldDurationMillis() {
        if (getState() != Call.State.HOLDING) {
            return 0L;
        }
        return SystemClock.elapsedRealtime() - this.mHoldingStartTime;
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
        if (!this.mDisconnected) {
            this.mOwner.hangup(this);
            return;
        }
        throw new CallStateException(ConferenceCallMessageHandler.STATUS_DISCONNECTED);
    }

    @Override
    public void separate() throws CallStateException {
        if (!this.mDisconnected) {
            this.mOwner.separate(this);
            return;
        }
        throw new CallStateException(ConferenceCallMessageHandler.STATUS_DISCONNECTED);
    }

    @Override
    public void proceedAfterWaitChar() {
        if (this.mPostDialState != Connection.PostDialState.WAIT) {
            Rlog.w(LOG_TAG, "GsmCdmaConnection.proceedAfterWaitChar(): Expected getPostDialState() to be WAIT but was " + this.mPostDialState);
        } else {
            setPostDialState(Connection.PostDialState.STARTED);
            processNextPostDialChar();
        }
    }

    @Override
    public void proceedAfterWildChar(String str) {
        if (this.mPostDialState != Connection.PostDialState.WILD) {
            Rlog.w(LOG_TAG, "GsmCdmaConnection.proceedAfterWaitChar(): Expected getPostDialState() to be WILD but was " + this.mPostDialState);
            return;
        }
        setPostDialState(Connection.PostDialState.STARTED);
        this.mPostDialString = str + this.mPostDialString.substring(this.mNextPostDialChar);
        this.mNextPostDialChar = 0;
        log(new StringBuilder().append("proceedAfterWildChar: new postDialString is ").append(this.mPostDialString).toString());
        processNextPostDialChar();
    }

    @Override
    public void cancelPostDial() {
        setPostDialState(Connection.PostDialState.CANCELLED);
    }

    void onHangupLocal() {
        this.mCause = 3;
        this.mPreciseCause = 0;
        this.mVendorCause = null;
    }

    int disconnectCauseFromCode(int causeCode) {
        switch (causeCode) {
            case 1:
                return 25;
            case 3:
                return 51;
            case 6:
                return 69;
            case 8:
                return 70;
            case 17:
                return 4;
            case 18:
                return 52;
            case 25:
                return 71;
            case 26:
                return 72;
            case CallFailCause.DESTINATION_OUT_OF_ORDER:
                return 73;
            case CallFailCause.INVALID_NUMBER_FORMAT:
                return 55;
            case CallFailCause.FACILITY_REJECTED:
                return 56;
            case 34:
            case 41:
            case 42:
            case CallFailCause.CHANNEL_NOT_AVAIL:
            case 49:
            case 58:
                return 5;
            case 38:
            case 63:
                return 63;
            case CallFailCause.ACCESS_INFORMATION_DISCARDED:
                return 74;
            case 47:
                return 60;
            case 50:
                return 75;
            case 55:
                return 76;
            case 57:
                return 61;
            case CallFailCause.BEARER_NOT_IMPLEMENT:
                return 64;
            case CallFailCause.ACM_LIMIT_EXCEEDED:
                return 15;
            case CallFailCause.FACILITY_NOT_IMPLEMENT:
                return 65;
            case 70:
                return 66;
            case 81:
                return 77;
            case 87:
                return 78;
            case CallFailCause.INCOMPATIBLE_DESTINATION:
                return 68;
            case CallFailCause.INVALID_TRANSIT_NETWORK_SELECTION:
                return 79;
            case CallFailCause.SEMANTICALLY_INCORRECT_MESSAGE:
                return 80;
            case 96:
                return 81;
            case CallFailCause.MESSAGE_TYPE_NON_EXISTENT:
                return 82;
            case CallFailCause.MESSAGE_TYPE_NOT_COMPATIBLE_WITH_PROT_STATE:
                return 83;
            case CallFailCause.IE_NON_EXISTENT_OR_NOT_IMPLEMENTED:
                return 84;
            case 100:
                return 85;
            case 101:
                return 86;
            case CallFailCause.RECOVERY_ON_TIMER_EXPIRY:
                return 87;
            case 111:
                return 88;
            case CallFailCause.CALL_BARRED:
                return 20;
            case CallFailCause.FDN_BLOCKED:
                return 21;
            case CallFailCause.IMEI_NOT_ACCEPTED:
                if (PhoneNumberUtils.isEmergencyNumber(getAddress())) {
                    return 2;
                }
                break;
            case 244:
                return 46;
            case 245:
                return 47;
            case 246:
                return 48;
            case 1000:
                return 26;
            case 1001:
                return 27;
            case 1002:
                return 28;
            case 1003:
                return 29;
            case 1004:
                return 30;
            case 1005:
                return 31;
            case 1006:
                return 32;
            case 1007:
                return 33;
            case 1008:
                return 34;
            case 1009:
                return 35;
            case CallFailCause.CM_MM_RR_CONNECTION_RELEASE:
                return 90;
        }
        GsmCdmaPhone phone = this.mOwner.getPhone();
        int serviceState = phone.getServiceState().getState();
        UiccCardApplication cardApp = phone.getUiccCardApplication();
        IccCardApplicationStatus.AppState uiccAppState = cardApp != null ? cardApp.getState() : IccCardApplicationStatus.AppState.APPSTATE_UNKNOWN;
        proprietaryLog("disconnectCauseFromCode, causeCode:" + causeCode + ", cardApp:" + cardApp + ", serviceState:" + serviceState + ", uiccAppState:" + uiccAppState);
        if (serviceState == 3) {
            return 17;
        }
        if (serviceState == 1 || serviceState == 2) {
            if (PhoneNumberUtils.isEmergencyNumber(getAddress())) {
                return (causeCode == 31 || causeCode == 16 || causeCode == 79) ? 2 : 36;
            }
            return 18;
        }
        if (!isPhoneTypeGsm()) {
            if (phone.mCdmaSubscriptionSource != 0 || uiccAppState == IccCardApplicationStatus.AppState.APPSTATE_READY) {
                return causeCode == 16 ? 2 : 36;
            }
            return 19;
        }
        if (uiccAppState != IccCardApplicationStatus.AppState.APPSTATE_READY) {
            return 19;
        }
        if (causeCode != 65535) {
            if (causeCode == 16) {
                return 2;
            }
            return (PhoneNumberUtils.isEmergencyNumber(getAddress()) && (causeCode == 31 || causeCode == 79)) ? 2 : 36;
        }
        if (phone.mSST.mRestrictedState.isCsRestricted()) {
            return 22;
        }
        if (phone.mSST.mRestrictedState.isCsEmergencyRestricted()) {
            return 24;
        }
        return phone.mSST.mRestrictedState.isCsNormalRestricted() ? 23 : 36;
    }

    void onRemoteDisconnect(int causeCode, String vendorCause) {
        this.mPreciseCause = causeCode;
        this.mVendorCause = vendorCause;
        onDisconnect(disconnectCauseFromCode(causeCode));
    }

    @Override
    public boolean onDisconnect(int cause) {
        boolean changed = false;
        this.mCause = cause;
        if (!this.mDisconnected) {
            doDisconnect();
            Rlog.d(LOG_TAG, "onDisconnect: cause=" + cause);
            this.mOwner.getPhone().notifyDisconnect(this);
            if (this.mParent != null) {
                changed = this.mParent.connectionDisconnected(this);
            }
            this.mOrigConnection = null;
        }
        clearPostDialListeners();
        releaseWakeLock();
        return changed;
    }

    void onLocalDisconnect() {
        if (!this.mDisconnected) {
            doDisconnect();
            if (VDBG) {
                Rlog.d(LOG_TAG, "onLoalDisconnect");
            }
            if (this.mParent != null) {
                this.mParent.detach(this);
            }
        }
        releaseWakeLock();
    }

    public boolean update(DriverCall dc) {
        boolean changed;
        boolean changed2 = false;
        boolean wasConnectingInOrOut = isConnectingInOrOut();
        boolean wasHolding = getState() == Call.State.HOLDING;
        GsmCdmaCall newParent = parentFromDCState(dc.state);
        log("parent= " + this.mParent + ", newParent= " + newParent);
        if (!isPhoneTypeGsm() || this.mOrigConnection == null) {
            log(" mNumberConverted " + this.mNumberConverted);
            if (!equalsHandlesNulls(this.mAddress, dc.number) && (!this.mNumberConverted || !equalsHandlesNulls(this.mConvertedNumber, dc.number))) {
                log("update: phone # changed!");
                this.mAddress = dc.number;
                changed2 = true;
            }
        } else {
            log("update: mOrigConnection is not null");
        }
        if (!TextUtils.isEmpty(dc.name) && !dc.name.equals(this.mCnapName)) {
            changed2 = true;
            this.mCnapName = dc.name;
        }
        log("--dssds----" + this.mCnapName);
        this.mCnapNamePresentation = dc.namePresentation;
        this.mNumberPresentation = dc.numberPresentation;
        if (this.mIsVideo != dc.isVideo) {
            this.mIsVideo = dc.isVideo;
            changed2 = true;
        }
        if (newParent != this.mParent) {
            if (this.mParent != null) {
                this.mParent.detach(this);
            }
            newParent.attach(this, dc);
            this.mParent = newParent;
            changed = true;
        } else {
            boolean parentStateChange = this.mParent.update(this, dc);
            changed = !changed2 ? parentStateChange : true;
        }
        log("update: parent=" + this.mParent + ", hasNewParent=" + (newParent != this.mParent) + ", wasConnectingInOrOut=" + wasConnectingInOrOut + ", wasHolding=" + wasHolding + ", isConnectingInOrOut=" + isConnectingInOrOut() + ", isVideo=" + this.mIsVideo + ", changed=" + changed);
        if (wasConnectingInOrOut && !isConnectingInOrOut()) {
            onConnectedInOrOut();
        }
        if (changed && !wasHolding && getState() == Call.State.HOLDING) {
            onStartedHolding();
        }
        if (!isPhoneTypeGsm()) {
            proprietaryLog("state:" + getState() + ", mReceivedAccepted:" + this.mReceivedAccepted);
            if (getState() == Call.State.ACTIVE && this.mReceivedAccepted) {
                if (onCdmaCallAccept()) {
                    this.mOwner.mPhone.notifyCdmaCallAccepted();
                }
                this.mReceivedAccepted = false;
            }
        }
        return changed;
    }

    void fakeHoldBeforeDial() {
        if (this.mParent != null) {
            this.mParent.detach(this);
        }
        this.mParent = this.mOwner.mBackgroundCall;
        this.mParent.attachFake(this, Call.State.HOLDING);
        onStartedHolding();
    }

    void resumeHoldAfterDialFailed() {
        if (this.mParent != null) {
            this.mParent.detach(this);
        }
        this.mParent = this.mOwner.mForegroundCall;
        this.mParent.attachFake(this, Call.State.ACTIVE);
    }

    public int getGsmCdmaIndex() throws CallStateException {
        if (this.mIndex >= 0) {
            return this.mIndex + 1;
        }
        throw new CallStateException("GsmCdma index not yet assigned");
    }

    void onConnectedInOrOut() {
        this.mConnectTime = System.currentTimeMillis();
        this.mConnectTimeReal = SystemClock.elapsedRealtime();
        this.mDuration = 0L;
        log("onConnectedInOrOut: connectTime=" + this.mConnectTime);
        if (!this.mIsIncoming) {
            if (isPhoneTypeGsm()) {
                processNextPostDialChar();
                return;
            }
            int count = this.mParent.mConnections.size();
            proprietaryLog("mParent.mConnections.size():" + count);
            if (!isInChina() && !this.mIsRealConnected && count == 1) {
                this.mIsRealConnected = true;
                processNextPostDialChar();
                vibrateForAccepted();
                this.mOwner.mPhone.notifyCdmaCallAccepted();
            }
            if (count <= 1) {
                return;
            }
            this.mIsRealConnected = true;
            processNextPostDialChar();
            return;
        }
        releaseWakeLock();
    }

    private void doDisconnect() {
        this.mIndex = -1;
        this.mDisconnectTime = System.currentTimeMillis();
        this.mDuration = SystemClock.elapsedRealtime() - this.mConnectTimeReal;
        this.mDisconnected = true;
        clearPostDialListeners();
    }

    void onStartedHolding() {
        this.mHoldingStartTime = SystemClock.elapsedRealtime();
    }

    private boolean processPostDialChar(char c) {
        if (PhoneNumberUtils.is12Key(c)) {
            this.mOwner.mCi.sendDtmf(c, this.mHandler.obtainMessage(1));
        } else if (isPause(c)) {
            if (!isPhoneTypeGsm()) {
                setPostDialState(Connection.PostDialState.PAUSE);
            }
            if (isPhoneTypeGsm()) {
                if (this.mNextPostDialChar == 1 && !SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                    try {
                        IGsmConnectionExt mGsmConnectionExt = (IGsmConnectionExt) MPlugin.createInstance(IGsmConnectionExt.class.getName(), this.mOwner.mPhone.getContext());
                        if (mGsmConnectionExt != null) {
                            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(2), mGsmConnectionExt.getFirstPauseDelayMSeconds(PAUSE_DELAY_FIRST_MILLIS_GSM));
                        } else {
                            Rlog.e(PROP_LOG_TAG, "Fail to initialize IGsmConnectionExt");
                        }
                    } catch (Exception e) {
                        Rlog.e(PROP_LOG_TAG, "Fail to create plug-in");
                        e.printStackTrace();
                    }
                } else {
                    this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(2), 3000L);
                }
            } else {
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(2), 2000L);
            }
        } else if (isWait(c)) {
            setPostDialState(Connection.PostDialState.WAIT);
        } else if (isWild(c)) {
            setPostDialState(Connection.PostDialState.WILD);
        } else {
            return false;
        }
        return true;
    }

    @Override
    public String getRemainingPostDialString() {
        String subStr = super.getRemainingPostDialString();
        if (!isPhoneTypeGsm() && !TextUtils.isEmpty(subStr)) {
            int wIndex = subStr.indexOf(59);
            int pIndex = subStr.indexOf(44);
            if (wIndex > 0 && (wIndex < pIndex || pIndex <= 0)) {
                return subStr.substring(0, wIndex);
            }
            if (pIndex > 0) {
                return subStr.substring(0, pIndex);
            }
            return subStr;
        }
        return subStr;
    }

    public void updateParent(GsmCdmaCall oldParent, GsmCdmaCall newParent) {
        if (newParent == oldParent) {
            return;
        }
        if (oldParent != null) {
            oldParent.detach(this);
        }
        newParent.attachFake(this, Call.State.ACTIVE);
        this.mParent = newParent;
    }

    protected void finalize() {
        if (this.mPartialWakeLock.isHeld()) {
            Rlog.e(LOG_TAG, "[GsmCdmaConn] UNEXPECTED; mPartialWakeLock is held when finalizing.");
        }
        clearPostDialListeners();
        releaseWakeLock();
    }

    private void processNextPostDialChar() {
        char c;
        Message notifyMessage;
        if (this.mPostDialState == Connection.PostDialState.CANCELLED) {
            releaseWakeLock();
            return;
        }
        if (this.mPostDialString == null || this.mPostDialString.length() <= this.mNextPostDialChar || this.mDisconnected) {
            setPostDialState(Connection.PostDialState.COMPLETE);
            releaseWakeLock();
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
        Registrant postDialHandler = this.mOwner.getPhone().getPostDialHandler();
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

    private boolean isConnectingInOrOut() {
        return this.mParent == null || this.mParent == this.mOwner.mRingingCall || this.mParent.mState == Call.State.DIALING || this.mParent.mState == Call.State.ALERTING;
    }

    private GsmCdmaCall parentFromDCState(DriverCall.State state) {
        switch (m15xd9c92f69()[state.ordinal()]) {
            case 1:
            case 2:
            case 3:
                return this.mOwner.mForegroundCall;
            case 4:
                return this.mOwner.mBackgroundCall;
            case 5:
            case 6:
                return this.mOwner.mRingingCall;
            default:
                throw new RuntimeException("illegal call state: " + state);
        }
    }

    private void setPostDialState(Connection.PostDialState s) {
        if (s == Connection.PostDialState.STARTED || s == Connection.PostDialState.PAUSE) {
            synchronized (this.mPartialWakeLock) {
                if (this.mPartialWakeLock.isHeld()) {
                    this.mHandler.removeMessages(4);
                } else {
                    acquireWakeLock();
                }
                Message msg = this.mHandler.obtainMessage(4);
                this.mHandler.sendMessageDelayed(msg, 60000L);
            }
        } else {
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
        log("acquireWakeLock, " + hashCode());
        this.mPartialWakeLock.acquire();
    }

    private void releaseWakeLock() {
        synchronized (this.mPartialWakeLock) {
            if (this.mPartialWakeLock.isHeld()) {
                log("releaseWakeLock, " + hashCode());
                this.mPartialWakeLock.release();
            }
        }
    }

    private void releaseAllWakeLocks() {
        synchronized (this.mPartialWakeLock) {
            while (this.mPartialWakeLock.isHeld()) {
                this.mPartialWakeLock.release();
            }
        }
    }

    private static boolean isPause(char c) {
        return c == ',';
    }

    private static boolean isWait(char c) {
        return c == ';';
    }

    private static boolean isWild(char c) {
        return c == 'N';
    }

    private static int findNextPCharOrNonPOrNonWCharIndex(String phoneNumber, int currIndex) {
        boolean wMatched = isWait(phoneNumber.charAt(currIndex));
        int index = currIndex + 1;
        int length = phoneNumber.length();
        while (index < length) {
            char cNext = phoneNumber.charAt(index);
            if (isWait(cNext)) {
                wMatched = true;
            }
            if (!isWait(cNext) && !isPause(cNext)) {
                break;
            }
            index++;
        }
        if (index < length && index > currIndex + 1 && !wMatched && isPause(phoneNumber.charAt(currIndex))) {
            return currIndex + 1;
        }
        return index;
    }

    private static char findPOrWCharToAppend(String phoneNumber, int currPwIndex, int nextNonPwCharIndex) {
        char c = phoneNumber.charAt(currPwIndex);
        char ret = isPause(c) ? ',' : ';';
        if (nextNonPwCharIndex > currPwIndex + 1) {
            return ';';
        }
        return ret;
    }

    private String maskDialString(String dialString) {
        if (VDBG) {
            return dialString;
        }
        return "<MASKED>";
    }

    private void fetchDtmfToneDelay(GsmCdmaPhone phone) {
        CarrierConfigManager configMgr = (CarrierConfigManager) phone.getContext().getSystemService("carrier_config");
        PersistableBundle b = configMgr.getConfigForSubId(phone.getSubId());
        if (b == null) {
            return;
        }
        this.mDtmfToneDelay = b.getInt(phone.getDtmfToneDelayKey());
    }

    private boolean isPhoneTypeGsm() {
        return this.mOwner.getPhone().getPhoneType() == 1;
    }

    private void log(String msg) {
        Rlog.d(LOG_TAG, "[GsmCdmaConn] " + msg);
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
    public int getPreciseDisconnectCause() {
        return this.mPreciseCause;
    }

    @Override
    public String getVendorDisconnectCause() {
        return this.mVendorCause;
    }

    @Override
    public void migrateFrom(Connection c) {
        if (c == null) {
            return;
        }
        super.migrateFrom(c);
        this.mUusInfo = c.getUUSInfo();
        setUserData(c.getUserData());
    }

    @Override
    public Connection getOrigConnection() {
        return this.mOrigConnection;
    }

    @Override
    public boolean isMultiparty() {
        if (this.mParent != null) {
            return this.mParent.isMultiparty();
        }
        return false;
    }

    public void onReplaceDisconnect(int cause) {
        this.mCause = cause;
        if (!this.mDisconnected) {
            this.mIndex = -1;
            this.mDisconnectTime = System.currentTimeMillis();
            this.mDuration = SystemClock.elapsedRealtime() - this.mConnectTimeReal;
            this.mDisconnected = true;
            log("onReplaceDisconnect: cause=" + cause);
            if (this.mParent != null) {
                this.mParent.connectionDisconnected(this);
            }
        }
        releaseWakeLock();
    }

    public boolean isRealConnected() {
        return this.mIsRealConnected;
    }

    boolean onCdmaCallAccept() {
        proprietaryLog("onCdmaCallAccept, mIsRealConnected:" + this.mIsRealConnected + ", state:" + getState());
        if (getState() != Call.State.ACTIVE) {
            this.mReceivedAccepted = true;
            return false;
        }
        this.mConnectTimeReal = SystemClock.elapsedRealtime();
        this.mDuration = 0L;
        this.mConnectTime = System.currentTimeMillis();
        if (!this.mIsRealConnected) {
            this.mIsRealConnected = true;
            processNextPostDialChar();
            vibrateForAccepted();
        }
        return true;
    }

    private boolean isInChina() {
        String numeric = TelephonyManager.getDefault().getNetworkOperatorForPhone(this.mOwner.mPhone.getPhoneId());
        proprietaryLog("isInChina, numeric:" + numeric);
        return numeric.indexOf(RadioCapabilitySwitchUtil.CN_MCC) == 0;
    }

    private void vibrateForAccepted() {
        Vibrator vibrator = (Vibrator) this.mOwner.mPhone.getContext().getSystemService("vibrator");
        vibrator.vibrate(200L);
    }

    @Override
    public boolean isVideo() {
        proprietaryLog("GsmConnection: isVideo = " + this.mIsVideo);
        return this.mIsVideo;
    }

    void updateConferenceParticipantAddress(String address) {
        this.mAddress = address;
    }

    void proprietaryLog(String s) {
        Rlog.d(PROP_LOG_TAG, s);
    }
}
