package com.android.server.wifi;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Looper;
import com.android.server.wifi.util.ScanResultUtil;

public class CarrierNetworkNotifier extends AvailableNetworkNotifier {
    private static final String STORE_DATA_IDENTIFIER = "CarrierNetworkNotifierBlacklist";
    public static final String TAG = "WifiCarrierNetworkNotifier";
    private static final String TOGGLE_SETTINGS_NAME = "wifi_carrier_networks_available_notification_on";

    public CarrierNetworkNotifier(Context context, Looper looper, FrameworkFacade frameworkFacade, Clock clock, WifiMetrics wifiMetrics, WifiConfigManager wifiConfigManager, WifiConfigStore wifiConfigStore, WifiStateMachine wifiStateMachine, ConnectToNetworkNotificationBuilder connectToNetworkNotificationBuilder) {
        super(TAG, STORE_DATA_IDENTIFIER, TOGGLE_SETTINGS_NAME, 46, context, looper, frameworkFacade, clock, wifiMetrics, wifiConfigManager, wifiConfigStore, wifiStateMachine, connectToNetworkNotificationBuilder);
    }

    @Override
    WifiConfiguration createRecommendedNetworkConfig(ScanResult scanResult) {
        WifiConfiguration wifiConfigurationCreateNetworkFromScanResult = ScanResultUtil.createNetworkFromScanResult(scanResult);
        int i = scanResult.carrierApEapType;
        if (i == 4 || i == 5 || i == 6) {
            wifiConfigurationCreateNetworkFromScanResult.allowedKeyManagement.set(2);
            wifiConfigurationCreateNetworkFromScanResult.allowedKeyManagement.set(3);
            wifiConfigurationCreateNetworkFromScanResult.enterpriseConfig = new WifiEnterpriseConfig();
            wifiConfigurationCreateNetworkFromScanResult.enterpriseConfig.setEapMethod(scanResult.carrierApEapType);
            wifiConfigurationCreateNetworkFromScanResult.enterpriseConfig.setIdentity("");
            wifiConfigurationCreateNetworkFromScanResult.enterpriseConfig.setAnonymousIdentity("");
        }
        return wifiConfigurationCreateNetworkFromScanResult;
    }
}
