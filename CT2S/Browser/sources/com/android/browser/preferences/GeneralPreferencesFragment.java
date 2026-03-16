package com.android.browser.preferences;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.TextView;
import com.android.browser.BrowserSettings;
import com.android.browser.R;
import com.android.browser.UrlUtils;

public class GeneralPreferencesFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    String[] mChoices;
    String mCurrentPage;
    String[] mValues;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Resources res = getActivity().getResources();
        this.mChoices = res.getStringArray(R.array.pref_homepage_choices);
        this.mValues = res.getStringArray(R.array.pref_homepage_values);
        this.mCurrentPage = getActivity().getIntent().getStringExtra("currentPage");
        addPreferencesFromResource(R.xml.general_preferences);
        ListPreference pref = (ListPreference) findPreference("homepage_picker");
        pref.setSummary(getHomepageSummary());
        pref.setPersistent(false);
        pref.setValue(getHomepageValue());
        pref.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object objValue) {
        if (getActivity() == null) {
            Log.w("PageContentPreferencesFragment", "onPreferenceChange called from detached fragment!");
            return false;
        }
        if (pref.getKey().equals("homepage_picker")) {
            BrowserSettings settings = BrowserSettings.getInstance();
            if ("current".equals(objValue)) {
                settings.setHomePage(this.mCurrentPage);
            }
            if ("blank".equals(objValue)) {
                settings.setHomePage("about:blank");
            }
            if ("default".equals(objValue)) {
                settings.setHomePage(BrowserSettings.getFactoryResetHomeUrl(getActivity()));
            }
            if ("most_visited".equals(objValue)) {
                settings.setHomePage("content://com.android.browser.home/");
            }
            if ("other".equals(objValue)) {
                promptForHomepage((ListPreference) pref);
                return false;
            }
            pref.setSummary(getHomepageSummary());
            ((ListPreference) pref).setValue(getHomepageValue());
            return false;
        }
        return true;
    }

    void promptForHomepage(final ListPreference pref) {
        final BrowserSettings settings = BrowserSettings.getInstance();
        final EditText editText = new EditText(getActivity());
        editText.setInputType(17);
        editText.setText(settings.getHomePage());
        editText.setSelectAllOnFocus(true);
        editText.setSingleLine(true);
        editText.setImeActionLabel(null, 6);
        final AlertDialog dialog = new AlertDialog.Builder(getActivity()).setView(editText).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog2, int which) {
                String homepage = editText.getText().toString().trim();
                settings.setHomePage(UrlUtils.smartUrlFilter(homepage));
                pref.setValue(GeneralPreferencesFragment.this.getHomepageValue());
                pref.setSummary(GeneralPreferencesFragment.this.getHomepageSummary());
            }
        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog2, int which) {
                dialog2.cancel();
            }
        }).setTitle(R.string.pref_set_homepage_to).create();
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId != 6) {
                    return false;
                }
                dialog.getButton(-1).performClick();
                return true;
            }
        });
        dialog.getWindow().setSoftInputMode(5);
        dialog.show();
    }

    String getHomepageValue() {
        BrowserSettings settings = BrowserSettings.getInstance();
        String homepage = settings.getHomePage();
        if (TextUtils.isEmpty(homepage) || "about:blank".endsWith(homepage)) {
            return "blank";
        }
        if ("content://com.android.browser.home/".equals(homepage)) {
            return "most_visited";
        }
        String defaultHomepage = BrowserSettings.getFactoryResetHomeUrl(getActivity());
        if (TextUtils.equals(defaultHomepage, homepage)) {
            return "default";
        }
        if (TextUtils.equals(this.mCurrentPage, homepage)) {
            return "current";
        }
        return "other";
    }

    String getHomepageSummary() {
        BrowserSettings settings = BrowserSettings.getInstance();
        if (settings.useMostVisitedHomepage()) {
            return getHomepageLabel("most_visited");
        }
        String homepage = settings.getHomePage();
        if (TextUtils.isEmpty(homepage) || "about:blank".equals(homepage)) {
            return getHomepageLabel("blank");
        }
        return homepage;
    }

    String getHomepageLabel(String value) {
        for (int i = 0; i < this.mValues.length; i++) {
            if (value.equals(this.mValues[i])) {
                return this.mChoices[i];
            }
        }
        return null;
    }
}
