package android.icu.util;

import android.icu.impl.Grego;
import android.icu.impl.ICUConfig;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.JavaTimeZone;
import android.icu.impl.OlsonTimeZone;
import android.icu.impl.TimeZoneAdapter;
import android.icu.impl.ZoneMeta;
import android.icu.text.TimeZoneFormat;
import android.icu.text.TimeZoneNames;
import android.icu.util.ULocale;
import java.io.Serializable;
import java.util.Date;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.logging.Logger;

public abstract class TimeZone implements Serializable, Cloneable, Freezable<TimeZone> {

    static final boolean f117assertionsDisabled;
    public static final int GENERIC_LOCATION = 7;
    public static final TimeZone GMT_ZONE;
    static final String GMT_ZONE_ID = "Etc/GMT";
    private static final Logger LOGGER;
    public static final int LONG = 1;
    public static final int LONG_GENERIC = 3;
    public static final int LONG_GMT = 5;
    public static final int SHORT = 0;
    public static final int SHORT_COMMONLY_USED = 6;
    public static final int SHORT_GENERIC = 2;
    public static final int SHORT_GMT = 4;
    public static final int TIMEZONE_ICU = 0;
    public static final int TIMEZONE_JDK = 1;
    private static final String TZIMPL_CONFIG_ICU = "ICU";
    private static final String TZIMPL_CONFIG_JDK = "JDK";
    private static final String TZIMPL_CONFIG_KEY = "android.icu.util.TimeZone.DefaultTimeZoneType";
    private static int TZ_IMPL = 0;
    public static final TimeZone UNKNOWN_ZONE;
    public static final String UNKNOWN_ZONE_ID = "Etc/Unknown";
    private static volatile TimeZone defaultZone = null;
    private static final long serialVersionUID = -744942128318337471L;
    private String ID;

    public abstract int getOffset(int i, int i2, int i3, int i4, int i5, int i6);

    public abstract int getRawOffset();

    public abstract boolean inDaylightTime(Date date);

    public abstract void setRawOffset(int i);

    public abstract boolean useDaylightTime();

    static {
        ConstantZone constantZone = null;
        int i = 0;
        f117assertionsDisabled = !TimeZone.class.desiredAssertionStatus();
        LOGGER = Logger.getLogger("android.icu.util.TimeZone");
        UNKNOWN_ZONE = new ConstantZone(i, UNKNOWN_ZONE_ID, constantZone).freeze();
        GMT_ZONE = new ConstantZone(i, GMT_ZONE_ID, constantZone).freeze();
        defaultZone = null;
        TZ_IMPL = 0;
        String type = ICUConfig.get(TZIMPL_CONFIG_KEY, TZIMPL_CONFIG_ICU);
        if (!type.equalsIgnoreCase(TZIMPL_CONFIG_JDK)) {
            return;
        }
        TZ_IMPL = 1;
    }

    public TimeZone() {
    }

    @Deprecated
    protected TimeZone(String ID) {
        if (ID == null) {
            throw new NullPointerException();
        }
        this.ID = ID;
    }

    public enum SystemTimeZoneType {
        ANY,
        CANONICAL,
        CANONICAL_LOCATION;

        public static SystemTimeZoneType[] valuesCustom() {
            return values();
        }
    }

    public int getOffset(long date) {
        int[] result = new int[2];
        getOffset(date, false, result);
        return result[0] + result[1];
    }

    public void getOffset(long date, boolean local, int[] offsets) {
        offsets[0] = getRawOffset();
        if (!local) {
            date += (long) offsets[0];
        }
        int[] fields = new int[6];
        int pass = 0;
        while (true) {
            Grego.timeToFields(date, fields);
            offsets[1] = getOffset(1, fields[0], fields[1], fields[2], fields[3], fields[5]) - offsets[0];
            if (pass != 0 || !local || offsets[1] == 0) {
                return;
            }
            date -= (long) offsets[1];
            pass++;
        }
    }

    public String getID() {
        return this.ID;
    }

