package com.mediatek.settings.ext;

import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

public interface IDeviceInfoSettingsExt {
    void addEpushPreference(PreferenceScreen preferenceScreen);

    String customeModelInfo(String str);

    void updateSummary(Preference preference, String str, String str2);
}
