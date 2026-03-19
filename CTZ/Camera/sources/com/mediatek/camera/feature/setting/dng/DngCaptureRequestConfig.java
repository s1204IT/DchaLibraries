package com.mediatek.camera.feature.setting.dng;

import android.annotation.TargetApi;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.DngCreator;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.Looper;
import android.util.Size;
import android.view.Surface;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.debug.profiler.IPerformanceProfile;
import com.mediatek.camera.common.debug.profiler.PerformanceTracker;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.feature.setting.dng.IDngConfig;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@TargetApi(21)
public class DngCaptureRequestConfig implements ICameraSetting.ICaptureRequestConfigure, IDngConfig {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(DngCaptureRequestConfig.class.getSimpleName());
    private TotalCaptureResult mCaptureResult;
    private boolean mCaptureResultReady;
    private CameraCharacteristics mCharas;
    private IDngConfig.OnDngValueUpdateListener mDngValueListener;
    private boolean mIsDngOn;
    private boolean mIsTakePicture;
    private Surface mRawCaptureSurface;
    private boolean mRawDataReady;
    private Image mRawImage;
    private ImageReader mRawImageReader;
    private Size mRawSize;
    private ISettingManager.SettingDevice2Requester mSettingDevice2Requester;
    private final Object mCaptureObject = new Object();
    private List<String> mDngList = new ArrayList();
    private ImageReader.OnImageAvailableListener mRawCaptureImageListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader) {
            LogHelper.i(DngCaptureRequestConfig.TAG, "mRawCaptureImageListener reader = " + imageReader);
            DngCaptureRequestConfig.this.mDngValueListener.onDngCreatorStateUpdate(true);
            DngCaptureRequestConfig.this.mRawImage = imageReader.acquireLatestImage();
            DngCaptureRequestConfig.this.mRawDataReady = true;
            DngCaptureRequestConfig.this.convertRawToDng(DngCaptureRequestConfig.this.mRawImage);
        }
    };
    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult) {
            super.onCaptureCompleted(cameraCaptureSession, captureRequest, totalCaptureResult);
            if (DngCaptureRequestConfig.this.mIsDngOn && CameraUtil.isStillCaptureTemplate(totalCaptureResult)) {
                LogHelper.i(DngCaptureRequestConfig.TAG, "onCaptureCompleted");
                DngCaptureRequestConfig.this.mCaptureResult = totalCaptureResult;
                DngCaptureRequestConfig.this.mCaptureResultReady = true;
                DngCaptureRequestConfig.this.mDngValueListener.onDngCreatorStateUpdate(true);
                DngCaptureRequestConfig.this.convertRawToDng(DngCaptureRequestConfig.this.mRawImage);
            }
        }
    };
    private final Handler mModeHandler = new Handler(Looper.myLooper());

    public DngCaptureRequestConfig(ISettingManager.SettingDevice2Requester settingDevice2Requester) {
        this.mSettingDevice2Requester = settingDevice2Requester;
    }

    @Override
    public void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics) {
        this.mDngList.clear();
        this.mDngList.add("off");
        if (isDngSupported(cameraCharacteristics)) {
            this.mDngList.add("on");
            this.mDngValueListener.onDngValueUpdate(this.mDngList, true);
        } else {
            this.mDngValueListener.onDngValueUpdate(this.mDngList, false);
        }
        this.mCharas = cameraCharacteristics;
    }

    @Override
    public void configCaptureRequest(CaptureRequest.Builder builder) {
        if (builder == null) {
            LogHelper.d(TAG, "[configCaptureRequest] captureBuilder is null");
            return;
        }
        LogHelper.d(TAG, "[configCaptureRequest] mIsDngOn : " + this.mIsDngOn);
        if (this.mIsDngOn && CameraUtil.isStillCaptureTemplate(builder.build()) && this.mRawCaptureSurface != null) {
            builder.addTarget(this.mRawCaptureSurface);
        }
    }

    @Override
    public void configSessionSurface(List<Surface> list) {
        LogHelper.d(TAG, "[configSessionSurface] mIsDngOn: " + this.mIsDngOn);
        if (this.mIsDngOn) {
            list.add(configRawSurface());
        } else {
            releaseRawSurface();
        }
    }

    @Override
    public CameraCaptureSession.CaptureCallback getRepeatingCaptureCallback() {
        return this.mCaptureCallback;
    }

    public void setDngValueUpdateListener(IDngConfig.OnDngValueUpdateListener onDngValueUpdateListener) {
        this.mDngValueListener = onDngValueUpdateListener;
    }

    @Override
    public void requestChangeOverrideValues() {
    }

    @Override
    public void setDngStatus(boolean z, boolean z2) {
        this.mIsDngOn = z;
        this.mIsTakePicture = z2;
    }

    @Override
    public void notifyOverrideValue(boolean z) {
    }

    @Override
    public void onModeClosed() {
        releaseRawSurface();
        resetDngCaptureStatus();
    }

    @Override
    public void sendSettingChangeRequest() {
        LogHelper.d(TAG, "[sendSettingChangeRequest] mIsDngOn : " + this.mIsDngOn + ", mIsTakePicture" + this.mIsTakePicture);
        if (!this.mIsTakePicture) {
            this.mSettingDevice2Requester.requestRestartSession();
        }
    }

    @Override
    public Surface configRawSurface() {
        LogHelper.i(TAG, "configRawSurface mIsDngOn = " + this.mIsDngOn);
        if (!this.mIsDngOn) {
            return null;
        }
        if (this.mRawCaptureSurface == null) {
            ImageReader imageReader = this.mRawImageReader;
            this.mRawImageReader = ImageReader.newInstance(this.mRawSize.getWidth(), this.mRawSize.getHeight(), 32, 3);
            this.mRawImageReader.setOnImageAvailableListener(this.mRawCaptureImageListener, this.mModeHandler);
            this.mRawCaptureSurface = this.mRawImageReader.getSurface();
        }
        return this.mRawCaptureSurface;
    }

    private void releaseRawSurface() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (DngCaptureRequestConfig.this.mCaptureObject) {
                    if (DngCaptureRequestConfig.this.mRawCaptureSurface != null) {
                        DngCaptureRequestConfig.this.mRawImageReader.close();
                        DngCaptureRequestConfig.this.mRawCaptureSurface.release();
                        DngCaptureRequestConfig.this.mRawImageReader = null;
                        DngCaptureRequestConfig.this.mRawCaptureSurface = null;
                    }
                    LogHelper.i(DngCaptureRequestConfig.TAG, "[releaseRawSurface] called");
                }
            }
        }).start();
    }

    private void resetDngCaptureStatus() {
        this.mRawDataReady = false;
        this.mCaptureResultReady = false;
        this.mIsTakePicture = false;
        this.mDngValueListener.onDngCreatorStateUpdate(false);
    }

    private boolean isDngSupported(CameraCharacteristics cameraCharacteristics) {
        if (!DngUtils.getAvailableCapablities(cameraCharacteristics).contains(3)) {
            LogHelper.e(TAG, "[isDngSupported] RAW capablity do not support");
            return false;
        }
        if (!DngUtils.isDngCaptureSizeAvailable(cameraCharacteristics)) {
            LogHelper.e(TAG, "[isDngSupported] No capture sizes available for raw format");
            return false;
        }
        this.mRawSize = DngUtils.getRawSize(cameraCharacteristics);
        return true;
    }

    private void convertRawToDng(Image image) {
        byte[] byteArray;
        ByteArrayOutputStream byteArrayOutputStream;
        if (!this.mCaptureResultReady || !this.mRawDataReady) {
            return;
        }
        if (DngUtils.getRawSize(this.mCharas) == null) {
            LogHelper.e(TAG, "[convertRawToDng], get raw size error");
            return;
        }
        synchronized (this.mCaptureObject) {
            if (this.mRawCaptureSurface != null) {
                this.mRawSize = DngUtils.getRawSize(this.mCharas);
                int dngOrientation = DngUtils.getDngOrientation(this.mDngValueListener.onDisplayOrientationUpdate());
                IPerformanceProfile iPerformanceProfileStart = PerformanceTracker.create(TAG, "dngcreator").start();
                try {
                    DngCreator dngCreator = new DngCreator(this.mCharas, this.mCaptureResult);
                    dngCreator.setOrientation(dngOrientation);
                    byteArrayOutputStream = new ByteArrayOutputStream();
                    dngCreator.writeImage(byteArrayOutputStream, image);
                    byteArray = byteArrayOutputStream.toByteArray();
                } catch (IOException e) {
                    byteArray = null;
                }
                try {
                    byteArrayOutputStream.close();
                    image.close();
                } catch (IOException e2) {
                    LogHelper.e(TAG, "[convertRawToDng], dng write error");
                }
                iPerformanceProfileStart.stop();
                LogHelper.i(TAG, "[convertRawToDng]");
                this.mDngValueListener.onSaveDngImage(byteArray, this.mRawSize);
            }
        }
        resetDngCaptureStatus();
    }
}
