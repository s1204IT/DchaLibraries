package com.mediatek.internal.telephony;

import android.content.Context;
import android.os.SystemProperties;
import android.telephony.Rlog;
import mediatek.telephony.MtkServiceState;

public class ServiceStateTrackerExt implements IServiceStateTrackerExt {
    private static final int CARD_TYPE_CSIM = 2;
    private static final int CARD_TYPE_NONE = 0;
    private static final int CARD_TYPE_RUIM = 4;
    private static final int CARD_TYPE_USIM = 1;
    private static final String[] PROPERTY_RIL_FULL_UICC_TYPE = {"vendor.gsm.ril.fulluicctype", "vendor.gsm.ril.fulluicctype.2", "vendor.gsm.ril.fulluicctype.3", "vendor.gsm.ril.fulluicctype.4"};
    static final String TAG = "SSTExt";
    protected Context mContext;

    public ServiceStateTrackerExt() {
    }

    public ServiceStateTrackerExt(Context context) {
        this.mContext = context;
    }

    @Override
    public String onUpdateSpnDisplay(String str, MtkServiceState mtkServiceState, int i) {
        return str;
    }

    @Override
    public boolean isImeiLocked() {
        return false;
    }

    @Override
    public boolean isBroadcastEmmrrsPsResume(int i) {
        return false;
    }

    @Override
    public boolean needEMMRRS() {
        return false;
    }

    @Override
    public boolean needSpnRuleShowPlmnOnly() {
        if (SystemProperties.get("ro.vendor.mtk_cta_support").equals("1")) {
            return true;
        }
        return false;
    }

    @Override
    public boolean needBrodcastAcmt(int i, int i2) {
        return false;
    }

    @Override
    public boolean needRejectCauseNotification(int i) {
        return false;
    }

    @Override
    public boolean needIgnoreFemtocellUpdate(int i, int i2) {
        return false;
    }

    @Override
    public boolean needToShowCsgId() {
        return true;
    }

    @Override
    public boolean needBlankDisplay(int i) {
        return false;
    }

    @Override
    public boolean needIgnoredState(int i, int i2, int i3) {
        if (i == 0 && i2 == 2) {
            Rlog.i(TAG, "set dontUpdateNetworkStateFlag for searching state");
            return true;
        }
        if (i3 != -1) {
            if (i == 0 && i2 == 3 && i3 != 0) {
                Rlog.i(TAG, "set dontUpdateNetworkStateFlag for REG_DENIED with cause");
                return true;
            }
            if (i == 0 && i2 == 0 && i3 != 0) {
                Rlog.i(TAG, "set dontUpdateNetworkStateFlag for NOT_REG_AND_NOT_SEARCH with cause");
                return true;
            }
        }
        Rlog.i(TAG, "clear dontUpdateNetworkStateFlag");
        return false;
    }

    @Override
    public boolean operatorDefinedInternationalRoaming(String str) {
        return false;
    }

    public void log(String str) {
        Rlog.d(TAG, str);
    }

    @Override
    public boolean allowSpnDisplayed() {
        return true;
    }

    @Override
    public int needAutoSwitchRatMode(int i, String str) {
        return -1;
    }

    @Override
    public boolean isSupportRatBalancing() {
        return false;
    }

    @Override
    public boolean isNeedDisableIVSR() {
        return false;
    }

    @Override
    public String onUpdateSpnDisplayForIms(String str, MtkServiceState mtkServiceState, int i, int i2, Object obj) {
        return str;
    }

    private boolean isCdmaLteDcSupport() {
        if (SystemProperties.get("ro.boot.opt_c2k_lte_mode").equals("1") || SystemProperties.get("ro.boot.opt_c2k_lte_mode").equals(MtkGsmCdmaPhone.ACT_TYPE_UTRAN)) {
            return true;
        }
        return false;
    }

    private String[] getSupportCardType(int i) {
        String[] strArrSplit = null;
        if (i < 0 || i >= PROPERTY_RIL_FULL_UICC_TYPE.length) {
            log("getSupportCardType: invalid slotId " + i);
            return null;
        }
        String str = SystemProperties.get(PROPERTY_RIL_FULL_UICC_TYPE[i], "");
        if (!str.equals("") && str.length() > 0) {
            strArrSplit = str.split(",");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("getSupportCardType slotId ");
        sb.append(i);
        sb.append(", prop value= ");
        sb.append(str);
        sb.append(", size= ");
        sb.append(strArrSplit != null ? strArrSplit.length : 0);
        log(sb.toString());
        return strArrSplit;
    }

    private boolean isCdma4GCard(int i) {
        String[] supportCardType = getSupportCardType(i);
        if (supportCardType == null) {
            log("isCdma4GCard, get non support card type");
            return false;
        }
        int i2 = 0;
        for (int i3 = 0; i3 < supportCardType.length; i3++) {
            if ("USIM".equals(supportCardType[i3])) {
                i2 |= 1;
            } else if ("RUIM".equals(supportCardType[i3])) {
                i2 |= 4;
            } else if ("CSIM".equals(supportCardType[i3])) {
                i2 |= 2;
            }
        }
        log("isCdma4GCard, cardType=" + i2);
        return ((i2 & 4) > 0 || (i2 & 2) > 0) && (i2 & 1) > 0;
    }

    @Override
    public boolean isRoamingForSpecialSIM(String str, String str2) {
        boolean zIsCdmaLteDcSupport = isCdmaLteDcSupport();
        log("isRoamingForSpecialSIM, strServingPlmn: " + str + ", strHomePlmn: " + str2 + ", cdmaLteSupport = " + zIsCdmaLteDcSupport);
        if (zIsCdmaLteDcSupport && str != null && !str.startsWith(RadioCapabilitySwitchUtil.CN_MCC)) {
            if ("45403".equals(str2) || "45404".equals(str2)) {
                Rlog.d(TAG, "special SIM, force roaming. IMSI:" + str2);
                return true;
            }
            return false;
        }
        return false;
    }
}
