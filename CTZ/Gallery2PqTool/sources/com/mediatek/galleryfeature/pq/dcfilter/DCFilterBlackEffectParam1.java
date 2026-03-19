package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterBlackEffectParam1 extends DCFilter {
    public DCFilterBlackEffectParam1(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetBlackEffectParam1Range();
        this.mDefaultIndex = nativeGetBlackEffectParam1Index();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetBlackEffectParam1Index(i);
    }
}
