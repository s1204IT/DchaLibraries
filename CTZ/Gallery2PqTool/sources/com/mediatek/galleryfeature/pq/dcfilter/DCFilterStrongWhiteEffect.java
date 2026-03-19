package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterStrongWhiteEffect extends DCFilter {
    public DCFilterStrongWhiteEffect(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetStrongWhiteEffectRange();
        this.mDefaultIndex = nativeGetStrongWhiteEffectIndex();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetStrongWhiteEffectIndex(i);
    }
}
