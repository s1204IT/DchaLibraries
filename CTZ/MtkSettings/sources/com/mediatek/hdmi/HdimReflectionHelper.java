package com.mediatek.hdmi;

import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class HdimReflectionHelper {
    public static final boolean HDMI_TB_SUPPORT = !"".equals(SystemProperties.get("ro.vendor.mtk_tb_hdmi"));
    public static final boolean HDMI_HIDL_SUPPORT = SystemProperties.get("ro.vendor.mtk_tb_hdmi").contains("hidl");

    public static String getHdmiManagerClass() {
        Log.d("HdimReflectionHelper", "getHdmiManagerClass, HDMI_TB_SUPPORT = " + HDMI_TB_SUPPORT);
        if (HDMI_TB_SUPPORT) {
            return "com.mediatek.hdmi.HdmiNative";
        }
        return "com.mediatek.hdmi.IMtkHdmiManager";
    }

    public static int getHdmiDisplayType(Object obj) {
        try {
            return getDeclaredMethod(Class.forName(getHdmiManagerClass(), false, ClassLoader.getSystemClassLoader()), obj, "getDisplayType");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static int getHdmiDisplayTypeConstant(String str) {
        try {
            Class<?> cls = Class.forName("com.mediatek.hdmi.HdmiDef", false, ClassLoader.getSystemClassLoader());
            return ((Integer) getNonPublicField(cls, str).get(cls)).intValue();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return -1;
        } catch (IllegalAccessException e2) {
            e2.printStackTrace();
            return -1;
        } catch (IllegalArgumentException e3) {
            e3.printStackTrace();
            return -1;
        }
    }

    public static boolean hasCapability(Object obj) {
        try {
            Method declaredMethod = Class.forName(getHdmiManagerClass(), false, ClassLoader.getSystemClassLoader()).getDeclaredMethod("hasCapability", Integer.TYPE);
            declaredMethod.setAccessible(true);
            return Boolean.valueOf(declaredMethod.invoke(obj, Integer.valueOf(getHdmiDisplayTypeConstant("CAPABILITY_SCALE_ADJUST"))).toString()).booleanValue();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static int[] getSupportedResolutions(Object obj) {
        try {
            Method declaredMethod = Class.forName(getHdmiManagerClass(), false, ClassLoader.getSystemClassLoader()).getDeclaredMethod("getSupportedResolutions", new Class[0]);
            declaredMethod.setAccessible(true);
            return (int[]) declaredMethod.invoke(obj, new Object[0]);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int getAudioParameter(Object obj) {
        try {
            Method declaredMethod = Class.forName(getHdmiManagerClass(), false, ClassLoader.getSystemClassLoader()).getDeclaredMethod("getAudioParameter", Integer.TYPE, Integer.TYPE);
            declaredMethod.setAccessible(true);
            return Integer.valueOf(declaredMethod.invoke(obj, Integer.valueOf(getHdmiDisplayTypeConstant("HDMI_MAX_CHANNEL")), Integer.valueOf(getHdmiDisplayTypeConstant("HDMI_MAX_CHANNEL_OFFSETS"))).toString()).intValue();
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static boolean isSignalOutputting(Object obj) {
        try {
            Method declaredMethod = Class.forName(getHdmiManagerClass(), false, ClassLoader.getSystemClassLoader()).getDeclaredMethod("isSignalOutputting", new Class[0]);
            declaredMethod.setAccessible(true);
            return Boolean.valueOf(declaredMethod.invoke(obj, new Object[0]).toString()).booleanValue();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void setVideoResolution(Object obj, int i) {
        try {
            Method declaredMethod = Class.forName(getHdmiManagerClass(), false, ClassLoader.getSystemClassLoader()).getDeclaredMethod("setVideoResolution", Integer.TYPE);
            declaredMethod.setAccessible(true);
            declaredMethod.invoke(obj, Integer.valueOf(i));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setVideoScale(Object obj, int i) {
        try {
            Method declaredMethod = Class.forName(getHdmiManagerClass(), false, ClassLoader.getSystemClassLoader()).getDeclaredMethod("setVideoScale", Integer.TYPE);
            declaredMethod.setAccessible(true);
            declaredMethod.invoke(obj, Integer.valueOf(i));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void enableHdmi(Object obj, boolean z) {
        try {
            Method declaredMethod = Class.forName(getHdmiManagerClass(), false, ClassLoader.getSystemClassLoader()).getDeclaredMethod("enableHdmi", Boolean.TYPE);
            declaredMethod.setAccessible(true);
            declaredMethod.invoke(obj, Boolean.valueOf(z));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setAutoMode(Object obj, boolean z) {
        try {
            Method declaredMethod = Class.forName(getHdmiManagerClass(), false, ClassLoader.getSystemClassLoader()).getDeclaredMethod("setAutoMode", Boolean.TYPE);
            declaredMethod.setAccessible(true);
            declaredMethod.invoke(obj, Boolean.valueOf(z));
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("HdimReflectionHelper", "setAutoMode, e = " + e);
        }
    }

    public static Object getHdmiService() {
        Object objInvoke;
        Log.d("HdimReflectionHelper", "getHdmiService, HDMI_TB_SUPPORT = " + HDMI_TB_SUPPORT);
        if (HDMI_TB_SUPPORT) {
            try {
                Class<?> cls = Class.forName("com.mediatek.hdmi.HdmiNative", false, ClassLoader.getSystemClassLoader());
                Log.d("HdimReflectionHelper", "getHdmiService, hdmiManagerClass = " + cls);
                Method declaredMethod = cls.getDeclaredMethod("getInstance", new Class[0]);
                declaredMethod.setAccessible(true);
                objInvoke = declaredMethod.invoke(cls, new Object[0]);
                Log.d("HdimReflectionHelper", "getHdmiService, obj = " + objInvoke);
            } catch (Exception e) {
                Log.d("HdimReflectionHelper", "getHdmiService, e = " + e);
                objInvoke = null;
            }
        } else {
            objInvoke = ServiceManager.getService("mtkhdmi");
        }
        Log.d("HdimReflectionHelper", "getHdmiService, obj = " + objInvoke);
        return objInvoke;
    }

    public static Field getNonPublicField(Class<?> cls, String str) {
        Field declaredField;
        try {
            declaredField = cls.getDeclaredField(str);
            try {
                declaredField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                e = e;
                e.printStackTrace();
            }
        } catch (NoSuchFieldException e2) {
            e = e2;
            declaredField = null;
        }
        return declaredField;
    }

    public static int getDeclaredMethod(Class<?> cls, Object obj, String str) {
        try {
            Method declaredMethod = cls.getDeclaredMethod(str, new Class[0]);
            declaredMethod.setAccessible(true);
            return Integer.valueOf(declaredMethod.invoke(obj, new Object[0]).toString()).intValue();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return -1;
        } catch (NoSuchMethodException e2) {
            e2.printStackTrace();
            return -1;
        } catch (InvocationTargetException e3) {
            e3.printStackTrace();
            return -1;
        }
    }
}
