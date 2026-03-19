package android.icu.impl;

import android.icu.impl.CurrencyData;
import android.icu.impl.locale.AsciiUtil;
import android.icu.lang.UCharacter;
import android.icu.lang.UScript;
import android.icu.text.BreakIterator;
import android.icu.text.DisplayContext;
import android.icu.text.LocaleDisplayNames;
import android.icu.text.MessageFormat;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import android.icu.util.UResourceBundleIterator;
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

    private static final int[] f11x3d2c68e = null;

    private static final int[] f12androidicutextDisplayContext$TypeSwitchesValues = null;
    private static final Cache cache = new Cache(null);
    private static final Map<String, CapitalizationContextUsage> contextUsageTypeMap = new HashMap();
    private final DisplayContext capitalization;
    private transient BreakIterator capitalizationBrkIter;
    private boolean[] capitalizationUsage;
    private final CurrencyData.CurrencyDisplayInfo currencyDisplayInfo;
    private final LocaleDisplayNames.DialectHandling dialectHandling;
    private final MessageFormat format;
    private final char formatCloseParen;
    private final char formatOpenParen;
    private final char formatReplaceCloseParen;
    private final char formatReplaceOpenParen;
    private final MessageFormat keyTypeFormat;
    private final DataTable langData;
    private final ULocale locale;
    private final DisplayContext nameLength;
    private final DataTable regionData;
    private final MessageFormat separatorFormat;

    private static int[] m31x6c76c532() {
        if (f11x3d2c68e != null) {
            return f11x3d2c68e;
        }
        int[] iArr = new int[DataTableType.valuesCustom().length];
        try {
            iArr[DataTableType.LANG.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[DataTableType.REGION.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        f11x3d2c68e = iArr;
        return iArr;
    }

    private static int[] m32getandroidicutextDisplayContext$TypeSwitchesValues() {
        if (f12androidicutextDisplayContext$TypeSwitchesValues != null) {
            return f12androidicutextDisplayContext$TypeSwitchesValues;
        }
        int[] iArr = new int[DisplayContext.Type.valuesCustom().length];
        try {
            iArr[DisplayContext.Type.CAPITALIZATION.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[DisplayContext.Type.DIALECT_HANDLING.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[DisplayContext.Type.DISPLAY_LENGTH.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        f12androidicutextDisplayContext$TypeSwitchesValues = iArr;
        return iArr;
    }

    static {
        contextUsageTypeMap.put("languages", CapitalizationContextUsage.LANGUAGE);
        contextUsageTypeMap.put("script", CapitalizationContextUsage.SCRIPT);
        contextUsageTypeMap.put("territory", CapitalizationContextUsage.TERRITORY);
        contextUsageTypeMap.put("variant", CapitalizationContextUsage.VARIANT);
        contextUsageTypeMap.put("key", CapitalizationContextUsage.KEY);
        contextUsageTypeMap.put("keyValue", CapitalizationContextUsage.KEYVALUE);
    }

    private enum CapitalizationContextUsage {
        LANGUAGE,
        SCRIPT,
        TERRITORY,
        VARIANT,
        KEY,
        KEYVALUE;

        public static CapitalizationContextUsage[] valuesCustom() {
            return values();
        }
    }

    public static LocaleDisplayNames getInstance(ULocale locale, LocaleDisplayNames.DialectHandling dialectHandling) {
        LocaleDisplayNames localeDisplayNames;
        synchronized (cache) {
            localeDisplayNames = cache.get(locale, dialectHandling);
        }
        return localeDisplayNames;
    }

    public static LocaleDisplayNames getInstance(ULocale locale, DisplayContext... contexts) {
        LocaleDisplayNames localeDisplayNames;
        synchronized (cache) {
            localeDisplayNames = cache.get(locale, contexts);
        }
        return localeDisplayNames;
    }

    public LocaleDisplayNamesImpl(ULocale locale, LocaleDisplayNames.DialectHandling dialectHandling) {
        DisplayContext[] displayContextArr = new DisplayContext[2];
        displayContextArr[0] = dialectHandling == LocaleDisplayNames.DialectHandling.STANDARD_NAMES ? DisplayContext.STANDARD_NAMES : DisplayContext.DIALECT_NAMES;
        displayContextArr[1] = DisplayContext.CAPITALIZATION_NONE;
        this(locale, displayContextArr);
    }

    public LocaleDisplayNamesImpl(ULocale locale, DisplayContext... contexts) {
        UResourceBundle contextTransformsBundle;
        this.capitalizationUsage = null;
        this.capitalizationBrkIter = null;
        LocaleDisplayNames.DialectHandling dialectHandling = LocaleDisplayNames.DialectHandling.STANDARD_NAMES;
        DisplayContext capitalization = DisplayContext.CAPITALIZATION_NONE;
        DisplayContext nameLength = DisplayContext.LENGTH_FULL;
        for (DisplayContext contextItem : contexts) {
            switch (m32getandroidicutextDisplayContext$TypeSwitchesValues()[contextItem.type().ordinal()]) {
                case 1:
                    capitalization = contextItem;
                    break;
                case 2:
                    dialectHandling = contextItem.value() == DisplayContext.STANDARD_NAMES.value() ? LocaleDisplayNames.DialectHandling.STANDARD_NAMES : LocaleDisplayNames.DialectHandling.DIALECT_NAMES;
                    break;
                case 3:
                    nameLength = contextItem;
                    break;
            }
        }
        this.dialectHandling = dialectHandling;
        this.capitalization = capitalization;
        this.nameLength = nameLength;
        this.langData = LangDataTables.impl.get(locale);
        this.regionData = RegionDataTables.impl.get(locale);
        this.locale = ULocale.ROOT.equals(this.langData.getLocale()) ? this.regionData.getLocale() : this.langData.getLocale();
        String sep = this.langData.get("localeDisplayPattern", "separator");
        this.separatorFormat = new MessageFormat("separator".equals(sep) ? "{0}, {1}" : sep);
        String pattern = this.langData.get("localeDisplayPattern", "pattern");
        pattern = "pattern".equals(pattern) ? "{0} ({1})" : pattern;
        this.format = new MessageFormat(pattern);
        if (pattern.contains("（")) {
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
        String keyTypePattern = this.langData.get("localeDisplayPattern", "keyTypePattern");
        this.keyTypeFormat = new MessageFormat("keyTypePattern".equals(keyTypePattern) ? "{0}={1}" : keyTypePattern);
        boolean needBrkIter = false;
        if (capitalization == DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU || capitalization == DisplayContext.CAPITALIZATION_FOR_STANDALONE) {
            this.capitalizationUsage = new boolean[CapitalizationContextUsage.valuesCustom().length];
            ICUResourceBundle rb = (ICUResourceBundle) UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", locale);
            try {
                contextTransformsBundle = rb.getWithFallback("contextTransforms");
            } catch (MissingResourceException e) {
                contextTransformsBundle = null;
            }
            if (contextTransformsBundle != null) {
                UResourceBundleIterator ctIterator = contextTransformsBundle.getIterator();
                while (ctIterator.hasNext()) {
                    UResourceBundle contextTransformUsage = ctIterator.next();
                    int[] intVector = contextTransformUsage.getIntVector();
                    if (intVector.length >= 2) {
                        String usageKey = contextTransformUsage.getKey();
                        CapitalizationContextUsage usage = contextUsageTypeMap.get(usageKey);
                        if (usage != null) {
                            int titlecaseInt = capitalization == DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU ? intVector[0] : intVector[1];
                            if (titlecaseInt != 0) {
                                this.capitalizationUsage[usage.ordinal()] = true;
                                needBrkIter = true;
                            }
                        }
                    }
                }
            }
        }
        if (needBrkIter || capitalization == DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE) {
            this.capitalizationBrkIter = BreakIterator.getSentenceInstance(locale);
        }
        this.currencyDisplayInfo = CurrencyData.provider.getInstance(locale, false);
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
        switch (m32getandroidicutextDisplayContext$TypeSwitchesValues()[type.ordinal()]) {
            case 1:
                DisplayContext result = this.capitalization;
                return result;
            case 2:
                if (this.dialectHandling == LocaleDisplayNames.DialectHandling.STANDARD_NAMES) {
                    DisplayContext result2 = DisplayContext.STANDARD_NAMES;
                    return result2;
                }
                DisplayContext result3 = DisplayContext.DIALECT_NAMES;
                return result3;
            case 3:
                DisplayContext result4 = this.nameLength;
                return result4;
            default:
                DisplayContext result5 = DisplayContext.STANDARD_NAMES;
                return result5;
        }
    }

    private String adjustForUsageAndContext(CapitalizationContextUsage usage, String name) {
        String titleCase;
        if (name != null && name.length() > 0 && UCharacter.isLowerCase(name.codePointAt(0)) && (this.capitalization == DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE || (this.capitalizationUsage != null && this.capitalizationUsage[usage.ordinal()]))) {
            synchronized (this) {
                if (this.capitalizationBrkIter == null) {
                    this.capitalizationBrkIter = BreakIterator.getSentenceInstance(this.locale);
                }
                titleCase = UCharacter.toTitleCase(this.locale, name, this.capitalizationBrkIter, 768);
            }
            return titleCase;
        }
        return name;
    }

    @Override
    public String localeDisplayName(ULocale locale) {
        return localeDisplayNameInternal(locale);
    }

    @Override
    public String localeDisplayName(Locale locale) {
        return localeDisplayNameInternal(ULocale.forLocale(locale));
    }

    @Override
    public String localeDisplayName(String localeId) {
        return localeDisplayNameInternal(new ULocale(localeId));
    }

    private String localeDisplayNameInternal(ULocale locale) {
        String resultName = null;
        String lang = locale.getLanguage();
        if (locale.getBaseName().length() == 0) {
            lang = "root";
        }
        String script = locale.getScript();
        String country = locale.getCountry();
        String variant = locale.getVariant();
        boolean hasScript = script.length() > 0;
        boolean hasCountry = country.length() > 0;
        boolean hasVariant = variant.length() > 0;
        if (this.dialectHandling == LocaleDisplayNames.DialectHandling.DIALECT_NAMES) {
            if (hasScript && hasCountry) {
                String langScriptCountry = lang + '_' + script + '_' + country;
                String result = localeIdName(langScriptCountry);
                if (!result.equals(langScriptCountry)) {
                    resultName = result;
                    hasScript = false;
                    hasCountry = false;
                }
            } else if (hasScript) {
                String langScript = lang + '_' + script;
                String result2 = localeIdName(langScript);
                if (!result2.equals(langScript)) {
                    resultName = result2;
                    hasScript = false;
                } else if (hasCountry) {
                    String langCountry = lang + '_' + country;
                    String result3 = localeIdName(langCountry);
                    if (!result3.equals(langCountry)) {
                        resultName = result3;
                        hasCountry = false;
                    }
                }
            }
        }
        if (resultName == null) {
            resultName = localeIdName(lang).replace(this.formatOpenParen, this.formatReplaceOpenParen).replace(this.formatCloseParen, this.formatReplaceCloseParen);
        }
        StringBuilder buf = new StringBuilder();
        if (hasScript) {
            buf.append(scriptDisplayNameInContext(script).replace(this.formatOpenParen, this.formatReplaceOpenParen).replace(this.formatCloseParen, this.formatReplaceCloseParen));
        }
        if (hasCountry) {
            appendWithSep(regionDisplayName(country).replace(this.formatOpenParen, this.formatReplaceOpenParen).replace(this.formatCloseParen, this.formatReplaceCloseParen), buf);
        }
        if (hasVariant) {
            appendWithSep(variantDisplayName(variant).replace(this.formatOpenParen, this.formatReplaceOpenParen).replace(this.formatCloseParen, this.formatReplaceCloseParen), buf);
        }
        Iterator<String> keys = locale.getKeywords();
        if (keys != null) {
            while (keys.hasNext()) {
                String key = keys.next();
                String value = locale.getKeywordValue(key);
                String keyDisplayName = keyDisplayName(key).replace(this.formatOpenParen, this.formatReplaceOpenParen).replace(this.formatCloseParen, this.formatReplaceCloseParen);
                String valueDisplayName = keyValueDisplayName(key, value).replace(this.formatOpenParen, this.formatReplaceOpenParen).replace(this.formatCloseParen, this.formatReplaceCloseParen);
                if (!valueDisplayName.equals(value)) {
                    appendWithSep(valueDisplayName, buf);
                } else if (!key.equals(keyDisplayName)) {
                    String keyValue = this.keyTypeFormat.format(new String[]{keyDisplayName, valueDisplayName});
                    appendWithSep(keyValue, buf);
                } else {
                    appendWithSep(keyDisplayName, buf).append("=").append(valueDisplayName);
                }
            }
        }
        String resultRemainder = null;
        if (buf.length() > 0) {
            resultRemainder = buf.toString();
        }
        if (resultRemainder != null) {
            resultName = this.format.format(new Object[]{resultName, resultRemainder});
        }
        return adjustForUsageAndContext(CapitalizationContextUsage.LANGUAGE, resultName);
    }

    private String localeIdName(String localeId) {
        if (this.nameLength == DisplayContext.LENGTH_SHORT) {
            String locIdName = this.langData.get("Languages%short", localeId);
            if (!locIdName.equals(localeId)) {
                return locIdName;
            }
        }
        return this.langData.get("Languages", localeId);
    }

    @Override
    public String languageDisplayName(String lang) {
        if (lang.equals("root") || lang.indexOf(95) != -1) {
            return lang;
        }
        if (this.nameLength == DisplayContext.LENGTH_SHORT) {
            String langName = this.langData.get("Languages%short", lang);
            if (!langName.equals(lang)) {
                return adjustForUsageAndContext(CapitalizationContextUsage.LANGUAGE, langName);
            }
        }
        return adjustForUsageAndContext(CapitalizationContextUsage.LANGUAGE, this.langData.get("Languages", lang));
    }

    @Override
    public String scriptDisplayName(String script) {
        String str = this.langData.get("Scripts%stand-alone", script);
        if (str.equals(script)) {
            if (this.nameLength == DisplayContext.LENGTH_SHORT) {
                String str2 = this.langData.get("Scripts%short", script);
                if (!str2.equals(script)) {
                    return adjustForUsageAndContext(CapitalizationContextUsage.SCRIPT, str2);
                }
            }
            str = this.langData.get("Scripts", script);
        }
        return adjustForUsageAndContext(CapitalizationContextUsage.SCRIPT, str);
    }

    @Override
    public String scriptDisplayNameInContext(String script) {
        if (this.nameLength == DisplayContext.LENGTH_SHORT) {
            String scriptName = this.langData.get("Scripts%short", script);
            if (!scriptName.equals(script)) {
                return adjustForUsageAndContext(CapitalizationContextUsage.SCRIPT, scriptName);
            }
        }
        return adjustForUsageAndContext(CapitalizationContextUsage.SCRIPT, this.langData.get("Scripts", script));
    }

    @Override
    public String scriptDisplayName(int scriptCode) {
        return scriptDisplayName(UScript.getShortName(scriptCode));
    }

    @Override
    public String regionDisplayName(String region) {
        if (this.nameLength == DisplayContext.LENGTH_SHORT) {
            String regionName = this.regionData.get("Countries%short", region);
            if (!regionName.equals(region)) {
                return adjustForUsageAndContext(CapitalizationContextUsage.TERRITORY, regionName);
            }
        }
        return adjustForUsageAndContext(CapitalizationContextUsage.TERRITORY, this.regionData.get("Countries", region));
    }

    @Override
    public String variantDisplayName(String variant) {
        return adjustForUsageAndContext(CapitalizationContextUsage.VARIANT, this.langData.get("Variants", variant));
    }

    @Override
    public String keyDisplayName(String key) {
        return adjustForUsageAndContext(CapitalizationContextUsage.KEY, this.langData.get("Keys", key));
    }

    @Override
    public String keyValueDisplayName(String key, String value) {
        String keyValueName = null;
        if (key.equals("currency")) {
            keyValueName = this.currencyDisplayInfo.getName(AsciiUtil.toUpperString(value));
            if (keyValueName == null) {
                keyValueName = value;
            }
        } else {
            if (this.nameLength == DisplayContext.LENGTH_SHORT) {
                String tmp = this.langData.get("Types%short", key, value);
                if (!tmp.equals(value)) {
                    keyValueName = tmp;
                }
            }
            if (keyValueName == null) {
                keyValueName = this.langData.get("Types", key, value);
            }
        }
        return adjustForUsageAndContext(CapitalizationContextUsage.KEYVALUE, keyValueName);
    }

    @Override
    public List<LocaleDisplayNames.UiListItem> getUiListCompareWholeItems(Set<ULocale> localeSet, Comparator<LocaleDisplayNames.UiListItem> comparator) {
        DisplayContext capContext = getContext(DisplayContext.Type.CAPITALIZATION);
        List<LocaleDisplayNames.UiListItem> result = new ArrayList<>();
        Map<ULocale, Set<ULocale>> baseToLocales = new HashMap<>();
        ULocale.Builder builder = new ULocale.Builder();
        for (ULocale locOriginal : localeSet) {
            builder.setLocale(locOriginal);
            ULocale loc = ULocale.addLikelySubtags(locOriginal);
            ULocale base = new ULocale(loc.getLanguage());
            Set<ULocale> locales = baseToLocales.get(base);
            if (locales == null) {
                locales = new HashSet<>();
                baseToLocales.put(base, locales);
            }
            locales.add(loc);
        }
        for (Map.Entry<ULocale, Set<ULocale>> entry : baseToLocales.entrySet()) {
            ULocale base2 = entry.getKey();
            Set<ULocale> values = entry.getValue();
            if (values.size() == 1) {
                ULocale locale = values.iterator().next();
                result.add(newRow(ULocale.minimizeSubtags(locale, ULocale.Minimize.FAVOR_SCRIPT), capContext));
            } else {
                Set<String> scripts = new HashSet<>();
                Set<String> regions = new HashSet<>();
                ULocale maxBase = ULocale.addLikelySubtags(base2);
                scripts.add(maxBase.getScript());
                regions.add(maxBase.getCountry());
                for (ULocale locale2 : values) {
                    scripts.add(locale2.getScript());
                    regions.add(locale2.getCountry());
                }
                boolean hasScripts = scripts.size() > 1;
                boolean hasRegions = regions.size() > 1;
                for (ULocale locale3 : values) {
                    ULocale.Builder modified = builder.setLocale(locale3);
                    if (!hasScripts) {
                        modified.setScript("");
                    }
                    if (!hasRegions) {
                        modified.setRegion("");
                    }
                    result.add(newRow(modified.build(), capContext));
                }
            }
        }
        Collections.sort(result, comparator);
        return result;
    }

    private LocaleDisplayNames.UiListItem newRow(ULocale modified, DisplayContext capContext) {
        ULocale minimized = ULocale.minimizeSubtags(modified, ULocale.Minimize.FAVOR_SCRIPT);
        String tempName = modified.getDisplayName(this.locale);
        boolean titlecase = capContext == DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU;
        String nameInDisplayLocale = titlecase ? UCharacter.toTitleFirst(this.locale, tempName) : tempName;
        String tempName2 = modified.getDisplayName(modified);
        String nameInSelf = capContext == DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU ? UCharacter.toTitleFirst(modified, tempName2) : tempName2;
        return new LocaleDisplayNames.UiListItem(minimized, modified, nameInDisplayLocale, nameInSelf);
    }

    public static class DataTable {
        ULocale getLocale() {
            return ULocale.ROOT;
        }

        String get(String tableName, String code) {
            return get(tableName, null, code);
        }

        String get(String tableName, String subTableName, String code) {
            return code;
        }
    }

    static class ICUDataTable extends DataTable {
        private final ICUResourceBundle bundle;

        public ICUDataTable(String path, ULocale locale) {
            this.bundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(path, locale.getBaseName());
        }

        @Override
        public ULocale getLocale() {
            return this.bundle.getULocale();
        }

        @Override
        public String get(String tableName, String subTableName, String code) {
            return ICUResourceTableAccess.getTableString(this.bundle, tableName, subTableName, code);
        }
    }

    static abstract class DataTables {
        public abstract DataTable get(ULocale uLocale);

        DataTables() {
        }

        public static DataTables load(String className) {
            try {
                return (DataTables) Class.forName(className).newInstance();
            } catch (Throwable th) {
                final DataTable NO_OP = new DataTable();
                return new DataTables() {
                    @Override
                    public DataTable get(ULocale locale) {
                        return NO_OP;
                    }
                };
            }
        }
    }

    static abstract class ICUDataTables extends DataTables {
        private final String path;

        protected ICUDataTables(String path) {
            this.path = path;
        }

        @Override
        public DataTable get(ULocale locale) {
            return new ICUDataTable(this.path, locale);
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

    public enum DataTableType {
        LANG,
        REGION;

        public static DataTableType[] valuesCustom() {
            return values();
        }
    }

    public static boolean haveData(DataTableType type) {
        switch (m31x6c76c532()[type.ordinal()]) {
            case 1:
                return LangDataTables.impl instanceof ICUDataTables;
            case 2:
                return RegionDataTables.impl instanceof ICUDataTables;
            default:
                throw new IllegalArgumentException("unknown type: " + type);
        }
    }

    private StringBuilder appendWithSep(String s, StringBuilder b) {
        if (b.length() == 0) {
            b.append(s);
        } else {
            String combined = this.separatorFormat.format(new String[]{b.toString(), s});
            b.replace(0, b.length(), combined);
        }
        return b;
    }

    private static class Cache {

        private static final int[] f13androidicutextDisplayContext$TypeSwitchesValues = null;
        private LocaleDisplayNames cache;
        private DisplayContext capitalization;
        private LocaleDisplayNames.DialectHandling dialectHandling;
        private ULocale locale;
        private DisplayContext nameLength;

        private static int[] m33getandroidicutextDisplayContext$TypeSwitchesValues() {
            if (f13androidicutextDisplayContext$TypeSwitchesValues != null) {
                return f13androidicutextDisplayContext$TypeSwitchesValues;
            }
            int[] iArr = new int[DisplayContext.Type.valuesCustom().length];
            try {
                iArr[DisplayContext.Type.CAPITALIZATION.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[DisplayContext.Type.DIALECT_HANDLING.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[DisplayContext.Type.DISPLAY_LENGTH.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            f13androidicutextDisplayContext$TypeSwitchesValues = iArr;
            return iArr;
        }

        Cache(Cache cache) {
            this();
        }

        private Cache() {
        }

        public LocaleDisplayNames get(ULocale locale, LocaleDisplayNames.DialectHandling dialectHandling) {
            if (!((dialectHandling == this.dialectHandling && DisplayContext.CAPITALIZATION_NONE == this.capitalization && DisplayContext.LENGTH_FULL == this.nameLength) ? locale.equals(this.locale) : false)) {
                this.locale = locale;
                this.dialectHandling = dialectHandling;
                this.capitalization = DisplayContext.CAPITALIZATION_NONE;
                this.nameLength = DisplayContext.LENGTH_FULL;
                this.cache = new LocaleDisplayNamesImpl(locale, dialectHandling);
            }
            return this.cache;
        }

        public LocaleDisplayNames get(ULocale locale, DisplayContext... contexts) {
            boolean zEquals = false;
            LocaleDisplayNames.DialectHandling dialectHandlingIn = LocaleDisplayNames.DialectHandling.STANDARD_NAMES;
            DisplayContext capitalizationIn = DisplayContext.CAPITALIZATION_NONE;
            DisplayContext nameLengthIn = DisplayContext.LENGTH_FULL;
            for (DisplayContext contextItem : contexts) {
                switch (m33getandroidicutextDisplayContext$TypeSwitchesValues()[contextItem.type().ordinal()]) {
                    case 1:
                        capitalizationIn = contextItem;
                        break;
                    case 2:
                        dialectHandlingIn = contextItem.value() == DisplayContext.STANDARD_NAMES.value() ? LocaleDisplayNames.DialectHandling.STANDARD_NAMES : LocaleDisplayNames.DialectHandling.DIALECT_NAMES;
                        break;
                    case 3:
                        nameLengthIn = contextItem;
                        break;
                }
            }
            if (dialectHandlingIn == this.dialectHandling && capitalizationIn == this.capitalization && nameLengthIn == this.nameLength) {
                zEquals = locale.equals(this.locale);
            }
            if (!zEquals) {
                this.locale = locale;
                this.dialectHandling = dialectHandlingIn;
                this.capitalization = capitalizationIn;
                this.nameLength = nameLengthIn;
                this.cache = new LocaleDisplayNamesImpl(locale, contexts);
            }
            return this.cache;
        }
    }
}
