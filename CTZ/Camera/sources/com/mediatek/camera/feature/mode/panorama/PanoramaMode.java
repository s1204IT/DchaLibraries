package com.mediatek.camera.feature.mode.panorama;

import android.media.MediaActionSound;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v1.CameraProxy;
import com.mediatek.camera.common.mode.CameraModeBase;
import com.mediatek.camera.common.mode.DeviceUsage;
import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.storage.MediaSaver;
import com.mediatek.camera.common.utils.BitmapCreator;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.feature.mode.panorama.PanoramaView;

public class PanoramaMode extends PanoramaBase implements IAppUiListener.OnGestureListener {
    private MediaActionSound mCameraSound;
    private boolean mIsShowingCollimatedDrawable;
    private Thread mLoadSoundTread;
    private Handler mMainHandler;
    private Runnable mOnHardwareStop;
    private PanoramaView mPanoramaView;
    private Runnable mRestartCaptureView;
    private static final LogUtil.Tag TAG = new LogUtil.Tag(PanoramaMode.class.getSimpleName());
    private static final String MODE_KEY = PanoramaMode.class.getName();
    private Object mLock = new Object();
    private State mState = State.STATE_UNKNOWN;
    private boolean mIsInStopProcess = false;
    private boolean mIsMerging = false;
    private boolean mIsCameraPreviewStarted = false;
    private int mCurrentNum = 0;
    private byte[] mJpegImageData = null;
    private PanoramaView.OnSaveButtonClickedListener mSaveButtonClickedListener = new PanoramaView.OnSaveButtonClickedListener() {
        @Override
        public void onSaveButtonClicked() {
            if (PanoramaMode.this.mCurrentNum > 0) {
                PanoramaMode.this.mPanoramaView.update(4, new Object[0]);
                PanoramaMode.this.stopCapture(true);
            }
        }
    };
    private PanoramaView.OnCancelButtonClickedListener mCancelButtonClickedListener = new PanoramaView.OnCancelButtonClickedListener() {
        @Override
        public void onCancelButtonClicked() {
            PanoramaMode.this.mPanoramaView.update(4, new Object[0]);
            PanoramaMode.this.stopCapture(false);
        }
    };
    private MediaSaver.MediaSaverListener mMediaSaverListener = new MediaSaver.MediaSaverListener() {
        @Override
        public void onFileSaved(Uri uri) {
            LogHelper.d(PanoramaMode.TAG, "[onFileSaved] uri = " + uri);
            ((CameraModeBase) PanoramaMode.this).mIApp.notifyNewMedia(uri, true);
            PanoramaMode.this.mMainHandler.sendEmptyMessage(1000);
        }
    };

