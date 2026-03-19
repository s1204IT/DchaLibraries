package com.android.internal.telephony.metrics;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.connectivity.CellularBatteryStats;
import com.android.internal.app.IBatteryStats;
import com.android.internal.telephony.nano.TelephonyProto;

public class ModemPowerMetrics {
    private final IBatteryStats mBatteryStats = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));

    public TelephonyProto.ModemPowerStats buildProto() {
        TelephonyProto.ModemPowerStats modemPowerStats = new TelephonyProto.ModemPowerStats();
        CellularBatteryStats stats = getStats();
        if (stats != null) {
            modemPowerStats.loggingDurationMs = stats.getLoggingDurationMs();
            modemPowerStats.energyConsumedMah = stats.getEnergyConsumedMaMs() / 3600000.0d;
            modemPowerStats.numPacketsTx = stats.getNumPacketsTx();
            modemPowerStats.cellularKernelActiveTimeMs = stats.getKernelActiveTimeMs();
            if (stats.getTimeInRxSignalStrengthLevelMs() != null && stats.getTimeInRxSignalStrengthLevelMs().length > 0) {
                modemPowerStats.timeInVeryPoorRxSignalLevelMs = stats.getTimeInRxSignalStrengthLevelMs()[0];
            }
            modemPowerStats.sleepTimeMs = stats.getSleepTimeMs();
            modemPowerStats.idleTimeMs = stats.getIdleTimeMs();
            modemPowerStats.rxTimeMs = stats.getRxTimeMs();
            long[] txTimeMs = stats.getTxTimeMs();
            modemPowerStats.txTimeMs = new long[txTimeMs.length];
            for (int i = 0; i < txTimeMs.length; i++) {
                modemPowerStats.txTimeMs[i] = txTimeMs[i];
            }
        }
        return modemPowerStats;
    }

    private CellularBatteryStats getStats() {
        try {
            return this.mBatteryStats.getCellularBatteryStats();
        } catch (RemoteException e) {
            return null;
        }
    }
}
