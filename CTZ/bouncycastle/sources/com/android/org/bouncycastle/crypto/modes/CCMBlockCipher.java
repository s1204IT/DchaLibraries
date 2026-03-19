package com.android.org.bouncycastle.crypto.modes;

import com.android.org.bouncycastle.crypto.BlockCipher;
import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.DataLengthException;
import com.android.org.bouncycastle.crypto.InvalidCipherTextException;
import com.android.org.bouncycastle.crypto.OutputLengthException;
import com.android.org.bouncycastle.crypto.macs.CBCBlockCipherMac;
import com.android.org.bouncycastle.crypto.params.AEADParameters;
import com.android.org.bouncycastle.crypto.params.ParametersWithIV;
import com.android.org.bouncycastle.util.Arrays;
import java.io.ByteArrayOutputStream;

public class CCMBlockCipher implements AEADBlockCipher {
    private int blockSize;
    private BlockCipher cipher;
    private boolean forEncryption;
    private byte[] initialAssociatedText;
    private CipherParameters keyParam;
    private byte[] macBlock;
    private int macSize;
    private byte[] nonce;
    private ExposedByteArrayOutputStream associatedText = new ExposedByteArrayOutputStream();
    private ExposedByteArrayOutputStream data = new ExposedByteArrayOutputStream();

    public CCMBlockCipher(BlockCipher blockCipher) {
        this.cipher = blockCipher;
        this.blockSize = blockCipher.getBlockSize();
        this.macBlock = new byte[this.blockSize];
        if (this.blockSize != 16) {
            throw new IllegalArgumentException("cipher required with a block size of 16.");
        }
    }

    @Override
    public BlockCipher getUnderlyingCipher() {
        return this.cipher;
    }

    @Override
    public void init(boolean z, CipherParameters cipherParameters) throws IllegalArgumentException {
        CipherParameters parameters;
        this.forEncryption = z;
        if (cipherParameters instanceof AEADParameters) {
            AEADParameters aEADParameters = (AEADParameters) cipherParameters;
            this.nonce = aEADParameters.getNonce();
            this.initialAssociatedText = aEADParameters.getAssociatedText();
            this.macSize = aEADParameters.getMacSize() / 8;
            parameters = aEADParameters.getKey();
        } else if (cipherParameters instanceof ParametersWithIV) {
            ParametersWithIV parametersWithIV = (ParametersWithIV) cipherParameters;
            this.nonce = parametersWithIV.getIV();
            this.initialAssociatedText = null;
            this.macSize = this.macBlock.length / 2;
            parameters = parametersWithIV.getParameters();
        } else {
            throw new IllegalArgumentException("invalid parameters passed to CCM: " + cipherParameters.getClass().getName());
        }
        if (parameters != null) {
            this.keyParam = parameters;
        }
        if (this.nonce == null || this.nonce.length < 7 || this.nonce.length > 13) {
            throw new IllegalArgumentException("nonce must have length from 7 to 13 octets");
        }
        reset();
    }

    @Override
    public String getAlgorithmName() {
        return this.cipher.getAlgorithmName() + "/CCM";
    }

    @Override
    public void processAADByte(byte b) {
        this.associatedText.write(b);
    }

    @Override
    public void processAADBytes(byte[] bArr, int i, int i2) {
        this.associatedText.write(bArr, i, i2);
    }

    @Override
    public int processByte(byte b, byte[] bArr, int i) throws IllegalStateException, DataLengthException {
        this.data.write(b);
        return 0;
    }

