package android.app.usage;

import android.os.Parcel;
import android.os.Parcelable;

public final class StorageStats implements Parcelable {
    public static final Parcelable.Creator<StorageStats> CREATOR = new Parcelable.Creator<StorageStats>() {
        @Override
        public StorageStats createFromParcel(Parcel parcel) {
            return new StorageStats(parcel);
        }

        @Override
        public StorageStats[] newArray(int i) {
            return new StorageStats[i];
        }
    };
    public long cacheBytes;
    public long codeBytes;
    public long dataBytes;

    public long getAppBytes() {
        return this.codeBytes;
    }

    @Deprecated
    public long getCodeBytes() {
        return getAppBytes();
    }

    public long getDataBytes() {
        return this.dataBytes;
    }

    public long getCacheBytes() {
        return this.cacheBytes;
    }

    public StorageStats() {
    }

    public StorageStats(Parcel parcel) {
        this.codeBytes = parcel.readLong();
        this.dataBytes = parcel.readLong();
        this.cacheBytes = parcel.readLong();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(this.codeBytes);
        parcel.writeLong(this.dataBytes);
        parcel.writeLong(this.cacheBytes);
    }
}
