package com.android.server;

import android.R;
import android.app.ActivityThread;
import android.app.INotificationManager;
import android.app.usage.UsageStatsManagerInternal;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.sqlite.SQLiteCompatibilityWalFlags;
import android.net.INetd;
import android.os.BaseBundle;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.FactoryTest;
import android.os.FileUtils;
import android.os.IBinder;
import android.os.IIncidentManager;
import android.os.IVibratorService;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Process;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.storage.IStorageManager;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.Slog;
import android.util.TimingsTraceLog;
import android.view.WindowManager;
import com.android.internal.app.ColorDisplayController;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.os.BinderInternal;
import com.android.internal.telephony.ITelephonyRegistry;
import com.android.internal.util.ConcurrentUtils;
import com.android.internal.widget.ILockSettings;
import com.android.server.InputMethodManagerService;
import com.android.server.NetworkScoreService;
import com.android.server.TextServicesManagerService;
import com.android.server.accessibility.AccessibilityManagerService;
import com.android.server.am.ActivityManagerService;
import com.android.server.audio.AudioService;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.broadcastradio.BroadcastRadioService;
import com.android.server.camera.CameraServiceProxy;
import com.android.server.clipboard.ClipboardService;
import com.android.server.connectivity.IpConnectivityMetrics;
import com.android.server.coverage.CoverageService;
import com.android.server.devicepolicy.DevicePolicyManagerService;
import com.android.server.display.ColorDisplayService;
import com.android.server.display.DisplayManagerService;
import com.android.server.dreams.DreamManagerService;
import com.android.server.emergency.EmergencyAffordanceService;
import com.android.server.fingerprint.FingerprintService;
import com.android.server.hdmi.HdmiControlService;
import com.android.server.input.InputManagerService;
import com.android.server.job.JobSchedulerService;
import com.android.server.lights.LightsService;
import com.android.server.media.MediaResourceMonitorService;
import com.android.server.media.MediaRouterService;
import com.android.server.media.MediaSessionService;
import com.android.server.media.MediaUpdateService;
import com.android.server.media.projection.MediaProjectionManagerService;
import com.android.server.net.NetworkPolicyManagerService;
import com.android.server.net.NetworkStatsService;
import com.android.server.net.watchlist.NetworkWatchlistService;
import com.android.server.notification.NotificationManagerService;
import com.android.server.oemlock.OemLockService;
import com.android.server.om.OverlayManagerService;
import com.android.server.os.DeviceIdentifiersPolicyService;
import com.android.server.os.SchedulingPolicyService;
import com.android.server.pm.BackgroundDexOptService;
import com.android.server.pm.CrossProfileAppsService;
import com.android.server.pm.Installer;
import com.android.server.pm.LauncherAppsService;
import com.android.server.pm.OtaDexoptService;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.ShortcutService;
import com.android.server.pm.UserManagerService;
import com.android.server.policy.PhoneWindowManager;
import com.android.server.power.PowerManagerService;
import com.android.server.power.ShutdownThread;
import com.android.server.restrictions.RestrictionsManagerService;
import com.android.server.security.KeyAttestationApplicationIdProviderService;
import com.android.server.security.KeyChainSystemService;
import com.android.server.soundtrigger.SoundTriggerService;
import com.android.server.stats.StatsCompanionService;
import com.android.server.statusbar.StatusBarManagerService;
import com.android.server.storage.DeviceStorageMonitorService;
import com.android.server.telecom.TelecomLoaderService;
import com.android.server.textclassifier.TextClassificationManagerService;
import com.android.server.trust.TrustManagerService;
import com.android.server.tv.TvInputManagerService;
import com.android.server.tv.TvRemoteService;
import com.android.server.twilight.TwilightService;
import com.android.server.usage.UsageStatsService;
import com.android.server.vr.VrManagerService;
import com.android.server.webkit.WebViewUpdateService;
import com.android.server.wm.WindowManagerService;
import com.mediatek.server.MtkSystemServer;
import dalvik.system.VMRuntime;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Timer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

public final class SystemServer {
    private static final String ACCOUNT_SERVICE_CLASS = "com.android.server.accounts.AccountManagerService$Lifecycle";
    private static final String APPWIDGET_SERVICE_CLASS = "com.android.server.appwidget.AppWidgetService";
    private static final String AUTO_FILL_MANAGER_SERVICE_CLASS = "com.android.server.autofill.AutofillManagerService";
    private static final String BACKUP_MANAGER_SERVICE_CLASS = "com.android.server.backup.BackupManagerService$Lifecycle";
    private static final String BLOCK_MAP_FILE = "/cache/recovery/block.map";
    private static final String CAR_SERVICE_HELPER_SERVICE_CLASS = "com.android.internal.car.CarServiceHelperService";
    private static final String COMPANION_DEVICE_MANAGER_SERVICE_CLASS = "com.android.server.companion.CompanionDeviceManagerService";
    private static final String CONTENT_SERVICE_CLASS = "com.android.server.content.ContentService$Lifecycle";
    private static final int DEFAULT_SYSTEM_THEME = 16974803;
    private static final long EARLIEST_SUPPORTED_TIME = 86400000;
    private static final String ENCRYPTED_STATE = "1";
    private static final String ENCRYPTING_STATE = "trigger_restart_min_framework";
    private static final String ETHERNET_SERVICE_CLASS = "com.android.server.ethernet.EthernetService";
    private static final String IOT_SERVICE_CLASS = "com.google.android.things.services.IoTSystemService";
    private static final String JOB_SCHEDULER_SERVICE_CLASS = "com.android.server.job.JobSchedulerService";
    private static final String LOCK_SETTINGS_SERVICE_CLASS = "com.android.server.locksettings.LockSettingsService$Lifecycle";
    private static final String LOWPAN_SERVICE_CLASS = "com.android.server.lowpan.LowpanService";
    private static final String MIDI_SERVICE_CLASS = "com.android.server.midi.MidiService$Lifecycle";
    private static final String PERSISTENT_DATA_BLOCK_PROP = "ro.frp.pst";
    private static final String PERSISTENT_OEM_VENDOR_LOCK = "ro.service.oem.vendorlock";
    private static final String PRINT_MANAGER_SERVICE_CLASS = "com.android.server.print.PrintManagerService";
    private static final String SEARCH_MANAGER_SERVICE_CLASS = "com.android.server.search.SearchManagerService$Lifecycle";
    private static final String SLICE_MANAGER_SERVICE_CLASS = "com.android.server.slice.SliceManagerService$Lifecycle";
    private static final long SLOW_DELIVERY_THRESHOLD_MS = 200;
    private static final long SLOW_DISPATCH_THRESHOLD_MS = 100;
    private static final long SNAPSHOT_INTERVAL = 3600000;
    private static final String START_HIDL_SERVICES = "StartHidlServices";
    private static final String START_SENSOR_SERVICE = "StartSensorService";
    private static final String STORAGE_MANAGER_SERVICE_CLASS = "com.android.server.StorageManagerService$Lifecycle";
    private static final String STORAGE_STATS_SERVICE_CLASS = "com.android.server.usage.StorageStatsService$Lifecycle";
    private static final String SYSTEM_SERVER_TIMING_ASYNC_TAG = "SystemServerTimingAsync";
    private static final String TAG = "SystemServer";
    private static final String THERMAL_OBSERVER_CLASS = "com.google.android.clockwork.ThermalObserver";
    private static final String TIME_ZONE_RULES_MANAGER_SERVICE_CLASS = "com.android.server.timezone.RulesManagerService$Lifecycle";
    private static final String UNCRYPT_PACKAGE_FILE = "/cache/recovery/uncrypt_file";
    private static final String USB_SERVICE_CLASS = "com.android.server.usb.UsbService$Lifecycle";
    private static final String VOICE_RECOGNITION_MANAGER_SERVICE_CLASS = "com.android.server.voiceinteraction.VoiceInteractionManagerService";
    private static final String WALLPAPER_SERVICE_CLASS = "com.android.server.wallpaper.WallpaperManagerService$Lifecycle";
    private static final String WEAR_CONFIG_SERVICE_CLASS = "com.google.android.clockwork.WearConfigManagerService";
    private static final String WEAR_CONNECTIVITY_SERVICE_CLASS = "com.android.clockwork.connectivity.WearConnectivityService";
    private static final String WEAR_DISPLAY_SERVICE_CLASS = "com.google.android.clockwork.display.WearDisplayService";
    private static final String WEAR_GLOBAL_ACTIONS_SERVICE_CLASS = "com.android.clockwork.globalactions.GlobalActionsService";
    private static final String WEAR_LEFTY_SERVICE_CLASS = "com.google.android.clockwork.lefty.WearLeftyService";
    private static final String WEAR_SIDEKICK_SERVICE_CLASS = "com.google.android.clockwork.sidekick.SidekickService";
    private static final String WEAR_TIME_SERVICE_CLASS = "com.google.android.clockwork.time.WearTimeService";
    private static final String WIFI_AWARE_SERVICE_CLASS = "com.android.server.wifi.aware.WifiAwareService";
    private static final String WIFI_P2P_SERVICE_CLASS = "com.android.server.wifi.p2p.WifiP2pService";
    private static final String WIFI_SERVICE_CLASS = "com.android.server.wifi.WifiService";
    private static final int sMaxBinderThreads = 31;
    private ActivityManagerService mActivityManagerService;
    private ContentResolver mContentResolver;
    private DisplayManagerService mDisplayManagerService;
    private EntropyMixer mEntropyMixer;
    private boolean mFirstBoot;
    private boolean mOnlyCore;
    private PackageManager mPackageManager;
    private PackageManagerService mPackageManagerService;
    private PowerManagerService mPowerManagerService;
    private Timer mProfilerSnapshotTimer;
    private Future<?> mSensorServiceStart;
    private Context mSystemContext;
    private SystemServiceManager mSystemServiceManager;
    private WebViewUpdateService mWebViewUpdateService;
    private Future<?> mZygotePreload;
    private static final String SYSTEM_SERVER_TIMING_TAG = "SystemServerTiming";
    private static final TimingsTraceLog BOOT_TIMINGS_TRACE_LOG = new TimingsTraceLog(SYSTEM_SERVER_TIMING_TAG, 524288);
    private static MtkSystemServer sMtkSystemServerIns = MtkSystemServer.getInstance();
    private final int mFactoryTestMode = FactoryTest.getMode();
    private final boolean mRuntimeRestart = ENCRYPTED_STATE.equals(SystemProperties.get("sys.boot_completed"));
    private final long mRuntimeStartElapsedTime = SystemClock.elapsedRealtime();
    private final long mRuntimeStartUptime = SystemClock.uptimeMillis();

    private static native void startHidlServices();

    private static native void startSensorService();

    public static void main(String[] strArr) {
        new SystemServer().run();
    }

