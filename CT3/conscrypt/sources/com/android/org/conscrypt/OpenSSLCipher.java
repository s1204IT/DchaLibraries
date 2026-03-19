package com.android.org.conscrypt;

import com.android.org.conscrypt.NativeRef;
import com.android.org.conscrypt.util.ArrayUtils;
import com.android.org.conscrypt.util.EmptyArray;
import java.io.IOException;
import java.lang.reflect.Constructor;
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
    protected byte[] encodedKey;
    private boolean encrypting;
    protected byte[] iv;
    protected Mode mode;
    private Padding padding;

    protected abstract void checkSupportedKeySize(int i) throws InvalidKeyException;

    protected abstract void checkSupportedMode(Mode mode) throws NoSuchAlgorithmException;

    protected abstract void checkSupportedPadding(Padding padding) throws NoSuchPaddingException;

    protected abstract int doFinalInternal(byte[] bArr, int i, int i2) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException;

    protected abstract void engineInitInternal(byte[] bArr, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException;

    protected abstract String getBaseCipherName();

    protected abstract int getCipherBlockSize();

    protected abstract int getOutputSizeForFinal(int i);

    protected abstract int getOutputSizeForUpdate(int i);

    protected abstract int updateInternal(byte[] bArr, int i, int i2, byte[] bArr2, int i3, int i4) throws ShortBufferException;

    protected enum Mode {
        CBC,
        CTR,
        ECB,
        GCM;

        public static Mode[] valuesCustom() {
            return values();
        }
    }

    protected enum Padding {
        NOPADDING,
        PKCS5PADDING,
        ISO10126PADDING;

        public static Padding[] valuesCustom() {
            return values();
        }
    }

    protected OpenSSLCipher() {
        this.mode = Mode.ECB;
        this.padding = Padding.PKCS5PADDING;
    }

    protected OpenSSLCipher(Mode mode, Padding padding) {
        this.mode = Mode.ECB;
        this.padding = Padding.PKCS5PADDING;
        this.mode = mode;
        this.padding = padding;
        this.blockSize = getCipherBlockSize();
    }

    protected boolean supportsVariableSizeKey() {
        return false;
    }

    protected boolean supportsVariableSizeIv() {
        return false;
    }

    @Override
    protected void engineSetMode(String modeStr) throws NoSuchAlgorithmException {
        try {
            Mode mode = Mode.valueOf(modeStr.toUpperCase(Locale.US));
            checkSupportedMode(mode);
            this.mode = mode;
        } catch (IllegalArgumentException e) {
            NoSuchAlgorithmException newE = new NoSuchAlgorithmException("No such mode: " + modeStr);
            newE.initCause(e);
            throw newE;
        }
    }

    @Override
    protected void engineSetPadding(String paddingStr) throws NoSuchPaddingException {
        String paddingStrUpper = paddingStr.toUpperCase(Locale.US);
        try {
            Padding padding = Padding.valueOf(paddingStrUpper);
            checkSupportedPadding(padding);
            this.padding = padding;
        } catch (IllegalArgumentException e) {
            NoSuchPaddingException newE = new NoSuchPaddingException("No such padding: " + paddingStr);
            newE.initCause(e);
            throw newE;
        }
    }

    protected Padding getPadding() {
        return this.padding;
    }

    @Override
    protected int engineGetBlockSize() {
        return this.blockSize;
    }

    @Override
    protected int engineGetOutputSize(int inputLen) {
        return getOutputSizeForFinal(inputLen);
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
            AlgorithmParameters params = AlgorithmParameters.getInstance(getBaseCipherName());
            params.init(this.iv);
            return params;
        } catch (IOException e) {
            return null;
        } catch (NoSuchAlgorithmException e2) {
            return null;
        }
    }

    @Override
    protected void engineInit(int opmode, Key key, SecureRandom random) throws InvalidKeyException {
        checkAndSetEncodedKey(opmode, key);
        try {
            engineInitInternal(this.encodedKey, null, random);
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void engineInit(int opmode, Key key, AlgorithmParameterSpec params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        checkAndSetEncodedKey(opmode, key);
        engineInitInternal(this.encodedKey, params, random);
    }

    @Override
    protected void engineInit(int opmode, Key key, AlgorithmParameters params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        AlgorithmParameterSpec parameterSpec;
        if (params != null) {
            try {
                parameterSpec = params.getParameterSpec(IvParameterSpec.class);
            } catch (InvalidParameterSpecException e) {
                throw new InvalidAlgorithmParameterException("Params must be convertible to IvParameterSpec", e);
            }
        } else {
            parameterSpec = null;
        }
        engineInit(opmode, key, parameterSpec, random);
    }

    @Override
    protected byte[] engineUpdate(byte[] input, int inputOffset, int inputLen) {
        byte[] output;
        int maximumLen = getOutputSizeForUpdate(inputLen);
        if (maximumLen > 0) {
            output = new byte[maximumLen];
        } else {
            output = EmptyArray.BYTE;
        }
        try {
            int bytesWritten = updateInternal(input, inputOffset, inputLen, output, 0, maximumLen);
            if (output.length == bytesWritten) {
                return output;
            }
            if (bytesWritten == 0) {
                return EmptyArray.BYTE;
            }
            return Arrays.copyOfRange(output, 0, bytesWritten);
        } catch (ShortBufferException e) {
            throw new RuntimeException("calculated buffer size was wrong: " + maximumLen);
        }
    }

    @Override
    protected int engineUpdate(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws ShortBufferException {
        int maximumLen = getOutputSizeForUpdate(inputLen);
        return updateInternal(input, inputOffset, inputLen, output, outputOffset, maximumLen);
    }

    @Override
    protected byte[] engineDoFinal(byte[] input, int inputOffset, int inputLen) throws BadPaddingException, IllegalBlockSizeException {
        int bytesWritten;
        int maximumLen = getOutputSizeForFinal(inputLen);
        byte[] output = new byte[maximumLen];
        if (inputLen > 0) {
            try {
                bytesWritten = updateInternal(input, inputOffset, inputLen, output, 0, maximumLen);
            } catch (ShortBufferException e) {
                throw new RuntimeException("our calculated buffer was too small", e);
            }
        } else {
            bytesWritten = 0;
        }
        try {
            int bytesWritten2 = bytesWritten + doFinalInternal(output, bytesWritten, maximumLen - bytesWritten);
            if (bytesWritten2 == output.length) {
                return output;
            }
            if (bytesWritten2 == 0) {
                return EmptyArray.BYTE;
            }
            return Arrays.copyOfRange(output, 0, bytesWritten2);
        } catch (ShortBufferException e2) {
            throw new RuntimeException("our calculated buffer was too small", e2);
        }
    }

    @Override
    protected int engineDoFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException {
        int bytesWritten;
        if (output == null) {
            throw new NullPointerException("output == null");
        }
        int maximumLen = getOutputSizeForFinal(inputLen);
        if (inputLen > 0) {
            bytesWritten = updateInternal(input, inputOffset, inputLen, output, outputOffset, maximumLen);
            outputOffset += bytesWritten;
            maximumLen -= bytesWritten;
        } else {
            bytesWritten = 0;
        }
        return doFinalInternal(output, outputOffset, maximumLen) + bytesWritten;
    }

    @Override
    protected byte[] engineWrap(Key key) throws IllegalBlockSizeException, InvalidKeyException {
        try {
            byte[] encoded = key.getEncoded();
            return engineDoFinal(encoded, 0, encoded.length);
        } catch (BadPaddingException e) {
            IllegalBlockSizeException newE = new IllegalBlockSizeException();
            newE.initCause(e);
            throw newE;
        }
    }

    @Override
    protected Key engineUnwrap(byte[] wrappedKey, String wrappedKeyAlgorithm, int wrappedKeyType) throws NoSuchAlgorithmException, InvalidKeyException {
        try {
            byte[] encoded = engineDoFinal(wrappedKey, 0, wrappedKey.length);
            if (wrappedKeyType == 1) {
                KeyFactory keyFactory = KeyFactory.getInstance(wrappedKeyAlgorithm);
                return keyFactory.generatePublic(new X509EncodedKeySpec(encoded));
            }
            if (wrappedKeyType == 2) {
                KeyFactory keyFactory2 = KeyFactory.getInstance(wrappedKeyAlgorithm);
                return keyFactory2.generatePrivate(new PKCS8EncodedKeySpec(encoded));
            }
            if (wrappedKeyType == 3) {
                return new SecretKeySpec(encoded, wrappedKeyAlgorithm);
            }
            throw new UnsupportedOperationException("wrappedKeyType == " + wrappedKeyType);
        } catch (InvalidKeySpecException e) {
            throw new InvalidKeyException(e);
        } catch (BadPaddingException e2) {
            throw new InvalidKeyException(e2);
        } catch (IllegalBlockSizeException e3) {
            throw new InvalidKeyException(e3);
        }
    }

    private byte[] checkAndSetEncodedKey(int opmode, Key key) throws InvalidKeyException {
        if (opmode == 1 || opmode == 3) {
            this.encrypting = true;
        } else if (opmode == 2 || opmode == 4) {
            this.encrypting = false;
        } else {
            throw new InvalidParameterException("Unsupported opmode " + opmode);
        }
        if (!(key instanceof SecretKey)) {
            throw new InvalidKeyException("Only SecretKey is supported");
        }
        byte[] encodedKey = key.getEncoded();
        if (encodedKey == null) {
            throw new InvalidKeyException("key.getEncoded() == null");
        }
        checkSupportedKeySize(encodedKey.length);
        this.encodedKey = encodedKey;
        return encodedKey;
    }

    protected boolean isEncrypting() {
        return this.encrypting;
    }

    public static abstract class EVP_CIPHER extends OpenSSLCipher {
        protected boolean calledUpdate;
        private final NativeRef.EVP_CIPHER_CTX cipherCtx;
        private int modeBlockSize;

        protected abstract String getCipherName(int i, Mode mode);

        public EVP_CIPHER(Mode mode, Padding padding) {
            super(mode, padding);
            this.cipherCtx = new NativeRef.EVP_CIPHER_CTX(NativeCrypto.EVP_CIPHER_CTX_new());
        }

        @Override
        protected void engineInitInternal(byte[] encodedKey, AlgorithmParameterSpec params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
            byte[] iv;
            if (params instanceof IvParameterSpec) {
                IvParameterSpec ivParams = (IvParameterSpec) params;
                iv = ivParams.getIV();
            } else {
                iv = null;
            }
            long cipherType = NativeCrypto.EVP_get_cipherbyname(getCipherName(encodedKey.length, this.mode));
            if (cipherType == 0) {
                throw new InvalidAlgorithmParameterException("Cannot find name for key length = " + (encodedKey.length * 8) + " and mode = " + this.mode);
            }
            boolean encrypting = isEncrypting();
            int expectedIvLength = NativeCrypto.EVP_CIPHER_iv_length(cipherType);
            if (iv != null || expectedIvLength == 0) {
                if (expectedIvLength == 0 && iv != null) {
                    throw new InvalidAlgorithmParameterException("IV not used in " + this.mode + " mode");
                }
                if (iv != null && iv.length != expectedIvLength) {
                    throw new InvalidAlgorithmParameterException("expected IV length of " + expectedIvLength + " but was " + iv.length);
                }
            } else {
                if (!encrypting) {
                    throw new InvalidAlgorithmParameterException("IV must be specified in " + this.mode + " mode");
                }
                iv = new byte[expectedIvLength];
                if (random == null) {
                    random = new SecureRandom();
                }
                random.nextBytes(iv);
            }
            this.iv = iv;
            if (supportsVariableSizeKey()) {
                NativeCrypto.EVP_CipherInit_ex(this.cipherCtx, cipherType, null, null, encrypting);
                NativeCrypto.EVP_CIPHER_CTX_set_key_length(this.cipherCtx, encodedKey.length);
                NativeCrypto.EVP_CipherInit_ex(this.cipherCtx, 0L, encodedKey, iv, isEncrypting());
            } else {
                NativeCrypto.EVP_CipherInit_ex(this.cipherCtx, cipherType, encodedKey, iv, encrypting);
            }
            NativeCrypto.EVP_CIPHER_CTX_set_padding(this.cipherCtx, getPadding() == Padding.PKCS5PADDING);
            this.modeBlockSize = NativeCrypto.EVP_CIPHER_CTX_block_size(this.cipherCtx);
            this.calledUpdate = false;
        }

        @Override
        protected int updateInternal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset, int maximumLen) throws ShortBufferException {
            int bytesLeft = output.length - outputOffset;
            if (bytesLeft < maximumLen) {
                throw new ShortBufferException("output buffer too small during update: " + bytesLeft + " < " + maximumLen);
            }
            int outputOffset2 = outputOffset + NativeCrypto.EVP_CipherUpdate(this.cipherCtx, output, outputOffset, input, inputOffset, inputLen);
            this.calledUpdate = true;
            return outputOffset2 - outputOffset;
        }

        @Override
        protected int doFinalInternal(byte[] output, int outputOffset, int maximumLen) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException {
            int writtenBytes;
            if (!isEncrypting() && !this.calledUpdate) {
                return 0;
            }
            int bytesLeft = output.length - outputOffset;
            if (bytesLeft >= maximumLen) {
                writtenBytes = NativeCrypto.EVP_CipherFinal_ex(this.cipherCtx, output, outputOffset);
            } else {
                byte[] lastBlock = new byte[maximumLen];
                writtenBytes = NativeCrypto.EVP_CipherFinal_ex(this.cipherCtx, lastBlock, 0);
                if (writtenBytes > bytesLeft) {
                    throw new ShortBufferException("buffer is too short: " + writtenBytes + " > " + bytesLeft);
                }
                if (writtenBytes > 0) {
                    System.arraycopy(lastBlock, 0, output, outputOffset, writtenBytes);
                }
            }
            reset();
            return (outputOffset + writtenBytes) - outputOffset;
        }

        @Override
        protected int getOutputSizeForFinal(int inputLen) {
            if (this.modeBlockSize == 1) {
                return inputLen;
            }
            int buffered = NativeCrypto.get_EVP_CIPHER_CTX_buf_len(this.cipherCtx);
            if (getPadding() == Padding.NOPADDING) {
                return buffered + inputLen;
            }
            boolean finalUsed = NativeCrypto.get_EVP_CIPHER_CTX_final_used(this.cipherCtx);
            int totalLen = inputLen + buffered + (finalUsed ? this.modeBlockSize : 0);
            int totalLen2 = totalLen + ((totalLen % this.modeBlockSize != 0 || isEncrypting()) ? this.modeBlockSize : 0);
            return totalLen2 - (totalLen2 % this.modeBlockSize);
        }

        @Override
        protected int getOutputSizeForUpdate(int inputLen) {
            return getOutputSizeForFinal(inputLen);
        }

        private void reset() {
            NativeCrypto.EVP_CipherInit_ex(this.cipherCtx, 0L, this.encodedKey, this.iv, isEncrypting());
            this.calledUpdate = false;
        }

        public static class AES extends EVP_CIPHER {

            private static final int[] f0comandroidorgconscryptOpenSSLCipher$ModeSwitchesValues = null;

            private static final int[] f1comandroidorgconscryptOpenSSLCipher$PaddingSwitchesValues = null;
            private static final int AES_BLOCK_SIZE = 16;

            private static int[] m0getcomandroidorgconscryptOpenSSLCipher$ModeSwitchesValues() {
                if (f0comandroidorgconscryptOpenSSLCipher$ModeSwitchesValues != null) {
                    return f0comandroidorgconscryptOpenSSLCipher$ModeSwitchesValues;
                }
                int[] iArr = new int[Mode.valuesCustom().length];
                try {
                    iArr[Mode.CBC.ordinal()] = 1;
                } catch (NoSuchFieldError e) {
                }
                try {
                    iArr[Mode.CTR.ordinal()] = 2;
                } catch (NoSuchFieldError e2) {
                }
                try {
                    iArr[Mode.ECB.ordinal()] = 3;
                } catch (NoSuchFieldError e3) {
                }
                try {
                    iArr[Mode.GCM.ordinal()] = 6;
                } catch (NoSuchFieldError e4) {
                }
                f0comandroidorgconscryptOpenSSLCipher$ModeSwitchesValues = iArr;
                return iArr;
            }

            private static int[] m1x8f9012d2() {
                if (f1comandroidorgconscryptOpenSSLCipher$PaddingSwitchesValues != null) {
                    return f1comandroidorgconscryptOpenSSLCipher$PaddingSwitchesValues;
                }
                int[] iArr = new int[Padding.valuesCustom().length];
                try {
                    iArr[Padding.ISO10126PADDING.ordinal()] = 6;
                } catch (NoSuchFieldError e) {
                }
                try {
                    iArr[Padding.NOPADDING.ordinal()] = 1;
                } catch (NoSuchFieldError e2) {
                }
                try {
                    iArr[Padding.PKCS5PADDING.ordinal()] = 2;
                } catch (NoSuchFieldError e3) {
                }
                f1comandroidorgconscryptOpenSSLCipher$PaddingSwitchesValues = iArr;
                return iArr;
            }

            protected AES(Mode mode, Padding padding) {
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
            protected void checkSupportedKeySize(int keyLength) throws InvalidKeyException {
                switch (keyLength) {
                    case 16:
                    case 24:
                    case 32:
                        return;
                    default:
                        throw new InvalidKeyException("Unsupported key size: " + keyLength + " bytes");
                }
            }

            @Override
            protected void checkSupportedMode(Mode mode) throws NoSuchAlgorithmException {
                switch (m0getcomandroidorgconscryptOpenSSLCipher$ModeSwitchesValues()[mode.ordinal()]) {
                    case 1:
                    case 2:
                    case 3:
                        return;
                    default:
                        throw new NoSuchAlgorithmException("Unsupported mode " + mode.toString());
                }
            }

            @Override
            protected void checkSupportedPadding(Padding padding) throws NoSuchPaddingException {
                switch (m1x8f9012d2()[padding.ordinal()]) {
                    case 1:
                    case 2:
                        return;
                    default:
                        throw new NoSuchPaddingException("Unsupported padding " + padding.toString());
                }
            }

            @Override
            protected String getBaseCipherName() {
                return "AES";
            }

            @Override
            protected String getCipherName(int keyLength, Mode mode) {
                return "aes-" + (keyLength * 8) + "-" + mode.toString().toLowerCase(Locale.US);
            }

            @Override
            protected int getCipherBlockSize() {
                return 16;
            }
        }

        public static class DESEDE extends EVP_CIPHER {

            private static final int[] f2comandroidorgconscryptOpenSSLCipher$PaddingSwitchesValues = null;
            private static int DES_BLOCK_SIZE = 8;

            private static int[] m2x8f9012d2() {
                if (f2comandroidorgconscryptOpenSSLCipher$PaddingSwitchesValues != null) {
                    return f2comandroidorgconscryptOpenSSLCipher$PaddingSwitchesValues;
                }
                int[] iArr = new int[Padding.valuesCustom().length];
                try {
                    iArr[Padding.ISO10126PADDING.ordinal()] = 3;
                } catch (NoSuchFieldError e) {
                }
                try {
                    iArr[Padding.NOPADDING.ordinal()] = 1;
                } catch (NoSuchFieldError e2) {
                }
                try {
                    iArr[Padding.PKCS5PADDING.ordinal()] = 2;
                } catch (NoSuchFieldError e3) {
                }
                f2comandroidorgconscryptOpenSSLCipher$PaddingSwitchesValues = iArr;
                return iArr;
            }

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
            protected String getBaseCipherName() {
                return "DESede";
            }

            @Override
            protected String getCipherName(int keySize, Mode mode) {
                String baseCipherName;
                if (keySize == 16) {
                    baseCipherName = "des-ede";
                } else {
                    baseCipherName = "des-ede3";
                }
                return baseCipherName + "-" + mode.toString().toLowerCase(Locale.US);
            }

            @Override
            protected void checkSupportedKeySize(int keySize) throws InvalidKeyException {
                if (keySize == 16 || keySize == 24) {
                } else {
                    throw new InvalidKeyException("key size must be 128 or 192 bits");
                }
            }

            @Override
            protected void checkSupportedMode(Mode mode) throws NoSuchAlgorithmException {
                if (mode == Mode.CBC) {
                } else {
                    throw new NoSuchAlgorithmException("Unsupported mode " + mode.toString());
                }
            }

            @Override
            protected void checkSupportedPadding(Padding padding) throws NoSuchPaddingException {
                switch (m2x8f9012d2()[padding.ordinal()]) {
                    case 1:
                    case 2:
                        return;
                    default:
                        throw new NoSuchPaddingException("Unsupported padding " + padding.toString());
                }
            }

            @Override
            protected int getCipherBlockSize() {
                return DES_BLOCK_SIZE;
            }
        }

        public static class ARC4 extends EVP_CIPHER {
            public ARC4() {
                super(Mode.ECB, Padding.NOPADDING);
            }

            @Override
            protected String getBaseCipherName() {
                return "ARCFOUR";
            }

            @Override
            protected String getCipherName(int keySize, Mode mode) {
                return "rc4";
            }

            @Override
            protected void checkSupportedKeySize(int keySize) throws InvalidKeyException {
            }

            @Override
            protected void checkSupportedMode(Mode mode) throws NoSuchAlgorithmException {
                throw new NoSuchAlgorithmException("ARC4 does not support modes");
            }

            @Override
            protected void checkSupportedPadding(Padding padding) throws NoSuchPaddingException {
                throw new NoSuchPaddingException("ARC4 does not support padding");
            }

            @Override
            protected int getCipherBlockSize() {
                return 0;
            }

            @Override
            protected boolean supportsVariableSizeKey() {
                return true;
            }
        }
    }

    public static abstract class EVP_AEAD extends OpenSSLCipher {
        private static final int DEFAULT_TAG_SIZE_BITS = 128;
        private static int lastGlobalMessageSize = 32;
        private byte[] aad;
        protected byte[] buf;
        protected int bufCount;
        protected long evpAead;
        private int tagLengthInBytes;

        protected abstract long getEVP_AEAD(int i) throws InvalidKeyException;

        public EVP_AEAD(Mode mode) {
            super(mode, Padding.NOPADDING);
        }

        private void expand(int i) {
            if (this.bufCount + i <= this.buf.length) {
                return;
            }
            byte[] newbuf = new byte[(this.bufCount + i) * 2];
            System.arraycopy(this.buf, 0, newbuf, 0, this.bufCount);
            this.buf = newbuf;
        }

        private void reset() {
            this.aad = null;
            int lastBufSize = lastGlobalMessageSize;
            if (this.buf == null) {
                this.buf = new byte[lastBufSize];
            } else if (this.bufCount > 0 && this.bufCount != lastBufSize) {
                lastGlobalMessageSize = this.bufCount;
                if (this.buf.length != this.bufCount) {
                    this.buf = new byte[this.bufCount];
                }
            }
            this.bufCount = 0;
        }

        @Override
        protected void engineInitInternal(byte[] encodedKey, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
            byte[] iv;
            int tagLenBits;
            if (algorithmParameterSpec == 0) {
                iv = null;
                tagLenBits = 128;
            } else {
                GCMParameters gcmParams = Platform.fromGCMParameterSpec(algorithmParameterSpec);
                if (gcmParams != null) {
                    iv = gcmParams.getIV();
                    tagLenBits = gcmParams.getTLen();
                } else if (algorithmParameterSpec instanceof IvParameterSpec) {
                    iv = algorithmParameterSpec.getIV();
                    tagLenBits = 128;
                } else {
                    iv = null;
                    tagLenBits = 128;
                }
            }
            if (tagLenBits % 8 != 0) {
                throw new InvalidAlgorithmParameterException("Tag length must be a multiple of 8; was " + this.tagLengthInBytes);
            }
            this.tagLengthInBytes = tagLenBits / 8;
            boolean encrypting = isEncrypting();
            this.evpAead = getEVP_AEAD(encodedKey.length);
            int expectedIvLength = NativeCrypto.EVP_AEAD_nonce_length(this.evpAead);
            if (iv == null && expectedIvLength != 0) {
                if (!encrypting) {
                    throw new InvalidAlgorithmParameterException("IV must be specified in " + this.mode + " mode");
                }
                iv = new byte[expectedIvLength];
                if (random == null) {
                    random = new SecureRandom();
                }
                random.nextBytes(iv);
            } else {
                if (expectedIvLength == 0 && iv != null) {
                    throw new InvalidAlgorithmParameterException("IV not used in " + this.mode + " mode");
                }
                if (iv != null && iv.length != expectedIvLength) {
                    throw new InvalidAlgorithmParameterException("Expected IV length of " + expectedIvLength + " but was " + iv.length);
                }
            }
            this.iv = iv;
            reset();
        }

        @Override
        protected int updateInternal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset, int maximumLen) throws ShortBufferException {
            if (this.buf == null) {
                throw new IllegalStateException("Cipher not initialized");
            }
            ArrayUtils.checkOffsetAndCount(input.length, inputOffset, inputLen);
            if (inputLen > 0) {
                expand(inputLen);
                System.arraycopy(input, inputOffset, this.buf, this.bufCount, inputLen);
                this.bufCount += inputLen;
            }
            return 0;
        }

        @Override
        protected int doFinalInternal(byte[] output, int outputOffset, int maximumLen) throws BadPaddingException, IllegalBlockSizeException {
            int bytesWritten;
            NativeRef.EVP_AEAD_CTX cipherCtx = new NativeRef.EVP_AEAD_CTX(NativeCrypto.EVP_AEAD_CTX_init(this.evpAead, this.encodedKey, this.tagLengthInBytes));
            try {
                if (isEncrypting()) {
                    bytesWritten = NativeCrypto.EVP_AEAD_CTX_seal(cipherCtx, output, outputOffset, this.iv, this.buf, 0, this.bufCount, this.aad);
                } else {
                    bytesWritten = NativeCrypto.EVP_AEAD_CTX_open(cipherCtx, output, outputOffset, this.iv, this.buf, 0, this.bufCount, this.aad);
                }
                reset();
                return bytesWritten;
            } catch (BadPaddingException e) {
                Constructor<?> aeadBadTagConstructor = null;
                try {
                    aeadBadTagConstructor = Class.forName("javax.crypto.AEADBadTagException").getConstructor(String.class);
                } catch (ClassNotFoundException | NoSuchMethodException e2) {
                }
                if (aeadBadTagConstructor != null) {
                    BadPaddingException badTagException = null;
                    try {
                        badTagException = (BadPaddingException) aeadBadTagConstructor.newInstance(e.getMessage());
                        badTagException.initCause(e.getCause());
                    } catch (IllegalAccessException | InstantiationException e3) {
                    } catch (InvocationTargetException e22) {
                        throw ((BadPaddingException) new BadPaddingException().initCause(e22.getTargetException()));
                    }
                    if (badTagException != null) {
                        throw badTagException;
                    }
                    throw e;
                }
                throw e;
            }
        }

        @Override
        protected void checkSupportedPadding(Padding padding) throws NoSuchPaddingException {
            if (padding == Padding.NOPADDING) {
            } else {
                throw new NoSuchPaddingException("Must be NoPadding for AEAD ciphers");
            }
        }

        @Override
        protected int getOutputSizeForFinal(int inputLen) {
            return (isEncrypting() ? NativeCrypto.EVP_AEAD_max_overhead(this.evpAead) : 0) + this.bufCount + inputLen;
        }

        @Override
        protected void engineUpdateAAD(byte[] input, int inputOffset, int inputLen) {
            if (this.aad == null) {
                this.aad = Arrays.copyOfRange(input, inputOffset, inputOffset + inputLen);
                return;
            }
            int newSize = this.aad.length + inputLen;
            byte[] newaad = new byte[newSize];
            System.arraycopy(this.aad, 0, newaad, 0, this.aad.length);
            System.arraycopy(input, inputOffset, newaad, this.aad.length, inputLen);
            this.aad = newaad;
        }

        @Override
        protected AlgorithmParameters engineGetParameters() {
            if (this.iv == null) {
                return null;
            }
            AlgorithmParameterSpec spec = Platform.toGCMParameterSpec(this.tagLengthInBytes * 8, this.iv);
            if (spec == null) {
                return super.engineGetParameters();
            }
            try {
                AlgorithmParameters params = AlgorithmParameters.getInstance("GCM");
                params.init(spec);
                return params;
            } catch (NoSuchAlgorithmException | InvalidParameterSpecException e) {
                return null;
            }
        }

        public static abstract class AES extends EVP_AEAD {
            private static final int AES_BLOCK_SIZE = 16;

            protected AES(Mode mode) {
                super(mode);
            }

            @Override
            protected void checkSupportedKeySize(int keyLength) throws InvalidKeyException {
                switch (keyLength) {
                    case 16:
                    case 32:
                        return;
                    default:
                        throw new InvalidKeyException("Unsupported key size: " + keyLength + " bytes (must be 16 or 32)");
                }
            }

            @Override
            protected String getBaseCipherName() {
                return "AES";
            }

            @Override
            protected int getCipherBlockSize() {
                return 16;
            }

            @Override
            protected int getOutputSizeForUpdate(int inputLen) {
                return 0;
            }

            public static class GCM extends AES {
                public GCM() {
                    super(Mode.GCM);
                }

                @Override
                protected void checkSupportedMode(Mode mode) throws NoSuchAlgorithmException {
                    if (mode == Mode.GCM) {
                    } else {
                        throw new NoSuchAlgorithmException("Mode must be GCM");
                    }
                }

                @Override
                protected long getEVP_AEAD(int keyLength) throws InvalidKeyException {
                    if (keyLength == 16) {
                        return NativeCrypto.EVP_aead_aes_128_gcm();
                    }
                    if (keyLength == 32) {
                        return NativeCrypto.EVP_aead_aes_256_gcm();
                    }
                    throw new RuntimeException("Unexpected key length: " + keyLength);
                }
            }
        }
    }
}
