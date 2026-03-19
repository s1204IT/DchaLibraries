package android.hardware.location;

import android.annotation.SystemApi;
import android.app.backup.FullBackup;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.content.NativeLibraryHelper;

@SystemApi
public class MemoryRegion implements Parcelable {
    public static final Parcelable.Creator<MemoryRegion> CREATOR = new Parcelable.Creator<MemoryRegion>() {
        @Override
        public MemoryRegion createFromParcel(Parcel parcel) {
            return new MemoryRegion(parcel);
        }

        @Override
        public MemoryRegion[] newArray(int i) {
            return new MemoryRegion[i];
        }
    };
    private boolean mIsExecutable;
    private boolean mIsReadable;
    private boolean mIsWritable;
    private int mSizeBytes;
    private int mSizeBytesFree;

    public int getCapacityBytes() {
        return this.mSizeBytes;
    }

    public int getFreeCapacityBytes() {
        return this.mSizeBytesFree;
    }

    public boolean isReadable() {
        return this.mIsReadable;
    }

    public boolean isWritable() {
        return this.mIsWritable;
    }

    public boolean isExecutable() {
        return this.mIsExecutable;
    }

    public String toString() {
        String str;
        String str2;
        String str3;
        if (isReadable()) {
            str = "" + FullBackup.ROOT_TREE_TOKEN;
        } else {
            str = "" + NativeLibraryHelper.CLEAR_ABI_OVERRIDE;
        }
        if (isWritable()) {
            str2 = str + "w";
        } else {
            str2 = str + NativeLibraryHelper.CLEAR_ABI_OVERRIDE;
        }
        if (isExecutable()) {
            str3 = str2 + "x";
        } else {
            str3 = str2 + NativeLibraryHelper.CLEAR_ABI_OVERRIDE;
        }
        return "[ " + this.mSizeBytesFree + "/ " + this.mSizeBytes + " ] : " + str3;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mSizeBytes);
        parcel.writeInt(this.mSizeBytesFree);
        parcel.writeInt(this.mIsReadable ? 1 : 0);
        parcel.writeInt(this.mIsWritable ? 1 : 0);
        parcel.writeInt(this.mIsExecutable ? 1 : 0);
    }

    public MemoryRegion(Parcel parcel) {
        this.mSizeBytes = parcel.readInt();
        this.mSizeBytesFree = parcel.readInt();
        this.mIsReadable = parcel.readInt() != 0;
        this.mIsWritable = parcel.readInt() != 0;
        this.mIsExecutable = parcel.readInt() != 0;
    }
}
