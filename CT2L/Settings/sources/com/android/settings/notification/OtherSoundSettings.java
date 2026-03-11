package com.android.settings.notification;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.SearchIndexableResource;
import android.telephony.TelephonyManager;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OtherSoundSettings extends SettingsPreferenceFragment implements Indexable {
    private static final SettingPref[] PREFS;
    private static final SettingPref PREF_DIAL_PAD_TONES;
    private static final SettingPref PREF_DOCKING_SOUNDS;
    private static final SettingPref PREF_DOCK_AUDIO_MEDIA;
    private static final SettingPref PREF_EMERGENCY_TONE;
    private static final SettingPref PREF_TOUCH_SOUNDS;
    private static final SettingPref PREF_VIBRATE_ON_TOUCH;
    private Context mContext;
    private final SettingsObserver mSettingsObserver = new SettingsObserver();
    private static final SettingPref PREF_SCREEN_LOCKING_SOUNDS = new SettingPref(2, "screen_locking_sounds", "lockscreen_sounds_enabled", 1, new int[0]);
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
            SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.other_sound_settings;
            return Arrays.asList(sir);
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            ArrayList<String> rt = new ArrayList<>();
            SettingPref[] arr$ = OtherSoundSettings.PREFS;
            for (SettingPref pref : arr$) {
                if (!pref.isApplicable(context)) {
                    rt.add(pref.getKey());
                }
            }
            return rt;
        }
    };

    static {
        int i = 2;
        int i2 = 0;
        int i3 = 1;
        PREF_DIAL_PAD_TONES = new SettingPref(i, "dial_pad_tones", "dtmf_tone", i3, new int[0]) {
            @Override
            public boolean isApplicable(Context context) {
                return Utils.isVoiceCapable(context);
            }
        };
        PREF_DOCKING_SOUNDS = new SettingPref(i3, "docking_sounds", "dock_sounds_enabled", i3, new int[0]) {
            @Override
            public boolean isApplicable(Context context) {
                return OtherSoundSettings.hasDockSettings(context);
            }
        };
        PREF_TOUCH_SOUNDS = new SettingPref(i, "touch_sounds", "sound_effects_enabled", i3, new int[0]) {
            @Override
            protected boolean setSetting(Context context, int value) {
                AudioManager am = (AudioManager) context.getSystemService("audio");
                if (value != 0) {
                    am.loadSoundEffects();
                } else {
                    am.unloadSoundEffects();
                }
                return super.setSetting(context, value);
            }
        };
        PREF_VIBRATE_ON_TOUCH = new SettingPref(i, "vibrate_on_touch", "haptic_feedback_enabled", i3, new int[0]) {
            @Override
            public boolean isApplicable(Context context) {
                return OtherSoundSettings.hasHaptic(context);
            }
        };
        PREF_DOCK_AUDIO_MEDIA = new SettingPref(i3, "dock_audio_media", "dock_audio_media_enabled", i2, 0, 1) {
            @Override
            public boolean isApplicable(Context context) {
                return OtherSoundSettings.hasDockSettings(context);
            }

            @Override
            protected String getCaption(Resources res, int value) {
                switch (value) {
                    case 0:
                        return res.getString(R.string.dock_audio_media_disabled);
                    case 1:
                        return res.getString(R.string.dock_audio_media_enabled);
                    default:
                        throw new IllegalArgumentException();
                }
            }
        };
        PREF_EMERGENCY_TONE = new SettingPref(i3, "emergency_tone", "emergency_tone", i2, 1, 2, 0) {
            @Override
            public boolean isApplicable(Context context) {
                int activePhoneType = TelephonyManager.getDefault().getCurrentPhoneType();
                return activePhoneType == 2;
            }

            @Override
            protected String getCaption(Resources res, int value) {
                switch (value) {
                    case 0:
                        return res.getString(R.string.emergency_tone_silent);
                    case 1:
                        return res.getString(R.string.emergency_tone_alert);
                    case 2:
                        return res.getString(R.string.emergency_tone_vibrate);
                    default:
                        throw new IllegalArgumentException();
                }
            }
        };
        PREFS = new SettingPref[]{PREF_DIAL_PAD_TONES, PREF_SCREEN_LOCKING_SOUNDS, PREF_DOCKING_SOUNDS, PREF_TOUCH_SOUNDS, PREF_VIBRATE_ON_TOUCH, PREF_DOCK_AUDIO_MEDIA, PREF_EMERGENCY_TONE};
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.other_sound_settings);
        this.mContext = getActivity();
        SettingPref[] arr$ = PREFS;
        for (SettingPref pref : arr$) {
            pref.init(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mSettingsObserver.register(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mSettingsObserver.register(false);
    }

    public static boolean hasDockSettings(Context context) {
        return context.getResources().getBoolean(R.bool.has_dock_settings);
    }

    public static boolean hasHaptic(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService("vibrator");
        return vibrator != null && vibrator.hasVibrator();
    }

    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver() {
            super(new Handler());
        }

        public void register(boolean register) {
            ContentResolver cr = OtherSoundSettings.this.getContentResolver();
            if (register) {
                SettingPref[] arr$ = OtherSoundSettings.PREFS;
                for (SettingPref pref : arr$) {
                    cr.registerContentObserver(pref.getUri(), false, this);
                }
                return;
            }
            cr.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            SettingPref[] arr$ = OtherSoundSettings.PREFS;
            for (SettingPref pref : arr$) {
                if (pref.getUri().equals(uri)) {
                    pref.update(OtherSoundSettings.this.mContext);
                    return;
                }
            }
        }
    }
}
