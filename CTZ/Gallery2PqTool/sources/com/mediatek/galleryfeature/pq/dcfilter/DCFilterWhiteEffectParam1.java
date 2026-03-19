package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterWhiteEffectParam1 extends DCFilter {
    public DCFilterWhiteEffectParam1(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetWhiteEffectParam1Range();
        this.mDefaultIndex = nativeGetWhiteEffectParam1Index();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetWhiteEffectParam1Index(i);
    }
}
