package com.svox.pico;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import java.util.ArrayList;
import java.util.Locale;

public class EngineSettings extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = new Intent();
        i.setClass(this, CheckVoiceData.class);
        startActivityForResult(i, 42);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 42) {
            ArrayList<String> available = data.getStringArrayListExtra("availableVoices");
            ArrayList<String> unavailable = data.getStringArrayListExtra("unavailableVoices");
            addPreferencesFromResource(R.xml.voices_list);
            for (int i = 0; i < available.size(); i++) {
                Log.e("debug", available.get(i));
                String[] languageCountry = available.get(i).split("-");
                Locale loc = new Locale(languageCountry[0], languageCountry[1]);
                Preference pref = findPreference(available.get(i));
                pref.setTitle(loc.getDisplayLanguage() + " (" + loc.getDisplayCountry() + ")");
                pref.setSummary(R.string.installed);
                pref.setEnabled(false);
            }
            for (int i2 = 0; i2 < unavailable.size(); i2++) {
                final String unavailableLang = unavailable.get(i2);
                String[] languageCountry2 = unavailableLang.split("-");
                Locale loc2 = new Locale(languageCountry2[0], languageCountry2[1]);
                Preference pref2 = findPreference(unavailableLang);
                pref2.setTitle(loc2.getDisplayLanguage() + " (" + loc2.getDisplayCountry() + ")");
                pref2.setSummary(R.string.not_installed);
                pref2.setEnabled(true);
                pref2.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Uri marketUri = Uri.parse("market://search?q=pname:com.svox.pico.voice." + unavailableLang.toLowerCase().replace("-", "."));
                        Intent marketIntent = new Intent("android.intent.action.VIEW", marketUri);
                        EngineSettings.this.startActivity(marketIntent);
                        return false;
                    }
                });
            }
        }
    }
}
