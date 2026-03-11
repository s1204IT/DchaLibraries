package com.android.settings.inputmethod;

import android.os.Bundle;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.inputmethod.InputMethodAndSubtypeEnablerManager;

public class InputMethodAndSubtypeEnabler extends SettingsPreferenceFragment {
    private InputMethodAndSubtypeEnablerManager mManager;

    @Override
    public int getMetricsCategory() {
        return 60;
    }

    @Override
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

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        String stringExtraFromIntentOrArguments = getStringExtraFromIntentOrArguments("android.intent.extra.TITLE");
        if (!TextUtils.isEmpty(stringExtraFromIntentOrArguments)) {
            getActivity().setTitle(stringExtraFromIntentOrArguments);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mManager.refresh(getContext(), this);
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mManager.save(getContext(), this);
    }
}
