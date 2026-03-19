package sun.util.calendar;

import java.net.HttpURLConnection;
import java.sql.Types;
import java.util.TimeZone;

public abstract class BaseCalendar extends AbstractCalendar {
    static final boolean $assertionsDisabled = false;
    public static final int APRIL = 4;
    public static final int AUGUST = 8;
    private static final int BASE_YEAR = 1970;
    public static final int DECEMBER = 12;
    public static final int FEBRUARY = 2;
    public static final int FRIDAY = 6;
    public static final int JANUARY = 1;
    public static final int JULY = 7;
    public static final int JUNE = 6;
    public static final int MARCH = 3;
    public static final int MAY = 5;
    public static final int MONDAY = 2;
    public static final int NOVEMBER = 11;
    public static final int OCTOBER = 10;
    public static final int SATURDAY = 7;
    public static final int SEPTEMBER = 9;
    public static final int SUNDAY = 1;
    public static final int THURSDAY = 5;
    public static final int TUESDAY = 3;
    public static final int WEDNESDAY = 4;
    private static final int[] FIXED_DATES = {719163, 719528, 719893, 720259, 720624, 720989, 721354, 721720, 722085, 722450, 722815, 723181, 723546, 723911, 724276, 724642, 725007, 725372, 725737, 726103, 726468, 726833, 727198, 727564, 727929, 728294, 728659, 729025, 729390, 729755, 730120, 730486, 730851, 731216, 731581, 731947, 732312, 732677, 733042, 733408, 733773, 734138, 734503, 734869, 735234, 735599, 735964, 736330, 736695, 737060, 737425, 737791, 738156, 738521, 738886, 739252, 739617, 739982, 740347, 740713, 741078, 741443, 741808, 742174, 742539, 742904, 743269, 743635, 744000, 744365};
    static final int[] DAYS_IN_MONTH = {31, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    static final int[] ACCUMULATED_DAYS_IN_MONTH = {-30, 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, HttpURLConnection.HTTP_NOT_MODIFIED, 334};
    static final int[] ACCUMULATED_DAYS_IN_MONTH_LEAP = {-30, 0, 31, 60, 91, 121, 152, 182, 213, 244, 274, HttpURLConnection.HTTP_USE_PROXY, 335};

    public static abstract class Date extends CalendarDate {
        long cachedFixedDateJan1;
        long cachedFixedDateNextJan1;
        int cachedYear;

        public abstract int getNormalizedYear();

        public abstract void setNormalizedYear(int i);

        protected Date() {
            this.cachedYear = Types.BLOB;
            this.cachedFixedDateJan1 = 731581L;
            this.cachedFixedDateNextJan1 = this.cachedFixedDateJan1 + 366;
        }

        protected Date(TimeZone timeZone) {
            super(timeZone);
            this.cachedYear = Types.BLOB;
            this.cachedFixedDateJan1 = 731581L;
            this.cachedFixedDateNextJan1 = this.cachedFixedDateJan1 + 366;
        }

        public Date setNormalizedDate(int i, int i2, int i3) {
            setNormalizedYear(i);
            setMonth(i2).setDayOfMonth(i3);
            return this;
        }

        protected final boolean hit(int i) {
            if (i == this.cachedYear) {
                return true;
            }
            return BaseCalendar.$assertionsDisabled;
        }

        protected final boolean hit(long j) {
            if (j < this.cachedFixedDateJan1 || j >= this.cachedFixedDateNextJan1) {
                return BaseCalendar.$assertionsDisabled;
            }
            return true;
        }

        protected int getCachedYear() {
            return this.cachedYear;
        }

        protected long getCachedJan1() {
            return this.cachedFixedDateJan1;
        }

        protected void setCache(int i, long j, int i2) {
            this.cachedYear = i;
            this.cachedFixedDateJan1 = j;
            this.cachedFixedDateNextJan1 = j + ((long) i2);
        }
    }

    @Override
    public boolean validate(CalendarDate calendarDate) {
        int dayOfMonth;
        Date date = (Date) calendarDate;
        if (date.isNormalized()) {
            return true;
        }
        int month = date.getMonth();
        if (month < 1 || month > 12 || (dayOfMonth = date.getDayOfMonth()) <= 0 || dayOfMonth > getMonthLength(date.getNormalizedYear(), month)) {
            return $assertionsDisabled;
        }
        int dayOfWeek = date.getDayOfWeek();
        if ((dayOfWeek != Integer.MIN_VALUE && dayOfWeek != getDayOfWeek(date)) || !validateTime(calendarDate)) {
            return $assertionsDisabled;
        }
        date.setNormalized(true);
        return true;
    }

    @Override
    public boolean normalize(CalendarDate calendarDate) {
        if (calendarDate.isNormalized()) {
            return true;
        }
        Date date = (Date) calendarDate;
        if (date.getZone() != null) {
            getTime(calendarDate);
            return true;
        }
        int iNormalizeTime = normalizeTime(date);
        normalizeMonth(date);
        long dayOfMonth = ((long) date.getDayOfMonth()) + ((long) iNormalizeTime);
        int month = date.getMonth();
        int normalizedYear = date.getNormalizedYear();
        int monthLength = getMonthLength(normalizedYear, month);
        if (dayOfMonth > 0 && dayOfMonth <= monthLength) {
            date.setDayOfWeek(getDayOfWeek(date));
        } else if (dayOfMonth <= 0 && dayOfMonth > -28) {
            int i = month - 1;
            date.setDayOfMonth((int) (dayOfMonth + ((long) getMonthLength(normalizedYear, i))));
            if (i == 0) {
                date.setNormalizedYear(normalizedYear - 1);
                i = 12;
            }
            date.setMonth(i);
        } else {
            long j = monthLength;
            if (dayOfMonth > j && dayOfMonth < monthLength + 28) {
                int i2 = month + 1;
                date.setDayOfMonth((int) (dayOfMonth - j));
                if (i2 > 12) {
                    date.setNormalizedYear(normalizedYear + 1);
                    i2 = 1;
                }
                date.setMonth(i2);
            } else {
                getCalendarDateFromFixedDate(date, (dayOfMonth + getFixedDate(normalizedYear, month, 1, date)) - 1);
            }
        }
        calendarDate.setLeapYear(isLeapYear(date.getNormalizedYear()));
        calendarDate.setZoneOffset(0);
        calendarDate.setDaylightSaving(0);
        date.setNormalized(true);
        return true;
    }

    void normalizeMonth(CalendarDate calendarDate) {
        Date date = (Date) calendarDate;
        int normalizedYear = date.getNormalizedYear();
        long month = date.getMonth();
        if (month <= 0) {
            long j = 1 - month;
            date.setNormalizedYear(normalizedYear - ((int) ((j / 12) + 1)));
            date.setMonth((int) (13 - (j % 12)));
            return;
        }
        if (month > 12) {
            long j2 = month - 1;
            date.setNormalizedYear(normalizedYear + ((int) (j2 / 12)));
            date.setMonth((int) ((j2 % 12) + 1));
        }
    }

    @Override
    public int getYearLength(CalendarDate calendarDate) {
        return isLeapYear(((Date) calendarDate).getNormalizedYear()) ? 366 : 365;
    }

    @Override
    public int getYearLengthInMonths(CalendarDate calendarDate) {
        return 12;
    }

    @Override
    public int getMonthLength(CalendarDate calendarDate) {
        Date date = (Date) calendarDate;
        int month = date.getMonth();
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Illegal month value: " + month);
        }
        return getMonthLength(date.getNormalizedYear(), month);
    }

