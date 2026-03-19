package com.mediatek.powerhalwrapper;

import android.os.Trace;
import android.util.Log;

public class PowerHalWrapper {
    private static final int AMS_BOOST_TIME = 30000;
    private static final String TAG = "PowerHalWrapper";
    private static boolean AMS_BOOST_PROCESS_CREATE = true;
    private static boolean AMS_BOOST_PACK_SWITCH = true;
    private static boolean AMS_BOOST_ACT_SWITCH = true;
    private static boolean EXT_PEAK_PERF_MODE = false;
    private static int pboost_pc_timeout = 0;
    private static int pboost_act_timeout = 0;
    private static int pextpeak_period = 0;
    private static int exLchProcessCreate = 0;
    private static int exLchPackSwitch = 0;
    private static int exLchActSwitch = 0;
    private static PowerHalWrapper sInstance = null;
    private static Object lock = new Object();

    public static native int nativeMtkCusPowerHint(int i, int i2);

    public static native int nativeMtkPowerHint(int i, int i2);

    public static native int nativeNotifyAppState(String str, String str2, int i, int i2);

    public static native int nativeQuerySysInfo(int i, int i2);

    public static native int nativeScnConfig(int i, int i2, int i3, int i4, int i5, int i6);

    public static native int nativeScnDisable(int i);

    public static native int nativeScnEnable(int i, int i2);

    public static native int nativeScnReg();

    public static native int nativeScnUltraCfg(int i, int i2, int i3, int i4, int i5, int i6);

    public static native int nativeScnUnreg(int i);

    static {
        System.loadLibrary("perfframeinfo_jni");
    }

    public static PowerHalWrapper getInstance() {
        if (sInstance == null) {
            synchronized (lock) {
                if (sInstance == null) {
                    sInstance = new PowerHalWrapper();
                }
            }
            pextpeak_period = nativeQuerySysInfo(17, 0);
            Log.e(TAG, "pextpeak_period: " + pextpeak_period);
            mtkExLchSort();
        }
        return sInstance;
    }

    private PowerHalWrapper() {
    }

    private static void mtkExLchSort() {
        int iNativeQuerySysInfo = nativeQuerySysInfo(8, 0);
        if (iNativeQuerySysInfo == 15) {
            exLchProcessCreate = nativeQuerySysInfo(9, 0);
            Log.e(TAG, "<mtkExLchSort> exLchScn:" + iNativeQuerySysInfo + " ,period:" + exLchProcessCreate);
        }
        int iNativeQuerySysInfo2 = nativeQuerySysInfo(10, 0);
        if (iNativeQuerySysInfo2 == 16) {
            exLchPackSwitch = nativeQuerySysInfo(11, 0);
            Log.e(TAG, "<mtkExLchSort> exLchScn:" + iNativeQuerySysInfo2 + " ,period:" + exLchPackSwitch);
        }
        int iNativeQuerySysInfo3 = nativeQuerySysInfo(12, 0);
        if (iNativeQuerySysInfo3 == 17) {
            exLchActSwitch = nativeQuerySysInfo(13, 0);
            Log.e(TAG, "<mtkExLchSort> exLchScn:" + iNativeQuerySysInfo3 + " ,period:" + exLchActSwitch);
        }
    }

    private void mtkPowerHint(int i, int i2) {
        nativeMtkPowerHint(i, i2);
    }

    public void mtkCusPowerHint(int i, int i2) {
        nativeMtkCusPowerHint(i, i2);
    }

    public int scnReg() {
        return nativeScnReg();
    }

    public int scnConfig(int i, int i2, int i3, int i4, int i5, int i6) {
        nativeScnConfig(i, i2, i3, i4, i5, i6);
        return 0;
    }

    public int scnUnreg(int i) {
        nativeScnUnreg(i);
        return 0;
    }

    public int scnEnable(int i, int i2) {
        nativeScnEnable(i, i2);
        return 0;
    }

    public int scnDisable(int i) {
        nativeScnDisable(i);
        return 0;
    }

    public int scnUltraCfg(int i, int i2, int i3, int i4, int i5, int i6) {
        nativeScnUltraCfg(i, i2, i3, i4, i5, i6);
        return 0;
    }

    public void galleryBoostEnable(int i) {
        Log.e(TAG, "<galleryBoostEnable> do boost with " + i + "ms");
        nativeMtkPowerHint(23, i);
    }

