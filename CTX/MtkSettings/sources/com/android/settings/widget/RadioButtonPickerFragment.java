package com.android.settings.widget;

import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.widget.RadioButtonPreference;
import com.android.settingslib.widget.CandidateInfo;
import java.util.List;
import java.util.Map;

public abstract class RadioButtonPickerFragment extends InstrumentedPreferenceFragment implements RadioButtonPreference.OnClickListener {
    static final String EXTRA_FOR_WORK = "for_work";
    private final Map<String, CandidateInfo> mCandidates = new ArrayMap();
    protected int mUserId;
    protected UserManager mUserManager;

    protected abstract List<? extends CandidateInfo> getCandidates();

    protected abstract String getDefaultKey();

    @Override
    protected abstract int getPreferenceScreenResId();

    protected abstract boolean setDefaultKey(String str);

    @Override
    public void onAttach(Context context) {
        boolean z;
        int iMyUserId;
        super.onAttach(context);
        this.mUserManager = (UserManager) context.getSystemService("user");
        Bundle arguments = getArguments();
        if (arguments != null) {
            z = arguments.getBoolean(EXTRA_FOR_WORK);
        } else {
            z = false;
        }
        UserHandle managedProfile = Utils.getManagedProfile(this.mUserManager);
        if (z && managedProfile != null) {
            iMyUserId = managedProfile.getIdentifier();
        } else {
            iMyUserId = UserHandle.myUserId();
        }
        this.mUserId = iMyUserId;
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String str) {
        super.onCreatePreferences(bundle, str);
        updateCandidates();
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View viewOnCreateView = super.onCreateView(layoutInflater, viewGroup, bundle);
        setHasOptionsMenu(true);
        return viewOnCreateView;
    }

    public void onRadioButtonClicked(RadioButtonPreference radioButtonPreference) {
        onRadioButtonConfirmed(radioButtonPreference.getKey());
    }

    protected void onSelectionPerformed(boolean z) {
    }

    protected boolean shouldShowItemNone() {
        return false;
    }

    protected void addStaticPreferences(PreferenceScreen preferenceScreen) {
    }

    protected CandidateInfo getCandidate(String str) {
        return this.mCandidates.get(str);
    }

    protected void onRadioButtonConfirmed(String str) {
        boolean defaultKey = setDefaultKey(str);
        if (defaultKey) {
            updateCheckedState(str);
        }
        onSelectionPerformed(defaultKey);
    }

    public void bindPreferenceExtra(RadioButtonPreference radioButtonPreference, String str, CandidateInfo candidateInfo, String str2, String str3) {
    }

    public void updateCandidates() {
        this.mCandidates.clear();
        List<? extends CandidateInfo> candidates = getCandidates();
        if (candidates != null) {
            for (CandidateInfo candidateInfo : candidates) {
                this.mCandidates.put(candidateInfo.getKey(), candidateInfo);
            }
        }
        String defaultKey = getDefaultKey();
        String systemDefaultKey = getSystemDefaultKey();
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.removeAll();
        addStaticPreferences(preferenceScreen);
        int radioButtonPreferenceCustomLayoutResId = getRadioButtonPreferenceCustomLayoutResId();
        if (shouldShowItemNone()) {
            RadioButtonPreference radioButtonPreference = new RadioButtonPreference(getPrefContext());
            if (radioButtonPreferenceCustomLayoutResId > 0) {
                radioButtonPreference.setLayoutResource(radioButtonPreferenceCustomLayoutResId);
            }
            radioButtonPreference.setIcon(R.drawable.ic_remove_circle);
            radioButtonPreference.setTitle(R.string.app_list_preference_none);
            radioButtonPreference.setChecked(TextUtils.isEmpty(defaultKey));
            radioButtonPreference.setOnClickListener(this);
            preferenceScreen.addPreference(radioButtonPreference);
        }
        if (candidates != null) {
            for (CandidateInfo candidateInfo2 : candidates) {
                RadioButtonPreference radioButtonPreference2 = new RadioButtonPreference(getPrefContext());
                if (radioButtonPreferenceCustomLayoutResId > 0) {
                    radioButtonPreference2.setLayoutResource(radioButtonPreferenceCustomLayoutResId);
                }
                bindPreference(radioButtonPreference2, candidateInfo2.getKey(), candidateInfo2, defaultKey);
                bindPreferenceExtra(radioButtonPreference2, candidateInfo2.getKey(), candidateInfo2, defaultKey, systemDefaultKey);
                preferenceScreen.addPreference(radioButtonPreference2);
            }
        }
        mayCheckOnlyRadioButton();
    }

    public RadioButtonPreference bindPreference(RadioButtonPreference radioButtonPreference, String str, CandidateInfo candidateInfo, String str2) {
        radioButtonPreference.setTitle(candidateInfo.loadLabel());
        Utils.setSafeIcon(radioButtonPreference, candidateInfo.loadIcon());
        radioButtonPreference.setKey(str);
        if (TextUtils.equals(str2, str)) {
            radioButtonPreference.setChecked(true);
        }
        radioButtonPreference.setEnabled(candidateInfo.enabled);
        radioButtonPreference.setOnClickListener(this);
        return radioButtonPreference;
    }

    public void updateCheckedState(String str) {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen != null) {
            int preferenceCount = preferenceScreen.getPreferenceCount();
            for (int i = 0; i < preferenceCount; i++) {
                Preference preference = preferenceScreen.getPreference(i);
                if (preference instanceof RadioButtonPreference) {
                    RadioButtonPreference radioButtonPreference = (RadioButtonPreference) preference;
                    if (radioButtonPreference.isChecked() != TextUtils.equals(preference.getKey(), str)) {
                        radioButtonPreference.setChecked(TextUtils.equals(preference.getKey(), str));
                    }
                }
            }
        }
    }

    public void mayCheckOnlyRadioButton() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen != null && preferenceScreen.getPreferenceCount() == 1) {
            Preference preference = preferenceScreen.getPreference(0);
            if (preference instanceof RadioButtonPreference) {
                ((RadioButtonPreference) preference).setChecked(true);
            }
        }
    }

    protected String getSystemDefaultKey() {
        return null;
    }

    protected int getRadioButtonPreferenceCustomLayoutResId() {
        return 0;
    }
}
