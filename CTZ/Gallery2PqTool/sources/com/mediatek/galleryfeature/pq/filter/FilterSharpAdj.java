package com.mediatek.galleryfeature.pq.filter;

public class FilterSharpAdj extends Filter {
    @Override
    public String getCurrentValue() {
        return "Sharpness:  " + super.getCurrentValue();
    }

    @Override
    public void setIndex(int i) {
        nativeSetSharpAdjIndex(i);
    }

    @Override
    public void init() {
        this.mDefaultIndex = nativeGetSharpAdjIndex();
        this.mCurrentIndex = this.mDefaultIndex;
        this.mRange = nativeGetSharpAdjRange();
    }
}
