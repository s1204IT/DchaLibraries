package com.android.server.wifi;

import android.R;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.Context;
import android.net.NetworkScoreManager;
import android.net.wifi.IWifiScanner;
import android.net.wifi.IWificond;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkScoreCache;
import android.net.wifi.WifiScanner;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserManager;
import android.security.KeyStore;
import android.telephony.TelephonyManager;
import android.util.LocalLog;
import com.android.internal.app.IBatteryStats;
import com.android.internal.os.PowerProfile;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.BatteryStatsService;
import com.android.server.net.DelayedDiskWrite;
import com.android.server.net.IpConfigStore;
import com.android.server.wifi.ClientModeManager;
import com.android.server.wifi.ScanOnlyModeManager;
import com.android.server.wifi.WifiConfigStoreLegacy;
import com.android.server.wifi.aware.WifiAwareMetrics;
import com.android.server.wifi.hotspot2.LegacyPasspointConfigParser;
import com.android.server.wifi.hotspot2.PasspointManager;
import com.android.server.wifi.hotspot2.PasspointNetworkEvaluator;
import com.android.server.wifi.hotspot2.PasspointObjectFactory;
import com.android.server.wifi.p2p.SupplicantP2pIfaceHal;
import com.android.server.wifi.p2p.WifiP2pMonitor;
import com.android.server.wifi.p2p.WifiP2pNative;
import com.android.server.wifi.rtt.RttMetrics;
import com.android.server.wifi.util.WifiPermissionsUtil;
import com.android.server.wifi.util.WifiPermissionsWrapper;
import com.mediatek.server.wifi.MtkSoftApManager;
import com.mediatek.server.wifi.MtkWifiStateMachinePrime;

public class WifiInjector {
    private static final String BOOT_DEFAULT_WIFI_COUNTRY_CODE = "ro.boot.wificountrycode";
    private static final String WIFICOND_SERVICE_NAME = "wificond";
    static WifiInjector sWifiInjector = null;
    private final IBatteryStats mBatteryStats;
    private final CarrierNetworkConfig mCarrierNetworkConfig;
    private final CarrierNetworkNotifier mCarrierNetworkNotifier;
    private final LocalLog mConnectivityLocalLog;
    private final Context mContext;
    private final WifiCountryCode mCountryCode;
    private HalDeviceManager mHalDeviceManager;
    private final HostapdHal mHostapdHal;
    private final IpConfigStore mIpConfigStore;
    private final WifiLockManager mLockManager;
    private final NetworkScoreManager mNetworkScoreManager;
    private final INetworkManagementService mNwManagementService;
    private final OpenNetworkNotifier mOpenNetworkNotifier;
    private final PasspointManager mPasspointManager;
    private final PasspointNetworkEvaluator mPasspointNetworkEvaluator;
    private HandlerThread mRttHandlerThread;
    private final SarManager mSarManager;
    private final SavedNetworkEvaluator mSavedNetworkEvaluator;
    private final ScanRequestProxy mScanRequestProxy;
    private final ScoredNetworkEvaluator mScoredNetworkEvaluator;
    private final ScoringParams mScoringParams;
    private final SelfRecovery mSelfRecovery;
    private final WifiSettingsStore mSettingsStore;
    private final SIMAccessor mSimAccessor;
    private final SupplicantP2pIfaceHal mSupplicantP2pIfaceHal;
    private final SupplicantStaIfaceHal mSupplicantStaIfaceHal;
    private final WifiTrafficPoller mTrafficPoller;
    private final boolean mUseRealLogger;
    private final WakeupController mWakeupController;
    private final WifiApConfigStore mWifiApConfigStore;
    private HandlerThread mWifiAwareHandlerThread;
    private final WifiBackupRestore mWifiBackupRestore;
    private final WifiConfigManager mWifiConfigManager;
    private final WifiConfigStore mWifiConfigStore;
    private final WifiConfigStoreLegacy mWifiConfigStoreLegacy;
    private final WifiConnectivityHelper mWifiConnectivityHelper;
    private final WifiController mWifiController;
    private final BaseWifiDiagnostics mWifiDiagnostics;
    private final WifiKeyStore mWifiKeyStore;
    private final WifiLastResortWatchdog mWifiLastResortWatchdog;
    private final WifiMetrics mWifiMetrics;
    private final WifiMonitor mWifiMonitor;
    private final WifiMulticastLockManager mWifiMulticastLockManager;
    private final WifiNative mWifiNative;
    private final WifiNetworkHistory mWifiNetworkHistory;
    private final WifiNetworkScoreCache mWifiNetworkScoreCache;
    private final WifiNetworkSelector mWifiNetworkSelector;
    private final WifiP2pMonitor mWifiP2pMonitor;
    private final WifiP2pNative mWifiP2pNative;
    private final WifiPermissionsUtil mWifiPermissionsUtil;
    private final WifiPermissionsWrapper mWifiPermissionsWrapper;
    private WifiScanner mWifiScanner;
    private final HandlerThread mWifiServiceHandlerThread;
    private final WifiStateMachine mWifiStateMachine;
    private final HandlerThread mWifiStateMachineHandlerThread;
    private final WifiStateMachinePrime mWifiStateMachinePrime;
    private final WifiStateTracker mWifiStateTracker;
    private final WifiVendorHal mWifiVendorHal;
    private final WificondControl mWificondControl;
    private final FrameworkFacade mFrameworkFacade = new FrameworkFacade();
    private final BackupManagerProxy mBackupManagerProxy = new BackupManagerProxy();
    private final Clock mClock = new Clock();
    private final PropertyService mPropertyService = new SystemPropertyService();
    private final BuildProperties mBuildProperties = new SystemBuildProperties();
    private final KeyStore mKeyStore = KeyStore.getInstance();

