package com.mediatek.camera.common.mode.video.intentvideo;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.mode.IReviewUI;
import com.mediatek.camera.common.mode.video.VideoHelper;
import com.mediatek.camera.common.mode.video.VideoMode;
import com.mediatek.camera.common.mode.video.device.IDeviceController;
import com.mediatek.camera.common.mode.video.recorder.IRecorder;
import com.mediatek.camera.common.mode.video.videoui.IVideoUI;
import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.storage.MediaSaver;
import com.mediatek.camera.common.utils.BitmapCreator;
import com.mediatek.camera.portability.MediaRecorderEx;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class IntentVideoMode extends VideoMode {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(IntentVideoMode.class.getSimpleName());
    private Bitmap mBitmap;
    private ContentResolver mContentResolver;
    private Uri mCurrentVideoUri;
    private String mFilePath;
    private Intent mIntent;
    private int mLimitDuration;
    private long mLimitSize;
    private IReviewUI mReviewUI;
    private ParcelFileDescriptor mVideoFileDescriptor;
    private boolean mIsReviewUIShowing = false;
    private IDeviceController.DeviceCallback mPreviewStartCallback = new IDeviceController.DeviceCallback() {
        @Override
        public void onCameraOpened(String str) {
            IntentVideoMode.this.updateModeDeviceState("opened");
        }

        @Override
        public void afterStopPreview() {
            IntentVideoMode.this.updateModeDeviceState("opened");
        }

        @Override
        public void beforeCloseCamera() {
            IntentVideoMode.this.updateModeDeviceState("closed");
        }

        @Override
        public void onPreviewStart() {
            if (IntentVideoMode.this.mIsReviewUIShowing) {
                IntentVideoMode.this.updateVideoState(VideoMode.VideoState.STATE_REVIEW_UI);
            } else {
                IntentVideoMode.this.updateVideoState(VideoMode.VideoState.STATE_PREVIEW);
            }
            IntentVideoMode.this.mAppUi.applyAllUIEnabled(true);
            IntentVideoMode.this.updateModeDeviceState("previewing");
            LogHelper.d(IntentVideoMode.TAG, "[onPreviewStart]");
        }

        @Override
        public void onError() {
            if (IntentVideoMode.this.getVideoState() == VideoMode.VideoState.STATE_PAUSED || IntentVideoMode.this.getVideoState() == VideoMode.VideoState.STATE_RECORDING) {
                IntentVideoMode.this.onShutterButtonClick();
            }
        }
    };
    private MediaSaver.MediaSaverListener mFileSavedListener = new MediaSaver.MediaSaverListener() {
        @Override
        public void onFileSaved(Uri uri) {
            LogHelper.d(IntentVideoMode.TAG, "[onFileSaved] uri = " + uri);
            if (IntentVideoMode.this.getVideoState() != VideoMode.VideoState.STATE_RECORDING && IntentVideoMode.this.getVideoState() != VideoMode.VideoState.STATE_PAUSED) {
                if (IntentVideoMode.this.mBitmap != null) {
                    IntentVideoMode.this.mBitmap.recycle();
                    IntentVideoMode.this.mBitmap = null;
                }
                if (IntentVideoMode.this.mCurrentVideoUri == null) {
                    IntentVideoMode.this.mCurrentVideoUri = uri;
                }
                IntentVideoMode.this.mBitmap = BitmapCreator.createBitmapFromVideo(IntentVideoMode.this.mFilePath, IntentVideoMode.this.getProfile().videoFrameWidth);
                IntentVideoMode.this.mReviewUI.showReviewUI(IntentVideoMode.this.mBitmap);
                IntentVideoMode.this.mIsReviewUIShowing = true;
                IntentVideoMode.this.updateVideoState(VideoMode.VideoState.STATE_PREVIEW);
                IntentVideoMode.this.mAppUi.hideSavingDialog();
                IntentVideoMode.this.mAppUi.applyAllUIVisibility(0);
                IntentVideoMode.this.mAppUi.setUIVisibility(5, 4);
                IntentVideoMode.this.mAppUi.setUIVisibility(8, 4);
            }
        }
    };
    private View.OnClickListener mRetakeListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            LogHelper.d(IntentVideoMode.TAG, "[mRetakeListener] onClick");
            IntentVideoMode.this.mIsReviewUIShowing = false;
            IntentVideoMode.this.updateVideoState(VideoMode.VideoState.STATE_PREVIEW);
            IntentVideoMode.this.mReviewUI.hideReviewUI();
            IntentVideoMode.this.mAppUi.setUIVisibility(5, 0);
            IntentVideoMode.this.mAppUi.setUIVisibility(8, 0);
            IntentVideoMode.this.mAppUi.applyAllUIEnabled(true);
            if (IntentVideoMode.this.mBitmap != null) {
                IntentVideoMode.this.mBitmap.recycle();
                IntentVideoMode.this.mBitmap = null;
            }
            IntentVideoMode.this.deleteCurrentVideo();
        }
    };
    private View.OnClickListener mPlayListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            LogHelper.d(IntentVideoMode.TAG, "[mPlayListener] onClick");
            IntentVideoMode.this.startPlayVideoActivity(IntentVideoMode.this.mCurrentVideoUri, IntentVideoMode.this.getProfile());
        }
    };
    private View.OnClickListener mSaveListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            LogHelper.d(IntentVideoMode.TAG, "[mSaveListener] onClick");
            IntentVideoMode.this.doReturnToCaller(IntentVideoMode.this.mCurrentVideoUri);
            ((VideoMode) IntentVideoMode.this).mApp.getActivity().finish();
        }
    };

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, boolean z) {
        super.init(iApp, iCameraContext, z);
        this.mIntent = this.mApp.getActivity().getIntent();
    }

    @Override
    public void onOrientationChanged(int i) {
        super.onOrientationChanged(i);
        if (this.mReviewUI != null) {
            this.mReviewUI.updateOrientation(i);
        }
    }

    @Override
    public void unInit() {
        super.unInit();
        if (this.mReviewUI != null) {
            this.mReviewUI.hideReviewUI();
            if (this.mBitmap != null) {
                this.mBitmap.recycle();
                this.mBitmap = null;
            }
        }
    }

    @Override
    protected IRecorder.RecorderSpec modifyRecorderSpec(IRecorder.RecorderSpec recorderSpec, boolean z) {
        if (!z) {
            return recorderSpec;
        }
        analysisIntent();
        recorderSpec.maxDurationMs = this.mLimitDuration * 1000;
        if (this.mLimitSize > 0 && this.mLimitSize < this.mVideoHelper.getRecorderMaxSize()) {
            recorderSpec.maxFileSizeBytes = this.mLimitSize;
        }
        if (this.mVideoFileDescriptor != null) {
            recorderSpec.outFileDes = this.mVideoFileDescriptor.getFileDescriptor();
            LogHelper.d(TAG, " recorderSpec.outFileDes = " + recorderSpec.outFileDes);
        }
        return recorderSpec;
    }

    @Override
    protected IVideoUI.UISpec modifyUISpec(IVideoUI.UISpec uISpec) {
        if (this.mLimitSize > 0 && this.mIsParameterExtraCanUse) {
            uISpec.recordingTotalSize = this.mLimitSize;
        }
        uISpec.isSupportedVss = false;
        return uISpec;
    }

    @Override
    protected void addFileToMediaStore() {
        int recordingRotation;
        initReviewUI();
        configReviewUI();
        if (this.mVideoFileDescriptor != null) {
            FileDescriptor fileDescriptor = this.mVideoFileDescriptor.getFileDescriptor();
            if (this.mBitmap != null) {
                this.mBitmap.recycle();
                this.mBitmap = null;
            }
            this.mBitmap = BitmapCreator.createBitmapFromVideo(fileDescriptor, getProfile().videoFrameWidth);
            this.mReviewUI.showReviewUI(this.mBitmap);
            this.mIsReviewUIShowing = true;
            this.mAppUi.hideSavingDialog();
            this.mAppUi.applyAllUIVisibility(0);
            this.mAppUi.setUIVisibility(5, 4);
            this.mAppUi.setUIVisibility(8, 4);
            updateVideoState(VideoMode.VideoState.STATE_REVIEW_UI);
            return;
        }
        if (getCameraApi() == CameraDeviceManagerFactory.CameraApi.API1) {
            VideoHelper videoHelper = this.mVideoHelper;
            recordingRotation = VideoHelper.getRecordingRotation(this.mApp.getGSensorOrientation(), this.mCameraDevice.getCameraInfo(0));
        } else {
            VideoHelper videoHelper2 = this.mVideoHelper;
            recordingRotation = VideoHelper.getRecordingRotation(this.mApp.getGSensorOrientation(), this.mVideoHelper.getCameraCharacteristics(this.mApp.getActivity(), this.mCameraId));
        }
        this.mCameraContext.getMediaSaver().addSaveRequest(modifyContentValues(this.mVideoHelper.prepareContentValues(true, recordingRotation, null)), this.mVideoHelper.getVideoTempPath(), this.mFileSavedListener);
    }

    @Override
    protected void setMediaRecorderParameters() {
        try {
            super.setMediaRecorderParameters();
            if (this.mLimitSize > 0) {
                MediaRecorder mediaRecorder = this.mRecorder.getMediaRecorder();
                StringBuilder sb = new StringBuilder();
                VideoHelper videoHelper = this.mVideoHelper;
                sb.append("media-recorder-info=");
                VideoHelper videoHelper2 = this.mVideoHelper;
                sb.append(895);
                MediaRecorderEx.setParametersExtra(mediaRecorder, sb.toString());
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (RuntimeException e2) {
            e2.printStackTrace();
        } catch (Exception e3) {
            e3.printStackTrace();
        }
    }

    @Override
    protected ContentValues modifyContentValues(ContentValues contentValues) {
        if (this.mVideoFileDescriptor == null) {
            this.mFilePath = contentValues.getAsString("_data");
        }
        return contentValues;
    }

    @Override
    protected Relation getPreviewedRestriction() {
        return IntentRestriction.getPreviewRelation().getRelation("preview", true);
    }

    @Override
    protected IDeviceController.DeviceCallback getPreviewStartCallback() {
        return this.mPreviewStartCallback;
    }

    @Override
    protected List<Relation> getRecordedRestriction(boolean z) {
        ArrayList arrayList = new ArrayList();
        if (z) {
            if (!this.mVideoHelper.isPDAFSupported(this.mCameraApi)) {
                arrayList.add(IntentRestriction.getRecordingRelationGroupForMode().getRelation("recording", true));
            }
        } else {
            arrayList.add(IntentRestriction.getRecordingRelationGroupForMode().getRelation("stop-recording", true));
        }
        return arrayList;
    }

    private void initReviewUI() {
        this.mReviewUI = this.mAppUi.getReviewUI();
    }

    private void configReviewUI() {
        LogHelper.d(TAG, "[configReviewUI]");
        IReviewUI.ReviewSpec reviewSpec = new IReviewUI.ReviewSpec();
        reviewSpec.retakeListener = this.mRetakeListener;
        reviewSpec.playListener = this.mPlayListener;
        reviewSpec.saveListener = this.mSaveListener;
        this.mReviewUI.initReviewUI(reviewSpec);
    }

    private void analysisIntent() {
        this.mContentResolver = this.mApp.getActivity().getContentResolver();
        this.mLimitSize = this.mIntent.getLongExtra("android.intent.extra.sizeLimit", 0L);
        this.mLimitDuration = this.mIntent.getIntExtra("android.intent.extra.durationLimit", 0);
        this.mCurrentVideoUri = null;
        Uri uri = (Uri) this.mIntent.getParcelableExtra("output");
        if (uri != null) {
            try {
                this.mVideoFileDescriptor = this.mContentResolver.openFileDescriptor(uri, "rw");
                this.mCurrentVideoUri = uri;
            } catch (FileNotFoundException e) {
                LogHelper.e(TAG, e.toString());
            }
        }
    }

    private void startPlayVideoActivity(Uri uri, CamcorderProfile camcorderProfile) {
        boolean z;
        LogHelper.d(TAG, "[startPlayVideoActivity], mCurrentVideoUri = " + uri + ",profile = " + camcorderProfile);
        if (camcorderProfile == null) {
            LogHelper.e(TAG, "[startPlayVideoActivity] current proflie is error,please check!");
        }
        Bundle extras = this.mIntent.getExtras();
        if (extras != null) {
            z = extras.getBoolean("CanShare", true);
        } else {
            z = true;
        }
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setFlags(1);
        intent.putExtra("CanShare", z);
        intent.setDataAndType(uri, this.mVideoHelper.convertOutputFormatToMimeType(camcorderProfile.fileFormat));
        try {
            this.mApp.getActivity().startActivity(intent);
        } catch (ActivityNotFoundException e) {
            LogHelper.e(TAG, "[startPlayVideoActivity] Couldn't view video " + uri, e);
        }
    }

    private void doReturnToCaller(Uri uri) {
        LogHelper.d(TAG, "[doReturnToCaller] uri = " + uri);
        Intent intent = new Intent();
        this.mApp.getActivity();
        this.mApp.getActivity();
        intent.setData(uri);
        intent.addFlags(1);
        LogHelper.d(TAG, "[doReturnToCaller] uri = " + uri);
        this.mApp.getActivity().setResult(-1, intent);
    }

    private void deleteCurrentVideo() {
        LogHelper.i(TAG, "[deleteCurrentVideo()] mCurrentVideoUri = " + this.mCurrentVideoUri);
        if (this.mCurrentVideoUri != null) {
            this.mContentResolver.delete(this.mCurrentVideoUri, null, null);
            this.mCurrentVideoUri = null;
        }
    }
}
