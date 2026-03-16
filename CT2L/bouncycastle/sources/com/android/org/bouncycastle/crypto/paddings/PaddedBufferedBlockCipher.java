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

    public PaddedBufferedBlockCipher(BlockCipher cipher, BlockCipherPadding padding) {
        this.cipher = cipher;
        this.padding = padding;
        this.buf = new byte[cipher.getBlockSize()];
        this.bufOff = 0;
    }

    public PaddedBufferedBlockCipher(BlockCipher cipher) {
        this(cipher, new PKCS7Padding());
    }

    @Override
    public void init(boolean forEncryption, CipherParameters params) throws IllegalArgumentException {
        this.forEncryption = forEncryption;
        reset();
        if (params instanceof ParametersWithRandom) {
            ParametersWithRandom p = (ParametersWithRandom) params;
            this.padding.init(p.getRandom());
            this.cipher.init(forEncryption, p.getParameters());
        } else {
            this.padding.init(null);
            this.cipher.init(forEncryption, params);
        }
    }

    @Override
    public int getOutputSize(int len) {
        int total = len + this.bufOff;
        int leftOver = total % this.buf.length;
        if (leftOver != 0) {
            return (total - leftOver) + this.buf.length;
        }
        if (this.forEncryption) {
            return total + this.buf.length;
        }
        return total;
    }

    @Override
    public int getUpdateOutputSize(int len) {
        int total = len + this.bufOff;
        int leftOver = total % this.buf.length;
        return leftOver == 0 ? total - this.buf.length : total - leftOver;
    }

    @Override
    public int processByte(byte in, byte[] out, int outOff) throws IllegalStateException, DataLengthException {
        int resultLen = 0;
        if (this.bufOff == this.buf.length) {
            resultLen = this.cipher.processBlock(this.buf, 0, out, outOff);
            this.bufOff = 0;
        }
        byte[] bArr = this.buf;
        int i = this.bufOff;
        this.bufOff = i + 1;
        bArr[i] = in;
        return resultLen;
    }

    @Override
    public int processBytes(byte[] in, int inOff, int len, byte[] out, int outOff) throws IllegalStateException, DataLengthException {
        if (len < 0) {
            throw new IllegalArgumentException("Can't have a negative input length!");
        }
        int blockSize = getBlockSize();
        int length = getUpdateOutputSize(len);
        if (length > 0 && outOff + length > out.length) {
            throw new OutputLengthException("output buffer too short");
        }
        int resultLen = 0;
        int gapLen = this.buf.length - this.bufOff;
        if (len > gapLen) {
            System.arraycopy(in, inOff, this.buf, this.bufOff, gapLen);
            resultLen = 0 + this.cipher.processBlock(this.buf, 0, out, outOff);
            this.bufOff = 0;
            len -= gapLen;
            inOff += gapLen;
            while (len > this.buf.length) {
                resultLen += this.cipher.processBlock(in, inOff, out, outOff + resultLen);
                len -= blockSize;
                inOff += blockSize;
            }
        }
        System.arraycopy(in, inOff, this.buf, this.bufOff, len);
        this.bufOff += len;
        return resultLen;
    }

    @Override
    public int doFinal(byte[] out, int outOff) throws IllegalStateException, DataLengthException, InvalidCipherTextException {
        int blockSize = this.cipher.getBlockSize();
        int resultLen = 0;
        if (this.forEncryption) {
            if (this.bufOff == blockSize) {
                if ((blockSize * 2) + outOff > out.length) {
                    reset();
                    throw new OutputLengthException("output buffer too short");
                }
                resultLen = this.cipher.processBlock(this.buf, 0, out, outOff);
                this.bufOff = 0;
            }
            this.padding.addPadding(this.buf, this.bufOff);
            return resultLen + this.cipher.processBlock(this.buf, 0, out, outOff + resultLen);
        }
        if (this.bufOff == blockSize) {
            int resultLen2 = this.cipher.processBlock(this.buf, 0, this.buf, 0);
            this.bufOff = 0;
            try {
                int resultLen3 = resultLen2 - this.padding.padCount(this.buf);
                System.arraycopy(this.buf, 0, out, outOff, resultLen3);
                return resultLen3;
            } finally {
                reset();
            }
        }
        reset();
        throw new DataLengthException("last block incomplete in decryption");
    }
}
