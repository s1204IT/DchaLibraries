package com.android.org.bouncycastle.crypto.encodings;

import com.android.org.bouncycastle.crypto.AsymmetricBlockCipher;
import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.InvalidCipherTextException;
import com.android.org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import com.android.org.bouncycastle.crypto.params.ParametersWithRandom;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.SecureRandom;

public class PKCS1Encoding implements AsymmetricBlockCipher {
    private static final int HEADER_LENGTH = 10;
    public static final String NOT_STRICT_LENGTH_ENABLED_PROPERTY = "org.bouncycastle.pkcs1.not_strict";
    public static final String STRICT_LENGTH_ENABLED_PROPERTY = "org.bouncycastle.pkcs1.strict";
    private AsymmetricBlockCipher engine;
    private byte[] fallback;
    private boolean forEncryption;
    private boolean forPrivateKey;
    private int pLen;
    private SecureRandom random;
    private boolean useStrictLength;

    public PKCS1Encoding(AsymmetricBlockCipher cipher) {
        this.pLen = -1;
        this.fallback = null;
        this.engine = cipher;
        this.useStrictLength = useStrict();
    }

    public PKCS1Encoding(AsymmetricBlockCipher cipher, int pLen) {
        this.pLen = -1;
        this.fallback = null;
        this.engine = cipher;
        this.useStrictLength = useStrict();
        this.pLen = pLen;
    }

    public PKCS1Encoding(AsymmetricBlockCipher cipher, byte[] fallback) {
        this.pLen = -1;
        this.fallback = null;
        this.engine = cipher;
        this.useStrictLength = useStrict();
        this.fallback = fallback;
        this.pLen = fallback.length;
    }

