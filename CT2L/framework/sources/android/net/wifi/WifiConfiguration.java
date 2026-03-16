package android.net.wifi;

import android.net.IpConfiguration;
import android.net.ProxyInfo;
import android.net.StaticIpConfiguration;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import com.android.internal.content.NativeLibraryHelper;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

public class WifiConfiguration implements Parcelable {
    public static final int AUTO_JOIN_DELETED = 200;
    public static final int AUTO_JOIN_DISABLED_NO_CREDENTIALS = 160;
    public static final int AUTO_JOIN_DISABLED_ON_AUTH_FAILURE = 128;
    public static final int AUTO_JOIN_DISABLED_USER_ACTION = 161;
    public static final int AUTO_JOIN_ENABLED = 0;
    public static final int AUTO_JOIN_TEMPORARY_DISABLED = 1;
    public static final int AUTO_JOIN_TEMPORARY_DISABLED_AT_SUPPLICANT = 64;
    public static final int AUTO_JOIN_TEMPORARY_DISABLED_LINK_ERRORS = 32;
    public static final int DEFAULT_CHANNEL_INDEX = 0;
    public static final int DISABLED_ASSOCIATION_REJECT = 4;
    public static final int DISABLED_AUTH_FAILURE = 3;
    public static final int DISABLED_BY_WIFI_MANAGER = 5;
    public static final int DISABLED_DHCP_FAILURE = 2;
    public static final int DISABLED_DNS_FAILURE = 1;
    public static final int DISABLED_UNKNOWN_REASON = 0;
    public static final int INVALID_NETWORK_ID = -1;
    private static final String TAG = "WifiConfiguration";
    public static final int WAPI_PSK_TYPE_ASCII = 0;
    public static final int WAPI_PSK_TYPE_HEX = 1;
    public static final String bssidVarName = "bssid";
    public static final String hiddenSSIDVarName = "scan_ssid";
    public static final String pmfVarName = "ieee80211w";
    public static final String priorityVarName = "priority";
    public static final String pskVarName = "psk";
    public static final String ssidVarName = "ssid";
    public static final String updateIdentiferVarName = "update_identifier";
    public static final String wapiPkcs12KeyVarName = "wapi_pkcs12_key";
    public static final String wapiPskTypeVarName = "wapi_psk_type";
    public static final String wapiPskVarName = "wapi_psk";
    public static final String wapiRootCertVarName = "wapi_root_cert";
    public static final String wapiUserCertVarName = "wapi_user_cert";
    public static final String wepTxKeyIdxVarName = "wep_tx_keyidx";
    public String BSSID;
    public String FQDN;
    public boolean NOT_UTF8;
    public String SSID;
    public BitSet allowedAuthAlgorithms;
    public BitSet allowedGroupCiphers;
    public BitSet allowedKeyManagement;
    public BitSet allowedPairwiseCiphers;
    public BitSet allowedProtocols;
    public String autoJoinBSSID;
    public boolean autoJoinBailedDueToLowRssi;
    public int autoJoinStatus;
    public int autoJoinUseAggressiveJoinAttemptThreshold;
    public long blackListTimestamp;
    public HashMap<String, Integer> connectChoices;
    public int creatorUid;
    public String defaultGwMacAddress;
    public String dhcpServer;
    public boolean didSelfAdd;
    public boolean dirty;
    public int disableReason;
    public WifiEnterpriseConfig enterpriseConfig;
    public boolean ephemeral;
    public boolean hiddenSSID;
    public int lastConnectUid;
    public long lastConnected;
    public long lastConnectionFailure;
    public long lastDisconnected;
    public String lastFailure;
    public long lastRoamingFailure;
    public int lastRoamingFailureReason;
    public int lastUpdateUid;
    public HashMap<String, Integer> linkedConfigurations;
    String mCachedConfigKey;
    private IpConfiguration mIpConfiguration;
    public String naiRealm;
    public int networkId;
    public int numAssociation;
    public int numAuthFailures;
    public int numConnectionFailures;
    public int numIpConfigFailures;
    public int numNoInternetAccessReports;
    public int numScorerOverride;
    public int numScorerOverrideAndSwitchedNetwork;
    public int numTicksAtBadRSSI;
    public int numTicksAtLowRSSI;
    public int numTicksAtNotHighRSSI;
    public int numUserTriggeredJoinAttempts;
    public int numUserTriggeredWifiDisableBadRSSI;
    public int numUserTriggeredWifiDisableLowRSSI;
    public int numUserTriggeredWifiDisableNotHighRSSI;
    public String peerWifiConfiguration;
    public String preSharedKey;
    public int priority;
    public boolean requirePMF;
    public long roamingFailureBlackListTimeMilli;
    public HashMap<String, ScanResult> scanResultCache;
    public boolean selfAdded;
    public int status;
    public String updateIdentifier;
    public boolean validatedInternetAccess;
    public Visibility visibility;
    public String wapiPkcs12Key;
    public String wapiPsk;
    public String wapiPskType;
    public String wapiRootCert;
    public String wapiUserCert;
    public String[] wepKeys;
    public int wepTxKeyIndex;
    public int wifiApChannelIndex;
    public static final String[] wepKeyVarNames = {"wep_key0", "wep_key1", "wep_key2", "wep_key3"};
    public static final String[] WAPI_PSK_TYPE = {"wapi_psk_ascii", "wapi_psk_hex"};
    public static int INVALID_RSSI = WifiInfo.INVALID_RSSI;
    public static int UNWANTED_BLACKLIST_SOFT_RSSI_24 = -80;
    public static int UNWANTED_BLACKLIST_SOFT_RSSI_5 = -70;
    public static int GOOD_RSSI_24 = -65;
    public static int LOW_RSSI_24 = -77;
    public static int BAD_RSSI_24 = -87;
    public static int GOOD_RSSI_5 = -60;
    public static int LOW_RSSI_5 = -72;
    public static int BAD_RSSI_5 = -82;
    public static int UNWANTED_BLACKLIST_SOFT_BUMP = 4;
    public static int UNWANTED_BLACKLIST_HARD_BUMP = 8;
    public static int UNBLACKLIST_THRESHOLD_24_SOFT = -77;
    public static int UNBLACKLIST_THRESHOLD_24_HARD = -68;
    public static int UNBLACKLIST_THRESHOLD_5_SOFT = -63;
    public static int UNBLACKLIST_THRESHOLD_5_HARD = -56;
    public static int INITIAL_AUTO_JOIN_ATTEMPT_MIN_24 = -80;
    public static int INITIAL_AUTO_JOIN_ATTEMPT_MIN_5 = -70;
    public static int A_BAND_PREFERENCE_RSSI_THRESHOLD = -65;
    public static int G_BAND_PREFERENCE_RSSI_THRESHOLD = -75;
    public static int HOME_NETWORK_RSSI_BOOST = 5;
    public static int MAX_INITIAL_AUTO_JOIN_RSSI_BOOST = 8;
    public static int ROAMING_FAILURE_IP_CONFIG = 1;
    public static int ROAMING_FAILURE_AUTH_FAILURE = 2;
    public static final Parcelable.Creator<WifiConfiguration> CREATOR = new Parcelable.Creator<WifiConfiguration>() {
        @Override
        public WifiConfiguration createFromParcel(Parcel in) {
            WifiConfiguration config = new WifiConfiguration();
            config.networkId = in.readInt();
            config.status = in.readInt();
            config.disableReason = in.readInt();
            config.SSID = in.readString();
            config.BSSID = in.readString();
            config.autoJoinBSSID = in.readString();
            config.FQDN = in.readString();
            config.naiRealm = in.readString();
            config.preSharedKey = in.readString();
            config.wapiPsk = in.readString();
            config.wapiPskType = in.readString();
            config.wapiPkcs12Key = in.readString();
            config.wapiRootCert = in.readString();
            config.wapiUserCert = in.readString();
            config.wifiApChannelIndex = in.readInt();
            for (int i = 0; i < config.wepKeys.length; i++) {
                config.wepKeys[i] = in.readString();
            }
            config.wepTxKeyIndex = in.readInt();
            config.priority = in.readInt();
            config.hiddenSSID = in.readInt() != 0;
            config.requirePMF = in.readInt() != 0;
            config.updateIdentifier = in.readString();
            config.allowedKeyManagement = WifiConfiguration.readBitSet(in);
            config.allowedProtocols = WifiConfiguration.readBitSet(in);
            config.allowedAuthAlgorithms = WifiConfiguration.readBitSet(in);
            config.allowedPairwiseCiphers = WifiConfiguration.readBitSet(in);
            config.allowedGroupCiphers = WifiConfiguration.readBitSet(in);
            config.enterpriseConfig = (WifiEnterpriseConfig) in.readParcelable(null);
            config.mIpConfiguration = (IpConfiguration) in.readParcelable(null);
            config.dhcpServer = in.readString();
            config.defaultGwMacAddress = in.readString();
            config.autoJoinStatus = in.readInt();
            config.selfAdded = in.readInt() != 0;
            config.didSelfAdd = in.readInt() != 0;
            config.validatedInternetAccess = in.readInt() != 0;
            config.ephemeral = in.readInt() != 0;
            config.creatorUid = in.readInt();
            config.lastConnectUid = in.readInt();
            config.lastUpdateUid = in.readInt();
            config.blackListTimestamp = in.readLong();
            config.lastConnectionFailure = in.readLong();
            config.lastRoamingFailure = in.readLong();
            config.lastRoamingFailureReason = in.readInt();
            config.roamingFailureBlackListTimeMilli = in.readLong();
            config.numConnectionFailures = in.readInt();
            config.numIpConfigFailures = in.readInt();
            config.numAuthFailures = in.readInt();
            config.numScorerOverride = in.readInt();
            config.numScorerOverrideAndSwitchedNetwork = in.readInt();
            config.numAssociation = in.readInt();
            config.numUserTriggeredWifiDisableLowRSSI = in.readInt();
            config.numUserTriggeredWifiDisableBadRSSI = in.readInt();
            config.numUserTriggeredWifiDisableNotHighRSSI = in.readInt();
            config.numTicksAtLowRSSI = in.readInt();
            config.numTicksAtBadRSSI = in.readInt();
            config.numTicksAtNotHighRSSI = in.readInt();
            config.numUserTriggeredJoinAttempts = in.readInt();
            config.autoJoinUseAggressiveJoinAttemptThreshold = in.readInt();
            config.autoJoinBailedDueToLowRssi = in.readInt() != 0;
            config.numNoInternetAccessReports = in.readInt();
            return config;
        }

        @Override
        public WifiConfiguration[] newArray(int size) {
            return new WifiConfiguration[size];
        }
    };

