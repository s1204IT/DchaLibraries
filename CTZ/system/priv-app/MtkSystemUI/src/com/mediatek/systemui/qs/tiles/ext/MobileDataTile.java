package com.mediatek.systemui.qs.tiles.ext;

import android.content.Intent;
import android.telephony.SubscriptionManager;
import android.util.Log;
import com.android.settingslib.net.DataUsageController;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.NetworkController;
import com.mediatek.systemui.ext.IQuickSettingsPlugin;
import com.mediatek.systemui.ext.OpSystemUICustomizationFactoryBase;
import com.mediatek.systemui.statusbar.extcb.IconIdWrapper;
import com.mediatek.systemui.statusbar.util.SIMHelper;

/* loaded from: classes.dex */
public class MobileDataTile extends QSTileImpl<QSTile.SignalState> {
    private final MobileDataSignalCallback mCallback;
    private boolean mConnected;
    private final NetworkController mController;
    private int mDataConnectionState;
    private final DataUsageController mDataController;
    private int mDataStateIconId;
    private final IconIdWrapper mDisableStateIconIdWrapper;
    private final IconIdWrapper mEnableStateIconIdWrapper;
    private boolean mEnabled;
    private IQuickSettingsPlugin mQuickSettingsExt;
    private CharSequence mTileLabel;

