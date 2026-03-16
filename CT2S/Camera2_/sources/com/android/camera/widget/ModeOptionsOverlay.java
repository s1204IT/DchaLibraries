package com.android.camera.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.camera.CaptureLayoutHelper;
import com.android.camera.ShutterButton;
import com.android.camera.debug.Log;
import com.android.camera.ui.PreviewOverlay;
import com.android.camera.ui.TouchCoordinate;
import com.android.camera2.R;

public class ModeOptionsOverlay extends FrameLayout implements PreviewOverlay.OnPreviewTouchedListener, ShutterButton.OnShutterButtonListener {
    private static final int BOTTOMBAR_OPTIONS_TIMEOUT_MS = 2000;
    private static final int BOTTOM_RIGHT = 85;
    private static final Log.Tag TAG = new Log.Tag("ModeOptionsOverlay");
    private static final int TOP_RIGHT = 53;
    private CaptureLayoutHelper mCaptureLayoutHelper;
    private ModeOptions mModeOptions;
    private LinearLayout mModeOptionsToggle;
    private ImageView mThreeDots;

    public ModeOptionsOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mCaptureLayoutHelper = null;
    }

    public void setCaptureLayoutHelper(CaptureLayoutHelper helper) {
        this.mCaptureLayoutHelper = helper;
    }

    public void setToggleClickable(boolean clickable) {
        this.mModeOptionsToggle.setClickable(clickable);
    }

    @Override
    public void onFinishInflate() {
        this.mModeOptions = (ModeOptions) findViewById(R.id.mode_options);
        this.mModeOptions.setClickable(true);
        this.mModeOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ModeOptionsOverlay.this.closeModeOptions();
            }
        });
        this.mModeOptionsToggle = (LinearLayout) findViewById(R.id.mode_options_toggle);
        this.mModeOptionsToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ModeOptionsOverlay.this.mModeOptions.animateVisible();
            }
        });
        this.mModeOptions.setViewToShowHide(this.mModeOptionsToggle);
        this.mThreeDots = (ImageView) findViewById(R.id.three_dots);
    }

    @Override
    public void onPreviewTouched(MotionEvent ev) {
        closeModeOptions();
    }

    @Override
    public void onShutterButtonClick() {
        closeModeOptions();
    }

    @Override
    public void onShutterCoordinate(TouchCoordinate coord) {
    }

    @Override
    public void onShutterButtonFocus(boolean pressed) {
    }

    public void closeModeOptions() {
        this.mModeOptions.animateHidden();
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        checkOrientation(configuration.orientation);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.mCaptureLayoutHelper == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            Log.e(TAG, "Capture layout helper needs to be set first.");
        } else {
            RectF uncoveredPreviewRect = this.mCaptureLayoutHelper.getUncoveredPreviewRect();
            super.onMeasure(View.MeasureSpec.makeMeasureSpec((int) uncoveredPreviewRect.width(), 1073741824), View.MeasureSpec.makeMeasureSpec((int) uncoveredPreviewRect.height(), 1073741824));
        }
    }

    private void checkOrientation(int orientation) {
        boolean isPortrait = 1 == orientation;
        int modeOptionsDimension = (int) getResources().getDimension(R.dimen.mode_options_height);
        FrameLayout.LayoutParams modeOptionsParams = (FrameLayout.LayoutParams) this.mModeOptions.getLayoutParams();
        FrameLayout.LayoutParams modeOptionsToggleParams = (FrameLayout.LayoutParams) this.mModeOptionsToggle.getLayoutParams();
        if (isPortrait) {
            modeOptionsParams.height = modeOptionsDimension;
            modeOptionsParams.width = -1;
            modeOptionsParams.gravity = 80;
            modeOptionsToggleParams.gravity = BOTTOM_RIGHT;
            this.mThreeDots.setImageResource(R.drawable.ic_options_port);
        } else {
            modeOptionsParams.width = modeOptionsDimension;
            modeOptionsParams.height = -1;
            modeOptionsParams.gravity = 5;
            modeOptionsToggleParams.gravity = TOP_RIGHT;
            this.mThreeDots.setImageResource(R.drawable.ic_options_land);
        }
        requestLayout();
    }
}
