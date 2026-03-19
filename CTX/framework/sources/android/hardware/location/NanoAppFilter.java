package android.hardware.location;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

@SystemApi
@Deprecated
public class NanoAppFilter implements Parcelable {
    public static final int APP_ANY = -1;
    public static final Parcelable.Creator<NanoAppFilter> CREATOR = new Parcelable.Creator<NanoAppFilter>() {
        @Override
        public NanoAppFilter createFromParcel(Parcel parcel) {
            return new NanoAppFilter(parcel);
        }

        @Override
        public NanoAppFilter[] newArray(int i) {
            return new NanoAppFilter[i];
        }
    };
    public static final int FLAGS_VERSION_ANY = -1;
    public static final int FLAGS_VERSION_GREAT_THAN = 2;
    public static final int FLAGS_VERSION_LESS_THAN = 4;
    public static final int FLAGS_VERSION_STRICTLY_EQUAL = 8;
    public static final int HUB_ANY = -1;
    private static final String TAG = "NanoAppFilter";
    public static final int VENDOR_ANY = -1;
    private long mAppId;
    private long mAppIdVendorMask;
    private int mAppVersion;
    private int mContextHubId;
    private int mVersionRestrictionMask;

    private NanoAppFilter(Parcel parcel) {
        this.mContextHubId = -1;
        this.mAppId = parcel.readLong();
        this.mAppVersion = parcel.readInt();
        this.mVersionRestrictionMask = parcel.readInt();
        this.mAppIdVendorMask = parcel.readLong();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(this.mAppId);
        parcel.writeInt(this.mAppVersion);
        parcel.writeInt(this.mVersionRestrictionMask);
        parcel.writeLong(this.mAppIdVendorMask);
    }

    public NanoAppFilter(long j, int i, int i2, long j2) {
        this.mContextHubId = -1;
        this.mAppId = j;
        this.mAppVersion = i;
        this.mVersionRestrictionMask = i2;
        this.mAppIdVendorMask = j2;
    }

    private boolean versionsMatch(int i, int i2, int i3) {
        return true;
    }

    public boolean testMatch(NanoAppInstanceInfo nanoAppInstanceInfo) {
        return (this.mContextHubId == -1 || nanoAppInstanceInfo.getContexthubId() == this.mContextHubId) && (this.mAppId == -1 || nanoAppInstanceInfo.getAppId() == this.mAppId) && versionsMatch(this.mVersionRestrictionMask, this.mAppVersion, nanoAppInstanceInfo.getAppVersion());
    }

    public String toString() {
        return "nanoAppId: 0x" + Long.toHexString(this.mAppId) + ", nanoAppVersion: 0x" + Integer.toHexString(this.mAppVersion) + ", versionMask: " + this.mVersionRestrictionMask + ", vendorMask: " + this.mAppIdVendorMask;
    }
}
