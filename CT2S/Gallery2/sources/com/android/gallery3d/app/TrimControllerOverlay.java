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
            ObjectAnimator anim = ObjectAnimator.ofFloat(this.mPlayPauseReplayView, "alpha", 1.0f, 0.0f);
            anim.setDuration(200L);
            anim.start();
            anim.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    TrimControllerOverlay.this.hidePlayButtonIfPlaying();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    TrimControllerOverlay.this.hidePlayButtonIfPlaying();
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
            return;
        }
        hidePlayButtonIfPlaying();
    }

    @Override
    public void setTimes(int currentTime, int totalTime, int trimStartTime, int trimEndTime) {
        this.mTimeBar.setTime(currentTime, totalTime, trimStartTime, trimEndTime);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!super.onTouchEvent(event)) {
            switch (event.getAction()) {
                case 0:
                    if (this.mState == CommonControllerOverlay.State.PLAYING || this.mState == CommonControllerOverlay.State.PAUSED) {
                        this.mListener.onPlayPause();
                        break;
                    } else {
                        if (this.mState == CommonControllerOverlay.State.ENDED && this.mCanReplay) {
                            this.mListener.onReplay();
                        }
                        break;
                    }
                default:
                    return true;
            }
        }
        return true;
    }
}
