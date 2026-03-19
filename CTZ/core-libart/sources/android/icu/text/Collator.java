package android.icu.text;

import android.icu.impl.ICUDebug;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.UResource;
import android.icu.impl.coll.CollationRoot;
import android.icu.lang.UCharacter;
import android.icu.lang.UProperty;
import android.icu.util.Freezable;
import android.icu.util.ICUException;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import android.icu.util.VersionInfo;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Set;

public abstract class Collator implements Comparator<Object>, Freezable<Collator>, Cloneable {
    private static final String BASE = "android/icu/impl/data/icudt60b/coll";
    public static final int CANONICAL_DECOMPOSITION = 17;
    public static final int FULL_DECOMPOSITION = 15;
    public static final int IDENTICAL = 15;
    public static final int NO_DECOMPOSITION = 16;
    public static final int PRIMARY = 0;
    public static final int QUATERNARY = 3;
    private static final String RESOURCE = "collations";
    public static final int SECONDARY = 1;
    public static final int TERTIARY = 2;
    private static ServiceShim shim;
    private static final String[] KEYWORDS = {"collation"};
    private static final boolean DEBUG = ICUDebug.enabled("collator");

    public interface ReorderCodes {
        public static final int CURRENCY = 4099;
        public static final int DEFAULT = -1;
        public static final int DIGIT = 4100;
        public static final int FIRST = 4096;

        @Deprecated
        public static final int LIMIT = 4101;
        public static final int NONE = 103;
        public static final int OTHERS = 103;
        public static final int PUNCTUATION = 4097;
        public static final int SPACE = 4096;
        public static final int SYMBOL = 4098;
    }

    public abstract int compare(String str, String str2);

    public abstract CollationKey getCollationKey(String str);

    public abstract RawCollationKey getRawCollationKey(String str, RawCollationKey rawCollationKey);

    public abstract VersionInfo getUCAVersion();

    public abstract int getVariableTop();

    public abstract VersionInfo getVersion();

    @Deprecated
    public abstract int setVariableTop(String str);

