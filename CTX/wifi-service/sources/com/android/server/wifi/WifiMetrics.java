package com.android.server.wifi;

import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.util.SparseIntArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.aware.WifiAwareMetrics;
import com.android.server.wifi.hotspot2.NetworkDetail;
import com.android.server.wifi.hotspot2.PasspointManager;
import com.android.server.wifi.nano.WifiMetricsProto;
import com.android.server.wifi.rtt.RttMetrics;
import com.android.server.wifi.util.ScanResultUtil;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WifiMetrics {
    public static final String CLEAN_DUMP_ARG = "clean";
    private static final int CONNECT_TO_NETWORK_NOTIFICATION_ACTION_KEY_MULTIPLIER = 1000;
    private static final boolean DBG = false;

    @VisibleForTesting
    static final int LOW_WIFI_SCORE = 50;
    public static final int MAX_CONNECTABLE_BSSID_NETWORK_BUCKET = 50;
    public static final int MAX_CONNECTABLE_SSID_NETWORK_BUCKET = 20;
    private static final int MAX_CONNECTION_EVENTS = 256;
    private static final int MAX_NUM_SOFT_AP_EVENTS = 256;
    public static final int MAX_PASSPOINT_APS_PER_UNIQUE_ESS_BUCKET = 50;
    public static final int MAX_RSSI_DELTA = 127;
    private static final int MAX_RSSI_POLL = 0;
    public static final int MAX_STA_EVENTS = 768;
    public static final int MAX_TOTAL_80211MC_APS_BUCKET = 20;
    public static final int MAX_TOTAL_PASSPOINT_APS_BUCKET = 50;
    public static final int MAX_TOTAL_PASSPOINT_UNIQUE_ESS_BUCKET = 20;
    public static final int MAX_TOTAL_SCAN_RESULTS_BUCKET = 250;
    public static final int MAX_TOTAL_SCAN_RESULT_SSIDS_BUCKET = 100;
    private static final int MAX_WIFI_SCORE = 60;
    public static final int MIN_RSSI_DELTA = -127;
    private static final int MIN_RSSI_POLL = -127;
    private static final int MIN_WIFI_SCORE = 0;
    public static final String PROTO_DUMP_ARG = "wifiMetricsProto";
    private static final int SCREEN_OFF = 0;
    private static final int SCREEN_ON = 1;
    private static final String TAG = "WifiMetrics";
    public static final long TIMEOUT_RSSI_DELTA_MILLIS = 3000;
    private Clock mClock;
    private Handler mHandler;
    private PasspointManager mPasspointManager;
    private long mRecordStartTimeSec;
    private RttMetrics mRttMetrics;
    private ScoringParams mScoringParams;
    private WifiAwareMetrics mWifiAwareMetrics;
    private WifiConfigManager mWifiConfigManager;
    private WifiNetworkSelector mWifiNetworkSelector;
    private final Object mLock = new Object();
    private final WifiMetricsProto.PnoScanMetrics mPnoScanMetrics = new WifiMetricsProto.PnoScanMetrics();
    private final WifiMetricsProto.WpsMetrics mWpsMetrics = new WifiMetricsProto.WpsMetrics();
    private final WifiMetricsProto.WifiLog mWifiLogProto = new WifiMetricsProto.WifiLog();
    private final List<ConnectionEvent> mConnectionEventList = new ArrayList();
    private final SparseIntArray mScanReturnEntries = new SparseIntArray();
    private final SparseIntArray mWifiSystemStateEntries = new SparseIntArray();
    private final Map<Integer, SparseIntArray> mRssiPollCountsMap = new HashMap();
    private final SparseIntArray mRssiDeltaCounts = new SparseIntArray();
    private int mScanResultRssi = 0;
    private long mScanResultRssiTimestampMillis = -1;
    private final SparseIntArray mWifiAlertReasonCounts = new SparseIntArray();
    private final SparseIntArray mWifiScoreCounts = new SparseIntArray();
    private final SparseIntArray mSoftApManagerReturnCodeCounts = new SparseIntArray();
    private final SparseIntArray mTotalSsidsInScanHistogram = new SparseIntArray();
    private final SparseIntArray mTotalBssidsInScanHistogram = new SparseIntArray();
    private final SparseIntArray mAvailableOpenSsidsInScanHistogram = new SparseIntArray();
    private final SparseIntArray mAvailableOpenBssidsInScanHistogram = new SparseIntArray();
    private final SparseIntArray mAvailableSavedSsidsInScanHistogram = new SparseIntArray();
    private final SparseIntArray mAvailableSavedBssidsInScanHistogram = new SparseIntArray();
    private final SparseIntArray mAvailableOpenOrSavedSsidsInScanHistogram = new SparseIntArray();
    private final SparseIntArray mAvailableOpenOrSavedBssidsInScanHistogram = new SparseIntArray();
    private final SparseIntArray mAvailableSavedPasspointProviderProfilesInScanHistogram = new SparseIntArray();
    private final SparseIntArray mAvailableSavedPasspointProviderBssidsInScanHistogram = new SparseIntArray();
    private final SparseIntArray mConnectToNetworkNotificationCount = new SparseIntArray();
    private final SparseIntArray mConnectToNetworkNotificationActionCount = new SparseIntArray();
    private int mOpenNetworkRecommenderBlacklistSize = 0;
    private boolean mIsWifiNetworksAvailableNotificationOn = DBG;
    private int mNumOpenNetworkConnectMessageFailedToSend = 0;
    private int mNumOpenNetworkRecommendationUpdates = 0;
    private final List<WifiMetricsProto.SoftApConnectedClientsEvent> mSoftApEventListTethered = new ArrayList();
    private final List<WifiMetricsProto.SoftApConnectedClientsEvent> mSoftApEventListLocalOnly = new ArrayList();
    private final SparseIntArray mObservedHotspotR1ApInScanHistogram = new SparseIntArray();
    private final SparseIntArray mObservedHotspotR2ApInScanHistogram = new SparseIntArray();
    private final SparseIntArray mObservedHotspotR1EssInScanHistogram = new SparseIntArray();
    private final SparseIntArray mObservedHotspotR2EssInScanHistogram = new SparseIntArray();
    private final SparseIntArray mObservedHotspotR1ApsPerEssInScanHistogram = new SparseIntArray();
    private final SparseIntArray mObservedHotspotR2ApsPerEssInScanHistogram = new SparseIntArray();
    private final SparseIntArray mObserved80211mcApInScanHistogram = new SparseIntArray();
    private WifiPowerMetrics mWifiPowerMetrics = new WifiPowerMetrics();
    private final WifiWakeMetrics mWifiWakeMetrics = new WifiWakeMetrics();
    private boolean mIsMacRandomizationOn = DBG;
    private boolean mWifiWins = DBG;
    private int mSupplicantStateChangeBitmask = 0;
    private LinkedList<StaEventWithTime> mStaEventList = new LinkedList<>();
    private int mLastPollRssi = -127;
    private int mLastPollLinkSpeed = -1;
    private int mLastPollFreq = -1;
    private int mLastScore = -1;
    private ConnectionEvent mCurrentConnectionEvent = null;
    private boolean mScreenOn = true;
    private int mWifiState = 1;

    class RouterFingerPrint {
        private WifiMetricsProto.RouterFingerPrint mRouterFingerPrintProto = new WifiMetricsProto.RouterFingerPrint();

        RouterFingerPrint() {
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            synchronized (WifiMetrics.this.mLock) {
                sb.append("mConnectionEvent.roamType=" + this.mRouterFingerPrintProto.roamType);
                sb.append(", mChannelInfo=" + this.mRouterFingerPrintProto.channelInfo);
                sb.append(", mDtim=" + this.mRouterFingerPrintProto.dtim);
                sb.append(", mAuthentication=" + this.mRouterFingerPrintProto.authentication);
                sb.append(", mHidden=" + this.mRouterFingerPrintProto.hidden);
                sb.append(", mRouterTechnology=" + this.mRouterFingerPrintProto.routerTechnology);
                sb.append(", mSupportsIpv6=" + this.mRouterFingerPrintProto.supportsIpv6);
            }
            return sb.toString();
        }

        public void updateFromWifiConfiguration(WifiConfiguration wifiConfiguration) {
            synchronized (WifiMetrics.this.mLock) {
                if (wifiConfiguration != null) {
                    try {
                        this.mRouterFingerPrintProto.hidden = wifiConfiguration.hiddenSSID;
                        if (wifiConfiguration.dtimInterval > 0) {
                            this.mRouterFingerPrintProto.dtim = wifiConfiguration.dtimInterval;
                        }
                        WifiMetrics.this.mCurrentConnectionEvent.mConfigSsid = wifiConfiguration.SSID;
                        if (wifiConfiguration.allowedKeyManagement != null && wifiConfiguration.allowedKeyManagement.get(0)) {
                            WifiMetrics.this.mCurrentConnectionEvent.mRouterFingerPrint.mRouterFingerPrintProto.authentication = 1;
                        } else if (wifiConfiguration.isEnterprise()) {
                            WifiMetrics.this.mCurrentConnectionEvent.mRouterFingerPrint.mRouterFingerPrintProto.authentication = 3;
                        } else {
                            WifiMetrics.this.mCurrentConnectionEvent.mRouterFingerPrint.mRouterFingerPrintProto.authentication = 2;
                        }
                        WifiMetrics.this.mCurrentConnectionEvent.mRouterFingerPrint.mRouterFingerPrintProto.passpoint = wifiConfiguration.isPasspoint();
                        ScanResult candidate = wifiConfiguration.getNetworkSelectionStatus().getCandidate();
                        if (candidate != null) {
                            WifiMetrics.this.updateMetricsFromScanResult(candidate);
                        }
                    } catch (Throwable th) {
                        throw th;
                    }
                }
            }
        }
    }

    class ConnectionEvent {
        public static final int FAILURE_ASSOCIATION_REJECTION = 2;
        public static final int FAILURE_ASSOCIATION_TIMED_OUT = 11;
        public static final int FAILURE_AUTHENTICATION_FAILURE = 3;
        public static final int FAILURE_CONNECT_NETWORK_FAILED = 5;
        public static final int FAILURE_DHCP = 10;
        public static final int FAILURE_NETWORK_DISCONNECTION = 6;
        public static final int FAILURE_NEW_CONNECTION_ATTEMPT = 7;
        public static final int FAILURE_NONE = 1;
        public static final int FAILURE_REDUNDANT_CONNECTION_ATTEMPT = 8;
        public static final int FAILURE_ROAM_TIMEOUT = 9;
        public static final int FAILURE_SSID_TEMP_DISABLED = 4;
        public static final int FAILURE_UNKNOWN = 0;
        private String mConfigBssid;
        private String mConfigSsid;
        WifiMetricsProto.ConnectionEvent mConnectionEvent;
        private long mRealEndTime;
        private long mRealStartTime;
        RouterFingerPrint mRouterFingerPrint;
        private boolean mScreenOn;
        private int mWifiState;

        private ConnectionEvent() {
            this.mConnectionEvent = new WifiMetricsProto.ConnectionEvent();
            this.mRealEndTime = 0L;
            this.mRealStartTime = 0L;
            this.mRouterFingerPrint = WifiMetrics.this.new RouterFingerPrint();
            this.mConnectionEvent.routerFingerprint = this.mRouterFingerPrint.mRouterFingerPrintProto;
            this.mConfigSsid = "<NULL>";
            this.mConfigBssid = "<NULL>";
            this.mWifiState = 0;
            this.mScreenOn = WifiMetrics.DBG;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("startTime=");
            Calendar calendar = Calendar.getInstance();
            synchronized (WifiMetrics.this.mLock) {
                calendar.setTimeInMillis(this.mConnectionEvent.startTimeMillis);
                sb.append(this.mConnectionEvent.startTimeMillis == 0 ? "            <null>" : String.format("%tm-%td %tH:%tM:%tS.%tL", calendar, calendar, calendar, calendar, calendar, calendar));
                sb.append(", SSID=");
                sb.append(this.mConfigSsid);
                sb.append(", BSSID=");
                sb.append(this.mConfigBssid);
                sb.append(", durationMillis=");
                sb.append(this.mConnectionEvent.durationTakenToConnectMillis);
                sb.append(", roamType=");
                switch (this.mConnectionEvent.roamType) {
                    case 1:
                        sb.append("ROAM_NONE");
                        break;
                    case 2:
                        sb.append("ROAM_DBDC");
                        break;
                    case 3:
                        sb.append("ROAM_ENTERPRISE");
                        break;
                    case 4:
                        sb.append("ROAM_USER_SELECTED");
                        break;
                    case 5:
                        sb.append("ROAM_UNRELATED");
                        break;
                    default:
                        sb.append("ROAM_UNKNOWN");
                        break;
                }
                sb.append(", connectionResult=");
                sb.append(this.mConnectionEvent.connectionResult);
                sb.append(", level2FailureCode=");
                switch (this.mConnectionEvent.level2FailureCode) {
                    case 1:
                        sb.append("NONE");
                        break;
                    case 2:
                        sb.append("ASSOCIATION_REJECTION");
                        break;
                    case 3:
                        sb.append("AUTHENTICATION_FAILURE");
                        break;
                    case 4:
                        sb.append("SSID_TEMP_DISABLED");
                        break;
                    case 5:
                        sb.append("CONNECT_NETWORK_FAILED");
                        break;
                    case 6:
                        sb.append("NETWORK_DISCONNECTION");
                        break;
                    case 7:
                        sb.append("NEW_CONNECTION_ATTEMPT");
                        break;
                    case 8:
                        sb.append("REDUNDANT_CONNECTION_ATTEMPT");
                        break;
                    case 9:
                        sb.append("ROAM_TIMEOUT");
                        break;
                    case 10:
                        sb.append("DHCP");
                        break;
                    case 11:
                        sb.append("ASSOCIATION_TIMED_OUT");
                        break;
                    default:
                        sb.append("UNKNOWN");
                        break;
                }
                sb.append(", connectivityLevelFailureCode=");
                switch (this.mConnectionEvent.connectivityLevelFailureCode) {
                    case 1:
                        sb.append("NONE");
                        break;
                    case 2:
                        sb.append("DHCP");
                        break;
                    case 3:
                        sb.append("NO_INTERNET");
                        break;
                    case 4:
                        sb.append("UNWANTED");
                        break;
                    default:
                        sb.append("UNKNOWN");
                        break;
                }
                sb.append(", signalStrength=");
                sb.append(this.mConnectionEvent.signalStrength);
                sb.append(", wifiState=");
                switch (this.mWifiState) {
                    case 1:
                        sb.append("WIFI_DISABLED");
                        break;
                    case 2:
                        sb.append("WIFI_DISCONNECTED");
                        break;
                    case 3:
                        sb.append("WIFI_ASSOCIATED");
                        break;
                    default:
                        sb.append("WIFI_UNKNOWN");
                        break;
                }
                sb.append(", screenOn=");
                sb.append(this.mScreenOn);
                sb.append(". mRouterFingerprint: ");
                sb.append(this.mRouterFingerPrint.toString());
            }
            return sb.toString();
        }
    }

    public WifiMetrics(Clock clock, Looper looper, WifiAwareMetrics wifiAwareMetrics, RttMetrics rttMetrics) {
        this.mClock = clock;
        this.mRecordStartTimeSec = this.mClock.getElapsedSinceBootMillis() / 1000;
        this.mWifiAwareMetrics = wifiAwareMetrics;
        this.mRttMetrics = rttMetrics;
        this.mHandler = new Handler(looper) {
            @Override
            public void handleMessage(Message message) {
                synchronized (WifiMetrics.this.mLock) {
                    WifiMetrics.this.processMessage(message);
                }
            }
        };
    }

    public void setScoringParams(ScoringParams scoringParams) {
        this.mScoringParams = scoringParams;
    }

    public void setWifiConfigManager(WifiConfigManager wifiConfigManager) {
        this.mWifiConfigManager = wifiConfigManager;
    }

    public void setWifiNetworkSelector(WifiNetworkSelector wifiNetworkSelector) {
        this.mWifiNetworkSelector = wifiNetworkSelector;
    }

    public void setPasspointManager(PasspointManager passpointManager) {
        this.mPasspointManager = passpointManager;
    }

    public void incrementPnoScanStartAttempCount() {
        synchronized (this.mLock) {
            this.mPnoScanMetrics.numPnoScanAttempts++;
        }
    }

    public void incrementPnoScanFailedCount() {
        synchronized (this.mLock) {
            this.mPnoScanMetrics.numPnoScanFailed++;
        }
    }

    public void incrementPnoScanStartedOverOffloadCount() {
        synchronized (this.mLock) {
            this.mPnoScanMetrics.numPnoScanStartedOverOffload++;
        }
    }

    public void incrementPnoScanFailedOverOffloadCount() {
        synchronized (this.mLock) {
            this.mPnoScanMetrics.numPnoScanFailedOverOffload++;
        }
    }

    public void incrementPnoFoundNetworkEventCount() {
        synchronized (this.mLock) {
            this.mPnoScanMetrics.numPnoFoundNetworkEvents++;
        }
    }

    public void incrementWpsAttemptCount() {
        synchronized (this.mLock) {
            this.mWpsMetrics.numWpsAttempts++;
        }
    }

    public void incrementWpsSuccessCount() {
        synchronized (this.mLock) {
            this.mWpsMetrics.numWpsSuccess++;
        }
    }

    public void incrementWpsStartFailureCount() {
        synchronized (this.mLock) {
            this.mWpsMetrics.numWpsStartFailure++;
        }
    }

    public void incrementWpsOverlapFailureCount() {
        synchronized (this.mLock) {
            this.mWpsMetrics.numWpsOverlapFailure++;
        }
    }

    public void incrementWpsTimeoutFailureCount() {
        synchronized (this.mLock) {
            this.mWpsMetrics.numWpsTimeoutFailure++;
        }
    }

    public void incrementWpsOtherConnectionFailureCount() {
        synchronized (this.mLock) {
            this.mWpsMetrics.numWpsOtherConnectionFailure++;
        }
    }

    public void incrementWpsSupplicantFailureCount() {
        synchronized (this.mLock) {
            this.mWpsMetrics.numWpsSupplicantFailure++;
        }
    }

    public void incrementWpsCancellationCount() {
        synchronized (this.mLock) {
            this.mWpsMetrics.numWpsCancellation++;
        }
    }

    public void startConnectionEvent(WifiConfiguration wifiConfiguration, String str, int i) {
        ScanResult candidate;
        synchronized (this.mLock) {
            if (this.mCurrentConnectionEvent != null) {
                if (this.mCurrentConnectionEvent.mConfigSsid != null && this.mCurrentConnectionEvent.mConfigBssid != null && wifiConfiguration != null && this.mCurrentConnectionEvent.mConfigSsid.equals(wifiConfiguration.SSID) && (this.mCurrentConnectionEvent.mConfigBssid.equals("any") || this.mCurrentConnectionEvent.mConfigBssid.equals(str))) {
                    this.mCurrentConnectionEvent.mConfigBssid = str;
                    endConnectionEvent(8, 1);
                } else {
                    endConnectionEvent(7, 1);
                }
            }
            while (this.mConnectionEventList.size() >= 256) {
                this.mConnectionEventList.remove(0);
            }
            this.mCurrentConnectionEvent = new ConnectionEvent();
            this.mCurrentConnectionEvent.mConnectionEvent.startTimeMillis = this.mClock.getWallClockMillis();
            this.mCurrentConnectionEvent.mConfigBssid = str;
            this.mCurrentConnectionEvent.mConnectionEvent.roamType = i;
            this.mCurrentConnectionEvent.mRouterFingerPrint.updateFromWifiConfiguration(wifiConfiguration);
            this.mCurrentConnectionEvent.mConfigBssid = "any";
            this.mCurrentConnectionEvent.mRealStartTime = this.mClock.getElapsedSinceBootMillis();
            this.mCurrentConnectionEvent.mWifiState = this.mWifiState;
            this.mCurrentConnectionEvent.mScreenOn = this.mScreenOn;
            this.mConnectionEventList.add(this.mCurrentConnectionEvent);
            this.mScanResultRssiTimestampMillis = -1L;
            if (wifiConfiguration != null && (candidate = wifiConfiguration.getNetworkSelectionStatus().getCandidate()) != null) {
                this.mScanResultRssi = candidate.level;
                this.mScanResultRssiTimestampMillis = this.mClock.getElapsedSinceBootMillis();
            }
        }
    }

    public void setConnectionEventRoamType(int i) {
        synchronized (this.mLock) {
            if (this.mCurrentConnectionEvent != null) {
                this.mCurrentConnectionEvent.mConnectionEvent.roamType = i;
            }
        }
    }

    public void setConnectionScanDetail(ScanDetail scanDetail) {
        synchronized (this.mLock) {
            if (this.mCurrentConnectionEvent != null && scanDetail != null) {
                NetworkDetail networkDetail = scanDetail.getNetworkDetail();
                ScanResult scanResult = scanDetail.getScanResult();
                if (networkDetail != null && scanResult != null && this.mCurrentConnectionEvent.mConfigSsid != null) {
                    if (this.mCurrentConnectionEvent.mConfigSsid.equals("\"" + networkDetail.getSSID() + "\"")) {
                        updateMetricsFromNetworkDetail(networkDetail);
                        updateMetricsFromScanResult(scanResult);
                    }
                }
            }
        }
    }

    public void endConnectionEvent(int i, int i2) {
        synchronized (this.mLock) {
            if (this.mCurrentConnectionEvent != null) {
                int i3 = 1;
                if (i != 1 || i2 != 1) {
                    i3 = 0;
                }
                this.mCurrentConnectionEvent.mConnectionEvent.connectionResult = i3;
                this.mCurrentConnectionEvent.mRealEndTime = this.mClock.getElapsedSinceBootMillis();
                this.mCurrentConnectionEvent.mConnectionEvent.durationTakenToConnectMillis = (int) (this.mCurrentConnectionEvent.mRealEndTime - this.mCurrentConnectionEvent.mRealStartTime);
                this.mCurrentConnectionEvent.mConnectionEvent.level2FailureCode = i;
                this.mCurrentConnectionEvent.mConnectionEvent.connectivityLevelFailureCode = i2;
                this.mCurrentConnectionEvent = null;
                if (i3 == 0) {
                    this.mScanResultRssiTimestampMillis = -1L;
                }
            }
        }
    }

    private void updateMetricsFromNetworkDetail(NetworkDetail networkDetail) {
        int i;
        int dtimInterval = networkDetail.getDtimInterval();
        if (dtimInterval > 0) {
            this.mCurrentConnectionEvent.mRouterFingerPrint.mRouterFingerPrintProto.dtim = dtimInterval;
        }
        switch (networkDetail.getWifiMode()) {
            case 0:
                i = 0;
                break;
            case 1:
                i = 1;
                break;
            case 2:
                i = 2;
                break;
            case 3:
                i = 3;
                break;
            case 4:
                i = 4;
                break;
            case 5:
                i = 5;
                break;
            default:
                i = 6;
                break;
        }
        this.mCurrentConnectionEvent.mRouterFingerPrint.mRouterFingerPrintProto.routerTechnology = i;
    }

    private void updateMetricsFromScanResult(ScanResult scanResult) {
        this.mCurrentConnectionEvent.mConnectionEvent.signalStrength = scanResult.level;
        this.mCurrentConnectionEvent.mRouterFingerPrint.mRouterFingerPrintProto.authentication = 1;
        this.mCurrentConnectionEvent.mConfigBssid = scanResult.BSSID;
        if (scanResult.capabilities != null) {
            if (ScanResultUtil.isScanResultForWepNetwork(scanResult) || ScanResultUtil.isScanResultForPskNetwork(scanResult)) {
                this.mCurrentConnectionEvent.mRouterFingerPrint.mRouterFingerPrintProto.authentication = 2;
            } else if (ScanResultUtil.isScanResultForEapNetwork(scanResult)) {
                this.mCurrentConnectionEvent.mRouterFingerPrint.mRouterFingerPrintProto.authentication = 3;
            }
        }
        this.mCurrentConnectionEvent.mRouterFingerPrint.mRouterFingerPrintProto.channelInfo = scanResult.frequency;
    }

    void setIsLocationEnabled(boolean z) {
        synchronized (this.mLock) {
            this.mWifiLogProto.isLocationEnabled = z;
        }
    }

    void setIsScanningAlwaysEnabled(boolean z) {
        synchronized (this.mLock) {
            this.mWifiLogProto.isScanningAlwaysEnabled = z;
        }
    }

    public void incrementNonEmptyScanResultCount() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numNonEmptyScanResults++;
        }
    }

    public void incrementEmptyScanResultCount() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numEmptyScanResults++;
        }
    }

    public void incrementBackgroundScanCount() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numBackgroundScans++;
        }
    }

    public int getBackgroundScanCount() {
        int i;
        synchronized (this.mLock) {
            i = this.mWifiLogProto.numBackgroundScans;
        }
        return i;
    }

    public void incrementOneshotScanCount() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numOneshotScans++;
        }
        incrementWifiSystemScanStateCount(this.mWifiState, this.mScreenOn);
    }

    public void incrementConnectivityOneshotScanCount() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numConnectivityOneshotScans++;
        }
    }

    public int getOneshotScanCount() {
        int i;
        synchronized (this.mLock) {
            i = this.mWifiLogProto.numOneshotScans;
        }
        return i;
    }

    public int getConnectivityOneshotScanCount() {
        int i;
        synchronized (this.mLock) {
            i = this.mWifiLogProto.numConnectivityOneshotScans;
        }
        return i;
    }

    public void incrementExternalAppOneshotScanRequestsCount() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numExternalAppOneshotScanRequests++;
        }
    }

    public void incrementExternalForegroundAppOneshotScanRequestsThrottledCount() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numExternalForegroundAppOneshotScanRequestsThrottled++;
        }
    }

    public void incrementExternalBackgroundAppOneshotScanRequestsThrottledCount() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numExternalBackgroundAppOneshotScanRequestsThrottled++;
        }
    }

    private String returnCodeToString(int i) {
        switch (i) {
            case 0:
                return "SCAN_UNKNOWN";
            case 1:
                return "SCAN_SUCCESS";
            case 2:
                return "SCAN_FAILURE_INTERRUPTED";
            case 3:
                return "SCAN_FAILURE_INVALID_CONFIGURATION";
            case 4:
                return "FAILURE_WIFI_DISABLED";
            default:
                return "<UNKNOWN>";
        }
    }

    public void incrementScanReturnEntry(int i, int i2) {
        synchronized (this.mLock) {
            this.mScanReturnEntries.put(i, this.mScanReturnEntries.get(i) + i2);
        }
    }

    public int getScanReturnEntry(int i) {
        int i2;
        synchronized (this.mLock) {
            i2 = this.mScanReturnEntries.get(i);
        }
        return i2;
    }

    private String wifiSystemStateToString(int i) {
        switch (i) {
            case 0:
                return "WIFI_UNKNOWN";
            case 1:
                return "WIFI_DISABLED";
            case 2:
                return "WIFI_DISCONNECTED";
            case 3:
                return "WIFI_ASSOCIATED";
            default:
                return HalDeviceManager.HAL_INSTANCE_NAME;
        }
    }

    public void incrementWifiSystemScanStateCount(int i, boolean z) {
        synchronized (this.mLock) {
            int i2 = (i * 2) + (z ? 1 : 0);
            this.mWifiSystemStateEntries.put(i2, this.mWifiSystemStateEntries.get(i2) + 1);
        }
    }

    public int getSystemStateCount(int i, boolean z) {
        int i2;
        synchronized (this.mLock) {
            i2 = this.mWifiSystemStateEntries.get((i * 2) + (z ? 1 : 0));
        }
        return i2;
    }

    public void incrementNumLastResortWatchdogTriggers() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numLastResortWatchdogTriggers++;
        }
    }

    public void addCountToNumLastResortWatchdogBadAssociationNetworksTotal(int i) {
        synchronized (this.mLock) {
            this.mWifiLogProto.numLastResortWatchdogBadAssociationNetworksTotal += i;
        }
    }

    public void addCountToNumLastResortWatchdogBadAuthenticationNetworksTotal(int i) {
        synchronized (this.mLock) {
            this.mWifiLogProto.numLastResortWatchdogBadAuthenticationNetworksTotal += i;
        }
    }

    public void addCountToNumLastResortWatchdogBadDhcpNetworksTotal(int i) {
        synchronized (this.mLock) {
            this.mWifiLogProto.numLastResortWatchdogBadDhcpNetworksTotal += i;
        }
    }

    public void addCountToNumLastResortWatchdogBadOtherNetworksTotal(int i) {
        synchronized (this.mLock) {
            this.mWifiLogProto.numLastResortWatchdogBadOtherNetworksTotal += i;
        }
    }

    public void addCountToNumLastResortWatchdogAvailableNetworksTotal(int i) {
        synchronized (this.mLock) {
            this.mWifiLogProto.numLastResortWatchdogAvailableNetworksTotal += i;
        }
    }

    public void incrementNumLastResortWatchdogTriggersWithBadAssociation() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numLastResortWatchdogTriggersWithBadAssociation++;
        }
    }

    public void incrementNumLastResortWatchdogTriggersWithBadAuthentication() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numLastResortWatchdogTriggersWithBadAuthentication++;
        }
    }

    public void incrementNumLastResortWatchdogTriggersWithBadDhcp() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numLastResortWatchdogTriggersWithBadDhcp++;
        }
    }

    public void incrementNumLastResortWatchdogTriggersWithBadOther() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numLastResortWatchdogTriggersWithBadOther++;
        }
    }

    public void incrementNumConnectivityWatchdogPnoGood() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numConnectivityWatchdogPnoGood++;
        }
    }

    public void incrementNumConnectivityWatchdogPnoBad() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numConnectivityWatchdogPnoBad++;
        }
    }

    public void incrementNumConnectivityWatchdogBackgroundGood() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numConnectivityWatchdogBackgroundGood++;
        }
    }

    public void incrementNumConnectivityWatchdogBackgroundBad() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numConnectivityWatchdogBackgroundBad++;
        }
    }

    public void handlePollResult(WifiInfo wifiInfo) {
        this.mLastPollRssi = wifiInfo.getRssi();
        this.mLastPollLinkSpeed = wifiInfo.getLinkSpeed();
        this.mLastPollFreq = wifiInfo.getFrequency();
        incrementRssiPollRssiCount(this.mLastPollFreq, this.mLastPollRssi);
    }

    @VisibleForTesting
    public void incrementRssiPollRssiCount(int i, int i2) {
        if (i2 < -127 || i2 > 0) {
            return;
        }
        synchronized (this.mLock) {
            if (!this.mRssiPollCountsMap.containsKey(Integer.valueOf(i))) {
                this.mRssiPollCountsMap.put(Integer.valueOf(i), new SparseIntArray());
            }
            SparseIntArray sparseIntArray = this.mRssiPollCountsMap.get(Integer.valueOf(i));
            sparseIntArray.put(i2, sparseIntArray.get(i2) + 1);
            maybeIncrementRssiDeltaCount(i2 - this.mScanResultRssi);
        }
    }

    private void maybeIncrementRssiDeltaCount(int i) {
        if (this.mScanResultRssiTimestampMillis >= 0) {
            if (this.mClock.getElapsedSinceBootMillis() - this.mScanResultRssiTimestampMillis <= TIMEOUT_RSSI_DELTA_MILLIS && i >= -127 && i <= 127) {
                this.mRssiDeltaCounts.put(i, this.mRssiDeltaCounts.get(i) + 1);
            }
            this.mScanResultRssiTimestampMillis = -1L;
        }
    }

    public void incrementNumLastResortWatchdogSuccesses() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numLastResortWatchdogSuccesses++;
        }
    }

    public void incrementWatchdogTotalConnectionFailureCountAfterTrigger() {
        synchronized (this.mLock) {
            this.mWifiLogProto.watchdogTotalConnectionFailureCountAfterTrigger++;
        }
    }

    public void setWatchdogSuccessTimeDurationMs(long j) {
        synchronized (this.mLock) {
            this.mWifiLogProto.watchdogTriggerToConnectionSuccessDurationMs = j;
        }
    }

    public void incrementAlertReasonCount(int i) {
        if (i > 64 || i < 0) {
            i = 0;
        }
        synchronized (this.mLock) {
            this.mWifiAlertReasonCounts.put(i, this.mWifiAlertReasonCounts.get(i) + 1);
        }
    }

    public void countScanResults(List<ScanDetail> list) {
        if (list == null) {
            return;
        }
        int i = 0;
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        int i5 = 0;
        int i6 = 0;
        int i7 = 0;
        for (ScanDetail scanDetail : list) {
            NetworkDetail networkDetail = scanDetail.getNetworkDetail();
            ScanResult scanResult = scanDetail.getScanResult();
            i++;
            if (networkDetail != null) {
                if (networkDetail.isHiddenBeaconFrame()) {
                    i2++;
                }
                if (networkDetail.getHSRelease() != null) {
                    if (networkDetail.getHSRelease() == NetworkDetail.HSRelease.R1) {
                        i3++;
                    } else if (networkDetail.getHSRelease() == NetworkDetail.HSRelease.R2) {
                        i4++;
                    }
                }
            }
            if (scanResult != null && scanResult.capabilities != null) {
                if (ScanResultUtil.isScanResultForEapNetwork(scanResult)) {
                    i5++;
                } else if (ScanResultUtil.isScanResultForPskNetwork(scanResult) || ScanResultUtil.isScanResultForWepNetwork(scanResult)) {
                    i6++;
                } else {
                    i7++;
                }
            }
        }
        synchronized (this.mLock) {
            this.mWifiLogProto.numTotalScanResults += i;
            this.mWifiLogProto.numOpenNetworkScanResults += i7;
            this.mWifiLogProto.numPersonalNetworkScanResults += i6;
            this.mWifiLogProto.numEnterpriseNetworkScanResults += i5;
            this.mWifiLogProto.numHiddenNetworkScanResults += i2;
            this.mWifiLogProto.numHotspot2R1NetworkScanResults += i3;
            this.mWifiLogProto.numHotspot2R2NetworkScanResults += i4;
            this.mWifiLogProto.numScans++;
        }
    }

    public void incrementWifiScoreCount(int i) {
        if (i < 0 || i > 60) {
            return;
        }
        synchronized (this.mLock) {
            boolean z = true;
            this.mWifiScoreCounts.put(i, this.mWifiScoreCounts.get(i) + 1);
            boolean z2 = this.mWifiWins;
            if (this.mWifiWins && i < 50) {
                z = DBG;
            } else if (this.mWifiWins || i <= 50) {
                z = z2;
            }
            this.mLastScore = i;
            if (z != this.mWifiWins) {
                this.mWifiWins = z;
                WifiMetricsProto.StaEvent staEvent = new WifiMetricsProto.StaEvent();
                staEvent.type = 16;
                addStaEvent(staEvent);
            }
        }
    }

    public void incrementSoftApStartResult(boolean z, int i) {
        synchronized (this.mLock) {
            try {
                if (z) {
                    this.mSoftApManagerReturnCodeCounts.put(1, this.mSoftApManagerReturnCodeCounts.get(1) + 1);
                    return;
                }
                if (i == 1) {
                    this.mSoftApManagerReturnCodeCounts.put(3, this.mSoftApManagerReturnCodeCounts.get(3) + 1);
                } else {
                    this.mSoftApManagerReturnCodeCounts.put(2, this.mSoftApManagerReturnCodeCounts.get(2) + 1);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public void addSoftApUpChangedEvent(boolean z, int i) {
        WifiMetricsProto.SoftApConnectedClientsEvent softApConnectedClientsEvent = new WifiMetricsProto.SoftApConnectedClientsEvent();
        softApConnectedClientsEvent.eventType = z ? 0 : 1;
        softApConnectedClientsEvent.numConnectedClients = 0;
        addSoftApConnectedClientsEvent(softApConnectedClientsEvent, i);
    }

    public void addSoftApNumAssociatedStationsChangedEvent(int i, int i2) {
        WifiMetricsProto.SoftApConnectedClientsEvent softApConnectedClientsEvent = new WifiMetricsProto.SoftApConnectedClientsEvent();
        softApConnectedClientsEvent.eventType = 2;
        softApConnectedClientsEvent.numConnectedClients = i;
        addSoftApConnectedClientsEvent(softApConnectedClientsEvent, i2);
    }

    private void addSoftApConnectedClientsEvent(WifiMetricsProto.SoftApConnectedClientsEvent softApConnectedClientsEvent, int i) {
        List<WifiMetricsProto.SoftApConnectedClientsEvent> list;
        synchronized (this.mLock) {
            switch (i) {
                case 1:
                    list = this.mSoftApEventListTethered;
                    break;
                case 2:
                    list = this.mSoftApEventListLocalOnly;
                    break;
                default:
                    return;
            }
            if (list.size() > 256) {
                return;
            }
            softApConnectedClientsEvent.timeStampMillis = this.mClock.getElapsedSinceBootMillis();
            list.add(softApConnectedClientsEvent);
        }
    }

    public void addSoftApChannelSwitchedEvent(int i, int i2, int i3) {
        List<WifiMetricsProto.SoftApConnectedClientsEvent> list;
        synchronized (this.mLock) {
            switch (i3) {
                case 1:
                    list = this.mSoftApEventListTethered;
                    break;
                case 2:
                    list = this.mSoftApEventListLocalOnly;
                    break;
                default:
                    return;
            }
            int size = list.size() - 1;
            while (true) {
                if (size >= 0) {
                    WifiMetricsProto.SoftApConnectedClientsEvent softApConnectedClientsEvent = list.get(size);
                    if (softApConnectedClientsEvent == null || softApConnectedClientsEvent.eventType != 0) {
                        size--;
                    } else {
                        softApConnectedClientsEvent.channelFrequency = i;
                        softApConnectedClientsEvent.channelBandwidth = i2;
                    }
                }
            }
        }
    }

    public void incrementNumHalCrashes() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numHalCrashes++;
        }
    }

    public void incrementNumWificondCrashes() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numWificondCrashes++;
        }
    }

    public void incrementNumSupplicantCrashes() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numSupplicantCrashes++;
        }
    }

    public void incrementNumHostapdCrashes() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numHostapdCrashes++;
        }
    }

    public void incrementNumSetupClientInterfaceFailureDueToHal() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numSetupClientInterfaceFailureDueToHal++;
        }
    }

    public void incrementNumSetupClientInterfaceFailureDueToWificond() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numSetupClientInterfaceFailureDueToWificond++;
        }
    }

    public void incrementNumSetupClientInterfaceFailureDueToSupplicant() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numSetupClientInterfaceFailureDueToSupplicant++;
        }
    }

    public void incrementNumSetupSoftApInterfaceFailureDueToHal() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numSetupSoftApInterfaceFailureDueToHal++;
        }
    }

    public void incrementNumSetupSoftApInterfaceFailureDueToWificond() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numSetupSoftApInterfaceFailureDueToWificond++;
        }
    }

    public void incrementNumSetupSoftApInterfaceFailureDueToHostapd() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numSetupSoftApInterfaceFailureDueToHostapd++;
        }
    }

    public void incrementNumClientInterfaceDown() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numClientInterfaceDown++;
        }
    }

    public void incrementNumSoftApInterfaceDown() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numSoftApInterfaceDown++;
        }
    }

    public void incrementNumPasspointProviderInstallation() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numPasspointProviderInstallation++;
        }
    }

    public void incrementNumPasspointProviderInstallSuccess() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numPasspointProviderInstallSuccess++;
        }
    }

    public void incrementNumPasspointProviderUninstallation() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numPasspointProviderUninstallation++;
        }
    }

    public void incrementNumPasspointProviderUninstallSuccess() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numPasspointProviderUninstallSuccess++;
        }
    }

    public void incrementNumRadioModeChangeToMcc() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numRadioModeChangeToMcc++;
        }
    }

    public void incrementNumRadioModeChangeToScc() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numRadioModeChangeToScc++;
        }
    }

    public void incrementNumRadioModeChangeToSbs() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numRadioModeChangeToSbs++;
        }
    }

    public void incrementNumRadioModeChangeToDbs() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numRadioModeChangeToDbs++;
        }
    }

    public void incrementNumSoftApUserBandPreferenceUnsatisfied() {
        synchronized (this.mLock) {
            this.mWifiLogProto.numSoftApUserBandPreferenceUnsatisfied++;
        }
    }

    public void incrementAvailableNetworksHistograms(List<ScanDetail> list, boolean z) {
        synchronized (this.mLock) {
            if (this.mWifiConfigManager != null && this.mWifiNetworkSelector != null && this.mPasspointManager != null) {
                if (!z) {
                    this.mWifiLogProto.partialAllSingleScanListenerResults++;
                    return;
                }
                HashSet hashSet = new HashSet();
                HashSet hashSet2 = new HashSet();
                HashSet hashSet3 = new HashSet();
                HashSet hashSet4 = new HashSet();
                HashMap map = new HashMap();
                HashMap map2 = new HashMap();
                Iterator<ScanDetail> it = list.iterator();
                int i = 0;
                int i2 = 0;
                int i3 = 0;
                int i4 = 0;
                int i5 = 0;
                while (it.hasNext()) {
                    ScanDetail next = it.next();
                    NetworkDetail networkDetail = next.getNetworkDetail();
                    Iterator<ScanDetail> it2 = it;
                    ScanResult scanResult = next.getScanResult();
                    if (networkDetail.is80211McResponderSupport()) {
                        i++;
                    }
                    ScanResultMatchInfo scanResultMatchInfoFromScanResult = ScanResultMatchInfo.fromScanResult(scanResult);
                    int i6 = i;
                    if (!this.mWifiNetworkSelector.isSignalTooWeak(scanResult)) {
                        hashSet.add(scanResultMatchInfoFromScanResult);
                        i2++;
                        boolean z2 = scanResultMatchInfoFromScanResult.networkType == 0 ? true : DBG;
                        WifiConfiguration configuredNetworkForScanDetail = this.mWifiConfigManager.getConfiguredNetworkForScanDetail(next);
                        boolean z3 = (configuredNetworkForScanDetail == null || configuredNetworkForScanDetail.isEphemeral() || configuredNetworkForScanDetail.isPasspoint()) ? DBG : true;
                        if (z2) {
                            hashSet2.add(scanResultMatchInfoFromScanResult);
                            i3++;
                        }
                        if (z3) {
                            hashSet3.add(scanResultMatchInfoFromScanResult);
                            i4++;
                        }
                        if (z2 || z3) {
                            i5++;
                        }
                    }
                    it = it2;
                    i = i6;
                }
                this.mWifiLogProto.fullBandAllSingleScanListenerResults++;
                incrementTotalScanSsids(this.mTotalSsidsInScanHistogram, hashSet.size());
                incrementTotalScanResults(this.mTotalBssidsInScanHistogram, i2);
                incrementSsid(this.mAvailableOpenSsidsInScanHistogram, hashSet2.size());
                incrementBssid(this.mAvailableOpenBssidsInScanHistogram, i3);
                incrementSsid(this.mAvailableSavedSsidsInScanHistogram, hashSet3.size());
                incrementBssid(this.mAvailableSavedBssidsInScanHistogram, i4);
                hashSet2.addAll(hashSet3);
                incrementSsid(this.mAvailableOpenOrSavedSsidsInScanHistogram, hashSet2.size());
                incrementBssid(this.mAvailableOpenOrSavedBssidsInScanHistogram, i5);
                incrementSsid(this.mAvailableSavedPasspointProviderProfilesInScanHistogram, hashSet4.size());
                incrementBssid(this.mAvailableSavedPasspointProviderBssidsInScanHistogram, 0);
                incrementTotalPasspointAps(this.mObservedHotspotR1ApInScanHistogram, 0);
                incrementTotalPasspointAps(this.mObservedHotspotR2ApInScanHistogram, 0);
                incrementTotalUniquePasspointEss(this.mObservedHotspotR1EssInScanHistogram, map.size());
                incrementTotalUniquePasspointEss(this.mObservedHotspotR2EssInScanHistogram, map2.size());
                Iterator it3 = map.values().iterator();
                while (it3.hasNext()) {
                    incrementPasspointPerUniqueEss(this.mObservedHotspotR1ApsPerEssInScanHistogram, ((Integer) it3.next()).intValue());
                }
                Iterator it4 = map2.values().iterator();
                while (it4.hasNext()) {
                    incrementPasspointPerUniqueEss(this.mObservedHotspotR2ApsPerEssInScanHistogram, ((Integer) it4.next()).intValue());
                }
                increment80211mcAps(this.mObserved80211mcApInScanHistogram, i);
            }
        }
    }

    public void incrementConnectToNetworkNotification(String str, int i) {
        synchronized (this.mLock) {
            this.mConnectToNetworkNotificationCount.put(i, this.mConnectToNetworkNotificationCount.get(i) + 1);
        }
    }

    public void incrementConnectToNetworkNotificationAction(String str, int i, int i2) {
        synchronized (this.mLock) {
            int i3 = (i * CONNECT_TO_NETWORK_NOTIFICATION_ACTION_KEY_MULTIPLIER) + i2;
            this.mConnectToNetworkNotificationActionCount.put(i3, this.mConnectToNetworkNotificationActionCount.get(i3) + 1);
        }
    }

    public void setNetworkRecommenderBlacklistSize(String str, int i) {
        synchronized (this.mLock) {
            this.mOpenNetworkRecommenderBlacklistSize = i;
        }
    }

    public void setIsWifiNetworksAvailableNotificationEnabled(String str, boolean z) {
        synchronized (this.mLock) {
            this.mIsWifiNetworksAvailableNotificationOn = z;
        }
    }

    public void incrementNumNetworkRecommendationUpdates(String str) {
        synchronized (this.mLock) {
            this.mNumOpenNetworkRecommendationUpdates++;
        }
    }

    public void incrementNumNetworkConnectMessageFailedToSend(String str) {
        synchronized (this.mLock) {
            this.mNumOpenNetworkConnectMessageFailedToSend++;
        }
    }

    public void setIsMacRandomizationOn(boolean z) {
        synchronized (this.mLock) {
            this.mIsMacRandomizationOn = z;
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        synchronized (this.mLock) {
            consolidateScoringParams();
            if (strArr != null && strArr.length > 0 && PROTO_DUMP_ARG.equals(strArr[0])) {
                consolidateProto(true);
                for (ConnectionEvent connectionEvent : this.mConnectionEventList) {
                    if (this.mCurrentConnectionEvent != connectionEvent) {
                        connectionEvent.mConnectionEvent.automaticBugReportTaken = true;
                    }
                }
                String strEncodeToString = Base64.encodeToString(WifiMetricsProto.WifiLog.toByteArray(this.mWifiLogProto), 0);
                if (strArr.length > 1 && CLEAN_DUMP_ARG.equals(strArr[1])) {
                    printWriter.print(strEncodeToString);
                } else {
                    printWriter.println("WifiMetrics:");
                    printWriter.println(strEncodeToString);
                    printWriter.println("EndWifiMetrics");
                }
                clear();
            } else {
                printWriter.println("WifiMetrics:");
                printWriter.println("mConnectionEvents:");
                for (ConnectionEvent connectionEvent2 : this.mConnectionEventList) {
                    String string = connectionEvent2.toString();
                    if (connectionEvent2 == this.mCurrentConnectionEvent) {
                        string = string + "CURRENTLY OPEN EVENT";
                    }
                    printWriter.println(string);
                }
                printWriter.println("mWifiLogProto.numSavedNetworks=" + this.mWifiLogProto.numSavedNetworks);
                printWriter.println("mWifiLogProto.numOpenNetworks=" + this.mWifiLogProto.numOpenNetworks);
                printWriter.println("mWifiLogProto.numPersonalNetworks=" + this.mWifiLogProto.numPersonalNetworks);
                printWriter.println("mWifiLogProto.numEnterpriseNetworks=" + this.mWifiLogProto.numEnterpriseNetworks);
                printWriter.println("mWifiLogProto.numHiddenNetworks=" + this.mWifiLogProto.numHiddenNetworks);
                printWriter.println("mWifiLogProto.numPasspointNetworks=" + this.mWifiLogProto.numPasspointNetworks);
                printWriter.println("mWifiLogProto.isLocationEnabled=" + this.mWifiLogProto.isLocationEnabled);
                printWriter.println("mWifiLogProto.isScanningAlwaysEnabled=" + this.mWifiLogProto.isScanningAlwaysEnabled);
                printWriter.println("mWifiLogProto.numNetworksAddedByUser=" + this.mWifiLogProto.numNetworksAddedByUser);
                printWriter.println("mWifiLogProto.numNetworksAddedByApps=" + this.mWifiLogProto.numNetworksAddedByApps);
                printWriter.println("mWifiLogProto.numNonEmptyScanResults=" + this.mWifiLogProto.numNonEmptyScanResults);
                printWriter.println("mWifiLogProto.numEmptyScanResults=" + this.mWifiLogProto.numEmptyScanResults);
                printWriter.println("mWifiLogProto.numConnecitvityOneshotScans=" + this.mWifiLogProto.numConnectivityOneshotScans);
                printWriter.println("mWifiLogProto.numOneshotScans=" + this.mWifiLogProto.numOneshotScans);
                printWriter.println("mWifiLogProto.numBackgroundScans=" + this.mWifiLogProto.numBackgroundScans);
                printWriter.println("mWifiLogProto.numExternalAppOneshotScanRequests=" + this.mWifiLogProto.numExternalAppOneshotScanRequests);
                printWriter.println("mWifiLogProto.numExternalForegroundAppOneshotScanRequestsThrottled=" + this.mWifiLogProto.numExternalForegroundAppOneshotScanRequestsThrottled);
                printWriter.println("mWifiLogProto.numExternalBackgroundAppOneshotScanRequestsThrottled=" + this.mWifiLogProto.numExternalBackgroundAppOneshotScanRequestsThrottled);
                printWriter.println("mScanReturnEntries:");
                printWriter.println("  SCAN_UNKNOWN: " + getScanReturnEntry(0));
                printWriter.println("  SCAN_SUCCESS: " + getScanReturnEntry(1));
                printWriter.println("  SCAN_FAILURE_INTERRUPTED: " + getScanReturnEntry(2));
                printWriter.println("  SCAN_FAILURE_INVALID_CONFIGURATION: " + getScanReturnEntry(3));
                printWriter.println("  FAILURE_WIFI_DISABLED: " + getScanReturnEntry(4));
                printWriter.println("mSystemStateEntries: <state><screenOn> : <scansInitiated>");
                printWriter.println("  WIFI_UNKNOWN       ON: " + getSystemStateCount(0, true));
                printWriter.println("  WIFI_DISABLED      ON: " + getSystemStateCount(1, true));
                printWriter.println("  WIFI_DISCONNECTED  ON: " + getSystemStateCount(2, true));
                printWriter.println("  WIFI_ASSOCIATED    ON: " + getSystemStateCount(3, true));
                printWriter.println("  WIFI_UNKNOWN      OFF: " + getSystemStateCount(0, DBG));
                printWriter.println("  WIFI_DISABLED     OFF: " + getSystemStateCount(1, DBG));
                printWriter.println("  WIFI_DISCONNECTED OFF: " + getSystemStateCount(2, DBG));
                printWriter.println("  WIFI_ASSOCIATED   OFF: " + getSystemStateCount(3, DBG));
                printWriter.println("mWifiLogProto.numConnectivityWatchdogPnoGood=" + this.mWifiLogProto.numConnectivityWatchdogPnoGood);
                printWriter.println("mWifiLogProto.numConnectivityWatchdogPnoBad=" + this.mWifiLogProto.numConnectivityWatchdogPnoBad);
                printWriter.println("mWifiLogProto.numConnectivityWatchdogBackgroundGood=" + this.mWifiLogProto.numConnectivityWatchdogBackgroundGood);
                printWriter.println("mWifiLogProto.numConnectivityWatchdogBackgroundBad=" + this.mWifiLogProto.numConnectivityWatchdogBackgroundBad);
                printWriter.println("mWifiLogProto.numLastResortWatchdogTriggers=" + this.mWifiLogProto.numLastResortWatchdogTriggers);
                printWriter.println("mWifiLogProto.numLastResortWatchdogBadAssociationNetworksTotal=" + this.mWifiLogProto.numLastResortWatchdogBadAssociationNetworksTotal);
                printWriter.println("mWifiLogProto.numLastResortWatchdogBadAuthenticationNetworksTotal=" + this.mWifiLogProto.numLastResortWatchdogBadAuthenticationNetworksTotal);
                printWriter.println("mWifiLogProto.numLastResortWatchdogBadDhcpNetworksTotal=" + this.mWifiLogProto.numLastResortWatchdogBadDhcpNetworksTotal);
                printWriter.println("mWifiLogProto.numLastResortWatchdogBadOtherNetworksTotal=" + this.mWifiLogProto.numLastResortWatchdogBadOtherNetworksTotal);
                printWriter.println("mWifiLogProto.numLastResortWatchdogAvailableNetworksTotal=" + this.mWifiLogProto.numLastResortWatchdogAvailableNetworksTotal);
                printWriter.println("mWifiLogProto.numLastResortWatchdogTriggersWithBadAssociation=" + this.mWifiLogProto.numLastResortWatchdogTriggersWithBadAssociation);
                printWriter.println("mWifiLogProto.numLastResortWatchdogTriggersWithBadAuthentication=" + this.mWifiLogProto.numLastResortWatchdogTriggersWithBadAuthentication);
                printWriter.println("mWifiLogProto.numLastResortWatchdogTriggersWithBadDhcp=" + this.mWifiLogProto.numLastResortWatchdogTriggersWithBadDhcp);
                printWriter.println("mWifiLogProto.numLastResortWatchdogTriggersWithBadOther=" + this.mWifiLogProto.numLastResortWatchdogTriggersWithBadOther);
                printWriter.println("mWifiLogProto.numLastResortWatchdogSuccesses=" + this.mWifiLogProto.numLastResortWatchdogSuccesses);
                printWriter.println("mWifiLogProto.recordDurationSec=" + ((this.mClock.getElapsedSinceBootMillis() / 1000) - this.mRecordStartTimeSec));
                try {
                    JSONObject jSONObject = new JSONObject();
                    for (Map.Entry<Integer, SparseIntArray> entry : this.mRssiPollCountsMap.entrySet()) {
                        int iIntValue = entry.getKey().intValue();
                        SparseIntArray value = entry.getValue();
                        JSONArray jSONArray = new JSONArray();
                        for (int i = -127; i <= 0; i++) {
                            int i2 = value.get(i);
                            if (i2 != 0) {
                                JSONObject jSONObject2 = new JSONObject();
                                jSONObject2.put(Integer.toString(i), i2);
                                jSONArray.put(jSONObject2);
                            }
                        }
                        jSONObject.put(Integer.toString(iIntValue), jSONArray);
                    }
                    printWriter.println("mWifiLogProto.rssiPollCount: " + jSONObject.toString());
                } catch (JSONException e) {
                    printWriter.println("JSONException occurred: " + e.getMessage());
                }
                printWriter.println("mWifiLogProto.rssiPollDeltaCount: Printing counts for [-127, 127]");
                StringBuilder sb = new StringBuilder();
                for (int i3 = -127; i3 <= 127; i3++) {
                    sb.append(this.mRssiDeltaCounts.get(i3) + " ");
                }
                printWriter.println("  " + sb.toString());
                printWriter.print("mWifiLogProto.alertReasonCounts=");
                sb.setLength(0);
                for (int i4 = 0; i4 <= 64; i4++) {
                    int i5 = this.mWifiAlertReasonCounts.get(i4);
                    if (i5 > 0) {
                        sb.append("(" + i4 + "," + i5 + "),");
                    }
                }
                if (sb.length() > 1) {
                    sb.setLength(sb.length() - 1);
                    printWriter.println(sb.toString());
                } else {
                    printWriter.println("()");
                }
                printWriter.println("mWifiLogProto.numTotalScanResults=" + this.mWifiLogProto.numTotalScanResults);
                printWriter.println("mWifiLogProto.numOpenNetworkScanResults=" + this.mWifiLogProto.numOpenNetworkScanResults);
                printWriter.println("mWifiLogProto.numPersonalNetworkScanResults=" + this.mWifiLogProto.numPersonalNetworkScanResults);
                printWriter.println("mWifiLogProto.numEnterpriseNetworkScanResults=" + this.mWifiLogProto.numEnterpriseNetworkScanResults);
                printWriter.println("mWifiLogProto.numHiddenNetworkScanResults=" + this.mWifiLogProto.numHiddenNetworkScanResults);
                printWriter.println("mWifiLogProto.numHotspot2R1NetworkScanResults=" + this.mWifiLogProto.numHotspot2R1NetworkScanResults);
                printWriter.println("mWifiLogProto.numHotspot2R2NetworkScanResults=" + this.mWifiLogProto.numHotspot2R2NetworkScanResults);
                printWriter.println("mWifiLogProto.numScans=" + this.mWifiLogProto.numScans);
                printWriter.println("mWifiLogProto.WifiScoreCount: [0, 60]");
                for (int i6 = 0; i6 <= 60; i6++) {
                    printWriter.print(this.mWifiScoreCounts.get(i6) + " ");
                }
                printWriter.println();
                printWriter.println("mWifiLogProto.SoftApManagerReturnCodeCounts:");
                printWriter.println("  SUCCESS: " + this.mSoftApManagerReturnCodeCounts.get(1));
                printWriter.println("  FAILED_GENERAL_ERROR: " + this.mSoftApManagerReturnCodeCounts.get(2));
                printWriter.println("  FAILED_NO_CHANNEL: " + this.mSoftApManagerReturnCodeCounts.get(3));
                printWriter.print("\n");
                printWriter.println("mWifiLogProto.numHalCrashes=" + this.mWifiLogProto.numHalCrashes);
                printWriter.println("mWifiLogProto.numWificondCrashes=" + this.mWifiLogProto.numWificondCrashes);
                printWriter.println("mWifiLogProto.numSupplicantCrashes=" + this.mWifiLogProto.numSupplicantCrashes);
                printWriter.println("mWifiLogProto.numHostapdCrashes=" + this.mWifiLogProto.numHostapdCrashes);
                printWriter.println("mWifiLogProto.numSetupClientInterfaceFailureDueToHal=" + this.mWifiLogProto.numSetupClientInterfaceFailureDueToHal);
                printWriter.println("mWifiLogProto.numSetupClientInterfaceFailureDueToWificond=" + this.mWifiLogProto.numSetupClientInterfaceFailureDueToWificond);
                printWriter.println("mWifiLogProto.numSetupClientInterfaceFailureDueToSupplicant=" + this.mWifiLogProto.numSetupClientInterfaceFailureDueToSupplicant);
                printWriter.println("mWifiLogProto.numSetupSoftApInterfaceFailureDueToHal=" + this.mWifiLogProto.numSetupSoftApInterfaceFailureDueToHal);
                printWriter.println("mWifiLogProto.numSetupSoftApInterfaceFailureDueToWificond=" + this.mWifiLogProto.numSetupSoftApInterfaceFailureDueToWificond);
                printWriter.println("mWifiLogProto.numSetupSoftApInterfaceFailureDueToHostapd=" + this.mWifiLogProto.numSetupSoftApInterfaceFailureDueToHostapd);
                printWriter.println("StaEventList:");
                Iterator<StaEventWithTime> it = this.mStaEventList.iterator();
                while (it.hasNext()) {
                    printWriter.println(it.next());
                }
                printWriter.println("mWifiLogProto.numPasspointProviders=" + this.mWifiLogProto.numPasspointProviders);
                printWriter.println("mWifiLogProto.numPasspointProviderInstallation=" + this.mWifiLogProto.numPasspointProviderInstallation);
                printWriter.println("mWifiLogProto.numPasspointProviderInstallSuccess=" + this.mWifiLogProto.numPasspointProviderInstallSuccess);
                printWriter.println("mWifiLogProto.numPasspointProviderUninstallation=" + this.mWifiLogProto.numPasspointProviderUninstallation);
                printWriter.println("mWifiLogProto.numPasspointProviderUninstallSuccess=" + this.mWifiLogProto.numPasspointProviderUninstallSuccess);
                printWriter.println("mWifiLogProto.numPasspointProvidersSuccessfullyConnected=" + this.mWifiLogProto.numPasspointProvidersSuccessfullyConnected);
                printWriter.println("mWifiLogProto.numRadioModeChangeToMcc=" + this.mWifiLogProto.numRadioModeChangeToMcc);
                printWriter.println("mWifiLogProto.numRadioModeChangeToScc=" + this.mWifiLogProto.numRadioModeChangeToScc);
                printWriter.println("mWifiLogProto.numRadioModeChangeToSbs=" + this.mWifiLogProto.numRadioModeChangeToSbs);
                printWriter.println("mWifiLogProto.numRadioModeChangeToDbs=" + this.mWifiLogProto.numRadioModeChangeToDbs);
                printWriter.println("mWifiLogProto.numSoftApUserBandPreferenceUnsatisfied=" + this.mWifiLogProto.numSoftApUserBandPreferenceUnsatisfied);
                printWriter.println("mTotalSsidsInScanHistogram:" + this.mTotalSsidsInScanHistogram.toString());
                printWriter.println("mTotalBssidsInScanHistogram:" + this.mTotalBssidsInScanHistogram.toString());
                printWriter.println("mAvailableOpenSsidsInScanHistogram:" + this.mAvailableOpenSsidsInScanHistogram.toString());
                printWriter.println("mAvailableOpenBssidsInScanHistogram:" + this.mAvailableOpenBssidsInScanHistogram.toString());
                printWriter.println("mAvailableSavedSsidsInScanHistogram:" + this.mAvailableSavedSsidsInScanHistogram.toString());
                printWriter.println("mAvailableSavedBssidsInScanHistogram:" + this.mAvailableSavedBssidsInScanHistogram.toString());
                printWriter.println("mAvailableOpenOrSavedSsidsInScanHistogram:" + this.mAvailableOpenOrSavedSsidsInScanHistogram.toString());
                printWriter.println("mAvailableOpenOrSavedBssidsInScanHistogram:" + this.mAvailableOpenOrSavedBssidsInScanHistogram.toString());
                printWriter.println("mAvailableSavedPasspointProviderProfilesInScanHistogram:" + this.mAvailableSavedPasspointProviderProfilesInScanHistogram.toString());
                printWriter.println("mAvailableSavedPasspointProviderBssidsInScanHistogram:" + this.mAvailableSavedPasspointProviderBssidsInScanHistogram.toString());
                printWriter.println("mWifiLogProto.partialAllSingleScanListenerResults=" + this.mWifiLogProto.partialAllSingleScanListenerResults);
                printWriter.println("mWifiLogProto.fullBandAllSingleScanListenerResults=" + this.mWifiLogProto.fullBandAllSingleScanListenerResults);
                printWriter.println("mWifiAwareMetrics:");
                this.mWifiAwareMetrics.dump(fileDescriptor, printWriter, strArr);
                printWriter.println("mRttMetrics:");
                this.mRttMetrics.dump(fileDescriptor, printWriter, strArr);
                printWriter.println("mPnoScanMetrics.numPnoScanAttempts=" + this.mPnoScanMetrics.numPnoScanAttempts);
                printWriter.println("mPnoScanMetrics.numPnoScanFailed=" + this.mPnoScanMetrics.numPnoScanFailed);
                printWriter.println("mPnoScanMetrics.numPnoScanStartedOverOffload=" + this.mPnoScanMetrics.numPnoScanStartedOverOffload);
                printWriter.println("mPnoScanMetrics.numPnoScanFailedOverOffload=" + this.mPnoScanMetrics.numPnoScanFailedOverOffload);
                printWriter.println("mPnoScanMetrics.numPnoFoundNetworkEvents=" + this.mPnoScanMetrics.numPnoFoundNetworkEvents);
                printWriter.println("mWifiLogProto.connectToNetworkNotificationCount=" + this.mConnectToNetworkNotificationCount.toString());
                printWriter.println("mWifiLogProto.connectToNetworkNotificationActionCount=" + this.mConnectToNetworkNotificationActionCount.toString());
                printWriter.println("mWifiLogProto.openNetworkRecommenderBlacklistSize=" + this.mOpenNetworkRecommenderBlacklistSize);
                printWriter.println("mWifiLogProto.isWifiNetworksAvailableNotificationOn=" + this.mIsWifiNetworksAvailableNotificationOn);
                printWriter.println("mWifiLogProto.numOpenNetworkRecommendationUpdates=" + this.mNumOpenNetworkRecommendationUpdates);
                printWriter.println("mWifiLogProto.numOpenNetworkConnectMessageFailedToSend=" + this.mNumOpenNetworkConnectMessageFailedToSend);
                printWriter.println("mWifiLogProto.observedHotspotR1ApInScanHistogram=" + this.mObservedHotspotR1ApInScanHistogram);
                printWriter.println("mWifiLogProto.observedHotspotR2ApInScanHistogram=" + this.mObservedHotspotR2ApInScanHistogram);
                printWriter.println("mWifiLogProto.observedHotspotR1EssInScanHistogram=" + this.mObservedHotspotR1EssInScanHistogram);
                printWriter.println("mWifiLogProto.observedHotspotR2EssInScanHistogram=" + this.mObservedHotspotR2EssInScanHistogram);
                printWriter.println("mWifiLogProto.observedHotspotR1ApsPerEssInScanHistogram=" + this.mObservedHotspotR1ApsPerEssInScanHistogram);
                printWriter.println("mWifiLogProto.observedHotspotR2ApsPerEssInScanHistogram=" + this.mObservedHotspotR2ApsPerEssInScanHistogram);
                printWriter.println("mWifiLogProto.observed80211mcSupportingApsInScanHistogram" + this.mObserved80211mcApInScanHistogram);
                printWriter.println("mSoftApTetheredEvents:");
                for (WifiMetricsProto.SoftApConnectedClientsEvent softApConnectedClientsEvent : this.mSoftApEventListTethered) {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("event_type=" + softApConnectedClientsEvent.eventType);
                    sb2.append(",time_stamp_millis=" + softApConnectedClientsEvent.timeStampMillis);
                    sb2.append(",num_connected_clients=" + softApConnectedClientsEvent.numConnectedClients);
                    sb2.append(",channel_frequency=" + softApConnectedClientsEvent.channelFrequency);
                    sb2.append(",channel_bandwidth=" + softApConnectedClientsEvent.channelBandwidth);
                    printWriter.println(sb2.toString());
                }
                printWriter.println("mSoftApLocalOnlyEvents:");
                for (WifiMetricsProto.SoftApConnectedClientsEvent softApConnectedClientsEvent2 : this.mSoftApEventListLocalOnly) {
                    StringBuilder sb3 = new StringBuilder();
                    sb3.append("event_type=" + softApConnectedClientsEvent2.eventType);
                    sb3.append(",time_stamp_millis=" + softApConnectedClientsEvent2.timeStampMillis);
                    sb3.append(",num_connected_clients=" + softApConnectedClientsEvent2.numConnectedClients);
                    sb3.append(",channel_frequency=" + softApConnectedClientsEvent2.channelFrequency);
                    sb3.append(",channel_bandwidth=" + softApConnectedClientsEvent2.channelBandwidth);
                    printWriter.println(sb3.toString());
                }
                printWriter.println("mWpsMetrics.numWpsAttempts=" + this.mWpsMetrics.numWpsAttempts);
                printWriter.println("mWpsMetrics.numWpsSuccess=" + this.mWpsMetrics.numWpsSuccess);
                printWriter.println("mWpsMetrics.numWpsStartFailure=" + this.mWpsMetrics.numWpsStartFailure);
                printWriter.println("mWpsMetrics.numWpsOverlapFailure=" + this.mWpsMetrics.numWpsOverlapFailure);
                printWriter.println("mWpsMetrics.numWpsTimeoutFailure=" + this.mWpsMetrics.numWpsTimeoutFailure);
                printWriter.println("mWpsMetrics.numWpsOtherConnectionFailure=" + this.mWpsMetrics.numWpsOtherConnectionFailure);
                printWriter.println("mWpsMetrics.numWpsSupplicantFailure=" + this.mWpsMetrics.numWpsSupplicantFailure);
                printWriter.println("mWpsMetrics.numWpsCancellation=" + this.mWpsMetrics.numWpsCancellation);
                this.mWifiPowerMetrics.dump(printWriter);
                this.mWifiWakeMetrics.dump(printWriter);
                printWriter.println("mWifiLogProto.isMacRandomizationOn=" + this.mIsMacRandomizationOn);
                printWriter.println("mWifiLogProto.scoreExperimentId=" + this.mWifiLogProto.scoreExperimentId);
            }
        }
    }

    public void updateSavedNetworks(List<WifiConfiguration> list) {
        synchronized (this.mLock) {
            this.mWifiLogProto.numSavedNetworks = list.size();
            this.mWifiLogProto.numOpenNetworks = 0;
            this.mWifiLogProto.numPersonalNetworks = 0;
            this.mWifiLogProto.numEnterpriseNetworks = 0;
            this.mWifiLogProto.numNetworksAddedByUser = 0;
            this.mWifiLogProto.numNetworksAddedByApps = 0;
            this.mWifiLogProto.numHiddenNetworks = 0;
            this.mWifiLogProto.numPasspointNetworks = 0;
            for (WifiConfiguration wifiConfiguration : list) {
                if (wifiConfiguration.allowedKeyManagement.get(0)) {
                    this.mWifiLogProto.numOpenNetworks++;
                } else if (wifiConfiguration.isEnterprise()) {
                    this.mWifiLogProto.numEnterpriseNetworks++;
                } else {
                    this.mWifiLogProto.numPersonalNetworks++;
                }
                if (wifiConfiguration.selfAdded) {
                    this.mWifiLogProto.numNetworksAddedByUser++;
                } else {
                    this.mWifiLogProto.numNetworksAddedByApps++;
                }
                if (wifiConfiguration.hiddenSSID) {
                    this.mWifiLogProto.numHiddenNetworks++;
                }
                if (wifiConfiguration.isPasspoint()) {
                    this.mWifiLogProto.numPasspointNetworks++;
                }
            }
        }
    }

    public void updateSavedPasspointProfiles(int i, int i2) {
        synchronized (this.mLock) {
            this.mWifiLogProto.numPasspointProviders = i;
            this.mWifiLogProto.numPasspointProvidersSuccessfullyConnected = i2;
        }
    }

    private void consolidateProto(boolean z) {
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        ArrayList arrayList3 = new ArrayList();
        ArrayList arrayList4 = new ArrayList();
        ArrayList arrayList5 = new ArrayList();
        synchronized (this.mLock) {
            for (ConnectionEvent connectionEvent : this.mConnectionEventList) {
                if (!z || (this.mCurrentConnectionEvent != connectionEvent && !connectionEvent.mConnectionEvent.automaticBugReportTaken)) {
                    arrayList.add(connectionEvent.mConnectionEvent);
                    if (z) {
                        connectionEvent.mConnectionEvent.automaticBugReportTaken = true;
                    }
                }
            }
            if (arrayList.size() > 0) {
                this.mWifiLogProto.connectionEvent = (WifiMetricsProto.ConnectionEvent[]) arrayList.toArray(this.mWifiLogProto.connectionEvent);
            }
            this.mWifiLogProto.scanReturnEntries = new WifiMetricsProto.WifiLog.ScanReturnEntry[this.mScanReturnEntries.size()];
            for (int i = 0; i < this.mScanReturnEntries.size(); i++) {
                this.mWifiLogProto.scanReturnEntries[i] = new WifiMetricsProto.WifiLog.ScanReturnEntry();
                this.mWifiLogProto.scanReturnEntries[i].scanReturnCode = this.mScanReturnEntries.keyAt(i);
                this.mWifiLogProto.scanReturnEntries[i].scanResultsCount = this.mScanReturnEntries.valueAt(i);
            }
            this.mWifiLogProto.wifiSystemStateEntries = new WifiMetricsProto.WifiLog.WifiSystemStateEntry[this.mWifiSystemStateEntries.size()];
            for (int i2 = 0; i2 < this.mWifiSystemStateEntries.size(); i2++) {
                this.mWifiLogProto.wifiSystemStateEntries[i2] = new WifiMetricsProto.WifiLog.WifiSystemStateEntry();
                this.mWifiLogProto.wifiSystemStateEntries[i2].wifiState = this.mWifiSystemStateEntries.keyAt(i2) / 2;
                this.mWifiLogProto.wifiSystemStateEntries[i2].wifiStateCount = this.mWifiSystemStateEntries.valueAt(i2);
                this.mWifiLogProto.wifiSystemStateEntries[i2].isScreenOn = this.mWifiSystemStateEntries.keyAt(i2) % 2 > 0;
            }
            this.mWifiLogProto.recordDurationSec = (int) ((this.mClock.getElapsedSinceBootMillis() / 1000) - this.mRecordStartTimeSec);
            for (Map.Entry<Integer, SparseIntArray> entry : this.mRssiPollCountsMap.entrySet()) {
                int iIntValue = entry.getKey().intValue();
                SparseIntArray value = entry.getValue();
                for (int i3 = 0; i3 < value.size(); i3++) {
                    WifiMetricsProto.RssiPollCount rssiPollCount = new WifiMetricsProto.RssiPollCount();
                    rssiPollCount.rssi = value.keyAt(i3);
                    rssiPollCount.count = value.valueAt(i3);
                    rssiPollCount.frequency = iIntValue;
                    arrayList2.add(rssiPollCount);
                }
            }
            this.mWifiLogProto.rssiPollRssiCount = (WifiMetricsProto.RssiPollCount[]) arrayList2.toArray(this.mWifiLogProto.rssiPollRssiCount);
            for (int i4 = 0; i4 < this.mRssiDeltaCounts.size(); i4++) {
                WifiMetricsProto.RssiPollCount rssiPollCount2 = new WifiMetricsProto.RssiPollCount();
                rssiPollCount2.rssi = this.mRssiDeltaCounts.keyAt(i4);
                rssiPollCount2.count = this.mRssiDeltaCounts.valueAt(i4);
                arrayList3.add(rssiPollCount2);
            }
            this.mWifiLogProto.rssiPollDeltaCount = (WifiMetricsProto.RssiPollCount[]) arrayList3.toArray(this.mWifiLogProto.rssiPollDeltaCount);
            for (int i5 = 0; i5 < this.mWifiAlertReasonCounts.size(); i5++) {
                WifiMetricsProto.AlertReasonCount alertReasonCount = new WifiMetricsProto.AlertReasonCount();
                alertReasonCount.reason = this.mWifiAlertReasonCounts.keyAt(i5);
                alertReasonCount.count = this.mWifiAlertReasonCounts.valueAt(i5);
                arrayList4.add(alertReasonCount);
            }
            this.mWifiLogProto.alertReasonCount = (WifiMetricsProto.AlertReasonCount[]) arrayList4.toArray(this.mWifiLogProto.alertReasonCount);
            for (int i6 = 0; i6 < this.mWifiScoreCounts.size(); i6++) {
                WifiMetricsProto.WifiScoreCount wifiScoreCount = new WifiMetricsProto.WifiScoreCount();
                wifiScoreCount.score = this.mWifiScoreCounts.keyAt(i6);
                wifiScoreCount.count = this.mWifiScoreCounts.valueAt(i6);
                arrayList5.add(wifiScoreCount);
            }
            this.mWifiLogProto.wifiScoreCount = (WifiMetricsProto.WifiScoreCount[]) arrayList5.toArray(this.mWifiLogProto.wifiScoreCount);
            int size = this.mSoftApManagerReturnCodeCounts.size();
            this.mWifiLogProto.softApReturnCode = new WifiMetricsProto.SoftApReturnCodeCount[size];
            for (int i7 = 0; i7 < size; i7++) {
                this.mWifiLogProto.softApReturnCode[i7] = new WifiMetricsProto.SoftApReturnCodeCount();
                this.mWifiLogProto.softApReturnCode[i7].startResult = this.mSoftApManagerReturnCodeCounts.keyAt(i7);
                this.mWifiLogProto.softApReturnCode[i7].count = this.mSoftApManagerReturnCodeCounts.valueAt(i7);
            }
            this.mWifiLogProto.staEventList = new WifiMetricsProto.StaEvent[this.mStaEventList.size()];
            for (int i8 = 0; i8 < this.mStaEventList.size(); i8++) {
                this.mWifiLogProto.staEventList[i8] = this.mStaEventList.get(i8).staEvent;
            }
            this.mWifiLogProto.totalSsidsInScanHistogram = makeNumConnectableNetworksBucketArray(this.mTotalSsidsInScanHistogram);
            this.mWifiLogProto.totalBssidsInScanHistogram = makeNumConnectableNetworksBucketArray(this.mTotalBssidsInScanHistogram);
            this.mWifiLogProto.availableOpenSsidsInScanHistogram = makeNumConnectableNetworksBucketArray(this.mAvailableOpenSsidsInScanHistogram);
            this.mWifiLogProto.availableOpenBssidsInScanHistogram = makeNumConnectableNetworksBucketArray(this.mAvailableOpenBssidsInScanHistogram);
            this.mWifiLogProto.availableSavedSsidsInScanHistogram = makeNumConnectableNetworksBucketArray(this.mAvailableSavedSsidsInScanHistogram);
            this.mWifiLogProto.availableSavedBssidsInScanHistogram = makeNumConnectableNetworksBucketArray(this.mAvailableSavedBssidsInScanHistogram);
            this.mWifiLogProto.availableOpenOrSavedSsidsInScanHistogram = makeNumConnectableNetworksBucketArray(this.mAvailableOpenOrSavedSsidsInScanHistogram);
            this.mWifiLogProto.availableOpenOrSavedBssidsInScanHistogram = makeNumConnectableNetworksBucketArray(this.mAvailableOpenOrSavedBssidsInScanHistogram);
            this.mWifiLogProto.availableSavedPasspointProviderProfilesInScanHistogram = makeNumConnectableNetworksBucketArray(this.mAvailableSavedPasspointProviderProfilesInScanHistogram);
            this.mWifiLogProto.availableSavedPasspointProviderBssidsInScanHistogram = makeNumConnectableNetworksBucketArray(this.mAvailableSavedPasspointProviderBssidsInScanHistogram);
            this.mWifiLogProto.wifiAwareLog = this.mWifiAwareMetrics.consolidateProto();
            this.mWifiLogProto.wifiRttLog = this.mRttMetrics.consolidateProto();
            this.mWifiLogProto.pnoScanMetrics = this.mPnoScanMetrics;
            WifiMetricsProto.ConnectToNetworkNotificationAndActionCount[] connectToNetworkNotificationAndActionCountArr = new WifiMetricsProto.ConnectToNetworkNotificationAndActionCount[this.mConnectToNetworkNotificationCount.size()];
            for (int i9 = 0; i9 < this.mConnectToNetworkNotificationCount.size(); i9++) {
                WifiMetricsProto.ConnectToNetworkNotificationAndActionCount connectToNetworkNotificationAndActionCount = new WifiMetricsProto.ConnectToNetworkNotificationAndActionCount();
                connectToNetworkNotificationAndActionCount.notification = this.mConnectToNetworkNotificationCount.keyAt(i9);
                connectToNetworkNotificationAndActionCount.recommender = 1;
                connectToNetworkNotificationAndActionCount.count = this.mConnectToNetworkNotificationCount.valueAt(i9);
                connectToNetworkNotificationAndActionCountArr[i9] = connectToNetworkNotificationAndActionCount;
            }
            this.mWifiLogProto.connectToNetworkNotificationCount = connectToNetworkNotificationAndActionCountArr;
            WifiMetricsProto.ConnectToNetworkNotificationAndActionCount[] connectToNetworkNotificationAndActionCountArr2 = new WifiMetricsProto.ConnectToNetworkNotificationAndActionCount[this.mConnectToNetworkNotificationActionCount.size()];
            for (int i10 = 0; i10 < this.mConnectToNetworkNotificationActionCount.size(); i10++) {
                WifiMetricsProto.ConnectToNetworkNotificationAndActionCount connectToNetworkNotificationAndActionCount2 = new WifiMetricsProto.ConnectToNetworkNotificationAndActionCount();
                int iKeyAt = this.mConnectToNetworkNotificationActionCount.keyAt(i10);
                connectToNetworkNotificationAndActionCount2.notification = iKeyAt / CONNECT_TO_NETWORK_NOTIFICATION_ACTION_KEY_MULTIPLIER;
                connectToNetworkNotificationAndActionCount2.action = iKeyAt % CONNECT_TO_NETWORK_NOTIFICATION_ACTION_KEY_MULTIPLIER;
                connectToNetworkNotificationAndActionCount2.recommender = 1;
                connectToNetworkNotificationAndActionCount2.count = this.mConnectToNetworkNotificationActionCount.valueAt(i10);
                connectToNetworkNotificationAndActionCountArr2[i10] = connectToNetworkNotificationAndActionCount2;
            }
            this.mWifiLogProto.connectToNetworkNotificationActionCount = connectToNetworkNotificationAndActionCountArr2;
            this.mWifiLogProto.openNetworkRecommenderBlacklistSize = this.mOpenNetworkRecommenderBlacklistSize;
            this.mWifiLogProto.isWifiNetworksAvailableNotificationOn = this.mIsWifiNetworksAvailableNotificationOn;
            this.mWifiLogProto.numOpenNetworkRecommendationUpdates = this.mNumOpenNetworkRecommendationUpdates;
            this.mWifiLogProto.numOpenNetworkConnectMessageFailedToSend = this.mNumOpenNetworkConnectMessageFailedToSend;
            this.mWifiLogProto.observedHotspotR1ApsInScanHistogram = makeNumConnectableNetworksBucketArray(this.mObservedHotspotR1ApInScanHistogram);
            this.mWifiLogProto.observedHotspotR2ApsInScanHistogram = makeNumConnectableNetworksBucketArray(this.mObservedHotspotR2ApInScanHistogram);
            this.mWifiLogProto.observedHotspotR1EssInScanHistogram = makeNumConnectableNetworksBucketArray(this.mObservedHotspotR1EssInScanHistogram);
            this.mWifiLogProto.observedHotspotR2EssInScanHistogram = makeNumConnectableNetworksBucketArray(this.mObservedHotspotR2EssInScanHistogram);
            this.mWifiLogProto.observedHotspotR1ApsPerEssInScanHistogram = makeNumConnectableNetworksBucketArray(this.mObservedHotspotR1ApsPerEssInScanHistogram);
            this.mWifiLogProto.observedHotspotR2ApsPerEssInScanHistogram = makeNumConnectableNetworksBucketArray(this.mObservedHotspotR2ApsPerEssInScanHistogram);
            this.mWifiLogProto.observed80211McSupportingApsInScanHistogram = makeNumConnectableNetworksBucketArray(this.mObserved80211mcApInScanHistogram);
            if (this.mSoftApEventListTethered.size() > 0) {
                this.mWifiLogProto.softApConnectedClientsEventsTethered = (WifiMetricsProto.SoftApConnectedClientsEvent[]) this.mSoftApEventListTethered.toArray(this.mWifiLogProto.softApConnectedClientsEventsTethered);
            }
            if (this.mSoftApEventListLocalOnly.size() > 0) {
                this.mWifiLogProto.softApConnectedClientsEventsLocalOnly = (WifiMetricsProto.SoftApConnectedClientsEvent[]) this.mSoftApEventListLocalOnly.toArray(this.mWifiLogProto.softApConnectedClientsEventsLocalOnly);
            }
            this.mWifiLogProto.wpsMetrics = this.mWpsMetrics;
            this.mWifiLogProto.wifiPowerStats = this.mWifiPowerMetrics.buildProto();
            this.mWifiLogProto.wifiWakeStats = this.mWifiWakeMetrics.buildProto();
            this.mWifiLogProto.isMacRandomizationOn = this.mIsMacRandomizationOn;
        }
    }

    private void consolidateScoringParams() {
        synchronized (this.mLock) {
            if (this.mScoringParams != null) {
                int experimentIdentifier = this.mScoringParams.getExperimentIdentifier();
                if (experimentIdentifier == 0) {
                    this.mWifiLogProto.scoreExperimentId = "";
                } else {
                    this.mWifiLogProto.scoreExperimentId = "x" + experimentIdentifier;
                }
            }
        }
    }

    private WifiMetricsProto.NumConnectableNetworksBucket[] makeNumConnectableNetworksBucketArray(SparseIntArray sparseIntArray) {
        WifiMetricsProto.NumConnectableNetworksBucket[] numConnectableNetworksBucketArr = new WifiMetricsProto.NumConnectableNetworksBucket[sparseIntArray.size()];
        for (int i = 0; i < sparseIntArray.size(); i++) {
            WifiMetricsProto.NumConnectableNetworksBucket numConnectableNetworksBucket = new WifiMetricsProto.NumConnectableNetworksBucket();
            numConnectableNetworksBucket.numConnectableNetworks = sparseIntArray.keyAt(i);
            numConnectableNetworksBucket.count = sparseIntArray.valueAt(i);
            numConnectableNetworksBucketArr[i] = numConnectableNetworksBucket;
        }
        return numConnectableNetworksBucketArr;
    }

    private void clear() {
        synchronized (this.mLock) {
            this.mConnectionEventList.clear();
            if (this.mCurrentConnectionEvent != null) {
                this.mConnectionEventList.add(this.mCurrentConnectionEvent);
            }
            this.mScanReturnEntries.clear();
            this.mWifiSystemStateEntries.clear();
            this.mRecordStartTimeSec = this.mClock.getElapsedSinceBootMillis() / 1000;
            this.mRssiPollCountsMap.clear();
            this.mRssiDeltaCounts.clear();
            this.mWifiAlertReasonCounts.clear();
            this.mWifiScoreCounts.clear();
            this.mWifiLogProto.clear();
            this.mScanResultRssiTimestampMillis = -1L;
            this.mSoftApManagerReturnCodeCounts.clear();
            this.mStaEventList.clear();
            this.mWifiAwareMetrics.clear();
            this.mRttMetrics.clear();
            this.mTotalSsidsInScanHistogram.clear();
            this.mTotalBssidsInScanHistogram.clear();
            this.mAvailableOpenSsidsInScanHistogram.clear();
            this.mAvailableOpenBssidsInScanHistogram.clear();
            this.mAvailableSavedSsidsInScanHistogram.clear();
            this.mAvailableSavedBssidsInScanHistogram.clear();
            this.mAvailableOpenOrSavedSsidsInScanHistogram.clear();
            this.mAvailableOpenOrSavedBssidsInScanHistogram.clear();
            this.mAvailableSavedPasspointProviderProfilesInScanHistogram.clear();
            this.mAvailableSavedPasspointProviderBssidsInScanHistogram.clear();
            this.mPnoScanMetrics.clear();
            this.mConnectToNetworkNotificationCount.clear();
            this.mConnectToNetworkNotificationActionCount.clear();
            this.mNumOpenNetworkRecommendationUpdates = 0;
            this.mNumOpenNetworkConnectMessageFailedToSend = 0;
            this.mObservedHotspotR1ApInScanHistogram.clear();
            this.mObservedHotspotR2ApInScanHistogram.clear();
            this.mObservedHotspotR1EssInScanHistogram.clear();
            this.mObservedHotspotR2EssInScanHistogram.clear();
            this.mObservedHotspotR1ApsPerEssInScanHistogram.clear();
            this.mObservedHotspotR2ApsPerEssInScanHistogram.clear();
            this.mSoftApEventListTethered.clear();
            this.mSoftApEventListLocalOnly.clear();
            this.mWpsMetrics.clear();
            this.mWifiWakeMetrics.clear();
            this.mObserved80211mcApInScanHistogram.clear();
        }
    }

    public void setScreenState(boolean z) {
        synchronized (this.mLock) {
            this.mScreenOn = z;
        }
    }

    public void setWifiState(int i) {
        synchronized (this.mLock) {
            this.mWifiState = i;
            this.mWifiWins = i == 3 ? true : DBG;
        }
    }

    private void processMessage(Message message) {
        WifiMetricsProto.StaEvent staEvent = new WifiMetricsProto.StaEvent();
        int i = message.what;
        boolean z = DBG;
        switch (i) {
            case 131213:
                staEvent.type = 10;
                z = true;
                if (z) {
                    addStaEvent(staEvent);
                }
                break;
            case 131219:
                staEvent.type = 6;
                z = true;
                if (z) {
                }
                break;
            case WifiMonitor.NETWORK_CONNECTION_EVENT:
                staEvent.type = 3;
                z = true;
                if (z) {
                }
                break;
            case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                staEvent.type = 4;
                staEvent.reason = message.arg2;
                if (message.arg1 != 0) {
                    z = true;
                }
                staEvent.localGen = z;
                z = true;
                if (z) {
                }
                break;
            case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                this.mSupplicantStateChangeBitmask = supplicantStateToBit(((StateChangeResult) message.obj).state) | this.mSupplicantStateChangeBitmask;
                if (z) {
                }
                break;
            case WifiMonitor.AUTHENTICATION_FAILURE_EVENT:
                staEvent.type = 2;
                switch (message.arg1) {
                    case 0:
                        staEvent.authFailureReason = 1;
                        break;
                    case 1:
                        staEvent.authFailureReason = 2;
                        break;
                    case 2:
                        staEvent.authFailureReason = 3;
                        break;
                    case 3:
                        staEvent.authFailureReason = 4;
                        break;
                }
                z = true;
                if (z) {
                }
                break;
            case WifiMonitor.ASSOCIATION_REJECTION_EVENT:
                staEvent.type = 1;
                if (message.arg1 > 0) {
                    z = true;
                }
                staEvent.associationTimedOut = z;
                staEvent.status = message.arg2;
                z = true;
                if (z) {
                }
                break;
        }
    }

    public void logStaEvent(int i) {
        logStaEvent(i, 0, null);
    }

    public void logStaEvent(int i, WifiConfiguration wifiConfiguration) {
        logStaEvent(i, 0, wifiConfiguration);
    }

    public void logStaEvent(int i, int i2) {
        logStaEvent(i, i2, null);
    }

    public void logStaEvent(int i, int i2, WifiConfiguration wifiConfiguration) {
        switch (i) {
            case 7:
            case 8:
            case 9:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
                WifiMetricsProto.StaEvent staEvent = new WifiMetricsProto.StaEvent();
                staEvent.type = i;
                if (i2 != 0) {
                    staEvent.frameworkDisconnectReason = i2;
                }
                staEvent.configInfo = createConfigInfo(wifiConfiguration);
                addStaEvent(staEvent);
                break;
            case 10:
            default:
                Log.e(TAG, "Unknown StaEvent:" + i);
                break;
        }
    }

    private void addStaEvent(WifiMetricsProto.StaEvent staEvent) {
        staEvent.startTimeMillis = this.mClock.getElapsedSinceBootMillis();
        staEvent.lastRssi = this.mLastPollRssi;
        staEvent.lastFreq = this.mLastPollFreq;
        staEvent.lastLinkSpeed = this.mLastPollLinkSpeed;
        staEvent.supplicantStateChangesBitmask = this.mSupplicantStateChangeBitmask;
        staEvent.lastScore = this.mLastScore;
        this.mSupplicantStateChangeBitmask = 0;
        this.mLastPollRssi = -127;
        this.mLastPollFreq = -1;
        this.mLastPollLinkSpeed = -1;
        this.mLastScore = -1;
        this.mStaEventList.add(new StaEventWithTime(staEvent, this.mClock.getWallClockMillis()));
        if (this.mStaEventList.size() > 768) {
            this.mStaEventList.remove();
        }
    }

    private WifiMetricsProto.StaEvent.ConfigInfo createConfigInfo(WifiConfiguration wifiConfiguration) {
        if (wifiConfiguration == null) {
            return null;
        }
        WifiMetricsProto.StaEvent.ConfigInfo configInfo = new WifiMetricsProto.StaEvent.ConfigInfo();
        configInfo.allowedKeyManagement = bitSetToInt(wifiConfiguration.allowedKeyManagement);
        configInfo.allowedProtocols = bitSetToInt(wifiConfiguration.allowedProtocols);
        configInfo.allowedAuthAlgorithms = bitSetToInt(wifiConfiguration.allowedAuthAlgorithms);
        configInfo.allowedPairwiseCiphers = bitSetToInt(wifiConfiguration.allowedPairwiseCiphers);
        configInfo.allowedGroupCiphers = bitSetToInt(wifiConfiguration.allowedGroupCiphers);
        configInfo.hiddenSsid = wifiConfiguration.hiddenSSID;
        configInfo.isPasspoint = wifiConfiguration.isPasspoint();
        configInfo.isEphemeral = wifiConfiguration.isEphemeral();
        configInfo.hasEverConnected = wifiConfiguration.getNetworkSelectionStatus().getHasEverConnected();
        ScanResult candidate = wifiConfiguration.getNetworkSelectionStatus().getCandidate();
        if (candidate != null) {
            configInfo.scanRssi = candidate.level;
            configInfo.scanFreq = candidate.frequency;
        }
        return configInfo;
    }

    public Handler getHandler() {
        return this.mHandler;
    }

    public WifiAwareMetrics getWifiAwareMetrics() {
        return this.mWifiAwareMetrics;
    }

    public WifiWakeMetrics getWakeupMetrics() {
        return this.mWifiWakeMetrics;
    }

    public RttMetrics getRttMetrics() {
        return this.mRttMetrics;
    }

    static class AnonymousClass2 {
        static final int[] $SwitchMap$android$net$wifi$SupplicantState = new int[SupplicantState.values().length];

        static {
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.DISCONNECTED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.INTERFACE_DISABLED.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.INACTIVE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.SCANNING.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.AUTHENTICATING.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.ASSOCIATING.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.ASSOCIATED.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.FOUR_WAY_HANDSHAKE.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.GROUP_HANDSHAKE.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.COMPLETED.ordinal()] = 10;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.DORMANT.ordinal()] = 11;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.UNINITIALIZED.ordinal()] = 12;
            } catch (NoSuchFieldError e12) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.INVALID.ordinal()] = 13;
            } catch (NoSuchFieldError e13) {
            }
        }
    }

    public static int supplicantStateToBit(SupplicantState supplicantState) {
        switch (AnonymousClass2.$SwitchMap$android$net$wifi$SupplicantState[supplicantState.ordinal()]) {
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 4;
            case 4:
                return 8;
            case 5:
                return 16;
            case 6:
                return 32;
            case 7:
                return 64;
            case 8:
                return 128;
            case 9:
                return 256;
            case 10:
                return 512;
            case 11:
                return 1024;
            case 12:
                return 2048;
            case 13:
                return 4096;
            default:
                Log.wtf(TAG, "Got unknown supplicant state: " + supplicantState.ordinal());
                return 0;
        }
    }

    private static String supplicantStateChangesBitmaskToString(int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("supplicantStateChangeEvents: {");
        if ((i & 1) > 0) {
            sb.append(" DISCONNECTED");
        }
        if ((i & 2) > 0) {
            sb.append(" INTERFACE_DISABLED");
        }
        if ((i & 4) > 0) {
            sb.append(" INACTIVE");
        }
        if ((i & 8) > 0) {
            sb.append(" SCANNING");
        }
        if ((i & 16) > 0) {
            sb.append(" AUTHENTICATING");
        }
        if ((i & 32) > 0) {
            sb.append(" ASSOCIATING");
        }
        if ((i & 64) > 0) {
            sb.append(" ASSOCIATED");
        }
        if ((i & 128) > 0) {
            sb.append(" FOUR_WAY_HANDSHAKE");
        }
        if ((i & 256) > 0) {
            sb.append(" GROUP_HANDSHAKE");
        }
        if ((i & 512) > 0) {
            sb.append(" COMPLETED");
        }
        if ((i & 1024) > 0) {
            sb.append(" DORMANT");
        }
        if ((i & 2048) > 0) {
            sb.append(" UNINITIALIZED");
        }
        if ((i & 4096) > 0) {
            sb.append(" INVALID");
        }
        sb.append("}");
        return sb.toString();
    }

    public static String staEventToString(WifiMetricsProto.StaEvent staEvent) {
        if (staEvent == null) {
            return "<NULL>";
        }
        StringBuilder sb = new StringBuilder();
        switch (staEvent.type) {
            case 1:
                sb.append("ASSOCIATION_REJECTION_EVENT");
                sb.append(" timedOut=");
                sb.append(staEvent.associationTimedOut);
                sb.append(" status=");
                sb.append(staEvent.status);
                sb.append(":");
                sb.append(ISupplicantStaIfaceCallback.StatusCode.toString(staEvent.status));
                break;
            case 2:
                sb.append("AUTHENTICATION_FAILURE_EVENT reason=");
                sb.append(staEvent.authFailureReason);
                sb.append(":");
                sb.append(authFailureReasonToString(staEvent.authFailureReason));
                break;
            case 3:
                sb.append("NETWORK_CONNECTION_EVENT");
                break;
            case 4:
                sb.append("NETWORK_DISCONNECTION_EVENT");
                sb.append(" local_gen=");
                sb.append(staEvent.localGen);
                sb.append(" reason=");
                sb.append(staEvent.reason);
                sb.append(":");
                sb.append(ISupplicantStaIfaceCallback.ReasonCode.toString(staEvent.reason >= 0 ? staEvent.reason : staEvent.reason * (-1)));
                break;
            case 5:
            default:
                sb.append("UNKNOWN " + staEvent.type + ":");
                break;
            case 6:
                sb.append("CMD_ASSOCIATED_BSSID");
                break;
            case 7:
                sb.append("CMD_IP_CONFIGURATION_SUCCESSFUL");
                break;
            case 8:
                sb.append("CMD_IP_CONFIGURATION_LOST");
                break;
            case 9:
                sb.append("CMD_IP_REACHABILITY_LOST");
                break;
            case 10:
                sb.append("CMD_TARGET_BSSID");
                break;
            case 11:
                sb.append("CMD_START_CONNECT");
                break;
            case 12:
                sb.append("CMD_START_ROAM");
                break;
            case 13:
                sb.append("CONNECT_NETWORK");
                break;
            case 14:
                sb.append("NETWORK_AGENT_VALID_NETWORK");
                break;
            case 15:
                sb.append("FRAMEWORK_DISCONNECT");
                sb.append(" reason=");
                sb.append(frameworkDisconnectReasonToString(staEvent.frameworkDisconnectReason));
                break;
            case 16:
                sb.append("SCORE_BREACH");
                break;
            case 17:
                sb.append("MAC_CHANGE");
                break;
        }
        if (staEvent.lastRssi != -127) {
            sb.append(" lastRssi=");
            sb.append(staEvent.lastRssi);
        }
        if (staEvent.lastFreq != -1) {
            sb.append(" lastFreq=");
            sb.append(staEvent.lastFreq);
        }
        if (staEvent.lastLinkSpeed != -1) {
            sb.append(" lastLinkSpeed=");
            sb.append(staEvent.lastLinkSpeed);
        }
        if (staEvent.lastScore != -1) {
            sb.append(" lastScore=");
            sb.append(staEvent.lastScore);
        }
        if (staEvent.supplicantStateChangesBitmask != 0) {
            sb.append(", ");
            sb.append(supplicantStateChangesBitmaskToString(staEvent.supplicantStateChangesBitmask));
        }
        if (staEvent.configInfo != null) {
            sb.append(", ");
            sb.append(configInfoToString(staEvent.configInfo));
        }
        return sb.toString();
    }

    private static String authFailureReasonToString(int i) {
        switch (i) {
            case 1:
                return "ERROR_AUTH_FAILURE_NONE";
            case 2:
                return "ERROR_AUTH_FAILURE_TIMEOUT";
            case 3:
                return "ERROR_AUTH_FAILURE_WRONG_PSWD";
            case 4:
                return "ERROR_AUTH_FAILURE_EAP_FAILURE";
            default:
                return "";
        }
    }

    private static String frameworkDisconnectReasonToString(int i) {
        switch (i) {
            case 1:
                return "DISCONNECT_API";
            case 2:
                return "DISCONNECT_GENERIC";
            case 3:
                return "DISCONNECT_UNWANTED";
            case 4:
                return "DISCONNECT_ROAM_WATCHDOG_TIMER";
            case 5:
                return "DISCONNECT_P2P_DISCONNECT_WIFI_REQUEST";
            case 6:
                return "DISCONNECT_RESET_SIM_NETWORKS";
            default:
                return "DISCONNECT_UNKNOWN=" + i;
        }
    }

    private static String configInfoToString(WifiMetricsProto.StaEvent.ConfigInfo configInfo) {
        return "ConfigInfo: allowed_key_management=" + configInfo.allowedKeyManagement + " allowed_protocols=" + configInfo.allowedProtocols + " allowed_auth_algorithms=" + configInfo.allowedAuthAlgorithms + " allowed_pairwise_ciphers=" + configInfo.allowedPairwiseCiphers + " allowed_group_ciphers=" + configInfo.allowedGroupCiphers + " hidden_ssid=" + configInfo.hiddenSsid + " is_passpoint=" + configInfo.isPasspoint + " is_ephemeral=" + configInfo.isEphemeral + " has_ever_connected=" + configInfo.hasEverConnected + " scan_rssi=" + configInfo.scanRssi + " scan_freq=" + configInfo.scanFreq;
    }

    private static int bitSetToInt(BitSet bitSet) {
        int length = bitSet.length() < 31 ? bitSet.length() : 31;
        int i = 0;
        for (int i2 = 0; i2 < length; i2++) {
            i += bitSet.get(i2) ? 1 << i2 : 0;
        }
        return i;
    }

    private void incrementSsid(SparseIntArray sparseIntArray, int i) {
        increment(sparseIntArray, Math.min(i, 20));
    }

    private void incrementBssid(SparseIntArray sparseIntArray, int i) {
        increment(sparseIntArray, Math.min(i, 50));
    }

    private void incrementTotalScanResults(SparseIntArray sparseIntArray, int i) {
        increment(sparseIntArray, Math.min(i, MAX_TOTAL_SCAN_RESULTS_BUCKET));
    }

    private void incrementTotalScanSsids(SparseIntArray sparseIntArray, int i) {
        increment(sparseIntArray, Math.min(i, 100));
    }

    private void incrementTotalPasspointAps(SparseIntArray sparseIntArray, int i) {
        increment(sparseIntArray, Math.min(i, 50));
    }

    private void incrementTotalUniquePasspointEss(SparseIntArray sparseIntArray, int i) {
        increment(sparseIntArray, Math.min(i, 20));
    }

    private void incrementPasspointPerUniqueEss(SparseIntArray sparseIntArray, int i) {
        increment(sparseIntArray, Math.min(i, 50));
    }

    private void increment80211mcAps(SparseIntArray sparseIntArray, int i) {
        increment(sparseIntArray, Math.min(i, 20));
    }

    private void increment(SparseIntArray sparseIntArray, int i) {
        sparseIntArray.put(i, sparseIntArray.get(i) + 1);
    }

    private static class StaEventWithTime {
        public WifiMetricsProto.StaEvent staEvent;
        public long wallClockMillis;

        StaEventWithTime(WifiMetricsProto.StaEvent staEvent, long j) {
            this.staEvent = staEvent;
            this.wallClockMillis = j;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(this.wallClockMillis);
            if (this.wallClockMillis != 0) {
                sb.append(String.format("%tm-%td %tH:%tM:%tS.%tL", calendar, calendar, calendar, calendar, calendar, calendar));
            } else {
                sb.append("                  ");
            }
            sb.append(" ");
            sb.append(WifiMetrics.staEventToString(this.staEvent));
            return sb.toString();
        }
    }
}
