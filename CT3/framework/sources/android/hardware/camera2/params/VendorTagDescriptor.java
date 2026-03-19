package android.hardware.camera2.params;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public final class VendorTagDescriptor implements Parcelable {
    public static final Parcelable.Creator<VendorTagDescriptor> CREATOR = new Parcelable.Creator<VendorTagDescriptor>() {
        @Override
        public VendorTagDescriptor createFromParcel(Parcel source) {
            try {
                VendorTagDescriptor vendorDescriptor = new VendorTagDescriptor(source, null);
                return vendorDescriptor;
            } catch (Exception e) {
                Log.e(VendorTagDescriptor.TAG, "Exception creating VendorTagDescriptor from parcel", e);
                return null;
            }
        }

        @Override
        public VendorTagDescriptor[] newArray(int size) {
            return new VendorTagDescriptor[size];
        }
    };
    private static final String TAG = "VendorTagDescriptor";

    VendorTagDescriptor(Parcel source, VendorTagDescriptor vendorTagDescriptor) {
        this(source);
    }

    private VendorTagDescriptor(Parcel source) {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (dest != null) {
        } else {
            throw new IllegalArgumentException("dest must not be null");
        }
    }
}
