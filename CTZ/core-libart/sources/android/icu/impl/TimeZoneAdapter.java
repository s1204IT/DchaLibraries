package android.icu.impl;

import java.util.Date;
import java.util.TimeZone;

public class TimeZoneAdapter extends TimeZone {
    static final long serialVersionUID = -2040072218820018557L;
    private android.icu.util.TimeZone zone;

    public static TimeZone wrap(android.icu.util.TimeZone timeZone) {
        return new TimeZoneAdapter(timeZone);
    }

    public android.icu.util.TimeZone unwrap() {
        return this.zone;
    }

    public TimeZoneAdapter(android.icu.util.TimeZone timeZone) {
        this.zone = timeZone;
        super.setID(timeZone.getID());
    }

    @Override
    public void setID(String str) {
        super.setID(str);
        this.zone.setID(str);
    }

    @Override
    public boolean hasSameRules(TimeZone timeZone) {
        return (timeZone instanceof TimeZoneAdapter) && this.zone.hasSameRules(((TimeZoneAdapter) timeZone).zone);
    }

    @Override
    public int getOffset(int i, int i2, int i3, int i4, int i5, int i6) {
        return this.zone.getOffset(i, i2, i3, i4, i5, i6);
    }

    @Override
    public int getRawOffset() {
        return this.zone.getRawOffset();
    }

    @Override
    public void setRawOffset(int i) {
        this.zone.setRawOffset(i);
    }

    @Override
    public boolean useDaylightTime() {
        return this.zone.useDaylightTime();
    }

    @Override
    public boolean inDaylightTime(Date date) {
        return this.zone.inDaylightTime(date);
    }

    @Override
    public Object clone() {
        return new TimeZoneAdapter((android.icu.util.TimeZone) this.zone.clone());
    }

    public synchronized int hashCode() {
        return this.zone.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj instanceof TimeZoneAdapter) {
            obj = ((TimeZoneAdapter) obj).zone;
        }
        return this.zone.equals(obj);
    }

    public String toString() {
        return "TimeZoneAdapter: " + this.zone.toString();
    }
}
