package com.android.settings.display;

import android.content.Context;
import android.hardware.SensorManager;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class LiftToWakePreferenceController extends AbstractPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    public LiftToWakePreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        SensorManager sensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        return (sensorManager == null || sensorManager.getDefaultSensor(23) == null) ? false : true;
    }

    @Override
    public String getPreferenceKey() {
        return "lift_to_wake";
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        Settings.Secure.putInt(this.mContext.getContentResolver(), "wake_gesture_enabled", ((Boolean) obj).booleanValue() ? 1 : 0);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        ((SwitchPreference) preference).setChecked(Settings.Secure.getInt(this.mContext.getContentResolver(), "wake_gesture_enabled", 0) != 0);
    }
}
