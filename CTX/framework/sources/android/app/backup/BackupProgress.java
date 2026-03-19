package android.app.backup;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

@SystemApi
public class BackupProgress implements Parcelable {
    public static final Parcelable.Creator<BackupProgress> CREATOR = new Parcelable.Creator<BackupProgress>() {
        @Override
        public BackupProgress createFromParcel(Parcel parcel) {
            return new BackupProgress(parcel);
        }

        @Override
        public BackupProgress[] newArray(int i) {
            return new BackupProgress[i];
        }
    };
    public final long bytesExpected;
    public final long bytesTransferred;

    public BackupProgress(long j, long j2) {
        this.bytesExpected = j;
        this.bytesTransferred = j2;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(this.bytesExpected);
        parcel.writeLong(this.bytesTransferred);
    }

    private BackupProgress(Parcel parcel) {
        this.bytesExpected = parcel.readLong();
        this.bytesTransferred = parcel.readLong();
    }
}
