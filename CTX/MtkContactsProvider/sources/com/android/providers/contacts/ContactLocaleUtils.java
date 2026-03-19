package com.android.providers.contacts;

import android.icu.text.AlphabeticIndex;
import android.icu.text.Transliterator;
import android.os.LocaleList;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import com.android.providers.contacts.HanziToPinyin;
import java.lang.Character;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ContactLocaleUtils {
    private static ContactLocaleUtils sSingleton;
    private final LocaleSet mLocales;
    private final ContactLocaleUtilsBase mUtils;
    public static final Locale LOCALE_ARABIC = new Locale("ar");
    public static final Locale LOCALE_GREEK = new Locale("el");
    public static final Locale LOCALE_HEBREW = new Locale("he");
    public static final Locale LOCALE_SERBIAN = new Locale("sr");
    public static final Locale LOCALE_UKRAINIAN = new Locale("uk");
    public static final Locale LOCALE_THAI = new Locale("th");
    private static final Locale[] sDefaultLabelLocales = {Locale.ENGLISH, Locale.JAPANESE, Locale.KOREAN, LOCALE_THAI, LOCALE_ARABIC, LOCALE_HEBREW, LOCALE_GREEK, LOCALE_UKRAINIAN, LOCALE_SERBIAN};

    static void dumpIndex(AlphabeticIndex.ImmutableIndex immutableIndex) {
        StringBuilder sb = new StringBuilder();
        String str = "";
        for (int i = 0; i < immutableIndex.getBucketCount(); i++) {
            sb.append(str);
            sb.append(immutableIndex.getBucket(i).getLabel());
            str = ",";
        }
        Log.d("ContactLocale", "Labels=[" + ((Object) sb) + "]");
    }

    private static class ContactLocaleUtilsBase {
        protected final AlphabeticIndex.ImmutableIndex mAlphabeticIndex;
        private final int mAlphabeticIndexBucketCount;
        private final int mNumberBucketIndex;
        private final boolean mUsePinyinTransliterator;

        public ContactLocaleUtilsBase(LocaleSet localeSet) {
            this.mUsePinyinTransliterator = localeSet.shouldPreferSimplifiedChinese();
            List<Locale> localesForBuckets = getLocalesForBuckets(localeSet);
            AlphabeticIndex maxLabelCount = new AlphabeticIndex(localesForBuckets.get(0)).setMaxLabelCount(300);
            for (int i = 1; i < localesForBuckets.size(); i++) {
                maxLabelCount.addLabels(localesForBuckets.get(i));
            }
            this.mAlphabeticIndex = maxLabelCount.buildImmutableIndex();
            this.mAlphabeticIndexBucketCount = this.mAlphabeticIndex.getBucketCount();
            this.mNumberBucketIndex = this.mAlphabeticIndexBucketCount - 1;
        }

        static List<Locale> getLocalesForBuckets(LocaleSet localeSet) {
            LocaleList allLocales = localeSet.getAllLocales();
            ArrayList arrayList = new ArrayList(allLocales.size() + ContactLocaleUtils.sDefaultLabelLocales.length);
            for (int i = 0; i < allLocales.size(); i++) {
                arrayList.add(allLocales.get(i));
            }
            for (int i2 = 0; i2 < ContactLocaleUtils.sDefaultLabelLocales.length; i2++) {
                arrayList.add(ContactLocaleUtils.sDefaultLabelLocales[i2]);
            }
            ArrayList arrayList2 = new ArrayList(arrayList.size());
            boolean z = true;
            for (int i3 = 0; i3 < arrayList.size(); i3++) {
                Locale locale = (Locale) arrayList.get(i3);
                if (!arrayList2.contains(locale)) {
                    if (!LocaleSet.isLanguageChinese(locale)) {
                        if (LocaleSet.isLanguageJapanese(locale)) {
                            z = false;
                        }
                        arrayList2.add(locale);
                    } else if (z) {
                        z = false;
                        if (LocaleSet.isLanguageJapanese(locale)) {
                        }
                        arrayList2.add(locale);
                    }
                }
            }
            return arrayList2;
        }

        public int getNumberBucketIndex() {
            return this.mNumberBucketIndex;
        }

        public int getBucketIndex(String str) {
            int length = str.length();
            boolean z = false;
            int iCharCount = 0;
            while (true) {
                if (iCharCount >= length) {
                    break;
                }
                int iCodePointAt = Character.codePointAt(str, iCharCount);
                if (!Character.isDigit(iCodePointAt)) {
                    if (!Character.isSpaceChar(iCodePointAt) && iCodePointAt != 43 && iCodePointAt != 40 && iCodePointAt != 41 && iCodePointAt != 46 && iCodePointAt != 45 && iCodePointAt != 35) {
                        break;
                    }
                    iCharCount += Character.charCount(iCodePointAt);
                } else {
                    z = true;
                    break;
                }
            }
            if (z) {
                return this.mNumberBucketIndex;
            }
            if (this.mUsePinyinTransliterator) {
                str = HanziToPinyin.getInstance().transliterate(str);
            }
            int bucketIndex = this.mAlphabeticIndex.getBucketIndex(str);
            if (bucketIndex < 0) {
                return -1;
            }
            if (bucketIndex >= this.mNumberBucketIndex) {
                return bucketIndex + 1;
            }
            return bucketIndex;
        }

        public int getBucketCount() {
            return this.mAlphabeticIndexBucketCount + 1;
        }

        public String getBucketLabel(int i) {
            if (i < 0 || i >= getBucketCount()) {
                return "";
            }
            if (i == this.mNumberBucketIndex) {
                return "#";
            }
            if (i > this.mNumberBucketIndex) {
                i--;
            }
            return this.mAlphabeticIndex.getBucket(i).getLabel();
        }

        public Iterator<String> getNameLookupKeys(String str, int i) {
            return null;
        }

        public ArrayList<String> getLabels() {
            int bucketCount = getBucketCount();
            ArrayList<String> arrayList = new ArrayList<>(bucketCount);
            for (int i = 0; i < bucketCount; i++) {
                arrayList.add(getBucketLabel(i));
            }
            return arrayList;
        }
    }

    private static class JapaneseContactUtils extends ContactLocaleUtilsBase {
        private static final Set<Character.UnicodeBlock> CJ_BLOCKS;
        private static boolean mInitializedTransliterator;
        private static Transliterator mJapaneseTransliterator;
        private final int mMiscBucketIndex;

        public JapaneseContactUtils(LocaleSet localeSet) {
            super(localeSet);
            this.mMiscBucketIndex = super.getBucketIndex("日");
        }

        static {
            ArraySet arraySet = new ArraySet();
            arraySet.add(Character.UnicodeBlock.HIRAGANA);
            arraySet.add(Character.UnicodeBlock.KATAKANA);
            arraySet.add(Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS);
            arraySet.add(Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS);
            arraySet.add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS);
            arraySet.add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A);
            arraySet.add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B);
            arraySet.add(Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION);
            arraySet.add(Character.UnicodeBlock.CJK_RADICALS_SUPPLEMENT);
            arraySet.add(Character.UnicodeBlock.CJK_COMPATIBILITY);
            arraySet.add(Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS);
            arraySet.add(Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS);
            arraySet.add(Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT);
            CJ_BLOCKS = Collections.unmodifiableSet(arraySet);
        }

        private static boolean isChineseOrJapanese(int i) {
            return CJ_BLOCKS.contains(Character.UnicodeBlock.of(i));
        }

        @Override
        public int getBucketIndex(String str) {
            int bucketIndex = super.getBucketIndex(str);
            if ((bucketIndex == this.mMiscBucketIndex && !isChineseOrJapanese(Character.codePointAt(str, 0))) || bucketIndex > this.mMiscBucketIndex) {
                return bucketIndex + 1;
            }
            return bucketIndex;
        }

        @Override
        public int getBucketCount() {
            return super.getBucketCount() + 1;
        }

        @Override
        public String getBucketLabel(int i) {
            if (i == this.mMiscBucketIndex) {
                return "他";
            }
            if (i > this.mMiscBucketIndex) {
                i--;
            }
            return super.getBucketLabel(i);
        }

        @Override
        public Iterator<String> getNameLookupKeys(String str, int i) {
            if (i == 4) {
                return getRomajiNameLookupKeys(str);
            }
            return null;
        }

        private static Transliterator getJapaneseTransliterator() {
            Transliterator transliterator;
            synchronized (JapaneseContactUtils.class) {
                if (!mInitializedTransliterator) {
                    mInitializedTransliterator = true;
                    Transliterator transliterator2 = null;
                    try {
                        transliterator2 = Transliterator.getInstance("Hiragana-Latin; Katakana-Latin; Latin-Ascii");
                    } catch (IllegalArgumentException e) {
                        Log.w("ContactLocale", "Hiragana/Katakana-Latin transliterator data is missing");
                    }
                    mJapaneseTransliterator = transliterator2;
                    transliterator = mJapaneseTransliterator;
                } else {
                    transliterator = mJapaneseTransliterator;
                }
            }
            return transliterator;
        }

        public static Iterator<String> getRomajiNameLookupKeys(String str) {
            Transliterator japaneseTransliterator = getJapaneseTransliterator();
            if (japaneseTransliterator == null) {
                return null;
            }
            String strTransliterate = japaneseTransliterator.transliterate(str);
            if (TextUtils.isEmpty(strTransliterate) || TextUtils.equals(str, strTransliterate)) {
                return null;
            }
            ArraySet arraySet = new ArraySet();
            arraySet.add(strTransliterate);
            return arraySet.iterator();
        }

        @Override
        public int getNumberBucketIndex() {
            int numberBucketIndex = super.getNumberBucketIndex();
            if (numberBucketIndex > this.mMiscBucketIndex) {
                return numberBucketIndex + 1;
            }
            return numberBucketIndex;
        }
    }

    private static class SimplifiedChineseContactUtils extends ContactLocaleUtilsBase {
        public SimplifiedChineseContactUtils(LocaleSet localeSet) {
            super(localeSet);
        }

        @Override
        public Iterator<String> getNameLookupKeys(String str, int i) {
            if (i != 4 && i != 5) {
                return getPinyinNameLookupKeys(str);
            }
            return null;
        }

        public static Iterator<String> getPinyinNameLookupKeys(String str) {
            ArraySet arraySet = new ArraySet();
            ArrayList<HanziToPinyin.Token> tokens = HanziToPinyin.getInstance().getTokens(str);
            int size = tokens.size();
            StringBuilder sb = new StringBuilder();
            StringBuilder sb2 = new StringBuilder();
            StringBuilder sb3 = new StringBuilder();
            for (int i = size - 1; i >= 0; i--) {
                HanziToPinyin.Token token = tokens.get(i);
                if (3 != token.type) {
                    if (2 != token.type) {
                        if (1 == token.type) {
                            if (sb.length() > 0) {
                                sb.insert(0, ' ');
                            }
                            if (sb3.length() > 0) {
                                sb3.insert(0, ' ');
                            }
                            sb.insert(0, token.source);
                            sb2.insert(0, token.source.charAt(0));
                        }
                    } else {
                        sb.insert(0, token.target);
                        sb2.insert(0, token.target.charAt(0));
                    }
                    sb3.insert(0, token.source);
                    arraySet.add(sb3.toString());
                    arraySet.add(sb.toString());
                    arraySet.add(sb2.toString());
                }
            }
            return arraySet.iterator();
        }
    }

    private ContactLocaleUtils(LocaleSet localeSet) {
        if (localeSet == null) {
            this.mLocales = LocaleSet.newDefault();
        } else {
            this.mLocales = localeSet;
        }
        if (this.mLocales.shouldPreferJapanese()) {
            this.mUtils = new JapaneseContactUtils(this.mLocales);
        } else if (this.mLocales.shouldPreferSimplifiedChinese()) {
            this.mUtils = new SimplifiedChineseContactUtils(this.mLocales);
        } else {
            this.mUtils = new ContactLocaleUtilsBase(this.mLocales);
        }
        Log.i("ContactLocale", "AddressBook Labels [" + this.mLocales.toString() + "]: " + getLabels().toString());
    }

    public boolean isLocale(LocaleSet localeSet) {
        return this.mLocales.equals(localeSet);
    }

    public static synchronized ContactLocaleUtils getInstance() {
        if (sSingleton == null) {
            sSingleton = new ContactLocaleUtils(LocaleSet.newDefault());
        }
        return sSingleton;
    }

    public static ContactLocaleUtils newInstanceForTest(Locale... localeArr) {
        return new ContactLocaleUtils(LocaleSet.newForTest(localeArr));
    }

    public static synchronized void setLocaleForTest(Locale... localeArr) {
        setLocales(LocaleSet.newForTest(localeArr));
    }

    public static synchronized void setLocales(LocaleSet localeSet) {
        if (sSingleton == null || !sSingleton.isLocale(localeSet)) {
            sSingleton = new ContactLocaleUtils(localeSet);
        }
    }

    public int getBucketIndex(String str) {
        return this.mUtils.getBucketIndex(str);
    }

    public int getNumberBucketIndex() {
        return this.mUtils.getNumberBucketIndex();
    }

    public String getBucketLabel(int i) {
        return this.mUtils.getBucketLabel(i);
    }

    public ArrayList<String> getLabels() {
        return this.mUtils.getLabels();
    }

    public Iterator<String> getNameLookupKeys(String str, int i) {
        if (!this.mLocales.isPrimaryLocaleCJK()) {
            if (this.mLocales.shouldPreferSimplifiedChinese()) {
                if (i == 3 || i == 2) {
                    return SimplifiedChineseContactUtils.getPinyinNameLookupKeys(str);
                }
            } else if (i == 4) {
                return JapaneseContactUtils.getRomajiNameLookupKeys(str);
            }
        }
        return this.mUtils.getNameLookupKeys(str, i);
    }
}
