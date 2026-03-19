package com.android.mtp;

import java.util.Objects;

class Identifier {
    final int mDeviceId;
    final String mDocumentId;
    final int mDocumentType;
    final int mObjectHandle;
    final int mStorageId;

    Identifier(int i, int i2, int i3, String str, int i4) {
        this.mDeviceId = i;
        this.mStorageId = i2;
        this.mObjectHandle = i3;
        this.mDocumentId = str;
        this.mDocumentType = i4;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Identifier)) {
            return false;
        }
        Identifier identifier = (Identifier) obj;
        return this.mDeviceId == identifier.mDeviceId && this.mStorageId == identifier.mStorageId && this.mObjectHandle == identifier.mObjectHandle && this.mDocumentId.equals(identifier.mDocumentId);
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mDeviceId), Integer.valueOf(this.mStorageId), Integer.valueOf(this.mObjectHandle), this.mDocumentId);
    }

    public String toString() {
        return "Identifier { mDeviceId: " + this.mDeviceId + ", mStorageId: " + this.mStorageId + ", mObjectHandle: " + this.mObjectHandle + ", mDocumentId: " + this.mDocumentId + ", mDocumentType: " + this.mDocumentType + " }";
    }
}
