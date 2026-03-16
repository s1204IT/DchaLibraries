package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.net.HttpURLConnection;

public class SimpleTimeZone extends TimeZone {
    private static final int DOM_MODE = 1;
    private static final int DOW_GE_DOM_MODE = 3;
    private static final int DOW_IN_MONTH_MODE = 2;
    private static final int DOW_LE_DOM_MODE = 4;
    public static final int STANDARD_TIME = 1;
    public static final int UTC_TIME = 2;
    public static final int WALL_TIME = 0;
    private static final ObjectStreamField[] serialPersistentFields = {new ObjectStreamField("dstSavings", Integer.TYPE), new ObjectStreamField("endDay", Integer.TYPE), new ObjectStreamField("endDayOfWeek", Integer.TYPE), new ObjectStreamField("endMode", Integer.TYPE), new ObjectStreamField("endMonth", Integer.TYPE), new ObjectStreamField("endTime", Integer.TYPE), new ObjectStreamField("monthLength", (Class<?>) byte[].class), new ObjectStreamField("rawOffset", Integer.TYPE), new ObjectStreamField("serialVersionOnStream", Integer.TYPE), new ObjectStreamField("startDay", Integer.TYPE), new ObjectStreamField("startDayOfWeek", Integer.TYPE), new ObjectStreamField("startMode", Integer.TYPE), new ObjectStreamField("startMonth", Integer.TYPE), new ObjectStreamField("startTime", Integer.TYPE), new ObjectStreamField("startYear", Integer.TYPE), new ObjectStreamField("useDaylight", Boolean.TYPE)};
    private static final long serialVersionUID = -403250971215465050L;
    private int dstSavings;
    private int endDay;
    private int endDayOfWeek;
    private int endMode;
    private int endMonth;
    private int endTime;
    private int rawOffset;
    private int startDay;
    private int startDayOfWeek;
    private int startMode;
    private int startMonth;
    private int startTime;
    private int startYear;
    private boolean useDaylight;

    public SimpleTimeZone(int offset, String name) {
        this.dstSavings = Grego.MILLIS_PER_HOUR;
        setID(name);
        this.rawOffset = offset;
    }

    public SimpleTimeZone(int offset, String name, int startMonth, int startDay, int startDayOfWeek, int startTime, int endMonth, int endDay, int endDayOfWeek, int endTime) {
        this(offset, name, startMonth, startDay, startDayOfWeek, startTime, endMonth, endDay, endDayOfWeek, endTime, Grego.MILLIS_PER_HOUR);
    }

    public SimpleTimeZone(int offset, String name, int startMonth, int startDay, int startDayOfWeek, int startTime, int endMonth, int endDay, int endDayOfWeek, int endTime, int daylightSavings) {
        this(offset, name);
        if (daylightSavings <= 0) {
            throw new IllegalArgumentException("Invalid daylightSavings: " + daylightSavings);
        }
        this.dstSavings = daylightSavings;
        this.startMonth = startMonth;
        this.startDay = startDay;
        this.startDayOfWeek = startDayOfWeek;
        this.startTime = startTime;
        setStartMode();
        this.endMonth = endMonth;
        this.endDay = endDay;
        this.endDayOfWeek = endDayOfWeek;
        this.endTime = endTime;
        setEndMode();
    }

    public SimpleTimeZone(int offset, String name, int startMonth, int startDay, int startDayOfWeek, int startTime, int startTimeMode, int endMonth, int endDay, int endDayOfWeek, int endTime, int endTimeMode, int daylightSavings) {
        this(offset, name, startMonth, startDay, startDayOfWeek, startTime, endMonth, endDay, endDayOfWeek, endTime, daylightSavings);
        this.startMode = startTimeMode;
        this.endMode = endTimeMode;
    }

    @Override
    public Object clone() {
        SimpleTimeZone zone = (SimpleTimeZone) super.clone();
        return zone;
    }

