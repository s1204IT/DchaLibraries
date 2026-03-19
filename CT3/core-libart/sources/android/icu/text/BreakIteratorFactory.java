package android.icu.text;

import android.icu.impl.Assert;
import android.icu.impl.ICUBinary;
import android.icu.impl.ICULocaleService;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.ICUService;
import android.icu.impl.locale.BaseLocale;
import android.icu.text.BreakIterator;
import android.icu.util.ULocale;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.MissingResourceException;

final class BreakIteratorFactory extends BreakIterator.BreakIteratorServiceShim {
    static final ICULocaleService service = new BFService();
    private static final String[] KIND_NAMES = {"grapheme", "word", "line", "sentence", "title"};

    BreakIteratorFactory() {
    }

    @Override
    public Object registerInstance(BreakIterator iter, ULocale locale, int kind) {
        iter.setText(new java.text.StringCharacterIterator(""));
        return service.registerObject(iter, locale, kind);
    }

    @Override
    public boolean unregister(Object key) {
        if (service.isDefault()) {
            return false;
        }
        return service.unregisterFactory((ICUService.Factory) key);
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
    public BreakIterator createBreakIterator(ULocale locale, int kind) {
        if (service.isDefault()) {
            return createBreakInstance(locale, kind);
        }
        ULocale[] actualLoc = new ULocale[1];
        BreakIterator iter = (BreakIterator) service.get(locale, kind, actualLoc);
        iter.setLocale(actualLoc[0], actualLoc[0]);
        return iter;
    }

    private static class BFService extends ICULocaleService {
        BFService() {
            super("BreakIterator");
            registerFactory(new ICULocaleService.ICUResourceBundleFactory() {
                @Override
                protected Object handleCreate(ULocale loc, int kind, ICUService srvc) {
                    return BreakIteratorFactory.createBreakInstance(loc, kind);
                }
            });
            markDefault();
        }

        @Override
        public String validateFallbackLocale() {
            return "";
        }
    }

    private static BreakIterator createBreakInstance(ULocale locale, int kind) {
        String lbKeyValue;
        RuleBasedBreakIterator iter = null;
        ICUResourceBundle rb = (ICUResourceBundle) ICUResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b/brkitr", locale, ICUResourceBundle.OpenType.LOCALE_ROOT);
        String typeKeyExt = null;
        if (kind == 2 && (lbKeyValue = locale.getKeywordValue("lb")) != null && (lbKeyValue.equals("strict") || lbKeyValue.equals("normal") || lbKeyValue.equals("loose"))) {
            typeKeyExt = BaseLocale.SEP + lbKeyValue;
        }
        try {
            String typeKey = typeKeyExt == null ? KIND_NAMES[kind] : KIND_NAMES[kind] + typeKeyExt;
            String brkfname = rb.getStringWithFallback("boundaries/" + typeKey);
            String rulesFileName = "brkitr/" + brkfname;
            ByteBuffer bytes = ICUBinary.getData(rulesFileName);
            try {
                iter = RuleBasedBreakIterator.getInstanceFromCompiledRules(bytes);
            } catch (IOException e) {
                Assert.fail(e);
            }
            ULocale uloc = ULocale.forLocale(rb.getLocale());
            iter.setLocale(uloc, uloc);
            iter.setBreakType(kind);
            return iter;
        } catch (Exception e2) {
            throw new MissingResourceException(e2.toString(), "", "");
        }
    }
}
