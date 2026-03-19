package android.icu.impl.number;

import android.icu.text.DecimalFormatSymbols;
import android.icu.util.Currency;
import android.icu.util.ULocale;

public class CustomSymbolCurrency extends Currency {
    private static final long serialVersionUID = 2497493016770137670L;
    private String symbol1;
    private String symbol2;

    public static Currency resolve(Currency currency, ULocale uLocale, DecimalFormatSymbols decimalFormatSymbols) {
        if (currency == null) {
            currency = decimalFormatSymbols.getCurrency();
        }
        String currencySymbol = decimalFormatSymbols.getCurrencySymbol();
        String internationalCurrencySymbol = decimalFormatSymbols.getInternationalCurrencySymbol();
        if (currency == null) {
            return new CustomSymbolCurrency("XXX", currencySymbol, internationalCurrencySymbol);
        }
        if (!currency.equals(decimalFormatSymbols.getCurrency())) {
            return currency;
        }
        String name = currency.getName(decimalFormatSymbols.getULocale(), 0, (boolean[]) null);
        String currencyCode = currency.getCurrencyCode();
        if (!name.equals(currencySymbol) || !currencyCode.equals(internationalCurrencySymbol)) {
            return new CustomSymbolCurrency(currencyCode, currencySymbol, internationalCurrencySymbol);
        }
        return currency;
    }

    public CustomSymbolCurrency(String str, String str2, String str3) {
        super(str);
        this.symbol1 = str2;
        this.symbol2 = str3;
    }

    @Override
    public String getName(ULocale uLocale, int i, boolean[] zArr) {
        if (i == 0) {
            return this.symbol1;
        }
        return super.getName(uLocale, i, zArr);
    }

    @Override
    public String getName(ULocale uLocale, int i, String str, boolean[] zArr) {
        if (i == 2 && this.subType.equals("XXX")) {
            return this.symbol1;
        }
        return super.getName(uLocale, i, str, zArr);
    }

    @Override
    public String getCurrencyCode() {
        return this.symbol2;
    }

    @Override
    public int hashCode() {
        return (super.hashCode() ^ this.symbol1.hashCode()) ^ this.symbol2.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            CustomSymbolCurrency customSymbolCurrency = (CustomSymbolCurrency) obj;
            if (customSymbolCurrency.symbol1.equals(this.symbol1) && customSymbolCurrency.symbol2.equals(this.symbol2)) {
                return true;
            }
        }
        return false;
    }
}
