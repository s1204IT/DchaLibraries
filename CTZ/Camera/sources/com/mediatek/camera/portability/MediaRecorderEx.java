package com.mediatek.camera.portability;

import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MediaRecorderEx {
    private static final Class[] METHOD_TYPES = {String.class};
    private static Method sSetParameter;
    private static Method sSetParametersExtra;

    static {
        try {
            sSetParameter = Class.forName("android.media.MediaRecorder").getDeclaredMethod("setParameter", METHOD_TYPES);
            if (sSetParameter != null) {
                sSetParameter.setAccessible(true);
            }
        } catch (ClassNotFoundException e) {
            Log.e("MediaRecorderEx", "ClassNotFoundException: android.media.MediaRecorder");
        } catch (NoSuchMethodException e2) {
            Log.e("MediaRecorderEx", "NoSuchMethodException: setParameter");
        }
        try {
            sSetParametersExtra = Class.forName("android.media.MediaRecorder").getDeclaredMethod("setParametersExtra", METHOD_TYPES);
            if (sSetParametersExtra != null) {
                sSetParametersExtra.setAccessible(true);
            }
        } catch (ClassNotFoundException e3) {
            Log.e("MediaRecorderEx", "ClassNotFoundException: android.media.MediaRecorder");
        } catch (NoSuchMethodException e4) {
            Log.e("MediaRecorderEx", "NoSuchMethodException: setParametersExtra");
        }
    }

    private static void setParameter(MediaRecorder mediaRecorder, String str) {
        if (sSetParameter != null) {
            try {
                sSetParameter.invoke(mediaRecorder, str);
                return;
            } catch (IllegalAccessException e) {
                Log.e("MediaRecorderEx", "IllegalAccessException!", e);
                return;
            } catch (IllegalArgumentException e2) {
                Log.e("MediaRecorderEx", "IllegalArgumentException!", e2);
                return;
            } catch (NullPointerException e3) {
                Log.e("MediaRecorderEx", "NullPointerException!", e3);
                return;
            } catch (InvocationTargetException e4) {
                Log.e("MediaRecorderEx", "InvocationTargetException!", e4);
                return;
            }
        }
        Log.e("MediaRecorderEx", "setParameter: Null method!");
    }

    public static void setVideoBitOffSet(MediaRecorder mediaRecorder, int i, boolean z) {
        if (z) {
            setParameter(mediaRecorder, "param-use-64bit-offset=" + i);
            Log.v("MediaRecorderEx", "setVideoBitOffSet is true,offset= " + i);
        }
    }

    public static void setParametersExtra(MediaRecorder mediaRecorder, String str) throws Exception {
        if (sSetParameter != null) {
            try {
                sSetParameter.invoke(mediaRecorder, str);
                return;
            } catch (IllegalAccessException e) {
                Log.e("MediaRecorderEx", "IllegalAccessException!", e);
                throw e;
            } catch (IllegalArgumentException e2) {
                Log.e("MediaRecorderEx", "IllegalArgumentException!", e2);
                throw e2;
            } catch (NullPointerException e3) {
                Log.e("MediaRecorderEx", "NullPointerException!", e3);
                throw e3;
            } catch (InvocationTargetException e4) {
                Log.e("MediaRecorderEx", "InvocationTargetException!", e4);
                throw e4;
            }
        }
        throw new Exception("Not found setParameter function");
    }

    public static void pause(MediaRecorder mediaRecorder) throws IllegalStateException {
        if (mediaRecorder == null) {
            return;
        }
        if (Build.VERSION.SDK_INT <= 23) {
            if (sSetParametersExtra != null) {
                try {
                    sSetParametersExtra.invoke(mediaRecorder, "media-param-pause=1");
                    return;
                } catch (IllegalAccessException e) {
                    Log.e("MediaRecorderEx", "IllegalAccessException!", e);
                    return;
                } catch (IllegalArgumentException e2) {
                    Log.e("MediaRecorderEx", "IllegalArgumentException!", e2);
                    return;
                } catch (NullPointerException e3) {
                    Log.e("MediaRecorderEx", "NullPointerException!", e3);
                    return;
                } catch (InvocationTargetException e4) {
                    Log.e("MediaRecorderEx", "InvocationTargetException!", e4);
                    return;
                }
            }
            return;
        }
        mediaRecorder.pause();
    }

    public static void resume(MediaRecorder mediaRecorder) throws IllegalStateException {
        if (mediaRecorder == null) {
            return;
        }
        if (Build.VERSION.SDK_INT <= 23) {
            if (sSetParametersExtra != null) {
                mediaRecorder.start();
                return;
            }
            return;
        }
        mediaRecorder.resume();
    }
}