    public WifiInjector(Context context) {
        if (context == null) {
            throw new IllegalStateException("WifiInjector should not be initialized with a null Context.");
        }
        if (sWifiInjector != null) {
            throw new IllegalStateException("WifiInjector was already created, use getInstance instead.");
        }
        sWifiInjector = this;
        this.mContext = context;
        this.mUseRealLogger = this.mContext.getResources().getBoolean(R.^attr-private.preferencePanelStyle);
        this.mSettingsStore = new WifiSettingsStore(this.mContext);
        this.mWifiPermissionsWrapper = new WifiPermissionsWrapper(this.mContext);
        this.mNetworkScoreManager = (NetworkScoreManager) this.mContext.getSystemService(NetworkScoreManager.class);
        this.mWifiNetworkScoreCache = new WifiNetworkScoreCache(this.mContext);
        this.mNetworkScoreManager.registerNetworkScoreCache(1, this.mWifiNetworkScoreCache, 0);
        this.mWifiPermissionsUtil = new WifiPermissionsUtil(this.mWifiPermissionsWrapper, this.mContext, this.mSettingsStore, UserManager.get(this.mContext), this);
        this.mWifiBackupRestore = new WifiBackupRestore(this.mWifiPermissionsUtil);
        this.mBatteryStats = IBatteryStats.Stub.asInterface(this.mFrameworkFacade.getService("batterystats"));
        this.mWifiStateTracker = new WifiStateTracker(this.mBatteryStats);
        this.mWifiServiceHandlerThread = new HandlerThread("WifiService");
        this.mWifiServiceHandlerThread.start();
        this.mWifiStateMachineHandlerThread = new HandlerThread("WifiStateMachine");
        this.mWifiStateMachineHandlerThread.start();
        Looper looper = this.mWifiStateMachineHandlerThread.getLooper();
        this.mCarrierNetworkConfig = new CarrierNetworkConfig(this.mContext, this.mWifiServiceHandlerThread.getLooper(), this.mFrameworkFacade);
        this.mWifiMetrics = new WifiMetrics(this.mClock, looper, new WifiAwareMetrics(this.mClock), new RttMetrics(this.mClock));
        this.mWifiMonitor = new WifiMonitor(this);
        this.mHalDeviceManager = new HalDeviceManager(this.mClock);
        this.mWifiVendorHal = new WifiVendorHal(this.mHalDeviceManager, this.mWifiStateMachineHandlerThread.getLooper());
        this.mSupplicantStaIfaceHal = new SupplicantStaIfaceHal(this.mContext, this.mWifiMonitor);
        this.mHostapdHal = new HostapdHal(this.mContext);
        this.mWificondControl = new WificondControl(this, this.mWifiMonitor, this.mCarrierNetworkConfig);
        this.mNwManagementService = INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"));
        this.mWifiNative = new WifiNative(this.mWifiVendorHal, this.mSupplicantStaIfaceHal, this.mHostapdHal, this.mWificondControl, this.mWifiMonitor, this.mNwManagementService, this.mPropertyService, this.mWifiMetrics);
        this.mWifiP2pMonitor = new WifiP2pMonitor(this);
        this.mSupplicantP2pIfaceHal = new SupplicantP2pIfaceHal(this.mWifiP2pMonitor);
        this.mWifiP2pNative = new WifiP2pNative(this.mSupplicantP2pIfaceHal, this.mHalDeviceManager);
        this.mTrafficPoller = new WifiTrafficPoller(this.mContext, this.mWifiServiceHandlerThread.getLooper(), this.mWifiNative);
        this.mCountryCode = new WifiCountryCode(this.mWifiNative, SystemProperties.get(BOOT_DEFAULT_WIFI_COUNTRY_CODE), this.mContext.getResources().getBoolean(R.^attr-private.quickContactWindowSize));
        this.mWifiApConfigStore = new WifiApConfigStore(this.mContext, this.mBackupManagerProxy);
        this.mWifiKeyStore = new WifiKeyStore(this.mKeyStore);
        this.mWifiConfigStore = new WifiConfigStore(this.mContext, looper, this.mClock, WifiConfigStore.createSharedFile());
        DelayedDiskWrite delayedDiskWrite = new DelayedDiskWrite();
        this.mWifiNetworkHistory = new WifiNetworkHistory(this.mContext, delayedDiskWrite);
        this.mIpConfigStore = new IpConfigStore(delayedDiskWrite);
        this.mWifiConfigStoreLegacy = new WifiConfigStoreLegacy(this.mWifiNetworkHistory, this.mWifiNative, new WifiConfigStoreLegacy.IpConfigStoreWrapper(), new LegacyPasspointConfigParser());
        this.mWifiConfigManager = new WifiConfigManager(this.mContext, this.mClock, UserManager.get(this.mContext), TelephonyManager.from(this.mContext), this.mWifiKeyStore, this.mWifiConfigStore, this.mWifiConfigStoreLegacy, this.mWifiPermissionsUtil, this.mWifiPermissionsWrapper, new NetworkListStoreData(this.mContext), new DeletedEphemeralSsidsStoreData());
        this.mWifiMetrics.setWifiConfigManager(this.mWifiConfigManager);
        this.mWifiConnectivityHelper = new WifiConnectivityHelper(this.mWifiNative);
        this.mConnectivityLocalLog = new LocalLog(ActivityManager.isLowRamDeviceStatic() ? 256 : 512);
        this.mScoringParams = new ScoringParams(this.mContext, this.mFrameworkFacade, new Handler(looper));
        this.mWifiMetrics.setScoringParams(this.mScoringParams);
        this.mWifiNetworkSelector = new WifiNetworkSelector(this.mContext, this.mScoringParams, this.mWifiConfigManager, this.mClock, this.mConnectivityLocalLog);
        this.mWifiMetrics.setWifiNetworkSelector(this.mWifiNetworkSelector);
        this.mSavedNetworkEvaluator = new SavedNetworkEvaluator(this.mContext, this.mScoringParams, this.mWifiConfigManager, this.mClock, this.mConnectivityLocalLog, this.mWifiConnectivityHelper);
        this.mScoredNetworkEvaluator = new ScoredNetworkEvaluator(context, looper, this.mFrameworkFacade, this.mNetworkScoreManager, this.mWifiConfigManager, this.mConnectivityLocalLog, this.mWifiNetworkScoreCache, this.mWifiPermissionsUtil);
        this.mSimAccessor = new SIMAccessor(this.mContext);
        this.mPasspointManager = new PasspointManager(this.mContext, this.mWifiNative, this.mWifiKeyStore, this.mClock, this.mSimAccessor, new PasspointObjectFactory(), this.mWifiConfigManager, this.mWifiConfigStore, this.mWifiMetrics, this.mWifiPermissionsUtil);
        this.mPasspointNetworkEvaluator = new PasspointNetworkEvaluator(this.mPasspointManager, this.mWifiConfigManager, this.mConnectivityLocalLog);
        this.mWifiMetrics.setPasspointManager(this.mPasspointManager);
        this.mScanRequestProxy = new ScanRequestProxy(this.mContext, (AppOpsManager) this.mContext.getSystemService("appops"), (ActivityManager) this.mContext.getSystemService("activity"), this, this.mWifiConfigManager, this.mWifiPermissionsUtil, this.mWifiMetrics, this.mClock);
        this.mSarManager = new SarManager(this.mContext, makeTelephonyManager(), looper, this.mWifiNative);
        if (this.mUseRealLogger) {
            this.mWifiDiagnostics = new WifiDiagnostics(this.mContext, this, this.mWifiNative, this.mBuildProperties, new LastMileLogger(this));
        } else {
            this.mWifiDiagnostics = new BaseWifiDiagnostics(this.mWifiNative);
        }
        this.mWifiStateMachine = new WifiStateMachine(this.mContext, this.mFrameworkFacade, looper, UserManager.get(this.mContext), this, this.mBackupManagerProxy, this.mCountryCode, this.mWifiNative, new WrongPasswordNotifier(this.mContext, this.mFrameworkFacade), this.mSarManager);
        this.mWifiStateMachinePrime = new MtkWifiStateMachinePrime(this, this.mContext, looper, this.mWifiNative, new DefaultModeManager(this.mContext, looper), this.mBatteryStats);
        this.mOpenNetworkNotifier = new OpenNetworkNotifier(this.mContext, this.mWifiStateMachineHandlerThread.getLooper(), this.mFrameworkFacade, this.mClock, this.mWifiMetrics, this.mWifiConfigManager, this.mWifiConfigStore, this.mWifiStateMachine, new ConnectToNetworkNotificationBuilder(this.mContext, this.mFrameworkFacade));
        this.mCarrierNetworkNotifier = new CarrierNetworkNotifier(this.mContext, this.mWifiStateMachineHandlerThread.getLooper(), this.mFrameworkFacade, this.mClock, this.mWifiMetrics, this.mWifiConfigManager, this.mWifiConfigStore, this.mWifiStateMachine, new ConnectToNetworkNotificationBuilder(this.mContext, this.mFrameworkFacade));
        this.mWakeupController = new WakeupController(this.mContext, this.mWifiStateMachineHandlerThread.getLooper(), new WakeupLock(this.mWifiConfigManager, this.mWifiMetrics.getWakeupMetrics(), this.mClock), WakeupEvaluator.fromContext(this.mContext), new WakeupOnboarding(this.mContext, this.mWifiConfigManager, this.mWifiStateMachineHandlerThread.getLooper(), this.mFrameworkFacade, new WakeupNotificationFactory(this.mContext, this.mFrameworkFacade)), this.mWifiConfigManager, this.mWifiConfigStore, this.mWifiMetrics.getWakeupMetrics(), this, this.mFrameworkFacade);
        this.mLockManager = new WifiLockManager(this.mContext, BatteryStatsService.getService());
        this.mWifiController = new WifiController(this.mContext, this.mWifiStateMachine, looper, this.mSettingsStore, this.mWifiServiceHandlerThread.getLooper(), this.mFrameworkFacade, this.mWifiStateMachinePrime);
        this.mSelfRecovery = new SelfRecovery(this.mWifiController, this.mClock);
        this.mWifiLastResortWatchdog = new WifiLastResortWatchdog(this.mSelfRecovery, this.mClock, this.mWifiMetrics, this.mWifiStateMachine, looper);
        this.mWifiMulticastLockManager = new WifiMulticastLockManager(this.mWifiStateMachine.getMcastLockManagerFilterController(), BatteryStatsService.getService());
    }

