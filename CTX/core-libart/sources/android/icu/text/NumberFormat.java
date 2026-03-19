package android.icu.text;

import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.math.BigDecimal;
import android.icu.text.DisplayContext;
import android.icu.util.Currency;
import android.icu.util.CurrencyAmount;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Collections;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Set;

public abstract class NumberFormat extends UFormat {
    static final boolean $assertionsDisabled = false;
    public static final int ACCOUNTINGCURRENCYSTYLE = 7;
    public static final int CASHCURRENCYSTYLE = 8;
    public static final int CURRENCYSTYLE = 1;
    public static final int FRACTION_FIELD = 1;
    public static final int INTEGERSTYLE = 4;
    public static final int INTEGER_FIELD = 0;
    public static final int ISOCURRENCYSTYLE = 5;
    public static final int NUMBERSTYLE = 0;
    public static final int PERCENTSTYLE = 2;
    public static final int PLURALCURRENCYSTYLE = 6;
    public static final int SCIENTIFICSTYLE = 3;
    public static final int STANDARDCURRENCYSTYLE = 9;
    static final int currentSerialVersion = 2;
    private static final char[] doubleCurrencySign = {164, 164};
    private static final String doubleCurrencyStr = new String(doubleCurrencySign);
    private static final long serialVersionUID = -2308460125733713944L;
    private static NumberFormatShim shim;
    private Currency currency;
    private boolean parseStrict;
    private boolean groupingUsed = true;
    private byte maxIntegerDigits = 40;
    private byte minIntegerDigits = 1;
    private byte maxFractionDigits = 3;
    private byte minFractionDigits = 0;
    private boolean parseIntegerOnly = false;
    private int maximumIntegerDigits = 40;
    private int minimumIntegerDigits = 1;
    private int maximumFractionDigits = 3;
    private int minimumFractionDigits = 0;
    private int serialVersionOnStream = 2;
    private DisplayContext capitalizationSetting = DisplayContext.CAPITALIZATION_NONE;

    public abstract StringBuffer format(double d, StringBuffer stringBuffer, FieldPosition fieldPosition);

    public abstract StringBuffer format(long j, StringBuffer stringBuffer, FieldPosition fieldPosition);

    public abstract StringBuffer format(BigDecimal bigDecimal, StringBuffer stringBuffer, FieldPosition fieldPosition);

    public abstract StringBuffer format(java.math.BigDecimal bigDecimal, StringBuffer stringBuffer, FieldPosition fieldPosition);

    public abstract StringBuffer format(BigInteger bigInteger, StringBuffer stringBuffer, FieldPosition fieldPosition);

    public abstract Number parse(String str, ParsePosition parsePosition);

    @Override
    public StringBuffer format(Object obj, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        if (obj instanceof Long) {
            return format(((Long) obj).longValue(), stringBuffer, fieldPosition);
        }
        if (obj instanceof BigInteger) {
            return format((BigInteger) obj, stringBuffer, fieldPosition);
        }
        if (obj instanceof java.math.BigDecimal) {
            return format((java.math.BigDecimal) obj, stringBuffer, fieldPosition);
        }
        if (obj instanceof BigDecimal) {
            return format((BigDecimal) obj, stringBuffer, fieldPosition);
        }
        if (obj instanceof CurrencyAmount) {
            return format((CurrencyAmount) obj, stringBuffer, fieldPosition);
        }
        if (obj instanceof Number) {
            return format(((Number) obj).doubleValue(), stringBuffer, fieldPosition);
        }
        throw new IllegalArgumentException("Cannot format given Object as a Number");
    }

    @Override
    public final Object parseObject(String str, ParsePosition parsePosition) {
        return parse(str, parsePosition);
    }

    public final String format(double d) {
        return format(d, new StringBuffer(), new FieldPosition(0)).toString();
    }

    public final String format(long j) {
        StringBuffer stringBuffer = new StringBuffer(19);
        format(j, stringBuffer, new FieldPosition(0));
        return stringBuffer.toString();
    }

    public final String format(BigInteger bigInteger) {
        return format(bigInteger, new StringBuffer(), new FieldPosition(0)).toString();
    }

    public final String format(java.math.BigDecimal bigDecimal) {
        return format(bigDecimal, new StringBuffer(), new FieldPosition(0)).toString();
    }

    public final String format(BigDecimal bigDecimal) {
        return format(bigDecimal, new StringBuffer(), new FieldPosition(0)).toString();
    }

    public final String format(CurrencyAmount currencyAmount) {
        return format(currencyAmount, new StringBuffer(), new FieldPosition(0)).toString();
    }

