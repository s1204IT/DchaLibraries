package com.mediatek.internal.telephony.uicc;

class IccFileRequest {
    public int mAppType;
    public byte[] mData;
    public String mEfPath;
    public int mEfType;
    public int mEfid;
    public String mKey = null;
    public String mPin2;
    public int mRecordNum;

    public IccFileRequest(int i, int i2, int i3, String str, byte[] bArr, int i4, String str2) {
        this.mEfid = i;
        this.mEfType = i2;
        this.mAppType = i3;
        this.mEfPath = str;
        this.mData = bArr;
        this.mRecordNum = i4;
        this.mPin2 = str2;
    }
}
