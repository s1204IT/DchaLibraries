package com.android.contacts.common.preference;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import com.android.contacts.R;

public class DisplayOptionsPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_display_options);
    }
}
