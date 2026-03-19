package android.os;

import android.os.Parcelable;

public final class CpuUsageInfo implements Parcelable {
    public static final Parcelable.Creator<CpuUsageInfo> CREATOR = new Parcelable.Creator<CpuUsageInfo>() {
        @Override
        public CpuUsageInfo createFromParcel(Parcel parcel) {
            return new CpuUsageInfo(parcel);
        }

        @Override
        public CpuUsageInfo[] newArray(int i) {
            return new CpuUsageInfo[i];
        }
    };
    private long mActive;
    private long mTotal;

    public CpuUsageInfo(long j, long j2) {
        this.mActive = j;
        this.mTotal = j2;
    }

    private CpuUsageInfo(Parcel parcel) {
        readFromParcel(parcel);
    }

    public long getActive() {
        return this.mActive;
    }

    public long getTotal() {
        return this.mTotal;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(this.mActive);
        parcel.writeLong(this.mTotal);
    }

    private void readFromParcel(Parcel parcel) {
        this.mActive = parcel.readLong();
        this.mTotal = parcel.readLong();
    }
}
