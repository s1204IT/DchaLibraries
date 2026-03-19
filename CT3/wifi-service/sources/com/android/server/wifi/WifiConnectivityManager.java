package com.android.server.wifi;

import android.R;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiScanner;
import android.os.Handler;
import android.os.Looper;
import android.util.LocalLog;
import android.util.Log;
import com.android.server.wifi.anqp.Constants;
import com.android.server.wifi.util.ScanDetailUtil;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class WifiConnectivityManager {
    private static final int CHANNEL_LIST_AGE_MS = 3600000;
    private static final int CONNECTED_PNO_SCAN_INTERVAL_MS = 160000;
    private static final int DISCONNECTED_PNO_SCAN_INTERVAL_MS = 20000;
    private static final boolean ENABLE_BACKGROUND_SCAN = false;
    private static final boolean ENABLE_CONNECTED_PNO_SCAN = false;
    private static final int LOW_RSSI_NETWORK_RETRY_MAX_DELAY_MS = 80000;
    private static final int LOW_RSSI_NETWORK_RETRY_START_DELAY_MS = 20000;
    public static final int MAX_CONNECTION_ATTEMPTS_RATE = 6;
    public static final int MAX_CONNECTION_ATTEMPTS_TIME_INTERVAL_MS = 240000;
    public static final int MAX_PERIODIC_SCAN_INTERVAL_MS = 160000;
    private static final int MAX_SCAN_RESTART_ALLOWED = 5;
    public static final int PERIODIC_SCAN_INTERVAL_MS = 20000;
    public static final String PERIODIC_SCAN_TIMER_TAG = "WifiConnectivityManager Schedule Periodic Scan Timer";
    private static final long RESET_TIME_STAMP = Long.MIN_VALUE;
    public static final String RESTART_CONNECTIVITY_SCAN_TIMER_TAG = "WifiConnectivityManager Restart Scan";
    private static final int RESTART_SCAN_DELAY_MS = 2000;
    public static final String RESTART_SINGLE_SCAN_TIMER_TAG = "WifiConnectivityManager Restart Single Scan";
    private static final boolean SCAN_IMMEDIATELY = true;
    private static final boolean SCAN_ON_SCHEDULE = false;
    private static final String TAG = "WifiConnectivityManager";
    public static final String T_PUT_MONITOR_TIMER_TAG = "WifiConnectivityManager Schedule T-put monitor Timer";
    private static final int WATCHDOG_INTERVAL_MS = 1200000;
    public static final String WATCHDOG_TIMER_TAG = "WifiConnectivityManager Schedule Watchdog Timer";
    public static final int WIFI_STATE_CONNECTED = 1;
    public static final int WIFI_STATE_DISCONNECTED = 2;
    public static final int WIFI_STATE_TRANSITIONING = 3;
    public static final int WIFI_STATE_UNKNOWN = 0;
    private final AlarmManager mAlarmManager;
    private boolean mBackoffScanCheckTputSupport;
    private int mBackoffScanInterval;
    private int mBand5GHzBonus;
    private final Clock mClock;
    private final WifiConfigManager mConfigManager;
    private final LinkedList<Long> mConnectionAttemptTimeStamps;
    private int mCurrentConnectionBonus;
    private boolean mDbg;
    private final Handler mEventHandler;
    private int mInitialScoreMax;
    private String mLastConnectionAttemptBssid;
    private long mLastPeriodicSingleScanTimeStamp;
    private boolean mLinkQualityBad;
    private final LocalLog mLocalLog;
    private int mMin24GHzRssi;
    private int mMin5GHzRssi;
    private final PeriodicScanListener mPeriodicScanListener;
    private final AlarmManager.OnAlarmListener mPeriodicScanTimerListener;
    private int mPeriodicSingleScanInterval;
    private final PnoScanListener mPnoScanListener;
    private final WifiQualifiedNetworkSelector mQualifiedNetworkSelector;
    private final AlarmManager.OnAlarmListener mRestartScanListener;
    private int mSameNetworkBonus;
    private int mScanRestartCount;
    private final WifiScanner mScanner;
    private boolean mScreenOn;
    private boolean mScreenOnConnectedBackoffScanSupport;
    private boolean mScreenOnConnectedScanEnabled;
    private int mSecureBonus;
    private int mSingleScanRestartCount;
    private final WifiStateMachine mStateMachine;
    private int mTotalConnectivityAttemptsRateLimited;
    private final AlarmManager.OnAlarmListener mTputMonitorTimerListener;
    private boolean mUntrustedConnectionAllowed;
    private final AlarmManager.OnAlarmListener mWatchdogListener;
    private boolean mWifiConnectivityManagerEnabled;
    private boolean mWifiEnabled;
    private final WifiInfo mWifiInfo;
    private final WifiLastResortWatchdog mWifiLastResortWatchdog;
    private final WifiMetrics mWifiMetrics;
    private int mWifiState;

    private void localLog(String log) {
        this.mLocalLog.log(log);
        Log.d(TAG, log);
    }

    private class RestartSingleScanListener implements AlarmManager.OnAlarmListener {
        private final boolean mIsFullBandScan;
        private final boolean mIsWatchdogTriggered;

        RestartSingleScanListener(boolean isWatchdogTriggered, boolean isFullBandScan) {
            this.mIsWatchdogTriggered = isWatchdogTriggered;
            this.mIsFullBandScan = isFullBandScan;
        }

        @Override
        public void onAlarm() {
            WifiConnectivityManager.this.startSingleScan(this.mIsWatchdogTriggered, this.mIsFullBandScan);
        }
    }

    private boolean handleScanResults(List<ScanDetail> scanDetails, String listenerName) throws Throwable {
        localLog(listenerName + " onResults: start QNS");
        WifiConfiguration candidate = this.mQualifiedNetworkSelector.selectQualifiedNetwork(false, this.mUntrustedConnectionAllowed, scanDetails, this.mStateMachine.isLinkDebouncing(), this.mStateMachine.isConnected(), this.mStateMachine.isDisconnected(), this.mStateMachine.isSupplicantTransientState());
        this.mWifiLastResortWatchdog.updateAvailableNetworks(this.mQualifiedNetworkSelector.getFilteredScanDetails());
        if (candidate == null) {
            return false;
        }
        localLog(listenerName + ": QNS candidate-" + candidate.SSID);
        connectToNetwork(candidate);
        return true;
    }

    private class PeriodicScanListener implements WifiScanner.ScanListener {
        private List<ScanDetail> mScanDetails;

        PeriodicScanListener(WifiConnectivityManager this$0, PeriodicScanListener periodicScanListener) {
            this();
        }

        private PeriodicScanListener() {
            this.mScanDetails = new ArrayList();
        }

        public void clearScanDetails() {
            this.mScanDetails.clear();
        }

        public void onSuccess() {
            WifiConnectivityManager.this.localLog("PeriodicScanListener onSuccess");
            WifiConnectivityManager.this.mScanRestartCount = 0;
        }

        public void onFailure(int reason, String description) {
            Log.e(WifiConnectivityManager.TAG, "PeriodicScanListener onFailure: reason: " + reason + " description: " + description);
            WifiConnectivityManager wifiConnectivityManager = WifiConnectivityManager.this;
            int i = wifiConnectivityManager.mScanRestartCount;
            wifiConnectivityManager.mScanRestartCount = i + 1;
            if (i < 5) {
                WifiConnectivityManager.this.scheduleDelayedConnectivityScan(WifiConnectivityManager.RESTART_SCAN_DELAY_MS);
            } else {
                WifiConnectivityManager.this.mScanRestartCount = 0;
                Log.e(WifiConnectivityManager.TAG, "Failed to successfully start periodic scan for 5 times");
            }
        }

        public void onPeriodChanged(int periodInMs) {
            WifiConnectivityManager.this.localLog("PeriodicScanListener onPeriodChanged: actual scan period " + periodInMs + "ms");
        }

        public void onResults(WifiScanner.ScanData[] results) throws Throwable {
            WifiConnectivityManager.this.localLog("PeriodicScanListener onResults: " + results);
            WifiConnectivityManager.this.handleScanResults(this.mScanDetails, "PeriodicScanListener");
            clearScanDetails();
        }

        public void onFullResult(ScanResult fullScanResult) {
            if (WifiConnectivityManager.this.mDbg) {
                WifiConnectivityManager.this.localLog("PeriodicScanListener onFullResult: " + fullScanResult.SSID + " capabilities " + fullScanResult.capabilities);
            }
            this.mScanDetails.add(ScanDetailUtil.toScanDetail(fullScanResult));
        }
    }

    private class SingleScanListener implements WifiScanner.ScanListener {
        private final boolean mIsFullBandScan;
        private final boolean mIsWatchdogTriggered;
        private List<ScanDetail> mScanDetails = new ArrayList();

        SingleScanListener(boolean isWatchdogTriggered, boolean isFullBandScan) {
            this.mIsWatchdogTriggered = isWatchdogTriggered;
            this.mIsFullBandScan = isFullBandScan;
        }

        public void clearScanDetails() {
            this.mScanDetails.clear();
        }

        public void onSuccess() {
            WifiConnectivityManager.this.localLog("SingleScanListener onSuccess");
            WifiConnectivityManager.this.mSingleScanRestartCount = 0;
        }

        public void onFailure(int reason, String description) {
            Log.e(WifiConnectivityManager.TAG, "SingleScanListener onFailure: reason: " + reason + " description: " + description);
            WifiConnectivityManager wifiConnectivityManager = WifiConnectivityManager.this;
            int i = wifiConnectivityManager.mSingleScanRestartCount;
            wifiConnectivityManager.mSingleScanRestartCount = i + 1;
            if (i < 5) {
                WifiConnectivityManager.this.scheduleDelayedSingleScan(this.mIsWatchdogTriggered, this.mIsFullBandScan);
            } else {
                WifiConnectivityManager.this.mSingleScanRestartCount = 0;
                Log.e(WifiConnectivityManager.TAG, "Failed to successfully start single scan for 5 times");
            }
        }

        public void onPeriodChanged(int periodInMs) {
            WifiConnectivityManager.this.localLog("SingleScanListener onPeriodChanged: actual scan period " + periodInMs + "ms");
        }

        public void onResults(WifiScanner.ScanData[] results) throws Throwable {
            WifiConnectivityManager.this.localLog("SingleScanListener onResults: " + results);
            boolean wasConnectAttempted = WifiConnectivityManager.this.handleScanResults(this.mScanDetails, "SingleScanListener");
            clearScanDetails();
            if (!this.mIsWatchdogTriggered) {
                return;
            }
            if (wasConnectAttempted) {
                if (WifiConnectivityManager.this.mScreenOn) {
                    WifiConnectivityManager.this.mWifiMetrics.incrementNumConnectivityWatchdogBackgroundBad();
                    return;
                } else {
                    WifiConnectivityManager.this.mWifiMetrics.incrementNumConnectivityWatchdogPnoBad();
                    return;
                }
            }
            if (WifiConnectivityManager.this.mScreenOn) {
                WifiConnectivityManager.this.mWifiMetrics.incrementNumConnectivityWatchdogBackgroundGood();
            } else {
                WifiConnectivityManager.this.mWifiMetrics.incrementNumConnectivityWatchdogPnoGood();
            }
        }

        public void onFullResult(ScanResult fullScanResult) {
            if (WifiConnectivityManager.this.mDbg) {
                WifiConnectivityManager.this.localLog("SingleScanListener onFullResult: " + fullScanResult.SSID + " capabilities " + fullScanResult.capabilities);
            }
            this.mScanDetails.add(ScanDetailUtil.toScanDetail(fullScanResult));
        }
    }

    private class PnoScanListener implements WifiScanner.PnoScanListener {
        private int mLowRssiNetworkRetryDelay;
        private List<ScanDetail> mScanDetails;

        PnoScanListener(WifiConnectivityManager this$0, PnoScanListener pnoScanListener) {
            this();
        }

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

        public int getLowRssiNetworkRetryDelay() {
            return this.mLowRssiNetworkRetryDelay;
        }

        public void onSuccess() {
            WifiConnectivityManager.this.localLog("PnoScanListener onSuccess");
            WifiConnectivityManager.this.mScanRestartCount = 0;
        }

        public void onFailure(int reason, String description) {
            Log.e(WifiConnectivityManager.TAG, "PnoScanListener onFailure: reason: " + reason + " description: " + description);
            WifiConnectivityManager wifiConnectivityManager = WifiConnectivityManager.this;
            int i = wifiConnectivityManager.mScanRestartCount;
            wifiConnectivityManager.mScanRestartCount = i + 1;
            if (i < 5) {
                WifiConnectivityManager.this.scheduleDelayedConnectivityScan(WifiConnectivityManager.RESTART_SCAN_DELAY_MS);
            } else {
                WifiConnectivityManager.this.mScanRestartCount = 0;
                Log.e(WifiConnectivityManager.TAG, "Failed to successfully start PNO scan for 5 times");
            }
        }

        public void onPeriodChanged(int periodInMs) {
            WifiConnectivityManager.this.localLog("PnoScanListener onPeriodChanged: actual scan period " + periodInMs + "ms");
        }

        public void onResults(WifiScanner.ScanData[] results) {
        }

        public void onFullResult(ScanResult fullScanResult) {
        }

        public void onPnoNetworkFound(ScanResult[] results) throws Throwable {
            WifiConnectivityManager.this.localLog("PnoScanListener: onPnoNetworkFound: results len = " + results.length);
            for (ScanResult result : results) {
                this.mScanDetails.add(ScanDetailUtil.toScanDetail(result));
            }
            boolean wasConnectAttempted = WifiConnectivityManager.this.handleScanResults(this.mScanDetails, "PnoScanListener");
            clearScanDetails();
            if (!wasConnectAttempted) {
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

    public WifiConnectivityManager(Context context, WifiStateMachine wifiStateMachine, WifiScanner wifiScanner, WifiConfigManager wifiConfigManager, WifiInfo wifiInfo, WifiQualifiedNetworkSelector wifiQualifiedNetworkSelector, WifiInjector wifiInjector, Looper looper) {
        PeriodicScanListener periodicScanListener = null;
        Object[] objArr = 0;
        this.mLocalLog = new LocalLog(ActivityManager.isLowRamDeviceStatic() ? Constants.ANQP_QUERY_LIST : 1024);
        this.mDbg = false;
        this.mWifiEnabled = false;
        this.mWifiConnectivityManagerEnabled = true;
        this.mScreenOn = false;
        this.mWifiState = 0;
        this.mUntrustedConnectionAllowed = false;
        this.mScanRestartCount = 0;
        this.mSingleScanRestartCount = 0;
        this.mTotalConnectivityAttemptsRateLimited = 0;
        this.mLastConnectionAttemptBssid = null;
        this.mPeriodicSingleScanInterval = PERIODIC_SCAN_INTERVAL_MS;
        this.mLastPeriodicSingleScanTimeStamp = RESET_TIME_STAMP;
        this.mLinkQualityBad = false;
        this.mScreenOnConnectedScanEnabled = false;
        this.mScreenOnConnectedBackoffScanSupport = false;
        this.mBackoffScanCheckTputSupport = false;
        this.mBackoffScanInterval = PERIODIC_SCAN_INTERVAL_MS;
        this.mRestartScanListener = new AlarmManager.OnAlarmListener() {
            @Override
            public void onAlarm() {
                WifiConnectivityManager.this.startConnectivityScan(true);
            }
        };
        this.mWatchdogListener = new AlarmManager.OnAlarmListener() {
            @Override
            public void onAlarm() {
                WifiConnectivityManager.this.watchdogHandler();
            }
        };
        this.mPeriodicScanTimerListener = new AlarmManager.OnAlarmListener() {
            @Override
            public void onAlarm() {
                WifiConnectivityManager.this.periodicScanTimerHandler();
            }
        };
        this.mPeriodicScanListener = new PeriodicScanListener(this, periodicScanListener);
        this.mPnoScanListener = new PnoScanListener(this, objArr == true ? 1 : 0);
        this.mTputMonitorTimerListener = new AlarmManager.OnAlarmListener() {
            @Override
            public void onAlarm() {
                WifiConnectivityManager.this.tputMonitorTimerHandler();
            }
        };
        this.mStateMachine = wifiStateMachine;
        this.mScanner = wifiScanner;
        this.mConfigManager = wifiConfigManager;
        this.mWifiInfo = wifiInfo;
        this.mQualifiedNetworkSelector = wifiQualifiedNetworkSelector;
        this.mWifiLastResortWatchdog = wifiInjector.getWifiLastResortWatchdog();
        this.mWifiMetrics = wifiInjector.getWifiMetrics();
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mEventHandler = new Handler(looper);
        this.mClock = wifiInjector.getClock();
        this.mConnectionAttemptTimeStamps = new LinkedList<>();
        this.mMin5GHzRssi = -82;
        this.mMin24GHzRssi = -85;
        this.mBand5GHzBonus = 40;
        this.mCurrentConnectionBonus = this.mConfigManager.mCurrentNetworkBoost.get();
        this.mSameNetworkBonus = context.getResources().getInteger(R.integer.config_am_tieredCachedAdjUiTierSize);
        this.mSecureBonus = context.getResources().getInteger(R.integer.config_attentiveTimeout);
        this.mInitialScoreMax = (this.mConfigManager.mThresholdSaturatedRssi24.get() + 85) * 4;
        this.mScreenOnConnectedBackoffScanSupport = context.getResources().getBoolean(135004163);
        this.mBackoffScanCheckTputSupport = context.getResources().getBoolean(135004164);
        Log.i(TAG, "PNO settings: min5GHzRssi " + this.mMin5GHzRssi + " min24GHzRssi " + this.mMin24GHzRssi + " currentConnectionBonus " + this.mCurrentConnectionBonus + " sameNetworkBonus " + this.mSameNetworkBonus + " secureNetworkBonus " + this.mSecureBonus + " initialScoreMax " + this.mInitialScoreMax + " backoffscan " + this.mScreenOnConnectedBackoffScanSupport + " backoffscanTput " + this.mBackoffScanCheckTputSupport);
        Log.i(TAG, "ConnectivityScanManager initialized ");
    }

    private boolean shouldSkipConnectionAttempt(Long timeMillis) {
        Iterator<Long> attemptIter = this.mConnectionAttemptTimeStamps.iterator();
        while (attemptIter.hasNext()) {
            Long connectionAttemptTimeMillis = attemptIter.next();
            if (timeMillis.longValue() - connectionAttemptTimeMillis.longValue() <= 240000) {
                break;
            }
            attemptIter.remove();
        }
        return this.mConnectionAttemptTimeStamps.size() >= 6;
    }

    private void noteConnectionAttempt(Long timeMillis) {
        this.mConnectionAttemptTimeStamps.addLast(timeMillis);
    }

    private void clearConnectionAttemptTimeStamps() {
        this.mConnectionAttemptTimeStamps.clear();
    }

    private void connectToNetwork(WifiConfiguration candidate) {
        ScanResult scanResultCandidate = candidate.getNetworkSelectionStatus().getCandidate();
        if (scanResultCandidate == null) {
            Log.e(TAG, "connectToNetwork: bad candidate - " + candidate + " scanResult: " + scanResultCandidate);
            return;
        }
        String targetBssid = scanResultCandidate.BSSID;
        String targetAssociationId = candidate.SSID + " : " + targetBssid;
        if (targetBssid != null && ((targetBssid.equals(this.mLastConnectionAttemptBssid) || targetBssid.equals(this.mWifiInfo.getBSSID())) && SupplicantState.isConnecting(this.mWifiInfo.getSupplicantState()))) {
            localLog("connectToNetwork: Either already connected or is connecting to " + targetAssociationId);
            return;
        }
        if (!this.mStateMachine.shouldSwitchNetwork()) {
            return;
        }
        Long elapsedTimeMillis = Long.valueOf(this.mClock.elapsedRealtime());
        if (!this.mScreenOn && shouldSkipConnectionAttempt(elapsedTimeMillis)) {
            localLog("connectToNetwork: Too many connection attempts. Skipping this attempt!");
            this.mTotalConnectivityAttemptsRateLimited++;
            return;
        }
        noteConnectionAttempt(elapsedTimeMillis);
        this.mLastConnectionAttemptBssid = targetBssid;
        WifiConfiguration currentConnectedNetwork = this.mConfigManager.getWifiConfiguration(this.mWifiInfo.getNetworkId());
        String currentAssociationId = currentConnectedNetwork == null ? "Disconnected" : this.mWifiInfo.getSSID() + " : " + this.mWifiInfo.getBSSID();
        if (currentConnectedNetwork != null && (currentConnectedNetwork.networkId == candidate.networkId || currentConnectedNetwork.isLinked(candidate))) {
            localLog("connectToNetwork: Roaming from " + currentAssociationId + " to " + targetAssociationId);
            this.mStateMachine.autoRoamToNetwork(candidate.networkId, scanResultCandidate);
        } else {
            localLog("connectToNetwork: Reconnect from " + currentAssociationId + " to " + targetAssociationId);
            this.mStateMachine.autoConnectToNetwork(candidate.networkId, scanResultCandidate.BSSID);
        }
    }

    private int getScanBand() {
        return getScanBand(true);
    }

    private int getScanBand(boolean isFullBandScan) {
        if (isFullBandScan) {
            int freqBand = this.mStateMachine.getFrequencyBand();
            if (freqBand == 1) {
                return 6;
            }
            return freqBand == 2 ? 1 : 7;
        }
        return 0;
    }

    private boolean setScanChannels(WifiScanner.ScanSettings settings) {
        WifiConfiguration config = this.mStateMachine.getCurrentWifiConfiguration();
        if (config == null) {
            return false;
        }
        HashSet<Integer> freqs = this.mConfigManager.makeChannelList(config, CHANNEL_LIST_AGE_MS);
        if (freqs != null && freqs.size() != 0) {
            int index = 0;
            settings.channels = new WifiScanner.ChannelSpec[freqs.size()];
            for (Integer freq : freqs) {
                settings.channels[index] = new WifiScanner.ChannelSpec(freq.intValue());
                index++;
            }
            return true;
        }
        localLog("No scan channels for " + config.configKey() + ". Perform full band scan");
        return false;
    }

    private void watchdogHandler() {
        localLog("watchdogHandler");
        if (this.mWifiState != 2) {
            return;
        }
        Log.i(TAG, "start a single scan from watchdogHandler");
        scheduleWatchdogTimer();
        startSingleScan(true, true);
    }

    private void startPeriodicSingleScan() {
        long currentTimeStamp = this.mClock.elapsedRealtime();
        if (this.mLastPeriodicSingleScanTimeStamp != RESET_TIME_STAMP) {
            long msSinceLastScan = currentTimeStamp - this.mLastPeriodicSingleScanTimeStamp;
            if (msSinceLastScan < 20000) {
                localLog("Last periodic single scan started " + msSinceLastScan + "ms ago, defer this new scan request.");
                schedulePeriodicScanTimer(20000 - ((int) msSinceLastScan));
                return;
            }
        }
        boolean isFullBandScan = true;
        boolean scanEnable = true;
        if (this.mWifiState == 1 && (this.mWifiInfo.txSuccessRate > 8.0d || this.mWifiInfo.rxSuccessRate > 16.0d)) {
            localLog("No full band scan due to heavy traffic, txSuccessRate=" + this.mWifiInfo.txSuccessRate + " rxSuccessRate=" + this.mWifiInfo.rxSuccessRate);
            isFullBandScan = false;
        }
        if (this.mWifiState == 1 && (this.mWifiInfo.txSuccessRate > 40.0d || this.mWifiInfo.rxSuccessRate > 80.0d)) {
            localLog("No scan due to heavy traffic, txSuccessRate=" + this.mWifiInfo.txSuccessRate + " rxSuccessRate=" + this.mWifiInfo.rxSuccessRate);
            scanEnable = false;
        }
        if ((this.mWifiState == 1 && this.mWifiInfo.is24GHz() && this.mWifiInfo.getLinkSpeed() < 6 && this.mWifiInfo.getRssi() < -70) || (this.mWifiInfo.is5GHz() && this.mWifiInfo.getLinkSpeed() <= 9 && this.mWifiInfo.getRssi() < -70)) {
            scanEnable = false;
        }
        this.mLastPeriodicSingleScanTimeStamp = currentTimeStamp;
        if (scanEnable) {
            startSingleScan(false, isFullBandScan);
        }
        schedulePeriodicScanTimer(this.mPeriodicSingleScanInterval);
        this.mPeriodicSingleScanInterval *= 2;
        if (this.mPeriodicSingleScanInterval > 160000) {
            this.mPeriodicSingleScanInterval = 160000;
        }
        if (this.mWifiState == 2) {
            this.mPeriodicSingleScanInterval = PERIODIC_SCAN_INTERVAL_MS;
        }
    }

    private void resetLastPeriodicSingleScanTimeStamp() {
        this.mLastPeriodicSingleScanTimeStamp = RESET_TIME_STAMP;
    }

    private void periodicScanTimerHandler() {
        localLog("periodicScanTimerHandler");
        if (!this.mScreenOn) {
            return;
        }
        startPeriodicSingleScan();
    }

    private void startSingleScan(boolean isWatchdogTriggered, boolean isFullBandScan) {
        if (!this.mWifiEnabled || !this.mWifiConnectivityManagerEnabled) {
            return;
        }
        this.mPnoScanListener.resetLowRssiNetworkRetryDelay();
        WifiScanner.ScanSettings settings = new WifiScanner.ScanSettings();
        if (!isFullBandScan && !setScanChannels(settings)) {
            isFullBandScan = true;
        }
        settings.band = getScanBand(isFullBandScan);
        settings.reportEvents = 3;
        settings.numBssidsPerScan = 0;
        Set<Integer> hiddenNetworkIds = this.mConfigManager.getHiddenConfiguredNetworkIds();
        if (hiddenNetworkIds != null && hiddenNetworkIds.size() > 0) {
            int i = 0;
            settings.hiddenNetworkIds = new int[hiddenNetworkIds.size()];
            for (Integer netId : hiddenNetworkIds) {
                settings.hiddenNetworkIds[i] = netId.intValue();
                i++;
            }
        }
        SingleScanListener singleScanListener = new SingleScanListener(isWatchdogTriggered, isFullBandScan);
        localLog("startSingleScan: " + settings);
        this.mScanner.startScan(settings, singleScanListener, WifiStateMachine.WIFI_WORK_SOURCE);
    }

    private void startPeriodicScan(boolean scanImmediately) {
        this.mPnoScanListener.resetLowRssiNetworkRetryDelay();
        if (scanImmediately) {
            resetLastPeriodicSingleScanTimeStamp();
        }
        this.mPeriodicSingleScanInterval = PERIODIC_SCAN_INTERVAL_MS;
        startPeriodicSingleScan();
    }

    private void startDisconnectedPnoScan() {
        WifiScanner.PnoSettings pnoSettings = new WifiScanner.PnoSettings();
        ArrayList<WifiScanner.PnoSettings.PnoNetwork> pnoNetworkList = this.mConfigManager.retrieveDisconnectedPnoNetworkList();
        int listSize = pnoNetworkList.size();
        if (listSize == 0) {
            localLog("No saved network for starting disconnected PNO.");
            return;
        }
        pnoSettings.networkList = new WifiScanner.PnoSettings.PnoNetwork[listSize];
        pnoSettings.networkList = (WifiScanner.PnoSettings.PnoNetwork[]) pnoNetworkList.toArray(pnoSettings.networkList);
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
        localLog("startDisconnectedPnoScan: " + scanSettings);
        this.mScanner.startDisconnectedPnoScan(scanSettings, pnoSettings, this.mPnoScanListener);
    }

    private void startConnectedPnoScan() {
    }

    private void scheduleWatchdogTimer() {
        Log.i(TAG, "scheduleWatchdogTimer 1200000");
        this.mAlarmManager.set(2, this.mClock.elapsedRealtime() + 1200000, WATCHDOG_TIMER_TAG, this.mWatchdogListener, this.mEventHandler);
    }

    private void schedulePeriodicScanTimer(int intervalMs) {
        Log.i(TAG, "schedulePeriodicScanTimer: " + intervalMs);
        this.mAlarmManager.set(2, this.mClock.elapsedRealtime() + ((long) intervalMs), PERIODIC_SCAN_TIMER_TAG, this.mPeriodicScanTimerListener, this.mEventHandler);
    }

    private void scheduleDelayedSingleScan(boolean isWatchdogTriggered, boolean isFullBandScan) {
        localLog("scheduleDelayedSingleScan");
        RestartSingleScanListener restartSingleScanListener = new RestartSingleScanListener(isWatchdogTriggered, isFullBandScan);
        this.mAlarmManager.set(2, this.mClock.elapsedRealtime() + 2000, RESTART_SINGLE_SCAN_TIMER_TAG, restartSingleScanListener, this.mEventHandler);
    }

    private void scheduleDelayedConnectivityScan(int msFromNow) {
        localLog("scheduleDelayedConnectivityScan");
        this.mAlarmManager.set(2, this.mClock.elapsedRealtime() + ((long) msFromNow), RESTART_CONNECTIVITY_SCAN_TIMER_TAG, this.mRestartScanListener, this.mEventHandler);
    }

    private void startConnectivityScan(boolean scanImmediately) {
        localLog("startConnectivityScan: screenOn=" + this.mScreenOn + " wifiState=" + this.mWifiState + " scanImmediately=" + scanImmediately + " wifiEnabled=" + this.mWifiEnabled + " wifiConnectivityManagerEnabled=" + this.mWifiConnectivityManagerEnabled);
        if (this.mWifiEnabled && this.mWifiConnectivityManagerEnabled) {
            stopConnectivityScan();
            if ((this.mWifiState == 1 || this.mWifiState == 2) && !this.mStateMachine.isTemporarilyDontReconnectWifi()) {
                if (this.mScreenOn) {
                    startPeriodicScan(scanImmediately);
                } else if (this.mWifiState == 1) {
                    startConnectedPnoScan();
                } else {
                    startDisconnectedPnoScan();
                }
            }
        }
    }

    private void stopConnectivityScan() {
        this.mAlarmManager.cancel(this.mPeriodicScanTimerListener);
        this.mScanner.stopPnoScan(this.mPnoScanListener);
        this.mScanRestartCount = 0;
    }

    public void handleScreenStateChanged(boolean screenOn) {
        localLog("handleScreenStateChanged: screenOn=" + screenOn);
        this.mScreenOn = screenOn;
        startConnectivityScan(false);
    }

    public void handleConnectionStateChanged(int state) {
        localLog("handleConnectionStateChanged: state=" + state);
        boolean isScanImmediate = false;
        this.mWifiState = state;
        if (this.mWifiState == 2) {
            scheduleWatchdogTimer();
            isScanImmediate = true;
        }
        startConnectivityScan(isScanImmediate);
    }

    public void setUntrustedConnectionAllowed(boolean allowed) {
        Log.i(TAG, "setUntrustedConnectionAllowed: allowed=" + allowed);
        if (this.mUntrustedConnectionAllowed == allowed) {
            return;
        }
        this.mUntrustedConnectionAllowed = allowed;
        startConnectivityScan(true);
    }

    public void connectToUserSelectNetwork(int netId, boolean persistent) {
        Log.i(TAG, "connectToUserSelectNetwork: netId=" + netId + " persist=" + persistent);
        this.mQualifiedNetworkSelector.userSelectNetwork(netId, persistent);
        clearConnectionAttemptTimeStamps();
    }

    public void forceConnectivityScan() {
        Log.i(TAG, "forceConnectivityScan");
        startConnectivityScan(true);
    }

    public boolean trackBssid(String bssid, boolean enable) {
        Log.i(TAG, "trackBssid: " + (enable ? "enable " : "disable ") + bssid);
        boolean ret = this.mQualifiedNetworkSelector.enableBssidForQualityNetworkSelection(bssid, enable);
        if (ret && !enable) {
            startConnectivityScan(true);
        }
        return ret;
    }

    public void setUserPreferredBand(int band) {
        Log.i(TAG, "User band preference: " + band);
        this.mQualifiedNetworkSelector.setUserPreferredBand(band);
        startConnectivityScan(true);
    }

    public void setWifiEnabled(boolean enable) {
        Log.i(TAG, "Set WiFi " + (enable ? "enabled" : "disabled"));
        this.mWifiEnabled = enable;
        if (this.mWifiEnabled) {
            return;
        }
        stopConnectivityScan();
        resetLastPeriodicSingleScanTimeStamp();
    }

    public void enable(boolean enable) {
        Log.i(TAG, "Set WiFiConnectivityManager " + (enable ? "enabled" : "disabled"));
        this.mWifiConnectivityManagerEnabled = enable;
        if (this.mWifiConnectivityManagerEnabled) {
            return;
        }
        stopConnectivityScan();
        resetLastPeriodicSingleScanTimeStamp();
    }

    public void enableVerboseLogging(int verbose) {
        this.mDbg = verbose > 0;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("Dump of WifiConnectivityManager");
        pw.println("WifiConnectivityManager - Log Begin ----");
        pw.println("WifiConnectivityManager - Number of connectivity attempts rate limited: " + this.mTotalConnectivityAttemptsRateLimited);
        this.mLocalLog.dump(fd, pw, args);
        pw.println("WifiConnectivityManager - Log End ----");
    }

    int getLowRssiNetworkRetryDelay() {
        return this.mPnoScanListener.getLowRssiNetworkRetryDelay();
    }

    long getLastPeriodicSingleScanTimeStamp() {
        return this.mLastPeriodicSingleScanTimeStamp;
    }

    public void handleScanStrategyChanged() {
        localLog("handleScanStrategyChanged");
        startConnectivityScan(false);
    }

    private void startBackOffPeriodicScan(boolean scanImmediately, boolean isFullBandScan) {
        this.mPnoScanListener.resetLowRssiNetworkRetryDelay();
        if (scanImmediately) {
            resetLastPeriodicSingleScanTimeStamp();
        }
        this.mPeriodicSingleScanInterval = PERIODIC_SCAN_INTERVAL_MS;
        startPeriodicSingleScan();
    }

    private void scheduleTputCheckTimer(int intervalMs) {
        Log.i(TAG, "scheduleTputCheckTimer: " + intervalMs);
        this.mAlarmManager.set(2, this.mClock.elapsedRealtime() + ((long) intervalMs), T_PUT_MONITOR_TIMER_TAG, this.mTputMonitorTimerListener, this.mEventHandler);
    }

    private void tputMonitorTimerHandler() {
        localLog("tputMonitorTimerHandler");
        if (!this.mScreenOn) {
            return;
        }
        checkTputForScan();
    }

    private void checkTputForScan() {
        boolean scanEnable = true;
        this.mBackoffScanInterval *= 2;
        if (this.mBackoffScanInterval > 160000) {
            this.mBackoffScanInterval = 160000;
        }
        if (this.mWifiInfo == null) {
            localLog("error: no mWifiInfo return");
            return;
        }
        double txSuccessRate = this.mWifiInfo.txSuccessRate;
        double rxSuccessRate = this.mWifiInfo.rxSuccessRate;
        this.mWifiInfo.is24GHz();
        this.mWifiInfo.is5GHz();
        int linkspeed = this.mWifiInfo.getLinkSpeed();
        localLog("txSuccessRate=" + txSuccessRate + " rxSuccessRate=" + rxSuccessRate + " linkspeed=" + linkspeed);
        if (this.mWifiState == 1 && (txSuccessRate > 40.0d || rxSuccessRate > 80.0d)) {
            scanEnable = false;
        }
        if ((this.mWifiInfo.is24GHz() && this.mWifiInfo.getLinkSpeed() < 6 && this.mWifiInfo.getRssi() < -70) || (this.mWifiInfo.is5GHz() && this.mWifiInfo.getLinkSpeed() <= 9 && this.mWifiInfo.getRssi() < -70)) {
            scanEnable = false;
        }
        if (this.mScreenOnConnectedScanEnabled != scanEnable) {
            if (!scanEnable) {
                localLog("stop BackOffPeriodicScan due to low link quality or tput high");
                this.mScanner.stopBackgroundScan(this.mPeriodicScanListener);
                this.mScreenOnConnectedScanEnabled = false;
                scheduleTputCheckTimer(this.mBackoffScanInterval);
                return;
            }
            boolean isFullBandScan = shouldFullBandScan();
            localLog("restart startBackOffPeriodicScan");
            startBackOffPeriodicScan(true, isFullBandScan);
            return;
        }
        scheduleTputCheckTimer(this.mBackoffScanInterval);
    }

    boolean shouldFullBandScan() {
        if (this.mWifiState != 1) {
            return true;
        }
        if (this.mWifiInfo.txSuccessRate <= 8.0d && this.mWifiInfo.rxSuccessRate <= 16.0d) {
            return true;
        }
        localLog("No full band scan due to heavy traffic, txSuccessRate=" + this.mWifiInfo.txSuccessRate + " rxSuccessRate=" + this.mWifiInfo.rxSuccessRate);
        return false;
    }
}
