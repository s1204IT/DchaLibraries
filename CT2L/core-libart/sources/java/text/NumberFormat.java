package java.text;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.Format;
import java.util.Currency;
import java.util.Locale;
import libcore.icu.ICU;
import libcore.icu.LocaleData;

public abstract class NumberFormat extends Format {
    public static final int FRACTION_FIELD = 1;
    public static final int INTEGER_FIELD = 0;
    private static final ObjectStreamField[] serialPersistentFields = {new ObjectStreamField("groupingUsed", Boolean.TYPE), new ObjectStreamField("maxFractionDigits", Byte.TYPE), new ObjectStreamField("maximumFractionDigits", Integer.TYPE), new ObjectStreamField("maximumIntegerDigits", Integer.TYPE), new ObjectStreamField("maxIntegerDigits", Byte.TYPE), new ObjectStreamField("minFractionDigits", Byte.TYPE), new ObjectStreamField("minimumFractionDigits", Integer.TYPE), new ObjectStreamField("minimumIntegerDigits", Integer.TYPE), new ObjectStreamField("minIntegerDigits", Byte.TYPE), new ObjectStreamField("parseIntegerOnly", Boolean.TYPE), new ObjectStreamField("serialVersionOnStream", Integer.TYPE)};
    private static final long serialVersionUID = -2308460125733713944L;
    private boolean groupingUsed = true;
    private boolean parseIntegerOnly = false;
    int maximumIntegerDigits = 40;
    int minimumIntegerDigits = 1;
    int maximumFractionDigits = 3;
    int minimumFractionDigits = 0;

    public abstract StringBuffer format(double d, StringBuffer stringBuffer, FieldPosition fieldPosition);

    public abstract StringBuffer format(long j, StringBuffer stringBuffer, FieldPosition fieldPosition);

    public abstract Number parse(String str, ParsePosition parsePosition);

    protected NumberFormat() {
    }

