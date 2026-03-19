package com.android.se.security;

public class ResponseApdu {
    protected byte[] mData;
    protected int mSw1;
    protected int mSw2;

    public ResponseApdu(byte[] bArr) {
        this.mSw1 = 0;
        this.mSw2 = 0;
        this.mData = new byte[0];
        if (bArr.length < 2) {
            return;
        }
        if (bArr.length > 2) {
            this.mData = new byte[bArr.length - 2];
            System.arraycopy(bArr, 0, this.mData, 0, bArr.length - 2);
        }
        this.mSw1 = bArr[bArr.length - 2] & 255;
        this.mSw2 = bArr[bArr.length - 1] & 255;
    }

    public int getSW1() {
        return this.mSw1;
    }

    public int getSW2() {
        return this.mSw2;
    }

    public int getSW1SW2() {
        return (this.mSw1 << 8) | this.mSw2;
    }

    public byte[] getData() {
        return this.mData;
    }

    public boolean isStatus(int i) {
        return getSW1SW2() == i;
    }
}
