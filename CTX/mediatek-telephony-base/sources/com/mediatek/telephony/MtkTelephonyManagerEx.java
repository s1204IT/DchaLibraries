package com.mediatek.telephony;

import android.app.ActivityThread;
import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.CellInfo;
import android.telephony.CellLocation;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import com.android.internal.telephony.IPhoneSubInfo;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.ITelephonyRegistry;
import com.mediatek.internal.telephony.IMtkPhoneSubInfoEx;
import com.mediatek.internal.telephony.IMtkTelephonyEx;
import com.mediatek.internal.telephony.MtkIccCardConstants;
import com.mediatek.internal.telephony.MtkPhoneNumberFormatUtil;
import com.mediatek.internal.telephony.MtkPhoneNumberUtils;
import com.mediatek.internal.telephony.PseudoCellInfo;
import java.util.List;

public class MtkTelephonyManagerEx {
    public static final String ACTION_ECC_IN_PROGRESS = "android.intent.action.ECC_IN_PROGRESS";
    public static final int APP_FAM_3GPP = 1;
    public static final int APP_FAM_3GPP2 = 2;
    public static final int APP_FAM_NONE = 0;
    public static final int CARD_TYPE_CSIM = 4;
    public static final int CARD_TYPE_NONE = 0;
    public static final int CARD_TYPE_RUIM = 8;
    public static final int CARD_TYPE_SIM = 1;
    public static final int CARD_TYPE_USIM = 2;
    public static final String EXTRA_IN_PROGRESS = "in_progress";
    private static final String PRLVERSION = "vendor.cdma.prl.version";
    private static final String PROPERTY_SIM_SLOT_LOCK_POLICY = "vendor.gsm.sim.slot.lock.policy";
    private static final String PROPERTY_SIM_SLOT_LOCK_STATE = "vendor.gsm.sim.slot.lock.state";
    private static final String PROPERTY_SML_MODE = "ro.vendor.sim_me_lock_mode";
    private static final String TAG = "MtkTelephonyManagerEx";
    private Context mContext;
    private boolean mIsSmlLockMode;
    private ITelephonyRegistry mRegistry;
    private static final String[] PROPERTY_RIL_FULL_UICC_TYPE = {"vendor.gsm.ril.fulluicctype", "vendor.gsm.ril.fulluicctype.2", "vendor.gsm.ril.fulluicctype.3", "vendor.gsm.ril.fulluicctype.4"};
    private static final String[] PROPERTY_RIL_CT3G = {"vendor.gsm.ril.ct3g", "vendor.gsm.ril.ct3g.2", "vendor.gsm.ril.ct3g.3", "vendor.gsm.ril.ct3g.4"};
    private static final String[] PROPERTY_RIL_CDMA_CARD_TYPE = {"vendor.ril.cdma.card.type.1", "vendor.ril.cdma.card.type.2", "vendor.ril.cdma.card.type.3", "vendor.ril.cdma.card.type.4"};
    private static final String[] PROPERTY_SIM_SLOT_LOCK_SERVICE_CAPABILITY = {"vendor.gsm.sim.slot.lock.service.capability", "vendor.gsm.sim.slot.lock.service.capability.2", "vendor.gsm.sim.slot.lock.service.capability.3", "vendor.gsm.sim.slot.lock.service.capability.4"};
    private static final String[] PROPERTY_SIM_SLOT_LOCK_CARD_VALID = {"vendor.gsm.sim.slot.lock.card.valid", "vendor.gsm.sim.slot.lock.card.valid.2", "vendor.gsm.sim.slot.lock.card.valid.3", "vendor.gsm.sim.slot.lock.card.valid.4"};
    private static MtkTelephonyManagerEx sInstance = new MtkTelephonyManagerEx();

    public MtkTelephonyManagerEx(Context context) {
        this.mContext = null;
        this.mIsSmlLockMode = SystemProperties.get(PROPERTY_SML_MODE, "").equals("3");
        this.mContext = context;
        this.mRegistry = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry"));
    }

