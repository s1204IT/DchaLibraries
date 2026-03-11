package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.BenesseExtension;
import android.os.Looper;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.WifiTracker;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NetworkController;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class AccessPointControllerImpl implements NetworkController.AccessPointController, WifiTracker.WifiListener {
    private static final boolean DEBUG = Log.isLoggable("AccessPointController", 3);
    private static final int[] ICONS = {R.drawable.ic_qs_wifi_full_0, R.drawable.ic_qs_wifi_full_1, R.drawable.ic_qs_wifi_full_2, R.drawable.ic_qs_wifi_full_3, R.drawable.ic_qs_wifi_full_4};
    private final Context mContext;
    private final UserManager mUserManager;
    private final WifiTracker mWifiTracker;
    private final ArrayList<NetworkController.AccessPointController.AccessPointCallback> mCallbacks = new ArrayList<>();
    private final WifiManager.ActionListener mConnectListener = new WifiManager.ActionListener() {
        public void onSuccess() {
            if (AccessPointControllerImpl.DEBUG) {
                Log.d("AccessPointController", "connect success");
            }
        }

        public void onFailure(int reason) {
            if (AccessPointControllerImpl.DEBUG) {
                Log.d("AccessPointController", "connect failure reason=" + reason);
            }
        }
    };
    private int mCurrentUser = ActivityManager.getCurrentUser();

    public AccessPointControllerImpl(Context context, Looper bgLooper) {
        this.mContext = context;
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mWifiTracker = new WifiTracker(context, this, bgLooper, false, true);
    }

    @Override
    public boolean canConfigWifi() {
        return !this.mUserManager.hasUserRestriction("no_config_wifi", new UserHandle(this.mCurrentUser));
    }

    @Override
    public void addAccessPointCallback(NetworkController.AccessPointController.AccessPointCallback callback) {
        if (callback == null || this.mCallbacks.contains(callback)) {
            return;
        }
        if (DEBUG) {
            Log.d("AccessPointController", "addCallback " + callback);
        }
        this.mCallbacks.add(callback);
        if (this.mCallbacks.size() != 1) {
            return;
        }
        this.mWifiTracker.startTracking();
    }

    @Override
    public void removeAccessPointCallback(NetworkController.AccessPointController.AccessPointCallback callback) {
        if (callback == null) {
            return;
        }
        if (DEBUG) {
            Log.d("AccessPointController", "removeCallback " + callback);
        }
        this.mCallbacks.remove(callback);
        if (!this.mCallbacks.isEmpty()) {
            return;
        }
        this.mWifiTracker.stopTracking();
    }

    @Override
    public void scanForAccessPoints() {
        if (DEBUG) {
            Log.d("AccessPointController", "scan!");
        }
        this.mWifiTracker.forceScan();
    }

    @Override
    public int getIcon(AccessPoint ap) {
        int level = ap.getLevel();
        int[] iArr = ICONS;
        if (level < 0) {
            level = 0;
        }
        return iArr[level];
    }

    @Override
    public boolean connect(AccessPoint ap) {
        if (ap == null) {
            return false;
        }
        if (ap.isSaved()) {
            if (DEBUG) {
                Log.d("AccessPointController", "connect networkId=" + ap.getConfig().networkId);
            }
            this.mWifiTracker.getManager().connect(ap.getConfig().networkId, this.mConnectListener);
        } else {
            if (ap.getSecurity() != 0 && BenesseExtension.getDchaState() == 0) {
                Intent intent = new Intent("android.settings.WIFI_SETTINGS");
                intent.putExtra("wifi_start_connect_ssid", ap.getSsidStr());
                intent.addFlags(268435456);
                fireSettingsIntentCallback(intent);
                return true;
            }
            ap.generateOpenNetworkConfig();
            this.mWifiTracker.getManager().connect(ap.getConfig(), this.mConnectListener);
        }
        return false;
    }

    private void fireSettingsIntentCallback(Intent intent) {
        for (NetworkController.AccessPointController.AccessPointCallback callback : this.mCallbacks) {
            callback.onSettingsActivityTriggered(intent);
        }
    }

    private void fireAcccessPointsCallback(List<AccessPoint> aps) {
        for (NetworkController.AccessPointController.AccessPointCallback callback : this.mCallbacks) {
            callback.onAccessPointsChanged(aps);
        }
    }

    public void dump(PrintWriter pw) {
        this.mWifiTracker.dump(pw);
    }

    @Override
    public void onWifiStateChanged(int state) {
    }

    @Override
    public void onConnectedChanged() {
        fireAcccessPointsCallback(this.mWifiTracker.getAccessPoints());
    }

    @Override
    public void onAccessPointsChanged() {
        fireAcccessPointsCallback(this.mWifiTracker.getAccessPoints());
    }
}
