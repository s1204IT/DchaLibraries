package com.android.server.wifi.scanner;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiScanner;
import com.android.server.wifi.WifiNative;
import java.util.ArrayList;

public class ScanScheduleUtil {
    public static boolean channelEquals(WifiNative.ChannelSettings channelSettings, WifiNative.ChannelSettings channelSettings2) {
        if (channelSettings == null || channelSettings2 == null) {
            return false;
        }
        if (channelSettings == channelSettings2) {
            return true;
        }
        if (channelSettings.frequency != channelSettings2.frequency || channelSettings.dwell_time_ms != channelSettings2.dwell_time_ms || channelSettings.passive != channelSettings2.passive) {
            return false;
        }
        return true;
    }

    public static boolean bucketEquals(WifiNative.BucketSettings bucketSettings, WifiNative.BucketSettings bucketSettings2) {
        if (bucketSettings == null || bucketSettings2 == null) {
            return false;
        }
        if (bucketSettings == bucketSettings2) {
            return true;
        }
        if (bucketSettings.bucket != bucketSettings2.bucket || bucketSettings.band != bucketSettings2.band || bucketSettings.period_ms != bucketSettings2.period_ms || bucketSettings.report_events != bucketSettings2.report_events || bucketSettings.num_channels != bucketSettings2.num_channels) {
            return false;
        }
        for (int i = 0; i < bucketSettings.num_channels; i++) {
            if (!channelEquals(bucketSettings.channels[i], bucketSettings2.channels[i])) {
                return false;
            }
        }
        return true;
    }

    public static boolean scheduleEquals(WifiNative.ScanSettings scanSettings, WifiNative.ScanSettings scanSettings2) {
        if (scanSettings == null || scanSettings2 == null) {
            return false;
        }
        if (scanSettings == scanSettings2) {
            return true;
        }
        if (scanSettings.base_period_ms != scanSettings2.base_period_ms || scanSettings.max_ap_per_scan != scanSettings2.max_ap_per_scan || scanSettings.report_threshold_percent != scanSettings2.report_threshold_percent || scanSettings.report_threshold_num_scans != scanSettings2.report_threshold_num_scans || scanSettings.num_buckets != scanSettings2.num_buckets) {
            return false;
        }
        for (int i = 0; i < scanSettings.num_buckets; i++) {
            if (!bucketEquals(scanSettings.buckets[i], scanSettings2.buckets[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean isBucketMaybeScanned(int i, int i2) {
        return i2 == 0 || i < 0 || ((1 << i) & i2) != 0;
    }

    private static boolean isBucketDefinitlyScanned(int i, int i2) {
        if (i < 0) {
            return true;
        }
        if (i2 != 0 && ((1 << i) & i2) != 0) {
            return true;
        }
        return false;
    }

    public static boolean shouldReportFullScanResultForSettings(ChannelHelper channelHelper, ScanResult scanResult, int i, WifiScanner.ScanSettings scanSettings, int i2) {
        if (isBucketMaybeScanned(i2, i)) {
            return channelHelper.settingsContainChannel(scanSettings, scanResult.frequency);
        }
        return false;
    }

    public static WifiScanner.ScanData[] filterResultsForSettings(ChannelHelper channelHelper, WifiScanner.ScanData[] scanDataArr, WifiScanner.ScanSettings scanSettings, int i) {
        ArrayList arrayList = new ArrayList(scanDataArr.length);
        ArrayList arrayList2 = new ArrayList();
        for (WifiScanner.ScanData scanData : scanDataArr) {
            if (isBucketMaybeScanned(i, scanData.getBucketsScanned())) {
                arrayList2.clear();
                for (ScanResult scanResult : scanData.getResults()) {
                    if (channelHelper.settingsContainChannel(scanSettings, scanResult.frequency)) {
                        arrayList2.add(scanResult);
                    }
                    if (scanSettings.numBssidsPerScan > 0 && arrayList2.size() >= scanSettings.numBssidsPerScan) {
                        break;
                    }
                }
                if (arrayList2.size() == scanData.getResults().length) {
                    arrayList.add(scanData);
                } else if (arrayList2.size() > 0 || isBucketDefinitlyScanned(i, scanData.getBucketsScanned())) {
                    arrayList.add(new WifiScanner.ScanData(scanData.getId(), scanData.getFlags(), (ScanResult[]) arrayList2.toArray(new ScanResult[arrayList2.size()])));
                }
            }
        }
        if (arrayList.size() == 0) {
            return null;
        }
        return (WifiScanner.ScanData[]) arrayList.toArray(new WifiScanner.ScanData[arrayList.size()]);
    }
}
