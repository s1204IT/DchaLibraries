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
        String keyValue = pref.getValue();
        if (keyValue == null) {
            pref.setValue("default");
        } else if (changeHomapagePicker(keyValue)) {
            pref.setValue(getHomepageValue());
        }
        pref.setSummary(getHomepageSummary(pref.getValue()));
        pref.setOnPreferenceChangeListener(this);
        getPreferenceScreen().removePreference(findPreference("general_autofill_title"));
    }

    private boolean changeHomapagePicker(String keyValue) {
        BrowserSettings settings = BrowserSettings.getInstance();
        String homepage = settings.getHomePage();
        if (keyValue.equals("default")) {
            String defaultHomepage = BrowserSettings.getFactoryResetHomeUrl(getActivity());
            if (TextUtils.equals(defaultHomepage, homepage)) {
                return false;
            }
        }
        return ((keyValue.equals("current") && TextUtils.equals(this.mCurrentPage, homepage)) || keyValue.equals("other")) ? false : true;
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
            } else if ("blank".equals(objValue)) {
                settings.setHomePage("about:blank");
            } else if ("default".equals(objValue)) {
                settings.setHomePage(BrowserSettings.getFactoryResetHomeUrl(getActivity()));
            } else if ("most_visited".equals(objValue)) {
                settings.setHomePage("content://com.android.browser.home/");
            } else if ("other".equals(objValue)) {
                promptForHomepage((ListPreference) pref, (String) objValue);
                return false;
            }
            pref.setSummary(getHomepageSummary((String) objValue));
            return true;
        }
        return true;
    }

    void promptForHomepage(final ListPreference pref, final String keyValue) {
        final BrowserSettings settings = BrowserSettings.getInstance();
        final EditText editText = new EditText(getActivity());
        editText.setInputType(17);
        editText.setLongClickable(false);
        editText.setText(settings.getHomePage());
        editText.setSelectAllOnFocus(true);
        editText.setSingleLine(true);
        editText.setImeActionLabel(null, 6);
        final AlertDialog dialog = new AlertDialog.Builder(getActivity()).setView(editText).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog2, int which) {
                String homepage = editText.getText().toString().trim();
                settings.setHomePage(UrlUtils.smartUrlFilter(homepage));
                pref.setValue(keyValue);
                pref.setSummary(GeneralPreferencesFragment.this.getHomepageSummary(keyValue));
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
                if (actionId == 6) {
                    dialog.getButton(-1).performClick();
                    return true;
                }
                return false;
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

    String getHomepageSummary(String keyValue) {
        if (keyValue == null || keyValue.length() <= 0) {
            return null;
        }
        BrowserSettings settings = BrowserSettings.getInstance();
        if (settings.useMostVisitedHomepage()) {
            return getHomepageLabel("most_visited");
        }
        String homepage = settings.getHomePage();
        if (TextUtils.isEmpty(homepage) || "about:blank".equals(homepage)) {
            keyValue = "blank";
        }
        if (keyValue.equals("current") || keyValue.equals("other")) {
            return homepage;
        }
        return getHomepageLabel(keyValue);
    }

    String getHomepageLabel(String value) {
        for (int i = 0; i < this.mValues.length; i++) {
            if (value.equals(this.mValues[i])) {
                return this.mChoices[i];
            }
        }
        return null;
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}
