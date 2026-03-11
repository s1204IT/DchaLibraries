package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.BenesseExtension;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.WifiTracker;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NetworkController;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AccessPointControllerImpl implements WifiTracker.WifiListener, NetworkController.AccessPointController {
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

        public void onFailure(int i) {
            if (AccessPointControllerImpl.DEBUG) {
                Log.d("AccessPointController", "connect failure reason=" + i);
            }
        }
    };
    private int mCurrentUser = ActivityManager.getCurrentUser();

    public AccessPointControllerImpl(Context context) {
        this.mContext = context;
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mWifiTracker = new WifiTracker(context, this, false, true);
    }

    protected void finalize() throws Throwable {
        super.finalize();
        this.mWifiTracker.onDestroy();
    }

    @Override
    public boolean canConfigWifi() {
        return !this.mUserManager.hasUserRestriction("no_config_wifi", new UserHandle(this.mCurrentUser));
    }

    public void onUserSwitched(int i) {
        this.mCurrentUser = i;
    }

    @Override
    public void addAccessPointCallback(NetworkController.AccessPointController.AccessPointCallback accessPointCallback) {
        if (accessPointCallback == null || this.mCallbacks.contains(accessPointCallback)) {
            return;
        }
        if (DEBUG) {
            Log.d("AccessPointController", "addCallback " + accessPointCallback);
        }
        this.mCallbacks.add(accessPointCallback);
        if (this.mCallbacks.size() == 1) {
            this.mWifiTracker.onStart();
        }
    }

    @Override
    public void removeAccessPointCallback(NetworkController.AccessPointController.AccessPointCallback accessPointCallback) {
        if (accessPointCallback == null) {
            return;
        }
        if (DEBUG) {
            Log.d("AccessPointController", "removeCallback " + accessPointCallback);
        }
        this.mCallbacks.remove(accessPointCallback);
        if (this.mCallbacks.isEmpty()) {
            this.mWifiTracker.onStop();
        }
    }

    @Override
    public void scanForAccessPoints() {
        fireAcccessPointsCallback(this.mWifiTracker.getAccessPoints());
    }

    @Override
    public int getIcon(AccessPoint accessPoint) {
        int level = accessPoint.getLevel();
        int[] iArr = ICONS;
        if (level < 0) {
            level = 0;
        }
        return iArr[level];
    }

    @Override
    public boolean connect(AccessPoint accessPoint) {
        if (accessPoint == null) {
            return false;
        }
        if (DEBUG) {
            Log.d("AccessPointController", "connect networkId=" + accessPoint.getConfig().networkId);
        }
        if (accessPoint.isSaved()) {
            this.mWifiTracker.getManager().connect(accessPoint.getConfig().networkId, this.mConnectListener);
        } else {
            if (accessPoint.getSecurity() != 0) {
                if (BenesseExtension.getDchaState() != 0) {
                    return true;
                }
                Intent intent = new Intent("android.settings.WIFI_SETTINGS");
                intent.putExtra("wifi_start_connect_ssid", accessPoint.getSsidStr());
                intent.addFlags(268435456);
                fireSettingsIntentCallback(intent);
                return true;
            }
            accessPoint.generateOpenNetworkConfig();
            this.mWifiTracker.getManager().connect(accessPoint.getConfig(), this.mConnectListener);
        }
        return false;
    }

    private void fireSettingsIntentCallback(Intent intent) {
        Iterator<NetworkController.AccessPointController.AccessPointCallback> it = this.mCallbacks.iterator();
        while (it.hasNext()) {
            it.next().onSettingsActivityTriggered(intent);
        }
    }

    private void fireAcccessPointsCallback(List<AccessPoint> list) {
        Iterator<NetworkController.AccessPointController.AccessPointCallback> it = this.mCallbacks.iterator();
        while (it.hasNext()) {
            it.next().onAccessPointsChanged(list);
        }
    }

    public void dump(PrintWriter printWriter) {
        this.mWifiTracker.dump(printWriter);
    }

    @Override
    public void onWifiStateChanged(int i) {
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
