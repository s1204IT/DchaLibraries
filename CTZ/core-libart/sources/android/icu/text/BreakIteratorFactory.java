package android.icu.text;

import android.icu.impl.Assert;
import android.icu.impl.ICUBinary;
import android.icu.impl.ICUData;
import android.icu.impl.ICULocaleService;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.ICUService;
import android.icu.impl.locale.BaseLocale;
import android.icu.text.BreakIterator;
import android.icu.util.ULocale;
import java.io.IOException;
import java.util.Locale;
import java.util.MissingResourceException;

final class BreakIteratorFactory extends BreakIterator.BreakIteratorServiceShim {
    static final ICULocaleService service = new BFService();
    private static final String[] KIND_NAMES = {"grapheme", "word", "line", "sentence", "title"};

    BreakIteratorFactory() {
    }

    @Override
    public Object registerInstance(BreakIterator breakIterator, ULocale uLocale, int i) {
        breakIterator.setText(new java.text.StringCharacterIterator(""));
        return service.registerObject(breakIterator, uLocale, i);
    }

    @Override
    public boolean unregister(Object obj) {
        if (service.isDefault()) {
            return false;
        }
        return service.unregisterFactory((ICUService.Factory) obj);
    }

    @Override
    public Locale[] getAvailableLocales() {
        if (service == null) {
            return ICUResourceBundle.getAvailableLocales();
        }
        return service.getAvailableLocales();
    }

    @Override
    public ULocale[] getAvailableULocales() {
        if (service == null) {
            return ICUResourceBundle.getAvailableULocales();
        }
        return service.getAvailableULocales();
    }

    @Override
    public BreakIterator createBreakIterator(ULocale uLocale, int i) {
        if (service.isDefault()) {
            return createBreakInstance(uLocale, i);
        }
        ULocale[] uLocaleArr = new ULocale[1];
        BreakIterator breakIterator = (BreakIterator) service.get(uLocale, i, uLocaleArr);
        breakIterator.setLocale(uLocaleArr[0], uLocaleArr[0]);
        return breakIterator;
    }

    private static class BFService extends ICULocaleService {
        BFService() {
            super("BreakIterator");
            registerFactory(new ICULocaleService.ICUResourceBundleFactory() {
                @Override
                protected Object handleCreate(ULocale uLocale, int i, ICUService iCUService) {
                    return BreakIteratorFactory.createBreakInstance(uLocale, i);
                }
            });
            markDefault();
        }

        @Override
        public String validateFallbackLocale() {
            return "";
        }
    }

    private static BreakIterator createBreakInstance(ULocale uLocale, int i) throws IOException {
        String str;
        String str2;
        String keywordValue;
        String keywordValue2;
        ICUResourceBundle bundleInstance = ICUResourceBundle.getBundleInstance(ICUData.ICU_BRKITR_BASE_NAME, uLocale, ICUResourceBundle.OpenType.LOCALE_ROOT);
        RuleBasedBreakIterator instanceFromCompiledRules = null;
        if (i == 2 && (keywordValue2 = uLocale.getKeywordValue("lb")) != null && (keywordValue2.equals("strict") || keywordValue2.equals("normal") || keywordValue2.equals("loose"))) {
            str = BaseLocale.SEP + keywordValue2;
        } else {
            str = null;
        }
        try {
            if (str == null) {
                str2 = KIND_NAMES[i];
            } else {
                str2 = KIND_NAMES[i] + str;
            }
            try {
                instanceFromCompiledRules = RuleBasedBreakIterator.getInstanceFromCompiledRules(ICUBinary.getData("brkitr/" + bundleInstance.getStringWithFallback("boundaries/" + str2)));
            } catch (IOException e) {
                Assert.fail(e);
            }
            ULocale uLocaleForLocale = ULocale.forLocale(bundleInstance.getLocale());
            instanceFromCompiledRules.setLocale(uLocaleForLocale, uLocaleForLocale);
            instanceFromCompiledRules.setBreakType(i);
            if (i == 3 && (keywordValue = uLocale.getKeywordValue("ss")) != null && keywordValue.equals("standard")) {
                return FilteredBreakIteratorBuilder.getInstance(new ULocale(uLocale.getBaseName())).wrapIteratorWithFilter(instanceFromCompiledRules);
            }
            return instanceFromCompiledRules;
        } catch (Exception e2) {
            throw new MissingResourceException(e2.toString(), "", "");
        }
    }
}