    public static WifiInjector getInstance() {
        if (sWifiInjector == null) {
            throw new IllegalStateException("Attempted to retrieve a WifiInjector instance before constructor was called.");
        }
        return sWifiInjector;
    }

    public void enableVerboseLogging(int i) {
        this.mWifiLastResortWatchdog.enableVerboseLogging(i);
        this.mWifiBackupRestore.enableVerboseLogging(i);
        this.mHalDeviceManager.enableVerboseLogging(i);
        this.mScanRequestProxy.enableVerboseLogging(i);
        this.mWakeupController.enableVerboseLogging(i);
        LogcatLog.enableVerboseLogging(i);
    }

    public UserManager getUserManager() {
        return UserManager.get(this.mContext);
    }

    public WifiMetrics getWifiMetrics() {
        return this.mWifiMetrics;
    }

    public SupplicantStaIfaceHal getSupplicantStaIfaceHal() {
        return this.mSupplicantStaIfaceHal;
    }

    public BackupManagerProxy getBackupManagerProxy() {
        return this.mBackupManagerProxy;
    }

    public FrameworkFacade getFrameworkFacade() {
        return this.mFrameworkFacade;
    }

    public HandlerThread getWifiServiceHandlerThread() {
        return this.mWifiServiceHandlerThread;
    }

