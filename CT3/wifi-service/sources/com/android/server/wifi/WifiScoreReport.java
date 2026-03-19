package com.android.server.wifi;

import android.net.NetworkAgent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.util.Log;

public class WifiScoreReport {
    private static final int AGGRESSIVE_HANDOVER_PENALTY = 6;
    private static final int BAD_LINKSPEED_PENALTY = 4;
    private static final int BAD_RSSI_COUNT_PENALTY = 2;
    private static final int GOOD_LINKSPEED_BONUS = 4;
    private static final int HOME_VISIBLE_NETWORK_MAX_COUNT = 6;
    private static final int LINK_STUCK_PENALTY = 2;
    private static final int MAX_BAD_LINKSPEED_COUNT = 6;
    private static final int MAX_BAD_RSSI_COUNT = 7;
    private static final int MAX_LOW_RSSI_COUNT = 1;
    private static final int MAX_STUCK_LINK_COUNT = 5;
    private static final int MAX_SUCCESS_COUNT_OF_STUCK_LINK = 3;
    private static final int MIN_NUM_TICKS_AT_STATE = 1000;
    private static final int MIN_SUCCESS_COUNT = 5;
    private static final int MIN_SUSTAINED_LINK_STUCK_COUNT = 1;
    private static final double MIN_TX_RATE_FOR_WORKING_LINK = 0.3d;
    private static final int SCAN_CACHE_COUNT_PENALTY = 2;
    private static final int SCAN_CACHE_VISIBILITY_MS = 12000;
    private static final int STARTING_SCORE = 56;
    private static final String TAG = "WifiStateMachine";
    private static final int USER_DISCONNECT_PENALTY = 5;
    private int mBadLinkspeedcount;
    private String mReport;

    WifiScoreReport(String report, int badLinkspeedcount) {
        this.mReport = report;
        this.mBadLinkspeedcount = badLinkspeedcount;
    }

    public String getReport() {
        return this.mReport;
    }

    public int getBadLinkspeedcount() {
        return this.mBadLinkspeedcount;
    }

