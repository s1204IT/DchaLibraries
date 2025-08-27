package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.widget.Switch;
import com.android.settingslib.graph.BatteryMeterDrawableBase;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.BatteryController;

/* loaded from: classes.dex */
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

    /* JADX DEBUG: Method merged with bridge method: newTileState()Lcom/android/systemui/plugins/qs/QSTile$State; */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl, com.android.systemui.plugins.qs.QSTile
    public int getMetricsCategory() {
        return 261;
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public void handleSetListening(boolean z) {
        if (z) {
            this.mBatteryController.addCallback(this);
        } else {
            this.mBatteryController.removeCallback(this);
        }
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    public Intent getLongClickIntent() {
        return new Intent("android.intent.action.POWER_USAGE_SUMMARY");
    }

    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleClick() {
        this.mBatteryController.setPowerSaveMode(!this.mPowerSave);
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.battery_detail_switch_title);
    }

    /* JADX DEBUG: Method merged with bridge method: handleUpdateState(Lcom/android/systemui/plugins/qs/QSTile$State;Ljava/lang/Object;)V */
    @Override // com.android.systemui.qs.tileimpl.QSTileImpl
    protected void handleUpdateState(QSTile.BooleanState booleanState, Object obj) {
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

    @Override // com.android.systemui.statusbar.policy.BatteryController.BatteryStateChangeCallback
    public void onBatteryLevelChanged(int i, boolean z, boolean z2) {
        this.mLevel = i;
        this.mPluggedIn = z;
        this.mCharging = z2;
        refreshState(Integer.valueOf(i));
    }

    @Override // com.android.systemui.statusbar.policy.BatteryController.BatteryStateChangeCallback
    public void onPowerSaveChanged(boolean z) {
        this.mPowerSave = z;
        refreshState(null);
    }

    public static class BatterySaverIcon extends QSTile.Icon {
        private int mState;

        @Override // com.android.systemui.plugins.qs.QSTile.Icon
        public Drawable getDrawable(Context context) throws Resources.NotFoundException {
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

        @Override // com.android.settingslib.graph.BatteryMeterDrawableBase
        protected int batteryColorForLevel(int i) {
            return 0;
        }

        @Override // com.android.settingslib.graph.BatteryMeterDrawableBase
        public void setBatteryLevel(int i) {
        }
    }
}