    public HandlerThread getWifiStateMachineHandlerThread() {
        return this.mWifiStateMachineHandlerThread;
    }

    public WifiTrafficPoller getWifiTrafficPoller() {
        return this.mTrafficPoller;
    }

    public WifiCountryCode getWifiCountryCode() {
        return this.mCountryCode;
    }

    public WifiApConfigStore getWifiApConfigStore() {
        return this.mWifiApConfigStore;
    }

    public SarManager getSarManager() {
        return this.mSarManager;
    }

    public WifiStateMachine getWifiStateMachine() {
        return this.mWifiStateMachine;
    }

    public Handler getWifiStateMachineHandler() {
        return this.mWifiStateMachine.getHandler();
    }

    public WifiStateMachinePrime getWifiStateMachinePrime() {
        return this.mWifiStateMachinePrime;
    }

    public WifiSettingsStore getWifiSettingsStore() {
        return this.mSettingsStore;
    }

    public WifiLockManager getWifiLockManager() {
        return this.mLockManager;
    }

    public WifiController getWifiController() {
        return this.mWifiController;
    }

    public WifiLastResortWatchdog getWifiLastResortWatchdog() {
        return this.mWifiLastResortWatchdog;
    }

    public Clock getClock() {
        return this.mClock;
    }

    public PropertyService getPropertyService() {
        return this.mPropertyService;
    }

