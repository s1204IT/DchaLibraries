package com.mediatek.galleryfeature.pq.filter;

public class FilterSkyToneS extends Filter {
    @Override
    public String getCurrentValue() {
        return "Sky tone(Sat):  " + super.getCurrentValue();
    }

    @Override
    public void init() {
        this.mRange = nativeGetSkyToneSRange();
        this.mDefaultIndex = nativeGetSkyToneSIndex();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetSkyToneSIndex(i);
    }
}
