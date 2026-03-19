package com.mediatek.camera.portability.storage;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.ServiceManager;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import com.mediatek.storage.StorageManagerEx;

public class StorageManagerExt {
    private static StorageManager sStorageManager;

    public static String getDefaultPath() {
        return StorageManagerEx.getDefaultPath();
    }

    public static void initStorageManager(Context context) {
        if (sStorageManager == null) {
            try {
                if (Build.VERSION.SDK_INT >= 23) {
                    sStorageManager = new StorageManager(context, null);
                } else {
                    sStorageManager = new StorageManager(null, null);
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (ServiceManager.ServiceNotFoundException e2) {
                e2.printStackTrace();
            }
        }
    }

    public static String getVolumeState(Context context, String str) {
        initStorageManager(context);
        return sStorageManager.getVolumeState(str);
    }

    public static boolean isSameStorage(Intent intent, String str) {
        StorageVolume storageVolume = (StorageVolume) intent.getParcelableExtra("android.os.storage.extra.STORAGE_VOLUME");
        if (storageVolume != null) {
            String path = storageVolume.getPath();
            if (str != null && str.equals(path)) {
                return true;
            }
        }
        return false;
    }
}
