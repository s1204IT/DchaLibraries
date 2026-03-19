package android.location;

import android.os.Parcel;
import android.os.Parcelable;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public final class GnssMeasurementsEvent implements Parcelable {
    public static final Parcelable.Creator<GnssMeasurementsEvent> CREATOR = new Parcelable.Creator<GnssMeasurementsEvent>() {
        @Override
        public GnssMeasurementsEvent createFromParcel(Parcel in) {
            ClassLoader classLoader = getClass().getClassLoader();
            GnssClock clock = (GnssClock) in.readParcelable(classLoader);
            int measurementsLength = in.readInt();
            GnssMeasurement[] measurementsArray = new GnssMeasurement[measurementsLength];
            in.readTypedArray(measurementsArray, GnssMeasurement.CREATOR);
            return new GnssMeasurementsEvent(clock, measurementsArray);
        }

        @Override
        public GnssMeasurementsEvent[] newArray(int size) {
            return new GnssMeasurementsEvent[size];
        }
    };
    public static final int STATUS_GNSS_LOCATION_DISABLED = 2;
    public static final int STATUS_NOT_SUPPORTED = 0;
    public static final int STATUS_READY = 1;
    private final GnssClock mClock;
    private final Collection<GnssMeasurement> mReadOnlyMeasurements;

    public static abstract class Callback {
        public static final int STATUS_LOCATION_DISABLED = 2;
        public static final int STATUS_NOT_SUPPORTED = 0;
        public static final int STATUS_READY = 1;

        public void onGnssMeasurementsReceived(GnssMeasurementsEvent eventArgs) {
        }

        public void onStatusChanged(int status) {
        }
    }

    public GnssMeasurementsEvent(GnssClock clock, GnssMeasurement[] measurements) {
        if (clock == null) {
            throw new InvalidParameterException("Parameter 'clock' must not be null.");
        }
        if (measurements == null || measurements.length == 0) {
            throw new InvalidParameterException("Parameter 'measurements' must not be null or empty.");
        }
        this.mClock = clock;
        Collection<GnssMeasurement> measurementCollection = Arrays.asList(measurements);
        this.mReadOnlyMeasurements = Collections.unmodifiableCollection(measurementCollection);
    }

    public GnssClock getClock() {
        return this.mClock;
    }

    public Collection<GnssMeasurement> getMeasurements() {
        return this.mReadOnlyMeasurements;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeParcelable(this.mClock, flags);
        int measurementsCount = this.mReadOnlyMeasurements.size();
        GnssMeasurement[] measurementsArray = (GnssMeasurement[]) this.mReadOnlyMeasurements.toArray(new GnssMeasurement[measurementsCount]);
        parcel.writeInt(measurementsArray.length);
        parcel.writeTypedArray(measurementsArray, flags);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder("[ GnssMeasurementsEvent:\n\n");
        builder.append(this.mClock.toString());
        builder.append("\n");
        for (GnssMeasurement measurement : this.mReadOnlyMeasurements) {
            builder.append(measurement.toString());
            builder.append("\n");
        }
        builder.append("]");
        return builder.toString();
    }
}
