package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterBlackRegionRange extends DCFilter {
    public DCFilterBlackRegionRange(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetBlackRegionRangeRange();
        this.mDefaultIndex = nativeGetBlackRegionRangeIndex();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetBlackRegionRangeIndex(i);
    }
}
