package com.android.systemui.qs.tiles;

import android.app.ActivityManager;
import android.content.Intent;
import android.widget.Switch;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.FlashlightController;

/* loaded from: classes.dex */
public class FlashlightTile extends QSTileImpl<QSTile.BooleanState> implements FlashlightController.FlashlightListener {
    private final FlashlightController mFlashlightController;
    private final QSTile.Icon mIcon;

    public FlashlightTile(QSHost qSHost) {
        super(qSHost);
        this.mIcon = QSTileImpl.ResourceIcon.get(R.drawable.ic_signal_flashlight);
        this.mFlashlightController = (FlashlightController) Dependency.get(FlashlightController.class);
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleDestroy() {
        super.handleDestroy();
    }

    /* JADX DEBUG: Method merged with bridge method: newTileState()Lcom/android/systemui/plugins/qs/QSTile$State; */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public void handleSetListening(boolean z) {
        if (z) {
            this.mFlashlightController.addCallback(this);
        } else {
            this.mFlashlightController.removeCallback(this);
        }
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleUserSwitch(int i) {
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public Intent getLongClickIntent() {
        return new Intent("android.media.action.STILL_IMAGE_CAMERA");
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl, com.android.systemui.plugins.qs.QSTile
    public boolean isAvailable() {
        return this.mFlashlightController.hasFlashlight();
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleClick() {
        if (ActivityManager.isUserAMonkey()) {
            return;
        }
        boolean z = !((QSTile.BooleanState) this.mState).value;
        refreshState(Boolean.valueOf(z));
        this.mFlashlightController.setFlashlight(z);
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_flashlight_label);
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleLongClick() {
        handleClick();
    }

    /* JADX DEBUG: Method merged with bridge method: handleUpdateState(Lcom/android/systemui/plugins/qs/QSTile$State;Ljava/lang/Object;)V */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleUpdateState(QSTile.BooleanState booleanState, Object obj) {
        if (booleanState.slash == null) {
            booleanState.slash = new QSTile.SlashState();
        }
        booleanState.label = this.mHost.getContext().getString(R.string.quick_settings_flashlight_label);
        if (!this.mFlashlightController.isAvailable()) {
            booleanState.icon = this.mIcon;
            booleanState.slash.isSlashed = true;
            booleanState.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_flashlight_unavailable);
            booleanState.state = 0;
            return;
        }
        if (obj instanceof Boolean) {
            boolean zBooleanValue = ((Boolean) obj).booleanValue();
            if (zBooleanValue == booleanState.value) {
                return;
            } else {
                booleanState.value = zBooleanValue;
            }
        } else {
            booleanState.value = this.mFlashlightController.isEnabled();
        }
        booleanState.icon = this.mIcon;
        booleanState.slash.isSlashed = !booleanState.value;
        booleanState.contentDescription = this.mContext.getString(R.string.quick_settings_flashlight_label);
        booleanState.expandedAccessibilityClassName = Switch.class.getName();
        booleanState.state = booleanState.value ? 2 : 1;
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl, com.android.systemui.plugins.qs.QSTile
    public int getMetricsCategory() {
        return com.android.systemui.plugins.R.styleable.AppCompatTheme_windowMinWidthMinor;
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected String composeChangeAnnouncement() {
        if (((QSTile.BooleanState) this.mState).value) {
            return this.mContext.getString(R.string.accessibility_quick_settings_flashlight_changed_on);
        }
        return this.mContext.getString(R.string.accessibility_quick_settings_flashlight_changed_off);
    }

    @Override // com.android.systemui.statusbar.policy.FlashlightController.FlashlightListener
    public void onFlashlightChanged(boolean z) {
        refreshState(Boolean.valueOf(z));
    }

    @Override // com.android.systemui.statusbar.policy.FlashlightController.FlashlightListener
    public void onFlashlightError() {
        refreshState(false);
    }

    @Override // com.android.systemui.statusbar.policy.FlashlightController.FlashlightListener
    public void onFlashlightAvailabilityChanged(boolean z) {
        refreshState();
    }
}
