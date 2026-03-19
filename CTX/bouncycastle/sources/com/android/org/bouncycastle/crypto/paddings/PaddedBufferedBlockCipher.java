package com.android.org.bouncycastle.crypto.paddings;

import com.android.org.bouncycastle.crypto.BlockCipher;
import com.android.org.bouncycastle.crypto.BufferedBlockCipher;
import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.DataLengthException;
import com.android.org.bouncycastle.crypto.InvalidCipherTextException;
import com.android.org.bouncycastle.crypto.OutputLengthException;
import com.android.org.bouncycastle.crypto.params.ParametersWithRandom;

public class PaddedBufferedBlockCipher extends BufferedBlockCipher {
    BlockCipherPadding padding;

    public PaddedBufferedBlockCipher(BlockCipher blockCipher, BlockCipherPadding blockCipherPadding) {
        this.cipher = blockCipher;
        this.padding = blockCipherPadding;
        this.buf = new byte[blockCipher.getBlockSize()];
        this.bufOff = 0;
    }

    public PaddedBufferedBlockCipher(BlockCipher blockCipher) {
        this(blockCipher, new PKCS7Padding());
    }

    @Override
    public void init(boolean z, CipherParameters cipherParameters) throws IllegalArgumentException {
        this.forEncryption = z;
        reset();
        if (cipherParameters instanceof ParametersWithRandom) {
            ParametersWithRandom parametersWithRandom = (ParametersWithRandom) cipherParameters;
            this.padding.init(parametersWithRandom.getRandom());
            this.cipher.init(z, parametersWithRandom.getParameters());
        } else {
            this.padding.init(null);
            this.cipher.init(z, cipherParameters);
        }
    }

    @Override
    public int getOutputSize(int i) {
        int i2 = i + this.bufOff;
        int length = i2 % this.buf.length;
        if (length == 0) {
            if (this.forEncryption) {
                return i2 + this.buf.length;
            }
            return i2;
        }
        return (i2 - length) + this.buf.length;
    }

    @Override
    public int getUpdateOutputSize(int i) {
        int i2 = i + this.bufOff;
        int length = i2 % this.buf.length;
        if (length == 0) {
            return Math.max(0, i2 - this.buf.length);
        }
        return i2 - length;
    }

    @Override
    public int processByte(byte b, byte[] bArr, int i) throws IllegalStateException, DataLengthException {
        int iProcessBlock;
        if (this.bufOff == this.buf.length) {
            iProcessBlock = this.cipher.processBlock(this.buf, 0, bArr, i);
            this.bufOff = 0;
        } else {
            iProcessBlock = 0;
        }
        byte[] bArr2 = this.buf;
        int i2 = this.bufOff;
        this.bufOff = i2 + 1;
        bArr2[i2] = b;
        return iProcessBlock;
    }

    @Override
    public int processBytes(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws IllegalStateException, DataLengthException {
        if (i2 < 0) {
            throw new IllegalArgumentException("Can't have a negative input length!");
        }
        int blockSize = getBlockSize();
        int updateOutputSize = getUpdateOutputSize(i2);
        if (updateOutputSize > 0 && updateOutputSize + i3 > bArr2.length) {
            throw new OutputLengthException("output buffer too short");
        }
        int length = this.buf.length - this.bufOff;
        int iProcessBlock = 0;
        if (i2 > length) {
            System.arraycopy(bArr, i, this.buf, this.bufOff, length);
            int iProcessBlock2 = this.cipher.processBlock(this.buf, 0, bArr2, i3) + 0;
            this.bufOff = 0;
            i2 -= length;
            i += length;
            iProcessBlock = iProcessBlock2;
            while (i2 > this.buf.length) {
                iProcessBlock += this.cipher.processBlock(bArr, i, bArr2, i3 + iProcessBlock);
                i2 -= blockSize;
                i += blockSize;
            }
        }
        System.arraycopy(bArr, i, this.buf, this.bufOff, i2);
        this.bufOff += i2;
        return iProcessBlock;
    }

    @Override
    public int doFinal(byte[] bArr, int i) throws IllegalStateException, DataLengthException, InvalidCipherTextException {
        int iProcessBlock;
        int blockSize = this.cipher.getBlockSize();
        if (this.forEncryption) {
            if (this.bufOff == blockSize) {
                if ((2 * blockSize) + i > bArr.length) {
                    reset();
                    throw new OutputLengthException("output buffer too short");
                }
                iProcessBlock = this.cipher.processBlock(this.buf, 0, bArr, i);
                this.bufOff = 0;
            } else {
                iProcessBlock = 0;
            }
            this.padding.addPadding(this.buf, this.bufOff);
            return iProcessBlock + this.cipher.processBlock(this.buf, 0, bArr, i + iProcessBlock);
        }
        if (this.bufOff == blockSize) {
            int iProcessBlock2 = this.cipher.processBlock(this.buf, 0, this.buf, 0);
            this.bufOff = 0;
            try {
                int iPadCount = iProcessBlock2 - this.padding.padCount(this.buf);
                System.arraycopy(this.buf, 0, bArr, i, iPadCount);
                return iPadCount;
            } finally {
                reset();
            }
        }
        reset();
        throw new DataLengthException("last block incomplete in decryption");
    }
}