    public void setRotationBoost(int i) {
        Log.e(TAG, "<setRotation> do boost with " + i + "ms");
        nativeMtkPowerHint(19, i);
    }

    public void setSpeedDownload(int i) {
        Log.e(TAG, "<setSpeedDownload> do boost with " + i + "ms");
        nativeMtkPowerHint(30, i);
    }

    public void setWFD(boolean z) {
        Log.e(TAG, "<setWFD> enable:" + z);
        if (z) {
            nativeMtkPowerHint(27, 268435455);
        } else {
            nativeMtkPowerHint(27, 0);
        }
    }

    public void setInstallationBoost(boolean z) {
        Log.e(TAG, "<setInstallationBoost> enable:" + z);
        if (z) {
            nativeMtkPowerHint(28, 15000);
        } else {
            nativeMtkPowerHint(28, 0);
        }
    }

    public void amsBoostResume(String str, String str2) {
        Trace.asyncTraceBegin(64L, "amPerfBoost", 0);
        nativeMtkPowerHint(29, 0);
        if (str == null || !str.equalsIgnoreCase(str2)) {
            AMS_BOOST_PACK_SWITCH = true;
            if (EXT_PEAK_PERF_MODE) {
                EXT_PEAK_PERF_MODE = false;
                nativeMtkPowerHint(15, 0);
            }
            nativeMtkPowerHint(16, AMS_BOOST_TIME);
            return;
        }
        AMS_BOOST_ACT_SWITCH = true;
        if (EXT_PEAK_PERF_MODE) {
            EXT_PEAK_PERF_MODE = false;
            nativeMtkPowerHint(15, 0);
        }
        nativeMtkPowerHint(17, AMS_BOOST_TIME);
    }

    public void amsBoostProcessCreate(String str, String str2) {
        if (str.compareTo("activity") == 0) {
            Trace.asyncTraceBegin(64L, "amPerfBoost", 0);
            AMS_BOOST_PROCESS_CREATE = true;
            nativeMtkPowerHint(29, 0);
            nativeMtkPowerHint(15, AMS_BOOST_TIME);
        }
    }

    public void amsBoostStop() {
        int i = pextpeak_period;
        if (AMS_BOOST_PACK_SWITCH) {
            AMS_BOOST_PACK_SWITCH = false;
            nativeMtkPowerHint(16, 0);
            if (exLchPackSwitch > 0) {
                nativeMtkPowerHint(29, exLchPackSwitch);
            }
        }
        if (AMS_BOOST_ACT_SWITCH) {
            AMS_BOOST_ACT_SWITCH = false;
            nativeMtkPowerHint(17, 0);
            if (exLchActSwitch > 0) {
                nativeMtkPowerHint(29, exLchActSwitch);
            }
        }
        if (AMS_BOOST_PROCESS_CREATE) {
            AMS_BOOST_PROCESS_CREATE = false;
            pboost_pc_timeout = nativeQuerySysInfo(16, 0);
            if (i > 0 || pboost_pc_timeout > 0) {
                EXT_PEAK_PERF_MODE = true;
                if (pboost_pc_timeout > 0) {
                    i += pboost_pc_timeout;
                    Log.e(TAG, "<amsBoostStop> duration: " + i + "ms, pboost_pc_timeout: " + pboost_pc_timeout);
                    pboost_pc_timeout = 0;
                }
                Log.e(TAG, "<amsBoostStop> duration: " + i + "ms");
                nativeMtkPowerHint(29, 0);
                nativeMtkPowerHint(15, i);
            } else {
                nativeMtkPowerHint(15, 0);
                if (exLchProcessCreate > 0) {
                    nativeMtkPowerHint(29, exLchProcessCreate);
                }
            }
        }
        Trace.asyncTraceEnd(64L, "amPerfBoost", 0);
    }

    public void amsBoostNotify(int i, String str, String str2) {
        nativeNotifyAppState(str2, str, i, 1);
    }

    private static void log(String str) {
        Log.d("@M_PowerHalWrapper", "[PerfServiceWrapper] " + str + " ");
    }

    private static void loge(String str) {
        Log.e("@M_PowerHalWrapper", "[PerfServiceWrapper] ERR: " + str + " ");
    }
}
