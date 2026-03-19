package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterBlackEffectLevel extends DCFilter {
    public DCFilterBlackEffectLevel(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetBlackEffectLevelRange();
        this.mDefaultIndex = nativeGetBlackEffectLevelIndex();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetBlackEffectLevelIndex(i);
    }
}
