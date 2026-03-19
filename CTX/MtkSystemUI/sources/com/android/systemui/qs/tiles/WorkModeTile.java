package com.android.systemui.qs.tiles;

import android.content.Intent;
import android.widget.Switch;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.phone.ManagedProfileController;

public class WorkModeTile extends QSTileImpl<QSTile.BooleanState> implements ManagedProfileController.Callback {
    private final QSTile.Icon mIcon;
    private final ManagedProfileController mProfileController;

    public WorkModeTile(QSHost qSHost) {
        super(qSHost);
        this.mIcon = QSTileImpl.ResourceIcon.get(R.drawable.ic_signal_workmode_disable);
        this.mProfileController = (ManagedProfileController) Dependency.get(ManagedProfileController.class);
    }

    @Override
    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    @Override
    public void handleSetListening(boolean z) {
        if (z) {
            this.mProfileController.addCallback(this);
        } else {
            this.mProfileController.removeCallback(this);
        }
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent("android.settings.MANAGED_PROFILE_SETTINGS");
    }

    @Override
    public void handleClick() {
        this.mProfileController.setWorkModeEnabled(!((QSTile.BooleanState) this.mState).value);
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
    protected void handleUpdateState(QSTile.BooleanState booleanState, Object obj) {
        if (!isAvailable()) {
            onManagedProfileRemoved();
        }
        if (booleanState.slash == null) {
            booleanState.slash = new QSTile.SlashState();
        }
        if (obj instanceof Boolean) {
            booleanState.value = ((Boolean) obj).booleanValue();
        } else {
            booleanState.value = this.mProfileController.isWorkModeEnabled();
        }
        booleanState.icon = this.mIcon;
        if (booleanState.value) {
            booleanState.slash.isSlashed = false;
            booleanState.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_work_mode_on);
        } else {
            booleanState.slash.isSlashed = true;
            booleanState.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_work_mode_off);
        }
        booleanState.label = this.mContext.getString(R.string.quick_settings_work_mode_label);
        booleanState.expandedAccessibilityClassName = Switch.class.getName();
        booleanState.state = booleanState.value ? 2 : 1;
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
