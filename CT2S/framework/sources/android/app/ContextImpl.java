package android.app;

import android.accounts.AccountManager;
import android.accounts.IAccountManager;
import android.app.IAlarmManager;
import android.app.LoadedApk;
import android.app.admin.DevicePolicyManager;
import android.app.job.IJobScheduler;
import android.app.trust.TrustManager;
import android.app.usage.IUsageStatsManager;
import android.app.usage.UsageStatsManager;
import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.IContentProvider;
import android.content.IIntentReceiver;
import android.content.IRestrictionsManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.RestrictionsManager;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.ILauncherApps;
import android.content.pm.IPackageManager;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.hardware.ConsumerIrManager;
import android.hardware.ISerialManager;
import android.hardware.SerialManager;
import android.hardware.SystemSensorManager;
import android.hardware.camera2.CameraManager;
import android.hardware.display.DisplayManager;
import android.hardware.hdmi.HdmiControlManager;
import android.hardware.hdmi.IHdmiControlService;
import android.hardware.input.InputManager;
import android.hardware.usb.IUsbManager;
import android.hardware.usb.UsbManager;
import android.location.CountryDetector;
import android.location.ICountryDetector;
import android.location.ILocationManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaRouter;
import android.media.projection.MediaProjectionManager;
import android.media.session.MediaSessionManager;
import android.media.tv.ITvInputManager;
import android.media.tv.TvInputManager;
import android.net.ConnectivityManager;
import android.net.EthernetManager;
import android.net.IConnectivityManager;
import android.net.IEthernetManager;
import android.net.INetworkPolicyManager;
import android.net.NetworkPolicyManager;
import android.net.NetworkScoreManager;
import android.net.ProxyInfo;
import android.net.Uri;
import android.net.nsd.INsdManager;
import android.net.nsd.NsdManager;
import android.net.wifi.IRttManager;
import android.net.wifi.IWifiManager;
import android.net.wifi.IWifiScanner;
import android.net.wifi.RttManager;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiScanner;
import android.net.wifi.p2p.IWifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.nfc.NfcManager;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.DropBoxManager;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.IPowerManager;
import android.os.IUserManager;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemVibrator;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.IMountService;
import android.os.storage.StorageManager;
import android.print.IPrintManager;
import android.print.PrintManager;
import android.service.fingerprint.FingerprintManager;
import android.service.fingerprint.IFingerprintService;
import android.service.notification.ZenModeConfig;
import android.service.persistentdata.IPersistentDataBlockService;
import android.service.persistentdata.PersistentDataBlockManager;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.AndroidRuntimeException;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.DisplayAdjustments;
import android.view.WindowManagerImpl;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.CaptioningManager;
import android.view.inputmethod.InputMethodManager;
import android.view.textservice.TextServicesManager;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.app.IAppOpsService;
import com.android.internal.appwidget.IAppWidgetService;
import com.android.internal.os.IDropBoxManagerService;
import com.android.internal.policy.PolicyManager;
import com.android.internal.util.Preconditions;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

class ContextImpl extends Context {
    private static final boolean DEBUG = false;
    private static final String TAG = "ContextImpl";
    private static ArrayMap<String, ArrayMap<String, SharedPreferencesImpl>> sSharedPrefs;
    private final IBinder mActivityToken;
    private final String mBasePackageName;

    @GuardedBy("mSync")
    private File mCacheDir;

    @GuardedBy("mSync")
    private File mCodeCacheDir;
    private final ApplicationContentResolver mContentResolver;

    @GuardedBy("mSync")
    private File mDatabasesDir;
    private final Display mDisplay;

    @GuardedBy("mSync")
    private File[] mExternalCacheDirs;

    @GuardedBy("mSync")
    private File[] mExternalFilesDirs;

    @GuardedBy("mSync")
    private File[] mExternalMediaDirs;

    @GuardedBy("mSync")
    private File[] mExternalObbDirs;

    @GuardedBy("mSync")
    private File mFilesDir;
    final ActivityThread mMainThread;

    @GuardedBy("mSync")
    private File mNoBackupFilesDir;
    private final String mOpPackageName;
    private final Configuration mOverrideConfiguration;
    final LoadedApk mPackageInfo;
    private PackageManager mPackageManager;

    @GuardedBy("mSync")
    private File mPreferencesDir;
    private final Resources mResources;
    private final ResourcesManager mResourcesManager;
    private final boolean mRestricted;
    private final UserHandle mUser;
    private static final String[] EMPTY_FILE_LIST = new String[0];
    private static final HashMap<String, ServiceFetcher> SYSTEM_SERVICE_MAP = new HashMap<>();
    private static int sNextPerContextServiceCacheIndex = 0;
    private static ServiceFetcher WALLPAPER_FETCHER = new ServiceFetcher() {
        @Override
        public Object createService(ContextImpl ctx) {
            return new WallpaperManager(ctx.getOuterContext(), ctx.mMainThread.getHandler());
        }
    };
    private final DisplayAdjustments mDisplayAdjustments = new DisplayAdjustments();
    private int mThemeResource = 0;
    private Resources.Theme mTheme = null;
    private Context mReceiverRestrictedContext = null;
    private final Object mSync = new Object();
    final ArrayList<Object> mServiceCache = new ArrayList<>();
    private Context mOuterContext = this;

