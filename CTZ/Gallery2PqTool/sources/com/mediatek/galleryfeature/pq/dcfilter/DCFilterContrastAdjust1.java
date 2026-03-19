package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterContrastAdjust1 extends DCFilter {
    public DCFilterContrastAdjust1(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetContrastAdjust1Range();
        this.mDefaultIndex = nativeGetContrastAdjust1Index();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetContrastAdjust1Index(i);
    }
}
