package com.android.server.location;

import android.R;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.hardware.location.GeofenceHardwareImpl;
import android.location.FusedBatchOptions;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.IGnssStatusListener;
import android.location.IGnssStatusProvider;
import android.location.IGpsGeofenceHardware;
import android.location.ILocationManager;
import android.location.INetInitiatedListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationRequest;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.util.NetworkConstants;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.PowerSaveState;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.WorkSource;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.app.IBatteryStats;
import com.android.internal.location.GpsNetInitiatedHandler;
import com.android.internal.location.ProviderProperties;
import com.android.internal.location.ProviderRequest;
import com.android.internal.location.gnssmetrics.GnssMetrics;
import com.android.server.UiModeManagerService;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.location.GnssSatelliteBlacklistHelper;
import com.android.server.location.NtpTimeHelper;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import libcore.io.IoUtils;

public class GnssLocationProvider implements LocationProviderInterface, NtpTimeHelper.InjectNtpTimeCallback, GnssSatelliteBlacklistHelper.GnssSatelliteBlacklistCallback {
    private static final int ADD_LISTENER = 8;
    private static final int AGPS_DATA_CONNECTION_CLOSED = 0;
    private static final int AGPS_DATA_CONNECTION_OPEN = 2;
    private static final int AGPS_DATA_CONNECTION_OPENING = 1;
    private static final int AGPS_REF_LOCATION_TYPE_GSM_CELLID = 1;
    private static final int AGPS_REF_LOCATION_TYPE_UMTS_CELLID = 2;
    private static final int AGPS_RIL_REQUEST_SETID_IMSI = 1;
    private static final int AGPS_RIL_REQUEST_SETID_MSISDN = 2;
    private static final int AGPS_SETID_TYPE_IMSI = 1;
    private static final int AGPS_SETID_TYPE_MSISDN = 2;
    private static final int AGPS_SETID_TYPE_NONE = 0;
    private static final int AGPS_SUPL_MODE_MSA = 2;
    private static final int AGPS_SUPL_MODE_MSB = 1;
    private static final int AGPS_TYPE_C2K = 2;
    private static final int AGPS_TYPE_SUPL = 1;
    private static final String ALARM_TIMEOUT = "com.android.internal.location.ALARM_TIMEOUT";
    private static final String ALARM_WAKEUP = "com.android.internal.location.ALARM_WAKEUP";
    private static final int APN_INVALID = 0;
    private static final int APN_IPV4 = 1;
    private static final int APN_IPV4V6 = 3;
    private static final int APN_IPV6 = 2;
    private static final int CHECK_LOCATION = 1;
    private static final boolean DEBUG;
    private static final String DEBUG_PROPERTIES_FILE = "/etc/gps_debug.conf";
    private static final String DOWNLOAD_EXTRA_WAKELOCK_KEY = "GnssLocationProviderXtraDownload";
    private static final int DOWNLOAD_XTRA_DATA = 6;
    private static final int DOWNLOAD_XTRA_DATA_FINISHED = 11;
    private static final long DOWNLOAD_XTRA_DATA_TIMEOUT_MS = 60000;
    private static final int ENABLE = 2;
    public static final boolean FORCE_DEBUG;
    private static final int GPS_AGPS_DATA_CONNECTED = 3;
    private static final int GPS_AGPS_DATA_CONN_DONE = 4;
    private static final int GPS_AGPS_DATA_CONN_FAILED = 5;
    private static final int GPS_CAPABILITY_GEOFENCING = 32;
    private static final int GPS_CAPABILITY_MEASUREMENTS = 64;
    private static final int GPS_CAPABILITY_MSA = 4;
    private static final int GPS_CAPABILITY_MSB = 2;
    private static final int GPS_CAPABILITY_NAV_MESSAGES = 128;
    private static final int GPS_CAPABILITY_ON_DEMAND_TIME = 16;
    private static final int GPS_CAPABILITY_SCHEDULING = 1;
    private static final int GPS_CAPABILITY_SINGLE_SHOT = 8;
    private static final int GPS_DELETE_ALL = 65535;
    private static final int GPS_DELETE_ALMANAC = 2;
    private static final int GPS_DELETE_CELLDB_INFO = 32768;
    private static final int GPS_DELETE_EPHEMERIS = 1;
    private static final int GPS_DELETE_EPO = 16384;
    private static final int GPS_DELETE_HEALTH = 64;
    private static final int GPS_DELETE_HOT_STILL = 8192;
    private static final int GPS_DELETE_IONO = 16;
    private static final int GPS_DELETE_POSITION = 4;
    private static final int GPS_DELETE_RTI = 1024;
    private static final int GPS_DELETE_SADATA = 512;
    private static final int GPS_DELETE_SVDIR = 128;
    private static final int GPS_DELETE_SVSTEER = 256;
    private static final int GPS_DELETE_TIME = 8;
    private static final int GPS_DELETE_UTC = 32;
    private static final int GPS_GEOFENCE_AVAILABLE = 2;
    private static final int GPS_GEOFENCE_ERROR_GENERIC = -149;
    private static final int GPS_GEOFENCE_ERROR_ID_EXISTS = -101;
    private static final int GPS_GEOFENCE_ERROR_ID_UNKNOWN = -102;
    private static final int GPS_GEOFENCE_ERROR_INVALID_TRANSITION = -103;
    private static final int GPS_GEOFENCE_ERROR_TOO_MANY_GEOFENCES = 100;
    private static final int GPS_GEOFENCE_OPERATION_SUCCESS = 0;
    private static final int GPS_GEOFENCE_UNAVAILABLE = 1;
    private static final int GPS_POLLING_THRESHOLD_INTERVAL = 10000;
    private static final int GPS_POSITION_MODE_MS_ASSISTED = 2;
    private static final int GPS_POSITION_MODE_MS_BASED = 1;
    private static final int GPS_POSITION_MODE_STANDALONE = 0;
    private static final int GPS_POSITION_RECURRENCE_PERIODIC = 0;
    private static final int GPS_POSITION_RECURRENCE_SINGLE = 1;
    private static final int GPS_RELEASE_AGPS_DATA_CONN = 2;
    private static final int GPS_REQUEST_AGPS_DATA_CONN = 1;
    private static final int GPS_STATUS_ENGINE_OFF = 4;
    private static final int GPS_STATUS_ENGINE_ON = 3;
    private static final int GPS_STATUS_NONE = 0;
    private static final int GPS_STATUS_SESSION_BEGIN = 1;
    private static final int GPS_STATUS_SESSION_END = 2;
    private static final int INITIALIZE_HANDLER = 13;
    private static final int INJECT_NTP_TIME = 5;
    private static final boolean IS_USER_BUILD;
    private static final float ITAR_SPEED_LIMIT_METERS_PER_SECOND = 400.0f;
    private static final int LAST_LOCATION_EXPIRED_TIMEOUT = 600000;
    private static final int LOCATION_HAS_ALTITUDE = 2;
    private static final int LOCATION_HAS_BEARING = 8;
    private static final int LOCATION_HAS_BEARING_ACCURACY = 128;
    private static final int LOCATION_HAS_HORIZONTAL_ACCURACY = 16;
    private static final int LOCATION_HAS_LAT_LONG = 1;
    private static final int LOCATION_HAS_SPEED = 4;
    private static final int LOCATION_HAS_SPEED_ACCURACY = 64;
    private static final int LOCATION_HAS_VERTICAL_ACCURACY = 32;
    private static final int LOCATION_INVALID = 0;
    private static final long LOCATION_UPDATE_DURATION_MILLIS = 0;
    private static final long LOCATION_UPDATE_MIN_TIME_INTERVAL_MILLIS = 1000;
    private static final String LPP_PROFILE = "persist.sys.gps.lpp";
    private static final long MAX_RETRY_INTERVAL = 14400000;
    private static final int NO_FIX_TIMEOUT = 60000;
    private static final ProviderProperties PROPERTIES;
    private static final String PROP_FORCE_DEBUG_KEY = "persist.vendor.log.tel_dbg";
    private static final long RECENT_FIX_TIMEOUT = 10000;
    private static final int RELEASE_SUPL_CONNECTION = 15;
    private static final int REMOVE_LISTENER = 9;
    private static final int REPORT_LOCATION = 17;
    private static final int REPORT_SV_STATUS = 18;
    private static final int REQUEST_LOCATION = 16;
    private static final int REQUEST_SUPL_CONNECTION = 14;
    private static final long RETRY_INTERVAL = 300000;
    private static final int SET_REQUEST = 3;
    private static final String SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";
    private static final int STATE_DOWNLOADING = 1;
    private static final int STATE_IDLE = 2;
    private static final int STATE_PENDING_NETWORK = 0;
    private static final int SUBSCRIPTION_OR_SIM_CHANGED = 12;
    private static final String TAG = "GnssLocationProvider";
    private static final int TCP_MAX_PORT = 65535;
    private static final int TCP_MIN_PORT = 0;
    private static final int UPDATE_LOCATION = 7;
    private static final int UPDATE_NETWORK_STATE = 4;
    private static final boolean VERBOSE;
    private static final String WAKELOCK_KEY = "GnssLocationProvider";
    private InetAddress mAGpsDataConnectionIpAddr;
    private int mAGpsDataConnectionState;
    private final AlarmManager mAlarmManager;
    private final AppOpsManager mAppOps;
    private final IBatteryStats mBatteryStats;
    private String mC2KServerHost;
    private int mC2KServerPort;
    private final ConnectivityManager mConnMgr;
    private final Context mContext;
    private final PowerManager.WakeLock mDownloadXtraWakeLock;
    private boolean mEnabled;
    private int mEngineCapabilities;
    private boolean mEngineOn;
    private final LocationChangeListener mFusedLocationListener;
    private GeofenceHardwareImpl mGeofenceHardwareImpl;
    private final GnssBatchingProvider mGnssBatchingProvider;
    private final GnssGeofenceProvider mGnssGeofenceProvider;
    private final GnssMeasurementsProvider mGnssMeasurementsProvider;
    private GnssMetrics mGnssMetrics;
    private final GnssNavigationMessageProvider mGnssNavigationMessageProvider;
    private final GnssSatelliteBlacklistHelper mGnssSatelliteBlacklistHelper;
    private Handler mHandler;
    private volatile String mHardwareModelName;
    private final ILocationManager mILocationManager;
    private long mLastFixTime;
    private final GnssStatusListenerHelper mListenerHelper;
    private final GpsNetInitiatedHandler mNIHandler;
    private boolean mNavigating;
    private final LocationChangeListener mNetworkLocationListener;
    private final NtpTimeHelper mNtpTimeHelper;
    private int mPositionMode;
    private final PowerManager mPowerManager;
    private Properties mProperties;
    private boolean mSingleShot;
    private boolean mStarted;
    private String mSuplServerHost;
    private boolean mSupportsXtra;
    private final PendingIntent mTimeoutIntent;
    private final PowerManager.WakeLock mWakeLock;
    private final PendingIntent mWakeupIntent;
    private final Object mLock = new Object();
    private int mStatus = 1;
    private long mStatusUpdateTime = SystemClock.elapsedRealtime();
    private final ExponentialBackOff mXtraBackOff = new ExponentialBackOff(300000, 14400000);
    private int mDownloadXtraDataPending = 0;
    private int mFixInterval = 1000;
    private boolean mLowPowerMode = false;
    private long mFixRequestTime = 0;
    private int mTimeToFirstFix = 0;
    private ProviderRequest mProviderRequest = null;
    private WorkSource mWorkSource = null;
    private boolean mDisableGps = false;
    private int mSuplServerPort = 0;
    private boolean mSuplEsEnabled = false;
    private final LocationExtras mLocationExtras = new LocationExtras();
    private WorkSource mClientSource = new WorkSource();
    private volatile int mHardwareYear = 0;
    private volatile boolean mItarSpeedLimitExceeded = false;
    private final IGnssStatusProvider mGnssStatusProvider = new IGnssStatusProvider.Stub() {
        public void registerGnssStatusCallback(IGnssStatusListener iGnssStatusListener) {
            GnssLocationProvider.this.mListenerHelper.addListener(iGnssStatusListener);
        }

        public void unregisterGnssStatusCallback(IGnssStatusListener iGnssStatusListener) {
            GnssLocationProvider.this.mListenerHelper.removeListener(iGnssStatusListener);
        }
    };
    private final ConnectivityManager.NetworkCallback mNetworkConnectivityCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            GnssLocationProvider.this.mNtpTimeHelper.onNetworkAvailable();
            if (GnssLocationProvider.this.mDownloadXtraDataPending == 0 && GnssLocationProvider.this.mSupportsXtra) {
                GnssLocationProvider.this.xtraDownloadRequest();
            }
            GnssLocationProvider.this.sendMessage(4, 0, network);
        }

        @Override
        public void onLost(Network network) {
            GnssLocationProvider.this.sendMessage(4, 0, network);
        }
    };
    private final ConnectivityManager.NetworkCallback mSuplConnectivityCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            GnssLocationProvider.this.sendMessage(4, 0, network);
        }

        @Override
        public void onLost(Network network) {
            GnssLocationProvider.this.releaseSuplConnection(2);
        }
    };
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (GnssLocationProvider.DEBUG) {
                Log.d("GnssLocationProvider", "receive broadcast intent, action: " + action);
            }
            if (action == null) {
                return;
            }
            if (action.equals(GnssLocationProvider.ALARM_WAKEUP)) {
                GnssLocationProvider.this.startNavigating(false);
                return;
            }
            if (action.equals(GnssLocationProvider.ALARM_TIMEOUT)) {
                GnssLocationProvider.this.hibernate();
                return;
            }
            if ("android.os.action.POWER_SAVE_MODE_CHANGED".equals(action) || "android.os.action.DEVICE_IDLE_MODE_CHANGED".equals(action) || "android.intent.action.SCREEN_OFF".equals(action) || "android.intent.action.SCREEN_ON".equals(action)) {
                GnssLocationProvider.this.updateLowPowerMode();
            } else if (action.equals(GnssLocationProvider.SIM_STATE_CHANGED)) {
                GnssLocationProvider.this.subscriptionOrSimChanged(context);
            }
        }
    };
    private final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangedListener = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            GnssLocationProvider.this.sendMessage(12, 0, null);
        }
    };
    private final INetInitiatedListener mNetInitiatedListener = new INetInitiatedListener.Stub() {
        public boolean sendNiResponse(int i, int i2) {
            if (GnssLocationProvider.DEBUG) {
                Log.d("GnssLocationProvider", "sendNiResponse, notifId: " + i + ", response: " + i2);
            }
            GnssLocationProvider.this.native_send_ni_response(i, i2);
            return true;
        }
    };
    private byte[] mNmeaBuffer = new byte[120];
    private Class<?> mMtkGnssProviderClass = null;
    private Object mMtkGnssProvider = null;

    public interface GnssMetricsProvider {
        String getGnssMetricsAsProtoString();
    }

    public interface GnssSystemInfoProvider {
        String getGnssHardwareModelName();

        int getGnssYearOfHardware();
    }

    interface SetCarrierProperty {
        boolean set(int i);
    }

    private static native void class_init_native();

    private native void native_agps_data_conn_closed();

    private native void native_agps_data_conn_failed();

    private native void native_agps_data_conn_open(String str, int i);

    private native void native_agps_ni_message(byte[] bArr, int i);

    private native void native_agps_set_id(int i, String str);

    private native void native_agps_set_ref_location_cellid(int i, int i2, int i3, int i4, int i5);

    private native void native_cleanup();

    private native void native_delete_aiding_data(int i);

    private native String native_get_internal_state();

    private native boolean native_init();

    private static native void native_init_once();

    private native void native_inject_best_location(int i, double d, double d2, double d3, float f, float f2, float f3, float f4, float f5, float f6, long j);

    private native void native_inject_location(double d, double d2, float f);

    private native void native_inject_time(long j, long j2, int i);

    private native void native_inject_xtra_data(byte[] bArr, int i);

    private static native boolean native_is_agps_ril_supported();

    private static native boolean native_is_gnss_configuration_supported();

    private static native boolean native_is_supported();

    private native int native_read_nmea(byte[] bArr, int i);

    private native void native_send_ni_response(int i, int i2);

    private native void native_set_agps_server(int i, String str, int i2);

    private static native boolean native_set_emergency_supl_pdn(int i);

    private static native boolean native_set_gnss_pos_protocol_select(int i);

    private static native boolean native_set_gps_lock(int i);

    private static native boolean native_set_lpp_profile(int i);

    private native boolean native_set_position_mode(int i, int i2, int i3, int i4, int i5, boolean z);

    private static native boolean native_set_satellite_blacklist(int[] iArr, int[] iArr2);

    private static native boolean native_set_supl_es(int i);

    private static native boolean native_set_supl_mode(int i);

    private static native boolean native_set_supl_version(int i);

    private native boolean native_start();

    private native boolean native_stop();

    private native boolean native_supports_xtra();

    private native void native_update_network_state(boolean z, int i, boolean z2, boolean z3, String str, String str2);

    static {
        boolean z = true;
        IS_USER_BUILD = "user".equals(Build.TYPE) || "userdebug".equals(Build.TYPE);
        FORCE_DEBUG = SystemProperties.getInt(PROP_FORCE_DEBUG_KEY, 0) == 1;
        DEBUG = !IS_USER_BUILD || Log.isLoggable("GnssLocationProvider", 3) || FORCE_DEBUG;
        if (IS_USER_BUILD && !Log.isLoggable("GnssLocationProvider", 2) && !FORCE_DEBUG) {
            z = false;
        }
        VERBOSE = z;
        PROPERTIES = new ProviderProperties(true, true, false, false, true, true, true, 3, 1);
        class_init_native();
    }

    private static class GpsRequest {
        public ProviderRequest request;
        public WorkSource source;

        public GpsRequest(ProviderRequest providerRequest, WorkSource workSource) {
            this.request = providerRequest;
            this.source = workSource;
        }
    }

    private static class LocationExtras {
        private final Bundle mBundle = new Bundle();
        private int mMaxCn0;
        private int mMeanCn0;
        private int mSvCount;

        public void set(int i, int i2, int i3) {
            synchronized (this) {
                this.mSvCount = i;
                this.mMeanCn0 = i2;
                this.mMaxCn0 = i3;
            }
            setBundle(this.mBundle);
        }

        public void reset() {
            set(0, 0, 0);
        }

        public void setBundle(Bundle bundle) {
            if (bundle != null) {
                synchronized (this) {
                    bundle.putInt("satellites", this.mSvCount);
                    bundle.putInt("meanCn0", this.mMeanCn0);
                    bundle.putInt("maxCn0", this.mMaxCn0);
                }
            }
        }

        public Bundle getBundle() {
            Bundle bundle;
            synchronized (this) {
                bundle = new Bundle(this.mBundle);
            }
            return bundle;
        }
    }

    public IGnssStatusProvider getGnssStatusProvider() {
        return this.mGnssStatusProvider;
    }

    public IGpsGeofenceHardware getGpsGeofenceProxy() {
        return this.mGnssGeofenceProvider;
    }

    public GnssMeasurementsProvider getGnssMeasurementsProvider() {
        return this.mGnssMeasurementsProvider;
    }

    public GnssNavigationMessageProvider getGnssNavigationMessageProvider() {
        return this.mGnssNavigationMessageProvider;
    }

    @Override
    public void onUpdateSatelliteBlacklist(final int[] iArr, final int[] iArr2) {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                GnssLocationProvider.native_set_satellite_blacklist(iArr, iArr2);
            }
        });
    }

    private void subscriptionOrSimChanged(Context context) {
        if (DEBUG) {
            Log.d("GnssLocationProvider", "received SIM related action: ");
        }
        TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
        String simOperator = telephonyManager.getSimOperator();
        boolean z = false;
        if (!TextUtils.isEmpty(simOperator)) {
            if (DEBUG) {
                Log.d("GnssLocationProvider", "SIM MCC/MNC is available: " + simOperator);
            }
            synchronized (this.mLock) {
                if (carrierConfigManager != null) {
                    try {
                        PersistableBundle config = carrierConfigManager.getConfig();
                        if (config != null) {
                            z = config.getBoolean("persist_lpp_mode_bool");
                        }
                    } finally {
                    }
                }
                if (z) {
                    loadPropertiesFromResource(context, this.mProperties);
                    String property = this.mProperties.getProperty("LPP_PROFILE");
                    if (property != null) {
                        SystemProperties.set(LPP_PROFILE, property);
                    }
                } else {
                    SystemProperties.set(LPP_PROFILE, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                }
                reloadGpsProperties(context, this.mProperties);
                this.mNIHandler.setSuplEsEnabled(this.mSuplEsEnabled);
            }
            return;
        }
        if (DEBUG) {
            Log.d("GnssLocationProvider", "SIM MCC/MNC is still not available");
        }
    }

    private void updateLowPowerMode() {
        boolean zIsDeviceIdleMode = this.mPowerManager.isDeviceIdleMode();
        PowerSaveState powerSaveState = this.mPowerManager.getPowerSaveState(1);
        if (powerSaveState.gpsMode == 1) {
            zIsDeviceIdleMode |= powerSaveState.batterySaverEnabled && !this.mPowerManager.isInteractive();
        }
        if (zIsDeviceIdleMode != this.mDisableGps) {
            this.mDisableGps = zIsDeviceIdleMode;
            Log.d("GnssLocationProvider", "updateRequirements trigger by low power mode disableGps =" + zIsDeviceIdleMode);
            updateRequirements();
        }
    }

    public static boolean isSupported() {
        return native_is_supported();
    }

    private void reloadGpsProperties(Context context, Properties properties) throws Throwable {
        if (DEBUG) {
            Log.d("GnssLocationProvider", "Reset GPS properties, previous size = " + properties.size());
        }
        loadPropertiesFromResource(context, properties);
        String str = SystemProperties.get(LPP_PROFILE);
        if (!TextUtils.isEmpty(str)) {
            properties.setProperty("LPP_PROFILE", str);
        }
        loadPropertiesFromFile(DEBUG_PROPERTIES_FILE, properties);
        setSuplHostPort(properties.getProperty("SUPL_HOST"), properties.getProperty("SUPL_PORT"));
        this.mC2KServerHost = properties.getProperty("C2K_HOST");
        String property = properties.getProperty("C2K_PORT");
        if (this.mC2KServerHost != null && property != null) {
            try {
                this.mC2KServerPort = Integer.parseInt(property);
            } catch (NumberFormatException e) {
                Log.e("GnssLocationProvider", "unable to parse C2K_PORT: " + property);
            }
        }
        if (native_is_gnss_configuration_supported()) {
            for (Map.Entry<String, SetCarrierProperty> entry : new AnonymousClass6().entrySet()) {
                String key = entry.getKey();
                String property2 = properties.getProperty(key);
                if (property2 != null) {
                    try {
                        if (!entry.getValue().set(Integer.decode(property2).intValue())) {
                            Log.e("GnssLocationProvider", "Unable to set " + key);
                        }
                    } catch (NumberFormatException e2) {
                        Log.e("GnssLocationProvider", "unable to parse propertyName: " + property2);
                    }
                }
            }
        } else if (DEBUG) {
            Log.d("GnssLocationProvider", "Skipped configuration update because GNSS configuration in GPS HAL is not supported");
        }
        String property3 = this.mProperties.getProperty("SUPL_ES");
        if (property3 != null) {
            try {
                boolean z = true;
                if (Integer.parseInt(property3) != 1) {
                    z = false;
                }
                this.mSuplEsEnabled = z;
            } catch (NumberFormatException e3) {
                Log.e("GnssLocationProvider", "unable to parse SUPL_ES: " + property3);
            }
        }
        String property4 = properties.getProperty("ES_EXTENSION_SEC", "0");
        try {
            this.mNIHandler.setEmergencyExtensionSeconds(Integer.parseInt(property4));
        } catch (NumberFormatException e4) {
            Log.e("GnssLocationProvider", "unable to parse ES_EXTENSION_SEC: " + property4);
        }
    }

    class AnonymousClass6 extends HashMap<String, SetCarrierProperty> {
        AnonymousClass6() {
            put("SUPL_VER", new SetCarrierProperty() {
                @Override
                public final boolean set(int i) {
                    return GnssLocationProvider.native_set_supl_version(i);
                }
            });
            put("SUPL_MODE", new SetCarrierProperty() {
                @Override
                public final boolean set(int i) {
                    return GnssLocationProvider.native_set_supl_mode(i);
                }
            });
            put("SUPL_ES", new SetCarrierProperty() {
                @Override
                public final boolean set(int i) {
                    return GnssLocationProvider.native_set_supl_es(i);
                }
            });
            put("LPP_PROFILE", new SetCarrierProperty() {
                @Override
                public final boolean set(int i) {
                    return GnssLocationProvider.native_set_lpp_profile(i);
                }
            });
            put("A_GLONASS_POS_PROTOCOL_SELECT", new SetCarrierProperty() {
                @Override
                public final boolean set(int i) {
                    return GnssLocationProvider.native_set_gnss_pos_protocol_select(i);
                }
            });
            put("USE_EMERGENCY_PDN_FOR_EMERGENCY_SUPL", new SetCarrierProperty() {
                @Override
                public final boolean set(int i) {
                    return GnssLocationProvider.native_set_emergency_supl_pdn(i);
                }
            });
            put("GPS_LOCK", new SetCarrierProperty() {
                @Override
                public final boolean set(int i) {
                    return GnssLocationProvider.native_set_gps_lock(i);
                }
            });
        }
    }

    private void loadPropertiesFromResource(Context context, Properties properties) {
        int i;
        for (String str : context.getResources().getStringArray(R.array.config_builtInDisplayIsRoundArray)) {
            if (DEBUG) {
                Log.d("GnssLocationProvider", "GpsParamsResource: " + str);
            }
            int iIndexOf = str.indexOf("=");
            if (iIndexOf > 0 && (i = iIndexOf + 1) < str.length()) {
                properties.setProperty(str.substring(0, iIndexOf).trim().toUpperCase(), str.substring(i));
            } else {
                Log.w("GnssLocationProvider", "malformed contents: " + str);
            }
        }
    }

    private boolean loadPropertiesFromFile(String str, Properties properties) throws Throwable {
        try {
            FileInputStream fileInputStream = null;
            try {
                FileInputStream fileInputStream2 = new FileInputStream(new File(str));
                try {
                    properties.load(fileInputStream2);
                    IoUtils.closeQuietly(fileInputStream2);
                    return true;
                } catch (Throwable th) {
                    th = th;
                    fileInputStream = fileInputStream2;
                    IoUtils.closeQuietly(fileInputStream);
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (IOException e) {
            if (DEBUG) {
                Log.d("GnssLocationProvider", "Could not open GPS configuration file " + str);
                return false;
            }
            return false;
        }
    }

    public GnssLocationProvider(Context context, ILocationManager iLocationManager, Looper looper) {
        this.mNetworkLocationListener = new NetworkLocationListener();
        this.mFusedLocationListener = new FusedLocationListener();
        this.mContext = context;
        this.mILocationManager = iLocationManager;
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mWakeLock = this.mPowerManager.newWakeLock(1, "GnssLocationProvider");
        this.mWakeLock.setReferenceCounted(true);
        this.mDownloadXtraWakeLock = this.mPowerManager.newWakeLock(1, DOWNLOAD_EXTRA_WAKELOCK_KEY);
        this.mDownloadXtraWakeLock.setReferenceCounted(true);
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mWakeupIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(ALARM_WAKEUP), 0);
        this.mTimeoutIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(ALARM_TIMEOUT), 0);
        this.mConnMgr = (ConnectivityManager) context.getSystemService("connectivity");
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
        this.mBatteryStats = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
        this.mHandler = new ProviderHandler(looper);
        this.mProperties = new Properties();
        this.mNIHandler = new GpsNetInitiatedHandler(context, this.mNetInitiatedListener, this.mSuplEsEnabled);
        sendMessage(13, 0, null);
        this.mListenerHelper = new GnssStatusListenerHelper(this.mHandler) {
            @Override
            protected boolean isAvailableInPlatform() {
                return GnssLocationProvider.isSupported();
            }

            @Override
            protected boolean isGpsEnabled() {
                return GnssLocationProvider.this.isEnabled();
            }
        };
        this.mGnssMeasurementsProvider = new GnssMeasurementsProvider(this.mContext, this.mHandler) {
            @Override
            protected boolean isGpsEnabled() {
                return GnssLocationProvider.this.isEnabled();
            }
        };
        this.mGnssNavigationMessageProvider = new GnssNavigationMessageProvider(this.mHandler) {
            @Override
            protected boolean isGpsEnabled() {
                return GnssLocationProvider.this.isEnabled();
            }
        };
        this.mGnssMetrics = new GnssMetrics(this.mBatteryStats);
        this.mNtpTimeHelper = new NtpTimeHelper(this.mContext, looper, this);
        this.mGnssSatelliteBlacklistHelper = new GnssSatelliteBlacklistHelper(this.mContext, looper, this);
        Handler handler = this.mHandler;
        final GnssSatelliteBlacklistHelper gnssSatelliteBlacklistHelper = this.mGnssSatelliteBlacklistHelper;
        Objects.requireNonNull(gnssSatelliteBlacklistHelper);
        handler.post(new Runnable() {
            @Override
            public final void run() {
                gnssSatelliteBlacklistHelper.updateSatelliteBlacklist();
            }
        });
        this.mGnssBatchingProvider = new GnssBatchingProvider();
        this.mGnssGeofenceProvider = new GnssGeofenceProvider(looper);
        initMtkGnssLocProvider();
    }

    @Override
    public String getName() {
        return "gps";
    }

    @Override
    public ProviderProperties getProperties() {
        return PROPERTIES;
    }

    @Override
    public void injectTime(long j, long j2, int i) {
        native_inject_time(j, j2, i);
    }

    private void handleUpdateNetworkState(Network network) throws Throwable {
        int i;
        String extraInfo;
        boolean zIsRoaming;
        boolean z;
        boolean z2;
        NetworkInfo networkInfo = this.mConnMgr.getNetworkInfo(network);
        if (networkInfo != null) {
            boolean z3 = networkInfo.isAvailable() && TelephonyManager.getDefault().getDataEnabled();
            boolean zIsConnected = networkInfo.isConnected();
            int type = networkInfo.getType();
            z2 = zIsConnected;
            zIsRoaming = networkInfo.isRoaming();
            extraInfo = networkInfo.getExtraInfo();
            z = z3;
            i = type;
        } else {
            i = -1;
            extraInfo = null;
            zIsRoaming = false;
            z = false;
            z2 = false;
        }
        if (DEBUG) {
            Log.d("GnssLocationProvider", String.format("UpdateNetworkState, state=%s, connected=%s, info=%s, capabilities=%S", agpsDataConnStateAsString(), Boolean.valueOf(z2), networkInfo, this.mConnMgr.getNetworkCapabilities(network)));
        }
        if (native_is_agps_ril_supported()) {
            String selectedApn = getSelectedApn();
            if (selectedApn == null) {
                selectedApn = "dummy-apn";
            }
            native_update_network_state(z2, i, zIsRoaming, z, extraInfo, selectedApn);
        } else if (DEBUG) {
            Log.d("GnssLocationProvider", "Skipped network state update because GPS HAL AGPS-RIL is not  supported");
        }
        if (this.mAGpsDataConnectionState == 1) {
            if (z2) {
                if (extraInfo == null) {
                    extraInfo = "dummy-apn";
                }
                int apnIpType = getApnIpType(extraInfo);
                setRouting();
                if (DEBUG) {
                    Log.d("GnssLocationProvider", String.format("native_agps_data_conn_open: mAgpsApn=%s, mApnIpType=%s", extraInfo, Integer.valueOf(apnIpType)));
                }
                native_agps_data_conn_open(extraInfo, apnIpType);
                this.mAGpsDataConnectionState = 2;
                return;
            }
            handleReleaseSuplConnection(5);
        }
    }

    private void handleRequestSuplConnection(InetAddress inetAddress) {
        if (DEBUG) {
            Log.d("GnssLocationProvider", String.format("requestSuplConnection, state=%s, address=%s", agpsDataConnStateAsString(), inetAddress));
        }
        if (this.mAGpsDataConnectionState != 0) {
            return;
        }
        this.mAGpsDataConnectionIpAddr = inetAddress;
        this.mAGpsDataConnectionState = 1;
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addTransportType(0);
        builder.addCapability(1);
        this.mConnMgr.requestNetwork(builder.build(), this.mSuplConnectivityCallback);
    }

    private void handleReleaseSuplConnection(int i) {
        if (DEBUG) {
            Log.d("GnssLocationProvider", String.format("releaseSuplConnection, state=%s, status=%s", agpsDataConnStateAsString(), agpsDataConnStatusAsString(i)));
        }
        if (this.mAGpsDataConnectionState == 0) {
            return;
        }
        this.mAGpsDataConnectionState = 0;
        this.mConnMgr.unregisterNetworkCallback(this.mSuplConnectivityCallback);
        if (i == 2) {
            native_agps_data_conn_closed();
            return;
        }
        if (i == 5) {
            native_agps_data_conn_failed();
            return;
        }
        Log.e("GnssLocationProvider", "Invalid status to release SUPL connection: " + i);
    }

    private void handleRequestLocation(boolean z) {
        final String str;
        LocationChangeListener locationChangeListener;
        if (isRequestLocationRateLimited()) {
            if (DEBUG) {
                Log.d("GnssLocationProvider", "RequestLocation is denied due to too frequent requests.");
                return;
            }
            return;
        }
        long j = Settings.Global.getLong(this.mContext.getContentResolver(), "gnss_hal_location_request_duration_millis", 0L);
        if (j == 0) {
            Log.i("GnssLocationProvider", "GNSS HAL location request is disabled by Settings.");
            return;
        }
        final LocationManager locationManager = (LocationManager) this.mContext.getSystemService("location");
        if (z) {
            str = "network";
            locationChangeListener = this.mNetworkLocationListener;
            mtkInjectLastKnownLocation();
        } else {
            str = "fused";
            locationChangeListener = this.mFusedLocationListener;
        }
        final LocationChangeListener locationChangeListener2 = locationChangeListener;
        Log.i("GnssLocationProvider", String.format("GNSS HAL Requesting location updates from %s provider for %d millis.", str, Long.valueOf(j)));
        try {
            locationManager.requestLocationUpdates(str, 1000L, 0.0f, locationChangeListener2, this.mHandler.getLooper());
            locationChangeListener2.numLocationUpdateRequest++;
            this.mHandler.postDelayed(new Runnable() {
                @Override
                public final void run() {
                    GnssLocationProvider.lambda$handleRequestLocation$1(locationChangeListener2, str, locationManager);
                }
            }, j);
        } catch (IllegalArgumentException e) {
            Log.w("GnssLocationProvider", "Unable to request location.", e);
        }
    }

    static void lambda$handleRequestLocation$1(LocationChangeListener locationChangeListener, String str, LocationManager locationManager) {
        int i = locationChangeListener.numLocationUpdateRequest - 1;
        locationChangeListener.numLocationUpdateRequest = i;
        if (i == 0) {
            Log.i("GnssLocationProvider", String.format("Removing location updates from %s provider.", str));
            locationManager.removeUpdates(locationChangeListener);
        }
    }

    private void injectBestLocation(Location location) {
        if (location.isFromMockProvider()) {
            return;
        }
        native_inject_best_location(1 | (location.hasAltitude() ? 2 : 0) | (location.hasSpeed() ? 4 : 0) | (location.hasBearing() ? 8 : 0) | (location.hasAccuracy() ? 16 : 0) | (location.hasVerticalAccuracy() ? 32 : 0) | (location.hasSpeedAccuracy() ? 64 : 0) | (location.hasBearingAccuracy() ? 128 : 0), location.getLatitude(), location.getLongitude(), location.getAltitude(), location.getSpeed(), location.getBearing(), location.getAccuracy(), location.getVerticalAccuracyMeters(), location.getSpeedAccuracyMetersPerSecond(), location.getBearingAccuracyDegrees(), location.getTime());
    }

    private boolean isRequestLocationRateLimited() {
        return false;
    }

    private void handleDownloadXtraData() {
        if (!this.mSupportsXtra) {
            Log.d("GnssLocationProvider", "handleDownloadXtraData() called when Xtra not supported");
            return;
        }
        if (this.mDownloadXtraDataPending == 1) {
            return;
        }
        if (!isDataNetworkConnected()) {
            this.mDownloadXtraDataPending = 0;
            return;
        }
        this.mDownloadXtraDataPending = 1;
        this.mDownloadXtraWakeLock.acquire(60000L);
        Log.i("GnssLocationProvider", "WakeLock acquired by handleDownloadXtraData()");
        AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                byte[] bArrDownloadXtraData = new GpsXtraDownloader(GnssLocationProvider.this.mProperties).downloadXtraData();
                if (bArrDownloadXtraData != null) {
                    if (GnssLocationProvider.DEBUG) {
                        Log.d("GnssLocationProvider", "calling native_inject_xtra_data");
                    }
                    GnssLocationProvider.this.native_inject_xtra_data(bArrDownloadXtraData, bArrDownloadXtraData.length);
                    GnssLocationProvider.this.mXtraBackOff.reset();
                }
                GnssLocationProvider.this.sendMessage(11, 0, null);
                if (bArrDownloadXtraData == null) {
                    GnssLocationProvider.this.mHandler.sendEmptyMessageDelayed(6, GnssLocationProvider.this.mXtraBackOff.nextBackoffMillis());
                }
                synchronized (GnssLocationProvider.this.mLock) {
                    if (GnssLocationProvider.this.mDownloadXtraWakeLock.isHeld()) {
                        try {
                            GnssLocationProvider.this.mDownloadXtraWakeLock.release();
                            if (GnssLocationProvider.DEBUG) {
                                Log.d("GnssLocationProvider", "WakeLock released by handleDownloadXtraData()");
                            }
                        } catch (Exception e) {
                            Log.i("GnssLocationProvider", "Wakelock timeout & release race exception in handleDownloadXtraData()", e);
                        }
                    } else {
                        Log.e("GnssLocationProvider", "WakeLock expired before release in handleDownloadXtraData()");
                    }
                }
            }
        });
    }

    private void handleUpdateLocation(Location location) {
        if (!location.isFromMockProvider() && location.hasAccuracy()) {
            native_inject_location(location.getLatitude(), location.getLongitude(), location.getAccuracy());
        }
    }

    @Override
    public void enable() {
        sendMessage(2, 1, null);
    }

    private void setSuplHostPort(String str, String str2) {
        if (str != null) {
            this.mSuplServerHost = str;
        }
        if (str2 != null) {
            try {
                this.mSuplServerPort = Integer.parseInt(str2);
            } catch (NumberFormatException e) {
                Log.e("GnssLocationProvider", "unable to parse SUPL_PORT: " + str2);
            }
        }
        if (this.mSuplServerHost != null && this.mSuplServerPort > 0 && this.mSuplServerPort <= 65535) {
            native_set_agps_server(1, this.mSuplServerHost, this.mSuplServerPort);
        }
    }

    private int getSuplMode(Properties properties, boolean z, boolean z2) {
        int i;
        if (z) {
            String property = properties.getProperty("SUPL_MODE");
            if (!TextUtils.isEmpty(property)) {
                try {
                    i = Integer.parseInt(property);
                } catch (NumberFormatException e) {
                    Log.e("GnssLocationProvider", "unable to parse SUPL_MODE: " + property);
                    return 0;
                }
            } else {
                i = 0;
            }
            if (hasCapability(2) && (i & 1) != 0) {
                return 1;
            }
            if (z2 && hasCapability(4) && (i & 2) != 0) {
                return 2;
            }
        }
        return 0;
    }

    private void handleEnable() {
        if (DEBUG) {
            Log.d("GnssLocationProvider", "handleEnable");
        }
        synchronized (this.mLock) {
            if (this.mEnabled) {
                return;
            }
            this.mEnabled = true;
            if (native_init()) {
                this.mSupportsXtra = native_supports_xtra();
                if (this.mSuplServerHost != null) {
                    native_set_agps_server(1, this.mSuplServerHost, this.mSuplServerPort);
                }
                if (this.mC2KServerHost != null) {
                    native_set_agps_server(2, this.mC2KServerHost, this.mC2KServerPort);
                }
                this.mGnssMeasurementsProvider.onGpsEnabledChanged();
                this.mGnssNavigationMessageProvider.onGpsEnabledChanged();
                this.mGnssBatchingProvider.enable();
                return;
            }
            synchronized (this.mLock) {
                this.mEnabled = false;
            }
            Log.w("GnssLocationProvider", "Failed to enable location provider");
        }
    }

    @Override
    public void disable() {
        sendMessage(2, 0, null);
    }

    private void handleDisable() {
        if (DEBUG) {
            Log.d("GnssLocationProvider", "handleDisable");
        }
        synchronized (this.mLock) {
            if (this.mEnabled) {
                this.mEnabled = false;
                updateClientUids(new WorkSource());
                stopNavigating();
                this.mAlarmManager.cancel(this.mWakeupIntent);
                this.mAlarmManager.cancel(this.mTimeoutIntent);
                this.mGnssBatchingProvider.disable();
                native_cleanup();
                this.mGnssMeasurementsProvider.onGpsEnabledChanged();
                this.mGnssNavigationMessageProvider.onGpsEnabledChanged();
            }
        }
    }

    @Override
    public boolean isEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mEnabled;
        }
        return z;
    }

    @Override
    public int getStatus(Bundle bundle) {
        this.mLocationExtras.setBundle(bundle);
        return this.mStatus;
    }

    private void updateStatus(int i) {
        if (i != this.mStatus) {
            this.mStatus = i;
            this.mStatusUpdateTime = SystemClock.elapsedRealtime();
        }
    }

    @Override
    public long getStatusUpdateTime() {
        return this.mStatusUpdateTime;
    }

    @Override
    public void setRequest(ProviderRequest providerRequest, WorkSource workSource) {
        sendMessage(3, 0, new GpsRequest(providerRequest, workSource));
    }

    private void handleSetRequest(ProviderRequest providerRequest, WorkSource workSource) {
        this.mProviderRequest = providerRequest;
        this.mWorkSource = workSource;
        updateRequirements();
    }

    private void updateRequirements() {
        if (this.mProviderRequest == null || this.mWorkSource == null) {
            return;
        }
        boolean z = false;
        if (this.mProviderRequest.locationRequests != null && this.mProviderRequest.locationRequests.size() > 0) {
            Iterator it = this.mProviderRequest.locationRequests.iterator();
            boolean z2 = true;
            while (it.hasNext()) {
                if (((LocationRequest) it.next()).getNumUpdates() != 1) {
                    z2 = false;
                }
            }
            z = z2;
        }
        Log.d("GnssLocationProvider", "setRequest " + this.mProviderRequest);
        if (this.mProviderRequest.reportLocation && !this.mDisableGps && isEnabled()) {
            updateClientUids(this.mWorkSource);
            this.mFixInterval = (int) this.mProviderRequest.interval;
            this.mLowPowerMode = this.mProviderRequest.lowPowerMode;
            if (this.mFixInterval != this.mProviderRequest.interval) {
                Log.w("GnssLocationProvider", "interval overflow: " + this.mProviderRequest.interval);
                this.mFixInterval = Integer.MAX_VALUE;
            }
            if (this.mStarted && hasCapability(1)) {
                if (!native_set_position_mode(this.mPositionMode, 0, this.mFixInterval, 0, 0, this.mLowPowerMode)) {
                    Log.e("GnssLocationProvider", "set_position_mode failed in updateRequirements");
                    return;
                }
                return;
            } else {
                if (!this.mStarted) {
                    startNavigating(z);
                    return;
                }
                this.mAlarmManager.cancel(this.mTimeoutIntent);
                if (this.mFixInterval >= NO_FIX_TIMEOUT) {
                    this.mAlarmManager.set(2, SystemClock.elapsedRealtime() + 60000, this.mTimeoutIntent);
                    return;
                }
                return;
            }
        }
        updateClientUids(new WorkSource());
        stopNavigating();
        this.mAlarmManager.cancel(this.mWakeupIntent);
        this.mAlarmManager.cancel(this.mTimeoutIntent);
    }

    private void updateClientUids(WorkSource workSource) {
        if (workSource.equals(this.mClientSource)) {
            return;
        }
        try {
            this.mBatteryStats.noteGpsChanged(this.mClientSource, workSource);
        } catch (RemoteException e) {
            Log.w("GnssLocationProvider", "RemoteException", e);
        }
        ArrayList[] arrayListArrDiffChains = WorkSource.diffChains(this.mClientSource, workSource);
        if (arrayListArrDiffChains != null) {
            ArrayList arrayList = arrayListArrDiffChains[0];
            ArrayList arrayList2 = arrayListArrDiffChains[1];
            if (arrayList != null) {
                for (int i = 0; i < arrayList.size(); i++) {
                    WorkSource.WorkChain workChain = (WorkSource.WorkChain) arrayList.get(i);
                    this.mAppOps.startOpNoThrow(2, workChain.getAttributionUid(), workChain.getAttributionTag());
                }
            }
            if (arrayList2 != null) {
                for (int i2 = 0; i2 < arrayList2.size(); i2++) {
                    WorkSource.WorkChain workChain2 = (WorkSource.WorkChain) arrayList2.get(i2);
                    this.mAppOps.finishOp(2, workChain2.getAttributionUid(), workChain2.getAttributionTag());
                }
            }
            this.mClientSource.transferWorkChains(workSource);
        }
        WorkSource[] returningDiffs = this.mClientSource.setReturningDiffs(workSource);
        if (returningDiffs != null) {
            WorkSource workSource2 = returningDiffs[0];
            WorkSource workSource3 = returningDiffs[1];
            if (workSource2 != null) {
                for (int i3 = 0; i3 < workSource2.size(); i3++) {
                    this.mAppOps.startOpNoThrow(2, workSource2.get(i3), workSource2.getName(i3));
                }
            }
            if (workSource3 != null) {
                for (int i4 = 0; i4 < workSource3.size(); i4++) {
                    this.mAppOps.finishOp(2, workSource3.get(i4), workSource3.getName(i4));
                }
            }
        }
    }

    @Override
    public boolean sendExtraCommand(String str, Bundle bundle) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        boolean zDeleteAidingData = false;
        try {
            if ("delete_aiding_data".equals(str)) {
                zDeleteAidingData = deleteAidingData(bundle);
            } else {
                if ("force_time_injection".equals(str)) {
                    requestUtcTime();
                } else if ("force_xtra_injection".equals(str)) {
                    if (this.mSupportsXtra) {
                        xtraDownloadRequest();
                    }
                } else {
                    Log.w("GnssLocationProvider", "sendExtraCommand: unknown command " + str);
                }
                zDeleteAidingData = true;
            }
            return zDeleteAidingData;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private boolean deleteAidingData(Bundle bundle) {
        int i = NetworkConstants.ARP_HWTYPE_RESERVED_HI;
        if (bundle != null) {
            int i2 = bundle.getBoolean("ephemeris") ? 1 : 0;
            if (bundle.getBoolean("almanac")) {
                i2 |= 2;
            }
            if (bundle.getBoolean("position")) {
                i2 |= 4;
            }
            if (bundle.getBoolean("time")) {
                i2 |= 8;
            }
            if (bundle.getBoolean("iono")) {
                i2 |= 16;
            }
            if (bundle.getBoolean("utc")) {
                i2 |= 32;
            }
            if (bundle.getBoolean("health")) {
                i2 |= 64;
            }
            if (bundle.getBoolean("svdir")) {
                i2 |= 128;
            }
            if (bundle.getBoolean("svsteer")) {
                i2 |= 256;
            }
            if (bundle.getBoolean("sadata")) {
                i2 |= 512;
            }
            if (bundle.getBoolean("rti")) {
                i2 |= 1024;
            }
            if (bundle.getBoolean("celldb-info")) {
                i2 |= 32768;
            }
            i = bundle.getBoolean("all") ? 65535 | i2 : i2;
        }
        int iMtkDeleteAidingData = mtkDeleteAidingData(bundle, i);
        if (iMtkDeleteAidingData == 0) {
            return false;
        }
        native_delete_aiding_data(iMtkDeleteAidingData);
        return true;
    }

    private void startNavigating(boolean z) {
        String str;
        if (!this.mStarted) {
            Log.d("GnssLocationProvider", "startNavigating, singleShot is " + z + " setRequest: " + this.mProviderRequest);
            this.mTimeToFirstFix = 0;
            this.mLastFixTime = 0L;
            this.mStarted = true;
            this.mSingleShot = z;
            this.mPositionMode = 0;
            if (this.mItarSpeedLimitExceeded) {
                Log.i("GnssLocationProvider", "startNavigating with ITAR limit in place. Output limited  until slow enough speed reported.");
            }
            this.mPositionMode = getSuplMode(this.mProperties, Settings.Global.getInt(this.mContext.getContentResolver(), "assisted_gps_enabled", 1) != 0, z);
            if (DEBUG) {
                switch (this.mPositionMode) {
                    case 0:
                        str = "standalone";
                        break;
                    case 1:
                        str = "MS_BASED";
                        break;
                    case 2:
                        str = "MS_ASSISTED";
                        break;
                    default:
                        str = UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
                        break;
                }
                Log.d("GnssLocationProvider", "setting position_mode to " + str);
            }
            int i = hasCapability(1) ? this.mFixInterval : 1000;
            this.mLowPowerMode = this.mProviderRequest.lowPowerMode;
            if (!native_set_position_mode(this.mPositionMode, 0, i, 0, 0, this.mLowPowerMode)) {
                this.mStarted = false;
                Log.e("GnssLocationProvider", "set_position_mode failed in startNavigating()");
                return;
            }
            if (!native_start()) {
                this.mStarted = false;
                Log.e("GnssLocationProvider", "native_start failed in startNavigating()");
                return;
            }
            updateStatus(1);
            this.mLocationExtras.reset();
            this.mFixRequestTime = SystemClock.elapsedRealtime();
            if (!hasCapability(1) && this.mFixInterval >= NO_FIX_TIMEOUT) {
                this.mAlarmManager.set(2, SystemClock.elapsedRealtime() + 60000, this.mTimeoutIntent);
            }
        }
    }

    private void stopNavigating() {
        if (DEBUG) {
            Log.d("GnssLocationProvider", "stopNavigating");
        }
        if (this.mStarted) {
            this.mStarted = false;
            this.mSingleShot = false;
            native_stop();
            this.mLastFixTime = 0L;
            updateStatus(1);
            this.mLocationExtras.reset();
        }
    }

    private void hibernate() {
        stopNavigating();
        this.mAlarmManager.cancel(this.mTimeoutIntent);
        this.mAlarmManager.cancel(this.mWakeupIntent);
        this.mAlarmManager.set(2, SystemClock.elapsedRealtime() + ((long) this.mFixInterval), this.mWakeupIntent);
    }

    private boolean hasCapability(int i) {
        return (i & this.mEngineCapabilities) != 0;
    }

    private void reportLocation(boolean z, Location location) {
        sendMessage(17, z ? 1 : 0, location);
    }

    private void handleReportLocation(boolean z, Location location) {
        if (location.hasSpeed()) {
            this.mItarSpeedLimitExceeded = location.getSpeed() > ITAR_SPEED_LIMIT_METERS_PER_SECOND;
        }
        if (this.mItarSpeedLimitExceeded) {
            Log.i("GnssLocationProvider", "Hal reported a speed in excess of ITAR limit.  GPS/GNSS Navigation output blocked.");
            if (this.mStarted) {
                this.mGnssMetrics.logReceivedLocationStatus(false);
                return;
            }
            return;
        }
        if (VERBOSE) {
            Log.v("GnssLocationProvider", "reportLocation " + location.toString());
        }
        location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        location.setExtras(this.mLocationExtras.getBundle());
        try {
            this.mILocationManager.reportLocation(location, false);
        } catch (RemoteException e) {
            Log.e("GnssLocationProvider", "RemoteException calling reportLocation");
        }
        if (this.mStarted) {
            this.mGnssMetrics.logReceivedLocationStatus(z);
            if (z) {
                if (location.hasAccuracy()) {
                    this.mGnssMetrics.logPositionAccuracyMeters(location.getAccuracy());
                }
                if (this.mTimeToFirstFix > 0) {
                    this.mGnssMetrics.logMissedReports(this.mFixInterval, (int) (SystemClock.elapsedRealtime() - this.mLastFixTime));
                }
            }
        }
        this.mLastFixTime = SystemClock.elapsedRealtime();
        if (this.mTimeToFirstFix == 0 && z) {
            this.mTimeToFirstFix = (int) (this.mLastFixTime - this.mFixRequestTime);
            Log.d("GnssLocationProvider", "TTFF: " + this.mTimeToFirstFix);
            if (this.mStarted) {
                this.mGnssMetrics.logTimeToFirstFixMilliSecs(this.mTimeToFirstFix);
            }
            this.mListenerHelper.onFirstFix(this.mTimeToFirstFix);
        }
        if (this.mSingleShot) {
            stopNavigating();
        }
        if (this.mStarted && this.mStatus != 2) {
            if (!hasCapability(1) && this.mFixInterval < NO_FIX_TIMEOUT) {
                this.mAlarmManager.cancel(this.mTimeoutIntent);
            }
            Intent intent = new Intent("android.location.GPS_FIX_CHANGE");
            intent.putExtra("enabled", true);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            updateStatus(2);
        }
        if (!hasCapability(1) && this.mStarted && this.mFixInterval > 10000) {
            if (DEBUG) {
                Log.d("GnssLocationProvider", "got fix, hibernating");
            }
            hibernate();
        }
    }

    private void reportStatus(int i) {
        if (DEBUG) {
            Log.v("GnssLocationProvider", "reportStatus status: " + i);
        }
        boolean z = this.mNavigating;
        switch (i) {
            case 1:
                this.mNavigating = true;
                this.mEngineOn = true;
                break;
            case 2:
                this.mNavigating = false;
                break;
            case 3:
                this.mEngineOn = true;
                break;
            case 4:
                this.mEngineOn = false;
                this.mNavigating = false;
                break;
        }
        if (z != this.mNavigating) {
            this.mListenerHelper.onStatusChanged(this.mNavigating);
            Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
            intent.putExtra("enabled", this.mNavigating);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    private static class SvStatusInfo {
        public float[] mCn0s;
        public float[] mSvAzimuths;
        public float[] mSvCarrierFreqs;
        public int mSvCount;
        public float[] mSvElevations;
        public int[] mSvidWithFlags;

        private SvStatusInfo() {
        }
    }

    private void reportSvStatus(int i, int[] iArr, float[] fArr, float[] fArr2, float[] fArr3, float[] fArr4) {
        SvStatusInfo svStatusInfo = new SvStatusInfo();
        svStatusInfo.mSvCount = i;
        svStatusInfo.mSvidWithFlags = iArr;
        svStatusInfo.mCn0s = fArr;
        svStatusInfo.mSvElevations = fArr2;
        svStatusInfo.mSvAzimuths = fArr3;
        svStatusInfo.mSvCarrierFreqs = fArr4;
        sendMessage(18, 0, svStatusInfo);
    }

    private void handleReportSvStatus(SvStatusInfo svStatusInfo) {
        this.mListenerHelper.onSvStatusChanged(svStatusInfo.mSvCount, svStatusInfo.mSvidWithFlags, svStatusInfo.mCn0s, svStatusInfo.mSvElevations, svStatusInfo.mSvAzimuths, svStatusInfo.mSvCarrierFreqs);
        this.mGnssMetrics.logCn0(svStatusInfo.mCn0s, svStatusInfo.mSvCount);
        if (VERBOSE) {
            Log.v("GnssLocationProvider", "SV count: " + svStatusInfo.mSvCount);
        }
        int i = 0;
        int i2 = 0;
        int i3 = 0;
        for (int i4 = 0; i4 < svStatusInfo.mSvCount; i4++) {
            if ((svStatusInfo.mSvidWithFlags[i4] & 4) != 0) {
                i++;
                if (svStatusInfo.mCn0s[i4] > i2) {
                    i2 = (int) svStatusInfo.mCn0s[i4];
                }
                i3 = (int) (i3 + svStatusInfo.mCn0s[i4]);
            }
            if (VERBOSE) {
                StringBuilder sb = new StringBuilder();
                sb.append("svid: ");
                sb.append(svStatusInfo.mSvidWithFlags[i4] >> 8);
                sb.append(" cn0: ");
                sb.append(svStatusInfo.mCn0s[i4]);
                sb.append(" elev: ");
                sb.append(svStatusInfo.mSvElevations[i4]);
                sb.append(" azimuth: ");
                sb.append(svStatusInfo.mSvAzimuths[i4]);
                sb.append(" carrier frequency: ");
                sb.append(svStatusInfo.mSvCarrierFreqs[i4]);
                sb.append((1 & svStatusInfo.mSvidWithFlags[i4]) == 0 ? "  " : " E");
                sb.append((2 & svStatusInfo.mSvidWithFlags[i4]) == 0 ? "  " : " A");
                sb.append((svStatusInfo.mSvidWithFlags[i4] & 4) == 0 ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : "U");
                sb.append((svStatusInfo.mSvidWithFlags[i4] & 8) == 0 ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : "F");
                Log.v("GnssLocationProvider", sb.toString());
            }
        }
        if (i > 0) {
            i3 /= i;
        }
        this.mLocationExtras.set(i, i3, i2);
        if (this.mNavigating && this.mStatus == 2 && this.mLastFixTime > 0 && SystemClock.elapsedRealtime() - this.mLastFixTime > 10000) {
            Intent intent = new Intent("android.location.GPS_FIX_CHANGE");
            intent.putExtra("enabled", false);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            updateStatus(1);
        }
    }

    private void reportAGpsStatus(int i, int i2, byte[] bArr) {
        InetAddress byAddress;
        UnknownHostException e;
        switch (i2) {
            case 1:
                if (DEBUG) {
                    Log.d("GnssLocationProvider", "GPS_REQUEST_AGPS_DATA_CONN");
                }
                Log.v("GnssLocationProvider", "Received SUPL IP addr[]: " + Arrays.toString(bArr));
                if (bArr != null) {
                    try {
                        byAddress = InetAddress.getByAddress(bArr);
                        try {
                            if (DEBUG) {
                                Log.d("GnssLocationProvider", "IP address converted to: " + byAddress);
                            }
                        } catch (UnknownHostException e2) {
                            e = e2;
                            Log.e("GnssLocationProvider", "Bad IP Address: " + bArr, e);
                        }
                    } catch (UnknownHostException e3) {
                        byAddress = null;
                        e = e3;
                    }
                } else {
                    byAddress = null;
                }
                sendMessage(14, 0, byAddress);
                break;
            case 2:
                if (DEBUG) {
                    Log.d("GnssLocationProvider", "GPS_RELEASE_AGPS_DATA_CONN");
                }
                releaseSuplConnection(2);
                break;
            case 3:
                if (DEBUG) {
                    Log.d("GnssLocationProvider", "GPS_AGPS_DATA_CONNECTED");
                }
                break;
            case 4:
                if (DEBUG) {
                    Log.d("GnssLocationProvider", "GPS_AGPS_DATA_CONN_DONE");
                }
                break;
            case 5:
                if (DEBUG) {
                    Log.d("GnssLocationProvider", "GPS_AGPS_DATA_CONN_FAILED");
                }
                break;
            default:
                if (DEBUG) {
                    Log.d("GnssLocationProvider", "Received Unknown AGPS status: " + i2);
                }
                break;
        }
    }

    private void releaseSuplConnection(int i) {
        sendMessage(15, i, null);
    }

    private void reportNmea(long j) {
        if (!this.mItarSpeedLimitExceeded) {
            this.mListenerHelper.onNmeaReceived(j, new String(this.mNmeaBuffer, 0, native_read_nmea(this.mNmeaBuffer, this.mNmeaBuffer.length)));
        }
    }

    private void reportMeasurementData(final GnssMeasurementsEvent gnssMeasurementsEvent) {
        if (!this.mItarSpeedLimitExceeded) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    GnssLocationProvider.this.mGnssMeasurementsProvider.onMeasurementsAvailable(gnssMeasurementsEvent);
                }
            });
        }
    }

    private void reportNavigationMessage(final GnssNavigationMessage gnssNavigationMessage) {
        if (!this.mItarSpeedLimitExceeded) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    GnssLocationProvider.this.mGnssNavigationMessageProvider.onNavigationMessageAvailable(gnssNavigationMessage);
                }
            });
        }
    }

    private void setEngineCapabilities(final int i) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                GnssLocationProvider.this.mEngineCapabilities = i;
                if (GnssLocationProvider.this.hasCapability(16)) {
                    GnssLocationProvider.this.mNtpTimeHelper.enablePeriodicTimeInjection();
                    GnssLocationProvider.this.requestUtcTime();
                }
                GnssLocationProvider.this.mGnssMeasurementsProvider.onCapabilitiesUpdated(GnssLocationProvider.this.hasCapability(64));
                GnssLocationProvider.this.mGnssNavigationMessageProvider.onCapabilitiesUpdated(GnssLocationProvider.this.hasCapability(128));
                GnssLocationProvider.this.restartRequests();
            }
        });
    }

    private void restartRequests() {
        Log.i("GnssLocationProvider", "restartRequests");
        restartLocationRequest();
        this.mGnssMeasurementsProvider.resumeIfStarted();
        this.mGnssNavigationMessageProvider.resumeIfStarted();
        this.mGnssBatchingProvider.resumeIfStarted();
        this.mGnssGeofenceProvider.resumeIfStarted();
    }

    private void restartLocationRequest() {
        if (DEBUG) {
            Log.d("GnssLocationProvider", "restartLocationRequest");
        }
        this.mStarted = false;
        updateRequirements();
    }

    private void setGnssYearOfHardware(int i) {
        if (DEBUG) {
            Log.d("GnssLocationProvider", "setGnssYearOfHardware called with " + i);
        }
        this.mHardwareYear = i;
    }

    private void setGnssHardwareModelName(String str) {
        if (DEBUG) {
            Log.d("GnssLocationProvider", "setGnssModelName called with " + str);
        }
        this.mHardwareModelName = str;
    }

    private void reportGnssServiceDied() {
        if (DEBUG) {
            Log.d("GnssLocationProvider", "reportGnssServiceDied");
        }
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() throws Throwable {
                GnssLocationProvider.lambda$reportGnssServiceDied$2(this.f$0);
            }
        });
    }

    public static void lambda$reportGnssServiceDied$2(GnssLocationProvider gnssLocationProvider) throws Throwable {
        class_init_native();
        native_init_once();
        if (gnssLocationProvider.isEnabled()) {
            gnssLocationProvider.handleEnable();
            gnssLocationProvider.reloadGpsProperties(gnssLocationProvider.mContext, gnssLocationProvider.mProperties);
        }
    }

    public GnssSystemInfoProvider getGnssSystemInfoProvider() {
        return new GnssSystemInfoProvider() {
            @Override
            public int getGnssYearOfHardware() {
                return GnssLocationProvider.this.mHardwareYear;
            }

            @Override
            public String getGnssHardwareModelName() {
                return GnssLocationProvider.this.mHardwareModelName;
            }
        };
    }

    public GnssBatchingProvider getGnssBatchingProvider() {
        return this.mGnssBatchingProvider;
    }

    public GnssMetricsProvider getGnssMetricsProvider() {
        return new GnssMetricsProvider() {
            @Override
            public String getGnssMetricsAsProtoString() {
                return GnssLocationProvider.this.mGnssMetrics.dumpGnssMetricsAsProtoString();
            }
        };
    }

    private void reportLocationBatch(Location[] locationArr) {
        ArrayList arrayList = new ArrayList(Arrays.asList(locationArr));
        if (DEBUG) {
            Log.d("GnssLocationProvider", "Location batch of size " + locationArr.length + " reported");
        }
        try {
            this.mILocationManager.reportLocationBatch(arrayList);
        } catch (RemoteException e) {
            Log.e("GnssLocationProvider", "RemoteException calling reportLocationBatch");
        }
    }

    private void xtraDownloadRequest() {
        if (DEBUG) {
            Log.d("GnssLocationProvider", "xtraDownloadRequest");
        }
        sendMessage(6, 0, null);
    }

    private int getGeofenceStatus(int i) {
        if (i == GPS_GEOFENCE_ERROR_GENERIC) {
            return 5;
        }
        if (i == 0) {
            return 0;
        }
        if (i != 100) {
            switch (i) {
                case GPS_GEOFENCE_ERROR_INVALID_TRANSITION:
                    return 4;
                case GPS_GEOFENCE_ERROR_ID_UNKNOWN:
                    return 3;
                case GPS_GEOFENCE_ERROR_ID_EXISTS:
                    return 2;
                default:
                    return -1;
            }
        }
        return 1;
    }

    private void reportGeofenceTransition(int i, Location location, int i2, long j) {
        if (this.mGeofenceHardwareImpl == null) {
            this.mGeofenceHardwareImpl = GeofenceHardwareImpl.getInstance(this.mContext);
        }
        this.mGeofenceHardwareImpl.reportGeofenceTransition(i, location, i2, j, 0, FusedBatchOptions.SourceTechnologies.GNSS);
    }

    private void reportGeofenceStatus(int i, Location location) {
        if (this.mGeofenceHardwareImpl == null) {
            this.mGeofenceHardwareImpl = GeofenceHardwareImpl.getInstance(this.mContext);
        }
        int i2 = 1;
        if (i == 2) {
            i2 = 0;
        }
        this.mGeofenceHardwareImpl.reportGeofenceMonitorStatus(0, i2, location, FusedBatchOptions.SourceTechnologies.GNSS);
    }

    private void reportGeofenceAddStatus(int i, int i2) {
        if (this.mGeofenceHardwareImpl == null) {
            this.mGeofenceHardwareImpl = GeofenceHardwareImpl.getInstance(this.mContext);
        }
        this.mGeofenceHardwareImpl.reportGeofenceAddStatus(i, getGeofenceStatus(i2));
    }

    private void reportGeofenceRemoveStatus(int i, int i2) {
        if (this.mGeofenceHardwareImpl == null) {
            this.mGeofenceHardwareImpl = GeofenceHardwareImpl.getInstance(this.mContext);
        }
        this.mGeofenceHardwareImpl.reportGeofenceRemoveStatus(i, getGeofenceStatus(i2));
    }

    private void reportGeofencePauseStatus(int i, int i2) {
        if (this.mGeofenceHardwareImpl == null) {
            this.mGeofenceHardwareImpl = GeofenceHardwareImpl.getInstance(this.mContext);
        }
        this.mGeofenceHardwareImpl.reportGeofencePauseStatus(i, getGeofenceStatus(i2));
    }

    private void reportGeofenceResumeStatus(int i, int i2) {
        if (this.mGeofenceHardwareImpl == null) {
            this.mGeofenceHardwareImpl = GeofenceHardwareImpl.getInstance(this.mContext);
        }
        this.mGeofenceHardwareImpl.reportGeofenceResumeStatus(i, getGeofenceStatus(i2));
    }

    public INetInitiatedListener getNetInitiatedListener() {
        return this.mNetInitiatedListener;
    }

    public void reportNiNotification(int i, int i2, int i3, int i4, int i5, String str, String str2, int i6, int i7) {
        Log.i("GnssLocationProvider", "reportNiNotification: entered");
        Log.i("GnssLocationProvider", "notificationId: " + i + ", niType: " + i2 + ", notifyFlags: " + i3 + ", timeout: " + i4 + ", defaultResponse: " + i5);
        StringBuilder sb = new StringBuilder();
        sb.append("requestorId: ");
        sb.append(str);
        sb.append(", text: ");
        sb.append(str2);
        sb.append(", requestorIdEncoding: ");
        sb.append(i6);
        sb.append(", textEncoding: ");
        sb.append(i7);
        Log.i("GnssLocationProvider", sb.toString());
        GpsNetInitiatedHandler.GpsNiNotification gpsNiNotification = new GpsNetInitiatedHandler.GpsNiNotification();
        gpsNiNotification.notificationId = i;
        gpsNiNotification.niType = i2;
        gpsNiNotification.needNotify = (i3 & 1) != 0;
        gpsNiNotification.needVerify = (i3 & 2) != 0;
        gpsNiNotification.privacyOverride = (i3 & 4) != 0;
        gpsNiNotification.timeout = i4;
        gpsNiNotification.defaultResponse = i5;
        gpsNiNotification.requestorId = str;
        gpsNiNotification.text = str2;
        gpsNiNotification.requestorIdEncoding = i6;
        gpsNiNotification.textEncoding = i7;
        this.mNIHandler.handleNiNotification(gpsNiNotification);
    }

    private void requestSetID(int i) {
        String line1Number;
        TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        String str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        int i2 = 2;
        if ((i & 1) == 1) {
            String subscriberId = telephonyManager.getSubscriberId();
            if (subscriberId == null) {
                i2 = 0;
            } else {
                str = subscriberId;
                i2 = 1;
            }
        } else if ((i & 2) != 2 || (line1Number = telephonyManager.getLine1Number()) == null) {
            i2 = 0;
        } else {
            str = line1Number;
        }
        native_agps_set_id(i2, str);
    }

    private void requestLocation(boolean z) {
        if (DEBUG) {
            Log.d("GnssLocationProvider", "requestLocation. independentFromGnss: " + z);
        }
        sendMessage(16, 0, Boolean.valueOf(z));
    }

    private void requestUtcTime() {
        if (DEBUG) {
            Log.d("GnssLocationProvider", "utcTimeRequest");
        }
        sendMessage(5, 0, null);
    }

    private void requestRefLocation() {
        TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        int phoneType = telephonyManager.getPhoneType();
        if (phoneType != 1) {
            if (phoneType == 2) {
                Log.e("GnssLocationProvider", "CDMA not supported.");
                return;
            }
            return;
        }
        GsmCellLocation gsmCellLocation = (GsmCellLocation) telephonyManager.getCellLocation();
        if (gsmCellLocation != null && telephonyManager.getNetworkOperator() != null && telephonyManager.getNetworkOperator().length() > 3) {
            int i = Integer.parseInt(telephonyManager.getNetworkOperator().substring(0, 3));
            int i2 = Integer.parseInt(telephonyManager.getNetworkOperator().substring(3));
            int networkType = telephonyManager.getNetworkType();
            native_agps_set_ref_location_cellid((networkType == 3 || networkType == 8 || networkType == 9 || networkType == 10 || networkType == 15) ? 2 : 1, i, i2, gsmCellLocation.getLac(), gsmCellLocation.getCid());
            return;
        }
        Log.e("GnssLocationProvider", "Error getting cell location info.");
    }

    private void sendMessage(int i, int i2, Object obj) {
        this.mWakeLock.acquire();
        if (Log.isLoggable("GnssLocationProvider", 4)) {
            Log.i("GnssLocationProvider", "WakeLock acquired by sendMessage(" + messageIdAsString(i) + ", " + i2 + ", " + obj + ")");
        }
        this.mHandler.obtainMessage(i, i2, 1, obj).sendToTarget();
    }

    private final class ProviderHandler extends Handler {
        public ProviderHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message message) throws Throwable {
            int i = message.what;
            switch (i) {
                case 2:
                    if (message.arg1 == 1) {
                        GnssLocationProvider.this.handleEnable();
                    } else {
                        GnssLocationProvider.this.handleDisable();
                    }
                    break;
                case 3:
                    GpsRequest gpsRequest = (GpsRequest) message.obj;
                    GnssLocationProvider.this.handleSetRequest(gpsRequest.request, gpsRequest.source);
                    break;
                case 4:
                    GnssLocationProvider.this.handleUpdateNetworkState((Network) message.obj);
                    break;
                case 5:
                    GnssLocationProvider.this.mNtpTimeHelper.retrieveAndInjectNtpTime();
                    break;
                case 6:
                    GnssLocationProvider.this.handleDownloadXtraData();
                    break;
                case 7:
                    GnssLocationProvider.this.handleUpdateLocation((Location) message.obj);
                    break;
                case 11:
                    GnssLocationProvider.this.mDownloadXtraDataPending = 2;
                    break;
                case 12:
                    GnssLocationProvider.this.subscriptionOrSimChanged(GnssLocationProvider.this.mContext);
                    break;
                case 13:
                    handleInitialize();
                    break;
                case 14:
                    GnssLocationProvider.this.handleRequestSuplConnection((InetAddress) message.obj);
                    break;
                case 15:
                    GnssLocationProvider.this.handleReleaseSuplConnection(message.arg1);
                    break;
                case 16:
                    GnssLocationProvider.this.handleRequestLocation(((Boolean) message.obj).booleanValue());
                    break;
                case 17:
                    GnssLocationProvider.this.handleReportLocation(message.arg1 == 1, (Location) message.obj);
                    break;
                case 18:
                    GnssLocationProvider.this.handleReportSvStatus((SvStatusInfo) message.obj);
                    break;
            }
            if (message.arg2 == 1) {
                GnssLocationProvider.this.mWakeLock.release();
                if (Log.isLoggable("GnssLocationProvider", 4)) {
                    Log.i("GnssLocationProvider", "WakeLock released by handleMessage(" + GnssLocationProvider.this.messageIdAsString(i) + ", " + message.arg1 + ", " + message.obj + ")");
                }
            }
        }

        private void handleInitialize() throws Throwable {
            GnssLocationProvider.native_init_once();
            if (GnssLocationProvider.this.native_init()) {
                GnssLocationProvider.this.native_cleanup();
            } else {
                Log.w("GnssLocationProvider", "Native initialization failed at bootup");
            }
            GnssLocationProvider.this.reloadGpsProperties(GnssLocationProvider.this.mContext, GnssLocationProvider.this.mProperties);
            SubscriptionManager.from(GnssLocationProvider.this.mContext).addOnSubscriptionsChangedListener(GnssLocationProvider.this.mOnSubscriptionsChangedListener);
            if (!GnssLocationProvider.native_is_agps_ril_supported()) {
                if (GnssLocationProvider.DEBUG) {
                    Log.d("GnssLocationProvider", "Skipped registration for SMS/WAP-PUSH messages because AGPS Ril in GPS HAL is not supported");
                }
            } else {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction("android.intent.action.DATA_SMS_RECEIVED");
                intentFilter.addDataScheme("sms");
                intentFilter.addDataAuthority("localhost", "7275");
                GnssLocationProvider.this.mContext.registerReceiver(GnssLocationProvider.this.mBroadcastReceiver, intentFilter, null, this);
                IntentFilter intentFilter2 = new IntentFilter();
                intentFilter2.addAction("android.provider.Telephony.WAP_PUSH_RECEIVED");
                try {
                    intentFilter2.addDataType("application/vnd.omaloc-supl-init");
                } catch (IntentFilter.MalformedMimeTypeException e) {
                    Log.w("GnssLocationProvider", "Malformed SUPL init mime type");
                }
                GnssLocationProvider.this.mContext.registerReceiver(GnssLocationProvider.this.mBroadcastReceiver, intentFilter2, null, this);
            }
            IntentFilter intentFilter3 = new IntentFilter();
            intentFilter3.addAction(GnssLocationProvider.ALARM_WAKEUP);
            intentFilter3.addAction(GnssLocationProvider.ALARM_TIMEOUT);
            intentFilter3.addAction("android.os.action.POWER_SAVE_MODE_CHANGED");
            intentFilter3.addAction("android.os.action.DEVICE_IDLE_MODE_CHANGED");
            intentFilter3.addAction("android.intent.action.SCREEN_OFF");
            intentFilter3.addAction("android.intent.action.SCREEN_ON");
            intentFilter3.addAction(GnssLocationProvider.SIM_STATE_CHANGED);
            GnssLocationProvider.this.mContext.registerReceiver(GnssLocationProvider.this.mBroadcastReceiver, intentFilter3, null, this);
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            builder.addCapability(12);
            builder.addCapability(16);
            builder.removeCapability(15);
            GnssLocationProvider.this.mConnMgr.registerNetworkCallback(builder.build(), GnssLocationProvider.this.mNetworkConnectivityCallback);
            LocationManager locationManager = (LocationManager) GnssLocationProvider.this.mContext.getSystemService("location");
            LocationRequest locationRequestCreateFromDeprecatedProvider = LocationRequest.createFromDeprecatedProvider("passive", 0L, 0.0f, false);
            locationRequestCreateFromDeprecatedProvider.setHideFromAppOps(true);
            locationManager.requestLocationUpdates(locationRequestCreateFromDeprecatedProvider, new NetworkLocationListener(), getLooper());
        }
    }

    private abstract class LocationChangeListener implements LocationListener {
        int numLocationUpdateRequest;

        private LocationChangeListener() {
        }

        @Override
        public void onStatusChanged(String str, int i, Bundle bundle) {
        }

        @Override
        public void onProviderEnabled(String str) {
        }

        @Override
        public void onProviderDisabled(String str) {
        }
    }

    private final class NetworkLocationListener extends LocationChangeListener {
        private NetworkLocationListener() {
            super();
        }

        @Override
        public void onLocationChanged(Location location) {
            if ("network".equals(location.getProvider())) {
                GnssLocationProvider.this.handleUpdateLocation(location);
            }
        }
    }

    private final class FusedLocationListener extends LocationChangeListener {
        private FusedLocationListener() {
            super();
        }

        @Override
        public void onLocationChanged(Location location) {
            if ("fused".equals(location.getProvider())) {
                GnssLocationProvider.this.injectBestLocation(location);
            }
        }
    }

    private String getSelectedApn() throws Throwable {
        Throwable th;
        Cursor cursorQuery;
        Cursor cursor = null;
        try {
            try {
                cursorQuery = this.mContext.getContentResolver().query(Uri.parse("content://telephony/carriers/preferapn"), new String[]{"apn"}, null, null, "name ASC");
                if (cursorQuery != null) {
                    try {
                        if (cursorQuery.moveToFirst()) {
                            String string = cursorQuery.getString(0);
                            if (cursorQuery != null) {
                                cursorQuery.close();
                            }
                            return string;
                        }
                    } catch (Exception e) {
                        e = e;
                        Log.e("GnssLocationProvider", "Error encountered on selecting the APN.", e);
                        if (cursorQuery != null) {
                        }
                    }
                }
                Log.e("GnssLocationProvider", "No APN found to select.");
            } catch (Throwable th2) {
                th = th2;
                if (0 != 0) {
                    cursor.close();
                }
                throw th;
            }
        } catch (Exception e2) {
            e = e2;
            cursorQuery = null;
        } catch (Throwable th3) {
            th = th3;
            if (0 != 0) {
            }
            throw th;
        }
        if (cursorQuery != null) {
            cursorQuery.close();
        }
        return null;
    }

    private int getApnIpType(String str) throws Throwable {
        Cursor cursorQuery;
        Exception e;
        ensureInHandlerThread();
        if (str == null) {
            return 0;
        }
        ?? r2 = {str};
        try {
            try {
                cursorQuery = this.mContext.getContentResolver().query(Telephony.Carriers.CONTENT_URI, new String[]{"protocol"}, String.format("current = 1 and apn = '%s' and carrier_enabled = 1", r2), null, "name ASC");
                if (cursorQuery != null) {
                    try {
                        if (cursorQuery.moveToFirst()) {
                            int iTranslateToApnIpType = translateToApnIpType(cursorQuery.getString(0), str);
                            if (cursorQuery != null) {
                                cursorQuery.close();
                            }
                            return iTranslateToApnIpType;
                        }
                    } catch (Exception e2) {
                        e = e2;
                        Log.e("GnssLocationProvider", "Error encountered on APN query for: " + str, e);
                        if (cursorQuery != null) {
                        }
                    }
                }
                Log.e("GnssLocationProvider", "No entry found in query for APN: " + str);
            } catch (Throwable th) {
                th = th;
                if (r2 != 0) {
                    r2.close();
                }
                throw th;
            }
        } catch (Exception e3) {
            cursorQuery = null;
            e = e3;
        } catch (Throwable th2) {
            th = th2;
            r2 = 0;
            if (r2 != 0) {
            }
            throw th;
        }
        if (cursorQuery != null) {
            cursorQuery.close();
        }
        return 0;
    }

    private int translateToApnIpType(String str, String str2) {
        if ("IP".equals(str)) {
            return 1;
        }
        if ("IPV6".equals(str)) {
            return 2;
        }
        if ("IPV4V6".equals(str)) {
            return 3;
        }
        Log.e("GnssLocationProvider", String.format("Unknown IP Protocol: %s, for APN: %s", str, str2));
        return 0;
    }

    private void setRouting() {
        if (this.mAGpsDataConnectionIpAddr == null) {
            return;
        }
        if (!this.mConnMgr.requestRouteToHostAddress(3, this.mAGpsDataConnectionIpAddr)) {
            Log.e("GnssLocationProvider", "Error requesting route to host: " + this.mAGpsDataConnectionIpAddr);
            return;
        }
        if (DEBUG) {
            Log.d("GnssLocationProvider", "Successfully requested route to host: " + this.mAGpsDataConnectionIpAddr);
        }
    }

    private boolean isDataNetworkConnected() {
        NetworkInfo activeNetworkInfo = this.mConnMgr.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void ensureInHandlerThread() {
        if (this.mHandler != null && Looper.myLooper() == this.mHandler.getLooper()) {
        } else {
            throw new RuntimeException("This method must run on the Handler thread.");
        }
    }

    private String agpsDataConnStateAsString() {
        switch (this.mAGpsDataConnectionState) {
            case 0:
                return "CLOSED";
            case 1:
                return "OPENING";
            case 2:
                return "OPEN";
            default:
                return "<Unknown>";
        }
    }

    private String agpsDataConnStatusAsString(int i) {
        switch (i) {
            case 1:
                return "REQUEST";
            case 2:
                return "RELEASE";
            case 3:
                return "CONNECTED";
            case 4:
                return "DONE";
            case 5:
                return "FAILED";
            default:
                return "<Unknown>";
        }
    }

    private String messageIdAsString(int i) {
        switch (i) {
            case 2:
                return "ENABLE";
            case 3:
                return "SET_REQUEST";
            case 4:
                return "UPDATE_NETWORK_STATE";
            case 5:
                return "INJECT_NTP_TIME";
            case 6:
                return "DOWNLOAD_XTRA_DATA";
            case 7:
                return "UPDATE_LOCATION";
            case 8:
            case 9:
            case 10:
            default:
                return "<Unknown>";
            case 11:
                return "DOWNLOAD_XTRA_DATA_FINISHED";
            case 12:
                return "SUBSCRIPTION_OR_SIM_CHANGED";
            case 13:
                return "INITIALIZE_HANDLER";
            case 14:
                return "REQUEST_SUPL_CONNECTION";
            case 15:
                return "RELEASE_SUPL_CONNECTION";
            case 16:
                return "REQUEST_LOCATION";
            case 17:
                return "REPORT_LOCATION";
            case 18:
                return "REPORT_SV_STATUS";
        }
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        StringBuilder sb = new StringBuilder();
        sb.append("  mStarted=");
        sb.append(this.mStarted);
        sb.append('\n');
        sb.append("  mFixInterval=");
        sb.append(this.mFixInterval);
        sb.append('\n');
        sb.append("  mLowPowerMode=");
        sb.append(this.mLowPowerMode);
        sb.append('\n');
        sb.append("  mGnssMeasurementsProvider.isRegistered()=");
        sb.append(this.mGnssMeasurementsProvider.isRegistered());
        sb.append('\n');
        sb.append("  mGnssNavigationMessageProvider.isRegistered()=");
        sb.append(this.mGnssNavigationMessageProvider.isRegistered());
        sb.append('\n');
        sb.append("  mDisableGps (battery saver mode)=");
        sb.append(this.mDisableGps);
        sb.append('\n');
        sb.append("  mEngineCapabilities=0x");
        sb.append(Integer.toHexString(this.mEngineCapabilities));
        sb.append(" ( ");
        if (hasCapability(1)) {
            sb.append("SCHEDULING ");
        }
        if (hasCapability(2)) {
            sb.append("MSB ");
        }
        if (hasCapability(4)) {
            sb.append("MSA ");
        }
        if (hasCapability(8)) {
            sb.append("SINGLE_SHOT ");
        }
        if (hasCapability(16)) {
            sb.append("ON_DEMAND_TIME ");
        }
        if (hasCapability(32)) {
            sb.append("GEOFENCING ");
        }
        if (hasCapability(64)) {
            sb.append("MEASUREMENTS ");
        }
        if (hasCapability(128)) {
            sb.append("NAV_MESSAGES ");
        }
        sb.append(")\n");
        sb.append(this.mGnssMetrics.dumpGnssMetricsAsText());
        sb.append("  native internal state: ");
        sb.append(native_get_internal_state());
        sb.append("\n");
        printWriter.append((CharSequence) sb);
    }

    private void initMtkGnssLocProvider() {
        Constructor<?> constructor;
        if (SystemProperties.get("ro.vendor.mtk_gps_support").equals("1")) {
            try {
                this.mMtkGnssProviderClass = Class.forName("com.mediatek.location.MtkLocationExt$GnssLocationProvider");
                if (DEBUG) {
                    Log.d("GnssLocationProvider", "class = " + this.mMtkGnssProviderClass);
                }
                if (this.mMtkGnssProviderClass != null && (constructor = this.mMtkGnssProviderClass.getConstructor(Context.class, Handler.class)) != null) {
                    this.mMtkGnssProvider = constructor.newInstance(this.mContext, this.mHandler);
                }
                Log.d("GnssLocationProvider", "mMtkGnssProvider = " + this.mMtkGnssProvider);
                this.mDownloadXtraDataPending = 2;
                this.mNtpTimeHelper.setNtpTimeStateIdle();
            } catch (Exception e) {
                Log.w("GnssLocationProvider", "Failed to init mMtkGnssProvider!");
            }
        }
    }

    private int mtkDeleteAidingData(Bundle bundle, int i) {
        if (this.mMtkGnssProvider != null) {
            if (bundle != null) {
                if (bundle.getBoolean("hot-still")) {
                    i |= 8192;
                }
                if (bundle.getBoolean("epo")) {
                    i |= 16384;
                }
            }
            Log.d("GnssLocationProvider", "mtkDeleteAidingData extras:" + bundle + "flags:" + i);
        }
        return i;
    }

    private void mtkInjectLastKnownLocation() {
        Location lastKnownLocation;
        if (this.mMtkGnssProvider != null && (lastKnownLocation = ((LocationManager) this.mContext.getSystemService("location")).getLastKnownLocation("network")) != null && System.currentTimeMillis() - lastKnownLocation.getTime() < 600000 && isDataNetworkConnected()) {
            this.mHandler.obtainMessage(7, 0, 0, lastKnownLocation).sendToTarget();
        }
    }
}
