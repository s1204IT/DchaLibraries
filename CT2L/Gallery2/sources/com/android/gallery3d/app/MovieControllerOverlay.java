package com.android.gallery3d.app;

import android.content.Context;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.android.gallery3d.R;
import com.android.gallery3d.app.CommonControllerOverlay;

public class MovieControllerOverlay extends CommonControllerOverlay implements Animation.AnimationListener {
    private final Handler handler;
    private boolean hidden;
    private final Animation hideAnimation;
    private final Runnable startHidingRunnable;

    public MovieControllerOverlay(Context context) {
        super(context);
        this.handler = new Handler();
        this.startHidingRunnable = new Runnable() {
            @Override
            public void run() {
                MovieControllerOverlay.this.startHiding();
            }
        };
        this.hideAnimation = AnimationUtils.loadAnimation(context, R.anim.player_out);
        this.hideAnimation.setAnimationListener(this);
        hide();
    }

    @Override
    protected void createTimeBar(Context context) {
        this.mTimeBar = new TimeBar(context, this);
    }

    @Override
    public void hide() {
        boolean wasHidden = this.hidden;
        this.hidden = true;
        super.hide();
        if (this.mListener != null && wasHidden != this.hidden) {
            this.mListener.onHidden();
        }
    }

    @Override
    public void show() {
        boolean wasHidden = this.hidden;
        this.hidden = false;
        super.show();
        if (this.mListener != null && wasHidden != this.hidden) {
            this.mListener.onShown();
        }
        maybeStartHiding();
    }

    private void maybeStartHiding() {
        cancelHiding();
        if (this.mState == CommonControllerOverlay.State.PLAYING) {
            this.handler.postDelayed(this.startHidingRunnable, 2500L);
        }
    }

    private void startHiding() {
        startHideAnimation(this.mBackground);
        startHideAnimation(this.mTimeBar);
        startHideAnimation(this.mPlayPauseReplayView);
    }

    private void startHideAnimation(View view) {
        if (view.getVisibility() == 0) {
            view.startAnimation(this.hideAnimation);
        }
    }

    private void cancelHiding() {
        this.handler.removeCallbacks(this.startHidingRunnable);
        this.mBackground.setAnimation(null);
        this.mTimeBar.setAnimation(null);
        this.mPlayPauseReplayView.setAnimation(null);
    }

    @Override
    public void onAnimationStart(Animation animation) {
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        hide();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (this.hidden) {
            show();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!super.onTouchEvent(event)) {
            if (this.hidden) {
                show();
            } else {
                switch (event.getAction()) {
                    case 0:
                        cancelHiding();
                        if (this.mState == CommonControllerOverlay.State.PLAYING || this.mState == CommonControllerOverlay.State.PAUSED) {
                            this.mListener.onPlayPause();
                        }
                        break;
                    case 1:
                        maybeStartHiding();
                        break;
                }
            }
        }
        return true;
    }

    @Override
    protected void updateViews() {
        if (!this.hidden) {
            super.updateViews();
        }
    }

    @Override
    public void onScrubbingStart() {
        cancelHiding();
        super.onScrubbingStart();
    }

    @Override
    public void onScrubbingMove(int time) {
        cancelHiding();
        super.onScrubbingMove(time);
    }

    @Override
    public void onScrubbingEnd(int time, int trimStartTime, int trimEndTime) {
        maybeStartHiding();
        super.onScrubbingEnd(time, trimStartTime, trimEndTime);
    }
}
