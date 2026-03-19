package com.android.deskclock.events;

import android.content.Context;
import android.support.annotation.StringRes;
import com.android.deskclock.LogUtils;

public final class LogEventTracker implements EventTracker {
    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("Events");
    private final Context mContext;

    public LogEventTracker(Context context) {
        this.mContext = context;
    }

    @Override
    public void sendEvent(@StringRes int i, @StringRes int i2, @StringRes int i3) {
        if (i3 == 0) {
            LOGGER.d("[%s] [%s]", safeGetString(i), safeGetString(i2));
        } else {
            LOGGER.d("[%s] [%s] [%s]", safeGetString(i), safeGetString(i2), safeGetString(i3));
        }
    }

    private String safeGetString(@StringRes int i) {
        if (i == 0) {
            return null;
        }
        return this.mContext.getString(i);
    }
}
