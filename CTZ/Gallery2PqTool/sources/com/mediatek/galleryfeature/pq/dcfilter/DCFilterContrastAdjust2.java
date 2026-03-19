package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterContrastAdjust2 extends DCFilter {
    public DCFilterContrastAdjust2(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetContrastAdjust2Range();
        this.mDefaultIndex = nativeGetContrastAdjust2Index();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetContrastAdjust2Index(i);
    }
}
