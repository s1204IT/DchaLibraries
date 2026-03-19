package com.android.org.conscrypt;

import com.android.org.conscrypt.NativeRef;
import java.nio.ByteBuffer;
import java.security.MessageDigestSpi;
import java.security.NoSuchAlgorithmException;

public class OpenSSLMessageDigestJDK extends MessageDigestSpi implements Cloneable {
    private final NativeRef.EVP_MD_CTX ctx;
    private boolean digestInitializedInContext;
    private final long evp_md;
    private final byte[] singleByte;
    private final int size;

    OpenSSLMessageDigestJDK(long evp_md, int size, OpenSSLMessageDigestJDK openSSLMessageDigestJDK) {
        this(evp_md, size);
    }

    private OpenSSLMessageDigestJDK(long evp_md, int size) throws NoSuchAlgorithmException {
        this.singleByte = new byte[1];
        this.evp_md = evp_md;
        this.size = size;
        NativeRef.EVP_MD_CTX ctxLocal = new NativeRef.EVP_MD_CTX(NativeCrypto.EVP_MD_CTX_create());
        this.ctx = ctxLocal;
    }

    private OpenSSLMessageDigestJDK(long evp_md, int size, NativeRef.EVP_MD_CTX ctx, boolean digestInitializedInContext) {
        this.singleByte = new byte[1];
        this.evp_md = evp_md;
        this.size = size;
        this.ctx = ctx;
        this.digestInitializedInContext = digestInitializedInContext;
    }

    private void ensureDigestInitializedInContext() {
        if (this.digestInitializedInContext) {
            return;
        }
        NativeRef.EVP_MD_CTX ctxLocal = this.ctx;
        NativeCrypto.EVP_DigestInit_ex(ctxLocal, this.evp_md);
        this.digestInitializedInContext = true;
    }

    @Override
    protected void engineReset() {
        NativeRef.EVP_MD_CTX ctxLocal = this.ctx;
        NativeCrypto.EVP_MD_CTX_cleanup(ctxLocal);
        this.digestInitializedInContext = false;
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
        ensureDigestInitializedInContext();
        NativeCrypto.EVP_DigestUpdate(this.ctx, input, offset, len);
    }

    @Override
    protected void engineUpdate(ByteBuffer input) {
        if (!input.hasRemaining()) {
            return;
        }
        if (!input.isDirect()) {
            super.engineUpdate(input);
            return;
        }
        long baseAddress = NativeCrypto.getDirectBufferAddress(input);
        if (baseAddress == 0) {
            super.engineUpdate(input);
            return;
        }
        int position = input.position();
        if (position < 0) {
            throw new RuntimeException("Negative position");
        }
        long ptr = baseAddress + ((long) position);
        int len = input.remaining();
        if (len < 0) {
            throw new RuntimeException("Negative remaining amount");
        }
        ensureDigestInitializedInContext();
        NativeCrypto.EVP_DigestUpdateDirect(this.ctx, ptr, len);
        input.position(position + len);
    }

    @Override
    protected byte[] engineDigest() {
        ensureDigestInitializedInContext();
        byte[] result = new byte[this.size];
        NativeCrypto.EVP_DigestFinal_ex(this.ctx, result, 0);
        this.digestInitializedInContext = false;
        return result;
    }

    public static class MD5 extends OpenSSLMessageDigestJDK {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("md5");
        private static final int SIZE = NativeCrypto.EVP_MD_size(EVP_MD);

        public MD5() throws NoSuchAlgorithmException {
            super(EVP_MD, SIZE, null);
        }
    }

    public static class SHA1 extends OpenSSLMessageDigestJDK {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha1");
        private static final int SIZE = NativeCrypto.EVP_MD_size(EVP_MD);

        public SHA1() throws NoSuchAlgorithmException {
            super(EVP_MD, SIZE, null);
        }
    }

    public static class SHA224 extends OpenSSLMessageDigestJDK {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha224");
        private static final int SIZE = NativeCrypto.EVP_MD_size(EVP_MD);

        public SHA224() throws NoSuchAlgorithmException {
            super(EVP_MD, SIZE, null);
        }
    }

    public static class SHA256 extends OpenSSLMessageDigestJDK {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha256");
        private static final int SIZE = NativeCrypto.EVP_MD_size(EVP_MD);

        public SHA256() throws NoSuchAlgorithmException {
            super(EVP_MD, SIZE, null);
        }
    }

    public static class SHA384 extends OpenSSLMessageDigestJDK {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha384");
        private static final int SIZE = NativeCrypto.EVP_MD_size(EVP_MD);

        public SHA384() throws NoSuchAlgorithmException {
            super(EVP_MD, SIZE, null);
        }
    }

    public static class SHA512 extends OpenSSLMessageDigestJDK {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha512");
        private static final int SIZE = NativeCrypto.EVP_MD_size(EVP_MD);

        public SHA512() throws NoSuchAlgorithmException {
            super(EVP_MD, SIZE, null);
        }
    }

    @Override
    public Object clone() {
        NativeRef.EVP_MD_CTX ctxCopy = new NativeRef.EVP_MD_CTX(NativeCrypto.EVP_MD_CTX_create());
        if (this.digestInitializedInContext) {
            NativeCrypto.EVP_MD_CTX_copy_ex(ctxCopy, this.ctx);
        }
        return new OpenSSLMessageDigestJDK(this.evp_md, this.size, ctxCopy, this.digestInitializedInContext);
    }
}
