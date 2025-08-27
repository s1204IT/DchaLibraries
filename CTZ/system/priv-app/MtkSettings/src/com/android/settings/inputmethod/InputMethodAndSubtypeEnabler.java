package com.android.settings.inputmethod;

import android.os.Bundle;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.inputmethod.InputMethodAndSubtypeEnablerManager;

/* loaded from: classes.dex */
public class InputMethodAndSubtypeEnabler extends SettingsPreferenceFragment {
    private InputMethodAndSubtypeEnablerManager mManager;

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 60;
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        String stringExtraFromIntentOrArguments = getStringExtraFromIntentOrArguments("input_method_id");
        PreferenceScreen preferenceScreenCreatePreferenceScreen = getPreferenceManager().createPreferenceScreen(getPrefContext());
        this.mManager = new InputMethodAndSubtypeEnablerManager(this);
        this.mManager.init(this, stringExtraFromIntentOrArguments, preferenceScreenCreatePreferenceScreen);
        setPreferenceScreen(preferenceScreenCreatePreferenceScreen);
    }

    private String getStringExtraFromIntentOrArguments(String str) {
        String stringExtra = getActivity().getIntent().getStringExtra(str);
        if (stringExtra != null) {
            return stringExtra;
        }
        Bundle arguments = getArguments();
        if (arguments == null) {
            return null;
        }
        return arguments.getString(str);
    }

    @Override // com.android.settings.SettingsPreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        String stringExtraFromIntentOrArguments = getStringExtraFromIntentOrArguments("android.intent.extra.TITLE");
        if (!TextUtils.isEmpty(stringExtraFromIntentOrArguments)) {
            getActivity().setTitle(stringExtraFromIntentOrArguments);
        }
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        this.mManager.refresh(getContext(), this);
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onPause() {
        super.onPause();
        this.mManager.save(getContext(), this);
    }
}
