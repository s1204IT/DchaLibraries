package com.android.org.bouncycastle.crypto;

public class BufferedBlockCipher {
    protected byte[] buf;
    protected int bufOff;
    protected BlockCipher cipher;
    protected boolean forEncryption;
    protected boolean partialBlockOkay;
    protected boolean pgpCFB;

    protected BufferedBlockCipher() {
    }

    public BufferedBlockCipher(BlockCipher blockCipher) {
        this.cipher = blockCipher;
        this.buf = new byte[blockCipher.getBlockSize()];
        boolean z = false;
        this.bufOff = 0;
        String algorithmName = blockCipher.getAlgorithmName();
        int iIndexOf = algorithmName.indexOf(47) + 1;
        this.pgpCFB = iIndexOf > 0 && algorithmName.startsWith("PGP", iIndexOf);
        if (this.pgpCFB || (blockCipher instanceof StreamCipher)) {
            this.partialBlockOkay = true;
            return;
        }
        if (iIndexOf > 0 && algorithmName.startsWith("OpenPGP", iIndexOf)) {
            z = true;
        }
        this.partialBlockOkay = z;
    }

    public BlockCipher getUnderlyingCipher() {
        return this.cipher;
    }

    public void init(boolean z, CipherParameters cipherParameters) throws IllegalArgumentException {
        this.forEncryption = z;
        reset();
        this.cipher.init(z, cipherParameters);
    }

    public int getBlockSize() {
        return this.cipher.getBlockSize();
    }

    public int getUpdateOutputSize(int i) {
        int length;
        int i2 = i + this.bufOff;
        if (this.pgpCFB && this.forEncryption) {
            length = (i2 % this.buf.length) - (this.cipher.getBlockSize() + 2);
        } else {
            length = i2 % this.buf.length;
        }
        return i2 - length;
    }

    public int getOutputSize(int i) {
        return i + this.bufOff;
    }

    public int processByte(byte b, byte[] bArr, int i) throws IllegalStateException, DataLengthException {
        byte[] bArr2 = this.buf;
        int i2 = this.bufOff;
        this.bufOff = i2 + 1;
        bArr2[i2] = b;
        if (this.bufOff != this.buf.length) {
            return 0;
        }
        int iProcessBlock = this.cipher.processBlock(this.buf, 0, bArr, i);
        this.bufOff = 0;
        return iProcessBlock;
    }

    public int processBytes(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws IllegalStateException, DataLengthException {
        int iProcessBlock;
        if (i2 < 0) {
            throw new IllegalArgumentException("Can't have a negative input length!");
        }
        int blockSize = getBlockSize();
        int updateOutputSize = getUpdateOutputSize(i2);
        if (updateOutputSize > 0 && updateOutputSize + i3 > bArr2.length) {
            throw new OutputLengthException("output buffer too short");
        }
        int length = this.buf.length - this.bufOff;
        if (i2 > length) {
            System.arraycopy(bArr, i, this.buf, this.bufOff, length);
            iProcessBlock = this.cipher.processBlock(this.buf, 0, bArr2, i3) + 0;
            this.bufOff = 0;
            i2 -= length;
            i += length;
            while (i2 > this.buf.length) {
                iProcessBlock += this.cipher.processBlock(bArr, i, bArr2, i3 + iProcessBlock);
                i2 -= blockSize;
                i += blockSize;
            }
        } else {
            iProcessBlock = 0;
        }
        System.arraycopy(bArr, i, this.buf, this.bufOff, i2);
        this.bufOff += i2;
        if (this.bufOff == this.buf.length) {
            int iProcessBlock2 = iProcessBlock + this.cipher.processBlock(this.buf, 0, bArr2, i3 + iProcessBlock);
            this.bufOff = 0;
            return iProcessBlock2;
        }
        return iProcessBlock;
    }

    public int doFinal(byte[] bArr, int i) throws IllegalStateException, DataLengthException, InvalidCipherTextException {
        int i2;
        try {
            if (this.bufOff + i > bArr.length) {
                throw new OutputLengthException("output buffer too short for doFinal()");
            }
            if (this.bufOff != 0) {
                if (!this.partialBlockOkay) {
                    throw new DataLengthException("data not block size aligned");
                }
                this.cipher.processBlock(this.buf, 0, this.buf, 0);
                i2 = this.bufOff;
                this.bufOff = 0;
                System.arraycopy(this.buf, 0, bArr, i, i2);
            } else {
                i2 = 0;
            }
            return i2;
        } finally {
            reset();
        }
    }

    public void reset() {
        for (int i = 0; i < this.buf.length; i++) {
            this.buf[i] = 0;
        }
        this.bufOff = 0;
        this.cipher.reset();
    }
}
