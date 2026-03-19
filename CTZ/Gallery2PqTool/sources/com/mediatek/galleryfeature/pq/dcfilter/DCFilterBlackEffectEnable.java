package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterBlackEffectEnable extends DCFilter {
    public DCFilterBlackEffectEnable(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetBlackEffectEnableRange();
        this.mDefaultIndex = nativeGetBlackEffectEnableIndex();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetBlackEffectEnableIndex(i);
    }
}
