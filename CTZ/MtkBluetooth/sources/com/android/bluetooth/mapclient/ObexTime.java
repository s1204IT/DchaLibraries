package com.android.bluetooth.mapclient;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ObexTime {
    private Date mDate;

    public ObexTime(String str) {
        Matcher matcher = Pattern.compile("(\\d{4})(\\d{2})(\\d{2})T(\\d{2})(\\d{2})(\\d{2})(([+-])(\\d{2})(\\d{2}))?").matcher(str);
        if (matcher.matches()) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)) - 1, Integer.parseInt(matcher.group(3)), Integer.parseInt(matcher.group(4)), Integer.parseInt(matcher.group(5)), Integer.parseInt(matcher.group(6)));
            if (matcher.group(7) != null) {
                int i = ((Integer.parseInt(matcher.group(9)) * 60) + Integer.parseInt(matcher.group(10))) * 60 * 1000;
                i = matcher.group(8).equals("-") ? -i : i;
                TimeZone timeZone = TimeZone.getTimeZone("UTC");
                timeZone.setRawOffset(i);
                calendar.setTimeZone(timeZone);
            }
            this.mDate = calendar.getTime();
        }
    }

    public ObexTime(Date date) {
        this.mDate = date;
    }

    public Date getTime() {
        return this.mDate;
    }

    public String toString() {
        if (this.mDate == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(this.mDate);
        return String.format(Locale.US, "%04d%02d%02dT%02d%02d%02d", Integer.valueOf(calendar.get(1)), Integer.valueOf(calendar.get(2) + 1), Integer.valueOf(calendar.get(5)), Integer.valueOf(calendar.get(11)), Integer.valueOf(calendar.get(12)), Integer.valueOf(calendar.get(13)));
    }
}
