package com.mediatek.internal.telephony.dataconnection;

import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.util.SparseArray;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.dataconnection.DcFailCause;
import java.util.EnumSet;
import java.util.HashMap;

public class DcFailCauseManager {
    private static final int ACTIVATION_REJECT_GGSN = 30;
    private static final int ACTIVATION_REJECT_UNSPECIFIED = 31;
    private static final int[] CC33_FAIL_CAUSE_TABLE;
    public static final boolean DBG = true;
    public static final String DEFAULT_DATA_RETRY_CONFIG_OP12 = "default_randomization=2000,5000,10000,20000,40000,80000,120000,180000,240000,240000,240000,240000,240000,240000,320000,640000,1280000,1800000";
    public static final String DEFAULT_DATA_RETRY_CONFIG_OP19 = "max_retries=10, 720000,1440000,2880000,5760000,11520000,23040000,23040000,23040000,23040000,46080000";
    private static final int INSUFFICIENT_RESOURCES = 26;
    public static final String LOG_TAG = "DcFcMgr";
    private static final int MISSING_UNKNOWN_APN = 27;
    public static boolean MTK_CC33_SUPPORT = false;
    private static final int NETWORK_FAILURE = 38;
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
    private static final String[][] specificPLMN = {new String[]{"50501"}, new String[]{"311480"}, new String[]{"732101"}};
    private Phone mPhone;

    static {
        MTK_CC33_SUPPORT = SystemProperties.getInt("persist.vendor.data.cc33.support", 0) == 1;
        CC33_FAIL_CAUSE_TABLE = new int[]{29, 33};
        OP19_FAIL_CAUSE_TABLE = new int[]{26, 27, 28, 30, 31, 34, 38, PDP_FAIL_FALLBACK_RETRY};
    }

    public enum Operator {
        NONE(-1),
        OP19(0),
        OP12(1),
        OP120(2);

        private static final HashMap<Integer, Operator> lookup = new HashMap<>();
        private int value;

        static {
            for (Operator operator : EnumSet.allOf(Operator.class)) {
                lookup.put(Integer.valueOf(operator.getValue()), operator);
            }
        }

        Operator(int i) {
            this.value = i;
        }

        public int getValue() {
            return this.value;
        }

        public static Operator get(int i) {
            return lookup.get(Integer.valueOf(i));
        }
    }

    private enum retryConfigForDefault {
        maxRetryCount(1),
        retryTime(0),
        randomizationTime(0);

        private final int value;

        retryConfigForDefault(int i) {
            this.value = i;
        }

        public int getValue() {
            return this.value;
        }
    }

    public enum retryConfigForCC33 {
        maxRetryCount(2),
        retryTime(45000),
        randomizationTime(0);

        private final int value;

        retryConfigForCC33(int i) {
            this.value = i;
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
            DcFailCauseManager dcFailCauseManager = sDcFailCauseManager.get(phoneId);
            if (dcFailCauseManager == null) {
                Rlog.d(LOG_TAG, "For phoneId:" + phoneId + " doesn't exist, create it");
                DcFailCauseManager dcFailCauseManager2 = new DcFailCauseManager(phone);
                sDcFailCauseManager.put(phoneId, dcFailCauseManager2);
                return dcFailCauseManager2;
            }
            return dcFailCauseManager;
        }
        Rlog.e(LOG_TAG, "Can't get phone to init!");
        return null;
    }

    private DcFailCauseManager(Phone phone) {
        log("DcFcMgr.constructor");
        this.mPhone = phone;
    }

    public void dispose() {
        log("DcFcMgr.dispose");
        sDcFailCauseManager.remove(this.mPhone.getPhoneId());
    }

