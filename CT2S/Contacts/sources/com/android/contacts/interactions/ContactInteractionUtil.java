package com.android.contacts.interactions;

import android.content.Context;
import android.text.format.DateUtils;
import com.android.contacts.R;
import com.google.common.base.Preconditions;
import java.text.DateFormat;
import java.util.Calendar;

public class ContactInteractionUtil {
    public static String questionMarks(int count) {
        Preconditions.checkArgument(count > 0);
        StringBuilder sb = new StringBuilder("(?");
        for (int i = 1; i < count; i++) {
            sb.append(",?");
        }
        return sb.append(")").toString();
    }

    public static String formatDateStringFromTimestamp(long timestamp, Context context) {
        return formatDateStringFromTimestamp(timestamp, context, Calendar.getInstance());
    }

    public static String formatDateStringFromTimestamp(long timestamp, Context context, Calendar compareCalendar) {
        Calendar interactionCalendar = Calendar.getInstance();
        interactionCalendar.setTimeInMillis(timestamp);
        if (compareCalendarDayYear(interactionCalendar, compareCalendar)) {
            return DateFormat.getTimeInstance(3).format(interactionCalendar.getTime());
        }
        compareCalendar.add(6, -1);
        if (compareCalendarDayYear(interactionCalendar, compareCalendar)) {
            return context.getString(R.string.yesterday);
        }
        compareCalendar.add(6, 2);
        if (compareCalendarDayYear(interactionCalendar, compareCalendar)) {
            return context.getString(R.string.tomorrow);
        }
        return DateUtils.formatDateTime(context, interactionCalendar.getTimeInMillis(), 24);
    }

    private static boolean compareCalendarDayYear(Calendar c1, Calendar c2) {
        return c1.get(1) == c2.get(1) && c1.get(6) == c2.get(6);
    }
}
