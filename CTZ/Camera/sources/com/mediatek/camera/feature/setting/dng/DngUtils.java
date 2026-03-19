package com.mediatek.camera.feature.setting.dng;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.DngCreator;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.os.Build;
import android.util.Size;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@TargetApi(21)
public class DngUtils {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(DngUtils.class.getSimpleName());

    public static byte[] getDngDataFromCreator(byte[] bArr, CameraCharacteristics cameraCharacteristics, CaptureResult captureResult, Size size, int i) {
        byte[] byteArray;
        ByteArrayOutputStream byteArrayOutputStream;
        try {
            DngCreator dngCreator = new DngCreator(cameraCharacteristics, captureResult);
            dngCreator.setOrientation(i);
            byteArrayOutputStream = new ByteArrayOutputStream();
            dngCreator.writeByteBuffer(byteArrayOutputStream, size, ByteBuffer.wrap(bArr), 0L);
            byteArray = byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            byteArray = null;
        }
        try {
            byteArrayOutputStream.close();
        } catch (IOException e2) {
            LogHelper.e(TAG, "[convertRawToDng], dng write error");
        }
        return byteArray;
    }

    public static int getDngOrientation(int i) {
        if (i == 0) {
            return 1;
        }
        if (i == 90) {
            return 6;
        }
        if (i == 180) {
            return 3;
        }
        return 8;
    }

    public static Size getRawSize(CameraCharacteristics cameraCharacteristics) {
        Rect rect;
        if (Build.VERSION.SDK_INT >= 23) {
            rect = (Rect) getValueFromKey(cameraCharacteristics, CameraCharacteristics.SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE);
        } else {
            rect = null;
        }
        if (rect == null) {
            LogHelper.e(TAG, "[getRawSize], get raw size error");
            return null;
        }
        int iWidth = rect.width();
        int iHeight = rect.height();
        LogHelper.d(TAG, "[getRawSize], rawWidth = " + iWidth + ", rawHeight = " + iHeight);
        return new Size(iWidth, iHeight);
    }

    public static <T> T getValueFromKey(CameraCharacteristics cameraCharacteristics, CameraCharacteristics.Key<T> key) {
        T t;
        try {
            t = (T) cameraCharacteristics.get(key);
            if (t == null) {
                try {
                    LogHelper.e(TAG, key.getName() + "was null");
                } catch (IllegalArgumentException e) {
                    LogHelper.e(TAG, key.getName() + " was not supported by this device");
                }
            }
        } catch (IllegalArgumentException e2) {
            t = null;
        }
        return t;
    }

    public static List<Integer> getAvailableCapablities(CameraCharacteristics cameraCharacteristics) {
        int[] iArr = (int[]) getValueFromKey(cameraCharacteristics, CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
        if (iArr == null) {
            LogHelper.i(TAG, "The camera available capabilities is null");
            return new ArrayList();
        }
        ArrayList arrayList = new ArrayList(iArr.length);
        String str = "";
        for (int i : iArr) {
            arrayList.add(Integer.valueOf(i));
            str = str + i + ", ";
        }
        LogHelper.d(TAG, "The camera available capabilities are:" + str);
        return arrayList;
    }

    public static ContentValues getContentValue(long j, String str, int i, int i2, int i3, Location location) {
        String strGenerateTitle = generateTitle(j);
        String str2 = strGenerateTitle + ".dng";
        ContentValues contentValues = new ContentValues();
        contentValues.put("datetaken", Long.valueOf(j));
        contentValues.put("title", strGenerateTitle);
        contentValues.put("_display_name", str2);
        contentValues.put("_data", str + '/' + str2);
        contentValues.put("mime_type", "image/x-adobe-dng");
        contentValues.put("width", Integer.valueOf(i));
        contentValues.put("height", Integer.valueOf(i2));
        contentValues.put("orientation", Integer.valueOf(i3));
        if (location != null) {
            contentValues.put("latitude", Double.valueOf(location.getLatitude()));
            contentValues.put("longitude", Double.valueOf(location.getLongitude()));
        }
        return contentValues;
    }

    public static boolean isDngCaptureSizeAvailable(CameraCharacteristics cameraCharacteristics) {
        boolean z;
        Size[] outputSizes = ((StreamConfigurationMap) cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)).getOutputSizes(32);
        if (outputSizes == null) {
            LogHelper.e(TAG, "[isDngCaptureSizeAvailable] No capture sizes available for raw format");
            return false;
        }
        for (Size size : outputSizes) {
            LogHelper.d(TAG, "[isDngSupported] raw supported size:" + size);
        }
        Rect rect = (Rect) getValueFromKey(cameraCharacteristics, CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        if (rect == null) {
            LogHelper.e(TAG, "[isDngSupported] Active array is null");
            return false;
        }
        LogHelper.d(TAG, "[isDngSupported] Active array is:" + rect);
        Size size2 = new Size(rect.width(), rect.height());
        int length = outputSizes.length;
        int i = 0;
        while (true) {
            if (i < length) {
                Size size3 = outputSizes[i];
                if (size3.getWidth() != size2.getWidth() || size3.getHeight() != size2.getHeight()) {
                    i++;
                } else {
                    z = true;
                    break;
                }
            } else {
                z = false;
                break;
            }
        }
        if (z) {
            return true;
        }
        LogHelper.e(TAG, "[isDngSupported] Aavailable sizes for RAW format do not include active array size");
        return false;
    }

    private static String generateTitle(long j) {
        return new SimpleDateFormat("'IMG'_yyyyMMdd_HHmmss").format((Date) new java.sql.Date(j));
    }
}
