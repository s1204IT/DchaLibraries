package com.android.calendarcommon2;

import android.text.format.Time;
import android.util.Log;
import java.util.TreeSet;

public class RecurrenceProcessor {
    private static final int[] DAYS_PER_MONTH = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    private static final int[] DAYS_IN_YEAR_PRECEDING_MONTH = {0, 31, 59, 90, 120, 151, 180, 212, 243, 273, 304, 334};
    private Time mIterator = new Time("UTC");
    private Time mUntil = new Time("UTC");
    private StringBuilder mStringBuilder = new StringBuilder();
    private Time mGenerated = new Time("UTC");
    private DaySet mDays = new DaySet(false);

    public long getLastOccurence(Time dtstart, RecurrenceSet recur) throws DateException {
        return getLastOccurence(dtstart, null, recur);
    }

    public long getLastOccurence(Time dtstart, Time maxtime, RecurrenceSet recur) throws DateException {
        long lastTime = -1;
        boolean hasCount = false;
        if (recur.rrules != null) {
            EventRecurrence[] arr$ = recur.rrules;
            for (EventRecurrence rrule : arr$) {
                if (rrule.count != 0) {
                    hasCount = true;
                } else if (rrule.until != null) {
                    this.mIterator.parse(rrule.until);
                    long untilTime = this.mIterator.toMillis(false);
                    if (untilTime > lastTime) {
                        lastTime = untilTime;
                    }
                }
            }
            if (lastTime != -1 && recur.rdates != null) {
                long[] arr$2 = recur.rdates;
                for (long dt : arr$2) {
                    if (dt > lastTime) {
                        lastTime = dt;
                    }
                }
            }
            if (lastTime != -1 && !hasCount) {
                return lastTime;
            }
        } else if (recur.rdates != null && recur.exrules == null && recur.exdates == null) {
            long[] arr$3 = recur.rdates;
            for (long dt2 : arr$3) {
                if (dt2 > lastTime) {
                    lastTime = dt2;
                }
            }
            return lastTime;
        }
        if (hasCount || recur.rdates != null || maxtime != null) {
            long[] dates = expand(dtstart, recur, dtstart.toMillis(false), maxtime != null ? maxtime.toMillis(false) : -1L);
            if (dates.length == 0) {
                return 0L;
            }
            return dates[dates.length - 1];
        }
        return -1L;
    }

    private static boolean listContains(int[] a, int N, int v) {
        for (int i = 0; i < N; i++) {
            if (a[i] == v) {
                return true;
            }
        }
        return false;
    }

