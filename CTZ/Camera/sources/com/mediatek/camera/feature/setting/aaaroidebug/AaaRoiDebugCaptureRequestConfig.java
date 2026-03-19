package com.mediatek.camera.feature.setting.aaaroidebug;

import android.graphics.Rect;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.view.Surface;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.portability.SystemProperties;
import java.util.List;
import junit.framework.Assert;

public class AaaRoiDebugCaptureRequestConfig implements ICameraSetting.ICaptureRequestConfigure {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(AaaRoiDebugCaptureRequestConfig.class.getSimpleName());
    private static boolean sIsLogRois;
    private CaptureResult.Key<int[]> mAeRangeResultKey;
    private CaptureResult.Key<int[]> mAfRangeResultKey;
    private CaptureResult.Key<int[]> mAwbRangeResultKey;
    private DebugInfoListener mDebugInfoListener;
    private CameraCaptureSession.CaptureCallback mPreviewCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult) {
            super.onCaptureCompleted(cameraCaptureSession, captureRequest, totalCaptureResult);
            if (AaaRoiDebugCaptureRequestConfig.this.mAeRangeResultKey == null && AaaRoiDebugCaptureRequestConfig.this.mAfRangeResultKey == null && AaaRoiDebugCaptureRequestConfig.this.mAwbRangeResultKey == null) {
                return;
            }
            Assert.assertNotNull(totalCaptureResult);
            Rect[] rectArrCovertRoiToRectArray = null;
            Rect[] rectArrCovertRoiToRectArray2 = null;
            Rect[] rectArrCovertRoiToRectArray3 = null;
            for (CaptureResult.Key<?> key : totalCaptureResult.getKeys()) {
                if (AaaRoiDebugCaptureRequestConfig.this.mAeRangeResultKey != null && key.getName().equals("com.mediatek.3afeature.aeroi")) {
                    rectArrCovertRoiToRectArray = AaaRoiDebugCaptureRequestConfig.this.covertRoiToRectArray((int[]) totalCaptureResult.get(AaaRoiDebugCaptureRequestConfig.this.mAeRangeResultKey));
                }
                if (AaaRoiDebugCaptureRequestConfig.this.mAfRangeResultKey != null && key.getName().equals("com.mediatek.3afeature.afroi")) {
                    rectArrCovertRoiToRectArray2 = AaaRoiDebugCaptureRequestConfig.this.covertRoiToRectArray((int[]) totalCaptureResult.get(AaaRoiDebugCaptureRequestConfig.this.mAfRangeResultKey));
                }
                if (AaaRoiDebugCaptureRequestConfig.this.mAwbRangeResultKey != null && key.getName().equals("com.mediatek.3afeature.awbroi")) {
                    rectArrCovertRoiToRectArray3 = AaaRoiDebugCaptureRequestConfig.this.covertRoiToRectArray((int[]) totalCaptureResult.get(AaaRoiDebugCaptureRequestConfig.this.mAwbRangeResultKey));
                }
            }
            if (AaaRoiDebugCaptureRequestConfig.sIsLogRois) {
                LogHelper.d(AaaRoiDebugCaptureRequestConfig.TAG, "[onCaptureCompleted] aeRois = " + AaaRoiDebugCaptureRequestConfig.this.covertRectArrayToString(rectArrCovertRoiToRectArray));
                LogHelper.d(AaaRoiDebugCaptureRequestConfig.TAG, "[onCaptureCompleted] afRois = " + AaaRoiDebugCaptureRequestConfig.this.covertRectArrayToString(rectArrCovertRoiToRectArray2));
                LogHelper.d(AaaRoiDebugCaptureRequestConfig.TAG, "[onCaptureCompleted] awbRois = " + AaaRoiDebugCaptureRequestConfig.this.covertRectArrayToString(rectArrCovertRoiToRectArray3));
            }
            if (AaaRoiDebugCaptureRequestConfig.this.mDebugInfoListener != null) {
                AaaRoiDebugCaptureRequestConfig.this.mDebugInfoListener.onRangeUpdate(rectArrCovertRoiToRectArray, rectArrCovertRoiToRectArray2, rectArrCovertRoiToRectArray3, (Rect) totalCaptureResult.get(CaptureResult.SCALER_CROP_REGION));
            }
        }
    };

    public interface DebugInfoListener {
        void onRangeUpdate(Rect[] rectArr, Rect[] rectArr2, Rect[] rectArr3, Rect rect);
    }

    static {
        sIsLogRois = SystemProperties.getInt("vendor.mtk.camera.app.3a.debug.log", 0) == 1;
    }

    public void setDebugInfoListener(DebugInfoListener debugInfoListener) {
        this.mDebugInfoListener = debugInfoListener;
    }

    @Override
    public void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics) {
        List<CaptureResult.Key<?>> availableCaptureResultKeys = cameraCharacteristics.getAvailableCaptureResultKeys();
        for (int i = 0; i < availableCaptureResultKeys.size(); i++) {
            CaptureResult.Key<int[]> key = (CaptureResult.Key) availableCaptureResultKeys.get(i);
            if ("com.mediatek.3afeature.aeroi".equals(key.getName())) {
                this.mAeRangeResultKey = key;
            } else if ("com.mediatek.3afeature.afroi".equals(key.getName())) {
                this.mAfRangeResultKey = key;
            } else if ("com.mediatek.3afeature.awbroi".equals(key.getName())) {
                this.mAwbRangeResultKey = key;
            }
        }
        LogHelper.d(TAG, "[setCameraCharacteristics] mAeRangeResultKey = " + this.mAeRangeResultKey + ", mAfRangeResultKey = " + this.mAfRangeResultKey + ", mAwbRangeResultKey = " + this.mAwbRangeResultKey);
    }

    @Override
    public void configCaptureRequest(CaptureRequest.Builder builder) {
    }

    @Override
    public void configSessionSurface(List<Surface> list) {
    }

    @Override
    public CameraCaptureSession.CaptureCallback getRepeatingCaptureCallback() {
        return this.mPreviewCallback;
    }

    @Override
    public void sendSettingChangeRequest() {
    }

    @Override
    public Surface configRawSurface() {
        return null;
    }

    private String covertRectArrayToString(Rect[] rectArr) {
        if (rectArr == null || rectArr.length == 0) {
            return "null";
        }
        String str = "";
        for (Rect rect : rectArr) {
            if (!str.equals("")) {
                str = str + ",";
            }
            str = rect == null ? str + "null" : str + rect;
        }
        return str;
    }

    private Rect[] covertRoiToRectArray(int[] iArr) {
        if (iArr == null || iArr.length < 7 || iArr.length != (iArr[1] * 5) + 2) {
            return null;
        }
        int i = iArr[1];
        Rect[] rectArr = new Rect[i];
        for (int i2 = 0; i2 < i; i2++) {
            int i3 = (i2 * 5) + 2;
            rectArr[i2] = new Rect(iArr[i3], iArr[i3 + 1], iArr[i3 + 2], iArr[i3 + 3]);
        }
        return rectArr;
    }
}
