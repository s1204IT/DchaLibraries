package com.mediatek.galleryportable;

import java.lang.reflect.Method;

public class PerfServiceUtils {
    private static Class<?> clazz = null;
    private static Method methodGalleryBoostEnable = null;
    private static boolean sHasChecked = false;
    private static boolean sSupportMtkPowerHal = false;

    public static void boostEnableTimeoutMs(int timeoutMs) {
        if (checkWetherSupport() && clazz != null && methodGalleryBoostEnable != null) {
            android.util.Log.d("Gallery2/PerfServiceUtils", "<boostEnableTimeoutMs> do boost with " + timeoutMs + "ms !");
            try {
                Method method = clazz.getDeclaredMethod("getInstance", new Class[0]);
                if (method != null) {
                    Object powerHal = method.invoke(clazz, new Object[0]);
                    android.util.Log.d("Gallery2/PerfServiceUtils", "<boostEnableTimeoutMs> powerHal " + powerHal);
                    methodGalleryBoostEnable.invoke(powerHal, Integer.valueOf(timeoutMs));
                }
            } catch (Exception e) {
                android.util.Log.e("Gallery2/PerfServiceUtils", "<boostEnableTimeoutMs> Exception", e);
            }
        }
    }

    private static boolean checkWetherSupport() {
        if (!sHasChecked) {
            try {
                clazz = PerfServiceUtils.class.getClassLoader().loadClass("com.mediatek.powerhalwrapper.PowerHalWrapper");
                android.util.Log.d("Gallery2/PerfServiceUtils", "<checkWetherSupport> clazz: " + clazz);
                methodGalleryBoostEnable = clazz.getDeclaredMethod("galleryBoostEnable", Integer.TYPE);
                methodGalleryBoostEnable.setAccessible(true);
                android.util.Log.d("Gallery2/PerfServiceUtils", "<checkWetherSupport> methodPowerHint: " + methodGalleryBoostEnable);
                sSupportMtkPowerHal = true;
            } catch (ClassNotFoundException e) {
                sSupportMtkPowerHal = false;
                android.util.Log.e("Gallery2/PerfServiceUtils", "<checkWetherSupport> ClassNotFoundException", e);
            } catch (NoSuchMethodException e2) {
                sSupportMtkPowerHal = false;
                android.util.Log.e("Gallery2/PerfServiceUtils", "<checkWetherSupport> NoSuchMethodException", e2);
            }
            sHasChecked = true;
            android.util.Log.d("Gallery2/PerfServiceUtils", "<checkWetherSupport> sSupportMtkPowerHal: " + sSupportMtkPowerHal);
        }
        return sSupportMtkPowerHal;
    }
}
