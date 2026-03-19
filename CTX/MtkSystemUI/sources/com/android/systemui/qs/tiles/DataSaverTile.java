package com.android.systemui.qs.tiles;

import android.R;
import android.content.DialogInterface;
import android.content.Intent;
import android.widget.Switch;
import com.android.systemui.Dependency;
import com.android.systemui.Prefs;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import com.android.systemui.statusbar.policy.DataSaverController;
import com.android.systemui.statusbar.policy.NetworkController;

public class DataSaverTile extends QSTileImpl<QSTile.BooleanState> implements DataSaverController.Listener {
    private final DataSaverController mDataSaverController;

    public DataSaverTile(QSHost qSHost) {
        super(qSHost);
        this.mDataSaverController = ((NetworkController) Dependency.get(NetworkController.class)).getDataSaverController();
    }

    @Override
    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    @Override
    public void handleSetListening(boolean z) {
        if (z) {
            this.mDataSaverController.addCallback(this);
        } else {
            this.mDataSaverController.removeCallback(this);
        }
    }

    @Override
    public Intent getLongClickIntent() {
        return CellularTile.getCellularSettingIntent();
    }

    @Override
    protected void handleClick() {
        if (((QSTile.BooleanState) this.mState).value || Prefs.getBoolean(this.mContext, "QsDataSaverDialogShown", false)) {
            toggleDataSaver();
            return;
        }
        SystemUIDialog systemUIDialog = new SystemUIDialog(this.mContext);
        systemUIDialog.setTitle(R.string.app_suspended_unsuspend_message);
        systemUIDialog.setMessage(R.string.app_suspended_more_details);
        systemUIDialog.setPositiveButton(R.string.app_suspended_title, new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int i) {
                this.f$0.toggleDataSaver();
            }
        });
        systemUIDialog.setNegativeButton(R.string.cancel, null);
        systemUIDialog.setShowForAllUsers(true);
        systemUIDialog.show();
        Prefs.putBoolean(this.mContext, "QsDataSaverDialogShown", true);
    }

    private void toggleDataSaver() {
        ((QSTile.BooleanState) this.mState).value = !this.mDataSaverController.isDataSaverEnabled();
        this.mDataSaverController.setDataSaverEnabled(((QSTile.BooleanState) this.mState).value);
        refreshState(Boolean.valueOf(((QSTile.BooleanState) this.mState).value));
    }

    @Override
    public CharSequence getTileLabel() {
        return this.mContext.getString(com.android.systemui.R.string.data_saver);
    }

    @Override
    protected void handleUpdateState(QSTile.BooleanState booleanState, Object obj) {
        booleanState.value = obj instanceof Boolean ? ((Boolean) obj).booleanValue() : this.mDataSaverController.isDataSaverEnabled();
        booleanState.state = booleanState.value ? 2 : 1;
        booleanState.label = this.mContext.getString(com.android.systemui.R.string.data_saver);
        booleanState.contentDescription = booleanState.label;
        booleanState.icon = QSTileImpl.ResourceIcon.get(booleanState.value ? com.android.systemui.R.drawable.ic_data_saver : com.android.systemui.R.drawable.ic_data_saver_off);
        booleanState.expandedAccessibilityClassName = Switch.class.getName();
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
    public void onDataSaverChanged(boolean z) {
        refreshState(Boolean.valueOf(z));
    }
}
