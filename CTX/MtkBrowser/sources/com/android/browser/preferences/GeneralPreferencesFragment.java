package com.android.browser.preferences;

import android.R;
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
import com.android.browser.UrlUtils;

public class GeneralPreferencesFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    String[] mChoices;
    String mCurrentPage;
    String[] mValues;

    private boolean changeHomapagePicker(String str) {
        String homePage = BrowserSettings.getInstance().getHomePage();
        if (str.equals("default") && TextUtils.equals(BrowserSettings.getFactoryResetHomeUrl(getActivity()), homePage)) {
            return false;
        }
        return ((str.equals("current") && TextUtils.equals(this.mCurrentPage, homePage)) || str.equals("other")) ? false : true;
    }

    String getHomepageLabel(String str) {
        for (int i = 0; i < this.mValues.length; i++) {
            if (str.equals(this.mValues[i])) {
                return this.mChoices[i];
            }
        }
        return null;
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
        return (str.equals("current") || str.equals("other")) ? homePage : getHomepageLabel(str);
    }

    String getHomepageValue() {
        String homePage = BrowserSettings.getInstance().getHomePage();
        return (TextUtils.isEmpty(homePage) || "about:blank".endsWith(homePage)) ? "blank" : "content://com.android.browser.home/".equals(homePage) ? "most_visited" : TextUtils.equals(BrowserSettings.getFactoryResetHomeUrl(getActivity()), homePage) ? "default" : TextUtils.equals(this.mCurrentPage, homePage) ? "current" : "other";
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Resources resources = getActivity().getResources();
        this.mChoices = resources.getStringArray(2131230822);
        this.mValues = resources.getStringArray(2131230823);
        this.mCurrentPage = getActivity().getIntent().getStringExtra("currentPage");
        addPreferencesFromResource(2131099655);
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

    @Override
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
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    void promptForHomepage(ListPreference listPreference, String str) {
        BrowserSettings browserSettings = BrowserSettings.getInstance();
        EditText editText = new EditText(getActivity());
        editText.setInputType(17);
        editText.setLongClickable(false);
        editText.setText(browserSettings.getHomePage());
        editText.setSelectAllOnFocus(true);
        editText.setSingleLine(true);
        editText.setImeActionLabel(null, 6);
        AlertDialog alertDialogCreate = new AlertDialog.Builder(getActivity()).setView(editText).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(this, editText, browserSettings, listPreference, str) {
            final GeneralPreferencesFragment this$0;
            final EditText val$editText;
            final String val$keyValue;
            final ListPreference val$pref;
            final BrowserSettings val$settings;

            {
                this.this$0 = this;
                this.val$editText = editText;
                this.val$settings = browserSettings;
                this.val$pref = listPreference;
                this.val$keyValue = str;
            }

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                this.val$settings.setHomePage(UrlUtils.smartUrlFilter(this.val$editText.getText().toString().trim()));
                this.val$pref.setValue(this.val$keyValue);
                this.val$pref.setSummary(this.this$0.getHomepageSummary(this.val$keyValue));
            }
        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(this) {
            final GeneralPreferencesFragment this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        }).setTitle(2131493067).create();
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener(this, alertDialogCreate) {
            final GeneralPreferencesFragment this$0;
            final AlertDialog val$dialog;

            {
                this.this$0 = this;
                this.val$dialog = alertDialogCreate;
            }

            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i != 6) {
                    return false;
                }
                this.val$dialog.getButton(-1).performClick();
                return true;
            }
        });
        alertDialogCreate.getWindow().setSoftInputMode(5);
        alertDialogCreate.show();
    }
}
