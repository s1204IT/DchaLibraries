package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterWhiteRegionRange extends DCFilter {
    public DCFilterWhiteRegionRange(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetWhiteRegionRangeRange();
        this.mDefaultIndex = nativeGetWhiteRegionRangeIndex();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetWhiteRegionRangeIndex(i);
    }
}
