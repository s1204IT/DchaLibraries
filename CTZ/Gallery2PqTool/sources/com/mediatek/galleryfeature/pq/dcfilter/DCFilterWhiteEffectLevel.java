package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterWhiteEffectLevel extends DCFilter {
    public DCFilterWhiteEffectLevel(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetWhiteEffectLevelRange();
        this.mDefaultIndex = nativeGetWhiteEffectLevelIndex();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetWhiteEffectLevelIndex(i);
    }
}
