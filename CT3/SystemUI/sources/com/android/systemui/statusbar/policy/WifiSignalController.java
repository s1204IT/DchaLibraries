package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import com.android.internal.util.AsyncChannel;
import com.android.settingslib.wifi.WifiStatusTracker;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.SignalController;
import com.mediatek.systemui.PluginManager;
import com.mediatek.systemui.ext.IMobileIconExt;
import com.mediatek.systemui.statusbar.util.FeatureOptions;
import java.util.BitSet;
import java.util.Objects;

public class WifiSignalController extends SignalController<WifiState, SignalController.IconGroup> {
    private final boolean mHasMobileData;
    private IMobileIconExt mMobileIconExt;
    private final AsyncChannel mWifiChannel;
    private final WifiManager mWifiManager;
    private final WifiStatusTracker mWifiTracker;

    public WifiSignalController(Context context, boolean hasMobileData, CallbackHandler callbackHandler, NetworkControllerImpl networkController) {
        super("WifiSignalController", context, 1, callbackHandler, networkController);
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mWifiTracker = new WifiStatusTracker(this.mWifiManager);
        this.mHasMobileData = hasMobileData;
        Handler handler = new WifiHandler(this, null);
        this.mWifiChannel = new AsyncChannel();
        Messenger wifiMessenger = this.mWifiManager.getWifiServiceMessenger();
        if (wifiMessenger != null) {
            this.mWifiChannel.connect(context, handler, wifiMessenger);
        }
        WifiState wifiState = (WifiState) this.mCurrentState;
        SignalController.IconGroup iconGroup = new SignalController.IconGroup("Wi-Fi Icons", WifiIcons.WIFI_SIGNAL_STRENGTH, WifiIcons.QS_WIFI_SIGNAL_STRENGTH, AccessibilityContentDescriptions.WIFI_CONNECTION_STRENGTH, R.drawable.stat_sys_wifi_signal_null, R.drawable.ic_qs_wifi_no_network, R.drawable.stat_sys_wifi_signal_null, R.drawable.ic_qs_wifi_no_network, R.string.accessibility_no_wifi);
        ((WifiState) this.mLastState).iconGroup = iconGroup;
        wifiState.iconGroup = iconGroup;
        this.mMobileIconExt = PluginManager.getMobileIconExt(context);
    }

    @Override
    public WifiState cleanState() {
        return new WifiState();
    }

    @Override
    public void notifyListeners(NetworkController.SignalCallback callback) {
        boolean wifiVisible = true;
        if (!((WifiState) this.mCurrentState).enabled) {
            wifiVisible = false;
        } else if (!((WifiState) this.mCurrentState).connected && this.mHasMobileData) {
            wifiVisible = false;
        }
        String str = wifiVisible ? ((WifiState) this.mCurrentState).ssid : null;
        boolean ssidPresent = wifiVisible && ((WifiState) this.mCurrentState).ssid != null;
        String contentDescription = getStringIfExists(getContentDescription());
        if (((WifiState) this.mCurrentState).inetCondition == 0) {
            contentDescription = contentDescription + "," + this.mContext.getString(R.string.accessibility_quick_settings_no_internet);
        }
        NetworkController.IconState statusIcon = new NetworkController.IconState(wifiVisible, getCurrentIconId(), contentDescription);
        NetworkController.IconState qsIcon = new NetworkController.IconState(((WifiState) this.mCurrentState).connected, getQsCurrentIconId(), contentDescription);
        callback.setWifiIndicators(((WifiState) this.mCurrentState).enabled, statusIcon, qsIcon, ssidPresent ? ((WifiState) this.mCurrentState).activityIn : false, ssidPresent ? ((WifiState) this.mCurrentState).activityOut : false, str);
    }

    public void handleBroadcast(Intent intent) {
        this.mWifiTracker.handleBroadcast(intent);
        ((WifiState) this.mCurrentState).enabled = this.mWifiTracker.enabled;
        ((WifiState) this.mCurrentState).connected = this.mWifiTracker.connected;
        ((WifiState) this.mCurrentState).ssid = this.mWifiTracker.ssid;
        ((WifiState) this.mCurrentState).rssi = this.mWifiTracker.rssi;
        ((WifiState) this.mCurrentState).level = this.mWifiTracker.level;
        notifyListenersIfNecessary();
    }

    void setActivity(int wifiActivity) {
        boolean z = true;
        WifiState wifiState = (WifiState) this.mCurrentState;
        boolean z2 = wifiActivity == 3 || wifiActivity == 1;
        wifiState.activityIn = z2;
        WifiState wifiState2 = (WifiState) this.mCurrentState;
        if (wifiActivity != 3 && wifiActivity != 2) {
            z = false;
        }
        wifiState2.activityOut = z;
        notifyListenersIfNecessary();
    }

    private class WifiHandler extends Handler {
        WifiHandler(WifiSignalController this$0, WifiHandler wifiHandler) {
            this();
        }

        private WifiHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    WifiSignalController.this.setActivity(msg.arg1);
                    break;
                case 69632:
                    if (msg.arg1 == 0) {
                        WifiSignalController.this.mWifiChannel.sendMessage(Message.obtain(this, 69633));
                    } else {
                        Log.e(WifiSignalController.this.mTag, "Failed to connect to wifi");
                    }
                    break;
            }
        }
    }

    static class WifiState extends SignalController.State {
        String ssid;

        WifiState() {
        }

        @Override
        public void copyFrom(SignalController.State s) {
            super.copyFrom(s);
            WifiState state = (WifiState) s;
            this.ssid = state.ssid;
        }

        @Override
        protected void toString(StringBuilder builder) {
            super.toString(builder);
            builder.append(',').append("ssid=").append(this.ssid);
        }

        @Override
        public boolean equals(Object o) {
            if (super.equals(o)) {
                return Objects.equals(((WifiState) o).ssid, this.ssid);
            }
            return false;
        }
    }

    @Override
    public void updateConnectivity(BitSet connectedTransports, BitSet validatedTransports) {
        ((WifiState) this.mCurrentState).inetCondition = validatedTransports.get(this.mTransportType) ? 1 : 0;
        Log.d("WifiSignalController", "mCurrentState.inetCondition = " + ((WifiState) this.mCurrentState).inetCondition);
        ((WifiState) this.mCurrentState).inetCondition = this.mMobileIconExt.customizeWifiNetCondition(((WifiState) this.mCurrentState).inetCondition);
        notifyListenersIfNecessary();
    }

    @Override
    public int getCurrentIconId() {
        int type;
        if (FeatureOptions.MTK_A1_SUPPORT) {
            return super.getCurrentIconId();
        }
        int iconId = super.getCurrentIconId();
        if (((WifiState) this.mCurrentState).connected) {
            if ((((WifiState) this.mCurrentState).activityIn || ((WifiState) this.mCurrentState).activityOut) && (type = getActiveType()) < WifiIcons.WIFI_SIGNAL_STRENGTH_INOUT[0].length) {
                return WifiIcons.WIFI_SIGNAL_STRENGTH_INOUT[((WifiState) this.mCurrentState).level][type];
            }
            return iconId;
        }
        return iconId;
    }

    private int getActiveType() {
        if (((WifiState) this.mCurrentState).activityIn && ((WifiState) this.mCurrentState).activityOut) {
            return 3;
        }
        if (((WifiState) this.mCurrentState).activityIn) {
            return 1;
        }
        if (!((WifiState) this.mCurrentState).activityOut) {
            return 0;
        }
        return 2;
    }
}
