package com.mediatek.gallerybasic.platform;

import com.mediatek.gallerybasic.base.MediaData;
import com.mediatek.gallerybasic.base.Work;

public interface Platform {
    float getMinScaleLimit(MediaData mediaData);

    boolean isOutOfDecodeSpec(long j, int i, int i2, String str);

    void submitJob(Work work);
}
