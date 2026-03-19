package com.mediatek.galleryfeature.pq.filter;

public class FilterContrastAdj extends Filter {
    @Override
    public String getCurrentValue() {
        return "Contrast:  " + super.getCurrentValue();
    }

    @Override
    public void init() {
        this.mDefaultIndex = nativeGetContrastAdjIndex();
        this.mCurrentIndex = this.mDefaultIndex;
        this.mRange = nativeGetContrastAdjRange();
    }

    @Override
    public void setIndex(int i) {
        nativeSetContrastAdjIndex(i);
    }
}
