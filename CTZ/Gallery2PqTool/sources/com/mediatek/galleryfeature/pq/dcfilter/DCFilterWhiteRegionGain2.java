package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterWhiteRegionGain2 extends DCFilter {
    public DCFilterWhiteRegionGain2(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetWhiteRegionGain2Range();
        this.mDefaultIndex = nativeGetWhiteRegionGain2Index();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetWhiteRegionGain2Index(i);
    }
}
