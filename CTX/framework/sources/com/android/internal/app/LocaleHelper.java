package com.android.internal.app;

import android.icu.text.ListFormatter;
import android.icu.util.ULocale;
import android.os.LocaleList;
import android.text.TextUtils;
import com.android.internal.app.LocaleStore;
import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import libcore.icu.ICU;

public class LocaleHelper {
    public static String toSentenceCase(String str, Locale locale) {
        if (str.isEmpty()) {
            return str;
        }
        int iOffsetByCodePoints = str.offsetByCodePoints(0, 1);
        return str.substring(0, iOffsetByCodePoints).toUpperCase(locale) + str.substring(iOffsetByCodePoints);
    }

    public static String normalizeForSearch(String str, Locale locale) {
        return str.toUpperCase();
    }

    private static boolean shouldUseDialectName(Locale locale) {
        String language = locale.getLanguage();
        return "fa".equals(language) || "ro".equals(language) || "zh".equals(language);
    }

    public static String getDisplayName(Locale locale, Locale locale2, boolean z) {
        String displayName;
        ULocale uLocaleForLocale = ULocale.forLocale(locale2);
        if (shouldUseDialectName(locale)) {
            displayName = ULocale.getDisplayNameWithDialect(locale.toLanguageTag(), uLocaleForLocale);
        } else {
            displayName = ULocale.getDisplayName(locale.toLanguageTag(), uLocaleForLocale);
        }
        return z ? toSentenceCase(displayName, locale2) : displayName;
    }

    public static String getDisplayName(Locale locale, boolean z) {
        return getDisplayName(locale, Locale.getDefault(), z);
    }

    public static String getDisplayCountry(Locale locale, Locale locale2) {
        String languageTag = locale.toLanguageTag();
        ULocale uLocaleForLocale = ULocale.forLocale(locale2);
        String displayCountry = ULocale.getDisplayCountry(languageTag, uLocaleForLocale);
        if (locale.getUnicodeLocaleType("nu") != null) {
            return String.format("%s (%s)", displayCountry, ULocale.getDisplayKeywordValue(languageTag, "numbers", uLocaleForLocale));
        }
        return displayCountry;
    }

    public static String getDisplayCountry(Locale locale) {
        return ULocale.getDisplayCountry(locale.toLanguageTag(), ULocale.getDefault());
    }

    public static String getDisplayLocaleList(LocaleList localeList, Locale locale, int i) {
        int size;
        int i2;
        if (locale == null) {
            locale = Locale.getDefault();
        }
        boolean z = localeList.size() > i;
        if (z) {
            size = i + 1;
            i2 = i;
        } else {
            size = localeList.size();
            i2 = size;
        }
        String[] strArr = new String[size];
        for (int i3 = 0; i3 < i2; i3++) {
            strArr[i3] = getDisplayName(localeList.get(i3), locale, false);
        }
        if (z) {
            strArr[i] = TextUtils.getEllipsisString(TextUtils.TruncateAt.END);
        }
        return ListFormatter.getInstance(locale).format(strArr);
    }

    public static Locale addLikelySubtags(Locale locale) {
        return ICU.addLikelySubtags(locale);
    }

    public static final class LocaleInfoComparator implements Comparator<LocaleStore.LocaleInfo> {
        private static final String PREFIX_ARABIC = "ال";
        private final Collator mCollator;
        private final boolean mCountryMode;

        public LocaleInfoComparator(Locale locale, boolean z) {
            this.mCollator = Collator.getInstance(locale);
            this.mCountryMode = z;
        }

        private String removePrefixForCompare(Locale locale, String str) {
            if ("ar".equals(locale.getLanguage()) && str.startsWith(PREFIX_ARABIC)) {
                return str.substring(PREFIX_ARABIC.length());
            }
            return str;
        }

        @Override
        public int compare(LocaleStore.LocaleInfo localeInfo, LocaleStore.LocaleInfo localeInfo2) {
            if (localeInfo.isSuggested() == localeInfo2.isSuggested()) {
                return this.mCollator.compare(removePrefixForCompare(localeInfo.getLocale(), localeInfo.getLabel(this.mCountryMode)), removePrefixForCompare(localeInfo2.getLocale(), localeInfo2.getLabel(this.mCountryMode)));
            }
            return localeInfo.isSuggested() ? -1 : 1;
        }
    }
}