    public StringBuffer format(CurrencyAmount currencyAmount, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        synchronized (this) {
            Currency currency = getCurrency();
            Currency currency2 = currencyAmount.getCurrency();
            boolean zEquals = currency2.equals(currency);
            if (!zEquals) {
                setCurrency(currency2);
            }
            format(currencyAmount.getNumber(), stringBuffer, fieldPosition);
            if (!zEquals) {
                setCurrency(currency);
            }
        }
        return stringBuffer;
    }

    public Number parse(String str) throws ParseException {
        ParsePosition parsePosition = new ParsePosition(0);
        Number number = parse(str, parsePosition);
        if (parsePosition.getIndex() == 0) {
            throw new ParseException("Unparseable number: \"" + str + '\"', parsePosition.getErrorIndex());
        }
        return number;
    }

    public CurrencyAmount parseCurrency(CharSequence charSequence, ParsePosition parsePosition) {
        Number number = parse(charSequence.toString(), parsePosition);
        if (number == null) {
            return null;
        }
        return new CurrencyAmount(number, getEffectiveCurrency());
    }

    public boolean isParseIntegerOnly() {
        return this.parseIntegerOnly;
    }

    public void setParseIntegerOnly(boolean z) {
        this.parseIntegerOnly = z;
    }

    public void setParseStrict(boolean z) {
        this.parseStrict = z;
    }

    public boolean isParseStrict() {
        return this.parseStrict;
    }

    public void setContext(DisplayContext displayContext) {
        if (displayContext.type() == DisplayContext.Type.CAPITALIZATION) {
            this.capitalizationSetting = displayContext;
        }
    }

    public DisplayContext getContext(DisplayContext.Type type) {
        return (type != DisplayContext.Type.CAPITALIZATION || this.capitalizationSetting == null) ? DisplayContext.CAPITALIZATION_NONE : this.capitalizationSetting;
    }

    public static final NumberFormat getInstance() {
        return getInstance(ULocale.getDefault(ULocale.Category.FORMAT), 0);
    }

    public static NumberFormat getInstance(Locale locale) {
        return getInstance(ULocale.forLocale(locale), 0);
    }

    public static NumberFormat getInstance(ULocale uLocale) {
        return getInstance(uLocale, 0);
    }

    public static final NumberFormat getInstance(int i) {
        return getInstance(ULocale.getDefault(ULocale.Category.FORMAT), i);
    }

    public static NumberFormat getInstance(Locale locale, int i) {
        return getInstance(ULocale.forLocale(locale), i);
    }

    public static final NumberFormat getNumberInstance() {
        return getInstance(ULocale.getDefault(ULocale.Category.FORMAT), 0);
    }

    public static NumberFormat getNumberInstance(Locale locale) {
        return getInstance(ULocale.forLocale(locale), 0);
    }

    public static NumberFormat getNumberInstance(ULocale uLocale) {
        return getInstance(uLocale, 0);
    }

    public static final NumberFormat getIntegerInstance() {
        return getInstance(ULocale.getDefault(ULocale.Category.FORMAT), 4);
    }

    public static NumberFormat getIntegerInstance(Locale locale) {
        return getInstance(ULocale.forLocale(locale), 4);
    }

    public static NumberFormat getIntegerInstance(ULocale uLocale) {
        return getInstance(uLocale, 4);
    }

    public static final NumberFormat getCurrencyInstance() {
        return getInstance(ULocale.getDefault(ULocale.Category.FORMAT), 1);
    }

    public static NumberFormat getCurrencyInstance(Locale locale) {
        return getInstance(ULocale.forLocale(locale), 1);
    }

    public static NumberFormat getCurrencyInstance(ULocale uLocale) {
        return getInstance(uLocale, 1);
    }

    public static final NumberFormat getPercentInstance() {
        return getInstance(ULocale.getDefault(ULocale.Category.FORMAT), 2);
    }

    public static NumberFormat getPercentInstance(Locale locale) {
        return getInstance(ULocale.forLocale(locale), 2);
    }

    public static NumberFormat getPercentInstance(ULocale uLocale) {
        return getInstance(uLocale, 2);
    }

    public static final NumberFormat getScientificInstance() {
        return getInstance(ULocale.getDefault(ULocale.Category.FORMAT), 3);
    }

    public static NumberFormat getScientificInstance(Locale locale) {
        return getInstance(ULocale.forLocale(locale), 3);
    }

    public static NumberFormat getScientificInstance(ULocale uLocale) {
        return getInstance(uLocale, 3);
    }

