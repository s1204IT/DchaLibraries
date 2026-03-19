package mf.org.apache.xerces.jaxp.datatype;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import mf.javax.xml.datatype.DatatypeConstants;
import mf.javax.xml.datatype.Duration;
import mf.javax.xml.datatype.XMLGregorianCalendar;
import mf.org.apache.xerces.util.DatatypeMessageFormatter;

class DurationImpl extends Duration implements Serializable {
    private static final BigDecimal[] FACTORS;
    private static final long serialVersionUID = -2650025807136350131L;
    private final BigInteger days;
    private final BigInteger hours;
    private final BigInteger minutes;
    private final BigInteger months;
    private final BigDecimal seconds;
    private final int signum;
    private final BigInteger years;
    private static final DatatypeConstants.Field[] FIELDS = {DatatypeConstants.YEARS, DatatypeConstants.MONTHS, DatatypeConstants.DAYS, DatatypeConstants.HOURS, DatatypeConstants.MINUTES, DatatypeConstants.SECONDS};
    private static final BigDecimal ZERO = BigDecimal.valueOf(0L);
    private static final XMLGregorianCalendar[] TEST_POINTS = {XMLGregorianCalendarImpl.parse("1696-09-01T00:00:00Z"), XMLGregorianCalendarImpl.parse("1697-02-01T00:00:00Z"), XMLGregorianCalendarImpl.parse("1903-03-01T00:00:00Z"), XMLGregorianCalendarImpl.parse("1903-07-01T00:00:00Z")};

    static {
        BigDecimal[] bigDecimalArr = new BigDecimal[5];
        bigDecimalArr[0] = BigDecimal.valueOf(12L);
        bigDecimalArr[2] = BigDecimal.valueOf(24L);
        bigDecimalArr[3] = BigDecimal.valueOf(60L);
        bigDecimalArr[4] = BigDecimal.valueOf(60L);
        FACTORS = bigDecimalArr;
    }

    @Override
    public int getSign() {
        return this.signum;
    }

    private int calcSignum(boolean isPositive) {
        if ((this.years == null || this.years.signum() == 0) && ((this.months == null || this.months.signum() == 0) && ((this.days == null || this.days.signum() == 0) && ((this.hours == null || this.hours.signum() == 0) && ((this.minutes == null || this.minutes.signum() == 0) && (this.seconds == null || this.seconds.signum() == 0)))))) {
            return 0;
        }
        if (isPositive) {
            return 1;
        }
        return -1;
    }

    protected DurationImpl(boolean isPositive, BigInteger years, BigInteger months, BigInteger days, BigInteger hours, BigInteger minutes, BigDecimal seconds) {
        this.years = years;
        this.months = months;
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;
        this.signum = calcSignum(isPositive);
        if (years == null && months == null && days == null && hours == null && minutes == null && seconds == null) {
            throw new IllegalArgumentException(DatatypeMessageFormatter.formatMessage(null, "AllFieldsNull", null));
        }
        testNonNegative(years, DatatypeConstants.YEARS);
        testNonNegative(months, DatatypeConstants.MONTHS);
        testNonNegative(days, DatatypeConstants.DAYS);
        testNonNegative(hours, DatatypeConstants.HOURS);
        testNonNegative(minutes, DatatypeConstants.MINUTES);
        testNonNegative(seconds, DatatypeConstants.SECONDS);
    }

    private static void testNonNegative(BigInteger n, DatatypeConstants.Field f) {
        if (n != null && n.signum() < 0) {
            throw new IllegalArgumentException(DatatypeMessageFormatter.formatMessage(null, "NegativeField", new Object[]{f.toString()}));
        }
    }

    private static void testNonNegative(BigDecimal n, DatatypeConstants.Field f) {
        if (n != null && n.signum() < 0) {
            throw new IllegalArgumentException(DatatypeMessageFormatter.formatMessage(null, "NegativeField", new Object[]{f.toString()}));
        }
    }

