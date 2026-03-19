package com.mediatek.lbsutils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.location.ILocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.ServiceState;
import android.text.TextUtils;
import android.util.Log;
import java.util.List;

public class LbsUtils {
    private static final boolean DEBUG = true;
    private static final String NETWORK_LOCATION_SERVICE_ACTION = "com.android.location.service.v3.NetworkLocationProvider";
    private static LbsUtils sInstance;
    private final ConnectivityManager mConnMgr;
    private Context mContext;
    private String[] mGmsLpPkgs;
    private String mMccMnc;
    private PackageManager mPackageManager;
    private String[] mVendorLpPkgs;
    private String mVendorNlpPackageName;
    private int mUsbPlugged = 0;
    private int mWifiConnected = 0;
    private int mStayAwake = 0;
    private Handler mGpsHandler = null;
    private boolean mInTestMode = false;
    private Object mLock = new Object();
    private final ConnectivityManager.NetworkCallback mNetworkConnectivityCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            if (LbsUtils.this.mWifiConnected == 0) {
                LbsUtils.this.mWifiConnected = 1;
                LbsUtils.log("wifi connected state changed: " + LbsUtils.this.mWifiConnected);
                LbsUtils.this.testModeConditionChanged();
            }
        }

        @Override
        public void onLost(Network network) {
            if (LbsUtils.this.mWifiConnected == 1) {
                LbsUtils.this.mWifiConnected = 0;
                LbsUtils.log("wifi connected state changed: " + LbsUtils.this.mWifiConnected);
                LbsUtils.this.testModeConditionChanged();
            }
        }

        @Override
        public void onUnavailable() {
            if (LbsUtils.this.mWifiConnected == 1) {
                LbsUtils.this.mWifiConnected = 0;
                LbsUtils.log("wifi connected state changed: " + LbsUtils.this.mWifiConnected);
                LbsUtils.this.testModeConditionChanged();
            }
        }
    };

    public static LbsUtils getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new LbsUtils(context);
        }
        return sInstance;
    }

    private LbsUtils(Context context) {
        log("LbsUtils constructor");
        this.mContext = context;
        this.mPackageManager = this.mContext.getPackageManager();
        this.mConnMgr = (ConnectivityManager) context.getSystemService("connectivity");
    }

    public void setHandler(Handler handler) {
        this.mGpsHandler = handler;
    }

    public void listenPhoneState(String[] strArr) {
        log("listenPhoneState");
        this.mGmsLpPkgs = strArr;
        registerAutoSwitchNlpFilter();
    }

    public void setVendorLpPkgs(String[] strArr) {
        log("setVendorLpPkgs");
        this.mVendorLpPkgs = strArr;
    }

    private void registerAutoSwitchNlpFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SERVICE_STATE");
        intentFilter.addAction("android.intent.action.BATTERY_CHANGED");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int intExtra;
                String action = intent.getAction();
                if ("android.intent.action.SERVICE_STATE".equals(action)) {
                    LbsUtils.this.serviceStateChanged(intent);
                    return;
                }
                if ("android.intent.action.BATTERY_CHANGED".equals(action) && (intExtra = intent.getIntExtra("plugged", 0)) != LbsUtils.this.mUsbPlugged) {
                    LbsUtils.log("usb plugged state changed: " + intExtra);
                    LbsUtils.this.mUsbPlugged = intExtra;
                    if (LbsUtils.this.mUsbPlugged == 0) {
                        LbsUtils.this.mInTestMode = false;
                    }
                    LbsUtils.this.testModeConditionChanged();
                }
            }
        }, intentFilter);
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addTransportType(1);
        this.mConnMgr.registerNetworkCallback(builder.build(), this.mNetworkConnectivityCallback);
        if (this.mGpsHandler != null) {
            try {
                this.mStayAwake = Settings.Global.getInt(this.mContext.getContentResolver(), "stay_on_while_plugged_in");
            } catch (Settings.SettingNotFoundException e) {
                log("settings not found exception");
            }
            this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("stay_on_while_plugged_in"), true, new ContentObserver(this.mGpsHandler) {
                @Override
                public void onChange(boolean z) {
                    try {
                        int i = Settings.Global.getInt(LbsUtils.this.mContext.getContentResolver(), "stay_on_while_plugged_in");
                        if (i != LbsUtils.this.mStayAwake) {
                            LbsUtils.log("Stay awake state changed: " + i);
                            LbsUtils.this.mStayAwake = i;
                            LbsUtils.this.testModeConditionChanged();
                        }
                    } catch (Settings.SettingNotFoundException e2) {
                        LbsUtils.log("settings not found exception");
                    }
                }
            }, -1);
        }
    }

    private int getNetworkProviderCount() {
        List listQueryIntentServicesAsUser = this.mPackageManager.queryIntentServicesAsUser(new Intent(NETWORK_LOCATION_SERVICE_ACTION), 128, 0);
        if (listQueryIntentServicesAsUser != null) {
            log("installed NLP count= " + listQueryIntentServicesAsUser.size());
            return listQueryIntentServicesAsUser.size();
        }
        log("installed NLP count= 0");
        return 0;
    }

    private void serviceStateChanged(Intent intent) {
        synchronized (this.mLock) {
            ServiceState serviceStateNewFromBundle = ServiceState.newFromBundle(intent.getExtras());
            String operatorNumeric = serviceStateNewFromBundle != null ? serviceStateNewFromBundle.getOperatorNumeric() : null;
            String stringExtra = intent.getStringExtra("mccMnc");
            log("received action ACTION_SERVICE_STATE_CHANGED, mccMnc=" + operatorNumeric + " testStr=" + stringExtra);
            if (TextUtils.isEmpty(operatorNumeric)) {
                operatorNumeric = stringExtra;
            }
            this.mMccMnc = operatorNumeric;
            if (!TextUtils.isEmpty(this.mMccMnc)) {
                log("Network MCC/MNC is available: " + this.mMccMnc);
                maybeRebindNetworkProvider();
            } else {
                log("Network MCC/MNC is still not available");
            }
        }
    }

    private void testModeConditionChanged() {
        maybeRebindNetworkProvider();
    }

    private String getNetworkProviderPackage() {
        try {
            return ILocationManager.Stub.asInterface(ServiceManager.getService("location")).getNetworkProviderPackage();
        } catch (RemoteException e) {
            return null;
        }
    }

    private void maybeRebindNetworkProvider() {
        synchronized (this.mLock) {
            if (getNetworkProviderCount() >= 2) {
                String networkProviderPackage = getNetworkProviderPackage();
                if (networkProviderPackage != null) {
                    log("current NLP package name: " + networkProviderPackage);
                } else {
                    log("currently there is no NLP binded.");
                }
                boolean zArrayContainsStr = arrayContainsStr(this.mGmsLpPkgs, networkProviderPackage);
                boolean zEquals = SystemProperties.get("persist.mtk_nlp_switch_support", "1").equals("1");
                if (!zArrayContainsStr) {
                    this.mVendorNlpPackageName = networkProviderPackage;
                }
                if (!zEquals) {
                    log("current in nlp switch disable mode");
                    resetPermissions();
                    if (!zArrayContainsStr || networkProviderPackage == null) {
                        reBindNetworkProvider(true);
                    }
                } else if ((this.mUsbPlugged != 0 && this.mWifiConnected != 0 && this.mStayAwake != 0) || this.mInTestMode) {
                    log("current in test mode, mInTestMode=" + this.mInTestMode + " mVendorNlpPackageName=" + this.mVendorNlpPackageName);
                    this.mInTestMode = true;
                    resetPermissions();
                    if (!zArrayContainsStr || networkProviderPackage == null) {
                        reBindNetworkProvider(true);
                    }
                } else if (this.mMccMnc != null && this.mMccMnc.startsWith("460")) {
                    if (zArrayContainsStr || networkProviderPackage == null) {
                        reBindNetworkProvider(false);
                    }
                } else if (!zArrayContainsStr || networkProviderPackage == null) {
                    reBindNetworkProvider(true);
                }
            }
        }
    }

    private void resetPermissions() {
        if (this.mVendorNlpPackageName == null) {
            if (this.mVendorLpPkgs != null) {
                for (int i = 0; i < this.mVendorLpPkgs.length; i++) {
                    resetPermissionGrant(this.mVendorLpPkgs[i]);
                }
                return;
            }
            return;
        }
        resetPermissionGrant(this.mVendorNlpPackageName);
    }

    private void resetPermissionGrant(String str) {
        if (str != null) {
            try {
                log("revokeRuntimePermission package: " + str);
                this.mPackageManager.revokeRuntimePermission(str, "android.permission.READ_PHONE_STATE", new UserHandle(0));
                this.mPackageManager.revokeRuntimePermission(str, "android.permission.ACCESS_COARSE_LOCATION", new UserHandle(0));
                this.mPackageManager.revokeRuntimePermission(str, "android.permission.ACCESS_FINE_LOCATION", new UserHandle(0));
                this.mPackageManager.revokeRuntimePermission(str, "android.permission.WRITE_EXTERNAL_STORAGE", new UserHandle(0));
                this.mPackageManager.revokeRuntimePermission(str, "android.permission.READ_EXTERNAL_STORAGE", new UserHandle(0));
            } catch (IllegalArgumentException e) {
                log("RevokeRuntimePermission IllegalArgumentException: " + str);
            }
        }
    }

    private boolean arrayContainsStr(String[] strArr, String str) {
        if (strArr != null) {
            for (String str2 : strArr) {
                if (str2 != null && str2.equals(str)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void reBindNetworkProvider(boolean z) {
        log("reBindNetworkProvider bindGmsPackage: " + z);
        Intent intent = new Intent("com.mediatek.lbs.action.NLP_BIND_REQUEST");
        intent.putExtra("bindGmsPackage", z);
        this.mContext.sendBroadcast(intent);
    }

    public static void log(String str) {
        Log.d("LbsUtils", str);
    }
}
