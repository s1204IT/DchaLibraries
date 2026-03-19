package com.android.server.wifi;

import android.net.wifi.WifiInfo;
import com.android.server.wifi.util.KalmanFilter;
import com.android.server.wifi.util.Matrix;

public class VelocityBasedConnectedScore extends ConnectedScore {
    private double mEstimatedRateOfRssiChange;
    private final KalmanFilter mFilter;
    private double mFilteredRssi;
    private int mFrequency;
    private long mLastMillis;
    private double mMinimumPpsForMeasuringSuccess;
    private final ScoringParams mScoringParams;
    private double mThresholdAdjustment;

    public VelocityBasedConnectedScore(ScoringParams scoringParams, Clock clock) {
        super(clock);
        this.mFrequency = ScoringParams.BAND5;
        this.mMinimumPpsForMeasuringSuccess = 2.0d;
        this.mScoringParams = scoringParams;
        this.mFilter = new KalmanFilter();
        this.mFilter.mH = new Matrix(2, new double[]{1.0d, 0.0d});
        this.mFilter.mR = new Matrix(1, new double[]{1.0d});
    }

    private void setDeltaTimeSeconds(double d) {
        this.mFilter.mF = new Matrix(2, new double[]{1.0d, d, 0.0d, 1.0d});
        Matrix matrix = new Matrix(1, new double[]{0.5d * d * d, d});
        this.mFilter.mQ = matrix.dotTranspose(matrix).dot(new Matrix(2, new double[]{4.0E-4d, 0.0d, 0.0d, 4.0E-4d}));
    }

    @Override
    public void reset() {
        this.mLastMillis = 0L;
        this.mThresholdAdjustment = 0.0d;
        this.mFilter.mx = null;
    }

    @Override
    public void updateUsingRssi(int i, long j, double d) {
        if (j <= 0) {
            return;
        }
        if (this.mLastMillis <= 0 || j < this.mLastMillis || this.mFilter.mx == null) {
            this.mFilter.mx = new Matrix(1, new double[]{i, 0.0d});
            this.mFilter.mP = new Matrix(2, new double[]{9.0d * d * d, 0.0d, 0.0d, 0.0d});
        } else {
            this.mFilter.mR.put(0, 0, d * d);
            setDeltaTimeSeconds((j - this.mLastMillis) * 0.001d);
            this.mFilter.predict();
            this.mFilter.update(new Matrix(1, new double[]{i}));
        }
        this.mLastMillis = j;
        this.mFilteredRssi = this.mFilter.mx.get(0, 0);
        this.mEstimatedRateOfRssiChange = this.mFilter.mx.get(1, 0);
    }

    @Override
    public void updateUsingWifiInfo(WifiInfo wifiInfo, long j) {
        int frequency = wifiInfo.getFrequency();
        if (frequency != this.mFrequency) {
            this.mLastMillis = 0L;
            this.mFrequency = frequency;
        }
        updateUsingRssi(wifiInfo.getRssi(), j, this.mDefaultRssiStandardDeviation);
        adjustThreshold(wifiInfo);
    }

    public double getFilteredRssi() {
        return this.mFilteredRssi;
    }

    public double getEstimatedRateOfRssiChange() {
        return this.mEstimatedRateOfRssiChange;
    }

    public double getAdjustedRssiThreshold() {
        return ((double) this.mScoringParams.getExitRssi(this.mFrequency)) + this.mThresholdAdjustment;
    }

    private void adjustThreshold(WifiInfo wifiInfo) {
        if (this.mThresholdAdjustment >= -7.0d && this.mFilteredRssi < getAdjustedRssiThreshold() + 2.0d && Math.abs(this.mEstimatedRateOfRssiChange) < 0.2d) {
            double d = wifiInfo.txSuccessRate;
            double d2 = wifiInfo.rxSuccessRate;
            if (d >= this.mMinimumPpsForMeasuringSuccess && d2 >= this.mMinimumPpsForMeasuringSuccess) {
                if (d / ((wifiInfo.txBadRate + d) + wifiInfo.txRetriesRate) > 0.2d) {
                    this.mThresholdAdjustment -= 0.5d;
                }
            }
        }
    }

    @Override
    public int generateScore() {
        if (this.mFilter.mx == null) {
            return 51;
        }
        double adjustedRssiThreshold = getAdjustedRssiThreshold();
        double horizonSeconds = this.mScoringParams.getHorizonSeconds();
        Matrix matrix = new Matrix(this.mFilter.mx);
        double d = matrix.get(0, 0);
        setDeltaTimeSeconds(horizonSeconds);
        double d2 = this.mFilter.mF.dot(matrix).get(0, 0);
        if (d2 > d) {
            d2 = d;
        }
        return ((int) (Math.round(d2) - adjustedRssiThreshold)) + 50;
    }
}