    public boolean equals(Object object) {
        if (!(object instanceof SimpleTimeZone)) {
            return false;
        }
        SimpleTimeZone tz = (SimpleTimeZone) object;
        if (getID().equals(tz.getID()) && this.rawOffset == tz.rawOffset && this.useDaylight == tz.useDaylight) {
            return !this.useDaylight || (this.startYear == tz.startYear && this.startMonth == tz.startMonth && this.startDay == tz.startDay && this.startMode == tz.startMode && this.startDayOfWeek == tz.startDayOfWeek && this.startTime == tz.startTime && this.endMonth == tz.endMonth && this.endDay == tz.endDay && this.endDayOfWeek == tz.endDayOfWeek && this.endTime == tz.endTime && this.endMode == tz.endMode && this.dstSavings == tz.dstSavings);
        }
        return false;
    }

    @Override
    public int getDSTSavings() {
        if (this.useDaylight) {
            return this.dstSavings;
        }
        return 0;
    }

    @Override
    public int getOffset(int era, int year, int month, int day, int dayOfWeek, int time) {
        if (era != 0 && era != 1) {
            throw new IllegalArgumentException("Invalid era: " + era);
        }
        checkRange(month, dayOfWeek, time);
        if (month != 1 || day != 29 || !isLeapYear(year)) {
            checkDay(month, day);
        }
        if (!useDaylightTime() || era != 1 || year < this.startYear) {
            return this.rawOffset;
        }
        if (this.endMonth < this.startMonth) {
            if (month > this.endMonth && month < this.startMonth) {
                return this.rawOffset;
            }
        } else if (month < this.startMonth || month > this.endMonth) {
            return this.rawOffset;
        }
        int ruleDay = 0;
        int firstDayOfMonth = mod7(dayOfWeek - day);
        if (month == this.startMonth) {
            switch (this.startMode) {
                case 1:
                    ruleDay = this.startDay;
                    break;
                case 2:
                    if (this.startDay >= 0) {
                        ruleDay = mod7(this.startDayOfWeek - firstDayOfMonth) + 1 + ((this.startDay - 1) * 7);
                    } else {
                        int i = GregorianCalendar.DaysInMonth[this.startMonth];
                        if (this.startMonth == 1 && isLeapYear(year)) {
                            i++;
                        }
                        ruleDay = i + 1 + mod7(this.startDayOfWeek - (firstDayOfMonth + i)) + (this.startDay * 7);
                    }
                    break;
                case 3:
                    ruleDay = this.startDay + mod7(this.startDayOfWeek - ((this.startDay + firstDayOfMonth) - 1));
                    break;
                case 4:
                    ruleDay = this.startDay + mod7(this.startDayOfWeek - ((this.startDay + firstDayOfMonth) - 1));
                    if (ruleDay != this.startDay) {
                        ruleDay -= 7;
                    }
                    break;
            }
            if (ruleDay > day || (ruleDay == day && time < this.startTime)) {
                return this.rawOffset;
            }
        }
        int ruleTime = this.endTime - this.dstSavings;
        int nextMonth = (month + 1) % 12;
        if (month == this.endMonth || (ruleTime < 0 && nextMonth == this.endMonth)) {
            switch (this.endMode) {
                case 1:
                    ruleDay = this.endDay;
                    break;
                case 2:
                    if (this.endDay >= 0) {
                        ruleDay = mod7(this.endDayOfWeek - firstDayOfMonth) + 1 + ((this.endDay - 1) * 7);
                    } else {
                        int i2 = GregorianCalendar.DaysInMonth[this.endMonth];
                        if (this.endMonth == 1 && isLeapYear(year)) {
                            i2++;
                        }
                        ruleDay = i2 + 1 + mod7(this.endDayOfWeek - (firstDayOfMonth + i2)) + (this.endDay * 7);
                    }
                    break;
                case 3:
                    ruleDay = this.endDay + mod7(this.endDayOfWeek - ((this.endDay + firstDayOfMonth) - 1));
                    break;
                case 4:
                    ruleDay = this.endDay + mod7(this.endDayOfWeek - ((this.endDay + firstDayOfMonth) - 1));
                    if (ruleDay != this.endDay) {
                        ruleDay -= 7;
                    }
                    break;
            }
            int ruleMonth = this.endMonth;
            if (ruleTime < 0) {
                int changeDays = 1 - (ruleTime / Grego.MILLIS_PER_DAY);
                ruleTime = (ruleTime % Grego.MILLIS_PER_DAY) + Grego.MILLIS_PER_DAY;
                ruleDay -= changeDays;
                if (ruleDay <= 0) {
                    ruleMonth--;
                    if (ruleMonth < 0) {
                        ruleMonth = 11;
                    }
                    ruleDay += GregorianCalendar.DaysInMonth[ruleMonth];
                    if (ruleMonth == 1 && isLeapYear(year)) {
                        ruleDay++;
                    }
                }
            }
            if (month == ruleMonth) {
                if (ruleDay < day || (ruleDay == day && time >= ruleTime)) {
                    return this.rawOffset;
                }
            } else if (nextMonth != ruleMonth) {
                return this.rawOffset;
            }
        }
        return this.rawOffset + this.dstSavings;
    }

