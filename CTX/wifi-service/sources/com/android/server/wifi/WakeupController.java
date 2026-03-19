package com.android.server.wifi;

import android.content.Context;
import android.database.ContentObserver;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiScanner;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.WakeupConfigStoreData;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class WakeupController {
    private static final String TAG = "WakeupController";
    private static final boolean USE_PLATFORM_WIFI_WAKE = true;
    private final ContentObserver mContentObserver;
    private final Context mContext;
    private final FrameworkFacade mFrameworkFacade;
    private final Handler mHandler;
    private boolean mVerboseLoggingEnabled;
    private final WakeupConfigStoreData mWakeupConfigStoreData;
    private final WakeupEvaluator mWakeupEvaluator;
    private final WakeupLock mWakeupLock;
    private final WakeupOnboarding mWakeupOnboarding;
    private final WifiConfigManager mWifiConfigManager;
    private final WifiInjector mWifiInjector;
    private final WifiWakeMetrics mWifiWakeMetrics;
    private boolean mWifiWakeupEnabled;
    private final WifiScanner.ScanListener mScanListener = new WifiScanner.ScanListener() {
        public void onPeriodChanged(int i) {
        }

        public void onResults(WifiScanner.ScanData[] scanDataArr) {
            if (scanDataArr.length == 1 && scanDataArr[0].isAllChannelsScanned()) {
                WakeupController.this.handleScanResults(WakeupController.this.filterDfsScanResults(Arrays.asList(scanDataArr[0].getResults())));
            }
        }

        public void onFullResult(ScanResult scanResult) {
        }

        public void onSuccess() {
        }

        public void onFailure(int i, String str) {
            Log.e(WakeupController.TAG, "ScanListener onFailure: " + i + ": " + str);
        }
    };
    private boolean mIsActive = false;
    private int mNumScansHandled = 0;

    public WakeupController(Context context, Looper looper, WakeupLock wakeupLock, WakeupEvaluator wakeupEvaluator, WakeupOnboarding wakeupOnboarding, WifiConfigManager wifiConfigManager, WifiConfigStore wifiConfigStore, WifiWakeMetrics wifiWakeMetrics, WifiInjector wifiInjector, FrameworkFacade frameworkFacade) {
        this.mContext = context;
        this.mHandler = new Handler(looper);
        this.mWakeupLock = wakeupLock;
        this.mWakeupEvaluator = wakeupEvaluator;
        this.mWakeupOnboarding = wakeupOnboarding;
        this.mWifiConfigManager = wifiConfigManager;
        this.mWifiWakeMetrics = wifiWakeMetrics;
        this.mFrameworkFacade = frameworkFacade;
        this.mWifiInjector = wifiInjector;
        this.mContentObserver = new ContentObserver(this.mHandler) {
            @Override
            public void onChange(boolean z) {
                WakeupController.this.readWifiWakeupEnabledFromSettings();
                WakeupController.this.mWakeupOnboarding.setOnboarded();
            }
        };
        this.mFrameworkFacade.registerContentObserver(this.mContext, Settings.Global.getUriFor("wifi_wakeup_enabled"), true, this.mContentObserver);
        readWifiWakeupEnabledFromSettings();
        this.mWakeupConfigStoreData = new WakeupConfigStoreData(new IsActiveDataSource(), this.mWakeupOnboarding.getIsOnboadedDataSource(), this.mWakeupOnboarding.getNotificationsDataSource(), this.mWakeupLock.getDataSource());
        wifiConfigStore.registerStoreData(this.mWakeupConfigStoreData);
    }

    private void readWifiWakeupEnabledFromSettings() {
        this.mWifiWakeupEnabled = this.mFrameworkFacade.getIntegerSetting(this.mContext, "wifi_wakeup_enabled", 0) == 1;
        StringBuilder sb = new StringBuilder();
        sb.append("WifiWake ");
        sb.append(this.mWifiWakeupEnabled ? "enabled" : "disabled");
        Log.d(TAG, sb.toString());
    }

    private void setActive(boolean z) {
        if (this.mIsActive != z) {
            Log.d(TAG, "Setting active to " + z);
            this.mIsActive = z;
            this.mWifiConfigManager.saveToStore(false);
        }
    }

    public void start() {
        Log.d(TAG, "start()");
        this.mWifiInjector.getWifiScanner().registerScanListener(this.mScanListener);
        if (this.mIsActive) {
            this.mWifiWakeMetrics.recordIgnoredStart();
            return;
        }
        setActive(true);
        if (isEnabled()) {
            this.mWakeupOnboarding.maybeShowNotification();
            Set<ScanResultMatchInfo> matchInfos = toMatchInfos(filterDfsScanResults(this.mWifiInjector.getWifiScanner().getSingleScanResults()));
            matchInfos.retainAll(getGoodSavedNetworks());
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "Saved networks in most recent scan:" + matchInfos);
            }
            this.mWifiWakeMetrics.recordStartEvent(matchInfos.size());
            this.mWakeupLock.setLock(matchInfos);
        }
    }

    public void stop() {
        Log.d(TAG, "stop()");
        this.mWifiInjector.getWifiScanner().deregisterScanListener(this.mScanListener);
        this.mWakeupOnboarding.onStop();
    }

    public void reset() {
        Log.d(TAG, "reset()");
        this.mWifiWakeMetrics.recordResetEvent(this.mNumScansHandled);
        this.mNumScansHandled = 0;
        setActive(false);
    }

    public void enableVerboseLogging(int i) {
        this.mVerboseLoggingEnabled = i > 0;
        this.mWakeupLock.enableVerboseLogging(this.mVerboseLoggingEnabled);
    }

    private List<ScanResult> filterDfsScanResults(Collection<ScanResult> collection) {
        int[] channelsForBand = this.mWifiInjector.getWifiNative().getChannelsForBand(4);
        if (channelsForBand == null) {
            channelsForBand = new int[0];
        }
        final Set set = (Set) Arrays.stream(channelsForBand).boxed().collect(Collectors.toSet());
        return (List) collection.stream().filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return WakeupController.lambda$filterDfsScanResults$0(set, (ScanResult) obj);
            }
        }).collect(Collectors.toList());
    }

    static boolean lambda$filterDfsScanResults$0(Set set, ScanResult scanResult) {
        return !set.contains(Integer.valueOf(scanResult.frequency));
    }

    private Set<ScanResultMatchInfo> getGoodSavedNetworks() {
        List<WifiConfiguration> savedNetworks = this.mWifiConfigManager.getSavedNetworks();
        HashSet hashSet = new HashSet(savedNetworks.size());
        for (WifiConfiguration wifiConfiguration : savedNetworks) {
            if (!isWideAreaNetwork(wifiConfiguration) && !wifiConfiguration.hasNoInternetAccess() && !wifiConfiguration.noInternetAccessExpected && wifiConfiguration.getNetworkSelectionStatus().getHasEverConnected()) {
                hashSet.add(ScanResultMatchInfo.fromWifiConfiguration(wifiConfiguration));
            }
        }
        return hashSet;
    }

    private static boolean isWideAreaNetwork(WifiConfiguration wifiConfiguration) {
        return false;
    }

    private void handleScanResults(Collection<ScanResult> collection) {
        ScanResult scanResultFindViableNetwork;
        if (!isEnabled()) {
            Log.d(TAG, "Attempted to handleScanResults while not enabled");
            return;
        }
        this.mNumScansHandled++;
        if (this.mVerboseLoggingEnabled) {
            Log.d(TAG, "Incoming scan #" + this.mNumScansHandled);
        }
        this.mWakeupOnboarding.maybeShowNotification();
        Set<ScanResultMatchInfo> goodSavedNetworks = getGoodSavedNetworks();
        Set<ScanResultMatchInfo> matchInfos = toMatchInfos(collection);
        matchInfos.retainAll(goodSavedNetworks);
        this.mWakeupLock.update(matchInfos);
        if (this.mWakeupLock.isUnlocked() && (scanResultFindViableNetwork = this.mWakeupEvaluator.findViableNetwork(collection, goodSavedNetworks)) != null) {
            Log.d(TAG, "Enabling wifi for network: " + scanResultFindViableNetwork.SSID);
            enableWifi();
        }
    }

    private static Set<ScanResultMatchInfo> toMatchInfos(Collection<ScanResult> collection) {
        return (Set) collection.stream().map(new Function() {
            @Override
            public final Object apply(Object obj) {
                return ScanResultMatchInfo.fromScanResult((ScanResult) obj);
            }
        }).collect(Collectors.toSet());
    }

    private void enableWifi() {
        if (this.mWifiInjector.getWifiSettingsStore().handleWifiToggled(true)) {
            this.mWifiInjector.getWifiController().sendMessage(155656);
            this.mWifiWakeMetrics.recordWakeupEvent(this.mNumScansHandled);
        }
    }

    @VisibleForTesting
    boolean isEnabled() {
        return this.mWifiWakeupEnabled && this.mWakeupConfigStoreData.hasBeenRead();
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("Dump of WakeupController");
        printWriter.println("USE_PLATFORM_WIFI_WAKE: true");
        printWriter.println("mWifiWakeupEnabled: " + this.mWifiWakeupEnabled);
        printWriter.println("isOnboarded: " + this.mWakeupOnboarding.isOnboarded());
        printWriter.println("configStore hasBeenRead: " + this.mWakeupConfigStoreData.hasBeenRead());
        printWriter.println("mIsActive: " + this.mIsActive);
        printWriter.println("mNumScansHandled: " + this.mNumScansHandled);
        this.mWakeupLock.dump(fileDescriptor, printWriter, strArr);
    }

    private class IsActiveDataSource implements WakeupConfigStoreData.DataSource<Boolean> {
        private IsActiveDataSource() {
        }

        @Override
        public Boolean getData() {
            return Boolean.valueOf(WakeupController.this.mIsActive);
        }

        @Override
        public void setData(Boolean bool) {
            WakeupController.this.mIsActive = bool.booleanValue();
        }
    }
}
