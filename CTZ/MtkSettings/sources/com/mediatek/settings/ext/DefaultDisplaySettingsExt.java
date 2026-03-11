package com.mediatek.settings.ext;

import android.content.Context;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;

public class DefaultDisplaySettingsExt implements IDisplaySettingsExt {
    private static final String KEY_CUSTOM_FONT_SIZE = "custom_font_size";
    private static final String TAG = "DefaultDisplaySettingsExt";
    private Context mContext;

    public DefaultDisplaySettingsExt(Context context) {
        this.mContext = context;
    }

    @Override
    public void addPreference(Context context, PreferenceScreen preferenceScreen) {
    }

    @Override
    public void removePreference(Context context, PreferenceScreen preferenceScreen) {
        preferenceScreen.removePreference(preferenceScreen.findPreference(KEY_CUSTOM_FONT_SIZE));
        Log.d(TAG, "removePreference KEY_CUSTOM_FONT_SIZE");
    }

    @Override
    public boolean isCustomPrefPresent() {
        return false;
    }

    @Override
    public String[] getFontEntries(String[] strArr) {
        return strArr;
    }

    @Override
    public String[] getFontEntryValues(String[] strArr) {
        return strArr;
    }
}
