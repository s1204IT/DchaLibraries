package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Types;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import libcore.icu.LocaleData;

public class Date implements Serializable, Cloneable, Comparable<Date> {
    private static final int CREATION_YEAR = new Date().getYear();
    private static final long serialVersionUID = 7523967970034938905L;
    private transient long milliseconds;

    public Date() {
        this(System.currentTimeMillis());
    }

    @Deprecated
    public Date(int year, int month, int day) {
        GregorianCalendar cal = new GregorianCalendar(false);
        cal.set(year + 1900, month, day);
        this.milliseconds = cal.getTimeInMillis();
    }

    @Deprecated
    public Date(int year, int month, int day, int hour, int minute) {
        GregorianCalendar cal = new GregorianCalendar(false);
        cal.set(year + 1900, month, day, hour, minute);
        this.milliseconds = cal.getTimeInMillis();
    }

    @Deprecated
    public Date(int year, int month, int day, int hour, int minute, int second) {
        GregorianCalendar cal = new GregorianCalendar(false);
        cal.set(year + 1900, month, day, hour, minute, second);
        this.milliseconds = cal.getTimeInMillis();
    }

    public Date(long milliseconds) {
        this.milliseconds = milliseconds;
    }

    @Deprecated
    public Date(String string) {
        this.milliseconds = parse(string);
    }

    public boolean after(Date date) {
        return this.milliseconds > date.milliseconds;
    }

