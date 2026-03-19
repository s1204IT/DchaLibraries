package com.android.deskclock.controller;

import android.support.annotation.StringRes;
import com.android.deskclock.events.EventTracker;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

class EventController {
    private final Collection<EventTracker> mEventTrackers = new ArrayList();

    EventController() {
    }

    void addEventTracker(EventTracker eventTracker) {
        this.mEventTrackers.add(eventTracker);
    }

    void removeEventTracker(EventTracker eventTracker) {
        this.mEventTrackers.remove(eventTracker);
    }

    void sendEvent(@StringRes int i, @StringRes int i2, @StringRes int i3) {
        Iterator<EventTracker> it = this.mEventTrackers.iterator();
        while (it.hasNext()) {
            it.next().sendEvent(i, i2, i3);
        }
    }
}
