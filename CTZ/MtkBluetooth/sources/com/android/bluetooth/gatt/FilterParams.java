package com.android.bluetooth.gatt;

public class FilterParams {
    private int mClientIf;
    private int mDelyMode;
    private int mFeatSeln;
    private int mFiltIndex;
    private int mFiltLogicType;
    private int mFoundTimeOut;
    private int mFoundTimeOutCnt;
    private int mListLogicType;
    private int mLostTimeOut;
    private int mNumOfTrackEntries;
    private int mRssiHighValue;
    private int mRssiLowValue;

    public FilterParams(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10, int i11, int i12) {
        this.mClientIf = i;
        this.mFiltIndex = i2;
        this.mFeatSeln = i3;
        this.mListLogicType = i4;
        this.mFiltLogicType = i5;
        this.mRssiHighValue = i6;
        this.mRssiLowValue = i7;
        this.mDelyMode = i8;
        this.mFoundTimeOut = i9;
        this.mLostTimeOut = i10;
        this.mFoundTimeOutCnt = i11;
        this.mNumOfTrackEntries = i12;
    }

    public int getClientIf() {
        return this.mClientIf;
    }

    public int getFiltIndex() {
        return this.mFiltIndex;
    }

    public int getFeatSeln() {
        return this.mFeatSeln;
    }

    public int getDelyMode() {
        return this.mDelyMode;
    }

    public int getListLogicType() {
        return this.mListLogicType;
    }

    public int getFiltLogicType() {
        return this.mFiltLogicType;
    }

    public int getRSSIHighValue() {
        return this.mRssiHighValue;
    }

    public int getRSSILowValue() {
        return this.mRssiLowValue;
    }

    public int getFoundTimeout() {
        return this.mFoundTimeOut;
    }

    public int getFoundTimeOutCnt() {
        return this.mFoundTimeOutCnt;
    }

    public int getLostTimeout() {
        return this.mLostTimeOut;
    }

    public int getNumOfTrackEntries() {
        return this.mNumOfTrackEntries;
    }
}
