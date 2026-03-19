package android.icu.text;

import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.Utility;
import android.icu.util.CaseInsensitiveString;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;

public abstract class Transliterator implements StringTransform {
    static final boolean DEBUG = false;
    public static final int FORWARD = 0;
    static final char ID_DELIM = ';';
    static final char ID_SEP = '-';
    private static final String RB_DISPLAY_NAME_PATTERN = "TransliteratorNamePattern";
    private static final String RB_DISPLAY_NAME_PREFIX = "%Translit%%";
    private static final String RB_RULE_BASED_IDS = "RuleBasedTransliteratorIDs";
    private static final String RB_SCRIPT_DISPLAY_NAME_PREFIX = "%Translit%";
    public static final int REVERSE = 1;
    private static final String ROOT = "root";
    static final char VARIANT_SEP = '/';
    private String ID;
    private UnicodeSet filter;
    private int maximumContextLength = 0;
    private static TransliteratorRegistry registry = new TransliteratorRegistry();
    private static Map<CaseInsensitiveString, String> displayNameCache = Collections.synchronizedMap(new HashMap());

    public interface Factory {
        Transliterator getInstance(String str);
    }

    protected abstract void handleTransliterate(Replaceable replaceable, Position position, boolean z);

    public static class Position {
        static final boolean $assertionsDisabled = false;
        public int contextLimit;
        public int contextStart;
        public int limit;
        public int start;

        public Position() {
            this(0, 0, 0, 0);
        }

        public Position(int i, int i2, int i3) {
            this(i, i2, i3, i2);
        }

        public Position(int i, int i2, int i3, int i4) {
            this.contextStart = i;
            this.contextLimit = i2;
            this.start = i3;
            this.limit = i4;
        }

        public Position(Position position) {
            set(position);
        }

