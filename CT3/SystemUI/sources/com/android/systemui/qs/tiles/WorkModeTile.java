package com.android.systemui.qs.tiles;

import android.content.Intent;
import android.os.BenesseExtension;
import android.widget.Switch;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.phone.ManagedProfileController;

public class WorkModeTile extends QSTile<QSTile.BooleanState> implements ManagedProfileController.Callback {
    private final QSTile<QSTile.BooleanState>.AnimationIcon mDisable;
    private final QSTile<QSTile.BooleanState>.AnimationIcon mEnable;
    private final ManagedProfileController mProfileController;

    public WorkModeTile(QSTile.Host host) {
        super(host);
        this.mEnable = new QSTile.AnimationIcon(R.drawable.ic_signal_workmode_enable_animation, R.drawable.ic_signal_workmode_disable);
        this.mDisable = new QSTile.AnimationIcon(R.drawable.ic_signal_workmode_disable_animation, R.drawable.ic_signal_workmode_enable);
        this.mProfileController = host.getManagedProfileController();
    }

    @Override
    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            this.mProfileController.addCallback(this);
        } else {
            this.mProfileController.removeCallback(this);
        }
    }

    @Override
    public Intent getLongClickIntent() {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        return new Intent("android.settings.SYNC_SETTINGS");
    }

    @Override
    public void handleClick() {
        MetricsLogger.action(this.mContext, getMetricsCategory(), !((QSTile.BooleanState) this.mState).value);
        this.mProfileController.setWorkModeEnabled(((QSTile.BooleanState) this.mState).value ? false : true);
    }

    @Override
    public boolean isAvailable() {
        return this.mProfileController.hasActiveProfile();
    }

    @Override
    public void onManagedProfileChanged() {
        refreshState(Boolean.valueOf(this.mProfileController.isWorkModeEnabled()));
    }

    @Override
    public void onManagedProfileRemoved() {
        this.mHost.removeTile(getTileSpec());
    }

    @Override
    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_work_mode_label);
    }

    @Override
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        if (arg instanceof Boolean) {
            state.value = ((Boolean) arg).booleanValue();
        } else {
            state.value = this.mProfileController.isWorkModeEnabled();
        }
        state.label = this.mContext.getString(R.string.quick_settings_work_mode_label);
        if (state.value) {
            state.icon = this.mEnable;
            state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_work_mode_on);
        } else {
            state.icon = this.mDisable;
            state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_work_mode_off);
        }
        String name = Switch.class.getName();
        state.expandedAccessibilityClassName = name;
        state.minimalAccessibilityClassName = name;
    }

    @Override
    public int getMetricsCategory() {
        return 257;
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (((QSTile.BooleanState) this.mState).value) {
            return this.mContext.getString(R.string.accessibility_quick_settings_work_mode_changed_on);
        }
        return this.mContext.getString(R.string.accessibility_quick_settings_work_mode_changed_off);
    }
}
