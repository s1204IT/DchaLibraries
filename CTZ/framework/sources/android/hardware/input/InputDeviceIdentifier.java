package android.hardware.input;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import java.util.Objects;

public final class InputDeviceIdentifier implements Parcelable {
    public static final Parcelable.Creator<InputDeviceIdentifier> CREATOR = new Parcelable.Creator<InputDeviceIdentifier>() {
        @Override
        public InputDeviceIdentifier createFromParcel(Parcel parcel) {
            return new InputDeviceIdentifier(parcel);
        }

        @Override
        public InputDeviceIdentifier[] newArray(int i) {
            return new InputDeviceIdentifier[i];
        }
    };
    private final String mDescriptor;
    private final int mProductId;
    private final int mVendorId;

    public InputDeviceIdentifier(String str, int i, int i2) {
        this.mDescriptor = str;
        this.mVendorId = i;
        this.mProductId = i2;
    }

    private InputDeviceIdentifier(Parcel parcel) {
        this.mDescriptor = parcel.readString();
        this.mVendorId = parcel.readInt();
        this.mProductId = parcel.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mDescriptor);
        parcel.writeInt(this.mVendorId);
        parcel.writeInt(this.mProductId);
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

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof InputDeviceIdentifier)) {
            return false;
        }
        InputDeviceIdentifier inputDeviceIdentifier = (InputDeviceIdentifier) obj;
        if (this.mVendorId == inputDeviceIdentifier.mVendorId && this.mProductId == inputDeviceIdentifier.mProductId && TextUtils.equals(this.mDescriptor, inputDeviceIdentifier.mDescriptor)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(this.mDescriptor, Integer.valueOf(this.mVendorId), Integer.valueOf(this.mProductId));
    }
}
