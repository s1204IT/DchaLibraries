package com.android.org.conscrypt;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.MacSpi;
import javax.crypto.SecretKey;

public abstract class OpenSSLMac extends MacSpi {
    private OpenSSLDigestContext ctx;
    private final long evp_md;
    private final int evp_pkey_type;
    private OpenSSLKey macKey;
    private final byte[] singleByte;
    private final int size;

    private OpenSSLMac(long evp_md, int size, int evp_pkey_type) {
        this.singleByte = new byte[1];
        this.evp_md = evp_md;
        this.size = size;
        this.evp_pkey_type = evp_pkey_type;
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
        if (key instanceof OpenSSLKeyHolder) {
            this.macKey = ((OpenSSLKeyHolder) key).getOpenSSLKey();
        } else {
            byte[] keyBytes = key.getEncoded();
            if (keyBytes == null) {
                throw new InvalidKeyException("key cannot be encoded");
            }
            this.macKey = new OpenSSLKey(NativeCrypto.EVP_PKEY_new_mac_key(this.evp_pkey_type, keyBytes));
        }
        resetContext();
    }

    private final void resetContext() {
        OpenSSLDigestContext ctxLocal = new OpenSSLDigestContext(NativeCrypto.EVP_MD_CTX_create());
        NativeCrypto.EVP_MD_CTX_init(ctxLocal);
        OpenSSLKey macKey = this.macKey;
        if (macKey != null) {
            NativeCrypto.EVP_DigestSignInit(ctxLocal, this.evp_md, macKey.getPkeyContext());
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
        OpenSSLDigestContext ctxLocal = this.ctx;
        NativeCrypto.EVP_DigestUpdate(ctxLocal, input, offset, len);
    }

    @Override
    protected byte[] engineDoFinal() {
        OpenSSLDigestContext ctxLocal = this.ctx;
        byte[] output = NativeCrypto.EVP_DigestSignFinal(ctxLocal);
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
            super(EVP_MD, SIZE, NativeCrypto.EVP_PKEY_HMAC);
        }
    }

    public static class HmacSHA1 extends OpenSSLMac {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha1");
        private static final int SIZE = NativeCrypto.EVP_MD_size(EVP_MD);

        public HmacSHA1() {
            super(EVP_MD, SIZE, NativeCrypto.EVP_PKEY_HMAC);
        }
    }

    public static class HmacSHA224 extends OpenSSLMac {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha224");
        private static final int SIZE = NativeCrypto.EVP_MD_size(EVP_MD);

        public HmacSHA224() throws NoSuchAlgorithmException {
            super(EVP_MD, SIZE, NativeCrypto.EVP_PKEY_HMAC);
        }
    }

    public static class HmacSHA256 extends OpenSSLMac {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha256");
        private static final int SIZE = NativeCrypto.EVP_MD_size(EVP_MD);

        public HmacSHA256() throws NoSuchAlgorithmException {
            super(EVP_MD, SIZE, NativeCrypto.EVP_PKEY_HMAC);
        }
    }

    public static class HmacSHA384 extends OpenSSLMac {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha384");
        private static final int SIZE = NativeCrypto.EVP_MD_size(EVP_MD);

        public HmacSHA384() throws NoSuchAlgorithmException {
            super(EVP_MD, SIZE, NativeCrypto.EVP_PKEY_HMAC);
        }
    }

    public static class HmacSHA512 extends OpenSSLMac {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha512");
        private static final int SIZE = NativeCrypto.EVP_MD_size(EVP_MD);

        public HmacSHA512() {
            super(EVP_MD, SIZE, NativeCrypto.EVP_PKEY_HMAC);
        }
    }
}
