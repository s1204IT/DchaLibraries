package com.android.systemui.tuner;

import android.content.Intent;
import com.android.systemui.Prefs;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.NightModeController;

public class NightModeTile extends QSTile<QSTile.State> implements NightModeController.Listener {
    private final NightModeController mNightModeController;

    public NightModeTile(QSTile.Host host) {
        super(host);
        this.mNightModeController = host.getNightModeController();
    }

    @Override
    public boolean isAvailable() {
        if (Prefs.getBoolean(this.mContext, "QsNightAdded", false)) {
            return TunerService.isTunerEnabled(this.mContext);
        }
        return false;
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            this.mNightModeController.addListener(this);
            refreshState();
        } else {
            this.mNightModeController.removeListener(this);
        }
    }

    @Override
    public QSTile.State newTileState() {
        return new QSTile.State();
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent(this.mContext, (Class<?>) TunerActivity.class).putExtra("show_night_mode", true);
    }

    @Override
    protected void handleClick() {
        this.mNightModeController.setNightMode(!this.mNightModeController.isEnabled());
        refreshState();
    }

    @Override
    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.night_mode);
    }

    @Override
    protected void handleUpdateState(QSTile.State state, Object arg) {
        boolean enabled = this.mNightModeController.isEnabled();
        state.icon = QSTile.ResourceIcon.get(enabled ? R.drawable.ic_night_mode : R.drawable.ic_night_mode_disabled);
        state.label = this.mContext.getString(R.string.night_mode);
        state.contentDescription = this.mContext.getString(R.string.night_mode);
    }

    @Override
    public void onNightModeChanged() {
        refreshState();
    }

    @Override
    public int getMetricsCategory() {
        return 267;
    }
}
