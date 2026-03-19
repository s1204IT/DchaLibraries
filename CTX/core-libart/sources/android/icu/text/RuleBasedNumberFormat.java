package android.icu.text;

import android.icu.impl.ICUData;
import android.icu.impl.ICUDebug;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.PatternProps;
import android.icu.impl.coll.CollationSettings;
import android.icu.lang.UCharacter;
import android.icu.math.BigDecimal;
import android.icu.text.DisplayContext;
import android.icu.text.PluralRules;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import android.icu.util.UResourceBundleIterator;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;

public class RuleBasedNumberFormat extends NumberFormat {
    public static final int DURATION = 3;
    public static final int NUMBERING_SYSTEM = 4;
    public static final int ORDINAL = 2;
    public static final int SPELLOUT = 1;
    static final long serialVersionUID = -7664252765575395068L;
    private transient BreakIterator capitalizationBrkIter;
    private boolean capitalizationForListOrMenu;
    private boolean capitalizationForStandAlone;
    private boolean capitalizationInfoIsSet;
    private transient DecimalFormat decimalFormat;
    private transient DecimalFormatSymbols decimalFormatSymbols;
    private transient NFRule defaultInfinityRule;
    private transient NFRule defaultNaNRule;
    private transient NFRuleSet defaultRuleSet;
    private boolean lenientParse;
    private transient String lenientParseRules;
    private ULocale locale;
    private transient boolean lookedForScanner;
    private transient String postProcessRules;
    private transient RBNFPostProcessor postProcessor;
    private String[] publicRuleSetNames;
    private int roundingMode;
    private Map<String, String[]> ruleSetDisplayNames;
    private transient NFRuleSet[] ruleSets;
    private transient Map<String, NFRuleSet> ruleSetsMap;
    private transient RbnfLenientScannerProvider scannerProvider;
    private static final boolean DEBUG = ICUDebug.enabled("rbnf");
    private static final String[] rulenames = {"SpelloutRules", "OrdinalRules", "DurationRules", "NumberingSystemRules"};
    private static final String[] locnames = {"SpelloutLocalizations", "OrdinalLocalizations", "DurationLocalizations", "NumberingSystemLocalizations"};
    private static final BigDecimal MAX_VALUE = BigDecimal.valueOf(Long.MAX_VALUE);
    private static final BigDecimal MIN_VALUE = BigDecimal.valueOf(Long.MIN_VALUE);

    public RuleBasedNumberFormat(String str) {
        this.ruleSets = null;
        this.ruleSetsMap = null;
        this.defaultRuleSet = null;
        this.locale = null;
        this.roundingMode = 7;
        this.scannerProvider = null;
        this.decimalFormatSymbols = null;
        this.decimalFormat = null;
        this.defaultInfinityRule = null;
        this.defaultNaNRule = null;
        this.lenientParse = false;
        this.capitalizationInfoIsSet = false;
        this.capitalizationForListOrMenu = false;
        this.capitalizationForStandAlone = false;
        this.capitalizationBrkIter = null;
        this.locale = ULocale.getDefault(ULocale.Category.FORMAT);
        init(str, null);
    }

    public RuleBasedNumberFormat(String str, String[][] strArr) {
        this.ruleSets = null;
        this.ruleSetsMap = null;
        this.defaultRuleSet = null;
        this.locale = null;
        this.roundingMode = 7;
        this.scannerProvider = null;
        this.decimalFormatSymbols = null;
        this.decimalFormat = null;
        this.defaultInfinityRule = null;
        this.defaultNaNRule = null;
        this.lenientParse = false;
        this.capitalizationInfoIsSet = false;
        this.capitalizationForListOrMenu = false;
        this.capitalizationForStandAlone = false;
        this.capitalizationBrkIter = null;
        this.locale = ULocale.getDefault(ULocale.Category.FORMAT);
        init(str, strArr);
    }

    public RuleBasedNumberFormat(String str, Locale locale) {
        this(str, ULocale.forLocale(locale));
    }

