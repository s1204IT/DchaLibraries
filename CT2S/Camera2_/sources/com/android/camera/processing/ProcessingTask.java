package com.android.camera.processing;

import android.content.Context;
import android.location.Location;
import com.android.camera.app.CameraServices;
import com.android.camera.session.CaptureSession;

public interface ProcessingTask {

    public interface ProcessingTaskDoneListener {
        void onDone(ProcessingResult processingResult);
    }

    Location getLocation();

    String getName();

    CaptureSession getSession();

    ProcessingResult process(Context context, CameraServices cameraServices, CaptureSession captureSession);

    void resume();

    void setDoneListener(ProcessingTaskDoneListener processingTaskDoneListener);

    void suspend();

    public static class ProcessingResult {
        public final CaptureSession mSession;
        public final boolean mSuccess;

        public ProcessingResult(boolean success, CaptureSession session) {
            this.mSuccess = success;
            this.mSession = session;
        }
    }
}
