package android.car.drivingstate;

import android.os.Parcel;
import android.os.Parcelable;

public class CarUxRestrictions implements Parcelable {
    public static final Parcelable.Creator<CarUxRestrictions> CREATOR = null;

    public boolean isRequiresDistractionOptimization() {
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

    public String toString() {
        throw new RuntimeException("Stub!");
    }
}
