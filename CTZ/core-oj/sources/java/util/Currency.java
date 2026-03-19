package java.util;

import java.io.Serializable;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import libcore.icu.ICU;
import sun.util.locale.BaseLocale;

public final class Currency implements Serializable {
    private static HashSet<Currency> available = null;
    private static ConcurrentMap<String, Currency> instances = new ConcurrentHashMap(7);
    private static final long serialVersionUID = -158308464356906721L;
    private final String currencyCode;
    private final transient android.icu.util.Currency icuCurrency;

    private Currency(android.icu.util.Currency currency) {
        this.icuCurrency = currency;
        this.currencyCode = currency.getCurrencyCode();
    }

    public static Currency getInstance(String str) {
        Currency currency = instances.get(str);
        if (currency != null) {
            return currency;
        }
        android.icu.util.Currency currency2 = android.icu.util.Currency.getInstance(str);
        if (currency2 == null) {
            return null;
        }
        Currency currency3 = new Currency(currency2);
        Currency currencyPutIfAbsent = instances.putIfAbsent(str, currency3);
        return currencyPutIfAbsent != null ? currencyPutIfAbsent : currency3;
    }

    public static Currency getInstance(Locale locale) {
        android.icu.util.Currency currency = android.icu.util.Currency.getInstance(locale);
        String variant = locale.getVariant();
        String country = locale.getCountry();
        if (!variant.isEmpty() && (variant.equals("EURO") || variant.equals("HK") || variant.equals("PREEURO"))) {
            country = country + BaseLocale.SEP + variant;
        }
        String currencyCode = ICU.getCurrencyCode(country);
        if (currencyCode == null) {
            throw new IllegalArgumentException("Unsupported ISO 3166 country: " + ((Object) locale));
        }
        if (currency == null || currency.getCurrencyCode().equals("XXX")) {
            return null;
        }
        return getInstance(currencyCode);
    }

    public static Set<Currency> getAvailableCurrencies() {
        synchronized (Currency.class) {
            if (available == null) {
                Set<android.icu.util.Currency> availableCurrencies = android.icu.util.Currency.getAvailableCurrencies();
                available = new HashSet<>();
                for (android.icu.util.Currency currency : availableCurrencies) {
                    Currency currency2 = getInstance(currency.getCurrencyCode());
                    if (currency2 == null) {
                        currency2 = new Currency(currency);
                        instances.put(currency2.currencyCode, currency2);
                    }
                    available.add(currency2);
                }
            }
        }
        return (Set) available.clone();
    }

    public String getCurrencyCode() {
        return this.currencyCode;
    }

    public String getSymbol() {
        return getSymbol(Locale.getDefault(Locale.Category.DISPLAY));
    }

    public String getSymbol(Locale locale) {
        if (locale == null) {
            throw new NullPointerException("locale == null");
        }
        return this.icuCurrency.getSymbol(locale);
    }

    public int getDefaultFractionDigits() {
        if (this.icuCurrency.getCurrencyCode().equals("XXX")) {
            return -1;
        }
        return this.icuCurrency.getDefaultFractionDigits();
    }

    public int getNumericCode() {
        return this.icuCurrency.getNumericCode();
    }

    public String getDisplayName() {
        return getDisplayName(Locale.getDefault(Locale.Category.DISPLAY));
    }

    public String getDisplayName(Locale locale) {
        return this.icuCurrency.getDisplayName((Locale) Objects.requireNonNull(locale));
    }

    public String toString() {
        return this.icuCurrency.toString();
    }

    private Object readResolve() {
        return getInstance(this.currencyCode);
    }
}
