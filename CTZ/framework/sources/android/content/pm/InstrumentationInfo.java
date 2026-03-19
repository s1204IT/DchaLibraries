package android.content.pm;

import android.net.wifi.WifiEnterpriseConfig;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

public class InstrumentationInfo extends PackageItemInfo implements Parcelable {
    public static final Parcelable.Creator<InstrumentationInfo> CREATOR = new Parcelable.Creator<InstrumentationInfo>() {
        @Override
        public InstrumentationInfo createFromParcel(Parcel parcel) {
            return new InstrumentationInfo(parcel);
        }

        @Override
        public InstrumentationInfo[] newArray(int i) {
            return new InstrumentationInfo[i];
        }
    };
    public String credentialProtectedDataDir;
    public String dataDir;
    public String deviceProtectedDataDir;
    public boolean functionalTest;
    public boolean handleProfiling;
    public String nativeLibraryDir;
    public String primaryCpuAbi;
    public String publicSourceDir;
    public String secondaryCpuAbi;
    public String secondaryNativeLibraryDir;
    public String sourceDir;
    public SparseArray<int[]> splitDependencies;
    public String[] splitNames;
    public String[] splitPublicSourceDirs;
    public String[] splitSourceDirs;
    public String targetPackage;
    public String targetProcesses;

    public InstrumentationInfo() {
    }

    public InstrumentationInfo(InstrumentationInfo instrumentationInfo) {
        super(instrumentationInfo);
        this.targetPackage = instrumentationInfo.targetPackage;
        this.targetProcesses = instrumentationInfo.targetProcesses;
        this.sourceDir = instrumentationInfo.sourceDir;
        this.publicSourceDir = instrumentationInfo.publicSourceDir;
        this.splitNames = instrumentationInfo.splitNames;
        this.splitSourceDirs = instrumentationInfo.splitSourceDirs;
        this.splitPublicSourceDirs = instrumentationInfo.splitPublicSourceDirs;
        this.splitDependencies = instrumentationInfo.splitDependencies;
        this.dataDir = instrumentationInfo.dataDir;
        this.deviceProtectedDataDir = instrumentationInfo.deviceProtectedDataDir;
        this.credentialProtectedDataDir = instrumentationInfo.credentialProtectedDataDir;
        this.primaryCpuAbi = instrumentationInfo.primaryCpuAbi;
        this.secondaryCpuAbi = instrumentationInfo.secondaryCpuAbi;
        this.nativeLibraryDir = instrumentationInfo.nativeLibraryDir;
        this.secondaryNativeLibraryDir = instrumentationInfo.secondaryNativeLibraryDir;
        this.handleProfiling = instrumentationInfo.handleProfiling;
        this.functionalTest = instrumentationInfo.functionalTest;
    }

    public String toString() {
        return "InstrumentationInfo{" + Integer.toHexString(System.identityHashCode(this)) + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + this.packageName + "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeString(this.targetPackage);
        parcel.writeString(this.targetProcesses);
        parcel.writeString(this.sourceDir);
        parcel.writeString(this.publicSourceDir);
        parcel.writeStringArray(this.splitNames);
        parcel.writeStringArray(this.splitSourceDirs);
        parcel.writeStringArray(this.splitPublicSourceDirs);
        parcel.writeSparseArray(this.splitDependencies);
        parcel.writeString(this.dataDir);
        parcel.writeString(this.deviceProtectedDataDir);
        parcel.writeString(this.credentialProtectedDataDir);
        parcel.writeString(this.primaryCpuAbi);
        parcel.writeString(this.secondaryCpuAbi);
        parcel.writeString(this.nativeLibraryDir);
        parcel.writeString(this.secondaryNativeLibraryDir);
        parcel.writeInt(this.handleProfiling ? 1 : 0);
        parcel.writeInt(this.functionalTest ? 1 : 0);
    }

    private InstrumentationInfo(Parcel parcel) {
        super(parcel);
        this.targetPackage = parcel.readString();
        this.targetProcesses = parcel.readString();
        this.sourceDir = parcel.readString();
        this.publicSourceDir = parcel.readString();
        this.splitNames = parcel.readStringArray();
        this.splitSourceDirs = parcel.readStringArray();
        this.splitPublicSourceDirs = parcel.readStringArray();
        this.splitDependencies = parcel.readSparseArray(null);
        this.dataDir = parcel.readString();
        this.deviceProtectedDataDir = parcel.readString();
        this.credentialProtectedDataDir = parcel.readString();
        this.primaryCpuAbi = parcel.readString();
        this.secondaryCpuAbi = parcel.readString();
        this.nativeLibraryDir = parcel.readString();
        this.secondaryNativeLibraryDir = parcel.readString();
        this.handleProfiling = parcel.readInt() != 0;
        this.functionalTest = parcel.readInt() != 0;
    }

    public void copyTo(ApplicationInfo applicationInfo) {
        applicationInfo.packageName = this.packageName;
        applicationInfo.sourceDir = this.sourceDir;
        applicationInfo.publicSourceDir = this.publicSourceDir;
        applicationInfo.splitNames = this.splitNames;
        applicationInfo.splitSourceDirs = this.splitSourceDirs;
        applicationInfo.splitPublicSourceDirs = this.splitPublicSourceDirs;
        applicationInfo.splitDependencies = this.splitDependencies;
        applicationInfo.dataDir = this.dataDir;
        applicationInfo.deviceProtectedDataDir = this.deviceProtectedDataDir;
        applicationInfo.credentialProtectedDataDir = this.credentialProtectedDataDir;
        applicationInfo.primaryCpuAbi = this.primaryCpuAbi;
        applicationInfo.secondaryCpuAbi = this.secondaryCpuAbi;
        applicationInfo.nativeLibraryDir = this.nativeLibraryDir;
        applicationInfo.secondaryNativeLibraryDir = this.secondaryNativeLibraryDir;
    }
}