    public static abstract class NumberFormatFactory {
        public static final int FORMAT_CURRENCY = 1;
        public static final int FORMAT_INTEGER = 4;
        public static final int FORMAT_NUMBER = 0;
        public static final int FORMAT_PERCENT = 2;
        public static final int FORMAT_SCIENTIFIC = 3;

        public abstract Set<String> getSupportedLocaleNames();

        public boolean visible() {
            return true;
        }

        public NumberFormat createFormat(ULocale uLocale, int i) {
            return createFormat(uLocale.toLocale(), i);
        }

        public NumberFormat createFormat(Locale locale, int i) {
            return createFormat(ULocale.forLocale(locale), i);
        }

        protected NumberFormatFactory() {
        }
    }

    public static abstract class SimpleNumberFormatFactory extends NumberFormatFactory {
        final Set<String> localeNames;
        final boolean visible;

        public SimpleNumberFormatFactory(Locale locale) {
            this(locale, true);
        }

        public SimpleNumberFormatFactory(Locale locale, boolean z) {
            this.localeNames = Collections.singleton(ULocale.forLocale(locale).getBaseName());
            this.visible = z;
        }

        public SimpleNumberFormatFactory(ULocale uLocale) {
            this(uLocale, true);
        }

        public SimpleNumberFormatFactory(ULocale uLocale, boolean z) {
            this.localeNames = Collections.singleton(uLocale.getBaseName());
            this.visible = z;
        }

        @Override
        public final boolean visible() {
            return this.visible;
        }

        @Override
        public final Set<String> getSupportedLocaleNames() {
            return this.localeNames;
        }
    }

    static abstract class NumberFormatShim {
        abstract NumberFormat createInstance(ULocale uLocale, int i);

        abstract Locale[] getAvailableLocales();

        abstract ULocale[] getAvailableULocales();

        abstract Object registerFactory(NumberFormatFactory numberFormatFactory);

        abstract boolean unregister(Object obj);

        NumberFormatShim() {
        }
    }

    private static NumberFormatShim getShim() {
        if (shim == null) {
            try {
                shim = (NumberFormatShim) Class.forName("android.icu.text.NumberFormatServiceShim").newInstance();
            } catch (MissingResourceException e) {
                throw e;
            } catch (Exception e2) {
                throw new RuntimeException(e2.getMessage());
            }
        }
        return shim;
    }

    public static Locale[] getAvailableLocales() {
        if (shim == null) {
            return ICUResourceBundle.getAvailableLocales();
        }
        return getShim().getAvailableLocales();
    }

    public static ULocale[] getAvailableULocales() {
        if (shim == null) {
            return ICUResourceBundle.getAvailableULocales();
        }
        return getShim().getAvailableULocales();
    }

    public static Object registerFactory(NumberFormatFactory numberFormatFactory) {
        if (numberFormatFactory == null) {
            throw new IllegalArgumentException("factory must not be null");
        }
        return getShim().registerFactory(numberFormatFactory);
    }

