package com.android.camera.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.os.Build;
import android.preference.PreferenceManager;
import com.android.ex.camera2.portability.Size;
import java.util.List;

public class CameraPictureSizesCacher {
    public static final String PICTURE_SIZES_BUILD_KEY = "CachedSupportedPictureSizes_Build_Camera";
    public static final String PICTURE_SIZES_SIZES_KEY = "CachedSupportedPictureSizes_Sizes_Camera";

    public static void updateSizesForCamera(Context context, int cameraId, List<Size> sizes) {
        String key_build = PICTURE_SIZES_BUILD_KEY + cameraId;
        SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String thisCameraCachedBuild = defaultPrefs.getString(key_build, null);
        if (thisCameraCachedBuild == null) {
            String key_sizes = PICTURE_SIZES_SIZES_KEY + cameraId;
            SharedPreferences.Editor editor = defaultPrefs.edit();
            editor.putString(key_build, Build.DISPLAY);
            editor.putString(key_sizes, Size.listToString(sizes));
            editor.apply();
        }
    }

    public static List<Size> getSizesForCamera(int cameraId, Context context) {
        String thisCameraCachedSizeList;
        String key_build = PICTURE_SIZES_BUILD_KEY + cameraId;
        String key_sizes = PICTURE_SIZES_SIZES_KEY + cameraId;
        SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String thisCameraCachedBuild = defaultPrefs.getString(key_build, null);
        if (thisCameraCachedBuild != null && thisCameraCachedBuild.equals(Build.DISPLAY) && (thisCameraCachedSizeList = defaultPrefs.getString(key_sizes, null)) != null) {
            return Size.stringToList(thisCameraCachedSizeList);
        }
        try {
            Camera thisCamera = Camera.open(cameraId);
            if (thisCamera == null) {
                return null;
            }
            List<Size> sizes = Size.buildListFromCameraSizes(thisCamera.getParameters().getSupportedPictureSizes());
            thisCamera.release();
            SharedPreferences.Editor editor = defaultPrefs.edit();
            editor.putString(key_build, Build.DISPLAY);
            editor.putString(key_sizes, Size.listToString(sizes));
            editor.apply();
            return sizes;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
