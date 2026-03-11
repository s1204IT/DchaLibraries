package com.android.systemui.qs.tiles;

import android.content.ComponentName;
import android.content.Intent;
import android.widget.Switch;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.GlobalSetting;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.DataSaverController;
import com.android.systemui.statusbar.policy.HotspotController;

public class HotspotTile extends QSTileImpl<QSTile.AirplaneBooleanState> {
    private static final Intent TETHER_SETTINGS = new Intent().setComponent(new ComponentName("com.android.settings", "com.android.settings.TetherSettings"));
    private final GlobalSetting mAirplaneMode;
    private final HotspotAndDataSaverCallbacks mCallbacks;
    private final DataSaverController mDataSaverController;
    private final QSTile.Icon mEnabledStatic;
    private final HotspotController mHotspotController;
    private boolean mListening;

    public HotspotTile(QSHost qSHost) {
        super(qSHost);
        this.mEnabledStatic = QSTileImpl.ResourceIcon.get(R.drawable.ic_hotspot);
        this.mCallbacks = new HotspotAndDataSaverCallbacks();
        this.mHotspotController = (HotspotController) Dependency.get(HotspotController.class);
        this.mDataSaverController = (DataSaverController) Dependency.get(DataSaverController.class);
        this.mAirplaneMode = new GlobalSetting(this.mContext, this.mHandler, "airplane_mode_on") {
            @Override
            protected void handleValueChanged(int i) {
                HotspotTile.this.refreshState();
            }
        };
    }

    @Override
    public boolean isAvailable() {
        return this.mHotspotController.isHotspotSupported();
    }

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
    }

    @Override
    public QSTile.AirplaneBooleanState newTileState() {
        return new QSTile.AirplaneBooleanState();
    }

    @Override
    public void handleSetListening(boolean z) {
        if (this.mListening == z) {
            return;
        }
        this.mListening = z;
        if (z) {
            this.mHotspotController.addCallback(this.mCallbacks);
            this.mDataSaverController.addCallback(this.mCallbacks);
            refreshState();
        } else {
            this.mHotspotController.removeCallback(this.mCallbacks);
            this.mDataSaverController.removeCallback(this.mCallbacks);
        }
        this.mAirplaneMode.setListening(z);
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent(TETHER_SETTINGS);
    }

    @Override
    protected void handleClick() {
        boolean z = ((QSTile.AirplaneBooleanState) this.mState).value;
        if (!z && (this.mAirplaneMode.getValue() != 0 || this.mDataSaverController.isDataSaverEnabled())) {
            return;
        }
        refreshState(z ? null : ARG_SHOW_TRANSIENT_ENABLING);
        this.mHotspotController.setHotspotEnabled(!z);
    }

    @Override
    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_hotspot_label);
    }

    @Override
    public void handleUpdateState(QSTile.AirplaneBooleanState airplaneBooleanState, Object obj) {
        int numConnectedDevices;
        boolean zIsDataSaverEnabled;
        boolean z = obj == ARG_SHOW_TRANSIENT_ENABLING;
        if (airplaneBooleanState.slash == null) {
            airplaneBooleanState.slash = new QSTile.SlashState();
        }
        boolean z2 = z || this.mHotspotController.isHotspotTransient();
        checkIfRestrictionEnforcedByAdminOnly(airplaneBooleanState, "no_config_tethering");
        if (obj instanceof CallbackInfo) {
            CallbackInfo callbackInfo = (CallbackInfo) obj;
            airplaneBooleanState.value = z || callbackInfo.isHotspotEnabled;
            numConnectedDevices = callbackInfo.numConnectedDevices;
            zIsDataSaverEnabled = callbackInfo.isDataSaverEnabled;
        } else {
            airplaneBooleanState.value = z || this.mHotspotController.isHotspotEnabled();
            numConnectedDevices = this.mHotspotController.getNumConnectedDevices();
            zIsDataSaverEnabled = this.mDataSaverController.isDataSaverEnabled();
        }
        airplaneBooleanState.icon = this.mEnabledStatic;
        airplaneBooleanState.label = this.mContext.getString(R.string.quick_settings_hotspot_label);
        airplaneBooleanState.isAirplaneMode = this.mAirplaneMode.getValue() != 0;
        airplaneBooleanState.isTransient = z2;
        airplaneBooleanState.slash.isSlashed = (airplaneBooleanState.value || airplaneBooleanState.isTransient) ? false : true;
        if (airplaneBooleanState.isTransient) {
            airplaneBooleanState.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_hotspot_transient_animation);
        }
        airplaneBooleanState.expandedAccessibilityClassName = Switch.class.getName();
        airplaneBooleanState.contentDescription = airplaneBooleanState.label;
        boolean z3 = airplaneBooleanState.isAirplaneMode || zIsDataSaverEnabled;
        boolean z4 = airplaneBooleanState.value || airplaneBooleanState.isTransient;
        if (z3) {
            airplaneBooleanState.state = 0;
        } else {
            airplaneBooleanState.state = z4 ? 2 : 1;
        }
        airplaneBooleanState.secondaryLabel = getSecondaryLabel(z4, z2, zIsDataSaverEnabled, numConnectedDevices);
    }

    private String getSecondaryLabel(boolean z, boolean z2, boolean z3, int i) {
        if (z2) {
            return this.mContext.getString(R.string.quick_settings_hotspot_secondary_label_transient);
        }
        if (z3) {
            return this.mContext.getString(R.string.quick_settings_hotspot_secondary_label_data_saver_enabled);
        }
        if (i > 0 && z) {
            return this.mContext.getResources().getQuantityString(R.plurals.quick_settings_hotspot_secondary_label_num_devices, i, Integer.valueOf(i));
        }
        return null;
    }

    @Override
    public int getMetricsCategory() {
        return com.android.systemui.plugins.R.styleable.AppCompatTheme_windowNoTitle;
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (((QSTile.AirplaneBooleanState) this.mState).value) {
            return this.mContext.getString(R.string.accessibility_quick_settings_hotspot_changed_on);
        }
        return this.mContext.getString(R.string.accessibility_quick_settings_hotspot_changed_off);
    }

    private final class HotspotAndDataSaverCallbacks implements DataSaverController.Listener, HotspotController.Callback {
        CallbackInfo mCallbackInfo;

        private HotspotAndDataSaverCallbacks() {
            this.mCallbackInfo = new CallbackInfo();
        }

        @Override
        public void onDataSaverChanged(boolean z) {
            this.mCallbackInfo.isDataSaverEnabled = z;
            HotspotTile.this.refreshState(this.mCallbackInfo);
        }

        @Override
        public void onHotspotChanged(boolean z, int i) {
            this.mCallbackInfo.isHotspotEnabled = z;
            this.mCallbackInfo.numConnectedDevices = i;
            HotspotTile.this.refreshState(this.mCallbackInfo);
        }
    }

    protected static final class CallbackInfo {
        boolean isDataSaverEnabled;
        boolean isHotspotEnabled;
        int numConnectedDevices;

        protected CallbackInfo() {
        }

        public String toString() {
            return "CallbackInfo[isHotspotEnabled=" + this.isHotspotEnabled + ",numConnectedDevices=" + this.numConnectedDevices + ",isDataSaverEnabled=" + this.isDataSaverEnabled + ']';
        }
    }
}
