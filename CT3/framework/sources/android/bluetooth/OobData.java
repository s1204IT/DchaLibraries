package android.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;

public class OobData implements Parcelable {
    public static final Parcelable.Creator<OobData> CREATOR = new Parcelable.Creator<OobData>() {
        @Override
        public OobData createFromParcel(Parcel in) {
            return new OobData(in, null);
        }

        @Override
        public OobData[] newArray(int size) {
            return new OobData[size];
        }
    };
    private byte[] securityManagerTk;

    OobData(Parcel in, OobData oobData) {
        this(in);
    }

    public byte[] getSecurityManagerTk() {
        return this.securityManagerTk;
    }

    public void setSecurityManagerTk(byte[] securityManagerTk) {
        this.securityManagerTk = securityManagerTk;
    }

    public OobData() {
    }

    private OobData(Parcel in) {
        this.securityManagerTk = in.createByteArray();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeByteArray(this.securityManagerTk);
    }
}
