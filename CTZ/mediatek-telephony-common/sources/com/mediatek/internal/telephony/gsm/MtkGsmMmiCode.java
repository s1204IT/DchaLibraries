package com.mediatek.internal.telephony.gsm;

import android.R;
import android.os.AsyncResult;
import android.os.Message;
import android.os.ResultReceiver;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.gsm.GsmMmiCode;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.mediatek.internal.telephony.MtkGsmCdmaPhone;
import com.mediatek.internal.telephony.MtkSSRequestDecisionMaker;
import com.mediatek.internal.telephony.MtkSuppServHelper;
import com.mediatek.internal.telephony.MtkSuppServManager;
import java.util.regex.Matcher;

public final class MtkGsmMmiCode extends GsmMmiCode {
    static final int EVENT_GET_COLP_COMPLETE = 9;
    static final int EVENT_GET_COLR_COMPLETE = 8;
    static final String LOG_TAG = "MtkGsmMmiCode";
    static final String SC_CNAP = "300";
    static final String SC_COLP = "76";
    static final String SC_COLR = "77";
    private MtkSSRequestDecisionMaker mMtkSSReqDecisionMaker;
    private int mOrigUtCfuMode;
    MtkGsmCdmaPhone mPhone;
    private boolean mUserInitiatedMMI;

    public static MtkGsmMmiCode newFromDialString(String str, MtkGsmCdmaPhone mtkGsmCdmaPhone, UiccCardApplication uiccCardApplication) {
        return newFromDialString(str, mtkGsmCdmaPhone, uiccCardApplication, null);
    }

    public static MtkGsmMmiCode newFromDialString(String str, MtkGsmCdmaPhone mtkGsmCdmaPhone, UiccCardApplication uiccCardApplication, ResultReceiver resultReceiver) {
        if (mtkGsmCdmaPhone.getServiceState().getVoiceRoaming() && mtkGsmCdmaPhone.supportsConversionOfCdmaCallerIdMmiCodesWhileRoaming()) {
            str = convertCdmaMmiCodesTo3gppMmiCodes(str);
        }
        Matcher matcher = sPatternSuppService.matcher(str);
        MtkGsmMmiCode mtkGsmMmiCode = null;
        if (matcher.matches()) {
            MtkGsmMmiCode mtkGsmMmiCode2 = new MtkGsmMmiCode(mtkGsmCdmaPhone, uiccCardApplication);
            mtkGsmMmiCode2.mPoundString = makeEmptyNull(matcher.group(1));
            mtkGsmMmiCode2.mAction = makeEmptyNull(matcher.group(2));
            mtkGsmMmiCode2.mSc = makeEmptyNull(matcher.group(3));
            mtkGsmMmiCode2.mSia = makeEmptyNull(matcher.group(5));
            mtkGsmMmiCode2.mSib = makeEmptyNull(matcher.group(7));
            mtkGsmMmiCode2.mSic = makeEmptyNull(matcher.group(9));
            mtkGsmMmiCode2.mPwd = makeEmptyNull(matcher.group(11));
            mtkGsmMmiCode2.mDialingNumber = makeEmptyNull(matcher.group(12));
            if (mtkGsmMmiCode2.mDialingNumber != null && mtkGsmMmiCode2.mDialingNumber.endsWith("#") && str.endsWith("#")) {
                mtkGsmMmiCode = new MtkGsmMmiCode(mtkGsmCdmaPhone, uiccCardApplication);
                mtkGsmMmiCode.mPoundString = str;
            } else if (!mtkGsmMmiCode2.isFacToDial()) {
                mtkGsmMmiCode = mtkGsmMmiCode2;
            }
        } else if (str.endsWith("#")) {
            mtkGsmMmiCode = new MtkGsmMmiCode(mtkGsmCdmaPhone, uiccCardApplication);
            mtkGsmMmiCode.mPoundString = str;
        } else if (!isTwoDigitShortCode(mtkGsmCdmaPhone.getContext(), str) && isShortCode(str, mtkGsmCdmaPhone)) {
            mtkGsmMmiCode = new MtkGsmMmiCode(mtkGsmCdmaPhone, uiccCardApplication);
            mtkGsmMmiCode.mDialingNumber = str;
        }
        if (mtkGsmMmiCode != null) {
            mtkGsmMmiCode.mCallbackReceiver = resultReceiver;
        }
        return mtkGsmMmiCode;
    }

    public static MtkGsmMmiCode newFromUssdUserInput(String str, MtkGsmCdmaPhone mtkGsmCdmaPhone, UiccCardApplication uiccCardApplication) {
        MtkGsmMmiCode mtkGsmMmiCode = new MtkGsmMmiCode(mtkGsmCdmaPhone, uiccCardApplication);
        mtkGsmMmiCode.mMessage = str;
        mtkGsmMmiCode.mState = MmiCode.State.PENDING;
        mtkGsmMmiCode.mIsPendingUSSD = true;
        return mtkGsmMmiCode;
    }

    private static int siToServiceClass(String str) {
        if (str == null || str.length() == 0) {
            return 0;
        }
        int i = Integer.parseInt(str, 10);
        if (i == 16) {
            return 8;
        }
        if (i != 99) {
            switch (i) {
                case 10:
                    return 13;
                case 11:
                    return 1;
                case 12:
                    return 12;
                case 13:
                    return 4;
                default:
                    switch (i) {
                        case 19:
                            return 5;
                        case 20:
                            return 48;
                        case 21:
                            return 160;
                        case 22:
                            return 80;
                        default:
                            switch (i) {
                                case 24:
                                    return 528;
                                case 25:
                                    return 32;
                                case 26:
                                    return 17;
                                default:
                                    throw new RuntimeException("unsupported MMI service code " + str);
                            }
                    }
            }
        }
        return 64;
    }

