package java.util;

import dalvik.bytecode.Opcodes;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;

public class GregorianCalendar extends Calendar {
    public static final int AD = 1;
    public static final int BC = 0;
    private static final long defaultGregorianCutover = -12219292800000L;
    private static final long serialVersionUID = -8125100834729963327L;
    private transient int changeYear;
    private int currentYearSkew;
    private long gregorianCutover;
    private transient int julianSkew;
    private int lastYearSkew;
    static byte[] DaysInMonth = {31, Character.OTHER_SYMBOL, 31, Character.FINAL_QUOTE_PUNCTUATION, 31, Character.FINAL_QUOTE_PUNCTUATION, 31, 31, Character.FINAL_QUOTE_PUNCTUATION, 31, Character.FINAL_QUOTE_PUNCTUATION, 31};
    private static int[] DaysInYear = {0, 31, 59, 90, Opcodes.OP_INVOKE_INTERFACE_RANGE, Opcodes.OP_XOR_INT, Opcodes.OP_AND_INT_2ADDR, Opcodes.OP_REM_INT_LIT16, Opcodes.OP_IGET_WIDE_QUICK, 273, HttpURLConnection.HTTP_NOT_MODIFIED, 334};
    private static int[] maximums = {1, 292278994, 11, 53, 6, 31, 366, 7, 6, 1, 11, 23, 59, 59, 999, 50400000, 7200000};
    private static int[] minimums = {0, 1, 0, 1, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, -46800000, 0};
    private static int[] leastMaximums = {1, 292269054, 11, 50, 3, 28, 355, 7, 3, 1, 11, 23, 59, 59, 999, 50400000, 1200000};

    public GregorianCalendar() {
        this(TimeZone.getDefault(), Locale.getDefault());
    }

    public GregorianCalendar(int year, int month, int day) {
        super(TimeZone.getDefault(), Locale.getDefault());
        this.gregorianCutover = defaultGregorianCutover;
        this.changeYear = 1582;
        this.julianSkew = (((this.changeYear - 2000) / HttpURLConnection.HTTP_BAD_REQUEST) + julianError()) - ((this.changeYear - 2000) / 100);
        this.currentYearSkew = 10;
        this.lastYearSkew = 0;
        set(year, month, day);
    }

    public GregorianCalendar(int year, int month, int day, int hour, int minute) {
        super(TimeZone.getDefault(), Locale.getDefault());
        this.gregorianCutover = defaultGregorianCutover;
        this.changeYear = 1582;
        this.julianSkew = (((this.changeYear - 2000) / HttpURLConnection.HTTP_BAD_REQUEST) + julianError()) - ((this.changeYear - 2000) / 100);
        this.currentYearSkew = 10;
        this.lastYearSkew = 0;
        set(year, month, day, hour, minute);
    }

    public GregorianCalendar(int year, int month, int day, int hour, int minute, int second) {
        super(TimeZone.getDefault(), Locale.getDefault());
        this.gregorianCutover = defaultGregorianCutover;
        this.changeYear = 1582;
        this.julianSkew = (((this.changeYear - 2000) / HttpURLConnection.HTTP_BAD_REQUEST) + julianError()) - ((this.changeYear - 2000) / 100);
        this.currentYearSkew = 10;
        this.lastYearSkew = 0;
        set(year, month, day, hour, minute, second);
    }

    GregorianCalendar(long milliseconds) {
        this(false);
        setTimeInMillis(milliseconds);
    }

    public GregorianCalendar(Locale locale) {
        this(TimeZone.getDefault(), locale);
    }

    public GregorianCalendar(TimeZone timezone) {
        this(timezone, Locale.getDefault());
    }

    public GregorianCalendar(TimeZone timezone, Locale locale) {
        super(timezone, locale);
        this.gregorianCutover = defaultGregorianCutover;
        this.changeYear = 1582;
        this.julianSkew = (((this.changeYear - 2000) / HttpURLConnection.HTTP_BAD_REQUEST) + julianError()) - ((this.changeYear - 2000) / 100);
        this.currentYearSkew = 10;
        this.lastYearSkew = 0;
        setTimeInMillis(System.currentTimeMillis());
    }

