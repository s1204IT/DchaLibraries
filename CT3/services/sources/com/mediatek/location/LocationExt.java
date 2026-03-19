package com.mediatek.location;

import android.R;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.server.LocationManagerService;
import com.android.server.audio.AudioService;
import com.android.server.location.GnssLocationProvider;
import com.mediatek.lbsutils.LbsUtils;
import java.io.File;
import java.util.Calendar;
import java.util.HashMap;

public class LocationExt {
    private static final boolean DEBUG = LocationManagerService.D;
    private static final int GPS_DELETE_EPO = 16384;
    private static final int GPS_DELETE_HOT_STILL = 8192;
    private static final int LOCATION_HAS_LAT_LONG = 1;
    public static final int SUPL_CONN_AVAILABLE = 2;
    public static final int SUPL_CONN_LOST = 1;
    public static final int SUPL_CONN_UNAVALIABLE = 0;
    private static final String TAG = "MtkLocationExt";
    private static final int UPDATE_NETWORK_STATE = 4;
    private static LocationExt sSingleton;
    private AgpsHelper mAgpsHelper;
    private C2kAgpsInterface mAgpsInterface;
    private final ConnectivityManager mConnMgr;
    private final Context mContext;
    private final GnssLocationProvider mGnssProvider;
    private final Handler mGpsHandler;
    private boolean mIsEmergencyCallDialed;
    private NlpUtils nlpUtils;
    private boolean mGpsTimeSyncFlag = true;
    private int mRouteNetworkType = 3;
    private GnssSvStatusHolder mGnssSvStatusHolder = new GnssSvStatusHolder();
    private String mFeature = "enableSUPL";
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (LocationExt.DEBUG) {
                Log.d(LocationExt.TAG, "receive broadcast intent, action: " + action);
            }
            if ("android.location.agps.EMERGENCY_CALL".equals(action)) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    int state = bundle.getInt("EM_Call_State");
                    if (1 == state) {
                        if (LocationExt.DEBUG) {
                            Log.d(LocationExt.TAG, "E911 dialed");
                        }
                        LocationExt.this.mIsEmergencyCallDialed = true;
                        return;
                    } else {
                        if (LocationExt.DEBUG) {
                            Log.d(LocationExt.TAG, "E911 ended");
                        }
                        LocationExt.this.mIsEmergencyCallDialed = false;
                        return;
                    }
                }
                Log.e(LocationExt.TAG, "E911 null bundle");
                return;
            }
            if (!"android.intent.action.AIRPLANE_MODE".equals(action)) {
                return;
            }
            boolean enabled = intent.getBooleanExtra(AudioService.CONNECT_INTENT_KEY_STATE, false);
            if (LocationExt.DEBUG) {
                Log.d(LocationExt.TAG, "ACTION_AIRPLANE_MODE_CHANGED enabled =" + enabled);
            }
            LocationExt.this.mAgpsInterface.setFlightMode(enabled);
        }
    };

    public static synchronized LocationExt getInstance(GnssLocationProvider gnssProvider, Context context, Handler gpsHandler, ConnectivityManager connMgr) {
        if (sSingleton == null && gnssProvider != null) {
            sSingleton = new LocationExt(gnssProvider, context, gpsHandler, connMgr);
        }
        return sSingleton;
    }

    public static boolean isEnabled() {
        return sSingleton != null;
    }

    public static boolean checkWapSuplInit(Intent intent) {
        if (!isEnabled()) {
            return true;
        }
        boolean ret = sSingleton.isWapPushLegal(intent);
        if (DEBUG) {
            Log.d(TAG, "[agps] WARNING: checkWapSuplInit ret=" + ret);
        }
        return ret;
    }

    public static int deleteAidingData(Bundle extras, int flags) {
        if (!isEnabled()) {
            return flags;
        }
        if (extras != null) {
            if (extras.getBoolean("hot-still")) {
                flags |= 8192;
            }
            if (extras.getBoolean("epo")) {
                flags |= 16384;
            }
        }
        Log.d(TAG, "deleteAidingData extras:" + extras + "flags:" + flags);
        return flags;
    }

    public static boolean setGpsTimeSyncFlag(boolean flag) {
        if (!isEnabled()) {
            return false;
        }
        sSingleton.mGpsTimeSyncFlag = flag;
        if (DEBUG) {
            Log.d(TAG, "setGpsTimeSyncFlag: " + flag);
        }
        return flag;
    }

    public static void startNavigating(boolean singleShot) {
        setGpsTimeSyncFlag(true);
    }

    public static void doSystemTimeSyncByGps(int flags, long timestamp) {
        if (!isEnabled() || !sSingleton.mGpsTimeSyncFlag || (flags & 1) != 1) {
            return;
        }
        if (sSingleton.getAutoGpsState()) {
            sSingleton.mGpsTimeSyncFlag = false;
            if (DEBUG) {
                Log.d(TAG, "GPS time sync is enabled");
            }
            if (DEBUG) {
                Log.d(TAG, " ########## Auto-sync time with GPS: timestamp = " + timestamp + " ########## ");
            }
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(timestamp);
            long when = c.getTimeInMillis();
            if (when / 1000 >= 2147483647L) {
                return;
            }
            SystemClock.setCurrentTimeMillis(when);
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "Auto-sync time with GPS is disabled by user settings!");
        }
        if (DEBUG) {
            Log.d(TAG, "GPS time sync is disabled");
        }
    }

    public static GnssSvStatusHolder getGnssSvStatusHolder() {
        if (isEnabled()) {
            return sSingleton.mGnssSvStatusHolder;
        }
        return null;
    }

    public static int getRouteNetworkType() {
        if (isEnabled()) {
            return sSingleton.mRouteNetworkType;
        }
        return 3;
    }

    public static int doStartUsingNetwork(ConnectivityManager connMgr, int networkType, String feature) {
        if (!isEnabled()) {
            return -1;
        }
        return sSingleton.doMtkStartUsingNetwork();
    }

    public static int doStopUsingNetwork(ConnectivityManager connMgr, int networkType, String feature) {
        if (!isEnabled()) {
            return -1;
        }
        sSingleton.doMtkStopUsingNetwork();
        return 0;
    }

    public static int suplConnectionCallback(int state, Network network) {
        if (!isEnabled()) {
            return -1;
        }
        sSingleton.doMtkSuplConnectionCallback(state, network);
        return 0;
    }

    public static void updateNetworkAvailable(Network network) {
        if (!isEnabled()) {
            return;
        }
        sSingleton.doUpdateNetworkAvailable(network);
    }

    public static boolean isFileExists(String path) {
        File f = new File(path);
        return f.exists();
    }

    public static boolean isESUPL() {
        return isFileExists("/data/agps_supl/isESUPL");
    }

    public static boolean isCtwap() {
        return isFileExists("/data/agps_supl/ctwap");
    }

    private LocationExt(GnssLocationProvider gnssProvider, Context context, Handler gpsHandler, ConnectivityManager connMgr) {
        this.mGnssProvider = gnssProvider;
        this.mContext = context;
        this.mGpsHandler = gpsHandler;
        this.mConnMgr = connMgr;
        this.mAgpsInterface = new C2kAgpsInterface(connMgr);
        listenForBroadcasts();
        this.nlpUtils = new NlpUtils(context, this.mGpsHandler);
        this.mAgpsHelper = new AgpsHelper(this, context, connMgr);
        LbsUtils lbsUtils = LbsUtils.getInstance(this.mContext);
        Resources resources = this.mContext.getResources();
        String[] gmsLpPkgs = resources.getStringArray(R.array.config_callBarringMMI_for_ims);
        String[] vendorLpPkgs = resources.getStringArray(com.mediatek.internal.R.array.config_cnLocationProviderPackageNames);
        lbsUtils.setHandler(gpsHandler);
        lbsUtils.listenPhoneState(gmsLpPkgs);
        lbsUtils.setVendorLpPkgs(vendorLpPkgs);
        if (this.mGnssProvider == null || isAllowedByUserSettingsLocked("gps")) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "init GPS in location off mode");
        }
        this.mGnssProvider.enable();
        this.mGnssProvider.disable();
    }

    private boolean isAllowedByUserSettingsLocked(String provider) {
        ContentResolver resolver = this.mContext.getContentResolver();
        return Settings.Secure.isLocationProviderEnabledForUser(resolver, provider, 0);
    }

    private void listenForBroadcasts() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.location.agps.EMERGENCY_CALL");
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter, null, this.mGpsHandler);
        this.mAgpsInterface.setFlightMode(isAirplaneModeOn());
        this.mContext.registerReceiver(this.mBroadcastReceiver, new IntentFilter("android.intent.action.AIRPLANE_MODE"), null, this.mGpsHandler);
    }

    private boolean isWapPushLegal(Intent intent) {
        try {
            String type = intent.getType();
            if (type == null || !type.equals("application/vnd.omaloc-supl-init")) {
                Log.e(TAG, "[agps] ERR: content type is [" + type + "], but we expect [application/vnd.omaloc-supl-init]");
                return false;
            }
            Bundle bundle = intent.getExtras();
            if (bundle == null) {
                Log.e(TAG, "[agps] ERR: wspBundle is null");
                return false;
            }
            HashMap<String, String> wspHeaders = (HashMap) bundle.get("wspHeaders");
            if (wspHeaders == null) {
                Log.e(TAG, "[agps] ERR: wspHeader is null");
                return false;
            }
            String appId = wspHeaders.get("X-Wap-Application-Id");
            if (appId == null) {
                Log.e(TAG, "[agps] ERR: appId(X-Wap-Application-Id) is null");
                return false;
            }
            if (!appId.equals("x-oma-application:ulp.ua")) {
                Log.e(TAG, "[agps] ERR: appId is [" + appId + "], but we expect [x-oma-application:ulp.ua]");
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean getAutoGpsState() {
        try {
            return Settings.System.getInt(this.mContext.getContentResolver(), "auto_time_gps") > 0;
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
    }

    public class GnssSvStatusHolder {
        public static final int MAX_GNSS_SVS = 256;
        public int[] mGnssSvs = new int[256];
        public float[] mGnssSnrs = new float[256];
        public float[] mGnssSvElevations = new float[256];
        public float[] mGnssSvAzimuths = new float[256];
        public boolean[] mGnssSvEphemeris = new boolean[256];
        public boolean[] mGnssSvAlmanac = new boolean[256];
        public boolean[] mGnssSvInFix = new boolean[256];

        public GnssSvStatusHolder() {
        }

        public int reportGnssSvStatusStep2(int svCount) {
            if (LocationExt.DEBUG) {
                Log.v(LocationExt.TAG, "GNSS SV count: " + svCount);
                for (int i = 0; i < svCount; i++) {
                    Log.v(LocationExt.TAG, "sv: " + this.mGnssSvs[i] + " snr: " + (this.mGnssSnrs[i] / 10.0f) + " elev: " + this.mGnssSvElevations[i] + " azimuth: " + this.mGnssSvAzimuths[i] + (this.mGnssSvEphemeris[i] ? " E" : " ") + (this.mGnssSvAlmanac[i] ? " A" : " ") + (this.mGnssSvInFix[i] ? " U" : " "));
                }
            }
            int svFixCount = 0;
            for (boolean value : this.mGnssSvInFix) {
                if (value) {
                    svFixCount++;
                }
            }
            return svFixCount;
        }

        public boolean reportGnssSvStatusStep3(boolean navigating, int gpsStatus, long lastFixTime, long recentFixTimeout) {
            if (!navigating || gpsStatus != 2 || lastFixTime <= 0 || SystemClock.elapsedRealtime() - lastFixTime <= recentFixTimeout) {
                return false;
            }
            Intent intent = new Intent("android.location.GPS_FIX_CHANGE");
            intent.putExtra("enabled", false);
            LocationExt.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            return true;
        }
    }

    boolean isEmergencyCallDialed() {
        if (isFileExists("/data/agps_supl/isEmergencyCallDialed")) {
            return true;
        }
        return this.mIsEmergencyCallDialed;
    }

    boolean hasIccCard() {
        TelephonyManager tpMgr = (TelephonyManager) this.mContext.getSystemService("phone");
        if (tpMgr != null) {
            return tpMgr.hasIccCard();
        }
        return false;
    }

    boolean isAirplaneModeOn() {
        return Settings.System.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1;
    }

    private int doMtkStartUsingNetwork() {
        String feature = "enableSUPL";
        this.mRouteNetworkType = 3;
        TelephonyManager phone = (TelephonyManager) this.mContext.getSystemService("phone");
        int phoneNetwokrType = phone.getNetworkType();
        Log.d(TAG, "[agps] WARNING: GnssLocationProvider  phoneNetwokrType=[" + phoneNetwokrType + "] isESUPL=[" + isESUPL() + "] isEmergencyCallDialed=[" + isEmergencyCallDialed() + "]");
        if (phoneNetwokrType == 13 && isESUPL()) {
            if (isEmergencyCallDialed()) {
                feature = "enableEmergency";
                this.mRouteNetworkType = 15;
            } else {
                feature = "enableIMS";
                this.mRouteNetworkType = 11;
            }
        } else if (isCtwap()) {
            this.mRouteNetworkType = 2;
            this.mAgpsInterface.requestNetwork();
            return 1;
        }
        if (("enableSUPL" != feature || hasIccCard()) && !isAirplaneModeOn()) {
            this.mFeature = feature;
            return this.mConnMgr.startUsingNetworkFeature(0, feature);
        }
        Log.d(TAG, "[agps] APN_REQUEST_FAILED: hasIccCard=" + hasIccCard() + " isAirplaneModeOn=" + isAirplaneModeOn());
        return 3;
    }

    private void doMtkStopUsingNetwork() {
        if (this.mRouteNetworkType == 2) {
            this.mAgpsInterface.releaseNetwork();
        } else {
            this.mConnMgr.stopUsingNetworkFeature(0, this.mFeature);
        }
    }

    private void doMtkSuplConnectionCallback(int state, Network network) {
        this.mAgpsInterface.doMtkSuplConnectionCallback(state, network);
    }

    private void doUpdateNetworkAvailable(Network network) {
        this.mGpsHandler.obtainMessage(4, 0, 0, network).sendToTarget();
    }
}
