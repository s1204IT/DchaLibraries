package com.android.deskclock.data;

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.util.ArrayMap;
import com.android.deskclock.R;
import com.google.android.flexbox.BuildConfig;
import java.text.DateFormatSymbols;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class Weekdays {
    private static final int ALL_DAYS = 127;
    private static final Map<Integer, Integer> sCalendarDayToBit;
    private final int mBits;
    public static final Weekdays ALL = fromBits(127);
    public static final Weekdays NONE = fromBits(0);

    public enum Order {
        SAT_TO_FRI(7, 1, 2, 3, 4, 5, 6),
        SUN_TO_SAT(1, 2, 3, 4, 5, 6, 7),
        MON_TO_SUN(2, 3, 4, 5, 6, 7, 1);

        private final List<Integer> mCalendarDays;

        Order(Integer... numArr) {
            this.mCalendarDays = Arrays.asList(numArr);
        }

        public List<Integer> getCalendarDays() {
            return this.mCalendarDays;
        }
    }

    static {
        ArrayMap arrayMap = new ArrayMap(7);
        arrayMap.put(2, 1);
        arrayMap.put(3, 2);
        arrayMap.put(4, 4);
        arrayMap.put(5, 8);
        arrayMap.put(6, 16);
        arrayMap.put(7, 32);
        arrayMap.put(1, 64);
        sCalendarDayToBit = Collections.unmodifiableMap(arrayMap);
    }

    private Weekdays(int i) {
        this.mBits = i & 127;
    }

    public static Weekdays fromBits(int i) {
        return new Weekdays(i);
    }

    public static Weekdays fromCalendarDays(int... iArr) {
        int iIntValue = 0;
        for (int i : iArr) {
            Integer num = sCalendarDayToBit.get(Integer.valueOf(i));
            if (num != null) {
                iIntValue |= num.intValue();
            }
        }
        return new Weekdays(iIntValue);
    }

    public Weekdays setBit(int i, boolean z) {
        int iIntValue;
        Integer num = sCalendarDayToBit.get(Integer.valueOf(i));
        if (num == null) {
            return this;
        }
        if (z) {
            iIntValue = num.intValue() | this.mBits;
        } else {
            iIntValue = (~num.intValue()) & this.mBits;
        }
        return new Weekdays(iIntValue);
    }

    public boolean isBitOn(int i) {
        Integer num = sCalendarDayToBit.get(Integer.valueOf(i));
        if (num != null) {
            return (this.mBits & num.intValue()) > 0;
        }
        throw new IllegalArgumentException(i + " is not a valid weekday");
    }

    public int getBits() {
        return this.mBits;
    }

    public boolean isRepeating() {
        return this.mBits != 0;
    }

    public int getDistanceToPreviousDay(Calendar calendar) {
        int i = calendar.get(7);
        for (int i2 = 1; i2 <= 7; i2++) {
            i--;
            if (i < 1) {
                i = 7;
            }
            if (isBitOn(i)) {
                return i2;
            }
        }
        return -1;
    }

    public int getDistanceToNextDay(Calendar calendar) {
        int i = calendar.get(7);
        for (int i2 = 0; i2 < 7; i2++) {
            if (isBitOn(i)) {
                return i2;
            }
            i++;
            if (i > 7) {
                i = 1;
            }
        }
        return -1;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && getClass() == obj.getClass() && this.mBits == ((Weekdays) obj).mBits) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return this.mBits;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(19);
        sb.append("[");
        if (isBitOn(2)) {
            sb.append(sb.length() > 1 ? " M" : "M");
        }
        if (isBitOn(3)) {
            sb.append(sb.length() > 1 ? " T" : "T");
        }
        if (isBitOn(4)) {
            sb.append(sb.length() > 1 ? " W" : "W");
        }
        if (isBitOn(5)) {
            sb.append(sb.length() > 1 ? " Th" : "Th");
        }
        if (isBitOn(6)) {
            sb.append(sb.length() > 1 ? " F" : "F");
        }
        if (isBitOn(7)) {
            sb.append(sb.length() > 1 ? " Sa" : "Sa");
        }
        if (isBitOn(1)) {
            sb.append(sb.length() > 1 ? " Su" : "Su");
        }
        sb.append("]");
        return sb.toString();
    }

    public String toString(Context context, Order order) {
        return toString(context, order, false);
    }

    public String toAccessibilityString(Context context, Order order) {
        return toString(context, order, true);
    }

    @VisibleForTesting
    int getCount() {
        int i = 0;
        for (int i2 = 1; i2 <= 7; i2++) {
            if (isBitOn(i2)) {
                i++;
            }
        }
        return i;
    }

    private String toString(Context context, Order order, boolean z) {
        if (!isRepeating()) {
            return BuildConfig.FLAVOR;
        }
        if (this.mBits == 127) {
            return context.getString(R.string.every_day);
        }
        boolean z2 = true;
        if (!z && getCount() > 1) {
            z2 = false;
        }
        DateFormatSymbols dateFormatSymbols = new DateFormatSymbols();
        String[] weekdays = z2 ? dateFormatSymbols.getWeekdays() : dateFormatSymbols.getShortWeekdays();
        String string = context.getString(R.string.day_concat);
        StringBuilder sb = new StringBuilder(40);
        Iterator<Integer> it = order.getCalendarDays().iterator();
        while (it.hasNext()) {
            int iIntValue = it.next().intValue();
            if (isBitOn(iIntValue)) {
                if (sb.length() > 0) {
                    sb.append(string);
                }
                sb.append(weekdays[iIntValue]);
            }
        }
        return sb.toString();
    }
}
