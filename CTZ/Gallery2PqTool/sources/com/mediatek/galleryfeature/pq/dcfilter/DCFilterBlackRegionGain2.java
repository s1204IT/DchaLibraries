package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterBlackRegionGain2 extends DCFilter {
    public DCFilterBlackRegionGain2(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetBlackRegionGain2Range();
        this.mDefaultIndex = nativeGetBlackRegionGain2Index();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetBlackRegionGain2Index(i);
    }
}
