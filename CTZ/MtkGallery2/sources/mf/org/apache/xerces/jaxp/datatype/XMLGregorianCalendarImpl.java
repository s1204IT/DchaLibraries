package mf.org.apache.xerces.jaxp.datatype;

import com.mediatek.gallery3d.ext.DefaultActivityHooker;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import mf.javax.xml.datatype.DatatypeConstants;
import mf.javax.xml.datatype.Duration;
import mf.javax.xml.datatype.XMLGregorianCalendar;
import mf.javax.xml.namespace.QName;
import mf.org.apache.xerces.dom3.as.ASContentModel;
import mf.org.apache.xerces.util.DatatypeMessageFormatter;

class XMLGregorianCalendarImpl extends XMLGregorianCalendar implements Serializable, Cloneable {
    private static final int BILLION_I = 1000000000;
    private static final int DAY = 2;
    private static final int HOUR = 3;
    private static final int MILLISECOND = 6;
    private static final int MINUTE = 4;
    private static final int MONTH = 1;
    private static final int SECOND = 5;
    private static final int TIMEZONE = 7;
    private static final int YEAR = 0;
    private static final long serialVersionUID = 3905403108073447394L;
    private BigInteger orig_eon;
    private BigDecimal orig_fracSeconds;
    private static final BigInteger BILLION_B = BigInteger.valueOf(1000000000);
    private static final Date PURE_GREGORIAN_CHANGE = new Date(Long.MIN_VALUE);
    private static final int[] MIN_FIELD_VALUE = {Integer.MIN_VALUE, 1, 1, 0, 0, 0, 0, -840};
    private static final int[] MAX_FIELD_VALUE = {ASContentModel.AS_UNBOUNDED, 12, 31, 24, 59, 60, DefaultActivityHooker.MENU_HOOKER_GROUP_ID, 840};
    private static final String[] FIELD_NAME = {"Year", "Month", "Day", "Hour", "Minute", "Second", "Millisecond", "Timezone"};
    public static final XMLGregorianCalendar LEAP_YEAR_DEFAULT = createDateTime(400, 1, 1, 0, 0, 0, Integer.MIN_VALUE, Integer.MIN_VALUE);
    private static final BigInteger FOUR = BigInteger.valueOf(4);
    private static final BigInteger HUNDRED = BigInteger.valueOf(100);
    private static final BigInteger FOUR_HUNDRED = BigInteger.valueOf(400);
    private static final BigInteger SIXTY = BigInteger.valueOf(60);
    private static final BigInteger TWENTY_FOUR = BigInteger.valueOf(24);
    private static final BigInteger TWELVE = BigInteger.valueOf(12);
    private static final BigDecimal DECIMAL_ZERO = BigDecimal.valueOf(0L);
    private static final BigDecimal DECIMAL_ONE = BigDecimal.valueOf(1L);
    private static final BigDecimal DECIMAL_SIXTY = BigDecimal.valueOf(60L);
    private int orig_year = Integer.MIN_VALUE;
    private int orig_month = Integer.MIN_VALUE;
    private int orig_day = Integer.MIN_VALUE;
    private int orig_hour = Integer.MIN_VALUE;
    private int orig_minute = Integer.MIN_VALUE;
    private int orig_second = Integer.MIN_VALUE;
    private int orig_timezone = Integer.MIN_VALUE;
    private BigInteger eon = null;
    private int year = Integer.MIN_VALUE;
    private int month = Integer.MIN_VALUE;
    private int day = Integer.MIN_VALUE;
    private int timezone = Integer.MIN_VALUE;
    private int hour = Integer.MIN_VALUE;
    private int minute = Integer.MIN_VALUE;
    private int second = Integer.MIN_VALUE;
    private BigDecimal fractionalSecond = null;

    protected XMLGregorianCalendarImpl(String lexicalRepresentation) throws IllegalArgumentException {
        String format;
        Parser parser = null;
        int lexRepLength = lexicalRepresentation.length();
        if (lexicalRepresentation.indexOf(84) != -1) {
            format = "%Y-%M-%DT%h:%m:%s%z";
        } else if (lexRepLength < 3 || lexicalRepresentation.charAt(2) != ':') {
            if (lexicalRepresentation.startsWith("--")) {
                if (lexRepLength >= 3 && lexicalRepresentation.charAt(2) == '-') {
                    format = "---%D%z";
                } else if (lexRepLength == 4 || (lexRepLength >= 6 && (lexicalRepresentation.charAt(4) == '+' || (lexicalRepresentation.charAt(4) == '-' && (lexicalRepresentation.charAt(5) == '-' || lexRepLength == 10))))) {
                    String format2 = "--%M--%z";
                    Parser p = new Parser(this, format2, lexicalRepresentation, parser);
                    try {
                        p.parse();
                        if (!isValid()) {
                            throw new IllegalArgumentException(DatatypeMessageFormatter.formatMessage(null, "InvalidXGCRepresentation", new Object[]{lexicalRepresentation}));
                        }
                        save();
                        return;
                    } catch (IllegalArgumentException e) {
                        format = "--%M%z";
                    }
                } else {
                    format = "--%M-%D%z";
                }
            } else {
                int countSeparator = 0;
                int timezoneOffset = lexicalRepresentation.indexOf(58);
                lexRepLength = timezoneOffset != -1 ? lexRepLength - 6 : lexRepLength;
                for (int i = 1; i < lexRepLength; i++) {
                    if (lexicalRepresentation.charAt(i) == '-') {
                        countSeparator++;
                    }
                }
                if (countSeparator == 0) {
                    format = "%Y%z";
                } else if (countSeparator == 1) {
                    format = "%Y-%M%z";
                } else {
                    format = "%Y-%M-%D%z";
                }
            }
        } else {
            format = "%h:%m:%s%z";
        }
        Parser p2 = new Parser(this, format, lexicalRepresentation, parser);
        p2.parse();
        if (!isValid()) {
            throw new IllegalArgumentException(DatatypeMessageFormatter.formatMessage(null, "InvalidXGCRepresentation", new Object[]{lexicalRepresentation}));
        }
        save();
    }

