package com.mediatek.systemui.qs.tiles.ext;

import android.content.Intent;
import android.telephony.SubscriptionManager;
import android.util.Log;
import com.android.settingslib.net.DataUsageController;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.SignalCallbackAdapter;
import com.mediatek.systemui.PluginManager;
import com.mediatek.systemui.statusbar.extcb.IconIdWrapper;
import com.mediatek.systemui.statusbar.util.SIMHelper;

public class MobileDataTile extends QSTile<QSTile.SignalState> {
    private static final int AIRPLANE_DATA_CONNECT = 2;
    private static final int DATA_CONNECT = 1;
    private static final int DATA_CONNECT_DISABLE = 3;
    private static final int DATA_DISCONNECT = 0;
    private static final int DATA_RADIO_OFF = 4;
    private static final boolean DEBUG = true;
    private static final int QS_MOBILE_DISABLE = 2130837723;
    private static final int QS_MOBILE_ENABLE = 2130837724;
    private final MobileDataSignalCallback mCallback;
    private final NetworkController mController;
    private int mDataConnectionState;
    private final DataUsageController mDataController;
    private int mDataStateIconId;
    private final IconIdWrapper mDisableStateIconIdWrapper;
    private final IconIdWrapper mEnableStateIconIdWrapper;
    private boolean mEnabled;
    private CharSequence mTileLabel;

    public MobileDataTile(QSTile.Host host) {
        super(host);
        this.mDataConnectionState = DATA_DISCONNECT;
        this.mDataStateIconId = R.drawable.ic_qs_mobile_off;
        this.mEnableStateIconIdWrapper = new IconIdWrapper();
        this.mDisableStateIconIdWrapper = new IconIdWrapper();
        this.mCallback = new MobileDataSignalCallback(this, null);
        this.mController = host.getNetworkController();
        this.mDataController = this.mController.getMobileDataController();
        Log.d(this.TAG, "create MobileDataTile");
    }

    @Override
    public void setListening(boolean listening) {
        Log.d(this.TAG, "setListening = " + listening);
        if (listening) {
            this.mController.addSignalCallback(this.mCallback);
        } else {
            this.mController.removeSignalCallback(this.mCallback);
        }
    }

    @Override
    public CharSequence getTileLabel() {
        this.mTileLabel = PluginManager.getQuickSettingsPlugin(this.mContext).getTileLabel("mobiledata");
        return this.mTileLabel;
    }

    @Override
    public Intent getLongClickIntent() {
        return null;
    }

    @Override
    public QSTile.SignalState newTileState() {
        return new QSTile.SignalState();
    }

    @Override
    public int getMetricsCategory() {
        return 111;
    }

    @Override
    protected void handleLongClick() {
        handleClick();
    }

    @Override
    protected void handleClick() {
        int subId;
        if (!this.mDataController.isMobileDataSupported() || !this.mEnabled) {
            return;
        }
        if (!((QSTile.SignalState) this.mState).connected && ((subId = SubscriptionManager.getDefaultDataSubscriptionId()) < 0 || !SIMHelper.isRadioOn(subId))) {
            return;
        }
        this.mDataController.setMobileDataEnabled(((QSTile.SignalState) this.mState).connected ? DATA_DISCONNECT : DEBUG);
    }

    @Override
    public void handleUpdateState(QSTile.SignalState state, Object arg) {
        Log.d(this.TAG, "handleUpdateState arg=" + arg);
        CallbackInfo cb = (CallbackInfo) arg;
        if (cb == null) {
            cb = this.mCallback.mInfo;
        }
        boolean enabled = (!this.mDataController.isMobileDataSupported() || cb.noSim || cb.airplaneModeEnabled) ? DATA_DISCONNECT : isDefaultDataSimRadioOn();
        boolean dataConnected = (enabled && this.mDataController.isMobileDataEnabled() && cb.mobileSignalIconId > 0) ? DEBUG : false;
        boolean dataNotConnected = (cb.mobileSignalIconId <= 0 || cb.enabledDesc != null) ? false : DEBUG;
        this.mEnabled = enabled;
        state.connected = dataConnected;
        state.activityIn = cb.enabled ? cb.activityIn : DATA_DISCONNECT;
        state.activityOut = cb.enabled ? cb.activityOut : DATA_DISCONNECT;
        state.filter = DEBUG;
        this.mEnableStateIconIdWrapper.setResources(this.mContext.getResources());
        this.mDisableStateIconIdWrapper.setResources(this.mContext.getResources());
        if (!enabled) {
            this.mDataConnectionState = DATA_CONNECT_DISABLE;
            this.mDataStateIconId = R.drawable.ic_qs_mobile_off;
            this.mDisableStateIconIdWrapper.setIconId(this.mDataStateIconId);
            state.label = PluginManager.getQuickSettingsPlugin(this.mContext).customizeDataConnectionTile(this.mDataConnectionState, this.mDisableStateIconIdWrapper, this.mContext.getString(R.string.mobile));
            state.icon = QsIconWrapper.get(this.mDisableStateIconIdWrapper.getIconId(), this.mDisableStateIconIdWrapper);
        } else if (dataConnected) {
            this.mDataConnectionState = DATA_CONNECT;
            this.mDataStateIconId = R.drawable.ic_qs_mobile_white;
            this.mEnableStateIconIdWrapper.setIconId(this.mDataStateIconId);
            state.label = PluginManager.getQuickSettingsPlugin(this.mContext).customizeDataConnectionTile(this.mDataConnectionState, this.mEnableStateIconIdWrapper, this.mContext.getString(R.string.mobile));
            state.icon = QsIconWrapper.get(this.mEnableStateIconIdWrapper.getIconId(), this.mEnableStateIconIdWrapper);
        } else if (dataNotConnected) {
            this.mDataConnectionState = DATA_DISCONNECT;
            this.mDataStateIconId = R.drawable.ic_qs_mobile_off;
            this.mDisableStateIconIdWrapper.setIconId(this.mDataStateIconId);
            state.label = PluginManager.getQuickSettingsPlugin(this.mContext).customizeDataConnectionTile(this.mDataConnectionState, this.mDisableStateIconIdWrapper, this.mContext.getString(R.string.mobile));
            state.icon = QsIconWrapper.get(this.mDisableStateIconIdWrapper.getIconId(), this.mDisableStateIconIdWrapper);
        } else {
            this.mDataConnectionState = DATA_DISCONNECT;
            this.mDataStateIconId = R.drawable.ic_qs_mobile_off;
            this.mDisableStateIconIdWrapper.setIconId(this.mDataStateIconId);
            state.label = PluginManager.getQuickSettingsPlugin(this.mContext).customizeDataConnectionTile(this.mDataConnectionState, this.mDisableStateIconIdWrapper, this.mContext.getString(R.string.mobile));
            state.icon = QsIconWrapper.get(this.mDisableStateIconIdWrapper.getIconId(), this.mDisableStateIconIdWrapper);
        }
        this.mTileLabel = state.label;
        Log.d(this.TAG, "handleUpdateState state=" + state);
    }

