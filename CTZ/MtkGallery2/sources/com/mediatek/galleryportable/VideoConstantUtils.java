package com.mediatek.galleryportable;

import android.media.MediaPlayer;
import android.os.SystemClock;
import com.mediatek.gallerybasic.util.ExtFieldsUtils;

public class VideoConstantUtils {
    private static boolean sIsConstantExisted = false;
    private static boolean sHasChecked = false;

    private static boolean isConstantExisted() {
        if (!sHasChecked) {
            SystemClock.elapsedRealtime();
            try {
                MediaPlayer.class.getDeclaredField("MEDIA_INFO_VIDEO_NOT_PLAYING");
                MediaPlayer.class.getDeclaredField("MEDIA_INFO_AUDIO_NOT_PLAYING");
                sIsConstantExisted = true;
            } catch (NoSuchFieldException e) {
                sIsConstantExisted = false;
            }
            sHasChecked = true;
            Log.d("VP_VideoConstantUtils", "isConstantExisted, sIsConstantExisted = " + sIsConstantExisted);
        }
        return sIsConstantExisted;
    }

    public static int get(int key) {
        int value = -1;
        if (key != 860) {
            if (key == 862) {
                if (isConstantExisted()) {
                    value = 804;
                } else {
                    value = 862;
                }
            }
        } else if (isConstantExisted()) {
            value = 805;
        } else {
            value = 860;
        }
        Log.d("VP_VideoConstantUtils", "get key = " + key + ", value = " + value);
        return value;
    }

    public static String get(String key) {
        String value = null;
        if (key.equals(ExtFieldsUtils.VIDEO_ROTATION_FIELD)) {
            value = ExtFieldsUtils.VIDEO_ROTATION_FIELD;
        } else if (key.equals("is_drm")) {
            value = "is_drm";
        }
        Log.d("VP_VideoConstantUtils", "get key = " + key + ", value = " + value);
        return value;
    }
}