    private static boolean listContains(int[] a, int N, int v, int max) {
        for (int i = 0; i < N; i++) {
            int w = a[i];
            if (w > 0) {
                if (w == v) {
                    return true;
                }
            } else {
                max += w;
                if (max == v) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int filter(EventRecurrence r, Time iterator) {
        int freq = r.freq;
        if (6 >= freq && r.bymonthCount > 0) {
            boolean found = listContains(r.bymonth, r.bymonthCount, iterator.month + 1);
            if (!found) {
                return 1;
            }
        }
        if (5 >= freq && r.byweeknoCount > 0) {
            boolean found2 = listContains(r.byweekno, r.byweeknoCount, iterator.getWeekNumber(), iterator.getActualMaximum(9));
            if (!found2) {
                return 2;
            }
        }
        if (4 >= freq) {
            if (r.byyeardayCount > 0) {
                boolean found3 = listContains(r.byyearday, r.byyeardayCount, iterator.yearDay, iterator.getActualMaximum(8));
                if (!found3) {
                    return 3;
                }
            }
            if (r.bymonthdayCount > 0) {
                boolean found4 = listContains(r.bymonthday, r.bymonthdayCount, iterator.monthDay, iterator.getActualMaximum(4));
                if (!found4) {
                    return 4;
                }
            }
            if (r.bydayCount > 0) {
                int[] a = r.byday;
                int N = r.bydayCount;
                int v = EventRecurrence.timeDay2Day(iterator.weekDay);
                for (int i = 0; i < N; i++) {
                    if (a[i] != v) {
                    }
                }
                return 5;
            }
        }
        if (3 >= freq) {
            boolean found5 = listContains(r.byhour, r.byhourCount, iterator.hour, iterator.getActualMaximum(3));
            if (!found5) {
                return 6;
            }
        }
        if (2 >= freq) {
            boolean found6 = listContains(r.byminute, r.byminuteCount, iterator.minute, iterator.getActualMaximum(2));
            if (!found6) {
                return 7;
            }
        }
        if (1 >= freq) {
            boolean found7 = listContains(r.bysecond, r.bysecondCount, iterator.second, iterator.getActualMaximum(1));
            if (!found7) {
                return 8;
            }
        }
        if (r.bysetposCount > 0) {
            if (freq == 6 && r.bydayCount > 0) {
                int i2 = r.bydayCount - 1;
                while (true) {
                    if (i2 >= 0) {
                        if (r.bydayNum[i2] == 0) {
                            i2--;
                        } else if (Log.isLoggable("RecurrenceProcessor", 2)) {
                            Log.v("RecurrenceProcessor", "BYSETPOS not supported with these rules: " + r);
                        }
                    } else if (!filterMonthlySetPos(r, iterator)) {
                        return 9;
                    }
                }
            } else if (Log.isLoggable("RecurrenceProcessor", 2)) {
                Log.v("RecurrenceProcessor", "BYSETPOS not supported with these rules: " + r);
            }
        }
        return 0;
    }

    private static boolean filterMonthlySetPos(EventRecurrence r, Time instance) {
        int daySetLength;
        int dotw = ((instance.weekDay - instance.monthDay) + 36) % 7;
        int bydayMask = 0;
        for (int i = 0; i < r.bydayCount; i++) {
            bydayMask |= r.byday[i];
        }
        int maxDay = instance.getActualMaximum(4);
        int[] daySet = new int[maxDay];
        int md = 1;
        int daySetLength2 = 0;
        while (md <= maxDay) {
            int dayBit = 65536 << dotw;
            if ((bydayMask & dayBit) != 0) {
                daySetLength = daySetLength2 + 1;
                daySet[daySetLength2] = md;
            } else {
                daySetLength = daySetLength2;
            }
            dotw++;
            if (dotw == 7) {
                dotw = 0;
            }
            md++;
            daySetLength2 = daySetLength;
        }
        for (int i2 = r.bysetposCount - 1; i2 >= 0; i2--) {
            int index = r.bysetpos[i2];
            if (index > 0) {
                if (index <= daySetLength2 && daySet[index - 1] == instance.monthDay) {
                    return true;
                }
            } else if (index < 0) {
                if (daySetLength2 + index >= 0 && daySet[daySetLength2 + index] == instance.monthDay) {
                    return true;
                }
            } else {
                throw new RuntimeException("invalid bysetpos value");
            }
        }
        return false;
    }

    private static boolean useBYX(int freq, int freqConstant, int count) {
        return freq > freqConstant && count > 0;
    }

    public static class DaySet {
        private int mDays;
        private int mMonth;
        private EventRecurrence mR;
        private Time mTime = new Time("UTC");
        private int mYear;

        public DaySet(boolean zulu) {
        }

        void setRecurrence(EventRecurrence r) {
            this.mYear = 0;
            this.mMonth = -1;
            this.mR = r;
        }

        boolean get(Time iterator, int day) {
            int realYear = iterator.year;
            int realMonth = iterator.month;
            Time t = null;
            if (day < 1 || day > 28) {
                t = this.mTime;
                t.set(day, realMonth, realYear);
                RecurrenceProcessor.unsafeNormalize(t);
                realYear = t.year;
                realMonth = t.month;
                day = t.monthDay;
            }
            if (realYear != this.mYear || realMonth != this.mMonth) {
                if (t == null) {
                    t = this.mTime;
                    t.set(day, realMonth, realYear);
                    RecurrenceProcessor.unsafeNormalize(t);
                }
                this.mYear = realYear;
                this.mMonth = realMonth;
                this.mDays = generateDaysList(t, this.mR);
            }
            return (this.mDays & (1 << day)) != 0;
        }

        private static int generateDaysList(Time generated, EventRecurrence r) {
            int count;
            int first;
            int days = 0;
            int lastDayThisMonth = generated.getActualMaximum(4);
            int count2 = r.bydayCount;
            if (count2 > 0) {
                int j = generated.monthDay;
                while (j >= 8) {
                    j -= 7;
                }
                int first2 = generated.weekDay;
                if (first2 >= j) {
                    first = (first2 - j) + 1;
                } else {
                    first = (first2 - j) + 8;
                }
                int[] byday = r.byday;
                int[] bydayNum = r.bydayNum;
                for (int i = 0; i < count2; i++) {
                    int v = bydayNum[i];
                    int j2 = (EventRecurrence.day2TimeDay(byday[i]) - first) + 1;
                    if (j2 <= 0) {
                        j2 += 7;
                    }
                    if (v == 0) {
                        while (j2 <= lastDayThisMonth) {
                            days |= 1 << j2;
                            j2 += 7;
                        }
                    } else if (v > 0) {
                        int j3 = j2 + ((v - 1) * 7);
                        if (j3 <= lastDayThisMonth) {
                            days |= 1 << j3;
                        }
                    } else {
                        while (j2 <= lastDayThisMonth) {
                            j2 += 7;
                        }
                        int j4 = j2 + (v * 7);
                        if (j4 >= 1) {
                            days |= 1 << j4;
                        }
                    }
                }
            }
            if (r.freq > 5 && (count = r.bymonthdayCount) != 0) {
                int[] bymonthday = r.bymonthday;
                if (r.bydayCount == 0) {
                    for (int i2 = 0; i2 < count; i2++) {
                        int v2 = bymonthday[i2];
                        if (v2 >= 0) {
                            days |= 1 << v2;
                        } else {
                            int j5 = lastDayThisMonth + v2 + 1;
                            if (j5 >= 1 && j5 <= lastDayThisMonth) {
                                days |= 1 << j5;
                            }
                        }
                    }
                } else {
                    for (int j6 = 1; j6 <= lastDayThisMonth; j6++) {
                        if (((1 << j6) & days) != 0) {
                            int i3 = 0;
                            while (true) {
                                if (i3 < count) {
                                    if (bymonthday[i3] == j6) {
                                        break;
                                    }
                                    i3++;
                                } else {
                                    days &= (1 << j6) ^ (-1);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            return days;
        }
    }

    public long[] expand(Time dtstart, RecurrenceSet recur, long rangeStartMillis, long rangeEndMillis) throws DateException {
        long rangeEndDateValue;
        String timezone = dtstart.timezone;
        this.mIterator.clear(timezone);
        this.mGenerated.clear(timezone);
        this.mIterator.set(rangeStartMillis);
        long rangeStartDateValue = normDateTimeComparisonValue(this.mIterator);
        if (rangeEndMillis != -1) {
            this.mIterator.set(rangeEndMillis);
            rangeEndDateValue = normDateTimeComparisonValue(this.mIterator);
        } else {
            rangeEndDateValue = Long.MAX_VALUE;
        }
        TreeSet<Long> dtSet = new TreeSet<>();
        if (recur.rrules != null) {
            EventRecurrence[] arr$ = recur.rrules;
            for (EventRecurrence rrule : arr$) {
                expand(dtstart, rrule, rangeStartDateValue, rangeEndDateValue, true, dtSet);
            }
        }
        if (recur.rdates != null) {
            long[] arr$2 = recur.rdates;
            for (long dt : arr$2) {
                this.mIterator.set(dt);
                long dtvalue = normDateTimeComparisonValue(this.mIterator);
                dtSet.add(Long.valueOf(dtvalue));
            }
        }
        if (recur.exrules != null) {
            EventRecurrence[] arr$3 = recur.exrules;
            for (EventRecurrence exrule : arr$3) {
                expand(dtstart, exrule, rangeStartDateValue, rangeEndDateValue, false, dtSet);
            }
        }
        if (recur.exdates != null) {
            long[] arr$4 = recur.exdates;
            for (long dt2 : arr$4) {
                this.mIterator.set(dt2);
                long dtvalue2 = normDateTimeComparisonValue(this.mIterator);
                dtSet.remove(Long.valueOf(dtvalue2));
            }
        }
        if (dtSet.isEmpty()) {
            return new long[0];
        }
        int len = dtSet.size();
        long[] dates = new long[len];
        int i = 0;
        for (Long val : dtSet) {
            setTimeFromLongValue(this.mIterator, val.longValue());
            dates[i] = this.mIterator.toMillis(true);
            i++;
        }
        return dates;
    }

    public void expand(Time dtstart, EventRecurrence r, long rangeStartDateValue, long rangeEndDateValue, boolean add, TreeSet<Long> out) throws DateException {
        int freqField;
        long untilDateValue;
        int day;
        int n;
        unsafeNormalize(dtstart);
        long dtstartDateValue = normDateTimeComparisonValue(dtstart);
        int count = 0;
        if (add && dtstartDateValue >= rangeStartDateValue && dtstartDateValue < rangeEndDateValue) {
            out.add(Long.valueOf(dtstartDateValue));
            count = 0 + 1;
        }
        Time iterator = this.mIterator;
        Time until = this.mUntil;
        StringBuilder sb = this.mStringBuilder;
        Time generated = this.mGenerated;
        DaySet days = this.mDays;
        try {
            days.setRecurrence(r);
            if (rangeEndDateValue == Long.MAX_VALUE && r.until == null && r.count == 0) {
                throw new DateException("No range end provided for a recurrence that has no UNTIL or COUNT.");
            }
            int freqAmount = r.interval;
            int freq = r.freq;
            switch (freq) {
                case 1:
                    freqField = 1;
                    break;
                case 2:
                    freqField = 2;
                    break;
                case 3:
                    freqField = 3;
                    break;
                case 4:
                    freqField = 4;
                    break;
                case 5:
                    freqField = 4;
                    freqAmount = r.interval * 7;
                    if (freqAmount <= 0) {
                        freqAmount = 7;
                    }
                    break;
                case 6:
                    freqField = 5;
                    break;
                case 7:
                    freqField = 6;
                    break;
                default:
                    throw new DateException("bad freq=" + freq);
            }
            if (freqAmount <= 0) {
                freqAmount = 1;
            }
            int bymonthCount = r.bymonthCount;
            boolean usebymonth = useBYX(freq, 6, bymonthCount);
            boolean useDays = freq >= 5 && (r.bydayCount > 0 || r.bymonthdayCount > 0);
            int byhourCount = r.byhourCount;
            boolean usebyhour = useBYX(freq, 3, byhourCount);
            int byminuteCount = r.byminuteCount;
            boolean usebyminute = useBYX(freq, 2, byminuteCount);
            int bysecondCount = r.bysecondCount;
            boolean usebysecond = useBYX(freq, 1, bysecondCount);
            iterator.set(dtstart);
            if (freqField == 5 && useDays) {
                iterator.monthDay = 1;
            }
            if (r.until != null) {
                String untilStr = r.until;
                if (untilStr.length() == 15) {
                    untilStr = untilStr + 'Z';
                }
                until.parse(untilStr);
                until.switchTimezone(dtstart.timezone);
                untilDateValue = normDateTimeComparisonValue(until);
            } else {
                untilDateValue = Long.MAX_VALUE;
            }
            sb.ensureCapacity(15);
            sb.setLength(15);
            int failsafe = 0;
            while (true) {
                int monthIndex = 0;
                int failsafe2 = failsafe + 1;
                if (failsafe > 2000) {
                    Log.w("RecurrenceProcessor", "Recurrence processing stuck with r=" + r + " rangeStart=" + rangeStartDateValue + " rangeEnd=" + rangeEndDateValue);
                    return;
                }
                unsafeNormalize(iterator);
                int iteratorYear = iterator.year;
                int iteratorMonth = iterator.month + 1;
                int iteratorDay = iterator.monthDay;
                int iteratorHour = iterator.hour;
                int iteratorMinute = iterator.minute;
                int iteratorSecond = iterator.second;
                generated.set(iterator);
                do {
                    int month = usebymonth ? r.bymonth[monthIndex] : iteratorMonth;
                    int month2 = month - 1;
                    int dayIndex = 1;
                    int lastDayToExamine = 0;
                    if (useDays) {
                        if (freq == 5) {
                            int weekStartAdj = ((iterator.weekDay - EventRecurrence.day2TimeDay(r.wkst)) + 7) % 7;
                            dayIndex = iterator.monthDay - weekStartAdj;
                            lastDayToExamine = dayIndex + 6;
                        } else {
                            lastDayToExamine = generated.getActualMaximum(4);
                        }
                    }
                    do {
                        if (useDays) {
                            if (!days.get(iterator, dayIndex)) {
                                dayIndex++;
                                if (useDays) {
                                }
                                monthIndex++;
                                if (!usebymonth) {
                                }
                                int oldDay = iterator.monthDay;
                                generated.set(iterator);
                                n = 1;
                                while (true) {
                                    int value = freqAmount * n;
                                    switch (freqField) {
                                        case 1:
                                            iterator.second += value;
                                            break;
                                        case 2:
                                            iterator.minute += value;
                                            break;
                                        case 3:
                                            iterator.hour += value;
                                            break;
                                        case 4:
                                            iterator.monthDay += value;
                                            break;
                                        case 5:
                                            iterator.month += value;
                                            break;
                                        case 6:
                                            iterator.year += value;
                                            break;
                                        case 7:
                                            iterator.monthDay += value;
                                            break;
                                        case 8:
                                            iterator.monthDay += value;
                                            break;
                                        default:
                                            throw new RuntimeException("bad field=" + freqField);
                                    }
                                    unsafeNormalize(iterator);
                                    if ((freqField == 6 || freqField == 5) && iterator.monthDay != oldDay) {
                                        n++;
                                        iterator.set(generated);
                                    }
                                }
                                failsafe = failsafe2;
                            } else {
                                day = dayIndex;
                            }
                        } else {
                            day = iteratorDay;
                        }
                        int hourIndex = 0;
                        do {
                            int hour = usebyhour ? r.byhour[hourIndex] : iteratorHour;
                            int minuteIndex = 0;
                            do {
                                int minute = usebyminute ? r.byminute[minuteIndex] : iteratorMinute;
                                int secondIndex = 0;
                                do {
                                    int second = usebysecond ? r.bysecond[secondIndex] : iteratorSecond;
                                    generated.set(second, minute, hour, day, month2, iteratorYear);
                                    unsafeNormalize(generated);
                                    long genDateValue = normDateTimeComparisonValue(generated);
                                    if (genDateValue >= dtstartDateValue) {
                                        int filtered = filter(r, generated);
                                        if (filtered == 0) {
                                            if (dtstartDateValue != genDateValue || !add || dtstartDateValue < rangeStartDateValue || dtstartDateValue >= rangeEndDateValue) {
                                                count++;
                                            }
                                            if (genDateValue <= untilDateValue && genDateValue < rangeEndDateValue) {
                                                if (genDateValue >= rangeStartDateValue) {
                                                    if (add) {
                                                        out.add(Long.valueOf(genDateValue));
                                                    } else {
                                                        out.remove(Long.valueOf(genDateValue));
                                                    }
                                                }
                                                if (r.count > 0 && r.count == count) {
                                                    return;
                                                }
                                            } else {
                                                return;
                                            }
                                        }
                                    }
                                    secondIndex++;
                                    if (usebysecond) {
                                    }
                                    minuteIndex++;
                                    if (!usebyminute) {
                                    }
                                    hourIndex++;
                                    if (usebyhour) {
                                    }
                                    dayIndex++;
                                    if (useDays) {
                                    }
                                    monthIndex++;
                                    if (!usebymonth) {
                                    }
                                    int oldDay2 = iterator.monthDay;
                                    generated.set(iterator);
                                    n = 1;
                                    while (true) {
                                        int value2 = freqAmount * n;
                                        switch (freqField) {
                                        }
                                        unsafeNormalize(iterator);
                                        if (freqField == 6) {
                                            n++;
                                            iterator.set(generated);
                                        } else {
                                            n++;
                                            iterator.set(generated);
                                        }
                                    }
                                    failsafe = failsafe2;
                                } while (secondIndex < bysecondCount);
                                minuteIndex++;
                                if (!usebyminute) {
                                }
                                hourIndex++;
                                if (usebyhour) {
                                }
                                dayIndex++;
                                if (useDays) {
                                }
                                monthIndex++;
                                if (!usebymonth) {
                                }
                                int oldDay22 = iterator.monthDay;
                                generated.set(iterator);
                                n = 1;
                                while (true) {
                                    int value22 = freqAmount * n;
                                    switch (freqField) {
                                    }
                                    unsafeNormalize(iterator);
                                    if (freqField == 6) {
                                    }
                                    n++;
                                    iterator.set(generated);
                                }
                                failsafe = failsafe2;
                            } while (minuteIndex < byminuteCount);
                            hourIndex++;
                            if (usebyhour) {
                            }
                            dayIndex++;
                            if (useDays) {
                            }
                            monthIndex++;
                            if (!usebymonth) {
                            }
                            int oldDay222 = iterator.monthDay;
                            generated.set(iterator);
                            n = 1;
                            while (true) {
                                int value222 = freqAmount * n;
                                switch (freqField) {
                                }
                                unsafeNormalize(iterator);
                                if (freqField == 6) {
                                }
                                n++;
                                iterator.set(generated);
                            }
                            failsafe = failsafe2;
                        } while (hourIndex < byhourCount);
                        dayIndex++;
                        if (useDays) {
                        }
                        monthIndex++;
                        if (!usebymonth) {
                        }
                        int oldDay2222 = iterator.monthDay;
                        generated.set(iterator);
                        n = 1;
                        while (true) {
                            int value2222 = freqAmount * n;
                            switch (freqField) {
                            }
                            unsafeNormalize(iterator);
                            if (freqField == 6) {
                            }
                            n++;
                            iterator.set(generated);
                        }
                        failsafe = failsafe2;
                    } while (dayIndex <= lastDayToExamine);
                    monthIndex++;
                    if (!usebymonth) {
                    }
                    int oldDay22222 = iterator.monthDay;
                    generated.set(iterator);
                    n = 1;
                    while (true) {
                        int value22222 = freqAmount * n;
                        switch (freqField) {
                        }
                        unsafeNormalize(iterator);
                        if (freqField == 6) {
                        }
                        n++;
                        iterator.set(generated);
                    }
                    failsafe = failsafe2;
                } while (monthIndex < bymonthCount);
                int oldDay222222 = iterator.monthDay;
                generated.set(iterator);
                n = 1;
                while (true) {
                    int value222222 = freqAmount * n;
                    switch (freqField) {
                    }
                    unsafeNormalize(iterator);
                    if (freqField == 6) {
                    }
                    n++;
                    iterator.set(generated);
                }
                failsafe = failsafe2;
            }
        } catch (DateException e) {
            Log.w("RecurrenceProcessor", "DateException with r=" + r + " rangeStart=" + rangeStartDateValue + " rangeEnd=" + rangeEndDateValue);
            throw e;
        } catch (RuntimeException t) {
            Log.w("RecurrenceProcessor", "RuntimeException with r=" + r + " rangeStart=" + rangeStartDateValue + " rangeEnd=" + rangeEndDateValue);
            throw t;
        }
    }

    static void unsafeNormalize(Time date) {
        int second = date.second;
        int minute = date.minute;
        int hour = date.hour;
        int monthDay = date.monthDay;
        int month = date.month;
        int year = date.year;
        int addMinutes = (second < 0 ? second - 59 : second) / 60;
        int second2 = second - (addMinutes * 60);
        int minute2 = minute + addMinutes;
        int addHours = (minute2 < 0 ? minute2 - 59 : minute2) / 60;
        int minute3 = minute2 - (addHours * 60);
        int hour2 = hour + addHours;
        int addDays = (hour2 < 0 ? hour2 - 23 : hour2) / 24;
        int hour3 = hour2 - (addDays * 24);
        int monthDay2 = monthDay + addDays;
        while (monthDay2 <= 0) {
            int days = month > 1 ? yearLength(year) : yearLength(year - 1);
            monthDay2 += days;
            year--;
        }
        if (month < 0) {
            int years = ((month + 1) / 12) - 1;
            year += years;
            month -= years * 12;
        } else if (month >= 12) {
            int years2 = month / 12;
            year += years2;
            month -= years2 * 12;
        }
        while (true) {
            if (month == 0) {
                int yearLength = yearLength(year);
                if (monthDay2 > yearLength) {
                    year++;
                    monthDay2 -= yearLength;
                }
            }
            int monthLength = monthLength(year, month);
            if (monthDay2 > monthLength) {
                monthDay2 -= monthLength;
                month++;
                if (month >= 12) {
                    month -= 12;
                    year++;
                }
            } else {
                date.second = second2;
                date.minute = minute3;
                date.hour = hour3;
                date.monthDay = monthDay2;
                date.month = month;
                date.year = year;
                date.weekDay = weekDay(year, month, monthDay2);
                date.yearDay = yearDay(year, month, monthDay2);
                return;
            }
        }
    }

    static boolean isLeapYear(int year) {
        return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
    }

    static int yearLength(int year) {
        return isLeapYear(year) ? 366 : 365;
    }

    static int monthLength(int year, int month) {
        int n = DAYS_PER_MONTH[month];
        if (n != 28) {
            return n;
        }
        return isLeapYear(year) ? 29 : 28;
    }

    static int weekDay(int year, int month, int day) {
        if (month <= 1) {
            month += 12;
            year--;
        }
        return ((((((((month * 13) - 14) / 5) + day) + year) + (year / 4)) - (year / 100)) + (year / 400)) % 7;
    }

    static int yearDay(int year, int month, int day) {
        int yearDay = (DAYS_IN_YEAR_PRECEDING_MONTH[month] + day) - 1;
        if (month >= 2 && isLeapYear(year)) {
            return yearDay + 1;
        }
        return yearDay;
    }

    private static final long normDateTimeComparisonValue(Time normalized) {
        return (((long) normalized.year) << 26) + ((long) (normalized.month << 22)) + ((long) (normalized.monthDay << 17)) + ((long) (normalized.hour << 12)) + ((long) (normalized.minute << 6)) + ((long) normalized.second);
    }

    private static final void setTimeFromLongValue(Time date, long val) {
        date.year = (int) (val >> 26);
        date.month = ((int) (val >> 22)) & 15;
        date.monthDay = ((int) (val >> 17)) & 31;
        date.hour = ((int) (val >> 12)) & 31;
        date.minute = ((int) (val >> 6)) & 63;
        date.second = (int) (63 & val);
    }
}
