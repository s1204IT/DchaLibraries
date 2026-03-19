package com.mediatek.galleryfeature.pq.filter;

public class FilterSatAdj extends Filter {
    @Override
    public String getCurrentValue() {
        return "GlobalSat:  " + super.getCurrentValue();
    }

    @Override
    public void setIndex(int i) {
        nativeSetSatAdjIndex(i);
    }

    @Override
    public void init() {
        this.mDefaultIndex = nativeGetSatAdjIndex();
        this.mCurrentIndex = this.mDefaultIndex;
        this.mRange = nativeGetSatAdjRange();
    }
}
