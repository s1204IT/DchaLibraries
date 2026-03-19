package com.mediatek.internal.telephony;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

public class MtkLteDataOnlyController {
    private static final String ACTION_CHECK_PERMISSISON_SERVICE = "com.mediatek.intent.action.LTE_DATA_ONLY_MANAGER";
    public static final int CDMA3G_SIM = 3;
    public static final int CDMA4G_SIM = 4;
    public static final int CDMA_SIM = 5;
    private static final String CHECK_PERMISSION_SERVICE_PACKAGE = "com.android.phone";
    private static final String CSIM = "CSIM";
    public static final int ERROR_SIM = -1;
    public static final int GSM_SIM = 2;
    private static final String[] PROPERTY_RIL_FULL_UICC_TYPE = {"vendor.gsm.ril.fulluicctype", "vendor.gsm.ril.fulluicctype.2", "vendor.gsm.ril.fulluicctype.3", "vendor.gsm.ril.fulluicctype.4"};
    private static final String RUIM = "RUIM";
    private static final String SIM = "SIM";
    public static final int SVLTE_RAT_MODE_3G = 1;
    public static final int SVLTE_RAT_MODE_4G = 0;
    public static final int SVLTE_RAT_MODE_4G_DATA_ONLY = 2;
    private static final String TAG = "MtkLteDataOnlyController";
    private static final String USIM = "USIM";
    private Context mContext;

    public MtkLteDataOnlyController(Context context) {
        this.mContext = context;
    }

    public boolean checkPermission() {
        if (isSupportTddDataOnlyCheck() && is4GDataOnly()) {
            startService();
            return false;
        }
        return true;
    }

    public boolean checkPermission(int i) {
        int slotIndex = SubscriptionManager.getSlotIndex(i);
        int i2 = SystemProperties.getInt("persist.vendor.radio.cdma_slot", -1) - 1;
        Rlog.d(TAG, "checkPermission subId=" + i + ", slotId=" + slotIndex + " cdmaSlotId=" + i2);
        if (i2 == slotIndex) {
            return checkPermission();
        }
        return true;
    }

    private void startService() {
        int[] subId = SubscriptionManager.getSubId(getCdmaSlot());
        Intent intent = new Intent(ACTION_CHECK_PERMISSISON_SERVICE);
        intent.setPackage(CHECK_PERMISSION_SERVICE_PACKAGE);
        if (subId != null) {
            intent.putExtra("subscription", subId[0]);
        }
        if (this.mContext != null) {
            this.mContext.startService(intent);
        }
    }

    private boolean is4GDataOnly() {
        int[] subId;
        if (this.mContext == null || (subId = SubscriptionManager.getSubId(SystemProperties.getInt("persist.vendor.radio.cdma_slot", -1) - 1)) == null) {
            return false;
        }
        ContentResolver contentResolver = this.mContext.getContentResolver();
        StringBuilder sb = new StringBuilder();
        sb.append("preferred_network_mode");
        sb.append(subId[0]);
        return Settings.Global.getInt(contentResolver, sb.toString(), MtkRILConstants.PREFERRED_NETWORK_MODE) == 31;
    }

    public static String getFullIccCardTypeExt() {
        int cdmaSlot = getCdmaSlot();
        if (cdmaSlot < 0 || cdmaSlot >= PROPERTY_RIL_FULL_UICC_TYPE.length) {
            cdmaSlot = 0;
        }
        String str = SystemProperties.get(PROPERTY_RIL_FULL_UICC_TYPE[cdmaSlot]);
        Rlog.d(TAG, "getFullIccCardTypeExt slotId = " + cdmaSlot + ",cardType = " + str);
        return str;
    }

    public static int getSimType() {
        String fullIccCardTypeExt = getFullIccCardTypeExt();
        if (fullIccCardTypeExt == null) {
            return -1;
        }
        if (fullIccCardTypeExt.contains(CSIM) || fullIccCardTypeExt.contains(RUIM)) {
            if (fullIccCardTypeExt.contains(CSIM) || fullIccCardTypeExt.contains(USIM)) {
                return 4;
            }
            if (fullIccCardTypeExt.contains(SIM)) {
                return 3;
            }
            return 5;
        }
        if (!fullIccCardTypeExt.contains(SIM) && !fullIccCardTypeExt.contains(USIM)) {
            return -1;
        }
        return 2;
    }

    public static boolean isCdmaCardType() {
        return getSimType() == 4 || getSimType() == 3 || getSimType() == 5;
    }

    public static boolean isCdmaLteCardType() {
        return getSimType() == 4;
    }

    public static boolean isCdma3GCardType() {
        return getSimType() == 3;
    }

    private static int getCdmaSlot() {
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            if (TelephonyManager.getDefault().getCurrentPhoneTypeForSlot(i) == 2) {
                return i;
            }
        }
        return -1;
    }

    private boolean isSupportTddDataOnlyCheck() {
        boolean z;
        boolean zIsCdmaLteCardType = isCdmaLteCardType();
        boolean zEquals = SystemProperties.get("ro.boot.opt_c2k_lte_mode").equals("1");
        boolean zEquals2 = "1".equals(SystemProperties.get("ro.vendor.mtk_tdd_data_only_support"));
        if (zIsCdmaLteCardType && zEquals && zEquals2) {
            z = true;
        } else {
            z = false;
        }
        Rlog.d(TAG, "isCdma4gCard : " + zIsCdmaLteCardType + ", isCdmaLteDcSupport : " + zEquals + ", isSupport4gDataOnly : " + zEquals2 + ", isSupportTddDataOnlyCheck return " + z);
        return z;
    }
}
