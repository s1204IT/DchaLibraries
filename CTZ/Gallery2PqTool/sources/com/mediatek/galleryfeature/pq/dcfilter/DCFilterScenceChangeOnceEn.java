package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterScenceChangeOnceEn extends DCFilter {
    public DCFilterScenceChangeOnceEn(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetScenceChangeOnceEnRange();
        this.mDefaultIndex = nativeGetScenceChangeOnceEnIndex();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetScenceChangeOnceEnIndex(i);
    }
}
