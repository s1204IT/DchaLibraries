package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterScenceChangeControlEn extends DCFilter {
    public DCFilterScenceChangeControlEn(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetScenceChangeControlEnRange();
        this.mDefaultIndex = nativeGetScenceChangeControlEnIndex();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetScenceChangeControlEnIndex(i);
    }
}
