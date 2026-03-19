package com.mediatek.camera.common.mode.photo.intent;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.mode.CameraModeBase;
import com.mediatek.camera.common.mode.DeviceUsage;
import com.mediatek.camera.common.mode.photo.PhotoMode;
import com.mediatek.camera.common.mode.photo.PhotoModeHelper;
import com.mediatek.camera.common.mode.photo.device.IDeviceController;
import com.mediatek.camera.common.mode.photo.intent.IIntentPhotoUi;
import com.mediatek.camera.common.storage.MediaSaver;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.Size;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class IntentPhotoMode extends PhotoMode implements IAppUiListener.OnShutterButtonListener, IIntentPhotoUi.OkButtonClickListener, IIntentPhotoUi.RetakeButtonClickListener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(IntentPhotoMode.class.getSimpleName());
    private Activity mActivity;
    private String mCropValue;
    private IIntentPhotoUi mIIntentPhotoUi;
    private boolean mIsQuickCapture;
    private byte[] mJpegData;
    private MediaSaver.MediaSaverListener mMediaSaverListener = new MediaSaver.MediaSaverListener() {
        @Override
        public void onFileSaved(Uri uri) {
            LogHelper.d(IntentPhotoMode.TAG, "[onFileSaved] uri = " + uri);
            ((CameraModeBase) IntentPhotoMode.this).mIApp.notifyNewMedia(uri, true);
        }
    };
    private Uri mSaveUri;

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, boolean z) {
        super.init(iApp, iCameraContext, z);
        LogHelper.d(TAG, "[init]");
        this.mIIntentPhotoUi = this.mIApp.getAppUi().getPhotoUi();
        this.mActivity = iApp.getActivity();
        registerUIListener();
        parserIntent();
    }

    @Override
    public void resume(DeviceUsage deviceUsage) {
        super.resume(deviceUsage);
        this.mIIntentPhotoUi.hide();
        this.mIApp.getAppUi().applyAllUIVisibility(0);
    }

    @Override
    public void pause(DeviceUsage deviceUsage) {
        super.pause(deviceUsage);
        this.mIIntentPhotoUi.hide();
        this.mJpegData = null;
    }

    @Override
    public void onDataReceived(IDeviceController.DataCallbackInfo dataCallbackInfo) throws Throwable {
        byte[] bArr = dataCallbackInfo.data;
        LogHelper.d(TAG, "[onDataReceived] data: " + bArr + ",mIsResumed = " + this.mIsResumed);
        if (this.mIsResumed && this.mCameraApi == CameraDeviceManagerFactory.CameraApi.API2) {
            this.mIDeviceController.stopPreview();
        }
        if (bArr != null && this.mIsResumed) {
            this.mJpegData = bArr;
            if (this.mIsQuickCapture) {
                doAttach();
            } else if (this.mIIntentPhotoUi != null) {
                this.mIIntentPhotoUi.onPictureReceived(bArr);
            }
        }
    }

    @Override
    public void onPreviewCallback(byte[] bArr, int i) {
        if (this.mJpegData == null) {
            super.onPreviewCallback(bArr, i);
        }
    }

    @Override
    public void unInit() {
        super.unInit();
        unRegisterUIListener();
    }

    @Override
    public boolean onShutterButtonFocus(boolean z) {
        if (this.mIIntentPhotoUi.isShown()) {
            return true;
        }
        return super.onShutterButtonFocus(z);
    }

    @Override
    public boolean onShutterButtonClick() {
        if (this.mIIntentPhotoUi.isShown()) {
            return true;
        }
        return super.onShutterButtonClick();
    }

    @Override
    public boolean onShutterButtonLongPressed() {
        if (this.mIIntentPhotoUi.isShown()) {
            return true;
        }
        return super.onShutterButtonLongPressed();
    }

    @Override
    public boolean onBackPressed() {
        LogHelper.i(TAG, "[onBackPressed]");
        if (this.mActivity.getFragmentManager().popBackStackImmediate()) {
            return true;
        }
        this.mActivity.setResult(0, new Intent());
        this.mJpegData = null;
        this.mActivity.finish();
        return true;
    }

    @Override
    public void onOrientationChanged(int i) {
        super.onOrientationChanged(i);
        if (this.mIIntentPhotoUi != null) {
            this.mIIntentPhotoUi.onOrientationChanged(i);
        }
    }

    @Override
    public void onOkClickClicked() throws Throwable {
        if (!this.mActivity.isFinishing()) {
            doAttach();
        }
    }

    @Override
    public void onRetakeClicked() {
        if (isCameraAvailable()) {
            this.mIDeviceController.stopPreview();
            this.mPhotoStatusResponder.statusChanged("key_photo_capture", "stop");
            this.mIDeviceController.startPreview();
            this.mIApp.getAppUi().applyAllUIVisibility(0);
            this.mIApp.getAppUi().applyAllUIEnabled(true);
            this.mJpegData = null;
        }
    }

    private void registerUIListener() {
        if (this.mIIntentPhotoUi != null) {
            this.mIIntentPhotoUi.setOkButtonClickListener(this);
            this.mIIntentPhotoUi.setRetakeButtonClickListener(this);
        }
    }

    private void unRegisterUIListener() {
        if (this.mIIntentPhotoUi != null) {
            this.mIIntentPhotoUi.setOkButtonClickListener(null);
            this.mIIntentPhotoUi.setRetakeButtonClickListener(null);
        }
    }

    private void parserIntent() {
        Intent intent = this.mActivity.getIntent();
        this.mCropValue = intent.getStringExtra("crop");
        this.mIsQuickCapture = intent.getBooleanExtra("android.intent.extra.quickCapture", false);
        this.mSaveUri = (Uri) intent.getParcelableExtra("output");
    }

    private void doAttach() throws Throwable {
        LogHelper.d(TAG, "[doAttach]mCropValue = " + this.mCropValue);
        saveData();
        if (this.mCropValue == null) {
            if (this.mSaveUri != null) {
                attachToSaveUri();
                return;
            } else {
                attachToInLinedIntent();
                return;
            }
        }
        attachToCropIntent();
    }

    private boolean isCameraAvailable() {
        return !"closed".equals(getModeDeviceStatus());
    }

    private void saveData() {
        LogHelper.d(TAG, "[saveData]");
        if (this.mCropValue == null && this.mSaveUri == null) {
            LogHelper.d(TAG, "[saveData] don't need save sdcard and insert DB");
            return;
        }
        String fileDirectory = this.mICameraContext.getStorageService().getFileDirectory();
        Size sizeFromExif = CameraUtil.getSizeFromExif(this.mJpegData);
        this.mICameraContext.getMediaSaver().addSaveRequest(this.mJpegData, this.mPhotoModeHelper.createContentValues(this.mJpegData, fileDirectory, sizeFromExif.getWidth(), sizeFromExif.getHeight()), null, this.mMediaSaverListener);
    }

    private void attachToSaveUri() throws Throwable {
        OutputStream outputStreamOpenOutputStream;
        Throwable th;
        LogHelper.d(TAG, "[attachToSaveUri]");
        OutputStream outputStream = null;
        try {
            try {
                outputStreamOpenOutputStream = this.mActivity.getContentResolver().openOutputStream(this.mSaveUri);
                if (outputStreamOpenOutputStream != null) {
                    try {
                        outputStreamOpenOutputStream.write(this.mJpegData);
                        outputStreamOpenOutputStream.close();
                    } catch (IOException e) {
                        outputStream = outputStreamOpenOutputStream;
                        LogHelper.w(TAG, "[doAttach] IOException");
                        PhotoModeHelper.closeSilently(outputStream);
                        return;
                    } catch (Throwable th2) {
                        th = th2;
                        PhotoModeHelper.closeSilently(outputStreamOpenOutputStream);
                        throw th;
                    }
                }
                this.mActivity.setResult(-1);
                this.mActivity.finish();
                PhotoModeHelper.closeSilently(outputStreamOpenOutputStream);
            } catch (IOException e2) {
            }
        } catch (Throwable th3) {
            outputStreamOpenOutputStream = outputStream;
            th = th3;
        }
    }

    private void attachToInLinedIntent() {
        LogHelper.d(TAG, "[attachToInLinedIntent]");
        Bitmap bitmapRotateAndMirror = PhotoModeHelper.rotateAndMirror(PhotoModeHelper.makeBitmap(this.mJpegData, 51200), CameraUtil.getOrientationFromExif(this.mJpegData), false);
        Intent intent = new Intent("inline-data");
        intent.putExtra("data", bitmapRotateAndMirror);
        this.mActivity.setResult(-1, intent);
        this.mActivity.finish();
    }

    private void attachToCropIntent() throws Throwable {
        LogHelper.d(TAG, "[attachToCropIntent]");
        Uri tempUri = getTempUri();
        if (tempUri == null) {
            LogHelper.w(TAG, "[attachToCropIntent] return because temp uri is null");
            return;
        }
        Bundle bundle = new Bundle();
        if ("circle".equalsIgnoreCase(this.mCropValue)) {
            bundle.putString("circleCrop", "true");
        }
        if (this.mSaveUri != null) {
            bundle.putParcelable("output", this.mSaveUri);
        } else {
            bundle.putBoolean("return-data", true);
        }
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setData(tempUri);
        intent.putExtras(bundle);
        this.mActivity.startActivityForResult(intent, 1000);
    }

    private Uri getTempUri() throws Throwable {
        FileOutputStream fileOutputStreamOpenFileOutput;
        File fileStreamPath = this.mActivity.getFileStreamPath("crop-temp");
        fileStreamPath.delete();
        try {
            try {
                fileOutputStreamOpenFileOutput = this.mActivity.openFileOutput("crop-temp", 0);
                try {
                    try {
                        fileOutputStreamOpenFileOutput.write(this.mJpegData);
                        fileOutputStreamOpenFileOutput.close();
                        Uri uriFromFile = Uri.fromFile(fileStreamPath);
                        PhotoModeHelper.closeSilently(fileOutputStreamOpenFileOutput);
                        return uriFromFile;
                    } catch (FileNotFoundException e) {
                        this.mActivity.setResult(0);
                        PhotoModeHelper.closeSilently(fileOutputStreamOpenFileOutput);
                        return null;
                    }
                } catch (IOException e2) {
                    this.mActivity.setResult(0);
                    PhotoModeHelper.closeSilently(fileOutputStreamOpenFileOutput);
                    return null;
                }
            } catch (Throwable th) {
                th = th;
                PhotoModeHelper.closeSilently(null);
                throw th;
            }
        } catch (FileNotFoundException e3) {
            fileOutputStreamOpenFileOutput = null;
        } catch (Throwable th2) {
            th = th2;
            PhotoModeHelper.closeSilently(null);
            throw th;
        }
    }
}
