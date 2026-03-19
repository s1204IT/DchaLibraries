package android.icu.text;

import android.icu.impl.CacheBase;
import android.icu.impl.CurrencyData;
import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.SoftCache;
import android.icu.impl.UResource;
import android.icu.util.Currency;
import android.icu.util.ICUCloneNotSupportedException;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Locale;
import java.util.MissingResourceException;

public class DecimalFormatSymbols implements Cloneable, Serializable {
    public static final int CURRENCY_SPC_CURRENCY_MATCH = 0;
    public static final int CURRENCY_SPC_INSERT = 2;
    public static final int CURRENCY_SPC_SURROUNDING_MATCH = 1;
    private static final String LATIN_NUMBERING_SYSTEM = "latn";
    private static final String NUMBER_ELEMENTS = "NumberElements";
    private static final String SYMBOLS = "symbols";
    private static final int currentSerialVersion = 8;
    private static final long serialVersionUID = 5772796243397350300L;
    private String NaN;
    private ULocale actualLocale;
    private transient int codePointZero;
    private transient Currency currency;
    private String currencyPattern;
    private String[] currencySpcAfterSym;
    private String[] currencySpcBeforeSym;
    private String currencySymbol;
    private char decimalSeparator;
    private String decimalSeparatorString;
    private char digit;
    private String[] digitStrings;
    private char[] digits;
    private String exponentMultiplicationSign;
    private String exponentSeparator;
    private char exponential;
    private char groupingSeparator;
    private String groupingSeparatorString;
    private String infinity;
    private String intlCurrencySymbol;
    private char minusSign;
    private String minusString;
    private char monetaryGroupingSeparator;
    private String monetaryGroupingSeparatorString;
    private char monetarySeparator;
    private String monetarySeparatorString;
    private char padEscape;
    private char patternSeparator;
    private char perMill;
    private String perMillString;
    private char percent;
    private String percentString;
    private char plusSign;
    private String plusString;
    private Locale requestedLocale;
    private int serialVersionOnStream;
    private char sigDigit;
    private ULocale ulocale;
    private ULocale validLocale;
    private char zeroDigit;
    private static final String[] SYMBOL_KEYS = {"decimal", "group", "list", "percentSign", "minusSign", "plusSign", "exponential", "perMille", "infinity", "nan", "currencyDecimal", "currencyGroup", "superscriptingExponent"};
    private static final String[] DEF_DIGIT_STRINGS_ARRAY = {AndroidHardcodedSystemProperties.JAVA_VERSION, "1", "2", "3", "4", "5", "6", "7", "8", "9"};
    private static final char[] DEF_DIGIT_CHARS_ARRAY = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    private static final char DEF_DECIMAL_SEPARATOR = '.';
    private static final char DEF_GROUPING_SEPARATOR = ',';
    private static final char DEF_PERCENT = '%';
    private static final char DEF_MINUS_SIGN = '-';
    private static final char DEF_PLUS_SIGN = '+';
    private static final char DEF_PERMILL = 8240;
    private static final String[] SYMBOL_DEFAULTS = {String.valueOf(DEF_DECIMAL_SEPARATOR), String.valueOf(DEF_GROUPING_SEPARATOR), ";", String.valueOf(DEF_PERCENT), String.valueOf(DEF_MINUS_SIGN), String.valueOf(DEF_PLUS_SIGN), DateFormat.ABBR_WEEKDAY, String.valueOf(DEF_PERMILL), "∞", "NaN", null, null, "×"};
    private static final CacheBase<ULocale, CacheData, Void> cachedLocaleData = new SoftCache<ULocale, CacheData, Void>() {
        @Override
        protected CacheData createInstance(ULocale uLocale, Void r2) {
            return DecimalFormatSymbols.loadData(uLocale);
        }
    };

    public DecimalFormatSymbols() {
        this(ULocale.getDefault(ULocale.Category.FORMAT));
    }

    public DecimalFormatSymbols(Locale locale) {
        this(ULocale.forLocale(locale));
    }

