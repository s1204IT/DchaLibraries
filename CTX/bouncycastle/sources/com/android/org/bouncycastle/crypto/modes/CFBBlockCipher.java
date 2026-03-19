package com.android.org.bouncycastle.crypto.modes;

import com.android.org.bouncycastle.crypto.BlockCipher;
import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.DataLengthException;
import com.android.org.bouncycastle.crypto.StreamBlockCipher;
import com.android.org.bouncycastle.crypto.params.ParametersWithIV;
import com.android.org.bouncycastle.util.Arrays;

public class CFBBlockCipher extends StreamBlockCipher {
    private byte[] IV;
    private int blockSize;
    private int byteCount;
    private byte[] cfbOutV;
    private byte[] cfbV;
    private BlockCipher cipher;
    private boolean encrypting;
    private byte[] inBuf;

    public CFBBlockCipher(BlockCipher blockCipher, int i) {
        super(blockCipher);
        this.cipher = null;
        this.cipher = blockCipher;
        this.blockSize = i / 8;
        this.IV = new byte[blockCipher.getBlockSize()];
        this.cfbV = new byte[blockCipher.getBlockSize()];
        this.cfbOutV = new byte[blockCipher.getBlockSize()];
        this.inBuf = new byte[this.blockSize];
    }

    @Override
    public void init(boolean z, CipherParameters cipherParameters) throws IllegalArgumentException {
        this.encrypting = z;
        if (cipherParameters instanceof ParametersWithIV) {
            ParametersWithIV parametersWithIV = (ParametersWithIV) cipherParameters;
            byte[] iv = parametersWithIV.getIV();
            if (iv.length < this.IV.length) {
                System.arraycopy(iv, 0, this.IV, this.IV.length - iv.length, iv.length);
                for (int i = 0; i < this.IV.length - iv.length; i++) {
                    this.IV[i] = 0;
                }
            } else {
                System.arraycopy(iv, 0, this.IV, 0, this.IV.length);
            }
            reset();
            if (parametersWithIV.getParameters() != null) {
                this.cipher.init(true, parametersWithIV.getParameters());
                return;
            }
            return;
        }
        reset();
        if (cipherParameters != null) {
            this.cipher.init(true, cipherParameters);
        }
    }

    @Override
    public String getAlgorithmName() {
        return this.cipher.getAlgorithmName() + "/CFB" + (this.blockSize * 8);
    }

    @Override
    protected byte calculateByte(byte b) throws IllegalStateException, DataLengthException {
        return this.encrypting ? encryptByte(b) : decryptByte(b);
    }

    private byte encryptByte(byte b) {
        if (this.byteCount == 0) {
            this.cipher.processBlock(this.cfbV, 0, this.cfbOutV, 0);
        }
        byte b2 = (byte) (b ^ this.cfbOutV[this.byteCount]);
        byte[] bArr = this.inBuf;
        int i = this.byteCount;
        this.byteCount = i + 1;
        bArr[i] = b2;
        if (this.byteCount == this.blockSize) {
            this.byteCount = 0;
            System.arraycopy(this.cfbV, this.blockSize, this.cfbV, 0, this.cfbV.length - this.blockSize);
            System.arraycopy(this.inBuf, 0, this.cfbV, this.cfbV.length - this.blockSize, this.blockSize);
        }
        return b2;
    }

    private byte decryptByte(byte b) {
        if (this.byteCount == 0) {
            this.cipher.processBlock(this.cfbV, 0, this.cfbOutV, 0);
        }
        this.inBuf[this.byteCount] = b;
        byte[] bArr = this.cfbOutV;
        int i = this.byteCount;
        this.byteCount = i + 1;
        byte b2 = (byte) (b ^ bArr[i]);
        if (this.byteCount == this.blockSize) {
            this.byteCount = 0;
            System.arraycopy(this.cfbV, this.blockSize, this.cfbV, 0, this.cfbV.length - this.blockSize);
            System.arraycopy(this.inBuf, 0, this.cfbV, this.cfbV.length - this.blockSize, this.blockSize);
        }
        return b2;
    }

    @Override
    public int getBlockSize() {
        return this.blockSize;
    }

    @Override
    public int processBlock(byte[] bArr, int i, byte[] bArr2, int i2) throws IllegalStateException, DataLengthException {
        processBytes(bArr, i, this.blockSize, bArr2, i2);
        return this.blockSize;
    }

    public int encryptBlock(byte[] bArr, int i, byte[] bArr2, int i2) throws IllegalStateException, DataLengthException {
        processBytes(bArr, i, this.blockSize, bArr2, i2);
        return this.blockSize;
    }

    public int decryptBlock(byte[] bArr, int i, byte[] bArr2, int i2) throws IllegalStateException, DataLengthException {
        processBytes(bArr, i, this.blockSize, bArr2, i2);
        return this.blockSize;
    }

    public byte[] getCurrentIV() {
        return Arrays.clone(this.cfbV);
    }

    @Override
    public void reset() {
        System.arraycopy(this.IV, 0, this.cfbV, 0, this.IV.length);
        Arrays.fill(this.inBuf, (byte) 0);
        this.byteCount = 0;
        this.cipher.reset();
    }
}
