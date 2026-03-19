package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterWhiteEffectEnable extends DCFilter {
    public DCFilterWhiteEffectEnable(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetWhiteEffectEnableRange();
        this.mDefaultIndex = nativeGetWhiteEffectEnableIndex();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetWhiteEffectEnableIndex(i);
    }
}
