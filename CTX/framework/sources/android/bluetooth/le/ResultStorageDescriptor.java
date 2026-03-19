package android.bluetooth.le;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

@SystemApi
public final class ResultStorageDescriptor implements Parcelable {
    public static final Parcelable.Creator<ResultStorageDescriptor> CREATOR = new Parcelable.Creator<ResultStorageDescriptor>() {
        @Override
        public ResultStorageDescriptor createFromParcel(Parcel parcel) {
            return new ResultStorageDescriptor(parcel);
        }

        @Override
        public ResultStorageDescriptor[] newArray(int i) {
            return new ResultStorageDescriptor[i];
        }
    };
    private int mLength;
    private int mOffset;
    private int mType;

    public int getType() {
        return this.mType;
    }

    public int getOffset() {
        return this.mOffset;
    }

    public int getLength() {
        return this.mLength;
    }

    public ResultStorageDescriptor(int i, int i2, int i3) {
        this.mType = i;
        this.mOffset = i2;
        this.mLength = i3;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mType);
        parcel.writeInt(this.mOffset);
        parcel.writeInt(this.mLength);
    }

    private ResultStorageDescriptor(Parcel parcel) {
        ReadFromParcel(parcel);
    }

    private void ReadFromParcel(Parcel parcel) {
        this.mType = parcel.readInt();
        this.mOffset = parcel.readInt();
        this.mLength = parcel.readInt();
    }
}
