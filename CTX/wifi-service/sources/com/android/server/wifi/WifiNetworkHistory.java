package com.android.server.wifi;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.net.DelayedDiskWrite;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
    private static final String FQDN_KEY = "FQDN";
    private static final String FREQ_KEY = "FREQ";
    private static final String HAS_EVER_CONNECTED_KEY = "HAS_EVER_CONNECTED";
    private static final String LINK_KEY = "LINK";
    private static final String METERED_HINT_KEY = "METERED_HINT";
    private static final String METERED_OVERRIDE_KEY = "METERED_OVERRIDE";
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
    HashSet<String> mLostConfigsDbg = new HashSet<>();
    protected final DelayedDiskWrite mWriter;

    public WifiNetworkHistory(Context context, DelayedDiskWrite delayedDiskWrite) {
        this.mContext = context;
        this.mWriter = delayedDiskWrite;
    }

    public void writeKnownNetworkHistory(final List<WifiConfiguration> list, final ConcurrentHashMap<Integer, ScanDetailCache> concurrentHashMap, final Set<String> set) {
        this.mWriter.write(NETWORK_HISTORY_CONFIG_FILE, new DelayedDiskWrite.Writer() {
            public void onWriteCalled(DataOutputStream dataOutputStream) throws IOException {
                for (WifiConfiguration wifiConfiguration : list) {
                    WifiConfiguration.NetworkSelectionStatus networkSelectionStatus = wifiConfiguration.getNetworkSelectionStatus();
                    int size = 0;
                    if (wifiConfiguration.linkedConfigurations != null) {
                        size = wifiConfiguration.linkedConfigurations.size();
                    }
                    WifiNetworkHistory.this.logd("saving network history: " + wifiConfiguration.configKey() + " gw: " + wifiConfiguration.defaultGwMacAddress + " Network Selection-status: " + networkSelectionStatus.getNetworkStatusString() + (wifiConfiguration.getNetworkSelectionStatus().isNetworkEnabled() ? "" : "Disable time: " + DateFormat.getInstance().format(Long.valueOf(wifiConfiguration.getNetworkSelectionStatus().getDisableTime()))) + " ephemeral=" + wifiConfiguration.ephemeral + " choice:" + networkSelectionStatus.getConnectChoice() + " link:" + size + " status:" + wifiConfiguration.status + " nid:" + wifiConfiguration.networkId + " hasEverConnected: " + networkSelectionStatus.getHasEverConnected());
                    if (WifiNetworkHistory.this.isValid(wifiConfiguration)) {
                        if (wifiConfiguration.SSID == null) {
                            WifiNetworkHistory.this.logv("writeKnownNetworkHistory trying to write config with null SSID");
                        } else {
                            WifiNetworkHistory.this.logv("writeKnownNetworkHistory write config " + wifiConfiguration.configKey());
                            dataOutputStream.writeUTF("CONFIG:  " + wifiConfiguration.configKey() + WifiNetworkHistory.NL);
                            if (wifiConfiguration.SSID != null) {
                                dataOutputStream.writeUTF("SSID:  " + wifiConfiguration.SSID + WifiNetworkHistory.NL);
                            }
                            if (wifiConfiguration.BSSID != null) {
                                dataOutputStream.writeUTF("CONFIG_BSSID:  " + wifiConfiguration.BSSID + WifiNetworkHistory.NL);
                            } else {
                                dataOutputStream.writeUTF("CONFIG_BSSID:  null\n");
                            }
                            if (wifiConfiguration.FQDN != null) {
                                dataOutputStream.writeUTF("FQDN:  " + wifiConfiguration.FQDN + WifiNetworkHistory.NL);
                            }
                            dataOutputStream.writeUTF("PRIORITY:  " + Integer.toString(wifiConfiguration.priority) + WifiNetworkHistory.NL);
                            dataOutputStream.writeUTF("ID:  " + Integer.toString(wifiConfiguration.networkId) + WifiNetworkHistory.NL);
                            dataOutputStream.writeUTF("SELF_ADDED:  " + Boolean.toString(wifiConfiguration.selfAdded) + WifiNetworkHistory.NL);
                            dataOutputStream.writeUTF("DID_SELF_ADD:  " + Boolean.toString(wifiConfiguration.didSelfAdd) + WifiNetworkHistory.NL);
                            dataOutputStream.writeUTF("NO_INTERNET_ACCESS_REPORTS:  " + Integer.toString(wifiConfiguration.numNoInternetAccessReports) + WifiNetworkHistory.NL);
                            dataOutputStream.writeUTF("VALIDATED_INTERNET_ACCESS:  " + Boolean.toString(wifiConfiguration.validatedInternetAccess) + WifiNetworkHistory.NL);
                            dataOutputStream.writeUTF("NO_INTERNET_ACCESS_EXPECTED:  " + Boolean.toString(wifiConfiguration.noInternetAccessExpected) + WifiNetworkHistory.NL);
                            dataOutputStream.writeUTF("EPHEMERAL:  " + Boolean.toString(wifiConfiguration.ephemeral) + WifiNetworkHistory.NL);
                            dataOutputStream.writeUTF("METERED_HINT:  " + Boolean.toString(wifiConfiguration.meteredHint) + WifiNetworkHistory.NL);
                            dataOutputStream.writeUTF("METERED_OVERRIDE:  " + Integer.toString(wifiConfiguration.meteredOverride) + WifiNetworkHistory.NL);
                            dataOutputStream.writeUTF("USE_EXTERNAL_SCORES:  " + Boolean.toString(wifiConfiguration.useExternalScores) + WifiNetworkHistory.NL);
                            if (wifiConfiguration.creationTime != null) {
                                dataOutputStream.writeUTF("CREATION_TIME:  " + wifiConfiguration.creationTime + WifiNetworkHistory.NL);
                            }
                            if (wifiConfiguration.updateTime != null) {
                                dataOutputStream.writeUTF("UPDATE_TIME:  " + wifiConfiguration.updateTime + WifiNetworkHistory.NL);
                            }
                            if (wifiConfiguration.peerWifiConfiguration != null) {
                                dataOutputStream.writeUTF("PEER_CONFIGURATION:  " + wifiConfiguration.peerWifiConfiguration + WifiNetworkHistory.NL);
                            }
                            dataOutputStream.writeUTF("SCORER_OVERRIDE:  " + Integer.toString(wifiConfiguration.numScorerOverride) + WifiNetworkHistory.NL);
                            dataOutputStream.writeUTF("SCORER_OVERRIDE_AND_SWITCH:  " + Integer.toString(wifiConfiguration.numScorerOverrideAndSwitchedNetwork) + WifiNetworkHistory.NL);
                            dataOutputStream.writeUTF("NUM_ASSOCIATION:  " + Integer.toString(wifiConfiguration.numAssociation) + WifiNetworkHistory.NL);
                            dataOutputStream.writeUTF("CREATOR_UID_KEY:  " + Integer.toString(wifiConfiguration.creatorUid) + WifiNetworkHistory.NL);
                            dataOutputStream.writeUTF("CONNECT_UID_KEY:  " + Integer.toString(wifiConfiguration.lastConnectUid) + WifiNetworkHistory.NL);
                            dataOutputStream.writeUTF("UPDATE_UID:  " + Integer.toString(wifiConfiguration.lastUpdateUid) + WifiNetworkHistory.NL);
                            dataOutputStream.writeUTF("CREATOR_NAME:  " + wifiConfiguration.creatorName + WifiNetworkHistory.NL);
                            dataOutputStream.writeUTF("UPDATE_NAME:  " + wifiConfiguration.lastUpdateName + WifiNetworkHistory.NL);
                            dataOutputStream.writeUTF("USER_APPROVED:  " + Integer.toString(wifiConfiguration.userApproved) + WifiNetworkHistory.NL);
                            dataOutputStream.writeUTF("SHARED:  " + Boolean.toString(wifiConfiguration.shared) + WifiNetworkHistory.NL);
                            dataOutputStream.writeUTF("AUTH:  " + WifiNetworkHistory.makeString(wifiConfiguration.allowedKeyManagement, WifiConfiguration.KeyMgmt.strings) + WifiNetworkHistory.NL);
                            dataOutputStream.writeUTF("NETWORK_SELECTION_STATUS:  " + networkSelectionStatus.getNetworkSelectionStatus() + WifiNetworkHistory.NL);
                            dataOutputStream.writeUTF("NETWORK_SELECTION_DISABLE_REASON:  " + networkSelectionStatus.getNetworkSelectionDisableReason() + WifiNetworkHistory.NL);
                            if (networkSelectionStatus.getConnectChoice() != null) {
                                dataOutputStream.writeUTF("CHOICE:  " + networkSelectionStatus.getConnectChoice() + WifiNetworkHistory.NL);
                                dataOutputStream.writeUTF("CHOICE_TIME:  " + networkSelectionStatus.getConnectChoiceTimestamp() + WifiNetworkHistory.NL);
                            }
                            if (wifiConfiguration.linkedConfigurations != null) {
                                WifiNetworkHistory.this.log("writeKnownNetworkHistory write linked " + wifiConfiguration.linkedConfigurations.size());
                                Iterator it = wifiConfiguration.linkedConfigurations.keySet().iterator();
                                while (it.hasNext()) {
                                    dataOutputStream.writeUTF("LINK:  " + ((String) it.next()) + WifiNetworkHistory.NL);
                                }
                            }
                            String str = wifiConfiguration.defaultGwMacAddress;
                            if (str != null) {
                                dataOutputStream.writeUTF("DEFAULT_GW:  " + str + WifiNetworkHistory.NL);
                            }
                            if (WifiNetworkHistory.this.getScanDetailCache(wifiConfiguration, concurrentHashMap) != null) {
                                Iterator<ScanDetail> it2 = WifiNetworkHistory.this.getScanDetailCache(wifiConfiguration, concurrentHashMap).values().iterator();
                                while (it2.hasNext()) {
                                    ScanResult scanResult = it2.next().getScanResult();
                                    dataOutputStream.writeUTF("BSSID:  " + scanResult.BSSID + WifiNetworkHistory.NL);
                                    dataOutputStream.writeUTF("FREQ:  " + Integer.toString(scanResult.frequency) + WifiNetworkHistory.NL);
                                    dataOutputStream.writeUTF("RSSI:  " + Integer.toString(scanResult.level) + WifiNetworkHistory.NL);
                                    dataOutputStream.writeUTF("/BSSID\n");
                                }
                            }
                            dataOutputStream.writeUTF("HAS_EVER_CONNECTED:  " + Boolean.toString(networkSelectionStatus.getHasEverConnected()) + WifiNetworkHistory.NL);
                            dataOutputStream.writeUTF(WifiNetworkHistory.NL);
                            dataOutputStream.writeUTF(WifiNetworkHistory.NL);
                            dataOutputStream.writeUTF(WifiNetworkHistory.NL);
                        }
                    }
                }
                if (set != null && set.size() > 0) {
                    for (String str2 : set) {
                        dataOutputStream.writeUTF(WifiNetworkHistory.DELETED_EPHEMERAL_KEY);
                        dataOutputStream.writeUTF(str2);
                        dataOutputStream.writeUTF(WifiNetworkHistory.NL);
                    }
                }
            }
        });
    }

    public void readNetworkHistory(Map<String, WifiConfiguration> map, Map<Integer, ScanDetailCache> map2, Set<String> set) {
        WifiConfiguration.NetworkSelectionStatus networkSelectionStatus;
        try {
            DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(NETWORK_HISTORY_CONFIG_FILE)));
            Throwable th = null;
            try {
                try {
                    int i = WifiConfiguration.INVALID_RSSI;
                    WifiConfiguration wifiConfiguration = null;
                    while (true) {
                        String utf = dataInputStream.readUTF();
                        if (utf == null) {
                            dataInputStream.close();
                            return;
                        }
                        int iIndexOf = utf.indexOf(58);
                        if (iIndexOf >= 0) {
                            String strTrim = utf.substring(0, iIndexOf).trim();
                            String strTrim2 = utf.substring(iIndexOf + 1).trim();
                            if (strTrim.equals(CONFIG_KEY)) {
                                wifiConfiguration = map.get(strTrim2);
                                if (wifiConfiguration == null) {
                                    Log.e(TAG, "readNetworkHistory didnt find netid for hash=" + Integer.toString(strTrim2.hashCode()) + " key: " + strTrim2);
                                    this.mLostConfigsDbg.add(strTrim2);
                                } else if (wifiConfiguration.creatorName == null || wifiConfiguration.lastUpdateName == null) {
                                    wifiConfiguration.creatorName = this.mContext.getPackageManager().getNameForUid(1000);
                                    wifiConfiguration.lastUpdateName = wifiConfiguration.creatorName;
                                    Log.w(TAG, "Upgrading network " + wifiConfiguration.networkId + " to " + wifiConfiguration.creatorName);
                                }
                            } else if (wifiConfiguration != null) {
                                networkSelectionStatus = wifiConfiguration.getNetworkSelectionStatus();
                                switch (strTrim) {
                                    case "SSID":
                                        if (wifiConfiguration.isPasspoint()) {
                                            break;
                                        } else {
                                            if (wifiConfiguration.SSID == null || wifiConfiguration.SSID.equals(strTrim2)) {
                                                wifiConfiguration.SSID = strTrim2;
                                            } else {
                                                loge("Error parsing network history file, mismatched SSIDs");
                                                wifiConfiguration = null;
                                            }
                                            break;
                                        }
                                        break;
                                    case "CONFIG_BSSID":
                                        if (strTrim2.equals("null")) {
                                            strTrim2 = null;
                                        }
                                        wifiConfiguration.BSSID = strTrim2;
                                        break;
                                    case "FQDN":
                                        if (strTrim2.equals("null")) {
                                            strTrim2 = null;
                                        }
                                        wifiConfiguration.FQDN = strTrim2;
                                        break;
                                    case "DEFAULT_GW":
                                        wifiConfiguration.defaultGwMacAddress = strTrim2;
                                        break;
                                    case "SELF_ADDED":
                                        wifiConfiguration.selfAdded = Boolean.parseBoolean(strTrim2);
                                        break;
                                    case "DID_SELF_ADD":
                                        wifiConfiguration.didSelfAdd = Boolean.parseBoolean(strTrim2);
                                        break;
                                    case "NO_INTERNET_ACCESS_REPORTS":
                                        wifiConfiguration.numNoInternetAccessReports = Integer.parseInt(strTrim2);
                                        break;
                                    case "VALIDATED_INTERNET_ACCESS":
                                        wifiConfiguration.validatedInternetAccess = Boolean.parseBoolean(strTrim2);
                                        break;
                                    case "NO_INTERNET_ACCESS_EXPECTED":
                                        wifiConfiguration.noInternetAccessExpected = Boolean.parseBoolean(strTrim2);
                                        break;
                                    case "CREATION_TIME":
                                        wifiConfiguration.creationTime = strTrim2;
                                        break;
                                    case "UPDATE_TIME":
                                        wifiConfiguration.updateTime = strTrim2;
                                        break;
                                    case "EPHEMERAL":
                                        wifiConfiguration.ephemeral = Boolean.parseBoolean(strTrim2);
                                        break;
                                    case "METERED_HINT":
                                        wifiConfiguration.meteredHint = Boolean.parseBoolean(strTrim2);
                                        break;
                                    case "METERED_OVERRIDE":
                                        wifiConfiguration.meteredOverride = Integer.parseInt(strTrim2);
                                        break;
                                    case "USE_EXTERNAL_SCORES":
                                        wifiConfiguration.useExternalScores = Boolean.parseBoolean(strTrim2);
                                        break;
                                    case "CREATOR_UID_KEY":
                                        wifiConfiguration.creatorUid = Integer.parseInt(strTrim2);
                                        break;
                                    case "SCORER_OVERRIDE":
                                        wifiConfiguration.numScorerOverride = Integer.parseInt(strTrim2);
                                        break;
                                    case "SCORER_OVERRIDE_AND_SWITCH":
                                        wifiConfiguration.numScorerOverrideAndSwitchedNetwork = Integer.parseInt(strTrim2);
                                        break;
                                    case "NUM_ASSOCIATION":
                                        wifiConfiguration.numAssociation = Integer.parseInt(strTrim2);
                                        break;
                                    case "CONNECT_UID_KEY":
                                        wifiConfiguration.lastConnectUid = Integer.parseInt(strTrim2);
                                        break;
                                    case "UPDATE_UID":
                                        wifiConfiguration.lastUpdateUid = Integer.parseInt(strTrim2);
                                        break;
                                    case "PEER_CONFIGURATION":
                                        wifiConfiguration.peerWifiConfiguration = strTrim2;
                                        break;
                                    case "NETWORK_SELECTION_STATUS":
                                        int i2 = Integer.parseInt(strTrim2);
                                        if (i2 == 1) {
                                            i2 = 0;
                                        }
                                        networkSelectionStatus.setNetworkSelectionStatus(i2);
                                        break;
                                    case "NETWORK_SELECTION_DISABLE_REASON":
                                        networkSelectionStatus.setNetworkSelectionDisableReason(Integer.parseInt(strTrim2));
                                        break;
                                    case "CHOICE":
                                        networkSelectionStatus.setConnectChoice(strTrim2);
                                        break;
                                    case "CHOICE_TIME":
                                        networkSelectionStatus.setConnectChoiceTimestamp(Long.parseLong(strTrim2));
                                        break;
                                    case "LINK":
                                        if (wifiConfiguration.linkedConfigurations == null) {
                                            wifiConfiguration.linkedConfigurations = new HashMap();
                                            break;
                                        } else {
                                            wifiConfiguration.linkedConfigurations.put(strTrim2, -1);
                                            break;
                                        }
                                        break;
                                    case "BSSID":
                                        int i3 = WifiConfiguration.INVALID_RSSI;
                                        break;
                                    case "RSSI":
                                        Integer.parseInt(strTrim2);
                                        break;
                                    case "FREQ":
                                        Integer.parseInt(strTrim2);
                                        break;
                                    case "DELETED_EPHEMERAL":
                                        if (TextUtils.isEmpty(strTrim2)) {
                                            break;
                                        } else {
                                            set.add(strTrim2);
                                            break;
                                        }
                                        break;
                                    case "CREATOR_NAME":
                                        wifiConfiguration.creatorName = strTrim2;
                                        break;
                                    case "UPDATE_NAME":
                                        wifiConfiguration.lastUpdateName = strTrim2;
                                        break;
                                    case "USER_APPROVED":
                                        wifiConfiguration.userApproved = Integer.parseInt(strTrim2);
                                        break;
                                    case "SHARED":
                                        wifiConfiguration.shared = Boolean.parseBoolean(strTrim2);
                                        break;
                                    case "HAS_EVER_CONNECTED":
                                        networkSelectionStatus.setHasEverConnected(Boolean.parseBoolean(strTrim2));
                                        break;
                                }
                            }
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                if (th != null) {
                    try {
                        dataInputStream.close();
                    } catch (Throwable th4) {
                        th.addSuppressed(th4);
                    }
                } else {
                    dataInputStream.close();
                }
                throw th3;
            }
        } catch (EOFException e) {
        } catch (FileNotFoundException e2) {
            Log.i(TAG, "readNetworkHistory: no config file, " + e2);
        } catch (IOException e3) {
            Log.e(TAG, "readNetworkHistory: failed to read, " + e3, e3);
        } catch (NumberFormatException e4) {
            Log.e(TAG, "readNetworkHistory: failed to parse, " + e4, e4);
        }
    }

    public boolean isValid(WifiConfiguration wifiConfiguration) {
        if (wifiConfiguration.allowedKeyManagement == null) {
            return false;
        }
        if (wifiConfiguration.allowedKeyManagement.cardinality() > 1) {
            if (wifiConfiguration.allowedKeyManagement.cardinality() != 2 || !wifiConfiguration.allowedKeyManagement.get(2)) {
                return false;
            }
            if (!wifiConfiguration.allowedKeyManagement.get(3) && !wifiConfiguration.allowedKeyManagement.get(1)) {
                return false;
            }
        }
        return true;
    }

    private static String makeString(BitSet bitSet, String[] strArr) {
        StringBuffer stringBuffer = new StringBuffer();
        BitSet bitSet2 = bitSet.get(0, strArr.length);
        int iNextSetBit = -1;
        while (true) {
            iNextSetBit = bitSet2.nextSetBit(iNextSetBit + 1);
            if (iNextSetBit == -1) {
                break;
            }
            stringBuffer.append(strArr[iNextSetBit].replace('_', '-'));
            stringBuffer.append(' ');
        }
        if (bitSet2.cardinality() > 0) {
            stringBuffer.setLength(stringBuffer.length() - 1);
        }
        return stringBuffer.toString();
    }

    protected void logv(String str) {
        Log.v(TAG, str);
    }

    protected void logd(String str) {
        Log.d(TAG, str);
    }

    protected void log(String str) {
        Log.d(TAG, str);
    }

    protected void loge(String str) {
        loge(str, false);
    }

    protected void loge(String str, boolean z) {
        if (z) {
            Log.e(TAG, str + " stack:" + Thread.currentThread().getStackTrace()[2].getMethodName() + " - " + Thread.currentThread().getStackTrace()[3].getMethodName() + " - " + Thread.currentThread().getStackTrace()[4].getMethodName() + " - " + Thread.currentThread().getStackTrace()[5].getMethodName());
            return;
        }
        Log.e(TAG, str);
    }

    private ScanDetailCache getScanDetailCache(WifiConfiguration wifiConfiguration, Map<Integer, ScanDetailCache> map) {
        if (wifiConfiguration == null || map == null) {
            return null;
        }
        ScanDetailCache scanDetailCache = map.get(Integer.valueOf(wifiConfiguration.networkId));
        if (scanDetailCache == null && wifiConfiguration.networkId != -1) {
            ScanDetailCache scanDetailCache2 = new ScanDetailCache(wifiConfiguration, WifiConfigManager.SCAN_CACHE_ENTRIES_MAX_SIZE, 128);
            map.put(Integer.valueOf(wifiConfiguration.networkId), scanDetailCache2);
            return scanDetailCache2;
        }
        return scanDetailCache;
    }
}
