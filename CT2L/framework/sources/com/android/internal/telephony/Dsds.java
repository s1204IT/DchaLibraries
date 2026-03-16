package com.android.internal.telephony;

import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.AndroidRuntimeException;
import com.android.internal.R;
import com.android.internal.telephony.PhoneConstants;

public final class Dsds {
    private static final int BIDDING_PHONE = 0;
    private static final String KEY_BIDDING_PHONE = "persist.radio.oriented.operator";
    private static final int OPEN_MARKET_PHONE = 1;
    private static String LOG_TAG = "Dsds";
    private static String SIM2_MASTER_KEY = "persist.sys.sim2.master.enable";
    private static String DATA_ALLOW_SIM_KEY = "persist.sys.data_allow_sim";
    private static TelephonyManager sTm = TelephonyManager.getDefault();

    public enum MasterUiccPref {
        MASTER_UICC_PREFERRED,
        MASTER_UICC_NORMAL,
        MASTER_UICC_RESTRICTED
    }

    public static boolean isDualSimSolution() {
        int phoneCount = sTm.getPhoneCount();
        if (phoneCount > 2) {
            throw new AndroidRuntimeException("Only support Single SIM and Dual SIM");
        }
        return phoneCount == 2;
    }

    public static boolean isSingleSimSolution() {
        int phoneCount = sTm.getPhoneCount();
        if (phoneCount > 2) {
            throw new AndroidRuntimeException("Only support Single SIM and Dual SIM");
        }
        return phoneCount == 1;
    }

    public static PhoneConstants.SimId defaultSimId() {
        return (isDualSimSolution() && hasOnlyIcc2()) ? PhoneConstants.SimId.SIM2 : PhoneConstants.SimId.SIM1;
    }

    public static PhoneConstants.SimId defaultSimId(Context context) {
        if (isDualSimSolution() && hasOnlyIcc2()) {
            return PhoneConstants.SimId.SIM2;
        }
        if (isDualSimSolution() && hasTwoIcc()) {
            PhoneConstants.SimId simId = PhoneConstants.SimId.SIM1;
            if ((!isSimEnabled(context, PhoneConstants.SimId.SIM1.ordinal()) || isSim2Master()) && isSimEnabled(context, PhoneConstants.SimId.SIM2.ordinal())) {
                PhoneConstants.SimId simId2 = PhoneConstants.SimId.SIM2;
                return simId2;
            }
            return simId;
        }
        PhoneConstants.SimId simId3 = PhoneConstants.SimId.SIM1;
        return simId3;
    }

    public static boolean hasNoIcc() {
        return (hasIcc1() || hasIcc2()) ? false : true;
    }

    public static boolean hasIcc1() {
        return hasIcc(PhoneConstants.SimId.SIM1.ordinal());
    }

    public static boolean hasIcc2() {
        return hasIcc(PhoneConstants.SimId.SIM2.ordinal());
    }

    public static boolean hasIcc(int simId) {
        return sTm.hasIccCard(simId);
    }

    public static boolean hasTwoIcc() {
        return hasIcc1() && hasIcc2();
    }

    public static boolean hasOnlyIcc1() {
        return hasIcc1() && !hasIcc2();
    }

    public static boolean hasOnlyIcc2() {
        return hasIcc2() && !hasIcc1();
    }

    public static boolean hasOnlyOneIcc() {
        return hasOnlyIcc1() || hasOnlyIcc2();
    }

    public static boolean hasAtLeastOneIcc() {
        return hasIcc1() || hasIcc2();
    }

    public static boolean hasAtMostOneIcc() {
        return !hasTwoIcc();
    }

    public static boolean isSim2Master() {
        return SystemProperties.getBoolean(SIM2_MASTER_KEY, false);
    }

    public static void setInitialDataAllowSIM(int simId) {
        SystemProperties.set(DATA_ALLOW_SIM_KEY, String.valueOf(simId));
    }

    public static int getInitialDataAllowSIM() {
        return isDualSimSolution() ? SystemProperties.getInt(DATA_ALLOW_SIM_KEY, defaultSimId().ordinal()) : PhoneConstants.SimId.SIM1.ordinal();
    }

    public static int getRilDataAllowSIM() {
        return SystemProperties.getInt(DATA_ALLOW_SIM_KEY, PhoneConstants.SimId.SIM1.ordinal());
    }

    public static void enableSim(Context context, int simId, boolean enabled) {
        if (!SubscriptionManager.isValidPhoneId(simId)) {
            throw new AndroidRuntimeException("Invalid Phone ID");
        }
        String simEnableKey = simId == PhoneConstants.SimId.SIM1.ordinal() ? Settings.Global.ENABLE_SIM1 : Settings.Global.ENABLE_SIM2;
        Settings.Global.putInt(context.getContentResolver(), simEnableKey, enabled ? 1 : 0);
        Intent intent = new Intent(Intent.ACTION_SIM_ENABLE_CHANGED);
        intent.putExtra("phone", simId);
        context.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    public static boolean isSimEnabled(Context context, int simId) {
        if (!SubscriptionManager.isValidPhoneId(simId)) {
            throw new AndroidRuntimeException("Invalid Phone ID");
        }
        String simEnableKey = simId == PhoneConstants.SimId.SIM1.ordinal() ? Settings.Global.ENABLE_SIM1 : Settings.Global.ENABLE_SIM2;
        return Settings.Global.getInt(context.getContentResolver(), simEnableKey, 1) != 0;
    }

    private static boolean isMatchedOperator(Context context, int resId, String mccmnc) {
        String[] perferredOperators = context.getResources().getStringArray(resId);
        if (perferredOperators != null && !TextUtils.isEmpty(mccmnc)) {
            for (String operator : perferredOperators) {
                if (TextUtils.equals(mccmnc, operator)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String getOperatorNumeric(Context context, int phoneId) {
        TelephonyManager tm = TelephonyManager.from(context);
        int[] subId = SubscriptionManager.getSubId(phoneId);
        if (subId == null || !SubscriptionManager.isValidSubscriptionId(subId[0])) {
            return null;
        }
        return tm.getSimOperator(subId[0]);
    }

    public static MasterUiccPref getMasterUiccPref(Context context, int phoneId) {
        if (isBiddingPhone()) {
            String mccmnc = getOperatorNumeric(context, phoneId);
            if (isMatchedOperator(context, R.array.preferred_operator_numeric, mccmnc)) {
                return MasterUiccPref.MASTER_UICC_PREFERRED;
            }
            if (isMatchedOperator(context, R.array.restricted_operator_numeric, mccmnc)) {
                return MasterUiccPref.MASTER_UICC_RESTRICTED;
            }
        }
        return MasterUiccPref.MASTER_UICC_NORMAL;
    }

    public static boolean isBiddingPhone() {
        return SystemProperties.getInt(KEY_BIDDING_PHONE, 0) == 0;
    }

    public static int getMasterSubId() {
        boolean isSim2Master = SystemProperties.getBoolean(SIM2_MASTER_KEY, false);
        int masterSimId = isSim2Master ? 1 : 0;
        int[] subs = SubscriptionManager.getSubId(masterSimId);
        if (subs != null && SubscriptionManager.isValidSubscriptionId(subs[0])) {
            return subs[0];
        }
        return -1;
    }
}
