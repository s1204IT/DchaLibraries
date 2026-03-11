package com.mediatek.audioprofile;

import android.content.Context;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.mediatek.settings.FeatureOption;
import java.util.ArrayList;
import java.util.List;

public class SoundEnhancement extends SettingsPreferenceFragment implements Indexable {
    public static final String ANC_UI_STATUS_DISABLED = "ANC_UI=off";
    public static final String ANC_UI_STATUS_ENABLED = "ANC_UI=on";
    protected static final int BESSURROUND_MODE_MOVIE = 0;
    protected static final int BESSURROUND_MODE_MUSIC = 1;
    protected static final String BESSURROUND_MOVIE = "BesSurround_Mode=0";
    protected static final String BESSURROUND_MUSIC = "BesSurround_Mode=1";
    protected static final String BESSURROUND_OFF = "BesSurround_OnOff=0";
    protected static final String BESSURROUND_ON = "BesSurround_OnOff=1";
    public static final String GET_ANC_UI_STATUS = "ANC_UI";
    private static final String GET_BESLOUDNESS_STATUS = "GetBesLoudnessStatus";
    private static final String GET_BESLOUDNESS_STATUS_ENABLED = "GetBesLoudnessStatus=1";
    protected static final String GET_BESSURROUND_MODE = "BesSurround_Mode";
    protected static final String GET_BESSURROUND_STATE = "BesSurround_OnOff";
    private static final String GET_MUSIC_PLUS_STATUS = "GetMusicPlusStatus";
    private static final String GET_MUSIC_PLUS_STATUS_ENABLED = "GetMusicPlusStatus=1";
    private static final String KEY_ANC = "anc_switch";
    private static final String KEY_BESLOUDNESS = "bes_loudness";
    private static final String KEY_BESSURROUND = "bes_surround";
    private static final String KEY_MUSIC_PLUS = "music_plus";
    private static final String KEY_SOUND_ENAHCNE = "sound_enhance";
    private static final String MTK_AUDENH_SUPPORT_State = "MTK_AUDENH_SUPPORT";
    private static final String MTK_AUDENH_SUPPORT_off = "MTK_AUDENH_SUPPORT=false";
    private static final String MTK_AUDENH_SUPPORT_on = "MTK_AUDENH_SUPPORT=true";
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> result = new ArrayList<>();
            Resources res = context.getResources();
            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.title = res.getString(R.string.sound_enhancement_title);
            data.screenTitle = res.getString(R.string.sound_enhancement_title);
            data.keywords = res.getString(R.string.sound_enhancement_title);
            result.add(data);
            return result;
        }
    };
    private static final String SET_BESLOUDNESS_DISABLED = "SetBesLoudnessStatus=0";
    private static final String SET_BESLOUDNESS_ENABLED = "SetBesLoudnessStatus=1";
    private static final String SET_MUSIC_PLUS_DISABLED = "SetMusicPlusStatus=0";
    private static final String SET_MUSIC_PLUS_ENABLED = "SetMusicPlusStatus=1";
    private static final int SOUND_PREFERENCE_NULL_COUNT = 0;
    private static final String TAG = "SoundEnhancement";
    private SwitchPreference mAncPref;
    private SwitchPreference mBesLoudnessPref;
    private Preference mBesSurroundPref;
    private Context mContext;
    private SwitchPreference mMusicPlusPrf;
    private AudioManager mAudioManager = null;
    private String mAudenhState = null;

    @Override
    public void onCreate(Bundle icicle) {
        Log.d("@M_SoundEnhancement", "onCreate");
        super.onCreate(icicle);
        this.mContext = getActivity();
        this.mAudioManager = (AudioManager) getSystemService("audio");
        this.mAudenhState = this.mAudioManager.getParameters(MTK_AUDENH_SUPPORT_State);
        Log.d("@M_SoundEnhancement", "AudENH state: " + this.mAudenhState);
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.audioprofile_sound_enhancement);
        this.mMusicPlusPrf = (SwitchPreference) findPreference(KEY_MUSIC_PLUS);
        this.mBesLoudnessPref = (SwitchPreference) findPreference(KEY_BESLOUDNESS);
        this.mBesSurroundPref = findPreference(KEY_BESSURROUND);
        this.mAncPref = (SwitchPreference) findPreference(KEY_ANC);
        if (!this.mAudenhState.equalsIgnoreCase(MTK_AUDENH_SUPPORT_on)) {
            Log.d("@M_SoundEnhancement", "remove audio enhance preference " + this.mMusicPlusPrf);
            getPreferenceScreen().removePreference(this.mMusicPlusPrf);
        }
        if (!FeatureOption.MTK_BESLOUDNESS_SUPPORT) {
            Log.d("@M_SoundEnhancement", "feature option is off, remove BesLoudness preference");
            getPreferenceScreen().removePreference(this.mBesLoudnessPref);
        }
        if (!FeatureOption.MTK_BESSURROUND_SUPPORT) {
            Log.d("@M_SoundEnhancement", "remove BesSurround preference " + this.mBesSurroundPref);
            getPreferenceScreen().removePreference(this.mBesSurroundPref);
        }
        if (!FeatureOption.MTK_ANC_SUPPORT) {
            Log.d("@M_SoundEnhancement", "feature option is off, remove ANC preference");
            getPreferenceScreen().removePreference(this.mAncPref);
        }
        setHasOptionsMenu(false);
    }

    private void updatePreferenceHierarchy() {
        if (this.mAudenhState.equalsIgnoreCase(MTK_AUDENH_SUPPORT_on)) {
            String state = this.mAudioManager.getParameters(GET_MUSIC_PLUS_STATUS);
            Log.d("@M_SoundEnhancement", "get the state: " + state);
            boolean isChecked = false;
            if (state != null) {
                isChecked = state.equals(GET_MUSIC_PLUS_STATUS_ENABLED);
            }
            this.mMusicPlusPrf.setChecked(isChecked);
        }
        if (FeatureOption.MTK_BESLOUDNESS_SUPPORT) {
            String state2 = this.mAudioManager.getParameters(GET_BESLOUDNESS_STATUS);
            Log.d("@M_SoundEnhancement", "get besloudness state: " + state2);
            this.mBesLoudnessPref.setChecked(GET_BESLOUDNESS_STATUS_ENABLED.equals(state2));
        }
        if (!FeatureOption.MTK_ANC_SUPPORT) {
            return;
        }
        String state3 = this.mAudioManager.getParameters(GET_ANC_UI_STATUS);
        Log.d("@M_SoundEnhancement", "ANC state: " + state3);
        boolean checkedStatus = ANC_UI_STATUS_ENABLED.equals(state3);
        this.mAncPref.setChecked(checkedStatus);
    }

    @Override
    public void onResume() {
        Log.d("@M_SoundEnhancement", "onResume");
        super.onResume();
        updatePreferenceHierarchy();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (this.mAudenhState.equalsIgnoreCase(MTK_AUDENH_SUPPORT_on) && this.mMusicPlusPrf == preference) {
            boolean enabled = ((SwitchPreference) preference).isChecked();
            String cmdStr = enabled ? SET_MUSIC_PLUS_ENABLED : SET_MUSIC_PLUS_DISABLED;
            Log.d("@M_SoundEnhancement", " set command about music plus: " + cmdStr);
            this.mAudioManager.setParameters(cmdStr);
        }
        if (FeatureOption.MTK_BESLOUDNESS_SUPPORT && this.mBesLoudnessPref == preference) {
            boolean enabled2 = ((SwitchPreference) preference).isChecked();
            String cmdStr2 = enabled2 ? SET_BESLOUDNESS_ENABLED : SET_BESLOUDNESS_DISABLED;
            Log.d("@M_SoundEnhancement", " set command about besloudness: " + cmdStr2);
            this.mAudioManager.setParameters(cmdStr2);
        }
        if (this.mBesSurroundPref == null) {
            Log.d("@M_SoundEnhancement", " mBesSurroundPref = null");
        } else if (this.mBesSurroundPref.getKey() == null) {
            Log.d("@M_SoundEnhancement", " mBesSurroundPref.getKey() == null)");
        }
        if (this.mBesSurroundPref == preference) {
            Log.d("@M_SoundEnhancement", " mBesSurroundPref onPreferenceTreeClick");
            ((SettingsActivity) getActivity()).startPreferencePanel(BesSurroundSettings.class.getName(), null, -1, this.mContext.getText(R.string.audio_profile_bes_surround_title), null, 0);
        }
        if (FeatureOption.MTK_ANC_SUPPORT && this.mAncPref == preference) {
            boolean enabled3 = ((SwitchPreference) preference).isChecked();
            String cmdStr3 = enabled3 ? ANC_UI_STATUS_ENABLED : ANC_UI_STATUS_DISABLED;
            Log.d("@M_SoundEnhancement", " set command about besloudness: " + cmdStr3);
            this.mAudioManager.setParameters(cmdStr3);
        }
        return super.onPreferenceTreeClick(preference);
    }

    protected static boolean getBesSurroundState(AudioManager audioManager) {
        String state = audioManager.getParameters(GET_BESSURROUND_STATE);
        Log.d("@M_SoundEnhancement", "getBesSurroundState: " + state);
        boolean besState = BESSURROUND_ON.equals(state);
        return besState;
    }

    protected static void setBesSurroundState(AudioManager audioManager, boolean state) {
        audioManager.setParameters(state ? BESSURROUND_ON : BESSURROUND_OFF);
    }

    protected static int getBesSurroundMode(AudioManager audioManager) {
        String state = audioManager.getParameters(GET_BESSURROUND_MODE);
        Log.d("@M_SoundEnhancement", "getBesSurroundMode: " + state);
        boolean modeMovie = BESSURROUND_MOVIE.equals(state);
        return modeMovie ? 0 : 1;
    }

    protected static void setBesSurroundMode(AudioManager audioManager, int mode) {
        audioManager.setParameters(mode == 0 ? BESSURROUND_MOVIE : BESSURROUND_MUSIC);
    }

    @Override
    protected int getMetricsCategory() {
        return 100003;
    }
}
