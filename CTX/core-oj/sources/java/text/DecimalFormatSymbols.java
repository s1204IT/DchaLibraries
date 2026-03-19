package java.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.util.Currency;
import java.util.Locale;
import libcore.icu.ICU;
import libcore.icu.LocaleData;

public class DecimalFormatSymbols implements Cloneable, Serializable {
    private static final int currentSerialVersion = 3;
    private static final ObjectStreamField[] serialPersistentFields = {new ObjectStreamField("currencySymbol", String.class), new ObjectStreamField("decimalSeparator", Character.TYPE), new ObjectStreamField("digit", Character.TYPE), new ObjectStreamField("exponential", Character.TYPE), new ObjectStreamField("exponentialSeparator", String.class), new ObjectStreamField("groupingSeparator", Character.TYPE), new ObjectStreamField("infinity", String.class), new ObjectStreamField("intlCurrencySymbol", String.class), new ObjectStreamField("minusSign", Character.TYPE), new ObjectStreamField("monetarySeparator", Character.TYPE), new ObjectStreamField("NaN", String.class), new ObjectStreamField("patternSeparator", Character.TYPE), new ObjectStreamField("percent", Character.TYPE), new ObjectStreamField("perMill", Character.TYPE), new ObjectStreamField("serialVersionOnStream", Integer.TYPE), new ObjectStreamField("zeroDigit", Character.TYPE), new ObjectStreamField("locale", Locale.class), new ObjectStreamField("minusSignStr", String.class), new ObjectStreamField("percentStr", String.class)};
    static final long serialVersionUID = 5772796243397350300L;
    private String NaN;
    private transient Currency currency;
    private String currencySymbol;
    private char decimalSeparator;
    private char digit;
    private char exponential;
    private String exponentialSeparator;
    private char groupingSeparator;
    private String infinity;
    private String intlCurrencySymbol;
    private Locale locale;
    private char minusSign;
    private char monetarySeparator;
    private char patternSeparator;
    private char perMill;
    private char percent;
    private char zeroDigit;
    private int serialVersionOnStream = 3;
    private transient android.icu.text.DecimalFormatSymbols cachedIcuDFS = null;

    public DecimalFormatSymbols() {
        initialize(Locale.getDefault(Locale.Category.FORMAT));
    }

    public DecimalFormatSymbols(Locale locale) {
        initialize(locale);
    }

    public static Locale[] getAvailableLocales() {
        return ICU.getAvailableLocales();
    }

    public static final DecimalFormatSymbols getInstance() {
        return getInstance(Locale.getDefault(Locale.Category.FORMAT));
    }

    public static final DecimalFormatSymbols getInstance(Locale locale) {
        return new DecimalFormatSymbols(locale);
    }

    public char getZeroDigit() {
        return this.zeroDigit;
    }

    public void setZeroDigit(char c) {
        this.zeroDigit = c;
        this.cachedIcuDFS = null;
    }

    public char getGroupingSeparator() {
        return this.groupingSeparator;
    }

    public void setGroupingSeparator(char c) {
        this.groupingSeparator = c;
        this.cachedIcuDFS = null;
    }

    public char getDecimalSeparator() {
        return this.decimalSeparator;
    }

    public void setDecimalSeparator(char c) {
        this.decimalSeparator = c;
        this.cachedIcuDFS = null;
    }

    public char getPerMill() {
        return this.perMill;
    }

    public void setPerMill(char c) {
        this.perMill = c;
        this.cachedIcuDFS = null;
    }

    public char getPercent() {
        return this.percent;
    }

    public String getPercentString() {
        return String.valueOf(this.percent);
    }

    public void setPercent(char c) {
        this.percent = c;
        this.cachedIcuDFS = null;
    }

    public char getDigit() {
        return this.digit;
    }

    public void setDigit(char c) {
        this.digit = c;
        this.cachedIcuDFS = null;
    }

    public char getPatternSeparator() {
        return this.patternSeparator;
    }

    public void setPatternSeparator(char c) {
        this.patternSeparator = c;
        this.cachedIcuDFS = null;
    }

    public String getInfinity() {
        return this.infinity;
    }

    public void setInfinity(String str) {
        this.infinity = str;
        this.cachedIcuDFS = null;
    }

    public String getNaN() {
        return this.NaN;
    }

    public void setNaN(String str) {
        this.NaN = str;
        this.cachedIcuDFS = null;
    }

    public char getMinusSign() {
        return this.minusSign;
    }

    public String getMinusSignString() {
        return String.valueOf(this.minusSign);
    }

