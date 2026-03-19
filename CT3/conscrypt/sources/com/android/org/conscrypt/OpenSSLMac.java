package com.android.org.conscrypt;

import com.android.org.conscrypt.NativeRef;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.MacSpi;
import javax.crypto.SecretKey;

public abstract class OpenSSLMac extends MacSpi {
    private NativeRef.HMAC_CTX ctx;
    private final long evp_md;
    private byte[] keyBytes;
    private final byte[] singleByte;
    private final int size;

    OpenSSLMac(long evp_md, int size, OpenSSLMac openSSLMac) {
        this(evp_md, size);
    }

    private OpenSSLMac(long evp_md, int size) {
        this.singleByte = new byte[1];
        this.evp_md = evp_md;
        this.size = size;
    }

    @Override
    protected int engineGetMacLength() {
        return this.size;
    }

    @Override
    protected void engineInit(Key key, AlgorithmParameterSpec params) throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (!(key instanceof SecretKey)) {
            throw new InvalidKeyException("key must be a SecretKey");
        }
        if (params != null) {
            throw new InvalidAlgorithmParameterException("unknown parameter type");
        }
        this.keyBytes = key.getEncoded();
        if (this.keyBytes == null) {
            throw new InvalidKeyException("key cannot be encoded");
        }
        resetContext();
    }

    private final void resetContext() {
        NativeRef.HMAC_CTX ctxLocal = new NativeRef.HMAC_CTX(NativeCrypto.HMAC_CTX_new());
        if (this.keyBytes != null) {
            NativeCrypto.HMAC_Init_ex(ctxLocal, this.keyBytes, this.evp_md);
        }
        this.ctx = ctxLocal;
    }

    @Override
    protected void engineUpdate(byte input) {
        this.singleByte[0] = input;
        engineUpdate(this.singleByte, 0, 1);
    }

    @Override
    protected void engineUpdate(byte[] input, int offset, int len) {
        NativeRef.HMAC_CTX ctxLocal = this.ctx;
        NativeCrypto.HMAC_Update(ctxLocal, input, offset, len);
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
        NativeRef.HMAC_CTX ctxLocal = this.ctx;
        NativeCrypto.HMAC_UpdateDirect(ctxLocal, ptr, len);
        input.position(position + len);
    }

    @Override
    protected byte[] engineDoFinal() {
        NativeRef.HMAC_CTX ctxLocal = this.ctx;
        byte[] output = NativeCrypto.HMAC_Final(ctxLocal);
        resetContext();
        return output;
    }

    @Override
    protected void engineReset() {
        resetContext();
    }

    public static class HmacMD5 extends OpenSSLMac {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("md5");
        private static final int SIZE = NativeCrypto.EVP_MD_size(EVP_MD);

        public HmacMD5() {
            super(EVP_MD, SIZE, null);
        }
    }

    public static class HmacSHA1 extends OpenSSLMac {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha1");
        private static final int SIZE = NativeCrypto.EVP_MD_size(EVP_MD);

        public HmacSHA1() {
            super(EVP_MD, SIZE, null);
        }
    }

    public static class HmacSHA224 extends OpenSSLMac {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha224");
        private static final int SIZE = NativeCrypto.EVP_MD_size(EVP_MD);

        public HmacSHA224() throws NoSuchAlgorithmException {
            super(EVP_MD, SIZE, null);
        }
    }

    public static class HmacSHA256 extends OpenSSLMac {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha256");
        private static final int SIZE = NativeCrypto.EVP_MD_size(EVP_MD);

        public HmacSHA256() throws NoSuchAlgorithmException {
            super(EVP_MD, SIZE, null);
        }
    }

    public static class HmacSHA384 extends OpenSSLMac {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha384");
        private static final int SIZE = NativeCrypto.EVP_MD_size(EVP_MD);

        public HmacSHA384() throws NoSuchAlgorithmException {
            super(EVP_MD, SIZE, null);
        }
    }

    public static class HmacSHA512 extends OpenSSLMac {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha512");
        private static final int SIZE = NativeCrypto.EVP_MD_size(EVP_MD);

        public HmacSHA512() {
            super(EVP_MD, SIZE, null);
        }
    }
}
