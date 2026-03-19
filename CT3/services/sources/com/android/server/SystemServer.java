package com.android.server;

import android.R;
import android.app.ActivityThread;
import android.app.INotificationManager;
import android.app.usage.UsageStatsManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.ICountryDetector;
import android.location.ILocationManager;
import android.media.IMediaRouterService;
import android.net.IConnectivityManager;
import android.net.INetworkPolicyManager;
import android.net.INetworkScoreService;
import android.net.INetworkStatsService;
import android.os.BaseBundle;
import android.os.BenesseExtension;
import android.os.Build;
import android.os.Environment;
import android.os.FactoryTest;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.IVibratorService;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.storage.IMountService;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import android.view.IAssetAtlas;
import android.view.WindowManager;
import com.android.internal.os.BinderInternal;
import com.android.internal.os.SamplingProfilerIntegration;
import com.android.internal.telephony.ITelephonyRegistry;
import com.android.internal.widget.ILockSettings;
import com.android.server.InputMethodManagerService;
import com.android.server.TextServicesManagerService;
import com.android.server.accessibility.AccessibilityManagerService;
import com.android.server.am.ActivityManagerService;
import com.android.server.audio.AudioService;
import com.android.server.camera.CameraService;
import com.android.server.clipboard.ClipboardService;
import com.android.server.connectivity.MetricsLoggerService;
import com.android.server.devicepolicy.DevicePolicyManagerService;
import com.android.server.display.DisplayManagerService;
import com.android.server.dreams.DreamManagerService;
import com.android.server.fingerprint.FingerprintService;
import com.android.server.hdmi.HdmiControlService;
import com.android.server.input.InputManagerService;
import com.android.server.job.JobSchedulerService;
import com.android.server.lights.LightsService;
import com.android.server.media.MediaResourceMonitorService;
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
import com.android.server.pm.OtaDexoptService;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.ShortcutService;
import com.android.server.pm.UserManagerService;
import com.android.server.power.PowerManagerService;
import com.android.server.power.ShutdownThread;
import com.android.server.restrictions.RestrictionsManagerService;
import com.android.server.soundtrigger.SoundTriggerService;
import com.android.server.statusbar.StatusBarManagerService;
import com.android.server.storage.DeviceStorageMonitorService;
import com.android.server.telecom.TelecomLoaderService;
import com.android.server.trust.TrustManagerService;
import com.android.server.tv.TvInputManagerService;
import com.android.server.tv.TvRemoteService;
import com.android.server.twilight.TwilightService;
import com.android.server.usage.UsageStatsService;
import com.android.server.vr.VrManagerService;
import com.android.server.webkit.WebViewUpdateService;
import com.android.server.wm.WindowManagerService;
import com.mediatek.hdmi.MtkHdmiManagerService;
import com.mediatek.msglogger.MessageMonitorService;
import com.mediatek.perfservice.IPerfServiceManager;
import com.mediatek.perfservice.PerfServiceImpl;
import com.mediatek.perfservice.PerfServiceManager;
import com.mediatek.runningbooster.RunningBoosterService;
import com.mediatek.search.SearchEngineManagerService;
import com.mediatek.sensorhub.SensorHubService;
import com.mediatek.suppression.service.SuppressionService;
import dalvik.system.VMRuntime;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public final class SystemServer {
    private static final String ACCOUNT_SERVICE_CLASS = "com.android.server.accounts.AccountManagerService$Lifecycle";
    private static final String APPDETECTION_SERVICE_CLASS = "com.mediatek.pq.AppDetectionService$Lifecycle";
    private static final String APPWIDGET_SERVICE_CLASS = "com.android.server.appwidget.AppWidgetService";
    private static final String BACKUP_MANAGER_SERVICE_CLASS = "com.android.server.backup.BackupManagerService$Lifecycle";
    private static final String BLOCK_MAP_FILE = "/cache/recovery/block.map";
    private static final String CONTENT_SERVICE_CLASS = "com.android.server.content.ContentService$Lifecycle";
    private static final String DATASHPAING_SERVICE_CLASS = "com.mediatek.datashaping.DataShapingService";
    private static final int DEFAULT_SYSTEM_THEME = 16974143;
    private static final long EARLIEST_SUPPORTED_TIME = 86400000;
    private static final String ENCRYPTED_STATE = "1";
    private static final String ENCRYPTING_STATE = "trigger_restart_min_framework";
    private static final String ETHERNET_SERVICE_CLASS = "com.android.server.ethernet.EthernetService";
    private static final boolean IS_USER_BUILD;
    private static final String JOB_SCHEDULER_SERVICE_CLASS = "com.android.server.job.JobSchedulerService";
    private static final String LOCK_SETTINGS_SERVICE_CLASS = "com.android.server.LockSettingsService$Lifecycle";
    private static final String LWX_SERVICE_CLASS = "com.mediatek.server.lwx.LwxService";
    private static final String MIDI_SERVICE_CLASS = "com.android.server.midi.MidiService$Lifecycle";
    private static final String MOUNT_SERVICE_CLASS = "com.android.server.MountService$Lifecycle";
    private static final String PERSISTENT_DATA_BLOCK_PROP = "ro.frp.pst";
    private static final String PRINT_MANAGER_SERVICE_CLASS = "com.android.server.print.PrintManagerService";
    private static final String SEARCH_MANAGER_SERVICE_CLASS = "com.android.server.search.SearchManagerService$Lifecycle";
    private static final long SNAPSHOT_INTERVAL = 3600000;
    private static final String TAG = "SystemServer";
    private static final String THERMAL_OBSERVER_CLASS = "com.google.android.clockwork.ThermalObserver";
    private static final String UNCRYPT_PACKAGE_FILE = "/cache/recovery/uncrypt_file";
    private static final String USB_SERVICE_CLASS = "com.android.server.usb.UsbService$Lifecycle";
    private static final String USP_SERVICE_CLASS = "com.mediatek.usp.UspService";
    private static final String VOICE_RECOGNITION_MANAGER_SERVICE_CLASS = "com.android.server.voiceinteraction.VoiceInteractionManagerService";
    private static final String WALLPAPER_SERVICE_CLASS = "com.android.server.wallpaper.WallpaperManagerService$Lifecycle";
    private static final String WEAR_BLUETOOTH_SERVICE_CLASS = "com.google.android.clockwork.bluetooth.WearBluetoothService";
    private static final String WIFI_NAN_SERVICE_CLASS = "com.android.server.wifi.nan.WifiNanService";
    private static final String WIFI_P2P_SERVICE_CLASS = "com.android.server.wifi.p2p.WifiP2pService";
    private static final String WIFI_SERVICE_CLASS = "com.android.server.wifi.WifiService";
    private static boolean mMTPROF_disable = false;
    private static final int sMaxBinderThreads = 31;
    private ActivityManagerService mActivityManagerService;
    private ContentResolver mContentResolver;
    private DisplayManagerService mDisplayManagerService;
    private EntropyMixer mEntropyMixer;
    private final int mFactoryTestMode = FactoryTest.getMode();
    private boolean mFirstBoot;
    private boolean mOnlyCore;
    private PackageManager mPackageManager;
    private PackageManagerService mPackageManagerService;
    private PowerManagerService mPowerManagerService;
    private Timer mProfilerSnapshotTimer;
    private Context mSystemContext;
    private SystemServiceManager mSystemServiceManager;
    private WebViewUpdateService mWebViewUpdateService;

    private static native void startSensorService();

    static {
        IS_USER_BUILD = !"user".equals(Build.TYPE) ? "userdebug".equals(Build.TYPE) : true;
    }

    public static void main(String[] args) {
        new SystemServer().run();
    }

    private void run() {
        try {
            Trace.traceBegin(2097152L, "InitBeforeStartServices");
            if (System.currentTimeMillis() < 86400000) {
                Slog.w(TAG, "System clock is before 1970; setting to 1970.");
                SystemClock.setCurrentTimeMillis(86400000L);
            }
            if (!SystemProperties.get("persist.sys.language").isEmpty()) {
                String languageTag = Locale.getDefault().toLanguageTag();
                SystemProperties.set("persist.sys.locale", languageTag);
                SystemProperties.set("persist.sys.language", "");
                SystemProperties.set("persist.sys.country", "");
                SystemProperties.set("persist.sys.localevar", "");
            }
            Slog.i(TAG, "Entered the Android system server!");
            EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_SYSTEM_RUN, SystemClock.uptimeMillis());
            addBootEvent("Android:SysServerInit_START");
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
            BaseBundle.setShouldDefuse(true);
            BinderInternal.disableBackgroundScheduling(true);
            BinderInternal.setMaxThreads(31);
            Process.setThreadPriority(-2);
            Process.setCanSelfBackground(false);
            Looper.prepareMainLooper();
            System.loadLibrary("android_servers");
            try {
                Runtime.getRuntime().exec("rm -r /data/piggybank");
            } catch (IOException e) {
                Slog.e(TAG, "system server init delete piggybank fail" + e);
            }
            performPendingShutdown();
            createSystemContext();
            this.mSystemServiceManager = new SystemServiceManager(this.mSystemContext);
            LocalServices.addService(SystemServiceManager.class, this.mSystemServiceManager);
            try {
                try {
                    Trace.traceBegin(2097152L, "StartServices");
                    startBootstrapServices();
                    startCoreServices();
                    startOtherServices();
                    Trace.traceEnd(2097152L);
                    if (StrictMode.conditionallyEnableDebugLogging()) {
                        Slog.i(TAG, "Enabled StrictMode for system server main thread.");
                    }
                    addBootEvent("Android:SysServerInit_END");
                    Looper.loop();
                    throw new RuntimeException("Main thread loop unexpectedly exited");
                } finally {
                }
            } catch (Throwable ex) {
                Slog.e("System", "******************************************");
                Slog.e("System", "************ Failure starting system services", ex);
                throw ex;
            }
        } finally {
        }
    }

    private void reportWtf(String msg, Throwable e) {
        Slog.w(TAG, "***********************************************");
        Slog.wtf(TAG, "BOOT FAILURE " + msg, e);
    }

    private static void addBootEvent(String bootevent) {
        if (mMTPROF_disable) {
            return;
        }
        try {
            FileOutputStream fbp = new FileOutputStream("/proc/bootprof");
            fbp.write(bootevent.getBytes());
            fbp.flush();
            fbp.close();
        } catch (FileNotFoundException e) {
            Slog.e("BOOTPROF", "Failure open /proc/bootprof, not found!", e);
        } catch (IOException e2) {
            Slog.e("BOOTPROF", "Failure open /proc/bootprof entry", e2);
        }
    }

    private void performPendingShutdown() {
        String strSubstring;
        String shutdownAction = SystemProperties.get(ShutdownThread.SHUTDOWN_ACTION_PROPERTY, "");
        if (shutdownAction == null || shutdownAction.length() <= 0) {
            return;
        }
        boolean reboot = shutdownAction.charAt(0) == '1';
        if (shutdownAction.length() > 1) {
            strSubstring = shutdownAction.substring(1, shutdownAction.length());
        } else {
            strSubstring = null;
        }
        if ("recovery-update".equals(strSubstring)) {
            File packageFile = new File(UNCRYPT_PACKAGE_FILE);
            if (packageFile.exists()) {
                String filename = null;
                try {
                    filename = FileUtils.readTextFile(packageFile, 0, null);
                } catch (IOException e) {
                    Slog.e(TAG, "Error reading uncrypt package file", e);
                }
                if (filename != null && filename.startsWith("/data") && !new File(BLOCK_MAP_FILE).exists()) {
                    Slog.e(TAG, "Can't find block map file, uncrypt failed or unexpected runtime restart?");
                    return;
                }
            }
        }
        ShutdownThread.rebootOrShutdown(null, reboot, strSubstring);
    }

    private void createSystemContext() {
        ActivityThread activityThread = ActivityThread.systemMain();
        this.mSystemContext = activityThread.getSystemContext();
        this.mSystemContext.setTheme(16974143);
    }

    private void startBootstrapServices() {
        MessageMonitorService msgMonitorService;
        Installer installer = (Installer) this.mSystemServiceManager.startService(Installer.class);
        if (!IS_USER_BUILD) {
            try {
                msgMonitorService = new MessageMonitorService();
            } catch (Throwable th) {
                e = th;
            }
            try {
                Slog.e(TAG, "Create message monitor service successfully .");
                ServiceManager.addService("msgmonitorservice", msgMonitorService.asBinder());
            } catch (Throwable th2) {
                e = th2;
                Slog.e(TAG, "Starting message monitor service exception ", e);
            }
        }
        this.mActivityManagerService = ((ActivityManagerService.Lifecycle) this.mSystemServiceManager.startService(ActivityManagerService.Lifecycle.class)).getService();
        this.mActivityManagerService.setSystemServiceManager(this.mSystemServiceManager);
        this.mActivityManagerService.setInstaller(installer);
        this.mPowerManagerService = (PowerManagerService) this.mSystemServiceManager.startService(PowerManagerService.class);
        Trace.traceBegin(2097152L, "InitPowerManagement");
        this.mActivityManagerService.initPowerManagement();
        Trace.traceEnd(2097152L);
        this.mSystemServiceManager.startService(LightsService.class);
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
        traceBeginAndSlog("StartPackageManagerService");
        this.mPackageManagerService = PackageManagerService.main(this.mSystemContext, installer, this.mFactoryTestMode != 0, this.mOnlyCore);
        this.mFirstBoot = this.mPackageManagerService.isFirstBoot();
        this.mPackageManager = this.mSystemContext.getPackageManager();
        Trace.traceEnd(2097152L);
        if (!this.mOnlyCore) {
            boolean disableOtaDexopt = SystemProperties.getBoolean("config.disable_otadexopt", false);
            if (!disableOtaDexopt) {
                traceBeginAndSlog("StartOtaDexOptService");
                try {
                    OtaDexoptService.main(this.mSystemContext, this.mPackageManagerService);
                } catch (Throwable e) {
                    reportWtf("starting OtaDexOptService", e);
                } finally {
                    Trace.traceEnd(2097152L);
                }
            }
        }
        traceBeginAndSlog("StartUserManagerService");
        this.mSystemServiceManager.startService(UserManagerService.LifeCycle.class);
        Trace.traceEnd(2097152L);
        AttributeCache.init(this.mSystemContext);
        this.mActivityManagerService.setSystemProcess();
        this.mPackageManagerService.onAmsAddedtoServiceMgr();
        startSensorService();
    }

    private void startCoreServices() {
        this.mSystemServiceManager.startService(BatteryService.class);
        this.mSystemServiceManager.startService(UsageStatsService.class);
        this.mActivityManagerService.setUsageStatsManager((UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class));
        this.mWebViewUpdateService = (WebViewUpdateService) this.mSystemServiceManager.startService(WebViewUpdateService.class);
    }

    private void startOtherServices() throws Throwable {
        ?? r96;
        ?? r92;
        ?? r60;
        ?? r2;
        ?? r22;
        PerfServiceManager perfServiceManager;
        IMediaRouterService.Stub mediaRouterService;
        IAssetAtlas.Stub assetAtlasService;
        ?? r6;
        INetworkPolicyManager.Stub networkPolicyManagerService;
        ?? r23;
        IConnectivityManager.Stub connectivityService;
        final Context context = this.mSystemContext;
        IVibratorService.Stub stub = null;
        stub = null;
        IMountService iMountServiceAsInterface = null;
        INetworkManagementService iNetworkManagementServiceCreate = null;
        ?? r62 = 0;
        INetworkStatsService.Stub stubCreate = null;
        ?? r63 = 0;
        IConnectivityManager.Stub stub2 = null;
        INetworkScoreService.Stub stub3 = null;
        ?? Main = 0;
        Main = 0;
        Main = 0;
        Main = 0;
        Main = 0;
        NetworkTimeUpdateService networkTimeUpdateService = null;
        CommonTimeManagementService commonTimeManagementService = null;
        ?? r602 = 0;
        r602 = 0;
        r602 = 0;
        r602 = 0;
        ITelephonyRegistry.Stub stub4 = null;
        RunningBoosterService runningBoosterService = null;
        boolean z = SystemProperties.getBoolean("config.disable_storage", false);
        boolean z2 = SystemProperties.getBoolean("config.disable_bluetooth", false);
        boolean z3 = SystemProperties.getBoolean("config.disable_location", false);
        boolean z4 = SystemProperties.getBoolean("config.disable_systemui", false);
        boolean z5 = SystemProperties.getBoolean("config.disable_noncore", false);
        boolean z6 = SystemProperties.getBoolean("config.disable_network", false);
        boolean z7 = SystemProperties.getBoolean("config.disable_networktime", false);
        boolean z8 = SystemProperties.getBoolean("config.disable_rtt", false);
        boolean z9 = SystemProperties.getBoolean("config.disable_mediaproj", false);
        boolean z10 = SystemProperties.getBoolean("config.disable_serial", false);
        boolean z11 = SystemProperties.getBoolean("config.disable_searchmanager", false);
        boolean z12 = SystemProperties.getBoolean("config.disable_trustmanager", false);
        boolean z13 = SystemProperties.getBoolean("config.disable_textservices", false);
        boolean z14 = SystemProperties.getBoolean("config.disable_samplingprof", false);
        boolean zEquals = SystemProperties.get("ro.kernel.qemu").equals(ENCRYPTED_STATE);
        try {
            Slog.i(TAG, "Reading configuration...");
            SystemConfig.getInstance();
            traceBeginAndSlog("StartSchedulingPolicyService");
            ServiceManager.addService("scheduling_policy", new SchedulingPolicyService());
            Trace.traceEnd(2097152L);
            this.mSystemServiceManager.startService(TelecomLoaderService.class);
            traceBeginAndSlog("StartTelephonyRegistry");
            ITelephonyRegistry.Stub telephonyRegistry = new TelephonyRegistry(context);
            try {
                ServiceManager.addService("telephony.registry", telephonyRegistry);
                Trace.traceEnd(2097152L);
                traceBeginAndSlog("StartEntropyMixer");
                this.mEntropyMixer = new EntropyMixer(context);
                Trace.traceEnd(2097152L);
                this.mContentResolver = context.getContentResolver();
                Slog.i(TAG, "Camera Service");
                this.mSystemServiceManager.startService(CameraService.class);
                traceBeginAndSlog("StartAccountManagerService");
                this.mSystemServiceManager.startService(ACCOUNT_SERVICE_CLASS);
                Trace.traceEnd(2097152L);
                traceBeginAndSlog("StartContentService");
                this.mSystemServiceManager.startService(CONTENT_SERVICE_CLASS);
                Trace.traceEnd(2097152L);
                traceBeginAndSlog("InstallSystemProviders");
                this.mActivityManagerService.installSystemProviders();
                Trace.traceEnd(2097152L);
                traceBeginAndSlog("StartVibratorService");
                IVibratorService.Stub vibratorService = new VibratorService(context);
                try {
                    ServiceManager.addService("vibrator", vibratorService);
                    Trace.traceEnd(2097152L);
                    traceBeginAndSlog("StartConsumerIrService");
                    try {
                        ServiceManager.addService("consumer_ir", new ConsumerIrService(context));
                        Trace.traceEnd(2097152L);
                        traceBeginAndSlog("StartAlarmManagerService");
                        this.mSystemServiceManager.startService(AlarmManagerService.class);
                        Trace.traceEnd(2097152L);
                        traceBeginAndSlog("InitWatchdog");
                        Watchdog.getInstance().init(context, this.mActivityManagerService);
                        Trace.traceEnd(2097152L);
                        traceBeginAndSlog("StartInputManagerService");
                        ?? inputManagerService = new InputManagerService(context);
                        try {
                            Trace.traceEnd(2097152L);
                            traceBeginAndSlog("StartWindowManagerService");
                            Main = WindowManagerService.main(context, inputManagerService, this.mFactoryTestMode != 1, !this.mFirstBoot, this.mOnlyCore);
                            ServiceManager.addService("window", (IBinder) Main);
                            ServiceManager.addService("input", (IBinder) inputManagerService);
                            Trace.traceEnd(2097152L);
                            traceBeginAndSlog("StartVrManagerService");
                            this.mSystemServiceManager.startService(VrManagerService.class);
                            Trace.traceEnd(2097152L);
                            this.mActivityManagerService.setWindowManager(Main);
                            inputManagerService.setWindowManagerCallbacks(Main.getInputMonitor());
                            inputManagerService.start();
                            this.mDisplayManagerService.windowManagerAndInputReady();
                            if (zEquals) {
                                Slog.i(TAG, "No Bluetooth Service (emulator)");
                            } else if (this.mFactoryTestMode == 1) {
                                Slog.i(TAG, "No Bluetooth Service (factory test)");
                            } else if (!context.getPackageManager().hasSystemFeature("android.hardware.bluetooth")) {
                                Slog.i(TAG, "No Bluetooth Service (Bluetooth Hardware Not Present)");
                            } else if (z2) {
                                Slog.i(TAG, "Bluetooth Service disabled by config");
                            } else {
                                this.mSystemServiceManager.startService(BluetoothService.class);
                            }
                            traceBeginAndSlog("ConnectivityMetricsLoggerService");
                            this.mSystemServiceManager.startService(MetricsLoggerService.class);
                            Trace.traceEnd(2097152L);
                            traceBeginAndSlog("PinnerService");
                            this.mSystemServiceManager.startService(PinnerService.class);
                            Trace.traceEnd(2097152L);
                            stub4 = telephonyRegistry;
                            r60 = inputManagerService;
                            r92 = vibratorService;
                            r96 = Main;
                        } catch (RuntimeException e) {
                            e = e;
                            stub4 = telephonyRegistry;
                            r602 = inputManagerService;
                            stub = vibratorService;
                            Slog.e("System", "******************************************");
                            Slog.e("System", "************ Failure starting core service", e);
                            r60 = r602;
                            r92 = stub;
                            r96 = Main;
                        }
                    } catch (RuntimeException e2) {
                        e = e2;
                        stub4 = telephonyRegistry;
                        stub = vibratorService;
                    }
                } catch (RuntimeException e3) {
                    e = e3;
                    stub4 = telephonyRegistry;
                    stub = vibratorService;
                }
            } catch (RuntimeException e4) {
                e = e4;
                stub4 = telephonyRegistry;
            }
        } catch (RuntimeException e5) {
            e = e5;
        }
        ILocationManager.Stub stub5 = null;
        ICountryDetector.Stub stub6 = null;
        ILockSettings iLockSettingsAsInterface = null;
        IPerfServiceManager iPerfServiceManager = null;
        IAssetAtlas.Stub stub7 = null;
        IMediaRouterService.Stub stub8 = null;
        if (this.mFactoryTestMode != 1) {
            this.mSystemServiceManager.startService(InputMethodManagerService.Lifecycle.class);
            traceBeginAndSlog("StartAccessibilityManagerService");
            try {
                ServiceManager.addService("accessibility", new AccessibilityManagerService(context));
            } catch (Throwable th) {
                reportWtf("starting Accessibility Manager", th);
            }
            Trace.traceEnd(2097152L);
        }
        try {
            r96.displayReady();
        } catch (Throwable th2) {
            reportWtf("making display ready", th2);
        }
        if (this.mFactoryTestMode != 1 && !z && !"0".equals(SystemProperties.get("system_init.startmountservice"))) {
            try {
                this.mSystemServiceManager.startService(MOUNT_SERVICE_CLASS);
                iMountServiceAsInterface = IMountService.Stub.asInterface(ServiceManager.getService("mount"));
            } catch (Throwable th3) {
                reportWtf("starting Mount Service", th3);
            }
        }
        this.mSystemServiceManager.startService(UiModeManagerService.class);
        if (!this.mOnlyCore) {
            Trace.traceBegin(2097152L, "UpdatePackagesIfNeeded");
            try {
                this.mPackageManagerService.updatePackagesIfNeeded();
            } catch (Throwable th4) {
                reportWtf("update packages", th4);
            }
            Trace.traceEnd(2097152L);
        }
        Trace.traceBegin(2097152L, "PerformFstrimIfNeeded");
        try {
            this.mPackageManagerService.performFstrimIfNeeded();
        } catch (Throwable th5) {
            reportWtf("performing fstrim", th5);
        }
        Trace.traceEnd(2097152L);
        if (this.mFactoryTestMode == 1) {
            r2 = 0;
        } else {
            if (!z5) {
                traceBeginAndSlog("StartLockSettingsService");
                try {
                    this.mSystemServiceManager.startService(LOCK_SETTINGS_SERVICE_CLASS);
                    iLockSettingsAsInterface = ILockSettings.Stub.asInterface(ServiceManager.getService("lock_settings"));
                } catch (Throwable th6) {
                    reportWtf("starting LockSettingsService service", th6);
                }
                Trace.traceEnd(2097152L);
                if (!SystemProperties.get(PERSISTENT_DATA_BLOCK_PROP).equals("")) {
                    this.mSystemServiceManager.startService(PersistentDataBlockService.class);
                }
                this.mSystemServiceManager.startService(DeviceIdleController.class);
                this.mSystemServiceManager.startService(DevicePolicyManagerService.Lifecycle.class);
            }
            if (!z4) {
                traceBeginAndSlog("StartStatusBarManagerService");
                try {
                } catch (Throwable th7) {
                    th = th7;
                }
                try {
                    ServiceManager.addService("statusbar", new StatusBarManagerService(context, r96));
                } catch (Throwable th8) {
                    th = th8;
                    reportWtf("starting StatusBarManagerService", th);
                }
                Trace.traceEnd(2097152L);
            }
            if (!z5) {
                traceBeginAndSlog("StartClipboardService");
                try {
                    ServiceManager.addService("clipboard", new ClipboardService(context));
                } catch (Throwable th9) {
                    reportWtf("starting Clipboard Service", th9);
                }
                Trace.traceEnd(2097152L);
            }
            if (!z6) {
                traceBeginAndSlog("StartNetworkManagementService");
                try {
                    iNetworkManagementServiceCreate = NetworkManagementService.create(context);
                    ServiceManager.addService("network_management", iNetworkManagementServiceCreate);
                } catch (Throwable th10) {
                    reportWtf("starting NetworkManagement Service", th10);
                }
                Trace.traceEnd(2097152L);
            }
            if (!z5 && !z13) {
                this.mSystemServiceManager.startService(TextServicesManagerService.Lifecycle.class);
            }
            if (z6) {
                r22 = 0;
            } else {
                traceBeginAndSlog("StartNetworkScoreService");
                try {
                    INetworkScoreService.Stub networkScoreService = new NetworkScoreService(context);
                    try {
                        ServiceManager.addService("network_score", networkScoreService);
                        stub3 = networkScoreService;
                    } catch (Throwable th11) {
                        th = th11;
                        stub3 = networkScoreService;
                        reportWtf("starting Network Score Service", th);
                    }
                } catch (Throwable th12) {
                    th = th12;
                }
                Trace.traceEnd(2097152L);
                traceBeginAndSlog("StartNetworkStatsService");
                try {
                    stubCreate = NetworkStatsService.create(context, iNetworkManagementServiceCreate);
                    ServiceManager.addService("netstats", stubCreate);
                    r6 = stubCreate;
                } catch (Throwable th13) {
                    reportWtf("starting NetworkStats Service", th13);
                    r6 = stubCreate;
                }
                Trace.traceEnd(2097152L);
                traceBeginAndSlog("StartNetworkPolicyManagerService");
                try {
                    networkPolicyManagerService = new NetworkPolicyManagerService(context, this.mActivityManagerService, ServiceManager.getService("power"), r6, iNetworkManagementServiceCreate);
                    try {
                        ServiceManager.addService("netpolicy", networkPolicyManagerService);
                        r23 = networkPolicyManagerService;
                    } catch (Throwable th14) {
                        th = th14;
                        reportWtf("starting NetworkPolicy Service", th);
                        r23 = networkPolicyManagerService;
                    }
                } catch (Throwable th15) {
                    th = th15;
                    networkPolicyManagerService = null;
                }
                Trace.traceEnd(2097152L);
                if (context.getPackageManager().hasSystemFeature("android.hardware.wifi.nan")) {
                    this.mSystemServiceManager.startService(WIFI_NAN_SERVICE_CLASS);
                } else {
                    Slog.i(TAG, "No Wi-Fi NAN Service (NAN support Not Present)");
                }
                this.mSystemServiceManager.startService(WIFI_P2P_SERVICE_CLASS);
                this.mSystemServiceManager.startService(WIFI_SERVICE_CLASS);
                this.mSystemServiceManager.startService("com.android.server.wifi.scanner.WifiScanningService");
                if (!z8) {
                    this.mSystemServiceManager.startService("com.android.server.wifi.RttService");
                }
                this.mSystemServiceManager.startService(ETHERNET_SERVICE_CLASS);
                traceBeginAndSlog("StartConnectivityService");
                try {
                    connectivityService = new ConnectivityService(context, iNetworkManagementServiceCreate, r6, r23);
                } catch (Throwable th16) {
                    th = th16;
                }
                try {
                    ServiceManager.addService("connectivity", connectivityService);
                    r6.bindConnectivityManager(connectivityService);
                    r23.bindConnectivityManager(connectivityService);
                    stub2 = connectivityService;
                } catch (Throwable th17) {
                    th = th17;
                    stub2 = connectivityService;
                    reportWtf("starting Connectivity Service", th);
                }
                Trace.traceEnd(2097152L);
                traceBeginAndSlog("StartNsdService");
                try {
                    ServiceManager.addService("servicediscovery", NsdService.create(context));
                } catch (Throwable th18) {
                    reportWtf("starting Service Discovery Service", th18);
                }
                Trace.traceEnd(2097152L);
                if (ENCRYPTED_STATE.equals(SystemProperties.get("persist.mtk.datashaping.support"))) {
                    traceBeginAndSlog("StartDataShapingService");
                    try {
                        this.mSystemServiceManager.startService(DATASHPAING_SERVICE_CLASS);
                    } catch (Throwable th19) {
                        reportWtf("starting DataShapingService", th19);
                    }
                    Trace.traceEnd(2097152L);
                }
                if (!ENCRYPTED_STATE.equals(SystemProperties.get("ro.mtk_lwa_support"))) {
                    r22 = r23;
                    r63 = r6;
                    if (ENCRYPTED_STATE.equals(SystemProperties.get("ro.mtk_lwi_support"))) {
                        traceBeginAndSlog("StartLwxService");
                        try {
                            this.mSystemServiceManager.startService(LWX_SERVICE_CLASS);
                        } catch (Throwable th20) {
                            reportWtf("starting LwxService", th20);
                        }
                        Trace.traceEnd(2097152L);
                        r22 = r23;
                        r63 = r6;
                    }
                }
            }
            if (!z5) {
                traceBeginAndSlog("StartUpdateLockService");
                try {
                    ServiceManager.addService("updatelock", new UpdateLockService(context));
                } catch (Throwable th21) {
                    reportWtf("starting UpdateLockService", th21);
                }
                Trace.traceEnd(2097152L);
            }
            if (!z5) {
                this.mSystemServiceManager.startService(RecoverySystemService.class);
            }
            if (iMountServiceAsInterface != null && !this.mOnlyCore) {
                Trace.traceBegin(2097152L, "WaitForAsecScan");
                try {
                    iMountServiceAsInterface.waitForAsecScan();
                } catch (RemoteException e6) {
                }
                Trace.traceEnd(2097152L);
            }
            this.mSystemServiceManager.startService(NotificationManagerService.class);
            r22.bindNotificationManager(INotificationManager.Stub.asInterface(ServiceManager.getService("notification")));
            this.mSystemServiceManager.startService(DeviceStorageMonitorService.class);
            if (!z3) {
                traceBeginAndSlog("StartLocationManagerService");
                try {
                    ILocationManager.Stub locationManagerService = new LocationManagerService(context);
                    try {
                        ServiceManager.addService("location", locationManagerService);
                        stub5 = locationManagerService;
                    } catch (Throwable th22) {
                        th = th22;
                        stub5 = locationManagerService;
                        reportWtf("starting Location Manager", th);
                    }
                } catch (Throwable th23) {
                    th = th23;
                }
                Trace.traceEnd(2097152L);
                traceBeginAndSlog("StartCountryDetectorService");
                try {
                    ICountryDetector.Stub countryDetectorService = new CountryDetectorService(context);
                    try {
                        ServiceManager.addService("country_detector", countryDetectorService);
                        stub6 = countryDetectorService;
                    } catch (Throwable th24) {
                        th = th24;
                        stub6 = countryDetectorService;
                        reportWtf("starting Country Detector", th);
                    }
                } catch (Throwable th25) {
                    th = th25;
                }
                Trace.traceEnd(2097152L);
            }
            if (!z5 && !z11) {
                traceBeginAndSlog("StartSearchManagerService");
                try {
                    this.mSystemServiceManager.startService(SEARCH_MANAGER_SERVICE_CLASS);
                } catch (Throwable th26) {
                    reportWtf("starting Search Service", th26);
                }
                Trace.traceEnd(2097152L);
                try {
                    Slog.i(TAG, "Search Engine Service");
                    ServiceManager.addService("search_engine", new SearchEngineManagerService(context));
                } catch (Throwable th27) {
                    reportWtf("starting Search Engine Service", th27);
                }
            }
            this.mSystemServiceManager.startService(DropBoxManagerService.class);
            if (!z5 && context.getResources().getBoolean(R.^attr-private.expandActivityOverflowButtonDrawable)) {
                traceBeginAndSlog("StartWallpaperManagerService");
                this.mSystemServiceManager.startService(WALLPAPER_SERVICE_CLASS);
                Trace.traceEnd(2097152L);
            }
            traceBeginAndSlog("StartAudioService");
            this.mSystemServiceManager.startService(AudioService.Lifecycle.class);
            Trace.traceEnd(2097152L);
            if (ENCRYPTED_STATE.equals(SystemProperties.get("ro.mtk_sensorhub_support"))) {
                try {
                    Slog.d(TAG, "SensorHubService");
                    ServiceManager.addService("sensorhubservice", new SensorHubService(context));
                } catch (Throwable th28) {
                    Slog.e(TAG, "starting SensorHub Service", th28);
                }
            }
            if (!z5) {
                this.mSystemServiceManager.startService(DockObserver.class);
                if (context.getPackageManager().hasSystemFeature("android.hardware.type.watch")) {
                    this.mSystemServiceManager.startService(THERMAL_OBSERVER_CLASS);
                }
            }
            traceBeginAndSlog("StartWiredAccessoryManager");
            try {
                r60.setWiredAccessoryCallbacks(new WiredAccessoryManager(context, r60));
            } catch (Throwable th29) {
                reportWtf("starting WiredAccessoryManager", th29);
            }
            Trace.traceEnd(2097152L);
            if (!z5) {
                if (this.mPackageManager.hasSystemFeature("android.software.midi")) {
                    this.mSystemServiceManager.startService(MIDI_SERVICE_CLASS);
                }
                if (this.mPackageManager.hasSystemFeature("android.hardware.usb.host") || this.mPackageManager.hasSystemFeature("android.hardware.usb.accessory")) {
                    Trace.traceBegin(2097152L, "StartUsbService");
                    this.mSystemServiceManager.startService(USB_SERVICE_CLASS);
                    Trace.traceEnd(2097152L);
                }
                if (!z10) {
                    traceBeginAndSlog("StartSerialService");
                    try {
                        try {
                            ServiceManager.addService("serial", new SerialService(context));
                        } catch (Throwable th30) {
                            th = th30;
                            Slog.e(TAG, "Failure starting SerialService", th);
                        }
                    } catch (Throwable th31) {
                        th = th31;
                    }
                    Trace.traceEnd(2097152L);
                }
                Trace.traceBegin(2097152L, "StartHardwarePropertiesManagerService");
                try {
                } catch (Throwable th32) {
                    th = th32;
                }
                try {
                    ServiceManager.addService("hardware_properties", new HardwarePropertiesManagerService(context));
                } catch (Throwable th33) {
                    th = th33;
                    Slog.e(TAG, "Failure starting HardwarePropertiesManagerService", th);
                }
                Trace.traceEnd(2097152L);
            }
            this.mSystemServiceManager.startService(TwilightService.class);
            this.mSystemServiceManager.startService(JobSchedulerService.class);
            this.mSystemServiceManager.startService(SoundTriggerService.class);
            if (!z5) {
                if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                    this.mSystemServiceManager.startService(BACKUP_MANAGER_SERVICE_CLASS);
                }
                if (this.mPackageManager.hasSystemFeature("android.software.app_widgets") || context.getResources().getBoolean(R.^attr-private.pathColor)) {
                    this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                }
                if (this.mPackageManager.hasSystemFeature("android.software.voice_recognizers")) {
                    this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                }
                if (GestureLauncherService.isGestureLauncherEnabled(context.getResources())) {
                    Slog.i(TAG, "Gesture Launcher Service");
                    this.mSystemServiceManager.startService(GestureLauncherService.class);
                }
                this.mSystemServiceManager.startService(SensorNotificationService.class);
                this.mSystemServiceManager.startService(ContextHubSystemService.class);
            }
            traceBeginAndSlog("StartDiskStatsService");
            try {
                ServiceManager.addService("diskstats", new DiskStatsService(context));
            } catch (Throwable th34) {
                reportWtf("starting DiskStats Service", th34);
            }
            Trace.traceEnd(2097152L);
            if (!z14) {
                traceBeginAndSlog("StartSamplingProfilerService");
                try {
                    ServiceManager.addService("samplingprofiler", new SamplingProfilerService(context));
                } catch (Throwable th35) {
                    reportWtf("starting SamplingProfiler Service", th35);
                }
                Trace.traceEnd(2097152L);
            }
            if (!z6 && !z7) {
                traceBeginAndSlog("StartNetworkTimeUpdateService");
                try {
                    NetworkTimeUpdateService networkTimeUpdateService2 = new NetworkTimeUpdateService(context);
                    try {
                        ServiceManager.addService("network_time_update_service", networkTimeUpdateService2);
                        networkTimeUpdateService = networkTimeUpdateService2;
                    } catch (Throwable th36) {
                        th = th36;
                        networkTimeUpdateService = networkTimeUpdateService2;
                        reportWtf("starting NetworkTimeUpdate service", th);
                    }
                } catch (Throwable th37) {
                    th = th37;
                }
                Trace.traceEnd(2097152L);
            }
            traceBeginAndSlog("StartCommonTimeManagementService");
            try {
                CommonTimeManagementService commonTimeManagementService2 = new CommonTimeManagementService(context);
                try {
                    ServiceManager.addService("commontime_management", commonTimeManagementService2);
                    commonTimeManagementService = commonTimeManagementService2;
                } catch (Throwable th38) {
                    th = th38;
                    commonTimeManagementService = commonTimeManagementService2;
                    reportWtf("starting CommonTimeManagementService service", th);
                }
            } catch (Throwable th39) {
                th = th39;
            }
            Trace.traceEnd(2097152L);
            if (!z6) {
                traceBeginAndSlog("CertBlacklister");
                try {
                    new CertBlacklister(context);
                } catch (Throwable th40) {
                    reportWtf("starting CertBlacklister", th40);
                }
                Trace.traceEnd(2097152L);
            }
            if (!z5) {
                this.mSystemServiceManager.startService(DreamManagerService.class);
            }
            if (!z5 && !SystemProperties.getBoolean("ro.hwui.disable_asset_atlas", false)) {
                traceBeginAndSlog("StartAssetAtlasService");
                try {
                    assetAtlasService = new AssetAtlasService(context);
                } catch (Throwable th41) {
                    th = th41;
                }
                try {
                    ServiceManager.addService(AssetAtlasService.ASSET_ATLAS_SERVICE, assetAtlasService);
                    stub7 = assetAtlasService;
                } catch (Throwable th42) {
                    th = th42;
                    stub7 = assetAtlasService;
                    reportWtf("starting AssetAtlasService", th);
                }
                Trace.traceEnd(2097152L);
            }
            if (!z5) {
                ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, new GraphicsStatsService(context));
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
            if (this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
                this.mSystemServiceManager.startService(MediaResourceMonitorService.class);
            }
            if (this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                this.mSystemServiceManager.startService(TvRemoteService.class);
            }
            if (!z5) {
                traceBeginAndSlog("StartMediaRouterService");
                try {
                    mediaRouterService = new MediaRouterService(context);
                } catch (Throwable th43) {
                    th = th43;
                }
                try {
                    ServiceManager.addService("media_router", mediaRouterService);
                    stub8 = mediaRouterService;
                } catch (Throwable th44) {
                    th = th44;
                    stub8 = mediaRouterService;
                    reportWtf("starting MediaRouterService", th);
                }
                Trace.traceEnd(2097152L);
                if (!z12) {
                    this.mSystemServiceManager.startService(TrustManagerService.class);
                }
                if (this.mPackageManager.hasSystemFeature("android.hardware.fingerprint")) {
                    this.mSystemServiceManager.startService(FingerprintService.class);
                }
                traceBeginAndSlog("StartBackgroundDexOptService");
                try {
                    BackgroundDexOptService.schedule(context);
                } catch (Throwable th45) {
                    reportWtf("starting BackgroundDexOptService", th45);
                }
                Trace.traceEnd(2097152L);
            }
            this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
            this.mSystemServiceManager.startService(LauncherAppsService.class);
            if (SystemProperties.get("ro.mtk_perfservice_support").equals(ENCRYPTED_STATE)) {
                try {
                    perfServiceManager = new PerfServiceManager(context);
                } catch (Throwable th46) {
                    th = th46;
                }
                try {
                    PerfServiceImpl perfServiceImpl = new PerfServiceImpl(context, perfServiceManager);
                    Slog.d("perfservice", "perfService=" + perfServiceImpl);
                    if (perfServiceImpl != null) {
                        ServiceManager.addService("mtk-perfservice", perfServiceImpl.asBinder());
                    }
                    iPerfServiceManager = perfServiceManager;
                } catch (Throwable th47) {
                    th = th47;
                    iPerfServiceManager = perfServiceManager;
                    Slog.e(TAG, "perfservice Failure starting PerfService", th);
                }
            }
            if (!z5 && SystemProperties.get("ro.mtk_hdmi_support").equals(ENCRYPTED_STATE)) {
                try {
                    Slog.i(TAG, "HDMI Manager Service");
                    try {
                        ServiceManager.addService("mtkhdmi", new MtkHdmiManagerService(context).asBinder());
                    } catch (Throwable th48) {
                        th = th48;
                        Slog.e(TAG, "Failure starting MtkHdmiManager", th);
                    }
                } catch (Throwable th49) {
                    th = th49;
                }
            }
            if (!"no".equals(SystemProperties.get("ro.mtk_carrierexpress_pack", "no"))) {
                traceBeginAndSlog("StartUspService");
                try {
                    this.mSystemServiceManager.startService(USP_SERVICE_CLASS);
                } catch (Throwable th50) {
                    reportWtf("starting usp Service", th50);
                }
                Trace.traceEnd(2097152L);
            }
            if (ENCRYPTED_STATE.equals(SystemProperties.get("ro.globalpq.support"))) {
                traceBeginAndSlog("StartAppDetectionService");
                try {
                    this.mSystemServiceManager.startService(APPDETECTION_SERVICE_CLASS);
                } catch (Throwable th51) {
                    reportWtf("Starting AppDetectionService", th51);
                }
                Trace.traceEnd(2097152L);
            }
            r2 = r22;
            r62 = r63;
            if (ENCRYPTED_STATE.equals(SystemProperties.get("persist.runningbooster.support"))) {
                try {
                    ((SuppressionService) this.mSystemServiceManager.startService(SuppressionService.class)).setActivityManager(this.mActivityManagerService);
                    Slog.i(TAG, "RunningBoosterService");
                    RunningBoosterService runningBoosterService2 = new RunningBoosterService(context);
                    try {
                        ServiceManager.addService("running_booster", runningBoosterService2.asBinder());
                        runningBoosterService = runningBoosterService2;
                        r2 = r22;
                        r62 = r63;
                    } catch (Throwable th52) {
                        th = th52;
                        runningBoosterService = runningBoosterService2;
                        reportWtf("starting RunningBoosterService", th);
                        r2 = r22;
                        r62 = r63;
                    }
                } catch (Throwable th53) {
                    th = th53;
                }
            }
        }
        if (!z5 && !z9) {
            this.mSystemServiceManager.startService(MediaProjectionManagerService.class);
        }
        if (context.getPackageManager().hasSystemFeature("android.hardware.type.watch")) {
            this.mSystemServiceManager.startService(WEAR_BLUETOOTH_SERVICE_CLASS);
        }
        try {
            Slog.i(TAG, "Benesse Extension Service");
            ServiceManager.addService("benesse_extension", new BenesseExtensionService(context));
        } catch (Throwable th54) {
            reportWtf("starting Benesse Extension Service", th54);
        }
        boolean zDetectSafeMode = r96.detectSafeMode();
        if (BenesseExtension.getDchaState() != 0) {
            zDetectSafeMode = false;
        }
        if (zDetectSafeMode) {
            this.mActivityManagerService.enterSafeMode();
            VMRuntime.getRuntime().disableJitCompilation();
        } else {
            VMRuntime.getRuntime().startJitCompilation();
        }
        final MmsServiceBroker mmsServiceBroker = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
        Trace.traceBegin(2097152L, "MakeVibratorServiceReady");
        try {
            r92.systemReady();
        } catch (Throwable th55) {
            reportWtf("making Vibrator Service ready", th55);
        }
        Trace.traceEnd(2097152L);
        Trace.traceBegin(2097152L, "MakeLockSettingsServiceReady");
        if (iLockSettingsAsInterface != null) {
            try {
                iLockSettingsAsInterface.systemReady();
            } catch (Throwable th56) {
                reportWtf("making Lock Settings Service ready", th56);
            }
        }
        Trace.traceEnd(2097152L);
        this.mSystemServiceManager.startBootPhase(SystemService.PHASE_LOCK_SETTINGS_READY);
        this.mSystemServiceManager.startBootPhase(SystemService.PHASE_SYSTEM_SERVICES_READY);
        Trace.traceBegin(2097152L, "MakeWindowManagerServiceReady");
        try {
            r96.systemReady();
        } catch (Throwable th57) {
            reportWtf("making Window Manager Service ready", th57);
        }
        Trace.traceEnd(2097152L);
        if (zDetectSafeMode) {
            this.mActivityManagerService.showSafeModeOverlay();
        }
        Configuration configurationComputeNewConfiguration = r96.computeNewConfiguration();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(displayMetrics);
        context.getResources().updateConfiguration(configurationComputeNewConfiguration, displayMetrics);
        Resources.Theme theme = context.getTheme();
        if (theme.getChangingConfigurations() != 0) {
            theme.rebase();
        }
        Trace.traceBegin(2097152L, "MakePowerManagerServiceReady");
        try {
            this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
            Trace.traceEnd(2097152L);
        } catch (Throwable th58) {
            reportWtf("making Power Manager Service ready", th58);
        }
        Trace.traceEnd(2097152L);
        Trace.traceBegin(2097152L, "MakePackageManagerServiceReady");
        try {
            this.mPackageManagerService.systemReady();
        } catch (Throwable th59) {
            reportWtf("making Package Manager Service ready", th59);
        }
        Trace.traceEnd(2097152L);
        Trace.traceBegin(2097152L, "MakeDisplayManagerServiceReady");
        try {
            this.mDisplayManagerService.systemReady(zDetectSafeMode, this.mOnlyCore);
        } catch (Throwable th60) {
            reportWtf("making Display Manager Service ready", th60);
        }
        Trace.traceEnd(2097152L);
        final INetworkManagementService iNetworkManagementService = iNetworkManagementServiceCreate;
        final ?? r13 = r62;
        final ?? r14 = r2;
        final ?? r15 = stub2;
        final ?? r11 = stub3;
        final ?? r16 = stub5;
        final ?? r17 = stub6;
        final NetworkTimeUpdateService networkTimeUpdateService3 = networkTimeUpdateService;
        final CommonTimeManagementService commonTimeManagementService3 = commonTimeManagementService;
        final ?? r20 = stub7;
        final ?? r21 = r60;
        final ?? r222 = stub4;
        final ?? r232 = stub8;
        final IPerfServiceManager iPerfServiceManager2 = iPerfServiceManager;
        final RunningBoosterService runningBoosterService3 = runningBoosterService;
        this.mActivityManagerService.systemReady(new Runnable() {
            @Override
            public void run() {
                Slog.i(SystemServer.TAG, "Making services ready");
                SystemServer.this.mSystemServiceManager.startBootPhase(SystemService.PHASE_ACTIVITY_MANAGER_READY);
                Trace.traceBegin(2097152L, "PhaseActivityManagerReady");
                Trace.traceBegin(2097152L, "StartObservingNativeCrashes");
                try {
                    SystemServer.this.mActivityManagerService.startObservingNativeCrashes();
                } catch (Throwable e7) {
                    SystemServer.this.reportWtf("observing native crashes", e7);
                }
                Trace.traceEnd(2097152L);
                if (!SystemServer.this.mOnlyCore) {
                    Slog.i(SystemServer.TAG, "WebViewFactory preparation");
                    Trace.traceBegin(2097152L, "WebViewFactoryPreparation");
                    SystemServer.this.mWebViewUpdateService.prepareWebViewInSystemServer();
                    Trace.traceEnd(2097152L);
                }
                Trace.traceBegin(2097152L, "StartSystemUI");
                try {
                    SystemServer.startSystemUi(context);
                } catch (Throwable e8) {
                    SystemServer.this.reportWtf("starting System UI", e8);
                }
                Trace.traceEnd(2097152L);
                Trace.traceBegin(2097152L, "MakeNetworkScoreReady");
                try {
                    if (r11 != null) {
                        r11.systemReady();
                    }
                } catch (Throwable e9) {
                    SystemServer.this.reportWtf("making Network Score Service ready", e9);
                }
                Trace.traceEnd(2097152L);
                Trace.traceBegin(2097152L, "MakeNetworkManagementServiceReady");
                try {
                    if (iNetworkManagementService != null) {
                        iNetworkManagementService.systemReady();
                    }
                } catch (Throwable e10) {
                    SystemServer.this.reportWtf("making Network Managment Service ready", e10);
                }
                Trace.traceEnd(2097152L);
                SystemServer.addBootEvent("SystemServer:NetworkManagementService systemReady");
                Trace.traceBegin(2097152L, "MakeNetworkStatsServiceReady");
                try {
                    if (r13 != null) {
                        r13.systemReady();
                    }
                } catch (Throwable e11) {
                    SystemServer.this.reportWtf("making Network Stats Service ready", e11);
                }
                Trace.traceEnd(2097152L);
                SystemServer.addBootEvent("SystemServer:NetworkStatsService systemReady");
                Trace.traceBegin(2097152L, "MakeNetworkPolicyServiceReady");
                try {
                    if (r14 != null) {
                        r14.systemReady();
                    }
                } catch (Throwable e12) {
                    SystemServer.this.reportWtf("making Network Policy Service ready", e12);
                }
                Trace.traceEnd(2097152L);
                SystemServer.addBootEvent("SystemServer:NetworkPolicyManagerService systemReady");
                Trace.traceBegin(2097152L, "MakeConnectivityServiceReady");
                try {
                    if (r15 != null) {
                        r15.systemReady();
                    }
                } catch (Throwable e13) {
                    SystemServer.this.reportWtf("making Connectivity Service ready", e13);
                }
                Trace.traceEnd(2097152L);
                SystemServer.addBootEvent("SystemServer:ConnectivityService systemReady");
                Watchdog.getInstance().start();
                Trace.traceEnd(2097152L);
                Trace.traceBegin(2097152L, "PhaseThirdPartyAppsCanStart");
                SystemServer.this.mSystemServiceManager.startBootPhase(600);
                try {
                    if (r16 != null) {
                        r16.systemRunning();
                    }
                } catch (Throwable e14) {
                    SystemServer.this.reportWtf("Notifying Location Service running", e14);
                }
                if (!(!"user".equals(Build.TYPE) ? "userdebug".equals(Build.TYPE) : true)) {
                    SystemServer.this.testSystemServer(context);
                }
                try {
                    if (r17 != null) {
                        r17.systemRunning();
                    }
                } catch (Throwable e15) {
                    SystemServer.this.reportWtf("Notifying CountryDetectorService running", e15);
                }
                try {
                    if (networkTimeUpdateService3 != null) {
                        networkTimeUpdateService3.systemRunning();
                    }
                } catch (Throwable e16) {
                    SystemServer.this.reportWtf("Notifying NetworkTimeService running", e16);
                }
                try {
                    if (commonTimeManagementService3 != null) {
                        commonTimeManagementService3.systemRunning();
                    }
                } catch (Throwable e17) {
                    SystemServer.this.reportWtf("Notifying CommonTimeManagementService running", e17);
                }
                try {
                    if (r20 != null) {
                        r20.systemRunning();
                    }
                } catch (Throwable e18) {
                    SystemServer.this.reportWtf("Notifying AssetAtlasService running", e18);
                }
                try {
                    if (r21 != null) {
                        r21.systemRunning();
                    }
                } catch (Throwable e19) {
                    SystemServer.this.reportWtf("Notifying InputManagerService running", e19);
                }
                try {
                    if (r222 != null) {
                        r222.systemRunning();
                    }
                } catch (Throwable e20) {
                    SystemServer.this.reportWtf("Notifying TelephonyRegistry running", e20);
                }
                try {
                    if (r232 != null) {
                        r232.systemRunning();
                    }
                } catch (Throwable e21) {
                    SystemServer.this.reportWtf("Notifying MediaRouterService running", e21);
                }
                try {
                    if (mmsServiceBroker != null) {
                        mmsServiceBroker.systemRunning();
                    }
                } catch (Throwable e22) {
                    SystemServer.this.reportWtf("Notifying MmsService running", e22);
                }
                try {
                    if (r11 != null) {
                        r11.systemRunning();
                    }
                } catch (Throwable e23) {
                    SystemServer.this.reportWtf("Notifying NetworkScoreService running", e23);
                }
                Trace.traceEnd(2097152L);
                SystemServer.addBootEvent("SystemServer:PhaseThirdPartyAppsCanStart");
                if (SystemProperties.get("ro.mtk_perfservice_support").equals(SystemServer.ENCRYPTED_STATE)) {
                    try {
                        Trace.traceBegin(2097152L, "MakePerfServiceReady");
                        if (iPerfServiceManager2 != null) {
                            iPerfServiceManager2.systemReady();
                        }
                        Trace.traceEnd(2097152L);
                    } catch (Throwable e24) {
                        SystemServer.this.reportWtf("making PerfServiceManager ready", e24);
                    }
                }
                try {
                    if (runningBoosterService3 != null) {
                        runningBoosterService3.systemRunning();
                    }
                } catch (Throwable e25) {
                    SystemServer.this.reportWtf("Notifying RunningBoosterService running", e25);
                }
            }
        });
    }

    final void testSystemServer(Context context) {
        IntentFilter testFilter = new IntentFilter();
        BroadcastReceiver broadcastReceiver = null;
        if (SystemProperties.get("persist.sys.anr_sys_key").equals(ENCRYPTED_STATE)) {
            testFilter.addAction("android.intent.action.BOOT_COMPLETED");
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(final Context context2, Intent intent) {
                    if (intent.getAction() != "android.intent.action.BOOT_COMPLETED") {
                        return;
                    }
                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            Log.i("ANR_DEBUG", "=== Start BadService2 ===");
                            Intent intent2 = new Intent("com.android.badservicesysserver");
                            intent2.setPackage("com.android.badservicesysserver");
                            ComponentName ret = context2.startService(intent2);
                            if (ret != null) {
                                Log.i("ANR_DEBUG", "=== result to start BadService2 === Name: " + ret.toString());
                            } else {
                                Log.i("ANR_DEBUG", "=== result to start BadService2 === Name: Null ");
                            }
                        }
                    });
                }
            };
        } else if (SystemProperties.get("persist.sys.test_system_service").equals(ENCRYPTED_STATE)) {
            testFilter.addAction("mediatek.intent.action.TEST_SERVICE");
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context2, Intent intent) {
                    String serviceName;
                    if (!intent.getAction().equals("mediatek.intent.action.TEST_SERVICE") || (serviceName = intent.getStringExtra("SERVICE_NAME")) == null) {
                        return;
                    }
                    try {
                        SystemServer.this.mSystemServiceManager.startService(serviceName);
                    } catch (Throwable e) {
                        SystemServer.this.reportWtf("starting" + serviceName + " Service", e);
                    }
                }
            };
        }
        if (broadcastReceiver == null) {
            return;
        }
        context.registerReceiver(broadcastReceiver, testFilter);
    }

    static final void startSystemUi(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.systemui", "com.android.systemui.SystemUIService"));
        intent.addFlags(256);
        context.startServiceAsUser(intent, UserHandle.SYSTEM);
    }

    private static void traceBeginAndSlog(String name) {
        Trace.traceBegin(2097152L, name);
        Slog.i(TAG, name);
    }
}
