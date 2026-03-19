package com.mediatek.camera.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.mediatek.camera.R;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.mode.IReviewUI;
import com.mediatek.camera.common.utils.CameraUtil;

public class ReviewUI implements IReviewUI {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ReviewUI.class.getSimpleName());
    private Activity mActivity;
    private IApp mApp;
    private Bitmap mBitmap;
    private int mOrientation;
    private ViewGroup mParentViewGroup;
    private ImageView mPlayButton;
    private ImageView mRetakeButton;
    private ImageView mReviewImage;
    private View mReviewRootView = null;
    private IReviewUI.ReviewSpec mReviewSpec;
    private ImageView mSaveButton;

    public ReviewUI(IApp iApp, ViewGroup viewGroup) {
        this.mApp = iApp;
        this.mActivity = this.mApp.getActivity();
        this.mParentViewGroup = viewGroup;
    }

    @Override
    public void initReviewUI(IReviewUI.ReviewSpec reviewSpec) {
        this.mReviewSpec = reviewSpec;
    }

    @Override
    public void showReviewUI(Bitmap bitmap) {
        this.mBitmap = bitmap;
        LogHelper.d(TAG, "[showReviewUI]");
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View viewInflate = ReviewUI.this.mActivity.getLayoutInflater().inflate(R.layout.review_layout, ReviewUI.this.mParentViewGroup, true);
                if (CameraUtil.isHasNavigationBar(ReviewUI.this.mApp.getActivity())) {
                    int navigationBarHeight = CameraUtil.getNavigationBarHeight(ReviewUI.this.mApp.getActivity());
                    RelativeLayout relativeLayout = (RelativeLayout) viewInflate.findViewById(R.id.review_btn_root);
                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) relativeLayout.getLayoutParams();
                    if (CameraUtil.isTablet()) {
                        int displayRotation = CameraUtil.getDisplayRotation(ReviewUI.this.mApp.getActivity());
                        LogHelper.d(ReviewUI.TAG, " showReviewUI displayRotation  " + displayRotation);
                        if (displayRotation == 90 || displayRotation == 270) {
                            ((ViewGroup.MarginLayoutParams) layoutParams).leftMargin += navigationBarHeight;
                            relativeLayout.setLayoutParams(layoutParams);
                        } else {
                            ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin += navigationBarHeight;
                            relativeLayout.setLayoutParams(layoutParams);
                        }
                    } else {
                        ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin += navigationBarHeight;
                        relativeLayout.setLayoutParams(layoutParams);
                    }
                }
                ReviewUI.this.mReviewRootView = viewInflate.findViewById(R.id.review_layout);
                CameraUtil.rotateRotateLayoutChildView(ReviewUI.this.mActivity, ReviewUI.this.mReviewRootView, ReviewUI.this.mApp.getGSensorOrientation(), false);
                ReviewUI.this.mReviewRootView.setVisibility(0);
                if (ReviewUI.this.mReviewSpec.playListener != null) {
                    ReviewUI.this.mPlayButton = (ImageView) ReviewUI.this.mReviewRootView.findViewById(R.id.btn_play);
                    ReviewUI.this.mPlayButton.setOnClickListener(ReviewUI.this.mReviewSpec.playListener);
                    ReviewUI.this.mPlayButton.setVisibility(0);
                }
                if (ReviewUI.this.mReviewSpec.saveListener != null) {
                    ReviewUI.this.mSaveButton = (ImageView) ReviewUI.this.mReviewRootView.findViewById(R.id.btn_save);
                    ReviewUI.this.mSaveButton.setOnClickListener(ReviewUI.this.mReviewSpec.saveListener);
                    ReviewUI.this.mSaveButton.setContentDescription("Done");
                    ReviewUI.this.mSaveButton.setVisibility(0);
                }
                if (ReviewUI.this.mReviewSpec.retakeListener != null) {
                    ReviewUI.this.mRetakeButton = (ImageView) ReviewUI.this.mReviewRootView.findViewById(R.id.btn_retake);
                    ReviewUI.this.mRetakeButton.setOnClickListener(ReviewUI.this.mReviewSpec.retakeListener);
                    ReviewUI.this.mReviewRootView.setVisibility(0);
                }
                if (ReviewUI.this.mBitmap != null) {
                    ReviewUI.this.mReviewImage = (ImageView) ReviewUI.this.mReviewRootView.findViewById(R.id.review_image);
                    ReviewUI.this.mReviewImage.setImageBitmap(ReviewUI.this.mBitmap);
                    ReviewUI.this.mReviewImage.setVisibility(0);
                }
            }
        });
    }

    @Override
    public void hideReviewUI() {
        LogHelper.d(TAG, "[hideReviewUI]");
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (ReviewUI.this.mReviewRootView != null) {
                    ReviewUI.this.mReviewRootView.setVisibility(4);
                }
                if (ReviewUI.this.mPlayButton != null) {
                    ReviewUI.this.mPlayButton.setVisibility(4);
                }
                if (ReviewUI.this.mRetakeButton != null) {
                    ReviewUI.this.mRetakeButton.setVisibility(4);
                }
                if (ReviewUI.this.mSaveButton != null) {
                    ReviewUI.this.mSaveButton.setVisibility(4);
                }
                if (ReviewUI.this.mBitmap != null) {
                    ReviewUI.this.mBitmap.recycle();
                    ReviewUI.this.mBitmap = null;
                }
                ReviewUI.this.mParentViewGroup.removeView(ReviewUI.this.mReviewRootView);
                ReviewUI.this.mReviewRootView = null;
            }
        });
    }

    @Override
    public void updateOrientation(int i) {
        LogHelper.d(TAG, "[updateOrientation] orientation = " + i);
        if (i == 0 || i == 90 || i == 180 || i == 270) {
            this.mOrientation = i;
            this.mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    CameraUtil.rotateRotateLayoutChildView(ReviewUI.this.mActivity, ReviewUI.this.mReviewRootView, ReviewUI.this.mOrientation, true);
                }
            });
            return;
        }
        LogHelper.e(TAG, "error orientation = " + i);
    }
}
