package com.android.org.bouncycastle.crypto.encodings;

import com.android.org.bouncycastle.crypto.AsymmetricBlockCipher;
import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.InvalidCipherTextException;
import com.android.org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import com.android.org.bouncycastle.crypto.params.ParametersWithRandom;
import com.android.org.bouncycastle.util.Arrays;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.SecureRandom;

public class PKCS1Encoding implements AsymmetricBlockCipher {
    private static final int HEADER_LENGTH = 10;
    public static final String NOT_STRICT_LENGTH_ENABLED_PROPERTY = "com.android.org.bouncycastle.pkcs1.not_strict";
    public static final String STRICT_LENGTH_ENABLED_PROPERTY = "com.android.org.bouncycastle.pkcs1.strict";
    private byte[] blockBuffer;
    private AsymmetricBlockCipher engine;
    private byte[] fallback;
    private boolean forEncryption;
    private boolean forPrivateKey;
    private int pLen;
    private SecureRandom random;
    private boolean useStrictLength;

    public PKCS1Encoding(AsymmetricBlockCipher asymmetricBlockCipher) {
        this.pLen = -1;
        this.fallback = null;
        this.engine = asymmetricBlockCipher;
        this.useStrictLength = useStrict();
    }

    public PKCS1Encoding(AsymmetricBlockCipher asymmetricBlockCipher, int i) {
        this.pLen = -1;
        this.fallback = null;
        this.engine = asymmetricBlockCipher;
        this.useStrictLength = useStrict();
        this.pLen = i;
    }

    public PKCS1Encoding(AsymmetricBlockCipher asymmetricBlockCipher, byte[] bArr) {
        this.pLen = -1;
        this.fallback = null;
        this.engine = asymmetricBlockCipher;
        this.useStrictLength = useStrict();
        this.fallback = bArr;
        this.pLen = bArr.length;
    }

