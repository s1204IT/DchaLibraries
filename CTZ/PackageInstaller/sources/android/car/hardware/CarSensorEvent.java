package android.car.hardware;

import android.os.Parcel;
import android.os.Parcelable;

public class CarSensorEvent implements Parcelable {
    public static final Parcelable.Creator<CarSensorEvent> CREATOR = null;
    public final float[] floatValues = null;
    public final int[] intValues = null;
    public final long[] longValues = null;

    CarSensorEvent() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public int describeContents() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        throw new RuntimeException("Stub!");
    }
}
