package com.android.server.wifi;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.connectivity.WifiBatteryStats;
import android.util.Log;
import com.android.internal.app.IBatteryStats;
import com.android.server.wifi.nano.WifiMetricsProto;
import java.io.PrintWriter;

public class WifiPowerMetrics {
    private static final String TAG = "WifiPowerMetrics";
    private final IBatteryStats mBatteryStats = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));

    public WifiMetricsProto.WifiPowerStats buildProto() {
        WifiMetricsProto.WifiPowerStats wifiPowerStats = new WifiMetricsProto.WifiPowerStats();
        WifiBatteryStats stats = getStats();
        if (stats != null) {
            wifiPowerStats.loggingDurationMs = stats.getLoggingDurationMs();
            wifiPowerStats.energyConsumedMah = stats.getEnergyConsumedMaMs() / 3600000.0d;
            wifiPowerStats.idleTimeMs = stats.getIdleTimeMs();
            wifiPowerStats.rxTimeMs = stats.getRxTimeMs();
            wifiPowerStats.txTimeMs = stats.getTxTimeMs();
        }
        return wifiPowerStats;
    }

    public void dump(PrintWriter printWriter) {
        WifiMetricsProto.WifiPowerStats wifiPowerStatsBuildProto = buildProto();
        if (wifiPowerStatsBuildProto != null) {
            printWriter.println("Wifi power metrics:");
            printWriter.println("Logging duration (time on battery): " + wifiPowerStatsBuildProto.loggingDurationMs);
            printWriter.println("Energy consumed by wifi (mAh): " + wifiPowerStatsBuildProto.energyConsumedMah);
            printWriter.println("Amount of time wifi is in idle (ms): " + wifiPowerStatsBuildProto.idleTimeMs);
            printWriter.println("Amount of time wifi is in rx (ms): " + wifiPowerStatsBuildProto.rxTimeMs);
            printWriter.println("Amount of time wifi is in tx (ms): " + wifiPowerStatsBuildProto.txTimeMs);
        }
    }

    private WifiBatteryStats getStats() {
        try {
            return this.mBatteryStats.getWifiBatteryStats();
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to obtain Wifi power stats from BatteryStats");
            return null;
        }
    }
}
