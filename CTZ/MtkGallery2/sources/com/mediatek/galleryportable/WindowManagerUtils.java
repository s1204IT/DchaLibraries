package com.mediatek.galleryportable;

import android.view.WindowManager;

public class WindowManagerUtils {
    private static boolean sIsSystemUiListenersFieldExisted = false;
    private static boolean sHasChecked = false;

    public static void setSystemUiListeners(WindowManager.LayoutParams lp) {
        if (hasSystemUiListeners()) {
            lp.hasSystemUiListeners = true;
        }
    }

    private static boolean hasSystemUiListeners() {
        if (!sHasChecked) {
            try {
                Class<?> clazz = WindowManagerUtils.class.getClassLoader().loadClass("android.view.WindowManager$LayoutParams");
                if (clazz != null) {
                    clazz.getDeclaredField("hasSystemUiListeners");
                }
                sIsSystemUiListenersFieldExisted = true;
            } catch (ClassNotFoundException e) {
                sIsSystemUiListenersFieldExisted = false;
                Log.e("VP_WindowManagerUtils", "ClassNotFoundException");
            } catch (NoSuchFieldException e2) {
                Log.e("VP_WindowManagerUtils", "NoSuchFieldException");
                sIsSystemUiListenersFieldExisted = false;
            }
            sHasChecked = true;
        }
        return sIsSystemUiListenersFieldExisted;
    }
}
