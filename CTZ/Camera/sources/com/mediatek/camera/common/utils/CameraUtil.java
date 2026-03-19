package com.mediatek.camera.common.utils;

import android.R;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.device.CameraOpenException;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.exif.ExifInterface;
import com.mediatek.camera.common.loader.FeatureLoader;
import com.mediatek.camera.common.mode.CameraApiHelper;
import com.mediatek.camera.common.widget.Rotatable;
import com.mediatek.camera.portability.SystemProperties;
import com.mediatek.camera.portability.WifiDisplayStatusEx;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class CameraUtil {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(CameraUtil.class.getSimpleName());
    private static AlertDialog sAlertDialog;

    public enum TableList {
        FILE_TABLE,
        VIDEO_TABLE,
        IMAGE_TABLE
    }

    public static int getDisplayRotation(Activity activity) {
        if (!isTablet() && !WifiDisplayStatusEx.isWfdEnabled(activity)) {
            return 0;
        }
        switch (activity.getWindowManager().getDefaultDisplay().getRotation()) {
        }
        return 0;
    }

    public static int getDisplayOrientation(int i, int i2, Context context) {
        CameraCharacteristics cameraCharacteristicsFromDeviceSpec = getCameraCharacteristicsFromDeviceSpec(context, i2);
        if (cameraCharacteristicsFromDeviceSpec == null) {
            LogHelper.e(TAG, "[getRecordingRotation] characteristics is null");
            return 0;
        }
        if (i == -1) {
            LogHelper.w(TAG, "[getRecordingRotation] unknown  degrees");
            return 0;
        }
        int iIntValue = ((Integer) cameraCharacteristicsFromDeviceSpec.get(CameraCharacteristics.LENS_FACING)).intValue();
        int iIntValue2 = ((Integer) cameraCharacteristicsFromDeviceSpec.get(CameraCharacteristics.SENSOR_ORIENTATION)).intValue();
        if (iIntValue == 0) {
            return (360 - ((iIntValue2 + i) % 360)) % 360;
        }
        return ((iIntValue2 - i) + 360) % 360;
    }

    public static int getOrientationFromExif(byte[] bArr) {
        short sShortValue;
        ExifInterface exifInterface = new ExifInterface();
        try {
            exifInterface.readExif(bArr);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Integer tagIntValue = exifInterface.getTagIntValue(ExifInterface.TAG_ORIENTATION);
        if (tagIntValue == null || (sShortValue = tagIntValue.shortValue()) == 1) {
            return 0;
        }
        if (sShortValue == 3) {
            return 180;
        }
        if (sShortValue == 6) {
            return 90;
        }
        if (sShortValue != 8) {
            return 0;
        }
        return 270;
    }

    public static Size getSizeFromExif(byte[] bArr) {
        if (bArr != null) {
            ExifInterface exifInterface = new ExifInterface();
            try {
                exifInterface.readExif(bArr);
            } catch (IOException e) {
                LogHelper.w(TAG, "Failed to read EXIF data", e);
            }
            Integer tagIntValue = exifInterface.getTagIntValue(ExifInterface.TAG_IMAGE_WIDTH);
            Integer tagIntValue2 = exifInterface.getTagIntValue(ExifInterface.TAG_IMAGE_LENGTH);
            if (tagIntValue != null && tagIntValue2 != null) {
                return new Size(tagIntValue.intValue(), tagIntValue2.intValue());
            }
        }
        return new Size(0, 0);
    }

    public static Size getSizeFromSdkExif(String str) {
        int attributeInt;
        android.media.ExifInterface exifInterface;
        int attributeInt2 = 0;
        try {
            exifInterface = new android.media.ExifInterface(str);
            attributeInt = exifInterface.getAttributeInt("ImageWidth", 0);
        } catch (IOException e) {
            e = e;
            attributeInt = 0;
        }
        try {
            attributeInt2 = exifInterface.getAttributeInt("ImageLength", 0);
        } catch (IOException e2) {
            e = e2;
            e.printStackTrace();
        }
        LogHelper.d(TAG, "[getSizeFromSdkExif] width = " + attributeInt + ",height = " + attributeInt2);
        return new Size(attributeInt, attributeInt2);
    }

    public static int getJpegRotation(int i, int i2, Context context) {
        CameraCharacteristics cameraCharacteristicsFromDeviceSpec = getCameraCharacteristicsFromDeviceSpec(context, i);
        if (cameraCharacteristicsFromDeviceSpec == null) {
            LogHelper.e(TAG, "[getJpegRotation] characteristics is null");
            return 0;
        }
        int iIntValue = ((Integer) cameraCharacteristicsFromDeviceSpec.get(CameraCharacteristics.LENS_FACING)).intValue();
        int iIntValue2 = ((Integer) cameraCharacteristicsFromDeviceSpec.get(CameraCharacteristics.SENSOR_ORIENTATION)).intValue();
        if (iIntValue == 0) {
            return ((iIntValue2 - i2) + 360) % 360;
        }
        return (iIntValue2 + i2) % 360;
    }

    public static void rotateViewOrientation(View view, int i, boolean z) {
        if (view == 0) {
            return;
        }
        if (view instanceof Rotatable) {
            ((Rotatable) view).setOrientation(i, z);
            return;
        }
        if (view instanceof ViewGroup) {
            int childCount = view.getChildCount();
            for (int i2 = 0; i2 < childCount; i2++) {
                rotateViewOrientation(view.getChildAt(i2), i, z);
            }
        }
    }

    public static void rotateRotateLayoutChildView(Activity activity, View view, int i, boolean z) {
        int displayRotation = getDisplayRotation(activity);
        if (displayRotation == 270 || displayRotation == 180) {
            i += 180;
        }
        rotateViewOrientation(view, i, z);
    }

    public static int calculateRotateLayoutCompensate(Activity activity) {
        int displayRotation = getDisplayRotation(activity);
        LogHelper.d(TAG, "calculateRotateLayoutCompensate displayRotation = " + displayRotation);
        if (displayRotation == 0) {
            return 0;
        }
        if (displayRotation == 90) {
            return 90;
        }
        if (displayRotation == 180) {
            return 0;
        }
        if (displayRotation == 270) {
            return 90;
        }
        return 1;
    }

    public static boolean isHasNavigationBar(Activity activity) throws NoSuchMethodException {
        Method method;
        int iIntValue;
        int iIntValue2;
        Point point = new Point();
        Point point2 = new Point();
        activity.getWindowManager().getDefaultDisplay().getSize(point);
        if (Build.VERSION.SDK_INT >= 17) {
            activity.getWindowManager().getDefaultDisplay().getRealSize(point2);
        } else {
            Display defaultDisplay = activity.getWindowManager().getDefaultDisplay();
            Method method2 = null;
            try {
                method = Display.class.getMethod("getRawWidth", new Class[0]);
                try {
                    method2 = Display.class.getMethod("getRawHeight", new Class[0]);
                } catch (NoSuchMethodException e) {
                    e = e;
                    e.printStackTrace();
                }
            } catch (NoSuchMethodException e2) {
                e = e2;
                method = null;
            }
            try {
                iIntValue = ((Integer) method.invoke(defaultDisplay, new Object[0])).intValue();
            } catch (IllegalAccessException e3) {
                e = e3;
                iIntValue = 0;
            } catch (InvocationTargetException e4) {
                e = e4;
                iIntValue = 0;
            }
            try {
                iIntValue2 = ((Integer) method2.invoke(defaultDisplay, new Object[0])).intValue();
            } catch (IllegalAccessException e5) {
                e = e5;
                e.printStackTrace();
                iIntValue2 = 0;
            } catch (InvocationTargetException e6) {
                e = e6;
                e.printStackTrace();
                iIntValue2 = 0;
            }
            point2.set(iIntValue, iIntValue2);
        }
        return !point2.equals(point);
    }

    public static int getNavigationBarHeight(Activity activity) {
        if (isHasNavigationBar(activity)) {
            return activity.getResources().getDimensionPixelSize(activity.getResources().getIdentifier("navigation_bar_height", "dimen", "android"));
        }
        return -1;
    }

    public static boolean isColumnExistInDB(Activity activity, TableList tableList, String str) throws Throwable {
        Uri contentUri;
        int columnIndex;
        LogHelper.d(TAG, "[isColumnExistInDB] + table = " + tableList + " column " + str);
        Cursor cursor = null;
        switch (AnonymousClass3.$SwitchMap$com$mediatek$camera$common$utils$CameraUtil$TableList[tableList.ordinal()]) {
            case Camera2Proxy.TEMPLATE_PREVIEW:
                contentUri = MediaStore.Files.getContentUri("external");
                break;
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                break;
            case Camera2Proxy.TEMPLATE_RECORD:
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                break;
            default:
                contentUri = null;
                break;
        }
        if (contentUri == null) {
            return false;
        }
        Uri uriBuild = contentUri.buildUpon().appendQueryParameter("limit", "1").build();
        ContentResolver contentResolver = activity.getContentResolver();
        int i = -1;
        try {
            try {
                Cursor cursorQuery = contentResolver.query(uriBuild, null, null, null, null);
                if (cursorQuery != null) {
                    try {
                        columnIndex = cursorQuery.getColumnIndex(str);
                    } catch (Exception e) {
                        e = e;
                        cursor = cursorQuery;
                        e.printStackTrace();
                        if (cursor != null) {
                            cursor.close();
                        }
                    } catch (Throwable th) {
                        th = th;
                        cursor = cursorQuery;
                        if (cursor != null) {
                            cursor.close();
                        }
                        throw th;
                    }
                } else {
                    columnIndex = -1;
                }
                z = columnIndex != -1;
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                i = columnIndex;
            } catch (Exception e2) {
                e = e2;
            }
            LogHelper.d(TAG, "[isColumnExistInDB] - index = " + i + " isInDB " + z);
            return z;
        } catch (Throwable th2) {
            th = th2;
        }
    }

    static class AnonymousClass3 {
        static final int[] $SwitchMap$com$mediatek$camera$common$utils$CameraUtil$TableList = new int[TableList.values().length];

        static {
            try {
                $SwitchMap$com$mediatek$camera$common$utils$CameraUtil$TableList[TableList.FILE_TABLE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$mediatek$camera$common$utils$CameraUtil$TableList[TableList.VIDEO_TABLE.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$mediatek$camera$common$utils$CameraUtil$TableList[TableList.IMAGE_TABLE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    public static void prepareMatrix(Matrix matrix, boolean z, int i, int i2, int i3) {
        LogHelper.d(TAG, "prepareMatrix mirror =" + z + " displayOrientation=" + i + " viewWidth=" + i2 + " viewHeight=" + i3);
        matrix.setScale(z ? -1.0f : 1.0f, 1.0f);
        matrix.postRotate(i);
        float f = i2;
        float f2 = i3;
        matrix.postScale(f / 2000.0f, f2 / 2000.0f);
        matrix.postTranslate(f / 2.0f, f2 / 2.0f);
    }

    public static void rectFToRect(RectF rectF, Rect rect) {
        rect.left = Math.round(rectF.left);
        rect.top = Math.round(rectF.top);
        rect.right = Math.round(rectF.right);
        rect.bottom = Math.round(rectF.bottom);
    }

    public static Size getSizeByTargetSize(List<Camera.Size> list, Camera.Size size, boolean z) {
        if (size == null || list == null || list.size() <= 0) {
            return null;
        }
        Size size2 = new Size(0, 0);
        long width = z ? 2147483647L : 0L;
        double d = ((double) size.width) / ((double) size.height);
        for (Camera.Size size3 : list) {
            Size size4 = new Size(size3.width, size3.height);
            if (Math.abs((((double) size3.width) / ((double) size3.height)) - d) <= 0.02d) {
                long width2 = size4.getWidth() * size4.getHeight();
                if (z && width2 < width) {
                    width = size4.getWidth() * size4.getHeight();
                    size2 = size4;
                }
                if (!z && width2 > width) {
                    width = size4.getWidth() * size4.getHeight();
                    size2 = size4;
                }
            }
        }
        return size2;
    }

    @TargetApi(17)
    public static Size getOptimalPreviewSize(Activity activity, List<Size> list, double d, boolean z) {
        Size sizeFindBestMatchPanelSize;
        Display defaultDisplay = ((WindowManager) activity.getSystemService("window")).getDefaultDisplay();
        Point point = new Point();
        defaultDisplay.getRealSize(point);
        int iMin = Math.min(point.x, point.y);
        int i = (int) (((double) iMin) * d);
        if (z) {
            LogHelper.d(TAG, "ratio mapping panel size: (" + i + ", " + iMin + ")");
            sizeFindBestMatchPanelSize = findBestMatchPanelSize(list, d, i, iMin);
            if (sizeFindBestMatchPanelSize != null) {
                return sizeFindBestMatchPanelSize;
            }
        } else {
            sizeFindBestMatchPanelSize = null;
        }
        double dAbs = Double.MAX_VALUE;
        if (sizeFindBestMatchPanelSize == null) {
            LogHelper.w(TAG, "[getPreviewSize] no preview size match the aspect ratio : " + d + ", then use standard 4:3 for preview");
            double d2 = Double.parseDouble("1.3333");
            for (Size size : list) {
                if (Math.abs((((double) size.getWidth()) / ((double) size.getHeight())) - d2) <= 0.02d && Math.abs(size.getHeight() - iMin) < dAbs) {
                    dAbs = Math.abs(size.getHeight() - iMin);
                    sizeFindBestMatchPanelSize = size;
                }
            }
        }
        return sizeFindBestMatchPanelSize;
    }

    private static Size findBestMatchPanelSize(List<Size> list, double d, int i, int i2) {
        double d2 = Double.MAX_VALUE;
        Size size = null;
        for (Size size2 : list) {
            if (Math.abs((((double) size2.getWidth()) / ((double) size2.getHeight())) - d) <= 0.02d) {
                double dAbs = Math.abs(size2.getHeight() - i2);
                if (dAbs <= d2) {
                    size = size2;
                    d2 = dAbs;
                }
            }
        }
        LogHelper.i(TAG, "findBestMatchPanelSize size: " + size.getWidth() + " X " + size.getHeight());
        return size;
    }

    public static boolean isTablet() {
        boolean zEquals = "tablet".equals(SystemProperties.getString("ro.build.characteristics", null));
        LogHelper.d(TAG, "isTablet = " + zEquals);
        return zEquals;
    }

    public static void showErrorInfoAndFinish(Activity activity, int i) {
        String string;
        Resources resources = activity.getResources();
        if (i == 100) {
            string = resources.getString(resources.getIdentifier("cannot_connect_camera_new", "string", activity.getPackageName()));
        } else if (i == 1000) {
            string = resources.getString(resources.getIdentifier("camera_disabled", "string", activity.getPackageName()));
        } else if (i != 1050) {
            switch (i) {
                case Camera2Proxy.TEMPLATE_PREVIEW:
                case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                    break;
                default:
                    string = resources.getString(resources.getIdentifier("cannot_connect_camera_new", "string", activity.getPackageName()));
                    break;
            }
        }
        showErrorAndFinish(activity, string, resources.getString(resources.getIdentifier("dialog_ok", "string", activity.getPackageName())));
    }

    public static int getJpegRotationFromDeviceSpec(int i, int i2, Context context) {
        CameraCharacteristics cameraCharacteristicsFromDeviceSpec = getCameraCharacteristicsFromDeviceSpec(context, i);
        if (cameraCharacteristicsFromDeviceSpec == null) {
            LogHelper.e(TAG, "[getJpegRotationFromDeviceSpec] characteristics is null");
            return 0;
        }
        int iIntValue = ((Integer) cameraCharacteristicsFromDeviceSpec.get(CameraCharacteristics.LENS_FACING)).intValue();
        int iIntValue2 = ((Integer) cameraCharacteristicsFromDeviceSpec.get(CameraCharacteristics.SENSOR_ORIENTATION)).intValue();
        if (iIntValue == 0) {
            return ((iIntValue2 - i2) + 360) % 360;
        }
        return (iIntValue2 + i2) % 360;
    }

    public static int getDisplayOrientationFromDeviceSpec(int i, int i2, Context context) {
        return getV2DisplayOrientation(i, i2, context);
    }

    public static int getV2DisplayOrientation(int i, int i2, Context context) {
        CameraCharacteristics cameraCharacteristicsFromDeviceSpec = getCameraCharacteristicsFromDeviceSpec(context, i2);
        if (cameraCharacteristicsFromDeviceSpec == null) {
            LogHelper.e(TAG, "[getV2DisplayOrientation] characteristics is null");
            return 0;
        }
        if (i == -1) {
            LogHelper.w(TAG, "[getV2DisplayOrientation] unknown  degrees");
            return 0;
        }
        int iIntValue = ((Integer) cameraCharacteristicsFromDeviceSpec.get(CameraCharacteristics.LENS_FACING)).intValue();
        int iIntValue2 = ((Integer) cameraCharacteristicsFromDeviceSpec.get(CameraCharacteristics.SENSOR_ORIENTATION)).intValue();
        if (iIntValue == 0) {
            return (360 - ((iIntValue2 + i) % 360)) % 360;
        }
        return ((iIntValue2 - i) + 360) % 360;
    }

    public static void hideAlertDialog(Activity activity) {
        if (sAlertDialog != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        CameraUtil.sAlertDialog.dismiss();
                        AlertDialog unused = CameraUtil.sAlertDialog = null;
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public static List<String> getCamIdsByFacing(boolean z, Context context) {
        int cameraNum = CameraApiHelper.getCameraNum(context);
        LogHelper.d(TAG, "[getCamIdsByFacing] cameraNum " + cameraNum);
        ArrayList arrayList = new ArrayList();
        if (cameraNum > 0) {
            for (int i = 0; i < cameraNum; i++) {
                CameraCharacteristics cameraCharacteristicsFromDeviceSpec = getCameraCharacteristicsFromDeviceSpec(context, i);
                if (cameraCharacteristicsFromDeviceSpec == null) {
                    break;
                }
                int iIntValue = ((Integer) cameraCharacteristicsFromDeviceSpec.get(CameraCharacteristics.LENS_FACING)).intValue();
                if (z && iIntValue == 1) {
                    arrayList.add(String.valueOf(i));
                }
                if (!z && iIntValue == 0) {
                    arrayList.add(String.valueOf(i));
                }
                LogHelper.d(TAG, "[getCamIdsByFacing] i =  " + i + ",facing = " + iIntValue);
            }
        }
        return arrayList;
    }

    private static void showErrorAndFinish(final Activity activity, final String str, final String str2) {
        LogHelper.d(TAG, "[showErrorAndFinish]");
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (activity.isFinishing() || CameraUtil.sAlertDialog != null) {
                    return;
                }
                AlertDialog.Builder neutralButton = new AlertDialog.Builder(activity).setCancelable(false).setIconAttribute(R.attr.alertDialogIcon).setTitle("").setMessage(str).setNeutralButton(str2, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        LogHelper.i(CameraUtil.TAG, "[showErrorAndFinish] on OK click, will finish activity");
                        activity.finish();
                    }
                });
                if (activity.isDestroyed() || activity.isFinishing()) {
                    LogHelper.i(CameraUtil.TAG, "[showErrorAndFinish] activity is finishing, do noting");
                } else {
                    AlertDialog unused = CameraUtil.sAlertDialog = neutralButton.show();
                }
            }
        });
    }

    private static CameraCharacteristics getCameraCharacteristicsFromDeviceSpec(Context context, int i) {
        return CameraApiHelper.getDeviceSpec(context).getDeviceDescriptionMap().get(String.valueOf(i)).getCameraCharacteristics();
    }

    public static boolean isCameraFacingFront(Context context, int i) {
        return ((Integer) getCameraCharacteristicsFromDeviceSpec(context, i).get(CameraCharacteristics.LENS_FACING)).intValue() == 0;
    }

    public static boolean isServiceRun(Context context, String str) {
        boolean z = false;
        if (context == null || str == null) {
            LogHelper.e(TAG, "isServiceRun mContext = " + context + " className = " + str);
            return false;
        }
        try {
            List<ActivityManager.RunningServiceInfo> runningServices = ((ActivityManager) context.getSystemService("activity")).getRunningServices(100);
            int size = runningServices.size();
            int i = 0;
            while (true) {
                if (i >= size) {
                    break;
                }
                if (!runningServices.get(i).service.getClassName().equals(str)) {
                    i++;
                } else {
                    z = true;
                    break;
                }
            }
        } catch (Exception e) {
            LogHelper.e(TAG, "isServiceRun " + e);
        }
        LogHelper.d(TAG, "isServiceRun service name = " + str + " is run " + z);
        return z;
    }

    public static void launchCamera(Activity activity) {
        LogHelper.d(TAG, "[launchCamera]+");
        CameraDeviceManagerFactory.CameraApi cameraApiType = CameraApiHelper.getCameraApiType(null);
        FeatureLoader.updateSettingCurrentModeKey(activity, "com.mediatek.camera.common.mode.photo.PhotoMode");
        try {
            CameraDeviceManagerFactory.getInstance().getCameraDeviceManager(activity, cameraApiType).openCamera("0", null, null);
        } catch (CameraOpenException e) {
            LogHelper.e(TAG, "[launchCamera] e:" + e);
            e.printStackTrace();
        }
        LogHelper.i(TAG, "[launchCamera]- id:0, api:" + cameraApiType);
    }

    public static CaptureRequest.Key<int[]> getRequestKey(CameraCharacteristics cameraCharacteristics, String str) {
        CaptureRequest.Key<int[]> key = 0;
        if (cameraCharacteristics == null) {
            LogHelper.i(TAG, "[getRequestKey] characteristics is null");
            return null;
        }
        List<CaptureRequest.Key<?>> availableCaptureRequestKeys = cameraCharacteristics.getAvailableCaptureRequestKeys();
        if (availableCaptureRequestKeys == null) {
            LogHelper.i(TAG, "[getRequestKey] No keys!");
            return null;
        }
        for (CaptureRequest.Key<?> key2 : availableCaptureRequestKeys) {
            if (key2.getName().equals(str)) {
                LogHelper.i(TAG, "[getRequestKey] key :" + str);
                key = key2;
            }
        }
        return key;
    }

    public static CaptureRequest.Key<int[]> getAvailableSessionKeys(CameraCharacteristics cameraCharacteristics, String str) {
        CaptureRequest.Key<int[]> key = 0;
        if (cameraCharacteristics == null) {
            LogHelper.i(TAG, "[getAvailableSessionKeys] characteristics is null");
            return null;
        }
        List<CaptureRequest.Key<?>> availableSessionKeys = cameraCharacteristics.getAvailableSessionKeys();
        if (availableSessionKeys == null) {
            LogHelper.i(TAG, "[getAvailableSessionKeys] No keys!");
            return null;
        }
        for (CaptureRequest.Key<?> key2 : availableSessionKeys) {
            if (key2.getName().equals(str)) {
                LogHelper.i(TAG, "[getAvailableSessionKeys] key :" + str);
                key = key2;
            }
        }
        return key;
    }

    public static CaptureResult.Key<int[]> getResultKey(CameraCharacteristics cameraCharacteristics, String str) {
        CaptureResult.Key<int[]> key = 0;
        if (cameraCharacteristics == null) {
            LogHelper.i(TAG, "[getResultKey] characteristics is null");
            return null;
        }
        List<CaptureResult.Key<?>> availableCaptureResultKeys = cameraCharacteristics.getAvailableCaptureResultKeys();
        if (availableCaptureResultKeys == null) {
            LogHelper.i(TAG, "[getResultKey] No keys!");
            return null;
        }
        for (CaptureResult.Key<?> key2 : availableCaptureResultKeys) {
            if (key2.getName().equals(str)) {
                LogHelper.i(TAG, "[getResultKey] key : " + str);
                key = key2;
            }
        }
        return key;
    }

    public static int[] getStaticKeyResult(CameraCharacteristics cameraCharacteristics, String str) {
        int[] iArr = null;
        if (cameraCharacteristics == null) {
            LogHelper.i(TAG, "[getStaticKeyResult] characteristics is null");
            return null;
        }
        List<CameraCharacteristics.Key<?>> keys = cameraCharacteristics.getKeys();
        if (keys == null) {
            LogHelper.i(TAG, "[getStaticKeyResult] No keys!");
            return null;
        }
        for (CameraCharacteristics.Key<?> key : keys) {
            if (key.getName().equals(str)) {
                LogHelper.i(TAG, "[getStaticKeyResult] key: " + str);
                iArr = (int[]) cameraCharacteristics.get(key);
            }
        }
        return iArr;
    }

    public static CameraCharacteristics getCameraCharacteristics(Activity activity, String str) {
        try {
            return ((CameraManager) activity.getSystemService("camera")).getCameraCharacteristics(str);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isSupportAvailableMode(CameraCharacteristics cameraCharacteristics, String str, int i) {
        if (cameraCharacteristics == null) {
            LogHelper.i(TAG, "[isSupportAvailableMode] characteristics is null");
            return false;
        }
        List<CameraCharacteristics.Key<?>> keys = cameraCharacteristics.getKeys();
        if (keys == null) {
            LogHelper.i(TAG, "[isSupportAvailableMode] No keys!");
            return false;
        }
        for (CameraCharacteristics.Key<?> key : keys) {
            if (key.getName().equals(str)) {
                LogHelper.i(TAG, "[isSupportAvailableMode] key: " + str);
                int[] iArr = (int[]) cameraCharacteristics.get(key);
                int length = iArr.length;
                for (int i2 = 0; i2 < length; i2++) {
                    if (iArr[i2] == i) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static void startService(Context context, Intent intent) {
        if (context == null || intent == null) {
            LogHelper.e(TAG, "isServiceRun mContext = " + context + " intent = " + intent);
            return;
        }
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                context.startService(intent);
            } catch (Exception e) {
                LogHelper.e(TAG, "Start service " + e);
            }
        }
    }

    public static boolean isSpecialKeyCodeEnabled() {
        boolean z = SystemProperties.getInt("mtk.camera.app.keycode.enable", 0) == 1;
        LogHelper.d(TAG, "[isSpecialKeyCodeEnabled] isEnable = " + z);
        return z;
    }

    public static boolean isNeedInitSetting(int i) {
        if (i == 32 || i == 33) {
            return true;
        }
        return false;
    }

    public static void enable4CellRequest(CameraCharacteristics cameraCharacteristics, CaptureRequest.Builder builder) {
        CaptureRequest.Key<int[]> requestKey = getRequestKey(cameraCharacteristics, "com.mediatek.control.capture.remosaicenable");
        if (requestKey != null) {
            builder.set(requestKey, new int[]{1});
        }
    }

    public static boolean isStillCaptureTemplate(CaptureResult captureResult) {
        try {
            if (2 == ((Integer) captureResult.get(CaptureResult.CONTROL_CAPTURE_INTENT)).intValue()) {
                return true;
            }
            return false;
        } catch (Exception e) {
            LogHelper.e(TAG, "[isStillCaptureTemplate] frame = " + captureResult.getFrameNumber());
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public static boolean isStillCaptureTemplate(CaptureRequest captureRequest) {
        try {
            if (2 == ((Integer) captureRequest.get(CaptureRequest.CONTROL_CAPTURE_INTENT)).intValue()) {
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public static boolean hasFocuser(CameraCharacteristics cameraCharacteristics) {
        boolean z = false;
        if (cameraCharacteristics == null) {
            LogHelper.w(TAG, "[hasFocuser] characteristics is null");
            return false;
        }
        Float f = (Float) cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
        if (f != null && f.floatValue() > 0.0f) {
            return true;
        }
        int[] iArr = (int[]) cameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        if (iArr == null) {
            return false;
        }
        int length = iArr.length;
        int i = 0;
        while (true) {
            if (i < length) {
                switch (iArr[i]) {
                    case Camera2Proxy.TEMPLATE_PREVIEW:
                    case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                    case Camera2Proxy.TEMPLATE_RECORD:
                    case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                        z = true;
                        break;
                    default:
                        i++;
                        break;
                }
            }
        }
        LogHelper.d(TAG, "[hasFocuser] hasFocuser = " + z);
        return z;
    }
}