    public GregorianCalendar(boolean ignored) {
        super(TimeZone.getDefault());
        this.gregorianCutover = defaultGregorianCutover;
        this.changeYear = 1582;
        this.julianSkew = (((this.changeYear - 2000) / HttpURLConnection.HTTP_BAD_REQUEST) + julianError()) - ((this.changeYear - 2000) / 100);
        this.currentYearSkew = 10;
        this.lastYearSkew = 0;
        setFirstDayOfWeek(1);
        setMinimalDaysInFirstWeek(1);
    }

    @Override
    public void add(int field, int value) {
        if (value != 0) {
            if (field < 0 || field >= 15) {
                throw new IllegalArgumentException();
            }
            if (field == 0) {
                complete();
                if (this.fields[0] == 1) {
                    if (value < 0) {
                        set(0, 0);
                    } else {
                        return;
                    }
                } else if (value > 0) {
                    set(0, 1);
                } else {
                    return;
                }
                complete();
                return;
            }
            if (field == 1 || field == 2) {
                complete();
                if (field == 2) {
                    int month = this.fields[2] + value;
                    if (month < 0) {
                        value = (month - 11) / 12;
                        month = (month % 12) + 12;
                    } else {
                        value = month / 12;
                    }
                    set(2, month % 12);
                }
                set(1, this.fields[1] + value);
                int days = daysInMonth(isLeapYear(this.fields[1]), this.fields[2]);
                if (this.fields[5] > days) {
                    set(5, days);
                }
                complete();
                return;
            }
            long multiplier = 0;
            getTimeInMillis();
            switch (field) {
                case 3:
                case 4:
                case 8:
                    multiplier = 604800000;
                    break;
                case 5:
                case 6:
                case 7:
                    multiplier = 86400000;
                    break;
                case 9:
                    multiplier = 43200000;
                    break;
                case 10:
                case 11:
                    this.time += ((long) value) * 3600000;
                    break;
                case 12:
                    this.time += ((long) value) * 60000;
                    break;
                case 13:
                    this.time += ((long) value) * 1000;
                    break;
                case 14:
                    this.time += (long) value;
                    break;
            }
            if (multiplier == 0) {
                this.areFieldsSet = false;
                complete();
                return;
            }
            long delta = ((long) value) * multiplier;
            int zoneOffset = getTimeZone().getRawOffset();
            int offsetBefore = getOffset(this.time + ((long) zoneOffset));
            int offsetAfter = getOffset(this.time + ((long) zoneOffset) + delta);
            int dstDelta = offsetBefore - offsetAfter;
            if (getOffset(this.time + ((long) zoneOffset) + delta + ((long) dstDelta)) == offsetAfter) {
                delta += (long) dstDelta;
            }
            this.time += delta;
            this.areFieldsSet = false;
            complete();
        }
    }

