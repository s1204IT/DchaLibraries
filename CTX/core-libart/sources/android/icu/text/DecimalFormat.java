package android.icu.text;

import android.icu.impl.number.AffixUtils;
import android.icu.impl.number.DecimalFormatProperties;
import android.icu.impl.number.Padder;
import android.icu.impl.number.Parse;
import android.icu.impl.number.PatternStringParser;
import android.icu.impl.number.PatternStringUtils;
import android.icu.impl.number.Properties;
import android.icu.math.MathContext;
import android.icu.number.FormattedNumber;
import android.icu.number.LocalizedNumberFormatter;
import android.icu.number.NumberFormatter;
import android.icu.text.PluralRules;
import android.icu.util.Currency;
import android.icu.util.CurrencyAmount;
import android.icu.util.ULocale;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.AttributedCharacterIterator;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;

public class DecimalFormat extends NumberFormat {
    static final boolean $assertionsDisabled = false;
    public static final int PAD_AFTER_PREFIX = 1;
    public static final int PAD_AFTER_SUFFIX = 3;
    public static final int PAD_BEFORE_PREFIX = 0;
    public static final int PAD_BEFORE_SUFFIX = 2;
    private static final long serialVersionUID = 864413376551465018L;
    private static final ThreadLocal<DecimalFormatProperties> threadLocalProperties = new ThreadLocal<DecimalFormatProperties>() {
        @Override
        protected DecimalFormatProperties initialValue() {
            return new DecimalFormatProperties();
        }
    };
    volatile transient DecimalFormatProperties exportedProperties;
    volatile transient LocalizedNumberFormatter formatter;
    private transient int icuMathContextForm;
    transient DecimalFormatProperties properties;
    private final int serialVersionOnStream;
    volatile transient DecimalFormatSymbols symbols;

    @Deprecated
    public interface PropertySetter {
        @Deprecated
        void set(DecimalFormatProperties decimalFormatProperties);
    }

    public DecimalFormat() {
        this.serialVersionOnStream = 5;
        this.icuMathContextForm = 0;
        String pattern = getPattern(ULocale.getDefault(ULocale.Category.FORMAT), 0);
        this.symbols = getDefaultSymbols();
        this.properties = new DecimalFormatProperties();
        this.exportedProperties = new DecimalFormatProperties();
        setPropertiesFromPattern(pattern, 1);
        refreshFormatter();
    }

    public DecimalFormat(String str) {
        this.serialVersionOnStream = 5;
        this.icuMathContextForm = 0;
        this.symbols = getDefaultSymbols();
        this.properties = new DecimalFormatProperties();
        this.exportedProperties = new DecimalFormatProperties();
        setPropertiesFromPattern(str, 1);
        refreshFormatter();
    }

    public DecimalFormat(String str, DecimalFormatSymbols decimalFormatSymbols) {
        this.serialVersionOnStream = 5;
        this.icuMathContextForm = 0;
        this.symbols = (DecimalFormatSymbols) decimalFormatSymbols.clone();
        this.properties = new DecimalFormatProperties();
        this.exportedProperties = new DecimalFormatProperties();
        setPropertiesFromPattern(str, 1);
        refreshFormatter();
    }

    public DecimalFormat(String str, DecimalFormatSymbols decimalFormatSymbols, CurrencyPluralInfo currencyPluralInfo, int i) {
        this(str, decimalFormatSymbols, i);
        this.properties.setCurrencyPluralInfo(currencyPluralInfo);
        refreshFormatter();
    }

    DecimalFormat(String str, DecimalFormatSymbols decimalFormatSymbols, int i) {
        this.serialVersionOnStream = 5;
        this.icuMathContextForm = 0;
        this.symbols = (DecimalFormatSymbols) decimalFormatSymbols.clone();
        this.properties = new DecimalFormatProperties();
        this.exportedProperties = new DecimalFormatProperties();
        if (i == 1 || i == 5 || i == 7 || i == 8 || i == 9 || i == 6) {
            setPropertiesFromPattern(str, 2);
        } else {
            setPropertiesFromPattern(str, 1);
        }
        refreshFormatter();
    }

    private static DecimalFormatSymbols getDefaultSymbols() {
        return DecimalFormatSymbols.getInstance();
    }

    public synchronized void applyPattern(String str) {
        setPropertiesFromPattern(str, 0);
        this.properties.setPositivePrefix(null);
        this.properties.setNegativePrefix(null);
        this.properties.setPositiveSuffix(null);
        this.properties.setNegativeSuffix(null);
        this.properties.setCurrencyPluralInfo(null);
        refreshFormatter();
    }

