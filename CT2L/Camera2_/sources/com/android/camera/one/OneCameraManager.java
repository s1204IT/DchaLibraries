package com.android.camera.one;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import com.android.camera.CameraActivity;
import com.android.camera.debug.Log;
import com.android.camera.one.OneCamera;
import com.android.camera.one.v2.OneCameraManagerImpl;
import com.android.camera.util.ApiHelper;
import com.android.camera.util.Size;

public abstract class OneCameraManager {
    private static Log.Tag TAG = new Log.Tag("OneCameraManager");

    public abstract boolean hasCameraFacing(OneCamera.Facing facing);

    public abstract void open(OneCamera.Facing facing, boolean z, Size size, OneCamera.OpenCallback openCallback, Handler handler);

    public static OneCameraManager get(CameraActivity activity) throws OneCameraException {
        return create(activity);
    }

    private static OneCameraManager create(CameraActivity activity) throws OneCameraException {
        CameraManager cameraManager;
        DisplayMetrics displayMetrics = getDisplayMetrics(activity);
        try {
            cameraManager = ApiHelper.HAS_CAMERA_2_API ? (CameraManager) activity.getSystemService("camera") : null;
        } catch (IllegalStateException ex) {
            cameraManager = null;
            Log.e(TAG, "Could not get camera service v2", ex);
        }
        if (cameraManager != null && isCamera2Supported(cameraManager)) {
            int maxMemoryMB = activity.getServices().getMemoryManager().getMaxAllowedNativeMemoryAllocation();
            return new OneCameraManagerImpl(activity.getAndroidContext(), cameraManager, maxMemoryMB, displayMetrics, activity.getSoundPlayer());
        }
        return new com.android.camera.one.v1.OneCameraManagerImpl();
    }

    private static boolean isCamera2Supported(CameraManager cameraManager) throws OneCameraException {
        if (!ApiHelper.HAS_CAMERA_2_API) {
            return false;
        }
        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            if (cameraIds.length == 0) {
                throw new OneCameraException("Camera 2 API supported but no devices available.");
            }
            String id = cameraIds[0];
            return ((Integer) cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)).intValue() != 2;
        } catch (CameraAccessException e) {
            Log.e(TAG, "Could not access camera to determine hardware-level API support.");
            return false;
        }
    }

    private static DisplayMetrics getDisplayMetrics(Context context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService("window");
        if (wm != null) {
            DisplayMetrics displayMetrics2 = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(displayMetrics2);
            return displayMetrics2;
        }
        return displayMetrics;
    }
}
