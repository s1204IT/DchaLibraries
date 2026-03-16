package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemProperties;
import android.os.UserHandle;

public class UserInfo implements Parcelable {
    public static final Parcelable.Creator<UserInfo> CREATOR = new Parcelable.Creator<UserInfo>() {
        @Override
        public UserInfo createFromParcel(Parcel source) {
            return new UserInfo(source);
        }

        @Override
        public UserInfo[] newArray(int size) {
            return new UserInfo[size];
        }
    };
    public static final int FLAG_ADMIN = 2;
    public static final int FLAG_DISABLED = 64;
    public static final int FLAG_GUEST = 4;
    public static final int FLAG_INITIALIZED = 16;
    public static final int FLAG_MANAGED_PROFILE = 32;
    public static final int FLAG_MASK_USER_TYPE = 255;
    public static final int FLAG_PRIMARY = 1;
    public static final int FLAG_RESTRICTED = 8;
    public static final int NO_PROFILE_GROUP_ID = -1;
    public long creationTime;
    public int flags;
    public boolean guestToRemove;
    public String iconPath;
    public int id;
    public long lastLoggedInTime;
    public String name;
    public boolean partial;
    public int profileGroupId;
    public int serialNumber;

    public UserInfo(int id, String name, int flags) {
        this(id, name, null, flags);
    }

    public UserInfo(int id, String name, String iconPath, int flags) {
        this.id = id;
        this.name = name;
        this.flags = flags;
        this.iconPath = iconPath;
        this.profileGroupId = -1;
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

    public boolean supportsSwitchTo() {
        return !isManagedProfile() || SystemProperties.getBoolean("fw.show_hidden_users", false);
    }

    public UserInfo() {
    }

    public UserInfo(UserInfo orig) {
        this.name = orig.name;
        this.iconPath = orig.iconPath;
        this.id = orig.id;
        this.flags = orig.flags;
        this.serialNumber = orig.serialNumber;
        this.creationTime = orig.creationTime;
        this.lastLoggedInTime = orig.lastLoggedInTime;
        this.partial = orig.partial;
        this.profileGroupId = orig.profileGroupId;
        this.guestToRemove = orig.guestToRemove;
    }

    public UserHandle getUserHandle() {
        return new UserHandle(this.id);
    }

    public String toString() {
        return "UserInfo{" + this.id + ":" + this.name + ":" + Integer.toHexString(this.flags) + "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int parcelableFlags) {
        dest.writeInt(this.id);
        dest.writeString(this.name);
        dest.writeString(this.iconPath);
        dest.writeInt(this.flags);
        dest.writeInt(this.serialNumber);
        dest.writeLong(this.creationTime);
        dest.writeLong(this.lastLoggedInTime);
        dest.writeInt(this.partial ? 1 : 0);
        dest.writeInt(this.profileGroupId);
        dest.writeInt(this.guestToRemove ? 1 : 0);
    }

    private UserInfo(Parcel source) {
        this.id = source.readInt();
        this.name = source.readString();
        this.iconPath = source.readString();
        this.flags = source.readInt();
        this.serialNumber = source.readInt();
        this.creationTime = source.readLong();
        this.lastLoggedInTime = source.readLong();
        this.partial = source.readInt() != 0;
        this.profileGroupId = source.readInt();
        this.guestToRemove = source.readInt() != 0;
    }
}
