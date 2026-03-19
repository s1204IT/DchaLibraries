package com.mediatek.internal.telephony;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Message;
import android.os.Registrant;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Vibrator;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.DriverCall;
import com.android.internal.telephony.GsmCdmaCall;
import com.android.internal.telephony.GsmCdmaCallTracker;
import com.android.internal.telephony.GsmCdmaConnection;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.TelephonyDevController;
import com.android.internal.telephony.cdma.CdmaCallWaitingNotification;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.mediatek.internal.telephony.datasub.DataSubConstants;
import com.mediatek.telephony.MtkTelephonyManagerEx;

public class MtkGsmCdmaConnection extends GsmCdmaConnection {
    private static final int MO_CALL_VIBRATE_TIME = 200;
    private static final String PROPERTY_HD_VOICE_STATUS = "vendor.audiohal.ril.hd.voice.status";
    private static final String PROP_LOG_TAG = "GsmCdmaConn";
    private static final boolean VDBG;
    String mForwardingAddress;
    private boolean mIsRealConnected;
    private boolean mReceivedAccepted;
    String mRedirectingAddress;
    TelephonyDevController mTelDevController;

    static {
        VDBG = SystemProperties.getInt("persist.vendor.log.tel_dbg", 0) == 1;
    }

    private boolean hasC2kOverImsModem() {
        return (this.mTelDevController == null || this.mTelDevController.getModem(0) == null || !((MtkHardwareConfig) this.mTelDevController.getModem(0)).hasC2kOverImsModem()) ? false : true;
    }

    public MtkGsmCdmaConnection(GsmCdmaPhone gsmCdmaPhone, String str, GsmCdmaCallTracker gsmCdmaCallTracker, GsmCdmaCall gsmCdmaCall, boolean z) {
        super(gsmCdmaPhone, str, gsmCdmaCallTracker, gsmCdmaCall, z);
        this.mTelDevController = TelephonyDevController.getInstance();
        this.mIsRealConnected = false;
        this.mReceivedAccepted = false;
    }

    public MtkGsmCdmaConnection(GsmCdmaPhone gsmCdmaPhone, DriverCall driverCall, GsmCdmaCallTracker gsmCdmaCallTracker, int i) {
        super(gsmCdmaPhone, driverCall, gsmCdmaCallTracker, i);
        this.mTelDevController = TelephonyDevController.getInstance();
        MtkGsmCdmaCallTracker mtkGsmCdmaCallTracker = (MtkGsmCdmaCallTracker) gsmCdmaCallTracker;
        String strConvertAddress = mtkGsmCdmaCallTracker.mMtkGsmCdmaCallTrackerExt.convertAddress(this.mAddress);
        if (strConvertAddress != null) {
            setConnectionExtras(mtkGsmCdmaCallTracker.mMtkGsmCdmaCallTrackerExt.getAddressExtras(this.mAddress));
            this.mNumberConverted = true;
            this.mConvertedNumber = this.mAddress;
            this.mAddress = strConvertAddress;
        }
        if (hasC2kOverImsModem() && (!TelephonyManager.getDefault().hasIccCard(gsmCdmaPhone.getPhoneId()) || gsmCdmaPhone.getServiceState().getState() != 0)) {
            this.mIsEmergencyCall = PhoneNumberUtils.isLocalEmergencyNumber(gsmCdmaPhone.getContext(), this.mAddress);
        } else {
            this.mIsEmergencyCall = PhoneNumberUtils.isLocalEmergencyNumber(gsmCdmaPhone.getContext(), gsmCdmaPhone.getSubId(), this.mAddress);
        }
    }

    public MtkGsmCdmaConnection(Context context, CdmaCallWaitingNotification cdmaCallWaitingNotification, GsmCdmaCallTracker gsmCdmaCallTracker, GsmCdmaCall gsmCdmaCall) {
        super(context, cdmaCallWaitingNotification, gsmCdmaCallTracker, gsmCdmaCall);
        this.mTelDevController = TelephonyDevController.getInstance();
    }

    public boolean compareTo(DriverCall driverCall) {
        if (!this.mIsIncoming && !driverCall.isMT) {
            return true;
        }
        if (!isPhoneTypeGsm() || this.mOrigConnection == null) {
            return this.mIsIncoming == driverCall.isMT && !((MtkGsmCdmaCallTracker) this.mOwner).mMtkGsmCdmaCallTrackerExt.isAddressChanged(this.mNumberConverted, this.mAddress, PhoneNumberUtils.stringFromStringAndTOA(driverCall.number, driverCall.TOA));
        }
        return true;
    }