    public RuleBasedNumberFormat(String str, ULocale uLocale) {
        this.ruleSets = null;
        this.ruleSetsMap = null;
        this.defaultRuleSet = null;
        this.locale = null;
        this.roundingMode = 7;
        this.scannerProvider = null;
        this.decimalFormatSymbols = null;
        this.decimalFormat = null;
        this.defaultInfinityRule = null;
        this.defaultNaNRule = null;
        this.lenientParse = false;
        this.capitalizationInfoIsSet = false;
        this.capitalizationForListOrMenu = false;
        this.capitalizationForStandAlone = false;
        this.capitalizationBrkIter = null;
        this.locale = uLocale;
        init(str, null);
    }

    public RuleBasedNumberFormat(String str, String[][] strArr, ULocale uLocale) {
        this.ruleSets = null;
        this.ruleSetsMap = null;
        this.defaultRuleSet = null;
        this.locale = null;
        this.roundingMode = 7;
        this.scannerProvider = null;
        this.decimalFormatSymbols = null;
        this.decimalFormat = null;
        this.defaultInfinityRule = null;
        this.defaultNaNRule = null;
        this.lenientParse = false;
        this.capitalizationInfoIsSet = false;
        this.capitalizationForListOrMenu = false;
        this.capitalizationForStandAlone = false;
        this.capitalizationBrkIter = null;
        this.locale = uLocale;
        init(str, strArr);
    }

    public RuleBasedNumberFormat(Locale locale, int i) {
        this(ULocale.forLocale(locale), i);
    }

