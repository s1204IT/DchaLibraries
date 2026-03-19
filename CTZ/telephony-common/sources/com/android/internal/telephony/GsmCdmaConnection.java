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
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.text.TextUtils;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.DriverCall;
import com.android.internal.telephony.cdma.CdmaCallWaitingNotification;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.UiccCardApplication;

public class GsmCdmaConnection extends Connection {
    private static final boolean DBG = true;
    static final int EVENT_DTMF_DELAY_DONE = 5;
    static final int EVENT_DTMF_DONE = 1;
    protected static final int EVENT_NEXT_POST_DIAL = 3;
    static final int EVENT_PAUSE_DONE = 2;
    static final int EVENT_WAKE_LOCK_TIMEOUT = 4;
    protected static final String LOG_TAG = "GsmCdmaConnection";
    static final int PAUSE_DELAY_MILLIS_CDMA = 2000;
    static final int PAUSE_DELAY_MILLIS_GSM = 3000;
    private static final boolean VDBG = false;
    static final int WAKE_LOCK_TIMEOUT_MILLIS = 60000;
    protected long mDisconnectTime;
    public boolean mDisconnected;
    private int mDtmfToneDelay;
    protected Handler mHandler;
    public int mIndex;
    protected boolean mIsEmergencyCall;
    public Connection mOrigConnection;
    public GsmCdmaCallTracker mOwner;
    protected GsmCdmaCall mParent;
    private PowerManager.WakeLock mPartialWakeLock;
    int mPreciseCause;
    UUSInfo mUusInfo;
    String mVendorCause;

    class MyHandler extends Handler {
        MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
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

    public GsmCdmaConnection(GsmCdmaPhone gsmCdmaPhone, DriverCall driverCall, GsmCdmaCallTracker gsmCdmaCallTracker, int i) {
        super(gsmCdmaPhone.getPhoneType());
        this.mPreciseCause = 0;
        this.mIsEmergencyCall = false;
        this.mDtmfToneDelay = 0;
        createWakeLock(gsmCdmaPhone.getContext());
        acquireWakeLock();
        this.mOwner = gsmCdmaCallTracker;
        this.mHandler = new MyHandler(this.mOwner.getLooper());
        this.mAddress = driverCall.number;
        this.mIsEmergencyCall = PhoneNumberUtils.isLocalEmergencyNumber(gsmCdmaPhone.getContext(), this.mAddress);
        this.mIsIncoming = driverCall.isMT;
        this.mCreateTime = System.currentTimeMillis();
        this.mCnapName = driverCall.name;
        this.mCnapNamePresentation = driverCall.namePresentation;
        this.mNumberPresentation = driverCall.numberPresentation;
        this.mUusInfo = driverCall.uusInfo;
        this.mIndex = i;
        this.mParent = parentFromDCState(driverCall.state);
        this.mParent.attach(this, driverCall);
        fetchDtmfToneDelay(gsmCdmaPhone);
        setAudioQuality(getAudioQualityFromDC(driverCall.audioQuality));
    }

    public GsmCdmaConnection(GsmCdmaPhone gsmCdmaPhone, String str, GsmCdmaCallTracker gsmCdmaCallTracker, GsmCdmaCall gsmCdmaCall, boolean z) {
        super(gsmCdmaPhone.getPhoneType());
        this.mPreciseCause = 0;
        this.mIsEmergencyCall = false;
        this.mDtmfToneDelay = 0;
        createWakeLock(gsmCdmaPhone.getContext());
        acquireWakeLock();
        this.mOwner = gsmCdmaCallTracker;
        this.mHandler = new MyHandler(this.mOwner.getLooper());
        if (isPhoneTypeGsm()) {
            this.mDialString = str;
        } else {
            Rlog.d(LOG_TAG, "[GsmCdmaConn] GsmCdmaConnection: dialString=" + maskDialString(str));
            str = formatDialString(str);
            Rlog.d(LOG_TAG, "[GsmCdmaConn] GsmCdmaConnection:formated dialString=" + maskDialString(str));
        }
        this.mAddress = PhoneNumberUtils.extractNetworkPortionAlt(str);
        this.mIsEmergencyCall = z;
        this.mPostDialString = PhoneNumberUtils.extractPostDialPortion(str);
        this.mIndex = -1;
        this.mIsIncoming = false;
        this.mCnapName = null;
        this.mCnapNamePresentation = 1;
        this.mNumberPresentation = 1;
        this.mCreateTime = System.currentTimeMillis();
        if (gsmCdmaCall != null) {
            this.mParent = gsmCdmaCall;
            if (!isPhoneTypeGsm() && gsmCdmaCall.mState == Call.State.ACTIVE) {
                gsmCdmaCall.attachFake(this, Call.State.ACTIVE);
            } else {
                gsmCdmaCall.attachFake(this, Call.State.DIALING);
            }
        }
        fetchDtmfToneDelay(gsmCdmaPhone);
    }

