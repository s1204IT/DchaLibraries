package com.mediatek.settings.cdma;

import android.content.Context;
import android.os.Build;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.Phone;
import com.android.phone.PhoneGlobals;
import com.android.phone.settings.SettingsConstants;
import com.mediatek.internal.telephony.MtkIccCardConstants;
import com.mediatek.telephony.MtkTelephonyManagerEx;
import java.util.Iterator;
import java.util.List;

public class TelephonyUtilsEx {
    private static final boolean DBG = "eng".equals(Build.TYPE);
    static final int preferredNetworkMode = Phone.PREFERRED_NT_MODE;
    private static final String[] CT_NUMERIC = {"45502", "45507", "46003", "46011", "46012", "46013", "20404", "45431"};

    public static boolean isCDMAPhone(Phone phone) {
        boolean z;
        if (phone != null && phone.getPhoneType() == 2) {
            z = true;
        } else {
            z = false;
        }
        log("isCDMAPhone: " + z);
        return z;
    }

    public static int getSimType(int i) {
        int iccAppFamily;
        MtkTelephonyManagerEx mtkTelephonyManagerEx = MtkTelephonyManagerEx.getDefault();
        if (mtkTelephonyManagerEx != null) {
            iccAppFamily = mtkTelephonyManagerEx.getIccAppFamily(i);
        } else {
            iccAppFamily = 0;
        }
        log("simType: " + iccAppFamily);
        return iccAppFamily;
    }

    public static boolean isAirPlaneMode() {
        boolean z = Settings.System.getInt(PhoneGlobals.getInstance().getContentResolver(), "airplane_mode_on", -1) == 1;
        log("isAirPlaneMode = " + z);
        return z;
    }

    public static boolean isSvlteSlotInserted() {
        boolean zHasIccCard;
        if (SubscriptionManager.isValidSubscriptionId(getCdmaSubId())) {
            int slotIndex = SubscriptionManager.getSlotIndex(getCdmaSubId());
            TelephonyManager telephonyManager = TelephonyManager.getDefault();
            if (telephonyManager != null) {
                zHasIccCard = telephonyManager.hasIccCard(slotIndex);
            } else {
                zHasIccCard = false;
            }
        }
        log("isSvlteSlotInserted = " + zHasIccCard);
        return zHasIccCard;
    }

    public static boolean getRadioStateForSlotId(int i) {
        boolean z = true;
        if ((Settings.System.getInt(PhoneGlobals.getInstance().getContentResolver(), "msim_mode_setting", -1) & (1 << i)) == 0) {
            z = false;
        }
        log("soltId: " + i + ", radiosState : " + z);
        return z;
    }

    public static boolean isSvlteSlotRadioOn() {
        boolean radioStateForSlotId;
        if (SubscriptionManager.isValidSubscriptionId(getCdmaSubId())) {
            radioStateForSlotId = getRadioStateForSlotId(SubscriptionManager.getSlotIndex(getCdmaSubId()));
        } else {
            radioStateForSlotId = false;
        }
        log("isSvlteSlotRadioOn = " + radioStateForSlotId);
        return radioStateForSlotId;
    }

    public static boolean isRoaming(Phone phone) {
        int subId;
        boolean z = false;
        if (phone != null) {
            subId = phone.getSubId();
            if (phone.getServiceState().getRoaming()) {
                z = true;
            }
        } else {
            subId = -1;
        }
        log("isRoaming[" + subId + "] " + z);
        return z;
    }

    public static boolean isCdmaRoaming(Phone phone) {
        int subId;
        boolean z = false;
        if (phone != null) {
            subId = phone.getSubId();
            boolean zIsCdmaCardInserted = isCdmaCardInserted(phone);
            if (isRoaming(phone) || (zIsCdmaCardInserted && phone.getPhoneType() == 1)) {
                z = true;
            }
        } else {
            subId = -1;
        }
        log("isCdmaRoaming[" + subId + "] " + z);
        return z;
    }

    public static int getMainPhoneId() {
        int i;
        String str = SystemProperties.get("persist.vendor.radio.simswitch", SettingsConstants.DUA_VAL_ON);
        log("current 3G Sim = " + str);
        if (!TextUtils.isEmpty(str)) {
            i = Integer.parseInt(str) - 1;
        } else {
            i = -1;
        }
        log("getMainPhoneId: " + i);
        return i;
    }

