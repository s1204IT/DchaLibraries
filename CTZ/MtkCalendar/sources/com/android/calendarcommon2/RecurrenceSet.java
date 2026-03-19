package com.android.calendarcommon2;

import android.text.TextUtils;
import android.text.format.Time;
import android.util.TimeFormatException;
import com.android.calendarcommon2.EventRecurrence;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class RecurrenceSet {
    private static final Pattern IGNORABLE_ICAL_WHITESPACE_RE = Pattern.compile("(?:\\r\\n?|\\n)[ \t]");
    private static final Pattern FOLD_RE = Pattern.compile(".{75}");
    public EventRecurrence[] rrules = null;
    public long[] rdates = null;
    public EventRecurrence[] exrules = null;
    public long[] exdates = null;

    public RecurrenceSet(String str, String str2, String str3, String str4) throws EventRecurrence.InvalidFormatException {
        init(str, str2, str3, str4);
    }

    private void init(String str, String str2, String str3, String str4) throws EventRecurrence.InvalidFormatException {
        if (!TextUtils.isEmpty(str) || !TextUtils.isEmpty(str2)) {
            this.rrules = parseMultiLineRecurrenceRules(str);
            this.rdates = parseMultiLineRecurrenceDates(str2);
            this.exrules = parseMultiLineRecurrenceRules(str3);
            this.exdates = parseMultiLineRecurrenceDates(str4);
        }
    }

    private EventRecurrence[] parseMultiLineRecurrenceRules(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        String[] strArrSplit = str.split("\n");
        EventRecurrence[] eventRecurrenceArr = new EventRecurrence[strArrSplit.length];
        for (int i = 0; i < strArrSplit.length; i++) {
            EventRecurrence eventRecurrence = new EventRecurrence();
            eventRecurrence.parse(strArrSplit[i]);
            eventRecurrenceArr[i] = eventRecurrence;
        }
        return eventRecurrenceArr;
    }

    private long[] parseMultiLineRecurrenceDates(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        ArrayList arrayList = new ArrayList();
        for (String str2 : str.split("\n")) {
            for (long j : parseRecurrenceDates(str2)) {
                arrayList.add(Long.valueOf(j));
            }
        }
        long[] jArr = new long[arrayList.size()];
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            jArr[i] = ((Long) arrayList.get(i)).longValue();
        }
        return jArr;
    }

    public static long[] parseRecurrenceDates(String str) throws EventRecurrence.InvalidFormatException {
        String strSubstring = "UTC";
        int iIndexOf = str.indexOf(";");
        if (iIndexOf != -1) {
            strSubstring = str.substring(0, iIndexOf);
            str = str.substring(iIndexOf + 1);
        }
        Time time = new Time(strSubstring);
        String[] strArrSplit = str.split(",");
        int length = strArrSplit.length;
        long[] jArr = new long[length];
        for (int i = 0; i < length; i++) {
            try {
                time.parse(strArrSplit[i]);
                jArr[i] = time.toMillis(false);
                time.timezone = strSubstring;
            } catch (TimeFormatException e) {
                throw new EventRecurrence.InvalidFormatException("TimeFormatException thrown when parsing time " + strArrSplit[i] + " in recurrence " + str);
            }
        }
        return jArr;
    }
}
