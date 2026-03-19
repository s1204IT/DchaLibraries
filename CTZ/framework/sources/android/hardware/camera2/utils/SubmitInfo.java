package android.hardware.camera2.utils;

import android.os.Parcel;
import android.os.Parcelable;

public class SubmitInfo implements Parcelable {
    public static final Parcelable.Creator<SubmitInfo> CREATOR = new Parcelable.Creator<SubmitInfo>() {
        @Override
        public SubmitInfo createFromParcel(Parcel parcel) {
            return new SubmitInfo(parcel);
        }

        @Override
        public SubmitInfo[] newArray(int i) {
            return new SubmitInfo[i];
        }
    };
    private long mLastFrameNumber;
    private int mRequestId;

    public SubmitInfo() {
        this.mRequestId = -1;
        this.mLastFrameNumber = -1L;
    }

    public SubmitInfo(int i, long j) {
        this.mRequestId = i;
        this.mLastFrameNumber = j;
    }

    private SubmitInfo(Parcel parcel) {
        readFromParcel(parcel);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mRequestId);
        parcel.writeLong(this.mLastFrameNumber);
    }

    public void readFromParcel(Parcel parcel) {
        this.mRequestId = parcel.readInt();
        this.mLastFrameNumber = parcel.readLong();
    }

    public int getRequestId() {
        return this.mRequestId;
    }

    public long getLastFrameNumber() {
        return this.mLastFrameNumber;
    }
}
