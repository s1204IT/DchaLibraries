package java.text;

import java.util.Calendar;

class CalendarBuilder {
    private static final int COMPUTED = 1;
    public static final int ISO_DAY_OF_WEEK = 1000;
    private static final int MAX_FIELD = 18;
    private static final int MINIMUM_USER_STAMP = 2;
    private static final int UNSET = 0;
    public static final int WEEK_YEAR = 17;
    private final int[] field = new int[36];
    private int nextStamp = 2;
    private int maxFieldIndex = -1;

    CalendarBuilder() {
    }

    CalendarBuilder set(int i, int i2) {
        if (i == 1000) {
            i = 7;
            i2 = toCalendarDayOfWeek(i2);
        }
        int[] iArr = this.field;
        int i3 = this.nextStamp;
        this.nextStamp = i3 + 1;
        iArr[i] = i3;
        this.field[18 + i] = i2;
        if (i > this.maxFieldIndex && i < 17) {
            this.maxFieldIndex = i;
        }
        return this;
    }

    CalendarBuilder addYear(int i) {
        int[] iArr = this.field;
        iArr[19] = iArr[19] + i;
        int[] iArr2 = this.field;
        iArr2[35] = iArr2[35] + i;
        return this;
    }

    boolean isSet(int i) {
        if (i == 1000) {
            i = 7;
        }
        return this.field[i] > 0;
    }

    CalendarBuilder clear(int i) {
        if (i == 1000) {
            i = 7;
        }
        this.field[i] = 0;
        this.field[18 + i] = 0;
        return this;
    }

    Calendar establish(Calendar calendar) {
        boolean z = isSet(17) && this.field[17] > this.field[1];
        if (z && !calendar.isWeekDateSupported()) {
            if (!isSet(1)) {
                set(1, this.field[35]);
            }
            z = false;
        }
        calendar.clear();
        for (int i = 2; i < this.nextStamp; i++) {
            int i2 = 0;
            while (true) {
                if (i2 > this.maxFieldIndex) {
                    break;
                }
                if (this.field[i2] != i) {
                    i2++;
                } else {
                    calendar.set(i2, this.field[18 + i2]);
                    break;
                }
            }
        }
        if (z) {
            int i3 = isSet(3) ? this.field[21] : 1;
            int firstDayOfWeek = isSet(7) ? this.field[25] : calendar.getFirstDayOfWeek();
            if (!isValidDayOfWeek(firstDayOfWeek) && calendar.isLenient()) {
                if (firstDayOfWeek >= 8) {
                    int i4 = firstDayOfWeek - 1;
                    i3 += i4 / 7;
                    firstDayOfWeek = (i4 % 7) + 1;
                } else {
                    while (firstDayOfWeek <= 0) {
                        firstDayOfWeek += 7;
                        i3--;
                    }
                }
                firstDayOfWeek = toCalendarDayOfWeek(firstDayOfWeek);
            }
            calendar.setWeekDate(this.field[35], i3, firstDayOfWeek);
        }
        return calendar;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CalendarBuilder:[");
        for (int i = 0; i < this.field.length; i++) {
            if (isSet(i)) {
                sb.append(i);
                sb.append('=');
                sb.append(this.field[18 + i]);
                sb.append(',');
            }
        }
        int length = sb.length() - 1;
        if (sb.charAt(length) == ',') {
            sb.setLength(length);
        }
        sb.append(']');
        return sb.toString();
    }

    static int toISODayOfWeek(int i) {
        if (i == 1) {
            return 7;
        }
        return i - 1;
    }

    static int toCalendarDayOfWeek(int i) {
        if (!isValidDayOfWeek(i)) {
            return i;
        }
        if (i == 7) {
            return 1;
        }
        return 1 + i;
    }

    static boolean isValidDayOfWeek(int i) {
        return i > 0 && i <= 7;
    }
}
