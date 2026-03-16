package com.android.internal.os;

import android.os.BatteryStats;

public class BatterySipper implements Comparable<BatterySipper> {
    public long cpuFgTime;
    public long cpuTime;
    public DrainType drainType;
    public long gpsTime;
    public String[] mPackages;
    public long mobileActive;
    public int mobileActiveCount;
    public long mobileRxBytes;
    public long mobileRxPackets;
    public long mobileTxBytes;
    public long mobileTxPackets;
    public double mobilemspp;
    public double noCoveragePercent;
    public String packageWithHighestDrain;
    public double percent;
    public BatteryStats.Uid uidObj;
    public long usageTime;
    public int userId;
    public double value;
    public double[] values;
    public long wakeLockTime;
    public long wifiRunningTime;
    public long wifiRxBytes;
    public long wifiRxPackets;
    public long wifiTxBytes;
    public long wifiTxPackets;

    public enum DrainType {
        IDLE,
        CELL,
        PHONE,
        WIFI,
        BLUETOOTH,
        FLASHLIGHT,
        SCREEN,
        APP,
        USER,
        UNACCOUNTED,
        OVERCOUNTED
    }

    public BatterySipper(DrainType drainType, BatteryStats.Uid uid, double[] values) {
        this.values = values;
        if (values != null) {
            this.value = values[0];
        }
        this.drainType = drainType;
        this.uidObj = uid;
    }

    public double[] getValues() {
        return this.values;
    }

    public void computeMobilemspp() {
        long packets = this.mobileRxPackets + this.mobileTxPackets;
        this.mobilemspp = packets > 0 ? this.mobileActive / packets : 0.0d;
    }

    @Override
    public int compareTo(BatterySipper other) {
        if (this.drainType != other.drainType) {
            if (this.drainType == DrainType.OVERCOUNTED) {
                return 1;
            }
            if (other.drainType == DrainType.OVERCOUNTED) {
                return -1;
            }
        }
        return Double.compare(other.value, this.value);
    }

    public String[] getPackages() {
        return this.mPackages;
    }

    public int getUid() {
        if (this.uidObj == null) {
            return 0;
        }
        return this.uidObj.getUid();
    }
}
