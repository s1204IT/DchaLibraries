package android.icu.text;

import android.icu.impl.ICULocaleService;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.ICUService;
import android.icu.text.NumberFormat;
import android.icu.util.Currency;
import android.icu.util.ULocale;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Set;

class NumberFormatServiceShim extends NumberFormat.NumberFormatShim {
    private static ICULocaleService service = new NFService();

    NumberFormatServiceShim() {
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

    private static final class NFFactory extends ICULocaleService.LocaleKeyFactory {
        private NumberFormat.NumberFormatFactory delegate;

        NFFactory(NumberFormat.NumberFormatFactory delegate) {
            super(delegate.visible());
            this.delegate = delegate;
        }

        @Override
        public Object create(ICUService.Key key, ICUService srvc) {
            if (!handlesKey(key) || !(key instanceof ICULocaleService.LocaleKey)) {
                return null;
            }
            Object result = this.delegate.createFormat(key.canonicalLocale(), key.kind());
            if (result == null) {
                return srvc.getKey(key, null, this);
            }
            return result;
        }

        @Override
        protected Set<String> getSupportedIDs() {
            return this.delegate.getSupportedLocaleNames();
        }
    }

    @Override
    Object registerFactory(NumberFormat.NumberFormatFactory factory) {
        return service.registerFactory(new NFFactory(factory));
    }

    @Override
    boolean unregister(Object registryKey) {
        return service.unregisterFactory((ICUService.Factory) registryKey);
    }

    @Override
    NumberFormat createInstance(ULocale desiredLocale, int choice) {
        ULocale[] actualLoc = new ULocale[1];
        NumberFormat fmt = (NumberFormat) service.get(desiredLocale, choice, actualLoc);
        if (fmt == null) {
            throw new MissingResourceException("Unable to construct NumberFormat", "", "");
        }
        NumberFormat fmt2 = (NumberFormat) fmt.clone();
        if (choice == 1 || choice == 5 || choice == 6) {
            fmt2.setCurrency(Currency.getInstance(desiredLocale));
        }
        ULocale uloc = actualLoc[0];
        fmt2.setLocale(uloc, uloc);
        return fmt2;
    }

    private static class NFService extends ICULocaleService {
        NFService() {
            super("NumberFormat");
            registerFactory(new ICULocaleService.ICUResourceBundleFactory() {
                @Override
                protected Object handleCreate(ULocale loc, int kind, ICUService srvc) {
                    return NumberFormat.createInstance(loc, kind);
                }
            });
            markDefault();
        }
    }
}
