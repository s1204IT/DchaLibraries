package com.android.settings.password;

import android.os.storage.StorageManager;

public class StorageManagerWrapper {
    public static boolean isFileEncryptedNativeOrEmulated() {
        return StorageManager.isFileEncryptedNativeOrEmulated();
    }
}
