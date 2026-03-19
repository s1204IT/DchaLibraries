package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterBlackEffectParam4 extends DCFilter {
    public DCFilterBlackEffectParam4(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetBlackEffectParam4Range();
        this.mDefaultIndex = nativeGetBlackEffectParam4Index();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetBlackEffectParam4Index(i);
    }
}
