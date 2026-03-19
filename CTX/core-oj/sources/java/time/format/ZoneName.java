package java.time.format;

import android.icu.impl.ZoneMeta;
import android.icu.text.TimeZoneNames;
import android.icu.util.ULocale;
import java.util.Locale;

class ZoneName {
    ZoneName() {
    }

    public static String toZid(String str, Locale locale) {
        TimeZoneNames timeZoneNames = TimeZoneNames.getInstance(locale);
        if (timeZoneNames.getAvailableMetaZoneIDs().contains(str)) {
            ULocale uLocaleForLocale = ULocale.forLocale(locale);
            String country = uLocaleForLocale.getCountry();
            if (country.length() == 0) {
                country = ULocale.addLikelySubtags(uLocaleForLocale).getCountry();
            }
            str = timeZoneNames.getReferenceZoneID(str, country);
        }
        return toZid(str);
    }

    public static String toZid(String str) {
        String canonicalCLDRID = ZoneMeta.getCanonicalCLDRID(str);
        if (canonicalCLDRID != null) {
            return canonicalCLDRID;
        }
        return str;
    }
}
