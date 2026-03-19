package com.mediatek.galleryfeature.pq.filter;

public class FilterGetXAxis extends Filter {
    @Override
    public String getMaxValue() {
        return "-1";
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
        this.mRange = nativeGetXAxisRange();
        this.mDefaultIndex = nativeGetXAxisIndex();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetXAxisIndex(i);
    }
}
