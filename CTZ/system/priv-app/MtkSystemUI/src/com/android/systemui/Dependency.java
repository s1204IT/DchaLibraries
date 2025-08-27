package com.android.systemui;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.ServiceManager;
import android.util.ArrayMap;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.ColorDisplayController;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.util.Preconditions;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.colorextraction.SysuiColorExtractor;
import com.android.systemui.fragments.FragmentService;
import com.android.systemui.keyguard.ScreenLifecycle;
import com.android.systemui.keyguard.WakefulnessLifecycle;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.PluginDependencyProvider;
import com.android.systemui.plugins.PluginManager;
import com.android.systemui.plugins.PluginManagerImpl;
import com.android.systemui.plugins.VolumeDialogController;
import com.android.systemui.power.EnhancedEstimates;
import com.android.systemui.power.EnhancedEstimatesImpl;
import com.android.systemui.power.PowerNotificationWarnings;
import com.android.systemui.power.PowerUI;
import com.android.systemui.statusbar.AppOpsListener;
import com.android.systemui.statusbar.VibratorHelper;
import com.android.systemui.statusbar.phone.ConfigurationControllerImpl;
import com.android.systemui.statusbar.phone.DarkIconDispatcherImpl;
import com.android.systemui.statusbar.phone.LightBarController;
import com.android.systemui.statusbar.phone.ManagedProfileController;
import com.android.systemui.statusbar.phone.ManagedProfileControllerImpl;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.phone.StatusBarIconControllerImpl;
import com.android.systemui.statusbar.phone.StatusBarWindowManager;
import com.android.systemui.statusbar.policy.AccessibilityController;
import com.android.systemui.statusbar.policy.AccessibilityManagerWrapper;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BatteryControllerImpl;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.BluetoothControllerImpl;
import com.android.systemui.statusbar.policy.CastController;
import com.android.systemui.statusbar.policy.CastControllerImpl;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import com.android.systemui.statusbar.policy.DataSaverController;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.DeviceProvisionedControllerImpl;
import com.android.systemui.statusbar.policy.ExtensionController;
import com.android.systemui.statusbar.policy.ExtensionControllerImpl;
import com.android.systemui.statusbar.policy.FlashlightController;
import com.android.systemui.statusbar.policy.FlashlightControllerImpl;
import com.android.systemui.statusbar.policy.HotspotController;
import com.android.systemui.statusbar.policy.HotspotControllerImpl;
import com.android.systemui.statusbar.policy.IconLogger;
import com.android.systemui.statusbar.policy.IconLoggerImpl;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.KeyguardMonitorImpl;
import com.android.systemui.statusbar.policy.LocationController;
import com.android.systemui.statusbar.policy.LocationControllerImpl;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkControllerImpl;
import com.android.systemui.statusbar.policy.NextAlarmController;
import com.android.systemui.statusbar.policy.NextAlarmControllerImpl;
import com.android.systemui.statusbar.policy.RotationLockController;
import com.android.systemui.statusbar.policy.RotationLockControllerImpl;
import com.android.systemui.statusbar.policy.SecurityController;
import com.android.systemui.statusbar.policy.SecurityControllerImpl;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.policy.UserInfoControllerImpl;
import com.android.systemui.statusbar.policy.UserSwitcherController;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.statusbar.policy.ZenModeControllerImpl;
import com.android.systemui.tuner.TunablePadding;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.tuner.TunerServiceImpl;
import com.android.systemui.util.AsyncSensorManager;
import com.android.systemui.util.leak.GarbageMonitor;
import com.android.systemui.util.leak.LeakDetector;
import com.android.systemui.util.leak.LeakReporter;
import com.android.systemui.volume.VolumeDialogControllerImpl;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

/* loaded from: classes.dex */
public class Dependency extends SystemUI {
    private static Dependency sDependency;
    private final ArrayMap<Object, Object> mDependencies = new ArrayMap<>();
    private final ArrayMap<Object, DependencyProvider> mProviders = new ArrayMap<>();
    public static final DependencyKey<Looper> BG_LOOPER = new DependencyKey<>("background_looper");
    public static final DependencyKey<Handler> TIME_TICK_HANDLER = new DependencyKey<>("time_tick_handler");
    public static final DependencyKey<Handler> MAIN_HANDLER = new DependencyKey<>("main_handler");
    public static final DependencyKey<String> LEAK_REPORT_EMAIL = new DependencyKey<>("leak_report_email");

