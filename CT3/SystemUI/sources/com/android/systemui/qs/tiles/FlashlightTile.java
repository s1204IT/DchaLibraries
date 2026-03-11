package com.android.systemui.qs.tiles;

import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.widget.Switch;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.FlashlightController;

public class FlashlightTile extends QSTile<QSTile.BooleanState> implements FlashlightController.FlashlightListener {
    private final QSTile<QSTile.BooleanState>.AnimationIcon mDisable;
    private final QSTile<QSTile.BooleanState>.AnimationIcon mEnable;
    private final FlashlightController mFlashlightController;

    public FlashlightTile(QSTile.Host host) {
        super(host);
        this.mEnable = new QSTile.AnimationIcon(R.drawable.ic_signal_flashlight_enable_animation, R.drawable.ic_signal_flashlight_disable);
        this.mDisable = new QSTile.AnimationIcon(R.drawable.ic_signal_flashlight_disable_animation, R.drawable.ic_signal_flashlight_enable);
        this.mFlashlightController = host.getFlashlightController();
        this.mFlashlightController.addListener(this);
    }

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
        this.mFlashlightController.removeListener(this);
    }

    @Override
    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    @Override
    public void setListening(boolean listening) {
    }

    @Override
    protected void handleUserSwitch(int newUserId) {
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent("android.media.action.STILL_IMAGE_CAMERA");
    }

    @Override
    public boolean isAvailable() {
        return this.mFlashlightController.hasFlashlight();
    }

    @Override
    protected void handleClick() {
        if (ActivityManager.isUserAMonkey()) {
            return;
        }
        MetricsLogger.action(this.mContext, getMetricsCategory(), !((QSTile.BooleanState) this.mState).value);
        boolean newState = !((QSTile.BooleanState) this.mState).value;
        refreshState(Boolean.valueOf(newState));
        this.mFlashlightController.setFlashlight(newState);
    }

    @Override
    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_flashlight_label);
    }

    @Override
    protected void handleLongClick() {
        handleClick();
    }

    @Override
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        state.label = this.mHost.getContext().getString(R.string.quick_settings_flashlight_label);
        if (!this.mFlashlightController.isAvailable()) {
            Drawable icon = this.mHost.getContext().getDrawable(R.drawable.ic_signal_flashlight_disable).mutate();
            int disabledColor = this.mHost.getContext().getColor(R.color.qs_tile_tint_unavailable);
            icon.setTint(disabledColor);
            state.icon = new QSTile.DrawableIcon(icon);
            state.label = new SpannableStringBuilder().append(state.label, new ForegroundColorSpan(disabledColor), 18);
            state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_flashlight_unavailable);
            return;
        }
        if (arg instanceof Boolean) {
            boolean value = ((Boolean) arg).booleanValue();
            if (value == state.value) {
                return;
            } else {
                state.value = value;
            }
        } else {
            state.value = this.mFlashlightController.isEnabled();
        }
        state.icon = state.value ? this.mEnable : this.mDisable;
        state.contentDescription = this.mContext.getString(R.string.quick_settings_flashlight_label);
        String name = Switch.class.getName();
        state.expandedAccessibilityClassName = name;
        state.minimalAccessibilityClassName = name;
    }

    @Override
    public int getMetricsCategory() {
        return 119;
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (((QSTile.BooleanState) this.mState).value) {
            return this.mContext.getString(R.string.accessibility_quick_settings_flashlight_changed_on);
        }
        return this.mContext.getString(R.string.accessibility_quick_settings_flashlight_changed_off);
    }

    @Override
    public void onFlashlightChanged(boolean enabled) {
        refreshState(Boolean.valueOf(enabled));
    }

    @Override
    public void onFlashlightError() {
        refreshState(false);
    }

    @Override
    public void onFlashlightAvailabilityChanged(boolean available) {
        refreshState();
    }
}
