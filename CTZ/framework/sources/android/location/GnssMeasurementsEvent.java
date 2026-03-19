package android.location;

import android.os.Parcel;
import android.os.Parcelable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

public final class GnssMeasurementsEvent implements Parcelable {
    public static final Parcelable.Creator<GnssMeasurementsEvent> CREATOR = new Parcelable.Creator<GnssMeasurementsEvent>() {
        @Override
        public GnssMeasurementsEvent createFromParcel(Parcel parcel) {
            GnssClock gnssClock = (GnssClock) parcel.readParcelable(getClass().getClassLoader());
            GnssMeasurement[] gnssMeasurementArr = new GnssMeasurement[parcel.readInt()];
            parcel.readTypedArray(gnssMeasurementArr, GnssMeasurement.CREATOR);
            return new GnssMeasurementsEvent(gnssClock, gnssMeasurementArr);
        }

        @Override
        public GnssMeasurementsEvent[] newArray(int i) {
            return new GnssMeasurementsEvent[i];
        }
    };
    private final GnssClock mClock;
    private final Collection<GnssMeasurement> mReadOnlyMeasurements;

    public static abstract class Callback {
        public static final int STATUS_LOCATION_DISABLED = 2;
        public static final int STATUS_NOT_ALLOWED = 3;
        public static final int STATUS_NOT_SUPPORTED = 0;
        public static final int STATUS_READY = 1;

        @Retention(RetentionPolicy.SOURCE)
        public @interface GnssMeasurementsStatus {
        }

        public void onGnssMeasurementsReceived(GnssMeasurementsEvent gnssMeasurementsEvent) {
        }

        public void onStatusChanged(int i) {
        }
    }

    public GnssMeasurementsEvent(GnssClock gnssClock, GnssMeasurement[] gnssMeasurementArr) {
        if (gnssClock == null) {
            throw new InvalidParameterException("Parameter 'clock' must not be null.");
        }
        if (gnssMeasurementArr == null || gnssMeasurementArr.length == 0) {
            this.mReadOnlyMeasurements = Collections.emptyList();
        } else {
            this.mReadOnlyMeasurements = Collections.unmodifiableCollection(Arrays.asList(gnssMeasurementArr));
        }
        this.mClock = gnssClock;
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
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.mClock, i);
        GnssMeasurement[] gnssMeasurementArr = (GnssMeasurement[]) this.mReadOnlyMeasurements.toArray(new GnssMeasurement[this.mReadOnlyMeasurements.size()]);
        parcel.writeInt(gnssMeasurementArr.length);
        parcel.writeTypedArray(gnssMeasurementArr, i);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("[ GnssMeasurementsEvent:\n\n");
        sb.append(this.mClock.toString());
        sb.append("\n");
        Iterator<GnssMeasurement> it = this.mReadOnlyMeasurements.iterator();
        while (it.hasNext()) {
            sb.append(it.next().toString());
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }
}