    public static class KeyMgmt {
        public static final int IEEE8021X = 3;
        public static final int NONE = 0;
        public static final int WPA2_PSK = 4;
        public static final int WPA_EAP = 2;
        public static final int WPA_PSK = 1;
        public static final String[] strings = {"NONE", "WPA_PSK", "WPA_EAP", "IEEE8021X", "WPA2_PSK"};
        public static final String varName = "key_mgmt";

        private KeyMgmt() {
        }
    }

    public static class Protocol {
        public static final int RSN = 1;
        public static final int WPA = 0;
        public static final String[] strings = {"WPA", "RSN"};
        public static final String varName = "proto";

        private Protocol() {
        }
    }

    public static class AuthAlgorithm {
        public static final int LEAP = 2;
        public static final int OPEN = 0;
        public static final int SHARED = 1;
        public static final String[] strings = {"OPEN", "SHARED", "LEAP"};
        public static final String varName = "auth_alg";

        private AuthAlgorithm() {
        }
    }

    public static class PairwiseCipher {
        public static final int CCMP = 2;
        public static final int NONE = 0;
        public static final int TKIP = 1;
        public static final String[] strings = {"NONE", "TKIP", "CCMP"};
        public static final String varName = "pairwise";

        private PairwiseCipher() {
        }
    }

