package com.android.phone.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import com.android.phone.R;

public class VoicemailSettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.voicemail_settings);
    }
}
