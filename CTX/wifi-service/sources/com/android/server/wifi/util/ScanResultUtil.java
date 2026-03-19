package com.android.server.wifi.util;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.server.wifi.ScanDetail;
import com.android.server.wifi.hotspot2.NetworkDetail;
import com.mediatek.server.wifi.MtkWapi;
import java.io.PrintWriter;
import java.util.List;

public class ScanResultUtil {
    private ScanResultUtil() {
    }

    public static ScanDetail toScanDetail(ScanResult scanResult) {
        return new ScanDetail(scanResult, new NetworkDetail(scanResult.BSSID, scanResult.informationElements, scanResult.anqpLines, scanResult.frequency));
    }

    public static boolean isScanResultForPskNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("PSK");
    }

    public static boolean isScanResultForEapNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("EAP");
    }

    public static boolean isScanResultForWepNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("WEP");
    }

    public static boolean isScanResultForOpenNetwork(ScanResult scanResult) {
        return (isScanResultForWepNetwork(scanResult) || isScanResultForPskNetwork(scanResult) || MtkWapi.isScanResultForWapiNetwork(scanResult) || isScanResultForEapNetwork(scanResult)) ? false : true;
    }

    @VisibleForTesting
    public static String createQuotedSSID(String str) {
        return "\"" + str + "\"";
    }

    public static WifiConfiguration createNetworkFromScanResult(ScanResult scanResult) {
        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        wifiConfiguration.SSID = createQuotedSSID(scanResult.SSID);
        setAllowedKeyManagementFromScanResult(scanResult, wifiConfiguration);
        return wifiConfiguration;
    }

    public static void setAllowedKeyManagementFromScanResult(ScanResult scanResult, WifiConfiguration wifiConfiguration) {
        if (MtkWapi.isScanResultForWapiNetwork(scanResult)) {
            wifiConfiguration.allowedKeyManagement.set(scanResult.capabilities.contains("PSK") ? 8 : 9);
            return;
        }
        if (isScanResultForPskNetwork(scanResult)) {
            wifiConfiguration.allowedKeyManagement.set(1);
            return;
        }
        if (isScanResultForEapNetwork(scanResult)) {
            wifiConfiguration.allowedKeyManagement.set(2);
            wifiConfiguration.allowedKeyManagement.set(3);
        } else {
            if (isScanResultForWepNetwork(scanResult)) {
                wifiConfiguration.allowedKeyManagement.set(0);
                wifiConfiguration.allowedAuthAlgorithms.set(0);
                wifiConfiguration.allowedAuthAlgorithms.set(1);
                return;
            }
            wifiConfiguration.allowedKeyManagement.set(0);
        }
    }

    public static void dumpScanResults(PrintWriter printWriter, List<ScanResult> list, long j) {
        String str;
        String str2;
        if (list != null && list.size() != 0) {
            printWriter.println("    BSSID              Frequency      RSSI           Age(sec)     SSID                                 Flags");
            for (ScanResult scanResult : list) {
                long j2 = scanResult.timestamp / 1000;
                if (j2 <= 0) {
                    str = "___?___";
                } else if (j < j2) {
                    str = "  0.000";
                } else if (j2 < j - 1000000) {
                    str = ">1000.0";
                } else {
                    str = String.format("%3.3f", Double.valueOf((j - j2) / 1000.0d));
                }
                String str3 = scanResult.SSID == null ? "" : scanResult.SSID;
                if (ArrayUtils.size(scanResult.radioChainInfos) == 1) {
                    str2 = String.format("%5d(%1d:%3d)       ", Integer.valueOf(scanResult.level), Integer.valueOf(scanResult.radioChainInfos[0].id), Integer.valueOf(scanResult.radioChainInfos[0].level));
                } else if (ArrayUtils.size(scanResult.radioChainInfos) == 2) {
                    str2 = String.format("%5d(%1d:%3d/%1d:%3d)", Integer.valueOf(scanResult.level), Integer.valueOf(scanResult.radioChainInfos[0].id), Integer.valueOf(scanResult.radioChainInfos[0].level), Integer.valueOf(scanResult.radioChainInfos[1].id), Integer.valueOf(scanResult.radioChainInfos[1].level));
                } else {
                    str2 = String.format("%9d         ", Integer.valueOf(scanResult.level));
                }
                printWriter.printf("  %17s  %9d  %18s   %7s    %-32s  %s\n", scanResult.BSSID, Integer.valueOf(scanResult.frequency), str2, str, String.format("%1.32s", str3), scanResult.capabilities);
            }
        }
    }
}
