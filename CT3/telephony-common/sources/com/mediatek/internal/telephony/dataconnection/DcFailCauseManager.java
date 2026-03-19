package com.mediatek.internal.telephony.dataconnection;

import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.SparseArray;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneInternalInterface;
import com.android.internal.telephony.dataconnection.DcFailCause;
import com.mediatek.common.MPlugin;
import com.mediatek.common.telephony.IGsmDCTExt;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.util.EnumSet;
import java.util.HashMap;

public class DcFailCauseManager {

    private static final int[] f41x1a230c8d = null;
    private static final int ACTIVATION_REJECT_GGSN = 30;
    private static final int ACTIVATION_REJECT_UNSPECIFIED = 31;
    public static final boolean DBG = true;
    public static final String DEFAULT_DATA_RETRY_CONFIG_OP12 = "default_randomization=2000,5000,10000,20000,40000,80000,120000,180000,240000,240000,240000,240000,240000,240000,320000,640000,1280000,1800000";
    public static final String DEFAULT_DATA_RETRY_CONFIG_OP19 = "max_retries=10, 720000,1440000,2880000,5760000,11520000,23040000,23040000,23040000,23040000,46080000";
    private static final int INSUFFICIENT_RESOURCES = 26;
    public static final String LOG_TAG = "DcFcMgr";
    private static final int MISSING_UNKNOWN_APN = 27;
    public static boolean MTK_CC33_SUPPORT = false;
    private static final int NETWORK_FAILURE = 38;
    private static final int[] OP112_FAIL_CAUSE_TABLE;
    private static final int[] OP19_FAIL_CAUSE_TABLE;
    private static final int OPERATOR_DETERMINED_BARRING = 8;
    private static final int PDP_FAIL_FALLBACK_RETRY = -1000;
    private static final int SERVICE_OPTION_NOT_SUBSCRIBED = 33;
    private static final int SERVICE_OPTION_NOT_SUPPORTED = 32;
    private static final int SERVICE_OPTION_OUT_OF_ORDER = 34;
    private static final int UNKNOWN_PDP_ADDRESS_TYPE = 28;
    private static final int USER_AUTHENTICATION = 29;
    public static final boolean VDBG = false;
    private static final SparseArray<DcFailCauseManager> sDcFailCauseManager = new SparseArray<>();
    private static final String[][] specificPLMN = {new String[]{"33402", "334020"}, new String[]{"50501"}, new String[]{"311480"}};
    private IGsmDCTExt mGsmDCTExt;
    private boolean mIsBsp = SystemProperties.getBoolean("ro.mtk_bsp_package", false);
    private Phone mPhone;

    private static int[] m615x62355c69() {
        if (f41x1a230c8d != null) {
            return f41x1a230c8d;
        }
        int[] iArr = new int[Operator.valuesCustom().length];
        try {
            iArr[Operator.NONE.ordinal()] = 3;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[Operator.OP112.ordinal()] = 1;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[Operator.OP12.ordinal()] = 4;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[Operator.OP19.ordinal()] = 2;
        } catch (NoSuchFieldError e4) {
        }
        f41x1a230c8d = iArr;
        return iArr;
    }

    static {
        MTK_CC33_SUPPORT = SystemProperties.getInt("persist.data.cc33.support", 0) == 1;
        OP112_FAIL_CAUSE_TABLE = new int[]{29, 33};
        OP19_FAIL_CAUSE_TABLE = new int[]{26, 27, 28, 30, 31, 34, 38, PDP_FAIL_FALLBACK_RETRY};
    }

    public enum Operator {
        NONE(-1),
        OP112(0),
        OP19(1),
        OP12(2);

        private static final HashMap<Integer, Operator> lookup = new HashMap<>();
        private int value;

        public static Operator[] valuesCustom() {
            return values();
        }

        static {
            for (Operator op : EnumSet.allOf(Operator.class)) {
                lookup.put(Integer.valueOf(op.getValue()), op);
            }
        }

        Operator(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }

        public static Operator get(int value) {
            return lookup.get(Integer.valueOf(value));
        }
    }

