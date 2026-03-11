package com.android.settings.datetime;

import android.content.Context;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.TwoStatePreference;
import android.text.TextUtils;
import android.text.format.DateFormat;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import java.util.Locale;

public class AutoTimeFormatPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    public AutoTimeFormatPreferenceController(Context context, UpdateTimeAndDateCallback updateTimeAndDateCallback) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return "auto_24hour";
    }

    @Override
    public void updateState(Preference preference) {
        if (!(preference instanceof SwitchPreference)) {
            return;
        }
        ((SwitchPreference) preference).setChecked(isAutoTimeFormatSelection(this.mContext));
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        Boolean boolValueOf;
        if (!(preference instanceof TwoStatePreference) || !TextUtils.equals("auto_24hour", preference.getKey())) {
            return false;
        }
        if (((SwitchPreference) preference).isChecked()) {
            boolValueOf = null;
        } else {
            boolValueOf = Boolean.valueOf(is24HourLocale(this.mContext.getResources().getConfiguration().locale));
        }
        TimeFormatPreferenceController.update24HourFormat(this.mContext, boolValueOf);
        return true;
    }

    boolean is24HourLocale(Locale locale) {
        return DateFormat.is24HourLocale(locale);
    }

    static boolean isAutoTimeFormatSelection(Context context) {
        return Settings.System.getString(context.getContentResolver(), "time_12_24") == null;
    }
}