    public DecimalFormatSymbols(ULocale uLocale) {
        this.exponentMultiplicationSign = null;
        this.serialVersionOnStream = 8;
        this.currencyPattern = null;
        initialize(uLocale, null);
    }

    private DecimalFormatSymbols(Locale locale, NumberingSystem numberingSystem) {
        this(ULocale.forLocale(locale), numberingSystem);
    }

    private DecimalFormatSymbols(ULocale uLocale, NumberingSystem numberingSystem) {
        this.exponentMultiplicationSign = null;
        this.serialVersionOnStream = 8;
        this.currencyPattern = null;
        initialize(uLocale, numberingSystem);
    }

    public static DecimalFormatSymbols getInstance() {
        return new DecimalFormatSymbols();
    }

    public static DecimalFormatSymbols getInstance(Locale locale) {
        return new DecimalFormatSymbols(locale);
    }

    public static DecimalFormatSymbols getInstance(ULocale uLocale) {
        return new DecimalFormatSymbols(uLocale);
    }

    public static DecimalFormatSymbols forNumberingSystem(Locale locale, NumberingSystem numberingSystem) {
        return new DecimalFormatSymbols(locale, numberingSystem);
    }

    public static DecimalFormatSymbols forNumberingSystem(ULocale uLocale, NumberingSystem numberingSystem) {
        return new DecimalFormatSymbols(uLocale, numberingSystem);
    }

    public static Locale[] getAvailableLocales() {
        return ICUResourceBundle.getAvailableLocales();
    }

    public static ULocale[] getAvailableULocales() {
        return ICUResourceBundle.getAvailableULocales();
    }

    public char getZeroDigit() {
        return this.zeroDigit;
    }

    public char[] getDigits() {
        return (char[]) this.digits.clone();
    }

    public void setZeroDigit(char c) {
        this.zeroDigit = c;
        this.digitStrings = (String[]) this.digitStrings.clone();
        this.digits = (char[]) this.digits.clone();
        this.digitStrings[0] = String.valueOf(c);
        this.digits[0] = c;
        for (int i = 1; i < 10; i++) {
            char c2 = (char) (c + i);
            this.digitStrings[i] = String.valueOf(c2);
            this.digits[i] = c2;
        }
        this.codePointZero = c;
    }

    public String[] getDigitStrings() {
        return (String[]) this.digitStrings.clone();
    }

    @Deprecated
    public String[] getDigitStringsLocal() {
        return this.digitStrings;
    }

    @Deprecated
    public int getCodePointZero() {
        return this.codePointZero;
    }

    public void setDigitStrings(String[] strArr) {
        int iCodePointAt;
        int iCharCount;
        if (strArr == null) {
            throw new NullPointerException("The input digit string array is null");
        }
        if (strArr.length != 10) {
            throw new IllegalArgumentException("Number of digit strings is not 10");
        }
        String[] strArr2 = new String[10];
        char[] cArr = new char[10];
        int i = -1;
        for (int i2 = 0; i2 < 10; i2++) {
            String str = strArr[i2];
            if (str == null) {
                throw new IllegalArgumentException("The input digit string array contains a null element");
            }
            strArr2[i2] = str;
            if (str.length() == 0) {
                iCodePointAt = -1;
                iCharCount = 0;
            } else {
                iCodePointAt = Character.codePointAt(strArr[i2], 0);
                iCharCount = Character.charCount(iCodePointAt);
            }
            if (iCharCount == str.length()) {
                if (iCharCount != 1 || cArr == null) {
                    cArr = null;
                } else {
                    cArr[i2] = (char) iCodePointAt;
                }
                if (i2 == 0) {
                    i = iCodePointAt;
                } else if (iCodePointAt != i + i2) {
                    i = -1;
                }
            } else {
                i = -1;
                cArr = null;
            }
        }
        this.digitStrings = strArr2;
        this.codePointZero = i;
        if (cArr == null) {
            this.zeroDigit = DEF_DIGIT_CHARS_ARRAY[0];
            this.digits = DEF_DIGIT_CHARS_ARRAY;
        } else {
            this.zeroDigit = cArr[0];
            this.digits = cArr;
        }
    }

