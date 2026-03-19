package android.icu.impl;

import android.icu.text.NumberFormat;
import android.icu.util.Output;
import android.icu.util.SimpleTimeZone;
import android.icu.util.TimeZone;
import android.icu.util.UResourceBundle;
import java.lang.ref.SoftReference;
import java.text.ParsePosition;
import java.util.Collections;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.TreeSet;

public final class ZoneMeta {

    private static final int[] f28androidicuutilTimeZone$SystemTimeZoneTypeSwitchesValues = null;

    static final boolean f29assertionsDisabled;
    private static final boolean ASSERT = false;
    private static ICUCache<String, String> CANONICAL_ID_CACHE = null;
    private static final CustomTimeZoneCache CUSTOM_ZONE_CACHE;
    private static SoftReference<Set<String>> REF_CANONICAL_SYSTEM_LOCATION_ZONES = null;
    private static SoftReference<Set<String>> REF_CANONICAL_SYSTEM_ZONES = null;
    private static SoftReference<Set<String>> REF_SYSTEM_ZONES = null;
    private static ICUCache<String, String> REGION_CACHE = null;
    private static ICUCache<String, Boolean> SINGLE_COUNTRY_CACHE = null;
    private static final SystemTimeZoneCache SYSTEM_ZONE_CACHE;
    private static String[] ZONEIDS = null;
    private static final String ZONEINFORESNAME = "zoneinfo64";
    private static final String kCUSTOM_TZ_PREFIX = "GMT";
    private static final String kGMT_ID = "GMT";
    private static final int kMAX_CUSTOM_HOUR = 23;
    private static final int kMAX_CUSTOM_MIN = 59;
    private static final int kMAX_CUSTOM_SEC = 59;
    private static final String kNAMES = "Names";
    private static final String kREGIONS = "Regions";
    private static final String kWorld = "001";
    private static final String kZONES = "Zones";

