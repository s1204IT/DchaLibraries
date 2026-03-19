package android.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;

public class SdpRecord implements Parcelable {
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public SdpRecord createFromParcel(Parcel parcel) {
            return new SdpRecord(parcel);
        }

        @Override
        public SdpRecord[] newArray(int i) {
            return new SdpRecord[i];
        }
    };
    private final byte[] mRawData;
    private final int mRawSize;

    public String toString() {
        return "BluetoothSdpRecord [rawData=" + Arrays.toString(this.mRawData) + ", rawSize=" + this.mRawSize + "]";
    }

    public SdpRecord(int i, byte[] bArr) {
        this.mRawData = bArr;
        this.mRawSize = i;
    }

    public SdpRecord(Parcel parcel) {
        this.mRawSize = parcel.readInt();
        this.mRawData = new byte[this.mRawSize];
        parcel.readByteArray(this.mRawData);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mRawSize);
        parcel.writeByteArray(this.mRawData);
    }

    public byte[] getRawData() {
        return this.mRawData;
    }

    public int getRawSize() {
        return this.mRawSize;
    }
}
