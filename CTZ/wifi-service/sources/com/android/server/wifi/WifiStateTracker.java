package com.android.server.wifi;

import android.os.RemoteException;
import android.util.Log;
import com.android.internal.app.IBatteryStats;
import java.util.concurrent.RejectedExecutionException;

public class WifiStateTracker {
    public static final int CONNECTED = 3;
    public static final int DISCONNECTED = 2;
    public static final int INVALID = 0;
    public static final int SCAN_MODE = 1;
    public static final int SOFT_AP = 4;
    private static final String TAG = "WifiStateTracker";
    private IBatteryStats mBatteryStats;
    private int mWifiState = 0;

    public WifiStateTracker(IBatteryStats iBatteryStats) {
        this.mBatteryStats = iBatteryStats;
    }

    private void informWifiStateBatteryStats(int i) {
        try {
            this.mBatteryStats.noteWifiState(i, (String) null);
        } catch (RemoteException e) {
            Log.e(TAG, "Battery stats unreachable " + e.getMessage());
        } catch (RejectedExecutionException e2) {
            Log.e(TAG, "Battery stats executor is being shutdown " + e2.getMessage());
        }
    }

    public void updateState(int i) {
        int i2;
        if (i != this.mWifiState) {
            switch (i) {
                case 0:
                    this.mWifiState = 0;
                    return;
                case 1:
                    i2 = 1;
                    break;
                case 2:
                    i2 = 3;
                    break;
                case 3:
                    i2 = 4;
                    break;
                case 4:
                    i2 = 7;
                    break;
                default:
                    return;
            }
            this.mWifiState = i;
            informWifiStateBatteryStats(i2);
        }
    }
}
