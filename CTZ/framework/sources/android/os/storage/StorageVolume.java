package android.os.storage;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;
import com.android.internal.content.NativeLibraryHelper;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import java.io.CharArrayWriter;
import java.io.File;

public final class StorageVolume implements Parcelable {
    private static final String ACTION_OPEN_EXTERNAL_DIRECTORY = "android.os.storage.action.OPEN_EXTERNAL_DIRECTORY";
    public static final Parcelable.Creator<StorageVolume> CREATOR = new Parcelable.Creator<StorageVolume>() {
        @Override
        public StorageVolume createFromParcel(Parcel parcel) {
            return new StorageVolume(parcel);
        }

        @Override
        public StorageVolume[] newArray(int i) {
            return new StorageVolume[i];
        }
    };
    public static final String EXTRA_DIRECTORY_NAME = "android.os.storage.extra.DIRECTORY_NAME";
    public static final String EXTRA_STORAGE_VOLUME = "android.os.storage.extra.STORAGE_VOLUME";
    public static final int STORAGE_ID_INVALID = 0;
    public static final int STORAGE_ID_PRIMARY = 65537;
    private final boolean mAllowMassStorage;
    private final String mDescription;
    private final boolean mEmulated;
    private final String mFsUuid;
    private final String mId;
    private final File mInternalPath;
    private final long mMaxFileSize;
    private final UserHandle mOwner;
    private final File mPath;
    private final boolean mPrimary;
    private final boolean mRemovable;
    private final String mState;

    public StorageVolume(String str, File file, File file2, String str2, boolean z, boolean z2, boolean z3, boolean z4, long j, UserHandle userHandle, String str3, String str4) {
        this.mId = (String) Preconditions.checkNotNull(str);
        this.mPath = (File) Preconditions.checkNotNull(file);
        this.mInternalPath = (File) Preconditions.checkNotNull(file2);
        this.mDescription = (String) Preconditions.checkNotNull(str2);
        this.mPrimary = z;
        this.mRemovable = z2;
        this.mEmulated = z3;
        this.mAllowMassStorage = z4;
        this.mMaxFileSize = j;
        this.mOwner = (UserHandle) Preconditions.checkNotNull(userHandle);
        this.mFsUuid = str3;
        this.mState = (String) Preconditions.checkNotNull(str4);
    }

    private StorageVolume(Parcel parcel) {
        this.mId = parcel.readString();
        this.mPath = new File(parcel.readString());
        this.mInternalPath = new File(parcel.readString());
        this.mDescription = parcel.readString();
        this.mPrimary = parcel.readInt() != 0;
        this.mRemovable = parcel.readInt() != 0;
        this.mEmulated = parcel.readInt() != 0;
        this.mAllowMassStorage = parcel.readInt() != 0;
        this.mMaxFileSize = parcel.readLong();
        this.mOwner = (UserHandle) parcel.readParcelable(null);
        this.mFsUuid = parcel.readString();
        this.mState = parcel.readString();
    }

    public String getId() {
        return this.mId;
    }

    public String getPath() {
        return this.mPath.toString();
    }

    public String getInternalPath() {
        return this.mInternalPath.toString();
    }

    public File getPathFile() {
        return this.mPath;
    }

    public String getDescription(Context context) {
        return this.mDescription;
    }

    public boolean isPrimary() {
        return this.mPrimary;
    }

    public boolean isRemovable() {
        return this.mRemovable;
    }

    public boolean isEmulated() {
        return this.mEmulated;
    }

    public boolean allowMassStorage() {
        return this.mAllowMassStorage;
    }

    public long getMaxFileSize() {
        return this.mMaxFileSize;
    }

    public UserHandle getOwner() {
        return this.mOwner;
    }

    public String getUuid() {
        return this.mFsUuid;
    }

