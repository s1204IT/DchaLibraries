package com.android.internal.app;

import android.content.Context;
import android.os.LocaleList;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import com.android.internal.content.NativeLibraryHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IllformedLocaleException;
import java.util.Locale;
import java.util.Set;

public class LocaleStore {
    private static final HashMap<String, LocaleInfo> sLocaleCache = new HashMap<>();
    private static boolean sFullyInitialized = false;

    public static class LocaleInfo {
        private static final int SUGGESTION_TYPE_CFG = 2;
        private static final int SUGGESTION_TYPE_NONE = 0;
        private static final int SUGGESTION_TYPE_SIM = 1;
        private String mFullCountryNameNative;
        private String mFullNameNative;
        private final String mId;
        private boolean mIsChecked;
        private boolean mIsPseudo;
        private boolean mIsTranslated;
        private String mLangScriptKey;
        private final Locale mLocale;
        private final Locale mParent;
        private int mSuggestionFlags;

        static int access$076(LocaleInfo localeInfo, int i) {
            int i2 = i | localeInfo.mSuggestionFlags;
            localeInfo.mSuggestionFlags = i2;
            return i2;
        }

        private LocaleInfo(Locale locale) {
            this.mLocale = locale;
            this.mId = locale.toLanguageTag();
            this.mParent = getParent(locale);
            this.mIsChecked = false;
            this.mSuggestionFlags = 0;
            this.mIsTranslated = false;
            this.mIsPseudo = false;
        }

        private LocaleInfo(String str) {
            this(Locale.forLanguageTag(str));
        }

        private static Locale getParent(Locale locale) {
            if (locale.getCountry().isEmpty()) {
                return null;
            }
            return new Locale.Builder().setLocale(locale).setRegion("").setExtension('u', "").build();
        }

        public String toString() {
            return this.mId;
        }

        public Locale getLocale() {
            return this.mLocale;
        }

        public Locale getParent() {
            return this.mParent;
        }

        public String getId() {
            return this.mId;
        }

        public boolean isTranslated() {
            return this.mIsTranslated;
        }

        public void setTranslated(boolean z) {
            this.mIsTranslated = z;
        }

        boolean isSuggested() {
            return this.mIsTranslated && this.mSuggestionFlags != 0;
        }

        private boolean isSuggestionOfType(int i) {
            return this.mIsTranslated && (this.mSuggestionFlags & i) == i;
        }

        public String getFullNameNative() {
            if (this.mFullNameNative == null) {
                this.mFullNameNative = LocaleHelper.getDisplayName(this.mLocale, this.mLocale, true);
            }
            return this.mFullNameNative;
        }

        String getFullCountryNameNative() {
            if (this.mFullCountryNameNative == null) {
                this.mFullCountryNameNative = LocaleHelper.getDisplayCountry(this.mLocale, this.mLocale);
            }
            return this.mFullCountryNameNative;
        }

        String getFullCountryNameInUiLanguage() {
            return LocaleHelper.getDisplayCountry(this.mLocale);
        }

        public String getFullNameInUiLanguage() {
            return LocaleHelper.getDisplayName(this.mLocale, true);
        }

        private String getLangScriptKey() {
            String languageTag;
            if (this.mLangScriptKey == null) {
                Locale parent = getParent(LocaleHelper.addLikelySubtags(new Locale.Builder().setLocale(this.mLocale).setExtension('u', "").build()));
                if (parent == null) {
                    languageTag = this.mLocale.toLanguageTag();
                } else {
                    languageTag = parent.toLanguageTag();
                }
                this.mLangScriptKey = languageTag;
            }
            return this.mLangScriptKey;
        }

        String getLabel(boolean z) {
            if (z) {
                return getFullCountryNameNative();
            }
            return getFullNameNative();
        }

        String getContentDescription(boolean z) {
            if (z) {
                return getFullCountryNameInUiLanguage();
            }
            return getFullNameInUiLanguage();
        }

        public boolean getChecked() {
            return this.mIsChecked;
        }

        public void setChecked(boolean z) {
            this.mIsChecked = z;
        }
    }

    private static Set<String> getSimCountries(Context context) {
        HashSet hashSet = new HashSet();
        TelephonyManager telephonyManagerFrom = TelephonyManager.from(context);
        if (telephonyManagerFrom != null) {
            String upperCase = telephonyManagerFrom.getSimCountryIso().toUpperCase(Locale.US);
            if (!upperCase.isEmpty()) {
                hashSet.add(upperCase);
            }
            String upperCase2 = telephonyManagerFrom.getNetworkCountryIso().toUpperCase(Locale.US);
            if (!upperCase2.isEmpty()) {
                hashSet.add(upperCase2);
            }
        }
        return hashSet;
    }

    public static void updateSimCountries(Context context) {
        Set<String> simCountries = getSimCountries(context);
        for (LocaleInfo localeInfo : sLocaleCache.values()) {
            if (simCountries.contains(localeInfo.getLocale().getCountry())) {
                LocaleInfo.access$076(localeInfo, 1);
            }
        }
    }

