package com.android.phone;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

public class CdmaCallOptions extends PreferenceActivity {
    private final boolean DBG = true;
    private CheckBoxPreference mButtonVoicePrivacy;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.cdma_call_privacy);
        this.mButtonVoicePrivacy = (CheckBoxPreference) findPreference("button_voice_privacy_key");
        if (PhoneGlobals.getPhone().getPhoneType() != 2 || getResources().getBoolean(R.bool.config_voice_privacy_disable)) {
            getPreferenceScreen().setEnabled(false);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return preference.getKey().equals("button_voice_privacy_key");
    }
}
