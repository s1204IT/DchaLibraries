package com.android.settings.fuelgauge;

import android.content.Intent;
import android.util.Log;
import com.android.settings.utils.VoiceSettingsActivity;
import com.android.settingslib.fuelgauge.BatterySaverUtils;

/* loaded from: classes.dex */
public class BatterySaverModeVoiceActivity extends VoiceSettingsActivity {
    @Override // com.android.settings.utils.VoiceSettingsActivity
    protected boolean onVoiceSettingInteraction(Intent intent) {
        if (intent.hasExtra("android.settings.extra.battery_saver_mode_enabled")) {
            if (BatterySaverUtils.setPowerSaveMode(this, intent.getBooleanExtra("android.settings.extra.battery_saver_mode_enabled", false), true)) {
                notifySuccess(null);
            } else {
                Log.v("BatterySaverModeVoiceActivity", "Unable to set power mode");
                notifyFailure(null);
            }
        } else {
            Log.v("BatterySaverModeVoiceActivity", "Missing battery saver mode extra");
        }
        return true;
    }
}
