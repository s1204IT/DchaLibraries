package com.android.internal.os;

import android.os.BatteryStats;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.telephony.PhoneConstants;

public class CpuPowerCalculator extends PowerCalculator {
    private static final boolean DEBUG = false;
    private static final long MICROSEC_IN_HR = 3600000000L;
    private static final String TAG = "CpuPowerCalculator";
    private final PowerProfile mProfile;

    public CpuPowerCalculator(PowerProfile powerProfile) {
        this.mProfile = powerProfile;
    }

    @Override
    public void calculateApp(BatterySipper batterySipper, BatteryStats.Uid uid, long j, long j2, int i) {
        double d;
        batterySipper.cpuTimeMs = (uid.getUserCpuTimeUs(i) + uid.getSystemCpuTimeUs(i)) / 1000;
        int numCpuClusters = this.mProfile.getNumCpuClusters();
        int i2 = 0;
        double d2 = 0.0d;
        while (i2 < numCpuClusters) {
            int numSpeedStepsInCpuCluster = this.mProfile.getNumSpeedStepsInCpuCluster(i2);
            double timeAtCpuSpeed = d2;
            for (int i3 = 0; i3 < numSpeedStepsInCpuCluster; i3++) {
                timeAtCpuSpeed += uid.getTimeAtCpuSpeed(i2, i3, i) * this.mProfile.getAveragePowerForCpuCore(i2, i3);
            }
            i2++;
            d2 = timeAtCpuSpeed;
        }
        double cpuActiveTime = d2 + (uid.getCpuActiveTime() * 1000 * this.mProfile.getAveragePower(PowerProfile.POWER_CPU_ACTIVE));
        long[] cpuClusterTimes = uid.getCpuClusterTimes();
        if (cpuClusterTimes != null) {
            if (cpuClusterTimes.length == numCpuClusters) {
                for (int i4 = 0; i4 < numCpuClusters; i4++) {
                    cpuActiveTime += cpuClusterTimes[i4] * 1000 * this.mProfile.getAveragePowerForCpuCluster(i4);
                }
            } else {
                Log.w(TAG, "UID " + uid.getUid() + " CPU cluster # mismatch: Power Profile # " + numCpuClusters + " actual # " + cpuClusterTimes.length);
            }
        }
        batterySipper.cpuPowerMah = cpuActiveTime / 3.6E9d;
        batterySipper.cpuFgTimeMs = 0L;
        ArrayMap<String, ? extends BatteryStats.Uid.Proc> processStats = uid.getProcessStats();
        int size = processStats.size();
        double d3 = 0.0d;
        for (int i5 = 0; i5 < size; i5++) {
            BatteryStats.Uid.Proc procValueAt = processStats.valueAt(i5);
            String strKeyAt = processStats.keyAt(i5);
            batterySipper.cpuFgTimeMs += procValueAt.getForegroundTime(i);
            long userTime = procValueAt.getUserTime(i) + procValueAt.getSystemTime(i) + procValueAt.getForegroundTime(i);
            if (batterySipper.packageWithHighestDrain == null || batterySipper.packageWithHighestDrain.startsWith(PhoneConstants.APN_TYPE_ALL)) {
                d = userTime;
                batterySipper.packageWithHighestDrain = strKeyAt;
            } else {
                d = userTime;
                if (d3 < d && !strKeyAt.startsWith(PhoneConstants.APN_TYPE_ALL)) {
                    batterySipper.packageWithHighestDrain = strKeyAt;
                }
            }
            d3 = d;
        }
        if (batterySipper.cpuFgTimeMs > batterySipper.cpuTimeMs) {
            batterySipper.cpuTimeMs = batterySipper.cpuFgTimeMs;
        }
    }
}
