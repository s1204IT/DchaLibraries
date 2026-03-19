package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterWhiteEffectParam4 extends DCFilter {
    public DCFilterWhiteEffectParam4(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetWhiteEffectParam4Range();
        this.mDefaultIndex = nativeGetWhiteEffectParam4Index();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetWhiteEffectParam4Index(i);
    }
}
