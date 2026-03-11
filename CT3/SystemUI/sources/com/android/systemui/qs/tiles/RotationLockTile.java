package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.os.BenesseExtension;
import android.widget.Switch;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.RotationLockController;

public class RotationLockTile extends QSTile<QSTile.BooleanState> {
    private final QSTile<QSTile.BooleanState>.AnimationIcon mAutoToLandscape;
    private final QSTile<QSTile.BooleanState>.AnimationIcon mAutoToPortrait;
    private final RotationLockController.RotationLockControllerCallback mCallback;
    private final RotationLockController mController;
    private final QSTile<QSTile.BooleanState>.AnimationIcon mLandscapeToAuto;
    private final QSTile<QSTile.BooleanState>.AnimationIcon mPortraitToAuto;

    public RotationLockTile(QSTile.Host host) {
        super(host);
        this.mPortraitToAuto = new QSTile.AnimationIcon(R.drawable.ic_portrait_to_auto_rotate_animation, R.drawable.ic_portrait_from_auto_rotate);
        this.mAutoToPortrait = new QSTile.AnimationIcon(R.drawable.ic_portrait_from_auto_rotate_animation, R.drawable.ic_portrait_to_auto_rotate);
        this.mLandscapeToAuto = new QSTile.AnimationIcon(R.drawable.ic_landscape_to_auto_rotate_animation, R.drawable.ic_landscape_from_auto_rotate);
        this.mAutoToLandscape = new QSTile.AnimationIcon(R.drawable.ic_landscape_from_auto_rotate_animation, R.drawable.ic_landscape_to_auto_rotate);
        this.mCallback = new RotationLockController.RotationLockControllerCallback() {
            @Override
            public void onRotationLockStateChanged(boolean rotationLocked, boolean affordanceVisible) {
                RotationLockTile.this.refreshState(Boolean.valueOf(rotationLocked));
            }
        };
        this.mController = host.getRotationLockController();
    }

    @Override
    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    @Override
    public void setListening(boolean listening) {
        if (this.mController == null) {
            return;
        }
        if (listening) {
            this.mController.addRotationLockControllerCallback(this.mCallback);
        } else {
            this.mController.removeRotationLockControllerCallback(this.mCallback);
        }
    }

    @Override
    public Intent getLongClickIntent() {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        return new Intent("android.settings.DISPLAY_SETTINGS");
    }

    @Override
    protected void handleClick() {
        if (this.mController == null) {
            return;
        }
        MetricsLogger.action(this.mContext, getMetricsCategory(), !((QSTile.BooleanState) this.mState).value);
        boolean newState = !((QSTile.BooleanState) this.mState).value;
        this.mController.setRotationLocked(newState ? false : true);
        refreshState(Boolean.valueOf(newState));
    }

    @Override
    public CharSequence getTileLabel() {
        return getState().label;
    }

    @Override
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        if (this.mController == null) {
            return;
        }
        boolean rotationLocked = this.mController.isRotationLocked();
        state.value = !rotationLocked;
        boolean portrait = isCurrentOrientationLockPortrait(this.mController, this.mContext);
        if (rotationLocked) {
            int label = portrait ? R.string.quick_settings_rotation_locked_portrait_label : R.string.quick_settings_rotation_locked_landscape_label;
            state.label = this.mContext.getString(label);
            state.icon = portrait ? this.mAutoToPortrait : this.mAutoToLandscape;
        } else {
            state.label = this.mContext.getString(R.string.quick_settings_rotation_unlocked_label);
            state.icon = portrait ? this.mPortraitToAuto : this.mLandscapeToAuto;
        }
        state.contentDescription = getAccessibilityString(rotationLocked);
        String name = Switch.class.getName();
        state.expandedAccessibilityClassName = name;
        state.minimalAccessibilityClassName = name;
    }

    public static boolean isCurrentOrientationLockPortrait(RotationLockController controller, Context context) {
        int lockOrientation = controller.getRotationLockOrientation();
        return lockOrientation == 0 ? context.getResources().getConfiguration().orientation != 2 : lockOrientation != 2;
    }

    @Override
    public int getMetricsCategory() {
        return 123;
    }

    private String getAccessibilityString(boolean locked) {
        String string;
        if (locked) {
            StringBuilder sbAppend = new StringBuilder().append(this.mContext.getString(R.string.accessibility_quick_settings_rotation)).append(",");
            Context context = this.mContext;
            Object[] objArr = new Object[1];
            if (isCurrentOrientationLockPortrait(this.mController, this.mContext)) {
                string = this.mContext.getString(R.string.quick_settings_rotation_locked_portrait_label);
            } else {
                string = this.mContext.getString(R.string.quick_settings_rotation_locked_landscape_label);
            }
            objArr[0] = string;
            return sbAppend.append(context.getString(R.string.accessibility_quick_settings_rotation_value, objArr)).toString();
        }
        return this.mContext.getString(R.string.accessibility_quick_settings_rotation);
    }

    @Override
    protected String composeChangeAnnouncement() {
        return getAccessibilityString(((QSTile.BooleanState) this.mState).value);
    }
}
