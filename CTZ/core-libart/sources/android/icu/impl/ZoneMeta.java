package android.icu.impl;

import android.icu.text.NumberFormat;
import android.icu.util.Output;
import android.icu.util.SimpleTimeZone;
import android.icu.util.TimeZone;
import android.icu.util.UResourceBundle;
import dalvik.system.VMRuntime;
import java.lang.ref.SoftReference;
import java.text.ParsePosition;
import java.util.Collections;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.TreeSet;

public final class ZoneMeta {
    static final boolean $assertionsDisabled = false;
    private static final boolean ASSERT = false;
    private static final CustomTimeZoneCache CUSTOM_ZONE_CACHE;
    private static SoftReference<Set<String>> REF_CANONICAL_SYSTEM_LOCATION_ZONES = null;
    private static SoftReference<Set<String>> REF_CANONICAL_SYSTEM_ZONES = null;
    private static SoftReference<Set<String>> REF_SYSTEM_ZONES = null;
    private static final SystemTimeZoneCache SYSTEM_ZONE_CACHE;
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
    private static String[] ZONEIDS = null;
    private static ICUCache<String, String> CANONICAL_ID_CACHE = new SimpleCache();
    private static ICUCache<String, String> REGION_CACHE = new SimpleCache();
    private static ICUCache<String, Boolean> SINGLE_COUNTRY_CACHE = new SimpleCache();

    static {
        SYSTEM_ZONE_CACHE = new SystemTimeZoneCache();
        CUSTOM_ZONE_CACHE = new CustomTimeZoneCache();
    }

    private static synchronized Set<String> getSystemZIDs() {
        Set<String> setUnmodifiableSet;
        setUnmodifiableSet = null;
        if (REF_SYSTEM_ZONES != null) {
            setUnmodifiableSet = REF_SYSTEM_ZONES.get();
        }
        if (setUnmodifiableSet == null) {
            TreeSet treeSet = new TreeSet();
            for (String str : getZoneIDs()) {
                if (!str.equals(TimeZone.UNKNOWN_ZONE_ID)) {
                    treeSet.add(str);
                }
            }
            setUnmodifiableSet = Collections.unmodifiableSet(treeSet);
            REF_SYSTEM_ZONES = new SoftReference<>(setUnmodifiableSet);
        }
        return setUnmodifiableSet;
    }

    private static synchronized Set<String> getCanonicalSystemZIDs() {
        Set<String> setUnmodifiableSet;
        setUnmodifiableSet = null;
        if (REF_CANONICAL_SYSTEM_ZONES != null) {
            setUnmodifiableSet = REF_CANONICAL_SYSTEM_ZONES.get();
        }
        if (setUnmodifiableSet == null) {
            TreeSet treeSet = new TreeSet();
            for (String str : getZoneIDs()) {
                if (!str.equals(TimeZone.UNKNOWN_ZONE_ID) && str.equals(getCanonicalCLDRID(str))) {
                    treeSet.add(str);
                }
            }
            setUnmodifiableSet = Collections.unmodifiableSet(treeSet);
            REF_CANONICAL_SYSTEM_ZONES = new SoftReference<>(setUnmodifiableSet);
        }
        return setUnmodifiableSet;
    }

    private static synchronized Set<String> getCanonicalSystemLocationZIDs() {
        Set<String> setUnmodifiableSet;
        String region;
        setUnmodifiableSet = null;
        if (REF_CANONICAL_SYSTEM_LOCATION_ZONES != null) {
            setUnmodifiableSet = REF_CANONICAL_SYSTEM_LOCATION_ZONES.get();
        }
        if (setUnmodifiableSet == null) {
            TreeSet treeSet = new TreeSet();
            for (String str : getZoneIDs()) {
                if (!str.equals(TimeZone.UNKNOWN_ZONE_ID) && str.equals(getCanonicalCLDRID(str)) && (region = getRegion(str)) != null && !region.equals(kWorld)) {
                    treeSet.add(str);
                }
            }
            setUnmodifiableSet = Collections.unmodifiableSet(treeSet);
            REF_CANONICAL_SYSTEM_LOCATION_ZONES = new SoftReference<>(setUnmodifiableSet);
        }
        return setUnmodifiableSet;
    }

