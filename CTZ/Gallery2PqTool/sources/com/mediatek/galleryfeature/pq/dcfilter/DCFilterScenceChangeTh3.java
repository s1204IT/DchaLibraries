package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterScenceChangeTh3 extends DCFilter {
    public DCFilterScenceChangeTh3(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetScenceChangeTh3Range();
        this.mDefaultIndex = nativeGetScenceChangeTh3Index();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetScenceChangeTh3Index(i);
    }
}
