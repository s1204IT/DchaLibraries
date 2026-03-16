package java.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Locale;
import libcore.icu.LocaleData;
import libcore.icu.NativeDecimalFormat;

public class DecimalFormat extends NumberFormat {
    private static final Double NEGATIVE_ZERO_DOUBLE = new Double(-0.0d);
    private static final ObjectStreamField[] serialPersistentFields = {new ObjectStreamField("positivePrefix", (Class<?>) String.class), new ObjectStreamField("positiveSuffix", (Class<?>) String.class), new ObjectStreamField("negativePrefix", (Class<?>) String.class), new ObjectStreamField("negativeSuffix", (Class<?>) String.class), new ObjectStreamField("posPrefixPattern", (Class<?>) String.class), new ObjectStreamField("posSuffixPattern", (Class<?>) String.class), new ObjectStreamField("negPrefixPattern", (Class<?>) String.class), new ObjectStreamField("negSuffixPattern", (Class<?>) String.class), new ObjectStreamField("multiplier", Integer.TYPE), new ObjectStreamField("groupingSize", Byte.TYPE), new ObjectStreamField("groupingUsed", Boolean.TYPE), new ObjectStreamField("decimalSeparatorAlwaysShown", Boolean.TYPE), new ObjectStreamField("parseBigDecimal", Boolean.TYPE), new ObjectStreamField("roundingMode", (Class<?>) RoundingMode.class), new ObjectStreamField("symbols", (Class<?>) DecimalFormatSymbols.class), new ObjectStreamField("useExponentialNotation", Boolean.TYPE), new ObjectStreamField("minExponentDigits", Byte.TYPE), new ObjectStreamField("maximumIntegerDigits", Integer.TYPE), new ObjectStreamField("minimumIntegerDigits", Integer.TYPE), new ObjectStreamField("maximumFractionDigits", Integer.TYPE), new ObjectStreamField("minimumFractionDigits", Integer.TYPE), new ObjectStreamField("serialVersionOnStream", Integer.TYPE)};
    private static final long serialVersionUID = 864413376551465018L;
    private transient NativeDecimalFormat ndf;
    private transient RoundingMode roundingMode;
    private transient DecimalFormatSymbols symbols;

    public DecimalFormat() {
        this.roundingMode = RoundingMode.HALF_EVEN;
        Locale locale = Locale.getDefault();
        this.symbols = new DecimalFormatSymbols(locale);
        initNative(LocaleData.get(locale).numberPattern);
    }

    public DecimalFormat(String pattern) {
        this(pattern, Locale.getDefault());
    }

    public DecimalFormat(String pattern, DecimalFormatSymbols value) {
        this.roundingMode = RoundingMode.HALF_EVEN;
        this.symbols = (DecimalFormatSymbols) value.clone();
        initNative(pattern);
    }

    DecimalFormat(String pattern, Locale locale) {
        this.roundingMode = RoundingMode.HALF_EVEN;
        this.symbols = new DecimalFormatSymbols(locale);
        initNative(pattern);
    }

    private void initNative(String pattern) {
        try {
            this.ndf = new NativeDecimalFormat(pattern, this.symbols);
            super.setMaximumFractionDigits(this.ndf.getMaximumFractionDigits());
            super.setMaximumIntegerDigits(this.ndf.getMaximumIntegerDigits());
            super.setMinimumFractionDigits(this.ndf.getMinimumFractionDigits());
            super.setMinimumIntegerDigits(this.ndf.getMinimumIntegerDigits());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(pattern);
        }
    }

    public void applyLocalizedPattern(String pattern) {
        this.ndf.applyLocalizedPattern(pattern);
        updateFieldsFromNative();
    }

    public void applyPattern(String pattern) {
        this.ndf.applyPattern(pattern);
        updateFieldsFromNative();
    }

    private void updateFieldsFromNative() {
        this.maximumIntegerDigits = this.ndf.getMaximumIntegerDigits();
        this.minimumIntegerDigits = this.ndf.getMinimumIntegerDigits();
        this.maximumFractionDigits = this.ndf.getMaximumFractionDigits();
        this.minimumFractionDigits = this.ndf.getMinimumFractionDigits();
    }