    private final boolean isDefaultDataSimRadioOn() {
        int subId = SubscriptionManager.getDefaultDataSubscriptionId();
        boolean zIsRadioOn = subId >= 0 ? SIMHelper.isRadioOn(subId) : false;
        Log.d(this.TAG, "isDefaultDataSimRadioOn subId=" + subId + ", isRadioOn=" + zIsRadioOn);
        return zIsRadioOn;
    }

    private static final class CallbackInfo {
        public boolean activityIn;
        public boolean activityOut;
        public boolean airplaneModeEnabled;
        public int dataTypeIconId;
        public boolean enabled;
        public String enabledDesc;
        public int mobileSignalIconId;
        public boolean noSim;
        public boolean wifiConnected;
        public boolean wifiEnabled;

        CallbackInfo(CallbackInfo callbackInfo) {
            this();
        }

        private CallbackInfo() {
        }

        public String toString() {
            return "CallbackInfo[enabled=" + this.enabled + ",wifiEnabled=" + this.wifiEnabled + ",wifiConnected=" + this.wifiConnected + ",airplaneModeEnabled=" + this.airplaneModeEnabled + ",mobileSignalIconId=" + this.mobileSignalIconId + ",dataTypeIconId=" + this.dataTypeIconId + ",activityIn=" + this.activityIn + ",activityOut=" + this.activityOut + ",enabledDesc=" + this.enabledDesc + ",noSim=" + this.noSim + ']';
        }
    }

    private final class MobileDataSignalCallback extends SignalCallbackAdapter {
        final CallbackInfo mInfo;

        MobileDataSignalCallback(MobileDataTile this$0, MobileDataSignalCallback mobileDataSignalCallback) {
            this();
        }

        private MobileDataSignalCallback() {
            this.mInfo = new CallbackInfo(null);
        }

        @Override
        public void setWifiIndicators(boolean enabled, NetworkController.IconState statusIcon, NetworkController.IconState qsIcon, boolean activityIn, boolean activityOut, String description) {
            this.mInfo.wifiEnabled = enabled;
            this.mInfo.wifiConnected = qsIcon.visible;
            MobileDataTile.this.refreshState(this.mInfo);
        }

        @Override
        public void setMobileDataIndicators(NetworkController.IconState statusIcon, NetworkController.IconState qsIcon, int statusType, int networkIcon, int volteType, int qsType, boolean activityIn, boolean activityOut, String typeContentDescription, String description, boolean isWide, int subId) {
            if (qsIcon == null) {
                return;
            }
            this.mInfo.enabled = qsIcon.visible;
            this.mInfo.mobileSignalIconId = qsIcon.icon;
            this.mInfo.dataTypeIconId = qsType;
            this.mInfo.activityIn = activityIn;
            this.mInfo.activityOut = activityOut;
            this.mInfo.enabledDesc = description;
            Log.d(MobileDataTile.this.TAG, "setMobileDataIndicators mInfo=" + this.mInfo);
            MobileDataTile.this.refreshState(this.mInfo);
        }

        @Override
        public void setNoSims(boolean show) {
            this.mInfo.noSim = show;
            if (this.mInfo.noSim) {
                this.mInfo.mobileSignalIconId = MobileDataTile.DATA_DISCONNECT;
                this.mInfo.dataTypeIconId = MobileDataTile.DATA_DISCONNECT;
                this.mInfo.enabled = false;
                Log.d(MobileDataTile.this.TAG, "setNoSims noSim=" + show);
            }
            MobileDataTile.this.refreshState(this.mInfo);
        }

        @Override
        public void setIsAirplaneMode(NetworkController.IconState icon) {
            this.mInfo.airplaneModeEnabled = icon.visible;
            if (this.mInfo.airplaneModeEnabled) {
                this.mInfo.mobileSignalIconId = MobileDataTile.DATA_DISCONNECT;
                this.mInfo.dataTypeIconId = MobileDataTile.DATA_DISCONNECT;
                this.mInfo.enabled = false;
            }
            MobileDataTile.this.refreshState(this.mInfo);
        }

        @Override
        public void setMobileDataEnabled(boolean enabled) {
            MobileDataTile.this.refreshState(this.mInfo);
        }
    }
}
