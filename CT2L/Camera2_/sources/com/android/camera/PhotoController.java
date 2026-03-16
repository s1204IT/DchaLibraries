package com.android.camera;

import android.view.View;
import com.android.camera.ShutterButton;

public interface PhotoController extends ShutterButton.OnShutterButtonListener {
    public static final int FOCUSING = 2;
    public static final int IDLE = 1;
    public static final int PREVIEW_STOPPED = 0;
    public static final int SNAPSHOT_IN_PROGRESS = 3;
    public static final int SWITCHING_CAMERA = 4;

    void cancelAutoFocus();

    int getCameraState();

    boolean isCameraIdle();

    boolean isImageCaptureIntent();

    void onCaptureCancelled();

    void onCaptureDone();

    void onCaptureRetake();

    void onPreviewUIDestroyed();

    void onPreviewUIReady();

    void onSingleTapUp(View view, int i, int i2);

    void onZoomChanged(float f);

    void startPreCaptureAnimation();

    void stopPreview();

    void updateCameraOrientation();

    void updatePreviewAspectRatio(float f);
}