    private enum retryConfigForDefault {
        maxRetryCount(1),
        retryTime(0),
        randomizationTime(0);

        private final int value;

        public static retryConfigForDefault[] valuesCustom() {
            return values();
        }

        retryConfigForDefault(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    private enum retryConfigForOp112 {
        maxRetryCount(2),
        retryTime(45000),
        randomizationTime(0);

        private final int value;

        public static retryConfigForOp112[] valuesCustom() {
            return values();
        }

        retryConfigForOp112(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    public static DcFailCauseManager getInstance(Phone phone) {
        if (phone != null) {
            int phoneId = phone.getPhoneId();
            if (phoneId < 0) {
                Rlog.e(LOG_TAG, "PhoneId[" + phoneId + "] is invalid!");
                return null;
            }
            DcFailCauseManager dcFcMgr = sDcFailCauseManager.get(phoneId);
            if (dcFcMgr == null) {
                Rlog.d(LOG_TAG, "For phoneId:" + phoneId + " doesn't exist, create it");
                DcFailCauseManager dcFcMgr2 = new DcFailCauseManager(phone);
                sDcFailCauseManager.put(phoneId, dcFcMgr2);
                return dcFcMgr2;
            }
            return dcFcMgr;
        }
        Rlog.e(LOG_TAG, "Can't get phone to init!");
        return null;
    }

    private DcFailCauseManager(Phone phone) {
        log("DcFcMgr.constructor");
        this.mPhone = phone;
        if (!createGsmDCTExt(this.mPhone)) {
            return;
        }
        log("mGsmDCTExt init success");
    }

    public void dispose() {
        log("DcFcMgr.dispose");
        sDcFailCauseManager.remove(this.mPhone.getPhoneId());
    }

    private boolean createGsmDCTExt(Phone phone) {
        if (this.mIsBsp) {
            return false;
        }
        try {
            this.mGsmDCTExt = (IGsmDCTExt) MPlugin.createInstance(IGsmDCTExt.class.getName(), phone.getContext());
            log("mGsmDCTExt init on phone[" + phone.getPhoneId() + "]");
            return true;
        } catch (Exception e) {
            loge("mGsmDCTExt init fail");
            e.printStackTrace();
            return false;
        }
    }

    public long getSuggestedRetryDelayByOp(DcFailCause cause) {
        long suggestedRetryTime = -2;
        Operator operator = getSpecificNetworkOperator();
        switch (m615x62355c69()[operator.ordinal()]) {
            case 1:
                for (int tempCause : OP112_FAIL_CAUSE_TABLE) {
                    DcFailCause dcFailCause = DcFailCause.fromInt(tempCause);
                    if (MTK_CC33_SUPPORT && cause.equals(dcFailCause)) {
                        suggestedRetryTime = retryConfigForOp112.retryTime.getValue();
                    }
                }
            default:
                return suggestedRetryTime;
        }
    }

    public long getRetryTimeByIndex(int idx, Operator op) {
        String configStr = null;
        switch (m615x62355c69()[op.ordinal()]) {
            case 2:
                configStr = DEFAULT_DATA_RETRY_CONFIG_OP19;
                break;
        }
        if (configStr == null) {
            return -1L;
        }
        String[] strArray = configStr.split(",");
        try {
            long retryTime = Long.parseLong(strArray[idx]);
            return retryTime;
        } catch (IndexOutOfBoundsException e) {
            loge("get retry time by index fail");
            e.printStackTrace();
            return -1L;
        }
    }

    public boolean isPermanentFailByOp(DcFailCause cause, Operator op) {
        int i = 0;
        boolean isPermanent = true;
        switch (m615x62355c69()[op.ordinal()]) {
            case 1:
                int[] iArr = OP112_FAIL_CAUSE_TABLE;
                int length = iArr.length;
                while (i < length) {
                    int tempCause = iArr[i];
                    DcFailCause dcFailCause = DcFailCause.fromInt(tempCause);
                    if (MTK_CC33_SUPPORT && cause.equals(dcFailCause)) {
                        isPermanent = false;
                    }
                    i++;
                }
                return isPermanent;
            case 2:
                if (isSpecificNetworkAndSimOperator(op)) {
                    int[] iArr2 = OP19_FAIL_CAUSE_TABLE;
                    int length2 = iArr2.length;
                    while (i < length2) {
                        int tempCause2 = iArr2[i];
                        DcFailCause dcFailCause2 = DcFailCause.fromInt(tempCause2);
                        if (cause.equals(dcFailCause2)) {
                            isPermanent = false;
                        }
                        i++;
                    }
                }
                return isPermanent;
            default:
                return isPermanent;
        }
    }

    public boolean canIgnoredReason(String reason) {
        Operator operator = getSpecificNetworkOperator();
        switch (m615x62355c69()[operator.ordinal()]) {
            case 1:
                if ((TextUtils.equals(reason, PhoneInternalInterface.REASON_DATA_ATTACHED) || TextUtils.equals(reason, DcFailCause.LOST_CONNECTION.toString())) && MTK_CC33_SUPPORT) {
                }
                break;
        }
        return false;
    }

    public boolean isSpecificNetworkAndSimOperator(Operator op) {
        if (op == null) {
            loge("op is null, return false!");
            return false;
        }
        Operator networkOp = getSpecificNetworkOperator();
        Operator simOp = getSpecificSimOperator();
        return op == networkOp && op == simOp;
    }

    public boolean isSpecificNetworkOperator(Operator op) {
        if (op == null) {
            loge("op is null, return false!");
            return false;
        }
        Operator networkOp = getSpecificNetworkOperator();
        return op == networkOp;
    }

    private Operator getSpecificNetworkOperator() {
        Operator operator = Operator.NONE;
        String plmn = UsimPBMemInfo.STRING_NOT_SET;
        try {
            plmn = TelephonyManager.getDefault().getNetworkOperatorForPhone(this.mPhone.getPhoneId());
            log("Check PLMN=" + plmn);
        } catch (Exception e) {
            loge("get plmn fail");
            e.printStackTrace();
        }
        for (int i = 0; i < specificPLMN.length; i++) {
            boolean isServingInSpecificPlmn = false;
            int j = 0;
            while (true) {
                if (j >= specificPLMN[i].length) {
                    break;
                }
                if (!plmn.equals(specificPLMN[i][j])) {
                    j++;
                } else {
                    isServingInSpecificPlmn = true;
                    break;
                }
            }
            if (isServingInSpecificPlmn) {
                Operator operator2 = Operator.get(i);
                log("Serving in specific network op=" + operator2 + "(" + i + ")");
                return operator2;
            }
        }
        return operator;
    }

    private Operator getSpecificSimOperator() {
        Operator operator = Operator.NONE;
        String hplmn = UsimPBMemInfo.STRING_NOT_SET;
        try {
            hplmn = TelephonyManager.getDefault().getSimOperatorNumericForPhone(this.mPhone.getPhoneId());
            log("Check HPLMN=" + hplmn);
        } catch (Exception e) {
            loge("get hplmn fail");
            e.printStackTrace();
        }
        for (int i = 0; i < specificPLMN.length; i++) {
            boolean isServingInSpecificPlmn = false;
            int j = 0;
            while (true) {
                if (j >= specificPLMN[i].length) {
                    break;
                }
                if (!hplmn.equals(specificPLMN[i][j])) {
                    j++;
                } else {
                    isServingInSpecificPlmn = true;
                    break;
                }
            }
            if (isServingInSpecificPlmn) {
                Operator operator2 = Operator.get(i);
                log("Serving in specific sim op=" + operator2 + "(" + i + ")");
                return operator2;
            }
        }
        return operator;
    }

    public String toString() {
        String ret = "sDcFailCauseManager: " + sDcFailCauseManager;
        return ret;
    }

    private void loge(String s) {
        Rlog.e(LOG_TAG, s);
    }

    private void log(String s) {
        Rlog.d(LOG_TAG, s);
    }
}
