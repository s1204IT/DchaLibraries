package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterWhiteEffectParam3 extends DCFilter {
    public DCFilterWhiteEffectParam3(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetWhiteEffectParam3Range();
        this.mDefaultIndex = nativeGetWhiteEffectParam3Index();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetWhiteEffectParam3Index(i);
    }
}
