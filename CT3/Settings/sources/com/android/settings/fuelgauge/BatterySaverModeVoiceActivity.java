package com.android.settings.fuelgauge;

import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;
import com.android.settings.utils.VoiceSettingsActivity;

public class BatterySaverModeVoiceActivity extends VoiceSettingsActivity {
    @Override
    protected boolean onVoiceSettingInteraction(Intent intent) {
        if (intent.hasExtra("android.settings.extra.battery_saver_mode_enabled")) {
            PowerManager powerManager = (PowerManager) getSystemService("power");
            if (powerManager.setPowerSaveMode(intent.getBooleanExtra("android.settings.extra.battery_saver_mode_enabled", false))) {
                notifySuccess(null);
                return true;
            }
            Log.v("BatterySaverModeVoiceActivity", "Unable to set power mode");
            notifyFailure(null);
            return true;
        }
        Log.v("BatterySaverModeVoiceActivity", "Missing battery saver mode extra");
        return true;
    }
}