    private int getMonthLength(int i, int i2) {
        int i3 = DAYS_IN_MONTH[i2];
        if (i2 == 2 && isLeapYear(i)) {
            return i3 + 1;
        }
        return i3;
    }

    public long getDayOfYear(CalendarDate calendarDate) {
        return getDayOfYear(((Date) calendarDate).getNormalizedYear(), calendarDate.getMonth(), calendarDate.getDayOfMonth());
    }

    final long getDayOfYear(int i, int i2, int i3) {
        return ((long) i3) + ((long) (isLeapYear(i) ? ACCUMULATED_DAYS_IN_MONTH_LEAP[i2] : ACCUMULATED_DAYS_IN_MONTH[i2]));
    }

    @Override
    public long getFixedDate(CalendarDate calendarDate) {
        if (!calendarDate.isNormalized()) {
            normalizeMonth(calendarDate);
        }
        Date date = (Date) calendarDate;
        return getFixedDate(date.getNormalizedYear(), calendarDate.getMonth(), calendarDate.getDayOfMonth(), date);
    }

    public long getFixedDate(int i, int i2, int i3, Date date) {
        long jFloorDivide;
        boolean z = (i2 == 1 && i3 == 1) ? true : $assertionsDisabled;
        if (date != null && date.hit(i)) {
            if (!z) {
                return (date.getCachedJan1() + getDayOfYear(i, i2, i3)) - 1;
            }
            return date.getCachedJan1();
        }
        int i4 = i - 1970;
        if (i4 < 0 || i4 >= FIXED_DATES.length) {
            long j = ((long) i) - 1;
            long j2 = i3;
            if (j >= 0) {
                jFloorDivide = j2 + (((365 * j) + (j / 4)) - (j / 100)) + (j / 400) + ((long) (((367 * i2) - 362) / 12));
            } else {
                jFloorDivide = j2 + (((365 * j) + CalendarUtils.floorDivide(j, 4L)) - CalendarUtils.floorDivide(j, 100L)) + CalendarUtils.floorDivide(j, 400L) + ((long) CalendarUtils.floorDivide((367 * i2) - 362, 12));
            }
            if (i2 > 2) {
                jFloorDivide -= isLeapYear(i) ? 1L : 2L;
            }
            if (date != null && z) {
                date.setCache(i, jFloorDivide, isLeapYear(i) ? 366 : 365);
            }
            return jFloorDivide;
        }
        long j3 = FIXED_DATES[i4];
        if (date != null) {
            date.setCache(i, j3, isLeapYear(i) ? 366 : 365);
        }
        return z ? j3 : (j3 + getDayOfYear(i, i2, i3)) - 1;
    }

