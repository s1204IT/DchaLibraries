package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.HttpURLConnection;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Locale;
import libcore.util.ZoneInfo;
import sun.util.calendar.AbstractCalendar;
import sun.util.calendar.BaseCalendar;
import sun.util.calendar.CalendarDate;
import sun.util.calendar.CalendarSystem;
import sun.util.calendar.CalendarUtils;
import sun.util.calendar.Era;
import sun.util.calendar.Gregorian;
import sun.util.calendar.JulianCalendar;

public class GregorianCalendar extends Calendar {
    static final boolean $assertionsDisabled = false;
    public static final int AD = 1;
    public static final int BC = 0;
    static final int BCE = 0;
    static final int CE = 1;
    static final long DEFAULT_GREGORIAN_CUTOVER = -12219292800000L;
    private static final int EPOCH_OFFSET = 719163;
    private static final int EPOCH_YEAR = 1970;
    private static final long ONE_DAY = 86400000;
    private static final int ONE_HOUR = 3600000;
    private static final int ONE_MINUTE = 60000;
    private static final int ONE_SECOND = 1000;
    private static final long ONE_WEEK = 604800000;
    private static JulianCalendar jcal = null;
    private static Era[] jeras = null;
    static final long serialVersionUID = -8125100834729963327L;
    private transient long cachedFixedDate;
    private transient BaseCalendar calsys;
    private transient BaseCalendar.Date cdate;
    private transient BaseCalendar.Date gdate;
    private long gregorianCutover;
    private transient long gregorianCutoverDate;
    private transient int gregorianCutoverYear;
    private transient int gregorianCutoverYearJulian;
    private transient int[] originalFields;
    private transient int[] zoneOffsets;
    static final int[] MONTH_LENGTH = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    static final int[] LEAP_MONTH_LENGTH = {31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    static final int[] MIN_VALUES = {0, 1, 0, 1, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, -46800000, 0};
    static final int[] LEAST_MAX_VALUES = {1, 292269054, 11, 52, 4, 28, 365, 7, 4, 1, 11, 23, 59, 59, 999, 50400000, 1200000};
    static final int[] MAX_VALUES = {1, 292278994, 11, 53, 6, 31, 366, 7, 6, 1, 11, 23, 59, 59, 999, 50400000, 7200000};
    private static final Gregorian gcal = CalendarSystem.getGregorianCalendar();

    public GregorianCalendar() {
        this(TimeZone.getDefaultRef(), Locale.getDefault(Locale.Category.FORMAT));
        setZoneShared(true);
    }

    public GregorianCalendar(TimeZone timeZone) {
        this(timeZone, Locale.getDefault(Locale.Category.FORMAT));
    }

    public GregorianCalendar(Locale locale) {
        this(TimeZone.getDefaultRef(), locale);
        setZoneShared(true);
    }

    public GregorianCalendar(TimeZone timeZone, Locale locale) {
        super(timeZone, locale);
        this.gregorianCutover = DEFAULT_GREGORIAN_CUTOVER;
        this.gregorianCutoverDate = 577736L;
        this.gregorianCutoverYear = 1582;
        this.gregorianCutoverYearJulian = 1582;
        this.cachedFixedDate = Long.MIN_VALUE;
        this.gdate = gcal.newCalendarDate(timeZone);
        setTimeInMillis(System.currentTimeMillis());
    }

    public GregorianCalendar(int i, int i2, int i3) {
        this(i, i2, i3, 0, 0, 0, 0);
    }

    public GregorianCalendar(int i, int i2, int i3, int i4, int i5) {
        this(i, i2, i3, i4, i5, 0, 0);
    }

    public GregorianCalendar(int i, int i2, int i3, int i4, int i5, int i6) {
        this(i, i2, i3, i4, i5, i6, 0);
    }

    GregorianCalendar(int i, int i2, int i3, int i4, int i5, int i6, int i7) {
        this.gregorianCutover = DEFAULT_GREGORIAN_CUTOVER;
        this.gregorianCutoverDate = 577736L;
        this.gregorianCutoverYear = 1582;
        this.gregorianCutoverYearJulian = 1582;
        this.cachedFixedDate = Long.MIN_VALUE;
        this.gdate = gcal.newCalendarDate(getZone());
        set(1, i);
        set(2, i2);
        set(5, i3);
        if (i4 >= 12 && i4 <= 23) {
            internalSet(9, 1);
            internalSet(10, i4 - 12);
        } else {
            internalSet(10, i4);
        }
        setFieldsComputed(1536);
        set(11, i4);
        set(12, i5);
        set(13, i6);
        internalSet(14, i7);
    }

    GregorianCalendar(TimeZone timeZone, Locale locale, boolean z) {
        super(timeZone, locale);
        this.gregorianCutover = DEFAULT_GREGORIAN_CUTOVER;
        this.gregorianCutoverDate = 577736L;
        this.gregorianCutoverYear = 1582;
        this.gregorianCutoverYearJulian = 1582;
        this.cachedFixedDate = Long.MIN_VALUE;
        this.gdate = gcal.newCalendarDate(getZone());
    }

    GregorianCalendar(long j) {
        this();
        setTimeInMillis(j);
    }

    public void setGregorianChange(Date date) {
        long time = date.getTime();
        if (time == this.gregorianCutover) {
            return;
        }
        complete();
        setGregorianChange(time);
    }

    private void setGregorianChange(long j) {
        this.gregorianCutover = j;
        this.gregorianCutoverDate = CalendarUtils.floorDivide(j, ONE_DAY) + 719163;
        if (j == Long.MAX_VALUE) {
            this.gregorianCutoverDate++;
        }
        this.gregorianCutoverYear = getGregorianCutoverDate().getYear();
        BaseCalendar julianCalendarSystem = getJulianCalendarSystem();
        BaseCalendar.Date date = (BaseCalendar.Date) julianCalendarSystem.newCalendarDate(TimeZone.NO_TIMEZONE);
        julianCalendarSystem.getCalendarDateFromFixedDate(date, this.gregorianCutoverDate - 1);
        this.gregorianCutoverYearJulian = date.getNormalizedYear();
        if (this.time < this.gregorianCutover) {
            setUnnormalized();
        }
    }

    public final Date getGregorianChange() {
        return new Date(this.gregorianCutover);
    }

    public boolean isLeapYear(int i) {
        boolean z;
        if ((i & 3) != 0) {
            return $assertionsDisabled;
        }
        if (i > this.gregorianCutoverYear) {
            if (i % 100 != 0 || i % HttpURLConnection.HTTP_BAD_REQUEST == 0) {
                return true;
            }
            return $assertionsDisabled;
        }
        if (i < this.gregorianCutoverYearJulian) {
            return true;
        }
        if (this.gregorianCutoverYear == this.gregorianCutoverYearJulian) {
            z = getCalendarDate(this.gregorianCutoverDate).getMonth() < 3;
        } else {
            z = i == this.gregorianCutoverYear;
        }
        if (z && i % 100 == 0 && i % HttpURLConnection.HTTP_BAD_REQUEST != 0) {
            return $assertionsDisabled;
        }
        return true;
    }

    @Override
    public String getCalendarType() {
        return "gregory";
    }

    @Override
    public boolean equals(Object obj) {
        if ((obj instanceof GregorianCalendar) && super.equals(obj) && this.gregorianCutover == ((GregorianCalendar) obj).gregorianCutover) {
            return true;
        }
        return $assertionsDisabled;
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ ((int) this.gregorianCutoverDate);
    }

    @Override
    public void add(int i, int i2) {
        long j;
        int i3;
        if (i2 == 0) {
            return;
        }
        if (i < 0 || i >= 15) {
            throw new IllegalArgumentException();
        }
        complete();
        if (i == 1) {
            int iInternalGet = internalGet(1);
            if (internalGetEra() == 1) {
                int i4 = iInternalGet + i2;
                if (i4 > 0) {
                    set(1, i4);
                } else {
                    set(1, 1 - i4);
                    set(0, 0);
                }
            } else {
                int i5 = iInternalGet - i2;
                if (i5 > 0) {
                    set(1, i5);
                } else {
                    set(1, 1 - i5);
                    set(0, 1);
                }
            }
            pinDayOfMonth();
            return;
        }
        if (i == 2) {
            int iInternalGet2 = internalGet(2) + i2;
            int iInternalGet3 = internalGet(1);
            if (iInternalGet2 >= 0) {
                i3 = iInternalGet2 / 12;
            } else {
                i3 = ((iInternalGet2 + 1) / 12) - 1;
            }
            if (i3 != 0) {
                if (internalGetEra() == 1) {
                    int i6 = iInternalGet3 + i3;
                    if (i6 > 0) {
                        set(1, i6);
                    } else {
                        set(1, 1 - i6);
                        set(0, 0);
                    }
                } else {
                    int i7 = iInternalGet3 - i3;
                    if (i7 > 0) {
                        set(1, i7);
                    } else {
                        set(1, 1 - i7);
                        set(0, 1);
                    }
                }
            }
            if (iInternalGet2 >= 0) {
                set(2, iInternalGet2 % 12);
            } else {
                int i8 = iInternalGet2 % 12;
                if (i8 < 0) {
                    i8 += 12;
                }
                set(2, 0 + i8);
            }
            pinDayOfMonth();
            return;
        }
        if (i == 0) {
            int iInternalGet4 = internalGet(0) + i2;
            if (iInternalGet4 < 0) {
                iInternalGet4 = 0;
            }
            if (iInternalGet4 > 1) {
                iInternalGet4 = 1;
            }
            set(0, iInternalGet4);
            return;
        }
        long j2 = i2;
        switch (i) {
            case 3:
            case 4:
            case 8:
                j2 *= 7;
                j = 0;
                break;
            case 5:
            case 6:
            case 7:
            case 14:
            default:
                j = 0;
                break;
            case 9:
                j2 = i2 / 2;
                j = (i2 % 2) * 12;
                break;
            case 10:
            case 11:
                j2 *= 3600000;
                j = 0;
                break;
            case 12:
                j2 *= 60000;
                j = 0;
                break;
            case 13:
                j2 *= 1000;
                j = 0;
                break;
        }
        if (i >= 10) {
            setTimeInMillis(this.time + j2);
            return;
        }
        long currentFixedDate = getCurrentFixedDate();
        long jInternalGet = ((((((j + ((long) internalGet(11))) * 60) + ((long) internalGet(12))) * 60) + ((long) internalGet(13))) * 1000) + ((long) internalGet(14));
        if (jInternalGet >= ONE_DAY) {
            currentFixedDate++;
            jInternalGet -= ONE_DAY;
        } else if (jInternalGet < 0) {
            currentFixedDate--;
            jInternalGet += ONE_DAY;
        }
        setTimeInMillis(adjustForZoneAndDaylightSavingsTime(0, (((currentFixedDate + j2) - 719163) * ONE_DAY) + jInternalGet, getZone()));
    }

    @Override
    public void roll(int i, boolean z) {
        roll(i, z ? 1 : -1);
    }

    @Override
    public void roll(int i, int i2) {
        BaseCalendar julianCalendarSystem;
        int i3;
        long jInternalGet;
        int monthLength;
        int dayOfMonth;
        int iInternalGet;
        int i4 = i2;
        if (i4 == 0) {
            return;
        }
        if (i < 0 || i >= 15) {
            throw new IllegalArgumentException();
        }
        complete();
        int minimum = getMinimum(i);
        int maximum = getMaximum(i);
        switch (i) {
            case 2:
                if (!isCutoverYear(this.cdate.getNormalizedYear())) {
                    int iInternalGet2 = (internalGet(2) + i4) % 12;
                    if (iInternalGet2 < 0) {
                        iInternalGet2 += 12;
                    }
                    set(2, iInternalGet2);
                    int iMonthLength = monthLength(iInternalGet2);
                    if (internalGet(5) > iMonthLength) {
                        set(5, iMonthLength);
                        return;
                    }
                    return;
                }
                int actualMaximum = getActualMaximum(2) + 1;
                int iInternalGet3 = (internalGet(2) + i4) % actualMaximum;
                if (iInternalGet3 < 0) {
                    iInternalGet3 += actualMaximum;
                }
                set(2, iInternalGet3);
                int actualMaximum2 = getActualMaximum(5);
                if (internalGet(5) > actualMaximum2) {
                    set(5, actualMaximum2);
                    return;
                }
                return;
            case 3:
                int normalizedYear = this.cdate.getNormalizedYear();
                int actualMaximum3 = getActualMaximum(3);
                set(7, internalGet(7));
                int iInternalGet4 = internalGet(3);
                int i5 = iInternalGet4 + i4;
                if (!isCutoverYear(normalizedYear)) {
                    int weekYear = getWeekYear();
                    if (weekYear == normalizedYear) {
                        if (i5 > minimum && i5 < actualMaximum3) {
                            set(3, i5);
                            return;
                        }
                        long currentFixedDate = getCurrentFixedDate();
                        if (this.calsys.getYearFromFixedDate(currentFixedDate - ((long) (7 * (iInternalGet4 - minimum)))) != normalizedYear) {
                            minimum++;
                        }
                        if (this.calsys.getYearFromFixedDate(currentFixedDate + ((long) (7 * (actualMaximum3 - internalGet(3))))) != normalizedYear) {
                            actualMaximum3--;
                        }
                        i3 = minimum;
                        minimum = iInternalGet4;
                    } else if (weekYear > normalizedYear) {
                        if (i4 < 0) {
                            i4++;
                        }
                        i3 = minimum;
                        minimum = actualMaximum3;
                    } else {
                        if (i4 > 0) {
                            i4 -= iInternalGet4 - actualMaximum3;
                        }
                        i3 = minimum;
                    }
                    set(i, getRolledValue(minimum, i4, i3, actualMaximum3));
                    return;
                }
                long currentFixedDate2 = getCurrentFixedDate();
                if (this.gregorianCutoverYear == this.gregorianCutoverYearJulian) {
                    julianCalendarSystem = getCutoverCalendarSystem();
                } else if (normalizedYear == this.gregorianCutoverYear) {
                    julianCalendarSystem = gcal;
                } else {
                    julianCalendarSystem = getJulianCalendarSystem();
                }
                long j = currentFixedDate2 - ((long) (7 * (iInternalGet4 - minimum)));
                if (julianCalendarSystem.getYearFromFixedDate(j) != normalizedYear) {
                    minimum++;
                }
                long j2 = currentFixedDate2 + ((long) (7 * (actualMaximum3 - iInternalGet4)));
                if ((j2 >= this.gregorianCutoverDate ? gcal : getJulianCalendarSystem()).getYearFromFixedDate(j2) != normalizedYear) {
                    actualMaximum3--;
                }
                BaseCalendar.Date calendarDate = getCalendarDate(j + ((long) ((getRolledValue(iInternalGet4, i4, minimum, actualMaximum3) - 1) * 7)));
                set(2, calendarDate.getMonth() - 1);
                set(5, calendarDate.getDayOfMonth());
                return;
            case 4:
                boolean zIsCutoverYear = isCutoverYear(this.cdate.getNormalizedYear());
                int iInternalGet5 = internalGet(7) - getFirstDayOfWeek();
                if (iInternalGet5 < 0) {
                    iInternalGet5 += 7;
                }
                long currentFixedDate3 = getCurrentFixedDate();
                if (zIsCutoverYear) {
                    jInternalGet = getFixedDateMonth1(this.cdate, currentFixedDate3);
                    monthLength = actualMonthLength();
                } else {
                    jInternalGet = (currentFixedDate3 - ((long) internalGet(5))) + 1;
                    monthLength = this.calsys.getMonthLength(this.cdate);
                }
                long dayOfWeekDateOnOrBefore = BaseCalendar.getDayOfWeekDateOnOrBefore(6 + jInternalGet, getFirstDayOfWeek());
                if (((int) (dayOfWeekDateOnOrBefore - jInternalGet)) >= getMinimalDaysInFirstWeek()) {
                    dayOfWeekDateOnOrBefore -= 7;
                }
                long rolledValue = ((long) iInternalGet5) + dayOfWeekDateOnOrBefore + ((long) ((getRolledValue(internalGet(i), i4, 1, getActualMaximum(i)) - 1) * 7));
                if (rolledValue >= jInternalGet) {
                    long j3 = ((long) monthLength) + jInternalGet;
                    if (rolledValue >= j3) {
                        rolledValue = j3 - 1;
                    }
                } else {
                    rolledValue = jInternalGet;
                }
                if (zIsCutoverYear) {
                    dayOfMonth = getCalendarDate(rolledValue).getDayOfMonth();
                } else {
                    dayOfMonth = ((int) (rolledValue - jInternalGet)) + 1;
                }
                set(5, dayOfMonth);
                return;
            case 5:
                if (!isCutoverYear(this.cdate.getNormalizedYear())) {
                    maximum = this.calsys.getMonthLength(this.cdate);
                } else {
                    long currentFixedDate4 = getCurrentFixedDate();
                    long fixedDateMonth1 = getFixedDateMonth1(this.cdate, currentFixedDate4);
                    set(5, getCalendarDate(fixedDateMonth1 + ((long) getRolledValue((int) (currentFixedDate4 - fixedDateMonth1), i4, 0, actualMonthLength() - 1))).getDayOfMonth());
                    return;
                }
                break;
            case 6:
                maximum = getActualMaximum(i);
                if (isCutoverYear(this.cdate.getNormalizedYear())) {
                    long currentFixedDate5 = getCurrentFixedDate();
                    long jInternalGet2 = (currentFixedDate5 - ((long) internalGet(6))) + 1;
                    BaseCalendar.Date calendarDate2 = getCalendarDate((jInternalGet2 + ((long) getRolledValue(((int) (currentFixedDate5 - jInternalGet2)) + 1, i4, minimum, maximum))) - 1);
                    set(2, calendarDate2.getMonth() - 1);
                    set(5, calendarDate2.getDayOfMonth());
                    return;
                }
                break;
            case 7:
                if (!isCutoverYear(this.cdate.getNormalizedYear()) && (iInternalGet = internalGet(3)) > 1 && iInternalGet < 52) {
                    set(3, iInternalGet);
                    maximum = 7;
                } else {
                    int i6 = i4 % 7;
                    if (i6 == 0) {
                        return;
                    }
                    long currentFixedDate6 = getCurrentFixedDate();
                    long dayOfWeekDateOnOrBefore2 = BaseCalendar.getDayOfWeekDateOnOrBefore(currentFixedDate6, getFirstDayOfWeek());
                    long j4 = currentFixedDate6 + ((long) i6);
                    if (j4 < dayOfWeekDateOnOrBefore2) {
                        j4 += 7;
                    } else if (j4 >= dayOfWeekDateOnOrBefore2 + 7) {
                        j4 -= 7;
                    }
                    BaseCalendar.Date calendarDate3 = getCalendarDate(j4);
                    set(0, calendarDate3.getNormalizedYear() <= 0 ? 0 : 1);
                    set(calendarDate3.getYear(), calendarDate3.getMonth() - 1, calendarDate3.getDayOfMonth());
                    return;
                }
                break;
            case 8:
                if (!isCutoverYear(this.cdate.getNormalizedYear())) {
                    int iInternalGet6 = internalGet(5);
                    int monthLength2 = this.calsys.getMonthLength(this.cdate);
                    int i7 = monthLength2 % 7;
                    int i8 = monthLength2 / 7;
                    if ((iInternalGet6 - 1) % 7 < i7) {
                        i8++;
                    }
                    maximum = i8;
                    set(7, internalGet(7));
                    minimum = 1;
                } else {
                    long currentFixedDate7 = getCurrentFixedDate();
                    long fixedDateMonth12 = getFixedDateMonth1(this.cdate, currentFixedDate7);
                    int iActualMonthLength = actualMonthLength();
                    int i9 = iActualMonthLength % 7;
                    int i10 = iActualMonthLength / 7;
                    int i11 = ((int) (currentFixedDate7 - fixedDateMonth12)) % 7;
                    if (i11 < i9) {
                        i10++;
                    }
                    long rolledValue2 = fixedDateMonth12 + ((long) ((getRolledValue(internalGet(i), i4, 1, i10) - 1) * 7)) + ((long) i11);
                    AbstractCalendar julianCalendarSystem2 = rolledValue2 >= this.gregorianCutoverDate ? gcal : getJulianCalendarSystem();
                    BaseCalendar.Date date = (BaseCalendar.Date) julianCalendarSystem2.newCalendarDate(TimeZone.NO_TIMEZONE);
                    julianCalendarSystem2.getCalendarDateFromFixedDate(date, rolledValue2);
                    set(5, date.getDayOfMonth());
                    return;
                }
                break;
            case 10:
            case 11:
                int i12 = maximum + 1;
                int iInternalGet7 = internalGet(i);
                int i13 = (i4 + iInternalGet7) % i12;
                if (i13 < 0) {
                    i13 += i12;
                }
                this.time += (long) (ONE_HOUR * (i13 - iInternalGet7));
                CalendarDate calendarDate4 = this.calsys.getCalendarDate(this.time, getZone());
                if (internalGet(5) != calendarDate4.getDayOfMonth()) {
                    calendarDate4.setDate(internalGet(1), internalGet(2) + 1, internalGet(5));
                    if (i == 10) {
                        calendarDate4.addHours(12);
                    }
                    this.time = this.calsys.getTime(calendarDate4);
                }
                int hours = calendarDate4.getHours();
                internalSet(i, hours % i12);
                if (i == 10) {
                    internalSet(11, hours);
                } else {
                    internalSet(9, hours / 12);
                    internalSet(10, hours % 12);
                }
                int zoneOffset = calendarDate4.getZoneOffset();
                int daylightSaving = calendarDate4.getDaylightSaving();
                internalSet(15, zoneOffset - daylightSaving);
                internalSet(16, daylightSaving);
                return;
        }
        set(i, getRolledValue(internalGet(i), i4, minimum, maximum));
    }

    @Override
    public int getMinimum(int i) {
        return MIN_VALUES[i];
    }

    @Override
    public int getMaximum(int i) {
        switch (i) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 8:
                if (this.gregorianCutoverYear <= 200) {
                    GregorianCalendar gregorianCalendar = (GregorianCalendar) clone();
                    gregorianCalendar.setLenient(true);
                    gregorianCalendar.setTimeInMillis(this.gregorianCutover);
                    int actualMaximum = gregorianCalendar.getActualMaximum(i);
                    gregorianCalendar.setTimeInMillis(this.gregorianCutover - 1);
                    return Math.max(MAX_VALUES[i], Math.max(actualMaximum, gregorianCalendar.getActualMaximum(i)));
                }
                break;
        }
        return MAX_VALUES[i];
    }

