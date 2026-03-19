package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.time.Instant;
import sun.util.calendar.BaseCalendar;
import sun.util.calendar.CalendarSystem;
import sun.util.calendar.CalendarUtils;

public class Date implements Serializable, Cloneable, Comparable<Date> {
    private static int defaultCenturyStart = 0;
    private static BaseCalendar jcal = null;
    private static final long serialVersionUID = 7523967970034938905L;
    private transient BaseCalendar.Date cdate;
    private transient long fastTime;
    private static final BaseCalendar gcal = CalendarSystem.getGregorianCalendar();
    private static final String[] wtb = {"am", "pm", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday", "january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december", "gmt", "ut", "utc", "est", "edt", "cst", "cdt", "mst", "mdt", "pst", "pdt"};
    private static final int[] ttb = {14, 1, 0, 0, 0, 0, 0, 0, 0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 10000, 10000, 10000, 10300, 10240, 10360, 10300, 10420, 10360, 10480, 10420};

    public Date() {
        this(System.currentTimeMillis());
    }

    public Date(long j) {
        this.fastTime = j;
    }

    @Deprecated
    public Date(int i, int i2, int i3) {
        this(i, i2, i3, 0, 0, 0);
    }

    @Deprecated
    public Date(int i, int i2, int i3, int i4, int i5) {
        this(i, i2, i3, i4, i5, 0);
    }

    @Deprecated
    public Date(int i, int i2, int i3, int i4, int i5, int i6) {
        int iFloorDivide = i + 1900;
        if (i2 >= 12) {
            iFloorDivide += i2 / 12;
            i2 %= 12;
        } else if (i2 < 0) {
            iFloorDivide += CalendarUtils.floorDivide(i2, 12);
            i2 = CalendarUtils.mod(i2, 12);
        }
        this.cdate = (BaseCalendar.Date) getCalendarSystem(iFloorDivide).newCalendarDate(TimeZone.getDefaultRef());
        this.cdate.setNormalizedDate(iFloorDivide, i2 + 1, i3).setTimeOfDay(i4, i5, i6, 0);
        getTimeImpl();
        this.cdate = null;
    }

    @Deprecated
    public Date(String str) {
        this(parse(str));
    }

    public Object clone() {
        try {
            Date date = (Date) super.clone();
            try {
                if (this.cdate != null) {
                    date.cdate = (BaseCalendar.Date) this.cdate.clone();
                    return date;
                }
                return date;
            } catch (CloneNotSupportedException e) {
                return date;
            }
        } catch (CloneNotSupportedException e2) {
            return null;
        }
    }

    @Deprecated
    public static long UTC(int i, int i2, int i3, int i4, int i5, int i6) {
        int iFloorDivide = i + 1900;
        if (i2 >= 12) {
            iFloorDivide += i2 / 12;
            i2 %= 12;
        } else if (i2 < 0) {
            iFloorDivide += CalendarUtils.floorDivide(i2, 12);
            i2 = CalendarUtils.mod(i2, 12);
        }
        BaseCalendar.Date date = (BaseCalendar.Date) getCalendarSystem(iFloorDivide).newCalendarDate(null);
        date.setNormalizedDate(iFloorDivide, i2 + 1, i3).setTimeOfDay(i4, i5, i6, 0);
        Date date2 = new Date(0L);
        date2.normalize(date);
        return date2.fastTime;
    }

    @Deprecated
    public static long parse(String str) {
        char cCharAt;
        int i;
        int i2;
        byte b;
        byte b2;
        if (str != null) {
            int length = str.length();
            int i3 = Integer.MIN_VALUE;
            int i4 = Integer.MIN_VALUE;
            int i5 = 0;
            char c = 0;
            byte b3 = -1;
            byte b4 = -1;
            int i6 = -1;
            int i7 = -1;
            byte b5 = -1;
            byte b6 = -1;
            while (true) {
                if (i5 < length) {
                    char cCharAt2 = str.charAt(i5);
                    i5++;
                    if (cCharAt2 > ' ' && cCharAt2 != ',') {
                        if (cCharAt2 == '(') {
                            int i8 = 1;
                            while (i5 < length) {
                                char cCharAt3 = str.charAt(i5);
                                i5++;
                                if (cCharAt3 == '(') {
                                    i8++;
                                } else if (cCharAt3 != ')' || i8 - 1 > 0) {
                                }
                            }
                        } else if ('0' <= cCharAt2 && cCharAt2 <= '9') {
                            char c2 = cCharAt2;
                            int i9 = cCharAt2 - '0';
                            while (true) {
                                if (i5 >= length) {
                                    cCharAt = c2;
                                    break;
                                }
                                cCharAt = str.charAt(i5);
                                if ('0' > cCharAt || cCharAt > '9') {
                                    break;
                                }
                                i9 = ((i9 * 10) + cCharAt) - 48;
                                i5++;
                                c2 = cCharAt;
                            }
                            if (c != '+' && (c != '-' || i4 == i3)) {
                                if (i9 < 70) {
                                    if (cCharAt != ':') {
                                        if (cCharAt != '/') {
                                            if (i5 < length && cCharAt != ',' && cCharAt > ' ' && cCharAt != '-') {
                                                break;
                                            }
                                            if (i7 >= 0 && b5 < 0) {
                                                b2 = (byte) i9;
                                                b5 = b2;
                                            } else if (b5 >= 0 && b6 < 0) {
                                                b6 = (byte) i9;
                                            } else if (b4 < 0) {
                                                b = (byte) i9;
                                                b4 = b;
                                            } else {
                                                if (i4 != i3 || b3 < 0 || b4 < 0) {
                                                    break;
                                                }
                                                i4 = i9;
                                            }
                                        } else if (b3 >= 0) {
                                            if (b4 >= 0) {
                                                break;
                                            }
                                            b = (byte) i9;
                                            b4 = b;
                                        } else {
                                            b3 = (byte) (i9 - 1);
                                        }
                                    } else if (i7 >= 0) {
                                        if (b5 >= 0) {
                                            break;
                                        }
                                        b2 = (byte) i9;
                                        b5 = b2;
                                    } else {
                                        i7 = (byte) i9;
                                    }
                                } else {
                                    if (i4 != i3 || (cCharAt > ' ' && cCharAt != ',' && cCharAt != '/' && i5 < length)) {
                                        break;
                                    }
                                    i4 = i9;
                                }
                                c = 0;
                            } else {
                                if (i6 != 0 && i6 != -1) {
                                    break;
                                }
                                if (i9 < 24) {
                                    int i10 = i9 * 60;
                                    if (i5 >= length || str.charAt(i5) != ':') {
                                        i2 = 0;
                                    } else {
                                        i5++;
                                        i2 = 0;
                                        while (i5 < length) {
                                            char cCharAt4 = str.charAt(i5);
                                            if ('0' > cCharAt4 || cCharAt4 > '9') {
                                                break;
                                            }
                                            i2 = (i2 * 10) + (cCharAt4 - '0');
                                            i5++;
                                        }
                                    }
                                    i = i10 + i2;
                                } else {
                                    i = ((i9 / 100) * 60) + (i9 % 100);
                                }
                                if (c == '+') {
                                    i = -i;
                                }
                                i6 = i;
                                c = 0;
                            }
                        } else if (cCharAt2 != '/' && cCharAt2 != ':' && cCharAt2 != '+' && cCharAt2 != '-') {
                            int i11 = i5 - 1;
                            int i12 = i5;
                            while (i12 < length) {
                                char cCharAt5 = str.charAt(i12);
                                if (('A' > cCharAt5 || cCharAt5 > 'Z') && ('a' > cCharAt5 || cCharAt5 > 'z')) {
                                    break;
                                }
                                i12++;
                            }
                            if (i12 <= i11 + 1) {
                                break;
                            }
                            int length2 = wtb.length;
                            while (true) {
                                int i13 = length2 - 1;
                                if (i13 < 0) {
                                    break;
                                }
                                if (wtb[i13].regionMatches(true, 0, str, i11, i12 - i11)) {
                                    int i14 = ttb[i13];
                                    if (i14 == 0) {
                                        break;
                                    }
                                    if (i14 == 1) {
                                        if (i7 > 12 || i7 < 1) {
                                            break;
                                        }
                                        if (i7 < 12) {
                                            i7 += 12;
                                        }
                                    } else if (i14 == 14) {
                                        if (i7 > 12 || i7 < 1) {
                                            break;
                                        }
                                        if (i7 == 12) {
                                            i7 = 0;
                                        }
                                    } else if (i14 > 13) {
                                        i6 = i14 - 10000;
                                    } else {
                                        if (b3 >= 0) {
                                            break;
                                        }
                                        b3 = (byte) (i14 - 2);
                                    }
                                } else {
                                    length2 = i13;
                                }
                            }
                        } else {
                            c = cCharAt2;
                        }
                    }
                    i3 = Integer.MIN_VALUE;
                } else if (i4 != Integer.MIN_VALUE && b3 >= 0 && b4 >= 0) {
                    if (i4 < 100) {
                        synchronized (Date.class) {
                            if (defaultCenturyStart == 0) {
                                defaultCenturyStart = gcal.getCalendarDate().getYear() - 80;
                            }
                        }
                        i4 += (defaultCenturyStart / 100) * 100;
                        if (i4 < defaultCenturyStart) {
                            i4 += 100;
                        }
                    }
                    byte b7 = b6 < 0 ? (byte) 0 : b6;
                    byte b8 = b5 < 0 ? (byte) 0 : b5;
                    if (i7 < 0) {
                        i7 = 0;
                    }
                    BaseCalendar calendarSystem = getCalendarSystem(i4);
                    if (i6 == -1) {
                        BaseCalendar.Date date = (BaseCalendar.Date) calendarSystem.newCalendarDate(TimeZone.getDefaultRef());
                        date.setDate(i4, b3 + 1, b4);
                        date.setTimeOfDay(i7, b8, b7, 0);
                        return calendarSystem.getTime(date);
                    }
                    BaseCalendar.Date date2 = (BaseCalendar.Date) calendarSystem.newCalendarDate(null);
                    date2.setDate(i4, b3 + 1, b4);
                    date2.setTimeOfDay(i7, b8, b7, 0);
                    return calendarSystem.getTime(date2) + ((long) (i6 * 60000));
                }
            }
        }
        throw new IllegalArgumentException();
    }

    @Deprecated
    public int getYear() {
        return normalize().getYear() - 1900;
    }

    @Deprecated
    public void setYear(int i) {
        getCalendarDate().setNormalizedYear(i + 1900);
    }

    @Deprecated
    public int getMonth() {
        return normalize().getMonth() - 1;
    }

    @Deprecated
    public void setMonth(int i) {
        int i2;
        if (i >= 12) {
            i2 = i / 12;
            i %= 12;
        } else if (i < 0) {
            int iFloorDivide = CalendarUtils.floorDivide(i, 12);
            i = CalendarUtils.mod(i, 12);
            i2 = iFloorDivide;
        } else {
            i2 = 0;
        }
        BaseCalendar.Date calendarDate = getCalendarDate();
        if (i2 != 0) {
            calendarDate.setNormalizedYear(calendarDate.getNormalizedYear() + i2);
        }
        calendarDate.setMonth(i + 1);
    }

    @Deprecated
    public int getDate() {
        return normalize().getDayOfMonth();
    }

    @Deprecated
    public void setDate(int i) {
        getCalendarDate().setDayOfMonth(i);
    }

    @Deprecated
    public int getDay() {
        return normalize().getDayOfWeek() - 1;
    }

    @Deprecated
    public int getHours() {
        return normalize().getHours();
    }

    @Deprecated
    public void setHours(int i) {
        getCalendarDate().setHours(i);
    }

    @Deprecated
    public int getMinutes() {
        return normalize().getMinutes();
    }

    @Deprecated
    public void setMinutes(int i) {
        getCalendarDate().setMinutes(i);
    }

    @Deprecated
    public int getSeconds() {
        return normalize().getSeconds();
    }

    @Deprecated
    public void setSeconds(int i) {
        getCalendarDate().setSeconds(i);
    }

    public long getTime() {
        return getTimeImpl();
    }

    private final long getTimeImpl() {
        if (this.cdate != null && !this.cdate.isNormalized()) {
            normalize();
        }
        return this.fastTime;
    }

    public void setTime(long j) {
        this.fastTime = j;
        this.cdate = null;
    }

    public boolean before(Date date) {
        return getMillisOf(this) < getMillisOf(date);
    }

    public boolean after(Date date) {
        return getMillisOf(this) > getMillisOf(date);
    }

    public boolean equals(Object obj) {
        return (obj instanceof Date) && getTime() == ((Date) obj).getTime();
    }

    static final long getMillisOf(Date date) {
        if (date.cdate == null || date.cdate.isNormalized()) {
            return date.fastTime;
        }
        return gcal.getTime((BaseCalendar.Date) date.cdate.clone());
    }

    @Override
    public int compareTo(Date date) {
        long millisOf = getMillisOf(this);
        long millisOf2 = getMillisOf(date);
        if (millisOf < millisOf2) {
            return -1;
        }
        return millisOf == millisOf2 ? 0 : 1;
    }

    public int hashCode() {
        long time = getTime();
        return ((int) (time >> 32)) ^ ((int) time);
    }

    public String toString() {
        BaseCalendar.Date dateNormalize = normalize();
        StringBuilder sb = new StringBuilder(28);
        int dayOfWeek = dateNormalize.getDayOfWeek();
        if (dayOfWeek == 1) {
            dayOfWeek = 8;
        }
        convertToAbbr(sb, wtb[dayOfWeek]).append(' ');
        convertToAbbr(sb, wtb[(dateNormalize.getMonth() - 1) + 2 + 7]).append(' ');
        CalendarUtils.sprintf0d(sb, dateNormalize.getDayOfMonth(), 2).append(' ');
        CalendarUtils.sprintf0d(sb, dateNormalize.getHours(), 2).append(':');
        CalendarUtils.sprintf0d(sb, dateNormalize.getMinutes(), 2).append(':');
        CalendarUtils.sprintf0d(sb, dateNormalize.getSeconds(), 2).append(' ');
        TimeZone zone = dateNormalize.getZone();
        if (zone != null) {
            sb.append(zone.getDisplayName(dateNormalize.isDaylightTime(), 0, Locale.US));
        } else {
            sb.append("GMT");
        }
        sb.append(' ');
        sb.append(dateNormalize.getYear());
        return sb.toString();
    }

    private static final StringBuilder convertToAbbr(StringBuilder sb, String str) {
        sb.append(Character.toUpperCase(str.charAt(0)));
        sb.append(str.charAt(1));
        sb.append(str.charAt(2));
        return sb;
    }

    @Deprecated
    public String toLocaleString() {
        return DateFormat.getDateTimeInstance().format(this);
    }

    @Deprecated
    public String toGMTString() {
        BaseCalendar.Date date = (BaseCalendar.Date) getCalendarSystem(getTime()).getCalendarDate(getTime(), (TimeZone) null);
        StringBuilder sb = new StringBuilder(32);
        CalendarUtils.sprintf0d(sb, date.getDayOfMonth(), 1).append(' ');
        convertToAbbr(sb, wtb[(date.getMonth() - 1) + 2 + 7]).append(' ');
        sb.append(date.getYear());
        sb.append(' ');
        CalendarUtils.sprintf0d(sb, date.getHours(), 2).append(':');
        CalendarUtils.sprintf0d(sb, date.getMinutes(), 2).append(':');
        CalendarUtils.sprintf0d(sb, date.getSeconds(), 2);
        sb.append(" GMT");
        return sb.toString();
    }

    @Deprecated
    public int getTimezoneOffset() {
        int zoneOffset;
        if (this.cdate == null) {
            GregorianCalendar gregorianCalendar = new GregorianCalendar(this.fastTime);
            zoneOffset = gregorianCalendar.get(15) + gregorianCalendar.get(16);
        } else {
            normalize();
            zoneOffset = this.cdate.getZoneOffset();
        }
        return (-zoneOffset) / 60000;
    }

    private final BaseCalendar.Date getCalendarDate() {
        if (this.cdate == null) {
            this.cdate = (BaseCalendar.Date) getCalendarSystem(this.fastTime).getCalendarDate(this.fastTime, TimeZone.getDefaultRef());
        }
        return this.cdate;
    }

    private final BaseCalendar.Date normalize() {
        if (this.cdate == null) {
            this.cdate = (BaseCalendar.Date) getCalendarSystem(this.fastTime).getCalendarDate(this.fastTime, TimeZone.getDefaultRef());
            return this.cdate;
        }
        if (!this.cdate.isNormalized()) {
            this.cdate = normalize(this.cdate);
        }
        TimeZone defaultRef = TimeZone.getDefaultRef();
        if (defaultRef != this.cdate.getZone()) {
            this.cdate.setZone(defaultRef);
            getCalendarSystem(this.cdate).getCalendarDate(this.fastTime, this.cdate);
        }
        return this.cdate;
    }

    private final BaseCalendar.Date normalize(BaseCalendar.Date date) {
        int normalizedYear = date.getNormalizedYear();
        int month = date.getMonth();
        int dayOfMonth = date.getDayOfMonth();
        int hours = date.getHours();
        int minutes = date.getMinutes();
        int seconds = date.getSeconds();
        int millis = date.getMillis();
        TimeZone zone = date.getZone();
        if (normalizedYear == 1582 || normalizedYear > 280000000 || normalizedYear < -280000000) {
            if (zone == null) {
                zone = TimeZone.getTimeZone("GMT");
            }
            GregorianCalendar gregorianCalendar = new GregorianCalendar(zone);
            gregorianCalendar.clear();
            gregorianCalendar.set(14, millis);
            gregorianCalendar.set(normalizedYear, month - 1, dayOfMonth, hours, minutes, seconds);
            this.fastTime = gregorianCalendar.getTimeInMillis();
            return (BaseCalendar.Date) getCalendarSystem(this.fastTime).getCalendarDate(this.fastTime, zone);
        }
        BaseCalendar calendarSystem = getCalendarSystem(normalizedYear);
        if (calendarSystem != getCalendarSystem(date)) {
            date = (BaseCalendar.Date) calendarSystem.newCalendarDate(zone);
            date.setNormalizedDate(normalizedYear, month, dayOfMonth).setTimeOfDay(hours, minutes, seconds, millis);
        }
        this.fastTime = calendarSystem.getTime(date);
        BaseCalendar calendarSystem2 = getCalendarSystem(this.fastTime);
        if (calendarSystem2 != calendarSystem) {
            BaseCalendar.Date date2 = (BaseCalendar.Date) calendarSystem2.newCalendarDate(zone);
            date2.setNormalizedDate(normalizedYear, month, dayOfMonth).setTimeOfDay(hours, minutes, seconds, millis);
            this.fastTime = calendarSystem2.getTime(date2);
            return date2;
        }
        return date;
    }

    private static final BaseCalendar getCalendarSystem(int i) {
        if (i >= 1582) {
            return gcal;
        }
        return getJulianCalendar();
    }

    private static final BaseCalendar getCalendarSystem(long j) {
        if (j >= 0 || j >= (-12219292800000L) - ((long) TimeZone.getDefaultRef().getOffset(j))) {
            return gcal;
        }
        return getJulianCalendar();
    }

    private static final BaseCalendar getCalendarSystem(BaseCalendar.Date date) {
        if (jcal == null) {
            return gcal;
        }
        if (date.getEra() != null) {
            return jcal;
        }
        return gcal;
    }

    private static final synchronized BaseCalendar getJulianCalendar() {
        if (jcal == null) {
            jcal = (BaseCalendar) CalendarSystem.forName("julian");
        }
        return jcal;
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.writeLong(getTimeImpl());
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        this.fastTime = objectInputStream.readLong();
    }

    public static Date from(Instant instant) {
        try {
            return new Date(instant.toEpochMilli());
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Instant toInstant() {
        return Instant.ofEpochMilli(getTime());
    }
}