    public char getSignificantDigit() {
        return this.sigDigit;
    }

    public void setSignificantDigit(char c) {
        this.sigDigit = c;
    }

    public char getGroupingSeparator() {
        return this.groupingSeparator;
    }

    public void setGroupingSeparator(char c) {
        this.groupingSeparator = c;
        this.groupingSeparatorString = String.valueOf(c);
    }

    public String getGroupingSeparatorString() {
        return this.groupingSeparatorString;
    }

    public void setGroupingSeparatorString(String str) {
        if (str == null) {
            throw new NullPointerException("The input grouping separator is null");
        }
        this.groupingSeparatorString = str;
        if (str.length() == 1) {
            this.groupingSeparator = str.charAt(0);
        } else {
            this.groupingSeparator = DEF_GROUPING_SEPARATOR;
        }
    }

    public char getDecimalSeparator() {
        return this.decimalSeparator;
    }

    public void setDecimalSeparator(char c) {
        this.decimalSeparator = c;
        this.decimalSeparatorString = String.valueOf(c);
    }

    public String getDecimalSeparatorString() {
        return this.decimalSeparatorString;
    }

    public void setDecimalSeparatorString(String str) {
        if (str == null) {
            throw new NullPointerException("The input decimal separator is null");
        }
        this.decimalSeparatorString = str;
        if (str.length() == 1) {
            this.decimalSeparator = str.charAt(0);
        } else {
            this.decimalSeparator = DEF_DECIMAL_SEPARATOR;
        }
    }

    public char getPerMill() {
        return this.perMill;
    }

    public void setPerMill(char c) {
        this.perMill = c;
        this.perMillString = String.valueOf(c);
    }

    public String getPerMillString() {
        return this.perMillString;
    }

    public void setPerMillString(String str) {
        if (str == null) {
            throw new NullPointerException("The input permille string is null");
        }
        this.perMillString = str;
        if (str.length() == 1) {
            this.perMill = str.charAt(0);
        } else {
            this.perMill = DEF_PERMILL;
        }
    }

    public char getPercent() {
        return this.percent;
    }

    public void setPercent(char c) {
        this.percent = c;
        this.percentString = String.valueOf(c);
    }

    public String getPercentString() {
        return this.percentString;
    }

    public void setPercentString(String str) {
        if (str == null) {
            throw new NullPointerException("The input percent sign is null");
        }
        this.percentString = str;
        if (str.length() == 1) {
            this.percent = str.charAt(0);
        } else {
            this.percent = DEF_PERCENT;
        }
    }

    public char getDigit() {
        return this.digit;
    }

    public void setDigit(char c) {
        this.digit = c;
    }

    public char getPatternSeparator() {
        return this.patternSeparator;
    }

    public void setPatternSeparator(char c) {
        this.patternSeparator = c;
    }

    public String getInfinity() {
        return this.infinity;
    }

    public void setInfinity(String str) {
        this.infinity = str;
    }

    public String getNaN() {
        return this.NaN;
    }

    public void setNaN(String str) {
        this.NaN = str;
    }

    public char getMinusSign() {
        return this.minusSign;
    }

    public void setMinusSign(char c) {
        this.minusSign = c;
        this.minusString = String.valueOf(c);
    }

    public String getMinusSignString() {
        return this.minusString;
    }

    public void setMinusSignString(String str) {
        if (str == null) {
            throw new NullPointerException("The input minus sign is null");
        }
        this.minusString = str;
        if (str.length() == 1) {
            this.minusSign = str.charAt(0);
        } else {
            this.minusSign = DEF_MINUS_SIGN;
        }
    }

    public char getPlusSign() {
        return this.plusSign;
    }

    public void setPlusSign(char c) {
        this.plusSign = c;
        this.plusString = String.valueOf(c);
    }

