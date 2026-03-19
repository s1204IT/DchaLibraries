package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.SettingsStringUtil;

public class UserInfo implements Parcelable {
    public static final Parcelable.Creator<UserInfo> CREATOR = new Parcelable.Creator<UserInfo>() {
        @Override
        public UserInfo createFromParcel(Parcel parcel) {
            return new UserInfo(parcel);
        }

        @Override
        public UserInfo[] newArray(int i) {
            return new UserInfo[i];
        }
    };
    public static final int FLAG_ADMIN = 2;
    public static final int FLAG_DEMO = 512;
    public static final int FLAG_DISABLED = 64;
    public static final int FLAG_EPHEMERAL = 256;
    public static final int FLAG_GUEST = 4;
    public static final int FLAG_INITIALIZED = 16;
    public static final int FLAG_MANAGED_PROFILE = 32;
    public static final int FLAG_MASK_USER_TYPE = 65535;
    public static final int FLAG_PRIMARY = 1;
    public static final int FLAG_QUIET_MODE = 128;
    public static final int FLAG_RESTRICTED = 8;
    public static final int NO_PROFILE_GROUP_ID = -10000;
    public long creationTime;
    public int flags;
    public boolean guestToRemove;
    public String iconPath;
    public int id;
    public String lastLoggedInFingerprint;
    public long lastLoggedInTime;
    public String name;
    public boolean partial;
    public int profileBadge;
    public int profileGroupId;
    public int restrictedProfileParentId;
    public int serialNumber;

    public UserInfo(int i, String str, int i2) {
        this(i, str, null, i2);
    }

    public UserInfo(int i, String str, String str2, int i2) {
        this.id = i;
        this.name = str;
        this.flags = i2;
        this.iconPath = str2;
        this.profileGroupId = -10000;
        this.restrictedProfileParentId = -10000;
    }

    public boolean isPrimary() {
        return (this.flags & 1) == 1;
    }

    public boolean isAdmin() {
        return (this.flags & 2) == 2;
    }

    public boolean isGuest() {
        return (this.flags & 4) == 4;
    }

    public boolean isRestricted() {
        return (this.flags & 8) == 8;
    }

    public boolean isManagedProfile() {
        return (this.flags & 32) == 32;
    }

    public boolean isEnabled() {
        return (this.flags & 64) != 64;
    }

    public boolean isQuietModeEnabled() {
        return (this.flags & 128) == 128;
    }

    public boolean isEphemeral() {
        return (this.flags & 256) == 256;
    }

    public boolean isInitialized() {
        return (this.flags & 16) == 16;
    }

    public boolean isDemo() {
        return (this.flags & 512) == 512;
    }

    public boolean isSystemOnly() {
        return isSystemOnly(this.id);
    }

    public static boolean isSystemOnly(int i) {
        return i == 0 && UserManager.isSplitSystemUser();
    }

    public boolean supportsSwitchTo() {
        if (isEphemeral() && !isEnabled()) {
            return false;
        }
        return !isManagedProfile();
    }

    public boolean supportsSwitchToByUser() {
        return !(UserManager.isSplitSystemUser() && this.id == 0) && supportsSwitchTo();
    }

    public boolean canHaveProfile() {
        if (isManagedProfile() || isGuest() || isRestricted()) {
            return false;
        }
        return UserManager.isSplitSystemUser() ? this.id != 0 : this.id == 0;
    }

    public UserInfo() {
    }

    public UserInfo(UserInfo userInfo) {
        this.name = userInfo.name;
        this.iconPath = userInfo.iconPath;
        this.id = userInfo.id;
        this.flags = userInfo.flags;
        this.serialNumber = userInfo.serialNumber;
        this.creationTime = userInfo.creationTime;
        this.lastLoggedInTime = userInfo.lastLoggedInTime;
        this.lastLoggedInFingerprint = userInfo.lastLoggedInFingerprint;
        this.partial = userInfo.partial;
        this.profileGroupId = userInfo.profileGroupId;
        this.restrictedProfileParentId = userInfo.restrictedProfileParentId;
        this.guestToRemove = userInfo.guestToRemove;
        this.profileBadge = userInfo.profileBadge;
    }

    public UserHandle getUserHandle() {
        return new UserHandle(this.id);
    }

    public String toString() {
        return "UserInfo{" + this.id + SettingsStringUtil.DELIMITER + this.name + SettingsStringUtil.DELIMITER + Integer.toHexString(this.flags) + "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.id);
        parcel.writeString(this.name);
        parcel.writeString(this.iconPath);
        parcel.writeInt(this.flags);
        parcel.writeInt(this.serialNumber);
        parcel.writeLong(this.creationTime);
        parcel.writeLong(this.lastLoggedInTime);
        parcel.writeString(this.lastLoggedInFingerprint);
        parcel.writeInt(this.partial ? 1 : 0);
        parcel.writeInt(this.profileGroupId);
        parcel.writeInt(this.guestToRemove ? 1 : 0);
        parcel.writeInt(this.restrictedProfileParentId);
        parcel.writeInt(this.profileBadge);
    }

    private UserInfo(Parcel parcel) {
        this.id = parcel.readInt();
        this.name = parcel.readString();
        this.iconPath = parcel.readString();
        this.flags = parcel.readInt();
        this.serialNumber = parcel.readInt();
        this.creationTime = parcel.readLong();
        this.lastLoggedInTime = parcel.readLong();
        this.lastLoggedInFingerprint = parcel.readString();
        this.partial = parcel.readInt() != 0;
        this.profileGroupId = parcel.readInt();
        this.guestToRemove = parcel.readInt() != 0;
        this.restrictedProfileParentId = parcel.readInt();
        this.profileBadge = parcel.readInt();
    }
}
