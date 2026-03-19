package com.mediatek.galleryfeature.pq.filter;

public class FilterSkyToneH extends Filter {
    @Override
    public String getCurrentValue() {
        return "Sky tone(Hue):  " + Integer.toString((((this.mRange / 2) + 1) - this.mRange) + this.mCurrentIndex);
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
        this.mDefaultIndex = nativeGetSkyToneHIndex();
        this.mCurrentIndex = this.mDefaultIndex;
        this.mRange = nativeGetSkyToneHRange();
    }

    @Override
    public void setIndex(int i) {
        nativeSetSkyToneHIndex(i);
    }
}