    public void setID(String ID) {
        if (ID == null) {
            throw new NullPointerException();
        }
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen TimeZone instance.");
        }
        this.ID = ID;
    }

    public final String getDisplayName() {
        return _getDisplayName(3, false, ULocale.getDefault(ULocale.Category.DISPLAY));
    }

    public final String getDisplayName(Locale locale) {
        return _getDisplayName(3, false, ULocale.forLocale(locale));
    }

    public final String getDisplayName(ULocale locale) {
        return _getDisplayName(3, false, locale);
    }

    public final String getDisplayName(boolean daylight, int style) {
        return getDisplayName(daylight, style, ULocale.getDefault(ULocale.Category.DISPLAY));
    }

    public String getDisplayName(boolean daylight, int style, Locale locale) {
        return getDisplayName(daylight, style, ULocale.forLocale(locale));
    }

    public String getDisplayName(boolean daylight, int style, ULocale locale) {
        if (style < 0 || style > 7) {
            throw new IllegalArgumentException("Illegal style: " + style);
        }
        return _getDisplayName(style, daylight, locale);
    }

    private String _getDisplayName(int style, boolean daylight, ULocale locale) {
        if (locale == null) {
            throw new NullPointerException("locale is null");
        }
        String result = null;
        if (style == 7 || style == 3 || style == 2) {
            TimeZoneFormat tzfmt = TimeZoneFormat.getInstance(locale);
            long date = System.currentTimeMillis();
            Output<TimeZoneFormat.TimeType> timeType = new Output<>(TimeZoneFormat.TimeType.UNKNOWN);
            switch (style) {
                case 2:
                    result = tzfmt.format(TimeZoneFormat.Style.GENERIC_SHORT, this, date, timeType);
                    break;
                case 3:
                    result = tzfmt.format(TimeZoneFormat.Style.GENERIC_LONG, this, date, timeType);
                    break;
                case 7:
                    result = tzfmt.format(TimeZoneFormat.Style.GENERIC_LOCATION, this, date, timeType);
                    break;
            }
            if ((daylight && timeType.value == TimeZoneFormat.TimeType.STANDARD) || (!daylight && timeType.value == TimeZoneFormat.TimeType.DAYLIGHT)) {
                int offset = daylight ? getRawOffset() + getDSTSavings() : getRawOffset();
                result = style == 2 ? tzfmt.formatOffsetShortLocalizedGMT(offset) : tzfmt.formatOffsetLocalizedGMT(offset);
            }
        } else if (style == 5 || style == 4) {
            TimeZoneFormat tzfmt2 = TimeZoneFormat.getInstance(locale);
            int offset2 = (daylight && useDaylightTime()) ? getRawOffset() + getDSTSavings() : getRawOffset();
            switch (style) {
                case 4:
                    result = tzfmt2.formatOffsetISO8601Basic(offset2, false, false, false);
                    break;
                case 5:
                    result = tzfmt2.formatOffsetLocalizedGMT(offset2);
                    break;
            }
        } else {
            if (!f117assertionsDisabled) {
                if (!(style == 1 || style == 0 || style == 6)) {
                    throw new AssertionError();
                }
            }
            long date2 = System.currentTimeMillis();
            TimeZoneNames tznames = TimeZoneNames.getInstance(locale);
            TimeZoneNames.NameType nameType = null;
            switch (style) {
                case 0:
                case 6:
                    nameType = !daylight ? TimeZoneNames.NameType.SHORT_STANDARD : TimeZoneNames.NameType.SHORT_DAYLIGHT;
                    break;
                case 1:
                    nameType = !daylight ? TimeZoneNames.NameType.LONG_STANDARD : TimeZoneNames.NameType.LONG_DAYLIGHT;
                    break;
            }
            result = tznames.getDisplayName(ZoneMeta.getCanonicalCLDRID(this), nameType, date2);
            if (result == null) {
                TimeZoneFormat tzfmt3 = TimeZoneFormat.getInstance(locale);
                int offset3 = (daylight && useDaylightTime()) ? getRawOffset() + getDSTSavings() : getRawOffset();
                result = style == 1 ? tzfmt3.formatOffsetLocalizedGMT(offset3) : tzfmt3.formatOffsetShortLocalizedGMT(offset3);
            }
        }
        if (!f117assertionsDisabled) {
            if (!(result != null)) {
                throw new AssertionError();
            }
        }
        return result;
    }

    public int getDSTSavings() {
        if (useDaylightTime()) {
            return 3600000;
        }
        return 0;
    }

    public boolean observesDaylightTime() {
        if (useDaylightTime()) {
            return true;
        }
        return inDaylightTime(new Date());
    }

    public static TimeZone getTimeZone(String ID) {
        return getTimeZone(ID, TZ_IMPL, false);
    }

    public static TimeZone getFrozenTimeZone(String ID) {
        return getTimeZone(ID, TZ_IMPL, true);
    }

    public static TimeZone getTimeZone(String ID, int type) {
        return getTimeZone(ID, type, false);
    }

    private static TimeZone getTimeZone(String ID, int type, boolean frozen) {
        TimeZone result;
        if (type == 1) {
            result = JavaTimeZone.createTimeZone(ID);
            if (result != null) {
                return frozen ? result.freeze() : result;
            }
        } else {
            if (ID == null) {
                throw new NullPointerException();
            }
            result = ZoneMeta.getSystemTimeZone(ID);
        }
        if (result == null) {
            result = ZoneMeta.getCustomTimeZone(ID);
        }
        if (result == null) {
            LOGGER.fine("\"" + ID + "\" is a bogus id so timezone is falling back to Etc/Unknown(GMT).");
            result = UNKNOWN_ZONE;
        }
        return frozen ? result : result.cloneAsThawed();
    }

    public static synchronized void setDefaultTimeZoneType(int type) {
        if (type != 0 && type != 1) {
            throw new IllegalArgumentException("Invalid timezone type");
        }
        TZ_IMPL = type;
    }

    public static int getDefaultTimeZoneType() {
        return TZ_IMPL;
    }

    public static Set<String> getAvailableIDs(SystemTimeZoneType zoneType, String region, Integer rawOffset) {
        return ZoneMeta.getAvailableIDs(zoneType, region, rawOffset);
    }

    public static String[] getAvailableIDs(int rawOffset) {
        Set<String> ids = getAvailableIDs(SystemTimeZoneType.ANY, null, Integer.valueOf(rawOffset));
        return (String[]) ids.toArray(new String[0]);
    }

    public static String[] getAvailableIDs(String country) {
        Set<String> ids = getAvailableIDs(SystemTimeZoneType.ANY, country, null);
        return (String[]) ids.toArray(new String[0]);
    }

    public static String[] getAvailableIDs() {
        Set<String> ids = getAvailableIDs(SystemTimeZoneType.ANY, null, null);
        return (String[]) ids.toArray(new String[0]);
    }

    public static int countEquivalentIDs(String id) {
        return ZoneMeta.countEquivalentIDs(id);
    }

    public static String getEquivalentID(String id, int index) {
        return ZoneMeta.getEquivalentID(id, index);
    }

    public static TimeZone getDefault() {
        if (defaultZone == null) {
            synchronized (TimeZone.class) {
                if (defaultZone == null) {
                    if (TZ_IMPL == 1) {
                        defaultZone = new JavaTimeZone();
                    } else {
                        java.util.TimeZone temp = java.util.TimeZone.getDefault();
                        defaultZone = getFrozenTimeZone(temp.getID());
                    }
                }
            }
        }
        return defaultZone.cloneAsThawed();
    }

    public static synchronized void clearCachedDefault() {
        defaultZone = null;
    }

    public static synchronized void setDefault(TimeZone tz) {
        defaultZone = tz;
        java.util.TimeZone jdkZone = null;
        if (defaultZone instanceof JavaTimeZone) {
            jdkZone = ((JavaTimeZone) defaultZone).unwrap();
        } else if (tz != null) {
            if (tz instanceof OlsonTimeZone) {
                String icuID = tz.getID();
                jdkZone = java.util.TimeZone.getTimeZone(icuID);
                if (!icuID.equals(jdkZone.getID())) {
                    String icuID2 = getCanonicalID(icuID);
                    jdkZone = java.util.TimeZone.getTimeZone(icuID2);
                    if (!icuID2.equals(jdkZone.getID())) {
                        jdkZone = null;
                    }
                }
            }
            if (jdkZone == null) {
                jdkZone = TimeZoneAdapter.wrap(tz);
            }
        }
        java.util.TimeZone.setDefault(jdkZone);
    }

    public boolean hasSameRules(TimeZone other) {
        return other != null && getRawOffset() == other.getRawOffset() && useDaylightTime() == other.useDaylightTime();
    }

    public Object clone() {
        if (isFrozen()) {
            return this;
        }
        return cloneAsThawed();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return this.ID.equals(((TimeZone) obj).ID);
    }

    public int hashCode() {
        return this.ID.hashCode();
    }

    public static String getTZDataVersion() {
        return VersionInfo.getTZDataVersion();
    }

    public static String getCanonicalID(String id) {
        return getCanonicalID(id, null);
    }

    public static String getCanonicalID(String id, boolean[] isSystemID) {
        String canonicalID = null;
        boolean systemTzid = false;
        if (id != null && id.length() != 0) {
            if (id.equals(UNKNOWN_ZONE_ID)) {
                canonicalID = UNKNOWN_ZONE_ID;
                systemTzid = false;
            } else {
                canonicalID = ZoneMeta.getCanonicalCLDRID(id);
                if (canonicalID != null) {
                    systemTzid = true;
                } else {
                    canonicalID = ZoneMeta.getCustomID(id);
                }
            }
        }
        if (isSystemID != null) {
            isSystemID[0] = systemTzid;
        }
        return canonicalID;
    }

    public static String getRegion(String id) {
        String region = null;
        if (!id.equals(UNKNOWN_ZONE_ID)) {
            region = ZoneMeta.getRegion(id);
        }
        if (region == null) {
            throw new IllegalArgumentException("Unknown system zone id: " + id);
        }
        return region;
    }

    public static String getWindowsID(String id) {
        boolean[] isSystemID = {false};
        String id2 = getCanonicalID(id, isSystemID);
        if (!isSystemID[0]) {
            return null;
        }
        UResourceBundle top = UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", "windowsZones", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        UResourceBundle mapTimezones = top.get("mapTimezones");
        UResourceBundleIterator resitr = mapTimezones.getIterator();
        while (resitr.hasNext()) {
            UResourceBundle winzone = resitr.next();
            if (winzone.getType() == 2) {
                UResourceBundleIterator rgitr = winzone.getIterator();
                while (rgitr.hasNext()) {
                    UResourceBundle regionalData = rgitr.next();
                    if (regionalData.getType() == 0) {
                        String[] tzids = regionalData.getString().split(" ");
                        for (String tzid : tzids) {
                            if (tzid.equals(id2)) {
                                return winzone.getKey();
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public static String getIDForWindowsID(String winid, String region) {
        int endIdx;
        String id = null;
        UResourceBundle top = UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", "windowsZones", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        UResourceBundle mapTimezones = top.get("mapTimezones");
        try {
            UResourceBundle zones = mapTimezones.get(winid);
            if (region != null) {
                try {
                    id = zones.getString(region);
                    if (id != null && (endIdx = id.indexOf(32)) > 0) {
                        id = id.substring(0, endIdx);
                    }
                } catch (MissingResourceException e) {
                }
            }
            if (id == null) {
                return zones.getString("001");
            }
            return id;
        } catch (MissingResourceException e2) {
            return null;
        }
    }

    public boolean isFrozen() {
        return false;
    }

    public TimeZone freeze() {
        throw new UnsupportedOperationException("Needs to be implemented by the subclass.");
    }

    public TimeZone cloneAsThawed() {
        try {
            TimeZone other = (TimeZone) super.clone();
            return other;
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException(e);
        }
    }

    private static final class ConstantZone extends TimeZone {
        private static final long serialVersionUID = 1;
        private volatile transient boolean isFrozen;
        private int rawOffset;

        ConstantZone(int rawOffset, String ID, ConstantZone constantZone) {
            this(rawOffset, ID);
        }

        private ConstantZone(int rawOffset, String ID) {
            super(ID);
            this.isFrozen = false;
            this.rawOffset = rawOffset;
        }

        @Override
        public int getOffset(int era, int year, int month, int day, int dayOfWeek, int milliseconds) {
            return this.rawOffset;
        }

        @Override
        public void setRawOffset(int offsetMillis) {
            if (isFrozen()) {
                throw new UnsupportedOperationException("Attempt to modify a frozen TimeZone instance.");
            }
            this.rawOffset = offsetMillis;
        }

        @Override
        public int getRawOffset() {
            return this.rawOffset;
        }

        @Override
        public boolean useDaylightTime() {
            return false;
        }

        @Override
        public boolean inDaylightTime(Date date) {
            return false;
        }

        @Override
        public boolean isFrozen() {
            return this.isFrozen;
        }

        @Override
        public TimeZone freeze() {
            this.isFrozen = true;
            return this;
        }

        @Override
        public TimeZone cloneAsThawed() {
            ConstantZone tz = (ConstantZone) super.cloneAsThawed();
            tz.isFrozen = false;
            return tz;
        }
    }
}
