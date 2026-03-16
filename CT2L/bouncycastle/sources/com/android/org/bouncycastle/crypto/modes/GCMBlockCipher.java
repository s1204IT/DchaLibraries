package com.android.org.bouncycastle.crypto.modes;

import com.android.org.bouncycastle.crypto.BlockCipher;
import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.DataLengthException;
import com.android.org.bouncycastle.crypto.InvalidCipherTextException;
import com.android.org.bouncycastle.crypto.modes.gcm.GCMExponentiator;
import com.android.org.bouncycastle.crypto.modes.gcm.GCMMultiplier;
import com.android.org.bouncycastle.crypto.modes.gcm.Tables1kGCMExponentiator;
import com.android.org.bouncycastle.crypto.modes.gcm.Tables8kGCMMultiplier;
import com.android.org.bouncycastle.crypto.params.AEADParameters;
import com.android.org.bouncycastle.crypto.params.KeyParameter;
import com.android.org.bouncycastle.crypto.params.ParametersWithIV;
import com.android.org.bouncycastle.crypto.util.Pack;
import com.android.org.bouncycastle.util.Arrays;

public class GCMBlockCipher implements AEADBlockCipher {
    private static final int BLOCK_SIZE = 16;
    private static final long MAX_INPUT_SIZE = 68719476704L;
    private byte[] H;
    private byte[] J0;
    private byte[] S;
    private byte[] S_at;
    private byte[] S_atPre;
    private byte[] atBlock;
    private int atBlockPos;
    private long atLength;
    private long atLengthPre;
    private byte[] bufBlock;
    private int bufOff;
    private BlockCipher cipher;
    private byte[] counter;
    private GCMExponentiator exp;
    private boolean forEncryption;
    private byte[] initialAssociatedText;
    private byte[] macBlock;
    private int macSize;
    private GCMMultiplier multiplier;
    private byte[] nonce;
    private long totalLength;

    public GCMBlockCipher(BlockCipher c) {
        this(c, null);
    }

    public GCMBlockCipher(BlockCipher c, GCMMultiplier m) {
        if (c.getBlockSize() != 16) {
            throw new IllegalArgumentException("cipher required with a block size of 16.");
        }
        m = m == null ? new Tables8kGCMMultiplier() : m;
        this.cipher = c;
        this.multiplier = m;
    }

    @Override
    public BlockCipher getUnderlyingCipher() {
        return this.cipher;
    }

    @Override
    public String getAlgorithmName() {
        return this.cipher.getAlgorithmName() + "/GCM";
    }

    @Override
    public void init(boolean forEncryption, CipherParameters params) throws IllegalArgumentException {
        KeyParameter keyParam;
        this.forEncryption = forEncryption;
        this.macBlock = null;
        if (params instanceof AEADParameters) {
            AEADParameters param = (AEADParameters) params;
            this.nonce = param.getNonce();
            this.initialAssociatedText = param.getAssociatedText();
            int macSizeBits = param.getMacSize();
            if (macSizeBits < 96 || macSizeBits > 128 || macSizeBits % 8 != 0) {
                throw new IllegalArgumentException("Invalid value for MAC size: " + macSizeBits);
            }
            this.macSize = macSizeBits / 8;
            keyParam = param.getKey();
        } else if (params instanceof ParametersWithIV) {
            ParametersWithIV param2 = (ParametersWithIV) params;
            this.nonce = param2.getIV();
            this.initialAssociatedText = null;
            this.macSize = 16;
            keyParam = (KeyParameter) param2.getParameters();
        } else {
            throw new IllegalArgumentException("invalid parameters passed to GCM");
        }
        int bufLength = forEncryption ? 16 : this.macSize + 16;
        this.bufBlock = new byte[bufLength];
        if (this.nonce == null || this.nonce.length < 1) {
            throw new IllegalArgumentException("IV must be at least 1 byte");
        }
        if (keyParam != null) {
            this.cipher.init(true, keyParam);
            this.H = new byte[16];
            this.cipher.processBlock(this.H, 0, this.H, 0);
            this.multiplier.init(this.H);
            this.exp = null;
        }
        this.J0 = new byte[16];
        if (this.nonce.length == 12) {
            System.arraycopy(this.nonce, 0, this.J0, 0, this.nonce.length);
            this.J0[15] = 1;
        } else {
            gHASH(this.J0, this.nonce, this.nonce.length);
            byte[] X = new byte[16];
            Pack.longToBigEndian(((long) this.nonce.length) * 8, X, 8);
            gHASHBlock(this.J0, X);
        }
        this.S = new byte[16];
        this.S_at = new byte[16];
        this.S_atPre = new byte[16];
        this.atBlock = new byte[16];
        this.atBlockPos = 0;
        this.atLength = 0L;
        this.atLengthPre = 0L;
        this.counter = Arrays.clone(this.J0);
        this.bufOff = 0;
        this.totalLength = 0L;
        if (this.initialAssociatedText != null) {
            processAADBytes(this.initialAssociatedText, 0, this.initialAssociatedText.length);
        }
    }

    @Override
    public byte[] getMac() {
        return Arrays.clone(this.macBlock);
    }

