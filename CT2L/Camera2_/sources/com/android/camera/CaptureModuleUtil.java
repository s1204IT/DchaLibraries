package com.android.camera;

import android.content.Context;
import android.content.res.Configuration;
import com.android.camera.debug.Log;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.Size;
import java.util.ArrayList;

public class CaptureModuleUtil {
    private static final Log.Tag TAG = new Log.Tag("CaptureModuleUtil");

    public static int getDeviceNaturalOrientation(Context context) {
        Configuration config = context.getResources().getConfiguration();
        int rotation = CameraUtil.getDisplayRotation(context);
        if ((rotation == 0 || rotation == 2) && config.orientation == 2) {
            return 2;
        }
        return ((rotation == 1 || rotation == 3) && config.orientation == 1) ? 2 : 1;
    }

    public static Size getOptimalPreviewSize(Context context, Size[] sizes, double targetRatio) {
        int count = 0;
        for (Size size : sizes) {
            if (size.getHeight() <= 1080) {
                count++;
            }
        }
        ArrayList<Size> camera1Sizes = new ArrayList<>(count);
        for (Size s : sizes) {
            if (s.getHeight() <= 1080) {
                camera1Sizes.add(new Size(s.getWidth(), s.getHeight()));
            }
        }
        int optimalIndex = CameraUtil.getOptimalPreviewSizeIndex(context, camera1Sizes, targetRatio);
        if (optimalIndex == -1) {
            return null;
        }
        Size optimal = camera1Sizes.get(optimalIndex);
        for (Size s2 : sizes) {
            if (s2.getWidth() == optimal.getWidth() && s2.getHeight() == optimal.getHeight()) {
                return s2;
            }
        }
        return null;
    }

    public static Size pickBufferDimensions(Size[] supportedPreviewSizes, double bestPreviewAspectRatio, Context context) {
        boolean swapDimens = CameraUtil.getDisplayRotation(context) % 180 == 90;
        if (getDeviceNaturalOrientation(context) == 1) {
            swapDimens = !swapDimens;
        }
        if (swapDimens) {
            double d = 1.0d / bestPreviewAspectRatio;
        }
        Size pick = getOptimalPreviewSize(context, supportedPreviewSizes, bestPreviewAspectRatio);
        Log.d(TAG, "Picked buffer size: " + pick.toString());
        return pick;
    }
}