    public String getPlusSignString() {
        return this.plusString;
    }

    public void setPlusSignString(String str) {
        if (str == null) {
            throw new NullPointerException("The input plus sign is null");
        }
        this.plusString = str;
        if (str.length() == 1) {
            this.plusSign = str.charAt(0);
        } else {
            this.plusSign = DEF_PLUS_SIGN;
        }
    }

    public String getCurrencySymbol() {
        return this.currencySymbol;
    }

    public void setCurrencySymbol(String str) {
        this.currencySymbol = str;
    }

    public String getInternationalCurrencySymbol() {
        return this.intlCurrencySymbol;
    }

    public void setInternationalCurrencySymbol(String str) {
        this.intlCurrencySymbol = str;
    }

    public Currency getCurrency() {
        return this.currency;
    }

    public void setCurrency(Currency currency) {
        if (currency == null) {
            throw new NullPointerException();
        }
        this.currency = currency;
        this.intlCurrencySymbol = currency.getCurrencyCode();
        this.currencySymbol = currency.getSymbol(this.requestedLocale);
    }

    public char getMonetaryDecimalSeparator() {
        return this.monetarySeparator;
    }

    public void setMonetaryDecimalSeparator(char c) {
        this.monetarySeparator = c;
        this.monetarySeparatorString = String.valueOf(c);
    }

    public String getMonetaryDecimalSeparatorString() {
        return this.monetarySeparatorString;
    }

    public void setMonetaryDecimalSeparatorString(String str) {
        if (str == null) {
            throw new NullPointerException("The input monetary decimal separator is null");
        }
        this.monetarySeparatorString = str;
        if (str.length() == 1) {
            this.monetarySeparator = str.charAt(0);
        } else {
            this.monetarySeparator = DEF_DECIMAL_SEPARATOR;
        }
    }

    public char getMonetaryGroupingSeparator() {
        return this.monetaryGroupingSeparator;
    }

    public void setMonetaryGroupingSeparator(char c) {
        this.monetaryGroupingSeparator = c;
        this.monetaryGroupingSeparatorString = String.valueOf(c);
    }

    public String getMonetaryGroupingSeparatorString() {
        return this.monetaryGroupingSeparatorString;
    }

    public void setMonetaryGroupingSeparatorString(String str) {
        if (str == null) {
            throw new NullPointerException("The input monetary grouping separator is null");
        }
        this.monetaryGroupingSeparatorString = str;
        if (str.length() == 1) {
            this.monetaryGroupingSeparator = str.charAt(0);
        } else {
            this.monetaryGroupingSeparator = DEF_GROUPING_SEPARATOR;
        }
    }

    String getCurrencyPattern() {
        return this.currencyPattern;
    }

    public String getExponentMultiplicationSign() {
        return this.exponentMultiplicationSign;
    }

    public void setExponentMultiplicationSign(String str) {
        this.exponentMultiplicationSign = str;
    }

    public String getExponentSeparator() {
        return this.exponentSeparator;
    }

    public void setExponentSeparator(String str) {
        this.exponentSeparator = str;
    }

    public char getPadEscape() {
        return this.padEscape;
    }

    public void setPadEscape(char c) {
        this.padEscape = c;
    }

    public String getPatternForCurrencySpacing(int i, boolean z) {
        if (i < 0 || i > 2) {
            throw new IllegalArgumentException("unknown currency spacing: " + i);
        }
        if (z) {
            return this.currencySpcBeforeSym[i];
        }
        return this.currencySpcAfterSym[i];
    }

    public void setPatternForCurrencySpacing(int i, boolean z, String str) {
        if (i < 0 || i > 2) {
            throw new IllegalArgumentException("unknown currency spacing: " + i);
        }
        if (z) {
            this.currencySpcBeforeSym[i] = str;
        } else {
            this.currencySpcAfterSym[i] = str;
        }
    }

    public Locale getLocale() {
        return this.requestedLocale;
    }

