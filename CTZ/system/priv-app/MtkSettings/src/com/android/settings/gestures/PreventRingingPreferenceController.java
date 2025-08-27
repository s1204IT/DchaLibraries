package com.android.settings.gestures;

import android.R;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.VideoPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreate;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.core.lifecycle.events.OnSaveInstanceState;

/* loaded from: classes.dex */
public class PreventRingingPreferenceController extends BasePreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin, LifecycleObserver, OnCreate, OnPause, OnResume, OnSaveInstanceState {
    static final String KEY_VIDEO_PAUSED = "key_video_paused";
    private static final String PREF_KEY_VIDEO = "gesture_prevent_ringing_video";
    private final String SECURE_KEY;
    boolean mVideoPaused;
    private VideoPreference mVideoPreference;

    public PreventRingingPreferenceController(Context context, String str) {
        super(context, str);
        this.SECURE_KEY = "volume_hush_gesture";
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        return this.mContext.getResources().getBoolean(R.^attr-private.materialColorSurfaceDim) ? 0 : 2;
    }

    @Override // com.android.settings.core.BasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        if (isAvailable()) {
            this.mVideoPreference = (VideoPreference) preferenceScreen.findPreference(getVideoPrefKey());
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference != null && (preference instanceof ListPreference)) {
            ListPreference listPreference = (ListPreference) preference;
            int i = Settings.Secure.getInt(this.mContext.getContentResolver(), "volume_hush_gesture", 1);
            switch (i) {
                case 1:
                    listPreference.setValue(String.valueOf(i));
                    break;
                case 2:
                    listPreference.setValue(String.valueOf(i));
                    break;
                default:
                    listPreference.setValue(String.valueOf(0));
                    break;
            }
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public CharSequence getSummary() {
        int i;
        switch (Settings.Secure.getInt(this.mContext.getContentResolver(), "volume_hush_gesture", 1)) {
            case 1:
                i = com.android.settings.R.string.prevent_ringing_option_vibrate_summary;
                break;
            case 2:
                i = com.android.settings.R.string.prevent_ringing_option_mute_summary;
                break;
            default:
                i = com.android.settings.R.string.prevent_ringing_option_none_summary;
                break;
        }
        return this.mContext.getString(i);
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

    @Override // com.android.settingslib.core.lifecycle.events.OnResume
    public void onResume() throws IllegalStateException {
        if (this.mVideoPreference != null) {
            this.mVideoPreference.onViewVisible(this.mVideoPaused);
        }
    }

    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) throws NumberFormatException {
        Settings.Secure.putInt(this.mContext.getContentResolver(), "volume_hush_gesture", Integer.parseInt((String) obj));
        preference.setSummary(getSummary());
        return true;
    }
}
