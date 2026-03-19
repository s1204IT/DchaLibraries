package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterWhiteRegionGain1 extends DCFilter {
    public DCFilterWhiteRegionGain1(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetWhiteRegionGain1Range();
        this.mDefaultIndex = nativeGetWhiteRegionGain1Index();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetWhiteRegionGain1Index(i);
    }
}
