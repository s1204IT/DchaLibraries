package com.android.settings.datetime;

import android.content.Context;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.Utils;
import com.android.settingslib.core.AbstractPreferenceController;

public class AutoTimeZonePreferenceController extends AbstractPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    private final UpdateTimeAndDateCallback mCallback;
    private final boolean mIsFromSUW;

    public AutoTimeZonePreferenceController(Context context, UpdateTimeAndDateCallback updateTimeAndDateCallback, boolean z) {
        super(context);
        this.mCallback = updateTimeAndDateCallback;
        this.mIsFromSUW = z;
    }

    @Override
    public boolean isAvailable() {
        return (Utils.isWifiOnly(this.mContext) || this.mIsFromSUW) ? false : true;
    }

    @Override
    public String getPreferenceKey() {
        return "auto_zone";
    }

    @Override
    public void updateState(Preference preference) {
        if (!(preference instanceof SwitchPreference)) {
            return;
        }
        ((SwitchPreference) preference).setChecked(isEnabled());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "auto_time_zone", ((Boolean) obj).booleanValue() ? 1 : 0);
        this.mCallback.updateTimeAndDateDisplay(this.mContext);
        return true;
    }

    public boolean isEnabled() {
        return isAvailable() && Settings.Global.getInt(this.mContext.getContentResolver(), "auto_time_zone", 0) > 0;
    }
}
