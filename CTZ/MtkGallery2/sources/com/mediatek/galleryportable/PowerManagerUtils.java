package com.mediatek.galleryportable;

import android.os.PowerManager;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PowerManagerUtils {
    private static boolean sHasSetBacklightOffForWfdFunction = false;
    private static boolean sHasChecked = false;
    private static Method sMethod = null;

    public static void setBacklightOffForWfd(PowerManager pm, boolean off) {
        if (hasSetBacklightOffForWfdFunction()) {
            try {
                sMethod.invoke(pm, Boolean.valueOf(off));
            } catch (IllegalAccessException e) {
                android.util.Log.w("setBacklightOffForWfd", e);
            } catch (InvocationTargetException e2) {
                android.util.Log.w("setBacklightOffForWfd", e2);
            }
        }
    }

    private static boolean hasSetBacklightOffForWfdFunction() {
        if (!sHasChecked) {
            try {
                sMethod = PowerManager.class.getDeclaredMethod("setBacklightOffForWfd", Boolean.TYPE);
                sHasSetBacklightOffForWfdFunction = sMethod != null;
                sHasChecked = true;
            } catch (NoSuchMethodException e) {
                sHasSetBacklightOffForWfdFunction = false;
                sHasChecked = true;
            }
        }
        return sHasSetBacklightOffForWfdFunction;
    }
}
