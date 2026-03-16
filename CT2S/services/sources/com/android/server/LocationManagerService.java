package com.android.server;

import android.R;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.location.ActivityRecognitionHardware;
import android.location.Address;
import android.location.Criteria;
import android.location.GeocoderParams;
import android.location.Geofence;
import android.location.IGpsMeasurementsListener;
import android.location.IGpsNavigationMessageListener;
import android.location.IGpsStatusListener;
import android.location.IGpsStatusProvider;
import android.location.ILocationListener;
import android.location.ILocationManager;
import android.location.INetInitiatedListener;
import android.location.Location;
import android.location.LocationProvider;
import android.location.LocationRequest;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.WorkSource;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import com.android.internal.content.PackageMonitor;
import com.android.internal.location.ProviderProperties;
import com.android.internal.location.ProviderRequest;
import com.android.internal.os.BackgroundThread;
import com.android.server.hdmi.HdmiCecKeycode;
import com.android.server.location.ActivityRecognitionProxy;
import com.android.server.location.FlpHardwareProvider;
import com.android.server.location.FusedProxy;
import com.android.server.location.GeocoderProxy;
import com.android.server.location.GeofenceManager;
import com.android.server.location.GeofenceProxy;
import com.android.server.location.GpsLocationProvider;
import com.android.server.location.GpsMeasurementsProvider;
import com.android.server.location.GpsNavigationMessageProvider;
import com.android.server.location.LocationBlacklist;
import com.android.server.location.LocationFudger;
import com.android.server.location.LocationProviderInterface;
import com.android.server.location.LocationProviderProxy;
import com.android.server.location.LocationRequestStatistics;
import com.android.server.location.MockProvider;
import com.android.server.location.PassiveProvider;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LocationManagerService extends ILocationManager.Stub {
    private static final String ACCESS_LOCATION_EXTRA_COMMANDS = "android.permission.ACCESS_LOCATION_EXTRA_COMMANDS";
    private static final String ACCESS_MOCK_LOCATION = "android.permission.ACCESS_MOCK_LOCATION";
    public static final boolean D = Log.isLoggable("LocationManagerService", 3);
    private static final LocationRequest DEFAULT_LOCATION_REQUEST = new LocationRequest();
    private static final String FUSED_LOCATION_SERVICE_ACTION = "com.android.location.service.FusedLocationProvider";
    private static final long HIGH_POWER_INTERVAL_MS = 300000;
    private static final String INSTALL_LOCATION_PROVIDER = "android.permission.INSTALL_LOCATION_PROVIDER";
    private static final int MAX_PROVIDER_SCHEDULING_JITTER_MS = 100;
    private static final int MSG_LOCATION_CHANGED = 1;
    private static final long NANOS_PER_MILLI = 1000000;
    private static final String NETWORK_LOCATION_SERVICE_ACTION = "com.android.location.service.v3.NetworkLocationProvider";
    private static final int RESOLUTION_LEVEL_COARSE = 1;
    private static final int RESOLUTION_LEVEL_FINE = 2;
    private static final int RESOLUTION_LEVEL_NONE = 0;
    private static final String TAG = "LocationManagerService";
    private static final String WAKELOCK_KEY = "LocationManagerService";
    private final AppOpsManager mAppOps;
    private LocationBlacklist mBlacklist;
    private final Context mContext;
    private GeocoderProxy mGeocodeProvider;
    private GeofenceManager mGeofenceManager;
    private GpsMeasurementsProvider mGpsMeasurementsProvider;
    private GpsNavigationMessageProvider mGpsNavigationMessageProvider;
    private IGpsStatusProvider mGpsStatusProvider;
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
    private int mCurrentUserId = 0;
    private int[] mCurrentUserProfiles = {0};
    private final PackageMonitor mPackageMonitor = new PackageMonitor() {
        public void onPackageDisappeared(String packageName, int reason) throws Throwable {
            ArrayList<Receiver> deadReceivers;
            synchronized (LocationManagerService.this.mLock) {
                ArrayList<Receiver> deadReceivers2 = null;
                try {
                    Iterator i$ = LocationManagerService.this.mReceivers.values().iterator();
                    while (true) {
                        try {
                            deadReceivers = deadReceivers2;
                            if (!i$.hasNext()) {
                                break;
                            }
                            Receiver receiver = (Receiver) i$.next();
                            if (receiver.mPackageName.equals(packageName)) {
                                deadReceivers2 = deadReceivers == null ? new ArrayList<>() : deadReceivers;
                                deadReceivers2.add(receiver);
                            } else {
                                deadReceivers2 = deadReceivers;
                            }
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    }
                    if (deadReceivers != null) {
                        Iterator<Receiver> it = deadReceivers.iterator();
                        while (it.hasNext()) {
                            LocationManagerService.this.removeUpdatesLocked(it.next());
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        }
    };

    public LocationManagerService(Context context) {
        this.mContext = context;
        this.mAppOps = (AppOpsManager) context.getSystemService("appops");
        if (D) {
            Log.d("LocationManagerService", "Constructed");
        }
    }

    public void systemRunning() {
        synchronized (this.mLock) {
            if (D) {
                Log.d("LocationManagerService", "systemReady()");
            }
            this.mPackageManager = this.mContext.getPackageManager();
            this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
            this.mLocationHandler = new LocationWorkerHandler(BackgroundThread.get().getLooper());
            this.mLocationFudger = new LocationFudger(this.mContext, this.mLocationHandler);
            this.mBlacklist = new LocationBlacklist(this.mContext, this.mLocationHandler);
            this.mBlacklist.init();
            this.mGeofenceManager = new GeofenceManager(this.mContext, this.mBlacklist);
            AppOpsManager.OnOpChangedListener callback = new AppOpsManager.OnOpChangedInternalListener() {
                public void onOpChanged(int op, String packageName) {
                    synchronized (LocationManagerService.this.mLock) {
                        for (Receiver receiver : LocationManagerService.this.mReceivers.values()) {
                            receiver.updateMonitoring(true);
                        }
                        LocationManagerService.this.applyAllProviderRequirementsLocked();
                    }
                }
            };
            this.mAppOps.startWatchingMode(0, (String) null, callback);
            this.mUserManager = (UserManager) this.mContext.getSystemService("user");
            updateUserProfiles(this.mCurrentUserId);
            loadProvidersLocked();
            updateProvidersLocked();
        }
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("location_providers_allowed"), true, new ContentObserver(this.mLocationHandler) {
            @Override
            public void onChange(boolean selfChange) {
                synchronized (LocationManagerService.this.mLock) {
                    LocationManagerService.this.updateProvidersLocked();
                }
            }
        }, -1);
        this.mPackageMonitor.register(this.mContext, this.mLocationHandler.getLooper(), true);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        intentFilter.addAction("android.intent.action.MANAGED_PROFILE_ADDED");
        intentFilter.addAction("android.intent.action.MANAGED_PROFILE_REMOVED");
        this.mContext.registerReceiverAsUser(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("android.intent.action.USER_SWITCHED".equals(action)) {
                    LocationManagerService.this.switchUser(intent.getIntExtra("android.intent.extra.user_handle", 0));
                } else if ("android.intent.action.MANAGED_PROFILE_ADDED".equals(action) || "android.intent.action.MANAGED_PROFILE_REMOVED".equals(action)) {
                    LocationManagerService.this.updateUserProfiles(LocationManagerService.this.mCurrentUserId);
                }
            }
        }, UserHandle.ALL, intentFilter, null, this.mLocationHandler);
    }

    void updateUserProfiles(int currentUserId) {
        List<UserInfo> profiles = this.mUserManager.getProfiles(currentUserId);
        synchronized (this.mLock) {
            this.mCurrentUserProfiles = new int[profiles.size()];
            for (int i = 0; i < this.mCurrentUserProfiles.length; i++) {
                this.mCurrentUserProfiles[i] = profiles.get(i).id;
            }
        }
    }

    private boolean isCurrentProfile(int userId) {
        boolean z;
        synchronized (this.mLock) {
            int i = 0;
            while (true) {
                if (i < this.mCurrentUserProfiles.length) {
                    if (this.mCurrentUserProfiles[i] != userId) {
                        i++;
                    } else {
                        z = true;
                        break;
                    }
                } else {
                    z = false;
                    break;
                }
            }
        }
        return z;
    }

    private void ensureFallbackFusedProviderPresentLocked(ArrayList<String> pkgs) {
        PackageManager pm = this.mContext.getPackageManager();
        String systemPackageName = this.mContext.getPackageName();
        ArrayList<HashSet<Signature>> sigSets = ServiceWatcher.getSignatureSets(this.mContext, pkgs);
        List<ResolveInfo> rInfos = pm.queryIntentServicesAsUser(new Intent(FUSED_LOCATION_SERVICE_ACTION), 128, this.mCurrentUserId);
        for (ResolveInfo rInfo : rInfos) {
            String packageName = rInfo.serviceInfo.packageName;
            try {
                PackageInfo pInfo = pm.getPackageInfo(packageName, 64);
                if (!ServiceWatcher.isSignatureMatch(pInfo.signatures, sigSets)) {
                    Log.w("LocationManagerService", packageName + " resolves service " + FUSED_LOCATION_SERVICE_ACTION + ", but has wrong signature, ignoring");
                } else if (rInfo.serviceInfo.metaData == null) {
                    Log.w("LocationManagerService", "Found fused provider without metadata: " + packageName);
                } else {
                    int version = rInfo.serviceInfo.metaData.getInt(ServiceWatcher.EXTRA_SERVICE_VERSION, -1);
                    if (version == 0) {
                        if ((rInfo.serviceInfo.applicationInfo.flags & 1) == 0) {
                            if (D) {
                                Log.d("LocationManagerService", "Fallback candidate not in /system: " + packageName);
                            }
                        } else if (pm.checkSignatures(systemPackageName, packageName) != 0) {
                            if (D) {
                                Log.d("LocationManagerService", "Fallback candidate not signed the same as system: " + packageName);
                            }
                        } else {
                            if (D) {
                                Log.d("LocationManagerService", "Found fallback provider: " + packageName);
                                return;
                            }
                            return;
                        }
                    } else if (D) {
                        Log.d("LocationManagerService", "Fallback candidate not version 0: " + packageName);
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("LocationManagerService", "missing package: " + packageName);
            }
        }
        throw new IllegalStateException("Unable to find a fused location provider that is in the system partition with version 0 and signed with the platform certificate. Such a package is needed to provide a default fused location provider in the event that no other fused location provider has been installed or is currently available. For example, coreOnly boot mode when decrypting the data partition. The fallback must also be marked coreApp=\"true\" in the manifest");
    }

    private void loadProvidersLocked() {
        FlpHardwareProvider flpHardwareProvider;
        PassiveProvider passiveProvider = new PassiveProvider(this);
        addProviderLocked(passiveProvider);
        this.mEnabledProviders.add(passiveProvider.getName());
        this.mPassiveProvider = passiveProvider;
        GpsLocationProvider gpsProvider = new GpsLocationProvider(this.mContext, this, this.mLocationHandler.getLooper());
        if (GpsLocationProvider.isSupported()) {
            this.mGpsStatusProvider = gpsProvider.getGpsStatusProvider();
            this.mNetInitiatedListener = gpsProvider.getNetInitiatedListener();
            addProviderLocked(gpsProvider);
            this.mRealProviders.put("gps", gpsProvider);
        }
        this.mGpsMeasurementsProvider = gpsProvider.getGpsMeasurementsProvider();
        this.mGpsNavigationMessageProvider = gpsProvider.getGpsNavigationMessageProvider();
        Resources resources = this.mContext.getResources();
        ArrayList<String> providerPackageNames = new ArrayList<>();
        String[] pkgs = resources.getStringArray(R.array.config_biometric_protected_package_names);
        if (D) {
            Log.d("LocationManagerService", "certificates for location providers pulled from: " + Arrays.toString(pkgs));
        }
        if (pkgs != null) {
            providerPackageNames.addAll(Arrays.asList(pkgs));
        }
        ensureFallbackFusedProviderPresentLocked(providerPackageNames);
        LocationProviderProxy networkProvider = LocationProviderProxy.createAndBind(this.mContext, "network", NETWORK_LOCATION_SERVICE_ACTION, R.^attr-private.dropdownListPreferredItemHeight, R.string.config_defaultBrowser, R.array.config_biometric_protected_package_names, this.mLocationHandler);
        if (networkProvider != null) {
            this.mRealProviders.put("network", networkProvider);
            this.mProxyProviders.add(networkProvider);
            addProviderLocked(networkProvider);
        } else {
            Slog.w("LocationManagerService", "no network location provider found");
        }
        LocationProviderProxy fusedLocationProvider = LocationProviderProxy.createAndBind(this.mContext, "fused", FUSED_LOCATION_SERVICE_ACTION, R.^attr-private.emergencyInstaller, R.string.config_defaultDialer, R.array.config_biometric_protected_package_names, this.mLocationHandler);
        if (fusedLocationProvider != null) {
            addProviderLocked(fusedLocationProvider);
            this.mProxyProviders.add(fusedLocationProvider);
            this.mEnabledProviders.add(fusedLocationProvider.getName());
            this.mRealProviders.put("fused", fusedLocationProvider);
        } else {
            Slog.e("LocationManagerService", "no fused location provider found", new IllegalStateException("Location service needs a fused location provider"));
        }
        this.mGeocodeProvider = GeocoderProxy.createAndBind(this.mContext, R.^attr-private.enableControlView, R.string.config_defaultCallRedirection, R.array.config_biometric_protected_package_names, this.mLocationHandler);
        if (this.mGeocodeProvider == null) {
            Slog.e("LocationManagerService", "no geocoder provider found");
        }
        if (FlpHardwareProvider.isSupported()) {
            flpHardwareProvider = FlpHardwareProvider.getInstance(this.mContext);
            FusedProxy fusedProxy = FusedProxy.createAndBind(this.mContext, this.mLocationHandler, flpHardwareProvider.getLocationHardware(), R.^attr-private.emulated, R.string.config_defaultSms, R.array.config_biometric_protected_package_names);
            if (fusedProxy == null) {
                Slog.e("LocationManagerService", "Unable to bind FusedProxy.");
            }
        } else {
            flpHardwareProvider = null;
            Slog.e("LocationManagerService", "FLP HAL not supported");
        }
        GeofenceProxy provider = GeofenceProxy.createAndBind(this.mContext, R.^attr-private.enableSubtitle, R.string.config_defaultCallScreening, R.array.config_biometric_protected_package_names, this.mLocationHandler, gpsProvider.getGpsGeofenceProxy(), flpHardwareProvider != null ? flpHardwareProvider.getGeofenceHardware() : null);
        if (provider == null) {
            Slog.e("LocationManagerService", "Unable to bind FLP Geofence proxy.");
        }
        if (ActivityRecognitionHardware.isSupported()) {
            ActivityRecognitionProxy proxy = ActivityRecognitionProxy.createAndBind(this.mContext, this.mLocationHandler, ActivityRecognitionHardware.getInstance(this.mContext), R.^attr-private.enlargeVertexEntryArea, R.string.config_systemGallery, R.array.config_biometric_protected_package_names);
            if (proxy == null) {
                Slog.e("LocationManagerService", "Unable to bind ActivityRecognitionProxy.");
            }
        } else {
            Slog.e("LocationManagerService", "Hardware Activity-Recognition not supported.");
        }
        String[] testProviderStrings = resources.getStringArray(R.array.config_biometric_sensors);
        for (String testProviderString : testProviderStrings) {
            String[] fragments = testProviderString.split(",");
            String name = fragments[0].trim();
            if (this.mProvidersByName.get(name) != null) {
                throw new IllegalArgumentException("Provider \"" + name + "\" already exists");
            }
            ProviderProperties properties = new ProviderProperties(Boolean.parseBoolean(fragments[1]), Boolean.parseBoolean(fragments[2]), Boolean.parseBoolean(fragments[3]), Boolean.parseBoolean(fragments[4]), Boolean.parseBoolean(fragments[5]), Boolean.parseBoolean(fragments[6]), Boolean.parseBoolean(fragments[7]), Integer.parseInt(fragments[8]), Integer.parseInt(fragments[9]));
            addTestProviderLocked(name, properties);
        }
    }

    private void switchUser(int userId) {
        if (this.mCurrentUserId != userId) {
            this.mBlacklist.switchUser(userId);
            this.mLocationHandler.removeMessages(1);
            synchronized (this.mLock) {
                this.mLastLocation.clear();
                this.mLastLocationCoarseInterval.clear();
                for (LocationProviderInterface p : this.mProviders) {
                    updateProviderListenersLocked(p.getName(), false);
                }
                this.mCurrentUserId = userId;
                updateUserProfiles(userId);
                updateProvidersLocked();
            }
        }
    }

    private final class Receiver implements IBinder.DeathRecipient, PendingIntent.OnFinished {
        final int mAllowedResolutionLevel;
        final boolean mHideFromAppOps;
        final Object mKey;
        final ILocationListener mListener;
        boolean mOpHighPowerMonitoring;
        boolean mOpMonitoring;
        final String mPackageName;
        int mPendingBroadcasts;
        final PendingIntent mPendingIntent;
        final int mPid;
        final int mUid;
        final HashMap<String, UpdateRecord> mUpdateRecords = new HashMap<>();
        PowerManager.WakeLock mWakeLock;
        final WorkSource mWorkSource;

        Receiver(ILocationListener listener, PendingIntent intent, int pid, int uid, String packageName, WorkSource workSource, boolean hideFromAppOps) {
            this.mListener = listener;
            this.mPendingIntent = intent;
            if (listener != null) {
                this.mKey = listener.asBinder();
            } else {
                this.mKey = intent;
            }
            this.mAllowedResolutionLevel = LocationManagerService.this.getAllowedResolutionLevel(pid, uid);
            this.mUid = uid;
            this.mPid = pid;
            this.mPackageName = packageName;
            if (workSource != null && workSource.size() <= 0) {
                workSource = null;
            }
            this.mWorkSource = workSource;
            this.mHideFromAppOps = hideFromAppOps;
            updateMonitoring(true);
            this.mWakeLock = LocationManagerService.this.mPowerManager.newWakeLock(1, "LocationManagerService");
            this.mWakeLock.setWorkSource(workSource == null ? new WorkSource(this.mUid, this.mPackageName) : workSource);
        }

        public boolean equals(Object otherObj) {
            if (otherObj instanceof Receiver) {
                return this.mKey.equals(((Receiver) otherObj).mKey);
            }
            return false;
        }

        public int hashCode() {
            return this.mKey.hashCode();
        }

        public String toString() {
            StringBuilder s = new StringBuilder();
            s.append("Reciever[");
            s.append(Integer.toHexString(System.identityHashCode(this)));
            if (this.mListener != null) {
                s.append(" listener");
            } else {
                s.append(" intent");
            }
            for (String p : this.mUpdateRecords.keySet()) {
                s.append(" ").append(this.mUpdateRecords.get(p).toString());
            }
            s.append("]");
            return s.toString();
        }

        public void updateMonitoring(boolean allow) {
            if (!this.mHideFromAppOps) {
                boolean requestingLocation = false;
                boolean requestingHighPowerLocation = false;
                if (allow) {
                    Iterator<UpdateRecord> it = this.mUpdateRecords.values().iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        UpdateRecord updateRecord = it.next();
                        if (LocationManagerService.this.isAllowedByCurrentUserSettingsLocked(updateRecord.mProvider)) {
                            requestingLocation = true;
                            LocationProviderInterface locationProvider = (LocationProviderInterface) LocationManagerService.this.mProvidersByName.get(updateRecord.mProvider);
                            ProviderProperties properties = locationProvider != null ? locationProvider.getProperties() : null;
                            if (properties != null && properties.mPowerRequirement == 3 && updateRecord.mRequest.getInterval() < LocationManagerService.HIGH_POWER_INTERVAL_MS) {
                                requestingHighPowerLocation = true;
                                break;
                            }
                        }
                    }
                }
                this.mOpMonitoring = updateMonitoring(requestingLocation, this.mOpMonitoring, 41);
                boolean wasHighPowerMonitoring = this.mOpHighPowerMonitoring;
                this.mOpHighPowerMonitoring = updateMonitoring(requestingHighPowerLocation, this.mOpHighPowerMonitoring, 42);
                if (this.mOpHighPowerMonitoring != wasHighPowerMonitoring) {
                    Intent intent = new Intent("android.location.HIGH_POWER_REQUEST_CHANGE");
                    LocationManagerService.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
                }
            }
        }

        private boolean updateMonitoring(boolean allowMonitoring, boolean currentlyMonitoring, int op) {
            if (!currentlyMonitoring) {
                if (allowMonitoring) {
                    return LocationManagerService.this.mAppOps.startOpNoThrow(op, this.mUid, this.mPackageName) == 0;
                }
            } else if (!allowMonitoring || LocationManagerService.this.mAppOps.checkOpNoThrow(op, this.mUid, this.mPackageName) != 0) {
                LocationManagerService.this.mAppOps.finishOp(op, this.mUid, this.mPackageName);
                return false;
            }
            return currentlyMonitoring;
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

        public boolean callStatusChangedLocked(String provider, int status, Bundle extras) {
            if (this.mListener != null) {
                try {
                    synchronized (this) {
                        this.mListener.onStatusChanged(provider, status, extras);
                        incrementPendingBroadcastsLocked();
                    }
                } catch (RemoteException e) {
                    return false;
                }
            } else {
                Intent statusChanged = new Intent();
                statusChanged.putExtras(new Bundle(extras));
                statusChanged.putExtra("status", status);
                try {
                    synchronized (this) {
                        this.mPendingIntent.send(LocationManagerService.this.mContext, 0, statusChanged, this, LocationManagerService.this.mLocationHandler, LocationManagerService.this.getResolutionPermission(this.mAllowedResolutionLevel));
                        incrementPendingBroadcastsLocked();
                    }
                } catch (PendingIntent.CanceledException e2) {
                    return false;
                }
            }
            return true;
        }

        public boolean callLocationChangedLocked(Location location) {
            if (this.mListener != null) {
                try {
                    synchronized (this) {
                        this.mListener.onLocationChanged(new Location(location));
                        incrementPendingBroadcastsLocked();
                    }
                } catch (RemoteException e) {
                    return false;
                }
            } else {
                Intent locationChanged = new Intent();
                locationChanged.putExtra("location", new Location(location));
                try {
                    synchronized (this) {
                        this.mPendingIntent.send(LocationManagerService.this.mContext, 0, locationChanged, this, LocationManagerService.this.mLocationHandler, LocationManagerService.this.getResolutionPermission(this.mAllowedResolutionLevel));
                        incrementPendingBroadcastsLocked();
                    }
                } catch (PendingIntent.CanceledException e2) {
                    return false;
                }
            }
            return true;
        }

        public boolean callProviderEnabledLocked(String provider, boolean enabled) {
            updateMonitoring(true);
            if (this.mListener != null) {
                try {
                    synchronized (this) {
                        if (enabled) {
                            this.mListener.onProviderEnabled(provider);
                        } else {
                            this.mListener.onProviderDisabled(provider);
                        }
                        incrementPendingBroadcastsLocked();
                    }
                } catch (RemoteException e) {
                    return false;
                }
            } else {
                Intent providerIntent = new Intent();
                providerIntent.putExtra("providerEnabled", enabled);
                try {
                    synchronized (this) {
                        this.mPendingIntent.send(LocationManagerService.this.mContext, 0, providerIntent, this, LocationManagerService.this.mLocationHandler, LocationManagerService.this.getResolutionPermission(this.mAllowedResolutionLevel));
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
                Log.d("LocationManagerService", "Location listener died");
            }
            synchronized (LocationManagerService.this.mLock) {
                LocationManagerService.this.removeUpdatesLocked(this);
            }
            synchronized (this) {
                clearPendingBroadcastsLocked();
            }
        }

        @Override
        public void onSendFinished(PendingIntent pendingIntent, Intent intent, int resultCode, String resultData, Bundle resultExtras) {
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

    public void locationCallbackFinished(ILocationListener listener) {
        synchronized (this.mLock) {
            IBinder binder = listener.asBinder();
            Receiver receiver = this.mReceivers.get(binder);
            if (receiver != null) {
                synchronized (receiver) {
                    long identity = Binder.clearCallingIdentity();
                    receiver.decrementPendingBroadcastsLocked();
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }
    }

    private void addProviderLocked(LocationProviderInterface provider) {
        this.mProviders.add(provider);
        this.mProvidersByName.put(provider.getName(), provider);
    }

    private void removeProviderLocked(LocationProviderInterface provider) {
        provider.disable();
        this.mProviders.remove(provider);
        this.mProvidersByName.remove(provider.getName());
    }

    private boolean isAllowedByCurrentUserSettingsLocked(String provider) {
        if (this.mEnabledProviders.contains(provider)) {
            return true;
        }
        if (this.mDisabledProviders.contains(provider)) {
            return false;
        }
        ContentResolver resolver = this.mContext.getContentResolver();
        return Settings.Secure.isLocationProviderEnabledForUser(resolver, provider, this.mCurrentUserId);
    }

    private boolean isAllowedByUserSettingsLocked(String provider, int uid) {
        if (isCurrentProfile(UserHandle.getUserId(uid)) || isUidALocationProvider(uid)) {
            return isAllowedByCurrentUserSettingsLocked(provider);
        }
        return false;
    }

    private String getResolutionPermission(int resolutionLevel) {
        switch (resolutionLevel) {
            case 1:
                return "android.permission.ACCESS_COARSE_LOCATION";
            case 2:
                return "android.permission.ACCESS_FINE_LOCATION";
            default:
                return null;
        }
    }

    private int getAllowedResolutionLevel(int pid, int uid) {
        if (this.mContext.checkPermission("android.permission.ACCESS_FINE_LOCATION", pid, uid) == 0) {
            return 2;
        }
        if (this.mContext.checkPermission("android.permission.ACCESS_COARSE_LOCATION", pid, uid) == 0) {
            return 1;
        }
        return 0;
    }

    private int getCallerAllowedResolutionLevel() {
        return getAllowedResolutionLevel(Binder.getCallingPid(), Binder.getCallingUid());
    }

    private void checkResolutionLevelIsSufficientForGeofenceUse(int allowedResolutionLevel) {
        if (allowedResolutionLevel < 2) {
            throw new SecurityException("Geofence usage requires ACCESS_FINE_LOCATION permission");
        }
    }

    private int getMinimumResolutionLevelForProviderUse(String provider) {
        ProviderProperties properties;
        if ("gps".equals(provider) || "passive".equals(provider)) {
            return 2;
        }
        if ("network".equals(provider) || "fused".equals(provider)) {
            return 1;
        }
        LocationProviderInterface lp = this.mMockProviders.get(provider);
        if (lp == null || (properties = lp.getProperties()) == null || properties.mRequiresSatellite) {
            return 2;
        }
        return (properties.mRequiresNetwork || properties.mRequiresCell) ? 1 : 2;
    }

    private void checkResolutionLevelIsSufficientForProviderUse(int allowedResolutionLevel, String providerName) {
        int requiredResolutionLevel = getMinimumResolutionLevelForProviderUse(providerName);
        if (allowedResolutionLevel < requiredResolutionLevel) {
            switch (requiredResolutionLevel) {
                case 1:
                    throw new SecurityException("\"" + providerName + "\" location provider requires ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION permission.");
                case 2:
                    throw new SecurityException("\"" + providerName + "\" location provider requires ACCESS_FINE_LOCATION permission.");
                default:
                    throw new SecurityException("Insufficient permission for \"" + providerName + "\" location provider.");
            }
        }
    }

    private void checkDeviceStatsAllowed() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.UPDATE_DEVICE_STATS", null);
    }

    private void checkUpdateAppOpsAllowed() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.UPDATE_APP_OPS_STATS", null);
    }

    public static int resolutionLevelToOp(int allowedResolutionLevel) {
        if (allowedResolutionLevel != 0) {
            return allowedResolutionLevel == 1 ? 0 : 1;
        }
        return -1;
    }

    boolean reportLocationAccessNoThrow(int uid, String packageName, int allowedResolutionLevel) {
        int op = resolutionLevelToOp(allowedResolutionLevel);
        return op < 0 || this.mAppOps.noteOpNoThrow(op, uid, packageName) == 0;
    }

    boolean checkLocationAccess(int uid, String packageName, int allowedResolutionLevel) {
        int op = resolutionLevelToOp(allowedResolutionLevel);
        return op < 0 || this.mAppOps.checkOp(op, uid, packageName) == 0;
    }

    public List<String> getAllProviders() {
        ArrayList<String> out;
        synchronized (this.mLock) {
            out = new ArrayList<>(this.mProviders.size());
            for (LocationProviderInterface provider : this.mProviders) {
                String name = provider.getName();
                if (!"fused".equals(name)) {
                    out.add(name);
                }
            }
        }
        if (D) {
            Log.d("LocationManagerService", "getAllProviders()=" + out);
        }
        return out;
    }

    public List<String> getProviders(Criteria criteria, boolean enabledOnly) {
        ArrayList<String> out;
        int allowedResolutionLevel = getCallerAllowedResolutionLevel();
        int uid = Binder.getCallingUid();
        long identity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                out = new ArrayList<>(this.mProviders.size());
                for (LocationProviderInterface provider : this.mProviders) {
                    String name = provider.getName();
                    if (!"fused".equals(name) && allowedResolutionLevel >= getMinimumResolutionLevelForProviderUse(name) && (!enabledOnly || isAllowedByUserSettingsLocked(name, uid))) {
                        if (criteria == null || LocationProvider.propertiesMeetCriteria(name, provider.getProperties(), criteria)) {
                            out.add(name);
                        }
                    }
                }
            }
            Binder.restoreCallingIdentity(identity);
            if (D) {
                Log.d("LocationManagerService", "getProviders()=" + out);
            }
            return out;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
    }

    public String getBestProvider(Criteria criteria, boolean enabledOnly) {
        List<String> providers = getProviders(criteria, enabledOnly);
        if (!providers.isEmpty()) {
            String result = pickBest(providers);
            if (D) {
                Log.d("LocationManagerService", "getBestProvider(" + criteria + ", " + enabledOnly + ")=" + result);
            }
            return result;
        }
        List<String> providers2 = getProviders(null, enabledOnly);
        if (!providers2.isEmpty()) {
            String result2 = pickBest(providers2);
            if (D) {
                Log.d("LocationManagerService", "getBestProvider(" + criteria + ", " + enabledOnly + ")=" + result2);
            }
            return result2;
        }
        if (!D) {
            return null;
        }
        Log.d("LocationManagerService", "getBestProvider(" + criteria + ", " + enabledOnly + ")=" + ((String) null));
        return null;
    }

    private String pickBest(List<String> providers) {
        if (providers.contains("gps")) {
            return "gps";
        }
        if (providers.contains("network")) {
            return "network";
        }
        return providers.get(0);
    }

    public boolean providerMeetsCriteria(String provider, Criteria criteria) {
        LocationProviderInterface p = this.mProvidersByName.get(provider);
        if (p == null) {
            throw new IllegalArgumentException("provider=" + provider);
        }
        boolean result = LocationProvider.propertiesMeetCriteria(p.getName(), p.getProperties(), criteria);
        if (D) {
            Log.d("LocationManagerService", "providerMeetsCriteria(" + provider + ", " + criteria + ")=" + result);
        }
        return result;
    }

    private void updateProvidersLocked() {
        boolean changesMade = false;
        for (int i = this.mProviders.size() - 1; i >= 0; i--) {
            LocationProviderInterface p = this.mProviders.get(i);
            boolean isEnabled = p.isEnabled();
            String name = p.getName();
            boolean shouldBeEnabled = isAllowedByCurrentUserSettingsLocked(name);
            if (isEnabled && !shouldBeEnabled) {
                updateProviderListenersLocked(name, false);
                this.mLastLocation.clear();
                this.mLastLocationCoarseInterval.clear();
                changesMade = true;
            } else if (!isEnabled && shouldBeEnabled) {
                updateProviderListenersLocked(name, true);
                changesMade = true;
            }
        }
        if (changesMade) {
            this.mContext.sendBroadcastAsUser(new Intent("android.location.PROVIDERS_CHANGED"), UserHandle.ALL);
            this.mContext.sendBroadcastAsUser(new Intent("android.location.MODE_CHANGED"), UserHandle.ALL);
        }
    }

    private void updateProviderListenersLocked(String provider, boolean enabled) {
        int listeners = 0;
        LocationProviderInterface p = this.mProvidersByName.get(provider);
        if (p != null) {
            ArrayList<Receiver> deadReceivers = null;
            ArrayList<UpdateRecord> records = this.mRecordsByProvider.get(provider);
            if (records != null) {
                int N = records.size();
                for (int i = 0; i < N; i++) {
                    UpdateRecord record = records.get(i);
                    if (isCurrentProfile(UserHandle.getUserId(record.mReceiver.mUid))) {
                        if (!record.mReceiver.callProviderEnabledLocked(provider, enabled)) {
                            if (deadReceivers == null) {
                                deadReceivers = new ArrayList<>();
                            }
                            deadReceivers.add(record.mReceiver);
                        }
                        listeners++;
                    }
                }
            }
            if (deadReceivers != null) {
                for (int i2 = deadReceivers.size() - 1; i2 >= 0; i2--) {
                    removeUpdatesLocked(deadReceivers.get(i2));
                }
            }
            if (enabled) {
                p.enable();
                if (listeners > 0) {
                    applyRequirementsLocked(provider);
                    return;
                }
                return;
            }
            p.disable();
        }
    }

    private void applyRequirementsLocked(String provider) {
        LocationProviderInterface p = this.mProvidersByName.get(provider);
        if (p != null) {
            ArrayList<UpdateRecord> records = this.mRecordsByProvider.get(provider);
            WorkSource worksource = new WorkSource();
            ProviderRequest providerRequest = new ProviderRequest();
            if (records != null) {
                for (UpdateRecord record : records) {
                    if (isCurrentProfile(UserHandle.getUserId(record.mReceiver.mUid)) && checkLocationAccess(record.mReceiver.mUid, record.mReceiver.mPackageName, record.mReceiver.mAllowedResolutionLevel)) {
                        LocationRequest locationRequest = record.mRequest;
                        providerRequest.locationRequests.add(locationRequest);
                        if (locationRequest.getInterval() < providerRequest.interval) {
                            providerRequest.reportLocation = true;
                            providerRequest.interval = locationRequest.getInterval();
                        }
                    }
                }
                if (providerRequest.reportLocation) {
                    long thresholdInterval = ((providerRequest.interval + 1000) * 3) / 2;
                    for (UpdateRecord record2 : records) {
                        if (isCurrentProfile(UserHandle.getUserId(record2.mReceiver.mUid)) && record2.mRequest.getInterval() <= thresholdInterval) {
                            if (record2.mReceiver.mWorkSource != null && record2.mReceiver.mWorkSource.size() > 0 && record2.mReceiver.mWorkSource.getName(0) != null) {
                                worksource.add(record2.mReceiver.mWorkSource);
                            } else {
                                worksource.add(record2.mReceiver.mUid, record2.mReceiver.mPackageName);
                            }
                        }
                    }
                }
            }
            if (D) {
                Log.d("LocationManagerService", "provider request: " + provider + " " + providerRequest);
            }
            p.setRequest(providerRequest, worksource);
        }
    }

    private class UpdateRecord {
        Location mLastFixBroadcast;
        long mLastStatusBroadcast;
        final String mProvider;
        final Receiver mReceiver;
        final LocationRequest mRequest;

        UpdateRecord(String provider, LocationRequest request, Receiver receiver) {
            this.mProvider = provider;
            this.mRequest = request;
            this.mReceiver = receiver;
            ArrayList<UpdateRecord> records = (ArrayList) LocationManagerService.this.mRecordsByProvider.get(provider);
            if (records == null) {
                records = new ArrayList<>();
                LocationManagerService.this.mRecordsByProvider.put(provider, records);
            }
            if (!records.contains(this)) {
                records.add(this);
            }
            LocationManagerService.this.mRequestStatistics.startRequesting(this.mReceiver.mPackageName, provider, request.getInterval());
        }

        void disposeLocked(boolean removeReceiver) {
            HashMap<String, UpdateRecord> receiverRecords;
            LocationManagerService.this.mRequestStatistics.stopRequesting(this.mReceiver.mPackageName, this.mProvider);
            ArrayList<UpdateRecord> globalRecords = (ArrayList) LocationManagerService.this.mRecordsByProvider.get(this.mProvider);
            if (globalRecords != null) {
                globalRecords.remove(this);
            }
            if (removeReceiver && (receiverRecords = this.mReceiver.mUpdateRecords) != null) {
                receiverRecords.remove(this.mProvider);
                if (removeReceiver && receiverRecords.size() == 0) {
                    LocationManagerService.this.removeUpdatesLocked(this.mReceiver);
                }
            }
        }

        public String toString() {
            StringBuilder s = new StringBuilder();
            s.append("UpdateRecord[");
            s.append(this.mProvider);
            s.append(' ').append(this.mReceiver.mPackageName).append('(');
            s.append(this.mReceiver.mUid).append(')');
            s.append(' ').append(this.mRequest);
            s.append(']');
            return s.toString();
        }
    }

    private Receiver getReceiverLocked(ILocationListener listener, int pid, int uid, String packageName, WorkSource workSource, boolean hideFromAppOps) {
        IBinder binder = listener.asBinder();
        Receiver receiver = this.mReceivers.get(binder);
        if (receiver == null) {
            receiver = new Receiver(listener, null, pid, uid, packageName, workSource, hideFromAppOps);
            this.mReceivers.put(binder, receiver);
            try {
                receiver.getListener().asBinder().linkToDeath(receiver, 0);
            } catch (RemoteException e) {
                Slog.e("LocationManagerService", "linkToDeath failed:", e);
                return null;
            }
        }
        return receiver;
    }

    private Receiver getReceiverLocked(PendingIntent intent, int pid, int uid, String packageName, WorkSource workSource, boolean hideFromAppOps) {
        Receiver receiver = this.mReceivers.get(intent);
        if (receiver == null) {
            Receiver receiver2 = new Receiver(null, intent, pid, uid, packageName, workSource, hideFromAppOps);
            this.mReceivers.put(intent, receiver2);
            return receiver2;
        }
        return receiver;
    }

    private LocationRequest createSanitizedRequest(LocationRequest request, int resolutionLevel) {
        LocationRequest sanitizedRequest = new LocationRequest(request);
        if (resolutionLevel < 2) {
            switch (sanitizedRequest.getQuality()) {
                case 100:
                    sanitizedRequest.setQuality(HdmiCecKeycode.CEC_KEYCODE_RESTORE_VOLUME_FUNCTION);
                    break;
                case 203:
                    sanitizedRequest.setQuality(201);
                    break;
            }
            if (sanitizedRequest.getInterval() < LocationFudger.FASTEST_INTERVAL_MS) {
                sanitizedRequest.setInterval(LocationFudger.FASTEST_INTERVAL_MS);
            }
            if (sanitizedRequest.getFastestInterval() < LocationFudger.FASTEST_INTERVAL_MS) {
                sanitizedRequest.setFastestInterval(LocationFudger.FASTEST_INTERVAL_MS);
            }
        }
        if (sanitizedRequest.getFastestInterval() > sanitizedRequest.getInterval()) {
            request.setFastestInterval(request.getInterval());
        }
        return sanitizedRequest;
    }

    private void checkPackageName(String packageName) {
        if (packageName == null) {
            throw new SecurityException("invalid package name: " + packageName);
        }
        int uid = Binder.getCallingUid();
        String[] packages = this.mPackageManager.getPackagesForUid(uid);
        if (packages == null) {
            throw new SecurityException("invalid UID " + uid);
        }
        for (String pkg : packages) {
            if (packageName.equals(pkg)) {
                return;
            }
        }
        throw new SecurityException("invalid package name: " + packageName);
    }

    private void checkPendingIntent(PendingIntent intent) {
        if (intent == null) {
            throw new IllegalArgumentException("invalid pending intent: " + intent);
        }
    }

    private Receiver checkListenerOrIntentLocked(ILocationListener listener, PendingIntent intent, int pid, int uid, String packageName, WorkSource workSource, boolean hideFromAppOps) {
        if (intent == null && listener == null) {
            throw new IllegalArgumentException("need either listener or intent");
        }
        if (intent != null && listener != null) {
            throw new IllegalArgumentException("cannot register both listener and intent");
        }
        if (intent == null) {
            return getReceiverLocked(listener, pid, uid, packageName, workSource, hideFromAppOps);
        }
        checkPendingIntent(intent);
        return getReceiverLocked(intent, pid, uid, packageName, workSource, hideFromAppOps);
    }

    public void requestLocationUpdates(LocationRequest request, ILocationListener listener, PendingIntent intent, String packageName) {
        if (request == null) {
            request = DEFAULT_LOCATION_REQUEST;
        }
        checkPackageName(packageName);
        int allowedResolutionLevel = getCallerAllowedResolutionLevel();
        checkResolutionLevelIsSufficientForProviderUse(allowedResolutionLevel, request.getProvider());
        WorkSource workSource = request.getWorkSource();
        if (workSource != null && workSource.size() > 0) {
            checkDeviceStatsAllowed();
        }
        boolean hideFromAppOps = request.getHideFromAppOps();
        if (hideFromAppOps) {
            checkUpdateAppOpsAllowed();
        }
        LocationRequest sanitizedRequest = createSanitizedRequest(request, allowedResolutionLevel);
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        long identity = Binder.clearCallingIdentity();
        try {
            checkLocationAccess(uid, packageName, allowedResolutionLevel);
            synchronized (this.mLock) {
                Receiver recevier = checkListenerOrIntentLocked(listener, intent, pid, uid, packageName, workSource, hideFromAppOps);
                requestLocationUpdatesLocked(sanitizedRequest, recevier, pid, uid, packageName);
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void requestLocationUpdatesLocked(LocationRequest request, Receiver receiver, int pid, int uid, String packageName) {
        if (request == null) {
            request = DEFAULT_LOCATION_REQUEST;
        }
        String name = request.getProvider();
        if (name == null) {
            throw new IllegalArgumentException("provider name must not be null");
        }
        if (D) {
            Log.d("LocationManagerService", "request " + Integer.toHexString(System.identityHashCode(receiver)) + " " + name + " " + request + " from " + packageName + "(" + uid + ")");
        }
        LocationProviderInterface provider = this.mProvidersByName.get(name);
        if (provider == null) {
            Log.d("LocationManagerService", "provider doesn't exist: " + name);
            return;
        }
        UpdateRecord record = new UpdateRecord(name, request, receiver);
        UpdateRecord oldRecord = receiver.mUpdateRecords.put(name, record);
        if (oldRecord != null) {
            oldRecord.disposeLocked(false);
        }
        boolean isProviderEnabled = isAllowedByUserSettingsLocked(name, uid);
        if (isProviderEnabled) {
            applyRequirementsLocked(name);
        } else {
            receiver.callProviderEnabledLocked(name, false);
        }
        receiver.updateMonitoring(true);
    }

    public void removeUpdates(ILocationListener listener, PendingIntent intent, String packageName) {
        checkPackageName(packageName);
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        synchronized (this.mLock) {
            Receiver receiver = checkListenerOrIntentLocked(listener, intent, pid, uid, packageName, null, false);
            long identity = Binder.clearCallingIdentity();
            try {
                removeUpdatesLocked(receiver);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    private void removeUpdatesLocked(Receiver receiver) {
        if (D) {
            Log.i("LocationManagerService", "remove " + Integer.toHexString(System.identityHashCode(receiver)));
        }
        if (this.mReceivers.remove(receiver.mKey) != null && receiver.isListener()) {
            receiver.getListener().asBinder().unlinkToDeath(receiver, 0);
            synchronized (receiver) {
                receiver.clearPendingBroadcastsLocked();
            }
        }
        receiver.updateMonitoring(false);
        HashSet<String> providers = new HashSet<>();
        HashMap<String, UpdateRecord> oldRecords = receiver.mUpdateRecords;
        if (oldRecords != null) {
            for (UpdateRecord record : oldRecords.values()) {
                record.disposeLocked(false);
            }
            providers.addAll(oldRecords.keySet());
        }
        for (String provider : providers) {
            if (isAllowedByCurrentUserSettingsLocked(provider)) {
                applyRequirementsLocked(provider);
            }
        }
    }

    private void applyAllProviderRequirementsLocked() {
        for (LocationProviderInterface p : this.mProviders) {
            if (isAllowedByCurrentUserSettingsLocked(p.getName())) {
                applyRequirementsLocked(p.getName());
            }
        }
    }

    public Location getLastLocation(LocationRequest request, String packageName) {
        Location location = null;
        if (D) {
            Log.d("LocationManagerService", "getLastLocation: " + request);
        }
        if (request == null) {
            request = DEFAULT_LOCATION_REQUEST;
        }
        int allowedResolutionLevel = getCallerAllowedResolutionLevel();
        checkPackageName(packageName);
        checkResolutionLevelIsSufficientForProviderUse(allowedResolutionLevel, request.getProvider());
        int uid = Binder.getCallingUid();
        long identity = Binder.clearCallingIdentity();
        try {
            if (this.mBlacklist.isBlacklisted(packageName)) {
                if (D) {
                    Log.d("LocationManagerService", "not returning last loc for blacklisted app: " + packageName);
                }
            } else if (reportLocationAccessNoThrow(uid, packageName, allowedResolutionLevel)) {
                synchronized (this.mLock) {
                    String name = request.getProvider();
                    if (name == null) {
                        name = "fused";
                    }
                    LocationProviderInterface provider = this.mProvidersByName.get(name);
                    if (provider != null) {
                        if (isAllowedByUserSettingsLocked(name, uid)) {
                            Location location2 = allowedResolutionLevel < 2 ? this.mLastLocationCoarseInterval.get(name) : this.mLastLocation.get(name);
                            if (location2 != null) {
                                if (allowedResolutionLevel < 2) {
                                    Location noGPSLocation = location2.getExtraLocation("noGPSLocation");
                                    if (noGPSLocation != null) {
                                        location = new Location(this.mLocationFudger.getOrCreate(noGPSLocation));
                                    }
                                } else {
                                    location = new Location(location2);
                                }
                            }
                        }
                    }
                }
            } else if (D) {
                Log.d("LocationManagerService", "not returning last loc for no op app: " + packageName);
            }
            return location;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void requestGeofence(LocationRequest request, Geofence geofence, PendingIntent intent, String packageName) {
        if (request == null) {
            request = DEFAULT_LOCATION_REQUEST;
        }
        int allowedResolutionLevel = getCallerAllowedResolutionLevel();
        checkResolutionLevelIsSufficientForGeofenceUse(allowedResolutionLevel);
        checkPendingIntent(intent);
        checkPackageName(packageName);
        checkResolutionLevelIsSufficientForProviderUse(allowedResolutionLevel, request.getProvider());
        LocationRequest sanitizedRequest = createSanitizedRequest(request, allowedResolutionLevel);
        if (D) {
            Log.d("LocationManagerService", "requestGeofence: " + sanitizedRequest + " " + geofence + " " + intent);
        }
        int uid = Binder.getCallingUid();
        if (UserHandle.getUserId(uid) != 0) {
            Log.w("LocationManagerService", "proximity alerts are currently available only to the primary user");
            return;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            this.mGeofenceManager.addFence(sanitizedRequest, geofence, intent, allowedResolutionLevel, uid, packageName);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void removeGeofence(Geofence geofence, PendingIntent intent, String packageName) {
        checkResolutionLevelIsSufficientForGeofenceUse(getCallerAllowedResolutionLevel());
        checkPendingIntent(intent);
        checkPackageName(packageName);
        if (D) {
            Log.d("LocationManagerService", "removeGeofence: " + geofence + " " + intent);
        }
        long identity = Binder.clearCallingIdentity();
        try {
            this.mGeofenceManager.removeFence(geofence, intent);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public boolean addGpsStatusListener(IGpsStatusListener listener, String packageName) {
        int allowedResolutionLevel = getCallerAllowedResolutionLevel();
        checkResolutionLevelIsSufficientForProviderUse(allowedResolutionLevel, "gps");
        int uid = Binder.getCallingUid();
        long ident = Binder.clearCallingIdentity();
        try {
            if (!checkLocationAccess(uid, packageName, allowedResolutionLevel)) {
                return false;
            }
            Binder.restoreCallingIdentity(ident);
            if (this.mGpsStatusProvider == null) {
                return false;
            }
            try {
                this.mGpsStatusProvider.addGpsStatusListener(listener);
                return true;
            } catch (RemoteException e) {
                Slog.e("LocationManagerService", "mGpsStatusProvider.addGpsStatusListener failed", e);
                return false;
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void removeGpsStatusListener(IGpsStatusListener listener) {
        synchronized (this.mLock) {
            try {
                this.mGpsStatusProvider.removeGpsStatusListener(listener);
            } catch (Exception e) {
                Slog.e("LocationManagerService", "mGpsStatusProvider.removeGpsStatusListener failed", e);
            }
        }
    }

    public boolean addGpsMeasurementsListener(IGpsMeasurementsListener listener, String packageName) {
        int allowedResolutionLevel = getCallerAllowedResolutionLevel();
        checkResolutionLevelIsSufficientForProviderUse(allowedResolutionLevel, "gps");
        int uid = Binder.getCallingUid();
        long identity = Binder.clearCallingIdentity();
        try {
            boolean hasLocationAccess = checkLocationAccess(uid, packageName, allowedResolutionLevel);
            if (!hasLocationAccess) {
                return false;
            }
            return this.mGpsMeasurementsProvider.addListener(listener);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void removeGpsMeasurementsListener(IGpsMeasurementsListener listener) {
        this.mGpsMeasurementsProvider.removeListener(listener);
    }

    public boolean addGpsNavigationMessageListener(IGpsNavigationMessageListener listener, String packageName) {
        int allowedResolutionLevel = getCallerAllowedResolutionLevel();
        checkResolutionLevelIsSufficientForProviderUse(allowedResolutionLevel, "gps");
        int uid = Binder.getCallingUid();
        long identity = Binder.clearCallingIdentity();
        try {
            boolean hasLocationAccess = checkLocationAccess(uid, packageName, allowedResolutionLevel);
            if (!hasLocationAccess) {
                return false;
            }
            return this.mGpsNavigationMessageProvider.addListener(listener);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void removeGpsNavigationMessageListener(IGpsNavigationMessageListener listener) {
        this.mGpsNavigationMessageProvider.removeListener(listener);
    }

    public boolean sendExtraCommand(String provider, String command, Bundle extras) {
        boolean zSendExtraCommand;
        if (provider == null) {
            throw new NullPointerException();
        }
        checkResolutionLevelIsSufficientForProviderUse(getCallerAllowedResolutionLevel(), provider);
        if (this.mContext.checkCallingOrSelfPermission(ACCESS_LOCATION_EXTRA_COMMANDS) != 0) {
            throw new SecurityException("Requires ACCESS_LOCATION_EXTRA_COMMANDS permission");
        }
        synchronized (this.mLock) {
            LocationProviderInterface p = this.mProvidersByName.get(provider);
            zSendExtraCommand = p == null ? false : p.sendExtraCommand(command, extras);
        }
        return zSendExtraCommand;
    }

    public boolean sendNiResponse(int notifId, int userResponse) {
        if (Binder.getCallingUid() != Process.myUid()) {
            throw new SecurityException("calling sendNiResponse from outside of the system is not allowed");
        }
        try {
            return this.mNetInitiatedListener.sendNiResponse(notifId, userResponse);
        } catch (RemoteException e) {
            Slog.e("LocationManagerService", "RemoteException in LocationManagerService.sendNiResponse");
            return false;
        }
    }

    public ProviderProperties getProviderProperties(String provider) {
        LocationProviderInterface p;
        if (this.mProvidersByName.get(provider) == null) {
            return null;
        }
        checkResolutionLevelIsSufficientForProviderUse(getCallerAllowedResolutionLevel(), provider);
        synchronized (this.mLock) {
            p = this.mProvidersByName.get(provider);
        }
        if (p != null) {
            return p.getProperties();
        }
        return null;
    }

    public boolean isProviderEnabled(String provider) {
        boolean zIsAllowedByUserSettingsLocked = false;
        if (!"fused".equals(provider)) {
            int uid = Binder.getCallingUid();
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    LocationProviderInterface p = this.mProvidersByName.get(provider);
                    if (p != null) {
                        zIsAllowedByUserSettingsLocked = isAllowedByUserSettingsLocked(provider, uid);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
        return zIsAllowedByUserSettingsLocked;
    }

    private boolean isUidALocationProvider(int uid) {
        if (uid == 1000) {
            return true;
        }
        if (this.mGeocodeProvider != null && doesUidHavePackage(uid, this.mGeocodeProvider.getConnectedPackageName())) {
            return true;
        }
        for (LocationProviderProxy proxy : this.mProxyProviders) {
            if (doesUidHavePackage(uid, proxy.getConnectedPackageName())) {
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

    private boolean doesUidHavePackage(int uid, String packageName) {
        String[] packageNames;
        if (packageName == null || (packageNames = this.mPackageManager.getPackagesForUid(uid)) == null) {
            return false;
        }
        for (String name : packageNames) {
            if (packageName.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public void reportLocation(Location location, boolean passive) {
        checkCallerIsProvider();
        if (!location.isComplete()) {
            Log.w("LocationManagerService", "Dropping incomplete location: " + location);
            return;
        }
        this.mLocationHandler.removeMessages(1, location);
        Message m = Message.obtain(this.mLocationHandler, 1, location);
        m.arg1 = passive ? 1 : 0;
        this.mLocationHandler.sendMessageAtFrontOfQueue(m);
    }

    private static boolean shouldBroadcastSafe(Location loc, Location lastLoc, UpdateRecord record, long now) {
        if (lastLoc == null) {
            return true;
        }
        long minTime = record.mRequest.getFastestInterval();
        long delta = (loc.getElapsedRealtimeNanos() - lastLoc.getElapsedRealtimeNanos()) / NANOS_PER_MILLI;
        if (delta < minTime - 100) {
            return false;
        }
        double minDistance = record.mRequest.getSmallestDisplacement();
        if ((minDistance > 0.0d && loc.distanceTo(lastLoc) <= minDistance) || record.mRequest.getNumUpdates() <= 0 || record.mRequest.getExpireAt() < now) {
            return false;
        }
        return true;
    }

    private void handleLocationChangedLocked(Location location, boolean passive) {
        Location notifyLocation;
        Location lastLoc;
        if (D) {
            Log.d("LocationManagerService", "incoming location: " + location);
        }
        long now = SystemClock.elapsedRealtime();
        String provider = passive ? "passive" : location.getProvider();
        LocationProviderInterface p = this.mProvidersByName.get(provider);
        if (p != null) {
            Location noGPSLocation = location.getExtraLocation("noGPSLocation");
            Location lastLocation = this.mLastLocation.get(provider);
            if (lastLocation == null) {
                lastLocation = new Location(provider);
                this.mLastLocation.put(provider, lastLocation);
            } else {
                Location lastNoGPSLocation = lastLocation.getExtraLocation("noGPSLocation");
                if (noGPSLocation == null && lastNoGPSLocation != null) {
                    location.setExtraLocation("noGPSLocation", lastNoGPSLocation);
                }
            }
            lastLocation.set(location);
            Location lastLocationCoarseInterval = this.mLastLocationCoarseInterval.get(provider);
            if (lastLocationCoarseInterval == null) {
                lastLocationCoarseInterval = new Location(location);
                this.mLastLocationCoarseInterval.put(provider, lastLocationCoarseInterval);
            }
            long timeDiffNanos = location.getElapsedRealtimeNanos() - lastLocationCoarseInterval.getElapsedRealtimeNanos();
            if (timeDiffNanos > 600000000000L) {
                lastLocationCoarseInterval.set(location);
            }
            Location noGPSLocation2 = lastLocationCoarseInterval.getExtraLocation("noGPSLocation");
            ArrayList<UpdateRecord> records = this.mRecordsByProvider.get(provider);
            if (records != null && records.size() != 0) {
                Location coarseLocation = null;
                if (noGPSLocation2 != null) {
                    coarseLocation = this.mLocationFudger.getOrCreate(noGPSLocation2);
                }
                long newStatusUpdateTime = p.getStatusUpdateTime();
                Bundle extras = new Bundle();
                int status = p.getStatus(extras);
                ArrayList<Receiver> deadReceivers = null;
                ArrayList<UpdateRecord> deadUpdateRecords = null;
                for (UpdateRecord r : records) {
                    Receiver receiver = r.mReceiver;
                    boolean receiverDead = false;
                    int receiverUserId = UserHandle.getUserId(receiver.mUid);
                    if (!isCurrentProfile(receiverUserId) && !isUidALocationProvider(receiver.mUid)) {
                        if (D) {
                            Log.d("LocationManagerService", "skipping loc update for background user " + receiverUserId + " (current user: " + this.mCurrentUserId + ", app: " + receiver.mPackageName + ")");
                        }
                    } else if (this.mBlacklist.isBlacklisted(receiver.mPackageName)) {
                        if (D) {
                            Log.d("LocationManagerService", "skipping loc update for blacklisted app: " + receiver.mPackageName);
                        }
                    } else if (!reportLocationAccessNoThrow(receiver.mUid, receiver.mPackageName, receiver.mAllowedResolutionLevel)) {
                        if (D) {
                            Log.d("LocationManagerService", "skipping loc update for no op app: " + receiver.mPackageName);
                        }
                    } else {
                        if (receiver.mAllowedResolutionLevel < 2) {
                            notifyLocation = coarseLocation;
                        } else {
                            notifyLocation = lastLocation;
                        }
                        if (notifyLocation != null && ((lastLoc = r.mLastFixBroadcast) == null || shouldBroadcastSafe(notifyLocation, lastLoc, r, now))) {
                            if (lastLoc == null) {
                                r.mLastFixBroadcast = new Location(notifyLocation);
                            } else {
                                lastLoc.set(notifyLocation);
                            }
                            if (!receiver.callLocationChangedLocked(notifyLocation)) {
                                Slog.w("LocationManagerService", "RemoteException calling onLocationChanged on " + receiver);
                                receiverDead = true;
                            }
                            r.mRequest.decrementNumUpdates();
                        }
                        long prevStatusUpdateTime = r.mLastStatusBroadcast;
                        if (newStatusUpdateTime > prevStatusUpdateTime && (prevStatusUpdateTime != 0 || status != 2)) {
                            r.mLastStatusBroadcast = newStatusUpdateTime;
                            if (!receiver.callStatusChangedLocked(provider, status, extras)) {
                                receiverDead = true;
                                Slog.w("LocationManagerService", "RemoteException calling onStatusChanged on " + receiver);
                            }
                        }
                        if (r.mRequest.getNumUpdates() <= 0 || r.mRequest.getExpireAt() < now) {
                            if (deadUpdateRecords == null) {
                                deadUpdateRecords = new ArrayList<>();
                            }
                            deadUpdateRecords.add(r);
                        }
                        if (receiverDead) {
                            if (deadReceivers == null) {
                                deadReceivers = new ArrayList<>();
                            }
                            if (!deadReceivers.contains(receiver)) {
                                deadReceivers.add(receiver);
                            }
                        }
                    }
                }
                if (deadReceivers != null) {
                    Iterator<Receiver> it = deadReceivers.iterator();
                    while (it.hasNext()) {
                        removeUpdatesLocked(it.next());
                    }
                }
                if (deadUpdateRecords != null) {
                    Iterator<UpdateRecord> it2 = deadUpdateRecords.iterator();
                    while (it2.hasNext()) {
                        it2.next().disposeLocked(true);
                    }
                    applyRequirementsLocked(provider);
                }
            }
        }
    }

    private class LocationWorkerHandler extends Handler {
        public LocationWorkerHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    LocationManagerService.this.handleLocationChanged((Location) msg.obj, msg.arg1 == 1);
                    break;
            }
        }
    }

    private boolean isMockProvider(String provider) {
        boolean zContainsKey;
        synchronized (this.mLock) {
            zContainsKey = this.mMockProviders.containsKey(provider);
        }
        return zContainsKey;
    }

    private void handleLocationChanged(Location location, boolean passive) {
        Location myLocation = new Location(location);
        String provider = myLocation.getProvider();
        if (!myLocation.isFromMockProvider() && isMockProvider(provider)) {
            myLocation.setIsFromMockProvider(true);
        }
        synchronized (this.mLock) {
            if (isAllowedByCurrentUserSettingsLocked(provider)) {
                if (!passive) {
                    this.mPassiveProvider.updateLocation(myLocation);
                }
                handleLocationChangedLocked(myLocation, passive);
            }
        }
    }

    public boolean geocoderIsPresent() {
        return this.mGeocodeProvider != null;
    }

    public String getFromLocation(double latitude, double longitude, int maxResults, GeocoderParams params, List<Address> addrs) {
        if (this.mGeocodeProvider != null) {
            return this.mGeocodeProvider.getFromLocation(latitude, longitude, maxResults, params, addrs);
        }
        return null;
    }

    public String getFromLocationName(String locationName, double lowerLeftLatitude, double lowerLeftLongitude, double upperRightLatitude, double upperRightLongitude, int maxResults, GeocoderParams params, List<Address> addrs) {
        if (this.mGeocodeProvider != null) {
            return this.mGeocodeProvider.getFromLocationName(locationName, lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude, maxResults, params, addrs);
        }
        return null;
    }

    private void checkMockPermissionsSafe() {
        boolean allowMocks = Settings.Secure.getInt(this.mContext.getContentResolver(), "mock_location", 0) == 1;
        if (!allowMocks) {
            throw new SecurityException("Requires ACCESS_MOCK_LOCATION secure setting");
        }
        if (this.mContext.checkCallingPermission(ACCESS_MOCK_LOCATION) != 0) {
            throw new SecurityException("Requires ACCESS_MOCK_LOCATION permission");
        }
    }

    public void addTestProvider(String name, ProviderProperties properties) {
        LocationProviderInterface p;
        checkMockPermissionsSafe();
        if ("passive".equals(name)) {
            throw new IllegalArgumentException("Cannot mock the passive location provider");
        }
        long identity = Binder.clearCallingIdentity();
        synchronized (this.mLock) {
            if (("gps".equals(name) || "network".equals(name) || "fused".equals(name)) && (p = this.mProvidersByName.get(name)) != null) {
                removeProviderLocked(p);
            }
            addTestProviderLocked(name, properties);
            updateProvidersLocked();
        }
        Binder.restoreCallingIdentity(identity);
    }

    private void addTestProviderLocked(String name, ProviderProperties properties) {
        if (this.mProvidersByName.get(name) != null) {
            throw new IllegalArgumentException("Provider \"" + name + "\" already exists");
        }
        MockProvider provider = new MockProvider(name, this, properties);
        addProviderLocked(provider);
        this.mMockProviders.put(name, provider);
        this.mLastLocation.put(name, null);
        this.mLastLocationCoarseInterval.put(name, null);
    }

    public void removeTestProvider(String provider) {
        checkMockPermissionsSafe();
        synchronized (this.mLock) {
            clearTestProviderEnabled(provider);
            clearTestProviderLocation(provider);
            clearTestProviderStatus(provider);
            MockProvider mockProvider = this.mMockProviders.remove(provider);
            if (mockProvider == null) {
                throw new IllegalArgumentException("Provider \"" + provider + "\" unknown");
            }
            long identity = Binder.clearCallingIdentity();
            removeProviderLocked(this.mProvidersByName.get(provider));
            LocationProviderInterface realProvider = this.mRealProviders.get(provider);
            if (realProvider != null) {
                addProviderLocked(realProvider);
            }
            this.mLastLocation.put(provider, null);
            this.mLastLocationCoarseInterval.put(provider, null);
            updateProvidersLocked();
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void setTestProviderLocation(String provider, Location loc) {
        checkMockPermissionsSafe();
        synchronized (this.mLock) {
            MockProvider mockProvider = this.mMockProviders.get(provider);
            if (mockProvider == null) {
                throw new IllegalArgumentException("Provider \"" + provider + "\" unknown");
            }
            Location mock = new Location(loc);
            mock.setIsFromMockProvider(true);
            if (!TextUtils.isEmpty(loc.getProvider()) && !provider.equals(loc.getProvider())) {
                EventLog.writeEvent(1397638484, "33091107", Integer.valueOf(Binder.getCallingUid()), provider + "!=" + loc.getProvider());
            }
            long identity = Binder.clearCallingIdentity();
            mockProvider.setLocation(mock);
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void clearTestProviderLocation(String provider) {
        checkMockPermissionsSafe();
        synchronized (this.mLock) {
            MockProvider mockProvider = this.mMockProviders.get(provider);
            if (mockProvider == null) {
                throw new IllegalArgumentException("Provider \"" + provider + "\" unknown");
            }
            mockProvider.clearLocation();
        }
    }

    public void setTestProviderEnabled(String provider, boolean enabled) {
        checkMockPermissionsSafe();
        synchronized (this.mLock) {
            MockProvider mockProvider = this.mMockProviders.get(provider);
            if (mockProvider == null) {
                throw new IllegalArgumentException("Provider \"" + provider + "\" unknown");
            }
            long identity = Binder.clearCallingIdentity();
            if (enabled) {
                mockProvider.enable();
                this.mEnabledProviders.add(provider);
                this.mDisabledProviders.remove(provider);
            } else {
                mockProvider.disable();
                this.mEnabledProviders.remove(provider);
                this.mDisabledProviders.add(provider);
            }
            updateProvidersLocked();
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void clearTestProviderEnabled(String provider) {
        checkMockPermissionsSafe();
        synchronized (this.mLock) {
            MockProvider mockProvider = this.mMockProviders.get(provider);
            if (mockProvider == null) {
                throw new IllegalArgumentException("Provider \"" + provider + "\" unknown");
            }
            long identity = Binder.clearCallingIdentity();
            this.mEnabledProviders.remove(provider);
            this.mDisabledProviders.remove(provider);
            updateProvidersLocked();
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void setTestProviderStatus(String provider, int status, Bundle extras, long updateTime) {
        checkMockPermissionsSafe();
        synchronized (this.mLock) {
            MockProvider mockProvider = this.mMockProviders.get(provider);
            if (mockProvider == null) {
                throw new IllegalArgumentException("Provider \"" + provider + "\" unknown");
            }
            mockProvider.setStatus(status, extras, updateTime);
        }
    }

    public void clearTestProviderStatus(String provider) {
        checkMockPermissionsSafe();
        synchronized (this.mLock) {
            MockProvider mockProvider = this.mMockProviders.get(provider);
            if (mockProvider == null) {
                throw new IllegalArgumentException("Provider \"" + provider + "\" unknown");
            }
            mockProvider.clearStatus();
        }
    }

    private void log(String log) {
        if (Log.isLoggable("LocationManagerService", 2)) {
            Slog.d("LocationManagerService", log);
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump LocationManagerService from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        synchronized (this.mLock) {
            pw.println("Current Location Manager state:");
            pw.println("  Location Listeners:");
            for (Receiver receiver : this.mReceivers.values()) {
                pw.println("    " + receiver);
            }
            pw.println("  Active Records by Provider:");
            for (Map.Entry<String, ArrayList<UpdateRecord>> entry : this.mRecordsByProvider.entrySet()) {
                pw.println("    " + entry.getKey() + ":");
                for (UpdateRecord record : entry.getValue()) {
                    pw.println("      " + record);
                }
            }
            pw.println("  Historical Records by Provider:");
            for (Map.Entry<LocationRequestStatistics.PackageProviderKey, LocationRequestStatistics.PackageStatistics> entry2 : this.mRequestStatistics.statistics.entrySet()) {
                LocationRequestStatistics.PackageProviderKey key = entry2.getKey();
                LocationRequestStatistics.PackageStatistics stats = entry2.getValue();
                pw.println("    " + key.packageName + ": " + key.providerName + ": " + stats);
            }
            pw.println("  Last Known Locations:");
            for (Map.Entry<String, Location> entry3 : this.mLastLocation.entrySet()) {
                String provider = entry3.getKey();
                Location location = entry3.getValue();
                pw.println("    " + provider + ": " + location);
            }
            pw.println("  Last Known Locations Coarse Intervals:");
            for (Map.Entry<String, Location> entry4 : this.mLastLocationCoarseInterval.entrySet()) {
                String provider2 = entry4.getKey();
                Location location2 = entry4.getValue();
                pw.println("    " + provider2 + ": " + location2);
            }
            this.mGeofenceManager.dump(pw);
            if (this.mEnabledProviders.size() > 0) {
                pw.println("  Enabled Providers:");
                for (String i : this.mEnabledProviders) {
                    pw.println("    " + i);
                }
            }
            if (this.mDisabledProviders.size() > 0) {
                pw.println("  Disabled Providers:");
                for (String i2 : this.mDisabledProviders) {
                    pw.println("    " + i2);
                }
            }
            pw.append("  ");
            this.mBlacklist.dump(pw);
            if (this.mMockProviders.size() > 0) {
                pw.println("  Mock Providers:");
                for (Map.Entry<String, MockProvider> i3 : this.mMockProviders.entrySet()) {
                    i3.getValue().dump(pw, "      ");
                }
            }
            pw.append("  fudger: ");
            this.mLocationFudger.dump(fd, pw, args);
            if (args.length <= 0 || !"short".equals(args[0])) {
                for (LocationProviderInterface provider3 : this.mProviders) {
                    pw.print(provider3.getName() + " Internal State");
                    if (provider3 instanceof LocationProviderProxy) {
                        LocationProviderProxy proxy = (LocationProviderProxy) provider3;
                        pw.print(" (" + proxy.getConnectedPackageName() + ")");
                    }
                    pw.println(":");
                    provider3.dump(fd, pw, args);
                }
            }
        }
    }
}
