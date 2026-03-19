package com.android.mtp;

import android.mtp.MtpStorageInfo;
import com.android.internal.annotations.VisibleForTesting;

class MtpRoot {
    final String mDescription;
    final int mDeviceId;
    final long mFreeSpace;
    final long mMaxCapacity;
    final int mStorageId;
    final String mVolumeIdentifier;

    @VisibleForTesting
    MtpRoot(int i, int i2, String str, long j, long j2, String str2) {
        this.mDeviceId = i;
        this.mStorageId = i2;
        this.mDescription = str;
        this.mFreeSpace = j;
        this.mMaxCapacity = j2;
        this.mVolumeIdentifier = str2;
    }

    MtpRoot(int i, MtpStorageInfo mtpStorageInfo) {
        this.mDeviceId = i;
        this.mStorageId = mtpStorageInfo.getStorageId();
        this.mDescription = mtpStorageInfo.getDescription();
        this.mFreeSpace = mtpStorageInfo.getFreeSpace();
        this.mMaxCapacity = mtpStorageInfo.getMaxCapacity();
        this.mVolumeIdentifier = mtpStorageInfo.getVolumeIdentifier();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof MtpRoot)) {
            return false;
        }
        MtpRoot mtpRoot = (MtpRoot) obj;
        return this.mDeviceId == mtpRoot.mDeviceId && this.mStorageId == mtpRoot.mStorageId && this.mDescription.equals(mtpRoot.mDescription) && this.mFreeSpace == mtpRoot.mFreeSpace && this.mMaxCapacity == mtpRoot.mMaxCapacity && this.mVolumeIdentifier.equals(mtpRoot.mVolumeIdentifier);
    }

    public int hashCode() {
        return ((((this.mDeviceId ^ this.mStorageId) ^ this.mDescription.hashCode()) ^ ((int) this.mFreeSpace)) ^ ((int) this.mMaxCapacity)) ^ this.mVolumeIdentifier.hashCode();
    }

    public String toString() {
        return "MtpRoot{Name: " + this.mDescription + "}";
    }
}
