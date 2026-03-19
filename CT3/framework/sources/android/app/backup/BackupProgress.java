package android.app.backup;

import android.os.Parcel;
import android.os.Parcelable;

public class BackupProgress implements Parcelable {
    public static final Parcelable.Creator<BackupProgress> CREATOR = new Parcelable.Creator<BackupProgress>() {
        @Override
        public BackupProgress createFromParcel(Parcel in) {
            return new BackupProgress(in, (BackupProgress) null);
        }

        @Override
        public BackupProgress[] newArray(int size) {
            return new BackupProgress[size];
        }
    };
    public final long bytesExpected;
    public final long bytesTransferred;

    BackupProgress(Parcel in, BackupProgress backupProgress) {
        this(in);
    }

    public BackupProgress(long _bytesExpected, long _bytesTransferred) {
        this.bytesExpected = _bytesExpected;
        this.bytesTransferred = _bytesTransferred;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(this.bytesExpected);
        out.writeLong(this.bytesTransferred);
    }

    private BackupProgress(Parcel in) {
        this.bytesExpected = in.readLong();
        this.bytesTransferred = in.readLong();
    }
}