    protected DurationImpl(boolean isPositive, int years, int months, int days, int hours, int minutes, int seconds) {
        this(isPositive, wrap(years), wrap(months), wrap(days), wrap(hours), wrap(minutes), seconds != 0 ? BigDecimal.valueOf(seconds) : null);
    }

    private static BigInteger wrap(int i) {
        if (i == Integer.MIN_VALUE) {
            return null;
        }
        return BigInteger.valueOf(i);
    }

    protected DurationImpl(long durationInMilliSeconds) {
        boolean is0x8000000000000000L = false;
        long l = durationInMilliSeconds;
        if (l > 0) {
            this.signum = 1;
        } else if (l < 0) {
            this.signum = -1;
            if (l == Long.MIN_VALUE) {
                l++;
                is0x8000000000000000L = true;
            }
            l *= -1;
        } else {
            this.signum = 0;
        }
        this.years = null;
        this.months = null;
        this.seconds = BigDecimal.valueOf((l % 60000) + ((long) (is0x8000000000000000L ? 1 : 0)), 3);
        long l2 = l / 60000;
        this.minutes = l2 == 0 ? null : BigInteger.valueOf(l2 % 60);
        long l3 = l2 / 60;
        this.hours = l3 == 0 ? null : BigInteger.valueOf(l3 % 24);
        long l4 = l3 / 24;
        this.days = l4 != 0 ? BigInteger.valueOf(l4) : null;
    }

    protected DurationImpl(String lexicalRepresentation) throws IllegalArgumentException {
        boolean positive;
        if (lexicalRepresentation == null) {
            throw new NullPointerException();
        }
        int length = lexicalRepresentation.length();
        boolean timeRequired = false;
        int[] idx = {0};
        if (length != idx[0] && lexicalRepresentation.charAt(idx[0]) == '-') {
            idx[0] = idx[0] + 1;
            positive = false;
        } else {
            positive = true;
        }
        if (length != idx[0]) {
            int i = idx[0];
            idx[0] = i + 1;
            if (lexicalRepresentation.charAt(i) != 'P') {
                throw new IllegalArgumentException(lexicalRepresentation);
            }
        }
        int dateLen = 0;
        String[] dateParts = new String[3];
        int[] datePartsIndex = new int[3];
        while (length != idx[0] && isDigit(lexicalRepresentation.charAt(idx[0])) && dateLen < 3) {
            datePartsIndex[dateLen] = idx[0];
            dateParts[dateLen] = parsePiece(lexicalRepresentation, idx);
            dateLen++;
        }
        int dateLen2 = idx[0];
        if (length != dateLen2) {
            int i2 = idx[0];
            idx[0] = i2 + 1;
            if (lexicalRepresentation.charAt(i2) != 'T') {
                throw new IllegalArgumentException(lexicalRepresentation);
            }
            timeRequired = true;
        }
        int timeLen = 0;
        String[] timeParts = new String[3];
        int[] timePartsIndex = new int[3];
        while (length != idx[0] && isDigitOrPeriod(lexicalRepresentation.charAt(idx[0])) && timeLen < 3) {
            timePartsIndex[timeLen] = idx[0];
            timeParts[timeLen] = parsePiece(lexicalRepresentation, idx);
            timeLen++;
        }
        if (timeRequired && timeLen == 0) {
            throw new IllegalArgumentException(lexicalRepresentation);
        }
        if (length != idx[0]) {
            throw new IllegalArgumentException(lexicalRepresentation);
        }
        if (dateLen != 0 || timeLen != 0) {
            organizeParts(lexicalRepresentation, dateParts, datePartsIndex, dateLen, "YMD");
            organizeParts(lexicalRepresentation, timeParts, timePartsIndex, timeLen, "HMS");
            this.years = parseBigInteger(lexicalRepresentation, dateParts[0], datePartsIndex[0]);
            this.months = parseBigInteger(lexicalRepresentation, dateParts[1], datePartsIndex[1]);
            this.days = parseBigInteger(lexicalRepresentation, dateParts[2], datePartsIndex[2]);
            this.hours = parseBigInteger(lexicalRepresentation, timeParts[0], timePartsIndex[0]);
            this.minutes = parseBigInteger(lexicalRepresentation, timeParts[1], timePartsIndex[1]);
            this.seconds = parseBigDecimal(lexicalRepresentation, timeParts[2], timePartsIndex[2]);
            this.signum = calcSignum(positive);
            return;
        }
        throw new IllegalArgumentException(lexicalRepresentation);
    }

