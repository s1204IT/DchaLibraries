package android.icu.text;

import android.icu.impl.Utility;
import android.icu.text.MessagePattern;
import android.icu.text.PluralRules;
import android.icu.util.ULocale;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Locale;
import java.util.Map;

public class PluralFormat extends UFormat {
    static final boolean $assertionsDisabled = false;
    private static final long serialVersionUID = 1;
    private transient MessagePattern msgPattern;
    private NumberFormat numberFormat;
    private transient double offset;
    private Map<String, String> parsedValues;
    private String pattern;
    private PluralRules pluralRules;
    private transient PluralSelectorAdapter pluralRulesWrapper;
    private ULocale ulocale;

    interface PluralSelector {
        String select(Object obj, double d);
    }

    public PluralFormat() {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter();
        init(null, PluralRules.PluralType.CARDINAL, ULocale.getDefault(ULocale.Category.FORMAT), null);
    }

    public PluralFormat(ULocale uLocale) {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter();
        init(null, PluralRules.PluralType.CARDINAL, uLocale, null);
    }

    public PluralFormat(Locale locale) {
        this(ULocale.forLocale(locale));
    }

    public PluralFormat(PluralRules pluralRules) {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter();
        init(pluralRules, PluralRules.PluralType.CARDINAL, ULocale.getDefault(ULocale.Category.FORMAT), null);
    }

    public PluralFormat(ULocale uLocale, PluralRules pluralRules) {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter();
        init(pluralRules, PluralRules.PluralType.CARDINAL, uLocale, null);
    }

    public PluralFormat(Locale locale, PluralRules pluralRules) {
        this(ULocale.forLocale(locale), pluralRules);
    }

    public PluralFormat(ULocale uLocale, PluralRules.PluralType pluralType) {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter();
        init(null, pluralType, uLocale, null);
    }

    public PluralFormat(Locale locale, PluralRules.PluralType pluralType) {
        this(ULocale.forLocale(locale), pluralType);
    }

    public PluralFormat(String str) {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter();
        init(null, PluralRules.PluralType.CARDINAL, ULocale.getDefault(ULocale.Category.FORMAT), null);
        applyPattern(str);
    }

    public PluralFormat(ULocale uLocale, String str) {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter();
        init(null, PluralRules.PluralType.CARDINAL, uLocale, null);
        applyPattern(str);
    }

    public PluralFormat(PluralRules pluralRules, String str) {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter();
        init(pluralRules, PluralRules.PluralType.CARDINAL, ULocale.getDefault(ULocale.Category.FORMAT), null);
        applyPattern(str);
    }

    public PluralFormat(ULocale uLocale, PluralRules pluralRules, String str) {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter();
        init(pluralRules, PluralRules.PluralType.CARDINAL, uLocale, null);
        applyPattern(str);
    }

    public PluralFormat(ULocale uLocale, PluralRules.PluralType pluralType, String str) {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter();
        init(null, pluralType, uLocale, null);
        applyPattern(str);
    }

    PluralFormat(ULocale uLocale, PluralRules.PluralType pluralType, String str, NumberFormat numberFormat) {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter();
        init(null, pluralType, uLocale, numberFormat);
        applyPattern(str);
    }

    private void init(PluralRules pluralRules, PluralRules.PluralType pluralType, ULocale uLocale, NumberFormat numberFormat) {
        this.ulocale = uLocale;
        if (pluralRules == null) {
            pluralRules = PluralRules.forLocale(this.ulocale, pluralType);
        }
        this.pluralRules = pluralRules;
        resetPattern();
        if (numberFormat == null) {
            numberFormat = NumberFormat.getInstance(this.ulocale);
        }
        this.numberFormat = numberFormat;
    }

    private void resetPattern() {
        this.pattern = null;
        if (this.msgPattern != null) {
            this.msgPattern.clear();
        }
        this.offset = 0.0d;
    }

    public void applyPattern(String str) {
        this.pattern = str;
        if (this.msgPattern == null) {
            this.msgPattern = new MessagePattern();
        }
        try {
            this.msgPattern.parsePluralStyle(str);
            this.offset = this.msgPattern.getPluralOffset(0);
        } catch (RuntimeException e) {
            resetPattern();
            throw e;
        }
    }

    public String toPattern() {
        return this.pattern;
    }

