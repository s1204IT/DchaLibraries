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
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
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
import com.mediatek.location.LocationExt;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import libcore.io.IoUtils;

public class GnssLocationProvider implements LocationProviderInterface {
    private static final int ADD_LISTENER = 8;
    private static final int AGPS_DATA_CONNECTION_CLOSED = 0;
    private static final int AGPS_DATA_CONNECTION_OPEN = 2;
    private static final int AGPS_DATA_CONNECTION_OPENING = 1;
    private static final int AGPS_REF_LOCATION_TYPE_GSM_CELLID = 1;
    private static final int AGPS_REF_LOCATION_TYPE_UMTS_CELLID = 2;
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
    private static final int APN_INVALID = 0;
    private static final int APN_IPV4 = 1;
    private static final int APN_IPV4V6 = 3;
    private static final int APN_IPV6 = 2;
    private static final String BATTERY_SAVER_GPS_MODE = "batterySaverGpsMode";
    private static final int BATTERY_SAVER_MODE_DISABLED_WHEN_SCREEN_OFF = 1;
    private static final int BATTERY_SAVER_MODE_NO_CHANGE = 0;
    private static final int CHECK_LOCATION = 1;
    private static final boolean DEBUG;
    private static final String DEFAULT_PROPERTIES_FILE = "/etc/gps.conf";
    private static final int DOWNLOAD_XTRA_DATA = 6;
    private static final int DOWNLOAD_XTRA_DATA_FINISHED = 11;
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
    private static final int INITIALIZE_HANDLER = 13;
    private static final int INJECT_NTP_TIME = 5;
    private static final int INJECT_NTP_TIME_FINISHED = 10;
    private static final boolean IS_USER_BUILD;
    private static final int LOCATION_HAS_ACCURACY = 16;
    private static final int LOCATION_HAS_ALTITUDE = 2;
    private static final int LOCATION_HAS_BEARING = 8;
    private static final int LOCATION_HAS_LAT_LONG = 1;
    private static final int LOCATION_HAS_SPEED = 4;
    private static final int LOCATION_INVALID = 0;
    private static final long MAX_RETRY_INTERVAL = 14400000;
    private static final int MAX_SVS = 64;
    private static final int NO_FIX_TIMEOUT = 60000;
    private static final long NTP_INTERVAL = 86400000;
    private static final ProviderProperties PROPERTIES;
    private static final String PROPERTIES_FILE_PREFIX = "/etc/gps";
    private static final String PROPERTIES_FILE_SUFFIX = ".conf";
    private static final String PROP_FORCE_DEBUG_KEY = "persist.log.tag.tel_dbg";
    private static final long RECENT_FIX_TIMEOUT = 10000;
    private static final int RELEASE_SUPL_CONNECTION = 15;
    private static final int REMOVE_LISTENER = 9;
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
    private final GnssMeasurementsProvider mGnssMeasurementsProvider;
    private final GnssNavigationMessageProvider mGnssNavigationMessageProvider;
    private Handler mHandler;
    private final ILocationManager mILocationManager;
    private long mLastFixTime;
    private final GnssStatusListenerHelper mListenerHelper;
    private final GpsNetInitiatedHandler mNIHandler;
    private boolean mNavigating;
    private final NtpTrustedTime mNtpTime;
    private boolean mOnDemandTimeInjection;
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
    private Object mLock = new Object();
    private int mLocationFlags = 0;
    private int mStatus = 1;
    private long mStatusUpdateTime = SystemClock.elapsedRealtime();
    private BackOff mNtpBackOff = new BackOff(300000, MAX_RETRY_INTERVAL);
    private BackOff mXtraBackOff = new BackOff(300000, MAX_RETRY_INTERVAL);
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
    private String mDefaultApn = null;
    private WorkSource mClientSource = new WorkSource();
    private int mYearOfHardware = 0;
    private final IGnssStatusProvider mGnssStatusProvider = new IGnssStatusProvider.Stub() {
        public void registerGnssStatusCallback(IGnssStatusListener callback) {
            GnssLocationProvider.this.mListenerHelper.addListener(callback);
        }

        public void unregisterGnssStatusCallback(IGnssStatusListener callback) {
            GnssLocationProvider.this.mListenerHelper.removeListener(callback);
        }
    };
    private final ConnectivityManager.NetworkCallback mNetworkConnectivityCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            GnssLocationProvider.this.requestUtcTime();
            GnssLocationProvider.this.xtraDownloadRequest();
            LocationExt.updateNetworkAvailable(network);
        }
    };
    private final ConnectivityManager.NetworkCallback mSuplConnectivityCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            LocationExt.suplConnectionCallback(2, network);
            GnssLocationProvider.this.sendMessage(4, 0, network);
        }

        @Override
        public void onLost(Network network) {
            LocationExt.suplConnectionCallback(1, network);
            GnssLocationProvider.this.releaseSuplConnection(2);
        }

        @Override
        public void onUnavailable() {
            LocationExt.suplConnectionCallback(0, null);
            GnssLocationProvider.this.releaseSuplConnection(5);
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
            if (action.equals("android.intent.action.DATA_SMS_RECEIVED")) {
                GnssLocationProvider.this.checkSmsSuplInit(intent);
                return;
            }
            if (action.equals("android.provider.Telephony.WAP_PUSH_RECEIVED")) {
                GnssLocationProvider.this.checkWapSuplInit(intent);
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
    private IGpsGeofenceHardware mGpsGeofenceBinder = new IGpsGeofenceHardware.Stub() {
        public boolean isHardwareGeofenceSupported() {
            return GnssLocationProvider.native_is_geofence_supported();
        }

        public boolean addCircularHardwareGeofence(int geofenceId, double latitude, double longitude, double radius, int lastTransition, int monitorTransitions, int notificationResponsiveness, int unknownTimer) {
            return GnssLocationProvider.native_add_geofence(geofenceId, latitude, longitude, radius, lastTransition, monitorTransitions, notificationResponsiveness, unknownTimer);
        }

        public boolean removeHardwareGeofence(int geofenceId) {
            return GnssLocationProvider.native_remove_geofence(geofenceId);
        }

        public boolean pauseHardwareGeofence(int geofenceId) {
            return GnssLocationProvider.native_pause_geofence(geofenceId);
        }

        public boolean resumeHardwareGeofence(int geofenceId, int monitorTransition) {
            return GnssLocationProvider.native_resume_geofence(geofenceId, monitorTransition);
        }
    };
    private final INetInitiatedListener mNetInitiatedListener = new INetInitiatedListener.Stub() {
        public boolean sendNiResponse(int notificationId, int userResponse) {
            if (GnssLocationProvider.DEBUG) {
                Log.d("GnssLocationProvider", "sendNiResponse, notifId: " + notificationId + ", response: " + userResponse);
            }
            GnssLocationProvider.this.native_send_ni_response(notificationId, userResponse);
            return true;
        }
    };
    private int[] mSvidWithFlags = new int[64];
    private float[] mCn0s = new float[64];
    private float[] mSvElevations = new float[64];
    private float[] mSvAzimuths = new float[64];
    private byte[] mNmeaBuffer = new byte[120];

    public interface GnssSystemInfoProvider {
        int getGnssYearOfHardware();
    }

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

    private static native boolean native_is_agps_ril_supported();

    private static native boolean native_is_geofence_supported();

    private static native boolean native_is_gnss_configuration_supported();

    private static native boolean native_is_measurement_supported();

    private static native boolean native_is_navigation_message_supported();

    private static native boolean native_is_supported();

    private static native boolean native_pause_geofence(int i);

    private native int native_read_nmea(byte[] bArr, int i);

    private native int native_read_sv_status(int[] iArr, float[] fArr, float[] fArr2, float[] fArr3);

    private static native boolean native_remove_geofence(int i);

    private static native boolean native_resume_geofence(int i, int i2);

    private native void native_send_ni_response(int i, int i2);

    private native void native_set_agps_server(int i, String str, int i2);

    private native boolean native_set_position_mode(int i, int i2, int i3, int i4, int i5);

    private static native void native_set_vzw_debug_screen(boolean z);

    private native boolean native_start();

    private native boolean native_start_measurement_collection();

    private native boolean native_start_navigation_message_collection();

    private native boolean native_stop();

    private native boolean native_stop_measurement_collection();

    private native boolean native_stop_navigation_message_collection();

    private native boolean native_supports_xtra();

    private native void native_update_network_state(boolean z, int i, boolean z2, boolean z3, String str, String str2);

    static {
        IS_USER_BUILD = !"user".equals(Build.TYPE) ? "userdebug".equals(Build.TYPE) : true;
        FORCE_DEBUG = SystemProperties.getInt(PROP_FORCE_DEBUG_KEY, 0) == 1;
        DEBUG = (!IS_USER_BUILD || Log.isLoggable("GnssLocationProvider", 3)) ? true : FORCE_DEBUG;
        VERBOSE = (!IS_USER_BUILD || Log.isLoggable("GnssLocationProvider", 2)) ? true : FORCE_DEBUG;
        PROPERTIES = new ProviderProperties(true, true, false, false, true, true, true, 3, 1);
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

    public IGnssStatusProvider getGnssStatusProvider() {
        return this.mGnssStatusProvider;
    }

    public IGpsGeofenceHardware getGpsGeofenceProxy() {
        return this.mGpsGeofenceBinder;
    }

    public GnssMeasurementsProvider getGnssMeasurementsProvider() {
        return this.mGnssMeasurementsProvider;
    }

    public GnssNavigationMessageProvider getGnssNavigationMessageProvider() {
        return this.mGnssNavigationMessageProvider;
    }

    private void subscriptionOrSimChanged(Context context) {
        if (DEBUG) {
            Log.d("GnssLocationProvider", "received SIM related action: ");
        }
        TelephonyManager phone = (TelephonyManager) this.mContext.getSystemService("phone");
        String mccMnc = phone.getSimOperator();
        if (!TextUtils.isEmpty(mccMnc)) {
            if (DEBUG) {
                Log.d("GnssLocationProvider", "SIM MCC/MNC is available: " + mccMnc);
            }
            synchronized (this.mLock) {
                reloadGpsProperties(context, this.mProperties);
                this.mNIHandler.setSuplEsEnabled(this.mSuplEsEnabled);
            }
            return;
        }
        if (DEBUG) {
            Log.d("GnssLocationProvider", "SIM MCC/MNC is still not available");
        }
    }

    private void checkSmsSuplInit(Intent intent) {
        byte[] suplInit;
        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (messages == null) {
            Log.e("GnssLocationProvider", "Message does not exist in the intent.");
            return;
        }
        for (SmsMessage message : messages) {
            if (message != null && message.mWrappedSmsMessage != null && (suplInit = message.getUserData()) != null) {
                native_agps_ni_message(suplInit, suplInit.length);
            }
        }
    }

    private void checkWapSuplInit(Intent intent) {
        byte[] suplInit;
        if (!LocationExt.checkWapSuplInit(intent) || (suplInit = intent.getByteArrayExtra("data")) == null) {
            return;
        }
        native_agps_ni_message(suplInit, suplInit.length);
    }

    private void updateLowPowerMode() {
        boolean z = false;
        boolean disableGps = this.mPowerManager.isDeviceIdleMode();
        switch (Settings.Secure.getInt(this.mContext.getContentResolver(), BATTERY_SAVER_GPS_MODE, 1)) {
            case 1:
                if (this.mPowerManager.isPowerSaveMode() && !this.mPowerManager.isInteractive()) {
                    z = true;
                }
                disableGps |= z;
                break;
        }
        if (disableGps == this.mDisableGps) {
            return;
        }
        this.mDisableGps = disableGps;
        updateRequirements();
    }

    public static boolean isSupported() {
        return native_is_supported();
    }

    private void reloadGpsProperties(Context context, Properties properties) throws Throwable {
        if (DEBUG) {
            Log.d("GnssLocationProvider", "Reset GPS properties, previous size = " + properties.size());
        }
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
        if (DEBUG) {
            Log.d("GnssLocationProvider", "GPS properties reloaded, size = " + properties.size());
        }
        setSuplHostPort(properties.getProperty("SUPL_HOST"), properties.getProperty("SUPL_PORT"));
        this.mC2KServerHost = properties.getProperty("C2K_HOST");
        String portString = properties.getProperty("C2K_PORT");
        if (this.mC2KServerHost != null && portString != null) {
            try {
                this.mC2KServerPort = Integer.parseInt(portString);
            } catch (NumberFormatException e) {
                Log.e("GnssLocationProvider", "unable to parse C2K_PORT: " + portString);
            }
        }
        if (native_is_gnss_configuration_supported()) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
                properties.store(baos, (String) null);
                native_configuration_update(baos.toString());
                if (DEBUG) {
                    Log.d("GnssLocationProvider", "final config = " + baos.toString());
                }
            } catch (IOException e2) {
                Log.e("GnssLocationProvider", "failed to dump properties contents");
            }
        } else if (DEBUG) {
            Log.d("GnssLocationProvider", "Skipped configuration update because GNSS configuration in GPS HAL is not supported");
        }
        String suplESProperty = this.mProperties.getProperty("SUPL_ES");
        if (suplESProperty != null) {
            try {
                this.mSuplEsEnabled = Integer.parseInt(suplESProperty) == 1;
            } catch (NumberFormatException e3) {
                Log.e("GnssLocationProvider", "unable to parse SUPL_ES: " + suplESProperty);
            }
        }
        String emergencyExtensionSecondsString = properties.getProperty("ES_EXTENSION_SEC", "0");
        try {
            int emergencyExtensionSeconds = Integer.parseInt(emergencyExtensionSecondsString);
            this.mNIHandler.setEmergencyExtensionSeconds(emergencyExtensionSeconds);
        } catch (NumberFormatException e4) {
            Log.e("GnssLocationProvider", "unable to parse ES_EXTENSION_SEC: " + emergencyExtensionSecondsString);
        }
    }

    private void loadPropertiesFromResource(Context context, Properties properties) {
        String[] configValues = context.getResources().getStringArray(R.array.config_defaultNotificationVibeWaveform);
        for (String item : configValues) {
            if (DEBUG) {
                Log.d("GnssLocationProvider", "GpsParamsResource: " + item);
            }
            String[] split = item.split("=");
            if (split.length == 2) {
                properties.setProperty(split[0].trim().toUpperCase(), split[1]);
            } else {
                Log.w("GnssLocationProvider", "malformed contents: " + item);
            }
        }
    }

    private boolean loadPropertiesFromFile(String filename, Properties properties) throws Throwable {
        FileInputStream stream;
        try {
            File file = new File(filename);
            FileInputStream stream2 = null;
            try {
                stream = new FileInputStream(file);
            } catch (Throwable th) {
                th = th;
            }
            try {
                properties.load(stream);
                IoUtils.closeQuietly(stream);
                return true;
            } catch (Throwable th2) {
                th = th2;
                stream2 = stream;
                IoUtils.closeQuietly(stream2);
                throw th;
            }
        } catch (IOException e) {
            Log.w("GnssLocationProvider", "Could not open GPS configuration file " + filename);
            return false;
        }
    }

    public GnssLocationProvider(Context context, ILocationManager ilocationManager, Looper looper) {
        this.mContext = context;
        this.mNtpTime = NtpTrustedTime.getInstance(context);
        this.mILocationManager = ilocationManager;
        this.mLocation.setExtras(this.mLocationExtras);
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mWakeLock = this.mPowerManager.newWakeLock(1, "GnssLocationProvider");
        this.mWakeLock.setReferenceCounted(true);
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mWakeupIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(ALARM_WAKEUP), 0);
        this.mTimeoutIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(ALARM_TIMEOUT), 0);
        this.mConnMgr = (ConnectivityManager) context.getSystemService("connectivity");
        this.mAppOpsService = IAppOpsService.Stub.asInterface(ServiceManager.getService("appops"));
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
        this.mGnssMeasurementsProvider = new GnssMeasurementsProvider(this.mHandler) {
            @Override
            public boolean isAvailableInPlatform() {
                return GnssLocationProvider.native_is_measurement_supported();
            }

            @Override
            protected boolean registerWithService() {
                return GnssLocationProvider.this.native_start_measurement_collection();
            }

            @Override
            protected void unregisterFromService() {
                GnssLocationProvider.this.native_stop_measurement_collection();
            }

            @Override
            protected boolean isGpsEnabled() {
                return GnssLocationProvider.this.isEnabled();
            }
        };
        this.mGnssNavigationMessageProvider = new GnssNavigationMessageProvider(this.mHandler) {
            @Override
            protected boolean isAvailableInPlatform() {
                return GnssLocationProvider.native_is_navigation_message_supported();
            }

            @Override
            protected boolean registerWithService() {
                return GnssLocationProvider.this.native_start_navigation_message_collection();
            }

            @Override
            protected void unregisterFromService() {
                GnssLocationProvider.this.native_stop_navigation_message_collection();
            }

            @Override
            protected boolean isGpsEnabled() {
                return GnssLocationProvider.this.isEnabled();
            }
        };
        initLocationExt();
    }

    @Override
    public String getName() {
        return "gps";
    }

    @Override
    public ProviderProperties getProperties() {
        return PROPERTIES;
    }

    private void handleUpdateNetworkState(Network network) {
        NetworkInfo info = this.mConnMgr.getNetworkInfo(network);
        if (info == null) {
            return;
        }
        boolean isConnected = info.isConnected();
        if (DEBUG) {
            String message = String.format("UpdateNetworkState, state=%s, connected=%s, info=%s, capabilities=%S", agpsDataConnStateAsString(), Boolean.valueOf(isConnected), info, this.mConnMgr.getNetworkCapabilities(network));
            Log.d("GnssLocationProvider", message);
        }
        if (native_is_agps_ril_supported()) {
            boolean dataEnabled = TelephonyManager.getDefault().getDataEnabled();
            boolean z = info.isAvailable() ? dataEnabled : false;
            if (info.getType() != 1) {
                this.mDefaultApn = getSelectedApn();
            }
            if (this.mDefaultApn == null) {
                this.mDefaultApn = "dummy-apn";
            }
            native_update_network_state(isConnected, info.getType(), info.isRoaming(), z, info.getExtraInfo(), this.mDefaultApn);
        } else if (DEBUG) {
            Log.d("GnssLocationProvider", "Skipped network state update because GPS HAL AGPS-RIL is not  supported");
        }
        if (this.mAGpsDataConnectionState != 1) {
            return;
        }
        if (isConnected) {
            String apnName = info.getExtraInfo();
            if (apnName == null) {
                apnName = "dummy-apn";
            }
            int apnIpType = getApnIpType(apnName);
            setRouting();
            if (DEBUG) {
                String message2 = String.format("native_agps_data_conn_open: mAgpsApn=%s, mApnIpType=%s", apnName, Integer.valueOf(apnIpType));
                Log.d("GnssLocationProvider", message2);
            }
            native_agps_data_conn_open(apnName, apnIpType);
            this.mAGpsDataConnectionState = 2;
            return;
        }
        handleReleaseSuplConnection(5);
    }

    private void handleRequestSuplConnection(InetAddress address) {
        if (DEBUG) {
            String message = String.format("requestSuplConnection, state=%s, address=%s", agpsDataConnStateAsString(), address);
            Log.d("GnssLocationProvider", message);
        }
        if (this.mAGpsDataConnectionState != 0) {
            return;
        }
        this.mAGpsDataConnectionIpAddr = address;
        this.mAGpsDataConnectionState = 1;
        NetworkRequest.Builder requestBuilder = new NetworkRequest.Builder();
        requestBuilder.addTransportType(0);
        requestBuilder.addCapability(1);
        NetworkRequest request = requestBuilder.build();
        this.mConnMgr.requestNetwork(request, this.mSuplConnectivityCallback, 6000000);
    }

    private void handleReleaseSuplConnection(int agpsDataConnStatus) {
        if (DEBUG) {
            String message = String.format("releaseSuplConnection, state=%s, status=%s", agpsDataConnStateAsString(), agpsDataConnStatusAsString(agpsDataConnStatus));
            Log.d("GnssLocationProvider", message);
        }
        if (this.mAGpsDataConnectionState == 0) {
        }
        this.mAGpsDataConnectionState = 0;
        this.mConnMgr.unregisterNetworkCallback(this.mSuplConnectivityCallback);
        switch (agpsDataConnStatus) {
            case 2:
                native_agps_data_conn_closed();
                break;
            case 3:
            case 4:
            default:
                Log.e("GnssLocationProvider", "Invalid status to release SUPL connection: " + agpsDataConnStatus);
                break;
            case 5:
                native_agps_data_conn_failed();
                break;
        }
    }

    private void handleInjectNtpTime() {
        if (this.mInjectNtpTimePending == 1) {
            return;
        }
        if (!isDataNetworkConnected()) {
            this.mInjectNtpTimePending = 0;
            return;
        }
        this.mInjectNtpTimePending = 1;
        this.mWakeLock.acquire();
        Log.i("GnssLocationProvider", "WakeLock acquired by handleInjectNtpTime()");
        AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                long delay;
                boolean refreshSuccess = GnssLocationProvider.this.mNtpTime.getCacheAge() >= 86400000 ? GnssLocationProvider.this.mNtpTime.forceRefresh() : true;
                if (GnssLocationProvider.this.mNtpTime.getCacheAge() < 86400000) {
                    long time = GnssLocationProvider.this.mNtpTime.getCachedNtpTime();
                    long timeReference = GnssLocationProvider.this.mNtpTime.getCachedNtpTimeReference();
                    long certainty = GnssLocationProvider.this.mNtpTime.getCacheCertainty();
                    long now = System.currentTimeMillis();
                    if (GnssLocationProvider.DEBUG) {
                        Log.d("GnssLocationProvider", "NTP server returned: " + time + " (" + new Date(time) + ") reference: " + timeReference + " certainty: " + certainty + " system time offset: " + (time - now));
                    }
                    GnssLocationProvider.this.native_inject_time(time, timeReference, (int) certainty);
                    delay = 86400000;
                    GnssLocationProvider.this.mNtpBackOff.reset();
                } else {
                    Log.e("GnssLocationProvider", "requestTime failed");
                    delay = GnssLocationProvider.this.mNtpBackOff.nextBackoffMillis();
                }
                GnssLocationProvider.this.sendMessage(10, 0, null);
                if (GnssLocationProvider.DEBUG) {
                    String message = String.format("onDemandTimeInjection=%s, refreshSuccess=%s, delay=%s", Boolean.valueOf(GnssLocationProvider.this.mOnDemandTimeInjection), Boolean.valueOf(refreshSuccess), Long.valueOf(delay));
                    Log.d("GnssLocationProvider", message);
                }
                if (GnssLocationProvider.this.mOnDemandTimeInjection || !refreshSuccess) {
                    GnssLocationProvider.this.mHandler.sendEmptyMessageDelayed(5, delay);
                }
                GnssLocationProvider.this.mWakeLock.release();
                Log.i("GnssLocationProvider", "WakeLock released by handleInjectNtpTime()");
            }
        });
    }

    private void handleDownloadXtraData() {
        if (this.mDownloadXtraDataPending == 1) {
            return;
        }
        if (!isDataNetworkConnected()) {
            this.mDownloadXtraDataPending = 0;
            return;
        }
        this.mDownloadXtraDataPending = 1;
        this.mWakeLock.acquire();
        Log.i("GnssLocationProvider", "WakeLock acquired by handleDownloadXtraData()");
        AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                GpsXtraDownloader xtraDownloader = new GpsXtraDownloader(GnssLocationProvider.this.mProperties);
                byte[] data = xtraDownloader.downloadXtraData();
                if (data != null) {
                    if (GnssLocationProvider.DEBUG) {
                        Log.d("GnssLocationProvider", "calling native_inject_xtra_data");
                    }
                    GnssLocationProvider.this.native_inject_xtra_data(data, data.length);
                    GnssLocationProvider.this.mXtraBackOff.reset();
                }
                GnssLocationProvider.this.sendMessage(11, 0, null);
                if (data == null) {
                    GnssLocationProvider.this.mHandler.sendEmptyMessageDelayed(6, GnssLocationProvider.this.mXtraBackOff.nextBackoffMillis());
                }
                GnssLocationProvider.this.mWakeLock.release();
                Log.i("GnssLocationProvider", "WakeLock released by handleDownloadXtraData()");
            }
        });
    }

    private void handleUpdateLocation(Location location) {
        if (!location.hasAccuracy()) {
            return;
        }
        native_inject_location(location.getLatitude(), location.getLongitude(), location.getAccuracy());
    }

    @Override
    public void enable() {
        synchronized (this.mLock) {
            if (this.mEnabled) {
                return;
            }
            this.mEnabled = true;
            sendMessage(2, 1, null);
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
                Log.e("GnssLocationProvider", "unable to parse SUPL_PORT: " + portString);
            }
        }
        if (this.mSuplServerHost == null || this.mSuplServerPort <= 0 || this.mSuplServerPort > 65535) {
            return;
        }
        native_set_agps_server(1, this.mSuplServerHost, this.mSuplServerPort);
    }

    private int getSuplMode(Properties properties, boolean agpsEnabled, boolean singleShot) {
        if (agpsEnabled) {
            String modeString = properties.getProperty("SUPL_MODE");
            int suplMode = 0;
            if (!TextUtils.isEmpty(modeString)) {
                try {
                    suplMode = Integer.parseInt(modeString);
                } catch (NumberFormatException e) {
                    Log.e("GnssLocationProvider", "unable to parse SUPL_MODE: " + modeString);
                    return 0;
                }
            }
            if (hasCapability(2) && (suplMode & 1) != 0) {
                return 1;
            }
            if (singleShot && hasCapability(4) && (suplMode & 2) != 0) {
                return 2;
            }
        }
        return 0;
    }

    private void handleEnable() {
        if (DEBUG) {
            Log.d("GnssLocationProvider", "handleEnable");
        }
        boolean enabled = native_init();
        if (enabled) {
            this.mSupportsXtra = native_supports_xtra();
            if (this.mSuplServerHost != null) {
                native_set_agps_server(1, this.mSuplServerHost, this.mSuplServerPort);
            }
            if (this.mC2KServerHost != null) {
                native_set_agps_server(2, this.mC2KServerHost, this.mC2KServerPort);
            }
            this.mGnssMeasurementsProvider.onGpsEnabledChanged();
            this.mGnssNavigationMessageProvider.onGpsEnabledChanged();
            return;
        }
        synchronized (this.mLock) {
            this.mEnabled = false;
        }
        Log.w("GnssLocationProvider", "Failed to enable location provider");
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
            Log.d("GnssLocationProvider", "handleDisable");
        }
        updateClientUids(new WorkSource());
        stopNavigating();
        this.mAlarmManager.cancel(this.mWakeupIntent);
        this.mAlarmManager.cancel(this.mTimeoutIntent);
        native_cleanup();
        this.mGnssMeasurementsProvider.onGpsEnabledChanged();
        this.mGnssNavigationMessageProvider.onGpsEnabledChanged();
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
        if (status == this.mStatus && svCount == this.mSvCount) {
            return;
        }
        this.mStatus = status;
        this.mSvCount = svCount;
        this.mLocationExtras.putInt("satellites", svCount);
        this.mStatusUpdateTime = SystemClock.elapsedRealtime();
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
        if (this.mProviderRequest == null || this.mWorkSource == null) {
            return;
        }
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
            Log.d("GnssLocationProvider", "setRequest " + this.mProviderRequest);
        }
        if (this.mProviderRequest.reportLocation && !this.mDisableGps && isEnabled()) {
            updateClientUids(this.mWorkSource);
            this.mFixInterval = (int) this.mProviderRequest.interval;
            if (this.mFixInterval != this.mProviderRequest.interval) {
                Log.w("GnssLocationProvider", "interval overflow: " + this.mProviderRequest.interval);
                this.mFixInterval = Integer.MAX_VALUE;
            }
            if (this.mStarted && hasCapability(1)) {
                Log.d("GnssLocationProvider", "set_position_mode setRequest " + this.mProviderRequest);
                if (native_set_position_mode(this.mPositionMode, 0, this.mFixInterval, 0, 0)) {
                    return;
                }
                Log.e("GnssLocationProvider", "set_position_mode failed in setMinTime()");
                return;
            }
            if (this.mStarted) {
                return;
            }
            startNavigating(singleShot);
            return;
        }
        updateClientUids(new WorkSource());
        stopNavigating();
        this.mAlarmManager.cancel(this.mWakeupIntent);
        this.mAlarmManager.cancel(this.mTimeoutIntent);
    }

    private void updateClientUids(WorkSource source) {
        WorkSource[] changes = this.mClientSource.setReturningDiffs(source);
        if (changes == null) {
            return;
        }
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
                    Log.w("GnssLocationProvider", "RemoteException", e);
                }
            }
        }
        if (goneWork == null) {
            return;
        }
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
                Log.w("GnssLocationProvider", "RemoteException", e2);
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
            requestUtcTime();
            result = true;
        } else if ("force_xtra_injection".equals(command)) {
            if (this.mSupportsXtra) {
                xtraDownloadRequest();
                result = true;
            }
        } else {
            Log.w("GnssLocationProvider", "sendExtraCommand: unknown command " + command);
        }
        Binder.restoreCallingIdentity(identity);
        return result;
    }

    private boolean deleteAidingData(Bundle extras) {
        int flags;
        if (extras == null) {
            flags = 65535;
        } else {
            flags = extras.getBoolean("ephemeris") ? 1 : 0;
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
                flags |= 32768;
            }
            if (extras.getBoolean("all")) {
                flags |= 65535;
            }
        }
        int flags2 = LocationExt.deleteAidingData(extras, flags);
        if (flags2 == 0) {
            return false;
        }
        native_delete_aiding_data(flags2);
        return true;
    }

    private void startNavigating(boolean singleShot) {
        String mode;
        if (this.mStarted) {
            return;
        }
        Log.d("GnssLocationProvider", "startNavigating, singleShot is " + singleShot + " setRequest: " + this.mProviderRequest);
        this.mTimeToFirstFix = 0;
        this.mLastFixTime = 0L;
        this.mStarted = true;
        this.mSingleShot = singleShot;
        this.mPositionMode = 0;
        LocationExt.startNavigating(singleShot);
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
            Log.d("GnssLocationProvider", "setting position_mode to " + mode);
        }
        int interval = hasCapability(1) ? this.mFixInterval : 1000;
        if (!native_set_position_mode(this.mPositionMode, 0, interval, 0, 0)) {
            this.mStarted = false;
            Log.e("GnssLocationProvider", "set_position_mode failed in startNavigating()");
        } else {
            if (!native_start()) {
                this.mStarted = false;
                Log.e("GnssLocationProvider", "native_start failed in startNavigating()");
                return;
            }
            updateStatus(1, 0);
            this.mFixRequestTime = SystemClock.elapsedRealtime();
            if (hasCapability(1) || this.mFixInterval < NO_FIX_TIMEOUT) {
                return;
            }
            this.mAlarmManager.set(2, SystemClock.elapsedRealtime() + 60000, this.mTimeoutIntent);
        }
    }

    private void stopNavigating() {
        if (!this.mStarted) {
            return;
        }
        Log.d("GnssLocationProvider", "stopNavigating");
        this.mStarted = false;
        this.mSingleShot = false;
        native_stop();
        this.mTimeToFirstFix = 0;
        this.mLastFixTime = 0L;
        this.mLocationFlags = 0;
        updateStatus(1, 0);
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
            Log.v("GnssLocationProvider", "reportLocation lat: " + latitude + " long: " + longitude + " timestamp: " + timestamp);
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
                Log.e("GnssLocationProvider", "RemoteException calling reportLocation");
            }
        }
        this.mLastFixTime = SystemClock.elapsedRealtime();
        if (this.mTimeToFirstFix == 0 && (flags & 1) == 1) {
            this.mTimeToFirstFix = (int) (this.mLastFixTime - this.mFixRequestTime);
            if (DEBUG) {
                Log.d("GnssLocationProvider", "TTFF: " + this.mTimeToFirstFix);
            }
            this.mListenerHelper.onFirstFix(this.mTimeToFirstFix);
        }
        LocationExt.doSystemTimeSyncByGps(flags, timestamp);
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
            updateStatus(2, this.mSvCount);
        }
        if (hasCapability(1) || !this.mStarted || this.mFixInterval <= 10000) {
            return;
        }
        if (DEBUG) {
            Log.d("GnssLocationProvider", "got fix, hibernating");
        }
        hibernate();
    }

    private void reportStatus(int status) {
        if (DEBUG) {
            Log.v("GnssLocationProvider", "reportStatus status: " + status);
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
        if (wasNavigating == this.mNavigating) {
            return;
        }
        this.mListenerHelper.onStatusChanged(this.mNavigating);
        Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
        intent.putExtra("enabled", this.mNavigating);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void reportSvStatus() {
        int svCount = native_read_sv_status(this.mSvidWithFlags, this.mCn0s, this.mSvElevations, this.mSvAzimuths);
        this.mListenerHelper.onSvStatusChanged(svCount, this.mSvidWithFlags, this.mCn0s, this.mSvElevations, this.mSvAzimuths);
        if (VERBOSE) {
            Log.v("GnssLocationProvider", "SV count: " + svCount);
        }
        int usedInFixCount = 0;
        for (int i = 0; i < svCount; i++) {
            if ((this.mSvidWithFlags[i] & 4) != 0) {
                usedInFixCount++;
            }
            if (VERBOSE) {
                Log.v("GnssLocationProvider", "svid: " + (this.mSvidWithFlags[i] >> 7) + " cn0: " + (this.mCn0s[i] / 10.0f) + " elev: " + this.mSvElevations[i] + " azimuth: " + this.mSvAzimuths[i] + ((this.mSvidWithFlags[i] & 1) == 0 ? "  " : " E") + ((this.mSvidWithFlags[i] & 2) == 0 ? "  " : " A") + ((this.mSvidWithFlags[i] & 4) == 0 ? "" : "U"));
            }
        }
        updateStatus(this.mStatus, usedInFixCount);
        if (!this.mNavigating || this.mStatus != 2 || this.mLastFixTime <= 0 || SystemClock.elapsedRealtime() - this.mLastFixTime <= 10000) {
            return;
        }
        Intent intent = new Intent("android.location.GPS_FIX_CHANGE");
        intent.putExtra("enabled", false);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        updateStatus(1, this.mSvCount);
    }

    private void reportAGpsStatus(int type, int status, byte[] ipaddr) {
        switch (status) {
            case 1:
                if (DEBUG) {
                    Log.d("GnssLocationProvider", "GPS_REQUEST_AGPS_DATA_CONN");
                }
                Log.v("GnssLocationProvider", "Received SUPL IP addr[]: " + Arrays.toString(ipaddr));
                InetAddress connectionIpAddress = null;
                if (ipaddr != null) {
                    try {
                        connectionIpAddress = InetAddress.getByAddress(ipaddr);
                        if (DEBUG) {
                            Log.d("GnssLocationProvider", "IP address converted to: " + connectionIpAddress);
                        }
                    } catch (UnknownHostException e) {
                        Log.e("GnssLocationProvider", "Bad IP Address: " + ipaddr, e);
                    }
                }
                sendMessage(14, 0, connectionIpAddress);
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
                    Log.d("GnssLocationProvider", "Received Unknown AGPS status: " + status);
                }
                break;
        }
    }

    private void releaseSuplConnection(int connStatus) {
        sendMessage(15, connStatus, null);
    }

    private void reportNmea(long timestamp) {
        int length = native_read_nmea(this.mNmeaBuffer, this.mNmeaBuffer.length);
        String nmea = new String(this.mNmeaBuffer, 0, length);
        this.mListenerHelper.onNmeaReceived(timestamp, nmea);
    }

    private void reportMeasurementData(GnssMeasurementsEvent event) {
        this.mGnssMeasurementsProvider.onMeasurementsAvailable(event);
    }

    private void reportNavigationMessage(GnssNavigationMessage event) {
        this.mGnssNavigationMessageProvider.onNavigationMessageAvailable(event);
    }

    private void setEngineCapabilities(int capabilities) {
        this.mEngineCapabilities = capabilities;
        if (hasCapability(16)) {
            this.mOnDemandTimeInjection = true;
            requestUtcTime();
        }
        this.mGnssMeasurementsProvider.onCapabilitiesUpdated((capabilities & 64) == 64);
        this.mGnssNavigationMessageProvider.onCapabilitiesUpdated((capabilities & 128) == 128);
    }

    private void setGnssYearOfHardware(int yearOfHardware) {
        if (DEBUG) {
            Log.d("GnssLocationProvider", "setGnssYearOfHardware called with " + yearOfHardware);
        }
        this.mYearOfHardware = yearOfHardware;
    }

    public GnssSystemInfoProvider getGnssSystemInfoProvider() {
        return new GnssSystemInfoProvider() {
            @Override
            public int getGnssYearOfHardware() {
                return GnssLocationProvider.this.mYearOfHardware;
            }
        };
    }

    private void xtraDownloadRequest() {
        if (DEBUG) {
            Log.d("GnssLocationProvider", "xtraDownloadRequest");
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
        Log.i("GnssLocationProvider", "reportNiNotification: entered");
        Log.i("GnssLocationProvider", "notificationId: " + notificationId + ", niType: " + niType + ", notifyFlags: " + notifyFlags + ", timeout: " + timeout + ", defaultResponse: " + defaultResponse);
        Log.i("GnssLocationProvider", "requestorId: " + requestorId + ", text: " + text + ", requestorIdEncoding: " + requestorIdEncoding + ", textEncoding: " + textEncoding);
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
            Log.e("GnssLocationProvider", "reportNiNotification cannot parse extras data: " + extras);
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
        if (DEBUG) {
            Log.d("GnssLocationProvider", "utcTimeRequest");
        }
        sendMessage(5, 0, null);
    }

    private void requestRefLocation(int flags) {
        int type;
        TelephonyManager phone = (TelephonyManager) this.mContext.getSystemService("phone");
        int phoneType = phone.getPhoneType();
        if (phoneType == 1) {
            GsmCellLocation gsm_cell = (GsmCellLocation) phone.getCellLocation();
            if (gsm_cell != null && phone.getNetworkOperator() != null && phone.getNetworkOperator().length() > 3) {
                int mcc = Integer.parseInt(phone.getNetworkOperator().substring(0, 3));
                int mnc = Integer.parseInt(phone.getNetworkOperator().substring(3));
                int networkType = phone.getNetworkType();
                if (networkType == 3 || networkType == 8 || networkType == 9 || networkType == 10 || networkType == 15) {
                    type = 2;
                } else {
                    type = 1;
                }
                native_agps_set_ref_location_cellid(type, mcc, mnc, gsm_cell.getLac(), gsm_cell.getCid());
                return;
            }
            Log.e("GnssLocationProvider", "Error getting cell location info.");
            return;
        }
        if (phoneType != 2) {
            return;
        }
        Log.e("GnssLocationProvider", "CDMA not supported.");
    }

    private void sendMessage(int message, int arg, Object obj) {
        this.mWakeLock.acquire();
        if (DEBUG) {
            Log.i("GnssLocationProvider", "WakeLock acquired by sendMessage(" + message + ", " + arg + ", " + obj + ")");
        }
        this.mHandler.obtainMessage(message, arg, 1, obj).sendToTarget();
    }

    private final class ProviderHandler extends Handler {
        public ProviderHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message msg) throws Throwable {
            int message = msg.what;
            switch (message) {
                case 2:
                    if (msg.arg1 != 1) {
                        GnssLocationProvider.this.handleDisable();
                    } else {
                        GnssLocationProvider.this.handleEnable();
                    }
                    break;
                case 3:
                    GpsRequest gpsRequest = (GpsRequest) msg.obj;
                    GnssLocationProvider.this.handleSetRequest(gpsRequest.request, gpsRequest.source);
                    break;
                case 4:
                    GnssLocationProvider.this.handleUpdateNetworkState((Network) msg.obj);
                    break;
                case 5:
                    GnssLocationProvider.this.handleInjectNtpTime();
                    break;
                case 6:
                    if (GnssLocationProvider.this.mSupportsXtra) {
                        GnssLocationProvider.this.handleDownloadXtraData();
                    }
                    break;
                case 7:
                    GnssLocationProvider.this.handleUpdateLocation((Location) msg.obj);
                    break;
                case 10:
                    GnssLocationProvider.this.mInjectNtpTimePending = 2;
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
                    GnssLocationProvider.this.handleRequestSuplConnection((InetAddress) msg.obj);
                    break;
                case 15:
                    GnssLocationProvider.this.handleReleaseSuplConnection(msg.arg1);
                    break;
            }
            if (msg.arg2 == 1) {
                GnssLocationProvider.this.mWakeLock.release();
                if (GnssLocationProvider.DEBUG) {
                    Log.i("GnssLocationProvider", "WakeLock released by handleMessage(" + message + ", " + msg.arg1 + ", " + msg.obj + ")");
                }
            }
        }

        private void handleInitialize() throws Throwable {
            NetworkLocationListener networkLocationListener = null;
            GnssLocationProvider.this.reloadGpsProperties(GnssLocationProvider.this.mContext, GnssLocationProvider.this.mProperties);
            SubscriptionManager.from(GnssLocationProvider.this.mContext).addOnSubscriptionsChangedListener(GnssLocationProvider.this.mOnSubscriptionsChangedListener);
            if (GnssLocationProvider.native_is_agps_ril_supported()) {
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
            } else if (GnssLocationProvider.DEBUG) {
                Log.d("GnssLocationProvider", "Skipped registration for SMS/WAP-PUSH messages because AGPS Ril in GPS HAL is not supported");
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
            NetworkRequest.Builder networkRequestBuilder = new NetworkRequest.Builder();
            networkRequestBuilder.addTransportType(0);
            networkRequestBuilder.addTransportType(1);
            NetworkRequest networkRequest = networkRequestBuilder.build();
            GnssLocationProvider.this.mConnMgr.registerNetworkCallback(networkRequest, GnssLocationProvider.this.mNetworkConnectivityCallback);
            LocationManager locManager = (LocationManager) GnssLocationProvider.this.mContext.getSystemService("location");
            LocationRequest request = LocationRequest.createFromDeprecatedProvider("passive", 0L, 0.0f, false);
            request.setHideFromAppOps(true);
            locManager.requestLocationUpdates(request, new NetworkLocationListener(GnssLocationProvider.this, networkLocationListener), getLooper());
        }
    }

    private final class NetworkLocationListener implements LocationListener {
        NetworkLocationListener(GnssLocationProvider this$0, NetworkLocationListener networkLocationListener) {
            this();
        }

        private NetworkLocationListener() {
        }

        @Override
        public void onLocationChanged(Location location) {
            if (!"network".equals(location.getProvider())) {
                return;
            }
            GnssLocationProvider.this.handleUpdateLocation(location);
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
        Cursor cursor;
        Uri uri = Uri.parse("content://telephony/carriers/preferapn");
        Cursor cursor2 = null;
        try {
            try {
                cursor = this.mContext.getContentResolver().query(uri, new String[]{"apn"}, null, null, "name ASC");
            } catch (Exception e) {
                Log.e("GnssLocationProvider", "Error encountered on selecting the APN.", e);
                if (0 != 0) {
                    cursor2.close();
                }
            }
            if (cursor == null || !cursor.moveToFirst()) {
                Log.e("GnssLocationProvider", "No APN found to select.");
                if (cursor != null) {
                    cursor.close();
                }
                return null;
            }
            String string = cursor.getString(0);
            if (cursor != null) {
                cursor.close();
            }
            return string;
        } catch (Throwable th) {
            if (0 != 0) {
                cursor2.close();
            }
            throw th;
        }
    }

    private int getApnIpType(String apn) {
        Cursor cursor;
        ensureInHandlerThread();
        if (apn == null) {
            return 0;
        }
        String selection = String.format("current = 1 and apn = '%s' and carrier_enabled = 1", apn);
        Cursor cursor2 = null;
        try {
            try {
                cursor = this.mContext.getContentResolver().query(Telephony.Carriers.CONTENT_URI, new String[]{"protocol"}, selection, null, "name ASC");
            } catch (Exception e) {
                Log.e("GnssLocationProvider", "Error encountered on APN query for: " + apn, e);
                if (0 != 0) {
                    cursor2.close();
                }
            }
            if (cursor == null || !cursor.moveToFirst()) {
                Log.e("GnssLocationProvider", "No entry found in query for APN: " + apn);
                if (cursor != null) {
                    cursor.close();
                }
                return 0;
            }
            int iTranslateToApnIpType = translateToApnIpType(cursor.getString(0), apn);
            if (cursor != null) {
                cursor.close();
            }
            return iTranslateToApnIpType;
        } catch (Throwable th) {
            if (0 != 0) {
                cursor2.close();
            }
            throw th;
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
        Log.e("GnssLocationProvider", message);
        return 0;
    }

    private void setRouting() {
        if (this.mAGpsDataConnectionIpAddr == null) {
            return;
        }
        boolean result = this.mConnMgr.requestRouteToHostAddress(3, this.mAGpsDataConnectionIpAddr);
        if (!result) {
            Log.e("GnssLocationProvider", "Error requesting route to host: " + this.mAGpsDataConnectionIpAddr);
        } else {
            if (!DEBUG) {
                return;
            }
            Log.d("GnssLocationProvider", "Successfully requested route to host: " + this.mAGpsDataConnectionIpAddr);
        }
    }

    private boolean isDataNetworkConnected() {
        NetworkInfo activeNetworkInfo = this.mConnMgr.getActiveNetworkInfo();
        if (activeNetworkInfo != null) {
            return activeNetworkInfo.isConnected();
        }
        return false;
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

    private String agpsDataConnStatusAsString(int agpsDataConnStatus) {
        switch (agpsDataConnStatus) {
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

    public void initLocationExt() {
        boolean mtkGpsSupport = SystemProperties.get("ro.mtk_gps_support").equals("1");
        if (!mtkGpsSupport) {
            return;
        }
        this.mDownloadXtraDataPending = 2;
        this.mInjectNtpTimePending = 2;
        LocationExt.getInstance(this, this.mContext, this.mHandler, this.mConnMgr);
        Log.d("GnssLocationProvider", "LocationExt is created");
    }

    public void setVzwDebugScreen(boolean enabled) {
        Log.d("GnssLocationProvider", "setVzwDebugScreen enabled= " + enabled);
        native_set_vzw_debug_screen(enabled);
    }

    public void reportVzwDebugMessage(String vzw_msg) {
        Log.d("GnssLocationProvider", "reportVzwDebugMessage vzw_msg: " + vzw_msg);
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        StringBuilder s = new StringBuilder();
        s.append("  mFixInterval=").append(this.mFixInterval).append('\n');
        s.append("  mDisableGps (battery saver mode)=").append(this.mDisableGps).append('\n');
        s.append("  mEngineCapabilities=0x").append(Integer.toHexString(this.mEngineCapabilities));
        s.append(" ( ");
        if (hasCapability(1)) {
            s.append("SCHEDULING ");
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
        if (hasCapability(32)) {
            s.append("GEOFENCING ");
        }
        if (hasCapability(64)) {
            s.append("MEASUREMENTS ");
        }
        if (hasCapability(128)) {
            s.append("NAV_MESSAGES ");
        }
        s.append(")\n");
        s.append(native_get_internal_state());
        pw.append((CharSequence) s);
    }

    private static final class BackOff {
        private static final int MULTIPLIER = 2;
        private long mCurrentIntervalMillis;
        private final long mInitIntervalMillis;
        private final long mMaxIntervalMillis;

        public BackOff(long initIntervalMillis, long maxIntervalMillis) {
            this.mInitIntervalMillis = initIntervalMillis;
            this.mMaxIntervalMillis = maxIntervalMillis;
            this.mCurrentIntervalMillis = this.mInitIntervalMillis / 2;
        }

        public long nextBackoffMillis() {
            if (this.mCurrentIntervalMillis > this.mMaxIntervalMillis) {
                return this.mMaxIntervalMillis;
            }
            this.mCurrentIntervalMillis *= 2;
            return this.mCurrentIntervalMillis;
        }

        public void reset() {
            this.mCurrentIntervalMillis = this.mInitIntervalMillis / 2;
        }
    }
}
