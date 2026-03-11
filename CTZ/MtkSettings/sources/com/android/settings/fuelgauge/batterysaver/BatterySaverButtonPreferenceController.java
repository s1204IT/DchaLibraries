package com.android.settings.fuelgauge.batterysaver;

import android.content.Context;
import android.os.PowerManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.fuelgauge.BatterySaverReceiver;
import com.android.settings.widget.TwoStateButtonPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.fuelgauge.BatterySaverUtils;

public class BatterySaverButtonPreferenceController extends TogglePreferenceController implements BatterySaverReceiver.BatterySaverListener, LifecycleObserver, OnStart, OnStop {
    private final BatterySaverReceiver mBatterySaverReceiver;
    private final PowerManager mPowerManager;
    private TwoStateButtonPreference mPreference;

    public BatterySaverButtonPreferenceController(Context context, String str) {
        super(context, str);
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mBatterySaverReceiver = new BatterySaverReceiver(context);
        this.mBatterySaverReceiver.setBatterySaverListener(this);
    }

    @Override
    public int getAvailabilityStatus() {
        return 0;
    }

    @Override
    public boolean isSliceable() {
        return true;
    }

    @Override
    public void onStart() {
        this.mBatterySaverReceiver.setListening(true);
    }

    @Override
    public void onStop() {
        this.mBatterySaverReceiver.setListening(false);
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = (TwoStateButtonPreference) preferenceScreen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean isChecked() {
        return this.mPowerManager.isPowerSaveMode();
    }

    @Override
    public boolean setChecked(boolean z) {
        return BatterySaverUtils.setPowerSaveMode(this.mContext, z, false);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (this.mPreference != null) {
            this.mPreference.setChecked(isChecked());
        }
    }

    @Override
    public void onPowerSaveModeChanged() {
        boolean zIsChecked = isChecked();
        if (this.mPreference != null && this.mPreference.isChecked() != zIsChecked) {
            this.mPreference.setChecked(zIsChecked);
        }
    }

    @Override
    public void onBatteryChanged(boolean z) {
        if (this.mPreference != null) {
            this.mPreference.setButtonEnabled(!z);
        }
    }
}
