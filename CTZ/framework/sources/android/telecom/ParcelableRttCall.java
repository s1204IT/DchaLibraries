package android.telecom;

import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;

public class ParcelableRttCall implements Parcelable {
    public static final Parcelable.Creator<ParcelableRttCall> CREATOR = new Parcelable.Creator<ParcelableRttCall>() {
        @Override
        public ParcelableRttCall createFromParcel(Parcel parcel) {
            return new ParcelableRttCall(parcel);
        }

        @Override
        public ParcelableRttCall[] newArray(int i) {
            return new ParcelableRttCall[i];
        }
    };
    private final ParcelFileDescriptor mReceiveStream;
    private final int mRttMode;
    private final ParcelFileDescriptor mTransmitStream;

    public ParcelableRttCall(int i, ParcelFileDescriptor parcelFileDescriptor, ParcelFileDescriptor parcelFileDescriptor2) {
        this.mRttMode = i;
        this.mTransmitStream = parcelFileDescriptor;
        this.mReceiveStream = parcelFileDescriptor2;
    }

    protected ParcelableRttCall(Parcel parcel) {
        this.mRttMode = parcel.readInt();
        this.mTransmitStream = (ParcelFileDescriptor) parcel.readParcelable(ParcelFileDescriptor.class.getClassLoader());
        this.mReceiveStream = (ParcelFileDescriptor) parcel.readParcelable(ParcelFileDescriptor.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mRttMode);
        parcel.writeParcelable(this.mTransmitStream, i);
        parcel.writeParcelable(this.mReceiveStream, i);
    }

    public int getRttMode() {
        return this.mRttMode;
    }

    public ParcelFileDescriptor getReceiveStream() {
        return this.mReceiveStream;
    }

    public ParcelFileDescriptor getTransmitStream() {
        return this.mTransmitStream;
    }
}
