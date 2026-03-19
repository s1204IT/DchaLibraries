package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.HttpURLConnection;
import sun.util.calendar.BaseCalendar;
import sun.util.calendar.CalendarDate;
import sun.util.calendar.CalendarSystem;
import sun.util.calendar.CalendarUtils;
import sun.util.calendar.Era;
import sun.util.calendar.Gregorian;
import sun.util.calendar.LocalGregorianCalendar;
import sun.util.locale.provider.CalendarDataUtility;

class JapaneseImperialCalendar extends Calendar {
    public static final int BEFORE_MEIJI = 0;
    private static final int EPOCH_OFFSET = 719163;
    private static final int EPOCH_YEAR = 1970;
    public static final int HEISEI = 4;
    public static final int MEIJI = 1;
    private static final long ONE_DAY = 86400000;
    private static final int ONE_HOUR = 3600000;
    private static final int ONE_MINUTE = 60000;
    private static final int ONE_SECOND = 1000;
    private static final long ONE_WEEK = 604800000;
    public static final int SHOWA = 3;
    public static final int TAISHO = 2;
    private static final Era[] eras;
    private static final long serialVersionUID = -3364572813905467929L;
    private static final long[] sinceFixedDates;
    private transient long cachedFixedDate;
    private transient LocalGregorianCalendar.Date jdate;
    private transient int[] originalFields;
    private transient int[] zoneOffsets;
    private static final LocalGregorianCalendar jcal = (LocalGregorianCalendar) CalendarSystem.forName("japanese");
    private static final Gregorian gcal = CalendarSystem.getGregorianCalendar();
    static final boolean $assertionsDisabled = false;
    private static final Era BEFORE_MEIJI_ERA = new Era("BeforeMeiji", "BM", Long.MIN_VALUE, $assertionsDisabled);
    static final int[] MIN_VALUES = {0, -292275055, 0, 1, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, -46800000, 0};
    static final int[] LEAST_MAX_VALUES = {0, 0, 0, 0, 4, 28, 0, 7, 4, 1, 11, 23, 59, 59, 999, 50400000, 1200000};
    static final int[] MAX_VALUES = {0, 292278994, 11, 53, 6, 31, 366, 7, 6, 1, 11, 23, 59, 59, 999, 50400000, 7200000};

    static {
        Era[] eras2 = jcal.getEras();
        int length = eras2.length + 1;
        eras = new Era[length];
        sinceFixedDates = new long[length];
        sinceFixedDates[0] = gcal.getFixedDate(BEFORE_MEIJI_ERA.getSinceDate());
        eras[0] = BEFORE_MEIJI_ERA;
        int length2 = eras2.length;
        int i = 1;
        int i2 = 0;
        while (i2 < length2) {
            Era era = eras2[i2];
            sinceFixedDates[i] = gcal.getFixedDate(era.getSinceDate());
            eras[i] = era;
            i2++;
            i++;
        }
        int[] iArr = LEAST_MAX_VALUES;
        int[] iArr2 = MAX_VALUES;
        int length3 = eras.length - 1;
        iArr2[0] = length3;
        iArr[0] = length3;
        Gregorian.Date dateNewCalendarDate = gcal.newCalendarDate(TimeZone.NO_TIMEZONE);
        int iMin = Integer.MAX_VALUE;
        int iMin2 = Integer.MAX_VALUE;
        for (int i3 = 1; i3 < eras.length; i3++) {
            long j = sinceFixedDates[i3];
            CalendarDate sinceDate = eras[i3].getSinceDate();
            dateNewCalendarDate.setDate(sinceDate.getYear(), 1, 1);
            long fixedDate = gcal.getFixedDate(dateNewCalendarDate);
            if (j != fixedDate) {
                iMin2 = Math.min(((int) (j - fixedDate)) + 1, iMin2);
            }
            dateNewCalendarDate.setDate(sinceDate.getYear(), 12, 31);
            long fixedDate2 = gcal.getFixedDate(dateNewCalendarDate);
            if (j != fixedDate2) {
                iMin2 = Math.min(((int) (fixedDate2 - j)) + 1, iMin2);
            }
            LocalGregorianCalendar.Date calendarDate = getCalendarDate(j - 1);
            int year = calendarDate.getYear();
            if (calendarDate.getMonth() != 1 || calendarDate.getDayOfMonth() != 1) {
                year--;
            }
            iMin = Math.min(year, iMin);
        }
        LEAST_MAX_VALUES[1] = iMin;
        LEAST_MAX_VALUES[6] = iMin2;
    }

    JapaneseImperialCalendar(TimeZone timeZone, Locale locale) {
        super(timeZone, locale);
        this.cachedFixedDate = Long.MIN_VALUE;
        this.jdate = jcal.newCalendarDate(timeZone);
        setTimeInMillis(System.currentTimeMillis());
    }

    JapaneseImperialCalendar(TimeZone timeZone, Locale locale, boolean z) {
        super(timeZone, locale);
        this.cachedFixedDate = Long.MIN_VALUE;
        this.jdate = jcal.newCalendarDate(timeZone);
    }

    @Override
    public String getCalendarType() {
        return "japanese";
    }

    @Override
    public boolean equals(Object obj) {
        if ((obj instanceof JapaneseImperialCalendar) && super.equals(obj)) {
            return true;
        }
        return $assertionsDisabled;
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ this.jdate.hashCode();
    }

