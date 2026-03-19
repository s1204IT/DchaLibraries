package com.android.org.bouncycastle.crypto.modes;

import com.android.org.bouncycastle.crypto.BlockCipher;
import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.DataLengthException;
import com.android.org.bouncycastle.crypto.SkippingStreamCipher;
import com.android.org.bouncycastle.crypto.StreamBlockCipher;
import com.android.org.bouncycastle.crypto.params.ParametersWithIV;
import com.android.org.bouncycastle.util.Arrays;
import com.android.org.bouncycastle.util.Pack;

public class SICBlockCipher extends StreamBlockCipher implements SkippingStreamCipher {
    private byte[] IV;
    private final int blockSize;
    private int byteCount;
    private final BlockCipher cipher;
    private byte[] counter;
    private byte[] counterOut;

    public SICBlockCipher(BlockCipher c) {
        super(c);
        this.cipher = c;
        this.blockSize = this.cipher.getBlockSize();
        this.IV = new byte[this.blockSize];
        this.counter = new byte[this.blockSize];
        this.counterOut = new byte[this.blockSize];
        this.byteCount = 0;
    }

    @Override
    public void init(boolean forEncryption, CipherParameters cipherParameters) throws IllegalArgumentException {
        if (cipherParameters instanceof ParametersWithIV) {
            this.IV = Arrays.clone(cipherParameters.getIV());
            if (this.blockSize < this.IV.length) {
                throw new IllegalArgumentException("CTR/SIC mode requires IV no greater than: " + this.blockSize + " bytes.");
            }
            int maxCounterSize = 8 > this.blockSize / 2 ? this.blockSize / 2 : 8;
            if (this.blockSize - this.IV.length > maxCounterSize) {
                throw new IllegalArgumentException("CTR/SIC mode requires IV of at least: " + (this.blockSize - maxCounterSize) + " bytes.");
            }
            if (cipherParameters.getParameters() != null) {
                this.cipher.init(true, cipherParameters.getParameters());
            }
            reset();
            return;
        }
        throw new IllegalArgumentException("CTR/SIC mode requires ParametersWithIV");
    }

    @Override
    public String getAlgorithmName() {
        return this.cipher.getAlgorithmName() + "/SIC";
    }

    @Override
    public int getBlockSize() {
        return this.cipher.getBlockSize();
    }

    @Override
    public int processBlock(byte[] in, int inOff, byte[] out, int outOff) throws IllegalStateException, DataLengthException {
        processBytes(in, inOff, this.blockSize, out, outOff);
        return this.blockSize;
    }

    @Override
    protected byte calculateByte(byte in) throws IllegalStateException, DataLengthException {
        if (this.byteCount == 0) {
            this.cipher.processBlock(this.counter, 0, this.counterOut, 0);
            byte[] bArr = this.counterOut;
            int i = this.byteCount;
            this.byteCount = i + 1;
            return (byte) (bArr[i] ^ in);
        }
        byte[] bArr2 = this.counterOut;
        int i2 = this.byteCount;
        this.byteCount = i2 + 1;
        byte rv = (byte) (bArr2[i2] ^ in);
        if (this.byteCount == this.counter.length) {
            this.byteCount = 0;
            incrementCounterAt(0);
            checkCounter();
        }
        return rv;
    }

    private void checkCounter() {
        if (this.IV.length >= this.blockSize) {
            return;
        }
        for (int i = 0; i != this.IV.length; i++) {
            if (this.counter[i] != this.IV[i]) {
                throw new IllegalStateException("Counter in CTR/SIC mode out of range.");
            }
        }
    }

    private void incrementCounterAt(int pos) {
        byte b;
        int i = this.counter.length - pos;
        do {
            i--;
            if (i < 0) {
                return;
            }
            byte[] bArr = this.counter;
            b = (byte) (bArr[i] + 1);
            bArr[i] = b;
        } while (b == 0);
    }

    private void incrementCounter(int offSet) {
        byte old = this.counter[this.counter.length - 1];
        byte[] bArr = this.counter;
        int length = this.counter.length - 1;
        bArr[length] = (byte) (bArr[length] + offSet);
        if (old == 0 || this.counter[this.counter.length - 1] >= old) {
            return;
        }
        incrementCounterAt(1);
    }

    private void decrementCounterAt(int pos) {
        byte b;
        int i = this.counter.length - pos;
        do {
            i--;
            if (i < 0) {
                return;
            }
            b = (byte) (r1[i] - 1);
            this.counter[i] = b;
        } while (b == -1);
    }

    private void adjustCounter(long n) {
        if (n >= 0) {
            long numBlocks = (((long) this.byteCount) + n) / ((long) this.blockSize);
            long rem = numBlocks;
            if (numBlocks > 255) {
                for (int i = 5; i >= 1; i--) {
                    long diff = 1 << (i * 8);
                    while (rem >= diff) {
                        incrementCounterAt(i);
                        rem -= diff;
                    }
                }
            }
            incrementCounter((int) rem);
            this.byteCount = (int) ((((long) this.byteCount) + n) - (((long) this.blockSize) * numBlocks));
            return;
        }
        long numBlocks2 = ((-n) - ((long) this.byteCount)) / ((long) this.blockSize);
        long rem2 = numBlocks2;
        if (numBlocks2 > 255) {
            for (int i2 = 5; i2 >= 1; i2--) {
                long diff2 = 1 << (i2 * 8);
                while (rem2 > diff2) {
                    decrementCounterAt(i2);
                    rem2 -= diff2;
                }
            }
        }
        for (long i3 = 0; i3 != rem2; i3++) {
            decrementCounterAt(0);
        }
        int gap = (int) (((long) this.byteCount) + n + (((long) this.blockSize) * numBlocks2));
        if (gap >= 0) {
            this.byteCount = 0;
        } else {
            decrementCounterAt(0);
            this.byteCount = this.blockSize + gap;
        }
    }

    @Override
    public void reset() {
        Arrays.fill(this.counter, (byte) 0);
        System.arraycopy(this.IV, 0, this.counter, 0, this.IV.length);
        this.cipher.reset();
        this.byteCount = 0;
    }

    @Override
    public long skip(long numberOfBytes) {
        adjustCounter(numberOfBytes);
        checkCounter();
        this.cipher.processBlock(this.counter, 0, this.counterOut, 0);
        return numberOfBytes;
    }

    @Override
    public long seekTo(long position) {
        reset();
        return skip(position);
    }

    @Override
    public long getPosition() {
        int v;
        byte[] res = new byte[this.counter.length];
        System.arraycopy(this.counter, 0, res, 0, res.length);
        for (int i = res.length - 1; i >= 1; i--) {
            if (i < this.IV.length) {
                v = (res[i] & 255) - (this.IV[i] & 255);
            } else {
                v = res[i] & 255;
            }
            if (v < 0) {
                res[i - 1] = (byte) (res[r3] - 1);
                v += 256;
            }
            res[i] = (byte) v;
        }
        return (Pack.bigEndianToLong(res, res.length - 8) * ((long) this.blockSize)) + ((long) this.byteCount);
    }
}
