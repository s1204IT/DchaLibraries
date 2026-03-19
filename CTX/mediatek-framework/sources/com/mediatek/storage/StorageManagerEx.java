package com.mediatek.storage;

import android.os.Environment;
import android.os.SystemProperties;
import android.util.Log;
import java.io.File;

public class StorageManagerEx {
    private static final String DIR_ANDROID = "Android";
    private static final String DIR_CACHE = "cache";
    private static final String DIR_DATA = "data";
    private static final String PROP_DEVICE_TABLET = "tablet";
    private static final String PROP_DEVICE_TYPE = "ro.build.characteristics";
    private static final String PROP_SD_DEFAULT_PATH = "persist.vendor.sys.sd.defaultpath";
    private static final String PROP_SD_EXTERNAL_PATH = "vold.path.external_sd";
    private static final String PROP_SD_SWAP = "vold.swap.state";
    private static final String PROP_SD_SWAP_FALSE = "0";
    private static final String PROP_SD_SWAP_TRUE = "1";
    private static final String STORAGE_PATH_EMULATED = "/storage/emulated/";
    private static final String STORAGE_PATH_SD1 = "/storage/sdcard0";
    private static final String STORAGE_PATH_SD1_ICS = "/mnt/sdcard";
    private static final String STORAGE_PATH_SD2 = "/storage/sdcard1";
    private static final String STORAGE_PATH_SD2_ICS = "/mnt/sdcard2";
    private static final String STORAGE_PATH_SHARE_SD = "/storage/emulated/0";
    private static final String TAG = "StorageManagerEx";

    public static String getDefaultPath() {
        String absolutePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        Log.i(TAG, " Default path taken as primary storage, path=" + absolutePath);
        return absolutePath;
    }

    public static void setDefaultPath(String str) {
        Log.i(TAG, "setDefaultPath path=" + str);
        if (str == null) {
            Log.e(TAG, "setDefaultPath error! path=null");
            return;
        }
        try {
            SystemProperties.set(PROP_SD_DEFAULT_PATH, str);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "IllegalArgumentException when set default path:" + e);
        }
    }

    public static File getExternalCacheDir(String str) {
        if (str == null) {
            Log.w(TAG, "packageName = null!");
            return null;
        }
        File fileBuildPath = Environment.buildPath(new File(getDefaultPath()), new String[]{DIR_ANDROID, DIR_DATA, str, DIR_CACHE});
        Log.d(TAG, "getExternalCacheDir path = " + fileBuildPath);
        return fileBuildPath;
    }

    public static String getExternalStoragePath() {
        String str;
        IllegalArgumentException e;
        try {
            str = SystemProperties.get(PROP_SD_EXTERNAL_PATH);
            try {
                Log.i(TAG, "getExternalStoragePath path=" + str);
            } catch (IllegalArgumentException e2) {
                e = e2;
                Log.e(TAG, "IllegalArgumentException when getExternalStoragePath:" + e);
            }
        } catch (IllegalArgumentException e3) {
            str = null;
            e = e3;
        }
        Log.d(TAG, "getExternalStoragePath path=" + str);
        return str;
    }

    public static String getInternalStoragePath() {
        Log.d(TAG, "getInternalStoragePath path= null");
        return null;
    }

    public static String getInternalStoragePathForLogger() {
        String internalStoragePath = getInternalStoragePath();
        Log.i(TAG, "getInternalStoragePathForLogger raw path=" + internalStoragePath);
        if (internalStoragePath != null && internalStoragePath.startsWith(STORAGE_PATH_EMULATED)) {
            internalStoragePath = STORAGE_PATH_SHARE_SD;
        }
        Log.i(TAG, "getInternalStoragePathForLogger path=" + internalStoragePath);
        return internalStoragePath;
    }
}
