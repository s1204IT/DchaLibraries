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
    Currency createInstance(ULocale uLocale) {
        if (service.isDefault()) {
            return Currency.createCurrency(uLocale);
        }
        return (Currency) service.get(uLocale);
    }

    @Override
    Object registerInstance(Currency currency, ULocale uLocale) {
        return service.registerObject(currency, uLocale);
    }

    @Override
    boolean unregister(Object obj) {
        return service.unregisterFactory((ICUService.Factory) obj);
    }

    private static class CFService extends ICULocaleService {
        CFService() {
            super("Currency");
            registerFactory(new ICULocaleService.ICUResourceBundleFactory() {
                @Override
                protected Object handleCreate(ULocale uLocale, int i, ICUService iCUService) {
                    return Currency.createCurrency(uLocale);
                }
            });
            markDefault();
        }
    }
}
