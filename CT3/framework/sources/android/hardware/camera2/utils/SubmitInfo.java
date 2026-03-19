package android.hardware.camera2.utils;

import android.os.Parcel;
import android.os.Parcelable;

public class SubmitInfo implements Parcelable {
    public static final Parcelable.Creator<SubmitInfo> CREATOR = new Parcelable.Creator<SubmitInfo>() {
        @Override
        public SubmitInfo createFromParcel(Parcel in) {
            return new SubmitInfo(in, (SubmitInfo) null);
        }

        @Override
        public SubmitInfo[] newArray(int size) {
            return new SubmitInfo[size];
        }
    };
    private long mLastFrameNumber;
    private int mRequestId;

    SubmitInfo(Parcel in, SubmitInfo submitInfo) {
        this(in);
    }

    public SubmitInfo() {
        this.mRequestId = -1;
        this.mLastFrameNumber = -1L;
    }

    public SubmitInfo(int requestId, long lastFrameNumber) {
        this.mRequestId = requestId;
        this.mLastFrameNumber = lastFrameNumber;
    }

    private SubmitInfo(Parcel in) {
        readFromParcel(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mRequestId);
        dest.writeLong(this.mLastFrameNumber);
    }

    public void readFromParcel(Parcel in) {
        this.mRequestId = in.readInt();
        this.mLastFrameNumber = in.readLong();
    }

    public int getRequestId() {
        return this.mRequestId;
    }

    public long getLastFrameNumber() {
        return this.mLastFrameNumber;
    }
}
