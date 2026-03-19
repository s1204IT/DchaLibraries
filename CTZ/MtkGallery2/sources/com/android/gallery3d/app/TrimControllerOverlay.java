package com.android.gallery3d.app;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.MotionEvent;
import com.android.gallery3d.app.CommonControllerOverlay;
import com.android.gallery3d.common.ApiHelper;

public class TrimControllerOverlay extends CommonControllerOverlay {
    public TrimControllerOverlay(Context context) {
        super(context);
        int i = (int) (context.getResources().getDisplayMetrics().density * 10.0f);
        this.mTimeBar.setPadding(i, 0, i, 0);
    }

    @Override
    protected void createTimeBar(Context context) {
        this.mTimeBar = new TrimTimeBar(context, this);
    }

    private void hidePlayButtonIfPlaying() {
        if (this.mState == CommonControllerOverlay.State.PLAYING) {
            this.mPlayPauseReplayView.setVisibility(4);
        }
        if (ApiHelper.HAS_OBJECT_ANIMATION) {
            this.mPlayPauseReplayView.setAlpha(1.0f);
        }
    }

    @Override
    public void showPlaying() {
        super.showPlaying();
        if (ApiHelper.HAS_OBJECT_ANIMATION) {
            ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this.mPlayPauseReplayView, "alpha", 1.0f, 0.0f);
            objectAnimatorOfFloat.setDuration(200L);
            objectAnimatorOfFloat.start();
            objectAnimatorOfFloat.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    TrimControllerOverlay.this.hidePlayButtonIfPlaying();
                }

                @Override
                public void onAnimationCancel(Animator animator) {
                    TrimControllerOverlay.this.hidePlayButtonIfPlaying();
                }

                @Override
                public void onAnimationRepeat(Animator animator) {
                }
            });
            return;
        }
        hidePlayButtonIfPlaying();
    }

    @Override
    public void setTimes(int i, int i2, int i3, int i4) {
        this.mTimeBar.setTime(i, i2, i3, i4);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (!super.onTouchEvent(motionEvent) && motionEvent.getAction() == 0) {
            if (this.mState == CommonControllerOverlay.State.PLAYING || this.mState == CommonControllerOverlay.State.PAUSED) {
                this.mListener.onPlayPause();
            } else if (this.mState == CommonControllerOverlay.State.ENDED && this.mCanReplay) {
                this.mListener.onReplay();
            }
        }
        return true;
    }
}
