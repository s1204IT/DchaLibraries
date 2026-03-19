package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterDCChangeSpeedLevel extends DCFilter {
    public DCFilterDCChangeSpeedLevel(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetDCChangeSpeedLevelRange();
        this.mDefaultIndex = nativeGetDCChangeSpeedLevelIndex();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetDCChangeSpeedLevelIndex(i);
    }
}
