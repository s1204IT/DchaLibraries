package com.mediatek.gallerybasic.platform;

import com.mediatek.gallerybasic.base.MediaData;
import com.mediatek.gallerybasic.base.Work;

public class PlatformHelper {
    private static final float SCALE_LIMIT = 4.0f;
    private static final String TAG = "MtkGallery2/PlatformHelper";
    private static Platform sPlatform;

    public static void setPlatform(Platform platform) {
        sPlatform = platform;
    }

    public static boolean isOutOfDecodeSpec(long j, int i, int i2, String str) {
        if (sPlatform != null) {
            return sPlatform.isOutOfDecodeSpec(j, i, i2, str);
        }
        return false;
    }

    public static void submitJob(Work work) {
        if (sPlatform != null) {
            sPlatform.submitJob(work);
        }
    }

    public static float getMinScaleLimit(MediaData mediaData) {
        if (sPlatform != null) {
            return sPlatform.getMinScaleLimit(mediaData);
        }
        return 4.0f;
    }
}