    public interface DependencyProvider<T> {
        T createDependency();
    }

    @Override // com.android.systemui.SystemUI
    public void start() {
        this.mProviders.put(TIME_TICK_HANDLER, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$kBVF3uQcq1aY_iBb0icDCYhmBoA
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$0();
            }
        });
        this.mProviders.put(BG_LOOPER, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$OPMs5tFKc41dcTd5aBaiMx5V8Jk
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$1();
            }
        });
        this.mProviders.put(MAIN_HANDLER, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$qMSnn_DLwc6UCaHtdUEsjaI9uJg
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$2();
            }
        });
        this.mProviders.put(ActivityStarter.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$qEfhMIObaWUB4JUpS1kyRh1wvtk
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$3();
            }
        });
        this.mProviders.put(ActivityStarterDelegate.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$YD3dFPIT9OLLCV0VFjYTtnEZZWg
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$4(this.f$0);
            }
        });
        this.mProviders.put(AsyncSensorManager.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$0H2oe1HD8YElVF7xZWH_GrR9Fus
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$5(this.f$0);
            }
        });
        this.mProviders.put(BluetoothController.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$Gy4QudNezotljEgQKa6AZ5wLN8g
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$6(this.f$0);
            }
        });
        this.mProviders.put(LocationController.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$vhpQrWxDbweIViML-8LCC1Ml6HA
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$7(this.f$0);
            }
        });
        this.mProviders.put(RotationLockController.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$Cqshp7K51OogqPsBzd-8WkWLscw
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$8(this.f$0);
            }
        });
        this.mProviders.put(NetworkController.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$sXmtIDKunu8wBZvqigyneB02fuU
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$9(this.f$0);
            }
        });
        this.mProviders.put(ZenModeController.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$MqeklNs0Y4sjPiwGncegTIKljdU
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$10(this.f$0);
            }
        });
        this.mProviders.put(HotspotController.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$bE-oHFBo0SQuS0prD0vCrQd97eU
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$11(this.f$0);
            }
        });
        this.mProviders.put(CastController.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$JOKZfAg6ZDkkuCsSsy35IBCARTA
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$12(this.f$0);
            }
        });
        this.mProviders.put(FlashlightController.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$oOQ0donQppauaJPERDAkKBaeXo8
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$13(this.f$0);
            }
        });
        this.mProviders.put(KeyguardMonitor.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$3n6-QJ1ZEPH6TMbkEd7wabHPggc
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$14(this.f$0);
            }
        });
        this.mProviders.put(UserSwitcherController.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$3ZrNl_prM_QqXnWtMCTUroZBqig
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$15(this.f$0);
            }
        });
        this.mProviders.put(UserInfoController.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$R9x9Mcq_hw4LFHdUOV1HoHSjDFY
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$16(this.f$0);
            }
        });
        this.mProviders.put(BatteryController.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$eVB3qUthhFg102GpQdjdNlDgpHI
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$17(this.f$0);
            }
        });
        this.mProviders.put(ColorDisplayController.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$sN7_PX5fS0mTAWfUtAOWiOYYsEw
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$18(this.f$0);
            }
        });
        this.mProviders.put(ManagedProfileController.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$Mc2Shc0BcQYa_D2DsNwT5hqVOkg
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$19(this.f$0);
            }
        });
        this.mProviders.put(NextAlarmController.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$8AJ7IdA5m7Auk6hpJZHZYOfed1g
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$20(this.f$0);
            }
        });
        this.mProviders.put(DataSaverController.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$LnsjDzhCyDXdveXQDCR8F1i775w
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return ((NetworkController) Dependency.get(NetworkController.class)).getDataSaverController();
            }
        });
        this.mProviders.put(AccessibilityController.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$ZJP4QYkVPngEF6sUOIH8Lf0Fxw8
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$22(this.f$0);
            }
        });
        this.mProviders.put(DeviceProvisionedController.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$F2PwPFK8ZYOsuPFjafNl1Rs3pss
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$23(this.f$0);
            }
        });
        this.mProviders.put(PluginManager.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$Rf9lPinWct-b4zmu1RmuBA1cyzQ
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$24(this.f$0);
            }
        });
        this.mProviders.put(AssistManager.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$bjwY1_gMs7pb_0dTeSR6EhvnTDY
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$25(this.f$0);
            }
        });
        this.mProviders.put(SecurityController.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$FS-BHneeFfLq-XLo_OH5s3NDjq4
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$26(this.f$0);
            }
        });
        this.mProviders.put(LeakDetector.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$u_O28tKtf6m63SoPms2fLLKgf0U
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return LeakDetector.create();
            }
        });
        this.mProviders.put(LEAK_REPORT_EMAIL, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$OuWnlhRSFZb_UXefM7psZTirfzM
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$27();
            }
        });
        this.mProviders.put(LeakReporter.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$TzkdpsFpcokB9wOdq8_AL394wXI
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$28(this.f$0);
            }
        });
        this.mProviders.put(GarbageMonitor.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$ObCj7gbBEIdh7uotev0wUsDF-gs
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$29(this.f$0);
            }
        });
        this.mProviders.put(TunerService.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$QeSLOyPvKnxd4T4ZD6vNH_c8Vsk
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$30(this.f$0);
            }
        });
        this.mProviders.put(StatusBarWindowManager.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$SfpVegTBkHb9tOvXbeeDUXrzjtM
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$31(this.f$0);
            }
        });
        this.mProviders.put(DarkIconDispatcher.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$fk0IwG2aHV8HxJ2AG1DMnwxab4Y
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$32(this.f$0);
            }
        });
        this.mProviders.put(ConfigurationController.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$QZywXZS1w5xNhq0ThGkucw65zmw
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$33(this.f$0);
            }
        });
        this.mProviders.put(StatusBarIconController.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$u55GtmTwAT7rU__EQu5suMFE5Gk
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$34(this.f$0);
            }
        });
        this.mProviders.put(ScreenLifecycle.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$QunsbUyDkSTDqQ3J4kJXl60dFCs
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$35();
            }
        });
        this.mProviders.put(WakefulnessLifecycle.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$WPtwQQqVU6m1ifaO5rX2-zG3-Ok
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$36();
            }
        });
        this.mProviders.put(FragmentService.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$uCOraqIWfgaprFSzzqWhj1-gO30
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$37(this.f$0);
            }
        });
        this.mProviders.put(ExtensionController.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$OdqeWGmU3r9_3T8q2CUebkYRzKg
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$38(this.f$0);
            }
        });
        this.mProviders.put(PluginDependencyProvider.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$mPl0hCzOT52s_1iFpnvMri2oLWc
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$39();
            }
        });
        this.mProviders.put(LocalBluetoothManager.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$RVNdlgnkHnvqc-7IeeJkxsAw71Y
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return LocalBluetoothManager.getInstance(this.f$0.mContext, null);
            }
        });
        this.mProviders.put(VolumeDialogController.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$ls_TAyxiP1x3eCUsnULK7QhAD1A
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$41(this.f$0);
            }
        });
        this.mProviders.put(MetricsLogger.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$Lgnz5tvjLnGXZv1_9doKGIuk72U
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$42();
            }
        });
        this.mProviders.put(AccessibilityManagerWrapper.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$3dskHwqhnk7rl6uWrynTBOpqoso
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$43(this.f$0);
            }
        });
        this.mProviders.put(SysuiColorExtractor.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$2EP6AlwVDwhJzblZCh1s1Kry3Yc
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$44(this.f$0);
            }
        });
        this.mProviders.put(TunablePadding.TunablePaddingService.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$SmuD-tT2siPWGoltmIITupDKFcI
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$45();
            }
        });
        this.mProviders.put(ForegroundServiceController.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$2AIelcCUvrZwK3gYlXHEDjszYXo
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$46(this.f$0);
            }
        });
        this.mProviders.put(UiOffloadThread.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$LSvgWTdQP87rDjd24R3t79hv97w
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return new UiOffloadThread();
            }
        });
        this.mProviders.put(PowerUI.WarningsUI.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$e-mzOcMSKyC2QbVIh_F62iw7WG8
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$47(this.f$0);
            }
        });
        this.mProviders.put(IconLogger.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$8d8ig7vA4dwKAi4m3UtJ-Z6-PlY
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$48(this.f$0);
            }
        });
        this.mProviders.put(LightBarController.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$tkMMgMiU67KrMuzMbk7S3dN7VvI
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$49(this.f$0);
            }
        });
        this.mProviders.put(IWindowManager.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$oi9PlpgtZI6Kz81pWN68RRJldvc
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return WindowManagerGlobal.getWindowManagerService();
            }
        });
        this.mProviders.put(OverviewProxyService.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$SOM1e6JLs0G26jRrrkR2E4IG8oA
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$51(this.f$0);
            }
        });
        this.mProviders.put(EnhancedEstimates.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$raxiz2FnXijKOrBPKw9rTGb9hMM
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$52();
            }
        });
        this.mProviders.put(AppOpsListener.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$nXKCbmpz8yBWU1XC5ocPwPoCMew
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$53(this.f$0);
            }
        });
        this.mProviders.put(VibratorHelper.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$LfG73zT4wzmAr13SlabcbmGRhQg
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return Dependency.lambda$start$54(this.f$0);
            }
        });
        this.mProviders.put(IStatusBarService.class, new DependencyProvider() { // from class: com.android.systemui.-$$Lambda$Dependency$YlrCQnToKQLXsi6GJYR6UeEdAHE
            @Override // com.android.systemui.Dependency.DependencyProvider
            public final Object createDependency() {
                return IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
            }
        });
        SystemUIFactory.getInstance().injectDependencies(this.mProviders, this.mContext);
        sDependency = this;
    }

    static /* synthetic */ Object lambda$start$0() {
        HandlerThread handlerThread = new HandlerThread("TimeTick");
        handlerThread.start();
        return new Handler(handlerThread.getLooper());
    }

    static /* synthetic */ Object lambda$start$1() {
        HandlerThread handlerThread = new HandlerThread("SysUiBg", 10);
        handlerThread.start();
        return handlerThread.getLooper();
    }

    static /* synthetic */ Object lambda$start$2() {
        return new Handler(Looper.getMainLooper());
    }

    static /* synthetic */ Object lambda$start$3() {
        return new ActivityStarterDelegate();
    }

    public static /* synthetic */ Object lambda$start$4(Dependency dependency) {
        return (ActivityStarter) dependency.getDependency(ActivityStarter.class);
    }

    public static /* synthetic */ Object lambda$start$5(Dependency dependency) {
        return new AsyncSensorManager((SensorManager) dependency.mContext.getSystemService(SensorManager.class));
    }

    public static /* synthetic */ Object lambda$start$6(Dependency dependency) {
        return new BluetoothControllerImpl(dependency.mContext, (Looper) dependency.getDependency(BG_LOOPER));
    }

    public static /* synthetic */ Object lambda$start$7(Dependency dependency) {
        return new LocationControllerImpl(dependency.mContext, (Looper) dependency.getDependency(BG_LOOPER));
    }

    public static /* synthetic */ Object lambda$start$8(Dependency dependency) {
        return new RotationLockControllerImpl(dependency.mContext);
    }

    public static /* synthetic */ Object lambda$start$9(Dependency dependency) {
        return new NetworkControllerImpl(dependency.mContext, (Looper) dependency.getDependency(BG_LOOPER), (DeviceProvisionedController) dependency.getDependency(DeviceProvisionedController.class));
    }

    public static /* synthetic */ Object lambda$start$10(Dependency dependency) {
        return new ZenModeControllerImpl(dependency.mContext, (Handler) dependency.getDependency(MAIN_HANDLER));
    }

    public static /* synthetic */ Object lambda$start$11(Dependency dependency) {
        return new HotspotControllerImpl(dependency.mContext);
    }

    public static /* synthetic */ Object lambda$start$12(Dependency dependency) {
        return new CastControllerImpl(dependency.mContext);
    }

    public static /* synthetic */ Object lambda$start$13(Dependency dependency) {
        return new FlashlightControllerImpl(dependency.mContext);
    }

    public static /* synthetic */ Object lambda$start$14(Dependency dependency) {
        return new KeyguardMonitorImpl(dependency.mContext);
    }

    public static /* synthetic */ Object lambda$start$15(Dependency dependency) {
        return new UserSwitcherController(dependency.mContext, (KeyguardMonitor) dependency.getDependency(KeyguardMonitor.class), (Handler) dependency.getDependency(MAIN_HANDLER), (ActivityStarter) dependency.getDependency(ActivityStarter.class));
    }

    public static /* synthetic */ Object lambda$start$16(Dependency dependency) {
        return new UserInfoControllerImpl(dependency.mContext);
    }

    public static /* synthetic */ Object lambda$start$17(Dependency dependency) {
        return new BatteryControllerImpl(dependency.mContext);
    }

    public static /* synthetic */ Object lambda$start$18(Dependency dependency) {
        return new ColorDisplayController(dependency.mContext);
    }

    public static /* synthetic */ Object lambda$start$19(Dependency dependency) {
        return new ManagedProfileControllerImpl(dependency.mContext);
    }

    public static /* synthetic */ Object lambda$start$20(Dependency dependency) {
        return new NextAlarmControllerImpl(dependency.mContext);
    }

    public static /* synthetic */ Object lambda$start$22(Dependency dependency) {
        return new AccessibilityController(dependency.mContext);
    }

    public static /* synthetic */ Object lambda$start$23(Dependency dependency) {
        return new DeviceProvisionedControllerImpl(dependency.mContext);
    }

    public static /* synthetic */ Object lambda$start$24(Dependency dependency) {
        return new PluginManagerImpl(dependency.mContext);
    }

    public static /* synthetic */ Object lambda$start$25(Dependency dependency) {
        return new AssistManager((DeviceProvisionedController) dependency.getDependency(DeviceProvisionedController.class), dependency.mContext);
    }

    public static /* synthetic */ Object lambda$start$26(Dependency dependency) {
        return new SecurityControllerImpl(dependency.mContext);
    }

    static /* synthetic */ Object lambda$start$27() {
        return null;
    }

    public static /* synthetic */ Object lambda$start$28(Dependency dependency) {
        return new LeakReporter(dependency.mContext, (LeakDetector) dependency.getDependency(LeakDetector.class), (String) dependency.getDependency(LEAK_REPORT_EMAIL));
    }

    public static /* synthetic */ Object lambda$start$29(Dependency dependency) {
        return new GarbageMonitor(dependency.mContext, (Looper) dependency.getDependency(BG_LOOPER), (LeakDetector) dependency.getDependency(LeakDetector.class), (LeakReporter) dependency.getDependency(LeakReporter.class));
    }

    public static /* synthetic */ Object lambda$start$30(Dependency dependency) {
        return new TunerServiceImpl(dependency.mContext);
    }

    public static /* synthetic */ Object lambda$start$31(Dependency dependency) {
        return new StatusBarWindowManager(dependency.mContext);
    }

    public static /* synthetic */ Object lambda$start$32(Dependency dependency) {
        return new DarkIconDispatcherImpl(dependency.mContext);
    }

    public static /* synthetic */ Object lambda$start$33(Dependency dependency) {
        return new ConfigurationControllerImpl(dependency.mContext);
    }

    public static /* synthetic */ Object lambda$start$34(Dependency dependency) {
        return new StatusBarIconControllerImpl(dependency.mContext);
    }

    static /* synthetic */ Object lambda$start$35() {
        return new ScreenLifecycle();
    }

    static /* synthetic */ Object lambda$start$36() {
        return new WakefulnessLifecycle();
    }

    public static /* synthetic */ Object lambda$start$37(Dependency dependency) {
        return new FragmentService(dependency.mContext);
    }

    public static /* synthetic */ Object lambda$start$38(Dependency dependency) {
        return new ExtensionControllerImpl(dependency.mContext);
    }

    static /* synthetic */ Object lambda$start$39() {
        return new PluginDependencyProvider((PluginManager) get(PluginManager.class));
    }

    public static /* synthetic */ Object lambda$start$41(Dependency dependency) {
        return new VolumeDialogControllerImpl(dependency.mContext);
    }

    static /* synthetic */ Object lambda$start$42() {
        return new MetricsLogger();
    }

    public static /* synthetic */ Object lambda$start$43(Dependency dependency) {
        return new AccessibilityManagerWrapper(dependency.mContext);
    }

    public static /* synthetic */ Object lambda$start$44(Dependency dependency) {
        return new SysuiColorExtractor(dependency.mContext);
    }

    static /* synthetic */ Object lambda$start$45() {
        return new TunablePadding.TunablePaddingService();
    }

    public static /* synthetic */ Object lambda$start$46(Dependency dependency) {
        return new ForegroundServiceControllerImpl(dependency.mContext);
    }

    public static /* synthetic */ Object lambda$start$47(Dependency dependency) {
        return new PowerNotificationWarnings(dependency.mContext);
    }

    public static /* synthetic */ Object lambda$start$48(Dependency dependency) {
        return new IconLoggerImpl(dependency.mContext, (Looper) dependency.getDependency(BG_LOOPER), (MetricsLogger) dependency.getDependency(MetricsLogger.class));
    }

    public static /* synthetic */ Object lambda$start$49(Dependency dependency) {
        return new LightBarController(dependency.mContext);
    }

    public static /* synthetic */ Object lambda$start$51(Dependency dependency) {
        return new OverviewProxyService(dependency.mContext);
    }

    static /* synthetic */ Object lambda$start$52() {
        return new EnhancedEstimatesImpl();
    }

    public static /* synthetic */ Object lambda$start$53(Dependency dependency) {
        return new AppOpsListener(dependency.mContext);
    }

    public static /* synthetic */ Object lambda$start$54(Dependency dependency) {
        return new VibratorHelper(dependency.mContext);
    }

    @Override // com.android.systemui.SystemUI
    public synchronized void dump(final FileDescriptor fileDescriptor, final PrintWriter printWriter, final String[] strArr) {
        super.dump(fileDescriptor, printWriter, strArr);
        printWriter.println("Dumping existing controllers:");
        this.mDependencies.values().stream().filter(new Predicate() { // from class: com.android.systemui.-$$Lambda$Dependency$dBguyfTWAn7ZIVTYEL-rJD-V1Ww
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return Dependency.lambda$dump$56(obj);
            }
        }).forEach(new Consumer() { // from class: com.android.systemui.-$$Lambda$Dependency$U1mWxo413ICs6yOan-sz-9kxG6E
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((Dumpable) obj).dump(fileDescriptor, printWriter, strArr);
            }
        });
    }

    static /* synthetic */ boolean lambda$dump$56(Object obj) {
        return obj instanceof Dumpable;
    }

    @Override // com.android.systemui.SystemUI
    protected synchronized void onConfigurationChanged(final Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mDependencies.values().stream().filter(new Predicate() { // from class: com.android.systemui.-$$Lambda$Dependency$Ma05gsMMbRDr3AMGfZ5wtGQFpwU
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return Dependency.lambda$onConfigurationChanged$58(obj);
            }
        }).forEach(new Consumer() { // from class: com.android.systemui.-$$Lambda$Dependency$qCqLAuR_HWCO0OdZ4yGmN24jCxU
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((ConfigurationChangedReceiver) obj).onConfigurationChanged(configuration);
            }
        });
    }

    static /* synthetic */ boolean lambda$onConfigurationChanged$58(Object obj) {
        return obj instanceof ConfigurationChangedReceiver;
    }

    protected final <T> T getDependency(Class<T> cls) {
        return (T) getDependencyInner(cls);
    }

    protected final <T> T getDependency(DependencyKey<T> dependencyKey) {
        return (T) getDependencyInner(dependencyKey);
    }

    private synchronized <T> T getDependencyInner(Object obj) {
        T t;
        t = (T) this.mDependencies.get(obj);
        if (t == null) {
            t = (T) createDependency(obj);
            this.mDependencies.put(obj, t);
        }
        return t;
    }

    @VisibleForTesting
    protected <T> T createDependency(Object obj) {
        Preconditions.checkArgument((obj instanceof DependencyKey) || (obj instanceof Class));
        DependencyProvider dependencyProvider = this.mProviders.get(obj);
        if (dependencyProvider == null) {
            throw new IllegalArgumentException("Unsupported dependency " + obj + ". " + this.mProviders.size() + " providers known.");
        }
        return (T) dependencyProvider.createDependency();
    }

    /* JADX DEBUG: Multi-variable search result rejected for r3v0, resolved type: java.util.function.Consumer<T> */
    /* JADX WARN: Multi-variable type inference failed */
    private <T> void destroyDependency(Class<T> cls, Consumer<T> consumer) {
        Object objRemove = this.mDependencies.remove(cls);
        if (objRemove != null && consumer != 0) {
            consumer.accept(objRemove);
        }
    }

    public static void initDependencies(Context context) {
        if (sDependency != null) {
            return;
        }
        Dependency dependency = new Dependency();
        dependency.mContext = context;
        dependency.mComponents = new HashMap();
        dependency.start();
    }

    public static void clearDependencies() {
        sDependency = null;
    }

    public static <T> void destroy(Class<T> cls, Consumer<T> consumer) {
        sDependency.destroyDependency(cls, consumer);
    }

    public static <T> T get(Class<T> cls) {
        return (T) sDependency.getDependency(cls);
    }

    public static <T> T get(DependencyKey<T> dependencyKey) {
        return (T) sDependency.getDependency(dependencyKey);
    }

    public static final class DependencyKey<V> {
        private final String mDisplayName;

        public DependencyKey(String str) {
            this.mDisplayName = str;
        }

        public String toString() {
            return this.mDisplayName;
        }
    }
}
