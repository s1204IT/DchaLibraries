package com.android.settingslib.inputmethod;

import android.content.Context;
import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.inputmethod.InputMethodUtils;
import java.util.Locale;

/* loaded from: classes.dex */
public class InputMethodSubtypePreference extends SwitchWithNoTextPreference {
    private final boolean mIsSystemLanguage;
    private final boolean mIsSystemLocale;

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
}
