package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;

public class PackageCleanItem implements Parcelable {
    public static final Parcelable.Creator<PackageCleanItem> CREATOR = new Parcelable.Creator<PackageCleanItem>() {
        @Override
        public PackageCleanItem createFromParcel(Parcel source) {
            return new PackageCleanItem(source, null);
        }

        @Override
        public PackageCleanItem[] newArray(int size) {
            return new PackageCleanItem[size];
        }
    };
    public final boolean andCode;
    public final String packageName;
    public final int userId;

    PackageCleanItem(Parcel source, PackageCleanItem packageCleanItem) {
        this(source);
    }

    public PackageCleanItem(int userId, String packageName, boolean andCode) {
        this.userId = userId;
        this.packageName = packageName;
        this.andCode = andCode;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null) {
            try {
                PackageCleanItem other = (PackageCleanItem) obj;
                if (this.userId == other.userId && this.packageName.equals(other.packageName)) {
                    return this.andCode == other.andCode;
                }
                return false;
            } catch (ClassCastException e) {
            }
        }
        return false;
    }

    public int hashCode() {
        int result = this.userId + 527;
        return (((result * 31) + this.packageName.hashCode()) * 31) + (this.andCode ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int parcelableFlags) {
        dest.writeInt(this.userId);
        dest.writeString(this.packageName);
        dest.writeInt(this.andCode ? 1 : 0);
    }

    private PackageCleanItem(Parcel source) {
        this.userId = source.readInt();
        this.packageName = source.readString();
        this.andCode = source.readInt() != 0;
    }
}
