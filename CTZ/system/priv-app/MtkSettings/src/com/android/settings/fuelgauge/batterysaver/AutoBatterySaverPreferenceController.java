package com.android.settings.fuelgauge.batterysaver;

import android.R;
import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.fuelgauge.BatterySaverUtils;

/* loaded from: classes.dex */
public class AutoBatterySaverPreferenceController extends TogglePreferenceController implements Preference.OnPreferenceChangeListener {
    static final int DEFAULT_TRIGGER_LEVEL = 0;
    static final String KEY_AUTO_BATTERY_SAVER = "auto_battery_saver";
    private final int mDefaultTriggerLevelForOn;

    public AutoBatterySaverPreferenceController(Context context) {
        super(context, KEY_AUTO_BATTERY_SAVER);
        this.mDefaultTriggerLevelForOn = this.mContext.getResources().getInteger(R.integer.config_defaultNotificationLedOff);
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        return 0;
    }

    @Override // com.android.settings.core.TogglePreferenceController
    public boolean isChecked() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "low_power_trigger_level", 0) != 0;
    }

    @Override // com.android.settings.core.TogglePreferenceController
    public boolean setChecked(boolean z) {
        BatterySaverUtils.setAutoBatterySaverTriggerLevel(this.mContext, z ? this.mDefaultTriggerLevelForOn : 0);
        return true;
    }
}
