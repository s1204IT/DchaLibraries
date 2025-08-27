package com.android.settings.development;

import android.content.Context;
import android.os.SystemProperties;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import com.android.settingslib.development.SystemPropPoker;

/* loaded from: classes.dex */
public class ForceGpuRenderingPreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    static final String HARDWARE_UI_PROPERTY = "persist.sys.ui.hw";

    public ForceGpuRenderingPreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "force_hw_ui";
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        SystemProperties.set(HARDWARE_UI_PROPERTY, ((Boolean) obj).booleanValue() ? Boolean.toString(true) : Boolean.toString(false));
        SystemPropPoker.getInstance().poke();
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        ((SwitchPreference) this.mPreference).setChecked(SystemProperties.getBoolean(HARDWARE_UI_PROPERTY, false));
    }

    @Override // com.android.settingslib.development.DeveloperOptionsPreferenceController
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        SystemProperties.set(HARDWARE_UI_PROPERTY, Boolean.toString(false));
        ((SwitchPreference) this.mPreference).setChecked(false);
    }
}
