package javax.xml.datatype;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.namespace.QName;

public abstract class Duration {
    public abstract Duration add(Duration duration);

    public abstract void addTo(Calendar calendar);

    public abstract int compare(Duration duration);

    public abstract Number getField(DatatypeConstants.Field field);

    public abstract int getSign();

    public abstract int hashCode();

    public abstract boolean isSet(DatatypeConstants.Field field);

    public abstract Duration multiply(BigDecimal bigDecimal);

    public abstract Duration negate();

    public abstract Duration normalizeWith(Calendar calendar);

    public QName getXMLSchemaType() {
        boolean zIsSet = isSet(DatatypeConstants.YEARS);
        boolean zIsSet2 = isSet(DatatypeConstants.MONTHS);
        boolean zIsSet3 = isSet(DatatypeConstants.DAYS);
        boolean zIsSet4 = isSet(DatatypeConstants.HOURS);
        boolean zIsSet5 = isSet(DatatypeConstants.MINUTES);
        boolean zIsSet6 = isSet(DatatypeConstants.SECONDS);
        if (zIsSet && zIsSet2 && zIsSet3 && zIsSet4 && zIsSet5 && zIsSet6) {
            return DatatypeConstants.DURATION;
        }
        if (!zIsSet && !zIsSet2 && zIsSet3 && zIsSet4 && zIsSet5 && zIsSet6) {
            return DatatypeConstants.DURATION_DAYTIME;
        }
        if (zIsSet && zIsSet2 && !zIsSet3 && !zIsSet4 && !zIsSet5 && !zIsSet6) {
            return DatatypeConstants.DURATION_YEARMONTH;
        }
        throw new IllegalStateException("javax.xml.datatype.Duration#getXMLSchemaType(): this Duration does not match one of the XML Schema date/time datatypes: year set = " + zIsSet + " month set = " + zIsSet2 + " day set = " + zIsSet3 + " hour set = " + zIsSet4 + " minute set = " + zIsSet5 + " second set = " + zIsSet6);
    }

    public int getYears() {
        return getFieldValueAsInt(DatatypeConstants.YEARS);
    }

    public int getMonths() {
        return getFieldValueAsInt(DatatypeConstants.MONTHS);
    }

    public int getDays() {
        return getFieldValueAsInt(DatatypeConstants.DAYS);
    }

    public int getHours() {
        return getFieldValueAsInt(DatatypeConstants.HOURS);
    }

    public int getMinutes() {
        return getFieldValueAsInt(DatatypeConstants.MINUTES);
    }

    public int getSeconds() {
        return getFieldValueAsInt(DatatypeConstants.SECONDS);
    }

    public long getTimeInMillis(Calendar calendar) {
        Calendar calendar2 = (Calendar) calendar.clone();
        addTo(calendar2);
        return getCalendarTimeInMillis(calendar2) - getCalendarTimeInMillis(calendar);
    }

    public long getTimeInMillis(Date date) {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(date);
        addTo(gregorianCalendar);
        return getCalendarTimeInMillis(gregorianCalendar) - date.getTime();
    }

    private int getFieldValueAsInt(DatatypeConstants.Field field) {
        Number field2 = getField(field);
        if (field2 != null) {
            return field2.intValue();
        }
        return 0;
    }

    public void addTo(Date date) {
        if (date == null) {
            throw new NullPointerException("date == null");
        }
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(date);
        addTo(gregorianCalendar);
        date.setTime(getCalendarTimeInMillis(gregorianCalendar));
    }

    public Duration subtract(Duration duration) {
        return add(duration.negate());
    }

    public Duration multiply(int i) {
        return multiply(BigDecimal.valueOf(i));
    }

    public boolean isLongerThan(Duration duration) {
        return compare(duration) == 1;
    }

    public boolean isShorterThan(Duration duration) {
        return compare(duration) == -1;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        return (obj instanceof Duration) && compare((Duration) obj) == 0;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (getSign() < 0) {
            sb.append('-');
        }
        sb.append('P');
        BigInteger bigInteger = (BigInteger) getField(DatatypeConstants.YEARS);
        if (bigInteger != null) {
            sb.append(bigInteger);
            sb.append('Y');
        }
        BigInteger bigInteger2 = (BigInteger) getField(DatatypeConstants.MONTHS);
        if (bigInteger2 != null) {
            sb.append(bigInteger2);
            sb.append('M');
        }
        BigInteger bigInteger3 = (BigInteger) getField(DatatypeConstants.DAYS);
        if (bigInteger3 != null) {
            sb.append(bigInteger3);
            sb.append('D');
        }
        BigInteger bigInteger4 = (BigInteger) getField(DatatypeConstants.HOURS);
        BigInteger bigInteger5 = (BigInteger) getField(DatatypeConstants.MINUTES);
        BigDecimal bigDecimal = (BigDecimal) getField(DatatypeConstants.SECONDS);
        if (bigInteger4 != null || bigInteger5 != null || bigDecimal != null) {
            sb.append('T');
            if (bigInteger4 != null) {
                sb.append(bigInteger4);
                sb.append('H');
            }
            if (bigInteger5 != null) {
                sb.append(bigInteger5);
                sb.append('M');
            }
            if (bigDecimal != null) {
                sb.append(toString(bigDecimal));
                sb.append('S');
            }
        }
        return sb.toString();
    }

    private String toString(BigDecimal bigDecimal) {
        StringBuilder sb;
        String string = bigDecimal.unscaledValue().toString();
        int iScale = bigDecimal.scale();
        if (iScale == 0) {
            return string;
        }
        int length = string.length() - iScale;
        if (length == 0) {
            return "0." + string;
        }
        if (length > 0) {
            sb = new StringBuilder(string);
            sb.insert(length, '.');
        } else {
            sb = new StringBuilder((3 - length) + string.length());
            sb.append("0.");
            for (int i = 0; i < (-length); i++) {
                sb.append('0');
            }
            sb.append(string);
        }
        return sb.toString();
    }

    private static long getCalendarTimeInMillis(Calendar calendar) {
        return calendar.getTime().getTime();
    }
}
