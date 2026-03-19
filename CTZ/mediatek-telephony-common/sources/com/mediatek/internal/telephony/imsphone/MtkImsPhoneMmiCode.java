package com.mediatek.internal.telephony.imsphone;

import android.R;
import android.hardware.radio.V1_0.LastCallFailCause;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Message;
import android.os.ResultReceiver;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import com.android.ims.ImsException;
import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.imsphone.ImsPhoneMmiCode;
import com.mediatek.internal.telephony.MtkGsmCdmaPhone;
import java.util.regex.Matcher;

public final class MtkImsPhoneMmiCode extends ImsPhoneMmiCode {
    static final String LOG_TAG = "MtkImsPhoneMmiCode";
    private static final String SC_CFNotRegister = "68";

    public static MtkImsPhoneMmiCode newFromDialString(String str, ImsPhone imsPhone) {
        return newFromDialString(str, imsPhone, null);
    }

    public static MtkImsPhoneMmiCode newFromDialString(String str, ImsPhone imsPhone, ResultReceiver resultReceiver) {
        Matcher matcher = sPatternSuppService.matcher(str);
        if (matcher.matches()) {
            MtkImsPhoneMmiCode mtkImsPhoneMmiCode = new MtkImsPhoneMmiCode(imsPhone);
            mtkImsPhoneMmiCode.mPoundString = makeEmptyNull(matcher.group(1));
            mtkImsPhoneMmiCode.mAction = makeEmptyNull(matcher.group(2));
            mtkImsPhoneMmiCode.mSc = makeEmptyNull(matcher.group(3));
            mtkImsPhoneMmiCode.mSia = makeEmptyNull(matcher.group(5));
            mtkImsPhoneMmiCode.mSib = makeEmptyNull(matcher.group(7));
            mtkImsPhoneMmiCode.mSic = makeEmptyNull(matcher.group(9));
            mtkImsPhoneMmiCode.mPwd = makeEmptyNull(matcher.group(11));
            mtkImsPhoneMmiCode.mDialingNumber = makeEmptyNull(matcher.group(12));
            mtkImsPhoneMmiCode.mCallbackReceiver = resultReceiver;
            if (mtkImsPhoneMmiCode.mDialingNumber != null && mtkImsPhoneMmiCode.mDialingNumber.endsWith("#") && str.endsWith("#")) {
                MtkImsPhoneMmiCode mtkImsPhoneMmiCode2 = new MtkImsPhoneMmiCode(imsPhone);
                mtkImsPhoneMmiCode2.mPoundString = str;
                return mtkImsPhoneMmiCode2;
            }
            return mtkImsPhoneMmiCode;
        }
        if (str.endsWith("#")) {
            MtkImsPhoneMmiCode mtkImsPhoneMmiCode3 = new MtkImsPhoneMmiCode(imsPhone);
            mtkImsPhoneMmiCode3.mPoundString = str;
            return mtkImsPhoneMmiCode3;
        }
        if (isTwoDigitShortCode(imsPhone.getContext(), str) || !isShortCode(str, imsPhone)) {
            return null;
        }
        MtkImsPhoneMmiCode mtkImsPhoneMmiCode4 = new MtkImsPhoneMmiCode(imsPhone);
        mtkImsPhoneMmiCode4.mDialingNumber = str;
        return mtkImsPhoneMmiCode4;
    }

    public static MtkImsPhoneMmiCode newNetworkInitiatedUssd(String str, boolean z, MtkImsPhone mtkImsPhone) {
        MtkImsPhoneMmiCode mtkImsPhoneMmiCode = new MtkImsPhoneMmiCode(mtkImsPhone);
        mtkImsPhoneMmiCode.mMessage = str;
        mtkImsPhoneMmiCode.mIsUssdRequest = z;
        if (z) {
            mtkImsPhoneMmiCode.mIsPendingUSSD = true;
            mtkImsPhoneMmiCode.mState = MmiCode.State.PENDING;
        } else {
            mtkImsPhoneMmiCode.mState = MmiCode.State.COMPLETE;
        }
        return mtkImsPhoneMmiCode;
    }

