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

    static final boolean f80assertionsDisabled;
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

    static {
        f80assertionsDisabled = !PluralFormat.class.desiredAssertionStatus();
    }

    public PluralFormat() {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter(this, null);
        init(null, PluralRules.PluralType.CARDINAL, ULocale.getDefault(ULocale.Category.FORMAT), null);
    }

    public PluralFormat(ULocale ulocale) {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter(this, null);
        init(null, PluralRules.PluralType.CARDINAL, ulocale, null);
    }

    public PluralFormat(Locale locale) {
        this(ULocale.forLocale(locale));
    }

    public PluralFormat(PluralRules rules) {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter(this, null);
        init(rules, PluralRules.PluralType.CARDINAL, ULocale.getDefault(ULocale.Category.FORMAT), null);
    }

    public PluralFormat(ULocale ulocale, PluralRules rules) {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter(this, null);
        init(rules, PluralRules.PluralType.CARDINAL, ulocale, null);
    }

    public PluralFormat(Locale locale, PluralRules rules) {
        this(ULocale.forLocale(locale), rules);
    }

    public PluralFormat(ULocale ulocale, PluralRules.PluralType type) {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter(this, null);
        init(null, type, ulocale, null);
    }

    public PluralFormat(Locale locale, PluralRules.PluralType type) {
        this(ULocale.forLocale(locale), type);
    }

    public PluralFormat(String pattern) {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter(this, null);
        init(null, PluralRules.PluralType.CARDINAL, ULocale.getDefault(ULocale.Category.FORMAT), null);
        applyPattern(pattern);
    }

    public PluralFormat(ULocale ulocale, String pattern) {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter(this, null);
        init(null, PluralRules.PluralType.CARDINAL, ulocale, null);
        applyPattern(pattern);
    }

    public PluralFormat(PluralRules rules, String pattern) {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter(this, null);
        init(rules, PluralRules.PluralType.CARDINAL, ULocale.getDefault(ULocale.Category.FORMAT), null);
        applyPattern(pattern);
    }

    public PluralFormat(ULocale ulocale, PluralRules rules, String pattern) {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter(this, null);
        init(rules, PluralRules.PluralType.CARDINAL, ulocale, null);
        applyPattern(pattern);
    }

    public PluralFormat(ULocale ulocale, PluralRules.PluralType type, String pattern) {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter(this, null);
        init(null, type, ulocale, null);
        applyPattern(pattern);
    }

    PluralFormat(ULocale ulocale, PluralRules.PluralType type, String pattern, NumberFormat numberFormat) {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter(this, null);
        init(null, type, ulocale, numberFormat);
        applyPattern(pattern);
    }

    private void init(PluralRules rules, PluralRules.PluralType type, ULocale locale, NumberFormat numberFormat) {
        this.ulocale = locale;
        if (rules == null) {
            rules = PluralRules.forLocale(this.ulocale, type);
        }
        this.pluralRules = rules;
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

    public void applyPattern(String pattern) {
        this.pattern = pattern;
        if (this.msgPattern == null) {
            this.msgPattern = new MessagePattern();
        }
        try {
            this.msgPattern.parsePluralStyle(pattern);
            this.offset = this.msgPattern.getPluralOffset(0);
        } catch (RuntimeException e) {
            resetPattern();
            throw e;
        }
    }

    public String toPattern() {
        return this.pattern;
    }

    static int findSubMessage(MessagePattern pattern, int partIndex, PluralSelector selector, Object context, double number) {
        double offset;
        int partIndex2;
        int count = pattern.countParts();
        MessagePattern.Part part = pattern.getPart(partIndex);
        if (part.getType().hasNumericValue()) {
            offset = pattern.getNumericValue(part);
            partIndex++;
        } else {
            offset = 0.0d;
        }
        String keyword = null;
        boolean haveKeywordMatch = false;
        int msgStart = 0;
        do {
            int partIndex3 = partIndex + 1;
            MessagePattern.Part part2 = pattern.getPart(partIndex);
            MessagePattern.Part.Type type = part2.getType();
            if (type == MessagePattern.Part.Type.ARG_LIMIT) {
                break;
            }
            if (!f80assertionsDisabled) {
                if (!(type == MessagePattern.Part.Type.ARG_SELECTOR)) {
                    throw new AssertionError();
                }
            }
            if (pattern.getPartType(partIndex3).hasNumericValue()) {
                partIndex2 = partIndex3 + 1;
                if (number == pattern.getNumericValue(pattern.getPart(partIndex3))) {
                    return partIndex2;
                }
            } else if (haveKeywordMatch) {
                partIndex2 = partIndex3;
            } else if (pattern.partSubstringMatches(part2, PluralRules.KEYWORD_OTHER)) {
                if (msgStart == 0) {
                    msgStart = partIndex3;
                    if (keyword != null && keyword.equals(PluralRules.KEYWORD_OTHER)) {
                        haveKeywordMatch = true;
                        partIndex2 = partIndex3;
                    }
                }
            } else {
                if (keyword == null) {
                    keyword = selector.select(context, number - offset);
                    if (msgStart != 0 && keyword.equals(PluralRules.KEYWORD_OTHER)) {
                        haveKeywordMatch = true;
                    }
                }
                if (!haveKeywordMatch && pattern.partSubstringMatches(part2, keyword)) {
                    msgStart = partIndex3;
                    haveKeywordMatch = true;
                    partIndex2 = partIndex3;
                }
            }
            partIndex = pattern.getLimitPartIndex(partIndex2) + 1;
        } while (partIndex < count);
        return msgStart;
    }

    private final class PluralSelectorAdapter implements PluralSelector {

        static final boolean f81assertionsDisabled;
        final boolean $assertionsDisabled;

        PluralSelectorAdapter(PluralFormat this$0, PluralSelectorAdapter pluralSelectorAdapter) {
            this();
        }

        static {
            f81assertionsDisabled = !PluralSelectorAdapter.class.desiredAssertionStatus();
        }

        private PluralSelectorAdapter() {
        }

        @Override
        public String select(Object context, double number) {
            PluralRules.FixedDecimal dec = (PluralRules.FixedDecimal) context;
            if (!f81assertionsDisabled) {
                double d = dec.source;
                if (dec.isNegative) {
                    number = -number;
                }
                if (!(d == number)) {
                    throw new AssertionError();
                }
            }
            return PluralFormat.this.pluralRules.select(dec);
        }
    }

    public final String format(double number) {
        return format(Double.valueOf(number), number);
    }

    @Override
    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        if (!(obj instanceof Number)) {
            throw new IllegalArgumentException("'" + obj + "' is not a Number");
        }
        toAppendTo.append(format(obj, obj.doubleValue()));
        return toAppendTo;
    }

    private String format(Number numberObject, double number) {
        String numberString;
        PluralRules.FixedDecimal dec;
        int index;
        if (this.msgPattern == null || this.msgPattern.countParts() == 0) {
            return this.numberFormat.format(numberObject);
        }
        double numberMinusOffset = number - this.offset;
        if (this.offset == 0.0d) {
            numberString = this.numberFormat.format(numberObject);
        } else {
            numberString = this.numberFormat.format(numberMinusOffset);
        }
        if (this.numberFormat instanceof DecimalFormat) {
            dec = ((DecimalFormat) this.numberFormat).getFixedDecimal(numberMinusOffset);
        } else {
            dec = new PluralRules.FixedDecimal(numberMinusOffset);
        }
        int partIndex = findSubMessage(this.msgPattern, 0, this.pluralRulesWrapper, dec, number);
        StringBuilder result = null;
        int prevIndex = this.msgPattern.getPart(partIndex).getLimit();
        while (true) {
            partIndex++;
            MessagePattern.Part part = this.msgPattern.getPart(partIndex);
            MessagePattern.Part.Type type = part.getType();
            index = part.getIndex();
            if (type == MessagePattern.Part.Type.MSG_LIMIT) {
                break;
            }
            if (type == MessagePattern.Part.Type.REPLACE_NUMBER || (type == MessagePattern.Part.Type.SKIP_SYNTAX && this.msgPattern.jdkAposMode())) {
                if (result == null) {
                    result = new StringBuilder();
                }
                result.append((CharSequence) this.pattern, prevIndex, index);
                if (type == MessagePattern.Part.Type.REPLACE_NUMBER) {
                    result.append(numberString);
                }
                prevIndex = part.getLimit();
            } else if (type == MessagePattern.Part.Type.ARG_START) {
                if (result == null) {
                    result = new StringBuilder();
                }
                result.append((CharSequence) this.pattern, prevIndex, index);
                partIndex = this.msgPattern.getLimitPartIndex(partIndex);
                int index2 = this.msgPattern.getPart(partIndex).getLimit();
                MessagePattern.appendReducedApostrophes(this.pattern, index, index2, result);
                prevIndex = index2;
            }
        }
        if (result == null) {
            return this.pattern.substring(prevIndex, index);
        }
        return result.append((CharSequence) this.pattern, prevIndex, index).toString();
    }

    public Number parse(String text, ParsePosition parsePosition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object parseObject(String source, ParsePosition pos) {
        throw new UnsupportedOperationException();
    }

    String parseType(String source, RbnfLenientScanner scanner, FieldPosition pos) {
        int currMatchIndex;
        if (this.msgPattern == null || this.msgPattern.countParts() == 0) {
            pos.setBeginIndex(-1);
            pos.setEndIndex(-1);
            return null;
        }
        int count = this.msgPattern.countParts();
        int startingAt = pos.getBeginIndex();
        if (startingAt < 0) {
            startingAt = 0;
        }
        String keyword = null;
        String matchedWord = null;
        int matchedIndex = -1;
        int partIndex = 0;
        while (partIndex < count) {
            int partIndex2 = partIndex + 1;
            MessagePattern.Part partSelector = this.msgPattern.getPart(partIndex);
            if (partSelector.getType() != MessagePattern.Part.Type.ARG_SELECTOR) {
                partIndex = partIndex2;
            } else {
                partIndex = partIndex2 + 1;
                MessagePattern.Part partStart = this.msgPattern.getPart(partIndex2);
                if (partStart.getType() == MessagePattern.Part.Type.MSG_START) {
                    int partIndex3 = partIndex + 1;
                    MessagePattern.Part partLimit = this.msgPattern.getPart(partIndex);
                    if (partLimit.getType() != MessagePattern.Part.Type.MSG_LIMIT) {
                        partIndex = partIndex3;
                    } else {
                        String currArg = this.pattern.substring(partStart.getLimit(), partLimit.getIndex());
                        if (scanner != null) {
                            int[] scannerMatchResult = scanner.findText(source, currArg, startingAt);
                            currMatchIndex = scannerMatchResult[0];
                        } else {
                            currMatchIndex = source.indexOf(currArg, startingAt);
                        }
                        if (currMatchIndex >= 0 && currMatchIndex >= matchedIndex && (matchedWord == null || currArg.length() > matchedWord.length())) {
                            matchedIndex = currMatchIndex;
                            matchedWord = currArg;
                            keyword = this.pattern.substring(partStart.getLimit(), partLimit.getIndex());
                        }
                        partIndex = partIndex3;
                    }
                }
            }
        }
        if (keyword != null) {
            pos.setBeginIndex(matchedIndex);
            pos.setEndIndex(matchedWord.length() + matchedIndex);
            return keyword;
        }
        pos.setBeginIndex(-1);
        pos.setEndIndex(-1);
        return null;
    }

    @Deprecated
    public void setLocale(ULocale ulocale) {
        if (ulocale == null) {
            ulocale = ULocale.getDefault(ULocale.Category.FORMAT);
        }
        init(null, PluralRules.PluralType.CARDINAL, ulocale, null);
    }

    public void setNumberFormat(NumberFormat format) {
        this.numberFormat = format;
    }

    public boolean equals(Object rhs) {
        if (this == rhs) {
            return true;
        }
        if (rhs == null || getClass() != rhs.getClass()) {
            return false;
        }
        PluralFormat pf = (PluralFormat) rhs;
        if (Utility.objectEquals(this.ulocale, pf.ulocale) && Utility.objectEquals(this.pluralRules, pf.pluralRules) && Utility.objectEquals(this.msgPattern, pf.msgPattern)) {
            return Utility.objectEquals(this.numberFormat, pf.numberFormat);
        }
        return false;
    }

    public boolean equals(PluralFormat rhs) {
        return equals((Object) rhs);
    }

    public int hashCode() {
        return this.pluralRules.hashCode() ^ this.parsedValues.hashCode();
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("locale=").append(this.ulocale);
        buf.append(", rules='").append(this.pluralRules).append("'");
        buf.append(", pattern='").append(this.pattern).append("'");
        buf.append(", format='").append(this.numberFormat).append("'");
        return buf.toString();
    }

    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        in.defaultReadObject();
        this.pluralRulesWrapper = new PluralSelectorAdapter(this, null);
        this.parsedValues = null;
        if (this.pattern == null) {
            return;
        }
        applyPattern(this.pattern);
    }
}
