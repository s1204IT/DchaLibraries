package com.android.server.locksettings.recoverablekeystore;

import android.security.keystore.AndroidKeyStoreSecretKey;

public class PlatformEncryptionKey {
    private final int mGenerationId;
    private final AndroidKeyStoreSecretKey mKey;

    public PlatformEncryptionKey(int i, AndroidKeyStoreSecretKey androidKeyStoreSecretKey) {
        this.mGenerationId = i;
        this.mKey = androidKeyStoreSecretKey;
    }

    public int getGenerationId() {
        return this.mGenerationId;
    }

    public AndroidKeyStoreSecretKey getKey() {
        return this.mKey;
    }
}
