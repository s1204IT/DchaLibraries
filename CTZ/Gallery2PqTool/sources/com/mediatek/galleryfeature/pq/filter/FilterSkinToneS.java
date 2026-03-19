package com.mediatek.galleryfeature.pq.filter;

public class FilterSkinToneS extends Filter {
    @Override
    public String getCurrentValue() {
        return "Skin tone(Sat):  " + super.getCurrentValue();
    }

    @Override
    public void init() {
        this.mDefaultIndex = nativeGetSkinToneSIndex();
        this.mCurrentIndex = this.mDefaultIndex;
        this.mRange = nativeGetSkinToneSRange();
    }

    @Override
    public void setIndex(int i) {
        nativeSetSkinToneSIndex(i);
    }

    @Override
    public String getSeekbarProgressValue() {
        return Integer.toString(this.mCurrentIndex);
    }
}
