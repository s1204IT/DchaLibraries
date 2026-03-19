package com.mediatek.camera.portability.pq;

import com.mediatek.camera.portability.ReflectUtil;
import java.lang.reflect.Method;

class PictureQualityWrapper {
    public static void enterCameraMode() {
        com.mediatek.pq.PictureQuality.setMode(1);
    }

    public static void exitCameraMode() {
        com.mediatek.pq.PictureQuality.setMode(0);
    }

    public static int getMinStepOfESSLED() {
        Method method = ReflectUtil.getMethod(com.mediatek.pq.PictureQuality.class, "getESSLEDMinStep", new Class[0]);
        if (method == null) {
            return 4;
        }
        return ((Integer) ReflectUtil.callMethodOnObject(null, method, new Object[0])).intValue();
    }

    public static int getMinStepOfESSOLED() {
        Method method = ReflectUtil.getMethod(com.mediatek.pq.PictureQuality.class, "getESSOLEDMinStep", new Class[0]);
        if (method == null) {
            return 2;
        }
        return ((Integer) ReflectUtil.callMethodOnObject(null, method, new Object[0])).intValue();
    }

    public static void setMinStepOfESSLED(int i) {
        Method method = ReflectUtil.getMethod(com.mediatek.pq.PictureQuality.class, "setESSLEDMinStep", Integer.TYPE);
        if (method != null) {
            ReflectUtil.callMethodOnObject(null, method, Integer.valueOf(i));
        }
    }

    public static void setMinStepOfESSOLED(int i) {
        Method method = ReflectUtil.getMethod(com.mediatek.pq.PictureQuality.class, "setESSOLEDMinStep", Integer.TYPE);
        if (method != null) {
            ReflectUtil.callMethodOnObject(null, method, Integer.valueOf(i));
        }
    }
}
