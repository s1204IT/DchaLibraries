package com.mediatek.callrecorder;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.util.Slog;
import java.io.File;

public final class RecorderUtils {
    private static final String TAG = RecorderUtils.class.getSimpleName();

    public static boolean diskSpaceAvailable(long j) {
        return getDiskAvailableSize() - j > 0;
    }

    public static boolean diskSpaceAvailable(String str) {
        Slog.d(TAG, "defaultPath = " + str);
        if (str == null) {
            return diskSpaceAvailable(2097152L);
        }
        File file = new File(str);
        try {
            if (!file.exists() || !file.isDirectory()) {
                return false;
            }
            StatFs statFs = new StatFs(file.getPath());
            long blockSize = ((long) statFs.getBlockSize()) * ((long) statFs.getAvailableBlocks());
            Slog.d(TAG, "totalSize = " + blockSize);
            return blockSize - 2097152 > 0;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean isExternalStorageMounted(Context context) {
        if (((StorageManager) context.getSystemService("storage")) == null) {
            return false;
        }
        String externalStorageState = Environment.getExternalStorageState();
        Slog.d(TAG, "isExternalStorageMounted = " + externalStorageState);
        return externalStorageState.equals("mounted");
    }

    public static String getExternalStorageDefaultPath(Context context) {
        return Environment.getExternalStorageDirectory().toString();
    }

    public static long getDiskAvailableSize() {
        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        try {
            if (!externalStorageDirectory.exists() || !externalStorageDirectory.isDirectory()) {
                return -1L;
            }
            StatFs statFs = new StatFs(externalStorageDirectory.getPath());
            long blockSize = ((long) statFs.getBlockSize()) * ((long) statFs.getAvailableBlocks());
            Slog.d(TAG, "total size in getDiskAvailableSize() = " + blockSize);
            return blockSize;
        } catch (IllegalArgumentException e) {
            return -1L;
        }
    }

    public static boolean isStorageAvailable(Context context) {
        if (!isExternalStorageMounted(context)) {
            Slog.e(TAG, "-----Please insert an SD card----");
            return false;
        }
        if (!diskSpaceAvailable(2097152L)) {
            Slog.e(TAG, "-----SD card storage is full----");
            return false;
        }
        return true;
    }
}
