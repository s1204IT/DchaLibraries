package com.android.settings.datetime;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.util.FeatureFlagUtils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.datetime.timezone.TimeZoneSettings;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.datetime.ZoneGetter;
import java.util.Calendar;

public class TimeZonePreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private final AutoTimeZonePreferenceController mAutoTimeZonePreferenceController;
    private final boolean mZonePickerV2;

    public TimeZonePreferenceController(Context context, AutoTimeZonePreferenceController autoTimeZonePreferenceController) {
        super(context);
        this.mAutoTimeZonePreferenceController = autoTimeZonePreferenceController;
        this.mZonePickerV2 = FeatureFlagUtils.isEnabled(this.mContext, "settings_zone_picker_v2");
    }

    @Override
    public void updateState(Preference preference) {
        if (!(preference instanceof RestrictedPreference)) {
            return;
        }
        if (this.mZonePickerV2) {
            preference.setFragment(TimeZoneSettings.class.getName());
        }
        preference.setSummary(getTimeZoneOffsetAndName());
        if (!((RestrictedPreference) preference).isDisabledByAdmin()) {
            preference.setEnabled(!this.mAutoTimeZonePreferenceController.isEnabled());
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return "timezone";
    }

    CharSequence getTimeZoneOffsetAndName() {
        Calendar calendar = Calendar.getInstance();
        return ZoneGetter.getTimeZoneOffsetAndName(this.mContext, calendar.getTimeZone(), calendar.getTime());
    }
}
