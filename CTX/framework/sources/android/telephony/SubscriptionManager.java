package android.telephony;

import android.annotation.SystemApi;
import android.app.BroadcastOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.INetworkPolicyManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import com.android.internal.telephony.IOnSubscriptionsChangedListener;
import com.android.internal.telephony.ISub;
import com.android.internal.telephony.ITelephonyRegistry;
import com.android.internal.telephony.PhoneConstants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SubscriptionManager {
    public static final String ACCESS_RULES = "access_rules";
    public static final String ACTION_DEFAULT_SMS_SUBSCRIPTION_CHANGED = "android.telephony.action.DEFAULT_SMS_SUBSCRIPTION_CHANGED";
    public static final String ACTION_DEFAULT_SUBSCRIPTION_CHANGED = "android.telephony.action.DEFAULT_SUBSCRIPTION_CHANGED";

    @SystemApi
    public static final String ACTION_MANAGE_SUBSCRIPTION_PLANS = "android.telephony.action.MANAGE_SUBSCRIPTION_PLANS";

    @SystemApi
    public static final String ACTION_REFRESH_SUBSCRIPTION_PLANS = "android.telephony.action.REFRESH_SUBSCRIPTION_PLANS";
    public static final String ACTION_SUBSCRIPTION_PLANS_CHANGED = "android.telephony.action.SUBSCRIPTION_PLANS_CHANGED";
    public static final String CARD_ID = "card_id";
    public static final String CARRIER_NAME = "carrier_name";
    public static final String CB_ALERT_REMINDER_INTERVAL = "alert_reminder_interval";
    public static final String CB_ALERT_SOUND_DURATION = "alert_sound_duration";
    public static final String CB_ALERT_SPEECH = "enable_alert_speech";
    public static final String CB_ALERT_VIBRATE = "enable_alert_vibrate";
    public static final String CB_AMBER_ALERT = "enable_cmas_amber_alerts";
    public static final String CB_CHANNEL_50_ALERT = "enable_channel_50_alerts";
    public static final String CB_CMAS_TEST_ALERT = "enable_cmas_test_alerts";
    public static final String CB_EMERGENCY_ALERT = "enable_emergency_alerts";
    public static final String CB_ETWS_TEST_ALERT = "enable_etws_test_alerts";
    public static final String CB_EXTREME_THREAT_ALERT = "enable_cmas_extreme_threat_alerts";
    public static final String CB_OPT_OUT_DIALOG = "show_cmas_opt_out_dialog";
    public static final String CB_SEVERE_THREAT_ALERT = "enable_cmas_severe_threat_alerts";
    public static final String COLOR = "color";
    public static final int COLOR_1 = 0;
    public static final int COLOR_2 = 1;
    public static final int COLOR_3 = 2;
    public static final int COLOR_4 = 3;
    public static final int COLOR_DEFAULT = 0;
    public static final Uri CONTENT_URI;
    public static final String DATA_ROAMING = "data_roaming";
    public static final int DATA_ROAMING_DEFAULT = 0;
    public static final int DATA_ROAMING_DISABLE = 0;
    public static final int DATA_ROAMING_ENABLE = 1;
    private static final boolean DBG = false;
    public static final int DEFAULT_NAME_RES = 17039374;
    public static final int DEFAULT_PHONE_INDEX = Integer.MAX_VALUE;
    public static final int DEFAULT_SIM_SLOT_INDEX = Integer.MAX_VALUE;
    public static final int DEFAULT_SUBSCRIPTION_ID = Integer.MAX_VALUE;
    public static final String DISPLAY_NAME = "display_name";
    public static final int DISPLAY_NUMBER_DEFAULT = 1;
    public static final int DISPLAY_NUMBER_FIRST = 1;
    public static final String DISPLAY_NUMBER_FORMAT = "display_number_format";
    public static final int DISPLAY_NUMBER_LAST = 2;
    public static final int DISPLAY_NUMBER_NONE = 0;
    public static final int DUMMY_SUBSCRIPTION_ID_BASE = -2;
    public static final String ENHANCED_4G_MODE_ENABLED = "volte_vt_enabled";
    public static final String EXTRA_SUBSCRIPTION_INDEX = "android.telephony.extra.SUBSCRIPTION_INDEX";
    public static final String ICC_ID = "icc_id";
    public static final int INVALID_PHONE_INDEX = -1;
    public static final int INVALID_SIM_SLOT_INDEX = -1;
    public static final int INVALID_SUBSCRIPTION_ID = -1;
    private static final boolean IS_DEBUG_BUILD;
    public static final String IS_EMBEDDED = "is_embedded";
    public static final String IS_REMOVABLE = "is_removable";
    private static final String LOG_TAG = "SubscriptionManager";
    public static final int MAX_SUBSCRIPTION_ID_VALUE = 2147483646;
    public static final String MCC = "mcc";
    public static final int MIN_SUBSCRIPTION_ID_VALUE = 0;
    public static final String MNC = "mnc";
    public static final String NAME_SOURCE = "name_source";
    public static final int NAME_SOURCE_DEFAULT_SOURCE = 0;
    public static final int NAME_SOURCE_SIM_SOURCE = 1;
    public static final int NAME_SOURCE_UNDEFINDED = -1;
    public static final int NAME_SOURCE_USER_INPUT = 2;
    public static final String NUMBER = "number";
    public static final int SIM_NOT_INSERTED = -1;
    public static final int SIM_PROVISIONED = 0;
    public static final String SIM_PROVISIONING_STATUS = "sim_provisioning_status";
    public static final String SIM_SLOT_INDEX = "sim_id";
    public static final String SUB_DEFAULT_CHANGED_ACTION = "android.intent.action.SUB_DEFAULT_CHANGED";
    public static final String UNIQUE_KEY_SUBSCRIPTION_ID = "_id";
    private static final boolean VDBG = false;
    public static final String VT_IMS_ENABLED = "vt_ims_enabled";
    public static final String WFC_IMS_ENABLED = "wfc_ims_enabled";
    public static final String WFC_IMS_MODE = "wfc_ims_mode";
    public static final String WFC_IMS_ROAMING_ENABLED = "wfc_ims_roaming_enabled";
    public static final String WFC_IMS_ROAMING_MODE = "wfc_ims_roaming_mode";
    private final Context mContext;
    private volatile INetworkPolicyManager mNetworkPolicy;

    static {
        IS_DEBUG_BUILD = Build.TYPE.equals("eng") || Build.TYPE.equals("userdebug");
        CONTENT_URI = Uri.parse("content://telephony/siminfo");
    }

    public static class OnSubscriptionsChangedListener {
        IOnSubscriptionsChangedListener callback;
        private final Handler mHandler;

        private class OnSubscriptionsChangedListenerHandler extends Handler {
            OnSubscriptionsChangedListenerHandler() {
            }

            OnSubscriptionsChangedListenerHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message message) {
                long jUptimeMillis;
                if (SubscriptionManager.IS_DEBUG_BUILD) {
                    jUptimeMillis = SystemClock.uptimeMillis();
                } else {
                    jUptimeMillis = 0;
                }
                OnSubscriptionsChangedListener.this.onSubscriptionsChanged();
                if (SubscriptionManager.IS_DEBUG_BUILD) {
                    long jUptimeMillis2 = SystemClock.uptimeMillis() - jUptimeMillis;
                    if (jUptimeMillis2 > 200) {
                        OnSubscriptionsChangedListener.this.log("Cost " + jUptimeMillis2 + "ms in OnSubscriptionsChangedListener.this=" + OnSubscriptionsChangedListener.this + " this.mHandler=" + OnSubscriptionsChangedListener.this.mHandler);
                    }
                }
            }
        }

        public OnSubscriptionsChangedListener() {
            this.callback = new IOnSubscriptionsChangedListener.Stub() {
                @Override
                public void onSubscriptionsChanged() {
                    OnSubscriptionsChangedListener.this.mHandler.sendEmptyMessage(0);
                }
            };
            this.mHandler = new OnSubscriptionsChangedListenerHandler();
        }

        public OnSubscriptionsChangedListener(Looper looper) {
            this.callback = new IOnSubscriptionsChangedListener.Stub() {
                @Override
                public void onSubscriptionsChanged() {
                    OnSubscriptionsChangedListener.this.mHandler.sendEmptyMessage(0);
                }
            };
            this.mHandler = new OnSubscriptionsChangedListenerHandler(looper);
        }

        public void onSubscriptionsChanged() {
        }

        private void log(String str) {
            Rlog.d(SubscriptionManager.LOG_TAG, str);
        }
    }

    public SubscriptionManager(Context context) {
        this.mContext = context;
    }

    @Deprecated
    public static SubscriptionManager from(Context context) {
        return (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
    }

    private final INetworkPolicyManager getNetworkPolicy() {
        if (this.mNetworkPolicy == null) {
            this.mNetworkPolicy = INetworkPolicyManager.Stub.asInterface(ServiceManager.getService(Context.NETWORK_POLICY_SERVICE));
        }
        return this.mNetworkPolicy;
    }

    public void addOnSubscriptionsChangedListener(OnSubscriptionsChangedListener onSubscriptionsChangedListener) {
        String opPackageName = this.mContext != null ? this.mContext.getOpPackageName() : MediaStore.UNKNOWN_STRING;
        if (IS_DEBUG_BUILD) {
            logd("register OnSubscriptionsChangedListener pkgName=" + opPackageName + " listener=" + onSubscriptionsChangedListener + " listener.mHandler=" + onSubscriptionsChangedListener.mHandler);
        }
        try {
            ITelephonyRegistry iTelephonyRegistryAsInterface = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry"));
            if (iTelephonyRegistryAsInterface != null) {
                iTelephonyRegistryAsInterface.addOnSubscriptionsChangedListener(opPackageName, onSubscriptionsChangedListener.callback);
            }
        } catch (RemoteException e) {
        }
    }

    public void removeOnSubscriptionsChangedListener(OnSubscriptionsChangedListener onSubscriptionsChangedListener) {
        String opPackageName = this.mContext != null ? this.mContext.getOpPackageName() : MediaStore.UNKNOWN_STRING;
        try {
            ITelephonyRegistry iTelephonyRegistryAsInterface = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry"));
            if (iTelephonyRegistryAsInterface != null) {
                iTelephonyRegistryAsInterface.removeOnSubscriptionsChangedListener(opPackageName, onSubscriptionsChangedListener.callback);
            }
        } catch (RemoteException e) {
        }
    }

    public SubscriptionInfo getActiveSubscriptionInfo(int i) {
        if (!isValidSubscriptionId(i)) {
            return null;
        }
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface != null) {
                return iSubAsInterface.getActiveSubscriptionInfo(i, this.mContext.getOpPackageName());
            }
            return null;
        } catch (RemoteException e) {
            return null;
        }
    }

    public SubscriptionInfo getActiveSubscriptionInfoForIccIndex(String str) {
        if (str == null) {
            logd("[getActiveSubscriptionInfoForIccIndex]- null iccid");
            return null;
        }
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface == null) {
                return null;
            }
            return iSubAsInterface.getActiveSubscriptionInfoForIccId(str, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            return null;
        }
    }

    public SubscriptionInfo getActiveSubscriptionInfoForSimSlotIndex(int i) {
        if (!isValidSlotIndex(i)) {
            logd("[getActiveSubscriptionInfoForSimSlotIndex]- invalid slotIndex");
            return null;
        }
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface != null) {
                return iSubAsInterface.getActiveSubscriptionInfoForSimSlotIndex(i, this.mContext.getOpPackageName());
            }
            return null;
        } catch (RemoteException e) {
            return null;
        }
    }

    public List<SubscriptionInfo> getAllSubscriptionInfoList() throws RemoteException {
        List<SubscriptionInfo> allSubInfoList = null;
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface != null) {
                allSubInfoList = iSubAsInterface.getAllSubInfoList(this.mContext.getOpPackageName());
            }
        } catch (RemoteException e) {
        }
        if (allSubInfoList == null) {
            return new ArrayList();
        }
        return allSubInfoList;
    }

    public List<SubscriptionInfo> getActiveSubscriptionInfoList() {
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface == null) {
                return null;
            }
            return iSubAsInterface.getActiveSubscriptionInfoList(this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            return null;
        }
    }

    @SystemApi
    public List<SubscriptionInfo> getAvailableSubscriptionInfoList() {
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface == null) {
                return null;
            }
            return iSubAsInterface.getAvailableSubscriptionInfoList(this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            return null;
        }
    }

    public List<SubscriptionInfo> getAccessibleSubscriptionInfoList() {
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface == null) {
                return null;
            }
            return iSubAsInterface.getAccessibleSubscriptionInfoList(this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            return null;
        }
    }

    @SystemApi
    public void requestEmbeddedSubscriptionInfoListRefresh() {
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface != null) {
                iSubAsInterface.requestEmbeddedSubscriptionInfoListRefresh();
            }
        } catch (RemoteException e) {
        }
    }

    public int getAllSubscriptionInfoCount() {
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface == null) {
                return 0;
            }
            return iSubAsInterface.getAllSubInfoCount(this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            return 0;
        }
    }

    public int getActiveSubscriptionInfoCount() {
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface == null) {
                return 0;
            }
            return iSubAsInterface.getActiveSubInfoCount(this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            return 0;
        }
    }

    public int getActiveSubscriptionInfoCountMax() {
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface == null) {
                return 0;
            }
            return iSubAsInterface.getActiveSubInfoCountMax();
        } catch (RemoteException e) {
            return 0;
        }
    }

    public Uri addSubscriptionInfoRecord(String str, int i) {
        if (str == null) {
            logd("[addSubscriptionInfoRecord]- null iccId");
        }
        if (!isValidSlotIndex(i)) {
            logd("[addSubscriptionInfoRecord]- invalid slotIndex");
        }
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface != null) {
                iSubAsInterface.addSubInfoRecord(str, i);
                return null;
            }
            logd("[addSubscriptionInfoRecord]- ISub service is null");
            return null;
        } catch (RemoteException e) {
            return null;
        }
    }

    public int setIconTint(int i, int i2) {
        if (!isValidSubscriptionId(i2)) {
            logd("[setIconTint]- fail");
            return -1;
        }
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface == null) {
                return 0;
            }
            return iSubAsInterface.setIconTint(i, i2);
        } catch (RemoteException e) {
            return 0;
        }
    }

    public int setDisplayName(String str, int i) {
        return setDisplayName(str, i, -1L);
    }

    public int setDisplayName(String str, int i, long j) {
        if (!isValidSubscriptionId(i)) {
            logd("[setDisplayName]- fail");
            return -1;
        }
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface == null) {
                return 0;
            }
            return iSubAsInterface.setDisplayNameUsingSrc(str, i, j);
        } catch (RemoteException e) {
            return 0;
        }
    }

    public int setDisplayNumber(String str, int i) {
        if (str == null || !isValidSubscriptionId(i)) {
            logd("[setDisplayNumber]- fail");
            return -1;
        }
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface == null) {
                return 0;
            }
            return iSubAsInterface.setDisplayNumber(str, i);
        } catch (RemoteException e) {
            return 0;
        }
    }

    public int setDataRoaming(int i, int i2) {
        if (i < 0 || !isValidSubscriptionId(i2)) {
            logd("[setDataRoaming]- fail");
            return -1;
        }
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface == null) {
                return 0;
            }
            return iSubAsInterface.setDataRoaming(i, i2);
        } catch (RemoteException e) {
            return 0;
        }
    }

    public static int getSlotIndex(int i) {
        isValidSubscriptionId(i);
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface == null) {
                return -1;
            }
            return iSubAsInterface.getSlotIndex(i);
        } catch (RemoteException e) {
            return -1;
        }
    }

    public static int[] getSubId(int i) {
        if (!isValidSlotIndex(i)) {
            logd("[getSubId]- fail");
            return null;
        }
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface != null) {
                return iSubAsInterface.getSubId(i);
            }
            return null;
        } catch (RemoteException e) {
            return null;
        }
    }

    public static int getPhoneId(int i) {
        if (!isValidSubscriptionId(i)) {
            if (i > (-2) - TelephonyManager.getDefault().getSimCount()) {
                return (-2) - i;
            }
            return -1;
        }
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface != null) {
                return iSubAsInterface.getPhoneId(i);
            }
            return -1;
        } catch (RemoteException e) {
            return -1;
        }
    }

    private static void logd(String str) {
        Rlog.d(LOG_TAG, str);
    }

    public static int getDefaultSubscriptionId() {
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface == null) {
                return -1;
            }
            return iSubAsInterface.getDefaultSubId();
        } catch (RemoteException e) {
            return -1;
        }
    }

    public static int getDefaultVoiceSubscriptionId() {
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface == null) {
                return -1;
            }
            return iSubAsInterface.getDefaultVoiceSubId();
        } catch (RemoteException e) {
            return -1;
        }
    }

    public void setDefaultVoiceSubId(int i) {
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface != null) {
                iSubAsInterface.setDefaultVoiceSubId(i);
            }
        } catch (RemoteException e) {
        }
    }

    public SubscriptionInfo getDefaultVoiceSubscriptionInfo() {
        return getActiveSubscriptionInfo(getDefaultVoiceSubscriptionId());
    }

    public static int getDefaultVoicePhoneId() {
        return getPhoneId(getDefaultVoiceSubscriptionId());
    }

    public static int getDefaultSmsSubscriptionId() {
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface == null) {
                return -1;
            }
            return iSubAsInterface.getDefaultSmsSubId();
        } catch (RemoteException e) {
            return -1;
        }
    }

    public void setDefaultSmsSubId(int i) {
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface != null) {
                iSubAsInterface.setDefaultSmsSubId(i);
            }
        } catch (RemoteException e) {
        }
    }

    public SubscriptionInfo getDefaultSmsSubscriptionInfo() {
        return getActiveSubscriptionInfo(getDefaultSmsSubscriptionId());
    }

    public int getDefaultSmsPhoneId() {
        return getPhoneId(getDefaultSmsSubscriptionId());
    }

    public static int getDefaultDataSubscriptionId() {
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface == null) {
                return -1;
            }
            return iSubAsInterface.getDefaultDataSubId();
        } catch (RemoteException e) {
            return -1;
        }
    }

    public void setDefaultDataSubId(int i) {
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface != null) {
                iSubAsInterface.setDefaultDataSubId(i);
            }
        } catch (RemoteException e) {
        }
    }

    public SubscriptionInfo getDefaultDataSubscriptionInfo() {
        return getActiveSubscriptionInfo(getDefaultDataSubscriptionId());
    }

    public int getDefaultDataPhoneId() {
        return getPhoneId(getDefaultDataSubscriptionId());
    }

    public void clearSubscriptionInfo() {
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface != null) {
                iSubAsInterface.clearSubInfo();
            }
        } catch (RemoteException e) {
        }
    }

    public boolean allDefaultsSelected() {
        return isValidSubscriptionId(getDefaultDataSubscriptionId()) && isValidSubscriptionId(getDefaultSmsSubscriptionId()) && isValidSubscriptionId(getDefaultVoiceSubscriptionId());
    }

    public void clearDefaultsForInactiveSubIds() {
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface != null) {
                iSubAsInterface.clearDefaultsForInactiveSubIds();
            }
        } catch (RemoteException e) {
        }
    }

    public static boolean isValidSubscriptionId(int i) {
        return i > -1;
    }

    public static boolean isUsableSubIdValue(int i) {
        return i >= 0 && i <= 2147483646;
    }

    public static boolean isValidSlotIndex(int i) {
        return i >= 0 && i < TelephonyManager.getDefault().getSimCount();
    }

    public static boolean isValidPhoneId(int i) {
        return i >= 0 && i < TelephonyManager.getDefault().getPhoneCount();
    }

    public static void putPhoneIdAndSubIdExtra(Intent intent, int i) {
        int[] subId = getSubId(i);
        if (subId != null && subId.length > 0) {
            putPhoneIdAndSubIdExtra(intent, i, subId[0]);
        } else {
            logd("putPhoneIdAndSubIdExtra: no valid subs");
        }
    }

    public static void putPhoneIdAndSubIdExtra(Intent intent, int i, int i2) {
        intent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, i2);
        intent.putExtra("android.telephony.extra.SUBSCRIPTION_INDEX", i2);
        intent.putExtra("phone", i);
        intent.putExtra(PhoneConstants.SLOT_KEY, i);
    }

    public int[] getActiveSubscriptionIdList() throws RemoteException {
        int[] activeSubIdList = null;
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface != null) {
                activeSubIdList = iSubAsInterface.getActiveSubIdList();
            }
        } catch (RemoteException e) {
        }
        if (activeSubIdList == null) {
            return new int[0];
        }
        return activeSubIdList;
    }

    public boolean isNetworkRoaming(int i) {
        if (getPhoneId(i) < 0) {
            return false;
        }
        return TelephonyManager.getDefault().isNetworkRoaming(i);
    }

    public static int getSimStateForSlotIndex(int i) {
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface == null) {
                return 0;
            }
            return iSubAsInterface.getSimStateForSlotIndex(i);
        } catch (RemoteException e) {
            return 0;
        }
    }

    public static void setSubscriptionProperty(int i, String str, String str2) {
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface != null) {
                iSubAsInterface.setSubscriptionProperty(i, str, str2);
            }
        } catch (RemoteException e) {
        }
    }

    private static String getSubscriptionProperty(int i, String str, Context context) {
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface == null) {
                return null;
            }
            return iSubAsInterface.getSubscriptionProperty(i, str, context.getOpPackageName());
        } catch (RemoteException e) {
            return null;
        }
    }

    public static boolean getBooleanSubscriptionProperty(int i, String str, boolean z, Context context) {
        String subscriptionProperty = getSubscriptionProperty(i, str, context);
        if (subscriptionProperty != null) {
            try {
                return Integer.parseInt(subscriptionProperty) == 1;
            } catch (NumberFormatException e) {
                logd("getBooleanSubscriptionProperty NumberFormat exception");
            }
        }
        return z;
    }

    public static int getIntegerSubscriptionProperty(int i, String str, int i2, Context context) {
        String subscriptionProperty = getSubscriptionProperty(i, str, context);
        if (subscriptionProperty != null) {
            try {
                return Integer.parseInt(subscriptionProperty);
            } catch (NumberFormatException e) {
                logd("getBooleanSubscriptionProperty NumberFormat exception");
            }
        }
        return i2;
    }

    public static Resources getResourcesForSubId(Context context, int i) {
        SubscriptionInfo activeSubscriptionInfo = from(context).getActiveSubscriptionInfo(i);
        Configuration configuration = context.getResources().getConfiguration();
        Configuration configuration2 = new Configuration();
        configuration2.setTo(configuration);
        if (activeSubscriptionInfo != null) {
            configuration2.mcc = activeSubscriptionInfo.getMcc();
            configuration2.mnc = activeSubscriptionInfo.getMnc();
            if (configuration2.mnc == 0) {
                configuration2.mnc = 65535;
            }
        }
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        DisplayMetrics displayMetrics2 = new DisplayMetrics();
        displayMetrics2.setTo(displayMetrics);
        return new Resources(context.getResources().getAssets(), displayMetrics2, configuration2);
    }

    public boolean isActiveSubId(int i) {
        try {
            ISub iSubAsInterface = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSubAsInterface != null) {
                return iSubAsInterface.isActiveSubId(i);
            }
            return false;
        } catch (RemoteException e) {
            return false;
        }
    }

    @SystemApi
    public List<SubscriptionPlan> getSubscriptionPlans(int i) {
        try {
            SubscriptionPlan[] subscriptionPlans = getNetworkPolicy().getSubscriptionPlans(i, this.mContext.getOpPackageName());
            return subscriptionPlans == null ? Collections.emptyList() : Arrays.asList(subscriptionPlans);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void setSubscriptionPlans(int i, List<SubscriptionPlan> list) {
        try {
            getNetworkPolicy().setSubscriptionPlans(i, (SubscriptionPlan[]) list.toArray(new SubscriptionPlan[list.size()]), this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private String getSubscriptionPlansOwner(int i) {
        try {
            return getNetworkPolicy().getSubscriptionPlansOwner(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void setSubscriptionOverrideUnmetered(int i, boolean z, long j) {
        try {
            getNetworkPolicy().setSubscriptionOverride(i, 1, z ? 1 : 0, j, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void setSubscriptionOverrideCongested(int i, boolean z, long j) {
        try {
            getNetworkPolicy().setSubscriptionOverride(i, 2, z ? 2 : 0, j, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Intent createManageSubscriptionIntent(int i) {
        String subscriptionPlansOwner = getSubscriptionPlansOwner(i);
        if (subscriptionPlansOwner == null || getSubscriptionPlans(i).isEmpty()) {
            return null;
        }
        Intent intent = new Intent(ACTION_MANAGE_SUBSCRIPTION_PLANS);
        intent.setPackage(subscriptionPlansOwner);
        intent.putExtra("android.telephony.extra.SUBSCRIPTION_INDEX", i);
        if (this.mContext.getPackageManager().queryIntentActivities(intent, 65536).isEmpty()) {
            return null;
        }
        return intent;
    }

    private Intent createRefreshSubscriptionIntent(int i) {
        String subscriptionPlansOwner = getSubscriptionPlansOwner(i);
        if (subscriptionPlansOwner == null || getSubscriptionPlans(i).isEmpty()) {
            return null;
        }
        Intent intent = new Intent(ACTION_REFRESH_SUBSCRIPTION_PLANS);
        intent.addFlags(268435456);
        intent.setPackage(subscriptionPlansOwner);
        intent.putExtra("android.telephony.extra.SUBSCRIPTION_INDEX", i);
        if (this.mContext.getPackageManager().queryBroadcastReceivers(intent, 0).isEmpty()) {
            return null;
        }
        return intent;
    }

    public boolean isSubscriptionPlansRefreshSupported(int i) {
        return createRefreshSubscriptionIntent(i) != null;
    }

    public void requestSubscriptionPlansRefresh(int i) {
        Intent intentCreateRefreshSubscriptionIntent = createRefreshSubscriptionIntent(i);
        BroadcastOptions broadcastOptionsMakeBasic = BroadcastOptions.makeBasic();
        broadcastOptionsMakeBasic.setTemporaryAppWhitelistDuration(TimeUnit.MINUTES.toMillis(1L));
        this.mContext.sendBroadcast(intentCreateRefreshSubscriptionIntent, (String) null, broadcastOptionsMakeBasic.toBundle());
    }

    public boolean canManageSubscription(SubscriptionInfo subscriptionInfo) {
        return canManageSubscription(subscriptionInfo, this.mContext.getPackageName());
    }

    public boolean canManageSubscription(SubscriptionInfo subscriptionInfo, String str) {
        if (!subscriptionInfo.isEmbedded()) {
            throw new IllegalArgumentException("Not an embedded subscription");
        }
        if (subscriptionInfo.getAccessRules() == null) {
            return false;
        }
        try {
            PackageInfo packageInfo = this.mContext.getPackageManager().getPackageInfo(str, 64);
            Iterator<UiccAccessRule> it = subscriptionInfo.getAccessRules().iterator();
            while (it.hasNext()) {
                if (it.next().getCarrierPrivilegeStatus(packageInfo) == 1) {
                    return true;
                }
            }
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalArgumentException("Unknown package: " + str, e);
        }
    }
}
