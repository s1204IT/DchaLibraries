package com.mediatek.telephony;

import android.app.ActivityThread;
import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.IPhoneSubInfo;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.ITelephonyRegistry;
import com.android.internal.telephony.IccCardConstants;
import com.mediatek.internal.telephony.ISetDefaultSubResultCallback;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.internal.telephony.MmsConfigInfo;
import com.mediatek.internal.telephony.MmsIcpInfo;
import com.mediatek.internal.telephony.PseudoBSRecord;
import com.mediatek.internal.telephony.ratconfiguration.RatConfiguration;
import com.mediatek.mmsdk.BaseParameters;
import java.util.ArrayList;
import java.util.List;

public class TelephonyManagerEx {
    public static final String ACTION_ECC_IN_PROGRESS = "android.intent.action.ECC_IN_PROGRESS";
    public static final int APP_FAM_3GPP = 1;
    public static final int APP_FAM_3GPP2 = 2;
    public static final int APP_FAM_NONE = 0;
    public static final int CARD_TYPE_CSIM = 4;
    public static final int CARD_TYPE_NONE = 0;
    public static final int CARD_TYPE_RUIM = 8;
    public static final int CARD_TYPE_SIM = 1;
    public static final int CARD_TYPE_USIM = 2;
    public static final int DEFAULT_DATA = 0;
    public static final int DEFAULT_SMS = 2;
    public static final int DEFAULT_VOICE = 1;
    public static final byte ERROR_CODE_GENERIC_ERROR = 1;
    public static final byte ERROR_CODE_NO_ERROR = 0;
    public static final byte ERROR_CODE_NO_SUPPORT_SC_ADDR = 2;
    public static final String EXTRA_IN_PROGRESS = "in_progress";
    public static final String GET_SC_ADDRESS_KEY_ADDRESS = "scAddress";
    public static final String GET_SC_ADDRESS_KEY_RESULT = "errorCode";
    private static final String PRLVERSION = "cdma.prl.version";
    private static final String TAG = "TelephonyManagerEx";
    private String[] PROPERTY_ICCID_SIM;
    private Context mContext;
    private ITelephonyRegistry mRegistry;
    private static int defaultSimId = 0;
    private static final String[] PROPERTY_RIL_FULL_UICC_TYPE = {"gsm.ril.fulluicctype", "gsm.ril.fulluicctype.2", "gsm.ril.fulluicctype.3", "gsm.ril.fulluicctype.4"};
    private static final String[] PROPERTY_RIL_CT3G = {"gsm.ril.ct3g", "gsm.ril.ct3g.2", "gsm.ril.ct3g.3", "gsm.ril.ct3g.4"};
    private static final String[] PROPERTY_RIL_CDMA_CARD_TYPE = {"ril.cdma.card.type.1", "ril.cdma.card.type.2", "ril.cdma.card.type.3", "ril.cdma.card.type.4"};
    private static final String[] PROPERTY_UIM_SUBSCRIBER_ID = {"ril.uim.subscriberid.1", "ril.uim.subscriberid.2", "ril.uim.subscriberid.3", "ril.uim.subscriberid.4"};
    private static TelephonyManagerEx sInstance = new TelephonyManagerEx();

    public TelephonyManagerEx(Context context) {
        this.mContext = null;
        this.PROPERTY_ICCID_SIM = new String[]{"ril.iccid.sim1", "ril.iccid.sim2", "ril.iccid.sim3", "ril.iccid.sim4"};
        Rlog.d(TAG, "getSubscriberInfo");
        this.mContext = context;
        this.mRegistry = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry"));
    }

    private TelephonyManagerEx() {
        this.mContext = null;
        this.PROPERTY_ICCID_SIM = new String[]{"ril.iccid.sim1", "ril.iccid.sim2", "ril.iccid.sim3", "ril.iccid.sim4"};
        this.mRegistry = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry"));
    }

    public static TelephonyManagerEx getDefault() {
        return sInstance;
    }

    public String getDeviceSoftwareVersion(int simId) {
        Rlog.d(TAG, "getDeviceSoftwareVersion simId=" + simId);
        try {
            return TelephonyManager.getDefault().getDeviceSoftwareVersion(simId);
        } catch (NullPointerException ex) {
            Rlog.e(TAG, "getDeviceSoftwareVersion error, return null. (" + ex.toString() + ")");
            return null;
        }
    }

