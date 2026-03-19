package com.mediatek.camera.feature.mode.panorama;

import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import com.google.common.base.Preconditions;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.mode.CameraModeBase;
import com.mediatek.camera.common.mode.DeviceUsage;
import com.mediatek.camera.common.mode.ICameraMode;
import com.mediatek.camera.common.mode.photo.PhotoModeHelper;
import com.mediatek.camera.common.relation.StatusMonitor;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.feature.mode.panorama.IPanoramaDeviceController;

public class PanoramaBase extends CameraModeBase implements IPanoramaDeviceController.CameraStateCallback, IPanoramaDeviceController.PreviewCallback, IPanoramaDeviceController.PreviewSizeCallback {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(PanoramaBase.class.getSimpleName());
    private Handler mAnimationHandler;
    private HandlerThread mAnimationHandlerThread;
    protected String mCameraId;
    protected int mCaptureHeight;
    protected int mCaptureWidth;
    protected IPanoramaDeviceController mIPanoramaDeviceController;
    protected ISettingManager mISettingManager;
    private IAppUiListener.ISurfaceStatusListener mISurfaceStatusListener;
    protected PhotoModeHelper mPhotoModeHelper;
    private byte[] mPreviewData;
    private int mPreviewFormat;
    private int mPreviewHeight;
    private int mPreviewWidth;
    private ISettingManager.SettingController mSettingController;
    private StatusMonitor.StatusChangeListener mStatusChangeListener;
    private volatile SwitchCameraTask mSwitchCameraTask;
    private volatile boolean mIsResumed = true;
    private Object mPreviewDataSync = new Object();