    private MtkTelephonyManagerEx() {
        this.mContext = null;
        this.mIsSmlLockMode = SystemProperties.get(PROPERTY_SML_MODE, "").equals("3");
        this.mRegistry = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry"));
    }

    public static MtkTelephonyManagerEx getDefault() {
        return sInstance;
    }

    public int getPhoneType(int i) {
        int[] subId = SubscriptionManager.getSubId(i);
        if (subId == null) {
            return TelephonyManager.getDefault().getCurrentPhoneType(-1);
        }
        Rlog.e(TAG, "Deprecated! getPhoneType with simId " + i + ", subId " + subId[0]);
        return TelephonyManager.getDefault().getCurrentPhoneType(subId[0]);
    }

    private int getSubIdBySlot(int i) {
        int[] subId = SubscriptionManager.getSubId(i);
        StringBuilder sb = new StringBuilder();
        sb.append("getSubIdBySlot, simId ");
        sb.append(i);
        sb.append("subId ");
        sb.append(subId != null ? Integer.valueOf(subId[0]) : "invalid!");
        Rlog.d(TAG, sb.toString());
        return subId != null ? subId[0] : SubscriptionManager.getDefaultSubscriptionId();
    }

    private ITelephony getITelephony() {
        return ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
    }

    private IMtkTelephonyEx getIMtkTelephonyEx() {
        return IMtkTelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
    }

    private IPhoneSubInfo getSubscriberInfo() {
        return IPhoneSubInfo.Stub.asInterface(ServiceManager.getService("iphonesubinfo"));
    }

