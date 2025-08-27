package com.android.settings.gestures;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.widget.VideoPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreate;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.core.lifecycle.events.OnSaveInstanceState;

/* loaded from: classes.dex */
public abstract class GesturePreferenceController extends TogglePreferenceController implements Preference.OnPreferenceChangeListener, LifecycleObserver, OnCreate, OnPause, OnResume, OnSaveInstanceState {
    static final String KEY_VIDEO_PAUSED = "key_video_paused";
    boolean mVideoPaused;
    private VideoPreference mVideoPreference;

    protected abstract String getVideoPrefKey();

    public GesturePreferenceController(Context context, String str) {
        super(context, str);
    }

    @Override // com.android.settings.core.BasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        if (isAvailable()) {
            this.mVideoPreference = (VideoPreference) preferenceScreen.findPreference(getVideoPrefKey());
        }
    }

    @Override // com.android.settings.core.TogglePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference != null) {
            preference.setEnabled(canHandleClicks());
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public CharSequence getSummary() {
        return this.mContext.getText(isChecked() ? R.string.gesture_setting_on : R.string.gesture_setting_off);
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnCreate
    public void onCreate(Bundle bundle) {
        if (bundle != null) {
            this.mVideoPaused = bundle.getBoolean(KEY_VIDEO_PAUSED, false);
        }
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnSaveInstanceState
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putBoolean(KEY_VIDEO_PAUSED, this.mVideoPaused);
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnPause
    public void onPause() throws IllegalStateException {
        if (this.mVideoPreference != null) {
            this.mVideoPaused = this.mVideoPreference.isVideoPaused();
            this.mVideoPreference.onViewInvisible();
        }
    }

    public void onResume() throws IllegalStateException {
        if (this.mVideoPreference != null) {
            this.mVideoPreference.onViewVisible(this.mVideoPaused);
        }
    }

    protected boolean canHandleClicks() {
        return true;
    }
}
