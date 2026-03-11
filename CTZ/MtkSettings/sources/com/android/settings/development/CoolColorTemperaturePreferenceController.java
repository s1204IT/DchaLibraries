package com.android.settings.development;

import android.content.Context;
import android.os.SystemProperties;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.widget.Toast;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import com.android.settingslib.development.SystemPropPoker;

public class CoolColorTemperaturePreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    static final String COLOR_TEMPERATURE_PROPERTY = "persist.sys.debug.color_temp";

    public CoolColorTemperaturePreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return this.mContext.getResources().getBoolean(R.bool.config_enableColorTemperature);
    }

    @Override
    public String getPreferenceKey() {
        return "color_temperature";
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        SystemProperties.set(COLOR_TEMPERATURE_PROPERTY, Boolean.toString(((Boolean) obj).booleanValue()));
        SystemPropPoker.getInstance().poke();
        displayColorTemperatureToast();
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        ((SwitchPreference) this.mPreference).setChecked(SystemProperties.getBoolean(COLOR_TEMPERATURE_PROPERTY, false));
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        SystemProperties.set(COLOR_TEMPERATURE_PROPERTY, Boolean.toString(false));
        ((SwitchPreference) this.mPreference).setChecked(false);
    }

    void displayColorTemperatureToast() {
        Toast.makeText(this.mContext, R.string.color_temperature_toast, 1).show();
    }
}