    public static boolean isCapabilityPhone(Phone phone) {
        boolean z = getMainPhoneId() == phone.getPhoneId();
        log("isCapabilityPhone result = " + z + " phoneId = " + phone.getPhoneId());
        return z;
    }

    public static boolean isCdma4gCard(int i) {
        boolean zIs4GCard;
        MtkIccCardConstants.CardType cdmaCardType = MtkTelephonyManagerEx.getDefault().getCdmaCardType(SubscriptionManager.getSlotIndex(i));
        if (cdmaCardType != null) {
            zIs4GCard = cdmaCardType.is4GCard();
        } else {
            log("isCdma4gCard: cardType == null ");
            zIs4GCard = false;
        }
        log("isCdma4gCard result = " + zIs4GCard + "; subId = " + i);
        return zIs4GCard;
    }

    public static int getCdmaSubId() {
        int[] subId;
        int i = SystemProperties.getInt("persist.vendor.radio.cdma_slot", -1) - 1;
        log("[getCdmaSlotId] : slotId = " + i);
        if (!SubscriptionManager.isValidSlotIndex(i) || (subId = SubscriptionManager.getSubId(i)) == null) {
            return -1;
        }
        log("[getCdmaSlotId] : subId = " + subId[0]);
        return subId[0];
    }

    public static boolean isCdmaCardInserted(Phone phone) {
        int simType = getSimType(phone.getPhoneId());
        boolean z = simType == 2 || simType == 3;
        log("isCdmaCardInserted simType = " + simType + "result = " + z);
        return z;
    }

    private static void log(String str) {
        if (DBG) {
            Log.d("TelephonyUtilsEx", str);
        }
    }

    public static boolean isCtSim(int i) {
        String simOperator = TelephonyManager.getDefault().getSimOperator(i);
        String[] strArr = CT_NUMERIC;
        int length = strArr.length;
        boolean z = false;
        int i2 = 0;
        while (true) {
            if (i2 >= length) {
                break;
            }
            if (!strArr[i2].equals(simOperator)) {
                i2++;
            } else {
                z = true;
                break;
            }
        }
        log("getSimOperator:" + simOperator + ", sub id :" + i + ", isCtSim " + z);
        return z;
    }

    public static boolean isCt4gSim(int i) {
        return isCtSim(i) && isCdma4gCard(i);
    }

    public static boolean isCtVolteEnabled() {
        String str = SystemProperties.get("persist.vendor.mtk_ct_volte_support");
        boolean z = str.equals(SettingsConstants.DUA_VAL_ON) || str.equals("2");
        log("volteValue = " + str + ", isCtVolteEnabled " + z);
        return z;
    }

    public static boolean isCtAutoVolteEnabled() {
        boolean zEquals = SystemProperties.get("persist.vendor.mtk_ct_volte_support").equals("2");
        log("autoVolte = " + zEquals);
        return zEquals;
    }

    public static boolean is4GDataOnly(Context context) {
        boolean z;
        if (context != null && SubscriptionManager.isValidSubscriptionId(getCdmaSubId())) {
            if (Settings.Global.getInt(context.getContentResolver(), "preferred_network_mode" + getCdmaSubId(), preferredNetworkMode) == 31) {
                z = true;
            }
        } else {
            z = false;
        }
        log("is4GDataOnly: " + z);
        return z;
    }

    public static boolean isBothslotCt4gSim(SubscriptionManager subscriptionManager) {
        List<SubscriptionInfo> activeSubscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
        boolean zIsCt4gSim = false;
        if (activeSubscriptionInfoList == null || activeSubscriptionInfoList.size() <= 1) {
            return false;
        }
        Iterator<SubscriptionInfo> it = activeSubscriptionInfoList.iterator();
        while (it.hasNext() && (zIsCt4gSim = isCt4gSim(it.next().getSubscriptionId()))) {
        }
        return zIsCt4gSim;
    }

    public static boolean isBothslotCtSim(SubscriptionManager subscriptionManager) {
        List<SubscriptionInfo> activeSubscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
        boolean zIsCtSim = false;
        if (activeSubscriptionInfoList == null || activeSubscriptionInfoList.size() <= 1) {
            return false;
        }
        Iterator<SubscriptionInfo> it = activeSubscriptionInfoList.iterator();
        while (it.hasNext() && (zIsCtSim = isCtSim(it.next().getSubscriptionId()))) {
        }
        return zIsCtSim;
    }
}
