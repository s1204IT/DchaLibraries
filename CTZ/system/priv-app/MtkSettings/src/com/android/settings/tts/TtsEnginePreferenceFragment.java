package com.android.settings.tts;

import android.content.Context;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TtsEngines;
import android.support.v7.preference.PreferenceCategory;
import android.util.Log;
import android.widget.Checkable;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.tts.TtsEnginePreference;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class TtsEnginePreferenceFragment extends SettingsPreferenceFragment implements Indexable, TtsEnginePreference.RadioButtonGroupState {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.tts.TtsEnginePreferenceFragment.2
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.tts_engine_picker;
            return Arrays.asList(searchIndexableResource);
        }
    };
    private Checkable mCurrentChecked;
    private String mCurrentEngine;
    private PreferenceCategory mEnginePreferenceCategory;
    private String mPreviousEngine;
    private TextToSpeech mTts = null;
    private TtsEngines mEnginesHelper = null;
    private final TextToSpeech.OnInitListener mUpdateListener = new TextToSpeech.OnInitListener() { // from class: com.android.settings.tts.TtsEnginePreferenceFragment.1
        @Override // android.speech.tts.TextToSpeech.OnInitListener
        public void onInit(int i) {
            TtsEnginePreferenceFragment.this.onUpdateEngine(i);
        }
    };

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.tts_engine_picker);
        this.mEnginePreferenceCategory = (PreferenceCategory) findPreference("tts_engine_preference_category");
        this.mEnginesHelper = new TtsEngines(getActivity().getApplicationContext());
        this.mTts = new TextToSpeech(getActivity().getApplicationContext(), null);
        initSettings();
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 93;
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onDestroy() {
        super.onDestroy();
        if (this.mTts != null) {
            this.mTts.shutdown();
            this.mTts = null;
        }
    }

    private void initSettings() {
        if (this.mTts != null) {
            this.mCurrentEngine = this.mTts.getCurrentEngine();
        }
        this.mEnginePreferenceCategory.removeAll();
        SettingsActivity settingsActivity = (SettingsActivity) getActivity();
        Iterator it = this.mEnginesHelper.getEngines().iterator();
        while (it.hasNext()) {
            this.mEnginePreferenceCategory.addPreference(new TtsEnginePreference(getPrefContext(), (TextToSpeech.EngineInfo) it.next(), this, settingsActivity));
        }
    }

    @Override // com.android.settings.tts.TtsEnginePreference.RadioButtonGroupState
    public Checkable getCurrentChecked() {
        return this.mCurrentChecked;
    }

    @Override // com.android.settings.tts.TtsEnginePreference.RadioButtonGroupState
    public String getCurrentKey() {
        return this.mCurrentEngine;
    }

    @Override // com.android.settings.tts.TtsEnginePreference.RadioButtonGroupState
    public void setCurrentChecked(Checkable checkable) {
        this.mCurrentChecked = checkable;
    }

    private void updateDefaultEngine(String str) {
        Log.d("TtsEnginePrefFragment", "Updating default synth to : " + str);
        this.mPreviousEngine = this.mTts.getCurrentEngine();
        Log.i("TtsEnginePrefFragment", "Shutting down current tts engine");
        if (this.mTts != null) {
            try {
                this.mTts.shutdown();
                this.mTts = null;
            } catch (Exception e) {
                Log.e("TtsEnginePrefFragment", "Error shutting down TTS engine" + e);
            }
        }
        Log.i("TtsEnginePrefFragment", "Updating engine : Attempting to connect to engine: " + str);
        this.mTts = new TextToSpeech(getActivity().getApplicationContext(), this.mUpdateListener, str);
        Log.i("TtsEnginePrefFragment", "Success");
    }

    public void onUpdateEngine(int i) {
        if (i == 0) {
            Log.d("TtsEnginePrefFragment", "Updating engine: Successfully bound to the engine: " + this.mTts.getCurrentEngine());
            Settings.Secure.putString(getContentResolver(), "tts_default_synth", this.mTts.getCurrentEngine());
            return;
        }
        Log.d("TtsEnginePrefFragment", "Updating engine: Failed to bind to engine, reverting.");
        if (this.mPreviousEngine != null) {
            this.mTts = new TextToSpeech(getActivity().getApplicationContext(), null, this.mPreviousEngine);
        }
        this.mPreviousEngine = null;
    }

    @Override // com.android.settings.tts.TtsEnginePreference.RadioButtonGroupState
    public void setCurrentKey(String str) {
        this.mCurrentEngine = str;
        updateDefaultEngine(this.mCurrentEngine);
    }
}
