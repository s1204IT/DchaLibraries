package com.android.org.conscrypt;

import com.android.org.conscrypt.NativeRef;
import com.android.org.conscrypt.ct.CTConstants;
import java.lang.reflect.InvocationTargetException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Locale;
import javax.crypto.BadPaddingException;
import javax.crypto.CipherSpi;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public abstract class OpenSSLCipher extends CipherSpi {
    private int blockSize;
    byte[] encodedKey;
    private boolean encrypting;
    byte[] iv;
    Mode mode;
    private Padding padding;

    enum Mode {
        NONE,
        CBC,
        CTR,
        ECB,
        GCM,
        POLY1305
    }

    abstract void checkSupportedKeySize(int i) throws InvalidKeyException;

    abstract void checkSupportedMode(Mode mode) throws NoSuchAlgorithmException;

    abstract void checkSupportedPadding(Padding padding) throws NoSuchPaddingException;

    abstract int doFinalInternal(byte[] bArr, int i, int i2) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException;

    abstract void engineInitInternal(byte[] bArr, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException;

    abstract String getBaseCipherName();

    abstract int getCipherBlockSize();

    abstract int getOutputSizeForFinal(int i);

    abstract int getOutputSizeForUpdate(int i);

    abstract int updateInternal(byte[] bArr, int i, int i2, byte[] bArr2, int i3, int i4) throws ShortBufferException;

    enum Padding {
        NOPADDING,
        PKCS5PADDING,
        PKCS7PADDING;

        public static Padding getNormalized(String str) {
            Padding paddingValueOf = valueOf(str);
            if (paddingValueOf == PKCS7PADDING) {
                return PKCS5PADDING;
            }
            return paddingValueOf;
        }
    }

    OpenSSLCipher() {
        this.mode = Mode.ECB;
        this.padding = Padding.PKCS5PADDING;
    }

    OpenSSLCipher(Mode mode, Padding padding) {
        this.mode = Mode.ECB;
        this.padding = Padding.PKCS5PADDING;
        this.mode = mode;
        this.padding = padding;
        this.blockSize = getCipherBlockSize();
    }

    boolean supportsVariableSizeKey() {
        return false;
    }

    boolean supportsVariableSizeIv() {
        return false;
    }

    @Override
    protected void engineSetMode(String str) throws NoSuchAlgorithmException {
        try {
            Mode modeValueOf = Mode.valueOf(str.toUpperCase(Locale.US));
            checkSupportedMode(modeValueOf);
            this.mode = modeValueOf;
        } catch (IllegalArgumentException e) {
            NoSuchAlgorithmException noSuchAlgorithmException = new NoSuchAlgorithmException("No such mode: " + str);
            noSuchAlgorithmException.initCause(e);
            throw noSuchAlgorithmException;
        }
    }

    @Override
    protected void engineSetPadding(String str) throws NoSuchPaddingException {
        try {
            Padding normalized = Padding.getNormalized(str.toUpperCase(Locale.US));
            checkSupportedPadding(normalized);
            this.padding = normalized;
        } catch (IllegalArgumentException e) {
            NoSuchPaddingException noSuchPaddingException = new NoSuchPaddingException("No such padding: " + str);
            noSuchPaddingException.initCause(e);
            throw noSuchPaddingException;
        }
    }

    Padding getPadding() {
        return this.padding;
    }

    @Override
    protected int engineGetBlockSize() {
        return this.blockSize;
    }

    @Override
    protected int engineGetOutputSize(int i) {
        return Math.max(getOutputSizeForUpdate(i), getOutputSizeForFinal(i));
    }

    @Override
    protected byte[] engineGetIV() {
        return this.iv;
    }

    @Override
    protected AlgorithmParameters engineGetParameters() {
        if (this.iv == null || this.iv.length <= 0) {
            return null;
        }
        try {
            AlgorithmParameters algorithmParameters = AlgorithmParameters.getInstance(getBaseCipherName());
            algorithmParameters.init(new IvParameterSpec(this.iv));
            return algorithmParameters;
        } catch (NoSuchAlgorithmException e) {
            return null;
        } catch (InvalidParameterSpecException e2) {
            return null;
        }
    }

    protected AlgorithmParameterSpec getParameterSpec(AlgorithmParameters algorithmParameters) throws InvalidAlgorithmParameterException {
        if (algorithmParameters != null) {
            try {
                return algorithmParameters.getParameterSpec(IvParameterSpec.class);
            } catch (InvalidParameterSpecException e) {
                throw new InvalidAlgorithmParameterException("Params must be convertible to IvParameterSpec", e);
            }
        }
        return null;
    }

    @Override
    protected void engineInit(int i, Key key, SecureRandom secureRandom) throws InvalidKeyException {
        checkAndSetEncodedKey(i, key);
        try {
            engineInitInternal(this.encodedKey, null, secureRandom);
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void engineInit(int i, Key key, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        checkAndSetEncodedKey(i, key);
        engineInitInternal(this.encodedKey, algorithmParameterSpec, secureRandom);
    }

    @Override
    protected void engineInit(int i, Key key, AlgorithmParameters algorithmParameters, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        engineInit(i, key, getParameterSpec(algorithmParameters), secureRandom);
    }

    @Override
    protected byte[] engineUpdate(byte[] bArr, int i, int i2) {
        byte[] bArr2;
        int outputSizeForUpdate = getOutputSizeForUpdate(i2);
        if (outputSizeForUpdate > 0) {
            bArr2 = new byte[outputSizeForUpdate];
        } else {
            bArr2 = EmptyArray.BYTE;
        }
        byte[] bArr3 = bArr2;
        try {
            int iUpdateInternal = updateInternal(bArr, i, i2, bArr3, 0, outputSizeForUpdate);
            if (bArr3.length == iUpdateInternal) {
                return bArr3;
            }
            if (iUpdateInternal == 0) {
                return EmptyArray.BYTE;
            }
            return Arrays.copyOfRange(bArr3, 0, iUpdateInternal);
        } catch (ShortBufferException e) {
            throw new RuntimeException("calculated buffer size was wrong: " + outputSizeForUpdate);
        }
    }

    @Override
    protected int engineUpdate(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws ShortBufferException {
        return updateInternal(bArr, i, i2, bArr2, i3, getOutputSizeForUpdate(i2));
    }

    @Override
    protected byte[] engineDoFinal(byte[] bArr, int i, int i2) throws BadPaddingException, IllegalBlockSizeException {
        int iUpdateInternal;
        int outputSizeForFinal = getOutputSizeForFinal(i2);
        byte[] bArr2 = new byte[outputSizeForFinal];
        if (i2 > 0) {
            try {
                iUpdateInternal = updateInternal(bArr, i, i2, bArr2, 0, outputSizeForFinal);
            } catch (ShortBufferException e) {
                throw new RuntimeException("our calculated buffer was too small", e);
            }
        } else {
            iUpdateInternal = 0;
        }
        try {
            int iDoFinalInternal = iUpdateInternal + doFinalInternal(bArr2, iUpdateInternal, outputSizeForFinal - iUpdateInternal);
            if (iDoFinalInternal == bArr2.length) {
                return bArr2;
            }
            if (iDoFinalInternal == 0) {
                return EmptyArray.BYTE;
            }
            return Arrays.copyOfRange(bArr2, 0, iDoFinalInternal);
        } catch (ShortBufferException e2) {
            throw new RuntimeException("our calculated buffer was too small", e2);
        }
    }

    @Override
    protected int engineDoFinal(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException {
        int iUpdateInternal;
        if (bArr2 == null) {
            throw new NullPointerException("output == null");
        }
        int outputSizeForFinal = getOutputSizeForFinal(i2);
        if (i2 > 0) {
            iUpdateInternal = updateInternal(bArr, i, i2, bArr2, i3, outputSizeForFinal);
            i3 += iUpdateInternal;
            outputSizeForFinal -= iUpdateInternal;
        } else {
            iUpdateInternal = 0;
        }
        return iUpdateInternal + doFinalInternal(bArr2, i3, outputSizeForFinal);
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

    @Override
    protected int engineGetKeySize(Key key) throws InvalidKeyException {
        if (!(key instanceof SecretKey)) {
            throw new InvalidKeyException("Only SecretKey is supported");
        }
        byte[] encoded = key.getEncoded();
        if (encoded == null) {
            throw new InvalidKeyException("key.getEncoded() == null");
        }
        checkSupportedKeySize(encoded.length);
        return encoded.length * 8;
    }

    private byte[] checkAndSetEncodedKey(int i, Key key) throws InvalidKeyException {
        if (i == 1 || i == 3) {
            this.encrypting = true;
        } else if (i == 2 || i == 4) {
            this.encrypting = false;
        } else {
            throw new InvalidParameterException("Unsupported opmode " + i);
        }
        if (!(key instanceof SecretKey)) {
            throw new InvalidKeyException("Only SecretKey is supported");
        }
        byte[] encoded = key.getEncoded();
        if (encoded == null) {
            throw new InvalidKeyException("key.getEncoded() == null");
        }
        checkSupportedKeySize(encoded.length);
        this.encodedKey = encoded;
        return encoded;
    }

    boolean isEncrypting() {
        return this.encrypting;
    }

    public static abstract class EVP_CIPHER extends OpenSSLCipher {
        boolean calledUpdate;
        private final NativeRef.EVP_CIPHER_CTX cipherCtx;
        private int modeBlockSize;

        abstract String getCipherName(int i, Mode mode);

        public EVP_CIPHER(Mode mode, Padding padding) {
            super(mode, padding);
            this.cipherCtx = new NativeRef.EVP_CIPHER_CTX(NativeCrypto.EVP_CIPHER_CTX_new());
        }

        @Override
        void engineInitInternal(byte[] bArr, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
            byte[] iv;
            if (algorithmParameterSpec instanceof IvParameterSpec) {
                iv = ((IvParameterSpec) algorithmParameterSpec).getIV();
            } else {
                iv = null;
            }
            long jEVP_get_cipherbyname = NativeCrypto.EVP_get_cipherbyname(getCipherName(bArr.length, this.mode));
            if (jEVP_get_cipherbyname == 0) {
                throw new InvalidAlgorithmParameterException("Cannot find name for key length = " + (bArr.length * 8) + " and mode = " + this.mode);
            }
            boolean zIsEncrypting = isEncrypting();
            int iEVP_CIPHER_iv_length = NativeCrypto.EVP_CIPHER_iv_length(jEVP_get_cipherbyname);
            if (iv == null && iEVP_CIPHER_iv_length != 0) {
                if (!zIsEncrypting) {
                    throw new InvalidAlgorithmParameterException("IV must be specified in " + this.mode + " mode");
                }
                iv = new byte[iEVP_CIPHER_iv_length];
                if (secureRandom != null) {
                    secureRandom.nextBytes(iv);
                } else {
                    NativeCrypto.RAND_bytes(iv);
                }
            } else {
                if (iEVP_CIPHER_iv_length == 0 && iv != null) {
                    throw new InvalidAlgorithmParameterException("IV not used in " + this.mode + " mode");
                }
                if (iv != null && iv.length != iEVP_CIPHER_iv_length) {
                    throw new InvalidAlgorithmParameterException("expected IV length of " + iEVP_CIPHER_iv_length + " but was " + iv.length);
                }
            }
            this.iv = iv;
            if (supportsVariableSizeKey()) {
                NativeCrypto.EVP_CipherInit_ex(this.cipherCtx, jEVP_get_cipherbyname, null, null, zIsEncrypting);
                NativeCrypto.EVP_CIPHER_CTX_set_key_length(this.cipherCtx, bArr.length);
                NativeCrypto.EVP_CipherInit_ex(this.cipherCtx, 0L, bArr, iv, isEncrypting());
            } else {
                NativeCrypto.EVP_CipherInit_ex(this.cipherCtx, jEVP_get_cipherbyname, bArr, iv, zIsEncrypting);
            }
            NativeCrypto.EVP_CIPHER_CTX_set_padding(this.cipherCtx, getPadding() == Padding.PKCS5PADDING);
            this.modeBlockSize = NativeCrypto.EVP_CIPHER_CTX_block_size(this.cipherCtx);
            this.calledUpdate = false;
        }

        @Override
        int updateInternal(byte[] bArr, int i, int i2, byte[] bArr2, int i3, int i4) throws ShortBufferException {
            int length = bArr2.length - i3;
            if (length < i4) {
                throw new ShortBufferException("output buffer too small during update: " + length + " < " + i4);
            }
            int iEVP_CipherUpdate = NativeCrypto.EVP_CipherUpdate(this.cipherCtx, bArr2, i3, bArr, i, i2) + i3;
            this.calledUpdate = true;
            return iEVP_CipherUpdate - i3;
        }

        @Override
        int doFinalInternal(byte[] bArr, int i, int i2) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException {
            int iEVP_CipherFinal_ex;
            if (!isEncrypting() && !this.calledUpdate) {
                return 0;
            }
            int length = bArr.length - i;
            if (length >= i2) {
                iEVP_CipherFinal_ex = NativeCrypto.EVP_CipherFinal_ex(this.cipherCtx, bArr, i);
            } else {
                byte[] bArr2 = new byte[i2];
                int iEVP_CipherFinal_ex2 = NativeCrypto.EVP_CipherFinal_ex(this.cipherCtx, bArr2, 0);
                if (iEVP_CipherFinal_ex2 > length) {
                    throw new ShortBufferException("buffer is too short: " + iEVP_CipherFinal_ex2 + " > " + length);
                }
                if (iEVP_CipherFinal_ex2 > 0) {
                    System.arraycopy(bArr2, 0, bArr, i, iEVP_CipherFinal_ex2);
                }
                iEVP_CipherFinal_ex = iEVP_CipherFinal_ex2;
            }
            reset();
            return (iEVP_CipherFinal_ex + i) - i;
        }

        @Override
        int getOutputSizeForFinal(int i) {
            if (this.modeBlockSize == 1) {
                return i;
            }
            int i2 = NativeCrypto.get_EVP_CIPHER_CTX_buf_len(this.cipherCtx);
            if (getPadding() == Padding.NOPADDING) {
                return i2 + i;
            }
            int i3 = i + i2 + (NativeCrypto.get_EVP_CIPHER_CTX_final_used(this.cipherCtx) ? this.modeBlockSize : 0);
            int i4 = i3 + ((i3 % this.modeBlockSize != 0 || isEncrypting()) ? this.modeBlockSize : 0);
            return i4 - (i4 % this.modeBlockSize);
        }

        @Override
        int getOutputSizeForUpdate(int i) {
            return getOutputSizeForFinal(i);
        }

        private void reset() {
            NativeCrypto.EVP_CipherInit_ex(this.cipherCtx, 0L, this.encodedKey, this.iv, isEncrypting());
            this.calledUpdate = false;
        }

        static abstract class AES_BASE extends EVP_CIPHER {
            private static final int AES_BLOCK_SIZE = 16;

            AES_BASE(Mode mode, Padding padding) {
                super(mode, padding);
            }

            @Override
            void checkSupportedMode(Mode mode) throws NoSuchAlgorithmException {
                switch (AnonymousClass1.$SwitchMap$org$conscrypt$OpenSSLCipher$Mode[mode.ordinal()]) {
                    case 1:
                    case 2:
                    case CTConstants.CERTIFICATE_LENGTH_BYTES:
                        return;
                    default:
                        throw new NoSuchAlgorithmException("Unsupported mode " + mode.toString());
                }
            }

            @Override
            void checkSupportedPadding(Padding padding) throws NoSuchPaddingException {
                switch (padding) {
                    case NOPADDING:
                    case PKCS5PADDING:
                        return;
                    default:
                        throw new NoSuchPaddingException("Unsupported padding " + padding.toString());
                }
            }

            @Override
            String getBaseCipherName() {
                return "AES";
            }

            @Override
            String getCipherName(int i, Mode mode) {
                return "aes-" + (i * 8) + "-" + mode.toString().toLowerCase(Locale.US);
            }

            @Override
            int getCipherBlockSize() {
                return AES_BLOCK_SIZE;
            }
        }

        public static class AES extends AES_BASE {
            AES(Mode mode, Padding padding) {
                super(mode, padding);
            }

            public static class CBC extends AES {
                public CBC(Padding padding) {
                    super(Mode.CBC, padding);
                }

                public static class NoPadding extends CBC {
                    public NoPadding() {
                        super(Padding.NOPADDING);
                    }
                }

                public static class PKCS5Padding extends CBC {
                    public PKCS5Padding() {
                        super(Padding.PKCS5PADDING);
                    }
                }
            }

            public static class CTR extends AES {
                public CTR() {
                    super(Mode.CTR, Padding.NOPADDING);
                }
            }

            public static class ECB extends AES {
                public ECB(Padding padding) {
                    super(Mode.ECB, padding);
                }

                public static class NoPadding extends ECB {
                    public NoPadding() {
                        super(Padding.NOPADDING);
                    }
                }

                public static class PKCS5Padding extends ECB {
                    public PKCS5Padding() {
                        super(Padding.PKCS5PADDING);
                    }
                }
            }

            @Override
            void checkSupportedKeySize(int i) throws InvalidKeyException {
                if (i == 16 || i == 24 || i == 32) {
                    return;
                }
                throw new InvalidKeyException("Unsupported key size: " + i + " bytes");
            }
        }

        public static class AES_128 extends AES_BASE {
            AES_128(Mode mode, Padding padding) {
                super(mode, padding);
            }

            public static class CBC extends AES_128 {
                public CBC(Padding padding) {
                    super(Mode.CBC, padding);
                }

                public static class NoPadding extends CBC {
                    public NoPadding() {
                        super(Padding.NOPADDING);
                    }
                }

                public static class PKCS5Padding extends CBC {
                    public PKCS5Padding() {
                        super(Padding.PKCS5PADDING);
                    }
                }
            }

            public static class CTR extends AES_128 {
                public CTR() {
                    super(Mode.CTR, Padding.NOPADDING);
                }
            }

            public static class ECB extends AES_128 {
                public ECB(Padding padding) {
                    super(Mode.ECB, padding);
                }

                public static class NoPadding extends ECB {
                    public NoPadding() {
                        super(Padding.NOPADDING);
                    }
                }

                public static class PKCS5Padding extends ECB {
                    public PKCS5Padding() {
                        super(Padding.PKCS5PADDING);
                    }
                }
            }

            @Override
            void checkSupportedKeySize(int i) throws InvalidKeyException {
                if (i != 16) {
                    throw new InvalidKeyException("Unsupported key size: " + i + " bytes");
                }
            }
        }

        public static class AES_256 extends AES_BASE {
            AES_256(Mode mode, Padding padding) {
                super(mode, padding);
            }

            public static class CBC extends AES_256 {
                public CBC(Padding padding) {
                    super(Mode.CBC, padding);
                }

                public static class NoPadding extends CBC {
                    public NoPadding() {
                        super(Padding.NOPADDING);
                    }
                }

                public static class PKCS5Padding extends CBC {
                    public PKCS5Padding() {
                        super(Padding.PKCS5PADDING);
                    }
                }
            }

            public static class CTR extends AES_256 {
                public CTR() {
                    super(Mode.CTR, Padding.NOPADDING);
                }
            }

            public static class ECB extends AES_256 {
                public ECB(Padding padding) {
                    super(Mode.ECB, padding);
                }

                public static class NoPadding extends ECB {
                    public NoPadding() {
                        super(Padding.NOPADDING);
                    }
                }

                public static class PKCS5Padding extends ECB {
                    public PKCS5Padding() {
                        super(Padding.PKCS5PADDING);
                    }
                }
            }

            @Override
            void checkSupportedKeySize(int i) throws InvalidKeyException {
                if (i != 32) {
                    throw new InvalidKeyException("Unsupported key size: " + i + " bytes");
                }
            }
        }

        public static class DESEDE extends EVP_CIPHER {
            private static final int DES_BLOCK_SIZE = 8;

            public DESEDE(Mode mode, Padding padding) {
                super(mode, padding);
            }

            public static class CBC extends DESEDE {
                public CBC(Padding padding) {
                    super(Mode.CBC, padding);
                }

                public static class NoPadding extends CBC {
                    public NoPadding() {
                        super(Padding.NOPADDING);
                    }
                }

                public static class PKCS5Padding extends CBC {
                    public PKCS5Padding() {
                        super(Padding.PKCS5PADDING);
                    }
                }
            }

            @Override
            String getBaseCipherName() {
                return "DESede";
            }

            @Override
            String getCipherName(int i, Mode mode) {
                String str;
                if (i == 16) {
                    str = "des-ede";
                } else {
                    str = "des-ede3";
                }
                return str + "-" + mode.toString().toLowerCase(Locale.US);
            }

            @Override
            void checkSupportedKeySize(int i) throws InvalidKeyException {
                if (i != 16 && i != 24) {
                    throw new InvalidKeyException("key size must be 128 or 192 bits");
                }
            }

            @Override
            void checkSupportedMode(Mode mode) throws NoSuchAlgorithmException {
                if (mode != Mode.CBC) {
                    throw new NoSuchAlgorithmException("Unsupported mode " + mode.toString());
                }
            }

            @Override
            void checkSupportedPadding(Padding padding) throws NoSuchPaddingException {
                switch (padding) {
                    case NOPADDING:
                    case PKCS5PADDING:
                        return;
                    default:
                        throw new NoSuchPaddingException("Unsupported padding " + padding.toString());
                }
            }

            @Override
            int getCipherBlockSize() {
                return 8;
            }
        }

        public static class ARC4 extends EVP_CIPHER {
            public ARC4() {
                super(Mode.ECB, Padding.NOPADDING);
            }

            @Override
            String getBaseCipherName() {
                return "ARCFOUR";
            }

            @Override
            String getCipherName(int i, Mode mode) {
                return "rc4";
            }

            @Override
            void checkSupportedKeySize(int i) throws InvalidKeyException {
            }

            @Override
            void checkSupportedMode(Mode mode) throws NoSuchAlgorithmException {
                if (mode != Mode.NONE && mode != Mode.ECB) {
                    throw new NoSuchAlgorithmException("Unsupported mode " + mode.toString());
                }
            }

            @Override
            void checkSupportedPadding(Padding padding) throws NoSuchPaddingException {
                if (padding != Padding.NOPADDING) {
                    throw new NoSuchPaddingException("Unsupported padding " + padding.toString());
                }
            }

            @Override
            int getCipherBlockSize() {
                return 0;
            }

            @Override
            boolean supportsVariableSizeKey() {
                return true;
            }
        }
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$org$conscrypt$OpenSSLCipher$Mode;

        static {
            try {
                $SwitchMap$org$conscrypt$OpenSSLCipher$Padding[Padding.NOPADDING.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$org$conscrypt$OpenSSLCipher$Padding[Padding.PKCS5PADDING.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            $SwitchMap$org$conscrypt$OpenSSLCipher$Mode = new int[Mode.values().length];
            try {
                $SwitchMap$org$conscrypt$OpenSSLCipher$Mode[Mode.CBC.ordinal()] = 1;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$org$conscrypt$OpenSSLCipher$Mode[Mode.CTR.ordinal()] = 2;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$org$conscrypt$OpenSSLCipher$Mode[Mode.ECB.ordinal()] = 3;
            } catch (NoSuchFieldError e5) {
            }
        }
    }

    public static abstract class EVP_AEAD extends OpenSSLCipher {
        private static final int DEFAULT_TAG_SIZE_BITS = 128;
        private static int lastGlobalMessageSize = 32;
        private byte[] aad;
        byte[] buf;
        int bufCount;
        long evpAead;
        private boolean mustInitialize;
        private byte[] previousIv;
        private byte[] previousKey;
        int tagLengthInBytes;

        abstract long getEVP_AEAD(int i) throws InvalidKeyException;

        public EVP_AEAD(Mode mode) {
            super(mode, Padding.NOPADDING);
        }

        private void checkInitialization() {
            if (this.mustInitialize) {
                throw new IllegalStateException("Cannot re-use same key and IV for multiple encryptions");
            }
        }

        private boolean arraysAreEqual(byte[] bArr, byte[] bArr2) {
            if (bArr.length != bArr2.length) {
                return false;
            }
            int i = 0;
            for (int i2 = 0; i2 < bArr.length; i2++) {
                i |= bArr[i2] ^ bArr2[i2];
            }
            return i == 0;
        }

        private void expand(int i) {
            if (this.bufCount + i <= this.buf.length) {
                return;
            }
            byte[] bArr = new byte[(this.bufCount + i) * 2];
            System.arraycopy(this.buf, 0, bArr, 0, this.bufCount);
            this.buf = bArr;
        }

        private void reset() {
            this.aad = null;
            int i = lastGlobalMessageSize;
            if (this.buf == null) {
                this.buf = new byte[i];
            } else if (this.bufCount > 0 && this.bufCount != i) {
                lastGlobalMessageSize = this.bufCount;
                if (this.buf.length != this.bufCount) {
                    this.buf = new byte[this.bufCount];
                }
            }
            this.bufCount = 0;
        }

        @Override
        void engineInitInternal(byte[] bArr, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
            byte[] iv = null;
            int tLen = 128;
            if (algorithmParameterSpec != null) {
                GCMParameters gCMParametersFromGCMParameterSpec = Platform.fromGCMParameterSpec(algorithmParameterSpec);
                if (gCMParametersFromGCMParameterSpec != null) {
                    iv = gCMParametersFromGCMParameterSpec.getIV();
                    tLen = gCMParametersFromGCMParameterSpec.getTLen();
                } else if (algorithmParameterSpec instanceof IvParameterSpec) {
                    iv = ((IvParameterSpec) algorithmParameterSpec).getIV();
                }
            }
            if (tLen % 8 != 0) {
                throw new InvalidAlgorithmParameterException("Tag length must be a multiple of 8; was " + this.tagLengthInBytes);
            }
            this.tagLengthInBytes = tLen / 8;
            boolean zIsEncrypting = isEncrypting();
            this.evpAead = getEVP_AEAD(bArr.length);
            int iEVP_AEAD_nonce_length = NativeCrypto.EVP_AEAD_nonce_length(this.evpAead);
            if (iv == null && iEVP_AEAD_nonce_length != 0) {
                if (!zIsEncrypting) {
                    throw new InvalidAlgorithmParameterException("IV must be specified in " + this.mode + " mode");
                }
                iv = new byte[iEVP_AEAD_nonce_length];
                if (secureRandom != null) {
                    secureRandom.nextBytes(iv);
                } else {
                    NativeCrypto.RAND_bytes(iv);
                }
            } else {
                if (iEVP_AEAD_nonce_length == 0 && iv != null) {
                    throw new InvalidAlgorithmParameterException("IV not used in " + this.mode + " mode");
                }
                if (iv != null && iv.length != iEVP_AEAD_nonce_length) {
                    throw new InvalidAlgorithmParameterException("Expected IV length of " + iEVP_AEAD_nonce_length + " but was " + iv.length);
                }
            }
            if (isEncrypting() && iv != null) {
                if (this.previousKey != null && this.previousIv != null && arraysAreEqual(this.previousKey, bArr) && arraysAreEqual(this.previousIv, iv)) {
                    this.mustInitialize = true;
                    throw new InvalidAlgorithmParameterException("When using AEAD key and IV must not be re-used");
                }
                this.previousKey = bArr;
                this.previousIv = iv;
            }
            this.mustInitialize = false;
            this.iv = iv;
            reset();
        }

        @Override
        int updateInternal(byte[] bArr, int i, int i2, byte[] bArr2, int i3, int i4) throws ShortBufferException {
            checkInitialization();
            if (this.buf == null) {
                throw new IllegalStateException("Cipher not initialized");
            }
            ArrayUtils.checkOffsetAndCount(bArr.length, i, i2);
            if (i2 > 0) {
                expand(i2);
                System.arraycopy(bArr, i, this.buf, this.bufCount, i2);
                this.bufCount += i2;
                return 0;
            }
            return 0;
        }

        private void throwAEADBadTagExceptionIfAvailable(String str, Throwable th) throws BadPaddingException {
            BadPaddingException badPaddingException;
            try {
                try {
                    try {
                        badPaddingException = (BadPaddingException) Class.forName("javax.crypto.AEADBadTagException").getConstructor(String.class).newInstance(str);
                        try {
                            badPaddingException.initCause(th);
                        } catch (IllegalAccessException e) {
                        } catch (InstantiationException e2) {
                        }
                    } catch (InvocationTargetException e3) {
                        throw ((BadPaddingException) new BadPaddingException().initCause(e3.getTargetException()));
                    }
                } catch (IllegalAccessException e4) {
                    badPaddingException = null;
                } catch (InstantiationException e5) {
                    badPaddingException = null;
                }
                if (badPaddingException != null) {
                    throw badPaddingException;
                }
            } catch (Exception e6) {
            }
        }

        @Override
        int doFinalInternal(byte[] bArr, int i, int i2) throws BadPaddingException, IllegalBlockSizeException {
            int iEVP_AEAD_CTX_open;
            checkInitialization();
            try {
                if (isEncrypting()) {
                    iEVP_AEAD_CTX_open = NativeCrypto.EVP_AEAD_CTX_seal(this.evpAead, this.encodedKey, this.tagLengthInBytes, bArr, i, this.iv, this.buf, 0, this.bufCount, this.aad);
                } else {
                    iEVP_AEAD_CTX_open = NativeCrypto.EVP_AEAD_CTX_open(this.evpAead, this.encodedKey, this.tagLengthInBytes, bArr, i, this.iv, this.buf, 0, this.bufCount, this.aad);
                }
                if (isEncrypting()) {
                    this.mustInitialize = true;
                }
                reset();
                return iEVP_AEAD_CTX_open;
            } catch (BadPaddingException e) {
                throwAEADBadTagExceptionIfAvailable(e.getMessage(), e.getCause());
                throw e;
            }
        }

        @Override
        void checkSupportedPadding(Padding padding) throws NoSuchPaddingException {
            if (padding != Padding.NOPADDING) {
                throw new NoSuchPaddingException("Must be NoPadding for AEAD ciphers");
            }
        }

        @Override
        int getOutputSizeForUpdate(int i) {
            return 0;
        }

        @Override
        int getOutputSizeForFinal(int i) {
            return this.bufCount + i + (isEncrypting() ? NativeCrypto.EVP_AEAD_max_overhead(this.evpAead) : 0);
        }

        @Override
        protected void engineUpdateAAD(byte[] bArr, int i, int i2) {
            checkInitialization();
            if (this.aad == null) {
                this.aad = Arrays.copyOfRange(bArr, i, i2 + i);
                return;
            }
            byte[] bArr2 = new byte[this.aad.length + i2];
            System.arraycopy(this.aad, 0, bArr2, 0, this.aad.length);
            System.arraycopy(bArr, i, bArr2, this.aad.length, i2);
            this.aad = bArr2;
        }

        public static abstract class AES extends EVP_AEAD {
            private static final int AES_BLOCK_SIZE = 16;

            AES(Mode mode) {
                super(mode);
            }

            @Override
            void checkSupportedKeySize(int i) throws InvalidKeyException {
                if (i == AES_BLOCK_SIZE || i == 32) {
                    return;
                }
                throw new InvalidKeyException("Unsupported key size: " + i + " bytes (must be 16 or 32)");
            }

            @Override
            String getBaseCipherName() {
                return "AES";
            }

            @Override
            int getCipherBlockSize() {
                return AES_BLOCK_SIZE;
            }

            public static class GCM extends AES {
                public GCM() {
                    super(Mode.GCM);
                }

                @Override
                void checkSupportedMode(Mode mode) throws NoSuchAlgorithmException {
                    if (mode != Mode.GCM) {
                        throw new NoSuchAlgorithmException("Mode must be GCM");
                    }
                }

                @Override
                protected AlgorithmParameters engineGetParameters() {
                    if (this.iv == null) {
                        return null;
                    }
                    AlgorithmParameterSpec gCMParameterSpec = Platform.toGCMParameterSpec(this.tagLengthInBytes * 8, this.iv);
                    if (gCMParameterSpec == null) {
                        return super.engineGetParameters();
                    }
                    try {
                        AlgorithmParameters algorithmParameters = AlgorithmParameters.getInstance("GCM");
                        algorithmParameters.init(gCMParameterSpec);
                        return algorithmParameters;
                    } catch (NoSuchAlgorithmException e) {
                        throw ((Error) new AssertionError("GCM not supported").initCause(e));
                    } catch (InvalidParameterSpecException e2) {
                        return null;
                    }
                }

                @Override
                protected AlgorithmParameterSpec getParameterSpec(AlgorithmParameters algorithmParameters) throws InvalidAlgorithmParameterException {
                    if (algorithmParameters != null) {
                        AlgorithmParameterSpec algorithmParameterSpecFromGCMParameters = Platform.fromGCMParameters(algorithmParameters);
                        if (algorithmParameterSpecFromGCMParameters != null) {
                            return algorithmParameterSpecFromGCMParameters;
                        }
                        return super.getParameterSpec(algorithmParameters);
                    }
                    return null;
                }

                @Override
                long getEVP_AEAD(int i) throws InvalidKeyException {
                    if (i == AES.AES_BLOCK_SIZE) {
                        return NativeCrypto.EVP_aead_aes_128_gcm();
                    }
                    if (i == 32) {
                        return NativeCrypto.EVP_aead_aes_256_gcm();
                    }
                    throw new RuntimeException("Unexpected key length: " + i);
                }

                public static class AES_128 extends GCM {
                    @Override
                    void checkSupportedKeySize(int i) throws InvalidKeyException {
                        if (i != AES.AES_BLOCK_SIZE) {
                            throw new InvalidKeyException("Unsupported key size: " + i + " bytes (must be 16)");
                        }
                    }
                }

                public static class AES_256 extends GCM {
                    @Override
                    void checkSupportedKeySize(int i) throws InvalidKeyException {
                        if (i != 32) {
                            throw new InvalidKeyException("Unsupported key size: " + i + " bytes (must be 32)");
                        }
                    }
                }
            }
        }

        public static class ChaCha20 extends EVP_AEAD {
            public ChaCha20() {
                super(Mode.POLY1305);
            }

            @Override
            void checkSupportedKeySize(int i) throws InvalidKeyException {
                if (i != 32) {
                    throw new InvalidKeyException("Unsupported key size: " + i + " bytes (must be 32)");
                }
            }

            @Override
            String getBaseCipherName() {
                return "ChaCha20";
            }

            @Override
            int getCipherBlockSize() {
                return 0;
            }

            @Override
            void checkSupportedMode(Mode mode) throws NoSuchAlgorithmException {
                if (mode != Mode.POLY1305) {
                    throw new NoSuchAlgorithmException("Mode must be Poly1305");
                }
            }

            @Override
            long getEVP_AEAD(int i) throws InvalidKeyException {
                if (i == 32) {
                    return NativeCrypto.EVP_aead_chacha20_poly1305();
                }
                throw new RuntimeException("Unexpected key length: " + i);
            }
        }
    }
}
