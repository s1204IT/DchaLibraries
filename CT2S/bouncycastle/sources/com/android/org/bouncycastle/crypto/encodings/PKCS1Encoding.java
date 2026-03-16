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
    public static final String STRICT_LENGTH_ENABLED_PROPERTY = "com.android.org.bouncycastle.pkcs1.strict";
    private AsymmetricBlockCipher engine;
    private boolean forEncryption;
    private boolean forPrivateKey;
    private SecureRandom random;
    private boolean useStrictLength = useStrict();

    public PKCS1Encoding(AsymmetricBlockCipher cipher) {
        this.engine = cipher;
    }

    private boolean useStrict() {
        String strict = (String) AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                return System.getProperty(PKCS1Encoding.STRICT_LENGTH_ENABLED_PROPERTY);
            }
        });
        return strict == null || strict.equals("true");
    }

    public AsymmetricBlockCipher getUnderlyingCipher() {
        return this.engine;
    }

    @Override
    public void init(boolean forEncryption, CipherParameters param) {
        AsymmetricKeyParameter kParam;
        if (param instanceof ParametersWithRandom) {
            ParametersWithRandom rParam = (ParametersWithRandom) param;
            this.random = rParam.getRandom();
            kParam = (AsymmetricKeyParameter) rParam.getParameters();
        } else {
            this.random = new SecureRandom();
            kParam = (AsymmetricKeyParameter) param;
        }
        this.engine.init(forEncryption, param);
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
        return this.forEncryption ? baseBlockSize : baseBlockSize - 10;
    }

    @Override
    public byte[] processBlock(byte[] in, int inOff, int inLen) throws InvalidCipherTextException {
        return this.forEncryption ? encodeBlock(in, inOff, inLen) : decodeBlock(in, inOff, inLen);
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

    private byte[] decodeBlock(byte[] in, int inOff, int inLen) throws InvalidCipherTextException {
        byte pad;
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
