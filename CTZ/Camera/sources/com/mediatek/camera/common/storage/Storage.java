package com.mediatek.camera.common.storage;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.portability.SystemProperties;
import com.mediatek.camera.portability.storage.StorageManagerExt;
import java.io.File;
import java.lang.reflect.Method;

class Storage {
    private static final long CAPTURE_LOW_STORAGE_THRESHOLD;
    private static final long RECORD_LOW_STORAGE_THRESHOLD;
    private static Context sContext;
    private static Method sGetDefaultPath;
    private static String sMountPoint;
    private static Storage sStorage;
    private boolean mIsDefaultPathCanUsed = false;
    private static final LogUtil.Tag TAG = new LogUtil.Tag(Storage.class.getSimpleName());
    private static final String DCIM_CAMERA_FOLDER_ABSOLUTE_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Camera";
    private static final String FOLDER_PATH = "/" + Environment.DIRECTORY_DCIM + "/Camera";

    static {
        if (isMtkFatOnNand() || isGmoROM()) {
            CAPTURE_LOW_STORAGE_THRESHOLD = 10485760L;
            RECORD_LOW_STORAGE_THRESHOLD = 9437184L;
            LogHelper.i(TAG, "CAPTURE_LOW_STORAGE_THRESHOLD = 10485760");
        } else {
            CAPTURE_LOW_STORAGE_THRESHOLD = 52428800L;
            RECORD_LOW_STORAGE_THRESHOLD = 50331648L;
            LogHelper.i(TAG, "CAPTURE_LOW_STORAGE_THRESHOLD = 52428800");
        }
        try {
            Class<?> cls = Class.forName("com.mediatek.storage.StorageManagerEx");
            if (cls != null) {
                sGetDefaultPath = cls.getDeclaredMethod("getDefaultPath", new Class[0]);
            }
            if (sGetDefaultPath != null) {
                sGetDefaultPath.setAccessible(true);
            }
        } catch (ClassNotFoundException e) {
            LogHelper.e(TAG, "ClassNotFoundException: com.mediatek.storage.StorageManagerEx");
        } catch (NoSuchMethodException e2) {
            LogHelper.e(TAG, "NoSuchMethodException: getDefaultPath");
        }
    }

    private Storage(Context context) {
        if (isExtendStorageCanUsed()) {
            LogHelper.d(TAG, "[Storage] init internal storage");
            initializeStorageManager(context);
        }
    }

    static Storage getStorage(Context context) {
        if (sStorage == null) {
            sStorage = new Storage(context);
        }
        sContext = context;
        return sStorage;
    }

    void updateDefaultDirectory() {
        mkFileDir(getFileDirectory());
    }

    String getFileDirectory() {
        if (isExtendStorageCanUsed()) {
            return sMountPoint + FOLDER_PATH;
        }
        return DCIM_CAMERA_FOLDER_ABSOLUTE_PATH;
    }

    long getAvailableSpace() {
        if (isExtendStorageCanUsed()) {
            return getAvailableSpace(StorageManagerExt.getVolumeState(sContext, sMountPoint));
        }
        return getAvailableSpace(Environment.getExternalStorageState());
    }

    boolean isSameStorage(Intent intent) {
        if (Build.VERSION.SDK_INT >= 23 && this.mIsDefaultPathCanUsed) {
            return StorageManagerExt.isSameStorage(intent, sMountPoint);
        }
        return false;
    }

    long getRecordThreshold() {
        return RECORD_LOW_STORAGE_THRESHOLD;
    }

    long getCaptureThreshold() {
        return CAPTURE_LOW_STORAGE_THRESHOLD;
    }

    private boolean isExtendStorageCanUsed() {
        return Build.VERSION.SDK_INT >= 23 && isDefaultPathCanUsed();
    }

    private boolean isDefaultPathCanUsed() {
        if (sGetDefaultPath != null) {
            try {
                sMountPoint = StorageManagerExt.getDefaultPath();
                File file = new File(sMountPoint + FOLDER_PATH);
                file.mkdirs();
                boolean zIsDirectory = file.isDirectory();
                boolean zCanWrite = file.canWrite();
                if (!zIsDirectory || !zCanWrite) {
                    this.mIsDefaultPathCanUsed = false;
                } else {
                    this.mIsDefaultPathCanUsed = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                this.mIsDefaultPathCanUsed = false;
            }
        }
        return this.mIsDefaultPathCanUsed;
    }

    private long getAvailableSpace(String str) {
        if ("checking".equals(str)) {
            return -2L;
        }
        if (!"mounted".equals(str)) {
            return -1L;
        }
        File file = new File(getFileDirectory());
        file.mkdirs();
        if (!file.isDirectory() || !file.canWrite()) {
            return -4L;
        }
        try {
            StatFs statFs = new StatFs(getFileDirectory());
            if (Build.VERSION.SDK_INT >= 18) {
                return statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong();
            }
            return ((long) statFs.getAvailableBlocks()) * ((long) statFs.getBlockSize());
        } catch (IllegalArgumentException e) {
            LogHelper.e(TAG, "Fail to access external storage", e);
            return -3L;
        }
    }

    private void initializeStorageManager(Context context) {
        StorageManagerExt.initStorageManager(context);
    }

    private static void mkFileDir(String str) {
        File file = new File(str);
        if (!file.exists()) {
            LogHelper.d(TAG, "dir not exit,will create this, path = " + str);
            file.mkdirs();
        }
    }

    private static boolean isGmoROM() {
        boolean z = SystemProperties.getInt("ro.vendor.gmo.rom_optimize", 0) == 1;
        LogHelper.d(TAG, "isGmoRom() return " + z);
        return z;
    }

    private static boolean isMtkFatOnNand() {
        boolean z = SystemProperties.getInt("ro.vendor.mtk_fat_on_nand", 0) == 1;
        LogHelper.d(TAG, "isMtkFatOnNand() return " + z);
        return z;
    }
}
