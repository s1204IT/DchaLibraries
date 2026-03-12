package com.android.systemui.statusbar.policy;

import android.R;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import com.android.systemui.statusbar.policy.HotspotController;
import java.util.ArrayList;

public class HotspotControllerImpl implements HotspotController {
    private static final boolean DEBUG = Log.isLoggable("HotspotController", 3);
    private final ConnectivityManager mConnectivityManager;
    private final Context mContext;
    private final WifiManager mWifiManager;
    private final ArrayList<HotspotController.Callback> mCallbacks = new ArrayList<>();
    private final Receiver mReceiver = new Receiver();

    public HotspotControllerImpl(Context context) {
        this.mContext = context;
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
    }

    @Override
    public void addCallback(HotspotController.Callback callback) {
        if (callback != null && !this.mCallbacks.contains(callback)) {
            if (DEBUG) {
                Log.d("HotspotController", "addCallback " + callback);
            }
            this.mCallbacks.add(callback);
            this.mReceiver.setListening(!this.mCallbacks.isEmpty());
        }
    }

    @Override
    public void removeCallback(HotspotController.Callback callback) {
        if (callback != null) {
            if (DEBUG) {
                Log.d("HotspotController", "removeCallback " + callback);
            }
            this.mCallbacks.remove(callback);
            this.mReceiver.setListening(!this.mCallbacks.isEmpty());
        }
    }

    @Override
    public boolean isHotspotEnabled() {
        return this.mWifiManager.getWifiApState() == 13;
    }

    @Override
    public boolean isHotspotSupported() {
        boolean isSecondaryUser = ActivityManager.getCurrentUser() != 0;
        return !isSecondaryUser && this.mConnectivityManager.isTetheringSupported();
    }

    public boolean isProvisioningNeeded() {
        String[] provisionApp = this.mContext.getResources().getStringArray(R.array.config_autoBrightnessLcdBacklightValues_doze);
        return (SystemProperties.getBoolean("net.tethering.noprovisioning", false) || provisionApp == null || provisionApp.length != 2) ? false : true;
    }

    @Override
    public void setHotspotEnabled(boolean enabled) {
        ContentResolver cr = this.mContext.getContentResolver();
        if (enabled) {
            if (isProvisioningNeeded()) {
                String tetherEnable = this.mContext.getResources().getString(R.string.config_helpPackageNameKey);
                Intent intent = new Intent();
                intent.putExtra("extraAddTetherType", 0);
                intent.putExtra("extraSetAlarm", true);
                intent.putExtra("extraRunProvision", true);
                intent.putExtra("extraEnableWifiTether", true);
                intent.setComponent(ComponentName.unflattenFromString(tetherEnable));
                this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
                return;
            }
            int wifiState = this.mWifiManager.getWifiState();
            if (wifiState == 2 || wifiState == 3) {
                this.mWifiManager.setWifiEnabled(false);
                Settings.Global.putInt(cr, "wifi_saved_state", 1);
            }
            this.mWifiManager.setWifiApEnabled(null, true);
            return;
        }
        this.mWifiManager.setWifiApEnabled(null, false);
        if (Settings.Global.getInt(cr, "wifi_saved_state", 0) == 1) {
            this.mWifiManager.setWifiEnabled(true);
            Settings.Global.putInt(cr, "wifi_saved_state", 0);
        }
    }

    public void fireCallback(boolean isEnabled) {
        for (HotspotController.Callback callback : this.mCallbacks) {
            callback.onHotspotChanged(isEnabled);
        }
    }

    private final class Receiver extends BroadcastReceiver {
        private boolean mRegistered;

        private Receiver() {
        }

        public void setListening(boolean listening) {
            if (listening && !this.mRegistered) {
                if (HotspotControllerImpl.DEBUG) {
                    Log.d("HotspotController", "Registering receiver");
                }
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
                HotspotControllerImpl.this.mContext.registerReceiver(this, filter);
                this.mRegistered = true;
                return;
            }
            if (!listening && this.mRegistered) {
                if (HotspotControllerImpl.DEBUG) {
                    Log.d("HotspotController", "Unregistering receiver");
                }
                HotspotControllerImpl.this.mContext.unregisterReceiver(this);
                this.mRegistered = false;
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (HotspotControllerImpl.DEBUG) {
                Log.d("HotspotController", "onReceive " + intent.getAction());
            }
            HotspotControllerImpl.this.fireCallback(HotspotControllerImpl.this.isHotspotEnabled());
        }
    }
}
