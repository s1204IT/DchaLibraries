package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterAdaptiveBlackEffect extends DCFilter {
    public DCFilterAdaptiveBlackEffect(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetAdaptiveBlackEffectRange();
        this.mDefaultIndex = nativeGetAdaptiveBlackEffectIndex();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetAdaptiveBlackEffectIndex(i);
    }
}
