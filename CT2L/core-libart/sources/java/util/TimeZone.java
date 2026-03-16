package java.util;

import java.io.IOException;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import libcore.icu.TimeZoneNames;
import libcore.io.IoUtils;
import libcore.util.ZoneInfoDB;
import org.apache.harmony.luni.internal.util.TimezoneGetter;

public abstract class TimeZone implements Serializable, Cloneable {
    public static final int LONG = 1;
    public static final int SHORT = 0;
    private static TimeZone defaultTimeZone = null;
    private static final long serialVersionUID = 3581463369166924961L;
    private String ID;
    private static final Pattern CUSTOM_ZONE_ID_PATTERN = Pattern.compile("^GMT[-+](\\d{1,2})(:?(\\d\\d))?$");
    private static final TimeZone GMT = new SimpleTimeZone(0, "GMT");
    private static final TimeZone UTC = new SimpleTimeZone(0, "UTC");

    public abstract int getOffset(int i, int i2, int i3, int i4, int i5, int i6);

    public abstract int getRawOffset();

    public abstract boolean inDaylightTime(Date date);

    public abstract void setRawOffset(int i);

    public abstract boolean useDaylightTime();

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public static synchronized String[] getAvailableIDs() {
        return ZoneInfoDB.getInstance().getAvailableIDs();
    }

    public static synchronized String[] getAvailableIDs(int offsetMillis) {
        return ZoneInfoDB.getInstance().getAvailableIDs(offsetMillis);
    }

    public static synchronized TimeZone getDefault() {
        if (defaultTimeZone == null) {
            TimezoneGetter tzGetter = TimezoneGetter.getInstance();
            String zoneName = tzGetter != null ? tzGetter.getId() : null;
            if (zoneName != null) {
                zoneName = zoneName.trim();
            }
            if (zoneName == null || zoneName.isEmpty()) {
                try {
                    zoneName = IoUtils.readFileAsString("/etc/timezone");
                } catch (IOException e) {
                    zoneName = "GMT";
                }
            }
            defaultTimeZone = getTimeZone(zoneName);
        }
        return (TimeZone) defaultTimeZone.clone();
    }

    public final String getDisplayName() {
        return getDisplayName(false, 1, Locale.getDefault());
    }

    public final String getDisplayName(Locale locale) {
        return getDisplayName(false, 1, locale);
    }

    public final String getDisplayName(boolean daylightTime, int style) {
        return getDisplayName(daylightTime, style, Locale.getDefault());
    }

    public String getDisplayName(boolean daylightTime, int style, Locale locale) {
        if (style != 0 && style != 1) {
            throw new IllegalArgumentException("Bad style: " + style);
        }
        String[][] zoneStrings = TimeZoneNames.getZoneStrings(locale);
        String result = TimeZoneNames.getDisplayName(zoneStrings, getID(), daylightTime, style);
        if (result == null) {
            int offsetMillis = getRawOffset();
            if (daylightTime) {
                offsetMillis += getDSTSavings();
            }
            return createGmtOffsetString(true, true, offsetMillis);
        }
        return result;
    }

    public static String createGmtOffsetString(boolean includeGmt, boolean includeMinuteSeparator, int offsetMillis) {
        int offsetMinutes = offsetMillis / Grego.MILLIS_PER_MINUTE;
        char sign = '+';
        if (offsetMinutes < 0) {
            sign = '-';
            offsetMinutes = -offsetMinutes;
        }
        StringBuilder builder = new StringBuilder(9);
        if (includeGmt) {
            builder.append("GMT");
        }
        builder.append(sign);
        appendNumber(builder, 2, offsetMinutes / 60);
        if (includeMinuteSeparator) {
            builder.append(':');
        }
        appendNumber(builder, 2, offsetMinutes % 60);
        return builder.toString();
    }

    private static void appendNumber(StringBuilder builder, int count, int value) {
        String string = Integer.toString(value);
        for (int i = 0; i < count - string.length(); i++) {
            builder.append('0');
        }
        builder.append(string);
    }

    public String getID() {
        return this.ID;
    }

    public int getDSTSavings() {
        if (useDaylightTime()) {
            return Grego.MILLIS_PER_HOUR;
        }
        return 0;
    }

    public int getOffset(long time) {
        return inDaylightTime(new Date(time)) ? getRawOffset() + getDSTSavings() : getRawOffset();
    }

    public static synchronized TimeZone getTimeZone(String id) {
        TimeZone timeZone;
        if (id == null) {
            throw new NullPointerException("id == null");
        }
        if (id.length() == 3) {
            if (id.equals("GMT")) {
                timeZone = (TimeZone) GMT.clone();
            } else if (id.equals("UTC")) {
                timeZone = (TimeZone) UTC.clone();
            }
        } else {
            TimeZone zone = null;
            try {
                zone = ZoneInfoDB.getInstance().makeTimeZone(id);
            } catch (IOException e) {
            }
            if (zone == null && id.length() > 3 && id.startsWith("GMT")) {
                zone = getCustomTimeZone(id);
            }
            if (zone == null) {
                zone = (TimeZone) GMT.clone();
            }
            timeZone = zone;
        }
        return timeZone;
    }

    private static TimeZone getCustomTimeZone(String id) {
        Matcher m = CUSTOM_ZONE_ID_PATTERN.matcher(id);
        if (!m.matches()) {
            return null;
        }
        int minute = 0;
        try {
            int hour = Integer.parseInt(m.group(1));
            if (m.group(3) != null) {
                minute = Integer.parseInt(m.group(3));
            }
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                return null;
            }
            char sign = id.charAt(3);
            int raw = (Grego.MILLIS_PER_HOUR * hour) + (Grego.MILLIS_PER_MINUTE * minute);
            if (sign == '-') {
                raw = -raw;
            }
            String cleanId = String.format("GMT%c%02d:%02d", Character.valueOf(sign), Integer.valueOf(hour), Integer.valueOf(minute));
            return new SimpleTimeZone(raw, cleanId);
        } catch (NumberFormatException impossible) {
            throw new AssertionError(impossible);
        }
    }

    public boolean hasSameRules(TimeZone timeZone) {
        return timeZone != null && getRawOffset() == timeZone.getRawOffset();
    }

    public static synchronized void setDefault(TimeZone timeZone) {
        defaultTimeZone = timeZone != null ? (TimeZone) timeZone.clone() : null;
    }

    public void setID(String id) {
        if (id == null) {
            throw new NullPointerException("id == null");
        }
        this.ID = id;
    }
}
