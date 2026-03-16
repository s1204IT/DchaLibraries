package com.android.camera.one;

import android.os.Build;
import com.android.camera.one.OneCamera;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public abstract class AbstractOneCamera implements OneCamera {
    static final int DEBUG_FOLDER_SERIAL_LENGTH = 4;
    protected OneCamera.CameraErrorListener mCameraErrorListener;
    protected OneCamera.FocusStateListener mFocusStateListener;
    protected OneCamera.ReadyStateChangedListener mReadyStateChangedListener;

    @Override
    public final void setCameraErrorListener(OneCamera.CameraErrorListener listener) {
        this.mCameraErrorListener = listener;
    }

    @Override
    public final void setFocusStateListener(OneCamera.FocusStateListener listener) {
        this.mFocusStateListener = listener;
    }

    @Override
    public void setReadyStateChangedListener(OneCamera.ReadyStateChangedListener listener) {
        this.mReadyStateChangedListener = listener;
    }

    protected static String makeDebugDir(File root, String folderName) {
        if (root == null) {
            return null;
        }
        if (!root.exists() || !root.isDirectory()) {
            throw new RuntimeException("Gcam debug directory not valid or doesn't exist: " + root.getAbsolutePath());
        }
        String serialSubstring = "";
        String serial = Build.SERIAL;
        if (serial != null) {
            int length = serial.length();
            if (length > 4) {
                serialSubstring = serial.substring(length - 4, length);
            } else {
                serialSubstring = serial;
            }
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS");
        simpleDateFormat.setTimeZone(TimeZone.getDefault());
        String currentDateAndTime = simpleDateFormat.format(new Date());
        String burstFolderName = String.format("%s_%s", serialSubstring, currentDateAndTime);
        File destFolder = new File(new File(root, folderName), burstFolderName);
        if (!destFolder.mkdirs()) {
            throw new RuntimeException("Could not create Gcam debug data folder.");
        }
        return destFolder.getAbsolutePath();
    }

    protected void broadcastReadyState(boolean readyForCapture) {
        if (this.mReadyStateChangedListener != null) {
            this.mReadyStateChangedListener.onReadyStateChanged(readyForCapture);
        }
    }

    @Override
    public float getMaxZoom() {
        return 1.0f;
    }

    @Override
    public void setZoom(float zoom) {
    }
}