    enum State {
        STATE_UNKNOWN,
        STATE_IDLE,
        STATE_CAPTURING,
        STATE_CLOSED
    }

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, boolean z) {
        LogHelper.i(TAG, "[init]+");
        super.init(iApp, iCameraContext, z);
        this.mCameraSound = new MediaActionSound();
        this.mLoadSoundTread = new LoadSoundTread();
        this.mLoadSoundTread.start();
        this.mMainHandler = new PanoramaHandler(this.mIApp.getActivity().getMainLooper());
        this.mPanoramaView = new PanoramaView(this.mIApp, Integer.parseInt(this.mCameraId));
        this.mPanoramaView.init();
        this.mPanoramaView.setSaveButtonClickedListener(this.mSaveButtonClickedListener);
        this.mPanoramaView.setCancelButtonClickedListener(this.mCancelButtonClickedListener);
        LogHelper.i(TAG, "[init]-");
    }

    @Override
    public void pause(DeviceUsage deviceUsage) {
        LogHelper.i(TAG, "[pause]+ mMainHandler = " + this.mMainHandler + ", mIsMerging = " + this.mIsMerging);
        stopCapture(false);
        super.pause(deviceUsage);
        this.mState = State.STATE_CLOSED;
        if (this.mMainHandler != null) {
            this.mMainHandler.removeMessages(1008);
            this.mMainHandler.sendEmptyMessage(1008);
            if (this.mIsMerging) {
                this.mIsMerging = false;
                this.mMainHandler.removeMessages(1000);
                this.mMainHandler.sendEmptyMessage(1000);
            }
        }
        LogHelper.i(TAG, "[pause]-");
    }

    @Override
    public void unInit() {
        LogHelper.i(TAG, "[unInit]+");
        super.unInit();
        if (this.mCameraSound != null) {
            this.mCameraSound.release();
            this.mCameraSound = null;
        }
        this.mPanoramaView.unInit();
        LogHelper.i(TAG, "[unInit]-");
    }

    @Override
    public boolean onShutterButtonClick() {
        return capture();
    }

    @Override
    public void onCameraOpened() {
        super.onCameraOpened();
        LogHelper.d(TAG, "[onCameraOpened]");
        this.mState = State.STATE_IDLE;
        Relation relation = PanoramaRestriction.getRestriction().getRelation("on", true);
        String strQueryValue = this.mISettingManager.getSettingController().queryValue("key_picture_size");
        relation.addBody("key_picture_size", strQueryValue, strQueryValue);
        this.mISettingManager.getSettingController().postRestriction(relation);
        this.mISettingManager.getSettingController().addViewEntry();
        this.mISettingManager.getSettingController().refreshViewEntry();
    }

    @Override
    public void beforeCloseCamera() {
        super.beforeCloseCamera();
        this.mIsCameraPreviewStarted = false;
    }

    @Override
    public void onCameraPreviewStarted() {
        super.onCameraPreviewStarted();
        this.mIsCameraPreviewStarted = true;
    }

    @Override
    public void onCameraPreviewStopped() {
        super.onCameraPreviewStopped();
        LogHelper.d(TAG, "[onCameraPreviewStopped]");
        this.mIsCameraPreviewStarted = false;
    }

    @Override
    public String getModeKey() {
        return MODE_KEY;
    }

    @Override
    public boolean onLongPress(float f, float f2) {
        return true;
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
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(float f, float f2) {
        return true;
    }

    @Override
    public boolean onDoubleTap(float f, float f2) {
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        return true;
    }

    @Override
    public boolean onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
        return true;
    }

    private class LoadSoundTread extends Thread {
        private LoadSoundTread() {
        }

        @Override
        public void run() {
            PanoramaMode.this.mCameraSound.load(0);
        }
    }

    private class PanoramaHandler extends Handler {
        public PanoramaHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what != 1005) {
                LogHelper.d(PanoramaMode.TAG, "[handleMessage]msg.what = " + message.what);
            }
            int i = message.what;
            if (i != 1000) {
                switch (i) {
                    case 1004:
                        PanoramaMode.this.saveFile();
                        break;
                    case 1005:
                        PanoramaMode.this.mPanoramaView.update(1, Integer.valueOf(message.arg1), Integer.valueOf(message.arg2), Boolean.valueOf(PanoramaMode.this.mIsShowingCollimatedDrawable || State.STATE_CAPTURING != PanoramaMode.this.mState || PanoramaMode.this.mCurrentNum < 1));
                        break;
                    case 1006:
                        PanoramaMode.this.mPanoramaView.update(0, Integer.valueOf(message.arg1));
                        break;
                    case 1007:
                        PanoramaMode.this.mPanoramaView.update(2, Integer.valueOf(message.arg1));
                        break;
                    case 1008:
                        PanoramaMode.this.mPanoramaView.reset();
                        PanoramaMode.this.mPanoramaView.hide();
                        break;
                    case 1009:
                        PanoramaMode.this.mPanoramaView.update(3, Integer.valueOf(message.arg1));
                        break;
                    case 1010:
                        PanoramaMode.this.mPanoramaView.update(4, Integer.valueOf(message.arg1));
                        break;
                }
            }
            PanoramaMode.this.dismissSavingProcess();
            PanoramaMode.this.resetCapture();
        }
    }

    private void onHardwareStopped(boolean z) {
        LogHelper.d(TAG, "[onHardwareStopped]isMerge = " + z);
        if (z) {
            this.mIPanoramaDeviceController.setAutoRamaCallback(null);
        }
        onCaptureDone(z);
    }

    private void onCaptureDone(boolean z) {
        LogHelper.d(TAG, "[onCaptureDone] isMerge = " + z);
        if (z && this.mJpegImageData != null) {
            this.mMainHandler.sendEmptyMessage(1004);
        } else {
            resetCapture();
        }
        this.mState = State.STATE_IDLE;
        updateModeDeviceState("previewing");
    }

    private void resetCapture() {
        LogHelper.d(TAG, "[resetCapture] current mState = " + this.mState);
        if (State.STATE_CLOSED != this.mState) {
            unlock3A();
            this.mPanoramaView.showGuideView(0);
        }
        this.mIApp.getAppUi().unregisterGestureListener(this);
    }

    private boolean capture() {
        boolean z = this.mICameraContext.getStorageService().getCaptureStorageSpace() > 0;
        LogHelper.i(TAG, "[capture] current mState = " + this.mState + ", mIsMerging = " + this.mIsMerging + ", storageReady = " + z + ", mIsCameraPreviewStarted = " + this.mIsCameraPreviewStarted);
        if (!z || State.STATE_IDLE != this.mState || this.mIsMerging || !this.mIsCameraPreviewStarted) {
            LogHelper.w(TAG, "[capture] return");
            return false;
        }
        if (!startCapture()) {
            LogHelper.w(TAG, "[capture] do not capture.");
            return false;
        }
        this.mPanoramaView.showGuideView(1);
        this.mMainHandler.sendEmptyMessage(1009);
        return true;
    }

    private boolean startCapture() {
        LogHelper.d(TAG, "[startCapture] mState = " + this.mState + ",mIsInStopProcess = " + this.mIsInStopProcess);
        if (State.STATE_IDLE != this.mState || this.mIsInStopProcess) {
            return false;
        }
        this.mState = State.STATE_CAPTURING;
        updateModeDeviceState("capturing");
        this.mCurrentNum = 0;
        this.mIsShowingCollimatedDrawable = false;
        this.mIPanoramaDeviceController.updateGSensorOrientation(this.mIApp.getGSensorOrientation());
        this.mIPanoramaDeviceController.setAutoRamaCallback(new VendorDataCallback());
        this.mIPanoramaDeviceController.startAutoRama(9);
        lock3A();
        this.mIApp.getAppUi().registerGestureListener(this, 0);
        this.mPanoramaView.show();
        return true;
    }

    private class VendorDataCallback implements CameraProxy.VendorDataCallback {
        private VendorDataCallback() {
        }

        @Override
        public void onDataTaken(Message message) {
            LogHelper.d(PanoramaMode.TAG, "[onDataTaken] message = " + message.what);
        }

        @Override
        public void onDataCallback(int i, byte[] bArr, int i2, int i3) {
            if (i3 == -1) {
                PanoramaMode.this.onPictureTaken(bArr);
            } else {
                PanoramaMode.this.mMainHandler.obtainMessage(1005, i2, i3).sendToTarget();
            }
        }
    }

    private void onPictureTaken(byte[] bArr) {
        LogHelper.d(TAG, "[onPictureTaken] mState = " + this.mState + ", mCurrentNum = " + this.mCurrentNum);
        if (State.STATE_IDLE == this.mState) {
            LogHelper.w(TAG, "[onPictureTaken]modeState is STATE_IDLE,return.");
            return;
        }
        if (this.mCurrentNum == 9 || this.mIsMerging) {
            LogHelper.d(TAG, "[onPictureTaken] autoRama done, mCurrentNum = " + this.mCurrentNum);
            this.mJpegImageData = bArr;
            this.mIsMerging = false;
            onHardwareStopped(true);
        } else if (this.mCurrentNum >= 0 && this.mCurrentNum < 9) {
            if (this.mCameraSound != null) {
                this.mCameraSound.play(0);
            }
            this.mMainHandler.obtainMessage(1006, this.mCurrentNum, 0).sendToTarget();
            if (this.mCurrentNum > 0) {
                if (this.mIsShowingCollimatedDrawable) {
                    this.mMainHandler.removeCallbacks(this.mRestartCaptureView);
                    this.mMainHandler.removeCallbacks(this.mOnHardwareStop);
                }
                this.mIsShowingCollimatedDrawable = true;
                this.mRestartCaptureView = new Runnable() {
                    @Override
                    public void run() {
                        PanoramaMode.this.mIsShowingCollimatedDrawable = false;
                        PanoramaMode.this.mMainHandler.obtainMessage(1007, PanoramaMode.this.mCurrentNum, 0).sendToTarget();
                    }
                };
                this.mMainHandler.postDelayed(this.mRestartCaptureView, 500L);
            }
        }
        this.mCurrentNum++;
        if (this.mCurrentNum == 9) {
            stop(true);
        }
    }

    private void stop(boolean z) {
        LogHelper.d(TAG, "[stop]isMerge = " + z + ", mState=" + this.mState + ",mIsMerging = " + this.mIsMerging);
        if (State.STATE_CAPTURING != this.mState) {
            LogHelper.i(TAG, "[stop] current mode state is not capturing, so return");
            return;
        }
        if (this.mIsMerging) {
            LogHelper.i(TAG, "[stop] current is also in merging,so cancel this time");
            return;
        }
        this.mIsMerging = z;
        if (z) {
            showSavingProgress();
        }
        this.mMainHandler.removeMessages(1005);
        this.mMainHandler.removeMessages(1008);
        this.mMainHandler.sendEmptyMessage(1008);
        stopAsync(z);
    }

    private void stopAsync(final boolean z) {
        LogHelper.d(TAG, "[stopAsync]isMerge=" + z + ", mIsInStopProcess = " + this.mIsInStopProcess);
        if (this.mIsInStopProcess) {
            return;
        }
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                PanoramaMode.this.doStop(z);
                PanoramaMode.this.mOnHardwareStop = new Runnable() {
                    @Override
                    public void run() {
                        if (!z) {
                            PanoramaMode.this.onHardwareStopped(false);
                        }
                    }
                };
                PanoramaMode.this.mMainHandler.post(PanoramaMode.this.mOnHardwareStop);
                synchronized (PanoramaMode.this.mLock) {
                    PanoramaMode.this.mIsInStopProcess = false;
                    PanoramaMode.this.mLock.notifyAll();
                }
            }
        });
        synchronized (this.mLock) {
            this.mIsInStopProcess = true;
        }
        thread.start();
    }

    private void doStop(boolean z) {
        LogHelper.d(TAG, "[doStop] isMerge=" + z);
        this.mIPanoramaDeviceController.stopAutoRama(z);
    }

    private void stopCapture(boolean z) {
        LogHelper.d(TAG, "[stopCapture]isMerge = " + z + ", current mState = " + this.mState);
        if (State.STATE_CAPTURING == this.mState) {
            this.mMainHandler.sendEmptyMessage(1010);
            stop(z);
        }
    }

    private void showSavingProgress() {
        this.mIApp.getAppUi().showSavingDialog(null, true);
    }

    private void dismissSavingProcess() {
        LogHelper.d(TAG, "[dismissSavingProcess]");
        this.mIApp.getAppUi().hideSavingDialog();
    }

    private void saveFile() {
        LogHelper.d(TAG, "[saveFile] mJpegImageData = " + this.mJpegImageData);
        if (this.mJpegImageData != null) {
            this.mIApp.getAppUi().updateThumbnail(BitmapCreator.createBitmapFromJpeg(this.mJpegImageData, this.mIApp.getAppUi().getThumbnailViewWidth()));
            Size sizeFromExif = CameraUtil.getSizeFromExif(this.mJpegImageData);
            this.mICameraContext.getMediaSaver().addSaveRequest(this.mJpegImageData, this.mPhotoModeHelper.createContentValues(this.mJpegImageData, this.mICameraContext.getStorageService().getFileDirectory(), sizeFromExif.getWidth(), sizeFromExif.getHeight()), null, this.mMediaSaverListener);
        }
    }

    private void lock3A() {
        if (!this.mIsCameraPreviewStarted) {
            LogHelper.d(TAG, "[lock3A] Preview not started, do nothing ");
        } else {
            this.mISettingManager.getSettingController().postRestriction(PanoramaRestriction.get3aRestriction().getRelation("on", true));
            this.mIPanoramaDeviceController.configParameters();
        }
    }

    private void unlock3A() {
        if (!this.mIsCameraPreviewStarted) {
            LogHelper.d(TAG, "[unlock3A] Preview not started, do nothing");
        } else {
            this.mISettingManager.getSettingController().postRestriction(PanoramaRestriction.get3aRestriction().getRelation("off", true));
            this.mIPanoramaDeviceController.configParameters();
        }
    }
}
