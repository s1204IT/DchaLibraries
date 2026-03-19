package sun.util.calendar;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class CalendarSystem {
    private static final Gregorian GREGORIAN_INSTANCE;
    private static final ConcurrentMap<String, CalendarSystem> calendars = new ConcurrentHashMap();
    private static final Map<String, Class<?>> names = new HashMap();

    public abstract CalendarDate getCalendarDate();

    public abstract CalendarDate getCalendarDate(long j);

    public abstract CalendarDate getCalendarDate(long j, TimeZone timeZone);

    public abstract CalendarDate getCalendarDate(long j, CalendarDate calendarDate);

    public abstract Era getEra(String str);

    public abstract Era[] getEras();

    public abstract int getMonthLength(CalendarDate calendarDate);

    public abstract String getName();

    public abstract CalendarDate getNthDayOfWeek(int i, int i2, CalendarDate calendarDate);

    public abstract long getTime(CalendarDate calendarDate);

    public abstract int getWeekLength();

    public abstract int getYearLength(CalendarDate calendarDate);

    public abstract int getYearLengthInMonths(CalendarDate calendarDate);

    public abstract CalendarDate newCalendarDate();

    public abstract CalendarDate newCalendarDate(TimeZone timeZone);

    public abstract boolean normalize(CalendarDate calendarDate);

    public abstract void setEra(CalendarDate calendarDate, String str);

    public abstract CalendarDate setTimeOfDay(CalendarDate calendarDate, int i);

    public abstract boolean validate(CalendarDate calendarDate);

    static {
        names.put("gregorian", Gregorian.class);
        names.put("japanese", LocalGregorianCalendar.class);
        names.put("julian", JulianCalendar.class);
        GREGORIAN_INSTANCE = new Gregorian();
    }

    public static Gregorian getGregorianCalendar() {
        return GREGORIAN_INSTANCE;
    }

    public static CalendarSystem forName(String str) {
        CalendarSystem localGregorianCalendar;
        if ("gregorian".equals(str)) {
            return GREGORIAN_INSTANCE;
        }
        CalendarSystem calendarSystem = calendars.get(str);
        if (calendarSystem != null) {
            return calendarSystem;
        }
        Class<?> cls = names.get(str);
        if (cls == null) {
            return null;
        }
        if (cls.isAssignableFrom(LocalGregorianCalendar.class)) {
            localGregorianCalendar = LocalGregorianCalendar.getLocalGregorianCalendar(str);
        } else {
            try {
                localGregorianCalendar = (CalendarSystem) cls.newInstance();
            } catch (Exception e) {
                throw new InternalError(e);
            }
        }
        if (localGregorianCalendar == null) {
            return null;
        }
        CalendarSystem calendarSystemPutIfAbsent = calendars.putIfAbsent(str, localGregorianCalendar);
        return calendarSystemPutIfAbsent == null ? localGregorianCalendar : calendarSystemPutIfAbsent;
    }

    public static Properties getCalendarProperties() throws Throwable {
        Properties properties = new Properties();
        InputStream systemResourceAsStream = ClassLoader.getSystemResourceAsStream("calendars.properties");
        try {
            properties.load(systemResourceAsStream);
            if (systemResourceAsStream != null) {
                systemResourceAsStream.close();
            }
            return properties;
        } catch (Throwable th) {
            th = th;
            try {
                throw th;
            } catch (Throwable th2) {
                th = th2;
                if (systemResourceAsStream != null) {
                    if (th != null) {
                        try {
                            systemResourceAsStream.close();
                        } catch (Throwable th3) {
                            th.addSuppressed(th3);
                        }
                    } else {
                        systemResourceAsStream.close();
                    }
                }
                throw th;
            }
        }
    }
}
