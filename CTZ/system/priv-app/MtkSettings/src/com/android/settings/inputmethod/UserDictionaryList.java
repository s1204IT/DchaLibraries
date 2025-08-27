package com.android.settings.inputmethod;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.UserDictionary;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeSet;

/* loaded from: classes.dex */
public class UserDictionaryList extends SettingsPreferenceFragment {
    private String mLocale;

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 61;
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getActivity()));
    }

    @Override // com.android.settings.SettingsPreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onActivityCreated(Bundle bundle) {
        String stringExtra;
        String string;
        super.onActivityCreated(bundle);
        getActivity().getActionBar().setTitle(R.string.user_dict_settings_title);
        Intent intent = getActivity().getIntent();
        if (intent != null) {
            stringExtra = intent.getStringExtra("locale");
        } else {
            stringExtra = null;
        }
        Bundle arguments = getArguments();
        if (arguments != null) {
            string = arguments.getString("locale");
        } else {
            string = null;
        }
        if (string == null) {
            if (stringExtra == null) {
                stringExtra = null;
            }
        } else {
            stringExtra = string;
        }
        this.mLocale = stringExtra;
    }

    public static TreeSet<String> getUserDictionaryLocalesSet(Context context) {
        Cursor cursorQuery = context.getContentResolver().query(UserDictionary.Words.CONTENT_URI, new String[]{"locale"}, null, null, null);
        TreeSet<String> treeSet = new TreeSet<>();
        if (cursorQuery == null) {
            return treeSet;
        }
        try {
            if (cursorQuery.moveToFirst()) {
                int columnIndex = cursorQuery.getColumnIndex("locale");
                do {
                    String string = cursorQuery.getString(columnIndex);
                    if (string == null) {
                        string = "";
                    }
                    treeSet.add(string);
                } while (cursorQuery.moveToNext());
            }
            cursorQuery.close();
            InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService("input_method");
            Iterator<InputMethodInfo> it = inputMethodManager.getEnabledInputMethodList().iterator();
            while (it.hasNext()) {
                Iterator<InputMethodSubtype> it2 = inputMethodManager.getEnabledInputMethodSubtypeList(it.next(), true).iterator();
                while (it2.hasNext()) {
                    String locale = it2.next().getLocale();
                    if (!TextUtils.isEmpty(locale)) {
                        treeSet.add(locale);
                    }
                }
            }
            if (!treeSet.contains(Locale.getDefault().getLanguage().toString())) {
                treeSet.add(Locale.getDefault().toString());
            }
            return treeSet;
        } catch (Throwable th) {
            cursorQuery.close();
            throw th;
        }
    }

    protected void createUserDictSettings(PreferenceGroup preferenceGroup) {
        Activity activity = getActivity();
        preferenceGroup.removeAll();
        TreeSet<String> userDictionaryLocalesSet = getUserDictionaryLocalesSet(activity);
        if (this.mLocale != null) {
            userDictionaryLocalesSet.add(this.mLocale);
        }
        if (userDictionaryLocalesSet.size() > 1) {
            userDictionaryLocalesSet.add("");
        }
        if (userDictionaryLocalesSet.isEmpty()) {
            preferenceGroup.addPreference(createUserDictionaryPreference(null, activity));
            return;
        }
        Iterator<String> it = userDictionaryLocalesSet.iterator();
        while (it.hasNext()) {
            preferenceGroup.addPreference(createUserDictionaryPreference(it.next(), activity));
        }
    }

    protected Preference createUserDictionaryPreference(String str, Activity activity) {
        Preference preference = new Preference(getPrefContext());
        Intent intent = new Intent("android.settings.USER_DICTIONARY_SETTINGS");
        if (str == null) {
            preference.setTitle(Locale.getDefault().getDisplayName());
        } else {
            if ("".equals(str)) {
                preference.setTitle(getString(R.string.user_dict_settings_all_languages));
            } else {
                preference.setTitle(Utils.createLocaleFromString(str).getDisplayName());
            }
            intent.putExtra("locale", str);
            preference.getExtras().putString("locale", str);
        }
        preference.setIntent(intent);
        preference.setFragment(UserDictionarySettings.class.getName());
        return preference;
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        createUserDictSettings(getPreferenceScreen());
    }
}
