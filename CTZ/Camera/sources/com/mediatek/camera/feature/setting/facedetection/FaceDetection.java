package com.mediatek.camera.feature.setting.facedetection;

import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.mode.ICameraMode;
import com.mediatek.camera.common.relation.StatusMonitor;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.SettingBase;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.feature.setting.facedetection.IFaceConfig;
import java.util.ArrayList;
import java.util.List;

public class FaceDetection extends SettingBase {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(FaceDetection.class.getSimpleName());
    private StatusMonitor.StatusResponder mFaceExistStatusResponder;
    private Handler mModeHandler;
    private Size mPreviewSize;
    private ICameraSetting.ISettingChangeRequester mSettingChangeRequester;
    private FaceViewCtrl mFaceViewCtrl = new FaceViewCtrl();
    private FaceDeviceCtrl mFaceDeviceCtrl = new FaceDeviceCtrl();
    private List<String> mSupportValues = new ArrayList();
    private boolean mIsFaceExistLastTime = false;
    private IApp.OnOrientationChangeListener mOrientationListener = new IApp.OnOrientationChangeListener() {
        @Override
        public void onOrientationChanged(int i) {
            FaceDetection.this.mModeHandler.post(new Runnable() {
                @Override
                public void run() {
                    if ("on".equals(FaceDetection.this.getValue())) {
                        FaceDetection.this.updateFaceDisplayOrientation();
                        FaceDetection.this.updateImageOrientation();
                    }
                }
            });
        }
    };
    private IAppUiListener.OnPreviewAreaChangedListener mPreviewAreaChangedListener = new IAppUiListener.OnPreviewAreaChangedListener() {
        @Override
        public void onPreviewAreaChanged(final RectF rectF, final Size size) {
            FaceDetection.this.mModeHandler.post(new Runnable() {
                @Override
                public void run() {
                    FaceDetection.this.mPreviewSize = size;
                    FaceDetection.this.mFaceViewCtrl.onPreviewAreaChanged(rectF);
                }
            });
        }
    };
    private IFaceConfig.OnDetectedFaceUpdateListener mOnDetectedFaceUpdateListener = new IFaceConfig.OnDetectedFaceUpdateListener() {
        @Override
        public void onDetectedFaceUpdate(Face[] faceArr) {
            FaceDetection.this.mFaceViewCtrl.onDetectedFaceUpdate(faceArr);
            boolean z = faceArr != null && faceArr.length > 0;
            if (z != FaceDetection.this.mIsFaceExistLastTime) {
                if (z) {
                    FaceDetection.this.mFaceExistStatusResponder.statusChanged("key_face_exist", String.valueOf(true));
                } else {
                    FaceDetection.this.mFaceExistStatusResponder.statusChanged("key_face_exist", String.valueOf(false));
                }
                FaceDetection.this.mIsFaceExistLastTime = z;
            }
        }
    };
    private IFaceConfig.OnFaceValueUpdateListener mOnFaceValueUpdateListener = new IFaceConfig.OnFaceValueUpdateListener() {
        @Override
        public Size onFacePreviewSizeUpdate() {
            return new Size(FaceDetection.this.mPreviewSize.getWidth(), FaceDetection.this.mPreviewSize.getHeight());
        }

        @Override
        public int onUpdateImageOrientation() {
            int iIntValue = Integer.valueOf(((SettingBase) FaceDetection.this).mSettingController.getCameraId()).intValue();
            int jpegRotationFromDeviceSpec = CameraUtil.getJpegRotationFromDeviceSpec(iIntValue, FaceDetection.this.mApp.getGSensorOrientation(), FaceDetection.this.mApp.getActivity());
            LogHelper.d(FaceDetection.TAG, "[onUpdateImageOrientation] camera id = " + iIntValue + ", rotation = " + jpegRotationFromDeviceSpec);
            return jpegRotationFromDeviceSpec;
        }

        @Override
        public void onFaceSettingValueUpdate(boolean z, List<String> list) {
            FaceDetection.this.setSupportedPlatformValues(list);
            FaceDetection.this.setSupportedEntryValues(list);
            FaceDetection.this.setEntryValues(list);
            FaceDetection.this.setValue(FaceDetection.this.getEntryValues().get(0));
            FaceDetection.this.mFaceDeviceCtrl.setDetectedFaceUpdateListener(FaceDetection.this.mOnDetectedFaceUpdateListener);
        }
    };
    private ICameraSetting.PreviewStateCallback mPreviewStateCallback = new ICameraSetting.PreviewStateCallback() {
        @Override
        public void onPreviewStopped() {
            FaceDetection.this.mFaceViewCtrl.onPreviewStatus(false);
            FaceDetection.this.mFaceDeviceCtrl.onPreviewStatus(false);
            FaceDetection.this.mFaceDeviceCtrl.setDetectedFaceUpdateListener(null);
        }

        @Override
        public void onPreviewStarted() {
            FaceDetection.this.mFaceDeviceCtrl.onPreviewStatus(true);
            FaceDetection.this.mFaceDeviceCtrl.setDetectedFaceUpdateListener(FaceDetection.this.mOnDetectedFaceUpdateListener);
            FaceDetection.this.requestFaceDetection();
        }
    };

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, ISettingManager.SettingController settingController) {
        LogHelper.d(TAG, "[init]");
        super.init(iApp, iCameraContext, settingController);
        this.mModeHandler = new Handler(Looper.myLooper());
        this.mFaceDeviceCtrl.init();
        this.mFaceViewCtrl.init(iApp);
        this.mFaceDeviceCtrl.setFaceValueUpdateListener(this.mOnFaceValueUpdateListener);
        iApp.registerOnOrientationChangeListener(this.mOrientationListener);
        iApp.getAppUi().registerOnPreviewAreaChangedListener(this.mPreviewAreaChangedListener);
        initSettingValue();
        updateFaceDisplayOrientation();
        this.mFaceExistStatusResponder = this.mStatusMonitor.getStatusResponder("key_face_exist");
        this.mStatusMonitor.registerValueChangedListener("key_focus_state", this.mFaceViewCtrl);
    }

    @Override
    public void unInit() {
        LogHelper.d(TAG, "[unInit]");
        this.mIsFaceExistLastTime = false;
        this.mFaceViewCtrl.unInit();
        this.mApp.getAppUi().unregisterOnPreviewAreaChangedListener(this.mPreviewAreaChangedListener);
        this.mApp.unregisterOnOrientationChangeListener(this.mOrientationListener);
        this.mStatusMonitor.unregisterValueChangedListener("key_focus_state", this.mFaceViewCtrl);
    }

    @Override
    public void postRestrictionAfterInitialized() {
    }

    @Override
    public ICameraSetting.SettingType getSettingType() {
        return ICameraSetting.SettingType.PHOTO_AND_VIDEO;
    }

    @Override
    public String getKey() {
        return "key_face_detection";
    }

    @Override
    public ICameraSetting.IParametersConfigure getParametersConfigure() {
        if (this.mSettingChangeRequester == null) {
            this.mSettingChangeRequester = this.mFaceDeviceCtrl.getParametersConfigure(this.mSettingDeviceRequester);
        }
        return (FaceParameterConfig) this.mSettingChangeRequester;
    }

    @Override
    public ICameraSetting.ICaptureRequestConfigure getCaptureRequestConfigure() {
        if (this.mSettingChangeRequester == null) {
            this.mSettingChangeRequester = this.mFaceDeviceCtrl.getCaptureRequestConfigure(this.mSettingDevice2Requester);
        }
        return (FaceCaptureRequestConfig) this.mSettingChangeRequester;
    }

    @Override
    public ICameraSetting.PreviewStateCallback getPreviewStateCallback() {
        return this.mPreviewStateCallback;
    }

    @Override
    public void overrideValues(String str, String str2, List<String> list) {
        super.overrideValues(str, str2, list);
        String value = getValue() == null ? "off" : getValue();
        if (this.mFaceDeviceCtrl.isFaceDetectionStatusChanged(value)) {
            LogHelper.d(TAG, "[overrideValues] curValue = " + value + ", headerKey = " + str);
            this.mFaceDeviceCtrl.updateFaceDetectionStatus(value);
            this.mFaceViewCtrl.enableFaceView("on".equals(value));
            requestFaceDetection();
        }
    }

    @Override
    public void onModeOpened(String str, ICameraMode.ModeType modeType) {
        LogHelper.d(TAG, "[onModeOpened] modeKey = " + str);
    }

    @Override
    public void onModeClosed(String str) {
        LogHelper.d(TAG, "[onModeClosed] modeKey = " + str);
        this.mIsFaceExistLastTime = false;
        if (!str.startsWith("com.mediatek.camera.feature.mode.pip.")) {
            super.onModeClosed(str);
        }
    }

    private void initSettingValue() {
        this.mSupportValues.add("off");
        this.mSupportValues.add("on");
        setSupportedPlatformValues(this.mSupportValues);
        setSupportedEntryValues(this.mSupportValues);
        setEntryValues(this.mSupportValues);
        setValue(this.mDataStore.getValue(getKey(), "on", getStoreScope()));
    }

    private void updateFaceDisplayOrientation() {
        int iIntValue = Integer.valueOf(this.mSettingController.getCameraId()).intValue();
        this.mFaceViewCtrl.updateFaceDisplayOrientation(CameraUtil.getDisplayOrientationFromDeviceSpec(CameraUtil.getDisplayRotation(this.mApp.getActivity()), iIntValue, this.mApp.getActivity()), iIntValue);
    }

    private void updateImageOrientation() {
        this.mFaceDeviceCtrl.updateImageOrientation();
    }

    private void requestFaceDetection() {
        if (this.mSettingChangeRequester != null) {
            this.mSettingChangeRequester.sendSettingChangeRequest();
        }
    }
}