    private boolean useStrict() {
        String strict = (String) AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                return System.getProperty("com.android.org.bouncycastle.pkcs1.strict");
            }
        });
        String notStrict = (String) AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                return System.getProperty("com.android.org.bouncycastle.pkcs1.not_strict");
            }
        });
        if (notStrict != null) {
            return !notStrict.equals("true");
        }
        if (strict != null) {
            return strict.equals("true");
        }
        return true;
    }

    public AsymmetricBlockCipher getUnderlyingCipher() {
        return this.engine;
    }

    @Override
    public void init(boolean forEncryption, CipherParameters cipherParameters) {
        AsymmetricKeyParameter kParam;
        if (cipherParameters instanceof ParametersWithRandom) {
            this.random = cipherParameters.getRandom();
            kParam = (AsymmetricKeyParameter) cipherParameters.getParameters();
        } else {
            kParam = (AsymmetricKeyParameter) cipherParameters;
            if (!kParam.isPrivate() && forEncryption) {
                this.random = new SecureRandom();
            }
        }
        this.engine.init(forEncryption, cipherParameters);
        this.forPrivateKey = kParam.isPrivate();
        this.forEncryption = forEncryption;
    }

    @Override
    public int getInputBlockSize() {
        int baseBlockSize = this.engine.getInputBlockSize();
        if (this.forEncryption) {
            return baseBlockSize - 10;
        }
        return baseBlockSize;
    }

    @Override
    public int getOutputBlockSize() {
        int baseBlockSize = this.engine.getOutputBlockSize();
        if (this.forEncryption) {
            return baseBlockSize;
        }
        return baseBlockSize - 10;
    }

    @Override
    public byte[] processBlock(byte[] in, int inOff, int inLen) throws InvalidCipherTextException {
        if (this.forEncryption) {
            return encodeBlock(in, inOff, inLen);
        }
        return decodeBlock(in, inOff, inLen);
    }

    private byte[] encodeBlock(byte[] in, int inOff, int inLen) throws InvalidCipherTextException {
        if (inLen > getInputBlockSize()) {
            throw new IllegalArgumentException("input data too large");
        }
        byte[] block = new byte[this.engine.getInputBlockSize()];
        if (this.forPrivateKey) {
            block[0] = 1;
            for (int i = 1; i != (block.length - inLen) - 1; i++) {
                block[i] = -1;
            }
        } else {
            this.random.nextBytes(block);
            block[0] = 2;
            for (int i2 = 1; i2 != (block.length - inLen) - 1; i2++) {
                while (block[i2] == 0) {
                    block[i2] = (byte) this.random.nextInt();
                }
            }
        }
        block[(block.length - inLen) - 1] = 0;
        System.arraycopy(in, inOff, block, block.length - inLen, inLen);
        return this.engine.processBlock(block, 0, block.length);
    }

    private static int checkPkcs1Encoding(byte[] encoded, int pLen) {
        int correct = (encoded[0] ^ 2) | 0;
        int plen = encoded.length - (pLen + 1);
        for (int i = 1; i < plen; i++) {
            int tmp = encoded[i];
            int tmp2 = tmp | (tmp >> 1);
            int tmp3 = tmp2 | (tmp2 >> 2);
            correct |= ((tmp3 | (tmp3 >> 4)) & 1) - 1;
        }
        int correct2 = correct | encoded[encoded.length - (pLen + 1)];
        int correct3 = correct2 | (correct2 >> 1);
        int correct4 = correct3 | (correct3 >> 2);
        return ~(((correct4 | (correct4 >> 4)) & 1) - 1);
    }

    private byte[] decodeBlockOrRandom(byte[] in, int inOff, int inLen) throws InvalidCipherTextException {
        byte[] random;
        if (!this.forPrivateKey) {
            throw new InvalidCipherTextException("sorry, this method is only for decryption, not for signing");
        }
        byte[] block = this.engine.processBlock(in, inOff, inLen);
        if (this.fallback == null) {
            random = new byte[this.pLen];
            this.random.nextBytes(random);
        } else {
            random = this.fallback;
        }
        if (block.length < getOutputBlockSize()) {
            throw new InvalidCipherTextException("block truncated");
        }
        if (this.useStrictLength && block.length != this.engine.getOutputBlockSize()) {
            throw new InvalidCipherTextException("block incorrect size");
        }
        int correct = checkPkcs1Encoding(block, this.pLen);
        byte[] result = new byte[this.pLen];
        for (int i = 0; i < this.pLen; i++) {
            result[i] = (byte) ((block[(block.length - this.pLen) + i] & (~correct)) | (random[i] & correct));
        }
        return result;
    }

    private byte[] decodeBlock(byte[] in, int inOff, int inLen) throws InvalidCipherTextException {
        byte pad;
        if (this.pLen != -1) {
            return decodeBlockOrRandom(in, inOff, inLen);
        }
        byte[] block = this.engine.processBlock(in, inOff, inLen);
        if (block.length < getOutputBlockSize()) {
            throw new InvalidCipherTextException("block truncated");
        }
        byte type = block[0];
        if (this.forPrivateKey) {
            if (type != 2) {
                throw new InvalidCipherTextException("unknown block type");
            }
        } else if (type != 1) {
            throw new InvalidCipherTextException("unknown block type");
        }
        if ((type == 1 && this.forPrivateKey) || (type == 2 && !this.forPrivateKey)) {
            throw new InvalidCipherTextException("invalid block type " + ((int) type));
        }
        if (this.useStrictLength && block.length != this.engine.getOutputBlockSize()) {
            throw new InvalidCipherTextException("block incorrect size");
        }
        int start = 1;
        while (start != block.length && (pad = block[start]) != 0) {
            if (type != 1 || pad == -1) {
                start++;
            } else {
                throw new InvalidCipherTextException("block padding incorrect");
            }
        }
        int start2 = start + 1;
        if (start2 > block.length || start2 < 10) {
            throw new InvalidCipherTextException("no data in block");
        }
        byte[] result = new byte[block.length - start2];
        System.arraycopy(block, start2, result, 0, result.length);
        return result;
    }
}
