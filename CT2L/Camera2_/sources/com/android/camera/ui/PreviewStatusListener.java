package com.android.camera.ui;

import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.TextureView;
import android.view.View;

public interface PreviewStatusListener extends TextureView.SurfaceTextureListener {

    public interface PreviewAreaChangedListener {
        void onPreviewAreaChanged(RectF rectF);
    }

    public interface PreviewAspectRatioChangedListener {
        void onPreviewAspectRatioChanged(float f);
    }

    GestureDetector.OnGestureListener getGestureListener();

    View.OnTouchListener getTouchListener();

    void onPreviewFlipped();

    void onPreviewLayoutChanged(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8);

    boolean shouldAutoAdjustBottomBar();

    boolean shouldAutoAdjustTransformMatrixOnLayout();
}
