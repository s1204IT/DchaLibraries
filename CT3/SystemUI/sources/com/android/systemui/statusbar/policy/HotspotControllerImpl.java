package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.UserManager;
import android.util.Log;
import com.android.systemui.statusbar.policy.HotspotController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

public class HotspotControllerImpl implements HotspotController {
    private static final boolean DEBUG = Log.isLoggable("HotspotController", 3);
    private final ConnectivityManager mConnectivityManager;
    private final Context mContext;
    private int mHotspotState;
    private final ArrayList<HotspotController.Callback> mCallbacks = new ArrayList<>();
    private final Receiver mReceiver = new Receiver(this, null);

    public HotspotControllerImpl(Context context) {
        this.mContext = context;
        this.mConnectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
    }

    @Override
    public boolean isHotspotSupported() {
        if (!this.mConnectivityManager.isTetheringSupported() || this.mConnectivityManager.getTetherableWifiRegexs().length == 0) {
            return false;
        }
        return UserManager.get(this.mContext).isUserAdmin(ActivityManager.getCurrentUser());
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("HotspotController state:");
        pw.print("  mHotspotEnabled=");
        pw.println(stateToString(this.mHotspotState));
    }

    private static String stateToString(int hotspotState) {
        switch (hotspotState) {
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

    @Override
    public void addCallback(HotspotController.Callback callback) {
        synchronized (this.mCallbacks) {
            if (callback != null) {
                if (!this.mCallbacks.contains(callback)) {
                    if (DEBUG) {
                        Log.d("HotspotController", "addCallback " + callback);
                    }
                    this.mCallbacks.add(callback);
                    this.mReceiver.setListening(!this.mCallbacks.isEmpty());
                }
            }
        }
    }

    @Override
    public void removeCallback(HotspotController.Callback callback) {
        if (callback == null) {
            return;
        }
        if (DEBUG) {
            Log.d("HotspotController", "removeCallback " + callback);
        }
        synchronized (this.mCallbacks) {
            this.mCallbacks.remove(callback);
            this.mReceiver.setListening(!this.mCallbacks.isEmpty());
        }
    }

    @Override
    public boolean isHotspotEnabled() {
        return this.mHotspotState == 13;
    }

    static final class OnStartTetheringCallback extends ConnectivityManager.OnStartTetheringCallback {
        OnStartTetheringCallback() {
        }

        public void onTetheringStarted() {
        }

        public void onTetheringFailed() {
        }
    }

    @Override
    public void setHotspotEnabled(boolean enabled) {
        if (enabled) {
            OnStartTetheringCallback callback = new OnStartTetheringCallback();
            this.mConnectivityManager.startTethering(0, false, callback);
        } else {
            this.mConnectivityManager.stopTethering(0);
        }
    }

    public void fireCallback(boolean isEnabled) {
        synchronized (this.mCallbacks) {
            for (HotspotController.Callback callback : this.mCallbacks) {
                callback.onHotspotChanged(isEnabled);
            }
        }
    }

    private final class Receiver extends BroadcastReceiver {
        private boolean mRegistered;

        Receiver(HotspotControllerImpl this$0, Receiver receiver) {
            this();
        }

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
            if (listening || !this.mRegistered) {
                return;
            }
            if (HotspotControllerImpl.DEBUG) {
                Log.d("HotspotController", "Unregistering receiver");
            }
            HotspotControllerImpl.this.mContext.unregisterReceiver(this);
            this.mRegistered = false;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (HotspotControllerImpl.DEBUG) {
                Log.d("HotspotController", "onReceive " + intent.getAction());
            }
            int state = intent.getIntExtra("wifi_state", 14);
            HotspotControllerImpl.this.mHotspotState = state;
            HotspotControllerImpl.this.fireCallback(HotspotControllerImpl.this.mHotspotState == 13);
        }
    }
}