    public void setMinusSign(char c) {
        this.minusSign = c;
        this.cachedIcuDFS = null;
    }

    public String getCurrencySymbol() {
        return this.currencySymbol;
    }

    public void setCurrencySymbol(String str) {
        this.currencySymbol = str;
        this.cachedIcuDFS = null;
    }

    public String getInternationalCurrencySymbol() {
        return this.intlCurrencySymbol;
    }

    public void setInternationalCurrencySymbol(String str) {
        this.intlCurrencySymbol = str;
        this.currency = null;
        if (str != null) {
            try {
                this.currency = Currency.getInstance(str);
                this.currencySymbol = this.currency.getSymbol(this.locale);
            } catch (IllegalArgumentException e) {
            }
        }
        this.cachedIcuDFS = null;
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
        this.currencySymbol = currency.getSymbol(this.locale);
        this.cachedIcuDFS = null;
    }

    public char getMonetaryDecimalSeparator() {
        return this.monetarySeparator;
    }

    public void setMonetaryDecimalSeparator(char c) {
        this.monetarySeparator = c;
        this.cachedIcuDFS = null;
    }

    char getExponentialSymbol() {
        return this.exponential;
    }

    public String getExponentSeparator() {
        return this.exponentialSeparator;
    }

    void setExponentialSymbol(char c) {
        this.exponential = c;
        this.cachedIcuDFS = null;
    }

    public void setExponentSeparator(String str) {
        if (str == null) {
            throw new NullPointerException();
        }
        this.exponentialSeparator = str;
    }

