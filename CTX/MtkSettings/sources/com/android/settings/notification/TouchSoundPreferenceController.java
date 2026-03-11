package com.android.settings.notification;

import android.content.Context;
import android.media.AudioManager;
import android.os.AsyncTask;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class TouchSoundPreferenceController extends SettingPrefController {
    public TouchSoundPreferenceController(Context context, SettingsPreferenceFragment settingsPreferenceFragment, Lifecycle lifecycle) {
        super(context, settingsPreferenceFragment, lifecycle);
        this.mPreference = new SettingPref(2, "touch_sounds", "sound_effects_enabled", 1, new int[0]) {
            @Override
            protected boolean setSetting(final Context context2, final int i) {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        AudioManager audioManager = (AudioManager) context2.getSystemService("audio");
                        if (i != 0) {
                            audioManager.loadSoundEffects();
                        } else {
                            audioManager.unloadSoundEffects();
                        }
                    }
                });
                return super.setSetting(context2, i);
            }
        };
    }

    @Override
    public boolean isAvailable() {
        return this.mContext.getResources().getBoolean(R.bool.config_show_touch_sounds);
    }
}
