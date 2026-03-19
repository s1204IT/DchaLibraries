package com.android.server.wifi;

import android.net.wifi.WifiInfo;

public class AggressiveConnectedScore extends ConnectedScore {
    private int mFrequencyMHz;
    private int mRssi;
    private final ScoringParams mScoringParams;

    public AggressiveConnectedScore(ScoringParams scoringParams, Clock clock) {
        super(clock);
        this.mFrequencyMHz = ScoringParams.BAND5;
        this.mRssi = 0;
        this.mScoringParams = scoringParams;
    }

    @Override
    public void updateUsingRssi(int i, long j, double d) {
        this.mRssi = i;
    }

    @Override
    public void updateUsingWifiInfo(WifiInfo wifiInfo, long j) {
        this.mFrequencyMHz = wifiInfo.getFrequency();
        this.mRssi = wifiInfo.getRssi();
    }

    @Override
    public void reset() {
        this.mFrequencyMHz = ScoringParams.BAND5;
    }

    @Override
    public int generateScore() {
        return (this.mRssi - this.mScoringParams.getSufficientRssi(this.mFrequencyMHz)) + 50;
    }
}