    static int findSubMessage(MessagePattern messagePattern, int i, PluralSelector pluralSelector, Object obj, double d) {
        double numericValue;
        int limitPartIndex;
        int iCountParts = messagePattern.countParts();
        MessagePattern.Part part = messagePattern.getPart(i);
        if (part.getType().hasNumericValue()) {
            numericValue = messagePattern.getNumericValue(part);
            limitPartIndex = i + 1;
        } else {
            numericValue = 0.0d;
            limitPartIndex = i;
        }
        boolean z = false;
        String strSelect = null;
        int i2 = 0;
        do {
            int i3 = limitPartIndex + 1;
            MessagePattern.Part part2 = messagePattern.getPart(limitPartIndex);
            if (part2.getType() == MessagePattern.Part.Type.ARG_LIMIT) {
                break;
            }
            if (messagePattern.getPartType(i3).hasNumericValue()) {
                int i4 = i3 + 1;
                if (d == messagePattern.getNumericValue(messagePattern.getPart(i3))) {
                    return i4;
                }
                i3 = i4;
            } else if (!z) {
                if (messagePattern.partSubstringMatches(part2, PluralRules.KEYWORD_OTHER)) {
                    if (i2 == 0) {
                        if (strSelect == null || !strSelect.equals(PluralRules.KEYWORD_OTHER)) {
                            i2 = i3;
                        } else {
                            i2 = i3;
                            z = true;
                        }
                    }
                } else {
                    if (strSelect == null) {
                        strSelect = pluralSelector.select(obj, d - numericValue);
                        if (i2 != 0 && strSelect.equals(PluralRules.KEYWORD_OTHER)) {
                            z = true;
                        }
                    }
                    if (!z && messagePattern.partSubstringMatches(part2, strSelect)) {
                        i2 = i3;
                        z = true;
                    }
                }
            }
            limitPartIndex = messagePattern.getLimitPartIndex(i3) + 1;
        } while (limitPartIndex < iCountParts);
        return i2;
    }

    private final class PluralSelectorAdapter implements PluralSelector {
        private PluralSelectorAdapter() {
        }

        @Override
        public String select(Object obj, double d) {
            return PluralFormat.this.pluralRules.select((PluralRules.IFixedDecimal) obj);
        }
    }

    public final String format(double d) {
        return format(Double.valueOf(d), d);
    }

    @Override
    public StringBuffer format(Object obj, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        if (!(obj instanceof Number)) {
            throw new IllegalArgumentException("'" + obj + "' is not a Number");
        }
        Number number = (Number) obj;
        stringBuffer.append(format(number, number.doubleValue()));
        return stringBuffer;
    }

    private String format(Number number, double d) {
        String str;
        PluralRules.IFixedDecimal fixedDecimal;
        int index;
        if (this.msgPattern == null || this.msgPattern.countParts() == 0) {
            return this.numberFormat.format(number);
        }
        double d2 = d - this.offset;
        if (this.offset == 0.0d) {
            str = this.numberFormat.format(number);
        } else {
            str = this.numberFormat.format(d2);
        }
        String str2 = str;
        if (this.numberFormat instanceof DecimalFormat) {
            fixedDecimal = ((DecimalFormat) this.numberFormat).getFixedDecimal(d2);
        } else {
            fixedDecimal = new PluralRules.FixedDecimal(d2);
        }
        int iFindSubMessage = findSubMessage(this.msgPattern, 0, this.pluralRulesWrapper, fixedDecimal, d);
        StringBuilder sb = null;
        int limit = this.msgPattern.getPart(iFindSubMessage).getLimit();
        while (true) {
            iFindSubMessage++;
            MessagePattern.Part part = this.msgPattern.getPart(iFindSubMessage);
            MessagePattern.Part.Type type = part.getType();
            index = part.getIndex();
            if (type == MessagePattern.Part.Type.MSG_LIMIT) {
                break;
            }
            if (type == MessagePattern.Part.Type.REPLACE_NUMBER || (type == MessagePattern.Part.Type.SKIP_SYNTAX && this.msgPattern.jdkAposMode())) {
                if (sb == null) {
                    sb = new StringBuilder();
                }
                sb.append((CharSequence) this.pattern, limit, index);
                if (type == MessagePattern.Part.Type.REPLACE_NUMBER) {
                    sb.append(str2);
                }
                limit = part.getLimit();
            } else if (type == MessagePattern.Part.Type.ARG_START) {
                if (sb == null) {
                    sb = new StringBuilder();
                }
                sb.append((CharSequence) this.pattern, limit, index);
                iFindSubMessage = this.msgPattern.getLimitPartIndex(iFindSubMessage);
                limit = this.msgPattern.getPart(iFindSubMessage).getLimit();
                MessagePattern.appendReducedApostrophes(this.pattern, index, limit, sb);
            }
        }
        if (sb == null) {
            return this.pattern.substring(limit, index);
        }
        sb.append((CharSequence) this.pattern, limit, index);
        return sb.toString();
    }

