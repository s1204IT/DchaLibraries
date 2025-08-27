package com.mediatek.settings.ext;

import android.content.Context;
import android.support.v7.preference.PreferenceScreen;

/* loaded from: classes.dex */
public interface IDisplaySettingsExt {
    void addPreference(Context context, PreferenceScreen preferenceScreen);

    String[] getFontEntries(String[] strArr);

    String[] getFontEntryValues(String[] strArr);

    boolean isCustomPrefPresent();

    void removePreference(Context context, PreferenceScreen preferenceScreen);
}
