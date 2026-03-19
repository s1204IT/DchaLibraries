package com.android.server;

import android.R;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.location.ActivityRecognitionHardware;
import android.location.Address;
import android.location.Criteria;
import android.location.GeocoderParams;
import android.location.Geofence;
import android.location.IBatchedLocationCallback;
import android.location.IGnssMeasurementsListener;
import android.location.IGnssNavigationMessageListener;
import android.location.IGnssStatusListener;
import android.location.IGnssStatusProvider;
import android.location.IGpsGeofenceHardware;
import android.location.ILocationListener;
import android.location.ILocationManager;
import android.location.INetInitiatedListener;
import android.location.Location;
import android.location.LocationProvider;
import android.location.LocationRequest;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.WorkSource;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import com.android.internal.content.PackageMonitor;
import com.android.internal.location.ProviderProperties;
import com.android.internal.location.ProviderRequest;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.hdmi.HdmiCecKeycode;
import com.android.server.location.ActivityRecognitionProxy;
import com.android.server.location.GeocoderProxy;
import com.android.server.location.GeofenceManager;
import com.android.server.location.GeofenceProxy;
import com.android.server.location.GnssBatchingProvider;
import com.android.server.location.GnssLocationProvider;
import com.android.server.location.GnssMeasurementsProvider;
import com.android.server.location.GnssNavigationMessageProvider;
import com.android.server.location.LocationBlacklist;
import com.android.server.location.LocationFudger;
import com.android.server.location.LocationProviderInterface;
import com.android.server.location.LocationProviderProxy;
import com.android.server.location.LocationRequestStatistics;
import com.android.server.location.MockProvider;
import com.android.server.location.PassiveProvider;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class LocationManagerService extends ILocationManager.Stub {
    private static final String ACCESS_LOCATION_EXTRA_COMMANDS = "android.permission.ACCESS_LOCATION_EXTRA_COMMANDS";
    private static final String ACCESS_MOCK_LOCATION = "android.permission.ACCESS_MOCK_LOCATION";
    public static final boolean D;
    private static final long DEFAULT_BACKGROUND_THROTTLE_INTERVAL_MS = 1800000;
    private static final LocationRequest DEFAULT_LOCATION_REQUEST;
    public static final boolean FORCE_DEBUG;
    private static final int FOREGROUND_IMPORTANCE_CUTOFF = 125;
    private static final String FUSED_LOCATION_SERVICE_ACTION = "com.android.location.service.FusedLocationProvider";
    private static final long HIGH_POWER_INTERVAL_MS = 300000;
    private static final String INSTALL_LOCATION_PROVIDER = "android.permission.INSTALL_LOCATION_PROVIDER";
    private static final boolean IS_USER_BUILD;
    private static final int MAX_PROVIDER_SCHEDULING_JITTER_MS = 100;
    private static final int MSG_LOCATION_CHANGED = 1;
    private static final long NANOS_PER_MILLI = 1000000;
    private static final String NETWORK_LOCATION_SERVICE_ACTION = "com.android.location.service.v3.NetworkLocationProvider";
    private static final String PROP_FORCE_DEBUG_KEY = "persist.vendor.log.tel_dbg";
    private static final int RESOLUTION_LEVEL_COARSE = 1;
    private static final int RESOLUTION_LEVEL_FINE = 2;
    private static final int RESOLUTION_LEVEL_NONE = 0;
    private static final String TAG = "LocationManagerService";
    private static final String WAKELOCK_KEY = "*location*";
    private ActivityManager mActivityManager;
    private final AppOpsManager mAppOps;
    private LocationBlacklist mBlacklist;
    private final Context mContext;
    private GeocoderProxy mGeocodeProvider;
    private GeofenceManager mGeofenceManager;
    private IBatchedLocationCallback mGnssBatchingCallback;
    private LinkedCallback mGnssBatchingDeathCallback;
    private GnssBatchingProvider mGnssBatchingProvider;
    private GnssMeasurementsProvider mGnssMeasurementsProvider;
    private GnssLocationProvider.GnssMetricsProvider mGnssMetricsProvider;
    private GnssNavigationMessageProvider mGnssNavigationMessageProvider;
    private IGnssStatusProvider mGnssStatusProvider;
    private GnssLocationProvider.GnssSystemInfoProvider mGnssSystemInfoProvider;
    private IGpsGeofenceHardware mGpsGeofenceProxy;
    private HandlerThread mHandlerThread;
    private LocationFudger mLocationFudger;
    private LocationWorkerHandler mLocationHandler;
    private INetInitiatedListener mNetInitiatedListener;
    private PackageManager mPackageManager;
    private PassiveProvider mPassiveProvider;
    private PowerManager mPowerManager;
    private UserManager mUserManager;
    private final Object mLock = new Object();
    private final Set<String> mEnabledProviders = new HashSet();
    private final Set<String> mDisabledProviders = new HashSet();
    private final HashMap<String, MockProvider> mMockProviders = new HashMap<>();
    private final HashMap<Object, Receiver> mReceivers = new HashMap<>();
    private final ArrayList<LocationProviderInterface> mProviders = new ArrayList<>();
    private final HashMap<String, LocationProviderInterface> mRealProviders = new HashMap<>();
    private final HashMap<String, LocationProviderInterface> mProvidersByName = new HashMap<>();
    private final HashMap<String, ArrayList<UpdateRecord>> mRecordsByProvider = new HashMap<>();
    private final LocationRequestStatistics mRequestStatistics = new LocationRequestStatistics();
    private final HashMap<String, Location> mLastLocation = new HashMap<>();
    private final HashMap<String, Location> mLastLocationCoarseInterval = new HashMap<>();
    private final ArrayList<LocationProviderProxy> mProxyProviders = new ArrayList<>();
    private final ArraySet<String> mBackgroundThrottlePackageWhitelist = new ArraySet<>();
    private final ArrayMap<IBinder, Identity> mGnssMeasurementsListeners = new ArrayMap<>();
    private final ArrayMap<IBinder, Identity> mGnssNavigationMessageListeners = new ArrayMap<>();
    private int mCurrentUserId = 0;
    private int[] mCurrentUserProfiles = {0};
    private boolean mGnssBatchingInProgress = false;
    private final PackageMonitor mPackageMonitor = new PackageMonitor() {
        public void onPackageDisappeared(String str, int i) {
            synchronized (LocationManagerService.this.mLock) {
                ArrayList arrayList = null;
                for (Receiver receiver : LocationManagerService.this.mReceivers.values()) {
                    if (receiver.mIdentity.mPackageName.equals(str)) {
                        if (arrayList == null) {
                            arrayList = new ArrayList();
                        }
                        arrayList.add(receiver);
                    }
                }
                if (arrayList != null) {
                    Iterator it = arrayList.iterator();
                    while (it.hasNext()) {
                        LocationManagerService.this.removeUpdatesLocked((Receiver) it.next());
                    }
                }
            }
        }
    };
    private Class<?> mMtkLocationManagerServiceClass = null;
    private Object mMtkLocationManagerService = null;
    private ArrayList<String> mWhitelistPackages = new ArrayList<>();
    private final HashMap<String, Boolean> mWhitelistProviders = new HashMap<>();
    private final String ELS_PACKAGE_NAME = "com.google.android.gms";
    private boolean mCtaSupported = false;

    static {
        boolean z = true;
        IS_USER_BUILD = "user".equals(Build.TYPE) || "userdebug".equals(Build.TYPE);
        FORCE_DEBUG = SystemProperties.getInt(PROP_FORCE_DEBUG_KEY, 0) == 1;
        if (IS_USER_BUILD && !Log.isLoggable(TAG, 3) && !FORCE_DEBUG) {
            z = false;
        }
        D = z;
        DEFAULT_LOCATION_REQUEST = new LocationRequest();
    }

    public LocationManagerService(Context context) {
        this.mContext = context;
        this.mAppOps = (AppOpsManager) context.getSystemService("appops");
        ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).setLocationPackagesProvider(new PackageManagerInternal.PackagesProvider() {
            public String[] getPackages(int i) {
                return LocationManagerService.this.mContext.getResources().getStringArray(R.array.config_cameraPrivacyLightAlsLuxThresholds);
            }
        });
        if (D) {
            Log.d(TAG, "Constructed");
        }
    }

    public void systemRunning() {
        synchronized (this.mLock) {
            if (D) {
                Log.d(TAG, "systemRunning()");
            }
            this.mPackageManager = this.mContext.getPackageManager();
            this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
            this.mActivityManager = (ActivityManager) this.mContext.getSystemService("activity");
            this.mLocationHandler = new LocationWorkerHandler(BackgroundThread.get().getLooper());
            initMtkLocationManagerService();
            this.mLocationFudger = new LocationFudger(this.mContext, this.mLocationHandler);
            this.mBlacklist = new LocationBlacklist(this.mContext, this.mLocationHandler);
            this.mBlacklist.init();
            this.mGeofenceManager = new GeofenceManager(this.mContext, this.mBlacklist);
            this.mAppOps.startWatchingMode(0, (String) null, new AppOpsManager.OnOpChangedInternalListener() {
                public void onOpChanged(int i, String str) {
                    synchronized (LocationManagerService.this.mLock) {
                        Iterator it = LocationManagerService.this.mReceivers.values().iterator();
                        while (it.hasNext()) {
                            ((Receiver) it.next()).updateMonitoring(true);
                        }
                        LocationManagerService.this.applyAllProviderRequirementsLocked();
                    }
                }
            });
            this.mPackageManager.addOnPermissionsChangeListener(new PackageManager.OnPermissionsChangedListener() {
                public void onPermissionsChanged(int i) {
                    synchronized (LocationManagerService.this.mLock) {
                        LocationManagerService.this.applyAllProviderRequirementsLocked();
                    }
                }
            });
            this.mActivityManager.addOnUidImportanceListener(new ActivityManager.OnUidImportanceListener() {
                public void onUidImportance(final int i, final int i2) {
                    LocationManagerService.this.mLocationHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            LocationManagerService.this.onUidImportanceChanged(i, i2);
                        }
                    });
                }
            }, FOREGROUND_IMPORTANCE_CUTOFF);
            this.mUserManager = (UserManager) this.mContext.getSystemService("user");
            updateUserProfiles(this.mCurrentUserId);
            updateBackgroundThrottlingWhitelistLocked();
            loadProvidersLocked();
            updateProvidersLocked();
        }
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("location_providers_allowed"), true, new ContentObserver(this.mLocationHandler) {
            @Override
            public void onChange(boolean z) {
                synchronized (LocationManagerService.this.mLock) {
                    LocationManagerService.this.updateProvidersLocked();
                }
            }
        }, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("location_background_throttle_interval_ms"), true, new ContentObserver(this.mLocationHandler) {
            @Override
            public void onChange(boolean z) {
                synchronized (LocationManagerService.this.mLock) {
                    LocationManagerService.this.updateProvidersLocked();
                }
            }
        }, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("location_background_throttle_package_whitelist"), true, new ContentObserver(this.mLocationHandler) {
            @Override
            public void onChange(boolean z) {
                synchronized (LocationManagerService.this.mLock) {
                    LocationManagerService.this.updateBackgroundThrottlingWhitelistLocked();
                    LocationManagerService.this.updateProvidersLocked();
                }
            }
        }, -1);
        this.mPackageMonitor.register(this.mContext, this.mLocationHandler.getLooper(), true);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        intentFilter.addAction("android.intent.action.MANAGED_PROFILE_ADDED");
        intentFilter.addAction("android.intent.action.MANAGED_PROFILE_REMOVED");
        intentFilter.addAction("android.intent.action.ACTION_SHUTDOWN");
        this.mContext.registerReceiverAsUser(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("android.intent.action.USER_SWITCHED".equals(action)) {
                    LocationManagerService.this.switchUser(intent.getIntExtra("android.intent.extra.user_handle", 0));
                    return;
                }
                if ("android.intent.action.MANAGED_PROFILE_ADDED".equals(action) || "android.intent.action.MANAGED_PROFILE_REMOVED".equals(action)) {
                    LocationManagerService.this.updateUserProfiles(LocationManagerService.this.mCurrentUserId);
                    return;
                }
                if ("android.intent.action.ACTION_SHUTDOWN".equals(action)) {
                    if (LocationManagerService.D) {
                        Log.d(LocationManagerService.TAG, "Shutdown received with UserId: " + getSendingUserId());
                    }
                    if (getSendingUserId() == -1) {
                        LocationManagerService.this.shutdownComponents();
                    }
                }
            }
        }, UserHandle.ALL, intentFilter, null, this.mLocationHandler);
    }

    private void onUidImportanceChanged(int i, int i2) {
        boolean zIsImportanceForeground = isImportanceForeground(i2);
        HashSet hashSet = new HashSet(this.mRecordsByProvider.size());
        synchronized (this.mLock) {
            for (Map.Entry<String, ArrayList<UpdateRecord>> entry : this.mRecordsByProvider.entrySet()) {
                String key = entry.getKey();
                for (UpdateRecord updateRecord : entry.getValue()) {
                    if (updateRecord.mReceiver.mIdentity.mUid == i && updateRecord.mIsForegroundUid != zIsImportanceForeground) {
                        if (D) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("request from uid ");
                            sb.append(i);
                            sb.append(" is now ");
                            sb.append(zIsImportanceForeground ? "foreground" : "background)");
                            Log.d(TAG, sb.toString());
                        }
                        updateRecord.updateForeground(zIsImportanceForeground);
                        if (!isThrottlingExemptLocked(updateRecord.mReceiver.mIdentity)) {
                            hashSet.add(key);
                        }
                    }
                }
            }
            Iterator it = hashSet.iterator();
            while (it.hasNext()) {
                applyRequirementsLocked((String) it.next());
            }
            for (Map.Entry<IBinder, Identity> entry2 : this.mGnssMeasurementsListeners.entrySet()) {
                if (entry2.getValue().mUid == i) {
                    if (D) {
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append("gnss measurements listener from uid ");
                        sb2.append(i);
                        sb2.append(" is now ");
                        sb2.append(zIsImportanceForeground ? "foreground" : "background)");
                        Log.d(TAG, sb2.toString());
                    }
                    if (zIsImportanceForeground || isThrottlingExemptLocked(entry2.getValue())) {
                        this.mGnssMeasurementsProvider.addListener(IGnssMeasurementsListener.Stub.asInterface(entry2.getKey()));
                    } else {
                        this.mGnssMeasurementsProvider.removeListener(IGnssMeasurementsListener.Stub.asInterface(entry2.getKey()));
                    }
                }
            }
            for (Map.Entry<IBinder, Identity> entry3 : this.mGnssNavigationMessageListeners.entrySet()) {
                if (entry3.getValue().mUid == i) {
                    if (D) {
                        StringBuilder sb3 = new StringBuilder();
                        sb3.append("gnss navigation message listener from uid ");
                        sb3.append(i);
                        sb3.append(" is now ");
                        sb3.append(zIsImportanceForeground ? "foreground" : "background)");
                        Log.d(TAG, sb3.toString());
                    }
                    if (zIsImportanceForeground || isThrottlingExemptLocked(entry3.getValue())) {
                        this.mGnssNavigationMessageProvider.addListener(IGnssNavigationMessageListener.Stub.asInterface(entry3.getKey()));
                    } else {
                        this.mGnssNavigationMessageProvider.removeListener(IGnssNavigationMessageListener.Stub.asInterface(entry3.getKey()));
                    }
                }
            }
        }
    }

    private static boolean isImportanceForeground(int i) {
        return i <= FOREGROUND_IMPORTANCE_CUTOFF;
    }

    private void shutdownComponents() {
        if (D) {
            Log.d(TAG, "Shutting down components...");
        }
        LocationProviderInterface locationProviderInterface = this.mProvidersByName.get("gps");
        if (locationProviderInterface != null && locationProviderInterface.isEnabled()) {
            locationProviderInterface.disable();
        }
    }

    void updateUserProfiles(int i) {
        int[] profileIdsWithDisabled = this.mUserManager.getProfileIdsWithDisabled(i);
        synchronized (this.mLock) {
            this.mCurrentUserProfiles = profileIdsWithDisabled;
        }
    }

    private boolean isCurrentProfile(int i) {
        boolean zContains;
        synchronized (this.mLock) {
            zContains = ArrayUtils.contains(this.mCurrentUserProfiles, i);
        }
        return zContains;
    }

    private void ensureFallbackFusedProviderPresentLocked(ArrayList<String> arrayList) {
        PackageManager packageManager = this.mContext.getPackageManager();
        String packageName = this.mContext.getPackageName();
        ArrayList<HashSet<Signature>> signatureSets = ServiceWatcher.getSignatureSets(this.mContext, arrayList);
        for (ResolveInfo resolveInfo : packageManager.queryIntentServicesAsUser(new Intent(FUSED_LOCATION_SERVICE_ACTION), 128, this.mCurrentUserId)) {
            String str = resolveInfo.serviceInfo.packageName;
            try {
                if (!ServiceWatcher.isSignatureMatch(packageManager.getPackageInfo(str, 64).signatures, signatureSets)) {
                    Log.w(TAG, str + " resolves service " + FUSED_LOCATION_SERVICE_ACTION + ", but has wrong signature, ignoring");
                } else if (resolveInfo.serviceInfo.metaData == null) {
                    Log.w(TAG, "Found fused provider without metadata: " + str);
                } else if (resolveInfo.serviceInfo.metaData.getInt(ServiceWatcher.EXTRA_SERVICE_VERSION, -1) == 0) {
                    if ((resolveInfo.serviceInfo.applicationInfo.flags & 1) == 0) {
                        if (D) {
                            Log.d(TAG, "Fallback candidate not in /system: " + str);
                        }
                    } else if (packageManager.checkSignatures(packageName, str) != 0) {
                        if (D) {
                            Log.d(TAG, "Fallback candidate not signed the same as system: " + str);
                        }
                    } else {
                        if (D) {
                            Log.d(TAG, "Found fallback provider: " + str);
                            return;
                        }
                        return;
                    }
                } else if (D) {
                    Log.d(TAG, "Fallback candidate not version 0: " + str);
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "missing package: " + str);
            }
        }
        throw new IllegalStateException("Unable to find a fused location provider that is in the system partition with version 0 and signed with the platform certificate. Such a package is needed to provide a default fused location provider in the event that no other fused location provider has been installed or is currently available. For example, coreOnly boot mode when decrypting the data partition. The fallback must also be marked coreApp=\"true\" in the manifest");
    }

    private void loadProvidersLocked() {
        PassiveProvider passiveProvider = new PassiveProvider(this);
        addProviderLocked(passiveProvider);
        this.mEnabledProviders.add(passiveProvider.getName());
        this.mPassiveProvider = passiveProvider;
        if (GnssLocationProvider.isSupported()) {
            GnssLocationProvider gnssLocationProvider = new GnssLocationProvider(this.mContext, this, this.mLocationHandler.getLooper());
            this.mGnssSystemInfoProvider = gnssLocationProvider.getGnssSystemInfoProvider();
            this.mGnssBatchingProvider = gnssLocationProvider.getGnssBatchingProvider();
            this.mGnssMetricsProvider = gnssLocationProvider.getGnssMetricsProvider();
            this.mGnssStatusProvider = gnssLocationProvider.getGnssStatusProvider();
            this.mNetInitiatedListener = gnssLocationProvider.getNetInitiatedListener();
            addProviderLocked(gnssLocationProvider);
            this.mRealProviders.put("gps", gnssLocationProvider);
            this.mGnssMeasurementsProvider = gnssLocationProvider.getGnssMeasurementsProvider();
            this.mGnssNavigationMessageProvider = gnssLocationProvider.getGnssNavigationMessageProvider();
            this.mGpsGeofenceProxy = gnssLocationProvider.getGpsGeofenceProxy();
        }
        Resources resources = this.mContext.getResources();
        ArrayList<String> arrayList = new ArrayList<>();
        String[] stringArray = resources.getStringArray(R.array.config_cameraPrivacyLightAlsLuxThresholds);
        if (D) {
            Log.d(TAG, "certificates for location providers pulled from: " + Arrays.toString(stringArray));
        }
        if (stringArray != null) {
            arrayList.addAll(Arrays.asList(stringArray));
        }
        ensureFallbackFusedProviderPresentLocked(arrayList);
        LocationProviderProxy locationProviderProxyCreateAndBind = LocationProviderProxy.createAndBind(this.mContext, "network", NETWORK_LOCATION_SERVICE_ACTION, R.^attr-private.glyphMap, R.string.android_upgrading_starting_apps, R.array.config_cameraPrivacyLightAlsLuxThresholds, this.mLocationHandler);
        if (locationProviderProxyCreateAndBind != null) {
            this.mRealProviders.put("network", locationProviderProxyCreateAndBind);
            this.mProxyProviders.add(locationProviderProxyCreateAndBind);
            addProviderLocked(locationProviderProxyCreateAndBind);
        } else {
            Slog.w(TAG, "no network location provider found");
        }
        LocationProviderProxy locationProviderProxyCreateAndBind2 = LocationProviderProxy.createAndBind(this.mContext, "fused", FUSED_LOCATION_SERVICE_ACTION, R.^attr-private.frameDuration, R.string.aerr_close, R.array.config_cameraPrivacyLightAlsLuxThresholds, this.mLocationHandler);
        if (locationProviderProxyCreateAndBind2 != null) {
            addProviderLocked(locationProviderProxyCreateAndBind2);
            this.mProxyProviders.add(locationProviderProxyCreateAndBind2);
            this.mEnabledProviders.add(locationProviderProxyCreateAndBind2.getName());
            this.mRealProviders.put("fused", locationProviderProxyCreateAndBind2);
        } else {
            Slog.e(TAG, "no fused location provider found", new IllegalStateException("Location service needs a fused location provider"));
        }
        this.mGeocodeProvider = GeocoderProxy.createAndBind(this.mContext, R.^attr-private.framesCount, R.string.aerr_close_app, R.array.config_cameraPrivacyLightAlsLuxThresholds, this.mLocationHandler);
        if (this.mGeocodeProvider == null) {
            Slog.e(TAG, "no geocoder provider found");
        }
        if (GeofenceProxy.createAndBind(this.mContext, R.^attr-private.fromBottom, R.string.aerr_mute, R.array.config_cameraPrivacyLightAlsLuxThresholds, this.mLocationHandler, this.mGpsGeofenceProxy, null) == null) {
            Slog.d(TAG, "Unable to bind FLP Geofence proxy.");
        }
        boolean zIsSupported = ActivityRecognitionHardware.isSupported();
        ActivityRecognitionHardware activityRecognitionHardware = null;
        if (zIsSupported) {
            activityRecognitionHardware = ActivityRecognitionHardware.getInstance(this.mContext);
        } else {
            Slog.d(TAG, "Hardware Activity-Recognition not supported.");
        }
        if (ActivityRecognitionProxy.createAndBind(this.mContext, this.mLocationHandler, zIsSupported, activityRecognitionHardware, R.^attr-private.floatingToolbarDividerColor, R.string.accessibility_system_action_dpad_center_label, R.array.config_cameraPrivacyLightAlsLuxThresholds) == null) {
            Slog.d(TAG, "Unable to bind ActivityRecognitionProxy.");
        }
        for (String str : resources.getStringArray(R.array.config_deviceStatesToReverseDefaultDisplayRotationAroundZAxis)) {
            String[] strArrSplit = str.split(",");
            String strTrim = strArrSplit[0].trim();
            if (this.mProvidersByName.get(strTrim) != null) {
                throw new IllegalArgumentException("Provider \"" + strTrim + "\" already exists");
            }
            addTestProviderLocked(strTrim, new ProviderProperties(Boolean.parseBoolean(strArrSplit[1]), Boolean.parseBoolean(strArrSplit[2]), Boolean.parseBoolean(strArrSplit[3]), Boolean.parseBoolean(strArrSplit[4]), Boolean.parseBoolean(strArrSplit[5]), Boolean.parseBoolean(strArrSplit[6]), Boolean.parseBoolean(strArrSplit[7]), Integer.parseInt(strArrSplit[8]), Integer.parseInt(strArrSplit[9])));
        }
    }

    private void switchUser(int i) {
        if (this.mCurrentUserId == i) {
            return;
        }
        this.mBlacklist.switchUser(i);
        this.mLocationHandler.removeMessages(1);
        synchronized (this.mLock) {
            this.mLastLocation.clear();
            this.mLastLocationCoarseInterval.clear();
            Iterator<LocationProviderInterface> it = this.mProviders.iterator();
            while (it.hasNext()) {
                updateProviderListenersLocked(it.next().getName(), false);
            }
            this.mCurrentUserId = i;
            updateUserProfiles(i);
            updateProvidersLocked();
        }
    }

    private static final class Identity {
        final String mPackageName;
        final int mPid;
        final int mUid;

        Identity(int i, int i2, String str) {
            this.mUid = i;
            this.mPid = i2;
            this.mPackageName = str;
        }
    }

    private final class Receiver implements IBinder.DeathRecipient, PendingIntent.OnFinished {
        final int mAllowedResolutionLevel;
        final boolean mHideFromAppOps;
        final Identity mIdentity;
        final Object mKey;
        final ILocationListener mListener;
        boolean mOpHighPowerMonitoring;
        boolean mOpMonitoring;
        int mPendingBroadcasts;
        final PendingIntent mPendingIntent;
        final HashMap<String, UpdateRecord> mUpdateRecords = new HashMap<>();
        PowerManager.WakeLock mWakeLock;
        final WorkSource mWorkSource;

        Receiver(ILocationListener iLocationListener, PendingIntent pendingIntent, int i, int i2, String str, WorkSource workSource, boolean z) {
            this.mListener = iLocationListener;
            this.mPendingIntent = pendingIntent;
            if (iLocationListener != null) {
                this.mKey = iLocationListener.asBinder();
            } else {
                this.mKey = pendingIntent;
            }
            this.mAllowedResolutionLevel = LocationManagerService.this.getAllowedResolutionLevel(i, i2);
            this.mIdentity = new Identity(i2, i, str);
            if (workSource != null && workSource.isEmpty()) {
                workSource = null;
            }
            this.mWorkSource = workSource;
            this.mHideFromAppOps = z;
            updateMonitoring(true);
            this.mWakeLock = LocationManagerService.this.mPowerManager.newWakeLock(1, LocationManagerService.WAKELOCK_KEY);
            this.mWakeLock.setWorkSource(workSource == null ? new WorkSource(this.mIdentity.mUid, this.mIdentity.mPackageName) : workSource);
        }

        public boolean equals(Object obj) {
            return (obj instanceof Receiver) && this.mKey.equals(((Receiver) obj).mKey);
        }

        public int hashCode() {
            return this.mKey.hashCode();
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Reciever[");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            if (this.mListener != null) {
                sb.append(" listener");
            } else {
                sb.append(" intent");
            }
            for (String str : this.mUpdateRecords.keySet()) {
                sb.append(" ");
                sb.append(this.mUpdateRecords.get(str).toString());
            }
            sb.append("]");
            return sb.toString();
        }

        public void updateMonitoring(boolean z) {
            if (this.mHideFromAppOps) {
                return;
            }
            boolean z2 = true;
            boolean z3 = false;
            if (z) {
                Iterator<UpdateRecord> it = this.mUpdateRecords.values().iterator();
                boolean z4 = false;
                while (true) {
                    if (it.hasNext()) {
                        UpdateRecord next = it.next();
                        if (LocationManagerService.this.isAllowedByCurrentUserSettingsLocked(next.mProvider)) {
                            LocationProviderInterface locationProviderInterface = (LocationProviderInterface) LocationManagerService.this.mProvidersByName.get(next.mProvider);
                            ProviderProperties properties = locationProviderInterface != null ? locationProviderInterface.getProperties() : null;
                            if (properties == null || properties.mPowerRequirement != 3 || next.mRequest.getInterval() >= 300000) {
                                z4 = true;
                            } else {
                                z3 = true;
                                break;
                            }
                        }
                    } else {
                        z2 = z4;
                        break;
                    }
                }
            } else {
                z2 = false;
            }
            this.mOpMonitoring = updateMonitoring(z2, this.mOpMonitoring, 41);
            boolean z5 = this.mOpHighPowerMonitoring;
            this.mOpHighPowerMonitoring = updateMonitoring(z3, this.mOpHighPowerMonitoring, 42);
            if (this.mOpHighPowerMonitoring != z5) {
                LocationManagerService.this.mContext.sendBroadcastAsUser(new Intent("android.location.HIGH_POWER_REQUEST_CHANGE"), UserHandle.ALL);
            }
        }

        private boolean updateMonitoring(boolean z, boolean z2, int i) {
            if (!z2) {
                if (z) {
                    return LocationManagerService.this.mAppOps.startOpNoThrow(i, this.mIdentity.mUid, this.mIdentity.mPackageName) == 0;
                }
            } else if (!z || LocationManagerService.this.mAppOps.checkOpNoThrow(i, this.mIdentity.mUid, this.mIdentity.mPackageName) != 0) {
                LocationManagerService.this.mAppOps.finishOp(i, this.mIdentity.mUid, this.mIdentity.mPackageName);
                return false;
            }
            return z2;
        }

        public boolean isListener() {
            return this.mListener != null;
        }

        public boolean isPendingIntent() {
            return this.mPendingIntent != null;
        }

        public ILocationListener getListener() {
            if (this.mListener != null) {
                return this.mListener;
            }
            throw new IllegalStateException("Request for non-existent listener");
        }

        public boolean callStatusChangedLocked(String str, int i, Bundle bundle) {
            if (this.mListener != null) {
                try {
                    synchronized (this) {
                        this.mListener.onStatusChanged(str, i, bundle);
                        incrementPendingBroadcastsLocked();
                    }
                    return true;
                } catch (RemoteException e) {
                    return false;
                }
            }
            Intent intent = new Intent();
            intent.putExtras(new Bundle(bundle));
            intent.putExtra("status", i);
            try {
                synchronized (this) {
                    this.mPendingIntent.send(LocationManagerService.this.mContext, 0, intent, this, LocationManagerService.this.mLocationHandler, LocationManagerService.this.getResolutionPermission(this.mAllowedResolutionLevel), PendingIntentUtils.createDontSendToRestrictedAppsBundle(null));
                    incrementPendingBroadcastsLocked();
                }
                return true;
            } catch (PendingIntent.CanceledException e2) {
                return false;
            }
        }

        public boolean callLocationChangedLocked(Location location) {
            if (this.mListener != null) {
                try {
                    synchronized (this) {
                        this.mListener.onLocationChanged(new Location(location));
                        incrementPendingBroadcastsLocked();
                    }
                    return true;
                } catch (RemoteException e) {
                    return false;
                }
            }
            Intent intent = new Intent();
            intent.putExtra("location", new Location(location));
            try {
                synchronized (this) {
                    this.mPendingIntent.send(LocationManagerService.this.mContext, 0, intent, this, LocationManagerService.this.mLocationHandler, LocationManagerService.this.getResolutionPermission(this.mAllowedResolutionLevel), PendingIntentUtils.createDontSendToRestrictedAppsBundle(null));
                    incrementPendingBroadcastsLocked();
                }
                return true;
            } catch (PendingIntent.CanceledException e2) {
                return false;
            }
        }

        public boolean callProviderEnabledLocked(String str, boolean z) {
            updateMonitoring(true);
            if (this.mListener != null) {
                try {
                    synchronized (this) {
                        try {
                            if (z) {
                                this.mListener.onProviderEnabled(str);
                            } else {
                                this.mListener.onProviderDisabled(str);
                            }
                            incrementPendingBroadcastsLocked();
                        } finally {
                        }
                    }
                } catch (RemoteException e) {
                    return false;
                }
            } else {
                Intent intent = new Intent();
                intent.putExtra("providerEnabled", z);
                try {
                    synchronized (this) {
                        this.mPendingIntent.send(LocationManagerService.this.mContext, 0, intent, this, LocationManagerService.this.mLocationHandler, LocationManagerService.this.getResolutionPermission(this.mAllowedResolutionLevel), PendingIntentUtils.createDontSendToRestrictedAppsBundle(null));
                        incrementPendingBroadcastsLocked();
                    }
                } catch (PendingIntent.CanceledException e2) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void binderDied() {
            if (LocationManagerService.D) {
                Log.d(LocationManagerService.TAG, "Location listener died");
            }
            synchronized (LocationManagerService.this.mLock) {
                LocationManagerService.this.removeUpdatesLocked(this);
            }
            synchronized (this) {
                clearPendingBroadcastsLocked();
            }
        }

        @Override
        public void onSendFinished(PendingIntent pendingIntent, Intent intent, int i, String str, Bundle bundle) {
            synchronized (this) {
                decrementPendingBroadcastsLocked();
            }
        }

        private void incrementPendingBroadcastsLocked() {
            int i = this.mPendingBroadcasts;
            this.mPendingBroadcasts = i + 1;
            if (i == 0) {
                this.mWakeLock.acquire();
            }
        }

        private void decrementPendingBroadcastsLocked() {
            int i = this.mPendingBroadcasts - 1;
            this.mPendingBroadcasts = i;
            if (i == 0 && this.mWakeLock.isHeld()) {
                this.mWakeLock.release();
            }
        }

        public void clearPendingBroadcastsLocked() {
            if (this.mPendingBroadcasts > 0) {
                this.mPendingBroadcasts = 0;
                if (this.mWakeLock.isHeld()) {
                    this.mWakeLock.release();
                }
            }
        }
    }

    public void locationCallbackFinished(ILocationListener iLocationListener) {
        synchronized (this.mLock) {
            Receiver receiver = this.mReceivers.get(iLocationListener.asBinder());
            if (receiver != null) {
                synchronized (receiver) {
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    receiver.decrementPendingBroadcastsLocked();
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }
    }

    public int getGnssYearOfHardware() {
        if (this.mGnssSystemInfoProvider != null) {
            return this.mGnssSystemInfoProvider.getGnssYearOfHardware();
        }
        return 0;
    }

    public String getGnssHardwareModelName() {
        if (this.mGnssSystemInfoProvider != null) {
            return this.mGnssSystemInfoProvider.getGnssHardwareModelName();
        }
        return null;
    }

    private boolean hasGnssPermissions(String str) {
        int callerAllowedResolutionLevel = getCallerAllowedResolutionLevel();
        checkResolutionLevelIsSufficientForProviderUse(callerAllowedResolutionLevel, "gps");
        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return checkLocationAccess(callingPid, callingUid, str, callerAllowedResolutionLevel);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public int getGnssBatchSize(String str) {
        this.mContext.enforceCallingPermission("android.permission.LOCATION_HARDWARE", "Location Hardware permission not granted to access hardware batching");
        if (hasGnssPermissions(str) && this.mGnssBatchingProvider != null) {
            return this.mGnssBatchingProvider.getBatchSize();
        }
        return 0;
    }

    public boolean addGnssBatchingCallback(IBatchedLocationCallback iBatchedLocationCallback, String str) {
        this.mContext.enforceCallingPermission("android.permission.LOCATION_HARDWARE", "Location Hardware permission not granted to access hardware batching");
        if (!hasGnssPermissions(str) || this.mGnssBatchingProvider == null) {
            return false;
        }
        this.mGnssBatchingCallback = iBatchedLocationCallback;
        this.mGnssBatchingDeathCallback = new LinkedCallback(iBatchedLocationCallback);
        try {
            iBatchedLocationCallback.asBinder().linkToDeath(this.mGnssBatchingDeathCallback, 0);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "Remote listener already died.", e);
            return false;
        }
    }

    private class LinkedCallback implements IBinder.DeathRecipient {
        private final IBatchedLocationCallback mCallback;

        public LinkedCallback(IBatchedLocationCallback iBatchedLocationCallback) {
            this.mCallback = iBatchedLocationCallback;
        }

        public IBatchedLocationCallback getUnderlyingListener() {
            return this.mCallback;
        }

        @Override
        public void binderDied() {
            Log.d(LocationManagerService.TAG, "Remote Batching Callback died: " + this.mCallback);
            LocationManagerService.this.stopGnssBatch();
            LocationManagerService.this.removeGnssBatchingCallback();
        }
    }

    public void removeGnssBatchingCallback() {
        try {
            this.mGnssBatchingCallback.asBinder().unlinkToDeath(this.mGnssBatchingDeathCallback, 0);
        } catch (NoSuchElementException e) {
            Log.e(TAG, "Couldn't unlink death callback.", e);
        }
        this.mGnssBatchingCallback = null;
        this.mGnssBatchingDeathCallback = null;
    }

    public boolean startGnssBatch(long j, boolean z, String str) {
        this.mContext.enforceCallingPermission("android.permission.LOCATION_HARDWARE", "Location Hardware permission not granted to access hardware batching");
        if (!hasGnssPermissions(str) || this.mGnssBatchingProvider == null) {
            return false;
        }
        if (this.mGnssBatchingInProgress) {
            Log.e(TAG, "startGnssBatch unexpectedly called w/o stopping prior batch");
            stopGnssBatch();
        }
        this.mGnssBatchingInProgress = true;
        return this.mGnssBatchingProvider.start(j, z);
    }

    public void flushGnssBatch(String str) {
        this.mContext.enforceCallingPermission("android.permission.LOCATION_HARDWARE", "Location Hardware permission not granted to access hardware batching");
        if (!hasGnssPermissions(str)) {
            Log.e(TAG, "flushGnssBatch called without GNSS permissions");
            return;
        }
        if (!this.mGnssBatchingInProgress) {
            Log.w(TAG, "flushGnssBatch called with no batch in progress");
        }
        if (this.mGnssBatchingProvider != null) {
            this.mGnssBatchingProvider.flush();
        }
    }

    public boolean stopGnssBatch() {
        this.mContext.enforceCallingPermission("android.permission.LOCATION_HARDWARE", "Location Hardware permission not granted to access hardware batching");
        if (this.mGnssBatchingProvider == null) {
            return false;
        }
        this.mGnssBatchingInProgress = false;
        return this.mGnssBatchingProvider.stop();
    }

    public void reportLocationBatch(List<Location> list) {
        checkCallerIsProvider();
        if (isAllowedByCurrentUserSettingsLocked("gps")) {
            if (this.mGnssBatchingCallback == null) {
                Slog.e(TAG, "reportLocationBatch() called without active Callback");
                return;
            }
            try {
                this.mGnssBatchingCallback.onLocationBatch(list);
                return;
            } catch (RemoteException e) {
                Slog.e(TAG, "mGnssBatchingCallback.onLocationBatch failed", e);
                return;
            }
        }
        Slog.w(TAG, "reportLocationBatch() called without user permission, locations blocked");
    }

    private void addProviderLocked(LocationProviderInterface locationProviderInterface) {
        this.mProviders.add(locationProviderInterface);
        this.mProvidersByName.put(locationProviderInterface.getName(), locationProviderInterface);
    }

    private void removeProviderLocked(LocationProviderInterface locationProviderInterface) {
        locationProviderInterface.disable();
        this.mProviders.remove(locationProviderInterface);
        this.mProvidersByName.remove(locationProviderInterface.getName());
    }

    private boolean isAllowedByCurrentUserSettingsLocked(String str) {
        return isAllowedByUserSettingsLockedForUser(str, this.mCurrentUserId);
    }

    private boolean isAllowedByUserSettingsLockedForUser(String str, int i) {
        if (this.mEnabledProviders.contains(str)) {
            return true;
        }
        if (this.mDisabledProviders.contains(str)) {
            return false;
        }
        return isLocationProviderEnabledForUser(str, i);
    }

    private boolean isAllowedByUserSettingsLocked(String str, int i, int i2) {
        if (!isCurrentProfile(UserHandle.getUserId(i)) && !isUidALocationProvider(i)) {
            return false;
        }
        return isAllowedByUserSettingsLockedForUser(str, i2);
    }

    private String getResolutionPermission(int i) {
        switch (i) {
            case 1:
                return "android.permission.ACCESS_COARSE_LOCATION";
            case 2:
                return "android.permission.ACCESS_FINE_LOCATION";
            default:
                return null;
        }
    }

    private int getAllowedResolutionLevel(int i, int i2) {
        if (this.mContext.checkPermission("android.permission.ACCESS_FINE_LOCATION", i, i2) == 0 && mtkCheckCtaOp(i, i2, 1)) {
            return 2;
        }
        return (this.mContext.checkPermission("android.permission.ACCESS_COARSE_LOCATION", i, i2) == 0 && mtkCheckCtaOp(i, i2, 0)) ? 1 : 0;
    }

    private int getCallerAllowedResolutionLevel() {
        return getAllowedResolutionLevel(Binder.getCallingPid(), Binder.getCallingUid());
    }

    private void checkResolutionLevelIsSufficientForGeofenceUse(int i) {
        if (i < 2) {
            throw new SecurityException("Geofence usage requires ACCESS_FINE_LOCATION permission");
        }
    }

    private int getMinimumResolutionLevelForProviderUse(String str) {
        ProviderProperties properties;
        if ("gps".equals(str) || "passive".equals(str)) {
            return 2;
        }
        if ("network".equals(str) || "fused".equals(str)) {
            return 1;
        }
        MockProvider mockProvider = this.mMockProviders.get(str);
        if (mockProvider == null || (properties = mockProvider.getProperties()) == null || properties.mRequiresSatellite) {
            return 2;
        }
        return (properties.mRequiresNetwork || properties.mRequiresCell) ? 1 : 2;
    }

    private void checkResolutionLevelIsSufficientForProviderUse(int i, String str) {
        int minimumResolutionLevelForProviderUse = getMinimumResolutionLevelForProviderUse(str);
        if (i < minimumResolutionLevelForProviderUse) {
            switch (minimumResolutionLevelForProviderUse) {
                case 1:
                    throw new SecurityException("\"" + str + "\" location provider requires ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION permission.");
                case 2:
                    throw new SecurityException("\"" + str + "\" location provider requires ACCESS_FINE_LOCATION permission.");
                default:
                    throw new SecurityException("Insufficient permission for \"" + str + "\" location provider.");
            }
        }
    }

    private void checkDeviceStatsAllowed() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.UPDATE_DEVICE_STATS", null);
    }

    private void checkUpdateAppOpsAllowed() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.UPDATE_APP_OPS_STATS", null);
    }

    public static int resolutionLevelToOp(int i) {
        if (i != 0) {
            if (i != 1) {
                return 1;
            }
            return 0;
        }
        return -1;
    }

    boolean reportLocationAccessNoThrow(int i, int i2, String str, int i3) {
        int iResolutionLevelToOp = resolutionLevelToOp(i3);
        return (iResolutionLevelToOp < 0 || this.mAppOps.noteOpNoThrow(iResolutionLevelToOp, i2, str) == 0 || mtkNoteOpForCta(iResolutionLevelToOp, i, i2, str, i3)) && getAllowedResolutionLevel(i, i2) >= i3;
    }

    boolean checkLocationAccess(int i, int i2, String str, int i3) {
        int iResolutionLevelToOp = resolutionLevelToOp(i3);
        return (iResolutionLevelToOp < 0 || this.mAppOps.checkOp(iResolutionLevelToOp, i2, str) == 0 || mtkCheckOpForCta(iResolutionLevelToOp, i, i2, str, i3)) && getAllowedResolutionLevel(i, i2) >= i3;
    }

    public List<String> getAllProviders() {
        ArrayList arrayList;
        synchronized (this.mLock) {
            arrayList = new ArrayList(this.mProviders.size());
            Iterator<LocationProviderInterface> it = this.mProviders.iterator();
            while (it.hasNext()) {
                String name = it.next().getName();
                if (!"fused".equals(name)) {
                    arrayList.add(name);
                }
            }
        }
        if (D) {
            Log.d(TAG, "getAllProviders()=" + arrayList);
        }
        return arrayList;
    }

    public List<String> getProviders(Criteria criteria, boolean z) {
        ArrayList arrayList;
        int callerAllowedResolutionLevel = getCallerAllowedResolutionLevel();
        int callingUid = Binder.getCallingUid();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                arrayList = new ArrayList(this.mProviders.size());
                for (LocationProviderInterface locationProviderInterface : this.mProviders) {
                    String name = locationProviderInterface.getName();
                    if (!"fused".equals(name)) {
                        if (callerAllowedResolutionLevel >= getMinimumResolutionLevelForProviderUse(name)) {
                            if (!z || isAllowedByUserSettingsLocked(name, callingUid, this.mCurrentUserId)) {
                                if (criteria == null || LocationProvider.propertiesMeetCriteria(name, locationProviderInterface.getProperties(), criteria)) {
                                    arrayList.add(name);
                                }
                            }
                        }
                    }
                }
            }
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            if (D) {
                Log.d(TAG, "getProviders()=" + arrayList);
            }
            return arrayList;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    public String getBestProvider(Criteria criteria, boolean z) {
        List<String> providers = getProviders(criteria, z);
        if (!providers.isEmpty()) {
            String strPickBest = pickBest(providers);
            if (D) {
                Log.d(TAG, "getBestProvider(" + criteria + ", " + z + ")=" + strPickBest);
            }
            return strPickBest;
        }
        List<String> providers2 = getProviders(null, z);
        if (!providers2.isEmpty()) {
            String strPickBest2 = pickBest(providers2);
            if (D) {
                Log.d(TAG, "getBestProvider(" + criteria + ", " + z + ")=" + strPickBest2);
            }
            return strPickBest2;
        }
        if (D) {
            Log.d(TAG, "getBestProvider(" + criteria + ", " + z + ")=" + ((String) null));
        }
        return null;
    }

    private String pickBest(List<String> list) {
        if (list.contains("gps")) {
            return "gps";
        }
        if (list.contains("network")) {
            return "network";
        }
        return list.get(0);
    }

    public boolean providerMeetsCriteria(String str, Criteria criteria) {
        LocationProviderInterface locationProviderInterface = this.mProvidersByName.get(str);
        if (locationProviderInterface == null) {
            throw new IllegalArgumentException("provider=" + str);
        }
        boolean zPropertiesMeetCriteria = LocationProvider.propertiesMeetCriteria(locationProviderInterface.getName(), locationProviderInterface.getProperties(), criteria);
        if (D) {
            Log.d(TAG, "providerMeetsCriteria(" + str + ", " + criteria + ")=" + zPropertiesMeetCriteria);
        }
        return zPropertiesMeetCriteria;
    }

    private void updateProvidersLocked() {
        boolean z = false;
        for (int size = this.mProviders.size() - 1; size >= 0; size += -1) {
            LocationProviderInterface locationProviderInterface = this.mProviders.get(size);
            boolean zIsEnabled = locationProviderInterface.isEnabled();
            String name = locationProviderInterface.getName();
            boolean zIsAllowedByCurrentUserSettingsLocked = isAllowedByCurrentUserSettingsLocked(name);
            if (zIsEnabled && !zIsAllowedByCurrentUserSettingsLocked) {
                updateProviderListenersLocked(name, false);
                this.mLastLocation.clear();
                this.mLastLocationCoarseInterval.clear();
            } else if (!zIsEnabled && zIsAllowedByCurrentUserSettingsLocked) {
                updateProviderListenersLocked(name, true);
            } else {
                if (!zIsEnabled || !zIsAllowedByCurrentUserSettingsLocked) {
                }
                Log.d(TAG, "updateProvidersLocked provider:" + name + " changesMade: " + z + " isEnabled:" + zIsEnabled + " shouldBeEnabled:" + zIsAllowedByCurrentUserSettingsLocked);
            }
            z = true;
            Log.d(TAG, "updateProvidersLocked provider:" + name + " changesMade: " + z + " isEnabled:" + zIsEnabled + " shouldBeEnabled:" + zIsAllowedByCurrentUserSettingsLocked);
        }
        if (z) {
            this.mContext.sendBroadcastAsUser(new Intent("android.location.PROVIDERS_CHANGED"), UserHandle.ALL);
            this.mContext.sendBroadcastAsUser(new Intent("android.location.MODE_CHANGED"), UserHandle.ALL);
        }
    }

    private void updateProviderListenersLocked(String str, boolean z) {
        int i;
        LocationProviderInterface locationProviderInterface = this.mProvidersByName.get(str);
        if (locationProviderInterface == null) {
            return;
        }
        ArrayList arrayList = null;
        ArrayList<UpdateRecord> arrayList2 = this.mRecordsByProvider.get(str);
        if (arrayList2 != null) {
            i = 0;
            for (UpdateRecord updateRecord : arrayList2) {
                if (isCurrentProfile(UserHandle.getUserId(updateRecord.mReceiver.mIdentity.mUid))) {
                    if (!updateRecord.mReceiver.callProviderEnabledLocked(str, z)) {
                        if (arrayList == null) {
                            arrayList = new ArrayList();
                        }
                        arrayList.add(updateRecord.mReceiver);
                    }
                    i++;
                }
            }
        } else {
            i = 0;
        }
        if (arrayList != null) {
            for (int size = arrayList.size() - 1; size >= 0; size--) {
                removeUpdatesLocked((Receiver) arrayList.get(size));
            }
        }
        if (z) {
            locationProviderInterface.enable();
            if (i > 0) {
                applyRequirementsLocked(str);
                return;
            }
            return;
        }
        locationProviderInterface.disable();
        if (isProviderInWhitelist(str) && isProviderRecordsContainsWhitelistPackage(str, false) && !isWhitelistWorkingMode(str)) {
            Log.d(TAG, "provider disabled to enter whitelist working mode");
            setWhitelistWorkingMode(str, true);
        }
    }

    private void applyRequirementsLocked(String str) {
        LocationProviderInterface locationProviderInterface = this.mProvidersByName.get(str);
        if (locationProviderInterface == null) {
            return;
        }
        ArrayList<UpdateRecord> arrayList = this.mRecordsByProvider.get(str);
        WorkSource workSource = new WorkSource();
        ProviderRequest providerRequest = new ProviderRequest();
        long j = Settings.Global.getLong(this.mContext.getContentResolver(), "location_background_throttle_interval_ms", 1800000L);
        providerRequest.lowPowerMode = true;
        if (arrayList != null) {
            for (UpdateRecord updateRecord : arrayList) {
                if (isCurrentProfile(UserHandle.getUserId(updateRecord.mReceiver.mIdentity.mUid)) && checkLocationAccess(updateRecord.mReceiver.mIdentity.mPid, updateRecord.mReceiver.mIdentity.mUid, updateRecord.mReceiver.mIdentity.mPackageName, updateRecord.mReceiver.mAllowedResolutionLevel)) {
                    LocationRequest locationRequest = updateRecord.mRealRequest;
                    long interval = locationRequest.getInterval();
                    if (!isThrottlingExemptLocked(updateRecord.mReceiver.mIdentity)) {
                        if (!updateRecord.mIsForegroundUid) {
                            interval = Math.max(interval, j);
                        }
                        if (interval != locationRequest.getInterval()) {
                            LocationRequest locationRequest2 = new LocationRequest(locationRequest);
                            locationRequest2.setInterval(interval);
                            locationRequest = locationRequest2;
                        }
                    }
                    updateRecord.mRequest = locationRequest;
                    providerRequest.locationRequests.add(locationRequest);
                    if (!locationRequest.isLowPowerMode()) {
                        providerRequest.lowPowerMode = false;
                    }
                    if (interval < providerRequest.interval) {
                        providerRequest.reportLocation = true;
                        providerRequest.interval = interval;
                    }
                }
            }
            if (providerRequest.reportLocation) {
                long j2 = ((providerRequest.interval + 1000) * 3) / 2;
                for (UpdateRecord updateRecord2 : arrayList) {
                    if (isCurrentProfile(UserHandle.getUserId(updateRecord2.mReceiver.mIdentity.mUid))) {
                        LocationRequest locationRequest3 = updateRecord2.mRequest;
                        if (providerRequest.locationRequests.contains(locationRequest3) && locationRequest3.getInterval() <= j2) {
                            if (updateRecord2.mReceiver.mWorkSource != null && isValidWorkSource(updateRecord2.mReceiver.mWorkSource)) {
                                workSource.add(updateRecord2.mReceiver.mWorkSource);
                            } else {
                                workSource.add(updateRecord2.mReceiver.mIdentity.mUid, updateRecord2.mReceiver.mIdentity.mPackageName);
                            }
                        }
                    }
                }
            }
        }
        if (D) {
            Log.d(TAG, "provider request: " + str + " " + providerRequest);
        }
        locationProviderInterface.setRequest(providerRequest, workSource);
    }

    private static boolean isValidWorkSource(WorkSource workSource) {
        if (workSource.size() > 0) {
            return workSource.getName(0) != null;
        }
        ArrayList workChains = workSource.getWorkChains();
        return (workChains == null || workChains.isEmpty() || ((WorkSource.WorkChain) workChains.get(0)).getAttributionTag() == null) ? false : true;
    }

    public String[] getBackgroundThrottlingWhitelist() {
        String[] strArr;
        synchronized (this.mLock) {
            strArr = (String[]) this.mBackgroundThrottlePackageWhitelist.toArray(new String[this.mBackgroundThrottlePackageWhitelist.size()]);
        }
        return strArr;
    }

    private void updateBackgroundThrottlingWhitelistLocked() {
        String string = Settings.Global.getString(this.mContext.getContentResolver(), "location_background_throttle_package_whitelist");
        if (string == null) {
            string = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        this.mBackgroundThrottlePackageWhitelist.clear();
        this.mBackgroundThrottlePackageWhitelist.addAll(SystemConfig.getInstance().getAllowUnthrottledLocation());
        this.mBackgroundThrottlePackageWhitelist.addAll(Arrays.asList(string.split(",")));
    }

    private boolean isThrottlingExemptLocked(Identity identity) {
        if (identity.mUid == 1000 || this.mBackgroundThrottlePackageWhitelist.contains(identity.mPackageName)) {
            return true;
        }
        Iterator<LocationProviderProxy> it = this.mProxyProviders.iterator();
        while (it.hasNext()) {
            if (identity.mPackageName.equals(it.next().getConnectedPackageName())) {
                return true;
            }
        }
        return false;
    }

    private class UpdateRecord {
        boolean mIsForegroundUid;
        Location mLastFixBroadcast;
        long mLastStatusBroadcast;
        final String mProvider;
        final LocationRequest mRealRequest;
        final Receiver mReceiver;
        LocationRequest mRequest;

        UpdateRecord(String str, LocationRequest locationRequest, Receiver receiver) {
            this.mProvider = str;
            this.mRealRequest = locationRequest;
            this.mRequest = locationRequest;
            this.mReceiver = receiver;
            this.mIsForegroundUid = LocationManagerService.isImportanceForeground(LocationManagerService.this.mActivityManager.getPackageImportance(this.mReceiver.mIdentity.mPackageName));
            ArrayList arrayList = (ArrayList) LocationManagerService.this.mRecordsByProvider.get(str);
            if (arrayList == null) {
                arrayList = new ArrayList();
                LocationManagerService.this.mRecordsByProvider.put(str, arrayList);
            }
            if (!arrayList.contains(this)) {
                arrayList.add(this);
            }
            LocationManagerService.this.mRequestStatistics.startRequesting(this.mReceiver.mIdentity.mPackageName, str, locationRequest.getInterval(), this.mIsForegroundUid);
        }

        void updateForeground(boolean z) {
            this.mIsForegroundUid = z;
            LocationManagerService.this.mRequestStatistics.updateForeground(this.mReceiver.mIdentity.mPackageName, this.mProvider, z);
        }

        void disposeLocked(boolean z) {
            HashMap<String, UpdateRecord> map;
            LocationManagerService.this.mRequestStatistics.stopRequesting(this.mReceiver.mIdentity.mPackageName, this.mProvider);
            ArrayList arrayList = (ArrayList) LocationManagerService.this.mRecordsByProvider.get(this.mProvider);
            if (arrayList != null) {
                arrayList.remove(this);
            }
            if (z && (map = this.mReceiver.mUpdateRecords) != null) {
                map.remove(this.mProvider);
                if (map.size() == 0) {
                    LocationManagerService.this.removeUpdatesLocked(this.mReceiver);
                }
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("UpdateRecord[");
            sb.append(this.mProvider);
            sb.append(" ");
            sb.append(this.mReceiver.mIdentity.mPackageName);
            sb.append("(");
            sb.append(this.mReceiver.mIdentity.mUid);
            sb.append(this.mIsForegroundUid ? " foreground" : " background");
            sb.append(") ");
            sb.append(this.mRealRequest);
            sb.append("]");
            return sb.toString();
        }
    }

    private Receiver getReceiverLocked(ILocationListener iLocationListener, int i, int i2, String str, WorkSource workSource, boolean z) {
        IBinder iBinderAsBinder = iLocationListener.asBinder();
        Receiver receiver = this.mReceivers.get(iBinderAsBinder);
        if (receiver == null) {
            Receiver receiver2 = new Receiver(iLocationListener, null, i, i2, str, workSource, z);
            try {
                receiver2.getListener().asBinder().linkToDeath(receiver2, 0);
                this.mReceivers.put(iBinderAsBinder, receiver2);
                return receiver2;
            } catch (RemoteException e) {
                Slog.e(TAG, "linkToDeath failed:", e);
                return null;
            }
        }
        return receiver;
    }

    private Receiver getReceiverLocked(PendingIntent pendingIntent, int i, int i2, String str, WorkSource workSource, boolean z) {
        Receiver receiver = this.mReceivers.get(pendingIntent);
        if (receiver == null) {
            Receiver receiver2 = new Receiver(null, pendingIntent, i, i2, str, workSource, z);
            this.mReceivers.put(pendingIntent, receiver2);
            return receiver2;
        }
        return receiver;
    }

    private LocationRequest createSanitizedRequest(LocationRequest locationRequest, int i, boolean z) {
        LocationRequest locationRequest2 = new LocationRequest(locationRequest);
        if (!z) {
            locationRequest2.setLowPowerMode(false);
        }
        if (i < 2) {
            int quality = locationRequest2.getQuality();
            if (quality == 100) {
                locationRequest2.setQuality(HdmiCecKeycode.CEC_KEYCODE_RESTORE_VOLUME_FUNCTION);
            } else if (quality == 203) {
                locationRequest2.setQuality(201);
            }
            if (locationRequest2.getInterval() < 600000) {
                locationRequest2.setInterval(600000L);
            }
            if (locationRequest2.getFastestInterval() < 600000) {
                locationRequest2.setFastestInterval(600000L);
            }
        }
        if (locationRequest2.getFastestInterval() > locationRequest2.getInterval()) {
            locationRequest.setFastestInterval(locationRequest.getInterval());
        }
        return locationRequest2;
    }

    private void checkPackageName(String str) {
        if (str == null) {
            throw new SecurityException("invalid package name: " + str);
        }
        int callingUid = Binder.getCallingUid();
        String[] packagesForUid = this.mPackageManager.getPackagesForUid(callingUid);
        if (packagesForUid == null) {
            throw new SecurityException("invalid UID " + callingUid);
        }
        for (String str2 : packagesForUid) {
            if (str.equals(str2)) {
                return;
            }
        }
        throw new SecurityException("invalid package name: " + str);
    }

    private void checkPendingIntent(PendingIntent pendingIntent) {
        if (pendingIntent == null) {
            throw new IllegalArgumentException("invalid pending intent: " + pendingIntent);
        }
    }

    private Receiver checkListenerOrIntentLocked(ILocationListener iLocationListener, PendingIntent pendingIntent, int i, int i2, String str, WorkSource workSource, boolean z) {
        if (pendingIntent == null && iLocationListener == null) {
            throw new IllegalArgumentException("need either listener or intent");
        }
        if (pendingIntent != null && iLocationListener != null) {
            throw new IllegalArgumentException("cannot register both listener and intent");
        }
        if (pendingIntent != null) {
            checkPendingIntent(pendingIntent);
            return getReceiverLocked(pendingIntent, i, i2, str, workSource, z);
        }
        return getReceiverLocked(iLocationListener, i, i2, str, workSource, z);
    }

    public void requestLocationUpdates(LocationRequest locationRequest, ILocationListener iLocationListener, PendingIntent pendingIntent, String str) {
        LocationRequest locationRequest2 = locationRequest == null ? DEFAULT_LOCATION_REQUEST : locationRequest;
        checkPackageName(str);
        int callerAllowedResolutionLevel = getCallerAllowedResolutionLevel();
        checkResolutionLevelIsSufficientForProviderUse(callerAllowedResolutionLevel, locationRequest2.getProvider());
        WorkSource workSource = locationRequest2.getWorkSource();
        if (workSource != null && !workSource.isEmpty()) {
            checkDeviceStatsAllowed();
        }
        boolean hideFromAppOps = locationRequest2.getHideFromAppOps();
        if (hideFromAppOps) {
            checkUpdateAppOpsAllowed();
        }
        LocationRequest locationRequestCreateSanitizedRequest = createSanitizedRequest(locationRequest2, callerAllowedResolutionLevel, this.mContext.checkCallingPermission("android.permission.LOCATION_HARDWARE") == 0);
        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            checkLocationAccess(callingPid, callingUid, str, callerAllowedResolutionLevel);
            synchronized (this.mLock) {
                requestLocationUpdatesLocked(locationRequestCreateSanitizedRequest, checkListenerOrIntentLocked(iLocationListener, pendingIntent, callingPid, callingUid, str, workSource, hideFromAppOps), callingPid, callingUid, str);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void requestLocationUpdatesLocked(LocationRequest locationRequest, Receiver receiver, int i, int i2, String str) {
        if (locationRequest == null) {
            locationRequest = DEFAULT_LOCATION_REQUEST;
        }
        String provider = locationRequest.getProvider();
        if (provider == null) {
            throw new IllegalArgumentException("provider name must not be null");
        }
        LocationProviderInterface locationProviderInterface = this.mProvidersByName.get(provider);
        if (locationProviderInterface == null) {
            throw new IllegalArgumentException("provider doesn't exist: " + provider);
        }
        UpdateRecord updateRecord = new UpdateRecord(provider, locationRequest, receiver);
        StringBuilder sb = new StringBuilder();
        sb.append("request ");
        sb.append(Integer.toHexString(System.identityHashCode(receiver)));
        sb.append(" ");
        sb.append(provider);
        sb.append(" ");
        sb.append(locationRequest);
        sb.append(" from ");
        sb.append(str);
        sb.append("(");
        sb.append(i2);
        sb.append(" ");
        sb.append(updateRecord.mIsForegroundUid ? "foreground" : "background");
        sb.append(isThrottlingExemptLocked(receiver.mIdentity) ? " [whitelisted]" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        sb.append(")");
        Log.d(TAG, sb.toString());
        UpdateRecord updateRecordPut = receiver.mUpdateRecords.put(provider, updateRecord);
        if (updateRecordPut != null) {
            updateRecordPut.disposeLocked(false);
        }
        if (isAllowedByUserSettingsLocked(provider, i2, this.mCurrentUserId)) {
            applyRequirementsLocked(provider);
        } else if (!mtkWhitelistRequestLocationUpdates(provider, str, locationProviderInterface)) {
            receiver.callProviderEnabledLocked(provider, false);
        }
        receiver.updateMonitoring(true);
    }

    public void removeUpdates(ILocationListener iLocationListener, PendingIntent pendingIntent, String str) {
        checkPackageName(str);
        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();
        synchronized (this.mLock) {
            Receiver receiverCheckListenerOrIntentLocked = checkListenerOrIntentLocked(iLocationListener, pendingIntent, callingPid, callingUid, str, null, false);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                removeUpdatesLocked(receiverCheckListenerOrIntentLocked);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    private void removeUpdatesLocked(Receiver receiver) {
        Log.i(TAG, "remove " + Integer.toHexString(System.identityHashCode(receiver)));
        if (this.mReceivers.remove(receiver.mKey) != null && receiver.isListener()) {
            receiver.getListener().asBinder().unlinkToDeath(receiver, 0);
            synchronized (receiver) {
                receiver.clearPendingBroadcastsLocked();
            }
        }
        receiver.updateMonitoring(false);
        HashSet<String> hashSet = new HashSet();
        HashMap<String, UpdateRecord> map = receiver.mUpdateRecords;
        if (map != null) {
            Iterator<UpdateRecord> it = map.values().iterator();
            while (it.hasNext()) {
                it.next().disposeLocked(false);
            }
            hashSet.addAll(map.keySet());
        }
        for (String str : hashSet) {
            if (isAllowedByCurrentUserSettingsLocked(str)) {
                applyRequirementsLocked(str);
            }
        }
        disableProviderWhenNoWhitelistPackageRegistered();
    }

    private void applyAllProviderRequirementsLocked() {
        for (LocationProviderInterface locationProviderInterface : this.mProviders) {
            if (isAllowedByCurrentUserSettingsLocked(locationProviderInterface.getName())) {
                applyRequirementsLocked(locationProviderInterface.getName());
            }
        }
    }

    public Location getLastLocation(LocationRequest locationRequest, String str) {
        if (D) {
            Log.d(TAG, "getLastLocation: " + locationRequest);
        }
        if (locationRequest == null) {
            locationRequest = DEFAULT_LOCATION_REQUEST;
        }
        int callerAllowedResolutionLevel = getCallerAllowedResolutionLevel();
        checkPackageName(str);
        checkResolutionLevelIsSufficientForProviderUse(callerAllowedResolutionLevel, locationRequest.getProvider());
        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (this.mBlacklist.isBlacklisted(str)) {
                if (D) {
                    Log.d(TAG, "not returning last loc for blacklisted app: " + str);
                }
                return null;
            }
            if (!reportLocationAccessNoThrow(callingPid, callingUid, str, callerAllowedResolutionLevel)) {
                if (D) {
                    Log.d(TAG, "not returning last loc for no op app: " + str);
                }
                return null;
            }
            synchronized (this.mLock) {
                String provider = locationRequest.getProvider();
                if (provider == null) {
                    provider = "fused";
                }
                if (this.mProvidersByName.get(provider) == null) {
                    return null;
                }
                if (!isAllowedByUserSettingsLocked(provider, callingUid, this.mCurrentUserId)) {
                    return null;
                }
                Location location = callerAllowedResolutionLevel < 2 ? this.mLastLocationCoarseInterval.get(provider) : this.mLastLocation.get(provider);
                if (location == null) {
                    return null;
                }
                if (callerAllowedResolutionLevel >= 2) {
                    return new Location(location);
                }
                Location extraLocation = location.getExtraLocation("noGPSLocation");
                if (extraLocation != null) {
                    return new Location(this.mLocationFudger.getOrCreate(extraLocation));
                }
                return null;
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public boolean injectLocation(Location location) {
        this.mContext.enforceCallingPermission("android.permission.LOCATION_HARDWARE", "Location Hardware permission not granted to inject location");
        this.mContext.enforceCallingPermission("android.permission.ACCESS_FINE_LOCATION", "Access Fine Location permission not granted to inject Location");
        if (location == null) {
            if (D) {
                Log.d(TAG, "injectLocation(): called with null location");
            }
            return false;
        }
        LocationProviderInterface locationProviderInterface = null;
        String provider = location.getProvider();
        if (provider != null) {
            locationProviderInterface = this.mProvidersByName.get(provider);
        }
        if (locationProviderInterface == null) {
            if (D) {
                Log.d(TAG, "injectLocation(): unknown provider");
            }
            return false;
        }
        synchronized (this.mLock) {
            if (!isAllowedByCurrentUserSettingsLocked(provider)) {
                if (D) {
                    Log.d(TAG, "Location disabled in Settings for current user:" + this.mCurrentUserId);
                }
                return false;
            }
            if (this.mLastLocation.get(provider) == null) {
                updateLastLocationLocked(location, provider);
                return true;
            }
            if (D) {
                Log.d(TAG, "injectLocation(): Location exists. Not updating");
            }
            return false;
        }
    }

    public void requestGeofence(LocationRequest locationRequest, Geofence geofence, PendingIntent pendingIntent, String str) {
        if (locationRequest == null) {
            locationRequest = DEFAULT_LOCATION_REQUEST;
        }
        int callerAllowedResolutionLevel = getCallerAllowedResolutionLevel();
        checkResolutionLevelIsSufficientForGeofenceUse(callerAllowedResolutionLevel);
        checkPendingIntent(pendingIntent);
        checkPackageName(str);
        checkResolutionLevelIsSufficientForProviderUse(callerAllowedResolutionLevel, locationRequest.getProvider());
        LocationRequest locationRequestCreateSanitizedRequest = createSanitizedRequest(locationRequest, callerAllowedResolutionLevel, this.mContext.checkCallingPermission("android.permission.LOCATION_HARDWARE") == 0);
        if (D) {
            Log.d(TAG, "requestGeofence: " + locationRequestCreateSanitizedRequest + " " + geofence + " " + pendingIntent);
        }
        int callingUid = Binder.getCallingUid();
        if (UserHandle.getUserId(callingUid) != 0) {
            Log.w(TAG, "proximity alerts are currently available only to the primary user");
            return;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mGeofenceManager.addFence(locationRequestCreateSanitizedRequest, geofence, pendingIntent, callerAllowedResolutionLevel, callingUid, str);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void removeGeofence(Geofence geofence, PendingIntent pendingIntent, String str) {
        checkPendingIntent(pendingIntent);
        checkPackageName(str);
        if (D) {
            Log.d(TAG, "removeGeofence: " + geofence + " " + pendingIntent);
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mGeofenceManager.removeFence(geofence, pendingIntent);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public boolean registerGnssStatusCallback(IGnssStatusListener iGnssStatusListener, String str) {
        if (!hasGnssPermissions(str) || this.mGnssStatusProvider == null) {
            return false;
        }
        try {
            this.mGnssStatusProvider.registerGnssStatusCallback(iGnssStatusListener);
            Log.d(TAG, "registerGnssStatusCallback by package: " + str + " ,callback binder: " + (iGnssStatusListener != null ? iGnssStatusListener.asBinder() : null));
            return true;
        } catch (RemoteException e) {
            Slog.e(TAG, "mGpsStatusProvider.registerGnssStatusCallback failed", e);
            return false;
        }
    }

    public void unregisterGnssStatusCallback(IGnssStatusListener iGnssStatusListener) {
        synchronized (this.mLock) {
            try {
                this.mGnssStatusProvider.unregisterGnssStatusCallback(iGnssStatusListener);
                Log.d(TAG, "unregisterGnssStatusCallback, callback binder: " + (iGnssStatusListener != null ? iGnssStatusListener.asBinder() : null));
            } catch (Exception e) {
                Slog.e(TAG, "mGpsStatusProvider.unregisterGnssStatusCallback failed", e);
            }
        }
    }

    public boolean addGnssMeasurementsListener(IGnssMeasurementsListener iGnssMeasurementsListener, String str) {
        if (!hasGnssPermissions(str) || this.mGnssMeasurementsProvider == null) {
            return false;
        }
        synchronized (this.mLock) {
            Identity identity = new Identity(Binder.getCallingUid(), Binder.getCallingPid(), str);
            this.mGnssMeasurementsListeners.put(iGnssMeasurementsListener.asBinder(), identity);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                if (isThrottlingExemptLocked(identity) || isImportanceForeground(this.mActivityManager.getPackageImportance(str))) {
                    return this.mGnssMeasurementsProvider.addListener(iGnssMeasurementsListener);
                }
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                return true;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    public void removeGnssMeasurementsListener(IGnssMeasurementsListener iGnssMeasurementsListener) {
        if (this.mGnssMeasurementsProvider != null) {
            synchronized (this.mLock) {
                this.mGnssMeasurementsListeners.remove(iGnssMeasurementsListener.asBinder());
                this.mGnssMeasurementsProvider.removeListener(iGnssMeasurementsListener);
            }
        }
    }

    public boolean addGnssNavigationMessageListener(IGnssNavigationMessageListener iGnssNavigationMessageListener, String str) {
        if (!hasGnssPermissions(str) || this.mGnssNavigationMessageProvider == null) {
            return false;
        }
        synchronized (this.mLock) {
            Identity identity = new Identity(Binder.getCallingUid(), Binder.getCallingPid(), str);
            this.mGnssNavigationMessageListeners.put(iGnssNavigationMessageListener.asBinder(), identity);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                if (isThrottlingExemptLocked(identity) || isImportanceForeground(this.mActivityManager.getPackageImportance(str))) {
                    return this.mGnssNavigationMessageProvider.addListener(iGnssNavigationMessageListener);
                }
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                return true;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    public void removeGnssNavigationMessageListener(IGnssNavigationMessageListener iGnssNavigationMessageListener) {
        if (this.mGnssNavigationMessageProvider != null) {
            synchronized (this.mLock) {
                this.mGnssNavigationMessageListeners.remove(iGnssNavigationMessageListener.asBinder());
                this.mGnssNavigationMessageProvider.removeListener(iGnssNavigationMessageListener);
            }
        }
    }

    public boolean sendExtraCommand(String str, String str2, Bundle bundle) {
        if (str == null) {
            throw new NullPointerException();
        }
        checkResolutionLevelIsSufficientForProviderUse(getCallerAllowedResolutionLevel(), str);
        if (this.mContext.checkCallingOrSelfPermission(ACCESS_LOCATION_EXTRA_COMMANDS) != 0) {
            throw new SecurityException("Requires ACCESS_LOCATION_EXTRA_COMMANDS permission");
        }
        synchronized (this.mLock) {
            LocationProviderInterface locationProviderInterface = this.mProvidersByName.get(str);
            if (locationProviderInterface == null) {
                return false;
            }
            return locationProviderInterface.sendExtraCommand(str2, bundle);
        }
    }

    public boolean sendNiResponse(int i, int i2) {
        if (Binder.getCallingUid() != Process.myUid()) {
            throw new SecurityException("calling sendNiResponse from outside of the system is not allowed");
        }
        try {
            return this.mNetInitiatedListener.sendNiResponse(i, i2);
        } catch (RemoteException e) {
            Slog.e(TAG, "RemoteException in LocationManagerService.sendNiResponse");
            return false;
        }
    }

    public ProviderProperties getProviderProperties(String str) {
        LocationProviderInterface locationProviderInterface;
        if (this.mProvidersByName.get(str) == null) {
            return null;
        }
        checkResolutionLevelIsSufficientForProviderUse(getCallerAllowedResolutionLevel(), str);
        synchronized (this.mLock) {
            locationProviderInterface = this.mProvidersByName.get(str);
        }
        if (locationProviderInterface == null) {
            return null;
        }
        return locationProviderInterface.getProperties();
    }

    public String getNetworkProviderPackage() {
        synchronized (this.mLock) {
            if (this.mProvidersByName.get("network") == null) {
                return null;
            }
            LocationProviderInterface locationProviderInterface = this.mProvidersByName.get("network");
            if (locationProviderInterface instanceof LocationProviderProxy) {
                return ((LocationProviderProxy) locationProviderInterface).getConnectedPackageName();
            }
            return null;
        }
    }

    public boolean isLocationEnabledForUser(int i) {
        boolean z;
        checkInteractAcrossUsersPermission(i);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                z = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "location_mode", 0, i) != 0;
            }
            return z;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void setLocationEnabledForUser(boolean z, int i) {
        this.mContext.enforceCallingPermission("android.permission.WRITE_SECURE_SETTINGS", "Requires WRITE_SECURE_SETTINGS permission");
        checkInteractAcrossUsersPermission(i);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "location_mode", z ? 3 : 0, i);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public boolean isProviderEnabledForUser(String str, int i) {
        checkInteractAcrossUsersPermission(i);
        boolean z = false;
        if ("fused".equals(str)) {
            return false;
        }
        int callingUid = Binder.getCallingUid();
        synchronized (this.mLock) {
            if (this.mProvidersByName.get(str) != null && isAllowedByUserSettingsLocked(str, callingUid, i)) {
                z = true;
            }
        }
        return z;
    }

    public boolean setProviderEnabledForUser(String str, boolean z, int i) {
        this.mContext.enforceCallingPermission("android.permission.WRITE_SECURE_SETTINGS", "Requires WRITE_SECURE_SETTINGS permission");
        checkInteractAcrossUsersPermission(i);
        if ("fused".equals(str)) {
            return false;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                if (!this.mProvidersByName.containsKey(str)) {
                    return false;
                }
                if (this.mMockProviders.containsKey(str)) {
                    setTestProviderEnabled(str, z);
                    return true;
                }
                StringBuilder sb = new StringBuilder();
                sb.append(z ? "+" : "-");
                sb.append(str);
                return Settings.Secure.putStringForUser(this.mContext.getContentResolver(), "location_providers_allowed", sb.toString(), i);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private boolean isLocationProviderEnabledForUser(String str, int i) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return TextUtils.delimitedStringContains(Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "location_providers_allowed", i), ',', str);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void checkInteractAcrossUsersPermission(int i) {
        int callingUid = Binder.getCallingUid();
        if (UserHandle.getUserId(callingUid) != i && ActivityManager.checkComponentPermission("android.permission.INTERACT_ACROSS_USERS", callingUid, -1, true) != 0) {
            throw new SecurityException("Requires INTERACT_ACROSS_USERS permission");
        }
    }

    private boolean isUidALocationProvider(int i) {
        if (i == 1000) {
            return true;
        }
        if (this.mGeocodeProvider != null && doesUidHavePackage(i, this.mGeocodeProvider.getConnectedPackageName())) {
            return true;
        }
        Iterator<LocationProviderProxy> it = this.mProxyProviders.iterator();
        while (it.hasNext()) {
            if (doesUidHavePackage(i, it.next().getConnectedPackageName())) {
                return true;
            }
        }
        return false;
    }

    private void checkCallerIsProvider() {
        if (this.mContext.checkCallingOrSelfPermission(INSTALL_LOCATION_PROVIDER) == 0 || isUidALocationProvider(Binder.getCallingUid())) {
        } else {
            throw new SecurityException("need INSTALL_LOCATION_PROVIDER permission, or UID of a currently bound location provider");
        }
    }

    private boolean doesUidHavePackage(int i, String str) {
        String[] packagesForUid;
        if (str == null || (packagesForUid = this.mPackageManager.getPackagesForUid(i)) == null) {
            return false;
        }
        for (String str2 : packagesForUid) {
            if (str.equals(str2)) {
                return true;
            }
        }
        return false;
    }

    public void reportLocation(Location location, boolean z) {
        checkCallerIsProvider();
        if (!location.isComplete()) {
            Log.w(TAG, "Dropping incomplete location: " + location);
            return;
        }
        this.mLocationHandler.removeMessages(1, location);
        Message messageObtain = Message.obtain(this.mLocationHandler, 1, location);
        messageObtain.arg1 = z ? 1 : 0;
        this.mLocationHandler.sendMessageAtFrontOfQueue(messageObtain);
    }

    private static boolean shouldBroadcastSafe(Location location, Location location2, UpdateRecord updateRecord, long j) {
        if (location2 == null) {
            return true;
        }
        if ((location.getElapsedRealtimeNanos() - location2.getElapsedRealtimeNanos()) / NANOS_PER_MILLI < updateRecord.mRealRequest.getFastestInterval() - 100) {
            return false;
        }
        double smallestDisplacement = updateRecord.mRealRequest.getSmallestDisplacement();
        return (smallestDisplacement <= 0.0d || ((double) location.distanceTo(location2)) > smallestDisplacement) && updateRecord.mRealRequest.getNumUpdates() > 0 && updateRecord.mRealRequest.getExpireAt() >= j;
    }

    private void handleLocationChangedLocked(Location location, boolean z) {
        Location location2;
        Location location3;
        Iterator<UpdateRecord> it;
        Bundle bundle;
        Location location4;
        if (D) {
            Log.d(TAG, "incoming location: " + location);
        } else {
            Log.d(TAG, "incoming location from: " + location.getProvider());
        }
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        String provider = z ? "passive" : location.getProvider();
        LocationProviderInterface locationProviderInterface = this.mProvidersByName.get(provider);
        if (locationProviderInterface == null) {
            return;
        }
        updateLastLocationLocked(location, provider);
        Location location5 = this.mLastLocation.get(provider);
        if (location5 == null) {
            Log.e(TAG, "handleLocationChangedLocked() updateLastLocation failed");
            return;
        }
        Location location6 = this.mLastLocationCoarseInterval.get(provider);
        if (location6 == null) {
            location6 = new Location(location);
            this.mLastLocationCoarseInterval.put(provider, location6);
        }
        if (location.getElapsedRealtimeNanos() - location6.getElapsedRealtimeNanos() > 600000000000L) {
            location6.set(location);
        }
        Location extraLocation = location6.getExtraLocation("noGPSLocation");
        ArrayList<UpdateRecord> arrayList = this.mRecordsByProvider.get(provider);
        if (arrayList == null || arrayList.size() == 0) {
            return;
        }
        ArrayList arrayList2 = null;
        Location orCreate = extraLocation != null ? this.mLocationFudger.getOrCreate(extraLocation) : isCtaSupported() ? this.mLocationFudger.getOrCreate(location6) : null;
        long statusUpdateTime = locationProviderInterface.getStatusUpdateTime();
        Bundle bundle2 = new Bundle();
        int status = locationProviderInterface.getStatus(bundle2);
        Iterator<UpdateRecord> it2 = arrayList.iterator();
        ArrayList arrayList3 = null;
        while (it2.hasNext()) {
            UpdateRecord next = it2.next();
            Receiver receiver = next.mReceiver;
            boolean z2 = false;
            int userId = UserHandle.getUserId(receiver.mIdentity.mUid);
            if (isCurrentProfile(userId)) {
                location2 = orCreate;
            } else {
                location2 = orCreate;
                if (!isUidALocationProvider(receiver.mIdentity.mUid)) {
                    if (D) {
                        StringBuilder sb = new StringBuilder();
                        location3 = location5;
                        sb.append("skipping loc update for background user ");
                        sb.append(userId);
                        sb.append(" (current user: ");
                        sb.append(this.mCurrentUserId);
                        sb.append(", app: ");
                        sb.append(receiver.mIdentity.mPackageName);
                        sb.append(")");
                        Log.d(TAG, sb.toString());
                    } else {
                        location3 = location5;
                    }
                }
                it = it2;
                orCreate = location2;
                location5 = location3;
                it2 = it;
            }
            location3 = location5;
            if (this.mBlacklist.isBlacklisted(receiver.mIdentity.mPackageName)) {
                if (D) {
                    Log.d(TAG, "skipping loc update for blacklisted app: " + receiver.mIdentity.mPackageName);
                }
                it = it2;
                orCreate = location2;
                location5 = location3;
                it2 = it;
            } else {
                it = it2;
                if (reportLocationAccessNoThrow(receiver.mIdentity.mPid, receiver.mIdentity.mUid, receiver.mIdentity.mPackageName, receiver.mAllowedResolutionLevel)) {
                    Location location7 = receiver.mAllowedResolutionLevel < 2 ? location2 : location3;
                    if (location7 != null && ((location4 = next.mLastFixBroadcast) == null || shouldBroadcastSafe(location7, location4, next, jElapsedRealtime))) {
                        if (location4 == null) {
                            next.mLastFixBroadcast = new Location(location7);
                        } else {
                            location4.set(location7);
                        }
                        if (!receiver.callLocationChangedLocked(location7)) {
                            Slog.w(TAG, "RemoteException calling onLocationChanged on " + receiver);
                            z2 = true;
                        }
                        next.mRealRequest.decrementNumUpdates();
                    }
                    boolean z3 = z2;
                    Bundle bundle3 = bundle2;
                    long j = next.mLastStatusBroadcast;
                    if (statusUpdateTime <= j || (j == 0 && status == 2)) {
                        bundle = bundle3;
                    } else {
                        next.mLastStatusBroadcast = statusUpdateTime;
                        bundle = bundle3;
                        if (!receiver.callStatusChangedLocked(provider, status, bundle)) {
                            Slog.w(TAG, "RemoteException calling onStatusChanged on " + receiver);
                            z3 = true;
                        }
                    }
                    if (next.mRealRequest.getNumUpdates() <= 0 || next.mRealRequest.getExpireAt() < jElapsedRealtime) {
                        if (arrayList3 == null) {
                            arrayList3 = new ArrayList();
                        }
                        arrayList3.add(next);
                    }
                    if (z3) {
                        if (arrayList2 == null) {
                            arrayList2 = new ArrayList();
                        }
                        if (!arrayList2.contains(receiver)) {
                            arrayList2.add(receiver);
                        }
                    }
                    bundle2 = bundle;
                } else if (D) {
                    Log.d(TAG, "skipping loc update for no op app: " + receiver.mIdentity.mPackageName);
                }
                orCreate = location2;
                location5 = location3;
                it2 = it;
            }
        }
        if (arrayList2 != null) {
            Iterator it3 = arrayList2.iterator();
            while (it3.hasNext()) {
                removeUpdatesLocked((Receiver) it3.next());
            }
        }
        if (arrayList3 != null) {
            Iterator it4 = arrayList3.iterator();
            while (it4.hasNext()) {
                ((UpdateRecord) it4.next()).disposeLocked(true);
            }
            applyRequirementsLocked(provider);
        }
    }

    private void updateLastLocationLocked(Location location, String str) {
        Location extraLocation = location.getExtraLocation("noGPSLocation");
        Location location2 = this.mLastLocation.get(str);
        if (location2 == null) {
            location2 = new Location(str);
            this.mLastLocation.put(str, location2);
        } else {
            Location extraLocation2 = location2.getExtraLocation("noGPSLocation");
            if (extraLocation == null && extraLocation2 != null) {
                location.setExtraLocation("noGPSLocation", extraLocation2);
            }
        }
        location2.set(location);
    }

    private class LocationWorkerHandler extends Handler {
        public LocationWorkerHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 1) {
                LocationManagerService.this.handleLocationChanged((Location) message.obj, message.arg1 == 1);
            }
        }
    }

    private boolean isMockProvider(String str) {
        boolean zContainsKey;
        synchronized (this.mLock) {
            zContainsKey = this.mMockProviders.containsKey(str);
        }
        return zContainsKey;
    }

    private void handleLocationChanged(Location location, boolean z) {
        Location location2 = new Location(location);
        String provider = location2.getProvider();
        if (!location2.isFromMockProvider() && isMockProvider(provider)) {
            location2.setIsFromMockProvider(true);
        }
        synchronized (this.mLock) {
            if (isAllowedByCurrentUserSettingsLocked(provider)) {
                if (!z) {
                    this.mPassiveProvider.updateLocation(location2);
                }
                handleLocationChangedLocked(location2, z);
            }
        }
    }

    public boolean geocoderIsPresent() {
        return this.mGeocodeProvider != null;
    }

    public String getFromLocation(double d, double d2, int i, GeocoderParams geocoderParams, List<Address> list) {
        if (this.mGeocodeProvider != null) {
            return this.mGeocodeProvider.getFromLocation(d, d2, i, geocoderParams, list);
        }
        return null;
    }

    public String getFromLocationName(String str, double d, double d2, double d3, double d4, int i, GeocoderParams geocoderParams, List<Address> list) {
        if (this.mGeocodeProvider != null) {
            return this.mGeocodeProvider.getFromLocationName(str, d, d2, d3, d4, i, geocoderParams, list);
        }
        return null;
    }

    private boolean canCallerAccessMockLocation(String str) {
        return this.mAppOps.noteOp(58, Binder.getCallingUid(), str) == 0;
    }

    public void addTestProvider(String str, ProviderProperties providerProperties, String str2) {
        LocationProviderInterface locationProviderInterface;
        if (!canCallerAccessMockLocation(str2)) {
            return;
        }
        if ("passive".equals(str)) {
            throw new IllegalArgumentException("Cannot mock the passive location provider");
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        synchronized (this.mLock) {
            if (("gps".equals(str) || "network".equals(str) || "fused".equals(str)) && (locationProviderInterface = this.mProvidersByName.get(str)) != null) {
                removeProviderLocked(locationProviderInterface);
            }
            addTestProviderLocked(str, providerProperties);
            updateProvidersLocked();
        }
        Binder.restoreCallingIdentity(jClearCallingIdentity);
    }

    private void addTestProviderLocked(String str, ProviderProperties providerProperties) {
        if (this.mProvidersByName.get(str) != null) {
            throw new IllegalArgumentException("Provider \"" + str + "\" already exists");
        }
        MockProvider mockProvider = new MockProvider(str, this, providerProperties);
        addProviderLocked(mockProvider);
        this.mMockProviders.put(str, mockProvider);
        this.mLastLocation.put(str, null);
        this.mLastLocationCoarseInterval.put(str, null);
    }

    public void removeTestProvider(String str, String str2) {
        if (!canCallerAccessMockLocation(str2)) {
            return;
        }
        synchronized (this.mLock) {
            clearTestProviderEnabled(str, str2);
            clearTestProviderLocation(str, str2);
            clearTestProviderStatus(str, str2);
            if (this.mMockProviders.remove(str) == null) {
                throw new IllegalArgumentException("Provider \"" + str + "\" unknown");
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            removeProviderLocked(this.mProvidersByName.get(str));
            LocationProviderInterface locationProviderInterface = this.mRealProviders.get(str);
            if (locationProviderInterface != null) {
                addProviderLocked(locationProviderInterface);
            }
            this.mLastLocation.put(str, null);
            this.mLastLocationCoarseInterval.put(str, null);
            updateProvidersLocked();
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void setTestProviderLocation(String str, Location location, String str2) {
        if (!canCallerAccessMockLocation(str2)) {
            return;
        }
        synchronized (this.mLock) {
            MockProvider mockProvider = this.mMockProviders.get(str);
            if (mockProvider == null) {
                throw new IllegalArgumentException("Provider \"" + str + "\" unknown");
            }
            Location location2 = new Location(location);
            location2.setIsFromMockProvider(true);
            if (!TextUtils.isEmpty(location.getProvider()) && !str.equals(location.getProvider())) {
                EventLog.writeEvent(1397638484, "33091107", Integer.valueOf(Binder.getCallingUid()), str + "!=" + location.getProvider());
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            mockProvider.setLocation(location2);
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void clearTestProviderLocation(String str, String str2) {
        if (!canCallerAccessMockLocation(str2)) {
            return;
        }
        synchronized (this.mLock) {
            MockProvider mockProvider = this.mMockProviders.get(str);
            if (mockProvider == null) {
                throw new IllegalArgumentException("Provider \"" + str + "\" unknown");
            }
            mockProvider.clearLocation();
        }
    }

    public void setTestProviderEnabled(String str, boolean z, String str2) {
        if (!canCallerAccessMockLocation(str2)) {
            return;
        }
        setTestProviderEnabled(str, z);
    }

    private void setTestProviderEnabled(String str, boolean z) {
        synchronized (this.mLock) {
            MockProvider mockProvider = this.mMockProviders.get(str);
            if (mockProvider == null) {
                throw new IllegalArgumentException("Provider \"" + str + "\" unknown");
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            if (z) {
                mockProvider.enable();
                this.mEnabledProviders.add(str);
                this.mDisabledProviders.remove(str);
            } else {
                mockProvider.disable();
                this.mEnabledProviders.remove(str);
                this.mDisabledProviders.add(str);
            }
            updateProvidersLocked();
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void clearTestProviderEnabled(String str, String str2) {
        if (!canCallerAccessMockLocation(str2)) {
            return;
        }
        synchronized (this.mLock) {
            if (this.mMockProviders.get(str) == null) {
                throw new IllegalArgumentException("Provider \"" + str + "\" unknown");
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            this.mEnabledProviders.remove(str);
            this.mDisabledProviders.remove(str);
            updateProvidersLocked();
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void setTestProviderStatus(String str, int i, Bundle bundle, long j, String str2) {
        if (!canCallerAccessMockLocation(str2)) {
            return;
        }
        synchronized (this.mLock) {
            MockProvider mockProvider = this.mMockProviders.get(str);
            if (mockProvider == null) {
                throw new IllegalArgumentException("Provider \"" + str + "\" unknown");
            }
            mockProvider.setStatus(i, bundle, j);
        }
    }

    public void clearTestProviderStatus(String str, String str2) {
        if (!canCallerAccessMockLocation(str2)) {
            return;
        }
        synchronized (this.mLock) {
            MockProvider mockProvider = this.mMockProviders.get(str);
            if (mockProvider == null) {
                throw new IllegalArgumentException("Provider \"" + str + "\" unknown");
            }
            mockProvider.clearStatus();
        }
    }

    private void log(String str) {
        if (Log.isLoggable(TAG, 2)) {
            Slog.d(TAG, str);
        }
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
            synchronized (this.mLock) {
                if (strArr.length > 0 && strArr[0].equals("--gnssmetrics")) {
                    if (this.mGnssMetricsProvider != null) {
                        printWriter.append((CharSequence) this.mGnssMetricsProvider.getGnssMetricsAsProtoString());
                    }
                    return;
                }
                printWriter.println("Current Location Manager state:");
                printWriter.println("  Location Listeners:");
                Iterator<Receiver> it = this.mReceivers.values().iterator();
                while (it.hasNext()) {
                    printWriter.println("    " + it.next());
                }
                printWriter.println("  Active Records by Provider:");
                for (Map.Entry<String, ArrayList<UpdateRecord>> entry : this.mRecordsByProvider.entrySet()) {
                    printWriter.println("    " + entry.getKey() + ":");
                    Iterator<UpdateRecord> it2 = entry.getValue().iterator();
                    while (it2.hasNext()) {
                        printWriter.println("      " + it2.next());
                    }
                }
                printWriter.println("  Active GnssMeasurement Listeners:");
                for (Identity identity : this.mGnssMeasurementsListeners.values()) {
                    printWriter.println("    " + identity.mPid + " " + identity.mUid + " " + identity.mPackageName + ": " + isThrottlingExemptLocked(identity));
                }
                printWriter.println("  Active GnssNavigationMessage Listeners:");
                for (Identity identity2 : this.mGnssNavigationMessageListeners.values()) {
                    printWriter.println("    " + identity2.mPid + " " + identity2.mUid + " " + identity2.mPackageName + ": " + isThrottlingExemptLocked(identity2));
                }
                printWriter.println("  Overlay Provider Packages:");
                for (LocationProviderInterface locationProviderInterface : this.mProviders) {
                    if (locationProviderInterface instanceof LocationProviderProxy) {
                        printWriter.println("    " + locationProviderInterface.getName() + ": " + ((LocationProviderProxy) locationProviderInterface).getConnectedPackageName());
                    }
                }
                printWriter.println("  Historical Records by Provider:");
                for (Map.Entry<LocationRequestStatistics.PackageProviderKey, LocationRequestStatistics.PackageStatistics> entry2 : this.mRequestStatistics.statistics.entrySet()) {
                    LocationRequestStatistics.PackageProviderKey key = entry2.getKey();
                    printWriter.println("    " + key.packageName + ": " + key.providerName + ": " + entry2.getValue());
                }
                printWriter.println("  Last Known Locations:");
                for (Map.Entry<String, Location> entry3 : this.mLastLocation.entrySet()) {
                    printWriter.println("    " + entry3.getKey() + ": " + entry3.getValue());
                }
                printWriter.println("  Last Known Locations Coarse Intervals:");
                for (Map.Entry<String, Location> entry4 : this.mLastLocationCoarseInterval.entrySet()) {
                    printWriter.println("    " + entry4.getKey() + ": " + entry4.getValue());
                }
                this.mGeofenceManager.dump(printWriter);
                if (this.mEnabledProviders.size() > 0) {
                    printWriter.println("  Enabled Providers:");
                    Iterator<String> it3 = this.mEnabledProviders.iterator();
                    while (it3.hasNext()) {
                        printWriter.println("    " + it3.next());
                    }
                }
                if (this.mDisabledProviders.size() > 0) {
                    printWriter.println("  Disabled Providers:");
                    Iterator<String> it4 = this.mDisabledProviders.iterator();
                    while (it4.hasNext()) {
                        printWriter.println("    " + it4.next());
                    }
                }
                printWriter.append("  ");
                this.mBlacklist.dump(printWriter);
                if (this.mMockProviders.size() > 0) {
                    printWriter.println("  Mock Providers:");
                    Iterator<Map.Entry<String, MockProvider>> it5 = this.mMockProviders.entrySet().iterator();
                    while (it5.hasNext()) {
                        it5.next().getValue().dump(printWriter, "      ");
                    }
                }
                if (!this.mBackgroundThrottlePackageWhitelist.isEmpty()) {
                    printWriter.println("  Throttling Whitelisted Packages:");
                    Iterator<String> it6 = this.mBackgroundThrottlePackageWhitelist.iterator();
                    while (it6.hasNext()) {
                        printWriter.println("    " + it6.next());
                    }
                }
                printWriter.append("  fudger: ");
                this.mLocationFudger.dump(fileDescriptor, printWriter, strArr);
                if (strArr.length <= 0 || !"short".equals(strArr[0])) {
                    for (LocationProviderInterface locationProviderInterface2 : this.mProviders) {
                        printWriter.print(locationProviderInterface2.getName() + " Internal State");
                        if (locationProviderInterface2 instanceof LocationProviderProxy) {
                            printWriter.print(" (" + ((LocationProviderProxy) locationProviderInterface2).getConnectedPackageName() + ")");
                        }
                        printWriter.println(":");
                        locationProviderInterface2.dump(fileDescriptor, printWriter, strArr);
                    }
                    if (this.mGnssBatchingInProgress) {
                        printWriter.println("  GNSS batching in progress");
                    }
                }
            }
        }
    }

    private void initMtkLocationManagerService() {
        Constructor<?> constructor;
        if (SystemProperties.get("ro.vendor.mtk_gps_support").equals("1")) {
            this.mHandlerThread = new HandlerThread("LocationManagerServiceThread");
            this.mHandlerThread.start();
            this.mLocationHandler = new LocationWorkerHandler(this.mHandlerThread.getLooper());
            try {
                this.mMtkLocationManagerServiceClass = Class.forName("com.mediatek.location.MtkLocationExt$LocationManagerService");
                if (D) {
                    Log.d(TAG, "class = " + this.mMtkLocationManagerServiceClass);
                }
                if (this.mMtkLocationManagerServiceClass != null && (constructor = this.mMtkLocationManagerServiceClass.getConstructor(Context.class, Handler.class)) != null) {
                    this.mMtkLocationManagerService = constructor.newInstance(this.mContext, this.mLocationHandler);
                }
                Log.d(TAG, "mMtkLocationManagerService = " + this.mMtkLocationManagerService);
                this.mCtaSupported = checkCtaSuport();
            } catch (Exception e) {
                Log.w(TAG, "Failed to init mMtkLocationManagerService!");
            }
            this.mWhitelistPackages.add("com.mediatek.ims");
            this.mWhitelistPackages.add("com.mediatek.location.lppe.main");
            this.mWhitelistPackages.add("com.mediatek.wfo.impl");
            this.mWhitelistProviders.put("gps", new Boolean(false));
            this.mWhitelistProviders.put("network", new Boolean(false));
        }
    }

    private boolean isPackageInWhitelist(String str) {
        if (this.mMtkLocationManagerService != null) {
            for (String str2 : this.mWhitelistPackages) {
                if (str2 != null && str2.equals(str)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private boolean isProviderInWhitelist(String str) {
        return this.mWhitelistProviders.containsKey(str);
    }

    private boolean mtkWhitelistRequestLocationUpdates(String str, String str2, LocationProviderInterface locationProviderInterface) {
        if (isPackageInWhitelist(str2) && isProviderInWhitelist(str) && isDeviceProvisioned()) {
            Log.d(TAG, "Update DB when first whitelist package requested: " + str2);
            setWhitelistWorkingMode(str, true);
            return true;
        }
        Log.d(TAG, "RequestLocationUpdates to disabled provider: " + str);
        return false;
    }

    private boolean isDeviceProvisioned() {
        int i = Settings.Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0);
        Log.d(TAG, "isDeviceProvisioned = " + i);
        if (i != 1) {
            return false;
        }
        return true;
    }

    private void setWhitelistWorkingMode(String str, boolean z) {
        if (this.mMtkLocationManagerService != null && this.mWhitelistProviders.containsKey(str)) {
            if (this.mWhitelistProviders.get(str).booleanValue() != z) {
                this.mWhitelistProviders.remove(str);
                this.mWhitelistProviders.put(str, new Boolean(z));
                Log.d(TAG, "setWhitelistWorkingMode provider: " + str + " as " + z);
            }
            ContentResolver contentResolver = this.mContext.getContentResolver();
            StringBuilder sb = new StringBuilder();
            sb.append(z ? "+" : "-");
            sb.append(str);
            Settings.Secure.putString(contentResolver, "location_providers_allowed", sb.toString());
            Log.d(TAG, "setWhitelistWorkingMode: update DB: " + z + " for " + str);
        }
    }

    private boolean isWhitelistWorkingMode(String str) {
        if (this.mMtkLocationManagerService != null && this.mWhitelistProviders.containsKey(str)) {
            return this.mWhitelistProviders.get(str).booleanValue();
        }
        return false;
    }

    private boolean isProviderRecordsContainsWhitelistPackage(String str, boolean z) {
        ArrayList<UpdateRecord> arrayList;
        boolean z2 = true;
        if (this.mMtkLocationManagerService != null && (arrayList = this.mRecordsByProvider.get(str)) != null) {
            for (UpdateRecord updateRecord : arrayList) {
                if (isPackageInWhitelist(updateRecord.mReceiver.mIdentity.mPackageName)) {
                    Log.d(TAG, "isProviderRecordsContainsWhitelistPackage contains package:  " + updateRecord.mReceiver.mIdentity.mPackageName);
                    break;
                }
                if (z && "com.google.android.gms".equals(updateRecord.mReceiver.mIdentity.mPackageName)) {
                    Log.d(TAG, "isProviderRecordsContainsWhitelistPackage contains ELS package:  " + updateRecord.mReceiver.mIdentity.mPackageName);
                    break;
                }
            }
            z2 = false;
        } else {
            z2 = false;
        }
        if (!z2) {
            Log.d(TAG, "isProviderRecordsContainsWhitelistPackage not found of: " + str);
        }
        return z2;
    }

    private void disableProviderWhenNoWhitelistPackageRegistered() {
        if ((isWhitelistWorkingMode("network") || isWhitelistWorkingMode("gps")) && !isProviderRecordsContainsWhitelistPackage("gps", true) && !isProviderRecordsContainsWhitelistPackage("network", true)) {
            if (isWhitelistWorkingMode("network")) {
                setWhitelistWorkingMode("network", false);
            }
            if (isWhitelistWorkingMode("gps")) {
                setWhitelistWorkingMode("gps", false);
            }
        }
    }

    private boolean isCtaSupported() {
        return this.mCtaSupported;
    }

    private boolean checkCtaSuport() {
        Boolean bool;
        try {
            if (this.mMtkLocationManagerService != null) {
                bool = (Boolean) this.mMtkLocationManagerServiceClass.getMethod("isCtaFeatureSupport", new Class[0]).invoke(this.mMtkLocationManagerService, new Object[0]);
                try {
                    Log.d(TAG, "checkCtaSupport = " + bool);
                } catch (Exception e) {
                    Log.w(TAG, "Failed to call isCtaFeatureSupport!");
                }
            } else {
                bool = false;
            }
        } catch (Exception e2) {
            bool = false;
        }
        return bool.booleanValue();
    }

    private boolean mtkCheckCtaOp(int i, int i2, int i3) {
        if (!isCtaSupported()) {
            return true;
        }
        String[] packagesForUid = this.mContext.getPackageManager().getPackagesForUid(i2);
        if (packagesForUid == null) {
            Log.d(TAG, "checkOp(pid = " + i + ", uid = " + i2 + ") pkg == null, return false");
            return false;
        }
        for (String str : packagesForUid) {
            if (this.mAppOps.checkOpNoThrow(i3, i2, str) == 0) {
                return true;
            }
        }
        return false;
    }

    private boolean mtkNoteOpForCta(int i, int i2, int i3, String str, int i4) {
        if (!isCtaSupported()) {
            return false;
        }
        if (i == 1) {
            if (this.mAppOps.noteOpNoThrow(0, i3, str) == 0) {
                return true;
            }
            Log.d(TAG, "noteOpNoThrow(op = OP_COARSE_LOCATION, uid = " + i3 + ", pkg = " + str + ") != ALLOWED");
            return false;
        }
        Log.d(TAG, "noteOpNoThrow(op = OP_COARSE_LOCATION) returns false");
        return false;
    }

    private boolean mtkCheckOpForCta(int i, int i2, int i3, String str, int i4) {
        if (!isCtaSupported()) {
            return false;
        }
        if (i == 1) {
            if (this.mAppOps.checkOp(0, i3, str) == 0) {
                return true;
            }
            Log.d(TAG, "checkOp(op = OP_COARSE_LOCATION , uid = " + i3 + ", pkg = " + str + ") != ALLOWED");
            return false;
        }
        Log.d(TAG, "checkOp(op = OP_COARSE_LOCATION) returns false");
        return false;
    }
}