    public MtkGsmMmiCode(MtkGsmCdmaPhone mtkGsmCdmaPhone, UiccCardApplication uiccCardApplication) {
        super(mtkGsmCdmaPhone, uiccCardApplication);
        this.mUserInitiatedMMI = false;
        this.mOrigUtCfuMode = 0;
        this.mPhone = mtkGsmCdmaPhone;
        this.mMtkSSReqDecisionMaker = mtkGsmCdmaPhone.getMtkSSRequestDecisionMaker();
    }

    public void setUserInitiatedMMI(boolean z) {
        this.mUserInitiatedMMI = z;
    }

    public boolean getUserInitiatedMMI() {
        return this.mUserInitiatedMMI;
    }

    public void cancel() {
        this.mPhone.mIsNetworkInitiatedUssr = false;
        super.cancel();
    }

    public void processCode() throws CallStateException {
        try {
            if (!supportMdAutoSetupIms() && (this.mPhone.isDuringVoLteCall() || this.mPhone.isDuringImsEccCall())) {
                Rlog.d(LOG_TAG, "Stop CS MMI during IMS Ecc Call or VoLTE call");
                if (this.mPhone.getCsFallbackStatus() == 1) {
                    this.mPhone.setCsFallbackStatus(0);
                }
                StringBuilder sb = new StringBuilder(getScString());
                sb.append("\n");
                this.mState = MmiCode.State.FAILED;
                sb.append(this.mContext.getText(R.string.ext_media_missing_message));
                this.mMessage = sb;
                this.mPhone.onMMIDone(this);
                return;
            }
            if (isShortCode()) {
                Rlog.d(LOG_TAG, "isShortCode");
                sendUssd(this.mDialingNumber);
            } else if (this.mDialingNumber != null) {
                Rlog.w(LOG_TAG, "Special USSD Support:" + this.mPoundString + this.mDialingNumber);
                StringBuilder sb2 = new StringBuilder();
                sb2.append(this.mPoundString);
                sb2.append(this.mDialingNumber);
                sendUssd(sb2.toString());
            } else if (this.mSc != null && this.mSc.equals(SC_CNAP) && isInterrogate()) {
                if (this.mPoundString != null) {
                    handleCNAP(this.mPoundString);
                }
            } else if (this.mSc != null && this.mSc.equals("30")) {
                handleCLIP();
            } else if (this.mSc != null && this.mSc.equals("31")) {
                handleCLIR();
            } else if (this.mSc != null && this.mSc.equals(SC_COLP)) {
                handleCOLP();
            } else if (this.mSc != null && this.mSc.equals(SC_COLR)) {
                handleCOLR();
            } else if (isServiceCodeCallForwarding(this.mSc)) {
                handleCallForward();
            } else if (isServiceCodeCallBarring(this.mSc)) {
                handleCallBarring();
            } else if (this.mSc != null && this.mSc.equals("03")) {
                handleChangeBarringPassward();
            } else if (this.mSc != null && this.mSc.equals("43")) {
                handleCW();
            } else if (isPinPukCommand()) {
                String str = this.mSia;
                String str2 = this.mSib;
                int length = str2.length();
                if (isRegister()) {
                    if (!str2.equals(this.mSic)) {
                        handlePasswordError(R.string.expand_action_accessibility);
                    } else if (length < 4 || length > 8) {
                        handlePasswordError(R.string.config_oemCredentialManagerDialogComponent);
                    } else if (this.mSc.equals("04") && this.mUiccApplication != null && this.mUiccApplication.getState() == IccCardApplicationStatus.AppState.APPSTATE_PUK) {
                        handlePasswordError(R.string.ext_media_new_notification_message);
                    } else if (this.mUiccApplication != null) {
                        Rlog.d(LOG_TAG, "process mmi service code using UiccApp sc=" + this.mSc);
                        if (this.mSc.equals("04")) {
                            this.mUiccApplication.changeIccLockPassword(str, str2, obtainMessage(1, this));
                        } else if (this.mSc.equals("042")) {
                            this.mUiccApplication.changeIccFdnPassword(str, str2, obtainMessage(1, this));
                        } else if (this.mSc.equals("05")) {
                            this.mUiccApplication.supplyPuk(str, str2, obtainMessage(1, this));
                        } else if (this.mSc.equals("052")) {
                            this.mUiccApplication.supplyPuk2(str, str2, obtainMessage(1, this));
                        } else {
                            throw new RuntimeException("uicc unsupported service code=" + this.mSc);
                        }
                    } else {
                        throw new RuntimeException("No application mUiccApplicaiton is null");
                    }
                } else {
                    throw new RuntimeException("Ivalid register/action=" + this.mAction);
                }
            } else if (this.mPoundString != null) {
                if (this.mPhone.getCsFallbackStatus() == 1) {
                    this.mPhone.setCsFallbackStatus(0);
                }
                sendUssd(this.mPoundString);
            } else {
                throw new RuntimeException("Invalid or Unsupported MMI Code");
            }
        } catch (RuntimeException e) {
            this.mState = MmiCode.State.FAILED;
            e.printStackTrace();
            Rlog.d(LOG_TAG, "exc.toString() = " + e.toString());
            Rlog.d(LOG_TAG, "procesCode: mState = FAILED");
            this.mMessage = this.mContext.getText(R.string.ext_media_missing_message);
            this.mPhone.onMMIDone(this);
        }
    }

