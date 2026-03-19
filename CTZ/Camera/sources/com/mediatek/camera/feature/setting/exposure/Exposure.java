package com.mediatek.camera.feature.setting.exposure;

import android.os.RemoteException;
import android.provider.Settings;
import android.view.IWindowManager;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.mode.ICameraMode;
import com.mediatek.camera.common.relation.StatusMonitor;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.SettingBase;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.feature.setting.exposure.ExposureView;
import com.mediatek.camera.feature.setting.exposure.IExposure;
import com.mediatek.camera.portability.pq.PictureQuality;
import java.util.List;

public class Exposure extends SettingBase implements IAppUiListener.OnGestureListener, IAppUiListener.OnShutterButtonListener, ExposureView.ExposureViewChangedListener, IExposure {
    private IExposure.Listener mExposureListener;
    private ExposureViewController mExposureViewController;
    private boolean mPreviewStarted;
    private ICameraSetting.ISettingChangeRequester mSettingChangeRequester;
    private LogUtil.Tag mTag;
    private StatusMonitor.StatusResponder mViewStatusResponder;
    private ICameraMode.ModeType mCurrentModeType = ICameraMode.ModeType.PHOTO;
    private String mLastModeState = "unknown";
    private int mCompensationOrientation = 0;
    private int mDefaultBrightNess = 0;
    private boolean mIsPanelOn = false;
    private int mCurrESSLEDMinStep = 0;
    private int mCurrESSOLEDMinStep = 0;
    private IApp.OnOrientationChangeListener mOrientationListener = new IApp.OnOrientationChangeListener() {
        @Override
        public void onOrientationChanged(int i) {
            Exposure.this.mCompensationOrientation = i + CameraUtil.getDisplayRotation(((SettingBase) Exposure.this).mActivity);
            LogHelper.d(Exposure.this.mTag, "[onOrientationChanged] mCompensationOrientation " + Exposure.this.mCompensationOrientation);
            Exposure.this.mExposureViewController.setOrientation(Exposure.this.mCompensationOrientation);
        }
    };
    private ICameraSetting.PreviewStateCallback mPreviewStateCallback = new ICameraSetting.PreviewStateCallback() {
        @Override
        public void onPreviewStopped() {
            LogHelper.d(Exposure.this.mTag, "[onPreviewStopped]");
            Exposure.this.mPreviewStarted = false;
            Exposure.this.mExposureViewController.setViewEnabled(false);
        }

        @Override
        public void onPreviewStarted() {
            LogHelper.d(Exposure.this.mTag, "[onPreviewStarted]");
            Exposure.this.mPreviewStarted = true;
            Exposure.this.mExposureViewController.setViewEnabled(true);
            if (!Exposure.this.hasDisableEvReset()) {
                Exposure.this.mExposureListener.updateEv(0);
            }
        }
    };
    private StatusMonitor.StatusChangeListener mStatusChangeListener = new StatusMonitor.StatusChangeListener() {
        @Override
        public void onStatusChanged(String str, String str2) {
            byte b;
            LogHelper.d(Exposure.this.mTag, "[onStatusChanged] + key " + str + ",value " + str2);
            int iHashCode = str.hashCode();
            if (iHashCode != -1115955062) {
                if (iHashCode != -819156918) {
                    b = (iHashCode == 3941910 && str.equals("key_video_status")) ? (byte) 1 : (byte) -1;
                } else if (str.equals("key_continuous_shot")) {
                    b = 2;
                }
            } else if (str.equals("key_focus_state")) {
                b = 0;
            }
            switch (b) {
                case 0:
                    if (!Exposure.this.hasDisableEvReset() && str2.equals("PASSIVE_SCAN") && Exposure.this.getValue() != String.valueOf(0)) {
                        if (Exposure.this.mExposureListener != null) {
                            Exposure.this.mExposureListener.updateEv(0);
                        }
                        if (Exposure.this.mSettingChangeRequester != null) {
                            Exposure.this.mSettingChangeRequester.sendSettingChangeRequest();
                        }
                    }
                    break;
                case Camera2Proxy.TEMPLATE_PREVIEW:
                    if ("recording".equals(str2)) {
                        ((ExposureCaptureRequestConfigure) Exposure.this.mSettingChangeRequester).changeFlashToTorchByAeState(true);
                    } else if ("preview".equals(str2)) {
                        ((ExposureCaptureRequestConfigure) Exposure.this.mSettingChangeRequester).changeFlashToTorchByAeState(false);
                    }
                    break;
                case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                    if ("start".equals(str2)) {
                        ((ExposureCaptureRequestConfigure) Exposure.this.mSettingChangeRequester).changeFlashToTorchByAeState(true);
                    } else if ("stop".equals(str2)) {
                        ((ExposureCaptureRequestConfigure) Exposure.this.mSettingChangeRequester).changeFlashToTorchByAeState(false);
                    }
                    break;
            }
        }
    };

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, ISettingManager.SettingController settingController) {
        super.init(iApp, iCameraContext, settingController);
        this.mTag = new LogUtil.Tag(Exposure.class.getSimpleName() + "-" + settingController.getCameraId());
        LogHelper.d(this.mTag, "[init] + ");
        this.mExposureViewController = new ExposureViewController(this.mApp, this);
        this.mAppUi.registerGestureListener(this, 9);
        this.mApp.registerOnOrientationChangeListener(this.mOrientationListener);
        this.mApp.getAppUi().registerOnShutterButtonListener(this, 30);
        this.mViewStatusResponder = this.mStatusMonitor.getStatusResponder("key_exposure_view");
        this.mStatusMonitor.registerValueChangedListener("key_focus_state", this.mStatusChangeListener);
        this.mStatusMonitor.registerValueChangedListener("key_video_status", this.mStatusChangeListener);
        this.mStatusMonitor.registerValueChangedListener("key_continuous_shot", this.mStatusChangeListener);
        this.mCompensationOrientation = this.mApp.getGSensorOrientation() + CameraUtil.getDisplayRotation(this.mActivity);
        this.mDefaultBrightNess = getScreenBrightness().intValue();
        LogHelper.d(this.mTag, "[init] - mCompensationOrientation " + this.mCompensationOrientation);
    }

    @Override
    public void unInit() {
        LogHelper.d(this.mTag, "[unInit] + ");
        this.mLastModeState = "unknown";
        this.mAppUi.unregisterGestureListener(this);
        this.mApp.getAppUi().unregisterOnShutterButtonListener(this);
        this.mApp.unregisterOnOrientationChangeListener(this.mOrientationListener);
        this.mStatusMonitor.unregisterValueChangedListener("key_video_status", this.mStatusChangeListener);
        this.mStatusMonitor.unregisterValueChangedListener("key_focus_state", this.mStatusChangeListener);
        this.mStatusMonitor.unregisterValueChangedListener("key_continuous_shot", this.mStatusChangeListener);
        this.mCurrentModeType = ICameraMode.ModeType.PHOTO;
        LogHelper.d(this.mTag, "[unInit] - ");
    }

    @Override
    public void onModeOpened(String str, ICameraMode.ModeType modeType) {
        LogHelper.d(this.mTag, "[onModeOpened] modeKey " + str + ",modeType " + modeType);
        this.mCurrentModeType = modeType;
    }

    @Override
    public void onModeClosed(String str) {
        if (this.mExposureListener != null) {
            this.mExposureListener.updateEv(0);
            this.mExposureListener.setAeLock(false);
        }
        if (this.mIsPanelOn) {
            setPanel(false, -1);
        }
        this.mLastModeState = "unknown";
    }

    @Override
    public void postRestrictionAfterInitialized() {
    }

    @Override
    public void overrideValues(String str, String str2, List<String> list) {
        LogHelper.d(this.mTag, "[overrideValues] + headerKey = " + str + " ,currentValue = " + str2 + ",supportValues " + list);
        if ("exposure-lock".equals(str2)) {
            if (Boolean.parseBoolean(list.get(0))) {
                doAeLock();
                return;
            }
            doAeUnLock();
            if (this.mExposureListener != null && getEntryValues().size() > 1 && getValue() != null && !getValue().equals("0")) {
                this.mExposureListener.updateEv(0);
                if (this.mSettingChangeRequester != null) {
                    this.mSettingChangeRequester.sendSettingChangeRequest();
                }
            }
            if (this.mExposureViewController != null) {
                this.mExposureViewController.resetExposureView();
                return;
            }
            return;
        }
        super.overrideValues(str, str2, list);
        this.mExposureViewController.resetExposureView();
        if (getValue() != null) {
            this.mExposureListener.overrideExposureValue(getValue(), getEntryValues());
        }
    }

    @Override
    public ICameraSetting.SettingType getSettingType() {
        return ICameraSetting.SettingType.PHOTO_AND_VIDEO;
    }

    @Override
    public String getKey() {
        return "key_exposure";
    }

    @Override
    public ICameraSetting.IParametersConfigure getParametersConfigure() {
        if (this.mSettingChangeRequester == null) {
            this.mSettingChangeRequester = new ExposureParameterConfigure(this, this.mSettingDeviceRequester);
            this.mExposureListener = (IExposure.Listener) this.mSettingChangeRequester;
        }
        return (ExposureParameterConfigure) this.mSettingChangeRequester;
    }

    @Override
    public ICameraSetting.ICaptureRequestConfigure getCaptureRequestConfigure() {
        if (this.mSettingChangeRequester == null) {
            this.mSettingChangeRequester = new ExposureCaptureRequestConfigure(this, this.mSettingDevice2Requester, this.mActivity.getApplicationContext());
            this.mExposureListener = (IExposure.Listener) this.mSettingChangeRequester;
        }
        this.mPreviewStarted = true;
        return (ExposureCaptureRequestConfigure) this.mSettingChangeRequester;
    }

    @Override
    public ICameraSetting.PreviewStateCallback getPreviewStateCallback() {
        return this.mPreviewStateCallback;
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onUp(MotionEvent motionEvent) {
        if (!isEnvironmentReady() || !this.mExposureViewController.needUpdateExposureView()) {
            return false;
        }
        this.mExposureViewController.onTrackingTouch(false);
        return false;
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
        if (!isEnvironmentReady() || !this.mExposureViewController.needUpdateExposureView()) {
            return false;
        }
        if (Math.abs(motionEvent2.getX() - motionEvent.getX()) > Math.abs(motionEvent2.getY() - motionEvent.getY())) {
            if (this.mCompensationOrientation == 90 || this.mCompensationOrientation == 270) {
                this.mExposureViewController.onVerticalScroll(motionEvent2, f);
                return true;
            }
        } else if (this.mCompensationOrientation == 0 || this.mCompensationOrientation == 180) {
            this.mExposureViewController.onVerticalScroll(motionEvent2, f2);
            return true;
        }
        return false;
    }

    @Override
    public boolean onSingleTapUp(float f, float f2) {
        return !isEnvironmentReady() ? false : false;
    }

    @Override
    public boolean onSingleTapConfirmed(float f, float f2) {
        return false;
    }

    @Override
    public boolean onDoubleTap(float f, float f2) {
        return false;
    }

    @Override
    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
        return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        return false;
    }

    @Override
    public boolean onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
        return false;
    }

    @Override
    public boolean onLongPress(float f, float f2) {
        LogHelper.d(this.mTag, "[onLongPress]");
        return !isEnvironmentReady() ? false : false;
    }

    @Override
    public boolean onShutterButtonFocus(boolean z) {
        return false;
    }

    @Override
    public boolean onShutterButtonClick() {
        if (!isEnvironmentReady() || ICameraMode.ModeType.VIDEO.equals(this.mCurrentModeType)) {
            return false;
        }
        if ((this.mExposureListener != null && !this.mExposureListener.needConsiderAePretrigger()) || this.mExposureListener == null || !this.mExposureListener.checkTodoCapturAfterAeConverted()) {
            return false;
        }
        LogHelper.d(this.mTag, "[onShutterButtonClick] need do capture after AE converted");
        return true;
    }

    @Override
    public boolean onShutterButtonLongPressed() {
        return false;
    }

    @Override
    public void onExposureViewChanged(final int i) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                LogHelper.d(Exposure.this.mTag, "[onExposureViewChanged]+ value " + i);
                if (Exposure.this.mExposureListener != null) {
                    Exposure.this.mExposureListener.updateEv(i);
                }
                if (Exposure.this.isEnvironmentReady()) {
                    if (Exposure.this.mSettingChangeRequester != null) {
                        Exposure.this.mSettingChangeRequester.sendSettingChangeRequest();
                    }
                    LogHelper.d(Exposure.this.mTag, "[onExposureViewChanged] - ");
                }
            }
        });
    }

    @Override
    public void onTrackingTouchStatusChanged(final boolean z) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (Exposure.this.isEnvironmentReady()) {
                    Exposure.this.mViewStatusResponder.statusChanged("key_exposure_view", String.valueOf(z));
                }
            }
        });
    }

    public void initExposureCompensation(int[] iArr) {
        this.mExposureViewController.initExposureValues(iArr);
    }

    @Override
    public void updateModeDeviceState(String str) {
        LogHelper.d(this.mTag, "[updateModeDeviceState] + newState = " + str + ",mLastModeState = " + this.mLastModeState);
        if (str.equals(this.mLastModeState)) {
            return;
        }
        this.mLastModeState = str;
        byte b = -1;
        if (str.hashCode() == -41631974 && str.equals("previewing")) {
            b = 0;
        }
        if (b == 0 && !hasDisableEvReset() && getValue() != null && !getValue().equals("0")) {
            this.mExposureListener.updateEv(0);
            if (this.mSettingChangeRequester != null) {
                this.mSettingChangeRequester.sendSettingChangeRequest();
            }
        }
    }

    protected ICameraMode.ModeType getCurrentModeType() {
        return this.mCurrentModeType;
    }

    protected String getCurrentFlashValue() {
        String strQueryValue = this.mSettingController.queryValue("key_flash");
        LogHelper.d(this.mTag, "[getCurrentFlashValue] flashValue " + strQueryValue);
        return strQueryValue;
    }

    protected String getCurrentShutterValue() {
        return this.mSettingController.queryValue("key_shutter_speed");
    }

    protected void capture() {
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Exposure.this.mAppUi.triggerShutterButtonClick(30);
            }
        });
    }

    protected void setPanel(final boolean z, final int i) {
        LogHelper.d(this.mTag, "[setPanel] to " + z + ",brightness = " + i);
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Exposure.this.mIsPanelOn = z;
                IWindowManager windowManagerService = WindowManagerGlobal.getWindowManagerService();
                if (z) {
                    try {
                        Exposure.this.mCurrESSLEDMinStep = PictureQuality.getMinStepOfESSLED();
                        Exposure.this.mCurrESSOLEDMinStep = PictureQuality.getMinStepOfESSOLED();
                        LogHelper.d(Exposure.this.mTag, "[setPanel] mCurrESSLEDMinStep " + Exposure.this.mCurrESSLEDMinStep + ",mCurrESSOLEDMinStep " + Exposure.this.mCurrESSOLEDMinStep);
                        windowManagerService.getAnimationScale(2);
                        windowManagerService.setAnimationScale(2, 0.0f);
                    } catch (RemoteException e) {
                        LogHelper.d(Exposure.this.mTag, "[setPanel] failed RemoteException");
                    }
                    PictureQuality.setMinStepOfESSLED(4096);
                    PictureQuality.setMinStepOfESSOLED(4096);
                    Exposure.this.mAppUi.updateBrightnessBackGround(true);
                    WindowManager.LayoutParams attributes = Exposure.this.mApp.getActivity().getWindow().getAttributes();
                    attributes.screenBrightness = (i * 1.0f) / 255.0f;
                    Exposure.this.mApp.getActivity().getWindow().setAttributes(attributes);
                    return;
                }
                try {
                    windowManagerService.setAnimationScale(2, windowManagerService.getAnimationScale(2));
                } catch (RemoteException e2) {
                    LogHelper.d(Exposure.this.mTag, "[setPanel] failed RemoteException");
                }
                PictureQuality.setMinStepOfESSLED(Exposure.this.mCurrESSLEDMinStep);
                PictureQuality.setMinStepOfESSOLED(Exposure.this.mCurrESSOLEDMinStep);
                WindowManager.LayoutParams attributes2 = Exposure.this.mApp.getActivity().getWindow().getAttributes();
                attributes2.screenBrightness = (Exposure.this.mDefaultBrightNess * 1.0f) / 255.0f;
                Exposure.this.mApp.getActivity().getWindow().setAttributes(attributes2);
                Exposure.this.mAppUi.updateBrightnessBackGround(false);
            }
        });
    }

    protected boolean isPanelOn() {
        return this.mIsPanelOn;
    }

    protected boolean isThirdPartyIntent() {
        String action = this.mApp.getActivity().getIntent().getAction();
        return "android.media.action.IMAGE_CAPTURE".equals(action) || "android.media.action.VIDEO_CAPTURE".equals(action);
    }

    private boolean isEnvironmentReady() {
        if (!this.mPreviewStarted) {
            LogHelper.w(this.mTag, "[isEnvironmentReady] preview not started ");
            return false;
        }
        return true;
    }

    private boolean hasDisableEvReset() {
        return (this.mExposureListener != null && this.mExposureListener.getAeLock()) || getEntryValues().size() <= 1;
    }

    void doAeLock() {
        this.mExposureListener.setAeLock(true);
        if (this.mSettingChangeRequester != null) {
            this.mSettingChangeRequester.sendSettingChangeRequest();
        }
    }

    private void doAeUnLock() {
        if (this.mExposureListener != null && this.mExposureListener.getAeLock()) {
            this.mExposureListener.setAeLock(false);
            if (this.mSettingChangeRequester != null) {
                this.mSettingChangeRequester.sendSettingChangeRequest();
            }
        }
    }

    private Integer getScreenBrightness() {
        int i;
        try {
            i = Settings.System.getInt(this.mApp.getActivity().getContentResolver(), "screen_brightness");
        } catch (Settings.SettingNotFoundException e) {
            LogHelper.d(this.mTag, "[getScreenBrightness] SettingNotFoundException");
            i = 0;
        }
        LogHelper.d(this.mTag, "[getScreenBrightness] brightness " + i);
        return Integer.valueOf(i);
    }

    protected int getCameraId() {
        return Integer.parseInt(this.mSettingController.getCameraId());
    }
}
