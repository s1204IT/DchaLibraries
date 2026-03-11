package com.android.settings.display;

import android.content.Context;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class ShowOperatorNamePreferenceController extends AbstractPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    public ShowOperatorNamePreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return this.mContext.getResources().getBoolean(R.bool.config_showOperatorNameInStatusBar);
    }

    @Override
    public String getPreferenceKey() {
        return "show_operator_name";
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        Settings.Secure.putInt(this.mContext.getContentResolver(), "show_operator_name", ((Boolean) obj).booleanValue() ? 1 : 0);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        ((SwitchPreference) preference).setChecked(Settings.Secure.getInt(this.mContext.getContentResolver(), "show_operator_name", 1) != 0);
    }
}
