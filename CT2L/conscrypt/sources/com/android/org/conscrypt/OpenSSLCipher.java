package com.android.org.conscrypt;

import com.android.org.conscrypt.util.EmptyArray;
import java.io.IOException;
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
    private boolean calledUpdate;
    private OpenSSLCipherContext cipherCtx;
    private byte[] encodedKey;
    private boolean encrypting;
    private byte[] iv;
    private Mode mode;
    private int modeBlockSize;
    private Padding padding;

    protected enum Mode {
        CBC,
        CFB,
        CFB1,
        CFB8,
        CFB128,
        CTR,
        CTS,
        ECB,
        OFB,
        OFB64,
        OFB128,
        PCBC
    }

    protected enum Padding {
        NOPADDING,
        PKCS5PADDING,
        ISO10126PADDING
    }

    protected abstract void checkSupportedKeySize(int i) throws InvalidKeyException;

    protected abstract void checkSupportedMode(Mode mode) throws NoSuchAlgorithmException;

    protected abstract void checkSupportedPadding(Padding padding) throws NoSuchPaddingException;

    protected abstract String getBaseCipherName();

    protected abstract int getCipherBlockSize();

    protected abstract String getCipherName(int i, Mode mode);

    protected OpenSSLCipher() {
        this.cipherCtx = new OpenSSLCipherContext(NativeCrypto.EVP_CIPHER_CTX_new());
        this.mode = Mode.ECB;
        this.padding = Padding.PKCS5PADDING;
    }

    protected OpenSSLCipher(Mode mode, Padding padding) {
        this.cipherCtx = new OpenSSLCipherContext(NativeCrypto.EVP_CIPHER_CTX_new());
        this.mode = Mode.ECB;
        this.padding = Padding.PKCS5PADDING;
        this.mode = mode;
        this.padding = padding;
        this.blockSize = getCipherBlockSize();
    }

    protected boolean supportsVariableSizeKey() {
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

    @Override
    protected int engineGetBlockSize() {
        return this.blockSize;
    }

    private int getOutputSize(int inputLen) {
        if (this.modeBlockSize != 1) {
            int buffered = NativeCrypto.get_EVP_CIPHER_CTX_buf_len(this.cipherCtx.getContext());
            if (this.padding == Padding.NOPADDING) {
                return inputLen + buffered;
            }
            int totalLen = inputLen + buffered + this.modeBlockSize;
            return totalLen - (totalLen % this.modeBlockSize);
        }
        return inputLen;
    }

    @Override
    protected int engineGetOutputSize(int inputLen) {
        return getOutputSize(inputLen);
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

    private void engineInitInternal(int opmode, Key key, byte[] iv, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
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
        long cipherType = NativeCrypto.EVP_get_cipherbyname(getCipherName(encodedKey.length, this.mode));
        if (cipherType == 0) {
            throw new InvalidAlgorithmParameterException("Cannot find name for key length = " + (encodedKey.length * 8) + " and mode = " + this.mode);
        }
        int ivLength = NativeCrypto.EVP_CIPHER_iv_length(cipherType);
        if (iv == null && ivLength != 0) {
            iv = new byte[ivLength];
            if (this.encrypting) {
                if (random == null) {
                    random = new SecureRandom();
                }
                random.nextBytes(iv);
            }
        } else if (iv != null && iv.length != ivLength) {
            throw new InvalidAlgorithmParameterException("expected IV length of " + ivLength);
        }
        this.iv = iv;
        if (supportsVariableSizeKey()) {
            NativeCrypto.EVP_CipherInit_ex(this.cipherCtx.getContext(), cipherType, null, null, this.encrypting);
            NativeCrypto.EVP_CIPHER_CTX_set_key_length(this.cipherCtx.getContext(), encodedKey.length);
            NativeCrypto.EVP_CipherInit_ex(this.cipherCtx.getContext(), 0L, encodedKey, iv, this.encrypting);
        } else {
            NativeCrypto.EVP_CipherInit_ex(this.cipherCtx.getContext(), cipherType, encodedKey, iv, this.encrypting);
        }
        NativeCrypto.EVP_CIPHER_CTX_set_padding(this.cipherCtx.getContext(), this.padding == Padding.PKCS5PADDING);
        this.modeBlockSize = NativeCrypto.EVP_CIPHER_CTX_block_size(this.cipherCtx.getContext());
        this.calledUpdate = false;
    }

    @Override
    protected void engineInit(int opmode, Key key, SecureRandom random) throws InvalidKeyException {
        try {
            engineInitInternal(opmode, key, null, random);
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void engineInit(int opmode, Key key, AlgorithmParameterSpec params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        byte[] iv;
        if (params instanceof IvParameterSpec) {
            IvParameterSpec ivParams = (IvParameterSpec) params;
            iv = ivParams.getIV();
        } else {
            iv = null;
        }
        engineInitInternal(opmode, key, iv, random);
    }

    @Override
    protected void engineInit(int opmode, Key key, AlgorithmParameters params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        AlgorithmParameterSpec spec;
        if (params != null) {
            try {
                spec = params.getParameterSpec(IvParameterSpec.class);
            } catch (InvalidParameterSpecException e) {
                throw new InvalidAlgorithmParameterException(e);
            }
        } else {
            spec = null;
        }
        engineInit(opmode, key, spec, random);
    }

    private final int updateInternal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset, int maximumLen) throws ShortBufferException {
        int bytesLeft = output.length - outputOffset;
        if (bytesLeft < maximumLen) {
            throw new ShortBufferException("output buffer too small during update: " + bytesLeft + " < " + maximumLen);
        }
        int outputOffset2 = outputOffset + NativeCrypto.EVP_CipherUpdate(this.cipherCtx.getContext(), output, outputOffset, input, inputOffset, inputLen);
        this.calledUpdate = true;
        return outputOffset2 - outputOffset;
    }

    @Override
    protected byte[] engineUpdate(byte[] input, int inputOffset, int inputLen) {
        byte[] output;
        int maximumLen = getOutputSize(inputLen);
        if (maximumLen > 0) {
            output = new byte[maximumLen];
        } else {
            output = EmptyArray.BYTE;
        }
        try {
            int bytesWritten = updateInternal(input, inputOffset, inputLen, output, 0, maximumLen);
            if (output.length != bytesWritten) {
                if (bytesWritten == 0) {
                    byte[] output2 = EmptyArray.BYTE;
                    return output2;
                }
                return Arrays.copyOfRange(output, 0, bytesWritten);
            }
            return output;
        } catch (ShortBufferException e) {
            throw new RuntimeException("calculated buffer size was wrong: " + maximumLen);
        }
    }

    @Override
    protected int engineUpdate(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws ShortBufferException {
        int maximumLen = getOutputSize(inputLen);
        return updateInternal(input, inputOffset, inputLen, output, outputOffset, maximumLen);
    }

    private void reset() {
        NativeCrypto.EVP_CipherInit_ex(this.cipherCtx.getContext(), 0L, this.encodedKey, this.iv, this.encrypting);
        this.calledUpdate = false;
    }

    private int doFinalInternal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset, int maximumLen) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException {
        int writtenBytes;
        if (inputLen > 0) {
            int updateBytesWritten = updateInternal(input, inputOffset, inputLen, output, outputOffset, maximumLen);
            outputOffset += updateBytesWritten;
            maximumLen -= updateBytesWritten;
        }
        if (!this.encrypting && !this.calledUpdate) {
            return 0;
        }
        int bytesLeft = output.length - outputOffset;
        if (bytesLeft >= maximumLen) {
            writtenBytes = NativeCrypto.EVP_CipherFinal_ex(this.cipherCtx.getContext(), output, outputOffset);
        } else {
            byte[] lastBlock = new byte[maximumLen];
            writtenBytes = NativeCrypto.EVP_CipherFinal_ex(this.cipherCtx.getContext(), lastBlock, 0);
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
    protected byte[] engineDoFinal(byte[] input, int inputOffset, int inputLen) throws BadPaddingException, IllegalBlockSizeException {
        if (!this.encrypting && !this.calledUpdate && inputLen == 0) {
            reset();
            return null;
        }
        int maximumLen = getOutputSize(inputLen);
        byte[] output = new byte[maximumLen];
        try {
            int bytesWritten = doFinalInternal(input, inputOffset, inputLen, output, 0, maximumLen);
            if (bytesWritten != output.length) {
                if (bytesWritten == 0) {
                    return EmptyArray.BYTE;
                }
                return Arrays.copyOfRange(output, 0, bytesWritten);
            }
            return output;
        } catch (ShortBufferException e) {
            throw new RuntimeException("our calculated buffer was too small", e);
        }
    }

    @Override
    protected int engineDoFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException {
        if (output == null) {
            throw new NullPointerException("output == null");
        }
        int maximumLen = getOutputSize(inputLen);
        return doFinalInternal(input, inputOffset, inputLen, output, outputOffset, maximumLen);
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

    public static class AES extends OpenSSLCipher {
        private static final int AES_BLOCK_SIZE = 16;

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

        public static class CFB extends AES {
            public CFB() {
                super(Mode.CFB, Padding.NOPADDING);
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

        public static class OFB extends AES {
            public OFB() {
                super(Mode.OFB, Padding.NOPADDING);
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
            switch (AnonymousClass1.$SwitchMap$org$conscrypt$OpenSSLCipher$Mode[mode.ordinal()]) {
                case 1:
                case 2:
                case 3:
                case 4:
                case NativeCrypto.SSL3_RT_HEADER_LENGTH:
                case NativeCrypto.EVP_PKEY_RSA:
                case 7:
                case 8:
                    return;
                default:
                    throw new NoSuchAlgorithmException("Unsupported mode " + mode.toString());
            }
        }

        @Override
        protected void checkSupportedPadding(Padding padding) throws NoSuchPaddingException {
            switch (padding) {
                case NOPADDING:
                case PKCS5PADDING:
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
                $SwitchMap$org$conscrypt$OpenSSLCipher$Mode[Mode.CFB.ordinal()] = 2;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$org$conscrypt$OpenSSLCipher$Mode[Mode.CFB1.ordinal()] = 3;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$org$conscrypt$OpenSSLCipher$Mode[Mode.CFB8.ordinal()] = 4;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$org$conscrypt$OpenSSLCipher$Mode[Mode.CFB128.ordinal()] = 5;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$org$conscrypt$OpenSSLCipher$Mode[Mode.CTR.ordinal()] = 6;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$org$conscrypt$OpenSSLCipher$Mode[Mode.ECB.ordinal()] = 7;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$org$conscrypt$OpenSSLCipher$Mode[Mode.OFB.ordinal()] = 8;
            } catch (NoSuchFieldError e10) {
            }
        }
    }

    public static class DESEDE extends OpenSSLCipher {
        private static int DES_BLOCK_SIZE = 8;

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

        public static class CFB extends DESEDE {
            public CFB() {
                super(Mode.CFB, Padding.NOPADDING);
            }
        }

        public static class ECB extends DESEDE {
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

        public static class OFB extends DESEDE {
            public OFB() {
                super(Mode.OFB, Padding.NOPADDING);
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
            return mode == Mode.ECB ? baseCipherName : baseCipherName + "-" + mode.toString().toLowerCase(Locale.US);
        }

        @Override
        protected void checkSupportedKeySize(int keySize) throws InvalidKeyException {
            if (keySize != 16 && keySize != 24) {
                throw new InvalidKeyException("key size must be 128 or 192 bits");
            }
        }

        @Override
        protected void checkSupportedMode(Mode mode) throws NoSuchAlgorithmException {
            switch (AnonymousClass1.$SwitchMap$org$conscrypt$OpenSSLCipher$Mode[mode.ordinal()]) {
                case 1:
                case 2:
                case 3:
                case 4:
                case 7:
                case 8:
                    return;
                case NativeCrypto.SSL3_RT_HEADER_LENGTH:
                case NativeCrypto.EVP_PKEY_RSA:
                default:
                    throw new NoSuchAlgorithmException("Unsupported mode " + mode.toString());
            }
        }

        @Override
        protected void checkSupportedPadding(Padding padding) throws NoSuchPaddingException {
            switch (padding) {
                case NOPADDING:
                case PKCS5PADDING:
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

    public static class ARC4 extends OpenSSLCipher {
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
