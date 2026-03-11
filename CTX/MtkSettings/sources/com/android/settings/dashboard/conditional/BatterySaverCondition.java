package com.android.settings.dashboard.conditional;

import android.graphics.drawable.Drawable;
import android.os.PowerManager;
import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.fuelgauge.BatterySaverReceiver;
import com.android.settings.fuelgauge.batterysaver.BatterySaverSettings;
import com.android.settingslib.fuelgauge.BatterySaverUtils;

public class BatterySaverCondition extends Condition implements BatterySaverReceiver.BatterySaverListener {
    private final BatterySaverReceiver mReceiver;

    public BatterySaverCondition(ConditionManager conditionManager) {
        super(conditionManager);
        this.mReceiver = new BatterySaverReceiver(conditionManager.getContext());
        this.mReceiver.setBatterySaverListener(this);
    }

    @Override
    public void refreshState() {
        setActive(((PowerManager) this.mManager.getContext().getSystemService(PowerManager.class)).isPowerSaveMode());
    }

    @Override
    public Drawable getIcon() {
        return this.mManager.getContext().getDrawable(R.drawable.ic_battery_saver_accent_24dp);
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
        new SubSettingLauncher(this.mManager.getContext()).setDestination(BatterySaverSettings.class.getName()).setSourceMetricsCategory(35).setTitle(R.string.battery_saver).addFlags(268435456).launch();
    }

    @Override
    public void onActionClick(int i) {
        if (i == 0) {
            BatterySaverUtils.setPowerSaveMode(this.mManager.getContext(), false, false);
            refreshState();
        } else {
            throw new IllegalArgumentException("Unexpected index " + i);
        }
    }

    @Override
    public int getMetricsConstant() {
        return 379;
    }

    @Override
    public void onResume() {
        this.mReceiver.setListening(true);
    }

    @Override
    public void onPause() {
        this.mReceiver.setListening(false);
    }

    @Override
    public void onPowerSaveModeChanged() {
        ((BatterySaverCondition) ConditionManager.get(this.mManager.getContext()).getCondition(BatterySaverCondition.class)).refreshState();
    }

    @Override
    public void onBatteryChanged(boolean z) {
    }
}