    public GsmCdmaConnection(Context context, CdmaCallWaitingNotification cdmaCallWaitingNotification, GsmCdmaCallTracker gsmCdmaCallTracker, GsmCdmaCall gsmCdmaCall) {
        super(gsmCdmaCall.getPhone().getPhoneType());
        this.mPreciseCause = 0;
        this.mIsEmergencyCall = false;
        this.mDtmfToneDelay = 0;
        createWakeLock(context);
        acquireWakeLock();
        this.mOwner = gsmCdmaCallTracker;
        this.mHandler = new MyHandler(this.mOwner.getLooper());
        this.mAddress = cdmaCallWaitingNotification.number;
        this.mNumberPresentation = cdmaCallWaitingNotification.numberPresentation;
        this.mCnapName = cdmaCallWaitingNotification.name;
        this.mCnapNamePresentation = cdmaCallWaitingNotification.namePresentation;
        this.mIndex = -1;
        this.mIsIncoming = true;
        this.mCreateTime = System.currentTimeMillis();
        this.mConnectTime = 0L;
        this.mParent = gsmCdmaCall;
        gsmCdmaCall.attachFake(this, Call.State.WAITING);
    }

    public void dispose() {
        clearPostDialListeners();
        if (this.mParent != null) {
            this.mParent.detach(this);
        }
        releaseAllWakeLocks();
    }

    static boolean equalsHandlesNulls(Object obj, Object obj2) {
        return obj == null ? obj2 == null : obj.equals(obj2);
    }

    static boolean equalsBaseDialString(String str, String str2) {
        if (str == null) {
            if (str2 != null) {
                return false;
            }
        } else if (str2 == null || !str.startsWith(str2)) {
            return false;
        }
        return true;
    }

    public static String formatDialString(String str) {
        if (str == null) {
            return null;
        }
        int length = str.length();
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < length) {
            char cCharAt = str.charAt(i);
            if (isPause(cCharAt) || isWait(cCharAt)) {
                int i2 = length - 1;
                if (i < i2) {
                    int iFindNextPCharOrNonPOrNonWCharIndex = findNextPCharOrNonPOrNonWCharIndex(str, i);
                    if (iFindNextPCharOrNonPOrNonWCharIndex < length) {
                        sb.append(findPOrWCharToAppend(str, i, iFindNextPCharOrNonPOrNonWCharIndex));
                        if (iFindNextPCharOrNonPOrNonWCharIndex > i + 1) {
                            i = iFindNextPCharOrNonPOrNonWCharIndex - 1;
                        }
                    } else if (iFindNextPCharOrNonPOrNonWCharIndex == length) {
                        i = i2;
                    }
                }
            } else {
                sb.append(cCharAt);
            }
            i++;
        }
        return PhoneNumberUtils.cdmaCheckAndProcessPlusCode(sb.toString());
    }

    public boolean compareTo(DriverCall driverCall) {
        if (!this.mIsIncoming && !driverCall.isMT) {
            return true;
        }
        if (!isPhoneTypeGsm() || this.mOrigConnection == null) {
            return this.mIsIncoming == driverCall.isMT && equalsHandlesNulls(this.mAddress, PhoneNumberUtils.stringFromStringAndTOA(driverCall.number, driverCall.TOA));
        }
        return true;
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
        throw new CallStateException("disconnected");
    }

    @Override
    public void deflect(String str) throws CallStateException {
        throw new CallStateException("deflect is not supported for CS");
    }

    @Override
    public void separate() throws CallStateException {
        if (!this.mDisconnected) {
            this.mOwner.separate(this);
            return;
        }
        throw new CallStateException("disconnected");
    }

    @Override
    public void proceedAfterWaitChar() {
        if (this.mPostDialState != Connection.PostDialState.WAIT) {
            Rlog.w(LOG_TAG, "GsmCdmaConnection.proceedAfterWaitChar(): Expected getPostDialState() to be WAIT but was " + this.mPostDialState);
            return;
        }
        setPostDialState(Connection.PostDialState.STARTED);
        processNextPostDialChar();
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
        log("proceedAfterWildChar: new postDialString is " + this.mPostDialString);
        processNextPostDialChar();
    }

