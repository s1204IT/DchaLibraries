package com.android.systemui.qs.tiles;

import android.R;
import android.content.DialogInterface;
import android.content.Intent;
import android.widget.Switch;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Prefs;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import com.android.systemui.statusbar.policy.DataSaverController;

public class DataSaverTile extends QSTile<QSTile.BooleanState> implements DataSaverController.Listener {
    private final DataSaverController mDataSaverController;

    public DataSaverTile(QSTile.Host host) {
        super(host);
        this.mDataSaverController = host.getNetworkController().getDataSaverController();
    }

    @Override
    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            this.mDataSaverController.addListener(this);
        } else {
            this.mDataSaverController.remListener(this);
        }
    }

    @Override
    public Intent getLongClickIntent() {
        return CellularTile.CELLULAR_SETTINGS;
    }

    @Override
    protected void handleClick() {
        if (((QSTile.BooleanState) this.mState).value || Prefs.getBoolean(this.mContext, "QsDataSaverDialogShown", false)) {
            toggleDataSaver();
            return;
        }
        SystemUIDialog dialog = new SystemUIDialog(this.mContext);
        dialog.setTitle(R.string.lockscreen_glogin_password_hint);
        dialog.setMessage(R.string.lockscreen_glogin_invalid_input);
        dialog.setPositiveButton(R.string.lockscreen_glogin_submit_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog2, int which) {
                DataSaverTile.this.toggleDataSaver();
            }
        });
        dialog.setNegativeButton(R.string.cancel, null);
        dialog.setShowForAllUsers(true);
        dialog.show();
        Prefs.putBoolean(this.mContext, "QsDataSaverDialogShown", true);
    }

    public void toggleDataSaver() {
        ((QSTile.BooleanState) this.mState).value = !this.mDataSaverController.isDataSaverEnabled();
        MetricsLogger.action(this.mContext, getMetricsCategory(), ((QSTile.BooleanState) this.mState).value);
        this.mDataSaverController.setDataSaverEnabled(((QSTile.BooleanState) this.mState).value);
        refreshState(Boolean.valueOf(((QSTile.BooleanState) this.mState).value));
    }

    @Override
    public CharSequence getTileLabel() {
        return this.mContext.getString(com.android.systemui.R.string.data_saver);
    }

    @Override
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        state.value = arg instanceof Boolean ? ((Boolean) arg).booleanValue() : this.mDataSaverController.isDataSaverEnabled();
        state.label = this.mContext.getString(com.android.systemui.R.string.data_saver);
        state.contentDescription = state.label;
        state.icon = QSTile.ResourceIcon.get(state.value ? com.android.systemui.R.drawable.ic_data_saver : com.android.systemui.R.drawable.ic_data_saver_off);
        String name = Switch.class.getName();
        state.expandedAccessibilityClassName = name;
        state.minimalAccessibilityClassName = name;
    }

    @Override
    public int getMetricsCategory() {
        return 284;
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (((QSTile.BooleanState) this.mState).value) {
            return this.mContext.getString(com.android.systemui.R.string.accessibility_quick_settings_data_saver_changed_on);
        }
        return this.mContext.getString(com.android.systemui.R.string.accessibility_quick_settings_data_saver_changed_off);
    }

    @Override
    public void onDataSaverChanged(boolean isDataSaving) {
        refreshState(Boolean.valueOf(isDataSaving));
    }
}
