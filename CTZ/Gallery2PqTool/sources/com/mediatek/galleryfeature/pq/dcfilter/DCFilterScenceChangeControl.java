package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterScenceChangeControl extends DCFilter {
    public DCFilterScenceChangeControl(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetScenceChangeControlRange();
        this.mDefaultIndex = nativeGetScenceChangeControlIndex();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetScenceChangeControlIndex(i);
    }
}
