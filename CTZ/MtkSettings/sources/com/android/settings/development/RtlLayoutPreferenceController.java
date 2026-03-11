package com.android.settings.development;

import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import com.android.internal.app.LocalePicker;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class RtlLayoutPreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    static final int SETTING_VALUE_OFF = 0;
    static final int SETTING_VALUE_ON = 1;

    public RtlLayoutPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return "force_rtl_layout_all_locales";
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        writeToForceRtlLayoutSetting(((Boolean) obj).booleanValue());
        updateLocales();
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        ((SwitchPreference) this.mPreference).setChecked(Settings.Global.getInt(this.mContext.getContentResolver(), "debug.force_rtl", 0) != 0);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        writeToForceRtlLayoutSetting(false);
        updateLocales();
        ((SwitchPreference) this.mPreference).setChecked(false);
    }

    void updateLocales() {
        LocalePicker.updateLocales(this.mContext.getResources().getConfiguration().getLocales());
    }

    private void writeToForceRtlLayoutSetting(boolean z) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "debug.force_rtl", z ? 1 : 0);
        SystemProperties.set("debug.force_rtl", z ? Integer.toString(1) : Integer.toString(0));
    }
}
