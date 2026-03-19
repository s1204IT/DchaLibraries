package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterMiddleRegionGain1 extends DCFilter {
    public DCFilterMiddleRegionGain1(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetMiddleRegionGain1Range();
        this.mDefaultIndex = nativeGetMiddleRegionGain1Index();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetMiddleRegionGain1Index(i);
    }
}
