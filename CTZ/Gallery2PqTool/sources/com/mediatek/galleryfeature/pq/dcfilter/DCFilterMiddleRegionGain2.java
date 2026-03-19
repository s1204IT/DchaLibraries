package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterMiddleRegionGain2 extends DCFilter {
    public DCFilterMiddleRegionGain2(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetMiddleRegionGain2Range();
        this.mDefaultIndex = nativeGetMiddleRegionGain2Index();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetMiddleRegionGain2Index(i);
    }
}
