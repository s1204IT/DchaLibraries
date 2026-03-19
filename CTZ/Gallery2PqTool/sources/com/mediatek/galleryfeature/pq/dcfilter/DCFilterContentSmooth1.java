package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterContentSmooth1 extends DCFilter {
    public DCFilterContentSmooth1(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetContentSmooth1Range();
        this.mDefaultIndex = nativeGetContentSmooth1Index();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetContentSmooth1Index(i);
    }
}
