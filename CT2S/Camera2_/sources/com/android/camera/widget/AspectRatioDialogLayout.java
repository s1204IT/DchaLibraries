package com.android.camera.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import com.android.camera.widget.AspectRatioSelector;
import com.android.camera2.R;

public class AspectRatioDialogLayout extends FrameLayout {
    private AspectRatioSelector.AspectRatio mAspectRatio;
    private AspectRatioSelector mAspectRatioSelector;
    private View mConfirmButton;
    private boolean mInitialized;
    private int mLastOrientation;
    private AspectRatioChangedListener mListener;

    public interface AspectRatioChangedListener {
        void onAspectRatioChanged(AspectRatioSelector.AspectRatio aspectRatio);
    }

    public AspectRatioDialogLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mLastOrientation = getResources().getConfiguration().orientation;
    }

    @Override
    public void onFinishInflate() {
        updateViewReference();
    }

    private void updateViewReference() {
        this.mAspectRatioSelector = (AspectRatioSelector) findViewById(R.id.aspect_ratio_selector);
        this.mConfirmButton = findViewById(R.id.confirm_button);
        this.mConfirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AspectRatioDialogLayout.this.mListener != null) {
                    AspectRatioDialogLayout.this.mListener.onAspectRatioChanged(AspectRatioDialogLayout.this.mAspectRatioSelector.getAspectRatio());
                }
            }
        });
        if (this.mInitialized) {
            this.mAspectRatioSelector.setAspectRatio(this.mAspectRatio);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        if (config.orientation != this.mLastOrientation) {
            this.mLastOrientation = config.orientation;
            this.mAspectRatio = this.mAspectRatioSelector.getAspectRatio();
            removeAllViews();
            inflate(getContext(), R.layout.aspect_ratio_dialog_content, this);
            updateViewReference();
        }
    }

    public void setAspectRatio(AspectRatioSelector.AspectRatio aspectRatio) {
        this.mAspectRatioSelector.setAspectRatio(aspectRatio);
    }

    public void initialize(AspectRatioChangedListener listener, AspectRatioSelector.AspectRatio aspectRatio) {
        this.mInitialized = true;
        this.mListener = listener;
        this.mAspectRatio = aspectRatio;
        if (this.mAspectRatioSelector != null) {
            this.mAspectRatioSelector.setAspectRatio(this.mAspectRatio);
        }
    }
}