    private void run() {
        try {
            traceBeginAndSlog("InitBeforeStartServices");
            if (System.currentTimeMillis() < 86400000) {
                Slog.w(TAG, "System clock is before 1970; setting to 1970.");
                SystemClock.setCurrentTimeMillis(86400000L);
            }
            String str = SystemProperties.get("persist.sys.timezone");
            if (str == null || str.isEmpty()) {
                Slog.w(TAG, "Timezone not set; setting to Asia/Tokyo.");
                SystemProperties.set("persist.sys.timezone", "Asia/Tokyo");
            }
            if (!SystemProperties.get("persist.sys.language").isEmpty()) {
                SystemProperties.set("persist.sys.locale", Locale.getDefault().toLanguageTag());
                SystemProperties.set("persist.sys.language", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                SystemProperties.set("persist.sys.country", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                SystemProperties.set("persist.sys.localevar", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            }
            Binder.setWarnOnBlocking(true);
            PackageItemInfo.setForceSafeLabels(true);
            SQLiteCompatibilityWalFlags.init((String) null);
            Slog.i(TAG, "Entered the Android system server!");
            int iElapsedRealtime = (int) SystemClock.elapsedRealtime();
            EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_SYSTEM_RUN, iElapsedRealtime);
            if (!this.mRuntimeRestart) {
                MetricsLogger.histogram((Context) null, "boot_system_server_init", iElapsedRealtime);
            }
            sMtkSystemServerIns.addBootEvent("Android:SysServerInit_START");
            SystemProperties.set("persist.sys.dalvik.vm.lib.2", VMRuntime.getRuntime().vmLibrary());
            VMRuntime.getRuntime().clearGrowthLimit();
            VMRuntime.getRuntime().setTargetHeapUtilization(0.8f);
            Build.ensureFingerprintProperty();
            Environment.setUserRequired(true);
            BaseBundle.setShouldDefuse(true);
            Parcel.setStackTraceParceling(true);
            BinderInternal.disableBackgroundScheduling(true);
            BinderInternal.setMaxThreads(31);
            Process.setThreadPriority(-2);
            Process.setCanSelfBackground(false);
            Looper.prepareMainLooper();
            Looper.getMainLooper().setSlowLogThresholdMs(SLOW_DISPATCH_THRESHOLD_MS, SLOW_DELIVERY_THRESHOLD_MS);
            System.loadLibrary("android_servers");
            performPendingShutdown();
            createSystemContext();
            this.mSystemServiceManager = new SystemServiceManager(this.mSystemContext);
            this.mSystemServiceManager.setStartInfo(this.mRuntimeRestart, this.mRuntimeStartElapsedTime, this.mRuntimeStartUptime);
            LocalServices.addService(SystemServiceManager.class, this.mSystemServiceManager);
            SystemServerInitThreadPool.get();
            traceEnd();
            sMtkSystemServerIns.setPrameters(BOOT_TIMINGS_TRACE_LOG, this.mSystemServiceManager, this.mSystemContext);
            try {
                try {
                    traceBeginAndSlog("StartServices");
                    startBootstrapServices();
                    sMtkSystemServerIns.startMtkBootstrapServices();
                    startCoreServices();
                    sMtkSystemServerIns.startMtkCoreServices();
                    startOtherServices();
                    SystemServerInitThreadPool.shutdown();
                    traceEnd();
                    StrictMode.initVmDefaults(null);
                    if ("user".equals(Build.TYPE) && !this.mRuntimeRestart && !isFirstBootOrUpgrade()) {
                        int iElapsedRealtime2 = (int) SystemClock.elapsedRealtime();
                        MetricsLogger.histogram((Context) null, "boot_system_server_ready", iElapsedRealtime2);
                        if (iElapsedRealtime2 > 60000) {
                            Slog.wtf(SYSTEM_SERVER_TIMING_TAG, "SystemServer init took too long. uptimeMillis=" + iElapsedRealtime2);
                        }
                    }
                    sMtkSystemServerIns.addBootEvent("Android:SysServerInit_END");
                    Looper.loop();
                    throw new RuntimeException("Main thread loop unexpectedly exited");
                } finally {
                }
            } catch (Throwable th) {
                Slog.e("System", "******************************************");
                Slog.e("System", "************ Failure starting system services", th);
                throw th;
            }
        } finally {
        }
    }

    private boolean isFirstBootOrUpgrade() {
        return this.mPackageManagerService.isFirstBoot() || this.mPackageManagerService.isUpgrade();
    }

    private void reportWtf(String str, Throwable th) {
        Slog.w(TAG, "***********************************************");
        Slog.wtf(TAG, "BOOT FAILURE " + str, th);
    }

    private void performPendingShutdown() {
        final String strSubstring;
        String textFile;
        String str = SystemProperties.get(ShutdownThread.SHUTDOWN_ACTION_PROPERTY, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        if (str != null && str.length() > 0) {
            final boolean z = str.charAt(0) == '1';
            if (str.length() > 1) {
                strSubstring = str.substring(1, str.length());
            } else {
                strSubstring = null;
            }
            if (strSubstring != null && strSubstring.startsWith("recovery-update")) {
                File file = new File(UNCRYPT_PACKAGE_FILE);
                if (file.exists()) {
                    try {
                        textFile = FileUtils.readTextFile(file, 0, null);
                    } catch (IOException e) {
                        Slog.e(TAG, "Error reading uncrypt package file", e);
                        textFile = null;
                    }
                    if (textFile != null && textFile.startsWith("/data") && !new File(BLOCK_MAP_FILE).exists()) {
                        Slog.e(TAG, "Can't find block map file, uncrypt failed or unexpected runtime restart?");
                        return;
                    }
                }
            }
            Message messageObtain = Message.obtain(UiThread.getHandler(), new Runnable() {
                @Override
                public void run() {
                    synchronized (this) {
                        ShutdownThread.rebootOrShutdown(null, z, strSubstring);
                    }
                }
            });
            messageObtain.setAsynchronous(true);
            UiThread.getHandler().sendMessage(messageObtain);
        }
    }

    private void createSystemContext() {
        ActivityThread activityThreadSystemMain = ActivityThread.systemMain();
        this.mSystemContext = activityThreadSystemMain.getSystemContext();
        this.mSystemContext.setTheme(16974803);
        activityThreadSystemMain.getSystemUiContext().setTheme(16974803);
    }

    private void startBootstrapServices() {
        Slog.i(TAG, "Reading configuration...");
        traceBeginAndSlog("ReadingSystemConfig");
        SystemServerInitThreadPool.get().submit(new Runnable() {
            @Override
            public final void run() {
                SystemConfig.getInstance();
            }
        }, "ReadingSystemConfig");
        traceEnd();
        traceBeginAndSlog("StartInstaller");
        Installer installer = (Installer) this.mSystemServiceManager.startService(Installer.class);
        traceEnd();
        traceBeginAndSlog("DeviceIdentifiersPolicyService");
        this.mSystemServiceManager.startService(DeviceIdentifiersPolicyService.class);
        traceEnd();
        traceBeginAndSlog("StartActivityManager");
        this.mActivityManagerService = ((ActivityManagerService.Lifecycle) this.mSystemServiceManager.startService(ActivityManagerService.Lifecycle.class)).getService();
        this.mActivityManagerService.setSystemServiceManager(this.mSystemServiceManager);
        this.mActivityManagerService.setInstaller(installer);
        traceEnd();
        traceBeginAndSlog("StartPowerManager");
        this.mPowerManagerService = (PowerManagerService) this.mSystemServiceManager.startService(PowerManagerService.class);
        traceEnd();
        traceBeginAndSlog("InitPowerManagement");
        this.mActivityManagerService.initPowerManagement();
        traceEnd();
        traceBeginAndSlog("StartRecoverySystemService");
        this.mSystemServiceManager.startService(RecoverySystemService.class);
        traceEnd();
        RescueParty.noteBoot(this.mSystemContext);
        traceBeginAndSlog("StartLightsService");
        this.mSystemServiceManager.startService(LightsService.class);
        traceEnd();
        traceBeginAndSlog("StartSidekickService");
        if (SystemProperties.getBoolean("config.enable_sidekick_graphics", false)) {
            this.mSystemServiceManager.startService(WEAR_SIDEKICK_SERVICE_CLASS);
        }
        traceEnd();
        traceBeginAndSlog("StartDisplayManager");
        this.mDisplayManagerService = (DisplayManagerService) this.mSystemServiceManager.startService(DisplayManagerService.class);
        traceEnd();
        traceBeginAndSlog("WaitForDisplay");
        this.mSystemServiceManager.startBootPhase(100);
        traceEnd();
        String str = SystemProperties.get("vold.decrypt");
        if (ENCRYPTING_STATE.equals(str)) {
            Slog.w(TAG, "Detected encryption in progress - only parsing core apps");
            this.mOnlyCore = true;
        } else if (ENCRYPTED_STATE.equals(str)) {
            Slog.w(TAG, "Device encrypted - only parsing core apps");
            this.mOnlyCore = true;
        }
        if (!this.mRuntimeRestart) {
            MetricsLogger.histogram((Context) null, "boot_package_manager_init_start", (int) SystemClock.elapsedRealtime());
        }
        traceBeginAndSlog("StartPackageManagerService");
        this.mPackageManagerService = PackageManagerService.main(this.mSystemContext, installer, this.mFactoryTestMode != 0, this.mOnlyCore);
        this.mFirstBoot = this.mPackageManagerService.isFirstBoot();
        this.mPackageManager = this.mSystemContext.getPackageManager();
        traceEnd();
        if (!this.mRuntimeRestart && !isFirstBootOrUpgrade()) {
            MetricsLogger.histogram((Context) null, "boot_package_manager_init_ready", (int) SystemClock.elapsedRealtime());
        }
        if (!this.mOnlyCore && !SystemProperties.getBoolean("config.disable_otadexopt", false)) {
            traceBeginAndSlog("StartOtaDexOptService");
            try {
                try {
                    OtaDexoptService.main(this.mSystemContext, this.mPackageManagerService);
                } catch (Throwable th) {
                    reportWtf("starting OtaDexOptService", th);
                }
                traceEnd();
            } catch (Throwable th2) {
                traceEnd();
                throw th2;
            }
        }
        traceBeginAndSlog("StartUserManagerService");
        this.mSystemServiceManager.startService(UserManagerService.LifeCycle.class);
        traceEnd();
        traceBeginAndSlog("InitAttributerCache");
        AttributeCache.init(this.mSystemContext);
        traceEnd();
        traceBeginAndSlog("SetSystemProcess");
        this.mActivityManagerService.setSystemProcess();
        traceEnd();
        this.mDisplayManagerService.setupSchedulerPolicies();
        this.mPackageManagerService.onAmsAddedtoServiceMgr();
        traceBeginAndSlog("StartOverlayManagerService");
        this.mSystemServiceManager.startService(new OverlayManagerService(this.mSystemContext, installer));
        traceEnd();
        this.mSensorServiceStart = SystemServerInitThreadPool.get().submit(new Runnable() {
            @Override
            public final void run() {
                SystemServer.lambda$startBootstrapServices$0();
            }
        }, START_SENSOR_SERVICE);
    }

    static void lambda$startBootstrapServices$0() {
        TimingsTraceLog timingsTraceLog = new TimingsTraceLog(SYSTEM_SERVER_TIMING_ASYNC_TAG, 524288L);
        timingsTraceLog.traceBegin(START_SENSOR_SERVICE);
        startSensorService();
        timingsTraceLog.traceEnd();
    }

    private void startCoreServices() {
        traceBeginAndSlog("StartBatteryService");
        this.mSystemServiceManager.startService(BatteryService.class);
        traceEnd();
        traceBeginAndSlog("StartUsageService");
        this.mSystemServiceManager.startService(UsageStatsService.class);
        this.mActivityManagerService.setUsageStatsManager((UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class));
        traceEnd();
        if (this.mPackageManager.hasSystemFeature("android.software.webview")) {
            traceBeginAndSlog("StartWebViewUpdateService");
            this.mWebViewUpdateService = (WebViewUpdateService) this.mSystemServiceManager.startService(WebViewUpdateService.class);
            traceEnd();
        }
        traceBeginAndSlog("StartBinderCallsStatsService");
        BinderCallsStatsService.start();
        traceEnd();
    }

    private void startOtherServices() throws Throwable {
        ILockSettings iLockSettings;
        int i;
        ?? r3;
        IVibratorService.Stub stub;
        ITelephonyRegistry.Stub stub2;
        ITelephonyRegistry.Stub stub3;
        ?? r8;
        ?? r7;
        ?? r5;
        ILockSettings iLockSettings2;
        final ILockSettings iLockSettings3;
        final ILockSettings iLockSettings4;
        ILockSettings commonTimeManagementService;
        ILockSettings iLockSettings5;
        ILockSettings iLockSettings6;
        ILockSettings iLockSettings7;
        ILockSettings iLockSettings8;
        ILockSettings iLockSettings9;
        ILockSettings iLockSettings10;
        boolean zDetectSafeMode;
        Resources.Theme theme;
        ILockSettings iLockSettingsAsInterface;
        boolean z;
        ILockSettings iLockSettingsCreate;
        ILockSettings iLockSettingsCreate2;
        ILockSettings iLockSettingsCreate3;
        ILockSettings networkPolicyManagerService;
        ILockSettings iLockSettings11;
        ILockSettings iLockSettings12;
        ILockSettings networkTimeUpdateService;
        ILockSettings mediaRouterService;
        ITelephonyRegistry.Stub stub4;
        InputManagerService inputManagerService;
        ?? r32;
        ?? Main;
        final Context context = this.mSystemContext;
        boolean z2 = SystemProperties.getBoolean("config.disable_systemtextclassifier", false);
        boolean z3 = SystemProperties.getBoolean("config.disable_cameraservice", false);
        boolean z4 = SystemProperties.getBoolean("config.disable_slices", false);
        boolean z5 = SystemProperties.getBoolean("config.enable_lefty", false);
        boolean zEquals = SystemProperties.get("ro.kernel.qemu").equals(ENCRYPTED_STATE);
        boolean zHasSystemFeature = context.getPackageManager().hasSystemFeature("android.hardware.type.watch");
        if (!Build.IS_DEBUGGABLE || !SystemProperties.getBoolean("debug.crash_system", false)) {
            ?? r72 = 0;
            r72 = 0;
            try {
                this.mZygotePreload = SystemServerInitThreadPool.get().submit(new Runnable() {
                    @Override
                    public final void run() {
                        SystemServer.lambda$startOtherServices$1();
                    }
                }, "SecondaryZygotePreload");
                traceBeginAndSlog("StartKeyAttestationApplicationIdProviderService");
                ServiceManager.addService("sec_key_att_app_id_provider", new KeyAttestationApplicationIdProviderService(context));
                traceEnd();
                traceBeginAndSlog("StartKeyChainSystemService");
                this.mSystemServiceManager.startService(KeyChainSystemService.class);
                traceEnd();
                traceBeginAndSlog("StartSchedulingPolicyService");
                ServiceManager.addService("scheduling_policy", new SchedulingPolicyService());
                traceEnd();
                traceBeginAndSlog("StartTelecomLoaderService");
                this.mSystemServiceManager.startService(TelecomLoaderService.class);
                traceEnd();
                traceBeginAndSlog("StartTelephonyRegistry");
                ITelephonyRegistry.Stub telephonyRegistry = new TelephonyRegistry(context);
                try {
                    ServiceManager.addService("telephony.registry", telephonyRegistry);
                    traceEnd();
                    traceBeginAndSlog("StartEntropyMixer");
                    this.mEntropyMixer = new EntropyMixer(context);
                    traceEnd();
                    this.mContentResolver = context.getContentResolver();
                    traceBeginAndSlog("StartAccountManagerService");
                    this.mSystemServiceManager.startService(ACCOUNT_SERVICE_CLASS);
                    traceEnd();
                    traceBeginAndSlog("StartContentService");
                    this.mSystemServiceManager.startService(CONTENT_SERVICE_CLASS);
                    traceEnd();
                    traceBeginAndSlog("InstallSystemProviders");
                    this.mActivityManagerService.installSystemProviders();
                    SQLiteCompatibilityWalFlags.reset();
                    traceEnd();
                    traceBeginAndSlog("StartDropBoxManager");
                    this.mSystemServiceManager.startService(DropBoxManagerService.class);
                    traceEnd();
                    traceBeginAndSlog("StartVibratorService");
                    IVibratorService.Stub vibratorService = new VibratorService(context);
                    try {
                        ServiceManager.addService("vibrator", vibratorService);
                        traceEnd();
                        if (!zHasSystemFeature) {
                            try {
                                traceBeginAndSlog("StartConsumerIrService");
                                ServiceManager.addService("consumer_ir", new ConsumerIrService(context));
                                traceEnd();
                            } catch (RuntimeException e) {
                                e = e;
                                stub4 = telephonyRegistry;
                                inputManagerService = null;
                                iLockSettings = null;
                                i = 1;
                                r3 = inputManagerService;
                                stub = vibratorService;
                                stub2 = stub4;
                                Slog.e("System", "******************************************");
                                Slog.e("System", "************ Failure starting core service", e);
                                r8 = r3;
                                r5 = stub;
                                r7 = r72;
                                stub3 = stub2;
                                if (this.mFactoryTestMode != i) {
                                }
                                traceBeginAndSlog("MakeDisplayReady");
                                r7.displayReady();
                                traceEnd();
                                if (this.mFactoryTestMode != i) {
                                }
                                traceBeginAndSlog("StartUiModeManager");
                                this.mSystemServiceManager.startService(UiModeManagerService.class);
                                traceEnd();
                                if (!this.mOnlyCore) {
                                }
                                traceBeginAndSlog("PerformFstrimIfNeeded");
                                this.mPackageManagerService.performFstrimIfNeeded();
                                traceEnd();
                                if (this.mFactoryTestMode != i) {
                                }
                                if (!zHasSystemFeature) {
                                }
                                if (zHasSystemFeature) {
                                }
                                if (!z4) {
                                }
                                if (!z3) {
                                }
                                if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                                }
                                traceBeginAndSlog("StartStatsCompanionService");
                                this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                                traceEnd();
                                zDetectSafeMode = r7.detectSafeMode();
                                if (!zDetectSafeMode) {
                                }
                                traceBeginAndSlog("StartMmsService");
                                final MmsServiceBroker mmsServiceBroker = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                                traceEnd();
                                if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                                }
                                sMtkSystemServerIns.startMtkOtherServices(this.mOnlyCore);
                                traceBeginAndSlog("MakeVibratorServiceReady");
                                r5.systemReady();
                                traceEnd();
                                traceBeginAndSlog("MakeLockSettingsServiceReady");
                                if (iLockSettings2 != null) {
                                }
                                traceEnd();
                                traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                                this.mSystemServiceManager.startBootPhase(SystemService.PHASE_LOCK_SETTINGS_READY);
                                traceEnd();
                                traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                                this.mSystemServiceManager.startBootPhase(SystemService.PHASE_SYSTEM_SERVICES_READY);
                                traceEnd();
                                traceBeginAndSlog("MakeWindowManagerServiceReady");
                                r7.systemReady();
                                traceEnd();
                                if (zDetectSafeMode) {
                                }
                                Configuration configurationComputeNewConfiguration = r7.computeNewConfiguration(0);
                                DisplayMetrics displayMetrics = new DisplayMetrics();
                                ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(displayMetrics);
                                context.getResources().updateConfiguration(configurationComputeNewConfiguration, displayMetrics);
                                theme = context.getTheme();
                                if (theme.getChangingConfigurations() != 0) {
                                }
                                traceBeginAndSlog("MakePowerManagerServiceReady");
                                this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                                traceEnd();
                                traceBeginAndSlog("MakePackageManagerServiceReady");
                                this.mPackageManagerService.systemReady();
                                traceEnd();
                                traceBeginAndSlog("MakeDisplayManagerServiceReady");
                                this.mDisplayManagerService.systemReady(zDetectSafeMode, this.mOnlyCore);
                                traceEnd();
                                this.mSystemServiceManager.setSafeMode(zDetectSafeMode);
                                traceBeginAndSlog("StartDeviceSpecificServices");
                                while (i < r4) {
                                }
                                traceEnd();
                                traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                                this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                                traceEnd();
                                final ?? r4 = r7;
                                final ILockSettings iLockSettings13 = iLockSettings;
                                final ILockSettings iLockSettings14 = iLockSettings6;
                                final ?? r16 = r8;
                                final ILockSettings iLockSettings15 = iLockSettings7;
                                final ILockSettings iLockSettings16 = iLockSettings10;
                                final ILockSettings iLockSettings17 = iLockSettings5;
                                final ILockSettings iLockSettings18 = iLockSettings8;
                                final ILockSettings iLockSettings19 = commonTimeManagementService;
                                final ?? r15 = stub3;
                                final ILockSettings iLockSettings20 = iLockSettings9;
                                this.mActivityManagerService.systemReady(new Runnable() {
                                    @Override
                                    public final void run() {
                                        SystemServer.lambda$startOtherServices$4(this.f$0, context, r4, iLockSettings13, iLockSettings3, iLockSettings14, iLockSettings15, iLockSettings16, iLockSettings4, iLockSettings17, iLockSettings18, iLockSettings19, r16, r15, iLockSettings20, mmsServiceBroker);
                                    }
                                }, BOOT_TIMINGS_TRACE_LOG);
                                return;
                            }
                        }
                        traceBeginAndSlog("StartAlarmManagerService");
                        if (!sMtkSystemServerIns.startMtkAlarmManagerService()) {
                            this.mSystemServiceManager.startService(AlarmManagerService.class);
                        }
                        traceEnd();
                        traceBeginAndSlog("InitWatchdog");
                        Watchdog.getInstance().init(context, this.mActivityManagerService);
                        traceEnd();
                        traceBeginAndSlog("StartInputManagerService");
                        InputManagerService inputManagerService2 = new InputManagerService(context);
                        try {
                            traceEnd();
                            traceBeginAndSlog("StartWindowManagerService");
                            ConcurrentUtils.waitForFutureNoInterrupt(this.mSensorServiceStart, START_SENSOR_SERVICE);
                            this.mSensorServiceStart = null;
                            try {
                                ITelephonyRegistry.Stub stub5 = telephonyRegistry;
                                iLockSettings = null;
                                try {
                                    Main = WindowManagerService.main(context, inputManagerService2, this.mFactoryTestMode != 1, !this.mFirstBoot, this.mOnlyCore, new PhoneWindowManager());
                                    try {
                                        ServiceManager.addService("window", (IBinder) Main, false, 17);
                                        r32 = inputManagerService2;
                                        i = 1;
                                        try {
                                            ServiceManager.addService("input", (IBinder) r32, false, 1);
                                            traceEnd();
                                            traceBeginAndSlog("SetWindowManagerService");
                                            this.mActivityManagerService.setWindowManager(Main);
                                            traceEnd();
                                            traceBeginAndSlog("WindowManagerServiceOnInitReady");
                                            Main.onInitReady();
                                            traceEnd();
                                            SystemServerInitThreadPool.get().submit(new Runnable() {
                                                @Override
                                                public final void run() {
                                                    SystemServer.lambda$startOtherServices$2();
                                                }
                                            }, START_HIDL_SERVICES);
                                            if (!zHasSystemFeature) {
                                                traceBeginAndSlog("StartVrManagerService");
                                                this.mSystemServiceManager.startService(VrManagerService.class);
                                                traceEnd();
                                            }
                                            traceBeginAndSlog("StartInputManager");
                                            r32.setWindowManagerCallbacks(Main.getInputMonitor());
                                            r32.start();
                                            traceEnd();
                                            traceBeginAndSlog("DisplayManagerWindowManagerAndInputReady");
                                            this.mDisplayManagerService.windowManagerAndInputReady();
                                            traceEnd();
                                            if (zEquals) {
                                                Slog.i(TAG, "No Bluetooth Service (emulator)");
                                            } else if (this.mFactoryTestMode == 1) {
                                                Slog.i(TAG, "No Bluetooth Service (factory test)");
                                            } else if (!context.getPackageManager().hasSystemFeature("android.hardware.bluetooth")) {
                                                Slog.i(TAG, "No Bluetooth Service (Bluetooth Hardware Not Present)");
                                            } else {
                                                traceBeginAndSlog("StartBluetoothService");
                                                this.mSystemServiceManager.startService(BluetoothService.class);
                                                traceEnd();
                                            }
                                            traceBeginAndSlog("IpConnectivityMetrics");
                                            this.mSystemServiceManager.startService(IpConnectivityMetrics.class);
                                            traceEnd();
                                            traceBeginAndSlog("NetworkWatchlistService");
                                            this.mSystemServiceManager.startService(NetworkWatchlistService.Lifecycle.class);
                                            traceEnd();
                                            traceBeginAndSlog("PinnerService");
                                            this.mSystemServiceManager.startService(PinnerService.class);
                                            traceEnd();
                                            r8 = r32;
                                            r5 = vibratorService;
                                            r7 = Main;
                                            stub3 = stub5;
                                        } catch (RuntimeException e2) {
                                            e = e2;
                                            stub = vibratorService;
                                            r3 = r32;
                                            r72 = Main;
                                            stub2 = stub5;
                                            Slog.e("System", "******************************************");
                                            Slog.e("System", "************ Failure starting core service", e);
                                            r8 = r3;
                                            r5 = stub;
                                            r7 = r72;
                                            stub3 = stub2;
                                        }
                                    } catch (RuntimeException e3) {
                                        e = e3;
                                        r32 = inputManagerService2;
                                        i = 1;
                                    }
                                } catch (RuntimeException e4) {
                                    e = e4;
                                    r32 = inputManagerService2;
                                    i = 1;
                                    Main = 0;
                                }
                            } catch (RuntimeException e5) {
                                e = e5;
                                r3 = inputManagerService2;
                                stub2 = telephonyRegistry;
                                iLockSettings = null;
                                i = 1;
                                stub = vibratorService;
                            }
                        } catch (RuntimeException e6) {
                            e = e6;
                            inputManagerService = inputManagerService2;
                            stub4 = telephonyRegistry;
                            iLockSettings = null;
                            i = 1;
                            r3 = inputManagerService;
                            stub = vibratorService;
                            stub2 = stub4;
                            Slog.e("System", "******************************************");
                            Slog.e("System", "************ Failure starting core service", e);
                            r8 = r3;
                            r5 = stub;
                            r7 = r72;
                            stub3 = stub2;
                            if (this.mFactoryTestMode != i) {
                            }
                            traceBeginAndSlog("MakeDisplayReady");
                            r7.displayReady();
                            traceEnd();
                            if (this.mFactoryTestMode != i) {
                                traceBeginAndSlog("StartStorageManagerService");
                                try {
                                    if (!sMtkSystemServerIns.startMtkStorageManagerService()) {
                                    }
                                    IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));
                                } catch (Throwable th) {
                                    reportWtf("starting StorageManagerService", th);
                                }
                                traceEnd();
                                traceBeginAndSlog("StartStorageStatsService");
                                try {
                                    this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
                                } catch (Throwable th2) {
                                    reportWtf("starting StorageStatsService", th2);
                                }
                                traceEnd();
                            }
                            traceBeginAndSlog("StartUiModeManager");
                            this.mSystemServiceManager.startService(UiModeManagerService.class);
                            traceEnd();
                            if (!this.mOnlyCore) {
                            }
                            traceBeginAndSlog("PerformFstrimIfNeeded");
                            this.mPackageManagerService.performFstrimIfNeeded();
                            traceEnd();
                            if (this.mFactoryTestMode != i) {
                            }
                            if (!zHasSystemFeature) {
                            }
                            if (zHasSystemFeature) {
                            }
                            if (!z4) {
                            }
                            if (!z3) {
                            }
                            if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                            }
                            traceBeginAndSlog("StartStatsCompanionService");
                            this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                            traceEnd();
                            zDetectSafeMode = r7.detectSafeMode();
                            if (!zDetectSafeMode) {
                            }
                            traceBeginAndSlog("StartMmsService");
                            final MmsServiceBroker mmsServiceBroker2 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                            }
                            sMtkSystemServerIns.startMtkOtherServices(this.mOnlyCore);
                            traceBeginAndSlog("MakeVibratorServiceReady");
                            r5.systemReady();
                            traceEnd();
                            traceBeginAndSlog("MakeLockSettingsServiceReady");
                            if (iLockSettings2 != null) {
                            }
                            traceEnd();
                            traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                            this.mSystemServiceManager.startBootPhase(SystemService.PHASE_LOCK_SETTINGS_READY);
                            traceEnd();
                            traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                            this.mSystemServiceManager.startBootPhase(SystemService.PHASE_SYSTEM_SERVICES_READY);
                            traceEnd();
                            traceBeginAndSlog("MakeWindowManagerServiceReady");
                            r7.systemReady();
                            traceEnd();
                            if (zDetectSafeMode) {
                            }
                            Configuration configurationComputeNewConfiguration2 = r7.computeNewConfiguration(0);
                            DisplayMetrics displayMetrics2 = new DisplayMetrics();
                            ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(displayMetrics2);
                            context.getResources().updateConfiguration(configurationComputeNewConfiguration2, displayMetrics2);
                            theme = context.getTheme();
                            if (theme.getChangingConfigurations() != 0) {
                            }
                            traceBeginAndSlog("MakePowerManagerServiceReady");
                            this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                            traceEnd();
                            traceBeginAndSlog("MakePackageManagerServiceReady");
                            this.mPackageManagerService.systemReady();
                            traceEnd();
                            traceBeginAndSlog("MakeDisplayManagerServiceReady");
                            this.mDisplayManagerService.systemReady(zDetectSafeMode, this.mOnlyCore);
                            traceEnd();
                            this.mSystemServiceManager.setSafeMode(zDetectSafeMode);
                            traceBeginAndSlog("StartDeviceSpecificServices");
                            while (i < r4) {
                            }
                            traceEnd();
                            traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                            this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                            traceEnd();
                            final WindowManagerService r42 = r7;
                            final NetworkManagementService iLockSettings132 = iLockSettings;
                            final IpSecService iLockSettings142 = iLockSettings6;
                            final InputManagerService r162 = r8;
                            final NetworkStatsService iLockSettings152 = iLockSettings7;
                            final ConnectivityService iLockSettings162 = iLockSettings10;
                            final CountryDetectorService iLockSettings172 = iLockSettings5;
                            final NetworkTimeUpdateService iLockSettings182 = iLockSettings8;
                            final CommonTimeManagementService iLockSettings192 = commonTimeManagementService;
                            final TelephonyRegistry r152 = stub3;
                            final MediaRouterService iLockSettings202 = iLockSettings9;
                            this.mActivityManagerService.systemReady(new Runnable() {
                                @Override
                                public final void run() {
                                    SystemServer.lambda$startOtherServices$4(this.f$0, context, r42, iLockSettings132, iLockSettings3, iLockSettings142, iLockSettings152, iLockSettings162, iLockSettings4, iLockSettings172, iLockSettings182, iLockSettings192, r162, r152, iLockSettings202, mmsServiceBroker2);
                                }
                            }, BOOT_TIMINGS_TRACE_LOG);
                            return;
                        }
                    } catch (RuntimeException e7) {
                        e = e7;
                        stub2 = telephonyRegistry;
                        iLockSettings = null;
                        i = 1;
                        r3 = 0;
                        r72 = 0;
                        stub = vibratorService;
                    }
                } catch (RuntimeException e8) {
                    e = e8;
                    stub2 = telephonyRegistry;
                    iLockSettings = null;
                    i = 1;
                    r3 = 0;
                    stub = null;
                    r72 = 0;
                }
            } catch (RuntimeException e9) {
                e = e9;
                iLockSettings = null;
                i = 1;
                r3 = 0;
                stub = null;
                r72 = 0;
                stub2 = null;
            }
            if (this.mFactoryTestMode != i) {
                traceBeginAndSlog("StartInputMethodManagerLifecycle");
                this.mSystemServiceManager.startService(InputMethodManagerService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("StartAccessibilityManagerService");
                try {
                    ServiceManager.addService("accessibility", new AccessibilityManagerService(context));
                } catch (Throwable th3) {
                    reportWtf("starting Accessibility Manager", th3);
                }
                traceEnd();
            }
            traceBeginAndSlog("MakeDisplayReady");
            try {
                r7.displayReady();
            } catch (Throwable th4) {
                reportWtf("making display ready", th4);
            }
            traceEnd();
            if (this.mFactoryTestMode != i && !"0".equals(SystemProperties.get("system_init.startmountservice"))) {
                traceBeginAndSlog("StartStorageManagerService");
                if (!sMtkSystemServerIns.startMtkStorageManagerService()) {
                    this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
                }
                IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));
                traceEnd();
                traceBeginAndSlog("StartStorageStatsService");
                this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
                traceEnd();
            }
            traceBeginAndSlog("StartUiModeManager");
            this.mSystemServiceManager.startService(UiModeManagerService.class);
            traceEnd();
            if (!this.mOnlyCore) {
                traceBeginAndSlog("UpdatePackagesIfNeeded");
                try {
                    this.mPackageManagerService.updatePackagesIfNeeded();
                } catch (Throwable th5) {
                    reportWtf("update packages", th5);
                }
                traceEnd();
            }
            traceBeginAndSlog("PerformFstrimIfNeeded");
            try {
                this.mPackageManagerService.performFstrimIfNeeded();
            } catch (Throwable th6) {
                reportWtf("performing fstrim", th6);
            }
            traceEnd();
            if (this.mFactoryTestMode != i) {
                traceBeginAndSlog("StartLockSettingsService");
                try {
                    this.mSystemServiceManager.startService(LOCK_SETTINGS_SERVICE_CLASS);
                    iLockSettingsAsInterface = ILockSettings.Stub.asInterface(ServiceManager.getService("lock_settings"));
                } catch (Throwable th7) {
                    reportWtf("starting LockSettingsService service", th7);
                    iLockSettingsAsInterface = iLockSettings;
                }
                traceEnd();
                int i2 = (SystemProperties.get(PERSISTENT_DATA_BLOCK_PROP).equals(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS) ? 1 : 0) ^ i;
                if (i2 != 0) {
                    traceBeginAndSlog("StartPersistentDataBlock");
                    this.mSystemServiceManager.startService(PersistentDataBlockService.class);
                    traceEnd();
                }
                if (OemLockService.isHalPresent()) {
                    if (!SystemProperties.get(PERSISTENT_OEM_VENDOR_LOCK).equals(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS)) {
                        SystemProperties.set(PERSISTENT_OEM_VENDOR_LOCK, ENCRYPTED_STATE);
                    }
                    z = true;
                } else {
                    z = false;
                }
                if (i2 != 0 || z) {
                    traceBeginAndSlog("StartOemLockService");
                    this.mSystemServiceManager.startService(OemLockService.class);
                    traceEnd();
                }
                traceBeginAndSlog("StartDeviceIdleController");
                this.mSystemServiceManager.startService(DeviceIdleController.class);
                traceEnd();
                traceBeginAndSlog("StartDevicePolicyManager");
                this.mSystemServiceManager.startService(DevicePolicyManagerService.Lifecycle.class);
                traceEnd();
                if (!zHasSystemFeature) {
                    traceBeginAndSlog("StartStatusBarManagerService");
                    try {
                        ServiceManager.addService("statusbar", new StatusBarManagerService(context, r7));
                    } catch (Throwable th8) {
                        reportWtf("starting StatusBarManagerService", th8);
                    }
                    traceEnd();
                }
                traceBeginAndSlog("StartClipboardService");
                this.mSystemServiceManager.startService(ClipboardService.class);
                traceEnd();
                traceBeginAndSlog("StartNetworkManagementService");
                try {
                    iLockSettingsCreate = NetworkManagementService.create(context);
                    try {
                        ServiceManager.addService("network_management", iLockSettingsCreate);
                    } catch (Throwable th9) {
                        th = th9;
                        reportWtf("starting NetworkManagement Service", th);
                    }
                } catch (Throwable th10) {
                    th = th10;
                    iLockSettingsCreate = iLockSettings;
                }
                traceEnd();
                traceBeginAndSlog("StartIpSecService");
                try {
                    iLockSettingsCreate2 = IpSecService.create(context);
                } catch (Throwable th11) {
                    th = th11;
                    iLockSettingsCreate2 = iLockSettings;
                }
                try {
                    ServiceManager.addService(INetd.IPSEC_INTERFACE_PREFIX, iLockSettingsCreate2);
                } catch (Throwable th12) {
                    th = th12;
                    reportWtf("starting IpSec Service", th);
                }
                traceEnd();
                traceBeginAndSlog("StartTextServicesManager");
                this.mSystemServiceManager.startService(TextServicesManagerService.Lifecycle.class);
                traceEnd();
                if (!z2) {
                    traceBeginAndSlog("StartTextClassificationManagerService");
                    this.mSystemServiceManager.startService(TextClassificationManagerService.Lifecycle.class);
                    traceEnd();
                }
                traceBeginAndSlog("StartNetworkScoreService");
                this.mSystemServiceManager.startService(NetworkScoreService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("StartNetworkStatsService");
                try {
                    iLockSettingsCreate3 = NetworkStatsService.create(context, iLockSettingsCreate);
                    try {
                        ServiceManager.addService("netstats", iLockSettingsCreate3);
                    } catch (Throwable th13) {
                        th = th13;
                        reportWtf("starting NetworkStats Service", th);
                    }
                } catch (Throwable th14) {
                    th = th14;
                    iLockSettingsCreate3 = iLockSettings;
                }
                traceEnd();
                traceBeginAndSlog("StartNetworkPolicyManagerService");
                try {
                    networkPolicyManagerService = new NetworkPolicyManagerService(context, this.mActivityManagerService, iLockSettingsCreate);
                    try {
                        ServiceManager.addService("netpolicy", networkPolicyManagerService);
                        iLockSettings11 = iLockSettingsAsInterface;
                    } catch (Throwable th15) {
                        th = th15;
                        iLockSettings11 = iLockSettingsAsInterface;
                        reportWtf("starting NetworkPolicy Service", th);
                    }
                } catch (Throwable th16) {
                    th = th16;
                    networkPolicyManagerService = iLockSettings;
                }
                traceEnd();
                if (!this.mOnlyCore) {
                    if (context.getPackageManager().hasSystemFeature("android.hardware.wifi")) {
                        traceBeginAndSlog("StartWifi");
                        this.mSystemServiceManager.startService(WIFI_SERVICE_CLASS);
                        traceEnd();
                        traceBeginAndSlog("StartWifiScanning");
                        this.mSystemServiceManager.startService("com.android.server.wifi.scanner.WifiScanningService");
                        traceEnd();
                    }
                    if (context.getPackageManager().hasSystemFeature("android.hardware.wifi.rtt")) {
                        traceBeginAndSlog("StartRttService");
                        this.mSystemServiceManager.startService("com.android.server.wifi.rtt.RttService");
                        traceEnd();
                    }
                    if (context.getPackageManager().hasSystemFeature("android.hardware.wifi.aware")) {
                        traceBeginAndSlog("StartWifiAware");
                        this.mSystemServiceManager.startService(WIFI_AWARE_SERVICE_CLASS);
                        traceEnd();
                    }
                    if (context.getPackageManager().hasSystemFeature("android.hardware.wifi.direct")) {
                        traceBeginAndSlog("StartWifiP2P");
                        this.mSystemServiceManager.startService(WIFI_P2P_SERVICE_CLASS);
                        traceEnd();
                    }
                    if (context.getPackageManager().hasSystemFeature("android.hardware.lowpan")) {
                        traceBeginAndSlog("StartLowpan");
                        this.mSystemServiceManager.startService(LOWPAN_SERVICE_CLASS);
                        traceEnd();
                    }
                }
                if (this.mPackageManager.hasSystemFeature("android.hardware.ethernet") || this.mPackageManager.hasSystemFeature("android.hardware.usb.host")) {
                    traceBeginAndSlog("StartEthernet");
                    this.mSystemServiceManager.startService(ETHERNET_SERVICE_CLASS);
                    traceEnd();
                }
                traceBeginAndSlog("StartConnectivityService");
                ILockSettings connectivityService = (ConnectivityService) sMtkSystemServerIns.getMtkConnectivityService(iLockSettingsCreate, iLockSettingsCreate3, networkPolicyManagerService);
                if (connectivityService == null) {
                    try {
                        connectivityService = new ConnectivityService(context, iLockSettingsCreate, iLockSettingsCreate3, networkPolicyManagerService);
                        iLockSettings12 = iLockSettingsCreate;
                        iLockSettings6 = iLockSettingsCreate2;
                        try {
                            ServiceManager.addService("connectivity", connectivityService, false, 6);
                            iLockSettingsCreate3.bindConnectivityManager(connectivityService);
                            networkPolicyManagerService.bindConnectivityManager(connectivityService);
                        } catch (Throwable th17) {
                            th = th17;
                            reportWtf("starting Connectivity Service", th);
                        }
                    } catch (Throwable th18) {
                        th = th18;
                        iLockSettings12 = iLockSettingsCreate;
                        iLockSettings6 = iLockSettingsCreate2;
                        reportWtf("starting Connectivity Service", th);
                        traceEnd();
                        traceBeginAndSlog("StartNsdService");
                        ServiceManager.addService("servicediscovery", NsdService.create(context));
                        traceEnd();
                        traceBeginAndSlog("StartSystemUpdateManagerService");
                        ServiceManager.addService("system_update", new SystemUpdateManagerService(context));
                        traceEnd();
                        traceBeginAndSlog("StartUpdateLockService");
                        ServiceManager.addService("updatelock", new UpdateLockService(context));
                        traceEnd();
                        traceBeginAndSlog("StartNotificationManager");
                        this.mSystemServiceManager.startService(NotificationManagerService.class);
                        SystemNotificationChannels.createAll(context);
                        INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
                        traceEnd();
                        traceBeginAndSlog("StartDeviceMonitor");
                        this.mSystemServiceManager.startService(DeviceStorageMonitorService.class);
                        traceEnd();
                        traceBeginAndSlog("StartLocationManagerService");
                        ILockSettings locationManagerService = new LocationManagerService(context);
                        ServiceManager.addService("location", locationManagerService);
                        traceEnd();
                        traceBeginAndSlog("StartCountryDetectorService");
                        ILockSettings countryDetectorService = new CountryDetectorService(context);
                        ServiceManager.addService("country_detector", countryDetectorService);
                        iLockSettings7 = iLockSettingsCreate3;
                        traceEnd();
                        if (!zHasSystemFeature) {
                        }
                        if (context.getResources().getBoolean(R.^attr-private.horizontalProgressLayout)) {
                        }
                        traceBeginAndSlog("StartAudioService");
                        this.mSystemServiceManager.startService(AudioService.Lifecycle.class);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.hardware.broadcastradio")) {
                        }
                        traceBeginAndSlog("StartDockObserver");
                        this.mSystemServiceManager.startService(DockObserver.class);
                        traceEnd();
                        if (zHasSystemFeature) {
                        }
                        traceBeginAndSlog("StartWiredAccessoryManager");
                        r8.setWiredAccessoryCallbacks(new WiredAccessoryManager(context, r8));
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.software.midi")) {
                        }
                        if (this.mPackageManager.hasSystemFeature("android.hardware.usb.host")) {
                        }
                        if (!zHasSystemFeature) {
                        }
                        if (zHasSystemFeature) {
                        }
                        if (!z4) {
                        }
                        if (!z3) {
                        }
                        if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                        }
                        traceBeginAndSlog("StartStatsCompanionService");
                        this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                        traceEnd();
                        zDetectSafeMode = r7.detectSafeMode();
                        if (!zDetectSafeMode) {
                        }
                        traceBeginAndSlog("StartMmsService");
                        final MmsServiceBroker mmsServiceBroker22 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                        }
                        sMtkSystemServerIns.startMtkOtherServices(this.mOnlyCore);
                        traceBeginAndSlog("MakeVibratorServiceReady");
                        r5.systemReady();
                        traceEnd();
                        traceBeginAndSlog("MakeLockSettingsServiceReady");
                        if (iLockSettings2 != null) {
                        }
                        traceEnd();
                        traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                        this.mSystemServiceManager.startBootPhase(SystemService.PHASE_LOCK_SETTINGS_READY);
                        traceEnd();
                        traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                        this.mSystemServiceManager.startBootPhase(SystemService.PHASE_SYSTEM_SERVICES_READY);
                        traceEnd();
                        traceBeginAndSlog("MakeWindowManagerServiceReady");
                        r7.systemReady();
                        traceEnd();
                        if (zDetectSafeMode) {
                        }
                        Configuration configurationComputeNewConfiguration22 = r7.computeNewConfiguration(0);
                        DisplayMetrics displayMetrics22 = new DisplayMetrics();
                        ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(displayMetrics22);
                        context.getResources().updateConfiguration(configurationComputeNewConfiguration22, displayMetrics22);
                        theme = context.getTheme();
                        if (theme.getChangingConfigurations() != 0) {
                        }
                        traceBeginAndSlog("MakePowerManagerServiceReady");
                        this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                        traceEnd();
                        traceBeginAndSlog("MakePackageManagerServiceReady");
                        this.mPackageManagerService.systemReady();
                        traceEnd();
                        traceBeginAndSlog("MakeDisplayManagerServiceReady");
                        this.mDisplayManagerService.systemReady(zDetectSafeMode, this.mOnlyCore);
                        traceEnd();
                        this.mSystemServiceManager.setSafeMode(zDetectSafeMode);
                        traceBeginAndSlog("StartDeviceSpecificServices");
                        while (i < r4) {
                        }
                        traceEnd();
                        traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                        this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                        traceEnd();
                        final WindowManagerService r422 = r7;
                        final NetworkManagementService iLockSettings1322 = iLockSettings;
                        final IpSecService iLockSettings1422 = iLockSettings6;
                        final InputManagerService r1622 = r8;
                        final NetworkStatsService iLockSettings1522 = iLockSettings7;
                        final ConnectivityService iLockSettings1622 = iLockSettings10;
                        final CountryDetectorService iLockSettings1722 = iLockSettings5;
                        final NetworkTimeUpdateService iLockSettings1822 = iLockSettings8;
                        final CommonTimeManagementService iLockSettings1922 = commonTimeManagementService;
                        final TelephonyRegistry r1522 = stub3;
                        final MediaRouterService iLockSettings2022 = iLockSettings9;
                        this.mActivityManagerService.systemReady(new Runnable() {
                            @Override
                            public final void run() {
                                SystemServer.lambda$startOtherServices$4(this.f$0, context, r422, iLockSettings1322, iLockSettings3, iLockSettings1422, iLockSettings1522, iLockSettings1622, iLockSettings4, iLockSettings1722, iLockSettings1822, iLockSettings1922, r1622, r1522, iLockSettings2022, mmsServiceBroker22);
                            }
                        }, BOOT_TIMINGS_TRACE_LOG);
                        return;
                    }
                    traceEnd();
                    traceBeginAndSlog("StartNsdService");
                    ServiceManager.addService("servicediscovery", NsdService.create(context));
                    traceEnd();
                    traceBeginAndSlog("StartSystemUpdateManagerService");
                    ServiceManager.addService("system_update", new SystemUpdateManagerService(context));
                    traceEnd();
                    traceBeginAndSlog("StartUpdateLockService");
                    ServiceManager.addService("updatelock", new UpdateLockService(context));
                    traceEnd();
                    traceBeginAndSlog("StartNotificationManager");
                    this.mSystemServiceManager.startService(NotificationManagerService.class);
                    SystemNotificationChannels.createAll(context);
                    INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
                    traceEnd();
                    traceBeginAndSlog("StartDeviceMonitor");
                    this.mSystemServiceManager.startService(DeviceStorageMonitorService.class);
                    traceEnd();
                    traceBeginAndSlog("StartLocationManagerService");
                    ILockSettings locationManagerService2 = new LocationManagerService(context);
                    ServiceManager.addService("location", locationManagerService2);
                    traceEnd();
                    traceBeginAndSlog("StartCountryDetectorService");
                    ILockSettings countryDetectorService2 = new CountryDetectorService(context);
                    ServiceManager.addService("country_detector", countryDetectorService2);
                    iLockSettings7 = iLockSettingsCreate3;
                    traceEnd();
                    if (!zHasSystemFeature) {
                        traceBeginAndSlog("StartSearchManagerService");
                        try {
                            this.mSystemServiceManager.startService(SEARCH_MANAGER_SERVICE_CLASS);
                        } catch (Throwable th19) {
                            reportWtf("starting Search Service", th19);
                        }
                        traceEnd();
                    }
                    if (context.getResources().getBoolean(R.^attr-private.horizontalProgressLayout)) {
                        traceBeginAndSlog("StartWallpaperManagerService");
                        this.mSystemServiceManager.startService(WALLPAPER_SERVICE_CLASS);
                        traceEnd();
                    }
                    traceBeginAndSlog("StartAudioService");
                    this.mSystemServiceManager.startService(AudioService.Lifecycle.class);
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.hardware.broadcastradio")) {
                        traceBeginAndSlog("StartBroadcastRadioService");
                        this.mSystemServiceManager.startService(BroadcastRadioService.class);
                        traceEnd();
                    }
                    traceBeginAndSlog("StartDockObserver");
                    this.mSystemServiceManager.startService(DockObserver.class);
                    traceEnd();
                    if (zHasSystemFeature) {
                        traceBeginAndSlog("StartThermalObserver");
                        this.mSystemServiceManager.startService(THERMAL_OBSERVER_CLASS);
                        traceEnd();
                    }
                    traceBeginAndSlog("StartWiredAccessoryManager");
                    r8.setWiredAccessoryCallbacks(new WiredAccessoryManager(context, r8));
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.software.midi")) {
                        traceBeginAndSlog("StartMidiManager");
                        this.mSystemServiceManager.startService(MIDI_SERVICE_CLASS);
                        traceEnd();
                    }
                    if (this.mPackageManager.hasSystemFeature("android.hardware.usb.host") || this.mPackageManager.hasSystemFeature("android.hardware.usb.accessory") || zEquals) {
                        traceBeginAndSlog("StartUsbService");
                        this.mSystemServiceManager.startService(USB_SERVICE_CLASS);
                        traceEnd();
                    }
                    if (!zHasSystemFeature) {
                        traceBeginAndSlog("StartSerialService");
                        try {
                            ServiceManager.addService("serial", new SerialService(context));
                        } catch (Throwable th20) {
                            Slog.e(TAG, "Failure starting SerialService", th20);
                        }
                        traceEnd();
                    }
                    traceBeginAndSlog("StartHardwarePropertiesManagerService");
                    ServiceManager.addService("hardware_properties", new HardwarePropertiesManagerService(context));
                    traceEnd();
                    traceBeginAndSlog("StartTwilightService");
                    this.mSystemServiceManager.startService(TwilightService.class);
                    traceEnd();
                    if (ColorDisplayController.isAvailable(context)) {
                        traceBeginAndSlog("StartNightDisplay");
                        this.mSystemServiceManager.startService(ColorDisplayService.class);
                        traceEnd();
                    }
                    traceBeginAndSlog("StartJobScheduler");
                    this.mSystemServiceManager.startService(JobSchedulerService.class);
                    traceEnd();
                    traceBeginAndSlog("StartSoundTrigger");
                    this.mSystemServiceManager.startService(SoundTriggerService.class);
                    traceEnd();
                    traceBeginAndSlog("StartTrustManager");
                    this.mSystemServiceManager.startService(TrustManagerService.class);
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                        traceBeginAndSlog("StartBackupManager");
                        this.mSystemServiceManager.startService(BACKUP_MANAGER_SERVICE_CLASS);
                        traceEnd();
                    }
                    if (!this.mPackageManager.hasSystemFeature("android.software.app_widgets") || context.getResources().getBoolean(R.^attr-private.floatingToolbarItemBackgroundBorderlessDrawable)) {
                        traceBeginAndSlog("StartAppWidgerService");
                        this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                        traceEnd();
                    }
                    traceBeginAndSlog("StartVoiceRecognitionManager");
                    this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                    traceEnd();
                    if (GestureLauncherService.isGestureLauncherEnabled(context.getResources())) {
                        traceBeginAndSlog("StartGestureLauncher");
                        this.mSystemServiceManager.startService(GestureLauncherService.class);
                        traceEnd();
                    }
                    traceBeginAndSlog("StartSensorNotification");
                    this.mSystemServiceManager.startService(SensorNotificationService.class);
                    traceEnd();
                    traceBeginAndSlog("StartContextHubSystemService");
                    this.mSystemServiceManager.startService(ContextHubSystemService.class);
                    traceEnd();
                    traceBeginAndSlog("StartDiskStatsService");
                    ServiceManager.addService("diskstats", new DiskStatsService(context));
                    traceEnd();
                    if (this.mOnlyCore && context.getResources().getBoolean(R.^attr-private.hideWheelUntilFocused)) {
                        traceBeginAndSlog("StartTimeZoneRulesManagerService");
                        this.mSystemServiceManager.startService(TIME_ZONE_RULES_MANAGER_SERVICE_CLASS);
                        traceEnd();
                    }
                    if (zHasSystemFeature) {
                        traceBeginAndSlog("StartNetworkTimeUpdateService");
                        try {
                            networkTimeUpdateService = new NetworkTimeUpdateService(context);
                            try {
                                ServiceManager.addService("network_time_update_service", networkTimeUpdateService);
                            } catch (Throwable th21) {
                                th = th21;
                                reportWtf("starting NetworkTimeUpdate service", th);
                            }
                        } catch (Throwable th22) {
                            th = th22;
                            networkTimeUpdateService = iLockSettings;
                        }
                        traceEnd();
                    } else {
                        networkTimeUpdateService = iLockSettings;
                    }
                    traceBeginAndSlog("StartCommonTimeManagementService");
                    commonTimeManagementService = new CommonTimeManagementService(context);
                    ServiceManager.addService("commontime_management", commonTimeManagementService);
                    iLockSettings8 = networkTimeUpdateService;
                    traceEnd();
                    traceBeginAndSlog("CertBlacklister");
                    new CertBlacklister(context);
                    traceEnd();
                    traceBeginAndSlog("StartEmergencyAffordanceService");
                    this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                    traceEnd();
                    traceBeginAndSlog("StartDreamManager");
                    this.mSystemServiceManager.startService(DreamManagerService.class);
                    traceEnd();
                    traceBeginAndSlog("AddGraphicsStatsService");
                    ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, new GraphicsStatsService(context));
                    traceEnd();
                    if (CoverageService.ENABLED) {
                        traceBeginAndSlog("AddCoverageService");
                        ServiceManager.addService(CoverageService.COVERAGE_SERVICE, new CoverageService());
                        traceEnd();
                    }
                    if (this.mPackageManager.hasSystemFeature("android.software.print")) {
                        traceBeginAndSlog("StartPrintManager");
                        this.mSystemServiceManager.startService(PRINT_MANAGER_SERVICE_CLASS);
                        traceEnd();
                    }
                    if (this.mPackageManager.hasSystemFeature("android.software.companion_device_setup")) {
                        traceBeginAndSlog("StartCompanionDeviceManager");
                        this.mSystemServiceManager.startService(COMPANION_DEVICE_MANAGER_SERVICE_CLASS);
                        traceEnd();
                    }
                    traceBeginAndSlog("StartRestrictionManager");
                    this.mSystemServiceManager.startService(RestrictionsManagerService.class);
                    traceEnd();
                    traceBeginAndSlog("StartMediaSessionService");
                    this.mSystemServiceManager.startService(MediaSessionService.class);
                    traceEnd();
                    traceBeginAndSlog("StartMediaUpdateService");
                    this.mSystemServiceManager.startService(MediaUpdateService.class);
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.hardware.hdmi.cec")) {
                        traceBeginAndSlog("StartHdmiControlService");
                        this.mSystemServiceManager.startService(HdmiControlService.class);
                        traceEnd();
                    }
                    if (!this.mPackageManager.hasSystemFeature("android.software.live_tv") || this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                        traceBeginAndSlog("StartTvInputManager");
                        this.mSystemServiceManager.startService(TvInputManagerService.class);
                        traceEnd();
                    }
                    if (this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
                        traceBeginAndSlog("StartMediaResourceMonitor");
                        this.mSystemServiceManager.startService(MediaResourceMonitorService.class);
                        traceEnd();
                    }
                    if (this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                        traceBeginAndSlog("StartTvRemoteService");
                        this.mSystemServiceManager.startService(TvRemoteService.class);
                        traceEnd();
                    }
                    traceBeginAndSlog("StartMediaRouterService");
                    try {
                        mediaRouterService = new MediaRouterService(context);
                        try {
                            ServiceManager.addService("media_router", mediaRouterService);
                            iLockSettings9 = mediaRouterService;
                        } catch (Throwable th23) {
                            th = th23;
                            iLockSettings9 = mediaRouterService;
                            reportWtf("starting MediaRouterService", th);
                        }
                    } catch (Throwable th24) {
                        th = th24;
                        mediaRouterService = iLockSettings;
                    }
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.hardware.fingerprint")) {
                        traceBeginAndSlog("StartFingerprintSensor");
                        this.mSystemServiceManager.startService(FingerprintService.class);
                        traceEnd();
                    }
                    traceBeginAndSlog("StartBackgroundDexOptService");
                    BackgroundDexOptService.schedule(context);
                    iLockSettings10 = connectivityService;
                    traceEnd();
                    traceBeginAndSlog("StartPruneInstantAppsJobService");
                    PruneInstantAppsJobService.schedule(context);
                    traceEnd();
                    traceBeginAndSlog("StartShortcutServiceLifecycle");
                    this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                    traceEnd();
                    traceBeginAndSlog("StartLauncherAppsService");
                    this.mSystemServiceManager.startService(LauncherAppsService.class);
                    traceEnd();
                    traceBeginAndSlog("StartCrossProfileAppsService");
                    this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                    traceEnd();
                    iLockSettings5 = countryDetectorService2;
                    iLockSettings3 = networkPolicyManagerService;
                    iLockSettings2 = iLockSettings11;
                    iLockSettings = iLockSettings12;
                    iLockSettings4 = locationManagerService2;
                } else {
                    iLockSettings12 = iLockSettingsCreate;
                    iLockSettings6 = iLockSettingsCreate2;
                    ServiceManager.addService("connectivity", connectivityService, false, 6);
                    iLockSettingsCreate3.bindConnectivityManager(connectivityService);
                    networkPolicyManagerService.bindConnectivityManager(connectivityService);
                    traceEnd();
                    traceBeginAndSlog("StartNsdService");
                    ServiceManager.addService("servicediscovery", NsdService.create(context));
                    traceEnd();
                    traceBeginAndSlog("StartSystemUpdateManagerService");
                    ServiceManager.addService("system_update", new SystemUpdateManagerService(context));
                    traceEnd();
                    traceBeginAndSlog("StartUpdateLockService");
                    ServiceManager.addService("updatelock", new UpdateLockService(context));
                    traceEnd();
                    traceBeginAndSlog("StartNotificationManager");
                    this.mSystemServiceManager.startService(NotificationManagerService.class);
                    SystemNotificationChannels.createAll(context);
                    INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
                    traceEnd();
                    traceBeginAndSlog("StartDeviceMonitor");
                    this.mSystemServiceManager.startService(DeviceStorageMonitorService.class);
                    traceEnd();
                    traceBeginAndSlog("StartLocationManagerService");
                    ILockSettings locationManagerService22 = new LocationManagerService(context);
                    ServiceManager.addService("location", locationManagerService22);
                    traceEnd();
                    traceBeginAndSlog("StartCountryDetectorService");
                    ILockSettings countryDetectorService22 = new CountryDetectorService(context);
                    ServiceManager.addService("country_detector", countryDetectorService22);
                    iLockSettings7 = iLockSettingsCreate3;
                    traceEnd();
                    if (!zHasSystemFeature) {
                    }
                    if (context.getResources().getBoolean(R.^attr-private.horizontalProgressLayout)) {
                    }
                    traceBeginAndSlog("StartAudioService");
                    this.mSystemServiceManager.startService(AudioService.Lifecycle.class);
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.hardware.broadcastradio")) {
                    }
                    traceBeginAndSlog("StartDockObserver");
                    this.mSystemServiceManager.startService(DockObserver.class);
                    traceEnd();
                    if (zHasSystemFeature) {
                    }
                    traceBeginAndSlog("StartWiredAccessoryManager");
                    r8.setWiredAccessoryCallbacks(new WiredAccessoryManager(context, r8));
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.software.midi")) {
                    }
                    if (this.mPackageManager.hasSystemFeature("android.hardware.usb.host")) {
                        traceBeginAndSlog("StartUsbService");
                        this.mSystemServiceManager.startService(USB_SERVICE_CLASS);
                        traceEnd();
                        if (!zHasSystemFeature) {
                        }
                        traceBeginAndSlog("StartHardwarePropertiesManagerService");
                        ServiceManager.addService("hardware_properties", new HardwarePropertiesManagerService(context));
                        traceEnd();
                        traceBeginAndSlog("StartTwilightService");
                        this.mSystemServiceManager.startService(TwilightService.class);
                        traceEnd();
                        if (ColorDisplayController.isAvailable(context)) {
                        }
                        traceBeginAndSlog("StartJobScheduler");
                        this.mSystemServiceManager.startService(JobSchedulerService.class);
                        traceEnd();
                        traceBeginAndSlog("StartSoundTrigger");
                        this.mSystemServiceManager.startService(SoundTriggerService.class);
                        traceEnd();
                        traceBeginAndSlog("StartTrustManager");
                        this.mSystemServiceManager.startService(TrustManagerService.class);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                        }
                        if (!this.mPackageManager.hasSystemFeature("android.software.app_widgets")) {
                            traceBeginAndSlog("StartAppWidgerService");
                            this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                            traceEnd();
                            traceBeginAndSlog("StartVoiceRecognitionManager");
                            this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                            traceEnd();
                            if (GestureLauncherService.isGestureLauncherEnabled(context.getResources())) {
                            }
                            traceBeginAndSlog("StartSensorNotification");
                            this.mSystemServiceManager.startService(SensorNotificationService.class);
                            traceEnd();
                            traceBeginAndSlog("StartContextHubSystemService");
                            this.mSystemServiceManager.startService(ContextHubSystemService.class);
                            traceEnd();
                            traceBeginAndSlog("StartDiskStatsService");
                            ServiceManager.addService("diskstats", new DiskStatsService(context));
                            traceEnd();
                            if (this.mOnlyCore) {
                                if (this.mOnlyCore && context.getResources().getBoolean(R.^attr-private.hideWheelUntilFocused)) {
                                }
                                if (zHasSystemFeature) {
                                }
                                traceBeginAndSlog("StartCommonTimeManagementService");
                                commonTimeManagementService = new CommonTimeManagementService(context);
                                ServiceManager.addService("commontime_management", commonTimeManagementService);
                                iLockSettings8 = networkTimeUpdateService;
                                traceEnd();
                                traceBeginAndSlog("CertBlacklister");
                                new CertBlacklister(context);
                                traceEnd();
                                traceBeginAndSlog("StartEmergencyAffordanceService");
                                this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                                traceEnd();
                                traceBeginAndSlog("StartDreamManager");
                                this.mSystemServiceManager.startService(DreamManagerService.class);
                                traceEnd();
                                traceBeginAndSlog("AddGraphicsStatsService");
                                ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, new GraphicsStatsService(context));
                                traceEnd();
                                if (CoverageService.ENABLED) {
                                }
                                if (this.mPackageManager.hasSystemFeature("android.software.print")) {
                                }
                                if (this.mPackageManager.hasSystemFeature("android.software.companion_device_setup")) {
                                }
                                traceBeginAndSlog("StartRestrictionManager");
                                this.mSystemServiceManager.startService(RestrictionsManagerService.class);
                                traceEnd();
                                traceBeginAndSlog("StartMediaSessionService");
                                this.mSystemServiceManager.startService(MediaSessionService.class);
                                traceEnd();
                                traceBeginAndSlog("StartMediaUpdateService");
                                this.mSystemServiceManager.startService(MediaUpdateService.class);
                                traceEnd();
                                if (this.mPackageManager.hasSystemFeature("android.hardware.hdmi.cec")) {
                                }
                                if (!this.mPackageManager.hasSystemFeature("android.software.live_tv")) {
                                    traceBeginAndSlog("StartTvInputManager");
                                    this.mSystemServiceManager.startService(TvInputManagerService.class);
                                    traceEnd();
                                    if (this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
                                    }
                                    if (this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                                    }
                                    traceBeginAndSlog("StartMediaRouterService");
                                    mediaRouterService = new MediaRouterService(context);
                                    ServiceManager.addService("media_router", mediaRouterService);
                                    iLockSettings9 = mediaRouterService;
                                    traceEnd();
                                    if (this.mPackageManager.hasSystemFeature("android.hardware.fingerprint")) {
                                    }
                                    traceBeginAndSlog("StartBackgroundDexOptService");
                                    BackgroundDexOptService.schedule(context);
                                    iLockSettings10 = connectivityService;
                                    traceEnd();
                                    traceBeginAndSlog("StartPruneInstantAppsJobService");
                                    PruneInstantAppsJobService.schedule(context);
                                    traceEnd();
                                    traceBeginAndSlog("StartShortcutServiceLifecycle");
                                    this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                                    traceEnd();
                                    traceBeginAndSlog("StartLauncherAppsService");
                                    this.mSystemServiceManager.startService(LauncherAppsService.class);
                                    traceEnd();
                                    traceBeginAndSlog("StartCrossProfileAppsService");
                                    this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                                    traceEnd();
                                    iLockSettings5 = countryDetectorService22;
                                    iLockSettings3 = networkPolicyManagerService;
                                    iLockSettings2 = iLockSettings11;
                                    iLockSettings = iLockSettings12;
                                    iLockSettings4 = locationManagerService22;
                                }
                            }
                        }
                    }
                }
            } else {
                iLockSettings2 = iLockSettings;
                iLockSettings3 = iLockSettings2;
                iLockSettings4 = iLockSettings3;
                commonTimeManagementService = iLockSettings4;
                iLockSettings5 = commonTimeManagementService;
                iLockSettings6 = iLockSettings5;
                iLockSettings7 = iLockSettings6;
                iLockSettings8 = iLockSettings7;
                iLockSettings9 = iLockSettings8;
                iLockSettings10 = iLockSettings9;
            }
            if (!zHasSystemFeature) {
                traceBeginAndSlog("StartMediaProjectionManager");
                this.mSystemServiceManager.startService(MediaProjectionManagerService.class);
                traceEnd();
            }
            if (zHasSystemFeature) {
                traceBeginAndSlog("StartWearConfigService");
                this.mSystemServiceManager.startService(WEAR_CONFIG_SERVICE_CLASS);
                traceEnd();
                traceBeginAndSlog("StartWearConnectivityService");
                this.mSystemServiceManager.startService(WEAR_CONNECTIVITY_SERVICE_CLASS);
                traceEnd();
                traceBeginAndSlog("StartWearTimeService");
                this.mSystemServiceManager.startService(WEAR_DISPLAY_SERVICE_CLASS);
                this.mSystemServiceManager.startService(WEAR_TIME_SERVICE_CLASS);
                traceEnd();
                if (z5) {
                    traceBeginAndSlog("StartWearLeftyService");
                    this.mSystemServiceManager.startService(WEAR_LEFTY_SERVICE_CLASS);
                    traceEnd();
                }
                traceBeginAndSlog("StartWearGlobalActionsService");
                this.mSystemServiceManager.startService(WEAR_GLOBAL_ACTIONS_SERVICE_CLASS);
                traceEnd();
            }
            if (!z4) {
                traceBeginAndSlog("StartSliceManagerService");
                this.mSystemServiceManager.startService(SLICE_MANAGER_SERVICE_CLASS);
                traceEnd();
            }
            if (!z3) {
                traceBeginAndSlog("StartCameraServiceProxy");
                this.mSystemServiceManager.startService(CameraServiceProxy.class);
                traceEnd();
            }
            if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                traceBeginAndSlog("StartIoTSystemService");
                this.mSystemServiceManager.startService(IOT_SERVICE_CLASS);
                traceEnd();
            }
            traceBeginAndSlog("StartStatsCompanionService");
            this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
            traceEnd();
            zDetectSafeMode = r7.detectSafeMode();
            if (!zDetectSafeMode) {
                traceBeginAndSlog("EnterSafeModeAndDisableJitCompilation");
                this.mActivityManagerService.enterSafeMode();
                VMRuntime.getRuntime().disableJitCompilation();
                traceEnd();
            } else {
                traceBeginAndSlog("StartJitCompilation");
                VMRuntime.getRuntime().startJitCompilation();
                traceEnd();
            }
            traceBeginAndSlog("StartMmsService");
            final MmsServiceBroker mmsServiceBroker222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
            traceEnd();
            if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                traceBeginAndSlog("StartAutoFillService");
                this.mSystemServiceManager.startService(AUTO_FILL_MANAGER_SERVICE_CLASS);
                traceEnd();
            }
            sMtkSystemServerIns.startMtkOtherServices(this.mOnlyCore);
            traceBeginAndSlog("MakeVibratorServiceReady");
            try {
                r5.systemReady();
            } catch (Throwable th25) {
                reportWtf("making Vibrator Service ready", th25);
            }
            traceEnd();
            traceBeginAndSlog("MakeLockSettingsServiceReady");
            if (iLockSettings2 != null) {
                try {
                    iLockSettings2.systemReady();
                } catch (Throwable th26) {
                    reportWtf("making Lock Settings Service ready", th26);
                }
            }
            traceEnd();
            traceBeginAndSlog("StartBootPhaseLockSettingsReady");
            this.mSystemServiceManager.startBootPhase(SystemService.PHASE_LOCK_SETTINGS_READY);
            traceEnd();
            traceBeginAndSlog("StartBootPhaseSystemServicesReady");
            this.mSystemServiceManager.startBootPhase(SystemService.PHASE_SYSTEM_SERVICES_READY);
            traceEnd();
            traceBeginAndSlog("MakeWindowManagerServiceReady");
            try {
                r7.systemReady();
            } catch (Throwable th27) {
                reportWtf("making Window Manager Service ready", th27);
            }
            traceEnd();
            if (zDetectSafeMode) {
                this.mActivityManagerService.showSafeModeOverlay();
            }
            Configuration configurationComputeNewConfiguration222 = r7.computeNewConfiguration(0);
            DisplayMetrics displayMetrics222 = new DisplayMetrics();
            ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(displayMetrics222);
            context.getResources().updateConfiguration(configurationComputeNewConfiguration222, displayMetrics222);
            theme = context.getTheme();
            if (theme.getChangingConfigurations() != 0) {
                theme.rebase();
            }
            traceBeginAndSlog("MakePowerManagerServiceReady");
            try {
                this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
            } catch (Throwable th28) {
                reportWtf("making Power Manager Service ready", th28);
            }
            traceEnd();
            traceBeginAndSlog("MakePackageManagerServiceReady");
            this.mPackageManagerService.systemReady();
            traceEnd();
            traceBeginAndSlog("MakeDisplayManagerServiceReady");
            try {
                this.mDisplayManagerService.systemReady(zDetectSafeMode, this.mOnlyCore);
            } catch (Throwable th29) {
                reportWtf("making Display Manager Service ready", th29);
            }
            traceEnd();
            this.mSystemServiceManager.setSafeMode(zDetectSafeMode);
            traceBeginAndSlog("StartDeviceSpecificServices");
            for (String str : this.mSystemContext.getResources().getStringArray(R.array.config_batteryPackageTypeService)) {
                traceBeginAndSlog("StartDeviceSpecificServices " + str);
                try {
                    this.mSystemServiceManager.startService(str);
                } catch (Throwable th30) {
                    reportWtf("starting " + str, th30);
                }
                traceEnd();
            }
            traceEnd();
            traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
            this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
            traceEnd();
            final WindowManagerService r4222 = r7;
            final NetworkManagementService iLockSettings13222 = iLockSettings;
            final IpSecService iLockSettings14222 = iLockSettings6;
            final InputManagerService r16222 = r8;
            final NetworkStatsService iLockSettings15222 = iLockSettings7;
            final ConnectivityService iLockSettings16222 = iLockSettings10;
            final CountryDetectorService iLockSettings17222 = iLockSettings5;
            final NetworkTimeUpdateService iLockSettings18222 = iLockSettings8;
            final CommonTimeManagementService iLockSettings19222 = commonTimeManagementService;
            final TelephonyRegistry r15222 = stub3;
            final MediaRouterService iLockSettings20222 = iLockSettings9;
            this.mActivityManagerService.systemReady(new Runnable() {
                @Override
                public final void run() {
                    SystemServer.lambda$startOtherServices$4(this.f$0, context, r4222, iLockSettings13222, iLockSettings3, iLockSettings14222, iLockSettings15222, iLockSettings16222, iLockSettings4, iLockSettings17222, iLockSettings18222, iLockSettings19222, r16222, r15222, iLockSettings20222, mmsServiceBroker222);
                }
            }, BOOT_TIMINGS_TRACE_LOG);
            return;
        }
        throw new RuntimeException();
    }

    static void lambda$startOtherServices$1() {
        try {
            Slog.i(TAG, "SecondaryZygotePreload");
            TimingsTraceLog timingsTraceLog = new TimingsTraceLog(SYSTEM_SERVER_TIMING_ASYNC_TAG, 524288L);
            timingsTraceLog.traceBegin("SecondaryZygotePreload");
            if (!Process.zygoteProcess.preloadDefault(Build.SUPPORTED_32_BIT_ABIS[0])) {
                Slog.e(TAG, "Unable to preload default resources");
            }
            timingsTraceLog.traceEnd();
        } catch (Exception e) {
            Slog.e(TAG, "Exception preloading default resources", e);
        }
    }

    static void lambda$startOtherServices$2() {
        TimingsTraceLog timingsTraceLog = new TimingsTraceLog(SYSTEM_SERVER_TIMING_ASYNC_TAG, 524288L);
        timingsTraceLog.traceBegin(START_HIDL_SERVICES);
        startHidlServices();
        timingsTraceLog.traceEnd();
    }

    public static void lambda$startOtherServices$4(final SystemServer systemServer, Context context, WindowManagerService windowManagerService, NetworkManagementService networkManagementService, NetworkPolicyManagerService networkPolicyManagerService, IpSecService ipSecService, NetworkStatsService networkStatsService, ConnectivityService connectivityService, LocationManagerService locationManagerService, CountryDetectorService countryDetectorService, NetworkTimeUpdateService networkTimeUpdateService, CommonTimeManagementService commonTimeManagementService, InputManagerService inputManagerService, TelephonyRegistry telephonyRegistry, MediaRouterService mediaRouterService, MmsServiceBroker mmsServiceBroker) {
        Slog.i(TAG, "Making services ready");
        traceBeginAndSlog("StartActivityManagerReadyPhase");
        systemServer.mSystemServiceManager.startBootPhase(SystemService.PHASE_ACTIVITY_MANAGER_READY);
        traceEnd();
        traceBeginAndSlog("StartObservingNativeCrashes");
        try {
            systemServer.mActivityManagerService.startObservingNativeCrashes();
        } catch (Throwable th) {
            systemServer.reportWtf("observing native crashes", th);
        }
        traceEnd();
        Future<?> futureSubmit = (systemServer.mOnlyCore || systemServer.mWebViewUpdateService == null) ? null : SystemServerInitThreadPool.get().submit(new Runnable() {
            @Override
            public final void run() {
                SystemServer.lambda$startOtherServices$3(this.f$0);
            }
        }, "WebViewFactoryPreparation");
        if (systemServer.mPackageManager.hasSystemFeature("android.hardware.type.automotive")) {
            traceBeginAndSlog("StartCarServiceHelperService");
            systemServer.mSystemServiceManager.startService(CAR_SERVICE_HELPER_SERVICE_CLASS);
            traceEnd();
        }
        traceBeginAndSlog("StartSystemUI");
        try {
            startSystemUi(context, windowManagerService);
        } catch (Throwable th2) {
            if (!ENCRYPTED_STATE.equals(SystemProperties.get("ro.config.simplelauncher"))) {
                systemServer.reportWtf("starting System UI", th2);
            }
        }
        traceEnd();
        traceBeginAndSlog("MakeNetworkManagementServiceReady");
        if (networkManagementService != null) {
            try {
                networkManagementService.systemReady();
            } catch (Throwable th3) {
                systemServer.reportWtf("making Network Managment Service ready", th3);
            }
        }
        CountDownLatch countDownLatchNetworkScoreAndNetworkManagementServiceReady = networkPolicyManagerService != null ? networkPolicyManagerService.networkScoreAndNetworkManagementServiceReady() : null;
        traceEnd();
        traceBeginAndSlog("MakeIpSecServiceReady");
        if (ipSecService != null) {
            try {
                ipSecService.systemReady();
            } catch (Throwable th4) {
                systemServer.reportWtf("making IpSec Service ready", th4);
            }
        }
        traceEnd();
        traceBeginAndSlog("MakeNetworkStatsServiceReady");
        if (networkStatsService != null) {
            try {
                networkStatsService.systemReady();
            } catch (Throwable th5) {
                systemServer.reportWtf("making Network Stats Service ready", th5);
            }
        }
        traceEnd();
        sMtkSystemServerIns.addBootEvent("SystemServer:NetworkStatsService systemReady");
        traceBeginAndSlog("MakeConnectivityServiceReady");
        if (connectivityService != null) {
            try {
                connectivityService.systemReady();
            } catch (Throwable th6) {
                systemServer.reportWtf("making Connectivity Service ready", th6);
            }
        }
        traceEnd();
        sMtkSystemServerIns.addBootEvent("SystemServer:ConnectivityService systemReady");
        traceBeginAndSlog("MakeNetworkPolicyServiceReady");
        if (networkPolicyManagerService != null) {
            try {
                networkPolicyManagerService.systemReady(countDownLatchNetworkScoreAndNetworkManagementServiceReady);
            } catch (Throwable th7) {
                systemServer.reportWtf("making Network Policy Service ready", th7);
            }
        }
        traceEnd();
        sMtkSystemServerIns.addBootEvent("SystemServer:NetworkPolicyManagerServ systemReady");
        traceBeginAndSlog("StartWatchdog");
        Watchdog.getInstance().start();
        traceEnd();
        systemServer.mPackageManagerService.waitForAppDataPrepared();
        traceBeginAndSlog("PhaseThirdPartyAppsCanStart");
        if (futureSubmit != null) {
            ConcurrentUtils.waitForFutureNoInterrupt(futureSubmit, "WebViewFactoryPreparation");
        }
        systemServer.mSystemServiceManager.startBootPhase(600);
        traceEnd();
        traceBeginAndSlog("MakeLocationServiceReady");
        if (locationManagerService != null) {
            try {
                locationManagerService.systemRunning();
            } catch (Throwable th8) {
                systemServer.reportWtf("Notifying Location Service running", th8);
            }
        }
        traceEnd();
        traceBeginAndSlog("MakeCountryDetectionServiceReady");
        if (countryDetectorService != null) {
            try {
                countryDetectorService.systemRunning();
            } catch (Throwable th9) {
                systemServer.reportWtf("Notifying CountryDetectorService running", th9);
            }
        }
        traceEnd();
        traceBeginAndSlog("MakeNetworkTimeUpdateReady");
        if (networkTimeUpdateService != null) {
            try {
                networkTimeUpdateService.systemRunning();
            } catch (Throwable th10) {
                systemServer.reportWtf("Notifying NetworkTimeService running", th10);
            }
        }
        traceEnd();
        traceBeginAndSlog("MakeCommonTimeManagementServiceReady");
        if (commonTimeManagementService != null) {
            try {
                commonTimeManagementService.systemRunning();
            } catch (Throwable th11) {
                systemServer.reportWtf("Notifying CommonTimeManagementService running", th11);
            }
        }
        traceEnd();
        traceBeginAndSlog("MakeInputManagerServiceReady");
        if (inputManagerService != null) {
            try {
                inputManagerService.systemRunning();
            } catch (Throwable th12) {
                systemServer.reportWtf("Notifying InputManagerService running", th12);
            }
        }
        traceEnd();
        traceBeginAndSlog("MakeTelephonyRegistryReady");
        if (telephonyRegistry != null) {
            try {
                telephonyRegistry.systemRunning();
            } catch (Throwable th13) {
                systemServer.reportWtf("Notifying TelephonyRegistry running", th13);
            }
        }
        traceEnd();
        traceBeginAndSlog("MakeMediaRouterServiceReady");
        if (mediaRouterService != null) {
            try {
                mediaRouterService.systemRunning();
            } catch (Throwable th14) {
                systemServer.reportWtf("Notifying MediaRouterService running", th14);
            }
        }
        traceEnd();
        traceBeginAndSlog("MakeMmsServiceReady");
        if (mmsServiceBroker != null) {
            try {
                mmsServiceBroker.systemRunning();
            } catch (Throwable th15) {
                systemServer.reportWtf("Notifying MmsService running", th15);
            }
        }
        traceEnd();
        traceBeginAndSlog("IncidentDaemonReady");
        try {
            IIncidentManager iIncidentManagerAsInterface = IIncidentManager.Stub.asInterface(ServiceManager.getService("incident"));
            if (iIncidentManagerAsInterface != null) {
                iIncidentManagerAsInterface.systemRunning();
            }
        } catch (Throwable th16) {
            systemServer.reportWtf("Notifying incident daemon running", th16);
        }
        traceEnd();
        sMtkSystemServerIns.addBootEvent("SystemServer:PhaseThirdPartyAppsCanStart");
    }

    public static void lambda$startOtherServices$3(SystemServer systemServer) {
        Slog.i(TAG, "WebViewFactoryPreparation");
        TimingsTraceLog timingsTraceLog = new TimingsTraceLog(SYSTEM_SERVER_TIMING_ASYNC_TAG, 524288L);
        timingsTraceLog.traceBegin("WebViewFactoryPreparation");
        ConcurrentUtils.waitForFutureNoInterrupt(systemServer.mZygotePreload, "Zygote preload");
        systemServer.mZygotePreload = null;
        systemServer.mWebViewUpdateService.prepareWebViewInSystemServer();
        timingsTraceLog.traceEnd();
    }

    static final void startSystemUi(Context context, WindowManagerService windowManagerService) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.systemui", "com.android.systemui.SystemUIService"));
        intent.addFlags(256);
        context.startServiceAsUser(intent, UserHandle.SYSTEM);
        windowManagerService.onSystemUiStarted();
    }

    private static void traceBeginAndSlog(String str) {
        Slog.i(TAG, str);
        BOOT_TIMINGS_TRACE_LOG.traceBegin(str);
    }

    private static void traceEnd() {
        BOOT_TIMINGS_TRACE_LOG.traceEnd();
    }
}
