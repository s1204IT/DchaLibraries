package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterScenceChangeTh2 extends DCFilter {
    public DCFilterScenceChangeTh2(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetScenceChangeTh2Range();
        this.mDefaultIndex = nativeGetScenceChangeTh2Index();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetScenceChangeTh2Index(i);
    }
}