    public String getForwardingAddress() {
        return this.mForwardingAddress;
    }

    public void setForwardingAddress(String str) {
        this.mForwardingAddress = str;
    }

    public String getRedirectingAddress() {
        return this.mRedirectingAddress;
    }

    public void setRedirectingAddress(String str) {
        this.mRedirectingAddress = str;
    }

    protected int getAudioQualityFromDC(int i) {
        String str = SystemProperties.get(DataSubConstants.PROPERTY_OPERATOR_OPTR, "OM");
        log("isHighDefAudio, optr:" + str);
        String strConcat = str.concat("=");
        String str2 = SystemProperties.get(PROPERTY_HD_VOICE_STATUS, "");
        if (str2 == null || str2.equals("")) {
            return (i == 2 || i == 8) ? 2 : 1;
        }
        log("HD voice status: " + str2);
        int iIndexOf = 0;
        boolean z = str2.indexOf(strConcat) != -1;
        boolean z2 = str2.indexOf("OM=") != -1;
        if (z && !strConcat.equals("OM=")) {
            iIndexOf = str2.indexOf(strConcat) + strConcat.length();
        } else if (z2) {
            iIndexOf = str2.indexOf("OM=") + 3;
        }
        int i2 = iIndexOf + 1;
        return (str2.length() > i2 ? str2.substring(iIndexOf, i2) : "").equals("Y") ? 2 : 1;
    }

    public void setNumberPresentation(int i) {
        this.mNumberPresentation = i;
    }

    public void onReplaceDisconnect(int i) {
        this.mCause = i;
        if (!this.mDisconnected) {
            this.mIndex = -1;
            this.mDisconnectTime = System.currentTimeMillis();
            this.mDuration = SystemClock.elapsedRealtime() - this.mConnectTimeReal;
            this.mDisconnected = true;
            log("onReplaceDisconnect: cause=" + i);
            if (this.mParent != null) {
                this.mParent.connectionDisconnected(this);
            }
        }
        releaseWakeLock();
    }