    public MobileDataTile(QSHost qSHost) {
        super(qSHost);
        this.mQuickSettingsExt = null;
        this.mDataConnectionState = 0;
        this.mDataStateIconId = R.drawable.ic_qs_mobile_off;
        this.mEnableStateIconIdWrapper = new IconIdWrapper();
        this.mDisableStateIconIdWrapper = new IconIdWrapper();
        this.mCallback = new MobileDataSignalCallback();
        this.mQuickSettingsExt = OpSystemUICustomizationFactoryBase.getOpFactory(this.mContext).makeQuickSettings(this.mContext);
        this.mController = (NetworkController) Dependency.get(NetworkController.class);
        this.mDataController = this.mController.getMobileDataController();
        Log.d(this.TAG, "create MobileDataTile");
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public void handleSetListening(boolean z) {
        Log.d(this.TAG, "setListening = " + z);
        if (z) {
            this.mController.addCallback((NetworkController.SignalCallback) this.mCallback);
        } else {
            this.mController.removeCallback((NetworkController.SignalCallback) this.mCallback);
        }
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public CharSequence getTileLabel() {
        this.mTileLabel = this.mQuickSettingsExt.getTileLabel("mobiledata");
        return this.mTileLabel;
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public Intent getLongClickIntent() {
        return null;
    }

    /* JADX DEBUG: Method merged with bridge method: newTileState()Lcom/android/systemui/plugins/qs/QSTile$State; */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public QSTile.SignalState newTileState() {
        return new QSTile.SignalState();
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl, com.android.systemui.plugins.qs.QSTile
    public int getMetricsCategory() {
        return com.android.systemui.plugins.R.styleable.AppCompatTheme_windowActionBar;
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleLongClick() {
        handleClick();
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleClick() {
        int defaultDataSubscriptionId;
        if (this.mDataController.isMobileDataSupported() && this.mEnabled) {
            if (!this.mConnected && ((defaultDataSubscriptionId = SubscriptionManager.getDefaultDataSubscriptionId()) < 0 || !SIMHelper.isRadioOn(defaultDataSubscriptionId))) {
                return;
            }
            this.mDataController.setMobileDataEnabled(!this.mConnected);
        }
    }

    /* JADX DEBUG: Method merged with bridge method: handleUpdateState(Lcom/android/systemui/plugins/qs/QSTile$State;Ljava/lang/Object;)V */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleUpdateState(QSTile.SignalState signalState, Object obj) {
        Log.d(this.TAG, "handleUpdateState arg=" + obj);
        CallbackInfo callbackInfo = (CallbackInfo) obj;
        if (callbackInfo == null) {
            callbackInfo = this.mCallback.mInfo;
        }
        boolean z = this.mDataController.isMobileDataSupported() && !callbackInfo.noSim && !callbackInfo.airplaneModeEnabled && isDefaultDataSimRadioOn();
        boolean z2 = z && this.mDataController.isMobileDataEnabled() && callbackInfo.mobileSignalIconId > 0;
        boolean z3 = callbackInfo.mobileSignalIconId > 0 && callbackInfo.enabledDesc == null;
        this.mEnabled = z;
        this.mConnected = z2;
        signalState.activityIn = callbackInfo.enabled && callbackInfo.activityIn;
        signalState.activityOut = callbackInfo.enabled && callbackInfo.activityOut;
        if (!z) {
            this.mDataConnectionState = 3;
            this.mDataStateIconId = R.drawable.ic_qs_mobile_off;
            this.mDisableStateIconIdWrapper.setResources(this.mContext.getResources());
            this.mDisableStateIconIdWrapper.setIconId(this.mDataStateIconId);
            signalState.label = this.mQuickSettingsExt.customizeDataConnectionTile(this.mDataConnectionState, this.mDisableStateIconIdWrapper, this.mContext.getString(R.string.mobile));
            signalState.icon = QsIconWrapper.get(this.mDisableStateIconIdWrapper.getIconId(), this.mDisableStateIconIdWrapper);
        } else if (z2) {
            this.mDataConnectionState = 1;
            this.mDataStateIconId = R.drawable.ic_qs_mobile_white;
            this.mEnableStateIconIdWrapper.setResources(this.mContext.getResources());
            this.mEnableStateIconIdWrapper.setIconId(this.mDataStateIconId);
            signalState.label = this.mQuickSettingsExt.customizeDataConnectionTile(this.mDataConnectionState, this.mEnableStateIconIdWrapper, this.mContext.getString(R.string.mobile));
            signalState.icon = QsIconWrapper.get(this.mEnableStateIconIdWrapper.getIconId(), this.mEnableStateIconIdWrapper);
        } else if (z3) {
            this.mDataConnectionState = 0;
            this.mDataStateIconId = R.drawable.ic_qs_mobile_off;
            this.mDisableStateIconIdWrapper.setResources(this.mContext.getResources());
            this.mDisableStateIconIdWrapper.setIconId(this.mDataStateIconId);
            signalState.label = this.mQuickSettingsExt.customizeDataConnectionTile(this.mDataConnectionState, this.mDisableStateIconIdWrapper, this.mContext.getString(R.string.mobile));
            signalState.icon = QsIconWrapper.get(this.mDisableStateIconIdWrapper.getIconId(), this.mDisableStateIconIdWrapper);
        } else {
            this.mDataConnectionState = 0;
            this.mDataStateIconId = R.drawable.ic_qs_mobile_off;
            this.mDisableStateIconIdWrapper.setResources(this.mContext.getResources());
            this.mDisableStateIconIdWrapper.setIconId(this.mDataStateIconId);
            signalState.label = this.mQuickSettingsExt.customizeDataConnectionTile(this.mDataConnectionState, this.mDisableStateIconIdWrapper, this.mContext.getString(R.string.mobile));
            signalState.icon = QsIconWrapper.get(this.mDisableStateIconIdWrapper.getIconId(), this.mDisableStateIconIdWrapper);
        }
        this.mTileLabel = signalState.label;
        Log.d(this.TAG, "handleUpdateState state=" + signalState);
    }

    private final boolean isDefaultDataSimRadioOn() {
        int defaultDataSubscriptionId = SubscriptionManager.getDefaultDataSubscriptionId();
        boolean z = defaultDataSubscriptionId >= 0 && SIMHelper.isRadioOn(defaultDataSubscriptionId);
        Log.d(this.TAG, "isDefaultDataSimRadioOn subId=" + defaultDataSubscriptionId + ", isRadioOn=" + z);
        return z;
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

        private CallbackInfo() {
        }

        public String toString() {
            return "CallbackInfo[enabled=" + this.enabled + ",wifiEnabled=" + this.wifiEnabled + ",wifiConnected=" + this.wifiConnected + ",airplaneModeEnabled=" + this.airplaneModeEnabled + ",mobileSignalIconId=" + this.mobileSignalIconId + ",dataTypeIconId=" + this.dataTypeIconId + ",activityIn=" + this.activityIn + ",activityOut=" + this.activityOut + ",enabledDesc=" + this.enabledDesc + ",noSim=" + this.noSim + ']';
        }
    }

    private final class MobileDataSignalCallback implements NetworkController.SignalCallback {
        final CallbackInfo mInfo;

        private MobileDataSignalCallback() {
            this.mInfo = new CallbackInfo();
        }

        @Override // com.android.systemui.statusbar.policy.NetworkController.SignalCallback
        public void setWifiIndicators(boolean z, NetworkController.IconState iconState, NetworkController.IconState iconState2, boolean z2, boolean z3, String str, boolean z4, String str2) {
            this.mInfo.wifiEnabled = z;
            this.mInfo.wifiConnected = iconState2.visible;
            MobileDataTile.this.refreshState(this.mInfo);
        }

        @Override // com.android.systemui.statusbar.policy.NetworkController.SignalCallback
        public void setMobileDataIndicators(NetworkController.IconState iconState, NetworkController.IconState iconState2, int i, int i2, int i3, int i4, boolean z, boolean z2, String str, String str2, boolean z3, int i5, boolean z4, boolean z5) {
            if (iconState2 == null) {
                return;
            }
            this.mInfo.enabled = iconState2.visible;
            this.mInfo.mobileSignalIconId = iconState2.icon;
            this.mInfo.dataTypeIconId = i4;
            this.mInfo.activityIn = z;
            this.mInfo.activityOut = z2;
            this.mInfo.enabledDesc = str2;
            Log.d(MobileDataTile.this.TAG, "setMobileDataIndicators mInfo=" + this.mInfo);
            MobileDataTile.this.refreshState(this.mInfo);
        }

        @Override // com.android.systemui.statusbar.policy.NetworkController.SignalCallback
        public void setNoSims(boolean z, boolean z2) {
            this.mInfo.noSim = z;
            if (this.mInfo.noSim) {
                this.mInfo.mobileSignalIconId = 0;
                this.mInfo.dataTypeIconId = 0;
                this.mInfo.enabled = false;
                Log.d(MobileDataTile.this.TAG, "setNoSims noSim=" + z);
            }
            MobileDataTile.this.refreshState(this.mInfo);
        }

        @Override // com.android.systemui.statusbar.policy.NetworkController.SignalCallback
        public void setIsAirplaneMode(NetworkController.IconState iconState) {
            this.mInfo.airplaneModeEnabled = iconState.visible;
            if (this.mInfo.airplaneModeEnabled) {
                this.mInfo.mobileSignalIconId = 0;
                this.mInfo.dataTypeIconId = 0;
                this.mInfo.enabled = false;
            }
            MobileDataTile.this.refreshState(this.mInfo);
        }

        @Override // com.android.systemui.statusbar.policy.NetworkController.SignalCallback
        public void setMobileDataEnabled(boolean z) {
            MobileDataTile.this.refreshState(this.mInfo);
        }
    }
}
