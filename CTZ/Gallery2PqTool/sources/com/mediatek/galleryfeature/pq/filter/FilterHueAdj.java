package com.mediatek.galleryfeature.pq.filter;

public class FilterHueAdj extends Filter {
    @Override
    public String getCurrentValue() {
        return "0";
    }

    @Override
    public String getMaxValue() {
        return "0";
    }

    @Override
    public String getMinValue() {
        return "0";
    }

    @Override
    public String getSeekbarProgressValue() {
        return "0";
    }

    @Override
    public void init() {
        this.mRange = nativeGetHueAdjRange();
        this.mDefaultIndex = nativeGetHueAdjIndex();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setCurrentIndex(int i) {
    }

    @Override
    public void setIndex(int i) {
        nativeSetHueAdjIndex(0);
    }
}