    public static MtkImsPhoneMmiCode newFromUssdUserInput(String str, MtkImsPhone mtkImsPhone) {
        MtkImsPhoneMmiCode mtkImsPhoneMmiCode = new MtkImsPhoneMmiCode(mtkImsPhone);
        mtkImsPhoneMmiCode.mMessage = str;
        mtkImsPhoneMmiCode.mState = MmiCode.State.PENDING;
        mtkImsPhoneMmiCode.mIsPendingUSSD = true;
        return mtkImsPhoneMmiCode;
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

    public MtkImsPhoneMmiCode(ImsPhone imsPhone) {
        super(imsPhone);
    }

    public void cancel() {
        if (this.mState == MmiCode.State.COMPLETE || this.mState == MmiCode.State.FAILED) {
            return;
        }
        this.mState = MmiCode.State.CANCELLED;
        if (this.mIsPendingUSSD) {
            ((MtkImsPhone) this.mPhone).cancelUSSD(obtainMessage(5, this));
        } else {
            this.mPhone.onMMIDone(this);
        }
    }

    public boolean isSupportedOverImsPhone() {
        if (isShortCode()) {
            return true;
        }
        if (this.mDialingNumber != null) {
            return false;
        }
        if (this.mSc != null && this.mSc.equals("300")) {
            return false;
        }
        if (!isServiceCodeCallForwarding(this.mSc) && !isServiceCodeCallBarring(this.mSc) && ((this.mSc == null || !this.mSc.equals("43")) && ((this.mSc == null || !this.mSc.equals("31")) && ((this.mSc == null || !this.mSc.equals("30")) && ((this.mSc == null || !this.mSc.equals("77")) && ((this.mSc == null || !this.mSc.equals("76")) && ((this.mSc == null || !this.mSc.equals("156")) && (this.mSc == null || !this.mSc.equals("157"))))))))) {
            return !isPinPukCommand() && (this.mSc == null || !(this.mSc.equals("03") || this.mSc.equals("30") || this.mSc.equals("31"))) && this.mPoundString != null;
        }
        if (supportMdAutoSetupIms()) {
            try {
                int iSiToServiceClass = siToServiceClass(this.mSib);
                if ((iSiToServiceClass & 1) == 0 && (iSiToServiceClass & 512) == 0 && iSiToServiceClass != 0) {
                    return false;
                }
                Rlog.d(LOG_TAG, "isSupportedOverImsPhone(), return true!");
                return true;
            } catch (RuntimeException e) {
                Rlog.d(LOG_TAG, "Invalid service class " + e);
            }
        } else if (this.mPhone.isVolteEnabled() || (this.mPhone.isWifiCallingEnabled() && this.mPhone.mDefaultPhone.isWFCUtSupport())) {
            try {
                int iSiToServiceClass2 = siToServiceClass(this.mSib);
                if ((iSiToServiceClass2 & 1) == 0 && (iSiToServiceClass2 & 512) == 0 && iSiToServiceClass2 != 0) {
                }
                Rlog.d(LOG_TAG, "isSupportedOverImsPhone(), return true!");
                return true;
            } catch (RuntimeException e2) {
                Rlog.d(LOG_TAG, "exc.toString() = " + e2.toString());
            }
        }
        return false;
    }

    public void processCode() throws CallStateException {
        try {
            if (!supportMdAutoSetupIms() && this.mPhone.mDefaultPhone.getCsFallbackStatus() != 0) {
                Rlog.d(LOG_TAG, "processCode(): getCsFallbackStatus(): CS Fallback!");
                ((MtkImsPhone) this.mPhone).removeMmi(this);
                throw new CallStateException("cs_fallback");
            }
            if (isShortCode()) {
                Rlog.d(LOG_TAG, "isShortCode, Sending '" + this.mDialingNumber + "' over IMS");
                sendUssd(this.mDialingNumber);
            } else if (isServiceCodeCallForwarding(this.mSc)) {
                handleCallForward();
            } else if (isServiceCodeCallBarring(this.mSc)) {
                handleCallBarring();
            } else if (this.mSc != null && this.mSc.equals("31")) {
                handleCLIR();
            } else if (this.mSc != null && this.mSc.equals("30")) {
                handleCLIP();
            } else if (this.mSc != null && this.mSc.equals("76")) {
                handleCOLP();
            } else if (this.mSc != null && this.mSc.equals("77")) {
                handleCOLR();
            } else if (this.mSc != null && this.mSc.equals("156")) {
                handleCallBarringSpecificMT();
            } else if (this.mSc != null && this.mSc.equals("157")) {
                handleCallBarringACR();
            } else if (this.mSc != null && this.mSc.equals("43")) {
                handleCW();
            } else if (this.mPoundString != null) {
                Rlog.d(LOG_TAG, "Sending pound string '" + this.mPoundString + "' over IMS.");
                sendUssd(this.mPoundString);
            } else {
                throw new RuntimeException("Invalid or Unsupported MMI Code");
            }
        } catch (RuntimeException e) {
            this.mState = MmiCode.State.FAILED;
            this.mMessage = this.mContext.getText(R.string.ext_media_missing_message);
            this.mPhone.onMMIDone(this);
        }
    }

    void handleCallForward() {
        int i;
        Rlog.d(LOG_TAG, "processCode: is CF");
        String str = this.mSia;
        int iScToCallForwardReason = scToCallForwardReason(this.mSc);
        int iSiToServiceClass = siToServiceClass(this.mSib);
        int iSiToTime = siToTime(this.mSic);
        int i2 = 1;
        if (isInterrogate()) {
            if (iSiToServiceClass != 0 && (this.mPhone.mDefaultPhone instanceof MtkGsmCdmaPhone)) {
                this.mPhone.mDefaultPhone.setServiceClass(iSiToServiceClass);
            }
            this.mPhone.getCallForwardingOption(iScToCallForwardReason, obtainMessage(1, this));
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
        int i3 = ((iScToCallForwardReason == 0 || iScToCallForwardReason == 4) && ((iSiToServiceClass & 1) != 0 || iSiToServiceClass == 0)) ? 1 : 0;
        if (i != 1 && i != 3) {
            i2 = 0;
        }
        Rlog.d(LOG_TAG, "is CF setCallForward");
        if (this.mPhone.mDefaultPhone.isOpReregisterForCF()) {
            Rlog.i(LOG_TAG, "Set ims dereg to ON.");
            SystemProperties.set(MtkGsmCdmaPhone.IMS_DEREG_PROP, "1");
        }
        this.mPhone.setCallForwardingOption(i, iScToCallForwardReason, str, iSiToServiceClass, iSiToTime, obtainMessage(4, i3, i2, this));
    }

    void handleCallBarring() {
        Rlog.d(LOG_TAG, "processCode: is CB");
        String str = this.mSia;
        String strScToBarringFacility = scToBarringFacility(this.mSc);
        int iSiToServiceClass = siToServiceClass(this.mSib);
        if (isInterrogate()) {
            this.mPhone.getCallBarring(strScToBarringFacility, str, obtainMessage(7, this), iSiToServiceClass);
        } else {
            if (isActivate() || isDeactivate()) {
                this.mPhone.setCallBarring(strScToBarringFacility, isActivate(), str, obtainMessage(0, this), iSiToServiceClass);
                return;
            }
            throw new RuntimeException("Invalid or Unsupported MMI Code");
        }
    }

    void handleCW() {
        Rlog.d(LOG_TAG, "processCode: is CW");
        int iSiToServiceClass = siToServiceClass(this.mSib);
        int tbcwMode = this.mPhone.mDefaultPhone.getTbcwMode();
        if (supportMdAutoSetupIms()) {
            if (isActivate() || isDeactivate()) {
                this.mPhone.setCallWaiting(isActivate(), iSiToServiceClass, obtainMessage(0, this));
                return;
            } else {
                if (isInterrogate()) {
                    this.mPhone.getCallWaiting(obtainMessage(3, this));
                    return;
                }
                throw new RuntimeException("Invalid or Unsupported MMI Code");
            }
        }
        if (isActivate() || isDeactivate()) {
            if (this.mPhone.mDefaultPhone.isOpNwCW()) {
                Rlog.d(LOG_TAG, "setCallWaiting() by Ut interface.");
                this.mPhone.setCallWaiting(isActivate(), iSiToServiceClass, obtainMessage(0, this));
                return;
            }
            if (tbcwMode == 3) {
                this.mPhone.mDefaultPhone.mCi.setCallWaiting(isActivate(), iSiToServiceClass, obtainMessage(0, isActivate() ? 1 : 0, -1, this));
                return;
            }
            String telephonyProperty = TelephonyManager.getTelephonyProperty(this.mPhone.mDefaultPhone.getPhoneId(), "persist.vendor.radio.terminal-based.cw", "disabled_tbcw");
            Rlog.d(LOG_TAG, "setCallWaiting(): tbcwStatus = " + telephonyProperty + ", enable = " + isActivate());
            if ("enabled_tbcw_on".equals(telephonyProperty)) {
                if (!isActivate()) {
                    this.mPhone.mDefaultPhone.setSSPropertyThroughHidl(this.mPhone.mDefaultPhone.getPhoneId(), "persist.vendor.radio.terminal-based.cw", "enabled_tbcw_off");
                }
                Message messageObtainMessage = obtainMessage(0, null);
                AsyncResult.forMessage(messageObtainMessage, (Object) null, (Throwable) null);
                sendMessage(messageObtainMessage);
                return;
            }
            if ("enabled_tbcw_off".equals(telephonyProperty)) {
                if (isActivate()) {
                    this.mPhone.mDefaultPhone.setSSPropertyThroughHidl(this.mPhone.mDefaultPhone.getPhoneId(), "persist.vendor.radio.terminal-based.cw", "enabled_tbcw_on");
                }
                Message messageObtainMessage2 = obtainMessage(0, null);
                AsyncResult.forMessage(messageObtainMessage2, (Object) null, (Throwable) null);
                sendMessage(messageObtainMessage2);
                return;
            }
            Rlog.d(LOG_TAG, "setCallWaiting() by Ut interface.");
            this.mPhone.setCallWaiting(isActivate(), iSiToServiceClass, obtainMessage(0, this));
            return;
        }
        if (isInterrogate()) {
            if (this.mPhone.mDefaultPhone.isOpNwCW()) {
                Rlog.d(LOG_TAG, "getCallWaiting() by Ut interface.");
                this.mPhone.getCallWaiting(obtainMessage(3, this));
                return;
            }
            if (tbcwMode == 3) {
                this.mPhone.mDefaultPhone.mCi.queryCallWaiting(iSiToServiceClass, obtainMessage(3, this));
                return;
            }
            String telephonyProperty2 = TelephonyManager.getTelephonyProperty(this.mPhone.mDefaultPhone.getPhoneId(), "persist.vendor.radio.terminal-based.cw", "disabled_tbcw");
            Rlog.d(LOG_TAG, "SC_WAIT isInterrogate() tbcwStatus = " + telephonyProperty2);
            if ("enabled_tbcw_on".equals(telephonyProperty2)) {
                Message messageObtainMessage3 = obtainMessage(3, null);
                AsyncResult.forMessage(messageObtainMessage3, new int[]{1, 1}, (Throwable) null);
                sendMessage(messageObtainMessage3);
                return;
            } else if ("enabled_tbcw_off".equals(telephonyProperty2)) {
                Message messageObtainMessage4 = obtainMessage(3, null);
                AsyncResult.forMessage(messageObtainMessage4, new int[]{0, 0}, (Throwable) null);
                sendMessage(messageObtainMessage4);
                return;
            } else {
                Rlog.d(LOG_TAG, "getCallWaiting() by Ut interface.");
                this.mPhone.getCallWaiting(obtainMessage(3, this));
                return;
            }
        }
        throw new RuntimeException("Invalid or Unsupported MMI Code");
    }

    void handleCLIP() {
        Rlog.d(LOG_TAG, "processCode: is CLIP");
        if (isInterrogate()) {
            try {
                this.mPhone.mCT.getUtInterface().queryCLIP(obtainMessage(7, this));
            } catch (ImsException e) {
                Rlog.d(LOG_TAG, "Could not get UT handle for queryCLIP.");
            }
        } else {
            if (isActivate() || isDeactivate()) {
                try {
                    this.mPhone.mCT.getUtInterface().updateCLIP(isActivate(), obtainMessage(0, this));
                    return;
                } catch (ImsException e2) {
                    Rlog.d(LOG_TAG, "Could not get UT handle for updateCLIP.");
                    return;
                }
            }
            throw new RuntimeException("Invalid or Unsupported MMI Code");
        }
    }

    void handleCLIR() {
        Rlog.d(LOG_TAG, "processCode: is CLIR");
        if (isActivate()) {
            if (!supportMdAutoSetupIms() && this.mPhone.mDefaultPhone.isOpTbClir()) {
                this.mPhone.mDefaultPhone.mCi.setCLIR(1, obtainMessage(0, 1, 0, this));
                return;
            }
            try {
                this.mPhone.mCT.getUtInterface().updateCLIR(1, obtainMessage(0, 1, 0, this));
                return;
            } catch (ImsException e) {
                Rlog.d(LOG_TAG, "Could not get UT handle for updateCLIR.");
                return;
            }
        }
        if (isDeactivate()) {
            if (!supportMdAutoSetupIms() && this.mPhone.mDefaultPhone.isOpTbClir()) {
                this.mPhone.mDefaultPhone.mCi.setCLIR(2, obtainMessage(0, 2, 0, this));
                return;
            }
            try {
                this.mPhone.mCT.getUtInterface().updateCLIR(2, obtainMessage(0, 2, 0, this));
                return;
            } catch (ImsException e2) {
                Rlog.d(LOG_TAG, "Could not get UT handle for updateCLIR.");
                return;
            }
        }
        if (isInterrogate()) {
            if (!supportMdAutoSetupIms() && this.mPhone.mDefaultPhone.isOpTbClir()) {
                Message messageObtainMessage = obtainMessage(6, this);
                if (messageObtainMessage != null) {
                    int[] savedClirSetting = this.mPhone.mDefaultPhone.getSavedClirSetting();
                    Bundle bundle = new Bundle();
                    bundle.putIntArray(MtkImsPhone.UT_BUNDLE_KEY_CLIR, savedClirSetting);
                    AsyncResult.forMessage(messageObtainMessage, bundle, (Throwable) null);
                    messageObtainMessage.sendToTarget();
                    return;
                }
                return;
            }
            try {
                this.mPhone.mCT.getUtInterface().queryCLIR(obtainMessage(6, this));
                return;
            } catch (ImsException e3) {
                Rlog.d(LOG_TAG, "Could not get UT handle for queryCLIR.");
                return;
            }
        }
        throw new RuntimeException("Invalid or Unsupported MMI Code");
    }

    void handleCOLP() {
        Rlog.d(LOG_TAG, "processCode: is COLP");
        if (isInterrogate()) {
            try {
                this.mPhone.mCT.getUtInterface().queryCOLP(obtainMessage(7, this));
            } catch (ImsException e) {
                Rlog.d(LOG_TAG, "Could not get UT handle for queryCOLP.");
            }
        } else {
            if (isActivate() || isDeactivate()) {
                try {
                    this.mPhone.mCT.getUtInterface().updateCOLP(isActivate(), obtainMessage(0, this));
                    return;
                } catch (ImsException e2) {
                    Rlog.d(LOG_TAG, "Could not get UT handle for updateCOLP.");
                    return;
                }
            }
            throw new RuntimeException("Invalid or Unsupported MMI Code");
        }
    }

    void handleCOLR() {
        Rlog.d(LOG_TAG, "processCode: is COLR");
        if (isActivate()) {
            try {
                this.mPhone.mCT.getUtInterface().updateCOLR(1, obtainMessage(0, this));
            } catch (ImsException e) {
                Rlog.d(LOG_TAG, "Could not get UT handle for updateCOLR.");
            }
        } else if (isDeactivate()) {
            try {
                this.mPhone.mCT.getUtInterface().updateCOLR(0, obtainMessage(0, this));
            } catch (ImsException e2) {
                Rlog.d(LOG_TAG, "Could not get UT handle for updateCOLR.");
            }
        } else {
            if (isInterrogate()) {
                try {
                    this.mPhone.mCT.getUtInterface().queryCOLR(obtainMessage(7, this));
                    return;
                } catch (ImsException e3) {
                    Rlog.d(LOG_TAG, "Could not get UT handle for queryCOLR.");
                    return;
                }
            }
            throw new RuntimeException("Invalid or Unsupported MMI Code");
        }
    }

    void handleCallBarringSpecificMT() {
        Rlog.d(LOG_TAG, "processCode: is CB (specifc MT)");
        try {
            if (isInterrogate()) {
                this.mPhone.mCT.getUtInterface().queryCallBarring(10, obtainMessage(10, this));
            } else {
                processIcbMmiCodeForUpdate();
            }
        } catch (ImsException e) {
            Rlog.d(LOG_TAG, "Could not get UT handle for ICB.");
        }
    }

    void handleCallBarringACR() {
        int i;
        Rlog.d(LOG_TAG, "processCode: is CB (ACR)");
        try {
            if (isInterrogate()) {
                this.mPhone.mCT.getUtInterface().queryCallBarring(6, obtainMessage(10, this));
                return;
            }
            if (isActivate()) {
                i = 1;
            } else {
                if (isDeactivate()) {
                }
                i = 0;
            }
            this.mPhone.mCT.getUtInterface().updateCallBarring(6, i, obtainMessage(0, this), (String[]) null);
        } catch (ImsException e) {
            Rlog.d(LOG_TAG, "Could not get UT handle for ICBa.");
        }
    }

    public void handleMessage(Message message) {
        if (triggerMmiCodeCsfb(message)) {
            return;
        }
        int i = message.what;
        if (i == 0) {
            AsyncResult asyncResult = (AsyncResult) message.obj;
            if (!supportMdAutoSetupIms() && this.mSc.equals("43") && this.mPhone.mDefaultPhone.getTbcwMode() == 3 && asyncResult.exception == null) {
                this.mPhone.mDefaultPhone.setTerminalBasedCallWaiting(message.arg1 == 1, null);
            }
            onSetComplete(message, asyncResult);
            return;
        }
        if (i == 2) {
            AsyncResult asyncResult2 = (AsyncResult) message.obj;
            if (asyncResult2.exception != null) {
                ((MtkImsPhone) this.mPhone).mUssiCSFB = true;
                this.mState = MmiCode.State.FAILED;
                this.mMessage = getErrorMessage(asyncResult2);
                this.mPhone.onMMIDone(this);
                return;
            }
            return;
        }
        if (i == 4) {
            AsyncResult asyncResult3 = (AsyncResult) message.obj;
            if (asyncResult3.exception == null && message.arg1 == 1) {
                boolean z = message.arg2 == 1;
                if (this.mPhone.mDefaultPhone.queryCFUAgainAfterSet()) {
                    if (asyncResult3.result != null) {
                        CallForwardInfo[] callForwardInfoArr = (CallForwardInfo[]) asyncResult3.result;
                        if (callForwardInfoArr == null || callForwardInfoArr.length == 0) {
                            Rlog.i(LOG_TAG, "cfInfo is null or length is 0.");
                        } else {
                            int i2 = 0;
                            while (true) {
                                if (i2 >= callForwardInfoArr.length) {
                                    break;
                                }
                                if ((callForwardInfoArr[i2].serviceClass & 1) == 0) {
                                    i2++;
                                } else if (callForwardInfoArr[i2].status == 1) {
                                    Rlog.i(LOG_TAG, "Set CF_ENABLE, serviceClass: " + callForwardInfoArr[i2].serviceClass);
                                    z = true;
                                } else {
                                    Rlog.i(LOG_TAG, "Set CF_DISABLE, serviceClass: " + callForwardInfoArr[i2].serviceClass);
                                    z = false;
                                }
                            }
                        }
                    } else {
                        Rlog.i(LOG_TAG, "ar.result is null.");
                    }
                }
                Rlog.i(LOG_TAG, "EVENT_SET_CFF_COMPLETE: cffEnabled:" + z + ", mDialingNumber=" + this.mDialingNumber + ", mIccRecords=" + this.mIccRecords);
                if (this.mIccRecords != null) {
                    this.mPhone.mDefaultPhone.setVoiceCallForwardingFlag(1, z, this.mDialingNumber);
                    ((MtkImsPhone) this.mPhone).saveTimeSlot(null);
                }
            }
            onSetComplete(message, asyncResult3);
            return;
        }
        super.handleMessage(message);
    }

    public boolean isUssdNumber() {
        if (isTemporaryModeCLIR()) {
            return false;
        }
        if (isShortCode() || this.mDialingNumber != null) {
            return true;
        }
        return (this.mSc == null || !(this.mSc.equals("300") || this.mSc.equals("30") || this.mSc.equals("31") || this.mSc.equals("76") || this.mSc.equals("77") || isServiceCodeCallForwarding(this.mSc) || isServiceCodeCallBarring(this.mSc) || this.mSc.equals("03") || this.mSc.equals("43") || isPinPukCommand())) && this.mPoundString != null;
    }

    public String getUssdDialString() {
        Rlog.d(LOG_TAG, "getUssdDialString(): mDialingNumber=" + this.mDialingNumber + ", mPoundString=" + this.mPoundString);
        if (this.mDialingNumber != null) {
            return this.mDialingNumber;
        }
        return this.mPoundString;
    }

    public static boolean isUtMmiCode(String str, ImsPhone imsPhone) {
        MtkImsPhoneMmiCode mtkImsPhoneMmiCodeNewFromDialString = newFromDialString(str, imsPhone);
        if (mtkImsPhoneMmiCodeNewFromDialString == null || mtkImsPhoneMmiCodeNewFromDialString.isTemporaryModeCLIR() || mtkImsPhoneMmiCodeNewFromDialString.isShortCode() || mtkImsPhoneMmiCodeNewFromDialString.mDialingNumber != null || mtkImsPhoneMmiCodeNewFromDialString.mSc == null || (!mtkImsPhoneMmiCodeNewFromDialString.mSc.equals("30") && !mtkImsPhoneMmiCodeNewFromDialString.mSc.equals("31") && !mtkImsPhoneMmiCodeNewFromDialString.mSc.equals("76") && !mtkImsPhoneMmiCodeNewFromDialString.mSc.equals("77") && !isServiceCodeCallForwarding(mtkImsPhoneMmiCodeNewFromDialString.mSc) && !isServiceCodeCallBarring(mtkImsPhoneMmiCodeNewFromDialString.mSc) && !mtkImsPhoneMmiCodeNewFromDialString.mSc.equals("43") && !mtkImsPhoneMmiCodeNewFromDialString.mSc.equals("156") && !mtkImsPhoneMmiCodeNewFromDialString.mSc.equals("157"))) {
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

    protected void onQueryCfComplete(AsyncResult asyncResult) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        if (asyncResult.exception != null) {
            super.onQueryCfComplete(asyncResult);
            return;
        }
        CallForwardInfo[] callForwardInfoArr = (CallForwardInfo[]) asyncResult.result;
        if (callForwardInfoArr == null || callForwardInfoArr.length == 0) {
            sb.append(this.mContext.getText(R.string.lockscreen_too_many_failed_password_attempts_dialog_message));
            if (this.mIccRecords != null) {
                this.mPhone.setVoiceCallForwardingFlag(1, false, (String) null);
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

    protected void onSuppSvcQueryComplete(AsyncResult asyncResult) {
        if (isServiceCodeCallBarring(this.mSc) && asyncResult.exception == null && !(asyncResult.result instanceof Bundle)) {
            StringBuilder sb = new StringBuilder(getScString());
            sb.append("\n");
            Rlog.d(LOG_TAG, "onSuppSvcQueryComplete: Received Call Barring Response.");
            int[] iArr = (int[]) asyncResult.result;
            if (iArr[0] == 0) {
                sb.append(this.mContext.getText(R.string.lockscreen_too_many_failed_password_attempts_dialog_message));
            } else {
                sb.append(createQueryCallBarringResultMessage(iArr[0]));
            }
            this.mState = MmiCode.State.COMPLETE;
            this.mMessage = sb;
            Rlog.d(LOG_TAG, "onSuppSvcQueryComplete mmi=" + this);
            this.mPhone.onMMIDone(this);
            return;
        }
        super.onSuppSvcQueryComplete(asyncResult);
    }

    private CharSequence createQueryCallBarringResultMessage(int i) {
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

    protected CharSequence getErrorMessage(AsyncResult asyncResult) {
        if (asyncResult.exception instanceof CommandException) {
            CommandException commandException = asyncResult.exception;
            if (commandException.getCommandError() == CommandException.Error.OEM_ERROR_1 && commandException.getMessage() != null && !commandException.getMessage().isEmpty()) {
                return commandException.getMessage();
            }
        }
        return super.getErrorMessage(asyncResult);
    }

    protected static boolean isServiceCodeCallForwarding(String str) {
        return str != null && (str.equals("21") || str.equals("67") || str.equals("61") || str.equals("62") || str.equals("002") || str.equals("004") || str.equals(SC_CFNotRegister));
    }

    protected static int scToCallForwardReason(String str) {
        if (str == null) {
            throw new RuntimeException("invalid call forward sc");
        }
        if (str.equals("002")) {
            return 4;
        }
        if (str.equals("21")) {
            return 0;
        }
        if (str.equals("67")) {
            return 1;
        }
        if (str.equals("62")) {
            return 3;
        }
        if (str.equals("61")) {
            return 2;
        }
        if (str.equals("004")) {
            return 5;
        }
        if (str.equals(SC_CFNotRegister)) {
            return 6;
        }
        throw new RuntimeException("invalid call forward sc");
    }

    private boolean triggerMmiCodeCsfb(Message message) {
        AsyncResult asyncResult;
        if (!supportMdAutoSetupIms() && !this.mPhone.mDefaultPhone.isNotSupportUtToCS() && (asyncResult = (AsyncResult) message.obj) != null && asyncResult.exception != null) {
            if (asyncResult.exception instanceof CommandException) {
                CommandException commandException = asyncResult.exception;
                if (commandException.getCommandError() == CommandException.Error.OPERATION_NOT_ALLOWED) {
                    Rlog.d(LOG_TAG, "handleMessage(): CommandException.Error.UT_XCAP_403_FORBIDDEN");
                    ((MtkImsPhone) this.mPhone).handleMmiCodeCsfb(LastCallFailCause.OEM_CAUSE_6, this);
                    return true;
                }
                if (commandException.getCommandError() == CommandException.Error.NO_NETWORK_FOUND) {
                    Rlog.d(LOG_TAG, "handleMessage(): CommandException.Error.UT_UNKNOWN_HOST");
                    ((MtkImsPhone) this.mPhone).handleMmiCodeCsfb(LastCallFailCause.OEM_CAUSE_7, this);
                    return true;
                }
                return false;
            }
            if (asyncResult.exception instanceof ImsException) {
                ImsException imsException = asyncResult.exception;
                if (imsException.getCode() == 61446) {
                    Rlog.d(LOG_TAG, "handleMessage(): ImsReasonInfo.CODE_UT_XCAP_403_FORBIDDEN");
                    ((MtkImsPhone) this.mPhone).handleMmiCodeCsfb(LastCallFailCause.OEM_CAUSE_6, this);
                    return true;
                }
                if (imsException.getCode() == 61447) {
                    Rlog.d(LOG_TAG, "handleMessage(): ImsReasonInfo.CODE_UT_UNKNOWN_HOST");
                    ((MtkImsPhone) this.mPhone).handleMmiCodeCsfb(LastCallFailCause.OEM_CAUSE_7, this);
                    return true;
                }
                return false;
            }
            return false;
        }
        return false;
    }
}
