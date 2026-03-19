package com.android.providers.contacts;

import android.icu.util.ULocale;
import android.os.LocaleList;
import java.util.Locale;
import java.util.Objects;

public class LocaleSet {
    private final Locale mDefaultLocaleOverrideForTest;
    private final LocaleList mLocaleList;

    private LocaleSet(LocaleList localeList, Locale locale) {
        this.mLocaleList = localeList;
        this.mDefaultLocaleOverrideForTest = locale;
    }

    public static LocaleSet newDefault() {
        return new LocaleSet(LocaleList.getDefault(), null);
    }

    public static LocaleSet newForTest(Locale... localeArr) {
        return new LocaleSet(new LocaleList(localeArr), localeArr[0]);
    }

    static boolean isLanguageChinese(Locale locale) {
        return locale != null && "zh".equals(locale.getLanguage());
    }

    static boolean isLanguageJapanese(Locale locale) {
        return locale != null && "ja".equals(locale.getLanguage());
    }

    static boolean isLanguageKorean(Locale locale) {
        return locale != null && "ko".equals(locale.getLanguage());
    }

    static boolean isLocaleCJK(Locale locale) {
        return isLanguageChinese(locale) || isLanguageJapanese(locale) || isLanguageKorean(locale);
    }

    private static String getLikelyScript(Locale locale) {
        String script = locale.getScript();
        if (!script.isEmpty()) {
            return script;
        }
        return ULocale.addLikelySubtags(ULocale.forLocale(locale)).getScript();
    }

    static String getScriptIfChinese(Locale locale) {
        if (isLanguageChinese(locale)) {
            return getLikelyScript(locale);
        }
        return null;
    }

    static boolean isLocaleSimplifiedChinese(Locale locale) {
        return "Hans".equals(getScriptIfChinese(locale));
    }

    static boolean isLocaleTraditionalChinese(Locale locale) {
        return "Hant".equals(getScriptIfChinese(locale));
    }

    public Locale getPrimaryLocale() {
        if (this.mDefaultLocaleOverrideForTest != null) {
            return this.mDefaultLocaleOverrideForTest;
        }
        return Locale.getDefault();
    }

    public LocaleList getAllLocales() {
        return this.mLocaleList;
    }

    public boolean isPrimaryLocaleCJK() {
        return isLocaleCJK(getPrimaryLocale());
    }

    public boolean shouldPreferJapanese() {
        if (isLanguageJapanese(getPrimaryLocale())) {
            return true;
        }
        for (int i = 0; i < this.mLocaleList.size(); i++) {
            Locale locale = this.mLocaleList.get(i);
            if (isLanguageJapanese(locale)) {
                return true;
            }
            if (isLanguageChinese(locale)) {
                return false;
            }
        }
        return false;
    }

    public boolean shouldPreferSimplifiedChinese() {
        if (isLocaleSimplifiedChinese(getPrimaryLocale())) {
            return true;
        }
        for (int i = 0; i < this.mLocaleList.size(); i++) {
            Locale locale = this.mLocaleList.get(i);
            if (isLocaleSimplifiedChinese(locale)) {
                return true;
            }
            if (isLanguageJapanese(locale) || isLocaleTraditionalChinese(locale)) {
                return false;
            }
        }
        return false;
    }

    public boolean isCurrent() {
        return Objects.equals(this.mLocaleList, LocaleList.getDefault());
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof LocaleSet) {
            return this.mLocaleList.equals(((LocaleSet) obj).mLocaleList);
        }
        return false;
    }

    public final String toString() {
        return this.mLocaleList.toString();
    }
}
