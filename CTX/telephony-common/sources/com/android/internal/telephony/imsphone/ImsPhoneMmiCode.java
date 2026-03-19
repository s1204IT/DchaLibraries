package com.android.internal.telephony.imsphone;

import android.R;
import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.ims.ImsSsData;
import android.telephony.ims.ImsSsInfo;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import com.android.ims.ImsException;
import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.uicc.IccRecords;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImsPhoneMmiCode extends Handler implements MmiCode {
    protected static final String ACTION_ACTIVATE = "*";
    protected static final String ACTION_DEACTIVATE = "#";
    protected static final String ACTION_ERASURE = "##";
    protected static final String ACTION_INTERROGATE = "*#";
    protected static final String ACTION_REGISTER = "**";
    protected static final int CLIR_DEFAULT = 0;
    protected static final int CLIR_INVOCATION = 1;
    protected static final int CLIR_NOT_PROVISIONED = 0;
    protected static final int CLIR_PRESENTATION_ALLOWED_TEMPORARY = 4;
    protected static final int CLIR_PRESENTATION_RESTRICTED_TEMPORARY = 3;
    protected static final int CLIR_PROVISIONED_PERMANENT = 1;
    protected static final int CLIR_SUPPRESSION = 2;
    protected static final char END_OF_USSD_COMMAND = '#';
    protected static final int EVENT_GET_CLIR_COMPLETE = 6;
    protected static final int EVENT_QUERY_CF_COMPLETE = 1;
    protected static final int EVENT_QUERY_COMPLETE = 3;
    protected static final int EVENT_QUERY_ICB_COMPLETE = 10;
    protected static final int EVENT_SET_CFF_COMPLETE = 4;
    protected static final int EVENT_SET_COMPLETE = 0;
    protected static final int EVENT_SUPP_SVC_QUERY_COMPLETE = 7;
    protected static final int EVENT_USSD_CANCEL_COMPLETE = 5;
    protected static final int EVENT_USSD_COMPLETE = 2;
    protected static final String IcbAnonymousMmi = "Anonymous Incoming Call Barring";
    protected static final String IcbDnMmi = "Specific Incoming Call Barring";
    static final String LOG_TAG = "ImsPhoneMmiCode";
    protected static final int MATCH_GROUP_ACTION = 2;
    protected static final int MATCH_GROUP_DIALING_NUMBER = 12;
    protected static final int MATCH_GROUP_POUND_STRING = 1;
    protected static final int MATCH_GROUP_PWD_CONFIRM = 11;
    protected static final int MATCH_GROUP_SERVICE_CODE = 3;
    protected static final int MATCH_GROUP_SIA = 5;
    protected static final int MATCH_GROUP_SIB = 7;
    protected static final int MATCH_GROUP_SIC = 9;
    protected static final int MAX_LENGTH_SHORT_CODE = 2;
    protected static final int NUM_PRESENTATION_ALLOWED = 0;
    protected static final int NUM_PRESENTATION_RESTRICTED = 1;
    protected static final String SC_BAIC = "35";
    protected static final String SC_BAICa = "157";
    protected static final String SC_BAICr = "351";
    protected static final String SC_BAOC = "33";
    protected static final String SC_BAOIC = "331";
    protected static final String SC_BAOICxH = "332";
    protected static final String SC_BA_ALL = "330";
    protected static final String SC_BA_MO = "333";
    protected static final String SC_BA_MT = "353";
    protected static final String SC_BS_MT = "156";
    protected static final String SC_CFB = "67";
    protected static final String SC_CFNR = "62";
    protected static final String SC_CFNRy = "61";
    protected static final String SC_CFU = "21";
    protected static final String SC_CFUT = "22";
    protected static final String SC_CF_All = "002";
    protected static final String SC_CF_All_Conditional = "004";
    protected static final String SC_CLIP = "30";
    protected static final String SC_CLIR = "31";
    protected static final String SC_CNAP = "300";
    protected static final String SC_COLP = "76";
    protected static final String SC_COLR = "77";
    protected static final String SC_PIN = "04";
    protected static final String SC_PIN2 = "042";
    protected static final String SC_PUK = "05";
    protected static final String SC_PUK2 = "052";
    protected static final String SC_PWD = "03";
    protected static final String SC_WAIT = "43";
    public static final String UT_BUNDLE_KEY_CLIR = "queryClir";
    public static final String UT_BUNDLE_KEY_SSINFO = "imsSsInfo";
    protected static Pattern sPatternSuppService = Pattern.compile("((\\*|#|\\*#|\\*\\*|##)(\\d{2,3})(\\*([^*#]*)(\\*([^*#]*)(\\*([^*#]*)(\\*([^*#]*))?)?)?)?#)(.*)");
    protected static String[] sTwoDigitNumberPattern;
    protected String mAction;
    protected ResultReceiver mCallbackReceiver;
    protected Context mContext;
    protected String mDialingNumber;
    protected IccRecords mIccRecords;
    protected boolean mIsCallFwdReg;
    protected boolean mIsPendingUSSD;
    protected boolean mIsSsInfo;
    protected boolean mIsUssdRequest;
    protected CharSequence mMessage;
    protected ImsPhone mPhone;
    protected String mPoundString;
    protected String mPwd;
    protected String mSc;
    protected String mSia;
    protected String mSib;
    protected String mSic;
    protected MmiCode.State mState;

    public static ImsPhoneMmiCode newFromDialString(String str, ImsPhone imsPhone) {
        return newFromDialString(str, imsPhone, null);
    }

    public static ImsPhoneMmiCode newFromDialString(String str, ImsPhone imsPhone, ResultReceiver resultReceiver) {
        if (imsPhone.getDefaultPhone().getServiceState().getVoiceRoaming() && imsPhone.getDefaultPhone().supportsConversionOfCdmaCallerIdMmiCodesWhileRoaming()) {
            str = convertCdmaMmiCodesTo3gppMmiCodes(str);
        }
        Matcher matcher = sPatternSuppService.matcher(str);
        if (matcher.matches()) {
            ImsPhoneMmiCode imsPhoneMmiCode = new ImsPhoneMmiCode(imsPhone);
            imsPhoneMmiCode.mPoundString = makeEmptyNull(matcher.group(1));
            imsPhoneMmiCode.mAction = makeEmptyNull(matcher.group(2));
            imsPhoneMmiCode.mSc = makeEmptyNull(matcher.group(3));
            imsPhoneMmiCode.mSia = makeEmptyNull(matcher.group(5));
            imsPhoneMmiCode.mSib = makeEmptyNull(matcher.group(7));
            imsPhoneMmiCode.mSic = makeEmptyNull(matcher.group(9));
            imsPhoneMmiCode.mPwd = makeEmptyNull(matcher.group(11));
            imsPhoneMmiCode.mDialingNumber = makeEmptyNull(matcher.group(12));
            imsPhoneMmiCode.mCallbackReceiver = resultReceiver;
            if (imsPhoneMmiCode.mDialingNumber != null && imsPhoneMmiCode.mDialingNumber.endsWith(ACTION_DEACTIVATE) && str.endsWith(ACTION_DEACTIVATE)) {
                ImsPhoneMmiCode imsPhoneMmiCode2 = new ImsPhoneMmiCode(imsPhone);
                imsPhoneMmiCode2.mPoundString = str;
                return imsPhoneMmiCode2;
            }
            return imsPhoneMmiCode;
        }
        if (str.endsWith(ACTION_DEACTIVATE)) {
            ImsPhoneMmiCode imsPhoneMmiCode3 = new ImsPhoneMmiCode(imsPhone);
            imsPhoneMmiCode3.mPoundString = str;
            return imsPhoneMmiCode3;
        }
        if (isTwoDigitShortCode(imsPhone.getContext(), str) || !isShortCode(str, imsPhone)) {
            return null;
        }
        ImsPhoneMmiCode imsPhoneMmiCode4 = new ImsPhoneMmiCode(imsPhone);
        imsPhoneMmiCode4.mDialingNumber = str;
        return imsPhoneMmiCode4;
    }

    protected static String convertCdmaMmiCodesTo3gppMmiCodes(String str) {
        Matcher matcher = sPatternCdmaMmiCodeWhileRoaming.matcher(str);
        if (matcher.matches()) {
            String strMakeEmptyNull = makeEmptyNull(matcher.group(1));
            String strGroup = matcher.group(2);
            String strMakeEmptyNull2 = makeEmptyNull(matcher.group(3));
            if (strMakeEmptyNull.equals(SC_CFB) && strMakeEmptyNull2 != null) {
                return "#31#" + strGroup + strMakeEmptyNull2;
            }
            if (strMakeEmptyNull.equals("82") && strMakeEmptyNull2 != null) {
                return "*31#" + strGroup + strMakeEmptyNull2;
            }
            return str;
        }
        return str;
    }

    public static ImsPhoneMmiCode newNetworkInitiatedUssd(String str, boolean z, ImsPhone imsPhone) {
        ImsPhoneMmiCode imsPhoneMmiCode = new ImsPhoneMmiCode(imsPhone);
        imsPhoneMmiCode.mMessage = str;
        imsPhoneMmiCode.mIsUssdRequest = z;
        if (z) {
            imsPhoneMmiCode.mIsPendingUSSD = true;
            imsPhoneMmiCode.mState = MmiCode.State.PENDING;
        } else {
            imsPhoneMmiCode.mState = MmiCode.State.COMPLETE;
        }
        return imsPhoneMmiCode;
    }

    public static ImsPhoneMmiCode newFromUssdUserInput(String str, ImsPhone imsPhone) {
        ImsPhoneMmiCode imsPhoneMmiCode = new ImsPhoneMmiCode(imsPhone);
        imsPhoneMmiCode.mMessage = str;
        imsPhoneMmiCode.mState = MmiCode.State.PENDING;
        imsPhoneMmiCode.mIsPendingUSSD = true;
        return imsPhoneMmiCode;
    }

    protected static String makeEmptyNull(String str) {
        if (str == null || str.length() != 0) {
            return str;
        }
        return null;
    }

    protected static boolean isScMatchesSuppServType(String str) {
        Matcher matcher = sPatternSuppService.matcher(str);
        if (matcher.matches()) {
            String strMakeEmptyNull = makeEmptyNull(matcher.group(3));
            if (strMakeEmptyNull.equals(SC_CFUT) || strMakeEmptyNull.equals(SC_BS_MT)) {
                return true;
            }
        }
        return false;
    }

    protected static boolean isEmptyOrNull(CharSequence charSequence) {
        return charSequence == null || charSequence.length() == 0;
    }

    protected static int scToCallForwardReason(String str) {
        if (str == null) {
            throw new RuntimeException("invalid call forward sc");
        }
        if (str.equals(SC_CF_All)) {
            return 4;
        }
        if (str.equals(SC_CFU)) {
            return 0;
        }
        if (str.equals(SC_CFB)) {
            return 1;
        }
        if (str.equals(SC_CFNR)) {
            return 3;
        }
        if (str.equals(SC_CFNRy)) {
            return 2;
        }
        if (str.equals(SC_CF_All_Conditional)) {
            return 5;
        }
        throw new RuntimeException("invalid call forward sc");
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
                                    return 16;
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

    protected static int siToTime(String str) {
        if (str == null || str.length() == 0) {
            return 0;
        }
        return Integer.parseInt(str, 10);
    }

    protected static boolean isServiceCodeCallForwarding(String str) {
        return str != null && (str.equals(SC_CFU) || str.equals(SC_CFB) || str.equals(SC_CFNRy) || str.equals(SC_CFNR) || str.equals(SC_CF_All) || str.equals(SC_CF_All_Conditional));
    }

    protected static boolean isServiceCodeCallBarring(String str) {
        String[] stringArray;
        Resources system = Resources.getSystem();
        if (str != null && (stringArray = system.getStringArray(R.array.config_autoBrightnessLevels)) != null) {
            for (String str2 : stringArray) {
                if (str.equals(str2)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected static String scToBarringFacility(String str) {
        if (str == null) {
            throw new RuntimeException("invalid call barring sc");
        }
        if (str.equals(SC_BAOC)) {
            return CommandsInterface.CB_FACILITY_BAOC;
        }
        if (str.equals(SC_BAOIC)) {
            return CommandsInterface.CB_FACILITY_BAOIC;
        }
        if (str.equals(SC_BAOICxH)) {
            return CommandsInterface.CB_FACILITY_BAOICxH;
        }
        if (str.equals(SC_BAIC)) {
            return CommandsInterface.CB_FACILITY_BAIC;
        }
        if (str.equals(SC_BAICr)) {
            return CommandsInterface.CB_FACILITY_BAICr;
        }
        if (str.equals(SC_BA_ALL)) {
            return CommandsInterface.CB_FACILITY_BA_ALL;
        }
        if (str.equals(SC_BA_MO)) {
            return CommandsInterface.CB_FACILITY_BA_MO;
        }
        if (str.equals(SC_BA_MT)) {
            return CommandsInterface.CB_FACILITY_BA_MT;
        }
        throw new RuntimeException("invalid call barring sc");
    }

    public ImsPhoneMmiCode(ImsPhone imsPhone) {
        super(imsPhone.getHandler().getLooper());
        this.mState = MmiCode.State.PENDING;
        this.mIsSsInfo = false;
        this.mPhone = imsPhone;
        this.mContext = imsPhone.getContext();
        this.mIccRecords = this.mPhone.mDefaultPhone.getIccRecords();
    }

    @Override
    public MmiCode.State getState() {
        return this.mState;
    }

    @Override
    public CharSequence getMessage() {
        return this.mMessage;
    }

    @Override
    public Phone getPhone() {
        return this.mPhone;
    }

    @Override
    public void cancel() {
        if (this.mState == MmiCode.State.COMPLETE || this.mState == MmiCode.State.FAILED) {
            return;
        }
        this.mState = MmiCode.State.CANCELLED;
        if (this.mIsPendingUSSD) {
            this.mPhone.cancelUSSD();
        } else {
            this.mPhone.onMMIDone(this);
        }
    }

    @Override
    public boolean isCancelable() {
        return this.mIsPendingUSSD;
    }

    public String getDialingNumber() {
        return this.mDialingNumber;
    }

    protected boolean isMMI() {
        return this.mPoundString != null;
    }

    protected boolean isShortCode() {
        return this.mPoundString == null && this.mDialingNumber != null && this.mDialingNumber.length() <= 2;
    }

    @Override
    public String getDialString() {
        return this.mPoundString;
    }

    protected static boolean isTwoDigitShortCode(Context context, String str) {
        Rlog.d(LOG_TAG, "isTwoDigitShortCode");
        if (str == null || str.length() > 2) {
            return false;
        }
        if (sTwoDigitNumberPattern == null) {
            sTwoDigitNumberPattern = context.getResources().getStringArray(R.array.config_displayCutoutPathArray);
        }
        for (String str2 : sTwoDigitNumberPattern) {
            Rlog.d(LOG_TAG, "Two Digit Number Pattern " + str2);
            if (str.equals(str2)) {
                Rlog.d(LOG_TAG, "Two Digit Number Pattern -true");
                return true;
            }
        }
        Rlog.d(LOG_TAG, "Two Digit Number Pattern -false");
        return false;
    }

    protected static boolean isShortCode(String str, ImsPhone imsPhone) {
        if (str == null || str.length() == 0 || PhoneNumberUtils.isLocalEmergencyNumber(imsPhone.getContext(), str)) {
            return false;
        }
        return isShortCodeUSSD(str, imsPhone);
    }

    protected static boolean isShortCodeUSSD(String str, ImsPhone imsPhone) {
        return (str == null || str.length() > 2 || (!imsPhone.isInCall() && str.length() == 2 && str.charAt(0) == '1')) ? false : true;
    }

    @Override
    public boolean isPinPukCommand() {
        return this.mSc != null && (this.mSc.equals(SC_PIN) || this.mSc.equals(SC_PIN2) || this.mSc.equals(SC_PUK) || this.mSc.equals(SC_PUK2));
    }

    public boolean isTemporaryModeCLIR() {
        return this.mSc != null && this.mSc.equals(SC_CLIR) && this.mDialingNumber != null && (isActivate() || isDeactivate());
    }

    public int getCLIRMode() {
        if (this.mSc != null && this.mSc.equals(SC_CLIR)) {
            if (isActivate()) {
                return 2;
            }
            if (isDeactivate()) {
                return 1;
            }
            return 0;
        }
        return 0;
    }

    protected boolean isActivate() {
        return this.mAction != null && this.mAction.equals("*");
    }

    protected boolean isDeactivate() {
        return this.mAction != null && this.mAction.equals(ACTION_DEACTIVATE);
    }

    protected boolean isInterrogate() {
        return this.mAction != null && this.mAction.equals(ACTION_INTERROGATE);
    }

    protected boolean isRegister() {
        return this.mAction != null && this.mAction.equals(ACTION_REGISTER);
    }

    protected boolean isErasure() {
        return this.mAction != null && this.mAction.equals(ACTION_ERASURE);
    }

    public boolean isPendingUSSD() {
        return this.mIsPendingUSSD;
    }

    @Override
    public boolean isUssdRequest() {
        return this.mIsUssdRequest;
    }

    public boolean isSupportedOverImsPhone() {
        if (isShortCode()) {
            return true;
        }
        if (isServiceCodeCallForwarding(this.mSc) || isServiceCodeCallBarring(this.mSc) || ((this.mSc != null && this.mSc.equals(SC_WAIT)) || ((this.mSc != null && this.mSc.equals(SC_CLIR)) || ((this.mSc != null && this.mSc.equals(SC_CLIP)) || ((this.mSc != null && this.mSc.equals(SC_COLR)) || ((this.mSc != null && this.mSc.equals(SC_COLP)) || ((this.mSc != null && this.mSc.equals(SC_BS_MT)) || (this.mSc != null && this.mSc.equals(SC_BAICa))))))))) {
            try {
                int iSiToServiceClass = siToServiceClass(this.mSib);
                return iSiToServiceClass == 0 || iSiToServiceClass == 1 || iSiToServiceClass == 80;
            } catch (RuntimeException e) {
                Rlog.d(LOG_TAG, "Invalid service class " + e);
            }
        } else if (!isPinPukCommand() && ((this.mSc == null || (!this.mSc.equals(SC_PWD) && !this.mSc.equals(SC_CLIP) && !this.mSc.equals(SC_CLIR))) && this.mPoundString != null)) {
            return true;
        }
        return false;
    }

    public int callBarAction(String str) {
        if (isActivate()) {
            return 1;
        }
        if (isDeactivate()) {
            return 0;
        }
        if (isRegister()) {
            if (!isEmptyOrNull(str)) {
                return 3;
            }
            throw new RuntimeException("invalid action");
        }
        if (isErasure()) {
            return 4;
        }
        throw new RuntimeException("invalid action");
    }

    @Override
    public void processCode() throws CallStateException {
        int i;
        try {
            if (isShortCode()) {
                Rlog.d(LOG_TAG, "processCode: isShortCode");
                Rlog.d(LOG_TAG, "processCode: Sending short code '" + this.mDialingNumber + "' over CS pipe.");
                throw new CallStateException(Phone.CS_FALLBACK);
            }
            int i2 = 1;
            if (isServiceCodeCallForwarding(this.mSc)) {
                Rlog.d(LOG_TAG, "processCode: is CF");
                String str = this.mSia;
                int iScToCallForwardReason = scToCallForwardReason(this.mSc);
                int iSiToServiceClass = siToServiceClass(this.mSib);
                int iSiToTime = siToTime(this.mSic);
                if (isInterrogate()) {
                    this.mPhone.getCallForwardingOption(iScToCallForwardReason, obtainMessage(1, this));
                } else if (!isActivate()) {
                    if (isDeactivate()) {
                        i = 0;
                    } else if (isRegister()) {
                        i = 3;
                    } else {
                        if (!isErasure()) {
                            throw new RuntimeException("invalid action");
                        }
                        i = 4;
                    }
                    if (iScToCallForwardReason == 0) {
                    }
                } else if (isEmptyOrNull(str)) {
                    this.mIsCallFwdReg = false;
                    i = 1;
                    int i3 = (iScToCallForwardReason == 0 || iScToCallForwardReason == 4) ? 1 : 0;
                    if (i != 1 && i != 3) {
                        i2 = 0;
                    }
                    Rlog.d(LOG_TAG, "processCode: is CF setCallForward");
                    this.mPhone.setCallForwardingOption(i, iScToCallForwardReason, str, iSiToServiceClass, iSiToTime, obtainMessage(4, i3, i2, this));
                } else {
                    this.mIsCallFwdReg = true;
                    i = 3;
                    if (iScToCallForwardReason == 0) {
                        if (i != 1) {
                            i2 = 0;
                        }
                        Rlog.d(LOG_TAG, "processCode: is CF setCallForward");
                        this.mPhone.setCallForwardingOption(i, iScToCallForwardReason, str, iSiToServiceClass, iSiToTime, obtainMessage(4, i3, i2, this));
                    }
                }
            } else if (isServiceCodeCallBarring(this.mSc)) {
                String str2 = this.mSia;
                String strScToBarringFacility = scToBarringFacility(this.mSc);
                int iSiToServiceClass2 = siToServiceClass(this.mSib);
                if (isInterrogate()) {
                    this.mPhone.getCallBarring(strScToBarringFacility, obtainMessage(7, this), iSiToServiceClass2);
                } else {
                    if (!isActivate() && !isDeactivate()) {
                        throw new RuntimeException("Invalid or Unsupported MMI Code");
                    }
                    this.mPhone.setCallBarring(strScToBarringFacility, isActivate(), str2, obtainMessage(0, this), iSiToServiceClass2);
                }
            } else if (this.mSc == null || !this.mSc.equals(SC_CLIR)) {
                if (this.mSc == null || !this.mSc.equals(SC_CLIP)) {
                    if (this.mSc == null || !this.mSc.equals(SC_COLP)) {
                        if (this.mSc == null || !this.mSc.equals(SC_COLR)) {
                            if (this.mSc != null && this.mSc.equals(SC_BS_MT)) {
                                try {
                                    if (isInterrogate()) {
                                        this.mPhone.mCT.getUtInterface().queryCallBarring(10, obtainMessage(10, this));
                                    } else {
                                        processIcbMmiCodeForUpdate();
                                    }
                                } catch (ImsException e) {
                                    Rlog.d(LOG_TAG, "processCode: Could not get UT handle for ICB.");
                                }
                            } else if (this.mSc != null && this.mSc.equals(SC_BAICa)) {
                                try {
                                    if (isInterrogate()) {
                                        this.mPhone.mCT.getUtInterface().queryCallBarring(6, obtainMessage(10, this));
                                    } else {
                                        if (!isActivate()) {
                                            if (isDeactivate()) {
                                            }
                                            i2 = 0;
                                        }
                                        this.mPhone.mCT.getUtInterface().updateCallBarring(6, i2, obtainMessage(0, this), (String[]) null);
                                    }
                                } catch (ImsException e2) {
                                    Rlog.d(LOG_TAG, "processCode: Could not get UT handle for ICBa.");
                                }
                            } else {
                                if (this.mSc == null || !this.mSc.equals(SC_WAIT)) {
                                    if (this.mPoundString == null) {
                                        Rlog.d(LOG_TAG, "processCode: invalid or unsupported MMI");
                                        throw new RuntimeException("Invalid or Unsupported MMI Code");
                                    }
                                    Rlog.d(LOG_TAG, "processCode: Sending pound string '" + this.mDialingNumber + "' over CS pipe.");
                                    throw new CallStateException(Phone.CS_FALLBACK);
                                }
                                int iSiToServiceClass3 = siToServiceClass(this.mSib);
                                if (isActivate() || isDeactivate()) {
                                    this.mPhone.setCallWaiting(isActivate(), iSiToServiceClass3, obtainMessage(0, this));
                                } else {
                                    if (!isInterrogate()) {
                                        throw new RuntimeException("Invalid or Unsupported MMI Code");
                                    }
                                    this.mPhone.getCallWaiting(obtainMessage(3, this));
                                }
                            }
                        } else if (isActivate()) {
                            try {
                                this.mPhone.mCT.getUtInterface().updateCOLR(1, obtainMessage(0, this));
                            } catch (ImsException e3) {
                                Rlog.d(LOG_TAG, "processCode: Could not get UT handle for updateCOLR.");
                            }
                        } else if (isDeactivate()) {
                            try {
                                this.mPhone.mCT.getUtInterface().updateCOLR(0, obtainMessage(0, this));
                            } catch (ImsException e4) {
                                Rlog.d(LOG_TAG, "processCode: Could not get UT handle for updateCOLR.");
                            }
                        } else {
                            if (!isInterrogate()) {
                                throw new RuntimeException("Invalid or Unsupported MMI Code");
                            }
                            try {
                                this.mPhone.mCT.getUtInterface().queryCOLR(obtainMessage(7, this));
                            } catch (ImsException e5) {
                                Rlog.d(LOG_TAG, "processCode: Could not get UT handle for queryCOLR.");
                            }
                        }
                    } else if (isInterrogate()) {
                        try {
                            this.mPhone.mCT.getUtInterface().queryCOLP(obtainMessage(7, this));
                        } catch (ImsException e6) {
                            Rlog.d(LOG_TAG, "processCode: Could not get UT handle for queryCOLP.");
                        }
                    } else {
                        if (!isActivate() && !isDeactivate()) {
                            throw new RuntimeException("Invalid or Unsupported MMI Code");
                        }
                        try {
                            this.mPhone.mCT.getUtInterface().updateCOLP(isActivate(), obtainMessage(0, this));
                        } catch (ImsException e7) {
                            Rlog.d(LOG_TAG, "processCode: Could not get UT handle for updateCOLP.");
                        }
                    }
                } else if (isInterrogate()) {
                    try {
                        this.mPhone.mCT.getUtInterface().queryCLIP(obtainMessage(7, this));
                    } catch (ImsException e8) {
                        Rlog.d(LOG_TAG, "processCode: Could not get UT handle for queryCLIP.");
                    }
                } else {
                    if (!isActivate() && !isDeactivate()) {
                        throw new RuntimeException("Invalid or Unsupported MMI Code");
                    }
                    try {
                        this.mPhone.mCT.getUtInterface().updateCLIP(isActivate(), obtainMessage(0, this));
                    } catch (ImsException e9) {
                        Rlog.d(LOG_TAG, "processCode: Could not get UT handle for updateCLIP.");
                    }
                }
            } else if (isActivate()) {
                try {
                    this.mPhone.mCT.getUtInterface().updateCLIR(1, obtainMessage(0, this));
                } catch (ImsException e10) {
                    Rlog.d(LOG_TAG, "processCode: Could not get UT handle for updateCLIR.");
                }
            } else if (isDeactivate()) {
                try {
                    this.mPhone.mCT.getUtInterface().updateCLIR(2, obtainMessage(0, this));
                } catch (ImsException e11) {
                    Rlog.d(LOG_TAG, "processCode: Could not get UT handle for updateCLIR.");
                }
            } else {
                if (!isInterrogate()) {
                    throw new RuntimeException("Invalid or Unsupported MMI Code");
                }
                try {
                    this.mPhone.mCT.getUtInterface().queryCLIR(obtainMessage(6, this));
                } catch (ImsException e12) {
                    Rlog.d(LOG_TAG, "processCode: Could not get UT handle for queryCLIR.");
                }
            }
        } catch (RuntimeException e13) {
            this.mState = MmiCode.State.FAILED;
            this.mMessage = this.mContext.getText(R.string.ext_media_missing_message);
            Rlog.d(LOG_TAG, "processCode: RuntimeException = " + e13);
            this.mPhone.onMMIDone(this);
        }
    }

    public void onUssdFinished(String str, boolean z) {
        if (this.mState == MmiCode.State.PENDING) {
            if (TextUtils.isEmpty(str)) {
                this.mMessage = this.mContext.getText(R.string.ext_media_init_action);
                Rlog.v(LOG_TAG, "onUssdFinished: no message; using: " + ((Object) this.mMessage));
            } else {
                Rlog.v(LOG_TAG, "onUssdFinished: message: " + str);
                this.mMessage = str;
            }
            this.mIsUssdRequest = z;
            if (!z) {
                this.mState = MmiCode.State.COMPLETE;
            }
            this.mPhone.onMMIDone(this);
        }
    }

    public void onUssdFinishedError() {
        if (this.mState == MmiCode.State.PENDING) {
            this.mState = MmiCode.State.FAILED;
            this.mMessage = this.mContext.getText(R.string.ext_media_missing_message);
            Rlog.d(LOG_TAG, "onUssdFinishedError: mmi=" + this);
            this.mPhone.onMMIDone(this);
        }
    }

    public void sendUssd(String str) {
        this.mIsPendingUSSD = true;
        this.mPhone.sendUSSD(str, obtainMessage(2, this));
    }

    @Override
    public void handleMessage(Message message) {
        boolean z;
        int i = message.what;
        if (i != 10) {
            switch (i) {
                case 0:
                    onSetComplete(message, (AsyncResult) message.obj);
                    break;
                case 1:
                    onQueryCfComplete((AsyncResult) message.obj);
                    break;
                case 2:
                    AsyncResult asyncResult = (AsyncResult) message.obj;
                    if (asyncResult.exception != null) {
                        this.mState = MmiCode.State.FAILED;
                        this.mMessage = getErrorMessage(asyncResult);
                        this.mPhone.onMMIDone(this);
                    }
                    break;
                case 3:
                    onQueryComplete((AsyncResult) message.obj);
                    break;
                case 4:
                    AsyncResult asyncResult2 = (AsyncResult) message.obj;
                    if (asyncResult2.exception == null && message.arg1 == 1) {
                        if (message.arg2 != 1) {
                            z = false;
                        } else {
                            z = true;
                        }
                        if (this.mIccRecords != null) {
                            this.mPhone.setVoiceCallForwardingFlag(1, z, this.mDialingNumber);
                        }
                    }
                    onSetComplete(message, asyncResult2);
                    break;
                case 5:
                    this.mPhone.onMMIDone(this);
                    break;
                case 6:
                    onQueryClirComplete((AsyncResult) message.obj);
                    break;
                case 7:
                    onSuppSvcQueryComplete((AsyncResult) message.obj);
                    break;
            }
        }
        onIcbQueryComplete((AsyncResult) message.obj);
    }

    protected void processIcbMmiCodeForUpdate() {
        String[] strArrSplit;
        String str = this.mSia;
        if (str != null) {
            strArrSplit = str.split("\\$");
        } else {
            strArrSplit = null;
        }
        try {
            this.mPhone.mCT.getUtInterface().updateCallBarring(10, callBarAction(str), obtainMessage(0, this), strArrSplit);
        } catch (ImsException e) {
            Rlog.d(LOG_TAG, "processIcbMmiCodeForUpdate:Could not get UT handle for updating ICB.");
        }
    }

    protected CharSequence getErrorMessage(AsyncResult asyncResult) {
        CharSequence mmiErrorMessage = getMmiErrorMessage(asyncResult);
        if (mmiErrorMessage != null) {
            return mmiErrorMessage;
        }
        return this.mContext.getText(R.string.ext_media_missing_message);
    }

    protected CharSequence getMmiErrorMessage(AsyncResult asyncResult) {
        if (asyncResult.exception instanceof ImsException) {
            int code = asyncResult.exception.getCode();
            if (code == 241) {
                return this.mContext.getText(R.string.ext_media_move_failure_message);
            }
            switch (code) {
                case 822:
                    return this.mContext.getText(R.string.mediasize_japanese_jis_b8);
                case 823:
                    return this.mContext.getText(R.string.mediasize_japanese_kahu);
                case 824:
                    return this.mContext.getText(R.string.mediasize_japanese_jis_exec);
                case 825:
                    return this.mContext.getText(R.string.mediasize_japanese_jis_b9);
                default:
                    return null;
            }
        }
        if (asyncResult.exception instanceof CommandException) {
            CommandException commandException = (CommandException) asyncResult.exception;
            if (commandException.getCommandError() == CommandException.Error.FDN_CHECK_FAILURE) {
                return this.mContext.getText(R.string.ext_media_move_failure_message);
            }
            if (commandException.getCommandError() == CommandException.Error.SS_MODIFIED_TO_DIAL) {
                return this.mContext.getText(R.string.mediasize_japanese_jis_b8);
            }
            if (commandException.getCommandError() == CommandException.Error.SS_MODIFIED_TO_USSD) {
                return this.mContext.getText(R.string.mediasize_japanese_kahu);
            }
            if (commandException.getCommandError() == CommandException.Error.SS_MODIFIED_TO_SS) {
                return this.mContext.getText(R.string.mediasize_japanese_jis_exec);
            }
            if (commandException.getCommandError() == CommandException.Error.SS_MODIFIED_TO_DIAL_VIDEO) {
                return this.mContext.getText(R.string.mediasize_japanese_jis_b9);
            }
        }
        return null;
    }

    protected CharSequence getScString() {
        if (this.mSc != null) {
            if (isServiceCodeCallBarring(this.mSc)) {
                return this.mContext.getText(R.string.config_helpPackageNameKey);
            }
            if (isServiceCodeCallForwarding(this.mSc)) {
                return this.mContext.getText(R.string.config_defaultAssistant);
            }
            if (this.mSc.equals(SC_PWD)) {
                return this.mContext.getText(R.string.config_systemUiIntelligence);
            }
            if (this.mSc.equals(SC_WAIT)) {
                return this.mContext.getText(R.string.config_systemAutomotiveProjection);
            }
            if (this.mSc.equals(SC_CLIP)) {
                return this.mContext.getText(R.string.config_defaultBrowser);
            }
            if (this.mSc.equals(SC_CLIR)) {
                return this.mContext.getText(R.string.config_defaultDialer);
            }
            if (this.mSc.equals(SC_COLP)) {
                return this.mContext.getText(R.string.config_systemGallery);
            }
            if (this.mSc.equals(SC_COLR)) {
                return this.mContext.getText(R.string.config_systemAutomotiveCluster);
            }
            if (this.mSc.equals(SC_BS_MT)) {
                return IcbDnMmi;
            }
            if (this.mSc.equals(SC_BAICa)) {
                return IcbAnonymousMmi;
            }
            return "";
        }
        return "";
    }

    protected void onSetComplete(Message message, AsyncResult asyncResult) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        if (asyncResult.exception != null) {
            this.mState = MmiCode.State.FAILED;
            if (asyncResult.exception instanceof CommandException) {
                CommandException commandException = (CommandException) asyncResult.exception;
                if (commandException.getCommandError() == CommandException.Error.PASSWORD_INCORRECT) {
                    sb.append(this.mContext.getText(R.string.factorytest_not_system));
                } else {
                    CharSequence mmiErrorMessage = getMmiErrorMessage(asyncResult);
                    if (mmiErrorMessage != null) {
                        sb.append(mmiErrorMessage);
                    } else if (commandException.getMessage() != null) {
                        sb.append(commandException.getMessage());
                    } else {
                        sb.append(this.mContext.getText(R.string.ext_media_missing_message));
                    }
                }
            } else if (asyncResult.exception instanceof ImsException) {
                sb.append(getImsErrorMessage(asyncResult));
            }
        } else if (isActivate()) {
            this.mState = MmiCode.State.COMPLETE;
            if (this.mIsCallFwdReg) {
                sb.append(this.mContext.getText(R.string.lockscreen_transport_play_description));
            } else {
                sb.append(this.mContext.getText(R.string.lockscreen_too_many_failed_pin_attempts_dialog_message));
            }
            if (this.mSc.equals(SC_CLIR)) {
                this.mPhone.saveClirSetting(1);
            }
        } else if (isDeactivate()) {
            this.mState = MmiCode.State.COMPLETE;
            sb.append(this.mContext.getText(R.string.lockscreen_too_many_failed_password_attempts_dialog_message));
            if (this.mSc.equals(SC_CLIR)) {
                this.mPhone.saveClirSetting(2);
            }
        } else if (isRegister()) {
            this.mState = MmiCode.State.COMPLETE;
            sb.append(this.mContext.getText(R.string.lockscreen_transport_play_description));
        } else if (isErasure()) {
            this.mState = MmiCode.State.COMPLETE;
            sb.append(this.mContext.getText(R.string.lockscreen_transport_next_description));
        } else {
            this.mState = MmiCode.State.FAILED;
            sb.append(this.mContext.getText(R.string.ext_media_missing_message));
        }
        this.mMessage = sb;
        Rlog.d(LOG_TAG, "onSetComplete: mmi=" + this);
        this.mPhone.onMMIDone(this);
    }

    protected CharSequence serviceClassToCFString(int i) {
        if (i == 4) {
            return this.mContext.getText(R.string.lockscreen_sound_off_label);
        }
        if (i == 8) {
            return this.mContext.getText(R.string.lockscreen_too_many_failed_attempts_countdown);
        }
        if (i == 16) {
            return this.mContext.getText(R.string.lockscreen_sim_unlock_progress_dialog_message);
        }
        if (i == 32) {
            return this.mContext.getText(R.string.lockscreen_sim_puk_locked_message);
        }
        if (i == 64) {
            return this.mContext.getText(R.string.lockscreen_storage_locked);
        }
        if (i != 128) {
            switch (i) {
                case 1:
                    return this.mContext.getText(R.string.lockscreen_too_many_failed_attempts_dialog_message);
                case 2:
                    return this.mContext.getText(R.string.lockscreen_sim_puk_locked_instructions);
                default:
                    return null;
            }
        }
        return this.mContext.getText(R.string.lockscreen_sound_on_label);
    }

    protected CharSequence makeCFQueryResultMessage(CallForwardInfo callForwardInfo, int i) {
        CharSequence text;
        String[] strArr = {"{0}", "{1}", "{2}"};
        CharSequence[] charSequenceArr = new CharSequence[3];
        boolean z = callForwardInfo.reason == 2;
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
            this.mState = MmiCode.State.FAILED;
            if (asyncResult.exception instanceof ImsException) {
                sb.append(getImsErrorMessage(asyncResult));
            } else {
                sb.append(getErrorMessage(asyncResult));
            }
        } else {
            CallForwardInfo[] callForwardInfoArr = (CallForwardInfo[]) asyncResult.result;
            if (callForwardInfoArr == null || callForwardInfoArr.length == 0) {
                sb.append(this.mContext.getText(R.string.lockscreen_too_many_failed_password_attempts_dialog_message));
                if (this.mIccRecords != null) {
                    this.mPhone.setVoiceCallForwardingFlag(1, false, null);
                }
            } else {
                SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
                for (int i = 1; i <= 128; i <<= 1) {
                    int length = callForwardInfoArr.length;
                    for (int i2 = 0; i2 < length; i2++) {
                        if ((callForwardInfoArr[i2].serviceClass & i) != 0) {
                            spannableStringBuilder.append(makeCFQueryResultMessage(callForwardInfoArr[i2], i));
                            spannableStringBuilder.append((CharSequence) "\n");
                        }
                    }
                }
                sb.append((CharSequence) spannableStringBuilder);
            }
            this.mState = MmiCode.State.COMPLETE;
        }
        this.mMessage = sb;
        Rlog.d(LOG_TAG, "onQueryCfComplete: mmi=" + this);
        this.mPhone.onMMIDone(this);
    }

    protected void onSuppSvcQueryComplete(AsyncResult asyncResult) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        this.mState = MmiCode.State.FAILED;
        if (asyncResult.exception != null) {
            if (asyncResult.exception instanceof ImsException) {
                sb.append(getImsErrorMessage(asyncResult));
            } else {
                sb.append(getErrorMessage(asyncResult));
            }
        } else if (asyncResult.result instanceof Bundle) {
            Rlog.d(LOG_TAG, "onSuppSvcQueryComplete: Received CLIP/COLP/COLR Response.");
            ImsSsInfo parcelable = ((Bundle) asyncResult.result).getParcelable(UT_BUNDLE_KEY_SSINFO);
            if (parcelable == null) {
                sb.append(this.mContext.getText(R.string.ext_media_missing_message));
            } else {
                Rlog.d(LOG_TAG, "onSuppSvcQueryComplete: ImsSsInfo mStatus = " + parcelable.getStatus());
                if (parcelable.getStatus() == 0) {
                    sb.append(this.mContext.getText(R.string.lockscreen_too_many_failed_password_attempts_dialog_message));
                    this.mState = MmiCode.State.COMPLETE;
                } else if (parcelable.getStatus() != 1) {
                    sb.append(this.mContext.getText(R.string.ext_media_missing_message));
                } else {
                    sb.append(this.mContext.getText(R.string.lockscreen_too_many_failed_pin_attempts_dialog_message));
                    this.mState = MmiCode.State.COMPLETE;
                }
            }
        } else {
            Rlog.d(LOG_TAG, "onSuppSvcQueryComplete: Received Call Barring Response.");
            if (((int[]) asyncResult.result)[0] == 1) {
                sb.append(this.mContext.getText(R.string.lockscreen_too_many_failed_pin_attempts_dialog_message));
                this.mState = MmiCode.State.COMPLETE;
            } else {
                sb.append(this.mContext.getText(R.string.lockscreen_too_many_failed_password_attempts_dialog_message));
                this.mState = MmiCode.State.COMPLETE;
            }
        }
        this.mMessage = sb;
        Rlog.d(LOG_TAG, "onSuppSvcQueryComplete mmi=" + this);
        this.mPhone.onMMIDone(this);
    }

    protected void onIcbQueryComplete(AsyncResult asyncResult) {
        Rlog.d(LOG_TAG, "onIcbQueryComplete mmi=" + this);
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        if (asyncResult.exception != null) {
            this.mState = MmiCode.State.FAILED;
            if (asyncResult.exception instanceof ImsException) {
                sb.append(getImsErrorMessage(asyncResult));
            } else {
                sb.append(getErrorMessage(asyncResult));
            }
        } else {
            ImsSsInfo[] imsSsInfoArr = (ImsSsInfo[]) asyncResult.result;
            if (imsSsInfoArr.length == 0) {
                sb.append(this.mContext.getText(R.string.lockscreen_too_many_failed_password_attempts_dialog_message));
            } else {
                int length = imsSsInfoArr.length;
                for (int i = 0; i < length; i++) {
                    if (imsSsInfoArr[i].getIcbNum() != null) {
                        sb.append("Num: " + imsSsInfoArr[i].getIcbNum() + " status: " + imsSsInfoArr[i].getStatus() + "\n");
                    } else if (imsSsInfoArr[i].getStatus() == 1) {
                        sb.append(this.mContext.getText(R.string.lockscreen_too_many_failed_pin_attempts_dialog_message));
                    } else {
                        sb.append(this.mContext.getText(R.string.lockscreen_too_many_failed_password_attempts_dialog_message));
                    }
                }
            }
            this.mState = MmiCode.State.COMPLETE;
        }
        this.mMessage = sb;
        this.mPhone.onMMIDone(this);
    }

    protected void onQueryClirComplete(AsyncResult asyncResult) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        this.mState = MmiCode.State.FAILED;
        if (asyncResult.exception != null) {
            if (asyncResult.exception instanceof ImsException) {
                sb.append(getImsErrorMessage(asyncResult));
            }
        } else {
            int[] intArray = ((Bundle) asyncResult.result).getIntArray(UT_BUNDLE_KEY_CLIR);
            Rlog.d(LOG_TAG, "onQueryClirComplete: CLIR param n=" + intArray[0] + " m=" + intArray[1]);
            switch (intArray[1]) {
                case 0:
                    sb.append(this.mContext.getText(R.string.lockscreen_transport_pause_description));
                    this.mState = MmiCode.State.COMPLETE;
                    break;
                case 1:
                    sb.append(this.mContext.getText(R.string.config_feedbackIntentNameKey));
                    this.mState = MmiCode.State.COMPLETE;
                    break;
                case 2:
                default:
                    sb.append(this.mContext.getText(R.string.ext_media_missing_message));
                    this.mState = MmiCode.State.FAILED;
                    break;
                case 3:
                    switch (intArray[0]) {
                        case 0:
                            sb.append(this.mContext.getText(R.string.config_feedbackIntentExtraKey));
                            this.mState = MmiCode.State.COMPLETE;
                            break;
                        case 1:
                            sb.append(this.mContext.getText(R.string.config_feedbackIntentExtraKey));
                            this.mState = MmiCode.State.COMPLETE;
                            break;
                        case 2:
                            sb.append(this.mContext.getText(R.string.config_helpIntentNameKey));
                            this.mState = MmiCode.State.COMPLETE;
                            break;
                        default:
                            sb.append(this.mContext.getText(R.string.ext_media_missing_message));
                            this.mState = MmiCode.State.FAILED;
                            break;
                    }
                    break;
                case 4:
                    switch (intArray[0]) {
                        case 0:
                            sb.append(this.mContext.getText(R.string.config_helpPackageNameValue));
                            this.mState = MmiCode.State.COMPLETE;
                            break;
                        case 1:
                            sb.append(this.mContext.getText(R.string.config_helpIntentExtraKey));
                            this.mState = MmiCode.State.COMPLETE;
                            break;
                        case 2:
                            sb.append(this.mContext.getText(R.string.config_helpPackageNameValue));
                            this.mState = MmiCode.State.COMPLETE;
                            break;
                        default:
                            sb.append(this.mContext.getText(R.string.ext_media_missing_message));
                            this.mState = MmiCode.State.FAILED;
                            break;
                    }
                    break;
            }
        }
        this.mMessage = sb;
        Rlog.d(LOG_TAG, "onQueryClirComplete mmi=" + this);
        this.mPhone.onMMIDone(this);
    }

    protected void onQueryComplete(AsyncResult asyncResult) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        if (asyncResult.exception != null) {
            this.mState = MmiCode.State.FAILED;
            if (asyncResult.exception instanceof ImsException) {
                sb.append(getImsErrorMessage(asyncResult));
            } else {
                sb.append(getErrorMessage(asyncResult));
            }
        } else {
            int[] iArr = (int[]) asyncResult.result;
            if (iArr.length != 0) {
                if (iArr[0] == 0) {
                    sb.append(this.mContext.getText(R.string.lockscreen_too_many_failed_password_attempts_dialog_message));
                } else if (this.mSc.equals(SC_WAIT)) {
                    sb.append(createQueryCallWaitingResultMessage(iArr[1]));
                } else if (iArr[0] == 1) {
                    sb.append(this.mContext.getText(R.string.lockscreen_too_many_failed_pin_attempts_dialog_message));
                } else {
                    sb.append(this.mContext.getText(R.string.ext_media_missing_message));
                }
            } else {
                sb.append(this.mContext.getText(R.string.ext_media_missing_message));
            }
            this.mState = MmiCode.State.COMPLETE;
        }
        this.mMessage = sb;
        Rlog.d(LOG_TAG, "onQueryComplete mmi=" + this);
        this.mPhone.onMMIDone(this);
    }

    protected CharSequence createQueryCallWaitingResultMessage(int i) {
        StringBuilder sb = new StringBuilder(this.mContext.getText(R.string.lockscreen_transport_ffw_description));
        for (int i2 = 1; i2 <= 128; i2 <<= 1) {
            int i3 = i2 & i;
            if (i3 != 0) {
                sb.append("\n");
                sb.append(serviceClassToCFString(i3));
            }
        }
        return sb;
    }

    protected CharSequence getImsErrorMessage(AsyncResult asyncResult) {
        ImsException imsException = asyncResult.exception;
        CharSequence mmiErrorMessage = getMmiErrorMessage(asyncResult);
        if (mmiErrorMessage != null) {
            return mmiErrorMessage;
        }
        if (imsException.getMessage() != null) {
            return imsException.getMessage();
        }
        return getErrorMessage(asyncResult);
    }

    @Override
    public ResultReceiver getUssdCallbackReceiver() {
        return this.mCallbackReceiver;
    }

    public void processImsSsData(AsyncResult asyncResult) throws ImsException {
        try {
            parseSsData((ImsSsData) asyncResult.result);
        } catch (ClassCastException | NullPointerException e) {
            throw new ImsException("Exception in parsing SS Data", 0);
        }
    }

    protected void parseSsData(ImsSsData imsSsData) {
        ImsException imsException;
        if (imsSsData.result != 0) {
            imsException = new ImsException((String) null, imsSsData.result);
        } else {
            imsException = null;
        }
        this.mSc = getScStringFromScType(imsSsData.serviceType);
        this.mAction = getActionStringFromReqType(imsSsData.requestType);
        Rlog.d(LOG_TAG, "parseSsData msc = " + this.mSc + ", action = " + this.mAction + ", ex = " + imsException);
        boolean z = false;
        switch (imsSsData.requestType) {
            case 0:
            case 1:
            case 3:
            case 4:
                if (imsSsData.result == 0 && imsSsData.isTypeUnConditional()) {
                    if ((imsSsData.requestType == 0 || imsSsData.requestType == 3) && isServiceClassVoiceVideoOrNone(imsSsData.serviceClass)) {
                        z = true;
                    }
                    Rlog.d(LOG_TAG, "setCallForwardingFlag cffEnabled: " + z);
                    if (this.mIccRecords != null) {
                        Rlog.d(LOG_TAG, "setVoiceCallForwardingFlag done from SS Info.");
                        this.mPhone.setVoiceCallForwardingFlag(1, z, null);
                    } else {
                        Rlog.e(LOG_TAG, "setCallForwardingFlag aborted. sim records is null.");
                    }
                }
                onSetComplete(null, new AsyncResult((Object) null, imsSsData.getCallForwardInfo(), imsException));
                break;
            case 2:
                if (imsSsData.isTypeClir()) {
                    Rlog.d(LOG_TAG, "CLIR INTERROGATION");
                    Bundle bundle = new Bundle();
                    bundle.putIntArray(UT_BUNDLE_KEY_CLIR, imsSsData.getSuppServiceInfo());
                    onQueryClirComplete(new AsyncResult((Object) null, bundle, imsException));
                } else if (imsSsData.isTypeCF()) {
                    Rlog.d(LOG_TAG, "CALL FORWARD INTERROGATION");
                    onQueryCfComplete(new AsyncResult((Object) null, this.mPhone.handleCfQueryResult(imsSsData.getCallForwardInfo()), imsException));
                } else if (imsSsData.isTypeBarring()) {
                    onSuppSvcQueryComplete(new AsyncResult((Object) null, imsSsData.getSuppServiceInfo(), imsException));
                } else if (imsSsData.isTypeColr() || imsSsData.isTypeClip() || imsSsData.isTypeColp()) {
                    Parcelable imsSsInfo = new ImsSsInfo(imsSsData.getSuppServiceInfo()[0], (String) null);
                    Bundle bundle2 = new Bundle();
                    bundle2.putParcelable(UT_BUNDLE_KEY_SSINFO, imsSsInfo);
                    onSuppSvcQueryComplete(new AsyncResult((Object) null, bundle2, imsException));
                } else if (imsSsData.isTypeIcb()) {
                    onIcbQueryComplete(new AsyncResult((Object) null, imsSsData.getImsSpecificSuppServiceInfo(), imsException));
                } else {
                    onQueryComplete(new AsyncResult((Object) null, imsSsData.getSuppServiceInfo(), imsException));
                }
                break;
            default:
                Rlog.e(LOG_TAG, "Invaid requestType in SSData : " + imsSsData.requestType);
                break;
        }
    }

    protected String getScStringFromScType(int i) {
        switch (i) {
            case 0:
                return SC_CFU;
            case 1:
                return SC_CFB;
            case 2:
                return SC_CFNRy;
            case 3:
                return SC_CFNR;
            case 4:
                return SC_CF_All;
            case 5:
                return SC_CF_All_Conditional;
            case 6:
            default:
                return null;
            case 7:
                return SC_CLIP;
            case 8:
                return SC_CLIR;
            case 9:
                return SC_COLP;
            case 10:
                return SC_COLR;
            case 11:
                return SC_CNAP;
            case 12:
                return SC_WAIT;
            case 13:
                return SC_BAOC;
            case 14:
                return SC_BAOIC;
            case 15:
                return SC_BAOICxH;
            case 16:
                return SC_BAIC;
            case 17:
                return SC_BAICr;
            case 18:
                return SC_BA_ALL;
            case 19:
                return SC_BA_MO;
            case 20:
                return SC_BA_MT;
            case 21:
                return SC_BS_MT;
            case 22:
                return SC_BAICa;
        }
    }

    protected String getActionStringFromReqType(int i) {
        switch (i) {
            case 0:
                return "*";
            case 1:
                return ACTION_DEACTIVATE;
            case 2:
                return ACTION_INTERROGATE;
            case 3:
                return ACTION_REGISTER;
            case 4:
                return ACTION_ERASURE;
            default:
                return null;
        }
    }

    protected boolean isServiceClassVoiceVideoOrNone(int i) {
        return i == 0 || i == 1 || i == 80;
    }

    public boolean isSsInfo() {
        return this.mIsSsInfo;
    }

    public void setIsSsInfo(boolean z) {
        this.mIsSsInfo = z;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ImsPhoneMmiCode {");
        sb.append("State=" + getState());
        if (this.mAction != null) {
            sb.append(" action=" + this.mAction);
        }
        if (this.mSc != null) {
            sb.append(" sc=" + this.mSc);
        }
        if (this.mSia != null) {
            sb.append(" sia=" + this.mSia);
        }
        if (this.mSib != null) {
            sb.append(" sib=" + this.mSib);
        }
        if (this.mSic != null) {
            sb.append(" sic=" + this.mSic);
        }
        if (this.mPoundString != null) {
            sb.append(" poundString=" + Rlog.pii(LOG_TAG, this.mPoundString));
        }
        if (this.mDialingNumber != null) {
            sb.append(" dialingNumber=" + Rlog.pii(LOG_TAG, this.mDialingNumber));
        }
        if (this.mPwd != null) {
            sb.append(" pwd=" + Rlog.pii(LOG_TAG, this.mPwd));
        }
        if (this.mCallbackReceiver != null) {
            sb.append(" hasReceiver");
        }
        sb.append("}");
        return sb.toString();
    }
}
