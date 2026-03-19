package android.hardware.location;

import android.app.backup.FullBackup;
import android.net.ProxyInfo;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;

public class MemoryRegion implements Parcelable {
    public static final Parcelable.Creator<MemoryRegion> CREATOR = new Parcelable.Creator<MemoryRegion>() {
        @Override
        public MemoryRegion createFromParcel(Parcel in) {
            return new MemoryRegion(in);
        }

        @Override
        public MemoryRegion[] newArray(int size) {
            return new MemoryRegion[size];
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
        String mask;
        String mask2;
        String mask3 = isReadable() ? ProxyInfo.LOCAL_EXCL_LIST + FullBackup.ROOT_TREE_TOKEN : ProxyInfo.LOCAL_EXCL_LIST + ContactsContract.Aas.ENCODE_SYMBOL;
        if (isWritable()) {
            mask = mask3 + "w";
        } else {
            mask = mask3 + ContactsContract.Aas.ENCODE_SYMBOL;
        }
        if (isExecutable()) {
            mask2 = mask + "x";
        } else {
            mask2 = mask + ContactsContract.Aas.ENCODE_SYMBOL;
        }
        String retVal = "[ " + this.mSizeBytesFree + "/ " + this.mSizeBytes + " ] : " + mask2;
        return retVal;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mSizeBytes);
        dest.writeInt(this.mSizeBytesFree);
        dest.writeInt(this.mIsReadable ? 1 : 0);
        dest.writeInt(this.mIsWritable ? 1 : 0);
        dest.writeInt(this.mIsExecutable ? 1 : 0);
    }

    public MemoryRegion(Parcel source) {
        this.mSizeBytes = source.readInt();
        this.mSizeBytesFree = source.readInt();
        this.mIsReadable = source.readInt() != 0;
        this.mIsWritable = source.readInt() != 0;
        this.mIsExecutable = source.readInt() != 0;
    }
}
