package android.icu.impl;

import android.icu.impl.CurrencyData;
import android.icu.impl.UResource;
import android.icu.impl.coll.CollationSettings;
import android.icu.impl.locale.AsciiUtil;
import android.icu.lang.UCharacter;
import android.icu.lang.UScript;
import android.icu.text.BreakIterator;
import android.icu.text.CaseMap;
import android.icu.text.DisplayContext;
import android.icu.text.LocaleDisplayNames;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;

public class LocaleDisplayNamesImpl extends LocaleDisplayNames {
    private static final CaseMap.Title TO_TITLE_WHOLE_STRING_NO_LOWERCASE;
    private static final Cache cache = new Cache();
    private static final Map<String, CapitalizationContextUsage> contextUsageTypeMap = new HashMap();
    private final DisplayContext capitalization;
    private transient BreakIterator capitalizationBrkIter;
    private boolean[] capitalizationUsage;
    private final CurrencyData.CurrencyDisplayInfo currencyDisplayInfo;
    private final LocaleDisplayNames.DialectHandling dialectHandling;
    private final String format;
    private final char formatCloseParen;
    private final char formatOpenParen;
    private final char formatReplaceCloseParen;
    private final char formatReplaceOpenParen;
    private final String keyTypeFormat;
    private final DataTable langData;
    private final ULocale locale;
    private final DisplayContext nameLength;
    private final DataTable regionData;
    private final String separatorFormat;
    private final DisplayContext substituteHandling;

    private enum CapitalizationContextUsage {
        LANGUAGE,
        SCRIPT,
        TERRITORY,
        VARIANT,
        KEY,
        KEYVALUE
    }

    public enum DataTableType {
        LANG,
        REGION
    }

    static {
        contextUsageTypeMap.put("languages", CapitalizationContextUsage.LANGUAGE);
        contextUsageTypeMap.put("script", CapitalizationContextUsage.SCRIPT);
        contextUsageTypeMap.put("territory", CapitalizationContextUsage.TERRITORY);
        contextUsageTypeMap.put("variant", CapitalizationContextUsage.VARIANT);
        contextUsageTypeMap.put("key", CapitalizationContextUsage.KEY);
        contextUsageTypeMap.put("keyValue", CapitalizationContextUsage.KEYVALUE);
        TO_TITLE_WHOLE_STRING_NO_LOWERCASE = CaseMap.toTitle().wholeString().noLowercase();
    }

    private static String toTitleWholeStringNoLowercase(ULocale uLocale, String str) {
        return ((StringBuilder) TO_TITLE_WHOLE_STRING_NO_LOWERCASE.apply(uLocale.toLocale(), null, str, new StringBuilder(), null)).toString();
    }

    public static LocaleDisplayNames getInstance(ULocale uLocale, LocaleDisplayNames.DialectHandling dialectHandling) {
        LocaleDisplayNames localeDisplayNames;
        synchronized (cache) {
            localeDisplayNames = cache.get(uLocale, dialectHandling);
        }
        return localeDisplayNames;
    }

    public static LocaleDisplayNames getInstance(ULocale uLocale, DisplayContext... displayContextArr) {
        LocaleDisplayNames localeDisplayNames;
        synchronized (cache) {
            localeDisplayNames = cache.get(uLocale, displayContextArr);
        }
        return localeDisplayNames;
    }

    private final class CapitalizationContextSink extends UResource.Sink {
        boolean hasCapitalizationUsage;

        private CapitalizationContextSink() {
            this.hasCapitalizationUsage = false;
        }