    public static Set<String> getAvailableIDs(TimeZone.SystemTimeZoneType systemTimeZoneType, String str, Integer num) {
        Set<String> systemZIDs;
        OlsonTimeZone systemTimeZone;
        switch (systemTimeZoneType) {
            case ANY:
                systemZIDs = getSystemZIDs();
                break;
            case CANONICAL:
                systemZIDs = getCanonicalSystemZIDs();
                break;
            case CANONICAL_LOCATION:
                systemZIDs = getCanonicalSystemLocationZIDs();
                break;
            default:
                throw new IllegalArgumentException("Unknown SystemTimeZoneType");
        }
        if (str == null && num == null) {
            return systemZIDs;
        }
        if (str != null) {
            str = str.toUpperCase(Locale.ENGLISH);
        }
        TreeSet treeSet = new TreeSet();
        for (String str2 : systemZIDs) {
            if (str == null || str.equals(getRegion(str2))) {
                if (num == null || ((systemTimeZone = getSystemTimeZone(str2)) != null && num.equals(Integer.valueOf(systemTimeZone.getRawOffset())))) {
                    treeSet.add(str2);
                }
            }
        }
        if (treeSet.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(treeSet);
    }

    public static synchronized int countEquivalentIDs(String str) {
        int length;
        UResourceBundle uResourceBundleOpenOlsonResource = openOlsonResource(null, str);
        if (uResourceBundleOpenOlsonResource != null) {
            try {
                length = uResourceBundleOpenOlsonResource.get("links").getIntVector().length;
            } catch (MissingResourceException e) {
                length = 0;
            }
        } else {
            length = 0;
        }
        return length;
    }

    public static synchronized String getEquivalentID(String str, int i) {
        String zoneID;
        if (i >= 0) {
            UResourceBundle uResourceBundleOpenOlsonResource = openOlsonResource(null, str);
            if (uResourceBundleOpenOlsonResource != null) {
                int i2 = -1;
                try {
                    int[] intVector = uResourceBundleOpenOlsonResource.get("links").getIntVector();
                    if (i < intVector.length) {
                        i2 = intVector[i];
                    }
                } catch (MissingResourceException e) {
                }
                if (i2 >= 0) {
                    zoneID = getZoneID(i2);
                    if (zoneID == null) {
                        zoneID = "";
                    }
                }
            }
        }
        return zoneID;
    }

    private static synchronized String[] getZoneIDs() {
        if (ZONEIDS == null) {
            try {
                ZONEIDS = UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, ZONEINFORESNAME, ICUResourceBundle.ICU_DATA_CLASS_LOADER).getStringArray(kNAMES);
            } catch (MissingResourceException e) {
            }
        }
        if (ZONEIDS == null) {
            ZONEIDS = new String[0];
        }
        return ZONEIDS;
    }

    private static String getZoneID(int i) {
        if (i >= 0) {
            String[] zoneIDs = getZoneIDs();
            if (i < zoneIDs.length) {
                return zoneIDs[i];
            }
            return null;
        }
        return null;
    }

    private static int getZoneIndex(String str) {
        String[] zoneIDs = getZoneIDs();
        if (zoneIDs.length > 0) {
            int i = 0;
            int length = zoneIDs.length;
            int i2 = Integer.MAX_VALUE;
            while (true) {
                int i3 = (i + length) / 2;
                if (i2 == i3) {
                    break;
                }
                int iCompareTo = str.compareTo(zoneIDs[i3]);
                if (iCompareTo == 0) {
                    return i3;
                }
                if (iCompareTo < 0) {
                    length = i3;
                } else {
                    i = i3;
                }
                i2 = i3;
            }
        }
        return -1;
    }

    public static String getCanonicalCLDRID(TimeZone timeZone) {
        if (timeZone instanceof OlsonTimeZone) {
            return ((OlsonTimeZone) timeZone).getCanonicalID();
        }
        return getCanonicalCLDRID(timeZone.getID());
    }

