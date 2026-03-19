package com.mediatek.camera.feature.mode.vsdof.photo;

import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.CameraSysTrace;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.memory.IMemoryManager;
import com.mediatek.camera.common.memory.MemoryManagerImpl;
import com.mediatek.camera.common.mode.CameraApiHelper;
import com.mediatek.camera.common.mode.CameraModeBase;
import com.mediatek.camera.common.mode.DeviceUsage;
import com.mediatek.camera.common.mode.ICameraMode;
import com.mediatek.camera.common.mode.photo.ThumbnailHelper;
import com.mediatek.camera.common.mode.photo.device.IDeviceController;
import com.mediatek.camera.common.relation.DataStore;
import com.mediatek.camera.common.relation.StatusMonitor;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.storage.MediaSaver;
import com.mediatek.camera.common.utils.BitmapCreator;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.feature.mode.vsdof.photo.device.DeviceControllerFactory;
import com.mediatek.camera.feature.mode.vsdof.photo.device.ISdofPhotoDeviceController;
import com.mediatek.camera.feature.mode.vsdof.view.SdofViewCtrl;
import java.util.ArrayList;

public class SdofPhotoMode extends CameraModeBase implements IMemoryManager.IMemoryListener, IDeviceController.CaptureDataCallback, ISdofPhotoDeviceController.DeviceCallback, ISdofPhotoDeviceController.PreviewSizeCallback {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(SdofPhotoMode.class.getSimpleName());
    private Handler mAnimationHandler;
    private HandlerThread mAnimationHandlerThread;
    private String mCameraId;
    protected int mCaptureWidth;
    protected ISdofPhotoDeviceController mISdofDeviceController;
    private ISettingManager mISettingManager;
    private IAppUiListener.ISurfaceStatusListener mISurfaceStatusListener;
    private MemoryManagerImpl mMemoryManager;
    protected StatusMonitor.StatusResponder mPhotoStatusResponder;
    private byte[] mPreviewData;
    private int mPreviewFormat;
    private int mPreviewHeight;
    private int mPreviewWidth;
    protected SdofPhotoHelper mSdofPhotoHelper;
    private StatusMonitor.StatusChangeListener mStatusChangeListener;
    protected int mCaptureHeight = Integer.MAX_VALUE;
    protected volatile boolean mIsResumed = true;
    private int mCapturingNumber = 0;
    private boolean mIsMatrixDisplayShow = false;
    private Object mPreviewDataSync = new Object();
    private Object mCaptureNumberSync = new Object();
    private IMemoryManager.MemoryAction mMemoryState = IMemoryManager.MemoryAction.NORMAL;
    private SdofViewCtrl mSdofViewCtrl = new SdofViewCtrl();
    private MediaSaver.MediaSaverListener mMediaSaverListener = new MediaSaver.MediaSaverListener() {
        @Override
        public void onFileSaved(Uri uri) {
            SdofPhotoMode.this.mIApp.notifyNewMedia(uri, true);
            synchronized (SdofPhotoMode.this.mCaptureNumberSync) {
                SdofPhotoMode.access$410(SdofPhotoMode.this);
                if (SdofPhotoMode.this.mCapturingNumber == 0) {
                    SdofPhotoMode.this.mMemoryState = IMemoryManager.MemoryAction.NORMAL;
                    SdofPhotoMode.this.mIApp.getAppUi().hideSavingDialog();
                    SdofPhotoMode.this.mIApp.getAppUi().applyAllUIVisibility(0);
                }
            }
            LogHelper.d(SdofPhotoMode.TAG, "[onFileSaved] uri = " + uri + ", mCapturingNumber = " + SdofPhotoMode.this.mCapturingNumber);
        }
    };
    private ISdofPhotoDeviceController.StereoWarningCallback mWarningCallback = new ISdofPhotoDeviceController.StereoWarningCallback() {
        @Override
        public void onWarning(int i) {
            LogHelper.i(SdofPhotoMode.TAG, "[StereoWarningCallback onWarning] " + i);
            if (i != 4) {
                switch (i) {
                    case 0:
                    case Camera2Proxy.TEMPLATE_PREVIEW:
                    case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                        break;
                    default:
                        LogHelper.w(SdofPhotoMode.TAG, "Warning message don't need to show");
                        break;
                }
            }
            SdofPhotoMode.this.mSdofViewCtrl.showWarningView(i);
        }
    };
    private SdofViewCtrl.ViewChangeListener mViewChangeListener = new SdofViewCtrl.ViewChangeListener() {
        @Override
        public void onVsDofLevelChanged(int i) {
            SdofPhotoMode.this.mISdofDeviceController.setVsDofLevelParameter(i);
        }
    };