    @Override
    public int getOffset(long time) {
        if (!useDaylightTime()) {
            return this.rawOffset;
        }
        int[] fields = Grego.timeToFields(((long) this.rawOffset) + time, null);
        return getOffset(1, fields[0], fields[1], fields[2], fields[3], fields[5]);
    }

    @Override
    public int getRawOffset() {
        return this.rawOffset;
    }

    public synchronized int hashCode() {
        int hashCode;
        hashCode = getID().hashCode() + this.rawOffset;
        if (this.useDaylight) {
            hashCode += this.startYear + this.startMonth + this.startDay + this.startDayOfWeek + this.startTime + this.startMode + this.endMonth + this.endDay + this.endDayOfWeek + this.endTime + this.endMode + this.dstSavings;
        }
        return hashCode;
    }

    @Override
    public boolean hasSameRules(TimeZone zone) {
        if (!(zone instanceof SimpleTimeZone)) {
            return false;
        }
        SimpleTimeZone tz = (SimpleTimeZone) zone;
        if (this.useDaylight != tz.useDaylight) {
            return false;
        }
        if (!this.useDaylight) {
            return this.rawOffset == tz.rawOffset;
        }
        return this.rawOffset == tz.rawOffset && this.dstSavings == tz.dstSavings && this.startYear == tz.startYear && this.startMonth == tz.startMonth && this.startDay == tz.startDay && this.startMode == tz.startMode && this.startDayOfWeek == tz.startDayOfWeek && this.startTime == tz.startTime && this.endMonth == tz.endMonth && this.endDay == tz.endDay && this.endDayOfWeek == tz.endDayOfWeek && this.endTime == tz.endTime && this.endMode == tz.endMode;
    }

    @Override
    public boolean inDaylightTime(Date time) {
        return useDaylightTime() && getOffset(time.getTime()) != getRawOffset();
    }

    private boolean isLeapYear(int year) {
        return year > 1582 ? year % 4 == 0 && (year % 100 != 0 || year % HttpURLConnection.HTTP_BAD_REQUEST == 0) : year % 4 == 0;
    }

    private int mod7(int num1) {
        int rem = num1 % 7;
        return (num1 >= 0 || rem >= 0) ? rem : rem + 7;
    }

    public void setDSTSavings(int milliseconds) {
        if (milliseconds <= 0) {
            throw new IllegalArgumentException("milliseconds <= 0: " + milliseconds);
        }
        this.dstSavings = milliseconds;
    }