    public static class GroupCipher {
        public static final int CCMP = 3;
        public static final int TKIP = 2;
        public static final int WEP104 = 1;
        public static final int WEP40 = 0;
        public static final String[] strings = {"WEP40", "WEP104", "TKIP", "CCMP"};
        public static final String varName = "group";

        private GroupCipher() {
        }
    }

    public static class Status {
        public static final int CURRENT = 0;
        public static final int DISABLED = 1;
        public static final int ENABLED = 2;
        public static final String[] strings = {"current", "disabled", "enabled"};

        private Status() {
        }
    }

    public final class Visibility {
        public String BSSID24;
        public String BSSID5;
        public long age24;
        public long age5;
        public int bandPreferenceBoost;
        public int currentNetworkBoost;
        public int lastChoiceBoost;
        public String lastChoiceConfig;
        public int num24;
        public int num5;
        public int rssi24;
        public int rssi5;
        public int score;

        public Visibility() {
            this.rssi5 = WifiConfiguration.INVALID_RSSI;
            this.rssi24 = WifiConfiguration.INVALID_RSSI;
        }

        public Visibility(Visibility source) {
            this.rssi5 = source.rssi5;
            this.rssi24 = source.rssi24;
            this.age24 = source.age24;
            this.age5 = source.age5;
            this.num24 = source.num24;
            this.num5 = source.num5;
            this.BSSID5 = source.BSSID5;
            this.BSSID24 = source.BSSID24;
        }

        public String toString() {
            StringBuilder sbuf = new StringBuilder();
            sbuf.append("[");
            if (this.rssi24 > WifiConfiguration.INVALID_RSSI) {
                sbuf.append(Integer.toString(this.rssi24));
                sbuf.append(",");
                sbuf.append(Integer.toString(this.num24));
                if (this.BSSID24 != null) {
                    sbuf.append(",").append(this.BSSID24);
                }
            }
            sbuf.append("; ");
            if (this.rssi5 > WifiConfiguration.INVALID_RSSI) {
                sbuf.append(Integer.toString(this.rssi5));
                sbuf.append(",");
                sbuf.append(Integer.toString(this.num5));
                if (this.BSSID5 != null) {
                    sbuf.append(",").append(this.BSSID5);
                }
            }
            if (this.score != 0) {
                sbuf.append("; ").append(this.score);
                sbuf.append(", ").append(this.currentNetworkBoost);
                sbuf.append(", ").append(this.bandPreferenceBoost);
                if (this.lastChoiceConfig != null) {
                    sbuf.append(", ").append(this.lastChoiceBoost);
                    sbuf.append(", ").append(this.lastChoiceConfig);
                }
            }
            sbuf.append("]");
            return sbuf.toString();
        }
    }

    public Visibility setVisibility(long age) {
        Visibility status = null;
        if (this.scanResultCache == null) {
            this.visibility = null;
        } else {
            status = new Visibility();
            long now_ms = System.currentTimeMillis();
            for (ScanResult result : this.scanResultCache.values()) {
                if (result.seen != 0) {
                    if (result.is5GHz()) {
                        status.num5++;
                    } else if (result.is24GHz()) {
                        status.num24++;
                    }
                    if (now_ms - result.seen <= age) {
                        if (result.is5GHz()) {
                            if (result.level > status.rssi5) {
                                status.rssi5 = result.level;
                                status.age5 = result.seen;
                                status.BSSID5 = result.BSSID;
                            }
                        } else if (result.is24GHz() && result.level > status.rssi24) {
                            status.rssi24 = result.level;
                            status.age24 = result.seen;
                            status.BSSID24 = result.BSSID;
                        }
                    }
                }
            }
            this.visibility = status;
        }
        return status;
    }

    public boolean hasNoInternetAccess() {
        return this.numNoInternetAccessReports > 0 && !this.validatedInternetAccess;
    }

    public WifiConfiguration() {
        this.NOT_UTF8 = false;
        this.roamingFailureBlackListTimeMilli = 1000L;
        this.networkId = -1;
        this.SSID = null;
        this.BSSID = null;
        this.FQDN = null;
        this.naiRealm = null;
        this.priority = 0;
        this.wifiApChannelIndex = 0;
        this.hiddenSSID = false;
        this.disableReason = 0;
        this.allowedKeyManagement = new BitSet();
        this.allowedProtocols = new BitSet();
        this.allowedAuthAlgorithms = new BitSet();
        this.allowedPairwiseCiphers = new BitSet();
        this.allowedGroupCiphers = new BitSet();
        this.wepKeys = new String[4];
        for (int i = 0; i < this.wepKeys.length; i++) {
            this.wepKeys[i] = null;
        }
        this.enterpriseConfig = new WifiEnterpriseConfig();
        this.autoJoinStatus = 0;
        this.selfAdded = false;
        this.didSelfAdd = false;
        this.ephemeral = false;
        this.validatedInternetAccess = false;
        this.mIpConfiguration = new IpConfiguration();
    }

