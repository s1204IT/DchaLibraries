package com.mediatek.camera.common.mode.video;

import android.app.Activity;
import android.content.ContentValues;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.mode.video.device.IDeviceController;
import com.mediatek.camera.common.mode.video.recorder.IRecorder;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.Size;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class VideoHelper {
    private static CamcorderProfile sProfile;
    private Activity mActivity;
    private IApp mApp;
    private ICameraContext mCameraContext;
    private IDeviceController mCameraDevice;
    private byte[] mCameraPreviewData;
    private long mDateTaken;
    private String mFileName;
    private String mFilePath;
    private int mPreviewFormat;
    private Size mPreviewSize;
    private String mTempPath;
    private String mTitle;
    private Handler mVideoHandler;
    private static final LogUtil.Tag TAG = new LogUtil.Tag(VideoHelper.class.getSimpleName());
    public static final int[] MEDIA_INFO = {1999, 1998};
    private int mOrientation = 0;
    private Lock mLock = new ReentrantLock();
    private IDeviceController.PreviewCallback mPreviewCallback = new IDeviceController.PreviewCallback() {
        @Override
        public void onPreviewCallback(byte[] bArr, int i, String str) {
            if (VideoHelper.this.mCameraPreviewData == null) {
                VideoHelper.this.stopSwitchCameraAnimation();
                VideoHelper.this.stopChangeModeAnimation();
                VideoHelper.this.mApp.getAppUi().onPreviewStarted(str);
            }
            VideoHelper.this.mCameraPreviewData = bArr;
            VideoHelper.this.mPreviewFormat = i;
        }
    };

    public VideoHelper(ICameraContext iCameraContext, IApp iApp, IDeviceController iDeviceController, Handler handler) {
        LogHelper.d(TAG, "[VideoHelper]");
        this.mCameraContext = iCameraContext;
        this.mActivity = iApp.getActivity();
        this.mCameraDevice = iDeviceController;
        this.mApp = iApp;
        this.mVideoHandler = handler;
    }

    public ContentValues prepareContentValues(boolean z, int i, Size size) {
        this.mLock.lock();
        try {
            LogHelper.d(TAG, "[prepareContentValues] isVideo = " + z + " orientation = " + i);
            this.mOrientation = i;
            initializeCommonInfo(z);
            if (z) {
                return createVideoValues();
            }
            return createPhotoValues(size);
        } finally {
            this.mLock.unlock();
        }
    }

    public String getVideoTempPath() {
        this.mTempPath = this.mCameraContext.getStorageService().getFileDirectory() + "/.videorecorder.3gp.tmp";
        LogHelper.d(TAG, "[getVideoTempPath] mTempPath = " + this.mTempPath);
        return this.mTempPath;
    }

    public String getVideoFilePath() {
        LogHelper.d(TAG, "[getVideoFilePath] mFilePath = " + this.mFilePath);
        return this.mFilePath;
    }

    public static int getRecordingRotation(int i, Camera.CameraInfo cameraInfo) {
        int i2;
        if (i != -1) {
            if (cameraInfo.facing == 1) {
                i2 = ((cameraInfo.orientation - i) + 360) % 360;
            } else {
                i2 = (cameraInfo.orientation + i) % 360;
            }
        } else {
            i2 = cameraInfo.orientation;
        }
        LogHelper.d(TAG, "[getRecordingRotation] orientation = " + i + " info " + cameraInfo + " rotation = " + i2);
        return i2;
    }

    public static int getRecordingRotation(int i, CameraCharacteristics cameraCharacteristics) {
        int iIntValue = ((Integer) cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)).intValue();
        boolean z = ((Integer) cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)).intValue() == 0;
        if (i != -1) {
            if (z) {
                return ((iIntValue - i) + 360) % 360;
            }
            return (iIntValue + i) % 360;
        }
        return iIntValue;
    }

    public CameraCharacteristics getCameraCharacteristics(Activity activity, String str) {
        try {
            return ((CameraManager) activity.getSystemService("camera")).getCameraCharacteristics(str);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    public long getRecorderMaxSize() {
        return this.mCameraContext.getStorageService().getRecordStorageSpace();
    }

    public void deleteVideoFile(String str) {
        if (!new File(str).delete()) {
            LogHelper.i(TAG, "[deleteVideoFile] Could not delete " + str);
        }
    }

    public String convertOutputFormatToMimeType(int i) {
        if (i == 2) {
            return "video/mp4";
        }
        return "video/3gpp";
    }

    public void pauseAudioPlayBack(IApp iApp) {
        LogHelper.i(TAG, "[pauseAudioPlayback]");
        Activity activity = iApp.getActivity();
        iApp.getActivity();
        AudioManager audioManager = (AudioManager) activity.getSystemService("audio");
        if (audioManager != null) {
            audioManager.requestAudioFocus(null, 3, 1);
        }
    }

    public void releaseAudioFocus(IApp iApp) {
        Activity activity = iApp.getActivity();
        iApp.getActivity();
        AudioManager audioManager = (AudioManager) activity.getSystemService("audio");
        if (audioManager != null) {
            audioManager.abandonAudioFocus(null);
        }
    }

    public IDeviceController.PreviewCallback getPreviewFrameCallback() {
        return this.mPreviewCallback;
    }

    public void releasePreviewFrameData() {
        this.mCameraPreviewData = null;
    }

    public SurfaceChangeListener getSurfaceListener() {
        return new SurfaceChangeListener();
    }

    public IRecorder.RecorderSpec configRecorderSpec(CamcorderProfile camcorderProfile, String str, CameraDeviceManagerFactory.CameraApi cameraApi, ISettingManager iSettingManager) {
        sProfile = camcorderProfile;
        IRecorder.RecorderSpec recorderSpec = new IRecorder.RecorderSpec();
        if (this.mCameraDevice.getCamera() != null) {
            this.mCameraDevice.unLockCamera();
            recorderSpec.camera = this.mCameraDevice.getCamera().getCamera();
        }
        if (cameraApi == CameraDeviceManagerFactory.CameraApi.API1) {
            recorderSpec.videoSource = 1;
            recorderSpec.orientationHint = getRecordingRotation(this.mApp.getGSensorOrientation(), this.mCameraDevice.getCameraInfo(Integer.parseInt(str)));
        } else {
            recorderSpec.videoSource = 2;
            recorderSpec.orientationHint = getRecordingRotation(this.mApp.getGSensorOrientation(), getCameraCharacteristics(this.mApp.getActivity(), str));
        }
        if ("on".equals(iSettingManager.getSettingController().queryValue("key_microphone"))) {
            recorderSpec.isRecordAudio = true;
            recorderSpec.audioSource = 5;
        } else {
            recorderSpec.isRecordAudio = false;
        }
        recorderSpec.profile = sProfile;
        recorderSpec.maxDurationMs = 0;
        recorderSpec.maxFileSizeBytes = getRecorderMaxSize();
        recorderSpec.location = this.mCameraContext.getLocation();
        recorderSpec.outFilePath = getVideoTempPath();
        return recorderSpec;
    }

    public synchronized void updatePreviewSize(Size size) {
        this.mPreviewSize = size;
    }

    public boolean isPDAFSupported(CameraDeviceManagerFactory.CameraApi cameraApi) {
        boolean z = false;
        if (CameraDeviceManagerFactory.CameraApi.API2 == cameraApi) {
            return false;
        }
        Camera.Parameters originalParameters = null;
        if (this.mCameraDevice.getCamera() != null) {
            originalParameters = this.mCameraDevice.getCamera().getOriginalParameters(false);
        }
        if (originalParameters != null && "true".equals(originalParameters.get("pdaf-supported"))) {
            z = true;
        }
        LogHelper.i(TAG, "[isPdafSupported] isSupported = " + z);
        return z;
    }

    public void stopEis25() {
        Camera.Parameters parameters = this.mCameraDevice.getCamera().getParameters();
        parameters.set("eis25-mode", 0);
        this.mCameraDevice.getCamera().setParameters(parameters);
        LogHelper.i(TAG, "[stopEis25]");
    }

    private ContentValues createVideoValues() throws IOException {
        ContentValues contentValues = new ContentValues();
        long duration = getDuration(this.mTempPath);
        String strConvertOutputFormatToMimeType = convertOutputFormatToMimeType(sProfile.fileFormat);
        contentValues.put("duration", Long.valueOf(duration));
        contentValues.put("title", this.mTitle);
        contentValues.put("_display_name", this.mFileName);
        contentValues.put("datetaken", Long.valueOf(this.mDateTaken));
        contentValues.put("mime_type", strConvertOutputFormatToMimeType);
        contentValues.put("_data", this.mFilePath);
        contentValues.put("width", Integer.valueOf(sProfile.videoFrameWidth));
        contentValues.put("height", Integer.valueOf(sProfile.videoFrameHeight));
        contentValues.put("resolution", Integer.toString(sProfile.videoFrameWidth) + "x" + Integer.toString(sProfile.videoFrameHeight));
        contentValues.put("_size", Long.valueOf(new File(this.mTempPath).length()));
        Location location = this.mCameraContext.getLocation();
        if (location != null) {
            contentValues.put("latitude", Double.valueOf(location.getLatitude()));
            contentValues.put("longitude", Double.valueOf(location.getLongitude()));
        }
        if (CameraUtil.isColumnExistInDB(this.mActivity, CameraUtil.TableList.VIDEO_TABLE, "orientation")) {
            contentValues.put("orientation", Integer.valueOf(this.mOrientation));
        }
        return contentValues;
    }

    private ContentValues createPhotoValues(Size size) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("datetaken", Long.valueOf(this.mDateTaken));
        contentValues.put("title", this.mTitle);
        contentValues.put("_display_name", this.mFileName);
        contentValues.put("mime_type", "image/jpeg");
        contentValues.put("orientation", Integer.valueOf(this.mOrientation));
        contentValues.put("_data", this.mFilePath);
        contentValues.put("width", Integer.valueOf(size.getWidth()));
        contentValues.put("height", Integer.valueOf(size.getHeight()));
        Location location = this.mCameraContext.getLocation();
        if (location != null) {
            contentValues.put("latitude", Double.valueOf(location.getLatitude()));
            contentValues.put("longitude", Double.valueOf(location.getLongitude()));
        }
        return contentValues;
    }

    private void initializeCommonInfo(boolean z) {
        this.mDateTaken = System.currentTimeMillis();
        this.mTitle = createFileTitle(z, this.mDateTaken);
        this.mFileName = createFileName(z, this.mTitle);
        this.mFilePath = this.mCameraContext.getStorageService().getFileDirectory() + '/' + this.mFileName;
    }

    private String createFileTitle(boolean z, long j) {
        SimpleDateFormat simpleDateFormat;
        Date date = new Date(j);
        if (z) {
            simpleDateFormat = new SimpleDateFormat("'VID'_yyyyMMdd_HHmmss");
        } else {
            simpleDateFormat = new SimpleDateFormat("'IMG'_yyyyMMdd_HHmmss_S");
        }
        return simpleDateFormat.format(date);
    }

    private String createFileName(boolean z, String str) {
        String str2 = str + ".jpg";
        if (z) {
            str2 = str + convertOutputFormatToFileExt(sProfile.fileFormat);
        }
        LogHelper.d(TAG, "[createFileName] + fileName = " + str2);
        return str2;
    }

    private String convertOutputFormatToFileExt(int i) {
        if (i == 2) {
            return ".mp4";
        }
        return ".3gp";
    }

    private long getDuration(String str) throws IOException {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        try {
            mediaMetadataRetriever.setDataSource(str);
            return Long.valueOf(mediaMetadataRetriever.extractMetadata(9)).longValue();
        } catch (IllegalArgumentException e) {
            return -1L;
        } catch (RuntimeException e2) {
            return -2L;
        } finally {
            mediaMetadataRetriever.release();
        }
    }

    private void stopSwitchCameraAnimation() {
        if (this.mApp != null) {
            this.mApp.getAppUi().animationEnd(IAppUi.AnimationType.TYPE_SWITCH_CAMERA);
        }
    }

    private void stopChangeModeAnimation() {
        this.mApp.getAppUi().animationEnd(IAppUi.AnimationType.TYPE_SWITCH_MODE);
    }

    private class SurfaceChangeListener implements IAppUiListener.ISurfaceStatusListener {
        private SurfaceChangeListener() {
        }

        @Override
        public void surfaceAvailable(final Object obj, int i, int i2) {
            LogHelper.i(VideoHelper.TAG, "[surfaceAvailable] holder = " + obj + " width = " + i + " height = " + i2);
            VideoHelper.this.updatePreviewSize(new Size(i, i2));
            if (VideoHelper.this.mVideoHandler != null) {
                VideoHelper.this.mVideoHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (VideoHelper.this.mCameraDevice != null) {
                            VideoHelper.this.mCameraDevice.updatePreviewSurface(obj);
                        }
                    }
                });
            }
        }

        @Override
        public void surfaceChanged(final Object obj, int i, int i2) {
            LogHelper.i(VideoHelper.TAG, "[surfaceChanged] holder = " + obj + " width = " + i + " height = " + i2);
            VideoHelper.this.updatePreviewSize(new Size(i, i2));
            if (VideoHelper.this.mVideoHandler != null) {
                VideoHelper.this.mVideoHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (VideoHelper.this.mCameraDevice != null) {
                            VideoHelper.this.mCameraDevice.updatePreviewSurface(obj);
                        }
                    }
                });
            }
        }

        @Override
        public void surfaceDestroyed(Object obj, int i, int i2) {
            LogHelper.i(VideoHelper.TAG, "[surfaceDestroyed] holder = " + obj);
            if (VideoHelper.this.mVideoHandler != null) {
                VideoHelper.this.mVideoHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (VideoHelper.this.mCameraDevice != null) {
                            VideoHelper.this.mCameraDevice.updatePreviewSurface(null);
                        }
                    }
                });
            }
        }
    }
}
