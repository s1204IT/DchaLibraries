package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterContentSmooth3 extends DCFilter {
    public DCFilterContentSmooth3(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetContentSmooth3Range();
        this.mDefaultIndex = nativeGetContentSmooth3Index();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetContentSmooth3Index(i);
    }
}
