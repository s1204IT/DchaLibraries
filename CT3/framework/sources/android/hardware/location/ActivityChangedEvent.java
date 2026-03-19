package android.hardware.location;

import android.os.Parcel;
import android.os.Parcelable;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.List;

public class ActivityChangedEvent implements Parcelable {
    public static final Parcelable.Creator<ActivityChangedEvent> CREATOR = new Parcelable.Creator<ActivityChangedEvent>() {
        @Override
        public ActivityChangedEvent createFromParcel(Parcel source) {
            int activityRecognitionEventsLength = source.readInt();
            ActivityRecognitionEvent[] activityRecognitionEvents = new ActivityRecognitionEvent[activityRecognitionEventsLength];
            source.readTypedArray(activityRecognitionEvents, ActivityRecognitionEvent.CREATOR);
            return new ActivityChangedEvent(activityRecognitionEvents);
        }

        @Override
        public ActivityChangedEvent[] newArray(int size) {
            return new ActivityChangedEvent[size];
        }
    };
    private final List<ActivityRecognitionEvent> mActivityRecognitionEvents;

    public ActivityChangedEvent(ActivityRecognitionEvent[] activityRecognitionEvents) {
        if (activityRecognitionEvents == null) {
            throw new InvalidParameterException("Parameter 'activityRecognitionEvents' must not be null.");
        }
        this.mActivityRecognitionEvents = Arrays.asList(activityRecognitionEvents);
    }

    public Iterable<ActivityRecognitionEvent> getActivityRecognitionEvents() {
        return this.mActivityRecognitionEvents;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        ActivityRecognitionEvent[] activityRecognitionEventArray = (ActivityRecognitionEvent[]) this.mActivityRecognitionEvents.toArray(new ActivityRecognitionEvent[0]);
        parcel.writeInt(activityRecognitionEventArray.length);
        parcel.writeTypedArray(activityRecognitionEventArray, flags);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder("[ ActivityChangedEvent:");
        for (ActivityRecognitionEvent event : this.mActivityRecognitionEvents) {
            builder.append("\n    ");
            builder.append(event.toString());
        }
        builder.append("\n]");
        return builder.toString();
    }
}
