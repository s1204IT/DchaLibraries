package com.android.server.wifi;

import android.R;
import android.content.Context;
import android.os.Looper;

public class OpenNetworkNotifier extends AvailableNetworkNotifier {
    private static final String STORE_DATA_IDENTIFIER = "OpenNetworkNotifierBlacklist";
    public static final String TAG = "WifiOpenNetworkNotifier";
    private static final String TOGGLE_SETTINGS_NAME = "wifi_networks_available_notification_on";

    public OpenNetworkNotifier(Context context, Looper looper, FrameworkFacade frameworkFacade, Clock clock, WifiMetrics wifiMetrics, WifiConfigManager wifiConfigManager, WifiConfigStore wifiConfigStore, WifiStateMachine wifiStateMachine, ConnectToNetworkNotificationBuilder connectToNetworkNotificationBuilder) {
        super(TAG, STORE_DATA_IDENTIFIER, TOGGLE_SETTINGS_NAME, R.drawable.notification_progress_indeterminate_horizontal_material, context, looper, frameworkFacade, clock, wifiMetrics, wifiConfigManager, wifiConfigStore, wifiStateMachine, connectToNetworkNotificationBuilder);
    }
}
