package android.app;

import android.os.Parcel;
import android.os.Parcelable;

public class ProcessMemoryState implements Parcelable {
    public static final Parcelable.Creator<ProcessMemoryState> CREATOR = new Parcelable.Creator<ProcessMemoryState>() {
        @Override
        public ProcessMemoryState createFromParcel(Parcel parcel) {
            return new ProcessMemoryState(parcel);
        }

        @Override
        public ProcessMemoryState[] newArray(int i) {
            return new ProcessMemoryState[i];
        }
    };
    public long cacheInBytes;
    public int oomScore;
    public long pgfault;
    public long pgmajfault;
    public String processName;
    public long rssInBytes;
    public long swapInBytes;
    public int uid;

    public ProcessMemoryState(int i, String str, int i2, long j, long j2, long j3, long j4, long j5) {
        this.uid = i;
        this.processName = str;
        this.oomScore = i2;
        this.pgfault = j;
        this.pgmajfault = j2;
        this.rssInBytes = j3;
        this.cacheInBytes = j4;
        this.swapInBytes = j5;
    }

    private ProcessMemoryState(Parcel parcel) {
        this.uid = parcel.readInt();
        this.processName = parcel.readString();
        this.oomScore = parcel.readInt();
        this.pgfault = parcel.readLong();
        this.pgmajfault = parcel.readLong();
        this.rssInBytes = parcel.readLong();
        this.cacheInBytes = parcel.readLong();
        this.swapInBytes = parcel.readLong();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.uid);
        parcel.writeString(this.processName);
        parcel.writeInt(this.oomScore);
        parcel.writeLong(this.pgfault);
        parcel.writeLong(this.pgmajfault);
        parcel.writeLong(this.rssInBytes);
        parcel.writeLong(this.cacheInBytes);
        parcel.writeLong(this.swapInBytes);
    }
}
