package com.mediatek.camera.feature.setting.facedetection;

import android.annotation.TargetApi;
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
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.CoordinatesTransform;
import com.mediatek.camera.feature.setting.facedetection.FaceDeviceCtrl;
import com.mediatek.camera.feature.setting.facedetection.IFaceConfig;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import junit.framework.Assert;

@TargetApi(21)
public class FaceCaptureRequestConfig implements ICameraSetting.ICaptureRequestConfigure, IFaceConfig {
    private CaptureRequest.Key<int[]> mFaceForce3aModesRequestKey;
    private FaceDeviceCtrl.IFacePerformerMonitor mFaceMonitor;
    private boolean mIsRequestConfigSupported;
    private boolean mIsVendorFace3ASupported;
    private IFaceConfig.OnDetectedFaceUpdateListener mOnDetectedFaceUpdateListener;
    private IFaceConfig.OnFaceValueUpdateListener mOnFaceValueUpdateListener;
    private static final LogUtil.Tag TAG = new LogUtil.Tag(FaceCaptureRequestConfig.class.getSimpleName());
    private static final int[] FACE_FORCE_FACE_3A_OFF = {0};
    private static final int[] FACE_FORCE_FACE_3A_ON = {1};
    private List<String> mSupportValueList = new ArrayList();
    private CameraCaptureSession.CaptureCallback mPreviewCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult) {
            super.onCaptureCompleted(cameraCaptureSession, captureRequest, totalCaptureResult);
            Assert.assertNotNull(totalCaptureResult);
            android.hardware.camera2.params.Face[] faceArr = (android.hardware.camera2.params.Face[]) totalCaptureResult.get(CaptureResult.STATISTICS_FACES);
            Rect rect = (Rect) totalCaptureResult.get(CaptureResult.SCALER_CROP_REGION);
            Rect[] previewRect = FaceCaptureRequestConfig.this.getPreviewRect(faceArr, rect);
            if (FaceCaptureRequestConfig.this.mOnDetectedFaceUpdateListener != null) {
                FaceCaptureRequestConfig.this.mOnDetectedFaceUpdateListener.onDetectedFaceUpdate(FaceCaptureRequestConfig.this.getFaces(faceArr, previewRect, rect));
            }
        }
    };

    public FaceCaptureRequestConfig(ISettingManager.SettingDevice2Requester settingDevice2Requester) {
    }

    public void setFaceMonitor(FaceDeviceCtrl.IFacePerformerMonitor iFacePerformerMonitor) {
        this.mFaceMonitor = iFacePerformerMonitor;
    }

    @Override
    public void updateImageOrientation() {
    }

    @Override
    public void resetFaceDetectionState() {
    }

    @Override
    public void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics) {
        this.mIsRequestConfigSupported = isFaceDetectionSupported(cameraCharacteristics);
        this.mFaceMonitor.setSupportedStatus(this.mIsRequestConfigSupported);
        if (this.mIsRequestConfigSupported) {
            this.mSupportValueList.clear();
            this.mSupportValueList.add("on");
            this.mSupportValueList.add("off");
            this.mIsVendorFace3ASupported = isFace3ASupported(cameraCharacteristics);
        }
        LogHelper.d(TAG, "[setCameraCharacteristics] mIsRequestConfigSupported = " + this.mIsRequestConfigSupported + ", mIsVendorFace3ASupported = " + this.mIsVendorFace3ASupported);
        this.mOnFaceValueUpdateListener.onFaceSettingValueUpdate(this.mIsRequestConfigSupported, this.mSupportValueList);
    }

    @Override
    public void configCaptureRequest(CaptureRequest.Builder builder) {
        if (builder == null) {
            LogHelper.d(TAG, "[configCaptureRequest] captureBuilder is null");
            return;
        }
        if (CameraUtil.isStillCaptureTemplate(builder.build())) {
            LogHelper.i(TAG, "[configCaptureRequest] capture request not has face dection.");
            return;
        }
        if (this.mFaceMonitor.isNeedToStart()) {
            LogHelper.i(TAG, "[configCaptureRequest] start face detection, this: " + this);
            builder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, 1);
            if (this.mIsVendorFace3ASupported) {
                builder.set(this.mFaceForce3aModesRequestKey, FACE_FORCE_FACE_3A_ON);
            }
        }
        if (this.mFaceMonitor.isNeedToStop()) {
            LogHelper.i(TAG, "[configCaptureRequest] stop face detection");
            builder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, 0);
            if (this.mIsVendorFace3ASupported) {
                builder.set(this.mFaceForce3aModesRequestKey, FACE_FORCE_FACE_3A_OFF);
            }
        }
    }

    @Override
    public void configSessionSurface(List<Surface> list) {
    }

    @Override
    public Surface configRawSurface() {
        return null;
    }

    @Override
    public CameraCaptureSession.CaptureCallback getRepeatingCaptureCallback() {
        return this.mPreviewCallback;
    }

    @Override
    public void sendSettingChangeRequest() {
    }

    @Override
    public void setFaceDetectionUpdateListener(IFaceConfig.OnDetectedFaceUpdateListener onDetectedFaceUpdateListener) {
        this.mOnDetectedFaceUpdateListener = onDetectedFaceUpdateListener;
    }

    public void setFaceValueUpdateListener(IFaceConfig.OnFaceValueUpdateListener onFaceValueUpdateListener) {
        this.mOnFaceValueUpdateListener = onFaceValueUpdateListener;
    }

    public static boolean isFaceDetectionSupported(CameraCharacteristics cameraCharacteristics) {
        int iIntValue;
        try {
            iIntValue = ((Integer) cameraCharacteristics.get(CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT)).intValue();
            try {
                LogHelper.d(TAG, "[isFaceDetectionSupported] faceNum = " + iIntValue);
            } catch (IllegalArgumentException e) {
                LogHelper.e(TAG, "[isFaceDetectionSupported] IllegalArgumentException");
            }
        } catch (IllegalArgumentException e2) {
            iIntValue = 0;
        }
        return iIntValue > 0;
    }

    private boolean isFace3ASupported(CameraCharacteristics cameraCharacteristics) {
        Iterator<CaptureRequest.Key<?>> it = cameraCharacteristics.getAvailableCaptureRequestKeys().iterator();
        while (it.hasNext()) {
            CaptureRequest.Key<int[]> key = (CaptureRequest.Key) it.next();
            if ("com.mediatek.facefeature.forceface3a".equals(key.getName())) {
                this.mFaceForce3aModesRequestKey = key;
                return true;
            }
        }
        return false;
    }

    private Face[] getFaces(android.hardware.camera2.params.Face[] faceArr, Rect[] rectArr, Rect rect) {
        if (faceArr == null) {
            return null;
        }
        if (faceArr != null && faceArr.length == 0) {
            return null;
        }
        Face[] faceArr2 = new Face[faceArr.length];
        for (int i = 0; i < faceArr.length; i++) {
            Face face = new Face();
            face.id = faceArr[i].getId();
            face.score = faceArr[i].getScore();
            face.cropRegion = rect;
            face.rect = rectArr[i];
            faceArr2[i] = face;
        }
        return faceArr2;
    }

    private Rect[] getPreviewRect(android.hardware.camera2.params.Face[] faceArr, Rect rect) {
        if (faceArr == null) {
            return null;
        }
        if (faceArr != null && faceArr.length == 0) {
            return null;
        }
        Rect[] rectArr = new Rect[faceArr.length];
        for (int i = 0; i < faceArr.length; i++) {
            rectArr[i] = CoordinatesTransform.sensorToNormalizedPreview(faceArr[i].getBounds(), this.mOnFaceValueUpdateListener.onFacePreviewSizeUpdate().getWidth(), this.mOnFaceValueUpdateListener.onFacePreviewSizeUpdate().getHeight(), rect);
        }
        return rectArr;
    }
}
