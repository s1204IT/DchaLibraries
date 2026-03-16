package com.android.org.conscrypt;

import java.security.MessageDigestSpi;
import java.security.NoSuchAlgorithmException;

public class OpenSSLMessageDigestJDK extends MessageDigestSpi implements Cloneable {
    private OpenSSLDigestContext ctx;
    private final long evp_md;
    private final byte[] singleByte;
    private final int size;

    private OpenSSLMessageDigestJDK(long evp_md, int size) throws NoSuchAlgorithmException {
        this.singleByte = new byte[1];
        this.evp_md = evp_md;
        this.size = size;
        resetContext();
    }

    private OpenSSLMessageDigestJDK(long evp_md, int size, OpenSSLDigestContext ctx) {
        this.singleByte = new byte[1];
        this.evp_md = evp_md;
        this.size = size;
        this.ctx = ctx;
    }

    private final void resetContext() {
        OpenSSLDigestContext ctxLocal = new OpenSSLDigestContext(NativeCrypto.EVP_MD_CTX_create());
        NativeCrypto.EVP_MD_CTX_init(ctxLocal);
        NativeCrypto.EVP_DigestInit(ctxLocal, this.evp_md);
        this.ctx = ctxLocal;
    }

    @Override
    protected void engineReset() {
        resetContext();
    }

    @Override
    protected int engineGetDigestLength() {
        return this.size;
    }

    @Override
    protected void engineUpdate(byte input) {
        this.singleByte[0] = input;
        engineUpdate(this.singleByte, 0, 1);
    }

    @Override
    protected void engineUpdate(byte[] input, int offset, int len) {
        NativeCrypto.EVP_DigestUpdate(this.ctx, input, offset, len);
    }

    @Override
    protected byte[] engineDigest() {
        byte[] result = new byte[this.size];
        NativeCrypto.EVP_DigestFinal(this.ctx, result, 0);
        resetContext();
        return result;
    }

    public static class MD5 extends OpenSSLMessageDigestJDK {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("md5");
        private static final int SIZE = NativeCrypto.EVP_MD_size(EVP_MD);

        public MD5() throws NoSuchAlgorithmException {
            super(EVP_MD, SIZE);
        }
    }

    public static class SHA1 extends OpenSSLMessageDigestJDK {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha1");
        private static final int SIZE = NativeCrypto.EVP_MD_size(EVP_MD);

        public SHA1() throws NoSuchAlgorithmException {
            super(EVP_MD, SIZE);
        }
    }

    public static class SHA224 extends OpenSSLMessageDigestJDK {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha224");
        private static final int SIZE = NativeCrypto.EVP_MD_size(EVP_MD);

        public SHA224() throws NoSuchAlgorithmException {
            super(EVP_MD, SIZE);
        }
    }

    public static class SHA256 extends OpenSSLMessageDigestJDK {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha256");
        private static final int SIZE = NativeCrypto.EVP_MD_size(EVP_MD);

        public SHA256() throws NoSuchAlgorithmException {
            super(EVP_MD, SIZE);
        }
    }

    public static class SHA384 extends OpenSSLMessageDigestJDK {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha384");
        private static final int SIZE = NativeCrypto.EVP_MD_size(EVP_MD);

        public SHA384() throws NoSuchAlgorithmException {
            super(EVP_MD, SIZE);
        }
    }

    public static class SHA512 extends OpenSSLMessageDigestJDK {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha512");
        private static final int SIZE = NativeCrypto.EVP_MD_size(EVP_MD);

        public SHA512() throws NoSuchAlgorithmException {
            super(EVP_MD, SIZE);
        }
    }

    @Override
    public Object clone() {
        OpenSSLDigestContext ctxCopy = new OpenSSLDigestContext(NativeCrypto.EVP_MD_CTX_create());
        NativeCrypto.EVP_MD_CTX_init(ctxCopy);
        NativeCrypto.EVP_MD_CTX_copy(ctxCopy, this.ctx);
        return new OpenSSLMessageDigestJDK(this.evp_md, this.size, ctxCopy);
    }
}
