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

    @Override
    public void start() {
        this.mProviders.put(TIME_TICK_HANDLER, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$0();
            }
        });
        this.mProviders.put(BG_LOOPER, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$1();
            }
        });
        this.mProviders.put(MAIN_HANDLER, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$2();
            }
        });
        this.mProviders.put(ActivityStarter.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$3();
            }
        });
        this.mProviders.put(ActivityStarterDelegate.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$4(this.f$0);
            }
        });
        this.mProviders.put(AsyncSensorManager.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$5(this.f$0);
            }
        });
        this.mProviders.put(BluetoothController.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$6(this.f$0);
            }
        });
        this.mProviders.put(LocationController.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$7(this.f$0);
            }
        });
        this.mProviders.put(RotationLockController.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$8(this.f$0);
            }
        });
        this.mProviders.put(NetworkController.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$9(this.f$0);
            }
        });
        this.mProviders.put(ZenModeController.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$10(this.f$0);
            }
        });
        this.mProviders.put(HotspotController.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$11(this.f$0);
            }
        });
        this.mProviders.put(CastController.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$12(this.f$0);
            }
        });
        this.mProviders.put(FlashlightController.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$13(this.f$0);
            }
        });
        this.mProviders.put(KeyguardMonitor.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$14(this.f$0);
            }
        });
        this.mProviders.put(UserSwitcherController.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$15(this.f$0);
            }
        });
        this.mProviders.put(UserInfoController.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$16(this.f$0);
            }
        });
        this.mProviders.put(BatteryController.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$17(this.f$0);
            }
        });
        this.mProviders.put(ColorDisplayController.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$18(this.f$0);
            }
        });
        this.mProviders.put(ManagedProfileController.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$19(this.f$0);
            }
        });
        this.mProviders.put(NextAlarmController.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$20(this.f$0);
            }
        });
        this.mProviders.put(DataSaverController.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return ((NetworkController) Dependency.get(NetworkController.class)).getDataSaverController();
            }
        });
        this.mProviders.put(AccessibilityController.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$22(this.f$0);
            }
        });
        this.mProviders.put(DeviceProvisionedController.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$23(this.f$0);
            }
        });
        this.mProviders.put(PluginManager.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$24(this.f$0);
            }
        });
        this.mProviders.put(AssistManager.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$25(this.f$0);
            }
        });
        this.mProviders.put(SecurityController.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$26(this.f$0);
            }
        });
        this.mProviders.put(LeakDetector.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return LeakDetector.create();
            }
        });
        this.mProviders.put(LEAK_REPORT_EMAIL, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$27();
            }
        });
        this.mProviders.put(LeakReporter.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$28(this.f$0);
            }
        });
        this.mProviders.put(GarbageMonitor.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$29(this.f$0);
            }
        });
        this.mProviders.put(TunerService.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$30(this.f$0);
            }
        });
        this.mProviders.put(StatusBarWindowManager.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$31(this.f$0);
            }
        });
        this.mProviders.put(DarkIconDispatcher.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$32(this.f$0);
            }
        });
        this.mProviders.put(ConfigurationController.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$33(this.f$0);
            }
        });
        this.mProviders.put(StatusBarIconController.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$34(this.f$0);
            }
        });
        this.mProviders.put(ScreenLifecycle.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$35();
            }
        });
        this.mProviders.put(WakefulnessLifecycle.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$36();
            }
        });
        this.mProviders.put(FragmentService.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$37(this.f$0);
            }
        });
        this.mProviders.put(ExtensionController.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$38(this.f$0);
            }
        });
        this.mProviders.put(PluginDependencyProvider.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$39();
            }
        });
        this.mProviders.put(LocalBluetoothManager.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return LocalBluetoothManager.getInstance(this.f$0.mContext, null);
            }
        });
        this.mProviders.put(VolumeDialogController.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$41(this.f$0);
            }
        });
        this.mProviders.put(MetricsLogger.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$42();
            }
        });
        this.mProviders.put(AccessibilityManagerWrapper.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$43(this.f$0);
            }
        });
        this.mProviders.put(SysuiColorExtractor.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$44(this.f$0);
            }
        });
        this.mProviders.put(TunablePadding.TunablePaddingService.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$45();
            }
        });
        this.mProviders.put(ForegroundServiceController.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$46(this.f$0);
            }
        });
        this.mProviders.put(UiOffloadThread.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return new UiOffloadThread();
            }
        });
        this.mProviders.put(PowerUI.WarningsUI.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$47(this.f$0);
            }
        });
        this.mProviders.put(IconLogger.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$48(this.f$0);
            }
        });
        this.mProviders.put(LightBarController.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$49(this.f$0);
            }
        });
        this.mProviders.put(IWindowManager.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return WindowManagerGlobal.getWindowManagerService();
            }
        });
        this.mProviders.put(OverviewProxyService.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$51(this.f$0);
            }
        });
        this.mProviders.put(EnhancedEstimates.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$52();
            }
        });
        this.mProviders.put(AppOpsListener.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$53(this.f$0);
            }
        });
        this.mProviders.put(VibratorHelper.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return Dependency.lambda$start$54(this.f$0);
            }
        });
        this.mProviders.put(IStatusBarService.class, new DependencyProvider() {
            @Override
            public final Object createDependency() {
                return IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
            }
        });
        SystemUIFactory.getInstance().injectDependencies(this.mProviders, this.mContext);
        sDependency = this;
    }

    static Object lambda$start$0() {
        HandlerThread handlerThread = new HandlerThread("TimeTick");
        handlerThread.start();
        return new Handler(handlerThread.getLooper());
    }

    static Object lambda$start$1() {
        HandlerThread handlerThread = new HandlerThread("SysUiBg", 10);
        handlerThread.start();
        return handlerThread.getLooper();
    }

    static Object lambda$start$2() {
        return new Handler(Looper.getMainLooper());
    }

    static Object lambda$start$3() {
        return new ActivityStarterDelegate();
    }

    public static Object lambda$start$4(Dependency dependency) {
        return (ActivityStarter) dependency.getDependency(ActivityStarter.class);
    }

    public static Object lambda$start$5(Dependency dependency) {
        return new AsyncSensorManager((SensorManager) dependency.mContext.getSystemService(SensorManager.class));
    }

    public static Object lambda$start$6(Dependency dependency) {
        return new BluetoothControllerImpl(dependency.mContext, (Looper) dependency.getDependency(BG_LOOPER));
    }

    public static Object lambda$start$7(Dependency dependency) {
        return new LocationControllerImpl(dependency.mContext, (Looper) dependency.getDependency(BG_LOOPER));
    }

    public static Object lambda$start$8(Dependency dependency) {
        return new RotationLockControllerImpl(dependency.mContext);
    }

    public static Object lambda$start$9(Dependency dependency) {
        return new NetworkControllerImpl(dependency.mContext, (Looper) dependency.getDependency(BG_LOOPER), (DeviceProvisionedController) dependency.getDependency(DeviceProvisionedController.class));
    }

    public static Object lambda$start$10(Dependency dependency) {
        return new ZenModeControllerImpl(dependency.mContext, (Handler) dependency.getDependency(MAIN_HANDLER));
    }

    public static Object lambda$start$11(Dependency dependency) {
        return new HotspotControllerImpl(dependency.mContext);
    }

    public static Object lambda$start$12(Dependency dependency) {
        return new CastControllerImpl(dependency.mContext);
    }

    public static Object lambda$start$13(Dependency dependency) {
        return new FlashlightControllerImpl(dependency.mContext);
    }

    public static Object lambda$start$14(Dependency dependency) {
        return new KeyguardMonitorImpl(dependency.mContext);
    }

    public static Object lambda$start$15(Dependency dependency) {
        return new UserSwitcherController(dependency.mContext, (KeyguardMonitor) dependency.getDependency(KeyguardMonitor.class), (Handler) dependency.getDependency(MAIN_HANDLER), (ActivityStarter) dependency.getDependency(ActivityStarter.class));
    }

    public static Object lambda$start$16(Dependency dependency) {
        return new UserInfoControllerImpl(dependency.mContext);
    }

    public static Object lambda$start$17(Dependency dependency) {
        return new BatteryControllerImpl(dependency.mContext);
    }

    public static Object lambda$start$18(Dependency dependency) {
        return new ColorDisplayController(dependency.mContext);
    }

    public static Object lambda$start$19(Dependency dependency) {
        return new ManagedProfileControllerImpl(dependency.mContext);
    }

    public static Object lambda$start$20(Dependency dependency) {
        return new NextAlarmControllerImpl(dependency.mContext);
    }

    public static Object lambda$start$22(Dependency dependency) {
        return new AccessibilityController(dependency.mContext);
    }

    public static Object lambda$start$23(Dependency dependency) {
        return new DeviceProvisionedControllerImpl(dependency.mContext);
    }

    public static Object lambda$start$24(Dependency dependency) {
        return new PluginManagerImpl(dependency.mContext);
    }

    public static Object lambda$start$25(Dependency dependency) {
        return new AssistManager((DeviceProvisionedController) dependency.getDependency(DeviceProvisionedController.class), dependency.mContext);
    }

    public static Object lambda$start$26(Dependency dependency) {
        return new SecurityControllerImpl(dependency.mContext);
    }

    static Object lambda$start$27() {
        return null;
    }

    public static Object lambda$start$28(Dependency dependency) {
        return new LeakReporter(dependency.mContext, (LeakDetector) dependency.getDependency(LeakDetector.class), (String) dependency.getDependency(LEAK_REPORT_EMAIL));
    }

    public static Object lambda$start$29(Dependency dependency) {
        return new GarbageMonitor(dependency.mContext, (Looper) dependency.getDependency(BG_LOOPER), (LeakDetector) dependency.getDependency(LeakDetector.class), (LeakReporter) dependency.getDependency(LeakReporter.class));
    }

    public static Object lambda$start$30(Dependency dependency) {
        return new TunerServiceImpl(dependency.mContext);
    }

    public static Object lambda$start$31(Dependency dependency) {
        return new StatusBarWindowManager(dependency.mContext);
    }

    public static Object lambda$start$32(Dependency dependency) {
        return new DarkIconDispatcherImpl(dependency.mContext);
    }

    public static Object lambda$start$33(Dependency dependency) {
        return new ConfigurationControllerImpl(dependency.mContext);
    }

    public static Object lambda$start$34(Dependency dependency) {
        return new StatusBarIconControllerImpl(dependency.mContext);
    }

    static Object lambda$start$35() {
        return new ScreenLifecycle();
    }

    static Object lambda$start$36() {
        return new WakefulnessLifecycle();
    }

    public static Object lambda$start$37(Dependency dependency) {
        return new FragmentService(dependency.mContext);
    }

    public static Object lambda$start$38(Dependency dependency) {
        return new ExtensionControllerImpl(dependency.mContext);
    }

    static Object lambda$start$39() {
        return new PluginDependencyProvider((PluginManager) get(PluginManager.class));
    }

    public static Object lambda$start$41(Dependency dependency) {
        return new VolumeDialogControllerImpl(dependency.mContext);
    }

    static Object lambda$start$42() {
        return new MetricsLogger();
    }

    public static Object lambda$start$43(Dependency dependency) {
        return new AccessibilityManagerWrapper(dependency.mContext);
    }

    public static Object lambda$start$44(Dependency dependency) {
        return new SysuiColorExtractor(dependency.mContext);
    }

    static Object lambda$start$45() {
        return new TunablePadding.TunablePaddingService();
    }

    public static Object lambda$start$46(Dependency dependency) {
        return new ForegroundServiceControllerImpl(dependency.mContext);
    }

    public static Object lambda$start$47(Dependency dependency) {
        return new PowerNotificationWarnings(dependency.mContext);
    }

    public static Object lambda$start$48(Dependency dependency) {
        return new IconLoggerImpl(dependency.mContext, (Looper) dependency.getDependency(BG_LOOPER), (MetricsLogger) dependency.getDependency(MetricsLogger.class));
    }

    public static Object lambda$start$49(Dependency dependency) {
        return new LightBarController(dependency.mContext);
    }

    public static Object lambda$start$51(Dependency dependency) {
        return new OverviewProxyService(dependency.mContext);
    }

    static Object lambda$start$52() {
        return new EnhancedEstimatesImpl();
    }

    public static Object lambda$start$53(Dependency dependency) {
        return new AppOpsListener(dependency.mContext);
    }

    public static Object lambda$start$54(Dependency dependency) {
        return new VibratorHelper(dependency.mContext);
    }

    @Override
    public synchronized void dump(final FileDescriptor fileDescriptor, final PrintWriter printWriter, final String[] strArr) {
        super.dump(fileDescriptor, printWriter, strArr);
        printWriter.println("Dumping existing controllers:");
        this.mDependencies.values().stream().filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return Dependency.lambda$dump$56(obj);
            }
        }).forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((Dumpable) obj).dump(fileDescriptor, printWriter, strArr);
            }
        });
    }

    static boolean lambda$dump$56(Object obj) {
        return obj instanceof Dumpable;
    }

    @Override
    protected synchronized void onConfigurationChanged(final Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mDependencies.values().stream().filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return Dependency.lambda$onConfigurationChanged$58(obj);
            }
        }).forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((ConfigurationChangedReceiver) obj).onConfigurationChanged(configuration);
            }
        });
    }

    static boolean lambda$onConfigurationChanged$58(Object obj) {
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
