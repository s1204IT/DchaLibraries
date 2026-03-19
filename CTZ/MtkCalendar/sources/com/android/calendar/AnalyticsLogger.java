package com.android.calendar;

import android.content.Context;

public interface AnalyticsLogger {
    boolean initialize(Context context);

    void trackView(String str);
}
