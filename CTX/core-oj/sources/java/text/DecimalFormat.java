package java.text;

import android.icu.text.DecimalFormat_ICU58_Android;
import android.icu.text.NumberFormat;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.AttributedCharacterIterator;
import java.text.Format;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import libcore.icu.LocaleData;
import sun.util.locale.LanguageTag;

public class DecimalFormat extends NumberFormat {
    static final int DOUBLE_FRACTION_DIGITS = 340;
    static final int DOUBLE_INTEGER_DIGITS = 309;
    static final int MAXIMUM_FRACTION_DIGITS = Integer.MAX_VALUE;
    static final int MAXIMUM_INTEGER_DIGITS = Integer.MAX_VALUE;
    static final int currentSerialVersion = 4;
    private static final ObjectStreamField[] serialPersistentFields = {new ObjectStreamField("positivePrefix", String.class), new ObjectStreamField("positiveSuffix", String.class), new ObjectStreamField("negativePrefix", String.class), new ObjectStreamField("negativeSuffix", String.class), new ObjectStreamField("posPrefixPattern", String.class), new ObjectStreamField("posSuffixPattern", String.class), new ObjectStreamField("negPrefixPattern", String.class), new ObjectStreamField("negSuffixPattern", String.class), new ObjectStreamField("multiplier", Integer.TYPE), new ObjectStreamField("groupingSize", Byte.TYPE), new ObjectStreamField("groupingUsed", Boolean.TYPE), new ObjectStreamField("decimalSeparatorAlwaysShown", Boolean.TYPE), new ObjectStreamField("parseBigDecimal", Boolean.TYPE), new ObjectStreamField("roundingMode", RoundingMode.class), new ObjectStreamField("symbols", DecimalFormatSymbols.class), new ObjectStreamField("useExponentialNotation", Boolean.TYPE), new ObjectStreamField("minExponentDigits", Byte.TYPE), new ObjectStreamField("maximumIntegerDigits", Integer.TYPE), new ObjectStreamField("minimumIntegerDigits", Integer.TYPE), new ObjectStreamField("maximumFractionDigits", Integer.TYPE), new ObjectStreamField("minimumFractionDigits", Integer.TYPE), new ObjectStreamField("serialVersionOnStream", Integer.TYPE)};
    static final long serialVersionUID = 864413376551465018L;
    private transient DecimalFormat_ICU58_Android icuDecimalFormat;
    private int maximumFractionDigits;
    private int maximumIntegerDigits;
    private int minimumFractionDigits;
    private int minimumIntegerDigits;
    private RoundingMode roundingMode = RoundingMode.HALF_EVEN;
    private DecimalFormatSymbols symbols;

    public DecimalFormat() {
        this.symbols = null;
        Locale locale = Locale.getDefault(Locale.Category.FORMAT);
        String str = LocaleData.get(locale).numberPattern;
        this.symbols = DecimalFormatSymbols.getInstance(locale);
        initPattern(str);
    }

    public DecimalFormat(String str) {
        this.symbols = null;
        this.symbols = DecimalFormatSymbols.getInstance(Locale.getDefault(Locale.Category.FORMAT));
        initPattern(str);
    }

    public DecimalFormat(String str, DecimalFormatSymbols decimalFormatSymbols) {
        this.symbols = null;
        this.symbols = (DecimalFormatSymbols) decimalFormatSymbols.clone();
        initPattern(str);
    }

    private void initPattern(String str) {
        this.icuDecimalFormat = new DecimalFormat_ICU58_Android(str, this.symbols.getIcuDecimalFormatSymbols());
        updateFieldsFromIcu();
    }

    private void updateFieldsFromIcu() {
        if (this.icuDecimalFormat.getMaximumIntegerDigits() == DOUBLE_INTEGER_DIGITS) {
            this.icuDecimalFormat.setMaximumIntegerDigits(2000000000);
        }
        this.maximumIntegerDigits = this.icuDecimalFormat.getMaximumIntegerDigits();
        this.minimumIntegerDigits = this.icuDecimalFormat.getMinimumIntegerDigits();
        this.maximumFractionDigits = this.icuDecimalFormat.getMaximumFractionDigits();
        this.minimumFractionDigits = this.icuDecimalFormat.getMinimumFractionDigits();
    }

