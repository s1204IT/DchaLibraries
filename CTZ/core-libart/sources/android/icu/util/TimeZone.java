package android.icu.util;

import android.icu.impl.Grego;
import android.icu.impl.ICUConfig;
import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.JavaTimeZone;
import android.icu.impl.OlsonTimeZone;
import android.icu.impl.TimeZoneAdapter;
import android.icu.impl.ZoneMeta;
import android.icu.impl.number.Padder;
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
    static final boolean $assertionsDisabled = false;
    public static final int GENERIC_LOCATION = 7;
    public static final TimeZone GMT_ZONE;
    static final String GMT_ZONE_ID = "Etc/GMT";
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
    private static final long serialVersionUID = -744942128318337471L;
    private String ID;
    private static final Logger LOGGER = Logger.getLogger("android.icu.util.TimeZone");
    private static volatile TimeZone defaultZone = null;

    public enum SystemTimeZoneType {
        ANY,
        CANONICAL,
        CANONICAL_LOCATION
    }

    public abstract int getOffset(int i, int i2, int i3, int i4, int i5, int i6);

    public abstract int getRawOffset();

    public abstract boolean inDaylightTime(Date date);

    public abstract void setRawOffset(int i);

    public abstract boolean useDaylightTime();

    static {
        int i = 0;
        UNKNOWN_ZONE = new ConstantZone(i, UNKNOWN_ZONE_ID).freeze();
        GMT_ZONE = new ConstantZone(i, GMT_ZONE_ID).freeze();
        TZ_IMPL = 0;
        if (ICUConfig.get(TZIMPL_CONFIG_KEY, TZIMPL_CONFIG_ICU).equalsIgnoreCase(TZIMPL_CONFIG_JDK)) {
            TZ_IMPL = 1;
        }
    }

    public TimeZone() {
    }

    @Deprecated
    protected TimeZone(String str) {
        if (str == null) {
            throw new NullPointerException();
        }
        this.ID = str;
    }

    public int getOffset(long j) {
        int[] iArr = new int[2];
        getOffset(j, false, iArr);
        return iArr[0] + iArr[1];
    }

    public void getOffset(long j, boolean z, int[] iArr) {
        iArr[0] = getRawOffset();
        if (!z) {
            j += (long) iArr[0];
        }
        int[] iArr2 = new int[6];
        int i = 0;
        while (true) {
            Grego.timeToFields(j, iArr2);
            iArr[1] = getOffset(1, iArr2[0], iArr2[1], iArr2[2], iArr2[3], iArr2[5]) - iArr[0];
            if (i == 0 && z && iArr[1] != 0) {
                j -= (long) iArr[1];
                i++;
            } else {
                return;
            }
        }
    }

    public String getID() {
        return this.ID;
    }

    public void setID(String str) {
        if (str == null) {
            throw new NullPointerException();
        }
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen TimeZone instance.");
        }
        this.ID = str;
    }

    public final String getDisplayName() {
        return _getDisplayName(3, false, ULocale.getDefault(ULocale.Category.DISPLAY));
    }

    public final String getDisplayName(Locale locale) {
        return _getDisplayName(3, false, ULocale.forLocale(locale));
    }

    public final String getDisplayName(ULocale uLocale) {
        return _getDisplayName(3, false, uLocale);
    }

    public final String getDisplayName(boolean z, int i) {
        return getDisplayName(z, i, ULocale.getDefault(ULocale.Category.DISPLAY));
    }

    public String getDisplayName(boolean z, int i, Locale locale) {
        return getDisplayName(z, i, ULocale.forLocale(locale));
    }

    public String getDisplayName(boolean z, int i, ULocale uLocale) {
        if (i < 0 || i > 7) {
            throw new IllegalArgumentException("Illegal style: " + i);
        }
        return _getDisplayName(i, z, uLocale);
    }

    private String _getDisplayName(int i, boolean z, ULocale uLocale) {
        if (uLocale == null) {
            throw new NullPointerException("locale is null");
        }
        String str = null;
        TimeZoneNames.NameType nameType = null;
        String offsetISO8601Basic = null;
        if (i == 7 || i == 3 || i == 2) {
            TimeZoneFormat timeZoneFormat = TimeZoneFormat.getInstance(uLocale);
            long jCurrentTimeMillis = System.currentTimeMillis();
            Output<TimeZoneFormat.TimeType> output = new Output<>(TimeZoneFormat.TimeType.UNKNOWN);
            if (i == 7) {
                str = timeZoneFormat.format(TimeZoneFormat.Style.GENERIC_LOCATION, this, jCurrentTimeMillis, output);
            } else {
                switch (i) {
                    case 2:
                        str = timeZoneFormat.format(TimeZoneFormat.Style.GENERIC_SHORT, this, jCurrentTimeMillis, output);
                        break;
                    case 3:
                        str = timeZoneFormat.format(TimeZoneFormat.Style.GENERIC_LONG, this, jCurrentTimeMillis, output);
                        break;
                }
            }
            if (!(z && output.value == TimeZoneFormat.TimeType.STANDARD) && (z || output.value != TimeZoneFormat.TimeType.DAYLIGHT)) {
                return str;
            }
            int rawOffset = z ? getRawOffset() + getDSTSavings() : getRawOffset();
            return i == 2 ? timeZoneFormat.formatOffsetShortLocalizedGMT(rawOffset) : timeZoneFormat.formatOffsetLocalizedGMT(rawOffset);
        }
        if (i == 5 || i == 4) {
            TimeZoneFormat timeZoneFormat2 = TimeZoneFormat.getInstance(uLocale);
            int rawOffset2 = (z && useDaylightTime()) ? getRawOffset() + getDSTSavings() : getRawOffset();
            switch (i) {
                case 4:
                    offsetISO8601Basic = timeZoneFormat2.formatOffsetISO8601Basic(rawOffset2, false, false, false);
                    break;
                case 5:
                    offsetISO8601Basic = timeZoneFormat2.formatOffsetLocalizedGMT(rawOffset2);
                    break;
            }
            return offsetISO8601Basic;
        }
        long jCurrentTimeMillis2 = System.currentTimeMillis();
        TimeZoneNames timeZoneNames = TimeZoneNames.getInstance(uLocale);
        if (i != 6) {
            switch (i) {
                case 0:
                    nameType = !z ? TimeZoneNames.NameType.SHORT_STANDARD : TimeZoneNames.NameType.SHORT_DAYLIGHT;
                    break;
                case 1:
                    nameType = !z ? TimeZoneNames.NameType.LONG_STANDARD : TimeZoneNames.NameType.LONG_DAYLIGHT;
                    break;
            }
        }
        String displayName = timeZoneNames.getDisplayName(ZoneMeta.getCanonicalCLDRID(this), nameType, jCurrentTimeMillis2);
        if (displayName == null) {
            TimeZoneFormat timeZoneFormat3 = TimeZoneFormat.getInstance(uLocale);
            int rawOffset3 = (z && useDaylightTime()) ? getRawOffset() + getDSTSavings() : getRawOffset();
            return i == 1 ? timeZoneFormat3.formatOffsetLocalizedGMT(rawOffset3) : timeZoneFormat3.formatOffsetShortLocalizedGMT(rawOffset3);
        }
        return displayName;
    }

    public int getDSTSavings() {
        if (useDaylightTime()) {
            return 3600000;
        }
        return 0;
    }

    public boolean observesDaylightTime() {
        return useDaylightTime() || inDaylightTime(new Date());
    }

    public static TimeZone getTimeZone(String str) {
        return getTimeZone(str, TZ_IMPL, false);
    }

    public static TimeZone getFrozenTimeZone(String str) {
        return getTimeZone(str, TZ_IMPL, true);
    }

    public static TimeZone getTimeZone(String str, int i) {
        return getTimeZone(str, i, false);
    }

    private static TimeZone getTimeZone(String str, int i, boolean z) {
        TimeZone frozenICUTimeZone;
        if (i == 1) {
            JavaTimeZone javaTimeZoneCreateTimeZone = JavaTimeZone.createTimeZone(str);
            if (javaTimeZoneCreateTimeZone != null) {
                return z ? javaTimeZoneCreateTimeZone.freeze() : javaTimeZoneCreateTimeZone;
            }
            frozenICUTimeZone = getFrozenICUTimeZone(str, false);
        } else {
            frozenICUTimeZone = getFrozenICUTimeZone(str, true);
        }
        if (frozenICUTimeZone == null) {
            LOGGER.fine("\"" + str + "\" is a bogus id so timezone is falling back to Etc/Unknown(GMT).");
            frozenICUTimeZone = UNKNOWN_ZONE;
        }
        return z ? frozenICUTimeZone : frozenICUTimeZone.cloneAsThawed();
    }

    static BasicTimeZone getFrozenICUTimeZone(String str, boolean z) {
        OlsonTimeZone systemTimeZone;
        if (z) {
            systemTimeZone = ZoneMeta.getSystemTimeZone(str);
        } else {
            systemTimeZone = null;
        }
        if (systemTimeZone == null) {
            return ZoneMeta.getCustomTimeZone(str);
        }
        return systemTimeZone;
    }

    public static synchronized void setDefaultTimeZoneType(int i) {
        if (i != 0 && i != 1) {
            throw new IllegalArgumentException("Invalid timezone type");
        }
        TZ_IMPL = i;
    }

    public static int getDefaultTimeZoneType() {
        return TZ_IMPL;
    }

    public static Set<String> getAvailableIDs(SystemTimeZoneType systemTimeZoneType, String str, Integer num) {
        return ZoneMeta.getAvailableIDs(systemTimeZoneType, str, num);
    }

    public static String[] getAvailableIDs(int i) {
        return (String[]) getAvailableIDs(SystemTimeZoneType.ANY, null, Integer.valueOf(i)).toArray(new String[0]);
    }

    public static String[] getAvailableIDs(String str) {
        return (String[]) getAvailableIDs(SystemTimeZoneType.ANY, str, null).toArray(new String[0]);
    }

    public static String[] getAvailableIDs() {
        return (String[]) getAvailableIDs(SystemTimeZoneType.ANY, null, null).toArray(new String[0]);
    }

    public static int countEquivalentIDs(String str) {
        return ZoneMeta.countEquivalentIDs(str);
    }

    public static String getEquivalentID(String str, int i) {
        return ZoneMeta.getEquivalentID(str, i);
    }

    public static TimeZone getDefault() {
        TimeZone frozenTimeZone;
        TimeZone timeZone = defaultZone;
        if (timeZone == null) {
            synchronized (java.util.TimeZone.class) {
                synchronized (TimeZone.class) {
                    frozenTimeZone = defaultZone;
                    if (frozenTimeZone == null) {
                        if (TZ_IMPL == 1) {
                            frozenTimeZone = new JavaTimeZone();
                        } else {
                            frozenTimeZone = getFrozenTimeZone(java.util.TimeZone.getDefault().getID());
                        }
                        defaultZone = frozenTimeZone;
                    }
                }
            }
            timeZone = frozenTimeZone;
        }
        return timeZone.cloneAsThawed();
    }

    public static synchronized void setDefault(TimeZone timeZone) {
        setICUDefault(timeZone);
        if (timeZone != null) {
            java.util.TimeZone timeZoneWrap = null;
            if (timeZone instanceof JavaTimeZone) {
                timeZoneWrap = ((JavaTimeZone) timeZone).unwrap();
            } else if (timeZone instanceof OlsonTimeZone) {
                String id = timeZone.getID();
                java.util.TimeZone timeZone2 = java.util.TimeZone.getTimeZone(id);
                if (!id.equals(timeZone2.getID())) {
                    String canonicalID = getCanonicalID(id);
                    timeZone2 = java.util.TimeZone.getTimeZone(canonicalID);
                    if (!canonicalID.equals(timeZone2.getID())) {
                    }
                }
                timeZoneWrap = timeZone2;
            }
            if (timeZoneWrap == null) {
                timeZoneWrap = TimeZoneAdapter.wrap(timeZone);
            }
            java.util.TimeZone.setDefault(timeZoneWrap);
        }
    }

    @Deprecated
    public static synchronized void setICUDefault(TimeZone timeZone) {
        try {
            if (timeZone == null) {
                defaultZone = null;
            } else if (timeZone.isFrozen()) {
                defaultZone = timeZone;
            } else {
                defaultZone = ((TimeZone) timeZone.clone()).freeze();
            }
        } catch (Throwable th) {
            throw th;
        }
    }

    public boolean hasSameRules(TimeZone timeZone) {
        return timeZone != null && getRawOffset() == timeZone.getRawOffset() && useDaylightTime() == timeZone.useDaylightTime();
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

    public static String getCanonicalID(String str) {
        return getCanonicalID(str, null);
    }

    public static String getCanonicalID(String str, boolean[] zArr) {
        String customID;
        boolean z;
        String canonicalCLDRID;
        if (str != null && str.length() != 0) {
            if (str.equals(UNKNOWN_ZONE_ID)) {
                customID = UNKNOWN_ZONE_ID;
            } else {
                canonicalCLDRID = ZoneMeta.getCanonicalCLDRID(str);
                if (canonicalCLDRID != null) {
                    z = true;
                    if (zArr != null) {
                        zArr[0] = z;
                    }
                    return canonicalCLDRID;
                }
                customID = ZoneMeta.getCustomID(str);
            }
        } else {
            customID = null;
        }
        canonicalCLDRID = customID;
        z = false;
        if (zArr != null) {
        }
        return canonicalCLDRID;
    }

    public static String getRegion(String str) {
        String region;
        if (!str.equals(UNKNOWN_ZONE_ID)) {
            region = ZoneMeta.getRegion(str);
        } else {
            region = null;
        }
        if (region == null) {
            throw new IllegalArgumentException("Unknown system zone id: " + str);
        }
        return region;
    }

    public static String getWindowsID(String str) {
        boolean[] zArr = {false};
        String canonicalID = getCanonicalID(str, zArr);
        if (!zArr[0]) {
            return null;
        }
        UResourceBundleIterator iterator = UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "windowsZones", ICUResourceBundle.ICU_DATA_CLASS_LOADER).get("mapTimezones").getIterator();
        while (iterator.hasNext()) {
            UResourceBundle next = iterator.next();
            if (next.getType() == 2) {
                UResourceBundleIterator iterator2 = next.getIterator();
                while (iterator2.hasNext()) {
                    UResourceBundle next2 = iterator2.next();
                    if (next2.getType() == 0) {
                        for (String str2 : next2.getString().split(Padder.FALLBACK_PADDING_STRING)) {
                            if (str2.equals(canonicalID)) {
                                return next.getKey();
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public static String getIDForWindowsID(String str, String str2) {
        String strSubstring = null;
        try {
            UResourceBundle uResourceBundle = UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "windowsZones", ICUResourceBundle.ICU_DATA_CLASS_LOADER).get("mapTimezones").get(str);
            if (str2 != null) {
                try {
                    String string = uResourceBundle.getString(str2);
                    if (string != null) {
                        try {
                            int iIndexOf = string.indexOf(32);
                            strSubstring = iIndexOf > 0 ? string.substring(0, iIndexOf) : string;
                        } catch (MissingResourceException e) {
                            strSubstring = string;
                        }
                    }
                } catch (MissingResourceException e2) {
                }
            }
            if (strSubstring == null) {
                return uResourceBundle.getString("001");
            }
            return strSubstring;
        } catch (MissingResourceException e3) {
            return strSubstring;
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
            return (TimeZone) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException(e);
        }
    }

    private static final class ConstantZone extends TimeZone {
        private static final long serialVersionUID = 1;
        private volatile transient boolean isFrozen;
        private int rawOffset;

        private ConstantZone(int i, String str) {
            super(str);
            this.isFrozen = false;
            this.rawOffset = i;
        }

        @Override
        public int getOffset(int i, int i2, int i3, int i4, int i5, int i6) {
            return this.rawOffset;
        }

        @Override
        public void setRawOffset(int i) {
            if (isFrozen()) {
                throw new UnsupportedOperationException("Attempt to modify a frozen TimeZone instance.");
            }
            this.rawOffset = i;
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
            ConstantZone constantZone = (ConstantZone) super.cloneAsThawed();
            constantZone.isFrozen = false;
            return constantZone;
        }
    }
}
