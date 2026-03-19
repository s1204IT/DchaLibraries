package android.icu.impl.duration;

import libcore.icu.RelativeDateTimeFormatter;

public final class TimeUnit {
    final String name;
    final byte ordinal;
    public static final TimeUnit YEAR = new TimeUnit("year", 0);
    public static final TimeUnit MONTH = new TimeUnit("month", 1);
    public static final TimeUnit WEEK = new TimeUnit("week", 2);
    public static final TimeUnit DAY = new TimeUnit("day", 3);
    public static final TimeUnit HOUR = new TimeUnit("hour", 4);
    public static final TimeUnit MINUTE = new TimeUnit("minute", 5);
    public static final TimeUnit SECOND = new TimeUnit("second", 6);
    public static final TimeUnit MILLISECOND = new TimeUnit("millisecond", 7);
    static final TimeUnit[] units = {YEAR, MONTH, WEEK, DAY, HOUR, MINUTE, SECOND, MILLISECOND};
    static final long[] approxDurations = {31557600000L, 2630880000L, RelativeDateTimeFormatter.WEEK_IN_MILLIS, 86400000, RelativeDateTimeFormatter.HOUR_IN_MILLIS, RelativeDateTimeFormatter.MINUTE_IN_MILLIS, 1000, 1};

    private TimeUnit(String name, int ordinal) {
        this.name = name;
        this.ordinal = (byte) ordinal;
    }

    public String toString() {
        return this.name;
    }

    public TimeUnit larger() {
        if (this.ordinal == 0) {
            return null;
        }
        return units[this.ordinal - 1];
    }

    public TimeUnit smaller() {
        if (this.ordinal == units.length - 1) {
            return null;
        }
        return units[this.ordinal + 1];
    }

    public int ordinal() {
        return this.ordinal;
    }
}