    private void fullFieldsCalc() {
        int millis = (int) (this.time % 86400000);
        long days = this.time / 86400000;
        if (millis < 0) {
            millis += Grego.MILLIS_PER_DAY;
            days--;
        }
        int millis2 = millis + this.fields[15];
        while (millis2 < 0) {
            millis2 += Grego.MILLIS_PER_DAY;
            days--;
        }
        while (millis2 >= 86400000) {
            millis2 -= Grego.MILLIS_PER_DAY;
            days++;
        }
        int dayOfYear = computeYearAndDay(days, this.time + ((long) this.fields[15]));
        this.fields[6] = dayOfYear;
        if (this.fields[1] == this.changeYear && this.gregorianCutover <= this.time + ((long) this.fields[15])) {
            dayOfYear += this.currentYearSkew;
        }
        int month = dayOfYear / 32;
        boolean leapYear = isLeapYear(this.fields[1]);
        int date = dayOfYear - daysInYear(leapYear, month);
        if (date > daysInMonth(leapYear, month)) {
            date -= daysInMonth(leapYear, month);
            month++;
        }
        this.fields[7] = mod7(days - 3) + 1;
        int dstOffset = this.fields[1] <= 0 ? 0 : getTimeZone().getOffset(1, this.fields[1], month, date, this.fields[7], millis2);
        if (this.fields[1] > 0) {
            dstOffset -= this.fields[15];
        }
        this.fields[16] = dstOffset;
        if (dstOffset != 0) {
            long oldDays = days;
            millis2 += dstOffset;
            if (millis2 < 0) {
                millis2 += Grego.MILLIS_PER_DAY;
                days--;
            } else if (millis2 >= 86400000) {
                millis2 -= Grego.MILLIS_PER_DAY;
                days++;
            }
            if (oldDays != days) {
                int dayOfYear2 = computeYearAndDay(days, (this.time - ((long) this.fields[15])) + ((long) dstOffset));
                this.fields[6] = dayOfYear2;
                if (this.fields[1] == this.changeYear && this.gregorianCutover <= (this.time - ((long) this.fields[15])) + ((long) dstOffset)) {
                    dayOfYear2 += this.currentYearSkew;
                }
                month = dayOfYear2 / 32;
                leapYear = isLeapYear(this.fields[1]);
                date = dayOfYear2 - daysInYear(leapYear, month);
                if (date > daysInMonth(leapYear, month)) {
                    date -= daysInMonth(leapYear, month);
                    month++;
                }
                this.fields[7] = mod7(days - 3) + 1;
            }
        }
        this.fields[14] = millis2 % 1000;
        int millis3 = millis2 / 1000;
        this.fields[13] = millis3 % 60;
        int millis4 = millis3 / 60;
        this.fields[12] = millis4 % 60;
        this.fields[11] = (millis4 / 60) % 24;
        this.fields[9] = this.fields[11] > 11 ? 1 : 0;
        this.fields[10] = this.fields[11] % 12;
        if (this.fields[1] <= 0) {
            this.fields[0] = 0;
            this.fields[1] = (-this.fields[1]) + 1;
        } else {
            this.fields[0] = 1;
        }
        this.fields[2] = month;
        this.fields[5] = date;
        this.fields[8] = ((date - 1) / 7) + 1;
        this.fields[4] = (((date - 1) + mod7(((days - ((long) date)) - 2) - ((long) (getFirstDayOfWeek() - 1)))) / 7) + 1;
        int daysFromStart = mod7(((days - 3) - ((long) (this.fields[6] - 1))) - ((long) (getFirstDayOfWeek() - 1)));
        int week = (((this.fields[6] - 1) + daysFromStart) / 7) + (7 - daysFromStart >= getMinimalDaysInFirstWeek() ? 1 : 0);
        if (week == 0) {
            this.fields[3] = 7 - mod7((long) (daysFromStart - (isLeapYear(this.fields[1] + (-1)) ? 2 : 1))) >= getMinimalDaysInFirstWeek() ? 53 : 52;
            return;
        }
        if (this.fields[6] >= (leapYear ? 367 : 366) - mod7((leapYear ? 2 : 1) + daysFromStart)) {
            int[] iArr = this.fields;
            if (7 - mod7((leapYear ? 2 : 1) + daysFromStart) >= getMinimalDaysInFirstWeek()) {
                week = 1;
            }
            iArr[3] = week;
            return;
        }
        this.fields[3] = week;
    }

    @Override
    protected void computeFields() {
        TimeZone timeZone = getTimeZone();
        int dstOffset = timeZone.inDaylightTime(new Date(this.time)) ? timeZone.getDSTSavings() : 0;
        int zoneOffset = timeZone.getRawOffset();
        this.fields[16] = dstOffset;
        this.fields[15] = zoneOffset;
        fullFieldsCalc();
        for (int i = 0; i < 17; i++) {
            this.isSet[i] = true;
        }
    }

