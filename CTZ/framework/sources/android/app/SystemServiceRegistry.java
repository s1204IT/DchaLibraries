package android.app;

import android.accounts.AccountManager;
import android.accounts.IAccountManager;
import android.app.IAlarmManager;
import android.app.IWallpaperManager;
import android.app.admin.DevicePolicyManager;
import android.app.admin.IDevicePolicyManager;
import android.app.job.IJobScheduler;
import android.app.job.JobScheduler;
import android.app.slice.SliceManager;
import android.app.timezone.RulesManager;
import android.app.trust.TrustManager;
import android.app.usage.IStorageStatsManager;
import android.app.usage.IUsageStatsManager;
import android.app.usage.NetworkStatsManager;
import android.app.usage.StorageStatsManager;
import android.app.usage.UsageStatsManager;
import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothManager;
import android.companion.CompanionDeviceManager;
import android.companion.ICompanionDeviceManager;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.IRestrictionsManager;
import android.content.RestrictionsManager;
import android.content.pm.CrossProfileApps;
import android.content.pm.ICrossProfileApps;
import android.content.pm.IShortcutService;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutManager;
import android.content.res.Resources;
import android.hardware.ConsumerIrManager;
import android.hardware.ISerialManager;
import android.hardware.SensorManager;
import android.hardware.SerialManager;
import android.hardware.SystemSensorManager;
import android.hardware.camera2.CameraManager;
import android.hardware.display.DisplayManager;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.IFingerprintService;
import android.hardware.hdmi.HdmiControlManager;
import android.hardware.hdmi.IHdmiControlService;
import android.hardware.input.InputManager;
import android.hardware.location.ContextHubManager;
import android.hardware.radio.RadioManager;
import android.hardware.usb.IUsbManager;
import android.hardware.usb.UsbManager;
import android.location.CountryDetector;
import android.location.ICountryDetector;
import android.location.ILocationManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaRouter;
import android.media.midi.IMidiManager;
import android.media.midi.MidiManager;
import android.media.projection.MediaProjectionManager;
import android.media.session.MediaSessionManager;
import android.media.soundtrigger.SoundTriggerManager;
import android.media.tv.ITvInputManager;
import android.media.tv.TvInputManager;
import android.net.ConnectivityManager;
import android.net.ConnectivityThread;
import android.net.EthernetManager;
import android.net.IConnectivityManager;
import android.net.IEthernetManager;
import android.net.IIpSecService;
import android.net.INetworkPolicyManager;
import android.net.IpSecManager;
import android.net.NetworkPolicyManager;
import android.net.NetworkScoreManager;
import android.net.NetworkWatchlistManager;
import android.net.lowpan.ILowpanManager;
import android.net.lowpan.LowpanManager;
import android.net.nsd.INsdManager;
import android.net.nsd.NsdManager;
import android.net.wifi.IWifiManager;
import android.net.wifi.IWifiScanner;
import android.net.wifi.RttManager;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiScanner;
import android.net.wifi.aware.IWifiAwareManager;
import android.net.wifi.aware.WifiAwareManager;
import android.net.wifi.p2p.IWifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.rtt.IWifiRttManager;
import android.net.wifi.rtt.WifiRttManager;
import android.nfc.NfcManager;
import android.os.BatteryManager;
import android.os.BatteryStats;
import android.os.DeviceIdleManager;
import android.os.DropBoxManager;
import android.os.HardwarePropertiesManager;
import android.os.IBatteryPropertiesRegistrar;
import android.os.IBinder;
import android.os.IDeviceIdleController;
import android.os.IHardwarePropertiesManager;
import android.os.IPowerManager;
import android.os.IRecoverySystem;
import android.os.ISystemUpdateManager;
import android.os.IUserManager;
import android.os.IncidentManager;
import android.os.PowerManager;
import android.os.Process;
import android.os.RecoverySystem;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.SystemUpdateManager;
import android.os.SystemVibrator;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.Vibrator;
import android.os.health.SystemHealthManager;
import android.os.storage.StorageManager;
import android.print.IPrintManager;
import android.print.PrintManager;
import android.service.oemlock.IOemLockService;
import android.service.oemlock.OemLockManager;
import android.service.persistentdata.IPersistentDataBlockService;
import android.service.persistentdata.PersistentDataBlockManager;
import android.service.vr.IVrManager;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.euicc.EuiccCardManager;
import android.telephony.euicc.EuiccManager;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.WindowManagerImpl;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.CaptioningManager;
import android.view.autofill.AutofillManager;
import android.view.autofill.IAutoFillManager;
import android.view.inputmethod.InputMethodManager;
import android.view.textclassifier.TextClassificationManager;
import android.view.textservice.TextServicesManager;
import com.android.internal.app.IAppOpsService;
import com.android.internal.app.IBatteryStats;
import com.android.internal.app.ISoundTriggerService;
import com.android.internal.appwidget.IAppWidgetService;
import com.android.internal.net.INetworkWatchlistManager;
import com.android.internal.os.IDropBoxManagerService;
import com.android.internal.policy.PhoneLayoutInflater;
import java.util.HashMap;

