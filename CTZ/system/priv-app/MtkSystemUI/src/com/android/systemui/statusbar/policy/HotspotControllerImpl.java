package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.UserManager;
import android.util.Log;
import com.android.systemui.Dependency;
import com.android.systemui.statusbar.policy.HotspotController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

/* loaded from: classes.dex */
public class HotspotControllerImpl implements WifiManager.SoftApCallback, HotspotController {
    private static final boolean DEBUG = Log.isLoggable("HotspotController", 3);
    private final ConnectivityManager mConnectivityManager;
    private final Context mContext;
    private int mHotspotState;
    private int mNumConnectedDevices;
    private boolean mWaitingForCallback;
    private final WifiManager mWifiManager;
    private final ArrayList<HotspotController.Callback> mCallbacks = new ArrayList<>();
    private final WifiStateReceiver mWifiStateReceiver = new WifiStateReceiver();

    public HotspotControllerImpl(Context context) {
        this.mContext = context;
        this.mConnectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
    }

    @Override // com.android.systemui.statusbar.policy.HotspotController
    public boolean isHotspotSupported() {
        return this.mConnectivityManager.isTetheringSupported() && this.mConnectivityManager.getTetherableWifiRegexs().length != 0 && UserManager.get(this.mContext).isUserAdmin(ActivityManager.getCurrentUser());
    }

    @Override // com.android.systemui.Dumpable
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("HotspotController state:");
        printWriter.print("  mHotspotEnabled=");
        printWriter.println(stateToString(this.mHotspotState));
    }

    private static String stateToString(int i) {
        switch (i) {
            case 10:
                return "DISABLING";
            case 11:
                return "DISABLED";
            case 12:
                return "ENABLING";
            case 13:
                return "ENABLED";
            case 14:
                return "FAILED";
            default:
                return null;
        }
    }

    /* JADX DEBUG: Method merged with bridge method: addCallback(Ljava/lang/Object;)V */
    @Override // com.android.systemui.statusbar.policy.CallbackController
    public void addCallback(HotspotController.Callback callback) {
        synchronized (this.mCallbacks) {
            if (callback != null) {
                try {
                    if (!this.mCallbacks.contains(callback)) {
                        if (DEBUG) {
                            Log.d("HotspotController", "addCallback " + callback);
                        }
                        this.mCallbacks.add(callback);
                        if (this.mCallbacks.size() == 1) {
                            updateWifiStateListeners(true);
                        }
                    }
                } finally {
                }
            }
        }
    }

    /* JADX DEBUG: Method merged with bridge method: removeCallback(Ljava/lang/Object;)V */
    @Override // com.android.systemui.statusbar.policy.CallbackController
    public void removeCallback(HotspotController.Callback callback) {
        if (callback == null) {
            return;
        }
        if (DEBUG) {
            Log.d("HotspotController", "removeCallback " + callback);
        }
        synchronized (this.mCallbacks) {
            this.mCallbacks.remove(callback);
            if (this.mCallbacks.size() == 0) {
                updateWifiStateListeners(false);
            }
        }
    }

    private void updateWifiStateListeners(boolean z) {
        if (this.mWifiManager == null) {
            return;
        }
        this.mWifiStateReceiver.setListening(z);
        if (z) {
            this.mWifiManager.registerSoftApCallback(this, (Handler) Dependency.get(Dependency.MAIN_HANDLER));
        } else {
            this.mWifiManager.unregisterSoftApCallback(this);
        }
    }

    @Override // com.android.systemui.statusbar.policy.HotspotController
    public boolean isHotspotEnabled() {
        return this.mHotspotState == 13;
    }

    @Override // com.android.systemui.statusbar.policy.HotspotController
    public boolean isHotspotTransient() {
        return this.mWaitingForCallback || this.mHotspotState == 12;
    }

    @Override // com.android.systemui.statusbar.policy.HotspotController
    public void setHotspotEnabled(boolean z) {
        if (!z) {
            this.mConnectivityManager.stopTethering(0);
            return;
        }
        OnStartTetheringCallback onStartTetheringCallback = new OnStartTetheringCallback();
        this.mWaitingForCallback = true;
        if (DEBUG) {
            Log.d("HotspotController", "Starting tethering");
        }
        this.mConnectivityManager.startTethering(0, false, onStartTetheringCallback);
    }

    @Override // com.android.systemui.statusbar.policy.HotspotController
    public int getNumConnectedDevices() {
        return this.mNumConnectedDevices;
    }

    private void fireHotspotChangedCallback(boolean z) {
        fireHotspotChangedCallback(z, this.mNumConnectedDevices);
    }

    private void fireHotspotChangedCallback(boolean z, int i) {
        synchronized (this.mCallbacks) {
            Iterator<HotspotController.Callback> it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                it.next().onHotspotChanged(z, i);
            }
        }
    }

    public void onStateChanged(int i, int i2) {
    }

    public void onNumClientsChanged(int i) {
        this.mNumConnectedDevices = i;
        fireHotspotChangedCallback(isHotspotEnabled(), i);
    }

    private final class OnStartTetheringCallback extends ConnectivityManager.OnStartTetheringCallback {
        private OnStartTetheringCallback() {
        }

        public void onTetheringStarted() {
            if (HotspotControllerImpl.DEBUG) {
                Log.d("HotspotController", "onTetheringStarted");
            }
            HotspotControllerImpl.this.mWaitingForCallback = false;
        }

        public void onTetheringFailed() {
            if (HotspotControllerImpl.DEBUG) {
                Log.d("HotspotController", "onTetheringFailed");
            }
            HotspotControllerImpl.this.mWaitingForCallback = false;
            HotspotControllerImpl.this.fireHotspotChangedCallback(HotspotControllerImpl.this.isHotspotEnabled());
        }
    }

    private final class WifiStateReceiver extends BroadcastReceiver {
        private boolean mRegistered;

        private WifiStateReceiver() {
        }

        public void setListening(boolean z) {
            if (z && !this.mRegistered) {
                if (HotspotControllerImpl.DEBUG) {
                    Log.d("HotspotController", "Registering receiver");
                }
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
                HotspotControllerImpl.this.mContext.registerReceiver(this, intentFilter);
                this.mRegistered = true;
                return;
            }
            if (!z && this.mRegistered) {
                if (HotspotControllerImpl.DEBUG) {
                    Log.d("HotspotController", "Unregistering receiver");
                }
                HotspotControllerImpl.this.mContext.unregisterReceiver(this);
                this.mRegistered = false;
            }
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            int intExtra = intent.getIntExtra("wifi_state", 14);
            if (HotspotControllerImpl.DEBUG) {
                Log.d("HotspotController", "onReceive " + intExtra);
            }
            HotspotControllerImpl.this.mHotspotState = intExtra;
            if (!HotspotControllerImpl.this.isHotspotEnabled()) {
                HotspotControllerImpl.this.mNumConnectedDevices = 0;
            }
            HotspotControllerImpl.this.fireHotspotChangedCallback(HotspotControllerImpl.this.isHotspotEnabled());
        }
    }
}
