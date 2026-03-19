package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterDCChangeSpeedLevel2 extends DCFilter {
    public DCFilterDCChangeSpeedLevel2(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetDCChangeSpeedLevel2Range();
        this.mDefaultIndex = nativeGetDCChangeSpeedLevel2Index();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetDCChangeSpeedLevel2Index(i);
    }
}
