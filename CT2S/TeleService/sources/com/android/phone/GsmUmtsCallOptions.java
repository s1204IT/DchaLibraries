package com.android.phone;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class GsmUmtsCallOptions extends PreferenceActivity {
    private final boolean DBG = true;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.gsm_umts_call_options);
        if (PhoneGlobals.getPhone().getPhoneType() != 1) {
            getPreferenceScreen().setEnabled(false);
        }
    }
}
