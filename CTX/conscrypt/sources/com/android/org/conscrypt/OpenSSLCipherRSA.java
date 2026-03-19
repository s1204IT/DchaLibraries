package com.android.org.conscrypt;

import com.android.org.conscrypt.EvpMdRef;
import com.android.org.conscrypt.NativeRef;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Locale;
import javax.crypto.BadPaddingException;
import javax.crypto.CipherSpi;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;

abstract class OpenSSLCipherRSA extends CipherSpi {
    private byte[] buffer;
    private int bufferOffset;
    boolean encrypting;
    private boolean inputTooLarge;
    OpenSSLKey key;
    int padding;
    boolean usingPrivateKey;

    abstract int doCryptoOperation(byte[] bArr, byte[] bArr2) throws BadPaddingException, IllegalBlockSizeException;

    OpenSSLCipherRSA(int i) {
        this.padding = 1;
        this.padding = i;
    }

    @Override
    protected void engineSetMode(String str) throws NoSuchAlgorithmException {
        String upperCase = str.toUpperCase(Locale.ROOT);
        if ("NONE".equals(upperCase) || "ECB".equals(upperCase)) {
            return;
        }
        throw new NoSuchAlgorithmException("mode not supported: " + str);
    }

    @Override
    protected void engineSetPadding(String str) throws NoSuchPaddingException {
        String upperCase = str.toUpperCase(Locale.ROOT);
        if ("PKCS1PADDING".equals(upperCase)) {
            this.padding = 1;
        } else {
            if ("NOPADDING".equals(upperCase)) {
                this.padding = 3;
                return;
            }
            throw new NoSuchPaddingException("padding not supported: " + str);
        }
    }

    @Override
    protected int engineGetBlockSize() {
        if (this.encrypting) {
            return paddedBlockSizeBytes();
        }
        return keySizeBytes();
    }

    @Override
    protected int engineGetOutputSize(int i) {
        if (this.encrypting) {
            return keySizeBytes();
        }
        return paddedBlockSizeBytes();
    }

    int paddedBlockSizeBytes() {
        int iKeySizeBytes = keySizeBytes();
        if (this.padding == 1) {
            return (iKeySizeBytes - 1) - 10;
        }
        return iKeySizeBytes;
    }

    int keySizeBytes() {
        if (!isInitialized()) {
            throw new IllegalStateException("cipher is not initialized");
        }
        return NativeCrypto.RSA_size(this.key.getNativeRef());
    }

    boolean isInitialized() {
        return this.key != null;
    }

    @Override
    protected byte[] engineGetIV() {
        return null;
    }

    @Override
    protected AlgorithmParameters engineGetParameters() {
        return null;
    }

    void doCryptoInit(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidKeyException, InvalidAlgorithmParameterException {
    }

    void engineInitInternal(int i, Key key, AlgorithmParameterSpec algorithmParameterSpec) throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (i == 1 || i == 3) {
            this.encrypting = true;
        } else if (i == 2 || i == 4) {
            this.encrypting = false;
        } else {
            throw new InvalidParameterException("Unsupported opmode " + i);
        }
        if (key instanceof OpenSSLRSAPrivateKey) {
            this.usingPrivateKey = true;
            this.key = ((OpenSSLRSAPrivateKey) key).getOpenSSLKey();
        } else if (key instanceof RSAPrivateCrtKey) {
            this.usingPrivateKey = true;
            this.key = OpenSSLRSAPrivateCrtKey.getInstance((RSAPrivateCrtKey) key);
        } else if (key instanceof RSAPrivateKey) {
            this.usingPrivateKey = true;
            this.key = OpenSSLRSAPrivateKey.getInstance((RSAPrivateKey) key);
        } else if (key instanceof OpenSSLRSAPublicKey) {
            this.usingPrivateKey = false;
            this.key = ((OpenSSLRSAPublicKey) key).getOpenSSLKey();
        } else if (key instanceof RSAPublicKey) {
            this.usingPrivateKey = false;
            this.key = OpenSSLRSAPublicKey.getInstance((RSAPublicKey) key);
        } else {
            if (key == null) {
                throw new InvalidKeyException("RSA private or public key is null");
            }
            throw new InvalidKeyException("Need RSA private or public key");
        }
        this.buffer = new byte[NativeCrypto.RSA_size(this.key.getNativeRef())];
        this.bufferOffset = 0;
        this.inputTooLarge = false;
        doCryptoInit(algorithmParameterSpec);
    }