    private static boolean isDigit(char ch) {
        return '0' <= ch && ch <= '9';
    }

    private static boolean isDigitOrPeriod(char ch) {
        return isDigit(ch) || ch == '.';
    }

    private static String parsePiece(String whole, int[] idx) throws IllegalArgumentException {
        int start = idx[0];
        while (idx[0] < whole.length() && isDigitOrPeriod(whole.charAt(idx[0]))) {
            idx[0] = idx[0] + 1;
        }
        if (idx[0] != whole.length()) {
            idx[0] = idx[0] + 1;
            return whole.substring(start, idx[0]);
        }
        throw new IllegalArgumentException(whole);
    }

    private static void organizeParts(String whole, String[] parts, int[] partsIndex, int len, String tokens) throws IllegalArgumentException {
        int idx = tokens.length();
        for (int i = len - 1; i >= 0; i--) {
            if (parts[i] == null) {
                throw new IllegalArgumentException(whole);
            }
            int nidx = tokens.lastIndexOf(parts[i].charAt(parts[i].length() - 1), idx - 1);
            if (nidx == -1) {
                throw new IllegalArgumentException(whole);
            }
            for (int j = nidx + 1; j < idx; j++) {
                parts[j] = null;
            }
            idx = nidx;
            parts[idx] = parts[i];
            partsIndex[idx] = partsIndex[i];
        }
        for (int idx2 = idx - 1; idx2 >= 0; idx2--) {
            parts[idx2] = null;
        }
    }

    private static BigInteger parseBigInteger(String whole, String part, int index) throws IllegalArgumentException {
        if (part == null) {
            return null;
        }
        return new BigInteger(part.substring(0, part.length() - 1));
    }

    private static BigDecimal parseBigDecimal(String whole, String part, int index) throws IllegalArgumentException {
        if (part == null) {
            return null;
        }
        return new BigDecimal(part.substring(0, part.length() - 1));
    }

