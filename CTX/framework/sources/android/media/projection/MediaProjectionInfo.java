package android.media.projection;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;
import java.util.Objects;

public final class MediaProjectionInfo implements Parcelable {
    public static final Parcelable.Creator<MediaProjectionInfo> CREATOR = new Parcelable.Creator<MediaProjectionInfo>() {
        @Override
        public MediaProjectionInfo createFromParcel(Parcel parcel) {
            return new MediaProjectionInfo(parcel);
        }

        @Override
        public MediaProjectionInfo[] newArray(int i) {
            return new MediaProjectionInfo[i];
        }
    };
    private final String mPackageName;
    private final UserHandle mUserHandle;

    public MediaProjectionInfo(String str, UserHandle userHandle) {
        this.mPackageName = str;
        this.mUserHandle = userHandle;
    }

    public MediaProjectionInfo(Parcel parcel) {
        this.mPackageName = parcel.readString();
        this.mUserHandle = UserHandle.readFromParcel(parcel);
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public UserHandle getUserHandle() {
        return this.mUserHandle;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof MediaProjectionInfo)) {
            return false;
        }
        MediaProjectionInfo mediaProjectionInfo = (MediaProjectionInfo) obj;
        return Objects.equals(mediaProjectionInfo.mPackageName, this.mPackageName) && Objects.equals(mediaProjectionInfo.mUserHandle, this.mUserHandle);
    }

    public int hashCode() {
        return Objects.hash(this.mPackageName, this.mUserHandle);
    }

    public String toString() {
        return "MediaProjectionInfo{mPackageName=" + this.mPackageName + ", mUserHandle=" + this.mUserHandle + "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mPackageName);
        UserHandle.writeToParcel(this.mUserHandle, parcel);
    }
}
