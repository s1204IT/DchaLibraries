package com.mediatek.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.Preference;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.android.phone.GsmUmtsAdditionalCallOptions;
import com.android.phone.GsmUmtsCallBarringOptions;
import com.android.phone.GsmUmtsCallForwardOptions;
import com.android.phone.PhoneGlobals;
import com.android.phone.PhoneUtils;
import com.android.phone.R;
import com.android.phone.SubscriptionInfoHelper;
import com.android.phone.settings.SettingsConstants;
import com.mediatek.ims.internal.MtkImsManager;
import com.mediatek.internal.telephony.IMtkTelephonyEx;
import com.mediatek.internal.telephony.MtkSubscriptionManager;
import com.mediatek.settings.cdma.CdmaCallWaitingUtOptions;
import com.mediatek.settings.cdma.TelephonyUtilsEx;
import com.mediatek.telephony.MtkTelephonyManagerEx;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CallSettingUtils {
    private static final boolean DBGLOG;
    private static final String[] PROPERTY_SIM_PIN2_RETRY;
    private static final String[] PROPERTY_SIM_PUK2_RETRY;
    private static final boolean USR_BUILD = TextUtils.equals(Build.TYPE, "user");
    private static final Map<OPID, List> mOPMap;

    public enum DialogType {
        NONE,
        DATA_OPEN,
        DATA_TRAFFIC,
        DATA_ROAMING
    }

    public enum OPID {
        OP01,
        OP02,
        OP09,
        OP12,
        OP117
    }

    static {
        DBGLOG = SystemProperties.getInt("persist.vendor.log.tel_dbg", 0) == 1;
        mOPMap = new HashMap<OPID, List>() {
            {
                put(OPID.OP01, Arrays.asList("46000", "46002", "46004", "46007", "46008"));
                put(OPID.OP02, Arrays.asList("46001", "46006", "46009", "45407"));
                put(OPID.OP09, Arrays.asList("45502", "46003", "46011", "46012", "45507"));
                put(OPID.OP12, Arrays.asList("311480"));
                put(OPID.OP117, Arrays.asList("51009", "51028"));
            }
        };
        PROPERTY_SIM_PIN2_RETRY = new String[]{"vendor.gsm.sim.retry.pin2", "vendor.gsm.sim.retry.pin2.2", "vendor.gsm.sim.retry.pin2.3", "vendor.gsm.sim.retry.pin2.4"};
        PROPERTY_SIM_PUK2_RETRY = new String[]{"vendor.gsm.sim.retry.puk2", "vendor.gsm.sim.retry.puk2.2", "vendor.gsm.sim.retry.puk2.3", "vendor.gsm.sim.retry.puk2.4"};
    }

    public static boolean isOperator(int i, OPID opid) {
        if (mOPMap.get(opid).contains(TelephonyManager.getDefault().getSimOperator(i))) {
            return true;
        }
        return false;
    }

    public static boolean isOperator(String str, OPID opid) {
        if (mOPMap.get(opid).contains(str)) {
            return true;
        }
        return false;
    }

    public static boolean isCmccOrCuCard(int i) {
        String simOperator = TelephonyManager.getDefault().getSimOperator(i);
        return isOperator(simOperator, OPID.OP01) || isOperator(simOperator, OPID.OP02);
    }

    public static boolean isCtVolte() {
        boolean z;
        String str = SystemProperties.get("persist.vendor.mtk_ct_volte_support");
        if (str.equals(SettingsConstants.DUA_VAL_ON) || str.equals("2")) {
            z = true;
        } else {
            z = false;
        }
        log("isCtVolte: " + z + " feature: " + str);
        return z;
    }

    public static boolean isCtVolteMix() {
        boolean zEquals = SystemProperties.get("persist.vendor.mtk_ct_volte_support").equals("2");
        log("isCtVolteMixEnabled " + zEquals);
        return zEquals;
    }

    public static boolean isUtPreferCdmaSim(int i) {
        String simOperator = TelephonyManager.getDefault().getSimOperator(i);
        return (isCtVolte() && isOperator(simOperator, OPID.OP09) && TelephonyUtilsEx.isCdma4gCard(i)) || isOperator(simOperator, OPID.OP117);
    }

    public static boolean isCtVolteMix4gSim(int i) {
        return isCtVolteMix() && isOperator(i, OPID.OP09) && TelephonyUtilsEx.isCdma4gCard(i);
    }

    public static boolean isCtVolteNone4gSim(int i) {
        return isCtVolte() && isOperator(i, OPID.OP09) && !TelephonyUtilsEx.isCdma4gCard(i);
    }

    public static boolean isUtSupport(int i) {
        return (isCmccOrCuCard(i) && TelephonyUtils.isUSIMCard(PhoneGlobals.getInstance().getApplicationContext(), i)) || isUtPreferCdmaSim(i);
    }

    public static boolean isUtPreferOnlyByCdmaSim(int i) {
        return isCtVolte() && isOperator(i, OPID.OP09) && TelephonyUtilsEx.isCdma4gCard(i);
    }

    public static boolean isUtPreferByCdmaSimAndImsOn(Context context, int i, boolean z, boolean z2) {
        if (z) {
            if (!isOperator(i, OPID.OP117) || !z2) {
                return false;
            }
        } else if (!isOperator(i, OPID.OP117) || !TelephonyUtils.isImsServiceAvailable(context, i)) {
            return false;
        }
        return true;
    }

    public static boolean isMtkDualMicSupport() {
        AudioManager audioManager = (AudioManager) PhoneGlobals.getInstance().getSystemService("audio");
        if (audioManager != null) {
            String parameters = audioManager.getParameters("MTK_DUAL_MIC_SUPPORT");
            log("isMtkDualMicSupport: " + parameters);
            if (parameters.equalsIgnoreCase("MTK_DUAL_MIC_SUPPORT=true")) {
                return true;
            }
            return false;
        }
        return false;
    }

    public static void setDualMicMode(String str) {
        Context applicationContext = PhoneGlobals.getInstance().getApplicationContext();
        if (applicationContext == null) {
            return;
        }
        ((AudioManager) applicationContext.getSystemService("audio")).setParameters("Enable_Dual_Mic_Setting=" + str);
    }

    public static boolean isDualMicModeEnabled() {
        AudioManager audioManager;
        Context applicationContext = PhoneGlobals.getInstance().getApplicationContext();
        if (applicationContext != null && (audioManager = (AudioManager) applicationContext.getSystemService("audio")) != null) {
            String parameters = audioManager.getParameters("Get_Dual_Mic_Setting");
            log("getDualMicMode(): state: " + parameters);
            if (parameters.equalsIgnoreCase("Get_Dual_Mic_Setting=1")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHacSupport() {
        AudioManager audioManager;
        Context applicationContext = PhoneGlobals.getInstance().getApplicationContext();
        if (applicationContext == null || (audioManager = (AudioManager) applicationContext.getSystemService("audio")) == null) {
            return false;
        }
        String parameters = audioManager.getParameters("GET_HAC_SUPPORT");
        log("hac support: " + parameters);
        return "GET_HAC_SUPPORT=1".equals(parameters);
    }

    private static void log(String str) {
        Log.d("CallSettingUtils", str);
    }

    public static boolean isPhoneBookReady(Context context, int i) {
        boolean zIsPhbReady;
        try {
            zIsPhbReady = IMtkTelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx")).isPhbReady(i);
        } catch (RemoteException e) {
            e = e;
            zIsPhbReady = false;
        }
        try {
            Log.d("CallSettingUtils", "isPhoneBookReady:" + zIsPhbReady + ", subId:" + i);
        } catch (RemoteException e2) {
            e = e2;
            Log.e("CallSettingUtils", "isPhoneBookReady catch exception:");
            e.printStackTrace();
        }
        if (!zIsPhbReady) {
            Toast.makeText(context, context.getString(R.string.fdn_phone_book_busy), 0).show();
        }
        return zIsPhbReady;
    }

    public static boolean isMobileDataEnabled(int i) {
        if (PhoneUtils.isValidSubId(i)) {
            boolean zIsUserDataEnabled = PhoneUtils.getPhoneUsingSubId(i).isUserDataEnabled();
            log("isMobileDataEnabled: " + zIsUserDataEnabled);
            return zIsUserDataEnabled;
        }
        log("isMobileDataEnabled invalid subId: " + i);
        return false;
    }

    public static DialogType getDialogTipsType(Context context, int i) {
        DialogType dialogType = DialogType.NONE;
        if (!PhoneUtils.isValidSubId(i)) {
            log("getDialogTipsType invalid subId: " + i);
            return dialogType;
        }
        if (!PhoneGlobals.getInstance().getCarrierConfigForSubId(i).getBoolean("mtk_show_open_mobile_data_dialog_bool")) {
            return dialogType;
        }
        if (TelephonyUtils.isImsServiceAvailable(context, i) || isUtSupport(i)) {
            log("getDialogTipsType: ss query need mobile data connection!");
            if (((TelephonyManager) context.getSystemService("phone")).isWifiCallingAvailable() && !isCmccOrCuCard(i)) {
                return dialogType;
            }
            boolean zIsMobileDataEnabled = isMobileDataEnabled(i);
            boolean z = (i == SubscriptionManager.getDefaultDataSubscriptionId() && zIsMobileDataEnabled) ? false : true;
            boolean zIsSupportMims = MtkImsManager.isSupportMims();
            if (!zIsMobileDataEnabled && !zIsSupportMims) {
                dialogType = DialogType.DATA_OPEN;
            } else if (z && zIsSupportMims) {
                dialogType = DialogType.DATA_TRAFFIC;
            } else if (!MtkTelephonyManagerEx.getDefault().isInHomeNetwork(i) && isCmccOrCuCard(i) && !PhoneUtils.getPhoneUsingSubId(i).getDataRoamingEnabled()) {
                log("getDialogTipsType: network is roaming!");
                dialogType = DialogType.DATA_ROAMING;
            }
        }
        log("getDialogTipsType subId: " + i + ",result: " + dialogType);
        return dialogType;
    }

    public static void showDialogTips(Context context, int i, DialogType dialogType, Preference preference) {
        Intent intent;
        if (dialogType == DialogType.DATA_TRAFFIC) {
            log("showDialogTips preference: " + preference);
            if (preference != null) {
                if (preference.getKey().equals("call_forwarding_key") || preference.getKey().equals("button_cf_expand_key")) {
                    intent = new Intent(context, (Class<?>) GsmUmtsCallForwardOptions.class);
                } else if (preference.getKey().equals("call_barring_key")) {
                    intent = new Intent(context, (Class<?>) GsmUmtsCallBarringOptions.class);
                } else if (preference.getKey().equals("additional_gsm_call_settings_key")) {
                    intent = new Intent(context, (Class<?>) GsmUmtsAdditionalCallOptions.class);
                } else {
                    intent = new Intent(context, (Class<?>) CdmaCallWaitingUtOptions.class);
                }
                SubscriptionInfoHelper.addExtrasToIntent(intent, MtkSubscriptionManager.getSubInfo((String) null, i));
            } else {
                intent = null;
            }
        }
        MobileDataDialogFragment.show(intent, dialogType, i, ((Activity) context).getFragmentManager());
    }

    private static boolean isOP09CSupport() {
        boolean z = "OP09".equals(SystemProperties.get("persist.vendor.operator.optr", "")) && "SEGC".equals(SystemProperties.get("persist.vendor.operator.seg", ""));
        Log.d("CallSettingUtils", "isOP09CSupport: " + z);
        return z;
    }

    public static boolean shouldShowDataTrafficDialog(int i) {
        return MtkImsManager.isSupportMims() && isOP09CSupport() && !isMobileDataEnabled(i);
    }

    public static int getPin2RetryNumber(int i) {
        String str;
        if (!PhoneUtils.isValidSubId(i)) {
            log("getPin2RetryNumber invalid subId: " + i);
            return -1;
        }
        int slotIndex = SubscriptionManager.getSlotIndex(i);
        log("getPin2RetryNumber subId: " + i + ",Slot: " + slotIndex);
        try {
            if (!TelephonyUtils.isGeminiProject()) {
                str = PROPERTY_SIM_PIN2_RETRY[0];
            } else if (slotIndex < PROPERTY_SIM_PIN2_RETRY.length) {
                str = PROPERTY_SIM_PIN2_RETRY[slotIndex];
            } else {
                Log.w("CallSettingUtils", "getPin2RetryNumber: Error happened!");
                str = PROPERTY_SIM_PIN2_RETRY[0];
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            log("getPin2RetryNumber exception: " + e.getMessage());
            str = PROPERTY_SIM_PIN2_RETRY[0];
        }
        return SystemProperties.getInt(str, -1);
    }

    public static String getPinPuk2RetryLeftNumTips(Context context, int i, boolean z) {
        int puk2RetryNumber;
        if (!PhoneUtils.isValidSubId(i)) {
            log("getPinPuk2RetryLeftNumTips inValid SubId: " + i);
            return " ";
        }
        if (z) {
            puk2RetryNumber = getPin2RetryNumber(i);
        } else {
            puk2RetryNumber = getPuk2RetryNumber(i);
        }
        log("getPinPuk2RetryLeftNumTips retry count: " + puk2RetryNumber + ",isPin: " + z);
        if (puk2RetryNumber == -1) {
            return " ";
        }
        return context.getString(R.string.retries_left, Integer.valueOf(puk2RetryNumber));
    }

    public static int getPuk2RetryNumber(int i) {
        String str;
        if (!PhoneUtils.isValidSubId(i)) {
            log("getPuk2RetryNumber inValid SubId: " + i);
            return -1;
        }
        int slotIndex = SubscriptionManager.getSlotIndex(i);
        log("getPuk2RetryNumber subId: " + i + ",Slot: " + slotIndex);
        if (TelephonyUtils.isGeminiProject()) {
            if (slotIndex < PROPERTY_SIM_PIN2_RETRY.length) {
                str = PROPERTY_SIM_PUK2_RETRY[slotIndex];
            } else {
                Log.w("CallSettingUtils", "getPuk2RetryNumber: Error happened!");
                str = PROPERTY_SIM_PUK2_RETRY[0];
            }
        } else {
            str = PROPERTY_SIM_PUK2_RETRY[0];
        }
        return SystemProperties.getInt(str, -1);
    }

    public static void sensitiveLog(String str, String str2, String str3) {
        if (!USR_BUILD || DBGLOG) {
            Log.d(str, str2 + str3);
            return;
        }
        Log.d(str, str2 + toSafeString(str3));
    }

    public static String toSafeString(String str) {
        if (str == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int length = str.length();
        for (int i = 0; i < length; i++) {
            sb.append('x');
        }
        return sb.toString();
    }
}
