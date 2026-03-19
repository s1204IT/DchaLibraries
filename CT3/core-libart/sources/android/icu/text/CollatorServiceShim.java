package android.icu.text;

import android.icu.impl.ICULocaleService;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.ICUService;
import android.icu.impl.coll.CollationLoader;
import android.icu.impl.coll.CollationTailoring;
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
    Collator getInstance(ULocale locale) {
        try {
            ULocale[] actualLoc = new ULocale[1];
            Collator coll = (Collator) service.get(locale, actualLoc);
            if (coll == null) {
                throw new MissingResourceException("Could not locate Collator data", "", "");
            }
            return (Collator) coll.clone();
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException(e);
        }
    }

    @Override
    Object registerInstance(Collator collator, ULocale locale) {
        collator.setLocale(locale, locale);
        return service.registerObject(collator, locale);
    }

    @Override
    Object registerFactory(Collator.CollatorFactory f) {
        return service.registerFactory(new ICULocaleService.LocaleKeyFactory(f) {
            Collator.CollatorFactory delegate;

            {
                super(f.visible());
                this.delegate = f;
            }

            @Override
            public Object handleCreate(ULocale loc, int kind, ICUService srvc) {
                Object coll = this.delegate.createCollator(loc);
                return coll;
            }

            @Override
            public String getDisplayName(String id, ULocale displayLocale) {
                ULocale objectLocale = new ULocale(id);
                return this.delegate.getDisplayName(objectLocale, displayLocale);
            }

            @Override
            public Set<String> getSupportedIDs() {
                return this.delegate.getSupportedLocaleIDs();
            }
        });
    }

    @Override
    boolean unregister(Object registryKey) {
        return service.unregisterFactory((ICUService.Factory) registryKey);
    }

    @Override
    Locale[] getAvailableLocales() {
        if (service.isDefault()) {
            Locale[] result = ICUResourceBundle.getAvailableLocales("android/icu/impl/data/icudt56b/coll", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
            return result;
        }
        Locale[] result2 = service.getAvailableLocales();
        return result2;
    }

    @Override
    ULocale[] getAvailableULocales() {
        if (service.isDefault()) {
            ULocale[] result = ICUResourceBundle.getAvailableULocales("android/icu/impl/data/icudt56b/coll", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
            return result;
        }
        ULocale[] result2 = service.getAvailableULocales();
        return result2;
    }

    @Override
    String getDisplayName(ULocale objectLocale, ULocale displayLocale) {
        String id = objectLocale.getName();
        return service.getDisplayName(id, displayLocale);
    }

    private static class CService extends ICULocaleService {
        CService() {
            super("Collator");
            registerFactory(new ICULocaleService.ICUResourceBundleFactory() {
                @Override
                protected Object handleCreate(ULocale uloc, int kind, ICUService srvc) {
                    return CollatorServiceShim.makeInstance(uloc);
                }
            });
            markDefault();
        }

        @Override
        public String validateFallbackLocale() {
            return "";
        }

        @Override
        protected Object handleDefault(ICUService.Key key, String[] actualIDReturn) {
            if (actualIDReturn != null) {
                actualIDReturn[0] = "root";
            }
            try {
                return CollatorServiceShim.makeInstance(ULocale.ROOT);
            } catch (MissingResourceException e) {
                return null;
            }
        }
    }

    private static final Collator makeInstance(ULocale desiredLocale) {
        Output<ULocale> validLocale = new Output<>(ULocale.ROOT);
        CollationTailoring t = CollationLoader.loadTailoring(desiredLocale, validLocale);
        return new RuleBasedCollator(t, validLocale.value);
    }
}
