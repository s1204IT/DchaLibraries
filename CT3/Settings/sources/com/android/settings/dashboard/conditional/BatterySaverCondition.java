package com.android.settings.dashboard.conditional;

import android.graphics.drawable.Icon;
import android.os.PowerManager;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.fuelgauge.BatterySaverSettings;

public class BatterySaverCondition extends Condition {
    public BatterySaverCondition(ConditionManager manager) {
        super(manager);
    }

    @Override
    public void refreshState() {
        PowerManager powerManager = (PowerManager) this.mManager.getContext().getSystemService(PowerManager.class);
        setActive(powerManager.isPowerSaveMode());
    }

    @Override
    public Icon getIcon() {
        return Icon.createWithResource(this.mManager.getContext(), R.drawable.ic_settings_battery);
    }

    @Override
    public CharSequence getTitle() {
        return this.mManager.getContext().getString(R.string.condition_battery_title);
    }

    @Override
    public CharSequence getSummary() {
        return this.mManager.getContext().getString(R.string.condition_battery_summary);
    }

    @Override
    public CharSequence[] getActions() {
        return new CharSequence[]{this.mManager.getContext().getString(R.string.condition_turn_off)};
    }

    @Override
    public void onPrimaryClick() {
        Utils.startWithFragment(this.mManager.getContext(), BatterySaverSettings.class.getName(), null, null, 0, R.string.battery_saver, null);
    }

    @Override
    public void onActionClick(int index) {
        if (index == 0) {
            ((PowerManager) this.mManager.getContext().getSystemService(PowerManager.class)).setPowerSaveMode(false);
            refreshState();
            return;
        }
        throw new IllegalArgumentException("Unexpected index " + index);
    }

    @Override
    public int getMetricsConstant() {
        return 379;
    }
}
