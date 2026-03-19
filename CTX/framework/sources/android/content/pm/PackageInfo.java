package android.content.pm;

import android.net.wifi.WifiEnterpriseConfig;
import android.os.Parcel;
import android.os.Parcelable;

public class PackageInfo implements Parcelable {
    public static final Parcelable.Creator<PackageInfo> CREATOR = new Parcelable.Creator<PackageInfo>() {
        @Override
        public PackageInfo createFromParcel(Parcel parcel) {
            return new PackageInfo(parcel);
        }

        @Override
        public PackageInfo[] newArray(int i) {
            return new PackageInfo[i];
        }
    };
    public static final int INSTALL_LOCATION_AUTO = 0;
    public static final int INSTALL_LOCATION_INTERNAL_ONLY = 1;
    public static final int INSTALL_LOCATION_PREFER_EXTERNAL = 2;
    public static final int INSTALL_LOCATION_UNSPECIFIED = -1;
    public static final int REQUESTED_PERMISSION_GRANTED = 2;
    public static final int REQUESTED_PERMISSION_REQUIRED = 1;
    public ActivityInfo[] activities;
    public ApplicationInfo applicationInfo;
    public int baseRevisionCode;
    public int compileSdkVersion;
    public String compileSdkVersionCodename;
    public ConfigurationInfo[] configPreferences;
    public boolean coreApp;
    public FeatureGroupInfo[] featureGroups;
    public long firstInstallTime;
    public int[] gids;
    public int installLocation;
    public InstrumentationInfo[] instrumentation;
    public boolean isStub;
    public long lastUpdateTime;
    boolean mOverlayIsStatic;
    public String overlayCategory;
    public int overlayPriority;
    public String overlayTarget;
    public String packageName;
    public PermissionInfo[] permissions;
    public ProviderInfo[] providers;
    public ActivityInfo[] receivers;
    public FeatureInfo[] reqFeatures;
    public String[] requestedPermissions;
    public int[] requestedPermissionsFlags;
    public String requiredAccountType;
    public boolean requiredForAllUsers;
    public String restrictedAccountType;
    public ServiceInfo[] services;
    public String sharedUserId;
    public int sharedUserLabel;

    @Deprecated
    public Signature[] signatures;
    public SigningInfo signingInfo;
    public String[] splitNames;
    public int[] splitRevisionCodes;

    @Deprecated
    public int versionCode;
    public int versionCodeMajor;
    public String versionName;

    public long getLongVersionCode() {
        return composeLongVersionCode(this.versionCodeMajor, this.versionCode);
    }

    public void setLongVersionCode(long j) {
        this.versionCodeMajor = (int) (j >> 32);
        this.versionCode = (int) j;
    }

    public static long composeLongVersionCode(int i, int i2) {
        return (((long) i2) & 4294967295L) | (((long) i) << 32);
    }

    public PackageInfo() {
        this.installLocation = 1;
    }

    public boolean isOverlayPackage() {
        return this.overlayTarget != null;
    }

    public boolean isStaticOverlayPackage() {
        return this.overlayTarget != null && this.mOverlayIsStatic;
    }

    public String toString() {
        return "PackageInfo{" + Integer.toHexString(System.identityHashCode(this)) + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + this.packageName + "}";
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
        parcel.writeString(this.versionName);
        parcel.writeInt(this.baseRevisionCode);
        parcel.writeIntArray(this.splitRevisionCodes);
        parcel.writeString(this.sharedUserId);
        parcel.writeInt(this.sharedUserLabel);
        if (this.applicationInfo != null) {
            parcel.writeInt(1);
            this.applicationInfo.writeToParcel(parcel, i);
        } else {
            parcel.writeInt(0);
        }
        parcel.writeLong(this.firstInstallTime);
        parcel.writeLong(this.lastUpdateTime);
        parcel.writeIntArray(this.gids);
        int i2 = i | 2;
        parcel.writeTypedArray(this.activities, i2);
        parcel.writeTypedArray(this.receivers, i2);
        parcel.writeTypedArray(this.services, i2);
        parcel.writeTypedArray(this.providers, i2);
        parcel.writeTypedArray(this.instrumentation, i);
        parcel.writeTypedArray(this.permissions, i);
        parcel.writeStringArray(this.requestedPermissions);
        parcel.writeIntArray(this.requestedPermissionsFlags);
        parcel.writeTypedArray(this.signatures, i);
        parcel.writeTypedArray(this.configPreferences, i);
        parcel.writeTypedArray(this.reqFeatures, i);
        parcel.writeTypedArray(this.featureGroups, i);
        parcel.writeInt(this.installLocation);
        parcel.writeInt(this.isStub ? 1 : 0);
        parcel.writeInt(this.coreApp ? 1 : 0);
        parcel.writeInt(this.requiredForAllUsers ? 1 : 0);
        parcel.writeString(this.restrictedAccountType);
        parcel.writeString(this.requiredAccountType);
        parcel.writeString(this.overlayTarget);
        parcel.writeString(this.overlayCategory);
        parcel.writeInt(this.overlayPriority);
        parcel.writeBoolean(this.mOverlayIsStatic);
        parcel.writeInt(this.compileSdkVersion);
        parcel.writeString(this.compileSdkVersionCodename);
        if (this.signingInfo != null) {
            parcel.writeInt(1);
            this.signingInfo.writeToParcel(parcel, i);
        } else {
            parcel.writeInt(0);
        }
    }

