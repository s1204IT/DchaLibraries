package com.android.server.wifi;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiSsid;
import android.os.Environment;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;
import com.android.server.net.DelayedDiskWrite;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WifiNetworkHistory {
    private static final String AUTH_KEY = "AUTH";
    private static final String BSSID_KEY = "BSSID";
    private static final String BSSID_KEY_END = "/BSSID";
    private static final String BSSID_STATUS_KEY = "BSSID_STATUS";
    private static final String CHOICE_KEY = "CHOICE";
    private static final String CHOICE_TIME_KEY = "CHOICE_TIME";
    private static final String CONFIG_BSSID_KEY = "CONFIG_BSSID";
    static final String CONFIG_KEY = "CONFIG";
    private static final String CONNECT_UID_KEY = "CONNECT_UID_KEY";
    private static final String CREATION_TIME_KEY = "CREATION_TIME";
    private static final String CREATOR_NAME_KEY = "CREATOR_NAME";
    static final String CREATOR_UID_KEY = "CREATOR_UID_KEY";
    private static final String DATE_KEY = "DATE";
    private static final boolean DBG = true;
    private static final String DEFAULT_GW_KEY = "DEFAULT_GW";
    private static final String DELETED_EPHEMERAL_KEY = "DELETED_EPHEMERAL";
    private static final String DID_SELF_ADD_KEY = "DID_SELF_ADD";
    private static final String EPHEMERAL_KEY = "EPHEMERAL";
    private static final String FAILURE_KEY = "FAILURE";
    private static final String FQDN_KEY = "FQDN";
    private static final String FREQ_KEY = "FREQ";
    private static final String HAS_EVER_CONNECTED_KEY = "HAS_EVER_CONNECTED";
    private static final String LINK_KEY = "LINK";
    private static final String METERED_HINT_KEY = "METERED_HINT";
    private static final String MILLI_KEY = "MILLI";
    static final String NETWORK_HISTORY_CONFIG_FILE = Environment.getDataDirectory() + "/misc/wifi/networkHistory.txt";
    private static final String NETWORK_ID_KEY = "ID";
    private static final String NETWORK_SELECTION_DISABLE_REASON_KEY = "NETWORK_SELECTION_DISABLE_REASON";
    private static final String NETWORK_SELECTION_STATUS_KEY = "NETWORK_SELECTION_STATUS";
    private static final String NL = "\n";
    private static final String NO_INTERNET_ACCESS_EXPECTED_KEY = "NO_INTERNET_ACCESS_EXPECTED";
    private static final String NO_INTERNET_ACCESS_REPORTS_KEY = "NO_INTERNET_ACCESS_REPORTS";
    private static final String NUM_ASSOCIATION_KEY = "NUM_ASSOCIATION";
    private static final String PEER_CONFIGURATION_KEY = "PEER_CONFIGURATION";
    private static final String PRIORITY_KEY = "PRIORITY";
    private static final String RSSI_KEY = "RSSI";
    private static final String SCORER_OVERRIDE_AND_SWITCH_KEY = "SCORER_OVERRIDE_AND_SWITCH";
    private static final String SCORER_OVERRIDE_KEY = "SCORER_OVERRIDE";
    private static final String SELF_ADDED_KEY = "SELF_ADDED";
    private static final String SEPARATOR = ":  ";
    static final String SHARED_KEY = "SHARED";
    private static final String SSID_KEY = "SSID";
    public static final String TAG = "WifiNetworkHistory";
    private static final String UPDATE_NAME_KEY = "UPDATE_NAME";
    private static final String UPDATE_TIME_KEY = "UPDATE_TIME";
    private static final String UPDATE_UID_KEY = "UPDATE_UID";
    private static final String USER_APPROVED_KEY = "USER_APPROVED";
    private static final String USE_EXTERNAL_SCORES_KEY = "USE_EXTERNAL_SCORES";
    private static final String VALIDATED_INTERNET_ACCESS_KEY = "VALIDATED_INTERNET_ACCESS";
    private static final boolean VDBG = true;
    Context mContext;
    private final LocalLog mLocalLog;
    HashSet<String> mLostConfigsDbg = new HashSet<>();
    protected final DelayedDiskWrite mWriter;

    public WifiNetworkHistory(Context c, LocalLog localLog, DelayedDiskWrite writer) {
        this.mContext = c;
        this.mWriter = writer;
        this.mLocalLog = localLog;
    }

    public void writeKnownNetworkHistory(final List<WifiConfiguration> networks, final ConcurrentHashMap<Integer, ScanDetailCache> scanDetailCaches, final Set<String> deletedEphemeralSSIDs) {
        this.mWriter.write(NETWORK_HISTORY_CONFIG_FILE, new DelayedDiskWrite.Writer() {
            public void onWriteCalled(DataOutputStream out) throws IOException {
                for (WifiConfiguration config : networks) {
                    WifiConfiguration.NetworkSelectionStatus status = config.getNetworkSelectionStatus();
                    int numlink = config.linkedConfigurations != null ? config.linkedConfigurations.size() : 0;
                    String disableTime = config.getNetworkSelectionStatus().isNetworkEnabled() ? "" : "Disable time: " + DateFormat.getInstance().format(Long.valueOf(config.getNetworkSelectionStatus().getDisableTime()));
                    WifiNetworkHistory.this.logd("saving network history: " + config.configKey() + " gw: " + config.defaultGwMacAddress + " Network Selection-status: " + status.getNetworkStatusString() + disableTime + " ephemeral=" + config.ephemeral + " choice:" + status.getConnectChoice() + " link:" + numlink + " status:" + config.status + " nid:" + config.networkId + " hasEverConnected: " + status.getHasEverConnected());
                    if (WifiNetworkHistory.this.isValid(config)) {
                        if (config.SSID == null) {
                            WifiNetworkHistory.this.logv("writeKnownNetworkHistory trying to write config with null SSID");
                        } else {
                            WifiNetworkHistory.this.logv("writeKnownNetworkHistory write config " + config.configKey());
                            out.writeUTF("CONFIG:  " + config.configKey() + WifiNetworkHistory.NL);
                            if (config.SSID != null) {
                                out.writeUTF("SSID:  " + config.SSID + WifiNetworkHistory.NL);
                            }
                            if (config.BSSID != null) {
                                out.writeUTF("CONFIG_BSSID:  " + config.BSSID + WifiNetworkHistory.NL);
                            } else {
                                out.writeUTF("CONFIG_BSSID:  null\n");
                            }
                            if (config.FQDN != null) {
                                out.writeUTF("FQDN:  " + config.FQDN + WifiNetworkHistory.NL);
                            }
                            out.writeUTF("PRIORITY:  " + Integer.toString(config.priority) + WifiNetworkHistory.NL);
                            out.writeUTF("ID:  " + Integer.toString(config.networkId) + WifiNetworkHistory.NL);
                            out.writeUTF("SELF_ADDED:  " + Boolean.toString(config.selfAdded) + WifiNetworkHistory.NL);
                            out.writeUTF("DID_SELF_ADD:  " + Boolean.toString(config.didSelfAdd) + WifiNetworkHistory.NL);
                            out.writeUTF("NO_INTERNET_ACCESS_REPORTS:  " + Integer.toString(config.numNoInternetAccessReports) + WifiNetworkHistory.NL);
                            out.writeUTF("VALIDATED_INTERNET_ACCESS:  " + Boolean.toString(config.validatedInternetAccess) + WifiNetworkHistory.NL);
                            out.writeUTF("NO_INTERNET_ACCESS_EXPECTED:  " + Boolean.toString(config.noInternetAccessExpected) + WifiNetworkHistory.NL);
                            out.writeUTF("EPHEMERAL:  " + Boolean.toString(config.ephemeral) + WifiNetworkHistory.NL);
                            out.writeUTF("METERED_HINT:  " + Boolean.toString(config.meteredHint) + WifiNetworkHistory.NL);
                            out.writeUTF("USE_EXTERNAL_SCORES:  " + Boolean.toString(config.useExternalScores) + WifiNetworkHistory.NL);
                            if (config.creationTime != null) {
                                out.writeUTF("CREATION_TIME:  " + config.creationTime + WifiNetworkHistory.NL);
                            }
                            if (config.updateTime != null) {
                                out.writeUTF("UPDATE_TIME:  " + config.updateTime + WifiNetworkHistory.NL);
                            }
                            if (config.peerWifiConfiguration != null) {
                                out.writeUTF("PEER_CONFIGURATION:  " + config.peerWifiConfiguration + WifiNetworkHistory.NL);
                            }
                            out.writeUTF("SCORER_OVERRIDE:  " + Integer.toString(config.numScorerOverride) + WifiNetworkHistory.NL);
                            out.writeUTF("SCORER_OVERRIDE_AND_SWITCH:  " + Integer.toString(config.numScorerOverrideAndSwitchedNetwork) + WifiNetworkHistory.NL);
                            out.writeUTF("NUM_ASSOCIATION:  " + Integer.toString(config.numAssociation) + WifiNetworkHistory.NL);
                            out.writeUTF("CREATOR_UID_KEY:  " + Integer.toString(config.creatorUid) + WifiNetworkHistory.NL);
                            out.writeUTF("CONNECT_UID_KEY:  " + Integer.toString(config.lastConnectUid) + WifiNetworkHistory.NL);
                            out.writeUTF("UPDATE_UID:  " + Integer.toString(config.lastUpdateUid) + WifiNetworkHistory.NL);
                            out.writeUTF("CREATOR_NAME:  " + config.creatorName + WifiNetworkHistory.NL);
                            out.writeUTF("UPDATE_NAME:  " + config.lastUpdateName + WifiNetworkHistory.NL);
                            out.writeUTF("USER_APPROVED:  " + Integer.toString(config.userApproved) + WifiNetworkHistory.NL);
                            out.writeUTF("SHARED:  " + Boolean.toString(config.shared) + WifiNetworkHistory.NL);
                            String allowedKeyManagementString = WifiNetworkHistory.makeString(config.allowedKeyManagement, WifiConfiguration.KeyMgmt.strings);
                            out.writeUTF("AUTH:  " + allowedKeyManagementString + WifiNetworkHistory.NL);
                            out.writeUTF("NETWORK_SELECTION_STATUS:  " + status.getNetworkSelectionStatus() + WifiNetworkHistory.NL);
                            out.writeUTF("NETWORK_SELECTION_DISABLE_REASON:  " + status.getNetworkSelectionDisableReason() + WifiNetworkHistory.NL);
                            if (status.getConnectChoice() != null) {
                                out.writeUTF("CHOICE:  " + status.getConnectChoice() + WifiNetworkHistory.NL);
                                out.writeUTF("CHOICE_TIME:  " + status.getConnectChoiceTimestamp() + WifiNetworkHistory.NL);
                            }
                            if (config.linkedConfigurations != null) {
                                WifiNetworkHistory.this.log("writeKnownNetworkHistory write linked " + config.linkedConfigurations.size());
                                for (String key : config.linkedConfigurations.keySet()) {
                                    out.writeUTF("LINK:  " + key + WifiNetworkHistory.NL);
                                }
                            }
                            String macAddress = config.defaultGwMacAddress;
                            if (macAddress != null) {
                                out.writeUTF("DEFAULT_GW:  " + macAddress + WifiNetworkHistory.NL);
                            }
                            if (WifiNetworkHistory.this.getScanDetailCache(config, scanDetailCaches) != null) {
                                for (ScanDetail scanDetail : WifiNetworkHistory.this.getScanDetailCache(config, scanDetailCaches).values()) {
                                    ScanResult result = scanDetail.getScanResult();
                                    out.writeUTF("BSSID:  " + result.BSSID + WifiNetworkHistory.NL);
                                    out.writeUTF("FREQ:  " + Integer.toString(result.frequency) + WifiNetworkHistory.NL);
                                    out.writeUTF("RSSI:  " + Integer.toString(result.level) + WifiNetworkHistory.NL);
                                    out.writeUTF("/BSSID\n");
                                }
                            }
                            if (config.lastFailure != null) {
                                out.writeUTF("FAILURE:  " + config.lastFailure + WifiNetworkHistory.NL);
                            }
                            out.writeUTF("HAS_EVER_CONNECTED:  " + Boolean.toString(status.getHasEverConnected()) + WifiNetworkHistory.NL);
                            out.writeUTF(WifiNetworkHistory.NL);
                            out.writeUTF(WifiNetworkHistory.NL);
                            out.writeUTF(WifiNetworkHistory.NL);
                        }
                    }
                }
                if (deletedEphemeralSSIDs == null || deletedEphemeralSSIDs.size() <= 0) {
                    return;
                }
                for (String ssid : deletedEphemeralSSIDs) {
                    out.writeUTF(WifiNetworkHistory.DELETED_EPHEMERAL_KEY);
                    out.writeUTF(ssid);
                    out.writeUTF(WifiNetworkHistory.NL);
                }
            }
        });
    }

    public void readNetworkHistory(Map<String, WifiConfiguration> configs, ConcurrentHashMap<Integer, ScanDetailCache> scanDetailCaches, Set<String> deletedEphemeralSSIDs) throws Throwable {
        Throwable th;
        localLog("readNetworkHistory() path:" + NETWORK_HISTORY_CONFIG_FILE);
        Throwable th2 = null;
        DataInputStream dataInputStream = null;
        try {
            DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(NETWORK_HISTORY_CONFIG_FILE)));
            String bssid = null;
            String ssid = null;
            int freq = 0;
            long seen = 0;
            try {
                int rssi = WifiConfiguration.INVALID_RSSI;
                String caps = null;
                WifiConfiguration config = null;
                while (true) {
                    String line = in.readUTF();
                    if (line == null) {
                        break;
                    }
                    int colon = line.indexOf(58);
                    if (colon >= 0) {
                        String key = line.substring(0, colon).trim();
                        String value = line.substring(colon + 1).trim();
                        if (key.equals(CONFIG_KEY)) {
                            config = configs.get(value);
                            if (config == null) {
                                localLog("readNetworkHistory didnt find netid for hash=" + Integer.toString(value.hashCode()) + " key: " + value);
                                this.mLostConfigsDbg.add(value);
                            } else if (config.creatorName == null || config.lastUpdateName == null) {
                                config.creatorName = this.mContext.getPackageManager().getNameForUid(1000);
                                config.lastUpdateName = config.creatorName;
                                Log.w(TAG, "Upgrading network " + config.networkId + " to " + config.creatorName);
                            }
                        } else if (config != null) {
                            WifiConfiguration.NetworkSelectionStatus networkStatus = config.getNetworkSelectionStatus();
                            if (key.equals("SSID")) {
                                if (!config.isPasspoint()) {
                                    ssid = value;
                                    if (config.SSID == null || config.SSID.equals(value)) {
                                        config.SSID = value;
                                    } else {
                                        loge("Error parsing network history file, mismatched SSIDs");
                                        config = null;
                                        ssid = null;
                                    }
                                }
                            } else if (key.equals(CONFIG_BSSID_KEY)) {
                                if (value.equals("null")) {
                                    value = null;
                                }
                                config.BSSID = value;
                            } else if (key.equals("FQDN")) {
                                if (value.equals("null")) {
                                    value = null;
                                }
                                config.FQDN = value;
                            } else if (key.equals(DEFAULT_GW_KEY)) {
                                config.defaultGwMacAddress = value;
                            } else if (key.equals(SELF_ADDED_KEY)) {
                                config.selfAdded = Boolean.parseBoolean(value);
                            } else if (key.equals(DID_SELF_ADD_KEY)) {
                                config.didSelfAdd = Boolean.parseBoolean(value);
                            } else if (key.equals(NO_INTERNET_ACCESS_REPORTS_KEY)) {
                                config.numNoInternetAccessReports = Integer.parseInt(value);
                            } else if (key.equals(VALIDATED_INTERNET_ACCESS_KEY)) {
                                config.validatedInternetAccess = Boolean.parseBoolean(value);
                            } else if (key.equals(NO_INTERNET_ACCESS_EXPECTED_KEY)) {
                                config.noInternetAccessExpected = Boolean.parseBoolean(value);
                            } else if (key.equals(CREATION_TIME_KEY)) {
                                config.creationTime = value;
                            } else if (key.equals(UPDATE_TIME_KEY)) {
                                config.updateTime = value;
                            } else if (key.equals(EPHEMERAL_KEY)) {
                                config.ephemeral = Boolean.parseBoolean(value);
                            } else if (key.equals(METERED_HINT_KEY)) {
                                config.meteredHint = Boolean.parseBoolean(value);
                            } else if (key.equals(USE_EXTERNAL_SCORES_KEY)) {
                                config.useExternalScores = Boolean.parseBoolean(value);
                            } else if (key.equals(CREATOR_UID_KEY)) {
                                config.creatorUid = Integer.parseInt(value);
                            } else if (key.equals(SCORER_OVERRIDE_KEY)) {
                                config.numScorerOverride = Integer.parseInt(value);
                            } else if (key.equals(SCORER_OVERRIDE_AND_SWITCH_KEY)) {
                                config.numScorerOverrideAndSwitchedNetwork = Integer.parseInt(value);
                            } else if (key.equals(NUM_ASSOCIATION_KEY)) {
                                config.numAssociation = Integer.parseInt(value);
                            } else if (key.equals(CONNECT_UID_KEY)) {
                                config.lastConnectUid = Integer.parseInt(value);
                            } else if (key.equals(UPDATE_UID_KEY)) {
                                config.lastUpdateUid = Integer.parseInt(value);
                            } else if (key.equals(FAILURE_KEY)) {
                                config.lastFailure = value;
                            } else if (key.equals(PEER_CONFIGURATION_KEY)) {
                                config.peerWifiConfiguration = value;
                            } else if (key.equals(NETWORK_SELECTION_STATUS_KEY)) {
                                int networkStatusValue = Integer.parseInt(value);
                                if (networkStatusValue == 1) {
                                    networkStatusValue = 0;
                                }
                                networkStatus.setNetworkSelectionStatus(networkStatusValue);
                            } else if (key.equals(NETWORK_SELECTION_DISABLE_REASON_KEY)) {
                                networkStatus.setNetworkSelectionDisableReason(Integer.parseInt(value));
                            } else if (key.equals(CHOICE_KEY)) {
                                networkStatus.setConnectChoice(value);
                            } else if (key.equals(CHOICE_TIME_KEY)) {
                                networkStatus.setConnectChoiceTimestamp(Long.parseLong(value));
                            } else if (key.equals(LINK_KEY)) {
                                if (config.linkedConfigurations == null) {
                                    config.linkedConfigurations = new HashMap();
                                } else {
                                    config.linkedConfigurations.put(value, -1);
                                }
                            } else if (key.equals(BSSID_KEY)) {
                                ssid = null;
                                bssid = null;
                                freq = 0;
                                seen = 0;
                                rssi = WifiConfiguration.INVALID_RSSI;
                                caps = "";
                            } else if (key.equals(RSSI_KEY)) {
                                rssi = Integer.parseInt(value);
                            } else if (key.equals(FREQ_KEY)) {
                                freq = Integer.parseInt(value);
                            } else if (!key.equals(DATE_KEY)) {
                                if (key.equals(BSSID_KEY_END)) {
                                    if (0 != 0 && ssid != null && getScanDetailCache(config, scanDetailCaches) != null) {
                                        WifiSsid wssid = WifiSsid.createFromAsciiEncoded(ssid);
                                        ScanDetail scanDetail = new ScanDetail(wssid, bssid, caps, rssi, freq, 0L, seen);
                                        getScanDetailCache(config, scanDetailCaches).put(scanDetail);
                                    }
                                } else if (key.equals(DELETED_EPHEMERAL_KEY)) {
                                    if (!TextUtils.isEmpty(value)) {
                                        deletedEphemeralSSIDs.add(value);
                                    }
                                } else if (key.equals(CREATOR_NAME_KEY)) {
                                    config.creatorName = value;
                                } else if (key.equals(UPDATE_NAME_KEY)) {
                                    config.lastUpdateName = value;
                                } else if (key.equals(USER_APPROVED_KEY)) {
                                    config.userApproved = Integer.parseInt(value);
                                } else if (key.equals(SHARED_KEY)) {
                                    config.shared = Boolean.parseBoolean(value);
                                } else if (key.equals(HAS_EVER_CONNECTED_KEY)) {
                                    networkStatus.setHasEverConnected(Boolean.parseBoolean(value));
                                }
                            }
                        }
                    }
                }
                if (in != null) {
                    try {
                        try {
                            in.close();
                        } catch (Throwable th3) {
                            th2 = th3;
                        }
                    } catch (EOFException e) {
                        return;
                    } catch (IOException e2) {
                        e = e2;
                        Log.e(TAG, "readNetworkHistory: No config file, revert to default, " + e, e);
                        return;
                    } catch (NumberFormatException e3) {
                        e = e3;
                        Log.e(TAG, "readNetworkHistory: failed to read, revert to default, " + e, e);
                        return;
                    }
                }
                if (th2 != null) {
                    throw th2;
                }
            } catch (Throwable th4) {
                th = th4;
                th = null;
                dataInputStream = in;
                if (dataInputStream != null) {
                }
                if (th != null) {
                }
            }
        } catch (Throwable th5) {
            th = th5;
            th = null;
        }
    }

    public boolean isValid(WifiConfiguration config) {
        if (config.allowedKeyManagement == null) {
            return false;
        }
        if (config.allowedKeyManagement.cardinality() > 1) {
            if (config.allowedKeyManagement.cardinality() != 2 || !config.allowedKeyManagement.get(2)) {
                return false;
            }
            if (!config.allowedKeyManagement.get(3) && !config.allowedKeyManagement.get(1)) {
                return false;
            }
        }
        return true;
    }

    private static String makeString(BitSet set, String[] strings) {
        StringBuffer buf = new StringBuffer();
        int nextSetBit = -1;
        BitSet set2 = set.get(0, strings.length);
        while (true) {
            nextSetBit = set2.nextSetBit(nextSetBit + 1);
            if (nextSetBit == -1) {
                break;
            }
            buf.append(strings[nextSetBit].replace('_', '-')).append(' ');
        }
        if (set2.cardinality() > 0) {
            buf.setLength(buf.length() - 1);
        }
        return buf.toString();
    }

    protected void logv(String s) {
        Log.v(TAG, s);
    }

    protected void logd(String s) {
        Log.d(TAG, s);
    }

    protected void log(String s) {
        Log.d(TAG, s);
    }

    protected void loge(String s) {
        loge(s, false);
    }

    protected void loge(String s, boolean stack) {
        if (stack) {
            Log.e(TAG, s + " stack:" + Thread.currentThread().getStackTrace()[2].getMethodName() + " - " + Thread.currentThread().getStackTrace()[3].getMethodName() + " - " + Thread.currentThread().getStackTrace()[4].getMethodName() + " - " + Thread.currentThread().getStackTrace()[5].getMethodName());
        } else {
            Log.e(TAG, s);
        }
    }

    private void localLog(String s) {
        if (this.mLocalLog == null) {
            return;
        }
        this.mLocalLog.log(s);
    }

    private ScanDetailCache getScanDetailCache(WifiConfiguration config, ConcurrentHashMap<Integer, ScanDetailCache> scanDetailCaches) {
        if (config == null || scanDetailCaches == null) {
            return null;
        }
        ScanDetailCache cache = scanDetailCaches.get(Integer.valueOf(config.networkId));
        if (cache == null && config.networkId != -1) {
            ScanDetailCache cache2 = new ScanDetailCache(config);
            scanDetailCaches.put(Integer.valueOf(config.networkId), cache2);
            return cache2;
        }
        return cache;
    }
}
