package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterBlackRegionGain1 extends DCFilter {
    public DCFilterBlackRegionGain1(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetBlackRegionGain1Range();
        this.mDefaultIndex = nativeGetBlackRegionGain1Index();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetBlackRegionGain1Index(i);
    }
}
