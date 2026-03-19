package com.mediatek.galleryfeature.pq.dcfilter;

public class DCFilterProtectRegionEffect extends DCFilter {
    public DCFilterProtectRegionEffect(String str) {
        super(str);
    }

    @Override
    public void init() {
        this.mRange = nativeGetProtectRegionEffectRange();
        this.mDefaultIndex = nativeGetProtectRegionEffectIndex();
        this.mCurrentIndex = this.mDefaultIndex;
    }

    @Override
    public void setIndex(int i) {
        nativeSetProtectRegionEffectIndex(i);
    }
}
