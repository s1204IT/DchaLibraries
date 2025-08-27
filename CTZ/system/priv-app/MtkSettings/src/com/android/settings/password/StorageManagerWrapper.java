package com.android.settings.password;

import android.os.storage.StorageManager;

/* loaded from: classes.dex */
public class StorageManagerWrapper {
    public static boolean isFileEncryptedNativeOrEmulated() {
        return StorageManager.isFileEncryptedNativeOrEmulated();
    }
}