        @Override
        public void put(UResource.Key key, UResource.Value value, boolean z) {
            UResource.Table table = value.getTable();
            for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                CapitalizationContextUsage capitalizationContextUsage = (CapitalizationContextUsage) LocaleDisplayNamesImpl.contextUsageTypeMap.get(key.toString());
                if (capitalizationContextUsage != null) {
                    int[] intVector = value.getIntVector();
                    if (intVector.length >= 2) {
                        if ((LocaleDisplayNamesImpl.this.capitalization == DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU ? intVector[0] : intVector[1]) != 0) {
                            LocaleDisplayNamesImpl.this.capitalizationUsage[capitalizationContextUsage.ordinal()] = true;
                            this.hasCapitalizationUsage = true;
                        }
                    }
                }
            }
        }
    }

    public LocaleDisplayNamesImpl(ULocale uLocale, LocaleDisplayNames.DialectHandling dialectHandling) {
        DisplayContext[] displayContextArr = new DisplayContext[2];
        displayContextArr[0] = dialectHandling == LocaleDisplayNames.DialectHandling.STANDARD_NAMES ? DisplayContext.STANDARD_NAMES : DisplayContext.DIALECT_NAMES;
        displayContextArr[1] = DisplayContext.CAPITALIZATION_NONE;
        this(uLocale, displayContextArr);
    }

    public LocaleDisplayNamesImpl(ULocale uLocale, DisplayContext... displayContextArr) {
        boolean z;
        this.capitalizationUsage = null;
        this.capitalizationBrkIter = null;
        LocaleDisplayNames.DialectHandling dialectHandling = LocaleDisplayNames.DialectHandling.STANDARD_NAMES;
        DisplayContext displayContext = DisplayContext.CAPITALIZATION_NONE;
        DisplayContext displayContext2 = DisplayContext.LENGTH_FULL;
        DisplayContext displayContext3 = DisplayContext.SUBSTITUTE;
        DisplayContext displayContext4 = displayContext2;
        DisplayContext displayContext5 = displayContext;
        LocaleDisplayNames.DialectHandling dialectHandling2 = dialectHandling;
        for (DisplayContext displayContext6 : displayContextArr) {
            switch (displayContext6.type()) {
                case DIALECT_HANDLING:
                    dialectHandling2 = displayContext6.value() == DisplayContext.STANDARD_NAMES.value() ? LocaleDisplayNames.DialectHandling.STANDARD_NAMES : LocaleDisplayNames.DialectHandling.DIALECT_NAMES;
                    break;
                case CAPITALIZATION:
                    displayContext5 = displayContext6;
                    break;
                case DISPLAY_LENGTH:
                    displayContext4 = displayContext6;
                    break;
                case SUBSTITUTE_HANDLING:
                    displayContext3 = displayContext6;
                    break;
            }
        }
        this.dialectHandling = dialectHandling2;
        this.capitalization = displayContext5;
        this.nameLength = displayContext4;
        this.substituteHandling = displayContext3;
        this.langData = LangDataTables.impl.get(uLocale, displayContext3 == DisplayContext.NO_SUBSTITUTE);
        this.regionData = RegionDataTables.impl.get(uLocale, displayContext3 == DisplayContext.NO_SUBSTITUTE);
        this.locale = ULocale.ROOT.equals(this.langData.getLocale()) ? this.regionData.getLocale() : this.langData.getLocale();
        String str = this.langData.get("localeDisplayPattern", "separator");
        str = (str == null || "separator".equals(str)) ? "{0}, {1}" : str;
        StringBuilder sb = new StringBuilder();
        this.separatorFormat = SimpleFormatterImpl.compileToStringMinMaxArguments(str, sb, 2, 2);
        String str2 = this.langData.get("localeDisplayPattern", "pattern");
        str2 = (str2 == null || "pattern".equals(str2)) ? "{0} ({1})" : str2;
        this.format = SimpleFormatterImpl.compileToStringMinMaxArguments(str2, sb, 2, 2);
        if (str2.contains("（")) {
            this.formatOpenParen = (char) 65288;
            this.formatCloseParen = (char) 65289;
            this.formatReplaceOpenParen = (char) 65339;
            this.formatReplaceCloseParen = (char) 65341;
        } else {
            this.formatOpenParen = '(';
            this.formatCloseParen = ')';
            this.formatReplaceOpenParen = '[';
            this.formatReplaceCloseParen = ']';
        }
        String str3 = this.langData.get("localeDisplayPattern", "keyTypePattern");
        this.keyTypeFormat = SimpleFormatterImpl.compileToStringMinMaxArguments((str3 == null || "keyTypePattern".equals(str3)) ? "{0}={1}" : str3, sb, 2, 2);
        if (displayContext5 == DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU || displayContext5 == DisplayContext.CAPITALIZATION_FOR_STANDALONE) {
            this.capitalizationUsage = new boolean[CapitalizationContextUsage.values().length];
            ICUResourceBundle iCUResourceBundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, uLocale);
            CapitalizationContextSink capitalizationContextSink = new CapitalizationContextSink();
            try {
                iCUResourceBundle.getAllItemsWithFallback("contextTransforms", capitalizationContextSink);
            } catch (MissingResourceException e) {
            }
            z = capitalizationContextSink.hasCapitalizationUsage;
        } else {
            z = false;
        }
        if (z || displayContext5 == DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE) {
            this.capitalizationBrkIter = BreakIterator.getSentenceInstance(uLocale);
        }
        this.currencyDisplayInfo = CurrencyData.provider.getInstance(uLocale, false);
    }

    @Override
    public ULocale getLocale() {
        return this.locale;
    }

    @Override
    public LocaleDisplayNames.DialectHandling getDialectHandling() {
        return this.dialectHandling;
    }

    @Override
    public DisplayContext getContext(DisplayContext.Type type) {
        switch (type) {
            case DIALECT_HANDLING:
                return this.dialectHandling == LocaleDisplayNames.DialectHandling.STANDARD_NAMES ? DisplayContext.STANDARD_NAMES : DisplayContext.DIALECT_NAMES;
            case CAPITALIZATION:
                return this.capitalization;
            case DISPLAY_LENGTH:
                return this.nameLength;
            case SUBSTITUTE_HANDLING:
                return this.substituteHandling;
            default:
                return DisplayContext.STANDARD_NAMES;
        }
    }

    private String adjustForUsageAndContext(CapitalizationContextUsage capitalizationContextUsage, String str) {
        String titleCase;
        if (str != null && str.length() > 0 && UCharacter.isLowerCase(str.codePointAt(0)) && (this.capitalization == DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE || (this.capitalizationUsage != null && this.capitalizationUsage[capitalizationContextUsage.ordinal()]))) {
            synchronized (this) {
                if (this.capitalizationBrkIter == null) {
                    this.capitalizationBrkIter = BreakIterator.getSentenceInstance(this.locale);
                }
                titleCase = UCharacter.toTitleCase(this.locale, str, this.capitalizationBrkIter, CollationSettings.CASE_FIRST_AND_UPPER_MASK);
            }
            return titleCase;
        }
        return str;
    }

    @Override
    public String localeDisplayName(ULocale uLocale) {
        return localeDisplayNameInternal(uLocale);
    }

    @Override
    public String localeDisplayName(Locale locale) {
        return localeDisplayNameInternal(ULocale.forLocale(locale));
    }

    @Override
    public String localeDisplayName(String str) {
        return localeDisplayNameInternal(new ULocale(str));
    }

    private String localeDisplayNameInternal(ULocale uLocale) {
        String compiledPattern;
        String language = uLocale.getLanguage();
        if (uLocale.getBaseName().length() == 0) {
            language = "root";
        }
        String script = uLocale.getScript();
        String country = uLocale.getCountry();
        String variant = uLocale.getVariant();
        boolean z = script.length() > 0;
        boolean z2 = country.length() > 0;
        boolean z3 = variant.length() > 0;
        if (this.dialectHandling != LocaleDisplayNames.DialectHandling.DIALECT_NAMES) {
            compiledPattern = null;
        } else if (z && z2) {
            String str = language + '_' + script + '_' + country;
            compiledPattern = localeIdName(str);
            if (compiledPattern != null && !compiledPattern.equals(str)) {
                z = false;
                z2 = false;
            }
        } else if (z) {
            String str2 = language + '_' + script;
            compiledPattern = localeIdName(str2);
            if (compiledPattern == null || compiledPattern.equals(str2)) {
                if (z2) {
                    String str3 = language + '_' + country;
                    String strLocaleIdName = localeIdName(str3);
                    if (strLocaleIdName != null && !strLocaleIdName.equals(str3)) {
                        z2 = false;
                        compiledPattern = strLocaleIdName;
                    }
                }
            } else {
                z = false;
            }
        }
        if (compiledPattern == null) {
            String strLocaleIdName2 = localeIdName(language);
            if (strLocaleIdName2 == null) {
                return null;
            }
            compiledPattern = strLocaleIdName2.replace(this.formatOpenParen, this.formatReplaceOpenParen).replace(this.formatCloseParen, this.formatReplaceCloseParen);
        }
        StringBuilder sb = new StringBuilder();
        if (z) {
            String strScriptDisplayNameInContext = scriptDisplayNameInContext(script, true);
            if (strScriptDisplayNameInContext == null) {
                return null;
            }
            sb.append(strScriptDisplayNameInContext.replace(this.formatOpenParen, this.formatReplaceOpenParen).replace(this.formatCloseParen, this.formatReplaceCloseParen));
        }
        if (z2) {
            String strRegionDisplayName = regionDisplayName(country, true);
            if (strRegionDisplayName == null) {
                return null;
            }
            appendWithSep(strRegionDisplayName.replace(this.formatOpenParen, this.formatReplaceOpenParen).replace(this.formatCloseParen, this.formatReplaceCloseParen), sb);
        }
        if (z3) {
            String strVariantDisplayName = variantDisplayName(variant, true);
            if (strVariantDisplayName == null) {
                return null;
            }
            appendWithSep(strVariantDisplayName.replace(this.formatOpenParen, this.formatReplaceOpenParen).replace(this.formatCloseParen, this.formatReplaceCloseParen), sb);
        }
        Iterator<String> keywords = uLocale.getKeywords();
        if (keywords != null) {
            while (keywords.hasNext()) {
                String next = keywords.next();
                String keywordValue = uLocale.getKeywordValue(next);
                String strKeyDisplayName = keyDisplayName(next, true);
                if (strKeyDisplayName == null) {
                    return null;
                }
                String strReplace = strKeyDisplayName.replace(this.formatOpenParen, this.formatReplaceOpenParen).replace(this.formatCloseParen, this.formatReplaceCloseParen);
                String strKeyValueDisplayName = keyValueDisplayName(next, keywordValue, true);
                if (strKeyValueDisplayName == null) {
                    return null;
                }
                String strReplace2 = strKeyValueDisplayName.replace(this.formatOpenParen, this.formatReplaceOpenParen).replace(this.formatCloseParen, this.formatReplaceCloseParen);
                if (!strReplace2.equals(keywordValue)) {
                    appendWithSep(strReplace2, sb);
                } else if (!next.equals(strReplace)) {
                    appendWithSep(SimpleFormatterImpl.formatCompiledPattern(this.keyTypeFormat, strReplace, strReplace2), sb);
                } else {
                    StringBuilder sbAppendWithSep = appendWithSep(strReplace, sb);
                    sbAppendWithSep.append("=");
                    sbAppendWithSep.append(strReplace2);
                }
            }
        }
        String string = sb.length() > 0 ? sb.toString() : null;
        if (string != null) {
            compiledPattern = SimpleFormatterImpl.formatCompiledPattern(this.format, compiledPattern, string);
        }
        return adjustForUsageAndContext(CapitalizationContextUsage.LANGUAGE, compiledPattern);
    }

    private String localeIdName(String str) {
        String str2;
        if (this.nameLength == DisplayContext.LENGTH_SHORT && (str2 = this.langData.get("Languages%short", str)) != null && !str2.equals(str)) {
            return str2;
        }
        return this.langData.get("Languages", str);
    }

    @Override
    public String languageDisplayName(String str) {
        String str2;
        if (str.equals("root") || str.indexOf(95) != -1) {
            if (this.substituteHandling == DisplayContext.SUBSTITUTE) {
                return str;
            }
            return null;
        }
        if (this.nameLength == DisplayContext.LENGTH_SHORT && (str2 = this.langData.get("Languages%short", str)) != null && !str2.equals(str)) {
            return adjustForUsageAndContext(CapitalizationContextUsage.LANGUAGE, str2);
        }
        return adjustForUsageAndContext(CapitalizationContextUsage.LANGUAGE, this.langData.get("Languages", str));
    }

    @Override
    public String scriptDisplayName(String str) {
        String str2;
        String str3 = this.langData.get("Scripts%stand-alone", str);
        if (str3 == null || str3.equals(str)) {
            if (this.nameLength == DisplayContext.LENGTH_SHORT && (str2 = this.langData.get("Scripts%short", str)) != null && !str2.equals(str)) {
                return adjustForUsageAndContext(CapitalizationContextUsage.SCRIPT, str2);
            }
            str3 = this.langData.get("Scripts", str);
        }
        return adjustForUsageAndContext(CapitalizationContextUsage.SCRIPT, str3);
    }

    private String scriptDisplayNameInContext(String str, boolean z) {
        String str2;
        if (this.nameLength == DisplayContext.LENGTH_SHORT && (str2 = this.langData.get("Scripts%short", str)) != null && !str2.equals(str)) {
            return z ? str2 : adjustForUsageAndContext(CapitalizationContextUsage.SCRIPT, str2);
        }
        String str3 = this.langData.get("Scripts", str);
        return z ? str3 : adjustForUsageAndContext(CapitalizationContextUsage.SCRIPT, str3);
    }

    @Override
    public String scriptDisplayNameInContext(String str) {
        return scriptDisplayNameInContext(str, false);
    }

    @Override
    public String scriptDisplayName(int i) {
        return scriptDisplayName(UScript.getShortName(i));
    }

    private String regionDisplayName(String str, boolean z) {
        String str2;
        if (this.nameLength == DisplayContext.LENGTH_SHORT && (str2 = this.regionData.get("Countries%short", str)) != null && !str2.equals(str)) {
            return z ? str2 : adjustForUsageAndContext(CapitalizationContextUsage.TERRITORY, str2);
        }
        String str3 = this.regionData.get("Countries", str);
        return z ? str3 : adjustForUsageAndContext(CapitalizationContextUsage.TERRITORY, str3);
    }

    @Override
    public String regionDisplayName(String str) {
        return regionDisplayName(str, false);
    }

    private String variantDisplayName(String str, boolean z) {
        String str2 = this.langData.get("Variants", str);
        return z ? str2 : adjustForUsageAndContext(CapitalizationContextUsage.VARIANT, str2);
    }

    @Override
    public String variantDisplayName(String str) {
        return variantDisplayName(str, false);
    }

    private String keyDisplayName(String str, boolean z) {
        String str2 = this.langData.get("Keys", str);
        return z ? str2 : adjustForUsageAndContext(CapitalizationContextUsage.KEY, str2);
    }

    @Override
    public String keyDisplayName(String str) {
        return keyDisplayName(str, false);
    }

    private String keyValueDisplayName(String str, String str2, boolean z) {
        String str3;
        if (str.equals("currency")) {
            String name = this.currencyDisplayInfo.getName(AsciiUtil.toUpperString(str2));
            if (name != null) {
                str2 = name;
            }
        } else {
            if (this.nameLength != DisplayContext.LENGTH_SHORT || (str3 = this.langData.get("Types%short", str, str2)) == null || str3.equals(str2)) {
                str3 = null;
            }
            str2 = str3 == null ? this.langData.get("Types", str, str2) : str3;
        }
        return z ? str2 : adjustForUsageAndContext(CapitalizationContextUsage.KEYVALUE, str2);
    }

    @Override
    public String keyValueDisplayName(String str, String str2) {
        return keyValueDisplayName(str, str2, false);
    }

    @Override
    public List<LocaleDisplayNames.UiListItem> getUiListCompareWholeItems(Set<ULocale> set, Comparator<LocaleDisplayNames.UiListItem> comparator) {
        DisplayContext context = getContext(DisplayContext.Type.CAPITALIZATION);
        ArrayList arrayList = new ArrayList();
        HashMap map = new HashMap();
        ULocale.Builder builder = new ULocale.Builder();
        for (ULocale uLocale : set) {
            builder.setLocale(uLocale);
            ULocale uLocaleAddLikelySubtags = ULocale.addLikelySubtags(uLocale);
            ULocale uLocale2 = new ULocale(uLocaleAddLikelySubtags.getLanguage());
            Set hashSet = (Set) map.get(uLocale2);
            if (hashSet == null) {
                hashSet = new HashSet();
                map.put(uLocale2, hashSet);
            }
            hashSet.add(uLocaleAddLikelySubtags);
        }
        for (Map.Entry entry : map.entrySet()) {
            ULocale uLocale3 = (ULocale) entry.getKey();
            Set<ULocale> set2 = (Set) entry.getValue();
            if (set2.size() == 1) {
                arrayList.add(newRow(ULocale.minimizeSubtags((ULocale) set2.iterator().next(), ULocale.Minimize.FAVOR_SCRIPT), context));
            } else {
                HashSet hashSet2 = new HashSet();
                HashSet hashSet3 = new HashSet();
                ULocale uLocaleAddLikelySubtags2 = ULocale.addLikelySubtags(uLocale3);
                hashSet2.add(uLocaleAddLikelySubtags2.getScript());
                hashSet3.add(uLocaleAddLikelySubtags2.getCountry());
                for (ULocale uLocale4 : set2) {
                    hashSet2.add(uLocale4.getScript());
                    hashSet3.add(uLocale4.getCountry());
                }
                boolean z = hashSet2.size() > 1;
                boolean z2 = hashSet3.size() > 1;
                Iterator it = set2.iterator();
                while (it.hasNext()) {
                    ULocale.Builder locale = builder.setLocale((ULocale) it.next());
                    if (!z) {
                        locale.setScript("");
                    }
                    if (!z2) {
                        locale.setRegion("");
                    }
                    arrayList.add(newRow(locale.build(), context));
                }
            }
        }
        Collections.sort(arrayList, comparator);
        return arrayList;
    }

    private LocaleDisplayNames.UiListItem newRow(ULocale uLocale, DisplayContext displayContext) {
        ULocale uLocaleMinimizeSubtags = ULocale.minimizeSubtags(uLocale, ULocale.Minimize.FAVOR_SCRIPT);
        String displayName = uLocale.getDisplayName(this.locale);
        if (displayContext == DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU) {
            displayName = toTitleWholeStringNoLowercase(this.locale, displayName);
        }
        String displayName2 = uLocale.getDisplayName(uLocale);
        if (displayContext == DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU) {
            displayName2 = toTitleWholeStringNoLowercase(uLocale, displayName2);
        }
        return new LocaleDisplayNames.UiListItem(uLocaleMinimizeSubtags, uLocale, displayName, displayName2);
    }

    public static class DataTable {
        final boolean nullIfNotFound;

        DataTable(boolean z) {
            this.nullIfNotFound = z;
        }

        ULocale getLocale() {
            return ULocale.ROOT;
        }

        String get(String str, String str2) {
            return get(str, null, str2);
        }

        String get(String str, String str2, String str3) {
            if (this.nullIfNotFound) {
                return null;
            }
            return str3;
        }
    }

    static class ICUDataTable extends DataTable {
        private final ICUResourceBundle bundle;

        public ICUDataTable(String str, ULocale uLocale, boolean z) {
            super(z);
            this.bundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(str, uLocale.getBaseName());
        }

        @Override
        public ULocale getLocale() {
            return this.bundle.getULocale();
        }

        @Override
        public String get(String str, String str2, String str3) {
            return ICUResourceTableAccess.getTableString(this.bundle, str, str2, str3, this.nullIfNotFound ? null : str3);
        }
    }

    static abstract class DataTables {
        public abstract DataTable get(ULocale uLocale, boolean z);

        DataTables() {
        }

        public static DataTables load(String str) {
            try {
                return (DataTables) Class.forName(str).newInstance();
            } catch (Throwable th) {
                return new DataTables() {
                    @Override
                    public DataTable get(ULocale uLocale, boolean z) {
                        return new DataTable(z);
                    }
                };
            }
        }
    }

    static abstract class ICUDataTables extends DataTables {
        private final String path;

        protected ICUDataTables(String str) {
            this.path = str;
        }

        @Override
        public DataTable get(ULocale uLocale, boolean z) {
            return new ICUDataTable(this.path, uLocale, z);
        }
    }

    static class LangDataTables {
        static final DataTables impl = DataTables.load("android.icu.impl.ICULangDataTables");

        LangDataTables() {
        }
    }

    static class RegionDataTables {
        static final DataTables impl = DataTables.load("android.icu.impl.ICURegionDataTables");

        RegionDataTables() {
        }
    }

    public static boolean haveData(DataTableType dataTableType) {
        switch (dataTableType) {
            case LANG:
                return LangDataTables.impl instanceof ICUDataTables;
            case REGION:
                return RegionDataTables.impl instanceof ICUDataTables;
            default:
                throw new IllegalArgumentException("unknown type: " + dataTableType);
        }
    }

    private StringBuilder appendWithSep(String str, StringBuilder sb) {
        if (sb.length() == 0) {
            sb.append(str);
        } else {
            SimpleFormatterImpl.formatAndReplace(this.separatorFormat, sb, null, sb, str);
        }
        return sb;
    }

    private static class Cache {
        private LocaleDisplayNames cache;
        private DisplayContext capitalization;
        private LocaleDisplayNames.DialectHandling dialectHandling;
        private ULocale locale;
        private DisplayContext nameLength;
        private DisplayContext substituteHandling;

        private Cache() {
        }

        public LocaleDisplayNames get(ULocale uLocale, LocaleDisplayNames.DialectHandling dialectHandling) {
            if (dialectHandling != this.dialectHandling || DisplayContext.CAPITALIZATION_NONE != this.capitalization || DisplayContext.LENGTH_FULL != this.nameLength || DisplayContext.SUBSTITUTE != this.substituteHandling || !uLocale.equals(this.locale)) {
                this.locale = uLocale;
                this.dialectHandling = dialectHandling;
                this.capitalization = DisplayContext.CAPITALIZATION_NONE;
                this.nameLength = DisplayContext.LENGTH_FULL;
                this.substituteHandling = DisplayContext.SUBSTITUTE;
                this.cache = new LocaleDisplayNamesImpl(uLocale, dialectHandling);
            }
            return this.cache;
        }

        public LocaleDisplayNames get(ULocale uLocale, DisplayContext... displayContextArr) {
            LocaleDisplayNames.DialectHandling dialectHandling = LocaleDisplayNames.DialectHandling.STANDARD_NAMES;
            DisplayContext displayContext = DisplayContext.CAPITALIZATION_NONE;
            DisplayContext displayContext2 = DisplayContext.LENGTH_FULL;
            DisplayContext displayContext3 = DisplayContext.SUBSTITUTE;
            for (DisplayContext displayContext4 : displayContextArr) {
                switch (displayContext4.type()) {
                    case DIALECT_HANDLING:
                        dialectHandling = displayContext4.value() == DisplayContext.STANDARD_NAMES.value() ? LocaleDisplayNames.DialectHandling.STANDARD_NAMES : LocaleDisplayNames.DialectHandling.DIALECT_NAMES;
                        break;
                    case CAPITALIZATION:
                        displayContext = displayContext4;
                        break;
                    case DISPLAY_LENGTH:
                        displayContext2 = displayContext4;
                        break;
                    case SUBSTITUTE_HANDLING:
                        displayContext3 = displayContext4;
                        break;
                }
            }
            if (dialectHandling != this.dialectHandling || displayContext != this.capitalization || displayContext2 != this.nameLength || displayContext3 != this.substituteHandling || !uLocale.equals(this.locale)) {
                this.locale = uLocale;
                this.dialectHandling = dialectHandling;
                this.capitalization = displayContext;
                this.nameLength = displayContext2;
                this.substituteHandling = displayContext3;
                this.cache = new LocaleDisplayNamesImpl(uLocale, displayContextArr);
            }
            return this.cache;
        }
    }
}