    @Override
    public void getCalendarDateFromFixedDate(CalendarDate calendarDate, long j) {
        int gregorianYearFromFixedDate;
        long fixedDate;
        boolean zIsLeapYear;
        int iFloorDivide;
        Date date = (Date) calendarDate;
        if (date.hit(j)) {
            gregorianYearFromFixedDate = date.getCachedYear();
            fixedDate = date.getCachedJan1();
            zIsLeapYear = isLeapYear(gregorianYearFromFixedDate);
        } else {
            gregorianYearFromFixedDate = getGregorianYearFromFixedDate(j);
            fixedDate = getFixedDate(gregorianYearFromFixedDate, 1, 1, null);
            zIsLeapYear = isLeapYear(gregorianYearFromFixedDate);
            date.setCache(gregorianYearFromFixedDate, fixedDate, zIsLeapYear ? 366 : 365);
        }
        int i = (int) (j - fixedDate);
        long j2 = 31 + fixedDate + 28;
        if (zIsLeapYear) {
            j2++;
        }
        if (j >= j2) {
            i += zIsLeapYear ? 1 : 2;
        }
        int i2 = (12 * i) + 373;
        if (i2 > 0) {
            iFloorDivide = i2 / 367;
        } else {
            iFloorDivide = CalendarUtils.floorDivide(i2, 367);
        }
        long j3 = fixedDate + ((long) ACCUMULATED_DAYS_IN_MONTH[iFloorDivide]);
        if (zIsLeapYear && iFloorDivide >= 3) {
            j3++;
        }
        int i3 = ((int) (j - j3)) + 1;
        int dayOfWeekFromFixedDate = getDayOfWeekFromFixedDate(j);
        date.setNormalizedYear(gregorianYearFromFixedDate);
        date.setMonth(iFloorDivide);
        date.setDayOfMonth(i3);
        date.setDayOfWeek(dayOfWeekFromFixedDate);
        date.setLeapYear(zIsLeapYear);
        date.setNormalized(true);
    }

    public int getDayOfWeek(CalendarDate calendarDate) {
        return getDayOfWeekFromFixedDate(getFixedDate(calendarDate));
    }

    public static final int getDayOfWeekFromFixedDate(long j) {
        if (j >= 0) {
            return ((int) (j % 7)) + 1;
        }
        return ((int) CalendarUtils.mod(j, 7L)) + 1;
    }

    public int getYearFromFixedDate(long j) {
        return getGregorianYearFromFixedDate(j);
    }

    final int getGregorianYearFromFixedDate(long j) {
        int iFloorDivide;
        int iFloorDivide2;
        int iFloorDivide3;
        int iFloorDivide4;
        if (j > 0) {
            long j2 = j - 1;
            iFloorDivide = (int) (j2 / 146097);
            int i = (int) (j2 % 146097);
            iFloorDivide2 = i / 36524;
            int i2 = i % 36524;
            iFloorDivide3 = i2 / 1461;
            int i3 = i2 % 1461;
            iFloorDivide4 = i3 / 365;
            int i4 = i3 % 365;
        } else {
            long j3 = j - 1;
            iFloorDivide = (int) CalendarUtils.floorDivide(j3, 146097L);
            int iMod = (int) CalendarUtils.mod(j3, 146097L);
            iFloorDivide2 = CalendarUtils.floorDivide(iMod, 36524);
            int iMod2 = CalendarUtils.mod(iMod, 36524);
            iFloorDivide3 = CalendarUtils.floorDivide(iMod2, 1461);
            int iMod3 = CalendarUtils.mod(iMod2, 1461);
            iFloorDivide4 = CalendarUtils.floorDivide(iMod3, 365);
            CalendarUtils.mod(iMod3, 365);
        }
        int i5 = (HttpURLConnection.HTTP_BAD_REQUEST * iFloorDivide) + (100 * iFloorDivide2) + (iFloorDivide3 * 4) + iFloorDivide4;
        if (iFloorDivide2 != 4 && iFloorDivide4 != 4) {
            return i5 + 1;
        }
        return i5;
    }

    @Override
    protected boolean isLeapYear(CalendarDate calendarDate) {
        return isLeapYear(((Date) calendarDate).getNormalizedYear());
    }

    boolean isLeapYear(int i) {
        return CalendarUtils.isGregorianLeapYear(i);
    }
}
