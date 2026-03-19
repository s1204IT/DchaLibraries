package com.android.deskclock.data;

import android.content.Context;
import android.os.SystemClock;
import android.text.format.DateFormat;
import java.util.Calendar;

final class TimeModel {
    private final Context mContext;

    TimeModel(Context context) {
        this.mContext = context;
    }

    long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    long elapsedRealtime() {
        return SystemClock.elapsedRealtime();
    }

    boolean is24HourFormat() {
        return DateFormat.is24HourFormat(this.mContext);
    }

    Calendar getCalendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentTimeMillis());
        return calendar;
    }
}