    public String getDeviceId(int simId) {
        Rlog.d(TAG, "getDeviceId simId=" + simId);
        try {
            return TelephonyManager.getDefault().getDeviceId(simId);
        } catch (NullPointerException ex) {
            Rlog.e(TAG, "getDeviceId error, return null. (" + ex.toString() + ")");
            return null;
        }
    }

    public CellLocation getCellLocation(int simId) {
        try {
            ITelephonyEx telephony = getITelephonyEx();
            if (telephony == null) {
                Rlog.d(TAG, "getCellLocation returning null because telephony is null");
                return null;
            }
            Bundle bundle = telephony.getCellLocationUsingSlotId(simId);
            if (bundle.isEmpty()) {
                Rlog.d(TAG, "getCellLocation returning null because bundle is empty");
                return null;
            }
            CellLocation cl = CellLocation.newFromBundle(bundle);
            if (cl.isEmpty()) {
                Rlog.d(TAG, "getCellLocation returning null because CellLocation is empty");
                return null;
            }
            return cl;
        } catch (RemoteException ex) {
            Rlog.d(TAG, "getCellLocation returning null due to RemoteException " + ex);
            return null;
        } catch (NullPointerException ex2) {
            Rlog.d(TAG, "getCellLocation returning null due to NullPointerException " + ex2);
            return null;
        }
    }

    public List<NeighboringCellInfo> getNeighboringCellInfo(int simId) {
        try {
            return getITelephonyEx().getNeighboringCellInfoUsingSlotId(simId);
        } catch (RemoteException ex) {
            Rlog.d(TAG, "getNeighboringCellInfo returning null due to RemoteException " + ex);
            return null;
        } catch (NullPointerException ex2) {
            ex2.printStackTrace();
            return null;
        }
    }

    public int getPhoneType(int simId) {
        int[] subIds = SubscriptionManager.getSubId(simId);
        if (subIds == null) {
            return TelephonyManager.getDefault().getCurrentPhoneType(-1);
        }
        Rlog.e(TAG, "Deprecated! getPhoneType with simId " + simId + ", subId " + subIds[0]);
        return TelephonyManager.getDefault().getCurrentPhoneType(subIds[0]);
    }

    public String getNetworkOperatorName(int simId) {
        try {
            int subId = getSubIdBySlot(simId);
            Rlog.e(TAG, "Deprecated! getNetworkOperatorName with simId " + simId + " ,subId " + subId);
            return TelephonyManager.getDefault().getNetworkOperatorName(subId);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
            return "";
        }
    }

    public String getNetworkOperator(int simId) {
        try {
            int subId = getSubIdBySlot(simId);
            Rlog.e(TAG, "Deprecated! getNetworkOperator with simId " + simId + " ,subId " + subId);
            return TelephonyManager.getDefault().getNetworkOperator(subId);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
            return "";
        }
    }