    public Object clone() {
        try {
            return (DecimalFormatSymbols) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
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
        DecimalFormatSymbols decimalFormatSymbols = (DecimalFormatSymbols) obj;
        if (this.zeroDigit != decimalFormatSymbols.zeroDigit || this.groupingSeparator != decimalFormatSymbols.groupingSeparator || this.decimalSeparator != decimalFormatSymbols.decimalSeparator || this.percent != decimalFormatSymbols.percent || this.perMill != decimalFormatSymbols.perMill || this.digit != decimalFormatSymbols.digit || this.minusSign != decimalFormatSymbols.minusSign || this.patternSeparator != decimalFormatSymbols.patternSeparator || !this.infinity.equals(decimalFormatSymbols.infinity) || !this.NaN.equals(decimalFormatSymbols.NaN) || !this.currencySymbol.equals(decimalFormatSymbols.currencySymbol) || !this.intlCurrencySymbol.equals(decimalFormatSymbols.intlCurrencySymbol) || this.currency != decimalFormatSymbols.currency || this.monetarySeparator != decimalFormatSymbols.monetarySeparator || !this.exponentialSeparator.equals(decimalFormatSymbols.exponentialSeparator) || !this.locale.equals(decimalFormatSymbols.locale)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return (((((((((((((((((((((((((((((this.zeroDigit * '%') + this.groupingSeparator) * 37) + this.decimalSeparator) * 37) + this.percent) * 37) + this.perMill) * 37) + this.digit) * 37) + this.minusSign) * 37) + this.patternSeparator) * 37) + this.infinity.hashCode()) * 37) + this.NaN.hashCode()) * 37) + this.currencySymbol.hashCode()) * 37) + this.intlCurrencySymbol.hashCode()) * 37) + this.currency.hashCode()) * 37) + this.monetarySeparator) * 37) + this.exponentialSeparator.hashCode()) * 37) + this.locale.hashCode();
    }

    private void initialize(Locale locale) {
        this.locale = locale;
        if (locale == null) {
            throw new NullPointerException("locale");
        }
        Locale localeMapInvalidAndNullLocales = LocaleData.mapInvalidAndNullLocales(locale);
        LocaleData localeData = LocaleData.get(localeMapInvalidAndNullLocales);
        Object[] objArr = new Object[3];
        objArr[0] = new String[]{String.valueOf(localeData.decimalSeparator), String.valueOf(localeData.groupingSeparator), String.valueOf(localeData.patternSeparator), localeData.percent, String.valueOf(localeData.zeroDigit), "#", localeData.minusSign, localeData.exponentSeparator, localeData.perMill, localeData.infinity, localeData.NaN};
        String[] strArr = (String[]) objArr[0];
        this.decimalSeparator = strArr[0].charAt(0);
        this.groupingSeparator = strArr[1].charAt(0);
        this.patternSeparator = strArr[2].charAt(0);
        this.percent = maybeStripMarkers(strArr[3], '%');
        this.zeroDigit = strArr[4].charAt(0);
        this.digit = strArr[5].charAt(0);
        this.minusSign = maybeStripMarkers(strArr[6], '-');
        this.exponential = strArr[7].charAt(0);
        this.exponentialSeparator = strArr[7];
        this.perMill = maybeStripMarkers(strArr[8], (char) 8240);
        this.infinity = strArr[9];
        this.NaN = strArr[10];
        if (localeMapInvalidAndNullLocales.getCountry().length() > 0) {
            try {
                this.currency = Currency.getInstance(localeMapInvalidAndNullLocales);
            } catch (IllegalArgumentException e) {
            }
        }
        if (this.currency != null) {
            this.intlCurrencySymbol = this.currency.getCurrencyCode();
            if (objArr[1] != null && objArr[1] == this.intlCurrencySymbol) {
                this.currencySymbol = (String) objArr[2];
            } else {
                this.currencySymbol = this.currency.getSymbol(localeMapInvalidAndNullLocales);
                objArr[1] = this.intlCurrencySymbol;
                objArr[2] = this.currencySymbol;
            }
        } else {
            this.intlCurrencySymbol = "XXX";
            try {
                this.currency = Currency.getInstance(this.intlCurrencySymbol);
            } catch (IllegalArgumentException e2) {
            }
            this.currencySymbol = "¤";
        }
        this.monetarySeparator = this.decimalSeparator;
    }

    public static char maybeStripMarkers(String str, char c) {
        int length = str.length();
        if (length >= 1) {
            boolean z = false;
            char c2 = 0;
            for (int i = 0; i < length; i++) {
                char cCharAt = str.charAt(i);
                if (cCharAt != 8206 && cCharAt != 8207 && cCharAt != 1564) {
                    if (!z) {
                        z = true;
                        c2 = cCharAt;
                    } else {
                        return c;
                    }
                }
            }
            if (z) {
                return c2;
            }
        }
        return c;
    }

    protected android.icu.text.DecimalFormatSymbols getIcuDecimalFormatSymbols() {
        if (this.cachedIcuDFS != null) {
            return this.cachedIcuDFS;
        }
        this.cachedIcuDFS = new android.icu.text.DecimalFormatSymbols(this.locale);
        this.cachedIcuDFS.setPlusSign('+');
        this.cachedIcuDFS.setZeroDigit(this.zeroDigit);
        this.cachedIcuDFS.setDigit(this.digit);
        this.cachedIcuDFS.setDecimalSeparator(this.decimalSeparator);
        this.cachedIcuDFS.setGroupingSeparator(this.groupingSeparator);
        this.cachedIcuDFS.setMonetaryGroupingSeparator(this.groupingSeparator);
        this.cachedIcuDFS.setPatternSeparator(this.patternSeparator);
        this.cachedIcuDFS.setPercent(this.percent);
        this.cachedIcuDFS.setPerMill(this.perMill);
        this.cachedIcuDFS.setMonetaryDecimalSeparator(this.monetarySeparator);
        this.cachedIcuDFS.setMinusSign(this.minusSign);
        this.cachedIcuDFS.setInfinity(this.infinity);
        this.cachedIcuDFS.setNaN(this.NaN);
        this.cachedIcuDFS.setExponentSeparator(this.exponentialSeparator);
        try {
            this.cachedIcuDFS.setCurrency(android.icu.util.Currency.getInstance(this.currency.getCurrencyCode()));
        } catch (NullPointerException e) {
            this.currency = Currency.getInstance("XXX");
        }
        this.cachedIcuDFS.setCurrencySymbol(this.currencySymbol);
        this.cachedIcuDFS.setInternationalCurrencySymbol(this.intlCurrencySymbol);
        return this.cachedIcuDFS;
    }

    protected static DecimalFormatSymbols fromIcuInstance(android.icu.text.DecimalFormatSymbols decimalFormatSymbols) {
        DecimalFormatSymbols decimalFormatSymbols2 = new DecimalFormatSymbols(decimalFormatSymbols.getLocale());
        decimalFormatSymbols2.setZeroDigit(decimalFormatSymbols.getZeroDigit());
        decimalFormatSymbols2.setDigit(decimalFormatSymbols.getDigit());
        decimalFormatSymbols2.setDecimalSeparator(decimalFormatSymbols.getDecimalSeparator());
        decimalFormatSymbols2.setGroupingSeparator(decimalFormatSymbols.getGroupingSeparator());
        decimalFormatSymbols2.setPatternSeparator(decimalFormatSymbols.getPatternSeparator());
        decimalFormatSymbols2.setPercent(decimalFormatSymbols.getPercent());
        decimalFormatSymbols2.setPerMill(decimalFormatSymbols.getPerMill());
        decimalFormatSymbols2.setMonetaryDecimalSeparator(decimalFormatSymbols.getMonetaryDecimalSeparator());
        decimalFormatSymbols2.setMinusSign(decimalFormatSymbols.getMinusSign());
        decimalFormatSymbols2.setInfinity(decimalFormatSymbols.getInfinity());
        decimalFormatSymbols2.setNaN(decimalFormatSymbols.getNaN());
        decimalFormatSymbols2.setExponentSeparator(decimalFormatSymbols.getExponentSeparator());
        try {
            if (decimalFormatSymbols.getCurrency() != null) {
                decimalFormatSymbols2.setCurrency(Currency.getInstance(decimalFormatSymbols.getCurrency().getCurrencyCode()));
            } else {
                decimalFormatSymbols2.setCurrency(Currency.getInstance("XXX"));
            }
        } catch (IllegalArgumentException e) {
            decimalFormatSymbols2.setCurrency(Currency.getInstance("XXX"));
        }
        decimalFormatSymbols2.setInternationalCurrencySymbol(decimalFormatSymbols.getInternationalCurrencySymbol());
        decimalFormatSymbols2.setCurrencySymbol(decimalFormatSymbols.getCurrencySymbol());
        return decimalFormatSymbols2;
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        ObjectOutputStream.PutField putFieldPutFields = objectOutputStream.putFields();
        putFieldPutFields.put("currencySymbol", this.currencySymbol);
        putFieldPutFields.put("decimalSeparator", getDecimalSeparator());
        putFieldPutFields.put("digit", getDigit());
        putFieldPutFields.put("exponential", this.exponentialSeparator.charAt(0));
        putFieldPutFields.put("exponentialSeparator", this.exponentialSeparator);
        putFieldPutFields.put("groupingSeparator", getGroupingSeparator());
        putFieldPutFields.put("infinity", this.infinity);
        putFieldPutFields.put("intlCurrencySymbol", this.intlCurrencySymbol);
        putFieldPutFields.put("monetarySeparator", getMonetaryDecimalSeparator());
        putFieldPutFields.put("NaN", this.NaN);
        putFieldPutFields.put("patternSeparator", getPatternSeparator());
        putFieldPutFields.put("perMill", getPerMill());
        putFieldPutFields.put("serialVersionOnStream", 3);
        putFieldPutFields.put("zeroDigit", getZeroDigit());
        putFieldPutFields.put("locale", this.locale);
        putFieldPutFields.put("minusSign", this.minusSign);
        putFieldPutFields.put("percent", this.percent);
        putFieldPutFields.put("minusSignStr", getMinusSignString());
        putFieldPutFields.put("percentStr", getPercentString());
        objectOutputStream.writeFields();
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = objectInputStream.readFields();
        int i = fields.get("serialVersionOnStream", 0);
        this.currencySymbol = (String) fields.get("currencySymbol", "");
        setDecimalSeparator(fields.get("decimalSeparator", '.'));
        setDigit(fields.get("digit", '#'));
        setGroupingSeparator(fields.get("groupingSeparator", ','));
        this.infinity = (String) fields.get("infinity", "");
        this.intlCurrencySymbol = (String) fields.get("intlCurrencySymbol", "");
        this.NaN = (String) fields.get("NaN", "");
        setPatternSeparator(fields.get("patternSeparator", ';'));
        String str = (String) fields.get("minusSignStr", (Object) null);
        if (str != null) {
            this.minusSign = str.charAt(0);
        } else {
            setMinusSign(fields.get("minusSign", '-'));
        }
        String str2 = (String) fields.get("percentStr", (Object) null);
        if (str2 != null) {
            this.percent = str2.charAt(0);
        } else {
            setPercent(fields.get("percent", '%'));
        }
        setPerMill(fields.get("perMill", (char) 8240));
        setZeroDigit(fields.get("zeroDigit", '0'));
        this.locale = (Locale) fields.get("locale", (Object) null);
        if (i == 0) {
            setMonetaryDecimalSeparator(getDecimalSeparator());
        } else {
            setMonetaryDecimalSeparator(fields.get("monetarySeparator", '.'));
        }
        if (i == 0) {
            this.exponentialSeparator = "E";
        } else if (i < 3) {
            setExponentSeparator(String.valueOf(fields.get("exponential", 'E')));
        } else {
            setExponentSeparator((String) fields.get("exponentialSeparator", "E"));
        }
        try {
            this.currency = Currency.getInstance(this.intlCurrencySymbol);
        } catch (IllegalArgumentException e) {
            this.currency = null;
        }
    }
}
