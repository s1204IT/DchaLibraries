package java.sql;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.regex.Pattern;

public class Timestamp extends java.util.Date {
    private static final String PADDING = "000000000";
    private static final String TIME_FORMAT_REGEX = "[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}.*";
    private static final long serialVersionUID = 2745179027874758501L;
    private int nanos;

    @Deprecated
    public Timestamp(int theYear, int theMonth, int theDate, int theHour, int theMinute, int theSecond, int theNano) throws IllegalArgumentException {
        super(theYear, theMonth, theDate, theHour, theMinute, theSecond);
        if (theNano < 0 || theNano > 999999999) {
            throw new IllegalArgumentException("ns out of range: " + theNano);
        }
        this.nanos = theNano;
    }

    public Timestamp(long theTime) {
        super(theTime);
        setTimeImpl(theTime);
    }

    public boolean after(Timestamp theTimestamp) {
        long thisTime = getTime();
        long compareTime = theTimestamp.getTime();
        if (thisTime > compareTime) {
            return true;
        }
        return thisTime >= compareTime && getNanos() > theTimestamp.getNanos();
    }

    public boolean before(Timestamp theTimestamp) {
        long thisTime = getTime();
        long compareTime = theTimestamp.getTime();
        if (thisTime < compareTime) {
            return true;
        }
        return thisTime <= compareTime && getNanos() < theTimestamp.getNanos();
    }

    @Override
    public int compareTo(java.util.Date theObject) throws ClassCastException {
        return compareTo((Timestamp) theObject);
    }

    public int compareTo(Timestamp theTimestamp) {
        int result = super.compareTo((java.util.Date) theTimestamp);
        if (result == 0) {
            int thisNano = getNanos();
            int thatNano = theTimestamp.getNanos();
            if (thisNano > thatNano) {
                return 1;
            }
            if (thisNano == thatNano) {
                return 0;
            }
            return -1;
        }
        return result;
    }

    @Override
    public boolean equals(Object theObject) {
        if (theObject instanceof Timestamp) {
            return equals((Timestamp) theObject);
        }
        return false;
    }

    public boolean equals(Timestamp theTimestamp) {
        return theTimestamp != null && getTime() == theTimestamp.getTime() && getNanos() == theTimestamp.getNanos();
    }

    public int getNanos() {
        return this.nanos;
    }

    @Override
    public long getTime() {
        long theTime = super.getTime();
        return theTime + ((long) (this.nanos / 1000000));
    }

    public void setNanos(int n) throws IllegalArgumentException {
        if (n < 0 || n > 999999999) {
            throw new IllegalArgumentException("Value out of range");
        }
        this.nanos = n;
    }

    @Override
    public void setTime(long theTime) {
        setTimeImpl(theTime);
    }

    private void setTimeImpl(long theTime) {
        int milliseconds = (int) (theTime % 1000);
        long theTime2 = theTime - ((long) milliseconds);
        if (milliseconds < 0) {
            theTime2 -= 1000;
            milliseconds += 1000;
        }
        super.setTime(theTime2);
        setNanos(1000000 * milliseconds);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(29);
        format(getYear() + 1900, 4, sb);
        sb.append('-');
        format(getMonth() + 1, 2, sb);
        sb.append('-');
        format(getDate(), 2, sb);
        sb.append(' ');
        format(getHours(), 2, sb);
        sb.append(':');
        format(getMinutes(), 2, sb);
        sb.append(':');
        format(getSeconds(), 2, sb);
        sb.append('.');
        if (this.nanos == 0) {
            sb.append('0');
        } else {
            format(this.nanos, 9, sb);
            while (sb.charAt(sb.length() - 1) == '0') {
                sb.setLength(sb.length() - 1);
            }
        }
        return sb.toString();
    }

    private void format(int date, int digits, StringBuilder sb) {
        String str = String.valueOf(date);
        if (digits - str.length() > 0) {
            sb.append(PADDING.substring(0, digits - str.length()));
        }
        sb.append(str);
    }

    public static Timestamp valueOf(String s) throws IllegalArgumentException {
        int nanos;
        if (s == null) {
            throw new IllegalArgumentException("Argument cannot be null");
        }
        String s2 = s.trim();
        if (!Pattern.matches(TIME_FORMAT_REGEX, s2)) {
            throw badTimestampString(s2);
        }
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        ParsePosition pp = new ParsePosition(0);
        try {
            java.util.Date date = df.parse(s2, pp);
            if (date == null) {
                throw badTimestampString(s2);
            }
            int position = pp.getIndex();
            int remaining = s2.length() - position;
            if (remaining == 0) {
                nanos = 0;
            } else {
                if (remaining < 2 || remaining > 10 || s2.charAt(position) != '.') {
                    throw badTimestampString(s2);
                }
                try {
                    nanos = Integer.parsePositiveInt(s2.substring(position + 1));
                    if (nanos != 0) {
                        for (int i = remaining - 1; i < 9; i++) {
                            nanos *= 10;
                        }
                    }
                } catch (NumberFormatException e) {
                    throw badTimestampString(s2);
                }
            }
            Timestamp timestamp = new Timestamp(date.getTime());
            timestamp.setNanos(nanos);
            return timestamp;
        } catch (Exception e2) {
            throw badTimestampString(s2);
        }
    }

    private static IllegalArgumentException badTimestampString(String s) {
        return new IllegalArgumentException("Timestamp format must be yyyy-MM-dd HH:mm:ss.fffffffff; was '" + s + "'");
    }
}
