package java.util;

import android.icu.text.TimeZoneNames;
import java.io.IOException;
import java.io.Serializable;
import java.time.ZoneId;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import libcore.io.IoUtils;
import libcore.util.ZoneInfoDB;
import org.apache.harmony.luni.internal.util.TimezoneGetter;

public abstract class TimeZone implements Serializable, Cloneable {
    public static final int LONG = 1;
    public static final int SHORT = 0;
    private static volatile TimeZone defaultTimeZone = null;
    static final long serialVersionUID = 3581463369166924961L;
    private String ID;
    private static final TimeZone GMT = new SimpleTimeZone(0, "GMT");
    private static final TimeZone UTC = new SimpleTimeZone(0, "UTC");
    static final TimeZone NO_TIMEZONE = null;

    private static native String getSystemGMTOffsetID();

    private static native String getSystemTimeZoneID(String str, String str2);

    public abstract int getOffset(int i, int i2, int i3, int i4, int i5, int i6);

    public abstract int getRawOffset();

    public abstract boolean inDaylightTime(Date date);

    public abstract void setRawOffset(int i);

    public abstract boolean useDaylightTime();

    private static class NoImagePreloadHolder {
        public static final Pattern CUSTOM_ZONE_ID_PATTERN = Pattern.compile("^GMT[-+](\\d{1,2})(:?(\\d\\d))?$");

        private NoImagePreloadHolder() {
        }
    }

    public int getOffset(long j) {
        if (inDaylightTime(new Date(j))) {
            return getRawOffset() + getDSTSavings();
        }
        return getRawOffset();
    }

    int getOffsets(long j, int[] iArr) {
        int dSTSavings;
        int rawOffset = getRawOffset();
        if (inDaylightTime(new Date(j))) {
            dSTSavings = getDSTSavings();
        } else {
            dSTSavings = 0;
        }
        if (iArr != null) {
            iArr[0] = rawOffset;
            iArr[1] = dSTSavings;
        }
        return rawOffset + dSTSavings;
    }

    public String getID() {
        return this.ID;
    }

    public void setID(String str) {
        if (str == null) {
            throw new NullPointerException();
        }
        this.ID = str;
    }

    public final String getDisplayName() {
        return getDisplayName(false, 1, Locale.getDefault(Locale.Category.DISPLAY));
    }

    public final String getDisplayName(Locale locale) {
        return getDisplayName(false, 1, locale);
    }

    public final String getDisplayName(boolean z, int i) {
        return getDisplayName(z, i, Locale.getDefault(Locale.Category.DISPLAY));
    }

    public String getDisplayName(boolean z, int i, Locale locale) {
        TimeZoneNames.NameType nameType;
        String displayName;
        switch (i) {
            case 0:
                if (z) {
                    nameType = TimeZoneNames.NameType.SHORT_DAYLIGHT;
                } else {
                    nameType = TimeZoneNames.NameType.SHORT_STANDARD;
                }
                break;
            case 1:
                if (z) {
                    nameType = TimeZoneNames.NameType.LONG_DAYLIGHT;
                } else {
                    nameType = TimeZoneNames.NameType.LONG_STANDARD;
                }
                break;
            default:
                throw new IllegalArgumentException("Illegal style: " + i);
        }
        String canonicalID = android.icu.util.TimeZone.getCanonicalID(getID());
        if (canonicalID != null && (displayName = TimeZoneNames.getInstance(locale).getDisplayName(canonicalID, nameType, System.currentTimeMillis())) != null) {
            return displayName;
        }
        int rawOffset = getRawOffset();
        if (z) {
            rawOffset += getDSTSavings();
        }
        return createGmtOffsetString(true, true, rawOffset);
    }

    public static String createGmtOffsetString(boolean z, boolean z2, int i) {
        char c;
        int i2 = i / 60000;
        if (i2 < 0) {
            c = '-';
            i2 = -i2;
        } else {
            c = '+';
        }
        StringBuilder sb = new StringBuilder(9);
        if (z) {
            sb.append("GMT");
        }
        sb.append(c);
        appendNumber(sb, 2, i2 / 60);
        if (z2) {
            sb.append(':');
        }
        appendNumber(sb, 2, i2 % 60);
        return sb.toString();
    }

