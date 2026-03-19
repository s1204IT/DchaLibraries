package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterProtectRegionWeight extends DCFilter {
    public DCFilterProtectRegionWeight(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetProtectRegionWeightRange();
        this.mDefaultIndex = nativeGetProtectRegionWeightIndex();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetProtectRegionWeightIndex(i);
    }
}
