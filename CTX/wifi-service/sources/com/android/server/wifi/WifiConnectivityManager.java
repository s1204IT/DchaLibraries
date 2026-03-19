package com.android.server.wifi;

import android.R;
import android.app.AlarmManager;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiScanner;
import android.os.Handler;
import android.os.Looper;
import android.os.WorkSource;
import android.util.LocalLog;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.WifiConfigManager;
import com.android.server.wifi.hotspot2.PasspointNetworkEvaluator;
import com.android.server.wifi.util.ScanResultUtil;
import com.mediatek.server.wifi.MtkWfcUtility;
import com.mediatek.server.wifi.MtkWifiServiceAdapter;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WifiConnectivityManager {

    @VisibleForTesting
    public static final int BSSID_BLACKLIST_EXPIRE_TIME_MS = 300000;

    @VisibleForTesting
    public static final int BSSID_BLACKLIST_THRESHOLD = 3;
    private static final int CHANNEL_LIST_AGE_MS = 3600000;
    private static final int CONNECTED_PNO_SCAN_INTERVAL_MS = 160000;
    private static final int DISCONNECTED_PNO_SCAN_INTERVAL_MS = 20000;
    private static final int LOW_RSSI_NETWORK_RETRY_MAX_DELAY_MS = 80000;
    private static final int LOW_RSSI_NETWORK_RETRY_START_DELAY_MS = 20000;
    public static final int MAX_CONNECTION_ATTEMPTS_RATE = 6;
    public static final int MAX_CONNECTION_ATTEMPTS_TIME_INTERVAL_MS = 240000;

    @VisibleForTesting
    public static final int MAX_PERIODIC_SCAN_INTERVAL_MS = 160000;

    @VisibleForTesting
    public static final int MAX_SCAN_RESTART_ALLOWED = 5;
    private static final int PASSPOINT_NETWORK_EVALUATOR_PRIORITY = 2;

    @VisibleForTesting
    public static final int PERIODIC_SCAN_INTERVAL_MS = 20000;
    public static final String PERIODIC_SCAN_TIMER_TAG = "WifiConnectivityManager Schedule Periodic Scan Timer";

    @VisibleForTesting
    public static final int REASON_CODE_AP_UNABLE_TO_HANDLE_NEW_STA = 17;
    private static final long RESET_TIME_STAMP = Long.MIN_VALUE;
    public static final String RESTART_CONNECTIVITY_SCAN_TIMER_TAG = "WifiConnectivityManager Restart Scan";
    private static final int RESTART_SCAN_DELAY_MS = 2000;
    public static final String RESTART_SINGLE_SCAN_TIMER_TAG = "WifiConnectivityManager Restart Single Scan";
    private static final int SAVED_NETWORK_EVALUATOR_PRIORITY = 1;
    private static final boolean SCAN_IMMEDIATELY = true;
    private static final boolean SCAN_ON_SCHEDULE = false;
    private static final int SCORED_NETWORK_EVALUATOR_PRIORITY = 3;
    private static final String TAG = "WifiConnectivityManager";
    private static final int WATCHDOG_INTERVAL_MS = 1200000;
    public static final String WATCHDOG_TIMER_TAG = "WifiConnectivityManager Schedule Watchdog Timer";
    public static final int WIFI_STATE_CONNECTED = 1;
    public static final int WIFI_STATE_DISCONNECTED = 2;
    public static final int WIFI_STATE_TRANSITIONING = 3;
    public static final int WIFI_STATE_UNKNOWN = 0;
    private final AlarmManager mAlarmManager;
    private final AllSingleScanListener mAllSingleScanListener;
    private int mBand5GHzBonus;
    private final CarrierNetworkConfig mCarrierNetworkConfig;
    private final CarrierNetworkNotifier mCarrierNetworkNotifier;
    private final Clock mClock;
    private final WifiConfigManager mConfigManager;
    private final WifiConnectivityHelper mConnectivityHelper;
    private int mCurrentConnectionBonus;
    private boolean mEnableAutoJoinWhenAssociated;
    private final Handler mEventHandler;
    private int mFullScanMaxRxRate;
    private int mFullScanMaxTxRate;
    private int mInitialScoreMax;
    private final LocalLog mLocalLog;
    private int mMin24GHzRssi;
    private int mMin5GHzRssi;
    private final WifiNetworkSelector mNetworkSelector;
    private final OpenNetworkNotifier mOpenNetworkNotifier;
    private final PnoScanListener mPnoScanListener;
    private int mSameNetworkBonus;
    private final WifiScanner mScanner;
    private final ScoringParams mScoringParams;
    private int mSecureBonus;
    private final WifiStateMachine mStateMachine;
    private boolean mUseSingleRadioChainScanResults;
    private boolean mWifiConnectivityManagerEnabled;
    private final WifiInfo mWifiInfo;
    private final WifiLastResortWatchdog mWifiLastResortWatchdog;
    private final WifiMetrics mWifiMetrics;
    boolean mDbg = SCAN_ON_SCHEDULE;
    private boolean mWifiEnabled = SCAN_ON_SCHEDULE;
    private boolean mScreenOn = SCAN_ON_SCHEDULE;
    private int mWifiState = 0;
    private boolean mUntrustedConnectionAllowed = SCAN_ON_SCHEDULE;
    private int mScanRestartCount = 0;
    private int mSingleScanRestartCount = 0;
    private int mTotalConnectivityAttemptsRateLimited = 0;
    private String mLastConnectionAttemptBssid = null;
    private int mPeriodicSingleScanInterval = PERIODIC_SCAN_INTERVAL_MS;
    private long mLastPeriodicSingleScanTimeStamp = RESET_TIME_STAMP;
    private boolean mPnoScanStarted = SCAN_ON_SCHEDULE;
    private boolean mPeriodicScanTimerSet = SCAN_ON_SCHEDULE;
    private boolean mWaitForFullBandScanResults = SCAN_ON_SCHEDULE;
    private Map<String, BssidBlacklistStatus> mBssidBlacklist = new HashMap();
    private final AlarmManager.OnAlarmListener mRestartScanListener = new AlarmManager.OnAlarmListener() {
        @Override
        public void onAlarm() {
            WifiConnectivityManager.this.startConnectivityScan(true);
        }
    };
    private final AlarmManager.OnAlarmListener mWatchdogListener = new AlarmManager.OnAlarmListener() {
        @Override
        public void onAlarm() {
            WifiConnectivityManager.this.watchdogHandler();
        }
    };
    private final AlarmManager.OnAlarmListener mPeriodicScanTimerListener = new AlarmManager.OnAlarmListener() {
        @Override
        public void onAlarm() {
            WifiConnectivityManager.this.periodicScanTimerHandler();
        }
    };
    private final LinkedList<Long> mConnectionAttemptTimeStamps = new LinkedList<>();

    static int access$1308(WifiConnectivityManager wifiConnectivityManager) {
        int i = wifiConnectivityManager.mSingleScanRestartCount;
        wifiConnectivityManager.mSingleScanRestartCount = i + 1;
        return i;
    }

    static int access$1508(WifiConnectivityManager wifiConnectivityManager) {
        int i = wifiConnectivityManager.mScanRestartCount;
        wifiConnectivityManager.mScanRestartCount = i + 1;
        return i;
    }

    private static class BssidBlacklistStatus {
        public long blacklistedTimeStamp;
        public int counter;
        public boolean isBlacklisted;

        private BssidBlacklistStatus() {
            this.blacklistedTimeStamp = WifiConnectivityManager.RESET_TIME_STAMP;
        }
    }

    private void localLog(String str) {
        this.mLocalLog.log(str);
        Log.d(TAG, str);
    }

    private class RestartSingleScanListener implements AlarmManager.OnAlarmListener {
        private final boolean mIsFullBandScan;

        RestartSingleScanListener(boolean z) {
            this.mIsFullBandScan = z;
        }

        @Override
        public void onAlarm() {
            WifiConnectivityManager.this.startSingleScan(this.mIsFullBandScan, WifiStateMachine.WIFI_WORK_SOURCE);
        }
    }

    private boolean handleScanResults(List<ScanDetail> list, String str) {
        refreshBssidBlacklist();
        MtkWfcUtility.updateSavedNetworkChannel(list);
        if (this.mStateMachine.isSupplicantTransientState()) {
            localLog(str + " onResults: No network selection because supplicantTransientState is " + this.mStateMachine.isSupplicantTransientState());
            return SCAN_ON_SCHEDULE;
        }
        localLog(str + " onResults: start network selection");
        WifiConfiguration wifiConfigurationSelectNetwork = this.mNetworkSelector.selectNetwork(list, buildBssidBlacklist(), this.mWifiInfo, this.mStateMachine.isConnected(), this.mStateMachine.isDisconnected(), this.mUntrustedConnectionAllowed);
        this.mWifiLastResortWatchdog.updateAvailableNetworks(this.mNetworkSelector.getConnectableScanDetails());
        this.mWifiMetrics.countScanResults(list);
        if (wifiConfigurationSelectNetwork != null) {
            localLog(str + ":  WNS candidate-" + wifiConfigurationSelectNetwork.SSID);
            connectToNetwork(wifiConfigurationSelectNetwork);
            return true;
        }
        if (this.mWifiState == 2) {
            this.mOpenNetworkNotifier.handleScanResults(this.mNetworkSelector.getFilteredScanDetailsForOpenUnsavedNetworks());
            if (this.mCarrierNetworkConfig.isCarrierEncryptionInfoAvailable()) {
                this.mCarrierNetworkNotifier.handleScanResults(this.mNetworkSelector.getFilteredScanDetailsForCarrierUnsavedNetworks(this.mCarrierNetworkConfig));
            }
            MtkWifiServiceAdapter.handleScanResults(list, this.mNetworkSelector.getFilteredScanDetailsForOpenUnsavedNetworks());
        }
        return SCAN_ON_SCHEDULE;
    }

    private class AllSingleScanListener implements WifiScanner.ScanListener {
        private int mNumScanResultsIgnoredDueToSingleRadioChain;
        private List<ScanDetail> mScanDetails;

        private AllSingleScanListener() {
            this.mScanDetails = new ArrayList();
            this.mNumScanResultsIgnoredDueToSingleRadioChain = 0;
        }

        public void clearScanDetails() {
            this.mScanDetails.clear();
            this.mNumScanResultsIgnoredDueToSingleRadioChain = 0;
        }

        public void onSuccess() {
        }

        public void onFailure(int i, String str) {
            WifiConnectivityManager.this.localLog("registerScanListener onFailure: reason: " + i + " description: " + str);
        }

        public void onPeriodChanged(int i) {
        }

        public void onResults(WifiScanner.ScanData[] scanDataArr) {
            if (WifiConnectivityManager.this.mWifiEnabled && WifiConnectivityManager.this.mWifiConnectivityManagerEnabled) {
                if (WifiConnectivityManager.this.mWaitForFullBandScanResults) {
                    if (!scanDataArr[0].isAllChannelsScanned()) {
                        WifiConnectivityManager.this.localLog("AllSingleScanListener waiting for full band scan results.");
                        clearScanDetails();
                        return;
                    }
                    WifiConnectivityManager.this.mWaitForFullBandScanResults = WifiConnectivityManager.SCAN_ON_SCHEDULE;
                }
                if (scanDataArr.length > 0) {
                    WifiConnectivityManager.this.mWifiMetrics.incrementAvailableNetworksHistograms(this.mScanDetails, scanDataArr[0].isAllChannelsScanned());
                }
                if (this.mNumScanResultsIgnoredDueToSingleRadioChain > 0) {
                    Log.i(WifiConnectivityManager.TAG, "Number of scan results ignored due to single radio chain scan: " + this.mNumScanResultsIgnoredDueToSingleRadioChain);
                }
                boolean zHandleScanResults = WifiConnectivityManager.this.handleScanResults(this.mScanDetails, "AllSingleScanListener");
                clearScanDetails();
                if (WifiConnectivityManager.this.mPnoScanStarted) {
                    if (zHandleScanResults) {
                        WifiConnectivityManager.this.mWifiMetrics.incrementNumConnectivityWatchdogPnoBad();
                        return;
                    } else {
                        WifiConnectivityManager.this.mWifiMetrics.incrementNumConnectivityWatchdogPnoGood();
                        return;
                    }
                }
                return;
            }
            clearScanDetails();
            WifiConnectivityManager.this.mWaitForFullBandScanResults = WifiConnectivityManager.SCAN_ON_SCHEDULE;
        }

        public void onFullResult(ScanResult scanResult) {
            if (!WifiConnectivityManager.this.mWifiEnabled || !WifiConnectivityManager.this.mWifiConnectivityManagerEnabled) {
                return;
            }
            if (WifiConnectivityManager.this.mDbg) {
                WifiConnectivityManager.this.localLog("AllSingleScanListener onFullResult: " + scanResult.SSID + " BSSID " + scanResult.BSSID + " level " + scanResult.level + " frequency " + scanResult.frequency + " capabilities " + scanResult.capabilities);
            }
            if (!WifiConnectivityManager.this.mUseSingleRadioChainScanResults && scanResult.radioChainInfos != null && scanResult.radioChainInfos.length == 1) {
                this.mNumScanResultsIgnoredDueToSingleRadioChain++;
            } else {
                this.mScanDetails.add(ScanResultUtil.toScanDetail(scanResult));
            }
        }
    }

    private class SingleScanListener implements WifiScanner.ScanListener {
        private final boolean mIsFullBandScan;

        SingleScanListener(boolean z) {
            this.mIsFullBandScan = z;
        }

        public void onSuccess() {
        }

        public void onFailure(int i, String str) {
            WifiConnectivityManager.this.localLog("SingleScanListener onFailure: reason: " + i + " description: " + str);
            if (WifiConnectivityManager.access$1308(WifiConnectivityManager.this) < 5) {
                WifiConnectivityManager.this.scheduleDelayedSingleScan(this.mIsFullBandScan);
            } else {
                WifiConnectivityManager.this.mSingleScanRestartCount = 0;
                WifiConnectivityManager.this.localLog("Failed to successfully start single scan for 5 times");
            }
        }

        public void onPeriodChanged(int i) {
            WifiConnectivityManager.this.localLog("SingleScanListener onPeriodChanged: actual scan period " + i + "ms");
        }

        public void onResults(WifiScanner.ScanData[] scanDataArr) {
        }

        public void onFullResult(ScanResult scanResult) {
        }
    }

    private class PnoScanListener implements WifiScanner.PnoScanListener {
        private int mLowRssiNetworkRetryDelay;
        private List<ScanDetail> mScanDetails;

        private PnoScanListener() {
            this.mScanDetails = new ArrayList();
            this.mLowRssiNetworkRetryDelay = WifiConnectivityManager.PERIODIC_SCAN_INTERVAL_MS;
        }

        public void clearScanDetails() {
            this.mScanDetails.clear();
        }

        public void resetLowRssiNetworkRetryDelay() {
            this.mLowRssiNetworkRetryDelay = WifiConnectivityManager.PERIODIC_SCAN_INTERVAL_MS;
        }

        @VisibleForTesting
        public int getLowRssiNetworkRetryDelay() {
            return this.mLowRssiNetworkRetryDelay;
        }

        public void onSuccess() {
        }

        public void onFailure(int i, String str) {
            WifiConnectivityManager.this.localLog("PnoScanListener onFailure: reason: " + i + " description: " + str);
            if (WifiConnectivityManager.access$1508(WifiConnectivityManager.this) < 5) {
                WifiConnectivityManager.this.scheduleDelayedConnectivityScan(WifiConnectivityManager.RESTART_SCAN_DELAY_MS);
            } else {
                WifiConnectivityManager.this.mScanRestartCount = 0;
                WifiConnectivityManager.this.localLog("Failed to successfully start PNO scan for 5 times");
            }
        }

        public void onPeriodChanged(int i) {
            WifiConnectivityManager.this.localLog("PnoScanListener onPeriodChanged: actual scan period " + i + "ms");
        }

        public void onResults(WifiScanner.ScanData[] scanDataArr) {
        }

        public void onFullResult(ScanResult scanResult) {
        }

        public void onPnoNetworkFound(ScanResult[] scanResultArr) {
            for (ScanResult scanResult : scanResultArr) {
                if (scanResult.informationElements == null) {
                    WifiConnectivityManager.this.localLog("Skipping scan result with null information elements");
                } else {
                    this.mScanDetails.add(ScanResultUtil.toScanDetail(scanResult));
                }
            }
            boolean zHandleScanResults = WifiConnectivityManager.this.handleScanResults(this.mScanDetails, "PnoScanListener");
            clearScanDetails();
            WifiConnectivityManager.this.mScanRestartCount = 0;
            if (!zHandleScanResults) {
                if (this.mLowRssiNetworkRetryDelay > WifiConnectivityManager.LOW_RSSI_NETWORK_RETRY_MAX_DELAY_MS) {
                    this.mLowRssiNetworkRetryDelay = WifiConnectivityManager.LOW_RSSI_NETWORK_RETRY_MAX_DELAY_MS;
                }
                WifiConnectivityManager.this.scheduleDelayedConnectivityScan(this.mLowRssiNetworkRetryDelay);
                this.mLowRssiNetworkRetryDelay *= 2;
                return;
            }
            resetLowRssiNetworkRetryDelay();
        }
    }

    private class OnSavedNetworkUpdateListener implements WifiConfigManager.OnSavedNetworkUpdateListener {
        private OnSavedNetworkUpdateListener() {
        }

        @Override
        public void onSavedNetworkAdded(int i) {
            updatePnoScan();
        }

        @Override
        public void onSavedNetworkEnabled(int i) {
            updatePnoScan();
        }

        @Override
        public void onSavedNetworkRemoved(int i) {
            updatePnoScan();
        }

        @Override
        public void onSavedNetworkUpdated(int i) {
            WifiConnectivityManager.this.mStateMachine.updateCapabilities();
            updatePnoScan();
        }

        @Override
        public void onSavedNetworkTemporarilyDisabled(int i, int i2) {
            if (i2 == 6) {
                return;
            }
            WifiConnectivityManager.this.mConnectivityHelper.removeNetworkIfCurrent(i);
        }

        @Override
        public void onSavedNetworkPermanentlyDisabled(int i, int i2) {
            WifiConnectivityManager.this.mConnectivityHelper.removeNetworkIfCurrent(i);
            updatePnoScan();
        }

        private void updatePnoScan() {
            if (!WifiConnectivityManager.this.mScreenOn) {
                WifiConnectivityManager.this.localLog("Saved networks updated");
                WifiConnectivityManager.this.startConnectivityScan(WifiConnectivityManager.SCAN_ON_SCHEDULE);
            }
        }
    }

    WifiConnectivityManager(Context context, ScoringParams scoringParams, WifiStateMachine wifiStateMachine, WifiScanner wifiScanner, WifiConfigManager wifiConfigManager, WifiInfo wifiInfo, WifiNetworkSelector wifiNetworkSelector, WifiConnectivityHelper wifiConnectivityHelper, WifiLastResortWatchdog wifiLastResortWatchdog, OpenNetworkNotifier openNetworkNotifier, CarrierNetworkNotifier carrierNetworkNotifier, CarrierNetworkConfig carrierNetworkConfig, WifiMetrics wifiMetrics, Looper looper, Clock clock, LocalLog localLog, boolean z, FrameworkFacade frameworkFacade, SavedNetworkEvaluator savedNetworkEvaluator, ScoredNetworkEvaluator scoredNetworkEvaluator, PasspointNetworkEvaluator passpointNetworkEvaluator) {
        this.mWifiConnectivityManagerEnabled = true;
        this.mUseSingleRadioChainScanResults = SCAN_ON_SCHEDULE;
        this.mAllSingleScanListener = new AllSingleScanListener();
        this.mPnoScanListener = new PnoScanListener();
        this.mStateMachine = wifiStateMachine;
        this.mScanner = wifiScanner;
        this.mConfigManager = wifiConfigManager;
        this.mWifiInfo = wifiInfo;
        this.mNetworkSelector = wifiNetworkSelector;
        this.mConnectivityHelper = wifiConnectivityHelper;
        this.mLocalLog = localLog;
        this.mWifiLastResortWatchdog = wifiLastResortWatchdog;
        this.mOpenNetworkNotifier = openNetworkNotifier;
        this.mCarrierNetworkNotifier = carrierNetworkNotifier;
        this.mCarrierNetworkConfig = carrierNetworkConfig;
        this.mWifiMetrics = wifiMetrics;
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mEventHandler = new Handler(looper);
        this.mClock = clock;
        this.mScoringParams = scoringParams;
        this.mMin5GHzRssi = this.mScoringParams.getEntryRssi(ScoringParams.BAND5);
        this.mMin24GHzRssi = this.mScoringParams.getEntryRssi(ScoringParams.BAND2);
        this.mBand5GHzBonus = context.getResources().getInteger(R.integer.config_hoverTapTimeoutMillis);
        this.mCurrentConnectionBonus = context.getResources().getInteger(R.integer.config_letterboxDefaultPositionForTabletopModeReachability);
        this.mSameNetworkBonus = context.getResources().getInteger(R.integer.config_jobSchedulerUserGracePeriod);
        this.mSecureBonus = context.getResources().getInteger(R.integer.config_jumpTapTimeoutMillis);
        this.mEnableAutoJoinWhenAssociated = context.getResources().getBoolean(R.^attr-private.productId);
        this.mUseSingleRadioChainScanResults = context.getResources().getBoolean(R.^attr-private.progressLayout);
        this.mInitialScoreMax = (Math.max(this.mScoringParams.getGoodRssi(ScoringParams.BAND2), this.mScoringParams.getGoodRssi(ScoringParams.BAND5)) + context.getResources().getInteger(R.integer.config_jobSchedulerInactivityIdleThreshold)) * context.getResources().getInteger(R.integer.config_jobSchedulerInactivityIdleThresholdOnStablePower);
        this.mFullScanMaxTxRate = context.getResources().getInteger(R.integer.config_lidNavigationAccessibility);
        this.mFullScanMaxRxRate = context.getResources().getInteger(R.integer.config_lidKeyboardAccessibility);
        localLog("PNO settings: min5GHzRssi " + this.mMin5GHzRssi + " min24GHzRssi " + this.mMin24GHzRssi + " currentConnectionBonus " + this.mCurrentConnectionBonus + " sameNetworkBonus " + this.mSameNetworkBonus + " secureNetworkBonus " + this.mSecureBonus + " initialScoreMax " + this.mInitialScoreMax);
        boolean zHasSystemFeature = context.getPackageManager().hasSystemFeature("android.hardware.wifi.passpoint");
        StringBuilder sb = new StringBuilder();
        sb.append("Passpoint is: ");
        sb.append(zHasSystemFeature ? "enabled" : "disabled");
        localLog(sb.toString());
        this.mNetworkSelector.registerNetworkEvaluator(savedNetworkEvaluator, 1);
        if (zHasSystemFeature) {
            this.mNetworkSelector.registerNetworkEvaluator(passpointNetworkEvaluator, 2);
        }
        this.mNetworkSelector.registerNetworkEvaluator(scoredNetworkEvaluator, 3);
        this.mScanner.registerScanListener(this.mAllSingleScanListener);
        this.mConfigManager.setOnSavedNetworkUpdateListener(new OnSavedNetworkUpdateListener());
        this.mWifiConnectivityManagerEnabled = z;
        StringBuilder sb2 = new StringBuilder();
        sb2.append("ConnectivityScanManager initialized and ");
        sb2.append(z ? "enabled" : "disabled");
        localLog(sb2.toString());
    }

    private boolean shouldSkipConnectionAttempt(Long l) {
        Iterator<Long> it = this.mConnectionAttemptTimeStamps.iterator();
        while (it.hasNext()) {
            if (l.longValue() - it.next().longValue() <= 240000) {
                break;
            }
            it.remove();
        }
        if (this.mConnectionAttemptTimeStamps.size() >= 6) {
            return true;
        }
        return SCAN_ON_SCHEDULE;
    }

    private void noteConnectionAttempt(Long l) {
        this.mConnectionAttemptTimeStamps.addLast(l);
    }

    private void clearConnectionAttemptTimeStamps() {
        this.mConnectionAttemptTimeStamps.clear();
    }

    private void connectToNetwork(WifiConfiguration wifiConfiguration) {
        String str;
        ScanResult candidate = wifiConfiguration.getNetworkSelectionStatus().getCandidate();
        if (candidate == null) {
            localLog("connectToNetwork: bad candidate - " + wifiConfiguration + " scanResult: " + candidate);
            return;
        }
        String str2 = candidate.BSSID;
        String str3 = wifiConfiguration.SSID + " : " + str2;
        if (str2 != null && ((str2.equals(this.mLastConnectionAttemptBssid) || str2.equals(this.mWifiInfo.getBSSID())) && SupplicantState.isConnecting(this.mWifiInfo.getSupplicantState()))) {
            localLog("connectToNetwork: Either already connected or is connecting to " + str3);
            return;
        }
        if (wifiConfiguration.BSSID != null && !wifiConfiguration.BSSID.equals("any") && !wifiConfiguration.BSSID.equals(str2)) {
            localLog("connecToNetwork: target BSSID " + str2 + " does not match the config specified BSSID " + wifiConfiguration.BSSID + ". Drop it!");
            return;
        }
        long elapsedSinceBootMillis = this.mClock.getElapsedSinceBootMillis();
        if (!this.mScreenOn && shouldSkipConnectionAttempt(Long.valueOf(elapsedSinceBootMillis))) {
            localLog("connectToNetwork: Too many connection attempts. Skipping this attempt!");
            this.mTotalConnectivityAttemptsRateLimited++;
            return;
        }
        noteConnectionAttempt(Long.valueOf(elapsedSinceBootMillis));
        this.mLastConnectionAttemptBssid = str2;
        WifiConfiguration configuredNetwork = this.mConfigManager.getConfiguredNetwork(this.mWifiInfo.getNetworkId());
        if (configuredNetwork == null) {
            str = "Disconnected";
        } else {
            str = this.mWifiInfo.getSSID() + " : " + this.mWifiInfo.getBSSID();
        }
        if (configuredNetwork != null && configuredNetwork.networkId == wifiConfiguration.networkId) {
            if (this.mConnectivityHelper.isFirmwareRoamingSupported()) {
                localLog("connectToNetwork: Roaming candidate - " + str3 + ". The actual roaming target is up to the firmware.");
                return;
            }
            localLog("connectToNetwork: Roaming to " + str3 + " from " + str);
            this.mStateMachine.startRoamToNetwork(wifiConfiguration.networkId, candidate);
            return;
        }
        if (this.mConnectivityHelper.isFirmwareRoamingSupported() && (wifiConfiguration.BSSID == null || wifiConfiguration.BSSID.equals("any"))) {
            str2 = "any";
            localLog("connectToNetwork: Connect to " + wifiConfiguration.SSID + ":any from " + str);
        } else {
            localLog("connectToNetwork: Connect to " + str3 + " from " + str);
        }
        this.mStateMachine.startConnectToNetwork(wifiConfiguration.networkId, 1010, str2);
    }

    private int getScanBand() {
        return getScanBand(true);
    }

    private int getScanBand(boolean z) {
        if (z) {
            return 7;
        }
        return 0;
    }

    private boolean setScanChannels(WifiScanner.ScanSettings scanSettings) {
        WifiConfiguration currentWifiConfiguration = this.mStateMachine.getCurrentWifiConfiguration();
        int i = 0;
        if (currentWifiConfiguration == null) {
            return SCAN_ON_SCHEDULE;
        }
        Set<Integer> setFetchChannelSetForNetworkForPartialScan = this.mConfigManager.fetchChannelSetForNetworkForPartialScan(currentWifiConfiguration.networkId, 3600000L, this.mWifiInfo.getFrequency());
        if (setFetchChannelSetForNetworkForPartialScan != null && setFetchChannelSetForNetworkForPartialScan.size() != 0) {
            scanSettings.channels = new WifiScanner.ChannelSpec[setFetchChannelSetForNetworkForPartialScan.size()];
            Iterator<Integer> it = setFetchChannelSetForNetworkForPartialScan.iterator();
            while (it.hasNext()) {
                scanSettings.channels[i] = new WifiScanner.ChannelSpec(it.next().intValue());
                i++;
            }
            return true;
        }
        localLog("No scan channels for " + currentWifiConfiguration.configKey() + ". Perform full band scan");
        return SCAN_ON_SCHEDULE;
    }

    private void watchdogHandler() {
        if (this.mWifiState == 2 && !this.mStateMachine.isP2pConnected()) {
            localLog("start a single scan from watchdogHandler");
            scheduleWatchdogTimer();
            startSingleScan(true, WifiStateMachine.WIFI_WORK_SOURCE);
        }
    }

    private void startPeriodicSingleScan() {
        long elapsedSinceBootMillis = this.mClock.getElapsedSinceBootMillis();
        if (this.mLastPeriodicSingleScanTimeStamp != RESET_TIME_STAMP) {
            long j = elapsedSinceBootMillis - this.mLastPeriodicSingleScanTimeStamp;
            if (j < 20000) {
                localLog("Last periodic single scan started " + j + "ms ago, defer this new scan request.");
                schedulePeriodicScanTimer(20000 - ((int) j));
                return;
            }
        }
        double d = this.mWifiInfo.txSuccessRate;
        double d2 = this.mFullScanMaxTxRate;
        boolean z = SCAN_ON_SCHEDULE;
        boolean z2 = true;
        boolean z3 = d > d2 || this.mWifiInfo.rxSuccessRate > ((double) this.mFullScanMaxRxRate);
        if (this.mWifiState == 1 && z3) {
            if (this.mConnectivityHelper.isFirmwareRoamingSupported()) {
                localLog("No partial scan because firmware roaming is supported.");
                z2 = false;
                z = true;
            } else {
                localLog("No full band scan due to ongoing traffic");
            }
        } else {
            z = true;
        }
        if (z2) {
            this.mLastPeriodicSingleScanTimeStamp = elapsedSinceBootMillis;
            startSingleScan(z, WifiStateMachine.WIFI_WORK_SOURCE);
            schedulePeriodicScanTimer(this.mPeriodicSingleScanInterval);
            this.mPeriodicSingleScanInterval *= 2;
            if (this.mPeriodicSingleScanInterval > 160000) {
                this.mPeriodicSingleScanInterval = 160000;
                return;
            }
            return;
        }
        schedulePeriodicScanTimer(this.mPeriodicSingleScanInterval);
    }

    private void resetLastPeriodicSingleScanTimeStamp() {
        this.mLastPeriodicSingleScanTimeStamp = RESET_TIME_STAMP;
    }

    private void periodicScanTimerHandler() {
        localLog("periodicScanTimerHandler");
        if (this.mScreenOn) {
            startPeriodicSingleScan();
        }
    }

    private void startSingleScan(boolean z, WorkSource workSource) {
        if (!this.mWifiEnabled || !this.mWifiConnectivityManagerEnabled) {
            return;
        }
        this.mPnoScanListener.resetLowRssiNetworkRetryDelay();
        WifiScanner.ScanSettings scanSettings = new WifiScanner.ScanSettings();
        if (!z && !setScanChannels(scanSettings)) {
            z = true;
        }
        scanSettings.type = 2;
        scanSettings.band = getScanBand(z);
        scanSettings.reportEvents = 3;
        scanSettings.numBssidsPerScan = 0;
        List<WifiScanner.ScanSettings.HiddenNetwork> listRetrieveHiddenNetworkList = this.mConfigManager.retrieveHiddenNetworkList();
        scanSettings.hiddenNetworks = (WifiScanner.ScanSettings.HiddenNetwork[]) listRetrieveHiddenNetworkList.toArray(new WifiScanner.ScanSettings.HiddenNetwork[listRetrieveHiddenNetworkList.size()]);
        this.mScanner.startScan(scanSettings, new SingleScanListener(z), workSource);
        this.mWifiMetrics.incrementConnectivityOneshotScanCount();
    }

    private void startPeriodicScan(boolean z) {
        this.mPnoScanListener.resetLowRssiNetworkRetryDelay();
        if (this.mWifiState == 1 && !this.mEnableAutoJoinWhenAssociated) {
            return;
        }
        if (z) {
            resetLastPeriodicSingleScanTimeStamp();
        }
        this.mPeriodicSingleScanInterval = PERIODIC_SCAN_INTERVAL_MS;
        startPeriodicSingleScan();
    }

    private void startDisconnectedPnoScan() {
        WifiScanner.PnoSettings pnoSettings = new WifiScanner.PnoSettings();
        List<WifiScanner.PnoSettings.PnoNetwork> listRetrievePnoNetworkList = this.mConfigManager.retrievePnoNetworkList();
        int size = listRetrievePnoNetworkList.size();
        if (size == 0) {
            localLog("No saved network for starting disconnected PNO.");
            return;
        }
        pnoSettings.networkList = new WifiScanner.PnoSettings.PnoNetwork[size];
        pnoSettings.networkList = (WifiScanner.PnoSettings.PnoNetwork[]) listRetrievePnoNetworkList.toArray(pnoSettings.networkList);
        pnoSettings.min5GHzRssi = this.mMin5GHzRssi;
        pnoSettings.min24GHzRssi = this.mMin24GHzRssi;
        pnoSettings.initialScoreMax = this.mInitialScoreMax;
        pnoSettings.currentConnectionBonus = this.mCurrentConnectionBonus;
        pnoSettings.sameNetworkBonus = this.mSameNetworkBonus;
        pnoSettings.secureBonus = this.mSecureBonus;
        pnoSettings.band5GHzBonus = this.mBand5GHzBonus;
        WifiScanner.ScanSettings scanSettings = new WifiScanner.ScanSettings();
        scanSettings.band = getScanBand();
        scanSettings.reportEvents = 4;
        scanSettings.numBssidsPerScan = 0;
        scanSettings.periodInMs = PERIODIC_SCAN_INTERVAL_MS;
        this.mPnoScanListener.clearScanDetails();
        this.mScanner.startDisconnectedPnoScan(scanSettings, pnoSettings, this.mPnoScanListener);
        this.mPnoScanStarted = true;
    }

    private void stopPnoScan() {
        if (this.mPnoScanStarted) {
            this.mScanner.stopPnoScan(this.mPnoScanListener);
        }
        this.mPnoScanStarted = SCAN_ON_SCHEDULE;
    }

    private void scheduleWatchdogTimer() {
        localLog("scheduleWatchdogTimer");
        this.mAlarmManager.set(2, 1200000 + this.mClock.getElapsedSinceBootMillis(), WATCHDOG_TIMER_TAG, this.mWatchdogListener, this.mEventHandler);
    }

    private void schedulePeriodicScanTimer(int i) {
        localLog("schedulePeriodicScanTimer, intervalMs: " + i);
        this.mAlarmManager.set(2, ((long) i) + this.mClock.getElapsedSinceBootMillis(), PERIODIC_SCAN_TIMER_TAG, this.mPeriodicScanTimerListener, this.mEventHandler);
        this.mPeriodicScanTimerSet = true;
    }

    private void cancelPeriodicScanTimer() {
        if (this.mPeriodicScanTimerSet) {
            this.mAlarmManager.cancel(this.mPeriodicScanTimerListener);
            this.mPeriodicScanTimerSet = SCAN_ON_SCHEDULE;
        }
    }

    private void scheduleDelayedSingleScan(boolean z) {
        localLog("scheduleDelayedSingleScan");
        this.mAlarmManager.set(2, 2000 + this.mClock.getElapsedSinceBootMillis(), RESTART_SINGLE_SCAN_TIMER_TAG, new RestartSingleScanListener(z), this.mEventHandler);
    }

    private void scheduleDelayedConnectivityScan(int i) {
        localLog("scheduleDelayedConnectivityScan");
        this.mAlarmManager.set(2, ((long) i) + this.mClock.getElapsedSinceBootMillis(), RESTART_CONNECTIVITY_SCAN_TIMER_TAG, this.mRestartScanListener, this.mEventHandler);
    }

    private void startConnectivityScan(boolean z) {
        localLog("startConnectivityScan: screenOn=" + this.mScreenOn + " wifiState=" + stateToString(this.mWifiState) + " scanImmediately=" + z + " wifiEnabled=" + this.mWifiEnabled + " wifiConnectivityManagerEnabled=" + this.mWifiConnectivityManagerEnabled);
        if (!this.mWifiEnabled || !this.mWifiConnectivityManagerEnabled) {
            return;
        }
        stopConnectivityScan();
        if ((this.mWifiState != 1 && this.mWifiState != 2) || this.mStateMachine.isTemporarilyDontReconnectWifi()) {
            return;
        }
        if (this.mScreenOn) {
            startPeriodicScan(z);
        } else if (this.mWifiState == 2 && !this.mPnoScanStarted) {
            startDisconnectedPnoScan();
        }
    }

    private void stopConnectivityScan() {
        cancelPeriodicScanTimer();
        stopPnoScan();
        this.mScanRestartCount = 0;
    }

    public void handleScreenStateChanged(boolean z) {
        localLog("handleScreenStateChanged: screenOn=" + z);
        this.mScreenOn = z;
        this.mOpenNetworkNotifier.handleScreenStateChanged(z);
        this.mCarrierNetworkNotifier.handleScreenStateChanged(z);
        startConnectivityScan(SCAN_ON_SCHEDULE);
    }

    private static String stateToString(int i) {
        switch (i) {
            case 1:
                return "connected";
            case 2:
                return "disconnected";
            case 3:
                return "transitioning";
            default:
                return "unknown";
        }
    }

    public void handleConnectionStateChanged(int i) {
        localLog("handleConnectionStateChanged: state=" + stateToString(i));
        this.mWifiState = i;
        if (this.mWifiState == 1) {
            this.mOpenNetworkNotifier.handleWifiConnected();
            this.mCarrierNetworkNotifier.handleWifiConnected();
        }
        if (this.mWifiState == 2) {
            this.mLastConnectionAttemptBssid = null;
            scheduleWatchdogTimer();
            startConnectivityScan(true);
            return;
        }
        startConnectivityScan(SCAN_ON_SCHEDULE);
    }

    public void handleConnectionAttemptEnded(int i) {
        if (i != 1) {
            this.mOpenNetworkNotifier.handleConnectionFailure();
            this.mCarrierNetworkNotifier.handleConnectionFailure();
        }
    }

    public void setUntrustedConnectionAllowed(boolean z) {
        localLog("setUntrustedConnectionAllowed: allowed=" + z);
        if (this.mUntrustedConnectionAllowed != z) {
            this.mUntrustedConnectionAllowed = z;
            startConnectivityScan(true);
        }
    }

    public void setUserConnectChoice(int i) {
        localLog("setUserConnectChoice: netId=" + i);
        this.mNetworkSelector.setUserConnectChoice(i);
    }

    public void prepareForForcedConnection(int i) {
        localLog("prepareForForcedConnection: netId=" + i);
        clearConnectionAttemptTimeStamps();
        clearBssidBlacklist();
    }

    public void forceConnectivityScan(WorkSource workSource) {
        localLog("forceConnectivityScan in request of " + workSource);
        this.mWaitForFullBandScanResults = true;
        startSingleScan(true, workSource);
    }

    private boolean updateBssidBlacklist(String str, boolean z, int i) {
        if (z) {
            if (this.mBssidBlacklist.remove(str) != null) {
                return true;
            }
            return SCAN_ON_SCHEDULE;
        }
        BssidBlacklistStatus bssidBlacklistStatus = this.mBssidBlacklist.get(str);
        if (bssidBlacklistStatus == null) {
            bssidBlacklistStatus = new BssidBlacklistStatus();
            this.mBssidBlacklist.put(str, bssidBlacklistStatus);
        }
        bssidBlacklistStatus.blacklistedTimeStamp = this.mClock.getElapsedSinceBootMillis();
        bssidBlacklistStatus.counter++;
        if (bssidBlacklistStatus.isBlacklisted || (bssidBlacklistStatus.counter < 3 && i != 17)) {
            return SCAN_ON_SCHEDULE;
        }
        bssidBlacklistStatus.isBlacklisted = true;
        return true;
    }

    public boolean trackBssid(String str, boolean z, int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("trackBssid: ");
        sb.append(z ? "enable " : "disable ");
        sb.append(str);
        sb.append(" reason code ");
        sb.append(i);
        localLog(sb.toString());
        if (str == null || !updateBssidBlacklist(str, z, i)) {
            return SCAN_ON_SCHEDULE;
        }
        updateFirmwareRoamingConfiguration();
        if (!z) {
            startConnectivityScan(true);
        }
        return true;
    }

    @VisibleForTesting
    public boolean isBssidDisabled(String str) {
        BssidBlacklistStatus bssidBlacklistStatus = this.mBssidBlacklist.get(str);
        return bssidBlacklistStatus == null ? SCAN_ON_SCHEDULE : bssidBlacklistStatus.isBlacklisted;
    }

    private HashSet<String> buildBssidBlacklist() {
        HashSet<String> hashSet = new HashSet<>();
        for (String str : this.mBssidBlacklist.keySet()) {
            if (isBssidDisabled(str)) {
                hashSet.add(str);
            }
        }
        return hashSet;
    }

    private void updateFirmwareRoamingConfiguration() {
        if (!this.mConnectivityHelper.isFirmwareRoamingSupported()) {
            return;
        }
        int maxNumBlacklistBssid = this.mConnectivityHelper.getMaxNumBlacklistBssid();
        if (maxNumBlacklistBssid <= 0) {
            Log.wtf(TAG, "Invalid max BSSID blacklist size:  " + maxNumBlacklistBssid);
            return;
        }
        ArrayList<String> arrayList = new ArrayList<>(buildBssidBlacklist());
        int size = arrayList.size();
        if (size > maxNumBlacklistBssid) {
            Log.wtf(TAG, "Attempt to write " + size + " blacklisted BSSIDs, max size is " + maxNumBlacklistBssid);
            ArrayList<String> arrayList2 = new ArrayList<>(arrayList.subList(0, maxNumBlacklistBssid));
            localLog("Trim down BSSID blacklist size from " + size + " to " + arrayList2.size());
            arrayList = arrayList2;
        }
        if (!this.mConnectivityHelper.setFirmwareRoamingConfiguration(arrayList, new ArrayList<>())) {
            localLog("Failed to set firmware roaming configuration.");
        }
    }

    private void refreshBssidBlacklist() {
        if (this.mBssidBlacklist.isEmpty()) {
            return;
        }
        boolean z = SCAN_ON_SCHEDULE;
        Iterator<BssidBlacklistStatus> it = this.mBssidBlacklist.values().iterator();
        Long lValueOf = Long.valueOf(this.mClock.getElapsedSinceBootMillis());
        while (it.hasNext()) {
            BssidBlacklistStatus next = it.next();
            if (next.isBlacklisted && lValueOf.longValue() - next.blacklistedTimeStamp >= 300000) {
                it.remove();
                z = true;
            }
        }
        if (z) {
            updateFirmwareRoamingConfiguration();
        }
    }

    private void clearBssidBlacklist() {
        this.mBssidBlacklist.clear();
        updateFirmwareRoamingConfiguration();
    }

    private void start() {
        this.mConnectivityHelper.getFirmwareRoamingInfo();
        clearBssidBlacklist();
        startConnectivityScan(true);
    }

    private void stop() {
        stopConnectivityScan();
        clearBssidBlacklist();
        resetLastPeriodicSingleScanTimeStamp();
        this.mOpenNetworkNotifier.clearPendingNotification(true);
        this.mCarrierNetworkNotifier.clearPendingNotification(true);
        this.mLastConnectionAttemptBssid = null;
        this.mWaitForFullBandScanResults = SCAN_ON_SCHEDULE;
    }

    private void updateRunningState() {
        if (this.mWifiEnabled && this.mWifiConnectivityManagerEnabled) {
            localLog("Starting up WifiConnectivityManager");
            start();
        } else {
            localLog("Stopping WifiConnectivityManager");
            stop();
        }
    }

    public void setWifiEnabled(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("Set WiFi ");
        sb.append(z ? "enabled" : "disabled");
        localLog(sb.toString());
        this.mWifiEnabled = z;
        updateRunningState();
    }

    public void enable(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("Set WiFiConnectivityManager ");
        sb.append(z ? "enabled" : "disabled");
        localLog(sb.toString());
        this.mWifiConnectivityManagerEnabled = z;
        updateRunningState();
    }

    @VisibleForTesting
    int getLowRssiNetworkRetryDelay() {
        return this.mPnoScanListener.getLowRssiNetworkRetryDelay();
    }

    @VisibleForTesting
    long getLastPeriodicSingleScanTimeStamp() {
        return this.mLastPeriodicSingleScanTimeStamp;
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("Dump of WifiConnectivityManager");
        printWriter.println("WifiConnectivityManager - Log Begin ----");
        this.mLocalLog.dump(fileDescriptor, printWriter, strArr);
        printWriter.println("WifiConnectivityManager - Log End ----");
        this.mOpenNetworkNotifier.dump(fileDescriptor, printWriter, strArr);
        this.mCarrierNetworkNotifier.dump(fileDescriptor, printWriter, strArr);
    }

    public void handleScanStrategyChanged() {
        localLog("handleScanStrategyChanged");
        startConnectivityScan(SCAN_ON_SCHEDULE);
    }
}