    @Override
    public int compare(Duration rhs) {
        BigInteger maxintAsBigInteger = BigInteger.valueOf(2147483647L);
        if (this.years != null && this.years.compareTo(maxintAsBigInteger) == 1) {
            throw new UnsupportedOperationException(DatatypeMessageFormatter.formatMessage(null, "TooLarge", new Object[]{String.valueOf(getClass().getName()) + "#compare(Duration duration)" + DatatypeConstants.YEARS.toString(), this.years.toString()}));
        }
        if (this.months != null && this.months.compareTo(maxintAsBigInteger) == 1) {
            throw new UnsupportedOperationException(DatatypeMessageFormatter.formatMessage(null, "TooLarge", new Object[]{String.valueOf(getClass().getName()) + "#compare(Duration duration)" + DatatypeConstants.MONTHS.toString(), this.months.toString()}));
        }
        if (this.days != null && this.days.compareTo(maxintAsBigInteger) == 1) {
            throw new UnsupportedOperationException(DatatypeMessageFormatter.formatMessage(null, "TooLarge", new Object[]{String.valueOf(getClass().getName()) + "#compare(Duration duration)" + DatatypeConstants.DAYS.toString(), this.days.toString()}));
        }
        if (this.hours != null && this.hours.compareTo(maxintAsBigInteger) == 1) {
            throw new UnsupportedOperationException(DatatypeMessageFormatter.formatMessage(null, "TooLarge", new Object[]{String.valueOf(getClass().getName()) + "#compare(Duration duration)" + DatatypeConstants.HOURS.toString(), this.hours.toString()}));
        }
        if (this.minutes != null && this.minutes.compareTo(maxintAsBigInteger) == 1) {
            throw new UnsupportedOperationException(DatatypeMessageFormatter.formatMessage(null, "TooLarge", new Object[]{String.valueOf(getClass().getName()) + "#compare(Duration duration)" + DatatypeConstants.MINUTES.toString(), this.minutes.toString()}));
        }
        if (this.seconds == null || this.seconds.toBigInteger().compareTo(maxintAsBigInteger) != 1) {
            BigInteger rhsYears = (BigInteger) rhs.getField(DatatypeConstants.YEARS);
            if (rhsYears == null || rhsYears.compareTo(maxintAsBigInteger) != 1) {
                BigInteger rhsMonths = (BigInteger) rhs.getField(DatatypeConstants.MONTHS);
                if (rhsMonths == null || rhsMonths.compareTo(maxintAsBigInteger) != 1) {
                    BigInteger rhsDays = (BigInteger) rhs.getField(DatatypeConstants.DAYS);
                    if (rhsDays == null || rhsDays.compareTo(maxintAsBigInteger) != 1) {
                        BigInteger rhsHours = (BigInteger) rhs.getField(DatatypeConstants.HOURS);
                        if (rhsHours == null || rhsHours.compareTo(maxintAsBigInteger) != 1) {
                            BigInteger rhsMinutes = (BigInteger) rhs.getField(DatatypeConstants.MINUTES);
                            if (rhsMinutes == null || rhsMinutes.compareTo(maxintAsBigInteger) != 1) {
                                BigDecimal rhsSecondsAsBigDecimal = (BigDecimal) rhs.getField(DatatypeConstants.SECONDS);
                                BigInteger rhsSeconds = null;
                                if (rhsSecondsAsBigDecimal != null) {
                                    rhsSeconds = rhsSecondsAsBigDecimal.toBigInteger();
                                }
                                if (rhsSeconds != null && rhsSeconds.compareTo(maxintAsBigInteger) == 1) {
                                    throw new UnsupportedOperationException(DatatypeMessageFormatter.formatMessage(null, "TooLarge", new Object[]{String.valueOf(getClass().getName()) + "#compare(Duration duration)" + DatatypeConstants.SECONDS.toString(), rhsSeconds.toString()}));
                                }
                                GregorianCalendar lhsCalendar = new GregorianCalendar(1970, 1, 1, 0, 0, 0);
                                lhsCalendar.add(1, getYears() * getSign());
                                lhsCalendar.add(2, getMonths() * getSign());
                                lhsCalendar.add(6, getDays() * getSign());
                                lhsCalendar.add(11, getHours() * getSign());
                                lhsCalendar.add(12, getMinutes() * getSign());
                                lhsCalendar.add(13, getSeconds() * getSign());
                                GregorianCalendar rhsCalendar = new GregorianCalendar(1970, 1, 1, 0, 0, 0);
                                rhsCalendar.add(1, rhs.getYears() * rhs.getSign());
                                rhsCalendar.add(2, rhs.getMonths() * rhs.getSign());
                                rhsCalendar.add(6, rhs.getDays() * rhs.getSign());
                                rhsCalendar.add(11, rhs.getHours() * rhs.getSign());
                                rhsCalendar.add(12, rhs.getMinutes() * rhs.getSign());
                                rhsCalendar.add(13, rhs.getSeconds() * rhs.getSign());
                                if (lhsCalendar.equals(rhsCalendar)) {
                                    return 0;
                                }
                                return compareDates(this, rhs);
                            }
                            throw new UnsupportedOperationException(DatatypeMessageFormatter.formatMessage(null, "TooLarge", new Object[]{String.valueOf(getClass().getName()) + "#compare(Duration duration)" + DatatypeConstants.MINUTES.toString(), rhsMinutes.toString()}));
                        }
                        throw new UnsupportedOperationException(DatatypeMessageFormatter.formatMessage(null, "TooLarge", new Object[]{String.valueOf(getClass().getName()) + "#compare(Duration duration)" + DatatypeConstants.HOURS.toString(), rhsHours.toString()}));
                    }
                    throw new UnsupportedOperationException(DatatypeMessageFormatter.formatMessage(null, "TooLarge", new Object[]{String.valueOf(getClass().getName()) + "#compare(Duration duration)" + DatatypeConstants.DAYS.toString(), rhsDays.toString()}));
                }
                throw new UnsupportedOperationException(DatatypeMessageFormatter.formatMessage(null, "TooLarge", new Object[]{String.valueOf(getClass().getName()) + "#compare(Duration duration)" + DatatypeConstants.MONTHS.toString(), rhsMonths.toString()}));
            }
            throw new UnsupportedOperationException(DatatypeMessageFormatter.formatMessage(null, "TooLarge", new Object[]{String.valueOf(getClass().getName()) + "#compare(Duration duration)" + DatatypeConstants.YEARS.toString(), rhsYears.toString()}));
        }
        throw new UnsupportedOperationException(DatatypeMessageFormatter.formatMessage(null, "TooLarge", new Object[]{String.valueOf(getClass().getName()) + "#compare(Duration duration)" + DatatypeConstants.SECONDS.toString(), toString(this.seconds)}));
    }

