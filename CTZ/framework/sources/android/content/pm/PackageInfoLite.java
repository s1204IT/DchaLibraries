package android.content.pm;

import android.net.wifi.WifiEnterpriseConfig;
import android.os.Parcel;
import android.os.Parcelable;

public class PackageInfoLite implements Parcelable {
    public static final Parcelable.Creator<PackageInfoLite> CREATOR = new Parcelable.Creator<PackageInfoLite>() {
        @Override
        public PackageInfoLite createFromParcel(Parcel parcel) {
            return new PackageInfoLite(parcel);
        }

        @Override
        public PackageInfoLite[] newArray(int i) {
            return new PackageInfoLite[i];
        }
    };
    public int baseRevisionCode;
    public int installLocation;
    public boolean multiArch;
    public String packageName;
    public int recommendedInstallLocation;
    public String[] splitNames;
    public int[] splitRevisionCodes;
    public VerifierInfo[] verifiers;

    @Deprecated
    public int versionCode;
    public int versionCodeMajor;

    public long getLongVersionCode() {
        return PackageInfo.composeLongVersionCode(this.versionCodeMajor, this.versionCode);
    }

    public PackageInfoLite() {
    }

    public String toString() {
        return "PackageInfoLite{" + Integer.toHexString(System.identityHashCode(this)) + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + this.packageName + "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.packageName);
        parcel.writeStringArray(this.splitNames);
        parcel.writeInt(this.versionCode);
        parcel.writeInt(this.versionCodeMajor);
        parcel.writeInt(this.baseRevisionCode);
        parcel.writeIntArray(this.splitRevisionCodes);
        parcel.writeInt(this.recommendedInstallLocation);
        parcel.writeInt(this.installLocation);
        parcel.writeInt(this.multiArch ? 1 : 0);
        if (this.verifiers == null || this.verifiers.length == 0) {
            parcel.writeInt(0);
        } else {
            parcel.writeInt(this.verifiers.length);
            parcel.writeTypedArray(this.verifiers, i);
        }
    }

    private PackageInfoLite(Parcel parcel) {
        this.packageName = parcel.readString();
        this.splitNames = parcel.createStringArray();
        this.versionCode = parcel.readInt();
        this.versionCodeMajor = parcel.readInt();
        this.baseRevisionCode = parcel.readInt();
        this.splitRevisionCodes = parcel.createIntArray();
        this.recommendedInstallLocation = parcel.readInt();
        this.installLocation = parcel.readInt();
        this.multiArch = parcel.readInt() != 0;
        int i = parcel.readInt();
        if (i == 0) {
            this.verifiers = new VerifierInfo[0];
        } else {
            this.verifiers = new VerifierInfo[i];
            parcel.readTypedArray(this.verifiers, VerifierInfo.CREATOR);
        }
    }
}
