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

/* loaded from: classes.dex */
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

    /* JADX DEBUG: Method merged with bridge method: newTileState()Lcom/android/systemui/plugins/qs/QSTile$State; */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public void handleSetListening(boolean z) {
        if (z) {
            this.mController.addCallback(this.mCallback);
            this.mKeyguard.addCallback(this.mCallback);
        } else {
            this.mController.removeCallback(this.mCallback);
            this.mKeyguard.removeCallback(this.mCallback);
        }
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public Intent getLongClickIntent() {
        return new Intent("android.settings.LOCATION_SOURCE_SETTINGS");
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleClick() {
        if (this.mKeyguard.isSecure() && this.mKeyguard.isShowing()) {
            ((ActivityStarter) Dependency.get(ActivityStarter.class)).postQSRunnableDismissingKeyguard(new Runnable() { // from class: com.android.systemui.qs.tiles.-$$Lambda$LocationTile$cnlxD4jGztrpcRYGbQTKRSm3Ng0
                @Override // java.lang.Runnable
                public final void run() {
                    LocationTile.lambda$handleClick$0(this.f$0);
                }
            });
        } else {
            this.mController.setLocationEnabled(!((QSTile.BooleanState) this.mState).value);
        }
    }

    public static /* synthetic */ void lambda$handleClick$0(LocationTile locationTile) {
        boolean z = ((QSTile.BooleanState) locationTile.mState).value;
        locationTile.mHost.openPanels();
        locationTile.mController.setLocationEnabled(!z);
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_location_label);
    }

    /* JADX DEBUG: Method merged with bridge method: handleUpdateState(Lcom/android/systemui/plugins/qs/QSTile$State;Ljava/lang/Object;)V */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
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

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl, com.android.systemui.plugins.qs.QSTile
    public int getMetricsCategory() {
        return 122;
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected String composeChangeAnnouncement() {
        if (((QSTile.BooleanState) this.mState).value) {
            return this.mContext.getString(R.string.accessibility_quick_settings_location_changed_on);
        }
        return this.mContext.getString(R.string.accessibility_quick_settings_location_changed_off);
    }

    private final class Callback implements KeyguardMonitor.Callback, LocationController.LocationChangeCallback {
        private Callback() {
        }

        @Override // com.android.systemui.statusbar.policy.LocationController.LocationChangeCallback
        public void onLocationSettingsChanged(boolean z) {
            LocationTile.this.refreshState();
        }

        @Override // com.android.systemui.statusbar.policy.KeyguardMonitor.Callback
        public void onKeyguardShowingChanged() {
            LocationTile.this.refreshState();
        }
    }
}
