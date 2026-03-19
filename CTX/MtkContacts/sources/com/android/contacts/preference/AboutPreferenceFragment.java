package com.android.contacts.preference;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.Toast;
import com.android.contacts.R;
import com.android.contacts.activities.LicenseActivity;

public class AboutPreferenceFragment extends PreferenceFragment {
    public static AboutPreferenceFragment newInstance() {
        return new AboutPreferenceFragment();
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.preference_about);
        try {
            findPreference(getString(R.string.pref_build_version_key)).setSummary(getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
        }
        findPreference(getString(R.string.pref_open_source_licenses_key)).setIntent(new Intent(getActivity(), (Class<?>) LicenseActivity.class));
        final Preference preferenceFindPreference = findPreference("pref_privacy_policy");
        final Preference preferenceFindPreference2 = findPreference("pref_terms_of_service");
        Preference.OnPreferenceClickListener onPreferenceClickListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    if (preference == preferenceFindPreference) {
                        AboutPreferenceFragment.this.startActivityForUrl("http://www.google.com/policies/privacy");
                    } else if (preference == preferenceFindPreference2) {
                        AboutPreferenceFragment.this.startActivityForUrl("http://www.google.com/policies/terms");
                    }
                    return true;
                } catch (ActivityNotFoundException e2) {
                    Toast.makeText(AboutPreferenceFragment.this.getContext(), AboutPreferenceFragment.this.getString(R.string.url_open_error_toast), 0).show();
                    return true;
                }
            }
        };
        preferenceFindPreference.setOnPreferenceClickListener(onPreferenceClickListener);
        preferenceFindPreference2.setOnPreferenceClickListener(onPreferenceClickListener);
    }

    @Override
    public Context getContext() {
        return getActivity();
    }

    private void startActivityForUrl(String str) {
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        intent.setData(Uri.parse(str));
        startActivity(intent);
    }
}
