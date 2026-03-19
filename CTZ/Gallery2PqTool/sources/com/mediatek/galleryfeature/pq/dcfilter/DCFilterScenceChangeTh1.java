package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterScenceChangeTh1 extends DCFilter {
    public DCFilterScenceChangeTh1(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetScenceChangeTh1Range();
        this.mDefaultIndex = nativeGetScenceChangeTh1Index();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetScenceChangeTh1Index(i);
    }
}
