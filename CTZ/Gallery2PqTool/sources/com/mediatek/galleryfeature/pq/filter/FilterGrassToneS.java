package com.mediatek.galleryfeature.pq.filter;

public class FilterGrassToneS extends Filter {
    @Override
    public String getCurrentValue() {
        return "Grass tone(Sat):  " + super.getCurrentValue();
    }

    @Override
    public void init() {
        this.mDefaultIndex = nativeGetGrassToneSIndex();
        this.mCurrentIndex = this.mDefaultIndex;
        this.mRange = nativeGetGrassToneSRange();
    }

    @Override
    public void setIndex(int i) {
        nativeSetGrassToneSIndex(i);
    }
}