    void handleCNAP(String str) {
        Rlog.d(LOG_TAG, "processCode: is CNAP");
        if (isInterrogate()) {
            if (this.mPhone.getCsFallbackStatus() == 1) {
                this.mPhone.setCsFallbackStatus(0);
            }
            this.mPhone.mMtkCi.sendCNAP(str, obtainMessage(5, this));
            return;
        }
        throw new RuntimeException("Invalid or Unsupported MMI Code");
    }

    void handleCLIP() {
        Rlog.d(LOG_TAG, "processCode: is CLIP");
        if (isActivate() || isDeactivate()) {
            boolean zIsActivate = isActivate();
            if (supportMdAutoSetupIms()) {
                this.mPhone.mMtkCi.setCLIP(zIsActivate ? 1 : 0, obtainMessage(1, this));
                return;
            } else if (this.mPhone.getCsFallbackStatus() == 0 && this.mPhone.isGsmUtSupport()) {
                this.mMtkSSReqDecisionMaker.setCLIP(zIsActivate ? 1 : 0, obtainMessage(1, this));
                return;
            } else {
                if (this.mPhone.getCsFallbackStatus() == 1) {
                    this.mPhone.setCsFallbackStatus(0);
                }
                this.mPhone.mMtkCi.setCLIP(zIsActivate ? 1 : 0, obtainMessage(1, this));
                return;
            }
        }
        if (isInterrogate()) {
            if (supportMdAutoSetupIms()) {
                this.mPhone.mMtkCi.queryCLIP(obtainMessage(5, this));
                return;
            }
            if (this.mPhone.getCsFallbackStatus() == 0 && this.mPhone.isGsmUtSupport()) {
                this.mMtkSSReqDecisionMaker.getCLIP(obtainMessage(5, this));
                return;
            }
            if (this.mPhone.getCsFallbackStatus() == 1) {
                this.mPhone.setCsFallbackStatus(0);
            }
            this.mPhone.mCi.queryCLIP(obtainMessage(5, this));
            return;
        }
        throw new RuntimeException("Invalid or Unsupported MMI Code");
    }

    void handleCLIR() {
        Rlog.d(LOG_TAG, "processCode: is CLIR");
        if (isActivate() || isDeactivate()) {
            int i = isActivate() ? 1 : 2;
            if (supportMdAutoSetupIms()) {
                this.mPhone.mCi.setCLIR(i, obtainMessage(1, this));
                return;
            }
            if (this.mPhone.isOpTbClir()) {
                this.mPhone.mCi.setCLIR(i, obtainMessage(1, this));
                return;
            }
            if (this.mPhone.getCsFallbackStatus() == 0 && this.mPhone.isGsmUtSupport()) {
                this.mMtkSSReqDecisionMaker.setCLIR(1, obtainMessage(1, this));
                return;
            }
            if (this.mPhone.getCsFallbackStatus() == 1) {
                this.mPhone.setCsFallbackStatus(0);
            }
            this.mPhone.mCi.setCLIR(i, obtainMessage(1, this));
            return;
        }
        if (isInterrogate()) {
            if (supportMdAutoSetupIms()) {
                this.mPhone.mCi.getCLIR(obtainMessage(2, this));
                return;
            }
            if (this.mPhone.isOpTbClir()) {
                this.mPhone.mCi.getCLIR(obtainMessage(2, this));
                return;
            }
            if (this.mPhone.getCsFallbackStatus() == 0 && this.mPhone.isGsmUtSupport()) {
                this.mMtkSSReqDecisionMaker.getCLIR(obtainMessage(2, this));
                return;
            }
            if (this.mPhone.getCsFallbackStatus() == 1) {
                this.mPhone.setCsFallbackStatus(0);
            }
            this.mPhone.mCi.getCLIR(obtainMessage(2, this));
            return;
        }
        throw new RuntimeException("Invalid or Unsupported MMI Code");
    }

    void handleCOLP() {
        Rlog.d(LOG_TAG, "processCode: is COLP");
        if (isInterrogate()) {
            if (supportMdAutoSetupIms()) {
                this.mPhone.mMtkCi.getCOLP(obtainMessage(9, this));
                return;
            }
            if (this.mPhone.getCsFallbackStatus() == 0 && this.mPhone.isGsmUtSupport()) {
                this.mMtkSSReqDecisionMaker.getCOLP(obtainMessage(9, this));
                return;
            }
            if (this.mPhone.getCsFallbackStatus() == 1) {
                this.mPhone.setCsFallbackStatus(0);
            }
            this.mPhone.mMtkCi.getCOLP(obtainMessage(9, this));
            return;
        }
        throw new RuntimeException("Invalid or Unsupported MMI Code");
    }

    void handleCOLR() {
        Rlog.d(LOG_TAG, "processCode: is COLR");
        if (isInterrogate()) {
            if (supportMdAutoSetupIms()) {
                this.mPhone.mMtkCi.getCOLR(obtainMessage(8, this));
                return;
            }
            if (this.mPhone.getCsFallbackStatus() == 0 && this.mPhone.isGsmUtSupport()) {
                this.mMtkSSReqDecisionMaker.getCOLR(obtainMessage(8, this));
                return;
            }
            if (this.mPhone.getCsFallbackStatus() == 1) {
                this.mPhone.setCsFallbackStatus(0);
            }
            this.mPhone.mMtkCi.getCOLR(obtainMessage(8, this));
            return;
        }
        throw new RuntimeException("Invalid or Unsupported MMI Code");
    }

