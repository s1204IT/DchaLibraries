package com.mediatek.camera.portability;

import android.media.CamcorderProfile;
import android.util.Log;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CamcorderProfileEx {
    public static CamcorderProfile getProfile(int i, int i2) {
        return getCamcorderProfileNative(i, i2);
    }

    private static final CamcorderProfile getCamcorderProfileNative(int i, int i2) {
        try {
            Method declaredMethod = CamcorderProfile.class.getDeclaredMethod("native_get_camcorder_profile", Integer.TYPE, Integer.TYPE);
            declaredMethod.setAccessible(true);
            return (CamcorderProfile) declaredMethod.invoke(null, Integer.valueOf(i), Integer.valueOf(i2));
        } catch (IllegalAccessException e) {
            Log.e("CamcorderProfileEx", "native_get_camcorder_profile error");
            return null;
        } catch (IllegalArgumentException e2) {
            Log.e("CamcorderProfileEx", "native_get_camcorder_profile error");
            return null;
        } catch (NoSuchMethodException e3) {
            Log.e("CamcorderProfileEx", "native_get_camcorder_profile error");
            return null;
        } catch (SecurityException e4) {
            Log.e("CamcorderProfileEx", "native_get_camcorder_profile error");
            return null;
        } catch (InvocationTargetException e5) {
            Log.e("CamcorderProfileEx", "native_get_camcorder_profile error");
            return null;
        }
    }
}
