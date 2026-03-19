package android.content.res;

import android.os.Parcel;
import android.os.Parcelable;

public class ObbInfo implements Parcelable {
    public static final Parcelable.Creator<ObbInfo> CREATOR = new Parcelable.Creator<ObbInfo>() {
        @Override
        public ObbInfo createFromParcel(Parcel parcel) {
            return new ObbInfo(parcel);
        }

        @Override
        public ObbInfo[] newArray(int i) {
            return new ObbInfo[i];
        }
    };
    public static final int OBB_OVERLAY = 1;
    public String filename;
    public int flags;
    public String packageName;
    public byte[] salt;
    public int version;

    ObbInfo() {
    }

    public String toString() {
        return "ObbInfo{" + Integer.toHexString(System.identityHashCode(this)) + " packageName=" + this.packageName + ",version=" + this.version + ",flags=" + this.flags + '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.filename);
        parcel.writeString(this.packageName);
        parcel.writeInt(this.version);
        parcel.writeInt(this.flags);
        parcel.writeByteArray(this.salt);
    }

    private ObbInfo(Parcel parcel) {
        this.filename = parcel.readString();
        this.packageName = parcel.readString();
        this.version = parcel.readInt();
        this.flags = parcel.readInt();
        this.salt = parcel.createByteArray();
    }
}