    public PanoramaBase() {
        this.mISurfaceStatusListener = new SurfaceChangeListener();
        this.mStatusChangeListener = new MyStatusChangeListener();
    }

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, boolean z) {
        LogHelper.d(TAG, "[init]+");
        Preconditions.checkNotNull(iApp);
        Preconditions.checkNotNull(iCameraContext);
        super.init(iApp, iCameraContext, z);
        this.mPhotoModeHelper = new PhotoModeHelper(iCameraContext);
        createAnimationHandler();
        this.mCameraId = getCameraIdByFacing(this.mDataStore.getValue("key_camera_switcher", null, this.mDataStore.getGlobalScope()));
        this.mIPanoramaDeviceController = new PanoramaDeviceController(iApp.getActivity(), this.mICameraContext);
        this.mIPanoramaDeviceController.setCameraStateCallback(this);
        initSettingManager(this.mCameraId);
        prepareAndOpenCamera(false);
        LogHelper.d(TAG, "[init]-");
    }

    @Override
    public void resume(DeviceUsage deviceUsage) {
        LogHelper.d(TAG, "[resume]+");
        super.resume(deviceUsage);
        synchronized (Boolean.valueOf(this.mIsResumed)) {
            this.mIsResumed = true;
        }
        this.mCameraId = getCameraIdByFacing(this.mDataStore.getValue("key_camera_switcher", null, this.mDataStore.getGlobalScope()));
        this.mIPanoramaDeviceController.queryCameraDeviceManager();
        initSettingManager(this.mCameraId);
        prepareAndOpenCamera(false);
        LogHelper.d(TAG, "[resume]-");
    }

    @Override
    public void pause(DeviceUsage deviceUsage) {
        LogHelper.d(TAG, "[pause]+");
        super.pause(deviceUsage);
        boolean z = this.mNeedCloseCameraIds == null || this.mNeedCloseCameraIds.size() > 0;
        boolean z2 = this.mNeedCloseCameraIds != null;
        synchronized (Boolean.valueOf(this.mIsResumed)) {
            this.mIsResumed = false;
        }
        this.mIApp.getAppUi().clearPreviewStatusListener(this.mISurfaceStatusListener);
        synchronized (this.mPreviewDataSync) {
            if (z2) {
                try {
                    if (this.mPreviewData != null) {
                        startChangeModeAnimation();
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
        if (z) {
            prePareAndCloseCamera(needCloseCameraSync());
            recycleSettingManager(this.mCameraId);
        } else {
            clearAllCallbacks();
            this.mIPanoramaDeviceController.stopPreview();
        }
        LogHelper.d(TAG, "[pause]-");
    }

    @Override
    public void unInit() {
        LogHelper.d(TAG, "[unInit]+");
        super.unInit();
        destroyAnimationHandler();
        this.mIPanoramaDeviceController.destroyDeviceController();
        LogHelper.d(TAG, "[unInit]-");
    }

    @Override
    public boolean onCameraSelected(String str) {
        LogHelper.i(TAG, "[onCameraSelected] ,new id:" + str + ",current id:" + this.mCameraId);
        if (canSelectCamera(str)) {
            synchronized (this.mPreviewDataSync) {
                startSwitchCameraAnimation();
            }
            recycleSettingManager(this.mCameraId);
            clearAllCallbacks();
            this.mCameraId = str;
            initSettingManager(this.mCameraId);
            doCameraSelect();
            return true;
        }
        return false;
    }

    @Override
    protected ISettingManager getSettingManager() {
        return this.mISettingManager;
    }

    @Override
    public boolean onShutterButtonClick() {
        return true;
    }

    @Override
    public void onPreviewCallback(byte[] bArr, int i) {
        synchronized (Boolean.valueOf(this.mIsResumed)) {
            if (this.mIsResumed) {
                synchronized (this.mPreviewDataSync) {
                    if (this.mPreviewData == null) {
                        String cameraIdByFacing = getCameraIdByFacing(this.mDataStore.getValue("key_camera_switcher", null, this.mDataStore.getGlobalScope()));
                        this.mIApp.getAppUi().applyAllUIEnabled(true);
                        this.mIApp.getAppUi().onPreviewStarted(cameraIdByFacing);
                        updateModeDeviceState("previewing");
                        stopAllAnimations();
                    }
                    this.mPreviewData = bArr;
                    this.mPreviewFormat = i;
                }
            }
        }
    }

    @Override
    public void onCameraOpened() {
        LogHelper.i(TAG, "[onCameraOpened]");
        updateModeDeviceState("opened");
    }

    @Override
    public void beforeCloseCamera() {
        updateModeDeviceState("closed");
    }

    @Override
    public void onCameraPreviewStarted() {
    }

    @Override
    public void onCameraPreviewStopped() {
        updateModeDeviceState("opened");
    }

    @Override
    public void onPreviewSizeReady(Size size) {
        LogHelper.d(TAG, "[onPreviewSizeReady] previewSize: " + size.toString());
        int width = size.getWidth();
        int height = size.getHeight();
        if (width != this.mPreviewWidth || height != this.mPreviewHeight) {
            onPreviewSizeChanged(width, height);
        }
    }

    private void onPreviewSizeChanged(int i, int i2) {
        synchronized (this.mPreviewDataSync) {
            this.mPreviewData = null;
        }
        this.mPreviewWidth = i;
        this.mPreviewHeight = i2;
        this.mIApp.getAppUi().setPreviewSize(this.mPreviewWidth, this.mPreviewHeight, this.mISurfaceStatusListener);
    }

    private void prepareAndOpenCamera(boolean z) {
        this.mICameraContext.getStatusMonitor(this.mCameraId).registerValueChangedListener("key_picture_size", this.mStatusChangeListener);
        this.mIPanoramaDeviceController.setPreviewCallback(this);
        this.mIPanoramaDeviceController.setPreviewSizeReadyCallback(this);
        PanoramaDeviceInfo panoramaDeviceInfo = new PanoramaDeviceInfo();
        panoramaDeviceInfo.setCameraId(this.mCameraId);
        panoramaDeviceInfo.setSettingManager(this.mISettingManager);
        panoramaDeviceInfo.setNeedOpenCameraSync(z);
        this.mIPanoramaDeviceController.openCamera(panoramaDeviceInfo);
    }

    private void prePareAndCloseCamera(boolean z) {
        clearAllCallbacks();
        this.mIPanoramaDeviceController.closeCamera(z);
        this.mPreviewData = null;
        this.mPreviewWidth = 0;
        this.mPreviewHeight = 0;
    }

    private void clearAllCallbacks() {
        this.mIPanoramaDeviceController.setPreviewSizeReadyCallback(null);
        this.mICameraContext.getStatusMonitor(this.mCameraId).unregisterValueChangedListener("key_picture_size", this.mStatusChangeListener);
    }

    private void initSettingManager(String str) {
        this.mISettingManager = this.mICameraContext.getSettingManagerFactory().getInstance(str, getModeKey(), ICameraMode.ModeType.PHOTO, this.mCameraApi);
        this.mSettingController = this.mISettingManager.getSettingController();
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

    private void stopAllAnimations() {
        LogHelper.d(TAG, "[stopAllAnimations]");
        this.mAnimationHandler.removeCallbacksAndMessages(null);
        this.mAnimationHandler.post(new Runnable() {
            @Override
            public void run() {
                LogHelper.d(PanoramaBase.TAG, "[stopAllAnimations] run");
                PanoramaBase.this.stopSwitchCameraAnimation();
                PanoramaBase.this.stopChangeModeAnimation();
            }
        });
    }

    private void startSwitchCameraAnimation() {
        this.mIApp.getAppUi().animationStart(IAppUi.AnimationType.TYPE_SWITCH_CAMERA, prepareAnimationData(this.mPreviewData, this.mPreviewWidth, this.mPreviewHeight, this.mPreviewFormat));
    }

    private void stopSwitchCameraAnimation() {
        this.mIApp.getAppUi().animationEnd(IAppUi.AnimationType.TYPE_SWITCH_CAMERA);
    }

    private void startChangeModeAnimation() {
        this.mIApp.getAppUi().animationStart(IAppUi.AnimationType.TYPE_SWITCH_MODE, prepareAnimationData(this.mPreviewData, this.mPreviewWidth, this.mPreviewHeight, this.mPreviewFormat));
    }

    private void stopChangeModeAnimation() {
        this.mIApp.getAppUi().animationEnd(IAppUi.AnimationType.TYPE_SWITCH_MODE);
    }

    private IAppUi.AnimationData prepareAnimationData(byte[] bArr, int i, int i2, int i3) {
        IAppUi.AnimationData animationData = new IAppUi.AnimationData();
        animationData.mData = bArr;
        animationData.mWidth = i;
        animationData.mHeight = i2;
        animationData.mFormat = i3;
        animationData.mOrientation = this.mPhotoModeHelper.getCameraInfoOrientation(this.mCameraId, this.mIApp.getActivity());
        animationData.mIsMirror = this.mPhotoModeHelper.isMirror(this.mCameraId, this.mIApp.getActivity());
        return animationData;
    }

    private class SurfaceChangeListener implements IAppUiListener.ISurfaceStatusListener {
        private SurfaceChangeListener() {
        }

        @Override
        public void surfaceAvailable(Object obj, int i, int i2) {
            LogHelper.i(PanoramaBase.TAG, "surfaceAvailable,device controller = " + PanoramaBase.this.mIPanoramaDeviceController + ",w = " + i + ",h = " + i2);
            synchronized (Boolean.valueOf(PanoramaBase.this.mIsResumed)) {
                if (PanoramaBase.this.mIPanoramaDeviceController != null && PanoramaBase.this.mIsResumed) {
                    PanoramaBase.this.mIPanoramaDeviceController.updatePreviewSurface(obj);
                }
            }
        }

        @Override
        public void surfaceChanged(Object obj, int i, int i2) {
            LogHelper.i(PanoramaBase.TAG, "surfaceChanged, device controller = " + PanoramaBase.this.mIPanoramaDeviceController + ",w = " + i + ",h = " + i2);
            synchronized (Boolean.valueOf(PanoramaBase.this.mIsResumed)) {
                if (PanoramaBase.this.mIPanoramaDeviceController != null && PanoramaBase.this.mIsResumed) {
                    PanoramaBase.this.mIPanoramaDeviceController.updatePreviewSurface(obj);
                }
            }
        }

        @Override
        public void surfaceDestroyed(Object obj, int i, int i2) {
            LogHelper.i(PanoramaBase.TAG, "surfaceDestroyed,device controller = " + PanoramaBase.this.mIPanoramaDeviceController);
            if (PanoramaBase.this.mIPanoramaDeviceController != null && PanoramaBase.this.mIsResumed) {
                PanoramaBase.this.mIPanoramaDeviceController.updatePreviewSurface(null);
            }
        }
    }

    private class SwitchCameraTask extends AsyncTask<Void, Void, Void> {
        private SwitchCameraTask() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            LogHelper.d(PanoramaBase.TAG, "[onPreExecute], will disable all UI");
            synchronized (Boolean.valueOf(PanoramaBase.this.mIsResumed)) {
                if (PanoramaBase.this.mIsResumed) {
                    PanoramaBase.this.mIApp.getAppUi().applyAllUIEnabled(false);
                }
            }
        }

        @Override
        protected Void doInBackground(Void... voidArr) {
            LogHelper.d(PanoramaBase.TAG, "[doInBackground]");
            synchronized (Boolean.valueOf(PanoramaBase.this.mIsResumed)) {
                if (PanoramaBase.this.mIsResumed) {
                    PanoramaBase.this.prePareAndCloseCamera(true);
                    PanoramaBase.this.prepareAndOpenCamera(false);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void r3) {
            super.onPostExecute(r3);
            LogHelper.d(PanoramaBase.TAG, "[onPostExecute], will enable all UI");
            synchronized (Boolean.valueOf(PanoramaBase.this.mIsResumed)) {
                PanoramaBase.this.mIApp.getAppUi().applyAllUIEnabled(true);
            }
            clearSwitchTask();
        }

        @Override
        protected void onCancelled(Void r2) {
            super.onCancelled(r2);
            LogHelper.d(PanoramaBase.TAG, "[onCancelled]");
            clearSwitchTask();
        }

        private void clearSwitchTask() {
            if (PanoramaBase.this.mSwitchCameraTask != null) {
                synchronized (PanoramaBase.this.mSwitchCameraTask) {
                    PanoramaBase.this.mSwitchCameraTask = null;
                }
            }
        }
    }

    private class MyStatusChangeListener implements StatusMonitor.StatusChangeListener {
        private MyStatusChangeListener() {
        }

        @Override
        public void onStatusChanged(String str, String str2) {
            LogHelper.i(PanoramaBase.TAG, "[onStatusChanged] key = " + str + ",value = " + str2);
            if (str2 != null && "key_picture_size".equalsIgnoreCase(str)) {
                String[] strArrSplit = str2.split("x");
                PanoramaBase.this.mCaptureWidth = Integer.parseInt(strArrSplit[0]);
                PanoramaBase.this.mCaptureHeight = Integer.parseInt(strArrSplit[1]);
                Size previewSize = PanoramaBase.this.mIPanoramaDeviceController.getPreviewSize(((double) PanoramaBase.this.mCaptureWidth) / ((double) PanoramaBase.this.mCaptureHeight));
                int width = previewSize.getWidth();
                int height = previewSize.getHeight();
                if (width != PanoramaBase.this.mPreviewWidth || height != PanoramaBase.this.mPreviewHeight) {
                    PanoramaBase.this.onPreviewSizeChanged(width, height);
                }
            }
        }
    }

    private String getCamerasFacing(int i) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(i, cameraInfo);
        if (cameraInfo.facing == 0) {
            return "back";
        }
        if (cameraInfo.facing == 1) {
            return "front";
        }
        return null;
    }

    private boolean canSelectCamera(String str) {
        boolean z;
        if (this.mSwitchCameraTask != null) {
            synchronized (this.mSwitchCameraTask) {
                z = AsyncTask.Status.RUNNING == this.mSwitchCameraTask.getStatus();
            }
        } else {
            z = false;
        }
        boolean z2 = (str == null || this.mCameraId.equalsIgnoreCase(str) || z) ? false : true;
        try {
            if ("front".equals(getCamerasFacing(Integer.valueOf(str).intValue()))) {
                z2 = false;
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        LogHelper.d(TAG, "[canSelectCamera] +: " + z2);
        return z2;
    }

    private void doCameraSelect() {
        handleOldSwitchCameraTask();
        this.mSwitchCameraTask = new SwitchCameraTask();
        this.mSwitchCameraTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    private void handleOldSwitchCameraTask() {
        if (this.mSwitchCameraTask != null) {
            synchronized (this.mSwitchCameraTask) {
                this.mSwitchCameraTask.cancel(true);
            }
        }
    }
}
