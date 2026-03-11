package com.mediatek.settings.ext;

import android.content.Context;
import android.os.storage.VolumeInfo;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;

public interface IStorageSettingsExt {
    void initCustomizationStoragePlugin(Context context);

    void updateCustomizedPrefDetails(VolumeInfo volumeInfo);

    void updateCustomizedPrivateSettingsPlugin(PreferenceScreen preferenceScreen, VolumeInfo volumeInfo);

    void updateCustomizedStorageSettingsPlugin(PreferenceCategory preferenceCategory);

    void updateCustomizedStorageSummary(Object obj, Object obj2);
}
