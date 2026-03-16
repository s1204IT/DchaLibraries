package com.android.providers.contacts;

import android.text.TextUtils;
import java.util.Locale;

public class LocaleSet {
    private static final String CHINESE_LANGUAGE = Locale.CHINESE.getLanguage().toLowerCase();
    private static final String JAPANESE_LANGUAGE = Locale.JAPANESE.getLanguage().toLowerCase();
    private static final String KOREAN_LANGUAGE = Locale.KOREAN.getLanguage().toLowerCase();
    private final LocaleWrapper mPrimaryLocale;
    private final LocaleWrapper mSecondaryLocale;

    private static class LocaleWrapper {
        private final String mLanguage;
        private final Locale mLocale;
        private final boolean mLocaleIsCJK;

        private static boolean isLanguageCJK(String language) {
            return LocaleSet.CHINESE_LANGUAGE.equals(language) || LocaleSet.JAPANESE_LANGUAGE.equals(language) || LocaleSet.KOREAN_LANGUAGE.equals(language);
        }

        public LocaleWrapper(Locale locale) {
            this.mLocale = locale;
            if (this.mLocale != null) {
                this.mLanguage = this.mLocale.getLanguage().toLowerCase();
                this.mLocaleIsCJK = isLanguageCJK(this.mLanguage);
            } else {
                this.mLanguage = null;
                this.mLocaleIsCJK = false;
            }
        }

        public boolean hasLocale() {
            return this.mLocale != null;
        }

        public Locale getLocale() {
            return this.mLocale;
        }

        public boolean isLocale(Locale locale) {
            return this.mLocale == null ? locale == null : this.mLocale.equals(locale);
        }

        public boolean isLocaleCJK() {
            return this.mLocaleIsCJK;
        }

        public boolean isLanguage(String language) {
            return this.mLanguage == null ? language == null : this.mLanguage.equalsIgnoreCase(language);
        }

        public String toString() {
            return this.mLocale != null ? this.mLocale.toLanguageTag() : "(null)";
        }
    }

    public static LocaleSet getDefault() {
        return new LocaleSet(Locale.getDefault());
    }

    public LocaleSet(Locale locale) {
        this(locale, null);
    }

    public static LocaleSet getLocaleSet(String localeString) {
        Locale secondaryLocale;
        if (localeString != null && localeString.indexOf(95) == -1) {
            String[] locales = localeString.split(";");
            Locale primaryLocale = Locale.forLanguageTag(locales[0]);
            if (primaryLocale != null && !TextUtils.equals(primaryLocale.toLanguageTag(), "und")) {
                if (locales.length > 1 && locales[1] != null && (secondaryLocale = Locale.forLanguageTag(locales[1])) != null && !TextUtils.equals(secondaryLocale.toLanguageTag(), "und")) {
                    return new LocaleSet(primaryLocale, secondaryLocale);
                }
                return new LocaleSet(primaryLocale);
            }
        }
        return getDefault();
    }

    public LocaleSet(Locale primaryLocale, Locale secondaryLocale) {
        this.mPrimaryLocale = new LocaleWrapper(primaryLocale);
        this.mSecondaryLocale = new LocaleWrapper(this.mPrimaryLocale.equals(secondaryLocale) ? null : secondaryLocale);
    }

    public LocaleSet normalize() {
        Locale primaryLocale = getPrimaryLocale();
        if (primaryLocale == null) {
            return getDefault();
        }
        Locale secondaryLocale = getSecondaryLocale();
        if (secondaryLocale == null || isPrimaryLanguage(secondaryLocale.getLanguage()) || (isPrimaryLocaleCJK() && isSecondaryLocaleCJK())) {
            return new LocaleSet(primaryLocale);
        }
        if (isSecondaryLanguage(Locale.ENGLISH.getLanguage())) {
            return new LocaleSet(primaryLocale);
        }
        return this;
    }

    public boolean hasSecondaryLocale() {
        return this.mSecondaryLocale.hasLocale();
    }

    public Locale getPrimaryLocale() {
        return this.mPrimaryLocale.getLocale();
    }

    public Locale getSecondaryLocale() {
        return this.mSecondaryLocale.getLocale();
    }

    public boolean isPrimaryLocale(Locale locale) {
        return this.mPrimaryLocale.isLocale(locale);
    }

    public boolean isSecondaryLocale(Locale locale) {
        return this.mSecondaryLocale.isLocale(locale);
    }

    public static boolean isLocaleSimplifiedChinese(Locale locale) {
        if (locale == null || !TextUtils.equals(locale.getLanguage(), CHINESE_LANGUAGE)) {
            return false;
        }
        if (!TextUtils.isEmpty(locale.getScript())) {
            return locale.getScript().equals("Hans");
        }
        return locale.equals(Locale.SIMPLIFIED_CHINESE);
    }

    public boolean isPrimaryLocaleSimplifiedChinese() {
        return isLocaleSimplifiedChinese(getPrimaryLocale());
    }

    public boolean isSecondaryLocaleSimplifiedChinese() {
        return isLocaleSimplifiedChinese(getSecondaryLocale());
    }

    public static boolean isLocaleTraditionalChinese(Locale locale) {
        if (locale == null || !TextUtils.equals(locale.getLanguage(), CHINESE_LANGUAGE)) {
            return false;
        }
        if (!TextUtils.isEmpty(locale.getScript())) {
            return locale.getScript().equals("Hant");
        }
        return locale.equals(Locale.TRADITIONAL_CHINESE);
    }

    public boolean isPrimaryLocaleCJK() {
        return this.mPrimaryLocale.isLocaleCJK();
    }

    public boolean isSecondaryLocaleCJK() {
        return this.mSecondaryLocale.isLocaleCJK();
    }

    public boolean isPrimaryLanguage(String language) {
        return this.mPrimaryLocale.isLanguage(language);
    }

    public boolean isSecondaryLanguage(String language) {
        return this.mSecondaryLocale.isLanguage(language);
    }

    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof LocaleSet)) {
            return false;
        }
        LocaleSet other = (LocaleSet) object;
        return other.isPrimaryLocale(this.mPrimaryLocale.getLocale()) && other.isSecondaryLocale(this.mSecondaryLocale.getLocale());
    }

    public final String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.mPrimaryLocale.toString());
        if (hasSecondaryLocale()) {
            builder.append(";");
            builder.append(this.mSecondaryLocale.toString());
        }
        return builder.toString();
    }
}