    public long getSuggestedRetryDelayByOp(DcFailCause dcFailCause) {
        long value = -2;
        if (AnonymousClass1.$SwitchMap$com$mediatek$internal$telephony$dataconnection$DcFailCauseManager$Operator[getSpecificNetworkOperator().ordinal()] == 1) {
            for (int i : CC33_FAIL_CAUSE_TABLE) {
                DcFailCause dcFailCauseFromInt = DcFailCause.fromInt(i);
                if (MTK_CC33_SUPPORT && dcFailCause.equals(dcFailCauseFromInt)) {
                    value = retryConfigForCC33.retryTime.getValue();
                }
            }
        }
        return value;
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$mediatek$internal$telephony$dataconnection$DcFailCauseManager$Operator = new int[Operator.values().length];

        static {
            try {
                $SwitchMap$com$mediatek$internal$telephony$dataconnection$DcFailCauseManager$Operator[Operator.OP120.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$mediatek$internal$telephony$dataconnection$DcFailCauseManager$Operator[Operator.OP19.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    public long getRetryTimeByIndex(int i, Operator operator) {
        String str;
        if (AnonymousClass1.$SwitchMap$com$mediatek$internal$telephony$dataconnection$DcFailCauseManager$Operator[operator.ordinal()] == 2) {
            str = DEFAULT_DATA_RETRY_CONFIG_OP19;
        } else {
            str = null;
        }
        if (str != null) {
            try {
                return Long.parseLong(str.split(",")[i]);
            } catch (IndexOutOfBoundsException e) {
                loge("get retry time by index fail");
                e.printStackTrace();
            }
        }
        return -1L;
    }

    public boolean isSpecificNetworkAndSimOperator(Operator operator) {
        if (operator != null) {
            return operator == getSpecificNetworkOperator() && operator == getSpecificSimOperator();
        }
        loge("op is null, return false!");
        return false;
    }

    public boolean isSpecificNetworkOperator(Operator operator) {
        if (operator != null) {
            return operator == getSpecificNetworkOperator();
        }
        loge("op is null, return false!");
        return false;
    }

    public boolean isNetworkOperatorForCC33() {
        return AnonymousClass1.$SwitchMap$com$mediatek$internal$telephony$dataconnection$DcFailCauseManager$Operator[getSpecificNetworkOperator().ordinal()] == 1;
    }

    private Operator getSpecificNetworkOperator() {
        String networkOperatorForPhone;
        Exception e;
        boolean z;
        Operator operator = Operator.NONE;
        try {
            networkOperatorForPhone = TelephonyManager.getDefault().getNetworkOperatorForPhone(this.mPhone.getPhoneId());
        } catch (Exception e2) {
            networkOperatorForPhone = "";
            e = e2;
        }
        try {
            log("Check PLMN=" + networkOperatorForPhone);
        } catch (Exception e3) {
            e = e3;
            loge("get plmn fail");
            e.printStackTrace();
        }
        for (int i = 0; i < specificPLMN.length; i++) {
            int i2 = 0;
            while (true) {
                if (i2 < specificPLMN[i].length) {
                    if (!networkOperatorForPhone.equals(specificPLMN[i][i2])) {
                        i2++;
                    } else {
                        z = true;
                        break;
                    }
                } else {
                    z = false;
                    break;
                }
            }
            if (z) {
                Operator operator2 = Operator.get(i);
                log("Serving in specific network op=" + operator2 + "(" + i + ")");
                return operator2;
            }
        }
        return operator;
    }

    private Operator getSpecificSimOperator() {
        String simOperatorNumericForPhone;
        Exception e;
        boolean z;
        Operator operator = Operator.NONE;
        try {
            simOperatorNumericForPhone = TelephonyManager.getDefault().getSimOperatorNumericForPhone(this.mPhone.getPhoneId());
        } catch (Exception e2) {
            simOperatorNumericForPhone = "";
            e = e2;
        }
        try {
            log("Check HPLMN=" + simOperatorNumericForPhone);
        } catch (Exception e3) {
            e = e3;
            loge("get hplmn fail");
            e.printStackTrace();
        }
        for (int i = 0; i < specificPLMN.length; i++) {
            int i2 = 0;
            while (true) {
                if (i2 < specificPLMN[i].length) {
                    if (!simOperatorNumericForPhone.equals(specificPLMN[i][i2])) {
                        i2++;
                    } else {
                        z = true;
                        break;
                    }
                } else {
                    z = false;
                    break;
                }
            }
            if (z) {
                Operator operator2 = Operator.get(i);
                log("Serving in specific sim op=" + operator2 + "(" + i + ")");
                return operator2;
            }
        }
        return operator;
    }

    public String toString() {
        return "sDcFailCauseManager: " + sDcFailCauseManager;
    }

    private void loge(String str) {
        Rlog.e(LOG_TAG, str);
    }

    private void log(String str) {
        Rlog.d(LOG_TAG, str);
    }
}
