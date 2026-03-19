package android.icu.util;

import android.icu.impl.ICULocaleService;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.ICUService;
import android.icu.util.Currency;
import java.util.Locale;

final class CurrencyServiceShim extends Currency.ServiceShim {
    static final ICULocaleService service = new CFService();

    CurrencyServiceShim() {
    }

    @Override
    Locale[] getAvailableLocales() {
        if (service.isDefault()) {
            return ICUResourceBundle.getAvailableLocales();
        }
        return service.getAvailableLocales();
    }

    @Override
    ULocale[] getAvailableULocales() {
        if (service.isDefault()) {
            return ICUResourceBundle.getAvailableULocales();
        }
        return service.getAvailableULocales();
    }

    @Override
    Currency createInstance(ULocale loc) {
        if (service.isDefault()) {
            return Currency.createCurrency(loc);
        }
        Currency curr = (Currency) service.get(loc);
        return curr;
    }

    @Override
    Object registerInstance(Currency currency, ULocale locale) {
        return service.registerObject(currency, locale);
    }

    @Override
    boolean unregister(Object registryKey) {
        return service.unregisterFactory((ICUService.Factory) registryKey);
    }

    private static class CFService extends ICULocaleService {
        CFService() {
            super("Currency");
            registerFactory(new ICULocaleService.ICUResourceBundleFactory() {
                @Override
                protected Object handleCreate(ULocale loc, int kind, ICUService srvc) {
                    return Currency.createCurrency(loc);
                }
            });
            markDefault();
        }
    }
}