    @Override
    protected void computeTime() {
        long days;
        int dayOfWeek;
        int dayOfWeek2;
        if (!isLenient()) {
            if (this.isSet[11]) {
                if (this.fields[11] < 0 || this.fields[11] > 23) {
                    throw new IllegalArgumentException();
                }
            } else if (this.isSet[10] && (this.fields[10] < 0 || this.fields[10] > 11)) {
                throw new IllegalArgumentException();
            }
            if (this.isSet[12] && (this.fields[12] < 0 || this.fields[12] > 59)) {
                throw new IllegalArgumentException();
            }
            if (this.isSet[13] && (this.fields[13] < 0 || this.fields[13] > 59)) {
                throw new IllegalArgumentException();
            }
            if (this.isSet[14] && (this.fields[14] < 0 || this.fields[14] > 999)) {
                throw new IllegalArgumentException();
            }
            if (this.isSet[3] && (this.fields[3] < 1 || this.fields[3] > 53)) {
                throw new IllegalArgumentException();
            }
            if (this.isSet[7] && (this.fields[7] < 1 || this.fields[7] > 7)) {
                throw new IllegalArgumentException();
            }
            if (this.isSet[8] && (this.fields[8] < 1 || this.fields[8] > 6)) {
                throw new IllegalArgumentException();
            }
            if (this.isSet[4] && (this.fields[4] < 1 || this.fields[4] > 6)) {
                throw new IllegalArgumentException();
            }
            if (this.isSet[9] && this.fields[9] != 0 && this.fields[9] != 1) {
                throw new IllegalArgumentException();
            }
            if (this.isSet[10] && (this.fields[10] < 0 || this.fields[10] > 11)) {
                throw new IllegalArgumentException();
            }
            if (this.isSet[1]) {
                if (this.isSet[0] && this.fields[0] == 0 && (this.fields[1] < 1 || this.fields[1] > 292269054)) {
                    throw new IllegalArgumentException();
                }
                if (this.fields[1] < 1 || this.fields[1] > 292278994) {
                    throw new IllegalArgumentException();
                }
            }
            if (this.isSet[2] && (this.fields[2] < 0 || this.fields[2] > 11)) {
                throw new IllegalArgumentException();
            }
        }
        long hour = 0;
        if (this.isSet[11] && this.lastTimeFieldSet != 10) {
            hour = this.fields[11];
        } else if (this.isSet[10]) {
            hour = (this.fields[9] * 12) + this.fields[10];
        }
        long timeVal = hour * 3600000;
        if (this.isSet[12]) {
            timeVal += ((long) this.fields[12]) * 60000;
        }
        if (this.isSet[13]) {
            timeVal += ((long) this.fields[13]) * 1000;
        }
        if (this.isSet[14]) {
            timeVal += (long) this.fields[14];
        }
        int year = this.isSet[1] ? this.fields[1] : 1970;
        if (this.isSet[0]) {
            if (this.fields[0] != 0 && this.fields[0] != 1) {
                throw new IllegalArgumentException();
            }
            if (this.fields[0] == 0) {
                year = 1 - year;
            }
        }
        boolean weekMonthSet = this.isSet[4] || this.isSet[8];
        boolean useMonth = (this.isSet[5] || this.isSet[2] || weekMonthSet) && this.lastDateFieldSet != 6;
        if (useMonth && (this.lastDateFieldSet == 7 || this.lastDateFieldSet == 3)) {
            if (this.isSet[3] && this.isSet[7]) {
                if (this.lastDateFieldSet == 3) {
                    useMonth = false;
                } else if (this.lastDateFieldSet == 7) {
                    useMonth = weekMonthSet;
                }
            } else if (this.isSet[6]) {
                useMonth = this.isSet[5] && this.isSet[2];
            }
        }
        if (useMonth) {
            int month = this.fields[2];
            year += month / 12;
            int month2 = month % 12;
            if (month2 < 0) {
                year--;
                month2 += 12;
            }
            boolean leapYear = isLeapYear(year);
            days = daysFromBaseYear(year) + ((long) daysInYear(leapYear, month2));
            boolean useDate = this.isSet[5];
            if (useDate && (this.lastDateFieldSet == 7 || this.lastDateFieldSet == 4 || this.lastDateFieldSet == 8)) {
                useDate = (this.isSet[7] && weekMonthSet) ? false : true;
            }
            if (useDate) {
                if (!isLenient() && (this.fields[5] < 1 || this.fields[5] > daysInMonth(leapYear, month2))) {
                    throw new IllegalArgumentException();
                }
                days += (long) (this.fields[5] - 1);
            } else {
                if (this.isSet[7]) {
                    dayOfWeek2 = this.fields[7] - 1;
                } else {
                    dayOfWeek2 = getFirstDayOfWeek() - 1;
                }
                if (this.isSet[4] && this.lastDateFieldSet != 8) {
                    int skew = mod7((days - 3) - ((long) (getFirstDayOfWeek() - 1)));
                    days += (long) ((((this.fields[4] - 1) * 7) + mod7(((long) (skew + dayOfWeek2)) - (days - 3))) - skew);
                } else if (this.isSet[8]) {
                    if (this.fields[8] >= 0) {
                        days += (long) (mod7(((long) dayOfWeek2) - (days - 3)) + ((this.fields[8] - 1) * 7));
                    } else {
                        days += (long) (daysInMonth(leapYear, month2) + mod7(((long) dayOfWeek2) - ((((long) daysInMonth(leapYear, month2)) + days) - 3)) + (this.fields[8] * 7));
                    }
                } else if (this.isSet[7]) {
                    int skew2 = mod7((days - 3) - ((long) (getFirstDayOfWeek() - 1)));
                    days += (long) mod7(mod7(((long) (skew2 + dayOfWeek2)) - (days - 3)) - skew2);
                }
            }
        } else {
            boolean useWeekYear = this.isSet[3] && this.lastDateFieldSet != 6;
            if (useWeekYear && this.isSet[6]) {
                useWeekYear = this.isSet[7];
            }
            days = daysFromBaseYear(year);
            if (useWeekYear) {
                if (this.isSet[7]) {
                    dayOfWeek = this.fields[7] - 1;
                } else {
                    dayOfWeek = getFirstDayOfWeek() - 1;
                }
                int skew3 = mod7((days - 3) - ((long) (getFirstDayOfWeek() - 1)));
                days += (long) ((((this.fields[3] - 1) * 7) + mod7(((long) (skew3 + dayOfWeek)) - (days - 3))) - skew3);
                if (7 - skew3 < getMinimalDaysInFirstWeek()) {
                    days += 7;
                }
            } else if (this.isSet[6]) {
                if (!isLenient()) {
                    if (this.fields[6] >= 1) {
                    }
                    throw new IllegalArgumentException();
                }
                days += (long) (this.fields[6] - 1);
            } else if (this.isSet[7]) {
                days += (long) mod7(((long) (this.fields[7] - 1)) - (days - 3));
            }
        }
        this.lastDateFieldSet = 0;
        long timeVal2 = timeVal + (86400000 * days);
        if (year == this.changeYear && timeVal2 >= this.gregorianCutover + (((long) julianError()) * 86400000)) {
            timeVal2 -= ((long) julianError()) * 86400000;
        }
        long timeValWithoutDST = (timeVal2 - ((long) getOffset(timeVal2))) + ((long) getTimeZone().getRawOffset());
        long timeVal3 = timeVal2 - ((long) getOffset(timeValWithoutDST));
        this.time = timeVal3;
        if (timeValWithoutDST != timeVal3) {
            computeFields();
            this.areFieldsSet = true;
        }
    }

