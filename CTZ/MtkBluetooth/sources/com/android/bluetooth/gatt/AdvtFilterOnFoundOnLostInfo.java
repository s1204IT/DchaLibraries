package com.android.bluetooth.gatt;

public class AdvtFilterOnFoundOnLostInfo {
    private int mAddrType;
    private String mAddress;
    private int mAdvInfoPresent;
    private byte[] mAdvPkt;
    private int mAdvPktLen;
    private int mAdvState;
    private int mClientIf;
    private int mFiltIndex;
    private int mRssiValue;
    private byte[] mScanRsp;
    private int mScanRspLen;
    private int mTimeStamp;
    private int mTxPower;

    public AdvtFilterOnFoundOnLostInfo(int i, int i2, byte[] bArr, int i3, byte[] bArr2, int i4, int i5, int i6, String str, int i7, int i8, int i9, int i10) {
        this.mClientIf = i;
        this.mAdvPktLen = i2;
        this.mAdvPkt = bArr;
        this.mScanRspLen = i3;
        this.mScanRsp = bArr2;
        this.mFiltIndex = i4;
        this.mAdvState = i5;
        this.mAdvInfoPresent = i6;
        this.mAddress = str;
        this.mAddrType = i7;
        this.mTxPower = i8;
        this.mRssiValue = i9;
        this.mTimeStamp = i10;
    }

    public int getClientIf() {
        return this.mClientIf;
    }

    public int getFiltIndex() {
        return this.mFiltIndex;
    }

    public int getAdvState() {
        return this.mAdvState;
    }

    public int getTxPower() {
        return this.mTxPower;
    }

    public int getTimeStamp() {
        return this.mTimeStamp;
    }

    public int getRSSIValue() {
        return this.mRssiValue;
    }

    public int getAdvInfoPresent() {
        return this.mAdvInfoPresent;
    }

    public String getAddress() {
        return this.mAddress;
    }

    public int getAddressType() {
        return this.mAddrType;
    }

    public byte[] getAdvPacketData() {
        return this.mAdvPkt;
    }

    public int getAdvPacketLen() {
        return this.mAdvPktLen;
    }

    public byte[] getScanRspData() {
        return this.mScanRsp;
    }

    public int getScanRspLen() {
        return this.mScanRspLen;
    }

    public byte[] getResult() {
        byte[] bArr = new byte[this.mAdvPkt.length + (this.mScanRsp != null ? this.mScanRsp.length : 0)];
        System.arraycopy(this.mAdvPkt, 0, bArr, 0, this.mAdvPkt.length);
        if (this.mScanRsp != null) {
            System.arraycopy(this.mScanRsp, 0, bArr, this.mAdvPkt.length, this.mScanRsp.length);
        }
        return bArr;
    }
}