    private void save() {
        this.orig_eon = this.eon;
        this.orig_year = this.year;
        this.orig_month = this.month;
        this.orig_day = this.day;
        this.orig_hour = this.hour;
        this.orig_minute = this.minute;
        this.orig_second = this.second;
        this.orig_fracSeconds = this.fractionalSecond;
        this.orig_timezone = this.timezone;
    }

    public XMLGregorianCalendarImpl() {
    }

    protected XMLGregorianCalendarImpl(BigInteger year, int month, int day, int hour, int minute, int second, BigDecimal fractionalSecond, int timezone) {
        setYear(year);
        setMonth(month);
        setDay(day);
        setTime(hour, minute, second, fractionalSecond);
        setTimezone(timezone);
        if (!isValid()) {
            throw new IllegalArgumentException(DatatypeMessageFormatter.formatMessage(null, "InvalidXGCValue-fractional", new Object[]{year, new Integer(month), new Integer(day), new Integer(hour), new Integer(minute), new Integer(second), fractionalSecond, new Integer(timezone)}));
        }
        save();
    }

    private XMLGregorianCalendarImpl(int year, int month, int day, int hour, int minute, int second, int millisecond, int timezone) {
        setYear(year);
        setMonth(month);
        setDay(day);
        setTime(hour, minute, second);
        setTimezone(timezone);
        BigDecimal realMilliseconds = millisecond != Integer.MIN_VALUE ? BigDecimal.valueOf(millisecond, 3) : null;
        setFractionalSecond(realMilliseconds);
        if (!isValid()) {
            throw new IllegalArgumentException(DatatypeMessageFormatter.formatMessage(null, "InvalidXGCValue-milli", new Object[]{new Integer(year), new Integer(month), new Integer(day), new Integer(hour), new Integer(minute), new Integer(second), new Integer(millisecond), new Integer(timezone)}));
        }
        save();
    }

    public XMLGregorianCalendarImpl(GregorianCalendar cal) {
        int year = cal.get(1);
        setYear(cal.get(0) == 0 ? -year : year);
        setMonth(cal.get(2) + 1);
        setDay(cal.get(5));
        setTime(cal.get(11), cal.get(12), cal.get(13), cal.get(14));
        int offsetInMinutes = (cal.get(15) + cal.get(16)) / 60000;
        setTimezone(offsetInMinutes);
        save();
    }

    public static XMLGregorianCalendar createDateTime(BigInteger year, int month, int day, int hours, int minutes, int seconds, BigDecimal fractionalSecond, int timezone) {
        return new XMLGregorianCalendarImpl(year, month, day, hours, minutes, seconds, fractionalSecond, timezone);
    }

