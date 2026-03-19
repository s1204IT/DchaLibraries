package com.android.server.wifi;

import android.net.wifi.WifiInfo;

public class ExtendedWifiInfo extends WifiInfo {
    private static final double FILTER_TIME_CONSTANT = 3000.0d;
    private static final long RESET_TIME_STAMP = Long.MIN_VALUE;
    private static final int SOURCE_LLSTATS = 2;
    private static final int SOURCE_TRAFFIC_COUNTERS = 1;
    private static final int SOURCE_UNKNOWN = 0;
    private int mLastSource = 0;
    private long mLastPacketCountUpdateTimeStamp = RESET_TIME_STAMP;
    private boolean mEnableConnectedMacRandomization = false;

    public void reset() {
        super.reset();
        this.mLastSource = 0;
        this.mLastPacketCountUpdateTimeStamp = RESET_TIME_STAMP;
        if (this.mEnableConnectedMacRandomization) {
            setMacAddress("02:00:00:00:00:00");
        }
    }

    public void updatePacketRates(WifiLinkLayerStats wifiLinkLayerStats, long j) {
        update(2, wifiLinkLayerStats.txmpdu_be + wifiLinkLayerStats.txmpdu_bk + wifiLinkLayerStats.txmpdu_vi + wifiLinkLayerStats.txmpdu_vo, wifiLinkLayerStats.retries_be + wifiLinkLayerStats.retries_bk + wifiLinkLayerStats.retries_vi + wifiLinkLayerStats.retries_vo, wifiLinkLayerStats.lostmpdu_be + wifiLinkLayerStats.lostmpdu_bk + wifiLinkLayerStats.lostmpdu_vi + wifiLinkLayerStats.lostmpdu_vo, wifiLinkLayerStats.rxmpdu_be + wifiLinkLayerStats.rxmpdu_bk + wifiLinkLayerStats.rxmpdu_vi + wifiLinkLayerStats.rxmpdu_vo, j);
    }

    public void updatePacketRates(long j, long j2, long j3) {
        update(1, j, 0L, 0L, j2, j3);
    }

    private void update(int i, long j, long j2, long j3, long j4, long j5) {
        long j6;
        if (i == this.mLastSource && this.mLastPacketCountUpdateTimeStamp != RESET_TIME_STAMP && this.mLastPacketCountUpdateTimeStamp < j5 && this.txBad <= j3 && this.txSuccess <= j && this.rxSuccess <= j4 && this.txRetries <= j2) {
            double d = j5 - this.mLastPacketCountUpdateTimeStamp;
            double dExp = Math.exp(((-1.0d) * d) / FILTER_TIME_CONSTANT);
            double d2 = 1.0d - dExp;
            this.txBadRate = (this.txBadRate * dExp) + ((((j3 - this.txBad) * 1000.0d) / d) * d2);
            this.txSuccessRate = (this.txSuccessRate * dExp) + ((((j - this.txSuccess) * 1000.0d) / d) * d2);
            this.rxSuccessRate = (this.rxSuccessRate * dExp) + ((((j4 - this.rxSuccess) * 1000.0d) / d) * d2);
            double d3 = this.txRetriesRate * dExp;
            j6 = j2;
            this.txRetriesRate = d3 + ((((j6 - this.txRetries) * 1000.0d) / d) * d2);
        } else {
            j6 = j2;
            this.txBadRate = 0.0d;
            this.txSuccessRate = 0.0d;
            this.rxSuccessRate = 0.0d;
            this.txRetriesRate = 0.0d;
            this.mLastSource = i;
        }
        this.txBad = j3;
        this.txSuccess = j;
        this.rxSuccess = j4;
        this.txRetries = j6;
        this.mLastPacketCountUpdateTimeStamp = j5;
    }

    public void setEnableConnectedMacRandomization(boolean z) {
        this.mEnableConnectedMacRandomization = z;
    }
}