    private static FieldPosition getIcuFieldPosition(FieldPosition fieldPosition) {
        NumberFormat.Field field;
        Format.Field fieldAttribute = fieldPosition.getFieldAttribute();
        if (fieldAttribute == null) {
            return fieldPosition;
        }
        if (fieldAttribute == NumberFormat.Field.INTEGER) {
            field = NumberFormat.Field.INTEGER;
        } else if (fieldAttribute == NumberFormat.Field.FRACTION) {
            field = NumberFormat.Field.FRACTION;
        } else if (fieldAttribute == NumberFormat.Field.DECIMAL_SEPARATOR) {
            field = NumberFormat.Field.DECIMAL_SEPARATOR;
        } else if (fieldAttribute == NumberFormat.Field.EXPONENT_SYMBOL) {
            field = NumberFormat.Field.EXPONENT_SYMBOL;
        } else if (fieldAttribute == NumberFormat.Field.EXPONENT_SIGN) {
            field = NumberFormat.Field.EXPONENT_SIGN;
        } else if (fieldAttribute == NumberFormat.Field.EXPONENT) {
            field = NumberFormat.Field.EXPONENT;
        } else if (fieldAttribute == NumberFormat.Field.GROUPING_SEPARATOR) {
            field = NumberFormat.Field.GROUPING_SEPARATOR;
        } else if (fieldAttribute == NumberFormat.Field.CURRENCY) {
            field = NumberFormat.Field.CURRENCY;
        } else if (fieldAttribute == NumberFormat.Field.PERCENT) {
            field = NumberFormat.Field.PERCENT;
        } else if (fieldAttribute == NumberFormat.Field.PERMILLE) {
            field = NumberFormat.Field.PERMILLE;
        } else if (fieldAttribute == NumberFormat.Field.SIGN) {
            field = NumberFormat.Field.SIGN;
        } else {
            throw new IllegalArgumentException("Unexpected field position attribute type.");
        }
        FieldPosition fieldPosition2 = new FieldPosition(field);
        fieldPosition2.setBeginIndex(fieldPosition.getBeginIndex());
        fieldPosition2.setEndIndex(fieldPosition.getEndIndex());
        return fieldPosition2;
    }

    private static NumberFormat.Field toJavaFieldAttribute(AttributedCharacterIterator.Attribute attribute) {
        String name = attribute.getName();
        if (name.equals(NumberFormat.Field.INTEGER.getName())) {
            return NumberFormat.Field.INTEGER;
        }
        if (name.equals(NumberFormat.Field.CURRENCY.getName())) {
            return NumberFormat.Field.CURRENCY;
        }
        if (name.equals(NumberFormat.Field.DECIMAL_SEPARATOR.getName())) {
            return NumberFormat.Field.DECIMAL_SEPARATOR;
        }
        if (name.equals(NumberFormat.Field.EXPONENT.getName())) {
            return NumberFormat.Field.EXPONENT;
        }
        if (name.equals(NumberFormat.Field.EXPONENT_SIGN.getName())) {
            return NumberFormat.Field.EXPONENT_SIGN;
        }
        if (name.equals(NumberFormat.Field.EXPONENT_SYMBOL.getName())) {
            return NumberFormat.Field.EXPONENT_SYMBOL;
        }
        if (name.equals(NumberFormat.Field.FRACTION.getName())) {
            return NumberFormat.Field.FRACTION;
        }
        if (name.equals(NumberFormat.Field.GROUPING_SEPARATOR.getName())) {
            return NumberFormat.Field.GROUPING_SEPARATOR;
        }
        if (name.equals(NumberFormat.Field.SIGN.getName())) {
            return NumberFormat.Field.SIGN;
        }
        if (name.equals(NumberFormat.Field.PERCENT.getName())) {
            return NumberFormat.Field.PERCENT;
        }
        if (name.equals(NumberFormat.Field.PERMILLE.getName())) {
            return NumberFormat.Field.PERMILLE;
        }
        throw new IllegalArgumentException("Unrecognized attribute: " + name);
    }

