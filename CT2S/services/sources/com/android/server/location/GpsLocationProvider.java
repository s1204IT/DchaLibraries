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
import android.location.GpsMeasurementsEvent;
import android.location.GpsNavigationMessageEvent;
import android.location.IGpsGeofenceHardware;
import android.location.IGpsStatusListener;
import android.location.IGpsStatusProvider;
import android.location.ILocationManager;
import android.location.INetInitiatedListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationRequest;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.WorkSource;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.Log;
import android.util.NtpTrustedTime;
import com.android.internal.app.IAppOpsService;
import com.android.internal.app.IBatteryStats;
import com.android.internal.location.GpsNetInitiatedHandler;
import com.android.internal.location.ProviderProperties;
import com.android.internal.location.ProviderRequest;
import com.android.server.pm.PackageManagerService;
import com.android.server.voiceinteraction.DatabaseHelper;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import libcore.io.IoUtils;

public class GpsLocationProvider implements LocationProviderInterface {
    private static final int ADD_LISTENER = 8;
    private static final int AGPS_DATA_CONNECTION_CLOSED = 0;
    private static final int AGPS_DATA_CONNECTION_OPEN = 2;
    private static final int AGPS_DATA_CONNECTION_OPENING = 1;
    private static final int AGPS_REF_LOCATION_TYPE_GSM_CELLID = 1;
    private static final int AGPS_REF_LOCATION_TYPE_UMTS_CELLID = 2;
    private static final int AGPS_REG_LOCATION_TYPE_LTE_CELLID = 4;
    private static final int AGPS_REG_LOCATION_TYPE_MAC = 3;
    private static final int AGPS_RIL_REQUEST_REFLOC_CELLID = 1;
    private static final int AGPS_RIL_REQUEST_REFLOC_MAC = 2;
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
    private static final int ALMANAC_MASK = 1;
    private static final int APN_INVALID = 0;
    private static final int APN_IPV4 = 1;
    private static final int APN_IPV4V6 = 3;
    private static final int APN_IPV6 = 2;
    private static final String BATTERY_SAVER_GPS_MODE = "batterySaverGpsMode";
    private static final int BATTERY_SAVER_MODE_DISABLED_WHEN_SCREEN_OFF = 1;
    private static final int BATTERY_SAVER_MODE_NO_CHANGE = 0;
    private static final int CHECK_LOCATION = 1;
    private static final String DEFAULT_PROPERTIES_FILE = "/etc/gps.conf";
    private static final int DOWNLOAD_XTRA_DATA = 6;
    private static final int DOWNLOAD_XTRA_DATA_FINISHED = 11;
    private static final int ENABLE = 2;
    private static final int EPHEMERIS_MASK = 0;
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
    private static final int GPS_DELETE_HEALTH = 64;
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
    private static final int INJECT_NTP_TIME = 5;
    private static final int INJECT_NTP_TIME_FINISHED = 10;
    private static final int LOCATION_HAS_ACCURACY = 16;
    private static final int LOCATION_HAS_ALTITUDE = 2;
    private static final int LOCATION_HAS_BEARING = 8;
    private static final int LOCATION_HAS_LAT_LONG = 1;
    private static final int LOCATION_HAS_SPEED = 4;
    private static final int LOCATION_INVALID = 0;
    private static final int MAX_SVS = 32;
    private static final int NO_FIX_TIMEOUT = 60000;
    private static final long NTP_INTERVAL = 86400000;
    private static final String PROPERTIES_FILE_PREFIX = "/etc/gps";
    private static final String PROPERTIES_FILE_SUFFIX = ".conf";
    private static final long RECENT_FIX_TIMEOUT = 10000;
    private static final int REMOVE_LISTENER = 9;
    private static final long RETRY_INTERVAL = 300000;
    private static final int SET_REQUEST = 3;
    private static final String SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";
    private static final int STATE_DOWNLOADING = 1;
    private static final int STATE_IDLE = 2;
    private static final int STATE_PENDING_NETWORK = 0;
    private static final String TAG = "GpsLocationProvider";
    private static final int TCP_MAX_PORT = 65535;
    private static final int TCP_MIN_PORT = 0;
    private static final int UPDATE_LOCATION = 7;
    private static final int UPDATE_NETWORK_STATE = 4;
    private static final int USED_FOR_FIX_MASK = 2;
    private static final String WAKELOCK_KEY = "GpsLocationProvider";
    private String mAGpsApn;
    private InetAddress mAGpsDataConnectionIpAddr;
    private int mAGpsDataConnectionState;
    private final AlarmManager mAlarmManager;
    private int mApnIpType;
    private final IAppOpsService mAppOpsService;
    private final IBatteryStats mBatteryStats;
    private String mC2KServerHost;
    private int mC2KServerPort;
    private final ConnectivityManager mConnMgr;
    private final Context mContext;
    private boolean mEnabled;
    private int mEngineCapabilities;
    private boolean mEngineOn;
    private GeofenceHardwareImpl mGeofenceHardwareImpl;
    private final GpsMeasurementsProvider mGpsMeasurementsProvider;
    private final GpsNavigationMessageProvider mGpsNavigationMessageProvider;
    private Handler mHandler;
    private final ILocationManager mILocationManager;
    private long mLastFixTime;
    private final GpsStatusListenerHelper mListenerHelper;
    private final GpsNetInitiatedHandler mNIHandler;
    private boolean mNavigating;
    private boolean mNetworkAvailable;
    private final NtpTrustedTime mNtpTime;
    private boolean mPeriodicTimeInjection;
    private int mPositionMode;
    private final PowerManager mPowerManager;
    private Properties mProperties;
    private boolean mSingleShot;
    private boolean mStarted;
    private String mSuplServerHost;
    private boolean mSupportsXtra;
    private int mSvCount;
    private final PendingIntent mTimeoutIntent;
    private final PowerManager.WakeLock mWakeLock;
    private final PendingIntent mWakeupIntent;
    private static final boolean DEBUG = Log.isLoggable("GpsLocationProvider", 3);
    private static final boolean VERBOSE = Log.isLoggable("GpsLocationProvider", 2);
    private static final ProviderProperties PROPERTIES = new ProviderProperties(true, true, false, false, true, true, true, 3, 1);
    private Object mLock = new Object();
    private int mLocationFlags = 0;
    private int mStatus = 1;
    private long mStatusUpdateTime = SystemClock.elapsedRealtime();
    private int mInjectNtpTimePending = 0;
    private int mDownloadXtraDataPending = 0;
    private int mFixInterval = 1000;
    private long mFixRequestTime = 0;
    private int mTimeToFirstFix = 0;
    private ProviderRequest mProviderRequest = null;
    private WorkSource mWorkSource = null;
    private boolean mDisableGps = false;
    private int mSuplServerPort = 0;
    private boolean mSuplEsEnabled = false;
    private Location mLocation = new Location("gps");
    private Bundle mLocationExtras = new Bundle();
    private WorkSource mClientSource = new WorkSource();
    private final IGpsStatusProvider mGpsStatusProvider = new IGpsStatusProvider.Stub() {
        public void addGpsStatusListener(IGpsStatusListener listener) throws RemoteException {
            GpsLocationProvider.this.mListenerHelper.addListener(listener);
        }

        public void removeGpsStatusListener(IGpsStatusListener listener) {
            GpsLocationProvider.this.mListenerHelper.removeListener(listener);
        }
    };
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int networkState;
            String action = intent.getAction();
            if (GpsLocationProvider.DEBUG) {
                Log.d("GpsLocationProvider", "receive broadcast intent, action: " + action);
            }
            if (action.equals(GpsLocationProvider.ALARM_WAKEUP)) {
                GpsLocationProvider.this.startNavigating(false);
                return;
            }
            if (action.equals(GpsLocationProvider.ALARM_TIMEOUT)) {
                GpsLocationProvider.this.hibernate();
                return;
            }
            if (action.equals("android.intent.action.DATA_SMS_RECEIVED")) {
                GpsLocationProvider.this.checkSmsSuplInit(intent);
                return;
            }
            if (action.equals("android.provider.Telephony.WAP_PUSH_RECEIVED")) {
                GpsLocationProvider.this.checkWapSuplInit(intent);
                return;
            }
            if (action.equals("android.net.conn.CONNECTIVITY_CHANGE_IMMEDIATE")) {
                NetworkInfo info = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                ConnectivityManager connManager = (ConnectivityManager) GpsLocationProvider.this.mContext.getSystemService("connectivity");
                NetworkInfo info2 = connManager.getNetworkInfo(info.getType());
                if (intent.getBooleanExtra("noConnectivity", false) || !info2.isConnected()) {
                    networkState = 1;
                } else {
                    networkState = 2;
                }
                GpsLocationProvider.this.updateNetworkState(networkState, info2);
                return;
            }
            if ("android.os.action.POWER_SAVE_MODE_CHANGED".equals(action) || "android.intent.action.SCREEN_OFF".equals(action) || "android.intent.action.SCREEN_ON".equals(action)) {
                GpsLocationProvider.this.updateLowPowerMode();
            } else if (action.equals(GpsLocationProvider.SIM_STATE_CHANGED)) {
                GpsLocationProvider.this.subscriptionOrSimChanged(context);
            }
        }
    };
    private final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangedListener = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            GpsLocationProvider.this.subscriptionOrSimChanged(GpsLocationProvider.this.mContext);
        }
    };
    private IGpsGeofenceHardware mGpsGeofenceBinder = new IGpsGeofenceHardware.Stub() {
        public boolean isHardwareGeofenceSupported() {
            return GpsLocationProvider.native_is_geofence_supported();
        }

        public boolean addCircularHardwareGeofence(int geofenceId, double latitude, double longitude, double radius, int lastTransition, int monitorTransitions, int notificationResponsiveness, int unknownTimer) {
            return GpsLocationProvider.native_add_geofence(geofenceId, latitude, longitude, radius, lastTransition, monitorTransitions, notificationResponsiveness, unknownTimer);
        }

        public boolean removeHardwareGeofence(int geofenceId) {
            return GpsLocationProvider.native_remove_geofence(geofenceId);
        }

        public boolean pauseHardwareGeofence(int geofenceId) {
            return GpsLocationProvider.native_pause_geofence(geofenceId);
        }

        public boolean resumeHardwareGeofence(int geofenceId, int monitorTransition) {
            return GpsLocationProvider.native_resume_geofence(geofenceId, monitorTransition);
        }
    };
    private final INetInitiatedListener mNetInitiatedListener = new INetInitiatedListener.Stub() {
        public boolean sendNiResponse(int notificationId, int userResponse) {
            if (GpsLocationProvider.DEBUG) {
                Log.d("GpsLocationProvider", "sendNiResponse, notifId: " + notificationId + ", response: " + userResponse);
            }
            GpsLocationProvider.this.native_send_ni_response(notificationId, userResponse);
            return true;
        }
    };
    private int[] mSvs = new int[32];
    private float[] mSnrs = new float[32];
    private float[] mSvElevations = new float[32];
    private float[] mSvAzimuths = new float[32];
    private int[] mSvMasks = new int[3];
    private byte[] mNmeaBuffer = new byte[120];

    private static native void class_init_native();

    private static native boolean native_add_geofence(int i, double d, double d2, double d3, int i2, int i3, int i4, int i5);

    private native void native_agps_data_conn_closed();

    private native void native_agps_data_conn_failed();

    private native void native_agps_data_conn_open(String str, int i);

    private native void native_agps_ni_message(byte[] bArr, int i);

    private native void native_agps_set_id(int i, String str);

    private native void native_agps_set_ref_location_cellid(int i, int i2, int i3, int i4, int i5);

    private native void native_cleanup();

    private static native void native_configuration_update(String str);

    private native void native_delete_aiding_data(int i);

    private native String native_get_internal_state();

    private native boolean native_init();

    private native void native_inject_location(double d, double d2, float f);

    private native void native_inject_time(long j, long j2, int i);

    private native void native_inject_xtra_data(byte[] bArr, int i);

    private static native boolean native_is_geofence_supported();

    private static native boolean native_is_measurement_supported();

    private static native boolean native_is_navigation_message_supported();

    private static native boolean native_is_supported();

    private static native boolean native_pause_geofence(int i);

    private native int native_read_nmea(byte[] bArr, int i);

    private native int native_read_sv_status(int[] iArr, float[] fArr, float[] fArr2, float[] fArr3, int[] iArr2);

    private static native boolean native_remove_geofence(int i);

    private static native boolean native_resume_geofence(int i, int i2);

    private native void native_send_ni_response(int i, int i2);

    private native void native_set_agps_server(int i, String str, int i2);

    private native boolean native_set_position_mode(int i, int i2, int i3, int i4, int i5);

    private native boolean native_start();

    private native boolean native_start_measurement_collection();

    private native boolean native_start_navigation_message_collection();

    private native boolean native_stop();

    private native boolean native_stop_measurement_collection();

    private native boolean native_stop_navigation_message_collection();

    private native boolean native_supports_xtra();

    private native void native_update_network_state(boolean z, int i, boolean z2, boolean z3, String str, String str2);

    static {
        class_init_native();
    }

    private static class GpsRequest {
        public ProviderRequest request;
        public WorkSource source;

        public GpsRequest(ProviderRequest request, WorkSource source) {
            this.request = request;
            this.source = source;
        }
    }

    public IGpsStatusProvider getGpsStatusProvider() {
        return this.mGpsStatusProvider;
    }

    public IGpsGeofenceHardware getGpsGeofenceProxy() {
        return this.mGpsGeofenceBinder;
    }

    public GpsMeasurementsProvider getGpsMeasurementsProvider() {
        return this.mGpsMeasurementsProvider;
    }

    public GpsNavigationMessageProvider getGpsNavigationMessageProvider() {
        return this.mGpsNavigationMessageProvider;
    }

    private void subscriptionOrSimChanged(Context context) {
        Log.d("GpsLocationProvider", "received SIM realted action: ");
        TelephonyManager phone = (TelephonyManager) this.mContext.getSystemService("phone");
        String mccMnc = phone.getSimOperator();
        if (!TextUtils.isEmpty(mccMnc)) {
            Log.d("GpsLocationProvider", "SIM MCC/MNC is available: " + mccMnc);
            synchronized (this.mLock) {
                reloadGpsProperties(context, this.mProperties);
                this.mNIHandler.setSuplEsEnabled(this.mSuplEsEnabled);
            }
            return;
        }
        Log.d("GpsLocationProvider", "SIM MCC/MNC is still not available");
    }

    private void checkSmsSuplInit(Intent intent) {
        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        for (SmsMessage smsMessage : messages) {
            byte[] supl_init = smsMessage.getUserData();
            native_agps_ni_message(supl_init, supl_init.length);
        }
    }

    private void checkWapSuplInit(Intent intent) {
        int bValidSupl = 0;
        byte[] supl_header = (byte[]) intent.getExtra("header");
        byte[] supl_init = (byte[]) intent.getExtra(DatabaseHelper.SoundModelContract.KEY_DATA);
        int strlen = "application/vnd.omaloc-supl-init".length() + 1 + 1 + "x-oma-application:ulp.ua".length() + 1;
        if (DEBUG) {
            Log.d("GpsLocationProvider", "SUPL head length:" + supl_header.length);
            Log.d("GpsLocationProvider", "SUPL min string length:" + strlen);
            Log.d("GpsLocationProvider", "SUPL number length:6");
        }
        if (supl_header.length >= strlen) {
            int initlen = "application/vnd.omaloc-supl-init".length();
            int parmid = supl_header[0 + initlen + 1] & 255;
            if (parmid == 175) {
                int index = 0 + initlen + 1;
                int index2 = index + 1;
                byte[] app_id = "x-oma-application:ulp.ua".getBytes();
                int j = 0;
                while (j < app_id.length && supl_header[index2 + j] == app_id[j]) {
                    j++;
                }
                if (j == app_id.length && supl_header[index2 + j] == 0) {
                    bValidSupl = 1;
                } else {
                    Log.w("GpsLocationProvider", "Wrong SUPL APPLICATION ID");
                }
            } else {
                Log.w("GpsLocationProvider", "Wrong Application ID field name (" + parmid + ") (should be 0xAF)");
            }
        } else if (supl_header.length == 6) {
            int initlen2 = supl_header[0];
            int parmid2 = supl_header[0 + initlen2 + 1] & 255;
            if (parmid2 == 175) {
                int index3 = 0 + initlen2 + 1;
                int appid = supl_header[index3 + 1] & 255;
                if (appid == 144) {
                    bValidSupl = 1;
                } else {
                    Log.w("GpsLocationProvider", "Wrong SUPL APPLICATION ID:" + appid);
                }
            } else {
                Log.w("GpsLocationProvider", "Wrong Application ID field name (" + parmid2 + ") (should be 0xAF)");
            }
        } else {
            Log.w("GpsLocationProvider", "Wrong SUPL APPLICATION ID!!!");
        }
        if (bValidSupl == 1) {
            if (DEBUG) {
                Log.d("GpsLocationProvider", "native_agps_ni_message will be called");
            }
            native_agps_ni_message(supl_init, supl_init.length);
        }
    }

    private void updateLowPowerMode() {
        boolean disableGps = true;
        switch (Settings.Secure.getInt(this.mContext.getContentResolver(), BATTERY_SAVER_GPS_MODE, 1)) {
            case 1:
                if (!this.mPowerManager.isPowerSaveMode() || this.mPowerManager.isInteractive()) {
                    disableGps = false;
                }
                break;
            default:
                disableGps = false;
                break;
        }
        if (disableGps != this.mDisableGps) {
            this.mDisableGps = disableGps;
            updateRequirements();
        }
    }

    public static boolean isSupported() {
        return native_is_supported();
    }

    private void reloadGpsProperties(Context context, Properties properties) throws Throwable {
        Log.d("GpsLocationProvider", "Reset GPS properties, previous size = " + properties.size());
        loadPropertiesFromResource(context, properties);
        boolean isPropertiesLoadedFromFile = false;
        String gpsHardware = SystemProperties.get("ro.hardware.gps");
        if (!TextUtils.isEmpty(gpsHardware)) {
            String propFilename = "/etc/gps." + gpsHardware + PROPERTIES_FILE_SUFFIX;
            isPropertiesLoadedFromFile = loadPropertiesFromFile(propFilename, properties);
        }
        if (!isPropertiesLoadedFromFile) {
            loadPropertiesFromFile(DEFAULT_PROPERTIES_FILE, properties);
        }
        Log.d("GpsLocationProvider", "GPS properties reloaded, size = " + properties.size());
        setSuplHostPort(properties.getProperty("SUPL_HOST"), properties.getProperty("SUPL_PORT"));
        this.mC2KServerHost = properties.getProperty("C2K_HOST");
        String portString = properties.getProperty("C2K_PORT");
        if (this.mC2KServerHost != null && portString != null) {
            try {
                this.mC2KServerPort = Integer.parseInt(portString);
            } catch (NumberFormatException e) {
                Log.e("GpsLocationProvider", "unable to parse C2K_PORT: " + portString);
            }
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(PackageManagerService.DumpState.DUMP_VERSION);
            properties.store(baos, (String) null);
            native_configuration_update(baos.toString());
            Log.d("GpsLocationProvider", "final config = " + baos.toString());
        } catch (IOException e2) {
            Log.w("GpsLocationProvider", "failed to dump properties contents");
        }
        String suplESProperty = this.mProperties.getProperty("SUPL_ES");
        if (suplESProperty != null) {
            try {
                this.mSuplEsEnabled = Integer.parseInt(suplESProperty) == 1;
            } catch (NumberFormatException e3) {
                Log.e("GpsLocationProvider", "unable to parse SUPL_ES: " + suplESProperty);
            }
        }
    }

    private void loadPropertiesFromResource(Context context, Properties properties) {
        String[] configValues = context.getResources().getStringArray(R.array.config_defaultAllowlistLaunchOnPrivateDisplayPackages);
        for (String item : configValues) {
            Log.d("GpsLocationProvider", "GpsParamsResource: " + item);
            String[] split = item.split("=");
            if (split.length == 2) {
                properties.setProperty(split[0].trim().toUpperCase(), split[1]);
            } else {
                Log.w("GpsLocationProvider", "malformed contents: " + item);
            }
        }
    }

    private boolean loadPropertiesFromFile(String filename, Properties properties) throws Throwable {
        try {
            File file = new File(filename);
            FileInputStream stream = null;
            try {
                FileInputStream stream2 = new FileInputStream(file);
                try {
                    properties.load(stream2);
                    IoUtils.closeQuietly(stream2);
                    return true;
                } catch (Throwable th) {
                    th = th;
                    stream = stream2;
                    IoUtils.closeQuietly(stream);
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (IOException e) {
            Log.w("GpsLocationProvider", "Could not open GPS configuration file " + filename);
            return false;
        }
    }

    public GpsLocationProvider(Context context, ILocationManager ilocationManager, Looper looper) throws Throwable {
        this.mContext = context;
        this.mNtpTime = NtpTrustedTime.getInstance(context);
        this.mILocationManager = ilocationManager;
        this.mLocation.setExtras(this.mLocationExtras);
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mWakeLock = this.mPowerManager.newWakeLock(1, "GpsLocationProvider");
        this.mWakeLock.setReferenceCounted(true);
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mWakeupIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(ALARM_WAKEUP), 0);
        this.mTimeoutIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(ALARM_TIMEOUT), 0);
        this.mConnMgr = (ConnectivityManager) context.getSystemService("connectivity");
        this.mAppOpsService = IAppOpsService.Stub.asInterface(ServiceManager.getService("appops"));
        this.mBatteryStats = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
        this.mProperties = new Properties();
        reloadGpsProperties(this.mContext, this.mProperties);
        this.mNIHandler = new GpsNetInitiatedHandler(context, this.mNetInitiatedListener, this.mSuplEsEnabled);
        SubscriptionManager.from(this.mContext).addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
        this.mHandler = new ProviderHandler(looper);
        listenForBroadcasts();
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                LocationManager locManager = (LocationManager) GpsLocationProvider.this.mContext.getSystemService("location");
                LocationRequest request = LocationRequest.createFromDeprecatedProvider("passive", 0L, 0.0f, false);
                request.setHideFromAppOps(true);
                locManager.requestLocationUpdates(request, new NetworkLocationListener(), GpsLocationProvider.this.mHandler.getLooper());
            }
        });
        this.mListenerHelper = new GpsStatusListenerHelper(this.mHandler) {
            @Override
            protected boolean isAvailableInPlatform() {
                return GpsLocationProvider.isSupported();
            }

            @Override
            protected boolean isGpsEnabled() {
                return GpsLocationProvider.this.isEnabled();
            }
        };
        this.mGpsMeasurementsProvider = new GpsMeasurementsProvider(this.mHandler) {
            @Override
            public boolean isAvailableInPlatform() {
                return GpsLocationProvider.native_is_measurement_supported();
            }

            @Override
            protected boolean registerWithService() {
                return GpsLocationProvider.this.native_start_measurement_collection();
            }

            @Override
            protected void unregisterFromService() {
                GpsLocationProvider.this.native_stop_measurement_collection();
            }

            @Override
            protected boolean isGpsEnabled() {
                return GpsLocationProvider.this.isEnabled();
            }
        };
        this.mGpsNavigationMessageProvider = new GpsNavigationMessageProvider(this.mHandler) {
            @Override
            protected boolean isAvailableInPlatform() {
                return GpsLocationProvider.native_is_navigation_message_supported();
            }

            @Override
            protected boolean registerWithService() {
                return GpsLocationProvider.this.native_start_navigation_message_collection();
            }

            @Override
            protected void unregisterFromService() {
                GpsLocationProvider.this.native_stop_navigation_message_collection();
            }

            @Override
            protected boolean isGpsEnabled() {
                return GpsLocationProvider.this.isEnabled();
            }
        };
    }

    private void listenForBroadcasts() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.DATA_SMS_RECEIVED");
        intentFilter.addDataScheme("sms");
        intentFilter.addDataAuthority("localhost", "7275");
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter, null, this.mHandler);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.provider.Telephony.WAP_PUSH_RECEIVED");
        try {
            intentFilter2.addDataType("application/vnd.omaloc-supl-init");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            Log.w("GpsLocationProvider", "Malformed SUPL init mime type");
        }
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter2, null, this.mHandler);
        IntentFilter intentFilter3 = new IntentFilter();
        intentFilter3.addAction(ALARM_WAKEUP);
        intentFilter3.addAction(ALARM_TIMEOUT);
        intentFilter3.addAction("android.net.conn.CONNECTIVITY_CHANGE_IMMEDIATE");
        intentFilter3.addAction("android.os.action.POWER_SAVE_MODE_CHANGED");
        intentFilter3.addAction("android.intent.action.SCREEN_OFF");
        intentFilter3.addAction("android.intent.action.SCREEN_ON");
        intentFilter3.addAction(SIM_STATE_CHANGED);
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter3, null, this.mHandler);
    }

    @Override
    public String getName() {
        return "gps";
    }

    @Override
    public ProviderProperties getProperties() {
        return PROPERTIES;
    }

    public void updateNetworkState(int state, NetworkInfo info) {
        sendMessage(4, state, info);
    }

    private void handleUpdateNetworkState(int state, NetworkInfo info) {
        this.mNetworkAvailable = state == 2;
        if (DEBUG) {
            Log.d("GpsLocationProvider", "updateNetworkState " + (this.mNetworkAvailable ? "available" : "unavailable") + " info: " + info);
        }
        if (info != null) {
            boolean dataEnabled = TelephonyManager.getDefault().getDataEnabled();
            boolean networkAvailable = info.isAvailable() && dataEnabled;
            String defaultApn = getSelectedApn();
            if (defaultApn == null) {
                defaultApn = "dummy-apn";
            }
            String extrainfo = null;
            LinkProperties lp = this.mConnMgr.getLinkProperties(info.getType());
            if (lp != null) {
                extrainfo = lp.getInterfaceName();
                Log.i("GpsLocationProvider", "updateNetworkState get iface:" + extrainfo);
            }
            native_update_network_state(info.isConnected(), info.getType(), info.isRoaming(), networkAvailable, extrainfo, defaultApn);
        }
        if (info != null && info.getType() == 3 && this.mAGpsDataConnectionState == 1) {
            if (this.mNetworkAvailable) {
                String apnName = info.getExtraInfo();
                if (apnName == null) {
                    apnName = "dummy-apn";
                }
                this.mAGpsApn = apnName;
                this.mApnIpType = getApnIpType(apnName);
                setRouting();
                if (DEBUG) {
                    String message = String.format("native_agps_data_conn_open: mAgpsApn=%s, mApnIpType=%s", this.mAGpsApn, Integer.valueOf(this.mApnIpType));
                    Log.d("GpsLocationProvider", message);
                }
                native_agps_data_conn_open(this.mAGpsApn, this.mApnIpType);
                this.mAGpsDataConnectionState = 2;
            } else {
                Log.e("GpsLocationProvider", "call native_agps_data_conn_failed, info: " + info);
                this.mAGpsApn = null;
                this.mApnIpType = 0;
                this.mAGpsDataConnectionState = 0;
                native_agps_data_conn_failed();
            }
        }
        if (this.mNetworkAvailable) {
            if (this.mInjectNtpTimePending == 0) {
                sendMessage(5, 0, null);
            }
            if (this.mDownloadXtraDataPending == 0) {
                sendMessage(6, 0, null);
            }
        }
    }

    private void handleInjectNtpTime() {
        if (this.mInjectNtpTimePending != 1) {
            if (!this.mNetworkAvailable) {
                this.mInjectNtpTimePending = 0;
                return;
            }
            this.mInjectNtpTimePending = 1;
            this.mWakeLock.acquire();
            AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    long delay;
                    if (GpsLocationProvider.this.mNtpTime.getCacheAge() >= GpsLocationProvider.NTP_INTERVAL) {
                        GpsLocationProvider.this.mNtpTime.forceRefresh();
                    }
                    if (GpsLocationProvider.this.mNtpTime.getCacheAge() < GpsLocationProvider.NTP_INTERVAL) {
                        long time = GpsLocationProvider.this.mNtpTime.getCachedNtpTime();
                        long timeReference = GpsLocationProvider.this.mNtpTime.getCachedNtpTimeReference();
                        long certainty = GpsLocationProvider.this.mNtpTime.getCacheCertainty();
                        long now = System.currentTimeMillis();
                        Log.d("GpsLocationProvider", "NTP server returned: " + time + " (" + new Date(time) + ") reference: " + timeReference + " certainty: " + certainty + " system time offset: " + (time - now));
                        GpsLocationProvider.this.native_inject_time(time, timeReference, (int) certainty);
                        delay = GpsLocationProvider.NTP_INTERVAL;
                    } else {
                        if (GpsLocationProvider.DEBUG) {
                            Log.d("GpsLocationProvider", "requestTime failed");
                        }
                        delay = GpsLocationProvider.RETRY_INTERVAL;
                    }
                    GpsLocationProvider.this.sendMessage(10, 0, null);
                    if (GpsLocationProvider.this.mPeriodicTimeInjection) {
                        GpsLocationProvider.this.mHandler.sendEmptyMessageDelayed(5, delay);
                    }
                    GpsLocationProvider.this.mWakeLock.release();
                }
            });
        }
    }

    private void handleDownloadXtraData() {
        if (this.mDownloadXtraDataPending != 1) {
            if (!this.mNetworkAvailable) {
                this.mDownloadXtraDataPending = 0;
                return;
            }
            this.mDownloadXtraDataPending = 1;
            this.mWakeLock.acquire();
            AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    GpsXtraDownloader xtraDownloader = new GpsXtraDownloader(GpsLocationProvider.this.mContext, GpsLocationProvider.this.mProperties);
                    byte[] data = xtraDownloader.downloadXtraData();
                    if (data != null) {
                        if (GpsLocationProvider.DEBUG) {
                            Log.d("GpsLocationProvider", "calling native_inject_xtra_data");
                        }
                        GpsLocationProvider.this.native_inject_xtra_data(data, data.length);
                    }
                    GpsLocationProvider.this.sendMessage(11, 0, null);
                    if (data == null) {
                        GpsLocationProvider.this.mHandler.sendEmptyMessageDelayed(6, GpsLocationProvider.RETRY_INTERVAL);
                    }
                    GpsLocationProvider.this.mWakeLock.release();
                }
            });
        }
    }

    private void handleUpdateLocation(Location location) {
        if (location.hasAccuracy()) {
            native_inject_location(location.getLatitude(), location.getLongitude(), location.getAccuracy());
        }
    }

    @Override
    public void enable() {
        synchronized (this.mLock) {
            if (!this.mEnabled) {
                this.mEnabled = true;
                sendMessage(2, 1, null);
            }
        }
    }

    private void setSuplHostPort(String hostString, String portString) {
        if (hostString != null) {
            this.mSuplServerHost = hostString;
        }
        if (portString != null) {
            try {
                this.mSuplServerPort = Integer.parseInt(portString);
            } catch (NumberFormatException e) {
                Log.e("GpsLocationProvider", "unable to parse SUPL_PORT: " + portString);
            }
        }
        if (this.mSuplServerHost != null && this.mSuplServerPort > 0 && this.mSuplServerPort <= 65535) {
            native_set_agps_server(1, this.mSuplServerHost, this.mSuplServerPort);
        }
    }

    private int getSuplMode(Properties properties, boolean agpsEnabled, boolean singleShot) {
        if (!agpsEnabled) {
            return 0;
        }
        String modeString = properties.getProperty("SUPL_MODE");
        int suplMode = 0;
        if (!TextUtils.isEmpty(modeString)) {
            try {
                suplMode = Integer.parseInt(modeString);
            } catch (NumberFormatException e) {
                Log.e("GpsLocationProvider", "unable to parse SUPL_MODE: " + modeString);
                return 0;
            }
        }
        if (singleShot && hasCapability(4) && (suplMode & 2) != 0) {
            return 2;
        }
        return (!hasCapability(2) || (suplMode & 1) == 0) ? 0 : 1;
    }

    private void handleEnable() {
        if (DEBUG) {
            Log.d("GpsLocationProvider", "handleEnable");
        }
        boolean enabled = native_init();
        if (enabled) {
            this.mSupportsXtra = native_supports_xtra();
            if (this.mSuplServerHost != null) {
                native_set_agps_server(1, this.mSuplServerHost, this.mSuplServerPort);
            }
            if (this.mC2KServerHost != null) {
                native_set_agps_server(2, this.mC2KServerHost, this.mC2KServerPort);
                return;
            }
            return;
        }
        synchronized (this.mLock) {
            this.mEnabled = false;
        }
        Log.w("GpsLocationProvider", "Failed to enable location provider");
    }

    @Override
    public void disable() {
        synchronized (this.mLock) {
            if (this.mEnabled) {
                this.mEnabled = false;
                sendMessage(2, 0, null);
            }
        }
    }

    private void handleDisable() {
        if (DEBUG) {
            Log.d("GpsLocationProvider", "handleDisable");
        }
        updateClientUids(new WorkSource());
        stopNavigating();
        this.mAlarmManager.cancel(this.mWakeupIntent);
        this.mAlarmManager.cancel(this.mTimeoutIntent);
        native_cleanup();
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
    public int getStatus(Bundle extras) {
        if (extras != null) {
            extras.putInt("satellites", this.mSvCount);
        }
        return this.mStatus;
    }

    private void updateStatus(int status, int svCount) {
        if (status != this.mStatus || svCount != this.mSvCount) {
            this.mStatus = status;
            this.mSvCount = svCount;
            this.mLocationExtras.putInt("satellites", svCount);
            this.mStatusUpdateTime = SystemClock.elapsedRealtime();
        }
    }

    @Override
    public long getStatusUpdateTime() {
        return this.mStatusUpdateTime;
    }

    @Override
    public void setRequest(ProviderRequest request, WorkSource source) {
        sendMessage(3, 0, new GpsRequest(request, source));
    }

    private void handleSetRequest(ProviderRequest request, WorkSource source) {
        this.mProviderRequest = request;
        this.mWorkSource = source;
        updateRequirements();
    }

    private void updateRequirements() {
        if (this.mProviderRequest != null && this.mWorkSource != null) {
            boolean singleShot = false;
            if (this.mProviderRequest.locationRequests != null && this.mProviderRequest.locationRequests.size() > 0) {
                singleShot = true;
                for (LocationRequest lr : this.mProviderRequest.locationRequests) {
                    if (lr.getNumUpdates() != 1) {
                        singleShot = false;
                    }
                }
            }
            if (DEBUG) {
                Log.d("GpsLocationProvider", "setRequest " + this.mProviderRequest);
            }
            if (this.mProviderRequest.reportLocation && !this.mDisableGps) {
                updateClientUids(this.mWorkSource);
                this.mFixInterval = (int) this.mProviderRequest.interval;
                if (this.mFixInterval != this.mProviderRequest.interval) {
                    Log.w("GpsLocationProvider", "interval overflow: " + this.mProviderRequest.interval);
                    this.mFixInterval = Integer.MAX_VALUE;
                }
                if (this.mStarted && hasCapability(1)) {
                    if (!native_set_position_mode(this.mPositionMode, 0, this.mFixInterval, 0, 0)) {
                        Log.e("GpsLocationProvider", "set_position_mode failed in setMinTime()");
                        return;
                    }
                    return;
                } else {
                    if (!this.mStarted) {
                        startNavigating(singleShot);
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
    }

    private void updateClientUids(WorkSource source) {
        WorkSource[] changes = this.mClientSource.setReturningDiffs(source);
        if (changes != null) {
            WorkSource newWork = changes[0];
            WorkSource goneWork = changes[1];
            if (newWork != null) {
                int lastuid = -1;
                for (int i = 0; i < newWork.size(); i++) {
                    try {
                        int uid = newWork.get(i);
                        this.mAppOpsService.startOperation(AppOpsManager.getToken(this.mAppOpsService), 2, uid, newWork.getName(i));
                        if (uid != lastuid) {
                            lastuid = uid;
                            this.mBatteryStats.noteStartGps(uid);
                        }
                    } catch (RemoteException e) {
                        Log.w("GpsLocationProvider", "RemoteException", e);
                    }
                }
            }
            if (goneWork != null) {
                int lastuid2 = -1;
                for (int i2 = 0; i2 < goneWork.size(); i2++) {
                    try {
                        int uid2 = goneWork.get(i2);
                        this.mAppOpsService.finishOperation(AppOpsManager.getToken(this.mAppOpsService), 2, uid2, goneWork.getName(i2));
                        if (uid2 != lastuid2) {
                            lastuid2 = uid2;
                            this.mBatteryStats.noteStopGps(uid2);
                        }
                    } catch (RemoteException e2) {
                        Log.w("GpsLocationProvider", "RemoteException", e2);
                    }
                }
            }
        }
    }

    @Override
    public boolean sendExtraCommand(String command, Bundle extras) {
        long identity = Binder.clearCallingIdentity();
        boolean result = false;
        if ("delete_aiding_data".equals(command)) {
            result = deleteAidingData(extras);
        } else if ("force_time_injection".equals(command)) {
            sendMessage(5, 0, null);
            result = true;
        } else if ("force_xtra_injection".equals(command)) {
            if (this.mSupportsXtra) {
                xtraDownloadRequest();
                result = true;
            }
        } else {
            Log.w("GpsLocationProvider", "sendExtraCommand: unknown command " + command);
        }
        Binder.restoreCallingIdentity(identity);
        return result;
    }

    private boolean deleteAidingData(Bundle extras) {
        int flags;
        if (extras == null) {
            flags = 65535;
        } else {
            flags = extras.getBoolean("ephemeris") ? 0 | 1 : 0;
            if (extras.getBoolean("almanac")) {
                flags |= 2;
            }
            if (extras.getBoolean("position")) {
                flags |= 4;
            }
            if (extras.getBoolean("time")) {
                flags |= 8;
            }
            if (extras.getBoolean("iono")) {
                flags |= 16;
            }
            if (extras.getBoolean("utc")) {
                flags |= 32;
            }
            if (extras.getBoolean("health")) {
                flags |= 64;
            }
            if (extras.getBoolean("svdir")) {
                flags |= 128;
            }
            if (extras.getBoolean("svsteer")) {
                flags |= 256;
            }
            if (extras.getBoolean("sadata")) {
                flags |= 512;
            }
            if (extras.getBoolean("rti")) {
                flags |= 1024;
            }
            if (extras.getBoolean("celldb-info")) {
                flags |= GPS_DELETE_CELLDB_INFO;
            }
            if (extras.getBoolean("all")) {
                flags |= 65535;
            }
        }
        if (flags != 0) {
            native_delete_aiding_data(flags);
            return true;
        }
        return false;
    }

    private void startNavigating(boolean singleShot) {
        String mode;
        if (!this.mStarted) {
            if (DEBUG) {
                Log.d("GpsLocationProvider", "startNavigating, singleShot is " + singleShot);
            }
            this.mTimeToFirstFix = 0;
            this.mLastFixTime = 0L;
            this.mStarted = true;
            this.mSingleShot = singleShot;
            this.mPositionMode = 0;
            boolean agpsEnabled = Settings.Global.getInt(this.mContext.getContentResolver(), "assisted_gps_enabled", 1) != 0;
            this.mPositionMode = getSuplMode(this.mProperties, agpsEnabled, singleShot);
            if (DEBUG) {
                switch (this.mPositionMode) {
                    case 0:
                        mode = "standalone";
                        break;
                    case 1:
                        mode = "MS_BASED";
                        break;
                    case 2:
                        mode = "MS_ASSISTED";
                        break;
                    default:
                        mode = "unknown";
                        break;
                }
                Log.d("GpsLocationProvider", "setting position_mode to " + mode);
            }
            int interval = hasCapability(1) ? this.mFixInterval : 1000;
            if (!native_set_position_mode(this.mPositionMode, 0, interval, 0, 0)) {
                this.mStarted = false;
                Log.e("GpsLocationProvider", "set_position_mode failed in startNavigating()");
            } else {
                if (!native_start()) {
                    this.mStarted = false;
                    Log.e("GpsLocationProvider", "native_start failed in startNavigating()");
                    return;
                }
                updateStatus(1, 0);
                this.mFixRequestTime = System.currentTimeMillis();
                if (!hasCapability(1) && this.mFixInterval >= NO_FIX_TIMEOUT) {
                    this.mAlarmManager.set(2, SystemClock.elapsedRealtime() + 60000, this.mTimeoutIntent);
                }
            }
        }
    }

    private void stopNavigating() {
        if (DEBUG) {
            Log.d("GpsLocationProvider", "stopNavigating");
        }
        if (this.mStarted) {
            this.mStarted = false;
            this.mSingleShot = false;
            native_stop();
            this.mTimeToFirstFix = 0;
            this.mLastFixTime = 0L;
            this.mLocationFlags = 0;
            updateStatus(1, 0);
        }
    }

    private void hibernate() {
        stopNavigating();
        this.mAlarmManager.cancel(this.mTimeoutIntent);
        this.mAlarmManager.cancel(this.mWakeupIntent);
        long now = SystemClock.elapsedRealtime();
        this.mAlarmManager.set(2, ((long) this.mFixInterval) + now, this.mWakeupIntent);
    }

    private boolean hasCapability(int capability) {
        return (this.mEngineCapabilities & capability) != 0;
    }

    private void reportLocation(int flags, double latitude, double longitude, double altitude, float speed, float bearing, float accuracy, long timestamp) {
        if (VERBOSE) {
            Log.v("GpsLocationProvider", "reportLocation lat: " + latitude + " long: " + longitude + " timestamp: " + timestamp);
        }
        synchronized (this.mLocation) {
            this.mLocationFlags = flags;
            if ((flags & 1) == 1) {
                this.mLocation.setLatitude(latitude);
                this.mLocation.setLongitude(longitude);
                this.mLocation.setTime(timestamp);
                this.mLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            }
            if ((flags & 2) == 2) {
                this.mLocation.setAltitude(altitude);
            } else {
                this.mLocation.removeAltitude();
            }
            if ((flags & 4) == 4) {
                this.mLocation.setSpeed(speed);
            } else {
                this.mLocation.removeSpeed();
            }
            if ((flags & 8) == 8) {
                this.mLocation.setBearing(bearing);
            } else {
                this.mLocation.removeBearing();
            }
            if ((flags & 16) == 16) {
                this.mLocation.setAccuracy(accuracy);
            } else {
                this.mLocation.removeAccuracy();
            }
            this.mLocation.setExtras(this.mLocationExtras);
            try {
                this.mILocationManager.reportLocation(this.mLocation, false);
            } catch (RemoteException e) {
                Log.e("GpsLocationProvider", "RemoteException calling reportLocation");
            }
        }
        this.mLastFixTime = System.currentTimeMillis();
        if (this.mTimeToFirstFix == 0 && (flags & 1) == 1) {
            this.mTimeToFirstFix = (int) (this.mLastFixTime - this.mFixRequestTime);
            if (DEBUG) {
                Log.d("GpsLocationProvider", "TTFF: " + this.mTimeToFirstFix);
            }
            this.mListenerHelper.onFirstFix(this.mTimeToFirstFix);
        }
        if (this.mSingleShot) {
            stopNavigating();
        }
        if (Settings.System.getInt(this.mContext.getContentResolver(), "PREF_KEY_GPS_TIME_SYNC_VALUE", 0) != 0) {
            Log.d("GpsLocationProvider", "Sync system time:" + this.mLastFixTime + "(ms) with gps time:" + timestamp + "(ms)");
            Log.d("GpsLocationProvider", "Delta time:" + Math.abs(this.mLastFixTime - timestamp) + "(ms)");
            if (Math.abs(this.mLastFixTime - timestamp) > 200) {
                SystemClock.setCurrentTimeMillis(timestamp);
            }
        }
        if (this.mStarted && this.mStatus != 2) {
            if (!hasCapability(1) && this.mFixInterval < NO_FIX_TIMEOUT) {
                this.mAlarmManager.cancel(this.mTimeoutIntent);
            }
            Intent intent = new Intent("android.location.GPS_FIX_CHANGE");
            intent.putExtra("enabled", true);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            updateStatus(2, this.mSvCount);
        }
        if (!hasCapability(1) && this.mStarted && this.mFixInterval > 10000) {
            if (DEBUG) {
                Log.d("GpsLocationProvider", "got fix, hibernating");
            }
            hibernate();
        }
    }

    private void reportStatus(int status) {
        if (DEBUG) {
            Log.v("GpsLocationProvider", "reportStatus status: " + status);
        }
        boolean wasNavigating = this.mNavigating;
        switch (status) {
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
        if (wasNavigating != this.mNavigating) {
            this.mListenerHelper.onGpsEnabledChanged(this.mNavigating);
            this.mGpsMeasurementsProvider.onGpsEnabledChanged(this.mNavigating);
            this.mGpsNavigationMessageProvider.onGpsEnabledChanged(this.mNavigating);
            Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
            intent.putExtra("enabled", this.mNavigating);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    private void reportSvStatus() {
        int svCount = native_read_sv_status(this.mSvs, this.mSnrs, this.mSvElevations, this.mSvAzimuths, this.mSvMasks);
        this.mListenerHelper.onSvStatusChanged(svCount, this.mSvs, this.mSnrs, this.mSvElevations, this.mSvAzimuths, this.mSvMasks[0], this.mSvMasks[1], this.mSvMasks[2]);
        if (VERBOSE) {
            Log.v("GpsLocationProvider", "SV count: " + svCount + " ephemerisMask: " + Integer.toHexString(this.mSvMasks[0]) + " almanacMask: " + Integer.toHexString(this.mSvMasks[1]));
            for (int i = 0; i < svCount; i++) {
                Log.v("GpsLocationProvider", "sv: " + this.mSvs[i] + " snr: " + (this.mSnrs[i] / 10.0f) + " elev: " + this.mSvElevations[i] + " azimuth: " + this.mSvAzimuths[i] + ((this.mSvMasks[0] & (1 << (this.mSvs[i] + (-1)))) == 0 ? "  " : " E") + ((this.mSvMasks[1] & (1 << (this.mSvs[i] + (-1)))) == 0 ? "  " : " A") + ((this.mSvMasks[2] & (1 << (this.mSvs[i] + (-1)))) == 0 ? "" : "U"));
            }
        }
        updateStatus(this.mStatus, Integer.bitCount(this.mSvMasks[2]));
        if (this.mNavigating && this.mStatus == 2 && this.mLastFixTime > 0 && System.currentTimeMillis() - this.mLastFixTime > RECENT_FIX_TIMEOUT) {
            Intent intent = new Intent("android.location.GPS_FIX_CHANGE");
            intent.putExtra("enabled", false);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            updateStatus(1, this.mSvCount);
        }
    }

    private void reportAGpsStatus(int type, int status, byte[] ipaddr) {
        switch (status) {
            case 1:
                if (DEBUG) {
                    Log.d("GpsLocationProvider", "GPS_REQUEST_AGPS_DATA_CONN");
                }
                Log.v("GpsLocationProvider", "Received SUPL IP addr[]: " + ipaddr);
                this.mAGpsDataConnectionState = 1;
                int result = this.mConnMgr.startUsingNetworkFeature(0, "enableSUPL");
                if (ipaddr != null) {
                    try {
                        this.mAGpsDataConnectionIpAddr = InetAddress.getByAddress(ipaddr);
                        Log.v("GpsLocationProvider", "IP address converted to: " + this.mAGpsDataConnectionIpAddr);
                    } catch (UnknownHostException e) {
                        Log.e("GpsLocationProvider", "Bad IP Address: " + ipaddr, e);
                        this.mAGpsDataConnectionIpAddr = null;
                    }
                }
                if (result == 0) {
                    if (DEBUG) {
                        Log.d("GpsLocationProvider", "PhoneConstants.APN_ALREADY_ACTIVE");
                    }
                    if (this.mAGpsApn != null) {
                        setRouting();
                        native_agps_data_conn_open(this.mAGpsApn, this.mApnIpType);
                        this.mAGpsDataConnectionState = 2;
                    } else {
                        Log.e("GpsLocationProvider", "mAGpsApn not set when receiving PhoneConstants.APN_ALREADY_ACTIVE");
                        this.mAGpsDataConnectionState = 0;
                        native_agps_data_conn_failed();
                    }
                } else if (result == 1) {
                    if (DEBUG) {
                        Log.d("GpsLocationProvider", "PhoneConstants.APN_REQUEST_STARTED");
                    }
                } else {
                    if (DEBUG) {
                        Log.d("GpsLocationProvider", "startUsingNetworkFeature failed, value is " + result);
                    }
                    this.mAGpsDataConnectionState = 0;
                    native_agps_data_conn_failed();
                }
                break;
            case 2:
                if (DEBUG) {
                    Log.d("GpsLocationProvider", "GPS_RELEASE_AGPS_DATA_CONN");
                }
                if (this.mAGpsDataConnectionState != 0) {
                    this.mConnMgr.stopUsingNetworkFeature(0, "enableSUPL");
                    native_agps_data_conn_closed();
                    this.mAGpsDataConnectionState = 0;
                    this.mAGpsDataConnectionIpAddr = null;
                }
                break;
            case 3:
                if (DEBUG) {
                    Log.d("GpsLocationProvider", "GPS_AGPS_DATA_CONNECTED");
                }
                break;
            case 4:
                if (DEBUG) {
                    Log.d("GpsLocationProvider", "GPS_AGPS_DATA_CONN_DONE");
                }
                break;
            case 5:
                if (DEBUG) {
                    Log.d("GpsLocationProvider", "GPS_AGPS_DATA_CONN_FAILED");
                }
                break;
            default:
                Log.d("GpsLocationProvider", "Received Unknown AGPS status: " + status);
                break;
        }
    }

    private void reportNmea(long timestamp) {
        int length = native_read_nmea(this.mNmeaBuffer, this.mNmeaBuffer.length);
        String nmea = new String(this.mNmeaBuffer, 0, length);
        this.mListenerHelper.onNmeaReceived(timestamp, nmea);
    }

    private void reportMeasurementData(GpsMeasurementsEvent event) {
        this.mGpsMeasurementsProvider.onMeasurementsAvailable(event);
    }

    private void reportNavigationMessage(GpsNavigationMessageEvent event) {
        this.mGpsNavigationMessageProvider.onNavigationMessageAvailable(event);
    }

    private void setEngineCapabilities(int capabilities) {
        this.mEngineCapabilities = capabilities;
        if (!hasCapability(16) && !this.mPeriodicTimeInjection) {
            this.mPeriodicTimeInjection = true;
            requestUtcTime();
        }
        this.mGpsMeasurementsProvider.onCapabilitiesUpdated((capabilities & 64) == 64);
        this.mGpsNavigationMessageProvider.onCapabilitiesUpdated((capabilities & 128) == 128);
    }

    private void xtraDownloadRequest() {
        if (DEBUG) {
            Log.d("GpsLocationProvider", "xtraDownloadRequest");
        }
        sendMessage(6, 0, null);
    }

    private Location buildLocation(int flags, double latitude, double longitude, double altitude, float speed, float bearing, float accuracy, long timestamp) {
        Location location = new Location("gps");
        if ((flags & 1) == 1) {
            location.setLatitude(latitude);
            location.setLongitude(longitude);
            location.setTime(timestamp);
            location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        }
        if ((flags & 2) == 2) {
            location.setAltitude(altitude);
        }
        if ((flags & 4) == 4) {
            location.setSpeed(speed);
        }
        if ((flags & 8) == 8) {
            location.setBearing(bearing);
        }
        if ((flags & 16) == 16) {
            location.setAccuracy(accuracy);
        }
        return location;
    }

    private int getGeofenceStatus(int status) {
        switch (status) {
            case GPS_GEOFENCE_ERROR_GENERIC:
                return 5;
            case GPS_GEOFENCE_ERROR_INVALID_TRANSITION:
                return 4;
            case GPS_GEOFENCE_ERROR_ID_UNKNOWN:
                return 3;
            case GPS_GEOFENCE_ERROR_ID_EXISTS:
                return 2;
            case 0:
                return 0;
            case 100:
                return 1;
            default:
                return -1;
        }
    }

    private void reportGeofenceTransition(int geofenceId, int flags, double latitude, double longitude, double altitude, float speed, float bearing, float accuracy, long timestamp, int transition, long transitionTimestamp) {
        if (this.mGeofenceHardwareImpl == null) {
            this.mGeofenceHardwareImpl = GeofenceHardwareImpl.getInstance(this.mContext);
        }
        Location location = buildLocation(flags, latitude, longitude, altitude, speed, bearing, accuracy, timestamp);
        this.mGeofenceHardwareImpl.reportGeofenceTransition(geofenceId, location, transition, transitionTimestamp, 0, FusedBatchOptions.SourceTechnologies.GNSS);
    }

    private void reportGeofenceStatus(int status, int flags, double latitude, double longitude, double altitude, float speed, float bearing, float accuracy, long timestamp) {
        if (this.mGeofenceHardwareImpl == null) {
            this.mGeofenceHardwareImpl = GeofenceHardwareImpl.getInstance(this.mContext);
        }
        Location location = buildLocation(flags, latitude, longitude, altitude, speed, bearing, accuracy, timestamp);
        int monitorStatus = 1;
        if (status == 2) {
            monitorStatus = 0;
        }
        this.mGeofenceHardwareImpl.reportGeofenceMonitorStatus(0, monitorStatus, location, FusedBatchOptions.SourceTechnologies.GNSS);
    }

    private void reportGeofenceAddStatus(int geofenceId, int status) {
        if (this.mGeofenceHardwareImpl == null) {
            this.mGeofenceHardwareImpl = GeofenceHardwareImpl.getInstance(this.mContext);
        }
        this.mGeofenceHardwareImpl.reportGeofenceAddStatus(geofenceId, getGeofenceStatus(status));
    }

    private void reportGeofenceRemoveStatus(int geofenceId, int status) {
        if (this.mGeofenceHardwareImpl == null) {
            this.mGeofenceHardwareImpl = GeofenceHardwareImpl.getInstance(this.mContext);
        }
        this.mGeofenceHardwareImpl.reportGeofenceRemoveStatus(geofenceId, getGeofenceStatus(status));
    }

    private void reportGeofencePauseStatus(int geofenceId, int status) {
        if (this.mGeofenceHardwareImpl == null) {
            this.mGeofenceHardwareImpl = GeofenceHardwareImpl.getInstance(this.mContext);
        }
        this.mGeofenceHardwareImpl.reportGeofencePauseStatus(geofenceId, getGeofenceStatus(status));
    }

    private void reportGeofenceResumeStatus(int geofenceId, int status) {
        if (this.mGeofenceHardwareImpl == null) {
            this.mGeofenceHardwareImpl = GeofenceHardwareImpl.getInstance(this.mContext);
        }
        this.mGeofenceHardwareImpl.reportGeofenceResumeStatus(geofenceId, getGeofenceStatus(status));
    }

    public INetInitiatedListener getNetInitiatedListener() {
        return this.mNetInitiatedListener;
    }

    public void reportNiNotification(int notificationId, int niType, int notifyFlags, int timeout, int defaultResponse, String requestorId, String text, int requestorIdEncoding, int textEncoding, String extras) {
        Log.i("GpsLocationProvider", "reportNiNotification: entered");
        Log.i("GpsLocationProvider", "notificationId: " + notificationId + ", niType: " + niType + ", notifyFlags: " + notifyFlags + ", timeout: " + timeout + ", defaultResponse: " + defaultResponse);
        Log.i("GpsLocationProvider", "requestorId: " + requestorId + ", text: " + text + ", requestorIdEncoding: " + requestorIdEncoding + ", textEncoding: " + textEncoding);
        GpsNetInitiatedHandler.GpsNiNotification notification = new GpsNetInitiatedHandler.GpsNiNotification();
        notification.notificationId = notificationId;
        notification.niType = niType;
        notification.needNotify = (notifyFlags & 1) != 0;
        notification.needVerify = (notifyFlags & 2) != 0;
        notification.privacyOverride = (notifyFlags & 4) != 0;
        notification.timeout = timeout;
        notification.defaultResponse = defaultResponse;
        notification.requestorId = requestorId;
        notification.text = text;
        notification.requestorIdEncoding = requestorIdEncoding;
        notification.textEncoding = textEncoding;
        Bundle bundle = new Bundle();
        if (extras == null) {
            extras = "";
        }
        Properties extraProp = new Properties();
        try {
            extraProp.load(new StringReader(extras));
        } catch (IOException e) {
            Log.e("GpsLocationProvider", "reportNiNotification cannot parse extras data: " + extras);
        }
        for (Map.Entry<Object, Object> ent : extraProp.entrySet()) {
            bundle.putString((String) ent.getKey(), (String) ent.getValue());
        }
        notification.extras = bundle;
        this.mNIHandler.handleNiNotification(notification);
    }

    private void requestSetID(int flags) {
        String data_temp;
        TelephonyManager phone = (TelephonyManager) this.mContext.getSystemService("phone");
        int type = 0;
        String data = "";
        if ((flags & 1) == 1) {
            String data_temp2 = phone.getSubscriberId();
            if (data_temp2 != null) {
                data = data_temp2;
                type = 1;
            }
        } else if ((flags & 2) == 2 && (data_temp = phone.getLine1Number()) != null) {
            data = data_temp;
            type = 2;
        }
        native_agps_set_id(type, data);
    }

    private void requestUtcTime() {
        sendMessage(5, 0, null);
    }

    private void requestRefLocation(int flags) {
        int type;
        TelephonyManager phone = (TelephonyManager) this.mContext.getSystemService("phone");
        int phoneType = phone.getPhoneType();
        if (phoneType == 1 || phoneType == 0) {
            GsmCellLocation gsm_cell = (GsmCellLocation) phone.getCellLocation();
            if (gsm_cell != null && phone.getNetworkOperator() != null && phone.getNetworkOperator().length() > 3) {
                int mcc = Integer.parseInt(phone.getNetworkOperator().substring(0, 3));
                int mnc = Integer.parseInt(phone.getNetworkOperator().substring(3));
                int networkType = phone.getNetworkType();
                Log.d("GpsLocationProvider", "NetworkType:" + networkType);
                if (networkType == 3 || networkType == 8 || networkType == 9 || networkType == 10 || networkType == 15) {
                    type = 2;
                } else if (networkType == 13) {
                    type = 4;
                    native_agps_set_ref_location_cellid(4, -1, -1, gsm_cell.getPsc(), -1);
                } else {
                    type = 1;
                }
                native_agps_set_ref_location_cellid(type, mcc, mnc, gsm_cell.getLac(), gsm_cell.getCid());
                return;
            }
            Log.e("GpsLocationProvider", "Error getting cell location info.");
            return;
        }
        if (phoneType == 2) {
            Log.e("GpsLocationProvider", "CDMA not supported.");
        }
    }

    private void sendMessage(int message, int arg, Object obj) {
        this.mWakeLock.acquire();
        this.mHandler.obtainMessage(message, arg, 1, obj).sendToTarget();
    }

    private final class ProviderHandler extends Handler {
        public ProviderHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message msg) {
            int message = msg.what;
            switch (message) {
                case 2:
                    if (msg.arg1 == 1) {
                        GpsLocationProvider.this.handleEnable();
                    } else {
                        GpsLocationProvider.this.handleDisable();
                    }
                    break;
                case 3:
                    GpsRequest gpsRequest = (GpsRequest) msg.obj;
                    GpsLocationProvider.this.handleSetRequest(gpsRequest.request, gpsRequest.source);
                    break;
                case 4:
                    GpsLocationProvider.this.handleUpdateNetworkState(msg.arg1, (NetworkInfo) msg.obj);
                    break;
                case 5:
                    GpsLocationProvider.this.handleInjectNtpTime();
                    break;
                case 6:
                    if (GpsLocationProvider.this.mSupportsXtra) {
                        GpsLocationProvider.this.handleDownloadXtraData();
                    }
                    break;
                case 7:
                    GpsLocationProvider.this.handleUpdateLocation((Location) msg.obj);
                    break;
                case 10:
                    GpsLocationProvider.this.mInjectNtpTimePending = 2;
                    break;
                case 11:
                    GpsLocationProvider.this.mDownloadXtraDataPending = 2;
                    break;
            }
            if (msg.arg2 == 1) {
                GpsLocationProvider.this.mWakeLock.release();
            }
        }
    }

    private final class NetworkLocationListener implements LocationListener {
        private NetworkLocationListener() {
        }

        @Override
        public void onLocationChanged(Location location) {
            if ("network".equals(location.getProvider())) {
                GpsLocationProvider.this.handleUpdateLocation(location);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    }

    private String getSelectedApn() {
        Uri uri = Uri.parse("content://telephony/carriers/preferapn");
        Cursor cursor = null;
        try {
            try {
                cursor = this.mContext.getContentResolver().query(uri, new String[]{"apn"}, null, null, "name ASC");
            } catch (Exception e) {
                Log.e("GpsLocationProvider", "Error encountered on selecting the APN.", e);
                if (cursor != null) {
                    cursor.close();
                }
            }
            if (cursor != null && cursor.moveToFirst()) {
                String string = cursor.getString(0);
            }
            Log.e("GpsLocationProvider", "No APN found to select.");
            if (cursor != null) {
                cursor.close();
            }
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private int getApnIpType(String apn) {
        if (apn == null) {
            return 0;
        }
        if (apn.equals(this.mAGpsApn) && this.mApnIpType != 0) {
            return this.mApnIpType;
        }
        String selection = String.format("current = 1 and apn = '%s' and carrier_enabled = 1", apn);
        Cursor cursor = null;
        try {
            try {
                cursor = this.mContext.getContentResolver().query(Telephony.Carriers.CONTENT_URI, new String[]{"protocol"}, selection, null, "name ASC");
            } catch (Exception e) {
                Log.e("GpsLocationProvider", "Error encountered on APN query for: " + apn, e);
                if (cursor != null) {
                    cursor.close();
                }
            }
            if (cursor != null && cursor.moveToFirst()) {
                int iTranslateToApnIpType = translateToApnIpType(cursor.getString(0), apn);
            }
            Log.e("GpsLocationProvider", "No entry found in query for APN: " + apn);
            if (cursor != null) {
                cursor.close();
            }
            return 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private int translateToApnIpType(String ipProtocol, String apn) {
        if ("IP".equals(ipProtocol)) {
            return 1;
        }
        if ("IPV6".equals(ipProtocol)) {
            return 2;
        }
        if ("IPV4V6".equals(ipProtocol)) {
            return 3;
        }
        String message = String.format("Unknown IP Protocol: %s, for APN: %s", ipProtocol, apn);
        Log.e("GpsLocationProvider", message);
        return 0;
    }

    private void setRouting() {
        if (this.mAGpsDataConnectionIpAddr != null) {
            boolean result = this.mConnMgr.requestRouteToHostAddress(3, this.mAGpsDataConnectionIpAddr);
            if (!result) {
                Log.e("GpsLocationProvider", "Error requesting route to host: " + this.mAGpsDataConnectionIpAddr);
            } else if (DEBUG) {
                Log.d("GpsLocationProvider", "Successfully requested route to host: " + this.mAGpsDataConnectionIpAddr);
            }
        }
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        StringBuilder s = new StringBuilder();
        s.append("  mFixInterval=").append(this.mFixInterval).append("\n");
        s.append("  mDisableGps (battery saver mode)=").append(this.mDisableGps).append("\n");
        s.append("  mEngineCapabilities=0x").append(Integer.toHexString(this.mEngineCapabilities)).append(" (");
        if (hasCapability(1)) {
            s.append("SCHED ");
        }
        if (hasCapability(2)) {
            s.append("MSB ");
        }
        if (hasCapability(4)) {
            s.append("MSA ");
        }
        if (hasCapability(8)) {
            s.append("SINGLE_SHOT ");
        }
        if (hasCapability(16)) {
            s.append("ON_DEMAND_TIME ");
        }
        s.append(")\n");
        s.append(native_get_internal_state());
        pw.append((CharSequence) s);
    }
}
