package com.android.settings.inputmethod;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.UserDictionary;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import com.android.settings.R;
import com.android.settings.Utils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeSet;

public class UserDictionaryAddWordContents {
    private static final String[] HAS_WORD_PROJECTION = {"word"};
    private String mLocale;
    private final int mMode;
    private final String mOldShortcut;
    private final String mOldWord;
    private String mSavedShortcut;
    private String mSavedWord;
    private final EditText mShortcutEditText;
    private final EditText mWordEditText;

    UserDictionaryAddWordContents(View view, Bundle bundle) {
        this.mWordEditText = (EditText) view.findViewById(R.id.user_dictionary_add_word_text);
        this.mShortcutEditText = (EditText) view.findViewById(R.id.user_dictionary_add_shortcut);
        String string = bundle.getString("word");
        if (string != null) {
            this.mWordEditText.setText(string);
            this.mWordEditText.setSelection(this.mWordEditText.getText().length());
        }
        String string2 = bundle.getString("shortcut");
        if (string2 != null && this.mShortcutEditText != null) {
            this.mShortcutEditText.setText(string2);
        }
        this.mMode = bundle.getInt("mode");
        this.mOldWord = bundle.getString("word");
        this.mOldShortcut = bundle.getString("shortcut");
        updateLocale(bundle.getString("locale"));
    }

    UserDictionaryAddWordContents(View view, UserDictionaryAddWordContents userDictionaryAddWordContents) {
        this.mWordEditText = (EditText) view.findViewById(R.id.user_dictionary_add_word_text);
        this.mShortcutEditText = (EditText) view.findViewById(R.id.user_dictionary_add_shortcut);
        this.mMode = 0;
        this.mOldWord = userDictionaryAddWordContents.mSavedWord;
        this.mOldShortcut = userDictionaryAddWordContents.mSavedShortcut;
        updateLocale(userDictionaryAddWordContents.getCurrentUserDictionaryLocale());
    }

    void updateLocale(String str) {
        if (str == null) {
            str = Locale.getDefault().toString();
        }
        this.mLocale = str;
    }

    void saveStateIntoBundle(Bundle bundle) {
        bundle.putString("word", this.mWordEditText.getText().toString());
        bundle.putString("originalWord", this.mOldWord);
        if (this.mShortcutEditText != null) {
            bundle.putString("shortcut", this.mShortcutEditText.getText().toString());
        }
        if (this.mOldShortcut != null) {
            bundle.putString("originalShortcut", this.mOldShortcut);
        }
        bundle.putString("locale", this.mLocale);
    }

    void delete(Context context) {
        if (this.mMode == 0 && !TextUtils.isEmpty(this.mOldWord)) {
            UserDictionarySettings.deleteWord(this.mOldWord, this.mOldShortcut, context.getContentResolver());
        }
    }

    int apply(Context context, Bundle bundle) {
        String string;
        if (bundle != null) {
            saveStateIntoBundle(bundle);
        }
        ContentResolver contentResolver = context.getContentResolver();
        if (this.mMode == 0 && !TextUtils.isEmpty(this.mOldWord)) {
            UserDictionarySettings.deleteWord(this.mOldWord, this.mOldShortcut, contentResolver);
        }
        String string2 = this.mWordEditText.getText().toString();
        if (this.mShortcutEditText != null) {
            string = this.mShortcutEditText.getText().toString();
            if (TextUtils.isEmpty(string)) {
            }
        } else {
            string = null;
        }
        if (TextUtils.isEmpty(string2)) {
            return 1;
        }
        this.mSavedWord = string2;
        this.mSavedShortcut = string;
        if (TextUtils.isEmpty(string) && hasWord(string2, context)) {
            return 2;
        }
        UserDictionarySettings.deleteWord(string2, null, contentResolver);
        if (!TextUtils.isEmpty(string)) {
            UserDictionarySettings.deleteWord(string2, string, contentResolver);
        }
        UserDictionary.Words.addWord(context, string2.toString(), 250, string, TextUtils.isEmpty(this.mLocale) ? null : Utils.createLocaleFromString(this.mLocale));
        return 0;
    }

    private boolean hasWord(String str, Context context) {
        Cursor cursorQuery;
        if ("".equals(this.mLocale)) {
            cursorQuery = context.getContentResolver().query(UserDictionary.Words.CONTENT_URI, HAS_WORD_PROJECTION, "word=? AND locale is null", new String[]{str}, null);
        } else {
            cursorQuery = context.getContentResolver().query(UserDictionary.Words.CONTENT_URI, HAS_WORD_PROJECTION, "word=? AND locale=?", new String[]{str, this.mLocale}, null);
        }
        if (cursorQuery == null) {
            return false;
        }
        try {
            boolean z = cursorQuery.getCount() > 0;
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return z;
        } finally {
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        }
    }

    public static class LocaleRenderer {
        private final String mDescription;
        private final String mLocaleString;

        public LocaleRenderer(Context context, String str) {
            this.mLocaleString = str;
            if (str == null) {
                this.mDescription = context.getString(R.string.user_dict_settings_more_languages);
            } else if ("".equals(str)) {
                this.mDescription = context.getString(R.string.user_dict_settings_all_languages);
            } else {
                this.mDescription = Utils.createLocaleFromString(str).getDisplayName();
            }
        }

        public String toString() {
            return this.mDescription;
        }
    }

    private static void addLocaleDisplayNameToList(Context context, ArrayList<LocaleRenderer> arrayList, String str) {
        if (str != null) {
            arrayList.add(new LocaleRenderer(context, str));
        }
    }

    public ArrayList<LocaleRenderer> getLocalesList(Activity activity) {
        TreeSet<String> userDictionaryLocalesSet = UserDictionaryList.getUserDictionaryLocalesSet(activity);
        userDictionaryLocalesSet.remove(this.mLocale);
        String string = Locale.getDefault().toString();
        userDictionaryLocalesSet.remove(string);
        userDictionaryLocalesSet.remove("");
        ArrayList<LocaleRenderer> arrayList = new ArrayList<>();
        addLocaleDisplayNameToList(activity, arrayList, this.mLocale);
        if (!string.equals(this.mLocale)) {
            addLocaleDisplayNameToList(activity, arrayList, string);
        }
        Iterator<String> it = userDictionaryLocalesSet.iterator();
        while (it.hasNext()) {
            addLocaleDisplayNameToList(activity, arrayList, it.next());
        }
        if (!"".equals(this.mLocale)) {
            addLocaleDisplayNameToList(activity, arrayList, "");
        }
        arrayList.add(new LocaleRenderer(activity, null));
        return arrayList;
    }

    public String getCurrentUserDictionaryLocale() {
        return this.mLocale;
    }
}
