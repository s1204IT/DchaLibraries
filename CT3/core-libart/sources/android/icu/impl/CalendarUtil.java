package android.icu.impl;

import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.util.MissingResourceException;

public class CalendarUtil {
    private static final String CALKEY = "calendar";
    private static ICUCache<String, String> CALTYPE_CACHE = new SimpleCache();
    private static final String DEFCAL = "gregorian";

    public static String getCalendarType(ULocale loc) {
        UResourceBundle order;
        String calType = loc.getKeywordValue(CALKEY);
        if (calType != null) {
            return calType;
        }
        String baseLoc = loc.getBaseName();
        String calType2 = CALTYPE_CACHE.get(baseLoc);
        if (calType2 != null) {
            return calType2;
        }
        ULocale canonical = ULocale.createCanonical(loc.toString());
        String calType3 = canonical.getKeywordValue(CALKEY);
        if (calType3 == null) {
            String region = canonical.getCountry();
            if (region.length() == 0) {
                ULocale fullLoc = ULocale.addLikelySubtags(canonical);
                region = fullLoc.getCountry();
            }
            try {
                UResourceBundle rb = UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", "supplementalData", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
                UResourceBundle calPref = rb.get("calendarPreferenceData");
                try {
                    order = calPref.get(region);
                } catch (MissingResourceException e) {
                    order = calPref.get("001");
                }
                calType3 = order.getString(0);
            } catch (MissingResourceException e2) {
            }
            if (calType3 == null) {
                calType3 = DEFCAL;
            }
        }
        CALTYPE_CACHE.put(baseLoc, calType3);
        return calType3;
    }
}
