package com.android.systemui.qs.tiles;

import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.LocationController;

public class LocationTile extends QSTile<QSTile.BooleanState> {
    private final Callback mCallback;
    private final LocationController mController;
    private final QSTile<QSTile.BooleanState>.AnimationIcon mDisable;
    private final QSTile<QSTile.BooleanState>.AnimationIcon mEnable;
    private final KeyguardMonitor mKeyguard;

    public LocationTile(QSTile.Host host) {
        super(host);
        this.mEnable = new QSTile.AnimationIcon(R.drawable.ic_signal_location_enable_animation);
        this.mDisable = new QSTile.AnimationIcon(R.drawable.ic_signal_location_disable_animation);
        this.mCallback = new Callback();
        this.mController = host.getLocationController();
        this.mKeyguard = host.getKeyguardMonitor();
    }

    @Override
    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            this.mController.addSettingsChangedCallback(this.mCallback);
            this.mKeyguard.addCallback(this.mCallback);
        } else {
            this.mController.removeSettingsChangedCallback(this.mCallback);
            this.mKeyguard.removeCallback(this.mCallback);
        }
    }

    @Override
    protected void handleClick() {
        boolean wasEnabled = Boolean.valueOf(((QSTile.BooleanState) this.mState).value).booleanValue();
        this.mController.setLocationEnabled(!wasEnabled);
        this.mEnable.setAllowAnimation(true);
        this.mDisable.setAllowAnimation(true);
    }

    @Override
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        boolean locationEnabled = this.mController.isLocationEnabled();
        state.visible = !this.mKeyguard.isShowing();
        state.value = locationEnabled;
        if (locationEnabled) {
            state.icon = this.mEnable;
            state.label = this.mContext.getString(R.string.quick_settings_location_label);
            state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_location_on);
        } else {
            state.icon = this.mDisable;
            state.label = this.mContext.getString(R.string.quick_settings_location_label);
            state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_location_off);
        }
    }

    @Override
    protected String composeChangeAnnouncement() {
        return ((QSTile.BooleanState) this.mState).value ? this.mContext.getString(R.string.accessibility_quick_settings_location_changed_on) : this.mContext.getString(R.string.accessibility_quick_settings_location_changed_off);
    }

    private final class Callback implements KeyguardMonitor.Callback, LocationController.LocationSettingsChangeCallback {
        private Callback() {
        }

        @Override
        public void onLocationSettingsChanged(boolean enabled) {
            LocationTile.this.refreshState();
        }

        @Override
        public void onKeyguardChanged() {
            LocationTile.this.refreshState();
        }
    }
}
