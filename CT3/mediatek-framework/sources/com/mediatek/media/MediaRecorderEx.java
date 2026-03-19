package com.mediatek.media;

import android.media.MediaRecorder;
import android.util.Log;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MediaRecorderEx {
    private static final String CLASS_NAME = "android.media.MediaRecorder";
    private static final String METHOD_NAME = "setParameter";
    private static final Class[] METHOD_TYPES = {String.class};
    private static final String TAG = "MediaRecorderEx";
    private static Method sSetParameter;

    static {
        try {
            sSetParameter = Class.forName(CLASS_NAME).getDeclaredMethod(METHOD_NAME, METHOD_TYPES);
            if (sSetParameter == null) {
                return;
            }
            sSetParameter.setAccessible(true);
        } catch (ClassNotFoundException e) {
            Log.e("@M_MediaRecorderEx", "ClassNotFoundException: android.media.MediaRecorder");
        } catch (NoSuchMethodException e2) {
            Log.e("@M_MediaRecorderEx", "NoSuchMethodException: setParameter");
        }
    }

    private static void setParameter(MediaRecorder recorder, String nameValuePair) {
        if (sSetParameter != null) {
            try {
                sSetParameter.invoke(recorder, nameValuePair);
                return;
            } catch (IllegalAccessException ae) {
                Log.e("@M_MediaRecorderEx", "IllegalAccessException!", ae);
                return;
            } catch (IllegalArgumentException ex) {
                Log.e("@M_MediaRecorderEx", "IllegalArgumentException!", ex);
                return;
            } catch (NullPointerException npe) {
                Log.e("@M_MediaRecorderEx", "NullPointerException!", npe);
                return;
            } catch (InvocationTargetException te) {
                Log.e("@M_MediaRecorderEx", "InvocationTargetException!", te);
                return;
            }
        }
        Log.e("@M_MediaRecorderEx", "setParameter: Null method!");
    }

    public static void pause(MediaRecorder recorder) throws IllegalStateException {
        if (recorder == null) {
            Log.e("@M_MediaRecorderEx", "Null MediaRecorder!");
        } else {
            recorder.setParametersExtra("media-param-pause=1");
        }
    }

    public final class HDRecordMode {
        public static final int INDOOR = 1;
        public static final int NORMAL = 0;
        public static final int OUTDOOR = 2;

        private HDRecordMode() {
        }
    }

    public static void setHDRecordMode(MediaRecorder recorder, int mode, boolean isVideo) throws IllegalStateException, IllegalArgumentException {
        if (mode < 0 || mode > 2) {
            throw new IllegalArgumentException("Illegal HDRecord mode:" + mode);
        }
        if (isVideo) {
            setParameter(recorder, "audio-param-hdrecvideomode=" + mode);
        } else {
            setParameter(recorder, "audio-param-hdrecvoicemode=" + mode);
        }
    }

    public static void setArtistTag(MediaRecorder recorder, String artist) throws IllegalStateException {
        setParameter(recorder, "media-param-tag-artist=" + artist);
    }

    public static void setAlbumTag(MediaRecorder recorder, String album) throws IllegalStateException {
        setParameter(recorder, "media-param-tag-album=" + album);
    }

    public static void setPreprocessEffect(MediaRecorder recorder, int effectOption) throws IllegalStateException {
        setParameter(recorder, "audio-param-preprocesseffect=" + effectOption);
    }

    public static void setVideoBitOffSet(MediaRecorder recorder, int offset, boolean video) {
        if (!video) {
            return;
        }
        setParameter(recorder, "param-use-64bit-offset=" + offset);
        Log.v("@M_MediaRecorderEx", "setVideoBitOffSet is true,offset= " + offset);
    }
}
