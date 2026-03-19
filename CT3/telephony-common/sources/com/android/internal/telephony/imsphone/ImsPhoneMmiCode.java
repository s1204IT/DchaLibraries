package com.android.internal.telephony.imsphone;

import android.R;
import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import com.android.ims.ImsException;
import com.android.ims.ImsSsInfo;
import com.android.internal.telephony.CallFailCause;
import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.uicc.IccRecords;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ImsPhoneMmiCode extends Handler implements MmiCode {
    private static final String ACTION_ACTIVATE = "*";
    private static final String ACTION_DEACTIVATE = "#";
    private static final String ACTION_ERASURE = "##";
    private static final String ACTION_INTERROGATE = "*#";
    private static final String ACTION_REGISTER = "**";
    private static final int CLIR_DEFAULT = 0;
    private static final int CLIR_INVOCATION = 1;
    private static final int CLIR_NOT_PROVISIONED = 0;
    private static final int CLIR_PRESENTATION_ALLOWED_TEMPORARY = 4;
    private static final int CLIR_PRESENTATION_RESTRICTED_TEMPORARY = 3;
    private static final int CLIR_PROVISIONED_PERMANENT = 1;
    private static final int CLIR_SUPPRESSION = 2;
    private static final char END_OF_USSD_COMMAND = '#';
    private static final int EVENT_GET_CLIR_COMPLETE = 6;
    private static final int EVENT_QUERY_CF_COMPLETE = 1;
    private static final int EVENT_QUERY_COMPLETE = 3;
    private static final int EVENT_QUERY_ICB_COMPLETE = 10;
    private static final int EVENT_SET_CFF_COMPLETE = 4;
    private static final int EVENT_SET_CLIR_COMPLETE = 8;
    private static final int EVENT_SET_COMPLETE = 0;
    private static final int EVENT_SUPP_SVC_QUERY_COMPLETE = 7;
    private static final int EVENT_USSD_CANCEL_COMPLETE = 5;
    private static final int EVENT_USSD_COMPLETE = 2;
    static final String IcbAnonymousMmi = "Anonymous Incoming Call Barring";
    static final String IcbDnMmi = "Specific Incoming Call Barring";
    static final String LOG_TAG = "ImsPhoneMmiCode";
    private static final int MATCH_GROUP_ACTION = 2;
    private static final int MATCH_GROUP_DIALING_NUMBER = 12;
    private static final int MATCH_GROUP_POUND_STRING = 1;
    private static final int MATCH_GROUP_PWD_CONFIRM = 11;
    private static final int MATCH_GROUP_SERVICE_CODE = 3;
    private static final int MATCH_GROUP_SIA = 5;
    private static final int MATCH_GROUP_SIB = 7;
    private static final int MATCH_GROUP_SIC = 9;
    private static final int MAX_LENGTH_SHORT_CODE = 2;
    private static final int NUM_PRESENTATION_ALLOWED = 0;
    private static final int NUM_PRESENTATION_RESTRICTED = 1;
    private static final String SC_BAIC = "35";
    private static final String SC_BAICa = "157";
    private static final String SC_BAICr = "351";
    private static final String SC_BAOC = "33";
    private static final String SC_BAOIC = "331";
    private static final String SC_BAOICxH = "332";
    private static final String SC_BA_ALL = "330";
    private static final String SC_BA_MO = "333";
    private static final String SC_BA_MT = "353";
    private static final String SC_BS_MT = "156";
    private static final String SC_CFB = "67";
    private static final String SC_CFNR = "62";
    private static final String SC_CFNRy = "61";
    private static final String SC_CFU = "21";
    private static final String SC_CFUT = "22";
    private static final String SC_CF_All = "002";
    private static final String SC_CF_All_Conditional = "004";
    private static final String SC_CLIP = "30";
    private static final String SC_CLIR = "31";
    private static final String SC_CNAP = "300";
    private static final String SC_COLP = "76";
    private static final String SC_COLR = "77";
    private static final String SC_PIN = "04";
    private static final String SC_PIN2 = "042";
    private static final String SC_PUK = "05";
    private static final String SC_PUK2 = "052";
    private static final String SC_PWD = "03";
    private static final String SC_WAIT = "43";
    public static final String UT_BUNDLE_KEY_CLIR = "queryClir";
    public static final String UT_BUNDLE_KEY_SSINFO = "imsSsInfo";
    private static Pattern sPatternSuppService = Pattern.compile("((\\*|#|\\*#|\\*\\*|##)(\\d{2,3})(\\*([^*#]*)(\\*([^*#]*)(\\*([^*#]*)(\\*([^*#]*))?)?)?)?#)(.*)");
    private static String[] sTwoDigitNumberPattern;
    private String mAction;
    private Context mContext;
    private String mDialingNumber;
    private IccRecords mIccRecords;
    private boolean mIsCallFwdReg;
    private boolean mIsPendingUSSD;
    private boolean mIsUssdRequest;
    private CharSequence mMessage;
    private ImsPhone mPhone;
    private String mPoundString;
    private String mPwd;
    private String mSc;
    private String mSia;
    private String mSib;
    private String mSic;
    private MmiCode.State mState;

    static ImsPhoneMmiCode newFromDialString(String dialString, ImsPhone phone) {
        Matcher m = sPatternSuppService.matcher(dialString);
        if (m.matches()) {
            ImsPhoneMmiCode ret = new ImsPhoneMmiCode(phone);
            ret.mPoundString = makeEmptyNull(m.group(1));
            ret.mAction = makeEmptyNull(m.group(2));
            ret.mSc = makeEmptyNull(m.group(3));
            ret.mSia = makeEmptyNull(m.group(5));
            ret.mSib = makeEmptyNull(m.group(7));
            ret.mSic = makeEmptyNull(m.group(9));
            ret.mPwd = makeEmptyNull(m.group(11));
            ret.mDialingNumber = makeEmptyNull(m.group(12));
            if (ret.mDialingNumber != null && ret.mDialingNumber.endsWith(ACTION_DEACTIVATE) && dialString.endsWith(ACTION_DEACTIVATE)) {
                ImsPhoneMmiCode ret2 = new ImsPhoneMmiCode(phone);
                ret2.mPoundString = dialString;
                return ret2;
            }
            return ret;
        }
        if (dialString.endsWith(ACTION_DEACTIVATE)) {
            ImsPhoneMmiCode ret3 = new ImsPhoneMmiCode(phone);
            ret3.mPoundString = dialString;
            return ret3;
        }
        if (isTwoDigitShortCode(phone.getContext(), dialString) || !isShortCode(dialString, phone)) {
            return null;
        }
        ImsPhoneMmiCode ret4 = new ImsPhoneMmiCode(phone);
        ret4.mDialingNumber = dialString;
        return ret4;
    }

    static ImsPhoneMmiCode newNetworkInitiatedUssd(String ussdMessage, boolean isUssdRequest, ImsPhone phone) {
        ImsPhoneMmiCode ret = new ImsPhoneMmiCode(phone);
        ret.mMessage = ussdMessage;
        ret.mIsUssdRequest = isUssdRequest;
        if (isUssdRequest) {
            ret.mIsPendingUSSD = true;
            ret.mState = MmiCode.State.PENDING;
        } else {
            ret.mState = MmiCode.State.COMPLETE;
        }
        return ret;
    }

    static ImsPhoneMmiCode newFromUssdUserInput(String ussdMessge, ImsPhone phone) {
        ImsPhoneMmiCode ret = new ImsPhoneMmiCode(phone);
        ret.mMessage = ussdMessge;
        ret.mState = MmiCode.State.PENDING;
        ret.mIsPendingUSSD = true;
        return ret;
    }

    private static String makeEmptyNull(String s) {
        if (s == null || s.length() != 0) {
            return s;
        }
        return null;
    }

    static boolean isScMatchesSuppServType(String dialString) {
        Matcher m = sPatternSuppService.matcher(dialString);
        if (!m.matches()) {
            return false;
        }
        String sc = makeEmptyNull(m.group(3));
        if (!sc.equals(SC_CFUT) && !sc.equals(SC_BS_MT)) {
            return false;
        }
        return true;
    }

    private static boolean isEmptyOrNull(CharSequence s) {
        return s == null || s.length() == 0;
    }

    private static int scToCallForwardReason(String sc) {
        if (sc == null) {
            throw new RuntimeException("invalid call forward sc");
        }
        if (sc.equals(SC_CF_All)) {
            return 4;
        }
        if (sc.equals(SC_CFU)) {
            return 0;
        }
        if (sc.equals(SC_CFB)) {
            return 1;
        }
        if (sc.equals(SC_CFNR)) {
            return 3;
        }
        if (sc.equals(SC_CFNRy)) {
            return 2;
        }
        if (sc.equals(SC_CF_All_Conditional)) {
            return 5;
        }
        throw new RuntimeException("invalid call forward sc");
    }

    private static int siToServiceClass(String si) {
        if (si == null || si.length() == 0) {
            return 0;
        }
        int serviceCode = Integer.parseInt(si, 10);
        switch (serviceCode) {
            case 10:
                return 13;
            case 11:
                return 1;
            case 12:
                return 12;
            case 13:
                return 4;
            case 16:
                return 8;
            case 19:
                return 5;
            case 20:
                return 48;
            case 21:
                return 160;
            case 22:
                return 80;
            case 24:
                return 16;
            case 25:
                return 32;
            case 26:
                return 17;
            case CallFailCause.IE_NON_EXISTENT_OR_NOT_IMPLEMENTED:
                return 64;
            default:
                throw new RuntimeException("unsupported MMI service code " + si);
        }
    }

    private static int siToTime(String si) {
        if (si == null || si.length() == 0) {
            return 0;
        }
        return Integer.parseInt(si, 10);
    }

    static boolean isServiceCodeCallForwarding(String sc) {
        if (sc == null) {
            return false;
        }
        if (sc.equals(SC_CFU) || sc.equals(SC_CFB) || sc.equals(SC_CFNRy) || sc.equals(SC_CFNR) || sc.equals(SC_CF_All)) {
            return true;
        }
        return sc.equals(SC_CF_All_Conditional);
    }

    static boolean isServiceCodeCallBarring(String sc) {
        String[] barringMMI;
        Resources resource = Resources.getSystem();
        if (sc != null && (barringMMI = resource.getStringArray(R.array.config_defaultCloudSearchServices)) != null) {
            for (String match : barringMMI) {
                if (sc.equals(match)) {
                    return true;
                }
            }
        }
        return false;
    }

    static String scToBarringFacility(String sc) {
        if (sc == null) {
            throw new RuntimeException("invalid call barring sc");
        }
        if (sc.equals(SC_BAOC)) {
            return CommandsInterface.CB_FACILITY_BAOC;
        }
        if (sc.equals(SC_BAOIC)) {
            return CommandsInterface.CB_FACILITY_BAOIC;
        }
        if (sc.equals(SC_BAOICxH)) {
            return CommandsInterface.CB_FACILITY_BAOICxH;
        }
        if (sc.equals(SC_BAIC)) {
            return CommandsInterface.CB_FACILITY_BAIC;
        }
        if (sc.equals(SC_BAICr)) {
            return CommandsInterface.CB_FACILITY_BAICr;
        }
        if (sc.equals(SC_BA_ALL)) {
            return CommandsInterface.CB_FACILITY_BA_ALL;
        }
        if (sc.equals(SC_BA_MO)) {
            return CommandsInterface.CB_FACILITY_BA_MO;
        }
        if (sc.equals(SC_BA_MT)) {
            return CommandsInterface.CB_FACILITY_BA_MT;
        }
        throw new RuntimeException("invalid call barring sc");
    }

    ImsPhoneMmiCode(ImsPhone phone) {
        super(phone.getHandler().getLooper());
        this.mState = MmiCode.State.PENDING;
        this.mPhone = phone;
        this.mContext = phone.getContext();
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

    String getDialingNumber() {
        return this.mDialingNumber;
    }

    boolean isMMI() {
        return this.mPoundString != null;
    }

    boolean isShortCode() {
        return this.mPoundString == null && this.mDialingNumber != null && this.mDialingNumber.length() <= 2;
    }

    private static boolean isTwoDigitShortCode(Context context, String dialString) {
        Rlog.d(LOG_TAG, "isTwoDigitShortCode");
        if (dialString == null || dialString.length() > 2) {
            return false;
        }
        if (sTwoDigitNumberPattern == null) {
            sTwoDigitNumberPattern = context.getResources().getStringArray(R.array.config_cameraPrivacyLightColors);
        }
        for (String dialnumber : sTwoDigitNumberPattern) {
            Rlog.d(LOG_TAG, "Two Digit Number Pattern " + dialnumber);
            if (dialString.equals(dialnumber)) {
                Rlog.d(LOG_TAG, "Two Digit Number Pattern -true");
                return true;
            }
        }
        Rlog.d(LOG_TAG, "Two Digit Number Pattern -false");
        return false;
    }

    private static boolean isShortCode(String dialString, ImsPhone phone) {
        if (dialString == null || dialString.length() == 0 || PhoneNumberUtils.isLocalEmergencyNumber(phone.getContext(), dialString)) {
            return false;
        }
        return isShortCodeUSSD(dialString, phone);
    }

    private static boolean isShortCodeUSSD(String dialString, ImsPhone phone) {
        return (dialString == null || dialString.length() > 2 || (!phone.isInCall() && dialString.length() == 2 && dialString.charAt(0) == '1')) ? false : true;
    }

    @Override
    public boolean isPinPukCommand() {
        if (this.mSc == null) {
            return false;
        }
        if (this.mSc.equals(SC_PIN) || this.mSc.equals(SC_PIN2) || this.mSc.equals(SC_PUK)) {
            return true;
        }
        return this.mSc.equals(SC_PUK2);
    }

    boolean isTemporaryModeCLIR() {
        if (this.mSc == null || !this.mSc.equals(SC_CLIR) || this.mDialingNumber == null) {
            return false;
        }
        if (isActivate()) {
            return true;
        }
        return isDeactivate();
    }

    int getCLIRMode() {
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

    boolean isActivate() {
        if (this.mAction != null) {
            return this.mAction.equals("*");
        }
        return false;
    }

    boolean isDeactivate() {
        if (this.mAction != null) {
            return this.mAction.equals(ACTION_DEACTIVATE);
        }
        return false;
    }

    boolean isInterrogate() {
        if (this.mAction != null) {
            return this.mAction.equals(ACTION_INTERROGATE);
        }
        return false;
    }

    boolean isRegister() {
        if (this.mAction != null) {
            return this.mAction.equals(ACTION_REGISTER);
        }
        return false;
    }

    boolean isErasure() {
        if (this.mAction != null) {
            return this.mAction.equals(ACTION_ERASURE);
        }
        return false;
    }

    public boolean isPendingUSSD() {
        return this.mIsPendingUSSD;
    }

    @Override
    public boolean isUssdRequest() {
        return this.mIsUssdRequest;
    }

    boolean isSupportedOverImsPhone() {
        if (isShortCode()) {
            return true;
        }
        if (this.mDialingNumber != null) {
            return false;
        }
        if (!isServiceCodeCallForwarding(this.mSc) && !isServiceCodeCallBarring(this.mSc) && ((this.mSc == null || !this.mSc.equals(SC_WAIT)) && ((this.mSc == null || !this.mSc.equals(SC_CLIR)) && ((this.mSc == null || !this.mSc.equals(SC_CLIP)) && ((this.mSc == null || !this.mSc.equals(SC_COLR)) && ((this.mSc == null || !this.mSc.equals(SC_COLP)) && ((this.mSc == null || !this.mSc.equals(SC_BS_MT)) && (this.mSc == null || !this.mSc.equals(SC_BAICa))))))))) {
            return !isPinPukCommand() && (this.mSc == null || !(this.mSc.equals(SC_PWD) || this.mSc.equals(SC_CLIP) || this.mSc.equals(SC_CLIR))) && this.mPoundString != null;
        }
        if (this.mPhone.isVolteEnabled() || (this.mPhone.isWifiCallingEnabled() && ((GsmCdmaPhone) this.mPhone.mDefaultPhone).isWFCUtSupport())) {
            try {
                int serviceClass = siToServiceClass(this.mSib);
                if ((serviceClass & 1) != 0 || (serviceClass & 512) != 0 || serviceClass == 0) {
                    Rlog.d(LOG_TAG, "isSupportedOverImsPhone(), return true!");
                    return true;
                }
            } catch (RuntimeException exc) {
                Rlog.d(LOG_TAG, "exc.toString() = " + exc.toString());
            }
        }
        return false;
    }

    public int callBarAction(String dialingNumber) {
        if (isActivate()) {
            return 1;
        }
        if (isDeactivate()) {
            return 0;
        }
        if (isRegister()) {
            if (!isEmptyOrNull(dialingNumber)) {
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
        int cfAction;
        try {
        } catch (RuntimeException e) {
            this.mState = MmiCode.State.FAILED;
            this.mMessage = this.mContext.getText(R.string.PERSOSUBSTATE_SIM_ICCID_IN_PROGRESS);
            this.mPhone.onMMIDone(this);
        }
        if (this.mPhone.mDefaultPhone.getCsFallbackStatus() != 0) {
            Rlog.d(LOG_TAG, "processCode(): getCsFallbackStatus(): CS Fallback!");
            this.mPhone.removeMmi(this);
            throw new CallStateException(Phone.CS_FALLBACK);
        }
        if (isShortCode()) {
            Rlog.d(LOG_TAG, "isShortCode");
            if (SystemProperties.get("persist.mtk_ussi_support").equals("1")) {
                Rlog.d(LOG_TAG, "Sending short code '" + this.mDialingNumber + "' over IMS.");
                sendUssd(this.mDialingNumber);
                return;
            } else {
                Rlog.d(LOG_TAG, "Sending short code '" + this.mDialingNumber + "' over CS pipe.");
                this.mPhone.removeMmi(this);
                throw new CallStateException(Phone.CS_FALLBACK);
            }
        }
        if (isServiceCodeCallForwarding(this.mSc)) {
            Rlog.d(LOG_TAG, "is CF");
            String dialingNumber = this.mSia;
            int reason = scToCallForwardReason(this.mSc);
            int serviceClass = siToServiceClass(this.mSib);
            int time = siToTime(this.mSic);
            if (isInterrogate()) {
                this.mPhone.getCallForwardingOption(reason, obtainMessage(1, this));
                return;
            }
            if (isActivate()) {
                if (isEmptyOrNull(dialingNumber)) {
                    cfAction = 1;
                    this.mIsCallFwdReg = false;
                } else {
                    cfAction = 3;
                    this.mIsCallFwdReg = true;
                }
            } else if (isDeactivate()) {
                cfAction = 0;
            } else if (isRegister()) {
                cfAction = 3;
            } else if (isErasure()) {
                cfAction = 4;
            } else {
                throw new RuntimeException("invalid action");
            }
            int isSettingUnconditional = (reason == 0 || reason == 4) ? 1 : 0;
            int isEnableDesired = (cfAction == 1 || cfAction == 3) ? 1 : 0;
            Rlog.d(LOG_TAG, "is CF setCallForward");
            if (((GsmCdmaPhone) this.mPhone.mDefaultPhone).isOpReregisterForCF()) {
                Rlog.i(LOG_TAG, "Set ims dereg to ON.");
                SystemProperties.set(GsmCdmaPhone.IMS_DEREG_PROP, "1");
            }
            this.mPhone.setCallForwardingOption(cfAction, reason, dialingNumber, serviceClass, time, obtainMessage(4, isSettingUnconditional, isEnableDesired, this));
            return;
        }
        if (isServiceCodeCallBarring(this.mSc)) {
            String password = this.mSia;
            String facility = scToBarringFacility(this.mSc);
            if (isInterrogate()) {
                this.mPhone.getCallBarring(facility, obtainMessage(7, this));
                return;
            } else {
                if (isActivate() || isDeactivate()) {
                    this.mPhone.setCallBarring(facility, isActivate(), password, obtainMessage(0, this));
                    return;
                }
                throw new RuntimeException("Invalid or Unsupported MMI Code");
            }
        }
        if (this.mSc != null && this.mSc.equals(SC_CLIR)) {
            if (isActivate()) {
                if (((GsmCdmaPhone) this.mPhone.mDefaultPhone).isOpTbClir()) {
                    ((GsmCdmaPhone) this.mPhone.mDefaultPhone).mCi.setCLIR(1, obtainMessage(8, 1, 0, this));
                    return;
                }
                try {
                    this.mPhone.mCT.getUtInterface().updateCLIR(1, obtainMessage(8, 1, 0, this));
                    return;
                } catch (ImsException e2) {
                    Rlog.d(LOG_TAG, "Could not get UT handle for updateCLIR.");
                    return;
                }
            }
            if (isDeactivate()) {
                if (((GsmCdmaPhone) this.mPhone.mDefaultPhone).isOpTbClir()) {
                    ((GsmCdmaPhone) this.mPhone.mDefaultPhone).mCi.setCLIR(2, obtainMessage(8, 2, 0, this));
                    return;
                }
                try {
                    this.mPhone.mCT.getUtInterface().updateCLIR(2, obtainMessage(8, 2, 0, this));
                    return;
                } catch (ImsException e3) {
                    Rlog.d(LOG_TAG, "Could not get UT handle for updateCLIR.");
                    return;
                }
            }
            if (isInterrogate()) {
                if (((GsmCdmaPhone) this.mPhone.mDefaultPhone).isOpTbClir()) {
                    Message msg = obtainMessage(6, this);
                    if (msg != null) {
                        int[] result = ((GsmCdmaPhone) this.mPhone.mDefaultPhone).getSavedClirSetting();
                        Bundle info = new Bundle();
                        info.putIntArray("queryClir", result);
                        AsyncResult.forMessage(msg, info, (Throwable) null);
                        msg.sendToTarget();
                        return;
                    }
                    return;
                }
                try {
                    this.mPhone.mCT.getUtInterface().queryCLIR(obtainMessage(6, this));
                    return;
                } catch (ImsException e4) {
                    Rlog.d(LOG_TAG, "Could not get UT handle for queryCLIR.");
                    return;
                }
            }
            throw new RuntimeException("Invalid or Unsupported MMI Code");
        }
        if (this.mSc != null && this.mSc.equals(SC_CLIP)) {
            if (isInterrogate()) {
                try {
                    this.mPhone.mCT.getUtInterface().queryCLIP(obtainMessage(7, this));
                    return;
                } catch (ImsException e5) {
                    Rlog.d(LOG_TAG, "Could not get UT handle for queryCLIP.");
                    return;
                }
            } else {
                if (isActivate() || isDeactivate()) {
                    try {
                        this.mPhone.mCT.getUtInterface().updateCLIP(isActivate(), obtainMessage(0, this));
                        return;
                    } catch (ImsException e6) {
                        Rlog.d(LOG_TAG, "Could not get UT handle for updateCLIP.");
                        return;
                    }
                }
                throw new RuntimeException("Invalid or Unsupported MMI Code");
            }
        }
        if (this.mSc != null && this.mSc.equals(SC_COLP)) {
            if (isInterrogate()) {
                try {
                    this.mPhone.mCT.getUtInterface().queryCOLP(obtainMessage(7, this));
                    return;
                } catch (ImsException e7) {
                    Rlog.d(LOG_TAG, "Could not get UT handle for queryCOLP.");
                    return;
                }
            } else {
                if (isActivate() || isDeactivate()) {
                    try {
                        this.mPhone.mCT.getUtInterface().updateCOLP(isActivate(), obtainMessage(0, this));
                        return;
                    } catch (ImsException e8) {
                        Rlog.d(LOG_TAG, "Could not get UT handle for updateCOLP.");
                        return;
                    }
                }
                throw new RuntimeException("Invalid or Unsupported MMI Code");
            }
        }
        if (this.mSc != null && this.mSc.equals(SC_COLR)) {
            if (isActivate()) {
                try {
                    this.mPhone.mCT.getUtInterface().updateCOLR(1, obtainMessage(0, this));
                    return;
                } catch (ImsException e9) {
                    Rlog.d(LOG_TAG, "Could not get UT handle for updateCOLR.");
                    return;
                }
            } else if (isDeactivate()) {
                try {
                    this.mPhone.mCT.getUtInterface().updateCOLR(0, obtainMessage(0, this));
                    return;
                } catch (ImsException e10) {
                    Rlog.d(LOG_TAG, "Could not get UT handle for updateCOLR.");
                    return;
                }
            } else {
                if (isInterrogate()) {
                    try {
                        this.mPhone.mCT.getUtInterface().queryCOLR(obtainMessage(7, this));
                        return;
                    } catch (ImsException e11) {
                        Rlog.d(LOG_TAG, "Could not get UT handle for queryCOLR.");
                        return;
                    }
                }
                throw new RuntimeException("Invalid or Unsupported MMI Code");
            }
        }
        if (this.mSc != null && this.mSc.equals(SC_BS_MT)) {
            try {
                if (isInterrogate()) {
                    this.mPhone.mCT.getUtInterface().queryCallBarring(10, obtainMessage(10, this));
                } else {
                    processIcbMmiCodeForUpdate();
                }
                return;
            } catch (ImsException e12) {
                Rlog.d(LOG_TAG, "Could not get UT handle for ICB.");
                return;
            }
        }
        if (this.mSc != null && this.mSc.equals(SC_BAICa)) {
            int callAction = 0;
            try {
                if (isInterrogate()) {
                    this.mPhone.mCT.getUtInterface().queryCallBarring(6, obtainMessage(10, this));
                    return;
                }
                if (isActivate()) {
                    callAction = 1;
                } else if (isDeactivate()) {
                    callAction = 0;
                }
                this.mPhone.mCT.getUtInterface().updateCallBarring(6, callAction, obtainMessage(0, this), (String[]) null);
                return;
            } catch (ImsException e13) {
                Rlog.d(LOG_TAG, "Could not get UT handle for ICBa.");
                return;
            }
        }
        if (this.mSc != null && this.mSc.equals(SC_WAIT)) {
            int serviceClass2 = siToServiceClass(this.mSib);
            if (isActivate() || isDeactivate()) {
                if (((GsmCdmaPhone) this.mPhone.mDefaultPhone).isOpNwCW()) {
                    Rlog.d(LOG_TAG, "setCallWaiting() by Ut interface.");
                    this.mPhone.setCallWaiting(isActivate(), serviceClass2, obtainMessage(0, this));
                    return;
                }
                String tbcwMode = this.mPhone.mDefaultPhone.getSystemProperty("persist.radio.terminal-based.cw", "disabled_tbcw");
                Rlog.d(LOG_TAG, "setCallWaiting(): tbcwMode = " + tbcwMode + ", enable = " + isActivate());
                if ("enabled_tbcw_on".equals(tbcwMode)) {
                    if (!isActivate()) {
                        this.mPhone.mDefaultPhone.setSystemProperty("persist.radio.terminal-based.cw", "enabled_tbcw_off");
                    }
                    Message msg2 = obtainMessage(0, null);
                    AsyncResult.forMessage(msg2, (Object) null, (Throwable) null);
                    sendMessage(msg2);
                    return;
                }
                if ("enabled_tbcw_off".equals(tbcwMode)) {
                    if (isActivate()) {
                        this.mPhone.mDefaultPhone.setSystemProperty("persist.radio.terminal-based.cw", "enabled_tbcw_on");
                    }
                    Message msg3 = obtainMessage(0, null);
                    AsyncResult.forMessage(msg3, (Object) null, (Throwable) null);
                    sendMessage(msg3);
                    return;
                }
                Rlog.d(LOG_TAG, "setCallWaiting() by Ut interface.");
                this.mPhone.setCallWaiting(isActivate(), serviceClass2, obtainMessage(0, this));
                return;
            }
            if (isInterrogate()) {
                if (((GsmCdmaPhone) this.mPhone.mDefaultPhone).isOpNwCW()) {
                    Rlog.d(LOG_TAG, "getCallWaiting() by Ut interface.");
                    this.mPhone.getCallWaiting(obtainMessage(3, this));
                    return;
                }
                String tbcwMode2 = this.mPhone.mDefaultPhone.getSystemProperty("persist.radio.terminal-based.cw", "disabled_tbcw");
                Rlog.d(LOG_TAG, "SC_WAIT isInterrogate() tbcwMode = " + tbcwMode2);
                if ("enabled_tbcw_on".equals(tbcwMode2)) {
                    int[] cwInfos = {1, 1};
                    Message msg4 = obtainMessage(3, null);
                    AsyncResult.forMessage(msg4, cwInfos, (Throwable) null);
                    sendMessage(msg4);
                    return;
                }
                if ("enabled_tbcw_off".equals(tbcwMode2)) {
                    int[] cwInfos2 = {0, 0};
                    Message msg5 = obtainMessage(3, null);
                    AsyncResult.forMessage(msg5, cwInfos2, (Throwable) null);
                    sendMessage(msg5);
                    return;
                }
                Rlog.d(LOG_TAG, "getCallWaiting() by Ut interface.");
                this.mPhone.getCallWaiting(obtainMessage(3, this));
                return;
            }
            throw new RuntimeException("Invalid or Unsupported MMI Code");
        }
        if (this.mPoundString != null) {
            if (SystemProperties.get("persist.mtk_ussi_support").equals("1")) {
                Rlog.d(LOG_TAG, "Sending pound string '" + this.mPoundString + "' over IMS.");
                sendUssd(this.mPoundString);
                return;
            } else {
                Rlog.d(LOG_TAG, "Sending pound string '" + this.mDialingNumber + "' over CS pipe.");
                this.mPhone.removeMmi(this);
                throw new CallStateException(Phone.CS_FALLBACK);
            }
        }
        throw new RuntimeException("Invalid or Unsupported MMI Code");
        this.mState = MmiCode.State.FAILED;
        this.mMessage = this.mContext.getText(R.string.PERSOSUBSTATE_SIM_ICCID_IN_PROGRESS);
        this.mPhone.onMMIDone(this);
    }

    void onUssdFinished(String ussdMessage, boolean isUssdRequest) {
        if (this.mState != MmiCode.State.PENDING) {
            return;
        }
        if (ussdMessage == null) {
            this.mMessage = this.mContext.getText(R.string.PERSOSUBSTATE_SIM_NETWORK_IN_PROGRESS);
        } else {
            this.mMessage = ussdMessage;
        }
        this.mIsUssdRequest = isUssdRequest;
        if (!isUssdRequest) {
            this.mState = MmiCode.State.COMPLETE;
        }
        this.mPhone.onMMIDone(this);
    }

    void onUssdFinishedError() {
        if (this.mState != MmiCode.State.PENDING) {
            return;
        }
        this.mState = MmiCode.State.FAILED;
        this.mMessage = this.mContext.getText(R.string.PERSOSUBSTATE_SIM_ICCID_IN_PROGRESS);
        this.mPhone.onMMIDone(this);
    }

    void sendUssd(String ussdMessage) {
        this.mIsPendingUSSD = true;
        this.mPhone.sendUSSD(ussdMessage, obtainMessage(2, this));
    }

    @Override
    public void handleMessage(Message msg) {
        AsyncResult ar = (AsyncResult) msg.obj;
        if (ar != null && ar.exception != null) {
            if (ar.exception instanceof CommandException) {
                CommandException cmdException = (CommandException) ar.exception;
                if (cmdException.getCommandError() == CommandException.Error.UT_XCAP_403_FORBIDDEN) {
                    Rlog.d(LOG_TAG, "handleMessage(): CommandException.Error.UT_XCAP_403_FORBIDDEN");
                    this.mPhone.handleMmiCodeCsfb(830, this);
                } else if (cmdException.getCommandError() == CommandException.Error.UT_UNKNOWN_HOST) {
                    Rlog.d(LOG_TAG, "handleMessage(): CommandException.Error.UT_UNKNOWN_HOST");
                    this.mPhone.handleMmiCodeCsfb(831, this);
                    return;
                }
            } else if (ar.exception instanceof ImsException) {
                ImsException imsException = ar.exception;
                if (imsException.getCode() == 830) {
                    Rlog.d(LOG_TAG, "handleMessage(): ImsReasonInfo.CODE_UT_XCAP_403_FORBIDDEN");
                    this.mPhone.handleMmiCodeCsfb(830, this);
                    return;
                } else if (imsException.getCode() == 831) {
                    Rlog.d(LOG_TAG, "handleMessage(): ImsReasonInfo.CODE_UT_UNKNOWN_HOST");
                    this.mPhone.handleMmiCodeCsfb(831, this);
                    return;
                }
            }
        }
        switch (msg.what) {
            case 0:
                onSetComplete(msg, (AsyncResult) msg.obj);
                break;
            case 1:
                onQueryCfComplete((AsyncResult) msg.obj);
                break;
            case 2:
                AsyncResult ar2 = (AsyncResult) msg.obj;
                if (ar2.exception != null) {
                    this.mPhone.mUssiCSFB = true;
                    this.mState = MmiCode.State.FAILED;
                    this.mMessage = getErrorMessage(ar2);
                    this.mPhone.onMMIDone(this);
                }
                break;
            case 3:
                onQueryComplete((AsyncResult) msg.obj);
                break;
            case 4:
                AsyncResult ar3 = (AsyncResult) msg.obj;
                if (ar3.exception == null && msg.arg1 == 1) {
                    boolean cffEnabled = msg.arg2 == 1;
                    if (((GsmCdmaPhone) this.mPhone.mDefaultPhone).queryCFUAgainAfterSet()) {
                        if (ar3.result != null) {
                            CallForwardInfo[] cfInfos = (CallForwardInfo[]) ar3.result;
                            if (cfInfos == null || cfInfos.length == 0) {
                                Rlog.i(LOG_TAG, "cfInfo is null or length is 0.");
                            } else {
                                int i = 0;
                                while (true) {
                                    if (i < cfInfos.length) {
                                        if ((cfInfos[i].serviceClass & 1) == 0) {
                                            i++;
                                        } else if (cfInfos[i].status == 1) {
                                            Rlog.i(LOG_TAG, "Set CF_ENABLE, serviceClass: " + cfInfos[i].serviceClass);
                                            cffEnabled = true;
                                        } else {
                                            Rlog.i(LOG_TAG, "Set CF_DISABLE, serviceClass: " + cfInfos[i].serviceClass);
                                            cffEnabled = false;
                                        }
                                    }
                                }
                            }
                        } else {
                            Rlog.i(LOG_TAG, "ar.result is null.");
                        }
                    }
                    Rlog.i(LOG_TAG, "EVENT_SET_CFF_COMPLETE: cffEnabled:" + cffEnabled + ", mDialingNumber=" + this.mDialingNumber + ", mIccRecords=" + this.mIccRecords);
                    if (this.mIccRecords != null) {
                        ((GsmCdmaPhone) this.mPhone.mDefaultPhone).setVoiceCallForwardingFlag(1, cffEnabled, this.mDialingNumber);
                        this.mPhone.saveTimeSlot(null);
                    }
                    if (cffEnabled) {
                        TelephonyManager.setTelephonyProperty(this.mPhone.getPhoneId(), "persist.radio.ut.cfu.mode", "enabled_ut_cfu_mode_on");
                    } else {
                        TelephonyManager.setTelephonyProperty(this.mPhone.getPhoneId(), "persist.radio.ut.cfu.mode", "enabled_ut_cfu_mode_off");
                    }
                }
                onSetComplete(msg, ar3);
                break;
            case 5:
                this.mPhone.onMMIDone(this);
                break;
            case 6:
                onQueryClirComplete((AsyncResult) msg.obj);
                break;
            case 7:
                onSuppSvcQueryComplete((AsyncResult) msg.obj);
                break;
            case 8:
                AsyncResult ar4 = (AsyncResult) msg.obj;
                if (ar4.exception == null) {
                    ((GsmCdmaPhone) this.mPhone.mDefaultPhone).saveClirSetting(msg.arg1);
                }
                onSetComplete(msg, ar4);
                break;
            case 10:
                onIcbQueryComplete((AsyncResult) msg.obj);
                break;
        }
    }

    private void processIcbMmiCodeForUpdate() {
        String dialingNumber = this.mSia;
        String[] icbNum = null;
        if (dialingNumber != null) {
            icbNum = dialingNumber.split("\\$");
        }
        int callAction = callBarAction(dialingNumber);
        try {
            this.mPhone.mCT.getUtInterface().updateCallBarring(10, callAction, obtainMessage(0, this), icbNum);
        } catch (ImsException e) {
            Rlog.d(LOG_TAG, "Could not get UT handle for updating ICB.");
        }
    }

    private CharSequence getErrorMessage(AsyncResult ar) {
        return this.mContext.getText(R.string.PERSOSUBSTATE_SIM_ICCID_IN_PROGRESS);
    }

    private CharSequence getScString() {
        if (this.mSc != null) {
            if (isServiceCodeCallBarring(this.mSc)) {
                return this.mContext.getText(R.string.PERSOSUBSTATE_SIM_NS_SP_SUCCESS);
            }
            if (isServiceCodeCallForwarding(this.mSc)) {
                return this.mContext.getText(R.string.PERSOSUBSTATE_SIM_NS_SP_ERROR);
            }
            if (this.mSc.equals(SC_PWD)) {
                return this.mContext.getText(R.string.PERSOSUBSTATE_SIM_SERVICE_PROVIDER_ENTRY);
            }
            if (this.mSc.equals(SC_WAIT)) {
                return this.mContext.getText(R.string.PERSOSUBSTATE_SIM_NS_SP_IN_PROGRESS);
            }
            if (this.mSc.equals(SC_CLIP)) {
                return this.mContext.getText(R.string.PERSOSUBSTATE_SIM_NETWORK_SUBSET_PUK_SUCCESS);
            }
            if (this.mSc.equals(SC_CLIR)) {
                return this.mContext.getText(R.string.PERSOSUBSTATE_SIM_NETWORK_SUBSET_SUCCESS);
            }
            if (this.mSc.equals(SC_COLP)) {
                return this.mContext.getText(R.string.PERSOSUBSTATE_SIM_NETWORK_SUCCESS);
            }
            if (this.mSc.equals(SC_COLR)) {
                return this.mContext.getText(R.string.PERSOSUBSTATE_SIM_NS_SP_ENTRY);
            }
            if (this.mSc.equals(SC_BS_MT)) {
                return IcbDnMmi;
            }
            if (this.mSc.equals(SC_BAICa)) {
                return IcbAnonymousMmi;
            }
            return UsimPBMemInfo.STRING_NOT_SET;
        }
        return UsimPBMemInfo.STRING_NOT_SET;
    }

    private void onSetComplete(Message msg, AsyncResult ar) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        if (ar.exception != null) {
            this.mState = MmiCode.State.FAILED;
            if (ar.exception instanceof CommandException) {
                CommandException err = (CommandException) ar.exception;
                if (err.getCommandError() == CommandException.Error.PASSWORD_INCORRECT) {
                    sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_NETWORK_ERROR));
                } else if (err.getMessage() != null) {
                    sb.append(err.getMessage());
                } else {
                    sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_ICCID_IN_PROGRESS));
                }
            } else {
                ImsException error = ar.exception;
                if (error.getMessage() != null) {
                    sb.append(error.getMessage());
                } else {
                    sb.append(getErrorMessage(ar));
                }
            }
        } else if (isActivate()) {
            this.mState = MmiCode.State.COMPLETE;
            if (this.mIsCallFwdReg) {
                sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_IMPI_SUCCESS));
            } else {
                sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_IMPI_ENTRY));
            }
        } else if (isDeactivate()) {
            this.mState = MmiCode.State.COMPLETE;
            sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_IMPI_IN_PROGRESS));
        } else if (isRegister()) {
            this.mState = MmiCode.State.COMPLETE;
            sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_IMPI_SUCCESS));
        } else if (isErasure()) {
            this.mState = MmiCode.State.COMPLETE;
            sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_NETWORK_ENTRY));
        } else {
            this.mState = MmiCode.State.FAILED;
            sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_ICCID_IN_PROGRESS));
        }
        this.mMessage = sb;
        this.mPhone.onMMIDone(this);
    }

    private CharSequence serviceClassToCFString(int serviceClass) {
        switch (serviceClass) {
            case 1:
                return this.mContext.getText(R.string.RestrictedOnAllVoiceTitle);
            case 2:
                return this.mContext.getText(R.string.RestrictedOnDataTitle);
            case 4:
                return this.mContext.getText(R.string.RestrictedOnEmergencyTitle);
            case 8:
                return this.mContext.getText(R.string.RestrictedOnNormalTitle);
            case 16:
                return this.mContext.getText(R.string.RestrictedStateContentMsimTemplate);
            case 32:
                return this.mContext.getText(R.string.RestrictedStateContent);
            case 64:
                return this.mContext.getText(R.string.RuacMmi);
            case 128:
                return this.mContext.getText(R.string.SetupCallDefault);
            default:
                return null;
        }
    }

    private CharSequence makeCFQueryResultMessage(CallForwardInfo info, int serviceClassMask) {
        CharSequence template;
        String[] sources = {"{0}", "{1}", "{2}"};
        CharSequence[] destinations = new CharSequence[3];
        boolean needTimeTemplate = info.reason == 2;
        if (info.status == 1) {
            if (needTimeTemplate) {
                template = this.mContext.getText(R.string.accessibility_dialog_button_uninstall);
            } else {
                template = this.mContext.getText(R.string.accessibility_dialog_button_deny);
            }
        } else if (info.status == 0 && isEmptyOrNull(info.number)) {
            template = this.mContext.getText(R.string.accessibility_dialog_button_allow);
        } else if (needTimeTemplate) {
            template = this.mContext.getText(R.string.accessibility_edit_shortcut_menu_button_title);
        } else {
            template = this.mContext.getText(R.string.accessibility_dialog_touch_filtered_warning);
        }
        destinations[0] = serviceClassToCFString(info.serviceClass & serviceClassMask);
        destinations[1] = PhoneNumberUtils.stringFromStringAndTOA(info.number, info.toa);
        destinations[2] = Integer.toString(info.timeSeconds);
        if (info.reason == 0 && (info.serviceClass & serviceClassMask) == 1) {
            boolean cffEnabled = info.status == 1;
            if (this.mIccRecords != null) {
                this.mPhone.setVoiceCallForwardingFlag(1, cffEnabled, info.number);
            }
        }
        return TextUtils.replace(template, sources, destinations);
    }

    private void onQueryCfComplete(AsyncResult ar) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        if (ar.exception != null) {
            this.mState = MmiCode.State.FAILED;
            if (ar.exception instanceof ImsException) {
                ImsException error = ar.exception;
                if (error.getMessage() != null) {
                    sb.append(error.getMessage());
                } else {
                    sb.append(getErrorMessage(ar));
                }
            } else {
                sb.append(getErrorMessage(ar));
            }
        } else {
            CallForwardInfo[] infos = (CallForwardInfo[]) ar.result;
            if (infos.length == 0) {
                sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_IMPI_IN_PROGRESS));
                if (this.mIccRecords != null) {
                    this.mPhone.setVoiceCallForwardingFlag(1, false, null);
                }
            } else {
                SpannableStringBuilder tb = new SpannableStringBuilder();
                for (int serviceClassMask = 1; serviceClassMask <= 512; serviceClassMask <<= 1) {
                    int s = infos.length;
                    for (int i = 0; i < s; i++) {
                        if ((infos[i].serviceClass & serviceClassMask) != 0) {
                            tb.append(makeCFQueryResultMessage(infos[i], serviceClassMask));
                            tb.append((CharSequence) "\n");
                        }
                    }
                }
                sb.append((CharSequence) tb);
            }
            this.mState = MmiCode.State.COMPLETE;
        }
        this.mMessage = sb;
        this.mPhone.onMMIDone(this);
    }

    private void onSuppSvcQueryComplete(AsyncResult ar) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        if (ar.exception != null) {
            this.mState = MmiCode.State.FAILED;
            if (ar.exception instanceof ImsException) {
                ImsException error = ar.exception;
                if (error.getMessage() != null) {
                    sb.append(error.getMessage());
                } else {
                    sb.append(getErrorMessage(ar));
                }
            } else {
                sb.append(getErrorMessage(ar));
            }
        } else {
            this.mState = MmiCode.State.FAILED;
            if (ar.result instanceof Bundle) {
                Rlog.d(LOG_TAG, "Received CLIP/COLP/COLR Response.");
                Bundle ssInfoResp = (Bundle) ar.result;
                ImsSsInfo ssInfo = ssInfoResp.getParcelable(UT_BUNDLE_KEY_SSINFO);
                if (ssInfo != null) {
                    Rlog.d(LOG_TAG, "ImsSsInfo mStatus = " + ssInfo.mStatus);
                    if (ssInfo.mStatus == 0) {
                        sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_IMPI_IN_PROGRESS));
                        this.mState = MmiCode.State.COMPLETE;
                    } else if (ssInfo.mStatus == 1) {
                        sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_IMPI_ENTRY));
                        this.mState = MmiCode.State.COMPLETE;
                    } else {
                        sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_ICCID_IN_PROGRESS));
                    }
                } else {
                    sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_ICCID_IN_PROGRESS));
                }
            } else {
                Rlog.d(LOG_TAG, "Received Call Barring Response.");
                int[] cbInfos = (int[]) ar.result;
                if (cbInfos[0] == 1) {
                    sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_IMPI_ENTRY));
                    this.mState = MmiCode.State.COMPLETE;
                } else {
                    sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_IMPI_IN_PROGRESS));
                    this.mState = MmiCode.State.COMPLETE;
                }
            }
        }
        this.mMessage = sb;
        this.mPhone.onMMIDone(this);
    }

    private void onIcbQueryComplete(AsyncResult ar) {
        Rlog.d(LOG_TAG, "onIcbQueryComplete ");
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        if (ar.exception != null) {
            this.mState = MmiCode.State.FAILED;
            if (ar.exception instanceof ImsException) {
                ImsException error = ar.exception;
                if (error.getMessage() != null) {
                    sb.append(error.getMessage());
                } else {
                    sb.append(getErrorMessage(ar));
                }
            } else {
                sb.append(getErrorMessage(ar));
            }
        } else {
            ImsSsInfo[] infos = (ImsSsInfo[]) ar.result;
            if (infos.length == 0) {
                sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_IMPI_IN_PROGRESS));
            } else {
                int s = infos.length;
                for (int i = 0; i < s; i++) {
                    if (infos[i].mIcbNum != null) {
                        sb.append("Num: ").append(infos[i].mIcbNum).append(" status: ").append(infos[i].mStatus).append("\n");
                    } else if (infos[i].mStatus == 1) {
                        sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_IMPI_ENTRY));
                    } else {
                        sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_IMPI_IN_PROGRESS));
                    }
                }
            }
            this.mState = MmiCode.State.COMPLETE;
        }
        this.mMessage = sb;
        this.mPhone.onMMIDone(this);
    }

    private void onQueryClirComplete(AsyncResult ar) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        this.mState = MmiCode.State.FAILED;
        if (ar.exception != null) {
            if (ar.exception instanceof ImsException) {
                ImsException error = ar.exception;
                if (error.getMessage() != null) {
                    sb.append(error.getMessage());
                } else {
                    sb.append(getErrorMessage(ar));
                }
            }
        } else {
            Bundle ssInfo = (Bundle) ar.result;
            int[] clirInfo = ssInfo.getIntArray("queryClir");
            Rlog.d(LOG_TAG, "CLIR param n=" + clirInfo[0] + " m=" + clirInfo[1]);
            switch (clirInfo[1]) {
                case 0:
                    sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_SIM_PUK_ERROR));
                    this.mState = MmiCode.State.COMPLETE;
                    break;
                case 1:
                    sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_SIM_PUK_IN_PROGRESS));
                    this.mState = MmiCode.State.COMPLETE;
                    break;
                case 2:
                default:
                    sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_ICCID_IN_PROGRESS));
                    this.mState = MmiCode.State.FAILED;
                    break;
                case 3:
                    switch (clirInfo[0]) {
                        case 0:
                            sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_SIM_ENTRY));
                            this.mState = MmiCode.State.COMPLETE;
                            break;
                        case 1:
                            sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_SIM_ENTRY));
                            this.mState = MmiCode.State.COMPLETE;
                            break;
                        case 2:
                            sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_SIM_ERROR));
                            this.mState = MmiCode.State.COMPLETE;
                            break;
                        default:
                            sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_ICCID_IN_PROGRESS));
                            this.mState = MmiCode.State.FAILED;
                            break;
                    }
                    break;
                case 4:
                    switch (clirInfo[0]) {
                        case 0:
                            sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_SIM_PUK_ENTRY));
                            this.mState = MmiCode.State.COMPLETE;
                            break;
                        case 1:
                            sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_SIM_IN_PROGRESS));
                            this.mState = MmiCode.State.COMPLETE;
                            break;
                        case 2:
                            sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_SIM_PUK_ENTRY));
                            this.mState = MmiCode.State.COMPLETE;
                            break;
                        default:
                            sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_ICCID_IN_PROGRESS));
                            this.mState = MmiCode.State.FAILED;
                            break;
                    }
                    break;
            }
        }
        this.mMessage = sb;
        this.mPhone.onMMIDone(this);
    }

    private void onQueryComplete(AsyncResult ar) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        if (ar.exception != null) {
            this.mState = MmiCode.State.FAILED;
            if (ar.exception instanceof ImsException) {
                ImsException error = ar.exception;
                if (error.getMessage() != null) {
                    sb.append(error.getMessage());
                } else {
                    sb.append(getErrorMessage(ar));
                }
            } else {
                sb.append(getErrorMessage(ar));
            }
        } else {
            int[] ints = (int[]) ar.result;
            if (ints.length != 0) {
                if (ints[0] == 0) {
                    sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_IMPI_IN_PROGRESS));
                } else if (this.mSc.equals(SC_WAIT)) {
                    sb.append(createQueryCallWaitingResultMessage(ints[1]));
                } else if (ints[0] == 1) {
                    sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_IMPI_ENTRY));
                } else {
                    sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_ICCID_IN_PROGRESS));
                }
            } else {
                sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_ICCID_IN_PROGRESS));
            }
            this.mState = MmiCode.State.COMPLETE;
        }
        this.mMessage = sb;
        this.mPhone.onMMIDone(this);
    }

    private CharSequence createQueryCallWaitingResultMessage(int serviceClass) {
        StringBuilder sb = new StringBuilder(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_IMPI_ERROR));
        for (int classMask = 1; classMask <= 512; classMask <<= 1) {
            if ((classMask & serviceClass) != 0) {
                sb.append("\n");
                sb.append(serviceClassToCFString(classMask & serviceClass));
            }
        }
        return sb;
    }

    @Override
    public boolean getUserInitiatedMMI() {
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ImsPhoneMmiCode {");
        sb.append("State=").append(getState());
        if (this.mAction != null) {
            sb.append(" action=").append(this.mAction);
        }
        if (this.mSc != null) {
            sb.append(" sc=").append(this.mSc);
        }
        if (this.mSia != null) {
            sb.append(" sia=").append(this.mSia);
        }
        if (this.mSib != null) {
            sb.append(" sib=").append(this.mSib);
        }
        if (this.mSic != null) {
            sb.append(" sic=").append(this.mSic);
        }
        if (this.mPoundString != null) {
            sb.append(" poundString=").append(this.mPoundString);
        }
        if (this.mDialingNumber != null) {
            sb.append(" dialingNumber=").append(this.mDialingNumber);
        }
        if (this.mPwd != null) {
            sb.append(" pwd=").append(this.mPwd);
        }
        sb.append("}");
        return sb.toString();
    }

    public boolean isUssdNumber() {
        if (isTemporaryModeCLIR()) {
            return false;
        }
        if (isShortCode() || this.mDialingNumber != null) {
            return true;
        }
        return (this.mSc == null || !(this.mSc.equals(SC_CNAP) || this.mSc.equals(SC_CLIP) || this.mSc.equals(SC_CLIR) || this.mSc.equals(SC_COLP) || this.mSc.equals(SC_COLR) || isServiceCodeCallForwarding(this.mSc) || isServiceCodeCallBarring(this.mSc) || this.mSc.equals(SC_PWD) || this.mSc.equals(SC_WAIT) || isPinPukCommand())) && this.mPoundString != null;
    }

    public String getUssdDialString() {
        Rlog.d(LOG_TAG, "getUssdDialString(): mDialingNumber=" + this.mDialingNumber + ", mPoundString=" + this.mPoundString);
        return this.mDialingNumber != null ? this.mDialingNumber : this.mPoundString;
    }

    public static boolean isUtMmiCode(String dialString, ImsPhone dialPhone) {
        ImsPhoneMmiCode mmi = newFromDialString(dialString, dialPhone);
        return (mmi == null || mmi.isTemporaryModeCLIR() || mmi.isShortCode() || mmi.mDialingNumber != null || mmi.mSc == null || (!mmi.mSc.equals(SC_CLIP) && !mmi.mSc.equals(SC_CLIR) && !mmi.mSc.equals(SC_COLP) && !mmi.mSc.equals(SC_COLR) && !isServiceCodeCallForwarding(mmi.mSc) && !isServiceCodeCallBarring(mmi.mSc) && !mmi.mSc.equals(SC_WAIT) && !mmi.mSc.equals(SC_BS_MT) && !mmi.mSc.equals(SC_BAICa))) ? false : true;
    }
}
