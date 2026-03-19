package android.telephony;

import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.app.ActivityThread;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkStats;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.service.carrier.CarrierIdentifier;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telephony.TelephonyScanManager;
import android.telephony.ims.aidl.IImsConfig;
import android.telephony.ims.aidl.IImsMmTelFeature;
import android.telephony.ims.aidl.IImsRcsFeature;
import android.telephony.ims.aidl.IImsRegistration;
import android.util.Log;
import com.android.ims.internal.IImsServiceFeatureCallback;
import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telecom.ITelecomService;
import com.android.internal.telephony.CellNetworkScanResult;
import com.android.internal.telephony.IPhoneSubInfo;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.ITelephonyRegistry;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.util.Preconditions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TelephonyManager {
    public static final String ACTION_CONFIGURE_VOICEMAIL = "android.telephony.action.CONFIGURE_VOICEMAIL";
    public static final String ACTION_DATA_STALL_DETECTED = "android.intent.action.DATA_STALL_DETECTED";
    public static final String ACTION_EMERGENCY_ASSISTANCE = "android.telephony.action.EMERGENCY_ASSISTANCE";
    public static final String ACTION_PHONE_STATE_CHANGED = "android.intent.action.PHONE_STATE";
    public static final String ACTION_PRECISE_CALL_STATE_CHANGED = "android.intent.action.PRECISE_CALL_STATE";

    @Deprecated
    public static final String ACTION_PRECISE_DATA_CONNECTION_STATE_CHANGED = "android.intent.action.PRECISE_DATA_CONNECTION_STATE_CHANGED";
    public static final String ACTION_RESPOND_VIA_MESSAGE = "android.intent.action.RESPOND_VIA_MESSAGE";
    public static final String ACTION_SHOW_VOICEMAIL_NOTIFICATION = "android.telephony.action.SHOW_VOICEMAIL_NOTIFICATION";

    @SystemApi
    public static final String ACTION_SIM_APPLICATION_STATE_CHANGED = "android.telephony.action.SIM_APPLICATION_STATE_CHANGED";

    @SystemApi
    public static final String ACTION_SIM_CARD_STATE_CHANGED = "android.telephony.action.SIM_CARD_STATE_CHANGED";

    @SystemApi
    public static final String ACTION_SIM_SLOT_STATUS_CHANGED = "android.telephony.action.SIM_SLOT_STATUS_CHANGED";
    public static final String ACTION_SUBSCRIPTION_CARRIER_IDENTITY_CHANGED = "android.telephony.action.SUBSCRIPTION_CARRIER_IDENTITY_CHANGED";
    public static final int APPTYPE_CSIM = 4;
    public static final int APPTYPE_ISIM = 5;
    public static final int APPTYPE_RUIM = 3;
    public static final int APPTYPE_SIM = 1;
    public static final int APPTYPE_USIM = 2;
    public static final int AUTHTYPE_EAP_AKA = 129;
    public static final int AUTHTYPE_EAP_SIM = 128;
    public static final int CALL_STATE_IDLE = 0;
    public static final int CALL_STATE_OFFHOOK = 2;
    public static final int CALL_STATE_RINGING = 1;
    public static final int CARD_POWER_DOWN = 0;
    public static final int CARD_POWER_UP = 1;
    public static final int CARD_POWER_UP_PASS_THROUGH = 2;

    @SystemApi
    public static final int CARRIER_PRIVILEGE_STATUS_ERROR_LOADING_RULES = -2;

    @SystemApi
    public static final int CARRIER_PRIVILEGE_STATUS_HAS_ACCESS = 1;

    @SystemApi
    public static final int CARRIER_PRIVILEGE_STATUS_NO_ACCESS = 0;

    @SystemApi
    public static final int CARRIER_PRIVILEGE_STATUS_RULES_NOT_LOADED = -1;
    public static final int CDMA_ROAMING_MODE_AFFILIATED = 1;
    public static final int CDMA_ROAMING_MODE_ANY = 2;
    public static final int CDMA_ROAMING_MODE_HOME = 0;
    public static final int CDMA_ROAMING_MODE_RADIO_DEFAULT = -1;
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
    public static final boolean EMERGENCY_ASSISTANCE_ENABLED = true;
    public static final String EVENT_CALL_FORWARDED = "android.telephony.event.EVENT_CALL_FORWARDED";
    public static final String EVENT_DOWNGRADE_DATA_DISABLED = "android.telephony.event.EVENT_DOWNGRADE_DATA_DISABLED";
    public static final String EVENT_DOWNGRADE_DATA_LIMIT_REACHED = "android.telephony.event.EVENT_DOWNGRADE_DATA_LIMIT_REACHED";
    public static final String EVENT_HANDOVER_TO_WIFI_FAILED = "android.telephony.event.EVENT_HANDOVER_TO_WIFI_FAILED";
    public static final String EVENT_HANDOVER_VIDEO_FROM_LTE_TO_WIFI = "android.telephony.event.EVENT_HANDOVER_VIDEO_FROM_LTE_TO_WIFI";
    public static final String EVENT_HANDOVER_VIDEO_FROM_WIFI_TO_LTE = "android.telephony.event.EVENT_HANDOVER_VIDEO_FROM_WIFI_TO_LTE";
    public static final String EVENT_NOTIFY_INTERNATIONAL_CALL_ON_WFC = "android.telephony.event.EVENT_NOTIFY_INTERNATIONAL_CALL_ON_WFC";
    public static final String EVENT_SUPPLEMENTARY_SERVICE_NOTIFICATION = "android.telephony.event.EVENT_SUPPLEMENTARY_SERVICE_NOTIFICATION";
    public static final String EXTRA_BACKGROUND_CALL_STATE = "background_state";
    public static final String EXTRA_CALL_VOICEMAIL_INTENT = "android.telephony.extra.CALL_VOICEMAIL_INTENT";
    public static final String EXTRA_CARRIER_ID = "android.telephony.extra.CARRIER_ID";
    public static final String EXTRA_CARRIER_NAME = "android.telephony.extra.CARRIER_NAME";
    public static final String EXTRA_DATA_APN = "apn";
    public static final String EXTRA_DATA_APN_TYPE = "apnType";
    public static final String EXTRA_DATA_CHANGE_REASON = "reason";
    public static final String EXTRA_DATA_FAILURE_CAUSE = "failCause";
    public static final String EXTRA_DATA_LINK_PROPERTIES_KEY = "linkProperties";
    public static final String EXTRA_DATA_NETWORK_TYPE = "networkType";
    public static final String EXTRA_DATA_STATE = "state";
    public static final String EXTRA_DISCONNECT_CAUSE = "disconnect_cause";
    public static final String EXTRA_FOREGROUND_CALL_STATE = "foreground_state";
    public static final String EXTRA_HIDE_PUBLIC_SETTINGS = "android.telephony.extra.HIDE_PUBLIC_SETTINGS";
    public static final String EXTRA_INCOMING_NUMBER = "incoming_number";
    public static final String EXTRA_IS_REFRESH = "android.telephony.extra.IS_REFRESH";
    public static final String EXTRA_LAUNCH_VOICEMAIL_SETTINGS_INTENT = "android.telephony.extra.LAUNCH_VOICEMAIL_SETTINGS_INTENT";
    public static final String EXTRA_NOTIFICATION_CODE = "android.telephony.extra.NOTIFICATION_CODE";
    public static final String EXTRA_NOTIFICATION_COUNT = "android.telephony.extra.NOTIFICATION_COUNT";
    public static final String EXTRA_NOTIFICATION_MESSAGE = "android.telephony.extra.NOTIFICATION_MESSAGE";
    public static final String EXTRA_NOTIFICATION_TYPE = "android.telephony.extra.NOTIFICATION_TYPE";
    public static final String EXTRA_PHONE_ACCOUNT_HANDLE = "android.telephony.extra.PHONE_ACCOUNT_HANDLE";
    public static final String EXTRA_PRECISE_DISCONNECT_CAUSE = "precise_disconnect_cause";
    public static final String EXTRA_RECOVERY_ACTION = "recoveryAction";
    public static final String EXTRA_RINGING_CALL_STATE = "ringing_state";

    @SystemApi
    public static final String EXTRA_SIM_STATE = "android.telephony.extra.SIM_STATE";
    public static final String EXTRA_STATE = "state";
    public static final String EXTRA_SUBSCRIPTION_ID = "android.telephony.extra.SUBSCRIPTION_ID";

    @SystemApi
    public static final String EXTRA_VISUAL_VOICEMAIL_ENABLED_BY_USER_BOOL = "android.telephony.extra.VISUAL_VOICEMAIL_ENABLED_BY_USER_BOOL";
    public static final String EXTRA_VOICEMAIL_NUMBER = "android.telephony.extra.VOICEMAIL_NUMBER";

    @SystemApi
    public static final String EXTRA_VOICEMAIL_SCRAMBLED_PIN_STRING = "android.telephony.extra.VOICEMAIL_SCRAMBLED_PIN_STRING";
    public static final int INDICATION_FILTER_DATA_CALL_DORMANCY_CHANGED = 4;
    public static final int INDICATION_FILTER_FULL_NETWORK_STATE = 2;
    public static final int INDICATION_FILTER_LINK_CAPACITY_ESTIMATE = 8;
    public static final int INDICATION_FILTER_PHYSICAL_CHANNEL_CONFIG = 16;
    public static final int INDICATION_FILTER_SIGNAL_STRENGTH = 1;
    public static final int INDICATION_UPDATE_MODE_IGNORE_SCREEN_OFF = 2;
    public static final int INDICATION_UPDATE_MODE_NORMAL = 1;
    public static final int KEY_TYPE_EPDG = 1;
    public static final int KEY_TYPE_WLAN = 2;
    public static final int MAX_NETWORK_TYPE = 19;
    public static final String METADATA_HIDE_VOICEMAIL_SETTINGS_MENU = "android.telephony.HIDE_VOICEMAIL_SETTINGS_MENU";
    public static final String MODEM_ACTIVITY_RESULT_KEY = "controller_activity";
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
    public static final int NETWORK_TYPE_IWLAN = 18;
    public static final int NETWORK_TYPE_LTE = 13;
    public static final int NETWORK_TYPE_LTE_CA = 19;
    public static final int NETWORK_TYPE_TD_SCDMA = 17;
    public static final int NETWORK_TYPE_UMTS = 3;
    public static final int NETWORK_TYPE_UNKNOWN = 0;
    public static final int OTASP_NEEDED = 2;
    public static final int OTASP_NOT_NEEDED = 3;
    public static final int OTASP_SIM_UNPROVISIONED = 5;
    public static final int OTASP_UNINITIALIZED = 0;
    public static final int OTASP_UNKNOWN = 1;
    public static final String PHONE_PROCESS_NAME = "com.android.phone";
    public static final int PHONE_TYPE_CDMA = 2;
    public static final int PHONE_TYPE_GSM = 1;
    public static final int PHONE_TYPE_NONE = 0;
    public static final int PHONE_TYPE_SIP = 3;

    @SystemApi
    public static final int SIM_ACTIVATION_STATE_ACTIVATED = 2;

    @SystemApi
    public static final int SIM_ACTIVATION_STATE_ACTIVATING = 1;

    @SystemApi
    public static final int SIM_ACTIVATION_STATE_DEACTIVATED = 3;

    @SystemApi
    public static final int SIM_ACTIVATION_STATE_RESTRICTED = 4;

    @SystemApi
    public static final int SIM_ACTIVATION_STATE_UNKNOWN = 0;
    public static final int SIM_STATE_ABSENT = 1;
    public static final int SIM_STATE_CARD_IO_ERROR = 8;
    public static final int SIM_STATE_CARD_RESTRICTED = 9;

    @SystemApi
    public static final int SIM_STATE_LOADED = 10;
    public static final int SIM_STATE_NETWORK_LOCKED = 4;
    public static final int SIM_STATE_NOT_READY = 6;
    public static final int SIM_STATE_PERM_DISABLED = 7;
    public static final int SIM_STATE_PIN_REQUIRED = 2;

    @SystemApi
    public static final int SIM_STATE_PRESENT = 11;
    public static final int SIM_STATE_PUK_REQUIRED = 3;
    public static final int SIM_STATE_READY = 5;
    public static final int SIM_STATE_UNKNOWN = 0;
    private static final String TAG = "TelephonyManager";
    public static final int UNKNOWN_CARRIER_ID = -1;
    public static final int UNKNOWN_CARRIER_ID_LIST_VERSION = -1;
    public static final int USSD_ERROR_SERVICE_UNAVAIL = -2;
    public static final String USSD_RESPONSE = "USSD_RESPONSE";
    public static final int USSD_RETURN_FAILURE = -1;
    public static final int USSD_RETURN_SUCCESS = 100;
    public static final String VVM_TYPE_CVVM = "vvm_type_cvvm";
    public static final String VVM_TYPE_OMTP = "vvm_type_omtp";
    private final Context mContext;
    private final int mSubId;
    private SubscriptionManager mSubscriptionManager;
    private TelephonyScanManager mTelephonyScanManager;
    private static String multiSimConfig = SystemProperties.get(TelephonyProperties.PROPERTY_MULTI_SIM_CONFIG);
    private static TelephonyManager sInstance = new TelephonyManager();
    public static final String EXTRA_STATE_IDLE = PhoneConstants.State.IDLE.toString();
    public static final String EXTRA_STATE_RINGING = PhoneConstants.State.RINGING.toString();
    public static final String EXTRA_STATE_OFFHOOK = PhoneConstants.State.OFFHOOK.toString();
    private static final String sKernelCmdLine = getProcCmdLine();
    private static final Pattern sProductTypePattern = Pattern.compile("\\sproduct_type\\s*=\\s*(\\w+)");
    private static final String sLteOnCdmaProductType = SystemProperties.get(TelephonyProperties.PROPERTY_LTE_ON_CDMA_PRODUCT_TYPE, "");

    @Retention(RetentionPolicy.SOURCE)
    public @interface IndicationFilters {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface IndicationUpdateMode {
    }

    public enum MultiSimVariants {
        DSDS,
        DSDA,
        TSTS,
        UNKNOWN
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface SimActivationState {
    }

    public interface WifiCallingChoices {
        public static final int ALWAYS_USE = 0;
        public static final int ASK_EVERY_TIME = 1;
        public static final int NEVER_USE = 2;
    }

    public TelephonyManager(Context context) {
        this(context, Integer.MAX_VALUE);
    }

    public TelephonyManager(Context context, int i) {
        this.mSubId = i;
        Context applicationContext = context.getApplicationContext();
        if (applicationContext != null) {
            this.mContext = applicationContext;
        } else {
            this.mContext = context;
        }
        this.mSubscriptionManager = SubscriptionManager.from(this.mContext);
    }

    private TelephonyManager() {
        this.mContext = null;
        this.mSubId = -1;
    }

    public static TelephonyManager getDefault() {
        return sInstance;
    }

    private String getOpPackageName() {
        if (this.mContext != null) {
            return this.mContext.getOpPackageName();
        }
        return ActivityThread.currentOpPackageName();
    }

    private boolean isSystemProcess() {
        return Process.myUid() == 1000;
    }

    public MultiSimVariants getMultiSimConfiguration() {
        String str = SystemProperties.get(TelephonyProperties.PROPERTY_MULTI_SIM_CONFIG);
        if (str.equals("dsds")) {
            return MultiSimVariants.DSDS;
        }
        if (str.equals("dsda")) {
            return MultiSimVariants.DSDA;
        }
        if (str.equals("tsts")) {
            return MultiSimVariants.TSTS;
        }
        return MultiSimVariants.UNKNOWN;
    }

    public int getPhoneCount() {
        ConnectivityManager connectivityManager;
        switch (getMultiSimConfiguration()) {
            case UNKNOWN:
                if (!isVoiceCapable() && !isSmsCapable() && this.mContext != null && (connectivityManager = (ConnectivityManager) this.mContext.getSystemService(Context.CONNECTIVITY_SERVICE)) != null && !connectivityManager.isNetworkSupported(0)) {
                    break;
                }
                break;
        }
        return 1;
    }

    public static TelephonyManager from(Context context) {
        return (TelephonyManager) context.getSystemService("phone");
    }

    public TelephonyManager createForSubscriptionId(int i) {
        return new TelephonyManager(this.mContext, i);
    }

    public TelephonyManager createForPhoneAccountHandle(PhoneAccountHandle phoneAccountHandle) {
        int subIdForPhoneAccountHandle = getSubIdForPhoneAccountHandle(phoneAccountHandle);
        if (!SubscriptionManager.isValidSubscriptionId(subIdForPhoneAccountHandle)) {
            return null;
        }
        return new TelephonyManager(this.mContext, subIdForPhoneAccountHandle);
    }

    public boolean isMultiSimEnabled() {
        return multiSimConfig.equals("dsds") || multiSimConfig.equals("dsda") || multiSimConfig.equals("tsts");
    }

    public String getDeviceSoftwareVersion() {
        return getDeviceSoftwareVersion(getSlotIndex());
    }

    public String getDeviceSoftwareVersion(int i) {
        ITelephony iTelephony = getITelephony();
        if (iTelephony == null) {
            return null;
        }
        try {
            return iTelephony.getDeviceSoftwareVersionForSlot(i, getOpPackageName());
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    @Deprecated
    public String getDeviceId() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony == null) {
                return null;
            }
            return iTelephony.getDeviceId(this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    @Deprecated
    public String getDeviceId(int i) {
        try {
            IPhoneSubInfo subscriberInfo = getSubscriberInfo();
            if (subscriberInfo == null) {
                return null;
            }
            return subscriberInfo.getDeviceIdForPhone(i, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getImei() {
        return getImei(getSlotIndex());
    }

    public String getImei(int i) {
        ITelephony iTelephony = getITelephony();
        if (iTelephony == null) {
            return null;
        }
        try {
            return iTelephony.getImeiForSlot(i, getOpPackageName());
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getMeid() {
        return getMeid(getSlotIndex());
    }

    public String getMeid(int i) {
        ITelephony iTelephony = getITelephony();
        if (iTelephony == null) {
            return null;
        }
        try {
            return iTelephony.getMeidForSlot(i, getOpPackageName());
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getNai() {
        return getNaiBySubscriberId(getSubId());
    }

    public String getNai(int i) {
        int[] subId = SubscriptionManager.getSubId(i);
        if (subId == null) {
            return null;
        }
        return getNaiBySubscriberId(subId[0]);
    }

    private String getNaiBySubscriberId(int i) {
        try {
            IPhoneSubInfo subscriberInfo = getSubscriberInfo();
            if (subscriberInfo == null) {
                return null;
            }
            String naiForSubscriber = subscriberInfo.getNaiForSubscriber(i, this.mContext.getOpPackageName());
            if (Log.isLoggable(TAG, 2)) {
                Rlog.v(TAG, "Nai = " + naiForSubscriber);
            }
            return naiForSubscriber;
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    @Deprecated
    public CellLocation getCellLocation() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony == null) {
                Rlog.d(TAG, "getCellLocation returning null because telephony is null");
                return null;
            }
            Bundle cellLocation = iTelephony.getCellLocation(this.mContext.getOpPackageName());
            if (cellLocation.isEmpty()) {
                Rlog.d(TAG, "getCellLocation returning null because bundle is empty");
                return null;
            }
            CellLocation cellLocationNewFromBundle = CellLocation.newFromBundle(cellLocation);
            if (cellLocationNewFromBundle.isEmpty()) {
                Rlog.d(TAG, "getCellLocation returning null because CellLocation is empty");
                return null;
            }
            return cellLocationNewFromBundle;
        } catch (RemoteException e) {
            Rlog.d(TAG, "getCellLocation returning null due to RemoteException " + e);
            return null;
        } catch (NullPointerException e2) {
            Rlog.d(TAG, "getCellLocation returning null due to NullPointerException " + e2);
            return null;
        }
    }

    public void enableLocationUpdates() {
        enableLocationUpdates(getSubId());
    }

    public void enableLocationUpdates(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                iTelephony.enableLocationUpdatesForSubscriber(i);
            }
        } catch (RemoteException e) {
        } catch (NullPointerException e2) {
        }
    }

    public void disableLocationUpdates() {
        disableLocationUpdates(getSubId());
    }

    public void disableLocationUpdates(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                iTelephony.disableLocationUpdatesForSubscriber(i);
            }
        } catch (RemoteException e) {
        } catch (NullPointerException e2) {
        }
    }

    @Deprecated
    public List<NeighboringCellInfo> getNeighboringCellInfo() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony == null) {
                return null;
            }
            return iTelephony.getNeighboringCellInfo(this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    @SystemApi
    public int getCurrentPhoneType() {
        return getCurrentPhoneType(getSubId());
    }

    @SystemApi
    public int getCurrentPhoneType(int i) {
        int phoneId;
        if (i == -1) {
            phoneId = 0;
        } else {
            phoneId = SubscriptionManager.getPhoneId(i);
        }
        return getCurrentPhoneTypeForSlot(phoneId);
    }

    public int getCurrentPhoneTypeForSlot(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.getActivePhoneTypeForSlot(i);
            }
            return getPhoneTypeFromProperty(i);
        } catch (RemoteException e) {
            return getPhoneTypeFromProperty(i);
        } catch (NullPointerException e2) {
            return getPhoneTypeFromProperty(i);
        }
    }

    public int getPhoneType() {
        if (!isVoiceCapable()) {
            return 0;
        }
        return getCurrentPhoneType();
    }

    private int getPhoneTypeFromProperty() {
        return getPhoneTypeFromProperty(getPhoneId());
    }

    private int getPhoneTypeFromProperty(int i) {
        String telephonyProperty = getTelephonyProperty(i, TelephonyProperties.CURRENT_ACTIVE_PHONE, null);
        if (telephonyProperty == null || telephonyProperty.isEmpty()) {
            return getPhoneTypeFromNetworkType(i);
        }
        return Integer.parseInt(telephonyProperty);
    }

    private int getPhoneTypeFromNetworkType() {
        return getPhoneTypeFromNetworkType(getPhoneId());
    }

    private int getPhoneTypeFromNetworkType(int i) {
        String telephonyProperty = getTelephonyProperty(i, "ro.telephony.default_network", null);
        if (telephonyProperty != null && !telephonyProperty.isEmpty()) {
            return getPhoneType(Integer.parseInt(telephonyProperty));
        }
        return 0;
    }

    public static int getPhoneType(int i) {
        switch (i) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 9:
            case 10:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 22:
                return 1;
            case 4:
            case 5:
            case 6:
                return 2;
            case 7:
            case 8:
            case 21:
                return 2;
            case 11:
                return getLteOnCdmaModeStatic() == 1 ? 2 : 1;
            default:
                return 1;
        }
    }

    private static java.lang.String getProcCmdLine() {
        r0 = "";
        r2 = new java.io.FileInputStream("/proc/cmdline");
        r1 = new byte[2048];
        r3 = r2.read(r1);
        if (r3 > 0) {
            r0 = new java.lang.String(r1, 0, r3);
        }
        r2.close();
        while (true) {
            r2 = new java.lang.StringBuilder();
            r2.append("/proc/cmdline=");
            r2.append(r0);
            android.telephony.Rlog.d(android.telephony.TelephonyManager.TAG, r2.toString());
            return r0;
        }
    }

    public static int getLteOnCdmaModeStatic() {
        String strGroup = "";
        int i = SystemProperties.getInt(TelephonyProperties.PROPERTY_LTE_ON_CDMA_DEVICE, -1);
        int i2 = 0;
        if (i == -1) {
            Matcher matcher = sProductTypePattern.matcher(sKernelCmdLine);
            if (matcher.find()) {
                strGroup = matcher.group(1);
                if (sLteOnCdmaProductType.equals(strGroup)) {
                    i2 = 1;
                }
            }
        } else {
            i2 = i;
        }
        Rlog.d(TAG, "getLteOnCdmaMode=" + i2 + " curVal=" + i + " product_type='" + strGroup + "' lteOnCdmaProductType='" + sLteOnCdmaProductType + "'");
        return i2;
    }

    public String getNetworkOperatorName() {
        return getNetworkOperatorName(getSubId());
    }

    public String getNetworkOperatorName(int i) {
        return getTelephonyProperty(SubscriptionManager.getPhoneId(i), TelephonyProperties.PROPERTY_OPERATOR_ALPHA, "");
    }

    public String getNetworkOperator() {
        return getNetworkOperatorForPhone(getPhoneId());
    }

    public String getNetworkOperator(int i) {
        return getNetworkOperatorForPhone(SubscriptionManager.getPhoneId(i));
    }

    public String getNetworkOperatorForPhone(int i) {
        return getTelephonyProperty(i, TelephonyProperties.PROPERTY_OPERATOR_NUMERIC, "");
    }

    public String getNetworkSpecifier() {
        return String.valueOf(getSubId());
    }

    public PersistableBundle getCarrierConfig() {
        return ((CarrierConfigManager) this.mContext.getSystemService(CarrierConfigManager.class)).getConfigForSubId(getSubId());
    }

    public boolean isNetworkRoaming() {
        return isNetworkRoaming(getSubId());
    }

    public boolean isNetworkRoaming(int i) {
        return Boolean.parseBoolean(getTelephonyProperty(SubscriptionManager.getPhoneId(i), TelephonyProperties.PROPERTY_OPERATOR_ISROAMING, null));
    }

    public String getNetworkCountryIso() {
        return getNetworkCountryIsoForPhone(getPhoneId());
    }

    public String getNetworkCountryIso(int i) {
        return getNetworkCountryIsoForPhone(getPhoneId(i));
    }

    public String getNetworkCountryIsoForPhone(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            return iTelephony == null ? "" : iTelephony.getNetworkCountryIsoForPhone(i);
        } catch (RemoteException e) {
            return "";
        }
    }

    public int getNetworkType() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony == null) {
                return 0;
            }
            return iTelephony.getNetworkType();
        } catch (RemoteException e) {
            return 0;
        } catch (NullPointerException e2) {
            return 0;
        }
    }

    public int getNetworkType(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony == null) {
                return 0;
            }
            return iTelephony.getNetworkTypeForSubscriber(i, getOpPackageName());
        } catch (RemoteException e) {
            return 0;
        } catch (NullPointerException e2) {
            return 0;
        }
    }

    public int getDataNetworkType() {
        return getDataNetworkType(getSubId(SubscriptionManager.getDefaultDataSubscriptionId()));
    }

    public int getDataNetworkType(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony == null) {
                return 0;
            }
            return iTelephony.getDataNetworkTypeForSubscriber(i, getOpPackageName());
        } catch (RemoteException e) {
            return 0;
        } catch (NullPointerException e2) {
            return 0;
        }
    }

    public int getVoiceNetworkType() {
        return getVoiceNetworkType(getSubId());
    }

    public int getVoiceNetworkType(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony == null) {
                return 0;
            }
            return iTelephony.getVoiceNetworkTypeForSubscriber(i, getOpPackageName());
        } catch (RemoteException e) {
            return 0;
        } catch (NullPointerException e2) {
            return 0;
        }
    }

    public static int getNetworkClass(int i) {
        switch (i) {
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
            case 17:
                return 2;
            case 13:
            case 18:
            case 19:
                return 3;
            default:
                return 0;
        }
    }

    public String getNetworkTypeName() {
        return getNetworkTypeName(getNetworkType());
    }

    public static String getNetworkTypeName(int i) {
        switch (i) {
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
            case 17:
                return "TD_SCDMA";
            case 18:
                return "IWLAN";
            case 19:
                return "LTE_CA";
            default:
                return IccCardConstants.INTENT_VALUE_ICC_UNKNOWN;
        }
    }

    public boolean hasIccCard() {
        return hasIccCard(getSlotIndex());
    }

    public boolean hasIccCard(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony == null) {
                return false;
            }
            return iTelephony.hasIccCardUsingSlotIndex(i);
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public int getSimState() {
        int simStateIncludingLoaded = getSimStateIncludingLoaded();
        if (simStateIncludingLoaded == 10) {
            return 5;
        }
        return simStateIncludingLoaded;
    }

    private int getSimStateIncludingLoaded() {
        int slotIndex = getSlotIndex();
        if (slotIndex < 0) {
            for (int i = 0; i < getPhoneCount(); i++) {
                int simState = getSimState(i);
                if (simState != 1) {
                    Rlog.d(TAG, "getSimState: default sim:" + slotIndex + ", sim state for slotIndex=" + i + " is " + simState + ", return state as unknown");
                    return 0;
                }
            }
            Rlog.d(TAG, "getSimState: default sim:" + slotIndex + ", all SIMs absent, return state as absent");
            return 1;
        }
        return SubscriptionManager.getSimStateForSlotIndex(slotIndex);
    }

    @SystemApi
    public int getSimCardState() {
        int simState = getSimState();
        switch (simState) {
            case 0:
            case 1:
            case 8:
            case 9:
                return simState;
            default:
                return 11;
        }
    }

    @SystemApi
    public int getSimApplicationState() {
        int simStateIncludingLoaded = getSimStateIncludingLoaded();
        switch (simStateIncludingLoaded) {
            case 0:
            case 1:
            case 8:
            case 9:
                return 0;
            case 5:
                return 6;
            default:
                return simStateIncludingLoaded;
        }
    }

    public int getSimState(int i) {
        int simStateForSlotIndex = SubscriptionManager.getSimStateForSlotIndex(i);
        if (simStateForSlotIndex == 10) {
            return 5;
        }
        return simStateForSlotIndex;
    }

    public String getSimOperator() {
        return getSimOperatorNumeric();
    }

    public String getSimOperator(int i) {
        return getSimOperatorNumeric(i);
    }

    public String getSimOperatorNumeric() {
        int defaultDataSubscriptionId = this.mSubId;
        if (!SubscriptionManager.isUsableSubIdValue(defaultDataSubscriptionId)) {
            defaultDataSubscriptionId = SubscriptionManager.getDefaultDataSubscriptionId();
            if (!SubscriptionManager.isUsableSubIdValue(defaultDataSubscriptionId)) {
                defaultDataSubscriptionId = SubscriptionManager.getDefaultSmsSubscriptionId();
                if (!SubscriptionManager.isUsableSubIdValue(defaultDataSubscriptionId)) {
                    defaultDataSubscriptionId = SubscriptionManager.getDefaultVoiceSubscriptionId();
                    if (!SubscriptionManager.isUsableSubIdValue(defaultDataSubscriptionId)) {
                        defaultDataSubscriptionId = SubscriptionManager.getDefaultSubscriptionId();
                    }
                }
            }
        }
        return getSimOperatorNumeric(defaultDataSubscriptionId);
    }

    public String getSimOperatorNumeric(int i) {
        return getSimOperatorNumericForPhone(SubscriptionManager.getPhoneId(i));
    }

    public String getSimOperatorNumericForPhone(int i) {
        return getTelephonyProperty(i, TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC, "");
    }

    public String getSimOperatorName() {
        return getSimOperatorNameForPhone(getPhoneId());
    }

    public String getSimOperatorName(int i) {
        return getSimOperatorNameForPhone(SubscriptionManager.getPhoneId(i));
    }

    public String getSimOperatorNameForPhone(int i) {
        return getTelephonyProperty(i, TelephonyProperties.PROPERTY_ICC_OPERATOR_ALPHA, "");
    }

    public String getSimCountryIso() {
        return getSimCountryIsoForPhone(getPhoneId());
    }

    public String getSimCountryIso(int i) {
        return getSimCountryIsoForPhone(SubscriptionManager.getPhoneId(i));
    }

    public String getSimCountryIsoForPhone(int i) {
        return getTelephonyProperty(i, TelephonyProperties.PROPERTY_ICC_OPERATOR_ISO_COUNTRY, "");
    }

    public String getSimSerialNumber() {
        return getSimSerialNumber(getSubId());
    }

    public String getSimSerialNumber(int i) {
        try {
            IPhoneSubInfo subscriberInfo = getSubscriberInfo();
            if (subscriberInfo == null) {
                return null;
            }
            return subscriberInfo.getIccSerialNumberForSubscriber(i, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public int getLteOnCdmaMode() {
        return getLteOnCdmaMode(getSubId());
    }

    public int getLteOnCdmaMode(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony == null) {
                return -1;
            }
            return iTelephony.getLteOnCdmaModeForSubscriber(i, getOpPackageName());
        } catch (RemoteException e) {
            return -1;
        } catch (NullPointerException e2) {
            return -1;
        }
    }

    @SystemApi
    public UiccSlotInfo[] getUiccSlotsInfo() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony == null) {
                return null;
            }
            return iTelephony.getUiccSlotsInfo();
        } catch (RemoteException e) {
            return null;
        }
    }

    public void refreshUiccProfile() {
        try {
            getITelephony().refreshUiccProfile(this.mSubId);
        } catch (RemoteException e) {
            Rlog.w(TAG, "RemoteException", e);
        }
    }

    @SystemApi
    public boolean switchSlots(int[] iArr) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony == null) {
                return false;
            }
            return iTelephony.switchSlots(iArr);
        } catch (RemoteException e) {
            return false;
        }
    }

    public String getSubscriberId() {
        return getSubscriberId(getSubId());
    }

    public String getSubscriberId(int i) {
        try {
            IPhoneSubInfo subscriberInfo = getSubscriberInfo();
            if (subscriberInfo == null) {
                return null;
            }
            return subscriberInfo.getSubscriberIdForSubscriber(i, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public ImsiEncryptionInfo getCarrierInfoForImsiEncryption(int i) {
        try {
            IPhoneSubInfo subscriberInfo = getSubscriberInfo();
            if (subscriberInfo == null) {
                Rlog.e(TAG, "IMSI error: Subscriber Info is null");
                return null;
            }
            int subId = getSubId(SubscriptionManager.getDefaultDataSubscriptionId());
            if (i != 1 && i != 2) {
                throw new IllegalArgumentException("IMSI error: Invalid key type");
            }
            ImsiEncryptionInfo carrierInfoForImsiEncryption = subscriberInfo.getCarrierInfoForImsiEncryption(subId, i, this.mContext.getOpPackageName());
            if (carrierInfoForImsiEncryption == null && isImsiEncryptionRequired(subId, i)) {
                Rlog.e(TAG, "IMSI error: key is required but not found");
                throw new IllegalArgumentException("IMSI error: key is required but not found");
            }
            return carrierInfoForImsiEncryption;
        } catch (RemoteException e) {
            Rlog.e(TAG, "getCarrierInfoForImsiEncryption RemoteException" + e);
            return null;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "getCarrierInfoForImsiEncryption NullPointerException" + e2);
            return null;
        }
    }

    public void resetCarrierKeysForImsiEncryption() {
        try {
            IPhoneSubInfo subscriberInfo = getSubscriberInfo();
            if (subscriberInfo == null) {
                Rlog.e(TAG, "IMSI error: Subscriber Info is null");
                if (!isSystemProcess()) {
                    throw new RuntimeException("IMSI error: Subscriber Info is null");
                }
                return;
            }
            subscriberInfo.resetCarrierKeysForImsiEncryption(getSubId(SubscriptionManager.getDefaultDataSubscriptionId()), this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            Rlog.e(TAG, "getCarrierInfoForImsiEncryption RemoteException" + e);
            if (!isSystemProcess()) {
                e.rethrowAsRuntimeException();
            }
        }
    }

    private static boolean isKeyEnabled(int i, int i2) {
        return ((i >> (i2 - 1)) & 1) == 1;
    }

    private boolean isImsiEncryptionRequired(int i, int i2) {
        PersistableBundle configForSubId;
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (carrierConfigManager == null || (configForSubId = carrierConfigManager.getConfigForSubId(i)) == null) {
            return false;
        }
        return isKeyEnabled(configForSubId.getInt(CarrierConfigManager.IMSI_KEY_AVAILABILITY_INT), i2);
    }

    public void setCarrierInfoForImsiEncryption(ImsiEncryptionInfo imsiEncryptionInfo) {
        try {
            IPhoneSubInfo subscriberInfo = getSubscriberInfo();
            if (subscriberInfo == null) {
                return;
            }
            subscriberInfo.setCarrierInfoForImsiEncryption(this.mSubId, this.mContext.getOpPackageName(), imsiEncryptionInfo);
        } catch (RemoteException e) {
            Rlog.e(TAG, "setCarrierInfoForImsiEncryption RemoteException", e);
        } catch (NullPointerException e2) {
        }
    }

    public String getGroupIdLevel1() {
        try {
            IPhoneSubInfo subscriberInfo = getSubscriberInfo();
            if (subscriberInfo == null) {
                return null;
            }
            return subscriberInfo.getGroupIdLevel1ForSubscriber(getSubId(), this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getGroupIdLevel1(int i) {
        try {
            IPhoneSubInfo subscriberInfo = getSubscriberInfo();
            if (subscriberInfo == null) {
                return null;
            }
            return subscriberInfo.getGroupIdLevel1ForSubscriber(i, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getLine1Number() {
        return getLine1Number(getSubId());
    }

    public String getLine1Number(int i) {
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
            IPhoneSubInfo subscriberInfo = getSubscriberInfo();
            if (subscriberInfo == null) {
                return null;
            }
            return subscriberInfo.getLine1NumberForSubscriber(i, this.mContext.getOpPackageName());
        } catch (RemoteException e2) {
            return null;
        } catch (NullPointerException e3) {
            return null;
        }
    }

    public boolean setLine1NumberForDisplay(String str, String str2) {
        return setLine1NumberForDisplay(getSubId(), str, str2);
    }

    public boolean setLine1NumberForDisplay(int i, String str, String str2) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.setLine1NumberForDisplayForSubscriber(i, str, str2);
            }
            return false;
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public String getLine1AlphaTag() {
        return getLine1AlphaTag(getSubId());
    }

    public String getLine1AlphaTag(int i) {
        String line1AlphaTagForDisplay;
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                line1AlphaTagForDisplay = iTelephony.getLine1AlphaTagForDisplay(i, getOpPackageName());
            } else {
                line1AlphaTagForDisplay = null;
            }
        } catch (RemoteException | NullPointerException e) {
            line1AlphaTagForDisplay = null;
        }
        if (line1AlphaTagForDisplay != null) {
            return line1AlphaTagForDisplay;
        }
        try {
            IPhoneSubInfo subscriberInfo = getSubscriberInfo();
            if (subscriberInfo == null) {
                return null;
            }
            return subscriberInfo.getLine1AlphaTagForSubscriber(i, getOpPackageName());
        } catch (RemoteException e2) {
            return null;
        } catch (NullPointerException e3) {
            return null;
        }
    }

    public String[] getMergedSubscriberIds() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.getMergedSubscriberIds(getOpPackageName());
            }
            return null;
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getMsisdn() {
        return getMsisdn(getSubId());
    }

    public String getMsisdn(int i) {
        try {
            IPhoneSubInfo subscriberInfo = getSubscriberInfo();
            if (subscriberInfo == null) {
                return null;
            }
            return subscriberInfo.getMsisdnForSubscriber(i, getOpPackageName());
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getVoiceMailNumber() {
        return getVoiceMailNumber(getSubId());
    }

    public String getVoiceMailNumber(int i) {
        try {
            IPhoneSubInfo subscriberInfo = getSubscriberInfo();
            if (subscriberInfo == null) {
                return null;
            }
            return subscriberInfo.getVoiceMailNumberForSubscriber(i, getOpPackageName());
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getCompleteVoiceMailNumber() {
        return getCompleteVoiceMailNumber(getSubId());
    }

    public String getCompleteVoiceMailNumber(int i) {
        try {
            IPhoneSubInfo subscriberInfo = getSubscriberInfo();
            if (subscriberInfo == null) {
                return null;
            }
            return subscriberInfo.getCompleteVoiceMailNumberForSubscriber(i);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public boolean setVoiceMailNumber(String str, String str2) {
        return setVoiceMailNumber(getSubId(), str, str2);
    }

    public boolean setVoiceMailNumber(int i, String str, String str2) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.setVoiceMailNumber(i, str, str2);
            }
            return false;
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    @SystemApi
    @SuppressLint({"Doclava125"})
    public void setVisualVoicemailEnabled(PhoneAccountHandle phoneAccountHandle, boolean z) {
    }

    @SystemApi
    @SuppressLint({"Doclava125"})
    public boolean isVisualVoicemailEnabled(PhoneAccountHandle phoneAccountHandle) {
        return false;
    }

    @SystemApi
    @SuppressLint({"Doclava125"})
    public Bundle getVisualVoicemailSettings() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.getVisualVoicemailSettings(this.mContext.getOpPackageName(), this.mSubId);
            }
            return null;
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getVisualVoicemailPackageName() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.getVisualVoicemailPackageName(this.mContext.getOpPackageName(), getSubId());
            }
            return null;
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public void setVisualVoicemailSmsFilterSettings(VisualVoicemailSmsFilterSettings visualVoicemailSmsFilterSettings) {
        if (visualVoicemailSmsFilterSettings == null) {
            disableVisualVoicemailSmsFilter(this.mSubId);
        } else {
            enableVisualVoicemailSmsFilter(this.mSubId, visualVoicemailSmsFilterSettings);
        }
    }

    public void sendVisualVoicemailSms(String str, int i, String str2, PendingIntent pendingIntent) {
        sendVisualVoicemailSmsForSubscriber(this.mSubId, str, i, str2, pendingIntent);
    }

    public void enableVisualVoicemailSmsFilter(int i, VisualVoicemailSmsFilterSettings visualVoicemailSmsFilterSettings) {
        if (visualVoicemailSmsFilterSettings == null) {
            throw new IllegalArgumentException("Settings cannot be null");
        }
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                iTelephony.enableVisualVoicemailSmsFilter(this.mContext.getOpPackageName(), i, visualVoicemailSmsFilterSettings);
            }
        } catch (RemoteException e) {
        } catch (NullPointerException e2) {
        }
    }

    public void disableVisualVoicemailSmsFilter(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                iTelephony.disableVisualVoicemailSmsFilter(this.mContext.getOpPackageName(), i);
            }
        } catch (RemoteException e) {
        } catch (NullPointerException e2) {
        }
    }

    public VisualVoicemailSmsFilterSettings getVisualVoicemailSmsFilterSettings(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.getVisualVoicemailSmsFilterSettings(this.mContext.getOpPackageName(), i);
            }
            return null;
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public VisualVoicemailSmsFilterSettings getActiveVisualVoicemailSmsFilterSettings(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.getActiveVisualVoicemailSmsFilterSettings(i);
            }
            return null;
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public void sendVisualVoicemailSmsForSubscriber(int i, String str, int i2, String str2, PendingIntent pendingIntent) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                iTelephony.sendVisualVoicemailSmsForSubscriber(this.mContext.getOpPackageName(), i, str, i2, str2, pendingIntent);
            }
        } catch (RemoteException e) {
        }
    }

    @SystemApi
    public void setVoiceActivationState(int i) {
        setVoiceActivationState(getSubId(), i);
    }

    public void setVoiceActivationState(int i, int i2) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                iTelephony.setVoiceActivationState(i, i2);
            }
        } catch (RemoteException e) {
        } catch (NullPointerException e2) {
        }
    }

    @SystemApi
    public void setDataActivationState(int i) {
        setDataActivationState(getSubId(), i);
    }

    public void setDataActivationState(int i, int i2) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                iTelephony.setDataActivationState(i, i2);
            }
        } catch (RemoteException e) {
        } catch (NullPointerException e2) {
        }
    }

    @SystemApi
    public int getVoiceActivationState() {
        return getVoiceActivationState(getSubId());
    }

    public int getVoiceActivationState(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.getVoiceActivationState(i, getOpPackageName());
            }
            return 0;
        } catch (RemoteException e) {
            return 0;
        } catch (NullPointerException e2) {
            return 0;
        }
    }

    @SystemApi
    public int getDataActivationState() {
        return getDataActivationState(getSubId());
    }

    public int getDataActivationState(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.getDataActivationState(i, getOpPackageName());
            }
            return 0;
        } catch (RemoteException e) {
            return 0;
        } catch (NullPointerException e2) {
            return 0;
        }
    }

    public int getVoiceMessageCount() {
        return getVoiceMessageCount(getSubId());
    }

    public int getVoiceMessageCount(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony == null) {
                return 0;
            }
            return iTelephony.getVoiceMessageCountForSubscriber(i);
        } catch (RemoteException e) {
            return 0;
        } catch (NullPointerException e2) {
            return 0;
        }
    }

    public String getVoiceMailAlphaTag() {
        return getVoiceMailAlphaTag(getSubId());
    }

    public String getVoiceMailAlphaTag(int i) {
        try {
            IPhoneSubInfo subscriberInfo = getSubscriberInfo();
            if (subscriberInfo == null) {
                return null;
            }
            return subscriberInfo.getVoiceMailAlphaTagForSubscriber(i, getOpPackageName());
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public void sendDialerSpecialCode(String str) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony == null) {
                if (!isSystemProcess()) {
                    throw new RuntimeException("Telephony service unavailable");
                }
            } else {
                iTelephony.sendDialerSpecialCode(this.mContext.getOpPackageName(), str);
            }
        } catch (RemoteException e) {
            if (!isSystemProcess()) {
                e.rethrowAsRuntimeException();
            }
        }
    }

    public String getIsimImpi() {
        try {
            IPhoneSubInfo subscriberInfo = getSubscriberInfo();
            if (subscriberInfo == null) {
                return null;
            }
            return subscriberInfo.getIsimImpi(getSubId());
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getIsimDomain() {
        try {
            IPhoneSubInfo subscriberInfo = getSubscriberInfo();
            if (subscriberInfo == null) {
                return null;
            }
            return subscriberInfo.getIsimDomain(getSubId());
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String[] getIsimImpu() {
        try {
            IPhoneSubInfo subscriberInfo = getSubscriberInfo();
            if (subscriberInfo == null) {
                return null;
            }
            return subscriberInfo.getIsimImpu(getSubId());
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
            ITelecomService telecomService = getTelecomService();
            if (telecomService != null) {
                return telecomService.getCallState();
            }
            return 0;
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelecomService#getCallState", e);
            return 0;
        }
    }

    public int getCallState(int i) {
        return getCallStateForSlot(SubscriptionManager.getPhoneId(i));
    }

    public int getCallStateForSlot(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony == null) {
                return 0;
            }
            return iTelephony.getCallStateForSlot(i);
        } catch (RemoteException e) {
            return 0;
        } catch (NullPointerException e2) {
            return 0;
        }
    }

    public int getDataActivity() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony == null) {
                return 0;
            }
            return iTelephony.getDataActivity();
        } catch (RemoteException e) {
            return 0;
        } catch (NullPointerException e2) {
            return 0;
        }
    }

    public int getDataState() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony == null) {
                return 0;
            }
            return iTelephony.getDataState();
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

    private ITelephonyRegistry getTelephonyRegistry() {
        return ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry"));
    }

    public void listen(PhoneStateListener phoneStateListener, int i) {
        if (this.mContext == null || phoneStateListener == null) {
            return;
        }
        try {
            boolean z = getITelephony() != null;
            if (phoneStateListener.mSubId == null) {
                phoneStateListener.mSubId = Integer.valueOf(this.mSubId);
            }
            ITelephonyRegistry telephonyRegistry = getTelephonyRegistry();
            if (telephonyRegistry != null) {
                telephonyRegistry.listenForSubscriber(phoneStateListener.mSubId.intValue(), getOpPackageName(), phoneStateListener.callback, i, z);
            } else {
                Rlog.w(TAG, "telephony registry not ready.");
            }
        } catch (RemoteException e) {
        }
    }

    public int getCdmaEriIconIndex() {
        return getCdmaEriIconIndex(getSubId());
    }

    public int getCdmaEriIconIndex(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony == null) {
                return -1;
            }
            return iTelephony.getCdmaEriIconIndexForSubscriber(i, getOpPackageName());
        } catch (RemoteException e) {
            return -1;
        } catch (NullPointerException e2) {
            return -1;
        }
    }

    public int getCdmaEriIconMode() {
        return getCdmaEriIconMode(getSubId());
    }

    public int getCdmaEriIconMode(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony == null) {
                return -1;
            }
            return iTelephony.getCdmaEriIconModeForSubscriber(i, getOpPackageName());
        } catch (RemoteException e) {
            return -1;
        } catch (NullPointerException e2) {
            return -1;
        }
    }

    public String getCdmaEriText() {
        return getCdmaEriText(getSubId());
    }

    public String getCdmaEriText(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony == null) {
                return null;
            }
            return iTelephony.getCdmaEriTextForSubscriber(i, getOpPackageName());
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
            ITelephony iTelephony = getITelephony();
            if (iTelephony == null) {
                return null;
            }
            return iTelephony.getAllCellInfo(getOpPackageName());
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public void setCellInfoListRate(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                iTelephony.setCellInfoListRate(i);
            }
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

    @Deprecated
    public IccOpenLogicalChannelResponse iccOpenLogicalChannel(String str) {
        return iccOpenLogicalChannel(getSubId(), str, -1);
    }

    public IccOpenLogicalChannelResponse iccOpenLogicalChannel(String str, int i) {
        return iccOpenLogicalChannel(getSubId(), str, i);
    }

    public IccOpenLogicalChannelResponse iccOpenLogicalChannel(int i, String str, int i2) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.iccOpenLogicalChannel(i, getOpPackageName(), str, i2);
            }
            return null;
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public boolean iccCloseLogicalChannel(int i) {
        return iccCloseLogicalChannel(getSubId(), i);
    }

    public boolean iccCloseLogicalChannel(int i, int i2) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.iccCloseLogicalChannel(i, i2);
            }
            return false;
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public String iccTransmitApduLogicalChannel(int i, int i2, int i3, int i4, int i5, int i6, String str) {
        return iccTransmitApduLogicalChannel(getSubId(), i, i2, i3, i4, i5, i6, str);
    }

    public String iccTransmitApduLogicalChannel(int i, int i2, int i3, int i4, int i5, int i6, int i7, String str) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.iccTransmitApduLogicalChannel(i, i2, i3, i4, i5, i6, i7, str);
            }
            return "";
        } catch (RemoteException e) {
            return "";
        } catch (NullPointerException e2) {
            return "";
        }
    }

    public String iccTransmitApduBasicChannel(int i, int i2, int i3, int i4, int i5, String str) {
        return iccTransmitApduBasicChannel(getSubId(), i, i2, i3, i4, i5, str);
    }

    public String iccTransmitApduBasicChannel(int i, int i2, int i3, int i4, int i5, int i6, String str) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.iccTransmitApduBasicChannel(i, getOpPackageName(), i2, i3, i4, i5, i6, str);
            }
            return "";
        } catch (RemoteException e) {
            return "";
        } catch (NullPointerException e2) {
            return "";
        }
    }

    public byte[] iccExchangeSimIO(int i, int i2, int i3, int i4, int i5, String str) {
        return iccExchangeSimIO(getSubId(), i, i2, i3, i4, i5, str);
    }

    public byte[] iccExchangeSimIO(int i, int i2, int i3, int i4, int i5, int i6, String str) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.iccExchangeSimIO(i, i2, i3, i4, i5, i6, str);
            }
            return null;
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String sendEnvelopeWithStatus(String str) {
        return sendEnvelopeWithStatus(getSubId(), str);
    }

    public String sendEnvelopeWithStatus(int i, String str) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.sendEnvelopeWithStatus(i, str);
            }
            return "";
        } catch (RemoteException e) {
            return "";
        } catch (NullPointerException e2) {
            return "";
        }
    }

    public String nvReadItem(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.nvReadItem(i);
            }
            return "";
        } catch (RemoteException e) {
            Rlog.e(TAG, "nvReadItem RemoteException", e);
            return "";
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "nvReadItem NPE", e2);
            return "";
        }
    }

    public boolean nvWriteItem(int i, String str) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.nvWriteItem(i, str);
            }
            return false;
        } catch (RemoteException e) {
            Rlog.e(TAG, "nvWriteItem RemoteException", e);
            return false;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "nvWriteItem NPE", e2);
            return false;
        }
    }

    public boolean nvWriteCdmaPrl(byte[] bArr) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.nvWriteCdmaPrl(bArr);
            }
            return false;
        } catch (RemoteException e) {
            Rlog.e(TAG, "nvWriteCdmaPrl RemoteException", e);
            return false;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "nvWriteCdmaPrl NPE", e2);
            return false;
        }
    }

    public boolean nvResetConfig(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.nvResetConfig(i);
            }
            return false;
        } catch (RemoteException e) {
            Rlog.e(TAG, "nvResetConfig RemoteException", e);
            return false;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "nvResetConfig NPE", e2);
            return false;
        }
    }

    private int getSubId() {
        if (SubscriptionManager.isUsableSubIdValue(this.mSubId)) {
            return this.mSubId;
        }
        return SubscriptionManager.getDefaultSubscriptionId();
    }

    private int getSubId(int i) {
        if (SubscriptionManager.isUsableSubIdValue(this.mSubId)) {
            return this.mSubId;
        }
        return i;
    }

    private int getPhoneId() {
        return SubscriptionManager.getPhoneId(getSubId());
    }

    private int getPhoneId(int i) {
        return SubscriptionManager.getPhoneId(getSubId(i));
    }

    @VisibleForTesting
    public int getSlotIndex() {
        int slotIndex = SubscriptionManager.getSlotIndex(getSubId());
        if (slotIndex == -1) {
            return Integer.MAX_VALUE;
        }
        return slotIndex;
    }

    public static void setTelephonyProperty(int i, String str, String str2) {
        String[] strArrSplit;
        String str3 = "";
        String str4 = SystemProperties.get(str);
        if (str2 == null) {
            str2 = "";
        }
        if (str4 != null) {
            strArrSplit = str4.split(",");
        } else {
            strArrSplit = null;
        }
        if (!SubscriptionManager.isValidPhoneId(i)) {
            Rlog.d(TAG, "setTelephonyProperty: invalid phoneId=" + i + " property=" + str + " value: " + str2 + " prop=" + str4);
            return;
        }
        for (int i2 = 0; i2 < i; i2++) {
            String str5 = "";
            if (strArrSplit != null && i2 < strArrSplit.length) {
                str5 = strArrSplit[i2];
            }
            str3 = str3 + str5 + ",";
        }
        String str6 = str3 + str2;
        if (strArrSplit != null) {
            for (int i3 = i + 1; i3 < strArrSplit.length; i3++) {
                str6 = str6 + "," + strArrSplit[i3];
            }
        }
        if (str6.length() > 91) {
            Rlog.d(TAG, "setTelephonyProperty: property too long phoneId=" + i + " property=" + str + " value: " + str2 + " propVal=" + str6);
            return;
        }
        SystemProperties.set(str, str6);
    }

    public static void setTelephonyProperty(String str, String str2) {
        if (str2 == null) {
            str2 = "";
        }
        Rlog.d(TAG, "setTelephonyProperty: success property=" + str + " value: " + str2);
        SystemProperties.set(str, str2);
    }

    public static int getIntAtIndex(ContentResolver contentResolver, String str, int i) throws Settings.SettingNotFoundException {
        String string = Settings.Global.getString(contentResolver, str);
        if (string != null) {
            String[] strArrSplit = string.split(",");
            if (i >= 0 && i < strArrSplit.length && strArrSplit[i] != null) {
                try {
                    return Integer.parseInt(strArrSplit[i]);
                } catch (NumberFormatException e) {
                }
            }
        }
        throw new Settings.SettingNotFoundException(str);
    }

    public static boolean putIntAtIndex(ContentResolver contentResolver, String str, int i, int i2) {
        String[] strArrSplit;
        String str2 = "";
        String string = Settings.Global.getString(contentResolver, str);
        if (i == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("putIntAtIndex index == MAX_VALUE index=" + i);
        }
        if (i < 0) {
            throw new IllegalArgumentException("putIntAtIndex index < 0 index=" + i);
        }
        if (string != null) {
            strArrSplit = string.split(",");
        } else {
            strArrSplit = null;
        }
        for (int i3 = 0; i3 < i; i3++) {
            String str3 = "";
            if (strArrSplit != null && i3 < strArrSplit.length) {
                str3 = strArrSplit[i3];
            }
            str2 = str2 + str3 + ",";
        }
        String str4 = str2 + i2;
        if (strArrSplit != null) {
            while (true) {
                i++;
                if (i >= strArrSplit.length) {
                    break;
                }
                str4 = str4 + "," + strArrSplit[i];
            }
        }
        return Settings.Global.putString(contentResolver, str, str4);
    }

    public static String getTelephonyProperty(int i, String str, String str2) {
        String str3;
        String str4 = SystemProperties.get(str);
        if (str4 != null && str4.length() > 0) {
            String[] strArrSplit = str4.split(",");
            if (i >= 0 && i < strArrSplit.length && strArrSplit[i] != null) {
                str3 = strArrSplit[i];
            }
        } else {
            str3 = null;
        }
        return str3 == null ? str2 : str3;
    }

    public static String getTelephonyProperty(String str, String str2) {
        String str3 = SystemProperties.get(str);
        return str3 == null ? str2 : str3;
    }

    public int getSimCount() {
        if (isMultiSimEnabled()) {
            return getPhoneCount();
        }
        return 1;
    }

    public String getIsimIst() {
        try {
            IPhoneSubInfo subscriberInfo = getSubscriberInfo();
            if (subscriberInfo == null) {
                return null;
            }
            return subscriberInfo.getIsimIst(getSubId());
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String[] getIsimPcscf() {
        try {
            IPhoneSubInfo subscriberInfo = getSubscriberInfo();
            if (subscriberInfo == null) {
                return null;
            }
            return subscriberInfo.getIsimPcscf(getSubId());
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String getIccAuthentication(int i, int i2, String str) {
        return getIccAuthentication(getSubId(), i, i2, str);
    }

    public String getIccAuthentication(int i, int i2, int i3, String str) {
        try {
            IPhoneSubInfo subscriberInfo = getSubscriberInfo();
            if (subscriberInfo == null) {
                return null;
            }
            return subscriberInfo.getIccSimChallengeResponse(i, i2, i3, str);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String[] getForbiddenPlmns() {
        return getForbiddenPlmns(getSubId(), 2);
    }

    public String[] getForbiddenPlmns(int i, int i2) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony == null) {
                return null;
            }
            return iTelephony.getForbiddenPlmns(i, i2, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    public String[] getPcscfAddress(String str) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony == null) {
                return new String[0];
            }
            return iTelephony.getPcscfAddress(str, getOpPackageName());
        } catch (RemoteException e) {
            return new String[0];
        }
    }

    public void enableIms(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                iTelephony.enableIms(i);
            }
        } catch (RemoteException e) {
            Rlog.e(TAG, "enableIms, RemoteException: " + e.getMessage());
        }
    }

    public void disableIms(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                iTelephony.disableIms(i);
            }
        } catch (RemoteException e) {
            Rlog.e(TAG, "disableIms, RemoteException: " + e.getMessage());
        }
    }

    public IImsMmTelFeature getImsMmTelFeatureAndListen(int i, IImsServiceFeatureCallback iImsServiceFeatureCallback) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.getMmTelFeatureAndListen(i, iImsServiceFeatureCallback);
            }
            return null;
        } catch (RemoteException e) {
            Rlog.e(TAG, "getImsMmTelFeatureAndListen, RemoteException: " + e.getMessage());
            return null;
        }
    }

    public IImsRcsFeature getImsRcsFeatureAndListen(int i, IImsServiceFeatureCallback iImsServiceFeatureCallback) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.getRcsFeatureAndListen(i, iImsServiceFeatureCallback);
            }
            return null;
        } catch (RemoteException e) {
            Rlog.e(TAG, "getImsRcsFeatureAndListen, RemoteException: " + e.getMessage());
            return null;
        }
    }

    public IImsRegistration getImsRegistration(int i, int i2) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.getImsRegistration(i, i2);
            }
            return null;
        } catch (RemoteException e) {
            Rlog.e(TAG, "getImsRegistration, RemoteException: " + e.getMessage());
            return null;
        }
    }

    public IImsConfig getImsConfig(int i, int i2) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.getImsConfig(i, i2);
            }
            return null;
        } catch (RemoteException e) {
            Rlog.e(TAG, "getImsRegistration, RemoteException: " + e.getMessage());
            return null;
        }
    }

    public boolean isResolvingImsBinding() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.isResolvingImsBinding();
            }
            return false;
        } catch (RemoteException e) {
            Rlog.e(TAG, "isResolvingImsBinding, RemoteException: " + e.getMessage());
            return false;
        }
    }

    public void setImsRegistrationState(boolean z) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                iTelephony.setImsRegistrationState(z);
            }
        } catch (RemoteException e) {
        }
    }

    public int getPreferredNetworkType(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.getPreferredNetworkType(i);
            }
            return -1;
        } catch (RemoteException e) {
            Rlog.e(TAG, "getPreferredNetworkType RemoteException", e);
            return -1;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "getPreferredNetworkType NPE", e2);
            return -1;
        }
    }

    public void setNetworkSelectionModeAutomatic() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                iTelephony.setNetworkSelectionModeAutomatic(getSubId());
            }
        } catch (RemoteException e) {
            Rlog.e(TAG, "setNetworkSelectionModeAutomatic RemoteException", e);
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "setNetworkSelectionModeAutomatic NPE", e2);
        }
    }

    public CellNetworkScanResult getCellNetworkScanResults(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.getCellNetworkScanResults(i);
            }
            return null;
        } catch (RemoteException e) {
            Rlog.e(TAG, "getCellNetworkScanResults RemoteException", e);
            return null;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "getCellNetworkScanResults NPE", e2);
            return null;
        }
    }

    public NetworkScan requestNetworkScan(NetworkScanRequest networkScanRequest, Executor executor, TelephonyScanManager.NetworkScanCallback networkScanCallback) {
        synchronized (this) {
            if (this.mTelephonyScanManager == null) {
                this.mTelephonyScanManager = new TelephonyScanManager();
            }
        }
        return this.mTelephonyScanManager.requestNetworkScan(getSubId(), networkScanRequest, executor, networkScanCallback);
    }

    @Deprecated
    public NetworkScan requestNetworkScan(NetworkScanRequest networkScanRequest, TelephonyScanManager.NetworkScanCallback networkScanCallback) {
        return requestNetworkScan(networkScanRequest, AsyncTask.SERIAL_EXECUTOR, networkScanCallback);
    }

    public boolean setNetworkSelectionModeManual(String str, boolean z) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.setNetworkSelectionModeManual(getSubId(), str, z);
            }
            return false;
        } catch (RemoteException e) {
            Rlog.e(TAG, "setNetworkSelectionModeManual RemoteException", e);
            return false;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "setNetworkSelectionModeManual NPE", e2);
            return false;
        }
    }

    public boolean setPreferredNetworkType(int i, int i2) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.setPreferredNetworkType(i, i2);
            }
            return false;
        } catch (RemoteException e) {
            Rlog.e(TAG, "setPreferredNetworkType RemoteException", e);
            return false;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "setPreferredNetworkType NPE", e2);
            return false;
        }
    }

    public boolean setPreferredNetworkTypeToGlobal() {
        return setPreferredNetworkTypeToGlobal(getSubId());
    }

    public boolean setPreferredNetworkTypeToGlobal(int i) {
        return setPreferredNetworkType(i, 10);
    }

    public int getTetherApnRequired() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.getTetherApnRequired();
            }
            return 2;
        } catch (RemoteException e) {
            Rlog.e(TAG, "hasMatchedTetherApnSetting RemoteException", e);
            return 2;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "hasMatchedTetherApnSetting NPE", e2);
            return 2;
        }
    }

    public boolean hasCarrierPrivileges() {
        return hasCarrierPrivileges(getSubId());
    }

    public boolean hasCarrierPrivileges(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.getCarrierPrivilegeStatus(this.mSubId) == 1;
            }
        } catch (RemoteException e) {
            Rlog.e(TAG, "hasCarrierPrivileges RemoteException", e);
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "hasCarrierPrivileges NPE", e2);
        }
        return false;
    }

    public boolean setOperatorBrandOverride(String str) {
        return setOperatorBrandOverride(getSubId(), str);
    }

    public boolean setOperatorBrandOverride(int i, String str) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.setOperatorBrandOverride(i, str);
            }
            return false;
        } catch (RemoteException e) {
            Rlog.e(TAG, "setOperatorBrandOverride RemoteException", e);
            return false;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "setOperatorBrandOverride NPE", e2);
            return false;
        }
    }

    public boolean setRoamingOverride(List<String> list, List<String> list2, List<String> list3, List<String> list4) {
        return setRoamingOverride(getSubId(), list, list2, list3, list4);
    }

    public boolean setRoamingOverride(int i, List<String> list, List<String> list2, List<String> list3, List<String> list4) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.setRoamingOverride(i, list, list2, list3, list4);
            }
            return false;
        } catch (RemoteException e) {
            Rlog.e(TAG, "setRoamingOverride RemoteException", e);
            return false;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "setRoamingOverride NPE", e2);
            return false;
        }
    }

    @SystemApi
    public String getCdmaMdn() {
        return getCdmaMdn(getSubId());
    }

    @SystemApi
    public String getCdmaMdn(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony == null) {
                return null;
            }
            return iTelephony.getCdmaMdn(i);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    @SystemApi
    public String getCdmaMin() {
        return getCdmaMin(getSubId());
    }

    @SystemApi
    public String getCdmaMin(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony == null) {
                return null;
            }
            return iTelephony.getCdmaMin(i);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    @SystemApi
    @SuppressLint({"Doclava125"})
    public int checkCarrierPrivilegesForPackage(String str) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.checkCarrierPrivilegesForPackage(str);
            }
            return 0;
        } catch (RemoteException e) {
            Rlog.e(TAG, "checkCarrierPrivilegesForPackage RemoteException", e);
            return 0;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "checkCarrierPrivilegesForPackage NPE", e2);
            return 0;
        }
    }

    @SystemApi
    @SuppressLint({"Doclava125"})
    public int checkCarrierPrivilegesForPackageAnyPhone(String str) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.checkCarrierPrivilegesForPackageAnyPhone(str);
            }
            return 0;
        } catch (RemoteException e) {
            Rlog.e(TAG, "checkCarrierPrivilegesForPackageAnyPhone RemoteException", e);
            return 0;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "checkCarrierPrivilegesForPackageAnyPhone NPE", e2);
            return 0;
        }
    }

    @SystemApi
    public List<String> getCarrierPackageNamesForIntent(Intent intent) {
        return getCarrierPackageNamesForIntentAndPhone(intent, getPhoneId());
    }

    @SystemApi
    public List<String> getCarrierPackageNamesForIntentAndPhone(Intent intent, int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.getCarrierPackageNamesForIntentAndPhone(intent, i);
            }
            return null;
        } catch (RemoteException e) {
            Rlog.e(TAG, "getCarrierPackageNamesForIntentAndPhone RemoteException", e);
            return null;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "getCarrierPackageNamesForIntentAndPhone NPE", e2);
            return null;
        }
    }

    public List<String> getPackagesWithCarrierPrivileges() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.getPackagesWithCarrierPrivileges();
            }
        } catch (RemoteException e) {
            Rlog.e(TAG, "getPackagesWithCarrierPrivileges RemoteException", e);
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "getPackagesWithCarrierPrivileges NPE", e2);
        }
        return Collections.EMPTY_LIST;
    }

    @SystemApi
    @SuppressLint({"Doclava125"})
    public void dial(String str) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                iTelephony.dial(str);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#dial", e);
        }
    }

    @SystemApi
    @Deprecated
    public void call(String str, String str2) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                iTelephony.call(str, str2);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#call", e);
        }
    }

    @SystemApi
    @Deprecated
    public boolean endCall() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.endCall();
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#endCall", e);
            return false;
        }
    }

    @SystemApi
    @Deprecated
    public void answerRingingCall() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                iTelephony.answerRingingCall();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#answerRingingCall", e);
        }
    }

    @SystemApi
    @SuppressLint({"Doclava125"})
    @Deprecated
    public void silenceRinger() {
        try {
            getTelecomService().silenceRinger(getOpPackageName());
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelecomService#silenceRinger", e);
        }
    }

    @SystemApi
    public boolean isOffhook() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.isOffhook(getOpPackageName());
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#isOffhook", e);
            return false;
        }
    }

    @SystemApi
    public boolean isRinging() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.isRinging(getOpPackageName());
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#isRinging", e);
            return false;
        }
    }

    @SystemApi
    public boolean isIdle() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.isIdle(getOpPackageName());
            }
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#isIdle", e);
            return true;
        }
    }

    @SystemApi
    public boolean isRadioOn() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.isRadioOn(getOpPackageName());
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#isRadioOn", e);
            return false;
        }
    }

    @SystemApi
    public boolean supplyPin(String str) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.supplyPin(str);
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#supplyPin", e);
            return false;
        }
    }

    @SystemApi
    public boolean supplyPuk(String str, String str2) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.supplyPuk(str, str2);
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#supplyPuk", e);
            return false;
        }
    }

    @SystemApi
    public int[] supplyPinReportResult(String str) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.supplyPinReportResult(str);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#supplyPinReportResult", e);
        }
        return new int[0];
    }

    @SystemApi
    public int[] supplyPukReportResult(String str, String str2) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.supplyPukReportResult(str, str2);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#]", e);
        }
        return new int[0];
    }

    public static abstract class UssdResponseCallback {
        public void onReceiveUssdResponse(TelephonyManager telephonyManager, String str, CharSequence charSequence) {
        }

        public void onReceiveUssdResponseFailed(TelephonyManager telephonyManager, String str, int i) {
        }
    }

    public void sendUssdRequest(String str, final UssdResponseCallback ussdResponseCallback, Handler handler) {
        Preconditions.checkNotNull(ussdResponseCallback, "UssdResponseCallback cannot be null.");
        ResultReceiver resultReceiver = new ResultReceiver(handler) {
            @Override
            protected void onReceiveResult(int i, Bundle bundle) {
                Rlog.d(TelephonyManager.TAG, "USSD:" + i);
                Preconditions.checkNotNull(bundle, "ussdResponse cannot be null.");
                UssdResponse ussdResponse = (UssdResponse) bundle.getParcelable(TelephonyManager.USSD_RESPONSE);
                if (i == 100) {
                    ussdResponseCallback.onReceiveUssdResponse(this, ussdResponse.getUssdRequest(), ussdResponse.getReturnMessage());
                } else {
                    ussdResponseCallback.onReceiveUssdResponseFailed(this, ussdResponse.getUssdRequest(), i);
                }
            }
        };
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                iTelephony.handleUssdRequest(getSubId(), str, resultReceiver);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#sendUSSDCode", e);
            UssdResponse ussdResponse = new UssdResponse(str, "");
            Bundle bundle = new Bundle();
            bundle.putParcelable(USSD_RESPONSE, ussdResponse);
            resultReceiver.send(-2, bundle);
        }
    }

    public boolean isConcurrentVoiceAndDataSupported() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony == null) {
                return false;
            }
            return iTelephony.isConcurrentVoiceAndDataAllowed(getSubId());
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#isConcurrentVoiceAndDataAllowed", e);
            return false;
        }
    }

    @SystemApi
    public boolean handlePinMmi(String str) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.handlePinMmi(str);
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#handlePinMmi", e);
            return false;
        }
    }

    @SystemApi
    public boolean handlePinMmiForSubscriber(int i, String str) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.handlePinMmiForSubscriber(i, str);
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#handlePinMmi", e);
            return false;
        }
    }

    @SystemApi
    public void toggleRadioOnOff() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                iTelephony.toggleRadioOnOff();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#toggleRadioOnOff", e);
        }
    }

    @SystemApi
    public boolean setRadio(boolean z) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.setRadio(z);
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#setRadio", e);
            return false;
        }
    }

    @SystemApi
    public boolean setRadioPower(boolean z) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.setRadioPower(z);
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#setRadioPower", e);
            return false;
        }
    }

    @SystemApi
    @SuppressLint({"Doclava125"})
    public void updateServiceLocation() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                iTelephony.updateServiceLocation();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#updateServiceLocation", e);
        }
    }

    @SystemApi
    public boolean enableDataConnectivity() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.enableDataConnectivity();
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#enableDataConnectivity", e);
            return false;
        }
    }

    @SystemApi
    public boolean disableDataConnectivity() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.disableDataConnectivity();
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#disableDataConnectivity", e);
            return false;
        }
    }

    @SystemApi
    public boolean isDataConnectivityPossible() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.isDataConnectivityPossible(getSubId(SubscriptionManager.getDefaultDataSubscriptionId()));
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#isDataAllowed", e);
            return false;
        }
    }

    @SystemApi
    public boolean needsOtaServiceProvisioning() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.needsOtaServiceProvisioning();
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#needsOtaServiceProvisioning", e);
            return false;
        }
    }

    public void setDataEnabled(boolean z) {
        setDataEnabled(getSubId(SubscriptionManager.getDefaultDataSubscriptionId()), z);
    }

    @SystemApi
    @Deprecated
    public void setDataEnabled(int i, boolean z) {
        try {
            Log.d(TAG, "setDataEnabled: enabled=" + z);
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                iTelephony.setUserDataEnabled(i, z);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#setUserDataEnabled", e);
        }
    }

    @SystemApi
    @Deprecated
    public boolean getDataEnabled() {
        return isDataEnabled();
    }

    public boolean isDataEnabled() {
        return getDataEnabled(getSubId(SubscriptionManager.getDefaultDataSubscriptionId()));
    }

    @SystemApi
    @Deprecated
    public boolean getDataEnabled(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony == null) {
                return false;
            }
            return iTelephony.isUserDataEnabled(i);
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#isUserDataEnabled", e);
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    @Deprecated
    public int invokeOemRilRequestRaw(byte[] bArr, byte[] bArr2) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.invokeOemRilRequestRaw(bArr, bArr2);
            }
            return -1;
        } catch (RemoteException e) {
            return -1;
        } catch (NullPointerException e2) {
            return -1;
        }
    }

    @SystemApi
    public void enableVideoCalling(boolean z) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                iTelephony.enableVideoCalling(z);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#enableVideoCalling", e);
        }
    }

    @SystemApi
    public boolean isVideoCallingEnabled() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.isVideoCallingEnabled(getOpPackageName());
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#isVideoCallingEnabled", e);
            return false;
        }
    }

    public boolean canChangeDtmfToneLength() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.canChangeDtmfToneLength();
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#canChangeDtmfToneLength", e);
            return false;
        } catch (SecurityException e2) {
            Log.e(TAG, "Permission error calling ITelephony#canChangeDtmfToneLength", e2);
            return false;
        }
    }

    public boolean isWorldPhone() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.isWorldPhone();
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#isWorldPhone", e);
            return false;
        } catch (SecurityException e2) {
            Log.e(TAG, "Permission error calling ITelephony#isWorldPhone", e2);
            return false;
        }
    }

    @Deprecated
    public boolean isTtyModeSupported() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.isTtyModeSupported();
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#isTtyModeSupported", e);
            return false;
        } catch (SecurityException e2) {
            Log.e(TAG, "Permission error calling ITelephony#isTtyModeSupported", e2);
            return false;
        }
    }

    public boolean isHearingAidCompatibilitySupported() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.isHearingAidCompatibilitySupported();
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#isHearingAidCompatibilitySupported", e);
            return false;
        } catch (SecurityException e2) {
            Log.e(TAG, "Permission error calling ITelephony#isHearingAidCompatibilitySupported", e2);
            return false;
        }
    }

    public boolean isImsRegistered(int i) {
        try {
            return getITelephony().isImsRegistered(i);
        } catch (RemoteException | NullPointerException e) {
            return false;
        }
    }

    public boolean isImsRegistered() {
        try {
            return getITelephony().isImsRegistered(getSubId());
        } catch (RemoteException | NullPointerException e) {
            return false;
        }
    }

    public boolean isVolteAvailable() {
        try {
            return getITelephony().isVolteAvailable(getSubId());
        } catch (RemoteException | NullPointerException e) {
            return false;
        }
    }

    public boolean isVideoTelephonyAvailable() {
        try {
            return getITelephony().isVideoTelephonyAvailable(getSubId());
        } catch (RemoteException | NullPointerException e) {
            return false;
        }
    }

    public boolean isWifiCallingAvailable() {
        try {
            return getITelephony().isWifiCallingAvailable(getSubId());
        } catch (RemoteException | NullPointerException e) {
            return false;
        }
    }

    public int getImsRegTechnologyForMmTel() {
        try {
            return getITelephony().getImsRegTechnologyForMmTel(getSubId());
        } catch (RemoteException e) {
            return -1;
        }
    }

    public void setSimOperatorNumeric(String str) {
        setSimOperatorNumericForPhone(getPhoneId(), str);
    }

    public void setSimOperatorNumericForPhone(int i, String str) {
        setTelephonyProperty(i, TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC, str);
    }

    public void setSimOperatorName(String str) {
        setSimOperatorNameForPhone(getPhoneId(), str);
    }

    public void setSimOperatorNameForPhone(int i, String str) {
        setTelephonyProperty(i, TelephonyProperties.PROPERTY_ICC_OPERATOR_ALPHA, str);
    }

    public void setSimCountryIso(String str) {
        setSimCountryIsoForPhone(getPhoneId(), str);
    }

    public void setSimCountryIsoForPhone(int i, String str) {
        setTelephonyProperty(i, TelephonyProperties.PROPERTY_ICC_OPERATOR_ISO_COUNTRY, str);
    }

    public void setSimState(String str) {
        setSimStateForPhone(getPhoneId(), str);
    }

    public void setSimStateForPhone(int i, String str) {
        setTelephonyProperty(i, TelephonyProperties.PROPERTY_SIM_STATE, str);
    }

    @SystemApi
    public void setSimPowerState(int i) {
        setSimPowerStateForSlot(getSlotIndex(), i);
    }

    @SystemApi
    public void setSimPowerStateForSlot(int i, int i2) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                iTelephony.setSimPowerStateForSlot(i, i2);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#setSimPowerStateForSlot", e);
        } catch (SecurityException e2) {
            Log.e(TAG, "Permission error calling ITelephony#setSimPowerStateForSlot", e2);
        }
    }

    public void setBasebandVersion(String str) {
        setBasebandVersionForPhone(getPhoneId(), str);
    }

    public void setBasebandVersionForPhone(int i, String str) {
        setTelephonyProperty(i, TelephonyProperties.PROPERTY_BASEBAND_VERSION, str);
    }

    public String getBasebandVersion() {
        return getBasebandVersionForPhone(getPhoneId());
    }

    private String getBasebandVersionLegacy(int i) {
        if (SubscriptionManager.isValidPhoneId(i)) {
            StringBuilder sb = new StringBuilder();
            sb.append(TelephonyProperties.PROPERTY_BASEBAND_VERSION);
            sb.append(i == 0 ? "" : Integer.toString(i));
            return SystemProperties.get(sb.toString());
        }
        return null;
    }

    public String getBasebandVersionForPhone(int i) {
        String basebandVersionLegacy = getBasebandVersionLegacy(i);
        if (basebandVersionLegacy != null && !basebandVersionLegacy.isEmpty()) {
            setBasebandVersionForPhone(i, basebandVersionLegacy);
        }
        return getTelephonyProperty(i, TelephonyProperties.PROPERTY_BASEBAND_VERSION, "");
    }

    public void setPhoneType(int i) {
        setPhoneType(getPhoneId(), i);
    }

    public void setPhoneType(int i, int i2) {
        if (SubscriptionManager.isValidPhoneId(i)) {
            setTelephonyProperty(i, TelephonyProperties.CURRENT_ACTIVE_PHONE, String.valueOf(i2));
        }
    }

    public String getOtaSpNumberSchema(String str) {
        return getOtaSpNumberSchemaForPhone(getPhoneId(), str);
    }

    public String getOtaSpNumberSchemaForPhone(int i, String str) {
        if (SubscriptionManager.isValidPhoneId(i)) {
            return getTelephonyProperty(i, TelephonyProperties.PROPERTY_OTASP_NUM_SCHEMA, str);
        }
        return str;
    }

    public boolean getSmsReceiveCapable(boolean z) {
        return getSmsReceiveCapableForPhone(getPhoneId(), z);
    }

    public boolean getSmsReceiveCapableForPhone(int i, boolean z) {
        if (SubscriptionManager.isValidPhoneId(i)) {
            return Boolean.parseBoolean(getTelephonyProperty(i, TelephonyProperties.PROPERTY_SMS_RECEIVE, String.valueOf(z)));
        }
        return z;
    }

    public boolean getSmsSendCapable(boolean z) {
        return getSmsSendCapableForPhone(getPhoneId(), z);
    }

    public boolean getSmsSendCapableForPhone(int i, boolean z) {
        if (SubscriptionManager.isValidPhoneId(i)) {
            return Boolean.parseBoolean(getTelephonyProperty(i, TelephonyProperties.PROPERTY_SMS_SEND, String.valueOf(z)));
        }
        return z;
    }

    public void setNetworkOperatorName(String str) {
        setNetworkOperatorNameForPhone(getPhoneId(), str);
    }

    public void setNetworkOperatorNameForPhone(int i, String str) {
        if (SubscriptionManager.isValidPhoneId(i)) {
            setTelephonyProperty(i, TelephonyProperties.PROPERTY_OPERATOR_ALPHA, str);
        }
    }

    public void setNetworkOperatorNumeric(String str) {
        setNetworkOperatorNumericForPhone(getPhoneId(), str);
    }

    public void setNetworkOperatorNumericForPhone(int i, String str) {
        setTelephonyProperty(i, TelephonyProperties.PROPERTY_OPERATOR_NUMERIC, str);
    }

    public void setNetworkRoaming(boolean z) {
        setNetworkRoamingForPhone(getPhoneId(), z);
    }

    public void setNetworkRoamingForPhone(int i, boolean z) {
        if (SubscriptionManager.isValidPhoneId(i)) {
            setTelephonyProperty(i, TelephonyProperties.PROPERTY_OPERATOR_ISROAMING, z ? "true" : "false");
        }
    }

    public void setNetworkCountryIso(String str) {
        setNetworkCountryIsoForPhone(getPhoneId(), str);
    }

    public void setNetworkCountryIsoForPhone(int i, String str) {
        if (SubscriptionManager.isValidPhoneId(i)) {
            setTelephonyProperty(i, TelephonyProperties.PROPERTY_OPERATOR_ISO_COUNTRY, str);
        }
    }

    public void setDataNetworkType(int i) {
        setDataNetworkTypeForPhone(getPhoneId(SubscriptionManager.getDefaultDataSubscriptionId()), i);
    }

    public void setDataNetworkTypeForPhone(int i, int i2) {
        if (SubscriptionManager.isValidPhoneId(i)) {
            setTelephonyProperty(i, TelephonyProperties.PROPERTY_DATA_NETWORK_TYPE, ServiceState.rilRadioTechnologyToString(i2));
        }
    }

    public int getSubIdForPhoneAccount(PhoneAccount phoneAccount) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony == null) {
                return -1;
            }
            return iTelephony.getSubIdForPhoneAccount(phoneAccount);
        } catch (RemoteException e) {
            return -1;
        }
    }

    private int getSubIdForPhoneAccountHandle(PhoneAccountHandle phoneAccountHandle) {
        try {
            ITelecomService telecomService = getTelecomService();
            if (telecomService == null) {
                return -1;
            }
            return getSubIdForPhoneAccount(telecomService.getPhoneAccount(phoneAccountHandle));
        } catch (RemoteException e) {
            return -1;
        }
    }

    public void factoryReset(int i) {
        try {
            Log.d(TAG, "factoryReset: subId=" + i);
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                iTelephony.factoryReset(i);
            }
        } catch (RemoteException e) {
        }
    }

    public String getLocaleFromDefaultSim() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.getLocaleFromDefaultSim();
            }
            return null;
        } catch (RemoteException e) {
            return null;
        }
    }

    public void requestModemActivityInfo(ResultReceiver resultReceiver) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                iTelephony.requestModemActivityInfo(resultReceiver);
                return;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#getModemActivityInfo", e);
        }
        resultReceiver.send(0, null);
    }

    public ServiceState getServiceState() {
        return getServiceStateForSubscriber(getSubId());
    }

    public ServiceState getServiceStateForSubscriber(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.getServiceStateForSubscriber(i, getOpPackageName());
            }
            return null;
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#getServiceStateForSubscriber", e);
            return null;
        }
    }

    public Uri getVoicemailRingtoneUri(PhoneAccountHandle phoneAccountHandle) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.getVoicemailRingtoneUri(phoneAccountHandle);
            }
            return null;
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#getVoicemailRingtoneUri", e);
            return null;
        }
    }

    public void setVoicemailRingtoneUri(PhoneAccountHandle phoneAccountHandle, Uri uri) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                iTelephony.setVoicemailRingtoneUri(getOpPackageName(), phoneAccountHandle, uri);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#setVoicemailRingtoneUri", e);
        }
    }

    public boolean isVoicemailVibrationEnabled(PhoneAccountHandle phoneAccountHandle) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.isVoicemailVibrationEnabled(phoneAccountHandle);
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#isVoicemailVibrationEnabled", e);
            return false;
        }
    }

    public void setVoicemailVibrationEnabled(PhoneAccountHandle phoneAccountHandle, boolean z) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                iTelephony.setVoicemailVibrationEnabled(getOpPackageName(), phoneAccountHandle, z);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#isVoicemailVibrationEnabled", e);
        }
    }

    public int getSimCarrierId() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.getSubscriptionCarrierId(getSubId());
            }
            return -1;
        } catch (RemoteException e) {
            return -1;
        }
    }

    public CharSequence getSimCarrierIdName() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.getSubscriptionCarrierName(getSubId());
            }
            return null;
        } catch (RemoteException e) {
            return null;
        }
    }

    public String getAidForAppType(int i) {
        return getAidForAppType(getSubId(), i);
    }

    public String getAidForAppType(int i, int i2) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.getAidForAppType(i, i2);
            }
            return null;
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#getAidForAppType", e);
            return null;
        }
    }

    public String getEsn() {
        return getEsn(getSubId());
    }

    public String getEsn(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.getEsn(i);
            }
            return null;
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#getEsn", e);
            return null;
        }
    }

    @SystemApi
    public String getCdmaPrlVersion() {
        return getCdmaPrlVersion(getSubId());
    }

    public String getCdmaPrlVersion(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.getCdmaPrlVersion(i);
            }
            return null;
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#getCdmaPrlVersion", e);
            return null;
        }
    }

    @SystemApi
    public List<TelephonyHistogram> getTelephonyHistograms() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.getTelephonyHistograms();
            }
            return null;
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#getTelephonyHistograms", e);
            return null;
        }
    }

    @SystemApi
    public int setAllowedCarriers(int i, List<CarrierIdentifier> list) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.setAllowedCarriers(i, list);
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#setAllowedCarriers", e);
            return -1;
        } catch (NullPointerException e2) {
            Log.e(TAG, "Error calling ITelephony#setAllowedCarriers", e2);
            return -1;
        }
    }

    @SystemApi
    public List<CarrierIdentifier> getAllowedCarriers(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.getAllowedCarriers(i);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#getAllowedCarriers", e);
        } catch (NullPointerException e2) {
            Log.e(TAG, "Error calling ITelephony#getAllowedCarriers", e2);
        }
        return new ArrayList(0);
    }

    public void carrierActionSetMeteredApnsEnabled(int i, boolean z) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                iTelephony.carrierActionSetMeteredApnsEnabled(i, z);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#carrierActionSetMeteredApnsEnabled", e);
        }
    }

    public void carrierActionSetRadioEnabled(int i, boolean z) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                iTelephony.carrierActionSetRadioEnabled(i, z);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#carrierActionSetRadioEnabled", e);
        }
    }

    public void carrierActionReportDefaultNetworkStatus(int i, boolean z) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                iTelephony.carrierActionReportDefaultNetworkStatus(i, z);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#carrierActionReportDefaultNetworkStatus", e);
        }
    }

    public NetworkStats getVtDataUsage(int i) {
        boolean z = i == 1;
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.getVtDataUsage(getSubId(), z);
            }
            return null;
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#getVtDataUsage", e);
            return null;
        }
    }

    public void setPolicyDataEnabled(boolean z, int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                iTelephony.setPolicyDataEnabled(z, i);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#setPolicyDataEnabled", e);
        }
    }

    public List<ClientRequestStats> getClientRequestStats(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.getClientRequestStats(getOpPackageName(), i);
            }
            return null;
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#getClientRequestStats", e);
            return null;
        }
    }

    @SystemApi
    public boolean getEmergencyCallbackMode() {
        return getEmergencyCallbackMode(getSubId());
    }

    public boolean getEmergencyCallbackMode(int i) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony == null) {
                return false;
            }
            return iTelephony.getEmergencyCallbackMode(i);
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#getEmergencyCallbackMode", e);
            return false;
        }
    }

    public SignalStrength getSignalStrength() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.getSignalStrength(getSubId());
            }
            return null;
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#getSignalStrength", e);
            return null;
        }
    }

    public boolean isDataCapable() {
        try {
            int subId = getSubId(SubscriptionManager.getDefaultDataSubscriptionId());
            ITelephony iTelephony = getITelephony();
            if (iTelephony == null) {
                return false;
            }
            return iTelephony.isDataEnabled(subId);
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling ITelephony#isDataEnabled", e);
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public void setRadioIndicationUpdateMode(int i, int i2) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                iTelephony.setRadioIndicationUpdateMode(getSubId(), i, i2);
            }
        } catch (RemoteException e) {
            if (!isSystemProcess()) {
                e.rethrowAsRuntimeException();
            }
        }
    }

    public void setCarrierTestOverride(String str, String str2, String str3, String str4, String str5, String str6, String str7) {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                iTelephony.setCarrierTestOverride(getSubId(), str, str2, str3, str4, str5, str6, str7);
            }
        } catch (RemoteException e) {
        }
    }

    public int getCarrierIdListVersion() {
        try {
            ITelephony iTelephony = getITelephony();
            if (iTelephony != null) {
                return iTelephony.getCarrierIdListVersion(getSubId());
            }
            return -1;
        } catch (RemoteException e) {
            return -1;
        }
    }
}
