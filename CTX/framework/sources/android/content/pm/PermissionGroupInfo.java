package android.content.pm;

import android.annotation.SystemApi;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class PermissionGroupInfo extends PackageItemInfo implements Parcelable {
    public static final Parcelable.Creator<PermissionGroupInfo> CREATOR = new Parcelable.Creator<PermissionGroupInfo>() {
        @Override
        public PermissionGroupInfo createFromParcel(Parcel parcel) {
            return new PermissionGroupInfo(parcel);
        }

        @Override
        public PermissionGroupInfo[] newArray(int i) {
            return new PermissionGroupInfo[i];
        }
    };
    public static final int FLAG_PERSONAL_INFO = 1;
    public int descriptionRes;
    public int flags;
    public CharSequence nonLocalizedDescription;
    public int priority;

    @SystemApi
    public int requestRes;

    public PermissionGroupInfo() {
    }

    public PermissionGroupInfo(PermissionGroupInfo permissionGroupInfo) {
        super(permissionGroupInfo);
        this.descriptionRes = permissionGroupInfo.descriptionRes;
        this.requestRes = permissionGroupInfo.requestRes;
        this.nonLocalizedDescription = permissionGroupInfo.nonLocalizedDescription;
        this.flags = permissionGroupInfo.flags;
        this.priority = permissionGroupInfo.priority;
    }

    public CharSequence loadDescription(PackageManager packageManager) {
        CharSequence text;
        if (this.nonLocalizedDescription != null) {
            return this.nonLocalizedDescription;
        }
        if (this.descriptionRes == 0 || (text = packageManager.getText(this.packageName, this.descriptionRes, null)) == null) {
            return null;
        }
        return text;
    }

    public String toString() {
        return "PermissionGroupInfo{" + Integer.toHexString(System.identityHashCode(this)) + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + this.name + " flgs=0x" + Integer.toHexString(this.flags) + "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeInt(this.descriptionRes);
        parcel.writeInt(this.requestRes);
        TextUtils.writeToParcel(this.nonLocalizedDescription, parcel, i);
        parcel.writeInt(this.flags);
        parcel.writeInt(this.priority);
    }

    private PermissionGroupInfo(Parcel parcel) {
        super(parcel);
        this.descriptionRes = parcel.readInt();
        this.requestRes = parcel.readInt();
        this.nonLocalizedDescription = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
        this.flags = parcel.readInt();
        this.priority = parcel.readInt();
    }
}
