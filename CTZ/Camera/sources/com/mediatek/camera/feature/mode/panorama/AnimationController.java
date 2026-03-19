package com.mediatek.camera.feature.mode.panorama;

import android.os.Handler;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;

public class AnimationController {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(AnimationController.class.getSimpleName());
    private ViewGroup mCenterArrow;
    private ViewGroup[] mDirectionIndicators;
    private int mCenterDotIndex = 0;
    private int mDirectionDotIndex = 0;
    private Handler mHandler = new Handler();
    private Runnable mApplyCenterArrowAnim = new Runnable() {
        private int mDotCount = 0;

        @Override
        public void run() {
            if (this.mDotCount == 0) {
                this.mDotCount = AnimationController.this.mCenterArrow.getChildCount();
            }
            if (this.mDotCount <= AnimationController.this.mCenterDotIndex) {
                LogHelper.w(AnimationController.TAG, "[run]mApplyCenterArrowAnim return, mDotCount = " + this.mDotCount + ",mCenterDotIndex =" + AnimationController.this.mCenterDotIndex);
                return;
            }
            AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
            alphaAnimation.setDuration(1440L);
            alphaAnimation.setRepeatCount(-1);
            if (AnimationController.this.mCenterArrow != null) {
                AnimationController.this.mCenterArrow.getChildAt(AnimationController.this.mCenterDotIndex).startAnimation(alphaAnimation);
            }
            alphaAnimation.startNow();
            AnimationController.access$108(AnimationController.this);
            AnimationController.this.mHandler.postDelayed(this, 360 / this.mDotCount);
        }
    };
    private Runnable mApplyDirectionAnim = new Runnable() {
        private int mDotCount = 0;

        @Override
        public void run() {
            for (ViewGroup viewGroup : AnimationController.this.mDirectionIndicators) {
                if (viewGroup == null) {
                    LogHelper.w(AnimationController.TAG, "[run]viewGroup is null,return!");
                    return;
                }
            }
            if (this.mDotCount == 0) {
                this.mDotCount = AnimationController.this.mDirectionIndicators[0].getChildCount();
            }
            if (this.mDotCount <= AnimationController.this.mDirectionDotIndex) {
                LogHelper.i(AnimationController.TAG, "[run]mApplyDirectionAnim,return,mDotCount = " + this.mDotCount + ",mCenterDotIndex =" + AnimationController.this.mCenterDotIndex);
                return;
            }
            AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
            alphaAnimation.setDuration(((180 * this.mDotCount) * 3) / 2);
            alphaAnimation.setRepeatCount(-1);
            AnimationController.this.mDirectionIndicators[0].getChildAt(AnimationController.this.mDirectionDotIndex).startAnimation(alphaAnimation);
            AnimationController.this.mDirectionIndicators[1].getChildAt((this.mDotCount - AnimationController.this.mDirectionDotIndex) - 1).startAnimation(alphaAnimation);
            AnimationController.this.mDirectionIndicators[2].getChildAt((this.mDotCount - AnimationController.this.mDirectionDotIndex) - 1).startAnimation(alphaAnimation);
            AnimationController.this.mDirectionIndicators[3].getChildAt(AnimationController.this.mDirectionDotIndex).startAnimation(alphaAnimation);
            alphaAnimation.startNow();
            AnimationController.access$508(AnimationController.this);
            AnimationController.this.mHandler.postDelayed(this, 90L);
        }
    };

    static int access$108(AnimationController animationController) {
        int i = animationController.mCenterDotIndex;
        animationController.mCenterDotIndex = i + 1;
        return i;
    }

    static int access$508(AnimationController animationController) {
        int i = animationController.mDirectionDotIndex;
        animationController.mDirectionDotIndex = i + 1;
        return i;
    }

    public AnimationController(ViewGroup[] viewGroupArr, ViewGroup viewGroup) {
        this.mDirectionIndicators = viewGroupArr;
        this.mCenterArrow = viewGroup;
    }

    public void startDirectionAnimation() {
        LogHelper.d(TAG, "[startDirectionAnimation]...");
        this.mDirectionDotIndex = 0;
        this.mApplyDirectionAnim.run();
    }

    public void startCenterAnimation() {
        LogHelper.d(TAG, "[startCenterAnimation]...");
        this.mCenterDotIndex = 0;
        this.mApplyCenterArrowAnim.run();
    }

    public void stopCenterAnimation() {
        LogHelper.d(TAG, "[stopCenterAnimation]...");
        if (this.mCenterArrow != null) {
            for (int i = 0; i < this.mCenterArrow.getChildCount(); i++) {
                this.mCenterArrow.getChildAt(i).clearAnimation();
            }
        }
    }
}
