package android.hardware.camera2.params;

import android.os.Parcel;
import android.os.Parcelable;

public final class VendorTagDescriptorCache implements Parcelable {
    public static final Parcelable.Creator<VendorTagDescriptorCache> CREATOR = new Parcelable.Creator<VendorTagDescriptorCache>() {
        @Override
        public VendorTagDescriptorCache createFromParcel(Parcel parcel) {
            return new VendorTagDescriptorCache(parcel);
        }

        @Override
        public VendorTagDescriptorCache[] newArray(int i) {
            return new VendorTagDescriptorCache[i];
        }
    };
    private static final String TAG = "VendorTagDescriptorCache";

    private VendorTagDescriptorCache(Parcel parcel) {
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
