package com.android.calendarcommon2;

import android.text.format.Time;
import android.util.Log;
import java.util.Iterator;
import java.util.TreeSet;

public class RecurrenceProcessor {
    private static final int[] DAYS_PER_MONTH = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    private static final int[] DAYS_IN_YEAR_PRECEDING_MONTH = {0, 31, 59, 90, 120, 151, 180, 212, 243, 273, 304, 334};
    private Time mIterator = new Time("UTC");
    private Time mUntil = new Time("UTC");
    private StringBuilder mStringBuilder = new StringBuilder();
    private Time mGenerated = new Time("UTC");
    private DaySet mDays = new DaySet(false);

    private static boolean listContains(int[] iArr, int i, int i2) {
        for (int i3 = 0; i3 < i; i3++) {
            if (iArr[i3] == i2) {
                return true;
            }
        }
        return false;
    }

    private static boolean listContains(int[] iArr, int i, int i2, int i3) {
        int i4 = i3;
        for (int i5 = 0; i5 < i; i5++) {
            int i6 = iArr[i5];
            if (i6 > 0) {
                if (i6 == i2) {
                    return true;
                }
            } else {
                i4 += i6;
                if (i4 == i2) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int filter(EventRecurrence eventRecurrence, Time time) {
        int i = eventRecurrence.freq;
        if (6 >= i && eventRecurrence.bymonthCount > 0 && !listContains(eventRecurrence.bymonth, eventRecurrence.bymonthCount, time.month + 1)) {
            return 1;
        }
        if (5 >= i && eventRecurrence.byweeknoCount > 0 && !listContains(eventRecurrence.byweekno, eventRecurrence.byweeknoCount, time.getWeekNumber(), time.getActualMaximum(9))) {
            return 2;
        }
        if (4 >= i) {
            if (eventRecurrence.byyeardayCount > 0 && !listContains(eventRecurrence.byyearday, eventRecurrence.byyeardayCount, time.yearDay, time.getActualMaximum(8))) {
                return 3;
            }
            if (eventRecurrence.bymonthdayCount > 0 && !listContains(eventRecurrence.bymonthday, eventRecurrence.bymonthdayCount, time.monthDay, time.getActualMaximum(4))) {
                return 4;
            }
            if (eventRecurrence.bydayCount > 0) {
                int[] iArr = eventRecurrence.byday;
                int i2 = eventRecurrence.bydayCount;
                int iTimeDay2Day = EventRecurrence.timeDay2Day(time.weekDay);
                for (int i3 = 0; i3 < i2; i3++) {
                    if (iArr[i3] != iTimeDay2Day) {
                    }
                }
                return 5;
            }
        }
        if (3 >= i && !listContains(eventRecurrence.byhour, eventRecurrence.byhourCount, time.hour, time.getActualMaximum(3))) {
            return 6;
        }
        if (2 >= i && !listContains(eventRecurrence.byminute, eventRecurrence.byminuteCount, time.minute, time.getActualMaximum(2))) {
            return 7;
        }
        if (1 >= i && !listContains(eventRecurrence.bysecond, eventRecurrence.bysecondCount, time.second, time.getActualMaximum(1))) {
            return 8;
        }
        if (eventRecurrence.bysetposCount > 0) {
            if (i == 6 && eventRecurrence.bydayCount > 0) {
                int i4 = eventRecurrence.bydayCount - 1;
                while (true) {
                    if (i4 >= 0) {
                        if (eventRecurrence.bydayNum[i4] == 0) {
                            i4--;
                        } else if (Log.isLoggable("RecurrenceProcessor", 2)) {
                            Log.v("RecurrenceProcessor", "BYSETPOS not supported with these rules: " + eventRecurrence);
                        }
                    } else if (!filterMonthlySetPos(eventRecurrence, time)) {
                        return 9;
                    }
                }
            } else if (Log.isLoggable("RecurrenceProcessor", 2)) {
                Log.v("RecurrenceProcessor", "BYSETPOS not supported with these rules: " + eventRecurrence);
            }
        }
        return 0;
    }

    private static boolean filterMonthlySetPos(EventRecurrence eventRecurrence, Time time) {
        int i = ((time.weekDay - time.monthDay) + 36) % 7;
        int i2 = 0;
        for (int i3 = 0; i3 < eventRecurrence.bydayCount; i3++) {
            i2 |= eventRecurrence.byday[i3];
        }
        int actualMaximum = time.getActualMaximum(4);
        int[] iArr = new int[actualMaximum];
        int i4 = i;
        int i5 = 0;
        for (int i6 = 1; i6 <= actualMaximum; i6++) {
            if (((65536 << i4) & i2) != 0) {
                iArr[i5] = i6;
                i5++;
            }
            i4++;
            if (i4 == 7) {
                i4 = 0;
            }
        }
        for (int i7 = eventRecurrence.bysetposCount - 1; i7 >= 0; i7--) {
            int i8 = eventRecurrence.bysetpos[i7];
            if (i8 > 0) {
                if (i8 <= i5 && iArr[i8 - 1] == time.monthDay) {
                    return true;
                }
            } else if (i8 < 0) {
                int i9 = i8 + i5;
                if (i9 >= 0 && iArr[i9] == time.monthDay) {
                    return true;
                }
            } else {
                throw new RuntimeException("invalid bysetpos value");
            }
        }
        return false;
    }

    private static boolean useBYX(int i, int i2, int i3) {
        return i > i2 && i3 > 0;
    }

    public static class DaySet {
        private int mDays;
        private int mMonth;
        private EventRecurrence mR;
        private Time mTime = new Time("UTC");
        private int mYear;

        public DaySet(boolean z) {
        }

        void setRecurrence(EventRecurrence eventRecurrence) {
            this.mYear = 0;
            this.mMonth = -1;
            this.mR = eventRecurrence;
        }

        boolean get(Time time, int i) {
            Time time2;
            int i2 = time.year;
            int i3 = time.month;
            if (i < 1 || i > 28) {
                time2 = this.mTime;
                time2.set(i, i3, i2);
                RecurrenceProcessor.unsafeNormalize(time2);
                i2 = time2.year;
                i3 = time2.month;
                i = time2.monthDay;
            } else {
                time2 = null;
            }
            if (i2 != this.mYear || i3 != this.mMonth) {
                if (time2 == null) {
                    time2 = this.mTime;
                    time2.set(i, i3, i2);
                    RecurrenceProcessor.unsafeNormalize(time2);
                }
                this.mYear = i2;
                this.mMonth = i3;
                this.mDays = generateDaysList(time2, this.mR);
            }
            return (this.mDays & (1 << i)) != 0;
        }

        private static int generateDaysList(Time time, EventRecurrence eventRecurrence) {
            int i;
            int i2;
            int i3;
            int actualMaximum = time.getActualMaximum(4);
            int i4 = eventRecurrence.bydayCount;
            if (i4 > 0) {
                int i5 = time.monthDay;
                while (i5 >= 8) {
                    i5 -= 7;
                }
                int i6 = time.weekDay;
                if (i6 < i5) {
                    i3 = (i6 - i5) + 8;
                } else {
                    i3 = (i6 - i5) + 1;
                }
                int[] iArr = eventRecurrence.byday;
                int[] iArr2 = eventRecurrence.bydayNum;
                i = 0;
                for (int i7 = 0; i7 < i4; i7++) {
                    int i8 = iArr2[i7];
                    int iDay2TimeDay = (EventRecurrence.day2TimeDay(iArr[i7]) - i3) + 1;
                    if (iDay2TimeDay <= 0) {
                        iDay2TimeDay += 7;
                    }
                    if (i8 == 0) {
                        while (iDay2TimeDay <= actualMaximum) {
                            i |= 1 << iDay2TimeDay;
                            iDay2TimeDay += 7;
                        }
                    } else if (i8 > 0) {
                        int i9 = iDay2TimeDay + (7 * (i8 - 1));
                        if (i9 <= actualMaximum) {
                            i |= 1 << i9;
                        }
                    } else {
                        while (iDay2TimeDay <= actualMaximum) {
                            iDay2TimeDay += 7;
                        }
                        int i10 = iDay2TimeDay + (7 * i8);
                        if (i10 >= 1) {
                            i |= 1 << i10;
                        }
                    }
                }
            } else {
                i = 0;
            }
            if (eventRecurrence.freq > 5 && (i2 = eventRecurrence.bymonthdayCount) != 0) {
                int[] iArr3 = eventRecurrence.bymonthday;
                if (eventRecurrence.bydayCount == 0) {
                    for (int i11 = 0; i11 < i2; i11++) {
                        int i12 = iArr3[i11];
                        if (i12 >= 0) {
                            i |= 1 << i12;
                        } else {
                            int i13 = i12 + actualMaximum + 1;
                            if (i13 >= 1 && i13 <= actualMaximum) {
                                i |= 1 << i13;
                            }
                        }
                    }
                } else {
                    for (int i14 = 1; i14 <= actualMaximum; i14++) {
                        int i15 = 1 << i14;
                        if ((i & i15) != 0) {
                            int i16 = 0;
                            while (true) {
                                if (i16 < i2) {
                                    if (iArr3[i16] == i14) {
                                        break;
                                    }
                                    i16++;
                                } else {
                                    i &= ~i15;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            return i;
        }
    }

    public long[] expand(Time time, RecurrenceSet recurrenceSet, long j, long j2) throws DateException {
        long jNormDateTimeComparisonValue;
        String str = time.timezone;
        this.mIterator.clear(str);
        this.mGenerated.clear(str);
        this.mIterator.set(j);
        long jNormDateTimeComparisonValue2 = normDateTimeComparisonValue(this.mIterator);
        if (j2 != -1) {
            this.mIterator.set(j2);
            jNormDateTimeComparisonValue = normDateTimeComparisonValue(this.mIterator);
        } else {
            jNormDateTimeComparisonValue = Long.MAX_VALUE;
        }
        long j3 = jNormDateTimeComparisonValue;
        TreeSet<Long> treeSet = new TreeSet<>();
        if (recurrenceSet.rrules != null) {
            EventRecurrence[] eventRecurrenceArr = recurrenceSet.rrules;
            int i = 0;
            for (int length = eventRecurrenceArr.length; i < length; length = length) {
                expand(time, eventRecurrenceArr[i], jNormDateTimeComparisonValue2, j3, true, treeSet);
                i++;
                eventRecurrenceArr = eventRecurrenceArr;
            }
        }
        if (recurrenceSet.rdates != null) {
            for (long j4 : recurrenceSet.rdates) {
                this.mIterator.set(j4);
                treeSet.add(Long.valueOf(normDateTimeComparisonValue(this.mIterator)));
            }
        }
        if (recurrenceSet.exrules != null) {
            EventRecurrence[] eventRecurrenceArr2 = recurrenceSet.exrules;
            int length2 = eventRecurrenceArr2.length;
            int i2 = 0;
            while (i2 < length2) {
                expand(time, eventRecurrenceArr2[i2], jNormDateTimeComparisonValue2, j3, false, treeSet);
                i2++;
                length2 = length2;
                eventRecurrenceArr2 = eventRecurrenceArr2;
            }
        }
        if (recurrenceSet.exdates != null) {
            for (long j5 : recurrenceSet.exdates) {
                this.mIterator.set(j5);
                treeSet.remove(Long.valueOf(normDateTimeComparisonValue(this.mIterator)));
            }
        }
        if (treeSet.isEmpty()) {
            return new long[0];
        }
        int i3 = 0;
        long[] jArr = new long[treeSet.size()];
        Iterator<Long> it = treeSet.iterator();
        while (it.hasNext()) {
            setTimeFromLongValue(this.mIterator, it.next().longValue());
            jArr[i3] = this.mIterator.toMillis(true);
            i3++;
        }
        return jArr;
    }

    public void expand(Time time, EventRecurrence eventRecurrence, long j, long j2, boolean z, TreeSet<Long> treeSet) throws DateException {
        int i;
        int i2;
        int i3;
        boolean zUseBYX;
        int i4;
        DaySet daySet;
        int i5;
        int i6;
        int i7;
        int i8;
        long j3;
        int i9;
        int i10;
        int i11;
        int i12;
        int i13;
        int actualMaximum;
        int i14;
        int i15;
        DaySet daySet2;
        int i16;
        int i17;
        int i18;
        DaySet daySet3;
        int i19;
        int i20;
        Time time2;
        int i21;
        int i22;
        int i23;
        boolean z2;
        int i24;
        int i25;
        int i26;
        long jNormDateTimeComparisonValue;
        long j4;
        int i27;
        int i28;
        int i29;
        int i30;
        int i31;
        int i32;
        int i33;
        int i34;
        int i35;
        int i36;
        int i37;
        Time time3;
        int i38;
        int i39;
        unsafeNormalize(time);
        long jNormDateTimeComparisonValue2 = normDateTimeComparisonValue(time);
        if (!z || jNormDateTimeComparisonValue2 < j || jNormDateTimeComparisonValue2 >= j2) {
            i = 0;
        } else {
            treeSet.add(Long.valueOf(jNormDateTimeComparisonValue2));
            i = 1;
        }
        Time time4 = this.mIterator;
        Time time5 = this.mUntil;
        StringBuilder sb = this.mStringBuilder;
        Time time6 = this.mGenerated;
        DaySet daySet4 = this.mDays;
        try {
            daySet4.setRecurrence(eventRecurrence);
            long jNormDateTimeComparisonValue3 = Long.MAX_VALUE;
            if (j2 == Long.MAX_VALUE) {
                i2 = i;
                if (eventRecurrence.until == null && eventRecurrence.count == 0) {
                    throw new DateException("No range end provided for a recurrence that has no UNTIL or COUNT.");
                }
            } else {
                i2 = i;
            }
            int i40 = eventRecurrence.interval;
            int i41 = eventRecurrence.freq;
            switch (i41) {
                case 1:
                    i3 = 1;
                    if (i40 <= 0) {
                        i40 = 1;
                    }
                    int i42 = eventRecurrence.bymonthCount;
                    zUseBYX = useBYX(i41, 6, i42);
                    boolean z3 = i41 < 5 && (eventRecurrence.bydayCount > 0 || eventRecurrence.bymonthdayCount > 0);
                    i4 = i42;
                    int i43 = eventRecurrence.byhourCount;
                    daySet = daySet4;
                    boolean zUseBYX2 = useBYX(i41, 3, i43);
                    i5 = i43;
                    int i44 = eventRecurrence.byminuteCount;
                    boolean zUseBYX3 = useBYX(i41, 2, i44);
                    i6 = i44;
                    int i45 = eventRecurrence.bysecondCount;
                    boolean zUseBYX4 = useBYX(i41, 1, i45);
                    time4.set(time);
                    if (i3 == 5 && z3) {
                        time4.monthDay = 1;
                    }
                    i7 = i3;
                    if (eventRecurrence.until == null) {
                        String str = eventRecurrence.until;
                        i8 = i45;
                        if (str.length() == 15) {
                            str = str + 'Z';
                        }
                        time5.parse(str);
                        time5.switchTimezone(time.timezone);
                        jNormDateTimeComparisonValue3 = normDateTimeComparisonValue(time5);
                    } else {
                        i8 = i45;
                    }
                    j3 = jNormDateTimeComparisonValue3;
                    sb.ensureCapacity(15);
                    sb.setLength(15);
                    i9 = 0;
                    while (true) {
                        int i46 = i9 + 1;
                        if (i9 <= 2000) {
                            Log.w("RecurrenceProcessor", "Recurrence processing stuck with r=" + eventRecurrence + " rangeStart=" + j + " rangeEnd=" + j2);
                            return;
                        }
                        unsafeNormalize(time4);
                        int i47 = time4.year;
                        int i48 = time4.month + 1;
                        int i49 = time4.monthDay;
                        int i50 = time4.hour;
                        int i51 = time4.minute;
                        int i52 = time4.second;
                        time6.set(time4);
                        int i53 = 0;
                        while (true) {
                            if (zUseBYX) {
                                i10 = i52;
                                i11 = eventRecurrence.bymonth[i53];
                            } else {
                                i10 = i52;
                                i11 = i48;
                            }
                            int i54 = i11 - 1;
                            if (z3) {
                                i12 = i48;
                                if (i41 == 5) {
                                    i13 = i49;
                                    int iDay2TimeDay = time4.monthDay - (((time4.weekDay - EventRecurrence.day2TimeDay(eventRecurrence.wkst)) + 7) % 7);
                                    i14 = iDay2TimeDay;
                                    actualMaximum = iDay2TimeDay + 6;
                                    while (true) {
                                        if (z3) {
                                            i15 = i41;
                                            daySet2 = daySet;
                                            i16 = i13;
                                        } else {
                                            i15 = i41;
                                            daySet2 = daySet;
                                            if (daySet2.get(time4, i14)) {
                                                i16 = i14;
                                            } else {
                                                i14++;
                                                j4 = j3;
                                                i25 = i47;
                                                i26 = i54;
                                                z2 = zUseBYX;
                                                daySet3 = daySet2;
                                                time2 = time4;
                                                i29 = i4;
                                                i35 = i5;
                                                i33 = i6;
                                                i31 = i8;
                                                if (z3 && i14 <= actualMaximum) {
                                                    i8 = i31;
                                                    i6 = i33;
                                                    i4 = i29;
                                                    i5 = i35;
                                                    i41 = i15;
                                                    daySet = daySet3;
                                                    time4 = time2;
                                                    zUseBYX = z2;
                                                    i47 = i25;
                                                    i54 = i26;
                                                    j3 = j4;
                                                }
                                            }
                                        }
                                        i17 = i2;
                                        i18 = 0;
                                        while (true) {
                                            if (zUseBYX2) {
                                                daySet3 = daySet2;
                                                i19 = i50;
                                            } else {
                                                daySet3 = daySet2;
                                                i19 = eventRecurrence.byhour[i18];
                                            }
                                            i20 = 0;
                                            while (true) {
                                                if (zUseBYX3) {
                                                    time2 = time4;
                                                    i21 = i51;
                                                } else {
                                                    time2 = time4;
                                                    i21 = eventRecurrence.byminute[i20];
                                                }
                                                i22 = i17;
                                                i23 = 0;
                                                while (true) {
                                                    if (zUseBYX4) {
                                                        z2 = zUseBYX;
                                                        i24 = i10;
                                                    } else {
                                                        z2 = zUseBYX;
                                                        i24 = eventRecurrence.bysecond[i23];
                                                    }
                                                    time6.set(i24, i21, i19, i16, i54, i47);
                                                    unsafeNormalize(time6);
                                                    i25 = i47;
                                                    i26 = i54;
                                                    jNormDateTimeComparisonValue = normDateTimeComparisonValue(time6);
                                                    if (jNormDateTimeComparisonValue >= jNormDateTimeComparisonValue2 || filter(eventRecurrence, time6) != 0) {
                                                        j4 = j3;
                                                        i27 = i19;
                                                        i28 = i21;
                                                        i29 = i4;
                                                    } else {
                                                        if (jNormDateTimeComparisonValue2 == jNormDateTimeComparisonValue) {
                                                            i27 = i19;
                                                            i29 = i4;
                                                            if (!z || jNormDateTimeComparisonValue2 < j || jNormDateTimeComparisonValue2 >= j2) {
                                                            }
                                                            i28 = i21;
                                                            i36 = i22;
                                                            if (jNormDateTimeComparisonValue > j3 && jNormDateTimeComparisonValue < j2) {
                                                                if (jNormDateTimeComparisonValue >= j) {
                                                                    j4 = j3;
                                                                } else if (z) {
                                                                    treeSet.add(Long.valueOf(jNormDateTimeComparisonValue));
                                                                    j4 = j3;
                                                                } else {
                                                                    j4 = j3;
                                                                    treeSet.remove(Long.valueOf(jNormDateTimeComparisonValue));
                                                                }
                                                                if (eventRecurrence.count <= 0 && eventRecurrence.count == i36) {
                                                                    return;
                                                                } else {
                                                                    i22 = i36;
                                                                }
                                                            }
                                                            return;
                                                        }
                                                        i27 = i19;
                                                        i29 = i4;
                                                        i22++;
                                                        i28 = i21;
                                                        i36 = i22;
                                                        if (jNormDateTimeComparisonValue > j3) {
                                                            return;
                                                        }
                                                        if (jNormDateTimeComparisonValue >= j) {
                                                        }
                                                        if (eventRecurrence.count <= 0) {
                                                        }
                                                        i22 = i36;
                                                    }
                                                    i30 = i23 + 1;
                                                    if (zUseBYX4) {
                                                        i31 = i8;
                                                    } else {
                                                        i31 = i8;
                                                        if (i30 < i31) {
                                                            i23 = i30;
                                                            i8 = i31;
                                                            i4 = i29;
                                                            zUseBYX = z2;
                                                            i47 = i25;
                                                            i54 = i26;
                                                            i19 = i27;
                                                            i21 = i28;
                                                            j3 = j4;
                                                        }
                                                    }
                                                }
                                                i32 = i20 + 1;
                                                if (zUseBYX3) {
                                                    i33 = i6;
                                                } else {
                                                    i33 = i6;
                                                    if (i32 < i33) {
                                                        i20 = i32;
                                                        i8 = i31;
                                                        i6 = i33;
                                                        i4 = i29;
                                                        i17 = i22;
                                                        time4 = time2;
                                                        zUseBYX = z2;
                                                        i47 = i25;
                                                        i54 = i26;
                                                        i19 = i27;
                                                        j3 = j4;
                                                    }
                                                }
                                                break;
                                            }
                                            i18 = i34;
                                            i8 = i31;
                                            i6 = i33;
                                            i4 = i29;
                                            i5 = i35;
                                            i17 = i22;
                                            daySet2 = daySet3;
                                            time4 = time2;
                                            zUseBYX = z2;
                                            i47 = i25;
                                            i54 = i26;
                                            j3 = j4;
                                        }
                                    }
                                } else {
                                    i13 = i49;
                                    actualMaximum = time6.getActualMaximum(4);
                                }
                            } else {
                                i12 = i48;
                                i13 = i49;
                                actualMaximum = 0;
                            }
                            i14 = 1;
                            while (true) {
                                if (z3) {
                                }
                                i17 = i2;
                                i18 = 0;
                                while (true) {
                                    if (zUseBYX2) {
                                    }
                                    i20 = 0;
                                    while (true) {
                                        if (zUseBYX3) {
                                        }
                                        i22 = i17;
                                        i23 = 0;
                                        while (true) {
                                            if (zUseBYX4) {
                                            }
                                            time6.set(i24, i21, i19, i16, i54, i47);
                                            unsafeNormalize(time6);
                                            i25 = i47;
                                            i26 = i54;
                                            jNormDateTimeComparisonValue = normDateTimeComparisonValue(time6);
                                            if (jNormDateTimeComparisonValue >= jNormDateTimeComparisonValue2) {
                                                j4 = j3;
                                                i27 = i19;
                                                i28 = i21;
                                                i29 = i4;
                                                i30 = i23 + 1;
                                                if (zUseBYX4) {
                                                }
                                            }
                                            if (z3) {
                                                i8 = i31;
                                                i6 = i33;
                                                i4 = i29;
                                                i5 = i35;
                                                i41 = i15;
                                                daySet = daySet3;
                                                time4 = time2;
                                                zUseBYX = z2;
                                                i47 = i25;
                                                i54 = i26;
                                                j3 = j4;
                                            }
                                            i23 = i30;
                                            i8 = i31;
                                            i4 = i29;
                                            zUseBYX = z2;
                                            i47 = i25;
                                            i54 = i26;
                                            i19 = i27;
                                            i21 = i28;
                                            j3 = j4;
                                            break;
                                        }
                                        i32 = i20 + 1;
                                        if (zUseBYX3) {
                                        }
                                        i20 = i32;
                                        i8 = i31;
                                        i6 = i33;
                                        i4 = i29;
                                        i17 = i22;
                                        time4 = time2;
                                        zUseBYX = z2;
                                        i47 = i25;
                                        i54 = i26;
                                        i19 = i27;
                                        j3 = j4;
                                    }
                                    i18 = i34;
                                    i8 = i31;
                                    i6 = i33;
                                    i4 = i29;
                                    i5 = i35;
                                    i17 = i22;
                                    daySet2 = daySet3;
                                    time4 = time2;
                                    zUseBYX = z2;
                                    i47 = i25;
                                    i54 = i26;
                                    j3 = j4;
                                }
                            }
                            i53 = i37;
                            i8 = i31;
                            i6 = i33;
                            i4 = i29;
                            i5 = i35;
                            i52 = i10;
                            i48 = i12;
                            i49 = i13;
                            i41 = i15;
                            daySet = daySet3;
                            time4 = time2;
                            zUseBYX = z2;
                            i47 = i25;
                            j3 = j4;
                        }
                        time3 = time2;
                        int i55 = time3.monthDay;
                        time6.set(time3);
                        int i56 = 1;
                        while (true) {
                            int i57 = i40 * i56;
                            switch (i7) {
                                case 1:
                                    time3.second += i57;
                                    break;
                                case 2:
                                    time3.minute += i57;
                                    break;
                                case 3:
                                    time3.hour += i57;
                                    break;
                                case 4:
                                    time3.monthDay += i57;
                                    break;
                                case 5:
                                    time3.month += i57;
                                    break;
                                case 6:
                                    time3.year += i57;
                                    break;
                                case 7:
                                    time3.monthDay += i57;
                                    break;
                                case 8:
                                    time3.monthDay += i57;
                                    break;
                                default:
                                    throw new RuntimeException("bad field=" + i7);
                            }
                            unsafeNormalize(time3);
                            i38 = i31;
                            i39 = i7;
                            if (i39 == 6 || i39 == 5) {
                                if (time3.monthDay == i55) {
                                }
                                i56++;
                                time3.set(time6);
                                i7 = i39;
                                i31 = i38;
                            }
                            break;
                        }
                        i7 = i39;
                        i6 = i33;
                        i4 = i29;
                        i5 = i35;
                        i9 = i46;
                        i41 = i15;
                        daySet = daySet3;
                        zUseBYX = z2;
                        i8 = i38;
                        time4 = time3;
                        j3 = j4;
                    }
                    break;
                case 2:
                    i3 = 2;
                    if (i40 <= 0) {
                    }
                    int i422 = eventRecurrence.bymonthCount;
                    zUseBYX = useBYX(i41, 6, i422);
                    if (i41 < 5) {
                    }
                    i4 = i422;
                    int i432 = eventRecurrence.byhourCount;
                    daySet = daySet4;
                    boolean zUseBYX22 = useBYX(i41, 3, i432);
                    i5 = i432;
                    int i442 = eventRecurrence.byminuteCount;
                    boolean zUseBYX32 = useBYX(i41, 2, i442);
                    i6 = i442;
                    int i452 = eventRecurrence.bysecondCount;
                    boolean zUseBYX42 = useBYX(i41, 1, i452);
                    time4.set(time);
                    if (i3 == 5) {
                        time4.monthDay = 1;
                    }
                    i7 = i3;
                    if (eventRecurrence.until == null) {
                    }
                    j3 = jNormDateTimeComparisonValue3;
                    sb.ensureCapacity(15);
                    sb.setLength(15);
                    i9 = 0;
                    while (true) {
                        int i462 = i9 + 1;
                        if (i9 <= 2000) {
                        }
                        i7 = i39;
                        i6 = i33;
                        i4 = i29;
                        i5 = i35;
                        i9 = i462;
                        i41 = i15;
                        daySet = daySet3;
                        zUseBYX = z2;
                        i8 = i38;
                        time4 = time3;
                        j3 = j4;
                    }
                    break;
                case 3:
                    i3 = 3;
                    if (i40 <= 0) {
                    }
                    int i4222 = eventRecurrence.bymonthCount;
                    zUseBYX = useBYX(i41, 6, i4222);
                    if (i41 < 5) {
                    }
                    i4 = i4222;
                    int i4322 = eventRecurrence.byhourCount;
                    daySet = daySet4;
                    boolean zUseBYX222 = useBYX(i41, 3, i4322);
                    i5 = i4322;
                    int i4422 = eventRecurrence.byminuteCount;
                    boolean zUseBYX322 = useBYX(i41, 2, i4422);
                    i6 = i4422;
                    int i4522 = eventRecurrence.bysecondCount;
                    boolean zUseBYX422 = useBYX(i41, 1, i4522);
                    time4.set(time);
                    if (i3 == 5) {
                    }
                    i7 = i3;
                    if (eventRecurrence.until == null) {
                    }
                    j3 = jNormDateTimeComparisonValue3;
                    sb.ensureCapacity(15);
                    sb.setLength(15);
                    i9 = 0;
                    while (true) {
                        int i4622 = i9 + 1;
                        if (i9 <= 2000) {
                        }
                        i7 = i39;
                        i6 = i33;
                        i4 = i29;
                        i5 = i35;
                        i9 = i4622;
                        i41 = i15;
                        daySet = daySet3;
                        zUseBYX = z2;
                        i8 = i38;
                        time4 = time3;
                        j3 = j4;
                    }
                    break;
                case 5:
                    int i58 = eventRecurrence.interval * 7;
                    if (i58 <= 0) {
                        i40 = 7;
                        break;
                    } else {
                        i40 = i58;
                        break;
                    }
                case 4:
                    i3 = 4;
                    if (i40 <= 0) {
                    }
                    int i42222 = eventRecurrence.bymonthCount;
                    zUseBYX = useBYX(i41, 6, i42222);
                    if (i41 < 5) {
                    }
                    i4 = i42222;
                    int i43222 = eventRecurrence.byhourCount;
                    daySet = daySet4;
                    boolean zUseBYX2222 = useBYX(i41, 3, i43222);
                    i5 = i43222;
                    int i44222 = eventRecurrence.byminuteCount;
                    boolean zUseBYX3222 = useBYX(i41, 2, i44222);
                    i6 = i44222;
                    int i45222 = eventRecurrence.bysecondCount;
                    boolean zUseBYX4222 = useBYX(i41, 1, i45222);
                    time4.set(time);
                    if (i3 == 5) {
                    }
                    i7 = i3;
                    if (eventRecurrence.until == null) {
                    }
                    j3 = jNormDateTimeComparisonValue3;
                    sb.ensureCapacity(15);
                    sb.setLength(15);
                    i9 = 0;
                    while (true) {
                        int i46222 = i9 + 1;
                        if (i9 <= 2000) {
                        }
                        i7 = i39;
                        i6 = i33;
                        i4 = i29;
                        i5 = i35;
                        i9 = i46222;
                        i41 = i15;
                        daySet = daySet3;
                        zUseBYX = z2;
                        i8 = i38;
                        time4 = time3;
                        j3 = j4;
                    }
                    break;
                case 6:
                    i3 = 5;
                    if (i40 <= 0) {
                    }
                    int i422222 = eventRecurrence.bymonthCount;
                    zUseBYX = useBYX(i41, 6, i422222);
                    if (i41 < 5) {
                    }
                    i4 = i422222;
                    int i432222 = eventRecurrence.byhourCount;
                    daySet = daySet4;
                    boolean zUseBYX22222 = useBYX(i41, 3, i432222);
                    i5 = i432222;
                    int i442222 = eventRecurrence.byminuteCount;
                    boolean zUseBYX32222 = useBYX(i41, 2, i442222);
                    i6 = i442222;
                    int i452222 = eventRecurrence.bysecondCount;
                    boolean zUseBYX42222 = useBYX(i41, 1, i452222);
                    time4.set(time);
                    if (i3 == 5) {
                    }
                    i7 = i3;
                    if (eventRecurrence.until == null) {
                    }
                    j3 = jNormDateTimeComparisonValue3;
                    sb.ensureCapacity(15);
                    sb.setLength(15);
                    i9 = 0;
                    while (true) {
                        int i462222 = i9 + 1;
                        if (i9 <= 2000) {
                        }
                        i7 = i39;
                        i6 = i33;
                        i4 = i29;
                        i5 = i35;
                        i9 = i462222;
                        i41 = i15;
                        daySet = daySet3;
                        zUseBYX = z2;
                        i8 = i38;
                        time4 = time3;
                        j3 = j4;
                    }
                    break;
                case 7:
                    i3 = 6;
                    if (i40 <= 0) {
                    }
                    int i4222222 = eventRecurrence.bymonthCount;
                    zUseBYX = useBYX(i41, 6, i4222222);
                    if (i41 < 5) {
                    }
                    i4 = i4222222;
                    int i4322222 = eventRecurrence.byhourCount;
                    daySet = daySet4;
                    boolean zUseBYX222222 = useBYX(i41, 3, i4322222);
                    i5 = i4322222;
                    int i4422222 = eventRecurrence.byminuteCount;
                    boolean zUseBYX322222 = useBYX(i41, 2, i4422222);
                    i6 = i4422222;
                    int i4522222 = eventRecurrence.bysecondCount;
                    boolean zUseBYX422222 = useBYX(i41, 1, i4522222);
                    time4.set(time);
                    if (i3 == 5) {
                    }
                    i7 = i3;
                    if (eventRecurrence.until == null) {
                    }
                    j3 = jNormDateTimeComparisonValue3;
                    sb.ensureCapacity(15);
                    sb.setLength(15);
                    i9 = 0;
                    while (true) {
                        int i4622222 = i9 + 1;
                        if (i9 <= 2000) {
                        }
                        i7 = i39;
                        i6 = i33;
                        i4 = i29;
                        i5 = i35;
                        i9 = i4622222;
                        i41 = i15;
                        daySet = daySet3;
                        zUseBYX = z2;
                        i8 = i38;
                        time4 = time3;
                        j3 = j4;
                    }
                    break;
                default:
                    throw new DateException("bad freq=" + i41);
            }
        } catch (DateException e) {
            Log.w("RecurrenceProcessor", "DateException with r=" + eventRecurrence + " rangeStart=" + j + " rangeEnd=" + j2);
            throw e;
        } catch (RuntimeException e2) {
            Log.w("RecurrenceProcessor", "RuntimeException with r=" + eventRecurrence + " rangeStart=" + j + " rangeEnd=" + j2);
            throw e2;
        }
    }

    static void unsafeNormalize(Time time) {
        int i = time.second;
        int i2 = time.minute;
        int i3 = time.hour;
        int i4 = time.monthDay;
        int i5 = time.month;
        int i6 = time.year;
        int i7 = (i < 0 ? i - 59 : i) / 60;
        int i8 = i - (i7 * 60);
        int i9 = i2 + i7;
        int i10 = (i9 < 0 ? i9 - 59 : i9) / 60;
        int i11 = i9 - (i10 * 60);
        int i12 = i3 + i10;
        int i13 = (i12 < 0 ? i12 - 23 : i12) / 24;
        int i14 = i12 - (i13 * 24);
        int iYearLength = i4 + i13;
        while (iYearLength <= 0) {
            iYearLength += i5 > 1 ? yearLength(i6) : yearLength(i6 - 1);
            i6--;
        }
        if (i5 < 0) {
            int i15 = ((i5 + 1) / 12) - 1;
            i6 += i15;
            i5 -= i15 * 12;
        } else if (i5 >= 12) {
            int i16 = i5 / 12;
            i6 += i16;
            i5 -= i16 * 12;
        }
        while (true) {
            if (i5 == 0) {
                int iYearLength2 = yearLength(i6);
                if (iYearLength > iYearLength2) {
                    i6++;
                    iYearLength -= iYearLength2;
                }
            }
            int iMonthLength = monthLength(i6, i5);
            if (iYearLength > iMonthLength) {
                iYearLength -= iMonthLength;
                i5++;
                if (i5 >= 12) {
                    i5 -= 12;
                    i6++;
                }
            } else {
                time.second = i8;
                time.minute = i11;
                time.hour = i14;
                time.monthDay = iYearLength;
                time.month = i5;
                time.year = i6;
                time.weekDay = weekDay(i6, i5, iYearLength);
                time.yearDay = yearDay(i6, i5, iYearLength);
                return;
            }
        }
    }

    static boolean isLeapYear(int i) {
        return i % 4 == 0 && (i % 100 != 0 || i % 400 == 0);
    }

    static int yearLength(int i) {
        return isLeapYear(i) ? 366 : 365;
    }

    static int monthLength(int i, int i2) {
        int i3 = DAYS_PER_MONTH[i2];
        if (i3 != 28) {
            return i3;
        }
        return isLeapYear(i) ? 29 : 28;
    }

    static int weekDay(int i, int i2, int i3) {
        if (i2 <= 1) {
            i2 += 12;
            i--;
        }
        return (((((i3 + (((13 * i2) - 14) / 5)) + i) + (i / 4)) - (i / 100)) + (i / 400)) % 7;
    }

    static int yearDay(int i, int i2, int i3) {
        int i4 = (DAYS_IN_YEAR_PRECEDING_MONTH[i2] + i3) - 1;
        if (i2 >= 2 && isLeapYear(i)) {
            return i4 + 1;
        }
        return i4;
    }

    private static final long normDateTimeComparisonValue(Time time) {
        return (((long) time.year) << 26) + ((long) (time.month << 22)) + ((long) (time.monthDay << 17)) + ((long) (time.hour << 12)) + ((long) (time.minute << 6)) + ((long) time.second);
    }

    private static final void setTimeFromLongValue(Time time, long j) {
        time.year = (int) (j >> 26);
        time.month = ((int) (j >> 22)) & 15;
        time.monthDay = ((int) (j >> 17)) & 31;
        time.hour = ((int) (j >> 12)) & 31;
        time.minute = ((int) (j >> 6)) & 63;
        time.second = (int) (j & 63);
    }
}
