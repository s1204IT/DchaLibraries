package com.android.server;

import android.R;
import android.accounts.IAccountManager;
import android.app.ActivityManagerNative;
import android.app.ActivityThread;
import android.app.IAlarmManager;
import android.app.INotificationManager;
import android.app.IWallpaperManager;
import android.app.usage.UsageStatsManagerInternal;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.ICountryDetector;
import android.location.ILocationManager;
import android.media.AudioService;
import android.media.IMediaRouterService;
import android.net.IConnectivityManager;
import android.net.INetworkPolicyManager;
import android.net.INetworkScoreService;
import android.net.INetworkStatsService;
import android.os.Build;
import android.os.Environment;
import android.os.FactoryTest;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.IVibratorService;
import android.os.Looper;
import android.os.Process;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.storage.IMountService;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.Slog;
import android.view.IAssetAtlas;
import android.view.WindowManager;
import android.webkit.WebViewFactory;
import com.android.internal.os.BinderInternal;
import com.android.internal.os.SamplingProfilerIntegration;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.telephony.ITelephonyRegistry;
import com.android.internal.textservice.ITextServicesManager;
import com.android.internal.view.IInputMethodManager;
import com.android.internal.widget.ILockSettings;
import com.android.server.accessibility.AccessibilityManagerService;
import com.android.server.accounts.AccountManagerService;
import com.android.server.am.ActivityManagerService;
import com.android.server.clipboard.ClipboardService;
import com.android.server.content.ContentService;
import com.android.server.devicepolicy.DevicePolicyManagerService;
import com.android.server.display.DisplayManagerService;
import com.android.server.dreams.DreamManagerService;
import com.android.server.fingerprint.FingerprintService;
import com.android.server.hdmi.HdmiControlService;
import com.android.server.input.InputManagerService;
import com.android.server.job.JobSchedulerService;
import com.android.server.lights.LightsService;
import com.android.server.media.MediaRouterService;
import com.android.server.media.MediaSessionService;
import com.android.server.media.projection.MediaProjectionManagerService;
import com.android.server.net.NetworkPolicyManagerService;
import com.android.server.net.NetworkStatsService;
import com.android.server.notification.NotificationManagerService;
import com.android.server.os.SchedulingPolicyService;
import com.android.server.pm.BackgroundDexOptService;
import com.android.server.pm.Installer;
import com.android.server.pm.LauncherAppsService;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.UserManagerService;
import com.android.server.power.PowerManagerService;
import com.android.server.power.ShutdownThread;
import com.android.server.restrictions.RestrictionsManagerService;
import com.android.server.search.SearchManagerService;
import com.android.server.statusbar.StatusBarManagerService;
import com.android.server.storage.DeviceStorageMonitorService;
import com.android.server.telecom.TelecomLoaderService;
import com.android.server.trust.TrustManagerService;
import com.android.server.tv.TvInputManagerService;
import com.android.server.twilight.TwilightService;
import com.android.server.usage.UsageStatsService;
import com.android.server.wallpaper.WallpaperManagerService;
import com.android.server.webkit.WebViewUpdateService;
import com.android.server.wm.WindowManagerService;
import dalvik.system.VMRuntime;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

public final class SystemServer {
    private static final String APPWIDGET_SERVICE_CLASS = "com.android.server.appwidget.AppWidgetService";
    private static final String BACKUP_MANAGER_SERVICE_CLASS = "com.android.server.backup.BackupManagerService$Lifecycle";
    private static final long EARLIEST_SUPPORTED_TIME = 86400000;
    private static final String ENCRYPTED_STATE = "1";
    private static final String ENCRYPTING_STATE = "trigger_restart_min_framework";
    private static final String ETHERNET_SERVICE_CLASS = "com.android.server.ethernet.EthernetService";
    private static final String JOB_SCHEDULER_SERVICE_CLASS = "com.android.server.job.JobSchedulerService";
    private static final String PERSISTENT_DATA_BLOCK_PROP = "ro.frp.pst";
    private static final String PRINT_MANAGER_SERVICE_CLASS = "com.android.server.print.PrintManagerService";
    private static final long SNAPSHOT_INTERVAL = 3600000;
    private static final String TAG = "SystemServer";
    private static final String USB_SERVICE_CLASS = "com.android.server.usb.UsbService$Lifecycle";
    private static final String VOICE_RECOGNITION_MANAGER_SERVICE_CLASS = "com.android.server.voiceinteraction.VoiceInteractionManagerService";
    private static final String WIFI_P2P_SERVICE_CLASS = "com.android.server.wifi.p2p.WifiP2pService";
    private static final String WIFI_SERVICE_CLASS = "com.android.server.wifi.WifiService";
    private ActivityManagerService mActivityManagerService;
    private ContentResolver mContentResolver;
    private DisplayManagerService mDisplayManagerService;
    private final int mFactoryTestMode = FactoryTest.getMode();
    private boolean mFirstBoot;
    private boolean mOnlyCore;
    private PackageManager mPackageManager;
    private PackageManagerService mPackageManagerService;
    private PowerManagerService mPowerManagerService;
    private Timer mProfilerSnapshotTimer;
    private Context mSystemContext;
    private SystemServiceManager mSystemServiceManager;

    private static native void nativeInit();

    public static void main(String[] args) {
        new SystemServer().run();
    }