    @Override
    public final StringBuffer format(Object obj, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        boolean z;
        if ((obj instanceof Long) || (obj instanceof Integer) || (obj instanceof Short) || (obj instanceof Byte) || (obj instanceof AtomicInteger) || (obj instanceof AtomicLong) || (((z = obj instanceof BigInteger)) && ((BigInteger) obj).bitLength() < 64)) {
            return format(((Number) obj).longValue(), stringBuffer, fieldPosition);
        }
        if (obj instanceof BigDecimal) {
            return format((BigDecimal) obj, stringBuffer, fieldPosition);
        }
        if (z) {
            return format((BigInteger) obj, stringBuffer, fieldPosition);
        }
        if (obj instanceof Number) {
            return format(((Number) obj).doubleValue(), stringBuffer, fieldPosition);
        }
        throw new IllegalArgumentException("Cannot format given Object as a Number");
    }

    @Override
    public StringBuffer format(double d, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        FieldPosition icuFieldPosition = getIcuFieldPosition(fieldPosition);
        this.icuDecimalFormat.format(d, stringBuffer, icuFieldPosition);
        fieldPosition.setBeginIndex(icuFieldPosition.getBeginIndex());
        fieldPosition.setEndIndex(icuFieldPosition.getEndIndex());
        return stringBuffer;
    }

    @Override
    public StringBuffer format(long j, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        FieldPosition icuFieldPosition = getIcuFieldPosition(fieldPosition);
        this.icuDecimalFormat.format(j, stringBuffer, icuFieldPosition);
        fieldPosition.setBeginIndex(icuFieldPosition.getBeginIndex());
        fieldPosition.setEndIndex(icuFieldPosition.getEndIndex());
        return stringBuffer;
    }

    private StringBuffer format(BigDecimal bigDecimal, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        FieldPosition icuFieldPosition = getIcuFieldPosition(fieldPosition);
        this.icuDecimalFormat.format(bigDecimal, stringBuffer, fieldPosition);
        fieldPosition.setBeginIndex(icuFieldPosition.getBeginIndex());
        fieldPosition.setEndIndex(icuFieldPosition.getEndIndex());
        return stringBuffer;
    }

    private StringBuffer format(BigInteger bigInteger, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        FieldPosition icuFieldPosition = getIcuFieldPosition(fieldPosition);
        this.icuDecimalFormat.format(bigInteger, stringBuffer, fieldPosition);
        fieldPosition.setBeginIndex(icuFieldPosition.getBeginIndex());
        fieldPosition.setEndIndex(icuFieldPosition.getEndIndex());
        return stringBuffer;
    }

