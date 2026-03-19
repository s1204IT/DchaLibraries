package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterBlackEffectParam2 extends DCFilter {
    public DCFilterBlackEffectParam2(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetBlackEffectParam2Range();
        this.mDefaultIndex = nativeGetBlackEffectParam2Index();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetBlackEffectParam2Index(i);
    }
}
