package com.mediatek.galleryfeature.pq.filter;

public class FilterGrassToneH extends Filter {
    @Override
    public String getCurrentValue() {
        return "Grass tone(Hue):  " + (((this.mCurrentIndex + (this.mRange / 2)) + 1) - this.mRange);
    }

    @Override
    public String getMaxValue() {
        return Integer.toString((this.mRange - 1) / 2);
    }

    @Override
    public String getMinValue() {
        return Integer.toString(((this.mRange / 2) + 1) - this.mRange);
    }

    @Override
    public void init() {
        this.mDefaultIndex = nativeGetGrassToneHIndex();
        this.mCurrentIndex = this.mDefaultIndex;
        this.mRange = nativeGetGrassToneHRange();
    }

    @Override
    public void setIndex(int i) {
        nativeSetGrassToneHIndex(i);
    }
}
