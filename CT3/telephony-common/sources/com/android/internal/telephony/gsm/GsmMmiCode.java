package com.android.internal.telephony.gsm;

import android.R;
import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.text.BidiFormatter;
import android.text.SpannableStringBuilder;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import com.android.internal.telephony.CallFailCause;
import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.SSRequestDecisionMaker;
import com.android.internal.telephony.gsm.SsData;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GsmMmiCode extends Handler implements MmiCode {

    private static final int[] f24x56051b6d = null;

    private static final int[] f25x56377213 = null;
    static final String ACTION_ACTIVATE = "*";
    static final String ACTION_DEACTIVATE = "#";
    static final String ACTION_ERASURE = "##";
    static final String ACTION_INTERROGATE = "*#";
    static final String ACTION_REGISTER = "**";
    static final char END_OF_USSD_COMMAND = '#';
    static final int EVENT_GET_CLIR_COMPLETE = 2;
    static final int EVENT_GET_COLP_COMPLETE = 9;
    static final int EVENT_GET_COLR_COMPLETE = 8;
    static final int EVENT_QUERY_CF_COMPLETE = 3;
    static final int EVENT_QUERY_COMPLETE = 5;
    static final int EVENT_SET_CFF_COMPLETE = 6;
    static final int EVENT_SET_COMPLETE = 1;
    static final int EVENT_USSD_CANCEL_COMPLETE = 7;
    static final int EVENT_USSD_COMPLETE = 4;
    static final String LOG_TAG = "GsmMmiCode";
    static final int MATCH_GROUP_ACTION = 2;
    static final int MATCH_GROUP_DIALING_NUMBER = 12;
    static final int MATCH_GROUP_POUND_STRING = 1;
    static final int MATCH_GROUP_PWD_CONFIRM = 11;
    static final int MATCH_GROUP_SERVICE_CODE = 3;
    static final int MATCH_GROUP_SIA = 5;
    static final int MATCH_GROUP_SIB = 7;
    static final int MATCH_GROUP_SIC = 9;
    static final int MAX_LENGTH_SHORT_CODE = 2;
    static final String PROPERTY_RIL_SIM2_PIN1 = "gsm.sim.retry.pin1.2";
    static final String PROPERTY_RIL_SIM2_PIN2 = "gsm.sim.retry.pin2.2";
    static final String PROPERTY_RIL_SIM2_PUK1 = "gsm.sim.retry.puk1.2";
    static final String PROPERTY_RIL_SIM2_PUK2 = "gsm.sim.retry.puk2.2";
    static final String PROPERTY_RIL_SIM_PIN1 = "gsm.sim.retry.pin1";
    static final String PROPERTY_RIL_SIM_PIN2 = "gsm.sim.retry.pin2";
    static final String PROPERTY_RIL_SIM_PUK1 = "gsm.sim.retry.puk1";
    static final String PROPERTY_RIL_SIM_PUK2 = "gsm.sim.retry.puk2";
    static final String RETRY_BLOCKED = "0";
    static final String SC_BAIC = "35";
    static final String SC_BAICr = "351";
    static final String SC_BAOC = "33";
    static final String SC_BAOIC = "331";
    static final String SC_BAOICxH = "332";
    static final String SC_BA_ALL = "330";
    static final String SC_BA_MO = "333";
    static final String SC_BA_MT = "353";
    static final String SC_CFB = "67";
    static final String SC_CFNR = "62";
    static final String SC_CFNRy = "61";
    static final String SC_CFU = "21";
    static final String SC_CF_All = "002";
    static final String SC_CF_All_Conditional = "004";
    static final String SC_CLIP = "30";
    static final String SC_CLIR = "31";
    static final String SC_CNAP = "300";
    static final String SC_COLP = "76";
    static final String SC_COLR = "77";
    static final String SC_PIN = "04";
    static final String SC_PIN2 = "042";
    static final String SC_PUK = "05";
    static final String SC_PUK2 = "052";
    static final String SC_PWD = "03";
    static final String SC_WAIT = "43";
    static final String USSD_HANDLED_BY_STK = "stk";
    static Pattern sPatternSuppService = Pattern.compile("((\\*|#|\\*#|\\*\\*|##)(\\d{2,3})(\\*([^*#]*)(\\*([^*#]*)(\\*([^*#]*)(\\*([^*#]*))?)?)?)?#)(.*)");
    private static String[] sTwoDigitNumberPattern;
    String mAction;
    Context mContext;
    public String mDialingNumber;
    IccRecords mIccRecords;
    private boolean mIsCallFwdReg;
    private boolean mIsPendingUSSD;
    private boolean mIsSsInfo;
    private boolean mIsUssdRequest;
    CharSequence mMessage;
    private int mOrigUtCfuMode;
    GsmCdmaPhone mPhone;
    String mPoundString;
    String mPwd;
    private SSRequestDecisionMaker mSSReqDecisionMaker;
    String mSc;
    String mSia;
    String mSib;
    String mSic;
    MmiCode.State mState;
    UiccCardApplication mUiccApplication;
    private boolean mUserInitiatedMMI;

    private static int[] m394xbea91a11() {
        if (f24x56051b6d != null) {
            return f24x56051b6d;
        }
        int[] iArr = new int[SsData.RequestType.valuesCustom().length];
        try {
            iArr[SsData.RequestType.SS_ACTIVATION.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[SsData.RequestType.SS_DEACTIVATION.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[SsData.RequestType.SS_ERASURE.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[SsData.RequestType.SS_INTERROGATION.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[SsData.RequestType.SS_REGISTRATION.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        f24x56051b6d = iArr;
        return iArr;
    }

    private static int[] m395xbedb70b7() {
        if (f25x56377213 != null) {
            return f25x56377213;
        }
        int[] iArr = new int[SsData.ServiceType.valuesCustom().length];
        try {
            iArr[SsData.ServiceType.SS_ALL_BARRING.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[SsData.ServiceType.SS_BAIC.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[SsData.ServiceType.SS_BAIC_ROAMING.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[SsData.ServiceType.SS_BAOC.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[SsData.ServiceType.SS_BAOIC.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[SsData.ServiceType.SS_BAOIC_EXC_HOME.ordinal()] = 6;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[SsData.ServiceType.SS_CFU.ordinal()] = 7;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[SsData.ServiceType.SS_CF_ALL.ordinal()] = 8;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[SsData.ServiceType.SS_CF_ALL_CONDITIONAL.ordinal()] = 9;
        } catch (NoSuchFieldError e9) {
        }
        try {
            iArr[SsData.ServiceType.SS_CF_BUSY.ordinal()] = 10;
        } catch (NoSuchFieldError e10) {
        }
        try {
            iArr[SsData.ServiceType.SS_CF_NOT_REACHABLE.ordinal()] = 11;
        } catch (NoSuchFieldError e11) {
        }
        try {
            iArr[SsData.ServiceType.SS_CF_NO_REPLY.ordinal()] = 12;
        } catch (NoSuchFieldError e12) {
        }
        try {
            iArr[SsData.ServiceType.SS_CLIP.ordinal()] = 13;
        } catch (NoSuchFieldError e13) {
        }
        try {
            iArr[SsData.ServiceType.SS_CLIR.ordinal()] = 14;
        } catch (NoSuchFieldError e14) {
        }
        try {
            iArr[SsData.ServiceType.SS_COLP.ordinal()] = 23;
        } catch (NoSuchFieldError e15) {
        }
        try {
            iArr[SsData.ServiceType.SS_COLR.ordinal()] = 24;
        } catch (NoSuchFieldError e16) {
        }
        try {
            iArr[SsData.ServiceType.SS_INCOMING_BARRING.ordinal()] = 15;
        } catch (NoSuchFieldError e17) {
        }
        try {
            iArr[SsData.ServiceType.SS_OUTGOING_BARRING.ordinal()] = 16;
        } catch (NoSuchFieldError e18) {
        }
        try {
            iArr[SsData.ServiceType.SS_WAIT.ordinal()] = 17;
        } catch (NoSuchFieldError e19) {
        }
        f25x56377213 = iArr;
        return iArr;
    }

    public static GsmMmiCode newFromDialString(String dialString, GsmCdmaPhone phone, UiccCardApplication app) {
        Matcher m = sPatternSuppService.matcher(dialString);
        if (m.matches()) {
            GsmMmiCode ret = new GsmMmiCode(phone, app);
            ret.mPoundString = makeEmptyNull(m.group(1));
            ret.mAction = makeEmptyNull(m.group(2));
            ret.mSc = makeEmptyNull(m.group(3));
            ret.mSia = makeEmptyNull(m.group(5));
            ret.mSib = makeEmptyNull(m.group(7));
            ret.mSic = makeEmptyNull(m.group(9));
            ret.mPwd = makeEmptyNull(m.group(11));
            ret.mDialingNumber = makeEmptyNull(m.group(12));
            if (ret.mDialingNumber != null && ret.mDialingNumber.endsWith(ACTION_DEACTIVATE) && dialString.endsWith(ACTION_DEACTIVATE)) {
                GsmMmiCode ret2 = new GsmMmiCode(phone, app);
                ret2.mPoundString = dialString;
                return ret2;
            }
            return ret;
        }
        if (dialString.endsWith(ACTION_DEACTIVATE)) {
            GsmMmiCode ret3 = new GsmMmiCode(phone, app);
            ret3.mPoundString = dialString;
            return ret3;
        }
        if (isTwoDigitShortCode(phone.getContext(), dialString) || !isShortCode(dialString, phone)) {
            return null;
        }
        GsmMmiCode ret4 = new GsmMmiCode(phone, app);
        ret4.mDialingNumber = dialString;
        return ret4;
    }

    public static GsmMmiCode newNetworkInitiatedUssd(String ussdMessage, boolean isUssdRequest, GsmCdmaPhone phone, UiccCardApplication app) {
        GsmMmiCode ret = new GsmMmiCode(phone, app);
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

    public static GsmMmiCode newFromUssdUserInput(String ussdMessge, GsmCdmaPhone phone, UiccCardApplication app) {
        GsmMmiCode ret = new GsmMmiCode(phone, app);
        ret.mMessage = ussdMessge;
        ret.mState = MmiCode.State.PENDING;
        ret.mIsPendingUSSD = true;
        return ret;
    }

    public void processSsData(AsyncResult data) {
        Rlog.d(LOG_TAG, "In processSsData");
        this.mIsSsInfo = true;
        try {
            SsData ssData = (SsData) data.result;
            parseSsData(ssData);
        } catch (ClassCastException ex) {
            Rlog.e(LOG_TAG, "Class Cast Exception in parsing SS Data : " + ex);
        } catch (NullPointerException ex2) {
            Rlog.e(LOG_TAG, "Null Pointer Exception in parsing SS Data : " + ex2);
        }
    }

    void parseSsData(SsData ssData) {
        boolean zIsServiceClassVoiceorNone;
        CommandException ex = CommandException.fromRilErrno(ssData.result);
        this.mSc = getScStringFromScType(ssData.serviceType);
        this.mAction = getActionStringFromReqType(ssData.requestType);
        Rlog.d(LOG_TAG, "parseSsData msc = " + this.mSc + ", action = " + this.mAction + ", ex = " + ex);
        switch (m394xbea91a11()[ssData.requestType.ordinal()]) {
            case 1:
            case 2:
            case 3:
            case 5:
                if (ssData.result == 0 && ssData.serviceType.isTypeUnConditional()) {
                    if (ssData.requestType != SsData.RequestType.SS_ACTIVATION && ssData.requestType != SsData.RequestType.SS_REGISTRATION) {
                        zIsServiceClassVoiceorNone = false;
                    } else {
                        zIsServiceClassVoiceorNone = isServiceClassVoiceorNone(ssData.serviceClass);
                    }
                    Rlog.d(LOG_TAG, "setVoiceCallForwardingFlag cffEnabled: " + zIsServiceClassVoiceorNone);
                    if (this.mIccRecords != null) {
                        this.mPhone.setVoiceCallForwardingFlag(1, zIsServiceClassVoiceorNone, null);
                        Rlog.d(LOG_TAG, "setVoiceCallForwardingFlag done from SS Info.");
                    } else {
                        Rlog.e(LOG_TAG, "setVoiceCallForwardingFlag aborted. sim records is null.");
                    }
                }
                onSetComplete(null, new AsyncResult((Object) null, ssData.cfInfo, ex));
                break;
            case 4:
                if (ssData.serviceType.isTypeClir()) {
                    Rlog.d(LOG_TAG, "CLIR INTERROGATION");
                    onGetClirComplete(new AsyncResult((Object) null, ssData.ssInfo, ex));
                } else if (ssData.serviceType.isTypeCF()) {
                    Rlog.d(LOG_TAG, "CALL FORWARD INTERROGATION");
                    onQueryCfComplete(new AsyncResult((Object) null, ssData.cfInfo, ex));
                } else {
                    onQueryComplete(new AsyncResult((Object) null, ssData.ssInfo, ex));
                }
                break;
            default:
                Rlog.e(LOG_TAG, "Invaid requestType in SSData : " + ssData.requestType);
                break;
        }
    }

    private String getScStringFromScType(SsData.ServiceType sType) {
        switch (m395xbedb70b7()[sType.ordinal()]) {
            case 1:
                return SC_BA_ALL;
            case 2:
                return SC_BAIC;
            case 3:
                return SC_BAICr;
            case 4:
                return SC_BAOC;
            case 5:
                return SC_BAOIC;
            case 6:
                return SC_BAOICxH;
            case 7:
                return SC_CFU;
            case 8:
                return SC_CF_All;
            case 9:
                return SC_CF_All_Conditional;
            case 10:
                return SC_CFB;
            case 11:
                return SC_CFNR;
            case 12:
                return SC_CFNRy;
            case 13:
                return SC_CLIP;
            case 14:
                return SC_CLIR;
            case 15:
                return SC_BA_MT;
            case 16:
                return SC_BA_MO;
            case 17:
                return SC_WAIT;
            default:
                return UsimPBMemInfo.STRING_NOT_SET;
        }
    }

    private String getActionStringFromReqType(SsData.RequestType rType) {
        switch (m394xbea91a11()[rType.ordinal()]) {
            case 1:
                return "*";
            case 2:
                return ACTION_DEACTIVATE;
            case 3:
                return ACTION_ERASURE;
            case 4:
                return ACTION_INTERROGATE;
            case 5:
                return ACTION_REGISTER;
            default:
                return UsimPBMemInfo.STRING_NOT_SET;
        }
    }

    private boolean isServiceClassVoiceorNone(int serviceClass) {
        return (serviceClass & 1) != 0 || serviceClass == 0;
    }

    private static String makeEmptyNull(String s) {
        if (s == null || s.length() != 0) {
            return s;
        }
        return null;
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

    public GsmMmiCode(GsmCdmaPhone phone, UiccCardApplication app) {
        super(phone.getHandler().getLooper());
        this.mState = MmiCode.State.PENDING;
        this.mIsSsInfo = false;
        this.mUserInitiatedMMI = false;
        this.mOrigUtCfuMode = 0;
        this.mPhone = phone;
        this.mContext = phone.getContext();
        this.mUiccApplication = app;
        if (app != null) {
            this.mIccRecords = app.getIccRecords();
        }
        this.mSSReqDecisionMaker = this.mPhone.getSSRequestDecisionMaker();
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

    public void setUserInitiatedMMI(boolean userinit) {
        this.mUserInitiatedMMI = userinit;
    }

    @Override
    public boolean getUserInitiatedMMI() {
        return this.mUserInitiatedMMI;
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

    private static boolean isShortCode(String dialString, GsmCdmaPhone phone) {
        if (dialString == null || dialString.length() == 0 || PhoneNumberUtils.isLocalEmergencyNumber(phone.getContext(), dialString)) {
            return false;
        }
        return isShortCodeUSSD(dialString, phone);
    }

    private static boolean isShortCodeUSSD(String dialString, GsmCdmaPhone phone) {
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

    public boolean isTemporaryModeCLIR() {
        if (this.mSc == null || !this.mSc.equals(SC_CLIR) || this.mDialingNumber == null) {
            return false;
        }
        if (isActivate()) {
            return true;
        }
        return isDeactivate();
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

    public boolean isSsInfo() {
        return this.mIsSsInfo;
    }

    @Override
    public void processCode() throws CallStateException {
        int cfAction;
        try {
            if (isShortCode()) {
                Rlog.d(LOG_TAG, "isShortCode");
                sendUssd(this.mDialingNumber);
                return;
            }
            if (this.mDialingNumber != null) {
                Rlog.w(LOG_TAG, "Special USSD Support:" + this.mPoundString + this.mDialingNumber);
                sendUssd(this.mPoundString + this.mDialingNumber);
                return;
            }
            if (this.mSc != null && this.mSc.equals(SC_CNAP)) {
                Rlog.d(LOG_TAG, "is CNAP");
                if (this.mPoundString != null) {
                    sendCNAPSS(this.mPoundString);
                    return;
                }
                return;
            }
            if (this.mSc != null && this.mSc.equals(SC_CLIP)) {
                Rlog.d(LOG_TAG, "is CLIP");
                if (isActivate()) {
                    Rlog.d(LOG_TAG, "is CLIP - isActivate");
                    if (this.mPhone.getCsFallbackStatus() == 0 && this.mPhone.isGsmUtSupport()) {
                        this.mSSReqDecisionMaker.setCLIP(1, obtainMessage(1, this));
                        return;
                    }
                    if (this.mPhone.getCsFallbackStatus() == 1) {
                        this.mPhone.setCsFallbackStatus(0);
                    }
                    this.mPhone.mCi.setCLIP(true, obtainMessage(1, this));
                    return;
                }
                if (isDeactivate()) {
                    Rlog.d(LOG_TAG, "is CLIP - isDeactivate");
                    if (this.mPhone.getCsFallbackStatus() == 0 && this.mPhone.isGsmUtSupport()) {
                        this.mSSReqDecisionMaker.setCLIP(0, obtainMessage(1, this));
                        return;
                    }
                    if (this.mPhone.getCsFallbackStatus() == 1) {
                        this.mPhone.setCsFallbackStatus(0);
                    }
                    this.mPhone.mCi.setCLIP(false, obtainMessage(1, this));
                    return;
                }
                if (!isInterrogate()) {
                    throw new RuntimeException("Invalid or Unsupported MMI Code");
                }
                if (this.mPhone.getCsFallbackStatus() == 0 && this.mPhone.isGsmUtSupport()) {
                    this.mSSReqDecisionMaker.getCLIP(obtainMessage(5, this));
                    return;
                }
                if (this.mPhone.getCsFallbackStatus() == 1) {
                    this.mPhone.setCsFallbackStatus(0);
                }
                this.mPhone.mCi.queryCLIP(obtainMessage(5, this));
                return;
            }
            if (this.mSc != null && this.mSc.equals(SC_CLIR)) {
                Rlog.d(LOG_TAG, "is CLIR");
                if (isActivate()) {
                    if (this.mPhone.isOpTbClir()) {
                        this.mPhone.mCi.setCLIR(1, obtainMessage(1, this));
                        return;
                    }
                    if (this.mPhone.getCsFallbackStatus() == 0 && this.mPhone.isGsmUtSupport()) {
                        this.mSSReqDecisionMaker.setCLIR(1, obtainMessage(1, this));
                        return;
                    }
                    if (this.mPhone.getCsFallbackStatus() == 1) {
                        this.mPhone.setCsFallbackStatus(0);
                    }
                    this.mPhone.mCi.setCLIR(1, obtainMessage(1, this));
                    return;
                }
                if (isDeactivate()) {
                    if (this.mPhone.isOpTbClir()) {
                        this.mPhone.mCi.setCLIR(2, obtainMessage(1, this));
                        return;
                    }
                    if (this.mPhone.getCsFallbackStatus() == 0 && this.mPhone.isGsmUtSupport()) {
                        this.mSSReqDecisionMaker.setCLIR(2, obtainMessage(1, this));
                        return;
                    }
                    if (this.mPhone.getCsFallbackStatus() == 1) {
                        this.mPhone.setCsFallbackStatus(0);
                    }
                    this.mPhone.mCi.setCLIR(2, obtainMessage(1, this));
                    return;
                }
                if (!isInterrogate()) {
                    throw new RuntimeException("Invalid or Unsupported MMI Code");
                }
                if (this.mPhone.isOpTbClir()) {
                    this.mPhone.mCi.getCLIR(obtainMessage(2, this));
                    return;
                }
                if (this.mPhone.getCsFallbackStatus() == 0 && this.mPhone.isGsmUtSupport()) {
                    this.mSSReqDecisionMaker.getCLIR(obtainMessage(2, this));
                    return;
                }
                if (this.mPhone.getCsFallbackStatus() == 1) {
                    this.mPhone.setCsFallbackStatus(0);
                }
                this.mPhone.mCi.getCLIR(obtainMessage(2, this));
                return;
            }
            if (this.mSc != null && this.mSc.equals(SC_COLP)) {
                Rlog.d(LOG_TAG, "is COLP");
                if (!isInterrogate()) {
                    throw new RuntimeException("Invalid or Unsupported MMI Code");
                }
                if (this.mPhone.getCsFallbackStatus() == 0 && this.mPhone.isGsmUtSupport()) {
                    this.mSSReqDecisionMaker.getCOLP(obtainMessage(9, this));
                    return;
                }
                if (this.mPhone.getCsFallbackStatus() == 1) {
                    this.mPhone.setCsFallbackStatus(0);
                }
                this.mPhone.mCi.getCOLP(obtainMessage(9, this));
                return;
            }
            if (this.mSc != null && this.mSc.equals(SC_COLR)) {
                Rlog.d(LOG_TAG, "is COLR");
                if (!isInterrogate()) {
                    throw new RuntimeException("Invalid or Unsupported MMI Code");
                }
                if (this.mPhone.getCsFallbackStatus() == 0 && this.mPhone.isGsmUtSupport()) {
                    this.mSSReqDecisionMaker.getCOLR(obtainMessage(8, this));
                    return;
                }
                if (this.mPhone.getCsFallbackStatus() == 1) {
                    this.mPhone.setCsFallbackStatus(0);
                }
                this.mPhone.mCi.getCOLR(obtainMessage(8, this));
                return;
            }
            if (isServiceCodeCallForwarding(this.mSc)) {
                Rlog.d(LOG_TAG, "is CF");
                String dialingNumber = this.mSia;
                int serviceClass = siToServiceClass(this.mSib);
                int reason = scToCallForwardReason(this.mSc);
                int time = siToTime(this.mSic);
                if (isInterrogate()) {
                    if (this.mPhone.getCsFallbackStatus() == 0 && this.mPhone.isGsmUtSupport()) {
                        this.mSSReqDecisionMaker.queryCallForwardStatus(reason, serviceClass, dialingNumber, obtainMessage(3, this));
                        return;
                    }
                    if (this.mPhone.getCsFallbackStatus() == 1) {
                        this.mPhone.setCsFallbackStatus(0);
                    }
                    this.mPhone.mCi.queryCallForwardStatus(reason, serviceClass, dialingNumber, obtainMessage(3, this));
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
                } else {
                    if (!isErasure()) {
                        throw new RuntimeException("invalid action");
                    }
                    cfAction = 4;
                }
                int isSettingUnconditionalVoice = ((reason == 0 || reason == 4) && ((serviceClass & 1) != 0 || serviceClass == 0)) ? 1 : 0;
                int isEnableDesired = (cfAction == 1 || cfAction == 3) ? 1 : 0;
                Rlog.d(LOG_TAG, "is CF setCallForward");
                if (isSettingUnconditionalVoice == 1) {
                    this.mOrigUtCfuMode = 0;
                    String utCfuMode = this.mPhone.getSystemProperty("persist.radio.ut.cfu.mode", "disabled_ut_cfu_mode");
                    if ("enabled_ut_cfu_mode_on".equals(utCfuMode)) {
                        this.mOrigUtCfuMode = 1;
                    } else if ("enabled_ut_cfu_mode_off".equals(utCfuMode)) {
                        this.mOrigUtCfuMode = 2;
                    }
                    this.mPhone.setSystemProperty("persist.radio.ut.cfu.mode", "disabled_ut_cfu_mode");
                }
                if (this.mPhone.getCsFallbackStatus() == 0 && this.mPhone.isGsmUtSupport()) {
                    this.mSSReqDecisionMaker.setCallForward(cfAction, reason, serviceClass, dialingNumber, time, obtainMessage(6, isSettingUnconditionalVoice, isEnableDesired, this));
                    return;
                }
                if (this.mPhone.getCsFallbackStatus() == 1) {
                    this.mPhone.setCsFallbackStatus(0);
                }
                this.mPhone.mCi.setCallForward(cfAction, reason, serviceClass, dialingNumber, time, obtainMessage(6, isSettingUnconditionalVoice, isEnableDesired, this));
                return;
            }
            if (isServiceCodeCallBarring(this.mSc)) {
                String password = this.mSia;
                int serviceClass2 = siToServiceClass(this.mSib);
                String facility = scToBarringFacility(this.mSc);
                if (isInterrogate()) {
                    if (password != null) {
                        throw new RuntimeException("Invalid or Unsupported MMI Code");
                    }
                    if (this.mPhone.getCsFallbackStatus() == 0 && this.mPhone.isGsmUtSupport()) {
                        this.mSSReqDecisionMaker.queryFacilityLock(facility, password, serviceClass2, obtainMessage(5, this));
                        return;
                    }
                    if (this.mPhone.getCsFallbackStatus() == 1) {
                        this.mPhone.setCsFallbackStatus(0);
                    }
                    this.mPhone.mCi.queryFacilityLock(facility, password, serviceClass2, obtainMessage(5, this));
                    return;
                }
                if (!isActivate() && !isDeactivate()) {
                    throw new RuntimeException("Invalid or Unsupported MMI Code");
                }
                if (password == null || password.length() != 4) {
                    handlePasswordError(R.string.PERSOSUBSTATE_SIM_NETWORK_ERROR);
                    return;
                }
                if (this.mPhone.getCsFallbackStatus() == 0 && this.mPhone.isGsmUtSupport()) {
                    this.mSSReqDecisionMaker.setFacilityLock(facility, isActivate(), password, serviceClass2, obtainMessage(1, this));
                    return;
                }
                if (this.mPhone.getCsFallbackStatus() == 1) {
                    this.mPhone.setCsFallbackStatus(0);
                }
                this.mPhone.mCi.setFacilityLock(facility, isActivate(), password, serviceClass2, obtainMessage(1, this));
                return;
            }
            if (this.mSc != null && this.mSc.equals(SC_PWD)) {
                String oldPwd = this.mSib;
                String newPwd = this.mSic;
                if (!isActivate() && !isRegister()) {
                    throw new RuntimeException("Invalid or Unsupported MMI Code");
                }
                this.mAction = ACTION_REGISTER;
                String facility2 = this.mSia == null ? CommandsInterface.CB_FACILITY_BA_ALL : scToBarringFacility(this.mSia);
                if (oldPwd == null || newPwd == null || this.mPwd == null) {
                    handlePasswordError(R.string.PERSOSUBSTATE_SIM_NETWORK_ERROR);
                    return;
                }
                if (this.mPwd.length() != newPwd.length() || oldPwd.length() != 4 || this.mPwd.length() != 4) {
                    handlePasswordError(R.string.PERSOSUBSTATE_SIM_NETWORK_ERROR);
                    return;
                }
                if (!this.mPhone.isDuringImsCall()) {
                    this.mPhone.mCi.changeBarringPassword(facility2, oldPwd, newPwd, this.mPwd, obtainMessage(1, this));
                    return;
                }
                Message msg = obtainMessage(1, this);
                CommandException ce = new CommandException(CommandException.Error.GENERIC_FAILURE);
                AsyncResult.forMessage(msg, (Object) null, ce);
                msg.sendToTarget();
                return;
            }
            if (this.mSc != null && this.mSc.equals(SC_WAIT)) {
                int serviceClass3 = siToServiceClass(this.mSia);
                int tbcwMode = this.mPhone.getTbcwMode();
                if (isActivate() || isDeactivate()) {
                    if (tbcwMode == 2 && !this.mPhone.isOpNwCW()) {
                        this.mPhone.setTerminalBasedCallWaiting(isActivate(), obtainMessage(1, this));
                        return;
                    }
                    if (tbcwMode == 3 || tbcwMode == 4) {
                        if (this.mPhone.getCsFallbackStatus() == 1) {
                            this.mPhone.setCsFallbackStatus(0);
                        }
                        this.mPhone.mCi.setCallWaiting(isActivate(), serviceClass3, obtainMessage(1, isActivate() ? 1 : 0, -1, this));
                        return;
                    } else {
                        Rlog.d(LOG_TAG, "processCode  setCallWaiting");
                        if (this.mPhone.getCsFallbackStatus() == 0 && this.mPhone.isGsmUtSupport()) {
                            this.mSSReqDecisionMaker.setCallWaiting(isActivate(), serviceClass3, obtainMessage(1, this));
                            return;
                        } else {
                            this.mPhone.mCi.setCallWaiting(isActivate(), serviceClass3, obtainMessage(1, this));
                            return;
                        }
                    }
                }
                if (!isInterrogate()) {
                    throw new RuntimeException("Invalid or Unsupported MMI Code");
                }
                if (tbcwMode == 2 && !this.mPhone.isOpNwCW()) {
                    this.mPhone.getTerminalBasedCallWaiting(obtainMessage(5, this));
                    return;
                }
                if (tbcwMode == 3 || tbcwMode == 4) {
                    if (this.mPhone.getCsFallbackStatus() == 1) {
                        this.mPhone.setCsFallbackStatus(0);
                    }
                    this.mPhone.mCi.queryCallWaiting(serviceClass3, obtainMessage(5, this));
                    return;
                } else if (this.mPhone.getCsFallbackStatus() == 0 && this.mPhone.isGsmUtSupport()) {
                    this.mSSReqDecisionMaker.queryCallWaiting(serviceClass3, obtainMessage(5, this));
                    return;
                } else {
                    this.mPhone.mCi.queryCallWaiting(serviceClass3, obtainMessage(5, this));
                    return;
                }
            }
            if (!isPinPukCommand()) {
                if (this.mPoundString == null) {
                    throw new RuntimeException("Invalid or Unsupported MMI Code");
                }
                sendUssd(this.mPoundString);
                return;
            }
            Rlog.d(LOG_TAG, "is PIN command");
            String oldPinOrPuk = this.mSia;
            String newPinOrPuk = this.mSib;
            int pinLen = newPinOrPuk != null ? newPinOrPuk.length() : 0;
            int phoneId = this.mPhone.getPhoneId();
            StringBuilder appendStr = new StringBuilder();
            if (phoneId != 0) {
                appendStr.append(".").append(phoneId + 1);
            }
            String retryPin1 = SystemProperties.get(PROPERTY_RIL_SIM_PIN1 + appendStr.toString(), (String) null);
            String retryPin2 = SystemProperties.get(PROPERTY_RIL_SIM_PIN2 + appendStr.toString(), (String) null);
            String retryPuk1 = SystemProperties.get(PROPERTY_RIL_SIM_PUK1 + appendStr.toString(), (String) null);
            String retryPuk2 = SystemProperties.get(PROPERTY_RIL_SIM_PUK2 + appendStr.toString(), (String) null);
            Rlog.d(LOG_TAG, "retryPin1:" + retryPin1 + "\nretryPin2:" + retryPin2 + "\nretryPuk1:" + retryPuk1 + "\nretryPuk2:" + retryPuk2 + "\n");
            if (!isRegister()) {
                throw new RuntimeException("Ivalid register/action=" + this.mAction);
            }
            if (newPinOrPuk == null || oldPinOrPuk == null) {
                handlePasswordError(R.string.PERSOSUBSTATE_SIM_ICCID_IN_PROGRESS);
                return;
            }
            int oldPinLen = oldPinOrPuk.length();
            Phone currentPhone = PhoneFactory.getPhone(this.mPhone.getPhoneId());
            IccCard iccCard = currentPhone.getIccCard();
            if (!iccCard.hasIccCard()) {
                handlePasswordError(R.string.PERSOSUBSTATE_SIM_ICCID_IN_PROGRESS);
                return;
            }
            if (!newPinOrPuk.equals(this.mSic)) {
                handlePasswordError(R.string.PERSOSUBSTATE_SIM_NETWORK_PUK_IN_PROGRESS);
                return;
            }
            if ((this.mSc.equals(SC_PIN) || this.mSc.equals(SC_PIN2)) && (pinLen < 4 || pinLen > 8 || oldPinLen < 4 || oldPinLen > 8)) {
                handlePasswordError(134545411);
                return;
            }
            if (((this.mSc.equals(SC_PUK) || this.mSc.equals(SC_PUK2)) && pinLen < 4) || pinLen > 8) {
                handlePasswordError(134545411);
                return;
            }
            if (this.mSc.equals(SC_PIN) && this.mUiccApplication != null && this.mUiccApplication.getState() == IccCardApplicationStatus.AppState.APPSTATE_PUK) {
                handlePasswordError(R.string.PERSOSUBSTATE_SIM_NETWORK_SUBSET_ERROR);
                return;
            }
            if (!isValidPin(newPinOrPuk)) {
                handlePasswordError(R.string.PERSOSUBSTATE_SIM_ICCID_IN_PROGRESS);
                return;
            }
            if (this.mUiccApplication == null) {
                throw new RuntimeException("No application mUiccApplicaiton is null");
            }
            Rlog.d(LOG_TAG, "process mmi service code using UiccApp sc=" + this.mSc);
            if (this.mSc.equals(SC_PIN)) {
                if ("0".equals(retryPin1)) {
                    handlePasswordError(R.string.PERSOSUBSTATE_SIM_NETWORK_SUBSET_ERROR);
                    return;
                } else {
                    this.mUiccApplication.changeIccLockPassword(oldPinOrPuk, newPinOrPuk, obtainMessage(1, this));
                    return;
                }
            }
            if (this.mSc.equals(SC_PIN2)) {
                if ("0".equals(retryPin2)) {
                    handlePasswordError(R.string.PERSOSUBSTATE_SIM_NETWORK_SUBSET_IN_PROGRESS);
                    return;
                } else {
                    this.mUiccApplication.changeIccFdnPassword(oldPinOrPuk, newPinOrPuk, obtainMessage(1, this));
                    return;
                }
            }
            if (this.mSc.equals(SC_PUK)) {
                if ("0".equals(retryPuk1)) {
                    handlePasswordError(134545413);
                    return;
                } else if (oldPinOrPuk.length() == 8) {
                    this.mUiccApplication.supplyPuk(oldPinOrPuk, newPinOrPuk, obtainMessage(1, this));
                    return;
                } else {
                    handlePasswordError(134545429);
                    return;
                }
            }
            if (this.mSc.equals(SC_PUK2)) {
                if ("0".equals(retryPuk2)) {
                    handlePasswordError(134545414);
                } else if (oldPinOrPuk.length() == 8) {
                    this.mUiccApplication.supplyPuk2(oldPinOrPuk, newPinOrPuk, obtainMessage(1, this));
                } else {
                    handlePasswordError(134545429);
                }
            }
        } catch (RuntimeException exc) {
            this.mState = MmiCode.State.FAILED;
            exc.printStackTrace();
            Rlog.d(LOG_TAG, "exc.toString() = " + exc.toString());
            Rlog.d(LOG_TAG, "procesCode: mState = FAILED");
            this.mMessage = this.mContext.getText(R.string.PERSOSUBSTATE_SIM_ICCID_IN_PROGRESS);
            this.mPhone.onMMIDone(this);
        }
    }

    private void handlePasswordError(int res) {
        this.mState = MmiCode.State.FAILED;
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        sb.append(this.mContext.getText(res));
        this.mMessage = sb;
        this.mPhone.onMMIDone(this);
    }

    public void onUssdFinished(String ussdMessage, boolean isUssdRequest) {
        if (this.mState != MmiCode.State.PENDING) {
            return;
        }
        if (ussdMessage == null || ussdMessage.length() == 0) {
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

    public void onUssdStkHandling(String ussdMessage, boolean isUssdRequest) {
        if (this.mState != MmiCode.State.PENDING) {
            return;
        }
        if (ussdMessage == null || ussdMessage.length() == 0) {
            this.mMessage = this.mContext.getText(R.string.PERSOSUBSTATE_SIM_NETWORK_IN_PROGRESS);
        } else {
            this.mMessage = ussdMessage;
        }
        this.mIsUssdRequest = isUssdRequest;
        if (!isUssdRequest) {
            this.mState = MmiCode.State.COMPLETE;
        }
        this.mPhone.onMMIDone(this, USSD_HANDLED_BY_STK);
    }

    public void onUssdFinishedError() {
        if (this.mState != MmiCode.State.PENDING) {
            return;
        }
        this.mState = MmiCode.State.FAILED;
        this.mMessage = this.mContext.getText(R.string.PERSOSUBSTATE_SIM_ICCID_IN_PROGRESS);
        this.mPhone.onMMIDone(this);
    }

    public void onUssdRelease() {
        if (this.mState != MmiCode.State.PENDING) {
            return;
        }
        this.mState = MmiCode.State.COMPLETE;
        this.mMessage = null;
        this.mPhone.onMMIDone(this);
    }

    public void sendUssd(String ussdMessage) {
        this.mIsPendingUSSD = true;
        this.mPhone.mCi.sendUSSD(ussdMessage, obtainMessage(4, this));
    }

    void sendCNAPSS(String cnapssMessage) {
        Rlog.d(LOG_TAG, "sendCNAPSS");
        this.mPhone.mCi.sendCNAPSS(cnapssMessage, obtainMessage(5, this));
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                AsyncResult ar = (AsyncResult) msg.obj;
                if (this.mSc.equals(SC_WAIT) && this.mPhone.getTbcwMode() == 4 && ar.exception == null) {
                    int ienable = msg.arg1;
                    boolean enable = ienable == 1;
                    this.mPhone.setTerminalBasedCallWaiting(enable, null);
                }
                onSetComplete(msg, ar);
                break;
            case 2:
                onGetClirComplete((AsyncResult) msg.obj);
                break;
            case 3:
                onQueryCfComplete((AsyncResult) msg.obj);
                break;
            case 4:
                AsyncResult ar2 = (AsyncResult) msg.obj;
                if (ar2.exception != null) {
                    this.mState = MmiCode.State.FAILED;
                    this.mMessage = getErrorMessage(ar2);
                    this.mPhone.onMMIDone(this);
                }
                break;
            case 5:
                AsyncResult ar3 = (AsyncResult) msg.obj;
                if (this.mSc.equals(SC_WAIT) && this.mPhone.getTbcwMode() == 4) {
                    Rlog.d(LOG_TAG, "TBCW_OPTBCW_WITH_CS");
                    if (ar3.exception == null) {
                        int[] cwArray = (int[]) ar3.result;
                        try {
                            Rlog.d(LOG_TAG, "EVENT_GET_CALL_WAITING_FOR_CS_TB cwArray[0]:cwArray[1] = " + cwArray[0] + ":" + cwArray[1]);
                            boolean csEnable = cwArray[0] == 1 && (cwArray[1] & 1) == 1;
                            this.mPhone.setTerminalBasedCallWaiting(csEnable, null);
                        } catch (ArrayIndexOutOfBoundsException e) {
                            Rlog.e(LOG_TAG, "EVENT_GET_CALL_WAITING_FOR_CS_TB: improper result: err =" + e.getMessage());
                        }
                    }
                }
                onQueryComplete(ar3);
                break;
            case 6:
                AsyncResult ar4 = (AsyncResult) msg.obj;
                if (ar4.exception == null && msg.arg1 == 1) {
                    boolean cffEnabled = msg.arg2 == 1;
                    if (this.mIccRecords != null) {
                        this.mPhone.setVoiceCallForwardingFlag(1, cffEnabled, this.mDialingNumber);
                        this.mPhone.saveTimeSlot(null);
                    }
                }
                if (ar4.exception != null && this.mOrigUtCfuMode != 0) {
                    if (this.mOrigUtCfuMode == 1) {
                        this.mPhone.setSystemProperty("persist.radio.ut.cfu.mode", "enabled_ut_cfu_mode_on");
                    } else {
                        this.mPhone.setSystemProperty("persist.radio.ut.cfu.mode", "enabled_ut_cfu_mode_off");
                    }
                }
                this.mOrigUtCfuMode = 0;
                onSetComplete(msg, ar4);
                break;
            case 7:
                this.mPhone.onMMIDone(this);
                break;
            case 8:
                onGetColrComplete((AsyncResult) msg.obj);
                break;
            case 9:
                onGetColpComplete((AsyncResult) msg.obj);
                break;
        }
    }

    private CharSequence getErrorMessage(AsyncResult ar) {
        if (ar.exception instanceof CommandException) {
            CommandException.Error err = ((CommandException) ar.exception).getCommandError();
            if (err == CommandException.Error.FDN_CHECK_FAILURE) {
                Rlog.i(LOG_TAG, "FDN_CHECK_FAILURE");
                return this.mContext.getText(R.string.PERSOSUBSTATE_SIM_ICCID_SUCCESS);
            }
            if (err == CommandException.Error.USSD_MODIFIED_TO_DIAL) {
                Rlog.i(LOG_TAG, "USSD_MODIFIED_TO_DIAL");
                return this.mContext.getText(R.string.lockscreen_permanent_disabled_sim_message_short);
            }
            if (err == CommandException.Error.USSD_MODIFIED_TO_SS) {
                Rlog.i(LOG_TAG, "USSD_MODIFIED_TO_SS");
                return this.mContext.getText(R.string.lockscreen_return_to_call);
            }
            if (err == CommandException.Error.USSD_MODIFIED_TO_USSD) {
                Rlog.i(LOG_TAG, "USSD_MODIFIED_TO_USSD");
                return this.mContext.getText(R.string.lockscreen_screen_locked);
            }
            if (err == CommandException.Error.SS_MODIFIED_TO_DIAL) {
                Rlog.i(LOG_TAG, "SS_MODIFIED_TO_DIAL");
                return this.mContext.getText(R.string.lockscreen_sim_locked_message);
            }
            if (err == CommandException.Error.SS_MODIFIED_TO_USSD) {
                Rlog.i(LOG_TAG, "SS_MODIFIED_TO_USSD");
                return this.mContext.getText(R.string.lockscreen_sim_puk_locked_instructions);
            }
            if (err == CommandException.Error.SS_MODIFIED_TO_SS) {
                Rlog.i(LOG_TAG, "SS_MODIFIED_TO_SS");
                return this.mContext.getText(R.string.lockscreen_sim_puk_locked_message);
            }
        }
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
            if (this.mSc.equals(SC_CLIP)) {
                return this.mContext.getText(R.string.PERSOSUBSTATE_SIM_NETWORK_SUBSET_PUK_SUCCESS);
            }
            if (this.mSc.equals(SC_CLIR)) {
                return this.mContext.getText(R.string.PERSOSUBSTATE_SIM_NETWORK_SUBSET_SUCCESS);
            }
            if (this.mSc.equals(SC_PWD)) {
                return this.mContext.getText(R.string.PERSOSUBSTATE_SIM_SERVICE_PROVIDER_ENTRY);
            }
            if (this.mSc.equals(SC_WAIT)) {
                return this.mContext.getText(R.string.PERSOSUBSTATE_SIM_NS_SP_IN_PROGRESS);
            }
            if (isPinPukCommand()) {
                return this.mContext.getText(R.string.PERSOSUBSTATE_SIM_SERVICE_PROVIDER_ERROR);
            }
            if (this.mSc.equals(SC_PIN)) {
                return this.mContext.getText(R.string.PERSOSUBSTATE_SIM_SERVICE_PROVIDER_ERROR);
            }
            if (this.mSc.equals(SC_PIN2)) {
                return this.mContext.getText(134545417);
            }
            if (this.mSc.equals(SC_PUK)) {
                return this.mContext.getText(134545418);
            }
            if (this.mSc.equals(SC_PUK2)) {
                return this.mContext.getText(134545419);
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
                CommandException.Error err = ((CommandException) ar.exception).getCommandError();
                if (err == CommandException.Error.PASSWORD_INCORRECT) {
                    if (isPinPukCommand()) {
                        if (this.mSc.equals(SC_PUK)) {
                            sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_NETWORK_PUK_ERROR));
                        } else if (this.mSc.equals(SC_PUK2)) {
                            sb.append(this.mContext.getText(134545410));
                        } else if (this.mSc.equals(SC_PIN)) {
                            sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_NETWORK_PUK_ENTRY));
                        } else if (this.mSc.equals(SC_PIN2)) {
                            sb.append(this.mContext.getText(134545409));
                        }
                        int attemptsRemaining = msg.arg1;
                        if (attemptsRemaining <= 0) {
                            Rlog.d(LOG_TAG, "onSetComplete: PUK locked, cancel as lock screen will handle this");
                            this.mState = MmiCode.State.CANCELLED;
                        } else if (attemptsRemaining > 0) {
                            Rlog.d(LOG_TAG, "onSetComplete: attemptsRemaining=" + attemptsRemaining);
                            sb.append(this.mContext.getResources().getQuantityString(R.menu.language_selection_list, attemptsRemaining, Integer.valueOf(attemptsRemaining)));
                        }
                    } else {
                        sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_NETWORK_ERROR));
                    }
                } else if (err == CommandException.Error.SIM_PUK2) {
                    sb.append(this.mContext.getText(134545409));
                    sb.append("\n");
                    sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_NETWORK_SUBSET_IN_PROGRESS));
                } else if (err == CommandException.Error.REQUEST_NOT_SUPPORTED) {
                    if (this.mSc.equals(SC_PIN)) {
                        sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_NETWORK_SUBSET_PUK_ENTRY));
                    }
                } else if (err == CommandException.Error.CALL_BARRED) {
                    sb.append(this.mContext.getText(134545416));
                } else if (err == CommandException.Error.FDN_CHECK_FAILURE) {
                    Rlog.i(LOG_TAG, "FDN_CHECK_FAILURE");
                    sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_ICCID_SUCCESS));
                } else {
                    sb.append(getErrorMessage(ar));
                }
            } else {
                sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_ICCID_IN_PROGRESS));
            }
        } else if (isActivate()) {
            this.mState = MmiCode.State.COMPLETE;
            if (this.mIsCallFwdReg) {
                sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_IMPI_SUCCESS));
            } else {
                sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_IMPI_ENTRY));
            }
            if (this.mSc.equals(SC_CLIR)) {
                this.mPhone.saveClirSetting(1);
            }
        } else if (isDeactivate()) {
            this.mState = MmiCode.State.COMPLETE;
            sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_IMPI_IN_PROGRESS));
            if (this.mSc.equals(SC_CLIR)) {
                this.mPhone.saveClirSetting(2);
            }
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

    private void onGetClirComplete(AsyncResult ar) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        if (ar.exception != null) {
            this.mState = MmiCode.State.FAILED;
            if (ar.exception instanceof CommandException) {
                CommandException.Error err = ((CommandException) ar.exception).getCommandError();
                if (err == CommandException.Error.CALL_BARRED) {
                    sb.append(this.mContext.getText(134545416));
                } else if (err == CommandException.Error.FDN_CHECK_FAILURE) {
                    sb.append(this.mContext.getText(134545415));
                } else {
                    sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_ICCID_IN_PROGRESS));
                }
            } else {
                sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_ICCID_IN_PROGRESS));
            }
        } else {
            int[] clirArgs = (int[]) ar.result;
            switch (clirArgs[1]) {
                case 0:
                    sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_SIM_PUK_ERROR));
                    this.mState = MmiCode.State.COMPLETE;
                    break;
                case 1:
                    sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_SIM_PUK_IN_PROGRESS));
                    this.mState = MmiCode.State.COMPLETE;
                    break;
                case 2:
                    sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_ICCID_IN_PROGRESS));
                    this.mState = MmiCode.State.FAILED;
                    break;
                case 3:
                    switch (clirArgs[0]) {
                        case 0:
                        default:
                            sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_SIM_ENTRY));
                            break;
                        case 1:
                            sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_SIM_ENTRY));
                            break;
                        case 2:
                            sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_SIM_ERROR));
                            break;
                    }
                    this.mState = MmiCode.State.COMPLETE;
                    break;
                case 4:
                    switch (clirArgs[0]) {
                        case 0:
                        default:
                            sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_SIM_PUK_ENTRY));
                            break;
                        case 1:
                            sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_SIM_IN_PROGRESS));
                            break;
                        case 2:
                            sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_SIM_PUK_ENTRY));
                            break;
                    }
                    this.mState = MmiCode.State.COMPLETE;
                    break;
            }
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
            case 256:
            case 512:
                return this.mContext.getText(134545476);
            default:
                return null;
        }
    }

    private CharSequence makeCFQueryResultMessage(CallForwardInfo info, int serviceClassMask) {
        CharSequence template;
        String[] sources = {"{0}", "{1}", "{2}"};
        CharSequence[] destinations = new CharSequence[3];
        boolean needTimeTemplate = info.reason == 2;
        if (info.status == 1 && !isEmptyOrNull(info.number)) {
            if (needTimeTemplate) {
                template = this.mContext.getText(R.string.accessibility_dialog_button_uninstall);
            } else {
                template = this.mContext.getText(R.string.accessibility_dialog_button_deny);
            }
        } else if (isEmptyOrNull(info.number)) {
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

    private String formatLtr(String str) {
        BidiFormatter fmt = BidiFormatter.getInstance();
        return str == null ? str : fmt.unicodeWrap(str, TextDirectionHeuristics.LTR, true);
    }

    private void onQueryCfComplete(AsyncResult ar) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        if (ar.exception != null) {
            this.mState = MmiCode.State.FAILED;
            if (ar.exception instanceof CommandException) {
                CommandException.Error err = ((CommandException) ar.exception).getCommandError();
                if (err == CommandException.Error.CALL_BARRED) {
                    sb.append(this.mContext.getText(134545416));
                } else if (err == CommandException.Error.FDN_CHECK_FAILURE) {
                    sb.append(this.mContext.getText(134545415));
                } else {
                    sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_ICCID_IN_PROGRESS));
                }
            } else {
                sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_ICCID_IN_PROGRESS));
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
                boolean isAllCfDisabled = false;
                int i = 0;
                int s = infos.length;
                while (true) {
                    if (i >= s) {
                        break;
                    }
                    if (infos[i].serviceClass != 61) {
                        i++;
                    } else {
                        isAllCfDisabled = true;
                        break;
                    }
                }
                Rlog.d(LOG_TAG, "[GsmMmiCode] isAllCfDisabled = " + isAllCfDisabled);
                for (int serviceClassMask = 1; serviceClassMask <= 512; serviceClassMask <<= 1) {
                    if (serviceClassMask != 256) {
                        if (isAllCfDisabled) {
                            if (serviceClassToCFString(serviceClassMask) != null) {
                                String getServiceName = serviceClassToCFString(serviceClassMask).toString();
                                if (getServiceName != null) {
                                    sb.append(getServiceName);
                                    sb.append(" : ");
                                    sb.append(this.mContext.getText(134545422));
                                    sb.append("\n");
                                }
                            } else {
                                Rlog.e(LOG_TAG, "[GsmMmiCode] " + serviceClassMask + " service returns null");
                            }
                        } else {
                            int s2 = infos.length;
                            for (int i2 = 0; i2 < s2; i2++) {
                                if ((infos[i2].serviceClass & serviceClassMask) != 0) {
                                    if (infos[i2].status == 1) {
                                        tb.append(makeCFQueryResultMessage(infos[i2], serviceClassMask));
                                        tb.append((CharSequence) "\n");
                                    } else if (serviceClassToCFString(serviceClassMask) != null) {
                                        String getServiceName1 = serviceClassToCFString(serviceClassMask).toString();
                                        sb.append(getServiceName1);
                                        sb.append(" : ");
                                        sb.append(this.mContext.getText(134545422));
                                        sb.append("\n");
                                    }
                                }
                            }
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

    private void onQueryComplete(AsyncResult ar) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        if (ar.exception != null) {
            this.mState = MmiCode.State.FAILED;
            if (ar.exception instanceof CommandException) {
                CommandException.Error err = ((CommandException) ar.exception).getCommandError();
                if (err == CommandException.Error.CALL_BARRED) {
                    sb.append(this.mContext.getText(134545416));
                } else if (err == CommandException.Error.FDN_CHECK_FAILURE) {
                    sb.append(this.mContext.getText(134545415));
                } else {
                    sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_ICCID_IN_PROGRESS));
                }
            } else {
                sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_ICCID_IN_PROGRESS));
            }
        } else {
            int[] ints = (int[]) ar.result;
            if (ints.length != 0) {
                if (ints[0] == 0) {
                    sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_IMPI_IN_PROGRESS));
                } else if (this.mSc.equals(SC_WAIT)) {
                    sb.append(createQueryCallWaitingResultMessage(ints[1]));
                } else if (isServiceCodeCallBarring(this.mSc)) {
                    sb.append(createQueryCallBarringResultMessage(ints[0]));
                } else if (this.mSc.equals(SC_CNAP)) {
                    Rlog.d(LOG_TAG, "onQueryComplete_CNAP");
                    sb.append(createQueryCnapResultMessage(ints[1]));
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

    private CharSequence createQueryCnapResultMessage(int serviceClass) {
        Rlog.d(LOG_TAG, "createQueryCnapResultMessage_CNAP");
        StringBuilder sb = new StringBuilder(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_IMPI_ERROR));
        for (int classMask = 1; classMask <= 512; classMask <<= 1) {
            if ((classMask & serviceClass) != 0) {
                sb.append("\n");
                sb.append(serviceClassToCFString(classMask & serviceClass));
            }
        }
        Rlog.d(LOG_TAG, "CNAP_sb = " + ((Object) sb));
        return sb;
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

    private CharSequence createQueryCallBarringResultMessage(int serviceClass) {
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
    public String toString() {
        StringBuilder sb = new StringBuilder("GsmMmiCode {");
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

    public static GsmMmiCode newNetworkInitiatedUssdError(String ussdMessage, boolean isUssdRequest, GsmCdmaPhone phone, UiccCardApplication app) {
        GsmMmiCode ret = new GsmMmiCode(phone, app);
        ret.mMessage = ret.mContext.getText(R.string.PERSOSUBSTATE_SIM_ICCID_IN_PROGRESS);
        ret.mIsUssdRequest = isUssdRequest;
        ret.mState = MmiCode.State.FAILED;
        return ret;
    }

    private boolean isValidPin(String address) {
        int count = address.length();
        for (int i = 0; i < count; i++) {
            if (address.charAt(i) < '0' || address.charAt(i) > '9') {
                return false;
            }
        }
        return true;
    }

    private void onGetColrComplete(AsyncResult ar) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        if (ar.exception != null) {
            this.mState = MmiCode.State.FAILED;
            if (ar.exception instanceof CommandException) {
                CommandException.Error err = ((CommandException) ar.exception).getCommandError();
                if (err == CommandException.Error.CALL_BARRED) {
                    sb.append(this.mContext.getText(134545416));
                } else if (err == CommandException.Error.FDN_CHECK_FAILURE) {
                    sb.append(this.mContext.getText(134545415));
                } else {
                    sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_ICCID_IN_PROGRESS));
                }
            } else {
                sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_ICCID_IN_PROGRESS));
            }
        } else {
            int[] colrArgs = (int[]) ar.result;
            switch (colrArgs[0]) {
                case 0:
                    sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_SIM_PUK_ERROR));
                    this.mState = MmiCode.State.COMPLETE;
                    break;
                case 1:
                    sb.append(this.mContext.getText(134545420));
                    this.mState = MmiCode.State.COMPLETE;
                    break;
                case 2:
                    sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_ICCID_IN_PROGRESS));
                    this.mState = MmiCode.State.FAILED;
                    break;
            }
        }
        this.mMessage = sb;
        this.mPhone.onMMIDone(this);
    }

    private void onGetColpComplete(AsyncResult ar) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        if (ar.exception != null) {
            this.mState = MmiCode.State.FAILED;
            if (ar.exception instanceof CommandException) {
                CommandException.Error err = ((CommandException) ar.exception).getCommandError();
                if (err == CommandException.Error.CALL_BARRED) {
                    sb.append(this.mContext.getText(134545416));
                } else if (err == CommandException.Error.FDN_CHECK_FAILURE) {
                    sb.append(this.mContext.getText(134545415));
                } else {
                    sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_ICCID_IN_PROGRESS));
                }
            } else {
                sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_ICCID_IN_PROGRESS));
            }
        } else {
            int[] colpArgs = (int[]) ar.result;
            switch (colpArgs[1]) {
                case 0:
                    sb.append(this.mContext.getText(R.string.PERSOSUBSTATE_SIM_SIM_PUK_ERROR));
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

    public static boolean isUtMmiCode(String dialString, GsmCdmaPhone dialPhone, UiccCardApplication iccApp) {
        GsmMmiCode mmi = newFromDialString(dialString, dialPhone, iccApp);
        return (mmi == null || mmi.isTemporaryModeCLIR() || mmi.isShortCode() || mmi.mDialingNumber != null || mmi.mSc == null || (!mmi.mSc.equals(SC_CLIP) && !mmi.mSc.equals(SC_CLIR) && !mmi.mSc.equals(SC_COLP) && !mmi.mSc.equals(SC_COLR) && !isServiceCodeCallForwarding(mmi.mSc) && !isServiceCodeCallBarring(mmi.mSc) && !mmi.mSc.equals(SC_WAIT))) ? false : true;
    }
}
