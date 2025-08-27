package com.mediatek.settings.ext;

import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

/* loaded from: classes.dex */
public interface IAudioProfileExt {
    void addCustomizedPreference(PreferenceScreen preferenceScreen);

    void onAudioProfileSettingPaused(PreferenceFragment preferenceFragment);

    void onAudioProfileSettingResumed(PreferenceFragment preferenceFragment);

    boolean onPreferenceTreeClick(Preference preference);
}