    @Override
    public int getOutputSize(int len) {
        int totalData = len + this.bufOff;
        if (this.forEncryption) {
            return this.macSize + totalData;
        }
        if (totalData < this.macSize) {
            return 0;
        }
        return totalData - this.macSize;
    }

    private long getTotalInputSizeAfterNewInput(int newInputLen) {
        return this.totalLength + ((long) newInputLen) + ((long) this.bufOff);
    }

    @Override
    public int getUpdateOutputSize(int len) {
        int totalData = len + this.bufOff;
        if (!this.forEncryption) {
            if (totalData < this.macSize) {
                return 0;
            }
            totalData -= this.macSize;
        }
        return totalData - (totalData % 16);
    }

    @Override
    public void processAADByte(byte in) {
        if (getTotalInputSizeAfterNewInput(1) > MAX_INPUT_SIZE) {
            throw new DataLengthException("Input exceeded 68719476704 bytes");
        }
        this.atBlock[this.atBlockPos] = in;
        int i = this.atBlockPos + 1;
        this.atBlockPos = i;
        if (i == 16) {
            gHASHBlock(this.S_at, this.atBlock);
            this.atBlockPos = 0;
            this.atLength += 16;
        }
    }

    @Override
    public void processAADBytes(byte[] in, int inOff, int len) {
        if (getTotalInputSizeAfterNewInput(len) > MAX_INPUT_SIZE) {
            throw new DataLengthException("Input exceeded 68719476704 bytes");
        }
        for (int i = 0; i < len; i++) {
            this.atBlock[this.atBlockPos] = in[inOff + i];
            int i2 = this.atBlockPos + 1;
            this.atBlockPos = i2;
            if (i2 == 16) {
                gHASHBlock(this.S_at, this.atBlock);
                this.atBlockPos = 0;
                this.atLength += 16;
            }
        }
    }

    private void initCipher() {
        if (this.atLength > 0) {
            System.arraycopy(this.S_at, 0, this.S_atPre, 0, 16);
            this.atLengthPre = this.atLength;
        }
        if (this.atBlockPos > 0) {
            gHASHPartial(this.S_atPre, this.atBlock, 0, this.atBlockPos);
            this.atLengthPre += (long) this.atBlockPos;
        }
        if (this.atLengthPre > 0) {
            System.arraycopy(this.S_atPre, 0, this.S, 0, 16);
        }
    }

    @Override
    public int processByte(byte in, byte[] out, int outOff) throws DataLengthException {
        if (getTotalInputSizeAfterNewInput(1) > MAX_INPUT_SIZE) {
            throw new DataLengthException("Input exceeded 68719476704 bytes");
        }
        this.bufBlock[this.bufOff] = in;
        int i = this.bufOff + 1;
        this.bufOff = i;
        if (i != this.bufBlock.length) {
            return 0;
        }
        outputBlock(out, outOff);
        return 16;
    }

    @Override
    public int processBytes(byte[] in, int inOff, int len, byte[] out, int outOff) throws DataLengthException {
        if (getTotalInputSizeAfterNewInput(len) > MAX_INPUT_SIZE) {
            throw new DataLengthException("Input exceeded 68719476704 bytes");
        }
        int resultLen = 0;
        for (int i = 0; i < len; i++) {
            this.bufBlock[this.bufOff] = in[inOff + i];
            int i2 = this.bufOff + 1;
            this.bufOff = i2;
            if (i2 == this.bufBlock.length) {
                outputBlock(out, outOff + resultLen);
                resultLen += 16;
            }
        }
        return resultLen;
    }

    private void outputBlock(byte[] output, int offset) {
        if (this.totalLength == 0) {
            initCipher();
        }
        gCTRBlock(this.bufBlock, output, offset);
        if (this.forEncryption) {
            this.bufOff = 0;
        } else {
            System.arraycopy(this.bufBlock, 16, this.bufBlock, 0, this.macSize);
            this.bufOff = this.macSize;
        }
    }

