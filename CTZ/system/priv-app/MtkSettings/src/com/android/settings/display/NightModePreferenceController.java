package com.android.settings.display;

import android.app.UiModeManager;
import android.content.Context;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

/* loaded from: classes.dex */
public class NightModePreferenceController extends AbstractPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    public NightModePreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return false;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "night_mode";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        if (!isAvailable()) {
            setVisible(preferenceScreen, "night_mode", false);
            return;
        }
        ListPreference listPreference = (ListPreference) preferenceScreen.findPreference("night_mode");
        if (listPreference != null) {
            listPreference.setValue(String.valueOf(((UiModeManager) this.mContext.getSystemService("uimode")).getNightMode()));
            listPreference.setOnPreferenceChangeListener(this);
        }
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) throws NumberFormatException {
        try {
            ((UiModeManager) this.mContext.getSystemService("uimode")).setNightMode(Integer.parseInt((String) obj));
            return true;
        } catch (NumberFormatException e) {
            Log.e("NightModePrefContr", "could not persist night mode setting", e);
            return false;
        }
    }
}
