package android.icu.text;

import android.icu.impl.ICUResourceBundle;
import android.icu.impl.Utility;
import android.icu.text.RuleBasedTransliterator;
import android.icu.text.TransliteratorIDParser;
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

        static final boolean f100assertionsDisabled;
        public int contextLimit;
        public int contextStart;
        public int limit;
        public int start;

        static {
            f100assertionsDisabled = !Position.class.desiredAssertionStatus();
        }

        public Position() {
            this(0, 0, 0, 0);
        }

        public Position(int contextStart, int contextLimit, int start) {
            this(contextStart, contextLimit, start, contextLimit);
        }

        public Position(int contextStart, int contextLimit, int start, int limit) {
            this.contextStart = contextStart;
            this.contextLimit = contextLimit;
            this.start = start;
            this.limit = limit;
        }

        public Position(Position pos) {
            set(pos);
        }

        public void set(Position pos) {
            this.contextStart = pos.contextStart;
            this.contextLimit = pos.contextLimit;
            this.start = pos.start;
            this.limit = pos.limit;
        }

        public boolean equals(Object obj) {
            return (obj instanceof Position) && this.contextStart == obj.contextStart && this.contextLimit == obj.contextLimit && this.start == obj.start && this.limit == obj.limit;
        }

        @Deprecated
        public int hashCode() {
            if (f100assertionsDisabled) {
                return 42;
            }
            throw new AssertionError("hashCode not designed");
        }

        public String toString() {
            return "[cs=" + this.contextStart + ", s=" + this.start + ", l=" + this.limit + ", cl=" + this.contextLimit + "]";
        }

        public final void validate(int length) {
            if (this.contextStart < 0 || this.start < this.contextStart || this.limit < this.start || this.contextLimit < this.limit || length < this.contextLimit) {
                throw new IllegalArgumentException("Invalid Position {cs=" + this.contextStart + ", s=" + this.start + ", l=" + this.limit + ", cl=" + this.contextLimit + "}, len=" + length);
            }
        }
    }

    protected Transliterator(String ID, UnicodeFilter filter) {
        if (ID == null) {
            throw new NullPointerException();
        }
        this.ID = ID;
        setFilter(filter);
    }

    public final int transliterate(Replaceable text, int start, int limit) {
        if (start < 0 || limit < start || text.length() < limit) {
            return -1;
        }
        Position pos = new Position(start, limit, start);
        filteredTransliterate(text, pos, false, true);
        return pos.limit;
    }

    public final void transliterate(Replaceable text) {
        transliterate(text, 0, text.length());
    }

    public final String transliterate(String text) {
        ReplaceableString result = new ReplaceableString(text);
        transliterate(result);
        return result.toString();
    }

    public final void transliterate(Replaceable text, Position index, String insertion) {
        index.validate(text.length());
        if (insertion != null) {
            text.replace(index.limit, index.limit, insertion);
            index.limit += insertion.length();
            index.contextLimit += insertion.length();
        }
        if (index.limit > 0 && UTF16.isLeadSurrogate(text.charAt(index.limit - 1))) {
            return;
        }
        filteredTransliterate(text, index, true, true);
    }

    public final void transliterate(Replaceable text, Position index, int insertion) {
        transliterate(text, index, UTF16.valueOf(insertion));
    }

    public final void transliterate(Replaceable text, Position index) {
        transliterate(text, index, (String) null);
    }

    public final void finishTransliteration(Replaceable text, Position index) {
        index.validate(text.length());
        filteredTransliterate(text, index, false, true);
    }

    private void filteredTransliterate(Replaceable text, Position index, boolean incremental, boolean rollback) {
        boolean z;
        if (this.filter == null && !rollback) {
            handleTransliterate(text, index, incremental);
            return;
        }
        int globalLimit = index.limit;
        do {
            if (this.filter != null) {
                while (index.start < globalLimit) {
                    UnicodeSet unicodeSet = this.filter;
                    int c = text.char32At(index.start);
                    if (unicodeSet.contains(c)) {
                        break;
                    } else {
                        index.start += UTF16.getCharCount(c);
                    }
                }
                index.limit = index.start;
                while (index.limit < globalLimit) {
                    UnicodeSet unicodeSet2 = this.filter;
                    int c2 = text.char32At(index.limit);
                    if (!unicodeSet2.contains(c2)) {
                        break;
                    } else {
                        index.limit += UTF16.getCharCount(c2);
                    }
                }
            }
            if (index.start == index.limit) {
                break;
            }
            z = index.limit < globalLimit ? false : incremental;
            if (rollback && z) {
                int runStart = index.start;
                int runLimit = index.limit;
                int runLength = runLimit - runStart;
                int rollbackOrigin = text.length();
                text.copy(runStart, runLimit, rollbackOrigin);
                int passStart = runStart;
                int rollbackStart = rollbackOrigin;
                int passLimit = index.start;
                int uncommittedLength = 0;
                int totalDelta = 0;
                while (true) {
                    int charLength = UTF16.getCharCount(text.char32At(passLimit));
                    passLimit += charLength;
                    if (passLimit > runLimit) {
                        break;
                    }
                    uncommittedLength += charLength;
                    index.limit = passLimit;
                    handleTransliterate(text, index, true);
                    int delta = index.limit - passLimit;
                    if (index.start != index.limit) {
                        int rs = (rollbackStart + delta) - (index.limit - passStart);
                        text.replace(passStart, index.limit, "");
                        text.copy(rs, rs + uncommittedLength, passStart);
                        index.start = passStart;
                        index.limit = passLimit;
                        index.contextLimit -= delta;
                    } else {
                        passLimit = index.start;
                        passStart = passLimit;
                        rollbackStart += delta + uncommittedLength;
                        uncommittedLength = 0;
                        runLimit += delta;
                        totalDelta += delta;
                    }
                }
                int rollbackOrigin2 = rollbackOrigin + totalDelta;
                globalLimit += totalDelta;
                text.replace(rollbackOrigin2, rollbackOrigin2 + runLength, "");
                index.start = passStart;
            } else {
                int limit = index.limit;
                handleTransliterate(text, index, z);
                int delta2 = index.limit - limit;
                if (!z && index.start != index.limit) {
                    throw new RuntimeException("ERROR: Incomplete non-incremental transliteration by " + getID());
                }
                globalLimit += delta2;
            }
            if (this.filter == null) {
                break;
            }
        } while (!z);
        index.limit = globalLimit;
    }

    public void filteredTransliterate(Replaceable text, Position index, boolean incremental) {
        filteredTransliterate(text, index, incremental, false);
    }

    public final int getMaximumContextLength() {
        return this.maximumContextLength;
    }

    protected void setMaximumContextLength(int a) {
        if (a < 0) {
            throw new IllegalArgumentException("Invalid context length " + a);
        }
        this.maximumContextLength = a;
    }

    public final String getID() {
        return this.ID;
    }

    protected final void setID(String id) {
        this.ID = id;
    }

    public static final String getDisplayName(String ID) {
        return getDisplayName(ID, ULocale.getDefault(ULocale.Category.DISPLAY));
    }

    public static String getDisplayName(String id, Locale inLocale) {
        return getDisplayName(id, ULocale.forLocale(inLocale));
    }

    public static String getDisplayName(String id, ULocale inLocale) {
        ICUResourceBundle bundle = (ICUResourceBundle) UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b/translit", inLocale);
        String[] stv = TransliteratorIDParser.IDtoSTV(id);
        if (stv == null) {
            return "";
        }
        String ID = stv[0] + ID_SEP + stv[1];
        if (stv[2] != null && stv[2].length() > 0) {
            ID = ID + VARIANT_SEP + stv[2];
        }
        String n = displayNameCache.get(new CaseInsensitiveString(ID));
        if (n != null) {
            return n;
        }
        try {
            return bundle.getString(RB_DISPLAY_NAME_PREFIX + ID);
        } catch (MissingResourceException e) {
            try {
                java.text.MessageFormat format = new java.text.MessageFormat(bundle.getString(RB_DISPLAY_NAME_PATTERN));
                Object[] args = new Object[3];
                args[0] = 2;
                args[1] = stv[0];
                args[2] = stv[1];
                for (int j = 1; j <= 2; j++) {
                    try {
                        args[j] = bundle.getString(RB_SCRIPT_DISPLAY_NAME_PREFIX + ((String) args[j]));
                    } catch (MissingResourceException e2) {
                    }
                }
                if (stv[2].length() > 0) {
                    return format.format(args) + VARIANT_SEP + stv[2];
                }
                return format.format(args);
            } catch (MissingResourceException e3) {
                throw new RuntimeException();
            }
        }
    }

    public final UnicodeFilter getFilter() {
        return this.filter;
    }

    public void setFilter(UnicodeFilter filter) {
        if (filter == null) {
            this.filter = null;
            return;
        }
        try {
            this.filter = new UnicodeSet((UnicodeSet) filter).freeze();
        } catch (Exception e) {
            this.filter = new UnicodeSet();
            filter.addMatchSetTo(this.filter);
            this.filter.freeze();
        }
    }

    public static final Transliterator getInstance(String ID) {
        return getInstance(ID, 0);
    }

    public static Transliterator getInstance(String ID, int dir) {
        Transliterator t;
        StringBuffer canonID = new StringBuffer();
        List<TransliteratorIDParser.SingleID> list = new ArrayList<>();
        UnicodeSet[] globalFilter = new UnicodeSet[1];
        if (!TransliteratorIDParser.parseCompoundID(ID, dir, canonID, list, globalFilter)) {
            throw new IllegalArgumentException("Invalid ID " + ID);
        }
        List<Transliterator> translits = TransliteratorIDParser.instantiateList(list);
        if (list.size() > 1 || canonID.indexOf(";") >= 0) {
            t = new CompoundTransliterator(translits);
        } else {
            Transliterator t2 = translits.get(0);
            t = t2;
        }
        t.setID(canonID.toString());
        if (globalFilter[0] != null) {
            t.setFilter(globalFilter[0]);
        }
        return t;
    }

    static Transliterator getBasicInstance(String id, String canonID) {
        StringBuffer s = new StringBuffer();
        Transliterator t = registry.get(id, s);
        if (s.length() != 0) {
            t = getInstance(s.toString(), 0);
        }
        if (t != null && canonID != null) {
            t.setID(canonID);
        }
        return t;
    }

    public static final Transliterator createFromRules(String ID, String rules, int dir) {
        int passNumber;
        Transliterator t;
        TransliteratorParser parser = new TransliteratorParser();
        parser.parse(rules, dir);
        if (parser.idBlockVector.size() == 0 && parser.dataVector.size() == 0) {
            return new NullTransliterator();
        }
        if (parser.idBlockVector.size() == 0 && parser.dataVector.size() == 1) {
            return new RuleBasedTransliterator(ID, parser.dataVector.get(0), parser.compoundFilter);
        }
        if (parser.idBlockVector.size() == 1 && parser.dataVector.size() == 0) {
            if (parser.compoundFilter != null) {
                t = getInstance(parser.compoundFilter.toPattern(false) + ";" + parser.idBlockVector.get(0));
            } else {
                t = getInstance(parser.idBlockVector.get(0));
            }
            if (t != null) {
                t.setID(ID);
                return t;
            }
            return t;
        }
        List<Transliterator> transliterators = new ArrayList<>();
        int limit = Math.max(parser.idBlockVector.size(), parser.dataVector.size());
        int i = 0;
        int passNumber2 = 1;
        while (i < limit) {
            if (i < parser.idBlockVector.size()) {
                String idBlock = parser.idBlockVector.get(i);
                if (idBlock.length() > 0) {
                    Transliterator temp = getInstance(idBlock);
                    if (!(temp instanceof NullTransliterator)) {
                        transliterators.add(getInstance(idBlock));
                    }
                }
            }
            if (i < parser.dataVector.size()) {
                RuleBasedTransliterator.Data data = parser.dataVector.get(i);
                passNumber = passNumber2 + 1;
                transliterators.add(new RuleBasedTransliterator("%Pass" + passNumber2, data, null));
            } else {
                passNumber = passNumber2;
            }
            i++;
            passNumber2 = passNumber;
        }
        Transliterator t2 = new CompoundTransliterator(transliterators, passNumber2 - 1);
        t2.setID(ID);
        if (parser.compoundFilter != null) {
            t2.setFilter(parser.compoundFilter);
            return t2;
        }
        return t2;
    }

    public String toRules(boolean escapeUnprintable) {
        return baseToRules(escapeUnprintable);
    }

    protected final String baseToRules(boolean escapeUnprintable) {
        if (escapeUnprintable) {
            StringBuffer rulesSource = new StringBuffer();
            String id = getID();
            int i = 0;
            while (i < id.length()) {
                int c = UTF16.charAt(id, i);
                if (!Utility.escapeUnprintable(rulesSource, c)) {
                    UTF16.append(rulesSource, c);
                }
                i += UTF16.getCharCount(c);
            }
            rulesSource.insert(0, "::");
            rulesSource.append(ID_DELIM);
            return rulesSource.toString();
        }
        return "::" + getID() + ID_DELIM;
    }

    public Transliterator[] getElements() {
        if (this instanceof CompoundTransliterator) {
            Transliterator[] result = new Transliterator[getCount()];
            for (int i = 0; i < result.length; i++) {
                result[i] = getTransliterator(i);
            }
            return result;
        }
        return new Transliterator[]{this};
    }

    public final UnicodeSet getSourceSet() {
        UnicodeSet result = new UnicodeSet();
        addSourceTargetSet(getFilterAsUnicodeSet(UnicodeSet.ALL_CODE_POINTS), result, new UnicodeSet());
        return result;
    }

    protected UnicodeSet handleGetSourceSet() {
        return new UnicodeSet();
    }

    public UnicodeSet getTargetSet() {
        UnicodeSet result = new UnicodeSet();
        addSourceTargetSet(getFilterAsUnicodeSet(UnicodeSet.ALL_CODE_POINTS), new UnicodeSet(), result);
        return result;
    }

    @Deprecated
    public void addSourceTargetSet(UnicodeSet inputFilter, UnicodeSet sourceSet, UnicodeSet targetSet) {
        UnicodeSet myFilter = getFilterAsUnicodeSet(inputFilter);
        UnicodeSet temp = new UnicodeSet(handleGetSourceSet()).retainAll(myFilter);
        sourceSet.addAll(temp);
        for (String s : temp) {
            String t = transliterate(s);
            if (!s.equals(t)) {
                targetSet.addAll(t);
            }
        }
    }

    @Deprecated
    public UnicodeSet getFilterAsUnicodeSet(UnicodeSet externalFilter) {
        UnicodeSet temp;
        if (this.filter == null) {
            return externalFilter;
        }
        UnicodeSet filterSet = new UnicodeSet(externalFilter);
        try {
            temp = this.filter;
        } catch (ClassCastException e) {
            UnicodeSet unicodeSet = this.filter;
            temp = new UnicodeSet();
            unicodeSet.addMatchSetTo(temp);
        }
        return filterSet.retainAll(temp).freeze();
    }

    public final Transliterator getInverse() {
        return getInstance(this.ID, 1);
    }

    public static void registerClass(String ID, Class<? extends Transliterator> transClass, String displayName) {
        registry.put(ID, transClass, true);
        if (displayName == null) {
            return;
        }
        displayNameCache.put(new CaseInsensitiveString(ID), displayName);
    }

    public static void registerFactory(String ID, Factory factory) {
        registry.put(ID, factory, true);
    }

    public static void registerInstance(Transliterator trans) {
        registry.put(trans.getID(), trans, true);
    }

    static void registerInstance(Transliterator trans, boolean visible) {
        registry.put(trans.getID(), trans, visible);
    }

    public static void registerAlias(String aliasID, String realID) {
        registry.put(aliasID, realID, true);
    }

    static void registerSpecialInverse(String target, String inverseTarget, boolean bidirectional) {
        TransliteratorIDParser.registerSpecialInverse(target, inverseTarget, bidirectional);
    }

    public static void unregister(String ID) {
        displayNameCache.remove(new CaseInsensitiveString(ID));
        registry.remove(ID);
    }

    public static final Enumeration<String> getAvailableIDs() {
        return registry.getAvailableIDs();
    }

    public static final Enumeration<String> getAvailableSources() {
        return registry.getAvailableSources();
    }

    public static final Enumeration<String> getAvailableTargets(String source) {
        return registry.getAvailableTargets(source);
    }

    public static final Enumeration<String> getAvailableVariants(String source, String target) {
        return registry.getAvailableVariants(source, target);
    }

    static {
        int dir;
        UResourceBundle bundle = UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b/translit", ROOT);
        UResourceBundle transIDs = bundle.get(RB_RULE_BASED_IDS);
        int maxRows = transIDs.getSize();
        for (int row = 0; row < maxRows; row++) {
            UResourceBundle colBund = transIDs.get(row);
            String ID = colBund.getKey();
            UResourceBundle res = colBund.get(0);
            String type = res.getKey();
            if (type.equals("file") || type.equals("internal")) {
                String resString = res.getString("resource");
                String direction = res.getString("direction");
                switch (direction.charAt(0)) {
                    case 'F':
                        dir = 0;
                        break;
                    case 'R':
                        dir = 1;
                        break;
                    default:
                        throw new RuntimeException("Can't parse direction: " + direction);
                }
                registry.put(ID, resString, "UTF-16", dir, !type.equals("internal"));
            } else if (type.equals("alias")) {
                String resString2 = res.getString();
                registry.put(ID, resString2, true);
            } else {
                throw new RuntimeException("Unknow type: " + type);
            }
        }
        registerSpecialInverse(NullTransliterator.SHORT_ID, NullTransliterator.SHORT_ID, false);
        registerClass(NullTransliterator._ID, NullTransliterator.class, null);
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
    public String transform(String source) {
        return transliterate(source);
    }
}