    @Override
    public int getGreatestMinimum(int i) {
        if (i == 5) {
            return Math.max(MIN_VALUES[i], getCalendarDate(getFixedDateMonth1(getGregorianCutoverDate(), this.gregorianCutoverDate)).getDayOfMonth());
        }
        return MIN_VALUES[i];
    }

    @Override
    public int getLeastMaximum(int i) {
        switch (i) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 8:
                GregorianCalendar gregorianCalendar = (GregorianCalendar) clone();
                gregorianCalendar.setLenient(true);
                gregorianCalendar.setTimeInMillis(this.gregorianCutover);
                int actualMaximum = gregorianCalendar.getActualMaximum(i);
                gregorianCalendar.setTimeInMillis(this.gregorianCutover - 1);
                return Math.min(LEAST_MAX_VALUES[i], Math.min(actualMaximum, gregorianCalendar.getActualMaximum(i)));
            case 7:
            default:
                return LEAST_MAX_VALUES[i];
        }
    }

    @Override
    public int getActualMinimum(int i) {
        if (i == 5) {
            GregorianCalendar normalizedCalendar = getNormalizedCalendar();
            int normalizedYear = normalizedCalendar.cdate.getNormalizedYear();
            if (normalizedYear == this.gregorianCutoverYear || normalizedYear == this.gregorianCutoverYearJulian) {
                return getCalendarDate(getFixedDateMonth1(normalizedCalendar.cdate, normalizedCalendar.calsys.getFixedDate(normalizedCalendar.cdate))).getDayOfMonth();
            }
        }
        return getMinimum(i);
    }

    @Override
    public int getActualMaximum(int i) {
        int year;
        long fixedDate;
        int iActualMonthLength;
        int dayOfWeek;
        if (((1 << i) & 130689) != 0) {
            return getMaximum(i);
        }
        GregorianCalendar normalizedCalendar = getNormalizedCalendar();
        BaseCalendar.Date date = normalizedCalendar.cdate;
        BaseCalendar baseCalendar = normalizedCalendar.calsys;
        int normalizedYear = date.getNormalizedYear();
        switch (i) {
            case 1:
                if (normalizedCalendar == this) {
                    normalizedCalendar = (GregorianCalendar) clone();
                }
                long yearOffsetInMillis = normalizedCalendar.getYearOffsetInMillis();
                if (normalizedCalendar.internalGetEra() == 1) {
                    normalizedCalendar.setTimeInMillis(Long.MAX_VALUE);
                    year = normalizedCalendar.get(1);
                    if (yearOffsetInMillis > normalizedCalendar.getYearOffsetInMillis()) {
                        year--;
                    }
                } else {
                    CalendarDate calendarDate = (normalizedCalendar.getTimeInMillis() >= this.gregorianCutover ? gcal : getJulianCalendarSystem()).getCalendarDate(Long.MIN_VALUE, getZone());
                    long dayOfYear = ((((((((baseCalendar.getDayOfYear(calendarDate) - 1) * 24) + ((long) calendarDate.getHours())) * 60) + ((long) calendarDate.getMinutes())) * 60) + ((long) calendarDate.getSeconds())) * 1000) + ((long) calendarDate.getMillis());
                    year = calendarDate.getYear();
                    if (year <= 0) {
                        year = 1 - year;
                    }
                    if (yearOffsetInMillis < dayOfYear) {
                        year--;
                    }
                }
                return year;
            case 2:
                if (!normalizedCalendar.isCutoverYear(normalizedYear)) {
                    return 11;
                }
                do {
                    normalizedYear++;
                    fixedDate = gcal.getFixedDate(normalizedYear, 1, 1, null);
                } while (fixedDate < this.gregorianCutoverDate);
                baseCalendar.getCalendarDateFromFixedDate((BaseCalendar.Date) date.clone(), fixedDate - 1);
                return r15.getMonth() - 1;
            case 3:
                if (!normalizedCalendar.isCutoverYear(normalizedYear)) {
                    CalendarDate calendarDateNewCalendarDate = baseCalendar.newCalendarDate(TimeZone.NO_TIMEZONE);
                    calendarDateNewCalendarDate.setDate(date.getYear(), 1, 1);
                    int dayOfWeek2 = baseCalendar.getDayOfWeek(calendarDateNewCalendarDate) - getFirstDayOfWeek();
                    if (dayOfWeek2 < 0) {
                        dayOfWeek2 += 7;
                    }
                    int minimalDaysInFirstWeek = (dayOfWeek2 + getMinimalDaysInFirstWeek()) - 1;
                    return (minimalDaysInFirstWeek == 6 || (date.isLeapYear() && (minimalDaysInFirstWeek == 5 || minimalDaysInFirstWeek == 12))) ? 53 : 52;
                }
                if (normalizedCalendar == this) {
                    normalizedCalendar = (GregorianCalendar) normalizedCalendar.clone();
                }
                int actualMaximum = getActualMaximum(6);
                normalizedCalendar.set(6, actualMaximum);
                int i2 = normalizedCalendar.get(3);
                if (internalGet(1) == normalizedCalendar.getWeekYear()) {
                    return i2;
                }
                normalizedCalendar.set(6, actualMaximum - 7);
                return normalizedCalendar.get(3);
            case 4:
                if (normalizedCalendar.isCutoverYear(normalizedYear)) {
                    if (normalizedCalendar == this) {
                        normalizedCalendar = (GregorianCalendar) normalizedCalendar.clone();
                    }
                    int iInternalGet = normalizedCalendar.internalGet(1);
                    int iInternalGet2 = normalizedCalendar.internalGet(2);
                    do {
                        int i3 = normalizedCalendar.get(4);
                        normalizedCalendar.add(4, 1);
                        if (normalizedCalendar.get(1) == iInternalGet) {
                        }
                        return i3;
                    } while (normalizedCalendar.get(2) == iInternalGet2);
                    return i3;
                }
                CalendarDate calendarDateNewCalendarDate2 = baseCalendar.newCalendarDate(null);
                calendarDateNewCalendarDate2.setDate(date.getYear(), date.getMonth(), 1);
                int dayOfWeek3 = baseCalendar.getDayOfWeek(calendarDateNewCalendarDate2);
                int monthLength = baseCalendar.getMonthLength(calendarDateNewCalendarDate2);
                int firstDayOfWeek = dayOfWeek3 - getFirstDayOfWeek();
                if (firstDayOfWeek < 0) {
                    firstDayOfWeek += 7;
                }
                int i4 = 7 - firstDayOfWeek;
                int i5 = i4 >= getMinimalDaysInFirstWeek() ? 4 : 3;
                int i6 = monthLength - (i4 + 21);
                if (i6 <= 0) {
                    return i5;
                }
                int i7 = i5 + 1;
                return i6 > 7 ? i7 + 1 : i7;
            case 5:
                int monthLength2 = baseCalendar.getMonthLength(date);
                if (normalizedCalendar.isCutoverYear(normalizedYear) && date.getDayOfMonth() != monthLength2) {
                    long currentFixedDate = normalizedCalendar.getCurrentFixedDate();
                    if (currentFixedDate < this.gregorianCutoverDate) {
                        return normalizedCalendar.getCalendarDate((normalizedCalendar.getFixedDateMonth1(normalizedCalendar.cdate, currentFixedDate) + ((long) normalizedCalendar.actualMonthLength())) - 1).getDayOfMonth();
                    }
                }
                return monthLength2;
            case 6:
                if (!normalizedCalendar.isCutoverYear(normalizedYear)) {
                    return baseCalendar.getYearLength(date);
                }
                long fixedDate2 = this.gregorianCutoverYear == this.gregorianCutoverYearJulian ? normalizedCalendar.getCutoverCalendarSystem().getFixedDate(normalizedYear, 1, 1, null) : normalizedYear == this.gregorianCutoverYearJulian ? baseCalendar.getFixedDate(normalizedYear, 1, 1, null) : this.gregorianCutoverDate;
                long fixedDate3 = gcal.getFixedDate(normalizedYear + 1, 1, 1, null);
                if (fixedDate3 < this.gregorianCutoverDate) {
                    fixedDate3 = this.gregorianCutoverDate;
                }
                return (int) (fixedDate3 - fixedDate2);
            case 7:
            default:
                throw new ArrayIndexOutOfBoundsException(i);
            case 8:
                int dayOfWeek4 = date.getDayOfWeek();
                if (normalizedCalendar.isCutoverYear(normalizedYear)) {
                    if (normalizedCalendar == this) {
                        normalizedCalendar = (GregorianCalendar) clone();
                    }
                    iActualMonthLength = normalizedCalendar.actualMonthLength();
                    normalizedCalendar.set(5, normalizedCalendar.getActualMinimum(5));
                    dayOfWeek = normalizedCalendar.get(7);
                } else {
                    BaseCalendar.Date date2 = (BaseCalendar.Date) date.clone();
                    iActualMonthLength = baseCalendar.getMonthLength(date2);
                    date2.setDayOfMonth(1);
                    baseCalendar.normalize(date2);
                    dayOfWeek = date2.getDayOfWeek();
                }
                int i8 = dayOfWeek4 - dayOfWeek;
                if (i8 < 0) {
                    i8 += 7;
                }
                return ((iActualMonthLength - i8) + 6) / 7;
        }
    }

    private long getYearOffsetInMillis() {
        return (((((((((long) ((internalGet(6) - 1) * 24)) + ((long) internalGet(11))) * 60) + ((long) internalGet(12))) * 60) + ((long) internalGet(13))) * 1000) + ((long) internalGet(14))) - ((long) (internalGet(15) + internalGet(16)));
    }

    @Override
    public Object clone() {
        GregorianCalendar gregorianCalendar = (GregorianCalendar) super.clone();
        gregorianCalendar.gdate = (BaseCalendar.Date) this.gdate.clone();
        if (this.cdate != null) {
            if (this.cdate != this.gdate) {
                gregorianCalendar.cdate = (BaseCalendar.Date) this.cdate.clone();
            } else {
                gregorianCalendar.cdate = gregorianCalendar.gdate;
            }
        }
        gregorianCalendar.originalFields = null;
        gregorianCalendar.zoneOffsets = null;
        return gregorianCalendar;
    }

    @Override
    public TimeZone getTimeZone() {
        TimeZone timeZone = super.getTimeZone();
        this.gdate.setZone(timeZone);
        if (this.cdate != null && this.cdate != this.gdate) {
            this.cdate.setZone(timeZone);
        }
        return timeZone;
    }

    @Override
    public void setTimeZone(TimeZone timeZone) {
        super.setTimeZone(timeZone);
        this.gdate.setZone(timeZone);
        if (this.cdate != null && this.cdate != this.gdate) {
            this.cdate.setZone(timeZone);
        }
    }

    @Override
    public final boolean isWeekDateSupported() {
        return true;
    }

    @Override
    public int getWeekYear() {
        int i = get(1);
        if (internalGetEra() == 0) {
            i = 1 - i;
        }
        if (i > this.gregorianCutoverYear + 1) {
            int iInternalGet = internalGet(3);
            if (internalGet(2) == 0) {
                if (iInternalGet >= 52) {
                    return i - 1;
                }
                return i;
            }
            if (iInternalGet == 1) {
                return i + 1;
            }
            return i;
        }
        int iInternalGet2 = internalGet(6);
        int actualMaximum = getActualMaximum(6);
        int minimalDaysInFirstWeek = getMinimalDaysInFirstWeek();
        if (iInternalGet2 > minimalDaysInFirstWeek && iInternalGet2 < actualMaximum - 6) {
            return i;
        }
        GregorianCalendar gregorianCalendar = (GregorianCalendar) clone();
        gregorianCalendar.setLenient(true);
        gregorianCalendar.setTimeZone(TimeZone.getTimeZone("GMT"));
        gregorianCalendar.set(6, 1);
        gregorianCalendar.complete();
        int firstDayOfWeek = getFirstDayOfWeek() - gregorianCalendar.get(7);
        if (firstDayOfWeek != 0) {
            if (firstDayOfWeek < 0) {
                firstDayOfWeek += 7;
            }
            gregorianCalendar.add(6, firstDayOfWeek);
        }
        int i2 = gregorianCalendar.get(6);
        if (iInternalGet2 < i2) {
            return i2 <= minimalDaysInFirstWeek ? i - 1 : i;
        }
        int i3 = i + 1;
        gregorianCalendar.set(1, i3);
        gregorianCalendar.set(6, 1);
        gregorianCalendar.complete();
        int firstDayOfWeek2 = getFirstDayOfWeek() - gregorianCalendar.get(7);
        if (firstDayOfWeek2 != 0) {
            if (firstDayOfWeek2 < 0) {
                firstDayOfWeek2 += 7;
            }
            gregorianCalendar.add(6, firstDayOfWeek2);
        }
        int i4 = gregorianCalendar.get(6) - 1;
        if (i4 == 0) {
            i4 = 7;
        }
        return (i4 < minimalDaysInFirstWeek || (actualMaximum - iInternalGet2) + 1 > 7 - i4) ? i : i3;
    }

    @Override
    public void setWeekDate(int i, int i2, int i3) {
        if (i3 < 1 || i3 > 7) {
            throw new IllegalArgumentException("invalid dayOfWeek: " + i3);
        }
        GregorianCalendar gregorianCalendar = (GregorianCalendar) clone();
        gregorianCalendar.setLenient(true);
        int i4 = gregorianCalendar.get(0);
        gregorianCalendar.clear();
        gregorianCalendar.setTimeZone(TimeZone.getTimeZone("GMT"));
        gregorianCalendar.set(0, i4);
        gregorianCalendar.set(1, i);
        gregorianCalendar.set(3, 1);
        gregorianCalendar.set(7, getFirstDayOfWeek());
        int firstDayOfWeek = i3 - getFirstDayOfWeek();
        if (firstDayOfWeek < 0) {
            firstDayOfWeek += 7;
        }
        int i5 = firstDayOfWeek + ((i2 - 1) * 7);
        if (i5 != 0) {
            gregorianCalendar.add(6, i5);
        } else {
            gregorianCalendar.complete();
        }
        if (isLenient() || (gregorianCalendar.getWeekYear() == i && gregorianCalendar.internalGet(3) == i2 && gregorianCalendar.internalGet(7) == i3)) {
            set(0, gregorianCalendar.internalGet(0));
            set(1, gregorianCalendar.internalGet(1));
            set(2, gregorianCalendar.internalGet(2));
            set(5, gregorianCalendar.internalGet(5));
            internalSet(3, i2);
            complete();
            return;
        }
        throw new IllegalArgumentException();
    }

    @Override
    public int getWeeksInWeekYear() {
        GregorianCalendar normalizedCalendar = getNormalizedCalendar();
        int weekYear = normalizedCalendar.getWeekYear();
        if (weekYear == normalizedCalendar.internalGet(1)) {
            return normalizedCalendar.getActualMaximum(3);
        }
        if (normalizedCalendar == this) {
            normalizedCalendar = (GregorianCalendar) normalizedCalendar.clone();
        }
        normalizedCalendar.setWeekDate(weekYear, 2, internalGet(7));
        return normalizedCalendar.getActualMaximum(3);
    }

    @Override
    protected void computeFields() {
        int i = 131071;
        if (isPartiallyNormalized()) {
            int setStateFields = getSetStateFields();
            int i2 = 131071 & (~setStateFields);
            if (i2 != 0 || this.calsys == null) {
                setStateFields |= computeFields(i2, 98304 & setStateFields);
            }
            i = setStateFields;
        } else {
            computeFields(131071, 0);
        }
        setFieldsComputed(i);
    }

    private int computeFields(int i, int i2) {
        int offsetsByUtcTime;
        int i3;
        int i4;
        int year;
        long fixedDate;
        ZoneInfo zone = getZone();
        if (this.zoneOffsets == null) {
            this.zoneOffsets = new int[2];
        }
        int weekNumber = 1;
        if (i2 == 98304) {
            offsetsByUtcTime = 0;
        } else if (zone instanceof ZoneInfo) {
            offsetsByUtcTime = zone.getOffsetsByUtcTime(this.time, this.zoneOffsets);
        } else {
            int offset = zone.getOffset(this.time);
            this.zoneOffsets[0] = zone.getRawOffset();
            this.zoneOffsets[1] = offset - this.zoneOffsets[0];
            offsetsByUtcTime = offset;
        }
        if (i2 != 0) {
            if (isFieldSet(i2, 15)) {
                this.zoneOffsets[0] = internalGet(15);
            }
            if (isFieldSet(i2, 16)) {
                this.zoneOffsets[1] = internalGet(16);
            }
            offsetsByUtcTime = this.zoneOffsets[1] + this.zoneOffsets[0];
        }
        long j = (((long) offsetsByUtcTime) / ONE_DAY) + (this.time / ONE_DAY);
        int i5 = (offsetsByUtcTime % 86400000) + ((int) (this.time % ONE_DAY));
        long j2 = i5;
        if (j2 >= ONE_DAY) {
            i3 = (int) (j2 - ONE_DAY);
            j++;
        } else {
            i3 = i5;
            while (i3 < 0) {
                i3 = (int) (((long) i3) + ONE_DAY);
                j--;
            }
        }
        long j3 = j + 719163;
        if (j3 >= this.gregorianCutoverDate) {
            if (j3 != this.cachedFixedDate) {
                gcal.getCalendarDateFromFixedDate(this.gdate, j3);
                this.cachedFixedDate = j3;
            }
            int year2 = this.gdate.getYear();
            if (year2 <= 0) {
                year = 1 - year2;
                i4 = 0;
            } else {
                year = year2;
                i4 = 1;
            }
            this.calsys = gcal;
            this.cdate = this.gdate;
        } else {
            this.calsys = getJulianCalendarSystem();
            this.cdate = jcal.newCalendarDate(getZone());
            jcal.getCalendarDateFromFixedDate(this.cdate, j3);
            i4 = this.cdate.getEra() == jeras[0] ? 0 : 1;
            year = this.cdate.getYear();
        }
        internalSet(0, i4);
        internalSet(1, year);
        int i6 = i | 3;
        int month = this.cdate.getMonth() - 1;
        int dayOfMonth = this.cdate.getDayOfMonth();
        if ((i & 164) != 0) {
            internalSet(2, month);
            internalSet(5, dayOfMonth);
            internalSet(7, this.cdate.getDayOfWeek());
            i6 |= 164;
        }
        if ((i & 32256) != 0) {
            if (i3 != 0) {
                int i7 = i3 / ONE_HOUR;
                internalSet(11, i7);
                internalSet(9, i7 / 12);
                internalSet(10, i7 % 12);
                int i8 = i3 % ONE_HOUR;
                internalSet(12, i8 / ONE_MINUTE);
                int i9 = i8 % ONE_MINUTE;
                internalSet(13, i9 / 1000);
                internalSet(14, i9 % 1000);
            } else {
                internalSet(11, 0);
                internalSet(9, 0);
                internalSet(10, 0);
                internalSet(12, 0);
                internalSet(13, 0);
                internalSet(14, 0);
            }
            i6 |= 32256;
        }
        if ((i & 98304) != 0) {
            internalSet(15, this.zoneOffsets[0]);
            internalSet(16, this.zoneOffsets[1]);
            i6 |= 98304;
        }
        if ((i & 344) != 0) {
            int normalizedYear = this.cdate.getNormalizedYear();
            long fixedDate2 = this.calsys.getFixedDate(normalizedYear, 1, 1, this.cdate);
            int i10 = ((int) (j3 - fixedDate2)) + 1;
            long fixedDateMonth1 = (j3 - ((long) dayOfMonth)) + 1;
            int i11 = this.calsys == gcal ? this.gregorianCutoverYear : this.gregorianCutoverYearJulian;
            int i12 = dayOfMonth - 1;
            if (normalizedYear == i11) {
                if (this.gregorianCutoverYearJulian <= this.gregorianCutoverYear) {
                    fixedDate2 = getFixedDateJan1(this.cdate, j3);
                    if (j3 >= this.gregorianCutoverDate) {
                        fixedDateMonth1 = getFixedDateMonth1(this.cdate, j3);
                    }
                }
                i10 = ((int) (j3 - fixedDate2)) + 1;
                i12 = (int) (j3 - fixedDateMonth1);
            }
            internalSet(6, i10);
            internalSet(8, (i12 / 7) + 1);
            int weekNumber2 = getWeekNumber(fixedDate2, j3);
            if (weekNumber2 == 0) {
                long j4 = fixedDate2 - 1;
                long fixedDate3 = fixedDate2 - 365;
                if (normalizedYear > i11 + 1) {
                    if (CalendarUtils.isGregorianLeapYear(normalizedYear - 1)) {
                        fixedDate3--;
                    }
                } else if (normalizedYear <= this.gregorianCutoverYearJulian) {
                    if (CalendarUtils.isJulianLeapYear(normalizedYear - 1)) {
                        fixedDate3--;
                    }
                } else {
                    BaseCalendar baseCalendar = this.calsys;
                    int normalizedYear2 = getCalendarDate(j4).getNormalizedYear();
                    if (normalizedYear2 == this.gregorianCutoverYear) {
                        BaseCalendar cutoverCalendarSystem = getCutoverCalendarSystem();
                        if (cutoverCalendarSystem == jcal) {
                            fixedDate3 = cutoverCalendarSystem.getFixedDate(normalizedYear2, 1, 1, null);
                        } else {
                            fixedDate3 = this.gregorianCutoverDate;
                            Gregorian gregorian = gcal;
                        }
                    } else if (normalizedYear2 <= this.gregorianCutoverYearJulian) {
                        fixedDate3 = getJulianCalendarSystem().getFixedDate(normalizedYear2, 1, 1, null);
                    }
                }
                weekNumber = getWeekNumber(fixedDate3, j4);
            } else if (normalizedYear > this.gregorianCutoverYear || normalizedYear < this.gregorianCutoverYearJulian - 1) {
                if (weekNumber2 >= 52) {
                    long j5 = fixedDate2 + 365;
                    if (this.cdate.isLeapYear()) {
                        j5++;
                    }
                    long dayOfWeekDateOnOrBefore = BaseCalendar.getDayOfWeekDateOnOrBefore(6 + j5, getFirstDayOfWeek());
                    if (((int) (dayOfWeekDateOnOrBefore - j5)) < getMinimalDaysInFirstWeek() || j3 < dayOfWeekDateOnOrBefore - 7) {
                        weekNumber = weekNumber2;
                    }
                } else {
                    weekNumber = weekNumber2;
                }
            } else {
                BaseCalendar cutoverCalendarSystem2 = this.calsys;
                int i13 = normalizedYear + 1;
                if (i13 == this.gregorianCutoverYearJulian + 1 && i13 < this.gregorianCutoverYear) {
                    i13 = this.gregorianCutoverYear;
                }
                if (i13 == this.gregorianCutoverYear) {
                    cutoverCalendarSystem2 = getCutoverCalendarSystem();
                }
                if (i13 > this.gregorianCutoverYear || this.gregorianCutoverYearJulian == this.gregorianCutoverYear || i13 == this.gregorianCutoverYearJulian) {
                    fixedDate = cutoverCalendarSystem2.getFixedDate(i13, 1, 1, null);
                } else {
                    fixedDate = this.gregorianCutoverDate;
                    Gregorian gregorian2 = gcal;
                }
                long dayOfWeekDateOnOrBefore2 = BaseCalendar.getDayOfWeekDateOnOrBefore(6 + fixedDate, getFirstDayOfWeek());
                if (((int) (dayOfWeekDateOnOrBefore2 - fixedDate)) < getMinimalDaysInFirstWeek() || j3 < dayOfWeekDateOnOrBefore2 - 7) {
                }
            }
            internalSet(3, weekNumber);
            internalSet(4, getWeekNumber(fixedDateMonth1, j3));
            return i6 | 344;
        }
        return i6;
    }

    private int getWeekNumber(long j, long j2) {
        long dayOfWeekDateOnOrBefore = Gregorian.getDayOfWeekDateOnOrBefore(6 + j, getFirstDayOfWeek());
        if (((int) (dayOfWeekDateOnOrBefore - j)) >= getMinimalDaysInFirstWeek()) {
            dayOfWeekDateOnOrBefore -= 7;
        }
        int i = (int) (j2 - dayOfWeekDateOnOrBefore);
        if (i >= 0) {
            return (i / 7) + 1;
        }
        return CalendarUtils.floorDivide(i, 7) + 1;
    }

    @Override
    protected void computeTime() {
        long jInternalGet;
        long fixedDate;
        long fixedDate2;
        if (!isLenient()) {
            if (this.originalFields == null) {
                this.originalFields = new int[17];
            }
            for (int i = 0; i < 17; i++) {
                int iInternalGet = internalGet(i);
                if (isExternallySet(i) && (iInternalGet < getMinimum(i) || iInternalGet > getMaximum(i))) {
                    throw new IllegalArgumentException(getFieldName(i));
                }
                this.originalFields[i] = iInternalGet;
            }
        }
        int iSelectFields = selectFields();
        int iInternalGet2 = isSet(1) ? internalGet(1) : EPOCH_YEAR;
        int iInternalGetEra = internalGetEra();
        if (iInternalGetEra == 0) {
            iInternalGet2 = 1 - iInternalGet2;
        } else if (iInternalGetEra != 1) {
            throw new IllegalArgumentException("Invalid era");
        }
        if (iInternalGet2 <= 0 && !isSet(0)) {
            iSelectFields |= 1;
            setFieldsComputed(1);
        }
        if (!isFieldSet(iSelectFields, 11)) {
            jInternalGet = ((long) internalGet(10)) + 0;
            if (isFieldSet(iSelectFields, 9)) {
                jInternalGet += (long) (internalGet(9) * 12);
            }
        } else {
            jInternalGet = ((long) internalGet(11)) + 0;
        }
        long jInternalGet2 = (((((jInternalGet * 60) + ((long) internalGet(12))) * 60) + ((long) internalGet(13))) * 1000) + ((long) internalGet(14));
        long j = jInternalGet2 / ONE_DAY;
        long j2 = jInternalGet2 % ONE_DAY;
        while (j2 < 0) {
            j2 += ONE_DAY;
            j--;
        }
        if (iInternalGet2 > this.gregorianCutoverYear && iInternalGet2 > this.gregorianCutoverYearJulian) {
            fixedDate = getFixedDate(gcal, iInternalGet2, iSelectFields) + j;
            if (fixedDate < this.gregorianCutoverDate) {
                fixedDate = j + getFixedDate(getJulianCalendarSystem(), iInternalGet2, iSelectFields);
                fixedDate2 = fixedDate;
                if (!isFieldSet(iSelectFields, 6)) {
                    if (this.gregorianCutoverYear != this.gregorianCutoverYearJulian) {
                    }
                }
            }
        } else {
            if (iInternalGet2 >= this.gregorianCutoverYear || iInternalGet2 >= this.gregorianCutoverYearJulian) {
                fixedDate = getFixedDate(getJulianCalendarSystem(), iInternalGet2, iSelectFields) + j;
                fixedDate2 = j + getFixedDate(gcal, iInternalGet2, iSelectFields);
            } else {
                fixedDate = getFixedDate(getJulianCalendarSystem(), iInternalGet2, iSelectFields) + j;
                if (fixedDate >= this.gregorianCutoverDate) {
                    fixedDate2 = fixedDate;
                }
            }
            if (!isFieldSet(iSelectFields, 6) || isFieldSet(iSelectFields, 3)) {
                if (this.gregorianCutoverYear != this.gregorianCutoverYearJulian) {
                    if (iInternalGet2 == this.gregorianCutoverYear) {
                        fixedDate = fixedDate2;
                    } else if (fixedDate2 >= this.gregorianCutoverDate) {
                        if (fixedDate >= this.gregorianCutoverDate || this.calsys == gcal || this.calsys == null) {
                        }
                    } else if (fixedDate >= this.gregorianCutoverDate && !isLenient()) {
                        throw new IllegalArgumentException("the specified date doesn't exist");
                    }
                }
            }
        }
        int i2 = 98304 & iSelectFields;
        this.time = adjustForZoneAndDaylightSavingsTime(i2, ((fixedDate - 719163) * ONE_DAY) + j2, getZone());
        int iComputeFields = computeFields(iSelectFields | getSetStateFields(), i2);
        if (!isLenient()) {
            for (int i3 = 0; i3 < 17; i3++) {
                if (isExternallySet(i3) && this.originalFields[i3] != internalGet(i3)) {
                    String str = this.originalFields[i3] + " -> " + internalGet(i3);
                    System.arraycopy((Object) this.originalFields, 0, (Object) this.fields, 0, this.fields.length);
                    throw new IllegalArgumentException(getFieldName(i3) + ": " + str);
                }
            }
        }
        setFieldsNormalized(iComputeFields);
    }

    private long adjustForZoneAndDaylightSavingsTime(int i, long j, TimeZone timeZone) {
        int iInternalGet;
        int iInternalGet2 = 0;
        if (i != 98304) {
            if (this.zoneOffsets == null) {
                this.zoneOffsets = new int[2];
            }
            long jInternalGet = j - ((long) (isFieldSet(i, 15) ? internalGet(15) : timeZone.getRawOffset()));
            if (timeZone instanceof ZoneInfo) {
                ((ZoneInfo) timeZone).getOffsetsByUtcTime(jInternalGet, this.zoneOffsets);
            } else {
                timeZone.getOffsets(jInternalGet, this.zoneOffsets);
            }
            iInternalGet2 = this.zoneOffsets[0];
            iInternalGet = adjustDstOffsetForInvalidWallClock(jInternalGet, timeZone, this.zoneOffsets[1]);
        } else {
            iInternalGet = 0;
        }
        if (i != 0) {
            if (isFieldSet(i, 15)) {
                iInternalGet2 = internalGet(15);
            }
            if (isFieldSet(i, 16)) {
                iInternalGet = internalGet(16);
            }
        }
        return (j - ((long) iInternalGet2)) - ((long) iInternalGet);
    }

    private int adjustDstOffsetForInvalidWallClock(long j, TimeZone timeZone, int i) {
        if (i != 0 && !timeZone.inDaylightTime(new Date(j - ((long) i)))) {
            return 0;
        }
        return i;
    }

    private long getFixedDate(BaseCalendar baseCalendar, int i, int i2) {
        int iFloorDivide;
        int iInternalGet;
        int firstDayOfWeek;
        int iInternalGet2;
        long dayOfWeekDateOnOrBefore;
        int i3 = 0;
        if (isFieldSet(i2, 2)) {
            int iInternalGet3 = internalGet(2);
            if (iInternalGet3 > 11) {
                iFloorDivide = i + (iInternalGet3 / 12);
                i3 = iInternalGet3 % 12;
            } else if (iInternalGet3 < 0) {
                int[] iArr = new int[1];
                iFloorDivide = i + CalendarUtils.floorDivide(iInternalGet3, 12, iArr);
                i3 = iArr[0];
            } else {
                iFloorDivide = i;
                i3 = iInternalGet3;
            }
        } else {
            iFloorDivide = i;
        }
        long fixedDate = baseCalendar.getFixedDate(iFloorDivide, i3 + 1, 1, baseCalendar == gcal ? this.gdate : null);
        if (isFieldSet(i2, 2)) {
            if (!isFieldSet(i2, 5)) {
                if (isFieldSet(i2, 4)) {
                    long dayOfWeekDateOnOrBefore2 = BaseCalendar.getDayOfWeekDateOnOrBefore(fixedDate + 6, getFirstDayOfWeek());
                    if (dayOfWeekDateOnOrBefore2 - fixedDate >= getMinimalDaysInFirstWeek()) {
                        dayOfWeekDateOnOrBefore2 -= 7;
                    }
                    if (isFieldSet(i2, 7)) {
                        dayOfWeekDateOnOrBefore2 = BaseCalendar.getDayOfWeekDateOnOrBefore(dayOfWeekDateOnOrBefore2 + 6, internalGet(7));
                    }
                    return dayOfWeekDateOnOrBefore2 + ((long) (7 * (internalGet(4) - 1)));
                }
                if (isFieldSet(i2, 7)) {
                    firstDayOfWeek = internalGet(7);
                } else {
                    firstDayOfWeek = getFirstDayOfWeek();
                }
                if (isFieldSet(i2, 8)) {
                    iInternalGet2 = internalGet(8);
                } else {
                    iInternalGet2 = 1;
                }
                if (iInternalGet2 >= 0) {
                    dayOfWeekDateOnOrBefore = BaseCalendar.getDayOfWeekDateOnOrBefore((fixedDate + ((long) (7 * iInternalGet2))) - 1, firstDayOfWeek);
                } else {
                    dayOfWeekDateOnOrBefore = BaseCalendar.getDayOfWeekDateOnOrBefore((fixedDate + ((long) (monthLength(i3, iFloorDivide) + (7 * (iInternalGet2 + 1))))) - 1, firstDayOfWeek);
                }
                return dayOfWeekDateOnOrBefore;
            }
            if (isSet(5)) {
                return (fixedDate + ((long) internalGet(5))) - 1;
            }
            return fixedDate;
        }
        if (iFloorDivide == this.gregorianCutoverYear && baseCalendar == gcal && fixedDate < this.gregorianCutoverDate && this.gregorianCutoverYear != this.gregorianCutoverYearJulian) {
            fixedDate = this.gregorianCutoverDate;
        }
        if (isFieldSet(i2, 6)) {
            return (fixedDate + ((long) internalGet(6))) - 1;
        }
        long dayOfWeekDateOnOrBefore3 = BaseCalendar.getDayOfWeekDateOnOrBefore(fixedDate + 6, getFirstDayOfWeek());
        if (dayOfWeekDateOnOrBefore3 - fixedDate >= getMinimalDaysInFirstWeek()) {
            dayOfWeekDateOnOrBefore3 -= 7;
        }
        if (isFieldSet(i2, 7) && (iInternalGet = internalGet(7)) != getFirstDayOfWeek()) {
            dayOfWeekDateOnOrBefore3 = BaseCalendar.getDayOfWeekDateOnOrBefore(dayOfWeekDateOnOrBefore3 + 6, iInternalGet);
        }
        return dayOfWeekDateOnOrBefore3 + (7 * (((long) internalGet(3)) - 1));
    }

    private GregorianCalendar getNormalizedCalendar() {
        if (!isFullyNormalized()) {
            GregorianCalendar gregorianCalendar = (GregorianCalendar) clone();
            gregorianCalendar.setLenient(true);
            gregorianCalendar.complete();
            return gregorianCalendar;
        }
        return this;
    }

    private static synchronized BaseCalendar getJulianCalendarSystem() {
        if (jcal == null) {
            jcal = (JulianCalendar) CalendarSystem.forName("julian");
            jeras = jcal.getEras();
        }
        return jcal;
    }

    private BaseCalendar getCutoverCalendarSystem() {
        if (this.gregorianCutoverYearJulian < this.gregorianCutoverYear) {
            return gcal;
        }
        return getJulianCalendarSystem();
    }

    private boolean isCutoverYear(int i) {
        if (i == (this.calsys == gcal ? this.gregorianCutoverYear : this.gregorianCutoverYearJulian)) {
            return true;
        }
        return $assertionsDisabled;
    }

    private long getFixedDateJan1(BaseCalendar.Date date, long j) {
        if (this.gregorianCutoverYear != this.gregorianCutoverYearJulian && j >= this.gregorianCutoverDate) {
            return this.gregorianCutoverDate;
        }
        return getJulianCalendarSystem().getFixedDate(date.getNormalizedYear(), 1, 1, null);
    }

    private long getFixedDateMonth1(BaseCalendar.Date date, long j) {
        BaseCalendar.Date gregorianCutoverDate = getGregorianCutoverDate();
        if (gregorianCutoverDate.getMonth() == 1 && gregorianCutoverDate.getDayOfMonth() == 1) {
            return (j - ((long) date.getDayOfMonth())) + 1;
        }
        if (date.getMonth() == gregorianCutoverDate.getMonth()) {
            BaseCalendar.Date lastJulianDate = getLastJulianDate();
            if (this.gregorianCutoverYear == this.gregorianCutoverYearJulian && gregorianCutoverDate.getMonth() == lastJulianDate.getMonth()) {
                return jcal.getFixedDate(date.getNormalizedYear(), date.getMonth(), 1, null);
            }
            return this.gregorianCutoverDate;
        }
        return (j - ((long) date.getDayOfMonth())) + 1;
    }

    private BaseCalendar.Date getCalendarDate(long j) {
        AbstractCalendar julianCalendarSystem = j >= this.gregorianCutoverDate ? gcal : getJulianCalendarSystem();
        BaseCalendar.Date date = (BaseCalendar.Date) julianCalendarSystem.newCalendarDate(TimeZone.NO_TIMEZONE);
        julianCalendarSystem.getCalendarDateFromFixedDate(date, j);
        return date;
    }

    private BaseCalendar.Date getGregorianCutoverDate() {
        return getCalendarDate(this.gregorianCutoverDate);
    }

    private BaseCalendar.Date getLastJulianDate() {
        return getCalendarDate(this.gregorianCutoverDate - 1);
    }

    private int monthLength(int i, int i2) {
        return isLeapYear(i2) ? LEAP_MONTH_LENGTH[i] : MONTH_LENGTH[i];
    }

    private int monthLength(int i) {
        int iInternalGet = internalGet(1);
        if (internalGetEra() == 0) {
            iInternalGet = 1 - iInternalGet;
        }
        return monthLength(i, iInternalGet);
    }

    private int actualMonthLength() {
        int normalizedYear = this.cdate.getNormalizedYear();
        if (normalizedYear != this.gregorianCutoverYear && normalizedYear != this.gregorianCutoverYearJulian) {
            return this.calsys.getMonthLength(this.cdate);
        }
        BaseCalendar.Date dateNewCalendarDate = (BaseCalendar.Date) this.cdate.clone();
        long fixedDateMonth1 = getFixedDateMonth1(dateNewCalendarDate, this.calsys.getFixedDate(dateNewCalendarDate));
        long monthLength = ((long) this.calsys.getMonthLength(dateNewCalendarDate)) + fixedDateMonth1;
        if (monthLength < this.gregorianCutoverDate) {
            return (int) (monthLength - fixedDateMonth1);
        }
        if (this.cdate != this.gdate) {
            dateNewCalendarDate = gcal.newCalendarDate(TimeZone.NO_TIMEZONE);
        }
        gcal.getCalendarDateFromFixedDate(dateNewCalendarDate, monthLength);
        return (int) (getFixedDateMonth1(dateNewCalendarDate, monthLength) - fixedDateMonth1);
    }

    private int yearLength(int i) {
        return isLeapYear(i) ? 366 : 365;
    }

    private int yearLength() {
        int iInternalGet = internalGet(1);
        if (internalGetEra() == 0) {
            iInternalGet = 1 - iInternalGet;
        }
        return yearLength(iInternalGet);
    }

    private void pinDayOfMonth() {
        int iMonthLength;
        int iInternalGet = internalGet(1);
        if (iInternalGet > this.gregorianCutoverYear || iInternalGet < this.gregorianCutoverYearJulian) {
            iMonthLength = monthLength(internalGet(2));
        } else {
            iMonthLength = getNormalizedCalendar().getActualMaximum(5);
        }
        if (internalGet(5) > iMonthLength) {
            set(5, iMonthLength);
        }
    }

    private long getCurrentFixedDate() {
        return this.calsys == gcal ? this.cachedFixedDate : this.calsys.getFixedDate(this.cdate);
    }

    private static int getRolledValue(int i, int i2, int i3, int i4) {
        int i5 = (i4 - i3) + 1;
        int i6 = i + (i2 % i5);
        if (i6 > i4) {
            return i6 - i5;
        }
        if (i6 < i3) {
            return i6 + i5;
        }
        return i6;
    }

    private int internalGetEra() {
        if (isSet(0)) {
            return internalGet(0);
        }
        return 1;
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        if (this.gdate == null) {
            this.gdate = gcal.newCalendarDate(getZone());
            this.cachedFixedDate = Long.MIN_VALUE;
        }
        setGregorianChange(this.gregorianCutover);
    }

    public ZonedDateTime toZonedDateTime() {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(getTimeInMillis()), getTimeZone().toZoneId());
    }

    public static GregorianCalendar from(ZonedDateTime zonedDateTime) {
        GregorianCalendar gregorianCalendar = new GregorianCalendar(TimeZone.getTimeZone(zonedDateTime.getZone()));
        gregorianCalendar.setGregorianChange(new Date(Long.MIN_VALUE));
        gregorianCalendar.setFirstDayOfWeek(2);
        gregorianCalendar.setMinimalDaysInFirstWeek(4);
        try {
            gregorianCalendar.setTimeInMillis(Math.addExact(Math.multiplyExact(zonedDateTime.toEpochSecond(), 1000L), zonedDateTime.get(ChronoField.MILLI_OF_SECOND)));
            return gregorianCalendar;
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