    @Override
    protected int engineGetKeySize(Key key) throws InvalidKeyException {
        if (key instanceof OpenSSLRSAPrivateKey) {
            return ((OpenSSLRSAPrivateKey) key).getModulus().bitLength();
        }
        if (key instanceof RSAPrivateCrtKey) {
            return ((RSAPrivateCrtKey) key).getModulus().bitLength();
        }
        if (key instanceof RSAPrivateKey) {
            return ((RSAPrivateKey) key).getModulus().bitLength();
        }
        if (key instanceof OpenSSLRSAPublicKey) {
            return ((OpenSSLRSAPublicKey) key).getModulus().bitLength();
        }
        if (key instanceof RSAPublicKey) {
            return ((RSAPublicKey) key).getModulus().bitLength();
        }
        if (key == null) {
            throw new InvalidKeyException("RSA private or public key is null");
        }
        throw new InvalidKeyException("Need RSA private or public key");
    }

    @Override
    protected void engineInit(int i, Key key, SecureRandom secureRandom) throws InvalidKeyException {
        try {
            engineInitInternal(i, key, null);
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidKeyException("Algorithm parameters rejected when none supplied", e);
        }
    }

    @Override
    protected void engineInit(int i, Key key, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (algorithmParameterSpec != null) {
            throw new InvalidAlgorithmParameterException("unknown param type: " + algorithmParameterSpec.getClass().getName());
        }
        engineInitInternal(i, key, algorithmParameterSpec);
    }

    @Override
    protected void engineInit(int i, Key key, AlgorithmParameters algorithmParameters, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (algorithmParameters != null) {
            throw new InvalidAlgorithmParameterException("unknown param type: " + algorithmParameters.getClass().getName());
        }
        engineInitInternal(i, key, null);
    }

    @Override
    protected byte[] engineUpdate(byte[] bArr, int i, int i2) {
        if (this.bufferOffset + i2 > this.buffer.length) {
            this.inputTooLarge = true;
            return EmptyArray.BYTE;
        }
        System.arraycopy(bArr, i, this.buffer, this.bufferOffset, i2);
        this.bufferOffset += i2;
        return EmptyArray.BYTE;
    }

