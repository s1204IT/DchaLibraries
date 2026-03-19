package com.mediatek.camera.common.mode.video.recorder;

import android.media.MediaRecorder;
import android.view.Surface;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.mode.video.recorder.IRecorder;
import java.io.IOException;

public class NormalRecorder implements IRecorder {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(NormalRecorder.class.getSimpleName());
    protected MediaRecorder mMediaRecorder = new MediaRecorder();

    @Override
    public void init(IRecorder.RecorderSpec recorderSpec) {
        StringBuffer stringBuffer = new StringBuffer();
        checkRecorderSpec(recorderSpec);
        if (recorderSpec.camera != null) {
            this.mMediaRecorder.setCamera(recorderSpec.camera);
        }
        this.mMediaRecorder.setVideoSource(recorderSpec.videoSource);
        if (recorderSpec.isRecordAudio) {
            this.mMediaRecorder.setAudioSource(recorderSpec.audioSource);
            this.mMediaRecorder.setProfile(recorderSpec.profile);
        } else {
            this.mMediaRecorder.setOutputFormat(recorderSpec.profile.fileFormat);
            if (recorderSpec.videoFrameRate != 0) {
                this.mMediaRecorder.setVideoFrameRate(recorderSpec.videoFrameRate);
            } else {
                this.mMediaRecorder.setVideoFrameRate(recorderSpec.profile.videoFrameRate);
            }
            this.mMediaRecorder.setVideoEncodingBitRate(recorderSpec.profile.videoBitRate);
            this.mMediaRecorder.setVideoSize(recorderSpec.profile.videoFrameWidth, recorderSpec.profile.videoFrameHeight);
            this.mMediaRecorder.setVideoEncoder(recorderSpec.profile.videoCodec);
        }
        if (recorderSpec.captureRate != 0) {
            this.mMediaRecorder.setCaptureRate(recorderSpec.captureRate);
        }
        this.mMediaRecorder.setMaxDuration(recorderSpec.maxDurationMs);
        if (recorderSpec.location != null) {
            this.mMediaRecorder.setLocation((float) recorderSpec.location.getLatitude(), (float) recorderSpec.location.getLongitude());
        }
        try {
            this.mMediaRecorder.setMaxFileSize(recorderSpec.maxFileSizeBytes);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        if (recorderSpec.outFileDes != null) {
            this.mMediaRecorder.setOutputFile(recorderSpec.outFileDes);
            stringBuffer.append("  filePath = " + recorderSpec.outFileDes);
        } else {
            this.mMediaRecorder.setOutputFile(recorderSpec.outFilePath);
            stringBuffer.append("  filePath = " + recorderSpec.outFilePath);
        }
        this.mMediaRecorder.setOrientationHint(recorderSpec.orientationHint);
        this.mMediaRecorder.setOnErrorListener(recorderSpec.errorListener);
        this.mMediaRecorder.setOnInfoListener(recorderSpec.infoListener);
        stringBuffer.append("  spec.captureRate = " + recorderSpec.captureRate);
        stringBuffer.append("  spec.videoFrameRate = " + recorderSpec.videoFrameRate);
        stringBuffer.append("  spec.orientationHint = " + recorderSpec.orientationHint);
        stringBuffer.append("  spec.profile.videoFrameRate = " + recorderSpec.profile.videoFrameRate);
        stringBuffer.append("  spec.profile.videoFrameWidth = " + recorderSpec.profile.videoFrameWidth);
        stringBuffer.append("  spec.profile.videoFrameHeight = " + recorderSpec.profile.videoFrameHeight);
        LogHelper.d(TAG, "[init] " + stringBuffer.toString());
    }

    @Override
    public MediaRecorder getMediaRecorder() {
        return this.mMediaRecorder;
    }

    @Override
    public Surface getSurface() {
        return this.mMediaRecorder.getSurface();
    }

    @Override
    public void prepare() {
        try {
            this.mMediaRecorder.prepare();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start() {
        this.mMediaRecorder.start();
    }

    @Override
    public void stop() {
        this.mMediaRecorder.stop();
    }

    @Override
    public void reset() {
        this.mMediaRecorder.reset();
    }

    @Override
    public void release() {
        this.mMediaRecorder.setOnErrorListener(null);
        this.mMediaRecorder.setOnInfoListener(null);
        this.mMediaRecorder.release();
    }

    private void checkRecorderSpec(IRecorder.RecorderSpec recorderSpec) {
        if (recorderSpec.profile == null || (recorderSpec.outFileDes == null && recorderSpec.outFilePath == null)) {
            LogHelper.e(TAG, "profile = " + recorderSpec.profile + " outFileDes = " + recorderSpec.outFileDes + " outFilePath = " + recorderSpec.outFilePath);
            throw new IllegalArgumentException();
        }
        int i = recorderSpec.orientationHint;
        if (i != 0 && i != 90 && i != 180 && i != 270) {
            LogHelper.e(TAG, "orientationHint = " + recorderSpec.orientationHint);
            throw new IllegalArgumentException();
        }
        switch (recorderSpec.videoSource) {
            case 0:
            case Camera2Proxy.TEMPLATE_PREVIEW:
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                return;
            default:
                LogHelper.e(TAG, "videoSource = " + recorderSpec.videoSource);
                throw new IllegalArgumentException();
        }
    }
}
