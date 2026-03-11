package com.android.settingslib.inputmethod;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodSubtype;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.inputmethod.InputMethodUtils;
import java.text.Collator;
import java.util.Locale;

public class InputMethodSubtypePreference extends SwitchWithNoTextPreference {
    private final boolean mIsSystemLanguage;
    private final boolean mIsSystemLocale;

    public InputMethodSubtypePreference(Context context, InputMethodSubtype inputMethodSubtype, InputMethodInfo inputMethodInfo) {
        this(context, inputMethodInfo.getId() + inputMethodSubtype.hashCode(), InputMethodAndSubtypeUtil.getSubtypeLocaleNameAsSentence(inputMethodSubtype, context, inputMethodInfo), inputMethodSubtype.getLocale(), context.getResources().getConfiguration().locale);
    }

    @VisibleForTesting
    InputMethodSubtypePreference(Context context, String str, CharSequence charSequence, String str2, Locale locale) {
        super(context);
        setPersistent(false);
        setKey(str);
        setTitle(charSequence);
        if (TextUtils.isEmpty(str2)) {
            this.mIsSystemLocale = false;
            this.mIsSystemLanguage = false;
        } else {
            this.mIsSystemLocale = str2.equals(locale.toString());
            this.mIsSystemLanguage = this.mIsSystemLocale || InputMethodUtils.getLanguageFromLocaleString(str2).equals(locale.getLanguage());
        }
    }

    public int compareTo(Preference preference, Collator collator) {
        if (this == preference) {
            return 0;
        }
        if (preference instanceof InputMethodSubtypePreference) {
            InputMethodSubtypePreference inputMethodSubtypePreference = (InputMethodSubtypePreference) preference;
            if (this.mIsSystemLocale && !inputMethodSubtypePreference.mIsSystemLocale) {
                return -1;
            }
            if (!this.mIsSystemLocale && inputMethodSubtypePreference.mIsSystemLocale) {
                return 1;
            }
            if (this.mIsSystemLanguage && !inputMethodSubtypePreference.mIsSystemLanguage) {
                return -1;
            }
            if (!this.mIsSystemLanguage && inputMethodSubtypePreference.mIsSystemLanguage) {
                return 1;
            }
            CharSequence title = getTitle();
            CharSequence title2 = preference.getTitle();
            boolean zIsEmpty = TextUtils.isEmpty(title);
            boolean zIsEmpty2 = TextUtils.isEmpty(title2);
            if (zIsEmpty || zIsEmpty2) {
                return (zIsEmpty ? -1 : 0) - (zIsEmpty2 ? -1 : 0);
            }
            return collator.compare(title.toString(), title2.toString());
        }
        return super.compareTo(preference);
    }
}
