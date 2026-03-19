package android.icu.util;

import android.icu.impl.ICUResourceBundle;
import android.icu.text.UnicodeSet;
import android.icu.util.ULocale;
import java.util.MissingResourceException;

public final class LocaleData {
    public static final int ALT_QUOTATION_END = 3;
    public static final int ALT_QUOTATION_START = 2;
    public static final int DELIMITER_COUNT = 4;
    public static final int ES_AUXILIARY = 1;
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

    public static UnicodeSet getExemplarSet(ULocale locale, int options) {
        return getInstance(locale).getExemplarSet(options, 0);
    }

    public static UnicodeSet getExemplarSet(ULocale locale, int options, int extype) {
        return getInstance(locale).getExemplarSet(options, extype);
    }

    public UnicodeSet getExemplarSet(int options, int extype) {
        String[] exemplarSetTypes = {"ExemplarCharacters", "AuxExemplarCharacters", "ExemplarCharactersIndex", "ExemplarCharactersCurrency", "ExemplarCharactersPunctuation"};
        if (extype == 3) {
            if (this.noSubstitute) {
                return null;
            }
            return UnicodeSet.EMPTY;
        }
        try {
            String aKey = exemplarSetTypes[extype];
            ICUResourceBundle stringBundle = (ICUResourceBundle) this.bundle.get(aKey);
            if (this.noSubstitute && stringBundle.getLoadingStatus() == 2) {
                return null;
            }
            String unicodeSetPattern = stringBundle.getString();
            return new UnicodeSet(unicodeSetPattern, options | 1);
        } catch (ArrayIndexOutOfBoundsException aiooe) {
            throw new IllegalArgumentException(aiooe);
        } catch (Exception e) {
            if (this.noSubstitute) {
                return null;
            }
            return UnicodeSet.EMPTY;
        }
    }

    public static final LocaleData getInstance(ULocale locale) {
        LocaleData ld = new LocaleData();
        ld.bundle = (ICUResourceBundle) UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", locale);
        ld.langBundle = (ICUResourceBundle) UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b/lang", locale);
        ld.noSubstitute = false;
        return ld;
    }

    public static final LocaleData getInstance() {
        return getInstance(ULocale.getDefault(ULocale.Category.FORMAT));
    }

    public void setNoSubstitute(boolean setting) {
        this.noSubstitute = setting;
    }

    public boolean getNoSubstitute() {
        return this.noSubstitute;
    }

    public String getDelimiter(int type) {
        ICUResourceBundle delimitersBundle = (ICUResourceBundle) this.bundle.get("delimiters");
        ICUResourceBundle stringBundle = delimitersBundle.getWithFallback(DELIMITER_TYPES[type]);
        if (this.noSubstitute && stringBundle.getLoadingStatus() == 2) {
            return null;
        }
        return stringBundle.getString();
    }

    private static UResourceBundle measurementTypeBundleForLocale(ULocale locale, String measurementType) {
        UResourceBundle measTypeBundle = null;
        ULocale fullLoc = ULocale.addLikelySubtags(locale);
        String region = fullLoc.getCountry();
        try {
            UResourceBundle rb = UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", "supplementalData", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
            UResourceBundle measurementData = rb.get("measurementData");
            try {
                UResourceBundle measDataBundle = measurementData.get(region);
                measTypeBundle = measDataBundle.get(measurementType);
            } catch (MissingResourceException e) {
                UResourceBundle measDataBundle2 = measurementData.get("001");
                measTypeBundle = measDataBundle2.get(measurementType);
            }
        } catch (MissingResourceException e2) {
        }
        return measTypeBundle;
    }

    public static final class MeasurementSystem {
        private int systemID;
        public static final MeasurementSystem SI = new MeasurementSystem(0);
        public static final MeasurementSystem US = new MeasurementSystem(1);
        public static final MeasurementSystem UK = new MeasurementSystem(2);

        private MeasurementSystem(int id) {
            this.systemID = id;
        }

        private boolean equals(int id) {
            return this.systemID == id;
        }
    }

    public static final MeasurementSystem getMeasurementSystem(ULocale locale) {
        UResourceBundle sysBundle = measurementTypeBundleForLocale(locale, MEASUREMENT_SYSTEM);
        int system = sysBundle.getInt();
        if (MeasurementSystem.US.equals(system)) {
            return MeasurementSystem.US;
        }
        if (MeasurementSystem.UK.equals(system)) {
            return MeasurementSystem.UK;
        }
        if (MeasurementSystem.SI.equals(system)) {
            return MeasurementSystem.SI;
        }
        return null;
    }

    public static final class PaperSize {
        private int height;
        private int width;

        PaperSize(int h, int w, PaperSize paperSize) {
            this(h, w);
        }

        private PaperSize(int h, int w) {
            this.height = h;
            this.width = w;
        }

        public int getHeight() {
            return this.height;
        }

        public int getWidth() {
            return this.width;
        }
    }

    public static final PaperSize getPaperSize(ULocale locale) {
        UResourceBundle obj = measurementTypeBundleForLocale(locale, PAPER_SIZE);
        int[] size = obj.getIntVector();
        return new PaperSize(size[0], size[1], null);
    }

    public String getLocaleDisplayPattern() {
        ICUResourceBundle locDispBundle = (ICUResourceBundle) this.langBundle.get(LOCALE_DISPLAY_PATTERN);
        String localeDisplayPattern = locDispBundle.getStringWithFallback(PATTERN);
        return localeDisplayPattern;
    }

    public String getLocaleSeparator() {
        ICUResourceBundle locDispBundle = (ICUResourceBundle) this.langBundle.get(LOCALE_DISPLAY_PATTERN);
        String localeSeparator = locDispBundle.getStringWithFallback(SEPARATOR);
        int index0 = localeSeparator.indexOf("{0}");
        int index1 = localeSeparator.indexOf("{1}");
        if (index0 >= 0 && index1 >= 0 && index0 <= index1) {
            return localeSeparator.substring("{0}".length() + index0, index1);
        }
        return localeSeparator;
    }

    public static VersionInfo getCLDRVersion() {
        if (gCLDRVersion == null) {
            UResourceBundle supplementalDataBundle = UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", "supplementalData", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
            UResourceBundle cldrVersionBundle = supplementalDataBundle.get("cldrVersion");
            gCLDRVersion = VersionInfo.getInstance(cldrVersionBundle.getString());
        }
        return gCLDRVersion;
    }
}
