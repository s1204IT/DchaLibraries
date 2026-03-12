package com.android.systemui.qs.tiles;

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
        this.mPortraitToAuto = new QSTile.AnimationIcon(R.drawable.ic_portrait_to_auto_rotate_animation);
        this.mAutoToPortrait = new QSTile.AnimationIcon(R.drawable.ic_portrait_from_auto_rotate_animation);
        this.mLandscapeToAuto = new QSTile.AnimationIcon(R.drawable.ic_landscape_to_auto_rotate_animation);
        this.mAutoToLandscape = new QSTile.AnimationIcon(R.drawable.ic_landscape_from_auto_rotate_animation);
        this.mCallback = new RotationLockController.RotationLockControllerCallback() {
            @Override
            public void onRotationLockStateChanged(boolean rotationLocked, boolean affordanceVisible) {
                RotationLockTile.this.refreshState(rotationLocked ? QSTile.UserBoolean.BACKGROUND_TRUE : QSTile.UserBoolean.BACKGROUND_FALSE);
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
        if (this.mController != null) {
            if (listening) {
                this.mController.addRotationLockControllerCallback(this.mCallback);
            } else {
                this.mController.removeRotationLockControllerCallback(this.mCallback);
            }
        }
    }

    @Override
    protected void handleClick() {
        if (this.mController != null) {
            boolean newState = !((QSTile.BooleanState) this.mState).value;
            this.mController.setRotationLocked(newState);
            refreshState(newState ? QSTile.UserBoolean.USER_TRUE : QSTile.UserBoolean.USER_FALSE);
        }
    }

    @Override
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        QSTile<QSTile.BooleanState>.AnimationIcon icon;
        if (this.mController != null) {
            boolean rotationLocked = arg != null ? ((QSTile.UserBoolean) arg).value : this.mController.isRotationLocked();
            boolean userInitiated = arg != null ? ((QSTile.UserBoolean) arg).userInitiated : false;
            state.visible = this.mController.isRotationLockAffordanceVisible();
            state.value = rotationLocked;
            boolean portrait = this.mContext.getResources().getConfiguration().orientation != 2;
            if (rotationLocked) {
                int label = portrait ? R.string.quick_settings_rotation_locked_portrait_label : R.string.quick_settings_rotation_locked_landscape_label;
                state.label = this.mContext.getString(label);
                icon = portrait ? this.mAutoToPortrait : this.mAutoToLandscape;
            } else {
                state.label = this.mContext.getString(R.string.quick_settings_rotation_unlocked_label);
                icon = portrait ? this.mPortraitToAuto : this.mLandscapeToAuto;
            }
            icon.setAllowAnimation(userInitiated);
            state.icon = icon;
            state.contentDescription = getAccessibilityString(rotationLocked, R.string.accessibility_rotation_lock_on_portrait, R.string.accessibility_rotation_lock_on_landscape, R.string.accessibility_rotation_lock_off);
        }
    }

    private String getAccessibilityString(boolean locked, int idWhenPortrait, int idWhenLandscape, int idWhenOff) {
        int stringID;
        if (locked) {
            boolean portrait = this.mContext.getResources().getConfiguration().orientation != 2;
            stringID = portrait ? idWhenPortrait : idWhenLandscape;
        } else {
            stringID = idWhenOff;
        }
        return this.mContext.getString(stringID);
    }

    @Override
    protected String composeChangeAnnouncement() {
        return getAccessibilityString(((QSTile.BooleanState) this.mState).value, R.string.accessibility_rotation_lock_on_portrait_changed, R.string.accessibility_rotation_lock_on_landscape_changed, R.string.accessibility_rotation_lock_off_changed);
    }
}
