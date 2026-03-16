package com.android.calendar;

import android.app.Activity;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;
import java.util.Arrays;

public class QuickResponseSettings extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    EditTextPreference[] mEditTextPrefs;
    String[] mResponses;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceScreen ps = getPreferenceManager().createPreferenceScreen(getActivity());
        ps.setTitle(R.string.quick_response_settings_title);
        this.mResponses = Utils.getQuickResponses(getActivity());
        if (this.mResponses != null) {
            this.mEditTextPrefs = new EditTextPreference[this.mResponses.length];
            Arrays.sort(this.mResponses);
            String[] arr$ = this.mResponses;
            int len$ = arr$.length;
            int i$ = 0;
            int i = 0;
            while (i$ < len$) {
                String response = arr$[i$];
                EditTextPreference et = new EditTextPreference(getActivity());
                et.setDialogTitle(R.string.quick_response_settings_edit_title);
                et.setTitle(response);
                et.setText(response);
                et.setOnPreferenceChangeListener(this);
                this.mEditTextPrefs[i] = et;
                ps.addPreference(et);
                i$++;
                i++;
            }
        } else {
            Log.wtf("QuickResponseSettings", "No responses found");
        }
        setPreferenceScreen(ps);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((CalendarSettingsActivity) activity).hideMenuButtons();
    }

    @Override
    public void onResume() {
        super.onResume();
        CalendarSettingsActivity activity = (CalendarSettingsActivity) getActivity();
        if (!activity.isMultiPane()) {
            activity.setTitle(R.string.quick_response_settings_title);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        for (int i = 0; i < this.mEditTextPrefs.length; i++) {
            if (this.mEditTextPrefs[i].compareTo(preference) == 0) {
                if (!this.mResponses[i].equals(newValue)) {
                    this.mResponses[i] = (String) newValue;
                    this.mEditTextPrefs[i].setTitle(this.mResponses[i]);
                    this.mEditTextPrefs[i].setText(this.mResponses[i]);
                    Utils.setSharedPreference(getActivity(), "preferences_quick_responses", this.mResponses);
                }
                return true;
            }
        }
        return false;
    }
}
