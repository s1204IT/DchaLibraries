package com.android.camera.ui;

import android.content.Context;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import com.android.camera.CaptureLayoutHelper;
import com.android.camera.debug.Log;
import com.android.camera2.R;

public class BottomBarModeOptionsWrapper extends FrameLayout {
    private static final Log.Tag TAG = new Log.Tag("BottomBarWrapper");
    private View mBottomBar;
    private CaptureLayoutHelper mCaptureLayoutHelper;
    private View mModeOptionsOverlay;

    public BottomBarModeOptionsWrapper(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mCaptureLayoutHelper = null;
    }

    @Override
    public void onFinishInflate() {
        this.mModeOptionsOverlay = findViewById(R.id.mode_options_overlay);
        this.mBottomBar = findViewById(R.id.bottom_bar);
    }

    public void setCaptureLayoutHelper(CaptureLayoutHelper helper) {
        this.mCaptureLayoutHelper = helper;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (this.mCaptureLayoutHelper == null) {
            Log.e(TAG, "Capture layout helper needs to be set first.");
            return;
        }
        RectF uncoveredPreviewRect = this.mCaptureLayoutHelper.getUncoveredPreviewRect();
        RectF bottomBarRect = this.mCaptureLayoutHelper.getBottomBarRect();
        this.mModeOptionsOverlay.layout((int) uncoveredPreviewRect.left, (int) uncoveredPreviewRect.top, (int) uncoveredPreviewRect.right, (int) uncoveredPreviewRect.bottom);
        this.mBottomBar.layout((int) bottomBarRect.left, (int) bottomBarRect.top, (int) bottomBarRect.right, (int) bottomBarRect.bottom);
    }
}
