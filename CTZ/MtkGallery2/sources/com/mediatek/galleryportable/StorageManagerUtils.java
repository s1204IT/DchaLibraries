package com.mediatek.galleryportable;

import android.content.Context;
import android.os.Environment;
import android.os.storage.StorageManager;
import com.mediatek.storage.StorageManagerEx;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class StorageManagerUtils {
    private static boolean sHasChecked = false;
    private static boolean sIsStorageManagerExExist = false;
    private static boolean sHasIsExternalSDCardFunction = false;
    private static boolean sHasIsUSBOTGFunction = false;
    private static boolean sHasGetDefaultPathFunction = false;
    private static boolean sHasGetVolumnPathsFunction = false;
    private static Method sMethodIsExternalSDCard = null;
    private static Method sMethodIsUSBOTG = null;

    public static String getDefaultPath() {
        checkWhetherSupport();
        if (sHasGetDefaultPathFunction) {
            try {
                return StorageManagerEx.getDefaultPath();
            } catch (RuntimeException e) {
                android.util.Log.d("Gallery2/StorageManagerUtils", "<getDefaultPath> RuntimeException, reget from sdk api", e);
                return Environment.getExternalStorageDirectory().getAbsolutePath();
            }
        }
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    public static String getStorageForCache(Context context) {
        StorageManager storageManager = (StorageManager) context.getSystemService("storage");
        String internalStoragePath = null;
        try {
            String[] volumes = storageManager.getVolumePaths();
            int length = volumes.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                String str = volumes[i];
                if (!isExternalSDCard(str) && !isUSBOTG(str) && "mounted".equalsIgnoreCase(storageManager.getVolumeState(str))) {
                    internalStoragePath = str;
                    break;
                }
                i++;
            }
        } catch (UnsupportedOperationException e) {
            android.util.Log.d("Gallery2/StorageManagerUtils", "<getStorageForCache> UnsupportedOperationException", e);
        }
        if (internalStoragePath == null || internalStoragePath.equals("")) {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        return internalStoragePath;
    }

    private static boolean isExternalSDCard(String path) throws UnsupportedOperationException {
        checkWhetherSupport();
        if (sHasIsExternalSDCardFunction) {
            try {
                return ((Boolean) sMethodIsExternalSDCard.invoke(null, path)).booleanValue();
            } catch (IllegalAccessException e) {
                android.util.Log.e("Gallery2/StorageManagerUtils", "isExternalSDCard", e);
                return false;
            } catch (InvocationTargetException e2) {
                android.util.Log.e("Gallery2/StorageManagerUtils", "isExternalSDCard", e2);
                return false;
            }
        }
        throw new UnsupportedOperationException("There is no StorageManagerEx in current platform, not support to check if input path is external sd card", new Throwable());
    }

    private static boolean isUSBOTG(String path) throws UnsupportedOperationException {
        checkWhetherSupport();
        if (sHasIsUSBOTGFunction) {
            try {
                return ((Boolean) sMethodIsUSBOTG.invoke(null, path)).booleanValue();
            } catch (IllegalAccessException e) {
                android.util.Log.e("Gallery2/StorageManagerUtils", "isUSBOTG", e);
                return false;
            } catch (InvocationTargetException e2) {
                android.util.Log.e("Gallery2/StorageManagerUtils", "isUSBOTG", e2);
                return false;
            }
        }
        throw new UnsupportedOperationException("There is no StorageManagerEx in current platform, not support to check if input path is USB OTG", new Throwable());
    }

    public static String[] getVolumnPaths(StorageManager storageManager) {
        checkWhetherSupport();
        if (sHasGetVolumnPathsFunction) {
            return storageManager.getVolumePaths();
        }
        throw new UnsupportedOperationException("There is no StorageManagerEx in current platform, not support to check if input path is USB OTG", new Throwable());
    }

    private static void checkWhetherSupport() {
        if (!sHasChecked) {
            try {
                Class<?> clazz = StorageManagerUtils.class.getClassLoader().loadClass("com.mediatek.storage.StorageManagerEx");
                sIsStorageManagerExExist = clazz != null;
            } catch (ClassNotFoundException e) {
                sIsStorageManagerExExist = false;
            }
            if (!sIsStorageManagerExExist) {
                sHasChecked = true;
                return;
            }
            try {
                sMethodIsExternalSDCard = StorageManagerEx.class.getDeclaredMethod("isExternalSDCard", String.class);
                sHasIsExternalSDCardFunction = sMethodIsExternalSDCard != null;
            } catch (NoSuchMethodException e2) {
                sHasIsExternalSDCardFunction = false;
            }
            try {
                sMethodIsUSBOTG = StorageManagerEx.class.getDeclaredMethod("isUSBOTG", String.class);
                sHasIsUSBOTGFunction = sMethodIsUSBOTG != null;
            } catch (NoSuchMethodException e3) {
                sHasIsUSBOTGFunction = false;
            }
            try {
                Method method = StorageManagerEx.class.getDeclaredMethod("getDefaultPath", new Class[0]);
                sHasGetDefaultPathFunction = method != null;
            } catch (NoSuchMethodException e4) {
                sHasGetDefaultPathFunction = false;
            }
            try {
                Method method2 = StorageManager.class.getDeclaredMethod("getVolumePaths", new Class[0]);
                sHasGetVolumnPathsFunction = method2 != null;
            } catch (NoSuchMethodException e5) {
                sHasGetVolumnPathsFunction = false;
            }
            sHasChecked = true;
        }
    }
}
