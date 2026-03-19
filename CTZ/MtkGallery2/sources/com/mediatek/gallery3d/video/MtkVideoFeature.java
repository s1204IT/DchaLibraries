package com.mediatek.gallery3d.video;

import android.content.Context;
import android.os.Build;
import android.os.storage.StorageManager;
import com.mediatek.gallery3d.util.Log;
import com.mediatek.galleryportable.StorageManagerUtils;
import com.mediatek.galleryportable.SystemPropertyUtils;
import java.io.File;

public class MtkVideoFeature {
    private static final String MTK_HOTKNOT = "ro.vendor.mtk_hotknot_support";
    private static final String MTK_SUPPORT = "1";
    private static final String TAG = "VP_MtkVideoFeature";
    private static final String MTK_GMO_RAM_OPTIMIZE = "ro.vendor.mtk_gmo_ram_optimize";
    private static final boolean mIsGmoRamOptimize = "1".equals(SystemPropertyUtils.get(MTK_GMO_RAM_OPTIMIZE));
    private static final String SUPPER_DIMMING = "ro.vendor.mtk_ultra_dimming_support";
    private static final boolean mIsSupperDimmingSupport = "1".equals(SystemPropertyUtils.get(SUPPER_DIMMING));
    private static final String MTK_CLEARMOTION = "ro.vendor.mtk_clearmotion_support";
    private static final boolean mIsClearMotionSupportd = "1".equals(SystemPropertyUtils.get(MTK_CLEARMOTION));
    private static final String MTK_OMA_DRM = "ro.vendor.mtk_oma_drm_support";
    private static final boolean mIsOmaDrmSupported = SystemPropertyUtils.getBoolean(MTK_OMA_DRM, false);
    private static final String CTA_PROPERTY = "ro.vendor.mtk_cta_set";
    private static final boolean mIsSupportCTA = "1".equals(SystemPropertyUtils.get(CTA_PROPERTY));

    public static boolean isSimulateWfd() {
        int i = SystemPropertyUtils.getInt("wfd_debug", 0);
        Log.d(TAG, "isSimulateWfd() support " + i);
        return i == 1;
    }

    public static boolean isGmoRAM() {
        boolean z = mIsGmoRamOptimize;
        Log.d(TAG, "isGmoRAM() return " + z);
        return z;
    }

    public static boolean isGmoRamOptimize() {
        Log.v(TAG, "isGmoRamOptimize() " + mIsGmoRamOptimize);
        return mIsGmoRamOptimize;
    }

    public static boolean isSupperDimmingSupport() {
        Log.v(TAG, "isSupperDimmingSupport() " + mIsSupperDimmingSupport);
        return mIsSupperDimmingSupport;
    }

    public static boolean isClearMotionSupport() {
        Log.d(TAG, "isClearMotionSupported() return " + mIsClearMotionSupportd);
        return mIsClearMotionSupportd;
    }

    public static boolean isOmaDrmSupported() {
        Log.d(TAG, "isOmaDrmSupported() return " + mIsOmaDrmSupported);
        return mIsOmaDrmSupported;
    }

    public static boolean isClearMotionMenuEnabled(Context context) {
        return isClearMotionSupport() && isFileExist(context, "SUPPORT_CLEARMOTION");
    }

    public static boolean isSupportCTA() {
        Log.d(TAG, "mIsSupportCTA() return " + mIsSupportCTA);
        return mIsSupportCTA;
    }

    public static boolean isPowerTest() {
        return "1".equals(SystemPropertyUtils.get("persist.power.auto.test"));
    }

    public static boolean isFileExist(Context context, String str) {
        String[] volumnPaths = StorageManagerUtils.getVolumnPaths((StorageManager) context.getSystemService("storage"));
        if (volumnPaths == null) {
            Log.w(TAG, "isFileExist() storage volume path is null, return false");
            return false;
        }
        boolean z = false;
        for (String str2 : volumnPaths) {
            if (volumnPaths != null) {
                File file = new File(str2, str);
                if (file.exists()) {
                    Log.v(TAG, "isFileExist() file exists with the name is " + file);
                    z = true;
                }
            }
        }
        Log.v(TAG, "isFileExist() exit with isFileExist is " + z);
        return z;
    }

    public static boolean isMultiWindowSupport() {
        return Build.VERSION.SDK_INT >= 24;
    }
}