    private int compareDates(Duration duration1, Duration duration2) {
        XMLGregorianCalendar tempA = (XMLGregorianCalendar) TEST_POINTS[0].clone();
        XMLGregorianCalendar tempB = (XMLGregorianCalendar) TEST_POINTS[0].clone();
        tempA.add(duration1);
        tempB.add(duration2);
        int resultA = tempA.compare(tempB);
        if (resultA == 2) {
            return 2;
        }
        XMLGregorianCalendar tempA2 = (XMLGregorianCalendar) TEST_POINTS[1].clone();
        XMLGregorianCalendar tempB2 = (XMLGregorianCalendar) TEST_POINTS[1].clone();
        tempA2.add(duration1);
        tempB2.add(duration2);
        int resultB = tempA2.compare(tempB2);
        int resultA2 = compareResults(resultA, resultB);
        if (resultA2 == 2) {
            return 2;
        }
        XMLGregorianCalendar tempA3 = (XMLGregorianCalendar) TEST_POINTS[2].clone();
        XMLGregorianCalendar tempB3 = (XMLGregorianCalendar) TEST_POINTS[2].clone();
        tempA3.add(duration1);
        tempB3.add(duration2);
        int resultB2 = tempA3.compare(tempB3);
        int resultA3 = compareResults(resultA2, resultB2);
        if (resultA3 == 2) {
            return 2;
        }
        XMLGregorianCalendar tempA4 = (XMLGregorianCalendar) TEST_POINTS[3].clone();
        XMLGregorianCalendar tempB4 = (XMLGregorianCalendar) TEST_POINTS[3].clone();
        tempA4.add(duration1);
        tempB4.add(duration2);
        int resultB3 = tempA4.compare(tempB4);
        return compareResults(resultA3, resultB3);
    }

    private int compareResults(int resultA, int resultB) {
        if (resultB == 2 || resultA != resultB) {
            return 2;
        }
        return resultA;
    }

