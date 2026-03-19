package android.icu.util;

import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.text.UnicodeSet;
import android.icu.util.ULocale;
import java.util.MissingResourceException;

public final class LocaleData {
    public static final int ALT_QUOTATION_END = 3;
    public static final int ALT_QUOTATION_START = 2;

    @Deprecated
    public static final int DELIMITER_COUNT = 4;
    public static final int ES_AUXILIARY = 1;

    @Deprecated
    public static final int ES_COUNT = 5;

    @Deprecated
    public static final int ES_CURRENCY = 3;
    public static final int ES_INDEX = 2;
    public static final int ES_PUNCTUATION = 4;
    public static final int ES_STANDARD = 0;
    private static final String LOCALE_DISPLAY_PATTERN = "localeDisplayPattern";
    private static final String MEASUREMENT_SYSTEM = "MeasurementSystem";
    private static final String PAPER_SIZE = "PaperSize";
    private static final String PATTERN = "pattern";
    public static final int QUOTATION_END = 1;
    public static final int QUOTATION_START = 0;
    private static final String SEPARATOR = "separator";
    private ICUResourceBundle bundle;
    private ICUResourceBundle langBundle;
    private boolean noSubstitute;
    private static final String[] DELIMITER_TYPES = {"quotationStart", "quotationEnd", "alternateQuotationStart", "alternateQuotationEnd"};
    private static VersionInfo gCLDRVersion = null;

    private LocaleData() {
    }

    public static UnicodeSet getExemplarSet(ULocale uLocale, int i) {
        return getInstance(uLocale).getExemplarSet(i, 0);
    }

    public static UnicodeSet getExemplarSet(ULocale uLocale, int i, int i2) {
        return getInstance(uLocale).getExemplarSet(i, i2);
    }

    public UnicodeSet getExemplarSet(int i, int i2) {
        String[] strArr = {"ExemplarCharacters", "AuxExemplarCharacters", "ExemplarCharactersIndex", "ExemplarCharactersCurrency", "ExemplarCharactersPunctuation"};
        if (i2 == 3) {
            if (this.noSubstitute) {
                return null;
            }
            return UnicodeSet.EMPTY;
        }
        try {
            ICUResourceBundle iCUResourceBundle = (ICUResourceBundle) this.bundle.get(strArr[i2]);
            if (this.noSubstitute && !this.bundle.isRoot() && iCUResourceBundle.isRoot()) {
                return null;
            }
            return new UnicodeSet(iCUResourceBundle.getString(), i | 1);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException(e);
        } catch (Exception e2) {
            if (this.noSubstitute) {
                return null;
            }
            return UnicodeSet.EMPTY;
        }
    }

    public static final LocaleData getInstance(ULocale uLocale) {
        LocaleData localeData = new LocaleData();
        localeData.bundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, uLocale);
        localeData.langBundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_LANG_BASE_NAME, uLocale);
        localeData.noSubstitute = false;
        return localeData;
    }

    public static final LocaleData getInstance() {
        return getInstance(ULocale.getDefault(ULocale.Category.FORMAT));
    }

    public void setNoSubstitute(boolean z) {
        this.noSubstitute = z;
    }

    public boolean getNoSubstitute() {
        return this.noSubstitute;
    }

    public String getDelimiter(int i) {
        ICUResourceBundle withFallback = ((ICUResourceBundle) this.bundle.get("delimiters")).getWithFallback(DELIMITER_TYPES[i]);
        if (this.noSubstitute && !this.bundle.isRoot() && withFallback.isRoot()) {
            return null;
        }
        return withFallback.getString();
    }

    private static UResourceBundle measurementTypeBundleForLocale(ULocale uLocale, String str) {
        UResourceBundle uResourceBundle;
        String regionForSupplementalData = ULocale.getRegionForSupplementalData(uLocale, true);
        try {
            UResourceBundle uResourceBundle2 = UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "supplementalData", ICUResourceBundle.ICU_DATA_CLASS_LOADER).get("measurementData");
            try {
                uResourceBundle = uResourceBundle2.get(regionForSupplementalData).get(str);
            } catch (MissingResourceException e) {
                uResourceBundle = uResourceBundle2.get("001").get(str);
            }
            return uResourceBundle;
        } catch (MissingResourceException e2) {
            return null;
        }
    }

    public static final class MeasurementSystem {
        public static final MeasurementSystem SI = new MeasurementSystem();
        public static final MeasurementSystem US = new MeasurementSystem();
        public static final MeasurementSystem UK = new MeasurementSystem();

        private MeasurementSystem() {
        }
    }

    public static final MeasurementSystem getMeasurementSystem(ULocale uLocale) {
        switch (measurementTypeBundleForLocale(uLocale, MEASUREMENT_SYSTEM).getInt()) {
            case 0:
                return MeasurementSystem.SI;
            case 1:
                return MeasurementSystem.US;
            case 2:
                return MeasurementSystem.UK;
            default:
                return null;
        }
    }

    public static final class PaperSize {
        private int height;
        private int width;

        private PaperSize(int i, int i2) {
            this.height = i;
            this.width = i2;
        }

        public int getHeight() {
            return this.height;
        }

        public int getWidth() {
            return this.width;
        }
    }

    public static final PaperSize getPaperSize(ULocale uLocale) {
        int[] intVector = measurementTypeBundleForLocale(uLocale, PAPER_SIZE).getIntVector();
        return new PaperSize(intVector[0], intVector[1]);
    }

    public String getLocaleDisplayPattern() {
        return ((ICUResourceBundle) this.langBundle.get(LOCALE_DISPLAY_PATTERN)).getStringWithFallback(PATTERN);
    }

    public String getLocaleSeparator() {
        String stringWithFallback = ((ICUResourceBundle) this.langBundle.get(LOCALE_DISPLAY_PATTERN)).getStringWithFallback(SEPARATOR);
        int iIndexOf = stringWithFallback.indexOf("{0}");
        int iIndexOf2 = stringWithFallback.indexOf("{1}");
        if (iIndexOf >= 0 && iIndexOf2 >= 0 && iIndexOf <= iIndexOf2) {
            return stringWithFallback.substring(iIndexOf + "{0}".length(), iIndexOf2);
        }
        return stringWithFallback;
    }

    public static VersionInfo getCLDRVersion() {
        if (gCLDRVersion == null) {
            gCLDRVersion = VersionInfo.getInstance(UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "supplementalData", ICUResourceBundle.ICU_DATA_CLASS_LOADER).get("cldrVersion").getString());
        }
        return gCLDRVersion;
    }
}
