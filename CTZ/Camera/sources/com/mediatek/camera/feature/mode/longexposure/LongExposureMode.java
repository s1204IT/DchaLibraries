package com.mediatek.camera.feature.mode.longexposure;

import android.app.Activity;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.RelativeLayout;
import com.mediatek.camera.R;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.CameraSysTrace;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.mode.CameraModeBase;
import com.mediatek.camera.common.mode.DeviceUsage;
import com.mediatek.camera.common.mode.ICameraMode;
import com.mediatek.camera.common.relation.StatusMonitor;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.storage.MediaSaver;
import com.mediatek.camera.common.utils.BitmapCreator;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.feature.mode.longexposure.ILongExposureDeviceController;
import com.mediatek.camera.feature.mode.longexposure.LongExposureView;

public class LongExposureMode extends CameraModeBase implements IAppUiListener.OnGestureListener, ILongExposureDeviceController.DeviceCallback, ILongExposureDeviceController.JpegCallback, ILongExposureDeviceController.PreviewSizeCallback {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(LongExposureMode.class.getSimpleName());
    private Activity mActivity;
    private String mCameraId;
    private IAppUi.HintInfo mGuideHint;
    private ILongExposureDeviceController mIDeviceController;
    private ISettingManager mISettingManager;
    private IAppUiListener.ISurfaceStatusListener mISurfaceStatusListener;
    private LongExposureHandler mLongExposureHandler;
    private LongExposureModeHelper mLongExposureModeHelper;
    private RelativeLayout mLongExposureRoot;
    private LongExposureView mLongExposureView;
    private LongExposureView.OnCountDownFinishListener mOnCountDownFinishListener;
    private CaptureAbortListener mOnLongExposureViewClickedListener;
    private int mPreviewHeight;
    private int mPreviewWidth;
    private StatusMonitor.StatusChangeListener mStatusChangeListener;
    private volatile boolean mIsResumed = false;
    private LongExposureView.LongExposureViewState mLastState = LongExposureView.LongExposureViewState.STATE_IDLE;
    private MediaSaver.MediaSaverListener mMediaSaverListener = new MediaSaver.MediaSaverListener() {
        @Override
        public void onFileSaved(Uri uri) {
            LongExposureMode.this.mIApp.notifyNewMedia(uri, true);
        }
    };

