package android.hardware.camera2.params;

import android.os.Parcel;
import android.os.Parcelable;

public final class VendorTagDescriptor implements Parcelable {
    public static final Parcelable.Creator<VendorTagDescriptor> CREATOR = new Parcelable.Creator<VendorTagDescriptor>() {
        @Override
        public VendorTagDescriptor createFromParcel(Parcel parcel) {
            return new VendorTagDescriptor(parcel);
        }

        @Override
        public VendorTagDescriptor[] newArray(int i) {
            return new VendorTagDescriptor[i];
        }
    };
    private static final String TAG = "VendorTagDescriptor";

    private VendorTagDescriptor(Parcel parcel) {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        if (parcel == null) {
            throw new IllegalArgumentException("dest must not be null");
        }
    }
}
