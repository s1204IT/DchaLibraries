package com.android.deskclock.events;

import android.support.annotation.StringRes;

public interface EventTracker {
    void sendEvent(@StringRes int i, @StringRes int i2, @StringRes int i3);
}
