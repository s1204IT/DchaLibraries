package com.mediatek.gallerybasic.util;

import com.mediatek.gallerybasic.base.MediaData;

public class ExtFieldsUtils {
    public static final int INVALID_VIDEO_ROTATION = -1;
    public static final int NOT_SUPPORT_VIDEO_ROTATION = -2;
    private static final String TAG = "MtkGallery2/ExtFieldsUtils";
    public static final String VIDEO_ROTATION_FIELD = "orientation";

    public static int getVideoRotation(MediaData mediaData) {
        if (mediaData == null) {
            Log.d(TAG, "<getVideoRotation> data is null, return -1");
            return -1;
        }
        if (mediaData.extFileds == null) {
            Log.d(TAG, "<getVideoRotation> extFileds is null, return -1");
            return -1;
        }
        ?? videoField = mediaData.extFileds.getVideoField(VIDEO_ROTATION_FIELD);
        if (videoField == 0) {
            Log.d(TAG, "<getVideoRotation> not support video rotation, return -2");
            return -2;
        }
        if (videoField instanceof Integer) {
            int iIntValue = videoField.intValue();
            Log.d(TAG, "<getVideoRotation> file: " + mediaData.filePath + ", rotation: " + iIntValue);
            return iIntValue;
        }
        Log.d(TAG, "<getVideoRotation> incorrect format, return -1");
        return -1;
    }
}
