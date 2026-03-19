package com.android.gallery3d.ingest.data;

import android.annotation.TargetApi;
import android.mtp.MtpDevice;
import android.mtp.MtpObjectInfo;

@TargetApi(12)
public class IngestObjectInfo implements Comparable<IngestObjectInfo> {
    private int mCompressedSize;
    private long mDateCreated;
    private int mFormat;
    private int mHandle;

    public IngestObjectInfo(MtpObjectInfo mtpObjectInfo) {
        this.mHandle = mtpObjectInfo.getObjectHandle();
        this.mDateCreated = mtpObjectInfo.getDateCreated();
        this.mFormat = mtpObjectInfo.getFormat();
        this.mCompressedSize = mtpObjectInfo.getCompressedSize();
    }

    public int getCompressedSize() {
        return this.mCompressedSize;
    }

    public int getFormat() {
        return this.mFormat;
    }

    public long getDateCreated() {
        return this.mDateCreated;
    }

    public int getObjectHandle() {
        return this.mHandle;
    }

    public String getName(MtpDevice mtpDevice) {
        MtpObjectInfo objectInfo;
        if (mtpDevice != null && (objectInfo = mtpDevice.getObjectInfo(this.mHandle)) != null) {
            return objectInfo.getName();
        }
        return null;
    }

    @Override
    public int compareTo(IngestObjectInfo ingestObjectInfo) {
        long dateCreated = getDateCreated() - ingestObjectInfo.getDateCreated();
        if (dateCreated < 0) {
            return -1;
        }
        if (dateCreated == 0) {
            return 0;
        }
        return 1;
    }

    public String toString() {
        return "IngestObjectInfo [mHandle=" + this.mHandle + ", mDateCreated=" + this.mDateCreated + ", mFormat=" + this.mFormat + ", mCompressedSize=" + this.mCompressedSize + "]";
    }

    public int hashCode() {
        return (31 * (((((this.mCompressedSize + 31) * 31) + ((int) (this.mDateCreated ^ (this.mDateCreated >>> 32)))) * 31) + this.mFormat)) + this.mHandle;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != 0 && (obj instanceof IngestObjectInfo) && this.mCompressedSize == obj.mCompressedSize && this.mDateCreated == obj.mDateCreated && this.mFormat == obj.mFormat && this.mHandle == obj.mHandle) {
            return true;
        }
        return false;
    }
}
