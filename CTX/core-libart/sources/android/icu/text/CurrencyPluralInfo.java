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
    private static final long serialVersionUID = 1;
    private Map<String, String> pluralCountToCurrencyUnitPattern = null;
    private PluralRules pluralRules = null;
    private ULocale ulocale = null;
    private static final char[] tripleCurrencySign = {164, 164, 164};
    private static final String tripleCurrencyStr = new String(tripleCurrencySign);
    private static final char[] defaultCurrencyPluralPatternChar = {0, '.', '#', '#', ' ', 164, 164, 164};
    private static final String defaultCurrencyPluralPattern = new String(defaultCurrencyPluralPatternChar);

    public CurrencyPluralInfo() {
        initialize(ULocale.getDefault(ULocale.Category.FORMAT));
    }

    public CurrencyPluralInfo(Locale locale) {
        initialize(ULocale.forLocale(locale));
    }

    public CurrencyPluralInfo(ULocale uLocale) {
        initialize(uLocale);
    }

    public static CurrencyPluralInfo getInstance() {
        return new CurrencyPluralInfo();
    }

    public static CurrencyPluralInfo getInstance(Locale locale) {
        return new CurrencyPluralInfo(locale);
    }

    public static CurrencyPluralInfo getInstance(ULocale uLocale) {
        return new CurrencyPluralInfo(uLocale);
    }

    public PluralRules getPluralRules() {
        return this.pluralRules;
    }

    public String getCurrencyPluralPattern(String str) {
        String str2 = this.pluralCountToCurrencyUnitPattern.get(str);
        if (str2 == null) {
            if (!str.equals(PluralRules.KEYWORD_OTHER)) {
                str2 = this.pluralCountToCurrencyUnitPattern.get(PluralRules.KEYWORD_OTHER);
            }
            if (str2 == null) {
                return defaultCurrencyPluralPattern;
            }
            return str2;
        }
        return str2;
    }

    public ULocale getLocale() {
        return this.ulocale;
    }

    public void setPluralRules(String str) {
        this.pluralRules = PluralRules.createRules(str);
    }

    public void setCurrencyPluralPattern(String str, String str2) {
        this.pluralCountToCurrencyUnitPattern.put(str, str2);
    }

    public void setLocale(ULocale uLocale) {
        this.ulocale = uLocale;
        initialize(uLocale);
    }

    public Object clone() {
        try {
            CurrencyPluralInfo currencyPluralInfo = (CurrencyPluralInfo) super.clone();
            currencyPluralInfo.ulocale = (ULocale) this.ulocale.clone();
            currencyPluralInfo.pluralCountToCurrencyUnitPattern = new HashMap();
            for (String str : this.pluralCountToCurrencyUnitPattern.keySet()) {
                currencyPluralInfo.pluralCountToCurrencyUnitPattern.put(str, this.pluralCountToCurrencyUnitPattern.get(str));
            }
            return currencyPluralInfo;
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException(e);
        }
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof CurrencyPluralInfo)) {
            return false;
        }
        CurrencyPluralInfo currencyPluralInfo = (CurrencyPluralInfo) obj;
        return this.pluralRules.equals(currencyPluralInfo.pluralRules) && this.pluralCountToCurrencyUnitPattern.equals(currencyPluralInfo.pluralCountToCurrencyUnitPattern);
    }

    @Deprecated
    public int hashCode() {
        return (this.pluralCountToCurrencyUnitPattern.hashCode() ^ this.pluralRules.hashCode()) ^ this.ulocale.hashCode();
    }

    @Deprecated
    String select(double d) {
        return this.pluralRules.select(d);
    }

    @Deprecated
    String select(PluralRules.FixedDecimal fixedDecimal) {
        return this.pluralRules.select(fixedDecimal);
    }

    Iterator<String> pluralPatternIterator() {
        return this.pluralCountToCurrencyUnitPattern.keySet().iterator();
    }

    private void initialize(ULocale uLocale) {
        this.ulocale = uLocale;
        this.pluralRules = PluralRules.forLocale(uLocale);
        setupCurrencyPluralPattern(uLocale);
    }

    private void setupCurrencyPluralPattern(ULocale uLocale) {
        String strSubstring;
        this.pluralCountToCurrencyUnitPattern = new HashMap();
        String pattern = NumberFormat.getPattern(uLocale, 0);
        int iIndexOf = pattern.indexOf(";");
        if (iIndexOf != -1) {
            strSubstring = pattern.substring(iIndexOf + 1);
            pattern = pattern.substring(0, iIndexOf);
        } else {
            strSubstring = null;
        }
        for (Map.Entry<String, String> entry : CurrencyData.provider.getInstance(uLocale, true).getUnitPatterns().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String strReplace = value.replace("{0}", pattern).replace("{1}", tripleCurrencyStr);
            if (iIndexOf != -1) {
                strReplace = strReplace + ";" + value.replace("{0}", strSubstring).replace("{1}", tripleCurrencyStr);
            }
            this.pluralCountToCurrencyUnitPattern.put(key, strReplace);
        }
    }
}
