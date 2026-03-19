package com.mediatek.camera.feature.setting.facedetection;

import android.hardware.Camera;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v1.CameraProxy;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.feature.setting.facedetection.FaceDeviceCtrl;
import com.mediatek.camera.feature.setting.facedetection.IFaceConfig;
import java.util.ArrayList;
import java.util.List;

public class FaceParameterConfig implements ICameraSetting.IParametersConfigure, IFaceConfig {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(FaceParameterConfig.class.getSimpleName());
    private FaceDeviceCtrl.IFacePerformerMonitor mFaceMonitor;
    private boolean mIsFaceDetectionStarted;
    private boolean mIsSupported;
    private IFaceConfig.OnDetectedFaceUpdateListener mOnDetectedFaceUpdateListener;
    private IFaceConfig.OnFaceValueUpdateListener mOnFaceValueUpdateListener;
    private ISettingManager.SettingDeviceRequester mSettingDeviceRequester;
    private List<String> mSupportValueList = new ArrayList();
    private Camera.FaceDetectionListener mFaceDetectionListener = new Camera.FaceDetectionListener() {
        @Override
        public void onFaceDetection(Camera.Face[] faceArr, Camera camera) {
            if (FaceParameterConfig.this.mOnDetectedFaceUpdateListener != null) {
                FaceParameterConfig.this.mOnDetectedFaceUpdateListener.onDetectedFaceUpdate(FaceParameterConfig.this.getFaces(faceArr));
            }
        }
    };

    public FaceParameterConfig(ISettingManager.SettingDeviceRequester settingDeviceRequester) {
        this.mSettingDeviceRequester = settingDeviceRequester;
    }

    @Override
    public void setOriginalParameters(Camera.Parameters parameters) {
        this.mIsSupported = parameters.getMaxNumDetectedFaces() > 0;
        this.mFaceMonitor.setSupportedStatus(this.mIsSupported);
        if (this.mIsSupported) {
            this.mSupportValueList.clear();
            this.mSupportValueList.add("on");
            this.mSupportValueList.add("off");
        }
        this.mOnFaceValueUpdateListener.onFaceSettingValueUpdate(this.mIsSupported, this.mSupportValueList);
    }

    @Override
    public boolean configParameters(Camera.Parameters parameters) {
        int iOnUpdateImageOrientation = this.mOnFaceValueUpdateListener.onUpdateImageOrientation();
        parameters.setRotation(iOnUpdateImageOrientation);
        LogHelper.d(TAG, "[configParameters] setRotation as " + iOnUpdateImageOrientation);
        return false;
    }

    @Override
    public void configCommand(CameraProxy cameraProxy) {
        if (this.mFaceMonitor.isNeedToStart()) {
            if (this.mIsFaceDetectionStarted) {
                LogHelper.d(TAG, "[configCommand] already started, return");
                return;
            }
            int iOnUpdateImageOrientation = this.mOnFaceValueUpdateListener.onUpdateImageOrientation();
            LogHelper.i(TAG, "[configCommand] start face detection, orientation = " + iOnUpdateImageOrientation + ", this = " + this);
            Camera.Parameters parameters = cameraProxy.getParameters();
            if (parameters != null) {
                parameters.setRotation(iOnUpdateImageOrientation);
                cameraProxy.setParameters(parameters);
            }
            cameraProxy.setFaceDetectionListener(this.mFaceDetectionListener);
            cameraProxy.startFaceDetection();
            this.mIsFaceDetectionStarted = true;
        }
        if (this.mFaceMonitor.isNeedToStop()) {
            if (!this.mIsFaceDetectionStarted) {
                LogHelper.i(TAG, "[configCommand] already stopped, return");
                return;
            }
            LogHelper.i(TAG, "[configCommand] stop face detection, this = " + this);
            cameraProxy.setFaceDetectionListener(null);
            this.mFaceDetectionListener.onFaceDetection(null, null);
            cameraProxy.stopFaceDetection();
            this.mIsFaceDetectionStarted = false;
        }
    }

    @Override
    public void sendSettingChangeRequest() {
        this.mSettingDeviceRequester.requestChangeCommand("key_face_detection");
    }

    public void setFaceMonitor(FaceDeviceCtrl.IFacePerformerMonitor iFacePerformerMonitor) {
        this.mFaceMonitor = iFacePerformerMonitor;
    }

    @Override
    public void updateImageOrientation() {
        LogHelper.d(TAG, "[updateImageOrientation]");
        this.mSettingDeviceRequester.requestChangeSettingValue("key_face_detection");
    }

    @Override
    public void resetFaceDetectionState() {
        LogHelper.i(TAG, "[resetFaceDetectionState]");
        this.mIsFaceDetectionStarted = false;
    }

    @Override
    public void setFaceDetectionUpdateListener(IFaceConfig.OnDetectedFaceUpdateListener onDetectedFaceUpdateListener) {
        this.mOnDetectedFaceUpdateListener = onDetectedFaceUpdateListener;
    }

    public void setFaceValueUpdateListener(IFaceConfig.OnFaceValueUpdateListener onFaceValueUpdateListener) {
        this.mOnFaceValueUpdateListener = onFaceValueUpdateListener;
    }

    private Face[] getFaces(Camera.Face[] faceArr) {
        if (faceArr == null || (faceArr != null && faceArr.length == 0)) {
            LogHelper.e(TAG, "[getFaces] no faces");
            return null;
        }
        Face[] faceArr2 = new Face[faceArr.length];
        for (int i = 0; i < faceArr.length; i++) {
            Face face = new Face();
            face.id = faceArr[i].id;
            face.score = faceArr[i].score;
            face.rect = faceArr[i].rect;
            faceArr2[i] = face;
        }
        return faceArr2;
    }
}
