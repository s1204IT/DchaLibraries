package com.android.systemui.tuner;

import android.support.v14.preference.ListPreferenceDialogFragment;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import com.android.systemui.tuner.CustomListPreference;

public abstract class TunerPreferenceFragment extends PreferenceFragment {
    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        ListPreferenceDialogFragment listPreferenceDialogFragmentNewInstance;
        if (preference instanceof CustomListPreference) {
            listPreferenceDialogFragmentNewInstance = CustomListPreference.CustomListPreferenceDialogFragment.newInstance(preference.getKey());
        } else {
            super.onDisplayPreferenceDialog(preference);
            listPreferenceDialogFragmentNewInstance = null;
        }
        listPreferenceDialogFragmentNewInstance.setTargetFragment(this, 0);
        listPreferenceDialogFragmentNewInstance.show(getFragmentManager(), "dialog_preference");
    }
}
