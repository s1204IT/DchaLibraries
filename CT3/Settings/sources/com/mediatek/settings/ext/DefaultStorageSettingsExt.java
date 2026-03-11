package com.mediatek.settings.ext;

import android.content.Context;
import android.os.storage.VolumeInfo;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;

public class DefaultStorageSettingsExt implements IStorageSettingsExt {
    @Override
    public void initCustomizationStoragePlugin(Context context) {
    }

    @Override
    public void updateCustomizedStorageSettingsPlugin(PreferenceCategory prefcategory) {
    }

    @Override
    public void updateCustomizedPrivateSettingsPlugin(PreferenceScreen screen, VolumeInfo vol) {
    }

    @Override
    public void updateCustomizedPrefDetails(VolumeInfo vol) {
    }

    @Override
    public void updateCustomizedStorageSummary(Object summaryProvider, Object summaryLoader) {
    }
}