    public static WifiScoreReport calculateScore(WifiInfo wifiInfo, WifiConfiguration currentConfiguration, WifiConfigManager wifiConfigManager, NetworkAgent networkAgent, WifiScoreReport lastReport, int aggressiveHandover, boolean hasCustomizedAutoConnect) {
        boolean debugLogging = wifiConfigManager.mEnableVerboseLogging.get() > 0 ? true : true;
        StringBuilder sb = new StringBuilder();
        int score = STARTING_SCORE;
        boolean isBadLinkspeed = (!wifiInfo.is24GHz() || wifiInfo.getLinkSpeed() >= wifiConfigManager.mBadLinkSpeed24) ? wifiInfo.is5GHz() && wifiInfo.getLinkSpeed() < wifiConfigManager.mBadLinkSpeed5 : true;
        boolean isGoodLinkspeed = (!wifiInfo.is24GHz() || wifiInfo.getLinkSpeed() < wifiConfigManager.mGoodLinkSpeed24) ? wifiInfo.is5GHz() && wifiInfo.getLinkSpeed() >= wifiConfigManager.mGoodLinkSpeed5 : true;
        int badLinkspeedcount = lastReport != null ? lastReport.getBadLinkspeedcount() : 0;
        if (isBadLinkspeed) {
            if (badLinkspeedcount < 6) {
                badLinkspeedcount++;
            }
        } else if (badLinkspeedcount > 0) {
            badLinkspeedcount--;
        }
        if (isBadLinkspeed) {
            sb.append(" bl(").append(badLinkspeedcount).append(")");
        }
        if (isGoodLinkspeed) {
            sb.append(" gl");
        }
        boolean use24Thresholds = false;
        boolean homeNetworkBoost = false;
        ScanDetailCache scanDetailCache = wifiConfigManager.getScanDetailCache(currentConfiguration);
        if (currentConfiguration != null && scanDetailCache != null) {
            currentConfiguration.setVisibility(scanDetailCache.getVisibility(12000L));
            if (currentConfiguration.visibility != null && currentConfiguration.visibility.rssi24 != WifiConfiguration.INVALID_RSSI && currentConfiguration.visibility.rssi24 >= currentConfiguration.visibility.rssi5 - 2) {
                use24Thresholds = true;
            }
            if (scanDetailCache.size() <= 6 && currentConfiguration.allowedKeyManagement.cardinality() == 1 && currentConfiguration.allowedKeyManagement.get(1)) {
                homeNetworkBoost = true;
            }
        }
        if (homeNetworkBoost) {
            sb.append(" hn");
        }
        if (use24Thresholds) {
            sb.append(" u24");
        }
        int rssi = (wifiInfo.getRssi() - (aggressiveHandover * 6)) + (homeNetworkBoost ? 5 : 0);
        sb.append(String.format(" rssi=%d ag=%d", Integer.valueOf(rssi), Integer.valueOf(aggressiveHandover)));
        boolean is24GHz = !use24Thresholds ? wifiInfo.is24GHz() : true;
        boolean isBadRSSI = (!is24GHz || rssi >= wifiConfigManager.mThresholdMinimumRssi24.get()) ? !is24GHz && rssi < wifiConfigManager.mThresholdMinimumRssi5.get() : true;
        boolean isLowRSSI = (!is24GHz || rssi >= wifiConfigManager.mThresholdQualifiedRssi24.get()) ? !is24GHz && wifiInfo.getRssi() < wifiConfigManager.mThresholdMinimumRssi5.get() : true;
        boolean isHighRSSI = (!is24GHz || rssi < wifiConfigManager.mThresholdSaturatedRssi24.get()) ? !is24GHz && wifiInfo.getRssi() >= wifiConfigManager.mThresholdSaturatedRssi5.get() : true;
        if (isBadRSSI) {
            sb.append(" br");
        }
        if (isLowRSSI) {
            sb.append(" lr");
        }
        if (isHighRSSI) {
            sb.append(" hr");
        }
        int penalizedDueToUserTriggeredDisconnect = 0;
        if (currentConfiguration != null && (wifiInfo.txSuccessRate > 5.0d || wifiInfo.rxSuccessRate > 5.0d)) {
            if (isBadRSSI) {
                currentConfiguration.numTicksAtBadRSSI++;
                if (currentConfiguration.numTicksAtBadRSSI > MIN_NUM_TICKS_AT_STATE) {
                    if (currentConfiguration.numUserTriggeredWifiDisableBadRSSI > 0) {
                        currentConfiguration.numUserTriggeredWifiDisableBadRSSI--;
                    }
                    if (currentConfiguration.numUserTriggeredWifiDisableLowRSSI > 0) {
                        currentConfiguration.numUserTriggeredWifiDisableLowRSSI--;
                    }
                    if (currentConfiguration.numUserTriggeredWifiDisableNotHighRSSI > 0) {
                        currentConfiguration.numUserTriggeredWifiDisableNotHighRSSI--;
                    }
                    currentConfiguration.numTicksAtBadRSSI = 0;
                }
                if (wifiConfigManager.mEnableWifiCellularHandoverUserTriggeredAdjustment && (currentConfiguration.numUserTriggeredWifiDisableBadRSSI > 0 || currentConfiguration.numUserTriggeredWifiDisableLowRSSI > 0 || currentConfiguration.numUserTriggeredWifiDisableNotHighRSSI > 0)) {
                    score = 51;
                    penalizedDueToUserTriggeredDisconnect = 1;
                    sb.append(" p1");
                }
            } else if (isLowRSSI) {
                currentConfiguration.numTicksAtLowRSSI++;
                if (currentConfiguration.numTicksAtLowRSSI > MIN_NUM_TICKS_AT_STATE) {
                    if (currentConfiguration.numUserTriggeredWifiDisableLowRSSI > 0) {
                        currentConfiguration.numUserTriggeredWifiDisableLowRSSI--;
                    }
                    if (currentConfiguration.numUserTriggeredWifiDisableNotHighRSSI > 0) {
                        currentConfiguration.numUserTriggeredWifiDisableNotHighRSSI--;
                    }
                    currentConfiguration.numTicksAtLowRSSI = 0;
                }
                if (wifiConfigManager.mEnableWifiCellularHandoverUserTriggeredAdjustment && (currentConfiguration.numUserTriggeredWifiDisableLowRSSI > 0 || currentConfiguration.numUserTriggeredWifiDisableNotHighRSSI > 0)) {
                    score = 51;
                    penalizedDueToUserTriggeredDisconnect = 2;
                    sb.append(" p2");
                }
            } else if (!isHighRSSI) {
                currentConfiguration.numTicksAtNotHighRSSI++;
                if (currentConfiguration.numTicksAtNotHighRSSI > MIN_NUM_TICKS_AT_STATE) {
                    if (currentConfiguration.numUserTriggeredWifiDisableNotHighRSSI > 0) {
                        currentConfiguration.numUserTriggeredWifiDisableNotHighRSSI--;
                    }
                    currentConfiguration.numTicksAtNotHighRSSI = 0;
                }
                if (wifiConfigManager.mEnableWifiCellularHandoverUserTriggeredAdjustment && currentConfiguration.numUserTriggeredWifiDisableNotHighRSSI > 0) {
                    score = 51;
                    penalizedDueToUserTriggeredDisconnect = 3;
                    sb.append(" p3");
                }
            }
            sb.append(String.format(" ticks %d,%d,%d", Integer.valueOf(currentConfiguration.numTicksAtBadRSSI), Integer.valueOf(currentConfiguration.numTicksAtLowRSSI), Integer.valueOf(currentConfiguration.numTicksAtNotHighRSSI)));
        }
        if (debugLogging) {
            String rssiStatus = "";
            if (isBadRSSI) {
                rssiStatus = " badRSSI ";
            } else if (isHighRSSI) {
                rssiStatus = " highRSSI ";
            } else if (isLowRSSI) {
                rssiStatus = " lowRSSI ";
            }
            if (isBadLinkspeed) {
                rssiStatus = rssiStatus + " lowSpeed ";
            }
            Log.d(TAG, "calculateWifiScore freq=" + Integer.toString(wifiInfo.getFrequency()) + " speed=" + Integer.toString(wifiInfo.getLinkSpeed()) + " score=" + Integer.toString(wifiInfo.score) + rssiStatus + " -> txbadrate=" + String.format("%.2f", Double.valueOf(wifiInfo.txBadRate)) + " txgoodrate=" + String.format("%.2f", Double.valueOf(wifiInfo.txSuccessRate)) + " txretriesrate=" + String.format("%.2f", Double.valueOf(wifiInfo.txRetriesRate)) + " rxrate=" + String.format("%.2f", Double.valueOf(wifiInfo.rxSuccessRate)) + " userTriggerdPenalty" + penalizedDueToUserTriggeredDisconnect);
        }
        if (wifiInfo.txBadRate >= 1.0d && wifiInfo.txSuccessRate < 3.0d && (isBadRSSI || isLowRSSI)) {
            if (wifiInfo.linkStuckCount < 5) {
                wifiInfo.linkStuckCount++;
            }
            sb.append(String.format(" ls+=%d", Integer.valueOf(wifiInfo.linkStuckCount)));
            if (debugLogging) {
                Log.d(TAG, " bad link -> stuck count =" + Integer.toString(wifiInfo.linkStuckCount));
            }
        } else if (wifiInfo.txBadRate < MIN_TX_RATE_FOR_WORKING_LINK) {
            if (wifiInfo.linkStuckCount > 0) {
                wifiInfo.linkStuckCount--;
            }
            sb.append(String.format(" ls-=%d", Integer.valueOf(wifiInfo.linkStuckCount)));
            if (debugLogging) {
                Log.d(TAG, " good link -> stuck count =" + Integer.toString(wifiInfo.linkStuckCount));
            }
        }
        sb.append(String.format(" [%d", Integer.valueOf(score)));
        if (wifiInfo.linkStuckCount > 1) {
            score -= (wifiInfo.linkStuckCount - 1) * 2;
        }
        sb.append(String.format(",%d", Integer.valueOf(score)));
        if (isBadLinkspeed) {
            score -= 4;
            if (debugLogging) {
                Log.d(TAG, " isBadLinkspeed   ---> count=" + badLinkspeedcount + " score=" + Integer.toString(score));
            }
        } else if (isGoodLinkspeed && wifiInfo.txSuccessRate > 5.0d) {
            score += 4;
        }
        sb.append(String.format(",%d", Integer.valueOf(score)));
        if (isBadRSSI) {
            if (wifiInfo.badRssiCount < 7) {
                wifiInfo.badRssiCount++;
            }
        } else if (isLowRSSI) {
            wifiInfo.lowRssiCount = 1;
            if (wifiInfo.badRssiCount > 0) {
                wifiInfo.badRssiCount--;
            }
        } else {
            wifiInfo.badRssiCount = 0;
            wifiInfo.lowRssiCount = 0;
        }
        int score2 = score - ((wifiInfo.badRssiCount * 2) + wifiInfo.lowRssiCount);
        sb.append(String.format(",%d", Integer.valueOf(score2)));
        if (debugLogging) {
            Log.d(TAG, " badRSSI count" + Integer.toString(wifiInfo.badRssiCount) + " lowRSSI count" + Integer.toString(wifiInfo.lowRssiCount) + " --> score " + Integer.toString(score2));
        }
        if (isHighRSSI) {
            score2 += 5;
            if (debugLogging) {
                Log.d(TAG, " isHighRSSI       ---> score=" + Integer.toString(score2));
            }
        }
        sb.append(String.format(",%d]", Integer.valueOf(score2)));
        sb.append(String.format(" brc=%d lrc=%d", Integer.valueOf(wifiInfo.badRssiCount), Integer.valueOf(wifiInfo.lowRssiCount)));
        if (score2 > 60) {
            score2 = 60;
        }
        if (score2 < 0) {
            score2 = 0;
        }
        if (score2 != wifiInfo.score) {
            if (debugLogging) {
                Log.d(TAG, "calculateWifiScore() report new score " + Integer.toString(score2));
            }
            wifiInfo.score = score2;
            if (networkAgent != null && !hasCustomizedAutoConnect) {
                networkAgent.sendNetworkScore(score2);
            }
        }
        return new WifiScoreReport(sb.toString(), badLinkspeedcount);
    }

    public static WifiScoreReport calculateScore(WifiInfo wifiInfo, WifiConfiguration currentConfiguration, WifiConfigManager wifiConfigManager, NetworkAgent networkAgent, WifiScoreReport lastReport, int aggressiveHandover) {
        return calculateScore(wifiInfo, currentConfiguration, wifiConfigManager, networkAgent, lastReport, aggressiveHandover, false);
    }
}
