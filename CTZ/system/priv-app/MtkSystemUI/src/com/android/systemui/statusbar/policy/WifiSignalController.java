package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkScoreManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.AsyncChannel;
import com.android.settingslib.wifi.WifiStatusTracker;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.SignalController;
import java.util.BitSet;
import java.util.Objects;

/* loaded from: classes.dex */
public class WifiSignalController extends SignalController<WifiState, SignalController.IconGroup> {
    private final boolean mHasMobileData;
    private final AsyncChannel mWifiChannel;
    private final WifiStatusTracker mWifiTracker;

    public WifiSignalController(Context context, boolean z, CallbackHandler callbackHandler, NetworkControllerImpl networkControllerImpl, WifiManager wifiManager) {
        Messenger wifiServiceMessenger;
        super("WifiSignalController", context, 1, callbackHandler, networkControllerImpl);
        this.mWifiTracker = new WifiStatusTracker(this.mContext, wifiManager, (NetworkScoreManager) context.getSystemService(NetworkScoreManager.class), (ConnectivityManager) context.getSystemService(ConnectivityManager.class), new Runnable() { // from class: com.android.systemui.statusbar.policy.-$$Lambda$WifiSignalController$AffzGdHvQakHA4bIzi_tW1MVLCY
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.handleStatusUpdated();
            }
        });
        this.mWifiTracker.setListening(true);
        this.mHasMobileData = z;
        WifiHandler wifiHandler = new WifiHandler(Looper.getMainLooper());
        this.mWifiChannel = new AsyncChannel();
        if (wifiManager != null && (wifiServiceMessenger = wifiManager.getWifiServiceMessenger()) != null) {
            this.mWifiChannel.connect(context, wifiHandler, wifiServiceMessenger);
        }
        WifiState wifiState = (WifiState) this.mCurrentState;
        WifiState wifiState2 = (WifiState) this.mLastState;
        SignalController.IconGroup iconGroup = new SignalController.IconGroup("Wi-Fi Icons", WifiIcons.WIFI_SIGNAL_STRENGTH, WifiIcons.QS_WIFI_SIGNAL_STRENGTH, AccessibilityContentDescriptions.WIFI_CONNECTION_STRENGTH, R.drawable.stat_sys_wifi_signal_null, R.drawable.ic_qs_wifi_no_network, R.drawable.stat_sys_wifi_signal_null, R.drawable.ic_qs_wifi_no_network, R.string.accessibility_no_wifi);
        wifiState2.iconGroup = iconGroup;
        wifiState.iconGroup = iconGroup;
    }

    /* JADX DEBUG: Method merged with bridge method: cleanState()Lcom/android/systemui/statusbar/policy/SignalController$State; */
    @Override // com.android.systemui.statusbar.policy.SignalController
    protected WifiState cleanState() {
        return new WifiState();
    }

    @Override // com.android.systemui.statusbar.policy.SignalController
    public void notifyListeners(NetworkController.SignalCallback signalCallback) {
        boolean z = ((WifiState) this.mCurrentState).enabled && (((WifiState) this.mCurrentState).connected || !this.mHasMobileData);
        String str = z ? ((WifiState) this.mCurrentState).ssid : null;
        boolean z2 = z && ((WifiState) this.mCurrentState).ssid != null;
        String stringIfExists = getStringIfExists(getContentDescription());
        if (((WifiState) this.mCurrentState).inetCondition == 0) {
            stringIfExists = stringIfExists + "," + this.mContext.getString(R.string.data_connection_no_internet);
        }
        signalCallback.setWifiIndicators(((WifiState) this.mCurrentState).enabled, new NetworkController.IconState(z, getCurrentIconId(), stringIfExists), new NetworkController.IconState(((WifiState) this.mCurrentState).connected, getQsCurrentIconId(), stringIfExists), z2 && ((WifiState) this.mCurrentState).activityIn, z2 && ((WifiState) this.mCurrentState).activityOut, str, ((WifiState) this.mCurrentState).isTransient, ((WifiState) this.mCurrentState).statusLabel);
    }

    public void handleBroadcast(Intent intent) {
        this.mWifiTracker.handleBroadcast(intent);
        ((WifiState) this.mCurrentState).enabled = this.mWifiTracker.enabled;
        ((WifiState) this.mCurrentState).connected = this.mWifiTracker.connected;
        ((WifiState) this.mCurrentState).ssid = this.mWifiTracker.ssid;
        ((WifiState) this.mCurrentState).rssi = this.mWifiTracker.rssi;
        ((WifiState) this.mCurrentState).level = this.mWifiTracker.level;
        ((WifiState) this.mCurrentState).statusLabel = this.mWifiTracker.statusLabel;
        notifyListenersIfNecessary();
    }

    private void handleStatusUpdated() {
        ((WifiState) this.mCurrentState).statusLabel = this.mWifiTracker.statusLabel;
        notifyListenersIfNecessary();
    }

    @VisibleForTesting
    void setActivity(int i) {
        ((WifiState) this.mCurrentState).activityIn = i == 3 || i == 1;
        ((WifiState) this.mCurrentState).activityOut = i == 3 || i == 2;
        notifyListenersIfNecessary();
    }

    private class WifiHandler extends Handler {
        WifiHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            int i = message.what;
            if (i == 1) {
                WifiSignalController.this.setActivity(message.arg1);
            } else if (i == 69632) {
                if (message.arg1 == 0) {
                    WifiSignalController.this.mWifiChannel.sendMessage(Message.obtain(this, 69633));
                } else {
                    Log.e(WifiSignalController.this.mTag, "Failed to connect to wifi");
                }
            }
        }
    }

    static class WifiState extends SignalController.State {
        boolean isTransient;
        String ssid;
        String statusLabel;

        WifiState() {
        }

        @Override // com.android.systemui.statusbar.policy.SignalController.State
        public void copyFrom(SignalController.State state) {
            super.copyFrom(state);
            WifiState wifiState = (WifiState) state;
            this.ssid = wifiState.ssid;
            this.isTransient = wifiState.isTransient;
            this.statusLabel = wifiState.statusLabel;
        }

        @Override // com.android.systemui.statusbar.policy.SignalController.State
        protected void toString(StringBuilder sb) {
            super.toString(sb);
            sb.append(",ssid=");
            sb.append(this.ssid);
            sb.append(",isTransient=");
            sb.append(this.isTransient);
            sb.append(",statusLabel=");
            sb.append(this.statusLabel);
        }

        @Override // com.android.systemui.statusbar.policy.SignalController.State
        public boolean equals(Object obj) {
            if (!super.equals(obj)) {
                return false;
            }
            WifiState wifiState = (WifiState) obj;
            return Objects.equals(wifiState.ssid, this.ssid) && wifiState.isTransient == this.isTransient && TextUtils.equals(wifiState.statusLabel, this.statusLabel);
        }
    }

    @Override // com.android.systemui.statusbar.policy.SignalController
    public void updateConnectivity(BitSet bitSet, BitSet bitSet2) {
        ((WifiState) this.mCurrentState).inetCondition = bitSet2.get(this.mTransportType) ? 1 : 0;
        Log.d("WifiSignalController", "mCurrentState.inetCondition = " + ((WifiState) this.mCurrentState).inetCondition);
        notifyListenersIfNecessary();
    }
}
