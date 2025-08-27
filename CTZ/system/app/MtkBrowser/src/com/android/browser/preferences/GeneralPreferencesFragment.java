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

/* loaded from: classes.dex */
public class GeneralPreferencesFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    String[] mChoices;
    String mCurrentPage;
    String[] mValues;

    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Resources resources = getActivity().getResources();
        this.mChoices = resources.getStringArray(R.array.pref_homepage_choices);
        this.mValues = resources.getStringArray(R.array.pref_homepage_values);
        this.mCurrentPage = getActivity().getIntent().getStringExtra("currentPage");
        addPreferencesFromResource(R.xml.general_preferences);
        ListPreference listPreference = (ListPreference) findPreference("homepage_picker");
        String value = listPreference.getValue();
        if (value == null) {
            listPreference.setValue("default");
        } else if (changeHomapagePicker(value)) {
            listPreference.setValue(getHomepageValue());
        }
        listPreference.setSummary(getHomepageSummary(listPreference.getValue()));
        listPreference.setOnPreferenceChangeListener(this);
        getPreferenceScreen().removePreference(findPreference("general_autofill_title"));
    }

    private boolean changeHomapagePicker(String str) {
        String homePage = BrowserSettings.getInstance().getHomePage();
        if (str.equals("default") && TextUtils.equals(BrowserSettings.getFactoryResetHomeUrl(getActivity()), homePage)) {
            return false;
        }
        return ((str.equals("current") && TextUtils.equals(this.mCurrentPage, homePage)) || str.equals("other")) ? false : true;
    }

    @Override // android.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (getActivity() == null) {
            Log.w("PageContentPreferences", "onPreferenceChange called from detached fragment!");
            return false;
        }
        if (preference.getKey().equals("homepage_picker")) {
            BrowserSettings browserSettings = BrowserSettings.getInstance();
            if ("current".equals(obj)) {
                browserSettings.setHomePage(this.mCurrentPage);
            } else if ("blank".equals(obj)) {
                browserSettings.setHomePage("about:blank");
            } else if ("default".equals(obj)) {
                browserSettings.setHomePage(BrowserSettings.getFactoryResetHomeUrl(getActivity()));
            } else if ("most_visited".equals(obj)) {
                browserSettings.setHomePage("content://com.android.browser.home/");
            } else if ("other".equals(obj)) {
                promptForHomepage((ListPreference) preference, (String) obj);
                return false;
            }
            preference.setSummary(getHomepageSummary((String) obj));
            return true;
        }
        return true;
    }

    void promptForHomepage(final ListPreference listPreference, final String str) {
        final BrowserSettings browserSettings = BrowserSettings.getInstance();
        final EditText editText = new EditText(getActivity());
        editText.setInputType(17);
        editText.setLongClickable(false);
        editText.setText(browserSettings.getHomePage());
        editText.setSelectAllOnFocus(true);
        editText.setSingleLine(true);
        editText.setImeActionLabel(null, 6);
        final AlertDialog alertDialogCreate = new AlertDialog.Builder(getActivity()).setView(editText).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() { // from class: com.android.browser.preferences.GeneralPreferencesFragment.2
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                browserSettings.setHomePage(UrlUtils.smartUrlFilter(editText.getText().toString().trim()));
                listPreference.setValue(str);
                listPreference.setSummary(GeneralPreferencesFragment.this.getHomepageSummary(str));
            }
        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() { // from class: com.android.browser.preferences.GeneralPreferencesFragment.1
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        }).setTitle(R.string.pref_set_homepage_to).create();
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() { // from class: com.android.browser.preferences.GeneralPreferencesFragment.3
            @Override // android.widget.TextView.OnEditorActionListener
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == 6) {
                    alertDialogCreate.getButton(-1).performClick();
                    return true;
                }
                return false;
            }
        });
        alertDialogCreate.getWindow().setSoftInputMode(5);
        alertDialogCreate.show();
    }

    String getHomepageValue() {
        String homePage = BrowserSettings.getInstance().getHomePage();
        if (TextUtils.isEmpty(homePage) || "about:blank".endsWith(homePage)) {
            return "blank";
        }
        if ("content://com.android.browser.home/".equals(homePage)) {
            return "most_visited";
        }
        if (TextUtils.equals(BrowserSettings.getFactoryResetHomeUrl(getActivity()), homePage)) {
            return "default";
        }
        if (TextUtils.equals(this.mCurrentPage, homePage)) {
            return "current";
        }
        return "other";
    }

    String getHomepageSummary(String str) {
        if (str == null || str.length() <= 0) {
            return null;
        }
        BrowserSettings browserSettings = BrowserSettings.getInstance();
        if (browserSettings.useMostVisitedHomepage()) {
            return getHomepageLabel("most_visited");
        }
        String homePage = browserSettings.getHomePage();
        if (TextUtils.isEmpty(homePage) || "about:blank".equals(homePage)) {
            str = "blank";
        }
        if (str.equals("current") || str.equals("other")) {
            return homePage;
        }
        return getHomepageLabel(str);
    }

    String getHomepageLabel(String str) {
        for (int i = 0; i < this.mValues.length; i++) {
            if (str.equals(this.mValues[i])) {
                return this.mChoices[i];
            }
        }
        return null;
    }

    @Override // android.app.Fragment
    public void onResume() {
        super.onResume();
    }
}
