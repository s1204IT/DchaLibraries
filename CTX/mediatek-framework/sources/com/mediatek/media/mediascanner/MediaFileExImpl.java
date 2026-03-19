package com.mediatek.media.mediascanner;

import android.media.MediaFile;
import android.os.SystemProperties;
import android.util.Log;
import com.mediatek.mmsdk.CameraEffectHalException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MediaFileExImpl extends MediaFileEx {
    private static final int FILE_TYPE_3GA = 193;
    private static final int FILE_TYPE_3GPP3 = 199;
    private static final int FILE_TYPE_ADPCM = 113;
    private static final int FILE_TYPE_APE = 111;
    private static final int FILE_TYPE_APK = 799;
    private static final int FILE_TYPE_CAF = 112;
    private static final int FILE_TYPE_FLA = 196;
    private static final int FILE_TYPE_FLV = 398;
    private static final int FILE_TYPE_ICS = 795;
    private static final int FILE_TYPE_ICZ = 796;
    private static final int FILE_TYPE_MP2 = 197;
    private static final int FILE_TYPE_MP2PS = 393;
    private static final int FILE_TYPE_MP2TS = 308;
    private static final int FILE_TYPE_MPO = 499;
    private static final int FILE_TYPE_MS_EXCEL = 705;
    private static final int FILE_TYPE_MS_POWERPOINT = 706;
    private static final int FILE_TYPE_MS_WORD = 704;
    private static final int FILE_TYPE_OGM = 394;
    private static final int FILE_TYPE_QUICKTIME_AUDIO = 194;
    private static final int FILE_TYPE_QUICKTIME_VIDEO = 397;
    private static final int FILE_TYPE_RA = 198;
    private static final int FILE_TYPE_VCF = 797;
    private static final int FILE_TYPE_VCS = 798;
    private static final String TAG = "MediaFileExImpl";
    private static final String ADD_FILE_TYPE = "addFileType";
    private static Method sAddFileType = getMethod(MediaFile.class, ADD_FILE_TYPE, String.class, Integer.TYPE, String.class);
    private static Method sAddFileTypeMoreDetail = getMethod(MediaFile.class, ADD_FILE_TYPE, String.class, Integer.TYPE, String.class, Integer.TYPE, Boolean.TYPE);

    private static Method getMethod(Class<?> cls, String str, Class<?>... clsArr) {
        try {
            Method declaredMethod = cls.getDeclaredMethod(str, clsArr);
            declaredMethod.setAccessible(true);
            return declaredMethod;
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "[getMethod]", e);
            return null;
        }
    }

    private static Object callMethodOnObject(Object obj, Method method, Object... objArr) {
        try {
            return method.invoke(obj, objArr);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "[callMethodOnObject]", e);
            return null;
        } catch (InvocationTargetException e2) {
            Log.e(TAG, "[callMethodOnObject]", e2);
            return null;
        }
    }

    public void addMoreVideoFileType() {
        callMethodOnObject(null, sAddFileType, "MTS", Integer.valueOf(FILE_TYPE_MP2TS), "video/mp2ts");
        callMethodOnObject(null, sAddFileType, "M2TS", Integer.valueOf(FILE_TYPE_MP2TS), "video/mp2ts");
        callMethodOnObject(null, sAddFileType, "MOV", Integer.valueOf(FILE_TYPE_QUICKTIME_VIDEO), "video/quicktime");
        callMethodOnObject(null, sAddFileType, "QT", Integer.valueOf(FILE_TYPE_QUICKTIME_VIDEO), "video/quicktime");
        callMethodOnObject(null, sAddFileType, "OGV", Integer.valueOf(FILE_TYPE_OGM), "video/ogm");
        callMethodOnObject(null, sAddFileType, "OGM", Integer.valueOf(FILE_TYPE_OGM), "video/ogm");
    }

    public void addMoreAudioFileType() {
        callMethodOnObject(null, sAddFileType, "3GP", Integer.valueOf(FILE_TYPE_3GPP3), "audio/3gpp");
        callMethodOnObject(null, sAddFileType, "3GA", Integer.valueOf(FILE_TYPE_3GA), "audio/3gpp");
        callMethodOnObject(null, sAddFileType, "MOV", Integer.valueOf(FILE_TYPE_QUICKTIME_AUDIO), "audio/quicktime");
        callMethodOnObject(null, sAddFileType, "QT", Integer.valueOf(FILE_TYPE_QUICKTIME_AUDIO), "audio/quicktime");
        callMethodOnObject(null, sAddFileTypeMoreDetail, "WAV", Integer.valueOf(CameraEffectHalException.EFFECT_HAL_FACTORY_ERROR), "audio/wav", 12296, true);
        callMethodOnObject(null, sAddFileTypeMoreDetail, "OGG", Integer.valueOf(CameraEffectHalException.EFFECT_HAL_IN_USE), "audio/vorbis", 47362, true);
        callMethodOnObject(null, sAddFileTypeMoreDetail, "OGG", Integer.valueOf(CameraEffectHalException.EFFECT_HAL_IN_USE), "audio/webm", 47362, true);
    }

    public void addMoreImageFileType() {
        if (!SystemProperties.getBoolean("ro.mtk_bsp_package", false)) {
            callMethodOnObject(null, sAddFileType, "MPO", Integer.valueOf(FILE_TYPE_MPO), "image/mpo");
        }
    }

    public void addMoreOtherFileType() {
        callMethodOnObject(null, sAddFileType, "ICS", Integer.valueOf(FILE_TYPE_ICS), "text/calendar");
        callMethodOnObject(null, sAddFileType, "ICZ", Integer.valueOf(FILE_TYPE_ICZ), "text/calendar");
        callMethodOnObject(null, sAddFileType, "VCF", Integer.valueOf(FILE_TYPE_VCF), "text/x-vcard");
        callMethodOnObject(null, sAddFileType, "VCS", Integer.valueOf(FILE_TYPE_VCS), "text/x-vcalendar");
        callMethodOnObject(null, sAddFileType, "APK", Integer.valueOf(FILE_TYPE_APK), "application/vnd.android.package-archive");
        callMethodOnObject(null, sAddFileType, "DOCX", Integer.valueOf(FILE_TYPE_MS_WORD), "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        callMethodOnObject(null, sAddFileType, "DOTX", Integer.valueOf(FILE_TYPE_MS_WORD), "application/vnd.openxmlformats-officedocument.wordprocessingml.template");
        callMethodOnObject(null, sAddFileType, "XLSX", Integer.valueOf(FILE_TYPE_MS_EXCEL), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        callMethodOnObject(null, sAddFileType, "XLTX", Integer.valueOf(FILE_TYPE_MS_EXCEL), "application/vnd.openxmlformats-officedocument.spreadsheetml.template");
        callMethodOnObject(null, sAddFileType, "PPTX", Integer.valueOf(FILE_TYPE_MS_POWERPOINT), "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        callMethodOnObject(null, sAddFileType, "POTX", Integer.valueOf(FILE_TYPE_MS_POWERPOINT), "application/vnd.openxmlformats-officedocument.presentationml.template");
        callMethodOnObject(null, sAddFileType, "PPSX", Integer.valueOf(FILE_TYPE_MS_POWERPOINT), "application/vnd.openxmlformats-officedocument.presentationml.slideshow");
    }
}