    public BuildProperties getBuildProperties() {
        return this.mBuildProperties;
    }

    public KeyStore getKeyStore() {
        return this.mKeyStore;
    }

    public WifiBackupRestore getWifiBackupRestore() {
        return this.mWifiBackupRestore;
    }

    public WifiMulticastLockManager getWifiMulticastLockManager() {
        return this.mWifiMulticastLockManager;
    }

    public WifiConfigManager getWifiConfigManager() {
        return this.mWifiConfigManager;
    }

    public PasspointManager getPasspointManager() {
        return this.mPasspointManager;
    }

    public WakeupController getWakeupController() {
        return this.mWakeupController;
    }

    public ScoringParams getScoringParams() {
        return this.mScoringParams;
    }

    public TelephonyManager makeTelephonyManager() {
        return (TelephonyManager) this.mContext.getSystemService("phone");
    }

    public WifiStateTracker getWifiStateTracker() {
        return this.mWifiStateTracker;
    }

    public IWificond makeWificond() {
        return IWificond.Stub.asInterface(ServiceManager.getService(WIFICOND_SERVICE_NAME));
    }

    public SoftApManager makeSoftApManager(WifiManager.SoftApCallback softApCallback, SoftApModeConfiguration softApModeConfiguration) {
        return new MtkSoftApManager(this.mContext, this.mWifiStateMachineHandlerThread.getLooper(), this.mFrameworkFacade, this.mWifiNative, this.mCountryCode.getCountryCode(), softApCallback, this.mWifiApConfigStore, softApModeConfiguration, this.mWifiMetrics);
    }

