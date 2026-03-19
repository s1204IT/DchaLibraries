package android.icu.text;

import android.icu.text.CompactDecimalDataCache;
import android.icu.text.DecimalFormat;
import android.icu.text.PluralRules;
import android.icu.util.Output;
import android.icu.util.ULocale;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.AttributedCharacterIterator;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CompactDecimalFormat extends DecimalFormat {

    private static final int[] f68xc9609b6e = null;
    private static final CompactDecimalDataCache cache = new CompactDecimalDataCache();
    private static final long serialVersionUID = 4716293295276629682L;
    private final long[] divisor;
    private final PluralRules pluralRules;
    private final Map<String, DecimalFormat.Unit> pluralToCurrencyAffixes;
    private final Map<String, DecimalFormat.Unit[]> units;

    private static int[] m105x2869bf4a() {
        if (f68xc9609b6e != null) {
            return f68xc9609b6e;
        }
        int[] iArr = new int[CompactStyle.valuesCustom().length];
        try {
            iArr[CompactStyle.LONG.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[CompactStyle.SHORT.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        f68xc9609b6e = iArr;
        return iArr;
    }

    public enum CompactStyle {
        SHORT,
        LONG;

        public static CompactStyle[] valuesCustom() {
            return values();
        }
    }

    public static CompactDecimalFormat getInstance(ULocale locale, CompactStyle style) {
        return new CompactDecimalFormat(locale, style);
    }

    public static CompactDecimalFormat getInstance(Locale locale, CompactStyle style) {
        return new CompactDecimalFormat(ULocale.forLocale(locale), style);
    }

    CompactDecimalFormat(ULocale locale, CompactStyle style) {
        this.pluralRules = PluralRules.forLocale(locale);
        DecimalFormat format = (DecimalFormat) NumberFormat.getInstance(locale);
        CompactDecimalDataCache.Data data = getData(locale, style);
        this.units = data.units;
        this.divisor = data.divisors;
        this.pluralToCurrencyAffixes = null;
        finishInit(style, format.toPattern(), format.getDecimalFormatSymbols());
    }

    @Deprecated
    public CompactDecimalFormat(String pattern, DecimalFormatSymbols formatSymbols, CompactStyle style, PluralRules pluralRules, long[] divisor, Map<String, String[][]> pluralAffixes, Map<String, String[]> currencyAffixes, Collection<String> debugCreationErrors) {
        this.pluralRules = pluralRules;
        this.units = otherPluralVariant(pluralAffixes, divisor, debugCreationErrors);
        if (!pluralRules.getKeywords().equals(this.units.keySet())) {
            debugCreationErrors.add("Missmatch in pluralCategories, should be: " + pluralRules.getKeywords() + ", was actually " + this.units.keySet());
        }
        this.divisor = (long[]) divisor.clone();
        if (currencyAffixes == null) {
            this.pluralToCurrencyAffixes = null;
        } else {
            this.pluralToCurrencyAffixes = new HashMap();
            for (Map.Entry<String, String[]> s : currencyAffixes.entrySet()) {
                String[] pair = s.getValue();
                this.pluralToCurrencyAffixes.put(s.getKey(), new DecimalFormat.Unit(pair[0], pair[1]));
            }
        }
        finishInit(style, pattern, formatSymbols);
    }

    private void finishInit(CompactStyle style, String pattern, DecimalFormatSymbols formatSymbols) {
        applyPattern(pattern);
        setDecimalFormatSymbols(formatSymbols);
        setMaximumSignificantDigits(2);
        setSignificantDigitsUsed(true);
        if (style == CompactStyle.SHORT) {
            setGroupingUsed(false);
        }
        setCurrency(null);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !super.equals(obj)) {
            return false;
        }
        CompactDecimalFormat other = (CompactDecimalFormat) obj;
        if (!mapsAreEqual(this.units, other.units) || !Arrays.equals(this.divisor, other.divisor)) {
            return false;
        }
        if (this.pluralToCurrencyAffixes == other.pluralToCurrencyAffixes || (this.pluralToCurrencyAffixes != null && this.pluralToCurrencyAffixes.equals(other.pluralToCurrencyAffixes))) {
            return this.pluralRules.equals(other.pluralRules);
        }
        return false;
    }

    private boolean mapsAreEqual(Map<String, DecimalFormat.Unit[]> lhs, Map<String, DecimalFormat.Unit[]> rhs) {
        if (lhs.size() != rhs.size()) {
            return false;
        }
        for (Map.Entry<String, DecimalFormat.Unit[]> entry : lhs.entrySet()) {
            DecimalFormat.Unit[] value = rhs.get(entry.getKey());
            if (value == null || !Arrays.equals(entry.getValue(), value)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
        Output<DecimalFormat.Unit> currencyUnit = new Output<>();
        Amount amount = toAmount(number, currencyUnit);
        if (currencyUnit.value != null) {
            currencyUnit.value.writePrefix(toAppendTo);
        }
        DecimalFormat.Unit unit = amount.getUnit();
        unit.writePrefix(toAppendTo);
        super.format(amount.getQty(), toAppendTo, pos);
        unit.writeSuffix(toAppendTo);
        if (currencyUnit.value != null) {
            currencyUnit.value.writeSuffix(toAppendTo);
        }
        return toAppendTo;
    }

    @Override
    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        if (!(obj instanceof Number)) {
            throw new IllegalArgumentException();
        }
        Amount amount = toAmount(obj.doubleValue(), null);
        return super.formatToCharacterIterator(Double.valueOf(amount.getQty()), amount.getUnit());
    }

    @Override
    public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
        return format(number, toAppendTo, pos);
    }

    @Override
    public StringBuffer format(BigInteger number, StringBuffer toAppendTo, FieldPosition pos) {
        return format(number.doubleValue(), toAppendTo, pos);
    }

    @Override
    public StringBuffer format(BigDecimal number, StringBuffer toAppendTo, FieldPosition pos) {
        return format(number.doubleValue(), toAppendTo, pos);
    }

    @Override
    public StringBuffer format(android.icu.math.BigDecimal number, StringBuffer toAppendTo, FieldPosition pos) {
        return format(number.doubleValue(), toAppendTo, pos);
    }

    @Override
    public Number parse(String text, ParsePosition parsePosition) {
        throw new UnsupportedOperationException();
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        throw new NotSerializableException();
    }

    private void readObject(ObjectInputStream in) throws IOException {
        throw new NotSerializableException();
    }

    private Amount toAmount(double number, Output<DecimalFormat.Unit> currencyUnit) {
        boolean negative = isNumberNegative(number);
        double number2 = adjustNumberAsInFormatting(number);
        int base = number2 <= 1.0d ? 0 : (int) Math.log10(number2);
        if (base >= 15) {
            base = 14;
        }
        double number3 = number2 / this.divisor[base];
        String pluralVariant = getPluralForm(getFixedDecimal(number3, toDigitList(number3)));
        if (this.pluralToCurrencyAffixes != null && currencyUnit != null) {
            currencyUnit.value = this.pluralToCurrencyAffixes.get(pluralVariant);
        }
        if (negative) {
            number3 = -number3;
        }
        return new Amount(number3, CompactDecimalDataCache.getUnit(this.units, pluralVariant, base));
    }

    private void recordError(Collection<String> creationErrors, String errorMessage) {
        if (creationErrors == null) {
            throw new IllegalArgumentException(errorMessage);
        }
        creationErrors.add(errorMessage);
    }

    private Map<String, DecimalFormat.Unit[]> otherPluralVariant(Map<String, String[][]> pluralCategoryToPower10ToAffix, long[] divisor, Collection<String> debugCreationErrors) {
        if (divisor.length < 15) {
            recordError(debugCreationErrors, "Must have at least 15 prefix items.");
        }
        long oldDivisor = 0;
        for (int i = 0; i < divisor.length; i++) {
            int log = (int) Math.log10(divisor[i]);
            if (log > i) {
                recordError(debugCreationErrors, "Divisor[" + i + "] must be less than or equal to 10^" + i + ", but is: " + divisor[i]);
            }
            long roundTrip = (long) Math.pow(10.0d, log);
            if (roundTrip != divisor[i]) {
                recordError(debugCreationErrors, "Divisor[" + i + "] must be a power of 10, but is: " + divisor[i]);
            }
            if (divisor[i] < oldDivisor) {
                recordError(debugCreationErrors, "Bad divisor, the divisor for 10E" + i + "(" + divisor[i] + ") is less than the divisor for the divisor for 10E" + (i - 1) + "(" + oldDivisor + ")");
            }
            oldDivisor = divisor[i];
        }
        Map<String, DecimalFormat.Unit[]> result = new HashMap<>();
        Map<String, Integer> seen = new HashMap<>();
        String[][] defaultPower10ToAffix = pluralCategoryToPower10ToAffix.get(PluralRules.KEYWORD_OTHER);
        for (Map.Entry<String, String[][]> pluralCategoryAndPower10ToAffix : pluralCategoryToPower10ToAffix.entrySet()) {
            String pluralCategory = pluralCategoryAndPower10ToAffix.getKey();
            String[][] power10ToAffix = pluralCategoryAndPower10ToAffix.getValue();
            if (power10ToAffix.length != divisor.length) {
                recordError(debugCreationErrors, "Prefixes & suffixes must be present for all divisors " + pluralCategory);
            }
            DecimalFormat.Unit[] units = new DecimalFormat.Unit[power10ToAffix.length];
            for (int i2 = 0; i2 < power10ToAffix.length; i2++) {
                String[] pair = power10ToAffix[i2];
                if (pair == null) {
                    pair = defaultPower10ToAffix[i2];
                }
                if (pair.length != 2 || pair[0] == null || pair[1] == null) {
                    recordError(debugCreationErrors, "Prefix or suffix is null for " + pluralCategory + ", " + i2 + ", " + Arrays.asList(pair));
                } else {
                    String key = pair[0] + "\uffff" + pair[1] + "\uffff" + (i2 - ((int) Math.log10(divisor[i2])));
                    Integer old = seen.get(key);
                    if (old == null) {
                        seen.put(key, Integer.valueOf(i2));
                    } else if (old.intValue() != i2) {
                        recordError(debugCreationErrors, "Collision between values for " + i2 + " and " + old + " for [prefix/suffix/index-log(divisor)" + key.replace((char) 65535, ';'));
                    }
                    units[i2] = new DecimalFormat.Unit(pair[0], pair[1]);
                }
            }
            result.put(pluralCategory, units);
        }
        return result;
    }

    private String getPluralForm(PluralRules.FixedDecimal fixedDecimal) {
        if (this.pluralRules == null) {
            return PluralRules.KEYWORD_OTHER;
        }
        return this.pluralRules.select(fixedDecimal);
    }

    private CompactDecimalDataCache.Data getData(ULocale locale, CompactStyle style) {
        CompactDecimalDataCache.DataBundle bundle = cache.get(locale);
        switch (m105x2869bf4a()[style.ordinal()]) {
        }
        return bundle.shortData;
    }

    private static class Amount {
        private final double qty;
        private final DecimalFormat.Unit unit;

        public Amount(double qty, DecimalFormat.Unit unit) {
            this.qty = qty;
            this.unit = unit;
        }

        public double getQty() {
            return this.qty;
        }

        public DecimalFormat.Unit getUnit() {
            return this.unit;
        }
    }
}