    public static boolean unregister(Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException("registryKey must not be null");
        }
        if (shim == null) {
            return false;
        }
        return shim.unregister(obj);
    }

    public int hashCode() {
        return (this.maximumIntegerDigits * 37) + this.maxFractionDigits;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        NumberFormat numberFormat = (NumberFormat) obj;
        if (this.maximumIntegerDigits != numberFormat.maximumIntegerDigits || this.minimumIntegerDigits != numberFormat.minimumIntegerDigits || this.maximumFractionDigits != numberFormat.maximumFractionDigits || this.minimumFractionDigits != numberFormat.minimumFractionDigits || this.groupingUsed != numberFormat.groupingUsed || this.parseIntegerOnly != numberFormat.parseIntegerOnly || this.parseStrict != numberFormat.parseStrict || this.capitalizationSetting != numberFormat.capitalizationSetting) {
            return false;
        }
        return true;
    }

    @Override
    public Object clone() {
        return (NumberFormat) super.clone();
    }

    public boolean isGroupingUsed() {
        return this.groupingUsed;
    }

    public void setGroupingUsed(boolean z) {
        this.groupingUsed = z;
    }

    public int getMaximumIntegerDigits() {
        return this.maximumIntegerDigits;
    }

    public void setMaximumIntegerDigits(int i) {
        this.maximumIntegerDigits = Math.max(0, i);
        if (this.minimumIntegerDigits > this.maximumIntegerDigits) {
            this.minimumIntegerDigits = this.maximumIntegerDigits;
        }
    }

    public int getMinimumIntegerDigits() {
        return this.minimumIntegerDigits;
    }

    public void setMinimumIntegerDigits(int i) {
        this.minimumIntegerDigits = Math.max(0, i);
        if (this.minimumIntegerDigits > this.maximumIntegerDigits) {
            this.maximumIntegerDigits = this.minimumIntegerDigits;
        }
    }

    public int getMaximumFractionDigits() {
        return this.maximumFractionDigits;
    }

    public void setMaximumFractionDigits(int i) {
        this.maximumFractionDigits = Math.max(0, i);
        if (this.maximumFractionDigits < this.minimumFractionDigits) {
            this.minimumFractionDigits = this.maximumFractionDigits;
        }
    }

    public int getMinimumFractionDigits() {
        return this.minimumFractionDigits;
    }

    public void setMinimumFractionDigits(int i) {
        this.minimumFractionDigits = Math.max(0, i);
        if (this.maximumFractionDigits < this.minimumFractionDigits) {
            this.maximumFractionDigits = this.minimumFractionDigits;
        }
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public Currency getCurrency() {
        return this.currency;
    }

    @Deprecated
    protected Currency getEffectiveCurrency() {
        Currency currency = getCurrency();
        if (currency == null) {
            ULocale locale = getLocale(ULocale.VALID_LOCALE);
            if (locale == null) {
                locale = ULocale.getDefault(ULocale.Category.FORMAT);
            }
            return Currency.getInstance(locale);
        }
        return currency;
    }

    public int getRoundingMode() {
        throw new UnsupportedOperationException("getRoundingMode must be implemented by the subclass implementation.");
    }

    public void setRoundingMode(int i) {
        throw new UnsupportedOperationException("setRoundingMode must be implemented by the subclass implementation.");
    }

    public static NumberFormat getInstance(ULocale uLocale, int i) {
        if (i < 0 || i > 9) {
            throw new IllegalArgumentException("choice should be from NUMBERSTYLE to STANDARDCURRENCYSTYLE");
        }
        return getShim().createInstance(uLocale, i);
    }

    static NumberFormat createInstance(ULocale uLocale, int i) {
        String currencyPattern;
        NumberFormat numberFormat;
        String pattern = getPattern(uLocale, i);
        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(uLocale);
        if ((i == 1 || i == 5 || i == 7 || i == 8 || i == 9) && (currencyPattern = decimalFormatSymbols.getCurrencyPattern()) != null) {
            pattern = currencyPattern;
        }
        if (i == 5) {
            pattern = pattern.replace("¤", doubleCurrencyStr);
        }
        NumberingSystem numberingSystem = NumberingSystem.getInstance(uLocale);
        if (numberingSystem == null) {
            return null;
        }
        int i2 = 4;
        if (numberingSystem != null && numberingSystem.isAlgorithmic()) {
            String description = numberingSystem.getDescription();
            int iIndexOf = description.indexOf("/");
            int iLastIndexOf = description.lastIndexOf("/");
            if (iLastIndexOf > iIndexOf) {
                String strSubstring = description.substring(0, iIndexOf);
                String strSubstring2 = description.substring(iIndexOf + 1, iLastIndexOf);
                description = description.substring(iLastIndexOf + 1);
                ULocale uLocale2 = new ULocale(strSubstring);
                if (strSubstring2.equals("SpelloutRules")) {
                    i2 = 1;
                }
                uLocale = uLocale2;
            }
            RuleBasedNumberFormat ruleBasedNumberFormat = new RuleBasedNumberFormat(uLocale, i2);
            ruleBasedNumberFormat.setDefaultRuleSet(description);
            numberFormat = ruleBasedNumberFormat;
        } else {
            DecimalFormat decimalFormat = new DecimalFormat(pattern, decimalFormatSymbols, i);
            if (i == 4) {
                decimalFormat.setMaximumFractionDigits(0);
                decimalFormat.setDecimalSeparatorAlwaysShown(false);
                decimalFormat.setParseIntegerOnly(true);
            }
            if (i == 8) {
                decimalFormat.setCurrencyUsage(Currency.CurrencyUsage.CASH);
            }
            if (i == 6) {
                decimalFormat.setCurrencyPluralInfo(CurrencyPluralInfo.getInstance(uLocale));
            }
            numberFormat = decimalFormat;
        }
        numberFormat.setLocale(decimalFormatSymbols.getLocale(ULocale.VALID_LOCALE), decimalFormatSymbols.getLocale(ULocale.ACTUAL_LOCALE));
        return numberFormat;
    }

    @Deprecated
    protected static String getPattern(Locale locale, int i) {
        return getPattern(ULocale.forLocale(locale), i);
    }

    protected static String getPattern(ULocale uLocale, int i) {
        return getPatternForStyle(uLocale, i);
    }

    @Deprecated
    public static String getPatternForStyle(ULocale uLocale, int i) {
        return getPatternForStyleAndNumberingSystem(uLocale, NumberingSystem.getInstance(uLocale).getName(), i);
    }

    @Deprecated
    public static String getPatternForStyleAndNumberingSystem(ULocale uLocale, String str, int i) {
        String str2;
        switch (i) {
            case 0:
            case 4:
            case 6:
                str2 = "decimalFormat";
                break;
            case 1:
                String keywordValue = uLocale.getKeywordValue("cf");
                str2 = (keywordValue != null && keywordValue.equals("account")) ? "accountingFormat" : "currencyFormat";
                break;
            case 2:
                str2 = "percentFormat";
                break;
            case 3:
                str2 = "scientificFormat";
                break;
            case 5:
            case 8:
            case 9:
                str2 = "currencyFormat";
                break;
            case 7:
                str2 = "accountingFormat";
                break;
            default:
                str2 = "decimalFormat";
                break;
        }
        ICUResourceBundle iCUResourceBundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, uLocale);
        String strFindStringWithFallback = iCUResourceBundle.findStringWithFallback("NumberElements/" + str + "/patterns/" + str2);
        if (strFindStringWithFallback == null) {
            return iCUResourceBundle.getStringWithFallback("NumberElements/latn/patterns/" + str2);
        }
        return strFindStringWithFallback;
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        if (this.serialVersionOnStream < 1) {
            this.maximumIntegerDigits = this.maxIntegerDigits;
            this.minimumIntegerDigits = this.minIntegerDigits;
            this.maximumFractionDigits = this.maxFractionDigits;
            this.minimumFractionDigits = this.minFractionDigits;
        }
        if (this.serialVersionOnStream < 2) {
            this.capitalizationSetting = DisplayContext.CAPITALIZATION_NONE;
        }
        if (this.minimumIntegerDigits > this.maximumIntegerDigits || this.minimumFractionDigits > this.maximumFractionDigits || this.minimumIntegerDigits < 0 || this.minimumFractionDigits < 0) {
            throw new InvalidObjectException("Digit count range invalid");
        }
        this.serialVersionOnStream = 2;
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        int i = this.maximumIntegerDigits;
        byte b = Bidi.LEVEL_DEFAULT_RTL;
        this.maxIntegerDigits = i > 127 ? (byte) 127 : (byte) this.maximumIntegerDigits;
        this.minIntegerDigits = this.minimumIntegerDigits > 127 ? (byte) 127 : (byte) this.minimumIntegerDigits;
        this.maxFractionDigits = this.maximumFractionDigits > 127 ? (byte) 127 : (byte) this.maximumFractionDigits;
        if (this.minimumFractionDigits <= 127) {
            b = (byte) this.minimumFractionDigits;
        }
        this.minFractionDigits = b;
        objectOutputStream.defaultWriteObject();
    }

    public static class Field extends Format.Field {
        static final long serialVersionUID = -4516273749929385842L;
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

        protected Field(String str) {
            super(str);
        }

        @Override
        protected Object readResolve() throws InvalidObjectException {
            if (getName().equals(INTEGER.getName())) {
                return INTEGER;
            }
            if (getName().equals(FRACTION.getName())) {
                return FRACTION;
            }
            if (getName().equals(EXPONENT.getName())) {
                return EXPONENT;
            }
            if (getName().equals(EXPONENT_SIGN.getName())) {
                return EXPONENT_SIGN;
            }
            if (getName().equals(EXPONENT_SYMBOL.getName())) {
                return EXPONENT_SYMBOL;
            }
            if (getName().equals(CURRENCY.getName())) {
                return CURRENCY;
            }
            if (getName().equals(DECIMAL_SEPARATOR.getName())) {
                return DECIMAL_SEPARATOR;
            }
            if (getName().equals(GROUPING_SEPARATOR.getName())) {
                return GROUPING_SEPARATOR;
            }
            if (getName().equals(PERCENT.getName())) {
                return PERCENT;
            }
            if (getName().equals(PERMILLE.getName())) {
                return PERMILLE;
            }
            if (getName().equals(SIGN.getName())) {
                return SIGN;
            }
            throw new InvalidObjectException("An invalid object.");
        }
    }
}
