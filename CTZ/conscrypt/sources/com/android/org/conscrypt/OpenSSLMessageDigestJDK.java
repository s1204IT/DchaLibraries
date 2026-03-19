package com.android.org.conscrypt;

import com.android.org.conscrypt.EvpMdRef;
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

    private OpenSSLMessageDigestJDK(long j, int i) throws NoSuchAlgorithmException {
        this.singleByte = new byte[1];
        this.evp_md = j;
        this.size = i;
        this.ctx = new NativeRef.EVP_MD_CTX(NativeCrypto.EVP_MD_CTX_create());
    }

    private OpenSSLMessageDigestJDK(long j, int i, NativeRef.EVP_MD_CTX evp_md_ctx, boolean z) {
        this.singleByte = new byte[1];
        this.evp_md = j;
        this.size = i;
        this.ctx = evp_md_ctx;
        this.digestInitializedInContext = z;
    }

    private void ensureDigestInitializedInContext() {
        if (!this.digestInitializedInContext) {
            NativeCrypto.EVP_DigestInit_ex(this.ctx, this.evp_md);
            this.digestInitializedInContext = true;
        }
    }

    @Override
    protected void engineReset() {
        NativeCrypto.EVP_MD_CTX_cleanup(this.ctx);
        this.digestInitializedInContext = false;
    }

    @Override
    protected int engineGetDigestLength() {
        return this.size;
    }

    @Override
    protected void engineUpdate(byte b) {
        this.singleByte[0] = b;
        engineUpdate(this.singleByte, 0, 1);
    }

    @Override
    protected void engineUpdate(byte[] bArr, int i, int i2) {
        ensureDigestInitializedInContext();
        NativeCrypto.EVP_DigestUpdate(this.ctx, bArr, i, i2);
    }

    @Override
    protected void engineUpdate(ByteBuffer byteBuffer) {
        if (!byteBuffer.hasRemaining()) {
            return;
        }
        if (!byteBuffer.isDirect()) {
            super.engineUpdate(byteBuffer);
            return;
        }
        long directBufferAddress = NativeCrypto.getDirectBufferAddress(byteBuffer);
        if (directBufferAddress == 0) {
            super.engineUpdate(byteBuffer);
            return;
        }
        int iPosition = byteBuffer.position();
        if (iPosition < 0) {
            throw new RuntimeException("Negative position");
        }
        long j = directBufferAddress + ((long) iPosition);
        int iRemaining = byteBuffer.remaining();
        if (iRemaining < 0) {
            throw new RuntimeException("Negative remaining amount");
        }
        ensureDigestInitializedInContext();
        NativeCrypto.EVP_DigestUpdateDirect(this.ctx, j, iRemaining);
        byteBuffer.position(iPosition + iRemaining);
    }

    @Override
    protected byte[] engineDigest() {
        ensureDigestInitializedInContext();
        byte[] bArr = new byte[this.size];
        NativeCrypto.EVP_DigestFinal_ex(this.ctx, bArr, 0);
        this.digestInitializedInContext = false;
        return bArr;
    }

    public static final class MD5 extends OpenSSLMessageDigestJDK {
        public MD5() throws NoSuchAlgorithmException {
            super(EvpMdRef.MD5.EVP_MD, EvpMdRef.MD5.SIZE_BYTES);
        }
    }

    public static final class SHA1 extends OpenSSLMessageDigestJDK {
        public SHA1() throws NoSuchAlgorithmException {
            super(EvpMdRef.SHA1.EVP_MD, EvpMdRef.SHA1.SIZE_BYTES);
        }
    }

    public static final class SHA224 extends OpenSSLMessageDigestJDK {
        public SHA224() throws NoSuchAlgorithmException {
            super(EvpMdRef.SHA224.EVP_MD, EvpMdRef.SHA224.SIZE_BYTES);
        }
    }

    public static final class SHA256 extends OpenSSLMessageDigestJDK {
        public SHA256() throws NoSuchAlgorithmException {
            super(EvpMdRef.SHA256.EVP_MD, EvpMdRef.SHA256.SIZE_BYTES);
        }
    }

    public static final class SHA384 extends OpenSSLMessageDigestJDK {
        public SHA384() throws NoSuchAlgorithmException {
            super(EvpMdRef.SHA384.EVP_MD, EvpMdRef.SHA384.SIZE_BYTES);
        }
    }

    public static final class SHA512 extends OpenSSLMessageDigestJDK {
        public SHA512() throws NoSuchAlgorithmException {
            super(EvpMdRef.SHA512.EVP_MD, EvpMdRef.SHA512.SIZE_BYTES);
        }
    }

    @Override
    public Object clone() {
        NativeRef.EVP_MD_CTX evp_md_ctx = new NativeRef.EVP_MD_CTX(NativeCrypto.EVP_MD_CTX_create());
        if (this.digestInitializedInContext) {
            NativeCrypto.EVP_MD_CTX_copy_ex(evp_md_ctx, this.ctx);
        }
        return new OpenSSLMessageDigestJDK(this.evp_md, this.size, evp_md_ctx, this.digestInitializedInContext);
    }
}
