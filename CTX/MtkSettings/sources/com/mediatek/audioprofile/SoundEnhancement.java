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
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.mediatek.settings.FeatureOption;
import java.util.ArrayList;
import java.util.List;

public class SoundEnhancement extends SettingsPreferenceFragment implements Indexable {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean z) {
            ArrayList arrayList = new ArrayList();
            Resources resources = context.getResources();
            SearchIndexableRaw searchIndexableRaw = new SearchIndexableRaw(context);
            searchIndexableRaw.title = resources.getString(R.string.sound_enhancement_title);
            searchIndexableRaw.screenTitle = resources.getString(R.string.sound_enhancement_title);
            searchIndexableRaw.keywords = resources.getString(R.string.sound_enhancement_title);
            arrayList.add(searchIndexableRaw);
            return arrayList;
        }
    };
    private SwitchPreference mAncPref;
    private SwitchPreference mBesLoudnessPref;
    private Context mContext;
    private SwitchPreference mHifiModePref;
    private SwitchPreference mMusicPlusPrf;
    private AudioManager mAudioManager = null;
    private String mAudenhState = null;

    @Override
    public void onCreate(Bundle bundle) {
        Log.d("@M_SoundEnhancement", "onCreate");
        super.onCreate(bundle);
        this.mContext = getActivity();
        this.mAudioManager = (AudioManager) getSystemService("audio");
        this.mAudenhState = this.mAudioManager.getParameters("MTK_AUDENH_SUPPORT");
        Log.d("@M_SoundEnhancement", "AudENH state: " + this.mAudenhState);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen != null) {
            preferenceScreen.removeAll();
        }
        addPreferencesFromResource(R.xml.audioprofile_sound_enhancement);
        this.mMusicPlusPrf = (SwitchPreference) findPreference("music_plus");
        this.mBesLoudnessPref = (SwitchPreference) findPreference("bes_loudness");
        this.mAncPref = (SwitchPreference) findPreference("anc_switch");
        this.mHifiModePref = (SwitchPreference) findPreference("hifi_mode");
        if (!this.mAudenhState.equalsIgnoreCase("MTK_AUDENH_SUPPORT=true")) {
            Log.d("@M_SoundEnhancement", "remove audio enhance preference " + this.mMusicPlusPrf);
            getPreferenceScreen().removePreference(this.mMusicPlusPrf);
        }
        if (!FeatureOption.MTK_BESLOUDNESS_SUPPORT) {
            Log.d("@M_SoundEnhancement", "feature option is off, remove BesLoudness preference");
            getPreferenceScreen().removePreference(this.mBesLoudnessPref);
        }
        if (!FeatureOption.MTK_ANC_SUPPORT) {
            Log.d("@M_SoundEnhancement", "feature option is off, remove ANC preference");
            getPreferenceScreen().removePreference(this.mAncPref);
        }
        if (!FeatureOption.MTK_HIFI_AUDIO_SUPPORT) {
            Log.d("@M_SoundEnhancement", "feature option is off, remove HiFi preference");
            getPreferenceScreen().removePreference(this.mHifiModePref);
        }
        setHasOptionsMenu(false);
    }

    private void updatePreferenceHierarchy() {
        if (this.mAudenhState.equalsIgnoreCase("MTK_AUDENH_SUPPORT=true")) {
            String parameters = this.mAudioManager.getParameters("GetMusicPlusStatus");
            Log.d("@M_SoundEnhancement", "get the state: " + parameters);
            boolean z = false;
            if (parameters != null && parameters.equals("GetMusicPlusStatus=1")) {
                z = true;
            }
            this.mMusicPlusPrf.setChecked(z);
        }
        if (FeatureOption.MTK_BESLOUDNESS_SUPPORT) {
            String parameters2 = this.mAudioManager.getParameters("GetBesLoudnessStatus");
            Log.d("@M_SoundEnhancement", "get besloudness state: " + parameters2);
            this.mBesLoudnessPref.setChecked("GetBesLoudnessStatus=1".equals(parameters2));
        }
        if (FeatureOption.MTK_ANC_SUPPORT) {
            String parameters3 = this.mAudioManager.getParameters("ANC_UI");
            Log.d("@M_SoundEnhancement", "ANC state: " + parameters3);
            this.mAncPref.setChecked("ANC_UI=on".equals(parameters3));
        }
        if (FeatureOption.MTK_HIFI_AUDIO_SUPPORT) {
            String parameters4 = this.mAudioManager.getParameters("hifi_dac");
            Log.d("@M_SoundEnhancement", "HiFi state: " + parameters4);
            this.mHifiModePref.setChecked("hifi_dac=on".equals(parameters4));
        }
    }

    @Override
    public void onResume() {
        Log.d("@M_SoundEnhancement", "onResume");
        super.onResume();
        updatePreferenceHierarchy();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (this.mAudenhState.equalsIgnoreCase("MTK_AUDENH_SUPPORT=true") && this.mMusicPlusPrf == preference) {
            String str = ((SwitchPreference) preference).isChecked() ? "SetMusicPlusStatus=1" : "SetMusicPlusStatus=0";
            Log.d("@M_SoundEnhancement", " set command about music plus: " + str);
            this.mAudioManager.setParameters(str);
        }
        if (FeatureOption.MTK_BESLOUDNESS_SUPPORT && this.mBesLoudnessPref == preference) {
            String str2 = ((SwitchPreference) preference).isChecked() ? "SetBesLoudnessStatus=1" : "SetBesLoudnessStatus=0";
            Log.d("@M_SoundEnhancement", " set command about besloudness: " + str2);
            this.mAudioManager.setParameters(str2);
        }
        if (FeatureOption.MTK_ANC_SUPPORT && this.mAncPref == preference) {
            String str3 = ((SwitchPreference) preference).isChecked() ? "ANC_UI=on" : "ANC_UI=off";
            Log.d("@M_SoundEnhancement", " set command about besloudness: " + str3);
            this.mAudioManager.setParameters(str3);
        }
        if (FeatureOption.MTK_HIFI_AUDIO_SUPPORT && this.mHifiModePref == preference) {
            String str4 = ((SwitchPreference) preference).isChecked() ? "hifi_dac=on" : "hifi_dac=off";
            Log.d("@M_SoundEnhancement", " set command about hifi mode: " + str4);
            this.mAudioManager.setParameters(str4);
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public int getMetricsCategory() {
        return 336;
    }
}