    public Number parse(String str, ParsePosition parsePosition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object parseObject(String str, ParsePosition parsePosition) {
        throw new UnsupportedOperationException();
    }

    String parseType(String str, RbnfLenientScanner rbnfLenientScanner, FieldPosition fieldPosition) {
        if (this.msgPattern == null || this.msgPattern.countParts() == 0) {
            fieldPosition.setBeginIndex(-1);
            fieldPosition.setEndIndex(-1);
            return null;
        }
        int iCountParts = this.msgPattern.countParts();
        int beginIndex = fieldPosition.getBeginIndex();
        char c = 0;
        if (beginIndex < 0) {
            beginIndex = 0;
        }
        int i = 0;
        String strSubstring = null;
        int i2 = -1;
        String str2 = null;
        while (i < iCountParts) {
            int i3 = i + 1;
            if (this.msgPattern.getPart(i).getType() != MessagePattern.Part.Type.ARG_SELECTOR) {
                i = i3;
            } else {
                int i4 = i3 + 1;
                MessagePattern.Part part = this.msgPattern.getPart(i3);
                if (part.getType() != MessagePattern.Part.Type.MSG_START) {
                    i = i4;
                } else {
                    int i5 = i4 + 1;
                    MessagePattern.Part part2 = this.msgPattern.getPart(i4);
                    if (part2.getType() != MessagePattern.Part.Type.MSG_LIMIT) {
                        i = i5;
                    } else {
                        String strSubstring2 = this.pattern.substring(part.getLimit(), part2.getIndex());
                        int iIndexOf = rbnfLenientScanner != null ? rbnfLenientScanner.findText(str, strSubstring2, beginIndex)[c] : str.indexOf(strSubstring2, beginIndex);
                        if (iIndexOf >= 0 && iIndexOf >= i2 && (str2 == null || strSubstring2.length() > str2.length())) {
                            str2 = strSubstring2;
                            i2 = iIndexOf;
                            strSubstring = this.pattern.substring(part.getLimit(), part2.getIndex());
                        }
                        i = i5;
                        c = 0;
                    }
                }
            }
        }
        if (strSubstring != null) {
            fieldPosition.setBeginIndex(i2);
            fieldPosition.setEndIndex(i2 + str2.length());
            return strSubstring;
        }
        fieldPosition.setBeginIndex(-1);
        fieldPosition.setEndIndex(-1);
        return null;
    }

    @Deprecated
    public void setLocale(ULocale uLocale) {
        if (uLocale == null) {
            uLocale = ULocale.getDefault(ULocale.Category.FORMAT);
        }
        init(null, PluralRules.PluralType.CARDINAL, uLocale, null);
    }

    public void setNumberFormat(NumberFormat numberFormat) {
        this.numberFormat = numberFormat;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PluralFormat pluralFormat = (PluralFormat) obj;
        if (Utility.objectEquals(this.ulocale, pluralFormat.ulocale) && Utility.objectEquals(this.pluralRules, pluralFormat.pluralRules) && Utility.objectEquals(this.msgPattern, pluralFormat.msgPattern) && Utility.objectEquals(this.numberFormat, pluralFormat.numberFormat)) {
            return true;
        }
        return false;
    }

    public boolean equals(PluralFormat pluralFormat) {
        return equals((Object) pluralFormat);
    }

    public int hashCode() {
        return this.pluralRules.hashCode() ^ this.parsedValues.hashCode();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("locale=" + this.ulocale);
        sb.append(", rules='" + this.pluralRules + "'");
        sb.append(", pattern='" + this.pattern + "'");
        sb.append(", format='" + this.numberFormat + "'");
        return sb.toString();
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        this.pluralRulesWrapper = new PluralSelectorAdapter();
        this.parsedValues = null;
        if (this.pattern != null) {
            applyPattern(this.pattern);
        }
    }
}
