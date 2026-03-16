package com.android.calendarcommon2;

import android.text.TextUtils;
import android.text.format.Time;
import android.util.TimeFormatException;
import com.android.calendarcommon2.EventRecurrence;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class RecurrenceSet {
    private static final Pattern IGNORABLE_ICAL_WHITESPACE_RE = Pattern.compile("(?:\\r\\n?|\\n)[ \t]");
    private static final Pattern FOLD_RE = Pattern.compile(".{75}");
    public EventRecurrence[] rrules = null;
    public long[] rdates = null;
    public EventRecurrence[] exrules = null;
    public long[] exdates = null;

    public RecurrenceSet(String rruleStr, String rdateStr, String exruleStr, String exdateStr) throws EventRecurrence.InvalidFormatException {
        init(rruleStr, rdateStr, exruleStr, exdateStr);
    }

    private void init(String rruleStr, String rdateStr, String exruleStr, String exdateStr) throws EventRecurrence.InvalidFormatException {
        if (!TextUtils.isEmpty(rruleStr) || !TextUtils.isEmpty(rdateStr)) {
            if (!TextUtils.isEmpty(rruleStr)) {
                String[] rruleStrs = rruleStr.split("\n");
                this.rrules = new EventRecurrence[rruleStrs.length];
                for (int i = 0; i < rruleStrs.length; i++) {
                    EventRecurrence rrule = new EventRecurrence();
                    rrule.parse(rruleStrs[i]);
                    this.rrules[i] = rrule;
                }
            }
            if (!TextUtils.isEmpty(rdateStr)) {
                this.rdates = parseRecurrenceDates(rdateStr);
            }
            if (!TextUtils.isEmpty(exruleStr)) {
                String[] exruleStrs = exruleStr.split("\n");
                this.exrules = new EventRecurrence[exruleStrs.length];
                for (int i2 = 0; i2 < exruleStrs.length; i2++) {
                    EventRecurrence exrule = new EventRecurrence();
                    exrule.parse(exruleStr);
                    this.exrules[i2] = exrule;
                }
            }
            if (!TextUtils.isEmpty(exdateStr)) {
                List<Long> list = new ArrayList<>();
                String[] arr$ = exdateStr.split("\n");
                for (String exdate : arr$) {
                    long[] dates = parseRecurrenceDates(exdate);
                    for (long date : dates) {
                        list.add(Long.valueOf(date));
                    }
                }
                this.exdates = new long[list.size()];
                int n = list.size();
                for (int i3 = 0; i3 < n; i3++) {
                    this.exdates[i3] = list.get(i3).longValue();
                }
            }
        }
    }

    public static long[] parseRecurrenceDates(String recurrence) throws EventRecurrence.InvalidFormatException {
        String tz = "UTC";
        int tzidx = recurrence.indexOf(";");
        if (tzidx != -1) {
            tz = recurrence.substring(0, tzidx);
            recurrence = recurrence.substring(tzidx + 1);
        }
        Time time = new Time(tz);
        String[] rawDates = recurrence.split(",");
        int n = rawDates.length;
        long[] dates = new long[n];
        for (int i = 0; i < n; i++) {
            try {
                time.parse(rawDates[i]);
                dates[i] = time.toMillis(false);
                time.timezone = tz;
            } catch (TimeFormatException e) {
                throw new EventRecurrence.InvalidFormatException("TimeFormatException thrown when parsing time " + rawDates[i] + " in recurrence " + recurrence);
            }
        }
        return dates;
    }
}
