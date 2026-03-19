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

        NFFactory(NumberFormat.NumberFormatFactory numberFormatFactory) {
            super(numberFormatFactory.visible());
            this.delegate = numberFormatFactory;
        }

        @Override
        public Object create(ICUService.Key key, ICUService iCUService) {
            if (!handlesKey(key) || !(key instanceof ICULocaleService.LocaleKey)) {
                return null;
            }
            ICULocaleService.LocaleKey localeKey = (ICULocaleService.LocaleKey) key;
            NumberFormat numberFormatCreateFormat = this.delegate.createFormat(localeKey.canonicalLocale(), localeKey.kind());
            if (numberFormatCreateFormat == null) {
                return iCUService.getKey(key, null, this);
            }
            return numberFormatCreateFormat;
        }

        @Override
        protected Set<String> getSupportedIDs() {
            return this.delegate.getSupportedLocaleNames();
        }
    }

    @Override
    Object registerFactory(NumberFormat.NumberFormatFactory numberFormatFactory) {
        return service.registerFactory(new NFFactory(numberFormatFactory));
    }

    @Override
    boolean unregister(Object obj) {
        return service.unregisterFactory((ICUService.Factory) obj);
    }

    @Override
    NumberFormat createInstance(ULocale uLocale, int i) {
        ULocale[] uLocaleArr = new ULocale[1];
        NumberFormat numberFormat = (NumberFormat) service.get(uLocale, i, uLocaleArr);
        if (numberFormat == null) {
            throw new MissingResourceException("Unable to construct NumberFormat", "", "");
        }
        NumberFormat numberFormat2 = (NumberFormat) numberFormat.clone();
        if (i == 1 || i == 5 || i == 6) {
            numberFormat2.setCurrency(Currency.getInstance(uLocale));
        }
        ULocale uLocale2 = uLocaleArr[0];
        numberFormat2.setLocale(uLocale2, uLocale2);
        return numberFormat2;
    }

    private static class NFService extends ICULocaleService {
        NFService() {
            super("NumberFormat");
            registerFactory(new ICULocaleService.ICUResourceBundleFactory() {
                @Override
                protected Object handleCreate(ULocale uLocale, int i, ICUService iCUService) {
                    return NumberFormat.createInstance(uLocale, i);
                }
            });
            markDefault();
        }
    }
}
