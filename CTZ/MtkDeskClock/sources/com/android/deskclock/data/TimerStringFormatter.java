package com.android.deskclock.data;

import android.content.Context;
import android.support.annotation.StringRes;
import com.android.deskclock.R;
import com.android.deskclock.Utils;

public class TimerStringFormatter {
    public static String formatTimeRemaining(Context context, long j, boolean z) {
        int i;
        int i2;
        int i3 = (int) (j / 3600000);
        int i4 = (int) ((j / 60000) % 60);
        int i5 = (int) ((j / 1000) % 60);
        if (j % 1000 != 0 && z && (i5 = i5 + 1) == 60) {
            i4++;
            if (i4 == 60) {
                i3++;
                i4 = 0;
                i5 = 0;
            } else {
                i5 = 0;
            }
        }
        String numberFormattedQuantityString = Utils.getNumberFormattedQuantityString(context, R.plurals.minutes, i4);
        String numberFormattedQuantityString2 = Utils.getNumberFormattedQuantityString(context, R.plurals.hours, i3);
        String numberFormattedQuantityString3 = Utils.getNumberFormattedQuantityString(context, R.plurals.seconds, i5);
        if (i4 > 1 || i3 > 1 || i5 > 1) {
            i = R.string.timer_remaining_multiple;
        } else {
            i = R.string.timer_remaining_single;
        }
        String string = context.getString(i);
        boolean z2 = i3 > 0;
        boolean z3 = i4 > 0;
        boolean z4 = i5 > 0 && z;
        if (z2) {
            if (z3) {
                if (z4) {
                    i2 = R.string.timer_notifications_hours_minutes_seconds;
                } else {
                    i2 = R.string.timer_notifications_hours_minutes;
                }
            } else if (z4) {
                i2 = R.string.timer_notifications_hours_seconds;
            } else {
                i2 = R.string.timer_notifications_hours;
            }
        } else if (z3) {
            if (z4) {
                i2 = R.string.timer_notifications_minutes_seconds;
            } else {
                i2 = R.string.timer_notifications_minutes;
            }
        } else if (z4) {
            i2 = R.string.timer_notifications_seconds;
        } else if (!z) {
            i2 = R.string.timer_notifications_less_min;
        } else {
            i2 = -1;
        }
        if (i2 == -1) {
            return null;
        }
        return String.format(context.getString(i2), numberFormattedQuantityString2, numberFormattedQuantityString, string, numberFormattedQuantityString3);
    }

    public static String formatString(Context context, @StringRes int i, long j, boolean z) {
        return String.format(context.getString(i), formatTimeRemaining(context, j, z));
    }
}