    protected int disconnectCauseFromCode(int i) {
        switch (i) {
            case 25:
                break;
            case 26:
                break;
            case 27:
                break;
            case 28:
                break;
            case 29:
                break;
            default:
                switch (i) {
                    case 69:
                        break;
                    case 70:
                        break;
                    default:
                        switch (i) {
                            case 87:
                                break;
                            case 88:
                                break;
                            default:
                                switch (i) {
                                    case 95:
                                        break;
                                    case 96:
                                        break;
                                    case 97:
                                        break;
                                    case 98:
                                        break;
                                    case 99:
                                        break;
                                    case 100:
                                        break;
                                    case 101:
                                        break;
                                    case 102:
                                        break;
                                    default:
                                        switch (i) {
                                            case 3:
                                                break;
                                            case 6:
                                                break;
                                            case 8:
                                                break;
                                            case 18:
                                                break;
                                            case 38:
                                            case 63:
                                                break;
                                            case 43:
                                                break;
                                            case 47:
                                                break;
                                            case 50:
                                                break;
                                            case 55:
                                                break;
                                            case 57:
                                                break;
                                            case 65:
                                                break;
                                            case 81:
                                                break;
                                            case 91:
                                                break;
                                            case 111:
                                                break;
                                            case 243:
                                                if (MtkPhoneNumberUtils.isEmergencyNumber(getAddress())) {
                                                }
                                                break;
                                            case MtkCallFailCause.CM_MM_RR_CONNECTION_RELEASE:
                                                break;
                                        }
                                        GsmCdmaPhone phone = this.mOwner.getPhone();
                                        int state = phone.getServiceState().getState();
                                        UiccCardApplication uiccCardApplication = phone.getUiccCardApplication();
                                        proprietaryLog("disconnectCauseFromCode, causeCode:" + i + ", cardApp:" + uiccCardApplication + ", serviceState:" + state + ", uiccAppState:" + (uiccCardApplication != null ? uiccCardApplication.getState() : IccCardApplicationStatus.AppState.APPSTATE_UNKNOWN));
                                        if (hasC2kOverImsModem() && !this.mIsEmergencyCall) {
                                            this.mIsEmergencyCall = MtkTelephonyManagerEx.getDefault().isEccInProgress();
                                        }
                                        if (i == 0) {
                                            if (!this.mIsEmergencyCall) {
                                                i = 65535;
                                            }
                                        }
                                        if (!this.mIsEmergencyCall || (i != 31 && i != 79)) {
                                        }
                                        break;
                                }
                                break;
                        }
                        break;
                }
                break;
        }
        return 2;
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
            log(" mNumberConverted " + this.mNumberConverted);
            if (((MtkGsmCdmaCallTracker) this.mOwner).mMtkGsmCdmaCallTrackerExt.isAddressChanged(this.mNumberConverted, driverCall.number, this.mAddress, this.mConvertedNumber)) {
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
            if (!TextUtils.isEmpty(driverCall.name) && !driverCall.name.equals(this.mCnapName)) {
                this.mCnapName = driverCall.name;
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
                if (!isPhoneTypeGsm()) {
                    log("state=" + getState() + ", mReceivedAccepted=" + this.mReceivedAccepted);
                    if (getState() == Call.State.ACTIVE && this.mReceivedAccepted) {
                        onCdmaCallAccepted();
                        this.mReceivedAccepted = false;
                    }
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
            if (!isPhoneTypeGsm()) {
            }
            return z2;
        }
        log("update: mOrigConnection is not null");
        z = false;
        audioQualityFromDC = getAudioQualityFromDC(driverCall.audioQuality);
        if (getAudioQuality() != audioQualityFromDC) {
        }
        if (!TextUtils.isEmpty(driverCall.name)) {
            this.mCnapName = driverCall.name;
            z = true;
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
        if (!isPhoneTypeGsm()) {
        }
        return z2;
    }

    protected void processNextPostDialChar() {
        char cCharAt;
        Message messageMessageForRegistrant;
        if (this.mPostDialState == Connection.PostDialState.CANCELLED) {
            releaseWakeLock();
            return;
        }
        if (this.mPostDialString == null || this.mPostDialString.length() <= this.mNextPostDialChar || this.mDisconnected) {
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
                Rlog.e("GsmCdmaConnection", "processNextPostDialChar: c=" + cCharAt + " isn't valid!");
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

    public boolean isRealConnected() {
        return this.mIsRealConnected;
    }

    boolean onCdmaCallAccepted() {
        log("onCdmaCallAccepted, mIsRealConnected=" + this.mIsRealConnected + ", state=" + getState());
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
        String networkOperatorForPhone = TelephonyManager.getDefault().getNetworkOperatorForPhone(this.mParent.getPhone().getPhoneId());
        log("isInChina, numeric=" + networkOperatorForPhone);
        return networkOperatorForPhone.indexOf(RadioCapabilitySwitchUtil.CN_MCC) == 0;
    }

    private void vibrateForAccepted() {
        if ("0".equals(SystemProperties.get("persist.vendor.radio.telecom.vibrate", "1"))) {
            log("vibrateForAccepted, disabled by Engineer Mode");
        } else {
            ((Vibrator) this.mParent.getPhone().getContext().getSystemService("vibrator")).vibrate(200L);
        }
    }

    public void onConnectedInOrOut() {
        this.mConnectTime = System.currentTimeMillis();
        this.mConnectTimeReal = SystemClock.elapsedRealtime();
        this.mDuration = 0L;
        log("onConnectedInOrOut: connectTime=" + this.mConnectTime);
        if (!this.mIsIncoming) {
            if (isPhoneTypeGsm()) {
                processNextPostDialChar();
                return;
            }
            int size = this.mParent.mConnections.size();
            log("mParent.mConnections.size()=" + size);
            if (!isInChina() && !this.mIsRealConnected && size == 1) {
                this.mIsRealConnected = true;
                processNextPostDialChar();
                vibrateForAccepted();
            }
            if (size > 1) {
                this.mIsRealConnected = true;
                processNextPostDialChar();
                return;
            }
            return;
        }
        releaseWakeLock();
    }

    void proprietaryLog(String str) {
        Rlog.d(PROP_LOG_TAG, str);
    }

    void resumeHoldAfterDialFailed() {
        if (this.mParent != null) {
            this.mParent.detach(this);
        }
        this.mParent = this.mOwner.mForegroundCall;
        this.mParent.attachFake(this, Call.State.ACTIVE);
    }

    void updateConferenceParticipantAddress(String str) {
        this.mAddress = str;
    }

    public boolean isMultiparty() {
        if (this.mParent != null) {
            return this.mParent.isMultiparty();
        }
        return false;
    }
}
