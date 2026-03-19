package android.icu.text;

import android.icu.impl.CurrencyData;
import android.icu.text.PluralRules;
import android.icu.util.ICUCloneNotSupportedException;
import android.icu.util.ULocale;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class CurrencyPluralInfo implements Cloneable, Serializable {

    static final boolean f69assertionsDisabled;
    private static final String defaultCurrencyPluralPattern;
    private static final char[] defaultCurrencyPluralPatternChar;
    private static final long serialVersionUID = 1;
    private static final char[] tripleCurrencySign;
    private static final String tripleCurrencyStr;
    private Map<String, String> pluralCountToCurrencyUnitPattern = null;
    private PluralRules pluralRules = null;
    private ULocale ulocale = null;

    public CurrencyPluralInfo() {
        initialize(ULocale.getDefault(ULocale.Category.FORMAT));
    }

    public CurrencyPluralInfo(Locale locale) {
        initialize(ULocale.forLocale(locale));
    }

    public CurrencyPluralInfo(ULocale locale) {
        initialize(locale);
    }

    public static CurrencyPluralInfo getInstance() {
        return new CurrencyPluralInfo();
    }

    public static CurrencyPluralInfo getInstance(Locale locale) {
        return new CurrencyPluralInfo(locale);
    }

    public static CurrencyPluralInfo getInstance(ULocale locale) {
        return new CurrencyPluralInfo(locale);
    }

    public PluralRules getPluralRules() {
        return this.pluralRules;
    }

    public String getCurrencyPluralPattern(String pluralCount) {
        String currencyPluralPattern = this.pluralCountToCurrencyUnitPattern.get(pluralCount);
        if (currencyPluralPattern == null) {
            if (!pluralCount.equals(PluralRules.KEYWORD_OTHER)) {
                currencyPluralPattern = this.pluralCountToCurrencyUnitPattern.get(PluralRules.KEYWORD_OTHER);
            }
            if (currencyPluralPattern == null) {
                return defaultCurrencyPluralPattern;
            }
            return currencyPluralPattern;
        }
        return currencyPluralPattern;
    }

    public ULocale getLocale() {
        return this.ulocale;
    }

    public void setPluralRules(String ruleDescription) {
        this.pluralRules = PluralRules.createRules(ruleDescription);
    }

    public void setCurrencyPluralPattern(String pluralCount, String pattern) {
        this.pluralCountToCurrencyUnitPattern.put(pluralCount, pattern);
    }

    public void setLocale(ULocale loc) {
        this.ulocale = loc;
        initialize(loc);
    }

    public Object clone() {
        try {
            CurrencyPluralInfo other = (CurrencyPluralInfo) super.clone();
            other.ulocale = (ULocale) this.ulocale.clone();
            other.pluralCountToCurrencyUnitPattern = new HashMap();
            for (String pluralCount : this.pluralCountToCurrencyUnitPattern.keySet()) {
                String currencyPattern = this.pluralCountToCurrencyUnitPattern.get(pluralCount);
                other.pluralCountToCurrencyUnitPattern.put(pluralCount, currencyPattern);
            }
            return other;
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException(e);
        }
    }

    public boolean equals(Object obj) {
        if ((obj instanceof CurrencyPluralInfo) && this.pluralRules.equals(obj.pluralRules)) {
            return this.pluralCountToCurrencyUnitPattern.equals(obj.pluralCountToCurrencyUnitPattern);
        }
        return false;
    }

    @Deprecated
    public int hashCode() {
        if (f69assertionsDisabled) {
            return 42;
        }
        throw new AssertionError("hashCode not designed");
    }

    @Deprecated
    String select(double number) {
        return this.pluralRules.select(number);
    }

    @Deprecated
    String select(PluralRules.FixedDecimal numberInfo) {
        return this.pluralRules.select(numberInfo);
    }

    Iterator<String> pluralPatternIterator() {
        return this.pluralCountToCurrencyUnitPattern.keySet().iterator();
    }

    private void initialize(ULocale uloc) {
        this.ulocale = uloc;
        this.pluralRules = PluralRules.forLocale(uloc);
        setupCurrencyPluralPattern(uloc);
    }

    private void setupCurrencyPluralPattern(ULocale uloc) {
        this.pluralCountToCurrencyUnitPattern = new HashMap();
        String numberStylePattern = NumberFormat.getPattern(uloc, 0);
        int separatorIndex = numberStylePattern.indexOf(";");
        String negNumberPattern = null;
        if (separatorIndex != -1) {
            negNumberPattern = numberStylePattern.substring(separatorIndex + 1);
            numberStylePattern = numberStylePattern.substring(0, separatorIndex);
        }
        Map<String, String> map = CurrencyData.provider.getInstance(uloc, true).getUnitPatterns();
        for (Map.Entry<String, String> e : map.entrySet()) {
            String pluralCount = e.getKey();
            String pattern = e.getValue();
            String patternWithNumber = pattern.replace("{0}", numberStylePattern);
            String patternWithCurrencySign = patternWithNumber.replace("{1}", tripleCurrencyStr);
            if (separatorIndex != -1) {
                String negWithNumber = pattern.replace("{0}", negNumberPattern);
                String negWithCurrSign = negWithNumber.replace("{1}", tripleCurrencyStr);
                patternWithCurrencySign = patternWithCurrencySign + ";" + negWithCurrSign;
            }
            this.pluralCountToCurrencyUnitPattern.put(pluralCount, patternWithCurrencySign);
        }
    }

    static {
        f69assertionsDisabled = !CurrencyPluralInfo.class.desiredAssertionStatus();
        tripleCurrencySign = new char[]{164, 164, 164};
        tripleCurrencyStr = new String(tripleCurrencySign);
        defaultCurrencyPluralPatternChar = new char[]{0, '.', '#', '#', ' ', 164, 164, 164};
        defaultCurrencyPluralPattern = new String(defaultCurrencyPluralPatternChar);
    }
}