    public boolean before(Date date) {
        return this.milliseconds < date.milliseconds;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public int compareTo(Date date) {
        if (this.milliseconds < date.milliseconds) {
            return -1;
        }
        if (this.milliseconds == date.milliseconds) {
            return 0;
        }
        return 1;
    }

    public boolean equals(Object object) {
        return object == this || ((object instanceof Date) && this.milliseconds == ((Date) object).milliseconds);
    }

    @Deprecated
    public int getDate() {
        return new GregorianCalendar(this.milliseconds).get(5);
    }

    @Deprecated
    public int getDay() {
        return new GregorianCalendar(this.milliseconds).get(7) - 1;
    }

    @Deprecated
    public int getHours() {
        return new GregorianCalendar(this.milliseconds).get(11);
    }

    @Deprecated
    public int getMinutes() {
        return new GregorianCalendar(this.milliseconds).get(12);
    }

    @Deprecated
    public int getMonth() {
        return new GregorianCalendar(this.milliseconds).get(2);
    }

    @Deprecated
    public int getSeconds() {
        return new GregorianCalendar(this.milliseconds).get(13);
    }

    public long getTime() {
        return this.milliseconds;
    }

    @Deprecated
    public int getTimezoneOffset() {
        GregorianCalendar cal = new GregorianCalendar(this.milliseconds);
        return (-(cal.get(15) + cal.get(16))) / Grego.MILLIS_PER_MINUTE;
    }

    @Deprecated
    public int getYear() {
        return new GregorianCalendar(this.milliseconds).get(1) - 1900;
    }

    public int hashCode() {
        return ((int) (this.milliseconds >>> 32)) ^ ((int) this.milliseconds);
    }

    private static int parse(String string, String[] array) {
        int alength = array.length;
        int slength = string.length();
        for (int i = 0; i < alength; i++) {
            if (string.regionMatches(true, 0, array[i], 0, slength)) {
                return i;
            }
        }
        return -1;
    }

    private static IllegalArgumentException parseError(String string) {
        throw new IllegalArgumentException("Parse error: " + string);
    }

    @Deprecated
    public static long parse(String string) {
        int hour;
        if (string == null) {
            throw new IllegalArgumentException("The string argument is null");
        }
        char sign = 0;
        int commentLevel = 0;
        int offset = 0;
        int length = string.length();
        int state = 0;
        int year = -1;
        int month = -1;
        int date = -1;
        int hour2 = -1;
        int minute = -1;
        int second = -1;
        int zoneOffset = 0;
        int minutesOffset = 0;
        boolean zone = false;
        StringBuilder buffer = new StringBuilder();
        while (offset <= length) {
            char next = offset < length ? string.charAt(offset) : '\r';
            offset++;
            if (next == '(') {
                commentLevel++;
            }
            if (commentLevel > 0) {
                if (next == ')') {
                    commentLevel--;
                }
                if (commentLevel == 0) {
                    next = ' ';
                } else {
                    continue;
                }
            }
            int nextState = 0;
            if (('a' <= next && next <= 'z') || ('A' <= next && next <= 'Z')) {
                nextState = 1;
            } else if ('0' <= next && next <= '9') {
                nextState = 2;
            } else if (!Character.isSpace(next) && ",+-:/".indexOf(next) == -1) {
                throw parseError(string);
            }
            if (state == 2 && nextState != 2) {
                int digit = Integer.parseInt(buffer.toString());
                buffer.setLength(0);
                if (sign == '+' || sign == '-') {
                    if (zoneOffset == 0) {
                        zone = true;
                        if (next == ':') {
                            minutesOffset = sign == '-' ? -Integer.parseInt(string.substring(offset, offset + 2)) : Integer.parseInt(string.substring(offset, offset + 2));
                            offset += 2;
                        }
                        zoneOffset = sign == '-' ? -digit : digit;
                        sign = 0;
                    } else {
                        throw parseError(string);
                    }
                } else if (digit >= 70) {
                    if (year == -1 && (Character.isSpace(next) || next == ',' || next == '/' || next == '\r')) {
                        year = digit;
                    } else {
                        throw parseError(string);
                    }
                } else if (next == ':') {
                    if (hour2 == -1) {
                        hour2 = digit;
                    } else if (minute == -1) {
                        minute = digit;
                    } else {
                        throw parseError(string);
                    }
                } else if (next == '/') {
                    if (month == -1) {
                        month = digit - 1;
                    } else if (date == -1) {
                        date = digit;
                    } else {
                        throw parseError(string);
                    }
                } else if (Character.isSpace(next) || next == ',' || next == '-' || next == '\r') {
                    if (hour2 != -1 && minute == -1) {
                        minute = digit;
                    } else if (minute != -1 && second == -1) {
                        second = digit;
                    } else if (date == -1) {
                        date = digit;
                    } else if (year == -1) {
                        year = digit;
                    } else {
                        throw parseError(string);
                    }
                } else if (year == -1 && month != -1 && date != -1) {
                    year = digit;
                } else {
                    throw parseError(string);
                }
            } else if (state == 1 && nextState != 1) {
                String text = buffer.toString().toUpperCase(Locale.US);
                buffer.setLength(0);
                if (text.length() == 1) {
                    throw parseError(string);
                }
                if (text.equals("AM")) {
                    if (hour2 == 12) {
                        hour2 = 0;
                    } else if (hour2 < 1 || hour2 > 12) {
                        throw parseError(string);
                    }
                } else if (text.equals("PM")) {
                    if (hour2 == 12) {
                        hour2 = 0;
                    } else if (hour2 < 1 || hour2 > 12) {
                        throw parseError(string);
                    }
                    hour2 += 12;
                } else {
                    DateFormatSymbols symbols = new DateFormatSymbols(Locale.US);
                    String[] weekdays = symbols.getWeekdays();
                    String[] months = symbols.getMonths();
                    if (parse(text, weekdays) == -1 && (month != -1 || (month = parse(text, months)) == -1)) {
                        if (text.equals("GMT") || text.equals("UT") || text.equals("UTC")) {
                            zone = true;
                            zoneOffset = 0;
                        } else {
                            int value = zone(text);
                            if (value != 0) {
                                zone = true;
                                zoneOffset = value;
                            } else {
                                throw parseError(string);
                            }
                        }
                    }
                }
            }
            if (next == '+' || (year != -1 && next == '-')) {
                sign = next;
            } else if (!Character.isSpace(next) && next != ',' && nextState != 2) {
                sign = 0;
            }
            if (nextState == 1 || nextState == 2) {
                buffer.append(next);
            }
            state = nextState;
        }
        if (year != -1 && month != -1 && date != -1) {
            if (hour2 == -1) {
                hour2 = 0;
            }
            if (minute == -1) {
                minute = 0;
            }
            if (second == -1) {
                second = 0;
            }
            if (year < CREATION_YEAR - 80) {
                year += Types.JAVA_OBJECT;
            } else if (year < 100) {
                year += 1900;
            }
            int minute2 = minute - minutesOffset;
            if (zone) {
                if (zoneOffset >= 24 || zoneOffset <= -24) {
                    hour = hour2 - (zoneOffset / 100);
                    minute2 -= zoneOffset % 100;
                } else {
                    hour = hour2 - zoneOffset;
                }
                return UTC(year - 1900, month, date, hour, minute2, second);
            }
            return new Date(year - 1900, month, date, hour2, minute2, second).getTime();
        }
        throw parseError(string);
    }

    @Deprecated
    public void setDate(int day) {
        GregorianCalendar cal = new GregorianCalendar(this.milliseconds);
        cal.set(5, day);
        this.milliseconds = cal.getTimeInMillis();
    }

    @Deprecated
    public void setHours(int hour) {
        GregorianCalendar cal = new GregorianCalendar(this.milliseconds);
        cal.set(11, hour);
        this.milliseconds = cal.getTimeInMillis();
    }

    @Deprecated
    public void setMinutes(int minute) {
        GregorianCalendar cal = new GregorianCalendar(this.milliseconds);
        cal.set(12, minute);
        this.milliseconds = cal.getTimeInMillis();
    }

    @Deprecated
    public void setMonth(int month) {
        GregorianCalendar cal = new GregorianCalendar(this.milliseconds);
        cal.set(2, month);
        this.milliseconds = cal.getTimeInMillis();
    }

    @Deprecated
    public void setSeconds(int second) {
        GregorianCalendar cal = new GregorianCalendar(this.milliseconds);
        cal.set(13, second);
        this.milliseconds = cal.getTimeInMillis();
    }

    public void setTime(long milliseconds) {
        this.milliseconds = milliseconds;
    }

    @Deprecated
    public void setYear(int year) {
        GregorianCalendar cal = new GregorianCalendar(this.milliseconds);
        cal.set(1, year + 1900);
        this.milliseconds = cal.getTimeInMillis();
    }

    @Deprecated
    public String toGMTString() {
        SimpleDateFormat sdf = new SimpleDateFormat("d MMM y HH:mm:ss 'GMT'", Locale.US);
        TimeZone gmtZone = TimeZone.getTimeZone("GMT");
        sdf.setTimeZone(gmtZone);
        GregorianCalendar gc = new GregorianCalendar(gmtZone);
        gc.setTimeInMillis(this.milliseconds);
        return sdf.format(this);
    }

    @Deprecated
    public String toLocaleString() {
        return DateFormat.getDateTimeInstance().format(this);
    }

    public String toString() {
        LocaleData localeData = LocaleData.get(Locale.US);
        Calendar cal = new GregorianCalendar(this.milliseconds);
        TimeZone tz = cal.getTimeZone();
        StringBuilder result = new StringBuilder();
        result.append(localeData.shortWeekdayNames[cal.get(7)]);
        result.append(' ');
        result.append(localeData.shortMonthNames[cal.get(2)]);
        result.append(' ');
        appendTwoDigits(result, cal.get(5));
        result.append(' ');
        appendTwoDigits(result, cal.get(11));
        result.append(':');
        appendTwoDigits(result, cal.get(12));
        result.append(':');
        appendTwoDigits(result, cal.get(13));
        result.append(' ');
        result.append(tz.getDisplayName(tz.inDaylightTime(this), 0));
        result.append(' ');
        result.append(cal.get(1));
        return result.toString();
    }

    private static void appendTwoDigits(StringBuilder sb, int n) {
        if (n < 10) {
            sb.append('0');
        }
        sb.append(n);
    }

    @Deprecated
    public static long UTC(int year, int month, int day, int hour, int minute, int second) {
        GregorianCalendar cal = new GregorianCalendar(false);
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        cal.set(year + 1900, month, day, hour, minute, second);
        return cal.getTimeInMillis();
    }

    private static int zone(String text) {
        if (text.equals("EST")) {
            return -5;
        }
        if (text.equals("EDT")) {
            return -4;
        }
        if (text.equals("CST")) {
            return -6;
        }
        if (text.equals("CDT")) {
            return -5;
        }
        if (text.equals("MST")) {
            return -7;
        }
        if (text.equals("MDT")) {
            return -6;
        }
        if (text.equals("PST")) {
            return -8;
        }
        return text.equals("PDT") ? -7 : 0;
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeLong(getTime());
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        setTime(stream.readLong());
    }
}
