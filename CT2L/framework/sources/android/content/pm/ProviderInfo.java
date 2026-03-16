package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.PatternMatcher;
import android.util.Printer;

public final class ProviderInfo extends ComponentInfo implements Parcelable {
    public static final Parcelable.Creator<ProviderInfo> CREATOR = new Parcelable.Creator<ProviderInfo>() {
        @Override
        public ProviderInfo createFromParcel(Parcel in) {
            return new ProviderInfo(in);
        }

        @Override
        public ProviderInfo[] newArray(int size) {
            return new ProviderInfo[size];
        }
    };
    public static final int FLAG_SINGLE_USER = 1073741824;
    public String authority;
    public int flags;
    public boolean grantUriPermissions;
    public int initOrder;

    @Deprecated
    public boolean isSyncable;
    public boolean multiprocess;
    public PathPermission[] pathPermissions;
    public String readPermission;
    public PatternMatcher[] uriPermissionPatterns;
    public String writePermission;

    public ProviderInfo() {
        this.authority = null;
        this.readPermission = null;
        this.writePermission = null;
        this.grantUriPermissions = false;
        this.uriPermissionPatterns = null;
        this.pathPermissions = null;
        this.multiprocess = false;
        this.initOrder = 0;
        this.flags = 0;
        this.isSyncable = false;
    }

    public ProviderInfo(ProviderInfo orig) {
        super(orig);
        this.authority = null;
        this.readPermission = null;
        this.writePermission = null;
        this.grantUriPermissions = false;
        this.uriPermissionPatterns = null;
        this.pathPermissions = null;
        this.multiprocess = false;
        this.initOrder = 0;
        this.flags = 0;
        this.isSyncable = false;
        this.authority = orig.authority;
        this.readPermission = orig.readPermission;
        this.writePermission = orig.writePermission;
        this.grantUriPermissions = orig.grantUriPermissions;
        this.uriPermissionPatterns = orig.uriPermissionPatterns;
        this.pathPermissions = orig.pathPermissions;
        this.multiprocess = orig.multiprocess;
        this.initOrder = orig.initOrder;
        this.flags = orig.flags;
        this.isSyncable = orig.isSyncable;
    }

    public void dump(Printer pw, String prefix) {
        super.dumpFront(pw, prefix);
        pw.println(prefix + "authority=" + this.authority);
        pw.println(prefix + "flags=0x" + Integer.toHexString(this.flags));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int parcelableFlags) {
        super.writeToParcel(out, parcelableFlags);
        out.writeString(this.authority);
        out.writeString(this.readPermission);
        out.writeString(this.writePermission);
        out.writeInt(this.grantUriPermissions ? 1 : 0);
        out.writeTypedArray(this.uriPermissionPatterns, parcelableFlags);
        out.writeTypedArray(this.pathPermissions, parcelableFlags);
        out.writeInt(this.multiprocess ? 1 : 0);
        out.writeInt(this.initOrder);
        out.writeInt(this.flags);
        out.writeInt(this.isSyncable ? 1 : 0);
    }

    public String toString() {
        return "ContentProviderInfo{name=" + this.authority + " className=" + this.name + "}";
    }

    private ProviderInfo(Parcel in) {
        super(in);
        this.authority = null;
        this.readPermission = null;
        this.writePermission = null;
        this.grantUriPermissions = false;
        this.uriPermissionPatterns = null;
        this.pathPermissions = null;
        this.multiprocess = false;
        this.initOrder = 0;
        this.flags = 0;
        this.isSyncable = false;
        this.authority = in.readString();
        this.readPermission = in.readString();
        this.writePermission = in.readString();
        this.grantUriPermissions = in.readInt() != 0;
        this.uriPermissionPatterns = (PatternMatcher[]) in.createTypedArray(PatternMatcher.CREATOR);
        this.pathPermissions = (PathPermission[]) in.createTypedArray(PathPermission.CREATOR);
        this.multiprocess = in.readInt() != 0;
        this.initOrder = in.readInt();
        this.flags = in.readInt();
        this.isSyncable = in.readInt() != 0;
    }
}
