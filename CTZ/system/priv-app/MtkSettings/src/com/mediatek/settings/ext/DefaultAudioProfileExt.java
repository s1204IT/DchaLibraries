package com.mediatek.settings.ext;

import android.content.Context;
import android.content.ContextWrapper;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

/* loaded from: classes.dex */
public class DefaultAudioProfileExt extends ContextWrapper implements IAudioProfileExt {
    public DefaultAudioProfileExt(Context context) {
        super(context);
    }

    @Override // com.mediatek.settings.ext.IAudioProfileExt
    public void addCustomizedPreference(PreferenceScreen preferenceScreen) {
    }

    @Override // com.mediatek.settings.ext.IAudioProfileExt
    public boolean onPreferenceTreeClick(Preference preference) {
        return false;
    }

    @Override // com.mediatek.settings.ext.IAudioProfileExt
    public void onAudioProfileSettingResumed(PreferenceFragment preferenceFragment) {
    }

    @Override // com.mediatek.settings.ext.IAudioProfileExt
    public void onAudioProfileSettingPaused(PreferenceFragment preferenceFragment) {
    }
}