    public boolean isInDsdaMode() {
        if (!SystemProperties.get("ro.vendor.mtk_switch_antenna", "0").equals(MtkPhoneNumberUtils.EccEntry.ECC_ALWAYS) && SystemProperties.getInt("ro.boot.opt_c2k_lte_mode", 0) == 1) {
            TelephonyManager telephonyManager = TelephonyManager.getDefault();
            int simCount = telephonyManager.getSimCount();
            for (int i = 0; i < simCount; i++) {
                int[] subId = SubscriptionManager.getSubId(i);
                if (subId == null) {
                    Rlog.d(TAG, "isInDsdaMode, allSubId is null for slot" + i);
                } else {
                    int currentPhoneType = telephonyManager.getCurrentPhoneType(subId[0]);
                    Rlog.d(TAG, "isInDsdaMode, allSubId[0]:" + subId[0] + ", phoneType:" + currentPhoneType);
                    if (currentPhoneType == 2) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void setTelLog(boolean z) {
        try {
            getIMtkTelephonyEx().setTelLog(z);
        } catch (RemoteException e) {
        } catch (NullPointerException e2) {
        }
    }

    public boolean isInHomeNetwork(int i) {
        try {
            IMtkTelephonyEx iMtkTelephonyEx = getIMtkTelephonyEx();
            if (iMtkTelephonyEx == null) {
                return false;
            }
            return iMtkTelephonyEx.isInHomeNetwork(i);
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    private IMtkPhoneSubInfoEx getMtkSubscriberInfoEx() {
        return IMtkPhoneSubInfoEx.Stub.asInterface(ServiceManager.getService("iphonesubinfoEx"));
    }

    public boolean getUsimService(int i) {
        return getUsimService(SubscriptionManager.getDefaultSubscriptionId(), i);
    }

    public boolean getUsimService(int i, int i2) {
        try {
            return getMtkSubscriberInfoEx().getUsimServiceForSubscriber(i, i2, getOpPackageName());
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public int getIccAppFamily(int i) {
        try {
            return getIMtkTelephonyEx().getIccAppFamily(i);
        } catch (RemoteException e) {
            return 0;
        } catch (NullPointerException e2) {
            return 0;
        }
    }

    public String getIccCardType(int i) {
        String iccCardType;
        try {
            iccCardType = getIMtkTelephonyEx().getIccCardType(i);
        } catch (RemoteException e) {
            e.printStackTrace();
            iccCardType = null;
        } catch (NullPointerException e2) {
            e2.printStackTrace();
            iccCardType = null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("getIccCardType sub ");
        sb.append(i);
        sb.append(" ,icc type ");
        sb.append(iccCardType != null ? iccCardType : "null");
        Rlog.d(TAG, sb.toString());
        return iccCardType;
    }

    private String getOpPackageName() {
        if (this.mContext != null) {
            return this.mContext.getOpPackageName();
        }
        return ActivityThread.currentOpPackageName();
    }

    public byte[] loadEFTransparent(int i, int i2, int i3, String str) {
        try {
            IMtkTelephonyEx iMtkTelephonyEx = getIMtkTelephonyEx();
            if (iMtkTelephonyEx != null) {
                return iMtkTelephonyEx.loadEFTransparent(i, i2, i3, str);
            }
            return null;
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public List<String> loadEFLinearFixedAll(int i, int i2, int i3, String str) {
        try {
            IMtkTelephonyEx iMtkTelephonyEx = getIMtkTelephonyEx();
            if (iMtkTelephonyEx != null) {
                return iMtkTelephonyEx.loadEFLinearFixedAll(i, i2, i3, str);
            }
            return null;
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getUimSubscriberId(int i) {
        try {
            return getIMtkTelephonyEx().getUimSubscriberId(getOpPackageName(), i);
        } catch (RemoteException e) {
            e.printStackTrace();
            return "";
        } catch (NullPointerException e2) {
            e2.printStackTrace();
            return "";
        }
    }

    public String[] getSupportCardType(int i) {
        String[] strArrSplit = null;
        if (i < 0 || i >= PROPERTY_RIL_FULL_UICC_TYPE.length) {
            Rlog.e(TAG, "getSupportCardType: invalid slotId " + i);
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
        Rlog.d(TAG, sb.toString());
        return strArrSplit;
    }

    public boolean isCt3gDualMode(int i) {
        if (i < 0 || i >= PROPERTY_RIL_CT3G.length) {
            Rlog.e(TAG, "isCt3gDualMode: invalid slotId " + i);
            return false;
        }
        String str = SystemProperties.get(PROPERTY_RIL_CT3G[i], "");
        Rlog.d(TAG, "isCt3gDualMode:  " + str);
        return MtkPhoneNumberUtils.EccEntry.ECC_ALWAYS.equals(str);
    }

    public MtkIccCardConstants.CardType getCdmaCardType(int i) {
        if (i < 0 || i >= PROPERTY_RIL_CT3G.length) {
            Rlog.e(TAG, "getCdmaCardType: invalid slotId " + i);
            return null;
        }
        MtkIccCardConstants.CardType cardTypeFromInt = MtkIccCardConstants.CardType.UNKNOW_CARD;
        String str = SystemProperties.get(PROPERTY_RIL_CDMA_CARD_TYPE[i], "");
        if (!str.equals("")) {
            cardTypeFromInt = MtkIccCardConstants.CardType.getCardTypeFromInt(Integer.parseInt(str));
        }
        Rlog.d(TAG, "getCdmaCardType result: " + str + "  mCdmaCardType: " + cardTypeFromInt);
        return cardTypeFromInt;
    }

    public String getSimSerialNumber(int i) {
        String simSerialNumber;
        if (i < 0 || i >= TelephonyManager.getDefault().getSimCount()) {
            Rlog.e(TAG, "getSimSerialNumber with invalid simId " + i);
            return null;
        }
        try {
            simSerialNumber = getIMtkTelephonyEx().getSimSerialNumber(getOpPackageName(), i);
        } catch (RemoteException e) {
            e.printStackTrace();
            simSerialNumber = null;
        } catch (NullPointerException e2) {
            e2.printStackTrace();
            simSerialNumber = null;
        }
        if (simSerialNumber == null) {
            return simSerialNumber;
        }
        if (simSerialNumber.equals("N/A") || simSerialNumber.equals("")) {
            return null;
        }
        return simSerialNumber;
    }

    public CellLocation getCellLocation(int i) {
        CellLocation gsmCellLocation;
        try {
            IMtkTelephonyEx iMtkTelephonyEx = getIMtkTelephonyEx();
            if (iMtkTelephonyEx == null) {
                Rlog.d(TAG, "getCellLocation returning null because telephony is null");
                return null;
            }
            Bundle cellLocationUsingSlotId = iMtkTelephonyEx.getCellLocationUsingSlotId(i);
            if (cellLocationUsingSlotId == null) {
                Rlog.d(TAG, "getCellLocation returning null because bundle is null");
                return null;
            }
            if (cellLocationUsingSlotId.isEmpty()) {
                Rlog.d(TAG, "getCellLocation returning null because bundle is empty");
                return null;
            }
            switch (getPhoneType(i)) {
                case 1:
                    gsmCellLocation = new GsmCellLocation(cellLocationUsingSlotId);
                    break;
                case 2:
                    gsmCellLocation = new CdmaCellLocation(cellLocationUsingSlotId);
                    break;
                default:
                    gsmCellLocation = null;
                    break;
            }
            Rlog.d(TAG, "getCellLocation is" + gsmCellLocation);
            if (gsmCellLocation == null) {
                Rlog.d(TAG, "getCellLocation returning null because cl is null");
                return null;
            }
            if (gsmCellLocation.isEmpty()) {
                Rlog.d(TAG, "getCellLocation returning null because CellLocation is empty");
                return null;
            }
            return gsmCellLocation;
        } catch (RemoteException e) {
            Rlog.d(TAG, "getCellLocation returning null due to RemoteException " + e);
            return null;
        } catch (NullPointerException e2) {
            Rlog.d(TAG, "getCellLocation returning null due to NullPointerException " + e2);
            return null;
        }
    }

    public boolean isGsm(int i) {
        return i == 1 || i == 2 || i == 3 || i == 8 || i == 9 || i == 10 || i == 13 || i == 15 || i == 16 || i == 17 || i == 19;
    }

    public String getIsimImpi(int i) {
        try {
            return getMtkSubscriberInfoEx().getIsimImpiForSubscriber(i);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getIsimDomain(int i) {
        try {
            return getMtkSubscriberInfoEx().getIsimDomainForSubscriber(i);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String[] getIsimImpu(int i) {
        try {
            return getMtkSubscriberInfoEx().getIsimImpuForSubscriber(i);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getIsimIst(int i) {
        try {
            return getMtkSubscriberInfoEx().getIsimIstForSubscriber(i);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String[] getIsimPcscf(int i) {
        try {
            return getMtkSubscriberInfoEx().getIsimPcscfForSubscriber(i);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public boolean isImsRegistered(int i) {
        try {
            return getIMtkTelephonyEx().isImsRegistered(i);
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public boolean isVolteEnabled(int i) {
        try {
            return getIMtkTelephonyEx().isVolteEnabled(i);
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public boolean isWifiCallingEnabled(int i) {
        try {
            return getIMtkTelephonyEx().isWifiCallingEnabled(i);
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public boolean isWifiCalllingActive(int i) {
        try {
            return getIMtkTelephonyEx().isWifiCallingEnabled(i);
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public String getIsimGbabp() {
        return getIsimGbabp(SubscriptionManager.getDefaultSubscriptionId());
    }

    public String getIsimGbabp(int i) {
        try {
            return getMtkSubscriberInfoEx().getIsimGbabpForSubscriber(i);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public void setIsimGbabp(String str, Message message) {
        setIsimGbabp(SubscriptionManager.getDefaultSubscriptionId(), str, message);
    }

    public void setIsimGbabp(int i, String str, Message message) {
        try {
            getMtkSubscriberInfoEx().setIsimGbabpForSubscriber(i, str, message);
        } catch (RemoteException e) {
        } catch (NullPointerException e2) {
        }
    }

    public String getUsimGbabp() {
        return getUsimGbabp(SubscriptionManager.getDefaultSubscriptionId());
    }

    public String getUsimGbabp(int i) {
        try {
            return getMtkSubscriberInfoEx().getUsimGbabpForSubscriber(i);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public void setUsimGbabp(String str, Message message) {
        setUsimGbabp(SubscriptionManager.getDefaultSubscriptionId(), str, message);
    }

    public void setUsimGbabp(int i, String str, Message message) {
        try {
            getMtkSubscriberInfoEx().setUsimGbabpForSubscriber(i, str, message);
        } catch (RemoteException e) {
        } catch (NullPointerException e2) {
        }
    }

    public String getPrlVersion(int i) {
        int slotIndex = SubscriptionManager.getSlotIndex(i);
        String str = SystemProperties.get(PRLVERSION + slotIndex, "");
        Rlog.d(TAG, "getPrlversion PRLVERSION subId = " + i + " key = " + PRLVERSION + slotIndex + " value = " + str);
        return str;
    }

    public int[] setRxTestConfig(int i) {
        try {
            return getIMtkTelephonyEx().setRxTestConfig(SubscriptionManager.getPhoneId(SubscriptionManager.getDefaultSubscriptionId()), i);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public int[] getRxTestResult() {
        try {
            return getIMtkTelephonyEx().getRxTestResult(SubscriptionManager.getPhoneId(SubscriptionManager.getDefaultSubscriptionId()));
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public void setEccInProgress(boolean z) {
        try {
            getIMtkTelephonyEx().setEccInProgress(z);
        } catch (RemoteException e) {
        } catch (NullPointerException e2) {
        }
    }

    public boolean isEccInProgress() {
        try {
            return getIMtkTelephonyEx().isEccInProgress();
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public boolean exitEmergencyCallbackMode(int i) {
        try {
            return getIMtkTelephonyEx().exitEmergencyCallbackMode(i);
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public void setApcMode(int i, int i2, boolean z, int i3) {
        if (i < 0 || i >= TelephonyManager.getDefault().getSimCount()) {
            Rlog.e(TAG, "setApcMode error with invalid slotId " + i);
            return;
        }
        if (i2 < 0 || i2 > 2) {
            Rlog.e(TAG, "setApcMode error with invalid mode " + i2);
            return;
        }
        try {
            IMtkTelephonyEx iMtkTelephonyEx = getIMtkTelephonyEx();
            if (iMtkTelephonyEx == null) {
                Rlog.e(TAG, "setApcMode error because telephony is null");
            } else {
                iMtkTelephonyEx.setApcModeUsingSlotId(i, i2, z, i3);
            }
        } catch (RemoteException e) {
            Rlog.e(TAG, "setApcMode error due to RemoteException " + e);
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "setApcMode error due to NullPointerException " + e2);
        }
    }

    public PseudoCellInfo getApcInfo(int i) {
        if (i < 0 || i >= TelephonyManager.getDefault().getSimCount()) {
            Rlog.e(TAG, "getApcInfo with invalid slotId " + i);
            return null;
        }
        try {
            IMtkTelephonyEx iMtkTelephonyEx = getIMtkTelephonyEx();
            if (iMtkTelephonyEx == null) {
                Rlog.e(TAG, "getApcInfo return null because telephony is null");
                return null;
            }
            return iMtkTelephonyEx.getApcInfoUsingSlotId(i);
        } catch (RemoteException e) {
            Rlog.e(TAG, "getApcInfo returning null due to RemoteException " + e);
            return null;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "getApcInfo returning null due to NullPointerException " + e2);
            return null;
        }
    }

    public int getCdmaSubscriptionActStatus(int i) {
        try {
            return getIMtkTelephonyEx().getCdmaSubscriptionActStatus(i);
        } catch (RemoteException e) {
            Rlog.d(TAG, "fail to getCdmaSubscriptionActStatus due to RemoteException");
            return 0;
        } catch (NullPointerException e2) {
            Rlog.d(TAG, "fail to getCdmaSubscriptionActStatus due to NullPointerException");
            return 0;
        }
    }

    public boolean useATTLogic() {
        if (SystemProperties.get("persist.vendor.operator.optr", "OM").equals("OP07")) {
            return true;
        }
        return false;
    }

    public boolean useVzwLogic() {
        String str = SystemProperties.get("persist.vendor.operator.optr", "OM");
        if (str.equals("OP12") || str.equals("OP20")) {
            return true;
        }
        return false;
    }

    public int invokeOemRilRequestRaw(byte[] bArr, byte[] bArr2) {
        try {
            IMtkTelephonyEx iMtkTelephonyEx = getIMtkTelephonyEx();
            if (iMtkTelephonyEx != null) {
                return iMtkTelephonyEx.invokeOemRilRequestRaw(bArr, bArr2);
            }
            return -1;
        } catch (RemoteException e) {
            return -1;
        } catch (NullPointerException e2) {
            return -1;
        }
    }

    public int invokeOemRilRequestRawBySlot(int i, byte[] bArr, byte[] bArr2) {
        try {
            IMtkTelephonyEx iMtkTelephonyEx = getIMtkTelephonyEx();
            if (iMtkTelephonyEx != null) {
                return iMtkTelephonyEx.invokeOemRilRequestRawBySlot(i, bArr, bArr2);
            }
            return -1;
        } catch (RemoteException e) {
            return -1;
        } catch (NullPointerException e2) {
            return -1;
        }
    }

    public boolean isDigitsSupported() {
        return SystemProperties.getInt("persist.vendor.mtk_digits_support", 0) == 1;
    }

    public boolean isInCsCall(int i) {
        try {
            IMtkTelephonyEx iMtkTelephonyEx = getIMtkTelephonyEx();
            if (iMtkTelephonyEx == null) {
                Rlog.e(TAG, "[isInCsCall] telephony = null");
                return false;
            }
            return iMtkTelephonyEx.isInCsCall(i);
        } catch (RemoteException e) {
            Rlog.e(TAG, "[isInCsCall] RemoteException " + e);
            return false;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "[isInCsCall] NullPointerException " + e2);
            return false;
        }
    }

    public int getSimCardState(int i) {
        int simStateForSlotIndex = SubscriptionManager.getSimStateForSlotIndex(i);
        switch (simStateForSlotIndex) {
            case 0:
            case 1:
            case 8:
            case MtkPhoneNumberFormatUtil.FORMAT_ITALY:
                return simStateForSlotIndex;
            default:
                return 11;
        }
    }

    public int getSimApplicationState(int i) {
        int simStateForSlotIndex = SubscriptionManager.getSimStateForSlotIndex(i);
        switch (simStateForSlotIndex) {
            case 0:
            case 1:
            case 8:
            case MtkPhoneNumberFormatUtil.FORMAT_ITALY:
                return 0;
            case 5:
                return 6;
            default:
                return simStateForSlotIndex;
        }
    }

    public List<CellInfo> getAllCellInfo(int i) {
        try {
            IMtkTelephonyEx iMtkTelephonyEx = getIMtkTelephonyEx();
            if (iMtkTelephonyEx == null) {
                return null;
            }
            return iMtkTelephonyEx.getAllCellInfo(i, getOpPackageName());
        } catch (RemoteException e) {
            return null;
        }
    }

    public String getLocatedPlmn(int i) {
        try {
            IMtkTelephonyEx iMtkTelephonyEx = getIMtkTelephonyEx();
            if (iMtkTelephonyEx == null) {
                return null;
            }
            return iMtkTelephonyEx.getLocatedPlmn(i);
        } catch (RemoteException e) {
            Rlog.e(TAG, "fail to getLocatedPlmn due to RemoteException");
            return null;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "fail to getLocatedPlmn due to NullPointerException");
            return null;
        }
    }

    public boolean isDssNoResetSupport() {
        if (SystemProperties.get("vendor.ril.simswitch.no_reset_support").equals(MtkPhoneNumberUtils.EccEntry.ECC_ALWAYS)) {
            Rlog.d(TAG, "return true for isDssNoResetSupport");
            return true;
        }
        Rlog.d(TAG, "return false for isDssNoResetSupport");
        return false;
    }

    public int getProtocolStackId(int i) throws RemoteException {
        int mainCapabilityPhoneId = 0;
        try {
            IMtkTelephonyEx iMtkTelephonyEx = getIMtkTelephonyEx();
            if (iMtkTelephonyEx != null) {
                mainCapabilityPhoneId = iMtkTelephonyEx.getMainCapabilityPhoneId();
            }
        } catch (RemoteException e) {
            Rlog.e(TAG, "fail to getMainCapabilityPhoneId due to RemoteException");
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "fail to getMainCapabilityPhoneId due to NullPointerException");
        }
        if (i == mainCapabilityPhoneId) {
            return 1;
        }
        if (isDssNoResetSupport()) {
            if (i < mainCapabilityPhoneId) {
                return i + 2;
            }
        } else if (i == 0) {
            return mainCapabilityPhoneId + 1;
        }
        return i + 1;
    }

    public int getSimLockPolicy() {
        if (this.mIsSmlLockMode) {
            int i = SystemProperties.getInt(PROPERTY_SIM_SLOT_LOCK_POLICY, -1);
            Rlog.d(TAG, "getSimLockPolicy: " + i);
            return i;
        }
        return 0;
    }

    public int getShouldServiceCapability(int i) {
        if (this.mIsSmlLockMode) {
            if (i < 0 || i >= PROPERTY_SIM_SLOT_LOCK_SERVICE_CAPABILITY.length) {
                Rlog.e(TAG, "getShouldServiceCapability: invalid slotId: " + i);
                return 4;
            }
            int i2 = SystemProperties.getInt(PROPERTY_SIM_SLOT_LOCK_SERVICE_CAPABILITY[i], -1);
            Rlog.d(TAG, "getShouldServiceCapability: " + i2 + ",slotId: " + i);
            return i2;
        }
        return 0;
    }

    public int checkValidCard(int i) {
        if (this.mIsSmlLockMode) {
            if (i < 0 || i >= PROPERTY_SIM_SLOT_LOCK_CARD_VALID.length) {
                Rlog.e(TAG, "checkValidCard: invalid slotId " + i);
                return 2;
            }
            int i2 = SystemProperties.getInt(PROPERTY_SIM_SLOT_LOCK_CARD_VALID[i], -1);
            Rlog.d(TAG, "checkValidCard: " + i2 + ",slotId: " + i);
            return i2;
        }
        return 0;
    }

    public int getSimLockState() {
        if (this.mIsSmlLockMode) {
            int i = SystemProperties.getInt(PROPERTY_SIM_SLOT_LOCK_STATE, -1);
            Rlog.d(TAG, "getSimLockState: " + i);
            return i;
        }
        return 1;
    }

    public String getLine1PhoneNumber(int i) {
        String line1NumberForDisplay;
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                line1NumberForDisplay = iTelephony.getLine1NumberForDisplay(i, this.mContext.getOpPackageName());
            } else {
                line1NumberForDisplay = null;
            }
        } catch (RemoteException | NullPointerException e) {
            line1NumberForDisplay = null;
        }
        if (line1NumberForDisplay != null) {
            return line1NumberForDisplay;
        }
        try {
            IMtkPhoneSubInfoEx mtkSubscriberInfoEx = getMtkSubscriberInfoEx();
            if (mtkSubscriberInfoEx == null) {
                return null;
            }
            return mtkSubscriberInfoEx.getLine1PhoneNumberForSubscriber(i, getOpPackageName());
        } catch (RemoteException e2) {
            return null;
        } catch (NullPointerException e3) {
            return null;
        }
    }
}