    public SdofPhotoMode() {
        this.mISurfaceStatusListener = new SurfaceChangeListener();
        this.mStatusChangeListener = new MyStatusChangeListener();
    }

    static int access$410(SdofPhotoMode sdofPhotoMode) {
        int i = sdofPhotoMode.mCapturingNumber;
        sdofPhotoMode.mCapturingNumber = i - 1;
        return i;
    }

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, boolean z) {
        LogHelper.d(TAG, "[init]+");
        super.init(iApp, iCameraContext, z);
        this.mSdofPhotoHelper = new SdofPhotoHelper(iCameraContext);
        createAnimationHandler();
        this.mCameraId = CameraApiHelper.getLogicalCameraId();
        LogHelper.d(TAG, "[init] mCameraId " + this.mCameraId);
        this.mISdofDeviceController = new DeviceControllerFactory().createDeviceController(iApp.getActivity(), this.mCameraApi, this.mICameraContext);
        initSettingManager(this.mCameraId);
        initStatusMonitor();
        this.mSdofViewCtrl.setViewChangeListener(this.mViewChangeListener);
        this.mSdofViewCtrl.init(iApp);
        ThumbnailHelper.setApp(iApp);
        this.mMemoryManager = new MemoryManagerImpl(iApp.getActivity());
        prepareAndOpenCamera(false, this.mCameraId, z);
        LogHelper.d(TAG, "[init]- ");
    }

    @Override
    public void resume(DeviceUsage deviceUsage) {
        super.resume(deviceUsage);
        this.mIsResumed = true;
        this.mSdofViewCtrl.showView();
        this.mSdofViewCtrl.onOrientationChanged(this.mIApp.getGSensorOrientation());
        initSettingManager(this.mCameraId);
        initStatusMonitor();
        this.mMemoryManager.addListener(this);
        this.mMemoryManager.initStateForCapture(this.mICameraContext.getStorageService().getCaptureStorageSpace());
        this.mMemoryState = IMemoryManager.MemoryAction.NORMAL;
        this.mISdofDeviceController.queryCameraDeviceManager();
        prepareAndOpenCamera(false, this.mCameraId, false);
    }

    @Override
    public void pause(DeviceUsage deviceUsage) {
        LogHelper.i(TAG, "[pause]+");
        super.pause(deviceUsage);
        this.mIsResumed = false;
        this.mMemoryManager.removeListener(this);
        this.mIApp.getAppUi().clearPreviewStatusListener(this.mISurfaceStatusListener);
        this.mSdofViewCtrl.unInit();
        if (this.mNeedCloseCameraIds.size() > 0) {
            prePareAndCloseCamera(needCloseCameraSync(), this.mCameraId);
            recycleSettingManager(this.mCameraId);
        } else {
            clearAllCallbacks(this.mCameraId);
            this.mISdofDeviceController.stopPreview();
        }
        LogHelper.i(TAG, "[pause]-");
    }

    @Override
    public void unInit() {
        super.unInit();
        destroyAnimationHandler();
        this.mISdofDeviceController.destroyDeviceController();
    }

    @Override
    public void onOrientationChanged(int i) {
        this.mSdofViewCtrl.onOrientationChanged(i);
    }

    @Override
    public DeviceUsage getDeviceUsage(DataStore dataStore, DeviceUsage deviceUsage) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(getCameraIdByFacing(dataStore.getValue("key_camera_switcher", null, dataStore.getGlobalScope())));
        updateModeDefinedCameraApi();
        return new DeviceUsage("vsdof", this.mCameraApi, arrayList);
    }

    @Override
    public boolean onCameraSelected(String str) {
        LogHelper.i(TAG, "[onCameraSelected] ,new id:" + str + ",current id:" + this.mCameraId);
        if (canSelectCamera(str)) {
            synchronized (this.mPreviewDataSync) {
                startSwitchCameraAnimation();
            }
            doCameraSelect(this.mCameraId, str);
            return true;
        }
        return false;
    }

    @Override
    public boolean onShutterButtonFocus(boolean z) {
        return true;
    }

    @Override
    protected boolean doShutterButtonClick() {
        boolean z = this.mICameraContext.getStorageService().getCaptureStorageSpace() > 0;
        boolean zIsReadyForCapture = this.mISdofDeviceController.isReadyForCapture();
        LogHelper.i(TAG, "onShutterButtonClick, is storage ready : " + z + ",isDeviceReady = " + zIsReadyForCapture);
        if (z && zIsReadyForCapture && this.mIsResumed && this.mMemoryState != IMemoryManager.MemoryAction.STOP) {
            startCaptureAnimation();
            this.mPhotoStatusResponder.statusChanged("key_photo_capture", "start");
            updateModeDeviceState("capturing");
            this.mIApp.getAppUi().applyAllUIEnabled(false);
            this.mISdofDeviceController.updateGSensorOrientation(this.mIApp.getGSensorOrientation());
            this.mISdofDeviceController.takePicture(this);
        }
        return true;
    }

    @Override
    public boolean onShutterButtonLongPressed() {
        return false;
    }

    @Override
    public void onDataReceived(IDeviceController.DataCallbackInfo dataCallbackInfo) {
        byte[] bArr = dataCallbackInfo.data;
        boolean z = dataCallbackInfo.needUpdateThumbnail;
        boolean z2 = dataCallbackInfo.needRestartPreview;
        LogHelper.d(TAG, "onDataReceived, data = " + bArr + ",mIsResumed = " + this.mIsResumed + ",needUpdateThumbnail = " + z + ",needRestartPreview = " + z2);
        if (bArr != null) {
            CameraSysTrace.onEventSystrace("jpeg callback", true);
        }
        if (bArr != null) {
            saveData(bArr);
        }
        if (this.mIsResumed && this.mCameraApi == CameraDeviceManagerFactory.CameraApi.API1 && z2 && !this.mIsMatrixDisplayShow) {
            this.mISdofDeviceController.startPreview();
        }
        if (bArr != null && z) {
            updateThumbnail(bArr);
        }
        if (bArr != null) {
            CameraSysTrace.onEventSystrace("jpeg callback", false);
        }
    }

    @Override
    public void onPostViewCallback(byte[] bArr) throws Throwable {
        LogHelper.d(TAG, "[onPostViewCallback] data = " + bArr + ",mIsResumed = " + this.mIsResumed);
        CameraSysTrace.onEventSystrace("post view callback", true);
        if (bArr != null && this.mIsResumed) {
            this.mIApp.getAppUi().updateThumbnail(BitmapCreator.createBitmapFromYuv(bArr, 17, ThumbnailHelper.getThumbnailWidth(), ThumbnailHelper.getThumbnailHeight(), this.mIApp.getAppUi().getThumbnailViewWidth(), CameraUtil.getJpegRotationFromDeviceSpec(Integer.parseInt(this.mCameraId), this.mIApp.getGSensorOrientation(), this.mIApp.getActivity())));
        }
        CameraSysTrace.onEventSystrace("post view callback", false);
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
    public void onPreviewCallback(byte[] bArr, int i) {
        if (!this.mIsResumed) {
            return;
        }
        synchronized (this.mPreviewDataSync) {
            if (!this.mIsMatrixDisplayShow) {
                this.mIApp.getAppUi().applyAllUIEnabled(true);
            }
            this.mIApp.getAppUi().onPreviewStarted(this.mCameraId);
            if (this.mPreviewData == null) {
                stopAllAnimations();
            }
            updateModeDeviceState("previewing");
            this.mPreviewData = bArr;
            this.mPreviewFormat = i;
        }
    }

    @Override
    public void onPreviewSizeReady(Size size) {
        LogHelper.d(TAG, "[onPreviewSizeReady] previewSize: " + size.toString());
        updatePictureSizeAndPreviewSize(size);
    }

    private void onPreviewSizeChanged(int i, int i2) {
        synchronized (this.mPreviewDataSync) {
            this.mPreviewData = null;
        }
        this.mPreviewWidth = i;
        this.mPreviewHeight = i2;
        this.mIApp.getAppUi().setPreviewSize(this.mPreviewWidth, this.mPreviewHeight, this.mISurfaceStatusListener);
    }

    private void prepareAndOpenCamera(boolean z, String str, boolean z2) {
        this.mCameraId = str;
        StatusMonitor statusMonitor = this.mICameraContext.getStatusMonitor(this.mCameraId);
        statusMonitor.registerValueChangedListener("key_picture_size", this.mStatusChangeListener);
        statusMonitor.registerValueChangedListener("key_matrix_display_show", this.mStatusChangeListener);
        this.mISdofDeviceController.setDeviceCallback(this);
        this.mISdofDeviceController.setPreviewSizeReadyCallback(this);
        this.mISdofDeviceController.setStereoWarningCallback(this.mWarningCallback);
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setCameraId(this.mCameraId);
        deviceInfo.setSettingManager(this.mISettingManager);
        deviceInfo.setNeedOpenCameraSync(z);
        deviceInfo.setNeedFastStartPreview(z2);
        this.mISdofDeviceController.openCamera(deviceInfo);
    }

    private void prePareAndCloseCamera(boolean z, String str) {
        clearAllCallbacks(str);
        this.mISdofDeviceController.closeCamera(z);
        this.mIsMatrixDisplayShow = false;
        this.mPreviewData = null;
        this.mPreviewWidth = 0;
        this.mPreviewHeight = 0;
    }

    private void clearAllCallbacks(String str) {
        this.mISdofDeviceController.setPreviewSizeReadyCallback(null);
        StatusMonitor statusMonitor = this.mICameraContext.getStatusMonitor(str);
        statusMonitor.unregisterValueChangedListener("key_picture_size", this.mStatusChangeListener);
        statusMonitor.unregisterValueChangedListener("key_matrix_display_show", this.mStatusChangeListener);
    }

    private void initSettingManager(String str) {
        this.mISettingManager = this.mICameraContext.getSettingManagerFactory().getInstance(str, getModeKey(), ICameraMode.ModeType.PHOTO, this.mCameraApi);
    }

    private void recycleSettingManager(String str) {
        this.mICameraContext.getSettingManagerFactory().recycle(str);
    }

    private void createAnimationHandler() {
        this.mAnimationHandlerThread = new HandlerThread("Animation_handler");
        this.mAnimationHandlerThread.start();
        this.mAnimationHandler = new Handler(this.mAnimationHandlerThread.getLooper());
    }

    private void destroyAnimationHandler() {
        if (this.mAnimationHandlerThread.isAlive()) {
            this.mAnimationHandlerThread.quit();
            this.mAnimationHandler = null;
        }
    }

    private boolean canSelectCamera(String str) {
        boolean z;
        if (str == null || this.mCameraId.equalsIgnoreCase(str)) {
            z = false;
        } else {
            z = true;
        }
        LogHelper.d(TAG, "[canSelectCamera] +: " + z);
        return z;
    }

    private void doCameraSelect(String str, String str2) {
        this.mIApp.getAppUi().applyAllUIEnabled(false);
        this.mIApp.getAppUi().onCameraSelected(str2);
        prePareAndCloseCamera(true, str);
        recycleSettingManager(str);
        initSettingManager(str2);
        prepareAndOpenCamera(false, str2, false);
    }

    private void stopAllAnimations() {
        LogHelper.d(TAG, "[stopAllAnimations]");
        if (this.mAnimationHandler == null) {
            return;
        }
        this.mAnimationHandler.removeCallbacksAndMessages(null);
        this.mAnimationHandler.post(new Runnable() {
            @Override
            public void run() {
                LogHelper.d(SdofPhotoMode.TAG, "[stopAllAnimations] run");
                SdofPhotoMode.this.stopSwitchCameraAnimation();
                SdofPhotoMode.this.stopChangeModeAnimation();
                SdofPhotoMode.this.stopCaptureAnimation();
            }
        });
    }

    private void startSwitchCameraAnimation() {
        this.mIApp.getAppUi().animationStart(IAppUi.AnimationType.TYPE_SWITCH_CAMERA, prepareAnimationData(this.mPreviewData, this.mPreviewWidth, this.mPreviewHeight, this.mPreviewFormat));
    }

    private void stopSwitchCameraAnimation() {
        this.mIApp.getAppUi().animationEnd(IAppUi.AnimationType.TYPE_SWITCH_CAMERA);
    }

    private void stopChangeModeAnimation() {
        this.mIApp.getAppUi().animationEnd(IAppUi.AnimationType.TYPE_SWITCH_MODE);
    }

    private void startCaptureAnimation() {
        this.mIApp.getAppUi().animationStart(IAppUi.AnimationType.TYPE_CAPTURE, null);
    }

    private void stopCaptureAnimation() {
        this.mIApp.getAppUi().animationEnd(IAppUi.AnimationType.TYPE_CAPTURE);
    }

    private IAppUi.AnimationData prepareAnimationData(byte[] bArr, int i, int i2, int i3) {
        IAppUi.AnimationData animationData = new IAppUi.AnimationData();
        animationData.mData = bArr;
        animationData.mWidth = i;
        animationData.mHeight = i2;
        animationData.mFormat = i3;
        animationData.mOrientation = this.mSdofPhotoHelper.getCameraInfoOrientation(this.mCameraId, this.mIApp.getActivity());
        animationData.mIsMirror = this.mSdofPhotoHelper.isMirror(this.mCameraId, this.mIApp.getActivity());
        return animationData;
    }

    private void updatePictureSizeAndPreviewSize(Size size) {
        String strQueryValue = this.mISettingManager.getSettingController().queryValue("key_picture_size");
        if (strQueryValue != null && this.mIsResumed) {
            String[] strArrSplit = strQueryValue.split("x");
            this.mCaptureWidth = Integer.parseInt(strArrSplit[0]);
            this.mCaptureHeight = Integer.parseInt(strArrSplit[1]);
            this.mISdofDeviceController.setPictureSize(new Size(this.mCaptureWidth, this.mCaptureHeight));
            int width = size.getWidth();
            int height = size.getHeight();
            LogHelper.d(TAG, "[updatePictureSizeAndPreviewSize] picture size: " + this.mCaptureWidth + " X" + this.mCaptureHeight + ",current preview size:" + this.mPreviewWidth + " X " + this.mPreviewHeight + ",new value :" + width + " X " + height);
            if (width != this.mPreviewWidth || height != this.mPreviewHeight) {
                onPreviewSizeChanged(width, height);
            }
        }
    }

    private void initStatusMonitor() {
        this.mPhotoStatusResponder = this.mICameraContext.getStatusMonitor(this.mCameraId).getStatusResponder("key_photo_capture");
    }

    private void saveData(byte[] bArr) {
        if (bArr != null) {
            String strQueryValue = this.mISettingManager.getSettingController().queryValue("key_dng");
            long length = bArr.length;
            if (strQueryValue != null && "on".equalsIgnoreCase(strQueryValue)) {
                length += 47185920;
            }
            synchronized (this.mCaptureNumberSync) {
                this.mCapturingNumber++;
                this.mMemoryManager.checkOneShotMemoryAction(length);
            }
            String fileDirectory = this.mICameraContext.getStorageService().getFileDirectory();
            Size sizeFromExif = CameraUtil.getSizeFromExif(bArr);
            this.mICameraContext.getMediaSaver().addSaveRequest(bArr, this.mSdofPhotoHelper.createContentValues(bArr, fileDirectory, sizeFromExif.getWidth(), sizeFromExif.getHeight()), null, this.mMediaSaverListener);
            synchronized (this.mPreviewDataSync) {
                this.mPreviewData = null;
            }
        }
    }

    private void updateThumbnail(byte[] bArr) {
        this.mIApp.getAppUi().updateThumbnail(BitmapCreator.createBitmapFromJpeg(bArr, this.mIApp.getAppUi().getThumbnailViewWidth()));
    }

    @Override
    public void onMemoryStateChanged(IMemoryManager.MemoryAction memoryAction) {
        if (memoryAction == IMemoryManager.MemoryAction.STOP && this.mCapturingNumber != 0) {
            LogHelper.d(TAG, "memory low, show saving");
            this.mIApp.getAppUi().showSavingDialog(null, true);
            this.mIApp.getAppUi().applyAllUIVisibility(4);
        }
    }

    private class SurfaceChangeListener implements IAppUiListener.ISurfaceStatusListener {
        private SurfaceChangeListener() {
        }

        @Override
        public void surfaceAvailable(final Object obj, int i, int i2) {
            LogHelper.d(SdofPhotoMode.TAG, "surfaceAvailable,device controller = " + SdofPhotoMode.this.mISdofDeviceController + ",w = " + i + ",h = " + i2);
            if (SdofPhotoMode.this.mModeHandler != null) {
                SdofPhotoMode.this.mModeHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (SdofPhotoMode.this.mISdofDeviceController != null && SdofPhotoMode.this.mIsResumed) {
                            SdofPhotoMode.this.mISdofDeviceController.updatePreviewSurface(obj);
                        }
                    }
                });
            }
        }

        @Override
        public void surfaceChanged(final Object obj, int i, int i2) {
            LogHelper.d(SdofPhotoMode.TAG, "surfaceChanged, device controller = " + SdofPhotoMode.this.mISdofDeviceController + ",w = " + i + ",h = " + i2);
            if (SdofPhotoMode.this.mModeHandler != null) {
                SdofPhotoMode.this.mModeHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (SdofPhotoMode.this.mISdofDeviceController != null && SdofPhotoMode.this.mIsResumed) {
                            SdofPhotoMode.this.mISdofDeviceController.updatePreviewSurface(obj);
                        }
                    }
                });
            }
        }

        @Override
        public void surfaceDestroyed(Object obj, int i, int i2) {
            LogHelper.d(SdofPhotoMode.TAG, "surfaceDestroyed,device controller = " + SdofPhotoMode.this.mISdofDeviceController);
        }
    }

    private class MyStatusChangeListener implements StatusMonitor.StatusChangeListener {
        private MyStatusChangeListener() {
        }

        @Override
        public void onStatusChanged(String str, String str2) {
            LogHelper.d(SdofPhotoMode.TAG, "[onStatusChanged] key = " + str + ",value = " + str2);
            if (!"key_picture_size".equalsIgnoreCase(str)) {
                if ("key_matrix_display_show".equals(str)) {
                    SdofPhotoMode.this.mIsMatrixDisplayShow = "true".equals(str2);
                    return;
                }
                return;
            }
            String[] strArrSplit = str2.split("x");
            SdofPhotoMode.this.mCaptureWidth = Integer.parseInt(strArrSplit[0]);
            SdofPhotoMode.this.mCaptureHeight = Integer.parseInt(strArrSplit[1]);
            SdofPhotoMode.this.mISdofDeviceController.setPictureSize(new Size(SdofPhotoMode.this.mCaptureWidth, SdofPhotoMode.this.mCaptureHeight));
            Size previewSize = SdofPhotoMode.this.mISdofDeviceController.getPreviewSize(((double) SdofPhotoMode.this.mCaptureWidth) / ((double) SdofPhotoMode.this.mCaptureHeight));
            int width = previewSize.getWidth();
            int height = previewSize.getHeight();
            if (width != SdofPhotoMode.this.mPreviewWidth || height != SdofPhotoMode.this.mPreviewHeight) {
                SdofPhotoMode.this.onPreviewSizeChanged(width, height);
            }
        }
    }
}
