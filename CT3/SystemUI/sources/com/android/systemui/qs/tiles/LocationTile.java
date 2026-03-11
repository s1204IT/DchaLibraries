package com.android.systemui.qs.tiles;

import android.content.Intent;
import android.os.BenesseExtension;
import android.widget.Switch;
import com.android.internal.logging.MetricsLogger;
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
        this.mEnable = new QSTile.AnimationIcon(R.drawable.ic_signal_location_enable_animation, R.drawable.ic_signal_location_disable);
        this.mDisable = new QSTile.AnimationIcon(R.drawable.ic_signal_location_disable_animation, R.drawable.ic_signal_location_enable);
        this.mCallback = new Callback(this, null);
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
    public Intent getLongClickIntent() {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        return new Intent("android.settings.LOCATION_SOURCE_SETTINGS");
    }

    @Override
    protected void handleClick() {
        if (this.mKeyguard.isSecure() && this.mKeyguard.isShowing()) {
            this.mHost.startRunnableDismissingKeyguard(new Runnable() {
                @Override
                public void run() {
                    boolean wasEnabled = Boolean.valueOf(((QSTile.BooleanState) LocationTile.this.mState).value).booleanValue();
                    LocationTile.this.mHost.openPanels();
                    MetricsLogger.action(LocationTile.this.mContext, LocationTile.this.getMetricsCategory(), !wasEnabled);
                    LocationTile.this.mController.setLocationEnabled(wasEnabled ? false : true);
                }
            });
            return;
        }
        boolean wasEnabled = Boolean.valueOf(((QSTile.BooleanState) this.mState).value).booleanValue();
        MetricsLogger.action(this.mContext, getMetricsCategory(), !wasEnabled);
        this.mController.setLocationEnabled(wasEnabled ? false : true);
    }

    @Override
    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_location_label);
    }

    @Override
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        boolean locationEnabled = this.mController.isLocationEnabled();
        state.value = locationEnabled;
        checkIfRestrictionEnforcedByAdminOnly(state, "no_share_location");
        if (locationEnabled) {
            state.icon = this.mEnable;
            state.label = this.mContext.getString(R.string.quick_settings_location_label);
            state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_location_on);
        } else {
            state.icon = this.mDisable;
            state.label = this.mContext.getString(R.string.quick_settings_location_label);
            state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_location_off);
        }
        String name = Switch.class.getName();
        state.expandedAccessibilityClassName = name;
        state.minimalAccessibilityClassName = name;
    }

    @Override
    public int getMetricsCategory() {
        return 122;
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (((QSTile.BooleanState) this.mState).value) {
            return this.mContext.getString(R.string.accessibility_quick_settings_location_changed_on);
        }
        return this.mContext.getString(R.string.accessibility_quick_settings_location_changed_off);
    }

    @Override
    public boolean isAvailable() {
        return this.mContext.getPackageManager().hasSystemFeature("android.hardware.location");
    }

    private final class Callback implements LocationController.LocationSettingsChangeCallback, KeyguardMonitor.Callback {
        Callback(LocationTile this$0, Callback callback) {
            this();
        }

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