public final class SystemServiceRegistry {
    private static final String PERSISTENT_DATA_BLOCK_PROP = "ro.frp.pst";
    private static final String PERSISTENT_OEM_VENDOR_LOCK = "ro.service.oem.vendorlock";
    private static final String TAG = "SystemServiceRegistry";
    public static Class<?> sMtkServiceRegistryClass;
    private static int sServiceCacheSize;
    private static final HashMap<Class<?>, String> SYSTEM_SERVICE_NAMES = new HashMap<>();
    private static final HashMap<String, ServiceFetcher<?>> SYSTEM_SERVICE_FETCHERS = new HashMap<>();

    public interface ServiceFetcher<T> {
        T getService(ContextImpl contextImpl);
    }

    static int access$008() {
        int i = sServiceCacheSize;
        sServiceCacheSize = i + 1;
        return i;
    }

    static {
        registerService(Context.ACCESSIBILITY_SERVICE, AccessibilityManager.class, new CachedServiceFetcher<AccessibilityManager>() {
            @Override
            public AccessibilityManager createService(ContextImpl contextImpl) {
                return AccessibilityManager.getInstance(contextImpl);
            }
        });
        registerService(Context.CAPTIONING_SERVICE, CaptioningManager.class, new CachedServiceFetcher<CaptioningManager>() {
            @Override
            public CaptioningManager createService(ContextImpl contextImpl) {
                return new CaptioningManager(contextImpl);
            }
        });
        registerService("account", AccountManager.class, new CachedServiceFetcher<AccountManager>() {
            @Override
            public AccountManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new AccountManager(contextImpl, IAccountManager.Stub.asInterface(ServiceManager.getServiceOrThrow("account")));
            }
        });
        registerService(Context.ACTIVITY_SERVICE, ActivityManager.class, new CachedServiceFetcher<ActivityManager>() {
            @Override
            public ActivityManager createService(ContextImpl contextImpl) {
                return new ActivityManager(contextImpl.getOuterContext(), contextImpl.mMainThread.getHandler());
            }
        });
        registerService("alarm", AlarmManager.class, new CachedServiceFetcher<AlarmManager>() {
            @Override
            public AlarmManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new AlarmManager(IAlarmManager.Stub.asInterface(ServiceManager.getServiceOrThrow("alarm")), contextImpl);
            }
        });
        registerService("audio", AudioManager.class, new CachedServiceFetcher<AudioManager>() {
            @Override
            public AudioManager createService(ContextImpl contextImpl) {
                return new AudioManager(contextImpl);
            }
        });
        registerService(Context.MEDIA_ROUTER_SERVICE, MediaRouter.class, new CachedServiceFetcher<MediaRouter>() {
            @Override
            public MediaRouter createService(ContextImpl contextImpl) {
                return new MediaRouter(contextImpl);
            }
        });
        registerService("bluetooth", BluetoothManager.class, new CachedServiceFetcher<BluetoothManager>() {
            @Override
            public BluetoothManager createService(ContextImpl contextImpl) {
                return new BluetoothManager(contextImpl);
            }
        });
        registerService(Context.HDMI_CONTROL_SERVICE, HdmiControlManager.class, new StaticServiceFetcher<HdmiControlManager>() {
            @Override
            public HdmiControlManager createService() throws ServiceManager.ServiceNotFoundException {
                return new HdmiControlManager(IHdmiControlService.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.HDMI_CONTROL_SERVICE)));
            }
        });
        registerService(Context.TEXT_CLASSIFICATION_SERVICE, TextClassificationManager.class, new CachedServiceFetcher<TextClassificationManager>() {
            @Override
            public TextClassificationManager createService(ContextImpl contextImpl) {
                return new TextClassificationManager(contextImpl);
            }
        });
        registerService(Context.CLIPBOARD_SERVICE, ClipboardManager.class, new CachedServiceFetcher<ClipboardManager>() {
            @Override
            public ClipboardManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new ClipboardManager(contextImpl.getOuterContext(), contextImpl.mMainThread.getHandler());
            }
        });
        SYSTEM_SERVICE_NAMES.put(android.text.ClipboardManager.class, Context.CLIPBOARD_SERVICE);
        registerService(Context.CONNECTIVITY_SERVICE, ConnectivityManager.class, new StaticApplicationContextServiceFetcher<ConnectivityManager>() {
            @Override
            public ConnectivityManager createService(Context context) throws ServiceManager.ServiceNotFoundException {
                return new ConnectivityManager(context, IConnectivityManager.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.CONNECTIVITY_SERVICE)));
            }
        });
        registerService("ipsec", IpSecManager.class, new CachedServiceFetcher<IpSecManager>() {
            @Override
            public IpSecManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new IpSecManager(contextImpl, IIpSecService.Stub.asInterface(ServiceManager.getService("ipsec")));
            }
        });
        registerService(Context.COUNTRY_DETECTOR, CountryDetector.class, new StaticServiceFetcher<CountryDetector>() {
            @Override
            public CountryDetector createService() throws ServiceManager.ServiceNotFoundException {
                return new CountryDetector(ICountryDetector.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.COUNTRY_DETECTOR)));
            }
        });
        registerService(Context.DEVICE_POLICY_SERVICE, DevicePolicyManager.class, new CachedServiceFetcher<DevicePolicyManager>() {
            @Override
            public DevicePolicyManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new DevicePolicyManager(contextImpl, IDevicePolicyManager.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.DEVICE_POLICY_SERVICE)));
            }
        });
        registerService(Context.DOWNLOAD_SERVICE, DownloadManager.class, new CachedServiceFetcher<DownloadManager>() {
            @Override
            public DownloadManager createService(ContextImpl contextImpl) {
                return new DownloadManager(contextImpl);
            }
        });
        registerService(Context.BATTERY_SERVICE, BatteryManager.class, new CachedServiceFetcher<BatteryManager>() {
            @Override
            public BatteryManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new BatteryManager(contextImpl, IBatteryStats.Stub.asInterface(ServiceManager.getServiceOrThrow(BatteryStats.SERVICE_NAME)), IBatteryPropertiesRegistrar.Stub.asInterface(ServiceManager.getServiceOrThrow("batteryproperties")));
            }
        });
        registerService("nfc", NfcManager.class, new CachedServiceFetcher<NfcManager>() {
            @Override
            public NfcManager createService(ContextImpl contextImpl) {
                return new NfcManager(contextImpl);
            }
        });
        registerService(Context.DROPBOX_SERVICE, DropBoxManager.class, new CachedServiceFetcher<DropBoxManager>() {
            @Override
            public DropBoxManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new DropBoxManager(contextImpl, IDropBoxManagerService.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.DROPBOX_SERVICE)));
            }
        });
        registerService("input", InputManager.class, new StaticServiceFetcher<InputManager>() {
            @Override
            public InputManager createService() {
                return InputManager.getInstance();
            }
        });
        registerService(Context.DISPLAY_SERVICE, DisplayManager.class, new CachedServiceFetcher<DisplayManager>() {
            @Override
            public DisplayManager createService(ContextImpl contextImpl) {
                return new DisplayManager(contextImpl.getOuterContext());
            }
        });
        registerService(Context.INPUT_METHOD_SERVICE, InputMethodManager.class, new StaticServiceFetcher<InputMethodManager>() {
            @Override
            public InputMethodManager createService() {
                return InputMethodManager.getInstance();
            }
        });
        registerService(Context.TEXT_SERVICES_MANAGER_SERVICE, TextServicesManager.class, new StaticServiceFetcher<TextServicesManager>() {
            @Override
            public TextServicesManager createService() {
                return TextServicesManager.getInstance();
            }
        });
        registerService(Context.KEYGUARD_SERVICE, KeyguardManager.class, new CachedServiceFetcher<KeyguardManager>() {
            @Override
            public KeyguardManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new KeyguardManager(contextImpl);
            }
        });
        registerService(Context.LAYOUT_INFLATER_SERVICE, LayoutInflater.class, new CachedServiceFetcher<LayoutInflater>() {
            @Override
            public LayoutInflater createService(ContextImpl contextImpl) {
                return new PhoneLayoutInflater(contextImpl.getOuterContext());
            }
        });
        registerService("location", LocationManager.class, new CachedServiceFetcher<LocationManager>() {
            @Override
            public LocationManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new LocationManager(contextImpl, ILocationManager.Stub.asInterface(ServiceManager.getServiceOrThrow("location")));
            }
        });
        registerService(Context.NETWORK_POLICY_SERVICE, NetworkPolicyManager.class, new CachedServiceFetcher<NetworkPolicyManager>() {
            @Override
            public NetworkPolicyManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new NetworkPolicyManager(contextImpl, INetworkPolicyManager.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.NETWORK_POLICY_SERVICE)));
            }
        });
        registerService(Context.NOTIFICATION_SERVICE, NotificationManager.class, new CachedServiceFetcher<NotificationManager>() {
            @Override
            public NotificationManager createService(ContextImpl contextImpl) {
                Context outerContext = contextImpl.getOuterContext();
                return new NotificationManager(new ContextThemeWrapper(outerContext, Resources.selectSystemTheme(0, outerContext.getApplicationInfo().targetSdkVersion, 16973835, 16973935, 16974126, 16974130)), contextImpl.mMainThread.getHandler());
            }
        });
        registerService(Context.NSD_SERVICE, NsdManager.class, new CachedServiceFetcher<NsdManager>() {
            @Override
            public NsdManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new NsdManager(contextImpl.getOuterContext(), INsdManager.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.NSD_SERVICE)));
            }
        });
        registerService(Context.POWER_SERVICE, PowerManager.class, new CachedServiceFetcher<PowerManager>() {
            @Override
            public PowerManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new PowerManager(contextImpl.getOuterContext(), IPowerManager.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.POWER_SERVICE)), contextImpl.mMainThread.getHandler());
            }
        });
        registerService("recovery", RecoverySystem.class, new CachedServiceFetcher<RecoverySystem>() {
            @Override
            public RecoverySystem createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new RecoverySystem(IRecoverySystem.Stub.asInterface(ServiceManager.getServiceOrThrow("recovery")));
            }
        });
        registerService("search", SearchManager.class, new CachedServiceFetcher<SearchManager>() {
            @Override
            public SearchManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new SearchManager(contextImpl.getOuterContext(), contextImpl.mMainThread.getHandler());
            }
        });
        registerService(Context.SENSOR_SERVICE, SensorManager.class, new CachedServiceFetcher<SensorManager>() {
            @Override
            public SensorManager createService(ContextImpl contextImpl) {
                return new SystemSensorManager(contextImpl.getOuterContext(), contextImpl.mMainThread.getHandler().getLooper());
            }
        });
        registerService(Context.STATS_MANAGER, StatsManager.class, new CachedServiceFetcher<StatsManager>() {
            @Override
            public StatsManager createService(ContextImpl contextImpl) {
                return new StatsManager(contextImpl.getOuterContext());
            }
        });
        registerService(Context.STATUS_BAR_SERVICE, StatusBarManager.class, new CachedServiceFetcher<StatusBarManager>() {
            @Override
            public StatusBarManager createService(ContextImpl contextImpl) {
                return new StatusBarManager(contextImpl.getOuterContext());
            }
        });
        registerService(Context.STORAGE_SERVICE, StorageManager.class, new CachedServiceFetcher<StorageManager>() {
            @Override
            public StorageManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new StorageManager(contextImpl, contextImpl.mMainThread.getHandler().getLooper());
            }
        });
        registerService(Context.STORAGE_STATS_SERVICE, StorageStatsManager.class, new CachedServiceFetcher<StorageStatsManager>() {
            @Override
            public StorageStatsManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new StorageStatsManager(contextImpl, IStorageStatsManager.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.STORAGE_STATS_SERVICE)));
            }
        });
        registerService(Context.SYSTEM_UPDATE_SERVICE, SystemUpdateManager.class, new CachedServiceFetcher<SystemUpdateManager>() {
            @Override
            public SystemUpdateManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new SystemUpdateManager(ISystemUpdateManager.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.SYSTEM_UPDATE_SERVICE)));
            }
        });
        registerService("phone", TelephonyManager.class, new CachedServiceFetcher<TelephonyManager>() {
            @Override
            public TelephonyManager createService(ContextImpl contextImpl) {
                return new TelephonyManager(contextImpl.getOuterContext());
            }
        });
        registerService(Context.TELEPHONY_SUBSCRIPTION_SERVICE, SubscriptionManager.class, new CachedServiceFetcher<SubscriptionManager>() {
            @Override
            public SubscriptionManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new SubscriptionManager(contextImpl.getOuterContext());
            }
        });
        registerService(Context.CARRIER_CONFIG_SERVICE, CarrierConfigManager.class, new CachedServiceFetcher<CarrierConfigManager>() {
            @Override
            public CarrierConfigManager createService(ContextImpl contextImpl) {
                return new CarrierConfigManager();
            }
        });
        registerService(Context.TELECOM_SERVICE, TelecomManager.class, new CachedServiceFetcher<TelecomManager>() {
            @Override
            public TelecomManager createService(ContextImpl contextImpl) {
                return new TelecomManager(contextImpl.getOuterContext());
            }
        });
        registerService(Context.EUICC_SERVICE, EuiccManager.class, new CachedServiceFetcher<EuiccManager>() {
            @Override
            public EuiccManager createService(ContextImpl contextImpl) {
                return new EuiccManager(contextImpl.getOuterContext());
            }
        });
        registerService(Context.EUICC_CARD_SERVICE, EuiccCardManager.class, new CachedServiceFetcher<EuiccCardManager>() {
            @Override
            public EuiccCardManager createService(ContextImpl contextImpl) {
                return new EuiccCardManager(contextImpl.getOuterContext());
            }
        });
        registerService(Context.UI_MODE_SERVICE, UiModeManager.class, new CachedServiceFetcher<UiModeManager>() {
            @Override
            public UiModeManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new UiModeManager();
            }
        });
        registerService(Context.USB_SERVICE, UsbManager.class, new CachedServiceFetcher<UsbManager>() {
            @Override
            public UsbManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new UsbManager(contextImpl, IUsbManager.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.USB_SERVICE)));
            }
        });
        registerService(Context.SERIAL_SERVICE, SerialManager.class, new CachedServiceFetcher<SerialManager>() {
            @Override
            public SerialManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new SerialManager(contextImpl, ISerialManager.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.SERIAL_SERVICE)));
            }
        });
        registerService(Context.VIBRATOR_SERVICE, Vibrator.class, new CachedServiceFetcher<Vibrator>() {
            @Override
            public Vibrator createService(ContextImpl contextImpl) {
                return new SystemVibrator(contextImpl);
            }
        });
        registerService(Context.WALLPAPER_SERVICE, WallpaperManager.class, new CachedServiceFetcher<WallpaperManager>() {
            @Override
            public WallpaperManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                IBinder service;
                if (contextImpl.getApplicationInfo().targetSdkVersion >= 28) {
                    service = ServiceManager.getServiceOrThrow(Context.WALLPAPER_SERVICE);
                } else {
                    service = ServiceManager.getService(Context.WALLPAPER_SERVICE);
                }
                return new WallpaperManager(IWallpaperManager.Stub.asInterface(service), contextImpl.getOuterContext(), contextImpl.mMainThread.getHandler());
            }
        });
        registerService("lowpan", LowpanManager.class, new CachedServiceFetcher<LowpanManager>() {
            @Override
            public LowpanManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new LowpanManager(contextImpl.getOuterContext(), ILowpanManager.Stub.asInterface(ServiceManager.getServiceOrThrow("lowpan")), ConnectivityThread.getInstanceLooper());
            }
        });
        registerService("wifi", WifiManager.class, new CachedServiceFetcher<WifiManager>() {
            @Override
            public WifiManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new WifiManager(contextImpl.getOuterContext(), IWifiManager.Stub.asInterface(ServiceManager.getServiceOrThrow("wifi")), ConnectivityThread.getInstanceLooper());
            }
        });
        registerService(Context.WIFI_P2P_SERVICE, WifiP2pManager.class, new StaticServiceFetcher<WifiP2pManager>() {
            @Override
            public WifiP2pManager createService() throws ServiceManager.ServiceNotFoundException {
                return new WifiP2pManager(IWifiP2pManager.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.WIFI_P2P_SERVICE)));
            }
        });
        registerService(Context.WIFI_AWARE_SERVICE, WifiAwareManager.class, new CachedServiceFetcher<WifiAwareManager>() {
            @Override
            public WifiAwareManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                IWifiAwareManager iWifiAwareManagerAsInterface = IWifiAwareManager.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.WIFI_AWARE_SERVICE));
                if (iWifiAwareManagerAsInterface == null) {
                    return null;
                }
                return new WifiAwareManager(contextImpl.getOuterContext(), iWifiAwareManagerAsInterface);
            }
        });
        registerService(Context.WIFI_SCANNING_SERVICE, WifiScanner.class, new CachedServiceFetcher<WifiScanner>() {
            @Override
            public WifiScanner createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new WifiScanner(contextImpl.getOuterContext(), IWifiScanner.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.WIFI_SCANNING_SERVICE)), ConnectivityThread.getInstanceLooper());
            }
        });
        registerService(Context.WIFI_RTT_SERVICE, RttManager.class, new CachedServiceFetcher<RttManager>() {
            @Override
            public RttManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new RttManager(contextImpl.getOuterContext(), new WifiRttManager(contextImpl.getOuterContext(), IWifiRttManager.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.WIFI_RTT_RANGING_SERVICE))));
            }
        });
        registerService(Context.WIFI_RTT_RANGING_SERVICE, WifiRttManager.class, new CachedServiceFetcher<WifiRttManager>() {
            @Override
            public WifiRttManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new WifiRttManager(contextImpl.getOuterContext(), IWifiRttManager.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.WIFI_RTT_RANGING_SERVICE)));
            }
        });
        registerService(Context.ETHERNET_SERVICE, EthernetManager.class, new CachedServiceFetcher<EthernetManager>() {
            @Override
            public EthernetManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new EthernetManager(contextImpl.getOuterContext(), IEthernetManager.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.ETHERNET_SERVICE)));
            }
        });
        registerService(Context.WINDOW_SERVICE, WindowManager.class, new CachedServiceFetcher<WindowManager>() {
            @Override
            public WindowManager createService(ContextImpl contextImpl) {
                return new WindowManagerImpl(contextImpl);
            }
        });
        registerService("user", UserManager.class, new CachedServiceFetcher<UserManager>() {
            @Override
            public UserManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new UserManager(contextImpl, IUserManager.Stub.asInterface(ServiceManager.getServiceOrThrow("user")));
            }
        });
        registerService(Context.APP_OPS_SERVICE, AppOpsManager.class, new CachedServiceFetcher<AppOpsManager>() {
            @Override
            public AppOpsManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new AppOpsManager(contextImpl, IAppOpsService.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.APP_OPS_SERVICE)));
            }
        });
        registerService(Context.CAMERA_SERVICE, CameraManager.class, new CachedServiceFetcher<CameraManager>() {
            @Override
            public CameraManager createService(ContextImpl contextImpl) {
                return new CameraManager(contextImpl);
            }
        });
        registerService(Context.LAUNCHER_APPS_SERVICE, LauncherApps.class, new CachedServiceFetcher<LauncherApps>() {
            @Override
            public LauncherApps createService(ContextImpl contextImpl) {
                return new LauncherApps(contextImpl);
            }
        });
        registerService(Context.RESTRICTIONS_SERVICE, RestrictionsManager.class, new CachedServiceFetcher<RestrictionsManager>() {
            @Override
            public RestrictionsManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new RestrictionsManager(contextImpl, IRestrictionsManager.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.RESTRICTIONS_SERVICE)));
            }
        });
        registerService(Context.PRINT_SERVICE, PrintManager.class, new CachedServiceFetcher<PrintManager>() {
            @Override
            public PrintManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                IPrintManager iPrintManagerAsInterface;
                if (contextImpl.getPackageManager().hasSystemFeature(PackageManager.FEATURE_PRINTING)) {
                    iPrintManagerAsInterface = IPrintManager.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.PRINT_SERVICE));
                } else {
                    iPrintManagerAsInterface = null;
                }
                return new PrintManager(contextImpl.getOuterContext(), iPrintManagerAsInterface, contextImpl.getUserId(), UserHandle.getAppId(contextImpl.getApplicationInfo().uid));
            }
        });
        registerService(Context.COMPANION_DEVICE_SERVICE, CompanionDeviceManager.class, new CachedServiceFetcher<CompanionDeviceManager>() {
            @Override
            public CompanionDeviceManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                ICompanionDeviceManager iCompanionDeviceManagerAsInterface;
                if (contextImpl.getPackageManager().hasSystemFeature(PackageManager.FEATURE_COMPANION_DEVICE_SETUP)) {
                    iCompanionDeviceManagerAsInterface = ICompanionDeviceManager.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.COMPANION_DEVICE_SERVICE));
                } else {
                    iCompanionDeviceManagerAsInterface = null;
                }
                return new CompanionDeviceManager(iCompanionDeviceManagerAsInterface, contextImpl.getOuterContext());
            }
        });
        registerService(Context.CONSUMER_IR_SERVICE, ConsumerIrManager.class, new CachedServiceFetcher<ConsumerIrManager>() {
            @Override
            public ConsumerIrManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new ConsumerIrManager(contextImpl);
            }
        });
        registerService(Context.MEDIA_SESSION_SERVICE, MediaSessionManager.class, new CachedServiceFetcher<MediaSessionManager>() {
            @Override
            public MediaSessionManager createService(ContextImpl contextImpl) {
                return new MediaSessionManager(contextImpl);
            }
        });
        registerService(Context.TRUST_SERVICE, TrustManager.class, new StaticServiceFetcher<TrustManager>() {
            @Override
            public TrustManager createService() throws ServiceManager.ServiceNotFoundException {
                return new TrustManager(ServiceManager.getServiceOrThrow(Context.TRUST_SERVICE));
            }
        });
        registerService(Context.FINGERPRINT_SERVICE, FingerprintManager.class, new CachedServiceFetcher<FingerprintManager>() {
            @Override
            public FingerprintManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                IBinder service;
                if (contextImpl.getApplicationInfo().targetSdkVersion >= 26) {
                    service = ServiceManager.getServiceOrThrow(Context.FINGERPRINT_SERVICE);
                } else {
                    service = ServiceManager.getService(Context.FINGERPRINT_SERVICE);
                }
                return new FingerprintManager(contextImpl.getOuterContext(), IFingerprintService.Stub.asInterface(service));
            }
        });
        registerService(Context.TV_INPUT_SERVICE, TvInputManager.class, new CachedServiceFetcher<TvInputManager>() {
            @Override
            public TvInputManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new TvInputManager(ITvInputManager.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.TV_INPUT_SERVICE)), contextImpl.getUserId());
            }
        });
        registerService(Context.NETWORK_SCORE_SERVICE, NetworkScoreManager.class, new CachedServiceFetcher<NetworkScoreManager>() {
            @Override
            public NetworkScoreManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new NetworkScoreManager(contextImpl);
            }
        });
        registerService(Context.USAGE_STATS_SERVICE, UsageStatsManager.class, new CachedServiceFetcher<UsageStatsManager>() {
            @Override
            public UsageStatsManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new UsageStatsManager(contextImpl.getOuterContext(), IUsageStatsManager.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.USAGE_STATS_SERVICE)));
            }
        });
        registerService(Context.NETWORK_STATS_SERVICE, NetworkStatsManager.class, new CachedServiceFetcher<NetworkStatsManager>() {
            @Override
            public NetworkStatsManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new NetworkStatsManager(contextImpl.getOuterContext());
            }
        });
        registerService(Context.JOB_SCHEDULER_SERVICE, JobScheduler.class, new StaticServiceFetcher<JobScheduler>() {
            @Override
            public JobScheduler createService() throws ServiceManager.ServiceNotFoundException {
                return new JobSchedulerImpl(IJobScheduler.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.JOB_SCHEDULER_SERVICE)));
            }
        });
        boolean z = !SystemProperties.get(PERSISTENT_DATA_BLOCK_PROP).equals("");
        boolean zEquals = SystemProperties.get(PERSISTENT_OEM_VENDOR_LOCK).equals(WifiEnterpriseConfig.ENGINE_ENABLE);
        if (z) {
            registerService(Context.PERSISTENT_DATA_BLOCK_SERVICE, PersistentDataBlockManager.class, new StaticServiceFetcher<PersistentDataBlockManager>() {
                @Override
                public PersistentDataBlockManager createService() throws ServiceManager.ServiceNotFoundException {
                    IPersistentDataBlockService iPersistentDataBlockServiceAsInterface = IPersistentDataBlockService.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.PERSISTENT_DATA_BLOCK_SERVICE));
                    if (iPersistentDataBlockServiceAsInterface != null) {
                        return new PersistentDataBlockManager(iPersistentDataBlockServiceAsInterface);
                    }
                    return null;
                }
            });
        }
        if (z || zEquals) {
            registerService(Context.OEM_LOCK_SERVICE, OemLockManager.class, new StaticServiceFetcher<OemLockManager>() {
                @Override
                public OemLockManager createService() throws ServiceManager.ServiceNotFoundException {
                    IOemLockService iOemLockServiceAsInterface = IOemLockService.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.OEM_LOCK_SERVICE));
                    if (iOemLockServiceAsInterface != null) {
                        return new OemLockManager(iOemLockServiceAsInterface);
                    }
                    return null;
                }
            });
        }
        registerService(Context.MEDIA_PROJECTION_SERVICE, MediaProjectionManager.class, new CachedServiceFetcher<MediaProjectionManager>() {
            @Override
            public MediaProjectionManager createService(ContextImpl contextImpl) {
                return new MediaProjectionManager(contextImpl);
            }
        });
        registerService(Context.APPWIDGET_SERVICE, AppWidgetManager.class, new CachedServiceFetcher<AppWidgetManager>() {
            @Override
            public AppWidgetManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new AppWidgetManager(contextImpl, IAppWidgetService.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.APPWIDGET_SERVICE)));
            }
        });
        registerService("midi", MidiManager.class, new CachedServiceFetcher<MidiManager>() {
            @Override
            public MidiManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new MidiManager(IMidiManager.Stub.asInterface(ServiceManager.getServiceOrThrow("midi")));
            }
        });
        registerService(Context.RADIO_SERVICE, RadioManager.class, new CachedServiceFetcher<RadioManager>() {
            @Override
            public RadioManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new RadioManager(contextImpl);
            }
        });
        registerService(Context.HARDWARE_PROPERTIES_SERVICE, HardwarePropertiesManager.class, new CachedServiceFetcher<HardwarePropertiesManager>() {
            @Override
            public HardwarePropertiesManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new HardwarePropertiesManager(contextImpl, IHardwarePropertiesManager.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.HARDWARE_PROPERTIES_SERVICE)));
            }
        });
        registerService(Context.SOUND_TRIGGER_SERVICE, SoundTriggerManager.class, new CachedServiceFetcher<SoundTriggerManager>() {
            @Override
            public SoundTriggerManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new SoundTriggerManager(contextImpl, ISoundTriggerService.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.SOUND_TRIGGER_SERVICE)));
            }
        });
        registerService("shortcut", ShortcutManager.class, new CachedServiceFetcher<ShortcutManager>() {
            @Override
            public ShortcutManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new ShortcutManager(contextImpl, IShortcutService.Stub.asInterface(ServiceManager.getServiceOrThrow("shortcut")));
            }
        });
        registerService(Context.NETWORK_WATCHLIST_SERVICE, NetworkWatchlistManager.class, new CachedServiceFetcher<NetworkWatchlistManager>() {
            @Override
            public NetworkWatchlistManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new NetworkWatchlistManager(contextImpl, INetworkWatchlistManager.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.NETWORK_WATCHLIST_SERVICE)));
            }
        });
        registerService(Context.SYSTEM_HEALTH_SERVICE, SystemHealthManager.class, new CachedServiceFetcher<SystemHealthManager>() {
            @Override
            public SystemHealthManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new SystemHealthManager(IBatteryStats.Stub.asInterface(ServiceManager.getServiceOrThrow(BatteryStats.SERVICE_NAME)));
            }
        });
        registerService(Context.CONTEXTHUB_SERVICE, ContextHubManager.class, new CachedServiceFetcher<ContextHubManager>() {
            @Override
            public ContextHubManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new ContextHubManager(contextImpl.getOuterContext(), contextImpl.mMainThread.getHandler().getLooper());
            }
        });
        registerService(Context.INCIDENT_SERVICE, IncidentManager.class, new CachedServiceFetcher<IncidentManager>() {
            @Override
            public IncidentManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new IncidentManager(contextImpl);
            }
        });
        registerService(Context.AUTOFILL_MANAGER_SERVICE, AutofillManager.class, new CachedServiceFetcher<AutofillManager>() {
            @Override
            public AutofillManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new AutofillManager(contextImpl.getOuterContext(), IAutoFillManager.Stub.asInterface(ServiceManager.getService(Context.AUTOFILL_MANAGER_SERVICE)));
            }
        });
        registerService(Context.VR_SERVICE, VrManager.class, new CachedServiceFetcher<VrManager>() {
            @Override
            public VrManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new VrManager(IVrManager.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.VR_SERVICE)));
            }
        });
        registerService(Context.TIME_ZONE_RULES_MANAGER_SERVICE, RulesManager.class, new CachedServiceFetcher<RulesManager>() {
            @Override
            public RulesManager createService(ContextImpl contextImpl) {
                return new RulesManager(contextImpl.getOuterContext());
            }
        });
        registerService(Context.CROSS_PROFILE_APPS_SERVICE, CrossProfileApps.class, new CachedServiceFetcher<CrossProfileApps>() {
            @Override
            public CrossProfileApps createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new CrossProfileApps(contextImpl.getOuterContext(), ICrossProfileApps.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.CROSS_PROFILE_APPS_SERVICE)));
            }
        });
        registerService("slice", SliceManager.class, new CachedServiceFetcher<SliceManager>() {
            @Override
            public SliceManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new SliceManager(contextImpl.getOuterContext(), contextImpl.mMainThread.getHandler());
            }
        });
        sMtkServiceRegistryClass = regMtkService();
        setMtkSystemServiceName();
        registerAllMtkService();
        registerService(Context.DEVICE_IDLE_CONTROLLER, DeviceIdleManager.class, new CachedServiceFetcher<DeviceIdleManager>() {
            @Override
            public DeviceIdleManager createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                return new DeviceIdleManager(contextImpl.getOuterContext(), IDeviceIdleController.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.DEVICE_IDLE_CONTROLLER)));
            }
        });
    }

    private SystemServiceRegistry() {
    }

    public static Object[] createServiceCache() {
        return new Object[sServiceCacheSize];
    }

    public static Object getSystemService(ContextImpl contextImpl, String str) {
        ServiceFetcher<?> serviceFetcher = SYSTEM_SERVICE_FETCHERS.get(str);
        if (serviceFetcher != null) {
            return serviceFetcher.getService(contextImpl);
        }
        return null;
    }

    public static String getSystemServiceName(Class<?> cls) {
        return SYSTEM_SERVICE_NAMES.get(cls);
    }

    private static <T> void registerService(String str, Class<T> cls, ServiceFetcher<T> serviceFetcher) {
        SYSTEM_SERVICE_NAMES.put(cls, str);
        SYSTEM_SERVICE_FETCHERS.put(str, serviceFetcher);
    }

    public static abstract class CachedServiceFetcher<T> implements ServiceFetcher<T> {
        private final int mCacheIndex = SystemServiceRegistry.access$008();

        public abstract T createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException;

        @Override
        public final T getService(ContextImpl contextImpl) {
            Object[] objArr = contextImpl.mServiceCache;
            int[] iArr = contextImpl.mServiceInitializationStateArray;
            while (true) {
                synchronized (objArr) {
                    T t = (T) objArr[this.mCacheIndex];
                    if (t != null || iArr[this.mCacheIndex] == 3) {
                        break;
                    }
                    boolean z = false;
                    if (iArr[this.mCacheIndex] == 2) {
                        iArr[this.mCacheIndex] = 0;
                    }
                    if (iArr[this.mCacheIndex] == 0) {
                        iArr[this.mCacheIndex] = 1;
                        z = true;
                    }
                    if (z) {
                        try {
                            try {
                                T tCreateService = createService(contextImpl);
                                synchronized (objArr) {
                                    objArr[this.mCacheIndex] = tCreateService;
                                    iArr[this.mCacheIndex] = 2;
                                    objArr.notifyAll();
                                }
                                return tCreateService;
                            } catch (ServiceManager.ServiceNotFoundException e) {
                                SystemServiceRegistry.onServiceNotFound(e);
                                synchronized (objArr) {
                                    objArr[this.mCacheIndex] = null;
                                    iArr[this.mCacheIndex] = 3;
                                    objArr.notifyAll();
                                    return null;
                                }
                            }
                        } catch (Throwable th) {
                            synchronized (objArr) {
                                objArr[this.mCacheIndex] = null;
                                iArr[this.mCacheIndex] = 3;
                                objArr.notifyAll();
                                throw th;
                            }
                        }
                    }
                    synchronized (objArr) {
                        while (iArr[this.mCacheIndex] < 2) {
                            try {
                                objArr.wait();
                            } catch (InterruptedException e2) {
                                Log.w(SystemServiceRegistry.TAG, "getService() interrupted");
                                Thread.currentThread().interrupt();
                                return null;
                            }
                        }
                    }
                }
            }
        }
    }

    static abstract class StaticServiceFetcher<T> implements ServiceFetcher<T> {
        private T mCachedInstance;

        public abstract T createService() throws ServiceManager.ServiceNotFoundException;

        StaticServiceFetcher() {
        }

        @Override
        public final T getService(ContextImpl contextImpl) {
            T t;
            synchronized (this) {
                if (this.mCachedInstance == null) {
                    try {
                        this.mCachedInstance = createService();
                    } catch (ServiceManager.ServiceNotFoundException e) {
                        SystemServiceRegistry.onServiceNotFound(e);
                    }
                    t = this.mCachedInstance;
                } else {
                    t = this.mCachedInstance;
                }
            }
            return t;
        }
    }

    static abstract class StaticApplicationContextServiceFetcher<T> implements ServiceFetcher<T> {
        private T mCachedInstance;

        public abstract T createService(Context context) throws ServiceManager.ServiceNotFoundException;

        StaticApplicationContextServiceFetcher() {
        }

        @Override
        public final T getService(ContextImpl contextImpl) {
            T t;
            synchronized (this) {
                if (this.mCachedInstance == null) {
                    Context applicationContext = contextImpl.getApplicationContext();
                    if (applicationContext != null) {
                        contextImpl = applicationContext;
                    }
                    try {
                        this.mCachedInstance = createService(contextImpl);
                    } catch (ServiceManager.ServiceNotFoundException e) {
                        SystemServiceRegistry.onServiceNotFound(e);
                    }
                    t = this.mCachedInstance;
                } else {
                    t = this.mCachedInstance;
                }
            }
            return t;
        }
    }

    public static void onServiceNotFound(ServiceManager.ServiceNotFoundException serviceNotFoundException) {
        if (Process.myUid() < 10000) {
            Log.wtf(TAG, serviceNotFoundException.getMessage(), serviceNotFoundException);
        } else {
            Log.w(TAG, serviceNotFoundException.getMessage());
        }
    }

    private static Class<?> regMtkService() {
        Log.i(TAG, "regMtkService start");
        try {
            return Class.forName("mediatek.app.MtkSystemServiceRegistry");
        } catch (Exception e) {
            Log.e(TAG, "regMtkService:" + e.toString());
            return null;
        }
    }

    private static void setMtkSystemServiceName() {
        Log.i(TAG, "setMtkSystemServiceName start");
        try {
            if (sMtkServiceRegistryClass != null) {
                sMtkServiceRegistryClass.getDeclaredMethod("setMtkSystemServiceName", HashMap.class, HashMap.class).invoke(sMtkServiceRegistryClass, SYSTEM_SERVICE_NAMES, SYSTEM_SERVICE_FETCHERS);
            }
        } catch (Exception e) {
            Log.e(TAG, "setMtkSystemServiceName" + e.toString());
        }
    }

    private static void registerAllMtkService() {
        Log.i(TAG, "registerAllMtkService start");
        try {
            if (sMtkServiceRegistryClass != null) {
                sMtkServiceRegistryClass.getDeclaredMethod("registerAllService", new Class[0]).invoke(sMtkServiceRegistryClass, new Object[0]);
            }
        } catch (Exception e) {
            Log.e(TAG, "createMtkSystemServer" + e.toString());
        }
    }
}