    void handleCallForward() {
        int i;
        Rlog.d(LOG_TAG, "processCode: is CF");
        String str = this.mSia;
        int iSiToServiceClass = siToServiceClass(this.mSib);
        int iScToCallForwardReason = scToCallForwardReason(this.mSc);
        int iSiToTime = siToTime(this.mSic);
        if (isInterrogate()) {
            if (supportMdAutoSetupIms()) {
                this.mPhone.mCi.queryCallForwardStatus(iScToCallForwardReason, iSiToServiceClass, str, obtainMessage(3, this));
                return;
            }
            if (this.mPhone.getCsFallbackStatus() == 0 && this.mPhone.isGsmUtSupport()) {
                this.mMtkSSReqDecisionMaker.queryCallForwardStatus(iScToCallForwardReason, iSiToServiceClass, str, obtainMessage(3, this));
                return;
            }
            if (this.mPhone.getCsFallbackStatus() == 1) {
                this.mPhone.setCsFallbackStatus(0);
            }
            this.mPhone.mCi.queryCallForwardStatus(iScToCallForwardReason, iSiToServiceClass, str, obtainMessage(3, this));
            return;
        }
        if (isActivate()) {
            if (isEmptyOrNull(str)) {
                this.mIsCallFwdReg = false;
                i = 1;
            } else {
                this.mIsCallFwdReg = true;
                i = 3;
            }
        } else if (isDeactivate()) {
            i = 0;
        } else if (isRegister()) {
            i = 3;
        } else {
            if (!isErasure()) {
                throw new RuntimeException("invalid action");
            }
            i = 4;
        }
        boolean zIsVoiceUnconditionalForwarding = isVoiceUnconditionalForwarding(iScToCallForwardReason, iSiToServiceClass);
        int i2 = (i == 1 || i == 3) ? 1 : 0;
        Rlog.d(LOG_TAG, "is CF setCallForward");
        if (zIsVoiceUnconditionalForwarding) {
            this.mOrigUtCfuMode = 0;
            String telephonyProperty = TelephonyManager.getTelephonyProperty(this.mPhone.getPhoneId(), "persist.vendor.radio.cfu.mode", "disabled_cfu_mode");
            if ("enabled_cfu_mode_on".equals(telephonyProperty)) {
                this.mOrigUtCfuMode = 1;
            } else if ("enabled_cfu_mode_off".equals(telephonyProperty)) {
                this.mOrigUtCfuMode = 2;
            }
            TelephonyManager.setTelephonyProperty(this.mPhone.getPhoneId(), "persist.vendor.radio.cfu.mode", "disabled_cfu_mode");
        }
        if (supportMdAutoSetupIms()) {
            this.mPhone.mCi.setCallForward(i, iScToCallForwardReason, iSiToServiceClass, str, iSiToTime, obtainMessage(6, zIsVoiceUnconditionalForwarding ? 1 : 0, i2, this));
            return;
        }
        if (this.mPhone.getCsFallbackStatus() == 0 && this.mPhone.isGsmUtSupport()) {
            this.mMtkSSReqDecisionMaker.setCallForward(i, iScToCallForwardReason, iSiToServiceClass, str, iSiToTime, obtainMessage(6, zIsVoiceUnconditionalForwarding ? 1 : 0, i2, this));
            return;
        }
        if (this.mPhone.getCsFallbackStatus() == 1) {
            this.mPhone.setCsFallbackStatus(0);
        }
        this.mPhone.mCi.setCallForward(i, iScToCallForwardReason, iSiToServiceClass, str, iSiToTime, obtainMessage(6, zIsVoiceUnconditionalForwarding ? 1 : 0, i2, this));
    }

    void handleCallBarring() {
        Rlog.d(LOG_TAG, "processCode: is CB");
        String str = this.mSia;
        int iSiToServiceClass = siToServiceClass(this.mSib);
        String strScToBarringFacility = scToBarringFacility(this.mSc);
        if (isInterrogate()) {
            if (str == null) {
                if (supportMdAutoSetupIms()) {
                    this.mPhone.mCi.queryFacilityLock(strScToBarringFacility, str, iSiToServiceClass, obtainMessage(5, this));
                    return;
                }
                if (this.mPhone.getCsFallbackStatus() == 0 && this.mPhone.isGsmUtSupport()) {
                    this.mMtkSSReqDecisionMaker.queryFacilityLock(strScToBarringFacility, str, iSiToServiceClass, obtainMessage(5, this));
                    return;
                }
                if (this.mPhone.getCsFallbackStatus() == 1) {
                    this.mPhone.setCsFallbackStatus(0);
                }
                if (this.mPhone.getUiccCardApplication() == null) {
                    Rlog.d(LOG_TAG, "handleCallBarring: getUiccCardApplication() == null");
                    Message messageObtainMessage = obtainMessage(5, this);
                    AsyncResult.forMessage(messageObtainMessage, (Object) null, new CommandException(CommandException.Error.GENERIC_FAILURE));
                    messageObtainMessage.sendToTarget();
                    return;
                }
                this.mPhone.mCi.queryFacilityLockForApp(strScToBarringFacility, str, iSiToServiceClass, this.mPhone.getUiccCardApplication().getAid(), obtainMessage(5, this));
                return;
            }
            throw new RuntimeException("Invalid or Unsupported MMI Code");
        }
        if (isActivate() || isDeactivate()) {
            if (str != null && str.length() == 4) {
                if (supportMdAutoSetupIms()) {
                    this.mPhone.mCi.setFacilityLock(strScToBarringFacility, isActivate(), str, iSiToServiceClass, obtainMessage(1, this));
                    return;
                }
                if (this.mPhone.getCsFallbackStatus() == 0 && this.mPhone.isGsmUtSupport()) {
                    this.mMtkSSReqDecisionMaker.setFacilityLock(strScToBarringFacility, isActivate(), str, iSiToServiceClass, obtainMessage(1, this));
                    return;
                }
                if (this.mPhone.getCsFallbackStatus() == 1) {
                    this.mPhone.setCsFallbackStatus(0);
                }
                if (this.mPhone.getUiccCardApplication() == null) {
                    Rlog.d(LOG_TAG, "handleCallBarring: getUiccCardApplication() == null");
                    Message messageObtainMessage2 = obtainMessage(1, this);
                    AsyncResult.forMessage(messageObtainMessage2, (Object) null, new CommandException(CommandException.Error.GENERIC_FAILURE));
                    messageObtainMessage2.sendToTarget();
                    return;
                }
                this.mPhone.mCi.setFacilityLockForApp(strScToBarringFacility, isActivate(), str, iSiToServiceClass, this.mPhone.getUiccCardApplication().getAid(), obtainMessage(1, this));
                return;
            }
            handlePasswordError(R.string.factorytest_not_system);
            return;
        }
        throw new RuntimeException("Invalid or Unsupported MMI Code");
    }

