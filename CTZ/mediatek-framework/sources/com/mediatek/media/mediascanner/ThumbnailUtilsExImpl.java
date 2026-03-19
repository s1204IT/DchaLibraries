package com.mediatek.media.mediascanner;

import android.graphics.BitmapFactory;
import com.mediatek.mmsdk.callback.MmsdkCallbackClient;

public class ThumbnailUtilsExImpl extends ThumbnailUtilsEx {
    public void correctOptions(String str, BitmapFactory.Options options) {
        if (str.endsWith(".dcf")) {
            options.inSampleSize |= MmsdkCallbackClient.CAMERA_MSG_COMPRESSED_IMAGE;
        }
    }
}