    public boolean isNetworkRoaming(int simId) {
        try {
            int subId = getSubIdBySlot(simId);
            Rlog.e(TAG, "Deprecated! isNetworkRoaming with simId " + simId + " ,subId " + subId);
            return TelephonyManager.getDefault().isNetworkRoaming(subId);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public String getNetworkCountryIso(int simId) {
        try {
            int subId = getSubIdBySlot(simId);
            Rlog.e(TAG, "Deprecated! getNetworkCountryIso with simId " + simId + " ,subId " + subId);
            return TelephonyManager.getDefault().getNetworkCountryIso(subId);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
            return "";
        }
    }

    public int getNetworkType(int simId) {
        int subId = getSubIdBySlot(simId);
        Rlog.e(TAG, "Deprecated! getNetworkType with simId " + simId + " ,subId " + subId);
        return TelephonyManager.getDefault().getNetworkType(subId);
    }

    public boolean hasIccCard(int simId) {
        Rlog.d(TAG, "hasIccCard simId=" + simId);
        return TelephonyManager.getDefault().hasIccCard(simId);
    }

    private int getSubIdBySlot(int slot) {
        int[] subId = SubscriptionManager.getSubId(slot);
        Rlog.d(TAG, "getSubIdBySlot, simId " + slot + "subId " + (subId != null ? Integer.valueOf(subId[0]) : "invalid!"));
        return subId != null ? subId[0] : SubscriptionManager.getDefaultSubscriptionId();
    }

    public String getIccCardType(int subId) {
        String type = null;
        try {
            ITelephonyEx telephony = getITelephonyEx();
            if (telephony != null) {
                type = telephony.getIccCardType(subId);
            }
        } catch (RemoteException ex) {
            ex.printStackTrace();
        } catch (NullPointerException ex2) {
            ex2.printStackTrace();
        }
        Rlog.d(TAG, "getIccCardType sub " + subId + " ,icc type " + (type != null ? type : "null"));
        return type;
    }

    public boolean isAppTypeSupported(int slotId, int appType) {
        boolean isSupported = false;
        try {
            ITelephonyEx telephony = getITelephonyEx();
            if (telephony != null) {
                isSupported = telephony.isAppTypeSupported(slotId, appType);
            }
        } catch (RemoteException ex) {
            ex.printStackTrace();
        } catch (NullPointerException ex2) {
            ex2.printStackTrace();
        }
        Rlog.d(TAG, "isAppTypeSupported slotId: " + slotId + ", appType: " + appType + ", isSupported: " + isSupported);
        return isSupported;
    }

    public boolean isTestIccCard(int simId) {
        boolean result = false;
        Rlog.d(TAG, "isTestIccCard simId=" + simId);
        try {
            ITelephonyEx telephony = getITelephonyEx();
            if (telephony != null) {
                result = telephony.isTestIccCard(simId);
            }
        } catch (RemoteException ex) {
            ex.printStackTrace();
        } catch (NullPointerException ex2) {
            ex2.printStackTrace();
        }
        Rlog.d(TAG, "isTestIccCard sim " + simId + " ,result " + result);
        return result;
    }

    public int getSimState(int simId) {
        Rlog.d(TAG, "getSimState simId=" + simId);
        return TelephonyManager.getDefault().getSimState(simId);
    }

    public String getSimOperator(int simId) {
        int subId = getSubIdBySlot(simId);
        Rlog.e(TAG, "getSimOperator with simId " + simId + " ,subId " + subId);
        return TelephonyManager.getDefault().getSimOperator(subId);
    }

    public String getSimOperatorName(int simId) {
        int subId = getSubIdBySlot(simId);
        Rlog.e(TAG, "getSimOperatorName with simId " + simId + " ,subId " + subId);
        return TelephonyManager.getDefault().getSimOperatorName(subId);
    }

    public String getSimCountryIso(int simId) {
        int subId = getSubIdBySlot(simId);
        Rlog.e(TAG, "getSimCountryIso with simId " + simId + " ,subId " + subId);
        return TelephonyManager.getDefault().getSimCountryIso(subId);
    }

    public String getSimSerialNumber(int simId) {
        if (simId < 0 || simId >= TelephonyManager.getDefault().getSimCount()) {
            Rlog.e(TAG, "getSimSerialNumber with invalid simId " + simId);
            return null;
        }
        String iccId = SystemProperties.get(this.PROPERTY_ICCID_SIM[simId], "");
        if (iccId == null) {
            return iccId;
        }
        if (iccId.equals("N/A") || iccId.equals("")) {
            return null;
        }
        return iccId;
    }

    public String getSubscriberId(int simId) {
        int subId = getSubIdBySlot(simId);
        Rlog.e(TAG, "getSubscriberId with simId " + simId + " ,subId " + subId);
        return TelephonyManager.getDefault().getSubscriberId(subId);
    }

    public String getLine1Number(int simId) {
        int subId = getSubIdBySlot(simId);
        Rlog.e(TAG, "getLine1Number with simId " + simId + " ,subId " + subId);
        return TelephonyManager.getDefault().getLine1Number(subId);
    }

    public String getVoiceMailNumber(int simId) {
        int subId = getSubIdBySlot(simId);
        Rlog.e(TAG, "getVoiceMailNumber with simId " + simId + " ,subId " + subId);
        return TelephonyManager.getDefault().getVoiceMailNumber(subId);
    }

    public String getVoiceMailAlphaTag(int simId) {
        int subId = getSubIdBySlot(simId);
        Rlog.e(TAG, "getVoiceMailAlphaTag with simId " + simId + " ,subId " + subId);
        return TelephonyManager.getDefault().getVoiceMailAlphaTag(subId);
    }

    public int getCallState(int simId) {
        Rlog.d(TAG, "getCallState simId=" + simId);
        return TelephonyManager.getDefault().getCallState(getSubIdBySlot(simId));
    }

    public void listen(PhoneStateListener listener, int events, int simId) {
        Rlog.d(TAG, "deprecated api, listen simId=" + simId + ",events=" + events);
        if (this.mContext != null) {
            this.mContext.getPackageName();
        }
        TelephonyManager.getDefault().listen(listener, events);
    }

    private ITelephony getITelephony() {
        return ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
    }

    private ITelephonyEx getITelephonyEx() {
        return ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
    }

    private int getPhoneTypeFromProperty() {
        int type = SystemProperties.getInt("gsm.current.phone-type", getPhoneTypeFromNetworkType());
        return type;
    }

    private int getPhoneTypeFromNetworkType() {
        int phoneId = SubscriptionManager.getPhoneId(SubscriptionManager.getDefaultSubscriptionId());
        String mode = TelephonyManager.getTelephonyProperty(phoneId, "ro.telephony.default_network", null);
        if (mode != null) {
            TelephonyManager.getDefault();
            return TelephonyManager.getPhoneType(Integer.parseInt(mode));
        }
        return 0;
    }

    public String getScAddress(int subId) {
        Bundle result = getScAddressWithErroCode(subId);
        if (result != null) {
            return (String) result.getCharSequence(GET_SC_ADDRESS_KEY_ADDRESS);
        }
        return null;
    }

    public Bundle getScAddressWithErroCode(int subId) {
        try {
            return getITelephonyEx().getScAddressUsingSubId(subId);
        } catch (RemoteException e1) {
            e1.printStackTrace();
            return null;
        } catch (NullPointerException e2) {
            e2.printStackTrace();
            return null;
        }
    }

    public boolean setScAddress(int subId, String address) {
        try {
            return getITelephonyEx().setScAddressUsingSubId(subId, address);
        } catch (RemoteException e1) {
            e1.printStackTrace();
            return false;
        } catch (NullPointerException e2) {
            e2.printStackTrace();
            return false;
        }
    }

    private IPhoneSubInfo getSubscriberInfo() {
        return IPhoneSubInfo.Stub.asInterface(ServiceManager.getService("iphonesubinfo"));
    }

    public String getIsimImpi(int subId) {
        try {
            return getSubscriberInfo().getIsimImpiForSubscriber(subId);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getIsimDomain(int subId) {
        try {
            return getSubscriberInfo().getIsimDomainForSubscriber(subId);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String[] getIsimImpu(int subId) {
        try {
            return getSubscriberInfo().getIsimImpuForSubscriber(subId);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getIsimIst(int subId) {
        try {
            return getSubscriberInfo().getIsimIstForSubscriber(subId);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String[] getIsimPcscf(int subId) {
        try {
            return getSubscriberInfo().getIsimPcscfForSubscriber(subId);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getIsimGbabp() {
        return getIsimGbabp(SubscriptionManager.getDefaultSubscriptionId());
    }

    public String getIsimGbabp(int subId) {
        try {
            return getSubscriberInfo().getIsimGbabpForSubscriber(subId);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public void setIsimGbabp(String gbabp, Message onComplete) {
        setIsimGbabp(SubscriptionManager.getDefaultSubscriptionId(), gbabp, onComplete);
    }

    public void setIsimGbabp(int subId, String gbabp, Message onComplete) {
        try {
            getSubscriberInfo().setIsimGbabpForSubscriber(subId, gbabp, onComplete);
        } catch (RemoteException e) {
        } catch (NullPointerException e2) {
        }
    }

    private String getOpPackageName() {
        if (this.mContext != null) {
            return this.mContext.getOpPackageName();
        }
        return ActivityThread.currentOpPackageName();
    }

    public boolean getUsimService(int service) {
        return getUsimService(SubscriptionManager.getDefaultSubscriptionId(), service);
    }

    public boolean getUsimService(int subId, int service) {
        try {
            return getSubscriberInfo().getUsimServiceForSubscriber(subId, service, getOpPackageName());
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public String getUsimGbabp() {
        return getUsimGbabp(SubscriptionManager.getDefaultSubscriptionId());
    }

    public String getUsimGbabp(int subId) {
        try {
            return getSubscriberInfo().getUsimGbabpForSubscriber(subId);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public void setUsimGbabp(String gbabp, Message onComplete) {
        setUsimGbabp(SubscriptionManager.getDefaultSubscriptionId(), gbabp, onComplete);
    }

    public void setUsimGbabp(int subId, String gbabp, Message onComplete) {
        try {
            getSubscriberInfo().setUsimGbabpForSubscriber(subId, gbabp, onComplete);
        } catch (RemoteException e) {
        } catch (NullPointerException e2) {
        }
    }

    public boolean isInDsdaMode() {
        if (!SystemProperties.get("ro.mtk_switch_antenna", BaseParameters.FEATURE_MASK_3DNR_OFF).equals(BaseParameters.FEATURE_MASK_3DNR_ON) && SystemProperties.getInt("ro.boot.opt_c2k_lte_mode", 0) == 1) {
            TelephonyManager tm = TelephonyManager.getDefault();
            int simCount = tm.getSimCount();
            for (int i = 0; i < simCount; i++) {
                int[] allSubId = SubscriptionManager.getSubId(i);
                if (allSubId == null) {
                    Rlog.d(TAG, "isInDsdaMode, allSubId is null for slot" + i);
                } else {
                    int phoneType = tm.getCurrentPhoneType(allSubId[0]);
                    Rlog.d(TAG, "isInDsdaMode, allSubId[0]:" + allSubId[0] + ", phoneType:" + phoneType);
                    if (phoneType == 2) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isAirplanemodeAvailableNow() {
        try {
            return getITelephonyEx().isAirplanemodeAvailableNow();
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public boolean isInHomeNetwork(int subId) {
        try {
            return getITelephonyEx().isInHomeNetwork(subId);
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public String[] getSupportCardType(int slotId) {
        String[] values = null;
        if (slotId < 0 || slotId >= PROPERTY_RIL_FULL_UICC_TYPE.length) {
            Rlog.e(TAG, "getSupportCardType: invalid slotId " + slotId);
            return null;
        }
        String prop = SystemProperties.get(PROPERTY_RIL_FULL_UICC_TYPE[slotId], "");
        if (!prop.equals("") && prop.length() > 0) {
            values = prop.split(",");
        }
        Rlog.d(TAG, "getSupportCardType slotId " + slotId + ", prop value= " + prop + ", size= " + (values != null ? values.length : 0));
        return values;
    }

    public boolean isOmhEnable(int subId) {
        try {
            return getITelephonyEx().isOmhEnable(subId);
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public boolean isOmhCard(int subId) {
        try {
            return getITelephonyEx().isOmhCard(subId);
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public MmsIcpInfo getMmsIcpInfo(int subId) {
        try {
            return getITelephonyEx().getMmsIcpInfo(subId);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public MmsConfigInfo getMmsConfigInfo(int subId) {
        try {
            return getITelephonyEx().getMmsConfigInfo(subId);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public ArrayList<PhoneNumberUtils.EccEntry> getUserCustomizedEccList() {
        ArrayList<PhoneNumberUtils.EccEntry> result = new ArrayList<>();
        try {
            Bundle bundle = getITelephonyEx().getUserCustomizedEccList();
            if (bundle != null) {
                ArrayList<String> names = bundle.getStringArrayList("names");
                ArrayList<String> numbers = bundle.getStringArrayList("numbers");
                if (names == null || numbers == null || names.size() != numbers.size()) {
                    return result;
                }
                for (int i = 0; i < names.size(); i++) {
                    PhoneNumberUtils.EccEntry entry = new PhoneNumberUtils.EccEntry(names.get(i), numbers.get(i));
                    result.add(entry);
                }
            }
            Rlog.d(TAG, "getUserCustomizedEccList, result:" + result);
            return result;
        } catch (RemoteException e) {
            return result;
        } catch (NullPointerException e2) {
            return result;
        }
    }

    public boolean updateUserCustomizedEccList(ArrayList<PhoneNumberUtils.EccEntry> eccList) {
        Rlog.d(TAG, "updateUserCustomizedEccList, eccList:" + eccList);
        Bundle bundle = null;
        if (eccList != null) {
            bundle = new Bundle();
            ArrayList<String> names = new ArrayList<>();
            ArrayList<String> numbers = new ArrayList<>();
            for (PhoneNumberUtils.EccEntry entry : eccList) {
                names.add(entry.getName());
                numbers.add(entry.getEcc());
            }
            bundle.putStringArrayList("names", names);
            bundle.putStringArrayList("numbers", numbers);
        }
        try {
            return getITelephonyEx().updateUserCustomizedEccList(bundle);
        } catch (RemoteException ex) {
            Rlog.d(TAG, "updateUserCustomizedEccList, RemoteException:" + ex);
            return false;
        } catch (NullPointerException ex2) {
            Rlog.d(TAG, "updateUserCustomizedEccList, NullPointerException:" + ex2);
            return false;
        }
    }

    public boolean isUserCustomizedEcc(String number) {
        if (number == null) {
            return false;
        }
        try {
            return getITelephonyEx().isUserCustomizedEcc(number);
        } catch (RemoteException ex) {
            Rlog.d(TAG, "isUserCustomizedEcc, RemoteException:" + ex);
            return false;
        } catch (NullPointerException ex2) {
            Rlog.d(TAG, "isUserCustomizedEcc, NullPointerException:" + ex2);
            return false;
        }
    }

    public int[] getCallForwardingFc(int type, int subId) {
        try {
            return getITelephonyEx().getCallForwardingFc(type, subId);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public int[] getCallWaitingFc(int subId) {
        try {
            return getITelephonyEx().getCallWaitingFc(subId);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public int[] getDonotDisturbFc(int subId) {
        try {
            return getITelephonyEx().getDonotDisturbFc(subId);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public int[] getVoiceMailRetrieveFc(int subId) {
        try {
            return getITelephonyEx().getVMRetrieveFc(subId);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public boolean isCt3gDualMode(int slotId) {
        if (slotId < 0 || slotId >= PROPERTY_RIL_CT3G.length) {
            Rlog.e(TAG, "isCt3gDualMode: invalid slotId " + slotId);
            return false;
        }
        String result = SystemProperties.get(PROPERTY_RIL_CT3G[slotId], "");
        Rlog.d(TAG, "isCt3gDualMode:  " + result);
        return BaseParameters.FEATURE_MASK_3DNR_ON.equals(result);
    }

    public IccCardConstants.CardType getCdmaCardType(int slotId) {
        if (slotId < 0 || slotId >= PROPERTY_RIL_CT3G.length) {
            Rlog.e(TAG, "getCdmaCardType: invalid slotId " + slotId);
            return null;
        }
        IccCardConstants.CardType mCdmaCardType = IccCardConstants.CardType.UNKNOW_CARD;
        String result = SystemProperties.get(PROPERTY_RIL_CDMA_CARD_TYPE[slotId], "");
        if (!result.equals("")) {
            int cardtype = Integer.parseInt(result);
            mCdmaCardType = IccCardConstants.CardType.getCardTypeFromInt(cardtype);
        }
        Rlog.d(TAG, "getCdmaCardType result: " + result + "  mCdmaCardType: " + mCdmaCardType);
        return mCdmaCardType;
    }

    public String getUimSubscriberId(int subId) {
        int phoneId = SubscriptionManager.getPhoneId(subId);
        if (phoneId < 0 || phoneId >= PROPERTY_UIM_SUBSCRIBER_ID.length) {
            Rlog.e(TAG, "getUimSubscriberId: invalid phoneId " + phoneId);
            return null;
        }
        String imsi = SystemProperties.get(PROPERTY_UIM_SUBSCRIBER_ID[phoneId], "");
        Rlog.d(TAG, "getUimSubscriberId phoneId = " + phoneId + ", imsi = " + imsi);
        return imsi;
    }

    public int getIccAppFamily(int slotId) {
        try {
            return getITelephonyEx().getIccAppFamily(slotId);
        } catch (RemoteException e) {
            return 0;
        } catch (NullPointerException e2) {
            return 0;
        }
    }

    public String getPrlVersion(int subId) {
        int slotId = SubscriptionManager.getSlotId(subId);
        String prlVersion = SystemProperties.get(PRLVERSION + slotId, "");
        Rlog.d(TAG, "getPrlversion PRLVERSION subId = " + subId + " key = " + PRLVERSION + slotId + " value = " + prlVersion);
        return prlVersion;
    }

    public int getCdmaSubscriptionActStatus(int subId) {
        try {
            int actStatus = getITelephonyEx().getCdmaSubscriptionActStatus(subId);
            return actStatus;
        } catch (RemoteException e) {
            Rlog.d(TAG, "fail to getCdmaSubscriptionActStatus due to RemoteException");
            return 0;
        } catch (NullPointerException e2) {
            Rlog.d(TAG, "fail to getCdmaSubscriptionActStatus due to NullPointerException");
            return 0;
        }
    }

    public static class SetDefaultSubResultCallback extends ISetDefaultSubResultCallback.Stub {
        public void onComplete(boolean result) {
            Rlog.d(TelephonyManagerEx.TAG, "onComplete: NOT OVERRIDDEN, result:" + result);
        }
    }

    public boolean canSwitchDefaultSubId() {
        try {
            return getITelephonyEx().canSwitchDefaultSubId();
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public boolean setDefaultSubIdForAll(int type, int subId, SetDefaultSubResultCallback callback) {
        try {
            return getITelephonyEx().setDefaultSubIdForAll(type, subId, callback);
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public void setEccInProgress(boolean state) {
        try {
            getITelephonyEx().setEccInProgress(state);
        } catch (RemoteException e) {
        } catch (NullPointerException e2) {
        }
    }

    public boolean isEccInProgress() {
        try {
            return getITelephonyEx().isEccInProgress();
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public boolean isC2kSupported() {
        boolean result = RatConfiguration.isC2kSupported();
        Rlog.d(TAG, "[isC2kSupported] " + result);
        return result;
    }

    public boolean isLteFddSupported() {
        boolean result = RatConfiguration.isLteFddSupported();
        Rlog.d(TAG, "[isLteFddSupported] " + result);
        return result;
    }

    public boolean isLteTddSupported() {
        boolean result = RatConfiguration.isLteTddSupported();
        Rlog.d(TAG, "[isLteTddSupported] " + result);
        return result;
    }

    public boolean isWcdmaSupported() {
        boolean result = RatConfiguration.isWcdmaSupported();
        Rlog.d(TAG, "[isWcdmaSupported] " + result);
        return result;
    }

    public boolean isTdscdmaSupported() {
        boolean result = RatConfiguration.isTdscdmaSupported();
        Rlog.d(TAG, "[isTdscdmaSupported] " + result);
        return result;
    }

    public boolean isGsmSupported() {
        boolean result = RatConfiguration.isGsmSupported();
        Rlog.d(TAG, "[isGsmSupported] " + result);
        return result;
    }

    public boolean isImsRegistered(int subId) {
        try {
            return getITelephonyEx().isImsRegistered(subId);
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public boolean isVolteEnabled(int subId) {
        try {
            return getITelephonyEx().isVolteEnabled(subId);
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public boolean isWifiCallingEnabled(int subId) {
        try {
            return getITelephonyEx().isWifiCallingEnabled(subId);
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public String getImei(int slotId) {
        try {
            return getITelephonyEx().getImei(slotId);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getMeid(int slotId) {
        try {
            return getITelephonyEx().getMeid(slotId);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public void enablePseudoBSMonitor(int slotId, boolean reportOn, int reportRateInSeconds) {
        try {
            getITelephonyEx().enablePseudoBSMonitor(slotId, reportOn, reportRateInSeconds);
        } catch (RemoteException e) {
        } catch (NullPointerException e2) {
        }
    }

    public void disablePseudoBSMonitor(int slotId) {
        try {
            getITelephonyEx().disablePseudoBSMonitor(slotId);
        } catch (RemoteException e) {
        } catch (NullPointerException e2) {
        }
    }

    public List<PseudoBSRecord> queryPseudoBSRecords(int slotId) {
        try {
            return getITelephonyEx().queryPseudoBSRecords(slotId);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public void setTelLog(boolean enable) {
        try {
            getITelephonyEx().setTelLog(enable);
        } catch (RemoteException e) {
        } catch (NullPointerException e2) {
        }
    }
}