    void handleChangeBarringPassward() {
        String strScToBarringFacility;
        Rlog.d(LOG_TAG, "processCode: is Change PWD");
        String str = this.mSib;
        String str2 = this.mSic;
        if (isActivate() || isRegister()) {
            this.mAction = "**";
            if (this.mSia == null) {
                strScToBarringFacility = "AB";
            } else {
                strScToBarringFacility = scToBarringFacility(this.mSia);
            }
            String str3 = strScToBarringFacility;
            if (str != null && str2 != null && this.mPwd != null) {
                if (this.mPwd.length() != str2.length() || str.length() != 4 || this.mPwd.length() != 4) {
                    handlePasswordError(R.string.factorytest_not_system);
                    return;
                } else {
                    if (this.mPhone.isDuringImsCall()) {
                        Message messageObtainMessage = obtainMessage(1, this);
                        AsyncResult.forMessage(messageObtainMessage, (Object) null, new CommandException(CommandException.Error.GENERIC_FAILURE));
                        messageObtainMessage.sendToTarget();
                        return;
                    }
                    this.mPhone.mMtkCi.changeBarringPassword(str3, str, str2, this.mPwd, obtainMessage(1, this));
                    return;
                }
            }
            handlePasswordError(R.string.factorytest_not_system);
            return;
        }
        throw new RuntimeException("Invalid or Unsupported MMI Code");
    }

    void handleCW() {
        Rlog.d(LOG_TAG, "processCode: is CW");
        int iSiToServiceClass = siToServiceClass(this.mSia);
        int tbcwMode = this.mPhone.getTbcwMode();
        if (supportMdAutoSetupIms()) {
            if (isActivate() || isDeactivate()) {
                this.mPhone.mCi.setCallWaiting(isActivate(), iSiToServiceClass, obtainMessage(1, this));
                return;
            } else {
                if (isInterrogate()) {
                    this.mPhone.mCi.queryCallWaiting(iSiToServiceClass, obtainMessage(5, this));
                    return;
                }
                throw new RuntimeException("Invalid or Unsupported MMI Code");
            }
        }
        if (isActivate() || isDeactivate()) {
            if (tbcwMode == 1 && !this.mPhone.isOpNwCW()) {
                this.mPhone.setTerminalBasedCallWaiting(isActivate(), obtainMessage(1, this));
                return;
            }
            if (tbcwMode == 2 || tbcwMode == 3) {
                if (this.mPhone.getCsFallbackStatus() == 1) {
                    this.mPhone.setCsFallbackStatus(0);
                }
                this.mPhone.mCi.setCallWaiting(isActivate(), iSiToServiceClass, obtainMessage(1, isActivate() ? 1 : 0, -1, this));
                return;
            } else {
                Rlog.d(LOG_TAG, "processCode setCallWaiting");
                if (this.mPhone.getCsFallbackStatus() == 0 && this.mPhone.isGsmUtSupport()) {
                    this.mMtkSSReqDecisionMaker.setCallWaiting(isActivate(), iSiToServiceClass, obtainMessage(1, this));
                    return;
                } else {
                    this.mPhone.mCi.setCallWaiting(isActivate(), iSiToServiceClass, obtainMessage(1, this));
                    return;
                }
            }
        }
        if (isInterrogate()) {
            if (tbcwMode == 1 && !this.mPhone.isOpNwCW()) {
                this.mPhone.getTerminalBasedCallWaiting(obtainMessage(5, this));
                return;
            }
            if (tbcwMode == 2 || tbcwMode == 3) {
                if (this.mPhone.getCsFallbackStatus() == 1) {
                    this.mPhone.setCsFallbackStatus(0);
                }
                this.mPhone.mCi.queryCallWaiting(iSiToServiceClass, obtainMessage(5, this));
                return;
            } else {
                Rlog.d(LOG_TAG, "processCode getCallWaiting");
                if (this.mPhone.getCsFallbackStatus() == 0 && this.mPhone.isGsmUtSupport()) {
                    this.mMtkSSReqDecisionMaker.queryCallWaiting(iSiToServiceClass, obtainMessage(5, this));
                    return;
                } else {
                    this.mPhone.mCi.queryCallWaiting(iSiToServiceClass, obtainMessage(5, this));
                    return;
                }
            }
        }
        throw new RuntimeException("Invalid or Unsupported MMI Code");
    }

