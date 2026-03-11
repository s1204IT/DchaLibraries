package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.widget.Switch;
import com.android.settingslib.graph.BatteryMeterDrawableBase;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.BatteryController;

public class BatterySaverTile extends QSTileImpl<QSTile.BooleanState> implements BatteryController.BatteryStateChangeCallback {
    private final BatteryController mBatteryController;
    private boolean mCharging;
    private int mLevel;
    private boolean mPluggedIn;
    private boolean mPowerSave;

    public BatterySaverTile(QSHost qSHost) {
        super(qSHost);
        this.mBatteryController = (BatteryController) Dependency.get(BatteryController.class);
    }

    @Override
    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    @Override
    public int getMetricsCategory() {
        return 261;
    }

    @Override
    public void handleSetListening(boolean z) {
        if (z) {
            this.mBatteryController.addCallback(this);
        } else {
            this.mBatteryController.removeCallback(this);
        }
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent("android.intent.action.POWER_USAGE_SUMMARY");
    }

    @Override
    protected void handleClick() {
        this.mBatteryController.setPowerSaveMode(!this.mPowerSave);
    }

    @Override
    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.battery_detail_switch_title);
    }

    @Override
    public void handleUpdateState(QSTile.BooleanState booleanState, Object obj) {
        int i;
        if (this.mPluggedIn) {
            i = 0;
        } else {
            i = this.mPowerSave ? 2 : 1;
        }
        booleanState.state = i;
        BatterySaverIcon batterySaverIcon = new BatterySaverIcon();
        batterySaverIcon.mState = booleanState.state;
        booleanState.icon = batterySaverIcon;
        booleanState.label = this.mContext.getString(R.string.battery_detail_switch_title);
        booleanState.contentDescription = booleanState.label;
        booleanState.value = this.mPowerSave;
        booleanState.expandedAccessibilityClassName = Switch.class.getName();
    }

    @Override
    public void onBatteryLevelChanged(int i, boolean z, boolean z2) {
        this.mLevel = i;
        this.mPluggedIn = z;
        this.mCharging = z2;
        refreshState(Integer.valueOf(i));
    }

    @Override
    public void onPowerSaveChanged(boolean z) {
        this.mPowerSave = z;
        refreshState(null);
    }

    public static class BatterySaverIcon extends QSTile.Icon {
        private int mState;

        @Override
        public Drawable getDrawable(Context context) {
            BatterySaverDrawable batterySaverDrawable = new BatterySaverDrawable(context, QSTileImpl.getColorForState(context, this.mState));
            batterySaverDrawable.mState = this.mState;
            int dimensionPixelSize = context.getResources().getDimensionPixelSize(R.dimen.qs_tile_divider_height);
            batterySaverDrawable.setPadding(dimensionPixelSize, dimensionPixelSize, dimensionPixelSize, dimensionPixelSize);
            return batterySaverDrawable;
        }
    }

    private static class BatterySaverDrawable extends BatteryMeterDrawableBase {
        private int mState;

        BatterySaverDrawable(Context context, int i) {
            super(context, i);
            super.setBatteryLevel(100);
            setPowerSave(true);
            setCharging(false);
            setPowerSaveAsColorError(false);
            this.mPowerSaveAsColorError = true;
            this.mFramePaint.setColor(0);
            this.mPowersavePaint.setColor(i);
            this.mFramePaint.setStrokeWidth(this.mPowersavePaint.getStrokeWidth());
            this.mPlusPaint.setColor(i);
        }

        @Override
        protected int batteryColorForLevel(int i) {
            return 0;
        }

        @Override
        public void setBatteryLevel(int i) {
        }
    }
}