    @Override
    public void add(int i, int i2) {
        long j;
        if (i2 == 0) {
            return;
        }
        if (i < 0 || i >= 15) {
            throw new IllegalArgumentException();
        }
        complete();
        if (i == 1) {
            LocalGregorianCalendar.Date date = (LocalGregorianCalendar.Date) this.jdate.clone();
            date.addYear(i2);
            pinDayOfMonth(date);
            set(0, getEraIndex(date));
            set(1, date.getYear());
            set(2, date.getMonth() - 1);
            set(5, date.getDayOfMonth());
            return;
        }
        if (i == 2) {
            LocalGregorianCalendar.Date date2 = (LocalGregorianCalendar.Date) this.jdate.clone();
            date2.addMonth(i2);
            pinDayOfMonth(date2);
            set(0, getEraIndex(date2));
            set(1, date2.getYear());
            set(2, date2.getMonth() - 1);
            set(5, date2.getDayOfMonth());
            return;
        }
        if (i == 0) {
            int iInternalGet = internalGet(0) + i2;
            if (iInternalGet >= 0) {
                if (iInternalGet > eras.length - 1) {
                    iInternalGet = eras.length - 1;
                }
            } else {
                iInternalGet = 0;
            }
            set(0, iInternalGet);
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
        long j3 = this.cachedFixedDate;
        long jInternalGet = ((((((j + ((long) internalGet(11))) * 60) + ((long) internalGet(12))) * 60) + ((long) internalGet(13))) * 1000) + ((long) internalGet(14));
        if (jInternalGet >= ONE_DAY) {
            j3++;
            jInternalGet -= ONE_DAY;
        } else if (jInternalGet < 0) {
            j3--;
            jInternalGet += ONE_DAY;
        }
        long j4 = j3 + j2;
        int iInternalGet2 = internalGet(15) + internalGet(16);
        setTimeInMillis((((j4 - 719163) * ONE_DAY) + jInternalGet) - ((long) iInternalGet2));
        int iInternalGet3 = iInternalGet2 - (internalGet(15) + internalGet(16));
        if (iInternalGet3 != 0) {
            long j5 = iInternalGet3;
            setTimeInMillis(this.time + j5);
            if (this.cachedFixedDate != j4) {
                setTimeInMillis(this.time - j5);
            }
        }
    }

    @Override
    public void roll(int i, boolean z) {
        roll(i, z ? 1 : -1);
    }

    @Override
    public void roll(int i, int i2) {
        int dayOfMonth;
        long jInternalGet;
        int monthLength;
        int iInternalGet;
        if (i2 == 0) {
            return;
        }
        if (i < 0 || i >= 15) {
            throw new IllegalArgumentException();
        }
        complete();
        int minimum = getMinimum(i);
        int maximum = getMaximum(i);
        switch (i) {
            case 1:
                minimum = getActualMinimum(i);
                maximum = getActualMaximum(i);
                break;
            case 2:
                if (!isTransitionYear(this.jdate.getNormalizedYear())) {
                    int year = this.jdate.getYear();
                    if (year == getMaximum(1)) {
                        LocalGregorianCalendar.Date calendarDate = jcal.getCalendarDate(this.time, getZone());
                        LocalGregorianCalendar.Date calendarDate2 = jcal.getCalendarDate(Long.MAX_VALUE, getZone());
                        int month = calendarDate2.getMonth() - 1;
                        int rolledValue = getRolledValue(internalGet(i), i2, minimum, month);
                        if (rolledValue == month) {
                            calendarDate.addYear(-400);
                            int i3 = rolledValue + 1;
                            calendarDate.setMonth(i3);
                            if (calendarDate.getDayOfMonth() > calendarDate2.getDayOfMonth()) {
                                calendarDate.setDayOfMonth(calendarDate2.getDayOfMonth());
                                jcal.normalize(calendarDate);
                            }
                            if (calendarDate.getDayOfMonth() == calendarDate2.getDayOfMonth() && calendarDate.getTimeOfDay() > calendarDate2.getTimeOfDay()) {
                                calendarDate.setMonth(i3);
                                calendarDate.setDayOfMonth(calendarDate2.getDayOfMonth() - 1);
                                jcal.normalize(calendarDate);
                                rolledValue = calendarDate.getMonth() - 1;
                            }
                            set(5, calendarDate.getDayOfMonth());
                        }
                        set(2, rolledValue);
                        return;
                    }
                    if (year != getMinimum(1)) {
                        int iInternalGet2 = (internalGet(2) + i2) % 12;
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
                    LocalGregorianCalendar.Date calendarDate3 = jcal.getCalendarDate(this.time, getZone());
                    LocalGregorianCalendar.Date calendarDate4 = jcal.getCalendarDate(Long.MIN_VALUE, getZone());
                    int month2 = calendarDate4.getMonth() - 1;
                    int rolledValue2 = getRolledValue(internalGet(i), i2, month2, maximum);
                    if (rolledValue2 == month2) {
                        calendarDate3.addYear(HttpURLConnection.HTTP_BAD_REQUEST);
                        int i4 = rolledValue2 + 1;
                        calendarDate3.setMonth(i4);
                        if (calendarDate3.getDayOfMonth() < calendarDate4.getDayOfMonth()) {
                            calendarDate3.setDayOfMonth(calendarDate4.getDayOfMonth());
                            jcal.normalize(calendarDate3);
                        }
                        if (calendarDate3.getDayOfMonth() == calendarDate4.getDayOfMonth() && calendarDate3.getTimeOfDay() < calendarDate4.getTimeOfDay()) {
                            calendarDate3.setMonth(i4);
                            calendarDate3.setDayOfMonth(calendarDate4.getDayOfMonth() + 1);
                            jcal.normalize(calendarDate3);
                            rolledValue2 = calendarDate3.getMonth() - 1;
                        }
                        set(5, calendarDate3.getDayOfMonth());
                    }
                    set(2, rolledValue2);
                    return;
                }
                int eraIndex = getEraIndex(this.jdate);
                CalendarDate sinceDate = null;
                if (this.jdate.getYear() == 1) {
                    sinceDate = eras[eraIndex].getSinceDate();
                    minimum = sinceDate.getMonth() - 1;
                } else if (eraIndex < eras.length - 1) {
                    sinceDate = eras[eraIndex + 1].getSinceDate();
                    if (sinceDate.getYear() == this.jdate.getNormalizedYear()) {
                        maximum = sinceDate.getMonth() - 1;
                        if (sinceDate.getDayOfMonth() == 1) {
                            maximum--;
                        }
                    }
                }
                if (minimum != maximum) {
                    int rolledValue3 = getRolledValue(internalGet(i), i2, minimum, maximum);
                    set(2, rolledValue3);
                    if (rolledValue3 == minimum) {
                        if ((sinceDate.getMonth() != 1 || sinceDate.getDayOfMonth() != 1) && this.jdate.getDayOfMonth() < sinceDate.getDayOfMonth()) {
                            set(5, sinceDate.getDayOfMonth());
                            return;
                        }
                        return;
                    }
                    if (rolledValue3 == maximum && sinceDate.getMonth() - 1 == rolledValue3 && this.jdate.getDayOfMonth() >= (dayOfMonth = sinceDate.getDayOfMonth())) {
                        set(5, dayOfMonth - 1);
                        return;
                    }
                    return;
                }
                return;
            case 3:
                int normalizedYear = this.jdate.getNormalizedYear();
                maximum = getActualMaximum(3);
                set(7, internalGet(7));
                int iInternalGet3 = internalGet(3);
                int i5 = iInternalGet3 + i2;
                if (!isTransitionYear(this.jdate.getNormalizedYear())) {
                    int year2 = this.jdate.getYear();
                    if (year2 == getMaximum(1)) {
                        maximum = getActualMaximum(3);
                    } else if (year2 == getMinimum(1)) {
                        minimum = getActualMinimum(3);
                        maximum = getActualMaximum(3);
                        if (i5 > minimum && i5 < maximum) {
                            set(3, i5);
                            return;
                        }
                    }
                    if (i5 > minimum && i5 < maximum) {
                        set(3, i5);
                        return;
                    }
                    long j = this.cachedFixedDate;
                    long j2 = j - ((long) (7 * (iInternalGet3 - minimum)));
                    if (year2 != getMinimum(1)) {
                        if (gcal.getYearFromFixedDate(j2) != normalizedYear) {
                            minimum++;
                        }
                    } else if (j2 < jcal.getFixedDate(jcal.getCalendarDate(Long.MIN_VALUE, getZone()))) {
                        minimum++;
                    }
                    if (gcal.getYearFromFixedDate(j + ((long) (7 * (maximum - internalGet(3))))) != normalizedYear) {
                        maximum--;
                    }
                } else {
                    long j3 = this.cachedFixedDate;
                    long j4 = j3 - ((long) (7 * (iInternalGet3 - minimum)));
                    LocalGregorianCalendar.Date calendarDate5 = getCalendarDate(j4);
                    if (calendarDate5.getEra() != this.jdate.getEra() || calendarDate5.getYear() != this.jdate.getYear()) {
                        minimum++;
                    }
                    jcal.getCalendarDateFromFixedDate(calendarDate5, j3 + ((long) (7 * (maximum - iInternalGet3))));
                    if (calendarDate5.getEra() != this.jdate.getEra() || calendarDate5.getYear() != this.jdate.getYear()) {
                        maximum--;
                    }
                    LocalGregorianCalendar.Date calendarDate6 = getCalendarDate(j4 + ((long) ((getRolledValue(iInternalGet3, i2, minimum, maximum) - 1) * 7)));
                    set(2, calendarDate6.getMonth() - 1);
                    set(5, calendarDate6.getDayOfMonth());
                    return;
                }
                break;
            case 4:
                boolean zIsTransitionYear = isTransitionYear(this.jdate.getNormalizedYear());
                int iInternalGet4 = internalGet(7) - getFirstDayOfWeek();
                if (iInternalGet4 < 0) {
                    iInternalGet4 += 7;
                }
                long j5 = this.cachedFixedDate;
                if (!zIsTransitionYear) {
                    jInternalGet = (j5 - ((long) internalGet(5))) + 1;
                    monthLength = jcal.getMonthLength(this.jdate);
                } else {
                    jInternalGet = getFixedDateMonth1(this.jdate, j5);
                    monthLength = actualMonthLength();
                }
                long dayOfWeekDateOnOrBefore = LocalGregorianCalendar.getDayOfWeekDateOnOrBefore(6 + jInternalGet, getFirstDayOfWeek());
                if (((int) (dayOfWeekDateOnOrBefore - jInternalGet)) >= getMinimalDaysInFirstWeek()) {
                    dayOfWeekDateOnOrBefore -= 7;
                }
                long rolledValue4 = ((long) iInternalGet4) + dayOfWeekDateOnOrBefore + ((long) ((getRolledValue(internalGet(i), i2, 1, getActualMaximum(i)) - 1) * 7));
                if (rolledValue4 >= jInternalGet) {
                    long j6 = ((long) monthLength) + jInternalGet;
                    if (rolledValue4 >= j6) {
                        rolledValue4 = j6 - 1;
                    }
                } else {
                    rolledValue4 = jInternalGet;
                }
                set(5, ((int) (rolledValue4 - jInternalGet)) + 1);
                return;
            case 5:
                if (!isTransitionYear(this.jdate.getNormalizedYear())) {
                    maximum = jcal.getMonthLength(this.jdate);
                } else {
                    long fixedDateMonth1 = getFixedDateMonth1(this.jdate, this.cachedFixedDate);
                    set(5, getCalendarDate(fixedDateMonth1 + ((long) getRolledValue((int) (this.cachedFixedDate - fixedDateMonth1), i2, 0, actualMonthLength() - 1))).getDayOfMonth());
                    return;
                }
                break;
            case 6:
                maximum = getActualMaximum(i);
                if (isTransitionYear(this.jdate.getNormalizedYear())) {
                    LocalGregorianCalendar.Date calendarDate7 = getCalendarDate((this.cachedFixedDate - ((long) internalGet(6))) + ((long) getRolledValue(internalGet(6), i2, minimum, maximum)));
                    set(2, calendarDate7.getMonth() - 1);
                    set(5, calendarDate7.getDayOfMonth());
                    return;
                }
                break;
            case 7:
                int normalizedYear2 = this.jdate.getNormalizedYear();
                if (!isTransitionYear(normalizedYear2) && !isTransitionYear(normalizedYear2 - 1) && (iInternalGet = internalGet(3)) > 1 && iInternalGet < 52) {
                    set(3, internalGet(3));
                    maximum = 7;
                } else {
                    int i6 = i2 % 7;
                    if (i6 == 0) {
                        return;
                    }
                    long j7 = this.cachedFixedDate;
                    long dayOfWeekDateOnOrBefore2 = LocalGregorianCalendar.getDayOfWeekDateOnOrBefore(j7, getFirstDayOfWeek());
                    long j8 = j7 + ((long) i6);
                    if (j8 < dayOfWeekDateOnOrBefore2) {
                        j8 += 7;
                    } else if (j8 >= dayOfWeekDateOnOrBefore2 + 7) {
                        j8 -= 7;
                    }
                    LocalGregorianCalendar.Date calendarDate8 = getCalendarDate(j8);
                    set(0, getEraIndex(calendarDate8));
                    set(calendarDate8.getYear(), calendarDate8.getMonth() - 1, calendarDate8.getDayOfMonth());
                    return;
                }
                break;
            case 8:
                if (!isTransitionYear(this.jdate.getNormalizedYear())) {
                    int iInternalGet5 = internalGet(5);
                    int monthLength2 = jcal.getMonthLength(this.jdate);
                    int i7 = monthLength2 % 7;
                    int i8 = monthLength2 / 7;
                    if ((iInternalGet5 - 1) % 7 < i7) {
                        i8++;
                    }
                    maximum = i8;
                    set(7, internalGet(7));
                    minimum = 1;
                } else {
                    long j9 = this.cachedFixedDate;
                    long fixedDateMonth12 = getFixedDateMonth1(this.jdate, j9);
                    int iActualMonthLength = actualMonthLength();
                    int i9 = iActualMonthLength % 7;
                    int i10 = iActualMonthLength / 7;
                    int i11 = ((int) (j9 - fixedDateMonth12)) % 7;
                    if (i11 < i9) {
                        i10++;
                    }
                    set(5, getCalendarDate(fixedDateMonth12 + ((long) ((getRolledValue(internalGet(i), i2, 1, i10) - 1) * 7)) + ((long) i11)).getDayOfMonth());
                    return;
                }
                break;
            case 10:
            case 11:
                int i12 = maximum + 1;
                int iInternalGet6 = internalGet(i);
                int i13 = (i2 + iInternalGet6) % i12;
                if (i13 < 0) {
                    i13 += i12;
                }
                this.time += (long) (ONE_HOUR * (i13 - iInternalGet6));
                LocalGregorianCalendar.Date calendarDate9 = jcal.getCalendarDate(this.time, getZone());
                if (internalGet(5) != calendarDate9.getDayOfMonth()) {
                    calendarDate9.setEra(this.jdate.getEra());
                    calendarDate9.setDate(internalGet(1), internalGet(2) + 1, internalGet(5));
                    if (i == 10) {
                        calendarDate9.addHours(12);
                    }
                    this.time = jcal.getTime(calendarDate9);
                }
                int hours = calendarDate9.getHours();
                internalSet(i, hours % i12);
                if (i == 10) {
                    internalSet(11, hours);
                } else {
                    internalSet(9, hours / 12);
                    internalSet(10, hours % 12);
                }
                int zoneOffset = calendarDate9.getZoneOffset();
                int daylightSaving = calendarDate9.getDaylightSaving();
                internalSet(15, zoneOffset - daylightSaving);
                internalSet(16, daylightSaving);
                return;
        }
        set(i, getRolledValue(internalGet(i), i2, minimum, maximum));
    }

    @Override
    public String getDisplayName(int i, int i2, Locale locale) {
        if (!checkDisplayNameParams(i, i2, 1, 4, locale, 647)) {
            return null;
        }
        int i3 = get(i);
        if (i == 1 && (getBaseStyle(i2) != 2 || i3 != 1 || get(0) == 0)) {
            return null;
        }
        String strRetrieveFieldValueName = CalendarDataUtility.retrieveFieldValueName(getCalendarType(), i, i3, i2, locale);
        if (strRetrieveFieldValueName == null && i == 0 && i3 < eras.length) {
            Era era = eras[i3];
            return i2 == 1 ? era.getAbbreviation() : era.getName();
        }
        return strRetrieveFieldValueName;
    }

    @Override
    public Map<String, Integer> getDisplayNames(int i, int i2, Locale locale) {
        if (!checkDisplayNameParams(i, i2, 0, 4, locale, 647)) {
            return null;
        }
        Map<String, Integer> mapRetrieveFieldValueNames = CalendarDataUtility.retrieveFieldValueNames(getCalendarType(), i, i2, locale);
        if (mapRetrieveFieldValueNames != null && i == 0) {
            int size = mapRetrieveFieldValueNames.size();
            if (i2 == 0) {
                HashSet hashSet = new HashSet();
                Iterator<String> it = mapRetrieveFieldValueNames.keySet().iterator();
                while (it.hasNext()) {
                    hashSet.add(mapRetrieveFieldValueNames.get(it.next()));
                }
                size = hashSet.size();
            }
            if (size < eras.length) {
                int baseStyle = getBaseStyle(i2);
                while (size < eras.length) {
                    Era era = eras[size];
                    if (baseStyle == 0 || baseStyle == 1 || baseStyle == 4) {
                        mapRetrieveFieldValueNames.put(era.getAbbreviation(), Integer.valueOf(size));
                    }
                    if (baseStyle == 0 || baseStyle == 2) {
                        mapRetrieveFieldValueNames.put(era.getName(), Integer.valueOf(size));
                    }
                    size++;
                }
            }
        }
        return mapRetrieveFieldValueNames;
    }

    @Override
    public int getMinimum(int i) {
        return MIN_VALUES[i];
    }

    @Override
    public int getMaximum(int i) {
        if (i == 1) {
            return Math.max(LEAST_MAX_VALUES[1], jcal.getCalendarDate(Long.MAX_VALUE, getZone()).getYear());
        }
        return MAX_VALUES[i];
    }

    @Override
    public int getGreatestMinimum(int i) {
        if (i == 1) {
            return 1;
        }
        return MIN_VALUES[i];
    }

    @Override
    public int getLeastMaximum(int i) {
        if (i == 1) {
            return Math.min(LEAST_MAX_VALUES[1], getMaximum(1));
        }
        return LEAST_MAX_VALUES[i];
    }

    @Override
    public int getActualMinimum(int i) {
        if (!isFieldSet(14, i)) {
            return getMinimum(i);
        }
        LocalGregorianCalendar.Date calendarDate = jcal.getCalendarDate(getNormalizedCalendar().getTimeInMillis(), getZone());
        int eraIndex = getEraIndex(calendarDate);
        switch (i) {
            case 1:
                if (eraIndex > 0) {
                    CalendarDate calendarDate2 = jcal.getCalendarDate(eras[eraIndex].getSince(getZone()), getZone());
                    calendarDate.setYear(calendarDate2.getYear());
                    jcal.normalize(calendarDate);
                    return getYearOffsetInMillis(calendarDate) < getYearOffsetInMillis(calendarDate2) ? 2 : 1;
                }
                int minimum = getMinimum(i);
                CalendarDate calendarDate3 = jcal.getCalendarDate(Long.MIN_VALUE, getZone());
                int year = calendarDate3.getYear();
                if (year > 400) {
                    year -= 400;
                }
                calendarDate.setYear(year);
                jcal.normalize(calendarDate);
                if (getYearOffsetInMillis(calendarDate) < getYearOffsetInMillis(calendarDate3)) {
                    minimum++;
                }
                return minimum;
            case 2:
                if (eraIndex <= 1 || calendarDate.getYear() != 1) {
                    return 0;
                }
                LocalGregorianCalendar.Date calendarDate4 = jcal.getCalendarDate(eras[eraIndex].getSince(getZone()), getZone());
                int month = calendarDate4.getMonth() - 1;
                if (calendarDate.getDayOfMonth() < calendarDate4.getDayOfMonth()) {
                    return month + 1;
                }
                return month;
            case 3:
                LocalGregorianCalendar.Date calendarDate5 = jcal.getCalendarDate(Long.MIN_VALUE, getZone());
                calendarDate5.addYear(HttpURLConnection.HTTP_BAD_REQUEST);
                jcal.normalize(calendarDate5);
                calendarDate.setEra(calendarDate5.getEra());
                calendarDate.setYear(calendarDate5.getYear());
                jcal.normalize(calendarDate);
                long fixedDate = jcal.getFixedDate(calendarDate5);
                long fixedDate2 = jcal.getFixedDate(calendarDate);
                long weekNumber = fixedDate2 - ((long) (7 * (getWeekNumber(fixedDate, fixedDate2) - 1)));
                return (weekNumber < fixedDate || (weekNumber == fixedDate && calendarDate.getTimeOfDay() < calendarDate5.getTimeOfDay())) ? 2 : 1;
            default:
                return 0;
        }
    }

    @Override
    public int getActualMaximum(int i) {
        LocalGregorianCalendar.Date calendarDate;
        int year;
        int yearLength;
        int fixedDate;
        if (((1 << i) & 130689) != 0) {
            return getMaximum(i);
        }
        JapaneseImperialCalendar normalizedCalendar = getNormalizedCalendar();
        LocalGregorianCalendar.Date date = normalizedCalendar.jdate;
        date.getNormalizedYear();
        switch (i) {
            case 1:
                LocalGregorianCalendar.Date calendarDate2 = jcal.getCalendarDate(normalizedCalendar.getTimeInMillis(), getZone());
                int eraIndex = getEraIndex(date);
                if (eraIndex == eras.length - 1) {
                    calendarDate = jcal.getCalendarDate(Long.MAX_VALUE, getZone());
                    year = calendarDate.getYear();
                    if (year > 400) {
                        calendarDate2.setYear(year - 400);
                    }
                } else {
                    calendarDate = jcal.getCalendarDate(eras[eraIndex + 1].getSince(getZone()) - 1, getZone());
                    year = calendarDate.getYear();
                    calendarDate2.setYear(year);
                }
                jcal.normalize(calendarDate2);
                if (getYearOffsetInMillis(calendarDate2) > getYearOffsetInMillis(calendarDate)) {
                    year--;
                }
                return year;
            case 2:
                int month = 11;
                if (isTransitionYear(date.getNormalizedYear())) {
                    int eraIndex2 = getEraIndex(date);
                    if (date.getYear() != 1) {
                        eraIndex2++;
                    }
                    long j = sinceFixedDates[eraIndex2];
                    if (normalizedCalendar.cachedFixedDate < j) {
                        LocalGregorianCalendar.Date date2 = (LocalGregorianCalendar.Date) date.clone();
                        jcal.getCalendarDateFromFixedDate(date2, j - 1);
                        month = date2.getMonth() - 1;
                    }
                    return month;
                }
                LocalGregorianCalendar.Date calendarDate3 = jcal.getCalendarDate(Long.MAX_VALUE, getZone());
                if (date.getEra() == calendarDate3.getEra() && date.getYear() == calendarDate3.getYear()) {
                    month = calendarDate3.getMonth() - 1;
                }
                return month;
            case 3:
                if (!isTransitionYear(date.getNormalizedYear())) {
                    LocalGregorianCalendar.Date calendarDate4 = jcal.getCalendarDate(Long.MAX_VALUE, getZone());
                    if (date.getEra() != calendarDate4.getEra() || date.getYear() != calendarDate4.getYear()) {
                        if (date.getEra() == null && date.getYear() == getMinimum(1)) {
                            LocalGregorianCalendar.Date calendarDate5 = jcal.getCalendarDate(Long.MIN_VALUE, getZone());
                            calendarDate5.addYear(HttpURLConnection.HTTP_BAD_REQUEST);
                            jcal.normalize(calendarDate5);
                            calendarDate4.setEra(calendarDate5.getEra());
                            calendarDate4.setDate(calendarDate5.getYear() + 1, 1, 1);
                            jcal.normalize(calendarDate4);
                            long fixedDate2 = jcal.getFixedDate(calendarDate5);
                            long fixedDate3 = jcal.getFixedDate(calendarDate4);
                            long dayOfWeekDateOnOrBefore = LocalGregorianCalendar.getDayOfWeekDateOnOrBefore(6 + fixedDate3, getFirstDayOfWeek());
                            if (((int) (dayOfWeekDateOnOrBefore - fixedDate3)) >= getMinimalDaysInFirstWeek()) {
                                dayOfWeekDateOnOrBefore -= 7;
                            }
                            return getWeekNumber(fixedDate2, dayOfWeekDateOnOrBefore);
                        }
                        Gregorian.Date dateNewCalendarDate = gcal.newCalendarDate(TimeZone.NO_TIMEZONE);
                        dateNewCalendarDate.setDate(date.getNormalizedYear(), 1, 1);
                        int dayOfWeek = gcal.getDayOfWeek(dateNewCalendarDate) - getFirstDayOfWeek();
                        if (dayOfWeek < 0) {
                            dayOfWeek += 7;
                        }
                        int minimalDaysInFirstWeek = (dayOfWeek + getMinimalDaysInFirstWeek()) - 1;
                        if (minimalDaysInFirstWeek != 6 && (!date.isLeapYear() || (minimalDaysInFirstWeek != 5 && minimalDaysInFirstWeek != 12))) {
                            return 52;
                        }
                        return 53;
                    }
                    long fixedDate4 = jcal.getFixedDate(calendarDate4);
                    return getWeekNumber(getFixedDateJan1(calendarDate4, fixedDate4), fixedDate4);
                }
                if (normalizedCalendar == this) {
                    normalizedCalendar = (JapaneseImperialCalendar) normalizedCalendar.clone();
                }
                int actualMaximum = getActualMaximum(6);
                normalizedCalendar.set(6, actualMaximum);
                int i2 = normalizedCalendar.get(3);
                if (i2 == 1 && actualMaximum > 7) {
                    normalizedCalendar.add(3, -1);
                    return normalizedCalendar.get(3);
                }
                return i2;
            case 4:
                LocalGregorianCalendar.Date calendarDate6 = jcal.getCalendarDate(Long.MAX_VALUE, getZone());
                if (date.getEra() != calendarDate6.getEra() || date.getYear() != calendarDate6.getYear()) {
                    Gregorian.Date dateNewCalendarDate2 = gcal.newCalendarDate(TimeZone.NO_TIMEZONE);
                    dateNewCalendarDate2.setDate(date.getNormalizedYear(), date.getMonth(), 1);
                    int dayOfWeek2 = gcal.getDayOfWeek(dateNewCalendarDate2);
                    int monthLength = gcal.getMonthLength(dateNewCalendarDate2);
                    int firstDayOfWeek = dayOfWeek2 - getFirstDayOfWeek();
                    if (firstDayOfWeek < 0) {
                        firstDayOfWeek += 7;
                    }
                    int i3 = 7 - firstDayOfWeek;
                    int i4 = i3 >= getMinimalDaysInFirstWeek() ? 4 : 3;
                    int i5 = monthLength - (i3 + 21);
                    if (i5 > 0) {
                        i4++;
                        if (i5 > 7) {
                            i4++;
                        }
                    }
                    return i4;
                }
                long fixedDate5 = jcal.getFixedDate(calendarDate6);
                return getWeekNumber((fixedDate5 - ((long) calendarDate6.getDayOfMonth())) + 1, fixedDate5);
            case 5:
                return jcal.getMonthLength(date);
            case 6:
                if (isTransitionYear(date.getNormalizedYear())) {
                    int eraIndex3 = getEraIndex(date);
                    if (date.getYear() != 1) {
                        eraIndex3++;
                    }
                    long j2 = sinceFixedDates[eraIndex3];
                    long j3 = normalizedCalendar.cachedFixedDate;
                    Gregorian.Date dateNewCalendarDate3 = gcal.newCalendarDate(TimeZone.NO_TIMEZONE);
                    dateNewCalendarDate3.setDate(date.getNormalizedYear(), 1, 1);
                    if (j3 < j2) {
                        fixedDate = (int) (j2 - gcal.getFixedDate(dateNewCalendarDate3));
                    } else {
                        dateNewCalendarDate3.addYear(1);
                        fixedDate = (int) (gcal.getFixedDate(dateNewCalendarDate3) - j2);
                    }
                    return fixedDate;
                }
                LocalGregorianCalendar.Date calendarDate7 = jcal.getCalendarDate(Long.MAX_VALUE, getZone());
                if (date.getEra() != calendarDate7.getEra() || date.getYear() != calendarDate7.getYear()) {
                    if (date.getYear() == getMinimum(1)) {
                        LocalGregorianCalendar.Date calendarDate8 = jcal.getCalendarDate(Long.MIN_VALUE, getZone());
                        long fixedDate6 = jcal.getFixedDate(calendarDate8);
                        calendarDate8.addYear(1);
                        calendarDate8.setMonth(1).setDayOfMonth(1);
                        jcal.normalize(calendarDate8);
                        yearLength = (int) (jcal.getFixedDate(calendarDate8) - fixedDate6);
                    } else {
                        yearLength = jcal.getYearLength(date);
                    }
                } else {
                    long fixedDate7 = jcal.getFixedDate(calendarDate7);
                    yearLength = ((int) (fixedDate7 - getFixedDateJan1(calendarDate7, fixedDate7))) + 1;
                }
                return yearLength;
            case 7:
            default:
                throw new ArrayIndexOutOfBoundsException(i);
            case 8:
                int dayOfWeek3 = date.getDayOfWeek();
                BaseCalendar.Date date3 = (BaseCalendar.Date) date.clone();
                int monthLength2 = jcal.getMonthLength(date3);
                date3.setDayOfMonth(1);
                jcal.normalize(date3);
                int dayOfWeek4 = dayOfWeek3 - date3.getDayOfWeek();
                if (dayOfWeek4 < 0) {
                    dayOfWeek4 += 7;
                }
                return ((monthLength2 - dayOfWeek4) + 6) / 7;
        }
    }

    private long getYearOffsetInMillis(CalendarDate calendarDate) {
        return (((jcal.getDayOfYear(calendarDate) - 1) * ONE_DAY) + calendarDate.getTimeOfDay()) - ((long) calendarDate.getZoneOffset());
    }

    @Override
    public Object clone() {
        JapaneseImperialCalendar japaneseImperialCalendar = (JapaneseImperialCalendar) super.clone();
        japaneseImperialCalendar.jdate = (LocalGregorianCalendar.Date) this.jdate.clone();
        japaneseImperialCalendar.originalFields = null;
        japaneseImperialCalendar.zoneOffsets = null;
        return japaneseImperialCalendar;
    }

    @Override
    public TimeZone getTimeZone() {
        TimeZone timeZone = super.getTimeZone();
        this.jdate.setZone(timeZone);
        return timeZone;
    }

    @Override
    public void setTimeZone(TimeZone timeZone) {
        super.setTimeZone(timeZone);
        this.jdate.setZone(timeZone);
    }

    @Override
    protected void computeFields() {
        int i = 131071;
        if (isPartiallyNormalized()) {
            int setStateFields = getSetStateFields();
            int i2 = 131071 & (~setStateFields);
            if (i2 != 0 || this.cachedFixedDate == Long.MIN_VALUE) {
                setStateFields |= computeFields(i2, 98304 & setStateFields);
            }
            i = setStateFields;
        } else {
            computeFields(131071, 0);
        }
        setFieldsComputed(i);
    }

    private int computeFields(int i, int i2) {
        int offset;
        int i3;
        int dayOfYear;
        long fixedDate;
        int i4;
        long fixedDate2;
        int weekNumber;
        long fixedDate3;
        TimeZone zone = getZone();
        if (this.zoneOffsets == null) {
            this.zoneOffsets = new int[2];
        }
        if (i2 != 98304) {
            offset = zone.getOffset(this.time);
            this.zoneOffsets[0] = zone.getRawOffset();
            this.zoneOffsets[1] = offset - this.zoneOffsets[0];
        } else {
            offset = 0;
        }
        if (i2 != 0) {
            if (isFieldSet(i2, 15)) {
                this.zoneOffsets[0] = internalGet(15);
            }
            if (isFieldSet(i2, 16)) {
                this.zoneOffsets[1] = internalGet(16);
            }
            offset = this.zoneOffsets[1] + this.zoneOffsets[0];
        }
        long j = (((long) offset) / ONE_DAY) + (this.time / ONE_DAY);
        int i5 = (offset % 86400000) + ((int) (this.time % ONE_DAY));
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
        if (j3 != this.cachedFixedDate || j3 < 0) {
            jcal.getCalendarDateFromFixedDate(this.jdate, j3);
            this.cachedFixedDate = j3;
        }
        int eraIndex = getEraIndex(this.jdate);
        int year = this.jdate.getYear();
        internalSet(0, eraIndex);
        internalSet(1, year);
        int i6 = i | 3;
        int month = this.jdate.getMonth() - 1;
        int dayOfMonth = this.jdate.getDayOfMonth();
        if ((i & 164) != 0) {
            internalSet(2, month);
            internalSet(5, dayOfMonth);
            internalSet(7, this.jdate.getDayOfWeek());
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
            int normalizedYear = this.jdate.getNormalizedYear();
            boolean zIsTransitionYear = isTransitionYear(this.jdate.getNormalizedYear());
            if (zIsTransitionYear) {
                fixedDate = getFixedDateJan1(this.jdate, j3);
                dayOfYear = ((int) (j3 - fixedDate)) + 1;
            } else if (normalizedYear == MIN_VALUES[1]) {
                fixedDate = jcal.getFixedDate(jcal.getCalendarDate(Long.MIN_VALUE, getZone()));
                dayOfYear = ((int) (j3 - fixedDate)) + 1;
            } else {
                dayOfYear = (int) jcal.getDayOfYear(this.jdate);
                fixedDate = (j3 - ((long) dayOfYear)) + 1;
            }
            long fixedDateMonth1 = zIsTransitionYear ? getFixedDateMonth1(this.jdate, j3) : (j3 - ((long) dayOfMonth)) + 1;
            internalSet(6, dayOfYear);
            internalSet(8, ((dayOfMonth - 1) / 7) + 1);
            int weekNumber2 = getWeekNumber(fixedDate, j3);
            if (weekNumber2 == 0) {
                long j4 = fixedDate - 1;
                LocalGregorianCalendar.Date calendarDate = getCalendarDate(j4);
                if (!zIsTransitionYear && !isTransitionYear(calendarDate.getNormalizedYear())) {
                    fixedDate3 = fixedDate - 365;
                    if (calendarDate.isLeapYear()) {
                        fixedDate3--;
                    }
                } else if (zIsTransitionYear) {
                    if (this.jdate.getYear() == 1) {
                        if (eraIndex > 4) {
                            CalendarDate sinceDate = eras[eraIndex - 1].getSinceDate();
                            if (normalizedYear == sinceDate.getYear()) {
                                calendarDate.setMonth(sinceDate.getMonth()).setDayOfMonth(sinceDate.getDayOfMonth());
                            }
                        } else {
                            calendarDate.setMonth(1).setDayOfMonth(1);
                        }
                        jcal.normalize(calendarDate);
                        fixedDate3 = jcal.getFixedDate(calendarDate);
                    } else {
                        fixedDate3 = fixedDate - 365;
                        if (calendarDate.isLeapYear()) {
                            fixedDate3--;
                        }
                    }
                } else {
                    CalendarDate sinceDate2 = eras[getEraIndex(this.jdate)].getSinceDate();
                    calendarDate.setMonth(sinceDate2.getMonth()).setDayOfMonth(sinceDate2.getDayOfMonth());
                    jcal.normalize(calendarDate);
                    fixedDate3 = jcal.getFixedDate(calendarDate);
                }
                weekNumber = getWeekNumber(fixedDate3, j4);
            } else if (!zIsTransitionYear) {
                if (weekNumber2 >= 52) {
                    long j5 = fixedDate + 365;
                    if (this.jdate.isLeapYear()) {
                        j5++;
                    }
                    long dayOfWeekDateOnOrBefore = LocalGregorianCalendar.getDayOfWeekDateOnOrBefore(6 + j5, getFirstDayOfWeek());
                    weekNumber = (((int) (dayOfWeekDateOnOrBefore - j5)) < getMinimalDaysInFirstWeek() || j3 < dayOfWeekDateOnOrBefore - 7) ? weekNumber2 : 1;
                } else {
                    weekNumber = weekNumber2;
                }
            } else {
                LocalGregorianCalendar.Date date = (LocalGregorianCalendar.Date) this.jdate.clone();
                if (this.jdate.getYear() == 1) {
                    date.addYear(1);
                    date.setMonth(1).setDayOfMonth(1);
                    fixedDate2 = jcal.getFixedDate(date);
                    i4 = 1;
                } else {
                    int eraIndex2 = getEraIndex(date) + 1;
                    CalendarDate sinceDate3 = eras[eraIndex2].getSinceDate();
                    date.setEra(eras[eraIndex2]);
                    i4 = 1;
                    date.setDate(1, sinceDate3.getMonth(), sinceDate3.getDayOfMonth());
                    jcal.normalize(date);
                    fixedDate2 = jcal.getFixedDate(date);
                }
                long dayOfWeekDateOnOrBefore2 = LocalGregorianCalendar.getDayOfWeekDateOnOrBefore(6 + fixedDate2, getFirstDayOfWeek());
                if (((int) (dayOfWeekDateOnOrBefore2 - fixedDate2)) >= getMinimalDaysInFirstWeek() && j3 >= dayOfWeekDateOnOrBefore2 - 7) {
                    weekNumber = i4;
                }
            }
            internalSet(3, weekNumber);
            internalSet(4, getWeekNumber(fixedDateMonth1, j3));
            return i6 | 344;
        }
        return i6;
    }

    private int getWeekNumber(long j, long j2) {
        long dayOfWeekDateOnOrBefore = LocalGregorianCalendar.getDayOfWeekDateOnOrBefore(6 + j, getFirstDayOfWeek());
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
        int length;
        int iInternalGet;
        long jInternalGet;
        if (!isLenient()) {
            if (this.originalFields == null) {
                this.originalFields = new int[17];
            }
            for (int i = 0; i < 17; i++) {
                int iInternalGet2 = internalGet(i);
                if (isExternallySet(i) && (iInternalGet2 < getMinimum(i) || iInternalGet2 > getMaximum(i))) {
                    throw new IllegalArgumentException(getFieldName(i));
                }
                this.originalFields[i] = iInternalGet2;
            }
        }
        int iSelectFields = selectFields();
        if (isSet(0)) {
            length = internalGet(0);
            if (isSet(1)) {
                iInternalGet = internalGet(1);
            } else {
                iInternalGet = 1;
            }
        } else if (isSet(1)) {
            length = eras.length - 1;
            iInternalGet = internalGet(1);
        } else {
            length = 3;
            iInternalGet = 45;
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
        long fixedDate = (((j + getFixedDate(length, iInternalGet, iSelectFields)) - 719163) * ONE_DAY) + j2;
        TimeZone zone = getZone();
        if (this.zoneOffsets == null) {
            this.zoneOffsets = new int[2];
        }
        int i2 = iSelectFields & 98304;
        if (i2 != 98304) {
            zone.getOffsets(fixedDate - ((long) zone.getRawOffset()), this.zoneOffsets);
        }
        if (i2 != 0) {
            if (isFieldSet(i2, 15)) {
                this.zoneOffsets[0] = internalGet(15);
            }
            if (isFieldSet(i2, 16)) {
                this.zoneOffsets[1] = internalGet(16);
            }
        }
        this.time = fixedDate - ((long) (this.zoneOffsets[0] + this.zoneOffsets[1]));
        int iComputeFields = computeFields(iSelectFields | getSetStateFields(), i2);
        if (!isLenient()) {
            for (int i3 = 0; i3 < 17; i3++) {
                if (isExternallySet(i3) && this.originalFields[i3] != internalGet(i3)) {
                    int iInternalGet3 = internalGet(i3);
                    System.arraycopy((Object) this.originalFields, 0, (Object) this.fields, 0, this.fields.length);
                    throw new IllegalArgumentException(getFieldName(i3) + "=" + iInternalGet3 + ", expected " + this.originalFields[i3]);
                }
            }
        }
        setFieldsNormalized(iComputeFields);
    }

    private long getFixedDate(int i, int i2, int i3) {
        int dayOfMonth;
        int iInternalGet;
        int firstDayOfWeek;
        int iInternalGet2;
        long dayOfWeekDateOnOrBefore;
        int iFloorDivide = i2;
        int month = 0;
        if (isFieldSet(i3, 2)) {
            int iInternalGet3 = internalGet(2);
            if (iInternalGet3 > 11) {
                iFloorDivide += iInternalGet3 / 12;
                month = iInternalGet3 % 12;
            } else if (iInternalGet3 < 0) {
                int[] iArr = new int[1];
                iFloorDivide += CalendarUtils.floorDivide(iInternalGet3, 12, iArr);
                month = iArr[0];
            } else {
                month = iInternalGet3;
            }
        } else {
            if (iFloorDivide == 1 && i != 0) {
                CalendarDate sinceDate = eras[i].getSinceDate();
                month = sinceDate.getMonth() - 1;
                dayOfMonth = sinceDate.getDayOfMonth();
            }
            if (iFloorDivide == MIN_VALUES[1]) {
                LocalGregorianCalendar.Date calendarDate = jcal.getCalendarDate(Long.MIN_VALUE, getZone());
                int month2 = calendarDate.getMonth() - 1;
                if (month < month2) {
                    month = month2;
                }
                if (month == month2) {
                    dayOfMonth = calendarDate.getDayOfMonth();
                }
            }
            LocalGregorianCalendar.Date dateNewCalendarDate = jcal.newCalendarDate(TimeZone.NO_TIMEZONE);
            dateNewCalendarDate.setEra(i <= 0 ? eras[i] : null);
            dateNewCalendarDate.setDate(iFloorDivide, month + 1, dayOfMonth);
            jcal.normalize(dateNewCalendarDate);
            long fixedDate = jcal.getFixedDate(dateNewCalendarDate);
            if (!isFieldSet(i3, 2)) {
                if (!isFieldSet(i3, 5)) {
                    if (isFieldSet(i3, 4)) {
                        long dayOfWeekDateOnOrBefore2 = LocalGregorianCalendar.getDayOfWeekDateOnOrBefore(fixedDate + 6, getFirstDayOfWeek());
                        if (dayOfWeekDateOnOrBefore2 - fixedDate >= getMinimalDaysInFirstWeek()) {
                            dayOfWeekDateOnOrBefore2 -= 7;
                        }
                        if (isFieldSet(i3, 7)) {
                            dayOfWeekDateOnOrBefore2 = LocalGregorianCalendar.getDayOfWeekDateOnOrBefore(dayOfWeekDateOnOrBefore2 + 6, internalGet(7));
                        }
                        return dayOfWeekDateOnOrBefore2 + ((long) (7 * (internalGet(4) - 1)));
                    }
                    if (isFieldSet(i3, 7)) {
                        firstDayOfWeek = internalGet(7);
                    } else {
                        firstDayOfWeek = getFirstDayOfWeek();
                    }
                    if (isFieldSet(i3, 8)) {
                        iInternalGet2 = internalGet(8);
                    } else {
                        iInternalGet2 = 1;
                    }
                    if (iInternalGet2 >= 0) {
                        dayOfWeekDateOnOrBefore = LocalGregorianCalendar.getDayOfWeekDateOnOrBefore((fixedDate + ((long) (7 * iInternalGet2))) - 1, firstDayOfWeek);
                    } else {
                        dayOfWeekDateOnOrBefore = LocalGregorianCalendar.getDayOfWeekDateOnOrBefore((fixedDate + ((long) (monthLength(month, iFloorDivide) + (7 * (iInternalGet2 + 1))))) - 1, firstDayOfWeek);
                    }
                    return dayOfWeekDateOnOrBefore;
                }
                if (isSet(5)) {
                    return (fixedDate + ((long) internalGet(5))) - ((long) dayOfMonth);
                }
                return fixedDate;
            }
            if (isFieldSet(i3, 6)) {
                if (isTransitionYear(dateNewCalendarDate.getNormalizedYear())) {
                    fixedDate = getFixedDateJan1(dateNewCalendarDate, fixedDate);
                }
                return (fixedDate + ((long) internalGet(6))) - 1;
            }
            long dayOfWeekDateOnOrBefore3 = LocalGregorianCalendar.getDayOfWeekDateOnOrBefore(fixedDate + 6, getFirstDayOfWeek());
            if (dayOfWeekDateOnOrBefore3 - fixedDate >= getMinimalDaysInFirstWeek()) {
                dayOfWeekDateOnOrBefore3 -= 7;
            }
            if (isFieldSet(i3, 7) && (iInternalGet = internalGet(7)) != getFirstDayOfWeek()) {
                dayOfWeekDateOnOrBefore3 = LocalGregorianCalendar.getDayOfWeekDateOnOrBefore(dayOfWeekDateOnOrBefore3 + 6, iInternalGet);
            }
            return dayOfWeekDateOnOrBefore3 + (7 * (((long) internalGet(3)) - 1));
        }
        dayOfMonth = 1;
        if (iFloorDivide == MIN_VALUES[1]) {
        }
        LocalGregorianCalendar.Date dateNewCalendarDate2 = jcal.newCalendarDate(TimeZone.NO_TIMEZONE);
        dateNewCalendarDate2.setEra(i <= 0 ? eras[i] : null);
        dateNewCalendarDate2.setDate(iFloorDivide, month + 1, dayOfMonth);
        jcal.normalize(dateNewCalendarDate2);
        long fixedDate2 = jcal.getFixedDate(dateNewCalendarDate2);
        if (!isFieldSet(i3, 2)) {
        }
    }

    private long getFixedDateJan1(LocalGregorianCalendar.Date date, long j) {
        date.getEra();
        if (date.getEra() != null && date.getYear() == 1) {
            for (int eraIndex = getEraIndex(date); eraIndex > 0; eraIndex--) {
                long fixedDate = gcal.getFixedDate(eras[eraIndex].getSinceDate());
                if (fixedDate <= j) {
                    return fixedDate;
                }
            }
        }
        Gregorian.Date dateNewCalendarDate = gcal.newCalendarDate(TimeZone.NO_TIMEZONE);
        dateNewCalendarDate.setDate(date.getNormalizedYear(), 1, 1);
        return gcal.getFixedDate(dateNewCalendarDate);
    }

    private long getFixedDateMonth1(LocalGregorianCalendar.Date date, long j) {
        int transitionEraIndex = getTransitionEraIndex(date);
        if (transitionEraIndex != -1) {
            long j2 = sinceFixedDates[transitionEraIndex];
            if (j2 <= j) {
                return j2;
            }
        }
        return (j - ((long) date.getDayOfMonth())) + 1;
    }

    private static LocalGregorianCalendar.Date getCalendarDate(long j) {
        LocalGregorianCalendar.Date dateNewCalendarDate = jcal.newCalendarDate(TimeZone.NO_TIMEZONE);
        jcal.getCalendarDateFromFixedDate(dateNewCalendarDate, j);
        return dateNewCalendarDate;
    }

    private int monthLength(int i, int i2) {
        return CalendarUtils.isGregorianLeapYear(i2) ? GregorianCalendar.LEAP_MONTH_LENGTH[i] : GregorianCalendar.MONTH_LENGTH[i];
    }

    private int monthLength(int i) {
        return this.jdate.isLeapYear() ? GregorianCalendar.LEAP_MONTH_LENGTH[i] : GregorianCalendar.MONTH_LENGTH[i];
    }

    private int actualMonthLength() {
        int monthLength = jcal.getMonthLength(this.jdate);
        int transitionEraIndex = getTransitionEraIndex(this.jdate);
        if (transitionEraIndex == -1) {
            long j = sinceFixedDates[transitionEraIndex];
            CalendarDate sinceDate = eras[transitionEraIndex].getSinceDate();
            if (j <= this.cachedFixedDate) {
                return monthLength - (sinceDate.getDayOfMonth() - 1);
            }
            return sinceDate.getDayOfMonth() - 1;
        }
        return monthLength;
    }

    private static int getTransitionEraIndex(LocalGregorianCalendar.Date date) {
        int eraIndex = getEraIndex(date);
        CalendarDate sinceDate = eras[eraIndex].getSinceDate();
        if (sinceDate.getYear() == date.getNormalizedYear() && sinceDate.getMonth() == date.getMonth()) {
            return eraIndex;
        }
        if (eraIndex < eras.length - 1) {
            int i = eraIndex + 1;
            CalendarDate sinceDate2 = eras[i].getSinceDate();
            if (sinceDate2.getYear() == date.getNormalizedYear() && sinceDate2.getMonth() == date.getMonth()) {
                return i;
            }
            return -1;
        }
        return -1;
    }

    private boolean isTransitionYear(int i) {
        for (int length = eras.length - 1; length > 0; length--) {
            int year = eras[length].getSinceDate().getYear();
            if (i == year) {
                return true;
            }
            if (i > year) {
                return $assertionsDisabled;
            }
        }
        return $assertionsDisabled;
    }

    private static int getEraIndex(LocalGregorianCalendar.Date date) {
        Era era = date.getEra();
        for (int length = eras.length - 1; length > 0; length--) {
            if (eras[length] == era) {
                return length;
            }
        }
        return 0;
    }

    private JapaneseImperialCalendar getNormalizedCalendar() {
        if (!isFullyNormalized()) {
            JapaneseImperialCalendar japaneseImperialCalendar = (JapaneseImperialCalendar) clone();
            japaneseImperialCalendar.setLenient(true);
            japaneseImperialCalendar.complete();
            return japaneseImperialCalendar;
        }
        return this;
    }

    private void pinDayOfMonth(LocalGregorianCalendar.Date date) {
        int year = date.getYear();
        int dayOfMonth = date.getDayOfMonth();
        if (year != getMinimum(1)) {
            date.setDayOfMonth(1);
            jcal.normalize(date);
            int monthLength = jcal.getMonthLength(date);
            if (dayOfMonth > monthLength) {
                date.setDayOfMonth(monthLength);
            } else {
                date.setDayOfMonth(dayOfMonth);
            }
            jcal.normalize(date);
            return;
        }
        LocalGregorianCalendar.Date calendarDate = jcal.getCalendarDate(Long.MIN_VALUE, getZone());
        LocalGregorianCalendar.Date calendarDate2 = jcal.getCalendarDate(this.time, getZone());
        long timeOfDay = calendarDate2.getTimeOfDay();
        calendarDate2.addYear(HttpURLConnection.HTTP_BAD_REQUEST);
        calendarDate2.setMonth(date.getMonth());
        calendarDate2.setDayOfMonth(1);
        jcal.normalize(calendarDate2);
        int monthLength2 = jcal.getMonthLength(calendarDate2);
        if (dayOfMonth > monthLength2) {
            calendarDate2.setDayOfMonth(monthLength2);
        } else if (dayOfMonth < calendarDate.getDayOfMonth()) {
            calendarDate2.setDayOfMonth(calendarDate.getDayOfMonth());
        } else {
            calendarDate2.setDayOfMonth(dayOfMonth);
        }
        if (calendarDate2.getDayOfMonth() == calendarDate.getDayOfMonth() && timeOfDay < calendarDate.getTimeOfDay()) {
            calendarDate2.setDayOfMonth(Math.min(dayOfMonth + 1, monthLength2));
        }
        date.setDate(year, calendarDate2.getMonth(), calendarDate2.getDayOfMonth());
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
        return isSet(0) ? internalGet(0) : eras.length - 1;
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        if (this.jdate == null) {
            this.jdate = jcal.newCalendarDate(getZone());
            this.cachedFixedDate = Long.MIN_VALUE;
        }
    }
}