    public synchronized void applyLocalizedPattern(String str) {
        applyPattern(PatternStringUtils.convertLocalized(str, this.symbols, false));
    }

    @Override
    public Object clone() {
        DecimalFormat decimalFormat = (DecimalFormat) super.clone();
        decimalFormat.symbols = (DecimalFormatSymbols) this.symbols.clone();
        decimalFormat.properties = this.properties.m3clone();
        decimalFormat.exportedProperties = new DecimalFormatProperties();
        decimalFormat.refreshFormatter();
        return decimalFormat;
    }

    private synchronized void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        objectOutputStream.writeInt(0);
        objectOutputStream.writeObject(this.properties);
        objectOutputStream.writeObject(this.symbols);
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        ObjectInputStream.GetField fields = objectInputStream.readFields();
        ObjectStreamField[] fields2 = fields.getObjectStreamClass().getFields();
        int i = fields.get("serialVersionOnStream", -1);
        if (i > 5) {
            throw new IOException("Cannot deserialize newer android.icu.text.DecimalFormat (v" + i + ")");
        }
        if (i == 5) {
            if (fields2.length > 1) {
                throw new IOException("Too many fields when reading serial version 5");
            }
            objectInputStream.readInt();
            Object object = objectInputStream.readObject();
            if (object instanceof DecimalFormatProperties) {
                this.properties = (DecimalFormatProperties) object;
            } else {
                this.properties = ((Properties) object).getInstance();
            }
            this.symbols = (DecimalFormatSymbols) objectInputStream.readObject();
            this.exportedProperties = new DecimalFormatProperties();
            refreshFormatter();
            return;
        }
        this.properties = new DecimalFormatProperties();
        int length = fields2.length;
        ?? r4 = 0;
        int i2 = 0;
        String str = null;
        String str2 = null;
        String str3 = null;
        String str4 = null;
        String str5 = null;
        String str6 = null;
        String str7 = null;
        String str8 = null;
        while (i2 < length) {
            String name = fields2[i2].getName();
            if (name.equals("decimalSeparatorAlwaysShown")) {
                setDecimalSeparatorAlwaysShown(fields.get("decimalSeparatorAlwaysShown", (boolean) r4));
            } else if (name.equals("exponentSignAlwaysShown")) {
                setExponentSignAlwaysShown(fields.get("exponentSignAlwaysShown", (boolean) r4));
            } else if (name.equals("formatWidth")) {
                setFormatWidth(fields.get("formatWidth", (int) r4));
            } else if (name.equals("groupingSize")) {
                setGroupingSize(fields.get("groupingSize", (byte) 3));
            } else if (name.equals("groupingSize2")) {
                setSecondaryGroupingSize(fields.get("groupingSize2", (byte) r4));
            } else if (name.equals("maxSignificantDigits")) {
                setMaximumSignificantDigits(fields.get("maxSignificantDigits", 6));
            } else if (name.equals("minExponentDigits")) {
                setMinimumExponentDigits(fields.get("minExponentDigits", (byte) r4));
            } else if (name.equals("minSignificantDigits")) {
                setMinimumSignificantDigits(fields.get("minSignificantDigits", 1));
            } else if (name.equals("multiplier")) {
                setMultiplier(fields.get("multiplier", 1));
            } else if (name.equals("pad")) {
                setPadCharacter(fields.get("pad", ' '));
            } else if (name.equals("padPosition")) {
                setPadPosition(fields.get("padPosition", 0));
            } else if (name.equals("parseBigDecimal")) {
                setParseBigDecimal(fields.get("parseBigDecimal", false));
            } else if (name.equals("parseRequireDecimalPoint")) {
                setDecimalPatternMatchRequired(fields.get("parseRequireDecimalPoint", false));
            } else if (name.equals("roundingMode")) {
                setRoundingMode(fields.get("roundingMode", 0));
            } else if (name.equals("useExponentialNotation")) {
                setScientificNotation(fields.get("useExponentialNotation", false));
            } else if (name.equals("useSignificantDigits")) {
                setSignificantDigitsUsed(fields.get("useSignificantDigits", false));
            } else {
                if (name.equals("currencyPluralInfo")) {
                    setCurrencyPluralInfo((CurrencyPluralInfo) fields.get("currencyPluralInfo", (Object) null));
                } else if (name.equals("mathContext")) {
                    setMathContextICU((MathContext) fields.get("mathContext", (Object) null));
                } else if (name.equals("negPrefixPattern")) {
                    str = (String) fields.get("negPrefixPattern", (Object) null);
                } else if (name.equals("negSuffixPattern")) {
                    str3 = (String) fields.get("negSuffixPattern", (Object) null);
                } else if (name.equals("negativePrefix")) {
                    str2 = (String) fields.get("negativePrefix", (Object) null);
                } else if (name.equals("negativeSuffix")) {
                    str4 = (String) fields.get("negativeSuffix", (Object) null);
                } else if (name.equals("posPrefixPattern")) {
                    str5 = (String) fields.get("posPrefixPattern", (Object) null);
                } else if (name.equals("posSuffixPattern")) {
                    str7 = (String) fields.get("posSuffixPattern", (Object) null);
                } else if (name.equals("positivePrefix")) {
                    str6 = (String) fields.get("positivePrefix", (Object) null);
                } else if (name.equals("positiveSuffix")) {
                    str8 = (String) fields.get("positiveSuffix", (Object) null);
                } else if (name.equals("roundingIncrement")) {
                    setRoundingIncrement((BigDecimal) fields.get("roundingIncrement", (Object) null));
                } else if (name.equals("symbols")) {
                    setDecimalFormatSymbols((DecimalFormatSymbols) fields.get("symbols", (Object) null));
                }
                i2++;
                r4 = 0;
            }
            i2++;
            r4 = 0;
        }
        if (str == null) {
            this.properties.setNegativePrefix(str2);
        } else {
            this.properties.setNegativePrefixPattern(str);
        }
        if (str3 == null) {
            this.properties.setNegativeSuffix(str4);
        } else {
            this.properties.setNegativeSuffixPattern(str3);
        }
        if (str5 == null) {
            this.properties.setPositivePrefix(str6);
        } else {
            this.properties.setPositivePrefixPattern(str5);
        }
        if (str7 == null) {
            this.properties.setPositiveSuffix(str8);
        } else {
            this.properties.setPositiveSuffixPattern(str7);
        }
        try {
            Field declaredField = NumberFormat.class.getDeclaredField("groupingUsed");
            declaredField.setAccessible(true);
            setGroupingUsed(((Boolean) declaredField.get(this)).booleanValue());
            Field declaredField2 = NumberFormat.class.getDeclaredField("parseIntegerOnly");
            declaredField2.setAccessible(true);
            setParseIntegerOnly(((Boolean) declaredField2.get(this)).booleanValue());
            Field declaredField3 = NumberFormat.class.getDeclaredField("maximumIntegerDigits");
            declaredField3.setAccessible(true);
            setMaximumIntegerDigits(((Integer) declaredField3.get(this)).intValue());
            Field declaredField4 = NumberFormat.class.getDeclaredField("minimumIntegerDigits");
            declaredField4.setAccessible(true);
            setMinimumIntegerDigits(((Integer) declaredField4.get(this)).intValue());
            Field declaredField5 = NumberFormat.class.getDeclaredField("maximumFractionDigits");
            declaredField5.setAccessible(true);
            setMaximumFractionDigits(((Integer) declaredField5.get(this)).intValue());
            Field declaredField6 = NumberFormat.class.getDeclaredField("minimumFractionDigits");
            declaredField6.setAccessible(true);
            setMinimumFractionDigits(((Integer) declaredField6.get(this)).intValue());
            Field declaredField7 = NumberFormat.class.getDeclaredField("currency");
            declaredField7.setAccessible(true);
            setCurrency((Currency) declaredField7.get(this));
            Field declaredField8 = NumberFormat.class.getDeclaredField("parseStrict");
            declaredField8.setAccessible(true);
            setParseStrict(((Boolean) declaredField8.get(this)).booleanValue());
            if (this.symbols == null) {
                this.symbols = getDefaultSymbols();
            }
            this.exportedProperties = new DecimalFormatProperties();
            refreshFormatter();
        } catch (IllegalAccessException e) {
            throw new IOException(e);
        } catch (IllegalArgumentException e2) {
            throw new IOException(e2);
        } catch (NoSuchFieldException e3) {
            throw new IOException(e3);
        } catch (SecurityException e4) {
            throw new IOException(e4);
        }
    }

    @Override
    public StringBuffer format(double d, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        FormattedNumber formattedNumber = this.formatter.format(d);
        formattedNumber.populateFieldPosition(fieldPosition, stringBuffer.length());
        formattedNumber.appendTo(stringBuffer);
        return stringBuffer;
    }

    @Override
    public StringBuffer format(long j, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        FormattedNumber formattedNumber = this.formatter.format(j);
        formattedNumber.populateFieldPosition(fieldPosition, stringBuffer.length());
        formattedNumber.appendTo(stringBuffer);
        return stringBuffer;
    }

    @Override
    public StringBuffer format(BigInteger bigInteger, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        FormattedNumber formattedNumber = this.formatter.format(bigInteger);
        formattedNumber.populateFieldPosition(fieldPosition, stringBuffer.length());
        formattedNumber.appendTo(stringBuffer);
        return stringBuffer;
    }

    @Override
    public StringBuffer format(BigDecimal bigDecimal, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        FormattedNumber formattedNumber = this.formatter.format(bigDecimal);
        formattedNumber.populateFieldPosition(fieldPosition, stringBuffer.length());
        formattedNumber.appendTo(stringBuffer);
        return stringBuffer;
    }

    @Override
    public StringBuffer format(android.icu.math.BigDecimal bigDecimal, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        FormattedNumber formattedNumber = this.formatter.format(bigDecimal);
        formattedNumber.populateFieldPosition(fieldPosition, stringBuffer.length());
        formattedNumber.appendTo(stringBuffer);
        return stringBuffer;
    }

    @Override
    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        if (!(obj instanceof Number)) {
            throw new IllegalArgumentException();
        }
        return this.formatter.format((Number) obj).getFieldIterator();
    }

    @Override
    public StringBuffer format(CurrencyAmount currencyAmount, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        FormattedNumber formattedNumber = this.formatter.format(currencyAmount);
        formattedNumber.populateFieldPosition(fieldPosition, stringBuffer.length());
        formattedNumber.appendTo(stringBuffer);
        return stringBuffer;
    }

    @Override
    public Number parse(String str, ParsePosition parsePosition) {
        DecimalFormatProperties decimalFormatProperties = threadLocalProperties.get();
        synchronized (this) {
            decimalFormatProperties.copyFrom(this.properties);
        }
        Number number = Parse.parse(str, parsePosition, decimalFormatProperties, this.symbols);
        if (number instanceof BigDecimal) {
            return safeConvertBigDecimal((BigDecimal) number);
        }
        return number;
    }

    @Override
    public CurrencyAmount parseCurrency(CharSequence charSequence, ParsePosition parsePosition) {
        try {
            DecimalFormatProperties decimalFormatProperties = threadLocalProperties.get();
            synchronized (this) {
                decimalFormatProperties.copyFrom(this.properties);
            }
            CurrencyAmount currency = Parse.parseCurrency(charSequence, parsePosition, decimalFormatProperties, this.symbols);
            if (currency == null) {
                return null;
            }
            Number number = currency.getNumber();
            return number instanceof BigDecimal ? new CurrencyAmount(safeConvertBigDecimal((BigDecimal) number), currency.getCurrency()) : currency;
        } catch (ParseException e) {
            return null;
        }
    }

    public synchronized DecimalFormatSymbols getDecimalFormatSymbols() {
        return (DecimalFormatSymbols) this.symbols.clone();
    }

    public synchronized void setDecimalFormatSymbols(DecimalFormatSymbols decimalFormatSymbols) {
        this.symbols = (DecimalFormatSymbols) decimalFormatSymbols.clone();
        refreshFormatter();
    }

    public synchronized String getPositivePrefix() {
        return this.formatter.format(1L).getPrefix();
    }

    public synchronized void setPositivePrefix(String str) {
        if (str == null) {
            throw new NullPointerException();
        }
        this.properties.setPositivePrefix(str);
        refreshFormatter();
    }

    public synchronized String getNegativePrefix() {
        return this.formatter.format(-1L).getPrefix();
    }

    public synchronized void setNegativePrefix(String str) {
        if (str == null) {
            throw new NullPointerException();
        }
        this.properties.setNegativePrefix(str);
        refreshFormatter();
    }

    public synchronized String getPositiveSuffix() {
        return this.formatter.format(1L).getSuffix();
    }

    public synchronized void setPositiveSuffix(String str) {
        if (str == null) {
            throw new NullPointerException();
        }
        this.properties.setPositiveSuffix(str);
        refreshFormatter();
    }

    public synchronized String getNegativeSuffix() {
        return this.formatter.format(-1L).getSuffix();
    }

    public synchronized void setNegativeSuffix(String str) {
        if (str == null) {
            throw new NullPointerException();
        }
        this.properties.setNegativeSuffix(str);
        refreshFormatter();
    }

    @Deprecated
    public synchronized boolean getSignAlwaysShown() {
        return this.properties.getSignAlwaysShown();
    }

    @Deprecated
    public synchronized void setSignAlwaysShown(boolean z) {
        this.properties.setSignAlwaysShown(z);
        refreshFormatter();
    }

    public synchronized int getMultiplier() {
        if (this.properties.getMultiplier() != null) {
            return this.properties.getMultiplier().intValue();
        }
        return (int) Math.pow(10.0d, this.properties.getMagnitudeMultiplier());
    }

    public synchronized void setMultiplier(int i) {
        if (i == 0) {
            throw new IllegalArgumentException("Multiplier must be nonzero.");
        }
        int i2 = 0;
        int i3 = i;
        while (true) {
            if (i == 1) {
                break;
            }
            i2++;
            int i4 = i3 / 10;
            if (i4 * 10 == i3) {
                i3 = i4;
            } else {
                i2 = -1;
                break;
            }
        }
        if (i2 != -1) {
            this.properties.setMagnitudeMultiplier(i2);
        } else {
            this.properties.setMultiplier(BigDecimal.valueOf(i));
        }
        refreshFormatter();
    }

    public synchronized BigDecimal getRoundingIncrement() {
        return this.exportedProperties.getRoundingIncrement();
    }

    public synchronized void setRoundingIncrement(BigDecimal bigDecimal) {
        if (bigDecimal != null) {
            if (bigDecimal.compareTo(BigDecimal.ZERO) == 0) {
                this.properties.setMaximumFractionDigits(Integer.MAX_VALUE);
                return;
            }
        }
        this.properties.setRoundingIncrement(bigDecimal);
        refreshFormatter();
    }

    public synchronized void setRoundingIncrement(android.icu.math.BigDecimal bigDecimal) {
        setRoundingIncrement(bigDecimal == null ? null : bigDecimal.toBigDecimal());
    }

    public synchronized void setRoundingIncrement(double d) {
        try {
            if (d == 0.0d) {
                setRoundingIncrement((BigDecimal) null);
            } else {
                setRoundingIncrement(BigDecimal.valueOf(d));
            }
        } catch (Throwable th) {
            throw th;
        }
    }

    @Override
    public synchronized int getRoundingMode() {
        RoundingMode roundingMode;
        roundingMode = this.exportedProperties.getRoundingMode();
        return roundingMode == null ? 0 : roundingMode.ordinal();
    }

    @Override
    public synchronized void setRoundingMode(int i) {
        this.properties.setRoundingMode(RoundingMode.valueOf(i));
        refreshFormatter();
    }

    public synchronized java.math.MathContext getMathContext() {
        return this.exportedProperties.getMathContext();
    }

    public synchronized void setMathContext(java.math.MathContext mathContext) {
        this.properties.setMathContext(mathContext);
        refreshFormatter();
    }

    public synchronized MathContext getMathContextICU() {
        java.math.MathContext mathContext;
        mathContext = getMathContext();
        return new MathContext(mathContext.getPrecision(), this.icuMathContextForm, false, mathContext.getRoundingMode().ordinal());
    }

    public synchronized void setMathContextICU(MathContext mathContext) {
        java.math.MathContext mathContext2;
        this.icuMathContextForm = mathContext.getForm();
        if (mathContext.getLostDigits()) {
            mathContext2 = new java.math.MathContext(mathContext.getDigits(), RoundingMode.UNNECESSARY);
        } else {
            mathContext2 = new java.math.MathContext(mathContext.getDigits(), RoundingMode.valueOf(mathContext.getRoundingMode()));
        }
        setMathContext(mathContext2);
    }

    @Override
    public synchronized int getMinimumIntegerDigits() {
        return this.exportedProperties.getMinimumIntegerDigits();
    }

    @Override
    public synchronized void setMinimumIntegerDigits(int i) {
        int maximumIntegerDigits = this.properties.getMaximumIntegerDigits();
        if (maximumIntegerDigits >= 0 && maximumIntegerDigits < i) {
            this.properties.setMaximumIntegerDigits(i);
        }
        this.properties.setMinimumIntegerDigits(i);
        refreshFormatter();
    }

    @Override
    public synchronized int getMaximumIntegerDigits() {
        return this.exportedProperties.getMaximumIntegerDigits();
    }

    @Override
    public synchronized void setMaximumIntegerDigits(int i) {
        int minimumIntegerDigits = this.properties.getMinimumIntegerDigits();
        if (minimumIntegerDigits >= 0 && minimumIntegerDigits > i) {
            this.properties.setMinimumIntegerDigits(i);
        }
        this.properties.setMaximumIntegerDigits(i);
        refreshFormatter();
    }

    @Override
    public synchronized int getMinimumFractionDigits() {
        return this.exportedProperties.getMinimumFractionDigits();
    }

    @Override
    public synchronized void setMinimumFractionDigits(int i) {
        int maximumFractionDigits = this.properties.getMaximumFractionDigits();
        if (maximumFractionDigits >= 0 && maximumFractionDigits < i) {
            this.properties.setMaximumFractionDigits(i);
        }
        this.properties.setMinimumFractionDigits(i);
        refreshFormatter();
    }

    @Override
    public synchronized int getMaximumFractionDigits() {
        return this.exportedProperties.getMaximumFractionDigits();
    }

    @Override
    public synchronized void setMaximumFractionDigits(int i) {
        int minimumFractionDigits = this.properties.getMinimumFractionDigits();
        if (minimumFractionDigits >= 0 && minimumFractionDigits > i) {
            this.properties.setMinimumFractionDigits(i);
        }
        this.properties.setMaximumFractionDigits(i);
        refreshFormatter();
    }

    public synchronized boolean areSignificantDigitsUsed() {
        boolean z;
        if (this.properties.getMinimumSignificantDigits() == -1) {
            z = this.properties.getMaximumSignificantDigits() != -1;
        }
        return z;
    }

    public synchronized void setSignificantDigitsUsed(boolean z) {
        try {
            if (z) {
                this.properties.setMinimumSignificantDigits(1);
                this.properties.setMaximumSignificantDigits(6);
            } else {
                this.properties.setMinimumSignificantDigits(-1);
                this.properties.setMaximumSignificantDigits(-1);
            }
            refreshFormatter();
        } catch (Throwable th) {
            throw th;
        }
    }

    public synchronized int getMinimumSignificantDigits() {
        return this.exportedProperties.getMinimumSignificantDigits();
    }

    public synchronized void setMinimumSignificantDigits(int i) {
        int maximumSignificantDigits = this.properties.getMaximumSignificantDigits();
        if (maximumSignificantDigits >= 0 && maximumSignificantDigits < i) {
            this.properties.setMaximumSignificantDigits(i);
        }
        this.properties.setMinimumSignificantDigits(i);
        refreshFormatter();
    }

    public synchronized int getMaximumSignificantDigits() {
        return this.exportedProperties.getMaximumSignificantDigits();
    }

    public synchronized void setMaximumSignificantDigits(int i) {
        int minimumSignificantDigits = this.properties.getMinimumSignificantDigits();
        if (minimumSignificantDigits >= 0 && minimumSignificantDigits > i) {
            this.properties.setMinimumSignificantDigits(i);
        }
        this.properties.setMaximumSignificantDigits(i);
        refreshFormatter();
    }

    public synchronized int getFormatWidth() {
        return this.properties.getFormatWidth();
    }

    public synchronized void setFormatWidth(int i) {
        this.properties.setFormatWidth(i);
        refreshFormatter();
    }

    public synchronized char getPadCharacter() {
        String padString = this.properties.getPadString();
        if (padString == null) {
            return '.';
        }
        return padString.charAt(0);
    }

    public synchronized void setPadCharacter(char c) {
        this.properties.setPadString(Character.toString(c));
        refreshFormatter();
    }

    public synchronized int getPadPosition() {
        Padder.PadPosition padPosition;
        padPosition = this.properties.getPadPosition();
        return padPosition == null ? 0 : padPosition.toOld();
    }

    public synchronized void setPadPosition(int i) {
        this.properties.setPadPosition(Padder.PadPosition.fromOld(i));
        refreshFormatter();
    }

    public synchronized boolean isScientificNotation() {
        return this.properties.getMinimumExponentDigits() != -1;
    }

    public synchronized void setScientificNotation(boolean z) {
        try {
            if (z) {
                this.properties.setMinimumExponentDigits(1);
            } else {
                this.properties.setMinimumExponentDigits(-1);
            }
            refreshFormatter();
        } catch (Throwable th) {
            throw th;
        }
    }

    public synchronized byte getMinimumExponentDigits() {
        return (byte) this.properties.getMinimumExponentDigits();
    }

    public synchronized void setMinimumExponentDigits(byte b) {
        this.properties.setMinimumExponentDigits(b);
        refreshFormatter();
    }

    public synchronized boolean isExponentSignAlwaysShown() {
        return this.properties.getExponentSignAlwaysShown();
    }

    public synchronized void setExponentSignAlwaysShown(boolean z) {
        this.properties.setExponentSignAlwaysShown(z);
        refreshFormatter();
    }

    @Override
    public synchronized boolean isGroupingUsed() {
        boolean z;
        if (this.properties.getGroupingSize() <= 0) {
            z = this.properties.getSecondaryGroupingSize() > 0;
        }
        return z;
    }

    @Override
    public synchronized void setGroupingUsed(boolean z) {
        try {
            if (z) {
                this.properties.setGroupingSize(3);
            } else {
                this.properties.setGroupingSize(0);
                this.properties.setSecondaryGroupingSize(0);
            }
            refreshFormatter();
        } catch (Throwable th) {
            throw th;
        }
    }

    public synchronized int getGroupingSize() {
        return this.properties.getGroupingSize();
    }

    public synchronized void setGroupingSize(int i) {
        this.properties.setGroupingSize(i);
        refreshFormatter();
    }

    public synchronized int getSecondaryGroupingSize() {
        int groupingSize = this.properties.getGroupingSize();
        int secondaryGroupingSize = this.properties.getSecondaryGroupingSize();
        if (groupingSize != secondaryGroupingSize && secondaryGroupingSize >= 0) {
            return this.properties.getSecondaryGroupingSize();
        }
        return 0;
    }

    public synchronized void setSecondaryGroupingSize(int i) {
        this.properties.setSecondaryGroupingSize(i);
        refreshFormatter();
    }

    @Deprecated
    public synchronized int getMinimumGroupingDigits() {
        return this.properties.getMinimumGroupingDigits() == 2 ? 2 : 1;
    }

    @Deprecated
    public synchronized void setMinimumGroupingDigits(int i) {
        this.properties.setMinimumGroupingDigits(i);
        refreshFormatter();
    }

    public synchronized boolean isDecimalSeparatorAlwaysShown() {
        return this.properties.getDecimalSeparatorAlwaysShown();
    }

    public synchronized void setDecimalSeparatorAlwaysShown(boolean z) {
        this.properties.setDecimalSeparatorAlwaysShown(z);
        refreshFormatter();
    }

    @Override
    public synchronized Currency getCurrency() {
        return this.properties.getCurrency();
    }

    @Override
    public synchronized void setCurrency(Currency currency) {
        this.properties.setCurrency(currency);
        if (currency != null) {
            this.symbols.setCurrency(currency);
            this.symbols.setCurrencySymbol(currency.getName(this.symbols.getULocale(), 0, (boolean[]) null));
        }
        refreshFormatter();
    }

    public synchronized Currency.CurrencyUsage getCurrencyUsage() {
        Currency.CurrencyUsage currencyUsage;
        currencyUsage = this.properties.getCurrencyUsage();
        if (currencyUsage == null) {
            currencyUsage = Currency.CurrencyUsage.STANDARD;
        }
        return currencyUsage;
    }

    public synchronized void setCurrencyUsage(Currency.CurrencyUsage currencyUsage) {
        this.properties.setCurrencyUsage(currencyUsage);
        refreshFormatter();
    }

    public synchronized CurrencyPluralInfo getCurrencyPluralInfo() {
        return this.properties.getCurrencyPluralInfo();
    }

    public synchronized void setCurrencyPluralInfo(CurrencyPluralInfo currencyPluralInfo) {
        this.properties.setCurrencyPluralInfo(currencyPluralInfo);
        refreshFormatter();
    }

    public synchronized boolean isParseBigDecimal() {
        return this.properties.getParseToBigDecimal();
    }

    public synchronized void setParseBigDecimal(boolean z) {
        this.properties.setParseToBigDecimal(z);
    }

    @Deprecated
    public int getParseMaxDigits() {
        return 1000;
    }

    @Deprecated
    public void setParseMaxDigits(int i) {
    }

    @Override
    public synchronized boolean isParseStrict() {
        return this.properties.getParseMode() == Parse.ParseMode.STRICT;
    }

    @Override
    public synchronized void setParseStrict(boolean z) {
        try {
            this.properties.setParseMode(z ? Parse.ParseMode.STRICT : Parse.ParseMode.LENIENT);
        } catch (Throwable th) {
            throw th;
        }
    }

    @Override
    public synchronized boolean isParseIntegerOnly() {
        return this.properties.getParseIntegerOnly();
    }

    @Override
    public synchronized void setParseIntegerOnly(boolean z) {
        this.properties.setParseIntegerOnly(z);
    }

    public synchronized boolean isDecimalPatternMatchRequired() {
        return this.properties.getDecimalPatternMatchRequired();
    }

    public synchronized void setDecimalPatternMatchRequired(boolean z) {
        this.properties.setDecimalPatternMatchRequired(z);
        refreshFormatter();
    }

    @Deprecated
    public synchronized boolean getParseNoExponent() {
        return this.properties.getParseNoExponent();
    }

    @Deprecated
    public synchronized void setParseNoExponent(boolean z) {
        this.properties.setParseNoExponent(z);
        refreshFormatter();
    }

    @Deprecated
    public synchronized boolean getParseCaseSensitive() {
        return this.properties.getParseCaseSensitive();
    }

    @Deprecated
    public synchronized void setParseCaseSensitive(boolean z) {
        this.properties.setParseCaseSensitive(z);
        refreshFormatter();
    }

    @Override
    public synchronized boolean equals(Object obj) {
        boolean z = false;
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DecimalFormat)) {
            return false;
        }
        DecimalFormat decimalFormat = (DecimalFormat) obj;
        if (this.properties.equals(decimalFormat.properties)) {
            if (this.symbols.equals(decimalFormat.symbols)) {
                z = true;
            }
        }
        return z;
    }

    @Override
    public synchronized int hashCode() {
        return this.properties.hashCode() ^ this.symbols.hashCode();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName());
        sb.append("@");
        sb.append(Integer.toHexString(hashCode()));
        sb.append(" { symbols@");
        sb.append(Integer.toHexString(this.symbols.hashCode()));
        synchronized (this) {
            this.properties.toStringBare(sb);
        }
        sb.append(" }");
        return sb.toString();
    }

    public synchronized String toPattern() {
        DecimalFormatProperties decimalFormatPropertiesCopyFrom;
        decimalFormatPropertiesCopyFrom = threadLocalProperties.get().copyFrom(this.properties);
        if (useCurrency(this.properties)) {
            decimalFormatPropertiesCopyFrom.setMinimumFractionDigits(this.exportedProperties.getMinimumFractionDigits());
            decimalFormatPropertiesCopyFrom.setMaximumFractionDigits(this.exportedProperties.getMaximumFractionDigits());
            decimalFormatPropertiesCopyFrom.setRoundingIncrement(this.exportedProperties.getRoundingIncrement());
        }
        return PatternStringUtils.propertiesToPatternString(decimalFormatPropertiesCopyFrom);
    }

    public synchronized String toLocalizedPattern() {
        return PatternStringUtils.convertLocalized(toPattern(), this.symbols, true);
    }

    public LocalizedNumberFormatter toNumberFormatter() {
        return this.formatter;
    }

    @Deprecated
    public PluralRules.IFixedDecimal getFixedDecimal(double d) {
        return this.formatter.format(d).getFixedDecimal();
    }

    void refreshFormatter() {
        if (this.exportedProperties == null) {
            return;
        }
        ULocale locale = getLocale(ULocale.ACTUAL_LOCALE);
        if (locale == null) {
            locale = this.symbols.getLocale(ULocale.ACTUAL_LOCALE);
        }
        if (locale == null) {
            locale = this.symbols.getULocale();
        }
        this.formatter = NumberFormatter.fromDecimalFormat(this.properties, this.symbols, this.exportedProperties).locale(locale);
    }

    private Number safeConvertBigDecimal(BigDecimal bigDecimal) {
        try {
            return new android.icu.math.BigDecimal(bigDecimal);
        } catch (NumberFormatException e) {
            if (bigDecimal.signum() > 0 && bigDecimal.scale() < 0) {
                return Double.valueOf(Double.POSITIVE_INFINITY);
            }
            if (bigDecimal.scale() < 0) {
                return Double.valueOf(Double.NEGATIVE_INFINITY);
            }
            if (bigDecimal.signum() < 0) {
                return Double.valueOf(-0.0d);
            }
            return Double.valueOf(0.0d);
        }
    }

    private static boolean useCurrency(DecimalFormatProperties decimalFormatProperties) {
        return decimalFormatProperties.getCurrency() != null || decimalFormatProperties.getCurrencyPluralInfo() != null || decimalFormatProperties.getCurrencyUsage() != null || AffixUtils.hasCurrencySymbols(decimalFormatProperties.getPositivePrefixPattern()) || AffixUtils.hasCurrencySymbols(decimalFormatProperties.getPositiveSuffixPattern()) || AffixUtils.hasCurrencySymbols(decimalFormatProperties.getNegativePrefixPattern()) || AffixUtils.hasCurrencySymbols(decimalFormatProperties.getNegativeSuffixPattern());
    }

    void setPropertiesFromPattern(String str, int i) {
        if (str == null) {
            throw new NullPointerException();
        }
        PatternStringParser.parseToExistingProperties(str, this.properties, i);
    }

    @Deprecated
    public synchronized void setProperties(PropertySetter propertySetter) {
        propertySetter.set(this.properties);
        refreshFormatter();
    }
}
