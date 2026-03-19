package com.mediatek.camera.feature.setting.shutterspeed;

import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.util.Range;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import java.util.ArrayList;
import java.util.List;

class ShutterSpeedHelper {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ShutterSpeedHelper.class.getSimpleName());

    ShutterSpeedHelper() {
    }

    public static List<String> getSupportedList(CameraCharacteristics cameraCharacteristics) {
        Long minShutterSpeed = getMinShutterSpeed(cameraCharacteristics);
        Long maxShutterSpeed = getMaxShutterSpeed(cameraCharacteristics);
        LogHelper.d(TAG, "[getSupportedList]+ shutter speed range (" + minShutterSpeed + ", " + maxShutterSpeed + ")");
        ArrayList arrayList = new ArrayList();
        arrayList.add("Auto");
        int i = Integer.parseInt(String.valueOf(maxShutterSpeed.longValue() / 1000000000));
        for (int i2 = Integer.parseInt(String.valueOf(minShutterSpeed.longValue() / 1000000000)); i2 <= i; i2++) {
            arrayList.add(String.valueOf(i2));
        }
        return getAppSupportedValues(arrayList);
    }

    public static List<String> getSupportedList(Camera.Parameters parameters) {
        ArrayList arrayList = new ArrayList();
        arrayList.add("Auto");
        String str = parameters.get("max-exposure-time");
        if (str == null) {
            return getAppSupportedValues(arrayList);
        }
        int i = Integer.parseInt(str) / 1000;
        for (int i2 = 1; i2 <= i; i2++) {
            arrayList.add(String.valueOf(i2));
        }
        return getAppSupportedValues(arrayList);
    }

    public static boolean isShutterSpeedSupported(CameraCharacteristics cameraCharacteristics) {
        if (cameraCharacteristics == null) {
            LogHelper.w(TAG, "[isShutterSpeedSupported] characteristics is null");
            return false;
        }
        Range range = (Range) cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
        LogHelper.i(TAG, "[isShutterSpeedSupported] shutterSpeedRange " + range);
        return range != null && ((Long) range.getUpper()).longValue() >= 1000000000;
    }

    public static boolean isShutterSpeedSupported(Camera.Parameters parameters) {
        if (parameters == null) {
            LogHelper.w(TAG, "[isShutterSpeedSupported] originalParameters is null");
            return false;
        }
        String str = parameters.get("max-exposure-time");
        if (str == null) {
            LogHelper.w(TAG, "[isShutterSpeedSupported] maxExposureTime is null");
            return false;
        }
        LogHelper.w(TAG, "[isShutterSpeedSupported] maxExposureTime = " + str);
        return str != null && ((long) (Integer.parseInt(str) / 1000)) >= 1;
    }

    private static Long getMinShutterSpeed(CameraCharacteristics cameraCharacteristics) {
        Range range;
        if (isShutterSpeedSupported(cameraCharacteristics) && (range = (Range) cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)) != null) {
            Long l = (Long) range.getLower();
            LogHelper.d(TAG, "[getMinShutterSpeed] " + l);
            return l;
        }
        return -1L;
    }

    private static Long getMaxShutterSpeed(CameraCharacteristics cameraCharacteristics) {
        Range range;
        if (isShutterSpeedSupported(cameraCharacteristics) && (range = (Range) cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)) != null) {
            Long l = (Long) range.getUpper();
            LogHelper.d(TAG, "[getMaxShutterSpeed] " + l);
            return l;
        }
        return -1L;
    }

    private static List<String> getAppSupportedValues(List<String> list) {
        ArrayList arrayList = new ArrayList();
        arrayList.add("Auto");
        arrayList.add("1");
        arrayList.add("2");
        arrayList.add("4");
        arrayList.add("8");
        arrayList.add("16");
        list.retainAll(arrayList);
        LogHelper.i(TAG, "[getAppSupportedValues] supported values " + list);
        return list;
    }
}
