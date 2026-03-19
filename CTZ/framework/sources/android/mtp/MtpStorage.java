package android.mtp;

import android.os.storage.StorageVolume;

public class MtpStorage {
    private final String mDescription;
    private final long mMaxFileSize;
    private final String mPath;
    private final boolean mRemovable;
    private final int mStorageId;

    public MtpStorage(StorageVolume storageVolume, int i) {
        this.mStorageId = i;
        this.mPath = storageVolume.getInternalPath();
        this.mDescription = storageVolume.getDescription(null);
        this.mRemovable = storageVolume.isRemovable();
        this.mMaxFileSize = storageVolume.getMaxFileSize();
    }

    public final int getStorageId() {
        return this.mStorageId;
    }

    public final String getPath() {
        return this.mPath;
    }

    public final String getDescription() {
        return this.mDescription;
    }

    public final boolean isRemovable() {
        return this.mRemovable;
    }

    public long getMaxFileSize() {
        return this.mMaxFileSize;
    }
}