    @Deprecated
    public abstract void setVariableTop(int i);

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj != null && getClass() == obj.getClass());
    }

    public int hashCode() {
        return 0;
    }

    private void checkNotFrozen() {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen Collator");
        }
    }

    public void setStrength(int i) {
        checkNotFrozen();
    }

    @Deprecated
    public Collator setStrength2(int i) {
        setStrength(i);
        return this;
    }

    public void setDecomposition(int i) {
        checkNotFrozen();
    }

    public void setReorderCodes(int... iArr) {
        throw new UnsupportedOperationException("Needs to be implemented by the subclass.");
    }

    public static final Collator getInstance() {
        return getInstance(ULocale.getDefault());
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public static abstract class CollatorFactory {
        public abstract Set<String> getSupportedLocaleIDs();

        public boolean visible() {
            return true;
        }

        public Collator createCollator(ULocale uLocale) {
            return createCollator(uLocale.toLocale());
        }

        public Collator createCollator(Locale locale) {
            return createCollator(ULocale.forLocale(locale));
        }

        public String getDisplayName(Locale locale, Locale locale2) {
            return getDisplayName(ULocale.forLocale(locale), ULocale.forLocale(locale2));
        }

        public String getDisplayName(ULocale uLocale, ULocale uLocale2) {
            if (visible() && getSupportedLocaleIDs().contains(uLocale.getBaseName())) {
                return uLocale.getDisplayName(uLocale2);
            }
            return null;
        }

        protected CollatorFactory() {
        }
    }

    static abstract class ServiceShim {
        abstract Locale[] getAvailableLocales();

        abstract ULocale[] getAvailableULocales();

        abstract String getDisplayName(ULocale uLocale, ULocale uLocale2);

        abstract Collator getInstance(ULocale uLocale);

        abstract Object registerFactory(CollatorFactory collatorFactory);

        abstract Object registerInstance(Collator collator, ULocale uLocale);

        abstract boolean unregister(Object obj);

        ServiceShim() {
        }
    }

    private static ServiceShim getShim() {
        if (shim == null) {
            try {
                shim = (ServiceShim) Class.forName("android.icu.text.CollatorServiceShim").newInstance();
            } catch (MissingResourceException e) {
                throw e;
            } catch (Exception e2) {
                if (DEBUG) {
                    e2.printStackTrace();
                }
                throw new ICUException(e2);
            }
        }
        return shim;
    }

    private static final class ASCII {
        private ASCII() {
        }

        static boolean equalIgnoreCase(CharSequence charSequence, CharSequence charSequence2) {
            int length = charSequence.length();
            if (length != charSequence2.length()) {
                return false;
            }
            for (int i = 0; i < length; i++) {
                char cCharAt = charSequence.charAt(i);
                char cCharAt2 = charSequence2.charAt(i);
                if (cCharAt != cCharAt2) {
                    if ('A' <= cCharAt && cCharAt <= 'Z') {
                        if (cCharAt + ' ' != cCharAt2) {
                            return false;
                        }
                    } else if ('A' > cCharAt2 || cCharAt2 > 'Z' || cCharAt2 + ' ' != cCharAt) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    private static final boolean getYesOrNo(String str, String str2) {
        if (ASCII.equalIgnoreCase(str2, "yes")) {
            return true;
        }
        if (ASCII.equalIgnoreCase(str2, "no")) {
            return false;
        }
        throw new IllegalArgumentException("illegal locale keyword=value: " + str + "=" + str2);
    }

    private static final int getIntValue(String str, String str2, String... strArr) {
        for (int i = 0; i < strArr.length; i++) {
            if (ASCII.equalIgnoreCase(str2, strArr[i])) {
                return i;
            }
        }
        throw new IllegalArgumentException("illegal locale keyword=value: " + str + "=" + str2);
    }

    private static final int getReorderCode(String str, String str2) {
        return 4096 + getIntValue(str, str2, "space", "punct", "symbol", "currency", "digit");
    }

    private static void setAttributesFromKeywords(ULocale uLocale, Collator collator, RuleBasedCollator ruleBasedCollator) {
        int reorderCode;
        if (uLocale.getKeywordValue("colHiraganaQuaternary") != null) {
            throw new UnsupportedOperationException("locale keyword kh/colHiraganaQuaternary");
        }
        if (uLocale.getKeywordValue("variableTop") != null) {
            throw new UnsupportedOperationException("locale keyword vt/variableTop");
        }
        String keywordValue = uLocale.getKeywordValue("colStrength");
        if (keywordValue != null) {
            int intValue = getIntValue("colStrength", keywordValue, "primary", "secondary", "tertiary", "quaternary", "identical");
            if (intValue > 3) {
                intValue = 15;
            }
            collator.setStrength(intValue);
        }
        String keywordValue2 = uLocale.getKeywordValue("colBackwards");
        if (keywordValue2 != null) {
            if (ruleBasedCollator != null) {
                ruleBasedCollator.setFrenchCollation(getYesOrNo("colBackwards", keywordValue2));
            } else {
                throw new UnsupportedOperationException("locale keyword kb/colBackwards only settable for RuleBasedCollator");
            }
        }
        String keywordValue3 = uLocale.getKeywordValue("colCaseLevel");
        if (keywordValue3 != null) {
            if (ruleBasedCollator != null) {
                ruleBasedCollator.setCaseLevel(getYesOrNo("colCaseLevel", keywordValue3));
            } else {
                throw new UnsupportedOperationException("locale keyword kb/colBackwards only settable for RuleBasedCollator");
            }
        }
        String keywordValue4 = uLocale.getKeywordValue("colCaseFirst");
        boolean z = true;
        if (keywordValue4 != null) {
            if (ruleBasedCollator != null) {
                int intValue2 = getIntValue("colCaseFirst", keywordValue4, "no", "lower", "upper");
                if (intValue2 == 0) {
                    ruleBasedCollator.setLowerCaseFirst(false);
                    ruleBasedCollator.setUpperCaseFirst(false);
                } else if (intValue2 == 1) {
                    ruleBasedCollator.setLowerCaseFirst(true);
                } else {
                    ruleBasedCollator.setUpperCaseFirst(true);
                }
            } else {
                throw new UnsupportedOperationException("locale keyword kf/colCaseFirst only settable for RuleBasedCollator");
            }
        }
        String keywordValue5 = uLocale.getKeywordValue("colAlternate");
        if (keywordValue5 != null) {
            if (ruleBasedCollator != null) {
                if (getIntValue("colAlternate", keywordValue5, "non-ignorable", "shifted") == 0) {
                    z = false;
                }
                ruleBasedCollator.setAlternateHandlingShifted(z);
            } else {
                throw new UnsupportedOperationException("locale keyword ka/colAlternate only settable for RuleBasedCollator");
            }
        }
        String keywordValue6 = uLocale.getKeywordValue("colNormalization");
        if (keywordValue6 != null) {
            collator.setDecomposition(getYesOrNo("colNormalization", keywordValue6) ? 17 : 16);
        }
        String keywordValue7 = uLocale.getKeywordValue("colNumeric");
        if (keywordValue7 != null) {
            if (ruleBasedCollator != null) {
                ruleBasedCollator.setNumericCollation(getYesOrNo("colNumeric", keywordValue7));
            } else {
                throw new UnsupportedOperationException("locale keyword kn/colNumeric only settable for RuleBasedCollator");
            }
        }
        String keywordValue8 = uLocale.getKeywordValue("colReorder");
        if (keywordValue8 != null) {
            int[] iArr = new int[183];
            int i = 0;
            int i2 = 0;
            while (i != iArr.length) {
                int i3 = i2;
                while (i3 < keywordValue8.length() && keywordValue8.charAt(i3) != '-') {
                    i3++;
                }
                String strSubstring = keywordValue8.substring(i2, i3);
                if (strSubstring.length() == 4) {
                    reorderCode = UCharacter.getPropertyValueEnum(UProperty.SCRIPT, strSubstring);
                } else {
                    reorderCode = getReorderCode("colReorder", strSubstring);
                }
                int i4 = i + 1;
                iArr[i] = reorderCode;
                if (i3 != keywordValue8.length()) {
                    i2 = i3 + 1;
                    i = i4;
                } else {
                    if (i4 == 0) {
                        throw new IllegalArgumentException("no script codes for colReorder locale keyword");
                    }
                    int[] iArr2 = new int[i4];
                    System.arraycopy(iArr, 0, iArr2, 0, i4);
                    collator.setReorderCodes(iArr2);
                }
            }
            throw new IllegalArgumentException("too many script codes for colReorder locale keyword: " + keywordValue8);
        }
        String keywordValue9 = uLocale.getKeywordValue("kv");
        if (keywordValue9 != null) {
            collator.setMaxVariable(getReorderCode("kv", keywordValue9));
        }
    }

    public static final Collator getInstance(ULocale uLocale) {
        if (uLocale == null) {
            uLocale = ULocale.getDefault();
        }
        Collator serviceShim = getShim().getInstance(uLocale);
        if (!uLocale.getName().equals(uLocale.getBaseName())) {
            setAttributesFromKeywords(uLocale, serviceShim, serviceShim instanceof RuleBasedCollator ? (RuleBasedCollator) serviceShim : null);
        }
        return serviceShim;
    }

    public static final Collator getInstance(Locale locale) {
        return getInstance(ULocale.forLocale(locale));
    }

    public static final Object registerInstance(Collator collator, ULocale uLocale) {
        return getShim().registerInstance(collator, uLocale);
    }

    public static final Object registerFactory(CollatorFactory collatorFactory) {
        return getShim().registerFactory(collatorFactory);
    }

    public static final boolean unregister(Object obj) {
        if (shim == null) {
            return false;
        }
        return shim.unregister(obj);
    }

    public static Locale[] getAvailableLocales() {
        if (shim == null) {
            return ICUResourceBundle.getAvailableLocales("android/icu/impl/data/icudt60b/coll", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        }
        return shim.getAvailableLocales();
    }

    public static final ULocale[] getAvailableULocales() {
        if (shim == null) {
            return ICUResourceBundle.getAvailableULocales("android/icu/impl/data/icudt60b/coll", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        }
        return shim.getAvailableULocales();
    }

    public static final String[] getKeywords() {
        return KEYWORDS;
    }

    public static final String[] getKeywordValues(String str) {
        if (!str.equals(KEYWORDS[0])) {
            throw new IllegalArgumentException("Invalid keyword: " + str);
        }
        return ICUResourceBundle.getKeywordValues("android/icu/impl/data/icudt60b/coll", RESOURCE);
    }

    public static final String[] getKeywordValuesForLocale(String str, ULocale uLocale, boolean z) {
        ICUResourceBundle iCUResourceBundle = (ICUResourceBundle) UResourceBundle.getBundleInstance("android/icu/impl/data/icudt60b/coll", uLocale);
        KeywordsSink keywordsSink = new KeywordsSink();
        iCUResourceBundle.getAllItemsWithFallback(RESOURCE, keywordsSink);
        return (String[]) keywordsSink.values.toArray(new String[keywordsSink.values.size()]);
    }

    private static final class KeywordsSink extends UResource.Sink {
        boolean hasDefault;
        LinkedList<String> values;

        private KeywordsSink() {
            this.values = new LinkedList<>();
            this.hasDefault = false;
        }

        @Override
        public void put(UResource.Key key, UResource.Value value, boolean z) {
            UResource.Table table = value.getTable();
            for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                int type = value.getType();
                if (type == 0) {
                    if (!this.hasDefault && key.contentEquals("default")) {
                        String string = value.getString();
                        if (!string.isEmpty()) {
                            this.values.remove(string);
                            this.values.addFirst(string);
                            this.hasDefault = true;
                        }
                    }
                } else if (type == 2 && !key.startsWith("private-")) {
                    String string2 = key.toString();
                    if (!this.values.contains(string2)) {
                        this.values.add(string2);
                    }
                }
            }
        }
    }

    public static final ULocale getFunctionalEquivalent(String str, ULocale uLocale, boolean[] zArr) {
        return ICUResourceBundle.getFunctionalEquivalent("android/icu/impl/data/icudt60b/coll", ICUResourceBundle.ICU_DATA_CLASS_LOADER, RESOURCE, str, uLocale, zArr, true);
    }

    public static final ULocale getFunctionalEquivalent(String str, ULocale uLocale) {
        return getFunctionalEquivalent(str, uLocale, null);
    }

    public static String getDisplayName(Locale locale, Locale locale2) {
        return getShim().getDisplayName(ULocale.forLocale(locale), ULocale.forLocale(locale2));
    }

    public static String getDisplayName(ULocale uLocale, ULocale uLocale2) {
        return getShim().getDisplayName(uLocale, uLocale2);
    }

    public static String getDisplayName(Locale locale) {
        return getShim().getDisplayName(ULocale.forLocale(locale), ULocale.getDefault(ULocale.Category.DISPLAY));
    }

    public static String getDisplayName(ULocale uLocale) {
        return getShim().getDisplayName(uLocale, ULocale.getDefault(ULocale.Category.DISPLAY));
    }

    public int getStrength() {
        return 2;
    }

    public int getDecomposition() {
        return 16;
    }

    public boolean equals(String str, String str2) {
        return compare(str, str2) == 0;
    }

    public UnicodeSet getTailoredSet() {
        return new UnicodeSet(0, 1114111);
    }

    @Override
    public int compare(Object obj, Object obj2) {
        return doCompare((CharSequence) obj, (CharSequence) obj2);
    }

    @Deprecated
    protected int doCompare(CharSequence charSequence, CharSequence charSequence2) {
        return compare(charSequence.toString(), charSequence2.toString());
    }

    public Collator setMaxVariable(int i) {
        throw new UnsupportedOperationException("Needs to be implemented by the subclass.");
    }

    public int getMaxVariable() {
        return 4097;
    }

    public int[] getReorderCodes() {
        throw new UnsupportedOperationException("Needs to be implemented by the subclass.");
    }

    public static int[] getEquivalentReorderCodes(int i) {
        return CollationRoot.getData().getEquivalentScripts(i);
    }

    @Override
    public boolean isFrozen() {
        return false;
    }

    @Override
    public Collator freeze() {
        throw new UnsupportedOperationException("Needs to be implemented by the subclass.");
    }

    @Override
    public Collator cloneAsThawed() {
        throw new UnsupportedOperationException("Needs to be implemented by the subclass.");
    }

    protected Collator() {
    }

    public ULocale getLocale(ULocale.Type type) {
        return ULocale.ROOT;
    }

    void setLocale(ULocale uLocale, ULocale uLocale2) {
    }
}
