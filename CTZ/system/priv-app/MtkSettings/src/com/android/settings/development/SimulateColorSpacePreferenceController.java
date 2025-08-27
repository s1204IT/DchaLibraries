package com.android.settings.development;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/* loaded from: classes.dex */
public class SimulateColorSpacePreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    static final int SETTING_VALUE_OFF = 0;
    static final int SETTING_VALUE_ON = 1;

    public SimulateColorSpacePreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "simulate_color_space";
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) throws NumberFormatException {
        writeSimulateColorSpace(obj);
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        updateSimulateColorSpace();
    }

    @Override // com.android.settingslib.development.DeveloperOptionsPreferenceController
    public void onDeveloperOptionsDisabled() throws NumberFormatException {
        super.onDeveloperOptionsDisabled();
        if (usingDevelopmentColorSpace()) {
            writeSimulateColorSpace(-1);
        }
    }

    private void updateSimulateColorSpace() {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        boolean z = Settings.Secure.getInt(contentResolver, "accessibility_display_daltonizer_enabled", 0) != 0;
        ListPreference listPreference = (ListPreference) this.mPreference;
        if (z) {
            String string = Integer.toString(Settings.Secure.getInt(contentResolver, "accessibility_display_daltonizer", -1));
            listPreference.setValue(string);
            if (listPreference.findIndexOfValue(string) < 0) {
                Resources resources = this.mContext.getResources();
                listPreference.setSummary(resources.getString(R.string.daltonizer_type_overridden, resources.getString(R.string.accessibility_display_daltonizer_preference_title)));
                return;
            } else {
                listPreference.setSummary("%s");
                return;
            }
        }
        listPreference.setValue(Integer.toString(-1));
    }

    private void writeSimulateColorSpace(Object obj) throws NumberFormatException {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        int i = Integer.parseInt(obj.toString());
        if (i < 0) {
            Settings.Secure.putInt(contentResolver, "accessibility_display_daltonizer_enabled", 0);
        } else {
            Settings.Secure.putInt(contentResolver, "accessibility_display_daltonizer_enabled", 1);
            Settings.Secure.putInt(contentResolver, "accessibility_display_daltonizer", i);
        }
    }

    private boolean usingDevelopmentColorSpace() {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        if (Settings.Secure.getInt(contentResolver, "accessibility_display_daltonizer_enabled", 0) != 0) {
            if (((ListPreference) this.mPreference).findIndexOfValue(Integer.toString(Settings.Secure.getInt(contentResolver, "accessibility_display_daltonizer", -1))) >= 0) {
                return true;
            }
        }
        return false;
    }
}