    private PackageInfo(Parcel parcel) {
        this.installLocation = 1;
        this.packageName = parcel.readString();
        this.splitNames = parcel.createStringArray();
        this.versionCode = parcel.readInt();
        this.versionCodeMajor = parcel.readInt();
        this.versionName = parcel.readString();
        this.baseRevisionCode = parcel.readInt();
        this.splitRevisionCodes = parcel.createIntArray();
        this.sharedUserId = parcel.readString();
        this.sharedUserLabel = parcel.readInt();
        if (parcel.readInt() != 0) {
            this.applicationInfo = ApplicationInfo.CREATOR.createFromParcel(parcel);
        }
        this.firstInstallTime = parcel.readLong();
        this.lastUpdateTime = parcel.readLong();
        this.gids = parcel.createIntArray();
        this.activities = (ActivityInfo[]) parcel.createTypedArray(ActivityInfo.CREATOR);
        this.receivers = (ActivityInfo[]) parcel.createTypedArray(ActivityInfo.CREATOR);
        this.services = (ServiceInfo[]) parcel.createTypedArray(ServiceInfo.CREATOR);
        this.providers = (ProviderInfo[]) parcel.createTypedArray(ProviderInfo.CREATOR);
        this.instrumentation = (InstrumentationInfo[]) parcel.createTypedArray(InstrumentationInfo.CREATOR);
        this.permissions = (PermissionInfo[]) parcel.createTypedArray(PermissionInfo.CREATOR);
        this.requestedPermissions = parcel.createStringArray();
        this.requestedPermissionsFlags = parcel.createIntArray();
        this.signatures = (Signature[]) parcel.createTypedArray(Signature.CREATOR);
        this.configPreferences = (ConfigurationInfo[]) parcel.createTypedArray(ConfigurationInfo.CREATOR);
        this.reqFeatures = (FeatureInfo[]) parcel.createTypedArray(FeatureInfo.CREATOR);
        this.featureGroups = (FeatureGroupInfo[]) parcel.createTypedArray(FeatureGroupInfo.CREATOR);
        this.installLocation = parcel.readInt();
        this.isStub = parcel.readInt() != 0;
        this.coreApp = parcel.readInt() != 0;
        this.requiredForAllUsers = parcel.readInt() != 0;
        this.restrictedAccountType = parcel.readString();
        this.requiredAccountType = parcel.readString();
        this.overlayTarget = parcel.readString();
        this.overlayCategory = parcel.readString();
        this.overlayPriority = parcel.readInt();
        this.mOverlayIsStatic = parcel.readBoolean();
        this.compileSdkVersion = parcel.readInt();
        this.compileSdkVersionCodename = parcel.readString();
        if (parcel.readInt() != 0) {
            this.signingInfo = SigningInfo.CREATOR.createFromParcel(parcel);
        }
        if (this.applicationInfo != null) {
            propagateApplicationInfo(this.applicationInfo, this.activities);
            propagateApplicationInfo(this.applicationInfo, this.receivers);
            propagateApplicationInfo(this.applicationInfo, this.services);
            propagateApplicationInfo(this.applicationInfo, this.providers);
        }
    }

    private void propagateApplicationInfo(ApplicationInfo applicationInfo, ComponentInfo[] componentInfoArr) {
        if (componentInfoArr != null) {
            for (ComponentInfo componentInfo : componentInfoArr) {
                componentInfo.applicationInfo = applicationInfo;
            }
        }
    }
}