    @Override
    public int hashCode() {
        Calendar cal = TEST_POINTS[0].toGregorianCalendar();
        addTo(cal);
        return (int) getCalendarTimeInMillis(cal);
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        if (this.signum < 0) {
            buf.append('-');
        }
        buf.append('P');
        if (this.years != null) {
            buf.append(this.years);
            buf.append('Y');
        }
        if (this.months != null) {
            buf.append(this.months);
            buf.append('M');
        }
        if (this.days != null) {
            buf.append(this.days);
            buf.append('D');
        }
        if (this.hours != null || this.minutes != null || this.seconds != null) {
            buf.append('T');
            if (this.hours != null) {
                buf.append(this.hours);
                buf.append('H');
            }
            if (this.minutes != null) {
                buf.append(this.minutes);
                buf.append('M');
            }
            if (this.seconds != null) {
                buf.append(toString(this.seconds));
                buf.append('S');
            }
        }
        return buf.toString();
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

    @Override
    public boolean isSet(DatatypeConstants.Field field) {
        if (field == null) {
            throw new NullPointerException(DatatypeMessageFormatter.formatMessage(null, "FieldCannotBeNull", new Object[]{"javax.xml.datatype.Duration#isSet(DatatypeConstants.Field field)"}));
        }
        if (field == DatatypeConstants.YEARS) {
            return this.years != null;
        }
        if (field == DatatypeConstants.MONTHS) {
            return this.months != null;
        }
        if (field == DatatypeConstants.DAYS) {
            return this.days != null;
        }
        if (field == DatatypeConstants.HOURS) {
            return this.hours != null;
        }
        if (field == DatatypeConstants.MINUTES) {
            return this.minutes != null;
        }
        if (field == DatatypeConstants.SECONDS) {
            return this.seconds != null;
        }
        throw new IllegalArgumentException(DatatypeMessageFormatter.formatMessage(null, "UnknownField", new Object[]{"javax.xml.datatype.Duration#isSet(DatatypeConstants.Field field)", field.toString()}));
    }

    @Override
    public Number getField(DatatypeConstants.Field field) {
        if (field == null) {
            throw new NullPointerException(DatatypeMessageFormatter.formatMessage(null, "FieldCannotBeNull", new Object[]{"javax.xml.datatype.Duration#isSet(DatatypeConstants.Field field) "}));
        }
        if (field == DatatypeConstants.YEARS) {
            return this.years;
        }
        if (field == DatatypeConstants.MONTHS) {
            return this.months;
        }
        if (field == DatatypeConstants.DAYS) {
            return this.days;
        }
        if (field == DatatypeConstants.HOURS) {
            return this.hours;
        }
        if (field == DatatypeConstants.MINUTES) {
            return this.minutes;
        }
        if (field == DatatypeConstants.SECONDS) {
            return this.seconds;
        }
        throw new IllegalArgumentException(DatatypeMessageFormatter.formatMessage(null, "UnknownField", new Object[]{"javax.xml.datatype.Duration#(getSet(DatatypeConstants.Field field)", field.toString()}));
    }

    @Override
    public int getYears() {
        return getInt(DatatypeConstants.YEARS);
    }

    @Override
    public int getMonths() {
        return getInt(DatatypeConstants.MONTHS);
    }

    @Override
    public int getDays() {
        return getInt(DatatypeConstants.DAYS);
    }

    @Override
    public int getHours() {
        return getInt(DatatypeConstants.HOURS);
    }

    @Override
    public int getMinutes() {
        return getInt(DatatypeConstants.MINUTES);
    }

    @Override
    public int getSeconds() {
        return getInt(DatatypeConstants.SECONDS);
    }

    private int getInt(DatatypeConstants.Field field) {
        Number n = getField(field);
        if (n == null) {
            return 0;
        }
        return n.intValue();
    }

    @Override
    public long getTimeInMillis(Calendar startInstant) {
        Calendar cal = (Calendar) startInstant.clone();
        addTo(cal);
        return getCalendarTimeInMillis(cal) - getCalendarTimeInMillis(startInstant);
    }

    @Override
    public long getTimeInMillis(Date startInstant) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(startInstant);
        addTo(cal);
        return getCalendarTimeInMillis(cal) - startInstant.getTime();
    }

