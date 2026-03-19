package mf.org.apache.xerces.impl.dv.xs;

import java.math.BigDecimal;
import java.math.BigInteger;
import mf.javax.xml.datatype.Duration;
import mf.org.apache.xerces.impl.dv.InvalidDatatypeValueException;
import mf.org.apache.xerces.impl.dv.ValidationContext;
import mf.org.apache.xerces.impl.dv.xs.AbstractDateTimeDV;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;

public class DurationDV extends AbstractDateTimeDV {
    private static final AbstractDateTimeDV.DateTimeData[] DATETIMES = {new AbstractDateTimeDV.DateTimeData(1696, 9, 1, 0, 0, 0.0d, 90, null, true, null), new AbstractDateTimeDV.DateTimeData(1697, 2, 1, 0, 0, 0.0d, 90, null, true, null), new AbstractDateTimeDV.DateTimeData(1903, 3, 1, 0, 0, 0.0d, 90, null, true, null), new AbstractDateTimeDV.DateTimeData(1903, 7, 1, 0, 0, 0.0d, 90, null, true, null)};
    public static final int DAYTIMEDURATION_TYPE = 2;
    public static final int DURATION_TYPE = 0;
    public static final int YEARMONTHDURATION_TYPE = 1;

    @Override
    public Object getActualValue(String content, ValidationContext context) throws InvalidDatatypeValueException {
        try {
            return parse(content, 0);
        } catch (Exception e) {
            throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.1", new Object[]{content, SchemaSymbols.ATTVAL_DURATION});
        }
    }

    protected AbstractDateTimeDV.DateTimeData parse(String str, int durationType) throws SchemaDateTimeException {
        int len = str.length();
        AbstractDateTimeDV.DateTimeData date = new AbstractDateTimeDV.DateTimeData(str, this);
        int start = 0 + 1;
        int start2 = str.charAt(0);
        if (start2 != 80 && start2 != 45) {
            throw new SchemaDateTimeException();
        }
        date.utc = start2 == 45 ? 45 : 0;
        if (start2 == 45) {
            int start3 = start + 1;
            if (str.charAt(start) != 80) {
                throw new SchemaDateTimeException();
            }
            start = start3;
        }
        int negate = 1;
        if (date.utc == 45) {
            negate = -1;
        }
        boolean designator = false;
        int endDate = indexOf(str, start, len, 'T');
        if (endDate == -1) {
            endDate = len;
        } else if (durationType == 1) {
            throw new SchemaDateTimeException();
        }
        int end = indexOf(str, start, endDate, 'Y');
        if (end != -1) {
            if (durationType == 2) {
                throw new SchemaDateTimeException();
            }
            date.year = parseInt(str, start, end) * negate;
            start = end + 1;
            designator = true;
        }
        int end2 = indexOf(str, start, endDate, 'M');
        if (end2 != -1) {
            if (durationType == 2) {
                throw new SchemaDateTimeException();
            }
            date.month = parseInt(str, start, end2) * negate;
            start = end2 + 1;
            designator = true;
        }
        int end3 = indexOf(str, start, endDate, 'D');
        if (end3 != -1) {
            if (durationType == 1) {
                throw new SchemaDateTimeException();
            }
            date.day = parseInt(str, start, end3) * negate;
            start = end3 + 1;
            designator = true;
        }
        if (len == endDate && start != len) {
            throw new SchemaDateTimeException();
        }
        if (len != endDate) {
            int start4 = start + 1;
            int end4 = indexOf(str, start4, len, 'H');
            if (end4 != -1) {
                date.hour = parseInt(str, start4, end4) * negate;
                start4 = end4 + 1;
                designator = true;
            }
            int end5 = indexOf(str, start4, len, 'M');
            if (end5 != -1) {
                date.minute = parseInt(str, start4, end5) * negate;
                start4 = end5 + 1;
                designator = true;
            }
            int end6 = indexOf(str, start4, len, 'S');
            if (end6 != -1) {
                date.second = ((double) negate) * parseSecond(str, start4, end6);
                start4 = end6 + 1;
                designator = true;
            }
            if (start4 != len || str.charAt(start4 - 1) == 'T') {
                throw new SchemaDateTimeException();
            }
        }
        if (!designator) {
            throw new SchemaDateTimeException();
        }
        return date;
    }

    @Override
    protected short compareDates(AbstractDateTimeDV.DateTimeData date1, AbstractDateTimeDV.DateTimeData date2, boolean strict) {
        if (compareOrder(date1, date2) == 0) {
            return (short) 0;
        }
        AbstractDateTimeDV.DateTimeData[] result = {new AbstractDateTimeDV.DateTimeData(null, this), new AbstractDateTimeDV.DateTimeData(null, this)};
        AbstractDateTimeDV.DateTimeData tempA = addDuration(date1, DATETIMES[0], result[0]);
        AbstractDateTimeDV.DateTimeData tempB = addDuration(date2, DATETIMES[0], result[1]);
        short resultA = compareOrder(tempA, tempB);
        if (resultA != 2) {
            AbstractDateTimeDV.DateTimeData tempA2 = addDuration(date1, DATETIMES[1], result[0]);
            AbstractDateTimeDV.DateTimeData tempB2 = addDuration(date2, DATETIMES[1], result[1]);
            short resultB = compareOrder(tempA2, tempB2);
            short resultA2 = compareResults(resultA, resultB, strict);
            if (resultA2 != 2) {
                AbstractDateTimeDV.DateTimeData tempA3 = addDuration(date1, DATETIMES[2], result[0]);
                AbstractDateTimeDV.DateTimeData tempB3 = addDuration(date2, DATETIMES[2], result[1]);
                short resultB2 = compareOrder(tempA3, tempB3);
                short resultA3 = compareResults(resultA2, resultB2, strict);
                if (resultA3 != 2) {
                    AbstractDateTimeDV.DateTimeData tempA4 = addDuration(date1, DATETIMES[3], result[0]);
                    AbstractDateTimeDV.DateTimeData tempB4 = addDuration(date2, DATETIMES[3], result[1]);
                    short resultB3 = compareOrder(tempA4, tempB4);
                    return compareResults(resultA3, resultB3, strict);
                }
                return (short) 2;
            }
            return (short) 2;
        }
        return (short) 2;
    }

