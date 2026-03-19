package com.android.server.backup.params;

import android.os.ParcelFileDescriptor;

public class AdbRestoreParams extends AdbParams {
    public AdbRestoreParams(ParcelFileDescriptor parcelFileDescriptor) {
        this.fd = parcelFileDescriptor;
    }
}
