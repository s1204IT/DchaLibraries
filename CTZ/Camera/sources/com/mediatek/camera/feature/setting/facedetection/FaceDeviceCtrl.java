package com.mediatek.camera.feature.setting.facedetection;

import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.feature.setting.facedetection.IFaceConfig;

public class FaceDeviceCtrl {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(FaceDeviceCtrl.class.getSimpleName());
    private FaceCaptureRequestConfig mCaptureRequestConfig;
    private IFaceConfig mFaceConfig;
    private IFaceConfig.OnFaceValueUpdateListener mFaceValueUpdateListener;
    private boolean mIsFaceDetectionSupported;
    private boolean mIsPreviewStarted;
    private FaceParameterConfig mParameterConfig;
    private String mFaceOverrideState = "on";
    private IFacePerformerMonitor mFaceMonitor = new FacePerformerMonitor();

    public interface IFacePerformerMonitor {
        boolean isNeedToStart();

        boolean isNeedToStop();

        void setSupportedStatus(boolean z);
    }

    public void init() {
    }

    public void onPreviewStatus(boolean z) {
        LogHelper.d(TAG, "[onPreviewStatus] isPreviewStarted = " + z);
        this.mIsPreviewStarted = z;
        if (!z && this.mFaceConfig != null) {
            this.mFaceConfig.resetFaceDetectionState();
        }
    }

    public void updateImageOrientation() {
        if (this.mFaceConfig != null) {
            this.mFaceConfig.updateImageOrientation();
        }
    }

    public ICameraSetting.IParametersConfigure getParametersConfigure(ISettingManager.SettingDeviceRequester settingDeviceRequester) {
        if (this.mParameterConfig == null) {
            this.mParameterConfig = new FaceParameterConfig(settingDeviceRequester);
            this.mParameterConfig.setFaceMonitor(this.mFaceMonitor);
            this.mParameterConfig.setFaceValueUpdateListener(this.mFaceValueUpdateListener);
            this.mFaceConfig = this.mParameterConfig;
        }
        return this.mParameterConfig;
    }

    public ICameraSetting.ICaptureRequestConfigure getCaptureRequestConfigure(ISettingManager.SettingDevice2Requester settingDevice2Requester) {
        if (this.mCaptureRequestConfig == null) {
            this.mCaptureRequestConfig = new FaceCaptureRequestConfig(settingDevice2Requester);
            this.mCaptureRequestConfig.setFaceMonitor(this.mFaceMonitor);
            this.mCaptureRequestConfig.setFaceValueUpdateListener(this.mFaceValueUpdateListener);
            this.mFaceConfig = this.mCaptureRequestConfig;
        }
        this.mIsPreviewStarted = true;
        return this.mCaptureRequestConfig;
    }

    public void updateFaceDetectionStatus(String str) {
        this.mFaceOverrideState = str;
    }

    public boolean isFaceDetectionStatusChanged(String str) {
        return !this.mFaceOverrideState.equals(str);
    }

    public void setDetectedFaceUpdateListener(IFaceConfig.OnDetectedFaceUpdateListener onDetectedFaceUpdateListener) {
        if (this.mFaceConfig != null) {
            this.mFaceConfig.setFaceDetectionUpdateListener(onDetectedFaceUpdateListener);
        }
    }

    public void setFaceValueUpdateListener(IFaceConfig.OnFaceValueUpdateListener onFaceValueUpdateListener) {
        this.mFaceValueUpdateListener = onFaceValueUpdateListener;
    }

    private class FacePerformerMonitor implements IFacePerformerMonitor {
        private FacePerformerMonitor() {
        }

        @Override
        public void setSupportedStatus(boolean z) {
            FaceDeviceCtrl.this.mIsFaceDetectionSupported = z;
        }

        @Override
        public boolean isNeedToStart() {
            boolean zEquals = FaceDeviceCtrl.this.mFaceOverrideState.equals("on");
            boolean z = zEquals && FaceDeviceCtrl.this.mIsPreviewStarted && FaceDeviceCtrl.this.mIsFaceDetectionSupported;
            LogHelper.d(FaceDeviceCtrl.TAG, "[isNeedStart]  overrideState = " + zEquals + ", mIsPreviewStarted = " + FaceDeviceCtrl.this.mIsPreviewStarted + ", mIsFaceDetectionSupported = " + FaceDeviceCtrl.this.mIsFaceDetectionSupported + ", needStart = " + z);
            return z;
        }

        @Override
        public boolean isNeedToStop() {
            boolean zEquals = FaceDeviceCtrl.this.mFaceOverrideState.equals("off");
            boolean z = zEquals && FaceDeviceCtrl.this.mIsPreviewStarted && FaceDeviceCtrl.this.mIsFaceDetectionSupported;
            LogHelper.d(FaceDeviceCtrl.TAG, "[isNeedStop]  overrideState = " + zEquals + ", mIsPreviewStarted = " + FaceDeviceCtrl.this.mIsPreviewStarted + ", mIsFaceDetectionSupported = " + FaceDeviceCtrl.this.mIsFaceDetectionSupported + ", needStop = " + z);
            return z;
        }
    }
}
