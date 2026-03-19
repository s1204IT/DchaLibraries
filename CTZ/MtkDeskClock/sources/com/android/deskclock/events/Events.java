package com.android.deskclock.events;

import android.support.annotation.StringRes;
import com.android.deskclock.R;
import com.android.deskclock.controller.Controller;

public final class Events {
    public static final String EXTRA_EVENT_LABEL = "com.android.deskclock.extra.EVENT_LABEL";

    public static void sendAlarmEvent(@StringRes int i, @StringRes int i2) {
        sendEvent(R.string.category_alarm, i, i2);
    }

    public static void sendClockEvent(@StringRes int i, @StringRes int i2) {
        sendEvent(R.string.category_clock, i, i2);
    }

    public static void sendTimerEvent(@StringRes int i, @StringRes int i2) {
        sendEvent(R.string.category_timer, i, i2);
    }

    public static void sendStopwatchEvent(@StringRes int i, @StringRes int i2) {
        sendEvent(R.string.category_stopwatch, i, i2);
    }

    public static void sendScreensaverEvent(@StringRes int i, @StringRes int i2) {
        sendEvent(R.string.category_screensaver, i, i2);
    }

    public static void sendEvent(@StringRes int i, @StringRes int i2, @StringRes int i3) {
        Controller.getController().sendEvent(i, i2, i3);
    }
}
