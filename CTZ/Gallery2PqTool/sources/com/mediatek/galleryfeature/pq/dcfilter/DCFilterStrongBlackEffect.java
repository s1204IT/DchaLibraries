package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterStrongBlackEffect extends DCFilter {
    public DCFilterStrongBlackEffect(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetStrongBlackEffectRange();
        this.mDefaultIndex = nativeGetStrongBlackEffectIndex();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetStrongBlackEffectIndex(i);
    }
}
