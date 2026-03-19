package com.android.org.conscrypt;

abstract class NativeRef {
    final long context;

    abstract void doFree(long j);

    NativeRef(long j) {
        if (j == 0) {
            throw new NullPointerException("context == 0");
        }
        this.context = j;
    }

    public boolean equals(Object obj) {
        return (obj instanceof NativeRef) && ((NativeRef) obj).context == this.context;
    }

    public int hashCode() {
        return (int) this.context;
    }

    protected void finalize() throws Throwable {
        try {
            if (this.context != 0) {
                doFree(this.context);
            }
        } finally {
            super.finalize();
        }
    }

    static final class EC_GROUP extends NativeRef {
        EC_GROUP(long j) {
            super(j);
        }

        @Override
        void doFree(long j) {
            NativeCrypto.EC_GROUP_clear_free(j);
        }
    }

    static final class EC_POINT extends NativeRef {
        EC_POINT(long j) {
            super(j);
        }

        @Override
        void doFree(long j) {
            NativeCrypto.EC_POINT_clear_free(j);
        }
    }

    static final class EVP_CIPHER_CTX extends NativeRef {
        EVP_CIPHER_CTX(long j) {
            super(j);
        }

        @Override
        void doFree(long j) {
            NativeCrypto.EVP_CIPHER_CTX_free(j);
        }
    }

    static final class EVP_MD_CTX extends NativeRef {
        EVP_MD_CTX(long j) {
            super(j);
        }

        @Override
        void doFree(long j) {
            NativeCrypto.EVP_MD_CTX_destroy(j);
        }
    }

    static final class EVP_PKEY extends NativeRef {
        EVP_PKEY(long j) {
            super(j);
        }

        @Override
        void doFree(long j) {
            NativeCrypto.EVP_PKEY_free(j);
        }
    }

    static final class EVP_PKEY_CTX extends NativeRef {
        EVP_PKEY_CTX(long j) {
            super(j);
        }

        @Override
        void doFree(long j) {
            NativeCrypto.EVP_PKEY_CTX_free(j);
        }
    }

    static final class HMAC_CTX extends NativeRef {
        HMAC_CTX(long j) {
            super(j);
        }

        @Override
        void doFree(long j) {
            NativeCrypto.HMAC_CTX_free(j);
        }
    }

    static final class SSL_SESSION extends NativeRef {
        SSL_SESSION(long j) {
            super(j);
        }

        @Override
        void doFree(long j) {
            NativeCrypto.SSL_SESSION_free(j);
        }
    }
}
