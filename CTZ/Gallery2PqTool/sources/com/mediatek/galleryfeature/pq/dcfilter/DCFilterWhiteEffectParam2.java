package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterWhiteEffectParam2 extends DCFilter {
    public DCFilterWhiteEffectParam2(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetWhiteEffectParam2Range();
        this.mDefaultIndex = nativeGetWhiteEffectParam2Index();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetWhiteEffectParam2Index(i);
    }
}
