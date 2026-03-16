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

    public String getName(MtpDevice device) {
        MtpObjectInfo info;
        if (device == null || (info = device.getObjectInfo(this.mHandle)) == null) {
            return null;
        }
        return info.getName();
    }

    @Override
    public int compareTo(IngestObjectInfo another) {
        long diff = getDateCreated() - another.getDateCreated();
        if (diff < 0) {
            return -1;
        }
        if (diff == 0) {
            return 0;
        }
        return 1;
    }

    public String toString() {
        return "IngestObjectInfo [mHandle=" + this.mHandle + ", mDateCreated=" + this.mDateCreated + ", mFormat=" + this.mFormat + ", mCompressedSize=" + this.mCompressedSize + "]";
    }

    public int hashCode() {
        int result = this.mCompressedSize + 31;
        return (((((result * 31) + ((int) (this.mDateCreated ^ (this.mDateCreated >>> 32)))) * 31) + this.mFormat) * 31) + this.mHandle;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && (obj instanceof IngestObjectInfo)) {
            IngestObjectInfo other = (IngestObjectInfo) obj;
            return this.mCompressedSize == other.mCompressedSize && this.mDateCreated == other.mDateCreated && this.mFormat == other.mFormat && this.mHandle == other.mHandle;
        }
        return false;
    }
}
