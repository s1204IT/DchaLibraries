package com.android.settings.development;

import android.content.Context;
import android.os.SystemProperties;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import com.android.settingslib.development.SystemPropPoker;

/* loaded from: classes.dex */
public class HardwareLayersUpdatesPreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    public HardwareLayersUpdatesPreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "show_hw_layers_updates";
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        SystemProperties.set("debug.hwui.show_layers_updates", ((Boolean) obj).booleanValue() ? "true" : null);
        SystemPropPoker.getInstance().poke();
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        ((SwitchPreference) this.mPreference).setChecked(SystemProperties.getBoolean("debug.hwui.show_layers_updates", false));
    }

    @Override // com.android.settingslib.development.DeveloperOptionsPreferenceController
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        SystemProperties.set("debug.hwui.show_layers_updates", (String) null);
        ((SwitchPreference) this.mPreference).setChecked(false);
    }
}