    static {
        registerService(Context.ACCESSIBILITY_SERVICE, new ServiceFetcher() {
            @Override
            public Object getService(ContextImpl ctx) {
                return AccessibilityManager.getInstance(ctx);
            }
        });
        registerService(Context.CAPTIONING_SERVICE, new ServiceFetcher() {
            @Override
            public Object getService(ContextImpl ctx) {
                return new CaptioningManager(ctx);
            }
        });
        registerService("account", new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                IBinder b = ServiceManager.getService("account");
                IAccountManager service = IAccountManager.Stub.asInterface(b);
                return new AccountManager(ctx, service);
            }
        });
        registerService(Context.ACTIVITY_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                return new ActivityManager(ctx.getOuterContext(), ctx.mMainThread.getHandler());
            }
        });
        registerService("alarm", new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                IBinder b = ServiceManager.getService("alarm");
                IAlarmManager service = IAlarmManager.Stub.asInterface(b);
                return new AlarmManager(service, ctx);
            }
        });
        registerService(Context.AUDIO_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                return new AudioManager(ctx);
            }
        });
        registerService(Context.MEDIA_ROUTER_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                return new MediaRouter(ctx);
            }
        });
        registerService("bluetooth", new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                return new BluetoothManager(ctx);
            }
        });
        registerService(Context.HDMI_CONTROL_SERVICE, new StaticServiceFetcher() {
            @Override
            public Object createStaticService() {
                IBinder b = ServiceManager.getService(Context.HDMI_CONTROL_SERVICE);
                return new HdmiControlManager(IHdmiControlService.Stub.asInterface(b));
            }
        });
        registerService(Context.CLIPBOARD_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                return new ClipboardManager(ctx.getOuterContext(), ctx.mMainThread.getHandler());
            }
        });
        registerService(Context.CONNECTIVITY_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                IBinder b = ServiceManager.getService(Context.CONNECTIVITY_SERVICE);
                return new ConnectivityManager(IConnectivityManager.Stub.asInterface(b));
            }
        });
        registerService(Context.COUNTRY_DETECTOR, new StaticServiceFetcher() {
            @Override
            public Object createStaticService() {
                IBinder b = ServiceManager.getService(Context.COUNTRY_DETECTOR);
                return new CountryDetector(ICountryDetector.Stub.asInterface(b));
            }
        });
        registerService(Context.DEVICE_POLICY_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                return DevicePolicyManager.create(ctx, ctx.mMainThread.getHandler());
            }
        });
        registerService(Context.DOWNLOAD_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                return new DownloadManager(ctx.getContentResolver(), ctx.getPackageName());
            }
        });
        registerService(Context.BATTERY_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                return new BatteryManager();
            }
        });
        registerService("nfc", new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                return new NfcManager(ctx);
            }
        });
        registerService(Context.DROPBOX_SERVICE, new StaticServiceFetcher() {
            @Override
            public Object createStaticService() {
                return ContextImpl.createDropBoxManager();
            }
        });
        registerService("input", new StaticServiceFetcher() {
            @Override
            public Object createStaticService() {
                return InputManager.getInstance();
            }
        });
        registerService(Context.DISPLAY_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                return new DisplayManager(ctx.getOuterContext());
            }
        });
        registerService(Context.INPUT_METHOD_SERVICE, new StaticServiceFetcher() {
            @Override
            public Object createStaticService() {
                return InputMethodManager.getInstance();
            }
        });
        registerService(Context.TEXT_SERVICES_MANAGER_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                return TextServicesManager.getInstance();
            }
        });
        registerService(Context.KEYGUARD_SERVICE, new ServiceFetcher() {
            @Override
            public Object getService(ContextImpl ctx) {
                return new KeyguardManager();
            }
        });
        registerService(Context.LAYOUT_INFLATER_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                return PolicyManager.makeNewLayoutInflater(ctx.getOuterContext());
            }
        });
        registerService("location", new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                IBinder b = ServiceManager.getService("location");
                return new LocationManager(ctx, ILocationManager.Stub.asInterface(b));
            }
        });
        registerService(Context.NETWORK_POLICY_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                return new NetworkPolicyManager(INetworkPolicyManager.Stub.asInterface(ServiceManager.getService(Context.NETWORK_POLICY_SERVICE)));
            }
        });
        registerService(Context.NOTIFICATION_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                Context outerContext = ctx.getOuterContext();
                return new NotificationManager(new ContextThemeWrapper(outerContext, Resources.selectSystemTheme(0, outerContext.getApplicationInfo().targetSdkVersion, 16973835, 16973935, 16974126, 16974130)), ctx.mMainThread.getHandler());
            }
        });
        registerService(Context.NSD_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                IBinder b = ServiceManager.getService(Context.NSD_SERVICE);
                INsdManager service = INsdManager.Stub.asInterface(b);
                return new NsdManager(ctx.getOuterContext(), service);
            }
        });
        registerService(Context.POWER_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                IBinder b = ServiceManager.getService(Context.POWER_SERVICE);
                IPowerManager service = IPowerManager.Stub.asInterface(b);
                if (service == null) {
                    Log.wtf(ContextImpl.TAG, "Failed to get power manager service.");
                }
                return new PowerManager(ctx.getOuterContext(), service, ctx.mMainThread.getHandler());
            }
        });
        registerService("search", new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                return new SearchManager(ctx.getOuterContext(), ctx.mMainThread.getHandler());
            }
        });
        registerService(Context.SENSOR_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                return new SystemSensorManager(ctx.getOuterContext(), ctx.mMainThread.getHandler().getLooper());
            }
        });
        registerService(Context.STATUS_BAR_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                return new StatusBarManager(ctx.getOuterContext());
            }
        });
        registerService(Context.STORAGE_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                try {
                    return new StorageManager(ctx.getContentResolver(), ctx.mMainThread.getHandler().getLooper());
                } catch (RemoteException rex) {
                    Log.e(ContextImpl.TAG, "Failed to create StorageManager", rex);
                    return null;
                }
            }
        });
        registerService("phone", new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                return new TelephonyManager(ctx.getOuterContext());
            }
        });
        registerService(Context.TELEPHONY_SUBSCRIPTION_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                return new SubscriptionManager(ctx.getOuterContext());
            }
        });
        registerService(Context.TELECOM_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                return new TelecomManager(ctx.getOuterContext());
            }
        });
        registerService(Context.UI_MODE_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                return new UiModeManager();
            }
        });
        registerService(Context.USB_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                IBinder b = ServiceManager.getService(Context.USB_SERVICE);
                return new UsbManager(ctx, IUsbManager.Stub.asInterface(b));
            }
        });
        registerService(Context.SERIAL_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                IBinder b = ServiceManager.getService(Context.SERIAL_SERVICE);
                return new SerialManager(ctx, ISerialManager.Stub.asInterface(b));
            }
        });
        registerService(Context.VIBRATOR_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                return new SystemVibrator(ctx);
            }
        });
        registerService(Context.WALLPAPER_SERVICE, WALLPAPER_FETCHER);
        registerService("wifi", new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                IBinder b = ServiceManager.getService("wifi");
                IWifiManager service = IWifiManager.Stub.asInterface(b);
                return new WifiManager(ctx.getOuterContext(), service);
            }
        });
        registerService(Context.WIFI_P2P_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                IBinder b = ServiceManager.getService(Context.WIFI_P2P_SERVICE);
                IWifiP2pManager service = IWifiP2pManager.Stub.asInterface(b);
                return new WifiP2pManager(service);
            }
        });
        registerService(Context.WIFI_SCANNING_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                IBinder b = ServiceManager.getService(Context.WIFI_SCANNING_SERVICE);
                IWifiScanner service = IWifiScanner.Stub.asInterface(b);
                return new WifiScanner(ctx.getOuterContext(), service);
            }
        });
        registerService(Context.WIFI_RTT_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                IBinder b = ServiceManager.getService(Context.WIFI_RTT_SERVICE);
                IRttManager service = IRttManager.Stub.asInterface(b);
                return new RttManager(ctx.getOuterContext(), service);
            }
        });
        registerService(Context.ETHERNET_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                IBinder b = ServiceManager.getService(Context.ETHERNET_SERVICE);
                IEthernetManager service = IEthernetManager.Stub.asInterface(b);
                return new EthernetManager(ctx.getOuterContext(), service);
            }
        });
        registerService(Context.WINDOW_SERVICE, new ServiceFetcher() {
            Display mDefaultDisplay;

            @Override
            public Object getService(ContextImpl ctx) {
                Display display = ctx.mDisplay;
                if (display == null) {
                    if (this.mDefaultDisplay == null) {
                        DisplayManager dm = (DisplayManager) ctx.getOuterContext().getSystemService(Context.DISPLAY_SERVICE);
                        this.mDefaultDisplay = dm.getDisplay(0);
                    }
                    display = this.mDefaultDisplay;
                }
                return new WindowManagerImpl(display);
            }
        });
        registerService("user", new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                IBinder b = ServiceManager.getService("user");
                IUserManager service = IUserManager.Stub.asInterface(b);
                return new UserManager(ctx, service);
            }
        });
        registerService(Context.APP_OPS_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                IBinder b = ServiceManager.getService(Context.APP_OPS_SERVICE);
                IAppOpsService service = IAppOpsService.Stub.asInterface(b);
                return new AppOpsManager(ctx, service);
            }
        });
        registerService("camera", new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                return new CameraManager(ctx);
            }
        });
        registerService(Context.LAUNCHER_APPS_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                IBinder b = ServiceManager.getService(Context.LAUNCHER_APPS_SERVICE);
                ILauncherApps service = ILauncherApps.Stub.asInterface(b);
                return new LauncherApps(ctx, service);
            }
        });
        registerService(Context.RESTRICTIONS_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                IBinder b = ServiceManager.getService(Context.RESTRICTIONS_SERVICE);
                IRestrictionsManager service = IRestrictionsManager.Stub.asInterface(b);
                return new RestrictionsManager(ctx, service);
            }
        });
        registerService(Context.PRINT_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                IBinder iBinder = ServiceManager.getService(Context.PRINT_SERVICE);
                IPrintManager service = IPrintManager.Stub.asInterface(iBinder);
                return new PrintManager(ctx.getOuterContext(), service, UserHandle.myUserId(), UserHandle.getAppId(Process.myUid()));
            }
        });
        registerService(Context.CONSUMER_IR_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                return new ConsumerIrManager(ctx);
            }
        });
        registerService(Context.MEDIA_SESSION_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                return new MediaSessionManager(ctx);
            }
        });
        registerService(Context.TRUST_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                IBinder b = ServiceManager.getService(Context.TRUST_SERVICE);
                return new TrustManager(b);
            }
        });
        registerService(Context.FINGERPRINT_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                IBinder binder = ServiceManager.getService(Context.FINGERPRINT_SERVICE);
                IFingerprintService service = IFingerprintService.Stub.asInterface(binder);
                return new FingerprintManager(ctx.getOuterContext(), service);
            }
        });
        registerService(Context.TV_INPUT_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                IBinder iBinder = ServiceManager.getService(Context.TV_INPUT_SERVICE);
                ITvInputManager service = ITvInputManager.Stub.asInterface(iBinder);
                return new TvInputManager(service, UserHandle.myUserId());
            }
        });
        registerService(Context.NETWORK_SCORE_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                return new NetworkScoreManager(ctx);
            }
        });
        registerService(Context.USAGE_STATS_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                IBinder iBinder = ServiceManager.getService(Context.USAGE_STATS_SERVICE);
                IUsageStatsManager service = IUsageStatsManager.Stub.asInterface(iBinder);
                return new UsageStatsManager(ctx.getOuterContext(), service);
            }
        });
        registerService(Context.JOB_SCHEDULER_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                IBinder b = ServiceManager.getService(Context.JOB_SCHEDULER_SERVICE);
                return new JobSchedulerImpl(IJobScheduler.Stub.asInterface(b));
            }
        });
        registerService(Context.PERSISTENT_DATA_BLOCK_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                IBinder b = ServiceManager.getService(Context.PERSISTENT_DATA_BLOCK_SERVICE);
                IPersistentDataBlockService persistentDataBlockService = IPersistentDataBlockService.Stub.asInterface(b);
                if (persistentDataBlockService != null) {
                    return new PersistentDataBlockManager(persistentDataBlockService);
                }
                return null;
            }
        });
        registerService(Context.MEDIA_PROJECTION_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                return new MediaProjectionManager(ctx);
            }
        });
        registerService(Context.APPWIDGET_SERVICE, new ServiceFetcher() {
            @Override
            public Object createService(ContextImpl ctx) {
                IBinder b = ServiceManager.getService(Context.APPWIDGET_SERVICE);
                return new AppWidgetManager(ctx, IAppWidgetService.Stub.asInterface(b));
            }
        });
    }

    static class ServiceFetcher {
        int mContextCacheIndex = -1;

        ServiceFetcher() {
        }

        public Object getService(ContextImpl ctx) {
            ArrayList<Object> cache = ctx.mServiceCache;
            synchronized (cache) {
                if (cache.size() == 0) {
                    for (int i = 0; i < ContextImpl.sNextPerContextServiceCacheIndex; i++) {
                        cache.add(null);
                    }
                } else {
                    Object service = cache.get(this.mContextCacheIndex);
                    if (service != null) {
                        return service;
                    }
                }
                Object service2 = createService(ctx);
                cache.set(this.mContextCacheIndex, service2);
                return service2;
            }
        }

        public Object createService(ContextImpl ctx) {
            throw new RuntimeException("Not implemented");
        }
    }

    static abstract class StaticServiceFetcher extends ServiceFetcher {
        private Object mCachedInstance;

        public abstract Object createStaticService();

        StaticServiceFetcher() {
        }

        @Override
        public final Object getService(ContextImpl unused) {
            Object service;
            synchronized (this) {
                service = this.mCachedInstance;
                if (service == null) {
                    service = createStaticService();
                    this.mCachedInstance = service;
                }
            }
            return service;
        }
    }

    private static void registerService(String serviceName, ServiceFetcher fetcher) {
        if (!(fetcher instanceof StaticServiceFetcher)) {
            int i = sNextPerContextServiceCacheIndex;
            sNextPerContextServiceCacheIndex = i + 1;
            fetcher.mContextCacheIndex = i;
        }
        SYSTEM_SERVICE_MAP.put(serviceName, fetcher);
    }

    static ContextImpl getImpl(Context context) {
        Context nextContext;
        while ((context instanceof ContextWrapper) && (nextContext = ((ContextWrapper) context).getBaseContext()) != null) {
            context = nextContext;
        }
        return (ContextImpl) context;
    }

    @Override
    public AssetManager getAssets() {
        return getResources().getAssets();
    }

    @Override
    public Resources getResources() {
        return this.mResources;
    }

    @Override
    public PackageManager getPackageManager() {
        if (this.mPackageManager != null) {
            return this.mPackageManager;
        }
        IPackageManager pm = ActivityThread.getPackageManager();
        if (pm != null) {
            ApplicationPackageManager applicationPackageManager = new ApplicationPackageManager(this, pm);
            this.mPackageManager = applicationPackageManager;
            return applicationPackageManager;
        }
        return null;
    }

    @Override
    public ContentResolver getContentResolver() {
        return this.mContentResolver;
    }

    @Override
    public Looper getMainLooper() {
        return this.mMainThread.getLooper();
    }

    @Override
    public Context getApplicationContext() {
        return this.mPackageInfo != null ? this.mPackageInfo.getApplication() : this.mMainThread.getApplication();
    }

    @Override
    public void setTheme(int resid) {
        this.mThemeResource = resid;
    }

    @Override
    public int getThemeResId() {
        return this.mThemeResource;
    }

    @Override
    public Resources.Theme getTheme() {
        if (this.mTheme == null) {
            this.mThemeResource = Resources.selectDefaultTheme(this.mThemeResource, getOuterContext().getApplicationInfo().targetSdkVersion);
            this.mTheme = this.mResources.newTheme();
            this.mTheme.applyStyle(this.mThemeResource, true);
        }
        return this.mTheme;
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.mPackageInfo != null ? this.mPackageInfo.getClassLoader() : ClassLoader.getSystemClassLoader();
    }

    @Override
    public String getPackageName() {
        return this.mPackageInfo != null ? this.mPackageInfo.getPackageName() : ZenModeConfig.SYSTEM_AUTHORITY;
    }

    @Override
    public String getBasePackageName() {
        return this.mBasePackageName != null ? this.mBasePackageName : getPackageName();
    }

    @Override
    public String getOpPackageName() {
        return this.mOpPackageName != null ? this.mOpPackageName : getBasePackageName();
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        if (this.mPackageInfo != null) {
            return this.mPackageInfo.getApplicationInfo();
        }
        throw new RuntimeException("Not supported in system context");
    }

    @Override
    public String getPackageResourcePath() {
        if (this.mPackageInfo != null) {
            return this.mPackageInfo.getResDir();
        }
        throw new RuntimeException("Not supported in system context");
    }

    @Override
    public String getPackageCodePath() {
        if (this.mPackageInfo != null) {
            return this.mPackageInfo.getAppDir();
        }
        throw new RuntimeException("Not supported in system context");
    }

    @Override
    public File getSharedPrefsFile(String name) {
        return makeFilename(getPreferencesDir(), name + ".xml");
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        synchronized (ContextImpl.class) {
            if (sSharedPrefs == null) {
                sSharedPrefs = new ArrayMap<>();
            }
            String packageName = getPackageName();
            ArrayMap<String, SharedPreferencesImpl> packagePrefs = sSharedPrefs.get(packageName);
            if (packagePrefs == null) {
                packagePrefs = new ArrayMap<>();
                sSharedPrefs.put(packageName, packagePrefs);
            }
            if (this.mPackageInfo.getApplicationInfo().targetSdkVersion < 19 && name == null) {
                name = "null";
            }
            SharedPreferencesImpl sp = packagePrefs.get(name);
            if (sp == null) {
                File prefsFile = getSharedPrefsFile(name);
                SharedPreferencesImpl sp2 = new SharedPreferencesImpl(prefsFile, mode);
                packagePrefs.put(name, sp2);
                return sp2;
            }
            if ((mode & 4) != 0 || getApplicationInfo().targetSdkVersion < 11) {
                sp.startReloadIfChangedUnexpectedly();
            }
            return sp;
        }
    }

    private File getPreferencesDir() {
        File file;
        synchronized (this.mSync) {
            if (this.mPreferencesDir == null) {
                this.mPreferencesDir = new File(getDataDirFile(), "shared_prefs");
            }
            file = this.mPreferencesDir;
        }
        return file;
    }

    @Override
    public FileInputStream openFileInput(String name) throws FileNotFoundException {
        File f = makeFilename(getFilesDir(), name);
        return new FileInputStream(f);
    }

    @Override
    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
        boolean append = (32768 & mode) != 0;
        File f = makeFilename(getFilesDir(), name);
        try {
            FileOutputStream fos = new FileOutputStream(f, append);
            setFilePermissionsFromMode(f.getPath(), mode, 0);
            return fos;
        } catch (FileNotFoundException e) {
            File parent = f.getParentFile();
            parent.mkdir();
            FileUtils.setPermissions(parent.getPath(), 505, -1, -1);
            FileOutputStream fos2 = new FileOutputStream(f, append);
            setFilePermissionsFromMode(f.getPath(), mode, 0);
            return fos2;
        }
    }

    @Override
    public boolean deleteFile(String name) {
        File f = makeFilename(getFilesDir(), name);
        return f.delete();
    }

    private static File createFilesDirLocked(File file) {
        if (!file.exists()) {
            if (!file.mkdirs()) {
                if (!file.exists()) {
                    Log.w(TAG, "Unable to create files subdir " + file.getPath());
                    return null;
                }
                return file;
            }
            FileUtils.setPermissions(file.getPath(), 505, -1, -1);
            return file;
        }
        return file;
    }

    @Override
    public File getFilesDir() {
        File fileCreateFilesDirLocked;
        synchronized (this.mSync) {
            if (this.mFilesDir == null) {
                this.mFilesDir = new File(getDataDirFile(), "files");
            }
            fileCreateFilesDirLocked = createFilesDirLocked(this.mFilesDir);
        }
        return fileCreateFilesDirLocked;
    }

    @Override
    public File getNoBackupFilesDir() {
        File fileCreateFilesDirLocked;
        synchronized (this.mSync) {
            if (this.mNoBackupFilesDir == null) {
                this.mNoBackupFilesDir = new File(getDataDirFile(), "no_backup");
            }
            fileCreateFilesDirLocked = createFilesDirLocked(this.mNoBackupFilesDir);
        }
        return fileCreateFilesDirLocked;
    }

    @Override
    public File getExternalFilesDir(String type) {
        return getExternalFilesDirs(type)[0];
    }

    @Override
    public File[] getExternalFilesDirs(String type) {
        File[] fileArrEnsureDirsExistOrFilter;
        synchronized (this.mSync) {
            if (this.mExternalFilesDirs == null) {
                this.mExternalFilesDirs = Environment.buildExternalStorageAppFilesDirs(getPackageName());
            }
            File[] dirs = this.mExternalFilesDirs;
            if (type != null) {
                dirs = Environment.buildPaths(dirs, type);
            }
            fileArrEnsureDirsExistOrFilter = ensureDirsExistOrFilter(dirs);
        }
        return fileArrEnsureDirsExistOrFilter;
    }

    @Override
    public File getObbDir() {
        return getObbDirs()[0];
    }

    @Override
    public File[] getObbDirs() {
        File[] fileArrEnsureDirsExistOrFilter;
        synchronized (this.mSync) {
            if (this.mExternalObbDirs == null) {
                this.mExternalObbDirs = Environment.buildExternalStorageAppObbDirs(getPackageName());
            }
            fileArrEnsureDirsExistOrFilter = ensureDirsExistOrFilter(this.mExternalObbDirs);
        }
        return fileArrEnsureDirsExistOrFilter;
    }

    @Override
    public File getCacheDir() {
        File fileCreateFilesDirLocked;
        synchronized (this.mSync) {
            if (this.mCacheDir == null) {
                this.mCacheDir = new File(getDataDirFile(), "cache");
            }
            fileCreateFilesDirLocked = createFilesDirLocked(this.mCacheDir);
        }
        return fileCreateFilesDirLocked;
    }

    @Override
    public File getCodeCacheDir() {
        File fileCreateFilesDirLocked;
        synchronized (this.mSync) {
            if (this.mCodeCacheDir == null) {
                this.mCodeCacheDir = new File(getDataDirFile(), "code_cache");
            }
            fileCreateFilesDirLocked = createFilesDirLocked(this.mCodeCacheDir);
        }
        return fileCreateFilesDirLocked;
    }

    @Override
    public File getExternalCacheDir() {
        return getExternalCacheDirs()[0];
    }

    @Override
    public File[] getExternalCacheDirs() {
        File[] fileArrEnsureDirsExistOrFilter;
        synchronized (this.mSync) {
            if (this.mExternalCacheDirs == null) {
                this.mExternalCacheDirs = Environment.buildExternalStorageAppCacheDirs(getPackageName());
            }
            fileArrEnsureDirsExistOrFilter = ensureDirsExistOrFilter(this.mExternalCacheDirs);
        }
        return fileArrEnsureDirsExistOrFilter;
    }

    @Override
    public File[] getExternalMediaDirs() {
        File[] fileArrEnsureDirsExistOrFilter;
        synchronized (this.mSync) {
            if (this.mExternalMediaDirs == null) {
                this.mExternalMediaDirs = Environment.buildExternalStorageAppMediaDirs(getPackageName());
            }
            fileArrEnsureDirsExistOrFilter = ensureDirsExistOrFilter(this.mExternalMediaDirs);
        }
        return fileArrEnsureDirsExistOrFilter;
    }

    @Override
    public File getFileStreamPath(String name) {
        return makeFilename(getFilesDir(), name);
    }

    @Override
    public String[] fileList() {
        String[] list = getFilesDir().list();
        return list != null ? list : EMPTY_FILE_LIST;
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
        return openOrCreateDatabase(name, mode, factory, null);
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler errorHandler) {
        File f = validateFilePath(name, true);
        int flags = (mode & 8) != 0 ? 268435456 | 536870912 : 268435456;
        SQLiteDatabase db = SQLiteDatabase.openDatabase(f.getPath(), factory, flags, errorHandler);
        setFilePermissionsFromMode(f.getPath(), mode, 0);
        return db;
    }

    @Override
    public boolean deleteDatabase(String name) {
        try {
            File f = validateFilePath(name, false);
            return SQLiteDatabase.deleteDatabase(f);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public File getDatabasePath(String name) {
        return validateFilePath(name, false);
    }

    @Override
    public String[] databaseList() {
        String[] list = getDatabasesDir().list();
        return list != null ? list : EMPTY_FILE_LIST;
    }

    private File getDatabasesDir() {
        File file;
        synchronized (this.mSync) {
            if (this.mDatabasesDir == null) {
                this.mDatabasesDir = new File(getDataDirFile(), "databases");
            }
            if (this.mDatabasesDir.getPath().equals("databases")) {
                this.mDatabasesDir = new File("/data/system");
            }
            file = this.mDatabasesDir;
        }
        return file;
    }

    @Override
    public Drawable getWallpaper() {
        return getWallpaperManager().getDrawable();
    }

    @Override
    public Drawable peekWallpaper() {
        return getWallpaperManager().peekDrawable();
    }

    @Override
    public int getWallpaperDesiredMinimumWidth() {
        return getWallpaperManager().getDesiredMinimumWidth();
    }

    @Override
    public int getWallpaperDesiredMinimumHeight() {
        return getWallpaperManager().getDesiredMinimumHeight();
    }

    @Override
    public void setWallpaper(Bitmap bitmap) throws Throwable {
        getWallpaperManager().setBitmap(bitmap);
    }

    @Override
    public void setWallpaper(InputStream data) throws Throwable {
        getWallpaperManager().setStream(data);
    }

    @Override
    public void clearWallpaper() throws Throwable {
        getWallpaperManager().clear();
    }

    @Override
    public void startActivity(Intent intent) {
        warnIfCallingFromSystemProcess();
        startActivity(intent, null);
    }

    @Override
    public void startActivityAsUser(Intent intent, UserHandle user) {
        startActivityAsUser(intent, null, user);
    }

    @Override
    public void startActivity(Intent intent, Bundle options) {
        warnIfCallingFromSystemProcess();
        if ((intent.getFlags() & 268435456) == 0) {
            throw new AndroidRuntimeException("Calling startActivity() from outside of an Activity  context requires the FLAG_ACTIVITY_NEW_TASK flag. Is this really what you want?");
        }
        this.mMainThread.getInstrumentation().execStartActivity(getOuterContext(), this.mMainThread.getApplicationThread(), (IBinder) null, (Activity) null, intent, -1, options);
    }

    @Override
    public void startActivityAsUser(Intent intent, Bundle options, UserHandle user) {
        try {
            ActivityManagerNative.getDefault().startActivityAsUser(this.mMainThread.getApplicationThread(), getBasePackageName(), intent, intent.resolveTypeIfNeeded(getContentResolver()), null, null, 0, 268435456, null, options, user.getIdentifier());
        } catch (RemoteException e) {
        }
    }

    @Override
    public void startActivities(Intent[] intents) {
        warnIfCallingFromSystemProcess();
        startActivities(intents, null);
    }

    @Override
    public void startActivitiesAsUser(Intent[] intents, Bundle options, UserHandle userHandle) {
        if ((intents[0].getFlags() & 268435456) == 0) {
            throw new AndroidRuntimeException("Calling startActivities() from outside of an Activity  context requires the FLAG_ACTIVITY_NEW_TASK flag on first Intent. Is this really what you want?");
        }
        this.mMainThread.getInstrumentation().execStartActivitiesAsUser(getOuterContext(), this.mMainThread.getApplicationThread(), null, (Activity) null, intents, options, userHandle.getIdentifier());
    }

    @Override
    public void startActivities(Intent[] intents, Bundle options) {
        warnIfCallingFromSystemProcess();
        if ((intents[0].getFlags() & 268435456) == 0) {
            throw new AndroidRuntimeException("Calling startActivities() from outside of an Activity  context requires the FLAG_ACTIVITY_NEW_TASK flag on first Intent. Is this really what you want?");
        }
        this.mMainThread.getInstrumentation().execStartActivities(getOuterContext(), this.mMainThread.getApplicationThread(), null, (Activity) null, intents, options);
    }

    @Override
    public void startIntentSender(IntentSender intent, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags) throws IntentSender.SendIntentException {
        startIntentSender(intent, fillInIntent, flagsMask, flagsValues, extraFlags, null);
    }

    @Override
    public void startIntentSender(IntentSender intent, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, Bundle options) throws IntentSender.SendIntentException {
        String resolvedType = null;
        if (fillInIntent != null) {
            try {
                fillInIntent.migrateExtraStreamToClipData();
                fillInIntent.prepareToLeaveProcess();
                resolvedType = fillInIntent.resolveTypeIfNeeded(getContentResolver());
            } catch (RemoteException e) {
                return;
            }
        }
        int result = ActivityManagerNative.getDefault().startActivityIntentSender(this.mMainThread.getApplicationThread(), intent, fillInIntent, resolvedType, null, null, 0, flagsMask, flagsValues, options);
        if (result == -6) {
            throw new IntentSender.SendIntentException();
        }
        Instrumentation.checkStartActivityResult(result, null);
    }

    @Override
    public void sendBroadcast(Intent intent) {
        warnIfCallingFromSystemProcess();
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        try {
            intent.prepareToLeaveProcess();
            ActivityManagerNative.getDefault().broadcastIntent(this.mMainThread.getApplicationThread(), intent, resolvedType, null, -1, null, null, null, -1, false, false, getUserId());
        } catch (RemoteException e) {
        }
    }

    @Override
    public void sendBroadcast(Intent intent, String receiverPermission) {
        warnIfCallingFromSystemProcess();
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        try {
            intent.prepareToLeaveProcess();
            ActivityManagerNative.getDefault().broadcastIntent(this.mMainThread.getApplicationThread(), intent, resolvedType, null, -1, null, null, receiverPermission, -1, false, false, getUserId());
        } catch (RemoteException e) {
        }
    }

    @Override
    public void sendBroadcast(Intent intent, String receiverPermission, int appOp) {
        warnIfCallingFromSystemProcess();
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        try {
            intent.prepareToLeaveProcess();
            ActivityManagerNative.getDefault().broadcastIntent(this.mMainThread.getApplicationThread(), intent, resolvedType, null, -1, null, null, receiverPermission, appOp, false, false, getUserId());
        } catch (RemoteException e) {
        }
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String receiverPermission) {
        warnIfCallingFromSystemProcess();
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        try {
            intent.prepareToLeaveProcess();
            ActivityManagerNative.getDefault().broadcastIntent(this.mMainThread.getApplicationThread(), intent, resolvedType, null, -1, null, null, receiverPermission, -1, true, false, getUserId());
        } catch (RemoteException e) {
        }
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String receiverPermission, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
        sendOrderedBroadcast(intent, receiverPermission, -1, resultReceiver, scheduler, initialCode, initialData, initialExtras);
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String receiverPermission, int appOp, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
        warnIfCallingFromSystemProcess();
        IIntentReceiver rd = null;
        if (resultReceiver != null) {
            if (this.mPackageInfo != null) {
                if (scheduler == null) {
                    scheduler = this.mMainThread.getHandler();
                }
                rd = this.mPackageInfo.getReceiverDispatcher(resultReceiver, getOuterContext(), scheduler, this.mMainThread.getInstrumentation(), false);
            } else {
                if (scheduler == null) {
                    scheduler = this.mMainThread.getHandler();
                }
                rd = new LoadedApk.ReceiverDispatcher(resultReceiver, getOuterContext(), scheduler, null, false).getIIntentReceiver();
            }
        }
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        try {
            intent.prepareToLeaveProcess();
            ActivityManagerNative.getDefault().broadcastIntent(this.mMainThread.getApplicationThread(), intent, resolvedType, rd, initialCode, initialData, initialExtras, receiverPermission, appOp, true, false, getUserId());
        } catch (RemoteException e) {
        }
    }

    @Override
    public void sendBroadcastAsUser(Intent intent, UserHandle user) {
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        try {
            intent.prepareToLeaveProcess();
            ActivityManagerNative.getDefault().broadcastIntent(this.mMainThread.getApplicationThread(), intent, resolvedType, null, -1, null, null, null, -1, false, false, user.getIdentifier());
        } catch (RemoteException e) {
        }
    }

    @Override
    public void sendBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission) {
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        try {
            intent.prepareToLeaveProcess();
            ActivityManagerNative.getDefault().broadcastIntent(this.mMainThread.getApplicationThread(), intent, resolvedType, null, -1, null, null, receiverPermission, -1, false, false, user.getIdentifier());
        } catch (RemoteException e) {
        }
    }

    @Override
    public void sendOrderedBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
        sendOrderedBroadcastAsUser(intent, user, receiverPermission, -1, resultReceiver, scheduler, initialCode, initialData, initialExtras);
    }

    @Override
    public void sendOrderedBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission, int appOp, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
        IIntentReceiver rd = null;
        if (resultReceiver != null) {
            if (this.mPackageInfo != null) {
                if (scheduler == null) {
                    scheduler = this.mMainThread.getHandler();
                }
                rd = this.mPackageInfo.getReceiverDispatcher(resultReceiver, getOuterContext(), scheduler, this.mMainThread.getInstrumentation(), false);
            } else {
                if (scheduler == null) {
                    scheduler = this.mMainThread.getHandler();
                }
                rd = new LoadedApk.ReceiverDispatcher(resultReceiver, getOuterContext(), scheduler, null, false).getIIntentReceiver();
            }
        }
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        try {
            intent.prepareToLeaveProcess();
            ActivityManagerNative.getDefault().broadcastIntent(this.mMainThread.getApplicationThread(), intent, resolvedType, rd, initialCode, initialData, initialExtras, receiverPermission, appOp, true, false, user.getIdentifier());
        } catch (RemoteException e) {
        }
    }

    @Override
    public void sendStickyBroadcast(Intent intent) {
        warnIfCallingFromSystemProcess();
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        try {
            intent.prepareToLeaveProcess();
            ActivityManagerNative.getDefault().broadcastIntent(this.mMainThread.getApplicationThread(), intent, resolvedType, null, -1, null, null, null, -1, false, true, getUserId());
        } catch (RemoteException e) {
        }
    }

    @Override
    public void sendStickyOrderedBroadcast(Intent intent, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
        warnIfCallingFromSystemProcess();
        IIntentReceiver rd = null;
        if (resultReceiver != null) {
            if (this.mPackageInfo != null) {
                if (scheduler == null) {
                    scheduler = this.mMainThread.getHandler();
                }
                rd = this.mPackageInfo.getReceiverDispatcher(resultReceiver, getOuterContext(), scheduler, this.mMainThread.getInstrumentation(), false);
            } else {
                if (scheduler == null) {
                    scheduler = this.mMainThread.getHandler();
                }
                rd = new LoadedApk.ReceiverDispatcher(resultReceiver, getOuterContext(), scheduler, null, false).getIIntentReceiver();
            }
        }
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        try {
            intent.prepareToLeaveProcess();
            ActivityManagerNative.getDefault().broadcastIntent(this.mMainThread.getApplicationThread(), intent, resolvedType, rd, initialCode, initialData, initialExtras, null, -1, true, true, getUserId());
        } catch (RemoteException e) {
        }
    }

    @Override
    public void removeStickyBroadcast(Intent intent) {
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        if (resolvedType != null) {
            Intent intent2 = new Intent(intent);
            intent2.setDataAndType(intent2.getData(), resolvedType);
            intent = intent2;
        }
        try {
            intent.prepareToLeaveProcess();
            ActivityManagerNative.getDefault().unbroadcastIntent(this.mMainThread.getApplicationThread(), intent, getUserId());
        } catch (RemoteException e) {
        }
    }

    @Override
    public void sendStickyBroadcastAsUser(Intent intent, UserHandle user) {
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        try {
            intent.prepareToLeaveProcess();
            ActivityManagerNative.getDefault().broadcastIntent(this.mMainThread.getApplicationThread(), intent, resolvedType, null, -1, null, null, null, -1, false, true, user.getIdentifier());
        } catch (RemoteException e) {
        }
    }

    @Override
    public void sendStickyOrderedBroadcastAsUser(Intent intent, UserHandle user, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
        IIntentReceiver rd = null;
        if (resultReceiver != null) {
            if (this.mPackageInfo != null) {
                if (scheduler == null) {
                    scheduler = this.mMainThread.getHandler();
                }
                rd = this.mPackageInfo.getReceiverDispatcher(resultReceiver, getOuterContext(), scheduler, this.mMainThread.getInstrumentation(), false);
            } else {
                if (scheduler == null) {
                    scheduler = this.mMainThread.getHandler();
                }
                rd = new LoadedApk.ReceiverDispatcher(resultReceiver, getOuterContext(), scheduler, null, false).getIIntentReceiver();
            }
        }
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        try {
            intent.prepareToLeaveProcess();
            ActivityManagerNative.getDefault().broadcastIntent(this.mMainThread.getApplicationThread(), intent, resolvedType, rd, initialCode, initialData, initialExtras, null, -1, true, true, user.getIdentifier());
        } catch (RemoteException e) {
        }
    }

    @Override
    public void removeStickyBroadcastAsUser(Intent intent, UserHandle user) {
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        if (resolvedType != null) {
            Intent intent2 = new Intent(intent);
            intent2.setDataAndType(intent2.getData(), resolvedType);
            intent = intent2;
        }
        try {
            intent.prepareToLeaveProcess();
            ActivityManagerNative.getDefault().unbroadcastIntent(this.mMainThread.getApplicationThread(), intent, user.getIdentifier());
        } catch (RemoteException e) {
        }
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        return registerReceiver(receiver, filter, null, null);
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, String broadcastPermission, Handler scheduler) {
        return registerReceiverInternal(receiver, getUserId(), filter, broadcastPermission, scheduler, getOuterContext());
    }

    @Override
    public Intent registerReceiverAsUser(BroadcastReceiver receiver, UserHandle user, IntentFilter filter, String broadcastPermission, Handler scheduler) {
        return registerReceiverInternal(receiver, user.getIdentifier(), filter, broadcastPermission, scheduler, getOuterContext());
    }

    private Intent registerReceiverInternal(BroadcastReceiver receiver, int userId, IntentFilter filter, String broadcastPermission, Handler scheduler, Context context) {
        IIntentReceiver rd = null;
        if (receiver != null) {
            if (this.mPackageInfo != null && context != null) {
                if (scheduler == null) {
                    scheduler = this.mMainThread.getHandler();
                }
                rd = this.mPackageInfo.getReceiverDispatcher(receiver, context, scheduler, this.mMainThread.getInstrumentation(), true);
            } else {
                if (scheduler == null) {
                    scheduler = this.mMainThread.getHandler();
                }
                rd = new LoadedApk.ReceiverDispatcher(receiver, context, scheduler, null, true).getIIntentReceiver();
            }
        }
        try {
            return ActivityManagerNative.getDefault().registerReceiver(this.mMainThread.getApplicationThread(), this.mBasePackageName, rd, filter, broadcastPermission, userId);
        } catch (RemoteException e) {
            return null;
        }
    }

    @Override
    public void unregisterReceiver(BroadcastReceiver receiver) {
        if (this.mPackageInfo != null) {
            IIntentReceiver rd = this.mPackageInfo.forgetReceiverDispatcher(getOuterContext(), receiver);
            try {
                ActivityManagerNative.getDefault().unregisterReceiver(rd);
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        throw new RuntimeException("Not supported in system context");
    }

    private void validateServiceIntent(Intent service) {
        if (service.getComponent() == null && service.getPackage() == null) {
            if (getApplicationInfo().targetSdkVersion >= 21) {
                IllegalArgumentException ex = new IllegalArgumentException("Service Intent must be explicit: " + service);
                throw ex;
            }
            Log.w(TAG, "Implicit intents with startService are not safe: " + service + " " + Debug.getCallers(2, 3));
        }
    }

    @Override
    public ComponentName startService(Intent service) {
        warnIfCallingFromSystemProcess();
        return startServiceCommon(service, this.mUser);
    }

    @Override
    public boolean stopService(Intent service) {
        warnIfCallingFromSystemProcess();
        return stopServiceCommon(service, this.mUser);
    }

    @Override
    public ComponentName startServiceAsUser(Intent service, UserHandle user) {
        return startServiceCommon(service, user);
    }

    private ComponentName startServiceCommon(Intent service, UserHandle user) {
        try {
            validateServiceIntent(service);
            service.prepareToLeaveProcess();
            ComponentName cn = ActivityManagerNative.getDefault().startService(this.mMainThread.getApplicationThread(), service, service.resolveTypeIfNeeded(getContentResolver()), user.getIdentifier());
            if (cn == null) {
                return cn;
            }
            if (cn.getPackageName().equals("!")) {
                throw new SecurityException("Not allowed to start service " + service + " without permission " + cn.getClassName());
            }
            if (cn.getPackageName().equals("!!")) {
                throw new SecurityException("Unable to start service " + service + ": " + cn.getClassName());
            }
            return cn;
        } catch (RemoteException e) {
            return null;
        }
    }

    @Override
    public boolean stopServiceAsUser(Intent service, UserHandle user) {
        return stopServiceCommon(service, user);
    }

    private boolean stopServiceCommon(Intent service, UserHandle user) {
        try {
            validateServiceIntent(service);
            service.prepareToLeaveProcess();
            int res = ActivityManagerNative.getDefault().stopService(this.mMainThread.getApplicationThread(), service, service.resolveTypeIfNeeded(getContentResolver()), user.getIdentifier());
            if (res < 0) {
                throw new SecurityException("Not allowed to stop service " + service);
            }
            return res != 0;
        } catch (RemoteException e) {
            return false;
        }
    }

    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        warnIfCallingFromSystemProcess();
        return bindServiceCommon(service, conn, flags, Process.myUserHandle());
    }

    @Override
    public boolean bindServiceAsUser(Intent service, ServiceConnection conn, int flags, UserHandle user) {
        return bindServiceCommon(service, conn, flags, user);
    }

    private boolean bindServiceCommon(Intent service, ServiceConnection conn, int flags, UserHandle user) {
        if (conn == null) {
            throw new IllegalArgumentException("connection is null");
        }
        if (this.mPackageInfo != null) {
            IServiceConnection sd = this.mPackageInfo.getServiceDispatcher(conn, getOuterContext(), this.mMainThread.getHandler(), flags);
            validateServiceIntent(service);
            try {
                IBinder token = getActivityToken();
                if (token == null && (flags & 1) == 0 && this.mPackageInfo != null && this.mPackageInfo.getApplicationInfo().targetSdkVersion < 14) {
                    flags |= 32;
                }
                service.prepareToLeaveProcess();
                int res = ActivityManagerNative.getDefault().bindService(this.mMainThread.getApplicationThread(), getActivityToken(), service, service.resolveTypeIfNeeded(getContentResolver()), sd, flags, user.getIdentifier());
                if (res < 0) {
                    throw new SecurityException("Not allowed to bind to service " + service);
                }
                return res != 0;
            } catch (RemoteException e) {
                return false;
            }
        }
        throw new RuntimeException("Not supported in system context");
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        if (conn == null) {
            throw new IllegalArgumentException("connection is null");
        }
        if (this.mPackageInfo != null) {
            IServiceConnection sd = this.mPackageInfo.forgetServiceDispatcher(getOuterContext(), conn);
            try {
                ActivityManagerNative.getDefault().unbindService(sd);
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        throw new RuntimeException("Not supported in system context");
    }

    @Override
    public boolean startInstrumentation(ComponentName className, String profileFile, Bundle arguments) {
        if (arguments != null) {
            try {
                arguments.setAllowFds(false);
            } catch (RemoteException e) {
                return false;
            }
        }
        return ActivityManagerNative.getDefault().startInstrumentation(className, profileFile, 0, arguments, null, null, getUserId(), null);
    }

    @Override
    public Object getSystemService(String name) {
        ServiceFetcher fetcher = SYSTEM_SERVICE_MAP.get(name);
        if (fetcher == null) {
            return null;
        }
        return fetcher.getService(this);
    }

    private WallpaperManager getWallpaperManager() {
        return (WallpaperManager) WALLPAPER_FETCHER.getService(this);
    }

    static DropBoxManager createDropBoxManager() {
        IBinder b = ServiceManager.getService(Context.DROPBOX_SERVICE);
        IDropBoxManagerService service = IDropBoxManagerService.Stub.asInterface(b);
        if (service == null) {
            return null;
        }
        return new DropBoxManager(service);
    }

    @Override
    public int checkPermission(String permission, int pid, int uid) {
        if (permission == null) {
            throw new IllegalArgumentException("permission is null");
        }
        try {
            return ActivityManagerNative.getDefault().checkPermission(permission, pid, uid);
        } catch (RemoteException e) {
            return -1;
        }
    }

    @Override
    public int checkPermission(String permission, int pid, int uid, IBinder callerToken) {
        if (permission == null) {
            throw new IllegalArgumentException("permission is null");
        }
        try {
            return ActivityManagerNative.getDefault().checkPermissionWithToken(permission, pid, uid, callerToken);
        } catch (RemoteException e) {
            return -1;
        }
    }

    @Override
    public int checkCallingPermission(String permission) {
        if (permission == null) {
            throw new IllegalArgumentException("permission is null");
        }
        int pid = Binder.getCallingPid();
        if (pid != Process.myPid()) {
            return checkPermission(permission, pid, Binder.getCallingUid());
        }
        return -1;
    }

    @Override
    public int checkCallingOrSelfPermission(String permission) {
        if (permission == null) {
            throw new IllegalArgumentException("permission is null");
        }
        return checkPermission(permission, Binder.getCallingPid(), Binder.getCallingUid());
    }

    private void enforce(String permission, int resultOfCheck, boolean selfToo, int uid, String message) {
        if (resultOfCheck != 0) {
            throw new SecurityException((message != null ? message + ": " : ProxyInfo.LOCAL_EXCL_LIST) + (selfToo ? "Neither user " + uid + " nor current process has " : "uid " + uid + " does not have ") + permission + ".");
        }
    }

    @Override
    public void enforcePermission(String permission, int pid, int uid, String message) {
        enforce(permission, checkPermission(permission, pid, uid), false, uid, message);
    }

    @Override
    public void enforceCallingPermission(String permission, String message) {
        enforce(permission, checkCallingPermission(permission), false, Binder.getCallingUid(), message);
    }

    @Override
    public void enforceCallingOrSelfPermission(String permission, String message) {
        enforce(permission, checkCallingOrSelfPermission(permission), true, Binder.getCallingUid(), message);
    }

    @Override
    public void grantUriPermission(String toPackage, Uri uri, int modeFlags) {
        try {
            ActivityManagerNative.getDefault().grantUriPermission(this.mMainThread.getApplicationThread(), toPackage, ContentProvider.getUriWithoutUserId(uri), modeFlags, resolveUserId(uri));
        } catch (RemoteException e) {
        }
    }

    @Override
    public void revokeUriPermission(Uri uri, int modeFlags) {
        try {
            ActivityManagerNative.getDefault().revokeUriPermission(this.mMainThread.getApplicationThread(), ContentProvider.getUriWithoutUserId(uri), modeFlags, resolveUserId(uri));
        } catch (RemoteException e) {
        }
    }

    @Override
    public int checkUriPermission(Uri uri, int pid, int uid, int modeFlags) {
        try {
            return ActivityManagerNative.getDefault().checkUriPermission(ContentProvider.getUriWithoutUserId(uri), pid, uid, modeFlags, resolveUserId(uri), null);
        } catch (RemoteException e) {
            return -1;
        }
    }

    @Override
    public int checkUriPermission(Uri uri, int pid, int uid, int modeFlags, IBinder callerToken) {
        try {
            return ActivityManagerNative.getDefault().checkUriPermission(ContentProvider.getUriWithoutUserId(uri), pid, uid, modeFlags, resolveUserId(uri), callerToken);
        } catch (RemoteException e) {
            return -1;
        }
    }

    private int resolveUserId(Uri uri) {
        return ContentProvider.getUserIdFromUri(uri, getUserId());
    }

    @Override
    public int checkCallingUriPermission(Uri uri, int modeFlags) {
        int pid = Binder.getCallingPid();
        if (pid != Process.myPid()) {
            return checkUriPermission(uri, pid, Binder.getCallingUid(), modeFlags);
        }
        return -1;
    }

    @Override
    public int checkCallingOrSelfUriPermission(Uri uri, int modeFlags) {
        return checkUriPermission(uri, Binder.getCallingPid(), Binder.getCallingUid(), modeFlags);
    }

    @Override
    public int checkUriPermission(Uri uri, String readPermission, String writePermission, int pid, int uid, int modeFlags) {
        if ((modeFlags & 1) != 0 && (readPermission == null || checkPermission(readPermission, pid, uid) == 0)) {
            return 0;
        }
        if ((modeFlags & 2) != 0 && (writePermission == null || checkPermission(writePermission, pid, uid) == 0)) {
            return 0;
        }
        if (uri != null) {
            return checkUriPermission(uri, pid, uid, modeFlags);
        }
        return -1;
    }

    private String uriModeFlagToString(int uriModeFlags) {
        StringBuilder builder = new StringBuilder();
        if ((uriModeFlags & 1) != 0) {
            builder.append("read and ");
        }
        if ((uriModeFlags & 2) != 0) {
            builder.append("write and ");
        }
        if ((uriModeFlags & 64) != 0) {
            builder.append("persistable and ");
        }
        if ((uriModeFlags & 128) != 0) {
            builder.append("prefix and ");
        }
        if (builder.length() > 5) {
            builder.setLength(builder.length() - 5);
            return builder.toString();
        }
        throw new IllegalArgumentException("Unknown permission mode flags: " + uriModeFlags);
    }

    private void enforceForUri(int modeFlags, int resultOfCheck, boolean selfToo, int uid, Uri uri, String message) {
        if (resultOfCheck != 0) {
            throw new SecurityException((message != null ? message + ": " : ProxyInfo.LOCAL_EXCL_LIST) + (selfToo ? "Neither user " + uid + " nor current process has " : "User " + uid + " does not have ") + uriModeFlagToString(modeFlags) + " permission on " + uri + ".");
        }
    }

    @Override
    public void enforceUriPermission(Uri uri, int pid, int uid, int modeFlags, String message) {
        enforceForUri(modeFlags, checkUriPermission(uri, pid, uid, modeFlags), false, uid, uri, message);
    }

    @Override
    public void enforceCallingUriPermission(Uri uri, int modeFlags, String message) {
        enforceForUri(modeFlags, checkCallingUriPermission(uri, modeFlags), false, Binder.getCallingUid(), uri, message);
    }

    @Override
    public void enforceCallingOrSelfUriPermission(Uri uri, int modeFlags, String message) {
        enforceForUri(modeFlags, checkCallingOrSelfUriPermission(uri, modeFlags), true, Binder.getCallingUid(), uri, message);
    }

    @Override
    public void enforceUriPermission(Uri uri, String readPermission, String writePermission, int pid, int uid, int modeFlags, String message) {
        enforceForUri(modeFlags, checkUriPermission(uri, readPermission, writePermission, pid, uid, modeFlags), false, uid, uri, message);
    }

    private void warnIfCallingFromSystemProcess() {
        if (Process.myUid() == 1000) {
            Slog.w(TAG, "Calling a method in the system process without a qualified user: " + Debug.getCallers(5));
        }
    }

    @Override
    public Context createApplicationContext(ApplicationInfo application, int flags) throws PackageManager.NameNotFoundException {
        LoadedApk pi = this.mMainThread.getPackageInfo(application, this.mResources.getCompatibilityInfo(), 1073741824 | flags);
        if (pi != null) {
            boolean restricted = (flags & 4) == 4;
            ContextImpl c = new ContextImpl(this, this.mMainThread, pi, this.mActivityToken, new UserHandle(UserHandle.getUserId(application.uid)), restricted, this.mDisplay, this.mOverrideConfiguration);
            if (c.mResources != null) {
                return c;
            }
        }
        throw new PackageManager.NameNotFoundException("Application package " + application.packageName + " not found");
    }

    @Override
    public Context createPackageContext(String packageName, int flags) throws PackageManager.NameNotFoundException {
        return createPackageContextAsUser(packageName, flags, this.mUser != null ? this.mUser : Process.myUserHandle());
    }

    @Override
    public Context createPackageContextAsUser(String packageName, int flags, UserHandle user) throws PackageManager.NameNotFoundException {
        boolean restricted = (flags & 4) == 4;
        if (packageName.equals("system") || packageName.equals(ZenModeConfig.SYSTEM_AUTHORITY)) {
            return new ContextImpl(this, this.mMainThread, this.mPackageInfo, this.mActivityToken, user, restricted, this.mDisplay, this.mOverrideConfiguration);
        }
        LoadedApk pi = this.mMainThread.getPackageInfo(packageName, this.mResources.getCompatibilityInfo(), 1073741824 | flags, user.getIdentifier());
        if (pi != null) {
            ContextImpl c = new ContextImpl(this, this.mMainThread, pi, this.mActivityToken, user, restricted, this.mDisplay, this.mOverrideConfiguration);
            if (c.mResources != null) {
                return c;
            }
        }
        throw new PackageManager.NameNotFoundException("Application package " + packageName + " not found");
    }

    @Override
    public Context createConfigurationContext(Configuration overrideConfiguration) {
        if (overrideConfiguration == null) {
            throw new IllegalArgumentException("overrideConfiguration must not be null");
        }
        return new ContextImpl(this, this.mMainThread, this.mPackageInfo, this.mActivityToken, this.mUser, this.mRestricted, this.mDisplay, overrideConfiguration);
    }

    @Override
    public Context createDisplayContext(Display display) {
        if (display == null) {
            throw new IllegalArgumentException("display must not be null");
        }
        return new ContextImpl(this, this.mMainThread, this.mPackageInfo, this.mActivityToken, this.mUser, this.mRestricted, display, this.mOverrideConfiguration);
    }

    private int getDisplayId() {
        if (this.mDisplay != null) {
            return this.mDisplay.getDisplayId();
        }
        return 0;
    }

    @Override
    public boolean isRestricted() {
        return this.mRestricted;
    }

    @Override
    public DisplayAdjustments getDisplayAdjustments(int displayId) {
        return this.mDisplayAdjustments;
    }

    private File getDataDirFile() {
        if (this.mPackageInfo != null) {
            return this.mPackageInfo.getDataDirFile();
        }
        throw new RuntimeException("Not supported in system context");
    }

    @Override
    public File getDir(String name, int mode) {
        File file = makeFilename(getDataDirFile(), "app_" + name);
        if (!file.exists()) {
            file.mkdir();
            setFilePermissionsFromMode(file.getPath(), mode, 505);
        }
        return file;
    }

    @Override
    public int getUserId() {
        return this.mUser.getIdentifier();
    }

    static ContextImpl createSystemContext(ActivityThread mainThread) {
        LoadedApk packageInfo = new LoadedApk(mainThread);
        ContextImpl context = new ContextImpl(null, mainThread, packageInfo, null, null, false, null, null);
        context.mResources.updateConfiguration(context.mResourcesManager.getConfiguration(), context.mResourcesManager.getDisplayMetricsLocked(0));
        return context;
    }

    static ContextImpl createAppContext(ActivityThread mainThread, LoadedApk packageInfo) {
        if (packageInfo == null) {
            throw new IllegalArgumentException("packageInfo");
        }
        return new ContextImpl(null, mainThread, packageInfo, null, null, false, null, null);
    }

    static ContextImpl createActivityContext(ActivityThread mainThread, LoadedApk packageInfo, IBinder activityToken) {
        if (packageInfo == null) {
            throw new IllegalArgumentException("packageInfo");
        }
        if (activityToken == null) {
            throw new IllegalArgumentException("activityInfo");
        }
        return new ContextImpl(null, mainThread, packageInfo, activityToken, null, false, null, null);
    }

    private ContextImpl(ContextImpl container, ActivityThread mainThread, LoadedApk packageInfo, IBinder activityToken, UserHandle user, boolean restricted, Display display, Configuration overrideConfiguration) {
        this.mMainThread = mainThread;
        this.mActivityToken = activityToken;
        this.mRestricted = restricted;
        user = user == null ? Process.myUserHandle() : user;
        this.mUser = user;
        this.mPackageInfo = packageInfo;
        this.mResourcesManager = ResourcesManager.getInstance();
        this.mDisplay = display;
        this.mOverrideConfiguration = overrideConfiguration;
        int displayId = getDisplayId();
        CompatibilityInfo compatInfo = container != null ? container.getDisplayAdjustments(displayId).getCompatibilityInfo() : null;
        if (compatInfo == null && displayId == 0) {
            compatInfo = packageInfo.getCompatibilityInfo();
        }
        this.mDisplayAdjustments.setCompatibilityInfo(compatInfo);
        this.mDisplayAdjustments.setActivityToken(activityToken);
        Resources resources = packageInfo.getResources(mainThread);
        if (resources != null && (activityToken != null || displayId != 0 || overrideConfiguration != null || (compatInfo != null && compatInfo.applicationScale != resources.getCompatibilityInfo().applicationScale))) {
            resources = this.mResourcesManager.getTopLevelResources(packageInfo.getResDir(), packageInfo.getSplitResDirs(), packageInfo.getOverlayDirs(), packageInfo.getApplicationInfo().sharedLibraryFiles, displayId, overrideConfiguration, compatInfo, activityToken);
        }
        this.mResources = resources;
        if (container != null) {
            this.mBasePackageName = container.mBasePackageName;
            this.mOpPackageName = container.mOpPackageName;
        } else {
            this.mBasePackageName = packageInfo.mPackageName;
            ApplicationInfo ainfo = packageInfo.getApplicationInfo();
            if (ainfo.uid == 1000 && ainfo.uid != Process.myUid()) {
                this.mOpPackageName = ActivityThread.currentPackageName();
            } else {
                this.mOpPackageName = this.mBasePackageName;
            }
        }
        this.mContentResolver = new ApplicationContentResolver(this, mainThread, user);
    }

    void installSystemApplicationInfo(ApplicationInfo info, ClassLoader classLoader) {
        this.mPackageInfo.installSystemApplicationInfo(info, classLoader);
    }

    final void scheduleFinalCleanup(String who, String what) {
        this.mMainThread.scheduleContextCleanup(this, who, what);
    }

    final void performFinalCleanup(String who, String what) {
        this.mPackageInfo.removeContextRegistrations(getOuterContext(), who, what);
    }

    final Context getReceiverRestrictedContext() {
        if (this.mReceiverRestrictedContext != null) {
            return this.mReceiverRestrictedContext;
        }
        ReceiverRestrictedContext receiverRestrictedContext = new ReceiverRestrictedContext(getOuterContext());
        this.mReceiverRestrictedContext = receiverRestrictedContext;
        return receiverRestrictedContext;
    }

    final void setOuterContext(Context context) {
        this.mOuterContext = context;
    }

    final Context getOuterContext() {
        return this.mOuterContext;
    }

    final IBinder getActivityToken() {
        return this.mActivityToken;
    }

    static void setFilePermissionsFromMode(String name, int mode, int extraPermissions) {
        int perms = extraPermissions | 432;
        if ((mode & 1) != 0) {
            perms |= 4;
        }
        if ((mode & 2) != 0) {
            perms |= 2;
        }
        FileUtils.setPermissions(name, perms, -1, -1);
    }

    private File validateFilePath(String name, boolean createDirectory) {
        File dir;
        File f;
        if (name.charAt(0) == File.separatorChar) {
            String dirPath = name.substring(0, name.lastIndexOf(File.separatorChar));
            dir = new File(dirPath);
            f = new File(dir, name.substring(name.lastIndexOf(File.separatorChar)));
        } else {
            dir = getDatabasesDir();
            f = makeFilename(dir, name);
        }
        if (createDirectory && !dir.isDirectory() && dir.mkdir()) {
            FileUtils.setPermissions(dir.getPath(), 505, -1, -1);
        }
        return f;
    }

    private File makeFilename(File base, String name) {
        if (name.indexOf(File.separatorChar) < 0) {
            return new File(base, name);
        }
        throw new IllegalArgumentException("File " + name + " contains a path separator");
    }

    private File[] ensureDirsExistOrFilter(File[] dirs) {
        File[] result = new File[dirs.length];
        for (int i = 0; i < dirs.length; i++) {
            File dir = dirs[i];
            if (!dir.exists() && !dir.mkdirs() && !dir.exists()) {
                IMountService mount = IMountService.Stub.asInterface(ServiceManager.getService("mount"));
                int res = -1;
                try {
                    res = mount.mkdirs(getPackageName(), dir.getAbsolutePath());
                } catch (Exception e) {
                }
                if (res != 0) {
                    Log.w(TAG, "Failed to ensure directory: " + dir);
                    dir = null;
                }
            }
            result[i] = dir;
        }
        return result;
    }

    private static final class ApplicationContentResolver extends ContentResolver {
        private final ActivityThread mMainThread;
        private final UserHandle mUser;

        public ApplicationContentResolver(Context context, ActivityThread mainThread, UserHandle user) {
            super(context);
            this.mMainThread = (ActivityThread) Preconditions.checkNotNull(mainThread);
            this.mUser = (UserHandle) Preconditions.checkNotNull(user);
        }

        @Override
        protected IContentProvider acquireProvider(Context context, String auth) {
            return this.mMainThread.acquireProvider(context, ContentProvider.getAuthorityWithoutUserId(auth), resolveUserIdFromAuthority(auth), true);
        }

        @Override
        protected IContentProvider acquireExistingProvider(Context context, String auth) {
            return this.mMainThread.acquireExistingProvider(context, ContentProvider.getAuthorityWithoutUserId(auth), resolveUserIdFromAuthority(auth), true);
        }

        @Override
        public boolean releaseProvider(IContentProvider provider) {
            return this.mMainThread.releaseProvider(provider, true);
        }

        @Override
        protected IContentProvider acquireUnstableProvider(Context c, String auth) {
            return this.mMainThread.acquireProvider(c, ContentProvider.getAuthorityWithoutUserId(auth), resolveUserIdFromAuthority(auth), false);
        }

        @Override
        public boolean releaseUnstableProvider(IContentProvider icp) {
            return this.mMainThread.releaseProvider(icp, false);
        }

        @Override
        public void unstableProviderDied(IContentProvider icp) {
            this.mMainThread.handleUnstableProviderDied(icp.asBinder(), true);
        }

        @Override
        public void appNotRespondingViaProvider(IContentProvider icp) {
            this.mMainThread.appNotRespondingViaProvider(icp.asBinder());
        }

        protected int resolveUserIdFromAuthority(String auth) {
            return ContentProvider.getUserIdFromAuthority(auth, this.mUser.getIdentifier());
        }
    }
}
