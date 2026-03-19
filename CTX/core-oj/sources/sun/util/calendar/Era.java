package sun.util.calendar;

import java.util.Locale;
import java.util.TimeZone;
import sun.util.calendar.Gregorian;

public final class Era {
    private final String abbr;
    private int hash = 0;
    private final boolean localTime;
    private final String name;
    private final long since;
    private final CalendarDate sinceDate;

    public Era(String str, String str2, long j, boolean z) {
        this.name = str;
        this.abbr = str2;
        this.since = j;
        this.localTime = z;
        Gregorian gregorianCalendar = CalendarSystem.getGregorianCalendar();
        Gregorian.Date dateNewCalendarDate = gregorianCalendar.newCalendarDate((TimeZone) null);
        gregorianCalendar.getCalendarDate(j, (CalendarDate) dateNewCalendarDate);
        this.sinceDate = new ImmutableGregorianDate(dateNewCalendarDate);
    }

    public String getName() {
        return this.name;
    }

    public String getDisplayName(Locale locale) {
        return this.name;
    }

    public String getAbbreviation() {
        return this.abbr;
    }

    public String getDiaplayAbbreviation(Locale locale) {
        return this.abbr;
    }

    public long getSince(TimeZone timeZone) {
        if (timeZone == null || !this.localTime) {
            return this.since;
        }
        return this.since - ((long) timeZone.getOffset(this.since));
    }

    public CalendarDate getSinceDate() {
        return this.sinceDate;
    }

    public boolean isLocalTime() {
        return this.localTime;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Era)) {
            return false;
        }
        Era era = (Era) obj;
        return this.name.equals(era.name) && this.abbr.equals(era.abbr) && this.since == era.since && this.localTime == era.localTime;
    }

    public int hashCode() {
        if (this.hash == 0) {
            this.hash = (((this.name.hashCode() ^ this.abbr.hashCode()) ^ ((int) this.since)) ^ ((int) (this.since >> 32))) ^ (this.localTime ? 1 : 0);
        }
        return this.hash;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        sb.append(getName());
        sb.append(" (");
        sb.append(getAbbreviation());
        sb.append(')');
        sb.append(" since ");
        sb.append((Object) getSinceDate());
        if (this.localTime) {
            sb.setLength(sb.length() - 1);
            sb.append(" local time");
        }
        sb.append(']');
        return sb.toString();
    }
}
