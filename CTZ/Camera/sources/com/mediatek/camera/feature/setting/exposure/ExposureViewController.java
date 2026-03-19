package com.mediatek.camera.feature.setting.exposure;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import com.mediatek.camera.R;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;

public class ExposureViewController {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ExposureViewController.class.getSimpleName());
    private Activity mActivity;
    private RelativeLayout mExpandView;
    private Exposure mExposure;
    private ExposureView mExposureView;
    private ViewGroup mFeatureRootView;

    public ExposureViewController(final IApp iApp, final Exposure exposure) {
        this.mActivity = iApp.getActivity();
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ExposureViewController.this.mExposure = exposure;
                ExposureViewController.this.mFeatureRootView = iApp.getAppUi().getPreviewFrameLayout();
                if (ExposureViewController.this.mFeatureRootView.findViewById(R.id.focus_view) == null) {
                    iApp.getActivity().getLayoutInflater().inflate(R.layout.focus_view, ExposureViewController.this.mFeatureRootView, true);
                }
                ExposureViewController.this.mExpandView = (RelativeLayout) ExposureViewController.this.mFeatureRootView.findViewById(R.id.expand_view);
                LogHelper.d(ExposureViewController.TAG, "ExposureViewController current EV = " + ExposureViewController.this.mFeatureRootView.findViewById(R.id.exposure_view));
                if (ExposureViewController.this.mFeatureRootView.findViewById(R.id.exposure_view) == null) {
                    iApp.getActivity().getLayoutInflater().inflate(R.layout.exposure_view, (ViewGroup) ExposureViewController.this.mExpandView, true);
                }
                ExposureViewController.this.mExposureView = (ExposureView) ExposureViewController.this.mFeatureRootView.findViewById(R.id.exposure_view);
                LogHelper.d(ExposureViewController.TAG, "ExposureViewController mExposureView EV = " + ExposureViewController.this.mExposureView);
                ExposureViewController.this.mExposureView.setListener(ExposureViewController.this.mExposure);
            }
        });
    }

    protected void resetExposureView() {
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (ExposureViewController.this.mExposureView == null) {
                    LogHelper.w(ExposureViewController.TAG, "[resetExposureView] mExposureView is null");
                    return;
                }
                int size = ExposureViewController.this.mExposure.getEntryValues().size();
                LogHelper.d(ExposureViewController.TAG, "[resetExposureView] size " + size);
                if (size <= 1) {
                    ExposureViewController.this.mExposureView.setListener(null);
                    ExposureViewController.this.mExposureView.setVisibility(8);
                } else {
                    ExposureViewController.this.mExposureView.setListener(ExposureViewController.this.mExposure);
                    ExposureViewController.this.mExposureView.setVisibility(0);
                    ExposureViewController.this.mExposureView.resetExposureView();
                }
            }
        });
    }

    protected void initExposureValues(final int[] iArr) {
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (ExposureViewController.this.mExposureView == null) {
                    LogHelper.w(ExposureViewController.TAG, "[initExposureValues] mExposureView is null");
                } else {
                    ExposureViewController.this.mExposureView.initExposureView(iArr);
                }
            }
        });
    }

    protected void setOrientation(final int i) {
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (ExposureViewController.this.mExposureView != null) {
                    ExposureViewController.this.mExposureView.setOrientation(i);
                }
            }
        });
    }

    protected boolean needUpdateExposureView() {
        return this.mExposureView != null && this.mExpandView.getVisibility() == 0 && this.mExposureView.getVisibility() == 0;
    }

    protected void onVerticalScroll(MotionEvent motionEvent, float f) {
        this.mExposureView.onVerticalScroll(motionEvent, f);
    }

    protected void onTrackingTouch(boolean z) {
        this.mExposureView.onTrackingTouch(z);
    }

    protected void setViewEnabled(final boolean z) {
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (ExposureViewController.this.mExposureView != null) {
                    ExposureViewController.this.mExposureView.setViewEnabled(z);
                }
            }
        });
    }
}
