package android.security.keystore;

import java.security.KeyStore;

class AndroidKeyStoreLoadStoreParameter implements KeyStore.LoadStoreParameter {
    private final int mUid;

    AndroidKeyStoreLoadStoreParameter(int i) {
        this.mUid = i;
    }

    @Override
    public KeyStore.ProtectionParameter getProtectionParameter() {
        return null;
    }

    int getUid() {
        return this.mUid;
    }
}