    private void checkRange(int month, int dayOfWeek, int time) {
        if (month < 0 || month > 11) {
            throw new IllegalArgumentException("Invalid month: " + month);
        }
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            throw new IllegalArgumentException("Invalid day of week: " + dayOfWeek);
        }
        if (time < 0 || time >= 86400000) {
            throw new IllegalArgumentException("Invalid time: " + time);
        }
    }

    private void checkDay(int month, int day) {
        if (day <= 0 || day > GregorianCalendar.DaysInMonth[month]) {
            throw new IllegalArgumentException("Invalid day of month: " + day);
        }
    }

    private void setEndMode() {
        if (this.endDayOfWeek == 0) {
            this.endMode = 1;
        } else if (this.endDayOfWeek < 0) {
            this.endDayOfWeek = -this.endDayOfWeek;
            if (this.endDay < 0) {
                this.endDay = -this.endDay;
                this.endMode = 4;
            } else {
                this.endMode = 3;
            }
        } else {
            this.endMode = 2;
        }
        this.useDaylight = (this.startDay == 0 || this.endDay == 0) ? false : true;
        if (this.endDay != 0) {
            checkRange(this.endMonth, this.endMode == 1 ? 1 : this.endDayOfWeek, this.endTime);
            if (this.endMode != 2) {
                checkDay(this.endMonth, this.endDay);
            } else if (this.endDay < -5 || this.endDay > 5) {
                throw new IllegalArgumentException("Day of week in month: " + this.endDay);
            }
        }
        if (this.endMode != 1) {
            this.endDayOfWeek--;
        }
    }

    public void setEndRule(int month, int dayOfMonth, int time) {
        this.endMonth = month;
        this.endDay = dayOfMonth;
        this.endDayOfWeek = 0;
        this.endTime = time;
        setEndMode();
    }

    public void setEndRule(int month, int day, int dayOfWeek, int time) {
        this.endMonth = month;
        this.endDay = day;
        this.endDayOfWeek = dayOfWeek;
        this.endTime = time;
        setEndMode();
    }

    public void setEndRule(int month, int day, int dayOfWeek, int time, boolean after) {
        this.endMonth = month;
        if (!after) {
            day = -day;
        }
        this.endDay = day;
        this.endDayOfWeek = -dayOfWeek;
        this.endTime = time;
        setEndMode();
    }

    @Override
    public void setRawOffset(int offset) {
        this.rawOffset = offset;
    }

    private void setStartMode() {
        if (this.startDayOfWeek == 0) {
            this.startMode = 1;
        } else if (this.startDayOfWeek < 0) {
            this.startDayOfWeek = -this.startDayOfWeek;
            if (this.startDay < 0) {
                this.startDay = -this.startDay;
                this.startMode = 4;
            } else {
                this.startMode = 3;
            }
        } else {
            this.startMode = 2;
        }
        this.useDaylight = (this.startDay == 0 || this.endDay == 0) ? false : true;
        if (this.startDay != 0) {
            checkRange(this.startMonth, this.startMode == 1 ? 1 : this.startDayOfWeek, this.startTime);
            if (this.startMode != 2) {
                checkDay(this.startMonth, this.startDay);
            } else if (this.startDay < -5 || this.startDay > 5) {
                throw new IllegalArgumentException("Day of week in month: " + this.startDay);
            }
        }
        if (this.startMode != 1) {
            this.startDayOfWeek--;
        }
    }

    public void setStartRule(int month, int dayOfMonth, int time) {
        this.startMonth = month;
        this.startDay = dayOfMonth;
        this.startDayOfWeek = 0;
        this.startTime = time;
        setStartMode();
    }

    public void setStartRule(int month, int day, int dayOfWeek, int time) {
        this.startMonth = month;
        this.startDay = day;
        this.startDayOfWeek = dayOfWeek;
        this.startTime = time;
        setStartMode();
    }

    public void setStartRule(int month, int day, int dayOfWeek, int time, boolean after) {
        this.startMonth = month;
        if (!after) {
            day = -day;
        }
        this.startDay = day;
        this.startDayOfWeek = -dayOfWeek;
        this.startTime = time;
        setStartMode();
    }

    public void setStartYear(int year) {
        this.startYear = year;
        this.useDaylight = true;
    }

    public String toString() {
        int i = 0;
        StringBuilder sbAppend = new StringBuilder().append(getClass().getName()).append("[id=").append(getID()).append(",offset=").append(this.rawOffset).append(",dstSavings=").append(this.dstSavings).append(",useDaylight=").append(this.useDaylight).append(",startYear=").append(this.startYear).append(",startMode=").append(this.startMode).append(",startMonth=").append(this.startMonth).append(",startDay=").append(this.startDay).append(",startDayOfWeek=").append((!this.useDaylight || this.startMode == 1) ? 0 : this.startDayOfWeek + 1).append(",startTime=").append(this.startTime).append(",endMode=").append(this.endMode).append(",endMonth=").append(this.endMonth).append(",endDay=").append(this.endDay).append(",endDayOfWeek=");
        if (this.useDaylight && this.endMode != 1) {
            i = this.endDayOfWeek + 1;
        }
        return sbAppend.append(i).append(",endTime=").append(this.endTime).append("]").toString();
    }

    @Override
    public boolean useDaylightTime() {
        return this.useDaylight;
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        int sEndDay = this.endDay;
        int sEndDayOfWeek = this.endDayOfWeek + 1;
        int sStartDay = this.startDay;
        int sStartDayOfWeek = this.startDayOfWeek + 1;
        if (this.useDaylight && (this.startMode != 2 || this.endMode != 2)) {
            Calendar cal = new GregorianCalendar(this);
            if (this.endMode != 2) {
                cal.set(2, this.endMonth);
                cal.set(5, this.endDay);
                sEndDay = cal.get(8);
                if (this.endMode == 1) {
                    sEndDayOfWeek = cal.getFirstDayOfWeek();
                }
            }
            if (this.startMode != 2) {
                cal.set(2, this.startMonth);
                cal.set(5, this.startDay);
                sStartDay = cal.get(8);
                if (this.startMode == 1) {
                    sStartDayOfWeek = cal.getFirstDayOfWeek();
                }
            }
        }
        ObjectOutputStream.PutField fields = stream.putFields();
        fields.put("dstSavings", this.dstSavings);
        fields.put("endDay", sEndDay);
        fields.put("endDayOfWeek", sEndDayOfWeek);
        fields.put("endMode", this.endMode);
        fields.put("endMonth", this.endMonth);
        fields.put("endTime", this.endTime);
        fields.put("monthLength", GregorianCalendar.DaysInMonth);
        fields.put("rawOffset", this.rawOffset);
        fields.put("serialVersionOnStream", 1);
        fields.put("startDay", sStartDay);
        fields.put("startDayOfWeek", sStartDayOfWeek);
        fields.put("startMode", this.startMode);
        fields.put("startMonth", this.startMonth);
        fields.put("startTime", this.startTime);
        fields.put("startYear", this.startYear);
        fields.put("useDaylight", this.useDaylight);
        stream.writeFields();
        stream.writeInt(4);
        byte[] values = new byte[4];
        values[0] = (byte) this.startDay;
        values[1] = (byte) (this.startMode == 1 ? 0 : this.startDayOfWeek + 1);
        values[2] = (byte) this.endDay;
        values[3] = (byte) (this.endMode != 1 ? this.endDayOfWeek + 1 : 0);
        stream.write(values);
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = stream.readFields();
        this.rawOffset = fields.get("rawOffset", 0);
        this.useDaylight = fields.get("useDaylight", false);
        if (this.useDaylight) {
            this.endMonth = fields.get("endMonth", 0);
            this.endTime = fields.get("endTime", 0);
            this.startMonth = fields.get("startMonth", 0);
            this.startTime = fields.get("startTime", 0);
            this.startYear = fields.get("startYear", 0);
        }
        if (fields.get("serialVersionOnStream", 0) == 0) {
            if (this.useDaylight) {
                this.endMode = 2;
                this.startMode = 2;
                this.endDay = fields.get("endDay", 0);
                this.endDayOfWeek = fields.get("endDayOfWeek", 0) - 1;
                this.startDay = fields.get("startDay", 0);
                this.startDayOfWeek = fields.get("startDayOfWeek", 0) - 1;
                return;
            }
            return;
        }
        this.dstSavings = fields.get("dstSavings", 0);
        if (this.useDaylight) {
            this.endMode = fields.get("endMode", 0);
            this.startMode = fields.get("startMode", 0);
            int length = stream.readInt();
            byte[] values = new byte[length];
            stream.readFully(values);
            if (length >= 4) {
                this.startDay = values[0];
                this.startDayOfWeek = values[1];
                if (this.startMode != 1) {
                    this.startDayOfWeek--;
                }
                this.endDay = values[2];
                this.endDayOfWeek = values[3];
                if (this.endMode != 1) {
                    this.endDayOfWeek--;
                }
            }
        }
    }
}