    public ScanOnlyModeManager makeScanOnlyModeManager(ScanOnlyModeManager.Listener listener) {
        return new ScanOnlyModeManager(this.mContext, this.mWifiStateMachineHandlerThread.getLooper(), this.mWifiNative, listener, this.mWifiMetrics, this.mScanRequestProxy, this.mWakeupController);
    }

    public ClientModeManager makeClientModeManager(ClientModeManager.Listener listener) {
        return new ClientModeManager(this.mContext, this.mWifiStateMachineHandlerThread.getLooper(), this.mWifiNative, listener, this.mWifiMetrics, this.mScanRequestProxy, this.mWifiStateMachine);
    }

    public WifiLog makeLog(String str) {
        return new LogcatLog(str);
    }

    public BaseWifiDiagnostics getWifiDiagnostics() {
        return this.mWifiDiagnostics;
    }

    public synchronized WifiScanner getWifiScanner() {
        if (this.mWifiScanner == null) {
            this.mWifiScanner = new WifiScanner(this.mContext, IWifiScanner.Stub.asInterface(ServiceManager.getService("wifiscanner")), this.mWifiStateMachineHandlerThread.getLooper());
        }
        return this.mWifiScanner;
    }

    public WifiConnectivityManager makeWifiConnectivityManager(WifiInfo wifiInfo, boolean z) {
        return new WifiConnectivityManager(this.mContext, getScoringParams(), this.mWifiStateMachine, getWifiScanner(), this.mWifiConfigManager, wifiInfo, this.mWifiNetworkSelector, this.mWifiConnectivityHelper, this.mWifiLastResortWatchdog, this.mOpenNetworkNotifier, this.mCarrierNetworkNotifier, this.mCarrierNetworkConfig, this.mWifiMetrics, this.mWifiStateMachineHandlerThread.getLooper(), this.mClock, this.mConnectivityLocalLog, z, this.mFrameworkFacade, this.mSavedNetworkEvaluator, this.mScoredNetworkEvaluator, this.mPasspointNetworkEvaluator);
    }

    public WifiPermissionsUtil getWifiPermissionsUtil() {
        return this.mWifiPermissionsUtil;
    }

    public WifiPermissionsWrapper getWifiPermissionsWrapper() {
        return this.mWifiPermissionsWrapper;
    }

    public HandlerThread getWifiAwareHandlerThread() {
        if (this.mWifiAwareHandlerThread == null) {
            this.mWifiAwareHandlerThread = new HandlerThread("wifiAwareService");
            this.mWifiAwareHandlerThread.start();
        }
        return this.mWifiAwareHandlerThread;
    }

    public HandlerThread getRttHandlerThread() {
        if (this.mRttHandlerThread == null) {
            this.mRttHandlerThread = new HandlerThread("wifiRttService");
            this.mRttHandlerThread.start();
        }
        return this.mRttHandlerThread;
    }

    public HalDeviceManager getHalDeviceManager() {
        return this.mHalDeviceManager;
    }

    public WifiNative getWifiNative() {
        return this.mWifiNative;
    }

    public WifiMonitor getWifiMonitor() {
        return this.mWifiMonitor;
    }

    public WifiP2pNative getWifiP2pNative() {
        return this.mWifiP2pNative;
    }

    public WifiP2pMonitor getWifiP2pMonitor() {
        return this.mWifiP2pMonitor;
    }

    public SelfRecovery getSelfRecovery() {
        return this.mSelfRecovery;
    }

    public PowerProfile getPowerProfile() {
        return new PowerProfile(this.mContext, false);
    }

    public ScanRequestProxy getScanRequestProxy() {
        return this.mScanRequestProxy;
    }

    public Runtime getJavaRuntime() {
        return Runtime.getRuntime();
    }

    public ActivityManagerService getActivityManagerService() {
        return ActivityManager.getService();
    }
}