    @Override
    public Object clone() {
        DecimalFormat clone = (DecimalFormat) super.clone();
        clone.ndf = (NativeDecimalFormat) this.ndf.clone();
        clone.symbols = (DecimalFormatSymbols) this.symbols.clone();
        return clone;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof DecimalFormat)) {
            return false;
        }
        DecimalFormat other = (DecimalFormat) object;
        if (this.ndf != null ? this.ndf.equals(other.ndf) : other.ndf == null) {
            if (getDecimalFormatSymbols().equals(other.getDecimalFormatSymbols())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public AttributedCharacterIterator formatToCharacterIterator(Object object) {
        if (object == null) {
            throw new NullPointerException("object == null");
        }
        return this.ndf.formatToCharacterIterator(object);
    }

    private void checkBufferAndFieldPosition(StringBuffer buffer, FieldPosition position) {
        if (buffer == null) {
            throw new NullPointerException("buffer == null");
        }
        if (position == null) {
            throw new NullPointerException("position == null");
        }
    }

    @Override
    public StringBuffer format(double value, StringBuffer buffer, FieldPosition position) {
        checkBufferAndFieldPosition(buffer, position);
        buffer.append(this.ndf.formatDouble(value, position));
        return buffer;
    }

    @Override
    public StringBuffer format(long value, StringBuffer buffer, FieldPosition position) {
        checkBufferAndFieldPosition(buffer, position);
        buffer.append(this.ndf.formatLong(value, position));
        return buffer;
    }

    @Override
    public final StringBuffer format(Object number, StringBuffer buffer, FieldPosition position) {
        checkBufferAndFieldPosition(buffer, position);
        if (number instanceof BigInteger) {
            BigInteger bigInteger = (BigInteger) number;
            char[] chars = bigInteger.bitLength() < 64 ? this.ndf.formatLong(bigInteger.longValue(), position) : this.ndf.formatBigInteger(bigInteger, position);
            buffer.append(chars);
            return buffer;
        }
        if (number instanceof BigDecimal) {
            buffer.append(this.ndf.formatBigDecimal((BigDecimal) number, position));
            return buffer;
        }
        return super.format(number, buffer, position);
    }

    public DecimalFormatSymbols getDecimalFormatSymbols() {
        return (DecimalFormatSymbols) this.symbols.clone();
    }

    @Override
    public Currency getCurrency() {
        return this.symbols.getCurrency();
    }

    public int getGroupingSize() {
        return this.ndf.getGroupingSize();
    }

    public String getNegativePrefix() {
        return this.ndf.getNegativePrefix();
    }

    public String getNegativeSuffix() {
        return this.ndf.getNegativeSuffix();
    }

    public String getPositivePrefix() {
        return this.ndf.getPositivePrefix();
    }

    public String getPositiveSuffix() {
        return this.ndf.getPositiveSuffix();
    }

    @Override
    public int hashCode() {
        return getPositivePrefix().hashCode();
    }

    public boolean isDecimalSeparatorAlwaysShown() {
        return this.ndf.isDecimalSeparatorAlwaysShown();
    }

    public boolean isParseBigDecimal() {
        return this.ndf.isParseBigDecimal();
    }

    @Override
    public void setParseIntegerOnly(boolean value) {
        super.setParseIntegerOnly(value);
        this.ndf.setParseIntegerOnly(value);
    }

    @Override
    public boolean isParseIntegerOnly() {
        return this.ndf.isParseIntegerOnly();
    }

    @Override
    public Number parse(String string, ParsePosition position) {
        Number number = this.ndf.parse(string, position);
        if (number == null) {
            return null;
        }
        if (isParseBigDecimal()) {
            if (number instanceof Long) {
                return new BigDecimal(number.longValue());
            }
            if ((number instanceof Double) && !((Double) number).isInfinite() && !((Double) number).isNaN()) {
                return new BigDecimal(number.toString());
            }
            if (number instanceof BigInteger) {
                return new BigDecimal(number.toString());
            }
            return number;
        }
        if ((number instanceof BigDecimal) || (number instanceof BigInteger)) {
            return new Double(number.doubleValue());
        }
        if (isParseIntegerOnly() && number.equals(NEGATIVE_ZERO_DOUBLE)) {
            return 0L;
        }
        return number;
    }

    public void setDecimalFormatSymbols(DecimalFormatSymbols value) {
        if (value != null) {
            this.symbols = (DecimalFormatSymbols) value.clone();
            this.ndf.setDecimalFormatSymbols(this.symbols);
        }
    }

    @Override
    public void setCurrency(Currency currency) {
        Currency instance = Currency.getInstance(currency.getCurrencyCode());
        this.symbols.setCurrency(instance);
        this.ndf.setCurrency(this.symbols.getCurrencySymbol(), currency.getCurrencyCode());
    }

    public void setDecimalSeparatorAlwaysShown(boolean value) {
        this.ndf.setDecimalSeparatorAlwaysShown(value);
    }

    public void setGroupingSize(int value) {
        this.ndf.setGroupingSize(value);
    }

    @Override
    public void setGroupingUsed(boolean value) {
        this.ndf.setGroupingUsed(value);
    }

    @Override
    public boolean isGroupingUsed() {
        return this.ndf.isGroupingUsed();
    }

    @Override
    public void setMaximumFractionDigits(int value) {
        super.setMaximumFractionDigits(value);
        this.ndf.setMaximumFractionDigits(getMaximumFractionDigits());
        setRoundingMode(this.roundingMode);
    }

    @Override
    public void setMaximumIntegerDigits(int value) {
        super.setMaximumIntegerDigits(value);
        this.ndf.setMaximumIntegerDigits(getMaximumIntegerDigits());
    }

    @Override
    public void setMinimumFractionDigits(int value) {
        super.setMinimumFractionDigits(value);
        this.ndf.setMinimumFractionDigits(getMinimumFractionDigits());
    }

    @Override
    public void setMinimumIntegerDigits(int value) {
        super.setMinimumIntegerDigits(value);
        this.ndf.setMinimumIntegerDigits(getMinimumIntegerDigits());
    }

    public int getMultiplier() {
        return this.ndf.getMultiplier();
    }

    public void setMultiplier(int value) {
        this.ndf.setMultiplier(value);
    }

    public void setNegativePrefix(String value) {
        this.ndf.setNegativePrefix(value);
    }

    public void setNegativeSuffix(String value) {
        this.ndf.setNegativeSuffix(value);
    }

    public void setPositivePrefix(String value) {
        this.ndf.setPositivePrefix(value);
    }

    public void setPositiveSuffix(String value) {
        this.ndf.setPositiveSuffix(value);
    }

    public void setParseBigDecimal(boolean newValue) {
        this.ndf.setParseBigDecimal(newValue);
    }

    public String toLocalizedPattern() {
        return this.ndf.toLocalizedPattern();
    }

    public String toPattern() {
        return this.ndf.toPattern();
    }

    private void writeObject(ObjectOutputStream stream) throws IOException, ClassNotFoundException {
        ObjectOutputStream.PutField fields = stream.putFields();
        fields.put("positivePrefix", this.ndf.getPositivePrefix());
        fields.put("positiveSuffix", this.ndf.getPositiveSuffix());
        fields.put("negativePrefix", this.ndf.getNegativePrefix());
        fields.put("negativeSuffix", this.ndf.getNegativeSuffix());
        fields.put("posPrefixPattern", (String) null);
        fields.put("posSuffixPattern", (String) null);
        fields.put("negPrefixPattern", (String) null);
        fields.put("negSuffixPattern", (String) null);
        fields.put("multiplier", this.ndf.getMultiplier());
        fields.put("groupingSize", (byte) this.ndf.getGroupingSize());
        fields.put("groupingUsed", this.ndf.isGroupingUsed());
        fields.put("decimalSeparatorAlwaysShown", this.ndf.isDecimalSeparatorAlwaysShown());
        fields.put("parseBigDecimal", this.ndf.isParseBigDecimal());
        fields.put("roundingMode", this.roundingMode);
        fields.put("symbols", this.symbols);
        fields.put("useExponentialNotation", false);
        fields.put("minExponentDigits", (byte) 0);
        fields.put("maximumIntegerDigits", this.ndf.getMaximumIntegerDigits());
        fields.put("minimumIntegerDigits", this.ndf.getMinimumIntegerDigits());
        fields.put("maximumFractionDigits", this.ndf.getMaximumFractionDigits());
        fields.put("minimumFractionDigits", this.ndf.getMinimumFractionDigits());
        fields.put("serialVersionOnStream", 4);
        stream.writeFields();
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = stream.readFields();
        this.symbols = (DecimalFormatSymbols) fields.get("symbols", (Object) null);
        initNative("");
        this.ndf.setPositivePrefix((String) fields.get("positivePrefix", ""));
        this.ndf.setPositiveSuffix((String) fields.get("positiveSuffix", ""));
        this.ndf.setNegativePrefix((String) fields.get("negativePrefix", "-"));
        this.ndf.setNegativeSuffix((String) fields.get("negativeSuffix", ""));
        this.ndf.setMultiplier(fields.get("multiplier", 1));
        this.ndf.setGroupingSize(fields.get("groupingSize", (byte) 3));
        this.ndf.setGroupingUsed(fields.get("groupingUsed", true));
        this.ndf.setDecimalSeparatorAlwaysShown(fields.get("decimalSeparatorAlwaysShown", false));
        setRoundingMode((RoundingMode) fields.get("roundingMode", RoundingMode.HALF_EVEN));
        int maximumIntegerDigits = fields.get("maximumIntegerDigits", 309);
        int minimumIntegerDigits = fields.get("minimumIntegerDigits", 309);
        int maximumFractionDigits = fields.get("maximumFractionDigits", 340);
        int minimumFractionDigits = fields.get("minimumFractionDigits", 340);
        this.ndf.setMaximumIntegerDigits(maximumIntegerDigits);
        super.setMaximumIntegerDigits(this.ndf.getMaximumIntegerDigits());
        setMinimumIntegerDigits(minimumIntegerDigits);
        setMinimumFractionDigits(minimumFractionDigits);
        setMaximumFractionDigits(maximumFractionDigits);
        setParseBigDecimal(fields.get("parseBigDecimal", false));
        if (fields.get("serialVersionOnStream", 0) < 3) {
            setMaximumIntegerDigits(super.getMaximumIntegerDigits());
            setMinimumIntegerDigits(super.getMinimumIntegerDigits());
            setMaximumFractionDigits(super.getMaximumFractionDigits());
            setMinimumFractionDigits(super.getMinimumFractionDigits());
        }
    }

    @Override
    public RoundingMode getRoundingMode() {
        return this.roundingMode;
    }

    @Override
    public void setRoundingMode(RoundingMode roundingMode) {
        if (roundingMode == null) {
            throw new NullPointerException("roundingMode == null");
        }
        this.roundingMode = roundingMode;
        this.ndf.setRoundingMode(roundingMode, 0.0d);
    }

    public String toString() {
        return this.ndf.toString();
    }
}
