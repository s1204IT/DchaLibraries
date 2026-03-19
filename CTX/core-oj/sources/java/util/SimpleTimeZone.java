package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import sun.util.calendar.BaseCalendar;
import sun.util.calendar.CalendarSystem;
import sun.util.calendar.CalendarUtils;
import sun.util.calendar.Gregorian;

public class SimpleTimeZone extends TimeZone {
    private static final int DOM_MODE = 1;
    private static final int DOW_GE_DOM_MODE = 3;
    private static final int DOW_IN_MONTH_MODE = 2;
    private static final int DOW_LE_DOM_MODE = 4;
    public static final int STANDARD_TIME = 1;
    public static final int UTC_TIME = 2;
    public static final int WALL_TIME = 0;
    static final int currentSerialVersion = 2;
    private static final int millisPerDay = 86400000;
    private static final int millisPerHour = 3600000;
    static final long serialVersionUID = -403250971215465050L;
    private transient long cacheEnd;
    private transient long cacheStart;
    private transient long cacheYear;
    private int dstSavings;
    private int endDay;
    private int endDayOfWeek;
    private int endMode;
    private int endMonth;
    private int endTime;
    private int endTimeMode;
    private final byte[] monthLength;
    private int rawOffset;
    private int serialVersionOnStream;
    private int startDay;
    private int startDayOfWeek;
    private int startMode;
    private int startMonth;
    private int startTime;
    private int startTimeMode;
    private int startYear;
    private boolean useDaylight;
    private static final byte[] staticMonthLength = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    private static final byte[] staticLeapMonthLength = {31, Character.INITIAL_QUOTE_PUNCTUATION, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    private static final Gregorian gcal = CalendarSystem.getGregorianCalendar();

    public SimpleTimeZone(int i, String str) {
        this.useDaylight = false;
        this.monthLength = staticMonthLength;
        this.serialVersionOnStream = 2;
        this.rawOffset = i;
        setID(str);
        this.dstSavings = millisPerHour;
    }

    public SimpleTimeZone(int i, String str, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9) {
        this(i, str, i2, i3, i4, i5, 0, i6, i7, i8, i9, 0, millisPerHour);
    }

    public SimpleTimeZone(int i, String str, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10) {
        this(i, str, i2, i3, i4, i5, 0, i6, i7, i8, i9, 0, i10);
    }

    public SimpleTimeZone(int i, String str, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10, int i11, int i12) {
        this.useDaylight = false;
        this.monthLength = staticMonthLength;
        this.serialVersionOnStream = 2;
        setID(str);
        this.rawOffset = i;
        this.startMonth = i2;
        this.startDay = i3;
        this.startDayOfWeek = i4;
        this.startTime = i5;
        this.startTimeMode = i6;
        this.endMonth = i7;
        this.endDay = i8;
        this.endDayOfWeek = i9;
        this.endTime = i10;
        this.endTimeMode = i11;
        this.dstSavings = i12;
        decodeRules();
        if (i12 <= 0) {
            throw new IllegalArgumentException("Illegal daylight saving value: " + i12);
        }
    }

    public void setStartYear(int i) {
        this.startYear = i;
        invalidateCache();
    }

    public void setStartRule(int i, int i2, int i3, int i4) {
        this.startMonth = i;
        this.startDay = i2;
        this.startDayOfWeek = i3;
        this.startTime = i4;
        this.startTimeMode = 0;
        decodeStartRule();
        invalidateCache();
    }

    public void setStartRule(int i, int i2, int i3) {
        setStartRule(i, i2, 0, i3);
    }

    public void setStartRule(int i, int i2, int i3, int i4, boolean z) {
        if (z) {
            setStartRule(i, i2, -i3, i4);
        } else {
            setStartRule(i, -i2, -i3, i4);
        }
    }

    public void setEndRule(int i, int i2, int i3, int i4) {
        this.endMonth = i;
        this.endDay = i2;
        this.endDayOfWeek = i3;
        this.endTime = i4;
        this.endTimeMode = 0;
        decodeEndRule();
        invalidateCache();
    }

    public void setEndRule(int i, int i2, int i3) {
        setEndRule(i, i2, 0, i3);
    }

    public void setEndRule(int i, int i2, int i3, int i4, boolean z) {
        if (z) {
            setEndRule(i, i2, -i3, i4);
        } else {
            setEndRule(i, -i2, -i3, i4);
        }
    }

    @Override
    public int getOffset(long j) {
        return getOffsets(j, null);
    }

    @Override
    int getOffsets(long j, int[] iArr) {
        int offset = this.rawOffset;
        if (this.useDaylight) {
            synchronized (this) {
                if (this.cacheStart != 0 && j >= this.cacheStart && j < this.cacheEnd) {
                    offset += this.dstSavings;
                } else {
                    BaseCalendar baseCalendar = j >= -12219292800000L ? gcal : (BaseCalendar) CalendarSystem.forName("julian");
                    BaseCalendar.Date date = (BaseCalendar.Date) baseCalendar.newCalendarDate(TimeZone.NO_TIMEZONE);
                    baseCalendar.getCalendarDate(((long) this.rawOffset) + j, date);
                    int normalizedYear = date.getNormalizedYear();
                    if (normalizedYear >= this.startYear) {
                        date.setTimeOfDay(0, 0, 0, 0);
                        offset = getOffset(baseCalendar, date, normalizedYear, j);
                    }
                }
            }
        }
        if (iArr != null) {
            iArr[0] = this.rawOffset;
            iArr[1] = offset - this.rawOffset;
        }
        return offset;
    }

    @Override
    public int getOffset(int i, int i2, int i3, int i4, int i5, int i6) {
        int iMod;
        if (i != 1 && i != 0) {
            throw new IllegalArgumentException("Illegal era " + i);
        }
        if (i == 0) {
            iMod = 1 - i2;
        } else {
            iMod = i2;
        }
        if (iMod >= 292278994) {
            iMod = (iMod % 2800) + 2800;
        } else if (iMod <= -292269054) {
            iMod = (int) CalendarUtils.mod(iMod, 28L);
        }
        int i7 = i3 + 1;
        BaseCalendar baseCalendar = gcal;
        BaseCalendar.Date date = (BaseCalendar.Date) baseCalendar.newCalendarDate(TimeZone.NO_TIMEZONE);
        date.setDate(iMod, i7, i4);
        long time = baseCalendar.getTime(date) + ((long) (i6 - this.rawOffset));
        if (time < -12219292800000L) {
            baseCalendar = (BaseCalendar) CalendarSystem.forName("julian");
            date = (BaseCalendar.Date) baseCalendar.newCalendarDate(TimeZone.NO_TIMEZONE);
            date.setNormalizedDate(iMod, i7, i4);
            time = (baseCalendar.getTime(date) + ((long) i6)) - ((long) this.rawOffset);
        }
        if (date.getNormalizedYear() != iMod || date.getMonth() != i7 || date.getDayOfMonth() != i4 || i5 < 1 || i5 > 7 || i6 < 0 || i6 >= millisPerDay) {
            throw new IllegalArgumentException();
        }
        if (!this.useDaylight || i2 < this.startYear || i != 1) {
            return this.rawOffset;
        }
        return getOffset(baseCalendar, date, iMod, time);
    }

    private int getOffset(BaseCalendar baseCalendar, BaseCalendar.Date date, int i, long j) {
        synchronized (this) {
            if (this.cacheStart != 0) {
                if (j >= this.cacheStart && j < this.cacheEnd) {
                    return this.rawOffset + this.dstSavings;
                }
                if (i == this.cacheYear) {
                    return this.rawOffset;
                }
            }
            long start = getStart(baseCalendar, date, i);
            long end = getEnd(baseCalendar, date, i);
            int i2 = this.rawOffset;
            if (start <= end) {
                if (j >= start && j < end) {
                    i2 += this.dstSavings;
                }
                synchronized (this) {
                    this.cacheYear = i;
                    this.cacheStart = start;
                    this.cacheEnd = end;
                }
            } else {
                if (j < end) {
                    start = getStart(baseCalendar, date, i - 1);
                    if (j >= start) {
                        i2 += this.dstSavings;
                    }
                } else if (j >= start) {
                    end = getEnd(baseCalendar, date, i + 1);
                    if (j < end) {
                        i2 += this.dstSavings;
                    }
                }
                if (start <= end) {
                    synchronized (this) {
                        this.cacheYear = ((long) this.startYear) - 1;
                        this.cacheStart = start;
                        this.cacheEnd = end;
                    }
                }
            }
            return i2;
        }
    }

    private long getStart(BaseCalendar baseCalendar, BaseCalendar.Date date, int i) {
        int i2 = this.startTime;
        if (this.startTimeMode != 2) {
            i2 -= this.rawOffset;
        }
        return getTransition(baseCalendar, date, this.startMode, i, this.startMonth, this.startDay, this.startDayOfWeek, i2);
    }

    private long getEnd(BaseCalendar baseCalendar, BaseCalendar.Date date, int i) {
        int i2 = this.endTime;
        if (this.endTimeMode != 2) {
            i2 -= this.rawOffset;
        }
        if (this.endTimeMode == 0) {
            i2 -= this.dstSavings;
        }
        return getTransition(baseCalendar, date, this.endMode, i, this.endMonth, this.endDay, this.endDayOfWeek, i2);
    }

    private long getTransition(BaseCalendar baseCalendar, BaseCalendar.Date date, int i, int i2, int i3, int i4, int i5, int i6) {
        date.setNormalizedYear(i2);
        date.setMonth(i3 + 1);
        switch (i) {
            case 1:
                date.setDayOfMonth(i4);
                break;
            case 2:
                date.setDayOfMonth(1);
                if (i4 < 0) {
                    date.setDayOfMonth(baseCalendar.getMonthLength(date));
                }
                date = (BaseCalendar.Date) baseCalendar.getNthDayOfWeek(i4, i5, date);
                break;
            case 3:
                date.setDayOfMonth(i4);
                date = (BaseCalendar.Date) baseCalendar.getNthDayOfWeek(1, i5, date);
                break;
            case 4:
                date.setDayOfMonth(i4);
                date = (BaseCalendar.Date) baseCalendar.getNthDayOfWeek(-1, i5, date);
                break;
        }
        return baseCalendar.getTime(date) + ((long) i6);
    }

    @Override
    public int getRawOffset() {
        return this.rawOffset;
    }

    @Override
    public void setRawOffset(int i) {
        this.rawOffset = i;
    }

    public void setDSTSavings(int i) {
        if (i <= 0) {
            throw new IllegalArgumentException("Illegal daylight saving value: " + i);
        }
        this.dstSavings = i;
    }

    @Override
    public int getDSTSavings() {
        if (this.useDaylight) {
            return this.dstSavings;
        }
        return 0;
    }

    @Override
    public boolean useDaylightTime() {
        return this.useDaylight;
    }

    @Override
    public boolean observesDaylightTime() {
        return useDaylightTime();
    }

    @Override
    public boolean inDaylightTime(Date date) {
        return getOffset(date.getTime()) != this.rawOffset;
    }

    @Override
    public Object clone() {
        return super.clone();
    }

    public synchronized int hashCode() {
        return (((((((this.startMonth ^ this.startDay) ^ this.startDayOfWeek) ^ this.startTime) ^ this.endMonth) ^ this.endDay) ^ this.endDayOfWeek) ^ this.endTime) ^ this.rawOffset;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SimpleTimeZone)) {
            return false;
        }
        SimpleTimeZone simpleTimeZone = (SimpleTimeZone) obj;
        return getID().equals(simpleTimeZone.getID()) && hasSameRules(simpleTimeZone);
    }

    @Override
    public boolean hasSameRules(TimeZone timeZone) {
        if (this == timeZone) {
            return true;
        }
        if (!(timeZone instanceof SimpleTimeZone)) {
            return false;
        }
        SimpleTimeZone simpleTimeZone = (SimpleTimeZone) timeZone;
        if (this.rawOffset == simpleTimeZone.rawOffset && this.useDaylight == simpleTimeZone.useDaylight) {
            if (!this.useDaylight) {
                return true;
            }
            if (this.dstSavings == simpleTimeZone.dstSavings && this.startMode == simpleTimeZone.startMode && this.startMonth == simpleTimeZone.startMonth && this.startDay == simpleTimeZone.startDay && this.startDayOfWeek == simpleTimeZone.startDayOfWeek && this.startTime == simpleTimeZone.startTime && this.startTimeMode == simpleTimeZone.startTimeMode && this.endMode == simpleTimeZone.endMode && this.endMonth == simpleTimeZone.endMonth && this.endDay == simpleTimeZone.endDay && this.endDayOfWeek == simpleTimeZone.endDayOfWeek && this.endTime == simpleTimeZone.endTime && this.endTimeMode == simpleTimeZone.endTimeMode && this.startYear == simpleTimeZone.startYear) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        return getClass().getName() + "[id=" + getID() + ",offset=" + this.rawOffset + ",dstSavings=" + this.dstSavings + ",useDaylight=" + this.useDaylight + ",startYear=" + this.startYear + ",startMode=" + this.startMode + ",startMonth=" + this.startMonth + ",startDay=" + this.startDay + ",startDayOfWeek=" + this.startDayOfWeek + ",startTime=" + this.startTime + ",startTimeMode=" + this.startTimeMode + ",endMode=" + this.endMode + ",endMonth=" + this.endMonth + ",endDay=" + this.endDay + ",endDayOfWeek=" + this.endDayOfWeek + ",endTime=" + this.endTime + ",endTimeMode=" + this.endTimeMode + ']';
    }

    private synchronized void invalidateCache() {
        this.cacheYear = this.startYear - 1;
        this.cacheEnd = 0L;
        this.cacheStart = 0L;
    }

    private void decodeRules() {
        decodeStartRule();
        decodeEndRule();
    }

    private void decodeStartRule() {
        this.useDaylight = (this.startDay == 0 || this.endDay == 0) ? false : true;
        if (this.startDay != 0) {
            if (this.startMonth < 0 || this.startMonth > 11) {
                throw new IllegalArgumentException("Illegal start month " + this.startMonth);
            }
            if (this.startTime < 0 || this.startTime > millisPerDay) {
                throw new IllegalArgumentException("Illegal start time " + this.startTime);
            }
            if (this.startDayOfWeek == 0) {
                this.startMode = 1;
            } else {
                if (this.startDayOfWeek > 0) {
                    this.startMode = 2;
                } else {
                    this.startDayOfWeek = -this.startDayOfWeek;
                    if (this.startDay > 0) {
                        this.startMode = 3;
                    } else {
                        this.startDay = -this.startDay;
                        this.startMode = 4;
                    }
                }
                if (this.startDayOfWeek > 7) {
                    throw new IllegalArgumentException("Illegal start day of week " + this.startDayOfWeek);
                }
            }
            if (this.startMode == 2) {
                if (this.startDay < -5 || this.startDay > 5) {
                    throw new IllegalArgumentException("Illegal start day of week in month " + this.startDay);
                }
                return;
            }
            if (this.startDay < 1 || this.startDay > staticMonthLength[this.startMonth]) {
                throw new IllegalArgumentException("Illegal start day " + this.startDay);
            }
        }
    }

    private void decodeEndRule() {
        this.useDaylight = (this.startDay == 0 || this.endDay == 0) ? false : true;
        if (this.endDay != 0) {
            if (this.endMonth < 0 || this.endMonth > 11) {
                throw new IllegalArgumentException("Illegal end month " + this.endMonth);
            }
            if (this.endTime < 0 || this.endTime > millisPerDay) {
                throw new IllegalArgumentException("Illegal end time " + this.endTime);
            }
            if (this.endDayOfWeek == 0) {
                this.endMode = 1;
            } else {
                if (this.endDayOfWeek > 0) {
                    this.endMode = 2;
                } else {
                    this.endDayOfWeek = -this.endDayOfWeek;
                    if (this.endDay > 0) {
                        this.endMode = 3;
                    } else {
                        this.endDay = -this.endDay;
                        this.endMode = 4;
                    }
                }
                if (this.endDayOfWeek > 7) {
                    throw new IllegalArgumentException("Illegal end day of week " + this.endDayOfWeek);
                }
            }
            if (this.endMode == 2) {
                if (this.endDay < -5 || this.endDay > 5) {
                    throw new IllegalArgumentException("Illegal end day of week in month " + this.endDay);
                }
                return;
            }
            if (this.endDay < 1 || this.endDay > staticMonthLength[this.endMonth]) {
                throw new IllegalArgumentException("Illegal end day " + this.endDay);
            }
        }
    }

    private void makeRulesCompatible() {
        int i = this.startMode;
        if (i == 1) {
            this.startDay = (this.startDay / 7) + 1;
            this.startDayOfWeek = 1;
        } else {
            switch (i) {
                case 3:
                    if (this.startDay != 1) {
                        this.startDay = (this.startDay / 7) + 1;
                    }
                    break;
                case 4:
                    if (this.startDay >= 30) {
                        this.startDay = -1;
                    } else {
                        this.startDay = (this.startDay / 7) + 1;
                    }
                    break;
            }
        }
        int i2 = this.endMode;
        if (i2 == 1) {
            this.endDay = (this.endDay / 7) + 1;
            this.endDayOfWeek = 1;
        } else {
            switch (i2) {
                case 3:
                    if (this.endDay != 1) {
                        this.endDay = (this.endDay / 7) + 1;
                    }
                    break;
                case 4:
                    if (this.endDay >= 30) {
                        this.endDay = -1;
                    } else {
                        this.endDay = (this.endDay / 7) + 1;
                    }
                    break;
            }
        }
        if (this.startTimeMode == 2) {
            this.startTime += this.rawOffset;
        }
        while (this.startTime < 0) {
            this.startTime += millisPerDay;
            this.startDayOfWeek = ((this.startDayOfWeek + 5) % 7) + 1;
        }
        while (this.startTime >= millisPerDay) {
            this.startTime -= millisPerDay;
            this.startDayOfWeek = (this.startDayOfWeek % 7) + 1;
        }
        switch (this.endTimeMode) {
            case 1:
                this.endTime += this.dstSavings;
                break;
            case 2:
                this.endTime += this.rawOffset + this.dstSavings;
                break;
        }
        while (this.endTime < 0) {
            this.endTime += millisPerDay;
            this.endDayOfWeek = ((this.endDayOfWeek + 5) % 7) + 1;
        }
        while (this.endTime >= millisPerDay) {
            this.endTime -= millisPerDay;
            this.endDayOfWeek = (this.endDayOfWeek % 7) + 1;
        }
    }

    private byte[] packRules() {
        return new byte[]{(byte) this.startDay, (byte) this.startDayOfWeek, (byte) this.endDay, (byte) this.endDayOfWeek, (byte) this.startTimeMode, (byte) this.endTimeMode};
    }

    private void unpackRules(byte[] bArr) {
        this.startDay = bArr[0];
        this.startDayOfWeek = bArr[1];
        this.endDay = bArr[2];
        this.endDayOfWeek = bArr[3];
        if (bArr.length >= 6) {
            this.startTimeMode = bArr[4];
            this.endTimeMode = bArr[5];
        }
    }

    private int[] packTimes() {
        return new int[]{this.startTime, this.endTime};
    }

    private void unpackTimes(int[] iArr) {
        this.startTime = iArr[0];
        this.endTime = iArr[1];
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        byte[] bArrPackRules = packRules();
        int[] iArrPackTimes = packTimes();
        makeRulesCompatible();
        objectOutputStream.defaultWriteObject();
        objectOutputStream.writeInt(bArrPackRules.length);
        objectOutputStream.write(bArrPackRules);
        objectOutputStream.writeObject(iArrPackTimes);
        unpackRules(bArrPackRules);
        unpackTimes(iArrPackTimes);
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        if (this.serialVersionOnStream < 1) {
            if (this.startDayOfWeek == 0) {
                this.startDayOfWeek = 1;
            }
            if (this.endDayOfWeek == 0) {
                this.endDayOfWeek = 1;
            }
            this.endMode = 2;
            this.startMode = 2;
            this.dstSavings = millisPerHour;
        } else {
            byte[] bArr = new byte[objectInputStream.readInt()];
            objectInputStream.readFully(bArr);
            unpackRules(bArr);
        }
        if (this.serialVersionOnStream >= 2) {
            unpackTimes((int[]) objectInputStream.readObject());
        }
        this.serialVersionOnStream = 2;
    }
}
