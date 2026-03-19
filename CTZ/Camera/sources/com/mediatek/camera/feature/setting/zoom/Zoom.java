package com.mediatek.camera.feature.setting.zoom;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.SettingBase;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.feature.setting.zoom.IZoomConfig;
import java.util.ArrayList;
import java.util.List;

public class Zoom extends SettingBase {
    private static final String[] ZOOM_IN_TARGET_RATIO = {"x2.", "x2.", "x2.", "x3.", "x4.", "x4."};
    private static final String[] ZOOM_OUT_Target_RATIO = {"x1.0", "x1.0", "x1.0", "x1.0", "x2.0", "x3.0"};
    private ZoomCaptureRequestConfig mCaptureRequestConfig;
    private String mCurrentRatioMsg;
    private MainHandler mMainHandler;
    private Handler mModeHandler;
    private ZoomParameterConfig mParametersConfig;
    private ICameraSetting.ISettingChangeRequester mSettingChangeRequester;
    private LogUtil.Tag mTag;
    private IZoomConfig mZoomConfig;
    private ZoomGestureImpl mZoomGestureImpl;
    private ZoomKeyEventListener mZoomKeyEventListener;
    private ZoomViewCtrl mZoomViewCtrl = new ZoomViewCtrl();
    private String mOverrideValue = "on";
    private List<String> mSupportValues = new ArrayList();
    private float mLastDistanceRatio = 0.0f;
    private IZoomConfig.OnZoomLevelUpdateListener mZoomLevelUpdateListener = new IZoomConfig.OnZoomLevelUpdateListener() {
        @Override
        public void onZoomLevelUpdate(String str) {
            Zoom.this.mCurrentRatioMsg = str;
            Zoom.this.mZoomViewCtrl.showView(str);
        }

        @Override
        public String onGetOverrideValue() {
            return Zoom.this.mOverrideValue;
        }
    };
    private IApp.OnOrientationChangeListener mOrientationListener = new IApp.OnOrientationChangeListener() {
        @Override
        public void onOrientationChanged(int i) {
            if (Zoom.this.mZoomViewCtrl != null) {
                Zoom.this.mZoomViewCtrl.onOrientationChanged(i);
            }
        }
    };

    public Zoom() {
        this.mZoomGestureImpl = new ZoomGestureImpl();
        this.mZoomKeyEventListener = new ZoomKeyEventListener();
    }

    static float access$1316(Zoom zoom, float f) {
        float f2 = zoom.mLastDistanceRatio + f;
        zoom.mLastDistanceRatio = f2;
        return f2;
    }