    public static String getCanonicalCLDRID(String str) {
        String strFindCLDRCanonicalID = CANONICAL_ID_CACHE.get(str);
        if (strFindCLDRCanonicalID == null) {
            strFindCLDRCanonicalID = findCLDRCanonicalID(str);
            if (strFindCLDRCanonicalID == null) {
                try {
                    int zoneIndex = getZoneIndex(str);
                    if (zoneIndex >= 0) {
                        UResourceBundle uResourceBundle = UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, ZONEINFORESNAME, ICUResourceBundle.ICU_DATA_CLASS_LOADER).get(kZONES).get(zoneIndex);
                        if (uResourceBundle.getType() == 7) {
                            String zoneID = getZoneID(uResourceBundle.getInt());
                            try {
                                strFindCLDRCanonicalID = findCLDRCanonicalID(zoneID);
                                str = zoneID;
                                if (strFindCLDRCanonicalID == null) {
                                    strFindCLDRCanonicalID = str;
                                }
                            } catch (MissingResourceException e) {
                                str = zoneID;
                            }
                        } else if (strFindCLDRCanonicalID == null) {
                        }
                    }
                } catch (MissingResourceException e2) {
                }
            }
            if (strFindCLDRCanonicalID != null) {
                CANONICAL_ID_CACHE.put(str, strFindCLDRCanonicalID);
            }
        }
        return strFindCLDRCanonicalID;
    }

    private static String findCLDRCanonicalID(String str) {
        String strReplace = str.replace('/', ':');
        String str2 = null;
        try {
            UResourceBundle bundleInstance = UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "keyTypeData", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
            try {
                bundleInstance.get("typeMap").get("timezone").get(strReplace);
                str2 = str;
            } catch (MissingResourceException e) {
            }
            if (str2 == null) {
                return bundleInstance.get("typeAlias").get("timezone").getString(strReplace);
            }
            return str2;
        } catch (MissingResourceException e2) {
            return str2;
        }
    }

    public static String getRegion(String str) {
        int zoneIndex;
        String string = REGION_CACHE.get(str);
        if (string == null && (zoneIndex = getZoneIndex(str)) >= 0) {
            try {
                UResourceBundle uResourceBundle = UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, ZONEINFORESNAME, ICUResourceBundle.ICU_DATA_CLASS_LOADER).get(kREGIONS);
                if (zoneIndex < uResourceBundle.getSize()) {
                    string = uResourceBundle.getString(zoneIndex);
                }
            } catch (MissingResourceException e) {
            }
            if (string != null) {
                REGION_CACHE.put(str, string);
            }
        }
        return string;
    }

    public static String getCanonicalCountry(String str) {
        String region = getRegion(str);
        if (region != null && region.equals(kWorld)) {
            return null;
        }
        return region;
    }

    public static String getCanonicalCountry(String str, Output<Boolean> output) {
        output.value = Boolean.FALSE;
        String region = getRegion(str);
        if (region != null && region.equals(kWorld)) {
            return null;
        }
        Boolean boolValueOf = SINGLE_COUNTRY_CACHE.get(str);
        if (boolValueOf == null) {
            boolValueOf = Boolean.valueOf(TimeZone.getAvailableIDs(TimeZone.SystemTimeZoneType.CANONICAL_LOCATION, region, null).size() <= 1);
            SINGLE_COUNTRY_CACHE.put(str, boolValueOf);
        }
        if (boolValueOf.booleanValue()) {
            output.value = Boolean.TRUE;
        } else {
            try {
                String string = UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "metaZones").get("primaryZones").getString(region);
                if (str.equals(string)) {
                    output.value = Boolean.TRUE;
                } else {
                    String canonicalCLDRID = getCanonicalCLDRID(str);
                    if (canonicalCLDRID != null && canonicalCLDRID.equals(string)) {
                        output.value = Boolean.TRUE;
                    }
                }
            } catch (MissingResourceException e) {
            }
        }
        return region;
    }

    public static UResourceBundle openOlsonResource(UResourceBundle uResourceBundle, String str) {
        int zoneIndex = getZoneIndex(str);
        if (zoneIndex < 0) {
            return null;
        }
        if (uResourceBundle == null) {
            try {
                uResourceBundle = UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, ZONEINFORESNAME, ICUResourceBundle.ICU_DATA_CLASS_LOADER);
            } catch (MissingResourceException e) {
                return null;
            }
        }
        UResourceBundle uResourceBundle2 = uResourceBundle.get(kZONES);
        UResourceBundle uResourceBundle3 = uResourceBundle2.get(zoneIndex);
        return uResourceBundle3.getType() == 7 ? uResourceBundle2.get(uResourceBundle3.getInt()) : uResourceBundle3;
    }

    private static class SystemTimeZoneCache extends SoftCache<String, OlsonTimeZone, String> {
        private SystemTimeZoneCache() {
        }

        @Override
        protected OlsonTimeZone createInstance(String str, String str2) {
            try {
                UResourceBundle bundleInstance = UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, ZoneMeta.ZONEINFORESNAME, ICUResourceBundle.ICU_DATA_CLASS_LOADER);
                UResourceBundle uResourceBundleOpenOlsonResource = ZoneMeta.openOlsonResource(bundleInstance, str2);
                if (uResourceBundleOpenOlsonResource == null) {
                    return null;
                }
                OlsonTimeZone olsonTimeZone = new OlsonTimeZone(bundleInstance, uResourceBundleOpenOlsonResource, str2);
                try {
                    olsonTimeZone.freeze();
                    return olsonTimeZone;
                } catch (MissingResourceException e) {
                    return olsonTimeZone;
                }
            } catch (MissingResourceException e2) {
                return null;
            }
        }
    }

    public static OlsonTimeZone getSystemTimeZone(String str) {
        return SYSTEM_ZONE_CACHE.getInstance(str, str);
    }

    private static class CustomTimeZoneCache extends SoftCache<Integer, SimpleTimeZone, int[]> {
        static final boolean $assertionsDisabled = false;

        private CustomTimeZoneCache() {
        }

        @Override
        protected SimpleTimeZone createInstance(Integer num, int[] iArr) {
            SimpleTimeZone simpleTimeZone = new SimpleTimeZone(iArr[0] * ((((iArr[1] * 60) + iArr[2]) * 60) + iArr[3]) * 1000, ZoneMeta.formatCustomID(iArr[1], iArr[2], iArr[3], iArr[0] < 0));
            simpleTimeZone.freeze();
            return simpleTimeZone;
        }
    }

    public static SimpleTimeZone getCustomTimeZone(String str) {
        int[] iArr = new int[4];
        if (parseCustomID(str, iArr)) {
            return CUSTOM_ZONE_CACHE.getInstance(Integer.valueOf(iArr[0] * (iArr[1] | (iArr[2] << 5) | (iArr[3] << 11))), iArr);
        }
        return null;
    }

    public static String getCustomID(String str) {
        int[] iArr = new int[4];
        if (parseCustomID(str, iArr)) {
            return formatCustomID(iArr[1], iArr[2], iArr[3], iArr[0] < 0);
        }
        return null;
    }

    static boolean parseCustomID(String str, int[] iArr) {
        int i;
        int i2;
        int i3;
        int iIntValue;
        if (str != null && str.length() > "GMT".length() && str.toUpperCase(Locale.ENGLISH).startsWith("GMT")) {
            ParsePosition parsePosition = new ParsePosition("GMT".length());
            if (str.charAt(parsePosition.getIndex()) == '-') {
                i = -1;
            } else {
                if (str.charAt(parsePosition.getIndex()) != '+') {
                    return false;
                }
                i = 1;
            }
            parsePosition.setIndex(parsePosition.getIndex() + 1);
            NumberFormat numberFormat = NumberFormat.getInstance();
            numberFormat.setParseIntegerOnly(true);
            int index = parsePosition.getIndex();
            Number number = numberFormat.parse(str, parsePosition);
            if (parsePosition.getIndex() == index) {
                return false;
            }
            int iIntValue2 = number.intValue();
            if (parsePosition.getIndex() >= str.length()) {
                int index2 = parsePosition.getIndex() - index;
                if (index2 > 0 && 6 >= index2) {
                    switch (index2) {
                        case 1:
                        case 2:
                        default:
                            i2 = 0;
                            i3 = 0;
                            break;
                        case 3:
                        case 4:
                            i2 = iIntValue2 % 100;
                            iIntValue2 /= 100;
                            i3 = 0;
                            break;
                        case 5:
                        case 6:
                            int i4 = iIntValue2 % 100;
                            int i5 = (iIntValue2 / 100) % 100;
                            iIntValue2 /= VMRuntime.SDK_VERSION_CUR_DEVELOPMENT;
                            i3 = i4;
                            i2 = i5;
                            break;
                    }
                } else {
                    return false;
                }
            } else {
                if (parsePosition.getIndex() - index > 2 || str.charAt(parsePosition.getIndex()) != ':') {
                    return false;
                }
                parsePosition.setIndex(parsePosition.getIndex() + 1);
                int index3 = parsePosition.getIndex();
                Number number2 = numberFormat.parse(str, parsePosition);
                if (parsePosition.getIndex() - index3 != 2) {
                    return false;
                }
                int iIntValue3 = number2.intValue();
                if (parsePosition.getIndex() >= str.length()) {
                    iIntValue = 0;
                } else {
                    if (str.charAt(parsePosition.getIndex()) != ':') {
                        return false;
                    }
                    parsePosition.setIndex(parsePosition.getIndex() + 1);
                    int index4 = parsePosition.getIndex();
                    Number number3 = numberFormat.parse(str, parsePosition);
                    if (parsePosition.getIndex() != str.length() || parsePosition.getIndex() - index4 != 2) {
                        return false;
                    }
                    iIntValue = number3.intValue();
                }
                i3 = iIntValue;
                i2 = iIntValue3;
            }
            if (iIntValue2 <= 23 && i2 <= 59 && i3 <= 59) {
                if (iArr != null) {
                    if (iArr.length >= 1) {
                        iArr[0] = i;
                    }
                    if (iArr.length >= 2) {
                        iArr[1] = iIntValue2;
                    }
                    if (iArr.length >= 3) {
                        iArr[2] = i2;
                    }
                    if (iArr.length >= 4) {
                        iArr[3] = i3;
                    }
                }
                return true;
            }
        }
        return false;
    }

    public static SimpleTimeZone getCustomTimeZone(int i) {
        boolean z;
        int i2;
        if (i < 0) {
            z = true;
            i2 = -i;
        } else {
            z = false;
            i2 = i;
        }
        int i3 = i2 / 1000;
        int i4 = i3 % 60;
        int i5 = i3 / 60;
        return new SimpleTimeZone(i, formatCustomID(i5 / 60, i5 % 60, i4, z));
    }

    static String formatCustomID(int i, int i2, int i3, boolean z) {
        StringBuilder sb = new StringBuilder("GMT");
        if (i != 0 || i2 != 0) {
            if (z) {
                sb.append('-');
            } else {
                sb.append('+');
            }
            if (i < 10) {
                sb.append('0');
            }
            sb.append(i);
            sb.append(':');
            if (i2 < 10) {
                sb.append('0');
            }
            sb.append(i2);
            if (i3 != 0) {
                sb.append(':');
                if (i3 < 10) {
                    sb.append('0');
                }
                sb.append(i3);
            }
        }
        return sb.toString();
    }

    public static String getShortID(TimeZone timeZone) {
        String canonicalCLDRID;
        if (timeZone instanceof OlsonTimeZone) {
            canonicalCLDRID = ((OlsonTimeZone) timeZone).getCanonicalID();
        } else {
            canonicalCLDRID = getCanonicalCLDRID(timeZone.getID());
        }
        if (canonicalCLDRID == null) {
            return null;
        }
        return getShortIDFromCanonical(canonicalCLDRID);
    }

    public static String getShortID(String str) {
        String canonicalCLDRID = getCanonicalCLDRID(str);
        if (canonicalCLDRID == null) {
            return null;
        }
        return getShortIDFromCanonical(canonicalCLDRID);
    }

    private static String getShortIDFromCanonical(String str) {
        try {
            return UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "keyTypeData", ICUResourceBundle.ICU_DATA_CLASS_LOADER).get("typeMap").get("timezone").getString(str.replace('/', ':'));
        } catch (MissingResourceException e) {
            return null;
        }
    }
}