    private int computeYearAndDay(long dayCount, long localTime) {
        int year = 1970;
        long days = dayCount;
        if (localTime < this.gregorianCutover) {
            days -= (long) this.julianSkew;
        }
        while (true) {
            int approxYears = (int) (days / 365);
            if (approxYears == 0) {
                break;
            }
            year += approxYears;
            days = dayCount - daysFromBaseYear(year);
        }
        if (days < 0) {
            year--;
            days += (long) daysInYear(year);
        }
        this.fields[1] = year;
        return ((int) days) + 1;
    }

    private long daysFromBaseYear(long year) {
        if (year < 1970) {
            if (year <= this.changeYear) {
                long days = ((year - 1970) * 365) + ((year - 1972) / 4) + ((long) this.julianSkew);
                return days;
            }
            long days2 = ((((year - 1970) * 365) + ((year - 1972) / 4)) - ((year - 2000) / 100)) + ((year - 2000) / 400);
            return days2;
        }
        long days3 = ((year - 1970) * 365) + ((year - 1969) / 4);
        if (year > this.changeYear) {
            return days3 - (((year - 1901) / 100) - ((year - 1601) / 400));
        }
        if (year == this.changeYear) {
            return days3 + ((long) this.currentYearSkew);
        }
        if (year == this.changeYear - 1) {
            return days3 + ((long) this.lastYearSkew);
        }
        return days3 + ((long) this.julianSkew);
    }

    private int daysInMonth() {
        return daysInMonth(isLeapYear(this.fields[1]), this.fields[2]);
    }

    private int daysInMonth(boolean leapYear, int month) {
        return (leapYear && month == 1) ? DaysInMonth[month] + 1 : DaysInMonth[month];
    }

    private int daysInYear(int year) {
        int daysInYear = isLeapYear(year) ? 366 : 365;
        if (year == this.changeYear) {
            daysInYear -= this.currentYearSkew;
        }
        if (year == this.changeYear - 1) {
            return daysInYear - this.lastYearSkew;
        }
        return daysInYear;
    }