    public boolean isValid() {
        if (this.allowedKeyManagement == null) {
            return false;
        }
        if (this.allowedKeyManagement.cardinality() > 1) {
            if (this.allowedKeyManagement.cardinality() != 2 || !this.allowedKeyManagement.get(2)) {
                return false;
            }
            if (!this.allowedKeyManagement.get(3) && !this.allowedKeyManagement.get(1)) {
                return false;
            }
        }
        return true;
    }

    public boolean isLinked(WifiConfiguration config) {
        return (config.linkedConfigurations == null || this.linkedConfigurations == null || config.linkedConfigurations.get(configKey()) == null || this.linkedConfigurations.get(config.configKey()) == null) ? false : true;
    }

    public ScanResult lastSeen() {
        ScanResult mostRecent = null;
        if (this.scanResultCache == null) {
            return null;
        }
        for (ScanResult result : this.scanResultCache.values()) {
            if (mostRecent == null) {
                if (result.seen != 0) {
                    mostRecent = result;
                }
            } else if (result.seen > mostRecent.seen) {
                mostRecent = result;
            }
        }
        return mostRecent;
    }

    public void setAutoJoinStatus(int status) {
        if (status < 0) {
            status = 0;
        }
        if (status == 0) {
            this.blackListTimestamp = 0L;
        } else if (status > this.autoJoinStatus) {
            this.blackListTimestamp = System.currentTimeMillis();
        }
        if (status != this.autoJoinStatus) {
            this.autoJoinStatus = status;
            this.dirty = true;
        }
    }

    public void trimScanResultsCache(int num) {
        int currenSize;
        if (this.scanResultCache != null && (currenSize = this.scanResultCache.size()) > num) {
            ArrayList<ScanResult> list = new ArrayList<>(this.scanResultCache.values());
            if (list.size() != 0) {
                Collections.sort(list, new Comparator() {
                    @Override
                    public int compare(Object o1, Object o2) {
                        ScanResult a = (ScanResult) o1;
                        ScanResult b = (ScanResult) o2;
                        if (a.seen > b.seen) {
                            return 1;
                        }
                        if (a.seen < b.seen) {
                            return -1;
                        }
                        return a.BSSID.compareTo(b.BSSID);
                    }
                });
            }
            for (int i = 0; i < currenSize - num; i++) {
                ScanResult result = list.get(i);
                this.scanResultCache.remove(result.BSSID);
            }
        }
    }

    private ArrayList<ScanResult> sortScanResults() {
        ArrayList<ScanResult> list = new ArrayList<>(this.scanResultCache.values());
        if (list.size() != 0) {
            Collections.sort(list, new Comparator() {
                @Override
                public int compare(Object o1, Object o2) {
                    ScanResult a = (ScanResult) o1;
                    ScanResult b = (ScanResult) o2;
                    if (a.numIpConfigFailures > b.numIpConfigFailures) {
                        return 1;
                    }
                    if (a.numIpConfigFailures >= b.numIpConfigFailures && a.seen <= b.seen) {
                        if (a.seen < b.seen) {
                            return 1;
                        }
                        if (a.level > b.level) {
                            return -1;
                        }
                        if (a.level >= b.level) {
                            return a.BSSID.compareTo(b.BSSID);
                        }
                        return 1;
                    }
                    return -1;
                }
            });
        }
        return list;
    }

