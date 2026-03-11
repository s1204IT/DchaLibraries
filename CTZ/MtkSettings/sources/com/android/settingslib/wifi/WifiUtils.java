package com.android.settingslib.wifi;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.os.SystemClock;
import android.util.ArraySet;
import com.android.settingslib.R;
import java.util.Map;

public class WifiUtils {
    public static String buildLoggingSummary(AccessPoint accessPoint, WifiConfiguration wifiConfiguration) {
        StringBuilder sb = new StringBuilder();
        WifiInfo info = accessPoint.getInfo();
        if (accessPoint.isActive() && info != null) {
            sb.append(" f=" + Integer.toString(info.getFrequency()));
        }
        sb.append(" " + getVisibilityStatus(accessPoint));
        if (wifiConfiguration != null && !wifiConfiguration.getNetworkSelectionStatus().isNetworkEnabled()) {
            sb.append(" (" + wifiConfiguration.getNetworkSelectionStatus().getNetworkStatusString());
            if (wifiConfiguration.getNetworkSelectionStatus().getDisableTime() > 0) {
                long jCurrentTimeMillis = (System.currentTimeMillis() - wifiConfiguration.getNetworkSelectionStatus().getDisableTime()) / 1000;
                long j = jCurrentTimeMillis % 60;
                long j2 = (jCurrentTimeMillis / 60) % 60;
                long j3 = (j2 / 60) % 60;
                sb.append(", ");
                if (j3 > 0) {
                    sb.append(Long.toString(j3) + "h ");
                }
                sb.append(Long.toString(j2) + "m ");
                sb.append(Long.toString(j) + "s ");
            }
            sb.append(")");
        }
        if (wifiConfiguration != null) {
            WifiConfiguration.NetworkSelectionStatus networkSelectionStatus = wifiConfiguration.getNetworkSelectionStatus();
            for (int i = 0; i < 14; i++) {
                if (networkSelectionStatus.getDisableReasonCounter(i) != 0) {
                    sb.append(" " + WifiConfiguration.NetworkSelectionStatus.getNetworkDisableReasonString(i) + "=" + networkSelectionStatus.getDisableReasonCounter(i));
                }
            }
        }
        return sb.toString();
    }

    static String getVisibilityStatus(AccessPoint accessPoint) {
        String bssid;
        WifiInfo info = accessPoint.getInfo();
        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        StringBuilder sb3 = new StringBuilder();
        int i = 0;
        if (accessPoint.isActive() && info != null) {
            bssid = info.getBSSID();
            if (bssid != null) {
                sb.append(" ");
                sb.append(bssid);
            }
            sb.append(" rssi=");
            sb.append(info.getRssi());
            sb.append(" ");
            sb.append(" score=");
            sb.append(info.score);
            if (accessPoint.getSpeed() != 0) {
                sb.append(" speed=");
                sb.append(accessPoint.getSpeedLabel());
            }
            sb.append(String.format(" tx=%.1f,", Double.valueOf(info.txSuccessRate)));
            sb.append(String.format("%.1f,", Double.valueOf(info.txRetriesRate)));
            sb.append(String.format("%.1f ", Double.valueOf(info.txBadRate)));
            sb.append(String.format("rx=%.1f", Double.valueOf(info.rxSuccessRate)));
        } else {
            bssid = null;
        }
        int i2 = WifiConfiguration.INVALID_RSSI;
        int i3 = WifiConfiguration.INVALID_RSSI;
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        int i4 = i2;
        int i5 = 0;
        for (ScanResult scanResult : new ArraySet(accessPoint.getScanResults())) {
            if (scanResult != null) {
                if (scanResult.frequency >= 4900 && scanResult.frequency <= 5900) {
                    i5++;
                    if (scanResult.level > i4) {
                        i4 = scanResult.level;
                    }
                    if (i5 <= 4) {
                        sb3.append(verboseScanResultSummary(accessPoint, scanResult, bssid, jElapsedRealtime));
                    }
                } else if (scanResult.frequency >= 2400 && scanResult.frequency <= 2500) {
                    i++;
                    if (scanResult.level > i3) {
                        i3 = scanResult.level;
                    }
                    if (i <= 4) {
                        sb2.append(verboseScanResultSummary(accessPoint, scanResult, bssid, jElapsedRealtime));
                    }
                }
            }
        }
        sb.append(" [");
        if (i > 0) {
            sb.append("(");
            sb.append(i);
            sb.append(")");
            if (i > 4) {
                sb.append("max=");
                sb.append(i3);
                sb.append(",");
            }
            sb.append(sb2.toString());
        }
        sb.append(";");
        if (i5 > 0) {
            sb.append("(");
            sb.append(i5);
            sb.append(")");
            if (i5 > 4) {
                sb.append("max=");
                sb.append(i4);
                sb.append(",");
            }
            sb.append(sb3.toString());
        }
        sb.append("]");
        return sb.toString();
    }

    static String verboseScanResultSummary(AccessPoint accessPoint, ScanResult scanResult, String str, long j) {
        StringBuilder sb = new StringBuilder();
        sb.append(" \n{");
        sb.append(scanResult.BSSID);
        if (scanResult.BSSID.equals(str)) {
            sb.append("*");
        }
        sb.append("=");
        sb.append(scanResult.frequency);
        sb.append(",");
        sb.append(scanResult.level);
        int specificApSpeed = getSpecificApSpeed(scanResult, accessPoint.getScoredNetworkCache());
        if (specificApSpeed != 0) {
            sb.append(",");
            sb.append(accessPoint.getSpeedLabel(specificApSpeed));
        }
        int i = ((int) (j - (scanResult.timestamp / 1000))) / 1000;
        sb.append(",");
        sb.append(i);
        sb.append("s");
        sb.append("}");
        return sb.toString();
    }

    private static int getSpecificApSpeed(ScanResult scanResult, Map<String, TimestampedScoredNetwork> map) {
        TimestampedScoredNetwork timestampedScoredNetwork = map.get(scanResult.BSSID);
        if (timestampedScoredNetwork == null) {
            return 0;
        }
        return timestampedScoredNetwork.getScore().calculateBadge(scanResult.level);
    }

    public static String getMeteredLabel(Context context, WifiConfiguration wifiConfiguration) {
        if (wifiConfiguration.meteredOverride == 1 || (wifiConfiguration.meteredHint && !isMeteredOverridden(wifiConfiguration))) {
            return context.getString(R.string.wifi_metered_label);
        }
        return context.getString(R.string.wifi_unmetered_label);
    }

    public static boolean isMeteredOverridden(WifiConfiguration wifiConfiguration) {
        return wifiConfiguration.meteredOverride != 0;
    }
}
