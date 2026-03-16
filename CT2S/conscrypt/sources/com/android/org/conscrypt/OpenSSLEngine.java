package com.android.org.conscrypt;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import javax.crypto.SecretKey;

public class OpenSSLEngine {
    private static final Object mLoadingLock;
    private final long ctx;

    static {
        NativeCrypto.ENGINE_load_dynamic();
        mLoadingLock = new Object();
    }

    public static OpenSSLEngine getInstance(String engine) throws IllegalArgumentException {
        long engineCtx;
        if (engine == null) {
            throw new NullPointerException("engine == null");
        }
        synchronized (mLoadingLock) {
            engineCtx = NativeCrypto.ENGINE_by_id(engine);
            if (engineCtx == 0) {
                throw new IllegalArgumentException("Unknown ENGINE id: " + engine);
            }
            NativeCrypto.ENGINE_add(engineCtx);
        }
        return new OpenSSLEngine(engineCtx);
    }

    private OpenSSLEngine(long engineCtx) {
        this.ctx = engineCtx;
        if (NativeCrypto.ENGINE_init(engineCtx) == 0) {
            NativeCrypto.ENGINE_free(engineCtx);
            throw new IllegalArgumentException("Could not initialize engine");
        }
    }

    public PrivateKey getPrivateKeyById(String id) throws InvalidKeyException {
        if (id == null) {
            throw new NullPointerException("id == null");
        }
        long keyRef = NativeCrypto.ENGINE_load_private_key(this.ctx, id);
        if (keyRef == 0) {
            return null;
        }
        OpenSSLKey pkey = new OpenSSLKey(keyRef, this, id);
        try {
            return pkey.getPrivateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidKeyException(e);
        }
    }

    public SecretKey getSecretKeyById(String id, String algorithm) throws InvalidKeyException {
        if (id == null) {
            throw new NullPointerException("id == null");
        }
        long keyRef = NativeCrypto.ENGINE_load_private_key(this.ctx, id);
        if (keyRef == 0) {
            return null;
        }
        OpenSSLKey pkey = new OpenSSLKey(keyRef, this, id);
        try {
            return pkey.getSecretKey(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidKeyException(e);
        }
    }

    long getEngineContext() {
        return this.ctx;
    }

    protected void finalize() throws Throwable {
        try {
            NativeCrypto.ENGINE_finish(this.ctx);
            NativeCrypto.ENGINE_free(this.ctx);
        } finally {
            super.finalize();
        }
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof OpenSSLEngine)) {
            return false;
        }
        OpenSSLEngine other = (OpenSSLEngine) o;
        if (other.getEngineContext() == this.ctx) {
            return true;
        }
        String id = NativeCrypto.ENGINE_get_id(this.ctx);
        if (id == null) {
            return false;
        }
        return id.equals(NativeCrypto.ENGINE_get_id(other.getEngineContext()));
    }

    public int hashCode() {
        return (int) this.ctx;
    }
}