    public String toString() {
        StringBuilder sbuf = new StringBuilder();
        if (this.status == 0) {
            sbuf.append("* ");
        } else if (this.status == 1) {
            sbuf.append("- DSBLE ");
        }
        sbuf.append("ID: ").append(this.networkId).append(" SSID: ").append(this.SSID).append(" BSSID: ").append(this.BSSID).append(" FQDN: ").append(this.FQDN).append(" REALM: ").append(this.naiRealm).append(" UAPChannelIndex: ").append(this.wifiApChannelIndex).append(" PRIO: ").append(this.priority).append('\n');
        if (this.numConnectionFailures > 0) {
            sbuf.append(" numConnectFailures ").append(this.numConnectionFailures).append("\n");
        }
        if (this.numIpConfigFailures > 0) {
            sbuf.append(" numIpConfigFailures ").append(this.numIpConfigFailures).append("\n");
        }
        if (this.numAuthFailures > 0) {
            sbuf.append(" numAuthFailures ").append(this.numAuthFailures).append("\n");
        }
        if (this.autoJoinStatus > 0) {
            sbuf.append(" autoJoinStatus ").append(this.autoJoinStatus).append("\n");
        }
        if (this.disableReason > 0) {
            sbuf.append(" disableReason ").append(this.disableReason).append("\n");
        }
        if (this.numAssociation > 0) {
            sbuf.append(" numAssociation ").append(this.numAssociation).append("\n");
        }
        if (this.numNoInternetAccessReports > 0) {
            sbuf.append(" numNoInternetAccessReports ");
            sbuf.append(this.numNoInternetAccessReports).append("\n");
        }
        if (this.didSelfAdd) {
            sbuf.append(" didSelfAdd");
        }
        if (this.selfAdded) {
            sbuf.append(" selfAdded");
        }
        if (this.validatedInternetAccess) {
            sbuf.append(" validatedInternetAccess");
        }
        if (this.ephemeral) {
            sbuf.append(" ephemeral");
        }
        if (this.didSelfAdd || this.selfAdded || this.validatedInternetAccess || this.ephemeral) {
            sbuf.append("\n");
        }
        sbuf.append(" KeyMgmt:");
        for (int k = 0; k < this.allowedKeyManagement.size(); k++) {
            if (this.allowedKeyManagement.get(k)) {
                sbuf.append(" ");
                if (k < KeyMgmt.strings.length) {
                    sbuf.append(KeyMgmt.strings[k]);
                } else {
                    sbuf.append("??");
                }
            }
        }
        sbuf.append(" Protocols:");
        for (int p = 0; p < this.allowedProtocols.size(); p++) {
            if (this.allowedProtocols.get(p)) {
                sbuf.append(" ");
                if (p < Protocol.strings.length) {
                    sbuf.append(Protocol.strings[p]);
                } else {
                    sbuf.append("??");
                }
            }
        }
        sbuf.append('\n');
        sbuf.append(" AuthAlgorithms:");
        for (int a = 0; a < this.allowedAuthAlgorithms.size(); a++) {
            if (this.allowedAuthAlgorithms.get(a)) {
                sbuf.append(" ");
                if (a < AuthAlgorithm.strings.length) {
                    sbuf.append(AuthAlgorithm.strings[a]);
                } else {
                    sbuf.append("??");
                }
            }
        }
        sbuf.append('\n');
        sbuf.append(" PairwiseCiphers:");
        for (int pc = 0; pc < this.allowedPairwiseCiphers.size(); pc++) {
            if (this.allowedPairwiseCiphers.get(pc)) {
                sbuf.append(" ");
                if (pc < PairwiseCipher.strings.length) {
                    sbuf.append(PairwiseCipher.strings[pc]);
                } else {
                    sbuf.append("??");
                }
            }
        }
        sbuf.append('\n');
        sbuf.append(" GroupCiphers:");
        for (int gc = 0; gc < this.allowedGroupCiphers.size(); gc++) {
            if (this.allowedGroupCiphers.get(gc)) {
                sbuf.append(" ");
                if (gc < GroupCipher.strings.length) {
                    sbuf.append(GroupCipher.strings[gc]);
                } else {
                    sbuf.append("??");
                }
            }
        }
        sbuf.append('\n').append(" PSK: ");
        if (this.preSharedKey != null) {
            sbuf.append('*');
        }
        sbuf.append("\nEnterprise config:\n");
        sbuf.append(this.enterpriseConfig);
        sbuf.append("IP config:\n");
        sbuf.append(this.mIpConfiguration.toString());
        if (this.creatorUid != 0) {
            sbuf.append(" uid=" + Integer.toString(this.creatorUid));
        }
        if (this.autoJoinBSSID != null) {
            sbuf.append(" autoJoinBSSID=" + this.autoJoinBSSID);
        }
        long now_ms = System.currentTimeMillis();
        if (this.blackListTimestamp != 0) {
            sbuf.append('\n');
            long diff = now_ms - this.blackListTimestamp;
            if (diff <= 0) {
                sbuf.append(" blackListed since <incorrect>");
            } else {
                sbuf.append(" blackListed: ").append(Long.toString(diff / 1000)).append("sec");
            }
        }
        if (this.lastConnected != 0) {
            sbuf.append('\n');
            long diff2 = now_ms - this.lastConnected;
            if (diff2 <= 0) {
                sbuf.append("lastConnected since <incorrect>");
            } else {
                sbuf.append("lastConnected: ").append(Long.toString(diff2 / 1000)).append("sec");
            }
        }
        if (this.lastConnectionFailure != 0) {
            sbuf.append('\n');
            long diff3 = now_ms - this.lastConnectionFailure;
            if (diff3 <= 0) {
                sbuf.append("lastConnectionFailure since <incorrect>");
            } else {
                sbuf.append("lastConnectionFailure: ").append(Long.toString(diff3 / 1000));
                sbuf.append("sec");
            }
        }
        if (this.lastRoamingFailure != 0) {
            sbuf.append('\n');
            long diff4 = now_ms - this.lastRoamingFailure;
            if (diff4 <= 0) {
                sbuf.append("lastRoamingFailure since <incorrect>");
            } else {
                sbuf.append("lastRoamingFailure: ").append(Long.toString(diff4 / 1000));
                sbuf.append("sec");
            }
        }
        sbuf.append("roamingFailureBlackListTimeMilli: ").append(Long.toString(this.roamingFailureBlackListTimeMilli));
        sbuf.append('\n');
        if (this.linkedConfigurations != null) {
            Iterator<String> it = this.linkedConfigurations.keySet().iterator();
            while (it.hasNext()) {
                sbuf.append(" linked: ").append(it.next());
                sbuf.append('\n');
            }
        }
        if (this.connectChoices != null) {
            for (String key : this.connectChoices.keySet()) {
                Integer choice = this.connectChoices.get(key);
                if (choice != null) {
                    sbuf.append(" choice: ").append(key);
                    sbuf.append(" = ").append(choice);
                    sbuf.append('\n');
                }
            }
        }
        if (this.scanResultCache != null) {
            sbuf.append("Scan Cache:  ").append('\n');
            ArrayList<ScanResult> list = sortScanResults();
            if (list.size() > 0) {
                for (ScanResult result : list) {
                    long milli = now_ms - result.seen;
                    long ageSec = 0;
                    long ageMin = 0;
                    long ageHour = 0;
                    long ageMilli = 0;
                    long ageDay = 0;
                    if (now_ms > result.seen && result.seen > 0) {
                        ageMilli = milli % 1000;
                        ageSec = (milli / 1000) % 60;
                        ageMin = (milli / DateUtils.MINUTE_IN_MILLIS) % 60;
                        ageHour = (milli / 3600000) % 24;
                        ageDay = milli / 86400000;
                    }
                    sbuf.append("{").append(result.BSSID).append(",").append(result.frequency);
                    sbuf.append(",").append(String.format("%3d", Integer.valueOf(result.level)));
                    if (result.autoJoinStatus > 0) {
                        sbuf.append(",st=").append(result.autoJoinStatus);
                    }
                    if (ageSec > 0 || ageMilli > 0) {
                        sbuf.append(String.format(",%4d.%02d.%02d.%02d.%03dms", Long.valueOf(ageDay), Long.valueOf(ageHour), Long.valueOf(ageMin), Long.valueOf(ageSec), Long.valueOf(ageMilli)));
                    }
                    if (result.numIpConfigFailures > 0) {
                        sbuf.append(",ipfail=");
                        sbuf.append(result.numIpConfigFailures);
                    }
                    sbuf.append("} ");
                }
                sbuf.append('\n');
            }
        }
        sbuf.append("triggeredLow: ").append(this.numUserTriggeredWifiDisableLowRSSI);
        sbuf.append(" triggeredBad: ").append(this.numUserTriggeredWifiDisableBadRSSI);
        sbuf.append(" triggeredNotHigh: ").append(this.numUserTriggeredWifiDisableNotHighRSSI);
        sbuf.append('\n');
        sbuf.append("ticksLow: ").append(this.numTicksAtLowRSSI);
        sbuf.append(" ticksBad: ").append(this.numTicksAtBadRSSI);
        sbuf.append(" ticksNotHigh: ").append(this.numTicksAtNotHighRSSI);
        sbuf.append('\n');
        sbuf.append("triggeredJoin: ").append(this.numUserTriggeredJoinAttempts);
        sbuf.append('\n');
        sbuf.append("autoJoinBailedDueToLowRssi: ").append(this.autoJoinBailedDueToLowRssi);
        sbuf.append('\n');
        sbuf.append("autoJoinUseAggressiveJoinAttemptThreshold: ");
        sbuf.append(this.autoJoinUseAggressiveJoinAttemptThreshold);
        sbuf.append('\n');
        if (this.wapiPsk != null) {
            sbuf.append(" wapiPsk ").append('*');
        }
        if (this.wapiPskType != null) {
            sbuf.append(" wapiPskType ").append(this.wapiPskType);
        }
        if (this.wapiPkcs12Key != null) {
            sbuf.append(" wapiPkcs12Key ").append('*');
        }
        sbuf.append('\n');
        return sbuf.toString();
    }