    @Override
    public Duration normalizeWith(Calendar startTimeInstant) {
        Calendar c = (Calendar) startTimeInstant.clone();
        c.add(1, getYears() * this.signum);
        c.add(2, getMonths() * this.signum);
        c.add(5, getDays() * this.signum);
        long diff = getCalendarTimeInMillis(c) - getCalendarTimeInMillis(startTimeInstant);
        int days = (int) (diff / 86400000);
        return new DurationImpl(days >= 0, (BigInteger) null, (BigInteger) null, wrap(Math.abs(days)), (BigInteger) getField(DatatypeConstants.HOURS), (BigInteger) getField(DatatypeConstants.MINUTES), (BigDecimal) getField(DatatypeConstants.SECONDS));
    }

    @Override
    public Duration multiply(int factor) {
        return multiply(BigDecimal.valueOf(factor));
    }

    @Override
    public Duration multiply(BigDecimal factor) {
        BigDecimal carry = ZERO;
        int factorSign = factor.signum();
        BigDecimal factor2 = factor.abs();
        BigDecimal[] buf = new BigDecimal[6];
        int i = 0;
        while (true) {
            if (i < 5) {
                BigDecimal bd = getFieldAsBigDecimal(FIELDS[i]).multiply(factor2).add(carry);
                buf[i] = bd.setScale(0, 1);
                BigDecimal bd2 = bd.subtract(buf[i]);
                if (i == 1) {
                    if (bd2.signum() != 0) {
                        throw new IllegalStateException();
                    }
                    carry = ZERO;
                } else {
                    carry = bd2.multiply(FACTORS[i]);
                }
                i++;
            } else {
                if (this.seconds != null) {
                    buf[5] = this.seconds.multiply(factor2).add(carry);
                } else {
                    buf[5] = carry;
                }
                return new DurationImpl(this.signum * factorSign >= 0, toBigInteger(buf[0], this.years == null), toBigInteger(buf[1], this.months == null), toBigInteger(buf[2], this.days == null), toBigInteger(buf[3], this.hours == null), toBigInteger(buf[4], this.minutes == null), (buf[5].signum() == 0 && this.seconds == null) ? null : buf[5]);
            }
        }
    }

    private BigDecimal getFieldAsBigDecimal(DatatypeConstants.Field f) {
        if (f == DatatypeConstants.SECONDS) {
            if (this.seconds != null) {
                return this.seconds;
            }
            return ZERO;
        }
        BigInteger bi = (BigInteger) getField(f);
        if (bi == null) {
            return ZERO;
        }
        return new BigDecimal(bi);
    }

    private static BigInteger toBigInteger(BigDecimal value, boolean canBeNull) {
        if (canBeNull && value.signum() == 0) {
            return null;
        }
        return value.unscaledValue();
    }

    @Override
    public Duration add(Duration rhs) {
        boolean z = false;
        BigDecimal[] buf = {sanitize((BigInteger) getField(DatatypeConstants.YEARS), getSign()).add(sanitize((BigInteger) rhs.getField(DatatypeConstants.YEARS), rhs.getSign())), sanitize((BigInteger) getField(DatatypeConstants.MONTHS), getSign()).add(sanitize((BigInteger) rhs.getField(DatatypeConstants.MONTHS), rhs.getSign())), sanitize((BigInteger) getField(DatatypeConstants.DAYS), getSign()).add(sanitize((BigInteger) rhs.getField(DatatypeConstants.DAYS), rhs.getSign())), sanitize((BigInteger) getField(DatatypeConstants.HOURS), getSign()).add(sanitize((BigInteger) rhs.getField(DatatypeConstants.HOURS), rhs.getSign())), sanitize((BigInteger) getField(DatatypeConstants.MINUTES), getSign()).add(sanitize((BigInteger) rhs.getField(DatatypeConstants.MINUTES), rhs.getSign())), sanitize((BigDecimal) getField(DatatypeConstants.SECONDS), getSign()).add(sanitize((BigDecimal) rhs.getField(DatatypeConstants.SECONDS), rhs.getSign()))};
        alignSigns(buf, 0, 2);
        alignSigns(buf, 2, 6);
        int s = 0;
        for (int i = 0; i < 6; i++) {
            if (buf[i].signum() * s < 0) {
                throw new IllegalStateException();
            }
            if (s == 0) {
                s = buf[i].signum();
            }
        }
        boolean z2 = s >= 0;
        BigInteger bigInteger = toBigInteger(sanitize(buf[0], s), getField(DatatypeConstants.YEARS) == null && rhs.getField(DatatypeConstants.YEARS) == null);
        BigInteger bigInteger2 = toBigInteger(sanitize(buf[1], s), getField(DatatypeConstants.MONTHS) == null && rhs.getField(DatatypeConstants.MONTHS) == null);
        BigInteger bigInteger3 = toBigInteger(sanitize(buf[2], s), getField(DatatypeConstants.DAYS) == null && rhs.getField(DatatypeConstants.DAYS) == null);
        BigInteger bigInteger4 = toBigInteger(sanitize(buf[3], s), getField(DatatypeConstants.HOURS) == null && rhs.getField(DatatypeConstants.HOURS) == null);
        BigDecimal bigDecimalSanitize = sanitize(buf[4], s);
        if (getField(DatatypeConstants.MINUTES) == null && rhs.getField(DatatypeConstants.MINUTES) == null) {
            z = true;
        }
        return new DurationImpl(z2, bigInteger, bigInteger2, bigInteger3, bigInteger4, toBigInteger(bigDecimalSanitize, z), (buf[5].signum() == 0 && getField(DatatypeConstants.SECONDS) == null && rhs.getField(DatatypeConstants.SECONDS) == null) ? null : sanitize(buf[5], s));
    }

