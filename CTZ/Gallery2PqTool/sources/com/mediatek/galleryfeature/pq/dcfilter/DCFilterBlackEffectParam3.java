package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterBlackEffectParam3 extends DCFilter {
    public DCFilterBlackEffectParam3(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetBlackEffectParam3Range();
        this.mDefaultIndex = nativeGetBlackEffectParam3Index();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetBlackEffectParam3Index(i);
    }
}
