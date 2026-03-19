package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class VersionedPackage implements Parcelable {
    public static final Parcelable.Creator<VersionedPackage> CREATOR = new Parcelable.Creator<VersionedPackage>() {
        @Override
        public VersionedPackage createFromParcel(Parcel parcel) {
            return new VersionedPackage(parcel);
        }

        @Override
        public VersionedPackage[] newArray(int i) {
            return new VersionedPackage[i];
        }
    };
    private final String mPackageName;
    private final long mVersionCode;

    @Retention(RetentionPolicy.SOURCE)
    public @interface VersionCode {
    }

    public VersionedPackage(String str, int i) {
        this.mPackageName = str;
        this.mVersionCode = i;
    }

    public VersionedPackage(String str, long j) {
        this.mPackageName = str;
        this.mVersionCode = j;
    }

    private VersionedPackage(Parcel parcel) {
        this.mPackageName = parcel.readString();
        this.mVersionCode = parcel.readLong();
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    @Deprecated
    public int getVersionCode() {
        return (int) (this.mVersionCode & 2147483647L);
    }

    public long getLongVersionCode() {
        return this.mVersionCode;
    }

    public String toString() {
        return "VersionedPackage[" + this.mPackageName + "/" + this.mVersionCode + "]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mPackageName);
        parcel.writeLong(this.mVersionCode);
    }
}
