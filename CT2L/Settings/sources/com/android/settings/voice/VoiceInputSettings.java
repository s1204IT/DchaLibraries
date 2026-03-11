package com.android.settings.voice;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.provider.Settings;
import android.service.voice.VoiceInteractionServiceInfo;
import android.widget.Checkable;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.voice.VoiceInputHelper;
import com.android.settings.voice.VoiceInputPreference;
import java.util.ArrayList;
import java.util.List;

public class VoiceInputSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceClickListener, Indexable, VoiceInputPreference.RadioButtonGroupState {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> indexables = new ArrayList<>();
            String screenTitle = context.getString(R.string.voice_input_settings_title);
            SearchIndexableRaw indexable = new SearchIndexableRaw(context);
            indexable.key = "voice_service_preference_section_title";
            indexable.title = context.getString(R.string.voice_service_preference_section_title);
            indexable.screenTitle = screenTitle;
            indexables.add(indexable);
            List<ResolveInfo> voiceInteractions = context.getPackageManager().queryIntentServices(new Intent("android.service.voice.VoiceInteractionService"), 128);
            int countInteractions = voiceInteractions.size();
            for (int i = 0; i < countInteractions; i++) {
                ResolveInfo info = voiceInteractions.get(i);
                VoiceInteractionServiceInfo visInfo = new VoiceInteractionServiceInfo(context.getPackageManager(), info.serviceInfo);
                if (visInfo.getParseError() == null) {
                    indexables.add(getSearchIndexableRaw(context, info, screenTitle));
                }
            }
            List<ResolveInfo> recognitions = context.getPackageManager().queryIntentServices(new Intent("android.speech.RecognitionService"), 128);
            int countRecognitions = recognitions.size();
            for (int i2 = 0; i2 < countRecognitions; i2++) {
                indexables.add(getSearchIndexableRaw(context, recognitions.get(i2), screenTitle));
            }
            return indexables;
        }

        private SearchIndexableRaw getSearchIndexableRaw(Context context, ResolveInfo info, String screenTitle) {
            ServiceInfo serviceInfo = info.serviceInfo;
            ComponentName componentName = new ComponentName(serviceInfo.packageName, serviceInfo.name);
            SearchIndexableRaw indexable = new SearchIndexableRaw(context);
            indexable.key = componentName.flattenToString();
            indexable.title = info.loadLabel(context.getPackageManager()).toString();
            indexable.screenTitle = screenTitle;
            return indexable;
        }
    };
    private Checkable mCurrentChecked;
    private String mCurrentKey;
    private VoiceInputHelper mHelper;
    private CharSequence mInteractorSummary;
    private CharSequence mInteractorWarning;
    private CharSequence mRecognizerSummary;
    private PreferenceCategory mServicePreferenceCategory;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.voice_input_settings);
        this.mServicePreferenceCategory = (PreferenceCategory) findPreference("voice_service_preference_section");
        this.mInteractorSummary = getActivity().getText(R.string.voice_interactor_preference_summary);
        this.mRecognizerSummary = getActivity().getText(R.string.voice_recognizer_preference_summary);
        this.mInteractorWarning = getActivity().getText(R.string.voice_interaction_security_warning);
    }

    @Override
    public void onStart() {
        super.onStart();
        initSettings();
    }

    private void initSettings() {
        this.mHelper = new VoiceInputHelper(getActivity());
        this.mHelper.buildUi();
        this.mServicePreferenceCategory.removeAll();
        if (this.mHelper.mCurrentVoiceInteraction != null) {
            this.mCurrentKey = this.mHelper.mCurrentVoiceInteraction.flattenToShortString();
        } else if (this.mHelper.mCurrentRecognizer != null) {
            this.mCurrentKey = this.mHelper.mCurrentRecognizer.flattenToShortString();
        } else {
            this.mCurrentKey = null;
        }
        for (int i = 0; i < this.mHelper.mAvailableInteractionInfos.size(); i++) {
            VoiceInputHelper.InteractionInfo info = this.mHelper.mAvailableInteractionInfos.get(i);
            VoiceInputPreference pref = new VoiceInputPreference(getActivity(), info, this.mInteractorSummary, this.mInteractorWarning, this);
            this.mServicePreferenceCategory.addPreference(pref);
        }
        for (int i2 = 0; i2 < this.mHelper.mAvailableRecognizerInfos.size(); i2++) {
            VoiceInputHelper.RecognizerInfo info2 = this.mHelper.mAvailableRecognizerInfos.get(i2);
            VoiceInputPreference pref2 = new VoiceInputPreference(getActivity(), info2, this.mRecognizerSummary, null, this);
            this.mServicePreferenceCategory.addPreference(pref2);
        }
    }

    @Override
    public Checkable getCurrentChecked() {
        return this.mCurrentChecked;
    }

    @Override
    public String getCurrentKey() {
        return this.mCurrentKey;
    }

    @Override
    public void setCurrentChecked(Checkable current) {
        this.mCurrentChecked = current;
    }

    @Override
    public void setCurrentKey(String key) {
        this.mCurrentKey = key;
        for (int i = 0; i < this.mHelper.mAvailableInteractionInfos.size(); i++) {
            VoiceInputHelper.InteractionInfo info = this.mHelper.mAvailableInteractionInfos.get(i);
            if (info.key.equals(key)) {
                Settings.Secure.putString(getActivity().getContentResolver(), "voice_interaction_service", key);
                if (info.settings != null) {
                    Settings.Secure.putString(getActivity().getContentResolver(), "voice_recognition_service", info.settings.flattenToShortString());
                    return;
                }
                return;
            }
        }
        for (int i2 = 0; i2 < this.mHelper.mAvailableRecognizerInfos.size(); i2++) {
            if (this.mHelper.mAvailableRecognizerInfos.get(i2).key.equals(key)) {
                Settings.Secure.putString(getActivity().getContentResolver(), "voice_interaction_service", "");
                Settings.Secure.putString(getActivity().getContentResolver(), "voice_recognition_service", key);
                return;
            }
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference instanceof VoiceInputPreference) {
            ((VoiceInputPreference) preference).doClick();
            return true;
        }
        return true;
    }
}
