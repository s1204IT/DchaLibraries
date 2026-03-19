package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterContentSmooth2 extends DCFilter {
    public DCFilterContentSmooth2(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetContentSmooth2Range();
        this.mDefaultIndex = nativeGetContentSmooth2Index();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetContentSmooth2Index(i);
    }
}
