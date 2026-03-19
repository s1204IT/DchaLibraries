package java.time.format;

import java.util.Calendar;

public enum TextStyle {
    FULL(2, 0),
    FULL_STANDALONE(Calendar.LONG_STANDALONE, 0),
    SHORT(1, 1),
    SHORT_STANDALONE(Calendar.SHORT_STANDALONE, 1),
    NARROW(4, 1),
    NARROW_STANDALONE(Calendar.NARROW_STANDALONE, 1);

    private final int calendarStyle;
    private final int zoneNameStyleIndex;

    TextStyle(int i, int i2) {
        this.calendarStyle = i;
        this.zoneNameStyleIndex = i2;
    }

    public boolean isStandalone() {
        return (ordinal() & 1) == 1;
    }

    public TextStyle asStandalone() {
        return values()[ordinal() | 1];
    }

    public TextStyle asNormal() {
        return values()[ordinal() & (-2)];
    }

    int toCalendarStyle() {
        return this.calendarStyle;
    }

    int zoneNameStyleIndex() {
        return this.zoneNameStyleIndex;
    }
}
