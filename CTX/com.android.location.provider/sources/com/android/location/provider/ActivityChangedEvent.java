package com.android.location.provider;

import java.security.InvalidParameterException;
import java.util.List;

public class ActivityChangedEvent {
    private final List<ActivityRecognitionEvent> mActivityRecognitionEvents;

    public ActivityChangedEvent(List<ActivityRecognitionEvent> list) {
        if (list == null) {
            throw new InvalidParameterException("Parameter 'activityRecognitionEvents' must not be null.");
        }
        this.mActivityRecognitionEvents = list;
    }

    public Iterable<ActivityRecognitionEvent> getActivityRecognitionEvents() {
        return this.mActivityRecognitionEvents;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("[ ActivityChangedEvent:");
        for (ActivityRecognitionEvent activityRecognitionEvent : this.mActivityRecognitionEvents) {
            sb.append("\n    ");
            sb.append(activityRecognitionEvent.toString());
        }
        sb.append("\n]");
        return sb.toString();
    }
}