    @Override
    protected int engineUpdate(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws ShortBufferException {
        engineUpdate(bArr, i, i2);
        return 0;
    }

    @Override
    protected byte[] engineDoFinal(byte[] bArr, int i, int i2) throws BadPaddingException, IllegalBlockSizeException {
        byte[] bArrCopyOf;
        if (bArr != null) {
            engineUpdate(bArr, i, i2);
        }
        if (this.inputTooLarge) {
            throw new IllegalBlockSizeException("input must be under " + this.buffer.length + " bytes");
        }
        if (this.bufferOffset != this.buffer.length) {
            if (this.padding == 3) {
                bArrCopyOf = new byte[this.buffer.length];
                System.arraycopy(this.buffer, 0, bArrCopyOf, this.buffer.length - this.bufferOffset, this.bufferOffset);
            } else {
                bArrCopyOf = Arrays.copyOf(this.buffer, this.bufferOffset);
            }
        } else {
            bArrCopyOf = this.buffer;
        }
        byte[] bArrCopyOf2 = new byte[this.buffer.length];
        int iDoCryptoOperation = doCryptoOperation(bArrCopyOf, bArrCopyOf2);
        if (!this.encrypting && iDoCryptoOperation != bArrCopyOf2.length) {
            bArrCopyOf2 = Arrays.copyOf(bArrCopyOf2, iDoCryptoOperation);
        }
        this.bufferOffset = 0;
        return bArrCopyOf2;
    }

    @Override
    protected int engineDoFinal(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException {
        byte[] bArrEngineDoFinal = engineDoFinal(bArr, i, i2);
        int length = bArrEngineDoFinal.length + i3;
        if (length > bArr2.length) {
            throw new ShortBufferException("output buffer is too small " + bArr2.length + " < " + length);
        }
        System.arraycopy(bArrEngineDoFinal, 0, bArr2, i3, bArrEngineDoFinal.length);
        return bArrEngineDoFinal.length;
    }

    @Override
    protected byte[] engineWrap(Key key) throws IllegalBlockSizeException, InvalidKeyException {
        try {
            byte[] encoded = key.getEncoded();
            return engineDoFinal(encoded, 0, encoded.length);
        } catch (BadPaddingException e) {
            IllegalBlockSizeException illegalBlockSizeException = new IllegalBlockSizeException();
            illegalBlockSizeException.initCause(e);
            throw illegalBlockSizeException;
        }
    }

    @Override
    protected Key engineUnwrap(byte[] bArr, String str, int i) throws NoSuchAlgorithmException, InvalidKeyException {
        try {
            byte[] bArrEngineDoFinal = engineDoFinal(bArr, 0, bArr.length);
            if (i == 1) {
                return KeyFactory.getInstance(str).generatePublic(new X509EncodedKeySpec(bArrEngineDoFinal));
            }
            if (i == 2) {
                return KeyFactory.getInstance(str).generatePrivate(new PKCS8EncodedKeySpec(bArrEngineDoFinal));
            }
            if (i == 3) {
                return new SecretKeySpec(bArrEngineDoFinal, str);
            }
            throw new UnsupportedOperationException("wrappedKeyType == " + i);
        } catch (InvalidKeySpecException e) {
            throw new InvalidKeyException(e);
        } catch (BadPaddingException e2) {
            throw new InvalidKeyException(e2);
        } catch (IllegalBlockSizeException e3) {
            throw new InvalidKeyException(e3);
        }
    }

    public static abstract class DirectRSA extends OpenSSLCipherRSA {
        public DirectRSA(int i) {
            super(i);
        }

        @Override
        int doCryptoOperation(byte[] bArr, byte[] bArr2) throws BadPaddingException, IllegalBlockSizeException {
            int iRSA_public_decrypt;
            if (this.encrypting) {
                if (this.usingPrivateKey) {
                    return NativeCrypto.RSA_private_encrypt(bArr.length, bArr, bArr2, this.key.getNativeRef(), this.padding);
                }
                return NativeCrypto.RSA_public_encrypt(bArr.length, bArr, bArr2, this.key.getNativeRef(), this.padding);
            }
            try {
                if (this.usingPrivateKey) {
                    iRSA_public_decrypt = NativeCrypto.RSA_private_decrypt(bArr.length, bArr, bArr2, this.key.getNativeRef(), this.padding);
                } else {
                    iRSA_public_decrypt = NativeCrypto.RSA_public_decrypt(bArr.length, bArr, bArr2, this.key.getNativeRef(), this.padding);
                }
                return iRSA_public_decrypt;
            } catch (SignatureException e) {
                IllegalBlockSizeException illegalBlockSizeException = new IllegalBlockSizeException();
                illegalBlockSizeException.initCause(e);
                throw illegalBlockSizeException;
            }
        }
    }

    public static final class PKCS1 extends DirectRSA {
        public PKCS1() {
            super(1);
        }
    }

    public static final class Raw extends DirectRSA {
        public Raw() {
            super(3);
        }
    }

    static class OAEP extends OpenSSLCipherRSA {
        private byte[] label;
        private long mgf1Md;
        private long oaepMd;
        private int oaepMdSizeBytes;
        private NativeRef.EVP_PKEY_CTX pkeyCtx;

        public OAEP(long j, int i) {
            super(4);
            this.mgf1Md = j;
            this.oaepMd = j;
            this.oaepMdSizeBytes = i;
        }

        @Override
        protected AlgorithmParameters engineGetParameters() {
            PSource.PSpecified pSpecified;
            if (!isInitialized()) {
                return null;
            }
            try {
                AlgorithmParameters algorithmParameters = AlgorithmParameters.getInstance("OAEP");
                if (this.label == null) {
                    pSpecified = PSource.PSpecified.DEFAULT;
                } else {
                    pSpecified = new PSource.PSpecified(this.label);
                }
                algorithmParameters.init(new OAEPParameterSpec(EvpMdRef.getJcaDigestAlgorithmStandardNameFromEVP_MD(this.oaepMd), "MGF1", new MGF1ParameterSpec(EvpMdRef.getJcaDigestAlgorithmStandardNameFromEVP_MD(this.mgf1Md)), pSpecified));
                return algorithmParameters;
            } catch (NoSuchAlgorithmException e) {
                throw ((Error) new AssertionError("OAEP not supported").initCause(e));
            } catch (InvalidParameterSpecException e2) {
                throw new RuntimeException("No providers of AlgorithmParameters.OAEP available");
            }
        }

        @Override
        protected void engineSetPadding(String str) throws NoSuchPaddingException {
            if (str.toUpperCase(Locale.US).equals("OAEPPADDING")) {
                this.padding = 4;
                return;
            }
            throw new NoSuchPaddingException("Only OAEP padding is supported");
        }

        @Override
        protected void engineInit(int i, Key key, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
            if (algorithmParameterSpec != null && !(algorithmParameterSpec instanceof OAEPParameterSpec)) {
                throw new InvalidAlgorithmParameterException("Only OAEPParameterSpec accepted in OAEP mode");
            }
            engineInitInternal(i, key, algorithmParameterSpec);
        }

        @Override
        protected void engineInit(int i, Key key, AlgorithmParameters algorithmParameters, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
            OAEPParameterSpec oAEPParameterSpec;
            if (algorithmParameters != null) {
                try {
                    oAEPParameterSpec = (OAEPParameterSpec) algorithmParameters.getParameterSpec(OAEPParameterSpec.class);
                } catch (InvalidParameterSpecException e) {
                    throw new InvalidAlgorithmParameterException("Only OAEP parameters are supported", e);
                }
            } else {
                oAEPParameterSpec = null;
            }
            engineInitInternal(i, key, oAEPParameterSpec);
        }

        @Override
        void engineInitInternal(int i, Key key, AlgorithmParameterSpec algorithmParameterSpec) throws InvalidKeyException, InvalidAlgorithmParameterException {
            if (i == 1 || i == 3) {
                if (!(key instanceof PublicKey)) {
                    throw new InvalidKeyException("Only public keys may be used to encrypt");
                }
            } else if ((i == 2 || i == 4) && !(key instanceof PrivateKey)) {
                throw new InvalidKeyException("Only private keys may be used to decrypt");
            }
            super.engineInitInternal(i, key, algorithmParameterSpec);
        }

        @Override
        void doCryptoInit(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidKeyException, InvalidAlgorithmParameterException {
            long jEVP_PKEY_decrypt_init;
            if (this.encrypting) {
                jEVP_PKEY_decrypt_init = NativeCrypto.EVP_PKEY_encrypt_init(this.key.getNativeRef());
            } else {
                jEVP_PKEY_decrypt_init = NativeCrypto.EVP_PKEY_decrypt_init(this.key.getNativeRef());
            }
            this.pkeyCtx = new NativeRef.EVP_PKEY_CTX(jEVP_PKEY_decrypt_init);
            if (algorithmParameterSpec instanceof OAEPParameterSpec) {
                readOAEPParameters((OAEPParameterSpec) algorithmParameterSpec);
            }
            NativeCrypto.EVP_PKEY_CTX_set_rsa_padding(this.pkeyCtx.context, 4);
            NativeCrypto.EVP_PKEY_CTX_set_rsa_oaep_md(this.pkeyCtx.context, this.oaepMd);
            NativeCrypto.EVP_PKEY_CTX_set_rsa_mgf1_md(this.pkeyCtx.context, this.mgf1Md);
            if (this.label != null && this.label.length > 0) {
                NativeCrypto.EVP_PKEY_CTX_set_rsa_oaep_label(this.pkeyCtx.context, this.label);
            }
        }

        @Override
        int paddedBlockSizeBytes() {
            return keySizeBytes() - ((this.oaepMdSizeBytes * 2) + 2);
        }

        private void readOAEPParameters(OAEPParameterSpec oAEPParameterSpec) throws InvalidAlgorithmParameterException {
            String upperCase = oAEPParameterSpec.getMGFAlgorithm().toUpperCase(Locale.US);
            AlgorithmParameterSpec mGFParameters = oAEPParameterSpec.getMGFParameters();
            if ((!"MGF1".equals(upperCase) && !"1.2.840.113549.1.1.8".equals(upperCase)) || !(mGFParameters instanceof MGF1ParameterSpec)) {
                throw new InvalidAlgorithmParameterException("Only MGF1 supported as mask generation function");
            }
            MGF1ParameterSpec mGF1ParameterSpec = (MGF1ParameterSpec) mGFParameters;
            String upperCase2 = oAEPParameterSpec.getDigestAlgorithm().toUpperCase(Locale.US);
            try {
                this.oaepMd = EvpMdRef.getEVP_MDByJcaDigestAlgorithmStandardName(upperCase2);
                this.oaepMdSizeBytes = EvpMdRef.getDigestSizeBytesByJcaDigestAlgorithmStandardName(upperCase2);
                this.mgf1Md = EvpMdRef.getEVP_MDByJcaDigestAlgorithmStandardName(mGF1ParameterSpec.getDigestAlgorithm());
                PSource pSource = oAEPParameterSpec.getPSource();
                if (!"PSpecified".equals(pSource.getAlgorithm()) || !(pSource instanceof PSource.PSpecified)) {
                    throw new InvalidAlgorithmParameterException("Only PSpecified accepted for PSource");
                }
                this.label = ((PSource.PSpecified) pSource).getValue();
            } catch (NoSuchAlgorithmException e) {
                throw new InvalidAlgorithmParameterException(e);
            }
        }

        @Override
        int doCryptoOperation(byte[] bArr, byte[] bArr2) throws BadPaddingException, IllegalBlockSizeException {
            if (this.encrypting) {
                return NativeCrypto.EVP_PKEY_encrypt(this.pkeyCtx, bArr2, 0, bArr, 0, bArr.length);
            }
            return NativeCrypto.EVP_PKEY_decrypt(this.pkeyCtx, bArr2, 0, bArr, 0, bArr.length);
        }

        public static final class SHA1 extends OAEP {
            public SHA1() {
                super(EvpMdRef.SHA1.EVP_MD, EvpMdRef.SHA1.SIZE_BYTES);
            }
        }

        public static final class SHA224 extends OAEP {
            public SHA224() {
                super(EvpMdRef.SHA224.EVP_MD, EvpMdRef.SHA224.SIZE_BYTES);
            }
        }

        public static final class SHA256 extends OAEP {
            public SHA256() {
                super(EvpMdRef.SHA256.EVP_MD, EvpMdRef.SHA256.SIZE_BYTES);
            }
        }

        public static final class SHA384 extends OAEP {
            public SHA384() {
                super(EvpMdRef.SHA384.EVP_MD, EvpMdRef.SHA384.SIZE_BYTES);
            }
        }

        public static final class SHA512 extends OAEP {
            public SHA512() {
                super(EvpMdRef.SHA512.EVP_MD, EvpMdRef.SHA512.SIZE_BYTES);
            }
        }
    }
}