    public void onUssdStkHandling(String str, boolean z) {
        if (this.mState == MmiCode.State.PENDING) {
            if (str == null || str.length() == 0) {
                this.mMessage = this.mContext.getText(R.string.ext_media_init_action);
            } else {
                this.mMessage = str;
            }
            this.mIsUssdRequest = z;
            if (!z) {
                this.mState = MmiCode.State.COMPLETE;
            }
            this.mPhone.onMMIDone(this, "stk");
        }
    }

    public void handleMessage(Message message) {
        switch (message.what) {
            case 1:
                AsyncResult asyncResult = (AsyncResult) message.obj;
                if (this.mSc.equals("43") && this.mPhone.getTbcwMode() == 3 && !supportMdAutoSetupIms() && asyncResult.exception == null) {
                    this.mPhone.setTerminalBasedCallWaiting(message.arg1 == 1, null);
                }
                onSetComplete(message, asyncResult);
                break;
            case 2:
            case 3:
            case 4:
            case 7:
            default:
                super.handleMessage(message);
                break;
            case 5:
                AsyncResult asyncResult2 = (AsyncResult) message.obj;
                if (this.mSc.equals("43") && this.mPhone.getTbcwMode() == 3 && !supportMdAutoSetupIms()) {
                    Rlog.d(LOG_TAG, "TBCW_WITH_CS");
                    if (asyncResult2.exception == null) {
                        int[] iArr = (int[]) asyncResult2.result;
                        try {
                            Rlog.d(LOG_TAG, "EVENT_GET_CALL_WAITING_FOR_CS_TB cwArray[0]:cwArray[1] = " + iArr[0] + ":" + iArr[1]);
                            if (iArr[0] == 1 && (iArr[1] & 1) == 1) {
                                z = true;
                            }
                            this.mPhone.setTerminalBasedCallWaiting(z, null);
                        } catch (ArrayIndexOutOfBoundsException e) {
                            Rlog.e(LOG_TAG, "EVENT_GET_CALL_WAITING_FOR_CS_TB: improper result: err =" + e.getMessage());
                        }
                    }
                }
                onQueryComplete(asyncResult2);
                break;
            case 6:
                AsyncResult asyncResult3 = (AsyncResult) message.obj;
                if (asyncResult3.exception == null && message.arg1 == 1) {
                    boolean z = message.arg2 == 1;
                    if (this.mIccRecords != null) {
                        this.mPhone.setVoiceCallForwardingFlag(1, z, this.mDialingNumber);
                        this.mPhone.saveTimeSlot(null);
                    }
                }
                if (asyncResult3.exception != null && this.mOrigUtCfuMode != 0) {
                    TelephonyManager.setTelephonyProperty(this.mPhone.getPhoneId(), "persist.vendor.radio.cfu.mode", this.mOrigUtCfuMode == 1 ? "enabled_cfu_mode_on" : "enabled_cfu_mode_off");
                }
                this.mOrigUtCfuMode = 0;
                onSetComplete(message, asyncResult3);
                break;
            case 8:
                onGetColrComplete((AsyncResult) message.obj);
                break;
            case 9:
                onGetColpComplete((AsyncResult) message.obj);
                break;
        }
    }

