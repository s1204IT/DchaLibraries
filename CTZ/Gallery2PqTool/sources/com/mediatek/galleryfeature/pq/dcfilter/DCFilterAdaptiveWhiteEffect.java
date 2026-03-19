package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterAdaptiveWhiteEffect extends DCFilter {
    public DCFilterAdaptiveWhiteEffect(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetAdaptiveWhiteEffectRange();
        this.mDefaultIndex = nativeGetAdaptiveWhiteEffectIndex();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetAdaptiveWhiteEffectIndex(i);
    }
}