    static float access$1324(Zoom zoom, float f) {
        float f2 = zoom.mLastDistanceRatio - f;
        zoom.mLastDistanceRatio = f2;
        return f2;
    }

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, ISettingManager.SettingController settingController) {
        super.init(iApp, iCameraContext, settingController);
        this.mTag = new LogUtil.Tag(Zoom.class.getSimpleName() + "-" + settingController.getCameraId());
        this.mModeHandler = new Handler(Looper.myLooper());
        this.mZoomGestureImpl.init();
        this.mZoomViewCtrl.init(iApp);
        initSettingValue();
        this.mApp.registerOnOrientationChangeListener(this.mOrientationListener);
        this.mAppUi.registerGestureListener(this.mZoomGestureImpl, Integer.MAX_VALUE);
        LogHelper.d(this.mTag, "[init] zoom: " + this + ", Gesture: " + this.mZoomGestureImpl);
        this.mMainHandler = new MainHandler(this.mActivity.getMainLooper());
        this.mApp.registerKeyEventListener(this.mZoomKeyEventListener, Integer.MAX_VALUE);
    }

    @Override
    public void unInit() {
        this.mZoomViewCtrl.unInit();
        this.mApp.unregisterOnOrientationChangeListener(this.mOrientationListener);
        this.mAppUi.unregisterGestureListener(this.mZoomGestureImpl);
        LogHelper.d(this.mTag, "[unInit] zoom: " + this + ", Gesture: " + this.mZoomGestureImpl);
        this.mApp.unRegisterKeyEventListener(this.mZoomKeyEventListener);
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
        return "key_camera_zoom";
    }

    @Override
    public ICameraSetting.IParametersConfigure getParametersConfigure() {
        if (this.mParametersConfig == null) {
            this.mParametersConfig = new ZoomParameterConfig(this.mSettingDeviceRequester);
            this.mParametersConfig.setZoomUpdateListener(this.mZoomLevelUpdateListener);
            this.mSettingChangeRequester = this.mParametersConfig;
            this.mZoomConfig = this.mParametersConfig;
            LogHelper.d(this.mTag, "[getParametersConfigure]mZoomConfig: " + this.mSettingChangeRequester);
        }
        return (ZoomParameterConfig) this.mSettingChangeRequester;
    }

    @Override
    public ICameraSetting.ICaptureRequestConfigure getCaptureRequestConfigure() {
        if (this.mCaptureRequestConfig == null) {
            this.mCaptureRequestConfig = new ZoomCaptureRequestConfig(this.mSettingDevice2Requester);
            this.mCaptureRequestConfig.setZoomUpdateListener(this.mZoomLevelUpdateListener);
            this.mSettingChangeRequester = this.mCaptureRequestConfig;
            this.mZoomConfig = this.mCaptureRequestConfig;
            LogHelper.d(this.mTag, "[getCaptureRequestConfigure]mZoomConfig: " + this.mSettingChangeRequester);
        }
        return (ZoomCaptureRequestConfig) this.mSettingChangeRequester;
    }

    @Override
    public void overrideValues(String str, String str2, List<String> list) {
        super.overrideValues(str, str2, list);
        LogHelper.i(this.mTag, "[overrideValues] headerKey = " + str + ", currentValue = " + str2);
        updateRestrictionValue(getValue());
    }

    @Override
    public ICameraSetting.PreviewStateCallback getPreviewStateCallback() {
        return null;
    }

    private void updateRestrictionValue(String str) {
        this.mOverrideValue = str;
        if ("off".equals(str)) {
            this.mZoomViewCtrl.hideView();
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

    private class ZoomGestureImpl implements IAppUiListener.OnGestureListener {
        private double mLastDistanceRatio;
        private float mPreviousSpan;
        private int mScreenDistance;

        private ZoomGestureImpl() {
        }

        public void init() {
            int height = Zoom.this.mApp.getActivity().getWindowManager().getDefaultDisplay().getHeight();
            int width = Zoom.this.mApp.getActivity().getWindowManager().getDefaultDisplay().getWidth();
            if (height < width) {
                height = width;
            }
            this.mScreenDistance = height;
            this.mScreenDistance = (int) (((double) this.mScreenDistance) * 0.2d);
        }

        @Override
        public boolean onDown(MotionEvent motionEvent) {
            return false;
        }

        @Override
        public boolean onUp(MotionEvent motionEvent) {
            return false;
        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
            return false;
        }

        @Override
        public boolean onSingleTapUp(float f, float f2) {
            return false;
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
            if (!"off".equals(Zoom.this.getValue())) {
                if (Zoom.this.mZoomConfig != null) {
                    double dCalculateDistanceRatio = calculateDistanceRatio(scaleGestureDetector);
                    Zoom.this.mZoomConfig.onScalePerformed(dCalculateDistanceRatio);
                    if (Math.abs(dCalculateDistanceRatio - this.mLastDistanceRatio) > 0.08d) {
                        Zoom.this.requestZoom();
                        this.mLastDistanceRatio = dCalculateDistanceRatio;
                        return true;
                    }
                    return true;
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
            if (!"off".equals(Zoom.this.getValue())) {
                LogHelper.d(Zoom.this.mTag, "[onScaleBegin], Gesture: " + this + ", mZoomConfig: " + Zoom.this.mZoomConfig);
                if (Zoom.this.mZoomConfig != null) {
                    Zoom.this.mZoomViewCtrl.clearInvalidView();
                    Zoom.this.mZoomConfig.onScaleStatus(true);
                    this.mPreviousSpan = scaleGestureDetector.getCurrentSpan();
                    this.mLastDistanceRatio = 0.0d;
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
            if ("off".equals(Zoom.this.getValue())) {
                return false;
            }
            LogHelper.d(Zoom.this.mTag, "[onScaleEnd]");
            if (Zoom.this.mZoomConfig != null) {
                Zoom.this.mZoomViewCtrl.resetView();
                Zoom.this.mZoomConfig.onScaleStatus(false);
                this.mPreviousSpan = 0.0f;
                this.mLastDistanceRatio = 0.0d;
                return true;
            }
            return true;
        }

        @Override
        public boolean onLongPress(float f, float f2) {
            return false;
        }

        private double calculateDistanceRatio(ScaleGestureDetector scaleGestureDetector) {
            double currentSpan = (scaleGestureDetector.getCurrentSpan() - this.mPreviousSpan) / this.mScreenDistance;
            LogHelper.d(Zoom.this.mTag, "[calculateDistanceRatio] distanceRatio = " + currentSpan);
            return currentSpan;
        }
    }

    private void requestZoom() {
        if (this.mModeHandler == null) {
            return;
        }
        this.mModeHandler.post(new Runnable() {
            @Override
            public void run() {
                Zoom.this.mSettingChangeRequester.sendSettingChangeRequest();
            }
        });
    }

    private class ZoomKeyEventListener implements IApp.KeyEventListener {
        private ZoomKeyEventListener() {
        }

        @Override
        public boolean onKeyDown(int i, KeyEvent keyEvent) {
            if ((i == 168 || i == 169) && CameraUtil.isSpecialKeyCodeEnabled() && !"off".equals(Zoom.this.getValue())) {
                if (Zoom.this.mZoomConfig != null) {
                    Zoom.this.mZoomViewCtrl.clearInvalidView();
                    Zoom.this.mZoomConfig.onScaleStatus(true);
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean onKeyUp(int i, KeyEvent keyEvent) {
            if (!CameraUtil.isSpecialKeyCodeEnabled()) {
                return false;
            }
            if (i != 168 && i != 169) {
                return false;
            }
            if ("off".equals(Zoom.this.getValue())) {
                LogHelper.w(Zoom.this.mTag, "onKeyUp keyCode zoom is OFF");
                return false;
            }
            if (i == 168) {
                Zoom.this.mMainHandler.obtainMessage(0, Zoom.this.getTargetRatioMsg(true)).sendToTarget();
            } else if (i == 169) {
                Zoom.this.mMainHandler.obtainMessage(1, Zoom.this.getTargetRatioMsg(false)).sendToTarget();
            }
            return true;
        }
    }

    private class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    if (Zoom.this.mZoomConfig != null) {
                        String str = (String) message.obj;
                        if (Zoom.this.mCurrentRatioMsg != null && Zoom.this.mCurrentRatioMsg.startsWith(str)) {
                            Zoom.this.mLastDistanceRatio = 0.0f;
                            Zoom.this.mZoomViewCtrl.resetView();
                            Zoom.this.mZoomConfig.onScaleStatus(false);
                            Zoom.this.mMainHandler.removeMessages(0);
                            LogHelper.d(Zoom.this.mTag, "[handleMessage] zoom in, mCurrentRatioMsg = " + Zoom.this.mCurrentRatioMsg + ", done");
                        } else {
                            Zoom.access$1316(Zoom.this, 0.01f);
                            Zoom.this.mZoomConfig.onScalePerformed(Zoom.this.mLastDistanceRatio);
                            Zoom.this.requestZoom();
                            Zoom.this.mMainHandler.sendMessageDelayed(Zoom.this.mMainHandler.obtainMessage(0, str), 50L);
                        }
                        break;
                    }
                    break;
                case Camera2Proxy.TEMPLATE_PREVIEW:
                    if (Zoom.this.mZoomConfig != null) {
                        String str2 = (String) message.obj;
                        if (str2.equals(Zoom.this.mCurrentRatioMsg)) {
                            Zoom.this.mLastDistanceRatio = 0.0f;
                            Zoom.this.mZoomViewCtrl.resetView();
                            Zoom.this.mZoomConfig.onScaleStatus(false);
                            Zoom.this.mMainHandler.removeMessages(1);
                            LogHelper.d(Zoom.this.mTag, "[handleMessage] zoom out, mCurrentRatioMsg = " + Zoom.this.mCurrentRatioMsg + ", done");
                        } else {
                            Zoom.access$1324(Zoom.this, 0.01f);
                            Zoom.this.mZoomConfig.onScalePerformed(Zoom.this.mLastDistanceRatio);
                            Zoom.this.requestZoom();
                            Zoom.this.mMainHandler.sendMessageDelayed(Zoom.this.mMainHandler.obtainMessage(1, str2), 50L);
                        }
                        break;
                    }
                    break;
            }
        }
    }

    private String getTargetRatioMsg(boolean z) {
        String str;
        String[] strArr = z ? ZOOM_IN_TARGET_RATIO : ZOOM_OUT_Target_RATIO;
        if (this.mCurrentRatioMsg == null) {
            str = strArr[0];
        } else if (this.mCurrentRatioMsg.equals("")) {
            str = strArr[1];
        } else if (this.mCurrentRatioMsg.startsWith("x1.")) {
            str = strArr[2];
        } else if (this.mCurrentRatioMsg.startsWith("x2.")) {
            str = strArr[3];
        } else if (this.mCurrentRatioMsg.startsWith("x3.")) {
            str = strArr[4];
        } else if (this.mCurrentRatioMsg.startsWith("x4.")) {
            str = strArr[5];
        } else {
            str = "x1.0";
        }
        LogHelper.d(this.mTag, "[getTargetRatioMsg] isZoomIn = " + z + ", mCurrentRatioMsg = " + this.mCurrentRatioMsg + ", return " + str);
        return str;
    }
}