    private int daysInYear(boolean leapYear, int month) {
        return (!leapYear || month <= 1) ? DaysInYear[month] : DaysInYear[month] + 1;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof GregorianCalendar)) {
            return false;
        }
        if (object != this) {
            return super.equals(object) && this.gregorianCutover == ((GregorianCalendar) object).gregorianCutover;
        }
        return true;
    }

    @Override
    public int getActualMaximum(int field) {
        int value = maximums[field];
        if (value != leastMaximums[field]) {
            complete();
            long orgTime = this.time;
            int result = 0;
            switch (field) {
                case 1:
                    GregorianCalendar clone = (GregorianCalendar) clone();
                    if (get(0) == 1) {
                        clone.setTimeInMillis(Long.MAX_VALUE);
                    } else {
                        clone.setTimeInMillis(Long.MIN_VALUE);
                    }
                    result = clone.get(1);
                    clone.set(1, get(1));
                    if (clone.before(this)) {
                        result--;
                    }
                    break;
                case 3:
                    set(5, 31);
                    set(2, 11);
                    result = get(3);
                    if (result == 1) {
                        set(5, 24);
                        result = get(3);
                    }
                    this.areFieldsSet = false;
                    break;
                case 4:
                    set(5, daysInMonth());
                    result = get(4);
                    this.areFieldsSet = false;
                    break;
                case 5:
                    return daysInMonth();
                case 6:
                    return daysInYear(this.fields[1]);
                case 8:
                    result = get(8) + ((daysInMonth() - get(5)) / 7);
                    break;
                case 16:
                    result = getMaximum(16);
                    break;
            }
            this.time = orgTime;
            return result;
        }
        return value;
    }

    @Override
    public int getActualMinimum(int field) {
        return getMinimum(field);
    }

    @Override
    public int getGreatestMinimum(int field) {
        return minimums[field];
    }

    public final Date getGregorianChange() {
        return new Date(this.gregorianCutover);
    }

    @Override
    public int getLeastMaximum(int field) {
        if (this.gregorianCutover != defaultGregorianCutover && field == 3) {
            long currentTimeInMillis = this.time;
            setTimeInMillis(this.gregorianCutover);
            int actual = getActualMaximum(field);
            setTimeInMillis(currentTimeInMillis);
            return actual;
        }
        int actual2 = leastMaximums[field];
        return actual2;
    }

    @Override
    public int getMaximum(int field) {
        return maximums[field];
    }

    @Override
    public int getMinimum(int field) {
        return minimums[field];
    }

    private int getOffset(long localTime) {
        TimeZone timeZone = getTimeZone();
        long dayCount = localTime / 86400000;
        int millis = (int) (localTime % 86400000);
        if (millis < 0) {
            millis += Grego.MILLIS_PER_DAY;
            dayCount--;
        }
        int year = 1970;
        long days = dayCount;
        if (localTime < this.gregorianCutover) {
            days -= (long) this.julianSkew;
        }
        while (true) {
            int approxYears = (int) (days / 365);
            if (approxYears == 0) {
                break;
            }
            year += approxYears;
            days = dayCount - daysFromBaseYear(year);
        }
        if (days < 0) {
            year--;
            days = 365 + days + ((long) (isLeapYear(year) ? 1 : 0));
            if (year == this.changeYear && localTime < this.gregorianCutover) {
                days -= (long) julianError();
            }
        }
        if (year <= 0) {
            return timeZone.getRawOffset();
        }
        int dayOfYear = ((int) days) + 1;
        int month = dayOfYear / 32;
        boolean leapYear = isLeapYear(year);
        int date = dayOfYear - daysInYear(leapYear, month);
        if (date > daysInMonth(leapYear, month)) {
            date -= daysInMonth(leapYear, month);
            month++;
        }
        int dayOfWeek = mod7(dayCount - 3) + 1;
        return timeZone.getOffset(1, year, month, date, dayOfWeek, millis);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + (((int) (this.gregorianCutover >>> 32)) ^ ((int) this.gregorianCutover));
    }

    public boolean isLeapYear(int year) {
        return year > this.changeYear ? year % 4 == 0 && (year % 100 != 0 || year % HttpURLConnection.HTTP_BAD_REQUEST == 0) : year % 4 == 0;
    }

    private int julianError() {
        return ((this.changeYear / 100) - (this.changeYear / HttpURLConnection.HTTP_BAD_REQUEST)) - 2;
    }

    private int mod(int value, int mod) {
        int rem = value % mod;
        if (value < 0 && rem < 0) {
            return rem + mod;
        }
        return rem;
    }

    private int mod7(long num1) {
        int rem = (int) (num1 % 7);
        if (num1 < 0 && rem < 0) {
            return rem + 7;
        }
        return rem;
    }

    @Override
    public void roll(int field, int value) {
        if (value != 0) {
            if (field < 0 || field >= 15) {
                throw new IllegalArgumentException();
            }
            complete();
            int max = -1;
            switch (field) {
                case 0:
                case 2:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                    set(field, mod(this.fields[field] + value, maximums[field] + 1));
                    if (field == 2 && this.fields[5] > daysInMonth()) {
                        set(5, daysInMonth());
                    } else if (field == 9) {
                        this.lastTimeFieldSet = 10;
                    }
                    break;
                case 1:
                    max = maximums[field];
                    break;
                case 3:
                    int days = daysInYear(this.fields[1]);
                    int mod = mod7((this.fields[7] - this.fields[6]) - (getFirstDayOfWeek() - 1));
                    int maxWeeks = (((days - 1) + mod) / 7) + 1;
                    int newWeek = mod((this.fields[field] - 1) + value, maxWeeks) + 1;
                    if (newWeek == maxWeeks) {
                        int addDays = (newWeek - this.fields[field]) * 7;
                        if (this.fields[6] > addDays && this.fields[6] + addDays > days) {
                            set(field, 1);
                        } else {
                            set(field, newWeek - 1);
                        }
                    } else if (newWeek == 1) {
                        int week = ((((this.fields[6] - (((this.fields[6] - 1) / 7) * 7)) - 1) + mod) / 7) + 1;
                        if (week > 1) {
                            set(field, 1);
                        } else {
                            set(field, newWeek);
                        }
                    } else {
                        set(field, newWeek);
                    }
                    break;
                case 4:
                    int days2 = daysInMonth();
                    int mod2 = mod7((this.fields[7] - this.fields[5]) - (getFirstDayOfWeek() - 1));
                    int maxWeeks2 = (((days2 - 1) + mod2) / 7) + 1;
                    int newWeek2 = mod((this.fields[field] - 1) + value, maxWeeks2) + 1;
                    if (newWeek2 == maxWeeks2) {
                        if (this.fields[5] + ((newWeek2 - this.fields[field]) * 7) > days2) {
                            set(5, days2);
                        } else {
                            set(field, newWeek2);
                        }
                    } else if (newWeek2 == 1) {
                        int week2 = ((((this.fields[5] - (((this.fields[5] - 1) / 7) * 7)) - 1) + mod2) / 7) + 1;
                        if (week2 > 1) {
                            set(5, 1);
                        } else {
                            set(field, newWeek2);
                        }
                    } else {
                        set(field, newWeek2);
                    }
                    break;
                case 5:
                    max = daysInMonth();
                    break;
                case 6:
                    max = daysInYear(this.fields[1]);
                    break;
                case 7:
                    max = maximums[field];
                    this.lastDateFieldSet = 4;
                    break;
                case 8:
                    max = (((this.fields[5] + (((daysInMonth() - this.fields[5]) / 7) * 7)) - 1) / 7) + 1;
                    break;
            }
            if (max != -1) {
                set(field, mod((this.fields[field] - 1) + value, max) + 1);
            }
            complete();
        }
    }

    @Override
    public void roll(int field, boolean increment) {
        roll(field, increment ? 1 : -1);
    }

    public void setGregorianChange(Date date) {
        this.gregorianCutover = date.getTime();
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.setTime(date);
        this.changeYear = cal.get(1);
        if (cal.get(0) == 0) {
            this.changeYear = 1 - this.changeYear;
        }
        this.julianSkew = (((this.changeYear - 2000) / HttpURLConnection.HTTP_BAD_REQUEST) + julianError()) - ((this.changeYear - 2000) / 100);
        int dayOfYear = cal.get(6);
        if (dayOfYear < this.julianSkew) {
            this.currentYearSkew = dayOfYear - 1;
            this.lastYearSkew = (this.julianSkew - dayOfYear) + 1;
        } else {
            this.lastYearSkew = 0;
            this.currentYearSkew = this.julianSkew;
        }
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        setGregorianChange(new Date(this.gregorianCutover));
    }
}
