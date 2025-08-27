package com.android.settings.notification;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.support.v7.preference.PreferenceScreen;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.notification.VolumeSeekBarPreference;

/* loaded from: classes.dex */
public abstract class VolumeSeekBarPreferenceController extends AdjustVolumeRestrictedPreferenceController implements LifecycleObserver {
    protected AudioHelper mHelper;
    protected VolumeSeekBarPreference mPreference;
    protected VolumeSeekBarPreference.Callback mVolumePreferenceCallback;

    protected abstract int getAudioStream();

    protected abstract int getMuteIcon();

    public VolumeSeekBarPreferenceController(Context context, String str) {
        super(context, str);
        setAudioHelper(new AudioHelper(context));
    }

    @VisibleForTesting
    void setAudioHelper(AudioHelper audioHelper) {
        this.mHelper = audioHelper;
    }

    public void setCallback(VolumeSeekBarPreference.Callback callback) {
        this.mVolumePreferenceCallback = callback;
    }

    @Override // com.android.settings.core.BasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        if (isAvailable()) {
            this.mPreference = (VolumeSeekBarPreference) preferenceScreen.findPreference(getPreferenceKey());
            this.mPreference.setCallback(this.mVolumePreferenceCallback);
            this.mPreference.setStream(getAudioStream());
            this.mPreference.setMuteIcon(getMuteIcon());
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        if (this.mPreference != null) {
            this.mPreference.onActivityResume();
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        if (this.mPreference != null) {
            this.mPreference.onActivityPause();
        }
    }

    @Override // com.android.settings.core.SliderPreferenceController
    public int getSliderPosition() {
        if (this.mPreference != null) {
            return this.mPreference.getProgress();
        }
        return this.mHelper.getStreamVolume(getAudioStream());
    }

    @Override // com.android.settings.core.SliderPreferenceController
    public boolean setSliderPosition(int i) {
        if (this.mPreference != null) {
            this.mPreference.setProgress(i);
        }
        return this.mHelper.setStreamVolume(getAudioStream(), i);
    }

    @Override // com.android.settings.core.SliderPreferenceController
    public int getMaxSteps() {
        if (this.mPreference != null) {
            return this.mPreference.getMax();
        }
        return this.mHelper.getMaxVolume(getAudioStream());
    }
}