    @Override
    public int doFinal(byte[] out, int outOff) throws IllegalStateException, InvalidCipherTextException {
        if (this.totalLength == 0) {
            initCipher();
        }
        int extra = this.bufOff;
        if (!this.forEncryption) {
            if (extra < this.macSize) {
                throw new InvalidCipherTextException("data too short");
            }
            extra -= this.macSize;
        }
        if (extra > 0) {
            gCTRPartial(this.bufBlock, 0, extra, out, outOff);
        }
        this.atLength += (long) this.atBlockPos;
        if (this.atLength > this.atLengthPre) {
            if (this.atBlockPos > 0) {
                gHASHPartial(this.S_at, this.atBlock, 0, this.atBlockPos);
            }
            if (this.atLengthPre > 0) {
                xor(this.S_at, this.S_atPre);
            }
            long c = ((this.totalLength * 8) + 127) >>> 7;
            byte[] H_c = new byte[16];
            if (this.exp == null) {
                this.exp = new Tables1kGCMExponentiator();
                this.exp.init(this.H);
            }
            this.exp.exponentiateX(c, H_c);
            multiply(this.S_at, H_c);
            xor(this.S, this.S_at);
        }
        byte[] X = new byte[16];
        Pack.longToBigEndian(this.atLength * 8, X, 0);
        Pack.longToBigEndian(this.totalLength * 8, X, 8);
        gHASHBlock(this.S, X);
        byte[] tag = new byte[16];
        this.cipher.processBlock(this.J0, 0, tag, 0);
        xor(tag, this.S);
        int resultLen = extra;
        this.macBlock = new byte[this.macSize];
        System.arraycopy(tag, 0, this.macBlock, 0, this.macSize);
        if (this.forEncryption) {
            System.arraycopy(this.macBlock, 0, out, this.bufOff + outOff, this.macSize);
            resultLen += this.macSize;
        } else {
            byte[] msgMac = new byte[this.macSize];
            System.arraycopy(this.bufBlock, extra, msgMac, 0, this.macSize);
            if (!Arrays.constantTimeAreEqual(this.macBlock, msgMac)) {
                throw new InvalidCipherTextException("mac check in GCM failed");
            }
        }
        reset(false);
        return resultLen;
    }

    @Override
    public void reset() {
        reset(true);
    }

    private void reset(boolean clearMac) {
        this.cipher.reset();
        this.S = new byte[16];
        this.S_at = new byte[16];
        this.S_atPre = new byte[16];
        this.atBlock = new byte[16];
        this.atBlockPos = 0;
        this.atLength = 0L;
        this.atLengthPre = 0L;
        this.counter = Arrays.clone(this.J0);
        this.bufOff = 0;
        this.totalLength = 0L;
        if (this.bufBlock != null) {
            Arrays.fill(this.bufBlock, (byte) 0);
        }
        if (clearMac) {
            this.macBlock = null;
        }
        if (this.initialAssociatedText != null) {
            processAADBytes(this.initialAssociatedText, 0, this.initialAssociatedText.length);
        }
    }

    private void gCTRBlock(byte[] block, byte[] out, int outOff) {
        byte[] tmp = getNextCounterBlock();
        xor(tmp, block);
        System.arraycopy(tmp, 0, out, outOff, 16);
        byte[] bArr = this.S;
        if (!this.forEncryption) {
            tmp = block;
        }
        gHASHBlock(bArr, tmp);
        this.totalLength += 16;
    }

    private void gCTRPartial(byte[] buf, int off, int len, byte[] out, int outOff) {
        byte[] tmp = getNextCounterBlock();
        xor(tmp, buf, off, len);
        System.arraycopy(tmp, 0, out, outOff, len);
        byte[] bArr = this.S;
        if (!this.forEncryption) {
            tmp = buf;
        }
        gHASHPartial(bArr, tmp, 0, len);
        this.totalLength += (long) len;
    }

    private void gHASH(byte[] Y, byte[] b, int len) {
        for (int pos = 0; pos < len; pos += 16) {
            int num = Math.min(len - pos, 16);
            gHASHPartial(Y, b, pos, num);
        }
    }

    private void gHASHBlock(byte[] Y, byte[] b) {
        xor(Y, b);
        this.multiplier.multiplyH(Y);
    }

    private void gHASHPartial(byte[] Y, byte[] b, int off, int len) {
        xor(Y, b, off, len);
        this.multiplier.multiplyH(Y);
    }

    private byte[] getNextCounterBlock() {
        for (int i = 15; i >= 12; i--) {
            byte b = (byte) ((this.counter[i] + 1) & 255);
            this.counter[i] = b;
            if (b != 0) {
                break;
            }
        }
        byte[] tmp = new byte[16];
        this.cipher.processBlock(this.counter, 0, tmp, 0);
        return tmp;
    }

    private static void multiply(byte[] block, byte[] val) {
        byte[] tmp = Arrays.clone(block);
        byte[] c = new byte[16];
        for (int i = 0; i < 16; i++) {
            byte bits = val[i];
            for (int j = 7; j >= 0; j--) {
                if (((1 << j) & bits) != 0) {
                    xor(c, tmp);
                }
                boolean lsb = (tmp[15] & 1) != 0;
                shiftRight(tmp);
                if (lsb) {
                    tmp[0] = (byte) (tmp[0] ^ (-31));
                }
            }
        }
        System.arraycopy(c, 0, block, 0, 16);
    }

    private static void shiftRight(byte[] block) {
        int i = 0;
        int bit = 0;
        while (true) {
            int b = block[i] & 255;
            block[i] = (byte) ((b >>> 1) | bit);
            i++;
            if (i != 16) {
                bit = (b & 1) << 7;
            } else {
                return;
            }
        }
    }

    private static void xor(byte[] block, byte[] val) {
        for (int i = 15; i >= 0; i--) {
            block[i] = (byte) (block[i] ^ val[i]);
        }
    }

    private static void xor(byte[] block, byte[] val, int off, int len) {
        while (true) {
            int len2 = len;
            len = len2 - 1;
            if (len2 > 0) {
                block[len] = (byte) (block[len] ^ val[off + len]);
            } else {
                return;
            }
        }
    }
}