    public String getPrintableSsid() {
        if (this.SSID == null) {
            return ProxyInfo.LOCAL_EXCL_LIST;
        }
        int length = this.SSID.length();
        if (length > 2 && this.SSID.charAt(0) == '\"' && this.SSID.charAt(length - 1) == '\"') {
            return this.SSID.substring(1, length - 1);
        }
        if (length > 3 && this.SSID.charAt(0) == 'P' && this.SSID.charAt(1) == '\"' && this.SSID.charAt(length - 1) == '\"') {
            WifiSsid wifiSsid = WifiSsid.createFromAsciiEncoded(this.SSID.substring(2, length - 1));
            return wifiSsid.toString();
        }
        return this.SSID;
    }

    public String getKeyIdForCredentials(WifiConfiguration current) {
        String keyMgmt = null;
        try {
            if (TextUtils.isEmpty(this.SSID)) {
                this.SSID = current.SSID;
            }
            if (this.allowedKeyManagement.cardinality() == 0) {
                this.allowedKeyManagement = current.allowedKeyManagement;
            }
            if (this.allowedKeyManagement.get(2)) {
                keyMgmt = KeyMgmt.strings[2];
            }
            if (this.allowedKeyManagement.get(3)) {
                keyMgmt = keyMgmt + KeyMgmt.strings[3];
            }
            if (TextUtils.isEmpty(keyMgmt)) {
                throw new IllegalStateException("Not an EAP network");
            }
            return trimStringForKeyId(this.SSID) + "_" + keyMgmt + "_" + trimStringForKeyId(this.enterpriseConfig.getKeyId(current != null ? current.enterpriseConfig : null));
        } catch (NullPointerException e) {
            throw new IllegalStateException("Invalid config details");
        }
    }

    private String trimStringForKeyId(String string) {
        return string.replace("\"", ProxyInfo.LOCAL_EXCL_LIST).replace(" ", ProxyInfo.LOCAL_EXCL_LIST);
    }

    private static BitSet readBitSet(Parcel src) {
        int cardinality = src.readInt();
        BitSet set = new BitSet();
        for (int i = 0; i < cardinality; i++) {
            set.set(src.readInt());
        }
        return set;
    }

    private static void writeBitSet(Parcel dest, BitSet set) {
        int nextSetBit = -1;
        dest.writeInt(set.cardinality());
        while (true) {
            nextSetBit = set.nextSetBit(nextSetBit + 1);
            if (nextSetBit != -1) {
                dest.writeInt(nextSetBit);
            } else {
                return;
            }
        }
    }

    public int getAuthType() {
        if (!isValid()) {
            throw new IllegalStateException("Invalid configuration");
        }
        if (this.allowedKeyManagement.get(1)) {
            return 1;
        }
        if (this.allowedKeyManagement.get(4)) {
            return 4;
        }
        if (this.allowedKeyManagement.get(2)) {
            return 2;
        }
        return this.allowedKeyManagement.get(3) ? 3 : 0;
    }

