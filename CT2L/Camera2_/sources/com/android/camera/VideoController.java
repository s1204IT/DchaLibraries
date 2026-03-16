package com.android.camera;

import android.view.View;
import com.android.camera.ShutterButton;

public interface VideoController extends ShutterButton.OnShutterButtonListener {
    boolean isInReviewMode();

    boolean isVideoCaptureIntent();

    void onPreviewUIDestroyed();

    void onPreviewUIReady();

    void onReviewCancelClicked(View view);

    void onReviewDoneClicked(View view);

    void onReviewPlayClicked(View view);

    void onSingleTapUp(View view, int i, int i2);

    void onZoomChanged(float f);

    void startPreCaptureAnimation();

    void stopPreview();

    void updateCameraOrientation();

    void updatePreviewAspectRatio(float f);
}
