package com.android.providers.contacts;

import android.text.TextUtils;
import android.util.Log;
import com.android.providers.contacts.HanziToPinyin;
import java.lang.Character;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import libcore.icu.AlphabeticIndex;
import libcore.icu.Transliterator;

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
    private static final String JAPANESE_LANGUAGE = Locale.JAPANESE.getLanguage().toLowerCase();

    private static class ContactLocaleUtilsBase {
        protected final AlphabeticIndex.ImmutableIndex mAlphabeticIndex;
        private final int mAlphabeticIndexBucketCount;
        private final boolean mEnableSecondaryLocalePinyin;
        private final int mNumberBucketIndex;

        public ContactLocaleUtilsBase(LocaleSet locales) {
            Locale secondaryLocale = locales.getSecondaryLocale();
            this.mEnableSecondaryLocalePinyin = locales.isSecondaryLocaleSimplifiedChinese();
            AlphabeticIndex ai = new AlphabeticIndex(locales.getPrimaryLocale()).setMaxLabelCount(300);
            if (secondaryLocale != null) {
                ai.addLabels(secondaryLocale);
            }
            this.mAlphabeticIndex = ai.addLabels(Locale.ENGLISH).addLabels(Locale.JAPANESE).addLabels(Locale.KOREAN).addLabels(ContactLocaleUtils.LOCALE_THAI).addLabels(ContactLocaleUtils.LOCALE_ARABIC).addLabels(ContactLocaleUtils.LOCALE_HEBREW).addLabels(ContactLocaleUtils.LOCALE_GREEK).addLabels(ContactLocaleUtils.LOCALE_UKRAINIAN).addLabels(ContactLocaleUtils.LOCALE_SERBIAN).getImmutableIndex();
            this.mAlphabeticIndexBucketCount = this.mAlphabeticIndex.getBucketCount();
            this.mNumberBucketIndex = this.mAlphabeticIndexBucketCount - 1;
        }

        public int getBucketIndex(String name) {
            boolean prefixIsNumeric = false;
            int length = name.length();
            int offset = 0;
            while (true) {
                if (offset >= length) {
                    break;
                }
                int codePoint = Character.codePointAt(name, offset);
                if (Character.isDigit(codePoint)) {
                    prefixIsNumeric = true;
                    break;
                }
                if (!Character.isSpaceChar(codePoint) && codePoint != 43 && codePoint != 40 && codePoint != 41 && codePoint != 46 && codePoint != 45 && codePoint != 35) {
                    break;
                }
                offset += Character.charCount(codePoint);
            }
            if (prefixIsNumeric) {
                return this.mNumberBucketIndex;
            }
            if (this.mEnableSecondaryLocalePinyin) {
                name = HanziToPinyin.getInstance().transliterate(name);
            }
            int bucket = this.mAlphabeticIndex.getBucketIndex(name);
            if (bucket < 0) {
                return -1;
            }
            if (bucket >= this.mNumberBucketIndex) {
                return bucket + 1;
            }
            return bucket;
        }

        public int getBucketCount() {
            return this.mAlphabeticIndexBucketCount + 1;
        }

        public String getBucketLabel(int bucketIndex) {
            if (bucketIndex < 0 || bucketIndex >= getBucketCount()) {
                return "";
            }
            if (bucketIndex == this.mNumberBucketIndex) {
                return "#";
            }
            if (bucketIndex > this.mNumberBucketIndex) {
                bucketIndex--;
            }
            return this.mAlphabeticIndex.getBucketLabel(bucketIndex);
        }

        public Iterator<String> getNameLookupKeys(String name, int nameStyle) {
            return null;
        }

        public ArrayList<String> getLabels() {
            int bucketCount = getBucketCount();
            ArrayList<String> labels = new ArrayList<>(bucketCount);
            for (int i = 0; i < bucketCount; i++) {
                labels.add(getBucketLabel(i));
            }
            return labels;
        }
    }

    private static class JapaneseContactUtils extends ContactLocaleUtilsBase {
        private static final Set<Character.UnicodeBlock> CJ_BLOCKS;
        private static boolean mInitializedTransliterator;
        private static Transliterator mJapaneseTransliterator;
        private final int mMiscBucketIndex;

        public JapaneseContactUtils(LocaleSet locales) {
            super(locales);
            this.mMiscBucketIndex = super.getBucketIndex("日");
        }

        static {
            Set<Character.UnicodeBlock> set = new HashSet<>();
            set.add(Character.UnicodeBlock.HIRAGANA);
            set.add(Character.UnicodeBlock.KATAKANA);
            set.add(Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS);
            set.add(Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS);
            set.add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS);
            set.add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A);
            set.add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B);
            set.add(Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION);
            set.add(Character.UnicodeBlock.CJK_RADICALS_SUPPLEMENT);
            set.add(Character.UnicodeBlock.CJK_COMPATIBILITY);
            set.add(Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS);
            set.add(Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS);
            set.add(Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT);
            CJ_BLOCKS = Collections.unmodifiableSet(set);
        }

        private static boolean isChineseOrJapanese(int codePoint) {
            return CJ_BLOCKS.contains(Character.UnicodeBlock.of(codePoint));
        }

        @Override
        public int getBucketIndex(String name) {
            int bucketIndex = super.getBucketIndex(name);
            if ((bucketIndex == this.mMiscBucketIndex && !isChineseOrJapanese(Character.codePointAt(name, 0))) || bucketIndex > this.mMiscBucketIndex) {
                return bucketIndex + 1;
            }
            return bucketIndex;
        }

        @Override
        public int getBucketCount() {
            return super.getBucketCount() + 1;
        }

        @Override
        public String getBucketLabel(int bucketIndex) {
            if (bucketIndex == this.mMiscBucketIndex) {
                return "他";
            }
            if (bucketIndex > this.mMiscBucketIndex) {
                bucketIndex--;
            }
            return super.getBucketLabel(bucketIndex);
        }

        @Override
        public Iterator<String> getNameLookupKeys(String name, int nameStyle) {
            if (nameStyle == 4) {
                return getRomajiNameLookupKeys(name);
            }
            return null;
        }

        private static Transliterator getJapaneseTransliterator() {
            Transliterator transliterator;
            synchronized (JapaneseContactUtils.class) {
                if (!mInitializedTransliterator) {
                    mInitializedTransliterator = true;
                    Transliterator t = null;
                    try {
                        Transliterator t2 = new Transliterator("Hiragana-Latin; Katakana-Latin; Latin-Ascii");
                        t = t2;
                    } catch (RuntimeException e) {
                        Log.w("ContactLocale", "Hiragana/Katakana-Latin transliterator data is missing");
                    }
                    mJapaneseTransliterator = t;
                    transliterator = mJapaneseTransliterator;
                } else {
                    transliterator = mJapaneseTransliterator;
                }
            }
            return transliterator;
        }

        public static Iterator<String> getRomajiNameLookupKeys(String name) {
            Transliterator t = getJapaneseTransliterator();
            if (t == null) {
                return null;
            }
            String romajiName = t.transliterate(name);
            if (TextUtils.isEmpty(romajiName) || TextUtils.equals(name, romajiName)) {
                return null;
            }
            HashSet<String> keys = new HashSet<>();
            keys.add(romajiName);
            return keys.iterator();
        }
    }

    private static class SimplifiedChineseContactUtils extends ContactLocaleUtilsBase {
        public SimplifiedChineseContactUtils(LocaleSet locales) {
            super(locales);
        }

        @Override
        public Iterator<String> getNameLookupKeys(String name, int nameStyle) {
            if (nameStyle == 4 || nameStyle == 5) {
                return null;
            }
            return getPinyinNameLookupKeys(name);
        }

        public static Iterator<String> getPinyinNameLookupKeys(String name) {
            HashSet<String> keys = new HashSet<>();
            ArrayList<HanziToPinyin.Token> tokens = HanziToPinyin.getInstance().getTokens(name);
            int tokenCount = tokens.size();
            StringBuilder keyPinyin = new StringBuilder();
            StringBuilder keyInitial = new StringBuilder();
            StringBuilder keyOriginal = new StringBuilder();
            for (int i = tokenCount - 1; i >= 0; i--) {
                HanziToPinyin.Token token = tokens.get(i);
                if (3 != token.type) {
                    if (2 == token.type) {
                        keyPinyin.insert(0, token.target);
                        keyInitial.insert(0, token.target.charAt(0));
                    } else if (1 == token.type) {
                        if (keyPinyin.length() > 0) {
                            keyPinyin.insert(0, ' ');
                        }
                        if (keyOriginal.length() > 0) {
                            keyOriginal.insert(0, ' ');
                        }
                        keyPinyin.insert(0, token.source);
                        keyInitial.insert(0, token.source.charAt(0));
                    }
                    keyOriginal.insert(0, token.source);
                    keys.add(keyOriginal.toString());
                    keys.add(keyPinyin.toString());
                    keys.add(keyInitial.toString());
                }
            }
            return keys.iterator();
        }
    }

    private ContactLocaleUtils(LocaleSet locales) {
        if (locales == null) {
            this.mLocales = LocaleSet.getDefault();
        } else {
            this.mLocales = locales;
        }
        if (this.mLocales.isPrimaryLanguage(JAPANESE_LANGUAGE)) {
            this.mUtils = new JapaneseContactUtils(this.mLocales);
        } else if (this.mLocales.isPrimaryLocaleSimplifiedChinese()) {
            this.mUtils = new SimplifiedChineseContactUtils(this.mLocales);
        } else {
            this.mUtils = new ContactLocaleUtilsBase(this.mLocales);
        }
        Log.i("ContactLocale", "AddressBook Labels [" + this.mLocales.toString() + "]: " + getLabels().toString());
    }

    public boolean isLocale(LocaleSet locales) {
        return this.mLocales.equals(locales);
    }

    public static synchronized ContactLocaleUtils getInstance() {
        if (sSingleton == null) {
            sSingleton = new ContactLocaleUtils(LocaleSet.getDefault());
        }
        return sSingleton;
    }

    public static synchronized void setLocale(Locale locale) {
        setLocales(new LocaleSet(locale));
    }

    public static synchronized void setLocales(LocaleSet locales) {
        if (sSingleton == null || !sSingleton.isLocale(locales)) {
            sSingleton = new ContactLocaleUtils(locales);
        }
    }

    public int getBucketIndex(String name) {
        return this.mUtils.getBucketIndex(name);
    }

    public String getBucketLabel(int bucketIndex) {
        return this.mUtils.getBucketLabel(bucketIndex);
    }

    public ArrayList<String> getLabels() {
        return this.mUtils.getLabels();
    }

    public Iterator<String> getNameLookupKeys(String name, int nameStyle) {
        if (!this.mLocales.isPrimaryLocaleCJK()) {
            if (this.mLocales.isSecondaryLocaleSimplifiedChinese()) {
                if (nameStyle == 3 || nameStyle == 2) {
                    return SimplifiedChineseContactUtils.getPinyinNameLookupKeys(name);
                }
            } else if (nameStyle == 4) {
                return JapaneseContactUtils.getRomajiNameLookupKeys(name);
            }
        }
        return this.mUtils.getNameLookupKeys(name, nameStyle);
    }
}
