package com.android.org.bouncycastle.crypto.digests;

import com.android.org.bouncycastle.crypto.ExtendedDigest;
import com.android.org.bouncycastle.util.Memoable;
import com.android.org.bouncycastle.util.Pack;

public abstract class GeneralDigest implements ExtendedDigest, Memoable {
    private static final int BYTE_LENGTH = 64;
    private long byteCount;
    private final byte[] xBuf;
    private int xBufOff;

    protected abstract void processBlock();

    protected abstract void processLength(long j);

    protected abstract void processWord(byte[] bArr, int i);

    protected GeneralDigest() {
        this.xBuf = new byte[4];
        this.xBufOff = 0;
    }

    protected GeneralDigest(GeneralDigest t) {
        this.xBuf = new byte[4];
        copyIn(t);
    }

    protected GeneralDigest(byte[] encodedState) {
        this.xBuf = new byte[4];
        System.arraycopy(encodedState, 0, this.xBuf, 0, this.xBuf.length);
        this.xBufOff = Pack.bigEndianToInt(encodedState, 4);
        this.byteCount = Pack.bigEndianToLong(encodedState, 8);
    }

    protected void copyIn(GeneralDigest t) {
        System.arraycopy(t.xBuf, 0, this.xBuf, 0, t.xBuf.length);
        this.xBufOff = t.xBufOff;
        this.byteCount = t.byteCount;
    }

    @Override
    public void update(byte in) {
        byte[] bArr = this.xBuf;
        int i = this.xBufOff;
        this.xBufOff = i + 1;
        bArr[i] = in;
        if (this.xBufOff == this.xBuf.length) {
            processWord(this.xBuf, 0);
            this.xBufOff = 0;
        }
        this.byteCount++;
    }

    @Override
    public void update(byte[] in, int inOff, int len) {
        int len2 = Math.max(0, len);
        int i = 0;
        if (this.xBufOff != 0) {
            while (true) {
                int i2 = i;
                if (i2 >= len2) {
                    i = i2;
                    break;
                }
                byte[] bArr = this.xBuf;
                int i3 = this.xBufOff;
                this.xBufOff = i3 + 1;
                i = i2 + 1;
                bArr[i3] = in[inOff + i2];
                if (this.xBufOff == 4) {
                    processWord(this.xBuf, 0);
                    this.xBufOff = 0;
                    break;
                }
            }
        }
        int limit = ((len2 - i) & (-4)) + i;
        while (i < limit) {
            processWord(in, inOff + i);
            i += 4;
        }
        while (true) {
            int i4 = i;
            if (i4 < len2) {
                byte[] bArr2 = this.xBuf;
                int i5 = this.xBufOff;
                this.xBufOff = i5 + 1;
                i = i4 + 1;
                bArr2[i5] = in[inOff + i4];
            } else {
                this.byteCount += (long) len2;
                return;
            }
        }
    }

    public void finish() {
        long bitLength = this.byteCount << 3;
        update((byte) -128);
        while (this.xBufOff != 0) {
            update((byte) 0);
        }
        processLength(bitLength);
        processBlock();
    }

    @Override
    public void reset() {
        this.byteCount = 0L;
        this.xBufOff = 0;
        for (int i = 0; i < this.xBuf.length; i++) {
            this.xBuf[i] = 0;
        }
    }

    protected void populateState(byte[] state) {
        System.arraycopy(this.xBuf, 0, state, 0, this.xBufOff);
        Pack.intToBigEndian(this.xBufOff, state, 4);
        Pack.longToBigEndian(this.byteCount, state, 8);
    }

    @Override
    public int getByteLength() {
        return 64;
    }
}
