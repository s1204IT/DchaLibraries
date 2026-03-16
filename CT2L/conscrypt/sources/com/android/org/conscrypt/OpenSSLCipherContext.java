package com.android.org.conscrypt;

class OpenSSLCipherContext {
    private final long context;

    OpenSSLCipherContext(long ctx) {
        if (ctx == 0) {
            throw new NullPointerException("ctx == 0");
        }
        this.context = ctx;
    }

    protected void finalize() throws Throwable {
        try {
            NativeCrypto.EVP_CIPHER_CTX_free(this.context);
        } finally {
            super.finalize();
        }
    }

    long getContext() {
        return this.context;
    }
}
