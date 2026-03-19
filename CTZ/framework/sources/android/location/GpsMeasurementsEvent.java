package android.location;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

@SystemApi
public class GpsMeasurementsEvent implements Parcelable {
    public static final Parcelable.Creator<GpsMeasurementsEvent> CREATOR = new Parcelable.Creator<GpsMeasurementsEvent>() {
        @Override
        public GpsMeasurementsEvent createFromParcel(Parcel parcel) {
            GpsClock gpsClock = (GpsClock) parcel.readParcelable(getClass().getClassLoader());
            GpsMeasurement[] gpsMeasurementArr = new GpsMeasurement[parcel.readInt()];
            parcel.readTypedArray(gpsMeasurementArr, GpsMeasurement.CREATOR);
            return new GpsMeasurementsEvent(gpsClock, gpsMeasurementArr);
        }

        @Override
        public GpsMeasurementsEvent[] newArray(int i) {
            return new GpsMeasurementsEvent[i];
        }
    };
    public static final int STATUS_GPS_LOCATION_DISABLED = 2;
    public static final int STATUS_NOT_SUPPORTED = 0;
    public static final int STATUS_READY = 1;
    private final GpsClock mClock;
    private final Collection<GpsMeasurement> mReadOnlyMeasurements;

    @SystemApi
    public interface Listener {
        void onGpsMeasurementsReceived(GpsMeasurementsEvent gpsMeasurementsEvent);

        void onStatusChanged(int i);
    }

    public GpsMeasurementsEvent(GpsClock gpsClock, GpsMeasurement[] gpsMeasurementArr) {
        if (gpsClock == null) {
            throw new InvalidParameterException("Parameter 'clock' must not be null.");
        }
        if (gpsMeasurementArr == null || gpsMeasurementArr.length == 0) {
            throw new InvalidParameterException("Parameter 'measurements' must not be null or empty.");
        }
        this.mClock = gpsClock;
        this.mReadOnlyMeasurements = Collections.unmodifiableCollection(Arrays.asList(gpsMeasurementArr));
    }

    public GpsClock getClock() {
        return this.mClock;
    }

    public Collection<GpsMeasurement> getMeasurements() {
        return this.mReadOnlyMeasurements;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.mClock, i);
        GpsMeasurement[] gpsMeasurementArr = (GpsMeasurement[]) this.mReadOnlyMeasurements.toArray(new GpsMeasurement[this.mReadOnlyMeasurements.size()]);
        parcel.writeInt(gpsMeasurementArr.length);
        parcel.writeTypedArray(gpsMeasurementArr, i);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("[ GpsMeasurementsEvent:\n\n");
        sb.append(this.mClock.toString());
        sb.append("\n");
        Iterator<GpsMeasurement> it = this.mReadOnlyMeasurements.iterator();
        while (it.hasNext()) {
            sb.append(it.next().toString());
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }
}