    @Override
    public int processBytes(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws IllegalStateException, DataLengthException {
        if (bArr.length < i + i2) {
            throw new DataLengthException("Input buffer too short");
        }
        this.data.write(bArr, i, i2);
        return 0;
    }

    @Override
    public int doFinal(byte[] bArr, int i) throws IllegalStateException, InvalidCipherTextException {
        int iProcessPacket = processPacket(this.data.getBuffer(), 0, this.data.size(), bArr, i);
        reset();
        return iProcessPacket;
    }

    @Override
    public void reset() {
        this.cipher.reset();
        this.associatedText.reset();
        this.data.reset();
    }

    @Override
    public byte[] getMac() {
        byte[] bArr = new byte[this.macSize];
        System.arraycopy(this.macBlock, 0, bArr, 0, bArr.length);
        return bArr;
    }

    @Override
    public int getUpdateOutputSize(int i) {
        return 0;
    }

    @Override
    public int getOutputSize(int i) {
        int size = i + this.data.size();
        if (this.forEncryption) {
            return size + this.macSize;
        }
        if (size < this.macSize) {
            return 0;
        }
        return size - this.macSize;
    }

    public byte[] processPacket(byte[] bArr, int i, int i2) throws IllegalStateException, InvalidCipherTextException {
        byte[] bArr2;
        if (this.forEncryption) {
            bArr2 = new byte[this.macSize + i2];
        } else {
            if (i2 < this.macSize) {
                throw new InvalidCipherTextException("data too short");
            }
            bArr2 = new byte[i2 - this.macSize];
        }
        processPacket(bArr, i, i2, bArr2, 0);
        return bArr2;
    }

    public int processPacket(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws IllegalStateException, InvalidCipherTextException, DataLengthException {
        int i4;
        int i5;
        if (this.keyParam == null) {
            throw new IllegalStateException("CCM cipher unitialized.");
        }
        int length = 15 - this.nonce.length;
        if (length < 4 && i2 >= (1 << (8 * length))) {
            throw new IllegalStateException("CCM packet too large for choice of q.");
        }
        byte[] bArr3 = new byte[this.blockSize];
        bArr3[0] = (byte) ((length - 1) & 7);
        System.arraycopy(this.nonce, 0, bArr3, 1, this.nonce.length);
        SICBlockCipher sICBlockCipher = new SICBlockCipher(this.cipher);
        sICBlockCipher.init(this.forEncryption, new ParametersWithIV(this.keyParam, bArr3));
        if (this.forEncryption) {
            i4 = this.macSize + i2;
            if (bArr2.length < i4 + i3) {
                throw new OutputLengthException("Output buffer too short.");
            }
            calculateMac(bArr, i, i2, this.macBlock);
            byte[] bArr4 = new byte[this.blockSize];
            sICBlockCipher.processBlock(this.macBlock, 0, bArr4, 0);
            int i6 = i;
            int i7 = i3;
            while (true) {
                i5 = i + i2;
                if (i6 >= i5 - this.blockSize) {
                    break;
                }
                sICBlockCipher.processBlock(bArr, i6, bArr2, i7);
                i7 += this.blockSize;
                i6 += this.blockSize;
            }
            byte[] bArr5 = new byte[this.blockSize];
            int i8 = i5 - i6;
            System.arraycopy(bArr, i6, bArr5, 0, i8);
            sICBlockCipher.processBlock(bArr5, 0, bArr5, 0);
            System.arraycopy(bArr5, 0, bArr2, i7, i8);
            System.arraycopy(bArr4, 0, bArr2, i3 + i2, this.macSize);
        } else {
            if (i2 < this.macSize) {
                throw new InvalidCipherTextException("data too short");
            }
            i4 = i2 - this.macSize;
            if (bArr2.length < i4 + i3) {
                throw new OutputLengthException("Output buffer too short.");
            }
            int i9 = i + i4;
            System.arraycopy(bArr, i9, this.macBlock, 0, this.macSize);
            sICBlockCipher.processBlock(this.macBlock, 0, this.macBlock, 0);
            for (int i10 = this.macSize; i10 != this.macBlock.length; i10++) {
                this.macBlock[i10] = 0;
            }
            int i11 = i;
            int i12 = i3;
            while (i11 < i9 - this.blockSize) {
                sICBlockCipher.processBlock(bArr, i11, bArr2, i12);
                i12 += this.blockSize;
                i11 += this.blockSize;
            }
            byte[] bArr6 = new byte[this.blockSize];
            int i13 = i4 - (i11 - i);
            System.arraycopy(bArr, i11, bArr6, 0, i13);
            sICBlockCipher.processBlock(bArr6, 0, bArr6, 0);
            System.arraycopy(bArr6, 0, bArr2, i12, i13);
            byte[] bArr7 = new byte[this.blockSize];
            calculateMac(bArr2, i3, i4, bArr7);
            if (!Arrays.constantTimeAreEqual(this.macBlock, bArr7)) {
                throw new InvalidCipherTextException("mac check in CCM failed");
            }
        }
        return i4;
    }

    private int calculateMac(byte[] bArr, int i, int i2, byte[] bArr2) {
        CBCBlockCipherMac cBCBlockCipherMac = new CBCBlockCipherMac(this.cipher, this.macSize * 8);
        cBCBlockCipherMac.init(this.keyParam);
        byte[] bArr3 = new byte[16];
        if (hasAssociatedText()) {
            bArr3[0] = (byte) (bArr3[0] | 64);
        }
        int i3 = 2;
        bArr3[0] = (byte) (bArr3[0] | ((((cBCBlockCipherMac.getMacSize() - 2) / 2) & 7) << 3));
        bArr3[0] = (byte) (bArr3[0] | (((15 - this.nonce.length) - 1) & 7));
        System.arraycopy(this.nonce, 0, bArr3, 1, this.nonce.length);
        int i4 = i2;
        int i5 = 1;
        while (i4 > 0) {
            bArr3[bArr3.length - i5] = (byte) (i4 & 255);
            i4 >>>= 8;
            i5++;
        }
        cBCBlockCipherMac.update(bArr3, 0, bArr3.length);
        if (hasAssociatedText()) {
            int associatedTextLength = getAssociatedTextLength();
            if (associatedTextLength < 65280) {
                cBCBlockCipherMac.update((byte) (associatedTextLength >> 8));
                cBCBlockCipherMac.update((byte) associatedTextLength);
            } else {
                cBCBlockCipherMac.update((byte) -1);
                cBCBlockCipherMac.update((byte) -2);
                cBCBlockCipherMac.update((byte) (associatedTextLength >> 24));
                cBCBlockCipherMac.update((byte) (associatedTextLength >> 16));
                cBCBlockCipherMac.update((byte) (associatedTextLength >> 8));
                cBCBlockCipherMac.update((byte) associatedTextLength);
                i3 = 6;
            }
            if (this.initialAssociatedText != null) {
                cBCBlockCipherMac.update(this.initialAssociatedText, 0, this.initialAssociatedText.length);
            }
            if (this.associatedText.size() > 0) {
                cBCBlockCipherMac.update(this.associatedText.getBuffer(), 0, this.associatedText.size());
            }
            int i6 = (i3 + associatedTextLength) % 16;
            if (i6 != 0) {
                while (i6 != 16) {
                    cBCBlockCipherMac.update((byte) 0);
                    i6++;
                }
            }
        }
        cBCBlockCipherMac.update(bArr, i, i2);
        return cBCBlockCipherMac.doFinal(bArr2, 0);
    }

    private int getAssociatedTextLength() {
        return this.associatedText.size() + (this.initialAssociatedText == null ? 0 : this.initialAssociatedText.length);
    }

    private boolean hasAssociatedText() {
        return getAssociatedTextLength() > 0;
    }

    private class ExposedByteArrayOutputStream extends ByteArrayOutputStream {
        public ExposedByteArrayOutputStream() {
        }

        public byte[] getBuffer() {
            return this.buf;
        }
    }
}
