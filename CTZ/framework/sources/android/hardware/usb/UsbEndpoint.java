package android.hardware.usb;

import android.os.Parcel;
import android.os.Parcelable;

public class UsbEndpoint implements Parcelable {
    public static final Parcelable.Creator<UsbEndpoint> CREATOR = new Parcelable.Creator<UsbEndpoint>() {
        @Override
        public UsbEndpoint createFromParcel(Parcel parcel) {
            return new UsbEndpoint(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt());
        }

        @Override
        public UsbEndpoint[] newArray(int i) {
            return new UsbEndpoint[i];
        }
    };
    private final int mAddress;
    private final int mAttributes;
    private final int mInterval;
    private final int mMaxPacketSize;

    public UsbEndpoint(int i, int i2, int i3, int i4) {
        this.mAddress = i;
        this.mAttributes = i2;
        this.mMaxPacketSize = i3;
        this.mInterval = i4;
    }

    public int getAddress() {
        return this.mAddress;
    }

    public int getEndpointNumber() {
        return this.mAddress & 15;
    }

    public int getDirection() {
        return this.mAddress & 128;
    }

    public int getAttributes() {
        return this.mAttributes;
    }

    public int getType() {
        return this.mAttributes & 3;
    }

    public int getMaxPacketSize() {
        return this.mMaxPacketSize;
    }

    public int getInterval() {
        return this.mInterval;
    }

    public String toString() {
        return "UsbEndpoint[mAddress=" + this.mAddress + ",mAttributes=" + this.mAttributes + ",mMaxPacketSize=" + this.mMaxPacketSize + ",mInterval=" + this.mInterval + "]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mAddress);
        parcel.writeInt(this.mAttributes);
        parcel.writeInt(this.mMaxPacketSize);
        parcel.writeInt(this.mInterval);
    }
}