        public void set(Position position) {
            this.contextStart = position.contextStart;
            this.contextLimit = position.contextLimit;
            this.start = position.start;
            this.limit = position.limit;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof Position)) {
                return false;
            }
            Position position = (Position) obj;
            return this.contextStart == position.contextStart && this.contextLimit == position.contextLimit && this.start == position.start && this.limit == position.limit;
        }

        @Deprecated
        public int hashCode() {
            return 42;
        }

        public String toString() {
            return "[cs=" + this.contextStart + ", s=" + this.start + ", l=" + this.limit + ", cl=" + this.contextLimit + "]";
        }

        public final void validate(int i) {
            if (this.contextStart < 0 || this.start < this.contextStart || this.limit < this.start || this.contextLimit < this.limit || i < this.contextLimit) {
                throw new IllegalArgumentException("Invalid Position {cs=" + this.contextStart + ", s=" + this.start + ", l=" + this.limit + ", cl=" + this.contextLimit + "}, len=" + i);
            }
        }
    }

    protected Transliterator(String str, UnicodeFilter unicodeFilter) {
        if (str == null) {
            throw new NullPointerException();
        }
        this.ID = str;
        setFilter(unicodeFilter);
    }

    public final int transliterate(Replaceable replaceable, int i, int i2) {
        if (i < 0 || i2 < i || replaceable.length() < i2) {
            return -1;
        }
        Position position = new Position(i, i2, i);
        filteredTransliterate(replaceable, position, false, true);
        return position.limit;
    }

    public final void transliterate(Replaceable replaceable) {
        transliterate(replaceable, 0, replaceable.length());
    }

    public final String transliterate(String str) {
        ReplaceableString replaceableString = new ReplaceableString(str);
        transliterate(replaceableString);
        return replaceableString.toString();
    }

    public final void transliterate(Replaceable replaceable, Position position, String str) {
        position.validate(replaceable.length());
        if (str != null) {
            replaceable.replace(position.limit, position.limit, str);
            position.limit += str.length();
            position.contextLimit += str.length();
        }
        if (position.limit > 0 && UTF16.isLeadSurrogate(replaceable.charAt(position.limit - 1))) {
            return;
        }
        filteredTransliterate(replaceable, position, true, true);
    }

    public final void transliterate(Replaceable replaceable, Position position, int i) {
        transliterate(replaceable, position, UTF16.valueOf(i));
    }

    public final void transliterate(Replaceable replaceable, Position position) {
        transliterate(replaceable, position, (String) null);
    }

    public final void finishTransliteration(Replaceable replaceable, Position position) {
        position.validate(replaceable.length());
        filteredTransliterate(replaceable, position, false, true);
    }

    private void filteredTransliterate(Replaceable replaceable, Position position, boolean z, boolean z2) {
        boolean z3;
        int i;
        if (this.filter == null && !z2) {
            handleTransliterate(replaceable, position, z);
            return;
        }
        int i2 = position.limit;
        do {
            if (this.filter != null) {
                while (position.start < i2) {
                    UnicodeSet unicodeSet = this.filter;
                    int iChar32At = replaceable.char32At(position.start);
                    if (unicodeSet.contains(iChar32At)) {
                        break;
                    } else {
                        position.start += UTF16.getCharCount(iChar32At);
                    }
                }
                position.limit = position.start;
                while (position.limit < i2) {
                    UnicodeSet unicodeSet2 = this.filter;
                    int iChar32At2 = replaceable.char32At(position.limit);
                    if (!unicodeSet2.contains(iChar32At2)) {
                        break;
                    } else {
                        position.limit += UTF16.getCharCount(iChar32At2);
                    }
                }
            }
            if (position.start == position.limit) {
                break;
            }
            z3 = position.limit < i2 ? false : z;
            if (z2 && z3) {
                int i3 = position.start;
                int i4 = position.limit;
                int i5 = i4 - i3;
                int length = replaceable.length();
                replaceable.copy(i3, i4, length);
                int i6 = position.start;
                int i7 = i3;
                int i8 = length;
                int i9 = 0;
                int i10 = 0;
                while (true) {
                    int charCount = UTF16.getCharCount(replaceable.char32At(i6));
                    i6 += charCount;
                    if (i6 > i4) {
                        break;
                    }
                    i9 += charCount;
                    position.limit = i6;
                    handleTransliterate(replaceable, position, true);
                    int i11 = position.limit - i6;
                    if (position.start != position.limit) {
                        int i12 = (i8 + i11) - (position.limit - i7);
                        i = i5;
                        replaceable.replace(i7, position.limit, "");
                        replaceable.copy(i12, i12 + i9, i7);
                        position.start = i7;
                        position.limit = i6;
                        position.contextLimit -= i11;
                    } else {
                        i = i5;
                        i8 += i9 + i11;
                        i4 += i11;
                        i10 += i11;
                        i6 = position.start;
                        i7 = i6;
                        i9 = 0;
                    }
                    i5 = i;
                }
                int i13 = length + i10;
                i2 += i10;
                replaceable.replace(i13, i5 + i13, "");
                position.start = i7;
            } else {
                int i14 = position.limit;
                handleTransliterate(replaceable, position, z3);
                int i15 = position.limit - i14;
                if (!z3 && position.start != position.limit) {
                    throw new RuntimeException("ERROR: Incomplete non-incremental transliteration by " + getID());
                }
                i2 += i15;
            }
            if (this.filter == null) {
                break;
            }
        } while (!z3);
        position.limit = i2;
    }

    public void filteredTransliterate(Replaceable replaceable, Position position, boolean z) {
        filteredTransliterate(replaceable, position, z, false);
    }

    public final int getMaximumContextLength() {
        return this.maximumContextLength;
    }

    protected void setMaximumContextLength(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("Invalid context length " + i);
        }
        this.maximumContextLength = i;
    }

    public final String getID() {
        return this.ID;
    }

    protected final void setID(String str) {
        this.ID = str;
    }

    public static final String getDisplayName(String str) {
        return getDisplayName(str, ULocale.getDefault(ULocale.Category.DISPLAY));
    }

    public static String getDisplayName(String str, Locale locale) {
        return getDisplayName(str, ULocale.forLocale(locale));
    }

    public static String getDisplayName(String str, ULocale uLocale) {
        ICUResourceBundle iCUResourceBundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_TRANSLIT_BASE_NAME, uLocale);
        String[] strArrIDtoSTV = TransliteratorIDParser.IDtoSTV(str);
        if (strArrIDtoSTV == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(strArrIDtoSTV[0]);
        sb.append(ID_SEP);
        sb.append(strArrIDtoSTV[1]);
        String string = sb.toString();
        if (strArrIDtoSTV[2] != null && strArrIDtoSTV[2].length() > 0) {
            string = string + VARIANT_SEP + strArrIDtoSTV[2];
        }
        String str2 = displayNameCache.get(new CaseInsensitiveString(string));
        if (str2 != null) {
            return str2;
        }
        try {
            return iCUResourceBundle.getString(RB_DISPLAY_NAME_PREFIX + string);
        } catch (MissingResourceException e) {
            try {
                java.text.MessageFormat messageFormat = new java.text.MessageFormat(iCUResourceBundle.getString(RB_DISPLAY_NAME_PATTERN));
                Object[] objArr = new Object[3];
                objArr[0] = 2;
                objArr[1] = strArrIDtoSTV[0];
                objArr[2] = strArrIDtoSTV[1];
                for (int i = 1; i <= 2; i++) {
                    try {
                        objArr[i] = iCUResourceBundle.getString(RB_SCRIPT_DISPLAY_NAME_PREFIX + ((String) objArr[i]));
                    } catch (MissingResourceException e2) {
                    }
                }
                if (strArrIDtoSTV[2].length() > 0) {
                    return messageFormat.format(objArr) + VARIANT_SEP + strArrIDtoSTV[2];
                }
                return messageFormat.format(objArr);
            } catch (MissingResourceException e3) {
                throw new RuntimeException();
            }
        }
    }

    public final UnicodeFilter getFilter() {
        return this.filter;
    }

    public void setFilter(UnicodeFilter unicodeFilter) {
        if (unicodeFilter == null) {
            this.filter = null;
            return;
        }
        try {
            this.filter = new UnicodeSet((UnicodeSet) unicodeFilter).freeze();
        } catch (Exception e) {
            this.filter = new UnicodeSet();
            unicodeFilter.addMatchSetTo(this.filter);
            this.filter.freeze();
        }
    }

    public static final Transliterator getInstance(String str) {
        return getInstance(str, 0);
    }

    public static Transliterator getInstance(String str, int i) {
        Transliterator compoundTransliterator;
        StringBuffer stringBuffer = new StringBuffer();
        ArrayList arrayList = new ArrayList();
        UnicodeSet[] unicodeSetArr = new UnicodeSet[1];
        if (!TransliteratorIDParser.parseCompoundID(str, i, stringBuffer, arrayList, unicodeSetArr)) {
            throw new IllegalArgumentException("Invalid ID " + str);
        }
        List<Transliterator> listInstantiateList = TransliteratorIDParser.instantiateList(arrayList);
        if (arrayList.size() > 1 || stringBuffer.indexOf(";") >= 0) {
            compoundTransliterator = new CompoundTransliterator(listInstantiateList);
        } else {
            compoundTransliterator = listInstantiateList.get(0);
        }
        compoundTransliterator.setID(stringBuffer.toString());
        if (unicodeSetArr[0] != null) {
            compoundTransliterator.setFilter(unicodeSetArr[0]);
        }
        return compoundTransliterator;
    }

    static Transliterator getBasicInstance(String str, String str2) {
        StringBuffer stringBuffer = new StringBuffer();
        Transliterator transliterator = registry.get(str, stringBuffer);
        if (stringBuffer.length() != 0) {
            transliterator = getInstance(stringBuffer.toString(), 0);
        }
        if (transliterator != null && str2 != null) {
            transliterator.setID(str2);
        }
        return transliterator;
    }

    public static final Transliterator createFromRules(String str, String str2, int i) {
        Transliterator transliterator;
        TransliteratorParser transliteratorParser = new TransliteratorParser();
        transliteratorParser.parse(str2, i);
        if (transliteratorParser.idBlockVector.size() == 0 && transliteratorParser.dataVector.size() == 0) {
            return new NullTransliterator();
        }
        if (transliteratorParser.idBlockVector.size() == 0 && transliteratorParser.dataVector.size() == 1) {
            return new RuleBasedTransliterator(str, transliteratorParser.dataVector.get(0), transliteratorParser.compoundFilter);
        }
        if (transliteratorParser.idBlockVector.size() == 1 && transliteratorParser.dataVector.size() == 0) {
            if (transliteratorParser.compoundFilter != null) {
                transliterator = getInstance(transliteratorParser.compoundFilter.toPattern(false) + ";" + transliteratorParser.idBlockVector.get(0));
            } else {
                transliterator = getInstance(transliteratorParser.idBlockVector.get(0));
            }
            if (transliterator == null) {
                return transliterator;
            }
            transliterator.setID(str);
            return transliterator;
        }
        ArrayList arrayList = new ArrayList();
        int iMax = Math.max(transliteratorParser.idBlockVector.size(), transliteratorParser.dataVector.size());
        int i2 = 1;
        for (int i3 = 0; i3 < iMax; i3++) {
            if (i3 < transliteratorParser.idBlockVector.size()) {
                String str3 = transliteratorParser.idBlockVector.get(i3);
                if (str3.length() > 0 && !(getInstance(str3) instanceof NullTransliterator)) {
                    arrayList.add(getInstance(str3));
                }
            }
            if (i3 < transliteratorParser.dataVector.size()) {
                arrayList.add(new RuleBasedTransliterator("%Pass" + i2, transliteratorParser.dataVector.get(i3), null));
                i2++;
            }
        }
        CompoundTransliterator compoundTransliterator = new CompoundTransliterator(arrayList, i2 - 1);
        compoundTransliterator.setID(str);
        if (transliteratorParser.compoundFilter != null) {
            compoundTransliterator.setFilter(transliteratorParser.compoundFilter);
        }
        return compoundTransliterator;
    }

    public String toRules(boolean z) {
        return baseToRules(z);
    }

    protected final String baseToRules(boolean z) {
        if (z) {
            StringBuffer stringBuffer = new StringBuffer();
            String id = getID();
            int charCount = 0;
            while (charCount < id.length()) {
                int iCharAt = UTF16.charAt(id, charCount);
                if (!Utility.escapeUnprintable(stringBuffer, iCharAt)) {
                    UTF16.append(stringBuffer, iCharAt);
                }
                charCount += UTF16.getCharCount(iCharAt);
            }
            stringBuffer.insert(0, "::");
            stringBuffer.append(ID_DELIM);
            return stringBuffer.toString();
        }
        return "::" + getID() + ID_DELIM;
    }

    public Transliterator[] getElements() {
        if (this instanceof CompoundTransliterator) {
            CompoundTransliterator compoundTransliterator = (CompoundTransliterator) this;
            Transliterator[] transliteratorArr = new Transliterator[compoundTransliterator.getCount()];
            for (int i = 0; i < transliteratorArr.length; i++) {
                transliteratorArr[i] = compoundTransliterator.getTransliterator(i);
            }
            return transliteratorArr;
        }
        return new Transliterator[]{this};
    }

    public final UnicodeSet getSourceSet() {
        UnicodeSet unicodeSet = new UnicodeSet();
        addSourceTargetSet(getFilterAsUnicodeSet(UnicodeSet.ALL_CODE_POINTS), unicodeSet, new UnicodeSet());
        return unicodeSet;
    }

    protected UnicodeSet handleGetSourceSet() {
        return new UnicodeSet();
    }

    public UnicodeSet getTargetSet() {
        UnicodeSet unicodeSet = new UnicodeSet();
        addSourceTargetSet(getFilterAsUnicodeSet(UnicodeSet.ALL_CODE_POINTS), new UnicodeSet(), unicodeSet);
        return unicodeSet;
    }

    @Deprecated
    public void addSourceTargetSet(UnicodeSet unicodeSet, UnicodeSet unicodeSet2, UnicodeSet unicodeSet3) {
        UnicodeSet unicodeSetRetainAll = new UnicodeSet(handleGetSourceSet()).retainAll(getFilterAsUnicodeSet(unicodeSet));
        unicodeSet2.addAll(unicodeSetRetainAll);
        for (String str : unicodeSetRetainAll) {
            String strTransliterate = transliterate(str);
            if (!str.equals(strTransliterate)) {
                unicodeSet3.addAll(strTransliterate);
            }
        }
    }

    @Deprecated
    public UnicodeSet getFilterAsUnicodeSet(UnicodeSet unicodeSet) {
        UnicodeSet unicodeSet2;
        if (this.filter == null) {
            return unicodeSet;
        }
        UnicodeSet unicodeSet3 = new UnicodeSet(unicodeSet);
        try {
            unicodeSet2 = this.filter;
        } catch (ClassCastException e) {
            UnicodeSet unicodeSet4 = this.filter;
            UnicodeSet unicodeSet5 = new UnicodeSet();
            unicodeSet4.addMatchSetTo(unicodeSet5);
            unicodeSet2 = unicodeSet5;
        }
        return unicodeSet3.retainAll(unicodeSet2).freeze();
    }

    public final Transliterator getInverse() {
        return getInstance(this.ID, 1);
    }

    public static void registerClass(String str, Class<? extends Transliterator> cls, String str2) {
        registry.put(str, cls, true);
        if (str2 != null) {
            displayNameCache.put(new CaseInsensitiveString(str), str2);
        }
    }

    public static void registerFactory(String str, Factory factory) {
        registry.put(str, factory, true);
    }

    public static void registerInstance(Transliterator transliterator) {
        registry.put(transliterator.getID(), transliterator, true);
    }

    static void registerInstance(Transliterator transliterator, boolean z) {
        registry.put(transliterator.getID(), transliterator, z);
    }

    public static void registerAlias(String str, String str2) {
        registry.put(str, str2, true);
    }

    static void registerSpecialInverse(String str, String str2, boolean z) {
        TransliteratorIDParser.registerSpecialInverse(str, str2, z);
    }

    public static void unregister(String str) {
        displayNameCache.remove(new CaseInsensitiveString(str));
        registry.remove(str);
    }

    public static final Enumeration<String> getAvailableIDs() {
        return registry.getAvailableIDs();
    }

    public static final Enumeration<String> getAvailableSources() {
        return registry.getAvailableSources();
    }

    public static final Enumeration<String> getAvailableTargets(String str) {
        return registry.getAvailableTargets(str);
    }

    public static final Enumeration<String> getAvailableVariants(String str, String str2) {
        return registry.getAvailableVariants(str, str2);
    }

    static {
        int i;
        UResourceBundle uResourceBundle = UResourceBundle.getBundleInstance(ICUData.ICU_TRANSLIT_BASE_NAME, ROOT).get(RB_RULE_BASED_IDS);
        int size = uResourceBundle.getSize();
        for (int i2 = 0; i2 < size; i2++) {
            UResourceBundle uResourceBundle2 = uResourceBundle.get(i2);
            String key = uResourceBundle2.getKey();
            if (key.indexOf("-t-") < 0) {
                UResourceBundle uResourceBundle3 = uResourceBundle2.get(0);
                String key2 = uResourceBundle3.getKey();
                if (key2.equals("file") || key2.equals("internal")) {
                    String string = uResourceBundle3.getString("resource");
                    String string2 = uResourceBundle3.getString("direction");
                    char cCharAt = string2.charAt(0);
                    if (cCharAt == 'F') {
                        i = 0;
                    } else {
                        if (cCharAt != 'R') {
                            throw new RuntimeException("Can't parse direction: " + string2);
                        }
                        i = 1;
                    }
                    registry.put(key, string, i, !key2.equals("internal"));
                } else if (key2.equals("alias")) {
                    registry.put(key, uResourceBundle3.getString(), true);
                } else {
                    throw new RuntimeException("Unknow type: " + key2);
                }
            }
        }
        registerSpecialInverse("Null", "Null", false);
        registerClass("Any-Null", NullTransliterator.class, null);
        RemoveTransliterator.register();
        EscapeTransliterator.register();
        UnescapeTransliterator.register();
        LowercaseTransliterator.register();
        UppercaseTransliterator.register();
        TitlecaseTransliterator.register();
        CaseFoldTransliterator.register();
        UnicodeNameTransliterator.register();
        NameUnicodeTransliterator.register();
        NormalizationTransliterator.register();
        BreakTransliterator.register();
        AnyTransliterator.register();
    }

    @Deprecated
    public static void registerAny() {
        AnyTransliterator.register();
    }

    @Override
    public String transform(String str) {
        return transliterate(str);
    }
}
