package com.android.se.security;

public class CommandApdu {
    protected int mCla;
    protected byte[] mData;
    protected int mIns;
    protected int mLc;
    protected int mLe;
    protected boolean mLeUsed;
    protected int mP1;
    protected int mP2;

    public CommandApdu(int i, int i2, int i3, int i4) {
        this.mCla = 0;
        this.mIns = 0;
        this.mP1 = 0;
        this.mP2 = 0;
        this.mLc = 0;
        this.mData = new byte[0];
        this.mLe = 0;
        this.mLeUsed = false;
        this.mCla = i;
        this.mIns = i2;
        this.mP1 = i3;
        this.mP2 = i4;
    }

    public CommandApdu() {
        this.mCla = 0;
        this.mIns = 0;
        this.mP1 = 0;
        this.mP2 = 0;
        this.mLc = 0;
        this.mData = new byte[0];
        this.mLe = 0;
        this.mLeUsed = false;
    }

    public CommandApdu(int i, int i2, int i3, int i4, byte[] bArr) {
        this.mCla = 0;
        this.mIns = 0;
        this.mP1 = 0;
        this.mP2 = 0;
        this.mLc = 0;
        this.mData = new byte[0];
        this.mLe = 0;
        this.mLeUsed = false;
        this.mCla = i;
        this.mIns = i2;
        this.mLc = bArr.length;
        this.mP1 = i3;
        this.mP2 = i4;
        this.mData = bArr;
    }

    public CommandApdu(int i, int i2, int i3, int i4, byte[] bArr, int i5) {
        this.mCla = 0;
        this.mIns = 0;
        this.mP1 = 0;
        this.mP2 = 0;
        this.mLc = 0;
        this.mData = new byte[0];
        this.mLe = 0;
        this.mLeUsed = false;
        this.mCla = i;
        this.mIns = i2;
        this.mLc = bArr.length;
        this.mP1 = i3;
        this.mP2 = i4;
        this.mData = bArr;
        this.mLe = i5;
        this.mLeUsed = true;
    }

    public CommandApdu(int i, int i2, int i3, int i4, int i5) {
        this.mCla = 0;
        this.mIns = 0;
        this.mP1 = 0;
        this.mP2 = 0;
        this.mLc = 0;
        this.mData = new byte[0];
        this.mLe = 0;
        this.mLeUsed = false;
        this.mCla = i;
        this.mIns = i2;
        this.mP1 = i3;
        this.mP2 = i4;
        this.mLe = i5;
        this.mLeUsed = true;
    }

    public static boolean compareHeaders(byte[] bArr, byte[] bArr2, byte[] bArr3) {
        if (bArr.length < 4 || bArr3.length < 4) {
            return false;
        }
        byte[] bArr4 = {(byte) (bArr[0] & bArr2[0]), (byte) (bArr[1] & bArr2[1]), (byte) (bArr[2] & bArr2[2]), (byte) (bArr[3] & bArr2[3])};
        return bArr4[0] == bArr3[0] && bArr4[1] == bArr3[1] && bArr4[2] == bArr3[2] && bArr4[3] == bArr3[3];
    }

    public int getP1() {
        return this.mP1;
    }

    public void setP1(int i) {
        this.mP1 = i;
    }

    public int getP2() {
        return this.mP2;
    }

    public void setP2(int i) {
        this.mP2 = i;
    }

    public int getLc() {
        return this.mLc;
    }

    public byte[] getData() {
        return this.mData;
    }

    public void setData(byte[] bArr) {
        this.mLc = bArr.length;
        this.mData = bArr;
    }

    public int getLe() {
        return this.mLe;
    }

    public void setLe(int i) {
        this.mLe = i;
        this.mLeUsed = true;
    }

    public byte[] toBytes() {
        int length;
        int length2 = 4;
        if (this.mData.length != 0) {
            length = this.mData.length + 5;
        } else {
            length = 4;
        }
        if (this.mLeUsed) {
            length++;
        }
        byte[] bArr = new byte[length];
        bArr[0] = (byte) this.mCla;
        bArr[1] = (byte) this.mIns;
        bArr[2] = (byte) this.mP1;
        bArr[3] = (byte) this.mP2;
        if (this.mData.length != 0) {
            bArr[4] = (byte) this.mLc;
            System.arraycopy(this.mData, 0, bArr, 5, this.mData.length);
            length2 = this.mData.length + 5;
        }
        if (this.mLeUsed) {
            bArr[length2] = (byte) (bArr[length2] + ((byte) this.mLe));
        }
        return bArr;
    }

    public CommandApdu m2clone() {
        CommandApdu commandApdu = new CommandApdu();
        commandApdu.mCla = this.mCla;
        commandApdu.mIns = this.mIns;
        commandApdu.mP1 = this.mP1;
        commandApdu.mP2 = this.mP2;
        commandApdu.mLc = this.mLc;
        commandApdu.mData = new byte[this.mData.length];
        System.arraycopy(this.mData, 0, commandApdu.mData, 0, this.mData.length);
        commandApdu.mLe = this.mLe;
        commandApdu.mLeUsed = this.mLeUsed;
        return commandApdu;
    }
}
