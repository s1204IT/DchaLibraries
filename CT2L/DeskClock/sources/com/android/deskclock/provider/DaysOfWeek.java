package com.android.deskclock.provider;

import android.content.Context;
import com.android.deskclock.R;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.HashSet;

public final class DaysOfWeek {
    private int mBitSet;

    private static int convertDayToBitIndex(int day) {
        return (day + 5) % 7;
    }

    private static int convertBitIndexToDay(int bitIndex) {
        return ((bitIndex + 1) % 7) + 1;
    }

    public DaysOfWeek(int bitSet) {
        this.mBitSet = bitSet;
    }

    public String toString(Context context, boolean showNever) {
        return toString(context, showNever, false);
    }

    public String toAccessibilityString(Context context) {
        return toString(context, false, true);
    }

    private String toString(Context context, boolean showNever, boolean forAccessibility) {
        StringBuilder ret = new StringBuilder();
        if (this.mBitSet == 0) {
            return showNever ? context.getText(R.string.never).toString() : "";
        }
        if (this.mBitSet == 127) {
            return context.getText(R.string.every_day).toString();
        }
        int dayCount = 0;
        for (int bitSet = this.mBitSet; bitSet > 0; bitSet >>= 1) {
            if ((bitSet & 1) == 1) {
                dayCount++;
            }
        }
        DateFormatSymbols dfs = new DateFormatSymbols();
        String[] dayList = (forAccessibility || dayCount <= 1) ? dfs.getWeekdays() : dfs.getShortWeekdays();
        for (int bitIndex = 0; bitIndex < 7; bitIndex++) {
            if ((this.mBitSet & (1 << bitIndex)) != 0) {
                ret.append(dayList[convertBitIndexToDay(bitIndex)]);
                dayCount--;
                if (dayCount > 0) {
                    ret.append(context.getText(R.string.day_concat));
                }
            }
        }
        return ret.toString();
    }

    public void setDaysOfWeek(boolean value, int... daysOfWeek) {
        for (int day : daysOfWeek) {
            setBit(convertDayToBitIndex(day), value);
        }
    }

    private boolean isBitEnabled(int bitIndex) {
        return (this.mBitSet & (1 << bitIndex)) > 0;
    }

    private void setBit(int bitIndex, boolean set) {
        if (set) {
            this.mBitSet |= 1 << bitIndex;
        } else {
            this.mBitSet &= (1 << bitIndex) ^ (-1);
        }
    }

    public void setBitSet(int bitSet) {
        this.mBitSet = bitSet;
    }

    public int getBitSet() {
        return this.mBitSet;
    }

    public HashSet<Integer> getSetDays() {
        HashSet<Integer> result = new HashSet<>();
        for (int bitIndex = 0; bitIndex < 7; bitIndex++) {
            if (isBitEnabled(bitIndex)) {
                result.add(Integer.valueOf(convertBitIndexToDay(bitIndex)));
            }
        }
        return result;
    }

    public boolean isRepeating() {
        return this.mBitSet != 0;
    }

    public int calculateDaysToNextAlarm(Calendar current) {
        if (!isRepeating()) {
            return -1;
        }
        int dayCount = 0;
        int currentDayBit = convertDayToBitIndex(current.get(7));
        while (dayCount < 7) {
            int nextAlarmBit = (currentDayBit + dayCount) % 7;
            if (!isBitEnabled(nextAlarmBit)) {
                dayCount++;
            } else {
                return dayCount;
            }
        }
        return dayCount;
    }

    public void clearAllDays() {
        this.mBitSet = 0;
    }

    public String toString() {
        return "DaysOfWeek{mBitSet=" + this.mBitSet + '}';
    }
}
