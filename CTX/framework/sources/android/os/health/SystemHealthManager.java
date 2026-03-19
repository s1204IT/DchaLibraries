package android.os.health;

import android.content.Context;
import android.os.BatteryStats;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.internal.app.IBatteryStats;

public class SystemHealthManager {
    private final IBatteryStats mBatteryStats;

    public SystemHealthManager() {
        this(IBatteryStats.Stub.asInterface(ServiceManager.getService(BatteryStats.SERVICE_NAME)));
    }

    public SystemHealthManager(IBatteryStats iBatteryStats) {
        this.mBatteryStats = iBatteryStats;
    }

    public static SystemHealthManager from(Context context) {
        return (SystemHealthManager) context.getSystemService(Context.SYSTEM_HEALTH_SERVICE);
    }

    public HealthStats takeUidSnapshot(int i) {
        try {
            return this.mBatteryStats.takeUidSnapshot(i).getHealthStats();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public HealthStats takeMyUidSnapshot() {
        return takeUidSnapshot(Process.myUid());
    }

    public HealthStats[] takeUidSnapshots(int[] iArr) {
        try {
            HealthStatsParceler[] healthStatsParcelerArrTakeUidSnapshots = this.mBatteryStats.takeUidSnapshots(iArr);
            HealthStats[] healthStatsArr = new HealthStats[iArr.length];
            int length = iArr.length;
            for (int i = 0; i < length; i++) {
                healthStatsArr[i] = healthStatsParcelerArrTakeUidSnapshots[i].getHealthStats();
            }
            return healthStatsArr;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }
}