    @Override
    public void cancelPostDial() {
        setPostDialState(Connection.PostDialState.CANCELLED);
    }

    public void onHangupLocal() {
        this.mCause = 3;
        this.mPreciseCause = 0;
        this.mVendorCause = null;
    }

    protected int disconnectCauseFromCode(int i) {
        switch (i) {
            case 41:
            case 42:
                return 5;
            default:
                switch (i) {
                    case 240:
                        return 20;
                    case 241:
                        return 21;
                    default:
                        switch (i) {
                            case 243:
                                return 58;
                            case 244:
                                return 46;
                            case 245:
                                return 47;
                            case 246:
                                return 48;
                            default:
                                switch (i) {
                                    case CallFailCause.EMERGENCY_TEMP_FAILURE:
                                        return 63;
                                    case CallFailCause.EMERGENCY_PERM_FAILURE:
                                        return 64;
                                    default:
                                        switch (i) {
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
                                            default:
                                                switch (i) {
                                                    case 1:
                                                        return 25;
                                                    case 8:
                                                        return 20;
                                                    case 17:
                                                        return 4;
                                                    case 19:
                                                        return 13;
                                                    case 31:
                                                        return 65;
                                                    case 34:
                                                    case 44:
                                                    case 49:
                                                    case 58:
                                                        return 5;
                                                    case 68:
                                                        return 15;
                                                    default:
                                                        GsmCdmaPhone phone = this.mOwner.getPhone();
                                                        int state = phone.getServiceState().getState();
                                                        UiccCardApplication uiccCardApplication = phone.getUiccCardApplication();
                                                        IccCardApplicationStatus.AppState state2 = uiccCardApplication != null ? uiccCardApplication.getState() : IccCardApplicationStatus.AppState.APPSTATE_UNKNOWN;
                                                        if (state == 3) {
                                                            return 17;
                                                        }
                                                        if (!this.mIsEmergencyCall) {
                                                            if (state == 1 || state == 2) {
                                                                return 18;
                                                            }
                                                            if (state2 != IccCardApplicationStatus.AppState.APPSTATE_READY && (isPhoneTypeGsm() || phone.mCdmaSubscriptionSource == 0)) {
                                                                return 19;
                                                            }
                                                        }
                                                        if (isPhoneTypeGsm() && i == 65535) {
                                                            if (phone.mSST.mRestrictedState.isCsRestricted()) {
                                                                return 22;
                                                            }
                                                            if (phone.mSST.mRestrictedState.isCsEmergencyRestricted()) {
                                                                return 24;
                                                            }
                                                            if (phone.mSST.mRestrictedState.isCsNormalRestricted()) {
                                                                return 23;
                                                            }
                                                        }
                                                        return i == 16 ? 2 : 36;
                                                }
                                        }
                                }
                        }
                }
        }
    }

    void onRemoteDisconnect(int i, String str) {
        this.mPreciseCause = i;
        this.mVendorCause = str;
        onDisconnect(disconnectCauseFromCode(i));
    }

    @Override
    public boolean onDisconnect(int i) {
        this.mCause = i;
        if (!this.mDisconnected) {
            doDisconnect();
            Rlog.d(LOG_TAG, "onDisconnect: cause=" + i);
            this.mOwner.getPhone().notifyDisconnect(this);
            notifyDisconnect(i);
            zConnectionDisconnected = this.mParent != null ? this.mParent.connectionDisconnected(this) : false;
            this.mOrigConnection = null;
        }
        clearPostDialListeners();
        releaseWakeLock();
        return zConnectionDisconnected;
    }

    public void onLocalDisconnect() {
        if (!this.mDisconnected) {
            doDisconnect();
            if (this.mParent != null) {
                this.mParent.detach(this);
            }
        }
        releaseWakeLock();
    }

