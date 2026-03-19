package com.android.internal.telephony.gsm;

import android.R;
import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.ResultReceiver;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.text.BidiFormatter;
import android.text.SpannableStringBuilder;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.gsm.SsData;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.util.ArrayUtils;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GsmMmiCode extends Handler implements MmiCode {
    protected static final String ACTION_ACTIVATE = "*";
    protected static final String ACTION_DEACTIVATE = "#";
    protected static final String ACTION_ERASURE = "##";
    protected static final String ACTION_INTERROGATE = "*#";
    protected static final String ACTION_REGISTER = "**";
    protected static final char END_OF_USSD_COMMAND = '#';
    protected static final int EVENT_GET_CLIR_COMPLETE = 2;
    protected static final int EVENT_QUERY_CF_COMPLETE = 3;
    protected static final int EVENT_QUERY_COMPLETE = 5;
    protected static final int EVENT_SET_CFF_COMPLETE = 6;
    protected static final int EVENT_SET_COMPLETE = 1;
    protected static final int EVENT_USSD_CANCEL_COMPLETE = 7;
    protected static final int EVENT_USSD_COMPLETE = 4;
    static final String LOG_TAG = "GsmMmiCode";
    protected static final int MATCH_GROUP_ACTION = 2;
    protected static final int MATCH_GROUP_DIALING_NUMBER = 12;
    protected static final int MATCH_GROUP_POUND_STRING = 1;
    protected static final int MATCH_GROUP_PWD_CONFIRM = 11;
    protected static final int MATCH_GROUP_SERVICE_CODE = 3;
    protected static final int MATCH_GROUP_SIA = 5;
    protected static final int MATCH_GROUP_SIB = 7;
    protected static final int MATCH_GROUP_SIC = 9;
    protected static final int MAX_LENGTH_SHORT_CODE = 2;
    protected static final String SC_BAIC = "35";
    protected static final String SC_BAICr = "351";
    protected static final String SC_BAOC = "33";
    protected static final String SC_BAOIC = "331";
    protected static final String SC_BAOICxH = "332";
    protected static final String SC_BA_ALL = "330";
    protected static final String SC_BA_MO = "333";
    protected static final String SC_BA_MT = "353";
    protected static final String SC_CFB = "67";
    protected static final String SC_CFNR = "62";
    protected static final String SC_CFNRy = "61";
    protected static final String SC_CFU = "21";
    protected static final String SC_CF_All = "002";
    protected static final String SC_CF_All_Conditional = "004";
    protected static final String SC_CLIP = "30";
    protected static final String SC_CLIR = "31";
    protected static final String SC_PIN = "04";
    protected static final String SC_PIN2 = "042";
    protected static final String SC_PUK = "05";
    protected static final String SC_PUK2 = "052";
    protected static final String SC_PWD = "03";
    protected static final String SC_WAIT = "43";
    protected static Pattern sPatternSuppService = Pattern.compile("((\\*|#|\\*#|\\*\\*|##)(\\d{2,3})(\\*([^*#]*)(\\*([^*#]*)(\\*([^*#]*)(\\*([^*#]*))?)?)?)?#)(.*)");
    protected static String[] sTwoDigitNumberPattern;
    protected String mAction;
    protected ResultReceiver mCallbackReceiver;
    protected Context mContext;
    public String mDialingNumber;
    protected IccRecords mIccRecords;
    protected boolean mIsCallFwdReg;
    protected boolean mIsPendingUSSD;
    protected boolean mIsSsInfo;
    protected boolean mIsUssdRequest;
    protected CharSequence mMessage;
    protected GsmCdmaPhone mPhone;
    protected String mPoundString;
    protected String mPwd;
    protected String mSc;
    protected String mSia;
    protected String mSib;
    protected String mSic;
    protected MmiCode.State mState;
    protected UiccCardApplication mUiccApplication;

    public static GsmMmiCode newFromDialString(String str, GsmCdmaPhone gsmCdmaPhone, UiccCardApplication uiccCardApplication) {
        return newFromDialString(str, gsmCdmaPhone, uiccCardApplication, null);
    }

    public static GsmMmiCode newFromDialString(String str, GsmCdmaPhone gsmCdmaPhone, UiccCardApplication uiccCardApplication, ResultReceiver resultReceiver) {
        if (gsmCdmaPhone.getServiceState().getVoiceRoaming() && gsmCdmaPhone.supportsConversionOfCdmaCallerIdMmiCodesWhileRoaming()) {
            str = convertCdmaMmiCodesTo3gppMmiCodes(str);
        }
        Matcher matcher = sPatternSuppService.matcher(str);
        GsmMmiCode gsmMmiCode = null;
        if (matcher.matches()) {
            GsmMmiCode gsmMmiCode2 = new GsmMmiCode(gsmCdmaPhone, uiccCardApplication);
            gsmMmiCode2.mPoundString = makeEmptyNull(matcher.group(1));
            gsmMmiCode2.mAction = makeEmptyNull(matcher.group(2));
            gsmMmiCode2.mSc = makeEmptyNull(matcher.group(3));
            gsmMmiCode2.mSia = makeEmptyNull(matcher.group(5));
            gsmMmiCode2.mSib = makeEmptyNull(matcher.group(7));
            gsmMmiCode2.mSic = makeEmptyNull(matcher.group(9));
            gsmMmiCode2.mPwd = makeEmptyNull(matcher.group(11));
            gsmMmiCode2.mDialingNumber = makeEmptyNull(matcher.group(12));
            if (gsmMmiCode2.mDialingNumber != null && gsmMmiCode2.mDialingNumber.endsWith(ACTION_DEACTIVATE) && str.endsWith(ACTION_DEACTIVATE)) {
                gsmMmiCode = new GsmMmiCode(gsmCdmaPhone, uiccCardApplication);
                gsmMmiCode.mPoundString = str;
            } else if (!gsmMmiCode2.isFacToDial()) {
                gsmMmiCode = gsmMmiCode2;
            }
        } else if (str.endsWith(ACTION_DEACTIVATE)) {
            gsmMmiCode = new GsmMmiCode(gsmCdmaPhone, uiccCardApplication);
            gsmMmiCode.mPoundString = str;
        } else if (!isTwoDigitShortCode(gsmCdmaPhone.getContext(), str) && isShortCode(str, gsmCdmaPhone)) {
            gsmMmiCode = new GsmMmiCode(gsmCdmaPhone, uiccCardApplication);
            gsmMmiCode.mDialingNumber = str;
        }
        if (gsmMmiCode != null) {
            gsmMmiCode.mCallbackReceiver = resultReceiver;
        }
        return gsmMmiCode;
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

    public static GsmMmiCode newNetworkInitiatedUssd(String str, boolean z, GsmCdmaPhone gsmCdmaPhone, UiccCardApplication uiccCardApplication) {
        GsmMmiCode gsmMmiCode = new GsmMmiCode(gsmCdmaPhone, uiccCardApplication);
        gsmMmiCode.mMessage = str;
        gsmMmiCode.mIsUssdRequest = z;
        if (z) {
            gsmMmiCode.mIsPendingUSSD = true;
            gsmMmiCode.mState = MmiCode.State.PENDING;
        } else {
            gsmMmiCode.mState = MmiCode.State.COMPLETE;
        }
        return gsmMmiCode;
    }

    public static GsmMmiCode newFromUssdUserInput(String str, GsmCdmaPhone gsmCdmaPhone, UiccCardApplication uiccCardApplication) {
        GsmMmiCode gsmMmiCode = new GsmMmiCode(gsmCdmaPhone, uiccCardApplication);
        gsmMmiCode.mMessage = str;
        gsmMmiCode.mState = MmiCode.State.PENDING;
        gsmMmiCode.mIsPendingUSSD = true;
        return gsmMmiCode;
    }

    public void processSsData(AsyncResult asyncResult) {
        Rlog.d(LOG_TAG, "In processSsData");
        this.mIsSsInfo = true;
        try {
            parseSsData((SsData) asyncResult.result);
        } catch (ClassCastException e) {
            Rlog.e(LOG_TAG, "Class Cast Exception in parsing SS Data : " + e);
        } catch (NullPointerException e2) {
            Rlog.e(LOG_TAG, "Null Pointer Exception in parsing SS Data : " + e2);
        }
    }

    protected void parseSsData(SsData ssData) {
        boolean z;
        CommandException commandExceptionFromRilErrno = CommandException.fromRilErrno(ssData.result);
        this.mSc = getScStringFromScType(ssData.serviceType);
        this.mAction = getActionStringFromReqType(ssData.requestType);
        Rlog.d(LOG_TAG, "parseSsData msc = " + this.mSc + ", action = " + this.mAction + ", ex = " + commandExceptionFromRilErrno);
        switch (ssData.requestType) {
            case SS_ACTIVATION:
            case SS_DEACTIVATION:
            case SS_REGISTRATION:
            case SS_ERASURE:
                if (ssData.result == 0 && ssData.serviceType.isTypeUnConditional()) {
                    if ((ssData.requestType != SsData.RequestType.SS_ACTIVATION && ssData.requestType != SsData.RequestType.SS_REGISTRATION) || !isServiceClassVoiceorNone(ssData.serviceClass)) {
                        z = false;
                    } else {
                        z = true;
                    }
                    Rlog.d(LOG_TAG, "setVoiceCallForwardingFlag cffEnabled: " + z);
                    if (this.mIccRecords != null) {
                        this.mPhone.setVoiceCallForwardingFlag(1, z, null);
                        Rlog.d(LOG_TAG, "setVoiceCallForwardingFlag done from SS Info.");
                    } else {
                        Rlog.e(LOG_TAG, "setVoiceCallForwardingFlag aborted. sim records is null.");
                    }
                }
                onSetComplete(null, new AsyncResult((Object) null, ssData.cfInfo, commandExceptionFromRilErrno));
                break;
            case SS_INTERROGATION:
                if (ssData.serviceType.isTypeClir()) {
                    Rlog.d(LOG_TAG, "CLIR INTERROGATION");
                    onGetClirComplete(new AsyncResult((Object) null, ssData.ssInfo, commandExceptionFromRilErrno));
                } else if (ssData.serviceType.isTypeCF()) {
                    Rlog.d(LOG_TAG, "CALL FORWARD INTERROGATION");
                    onQueryCfComplete(new AsyncResult((Object) null, ssData.cfInfo, commandExceptionFromRilErrno));
                } else {
                    onQueryComplete(new AsyncResult((Object) null, ssData.ssInfo, commandExceptionFromRilErrno));
                }
                break;
            default:
                Rlog.e(LOG_TAG, "Invaid requestType in SSData : " + ssData.requestType);
                break;
        }
    }

    protected String getScStringFromScType(SsData.ServiceType serviceType) {
        switch (serviceType) {
            case SS_CFU:
                return SC_CFU;
            case SS_CF_BUSY:
                return SC_CFB;
            case SS_CF_NO_REPLY:
                return SC_CFNRy;
            case SS_CF_NOT_REACHABLE:
                return SC_CFNR;
            case SS_CF_ALL:
                return SC_CF_All;
            case SS_CF_ALL_CONDITIONAL:
                return SC_CF_All_Conditional;
            case SS_CLIP:
                return SC_CLIP;
            case SS_CLIR:
                return SC_CLIR;
            case SS_WAIT:
                return SC_WAIT;
            case SS_BAOC:
                return SC_BAOC;
            case SS_BAOIC:
                return SC_BAOIC;
            case SS_BAOIC_EXC_HOME:
                return SC_BAOICxH;
            case SS_BAIC:
                return SC_BAIC;
            case SS_BAIC_ROAMING:
                return SC_BAICr;
            case SS_ALL_BARRING:
                return SC_BA_ALL;
            case SS_OUTGOING_BARRING:
                return SC_BA_MO;
            case SS_INCOMING_BARRING:
                return SC_BA_MT;
            default:
                return "";
        }
    }

    protected String getActionStringFromReqType(SsData.RequestType requestType) {
        switch (requestType) {
            case SS_ACTIVATION:
                return "*";
            case SS_DEACTIVATION:
                return ACTION_DEACTIVATE;
            case SS_REGISTRATION:
                return ACTION_REGISTER;
            case SS_ERASURE:
                return ACTION_ERASURE;
            case SS_INTERROGATION:
                return ACTION_INTERROGATE;
            default:
                return "";
        }
    }

    protected boolean isServiceClassVoiceorNone(int i) {
        return (i & 1) != 0 || i == 0;
    }

    protected static String makeEmptyNull(String str) {
        if (str == null || str.length() != 0) {
            return str;
        }
        return null;
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

    public GsmMmiCode(GsmCdmaPhone gsmCdmaPhone, UiccCardApplication uiccCardApplication) {
        super(gsmCdmaPhone.getHandler().getLooper());
        this.mState = MmiCode.State.PENDING;
        this.mIsSsInfo = false;
        this.mPhone = gsmCdmaPhone;
        this.mContext = gsmCdmaPhone.getContext();
        this.mUiccApplication = uiccCardApplication;
        if (uiccCardApplication != null) {
            this.mIccRecords = uiccCardApplication.getIccRecords();
        }
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
            this.mPhone.mCi.cancelPendingUssd(obtainMessage(7, this));
        } else {
            this.mPhone.onMMIDone(this);
        }
    }

    @Override
    public boolean isCancelable() {
        return this.mIsPendingUSSD;
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

    protected static boolean isShortCode(String str, GsmCdmaPhone gsmCdmaPhone) {
        if (str == null || str.length() == 0 || PhoneNumberUtils.isLocalEmergencyNumber(gsmCdmaPhone.getContext(), str)) {
            return false;
        }
        return isShortCodeUSSD(str, gsmCdmaPhone);
    }

    protected static boolean isShortCodeUSSD(String str, GsmCdmaPhone gsmCdmaPhone) {
        return (str == null || str.length() > 2 || (!gsmCdmaPhone.isInCall() && str.length() == 2 && str.charAt(0) == '1')) ? false : true;
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

    protected boolean isFacToDial() {
        PersistableBundle configForSubId = ((CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config")).getConfigForSubId(this.mPhone.getSubId());
        if (configForSubId != null) {
            String[] stringArray = configForSubId.getStringArray("feature_access_codes_string_array");
            if (!ArrayUtils.isEmpty(stringArray)) {
                for (String str : stringArray) {
                    if (str.equals(this.mSc)) {
                        return true;
                    }
                }
            }
        }
        return false;
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

    public boolean isSsInfo() {
        return this.mIsSsInfo;
    }

    public static boolean isVoiceUnconditionalForwarding(int i, int i2) {
        return (i == 0 || i == 4) && ((i2 & 1) != 0 || i2 == 0);
    }

    @Override
    public void processCode() throws CallStateException {
        int i;
        try {
            if (isShortCode()) {
                Rlog.d(LOG_TAG, "processCode: isShortCode");
                sendUssd(this.mDialingNumber);
            } else {
                if (this.mDialingNumber != null) {
                    throw new RuntimeException("Invalid or Unsupported MMI Code");
                }
                if (this.mSc == null || !this.mSc.equals(SC_CLIP)) {
                    int i2 = 1;
                    if (this.mSc != null && this.mSc.equals(SC_CLIR)) {
                        Rlog.d(LOG_TAG, "processCode: is CLIR");
                        if (isActivate()) {
                            this.mPhone.mCi.setCLIR(1, obtainMessage(1, this));
                        } else if (isDeactivate()) {
                            this.mPhone.mCi.setCLIR(2, obtainMessage(1, this));
                        } else {
                            if (!isInterrogate()) {
                                throw new RuntimeException("Invalid or Unsupported MMI Code");
                            }
                            this.mPhone.mCi.getCLIR(obtainMessage(2, this));
                        }
                    } else if (isServiceCodeCallForwarding(this.mSc)) {
                        Rlog.d(LOG_TAG, "processCode: is CF");
                        String str = this.mSia;
                        int iSiToServiceClass = siToServiceClass(this.mSib);
                        int iScToCallForwardReason = scToCallForwardReason(this.mSc);
                        int iSiToTime = siToTime(this.mSic);
                        if (isInterrogate()) {
                            this.mPhone.mCi.queryCallForwardStatus(iScToCallForwardReason, iSiToServiceClass, str, obtainMessage(3, this));
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
                            if (i != 1) {
                            }
                            Rlog.d(LOG_TAG, "processCode: is CF setCallForward");
                            this.mPhone.mCi.setCallForward(i, iScToCallForwardReason, iSiToServiceClass, str, iSiToTime, obtainMessage(6, isVoiceUnconditionalForwarding(iScToCallForwardReason, iSiToServiceClass) ? 1 : 0, i2, this));
                        } else if (isEmptyOrNull(str)) {
                            this.mIsCallFwdReg = false;
                            i = 1;
                            if (i != 1 && i != 3) {
                                i2 = 0;
                            }
                            Rlog.d(LOG_TAG, "processCode: is CF setCallForward");
                            this.mPhone.mCi.setCallForward(i, iScToCallForwardReason, iSiToServiceClass, str, iSiToTime, obtainMessage(6, isVoiceUnconditionalForwarding(iScToCallForwardReason, iSiToServiceClass) ? 1 : 0, i2, this));
                        } else {
                            this.mIsCallFwdReg = true;
                            i = 3;
                            if (i != 1) {
                                i2 = 0;
                            }
                            Rlog.d(LOG_TAG, "processCode: is CF setCallForward");
                            this.mPhone.mCi.setCallForward(i, iScToCallForwardReason, iSiToServiceClass, str, iSiToTime, obtainMessage(6, isVoiceUnconditionalForwarding(iScToCallForwardReason, iSiToServiceClass) ? 1 : 0, i2, this));
                        }
                    } else if (isServiceCodeCallBarring(this.mSc)) {
                        String str2 = this.mSia;
                        int iSiToServiceClass2 = siToServiceClass(this.mSib);
                        String strScToBarringFacility = scToBarringFacility(this.mSc);
                        if (isInterrogate()) {
                            this.mPhone.mCi.queryFacilityLock(strScToBarringFacility, str2, iSiToServiceClass2, obtainMessage(5, this));
                        } else {
                            if (!isActivate() && !isDeactivate()) {
                                throw new RuntimeException("Invalid or Unsupported MMI Code");
                            }
                            this.mPhone.mCi.setFacilityLock(strScToBarringFacility, isActivate(), str2, iSiToServiceClass2, obtainMessage(1, this));
                        }
                    } else if (this.mSc != null && this.mSc.equals(SC_PWD)) {
                        String str3 = this.mSib;
                        String str4 = this.mSic;
                        if (!isActivate() && !isRegister()) {
                            throw new RuntimeException("Invalid or Unsupported MMI Code");
                        }
                        this.mAction = ACTION_REGISTER;
                        String strScToBarringFacility2 = this.mSia == null ? CommandsInterface.CB_FACILITY_BA_ALL : scToBarringFacility(this.mSia);
                        if (str4.equals(this.mPwd)) {
                            this.mPhone.mCi.changeBarringPassword(strScToBarringFacility2, str3, str4, obtainMessage(1, this));
                        } else {
                            handlePasswordError(R.string.factorytest_not_system);
                        }
                    } else if (this.mSc != null && this.mSc.equals(SC_WAIT)) {
                        int iSiToServiceClass3 = siToServiceClass(this.mSia);
                        if (isActivate() || isDeactivate()) {
                            this.mPhone.mCi.setCallWaiting(isActivate(), iSiToServiceClass3, obtainMessage(1, this));
                        } else {
                            if (!isInterrogate()) {
                                throw new RuntimeException("Invalid or Unsupported MMI Code");
                            }
                            this.mPhone.mCi.queryCallWaiting(iSiToServiceClass3, obtainMessage(5, this));
                        }
                    } else if (isPinPukCommand()) {
                        String str5 = this.mSia;
                        String str6 = this.mSib;
                        int length = str6.length();
                        if (!isRegister()) {
                            throw new RuntimeException("Ivalid register/action=" + this.mAction);
                        }
                        if (!str6.equals(this.mSic)) {
                            handlePasswordError(R.string.expand_action_accessibility);
                        } else if (length < 4 || length > 8) {
                            handlePasswordError(R.string.config_oemCredentialManagerDialogComponent);
                        } else if (this.mSc.equals(SC_PIN) && this.mUiccApplication != null && this.mUiccApplication.getState() == IccCardApplicationStatus.AppState.APPSTATE_PUK) {
                            handlePasswordError(R.string.ext_media_new_notification_message);
                        } else {
                            if (this.mUiccApplication == null) {
                                throw new RuntimeException("No application mUiccApplicaiton is null");
                            }
                            Rlog.d(LOG_TAG, "processCode: process mmi service code using UiccApp sc=" + this.mSc);
                            if (this.mSc.equals(SC_PIN)) {
                                this.mUiccApplication.changeIccLockPassword(str5, str6, obtainMessage(1, this));
                            } else if (this.mSc.equals(SC_PIN2)) {
                                this.mUiccApplication.changeIccFdnPassword(str5, str6, obtainMessage(1, this));
                            } else if (this.mSc.equals(SC_PUK)) {
                                this.mUiccApplication.supplyPuk(str5, str6, obtainMessage(1, this));
                            } else {
                                if (!this.mSc.equals(SC_PUK2)) {
                                    throw new RuntimeException("uicc unsupported service code=" + this.mSc);
                                }
                                this.mUiccApplication.supplyPuk2(str5, str6, obtainMessage(1, this));
                            }
                        }
                    } else {
                        if (this.mPoundString == null) {
                            Rlog.d(LOG_TAG, "processCode: Invalid or Unsupported MMI Code");
                            throw new RuntimeException("Invalid or Unsupported MMI Code");
                        }
                        sendUssd(this.mPoundString);
                    }
                } else {
                    Rlog.d(LOG_TAG, "processCode: is CLIP");
                    if (!isInterrogate()) {
                        throw new RuntimeException("Invalid or Unsupported MMI Code");
                    }
                    this.mPhone.mCi.queryCLIP(obtainMessage(5, this));
                }
            }
        } catch (RuntimeException e) {
            this.mState = MmiCode.State.FAILED;
            this.mMessage = this.mContext.getText(R.string.ext_media_missing_message);
            Rlog.d(LOG_TAG, "processCode: RuntimeException=" + e);
            this.mPhone.onMMIDone(this);
        }
    }

    protected void handlePasswordError(int i) {
        this.mState = MmiCode.State.FAILED;
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        sb.append(this.mContext.getText(i));
        this.mMessage = sb;
        this.mPhone.onMMIDone(this);
    }

    public void onUssdFinished(String str, boolean z) {
        if (this.mState == MmiCode.State.PENDING) {
            if (TextUtils.isEmpty(str)) {
                Rlog.d(LOG_TAG, "onUssdFinished: no network provided message; using default.");
                this.mMessage = this.mContext.getText(R.string.ext_media_init_action);
            } else {
                this.mMessage = str;
            }
            this.mIsUssdRequest = z;
            if (!z) {
                this.mState = MmiCode.State.COMPLETE;
            }
            Rlog.d(LOG_TAG, "onUssdFinished: ussdMessage=" + str);
            this.mPhone.onMMIDone(this);
        }
    }

    public void onUssdFinishedError() {
        if (this.mState == MmiCode.State.PENDING) {
            this.mState = MmiCode.State.FAILED;
            this.mMessage = this.mContext.getText(R.string.ext_media_missing_message);
            Rlog.d(LOG_TAG, "onUssdFinishedError");
            this.mPhone.onMMIDone(this);
        }
    }

    public void onUssdRelease() {
        if (this.mState == MmiCode.State.PENDING) {
            this.mState = MmiCode.State.COMPLETE;
            this.mMessage = null;
            Rlog.d(LOG_TAG, "onUssdRelease");
            this.mPhone.onMMIDone(this);
        }
    }

    public void sendUssd(String str) {
        this.mIsPendingUSSD = true;
        this.mPhone.mCi.sendUSSD(str, obtainMessage(4, this));
    }

    @Override
    public void handleMessage(Message message) {
        boolean z;
        switch (message.what) {
            case 1:
                onSetComplete(message, (AsyncResult) message.obj);
                break;
            case 2:
                onGetClirComplete((AsyncResult) message.obj);
                break;
            case 3:
                onQueryCfComplete((AsyncResult) message.obj);
                break;
            case 4:
                AsyncResult asyncResult = (AsyncResult) message.obj;
                if (asyncResult.exception != null) {
                    this.mState = MmiCode.State.FAILED;
                    this.mMessage = getErrorMessage(asyncResult);
                    this.mPhone.onMMIDone(this);
                }
                break;
            case 5:
                onQueryComplete((AsyncResult) message.obj);
                break;
            case 6:
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
            case 7:
                this.mPhone.onMMIDone(this);
                break;
        }
    }

    protected CharSequence getErrorMessage(AsyncResult asyncResult) {
        if (asyncResult.exception instanceof CommandException) {
            CommandException.Error commandError = ((CommandException) asyncResult.exception).getCommandError();
            if (commandError == CommandException.Error.FDN_CHECK_FAILURE) {
                Rlog.i(LOG_TAG, "FDN_CHECK_FAILURE");
                return this.mContext.getText(R.string.ext_media_move_failure_message);
            }
            if (commandError == CommandException.Error.USSD_MODIFIED_TO_DIAL) {
                Rlog.i(LOG_TAG, "USSD_MODIFIED_TO_DIAL");
                return this.mContext.getText(R.string.mediasize_japanese_kaku2);
            }
            if (commandError == CommandException.Error.USSD_MODIFIED_TO_SS) {
                Rlog.i(LOG_TAG, "USSD_MODIFIED_TO_SS");
                return this.mContext.getText(R.string.mediasize_japanese_oufuku);
            }
            if (commandError == CommandException.Error.USSD_MODIFIED_TO_USSD) {
                Rlog.i(LOG_TAG, "USSD_MODIFIED_TO_USSD");
                return this.mContext.getText(R.string.mediasize_japanese_you4);
            }
            if (commandError == CommandException.Error.SS_MODIFIED_TO_DIAL) {
                Rlog.i(LOG_TAG, "SS_MODIFIED_TO_DIAL");
                return this.mContext.getText(R.string.mediasize_japanese_jis_b8);
            }
            if (commandError == CommandException.Error.SS_MODIFIED_TO_USSD) {
                Rlog.i(LOG_TAG, "SS_MODIFIED_TO_USSD");
                return this.mContext.getText(R.string.mediasize_japanese_kahu);
            }
            if (commandError == CommandException.Error.SS_MODIFIED_TO_SS) {
                Rlog.i(LOG_TAG, "SS_MODIFIED_TO_SS");
                return this.mContext.getText(R.string.mediasize_japanese_jis_exec);
            }
            if (commandError == CommandException.Error.OEM_ERROR_1) {
                Rlog.i(LOG_TAG, "OEM_ERROR_1 USSD_MODIFIED_TO_DIAL_VIDEO");
                return this.mContext.getText(R.string.mediasize_japanese_l);
            }
        }
        return this.mContext.getText(R.string.ext_media_missing_message);
    }

    protected CharSequence getScString() {
        if (this.mSc != null) {
            if (isServiceCodeCallBarring(this.mSc)) {
                return this.mContext.getText(R.string.config_helpPackageNameKey);
            }
            if (isServiceCodeCallForwarding(this.mSc)) {
                return this.mContext.getText(R.string.config_defaultAssistant);
            }
            if (this.mSc.equals(SC_CLIP)) {
                return this.mContext.getText(R.string.config_defaultBrowser);
            }
            if (this.mSc.equals(SC_CLIR)) {
                return this.mContext.getText(R.string.config_defaultDialer);
            }
            if (this.mSc.equals(SC_PWD)) {
                return this.mContext.getText(R.string.config_systemUiIntelligence);
            }
            if (this.mSc.equals(SC_WAIT)) {
                return this.mContext.getText(R.string.config_systemAutomotiveProjection);
            }
            if (isPinPukCommand()) {
                return this.mContext.getText(R.string.config_systemTelevisionNotificationHandler);
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
                CommandException.Error commandError = ((CommandException) asyncResult.exception).getCommandError();
                if (commandError == CommandException.Error.PASSWORD_INCORRECT) {
                    if (isPinPukCommand()) {
                        if (this.mSc.equals(SC_PUK) || this.mSc.equals(SC_PUK2)) {
                            sb.append(this.mContext.getText(R.string.accept));
                        } else {
                            sb.append(this.mContext.getText(R.string.ThreeWCMmi));
                        }
                        int i = message.arg1;
                        if (i <= 0) {
                            Rlog.d(LOG_TAG, "onSetComplete: PUK locked, cancel as lock screen will handle this");
                            this.mState = MmiCode.State.CANCELLED;
                        } else if (i > 0) {
                            Rlog.d(LOG_TAG, "onSetComplete: attemptsRemaining=" + i);
                            sb.append(this.mContext.getResources().getQuantityString(R.plurals.matches_found, i, Integer.valueOf(i)));
                        }
                    } else {
                        sb.append(this.mContext.getText(R.string.factorytest_not_system));
                    }
                } else if (commandError == CommandException.Error.SIM_PUK2) {
                    sb.append(this.mContext.getText(R.string.ThreeWCMmi));
                    sb.append("\n");
                    sb.append(this.mContext.getText(R.string.ext_media_new_notification_title));
                } else if (commandError == CommandException.Error.REQUEST_NOT_SUPPORTED) {
                    if (this.mSc.equals(SC_PIN)) {
                        sb.append(this.mContext.getText(R.string.capability_desc_canRetrieveWindowContent));
                    }
                } else if (commandError == CommandException.Error.FDN_CHECK_FAILURE) {
                    Rlog.i(LOG_TAG, "FDN_CHECK_FAILURE");
                    sb.append(this.mContext.getText(R.string.ext_media_move_failure_message));
                } else if (commandError == CommandException.Error.MODEM_ERR && isServiceCodeCallForwarding(this.mSc) && this.mPhone.getServiceState().getVoiceRoaming() && !this.mPhone.supports3gppCallForwardingWhileRoaming()) {
                    sb.append(this.mContext.getText(R.string.ext_media_missing_title));
                } else {
                    sb.append(getErrorMessage(asyncResult));
                }
            } else {
                sb.append(this.mContext.getText(R.string.ext_media_missing_message));
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
        Rlog.d(LOG_TAG, "onSetComplete mmi=" + this);
        this.mPhone.onMMIDone(this);
    }

    protected void onGetClirComplete(AsyncResult asyncResult) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        if (asyncResult.exception != null) {
            this.mState = MmiCode.State.FAILED;
            sb.append(getErrorMessage(asyncResult));
        } else {
            int[] iArr = (int[]) asyncResult.result;
            switch (iArr[1]) {
                case 0:
                    sb.append(this.mContext.getText(R.string.lockscreen_transport_pause_description));
                    this.mState = MmiCode.State.COMPLETE;
                    break;
                case 1:
                    sb.append(this.mContext.getText(R.string.config_feedbackIntentNameKey));
                    this.mState = MmiCode.State.COMPLETE;
                    break;
                case 2:
                    sb.append(this.mContext.getText(R.string.ext_media_missing_message));
                    this.mState = MmiCode.State.FAILED;
                    break;
                case 3:
                    switch (iArr[0]) {
                        case 1:
                            sb.append(this.mContext.getText(R.string.config_feedbackIntentExtraKey));
                            break;
                        case 2:
                            sb.append(this.mContext.getText(R.string.config_helpIntentNameKey));
                            break;
                        default:
                            sb.append(this.mContext.getText(R.string.config_feedbackIntentExtraKey));
                            break;
                    }
                    this.mState = MmiCode.State.COMPLETE;
                    break;
                case 4:
                    switch (iArr[0]) {
                        case 1:
                            sb.append(this.mContext.getText(R.string.config_helpIntentExtraKey));
                            break;
                        case 2:
                            sb.append(this.mContext.getText(R.string.config_helpPackageNameValue));
                            break;
                        default:
                            sb.append(this.mContext.getText(R.string.config_helpPackageNameValue));
                            break;
                    }
                    this.mState = MmiCode.State.COMPLETE;
                    break;
            }
        }
        this.mMessage = sb;
        Rlog.d(LOG_TAG, "onGetClirComplete: mmi=" + this);
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
        charSequenceArr[1] = formatLtr(PhoneNumberUtils.stringFromStringAndTOA(callForwardInfo.number, callForwardInfo.toa));
        charSequenceArr[2] = Integer.toString(callForwardInfo.timeSeconds);
        if (callForwardInfo.reason == 0 && (i & callForwardInfo.serviceClass) == 1) {
            boolean z2 = callForwardInfo.status == 1;
            if (this.mIccRecords != null) {
                this.mPhone.setVoiceCallForwardingFlag(1, z2, callForwardInfo.number);
            }
        }
        return TextUtils.replace(text, strArr, charSequenceArr);
    }

    protected String formatLtr(String str) {
        return str == null ? str : BidiFormatter.getInstance().unicodeWrap(str, TextDirectionHeuristics.LTR, true);
    }

    protected void onQueryCfComplete(AsyncResult asyncResult) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        if (asyncResult.exception != null) {
            this.mState = MmiCode.State.FAILED;
            sb.append(getErrorMessage(asyncResult));
        } else {
            CallForwardInfo[] callForwardInfoArr = (CallForwardInfo[]) asyncResult.result;
            if (callForwardInfoArr.length == 0) {
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

    protected void onQueryComplete(AsyncResult asyncResult) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        if (asyncResult.exception != null) {
            this.mState = MmiCode.State.FAILED;
            sb.append(getErrorMessage(asyncResult));
        } else {
            int[] iArr = (int[]) asyncResult.result;
            if (iArr.length != 0) {
                if (iArr[0] == 0) {
                    sb.append(this.mContext.getText(R.string.lockscreen_too_many_failed_password_attempts_dialog_message));
                } else if (this.mSc.equals(SC_WAIT)) {
                    sb.append(createQueryCallWaitingResultMessage(iArr[1]));
                } else if (isServiceCodeCallBarring(this.mSc)) {
                    sb.append(createQueryCallBarringResultMessage(iArr[0]));
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
        Rlog.d(LOG_TAG, "onQueryComplete: mmi=" + this);
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

    protected CharSequence createQueryCallBarringResultMessage(int i) {
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

    @Override
    public ResultReceiver getUssdCallbackReceiver() {
        return this.mCallbackReceiver;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("GsmMmiCode {");
        sb.append("State=" + getState());
        if (this.mAction != null) {
            sb.append(" action=" + this.mAction);
        }
        if (this.mSc != null) {
            sb.append(" sc=" + this.mSc);
        }
        if (this.mSia != null) {
            sb.append(" sia=" + Rlog.pii(LOG_TAG, this.mSia));
        }
        if (this.mSib != null) {
            sb.append(" sib=" + Rlog.pii(LOG_TAG, this.mSib));
        }
        if (this.mSic != null) {
            sb.append(" sic=" + Rlog.pii(LOG_TAG, this.mSic));
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
