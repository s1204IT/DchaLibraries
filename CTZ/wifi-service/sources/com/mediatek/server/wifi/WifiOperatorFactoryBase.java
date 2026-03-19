package com.mediatek.server.wifi;

import android.R;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import com.mediatek.server.wifi.MtkWifiServiceAdapter;
import java.util.List;

public class WifiOperatorFactoryBase {

    public interface IMtkWifiServiceExt {
        public static final String ACTION_RESELECTION_AP = "android.net.wifi.WIFI_RESELECTION_AP";
        public static final String ACTION_SUSPEND_NOTIFICATION = "com.mediatek.wifi.ACTION_SUSPEND_NOTIFICATION";
        public static final String ACTION_WIFI_FAILOVER_GPRS_DIALOG = "com.mediatek.intent.WIFI_FAILOVER_GPRS_DIALOG";
        public static final String AUTOCONNECT_ENABLE_ALL_NETWORKS = "com.mediatek.common.wifi.AUTOCONNECT_ENABLE_ALL_NETWORK";
        public static final String AUTOCONNECT_SETTINGS_CHANGE = "com.mediatek.common.wifi.AUTOCONNECT_SETTINGS_CHANGE";
        public static final int BEST_SIGNAL_THRESHOLD = -79;
        public static final int BSS_EXPIRE_AGE = 10;
        public static final int BSS_EXPIRE_COUNT = 1;
        public static final int DEFAULT_FRAMEWORK_SCAN_INTERVAL_MS = 15000;
        public static final String EXTRA_NOTIFICATION_NETWORKID = "network_id";
        public static final String EXTRA_NOTIFICATION_SSID = "ssid";
        public static final String EXTRA_SHOW_RESELECT_DIALOG_FLAG = "SHOW_RESELECT_DIALOG";
        public static final String EXTRA_SUSPEND_TYPE = "type";
        public static final int MIN_INTERVAL_CHECK_WEAK_SIGNAL_MS = 60000;
        public static final int MIN_INTERVAL_SCAN_SUPRESSION_MS = 10000;
        public static final int MIN_NETWORKS_NUM = 2;
        public static final int NOTIFY_TYPE_RESELECT = 1;
        public static final int NOTIFY_TYPE_SWITCH = 0;
        public static final int OP_01 = 1;
        public static final int OP_03 = 3;
        public static final int OP_NONE = 0;
        public static final String RESELECT_DIALOG_CLASSNAME = "com.mediatek.op01.plugin.WifiReselectApDialog";
        public static final long SUSPEND_NOTIFICATION_DURATION = 3600000;
        public static final int WEAK_SIGNAL_THRESHOLD = -85;
        public static final String WIFISETTINGS_CLASSNAME = "com.android.settings.Settings$WifiSettingsActivity";
        public static final int WIFI_CONNECT_REMINDER_ALWAYS = 0;
        public static final String WIFI_NOTIFICATION_ACTION = "android.net.wifi.WIFI_NOTIFICATION";

        int defaultFrameworkScanIntervalMs();

        String getApDefaultSsid();

        int getSecurity(ScanResult scanResult);

        int getSecurity(WifiConfiguration wifiConfiguration);

        boolean handleNetworkReselection();

        boolean hasConnectableAp();

        boolean hasCustomizedAutoConnect();

        int hasNetworkSelection();

        void init();

        boolean isWifiConnecting(int i, List<Integer> list);

        boolean needRandomSsid();

        void setCustomizedWifiSleepPolicy(Context context);

        boolean shouldAutoConnect();

        void suspendNotification(int i);
    }

    public IMtkWifiServiceExt createWifiFwkExt(Context context, MtkWifiServiceAdapter.IMtkWifiService iMtkWifiService) {
        return new DefaultMtkWifiServiceExt(context, iMtkWifiService);
    }

    public static class DefaultMtkWifiServiceExt implements IMtkWifiServiceExt {
        static final int SECURITY_EAP = 3;
        static final int SECURITY_NONE = 0;
        static final int SECURITY_PSK = 2;
        static final int SECURITY_WAPI_CERT = 5;
        static final int SECURITY_WAPI_PSK = 4;
        static final int SECURITY_WEP = 1;
        static final int SECURITY_WPA2_PSK = 6;
        private static final String TAG = "DefaultMtkWifiServiceExt";
        protected Context mContext;
        protected MtkWifiServiceAdapter.IMtkWifiService mService;

        public DefaultMtkWifiServiceExt(Context context, MtkWifiServiceAdapter.IMtkWifiService iMtkWifiService) {
            this.mContext = context;
            this.mService = iMtkWifiService;
        }

        @Override
        public void init() {
        }

        @Override
        public boolean hasCustomizedAutoConnect() {
            return false;
        }

        @Override
        public boolean shouldAutoConnect() {
            return true;
        }

        @Override
        public boolean isWifiConnecting(int i, List<Integer> list) {
            return false;
        }

        @Override
        public boolean hasConnectableAp() {
            return false;
        }

        @Override
        public void suspendNotification(int i) {
        }

        @Override
        public int defaultFrameworkScanIntervalMs() {
            return this.mContext.getResources().getInteger(R.integer.config_longPressOnHomeBehavior);
        }

        @Override
        public int getSecurity(WifiConfiguration wifiConfiguration) {
            if (wifiConfiguration.allowedKeyManagement.get(1)) {
                return 2;
            }
            if (wifiConfiguration.allowedKeyManagement.get(2) || wifiConfiguration.allowedKeyManagement.get(3)) {
                return 3;
            }
            if (wifiConfiguration.allowedKeyManagement.get(8)) {
                return 4;
            }
            if (wifiConfiguration.allowedKeyManagement.get(9)) {
                return 5;
            }
            if (wifiConfiguration.wepTxKeyIndex >= 0 && wifiConfiguration.wepTxKeyIndex < wifiConfiguration.wepKeys.length && wifiConfiguration.wepKeys[wifiConfiguration.wepTxKeyIndex] != null) {
                return 1;
            }
            if (wifiConfiguration.allowedKeyManagement.get(4)) {
                return 6;
            }
            return 0;
        }

        @Override
        public int getSecurity(ScanResult scanResult) {
            if (scanResult.capabilities.contains("WAPI-PSK")) {
                return 4;
            }
            if (scanResult.capabilities.contains("WAPI-CERT")) {
                return 5;
            }
            if (scanResult.capabilities.contains("WEP")) {
                return 1;
            }
            if (scanResult.capabilities.contains("PSK")) {
                return 2;
            }
            if (scanResult.capabilities.contains("EAP")) {
                return 3;
            }
            if (scanResult.capabilities.contains("WPA2-PSK")) {
                return 6;
            }
            return 0;
        }

        @Override
        public String getApDefaultSsid() {
            return this.mContext.getString(R.string.notification_feedback_indicator_alerted);
        }

        @Override
        public boolean needRandomSsid() {
            return false;
        }

        @Override
        public void setCustomizedWifiSleepPolicy(Context context) {
        }

        @Override
        public boolean handleNetworkReselection() {
            return false;
        }

        @Override
        public int hasNetworkSelection() {
            return 0;
        }

        public boolean isPppoeSupported() {
            return false;
        }

        public void setNotificationVisible(boolean z) {
        }
    }
}
