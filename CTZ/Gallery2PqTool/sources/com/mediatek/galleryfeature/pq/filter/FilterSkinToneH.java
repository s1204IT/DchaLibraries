package com.mediatek.galleryfeature.pq.filter;

public class FilterSkinToneH extends Filter {
    @Override
    public String getCurrentValue() {
        return "Skin tone(Hue):  " + ((((this.mRange / 2) + 1) - this.mRange) + this.mCurrentIndex);
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
    public void setIndex(int i) {
        nativeSetSkinToneHIndex(i);
    }

    @Override
    public void init() {
        this.mDefaultIndex = nativeGetSkinToneHIndex();
        this.mCurrentIndex = this.mDefaultIndex;
        this.mRange = nativeGetSkinToneHRange();
    }
}
