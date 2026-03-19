package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.PatternMatcher;

public class PathPermission extends PatternMatcher {
    public static final Parcelable.Creator<PathPermission> CREATOR = new Parcelable.Creator<PathPermission>() {
        @Override
        public PathPermission createFromParcel(Parcel parcel) {
            return new PathPermission(parcel);
        }

        @Override
        public PathPermission[] newArray(int i) {
            return new PathPermission[i];
        }
    };
    private final String mReadPermission;
    private final String mWritePermission;

    public PathPermission(String str, int i, String str2, String str3) {
        super(str, i);
        this.mReadPermission = str2;
        this.mWritePermission = str3;
    }

    public String getReadPermission() {
        return this.mReadPermission;
    }

    public String getWritePermission() {
        return this.mWritePermission;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeString(this.mReadPermission);
        parcel.writeString(this.mWritePermission);
    }

    public PathPermission(Parcel parcel) {
        super(parcel);
        this.mReadPermission = parcel.readString();
        this.mWritePermission = parcel.readString();
    }
}
