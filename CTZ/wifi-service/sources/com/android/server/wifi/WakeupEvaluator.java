package com.android.server.wifi;

import android.content.Context;
import android.net.wifi.ScanResult;
import com.android.internal.annotations.VisibleForTesting;
import java.util.Collection;

public class WakeupEvaluator {
    private final int mThresholdMinimumRssi24;
    private final int mThresholdMinimumRssi5;

    public static WakeupEvaluator fromContext(Context context) {
        ScoringParams scoringParams = new ScoringParams(context);
        return new WakeupEvaluator(scoringParams.getEntryRssi(ScoringParams.BAND2), scoringParams.getEntryRssi(ScoringParams.BAND5));
    }

    @VisibleForTesting
    WakeupEvaluator(int i, int i2) {
        this.mThresholdMinimumRssi24 = i;
        this.mThresholdMinimumRssi5 = i2;
    }

    public ScanResult findViableNetwork(Collection<ScanResult> collection, Collection<ScanResultMatchInfo> collection2) {
        ScanResult scanResult = null;
        for (ScanResult scanResult2 : collection) {
            if (!isBelowThreshold(scanResult2) && collection2.contains(ScanResultMatchInfo.fromScanResult(scanResult2)) && (scanResult == null || scanResult.level < scanResult2.level)) {
                scanResult = scanResult2;
            }
        }
        return scanResult;
    }

    public boolean isBelowThreshold(ScanResult scanResult) {
        return (scanResult.is24GHz() && scanResult.level < this.mThresholdMinimumRssi24) || (scanResult.is5GHz() && scanResult.level < this.mThresholdMinimumRssi5);
    }
}
