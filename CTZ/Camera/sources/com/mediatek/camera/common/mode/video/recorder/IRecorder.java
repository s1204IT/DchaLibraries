package com.mediatek.camera.common.mode.video.recorder;

import android.hardware.Camera;
import android.location.Location;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.view.Surface;
import java.io.FileDescriptor;

public interface IRecorder {

    public static class RecorderSpec {
        public int orientationHint;
        public boolean isRecordAudio = true;
        public int audioSource = 0;
        public Camera camera = null;
        public int videoSource = 0;
        public String outFilePath = null;
        public FileDescriptor outFileDes = null;
        public CamcorderProfile profile = null;
        public Location location = null;
        public int maxDurationMs = 0;
        public long maxFileSizeBytes = 0;
        public MediaRecorder.OnInfoListener infoListener = null;
        public MediaRecorder.OnErrorListener errorListener = null;
        public MediaRecorder.OnInfoListener releaseListener = null;
        public int captureRate = 0;
        public int videoFrameRate = 0;
    }

    MediaRecorder getMediaRecorder();

    Surface getSurface();

    void init(RecorderSpec recorderSpec);

    void prepare();

    void release();

    void reset();

    void start();

    void stop();
}