    private boolean useStrict() {
        String str = (String) AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                return System.getProperty(PKCS1Encoding.STRICT_LENGTH_ENABLED_PROPERTY);
            }
        });
        if (((String) AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                return System.getProperty(PKCS1Encoding.NOT_STRICT_LENGTH_ENABLED_PROPERTY);
            }
        })) != null) {
            return !r1.equals("true");
        }
        return str == null || str.equals("true");
    }

    public AsymmetricBlockCipher getUnderlyingCipher() {
        return this.engine;
    }

    @Override
    public void init(boolean z, CipherParameters cipherParameters) {
        AsymmetricKeyParameter asymmetricKeyParameter;
        if (cipherParameters instanceof ParametersWithRandom) {
            ParametersWithRandom parametersWithRandom = (ParametersWithRandom) cipherParameters;
            this.random = parametersWithRandom.getRandom();
            asymmetricKeyParameter = (AsymmetricKeyParameter) parametersWithRandom.getParameters();
        } else {
            asymmetricKeyParameter = (AsymmetricKeyParameter) cipherParameters;
            if (!asymmetricKeyParameter.isPrivate() && z) {
                this.random = new SecureRandom();
            }
        }
        this.engine.init(z, cipherParameters);
        this.forPrivateKey = asymmetricKeyParameter.isPrivate();
        this.forEncryption = z;
        this.blockBuffer = new byte[this.engine.getOutputBlockSize()];
        if (this.pLen > 0 && this.fallback == null && this.random == null) {
            throw new IllegalArgumentException("encoder requires random");
        }
    }

    @Override
    public int getInputBlockSize() {
        int inputBlockSize = this.engine.getInputBlockSize();
        if (this.forEncryption) {
            return inputBlockSize - 10;
        }
        return inputBlockSize;
    }

    @Override
    public int getOutputBlockSize() {
        int outputBlockSize = this.engine.getOutputBlockSize();
        if (this.forEncryption) {
            return outputBlockSize;
        }
        return outputBlockSize - 10;
    }

    @Override
    public byte[] processBlock(byte[] bArr, int i, int i2) throws InvalidCipherTextException {
        if (this.forEncryption) {
            return encodeBlock(bArr, i, i2);
        }
        return decodeBlock(bArr, i, i2);
    }

    private byte[] encodeBlock(byte[] bArr, int i, int i2) throws InvalidCipherTextException {
        if (i2 > getInputBlockSize()) {
            throw new IllegalArgumentException("input data too large");
        }
        byte[] bArr2 = new byte[this.engine.getInputBlockSize()];
        if (this.forPrivateKey) {
            bArr2[0] = 1;
            for (int i3 = 1; i3 != (bArr2.length - i2) - 1; i3++) {
                bArr2[i3] = -1;
            }
        } else {
            this.random.nextBytes(bArr2);
            bArr2[0] = 2;
            for (int i4 = 1; i4 != (bArr2.length - i2) - 1; i4++) {
                while (bArr2[i4] == 0) {
                    bArr2[i4] = (byte) this.random.nextInt();
                }
            }
        }
        bArr2[(bArr2.length - i2) - 1] = 0;
        System.arraycopy(bArr, i, bArr2, bArr2.length - i2, i2);
        return this.engine.processBlock(bArr2, 0, bArr2.length);
    }

    private static int checkPkcs1Encoding(byte[] bArr, int i) {
        int i2 = 0 | (bArr[0] ^ 2);
        int i3 = i + 1;
        int length = bArr.length - i3;
        int i4 = i2;
        for (int i5 = 1; i5 < length; i5++) {
            byte b = bArr[i5];
            int i6 = b | (b >> 1);
            int i7 = i6 | (i6 >> 2);
            i4 |= ((i7 | (i7 >> 4)) & 1) - 1;
        }
        int i8 = bArr[bArr.length - i3] | i4;
        int i9 = i8 | (i8 >> 1);
        int i10 = i9 | (i9 >> 2);
        return ~(((i10 | (i10 >> 4)) & 1) - 1);
    }

    private byte[] decodeBlockOrRandom(byte[] bArr, int i, int i2) throws InvalidCipherTextException {
        byte[] bArr2;
        if (!this.forPrivateKey) {
            throw new InvalidCipherTextException("sorry, this method is only for decryption, not for signing");
        }
        byte[] bArrProcessBlock = this.engine.processBlock(bArr, i, i2);
        if (this.fallback == null) {
            bArr2 = new byte[this.pLen];
            this.random.nextBytes(bArr2);
        } else {
            bArr2 = this.fallback;
        }
        if (this.useStrictLength & (bArrProcessBlock.length != this.engine.getOutputBlockSize())) {
            bArrProcessBlock = this.blockBuffer;
        }
        int iCheckPkcs1Encoding = checkPkcs1Encoding(bArrProcessBlock, this.pLen);
        byte[] bArr3 = new byte[this.pLen];
        for (int i3 = 0; i3 < this.pLen; i3++) {
            bArr3[i3] = (byte) ((bArrProcessBlock[(bArrProcessBlock.length - this.pLen) + i3] & (~iCheckPkcs1Encoding)) | (bArr2[i3] & iCheckPkcs1Encoding));
        }
        Arrays.fill(bArrProcessBlock, (byte) 0);
        return bArr3;
    }

    private byte[] decodeBlock(byte[] bArr, int i, int i2) throws InvalidCipherTextException {
        if (this.pLen != -1) {
            return decodeBlockOrRandom(bArr, i, i2);
        }
        byte[] bArrProcessBlock = this.engine.processBlock(bArr, i, i2);
        boolean z = this.useStrictLength & (bArrProcessBlock.length != this.engine.getOutputBlockSize());
        if (bArrProcessBlock.length < getOutputBlockSize()) {
            bArrProcessBlock = this.blockBuffer;
        }
        byte b = bArrProcessBlock[0];
        boolean z2 = !this.forPrivateKey ? b == 1 : b == 2;
        int iFindStart = findStart(b, bArrProcessBlock) + 1;
        if (z2 | (iFindStart < 10)) {
            Arrays.fill(bArrProcessBlock, (byte) 0);
            throw new InvalidCipherTextException("block incorrect");
        }
        if (z) {
            Arrays.fill(bArrProcessBlock, (byte) 0);
            throw new InvalidCipherTextException("block incorrect size");
        }
        byte[] bArr2 = new byte[bArrProcessBlock.length - iFindStart];
        System.arraycopy(bArrProcessBlock, iFindStart, bArr2, 0, bArr2.length);
        return bArr2;
    }

    private int findStart(byte b, byte[] bArr) throws InvalidCipherTextException {
        int i = -1;
        boolean z = false;
        for (int i2 = 1; i2 != bArr.length; i2++) {
            byte b2 = bArr[i2];
            if ((b2 == 0) & (i < 0)) {
                i = i2;
            }
            z |= (b2 != -1) & (b == 1) & (i < 0);
        }
        if (z) {
            return -1;
        }
        return i;
    }
}