    public boolean update(DriverCall driverCall) {
        boolean z;
        int audioQualityFromDC;
        boolean z2;
        boolean zIsConnectingInOrOut = isConnectingInOrOut();
        boolean z3 = getState() == Call.State.HOLDING;
        GsmCdmaCall gsmCdmaCallParentFromDCState = parentFromDCState(driverCall.state);
        log("parent= " + this.mParent + ", newParent= " + gsmCdmaCallParentFromDCState);
        if (!isPhoneTypeGsm() || this.mOrigConnection == null) {
            if (isIncoming() && !equalsBaseDialString(this.mAddress, driverCall.number) && (!this.mNumberConverted || !equalsBaseDialString(this.mConvertedNumber, driverCall.number))) {
                log("update: phone # changed!");
                this.mAddress = driverCall.number;
                z = true;
            }
            audioQualityFromDC = getAudioQualityFromDC(driverCall.audioQuality);
            if (getAudioQuality() != audioQualityFromDC) {
                StringBuilder sb = new StringBuilder();
                sb.append("update: audioQuality # changed!:  ");
                sb.append(audioQualityFromDC == 2 ? "high" : "standard");
                log(sb.toString());
                setAudioQuality(audioQualityFromDC);
                z = true;
            }
            if (TextUtils.isEmpty(driverCall.name)) {
                if (!driverCall.name.equals(this.mCnapName)) {
                    this.mCnapName = driverCall.name;
                    z = true;
                }
            } else if (!TextUtils.isEmpty(this.mCnapName)) {
                this.mCnapName = "";
                z = true;
            }
            log("--dssds----" + this.mCnapName);
            this.mCnapNamePresentation = driverCall.namePresentation;
            this.mNumberPresentation = driverCall.numberPresentation;
            if (gsmCdmaCallParentFromDCState != this.mParent) {
                boolean zUpdate = this.mParent.update(this, driverCall);
                if (!z && !zUpdate) {
                    z2 = false;
                }
                StringBuilder sb2 = new StringBuilder();
                sb2.append("update: parent=");
                sb2.append(this.mParent);
                sb2.append(", hasNewParent=");
                sb2.append(gsmCdmaCallParentFromDCState != this.mParent);
                sb2.append(", wasConnectingInOrOut=");
                sb2.append(zIsConnectingInOrOut);
                sb2.append(", wasHolding=");
                sb2.append(z3);
                sb2.append(", isConnectingInOrOut=");
                sb2.append(isConnectingInOrOut());
                sb2.append(", changed=");
                sb2.append(z2);
                log(sb2.toString());
                if (zIsConnectingInOrOut && !isConnectingInOrOut()) {
                    onConnectedInOrOut();
                }
                if (z2 && !z3 && getState() == Call.State.HOLDING) {
                    onStartedHolding();
                }
                return z2;
            }
            if (this.mParent != null) {
                this.mParent.detach(this);
            }
            gsmCdmaCallParentFromDCState.attach(this, driverCall);
            this.mParent = gsmCdmaCallParentFromDCState;
            z2 = true;
            StringBuilder sb22 = new StringBuilder();
            sb22.append("update: parent=");
            sb22.append(this.mParent);
            sb22.append(", hasNewParent=");
            sb22.append(gsmCdmaCallParentFromDCState != this.mParent);
            sb22.append(", wasConnectingInOrOut=");
            sb22.append(zIsConnectingInOrOut);
            sb22.append(", wasHolding=");
            sb22.append(z3);
            sb22.append(", isConnectingInOrOut=");
            sb22.append(isConnectingInOrOut());
            sb22.append(", changed=");
            sb22.append(z2);
            log(sb22.toString());
            if (zIsConnectingInOrOut) {
                onConnectedInOrOut();
            }
            if (z2) {
                onStartedHolding();
            }
            return z2;
        }
        log("update: mOrigConnection is not null");
        z = false;
        audioQualityFromDC = getAudioQualityFromDC(driverCall.audioQuality);
        if (getAudioQuality() != audioQualityFromDC) {
        }
        if (TextUtils.isEmpty(driverCall.name)) {
        }
        log("--dssds----" + this.mCnapName);
        this.mCnapNamePresentation = driverCall.namePresentation;
        this.mNumberPresentation = driverCall.numberPresentation;
        if (gsmCdmaCallParentFromDCState != this.mParent) {
        }
        z2 = true;
        StringBuilder sb222 = new StringBuilder();
        sb222.append("update: parent=");
        sb222.append(this.mParent);
        sb222.append(", hasNewParent=");
        sb222.append(gsmCdmaCallParentFromDCState != this.mParent);
        sb222.append(", wasConnectingInOrOut=");
        sb222.append(zIsConnectingInOrOut);
        sb222.append(", wasHolding=");
        sb222.append(z3);
        sb222.append(", isConnectingInOrOut=");
        sb222.append(isConnectingInOrOut());
        sb222.append(", changed=");
        sb222.append(z2);
        log(sb222.toString());
        if (zIsConnectingInOrOut) {
        }
        if (z2) {
        }
        return z2;
    }