    public ULocale getULocale() {
        return this.ulocale;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException(e);
        }
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof DecimalFormatSymbols)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        DecimalFormatSymbols decimalFormatSymbols = (DecimalFormatSymbols) obj;
        for (int i = 0; i <= 2; i++) {
            if (!this.currencySpcBeforeSym[i].equals(decimalFormatSymbols.currencySpcBeforeSym[i]) || !this.currencySpcAfterSym[i].equals(decimalFormatSymbols.currencySpcAfterSym[i])) {
                return false;
            }
        }
        if (decimalFormatSymbols.digits == null) {
            for (int i2 = 0; i2 < 10; i2++) {
                if (this.digits[i2] != decimalFormatSymbols.zeroDigit + i2) {
                    return false;
                }
            }
        } else if (!Arrays.equals(this.digits, decimalFormatSymbols.digits)) {
            return false;
        }
        return this.groupingSeparator == decimalFormatSymbols.groupingSeparator && this.decimalSeparator == decimalFormatSymbols.decimalSeparator && this.percent == decimalFormatSymbols.percent && this.perMill == decimalFormatSymbols.perMill && this.digit == decimalFormatSymbols.digit && this.minusSign == decimalFormatSymbols.minusSign && this.minusString.equals(decimalFormatSymbols.minusString) && this.patternSeparator == decimalFormatSymbols.patternSeparator && this.infinity.equals(decimalFormatSymbols.infinity) && this.NaN.equals(decimalFormatSymbols.NaN) && this.currencySymbol.equals(decimalFormatSymbols.currencySymbol) && this.intlCurrencySymbol.equals(decimalFormatSymbols.intlCurrencySymbol) && this.padEscape == decimalFormatSymbols.padEscape && this.plusSign == decimalFormatSymbols.plusSign && this.plusString.equals(decimalFormatSymbols.plusString) && this.exponentSeparator.equals(decimalFormatSymbols.exponentSeparator) && this.monetarySeparator == decimalFormatSymbols.monetarySeparator && this.monetaryGroupingSeparator == decimalFormatSymbols.monetaryGroupingSeparator && this.exponentMultiplicationSign.equals(decimalFormatSymbols.exponentMultiplicationSign);
    }

    public int hashCode() {
        return (((this.digits[0] * DEF_PERCENT) + this.groupingSeparator) * 37) + this.decimalSeparator;
    }

    private static final class DecFmtDataSink extends UResource.Sink {
        private String[] numberElements;

        public DecFmtDataSink(String[] strArr) {
            this.numberElements = strArr;
        }

        @Override
        public void put(UResource.Key key, UResource.Value value, boolean z) {
            UResource.Table table = value.getTable();
            for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                int i2 = 0;
                while (true) {
                    if (i2 >= DecimalFormatSymbols.SYMBOL_KEYS.length) {
                        break;
                    }
                    if (!key.contentEquals(DecimalFormatSymbols.SYMBOL_KEYS[i2])) {
                        i2++;
                    } else if (this.numberElements[i2] == null) {
                        this.numberElements[i2] = value.toString();
                    }
                }
            }
        }
    }

    private void initialize(ULocale uLocale, NumberingSystem numberingSystem) {
        ULocale keywordValue;
        this.requestedLocale = uLocale.toLocale();
        this.ulocale = uLocale;
        if (numberingSystem != null) {
            keywordValue = uLocale.setKeywordValue("numbers", numberingSystem.getName());
        } else {
            keywordValue = uLocale;
        }
        CacheData cacheBase = cachedLocaleData.getInstance(keywordValue, null);
        setLocale(cacheBase.validLocale, cacheBase.validLocale);
        setDigitStrings(cacheBase.digits);
        String[] strArr = cacheBase.numberElements;
        setDecimalSeparatorString(strArr[0]);
        setGroupingSeparatorString(strArr[1]);
        this.patternSeparator = strArr[2].charAt(0);
        setPercentString(strArr[3]);
        setMinusSignString(strArr[4]);
        setPlusSignString(strArr[5]);
        setExponentSeparator(strArr[6]);
        setPerMillString(strArr[7]);
        setInfinity(strArr[8]);
        setNaN(strArr[9]);
        setMonetaryDecimalSeparatorString(strArr[10]);
        setMonetaryGroupingSeparatorString(strArr[11]);
        setExponentMultiplicationSign(strArr[12]);
        this.digit = '#';
        this.padEscape = '*';
        this.sigDigit = '@';
        CurrencyData.CurrencyDisplayInfo currencyDisplayInfoProvider = CurrencyData.provider.getInstance(uLocale, true);
        this.currency = Currency.getInstance(uLocale);
        if (this.currency != null) {
            this.intlCurrencySymbol = this.currency.getCurrencyCode();
            this.currencySymbol = this.currency.getName(uLocale, 0, (boolean[]) null);
            CurrencyData.CurrencyFormatInfo formatInfo = currencyDisplayInfoProvider.getFormatInfo(this.intlCurrencySymbol);
            if (formatInfo != null) {
                this.currencyPattern = formatInfo.currencyPattern;
                setMonetaryDecimalSeparatorString(formatInfo.monetaryDecimalSeparator);
                setMonetaryGroupingSeparatorString(formatInfo.monetaryGroupingSeparator);
            }
        } else {
            this.intlCurrencySymbol = "XXX";
            this.currencySymbol = "¤";
        }
        initSpacingInfo(currencyDisplayInfoProvider.getSpacingInfo());
    }

    private static CacheData loadData(ULocale uLocale) {
        String name;
        boolean z;
        NumberingSystem numberingSystem = NumberingSystem.getInstance(uLocale);
        String[] strArr = new String[10];
        if (numberingSystem != null && numberingSystem.getRadix() == 10 && !numberingSystem.isAlgorithmic() && NumberingSystem.isValidDigitString(numberingSystem.getDescription())) {
            String description = numberingSystem.getDescription();
            int i = 0;
            int i2 = 0;
            while (i < 10) {
                int iCharCount = Character.charCount(description.codePointAt(i2)) + i2;
                strArr[i] = description.substring(i2, iCharCount);
                i++;
                i2 = iCharCount;
            }
            name = numberingSystem.getName();
        } else {
            strArr = DEF_DIGIT_STRINGS_ARRAY;
            name = LATIN_NUMBERING_SYSTEM;
        }
        ICUResourceBundle iCUResourceBundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, uLocale);
        ULocale uLocale2 = iCUResourceBundle.getULocale();
        String[] strArr2 = new String[SYMBOL_KEYS.length];
        DecFmtDataSink decFmtDataSink = new DecFmtDataSink(strArr2);
        try {
            iCUResourceBundle.getAllItemsWithFallback("NumberElements/" + name + "/" + SYMBOLS, decFmtDataSink);
        } catch (MissingResourceException e) {
        }
        int length = strArr2.length;
        int i3 = 0;
        while (true) {
            if (i3 < length) {
                if (strArr2[i3] != null) {
                    i3++;
                } else {
                    z = true;
                    break;
                }
            } else {
                z = false;
                break;
            }
        }
        if (z && !name.equals(LATIN_NUMBERING_SYSTEM)) {
            iCUResourceBundle.getAllItemsWithFallback("NumberElements/latn/symbols", decFmtDataSink);
        }
        for (int i4 = 0; i4 < SYMBOL_KEYS.length; i4++) {
            if (strArr2[i4] == null) {
                strArr2[i4] = SYMBOL_DEFAULTS[i4];
            }
        }
        if (strArr2[10] == null) {
            strArr2[10] = strArr2[0];
        }
        if (strArr2[11] == null) {
            strArr2[11] = strArr2[1];
        }
        return new CacheData(uLocale2, strArr, strArr2);
    }

    private void initSpacingInfo(CurrencyData.CurrencySpacingInfo currencySpacingInfo) {
        this.currencySpcBeforeSym = currencySpacingInfo.getBeforeSymbols();
        this.currencySpcAfterSym = currencySpacingInfo.getAfterSymbols();
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        if (this.serialVersionOnStream < 1) {
            this.monetarySeparator = this.decimalSeparator;
            this.exponential = 'E';
        }
        if (this.serialVersionOnStream < 2) {
            this.padEscape = '*';
            this.plusSign = DEF_PLUS_SIGN;
            this.exponentSeparator = String.valueOf(this.exponential);
        }
        if (this.serialVersionOnStream < 3) {
            this.requestedLocale = Locale.getDefault();
        }
        if (this.serialVersionOnStream < 4) {
            this.ulocale = ULocale.forLocale(this.requestedLocale);
        }
        if (this.serialVersionOnStream < 5) {
            this.monetaryGroupingSeparator = this.groupingSeparator;
        }
        if (this.serialVersionOnStream < 6) {
            if (this.currencySpcBeforeSym == null) {
                this.currencySpcBeforeSym = new String[3];
            }
            if (this.currencySpcAfterSym == null) {
                this.currencySpcAfterSym = new String[3];
            }
            initSpacingInfo(CurrencyData.CurrencySpacingInfo.DEFAULT);
        }
        if (this.serialVersionOnStream < 7) {
            if (this.minusString == null) {
                this.minusString = String.valueOf(this.minusSign);
            }
            if (this.plusString == null) {
                this.plusString = String.valueOf(this.plusSign);
            }
        }
        if (this.serialVersionOnStream < 8 && this.exponentMultiplicationSign == null) {
            this.exponentMultiplicationSign = "×";
        }
        if (this.serialVersionOnStream < 9) {
            if (this.digitStrings == null) {
                this.digitStrings = new String[10];
                int i = 0;
                if (this.digits != null && this.digits.length == 10) {
                    this.zeroDigit = this.digits[0];
                    while (i < 10) {
                        this.digitStrings[i] = String.valueOf(this.digits[i]);
                        i++;
                    }
                } else {
                    char c = this.zeroDigit;
                    if (this.digits == null) {
                        this.digits = new char[10];
                    }
                    while (i < 10) {
                        this.digits[i] = c;
                        this.digitStrings[i] = String.valueOf(c);
                        c = (char) (c + 1);
                        i++;
                    }
                }
            }
            if (this.decimalSeparatorString == null) {
                this.decimalSeparatorString = String.valueOf(this.decimalSeparator);
            }
            if (this.groupingSeparatorString == null) {
                this.groupingSeparatorString = String.valueOf(this.groupingSeparator);
            }
            if (this.percentString == null) {
                this.percentString = String.valueOf(this.percent);
            }
            if (this.perMillString == null) {
                this.perMillString = String.valueOf(this.perMill);
            }
            if (this.monetarySeparatorString == null) {
                this.monetarySeparatorString = String.valueOf(this.monetarySeparator);
            }
            if (this.monetaryGroupingSeparatorString == null) {
                this.monetaryGroupingSeparatorString = String.valueOf(this.monetaryGroupingSeparator);
            }
        }
        this.serialVersionOnStream = 8;
        this.currency = Currency.getInstance(this.intlCurrencySymbol);
        setDigitStrings(this.digitStrings);
    }

    public final ULocale getLocale(ULocale.Type type) {
        return type == ULocale.ACTUAL_LOCALE ? this.actualLocale : this.validLocale;
    }

    final void setLocale(ULocale uLocale, ULocale uLocale2) {
        if ((uLocale == null) != (uLocale2 == null)) {
            throw new IllegalArgumentException();
        }
        this.validLocale = uLocale;
        this.actualLocale = uLocale2;
    }

    private static class CacheData {
        final String[] digits;
        final String[] numberElements;
        final ULocale validLocale;

        public CacheData(ULocale uLocale, String[] strArr, String[] strArr2) {
            this.validLocale = uLocale;
            this.digits = strArr;
            this.numberElements = strArr2;
        }
    }
}
