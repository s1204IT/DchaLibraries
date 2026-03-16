package com.android.deskclock;

import android.content.Context;
import com.android.deskclock.provider.Alarm;

public interface DeskClockExtensions {
    void addAlarm(Context context, Alarm alarm);

    void deleteAlarm(Context context, long j);
}
