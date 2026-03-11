package com.android.settings.development;

import android.content.Context;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.AbstractLogdSizePreferenceController;

public class LogdSizePreferenceController extends AbstractLogdSizePreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    public LogdSizePreferenceController(Context context) {
        super(context);
    }

    @Override
    public void updateState(Preference preference) {
        updateLogdSizeValues();
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        writeLogdSizeOption(null);
    }
}
