package android.app.admin;

import android.app.admin.SystemUpdatePolicy;
import android.util.Log;
import android.util.Pair;
import com.android.internal.content.NativeLibraryHelper;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FreezePeriod {
    static final int DAYS_IN_YEAR = 365;
    private static final int DUMMY_YEAR = 2001;
    private static final String TAG = "FreezePeriod";
    private final MonthDay mEnd;
    private final int mEndDay;
    private final MonthDay mStart;
    private final int mStartDay;

    public FreezePeriod(MonthDay monthDay, MonthDay monthDay2) {
        this.mStart = monthDay;
        this.mStartDay = this.mStart.atYear(2001).getDayOfYear();
        this.mEnd = monthDay2;
        this.mEndDay = this.mEnd.atYear(2001).getDayOfYear();
    }

    public MonthDay getStart() {
        return this.mStart;
    }

    public MonthDay getEnd() {
        return this.mEnd;
    }

    private FreezePeriod(int i, int i2) {
        this.mStartDay = i;
        this.mStart = dayOfYearToMonthDay(i);
        this.mEndDay = i2;
        this.mEnd = dayOfYearToMonthDay(i2);
    }

    int getLength() {
        return (getEffectiveEndDay() - this.mStartDay) + 1;
    }

    boolean isWrapped() {
        return this.mEndDay < this.mStartDay;
    }

    int getEffectiveEndDay() {
        if (!isWrapped()) {
            return this.mEndDay;
        }
        return this.mEndDay + 365;
    }

    boolean contains(LocalDate localDate) {
        int iDayOfYearDisregardLeapYear = dayOfYearDisregardLeapYear(localDate);
        return !isWrapped() ? this.mStartDay <= iDayOfYearDisregardLeapYear && iDayOfYearDisregardLeapYear <= this.mEndDay : this.mStartDay <= iDayOfYearDisregardLeapYear || iDayOfYearDisregardLeapYear <= this.mEndDay;
    }

    boolean after(LocalDate localDate) {
        return this.mStartDay > dayOfYearDisregardLeapYear(localDate);
    }

    Pair<LocalDate, LocalDate> toCurrentOrFutureRealDates(LocalDate localDate) {
        boolean zIsWrapped;
        int iDayOfYearDisregardLeapYear = dayOfYearDisregardLeapYear(localDate);
        int i = 1;
        ?? r3 = 0;
        if (contains(localDate)) {
            if (this.mStartDay <= iDayOfYearDisregardLeapYear) {
                zIsWrapped = isWrapped();
                r3 = zIsWrapped;
                i = 0;
            } else {
                i = -1;
            }
        } else if (this.mStartDay > iDayOfYearDisregardLeapYear) {
            zIsWrapped = isWrapped();
            r3 = zIsWrapped;
            i = 0;
        } else {
            r3 = 1;
        }
        return new Pair<>(LocalDate.ofYearDay(2001, this.mStartDay).withYear(localDate.getYear() + i), LocalDate.ofYearDay(2001, this.mEndDay).withYear(localDate.getYear() + r3));
    }

    public String toString() {
        DateTimeFormatter dateTimeFormatterOfPattern = DateTimeFormatter.ofPattern("MMM dd");
        return LocalDate.ofYearDay(2001, this.mStartDay).format(dateTimeFormatterOfPattern) + " - " + LocalDate.ofYearDay(2001, this.mEndDay).format(dateTimeFormatterOfPattern);
    }

    private static MonthDay dayOfYearToMonthDay(int i) {
        LocalDate localDateOfYearDay = LocalDate.ofYearDay(2001, i);
        return MonthDay.of(localDateOfYearDay.getMonth(), localDateOfYearDay.getDayOfMonth());
    }

    private static int dayOfYearDisregardLeapYear(LocalDate localDate) {
        return localDate.withYear(2001).getDayOfYear();
    }

    public static int distanceWithoutLeapYear(LocalDate localDate, LocalDate localDate2) {
        return (dayOfYearDisregardLeapYear(localDate) - dayOfYearDisregardLeapYear(localDate2)) + (365 * (localDate.getYear() - localDate2.getYear()));
    }

    static List<FreezePeriod> canonicalizePeriods(List<FreezePeriod> list) {
        boolean[] zArr = new boolean[365];
        for (FreezePeriod freezePeriod : list) {
            for (int i = freezePeriod.mStartDay; i <= freezePeriod.getEffectiveEndDay(); i++) {
                zArr[(i - 1) % 365] = true;
            }
        }
        ArrayList arrayList = new ArrayList();
        int i2 = 0;
        while (i2 < 365) {
            if (!zArr[i2]) {
                i2++;
            } else {
                int i3 = i2 + 1;
                while (i2 < 365 && zArr[i2]) {
                    i2++;
                }
                arrayList.add(new FreezePeriod(i3, i2));
            }
        }
        int size = arrayList.size() - 1;
        if (size > 0 && ((FreezePeriod) arrayList.get(size)).mEndDay == 365 && ((FreezePeriod) arrayList.get(0)).mStartDay == 1) {
            arrayList.set(size, new FreezePeriod(((FreezePeriod) arrayList.get(size)).mStartDay, ((FreezePeriod) arrayList.get(0)).mEndDay));
            arrayList.remove(0);
        }
        return arrayList;
    }

    static void validatePeriods(List<FreezePeriod> list) {
        int i;
        List<FreezePeriod> listCanonicalizePeriods = canonicalizePeriods(list);
        if (listCanonicalizePeriods.size() != list.size()) {
            throw SystemUpdatePolicy.ValidationFailedException.duplicateOrOverlapPeriods();
        }
        int i2 = 0;
        while (i2 < listCanonicalizePeriods.size()) {
            FreezePeriod freezePeriod = listCanonicalizePeriods.get(i2);
            if (freezePeriod.getLength() > 90) {
                throw SystemUpdatePolicy.ValidationFailedException.freezePeriodTooLong("Freeze period " + freezePeriod + " is too long: " + freezePeriod.getLength() + " days");
            }
            FreezePeriod freezePeriod2 = i2 > 0 ? listCanonicalizePeriods.get(i2 - 1) : listCanonicalizePeriods.get(listCanonicalizePeriods.size() - 1);
            if (freezePeriod2 != freezePeriod) {
                if (i2 == 0 && !freezePeriod2.isWrapped()) {
                    i = (freezePeriod.mStartDay + (365 - freezePeriod2.mEndDay)) - 1;
                } else {
                    i = (freezePeriod.mStartDay - freezePeriod2.mEndDay) - 1;
                }
                if (i < 60) {
                    throw SystemUpdatePolicy.ValidationFailedException.freezePeriodTooClose("Freeze periods " + freezePeriod2 + " and " + freezePeriod + " are too close together: " + i + " days apart");
                }
            }
            i2++;
        }
    }

    static void validateAgainstPreviousFreezePeriod(List<FreezePeriod> list, LocalDate localDate, LocalDate localDate2, LocalDate localDate3) {
        if (list.size() == 0 || localDate == null || localDate2 == null) {
            return;
        }
        if (localDate.isAfter(localDate3) || localDate2.isAfter(localDate3)) {
            Log.w(TAG, "Previous period (" + localDate + "," + localDate2 + ") is after current date " + localDate3);
        }
        List<FreezePeriod> listCanonicalizePeriods = canonicalizePeriods(list);
        FreezePeriod freezePeriod = listCanonicalizePeriods.get(0);
        for (FreezePeriod freezePeriod2 : listCanonicalizePeriods) {
            if (freezePeriod2.contains(localDate3) || freezePeriod2.mStartDay > dayOfYearDisregardLeapYear(localDate3)) {
                freezePeriod = freezePeriod2;
                break;
            }
        }
        Pair<LocalDate, LocalDate> currentOrFutureRealDates = freezePeriod.toCurrentOrFutureRealDates(localDate3);
        if (localDate3.isAfter(currentOrFutureRealDates.first)) {
            currentOrFutureRealDates = new Pair<>(localDate3, currentOrFutureRealDates.second);
        }
        if (currentOrFutureRealDates.first.isAfter(currentOrFutureRealDates.second)) {
            throw new IllegalStateException("Current freeze dates inverted: " + currentOrFutureRealDates.first + NativeLibraryHelper.CLEAR_ABI_OVERRIDE + currentOrFutureRealDates.second);
        }
        String str = "Prev: " + localDate + "," + localDate2 + "; cur: " + currentOrFutureRealDates.first + "," + currentOrFutureRealDates.second;
        long jDistanceWithoutLeapYear = distanceWithoutLeapYear(currentOrFutureRealDates.first, localDate2) - 1;
        if (jDistanceWithoutLeapYear > 0) {
            if (jDistanceWithoutLeapYear < 60) {
                throw SystemUpdatePolicy.ValidationFailedException.combinedPeriodTooClose("Previous freeze period too close to new period: " + jDistanceWithoutLeapYear + ", " + str);
            }
            return;
        }
        long jDistanceWithoutLeapYear2 = distanceWithoutLeapYear(currentOrFutureRealDates.second, localDate) + 1;
        if (jDistanceWithoutLeapYear2 > 90) {
            throw SystemUpdatePolicy.ValidationFailedException.combinedPeriodTooLong("Combined freeze period exceeds maximum days: " + jDistanceWithoutLeapYear2 + ", " + str);
        }
    }
}
