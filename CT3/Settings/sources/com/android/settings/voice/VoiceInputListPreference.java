package com.android.settings.voice;

import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import com.android.settings.AppListPreferenceWithSettings;
import com.android.settings.R;
import com.android.settings.voice.VoiceInputHelper;
import com.mediatek.settings.inputmethod.InputMethodExts;
import java.util.ArrayList;
import java.util.List;

public class VoiceInputListPreference extends AppListPreferenceWithSettings {
    private ComponentName mAssistRestrict;
    private final List<Integer> mAvailableIndexes;
    private VoiceInputHelper mHelper;

    public VoiceInputListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mAvailableIndexes = new ArrayList();
        setDialogTitle(R.string.choose_voice_input_title);
    }

    @Override
    protected ListAdapter createListAdapter() {
        return new CustomAdapter(getContext(), getEntries());
    }

    @Override
    protected boolean persistString(String value) {
        for (int i = 0; i < this.mHelper.mAvailableInteractionInfos.size(); i++) {
            VoiceInputHelper.InteractionInfo info = this.mHelper.mAvailableInteractionInfos.get(i);
            if (info.key.equals(value)) {
                Settings.Secure.putString(getContext().getContentResolver(), "voice_interaction_service", value);
                Settings.Secure.putString(getContext().getContentResolver(), "voice_recognition_service", new ComponentName(info.service.packageName, info.serviceInfo.getRecognitionService()).flattenToShortString());
                setSummary(getEntry());
                setSettingsComponent(info.settings);
                return true;
            }
        }
        for (int i2 = 0; i2 < this.mHelper.mAvailableRecognizerInfos.size(); i2++) {
            VoiceInputHelper.RecognizerInfo info2 = this.mHelper.mAvailableRecognizerInfos.get(i2);
            if (info2.key.equals(value)) {
                Settings.Secure.putString(getContext().getContentResolver(), "voice_interaction_service", "");
                Settings.Secure.putString(getContext().getContentResolver(), "voice_recognition_service", value);
                setSummary(getEntry());
                setSettingsComponent(info2.settings);
                return true;
            }
        }
        setSettingsComponent(null);
        return true;
    }

    @Override
    public void setPackageNames(CharSequence[] packageNames, CharSequence defaultPackageName) {
    }

    public void setAssistRestrict(ComponentName assistRestrict) {
        this.mAssistRestrict = assistRestrict;
    }

    public void refreshVoiceInputs() {
        this.mHelper = new VoiceInputHelper(getContext());
        this.mHelper.buildUi();
        String assistKey = this.mAssistRestrict == null ? "" : this.mAssistRestrict.flattenToShortString();
        this.mAvailableIndexes.clear();
        List<CharSequence> entries = new ArrayList<>();
        List<CharSequence> values = new ArrayList<>();
        for (int i = 0; i < this.mHelper.mAvailableInteractionInfos.size(); i++) {
            VoiceInputHelper.InteractionInfo info = this.mHelper.mAvailableInteractionInfos.get(i);
            if (InputMethodExts.isVoiceInteractionServiceSupport(getContext(), info.key)) {
                entries.add(info.appLabel);
                values.add(info.key);
                if (info.key.contentEquals(assistKey)) {
                    this.mAvailableIndexes.add(Integer.valueOf(i));
                }
            }
        }
        boolean assitIsService = !this.mAvailableIndexes.isEmpty();
        int serviceCount = entries.size();
        for (int i2 = 0; i2 < this.mHelper.mAvailableRecognizerInfos.size(); i2++) {
            VoiceInputHelper.RecognizerInfo info2 = this.mHelper.mAvailableRecognizerInfos.get(i2);
            if (!InputMethodExts.isVoiceRecognitionServiceSupport(getContext(), info2.key)) {
                entries.add(info2.label);
                values.add(info2.key);
                if (!assitIsService) {
                    this.mAvailableIndexes.add(Integer.valueOf(serviceCount + i2));
                }
            }
        }
        setEntries((CharSequence[]) entries.toArray(new CharSequence[entries.size()]));
        setEntryValues((CharSequence[]) values.toArray(new CharSequence[values.size()]));
        if (this.mHelper.mCurrentVoiceInteraction != null) {
            setValue(this.mHelper.mCurrentVoiceInteraction.flattenToShortString());
        } else if (this.mHelper.mCurrentRecognizer != null) {
            setValue(this.mHelper.mCurrentRecognizer.flattenToShortString());
        } else {
            setValue(null);
        }
    }

    public ComponentName getCurrentService() {
        if (this.mHelper.mCurrentVoiceInteraction != null) {
            return this.mHelper.mCurrentVoiceInteraction;
        }
        if (this.mHelper.mCurrentRecognizer != null) {
            return this.mHelper.mCurrentRecognizer;
        }
        return null;
    }

    private class CustomAdapter extends ArrayAdapter<CharSequence> {
        public CustomAdapter(Context context, CharSequence[] objects) {
            super(context, android.R.layout.notification_template_material_big_call, android.R.id.text1, objects);
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return VoiceInputListPreference.this.mAvailableIndexes.contains(Integer.valueOf(position));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            view.setEnabled(isEnabled(position));
            return view;
        }
    }
}
