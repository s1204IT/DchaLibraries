package com.android.server.wifi;

import android.content.Context;
import android.net.NetworkKey;
import android.net.NetworkScoreManager;
import android.net.WifiKey;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConnectionStatistics;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class WifiAutoJoinController {
    public static final int AUTO_JOIN_EXTENDED_ROAMING = 2;
    public static final int AUTO_JOIN_IDLE = 0;
    public static final int AUTO_JOIN_OUT_OF_NETWORK_ROAMING = 3;
    public static final int AUTO_JOIN_ROAMING = 1;
    private static final long DEFAULT_EPHEMERAL_OUT_OF_RANGE_TIMEOUT_MS = 60000;
    public static final int HIGH_THRESHOLD_MODIFIER = 5;
    private static final String TAG = "WifiAutoJoinController ";
    private static final long loseBlackListHardMilli = 28800000;
    private static final long loseBlackListSoftMilli = 1800000;
    private static final boolean mStaStaSupported = false;
    private Context mContext;
    private WifiNetworkScoreCache mNetworkScoreCache;
    private WifiConfigStore mWifiConfigStore;
    private WifiConnectionStatistics mWifiConnectionStatistics;
    private WifiNative mWifiNative;
    private WifiStateMachine mWifiStateMachine;
    private NetworkScoreManager scoreManager;
    private static boolean DBG = false;
    private static boolean VDBG = false;
    public static int mScanResultMaximumAge = 40000;
    public static int mScanResultAutoJoinAge = 5000;
    private String mCurrentConfigurationKey = null;
    private HashMap<String, ScanResult> scanResultCache = new HashMap<>();
    private boolean mAllowUntrustedConnections = false;
    boolean didOverride = false;
    boolean didBailDueToWeakRssi = false;
    int weakRssiBailCount = 0;

    WifiAutoJoinController(Context c, WifiStateMachine w, WifiConfigStore s, WifiConnectionStatistics st, WifiNative n) {
        this.mContext = c;
        this.mWifiStateMachine = w;
        this.mWifiConfigStore = s;
        this.mWifiNative = n;
        this.mNetworkScoreCache = null;
        this.mWifiConnectionStatistics = st;
        this.scoreManager = (NetworkScoreManager) this.mContext.getSystemService("network_score");
        if (this.scoreManager == null) {
            logDbg("Registered scoreManager NULL  service network_score");
        }
        if (this.scoreManager != null) {
            this.mNetworkScoreCache = new WifiNetworkScoreCache(this.mContext);
            this.scoreManager.registerNetworkScoreCache(1, this.mNetworkScoreCache);
        } else {
            logDbg("No network score service: Couldnt register as a WiFi score Manager, type=" + Integer.toString(1) + " service network_score");
            this.mNetworkScoreCache = null;
        }
    }

    void enableVerboseLogging(int verbose) {
        if (verbose > 0) {
            DBG = true;
            VDBG = true;
        } else {
            DBG = false;
            VDBG = false;
        }
    }

    private void ageScanResultsOut(int delay) {
        if (delay <= 0) {
            delay = mScanResultMaximumAge;
        }
        long milli = System.currentTimeMillis();
        if (VDBG) {
            logDbg("ageScanResultsOut delay " + Integer.valueOf(delay) + " size " + Integer.valueOf(this.scanResultCache.size()) + " now " + Long.valueOf(milli));
        }
        Iterator<Map.Entry<String, ScanResult>> iter = this.scanResultCache.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, ScanResult> entry = iter.next();
            ScanResult result = entry.getValue();
            if (result.seen + ((long) delay) < milli) {
                iter.remove();
            }
        }
    }

    int addToScanCache(List<ScanResult> scanList) throws Throwable {
        WifiKey wkey;
        int numScanResultsKnown = 0;
        long now = System.currentTimeMillis();
        ArrayList<NetworkKey> unknownScanResults = new ArrayList<>();
        for (ScanResult result : scanList) {
            if (result.SSID != null) {
                result.seen = System.currentTimeMillis();
                ScanResult sr = this.scanResultCache.get(result.BSSID);
                if (sr != null) {
                    if (this.mWifiConfigStore.scanResultRssiLevelPatchUp != 0 && result.level == 0 && sr.level < -20) {
                        result.level = sr.level;
                    }
                    result.averageRssi(sr.level, sr.seen, mScanResultMaximumAge);
                    this.scanResultCache.remove(result.BSSID);
                } else if (this.mWifiConfigStore.scanResultRssiLevelPatchUp != 0 && result.level == 0) {
                    result.level = this.mWifiConfigStore.scanResultRssiLevelPatchUp;
                }
                if (!this.mNetworkScoreCache.isScoredNetwork(result)) {
                    try {
                        wkey = new WifiKey("\"" + result.SSID + "\"", result.BSSID);
                    } catch (IllegalArgumentException e) {
                        logDbg("AutoJoinController: received badly encoded SSID=[" + result.SSID + "] ->skipping this network");
                        wkey = null;
                    }
                    if (wkey != null) {
                        NetworkKey nkey = new NetworkKey(wkey);
                        unknownScanResults.add(nkey);
                    }
                    if (VDBG) {
                        String cap = "";
                        if (result.capabilities != null) {
                            cap = result.capabilities;
                        }
                        logDbg(result.SSID + " " + result.BSSID + " rssi=" + result.level + " cap " + cap + " is not scored");
                    }
                } else if (VDBG) {
                    String cap2 = "";
                    if (result.capabilities != null) {
                        cap2 = result.capabilities;
                    }
                    int score = this.mNetworkScoreCache.getNetworkScore(result);
                    logDbg(result.SSID + " " + result.BSSID + " rssi=" + result.level + " cap " + cap2 + " is scored : " + score);
                }
                this.scanResultCache.put(result.BSSID, result);
                boolean didAssociate = this.mWifiConfigStore.updateSavedNetworkHistory(result);
                if (!didAssociate) {
                    result.untrusted = true;
                    WifiConfiguration associatedConfig = this.mWifiConfigStore.associateWithConfiguration(result);
                    if (associatedConfig != null && associatedConfig.SSID != null) {
                        if (VDBG) {
                            logDbg("addToScanCache save associated config " + associatedConfig.SSID + " with " + result.SSID + " status " + associatedConfig.autoJoinStatus + " reason " + associatedConfig.disableReason + " tsp " + associatedConfig.blackListTimestamp + " was " + (now - associatedConfig.blackListTimestamp));
                        }
                        this.mWifiStateMachine.sendMessage(131218, associatedConfig);
                        didAssociate = true;
                    }
                } else if (now - result.blackListTimestamp > loseBlackListHardMilli) {
                    result.setAutoJoinStatus(0);
                }
                if (didAssociate) {
                    numScanResultsKnown++;
                    result.isAutoJoinCandidate++;
                } else {
                    result.isAutoJoinCandidate = 0;
                }
            }
        }
        if (unknownScanResults.size() != 0) {
            NetworkKey[] newKeys = (NetworkKey[]) unknownScanResults.toArray(new NetworkKey[unknownScanResults.size()]);
            this.scoreManager.requestScores(newKeys);
        }
        return numScanResultsKnown;
    }

    void logDbg(String message) {
        logDbg(message, false);
    }

    void logDbg(String message, boolean stackTrace) {
        if (stackTrace) {
            Log.e(TAG, message + " stack:" + Thread.currentThread().getStackTrace()[2].getMethodName() + " - " + Thread.currentThread().getStackTrace()[3].getMethodName() + " - " + Thread.currentThread().getStackTrace()[4].getMethodName() + " - " + Thread.currentThread().getStackTrace()[5].getMethodName());
        } else {
            Log.e(TAG, message);
        }
    }

    int newSupplicantResults(boolean doAutoJoin) throws Throwable {
        List<ScanResult> scanList = this.mWifiStateMachine.getScanResultsListNoCopyUnsync();
        int numScanResultsKnown = addToScanCache(scanList);
        ageScanResultsOut(mScanResultMaximumAge);
        if (DBG) {
            logDbg("newSupplicantResults size=" + Integer.valueOf(this.scanResultCache.size()) + " known=" + numScanResultsKnown + " " + doAutoJoin);
        }
        if (doAutoJoin) {
            attemptAutoJoin();
        }
        this.mWifiConfigStore.writeKnownNetworkHistory(false);
        return numScanResultsKnown;
    }

    void newHalScanResults() throws Throwable {
        String akm = WifiParser.parse_akm(null, null);
        logDbg(akm);
        addToScanCache(null);
        ageScanResultsOut(0);
        attemptAutoJoin();
        this.mWifiConfigStore.writeKnownNetworkHistory(false);
    }

    void linkQualitySignificantChange() {
        attemptAutoJoin();
    }

    private int compareNetwork(WifiConfiguration candidate, String lastSelectedConfiguration) {
        if (candidate == null) {
            return -3;
        }
        WifiConfiguration currentNetwork = this.mWifiStateMachine.getCurrentWifiConfiguration();
        if (currentNetwork == null) {
            return 1000;
        }
        if (candidate.configKey(true).equals(currentNetwork.configKey(true))) {
            return -2;
        }
        if (DBG) {
            logDbg("compareNetwork will compare " + candidate.configKey() + " with current " + currentNetwork.configKey());
        }
        int order = compareWifiConfigurations(currentNetwork, candidate);
        if (lastSelectedConfiguration != null && currentNetwork.configKey().equals(lastSelectedConfiguration)) {
            int order2 = order - 100;
            if (VDBG) {
                logDbg("     ...and prefers -100 " + currentNetwork.configKey() + " over " + candidate.configKey() + " because it is the last selected -> " + Integer.toString(order2));
                return order2;
            }
            return order2;
        }
        if (lastSelectedConfiguration != null && candidate.configKey().equals(lastSelectedConfiguration)) {
            int order3 = order + 100;
            if (VDBG) {
                logDbg("     ...and prefers +100 " + candidate.configKey() + " over " + currentNetwork.configKey() + " because it is the last selected -> " + Integer.toString(order3));
                return order3;
            }
            return order3;
        }
        return order;
    }

    public void updateConfigurationHistory(int netId, boolean userTriggered, boolean connect) {
        int choice;
        WifiConfiguration selected = this.mWifiConfigStore.getWifiConfiguration(netId);
        if (selected == null) {
            logDbg("updateConfigurationHistory nid=" + netId + " no selected configuration!");
            return;
        }
        if (selected.SSID == null) {
            logDbg("updateConfigurationHistory nid=" + netId + " no SSID in selected configuration!");
            return;
        }
        if (userTriggered) {
            selected.setAutoJoinStatus(0);
            selected.selfAdded = false;
            selected.dirty = true;
        }
        if (DBG && userTriggered) {
            if (selected.connectChoices != null) {
                logDbg("updateConfigurationHistory will update " + Integer.toString(netId) + " now: " + Integer.toString(selected.connectChoices.size()) + " uid=" + Integer.toString(selected.creatorUid), true);
            } else {
                logDbg("updateConfigurationHistory will update " + Integer.toString(netId) + " uid=" + Integer.toString(selected.creatorUid), true);
            }
        }
        if (connect && userTriggered) {
            boolean found = false;
            selected.numUserTriggeredWifiDisableLowRSSI = 0;
            selected.numUserTriggeredWifiDisableBadRSSI = 0;
            selected.numUserTriggeredWifiDisableNotHighRSSI = 0;
            selected.numUserTriggeredJoinAttempts++;
            List<WifiConfiguration> networks = this.mWifiConfigStore.getRecentConfiguredNetworks(12000, false);
            int size = networks != null ? networks.size() : 0;
            logDbg("updateConfigurationHistory found " + size + " networks");
            if (networks != null) {
                for (WifiConfiguration config : networks) {
                    if (DBG) {
                        logDbg("updateConfigurationHistory got " + config.SSID + " nid=" + Integer.toString(config.networkId));
                    }
                    if (selected.configKey(true).equals(config.configKey(true))) {
                        found = true;
                    } else {
                        int order = compareWifiConfigurationsRSSI(config, selected, null);
                        if (order < -30) {
                            choice = 60;
                        } else if (order < -20) {
                            choice = 50;
                        } else if (order < -10) {
                            choice = 40;
                        } else if (order < 20) {
                            choice = 30;
                        } else {
                            choice = 20;
                        }
                        if (selected.connectChoices == null) {
                            selected.connectChoices = new HashMap();
                        }
                        logDbg("updateConfigurationHistory add a choice " + selected.configKey(true) + " over " + config.configKey(true) + " choice " + Integer.toString(choice));
                        Integer currentChoice = (Integer) selected.connectChoices.get(config.configKey(true));
                        if (currentChoice != null) {
                            choice += currentChoice.intValue();
                        }
                        selected.connectChoices.put(config.configKey(true), Integer.valueOf(choice));
                        if (config.connectChoices != null) {
                            if (VDBG) {
                                logDbg("updateConfigurationHistory will remove " + selected.configKey(true) + " from " + config.configKey(true));
                            }
                            config.connectChoices.remove(selected.configKey(true));
                            if (selected.linkedConfigurations != null) {
                                for (String key : selected.linkedConfigurations.keySet()) {
                                    config.connectChoices.remove(key);
                                }
                            }
                        }
                    }
                }
                if (!found) {
                    logDbg("updateConfigurationHistory try to connect to an old network!! : " + selected.configKey());
                }
                if (selected.connectChoices != null && VDBG) {
                    logDbg("updateConfigurationHistory " + Integer.toString(netId) + " now: " + Integer.toString(selected.connectChoices.size()));
                }
            }
        }
        if (userTriggered || connect) {
            this.mWifiConfigStore.writeKnownNetworkHistory(false);
        }
    }

    int getConnectChoice(WifiConfiguration source, WifiConfiguration target) {
        Integer choice = null;
        if (source == null || target == null) {
            return 0;
        }
        if (source.connectChoices != null && source.connectChoices.containsKey(target.configKey(true))) {
            choice = (Integer) source.connectChoices.get(target.configKey(true));
        } else if (source.linkedConfigurations != null) {
            for (String key : source.linkedConfigurations.keySet()) {
                WifiConfiguration config = this.mWifiConfigStore.getWifiConfiguration(key);
                if (config != null && config.connectChoices != null) {
                    choice = (Integer) config.connectChoices.get(target.configKey(true));
                }
            }
        }
        if (choice == null) {
            return 0;
        }
        if (choice.intValue() < 0) {
            choice = 20;
        }
        return choice.intValue();
    }

    int compareWifiConfigurationsFromVisibility(WifiConfiguration a, int aRssiBoost, WifiConfiguration b, int bRssiBoost) {
        int aScore;
        int bScore;
        boolean aPrefers5GHz = false;
        boolean bPrefers5GHz = false;
        int aRssiBoost5 = rssiBoostFrom5GHzRssi(a.visibility.rssi5, a.configKey() + "->");
        int bRssiBoost5 = rssiBoostFrom5GHzRssi(b.visibility.rssi5, b.configKey() + "->");
        if (a.visibility.rssi5 + aRssiBoost5 > a.visibility.rssi24) {
            aPrefers5GHz = true;
        }
        if (b.visibility.rssi5 + bRssiBoost5 > b.visibility.rssi24) {
            bPrefers5GHz = true;
        }
        if (aPrefers5GHz) {
            if (bPrefers5GHz) {
                aScore = a.visibility.rssi5 + aRssiBoost;
            } else {
                aScore = a.visibility.rssi5 + aRssiBoost + aRssiBoost5;
            }
        } else {
            aScore = a.visibility.rssi24 + aRssiBoost;
        }
        if (bPrefers5GHz) {
            if (aPrefers5GHz) {
                bScore = b.visibility.rssi5 + bRssiBoost;
            } else {
                bScore = b.visibility.rssi5 + bRssiBoost + bRssiBoost5;
            }
        } else {
            bScore = b.visibility.rssi24 + bRssiBoost;
        }
        if (VDBG) {
            logDbg("        " + a.configKey() + " is5=" + aPrefers5GHz + " score=" + aScore + " " + b.configKey() + " is5=" + bPrefers5GHz + " score=" + bScore);
        }
        if (a.visibility != null) {
            a.visibility.score = aScore;
            a.visibility.currentNetworkBoost = aRssiBoost;
            a.visibility.bandPreferenceBoost = aRssiBoost5;
        }
        if (b.visibility != null) {
            b.visibility.score = bScore;
            b.visibility.currentNetworkBoost = bRssiBoost;
            b.visibility.bandPreferenceBoost = bRssiBoost5;
        }
        return bScore - aScore;
    }

    int compareWifiConfigurationsRSSI(WifiConfiguration a, WifiConfiguration b, String currentConfiguration) {
        int aRssiBoost = 0;
        int bRssiBoost = 0;
        WifiConfiguration.Visibility astatus = a.visibility;
        WifiConfiguration.Visibility bstatus = b.visibility;
        if (astatus == null || bstatus == null) {
            logDbg("    compareWifiConfigurations NULL band status!");
            return 0;
        }
        if (currentConfiguration != null) {
            if (a.configKey().equals(currentConfiguration)) {
                aRssiBoost = this.mWifiConfigStore.currentNetworkBoost;
            } else if (b.configKey().equals(currentConfiguration)) {
                bRssiBoost = this.mWifiConfigStore.currentNetworkBoost;
            }
        }
        if (VDBG) {
            logDbg("    compareWifiConfigurationsRSSI: " + a.configKey() + " rssi=" + Integer.toString(astatus.rssi24) + "," + Integer.toString(astatus.rssi5) + " boost=" + Integer.toString(aRssiBoost) + " " + b.configKey() + " rssi=" + Integer.toString(bstatus.rssi24) + "," + Integer.toString(bstatus.rssi5) + " boost=" + Integer.toString(bRssiBoost));
        }
        int order = compareWifiConfigurationsFromVisibility(a, aRssiBoost, b, bRssiBoost);
        if (order > 50) {
            order = 50;
        } else if (order < -50) {
            order = -50;
        }
        if (VDBG) {
            String prefer = " = ";
            if (order > 0) {
                prefer = " < ";
            } else if (order < 0) {
                prefer = " > ";
            }
            logDbg("    compareWifiConfigurationsRSSI " + a.configKey() + " rssi=(" + a.visibility.rssi24 + "," + a.visibility.rssi5 + ") num=(" + a.visibility.num24 + "," + a.visibility.num5 + ")" + prefer + b.configKey() + " rssi=(" + b.visibility.rssi24 + "," + b.visibility.rssi5 + ") num=(" + b.visibility.num24 + "," + b.visibility.num5 + ") -> " + order);
        }
        return order;
    }

    int compareWifiConfigurations(WifiConfiguration a, WifiConfiguration b) {
        boolean linked = false;
        if (a.linkedConfigurations != null && b.linkedConfigurations != null && a.autoJoinStatus == 0 && b.autoJoinStatus == 0 && a.linkedConfigurations.get(b.configKey(true)) != null && b.linkedConfigurations.get(a.configKey(true)) != null) {
            linked = true;
        }
        if (a.ephemeral && !b.ephemeral) {
            if (!VDBG) {
                return 1;
            }
            logDbg("    compareWifiConfigurations ephemeral and prefers " + b.configKey() + " over " + a.configKey());
            return 1;
        }
        if (!b.ephemeral || a.ephemeral) {
            int order = 0 + compareWifiConfigurationsRSSI(a, b, this.mCurrentConfigurationKey);
            if (!linked) {
                int choice = getConnectChoice(a, b);
                if (choice > 0) {
                    order -= choice;
                    if (VDBG) {
                        logDbg("    compareWifiConfigurations prefers " + a.configKey() + " over " + b.configKey() + " due to user choice of " + choice + " order -> " + Integer.toString(order));
                    }
                    if (a.visibility != null) {
                        a.visibility.lastChoiceBoost = choice;
                        a.visibility.lastChoiceConfig = b.configKey();
                    }
                }
                int choice2 = getConnectChoice(b, a);
                if (choice2 > 0) {
                    order += choice2;
                    if (VDBG) {
                        logDbg("    compareWifiConfigurations prefers " + b.configKey() + " over " + a.configKey() + " due to user choice of " + choice2 + " order ->" + Integer.toString(order));
                    }
                    if (b.visibility != null) {
                        b.visibility.lastChoiceBoost = choice2;
                        b.visibility.lastChoiceConfig = a.configKey();
                    }
                }
            }
            if (order == 0) {
                if (a.priority > b.priority) {
                    if (VDBG) {
                        logDbg("    compareWifiConfigurations prefers -1 " + a.configKey() + " over " + b.configKey() + " due to priority");
                    }
                    order = -1;
                } else if (a.priority < b.priority) {
                    if (VDBG) {
                        logDbg("    compareWifiConfigurations prefers +1 " + b.configKey() + " over " + a.configKey() + " due to priority");
                    }
                    order = 1;
                }
            }
            String sorder = " == ";
            if (order > 0) {
                sorder = " < ";
            } else if (order < 0) {
                sorder = " > ";
            }
            if (VDBG) {
                logDbg("compareWifiConfigurations: " + a.configKey() + sorder + b.configKey() + " order " + Integer.toString(order));
            }
            return order;
        }
        if (VDBG) {
            logDbg("    compareWifiConfigurations ephemeral and prefers " + a.configKey() + " over " + b.configKey());
        }
        return -1;
    }

    boolean isBadCandidate(int rssi5, int rssi24) {
        return rssi5 < -80 && rssi24 < -90;
    }

    public int rssiBoostFrom5GHzRssi(int rssi, String dbg) {
        if (!this.mWifiConfigStore.enable5GHzPreference) {
            return 0;
        }
        if (rssi > this.mWifiConfigStore.bandPreferenceBoostThreshold5) {
            int boost = this.mWifiConfigStore.bandPreferenceBoostFactor5 * (rssi - this.mWifiConfigStore.bandPreferenceBoostThreshold5);
            if (boost > 50) {
                boost = 50;
            }
            if (VDBG && dbg != null) {
                logDbg("        " + dbg + ":    rssi5 " + rssi + " 5GHz-boost " + boost);
                return boost;
            }
            return boost;
        }
        if (rssi < this.mWifiConfigStore.bandPreferencePenaltyThreshold5) {
            return this.mWifiConfigStore.bandPreferencePenaltyFactor5 * (rssi - this.mWifiConfigStore.bandPreferencePenaltyThreshold5);
        }
        return 0;
    }

    public ScanResult attemptRoam(ScanResult a, WifiConfiguration current, int age, String currentBSSID) {
        if (current == null) {
            if (VDBG) {
                logDbg("attemptRoam not associated");
            }
            return a;
        }
        if (current.scanResultCache == null) {
            if (VDBG) {
                logDbg("attemptRoam no scan cache");
            }
            return a;
        }
        if (current.scanResultCache.size() > 6) {
            if (VDBG) {
                logDbg("attemptRoam scan cache size " + current.scanResultCache.size() + " --> bail");
            }
            return a;
        }
        if (current.BSSID != null && !current.BSSID.equals("any")) {
            if (DBG) {
                logDbg("attemptRoam() BSSID is set " + current.BSSID + " -> bail");
            }
            return a;
        }
        long nowMs = System.currentTimeMillis();
        for (ScanResult b : current.scanResultCache.values()) {
            int bRssiBoost5 = 0;
            int aRssiBoost5 = 0;
            int bRssiBoost = 0;
            int aRssiBoost = 0;
            if (b.seen != 0 && b.BSSID != null && nowMs - b.seen <= age && b.autoJoinStatus == 0 && b.numIpConfigFailures <= 8) {
                if (a == null) {
                    a = b;
                } else if (b.numIpConfigFailures < a.numIpConfigFailures - 1) {
                    logDbg("attemptRoam: " + b.BSSID + " rssi=" + b.level + " ipfail=" + b.numIpConfigFailures + " freq=" + b.frequency + " > " + a.BSSID + " rssi=" + a.level + " ipfail=" + a.numIpConfigFailures + " freq=" + a.frequency);
                    a = b;
                } else {
                    if (currentBSSID != null && currentBSSID.equals(b.BSSID)) {
                        bRssiBoost = b.level <= this.mWifiConfigStore.bandPreferencePenaltyThreshold5 ? this.mWifiConfigStore.associatedHysteresisLow : this.mWifiConfigStore.associatedHysteresisHigh;
                    }
                    if (currentBSSID != null && currentBSSID.equals(a.BSSID)) {
                        aRssiBoost = a.level <= this.mWifiConfigStore.bandPreferencePenaltyThreshold5 ? this.mWifiConfigStore.associatedHysteresisLow : this.mWifiConfigStore.associatedHysteresisHigh;
                    }
                    if (b.is5GHz()) {
                        bRssiBoost5 = rssiBoostFrom5GHzRssi(b.level + bRssiBoost, b.BSSID);
                    }
                    if (a.is5GHz()) {
                        aRssiBoost5 = rssiBoostFrom5GHzRssi(a.level + aRssiBoost, a.BSSID);
                    }
                    if (VDBG) {
                        String comp = " < ";
                        if (b.level + bRssiBoost + bRssiBoost5 > a.level + aRssiBoost + aRssiBoost5) {
                            comp = " > ";
                        }
                        logDbg("attemptRoam: " + b.BSSID + " rssi=" + b.level + " boost=" + Integer.toString(bRssiBoost) + "/" + Integer.toString(bRssiBoost5) + " freq=" + b.frequency + comp + a.BSSID + " rssi=" + a.level + " boost=" + Integer.toString(aRssiBoost) + "/" + Integer.toString(aRssiBoost5) + " freq=" + a.frequency);
                    }
                    if (b.level + bRssiBoost + bRssiBoost5 > a.level + aRssiBoost + aRssiBoost5) {
                        a = b;
                    }
                }
            }
        }
        if (a != null && VDBG) {
            StringBuilder sb = new StringBuilder();
            sb.append("attemptRoam: " + current.configKey() + " Found " + a.BSSID + " rssi=" + a.level + " freq=" + a.frequency);
            if (currentBSSID != null) {
                sb.append(" Current: " + currentBSSID);
            }
            sb.append("\n");
            logDbg(sb.toString());
        }
        return a;
    }

    int getConfigNetworkScore(WifiConfiguration config, int age, boolean isActive) {
        int sc;
        if (this.mNetworkScoreCache == null) {
            if (VDBG) {
                logDbg("       getConfigNetworkScore for " + config.configKey() + "  -> no scorer, hence no scores");
            }
            return WifiNetworkScoreCache.INVALID_NETWORK_SCORE;
        }
        if (config.scanResultCache == null) {
            if (VDBG) {
                logDbg("       getConfigNetworkScore for " + config.configKey() + " -> no scan cache");
            }
            return WifiNetworkScoreCache.INVALID_NETWORK_SCORE;
        }
        long nowMs = System.currentTimeMillis();
        int startScore = -10000;
        for (ScanResult result : config.scanResultCache.values()) {
            if (nowMs - result.seen < age && (sc = this.mNetworkScoreCache.getNetworkScore(result, isActive)) > startScore) {
                startScore = sc;
            }
        }
        if (startScore == -10000) {
            startScore = WifiNetworkScoreCache.INVALID_NETWORK_SCORE;
        }
        if (VDBG) {
            if (startScore == WifiNetworkScoreCache.INVALID_NETWORK_SCORE) {
                logDbg("    getConfigNetworkScore for " + config.configKey() + " -> no available score");
                return startScore;
            }
            logDbg("    getConfigNetworkScore for " + config.configKey() + " isActive=" + isActive + " score = " + Integer.toString(startScore));
            return startScore;
        }
        return startScore;
    }

    void setAllowUntrustedConnections(boolean allow) {
        boolean changed = this.mAllowUntrustedConnections != allow;
        this.mAllowUntrustedConnections = allow;
        if (changed) {
            this.mWifiStateMachine.startScanForUntrustedSettingChange();
        }
    }

    private boolean isOpenNetwork(ScanResult result) {
        return (result.capabilities.contains("WEP") || result.capabilities.contains("PSK") || result.capabilities.contains("EAP")) ? false : true;
    }

    private boolean haveRecentlySeenScoredBssid(WifiConfiguration config) {
        long ephemeralOutOfRangeTimeoutMs = Settings.Global.getLong(this.mContext.getContentResolver(), "wifi_ephemeral_out_of_range_timeout_ms", DEFAULT_EPHEMERAL_OUT_OF_RANGE_TIMEOUT_MS);
        ScanResult currentScanResult = this.mWifiStateMachine.getCurrentScanResult();
        boolean currentNetworkHasScoreCurve = this.mNetworkScoreCache.hasScoreCurve(currentScanResult);
        if (ephemeralOutOfRangeTimeoutMs <= 0 || currentNetworkHasScoreCurve) {
            if (DBG) {
                if (currentNetworkHasScoreCurve) {
                    logDbg("Current network has a score curve, keeping network: " + currentScanResult);
                    return currentNetworkHasScoreCurve;
                }
                logDbg("Current network has no score curve, giving up: " + config.SSID);
                return currentNetworkHasScoreCurve;
            }
            return currentNetworkHasScoreCurve;
        }
        if (config.scanResultCache == null || config.scanResultCache.isEmpty()) {
            return false;
        }
        long currentTimeMs = System.currentTimeMillis();
        for (ScanResult result : config.scanResultCache.values()) {
            if (currentTimeMs > result.seen && currentTimeMs - result.seen < ephemeralOutOfRangeTimeoutMs && this.mNetworkScoreCache.hasScoreCurve(result)) {
                if (DBG) {
                    logDbg("Found scored BSSID, keeping network: " + result.BSSID);
                }
                return true;
            }
        }
        if (DBG) {
            logDbg("No recently scored BSSID found, giving up connection: " + config.SSID);
        }
        return false;
    }

    boolean attemptAutoJoin() {
        char c;
        boolean found = false;
        this.didOverride = false;
        this.didBailDueToWeakRssi = false;
        int networkSwitchType = 0;
        long now = System.currentTimeMillis();
        String lastSelectedConfiguration = this.mWifiConfigStore.getLastSelectedConfiguration();
        this.mCurrentConfigurationKey = null;
        WifiConfiguration currentConfiguration = this.mWifiStateMachine.getCurrentWifiConfiguration();
        WifiConfiguration candidate = null;
        List<WifiConfiguration> list = this.mWifiConfigStore.getRecentConfiguredNetworks(mScanResultAutoJoinAge, false);
        if (list == null) {
            if (VDBG) {
                logDbg("attemptAutoJoin nothing known=" + this.mWifiConfigStore.getconfiguredNetworkSize());
            }
            return false;
        }
        String val = this.mWifiNative.status(true);
        String[] status = val.split("\\r?\\n");
        if (VDBG) {
            logDbg("attemptAutoJoin() status=" + val + " split=" + Integer.toString(status.length));
        }
        int supplicantNetId = -1;
        for (String key : status) {
            if (key.regionMatches(0, "id=", 0, 3)) {
                supplicantNetId = 0;
                for (int idx = 3; idx < key.length() && (c = key.charAt(idx)) >= '0' && c <= '9'; idx++) {
                    supplicantNetId = (supplicantNetId * 10) + (c - '0');
                }
            } else if (key.contains("wpa_state=ASSOCIATING") || key.contains("wpa_state=ASSOCIATED") || key.contains("wpa_state=FOUR_WAY_HANDSHAKE") || key.contains("wpa_state=GROUP_KEY_HANDSHAKE")) {
                if (DBG) {
                    logDbg("attemptAutoJoin: bail out due to sup state " + key);
                }
                return false;
            }
        }
        if (DBG) {
            String conf = "";
            String last = "";
            if (currentConfiguration != null) {
                conf = " current=" + currentConfiguration.configKey();
            }
            if (lastSelectedConfiguration != null) {
                last = " last=" + lastSelectedConfiguration;
            }
            logDbg("attemptAutoJoin() num recent config " + Integer.toString(list.size()) + conf + last + " ---> suppNetId=" + Integer.toString(supplicantNetId));
        }
        if (currentConfiguration != null) {
            if (supplicantNetId != currentConfiguration.networkId && supplicantNetId != -1 && currentConfiguration.networkId != -1) {
                logDbg("attemptAutoJoin() ERROR wpa_supplicant out of sync nid=" + Integer.toString(supplicantNetId) + " WifiStateMachine=" + Integer.toString(currentConfiguration.networkId));
                this.mWifiStateMachine.disconnectCommand();
                return false;
            }
            if (currentConfiguration.ephemeral && (!this.mAllowUntrustedConnections || !haveRecentlySeenScoredBssid(currentConfiguration))) {
                logDbg("attemptAutoJoin() disconnecting from unwanted ephemeral network");
                this.mWifiStateMachine.disconnectCommand(1010, this.mAllowUntrustedConnections ? 1 : 0);
                return false;
            }
            this.mCurrentConfigurationKey = currentConfiguration.configKey();
        } else if (supplicantNetId != -1) {
            return false;
        }
        int currentNetId = -1;
        if (currentConfiguration != null) {
            currentNetId = currentConfiguration.networkId;
        }
        Iterator<WifiConfiguration> it = list.iterator();
        while (it.hasNext()) {
            WifiConfiguration config = it.next();
            if (config.SSID != null) {
                if (config.autoJoinStatus >= 128) {
                    if (config.disableReason == 2 || config.disableReason == 4 || config.disableReason == 3) {
                        if (config.blackListTimestamp == 0 || config.blackListTimestamp > now) {
                            config.blackListTimestamp = now;
                        }
                        if (now - config.blackListTimestamp > this.mWifiConfigStore.wifiConfigBlacklistMinTimeMilli) {
                            config.status = 2;
                            config.numConnectionFailures = 0;
                            config.numIpConfigFailures = 0;
                            config.numAuthFailures = 0;
                            config.setAutoJoinStatus(0);
                            config.dirty = true;
                        } else if (VDBG) {
                            long delay = ((long) this.mWifiConfigStore.wifiConfigBlacklistMinTimeMilli) - (now - config.blackListTimestamp);
                            logDbg("attemptautoJoin " + config.configKey() + " dont unblacklist yet, waiting for " + delay + " ms");
                        }
                    }
                    if (DBG) {
                        logDbg("attemptAutoJoin skip candidate due to auto join status " + Integer.toString(config.autoJoinStatus) + " key " + config.configKey(true) + " reason " + config.disableReason);
                    }
                } else {
                    if (config.blackListTimestamp > 0) {
                        if (now < config.blackListTimestamp || now - config.blackListTimestamp > loseBlackListHardMilli) {
                            config.setAutoJoinStatus(0);
                        } else if (now - config.blackListTimestamp > loseBlackListSoftMilli) {
                            config.setAutoJoinStatus(config.autoJoinStatus - 8);
                        }
                    }
                    if (config.visibility.rssi5 < this.mWifiConfigStore.thresholdUnblacklistThreshold5Soft && config.visibility.rssi24 < this.mWifiConfigStore.thresholdUnblacklistThreshold24Soft) {
                        if (DBG) {
                            logDbg("attemptAutoJoin do not unblacklist due to low visibility " + config.autoJoinStatus + " key " + config.configKey(true) + " rssi=(" + config.visibility.rssi24 + "," + config.visibility.rssi5 + ") num=(" + config.visibility.num24 + "," + config.visibility.num5 + ")");
                        }
                    } else if (config.visibility.rssi5 < this.mWifiConfigStore.thresholdUnblacklistThreshold5Hard && config.visibility.rssi24 < this.mWifiConfigStore.thresholdUnblacklistThreshold24Hard) {
                        config.setAutoJoinStatus(config.autoJoinStatus - 1);
                        if (DBG) {
                            logDbg("attemptAutoJoin good candidate seen, bumped soft -> status=" + config.autoJoinStatus + " " + config.configKey(true) + " rssi=(" + config.visibility.rssi24 + "," + config.visibility.rssi5 + ") num=(" + config.visibility.num24 + "," + config.visibility.num5 + ")");
                        }
                    } else {
                        config.setAutoJoinStatus(config.autoJoinStatus - 3);
                        if (DBG) {
                            logDbg("attemptAutoJoin good candidate seen, bumped hard -> status=" + config.autoJoinStatus + " " + config.configKey(true) + " rssi=(" + config.visibility.rssi24 + "," + config.visibility.rssi5 + ") num=(" + config.visibility.num24 + "," + config.visibility.num5 + ")");
                        }
                    }
                    if (config.autoJoinStatus >= 1) {
                        if (DBG) {
                            logDbg("attemptAutoJoin skip blacklisted -> status=" + config.autoJoinStatus + " " + config.configKey(true) + " rssi=(" + config.visibility.rssi24 + "," + config.visibility.rssi5 + ") num=(" + config.visibility.num24 + "," + config.visibility.num5 + ")");
                        }
                    } else if (config.networkId == currentNetId) {
                        if (DBG) {
                            logDbg("attemptAutoJoin skip current candidate  " + Integer.toString(currentNetId) + " key " + config.configKey(true));
                        }
                    } else {
                        boolean isLastSelected = false;
                        if (lastSelectedConfiguration != null && config.configKey().equals(lastSelectedConfiguration)) {
                            isLastSelected = true;
                        }
                        if (config.visibility != null) {
                            if (config.lastRoamingFailure != 0 && currentConfiguration != null && ((lastSelectedConfiguration == null || !config.configKey().equals(lastSelectedConfiguration)) && now > config.lastRoamingFailure && now - config.lastRoamingFailure < config.roamingFailureBlackListTimeMilli)) {
                                if (DBG) {
                                    logDbg("compareNetwork not switching to " + config.configKey() + " from current " + currentConfiguration.configKey() + " because it is blacklisted due to roam failure,  blacklist remain time = " + (now - config.lastRoamingFailure) + " ms");
                                }
                            } else {
                                int boost = config.autoJoinUseAggressiveJoinAttemptThreshold + this.weakRssiBailCount;
                                if (config.visibility.rssi5 + boost < this.mWifiConfigStore.thresholdInitialAutoJoinAttemptMin5RSSI && config.visibility.rssi24 + boost < this.mWifiConfigStore.thresholdInitialAutoJoinAttemptMin24RSSI) {
                                    if (DBG) {
                                        logDbg("attemptAutoJoin skip due to low visibility -> status=" + config.autoJoinStatus + " key " + config.configKey(true) + " rssi=" + config.visibility.rssi24 + ", " + config.visibility.rssi5 + " num=" + config.visibility.num24 + ", " + config.visibility.num5);
                                    }
                                    if (!isLastSelected) {
                                        config.autoJoinBailedDueToLowRssi = true;
                                        this.didBailDueToWeakRssi = true;
                                    } else if (config.autoJoinUseAggressiveJoinAttemptThreshold < WifiConfiguration.MAX_INITIAL_AUTO_JOIN_RSSI_BOOST && config.autoJoinBailedDueToLowRssi) {
                                        config.autoJoinUseAggressiveJoinAttemptThreshold += 4;
                                    }
                                }
                                if (config.numNoInternetAccessReports > 0 && !isLastSelected && !config.validatedInternetAccess) {
                                    if (DBG) {
                                        logDbg("attemptAutoJoin skip candidate due to no InternetAccess  " + config.configKey(true) + " num reports " + config.numNoInternetAccessReports);
                                    }
                                } else {
                                    if (DBG) {
                                        String cur = "";
                                        if (candidate != null) {
                                            cur = " current candidate " + candidate.configKey();
                                        }
                                        logDbg("attemptAutoJoin trying id=" + Integer.toString(config.networkId) + " " + config.configKey(true) + " status=" + config.autoJoinStatus + cur);
                                    }
                                    if (candidate == null) {
                                        candidate = config;
                                    } else {
                                        if (VDBG) {
                                            logDbg("attemptAutoJoin will compare candidate  " + candidate.configKey() + " with " + config.configKey());
                                        }
                                        int order = compareWifiConfigurations(candidate, config);
                                        if (lastSelectedConfiguration != null && candidate.configKey().equals(lastSelectedConfiguration)) {
                                            order -= 100;
                                            if (VDBG) {
                                                logDbg("     ...and prefers -100 " + candidate.configKey() + " over " + config.configKey() + " because it is the last selected -> " + Integer.toString(order));
                                            }
                                        } else if (lastSelectedConfiguration != null && config.configKey().equals(lastSelectedConfiguration)) {
                                            order += 100;
                                            if (VDBG) {
                                                logDbg("     ...and prefers +100 " + config.configKey() + " over " + candidate.configKey() + " because it is the last selected -> " + Integer.toString(order));
                                            }
                                        }
                                        if (order > 0) {
                                            candidate = config;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (this.mNetworkScoreCache != null && this.mAllowUntrustedConnections) {
            int rssi5 = WifiConfiguration.INVALID_RSSI;
            int rssi24 = WifiConfiguration.INVALID_RSSI;
            if (candidate != null) {
                rssi5 = candidate.visibility.rssi5;
                rssi24 = candidate.visibility.rssi24;
            }
            long nowMs = System.currentTimeMillis();
            int currentScore = -10000;
            ScanResult untrustedCandidate = null;
            if (isBadCandidate(rssi24, rssi5)) {
                for (ScanResult result : this.scanResultCache.values()) {
                    if (!TextUtils.isEmpty(result.SSID) && result.untrusted && isOpenNetwork(result)) {
                        String quotedSSID = "\"" + result.SSID + "\"";
                        if (!this.mWifiConfigStore.mDeletedEphemeralSSIDs.contains(quotedSSID) && nowMs - result.seen < mScanResultAutoJoinAge) {
                            this.mWifiConnectionStatistics.incrementOrAddUntrusted(quotedSSID, 0, 1);
                            boolean isActiveNetwork = currentConfiguration != null && currentConfiguration.SSID.equals(quotedSSID);
                            int score = this.mNetworkScoreCache.getNetworkScore(result, isActiveNetwork);
                            if (score != WifiNetworkScoreCache.INVALID_NETWORK_SCORE && score > currentScore) {
                                currentScore = score;
                                untrustedCandidate = result;
                                if (VDBG) {
                                    logDbg("AutoJoinController: found untrusted candidate " + result.SSID + " RSSI=" + result.level + " freq=" + result.frequency + " score=" + score);
                                }
                            }
                        }
                    }
                }
            }
            if (untrustedCandidate != null) {
                candidate = this.mWifiConfigStore.wifiConfigurationFromScanResult(untrustedCandidate);
                candidate.allowedKeyManagement.set(0);
                candidate.ephemeral = true;
            }
        }
        long lastUnwanted = System.currentTimeMillis() - this.mWifiConfigStore.lastUnwantedNetworkDisconnectTimestamp;
        if (candidate == null && lastSelectedConfiguration == null && currentConfiguration == null && this.didBailDueToWeakRssi && (this.mWifiConfigStore.lastUnwantedNetworkDisconnectTimestamp == 0 || lastUnwanted > 604800000)) {
            if (this.weakRssiBailCount < 10) {
                this.weakRssiBailCount++;
            }
        } else if (this.weakRssiBailCount > 0) {
            this.weakRssiBailCount--;
        }
        int networkDelta = compareNetwork(candidate, lastSelectedConfiguration);
        if (DBG && candidate != null) {
            String doSwitch = "";
            String current = "";
            if (networkDelta < 0) {
                doSwitch = " -> not switching";
            }
            if (currentConfiguration != null) {
                current = " with current " + currentConfiguration.configKey();
            }
            logDbg("attemptAutoJoin networkSwitching candidate " + candidate.configKey() + current + " linked=" + (currentConfiguration != null && currentConfiguration.isLinked(candidate)) + " : delta=" + Integer.toString(networkDelta) + " " + doSwitch);
        }
        if (this.mWifiStateMachine.shouldSwitchNetwork(networkDelta)) {
            if (currentConfiguration != null && currentConfiguration.isLinked(candidate)) {
                networkSwitchType = 2;
            } else {
                networkSwitchType = 3;
            }
            if (DBG) {
                logDbg("AutoJoin auto connect with netId " + Integer.toString(candidate.networkId) + " to " + candidate.configKey());
            }
            if (this.didOverride) {
                candidate.numScorerOverrideAndSwitchedNetwork++;
            }
            candidate.numAssociation++;
            this.mWifiConnectionStatistics.numAutoJoinAttempt++;
            if (candidate.ephemeral) {
                this.mWifiConnectionStatistics.incrementOrAddUntrusted(candidate.SSID, 1, 0);
            }
            if (candidate.BSSID == null || candidate.BSSID.equals("any")) {
                String currentBSSID = this.mWifiStateMachine.getCurrentBSSID();
                ScanResult roamCandidate = attemptRoam(null, candidate, mScanResultAutoJoinAge, null);
                if (roamCandidate != null && currentBSSID != null && currentBSSID.equals(roamCandidate.BSSID)) {
                    roamCandidate = null;
                }
                if (roamCandidate != null && roamCandidate.is5GHz()) {
                    candidate.autoJoinBSSID = roamCandidate.BSSID;
                    if (VDBG) {
                        logDbg("AutoJoinController: lock to 5GHz " + candidate.autoJoinBSSID + " RSSI=" + roamCandidate.level + " freq=" + roamCandidate.frequency);
                    }
                } else {
                    candidate.autoJoinBSSID = "any";
                }
            }
            this.mWifiStateMachine.sendMessage(131215, candidate.networkId, networkSwitchType, candidate);
            found = true;
        }
        if (networkSwitchType == 0) {
            String currentBSSID2 = this.mWifiStateMachine.getCurrentBSSID();
            ScanResult roamCandidate2 = attemptRoam(null, currentConfiguration, mScanResultAutoJoinAge, currentBSSID2);
            if (roamCandidate2 != null && currentBSSID2 != null && currentBSSID2.equals(roamCandidate2.BSSID)) {
                roamCandidate2 = null;
            }
            if (roamCandidate2 != null && this.mWifiStateMachine.shouldSwitchNetwork(999)) {
                if (DBG) {
                    logDbg("AutoJoin auto roam with netId " + Integer.toString(currentConfiguration.networkId) + " " + currentConfiguration.configKey() + " to BSSID=" + roamCandidate2.BSSID + " freq=" + roamCandidate2.frequency + " RSSI=" + roamCandidate2.level);
                }
                networkSwitchType = 1;
                this.mWifiConnectionStatistics.numAutoRoamAttempt++;
                this.mWifiStateMachine.sendMessage(131217, currentConfiguration.networkId, 1, roamCandidate2);
                found = true;
            }
        }
        if (VDBG) {
            logDbg("Done attemptAutoJoin status=" + Integer.toString(networkSwitchType));
        }
        return found;
    }
}