    public static XMLGregorianCalendar createDateTime(int year, int month, int day, int hour, int minute, int second) {
        return new XMLGregorianCalendarImpl(year, month, day, hour, minute, second, Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    public static XMLGregorianCalendar createDateTime(int year, int month, int day, int hours, int minutes, int seconds, int milliseconds, int timezone) {
        return new XMLGregorianCalendarImpl(year, month, day, hours, minutes, seconds, milliseconds, timezone);
    }

    public static XMLGregorianCalendar createDate(int year, int month, int day, int timezone) {
        return new XMLGregorianCalendarImpl(year, month, day, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, timezone);
    }

    public static XMLGregorianCalendar createTime(int hours, int minutes, int seconds, int timezone) {
        return new XMLGregorianCalendarImpl(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, hours, minutes, seconds, Integer.MIN_VALUE, timezone);
    }

    public static XMLGregorianCalendar createTime(int hours, int minutes, int seconds, BigDecimal fractionalSecond, int timezone) {
        return new XMLGregorianCalendarImpl((BigInteger) null, Integer.MIN_VALUE, Integer.MIN_VALUE, hours, minutes, seconds, fractionalSecond, timezone);
    }

    public static XMLGregorianCalendar createTime(int hours, int minutes, int seconds, int milliseconds, int timezone) {
        return new XMLGregorianCalendarImpl(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, hours, minutes, seconds, milliseconds, timezone);
    }

    @Override
    public BigInteger getEon() {
        return this.eon;
    }

    @Override
    public int getYear() {
        return this.year;
    }

    @Override
    public BigInteger getEonAndYear() {
        if (this.year != Integer.MIN_VALUE && this.eon != null) {
            return this.eon.add(BigInteger.valueOf(this.year));
        }
        if (this.year != Integer.MIN_VALUE && this.eon == null) {
            return BigInteger.valueOf(this.year);
        }
        return null;
    }

    @Override
    public int getMonth() {
        return this.month;
    }

    @Override
    public int getDay() {
        return this.day;
    }

    @Override
    public int getTimezone() {
        return this.timezone;
    }

    @Override
    public int getHour() {
        return this.hour;
    }

    @Override
    public int getMinute() {
        return this.minute;
    }

    @Override
    public int getSecond() {
        return this.second;
    }

    private BigDecimal getSeconds() {
        if (this.second == Integer.MIN_VALUE) {
            return DECIMAL_ZERO;
        }
        BigDecimal result = BigDecimal.valueOf(this.second);
        if (this.fractionalSecond != null) {
            return result.add(this.fractionalSecond);
        }
        return result;
    }

    @Override
    public int getMillisecond() {
        if (this.fractionalSecond == null) {
            return Integer.MIN_VALUE;
        }
        return this.fractionalSecond.movePointRight(3).intValue();
    }

    @Override
    public BigDecimal getFractionalSecond() {
        return this.fractionalSecond;
    }

    @Override
    public void setYear(BigInteger year) {
        if (year == null) {
            this.eon = null;
            this.year = Integer.MIN_VALUE;
        } else {
            BigInteger temp = year.remainder(BILLION_B);
            this.year = temp.intValue();
            setEon(year.subtract(temp));
        }
    }

    @Override
    public void setYear(int year) {
        if (year == Integer.MIN_VALUE) {
            this.year = Integer.MIN_VALUE;
            this.eon = null;
        } else if (Math.abs(year) < BILLION_I) {
            this.year = year;
            this.eon = null;
        } else {
            BigInteger theYear = BigInteger.valueOf(year);
            BigInteger remainder = theYear.remainder(BILLION_B);
            this.year = remainder.intValue();
            setEon(theYear.subtract(remainder));
        }
    }

    private void setEon(BigInteger eon) {
        if (eon != null && eon.compareTo(BigInteger.ZERO) == 0) {
            this.eon = null;
        } else {
            this.eon = eon;
        }
    }

    @Override
    public void setMonth(int month) {
        checkFieldValueConstraint(1, month);
        this.month = month;
    }

    @Override
    public void setDay(int day) {
        checkFieldValueConstraint(2, day);
        this.day = day;
    }

    @Override
    public void setTimezone(int offset) {
        checkFieldValueConstraint(7, offset);
        this.timezone = offset;
    }

    @Override
    public void setTime(int hour, int minute, int second) {
        setTime(hour, minute, second, (BigDecimal) null);
    }

    private void checkFieldValueConstraint(int field, int value) throws IllegalArgumentException {
        if ((value < MIN_FIELD_VALUE[field] && value != Integer.MIN_VALUE) || value > MAX_FIELD_VALUE[field]) {
            throw new IllegalArgumentException(DatatypeMessageFormatter.formatMessage(null, "InvalidFieldValue", new Object[]{new Integer(value), FIELD_NAME[field]}));
        }
    }

    @Override
    public void setHour(int hour) {
        checkFieldValueConstraint(3, hour);
        this.hour = hour;
    }

    @Override
    public void setMinute(int minute) {
        checkFieldValueConstraint(4, minute);
        this.minute = minute;
    }

    @Override
    public void setSecond(int second) {
        checkFieldValueConstraint(5, second);
        this.second = second;
    }

    @Override
    public void setTime(int hour, int minute, int second, BigDecimal fractional) {
        setHour(hour);
        setMinute(minute);
        setSecond(second);
        setFractionalSecond(fractional);
    }

    @Override
    public void setTime(int hour, int minute, int second, int millisecond) {
        setHour(hour);
        setMinute(minute);
        setSecond(second);
        setMillisecond(millisecond);
    }

    @Override
    public int compare(XMLGregorianCalendar rhs) {
        XMLGregorianCalendar P = this;
        XMLGregorianCalendar Q = rhs;
        if (P.getTimezone() == Q.getTimezone()) {
            return internalCompare(P, Q);
        }
        if (P.getTimezone() != Integer.MIN_VALUE && Q.getTimezone() != Integer.MIN_VALUE) {
            return internalCompare((XMLGregorianCalendarImpl) P.normalize(), (XMLGregorianCalendarImpl) Q.normalize());
        }
        if (P.getTimezone() != Integer.MIN_VALUE) {
            if (P.getTimezone() != 0) {
                P = (XMLGregorianCalendarImpl) P.normalize();
            }
            XMLGregorianCalendar MinQ = normalizeToTimezone(Q, 840);
            int result = internalCompare(P, MinQ);
            if (result == -1) {
                return result;
            }
            XMLGregorianCalendar MaxQ = normalizeToTimezone(Q, -840);
            int result2 = internalCompare(P, MaxQ);
            if (result2 == 1) {
                return result2;
            }
            return 2;
        }
        if (Q.getTimezone() != 0) {
            Q = (XMLGregorianCalendarImpl) normalizeToTimezone(Q, Q.getTimezone());
        }
        XMLGregorianCalendar MaxP = normalizeToTimezone(P, -840);
        int result3 = internalCompare(MaxP, Q);
        if (result3 == -1) {
            return result3;
        }
        XMLGregorianCalendar MinP = normalizeToTimezone(P, 840);
        int result4 = internalCompare(MinP, Q);
        if (result4 == 1) {
            return result4;
        }
        return 2;
    }

    @Override
    public XMLGregorianCalendar normalize() {
        XMLGregorianCalendar normalized = normalizeToTimezone(this, this.timezone);
        if (getTimezone() == Integer.MIN_VALUE) {
            normalized.setTimezone(Integer.MIN_VALUE);
        }
        if (getMillisecond() == Integer.MIN_VALUE) {
            normalized.setMillisecond(Integer.MIN_VALUE);
        }
        return normalized;
    }

    private XMLGregorianCalendar normalizeToTimezone(XMLGregorianCalendar cal, int timezone) {
        XMLGregorianCalendar result = (XMLGregorianCalendar) cal.clone();
        int minutes = -timezone;
        Duration d = new DurationImpl(minutes >= 0, 0, 0, 0, 0, minutes < 0 ? -minutes : minutes, 0);
        result.add(d);
        result.setTimezone(0);
        return result;
    }

    private static int internalCompare(XMLGregorianCalendar P, XMLGregorianCalendar Q) {
        if (P.getEon() == Q.getEon()) {
            int result = compareField(P.getYear(), Q.getYear());
            if (result != 0) {
                return result;
            }
        } else {
            int result2 = compareField(P.getEonAndYear(), Q.getEonAndYear());
            if (result2 != 0) {
                return result2;
            }
        }
        int result3 = compareField(P.getMonth(), Q.getMonth());
        if (result3 != 0) {
            return result3;
        }
        int result4 = compareField(P.getDay(), Q.getDay());
        if (result4 != 0) {
            return result4;
        }
        int result5 = compareField(P.getHour(), Q.getHour());
        if (result5 != 0) {
            return result5;
        }
        int result6 = compareField(P.getMinute(), Q.getMinute());
        if (result6 != 0) {
            return result6;
        }
        int result7 = compareField(P.getSecond(), Q.getSecond());
        if (result7 != 0) {
            return result7;
        }
        return compareField(P.getFractionalSecond(), Q.getFractionalSecond());
    }

    private static int compareField(int Pfield, int Qfield) {
        if (Pfield == Qfield) {
            return 0;
        }
        if (Pfield == Integer.MIN_VALUE || Qfield == Integer.MIN_VALUE) {
            return 2;
        }
        return Pfield < Qfield ? -1 : 1;
    }

    private static int compareField(BigInteger Pfield, BigInteger Qfield) {
        if (Pfield == null) {
            return Qfield == null ? 0 : 2;
        }
        if (Qfield == null) {
            return 2;
        }
        return Pfield.compareTo(Qfield);
    }

    private static int compareField(BigDecimal Pfield, BigDecimal Qfield) {
        if (Pfield == Qfield) {
            return 0;
        }
        if (Pfield == null) {
            Pfield = DECIMAL_ZERO;
        }
        if (Qfield == null) {
            Qfield = DECIMAL_ZERO;
        }
        return Pfield.compareTo(Qfield);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        return (obj instanceof XMLGregorianCalendar) && compare((XMLGregorianCalendar) obj) == 0;
    }

    @Override
    public int hashCode() {
        int timezone = getTimezone();
        if (timezone == Integer.MIN_VALUE) {
            timezone = 0;
        }
        XMLGregorianCalendar gc = this;
        if (timezone != 0) {
            gc = normalizeToTimezone(this, getTimezone());
        }
        return gc.getYear() + gc.getMonth() + gc.getDay() + gc.getHour() + gc.getMinute() + gc.getSecond();
    }

    public static XMLGregorianCalendar parse(String lexicalRepresentation) {
        return new XMLGregorianCalendarImpl(lexicalRepresentation);
    }

    @Override
    public String toXMLFormat() {
        QName typekind = getXMLSchemaType();
        String formatString = null;
        if (typekind == DatatypeConstants.DATETIME) {
            formatString = "%Y-%M-%DT%h:%m:%s%z";
        } else if (typekind == DatatypeConstants.DATE) {
            formatString = "%Y-%M-%D%z";
        } else if (typekind == DatatypeConstants.TIME) {
            formatString = "%h:%m:%s%z";
        } else if (typekind == DatatypeConstants.GMONTH) {
            formatString = "--%M--%z";
        } else if (typekind == DatatypeConstants.GDAY) {
            formatString = "---%D%z";
        } else if (typekind == DatatypeConstants.GYEAR) {
            formatString = "%Y%z";
        } else if (typekind == DatatypeConstants.GYEARMONTH) {
            formatString = "%Y-%M%z";
        } else if (typekind == DatatypeConstants.GMONTHDAY) {
            formatString = "--%M-%D%z";
        }
        return format(formatString);
    }

    @Override
    public QName getXMLSchemaType() {
        if (this.year != Integer.MIN_VALUE && this.month != Integer.MIN_VALUE && this.day != Integer.MIN_VALUE && this.hour != Integer.MIN_VALUE && this.minute != Integer.MIN_VALUE && this.second != Integer.MIN_VALUE) {
            return DatatypeConstants.DATETIME;
        }
        if (this.year != Integer.MIN_VALUE && this.month != Integer.MIN_VALUE && this.day != Integer.MIN_VALUE && this.hour == Integer.MIN_VALUE && this.minute == Integer.MIN_VALUE && this.second == Integer.MIN_VALUE) {
            return DatatypeConstants.DATE;
        }
        if (this.year == Integer.MIN_VALUE && this.month == Integer.MIN_VALUE && this.day == Integer.MIN_VALUE && this.hour != Integer.MIN_VALUE && this.minute != Integer.MIN_VALUE && this.second != Integer.MIN_VALUE) {
            return DatatypeConstants.TIME;
        }
        if (this.year != Integer.MIN_VALUE && this.month != Integer.MIN_VALUE && this.day == Integer.MIN_VALUE && this.hour == Integer.MIN_VALUE && this.minute == Integer.MIN_VALUE && this.second == Integer.MIN_VALUE) {
            return DatatypeConstants.GYEARMONTH;
        }
        if (this.year == Integer.MIN_VALUE && this.month != Integer.MIN_VALUE && this.day != Integer.MIN_VALUE && this.hour == Integer.MIN_VALUE && this.minute == Integer.MIN_VALUE && this.second == Integer.MIN_VALUE) {
            return DatatypeConstants.GMONTHDAY;
        }
        if (this.year != Integer.MIN_VALUE && this.month == Integer.MIN_VALUE && this.day == Integer.MIN_VALUE && this.hour == Integer.MIN_VALUE && this.minute == Integer.MIN_VALUE && this.second == Integer.MIN_VALUE) {
            return DatatypeConstants.GYEAR;
        }
        if (this.year == Integer.MIN_VALUE && this.month != Integer.MIN_VALUE && this.day == Integer.MIN_VALUE && this.hour == Integer.MIN_VALUE && this.minute == Integer.MIN_VALUE && this.second == Integer.MIN_VALUE) {
            return DatatypeConstants.GMONTH;
        }
        if (this.year == Integer.MIN_VALUE && this.month == Integer.MIN_VALUE && this.day != Integer.MIN_VALUE && this.hour == Integer.MIN_VALUE && this.minute == Integer.MIN_VALUE && this.second == Integer.MIN_VALUE) {
            return DatatypeConstants.GDAY;
        }
        throw new IllegalStateException(String.valueOf(getClass().getName()) + "#getXMLSchemaType() :" + DatatypeMessageFormatter.formatMessage(null, "InvalidXGCFields", null));
    }

    @Override
    public boolean isValid() {
        if (this.month != Integer.MIN_VALUE && this.day != Integer.MIN_VALUE) {
            if (this.year != Integer.MIN_VALUE) {
                if (this.eon == null) {
                    if (this.day > maximumDayInMonthFor(this.year, this.month)) {
                        return false;
                    }
                } else if (this.day > maximumDayInMonthFor(getEonAndYear(), this.month)) {
                    return false;
                }
            } else if (this.day > maximumDayInMonthFor(2000, this.month)) {
                return false;
            }
        }
        if (this.hour != 24 || (this.minute == 0 && this.second == 0 && (this.fractionalSecond == null || this.fractionalSecond.compareTo(DECIMAL_ZERO) == 0))) {
            return (this.eon == null && this.year == 0) ? false : true;
        }
        return false;
    }

    @Override
    public void add(Duration duration) {
        BigDecimal startSeconds;
        BigInteger tempDays;
        BigInteger tempDays2;
        BigInteger temp;
        BigInteger carry;
        int monthCarry;
        BigInteger dMinutes;
        int endMonth;
        int quotient;
        int startHours;
        BigInteger mdimf;
        boolean[] fieldUndefined = new boolean[6];
        int signum = duration.getSign();
        int startMonth = getMonth();
        if (startMonth == Integer.MIN_VALUE) {
            startMonth = MIN_FIELD_VALUE[1];
            fieldUndefined[1] = true;
        }
        BigInteger dMonths = sanitize(duration.getField(DatatypeConstants.MONTHS), signum);
        BigInteger temp2 = BigInteger.valueOf(startMonth).add(dMonths);
        setMonth(temp2.subtract(BigInteger.ONE).mod(TWELVE).intValue() + 1);
        BigInteger carry2 = new BigDecimal(temp2.subtract(BigInteger.ONE)).divide(new BigDecimal(TWELVE), 3).toBigInteger();
        BigInteger startYear = getEonAndYear();
        if (startYear == null) {
            fieldUndefined[0] = true;
            startYear = BigInteger.ZERO;
        }
        BigInteger dYears = sanitize(duration.getField(DatatypeConstants.YEARS), signum);
        BigInteger endYear = startYear.add(dYears).add(carry2);
        setYear(endYear);
        if (getSecond() == Integer.MIN_VALUE) {
            fieldUndefined[5] = true;
            startSeconds = DECIMAL_ZERO;
        } else {
            startSeconds = getSeconds();
        }
        BigDecimal dSeconds = DurationImpl.sanitize((BigDecimal) duration.getField(DatatypeConstants.SECONDS), signum);
        BigDecimal tempBD = startSeconds.add(dSeconds);
        BigDecimal fQuotient = new BigDecimal(new BigDecimal(tempBD.toBigInteger()).divide(DECIMAL_SIXTY, 3).toBigInteger());
        BigDecimal endSeconds = tempBD.subtract(fQuotient.multiply(DECIMAL_SIXTY));
        BigInteger carry3 = fQuotient.toBigInteger();
        setSecond(endSeconds.intValue());
        BigDecimal tempFracSeconds = endSeconds.subtract(new BigDecimal(BigInteger.valueOf(getSecond())));
        if (tempFracSeconds.compareTo(DECIMAL_ZERO) < 0) {
            setFractionalSecond(DECIMAL_ONE.add(tempFracSeconds));
            if (getSecond() == 0) {
                setSecond(59);
                carry3 = carry3.subtract(BigInteger.ONE);
            } else {
                setSecond(getSecond() - 1);
            }
        } else {
            setFractionalSecond(tempFracSeconds);
        }
        int startMinutes = getMinute();
        if (startMinutes == Integer.MIN_VALUE) {
            fieldUndefined[4] = true;
            startMinutes = MIN_FIELD_VALUE[4];
        }
        BigInteger dMinutes2 = sanitize(duration.getField(DatatypeConstants.MINUTES), signum);
        BigInteger temp3 = BigInteger.valueOf(startMinutes).add(dMinutes2).add(carry3);
        setMinute(temp3.mod(SIXTY).intValue());
        BigInteger carry4 = new BigDecimal(temp3).divide(DECIMAL_SIXTY, 3).toBigInteger();
        int startHours2 = getHour();
        if (startHours2 == Integer.MIN_VALUE) {
            fieldUndefined[3] = true;
            startHours2 = MIN_FIELD_VALUE[3];
        }
        BigInteger dHours = sanitize(duration.getField(DatatypeConstants.HOURS), signum);
        BigInteger temp4 = BigInteger.valueOf(startHours2).add(dHours).add(carry4);
        setHour(temp4.mod(TWENTY_FOUR).intValue());
        BigInteger carry5 = new BigDecimal(temp4).divide(new BigDecimal(TWENTY_FOUR), 3).toBigInteger();
        int startDay = getDay();
        if (startDay == Integer.MIN_VALUE) {
            fieldUndefined[2] = true;
            startDay = MIN_FIELD_VALUE[2];
        }
        BigInteger dDays = sanitize(duration.getField(DatatypeConstants.DAYS), signum);
        int maxDayInMonth = maximumDayInMonthFor(getEonAndYear(), getMonth());
        if (startDay > maxDayInMonth) {
            tempDays = BigInteger.valueOf(maxDayInMonth);
        } else if (startDay < 1) {
            tempDays = BigInteger.ONE;
        } else {
            tempDays = BigInteger.valueOf(startDay);
        }
        BigInteger endDays = tempDays.add(dDays).add(carry5);
        while (true) {
            int maxDayInMonth2 = maxDayInMonth;
            if (endDays.compareTo(BigInteger.ONE) < 0) {
                tempDays2 = tempDays;
                if (this.month >= 2) {
                    temp = temp4;
                    carry = carry5;
                    mdimf = BigInteger.valueOf(maximumDayInMonthFor(getEonAndYear(), getMonth() - 1));
                } else {
                    temp = temp4;
                    carry = carry5;
                    mdimf = BigInteger.valueOf(maximumDayInMonthFor(getEonAndYear().subtract(BigInteger.valueOf(1L)), 12));
                }
                monthCarry = -1;
                endDays = endDays.add(mdimf);
            } else {
                tempDays2 = tempDays;
                temp = temp4;
                carry = carry5;
                if (endDays.compareTo(BigInteger.valueOf(maximumDayInMonthFor(getEonAndYear(), getMonth()))) <= 0) {
                    break;
                }
                endDays = endDays.add(BigInteger.valueOf(-maximumDayInMonthFor(getEonAndYear(), getMonth())));
                monthCarry = 1;
            }
            int intTemp = getMonth() + monthCarry;
            int endMonth2 = (intTemp - 1) % 12;
            if (endMonth2 < 0) {
                int endMonth3 = 12 + endMonth2 + 1;
                dMinutes = dMinutes2;
                int quotient2 = BigDecimal.valueOf(intTemp - 1).divide(new BigDecimal(TWELVE), 0).intValue();
                endMonth = quotient2;
                quotient = endMonth3;
            } else {
                dMinutes = dMinutes2;
                endMonth = (intTemp - 1) / 12;
                quotient = endMonth2 + 1;
            }
            setMonth(quotient);
            if (endMonth == 0) {
                startHours = startHours2;
            } else {
                startHours = startHours2;
                setYear(getEonAndYear().add(BigInteger.valueOf(endMonth)));
            }
            maxDayInMonth = maxDayInMonth2;
            tempDays = tempDays2;
            temp4 = temp;
            carry5 = carry;
            dMinutes2 = dMinutes;
            startHours2 = startHours;
        }
        setDay(endDays.intValue());
        for (int i = 0; i <= 5; i++) {
            if (fieldUndefined[i]) {
                switch (i) {
                    case 0:
                        setYear(Integer.MIN_VALUE);
                        break;
                    case 1:
                        setMonth(Integer.MIN_VALUE);
                        break;
                    case 2:
                        setDay(Integer.MIN_VALUE);
                        break;
                    case 3:
                        setHour(Integer.MIN_VALUE);
                        break;
                    case 4:
                        setMinute(Integer.MIN_VALUE);
                        break;
                    case 5:
                        setSecond(Integer.MIN_VALUE);
                        setFractionalSecond(null);
                        break;
                }
            }
        }
    }

    private static class DaysInMonth {
        private static final int[] table = {0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

        private DaysInMonth() {
        }
    }

    private static int maximumDayInMonthFor(BigInteger year, int month) {
        if (month != 2) {
            return DaysInMonth.table[month];
        }
        if (!year.mod(FOUR_HUNDRED).equals(BigInteger.ZERO)) {
            if (year.mod(HUNDRED).equals(BigInteger.ZERO) || !year.mod(FOUR).equals(BigInteger.ZERO)) {
                return DaysInMonth.table[month];
            }
            return 29;
        }
        return 29;
    }

    private static int maximumDayInMonthFor(int year, int month) {
        if (month != 2) {
            return DaysInMonth.table[month];
        }
        if (year % 400 != 0) {
            if (year % 100 == 0 || year % 4 != 0) {
                return DaysInMonth.table[2];
            }
            return 29;
        }
        return 29;
    }

    @Override
    public GregorianCalendar toGregorianCalendar() {
        TimeZone tz = getTimeZone(Integer.MIN_VALUE);
        Locale locale = Locale.getDefault();
        GregorianCalendar result = new GregorianCalendar(tz, locale);
        result.clear();
        result.setGregorianChange(PURE_GREGORIAN_CHANGE);
        if (this.year != Integer.MIN_VALUE) {
            if (this.eon == null) {
                result.set(0, this.year < 0 ? 0 : 1);
                result.set(1, Math.abs(this.year));
            } else {
                BigInteger eonAndYear = getEonAndYear();
                result.set(0, eonAndYear.signum() == -1 ? 0 : 1);
                result.set(1, eonAndYear.abs().intValue());
            }
        }
        if (this.month != Integer.MIN_VALUE) {
            result.set(2, this.month - 1);
        }
        if (this.day != Integer.MIN_VALUE) {
            result.set(5, this.day);
        }
        if (this.hour != Integer.MIN_VALUE) {
            result.set(11, this.hour);
        }
        if (this.minute != Integer.MIN_VALUE) {
            result.set(12, this.minute);
        }
        if (this.second != Integer.MIN_VALUE) {
            result.set(13, this.second);
        }
        if (this.fractionalSecond != null) {
            result.set(14, getMillisecond());
        }
        return result;
    }

    @Override
    public GregorianCalendar toGregorianCalendar(TimeZone timezone, Locale aLocale, XMLGregorianCalendar defaults) {
        int defaultYear;
        TimeZone tz = timezone;
        if (tz == null) {
            int defaultZoneoffset = Integer.MIN_VALUE;
            if (defaults != null) {
                defaultZoneoffset = defaults.getTimezone();
            }
            tz = getTimeZone(defaultZoneoffset);
        }
        if (aLocale == null) {
            aLocale = Locale.getDefault();
        }
        GregorianCalendar result = new GregorianCalendar(tz, aLocale);
        result.clear();
        result.setGregorianChange(PURE_GREGORIAN_CHANGE);
        if (this.year != Integer.MIN_VALUE) {
            if (this.eon == null) {
                result.set(0, this.year < 0 ? 0 : 1);
                result.set(1, Math.abs(this.year));
            } else {
                BigInteger eonAndYear = getEonAndYear();
                result.set(0, eonAndYear.signum() == -1 ? 0 : 1);
                result.set(1, eonAndYear.abs().intValue());
            }
        } else if (defaults != null && (defaultYear = defaults.getYear()) != Integer.MIN_VALUE) {
            if (defaults.getEon() == null) {
                result.set(0, defaultYear < 0 ? 0 : 1);
                result.set(1, Math.abs(defaultYear));
            } else {
                BigInteger defaultEonAndYear = defaults.getEonAndYear();
                result.set(0, defaultEonAndYear.signum() == -1 ? 0 : 1);
                result.set(1, defaultEonAndYear.abs().intValue());
            }
        }
        if (this.month != Integer.MIN_VALUE) {
            result.set(2, this.month - 1);
        } else {
            int defaultMonth = defaults != null ? defaults.getMonth() : Integer.MIN_VALUE;
            if (defaultMonth != Integer.MIN_VALUE) {
                result.set(2, defaultMonth - 1);
            }
        }
        if (this.day != Integer.MIN_VALUE) {
            result.set(5, this.day);
        } else {
            int defaultDay = defaults != null ? defaults.getDay() : Integer.MIN_VALUE;
            if (defaultDay != Integer.MIN_VALUE) {
                result.set(5, defaultDay);
            }
        }
        if (this.hour != Integer.MIN_VALUE) {
            result.set(11, this.hour);
        } else {
            int defaultHour = defaults != null ? defaults.getHour() : Integer.MIN_VALUE;
            if (defaultHour != Integer.MIN_VALUE) {
                result.set(11, defaultHour);
            }
        }
        if (this.minute != Integer.MIN_VALUE) {
            result.set(12, this.minute);
        } else {
            int defaultMinute = defaults != null ? defaults.getMinute() : Integer.MIN_VALUE;
            if (defaultMinute != Integer.MIN_VALUE) {
                result.set(12, defaultMinute);
            }
        }
        if (this.second != Integer.MIN_VALUE) {
            result.set(13, this.second);
        } else {
            int defaultSecond = defaults != null ? defaults.getSecond() : Integer.MIN_VALUE;
            if (defaultSecond != Integer.MIN_VALUE) {
                result.set(13, defaultSecond);
            }
        }
        if (this.fractionalSecond != null) {
            result.set(14, getMillisecond());
        } else {
            BigDecimal defaultFractionalSecond = defaults != null ? defaults.getFractionalSecond() : null;
            if (defaultFractionalSecond != null) {
                result.set(14, defaults.getMillisecond());
            }
        }
        return result;
    }

    @Override
    public TimeZone getTimeZone(int defaultZoneoffset) {
        int zoneoffset = getTimezone();
        if (zoneoffset == Integer.MIN_VALUE) {
            zoneoffset = defaultZoneoffset;
        }
        if (zoneoffset == Integer.MIN_VALUE) {
            TimeZone result = TimeZone.getDefault();
            return result;
        }
        char sign = zoneoffset < 0 ? '-' : '+';
        if (sign == '-') {
            zoneoffset = -zoneoffset;
        }
        int hour = zoneoffset / 60;
        int minutes = zoneoffset - (hour * 60);
        StringBuffer customTimezoneId = new StringBuffer(8);
        customTimezoneId.append("GMT");
        customTimezoneId.append(sign);
        customTimezoneId.append(hour);
        if (minutes != 0) {
            if (minutes < 10) {
                customTimezoneId.append('0');
            }
            customTimezoneId.append(minutes);
        }
        TimeZone result2 = TimeZone.getTimeZone(customTimezoneId.toString());
        return result2;
    }

    @Override
    public Object clone() {
        return new XMLGregorianCalendarImpl(getEonAndYear(), this.month, this.day, this.hour, this.minute, this.second, this.fractionalSecond, this.timezone);
    }

    @Override
    public void clear() {
        this.eon = null;
        this.year = Integer.MIN_VALUE;
        this.month = Integer.MIN_VALUE;
        this.day = Integer.MIN_VALUE;
        this.timezone = Integer.MIN_VALUE;
        this.hour = Integer.MIN_VALUE;
        this.minute = Integer.MIN_VALUE;
        this.second = Integer.MIN_VALUE;
        this.fractionalSecond = null;
    }

    @Override
    public void setMillisecond(int millisecond) {
        if (millisecond == Integer.MIN_VALUE) {
            this.fractionalSecond = null;
        } else {
            checkFieldValueConstraint(6, millisecond);
            this.fractionalSecond = BigDecimal.valueOf(millisecond, 3);
        }
    }

    @Override
    public void setFractionalSecond(BigDecimal fractional) {
        if (fractional != null && (fractional.compareTo(DECIMAL_ZERO) < 0 || fractional.compareTo(DECIMAL_ONE) > 0)) {
            throw new IllegalArgumentException(DatatypeMessageFormatter.formatMessage(null, "InvalidFractional", new Object[]{fractional}));
        }
        this.fractionalSecond = fractional;
    }

    private final class Parser {
        private int fidx;
        private final int flen;
        private final String format;
        private final String value;
        private int vidx;
        private final int vlen;

        private Parser(String format, String value) {
            this.format = format;
            this.value = value;
            this.flen = format.length();
            this.vlen = value.length();
        }

        Parser(XMLGregorianCalendarImpl xMLGregorianCalendarImpl, String str, String str2, Parser parser) {
            this(str, str2);
        }

        public void parse() throws IllegalArgumentException {
            while (this.fidx < this.flen) {
                String str = this.format;
                int i = this.fidx;
                this.fidx = i + 1;
                char fch = str.charAt(i);
                if (fch != '%') {
                    skip(fch);
                } else {
                    String str2 = this.format;
                    int i2 = this.fidx;
                    this.fidx = i2 + 1;
                    char cCharAt = str2.charAt(i2);
                    if (cCharAt == 'D') {
                        XMLGregorianCalendarImpl.this.setDay(parseInt(2, 2));
                    } else if (cCharAt == 'M') {
                        XMLGregorianCalendarImpl.this.setMonth(parseInt(2, 2));
                    } else if (cCharAt == 'Y') {
                        parseYear();
                    } else if (cCharAt == 'h') {
                        XMLGregorianCalendarImpl.this.setHour(parseInt(2, 2));
                    } else if (cCharAt == 'm') {
                        XMLGregorianCalendarImpl.this.setMinute(parseInt(2, 2));
                    } else if (cCharAt == 's') {
                        XMLGregorianCalendarImpl.this.setSecond(parseInt(2, 2));
                        if (peek() == '.') {
                            XMLGregorianCalendarImpl.this.setFractionalSecond(parseBigDecimal());
                        }
                    } else if (cCharAt == 'z') {
                        char vch = peek();
                        if (vch == 'Z') {
                            this.vidx++;
                            XMLGregorianCalendarImpl.this.setTimezone(0);
                        } else if (vch == '+' || vch == '-') {
                            this.vidx++;
                            int h = parseInt(2, 2);
                            skip(':');
                            int m = parseInt(2, 2);
                            XMLGregorianCalendarImpl.this.setTimezone(((h * 60) + m) * (vch != '+' ? -1 : 1));
                        }
                    } else {
                        throw new InternalError();
                    }
                }
            }
            if (this.vidx != this.vlen) {
                throw new IllegalArgumentException(this.value);
            }
        }

        private char peek() throws IllegalArgumentException {
            if (this.vidx == this.vlen) {
                return (char) 65535;
            }
            return this.value.charAt(this.vidx);
        }

        private char read() throws IllegalArgumentException {
            if (this.vidx == this.vlen) {
                throw new IllegalArgumentException(this.value);
            }
            String str = this.value;
            int i = this.vidx;
            this.vidx = i + 1;
            return str.charAt(i);
        }

        private void skip(char ch) throws IllegalArgumentException {
            if (read() != ch) {
                throw new IllegalArgumentException(this.value);
            }
        }

        private void parseYear() throws IllegalArgumentException {
            int vstart = this.vidx;
            int sign = 0;
            if (peek() == '-') {
                this.vidx++;
                sign = 1;
            }
            while (XMLGregorianCalendarImpl.isDigit(peek())) {
                this.vidx++;
            }
            int digits = (this.vidx - vstart) - sign;
            if (digits < 4) {
                throw new IllegalArgumentException(this.value);
            }
            String yearString = this.value.substring(vstart, this.vidx);
            if (digits < 10) {
                XMLGregorianCalendarImpl.this.setYear(Integer.parseInt(yearString));
            } else {
                XMLGregorianCalendarImpl.this.setYear(new BigInteger(yearString));
            }
        }

        private int parseInt(int minDigits, int maxDigits) throws IllegalArgumentException {
            int vstart = this.vidx;
            while (XMLGregorianCalendarImpl.isDigit(peek()) && this.vidx - vstart < maxDigits) {
                this.vidx++;
            }
            if (this.vidx - vstart < minDigits) {
                throw new IllegalArgumentException(this.value);
            }
            return Integer.parseInt(this.value.substring(vstart, this.vidx));
        }

        private BigDecimal parseBigDecimal() throws IllegalArgumentException {
            int vstart = this.vidx;
            if (peek() == '.') {
                this.vidx++;
                while (XMLGregorianCalendarImpl.isDigit(peek())) {
                    this.vidx++;
                }
                return new BigDecimal(this.value.substring(vstart, this.vidx));
            }
            throw new IllegalArgumentException(this.value);
        }
    }

    private static boolean isDigit(char ch) {
        return '0' <= ch && ch <= '9';
    }

    private String format(String format) {
        StringBuffer buf = new StringBuffer();
        int fidx = 0;
        int flen = format.length();
        while (fidx < flen) {
            int fidx2 = fidx + 1;
            char fch = format.charAt(fidx);
            if (fch != '%') {
                buf.append(fch);
                fidx = fidx2;
            } else {
                int fidx3 = fidx2 + 1;
                int fidx4 = format.charAt(fidx2);
                if (fidx4 == 68) {
                    printNumber(buf, getDay(), 2);
                } else if (fidx4 == 77) {
                    printNumber(buf, getMonth(), 2);
                } else if (fidx4 != 89) {
                    if (fidx4 == 104) {
                        printNumber(buf, getHour(), 2);
                    } else if (fidx4 == 109) {
                        printNumber(buf, getMinute(), 2);
                    } else if (fidx4 == 115) {
                        printNumber(buf, getSecond(), 2);
                        if (getFractionalSecond() != null) {
                            String frac = toString(getFractionalSecond());
                            buf.append(frac.substring(1, frac.length()));
                        }
                    } else if (fidx4 == 122) {
                        int offset = getTimezone();
                        if (offset == 0) {
                            buf.append('Z');
                        } else if (offset != Integer.MIN_VALUE) {
                            if (offset < 0) {
                                buf.append('-');
                                offset *= -1;
                            } else {
                                buf.append('+');
                            }
                            printNumber(buf, offset / 60, 2);
                            buf.append(':');
                            printNumber(buf, offset % 60, 2);
                        }
                    } else {
                        throw new InternalError();
                    }
                } else if (this.eon == null) {
                    int absYear = this.year;
                    if (absYear < 0) {
                        buf.append('-');
                        absYear = -this.year;
                    }
                    printNumber(buf, absYear, 4);
                } else {
                    printNumber(buf, getEonAndYear(), 4);
                }
                fidx = fidx3;
            }
        }
        return buf.toString();
    }

    private void printNumber(StringBuffer out, int number, int nDigits) {
        String s = String.valueOf(number);
        for (int i = s.length(); i < nDigits; i++) {
            out.append('0');
        }
        out.append(s);
    }

    private void printNumber(StringBuffer out, BigInteger number, int nDigits) {
        String s = number.toString();
        for (int i = s.length(); i < nDigits; i++) {
            out.append('0');
        }
        out.append(s);
    }

    private String toString(BigDecimal bd) {
        StringBuffer buf;
        String intString = bd.unscaledValue().toString();
        int scale = bd.scale();
        if (scale == 0) {
            return intString;
        }
        int insertionPoint = intString.length() - scale;
        if (insertionPoint == 0) {
            return "0." + intString;
        }
        if (insertionPoint > 0) {
            buf = new StringBuffer(intString);
            buf.insert(insertionPoint, '.');
        } else {
            buf = new StringBuffer((3 - insertionPoint) + intString.length());
            buf.append("0.");
            for (int i = 0; i < (-insertionPoint); i++) {
                buf.append('0');
            }
            buf.append(intString);
        }
        return buf.toString();
    }

    static BigInteger sanitize(Number value, int signum) {
        if (signum == 0 || value == null) {
            return BigInteger.ZERO;
        }
        return signum < 0 ? ((BigInteger) value).negate() : (BigInteger) value;
    }

    @Override
    public void reset() {
        this.eon = this.orig_eon;
        this.year = this.orig_year;
        this.month = this.orig_month;
        this.day = this.orig_day;
        this.hour = this.orig_hour;
        this.minute = this.orig_minute;
        this.second = this.orig_second;
        this.fractionalSecond = this.orig_fracSeconds;
        this.timezone = this.orig_timezone;
    }

    private Object writeReplace() throws IOException {
        return new SerializedXMLGregorianCalendar(toXMLFormat());
    }
}
