package com.android.calendar;

import android.app.Activity;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import java.util.Arrays;

public class QuickResponseSettings extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    EditTextPreference[] mEditTextPrefs;
    String[] mResponses;
    private Toast mToast;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        PreferenceScreen preferenceScreenCreatePreferenceScreen = getPreferenceManager().createPreferenceScreen(getActivity());
        preferenceScreenCreatePreferenceScreen.setTitle(R.string.quick_response_settings_title);
        this.mResponses = Utils.getQuickResponses(getActivity());
        if (this.mResponses != null) {
            this.mEditTextPrefs = new EditTextPreference[this.mResponses.length];
            Arrays.sort(this.mResponses);
            String[] strArr = this.mResponses;
            int length = strArr.length;
            int i = 0;
            int i2 = 0;
            while (i < length) {
                String str = strArr[i];
                EditTextPreference editTextPreference = new EditTextPreference(getActivity());
                editTextPreference.setDialogTitle(R.string.quick_response_settings_edit_title);
                editTextPreference.setTitle(str);
                editTextPreference.setText(str);
                editTextPreference.setKey(str);
                editTextPreference.setOnPreferenceChangeListener(this);
                this.mEditTextPrefs[i2] = editTextPreference;
                preferenceScreenCreatePreferenceScreen.addPreference(editTextPreference);
                i++;
                i2++;
            }
        } else {
            Log.wtf("QuickResponseSettings", "No responses found");
        }
        setPreferenceScreen(preferenceScreenCreatePreferenceScreen);
        this.mToast = Toast.makeText(getActivity(), "", 0);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((CalendarSettingsActivity) activity).hideMenuButtons();
    }

    @Override
    public void onResume() {
        super.onResume();
        CalendarSettingsActivity calendarSettingsActivity = (CalendarSettingsActivity) getActivity();
        if (!calendarSettingsActivity.isMultiPane()) {
            calendarSettingsActivity.setTitle(R.string.quick_response_settings_title);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        for (int i = 0; i < this.mEditTextPrefs.length; i++) {
            if (this.mEditTextPrefs[i].compareTo(preference) == 0) {
                if (!this.mResponses[i].equals(obj)) {
                    String str = (String) obj;
                    if (isValidResponse(str.trim())) {
                        this.mResponses[i] = str;
                        this.mEditTextPrefs[i].setTitle(this.mResponses[i]);
                        this.mEditTextPrefs[i].setText(this.mResponses[i]);
                        Utils.setSharedPreference(getActivity(), "preferences_quick_responses", this.mResponses);
                        return true;
                    }
                    this.mEditTextPrefs[i].getEditText().setText(this.mResponses[i]);
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    private boolean isValidResponse(String str) {
        if (TextUtils.isEmpty(str)) {
            Log.i("QuickResponseSettings", "The response text is empty!");
            this.mToast.setText(R.string.quick_response_error_null);
            this.mToast.show();
            return false;
        }
        for (int i = 0; i < this.mResponses.length; i++) {
            if (this.mResponses[i].equals(str)) {
                Log.i("QuickResponseSettings", "The response exist, i=" + i);
                this.mToast.setText(R.string.quick_response_error_already_exist);
                this.mToast.show();
                return false;
            }
        }
        return true;
    }
}