    public RuleBasedNumberFormat(ULocale uLocale, int i) {
        String[][] strArr = null;
        this.ruleSets = null;
        this.ruleSetsMap = null;
        this.defaultRuleSet = null;
        this.locale = null;
        this.roundingMode = 7;
        this.scannerProvider = null;
        this.decimalFormatSymbols = null;
        this.decimalFormat = null;
        this.defaultInfinityRule = null;
        this.defaultNaNRule = null;
        this.lenientParse = false;
        this.capitalizationInfoIsSet = false;
        this.capitalizationForListOrMenu = false;
        this.capitalizationForStandAlone = false;
        this.capitalizationBrkIter = null;
        this.locale = uLocale;
        ICUResourceBundle iCUResourceBundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_RBNF_BASE_NAME, uLocale);
        ULocale uLocale2 = iCUResourceBundle.getULocale();
        setLocale(uLocale2, uLocale2);
        StringBuilder sb = new StringBuilder();
        try {
            UResourceBundleIterator iterator = iCUResourceBundle.getWithFallback("RBNFRules/" + rulenames[i - 1]).getIterator();
            while (iterator.hasNext()) {
                sb.append(iterator.nextString());
            }
        } catch (MissingResourceException e) {
        }
        ICUResourceBundle iCUResourceBundleFindTopLevel = iCUResourceBundle.findTopLevel(locnames[i - 1]);
        if (iCUResourceBundleFindTopLevel != null) {
            strArr = new String[iCUResourceBundleFindTopLevel.getSize()][];
            for (int i2 = 0; i2 < strArr.length; i2++) {
                strArr[i2] = iCUResourceBundleFindTopLevel.get(i2).getStringArray();
            }
        }
        init(sb.toString(), strArr);
    }

    public RuleBasedNumberFormat(int i) {
        this(ULocale.getDefault(ULocale.Category.FORMAT), i);
    }

    @Override
    public Object clone() {
        return super.clone();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RuleBasedNumberFormat)) {
            return false;
        }
        RuleBasedNumberFormat ruleBasedNumberFormat = (RuleBasedNumberFormat) obj;
        if (!this.locale.equals(ruleBasedNumberFormat.locale) || this.lenientParse != ruleBasedNumberFormat.lenientParse || this.ruleSets.length != ruleBasedNumberFormat.ruleSets.length) {
            return false;
        }
        for (int i = 0; i < this.ruleSets.length; i++) {
            if (!this.ruleSets[i].equals(ruleBasedNumberFormat.ruleSets[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    @Deprecated
    public int hashCode() {
        return super.hashCode();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (NFRuleSet nFRuleSet : this.ruleSets) {
            sb.append(nFRuleSet.toString());
        }
        return sb.toString();
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.writeUTF(toString());
        objectOutputStream.writeObject(this.locale);
        objectOutputStream.writeInt(this.roundingMode);
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException {
        ULocale uLocale;
        String utf = objectInputStream.readUTF();
        try {
            uLocale = (ULocale) objectInputStream.readObject();
        } catch (Exception e) {
            uLocale = ULocale.getDefault(ULocale.Category.FORMAT);
        }
        try {
            this.roundingMode = objectInputStream.readInt();
        } catch (Exception e2) {
        }
        RuleBasedNumberFormat ruleBasedNumberFormat = new RuleBasedNumberFormat(utf, uLocale);
        this.ruleSets = ruleBasedNumberFormat.ruleSets;
        this.ruleSetsMap = ruleBasedNumberFormat.ruleSetsMap;
        this.defaultRuleSet = ruleBasedNumberFormat.defaultRuleSet;
        this.publicRuleSetNames = ruleBasedNumberFormat.publicRuleSetNames;
        this.decimalFormatSymbols = ruleBasedNumberFormat.decimalFormatSymbols;
        this.decimalFormat = ruleBasedNumberFormat.decimalFormat;
        this.locale = ruleBasedNumberFormat.locale;
        this.defaultInfinityRule = ruleBasedNumberFormat.defaultInfinityRule;
        this.defaultNaNRule = ruleBasedNumberFormat.defaultNaNRule;
    }

    public String[] getRuleSetNames() {
        return (String[]) this.publicRuleSetNames.clone();
    }

    public ULocale[] getRuleSetDisplayNameLocales() {
        if (this.ruleSetDisplayNames != null) {
            Set<String> setKeySet = this.ruleSetDisplayNames.keySet();
            String[] strArr = (String[]) setKeySet.toArray(new String[setKeySet.size()]);
            Arrays.sort(strArr, String.CASE_INSENSITIVE_ORDER);
            ULocale[] uLocaleArr = new ULocale[strArr.length];
            for (int i = 0; i < strArr.length; i++) {
                uLocaleArr[i] = new ULocale(strArr[i]);
            }
            return uLocaleArr;
        }
        return null;
    }

    private String[] getNameListForLocale(ULocale uLocale) {
        if (uLocale != null && this.ruleSetDisplayNames != null) {
            String[] strArr = {uLocale.getBaseName(), ULocale.getDefault(ULocale.Category.DISPLAY).getBaseName()};
            int length = strArr.length;
            for (int i = 0; i < length; i++) {
                for (String fallback = strArr[i]; fallback.length() > 0; fallback = ULocale.getFallback(fallback)) {
                    String[] strArr2 = this.ruleSetDisplayNames.get(fallback);
                    if (strArr2 != null) {
                        return strArr2;
                    }
                }
            }
            return null;
        }
        return null;
    }

    public String[] getRuleSetDisplayNames(ULocale uLocale) {
        String[] nameListForLocale = getNameListForLocale(uLocale);
        if (nameListForLocale != null) {
            return (String[]) nameListForLocale.clone();
        }
        String[] ruleSetNames = getRuleSetNames();
        for (int i = 0; i < ruleSetNames.length; i++) {
            ruleSetNames[i] = ruleSetNames[i].substring(1);
        }
        return ruleSetNames;
    }

    public String[] getRuleSetDisplayNames() {
        return getRuleSetDisplayNames(ULocale.getDefault(ULocale.Category.DISPLAY));
    }

    public String getRuleSetDisplayName(String str, ULocale uLocale) {
        String[] strArr = this.publicRuleSetNames;
        for (int i = 0; i < strArr.length; i++) {
            if (strArr[i].equals(str)) {
                String[] nameListForLocale = getNameListForLocale(uLocale);
                if (nameListForLocale != null) {
                    return nameListForLocale[i];
                }
                return strArr[i].substring(1);
            }
        }
        throw new IllegalArgumentException("unrecognized rule set name: " + str);
    }

    public String getRuleSetDisplayName(String str) {
        return getRuleSetDisplayName(str, ULocale.getDefault(ULocale.Category.DISPLAY));
    }

    public String format(double d, String str) throws IllegalArgumentException {
        if (str.startsWith("%%")) {
            throw new IllegalArgumentException("Can't use internal rule set");
        }
        return adjustForContext(format(d, findRuleSet(str)));
    }

    public String format(long j, String str) throws IllegalArgumentException {
        if (str.startsWith("%%")) {
            throw new IllegalArgumentException("Can't use internal rule set");
        }
        return adjustForContext(format(j, findRuleSet(str)));
    }

    @Override
    public StringBuffer format(double d, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        if (stringBuffer.length() == 0) {
            stringBuffer.append(adjustForContext(format(d, this.defaultRuleSet)));
        } else {
            stringBuffer.append(format(d, this.defaultRuleSet));
        }
        return stringBuffer;
    }

    @Override
    public StringBuffer format(long j, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        if (stringBuffer.length() == 0) {
            stringBuffer.append(adjustForContext(format(j, this.defaultRuleSet)));
        } else {
            stringBuffer.append(format(j, this.defaultRuleSet));
        }
        return stringBuffer;
    }

    @Override
    public StringBuffer format(BigInteger bigInteger, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        return format(new BigDecimal(bigInteger), stringBuffer, fieldPosition);
    }

    @Override
    public StringBuffer format(java.math.BigDecimal bigDecimal, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        return format(new BigDecimal(bigDecimal), stringBuffer, fieldPosition);
    }

    @Override
    public StringBuffer format(BigDecimal bigDecimal, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        if (MIN_VALUE.compareTo(bigDecimal) > 0 || MAX_VALUE.compareTo(bigDecimal) < 0) {
            return getDecimalFormat().format(bigDecimal, stringBuffer, fieldPosition);
        }
        if (bigDecimal.scale() == 0) {
            return format(bigDecimal.longValue(), stringBuffer, fieldPosition);
        }
        return format(bigDecimal.doubleValue(), stringBuffer, fieldPosition);
    }

    @Override
    public Number parse(String str, ParsePosition parsePosition) {
        String strSubstring = str.substring(parsePosition.getIndex());
        ParsePosition parsePosition2 = new ParsePosition(0);
        Number number = NFRule.ZERO;
        ParsePosition parsePosition3 = new ParsePosition(parsePosition2.getIndex());
        for (int length = this.ruleSets.length - 1; length >= 0; length--) {
            if (this.ruleSets[length].isPublic() && this.ruleSets[length].isParseable()) {
                Number number2 = this.ruleSets[length].parse(strSubstring, parsePosition2, Double.MAX_VALUE);
                if (parsePosition2.getIndex() > parsePosition3.getIndex()) {
                    parsePosition3.setIndex(parsePosition2.getIndex());
                    number = number2;
                }
                if (parsePosition3.getIndex() == strSubstring.length()) {
                    break;
                }
                parsePosition2.setIndex(0);
            }
        }
        parsePosition.setIndex(parsePosition.getIndex() + parsePosition3.getIndex());
        return number;
    }

    public void setLenientParseMode(boolean z) {
        this.lenientParse = z;
    }

    public boolean lenientParseEnabled() {
        return this.lenientParse;
    }

    public void setLenientScannerProvider(RbnfLenientScannerProvider rbnfLenientScannerProvider) {
        this.scannerProvider = rbnfLenientScannerProvider;
    }

    public RbnfLenientScannerProvider getLenientScannerProvider() {
        if (this.scannerProvider == null && this.lenientParse && !this.lookedForScanner) {
            try {
                this.lookedForScanner = true;
                setLenientScannerProvider((RbnfLenientScannerProvider) Class.forName("android.icu.impl.text.RbnfScannerProviderImpl").newInstance());
            } catch (Exception e) {
            }
        }
        return this.scannerProvider;
    }

    public void setDefaultRuleSet(String str) {
        String name;
        if (str == null) {
            if (this.publicRuleSetNames.length > 0) {
                this.defaultRuleSet = findRuleSet(this.publicRuleSetNames[0]);
                return;
            }
            this.defaultRuleSet = null;
            int length = this.ruleSets.length;
            do {
                length--;
                if (length >= 0) {
                    name = this.ruleSets[length].getName();
                    if (name.equals("%spellout-numbering") || name.equals("%digits-ordinal")) {
                        break;
                    }
                } else {
                    int length2 = this.ruleSets.length;
                    do {
                        length2--;
                        if (length2 < 0) {
                            return;
                        }
                    } while (!this.ruleSets[length2].isPublic());
                    this.defaultRuleSet = this.ruleSets[length2];
                    return;
                }
            } while (!name.equals("%duration"));
            this.defaultRuleSet = this.ruleSets[length];
            return;
        }
        if (str.startsWith("%%")) {
            throw new IllegalArgumentException("cannot use private rule set: " + str);
        }
        this.defaultRuleSet = findRuleSet(str);
    }

    public String getDefaultRuleSetName() {
        if (this.defaultRuleSet != null && this.defaultRuleSet.isPublic()) {
            return this.defaultRuleSet.getName();
        }
        return "";
    }

    public void setDecimalFormatSymbols(DecimalFormatSymbols decimalFormatSymbols) {
        if (decimalFormatSymbols != null) {
            this.decimalFormatSymbols = (DecimalFormatSymbols) decimalFormatSymbols.clone();
            if (this.decimalFormat != null) {
                this.decimalFormat.setDecimalFormatSymbols(this.decimalFormatSymbols);
            }
            if (this.defaultInfinityRule != null) {
                this.defaultInfinityRule = null;
                getDefaultInfinityRule();
            }
            if (this.defaultNaNRule != null) {
                this.defaultNaNRule = null;
                getDefaultNaNRule();
            }
            for (NFRuleSet nFRuleSet : this.ruleSets) {
                nFRuleSet.setDecimalFormatSymbols(this.decimalFormatSymbols);
            }
        }
    }

    @Override
    public void setContext(DisplayContext displayContext) {
        super.setContext(displayContext);
        if (!this.capitalizationInfoIsSet && (displayContext == DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU || displayContext == DisplayContext.CAPITALIZATION_FOR_STANDALONE)) {
            initCapitalizationContextInfo(this.locale);
            this.capitalizationInfoIsSet = true;
        }
        if (this.capitalizationBrkIter == null) {
            if (displayContext == DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE || ((displayContext == DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU && this.capitalizationForListOrMenu) || (displayContext == DisplayContext.CAPITALIZATION_FOR_STANDALONE && this.capitalizationForStandAlone))) {
                this.capitalizationBrkIter = BreakIterator.getSentenceInstance(this.locale);
            }
        }
    }

    @Override
    public int getRoundingMode() {
        return this.roundingMode;
    }

    @Override
    public void setRoundingMode(int i) {
        if (i < 0 || i > 7) {
            throw new IllegalArgumentException("Invalid rounding mode: " + i);
        }
        this.roundingMode = i;
    }

    NFRuleSet getDefaultRuleSet() {
        return this.defaultRuleSet;
    }

    RbnfLenientScanner getLenientScanner() {
        RbnfLenientScannerProvider lenientScannerProvider;
        if (this.lenientParse && (lenientScannerProvider = getLenientScannerProvider()) != null) {
            return lenientScannerProvider.get(this.locale, this.lenientParseRules);
        }
        return null;
    }

    DecimalFormatSymbols getDecimalFormatSymbols() {
        if (this.decimalFormatSymbols == null) {
            this.decimalFormatSymbols = new DecimalFormatSymbols(this.locale);
        }
        return this.decimalFormatSymbols;
    }

    DecimalFormat getDecimalFormat() {
        if (this.decimalFormat == null) {
            this.decimalFormat = new DecimalFormat(getPattern(this.locale, 0), getDecimalFormatSymbols());
        }
        return this.decimalFormat;
    }

    PluralFormat createPluralFormat(PluralRules.PluralType pluralType, String str) {
        return new PluralFormat(this.locale, pluralType, str, getDecimalFormat());
    }

    NFRule getDefaultInfinityRule() {
        if (this.defaultInfinityRule == null) {
            this.defaultInfinityRule = new NFRule(this, "Inf: " + getDecimalFormatSymbols().getInfinity());
        }
        return this.defaultInfinityRule;
    }

    NFRule getDefaultNaNRule() {
        if (this.defaultNaNRule == null) {
            this.defaultNaNRule = new NFRule(this, "NaN: " + getDecimalFormatSymbols().getNaN());
        }
        return this.defaultNaNRule;
    }

    private String extractSpecial(StringBuilder sb, String str) {
        int iIndexOf = sb.indexOf(str);
        if (iIndexOf != -1 && (iIndexOf == 0 || sb.charAt(iIndexOf - 1) == ';')) {
            int iIndexOf2 = sb.indexOf(";%", iIndexOf);
            if (iIndexOf2 == -1) {
                iIndexOf2 = sb.length() - 1;
            }
            int length = str.length() + iIndexOf;
            while (length < iIndexOf2 && PatternProps.isWhiteSpace(sb.charAt(length))) {
                length++;
            }
            String strSubstring = sb.substring(length, iIndexOf2);
            sb.delete(iIndexOf, iIndexOf2 + 1);
            return strSubstring;
        }
        return null;
    }

    private void init(String str, String[][] strArr) {
        initLocalizations(strArr);
        StringBuilder sbStripWhitespace = stripWhitespace(str);
        this.lenientParseRules = extractSpecial(sbStripWhitespace, "%%lenient-parse:");
        this.postProcessRules = extractSpecial(sbStripWhitespace, "%%post-process:");
        int i = 0;
        int i2 = 1;
        while (true) {
            int iIndexOf = sbStripWhitespace.indexOf(";%", i);
            if (iIndexOf == -1) {
                break;
            }
            i2++;
            i = iIndexOf + 2;
        }
        this.ruleSets = new NFRuleSet[i2];
        this.ruleSetsMap = new HashMap((i2 * 2) + 1);
        this.defaultRuleSet = null;
        String[] strArr2 = new String[i2];
        int i3 = 0;
        int i4 = 0;
        int i5 = 0;
        while (i3 < this.ruleSets.length) {
            int iIndexOf2 = sbStripWhitespace.indexOf(";%", i4);
            if (iIndexOf2 < 0) {
                iIndexOf2 = sbStripWhitespace.length() - 1;
            }
            int i6 = iIndexOf2 + 1;
            strArr2[i3] = sbStripWhitespace.substring(i4, i6);
            NFRuleSet nFRuleSet = new NFRuleSet(this, strArr2, i3);
            this.ruleSets[i3] = nFRuleSet;
            String name = nFRuleSet.getName();
            this.ruleSetsMap.put(name, nFRuleSet);
            if (!name.startsWith("%%")) {
                i5++;
                if ((this.defaultRuleSet == null && name.equals("%spellout-numbering")) || name.equals("%digits-ordinal") || name.equals("%duration")) {
                    this.defaultRuleSet = nFRuleSet;
                }
            }
            i3++;
            i4 = i6;
        }
        if (this.defaultRuleSet == null) {
            int length = this.ruleSets.length - 1;
            while (true) {
                if (length < 0) {
                    break;
                }
                if (!this.ruleSets[length].getName().startsWith("%%")) {
                    this.defaultRuleSet = this.ruleSets[length];
                    break;
                }
                length--;
            }
        }
        if (this.defaultRuleSet == null) {
            this.defaultRuleSet = this.ruleSets[this.ruleSets.length - 1];
        }
        for (int i7 = 0; i7 < this.ruleSets.length; i7++) {
            this.ruleSets[i7].parseRules(strArr2[i7]);
        }
        String[] strArr3 = new String[i5];
        int i8 = 0;
        for (int length2 = this.ruleSets.length - 1; length2 >= 0; length2--) {
            if (!this.ruleSets[length2].getName().startsWith("%%")) {
                strArr3[i8] = this.ruleSets[length2].getName();
                i8++;
            }
        }
        if (this.publicRuleSetNames != null) {
            int i9 = 0;
            while (i9 < this.publicRuleSetNames.length) {
                String str2 = this.publicRuleSetNames[i9];
                for (String str3 : strArr3) {
                    if (str2.equals(str3)) {
                        break;
                    }
                }
                throw new IllegalArgumentException("did not find public rule set: " + str2);
            }
            this.defaultRuleSet = findRuleSet(this.publicRuleSetNames[0]);
            return;
        }
        this.publicRuleSetNames = strArr3;
    }

    private void initLocalizations(String[][] strArr) {
        if (strArr != null) {
            this.publicRuleSetNames = (String[]) strArr[0].clone();
            HashMap map = new HashMap();
            for (int i = 1; i < strArr.length; i++) {
                String[] strArr2 = strArr[i];
                String str = strArr2[0];
                String[] strArr3 = new String[strArr2.length - 1];
                if (strArr3.length == this.publicRuleSetNames.length) {
                    System.arraycopy(strArr2, 1, strArr3, 0, strArr3.length);
                    map.put(str, strArr3);
                } else {
                    throw new IllegalArgumentException("public name length: " + this.publicRuleSetNames.length + " != localized names[" + i + "] length: " + strArr3.length);
                }
            }
            if (!map.isEmpty()) {
                this.ruleSetDisplayNames = map;
            }
        }
    }

    private void initCapitalizationContextInfo(ULocale uLocale) {
        try {
            int[] intVector = ((ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, uLocale)).getWithFallback("contextTransforms/number-spellout").getIntVector();
            if (intVector.length >= 2) {
                this.capitalizationForListOrMenu = intVector[0] != 0;
                this.capitalizationForStandAlone = intVector[1] != 0;
            }
        } catch (MissingResourceException e) {
        }
    }

    private StringBuilder stripWhitespace(String str) {
        StringBuilder sb = new StringBuilder();
        int length = str.length();
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            }
            while (i < length && PatternProps.isWhiteSpace(str.charAt(i))) {
                i++;
            }
            if (i < length && str.charAt(i) == ';') {
                i++;
            } else {
                int iIndexOf = str.indexOf(59, i);
                if (iIndexOf == -1) {
                    sb.append(str.substring(i));
                    break;
                }
                if (iIndexOf >= length) {
                    break;
                }
                int i2 = iIndexOf + 1;
                sb.append(str.substring(i, i2));
                i = i2;
            }
        }
        return sb;
    }

    private String format(double d, NFRuleSet nFRuleSet) {
        StringBuilder sb = new StringBuilder();
        if (getRoundingMode() != 7 && !Double.isNaN(d) && !Double.isInfinite(d)) {
            d = new BigDecimal(Double.toString(d)).setScale(getMaximumFractionDigits(), this.roundingMode).doubleValue();
        }
        nFRuleSet.format(d, sb, 0, 0);
        postProcess(sb, nFRuleSet);
        return sb.toString();
    }

    private String format(long j, NFRuleSet nFRuleSet) {
        StringBuilder sb = new StringBuilder();
        if (j == Long.MIN_VALUE) {
            sb.append(getDecimalFormat().format(Long.MIN_VALUE));
        } else {
            nFRuleSet.format(j, sb, 0, 0);
        }
        postProcess(sb, nFRuleSet);
        return sb.toString();
    }

    private void postProcess(StringBuilder sb, NFRuleSet nFRuleSet) {
        if (this.postProcessRules != null) {
            if (this.postProcessor == null) {
                int iIndexOf = this.postProcessRules.indexOf(";");
                if (iIndexOf == -1) {
                    iIndexOf = this.postProcessRules.length();
                }
                String strTrim = this.postProcessRules.substring(0, iIndexOf).trim();
                try {
                    this.postProcessor = (RBNFPostProcessor) Class.forName(strTrim).newInstance();
                    this.postProcessor.init(this, this.postProcessRules);
                } catch (Exception e) {
                    if (DEBUG) {
                        System.out.println("could not locate " + strTrim + ", error " + e.getClass().getName() + ", " + e.getMessage());
                    }
                    this.postProcessor = null;
                    this.postProcessRules = null;
                    return;
                }
            }
            this.postProcessor.process(sb, nFRuleSet);
        }
    }

    private String adjustForContext(String str) {
        DisplayContext context = getContext(DisplayContext.Type.CAPITALIZATION);
        if (context != DisplayContext.CAPITALIZATION_NONE && str != null && str.length() > 0 && UCharacter.isLowerCase(str.codePointAt(0)) && (context == DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE || ((context == DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU && this.capitalizationForListOrMenu) || (context == DisplayContext.CAPITALIZATION_FOR_STANDALONE && this.capitalizationForStandAlone)))) {
            if (this.capitalizationBrkIter == null) {
                this.capitalizationBrkIter = BreakIterator.getSentenceInstance(this.locale);
            }
            return UCharacter.toTitleCase(this.locale, str, this.capitalizationBrkIter, CollationSettings.CASE_FIRST_AND_UPPER_MASK);
        }
        return str;
    }

    NFRuleSet findRuleSet(String str) throws IllegalArgumentException {
        NFRuleSet nFRuleSet = this.ruleSetsMap.get(str);
        if (nFRuleSet == null) {
            throw new IllegalArgumentException("No rule set named " + str);
        }
        return nFRuleSet;
    }
}