    private short compareResults(short resultA, short resultB, boolean strict) {
        if (resultB == 2) {
            return (short) 2;
        }
        if (resultA != resultB && strict) {
            return (short) 2;
        }
        if (resultA != resultB && !strict) {
            if (resultA == 0 || resultB == 0) {
                return resultA != 0 ? resultA : resultB;
            }
            return (short) 2;
        }
        return resultA;
    }

    private AbstractDateTimeDV.DateTimeData addDuration(AbstractDateTimeDV.DateTimeData date, AbstractDateTimeDV.DateTimeData addto, AbstractDateTimeDV.DateTimeData duration) {
        int carry;
        resetDateObj(duration);
        int temp = addto.month + date.month;
        duration.month = modulo(temp, 1, 13);
        duration.year = addto.year + date.year + fQuotient(temp, 1, 13);
        double dtemp = addto.second + date.second;
        int carry2 = (int) Math.floor(dtemp / 60.0d);
        duration.second = dtemp - ((double) (carry2 * 60));
        int temp2 = addto.minute + date.minute + carry2;
        int carry3 = fQuotient(temp2, 60);
        duration.minute = mod(temp2, 60, carry3);
        int temp3 = addto.hour + date.hour + carry3;
        int carry4 = fQuotient(temp3, 24);
        duration.hour = mod(temp3, 24, carry4);
        duration.day = addto.day + date.day + carry4;
        while (true) {
            int temp4 = maxDayInMonthFor(duration.year, duration.month);
            if (duration.day < 1) {
                duration.day += maxDayInMonthFor(duration.year, duration.month - 1);
                carry = -1;
            } else if (duration.day > temp4) {
                duration.day -= temp4;
                carry = 1;
            } else {
                duration.utc = 90;
                return duration;
            }
            int temp5 = duration.month + carry;
            duration.month = modulo(temp5, 1, 13);
            duration.year += fQuotient(temp5, 1, 13);
        }
    }

    @Override
    protected double parseSecond(String buffer, int start, int end) throws NumberFormatException {
        int dot = -1;
        for (int i = start; i < end; i++) {
            char ch = buffer.charAt(i);
            if (ch == '.') {
                dot = i;
            } else if (ch > '9' || ch < '0') {
                throw new NumberFormatException("'" + buffer + "' has wrong format");
            }
        }
        int i2 = dot + 1;
        if (i2 == end) {
            throw new NumberFormatException("'" + buffer + "' has wrong format");
        }
        double value = Double.parseDouble(buffer.substring(start, end));
        if (value == Double.POSITIVE_INFINITY) {
            throw new NumberFormatException("'" + buffer + "' has wrong format");
        }
        return value;
    }

    @Override
    protected String dateToString(AbstractDateTimeDV.DateTimeData date) {
        StringBuffer message = new StringBuffer(30);
        if (date.year < 0 || date.month < 0 || date.day < 0 || date.hour < 0 || date.minute < 0 || date.second < 0.0d) {
            message.append('-');
        }
        message.append('P');
        message.append((date.year < 0 ? -1 : 1) * date.year);
        message.append('Y');
        message.append((date.month < 0 ? -1 : 1) * date.month);
        message.append('M');
        message.append((date.day < 0 ? -1 : 1) * date.day);
        message.append('D');
        message.append('T');
        message.append((date.hour < 0 ? -1 : 1) * date.hour);
        message.append('H');
        message.append((date.minute < 0 ? -1 : 1) * date.minute);
        message.append('M');
        append2(message, ((double) (date.second < 0.0d ? -1 : 1)) * date.second);
        message.append('S');
        return message.toString();
    }

    @Override
    protected Duration getDuration(AbstractDateTimeDV.DateTimeData date) {
        int sign = 1;
        if (date.year < 0 || date.month < 0 || date.day < 0 || date.hour < 0 || date.minute < 0 || date.second < 0.0d) {
            sign = -1;
        }
        return datatypeFactory.newDuration(sign == 1, date.year != Integer.MIN_VALUE ? BigInteger.valueOf(date.year * sign) : null, date.month != Integer.MIN_VALUE ? BigInteger.valueOf(date.month * sign) : null, date.day != Integer.MIN_VALUE ? BigInteger.valueOf(date.day * sign) : null, date.hour != Integer.MIN_VALUE ? BigInteger.valueOf(date.hour * sign) : null, date.minute != Integer.MIN_VALUE ? BigInteger.valueOf(date.minute * sign) : null, date.second != -2.147483648E9d ? new BigDecimal(String.valueOf(((double) sign) * date.second)) : null);
    }
}