    private void run() {
        if (System.currentTimeMillis() < EARLIEST_SUPPORTED_TIME) {
            Slog.w(TAG, "System clock is before 1970; setting to 1970.");
            SystemClock.setCurrentTimeMillis(EARLIEST_SUPPORTED_TIME);
        }
        Slog.i(TAG, "Entered the Android system server!");
        EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_SYSTEM_RUN, SystemClock.uptimeMillis());
        SystemProperties.set("persist.sys.dalvik.vm.lib.2", VMRuntime.getRuntime().vmLibrary());
        if (SamplingProfilerIntegration.isEnabled()) {
            SamplingProfilerIntegration.start();
            this.mProfilerSnapshotTimer = new Timer();
            this.mProfilerSnapshotTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    SamplingProfilerIntegration.writeSnapshot("system_server", (PackageInfo) null);
                }
            }, SNAPSHOT_INTERVAL, SNAPSHOT_INTERVAL);
        }
        VMRuntime.getRuntime().clearGrowthLimit();
        VMRuntime.getRuntime().setTargetHeapUtilization(0.8f);
        Build.ensureFingerprintProperty();
        Environment.setUserRequired(true);
        BinderInternal.disableBackgroundScheduling(true);
        Process.setThreadPriority(-2);
        Process.setCanSelfBackground(false);
        Looper.prepareMainLooper();
        System.loadLibrary("android_servers");
        nativeInit();
        performPendingShutdown();
        createSystemContext();
        this.mSystemServiceManager = new SystemServiceManager(this.mSystemContext);
        LocalServices.addService(SystemServiceManager.class, this.mSystemServiceManager);
        try {
            startBootstrapServices();
            startCoreServices();
            startOtherServices();
            if (StrictMode.conditionallyEnableDebugLogging()) {
                Slog.i(TAG, "Enabled StrictMode for system server main thread.");
            }
            Looper.loop();
            throw new RuntimeException("Main thread loop unexpectedly exited");
        } catch (Throwable ex) {
            Slog.e("System", "******************************************");
            Slog.e("System", "************ Failure starting system services", ex);
            throw ex;
        }
    }

    private void reportWtf(String msg, Throwable e) {
        Slog.w(TAG, "***********************************************");
        Slog.wtf(TAG, "BOOT FAILURE " + msg, e);
    }

    private void performPendingShutdown() {
        String reason;
        String shutdownAction = SystemProperties.get(ShutdownThread.SHUTDOWN_ACTION_PROPERTY, "");
        if (shutdownAction != null && shutdownAction.length() > 0) {
            boolean reboot = shutdownAction.charAt(0) == '1';
            if (shutdownAction.length() > 1) {
                reason = shutdownAction.substring(1, shutdownAction.length());
            } else {
                reason = null;
            }
            ShutdownThread.rebootOrShutdown(reboot, reason);
        }
    }

    private void createSystemContext() {
        ActivityThread activityThread = ActivityThread.systemMain();
        this.mSystemContext = activityThread.getSystemContext();
        this.mSystemContext.setTheme(R.style.Theme.DeviceDefault.Light.DarkActionBar);
    }

    private void startBootstrapServices() {
        Installer installer = (Installer) this.mSystemServiceManager.startService(Installer.class);
        this.mActivityManagerService = ((ActivityManagerService.Lifecycle) this.mSystemServiceManager.startService(ActivityManagerService.Lifecycle.class)).getService();
        this.mActivityManagerService.setSystemServiceManager(this.mSystemServiceManager);
        this.mActivityManagerService.setInstaller(installer);
        this.mPowerManagerService = (PowerManagerService) this.mSystemServiceManager.startService(PowerManagerService.class);
        this.mActivityManagerService.initPowerManagement();
        this.mDisplayManagerService = (DisplayManagerService) this.mSystemServiceManager.startService(DisplayManagerService.class);
        this.mSystemServiceManager.startBootPhase(100);
        String cryptState = SystemProperties.get("vold.decrypt");
        if (ENCRYPTING_STATE.equals(cryptState)) {
            Slog.w(TAG, "Detected encryption in progress - only parsing core apps");
            this.mOnlyCore = true;
        } else if (ENCRYPTED_STATE.equals(cryptState)) {
            Slog.w(TAG, "Device encrypted - only parsing core apps");
            this.mOnlyCore = true;
        }
        Slog.i(TAG, "Package Manager");
        this.mPackageManagerService = PackageManagerService.main(this.mSystemContext, installer, this.mFactoryTestMode != 0, this.mOnlyCore);
        this.mFirstBoot = this.mPackageManagerService.isFirstBoot();
        this.mPackageManager = this.mSystemContext.getPackageManager();
        Slog.i(TAG, "User Service");
        ServiceManager.addService("user", UserManagerService.getInstance());
        AttributeCache.init(this.mSystemContext);
        this.mActivityManagerService.setSystemProcess();
    }

    private void startCoreServices() {
        this.mSystemServiceManager.startService(LightsService.class);
        this.mSystemServiceManager.startService(BatteryService.class);
        this.mSystemServiceManager.startService(UsageStatsService.class);
        this.mActivityManagerService.setUsageStatsManager((UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class));
        this.mPackageManagerService.getUsageStatsIfNoPackageUsageInfo();
        this.mSystemServiceManager.startService(WebViewUpdateService.class);
    }

    private void startOtherServices() throws Throwable {
        ?? r101;
        ?? r95;
        ?? r62;
        ?? r31;
        ?? r67;
        IInputMethodManager.Stub inputMethodManagerService;
        ?? r2;
        boolean zDetectSafeMode;
        ILockSettings.Stub stub;
        IStatusBarService.Stub statusBarManagerService;
        ITextServicesManager.Stub textServicesManagerService;
        ?? r6;
        INetworkPolicyManager.Stub networkPolicyManagerService;
        ?? r22;
        INetworkStatsService.Stub networkStatsService;
        INetworkScoreService.Stub networkScoreService;
        ?? r23;
        IAssetAtlas.Stub assetAtlasService;
        AudioService audioService;
        IWallpaperManager.Stub wallpaperManagerService;
        IMountService.Stub mountService;
        ITelephonyRegistry.Stub telephonyRegistry;
        IAccountManager.Stub accountManagerService;
        final Context context = this.mSystemContext;
        IAccountManager.Stub stub2 = null;
        IAccountManager.Stub stub3 = null;
        stub2 = null;
        ContentService contentServiceMain = null;
        IVibratorService.Stub stub4 = null;
        stub4 = null;
        stub4 = null;
        IAlarmManager iAlarmManagerAsInterface = null;
        ?? r73 = 0;
        IMountService.Stub stub5 = null;
        r73 = 0;
        r73 = 0;
        INetworkManagementService iNetworkManagementServiceCreate = null;
        ?? r63 = 0;
        ?? r64 = 0;
        INetworkStatsService.Stub stub6 = null;
        IConnectivityManager.Stub stub7 = null;
        INetworkScoreService.Stub stub8 = null;
        ?? Main = 0;
        Main = 0;
        Main = 0;
        Main = 0;
        Main = 0;
        Main = 0;
        NetworkTimeUpdateService networkTimeUpdateService = null;
        CommonTimeManagementService commonTimeManagementService = null;
        ?? r622 = 0;
        r622 = 0;
        r622 = 0;
        r622 = 0;
        r622 = 0;
        ITelephonyRegistry.Stub stub9 = null;
        AudioService audioService2 = null;
        boolean z = SystemProperties.getBoolean("config.disable_storage", false);
        boolean z2 = SystemProperties.getBoolean("config.disable_media", false);
        boolean z3 = SystemProperties.getBoolean("config.disable_bluetooth", false);
        SystemProperties.getBoolean("config.disable_telephony", false);
        boolean z4 = SystemProperties.getBoolean("config.disable_location", false);
        boolean z5 = SystemProperties.getBoolean("config.disable_systemui", false);
        boolean z6 = SystemProperties.getBoolean("config.disable_noncore", false);
        boolean z7 = SystemProperties.getBoolean("config.disable_network", false);
        boolean z8 = SystemProperties.getBoolean("config.disable_networktime", false);
        boolean zEquals = SystemProperties.get("ro.kernel.qemu").equals(ENCRYPTED_STATE);
        try {
            Slog.i(TAG, "Reading configuration...");
            SystemConfig.getInstance();
            Slog.i(TAG, "Scheduling Policy");
            ServiceManager.addService("scheduling_policy", new SchedulingPolicyService());
            this.mSystemServiceManager.startService(TelecomLoaderService.class);
            Slog.i(TAG, "Telephony Registry");
            telephonyRegistry = new TelephonyRegistry(context);
            try {
                ServiceManager.addService("telephony.registry", telephonyRegistry);
                Slog.i(TAG, "Entropy Mixer");
                ServiceManager.addService("entropy", new EntropyMixer(context));
                this.mContentResolver = context.getContentResolver();
                try {
                    Slog.i(TAG, "Account Manager");
                    accountManagerService = new AccountManagerService(context);
                } catch (Throwable th) {
                    th = th;
                }
            } catch (RuntimeException e) {
                e = e;
                stub9 = telephonyRegistry;
            }
        } catch (RuntimeException e2) {
            e = e2;
        }
        try {
            ServiceManager.addService("account", accountManagerService);
            stub2 = accountManagerService;
        } catch (RuntimeException e3) {
            e = e3;
            stub9 = telephonyRegistry;
            stub2 = accountManagerService;
            Slog.e("System", "******************************************");
            Slog.e("System", "************ Failure starting core service", e);
            r31 = stub2;
            r62 = r622;
            r95 = stub4;
            r101 = Main;
            IStatusBarService.Stub stub10 = null;
            IInputMethodManager.Stub stub11 = null;
            IWallpaperManager.Stub stub12 = null;
            ILocationManager.Stub stub13 = null;
            ICountryDetector.Stub stub14 = null;
            ITextServicesManager.Stub stub15 = null;
            r67 = 0;
            ILockSettings.Stub stub16 = null;
            ILockSettings.Stub stub17 = null;
            IAssetAtlas.Stub stub18 = null;
            IMediaRouterService.Stub stub19 = null;
            if (this.mFactoryTestMode != 1) {
            }
            r101.displayReady();
            if (this.mFactoryTestMode != 1) {
            }
            this.mPackageManagerService.performBootDexOpt();
            ActivityManagerNative.getDefault().showBootMessage(context.getResources().getText(R.string.hearing_device_switch_hearing_mic_notification_text), false);
            if (this.mFactoryTestMode == 1) {
            }
            if (!z6) {
            }
            Slog.i(TAG, "Benesse Extension Service");
            ServiceManager.addService("benesse_extension", new BenesseExtensionService(context));
            zDetectSafeMode = r101.detectSafeMode();
            if (zDetectSafeMode) {
            }
            final MmsServiceBroker mmsServiceBroker = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
            r95.systemReady();
            if (r67 != 0) {
            }
            this.mSystemServiceManager.startBootPhase(SystemService.PHASE_LOCK_SETTINGS_READY);
            this.mSystemServiceManager.startBootPhase(SystemService.PHASE_SYSTEM_SERVICES_READY);
            r101.systemReady();
            if (zDetectSafeMode) {
            }
            Configuration configurationComputeNewConfiguration = r101.computeNewConfiguration();
            DisplayMetrics displayMetrics = new DisplayMetrics();
            ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(displayMetrics);
            context.getResources().updateConfiguration(configurationComputeNewConfiguration, displayMetrics);
            this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
            this.mPackageManagerService.systemReady();
            this.mDisplayManagerService.systemReady(zDetectSafeMode, this.mOnlyCore);
            final ?? r11 = r73;
            final INetworkManagementService iNetworkManagementService = iNetworkManagementServiceCreate;
            final ?? r14 = r63;
            final ?? r15 = r2;
            final ?? r16 = stub7;
            final ?? r12 = stub8;
            final ?? r18 = stub12;
            final ?? r19 = stub11;
            final ?? r21 = stub13;
            final ?? r222 = stub14;
            final NetworkTimeUpdateService networkTimeUpdateService2 = networkTimeUpdateService;
            final CommonTimeManagementService commonTimeManagementService2 = commonTimeManagementService;
            final ?? r25 = stub15;
            final ?? r20 = stub10;
            final ?? r26 = stub18;
            final ?? r27 = r62;
            final ?? r28 = stub9;
            final ?? r29 = stub19;
            final AudioService audioService3 = audioService2;
            this.mActivityManagerService.systemReady(new Runnable() {
                @Override
                public void run() {
                    Slog.i(SystemServer.TAG, "Making services ready");
                    SystemServer.this.mSystemServiceManager.startBootPhase(SystemService.PHASE_ACTIVITY_MANAGER_READY);
                    try {
                        SystemServer.this.mActivityManagerService.startObservingNativeCrashes();
                    } catch (Throwable e4) {
                        SystemServer.this.reportWtf("observing native crashes", e4);
                    }
                    Slog.i(SystemServer.TAG, "WebViewFactory preparation");
                    WebViewFactory.prepareWebViewInSystemServer();
                    try {
                        SystemServer.startSystemUi(context);
                    } catch (Throwable e5) {
                        SystemServer.this.reportWtf("starting System UI", e5);
                    }
                    try {
                        if (r11 != null) {
                            r11.systemReady();
                        }
                    } catch (Throwable e6) {
                        SystemServer.this.reportWtf("making Mount Service ready", e6);
                    }
                    try {
                        if (r12 != null) {
                            r12.systemReady();
                        }
                    } catch (Throwable e7) {
                        SystemServer.this.reportWtf("making Network Score Service ready", e7);
                    }
                    try {
                        if (iNetworkManagementService != null) {
                            iNetworkManagementService.systemReady();
                        }
                    } catch (Throwable e8) {
                        SystemServer.this.reportWtf("making Network Managment Service ready", e8);
                    }
                    try {
                        if (r14 != null) {
                            r14.systemReady();
                        }
                    } catch (Throwable e9) {
                        SystemServer.this.reportWtf("making Network Stats Service ready", e9);
                    }
                    try {
                        if (r15 != null) {
                            r15.systemReady();
                        }
                    } catch (Throwable e10) {
                        SystemServer.this.reportWtf("making Network Policy Service ready", e10);
                    }
                    try {
                        if (r16 != null) {
                            r16.systemReady();
                        }
                    } catch (Throwable e11) {
                        SystemServer.this.reportWtf("making Connectivity Service ready", e11);
                    }
                    try {
                        if (audioService3 != null) {
                            audioService3.systemReady();
                        }
                    } catch (Throwable e12) {
                        SystemServer.this.reportWtf("Notifying AudioService running", e12);
                    }
                    Watchdog.getInstance().start();
                    SystemServer.this.mSystemServiceManager.startBootPhase(600);
                    try {
                        if (r18 != null) {
                            r18.systemRunning();
                        }
                    } catch (Throwable e13) {
                        SystemServer.this.reportWtf("Notifying WallpaperService running", e13);
                    }
                    try {
                        if (r19 != null) {
                            r19.systemRunning(r20);
                        }
                    } catch (Throwable e14) {
                        SystemServer.this.reportWtf("Notifying InputMethodService running", e14);
                    }
                    try {
                        if (r21 != null) {
                            r21.systemRunning();
                        }
                    } catch (Throwable e15) {
                        SystemServer.this.reportWtf("Notifying Location Service running", e15);
                    }
                    try {
                        if (r222 != null) {
                            r222.systemRunning();
                        }
                    } catch (Throwable e16) {
                        SystemServer.this.reportWtf("Notifying CountryDetectorService running", e16);
                    }
                    try {
                        if (networkTimeUpdateService2 != null) {
                            networkTimeUpdateService2.systemRunning();
                        }
                    } catch (Throwable e17) {
                        SystemServer.this.reportWtf("Notifying NetworkTimeService running", e17);
                    }
                    try {
                        if (commonTimeManagementService2 != null) {
                            commonTimeManagementService2.systemRunning();
                        }
                    } catch (Throwable e18) {
                        SystemServer.this.reportWtf("Notifying CommonTimeManagementService running", e18);
                    }
                    try {
                        if (r25 != null) {
                            r25.systemRunning();
                        }
                    } catch (Throwable e19) {
                        SystemServer.this.reportWtf("Notifying TextServicesManagerService running", e19);
                    }
                    try {
                        if (r26 != null) {
                            r26.systemRunning();
                        }
                    } catch (Throwable e20) {
                        SystemServer.this.reportWtf("Notifying AssetAtlasService running", e20);
                    }
                    try {
                        if (r27 != null) {
                            r27.systemRunning();
                        }
                    } catch (Throwable e21) {
                        SystemServer.this.reportWtf("Notifying InputManagerService running", e21);
                    }
                    try {
                        if (r28 != null) {
                            r28.systemRunning();
                        }
                    } catch (Throwable e22) {
                        SystemServer.this.reportWtf("Notifying TelephonyRegistry running", e22);
                    }
                    try {
                        if (r29 != null) {
                            r29.systemRunning();
                        }
                    } catch (Throwable e23) {
                        SystemServer.this.reportWtf("Notifying MediaRouterService running", e23);
                    }
                    try {
                        if (mmsServiceBroker != null) {
                            mmsServiceBroker.systemRunning();
                        }
                    } catch (Throwable e24) {
                        SystemServer.this.reportWtf("Notifying MmsService running", e24);
                    }
                }
            });
        } catch (Throwable th2) {
            th = th2;
            stub3 = accountManagerService;
            Slog.e(TAG, "Failure starting Account Manager", th);
            stub2 = stub3;
        }
        Slog.i(TAG, "Content Manager");
        contentServiceMain = ContentService.main(context, this.mFactoryTestMode == 1);
        Slog.i(TAG, "System Content Providers");
        this.mActivityManagerService.installSystemProviders();
        Slog.i(TAG, "Vibrator Service");
        IVibratorService.Stub vibratorService = new VibratorService(context);
        ServiceManager.addService("vibrator", vibratorService);
        Slog.i(TAG, "Consumer IR Service");
        ServiceManager.addService("consumer_ir", new ConsumerIrService(context));
        this.mSystemServiceManager.startService(AlarmManagerService.class);
        iAlarmManagerAsInterface = IAlarmManager.Stub.asInterface(ServiceManager.getService("alarm"));
        Slog.i(TAG, "Init Watchdog");
        Watchdog.getInstance().init(context, this.mActivityManagerService);
        Slog.i(TAG, "Input Manager");
        ?? inputManagerService = new InputManagerService(context);
        Slog.i(TAG, "Window Manager");
        Main = WindowManagerService.main(context, inputManagerService, this.mFactoryTestMode != 1, !this.mFirstBoot, this.mOnlyCore);
        ServiceManager.addService("window", (IBinder) Main);
        ServiceManager.addService("input", (IBinder) inputManagerService);
        this.mActivityManagerService.setWindowManager(Main);
        inputManagerService.setWindowManagerCallbacks(Main.getInputMonitor());
        inputManagerService.start();
        this.mDisplayManagerService.windowManagerAndInputReady();
        if (zEquals) {
            Slog.i(TAG, "No Bluetooh Service (emulator)");
        } else if (this.mFactoryTestMode == 1) {
            Slog.i(TAG, "No Bluetooth Service (factory test)");
        } else if (!context.getPackageManager().hasSystemFeature("android.hardware.bluetooth")) {
            Slog.i(TAG, "No Bluetooth Service (Bluetooth Hardware Not Present)");
        } else if (z3) {
            Slog.i(TAG, "Bluetooth Service disabled by config");
        } else {
            Slog.i(TAG, "Bluetooth Manager Service");
            try {
                ServiceManager.addService("bluetooth_manager", new BluetoothManagerService(context));
            } catch (RuntimeException e4) {
                e = e4;
                stub9 = telephonyRegistry;
                r622 = inputManagerService;
                stub4 = vibratorService;
                Slog.e("System", "******************************************");
                Slog.e("System", "************ Failure starting core service", e);
                r31 = stub2;
                r62 = r622;
                r95 = stub4;
                r101 = Main;
                IStatusBarService.Stub stub102 = null;
                IInputMethodManager.Stub stub112 = null;
                IWallpaperManager.Stub stub122 = null;
                ILocationManager.Stub stub132 = null;
                ICountryDetector.Stub stub142 = null;
                ITextServicesManager.Stub stub152 = null;
                r67 = 0;
                ILockSettings.Stub stub162 = null;
                ILockSettings.Stub stub172 = null;
                IAssetAtlas.Stub stub182 = null;
                IMediaRouterService.Stub stub192 = null;
                if (this.mFactoryTestMode != 1) {
                }
                r101.displayReady();
                if (this.mFactoryTestMode != 1) {
                    try {
                        Slog.i(TAG, "Mount Service");
                        mountService = new MountService(context);
                        try {
                            ServiceManager.addService("mount", mountService);
                            r73 = mountService;
                        } catch (Throwable th3) {
                            th = th3;
                            stub5 = mountService;
                            reportWtf("starting Mount Service", th);
                            r73 = stub5;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                    }
                }
                this.mPackageManagerService.performBootDexOpt();
                ActivityManagerNative.getDefault().showBootMessage(context.getResources().getText(R.string.hearing_device_switch_hearing_mic_notification_text), false);
                if (this.mFactoryTestMode == 1) {
                }
                if (!z6) {
                }
                Slog.i(TAG, "Benesse Extension Service");
                ServiceManager.addService("benesse_extension", new BenesseExtensionService(context));
                zDetectSafeMode = r101.detectSafeMode();
                if (zDetectSafeMode) {
                }
                final MmsServiceBroker mmsServiceBroker2 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                r95.systemReady();
                if (r67 != 0) {
                }
                this.mSystemServiceManager.startBootPhase(SystemService.PHASE_LOCK_SETTINGS_READY);
                this.mSystemServiceManager.startBootPhase(SystemService.PHASE_SYSTEM_SERVICES_READY);
                r101.systemReady();
                if (zDetectSafeMode) {
                }
                Configuration configurationComputeNewConfiguration2 = r101.computeNewConfiguration();
                DisplayMetrics displayMetrics2 = new DisplayMetrics();
                ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(displayMetrics2);
                context.getResources().updateConfiguration(configurationComputeNewConfiguration2, displayMetrics2);
                this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                this.mPackageManagerService.systemReady();
                this.mDisplayManagerService.systemReady(zDetectSafeMode, this.mOnlyCore);
                final MountService r112 = r73;
                final NetworkManagementService iNetworkManagementService2 = iNetworkManagementServiceCreate;
                final NetworkStatsService r142 = r63;
                final NetworkPolicyManagerService r152 = r2;
                final ConnectivityService r162 = stub7;
                final NetworkScoreService r122 = stub8;
                final WallpaperManagerService r182 = stub122;
                final InputMethodManagerService r192 = stub112;
                final LocationManagerService r212 = stub132;
                final CountryDetectorService r2222 = stub142;
                final NetworkTimeUpdateService networkTimeUpdateService22 = networkTimeUpdateService;
                final CommonTimeManagementService commonTimeManagementService22 = commonTimeManagementService;
                final TextServicesManagerService r252 = stub152;
                final StatusBarManagerService r202 = stub102;
                final AssetAtlasService r262 = stub182;
                final InputManagerService r272 = r62;
                final TelephonyRegistry r282 = stub9;
                final MediaRouterService r292 = stub192;
                final AudioService audioService32 = audioService2;
                this.mActivityManagerService.systemReady(new Runnable() {
                    @Override
                    public void run() {
                        Slog.i(SystemServer.TAG, "Making services ready");
                        SystemServer.this.mSystemServiceManager.startBootPhase(SystemService.PHASE_ACTIVITY_MANAGER_READY);
                        try {
                            SystemServer.this.mActivityManagerService.startObservingNativeCrashes();
                        } catch (Throwable e42) {
                            SystemServer.this.reportWtf("observing native crashes", e42);
                        }
                        Slog.i(SystemServer.TAG, "WebViewFactory preparation");
                        WebViewFactory.prepareWebViewInSystemServer();
                        try {
                            SystemServer.startSystemUi(context);
                        } catch (Throwable e5) {
                            SystemServer.this.reportWtf("starting System UI", e5);
                        }
                        try {
                            if (r112 != null) {
                                r112.systemReady();
                            }
                        } catch (Throwable e6) {
                            SystemServer.this.reportWtf("making Mount Service ready", e6);
                        }
                        try {
                            if (r122 != null) {
                                r122.systemReady();
                            }
                        } catch (Throwable e7) {
                            SystemServer.this.reportWtf("making Network Score Service ready", e7);
                        }
                        try {
                            if (iNetworkManagementService2 != null) {
                                iNetworkManagementService2.systemReady();
                            }
                        } catch (Throwable e8) {
                            SystemServer.this.reportWtf("making Network Managment Service ready", e8);
                        }
                        try {
                            if (r142 != null) {
                                r142.systemReady();
                            }
                        } catch (Throwable e9) {
                            SystemServer.this.reportWtf("making Network Stats Service ready", e9);
                        }
                        try {
                            if (r152 != null) {
                                r152.systemReady();
                            }
                        } catch (Throwable e10) {
                            SystemServer.this.reportWtf("making Network Policy Service ready", e10);
                        }
                        try {
                            if (r162 != null) {
                                r162.systemReady();
                            }
                        } catch (Throwable e11) {
                            SystemServer.this.reportWtf("making Connectivity Service ready", e11);
                        }
                        try {
                            if (audioService32 != null) {
                                audioService32.systemReady();
                            }
                        } catch (Throwable e12) {
                            SystemServer.this.reportWtf("Notifying AudioService running", e12);
                        }
                        Watchdog.getInstance().start();
                        SystemServer.this.mSystemServiceManager.startBootPhase(600);
                        try {
                            if (r182 != null) {
                                r182.systemRunning();
                            }
                        } catch (Throwable e13) {
                            SystemServer.this.reportWtf("Notifying WallpaperService running", e13);
                        }
                        try {
                            if (r192 != null) {
                                r192.systemRunning(r202);
                            }
                        } catch (Throwable e14) {
                            SystemServer.this.reportWtf("Notifying InputMethodService running", e14);
                        }
                        try {
                            if (r212 != null) {
                                r212.systemRunning();
                            }
                        } catch (Throwable e15) {
                            SystemServer.this.reportWtf("Notifying Location Service running", e15);
                        }
                        try {
                            if (r2222 != null) {
                                r2222.systemRunning();
                            }
                        } catch (Throwable e16) {
                            SystemServer.this.reportWtf("Notifying CountryDetectorService running", e16);
                        }
                        try {
                            if (networkTimeUpdateService22 != null) {
                                networkTimeUpdateService22.systemRunning();
                            }
                        } catch (Throwable e17) {
                            SystemServer.this.reportWtf("Notifying NetworkTimeService running", e17);
                        }
                        try {
                            if (commonTimeManagementService22 != null) {
                                commonTimeManagementService22.systemRunning();
                            }
                        } catch (Throwable e18) {
                            SystemServer.this.reportWtf("Notifying CommonTimeManagementService running", e18);
                        }
                        try {
                            if (r252 != null) {
                                r252.systemRunning();
                            }
                        } catch (Throwable e19) {
                            SystemServer.this.reportWtf("Notifying TextServicesManagerService running", e19);
                        }
                        try {
                            if (r262 != null) {
                                r262.systemRunning();
                            }
                        } catch (Throwable e20) {
                            SystemServer.this.reportWtf("Notifying AssetAtlasService running", e20);
                        }
                        try {
                            if (r272 != null) {
                                r272.systemRunning();
                            }
                        } catch (Throwable e21) {
                            SystemServer.this.reportWtf("Notifying InputManagerService running", e21);
                        }
                        try {
                            if (r282 != null) {
                                r282.systemRunning();
                            }
                        } catch (Throwable e22) {
                            SystemServer.this.reportWtf("Notifying TelephonyRegistry running", e22);
                        }
                        try {
                            if (r292 != null) {
                                r292.systemRunning();
                            }
                        } catch (Throwable e23) {
                            SystemServer.this.reportWtf("Notifying MediaRouterService running", e23);
                        }
                        try {
                            if (mmsServiceBroker2 != null) {
                                mmsServiceBroker2.systemRunning();
                            }
                        } catch (Throwable e24) {
                            SystemServer.this.reportWtf("Notifying MmsService running", e24);
                        }
                    }
                });
            }
        }
        Slog.i(TAG, "Pen Calibration Service");
        ServiceManager.addService("pen_calibration", new PenCalibrationService());
        stub9 = telephonyRegistry;
        r62 = inputManagerService;
        r95 = vibratorService;
        r31 = stub2;
        r101 = Main;
        IStatusBarService.Stub stub1022 = null;
        IInputMethodManager.Stub stub1122 = null;
        IWallpaperManager.Stub stub1222 = null;
        ILocationManager.Stub stub1322 = null;
        ICountryDetector.Stub stub1422 = null;
        ITextServicesManager.Stub stub1522 = null;
        r67 = 0;
        ILockSettings.Stub stub1622 = null;
        ILockSettings.Stub stub1722 = null;
        IAssetAtlas.Stub stub1822 = null;
        IMediaRouterService.Stub stub1922 = null;
        if (this.mFactoryTestMode != 1) {
            try {
                Slog.i(TAG, "Input Method Service");
                inputMethodManagerService = new InputMethodManagerService(context, r101);
            } catch (Throwable th5) {
                th = th5;
            }
            try {
                ServiceManager.addService("input_method", inputMethodManagerService);
                stub1122 = inputMethodManagerService;
            } catch (Throwable th6) {
                th = th6;
                stub1122 = inputMethodManagerService;
                reportWtf("starting Input Manager Service", th);
            }
            try {
                Slog.i(TAG, "Accessibility Manager");
                ServiceManager.addService("accessibility", new AccessibilityManagerService(context));
            } catch (Throwable th7) {
                reportWtf("starting Accessibility Manager", th7);
            }
        }
        r101.displayReady();
        if (this.mFactoryTestMode != 1 && !z && !"0".equals(SystemProperties.get("system_init.startmountservice"))) {
            Slog.i(TAG, "Mount Service");
            mountService = new MountService(context);
            ServiceManager.addService("mount", mountService);
            r73 = mountService;
        }
        this.mPackageManagerService.performBootDexOpt();
        ActivityManagerNative.getDefault().showBootMessage(context.getResources().getText(R.string.hearing_device_switch_hearing_mic_notification_text), false);
        if (this.mFactoryTestMode == 1) {
            r2 = 0;
        } else {
            if (!z6) {
                try {
                    Slog.i(TAG, "LockSettingsService");
                    ILockSettings.Stub lockSettingsService = new LockSettingsService(context);
                    try {
                        ServiceManager.addService("lock_settings", lockSettingsService);
                        stub = lockSettingsService;
                    } catch (Throwable th8) {
                        th = th8;
                        stub1622 = lockSettingsService;
                        reportWtf("starting LockSettingsService service", th);
                        stub = stub1622;
                    }
                } catch (Throwable th9) {
                    th = th9;
                }
                if (!SystemProperties.get(PERSISTENT_DATA_BLOCK_PROP).equals("")) {
                    this.mSystemServiceManager.startService(PersistentDataBlockService.class);
                }
                this.mSystemServiceManager.startService(DevicePolicyManagerService.Lifecycle.class);
                stub1722 = stub;
            }
            if (!z5) {
                try {
                    Slog.i(TAG, "Status Bar");
                    statusBarManagerService = new StatusBarManagerService(context, r101);
                } catch (Throwable th10) {
                    th = th10;
                }
                try {
                    ServiceManager.addService("statusbar", statusBarManagerService);
                    stub1022 = statusBarManagerService;
                } catch (Throwable th11) {
                    th = th11;
                    stub1022 = statusBarManagerService;
                    reportWtf("starting StatusBarManagerService", th);
                }
            }
            if (!z6) {
                try {
                    Slog.i(TAG, "Clipboard Service");
                    ServiceManager.addService("clipboard", new ClipboardService(context));
                } catch (Throwable th12) {
                    reportWtf("starting Clipboard Service", th12);
                }
            }
            if (!z7) {
                try {
                    Slog.i(TAG, "NetworkManagement Service");
                    iNetworkManagementServiceCreate = NetworkManagementService.create(context);
                    ServiceManager.addService("network_management", iNetworkManagementServiceCreate);
                } catch (Throwable th13) {
                    reportWtf("starting NetworkManagement Service", th13);
                }
            }
            if (!z6) {
                try {
                    Slog.i(TAG, "Text Service Manager Service");
                    textServicesManagerService = new TextServicesManagerService(context);
                } catch (Throwable th14) {
                    th = th14;
                }
                try {
                    ServiceManager.addService("textservices", textServicesManagerService);
                    stub1522 = textServicesManagerService;
                } catch (Throwable th15) {
                    th = th15;
                    stub1522 = textServicesManagerService;
                    reportWtf("starting Text Service Manager Service", th);
                }
            }
            if (z7) {
                r23 = 0;
            } else {
                try {
                    Slog.i(TAG, "Network Score Service");
                    networkScoreService = new NetworkScoreService(context);
                } catch (Throwable th16) {
                    th = th16;
                }
                try {
                    ServiceManager.addService("network_score", networkScoreService);
                    stub8 = networkScoreService;
                } catch (Throwable th17) {
                    th = th17;
                    stub8 = networkScoreService;
                    reportWtf("starting Network Score Service", th);
                }
                try {
                    Slog.i(TAG, "NetworkStats Service");
                    networkStatsService = new NetworkStatsService(context, iNetworkManagementServiceCreate, iAlarmManagerAsInterface);
                } catch (Throwable th18) {
                    th = th18;
                }
                try {
                    ServiceManager.addService("netstats", networkStatsService);
                    r6 = networkStatsService;
                } catch (Throwable th19) {
                    th = th19;
                    stub6 = networkStatsService;
                    reportWtf("starting NetworkStats Service", th);
                    r6 = stub6;
                }
                try {
                    Slog.i(TAG, "NetworkPolicy Service");
                    networkPolicyManagerService = new NetworkPolicyManagerService(context, this.mActivityManagerService, ServiceManager.getService("power"), r6, iNetworkManagementServiceCreate);
                } catch (Throwable th20) {
                    th = th20;
                    networkPolicyManagerService = null;
                }
                try {
                    ServiceManager.addService("netpolicy", networkPolicyManagerService);
                    r22 = networkPolicyManagerService;
                } catch (Throwable th21) {
                    th = th21;
                    reportWtf("starting NetworkPolicy Service", th);
                    r22 = networkPolicyManagerService;
                }
                this.mSystemServiceManager.startService(WIFI_P2P_SERVICE_CLASS);
                this.mSystemServiceManager.startService(WIFI_SERVICE_CLASS);
                this.mSystemServiceManager.startService("com.android.server.wifi.WifiScanningService");
                this.mSystemServiceManager.startService("com.android.server.wifi.RttService");
                this.mSystemServiceManager.startService(ETHERNET_SERVICE_CLASS);
                try {
                    Slog.i(TAG, "Connectivity Service");
                    IConnectivityManager.Stub connectivityService = new ConnectivityService(context, iNetworkManagementServiceCreate, r6, r22);
                    try {
                        ServiceManager.addService("connectivity", connectivityService);
                        r6.bindConnectivityManager(connectivityService);
                        r22.bindConnectivityManager(connectivityService);
                        stub7 = connectivityService;
                    } catch (Throwable th22) {
                        th = th22;
                        stub7 = connectivityService;
                        reportWtf("starting Connectivity Service", th);
                    }
                } catch (Throwable th23) {
                    th = th23;
                }
                try {
                    Slog.i(TAG, "Network Service Discovery Service");
                    ServiceManager.addService("servicediscovery", NsdService.create(context));
                    r23 = r22;
                    r64 = r6;
                } catch (Throwable th24) {
                    reportWtf("starting Service Discovery Service", th24);
                    r23 = r22;
                    r64 = r6;
                }
            }
            if (!z6) {
                try {
                    Slog.i(TAG, "UpdateLock Service");
                    ServiceManager.addService("updatelock", new UpdateLockService(context));
                } catch (Throwable th25) {
                    reportWtf("starting UpdateLockService", th25);
                }
            }
            if (r73 != 0 && !this.mOnlyCore) {
                r73.waitForAsecScan();
            }
            if (r31 != 0) {
                try {
                    r31.systemReady();
                } catch (Throwable th26) {
                    reportWtf("making Account Manager Service ready", th26);
                }
            }
            if (contentServiceMain != null) {
                try {
                    contentServiceMain.systemReady();
                } catch (Throwable th27) {
                    reportWtf("making Content Service ready", th27);
                }
            }
            this.mSystemServiceManager.startService(NotificationManagerService.class);
            r23.bindNotificationManager(INotificationManager.Stub.asInterface(ServiceManager.getService("notification")));
            this.mSystemServiceManager.startService(DeviceStorageMonitorService.class);
            if (!z4) {
                try {
                    Slog.i(TAG, "Location Manager");
                    ILocationManager.Stub locationManagerService = new LocationManagerService(context);
                    try {
                        ServiceManager.addService("location", locationManagerService);
                        stub1322 = locationManagerService;
                    } catch (Throwable th28) {
                        th = th28;
                        stub1322 = locationManagerService;
                        reportWtf("starting Location Manager", th);
                    }
                } catch (Throwable th29) {
                    th = th29;
                }
                try {
                    Slog.i(TAG, "Country Detector");
                    ICountryDetector.Stub countryDetectorService = new CountryDetectorService(context);
                    try {
                        ServiceManager.addService("country_detector", countryDetectorService);
                        stub1422 = countryDetectorService;
                    } catch (Throwable th30) {
                        th = th30;
                        stub1422 = countryDetectorService;
                        reportWtf("starting Country Detector", th);
                    }
                } catch (Throwable th31) {
                    th = th31;
                }
            }
            if (!z6) {
                try {
                    Slog.i(TAG, "Search Service");
                    ServiceManager.addService("search", new SearchManagerService(context));
                } catch (Throwable th32) {
                    reportWtf("starting Search Service", th32);
                }
            }
            try {
                Slog.i(TAG, "DropBox Service");
                ServiceManager.addService("dropbox", new DropBoxManagerService(context, new File("/data/system/dropbox")));
            } catch (Throwable th33) {
                reportWtf("starting DropBoxManagerService", th33);
            }
            if (!z6 && context.getResources().getBoolean(R.^attr-private.dreamActivityOpenExitAnimation) && !this.mOnlyCore) {
                try {
                    Slog.i(TAG, "Wallpaper Service");
                    wallpaperManagerService = new WallpaperManagerService(context);
                } catch (Throwable th34) {
                    th = th34;
                }
                try {
                    ServiceManager.addService("wallpaper", wallpaperManagerService);
                    stub1222 = wallpaperManagerService;
                } catch (Throwable th35) {
                    th = th35;
                    stub1222 = wallpaperManagerService;
                    reportWtf("starting Wallpaper Service", th);
                }
            }
            if (!z2 && !"0".equals(SystemProperties.get("system_init.startaudioservice"))) {
                try {
                    Slog.i(TAG, "Audio Service");
                    audioService = new AudioService(context);
                } catch (Throwable th36) {
                    th = th36;
                }
                try {
                    ServiceManager.addService("audio", audioService);
                    audioService2 = audioService;
                } catch (Throwable th37) {
                    th = th37;
                    audioService2 = audioService;
                    reportWtf("starting Audio Service", th);
                }
            }
            if (!z6) {
                this.mSystemServiceManager.startService(DockObserver.class);
            }
            if (!z2) {
                try {
                    Slog.i(TAG, "Wired Accessory Manager");
                    r62.setWiredAccessoryCallbacks(new WiredAccessoryManager(context, r62));
                } catch (Throwable th38) {
                    reportWtf("starting WiredAccessoryManager", th38);
                }
            }
            if (!z6) {
                if (this.mPackageManager.hasSystemFeature("android.hardware.usb.host") || this.mPackageManager.hasSystemFeature("android.hardware.usb.accessory")) {
                    this.mSystemServiceManager.startService(USB_SERVICE_CLASS);
                }
                try {
                    Slog.i(TAG, "Serial Service");
                    try {
                        ServiceManager.addService("serial", new SerialService(context));
                    } catch (Throwable th39) {
                        th = th39;
                        Slog.e(TAG, "Failure starting SerialService", th);
                    }
                } catch (Throwable th40) {
                    th = th40;
                }
            }
            this.mSystemServiceManager.startService(TwilightService.class);
            this.mSystemServiceManager.startService(UiModeManagerService.class);
            this.mSystemServiceManager.startService(JobSchedulerService.class);
            if (!z6) {
                if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                    this.mSystemServiceManager.startService(BACKUP_MANAGER_SERVICE_CLASS);
                }
                if (this.mPackageManager.hasSystemFeature("android.software.app_widgets")) {
                    this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                }
                if (this.mPackageManager.hasSystemFeature("android.software.voice_recognizers")) {
                    this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                }
            }
            try {
                Slog.i(TAG, "DiskStats Service");
                ServiceManager.addService("diskstats", new DiskStatsService(context));
            } catch (Throwable th41) {
                reportWtf("starting DiskStats Service", th41);
            }
            try {
                Slog.i(TAG, "SamplingProfiler Service");
                ServiceManager.addService("samplingprofiler", new SamplingProfilerService(context));
            } catch (Throwable th42) {
                reportWtf("starting SamplingProfiler Service", th42);
            }
            if (!z7 && !z8) {
                try {
                    Slog.i(TAG, "NetworkTimeUpdateService");
                    networkTimeUpdateService = new NetworkTimeUpdateService(context);
                } catch (Throwable th43) {
                    reportWtf("starting NetworkTimeUpdate service", th43);
                }
            }
            if (!z2) {
                try {
                    Slog.i(TAG, "CommonTimeManagementService");
                    CommonTimeManagementService commonTimeManagementService3 = new CommonTimeManagementService(context);
                    try {
                        ServiceManager.addService("commontime_management", commonTimeManagementService3);
                        commonTimeManagementService = commonTimeManagementService3;
                    } catch (Throwable th44) {
                        th = th44;
                        commonTimeManagementService = commonTimeManagementService3;
                        reportWtf("starting CommonTimeManagementService service", th);
                    }
                } catch (Throwable th45) {
                    th = th45;
                }
            }
            if (!z7) {
                try {
                    Slog.i(TAG, "CertBlacklister");
                    new CertBlacklister(context);
                } catch (Throwable th46) {
                    reportWtf("starting CertBlacklister", th46);
                }
            }
            if (!z6) {
                this.mSystemServiceManager.startService(DreamManagerService.class);
            }
            if (!z6) {
                try {
                    Slog.i(TAG, "Assets Atlas Service");
                    assetAtlasService = new AssetAtlasService(context);
                } catch (Throwable th47) {
                    th = th47;
                }
                try {
                    ServiceManager.addService(AssetAtlasService.ASSET_ATLAS_SERVICE, assetAtlasService);
                    stub1822 = assetAtlasService;
                } catch (Throwable th48) {
                    th = th48;
                    stub1822 = assetAtlasService;
                    reportWtf("starting AssetAtlasService", th);
                }
            }
            if (this.mPackageManager.hasSystemFeature("android.software.print")) {
                this.mSystemServiceManager.startService(PRINT_MANAGER_SERVICE_CLASS);
            }
            this.mSystemServiceManager.startService(RestrictionsManagerService.class);
            this.mSystemServiceManager.startService(MediaSessionService.class);
            if (this.mPackageManager.hasSystemFeature("android.hardware.hdmi.cec")) {
                this.mSystemServiceManager.startService(HdmiControlService.class);
            }
            if (this.mPackageManager.hasSystemFeature("android.software.live_tv")) {
                this.mSystemServiceManager.startService(TvInputManagerService.class);
            }
            if (!z6) {
                try {
                    Slog.i(TAG, "Media Router Service");
                    IMediaRouterService.Stub mediaRouterService = new MediaRouterService(context);
                    try {
                        ServiceManager.addService("media_router", mediaRouterService);
                        stub1922 = mediaRouterService;
                    } catch (Throwable th49) {
                        th = th49;
                        stub1922 = mediaRouterService;
                        reportWtf("starting MediaRouterService", th);
                    }
                } catch (Throwable th50) {
                    th = th50;
                }
                this.mSystemServiceManager.startService(TrustManagerService.class);
                this.mSystemServiceManager.startService(FingerprintService.class);
                try {
                    Slog.i(TAG, "BackgroundDexOptService");
                    BackgroundDexOptService.schedule(context);
                } catch (Throwable th51) {
                    reportWtf("starting BackgroundDexOptService", th51);
                }
            }
            this.mSystemServiceManager.startService(LauncherAppsService.class);
            r2 = r23;
            r63 = r64;
            r67 = stub1722;
        }
        if (!z6) {
            this.mSystemServiceManager.startService(MediaProjectionManagerService.class);
        }
        Slog.i(TAG, "Benesse Extension Service");
        ServiceManager.addService("benesse_extension", new BenesseExtensionService(context));
        zDetectSafeMode = r101.detectSafeMode();
        if (zDetectSafeMode) {
            this.mActivityManagerService.enterSafeMode();
            VMRuntime.getRuntime().disableJitCompilation();
        } else {
            VMRuntime.getRuntime().startJitCompilation();
        }
        final MmsServiceBroker mmsServiceBroker22 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
        r95.systemReady();
        if (r67 != 0) {
            try {
                r67.systemReady();
            } catch (Throwable th52) {
                reportWtf("making Lock Settings Service ready", th52);
            }
        }
        this.mSystemServiceManager.startBootPhase(SystemService.PHASE_LOCK_SETTINGS_READY);
        this.mSystemServiceManager.startBootPhase(SystemService.PHASE_SYSTEM_SERVICES_READY);
        r101.systemReady();
        if (zDetectSafeMode) {
            this.mActivityManagerService.showSafeModeOverlay();
        }
        Configuration configurationComputeNewConfiguration22 = r101.computeNewConfiguration();
        DisplayMetrics displayMetrics22 = new DisplayMetrics();
        ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(displayMetrics22);
        context.getResources().updateConfiguration(configurationComputeNewConfiguration22, displayMetrics22);
        this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
        this.mPackageManagerService.systemReady();
        this.mDisplayManagerService.systemReady(zDetectSafeMode, this.mOnlyCore);
        final MountService r1122 = r73;
        final NetworkManagementService iNetworkManagementService22 = iNetworkManagementServiceCreate;
        final NetworkStatsService r1422 = r63;
        final NetworkPolicyManagerService r1522 = r2;
        final ConnectivityService r1622 = stub7;
        final NetworkScoreService r1222 = stub8;
        final WallpaperManagerService r1822 = stub1222;
        final InputMethodManagerService r1922 = stub1122;
        final LocationManagerService r2122 = stub1322;
        final CountryDetectorService r22222 = stub1422;
        final NetworkTimeUpdateService networkTimeUpdateService222 = networkTimeUpdateService;
        final CommonTimeManagementService commonTimeManagementService222 = commonTimeManagementService;
        final TextServicesManagerService r2522 = stub1522;
        final StatusBarManagerService r2022 = stub1022;
        final AssetAtlasService r2622 = stub1822;
        final InputManagerService r2722 = r62;
        final TelephonyRegistry r2822 = stub9;
        final MediaRouterService r2922 = stub1922;
        final AudioService audioService322 = audioService2;
        this.mActivityManagerService.systemReady(new Runnable() {
            @Override
            public void run() {
                Slog.i(SystemServer.TAG, "Making services ready");
                SystemServer.this.mSystemServiceManager.startBootPhase(SystemService.PHASE_ACTIVITY_MANAGER_READY);
                try {
                    SystemServer.this.mActivityManagerService.startObservingNativeCrashes();
                } catch (Throwable e42) {
                    SystemServer.this.reportWtf("observing native crashes", e42);
                }
                Slog.i(SystemServer.TAG, "WebViewFactory preparation");
                WebViewFactory.prepareWebViewInSystemServer();
                try {
                    SystemServer.startSystemUi(context);
                } catch (Throwable e5) {
                    SystemServer.this.reportWtf("starting System UI", e5);
                }
                try {
                    if (r1122 != null) {
                        r1122.systemReady();
                    }
                } catch (Throwable e6) {
                    SystemServer.this.reportWtf("making Mount Service ready", e6);
                }
                try {
                    if (r1222 != null) {
                        r1222.systemReady();
                    }
                } catch (Throwable e7) {
                    SystemServer.this.reportWtf("making Network Score Service ready", e7);
                }
                try {
                    if (iNetworkManagementService22 != null) {
                        iNetworkManagementService22.systemReady();
                    }
                } catch (Throwable e8) {
                    SystemServer.this.reportWtf("making Network Managment Service ready", e8);
                }
                try {
                    if (r1422 != null) {
                        r1422.systemReady();
                    }
                } catch (Throwable e9) {
                    SystemServer.this.reportWtf("making Network Stats Service ready", e9);
                }
                try {
                    if (r1522 != null) {
                        r1522.systemReady();
                    }
                } catch (Throwable e10) {
                    SystemServer.this.reportWtf("making Network Policy Service ready", e10);
                }
                try {
                    if (r1622 != null) {
                        r1622.systemReady();
                    }
                } catch (Throwable e11) {
                    SystemServer.this.reportWtf("making Connectivity Service ready", e11);
                }
                try {
                    if (audioService322 != null) {
                        audioService322.systemReady();
                    }
                } catch (Throwable e12) {
                    SystemServer.this.reportWtf("Notifying AudioService running", e12);
                }
                Watchdog.getInstance().start();
                SystemServer.this.mSystemServiceManager.startBootPhase(600);
                try {
                    if (r1822 != null) {
                        r1822.systemRunning();
                    }
                } catch (Throwable e13) {
                    SystemServer.this.reportWtf("Notifying WallpaperService running", e13);
                }
                try {
                    if (r1922 != null) {
                        r1922.systemRunning(r2022);
                    }
                } catch (Throwable e14) {
                    SystemServer.this.reportWtf("Notifying InputMethodService running", e14);
                }
                try {
                    if (r2122 != null) {
                        r2122.systemRunning();
                    }
                } catch (Throwable e15) {
                    SystemServer.this.reportWtf("Notifying Location Service running", e15);
                }
                try {
                    if (r22222 != null) {
                        r22222.systemRunning();
                    }
                } catch (Throwable e16) {
                    SystemServer.this.reportWtf("Notifying CountryDetectorService running", e16);
                }
                try {
                    if (networkTimeUpdateService222 != null) {
                        networkTimeUpdateService222.systemRunning();
                    }
                } catch (Throwable e17) {
                    SystemServer.this.reportWtf("Notifying NetworkTimeService running", e17);
                }
                try {
                    if (commonTimeManagementService222 != null) {
                        commonTimeManagementService222.systemRunning();
                    }
                } catch (Throwable e18) {
                    SystemServer.this.reportWtf("Notifying CommonTimeManagementService running", e18);
                }
                try {
                    if (r2522 != null) {
                        r2522.systemRunning();
                    }
                } catch (Throwable e19) {
                    SystemServer.this.reportWtf("Notifying TextServicesManagerService running", e19);
                }
                try {
                    if (r2622 != null) {
                        r2622.systemRunning();
                    }
                } catch (Throwable e20) {
                    SystemServer.this.reportWtf("Notifying AssetAtlasService running", e20);
                }
                try {
                    if (r2722 != null) {
                        r2722.systemRunning();
                    }
                } catch (Throwable e21) {
                    SystemServer.this.reportWtf("Notifying InputManagerService running", e21);
                }
                try {
                    if (r2822 != null) {
                        r2822.systemRunning();
                    }
                } catch (Throwable e22) {
                    SystemServer.this.reportWtf("Notifying TelephonyRegistry running", e22);
                }
                try {
                    if (r2922 != null) {
                        r2922.systemRunning();
                    }
                } catch (Throwable e23) {
                    SystemServer.this.reportWtf("Notifying MediaRouterService running", e23);
                }
                try {
                    if (mmsServiceBroker22 != null) {
                        mmsServiceBroker22.systemRunning();
                    }
                } catch (Throwable e24) {
                    SystemServer.this.reportWtf("Notifying MmsService running", e24);
                }
            }
        });
    }

    static final void startSystemUi(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.systemui", "com.android.systemui.SystemUIService"));
        context.startServiceAsUser(intent, UserHandle.OWNER);
    }
}
