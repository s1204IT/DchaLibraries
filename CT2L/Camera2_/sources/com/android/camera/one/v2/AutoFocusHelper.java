package com.android.camera.one.v2;

import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.support.v4.os.EnvironmentCompat;
import com.android.camera.debug.Log;
import com.android.camera.one.OneCamera;
import com.android.camera.one.Settings3A;
import com.android.camera.util.CameraUtil;

public class AutoFocusHelper {
    private static final Log.Tag TAG = new Log.Tag("OneCameraAFHelp");
    private static final int CAMERA2_REGION_WEIGHT = (int) CameraUtil.lerp(0.0f, 1000.0f, Settings3A.getMeteringRegionWeight());
    private static final MeteringRectangle[] ZERO_WEIGHT_3A_REGION = {new MeteringRectangle(0, 0, 0, 0, 0)};

    public static MeteringRectangle[] getZeroWeightRegion() {
        return ZERO_WEIGHT_3A_REGION;
    }

    public static OneCamera.AutoFocusState stateFromCamera2State(int state) {
        switch (state) {
            case 1:
                return OneCamera.AutoFocusState.PASSIVE_SCAN;
            case 2:
                return OneCamera.AutoFocusState.PASSIVE_FOCUSED;
            case 3:
                return OneCamera.AutoFocusState.ACTIVE_SCAN;
            case 4:
                return OneCamera.AutoFocusState.ACTIVE_FOCUSED;
            case 5:
                return OneCamera.AutoFocusState.ACTIVE_UNFOCUSED;
            case 6:
                return OneCamera.AutoFocusState.PASSIVE_UNFOCUSED;
            default:
                return OneCamera.AutoFocusState.INACTIVE;
        }
    }

    public static boolean checkControlAfState(CaptureResult result) {
        boolean missing = result.get(CaptureResult.CONTROL_AF_STATE) == null;
        if (missing) {
            Log.e(TAG, "\n!!!! TotalCaptureResult missing CONTROL_AF_STATE. !!!!\n ");
        }
        return !missing;
    }

    public static boolean checkLensState(CaptureResult result) {
        boolean missing = result.get(CaptureResult.LENS_STATE) == null;
        if (missing) {
            Log.e(TAG, "\n!!!! TotalCaptureResult missing LENS_STATE. !!!!\n ");
        }
        return !missing;
    }

    public static void logExtraFocusInfo(CaptureResult result) {
        if (checkControlAfState(result) && checkLensState(result)) {
            Object tag = result.getRequest().getTag();
            Log.Tag tag2 = TAG;
            Object[] objArr = new Object[4];
            objArr[0] = controlAFStateToString(((Integer) result.get(CaptureResult.CONTROL_AF_STATE)).intValue());
            objArr[1] = result.get(CaptureResult.LENS_FOCUS_DISTANCE);
            objArr[2] = lensStateToString(((Integer) result.get(CaptureResult.LENS_STATE)).intValue());
            objArr[3] = tag == null ? "" : "[" + tag + "]";
            Log.v(tag2, String.format("af_state:%-17s  lens_foc_dist:%.3f  lens_state:%-10s  %s", objArr));
        }
    }

    private static MeteringRectangle[] regionsForNormalizedCoord(float nx, float ny, float fraction, Rect cropRegion, int sensorOrientation) {
        int minCropEdge = Math.min(cropRegion.width(), cropRegion.height());
        int halfSideLength = (int) (0.5f * fraction * minCropEdge);
        PointF nsc = CameraUtil.normalizedSensorCoordsForNormalizedDisplayCoords(nx, ny, sensorOrientation);
        int xCenterSensor = (int) (cropRegion.left + (nsc.x * cropRegion.width()));
        int yCenterSensor = (int) (cropRegion.top + (nsc.y * cropRegion.height()));
        Rect meteringRegion = new Rect(xCenterSensor - halfSideLength, yCenterSensor - halfSideLength, xCenterSensor + halfSideLength, yCenterSensor + halfSideLength);
        meteringRegion.left = CameraUtil.clamp(meteringRegion.left, cropRegion.left, cropRegion.right);
        meteringRegion.top = CameraUtil.clamp(meteringRegion.top, cropRegion.top, cropRegion.bottom);
        meteringRegion.right = CameraUtil.clamp(meteringRegion.right, cropRegion.left, cropRegion.right);
        meteringRegion.bottom = CameraUtil.clamp(meteringRegion.bottom, cropRegion.top, cropRegion.bottom);
        return new MeteringRectangle[]{new MeteringRectangle(meteringRegion, CAMERA2_REGION_WEIGHT)};
    }

    public static MeteringRectangle[] afRegionsForNormalizedCoord(float nx, float ny, Rect cropRegion, int sensorOrientation) {
        return regionsForNormalizedCoord(nx, ny, Settings3A.getAutoFocusRegionWidth(), cropRegion, sensorOrientation);
    }

    public static MeteringRectangle[] aeRegionsForNormalizedCoord(float nx, float ny, Rect cropRegion, int sensorOrientation) {
        return regionsForNormalizedCoord(nx, ny, Settings3A.getMeteringRegionWidth(), cropRegion, sensorOrientation);
    }

    public static MeteringRectangle[] gcamAERegionsForNormalizedCoord(float nx, float ny, Rect cropRegion, int sensorOrientation) {
        return regionsForNormalizedCoord(nx, ny, Settings3A.getGcamMeteringRegionFraction(), cropRegion, sensorOrientation);
    }

    public static Rect cropRegionForZoom(CameraCharacteristics characteristics, float zoom) {
        Rect sensor = (Rect) characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        int xCenter = sensor.width() / 2;
        int yCenter = sensor.height() / 2;
        int xDelta = (int) ((sensor.width() * 0.5f) / zoom);
        int yDelta = (int) ((sensor.height() * 0.5f) / zoom);
        return new Rect(xCenter - xDelta, yCenter - yDelta, xCenter + xDelta, yCenter + yDelta);
    }

    private static String controlAFStateToString(int controlAFState) {
        switch (controlAFState) {
            case 0:
                return "inactive";
            case 1:
                return "passive_scan";
            case 2:
                return "passive_focused";
            case 3:
                return "active_scan";
            case 4:
                return "focus_locked";
            case 5:
                return "not_focus_locked";
            case 6:
                return "passive_unfocused";
            default:
                return EnvironmentCompat.MEDIA_UNKNOWN;
        }
    }

    private static String lensStateToString(int lensState) {
        switch (lensState) {
            case 0:
                return "stationary";
            case 1:
                return "moving";
            default:
                return EnvironmentCompat.MEDIA_UNKNOWN;
        }
    }
}