    void fakeHoldBeforeDial() {
        if (this.mParent != null) {
            this.mParent.detach(this);
        }
        this.mParent = this.mOwner.mBackgroundCall;
        this.mParent.attachFake(this, Call.State.HOLDING);
        onStartedHolding();
    }

    public int getGsmCdmaIndex() throws CallStateException {
        if (this.mIndex >= 0) {
            return this.mIndex + 1;
        }
        throw new CallStateException("GsmCdma index not yet assigned");
    }

    public void onConnectedInOrOut() {
        this.mConnectTime = System.currentTimeMillis();
        this.mConnectTimeReal = SystemClock.elapsedRealtime();
        this.mDuration = 0L;
        log("onConnectedInOrOut: connectTime=" + this.mConnectTime);
        if (!this.mIsIncoming) {
            processNextPostDialChar();
        } else {
            releaseWakeLock();
        }
    }

    private void doDisconnect() {
        this.mIndex = -1;
        this.mDisconnectTime = System.currentTimeMillis();
        this.mDuration = SystemClock.elapsedRealtime() - this.mConnectTimeReal;
        this.mDisconnected = true;
        clearPostDialListeners();
    }

    protected void onStartedHolding() {
        this.mHoldingStartTime = SystemClock.elapsedRealtime();
    }

    protected boolean processPostDialChar(char c) {
        if (PhoneNumberUtils.is12Key(c)) {
            this.mOwner.mCi.sendDtmf(c, this.mHandler.obtainMessage(1));
        } else if (isPause(c)) {
            if (!isPhoneTypeGsm()) {
                setPostDialState(Connection.PostDialState.PAUSE);
            }
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(2), isPhoneTypeGsm() ? 3000L : 2000L);
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
        String remainingPostDialString = super.getRemainingPostDialString();
        if (!isPhoneTypeGsm() && !TextUtils.isEmpty(remainingPostDialString)) {
            int iIndexOf = remainingPostDialString.indexOf(59);
            int iIndexOf2 = remainingPostDialString.indexOf(44);
            if (iIndexOf > 0 && (iIndexOf < iIndexOf2 || iIndexOf2 <= 0)) {
                return remainingPostDialString.substring(0, iIndexOf);
            }
            if (iIndexOf2 > 0) {
                return remainingPostDialString.substring(0, iIndexOf2);
            }
            return remainingPostDialString;
        }
        return remainingPostDialString;
    }

    public void updateParent(GsmCdmaCall gsmCdmaCall, GsmCdmaCall gsmCdmaCall2) {
        if (gsmCdmaCall2 != gsmCdmaCall) {
            if (gsmCdmaCall != null) {
                gsmCdmaCall.detach(this);
            }
            gsmCdmaCall2.attachFake(this, Call.State.ACTIVE);
            this.mParent = gsmCdmaCall2;
        }
    }

    protected void finalize() {
        if (this.mPartialWakeLock != null && this.mPartialWakeLock.isHeld()) {
            Rlog.e(LOG_TAG, "UNEXPECTED; mPartialWakeLock is held when finalizing.");
        }
        clearPostDialListeners();
        releaseWakeLock();
    }