    public String configKey(boolean allowCached) {
        String key;
        if (allowCached && this.mCachedConfigKey != null) {
            String key2 = this.mCachedConfigKey;
            return key2;
        }
        if (this.allowedKeyManagement.get(1)) {
            key = this.SSID + KeyMgmt.strings[1];
        } else if (this.allowedKeyManagement.get(2) || this.allowedKeyManagement.get(3)) {
            key = this.SSID + KeyMgmt.strings[2];
        } else if (this.wepKeys[0] != null) {
            key = this.SSID + "WEP";
        } else {
            key = this.SSID + (this.wapiPsk != null ? "WAPI-PSK" : ProxyInfo.LOCAL_EXCL_LIST) + (this.wapiPkcs12Key != null ? "WAPI-PKCS12" : ProxyInfo.LOCAL_EXCL_LIST) + ((this.wapiRootCert == null && this.wapiUserCert == null) ? ProxyInfo.LOCAL_EXCL_LIST : "WAPI-CERT") + KeyMgmt.strings[0];
        }
        this.mCachedConfigKey = key;
        return key;
    }

    public String configKey() {
        return configKey(false);
    }

    public static String configKey(ScanResult result) {
        String key = "\"" + result.SSID + "\"";
        if (result.capabilities.contains("WEP")) {
            key = key + "-WEP";
        }
        if (result.capabilities.contains("PSK")) {
            key = key + NativeLibraryHelper.CLEAR_ABI_OVERRIDE + KeyMgmt.strings[1];
        }
        if (result.capabilities.contains("EAP")) {
            return key + NativeLibraryHelper.CLEAR_ABI_OVERRIDE + KeyMgmt.strings[2];
        }
        return key;
    }

    public IpConfiguration getIpConfiguration() {
        return this.mIpConfiguration;
    }

    public void setIpConfiguration(IpConfiguration ipConfiguration) {
        this.mIpConfiguration = ipConfiguration;
    }

    public StaticIpConfiguration getStaticIpConfiguration() {
        return this.mIpConfiguration.getStaticIpConfiguration();
    }

    public void setStaticIpConfiguration(StaticIpConfiguration staticIpConfiguration) {
        this.mIpConfiguration.setStaticIpConfiguration(staticIpConfiguration);
    }

    public IpConfiguration.IpAssignment getIpAssignment() {
        return this.mIpConfiguration.ipAssignment;
    }

    public void setIpAssignment(IpConfiguration.IpAssignment ipAssignment) {
        this.mIpConfiguration.ipAssignment = ipAssignment;
    }

    public IpConfiguration.ProxySettings getProxySettings() {
        return this.mIpConfiguration.proxySettings;
    }

    public void setProxySettings(IpConfiguration.ProxySettings proxySettings) {
        this.mIpConfiguration.proxySettings = proxySettings;
    }

    public ProxyInfo getHttpProxy() {
        return this.mIpConfiguration.httpProxy;
    }

    public void setHttpProxy(ProxyInfo httpProxy) {
        this.mIpConfiguration.httpProxy = httpProxy;
    }