    protected void onSetComplete(Message message, AsyncResult asyncResult) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        if (asyncResult.exception != null) {
            this.mState = MmiCode.State.FAILED;
            if ((asyncResult.exception instanceof CommandException) && asyncResult.exception.getCommandError() == CommandException.Error.REQUEST_NOT_SUPPORTED && (this.mSc.equals("31") || this.mSc.equals("30"))) {
                sb.append(this.mContext.getText(R.string.ext_media_missing_message));
                this.mMessage = sb;
                this.mPhone.onMMIDone(this);
                return;
            }
        }
        super.onSetComplete(message, asyncResult);
    }

    protected CharSequence serviceClassToCFString(int i) {
        Rlog.d(LOG_TAG, "serviceClassToCFString, serviceClass = " + i);
        if (i == 256 || i == 512) {
            return this.mContext.getText(134545476);
        }
        return super.serviceClassToCFString(i);
    }

    protected CharSequence makeCFQueryResultMessage(CallForwardInfo callForwardInfo, int i) {
        CharSequence text;
        String[] strArr = {"{0}", "{1}", "{2}"};
        CharSequence[] charSequenceArr = new CharSequence[3];
        boolean z = callForwardInfo.reason == 2 && callForwardInfo.timeSeconds >= 0;
        if (callForwardInfo.status == 1) {
            if (z) {
                text = this.mContext.getText(R.string.accessibility_service_action_perform_title);
            } else {
                text = this.mContext.getText(R.string.accessibility_service_action_perform_description);
            }
        } else if (callForwardInfo.status == 0 && isEmptyOrNull(callForwardInfo.number)) {
            text = this.mContext.getText(R.string.accessibility_service_screen_control_description);
        } else if (z) {
            text = this.mContext.getText(R.string.accessibility_service_warning_description);
        } else {
            text = this.mContext.getText(R.string.accessibility_service_screen_control_title);
        }
        charSequenceArr[0] = serviceClassToCFString(callForwardInfo.serviceClass & i);
        charSequenceArr[1] = PhoneNumberUtils.stringFromStringAndTOA(callForwardInfo.number, callForwardInfo.toa);
        charSequenceArr[2] = Integer.toString(callForwardInfo.timeSeconds);
        if (callForwardInfo.reason == 0 && (i & callForwardInfo.serviceClass) == 1) {
            boolean z2 = callForwardInfo.status == 1;
            if (this.mIccRecords != null) {
                this.mPhone.setVoiceCallForwardingFlag(1, z2, callForwardInfo.number);
            }
        }
        return TextUtils.replace(text, strArr, charSequenceArr);
    }

    protected void onQueryCfComplete(AsyncResult asyncResult) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        if (asyncResult.exception != null) {
            super.onQueryCfComplete(asyncResult);
            return;
        }
        CallForwardInfo[] callForwardInfoArr = (CallForwardInfo[]) asyncResult.result;
        if (callForwardInfoArr.length == 0) {
            sb.append(this.mContext.getText(R.string.lockscreen_too_many_failed_password_attempts_dialog_message));
            if (this.mIccRecords != null) {
                this.mPhone.setVoiceCallForwardingFlag(1, false, null);
            }
        } else {
            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
            for (int i = 1; i <= 512; i <<= 1) {
                if (i != 256) {
                    int length = callForwardInfoArr.length;
                    for (int i2 = 0; i2 < length; i2++) {
                        if ((callForwardInfoArr[i2].serviceClass & i) != 0) {
                            spannableStringBuilder.append(makeCFQueryResultMessage(callForwardInfoArr[i2], i));
                            spannableStringBuilder.append((CharSequence) "\n");
                            if (callForwardInfoArr[i2].reason == 0 && (callForwardInfoArr[i2].serviceClass & i) == 1 && this.mIccRecords != null) {
                                this.mPhone.setVoiceCallForwardingFlag(1, callForwardInfoArr[i2].status == 1, null);
                            }
                        }
                    }
                }
            }
            sb.append((CharSequence) spannableStringBuilder);
        }
        this.mState = MmiCode.State.COMPLETE;
        this.mMessage = sb;
        Rlog.d(LOG_TAG, "onQueryCfComplete: mmi=" + this);
        this.mPhone.onMMIDone(this);
    }

    protected void onQueryComplete(AsyncResult asyncResult) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        if (asyncResult.exception == null) {
            int[] iArr = (int[]) asyncResult.result;
            if (iArr.length != 0 && iArr[0] != 0 && this.mSc.equals(SC_CNAP)) {
                Rlog.d(LOG_TAG, "onQueryComplete_CNAP");
                sb.append(createQueryCnapResultMessage(iArr[1]));
                this.mMessage = sb;
                this.mState = MmiCode.State.COMPLETE;
                this.mPhone.onMMIDone(this);
                return;
            }
        }
        super.onQueryComplete(asyncResult);
    }

    protected CharSequence createQueryCallWaitingResultMessage(int i) {
        StringBuilder sb = new StringBuilder(this.mContext.getText(R.string.lockscreen_transport_ffw_description));
        for (int i2 = 1; i2 <= 512; i2 <<= 1) {
            int i3 = i2 & i;
            if (i3 != 0) {
                sb.append("\n");
                sb.append(serviceClassToCFString(i3));
            }
        }
        return sb;
    }

    protected CharSequence createQueryCallBarringResultMessage(int i) {
        StringBuilder sb = new StringBuilder(this.mContext.getText(R.string.lockscreen_transport_ffw_description));
        for (int i2 = 1; i2 <= 512; i2 <<= 1) {
            int i3 = i2 & i;
            if (i3 != 0) {
                sb.append("\n");
                sb.append(serviceClassToCFString(i3));
            }
        }
        return sb;
    }

    private CharSequence createQueryCnapResultMessage(int i) {
        Rlog.d(LOG_TAG, "createQueryCnapResultMessage");
        StringBuilder sb = new StringBuilder(this.mContext.getText(R.string.lockscreen_transport_ffw_description));
        sb.append("\n");
        switch (i) {
            case 0:
                sb.append(this.mContext.getText(R.string.lockscreen_transport_pause_description));
                break;
            case 1:
                sb.append(this.mContext.getText(134545420));
                break;
            default:
                sb.append(this.mContext.getText(R.string.lockscreen_transport_pause_description));
                break;
        }
        Rlog.d(LOG_TAG, "CNAP_sb = " + ((Object) sb));
        return sb;
    }

    public static MtkGsmMmiCode newNetworkInitiatedUssd(String str, boolean z, MtkGsmCdmaPhone mtkGsmCdmaPhone, UiccCardApplication uiccCardApplication) {
        MtkGsmMmiCode mtkGsmMmiCode = new MtkGsmMmiCode(mtkGsmCdmaPhone, uiccCardApplication);
        mtkGsmMmiCode.mMessage = str;
        mtkGsmMmiCode.mIsUssdRequest = z;
        if (z) {
            mtkGsmMmiCode.mIsPendingUSSD = true;
            mtkGsmMmiCode.mState = MmiCode.State.PENDING;
        } else {
            mtkGsmMmiCode.mState = MmiCode.State.COMPLETE;
        }
        return mtkGsmMmiCode;
    }

    public static MtkGsmMmiCode newNetworkInitiatedUssdError(String str, boolean z, MtkGsmCdmaPhone mtkGsmCdmaPhone, UiccCardApplication uiccCardApplication) {
        MtkGsmMmiCode mtkGsmMmiCode = new MtkGsmMmiCode(mtkGsmCdmaPhone, uiccCardApplication);
        mtkGsmMmiCode.mMessage = mtkGsmMmiCode.mContext.getText(R.string.ext_media_missing_message);
        mtkGsmMmiCode.mIsUssdRequest = z;
        mtkGsmMmiCode.mState = MmiCode.State.FAILED;
        return mtkGsmMmiCode;
    }

    private void onGetColpComplete(AsyncResult asyncResult) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        if (asyncResult.exception != null) {
            this.mState = MmiCode.State.FAILED;
            sb.append(getErrorMessage(asyncResult));
        } else {
            switch (((int[]) asyncResult.result)[1]) {
                case 0:
                    sb.append(this.mContext.getText(R.string.lockscreen_transport_pause_description));
                    this.mState = MmiCode.State.COMPLETE;
                    break;
                case 1:
                    sb.append(this.mContext.getText(134545420));
                    this.mState = MmiCode.State.COMPLETE;
                    break;
                case 2:
                    sb.append(this.mContext.getText(134545421));
                    this.mState = MmiCode.State.COMPLETE;
                    break;
            }
        }
        this.mMessage = sb;
        this.mPhone.onMMIDone(this);
    }

    private void onGetColrComplete(AsyncResult asyncResult) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        if (asyncResult.exception != null) {
            this.mState = MmiCode.State.FAILED;
            sb.append(getErrorMessage(asyncResult));
        } else {
            switch (((int[]) asyncResult.result)[0]) {
                case 0:
                    sb.append(this.mContext.getText(R.string.lockscreen_transport_pause_description));
                    this.mState = MmiCode.State.COMPLETE;
                    break;
                case 1:
                    sb.append(this.mContext.getText(134545420));
                    this.mState = MmiCode.State.COMPLETE;
                    break;
                case 2:
                    sb.append(this.mContext.getText(R.string.ext_media_missing_message));
                    this.mState = MmiCode.State.FAILED;
                    break;
            }
        }
        this.mMessage = sb;
        this.mPhone.onMMIDone(this);
    }

    public static boolean isUtMmiCode(String str, MtkGsmCdmaPhone mtkGsmCdmaPhone, UiccCardApplication uiccCardApplication) {
        MtkGsmMmiCode mtkGsmMmiCodeNewFromDialString = newFromDialString(str, mtkGsmCdmaPhone, uiccCardApplication);
        if (mtkGsmMmiCodeNewFromDialString == null || mtkGsmMmiCodeNewFromDialString.isTemporaryModeCLIR() || mtkGsmMmiCodeNewFromDialString.isShortCode() || mtkGsmMmiCodeNewFromDialString.mDialingNumber != null || mtkGsmMmiCodeNewFromDialString.mSc == null || (!mtkGsmMmiCodeNewFromDialString.mSc.equals("30") && !mtkGsmMmiCodeNewFromDialString.mSc.equals("31") && !mtkGsmMmiCodeNewFromDialString.mSc.equals(SC_COLP) && !mtkGsmMmiCodeNewFromDialString.mSc.equals(SC_COLR) && !isServiceCodeCallForwarding(mtkGsmMmiCodeNewFromDialString.mSc) && !isServiceCodeCallBarring(mtkGsmMmiCodeNewFromDialString.mSc) && !mtkGsmMmiCodeNewFromDialString.mSc.equals("43"))) {
            return false;
        }
        return true;
    }

    private boolean supportMdAutoSetupIms() {
        if (SystemProperties.get("ro.vendor.md_auto_setup_ims").equals("1")) {
            return true;
        }
        return false;
    }

    protected CharSequence getErrorMessage(AsyncResult asyncResult) {
        if (asyncResult.exception instanceof CommandException) {
            CommandException.Error commandError = asyncResult.exception.getCommandError();
            if (commandError == CommandException.Error.OEM_ERROR_1) {
                if (supportMdAutoSetupIms()) {
                    Rlog.i(LOG_TAG, "getErrorMessage, OEM_ERROR_1 409_CONFLICT");
                    MtkSuppServHelper suppServHelper = MtkSuppServManager.getSuppServHelper(this.mPhone.getPhoneId());
                    if (suppServHelper != null) {
                        String xCAPErrorMessageFromSysProp = suppServHelper.getXCAPErrorMessageFromSysProp(CommandException.Error.OEM_ERROR_1);
                        if (xCAPErrorMessageFromSysProp != null && !xCAPErrorMessageFromSysProp.isEmpty()) {
                            return xCAPErrorMessageFromSysProp;
                        }
                        return this.mContext.getText(R.string.ext_media_missing_message);
                    }
                } else {
                    return this.mContext.getText(R.string.ext_media_missing_message);
                }
            } else {
                if (commandError == CommandException.Error.OEM_ERROR_10) {
                    Rlog.i(LOG_TAG, "getErrorMessage, OEM_ERROR_10 USSD_MODIFIED_TO_DIAL_VIDEO");
                    return this.mContext.getText(R.string.mediasize_japanese_l);
                }
                if (commandError == CommandException.Error.OEM_ERROR_5) {
                    Rlog.i(LOG_TAG, "getErrorMessage, OEM_ERROR_5 CALL_BARRED");
                    return this.mContext.getText(134545416);
                }
                if (commandError == CommandException.Error.FDN_CHECK_FAILURE) {
                    Rlog.i(LOG_TAG, "getErrorMessage, FDN_CHECK_FAILURE");
                    return this.mContext.getText(134545415);
                }
            }
        }
        return super.getErrorMessage(asyncResult);
    }
}