    public int getFatVolumeId() {
        if (this.mFsUuid == null || this.mFsUuid.length() != 9) {
            return -1;
        }
        try {
            return (int) Long.parseLong(this.mFsUuid.replace(NativeLibraryHelper.CLEAR_ABI_OVERRIDE, ""), 16);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public String getUserLabel() {
        return this.mDescription;
    }

    public String getState() {
        return this.mState;
    }

    public Intent createAccessIntent(String str) {
        if (!isPrimary() || str != null) {
            if (str != null && !Environment.isStandardDirectory(str)) {
                return null;
            }
            Intent intent = new Intent(ACTION_OPEN_EXTERNAL_DIRECTORY);
            intent.putExtra(EXTRA_STORAGE_VOLUME, this);
            intent.putExtra(EXTRA_DIRECTORY_NAME, str);
            return intent;
        }
        return null;
    }

    public boolean equals(Object obj) {
        if ((obj instanceof StorageVolume) && this.mPath != null) {
            return this.mPath.equals(((StorageVolume) obj).mPath);
        }
        return false;
    }

    public int hashCode() {
        return this.mPath.hashCode();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("StorageVolume: ");
        sb.append(this.mDescription);
        if (this.mFsUuid != null) {
            sb.append(" (");
            sb.append(this.mFsUuid);
            sb.append(")");
        }
        return sb.toString();
    }

    public String dump() {
        CharArrayWriter charArrayWriter = new CharArrayWriter();
        dump(new IndentingPrintWriter(charArrayWriter, "    ", 80));
        return charArrayWriter.toString();
    }

    public void dump(IndentingPrintWriter indentingPrintWriter) {
        indentingPrintWriter.println("StorageVolume:");
        indentingPrintWriter.increaseIndent();
        indentingPrintWriter.printPair("mId", this.mId);
        indentingPrintWriter.printPair("mPath", this.mPath);
        indentingPrintWriter.printPair("mInternalPath", this.mInternalPath);
        indentingPrintWriter.printPair("mDescription", this.mDescription);
        indentingPrintWriter.printPair("mPrimary", Boolean.valueOf(this.mPrimary));
        indentingPrintWriter.printPair("mRemovable", Boolean.valueOf(this.mRemovable));
        indentingPrintWriter.printPair("mEmulated", Boolean.valueOf(this.mEmulated));
        indentingPrintWriter.printPair("mAllowMassStorage", Boolean.valueOf(this.mAllowMassStorage));
        indentingPrintWriter.printPair("mMaxFileSize", Long.valueOf(this.mMaxFileSize));
        indentingPrintWriter.printPair("mOwner", this.mOwner);
        indentingPrintWriter.printPair("mFsUuid", this.mFsUuid);
        indentingPrintWriter.printPair("mState", this.mState);
        indentingPrintWriter.decreaseIndent();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mId);
        parcel.writeString(this.mPath.toString());
        parcel.writeString(this.mInternalPath.toString());
        parcel.writeString(this.mDescription);
        parcel.writeInt(this.mPrimary ? 1 : 0);
        parcel.writeInt(this.mRemovable ? 1 : 0);
        parcel.writeInt(this.mEmulated ? 1 : 0);
        parcel.writeInt(this.mAllowMassStorage ? 1 : 0);
        parcel.writeLong(this.mMaxFileSize);
        parcel.writeParcelable(this.mOwner, i);
        parcel.writeString(this.mFsUuid);
        parcel.writeString(this.mState);
    }

    public static final class ScopedAccessProviderContract {
        public static final String AUTHORITY = "com.android.documentsui.scopedAccess";
        public static final String COL_DIRECTORY = "directory";
        public static final String COL_PACKAGE = "package_name";
        public static final String TABLE_PACKAGES = "packages";
        public static final int TABLE_PACKAGES_COL_PACKAGE = 0;
        public static final String TABLE_PERMISSIONS = "permissions";
        public static final int TABLE_PERMISSIONS_COL_DIRECTORY = 2;
        public static final int TABLE_PERMISSIONS_COL_GRANTED = 3;
        public static final int TABLE_PERMISSIONS_COL_PACKAGE = 0;
        public static final int TABLE_PERMISSIONS_COL_VOLUME_UUID = 1;
        public static final String[] TABLE_PACKAGES_COLUMNS = {"package_name"};
        public static final String COL_VOLUME_UUID = "volume_uuid";
        public static final String COL_GRANTED = "granted";
        public static final String[] TABLE_PERMISSIONS_COLUMNS = {"package_name", COL_VOLUME_UUID, "directory", COL_GRANTED};

        private ScopedAccessProviderContract() {
            throw new UnsupportedOperationException("contains constants only");
        }
    }
}
