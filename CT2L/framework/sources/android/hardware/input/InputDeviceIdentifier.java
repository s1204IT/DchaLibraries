package android.hardware.input;

import android.os.Parcel;
import android.os.Parcelable;

public final class InputDeviceIdentifier implements Parcelable {
    public static final Parcelable.Creator<InputDeviceIdentifier> CREATOR = new Parcelable.Creator<InputDeviceIdentifier>() {
        @Override
        public InputDeviceIdentifier createFromParcel(Parcel source) {
            return new InputDeviceIdentifier(source);
        }

        @Override
        public InputDeviceIdentifier[] newArray(int size) {
            return new InputDeviceIdentifier[size];
        }
    };
    private final String mDescriptor;
    private final int mProductId;
    private final int mVendorId;

    public InputDeviceIdentifier(String descriptor, int vendorId, int productId) {
        this.mDescriptor = descriptor;
        this.mVendorId = vendorId;
        this.mProductId = productId;
    }

    private InputDeviceIdentifier(Parcel src) {
        this.mDescriptor = src.readString();
        this.mVendorId = src.readInt();
        this.mProductId = src.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mDescriptor);
        dest.writeInt(this.mVendorId);
        dest.writeInt(this.mProductId);
    }

    public String getDescriptor() {
        return this.mDescriptor;
    }

    public int getVendorId() {
        return this.mVendorId;
    }

    public int getProductId() {
        return this.mProductId;
    }
}
