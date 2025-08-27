package com.android.settings.datetime;

import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.AbstractPreferenceController;

/* loaded from: classes.dex */
public class AutoTimePreferenceController extends AbstractPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    private final UpdateTimeAndDateCallback mCallback;

    public AutoTimePreferenceController(Context context, UpdateTimeAndDateCallback updateTimeAndDateCallback) {
        super(context);
        this.mCallback = updateTimeAndDateCallback;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        if (!(preference instanceof RestrictedSwitchPreference)) {
            return;
        }
        RestrictedSwitchPreference restrictedSwitchPreference = (RestrictedSwitchPreference) preference;
        if (!restrictedSwitchPreference.isDisabledByAdmin()) {
            restrictedSwitchPreference.setDisabledByAdmin(getEnforcedAdminProperty());
        }
        restrictedSwitchPreference.setChecked(isEnabled());
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "auto_time";
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "auto_time", ((Boolean) obj).booleanValue() ? 1 : 0);
        this.mCallback.updateTimeAndDateDisplay(this.mContext);
        return true;
    }

    public boolean isEnabled() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "auto_time", 0) > 0;
    }

    private RestrictedLockUtils.EnforcedAdmin getEnforcedAdminProperty() {
        return RestrictedLockUtils.checkIfAutoTimeRequired(this.mContext);
    }
}
