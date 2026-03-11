package com.mediatek.settings.ext;

import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

public class DefaultDeviceInfoSettingsExt implements IDeviceInfoSettingsExt {
    @Override
    public void addEpushPreference(PreferenceScreen preferenceScreen) {
    }

    @Override
    public void updateSummary(Preference preference, String str, String str2) {
    }

    @Override
    public String customeModelInfo(String str) {
        return str;
    }
}