    @Override
    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        if (obj == null) {
            throw new NullPointerException("object == null");
        }
        AttributedCharacterIterator toCharacterIterator = this.icuDecimalFormat.formatToCharacterIterator(obj);
        StringBuilder sb = new StringBuilder(toCharacterIterator.getEndIndex() - toCharacterIterator.getBeginIndex());
        for (int beginIndex = toCharacterIterator.getBeginIndex(); beginIndex < toCharacterIterator.getEndIndex(); beginIndex++) {
            sb.append(toCharacterIterator.current());
            toCharacterIterator.next();
        }
        AttributedString attributedString = new AttributedString(sb.toString());
        for (int beginIndex2 = toCharacterIterator.getBeginIndex(); beginIndex2 < toCharacterIterator.getEndIndex(); beginIndex2++) {
            toCharacterIterator.setIndex(beginIndex2);
            for (AttributedCharacterIterator.Attribute attribute : toCharacterIterator.getAttributes().keySet()) {
                int runStart = toCharacterIterator.getRunStart();
                int runLimit = toCharacterIterator.getRunLimit();
                NumberFormat.Field javaFieldAttribute = toJavaFieldAttribute(attribute);
                attributedString.addAttribute(javaFieldAttribute, javaFieldAttribute, runStart, runLimit);
            }
        }
        return attributedString.getIterator();
    }

    @Override
    public Number parse(String str, ParsePosition parsePosition) {
        Number number;
        if (parsePosition.index < 0 || parsePosition.index >= str.length() || (number = this.icuDecimalFormat.parse(str, parsePosition)) == null) {
            return null;
        }
        if (isParseBigDecimal()) {
            if (number instanceof Long) {
                return new BigDecimal(number.longValue());
            }
            boolean z = number instanceof Double;
            if (z) {
                Double d = (Double) number;
                if (!d.isInfinite() && !d.isNaN()) {
                    return new BigDecimal(number.toString());
                }
            }
            if (z) {
                Double d2 = (Double) number;
                if (d2.isNaN() || d2.isInfinite()) {
                    return number;
                }
            }
            if (number instanceof android.icu.math.BigDecimal) {
                return ((android.icu.math.BigDecimal) number).toBigDecimal();
            }
        }
        if ((number instanceof android.icu.math.BigDecimal) || (number instanceof BigInteger)) {
            return Double.valueOf(number.doubleValue());
        }
        if (isParseIntegerOnly() && number.equals(new Double(-0.0d))) {
            return 0L;
        }
        return number;
    }

    public DecimalFormatSymbols getDecimalFormatSymbols() {
        return DecimalFormatSymbols.fromIcuInstance(this.icuDecimalFormat.getDecimalFormatSymbols());
    }

    public void setDecimalFormatSymbols(DecimalFormatSymbols decimalFormatSymbols) {
        try {
            this.symbols = (DecimalFormatSymbols) decimalFormatSymbols.clone();
            this.icuDecimalFormat.setDecimalFormatSymbols(this.symbols.getIcuDecimalFormatSymbols());
        } catch (Exception e) {
        }
    }

    public String getPositivePrefix() {
        return this.icuDecimalFormat.getPositivePrefix();
    }

    public void setPositivePrefix(String str) {
        this.icuDecimalFormat.setPositivePrefix(str);
    }

    public String getNegativePrefix() {
        return this.icuDecimalFormat.getNegativePrefix();
    }

    public void setNegativePrefix(String str) {
        this.icuDecimalFormat.setNegativePrefix(str);
    }

    public String getPositiveSuffix() {
        return this.icuDecimalFormat.getPositiveSuffix();
    }

    public void setPositiveSuffix(String str) {
        this.icuDecimalFormat.setPositiveSuffix(str);
    }

    public String getNegativeSuffix() {
        return this.icuDecimalFormat.getNegativeSuffix();
    }

    public void setNegativeSuffix(String str) {
        this.icuDecimalFormat.setNegativeSuffix(str);
    }

    public int getMultiplier() {
        return this.icuDecimalFormat.getMultiplier();
    }

    public void setMultiplier(int i) {
        this.icuDecimalFormat.setMultiplier(i);
    }

    @Override
    public void setGroupingUsed(boolean z) {
        this.icuDecimalFormat.setGroupingUsed(z);
    }

    @Override
    public boolean isGroupingUsed() {
        return this.icuDecimalFormat.isGroupingUsed();
    }

    public int getGroupingSize() {
        return this.icuDecimalFormat.getGroupingSize();
    }

    public void setGroupingSize(int i) {
        this.icuDecimalFormat.setGroupingSize(i);
    }

    public boolean isDecimalSeparatorAlwaysShown() {
        return this.icuDecimalFormat.isDecimalSeparatorAlwaysShown();
    }

    public void setDecimalSeparatorAlwaysShown(boolean z) {
        this.icuDecimalFormat.setDecimalSeparatorAlwaysShown(z);
    }

    public boolean isParseBigDecimal() {
        return this.icuDecimalFormat.isParseBigDecimal();
    }

    public void setParseBigDecimal(boolean z) {
        this.icuDecimalFormat.setParseBigDecimal(z);
    }

    @Override
    public boolean isParseIntegerOnly() {
        return this.icuDecimalFormat.isParseIntegerOnly();
    }

    @Override
    public void setParseIntegerOnly(boolean z) {
        super.setParseIntegerOnly(z);
        this.icuDecimalFormat.setParseIntegerOnly(z);
    }

    @Override
    public Object clone() {
        try {
            DecimalFormat decimalFormat = (DecimalFormat) super.clone();
            decimalFormat.icuDecimalFormat = (DecimalFormat_ICU58_Android) this.icuDecimalFormat.clone();
            decimalFormat.symbols = (DecimalFormatSymbols) this.symbols.clone();
            return decimalFormat;
        } catch (Exception e) {
            throw new InternalError();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DecimalFormat)) {
            return false;
        }
        DecimalFormat decimalFormat = (DecimalFormat) obj;
        if (!this.icuDecimalFormat.equals(decimalFormat.icuDecimalFormat) || !compareIcuRoundingIncrement(decimalFormat.icuDecimalFormat)) {
            return false;
        }
        return true;
    }

    private boolean compareIcuRoundingIncrement(DecimalFormat_ICU58_Android decimalFormat_ICU58_Android) {
        BigDecimal roundingIncrement = this.icuDecimalFormat.getRoundingIncrement();
        return roundingIncrement != null ? decimalFormat_ICU58_Android.getRoundingIncrement() != null && roundingIncrement.equals(decimalFormat_ICU58_Android.getRoundingIncrement()) : decimalFormat_ICU58_Android.getRoundingIncrement() == null;
    }

    @Override
    public int hashCode() {
        return (super.hashCode() * 37) + getPositivePrefix().hashCode();
    }

    public String toPattern() {
        return this.icuDecimalFormat.toPattern();
    }

    public String toLocalizedPattern() {
        return this.icuDecimalFormat.toLocalizedPattern();
    }

    public void applyPattern(String str) {
        this.icuDecimalFormat.applyPattern(str);
        updateFieldsFromIcu();
    }

    public void applyLocalizedPattern(String str) {
        this.icuDecimalFormat.applyLocalizedPattern(str);
        updateFieldsFromIcu();
    }

    @Override
    public void setMaximumIntegerDigits(int i) {
        int i2;
        this.maximumIntegerDigits = Math.min(Math.max(0, i), Integer.MAX_VALUE);
        int i3 = this.maximumIntegerDigits;
        int i4 = DOUBLE_INTEGER_DIGITS;
        if (i3 > DOUBLE_INTEGER_DIGITS) {
            i2 = DOUBLE_INTEGER_DIGITS;
        } else {
            i2 = this.maximumIntegerDigits;
        }
        super.setMaximumIntegerDigits(i2);
        if (this.minimumIntegerDigits > this.maximumIntegerDigits) {
            this.minimumIntegerDigits = this.maximumIntegerDigits;
            if (this.minimumIntegerDigits <= DOUBLE_INTEGER_DIGITS) {
                i4 = this.minimumIntegerDigits;
            }
            super.setMinimumIntegerDigits(i4);
        }
        this.icuDecimalFormat.setMaximumIntegerDigits(getMaximumIntegerDigits());
    }

    @Override
    public void setMinimumIntegerDigits(int i) {
        int i2;
        this.minimumIntegerDigits = Math.min(Math.max(0, i), Integer.MAX_VALUE);
        int i3 = this.minimumIntegerDigits;
        int i4 = DOUBLE_INTEGER_DIGITS;
        if (i3 > DOUBLE_INTEGER_DIGITS) {
            i2 = DOUBLE_INTEGER_DIGITS;
        } else {
            i2 = this.minimumIntegerDigits;
        }
        super.setMinimumIntegerDigits(i2);
        if (this.minimumIntegerDigits > this.maximumIntegerDigits) {
            this.maximumIntegerDigits = this.minimumIntegerDigits;
            if (this.maximumIntegerDigits <= DOUBLE_INTEGER_DIGITS) {
                i4 = this.maximumIntegerDigits;
            }
            super.setMaximumIntegerDigits(i4);
        }
        this.icuDecimalFormat.setMinimumIntegerDigits(getMinimumIntegerDigits());
    }

    @Override
    public void setMaximumFractionDigits(int i) {
        int i2;
        this.maximumFractionDigits = Math.min(Math.max(0, i), Integer.MAX_VALUE);
        int i3 = this.maximumFractionDigits;
        int i4 = DOUBLE_FRACTION_DIGITS;
        if (i3 > DOUBLE_FRACTION_DIGITS) {
            i2 = DOUBLE_FRACTION_DIGITS;
        } else {
            i2 = this.maximumFractionDigits;
        }
        super.setMaximumFractionDigits(i2);
        if (this.minimumFractionDigits > this.maximumFractionDigits) {
            this.minimumFractionDigits = this.maximumFractionDigits;
            if (this.minimumFractionDigits <= DOUBLE_FRACTION_DIGITS) {
                i4 = this.minimumFractionDigits;
            }
            super.setMinimumFractionDigits(i4);
        }
        this.icuDecimalFormat.setMaximumFractionDigits(getMaximumFractionDigits());
    }

    @Override
    public void setMinimumFractionDigits(int i) {
        int i2;
        this.minimumFractionDigits = Math.min(Math.max(0, i), Integer.MAX_VALUE);
        int i3 = this.minimumFractionDigits;
        int i4 = DOUBLE_FRACTION_DIGITS;
        if (i3 > DOUBLE_FRACTION_DIGITS) {
            i2 = DOUBLE_FRACTION_DIGITS;
        } else {
            i2 = this.minimumFractionDigits;
        }
        super.setMinimumFractionDigits(i2);
        if (this.minimumFractionDigits > this.maximumFractionDigits) {
            this.maximumFractionDigits = this.minimumFractionDigits;
            if (this.maximumFractionDigits <= DOUBLE_FRACTION_DIGITS) {
                i4 = this.maximumFractionDigits;
            }
            super.setMaximumFractionDigits(i4);
        }
        this.icuDecimalFormat.setMinimumFractionDigits(getMinimumFractionDigits());
    }

    @Override
    public int getMaximumIntegerDigits() {
        return this.maximumIntegerDigits;
    }

    @Override
    public int getMinimumIntegerDigits() {
        return this.minimumIntegerDigits;
    }

    @Override
    public int getMaximumFractionDigits() {
        return this.maximumFractionDigits;
    }

    @Override
    public int getMinimumFractionDigits() {
        return this.minimumFractionDigits;
    }

    @Override
    public Currency getCurrency() {
        return this.symbols.getCurrency();
    }

    @Override
    public void setCurrency(Currency currency) {
        if (currency != this.symbols.getCurrency() || !currency.getSymbol().equals(this.symbols.getCurrencySymbol())) {
            this.symbols.setCurrency(currency);
            this.icuDecimalFormat.setDecimalFormatSymbols(this.symbols.getIcuDecimalFormatSymbols());
            this.icuDecimalFormat.setMinimumFractionDigits(this.minimumFractionDigits);
            this.icuDecimalFormat.setMaximumFractionDigits(this.maximumFractionDigits);
        }
    }

    @Override
    public RoundingMode getRoundingMode() {
        return this.roundingMode;
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$java$math$RoundingMode = new int[RoundingMode.values().length];

        static {
            try {
                $SwitchMap$java$math$RoundingMode[RoundingMode.UP.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$java$math$RoundingMode[RoundingMode.DOWN.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$java$math$RoundingMode[RoundingMode.CEILING.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$java$math$RoundingMode[RoundingMode.FLOOR.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$java$math$RoundingMode[RoundingMode.HALF_UP.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$java$math$RoundingMode[RoundingMode.HALF_DOWN.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$java$math$RoundingMode[RoundingMode.HALF_EVEN.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$java$math$RoundingMode[RoundingMode.UNNECESSARY.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
        }
    }

    private static int convertRoundingMode(RoundingMode roundingMode) {
        switch (AnonymousClass1.$SwitchMap$java$math$RoundingMode[roundingMode.ordinal()]) {
            case 1:
                return 0;
            case 2:
                return 1;
            case 3:
                return 2;
            case 4:
                return 3;
            case 5:
                return 4;
            case 6:
                return 5;
            case 7:
                return 6;
            case 8:
                return 7;
            default:
                throw new IllegalArgumentException("Invalid rounding mode specified");
        }
    }

    @Override
    public void setRoundingMode(RoundingMode roundingMode) {
        if (roundingMode == null) {
            throw new NullPointerException();
        }
        this.roundingMode = roundingMode;
        this.icuDecimalFormat.setRoundingMode(convertRoundingMode(roundingMode));
    }

    void adjustForCurrencyDefaultFractionDigits() {
        int defaultFractionDigits;
        Currency currency = this.symbols.getCurrency();
        if (currency == null) {
            try {
                currency = Currency.getInstance(this.symbols.getInternationalCurrencySymbol());
            } catch (IllegalArgumentException e) {
            }
        }
        if (currency != null && (defaultFractionDigits = currency.getDefaultFractionDigits()) != -1) {
            int minimumFractionDigits = getMinimumFractionDigits();
            if (minimumFractionDigits == getMaximumFractionDigits()) {
                setMinimumFractionDigits(defaultFractionDigits);
                setMaximumFractionDigits(defaultFractionDigits);
            } else {
                setMinimumFractionDigits(Math.min(defaultFractionDigits, minimumFractionDigits));
                setMaximumFractionDigits(defaultFractionDigits);
            }
        }
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException, ClassNotFoundException {
        ObjectOutputStream.PutField putFieldPutFields = objectOutputStream.putFields();
        putFieldPutFields.put("positivePrefix", this.icuDecimalFormat.getPositivePrefix());
        putFieldPutFields.put("positiveSuffix", this.icuDecimalFormat.getPositiveSuffix());
        putFieldPutFields.put("negativePrefix", this.icuDecimalFormat.getNegativePrefix());
        putFieldPutFields.put("negativeSuffix", this.icuDecimalFormat.getNegativeSuffix());
        String str = (String) null;
        putFieldPutFields.put("posPrefixPattern", str);
        putFieldPutFields.put("posSuffixPattern", str);
        putFieldPutFields.put("negPrefixPattern", str);
        putFieldPutFields.put("negSuffixPattern", str);
        putFieldPutFields.put("multiplier", this.icuDecimalFormat.getMultiplier());
        putFieldPutFields.put("groupingSize", (byte) this.icuDecimalFormat.getGroupingSize());
        putFieldPutFields.put("groupingUsed", this.icuDecimalFormat.isGroupingUsed());
        putFieldPutFields.put("decimalSeparatorAlwaysShown", this.icuDecimalFormat.isDecimalSeparatorAlwaysShown());
        putFieldPutFields.put("parseBigDecimal", this.icuDecimalFormat.isParseBigDecimal());
        putFieldPutFields.put("roundingMode", this.roundingMode);
        putFieldPutFields.put("symbols", this.symbols);
        putFieldPutFields.put("useExponentialNotation", false);
        putFieldPutFields.put("minExponentDigits", (byte) 0);
        putFieldPutFields.put("maximumIntegerDigits", this.icuDecimalFormat.getMaximumIntegerDigits());
        putFieldPutFields.put("minimumIntegerDigits", this.icuDecimalFormat.getMinimumIntegerDigits());
        putFieldPutFields.put("maximumFractionDigits", this.icuDecimalFormat.getMaximumFractionDigits());
        putFieldPutFields.put("minimumFractionDigits", this.icuDecimalFormat.getMinimumFractionDigits());
        putFieldPutFields.put("serialVersionOnStream", 4);
        objectOutputStream.writeFields();
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = objectInputStream.readFields();
        this.symbols = (DecimalFormatSymbols) fields.get("symbols", (Object) null);
        initPattern("#");
        String str = (String) fields.get("positivePrefix", "");
        if (!Objects.equals(str, this.icuDecimalFormat.getPositivePrefix())) {
            this.icuDecimalFormat.setPositivePrefix(str);
        }
        String str2 = (String) fields.get("positiveSuffix", "");
        if (!Objects.equals(str2, this.icuDecimalFormat.getPositiveSuffix())) {
            this.icuDecimalFormat.setPositiveSuffix(str2);
        }
        String str3 = (String) fields.get("negativePrefix", LanguageTag.SEP);
        if (!Objects.equals(str3, this.icuDecimalFormat.getNegativePrefix())) {
            this.icuDecimalFormat.setNegativePrefix(str3);
        }
        String str4 = (String) fields.get("negativeSuffix", "");
        if (!Objects.equals(str4, this.icuDecimalFormat.getNegativeSuffix())) {
            this.icuDecimalFormat.setNegativeSuffix(str4);
        }
        int i = fields.get("multiplier", 1);
        if (i != this.icuDecimalFormat.getMultiplier()) {
            this.icuDecimalFormat.setMultiplier(i);
        }
        boolean z = fields.get("groupingUsed", true);
        if (z != this.icuDecimalFormat.isGroupingUsed()) {
            this.icuDecimalFormat.setGroupingUsed(z);
        }
        byte b = fields.get("groupingSize", (byte) 3);
        if (b != this.icuDecimalFormat.getGroupingSize()) {
            this.icuDecimalFormat.setGroupingSize(b);
        }
        boolean z2 = fields.get("decimalSeparatorAlwaysShown", false);
        if (z2 != this.icuDecimalFormat.isDecimalSeparatorAlwaysShown()) {
            this.icuDecimalFormat.setDecimalSeparatorAlwaysShown(z2);
        }
        RoundingMode roundingMode = (RoundingMode) fields.get("roundingMode", RoundingMode.HALF_EVEN);
        if (convertRoundingMode(roundingMode) != this.icuDecimalFormat.getRoundingMode()) {
            setRoundingMode(roundingMode);
        }
        int i2 = fields.get("maximumIntegerDigits", DOUBLE_INTEGER_DIGITS);
        if (i2 != this.icuDecimalFormat.getMaximumIntegerDigits()) {
            this.icuDecimalFormat.setMaximumIntegerDigits(i2);
        }
        int i3 = fields.get("minimumIntegerDigits", DOUBLE_INTEGER_DIGITS);
        if (i3 != this.icuDecimalFormat.getMinimumIntegerDigits()) {
            this.icuDecimalFormat.setMinimumIntegerDigits(i3);
        }
        int i4 = fields.get("maximumFractionDigits", DOUBLE_FRACTION_DIGITS);
        if (i4 != this.icuDecimalFormat.getMaximumFractionDigits()) {
            this.icuDecimalFormat.setMaximumFractionDigits(i4);
        }
        int i5 = fields.get("minimumFractionDigits", DOUBLE_FRACTION_DIGITS);
        if (i5 != this.icuDecimalFormat.getMinimumFractionDigits()) {
            this.icuDecimalFormat.setMinimumFractionDigits(i5);
        }
        boolean z3 = fields.get("parseBigDecimal", true);
        if (z3 != this.icuDecimalFormat.isParseBigDecimal()) {
            this.icuDecimalFormat.setParseBigDecimal(z3);
        }
        updateFieldsFromIcu();
        if (fields.get("serialVersionOnStream", 0) < 3) {
            setMaximumIntegerDigits(super.getMaximumIntegerDigits());
            setMinimumIntegerDigits(super.getMinimumIntegerDigits());
            setMaximumFractionDigits(super.getMaximumFractionDigits());
            setMinimumFractionDigits(super.getMinimumFractionDigits());
        }
    }
}
