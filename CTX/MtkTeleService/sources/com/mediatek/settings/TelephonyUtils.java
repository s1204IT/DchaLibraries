package com.mediatek.settings;

import android.content.Context;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.phone.PhoneGlobals;
import com.android.phone.PhoneUtils;
import com.android.phone.settings.SettingsConstants;
import com.mediatek.ims.internal.MtkImsManager;
import com.mediatek.internal.telephony.MtkGsmCdmaPhone;
import com.mediatek.phone.PhoneFeatureConstants;
import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.settings.cdma.TelephonyUtilsEx;
import com.mediatek.telephony.MtkTelephonyManagerEx;
import java.util.List;

public class TelephonyUtils {
    public static boolean isUSIMCard(Context context, int i) {
        log("isUSIMCard()... subId = " + i);
        String iccCardType = MtkTelephonyManagerEx.getDefault().getIccCardType(i);
        log("isUSIMCard()... type = " + iccCardType);
        return "USIM".equals(iccCardType);
    }

    public static boolean isSimStateReady(int i) {
        boolean z = 5 == TelephonyManager.getDefault().getSimState(i);
        log("isSimStateReady: " + z);
        return z;
    }

    public static boolean isRadioOn(int i, Context context) {
        boolean zIsRadioOnForSubscriber;
        log("[isRadioOn]subId:" + i);
        ITelephony iTelephonyAsInterface = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        if (iTelephonyAsInterface != null && PhoneUtils.isValidSubId(i)) {
            try {
                zIsRadioOnForSubscriber = iTelephonyAsInterface.isRadioOnForSubscriber(i, context.getPackageName());
            } catch (RemoteException e) {
                log("[isRadioOn] failed to get radio state for sub " + i);
                zIsRadioOnForSubscriber = false;
            }
            log("[isRadioOn]isRadioOn:" + zIsRadioOnForSubscriber);
            return zIsRadioOnForSubscriber && !isAirplaneModeOn(PhoneGlobals.getInstance());
        }
        log("[isRadioOn]failed to check radio");
        zIsRadioOnForSubscriber = false;
        log("[isRadioOn]isRadioOn:" + zIsRadioOnForSubscriber);
        if (zIsRadioOnForSubscriber) {
            return false;
        }
    }

    public static boolean isGeminiProject() {
        boolean zIsMultiSimEnabled = TelephonyManager.getDefault().isMultiSimEnabled();
        log("isGeminiProject : " + zIsMultiSimEnabled);
        return zIsMultiSimEnabled;
    }

    public static boolean isInCall(Context context) {
        boolean zIsInCall;
        TelecomManager telecomManager = (TelecomManager) context.getSystemService("telecom");
        if (telecomManager != null) {
            zIsInCall = telecomManager.isInCall();
        } else {
            zIsInCall = false;
        }
        log("[isInCall] = " + zIsInCall);
        return zIsInCall;
    }

    public static boolean isAirplaneModeOn(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), "airplane_mode_on", 0) != 0;
    }

    public static boolean isImsServiceAvailable(Context context, int i) {
        boolean zIsImsRegistered;
        if (PhoneUtils.isValidSubId(i)) {
            zIsImsRegistered = MtkTelephonyManagerEx.getDefault().isImsRegistered(i);
        } else {
            zIsImsRegistered = false;
        }
        log("isImsServiceAvailable[" + i + "], available = " + zIsImsRegistered);
        return zIsImsRegistered;
    }

    public static boolean isHotSwapHanppened(List<SubscriptionInfo> list, List<SubscriptionInfo> list2) {
        if (list.size() != list2.size()) {
            return true;
        }
        boolean z = false;
        int i = 0;
        while (true) {
            if (i >= list2.size()) {
                break;
            }
            if (list2.get(i).getIccId().equals(list.get(i).getIccId())) {
                i++;
            } else {
                z = true;
                break;
            }
        }
        log("isHotSwapHanppened : " + z);
        return z;
    }

    private static void log(String str) {
        Log.d("TelephonyUtils", str);
    }

    public static boolean isMtkTddDataOnlySupport() {
        boolean z = SettingsConstants.DUA_VAL_ON.equals(SystemProperties.get("ro.vendor.mtk_tdd_data_only_support"));
        Log.d("TelephonyUtils", "isMtkTddDataOnlySupport(): " + z);
        return z;
    }

    public static boolean isCTLteTddTestSupport() {
        String[] supportCardType = MtkTelephonyManagerEx.getDefault().getSupportCardType(0);
        if (supportCardType == null) {
            return false;
        }
        return PhoneFeatureConstants.FeatureOption.isMtkSvlteSupport() && (supportCardType.length == 1 && "USIM".equals(supportCardType[0]));
    }

    public static boolean shouldShowOpenMobileDataDialog(Context context, int i) {
        if (!PhoneUtils.isValidSubId(i)) {
            log("[shouldShowOpenMobileDataDialog] invalid subId!!!  " + i);
            return false;
        }
        PersistableBundle carrierConfigForSubId = PhoneGlobals.getInstance().getCarrierConfigForSubId(i);
        if (!ExtensionManager.getCallFeaturesSettingExt().needShowOpenMobileDataDialog(context, i) || !carrierConfigForSubId.getBoolean("mtk_show_open_mobile_data_dialog_bool")) {
            return false;
        }
        Phone phoneUsingSubId = PhoneUtils.getPhoneUsingSubId(i);
        MtkGsmCdmaPhone phone = PhoneFactory.getPhone(phoneUsingSubId.getPhoneId());
        boolean z = true;
        if (isImsServiceAvailable(context, i) || (CallSettingUtils.isUtSupport(i) && phone.getCsFallbackStatus() == 0)) {
            log("[shouldShowOpenMobileDataDialog] ss query need mobile data connection!");
            if (((TelephonyManager) context.getSystemService("phone")).isWifiCallingAvailable() && !CallSettingUtils.isCmccOrCuCard(i)) {
                return false;
            }
            if (CallSettingUtils.isMobileDataEnabled(i) && (i == SubscriptionManager.getDefaultDataSubscriptionId() || !isSupportDualVolte(i))) {
                if (!MtkTelephonyManagerEx.getDefault().isInHomeNetwork(i) && CallSettingUtils.isCmccOrCuCard(i) && !phoneUsingSubId.getDataRoamingEnabled()) {
                    log("[shouldShowOpenMobileDataDialog] network is roaming!");
                } else {
                    z = false;
                }
            }
        }
        log("[shouldShowOpenMobileDataDialog] subId: " + i + ",result: " + z);
        return z;
    }

    public static boolean isSupportDualVolte(int i) {
        return MtkImsManager.isSupportMims() && (CallSettingUtils.isCmccOrCuCard(i) || (TelephonyUtilsEx.isCtVolteEnabled() && TelephonyUtilsEx.isCt4gSim(i)));
    }
}
