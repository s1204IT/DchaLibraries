package android.content;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public final class UriPermission implements Parcelable {
    public static final Parcelable.Creator<UriPermission> CREATOR = new Parcelable.Creator<UriPermission>() {
        @Override
        public UriPermission createFromParcel(Parcel parcel) {
            return new UriPermission(parcel);
        }

        @Override
        public UriPermission[] newArray(int i) {
            return new UriPermission[i];
        }
    };
    public static final long INVALID_TIME = Long.MIN_VALUE;
    private final int mModeFlags;
    private final long mPersistedTime;
    private final Uri mUri;

    public UriPermission(Uri uri, int i, long j) {
        this.mUri = uri;
        this.mModeFlags = i;
        this.mPersistedTime = j;
    }

    public UriPermission(Parcel parcel) {
        this.mUri = (Uri) parcel.readParcelable(null);
        this.mModeFlags = parcel.readInt();
        this.mPersistedTime = parcel.readLong();
    }

    public Uri getUri() {
        return this.mUri;
    }

    public boolean isReadPermission() {
        return (this.mModeFlags & 1) != 0;
    }

    public boolean isWritePermission() {
        return (this.mModeFlags & 2) != 0;
    }

    public long getPersistedTime() {
        return this.mPersistedTime;
    }

    public String toString() {
        return "UriPermission {uri=" + this.mUri + ", modeFlags=" + this.mModeFlags + ", persistedTime=" + this.mPersistedTime + "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.mUri, i);
        parcel.writeInt(this.mModeFlags);
        parcel.writeLong(this.mPersistedTime);
    }
}
