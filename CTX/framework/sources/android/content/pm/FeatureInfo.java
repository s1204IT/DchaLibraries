package android.content.pm;

import android.net.wifi.WifiEnterpriseConfig;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.proto.ProtoOutputStream;

public class FeatureInfo implements Parcelable {
    public static final Parcelable.Creator<FeatureInfo> CREATOR = new Parcelable.Creator<FeatureInfo>() {
        @Override
        public FeatureInfo createFromParcel(Parcel parcel) {
            return new FeatureInfo(parcel);
        }

        @Override
        public FeatureInfo[] newArray(int i) {
            return new FeatureInfo[i];
        }
    };
    public static final int FLAG_REQUIRED = 1;
    public static final int GL_ES_VERSION_UNDEFINED = 0;
    public int flags;
    public String name;
    public int reqGlEsVersion;
    public int version;

    public FeatureInfo() {
    }

    public FeatureInfo(FeatureInfo featureInfo) {
        this.name = featureInfo.name;
        this.version = featureInfo.version;
        this.reqGlEsVersion = featureInfo.reqGlEsVersion;
        this.flags = featureInfo.flags;
    }

    public String toString() {
        if (this.name != null) {
            return "FeatureInfo{" + Integer.toHexString(System.identityHashCode(this)) + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + this.name + " v=" + this.version + " fl=0x" + Integer.toHexString(this.flags) + "}";
        }
        return "FeatureInfo{" + Integer.toHexString(System.identityHashCode(this)) + " glEsVers=" + getGlEsVersion() + " fl=0x" + Integer.toHexString(this.flags) + "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.name);
        parcel.writeInt(this.version);
        parcel.writeInt(this.reqGlEsVersion);
        parcel.writeInt(this.flags);
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        if (this.name != null) {
            protoOutputStream.write(1138166333441L, this.name);
        }
        protoOutputStream.write(1120986464258L, this.version);
        protoOutputStream.write(1138166333443L, getGlEsVersion());
        protoOutputStream.write(1120986464260L, this.flags);
        protoOutputStream.end(jStart);
    }

    private FeatureInfo(Parcel parcel) {
        this.name = parcel.readString();
        this.version = parcel.readInt();
        this.reqGlEsVersion = parcel.readInt();
        this.flags = parcel.readInt();
    }

    public String getGlEsVersion() {
        return String.valueOf((this.reqGlEsVersion & (-65536)) >> 16) + "." + String.valueOf(this.reqGlEsVersion & 65535);
    }
}