    protected void processNextPostDialChar() {
        char cCharAt;
        Message messageMessageForRegistrant;
        if (this.mPostDialState == Connection.PostDialState.CANCELLED) {
            releaseWakeLock();
            return;
        }
        if (this.mPostDialString == null || this.mPostDialString.length() <= this.mNextPostDialChar) {
            setPostDialState(Connection.PostDialState.COMPLETE);
            releaseWakeLock();
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
        Registrant postDialHandler = this.mOwner.getPhone().getPostDialHandler();
        if (postDialHandler != null && (messageMessageForRegistrant = postDialHandler.messageForRegistrant()) != null) {
            Connection.PostDialState postDialState = this.mPostDialState;
            AsyncResult asyncResultForMessage = AsyncResult.forMessage(messageMessageForRegistrant);
            asyncResultForMessage.result = this;
            asyncResultForMessage.userObj = postDialState;
            messageMessageForRegistrant.arg1 = cCharAt;
            messageMessageForRegistrant.sendToTarget();
        }
    }

    protected boolean isConnectingInOrOut() {
        return this.mParent == null || this.mParent == this.mOwner.mRingingCall || this.mParent.mState == Call.State.DIALING || this.mParent.mState == Call.State.ALERTING;
    }

    protected GsmCdmaCall parentFromDCState(DriverCall.State state) {
        switch (state) {
            case ACTIVE:
            case DIALING:
            case ALERTING:
                return this.mOwner.mForegroundCall;
            case HOLDING:
                return this.mOwner.mBackgroundCall;
            case INCOMING:
            case WAITING:
                return this.mOwner.mRingingCall;
            default:
                throw new RuntimeException("illegal call state: " + state);
        }
    }

    protected int getAudioQualityFromDC(int i) {
        if (i == 2 || i == 9) {
            return 2;
        }
        return 1;
    }

    protected void setPostDialState(Connection.PostDialState postDialState) {
        if (postDialState == Connection.PostDialState.STARTED || postDialState == Connection.PostDialState.PAUSE) {
            synchronized (this.mPartialWakeLock) {
                if (this.mPartialWakeLock.isHeld()) {
                    this.mHandler.removeMessages(4);
                } else {
                    acquireWakeLock();
                }
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(4), 60000L);
            }
        } else {
            this.mHandler.removeMessages(4);
            releaseWakeLock();
        }
        this.mPostDialState = postDialState;
        notifyPostDialListeners();
    }

    private void createWakeLock(Context context) {
        this.mPartialWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, LOG_TAG);
    }

    private void acquireWakeLock() {
        if (this.mPartialWakeLock != null) {
            synchronized (this.mPartialWakeLock) {
                log("acquireWakeLock");
                this.mPartialWakeLock.acquire();
            }
        }
    }

    protected void releaseWakeLock() {
        if (this.mPartialWakeLock != null) {
            synchronized (this.mPartialWakeLock) {
                if (this.mPartialWakeLock.isHeld()) {
                    log("releaseWakeLock");
                    this.mPartialWakeLock.release();
                }
            }
        }
    }

    private void releaseAllWakeLocks() {
        if (this.mPartialWakeLock != null) {
            synchronized (this.mPartialWakeLock) {
                while (this.mPartialWakeLock.isHeld()) {
                    this.mPartialWakeLock.release();
                }
            }
        }
    }

    protected static boolean isPause(char c) {
        return c == ',';
    }

    protected static boolean isWait(char c) {
        return c == ';';
    }

    private static boolean isWild(char c) {
        return c == 'N';
    }

    protected static int findNextPCharOrNonPOrNonWCharIndex(String str, int i) {
        boolean zIsWait = isWait(str.charAt(i));
        int i2 = i + 1;
        int length = str.length();
        boolean z = zIsWait;
        int i3 = i2;
        while (i3 < length) {
            char cCharAt = str.charAt(i3);
            if (isWait(cCharAt)) {
                z = true;
            }
            if (!isWait(cCharAt) && !isPause(cCharAt)) {
                break;
            }
            i3++;
        }
        if (i3 < length && i3 > i2 && !z && isPause(str.charAt(i))) {
            return i2;
        }
        return i3;
    }

    protected static char findPOrWCharToAppend(String str, int i, int i2) {
        char c = isPause(str.charAt(i)) ? ',' : ';';
        if (i2 > i + 1) {
            return ';';
        }
        return c;
    }

    private String maskDialString(String str) {
        return "<MASKED>";
    }

    private void fetchDtmfToneDelay(GsmCdmaPhone gsmCdmaPhone) {
        PersistableBundle configForSubId = ((CarrierConfigManager) gsmCdmaPhone.getContext().getSystemService("carrier_config")).getConfigForSubId(gsmCdmaPhone.getSubId());
        if (configForSubId != null) {
            this.mDtmfToneDelay = configForSubId.getInt(gsmCdmaPhone.getDtmfToneDelayKey());
        }
    }

    protected boolean isPhoneTypeGsm() {
        return this.mOwner.getPhone().getPhoneType() == 1;
    }

    protected void log(String str) {
        Rlog.d(LOG_TAG, "[GsmCdmaConn] " + str);
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
    public void migrateFrom(Connection connection) {
        if (connection == null) {
            return;
        }
        super.migrateFrom(connection);
        this.mUusInfo = connection.getUUSInfo();
        setUserData(connection.getUserData());
    }

    @Override
    public Connection getOrigConnection() {
        return this.mOrigConnection;
    }

    @Override
    public boolean isMultiparty() {
        if (this.mOrigConnection != null) {
            return this.mOrigConnection.isMultiparty();
        }
        return false;
    }
}