    public LongExposureMode() {
        this.mISurfaceStatusListener = new SurfaceChangeListener();
        this.mStatusChangeListener = new MyStatusChangeListener();
        this.mOnCountDownFinishListener = new MyOnCountDownFinishListener();
        this.mOnLongExposureViewClickedListener = new CaptureAbortListener();
    }

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, boolean z) {
        LogHelper.d(TAG, "[init]+");
        super.init(iApp, iCameraContext, z);
        this.mLongExposureHandler = new LongExposureHandler(Looper.myLooper());
        this.mActivity = this.mIApp.getActivity();
        this.mIApp.getAppUi().registerGestureListener(this, 0);
        this.mLongExposureModeHelper = new LongExposureModeHelper(iCameraContext);
        this.mCameraId = getCameraIdByFacing(this.mDataStore.getValue("key_camera_switcher", null, this.mDataStore.getGlobalScope()));
        this.mIDeviceController = new DeviceControllerFactory().createDeviceController(iApp.getActivity(), this.mCameraApi, this.mICameraContext);
        initSettingManager(this.mCameraId);
        initLongExposureView();
        prepareAndOpenCamera(this.mCameraId);
        LogHelper.d(TAG, "[init]-");
    }

    @Override
    public void resume(DeviceUsage deviceUsage) {
        LogHelper.d(TAG, "[resume]+");
        super.resume(deviceUsage);
        this.mIsResumed = true;
        this.mIDeviceController.queryCameraDeviceManager();
        initSettingManager(this.mCameraId);
        prepareAndOpenCamera(this.mCameraId);
        LogHelper.d(TAG, "[resume]-");
    }

    @Override
    public void pause(DeviceUsage deviceUsage) {
        LogHelper.d(TAG, "[pause]+");
        super.pause(deviceUsage);
        updateUiState(LongExposureView.LongExposureViewState.STATE_ABORT);
        this.mIsResumed = false;
        this.mIApp.getAppUi().clearPreviewStatusListener(this.mISurfaceStatusListener);
        if (this.mNeedCloseCameraIds.size() > 0) {
            prePareAndCloseCamera(needCloseCameraSync(), this.mCameraId);
            recycleSettingManager(this.mCameraId);
        } else {
            clearAllCallbacks(this.mCameraId);
            this.mIDeviceController.stopPreview();
        }
        LogHelper.d(TAG, "[pause]-");
    }

    @Override
    public void unInit() {
        LogHelper.i(TAG, "[unInit]+");
        super.unInit();
        this.mIApp.getAppUi().unregisterGestureListener(this);
        this.mIApp.getAppUi().hideScreenHint(this.mGuideHint);
        this.mIDeviceController.destroyDeviceController();
        LogHelper.i(TAG, "[unInit]-");
    }

    @Override
    public boolean onShutterButtonFocus(boolean z) {
        return true;
    }

    @Override
    public boolean onShutterButtonClick() {
        boolean z;
        if (this.mICameraContext.getStorageService().getCaptureStorageSpace() <= 0) {
            z = false;
        } else {
            z = true;
        }
        boolean zIsReadyForCapture = this.mIDeviceController.isReadyForCapture();
        LogHelper.i(TAG, "[onShutterButtonClick], is storage ready : " + z + ", isDeviceReady = " + zIsReadyForCapture + ",getModeDeviceStatus() " + getModeDeviceStatus());
        if ("capturing".equals(getModeDeviceStatus())) {
            doAbort();
            return true;
        }
        if (z && zIsReadyForCapture) {
            updateModeDeviceState("capturing");
            this.mIApp.getAppUi().hideScreenHint(this.mGuideHint);
            this.mLongExposureHandler.sendEmptyMessage(1);
            updateUiState(LongExposureView.LongExposureViewState.STATE_CAPTURE);
        }
        return true;
    }

    @Override
    public boolean onShutterButtonLongPressed() {
        return false;
    }

    @Override
    public void onDataReceived(byte[] bArr) {
        LogHelper.d(TAG, "[onDataReceived] data = " + bArr);
        updateUiState(LongExposureView.LongExposureViewState.STATE_PREVIEW);
        CameraSysTrace.onEventSystrace("jpeg callback", true);
        if (bArr != null) {
            saveData(bArr);
            updateThumbnail(bArr);
        }
        if (this.mIsResumed && this.mCameraApi == CameraDeviceManagerFactory.CameraApi.API1) {
            this.mIDeviceController.startPreview();
        }
        CameraSysTrace.onEventSystrace("jpeg callback", false);
    }

    @Override
    protected ISettingManager getSettingManager() {
        return this.mISettingManager;
    }

    @Override
    public void onCameraOpened(String str) {
        updateModeDeviceState("opened");
    }

    @Override
    public void beforeCloseCamera() {
        updateModeDeviceState("closed");
    }

    @Override
    public void afterStopPreview() {
        updateModeDeviceState("opened");
    }

    @Override
    public void onPreviewCallback(byte[] bArr, int i) {
        if (!this.mIsResumed) {
            return;
        }
        LogHelper.d(TAG, "[onPreviewCallback]");
        this.mIApp.getAppUi().onPreviewStarted(this.mCameraId);
        updateUiState(LongExposureView.LongExposureViewState.STATE_PREVIEW);
        updateModeDeviceState("previewing");
        this.mLongExposureHandler.sendEmptyMessage(0);
    }

    @Override
    public void onPreviewSizeReady(Size size) {
        LogHelper.d(TAG, "[onPreviewSizeReady] previewSize: " + size.toString());
        updatePictureSizeAndPreviewSize(size);
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return true;
    }

    @Override
    public boolean onUp(MotionEvent motionEvent) {
        return true;
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
        return true;
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
        return true;
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
        return true;
    }

    private void onPreviewSizeChanged(int i, int i2) {
        this.mPreviewWidth = i;
        this.mPreviewHeight = i2;
        this.mIApp.getAppUi().setPreviewSize(this.mPreviewWidth, this.mPreviewHeight, this.mISurfaceStatusListener);
    }

    private void prepareAndOpenCamera(String str) {
        this.mCameraId = str;
        this.mICameraContext.getStatusMonitor(this.mCameraId).registerValueChangedListener("key_picture_size", this.mStatusChangeListener);
        this.mIDeviceController.setDeviceCallback(this);
        this.mIDeviceController.setPreviewSizeReadyCallback(this);
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setCameraId(this.mCameraId);
        deviceInfo.setSettingManager(this.mISettingManager);
        this.mIDeviceController.openCamera(deviceInfo);
    }

    private void prePareAndCloseCamera(boolean z, String str) {
        clearAllCallbacks(str);
        this.mIDeviceController.closeCamera(z);
        this.mPreviewWidth = 0;
        this.mPreviewHeight = 0;
    }

    private void clearAllCallbacks(String str) {
        this.mIDeviceController.setPreviewSizeReadyCallback(null);
        this.mICameraContext.getStatusMonitor(str).unregisterValueChangedListener("key_picture_size", this.mStatusChangeListener);
    }

    private void initSettingManager(String str) {
        this.mISettingManager = this.mICameraContext.getSettingManagerFactory().getInstance(str, getModeKey(), ICameraMode.ModeType.PHOTO, this.mCameraApi);
    }

    private void initLongExposureView() {
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LongExposureMode.this.initGuideHint();
                LongExposureMode.this.mActivity.getLayoutInflater().inflate(R.layout.longexposurecapture, LongExposureMode.this.mIApp.getAppUi().getModeRootView(), true);
                LongExposureMode.this.mLongExposureRoot = (RelativeLayout) LongExposureMode.this.mActivity.findViewById(R.id.long_exposure_ui);
                LongExposureMode.this.mLongExposureView = (LongExposureView) LongExposureMode.this.mActivity.findViewById(R.id.long_exposure_progress);
                LongExposureMode.this.mLongExposureView.setViewStateChangedListener(LongExposureMode.this.mOnLongExposureViewClickedListener);
                LongExposureMode.this.mLongExposureView.setAddCountDownListener(LongExposureMode.this.mOnCountDownFinishListener);
                LongExposureMode.this.mIApp.getAppUi().showScreenHint(LongExposureMode.this.mGuideHint);
            }
        });
    }

    private void initGuideHint() {
        this.mGuideHint = new IAppUi.HintInfo();
        int identifier = this.mIApp.getActivity().getResources().getIdentifier("hint_text_background", "drawable", this.mIApp.getActivity().getPackageName());
        this.mGuideHint.mBackground = this.mIApp.getActivity().getDrawable(identifier);
        this.mGuideHint.mType = IAppUi.HintType.TYPE_AUTO_HIDE;
        this.mGuideHint.mDelayTime = 5000;
        this.mGuideHint.mHintText = this.mActivity.getString(R.string.long_exposure_guide_hint);
    }

    private void recycleSettingManager(String str) {
        this.mICameraContext.getSettingManagerFactory().recycle(str);
    }

    private void updatePictureSizeAndPreviewSize(Size size) {
        String strQueryValue = this.mISettingManager.getSettingController().queryValue("key_picture_size");
        if (strQueryValue != null && this.mIsResumed) {
            String[] strArrSplit = strQueryValue.split("x");
            int i = Integer.parseInt(strArrSplit[0]);
            int i2 = Integer.parseInt(strArrSplit[1]);
            this.mIDeviceController.setPictureSize(new Size(i, i2));
            int width = size.getWidth();
            int height = size.getHeight();
            LogHelper.d(TAG, "[updatePictureSizeAndPreviewSize] picture size: " + i + " X" + i2 + ",current preview size:" + this.mPreviewWidth + " X " + this.mPreviewHeight + ",new value :" + width + " X " + height);
            if (width != this.mPreviewWidth || height != this.mPreviewHeight) {
                onPreviewSizeChanged(width, height);
            }
        }
    }

    private void saveData(byte[] bArr) {
        LogHelper.d(TAG, "[saveData]");
        if (bArr != null) {
            String fileDirectory = this.mICameraContext.getStorageService().getFileDirectory();
            Size sizeFromExif = CameraUtil.getSizeFromExif(bArr);
            this.mICameraContext.getMediaSaver().addSaveRequest(bArr, this.mLongExposureModeHelper.createContentValues(bArr, fileDirectory, sizeFromExif.getWidth(), sizeFromExif.getHeight()), null, this.mMediaSaverListener);
        }
    }

    private void updateThumbnail(byte[] bArr) {
        this.mIApp.getAppUi().updateThumbnail(BitmapCreator.createBitmapFromJpeg(bArr, this.mIApp.getAppUi().getThumbnailViewWidth()));
    }

    private void updateUiState(final LongExposureView.LongExposureViewState longExposureViewState) {
        if (this.mLongExposureView == null) {
            return;
        }
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LogHelper.d(LongExposureMode.TAG, "[updateUiState] state = " + longExposureViewState + ",mLastState " + LongExposureMode.this.mLastState);
                if (longExposureViewState == LongExposureMode.this.mLastState) {
                    return;
                }
                LongExposureMode.this.mLastState = longExposureViewState;
                LongExposureMode.this.mLongExposureView.updateViewState(longExposureViewState);
                if (LongExposureView.LongExposureViewState.STATE_PREVIEW == longExposureViewState) {
                    LongExposureMode.this.mIApp.getAppUi().animationEnd(IAppUi.AnimationType.TYPE_CAPTURE);
                    LongExposureMode.this.mLongExposureRoot.setVisibility(4);
                    LongExposureMode.this.mIApp.getAppUi().applyAllUIVisibility(0);
                    LongExposureMode.this.mIApp.getAppUi().applyAllUIEnabled(true);
                    return;
                }
                if (LongExposureView.LongExposureViewState.STATE_ABORT == longExposureViewState) {
                    LongExposureMode.this.mLongExposureRoot.setVisibility(4);
                    LongExposureMode.this.mLongExposureRoot.setClickable(false);
                    LongExposureMode.this.mIApp.getAppUi().applyAllUIVisibility(0);
                    LongExposureMode.this.mIApp.getAppUi().applyAllUIEnabled(false);
                    return;
                }
                if (LongExposureView.LongExposureViewState.STATE_CAPTURE == longExposureViewState) {
                    LongExposureMode.this.mIApp.getAppUi().animationStart(IAppUi.AnimationType.TYPE_CAPTURE, null);
                    String strQueryValue = LongExposureMode.this.mISettingManager.getSettingController().queryValue("key_shutter_speed");
                    LogHelper.d(LongExposureMode.TAG, "[updateUi] mShutterSpeed speed = " + strQueryValue);
                    if (strQueryValue == null || "Auto".equals(strQueryValue)) {
                        LongExposureMode.this.mIDeviceController.setNeedWaitPictureDone(true);
                        return;
                    }
                    LongExposureMode.this.mIApp.getAppUi().applyAllUIVisibility(4);
                    LongExposureMode.this.mLongExposureRoot.setVisibility(0);
                    LongExposureMode.this.mLongExposureRoot.setClickable(true);
                    LongExposureMode.this.mLongExposureView.setCountdownTime(Integer.parseInt(strQueryValue));
                    LongExposureMode.this.mLongExposureView.startCountDown();
                }
            }
        });
    }

    private class SurfaceChangeListener implements IAppUiListener.ISurfaceStatusListener {
        private SurfaceChangeListener() {
        }

        @Override
        public void surfaceAvailable(Object obj, int i, int i2) {
            LogHelper.d(LongExposureMode.TAG, "[surfaceAvailable] device controller = " + LongExposureMode.this.mIDeviceController + ",mIsResumed = " + LongExposureMode.this.mIsResumed + ",w = " + i + ",h = " + i2);
            if (LongExposureMode.this.mIDeviceController != null && LongExposureMode.this.mIsResumed) {
                LongExposureMode.this.mIDeviceController.updatePreviewSurface(obj);
            }
        }

        @Override
        public void surfaceChanged(Object obj, int i, int i2) {
            LogHelper.d(LongExposureMode.TAG, "[surfaceChanged] device controller = " + LongExposureMode.this.mIDeviceController + ",mIsResumed = " + LongExposureMode.this.mIsResumed + ",w = " + i + ",h = " + i2);
            if (LongExposureMode.this.mIDeviceController != null && LongExposureMode.this.mIsResumed) {
                LongExposureMode.this.mIDeviceController.updatePreviewSurface(obj);
            }
        }

        @Override
        public void surfaceDestroyed(Object obj, int i, int i2) {
            LogHelper.d(LongExposureMode.TAG, "[surfaceDestroyed] device controller = " + LongExposureMode.this.mIDeviceController);
        }
    }

    private final class LongExposureHandler extends Handler {
        public LongExposureHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            LogHelper.d(LongExposureMode.TAG, "[handleMessage] " + message.what);
            switch (message.what) {
                case Camera2Proxy.TEMPLATE_PREVIEW:
                    LongExposureMode.this.mIDeviceController.updateGSensorOrientation(LongExposureMode.this.mIApp.getGSensorOrientation());
                    LongExposureMode.this.mIDeviceController.takePicture(LongExposureMode.this);
                    break;
                case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                    LongExposureMode.this.mIDeviceController.stopCapture();
                    break;
            }
        }
    }

    private final class MyOnCountDownFinishListener implements LongExposureView.OnCountDownFinishListener {
        private MyOnCountDownFinishListener() {
        }

        @Override
        public void countDownFinished(boolean z) {
            LongExposureMode.this.mIDeviceController.setNeedWaitPictureDone(z);
        }
    }

    private class MyStatusChangeListener implements StatusMonitor.StatusChangeListener {
        private MyStatusChangeListener() {
        }

        @Override
        public void onStatusChanged(String str, String str2) {
            LogHelper.d(LongExposureMode.TAG, "[onStatusChanged] key = " + str + ",value = " + str2);
            if ("key_picture_size".equalsIgnoreCase(str)) {
                String[] strArrSplit = str2.split("x");
                int i = Integer.parseInt(strArrSplit[0]);
                int i2 = Integer.parseInt(strArrSplit[1]);
                LongExposureMode.this.mIDeviceController.setPictureSize(new Size(i, i2));
                Size previewSize = LongExposureMode.this.mIDeviceController.getPreviewSize(((double) i) / ((double) i2));
                int width = previewSize.getWidth();
                int height = previewSize.getHeight();
                if (width != LongExposureMode.this.mPreviewWidth || height != LongExposureMode.this.mPreviewHeight) {
                    LongExposureMode.this.onPreviewSizeChanged(width, height);
                }
            }
        }
    }

    private class CaptureAbortListener implements LongExposureView.OnCaptureAbortedListener {
        private CaptureAbortListener() {
        }

        @Override
        public void onCaptureAbort() {
            LongExposureMode.this.doAbort();
        }
    }

    private void doAbort() {
        if ("Auto".equals(this.mISettingManager.getSettingController().queryValue("key_shutter_speed"))) {
            return;
        }
        if (!"capturing".equals(getModeDeviceStatus())) {
            LogHelper.w(TAG, "[doAbort] mode device state " + getModeDeviceStatus());
            return;
        }
        if (this.mLastState == LongExposureView.LongExposureViewState.STATE_ABORT) {
            LogHelper.w(TAG, "[doAbort] still during aborting");
        } else {
            updateUiState(LongExposureView.LongExposureViewState.STATE_ABORT);
            this.mLongExposureHandler.sendEmptyMessage(2);
        }
    }
}