    private static void addSuggestedLocalesForRegion(Locale locale) {
        if (locale == null) {
            return;
        }
        String country = locale.getCountry();
        if (country.isEmpty()) {
            return;
        }
        for (LocaleInfo localeInfo : sLocaleCache.values()) {
            if (country.equals(localeInfo.getLocale().getCountry())) {
                LocaleInfo.access$076(localeInfo, 1);
            }
        }
    }

    public static void fillCache(Context context) {
        LocaleInfo localeInfo;
        Locale parent;
        if (sFullyInitialized) {
            return;
        }
        Set<String> simCountries = getSimCountries(context);
        boolean z = Settings.Global.getInt(context.getContentResolver(), "development_settings_enabled", 0) != 0;
        String[] supportedLocales = LocalePicker.getSupportedLocales(context);
        int length = supportedLocales.length;
        int i = 0;
        while (true) {
            if (i < length) {
                String str = supportedLocales[i];
                if (str.isEmpty()) {
                    throw new IllformedLocaleException("Bad locale entry in locale_config.xml");
                }
                LocaleInfo localeInfo2 = new LocaleInfo(str);
                if (LocaleList.isPseudoLocale(localeInfo2.getLocale())) {
                    if (z) {
                        localeInfo2.setTranslated(true);
                        localeInfo2.mIsPseudo = true;
                        LocaleInfo.access$076(localeInfo2, 1);
                        if (simCountries.contains(localeInfo2.getLocale().getCountry())) {
                        }
                        sLocaleCache.put(localeInfo2.getId(), localeInfo2);
                        parent = localeInfo2.getParent();
                        if (parent == null) {
                        }
                    }
                } else {
                    if (simCountries.contains(localeInfo2.getLocale().getCountry())) {
                        LocaleInfo.access$076(localeInfo2, 1);
                    }
                    sLocaleCache.put(localeInfo2.getId(), localeInfo2);
                    parent = localeInfo2.getParent();
                    if (parent == null) {
                        String languageTag = parent.toLanguageTag();
                        if (!sLocaleCache.containsKey(languageTag)) {
                            sLocaleCache.put(languageTag, new LocaleInfo(parent));
                        }
                    }
                }
                i++;
            } else {
                HashSet hashSet = new HashSet();
                for (String str2 : LocalePicker.getSystemAssetLocales()) {
                    LocaleInfo localeInfo3 = new LocaleInfo(str2);
                    String country = localeInfo3.getLocale().getCountry();
                    if (!country.isEmpty()) {
                        if (sLocaleCache.containsKey(localeInfo3.getId())) {
                            localeInfo = sLocaleCache.get(localeInfo3.getId());
                        } else {
                            String str3 = localeInfo3.getLangScriptKey() + NativeLibraryHelper.CLEAR_ABI_OVERRIDE + country;
                            if (sLocaleCache.containsKey(str3)) {
                                localeInfo = sLocaleCache.get(str3);
                            } else {
                                localeInfo = null;
                            }
                        }
                        if (localeInfo != null) {
                            LocaleInfo.access$076(localeInfo, 2);
                        }
                    }
                    hashSet.add(localeInfo3.getLangScriptKey());
                }
                for (LocaleInfo localeInfo4 : sLocaleCache.values()) {
                    localeInfo4.setTranslated(hashSet.contains(localeInfo4.getLangScriptKey()));
                }
                addSuggestedLocalesForRegion(Locale.getDefault());
                sFullyInitialized = true;
                return;
            }
        }
    }

    private static int getLevel(Set<String> set, LocaleInfo localeInfo, boolean z) {
        if (set.contains(localeInfo.getId())) {
            return 0;
        }
        if (localeInfo.mIsPseudo) {
            return 2;
        }
        return ((!z || localeInfo.isTranslated()) && localeInfo.getParent() != null) ? 2 : 0;
    }

    public static Set<LocaleInfo> getLevelLocales(Context context, Set<String> set, LocaleInfo localeInfo, boolean z) {
        fillCache(context);
        String id = localeInfo == null ? null : localeInfo.getId();
        HashSet hashSet = new HashSet();
        ArrayList<LocaleInfo> arrayList = new ArrayList();
        arrayList.addAll(sLocaleCache.values());
        for (LocaleInfo localeInfo2 : arrayList) {
            if (getLevel(set, localeInfo2, z) == 2) {
                if (localeInfo == null) {
                    if (localeInfo2.isSuggestionOfType(1)) {
                        hashSet.add(localeInfo2);
                    } else {
                        hashSet.add(getLocaleInfo(localeInfo2.getParent()));
                    }
                } else if (id.equals(localeInfo2.getParent().toLanguageTag())) {
                    hashSet.add(localeInfo2);
                }
            }
        }
        return hashSet;
    }

    public static LocaleInfo getLocaleInfo(Locale locale) {
        String languageTag = locale.toLanguageTag();
        if (!sLocaleCache.containsKey(languageTag)) {
            LocaleInfo localeInfo = new LocaleInfo(locale);
            sLocaleCache.put(languageTag, localeInfo);
            return localeInfo;
        }
        return sLocaleCache.get(languageTag);
    }
}