    public void setProxy(IpConfiguration.ProxySettings settings, ProxyInfo proxy) {
        this.mIpConfiguration.proxySettings = settings;
        this.mIpConfiguration.httpProxy = proxy;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public WifiConfiguration(WifiConfiguration source) {
        this.NOT_UTF8 = false;
        this.roamingFailureBlackListTimeMilli = 1000L;
        if (source != null) {
            this.networkId = source.networkId;
            this.status = source.status;
            this.disableReason = source.disableReason;
            this.disableReason = source.disableReason;
            this.SSID = source.SSID;
            this.BSSID = source.BSSID;
            this.FQDN = source.FQDN;
            this.naiRealm = source.naiRealm;
            this.preSharedKey = source.preSharedKey;
            this.wapiPsk = source.wapiPsk;
            this.wapiPskType = source.wapiPskType;
            this.wapiPkcs12Key = source.wapiPkcs12Key;
            this.wapiRootCert = source.wapiRootCert;
            this.wapiUserCert = source.wapiUserCert;
            this.wepKeys = new String[4];
            for (int i = 0; i < this.wepKeys.length; i++) {
                this.wepKeys[i] = source.wepKeys[i];
            }
            this.wepTxKeyIndex = source.wepTxKeyIndex;
            this.wifiApChannelIndex = source.wifiApChannelIndex;
            this.priority = source.priority;
            this.hiddenSSID = source.hiddenSSID;
            this.allowedKeyManagement = (BitSet) source.allowedKeyManagement.clone();
            this.allowedProtocols = (BitSet) source.allowedProtocols.clone();
            this.allowedAuthAlgorithms = (BitSet) source.allowedAuthAlgorithms.clone();
            this.allowedPairwiseCiphers = (BitSet) source.allowedPairwiseCiphers.clone();
            this.allowedGroupCiphers = (BitSet) source.allowedGroupCiphers.clone();
            this.enterpriseConfig = new WifiEnterpriseConfig(source.enterpriseConfig);
            this.defaultGwMacAddress = source.defaultGwMacAddress;
            this.mIpConfiguration = new IpConfiguration(source.mIpConfiguration);
            if (source.scanResultCache != null && source.scanResultCache.size() > 0) {
                this.scanResultCache = new HashMap<>();
                this.scanResultCache.putAll(source.scanResultCache);
            }
            if (source.connectChoices != null && source.connectChoices.size() > 0) {
                this.connectChoices = new HashMap<>();
                this.connectChoices.putAll(source.connectChoices);
            }
            if (source.linkedConfigurations != null && source.linkedConfigurations.size() > 0) {
                this.linkedConfigurations = new HashMap<>();
                this.linkedConfigurations.putAll(source.linkedConfigurations);
            }
            this.mCachedConfigKey = null;
            this.autoJoinStatus = source.autoJoinStatus;
            this.selfAdded = source.selfAdded;
            this.validatedInternetAccess = source.validatedInternetAccess;
            this.ephemeral = source.ephemeral;
            if (source.visibility != null) {
                this.visibility = new Visibility(source.visibility);
            }
            this.lastFailure = source.lastFailure;
            this.didSelfAdd = source.didSelfAdd;
            this.lastConnectUid = source.lastConnectUid;
            this.lastUpdateUid = source.lastUpdateUid;
            this.creatorUid = source.creatorUid;
            this.peerWifiConfiguration = source.peerWifiConfiguration;
            this.blackListTimestamp = source.blackListTimestamp;
            this.lastConnected = source.lastConnected;
            this.lastDisconnected = source.lastDisconnected;
            this.lastConnectionFailure = source.lastConnectionFailure;
            this.lastRoamingFailure = source.lastRoamingFailure;
            this.lastRoamingFailureReason = source.lastRoamingFailureReason;
            this.roamingFailureBlackListTimeMilli = source.roamingFailureBlackListTimeMilli;
            this.numConnectionFailures = source.numConnectionFailures;
            this.numIpConfigFailures = source.numIpConfigFailures;
            this.numAuthFailures = source.numAuthFailures;
            this.numScorerOverride = source.numScorerOverride;
            this.numScorerOverrideAndSwitchedNetwork = source.numScorerOverrideAndSwitchedNetwork;
            this.numAssociation = source.numAssociation;
            this.numUserTriggeredWifiDisableLowRSSI = source.numUserTriggeredWifiDisableLowRSSI;
            this.numUserTriggeredWifiDisableBadRSSI = source.numUserTriggeredWifiDisableBadRSSI;
            this.numUserTriggeredWifiDisableNotHighRSSI = source.numUserTriggeredWifiDisableNotHighRSSI;
            this.numTicksAtLowRSSI = source.numTicksAtLowRSSI;
            this.numTicksAtBadRSSI = source.numTicksAtBadRSSI;
            this.numTicksAtNotHighRSSI = source.numTicksAtNotHighRSSI;
            this.numUserTriggeredJoinAttempts = source.numUserTriggeredJoinAttempts;
            this.autoJoinBSSID = source.autoJoinBSSID;
            this.autoJoinUseAggressiveJoinAttemptThreshold = source.autoJoinUseAggressiveJoinAttemptThreshold;
            this.autoJoinBailedDueToLowRssi = source.autoJoinBailedDueToLowRssi;
            this.dirty = source.dirty;
            this.numNoInternetAccessReports = source.numNoInternetAccessReports;
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.networkId);
        dest.writeInt(this.status);
        dest.writeInt(this.disableReason);
        dest.writeString(this.SSID);
        dest.writeString(this.BSSID);
        dest.writeString(this.autoJoinBSSID);
        dest.writeString(this.FQDN);
        dest.writeString(this.naiRealm);
        dest.writeString(this.preSharedKey);
        dest.writeString(this.wapiPsk);
        dest.writeString(this.wapiPskType);
        dest.writeString(this.wapiPkcs12Key);
        dest.writeString(this.wapiRootCert);
        dest.writeString(this.wapiUserCert);
        dest.writeInt(this.wifiApChannelIndex);
        String[] arr$ = this.wepKeys;
        for (String wepKey : arr$) {
            dest.writeString(wepKey);
        }
        dest.writeInt(this.wepTxKeyIndex);
        dest.writeInt(this.priority);
        dest.writeInt(this.hiddenSSID ? 1 : 0);
        dest.writeInt(this.requirePMF ? 1 : 0);
        dest.writeString(this.updateIdentifier);
        writeBitSet(dest, this.allowedKeyManagement);
        writeBitSet(dest, this.allowedProtocols);
        writeBitSet(dest, this.allowedAuthAlgorithms);
        writeBitSet(dest, this.allowedPairwiseCiphers);
        writeBitSet(dest, this.allowedGroupCiphers);
        dest.writeParcelable(this.enterpriseConfig, flags);
        dest.writeParcelable(this.mIpConfiguration, flags);
        dest.writeString(this.dhcpServer);
        dest.writeString(this.defaultGwMacAddress);
        dest.writeInt(this.autoJoinStatus);
        dest.writeInt(this.selfAdded ? 1 : 0);
        dest.writeInt(this.didSelfAdd ? 1 : 0);
        dest.writeInt(this.validatedInternetAccess ? 1 : 0);
        dest.writeInt(this.ephemeral ? 1 : 0);
        dest.writeInt(this.creatorUid);
        dest.writeInt(this.lastConnectUid);
        dest.writeInt(this.lastUpdateUid);
        dest.writeLong(this.blackListTimestamp);
        dest.writeLong(this.lastConnectionFailure);
        dest.writeLong(this.lastRoamingFailure);
        dest.writeInt(this.lastRoamingFailureReason);
        dest.writeLong(this.roamingFailureBlackListTimeMilli);
        dest.writeInt(this.numConnectionFailures);
        dest.writeInt(this.numIpConfigFailures);
        dest.writeInt(this.numAuthFailures);
        dest.writeInt(this.numScorerOverride);
        dest.writeInt(this.numScorerOverrideAndSwitchedNetwork);
        dest.writeInt(this.numAssociation);
        dest.writeInt(this.numUserTriggeredWifiDisableLowRSSI);
        dest.writeInt(this.numUserTriggeredWifiDisableBadRSSI);
        dest.writeInt(this.numUserTriggeredWifiDisableNotHighRSSI);
        dest.writeInt(this.numTicksAtLowRSSI);
        dest.writeInt(this.numTicksAtBadRSSI);
        dest.writeInt(this.numTicksAtNotHighRSSI);
        dest.writeInt(this.numUserTriggeredJoinAttempts);
        dest.writeInt(this.autoJoinUseAggressiveJoinAttemptThreshold);
        dest.writeInt(this.autoJoinBailedDueToLowRssi ? 1 : 0);
        dest.writeInt(this.numNoInternetAccessReports);
    }
}