    private static int[] m70getandroidicuutilTimeZone$SystemTimeZoneTypeSwitchesValues() {
        if (f28androidicuutilTimeZone$SystemTimeZoneTypeSwitchesValues != null) {
            return f28androidicuutilTimeZone$SystemTimeZoneTypeSwitchesValues;
        }
        int[] iArr = new int[TimeZone.SystemTimeZoneType.valuesCustom().length];
        try {
            iArr[TimeZone.SystemTimeZoneType.ANY.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[TimeZone.SystemTimeZoneType.CANONICAL.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[TimeZone.SystemTimeZoneType.CANONICAL_LOCATION.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        f28androidicuutilTimeZone$SystemTimeZoneTypeSwitchesValues = iArr;
        return iArr;
    }

    private static synchronized Set<String> getSystemZIDs() {
        Set<String> systemZones;
        systemZones = null;
        if (REF_SYSTEM_ZONES != null) {
            systemZones = REF_SYSTEM_ZONES.get();
        }
        if (systemZones == null) {
            Set<String> systemIDs = new TreeSet<>();
            String[] allIDs = getZoneIDs();
            for (String id : allIDs) {
                if (!id.equals(TimeZone.UNKNOWN_ZONE_ID)) {
                    systemIDs.add(id);
                }
            }
            systemZones = Collections.unmodifiableSet(systemIDs);
            REF_SYSTEM_ZONES = new SoftReference<>(systemZones);
        }
        return systemZones;
    }

    private static synchronized Set<String> getCanonicalSystemZIDs() {
        Set<String> canonicalSystemZones;
        canonicalSystemZones = null;
        if (REF_CANONICAL_SYSTEM_ZONES != null) {
            canonicalSystemZones = REF_CANONICAL_SYSTEM_ZONES.get();
        }
        if (canonicalSystemZones == null) {
            Set<String> canonicalSystemIDs = new TreeSet<>();
            String[] allIDs = getZoneIDs();
            for (String id : allIDs) {
                if (!id.equals(TimeZone.UNKNOWN_ZONE_ID)) {
                    String canonicalID = getCanonicalCLDRID(id);
                    if (id.equals(canonicalID)) {
                        canonicalSystemIDs.add(id);
                    }
                }
            }
            canonicalSystemZones = Collections.unmodifiableSet(canonicalSystemIDs);
            REF_CANONICAL_SYSTEM_ZONES = new SoftReference<>(canonicalSystemZones);
        }
        return canonicalSystemZones;
    }

    private static synchronized Set<String> getCanonicalSystemLocationZIDs() {
        Set<String> canonicalSystemLocationZones;
        String region;
        canonicalSystemLocationZones = null;
        if (REF_CANONICAL_SYSTEM_LOCATION_ZONES != null) {
            canonicalSystemLocationZones = REF_CANONICAL_SYSTEM_LOCATION_ZONES.get();
        }
        if (canonicalSystemLocationZones == null) {
            Set<String> canonicalSystemLocationIDs = new TreeSet<>();
            String[] allIDs = getZoneIDs();
            for (String id : allIDs) {
                if (!id.equals(TimeZone.UNKNOWN_ZONE_ID)) {
                    String canonicalID = getCanonicalCLDRID(id);
                    if (id.equals(canonicalID) && (region = getRegion(id)) != null && !region.equals(kWorld)) {
                        canonicalSystemLocationIDs.add(id);
                    }
                }
            }
            canonicalSystemLocationZones = Collections.unmodifiableSet(canonicalSystemLocationIDs);
            REF_CANONICAL_SYSTEM_LOCATION_ZONES = new SoftReference<>(canonicalSystemLocationZones);
        }
        return canonicalSystemLocationZones;
    }

    public static Set<String> getAvailableIDs(TimeZone.SystemTimeZoneType type, String region, Integer rawOffset) {
        Set<String> baseSet;
        TimeZone z;
        switch (m70getandroidicuutilTimeZone$SystemTimeZoneTypeSwitchesValues()[type.ordinal()]) {
            case 1:
                baseSet = getSystemZIDs();
                break;
            case 2:
                baseSet = getCanonicalSystemZIDs();
                break;
            case 3:
                baseSet = getCanonicalSystemLocationZIDs();
                break;
            default:
                throw new IllegalArgumentException("Unknown SystemTimeZoneType");
        }
        if (region == null && rawOffset == null) {
            return baseSet;
        }
        if (region != null) {
            region = region.toUpperCase(Locale.ENGLISH);
        }
        Set<String> result = new TreeSet<>();
        for (String id : baseSet) {
            if (region != null) {
                String r = getRegion(id);
                if (region.equals(r)) {
                }
            }
            if (rawOffset == null || ((z = getSystemTimeZone(id)) != null && rawOffset.equals(Integer.valueOf(z.getRawOffset())))) {
                result.add(id);
            }
        }
        if (result.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(result);
    }

    public static synchronized int countEquivalentIDs(String id) {
        int count;
        count = 0;
        UResourceBundle res = openOlsonResource(null, id);
        if (res != null) {
            try {
                UResourceBundle links = res.get("links");
                int[] v = links.getIntVector();
                count = v.length;
            } catch (MissingResourceException e) {
            }
        }
        return count;
    }

    public static synchronized String getEquivalentID(String id, int index) {
        String result;
        result = "";
        if (index >= 0) {
            UResourceBundle res = openOlsonResource(null, id);
            if (res != null) {
                int zoneIdx = -1;
                try {
                    UResourceBundle links = res.get("links");
                    int[] zones = links.getIntVector();
                    if (index < zones.length) {
                        zoneIdx = zones[index];
                    }
                } catch (MissingResourceException e) {
                }
                if (zoneIdx >= 0) {
                    String tmp = getZoneID(zoneIdx);
                    if (tmp != null) {
                        result = tmp;
                    }
                }
            }
        }
        return result;
    }

    static {
        SystemTimeZoneCache systemTimeZoneCache = null;
        Object[] objArr = 0;
        f29assertionsDisabled = !ZoneMeta.class.desiredAssertionStatus();
        ZONEIDS = null;
        CANONICAL_ID_CACHE = new SimpleCache();
        REGION_CACHE = new SimpleCache();
        SINGLE_COUNTRY_CACHE = new SimpleCache();
        SYSTEM_ZONE_CACHE = new SystemTimeZoneCache(systemTimeZoneCache);
        CUSTOM_ZONE_CACHE = new CustomTimeZoneCache(objArr == true ? 1 : 0);
    }

    private static synchronized String[] getZoneIDs() {
        if (ZONEIDS == null) {
            try {
                UResourceBundle top = UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", ZONEINFORESNAME, ICUResourceBundle.ICU_DATA_CLASS_LOADER);
                ZONEIDS = top.getStringArray(kNAMES);
            } catch (MissingResourceException e) {
            }
        }
        if (ZONEIDS == null) {
            ZONEIDS = new String[0];
        }
        return ZONEIDS;
    }

    private static String getZoneID(int idx) {
        if (idx >= 0) {
            String[] ids = getZoneIDs();
            if (idx < ids.length) {
                return ids[idx];
            }
            return null;
        }
        return null;
    }

    private static int getZoneIndex(String zid) {
        String[] all = getZoneIDs();
        if (all.length <= 0) {
            return -1;
        }
        int start = 0;
        int limit = all.length;
        int lastMid = Integer.MAX_VALUE;
        while (true) {
            int mid = (start + limit) / 2;
            if (lastMid == mid) {
                return -1;
            }
            lastMid = mid;
            int r = zid.compareTo(all[mid]);
            if (r == 0) {
                return mid;
            }
            if (r < 0) {
                limit = mid;
            } else {
                start = mid;
            }
        }
    }

    public static String getCanonicalCLDRID(TimeZone tz) {
        if (tz instanceof OlsonTimeZone) {
            return ((OlsonTimeZone) tz).getCanonicalID();
        }
        return getCanonicalCLDRID(tz.getID());
    }

    public static String getCanonicalCLDRID(String tzid) {
        String canonical = CANONICAL_ID_CACHE.get(tzid);
        if (canonical == null) {
            canonical = findCLDRCanonicalID(tzid);
            if (canonical == null) {
                try {
                    int zoneIdx = getZoneIndex(tzid);
                    if (zoneIdx >= 0) {
                        UResourceBundle top = UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", ZONEINFORESNAME, ICUResourceBundle.ICU_DATA_CLASS_LOADER);
                        UResourceBundle zones = top.get(kZONES);
                        UResourceBundle zone = zones.get(zoneIdx);
                        if (zone.getType() == 7) {
                            tzid = getZoneID(zone.getInt());
                            canonical = findCLDRCanonicalID(tzid);
                        }
                        if (canonical == null) {
                            canonical = tzid;
                        }
                    }
                } catch (MissingResourceException e) {
                }
            }
            if (canonical != null) {
                CANONICAL_ID_CACHE.put(tzid, canonical);
            }
        }
        return canonical;
    }

    private static String findCLDRCanonicalID(String tzid) {
        String canonical = null;
        String tzidKey = tzid.replace('/', ':');
        try {
            UResourceBundle keyTypeData = UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", "keyTypeData", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
            UResourceBundle typeMap = keyTypeData.get("typeMap");
            UResourceBundle typeKeys = typeMap.get("timezone");
            try {
                typeKeys.get(tzidKey);
                canonical = tzid;
            } catch (MissingResourceException e) {
            }
            if (canonical == null) {
                UResourceBundle typeAlias = keyTypeData.get("typeAlias");
                UResourceBundle aliasesForKey = typeAlias.get("timezone");
                return aliasesForKey.getString(tzidKey);
            }
            return canonical;
        } catch (MissingResourceException e2) {
            return null;
        }
    }

    public static String getRegion(String tzid) {
        int zoneIdx;
        String region = REGION_CACHE.get(tzid);
        if (region == null && (zoneIdx = getZoneIndex(tzid)) >= 0) {
            try {
                UResourceBundle top = UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", ZONEINFORESNAME, ICUResourceBundle.ICU_DATA_CLASS_LOADER);
                UResourceBundle regions = top.get(kREGIONS);
                if (zoneIdx < regions.getSize()) {
                    region = regions.getString(zoneIdx);
                }
            } catch (MissingResourceException e) {
            }
            if (region != null) {
                REGION_CACHE.put(tzid, region);
            }
        }
        return region;
    }

    public static String getCanonicalCountry(String tzid) {
        String country = getRegion(tzid);
        if (country != null && country.equals(kWorld)) {
            return null;
        }
        return country;
    }

    public static String getCanonicalCountry(String tzid, Output<Boolean> isPrimary) {
        isPrimary.value = Boolean.FALSE;
        String country = getRegion(tzid);
        if (country != null && country.equals(kWorld)) {
            return null;
        }
        Boolean singleZone = SINGLE_COUNTRY_CACHE.get(tzid);
        if (singleZone == null) {
            Set<String> ids = TimeZone.getAvailableIDs(TimeZone.SystemTimeZoneType.CANONICAL_LOCATION, country, null);
            if (!f29assertionsDisabled) {
                if (!(ids.size() >= 1)) {
                    throw new AssertionError();
                }
            }
            singleZone = Boolean.valueOf(ids.size() <= 1);
            SINGLE_COUNTRY_CACHE.put(tzid, singleZone);
        }
        if (singleZone.booleanValue()) {
            isPrimary.value = Boolean.TRUE;
        } else {
            try {
                UResourceBundle bundle = UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", "metaZones");
                UResourceBundle primaryZones = bundle.get("primaryZones");
                String primaryZone = primaryZones.getString(country);
                if (tzid.equals(primaryZone)) {
                    isPrimary.value = Boolean.TRUE;
                } else {
                    String canonicalID = getCanonicalCLDRID(tzid);
                    if (canonicalID != null && canonicalID.equals(primaryZone)) {
                        isPrimary.value = Boolean.TRUE;
                    }
                }
            } catch (MissingResourceException e) {
            }
        }
        return country;
    }

    public static UResourceBundle openOlsonResource(UResourceBundle top, String id) {
        int zoneIdx = getZoneIndex(id);
        if (zoneIdx < 0) {
            return null;
        }
        if (top == null) {
            try {
                top = UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", ZONEINFORESNAME, ICUResourceBundle.ICU_DATA_CLASS_LOADER);
            } catch (MissingResourceException e) {
                return null;
            }
        }
        UResourceBundle zones = top.get(kZONES);
        UResourceBundle zone = zones.get(zoneIdx);
        if (zone.getType() == 7) {
            zone = zones.get(zone.getInt());
        }
        UResourceBundle res = zone;
        return res;
    }

    private static class SystemTimeZoneCache extends SoftCache<String, OlsonTimeZone, String> {
        SystemTimeZoneCache(SystemTimeZoneCache systemTimeZoneCache) {
            this();
        }

        private SystemTimeZoneCache() {
        }

        @Override
        protected OlsonTimeZone createInstance(String key, String data) {
            try {
                UResourceBundle top = UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", ZoneMeta.ZONEINFORESNAME, ICUResourceBundle.ICU_DATA_CLASS_LOADER);
                UResourceBundle res = ZoneMeta.openOlsonResource(top, data);
                if (res == null) {
                    return null;
                }
                OlsonTimeZone tz = new OlsonTimeZone(top, res, data);
                try {
                    tz.freeze();
                    return tz;
                } catch (MissingResourceException e) {
                    return tz;
                }
            } catch (MissingResourceException e2) {
                return null;
            }
        }
    }

    public static TimeZone getSystemTimeZone(String id) {
        return SYSTEM_ZONE_CACHE.getInstance(id, id);
    }

    private static class CustomTimeZoneCache extends SoftCache<Integer, SimpleTimeZone, int[]> {

        static final boolean f30assertionsDisabled;

        CustomTimeZoneCache(CustomTimeZoneCache customTimeZoneCache) {
            this();
        }

        static {
            f30assertionsDisabled = !CustomTimeZoneCache.class.desiredAssertionStatus();
        }

        private CustomTimeZoneCache() {
        }

        @Override
        protected SimpleTimeZone createInstance(Integer key, int[] data) {
            if (!f30assertionsDisabled) {
                if (!(data.length == 4)) {
                    throw new AssertionError();
                }
            }
            if (!f30assertionsDisabled) {
                if (!(data[0] == 1 || data[0] == -1)) {
                    throw new AssertionError();
                }
            }
            if (!f30assertionsDisabled) {
                if (!(data[1] >= 0 && data[1] <= 23)) {
                    throw new AssertionError();
                }
            }
            if (!f30assertionsDisabled) {
                if (!(data[2] >= 0 && data[2] <= 59)) {
                    throw new AssertionError();
                }
            }
            if (!f30assertionsDisabled) {
                if (!(data[3] >= 0 && data[3] <= 59)) {
                    throw new AssertionError();
                }
            }
            String id = ZoneMeta.formatCustomID(data[1], data[2], data[3], data[0] < 0);
            int offset = data[0] * ((((data[1] * 60) + data[2]) * 60) + data[3]) * 1000;
            SimpleTimeZone tz = new SimpleTimeZone(offset, id);
            tz.freeze();
            return tz;
        }
    }

    public static TimeZone getCustomTimeZone(String id) {
        int[] fields = new int[4];
        if (parseCustomID(id, fields)) {
            Integer key = Integer.valueOf(fields[0] * (fields[1] | (fields[2] << 5) | (fields[3] << 11)));
            return CUSTOM_ZONE_CACHE.getInstance(key, fields);
        }
        return null;
    }

    public static String getCustomID(String id) {
        int[] fields = new int[4];
        if (parseCustomID(id, fields)) {
            return formatCustomID(fields[1], fields[2], fields[3], fields[0] < 0);
        }
        return null;
    }

    static boolean parseCustomID(String id, int[] fields) {
        if (id != null && id.length() > "GMT".length() && id.toUpperCase(Locale.ENGLISH).startsWith("GMT")) {
            ParsePosition pos = new ParsePosition("GMT".length());
            int sign = 1;
            int min = 0;
            int sec = 0;
            if (id.charAt(pos.getIndex()) == '-') {
                sign = -1;
            } else if (id.charAt(pos.getIndex()) != '+') {
                return false;
            }
            pos.setIndex(pos.getIndex() + 1);
            NumberFormat numberFormat = NumberFormat.getInstance();
            numberFormat.setParseIntegerOnly(true);
            int start = pos.getIndex();
            Number n = numberFormat.parse(id, pos);
            if (pos.getIndex() == start) {
                return false;
            }
            int hour = n.intValue();
            if (pos.getIndex() < id.length()) {
                if (pos.getIndex() - start > 2 || id.charAt(pos.getIndex()) != ':') {
                    return false;
                }
                pos.setIndex(pos.getIndex() + 1);
                int oldPos = pos.getIndex();
                Number n2 = numberFormat.parse(id, pos);
                if (pos.getIndex() - oldPos != 2) {
                    return false;
                }
                min = n2.intValue();
                if (pos.getIndex() < id.length()) {
                    if (id.charAt(pos.getIndex()) != ':') {
                        return false;
                    }
                    pos.setIndex(pos.getIndex() + 1);
                    int oldPos2 = pos.getIndex();
                    Number n3 = numberFormat.parse(id, pos);
                    if (pos.getIndex() != id.length() || pos.getIndex() - oldPos2 != 2) {
                        return false;
                    }
                    sec = n3.intValue();
                }
            } else {
                int length = pos.getIndex() - start;
                if (length <= 0 || 6 < length) {
                    return false;
                }
                switch (length) {
                    case 3:
                    case 4:
                        min = hour % 100;
                        hour /= 100;
                        break;
                    case 5:
                    case 6:
                        sec = hour % 100;
                        min = (hour / 100) % 100;
                        hour /= 10000;
                        break;
                }
            }
            if (hour <= 23 && min <= 59 && sec <= 59) {
                if (fields != null) {
                    if (fields.length >= 1) {
                        fields[0] = sign;
                    }
                    if (fields.length >= 2) {
                        fields[1] = hour;
                    }
                    if (fields.length >= 3) {
                        fields[2] = min;
                    }
                    if (fields.length >= 4) {
                        fields[3] = sec;
                        return true;
                    }
                    return true;
                }
                return true;
            }
            return false;
        }
        return false;
    }

    public static TimeZone getCustomTimeZone(int offset) {
        boolean negative = false;
        int tmp = offset;
        if (offset < 0) {
            negative = true;
            tmp = -offset;
        }
        int tmp2 = tmp / 1000;
        int sec = tmp2 % 60;
        int tmp3 = tmp2 / 60;
        int min = tmp3 % 60;
        int hour = tmp3 / 60;
        String zid = formatCustomID(hour, min, sec, negative);
        return new SimpleTimeZone(offset, zid);
    }

    static String formatCustomID(int hour, int min, int sec, boolean negative) {
        StringBuilder zid = new StringBuilder("GMT");
        if (hour != 0 || min != 0) {
            if (negative) {
                zid.append('-');
            } else {
                zid.append('+');
            }
            if (hour < 10) {
                zid.append('0');
            }
            zid.append(hour);
            zid.append(':');
            if (min < 10) {
                zid.append('0');
            }
            zid.append(min);
            if (sec != 0) {
                zid.append(':');
                if (sec < 10) {
                    zid.append('0');
                }
                zid.append(sec);
            }
        }
        return zid.toString();
    }

    public static String getShortID(TimeZone tz) {
        if (tz instanceof OlsonTimeZone) {
            ((OlsonTimeZone) tz).getCanonicalID();
        }
        String canonicalID = getCanonicalCLDRID(tz.getID());
        if (canonicalID == null) {
            return null;
        }
        return getShortIDFromCanonical(canonicalID);
    }

    public static String getShortID(String id) {
        String canonicalID = getCanonicalCLDRID(id);
        if (canonicalID == null) {
            return null;
        }
        return getShortIDFromCanonical(canonicalID);
    }

    private static String getShortIDFromCanonical(String canonicalID) {
        String tzidKey = canonicalID.replace('/', ':');
        try {
            UResourceBundle keyTypeData = UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", "keyTypeData", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
            UResourceBundle typeMap = keyTypeData.get("typeMap");
            UResourceBundle typeKeys = typeMap.get("timezone");
            String shortID = typeKeys.getString(tzidKey);
            return shortID;
        } catch (MissingResourceException e) {
            return null;
        }
    }
}
