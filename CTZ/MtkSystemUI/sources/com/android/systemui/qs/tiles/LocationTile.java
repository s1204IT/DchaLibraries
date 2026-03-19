package com.android.systemui.qs.tiles;

import android.content.Intent;
import android.widget.Switch;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.LocationController;

public class LocationTile extends QSTileImpl<QSTile.BooleanState> {
    private final Callback mCallback;
    private final LocationController mController;
    private final QSTile.Icon mIcon;
    private final KeyguardMonitor mKeyguard;

    public LocationTile(QSHost qSHost) {
        super(qSHost);
        this.mIcon = QSTileImpl.ResourceIcon.get(R.drawable.ic_signal_location);
        this.mCallback = new Callback();
        this.mController = (LocationController) Dependency.get(LocationController.class);
        this.mKeyguard = (KeyguardMonitor) Dependency.get(KeyguardMonitor.class);
    }

    @Override
    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    @Override
    public void handleSetListening(boolean z) {
        if (z) {
            this.mController.addCallback(this.mCallback);
            this.mKeyguard.addCallback(this.mCallback);
        } else {
            this.mController.removeCallback(this.mCallback);
            this.mKeyguard.removeCallback(this.mCallback);
        }
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent("android.settings.LOCATION_SOURCE_SETTINGS");
    }

    @Override
    protected void handleClick() {
        if (this.mKeyguard.isSecure() && this.mKeyguard.isShowing()) {
            ((ActivityStarter) Dependency.get(ActivityStarter.class)).postQSRunnableDismissingKeyguard(new Runnable() {
                @Override
                public final void run() {
                    LocationTile.lambda$handleClick$0(this.f$0);
                }
            });
        } else {
            this.mController.setLocationEnabled(!((QSTile.BooleanState) this.mState).value);
        }
    }

    public static void lambda$handleClick$0(LocationTile locationTile) {
        boolean z = ((QSTile.BooleanState) locationTile.mState).value;
        locationTile.mHost.openPanels();
        locationTile.mController.setLocationEnabled(!z);
    }

    @Override
    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_location_label);
    }

    @Override
    protected void handleUpdateState(QSTile.BooleanState booleanState, Object obj) {
        if (booleanState.slash == null) {
            booleanState.slash = new QSTile.SlashState();
        }
        boolean zIsLocationEnabled = this.mController.isLocationEnabled();
        booleanState.value = zIsLocationEnabled;
        checkIfRestrictionEnforcedByAdminOnly(booleanState, "no_share_location");
        if (!booleanState.disabledByPolicy) {
            checkIfRestrictionEnforcedByAdminOnly(booleanState, "no_config_location");
        }
        booleanState.icon = this.mIcon;
        booleanState.slash.isSlashed = !booleanState.value;
        if (zIsLocationEnabled) {
            booleanState.label = this.mContext.getString(R.string.quick_settings_location_label);
            booleanState.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_location_on);
        } else {
            booleanState.label = this.mContext.getString(R.string.quick_settings_location_label);
            booleanState.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_location_off);
        }
        booleanState.state = booleanState.value ? 2 : 1;
        booleanState.expandedAccessibilityClassName = Switch.class.getName();
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

    private final class Callback implements KeyguardMonitor.Callback, LocationController.LocationChangeCallback {
        private Callback() {
        }

        @Override
        public void onLocationSettingsChanged(boolean z) {
            LocationTile.this.refreshState();
        }

        @Override
        public void onKeyguardShowingChanged() {
            LocationTile.this.refreshState();
        }
    }
}
