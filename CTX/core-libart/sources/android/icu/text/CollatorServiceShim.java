package android.icu.text;

import android.icu.impl.ICUData;
import android.icu.impl.ICULocaleService;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.ICUService;
import android.icu.impl.coll.CollationLoader;
import android.icu.text.Collator;
import android.icu.util.ICUCloneNotSupportedException;
import android.icu.util.Output;
import android.icu.util.ULocale;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Set;

final class CollatorServiceShim extends Collator.ServiceShim {
    private static ICULocaleService service = new CService();

    CollatorServiceShim() {
    }

    @Override
    Collator getInstance(ULocale uLocale) {
        try {
            Collator collator = (Collator) service.get(uLocale, new ULocale[1]);
            if (collator == null) {
                throw new MissingResourceException("Could not locate Collator data", "", "");
            }
            return (Collator) collator.clone();
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException(e);
        }
    }

    @Override
    Object registerInstance(Collator collator, ULocale uLocale) {
        collator.setLocale(uLocale, uLocale);
        return service.registerObject(collator, uLocale);
    }

    @Override
    Object registerFactory(Collator.CollatorFactory collatorFactory) {
        return service.registerFactory(new ICULocaleService.LocaleKeyFactory(collatorFactory) {
            Collator.CollatorFactory delegate;

            {
                super(collatorFactory.visible());
                this.delegate = collatorFactory;
            }

            @Override
            public Object handleCreate(ULocale uLocale, int i, ICUService iCUService) {
                return this.delegate.createCollator(uLocale);
            }

            @Override
            public String getDisplayName(String str, ULocale uLocale) {
                return this.delegate.getDisplayName(new ULocale(str), uLocale);
            }

            @Override
            public Set<String> getSupportedIDs() {
                return this.delegate.getSupportedLocaleIDs();
            }
        });
    }

    @Override
    boolean unregister(Object obj) {
        return service.unregisterFactory((ICUService.Factory) obj);
    }

    @Override
    Locale[] getAvailableLocales() {
        if (service.isDefault()) {
            return ICUResourceBundle.getAvailableLocales(ICUData.ICU_COLLATION_BASE_NAME, ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        }
        return service.getAvailableLocales();
    }

    @Override
    ULocale[] getAvailableULocales() {
        if (service.isDefault()) {
            return ICUResourceBundle.getAvailableULocales(ICUData.ICU_COLLATION_BASE_NAME, ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        }
        return service.getAvailableULocales();
    }

    @Override
    String getDisplayName(ULocale uLocale, ULocale uLocale2) {
        return service.getDisplayName(uLocale.getName(), uLocale2);
    }

    private static class CService extends ICULocaleService {
        CService() {
            super("Collator");
            registerFactory(new ICULocaleService.ICUResourceBundleFactory() {
                @Override
                protected Object handleCreate(ULocale uLocale, int i, ICUService iCUService) {
                    return CollatorServiceShim.makeInstance(uLocale);
                }
            });
            markDefault();
        }

        @Override
        public String validateFallbackLocale() {
            return "";
        }

        @Override
        protected Object handleDefault(ICUService.Key key, String[] strArr) {
            if (strArr != null) {
                strArr[0] = "root";
            }
            try {
                return CollatorServiceShim.makeInstance(ULocale.ROOT);
            } catch (MissingResourceException e) {
                return null;
            }
        }
    }

    private static final Collator makeInstance(ULocale uLocale) {
        Output output = new Output(ULocale.ROOT);
        return new RuleBasedCollator(CollationLoader.loadTailoring(uLocale, output), (ULocale) output.value);
    }
}
