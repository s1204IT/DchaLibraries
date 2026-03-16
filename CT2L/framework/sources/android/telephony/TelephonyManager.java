package android.telephony;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.ProxyInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.R;
import com.android.internal.telecom.ITelecomService;
import com.android.internal.telephony.IPhoneSubInfo;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.ITelephonyRegistry;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyProperties;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TelephonyManager {
    public static final String ACTION_PHONE_STATE_CHANGED = "android.intent.action.PHONE_STATE";
    public static final String ACTION_PRECISE_CALL_STATE_CHANGED = "android.intent.action.PRECISE_CALL_STATE";
    public static final String ACTION_PRECISE_DATA_CONNECTION_STATE_CHANGED = "android.intent.action.PRECISE_DATA_CONNECTION_STATE_CHANGED";
    public static final String ACTION_RESPOND_VIA_MESSAGE = "android.intent.action.RESPOND_VIA_MESSAGE";
    public static final int CALL_STATE_IDLE = 0;
    public static final int CALL_STATE_OFFHOOK = 2;
    public static final int CALL_STATE_RINGING = 1;
    public static final int CARRIER_PRIVILEGE_STATUS_ERROR_LOADING_RULES = -2;
    public static final int CARRIER_PRIVILEGE_STATUS_HAS_ACCESS = 1;
    public static final int CARRIER_PRIVILEGE_STATUS_NO_ACCESS = 0;
    public static final int CARRIER_PRIVILEGE_STATUS_RULES_NOT_LOADED = -1;
    public static final int DATA_ACTIVITY_DORMANT = 4;
    public static final int DATA_ACTIVITY_IN = 1;
    public static final int DATA_ACTIVITY_INOUT = 3;
    public static final int DATA_ACTIVITY_NONE = 0;
    public static final int DATA_ACTIVITY_OUT = 2;
    public static final int DATA_CONNECTED = 2;
    public static final int DATA_CONNECTING = 1;
    public static final int DATA_DISCONNECTED = 0;
    public static final int DATA_SUSPENDED = 3;
    public static final int DATA_UNKNOWN = -1;
    public static final String EXTRA_BACKGROUND_CALL_STATE = "background_state";
    public static final String EXTRA_DATA_APN = "apn";
    public static final String EXTRA_DATA_APN_TYPE = "apnType";
    public static final String EXTRA_DATA_CHANGE_REASON = "reason";
    public static final String EXTRA_DATA_FAILURE_CAUSE = "failCause";
    public static final String EXTRA_DATA_LINK_PROPERTIES_KEY = "linkProperties";
    public static final String EXTRA_DATA_NETWORK_TYPE = "networkType";
    public static final String EXTRA_DATA_STATE = "state";
    public static final String EXTRA_DISCONNECT_CAUSE = "disconnect_cause";
    public static final String EXTRA_FOREGROUND_CALL_STATE = "foreground_state";
    public static final String EXTRA_INCOMING_NUMBER = "incoming_number";
    public static final String EXTRA_PRECISE_DISCONNECT_CAUSE = "precise_disconnect_cause";
    public static final String EXTRA_RINGING_CALL_STATE = "ringing_state";
    public static final String EXTRA_STATE = "state";
    public static final int NETWORK_CLASS_2_G = 1;
    public static final int NETWORK_CLASS_3_G = 2;
    public static final int NETWORK_CLASS_4_G = 3;
    public static final int NETWORK_CLASS_UNKNOWN = 0;
    public static final int NETWORK_TYPE_1xRTT = 7;
    public static final int NETWORK_TYPE_CDMA = 4;
    public static final int NETWORK_TYPE_EDGE = 2;
    public static final int NETWORK_TYPE_EHRPD = 14;
    public static final int NETWORK_TYPE_EVDO_0 = 5;
    public static final int NETWORK_TYPE_EVDO_A = 6;
    public static final int NETWORK_TYPE_EVDO_B = 12;
    public static final int NETWORK_TYPE_GPRS = 1;
    public static final int NETWORK_TYPE_GSM = 16;
    public static final int NETWORK_TYPE_HSDPA = 8;
    public static final int NETWORK_TYPE_HSPA = 10;
    public static final int NETWORK_TYPE_HSPAP = 15;
    public static final int NETWORK_TYPE_HSUPA = 9;
    public static final int NETWORK_TYPE_IDEN = 11;
    public static final int NETWORK_TYPE_LTE = 13;
    public static final int NETWORK_TYPE_UMTS = 3;
    public static final int NETWORK_TYPE_UNKNOWN = 0;
    public static final int PHONE_TYPE_CDMA = 2;
    public static final int PHONE_TYPE_GSM = 1;
    public static final int PHONE_TYPE_NONE = 0;
    public static final int PHONE_TYPE_SIP = 3;
    public static final int SIM_STATE_ABSENT = 1;
    public static final int SIM_STATE_CARD_IO_ERROR = 8;
    public static final int SIM_STATE_NETWORK_LOCKED = 4;
    public static final int SIM_STATE_NOT_READY = 6;
    public static final int SIM_STATE_PERM_DISABLED = 7;
    public static final int SIM_STATE_PIN_REQUIRED = 2;
    public static final int SIM_STATE_PUK_REQUIRED = 3;
    public static final int SIM_STATE_READY = 5;
    public static final int SIM_STATE_UNKNOWN = 0;
    private static final String TAG = "TelephonyManager";
    private static ITelephonyRegistry sRegistry;
    private final Context mContext;
    private SubscriptionManager mSubscriptionManager;
    private static String multiSimConfig = SystemProperties.get(TelephonyProperties.PROPERTY_MULTI_SIM_CONFIG);
    private static TelephonyManager sInstance = new TelephonyManager();
    public static final String EXTRA_STATE_IDLE = PhoneConstants.State.IDLE.toString();
    public static final String EXTRA_STATE_RINGING = PhoneConstants.State.RINGING.toString();
    public static final String EXTRA_STATE_OFFHOOK = PhoneConstants.State.OFFHOOK.toString();
    private static final String sKernelCmdLine = getProcCmdLine();
    private static final Pattern sProductTypePattern = Pattern.compile("\\sproduct_type\\s*=\\s*(\\w+)");
    private static final String sLteOnCdmaProductType = SystemProperties.get(TelephonyProperties.PROPERTY_LTE_ON_CDMA_PRODUCT_TYPE, ProxyInfo.LOCAL_EXCL_LIST);

    public enum MultiSimVariants {
        DSDS,
        DSDA,
        TSTS,
        UNKNOWN
    }

    public interface WifiCallingChoices {
        public static final int ALWAYS_USE = 0;
        public static final int ASK_EVERY_TIME = 1;
        public static final int NEVER_USE = 2;
    }

    public TelephonyManager(Context context) {
        Context appContext = context.getApplicationContext();
        if (appContext != null) {
            this.mContext = appContext;
        } else {
            this.mContext = context;
        }
        this.mSubscriptionManager = SubscriptionManager.from(this.mContext);
        if (sRegistry == null) {
            sRegistry = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry"));
        }
    }

    private TelephonyManager() {
        this.mContext = null;
    }

    public static TelephonyManager getDefault() {
        return sInstance;
    }

    public MultiSimVariants getMultiSimConfiguration() {
        String mSimConfig = SystemProperties.get(TelephonyProperties.PROPERTY_MULTI_SIM_CONFIG);
        if (mSimConfig.equals("dsds")) {
            return MultiSimVariants.DSDS;
        }
        if (mSimConfig.equals("dsda")) {
            return MultiSimVariants.DSDA;
        }
        if (mSimConfig.equals("tsts")) {
            return MultiSimVariants.TSTS;
        }
        return MultiSimVariants.UNKNOWN;
    }

    public int getPhoneCount() {
        switch (getMultiSimConfiguration()) {
        }
        return 1;
    }

    public static TelephonyManager from(Context context) {
        return (TelephonyManager) context.getSystemService("phone");
    }

    public boolean isMultiSimEnabled() {
        return multiSimConfig.equals("dsds") || multiSimConfig.equals("dsda") || multiSimConfig.equals("tsts");
    }

    public String getDeviceSoftwareVersion() {
        return getDeviceSoftwareVersion(getDefaultSim());
    }

    public String getDeviceSoftwareVersion(int slotId) {
        int[] subId = SubscriptionManager.getSubId(slotId);
        if (subId == null || subId.length == 0) {
            return null;
        }
        try {
            return getSubscriberInfo().getDeviceSvnUsingSubId(subId[0]);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getDeviceId() {
        try {
            return getITelephony().getDeviceId();
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getDeviceId(int slotId) {
        try {
            return getSubscriberInfo().getDeviceIdForPhone(slotId);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getImei() {
        return getImei(getDefaultSim());
    }

    public String getImei(int slotId) {
        int[] subId = SubscriptionManager.getSubId(slotId);
        try {
            return getSubscriberInfo().getImeiForSubscriber(subId[0]);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getNai() {
        return getNai(getDefaultSim());
    }

    public String getNai(int slotId) {
        int[] subId = SubscriptionManager.getSubId(slotId);
        try {
            String nai = getSubscriberInfo().getNaiForSubscriber(subId[0]);
            if (Log.isLoggable(TAG, 2)) {
                Rlog.v(TAG, "Nai = " + nai);
                return nai;
            }
            return nai;
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public CellLocation getCellLocation() {
        try {
            Bundle bundle = getITelephony().getCellLocation();
            if (bundle.isEmpty()) {
                return null;
            }
            CellLocation cl = CellLocation.newFromBundle(bundle);
            if (cl.isEmpty()) {
                return null;
            }
            return cl;
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public void enableLocationUpdates() {
        enableLocationUpdates(getDefaultSubscription());
    }

    public void enableLocationUpdates(int subId) {
        try {
            getITelephony().enableLocationUpdatesForSubscriber(subId);
        } catch (RemoteException e) {
        } catch (NullPointerException e2) {
        }
    }

    public void disableLocationUpdates() {
        disableLocationUpdates(getDefaultSubscription());
    }

    public void disableLocationUpdates(int subId) {
        try {
            getITelephony().disableLocationUpdatesForSubscriber(subId);
        } catch (RemoteException e) {
        } catch (NullPointerException e2) {
        }
    }

    public List<NeighboringCellInfo> getNeighboringCellInfo() {
        try {
            return getITelephony().getNeighboringCellInfo(this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public int getCurrentPhoneType() {
        return getCurrentPhoneType(getDefaultSubscription());
    }

    public int getCurrentPhoneType(int subId) {
        int phoneTypeFromProperty;
        int phoneId = SubscriptionManager.getPhoneId(subId);
        try {
            ITelephony telephony = getITelephony();
            if (telephony != null) {
                phoneTypeFromProperty = telephony.getActivePhoneTypeForSubscriber(subId);
            } else {
                phoneTypeFromProperty = getPhoneTypeFromProperty(phoneId);
            }
            return phoneTypeFromProperty;
        } catch (RemoteException e) {
            return getPhoneTypeFromProperty(phoneId);
        } catch (NullPointerException e2) {
            return getPhoneTypeFromProperty(phoneId);
        }
    }

    public int getPhoneType() {
        if (isVoiceCapable()) {
            return getCurrentPhoneType();
        }
        return 0;
    }

    private int getPhoneTypeFromProperty() {
        return getPhoneTypeFromProperty(getDefaultPhone());
    }

    private int getPhoneTypeFromProperty(int phoneId) {
        String type = getTelephonyProperty(phoneId, TelephonyProperties.CURRENT_ACTIVE_PHONE, null);
        return (type == null || type.equals(ProxyInfo.LOCAL_EXCL_LIST)) ? getPhoneTypeFromNetworkType(phoneId) : Integer.parseInt(type);
    }

    private int getPhoneTypeFromNetworkType() {
        return getPhoneTypeFromNetworkType(getDefaultPhone());
    }

    private int getPhoneTypeFromNetworkType(int phoneId) {
        String mode = getTelephonyProperty(phoneId, "ro.telephony.default_network", null);
        if (mode != null) {
            return getPhoneType(Integer.parseInt(mode));
        }
        return 0;
    }

    public static int getPhoneType(int networkMode) {
        switch (networkMode) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 9:
            case 10:
            case 12:
                return 1;
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
                return 2;
            case 11:
                return getLteOnCdmaModeStatic() != 1 ? 1 : 2;
            default:
                return 1;
        }
    }

    private static String getProcCmdLine() throws Throwable {
        FileInputStream is;
        String cmdline = ProxyInfo.LOCAL_EXCL_LIST;
        FileInputStream is2 = null;
        try {
            try {
                is = new FileInputStream("/proc/cmdline");
            } catch (IOException e) {
                e = e;
            }
        } catch (Throwable th) {
            th = th;
        }
        try {
            byte[] buffer = new byte[2048];
            int count = is.read(buffer);
            if (count > 0) {
                cmdline = new String(buffer, 0, count);
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e2) {
                }
            }
        } catch (IOException e3) {
            e = e3;
            is2 = is;
            Rlog.d(TAG, "No /proc/cmdline exception=" + e);
            if (is2 != null) {
                try {
                    is2.close();
                } catch (IOException e4) {
                }
            }
        } catch (Throwable th2) {
            th = th2;
            is2 = is;
            if (is2 != null) {
                try {
                    is2.close();
                } catch (IOException e5) {
                }
            }
            throw th;
        }
        Rlog.d(TAG, "/proc/cmdline=" + cmdline);
        return cmdline;
    }

    public static int getLteOnCdmaModeStatic() {
        String productType = ProxyInfo.LOCAL_EXCL_LIST;
        int curVal = SystemProperties.getInt(TelephonyProperties.PROPERTY_LTE_ON_CDMA_DEVICE, -1);
        int retVal = curVal;
        if (retVal == -1) {
            Matcher matcher = sProductTypePattern.matcher(sKernelCmdLine);
            if (matcher.find()) {
                productType = matcher.group(1);
                if (sLteOnCdmaProductType.equals(productType)) {
                    retVal = 1;
                } else {
                    retVal = 0;
                }
            } else {
                retVal = 0;
            }
        }
        Rlog.d(TAG, "getLteOnCdmaMode=" + retVal + " curVal=" + curVal + " product_type='" + productType + "' lteOnCdmaProductType='" + sLteOnCdmaProductType + "'");
        return retVal;
    }

    public String getNetworkOperatorName() {
        return getNetworkOperatorName(getDefaultSubscription());
    }

    public String getNetworkOperatorName(int subId) {
        int phoneId = SubscriptionManager.getPhoneId(subId);
        return getTelephonyProperty(phoneId, TelephonyProperties.PROPERTY_OPERATOR_ALPHA, ProxyInfo.LOCAL_EXCL_LIST);
    }

    public String getNetworkOperator() {
        return getNetworkOperatorForPhone(getDefaultPhone());
    }

    public String getNetworkOperatorForSubscription(int subId) {
        int phoneId = SubscriptionManager.getPhoneId(subId);
        return getNetworkOperatorForPhone(phoneId);
    }

    public String getNetworkOperatorForPhone(int phoneId) {
        return getTelephonyProperty(phoneId, TelephonyProperties.PROPERTY_OPERATOR_NUMERIC, ProxyInfo.LOCAL_EXCL_LIST);
    }

    public boolean isNetworkRoaming() {
        return isNetworkRoaming(getDefaultSubscription());
    }

    public boolean isNetworkRoaming(int subId) {
        int phoneId = SubscriptionManager.getPhoneId(subId);
        return Boolean.parseBoolean(getTelephonyProperty(phoneId, TelephonyProperties.PROPERTY_OPERATOR_ISROAMING, null));
    }

    public String getNetworkCountryIso() {
        return getNetworkCountryIsoForPhone(getDefaultPhone());
    }

    public String getNetworkCountryIsoForSubscription(int subId) {
        int phoneId = SubscriptionManager.getPhoneId(subId);
        return getNetworkCountryIsoForPhone(phoneId);
    }

    public String getNetworkCountryIsoForPhone(int phoneId) {
        return getTelephonyProperty(phoneId, TelephonyProperties.PROPERTY_OPERATOR_ISO_COUNTRY, ProxyInfo.LOCAL_EXCL_LIST);
    }

    public int getNetworkType() {
        return getDataNetworkType();
    }

    public int getNetworkType(int subId) {
        try {
            ITelephony telephony = getITelephony();
            if (telephony != null) {
                return telephony.getNetworkTypeForSubscriber(subId);
            }
            return 0;
        } catch (RemoteException e) {
            return 0;
        } catch (NullPointerException e2) {
            return 0;
        }
    }

    public int getDataNetworkType() {
        return getDataNetworkType(getDefaultSubscription());
    }

    public int getDataNetworkType(int subId) {
        try {
            ITelephony telephony = getITelephony();
            if (telephony != null) {
                return telephony.getDataNetworkTypeForSubscriber(subId);
            }
            return 0;
        } catch (RemoteException e) {
            return 0;
        } catch (NullPointerException e2) {
            return 0;
        }
    }

    public int getVoiceNetworkType() {
        return getVoiceNetworkType(getDefaultSubscription());
    }

    public int getVoiceNetworkType(int subId) {
        try {
            ITelephony telephony = getITelephony();
            if (telephony != null) {
                return telephony.getVoiceNetworkTypeForSubscriber(subId);
            }
            return 0;
        } catch (RemoteException e) {
            return 0;
        } catch (NullPointerException e2) {
            return 0;
        }
    }

    public static int getNetworkClass(int networkType) {
        switch (networkType) {
            case 1:
            case 2:
            case 4:
            case 7:
            case 11:
            case 16:
                return 1;
            case 3:
            case 5:
            case 6:
            case 8:
            case 9:
            case 10:
            case 12:
            case 14:
            case 15:
                return 2;
            case 13:
                return 3;
            default:
                return 0;
        }
    }

    public String getNetworkTypeName() {
        return getNetworkTypeName(getNetworkType());
    }

    public static String getNetworkTypeName(int type) {
        switch (type) {
            case 1:
                return "GPRS";
            case 2:
                return "EDGE";
            case 3:
                return "UMTS";
            case 4:
                return "CDMA";
            case 5:
                return "CDMA - EvDo rev. 0";
            case 6:
                return "CDMA - EvDo rev. A";
            case 7:
                return "CDMA - 1xRTT";
            case 8:
                return "HSDPA";
            case 9:
                return "HSUPA";
            case 10:
                return "HSPA";
            case 11:
                return "iDEN";
            case 12:
                return "CDMA - EvDo rev. B";
            case 13:
                return "LTE";
            case 14:
                return "CDMA - eHRPD";
            case 15:
                return "HSPA+";
            case 16:
                return "GSM";
            default:
                return IccCardConstants.INTENT_VALUE_ICC_UNKNOWN;
        }
    }

    public boolean hasIccCard() {
        return hasIccCard(getDefaultSim());
    }

    public boolean hasIccCard(int slotId) {
        try {
            return getITelephony().hasIccCardUsingSlotId(slotId);
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public int getSimState() {
        return getSimState(getDefaultSim());
    }

    public int getSimState(int slotIdx) {
        int[] subId = SubscriptionManager.getSubId(slotIdx);
        if (subId == null || subId.length == 0) {
            Rlog.d(TAG, "getSimState:- empty subId return SIM_STATE_ABSENT");
            return 0;
        }
        int simState = SubscriptionManager.getSimStateForSubscriber(subId[0]);
        Rlog.d(TAG, "getSimState: simState=" + simState + " slotIdx=" + slotIdx);
        return simState;
    }

    public String getSimOperator() {
        return getSimOperatorNumeric();
    }

    public String getSimOperator(int subId) {
        return getSimOperatorNumericForSubscription(subId);
    }

    public String getSimOperatorNumeric() {
        int subId = SubscriptionManager.getDefaultDataSubId();
        if (!SubscriptionManager.isUsableSubIdValue(subId)) {
            subId = SubscriptionManager.getDefaultSmsSubId();
            if (!SubscriptionManager.isUsableSubIdValue(subId)) {
                subId = SubscriptionManager.getDefaultVoiceSubId();
                if (!SubscriptionManager.isUsableSubIdValue(subId)) {
                    subId = SubscriptionManager.getDefaultSubId();
                }
            }
        }
        Rlog.d(TAG, "getSimOperatorNumeric(): default subId=" + subId);
        return getSimOperatorNumericForSubscription(subId);
    }

    public String getSimOperatorNumericForSubscription(int subId) {
        int phoneId = SubscriptionManager.getPhoneId(subId);
        return getSimOperatorNumericForPhone(phoneId);
    }

    public String getSimOperatorNumericForPhone(int phoneId) {
        return getTelephonyProperty(phoneId, TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC, ProxyInfo.LOCAL_EXCL_LIST);
    }

    public String getSimOperatorName() {
        return getSimOperatorNameForPhone(getDefaultPhone());
    }

    public String getSimOperatorNameForSubscription(int subId) {
        int phoneId = SubscriptionManager.getPhoneId(subId);
        return getSimOperatorNameForPhone(phoneId);
    }

    public String getSimOperatorNameForPhone(int phoneId) {
        return getTelephonyProperty(phoneId, TelephonyProperties.PROPERTY_ICC_OPERATOR_ALPHA, ProxyInfo.LOCAL_EXCL_LIST);
    }

    public String getSimCountryIso() {
        return getSimCountryIsoForPhone(getDefaultPhone());
    }

    public String getSimCountryIso(int subId) {
        return getSimCountryIsoForSubscription(subId);
    }

    public String getSimCountryIsoForSubscription(int subId) {
        int phoneId = SubscriptionManager.getPhoneId(subId);
        return getSimCountryIsoForPhone(phoneId);
    }

    public String getSimCountryIsoForPhone(int phoneId) {
        return getTelephonyProperty(phoneId, TelephonyProperties.PROPERTY_ICC_OPERATOR_ISO_COUNTRY, ProxyInfo.LOCAL_EXCL_LIST);
    }

    public String getSimSerialNumber() {
        return getSimSerialNumber(getDefaultSubscription());
    }

    public String getSimSerialNumber(int subId) {
        try {
            return getSubscriberInfo().getIccSerialNumberForSubscriber(subId);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public int getLteOnCdmaMode() {
        return getLteOnCdmaMode(getDefaultSubscription());
    }

    public int getLteOnCdmaMode(int subId) {
        try {
            return getITelephony().getLteOnCdmaModeForSubscriber(subId);
        } catch (RemoteException e) {
            return -1;
        } catch (NullPointerException e2) {
            return -1;
        }
    }

    public String getSubscriberId() {
        return getSubscriberId(getDefaultSubscription());
    }

    public String getSubscriberId(int subId) {
        try {
            return getSubscriberInfo().getSubscriberIdForSubscriber(subId);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getGroupIdLevel1() {
        try {
            return getSubscriberInfo().getGroupIdLevel1();
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getGroupIdLevel1(int subId) {
        try {
            return getSubscriberInfo().getGroupIdLevel1ForSubscriber(subId);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getLine1Number() {
        return getLine1NumberForSubscriber(getDefaultSubscription());
    }

    public String getLine1NumberForSubscriber(int subId) {
        String number = null;
        try {
            number = getITelephony().getLine1NumberForDisplay(subId);
        } catch (RemoteException e) {
        } catch (NullPointerException e2) {
        }
        if (number == null) {
            try {
                return getSubscriberInfo().getLine1NumberForSubscriber(subId);
            } catch (RemoteException e3) {
                return null;
            } catch (NullPointerException e4) {
                return null;
            }
        }
        return number;
    }

    public boolean setLine1NumberForDisplay(String alphaTag, String number) {
        return setLine1NumberForDisplayForSubscriber(getDefaultSubscription(), alphaTag, number);
    }

    public boolean setLine1NumberForDisplayForSubscriber(int subId, String alphaTag, String number) {
        try {
            return getITelephony().setLine1NumberForDisplayForSubscriber(subId, alphaTag, number);
        } catch (RemoteException | NullPointerException e) {
            return false;
        }
    }

    public String getLine1AlphaTag() {
        return getLine1AlphaTagForSubscriber(getDefaultSubscription());
    }

    public String getLine1AlphaTagForSubscriber(int subId) {
        String alphaTag = null;
        try {
            alphaTag = getITelephony().getLine1AlphaTagForDisplay(subId);
        } catch (RemoteException e) {
        } catch (NullPointerException e2) {
        }
        if (alphaTag == null) {
            try {
                return getSubscriberInfo().getLine1AlphaTagForSubscriber(subId);
            } catch (RemoteException e3) {
                return null;
            } catch (NullPointerException e4) {
                return null;
            }
        }
        return alphaTag;
    }

    public String[] getMergedSubscriberIds() {
        try {
            return getITelephony().getMergedSubscriberIds();
        } catch (RemoteException | NullPointerException e) {
            return null;
        }
    }

    public String getMsisdn() {
        return getMsisdn(getDefaultSubscription());
    }

    public String getMsisdn(int subId) {
        try {
            return getSubscriberInfo().getMsisdnForSubscriber(subId);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getVoiceMailNumber() {
        return getVoiceMailNumber(getDefaultSubscription());
    }

    public String getVoiceMailNumber(int subId) {
        try {
            return getSubscriberInfo().getVoiceMailNumberForSubscriber(subId);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getCompleteVoiceMailNumber() {
        return getCompleteVoiceMailNumber(getDefaultSubscription());
    }

    public String getCompleteVoiceMailNumber(int subId) {
        try {
            return getSubscriberInfo().getCompleteVoiceMailNumberForSubscriber(subId);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public boolean setVoiceMailNumber(String alphaTag, String number) {
        return setVoiceMailNumber(getDefaultSubscription(), alphaTag, number);
    }

    public boolean setVoiceMailNumber(int subId, String alphaTag, String number) {
        try {
            return getITelephony().setVoiceMailNumber(subId, alphaTag, number);
        } catch (RemoteException | NullPointerException e) {
            return false;
        }
    }

    public int getVoiceMessageCount() {
        return getVoiceMessageCount(getDefaultSubscription());
    }

    public int getVoiceMessageCount(int subId) {
        try {
            return getITelephony().getVoiceMessageCountForSubscriber(subId);
        } catch (RemoteException e) {
            return 0;
        } catch (NullPointerException e2) {
            return 0;
        }
    }

    public String getVoiceMailAlphaTag() {
        return getVoiceMailAlphaTag(getDefaultSubscription());
    }

    public String getVoiceMailAlphaTag(int subId) {
        try {
            return getSubscriberInfo().getVoiceMailAlphaTagForSubscriber(subId);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getIsimImpi() {
        try {
            return getSubscriberInfo().getIsimImpi();
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getIsimDomain() {
        try {
            return getSubscriberInfo().getIsimDomain();
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String[] getIsimImpu() {
        try {
            return getSubscriberInfo().getIsimImpu();
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    private IPhoneSubInfo getSubscriberInfo() {
        return IPhoneSubInfo.Stub.asInterface(ServiceManager.getService("iphonesubinfo"));
    }

    public int getCallState() {
        try {
            return getTelecomService().getCallState();
        } catch (RemoteException | NullPointerException e) {
            return 0;
        }
    }

    public int getCallState(int subId) {
        try {
            return getITelephony().getCallStateForSubscriber(subId);
        } catch (RemoteException e) {
            return 0;
        } catch (NullPointerException e2) {
            return 0;
        }
    }

    public int getDataActivity() {
        try {
            return getITelephony().getDataActivity();
        } catch (RemoteException e) {
            return 0;
        } catch (NullPointerException e2) {
            return 0;
        }
    }

    public int getDataState() {
        try {
            return getITelephony().getDataState();
        } catch (RemoteException e) {
            return 0;
        } catch (NullPointerException e2) {
            return 0;
        }
    }

    private ITelephony getITelephony() {
        return ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
    }

    private ITelecomService getTelecomService() {
        return ITelecomService.Stub.asInterface(ServiceManager.getService(Context.TELECOM_SERVICE));
    }

    public void listen(PhoneStateListener listener, int events) {
        String pkgForDebug = this.mContext != null ? this.mContext.getPackageName() : MediaStore.UNKNOWN_STRING;
        try {
            Boolean notifyNow = Boolean.valueOf(getITelephony() != null);
            sRegistry.listenForSubscriber(listener.mSubId, pkgForDebug, listener.callback, events, notifyNow.booleanValue());
        } catch (RemoteException e) {
        } catch (NullPointerException e2) {
        }
    }

    public int getCdmaEriIconIndex() {
        return getCdmaEriIconIndex(getDefaultSubscription());
    }

    public int getCdmaEriIconIndex(int subId) {
        try {
            return getITelephony().getCdmaEriIconIndexForSubscriber(subId);
        } catch (RemoteException e) {
            return -1;
        } catch (NullPointerException e2) {
            return -1;
        }
    }

    public int getCdmaEriIconMode() {
        return getCdmaEriIconMode(getDefaultSubscription());
    }

    public int getCdmaEriIconMode(int subId) {
        try {
            return getITelephony().getCdmaEriIconModeForSubscriber(subId);
        } catch (RemoteException e) {
            return -1;
        } catch (NullPointerException e2) {
            return -1;
        }
    }

    public String getCdmaEriText() {
        return getCdmaEriText(getDefaultSubscription());
    }

    public String getCdmaEriText(int subId) {
        try {
            return getITelephony().getCdmaEriTextForSubscriber(subId);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public boolean isVoiceCapable() {
        if (this.mContext == null) {
            return true;
        }
        return this.mContext.getResources().getBoolean(R.bool.config_voice_capable);
    }

    public boolean isSmsCapable() {
        if (this.mContext == null) {
            return true;
        }
        return this.mContext.getResources().getBoolean(R.bool.config_sms_capable);
    }

    public List<CellInfo> getAllCellInfo() {
        try {
            return getITelephony().getAllCellInfo();
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public void setCellInfoListRate(int rateInMillis) {
        try {
            getITelephony().setCellInfoListRate(rateInMillis);
        } catch (RemoteException e) {
        } catch (NullPointerException e2) {
        }
    }

    public String getMmsUserAgent() {
        if (this.mContext == null) {
            return null;
        }
        return this.mContext.getResources().getString(R.string.config_mms_user_agent);
    }

    public String getMmsUAProfUrl() {
        if (this.mContext == null) {
            return null;
        }
        return this.mContext.getResources().getString(R.string.config_mms_user_agent_profile_url);
    }

    public IccOpenLogicalChannelResponse iccOpenLogicalChannel(String AID) {
        try {
            return getITelephony().iccOpenLogicalChannel(AID);
        } catch (RemoteException | NullPointerException e) {
            return null;
        }
    }

    public boolean iccCloseLogicalChannel(int channel) {
        try {
            return getITelephony().iccCloseLogicalChannel(channel);
        } catch (RemoteException | NullPointerException e) {
            return false;
        }
    }

    public String iccTransmitApduLogicalChannel(int channel, int cla, int instruction, int p1, int p2, int p3, String data) {
        try {
            return getITelephony().iccTransmitApduLogicalChannel(channel, cla, instruction, p1, p2, p3, data);
        } catch (RemoteException | NullPointerException e) {
            return ProxyInfo.LOCAL_EXCL_LIST;
        }
    }

    public String iccTransmitApduBasicChannel(int cla, int instruction, int p1, int p2, int p3, String data) {
        try {
            return getITelephony().iccTransmitApduBasicChannel(cla, instruction, p1, p2, p3, data);
        } catch (RemoteException | NullPointerException e) {
            return ProxyInfo.LOCAL_EXCL_LIST;
        }
    }

    public byte[] iccExchangeSimIO(int fileID, int command, int p1, int p2, int p3, String filePath) {
        try {
            return getITelephony().iccExchangeSimIO(fileID, command, p1, p2, p3, filePath);
        } catch (RemoteException | NullPointerException e) {
            return null;
        }
    }

    public String sendEnvelopeWithStatus(String content) {
        try {
            return getITelephony().sendEnvelopeWithStatus(content);
        } catch (RemoteException | NullPointerException e) {
            return ProxyInfo.LOCAL_EXCL_LIST;
        }
    }

    public String nvReadItem(int itemID) {
        try {
            return getITelephony().nvReadItem(itemID);
        } catch (RemoteException ex) {
            Rlog.e(TAG, "nvReadItem RemoteException", ex);
            return ProxyInfo.LOCAL_EXCL_LIST;
        } catch (NullPointerException ex2) {
            Rlog.e(TAG, "nvReadItem NPE", ex2);
            return ProxyInfo.LOCAL_EXCL_LIST;
        }
    }

    public boolean nvWriteItem(int itemID, String itemValue) {
        try {
            return getITelephony().nvWriteItem(itemID, itemValue);
        } catch (RemoteException ex) {
            Rlog.e(TAG, "nvWriteItem RemoteException", ex);
            return false;
        } catch (NullPointerException ex2) {
            Rlog.e(TAG, "nvWriteItem NPE", ex2);
            return false;
        }
    }

    public boolean nvWriteCdmaPrl(byte[] preferredRoamingList) {
        try {
            return getITelephony().nvWriteCdmaPrl(preferredRoamingList);
        } catch (RemoteException ex) {
            Rlog.e(TAG, "nvWriteCdmaPrl RemoteException", ex);
            return false;
        } catch (NullPointerException ex2) {
            Rlog.e(TAG, "nvWriteCdmaPrl NPE", ex2);
            return false;
        }
    }

    public boolean nvResetConfig(int resetType) {
        try {
            return getITelephony().nvResetConfig(resetType);
        } catch (RemoteException ex) {
            Rlog.e(TAG, "nvResetConfig RemoteException", ex);
            return false;
        } catch (NullPointerException ex2) {
            Rlog.e(TAG, "nvResetConfig NPE", ex2);
            return false;
        }
    }

    private static int getDefaultSubscription() {
        return SubscriptionManager.getDefaultSubId();
    }

    private static int getDefaultPhone() {
        return SubscriptionManager.getPhoneId(SubscriptionManager.getDefaultSubId());
    }

    public int getDefaultSim() {
        return SubscriptionManager.getSlotId(SubscriptionManager.getDefaultSubId());
    }

    public static void setTelephonyProperty(int phoneId, String property, String value) {
        String propVal = ProxyInfo.LOCAL_EXCL_LIST;
        String[] p = null;
        String prop = SystemProperties.get(property);
        if (value == null) {
            value = ProxyInfo.LOCAL_EXCL_LIST;
        }
        if (prop != null) {
            p = prop.split(",");
        }
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            Rlog.d(TAG, "setTelephonyProperty: invalid phoneId=" + phoneId + " property=" + property + " value: " + value + " prop=" + prop);
            return;
        }
        for (int i = 0; i < phoneId; i++) {
            String str = ProxyInfo.LOCAL_EXCL_LIST;
            if (p != null && i < p.length) {
                str = p[i];
            }
            propVal = propVal + str + ",";
        }
        String propVal2 = propVal + value;
        if (p != null) {
            for (int i2 = phoneId + 1; i2 < p.length; i2++) {
                propVal2 = propVal2 + "," + p[i2];
            }
        }
        if (property.length() > 31 || propVal2.length() > 91) {
            Rlog.d(TAG, "setTelephonyProperty: property to long phoneId=" + phoneId + " property=" + property + " value: " + value + " propVal=" + propVal2);
        } else {
            Rlog.d(TAG, "setTelephonyProperty: success phoneId=" + phoneId + " property=" + property + " value: " + value + " propVal=" + propVal2);
            SystemProperties.set(property, propVal2);
        }
    }

    public static int getIntAtIndex(ContentResolver cr, String name, int index) throws Settings.SettingNotFoundException {
        String v = Settings.Global.getString(cr, name);
        if (v != null) {
            String[] valArray = v.split(",");
            if (index >= 0 && index < valArray.length && valArray[index] != null) {
                try {
                    return Integer.parseInt(valArray[index]);
                } catch (NumberFormatException e) {
                }
            }
        }
        throw new Settings.SettingNotFoundException(name);
    }

    public static boolean putIntAtIndex(ContentResolver cr, String name, int index, int value) {
        String data = ProxyInfo.LOCAL_EXCL_LIST;
        String[] valArray = null;
        String v = Settings.Global.getString(cr, name);
        if (index == Integer.MAX_VALUE) {
            throw new RuntimeException("putIntAtIndex index == MAX_VALUE index=" + index);
        }
        if (index < 0) {
            throw new RuntimeException("putIntAtIndex index < 0 index=" + index);
        }
        if (v != null) {
            valArray = v.split(",");
        }
        for (int i = 0; i < index; i++) {
            String str = ProxyInfo.LOCAL_EXCL_LIST;
            if (valArray != null && i < valArray.length) {
                str = valArray[i];
            }
            data = data + str + ",";
        }
        String data2 = data + value;
        if (valArray != null) {
            for (int i2 = index + 1; i2 < valArray.length; i2++) {
                data2 = data2 + "," + valArray[i2];
            }
        }
        return Settings.Global.putString(cr, name, data2);
    }

    public static String getTelephonyProperty(int phoneId, String property, String defaultVal) {
        String propVal = null;
        String prop = SystemProperties.get(property);
        if (prop != null && prop.length() > 0) {
            String[] values = prop.split(",");
            if (phoneId >= 0 && phoneId < values.length && values[phoneId] != null) {
                propVal = values[phoneId];
            }
        }
        Rlog.d(TAG, "getTelephonyProperty: return propVal='" + propVal + "' phoneId=" + phoneId + " property='" + property + "' defaultVal='" + defaultVal + "' prop=" + prop);
        return propVal == null ? defaultVal : propVal;
    }

    public int getSimCount() {
        return isMultiSimEnabled() ? 2 : 1;
    }

    public String getIsimIst() {
        try {
            return getSubscriberInfo().getIsimIst();
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String[] getIsimPcscf() {
        try {
            return getSubscriberInfo().getIsimPcscf();
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getIsimChallengeResponse(String nonce) {
        try {
            return getSubscriberInfo().getIsimChallengeResponse(nonce);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getIccSimChallengeResponse(int subId, int appType, String data) {
        try {
            return getSubscriberInfo().getIccSimChallengeResponse(subId, appType, data);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getIccSimChallengeResponse(int appType, String data) {
        return getIccSimChallengeResponse(getDefaultSubscription(), appType, data);
    }

    public String[] getPcscfAddress(String apnType) {
        try {
            return getITelephony().getPcscfAddress(apnType);
        } catch (RemoteException e) {
            return new String[0];
        }
    }

    public void setImsRegistrationState(boolean registered) {
        try {
            getITelephony().setImsRegistrationState(registered);
        } catch (RemoteException e) {
        }
    }

    public int getPreferredNetworkType() {
        try {
            return getITelephony().getPreferredNetworkType();
        } catch (RemoteException ex) {
            Rlog.e(TAG, "getPreferredNetworkType RemoteException", ex);
            return -1;
        } catch (NullPointerException ex2) {
            Rlog.e(TAG, "getPreferredNetworkType NPE", ex2);
            return -1;
        }
    }

    public int getPreferredNetworkType(int subId) {
        try {
            return getITelephony().getPreferredNetworkTypeForSubscription(subId);
        } catch (RemoteException ex) {
            Rlog.e(TAG, "getPreferredNetworkType RemoteException", ex);
            return -1;
        } catch (NullPointerException ex2) {
            Rlog.e(TAG, "getPreferredNetworkType NPE", ex2);
            return -1;
        }
    }

    public boolean setPreferredNetworkType(int networkType) {
        try {
            return getITelephony().setPreferredNetworkType(networkType);
        } catch (RemoteException ex) {
            Rlog.e(TAG, "setPreferredNetworkType RemoteException", ex);
            return false;
        } catch (NullPointerException ex2) {
            Rlog.e(TAG, "setPreferredNetworkType NPE", ex2);
            return false;
        }
    }

    public boolean setPreferredNetworkType(int networkType, int subId) {
        try {
            return getITelephony().setPreferredNetworkTypeForSubscription(networkType, subId);
        } catch (RemoteException ex) {
            Rlog.e(TAG, "setPreferredNetworkType RemoteException", ex);
            return false;
        } catch (NullPointerException ex2) {
            Rlog.e(TAG, "setPreferredNetworkType NPE", ex2);
            return false;
        }
    }

    public boolean setPreferredNetworkTypeToGlobal() {
        return setPreferredNetworkType(10);
    }

    public int getTetherApnRequired() {
        try {
            return getITelephony().getTetherApnRequired();
        } catch (RemoteException ex) {
            Rlog.e(TAG, "hasMatchedTetherApnSetting RemoteException", ex);
            return 2;
        } catch (NullPointerException ex2) {
            Rlog.e(TAG, "hasMatchedTetherApnSetting NPE", ex2);
            return 2;
        }
    }

    public boolean hasCarrierPrivileges() {
        try {
            return getITelephony().getCarrierPrivilegeStatus() == 1;
        } catch (RemoteException ex) {
            Rlog.e(TAG, "hasCarrierPrivileges RemoteException", ex);
            return false;
        } catch (NullPointerException ex2) {
            Rlog.e(TAG, "hasCarrierPrivileges NPE", ex2);
            return false;
        }
    }

    public boolean setOperatorBrandOverride(String brand) {
        try {
            return getITelephony().setOperatorBrandOverride(brand);
        } catch (RemoteException ex) {
            Rlog.e(TAG, "setOperatorBrandOverride RemoteException", ex);
            return false;
        } catch (NullPointerException ex2) {
            Rlog.e(TAG, "setOperatorBrandOverride NPE", ex2);
            return false;
        }
    }

    public boolean setRoamingOverride(List<String> gsmRoamingList, List<String> gsmNonRoamingList, List<String> cdmaRoamingList, List<String> cdmaNonRoamingList) {
        try {
            return getITelephony().setRoamingOverride(gsmRoamingList, gsmNonRoamingList, cdmaRoamingList, cdmaNonRoamingList);
        } catch (RemoteException ex) {
            Rlog.e(TAG, "setRoamingOverride RemoteException", ex);
            return false;
        } catch (NullPointerException ex2) {
            Rlog.e(TAG, "setRoamingOverride NPE", ex2);
            return false;
        }
    }

    public String getCdmaMdn() {
        return getCdmaMdn(getDefaultSubscription());
    }

    public String getCdmaMdn(int subId) {
        try {
            return getITelephony().getCdmaMdn(subId);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getCdmaMin() {
        return getCdmaMin(getDefaultSubscription());
    }

    public String getCdmaMin(int subId) {
        try {
            return getITelephony().getCdmaMin(subId);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public int checkCarrierPrivilegesForPackage(String pkgname) {
        try {
            return getITelephony().checkCarrierPrivilegesForPackage(pkgname);
        } catch (RemoteException ex) {
            Rlog.e(TAG, "checkCarrierPrivilegesForPackage RemoteException", ex);
            return 0;
        } catch (NullPointerException ex2) {
            Rlog.e(TAG, "checkCarrierPrivilegesForPackage NPE", ex2);
            return 0;
        }
    }

    public List<String> getCarrierPackageNamesForIntent(Intent intent) {
        try {
            return getITelephony().getCarrierPackageNamesForIntent(intent);
        } catch (RemoteException ex) {
            Rlog.e(TAG, "getCarrierPackageNamesForIntent RemoteException", ex);
            return null;
        } catch (NullPointerException ex2) {
            Rlog.e(TAG, "getCarrierPackageNamesForIntent NPE", ex2);
            return null;
        }
    }

    public void dial(String number) {
        try {
            getITelephony().dial(number);
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#dial", e);
        }
    }

    public void call(String callingPackage, String number) {
        try {
            getITelephony().call(callingPackage, number);
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#call", e);
        }
    }

    public boolean endCall() {
        try {
            return getITelephony().endCall();
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#endCall", e);
            return false;
        }
    }

    public void answerRingingCall() {
        try {
            getITelephony().answerRingingCall();
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#answerRingingCall", e);
        }
    }

    public void silenceRinger() {
        try {
            getTelecomService().silenceRinger();
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelecomService#silenceRinger", e);
        }
    }

    public boolean isOffhook() {
        try {
            return getITelephony().isOffhook();
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#isOffhook", e);
            return false;
        }
    }

    public boolean isRinging() {
        try {
            return getITelephony().isRinging();
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#isRinging", e);
            return false;
        }
    }

    public boolean isIdle() {
        try {
            return getITelephony().isIdle();
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#isIdle", e);
            return true;
        }
    }

    public boolean isRadioOn() {
        try {
            return getITelephony().isRadioOn();
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#isRadioOn", e);
            return false;
        }
    }

    public boolean isSimPinEnabled() {
        try {
            return getITelephony().isSimPinEnabled();
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#isSimPinEnabled", e);
            return false;
        }
    }

    public boolean supplyPin(String pin) {
        try {
            return getITelephony().supplyPin(pin);
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#supplyPin", e);
            return false;
        }
    }

    public boolean supplyPuk(String puk, String pin) {
        try {
            return getITelephony().supplyPuk(puk, pin);
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#supplyPuk", e);
            return false;
        }
    }

    public int[] supplyPinReportResult(String pin) {
        try {
            return getITelephony().supplyPinReportResult(pin);
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#supplyPinReportResult", e);
            return new int[0];
        }
    }

    public int[] supplyPukReportResult(String puk, String pin) {
        try {
            return getITelephony().supplyPukReportResult(puk, pin);
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#]", e);
            return new int[0];
        }
    }

    public boolean handlePinMmi(String dialString) {
        try {
            return getITelephony().handlePinMmi(dialString);
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#handlePinMmi", e);
            return false;
        }
    }

    public boolean handlePinMmiForSubscriber(int subId, String dialString) {
        try {
            return getITelephony().handlePinMmiForSubscriber(subId, dialString);
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#handlePinMmi", e);
            return false;
        }
    }

    public void toggleRadioOnOff() {
        try {
            getITelephony().toggleRadioOnOff();
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#toggleRadioOnOff", e);
        }
    }

    public boolean setRadio(boolean turnOn) {
        try {
            return getITelephony().setRadio(turnOn);
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#setRadio", e);
            return false;
        }
    }

    public boolean setRadioPower(boolean turnOn) {
        try {
            return getITelephony().setRadioPower(turnOn);
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#setRadioPower", e);
            return false;
        }
    }

    public void updateServiceLocation() {
        try {
            getITelephony().updateServiceLocation();
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#updateServiceLocation", e);
        }
    }

    public boolean enableDataConnectivity() {
        try {
            return getITelephony().enableDataConnectivity();
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#enableDataConnectivity", e);
            return false;
        }
    }

    public boolean disableDataConnectivity() {
        try {
            return getITelephony().disableDataConnectivity();
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#disableDataConnectivity", e);
            return false;
        }
    }

    public boolean isDataConnectivityPossible() {
        try {
            return getITelephony().isDataConnectivityPossible();
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#isDataConnectivityPossible", e);
            return false;
        }
    }

    public boolean isDataConnectivityPossibleForSubscriber(int subId) {
        try {
            return getITelephony().isDataConnectivityPossibleForSubscriber(subId);
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#isDataConnectivityPossible", e);
            return false;
        }
    }

    public boolean needsOtaServiceProvisioning() {
        try {
            return getITelephony().needsOtaServiceProvisioning();
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#needsOtaServiceProvisioning", e);
            return false;
        }
    }

    public void setDataEnabled(boolean enable) {
        setDataEnabled(SubscriptionManager.getDefaultDataSubId(), enable);
    }

    public void setDataEnabled(int subId, boolean enable) {
        try {
            Log.d(TAG, "setDataEnabled: enabled=" + enable);
            getITelephony().setDataEnabled(subId, enable);
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#setDataEnabled", e);
        }
    }

    public boolean getDataEnabled() {
        return getDataEnabled(SubscriptionManager.getDefaultDataSubId());
    }

    public boolean getDataEnabled(int subId) {
        boolean retVal = false;
        try {
            retVal = getITelephony().getDataEnabled(subId);
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#getDataEnabled", e);
        } catch (NullPointerException e2) {
        }
        Log.d(TAG, "getDataEnabled: retVal=" + retVal);
        return retVal;
    }

    public int invokeOemRilRequestRaw(byte[] oemReq, byte[] oemResp) {
        try {
            return getITelephony().invokeOemRilRequestRaw(oemReq, oemResp);
        } catch (RemoteException | NullPointerException e) {
            return -1;
        }
    }

    public void enableVideoCalling(boolean enable) {
        try {
            getITelephony().enableVideoCalling(enable);
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#enableVideoCalling", e);
        }
    }

    public boolean isVideoCallingEnabled() {
        try {
            return getITelephony().isVideoCallingEnabled();
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#isVideoCallingEnabled", e);
            return false;
        }
    }

    public static int getIntWithSubId(ContentResolver cr, String name, int subId, int def) {
        return Settings.Global.getInt(cr, name + subId, Settings.Global.getInt(cr, name, def));
    }

    public static int getIntWithSubId(ContentResolver cr, String name, int subId) throws Settings.SettingNotFoundException {
        try {
            return Settings.Global.getInt(cr, name + subId);
        } catch (Settings.SettingNotFoundException e) {
            try {
                int val = Settings.Global.getInt(cr, name);
                Settings.Global.putInt(cr, name + subId, val);
                int default_val = val;
                if (name.equals(Settings.Global.MOBILE_DATA)) {
                    default_val = "true".equalsIgnoreCase(SystemProperties.get("ro.com.android.mobiledata", "true")) ? 1 : 0;
                } else if (name.equals("data_roaming")) {
                    default_val = "true".equalsIgnoreCase(SystemProperties.get("ro.com.android.dataroaming", "false")) ? 1 : 0;
                }
                if (default_val != val) {
                    Settings.Global.putInt(cr, name, default_val);
                    return val;
                }
                return val;
            } catch (Settings.SettingNotFoundException e2) {
                throw new Settings.SettingNotFoundException(name);
            }
        }
    }

    public boolean isImsRegistered() {
        try {
            return getITelephony().isImsRegistered();
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public void setSimOperatorNumeric(String numeric) {
        int phoneId = getDefaultPhone();
        setSimOperatorNumericForPhone(phoneId, numeric);
    }

    public void setSimOperatorNumericForPhone(int phoneId, String numeric) {
        setTelephonyProperty(phoneId, TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC, numeric);
    }

    public void setSimOperatorName(String name) {
        int phoneId = getDefaultPhone();
        setSimOperatorNameForPhone(phoneId, name);
    }

    public void setSimOperatorNameForPhone(int phoneId, String name) {
        setTelephonyProperty(phoneId, TelephonyProperties.PROPERTY_ICC_OPERATOR_ALPHA, name);
    }

    public void setSimCountryIso(String iso) {
        int phoneId = getDefaultPhone();
        setSimCountryIsoForPhone(phoneId, iso);
    }

    public void setSimCountryIsoForPhone(int phoneId, String iso) {
        setTelephonyProperty(phoneId, TelephonyProperties.PROPERTY_ICC_OPERATOR_ISO_COUNTRY, iso);
    }

    public void setSimState(String state) {
        int phoneId = getDefaultPhone();
        setSimStateForPhone(phoneId, state);
    }

    public void setSimStateForPhone(int phoneId, String state) {
        setTelephonyProperty(phoneId, TelephonyProperties.PROPERTY_SIM_STATE, state);
    }

    public void setBasebandVersion(String version) {
        int phoneId = getDefaultPhone();
        setBasebandVersionForPhone(phoneId, version);
    }

    public void setBasebandVersionForPhone(int phoneId, String version) {
        if (SubscriptionManager.isValidPhoneId(phoneId)) {
            String prop = TelephonyProperties.PROPERTY_BASEBAND_VERSION + (phoneId == 0 ? ProxyInfo.LOCAL_EXCL_LIST : Integer.toString(phoneId));
            SystemProperties.set(prop, version);
        }
    }

    public void setPhoneType(int type) {
        int phoneId = getDefaultPhone();
        setPhoneType(phoneId, type);
    }

    public void setPhoneType(int phoneId, int type) {
        if (SubscriptionManager.isValidPhoneId(phoneId)) {
            setTelephonyProperty(phoneId, TelephonyProperties.CURRENT_ACTIVE_PHONE, String.valueOf(type));
        }
    }

    public String getOtaSpNumberSchema(String defaultValue) {
        int phoneId = getDefaultPhone();
        return getOtaSpNumberSchemaForPhone(phoneId, defaultValue);
    }

    public String getOtaSpNumberSchemaForPhone(int phoneId, String defaultValue) {
        if (SubscriptionManager.isValidPhoneId(phoneId)) {
            return getTelephonyProperty(phoneId, TelephonyProperties.PROPERTY_OTASP_NUM_SCHEMA, defaultValue);
        }
        return defaultValue;
    }

    public boolean getSmsReceiveCapable(boolean defaultValue) {
        int phoneId = getDefaultPhone();
        return getSmsReceiveCapableForPhone(phoneId, defaultValue);
    }

    public boolean getSmsReceiveCapableForPhone(int phoneId, boolean defaultValue) {
        if (SubscriptionManager.isValidPhoneId(phoneId)) {
            return Boolean.valueOf(getTelephonyProperty(phoneId, TelephonyProperties.PROPERTY_SMS_RECEIVE, String.valueOf(defaultValue))).booleanValue();
        }
        return defaultValue;
    }

    public boolean getSmsSendCapable(boolean defaultValue) {
        int phoneId = getDefaultPhone();
        return getSmsSendCapableForPhone(phoneId, defaultValue);
    }

    public boolean getSmsSendCapableForPhone(int phoneId, boolean defaultValue) {
        if (SubscriptionManager.isValidPhoneId(phoneId)) {
            return Boolean.valueOf(getTelephonyProperty(phoneId, TelephonyProperties.PROPERTY_SMS_SEND, String.valueOf(defaultValue))).booleanValue();
        }
        return defaultValue;
    }

    public void setNetworkOperatorName(String name) {
        int phoneId = getDefaultPhone();
        setNetworkOperatorNameForPhone(phoneId, name);
    }

    public void setNetworkOperatorNameForPhone(int phoneId, String name) {
        if (SubscriptionManager.isValidPhoneId(phoneId)) {
            setTelephonyProperty(phoneId, TelephonyProperties.PROPERTY_OPERATOR_ALPHA, name);
        }
    }

    public void setNetworkOperatorNumeric(String numeric) {
        int phoneId = getDefaultPhone();
        setNetworkOperatorNumericForPhone(phoneId, numeric);
    }

    public void setNetworkOperatorNumericForPhone(int phoneId, String numeric) {
        setTelephonyProperty(phoneId, TelephonyProperties.PROPERTY_OPERATOR_NUMERIC, numeric);
    }

    public void setNetworkRoaming(boolean isRoaming) {
        int phoneId = getDefaultPhone();
        setNetworkRoamingForPhone(phoneId, isRoaming);
    }

    public void setNetworkRoamingForPhone(int phoneId, boolean isRoaming) {
        if (SubscriptionManager.isValidPhoneId(phoneId)) {
            setTelephonyProperty(phoneId, TelephonyProperties.PROPERTY_OPERATOR_ISROAMING, isRoaming ? "true" : "false");
        }
    }

    public void setNetworkCountryIso(String iso) {
        int phoneId = getDefaultPhone();
        setNetworkCountryIsoForPhone(phoneId, iso);
    }

    public void setNetworkCountryIsoForPhone(int phoneId, String iso) {
        if (SubscriptionManager.isValidPhoneId(phoneId)) {
            setTelephonyProperty(phoneId, TelephonyProperties.PROPERTY_OPERATOR_ISO_COUNTRY, iso);
        }
    }

    public void setDataNetworkType(int type) {
        int phoneId = getDefaultPhone();
        setDataNetworkTypeForPhone(phoneId, type);
    }

    public void setDataNetworkTypeForPhone(int phoneId, int type) {
        if (SubscriptionManager.isValidPhoneId(phoneId)) {
            setTelephonyProperty(phoneId, TelephonyProperties.PROPERTY_DATA_NETWORK_TYPE, ServiceState.rilRadioTechnologyToString(type));
        }
    }

    public int getUiccAppType(int subId) {
        try {
            return getITelephony().getUiccAppType(subId);
        } catch (RemoteException | NullPointerException e) {
            return 0;
        }
    }

    public int getUiccAppType() {
        return getUiccAppType(getDefaultSubscription());
    }

    public boolean isVideoEnabled() {
        try {
            return getITelephony().isVideoEnabled();
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#isVideoEnabled", e);
            return false;
        }
    }
}
