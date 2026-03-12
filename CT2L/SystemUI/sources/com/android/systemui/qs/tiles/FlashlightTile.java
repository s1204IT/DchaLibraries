package com.android.systemui.qs.tiles;

import android.app.ActivityManager;
import android.os.SystemClock;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.FlashlightController;

public class FlashlightTile extends QSTile<QSTile.BooleanState> implements FlashlightController.FlashlightListener {
    private final QSTile<QSTile.BooleanState>.AnimationIcon mDisable;
    private final QSTile<QSTile.BooleanState>.AnimationIcon mEnable;
    private final FlashlightController mFlashlightController;
    private Runnable mRecentlyOnTimeout;
    private long mWasLastOn;

    public FlashlightTile(QSTile.Host host) {
        super(host);
        this.mEnable = new QSTile.AnimationIcon(R.drawable.ic_signal_flashlight_enable_animation);
        this.mDisable = new QSTile.AnimationIcon(R.drawable.ic_signal_flashlight_disable_animation);
        this.mRecentlyOnTimeout = new Runnable() {
            @Override
            public void run() {
                FlashlightTile.this.refreshState();
            }
        };
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
    protected void handleClick() {
        if (!ActivityManager.isUserAMonkey()) {
            boolean newState = !((QSTile.BooleanState) this.mState).value;
            this.mFlashlightController.setFlashlight(newState);
            refreshState(newState ? QSTile.UserBoolean.USER_TRUE : QSTile.UserBoolean.USER_FALSE);
        }
    }

    @Override
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        if (state.value) {
            this.mWasLastOn = SystemClock.uptimeMillis();
        }
        if (arg instanceof QSTile.UserBoolean) {
            state.value = ((QSTile.UserBoolean) arg).value;
        }
        if (!state.value && this.mWasLastOn != 0) {
            if (SystemClock.uptimeMillis() > this.mWasLastOn + 500) {
                this.mWasLastOn = 0L;
            } else {
                this.mHandler.removeCallbacks(this.mRecentlyOnTimeout);
                this.mHandler.postAtTime(this.mRecentlyOnTimeout, this.mWasLastOn + 500);
            }
        }
        state.visible = this.mWasLastOn != 0 || this.mFlashlightController.isAvailable();
        state.label = this.mHost.getContext().getString(R.string.quick_settings_flashlight_label);
        QSTile<QSTile.BooleanState>.AnimationIcon icon = state.value ? this.mEnable : this.mDisable;
        icon.setAllowAnimation((arg instanceof QSTile.UserBoolean) && ((QSTile.UserBoolean) arg).userInitiated);
        state.icon = icon;
        int onOrOffId = state.value ? R.string.accessibility_quick_settings_flashlight_on : R.string.accessibility_quick_settings_flashlight_off;
        state.contentDescription = this.mContext.getString(onOrOffId);
    }

    @Override
    protected String composeChangeAnnouncement() {
        return ((QSTile.BooleanState) this.mState).value ? this.mContext.getString(R.string.accessibility_quick_settings_flashlight_changed_on) : this.mContext.getString(R.string.accessibility_quick_settings_flashlight_changed_off);
    }

    @Override
    public void onFlashlightOff() {
        refreshState(QSTile.UserBoolean.BACKGROUND_FALSE);
    }

    @Override
    public void onFlashlightError() {
        refreshState(QSTile.UserBoolean.BACKGROUND_FALSE);
    }

    @Override
    public void onFlashlightAvailabilityChanged(boolean available) {
        refreshState();
    }
}