    private static void appendNumber(StringBuilder sb, int i, int i2) {
        String string = Integer.toString(i2);
        for (int i3 = 0; i3 < i - string.length(); i3++) {
            sb.append('0');
        }
        sb.append(string);
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

    public static synchronized TimeZone getTimeZone(String str) {
        if (str == null) {
            throw new NullPointerException("id == null");
        }
        if (str.length() == 3) {
            if (str.equals("GMT")) {
                return (TimeZone) GMT.clone();
            }
            if (str.equals("UTC")) {
                return (TimeZone) UTC.clone();
            }
        }
        TimeZone customTimeZone = null;
        try {
            customTimeZone = ZoneInfoDB.getInstance().makeTimeZone(str);
        } catch (IOException e) {
        }
        if (customTimeZone == null && str.length() > 3 && str.startsWith("GMT")) {
            customTimeZone = getCustomTimeZone(str);
        }
        if (customTimeZone == null) {
            customTimeZone = (TimeZone) GMT.clone();
        }
        return customTimeZone;
    }

    public static TimeZone getTimeZone(ZoneId zoneId) {
        String id = zoneId.getId();
        char cCharAt = id.charAt(0);
        if (cCharAt == '+' || cCharAt == '-') {
            id = "GMT" + id;
        } else if (cCharAt == 'Z' && id.length() == 1) {
            id = "UTC";
        }
        return getTimeZone(id);
    }

    public ZoneId toZoneId() {
        return ZoneId.of(getID(), ZoneId.SHORT_IDS);
    }

    private static TimeZone getCustomTimeZone(String str) {
        int i;
        Matcher matcher = NoImagePreloadHolder.CUSTOM_ZONE_ID_PATTERN.matcher(str);
        if (!matcher.matches()) {
            return null;
        }
        try {
            int i2 = Integer.parseInt(matcher.group(1));
            if (matcher.group(3) != null) {
                i = Integer.parseInt(matcher.group(3));
            } else {
                i = 0;
            }
            if (i2 < 0 || i2 > 23 || i < 0 || i > 59) {
                return null;
            }
            char cCharAt = str.charAt(3);
            int i3 = (3600000 * i2) + (60000 * i);
            if (cCharAt == '-') {
                i3 = -i3;
            }
            return new SimpleTimeZone(i3, String.format(Locale.ROOT, "GMT%c%02d:%02d", Character.valueOf(cCharAt), Integer.valueOf(i2), Integer.valueOf(i)));
        } catch (NumberFormatException e) {
            throw new AssertionError(e);
        }
    }

    public static synchronized String[] getAvailableIDs(int i) {
        return ZoneInfoDB.getInstance().getAvailableIDs(i);
    }

    public static synchronized String[] getAvailableIDs() {
        return ZoneInfoDB.getInstance().getAvailableIDs();
    }

    public static TimeZone getDefault() {
        return (TimeZone) getDefaultRef().clone();
    }

    static synchronized TimeZone getDefaultRef() {
        if (defaultTimeZone == null) {
            TimezoneGetter timezoneGetter = TimezoneGetter.getInstance();
            String id = timezoneGetter != null ? timezoneGetter.getId() : null;
            if (id != null) {
                id = id.trim();
            }
            if (id == null || id.isEmpty()) {
                try {
                    id = IoUtils.readFileAsString("/etc/timezone");
                } catch (IOException e) {
                    id = "GMT";
                }
            }
            defaultTimeZone = getTimeZone(id);
        }
        return defaultTimeZone;
    }

    public static synchronized void setDefault(TimeZone timeZone) {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(new PropertyPermission("user.timezone", "write"));
        }
        defaultTimeZone = timeZone != null ? (TimeZone) timeZone.clone() : null;
        android.icu.util.TimeZone.setICUDefault(null);
    }

    public boolean hasSameRules(TimeZone timeZone) {
        return timeZone != null && getRawOffset() == timeZone.getRawOffset() && useDaylightTime() == timeZone.useDaylightTime();
    }

    public Object clone() {
        try {
            TimeZone timeZone = (TimeZone) super.clone();
            timeZone.ID = this.ID;
            return timeZone;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
}
