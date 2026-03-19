package com.android.systemui.qs.tiles;

import android.app.ActivityManager;
import android.content.Intent;
import android.metrics.LogMaker;
import android.provider.Settings;
import android.util.Log;
import android.widget.Switch;
import com.android.internal.app.ColorDisplayController;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class NightDisplayTile extends QSTileImpl<QSTile.BooleanState> implements ColorDisplayController.Callback {
    private ColorDisplayController mController;
    private boolean mIsListening;

    public NightDisplayTile(QSHost qSHost) {
        super(qSHost);
        this.mController = new ColorDisplayController(this.mContext, ActivityManager.getCurrentUser());
    }

    @Override
    public boolean isAvailable() {
        return ColorDisplayController.isAvailable(this.mContext);
    }

    @Override
    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    @Override
    protected void handleClick() {
        if ("1".equals(Settings.Global.getString(this.mContext.getContentResolver(), "night_display_forced_auto_mode_available")) && this.mController.getAutoModeRaw() == -1) {
            this.mController.setAutoMode(1);
            Log.i("NightDisplayTile", "Enrolled in forced night display auto mode");
        }
        this.mController.setActivated(!((QSTile.BooleanState) this.mState).value);
    }

    @Override
    protected void handleUserSwitch(int i) {
        if (this.mIsListening) {
            this.mController.setListener((ColorDisplayController.Callback) null);
        }
        this.mController = new ColorDisplayController(this.mContext, i);
        if (this.mIsListening) {
            this.mController.setListener(this);
        }
        super.handleUserSwitch(i);
    }

    @Override
    protected void handleUpdateState(QSTile.BooleanState booleanState, Object obj) {
        booleanState.value = this.mController.isActivated();
        String string = this.mContext.getString(R.string.quick_settings_night_display_label);
        booleanState.contentDescription = string;
        booleanState.label = string;
        booleanState.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_night_display_on);
        booleanState.expandedAccessibilityClassName = Switch.class.getName();
        booleanState.state = booleanState.value ? 2 : 1;
        booleanState.secondaryLabel = getSecondaryLabel(booleanState.value);
    }

    private String getSecondaryLabel(boolean z) {
        LocalTime customStartTime;
        int i;
        switch (this.mController.getAutoMode()) {
            case 1:
                if (z) {
                    customStartTime = this.mController.getCustomEndTime();
                    i = R.string.quick_settings_secondary_label_until;
                } else {
                    customStartTime = this.mController.getCustomStartTime();
                    i = R.string.quick_settings_night_secondary_label_on_at;
                }
                return this.mContext.getString(i, customStartTime.format(DateTimeFormatter.ofPattern(customStartTime.getMinute() == 0 ? "h a" : "h:mm a")));
            case 2:
                if (z) {
                    return this.mContext.getString(R.string.quick_settings_night_secondary_label_until_sunrise);
                }
                return this.mContext.getString(R.string.quick_settings_night_secondary_label_on_at_sunset);
            default:
                return null;
        }
    }

    @Override
    public int getMetricsCategory() {
        return 491;
    }

    @Override
    public LogMaker populate(LogMaker logMaker) {
        return super.populate(logMaker).addTaggedData(1311, Integer.valueOf(this.mController.getAutoModeRaw()));
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent("android.settings.NIGHT_DISPLAY_SETTINGS");
    }

    @Override
    protected void handleSetListening(boolean z) {
        this.mIsListening = z;
        if (z) {
            this.mController.setListener(this);
            refreshState();
        } else {
            this.mController.setListener((ColorDisplayController.Callback) null);
        }
    }

    @Override
    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_night_display_label);
    }

    public void onActivated(boolean z) {
        refreshState();
    }
}
