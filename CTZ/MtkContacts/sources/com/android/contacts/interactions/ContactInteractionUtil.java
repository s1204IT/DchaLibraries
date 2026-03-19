package com.android.contacts.interactions;

import android.content.Context;
import android.text.format.DateUtils;
import com.android.contacts.R;
import com.google.common.base.Preconditions;
import java.text.DateFormat;
import java.util.Calendar;

public class ContactInteractionUtil {
    public static String questionMarks(int i) {
        Preconditions.checkArgument(i > 0);
        StringBuilder sb = new StringBuilder("(?");
        for (int i2 = 1; i2 < i; i2++) {
            sb.append(",?");
        }
        sb.append(")");
        return sb.toString();
    }

    public static String formatDateStringFromTimestamp(long j, Context context) {
        return formatDateStringFromTimestamp(j, context, Calendar.getInstance());
    }

    public static String formatDateStringFromTimestamp(long j, Context context, Calendar calendar) {
        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTimeInMillis(j);
        if (compareCalendarDayYear(calendar2, calendar)) {
            return DateFormat.getTimeInstance(3).format(calendar2.getTime());
        }
        return DateUtils.formatDateTime(context, j, 23);
    }

    private static boolean compareCalendarDayYear(Calendar calendar, Calendar calendar2) {
        return calendar.get(1) == calendar2.get(1) && calendar.get(6) == calendar2.get(6);
    }

    public static String formatDuration(long j, Context context) {
        int i = ((int) j) / 3600;
        int i2 = ((int) (j % 3600)) / 60;
        int i3 = (int) (j % 60);
        if (i > 0) {
            return context.getString(R.string.callDurationHourFormat, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3));
        }
        if (i2 > 0) {
            return context.getString(R.string.callDurationMinuteFormat, Integer.valueOf(i2), Integer.valueOf(i3));
        }
        return context.getString(R.string.callDurationSecondFormat, Integer.valueOf(i3));
    }
}
