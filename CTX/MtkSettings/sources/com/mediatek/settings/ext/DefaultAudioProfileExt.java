package com.mediatek.settings.ext;

import android.content.Context;
import android.content.ContextWrapper;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

public class DefaultAudioProfileExt extends ContextWrapper implements IAudioProfileExt {
    public DefaultAudioProfileExt(Context context) {
        super(context);
    }

    @Override
    public void addCustomizedPreference(PreferenceScreen preferenceScreen) {
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        return false;
    }

    @Override
    public void onAudioProfileSettingResumed(PreferenceFragment preferenceFragment) {
    }

    @Override
    public void onAudioProfileSettingPaused(PreferenceFragment preferenceFragment) {
    }
}