    private static void alignSigns(BigDecimal[] buf, int start, int end) {
        boolean touched;
        do {
            touched = false;
            int s = 0;
            for (int i = start; i < end; i++) {
                if (buf[i].signum() * s < 0) {
                    touched = true;
                    BigDecimal borrow = buf[i].abs().divide(FACTORS[i - 1], 0);
                    if (buf[i].signum() > 0) {
                        borrow = borrow.negate();
                    }
                    buf[i - 1] = buf[i - 1].subtract(borrow);
                    buf[i] = buf[i].add(borrow.multiply(FACTORS[i - 1]));
                }
                if (buf[i].signum() != 0) {
                    s = buf[i].signum();
                }
            }
        } while (touched);
    }

    private static BigDecimal sanitize(BigInteger value, int signum) {
        if (signum == 0 || value == null) {
            return ZERO;
        }
        if (signum > 0) {
            return new BigDecimal(value);
        }
        return new BigDecimal(value.negate());
    }

    static BigDecimal sanitize(BigDecimal value, int signum) {
        if (signum == 0 || value == null) {
            return ZERO;
        }
        if (signum > 0) {
            return value;
        }
        return value.negate();
    }

    @Override
    public Duration subtract(Duration rhs) {
        return add(rhs.negate());
    }

    @Override
    public Duration negate() {
        return new DurationImpl(this.signum <= 0, this.years, this.months, this.days, this.hours, this.minutes, this.seconds);
    }

    public int signum() {
        return this.signum;
    }

    @Override
    public void addTo(Calendar calendar) {
        calendar.add(1, getYears() * this.signum);
        calendar.add(2, getMonths() * this.signum);
        calendar.add(5, getDays() * this.signum);
        calendar.add(10, getHours() * this.signum);
        calendar.add(12, getMinutes() * this.signum);
        calendar.add(13, getSeconds() * this.signum);
        if (this.seconds != null) {
            BigDecimal fraction = this.seconds.subtract(this.seconds.setScale(0, 1));
            int millisec = fraction.movePointRight(3).intValue();
            calendar.add(14, this.signum * millisec);
        }
    }

    @Override
    public void addTo(Date date) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);
        addTo(cal);
        date.setTime(getCalendarTimeInMillis(cal));
    }

    private static long getCalendarTimeInMillis(Calendar cal) {
        return cal.getTime().getTime();
    }

    private Object writeReplace() throws IOException {
        return new SerializedDuration(toString());
    }
}
