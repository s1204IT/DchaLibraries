package android.hardware.camera2.impl;

import android.os.Parcel;
import android.os.Parcelable;

public class CaptureResultExtras implements Parcelable {
    public static final Parcelable.Creator<CaptureResultExtras> CREATOR = new Parcelable.Creator<CaptureResultExtras>() {
        @Override
        public CaptureResultExtras createFromParcel(Parcel parcel) {
            return new CaptureResultExtras(parcel);
        }

        @Override
        public CaptureResultExtras[] newArray(int i) {
            return new CaptureResultExtras[i];
        }
    };
    private int afTriggerId;
    private int errorStreamId;
    private long frameNumber;
    private int partialResultCount;
    private int precaptureTriggerId;
    private int requestId;
    private int subsequenceId;

    private CaptureResultExtras(Parcel parcel) {
        readFromParcel(parcel);
    }

    public CaptureResultExtras(int i, int i2, int i3, int i4, long j, int i5, int i6) {
        this.requestId = i;
        this.subsequenceId = i2;
        this.afTriggerId = i3;
        this.precaptureTriggerId = i4;
        this.frameNumber = j;
        this.partialResultCount = i5;
        this.errorStreamId = i6;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.requestId);
        parcel.writeInt(this.subsequenceId);
        parcel.writeInt(this.afTriggerId);
        parcel.writeInt(this.precaptureTriggerId);
        parcel.writeLong(this.frameNumber);
        parcel.writeInt(this.partialResultCount);
        parcel.writeInt(this.errorStreamId);
    }

    public void readFromParcel(Parcel parcel) {
        this.requestId = parcel.readInt();
        this.subsequenceId = parcel.readInt();
        this.afTriggerId = parcel.readInt();
        this.precaptureTriggerId = parcel.readInt();
        this.frameNumber = parcel.readLong();
        this.partialResultCount = parcel.readInt();
        this.errorStreamId = parcel.readInt();
    }

    public int getRequestId() {
        return this.requestId;
    }

    public int getSubsequenceId() {
        return this.subsequenceId;
    }

    public int getAfTriggerId() {
        return this.afTriggerId;
    }

    public int getPrecaptureTriggerId() {
        return this.precaptureTriggerId;
    }

    public long getFrameNumber() {
        return this.frameNumber;
    }

    public int getPartialResultCount() {
        return this.partialResultCount;
    }

    public int getErrorStreamId() {
        return this.errorStreamId;
    }
}