    @Override
    public Object clone() {
        return super.clone();
    }

    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof NumberFormat)) {
            return false;
        }
        NumberFormat obj = (NumberFormat) object;
        return this.groupingUsed == obj.groupingUsed && this.parseIntegerOnly == obj.parseIntegerOnly && this.maximumFractionDigits == obj.maximumFractionDigits && this.maximumIntegerDigits == obj.maximumIntegerDigits && this.minimumFractionDigits == obj.minimumFractionDigits && this.minimumIntegerDigits == obj.minimumIntegerDigits;
    }

    public final String format(double value) {
        return format(value, new StringBuffer(), new FieldPosition(0)).toString();
    }

    public final String format(long value) {
        return format(value, new StringBuffer(), new FieldPosition(0)).toString();
    }

    @Override
    public StringBuffer format(Object object, StringBuffer buffer, FieldPosition field) {
        if ((object instanceof Byte) || (object instanceof Short) || (object instanceof Integer) || (object instanceof Long) || ((object instanceof BigInteger) && ((BigInteger) object).bitLength() < 64)) {
            long lv = ((Number) object).longValue();
            return format(lv, buffer, field);
        }
        if (object instanceof Number) {
            double dv = ((Number) object).doubleValue();
            return format(dv, buffer, field);
        }
        if (object == null) {
            throw new IllegalArgumentException("Can't format null object");
        }
        throw new IllegalArgumentException("Bad class: " + object.getClass());
    }

    public static Locale[] getAvailableLocales() {
        return ICU.getAvailableNumberFormatLocales();
    }

    public Currency getCurrency() {
        throw new UnsupportedOperationException();
    }

    public static final NumberFormat getCurrencyInstance() {
        return getCurrencyInstance(Locale.getDefault());
    }

    public static NumberFormat getCurrencyInstance(Locale locale) {
        if (locale == null) {
            throw new NullPointerException("locale == null");
        }
        return getInstance(LocaleData.get(locale).currencyPattern, locale);
    }

    public static final NumberFormat getIntegerInstance() {
        return getIntegerInstance(Locale.getDefault());
    }

    public static NumberFormat getIntegerInstance(Locale locale) {
        if (locale == null) {
            throw new NullPointerException("locale == null");
        }
        NumberFormat result = getInstance(LocaleData.get(locale).integerPattern, locale);
        result.setParseIntegerOnly(true);
        return result;
    }

    public static final NumberFormat getInstance() {
        return getNumberInstance();
    }

    public static NumberFormat getInstance(Locale locale) {
        return getNumberInstance(locale);
    }

    private static NumberFormat getInstance(String pattern, Locale locale) {
        return new DecimalFormat(pattern, locale);
    }

    public int getMaximumFractionDigits() {
        return this.maximumFractionDigits;
    }

    public int getMaximumIntegerDigits() {
        return this.maximumIntegerDigits;
    }

    public int getMinimumFractionDigits() {
        return this.minimumFractionDigits;
    }

    public int getMinimumIntegerDigits() {
        return this.minimumIntegerDigits;
    }

    public static final NumberFormat getNumberInstance() {
        return getNumberInstance(Locale.getDefault());
    }

    public static NumberFormat getNumberInstance(Locale locale) {
        if (locale == null) {
            throw new NullPointerException("locale == null");
        }
        return getInstance(LocaleData.get(locale).numberPattern, locale);
    }

    public static final NumberFormat getPercentInstance() {
        return getPercentInstance(Locale.getDefault());
    }

    public static NumberFormat getPercentInstance(Locale locale) {
        if (locale == null) {
            throw new NullPointerException("locale == null");
        }
        return getInstance(LocaleData.get(locale).percentPattern, locale);
    }

    public int hashCode() {
        return (this.groupingUsed ? 1231 : 1237) + (this.parseIntegerOnly ? 1231 : 1237) + this.maximumFractionDigits + this.maximumIntegerDigits + this.minimumFractionDigits + this.minimumIntegerDigits;
    }

    public boolean isGroupingUsed() {
        return this.groupingUsed;
    }

    public boolean isParseIntegerOnly() {
        return this.parseIntegerOnly;
    }

    public Number parse(String string) throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        Number number = parse(string, pos);
        if (pos.getIndex() == 0) {
            throw new ParseException("Unparseable number: \"" + string + "\"", pos.getErrorIndex());
        }
        return number;
    }

    @Override
    public final Object parseObject(String string, ParsePosition position) {
        if (position == null) {
            throw new NullPointerException("position == null");
        }
        try {
            return parse(string, position);
        } catch (Exception e) {
            return null;
        }
    }

    public void setCurrency(Currency currency) {
        throw new UnsupportedOperationException();
    }

    public void setGroupingUsed(boolean value) {
        this.groupingUsed = value;
    }

    public void setMaximumFractionDigits(int value) {
        if (value < 0) {
            value = 0;
        }
        this.maximumFractionDigits = value;
        if (this.maximumFractionDigits < this.minimumFractionDigits) {
            this.minimumFractionDigits = this.maximumFractionDigits;
        }
    }

    public void setMaximumIntegerDigits(int value) {
        if (value < 0) {
            value = 0;
        }
        this.maximumIntegerDigits = value;
        if (this.maximumIntegerDigits < this.minimumIntegerDigits) {
            this.minimumIntegerDigits = this.maximumIntegerDigits;
        }
    }

    public void setMinimumFractionDigits(int value) {
        if (value < 0) {
            value = 0;
        }
        this.minimumFractionDigits = value;
        if (this.maximumFractionDigits < this.minimumFractionDigits) {
            this.maximumFractionDigits = this.minimumFractionDigits;
        }
    }

    public void setMinimumIntegerDigits(int value) {
        if (value < 0) {
            value = 0;
        }
        this.minimumIntegerDigits = value;
        if (this.maximumIntegerDigits < this.minimumIntegerDigits) {
            this.maximumIntegerDigits = this.minimumIntegerDigits;
        }
    }

    public void setParseIntegerOnly(boolean value) {
        this.parseIntegerOnly = value;
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        byte b = Byte.MAX_VALUE;
        ObjectOutputStream.PutField fields = stream.putFields();
        fields.put("groupingUsed", this.groupingUsed);
        fields.put("maxFractionDigits", this.maximumFractionDigits < 127 ? (byte) this.maximumFractionDigits : (byte) 127);
        fields.put("maximumFractionDigits", this.maximumFractionDigits);
        fields.put("maximumIntegerDigits", this.maximumIntegerDigits);
        fields.put("maxIntegerDigits", this.maximumIntegerDigits < 127 ? (byte) this.maximumIntegerDigits : (byte) 127);
        fields.put("minFractionDigits", this.minimumFractionDigits < 127 ? (byte) this.minimumFractionDigits : (byte) 127);
        fields.put("minimumFractionDigits", this.minimumFractionDigits);
        fields.put("minimumIntegerDigits", this.minimumIntegerDigits);
        if (this.minimumIntegerDigits < 127) {
            b = (byte) this.minimumIntegerDigits;
        }
        fields.put("minIntegerDigits", b);
        fields.put("parseIntegerOnly", this.parseIntegerOnly);
        fields.put("serialVersionOnStream", 1);
        stream.writeFields();
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = stream.readFields();
        this.groupingUsed = fields.get("groupingUsed", true);
        this.parseIntegerOnly = fields.get("parseIntegerOnly", false);
        if (fields.get("serialVersionOnStream", 0) == 0) {
            this.maximumFractionDigits = fields.get("maxFractionDigits", (byte) 3);
            this.maximumIntegerDigits = fields.get("maxIntegerDigits", (byte) 40);
            this.minimumFractionDigits = fields.get("minFractionDigits", (byte) 0);
            this.minimumIntegerDigits = fields.get("minIntegerDigits", (byte) 1);
        } else {
            this.maximumFractionDigits = fields.get("maximumFractionDigits", 3);
            this.maximumIntegerDigits = fields.get("maximumIntegerDigits", 40);
            this.minimumFractionDigits = fields.get("minimumFractionDigits", 0);
            this.minimumIntegerDigits = fields.get("minimumIntegerDigits", 1);
        }
        if (this.minimumIntegerDigits > this.maximumIntegerDigits || this.minimumFractionDigits > this.maximumFractionDigits) {
            throw new InvalidObjectException("min digits greater than max digits");
        }
        if (this.minimumIntegerDigits < 0 || this.maximumIntegerDigits < 0 || this.minimumFractionDigits < 0 || this.maximumFractionDigits < 0) {
            throw new InvalidObjectException("min or max digits negative");
        }
    }

    public static class Field extends Format.Field {
        private static final long serialVersionUID = 7494728892700160890L;
        public static final Field SIGN = new Field("sign");
        public static final Field INTEGER = new Field("integer");
        public static final Field FRACTION = new Field("fraction");
        public static final Field EXPONENT = new Field("exponent");
        public static final Field EXPONENT_SIGN = new Field("exponent sign");
        public static final Field EXPONENT_SYMBOL = new Field("exponent symbol");
        public static final Field DECIMAL_SEPARATOR = new Field("decimal separator");
        public static final Field GROUPING_SEPARATOR = new Field("grouping separator");
        public static final Field PERCENT = new Field("percent");
        public static final Field PERMILLE = new Field("per mille");
        public static final Field CURRENCY = new Field("currency");

        protected Field(String fieldName) {
            super(fieldName);
        }
    }

    public RoundingMode getRoundingMode() {
        throw new UnsupportedOperationException();
    }

    public void setRoundingMode(RoundingMode roundingMode) {
        throw new UnsupportedOperationException();
    }
}
