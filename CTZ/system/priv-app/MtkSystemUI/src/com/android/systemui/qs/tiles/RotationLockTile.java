package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.widget.Switch;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.RotationLockController;

/* loaded from: classes.dex */
public class RotationLockTile extends QSTileImpl<QSTile.BooleanState> {
    private final RotationLockController.RotationLockControllerCallback mCallback;
    private final RotationLockController mController;
    private final QSTile.Icon mIcon;

    public RotationLockTile(QSHost qSHost) {
        super(qSHost);
        this.mIcon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_auto_rotate);
        this.mCallback = new RotationLockController.RotationLockControllerCallback() { // from class: com.android.systemui.qs.tiles.RotationLockTile.1
            @Override // com.android.systemui.statusbar.policy.RotationLockController.RotationLockControllerCallback
            public void onRotationLockStateChanged(boolean z, boolean z2) {
                RotationLockTile.this.refreshState(Boolean.valueOf(z));
            }
        };
        this.mController = (RotationLockController) Dependency.get(RotationLockController.class);
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
        } else {
            this.mController.removeCallback(this.mCallback);
        }
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public Intent getLongClickIntent() {
        return new Intent("android.settings.DISPLAY_SETTINGS");
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleClick() {
        boolean z = !((QSTile.BooleanState) this.mState).value;
        this.mController.setRotationLocked(z ? false : true);
        refreshState(Boolean.valueOf(z));
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public CharSequence getTileLabel() {
        return getState().label;
    }

    /* JADX DEBUG: Method merged with bridge method: handleUpdateState(Lcom/android/systemui/plugins/qs/QSTile$State;Ljava/lang/Object;)V */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleUpdateState(QSTile.BooleanState booleanState, Object obj) {
        boolean zIsRotationLocked = this.mController.isRotationLocked();
        booleanState.value = !zIsRotationLocked;
        booleanState.label = this.mContext.getString(R.string.quick_settings_rotation_unlocked_label);
        booleanState.icon = this.mIcon;
        booleanState.contentDescription = getAccessibilityString(zIsRotationLocked);
        booleanState.expandedAccessibilityClassName = Switch.class.getName();
        booleanState.state = booleanState.value ? 2 : 1;
    }

    public static boolean isCurrentOrientationLockPortrait(RotationLockController rotationLockController, Context context) {
        int rotationLockOrientation = rotationLockController.getRotationLockOrientation();
        return rotationLockOrientation == 0 ? context.getResources().getConfiguration().orientation != 2 : rotationLockOrientation != 2;
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl, com.android.systemui.plugins.qs.QSTile
    public int getMetricsCategory() {
        return 123;
    }

    private String getAccessibilityString(boolean z) {
        return this.mContext.getString(R.string.accessibility_quick_settings_rotation);
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected String composeChangeAnnouncement() {
        return getAccessibilityString(((QSTile.BooleanState) this.mState).value);
    }
}
