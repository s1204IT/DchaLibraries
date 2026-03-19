package com.android.org.bouncycastle.crypto.params;

import com.android.org.bouncycastle.crypto.CipherParameters;

public class AsymmetricKeyParameter implements CipherParameters {
    boolean privateKey;

    public AsymmetricKeyParameter(boolean z) {
        this.privateKey = z;
    }

    public boolean isPrivate() {
        return this.privateKey;
    }
}
