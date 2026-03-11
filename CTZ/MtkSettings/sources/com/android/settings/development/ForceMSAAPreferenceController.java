package com.android.settings.development;

import android.content.Context;
import android.os.SystemProperties;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import com.android.settingslib.development.SystemPropPoker;

public class ForceMSAAPreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    static final String MSAA_PROPERTY = "debug.egl.force_msaa";

    public ForceMSAAPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return "force_msaa";
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        SystemProperties.set(MSAA_PROPERTY, ((Boolean) obj).booleanValue() ? Boolean.toString(true) : Boolean.toString(false));
        SystemPropPoker.getInstance().poke();
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        ((SwitchPreference) this.mPreference).setChecked(SystemProperties.getBoolean(MSAA_PROPERTY, false));
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        SystemProperties.set(MSAA_PROPERTY, Boolean.toString(false));
        ((SwitchPreference) this.mPreference).setChecked(false);
    }
}
