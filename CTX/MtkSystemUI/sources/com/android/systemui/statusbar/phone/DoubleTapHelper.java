package com.android.systemui.statusbar.phone;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import com.android.systemui.R;

public class DoubleTapHelper {
    private boolean mActivated;
    private final ActivationListener mActivationListener;
    private float mActivationX;
    private float mActivationY;
    private final DoubleTapListener mDoubleTapListener;
    private final DoubleTapLogListener mDoubleTapLogListener;
    private float mDoubleTapSlop;
    private float mDownX;
    private float mDownY;
    private final SlideBackListener mSlideBackListener;
    private Runnable mTapTimeoutRunnable = new Runnable() {
        @Override
        public final void run() {
            this.f$0.makeInactive();
        }
    };
    private float mTouchSlop;
    private boolean mTrackTouch;
    private final View mView;

    @FunctionalInterface
    public interface ActivationListener {
        void onActiveChanged(boolean z);
    }

    @FunctionalInterface
    public interface DoubleTapListener {
        boolean onDoubleTap();
    }

    @FunctionalInterface
    public interface DoubleTapLogListener {
        void onDoubleTapLog(boolean z, float f, float f2);
    }

    @FunctionalInterface
    public interface SlideBackListener {
        boolean onSlideBack();
    }

    public DoubleTapHelper(View view, ActivationListener activationListener, DoubleTapListener doubleTapListener, SlideBackListener slideBackListener, DoubleTapLogListener doubleTapLogListener) {
        this.mTouchSlop = ViewConfiguration.get(view.getContext()).getScaledTouchSlop();
        this.mDoubleTapSlop = view.getResources().getDimension(R.dimen.double_tap_slop);
        this.mView = view;
        this.mActivationListener = activationListener;
        this.mDoubleTapListener = doubleTapListener;
        this.mSlideBackListener = slideBackListener;
        this.mDoubleTapLogListener = doubleTapLogListener;
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        return onTouchEvent(motionEvent, Integer.MAX_VALUE);
    }

    public boolean onTouchEvent(MotionEvent motionEvent, int i) {
        switch (motionEvent.getActionMasked()) {
            case 0:
                this.mDownX = motionEvent.getX();
                this.mDownY = motionEvent.getY();
                this.mTrackTouch = true;
                if (this.mDownY > i) {
                    this.mTrackTouch = false;
                }
                break;
            case 1:
                if (isWithinTouchSlop(motionEvent)) {
                    if (this.mSlideBackListener != null && this.mSlideBackListener.onSlideBack()) {
                        return true;
                    }
                    if (!this.mActivated) {
                        makeActive();
                        this.mView.postDelayed(this.mTapTimeoutRunnable, 1200L);
                        this.mActivationX = motionEvent.getX();
                        this.mActivationY = motionEvent.getY();
                    } else {
                        boolean zIsWithinDoubleTapSlop = isWithinDoubleTapSlop(motionEvent);
                        if (this.mDoubleTapLogListener != null) {
                            this.mDoubleTapLogListener.onDoubleTapLog(zIsWithinDoubleTapSlop, motionEvent.getX() - this.mActivationX, motionEvent.getY() - this.mActivationY);
                        }
                        if (zIsWithinDoubleTapSlop) {
                            if (!this.mDoubleTapListener.onDoubleTap()) {
                                return false;
                            }
                        } else {
                            makeInactive();
                            this.mTrackTouch = false;
                        }
                    }
                } else {
                    makeInactive();
                    this.mTrackTouch = false;
                }
                break;
            case 2:
                if (!isWithinTouchSlop(motionEvent)) {
                    makeInactive();
                    this.mTrackTouch = false;
                }
                break;
            case 3:
                makeInactive();
                this.mTrackTouch = false;
                break;
        }
        return this.mTrackTouch;
    }

    private void makeActive() {
        if (!this.mActivated) {
            this.mActivated = true;
            this.mActivationListener.onActiveChanged(true);
        }
    }

    private void makeInactive() {
        if (this.mActivated) {
            this.mActivated = false;
            this.mActivationListener.onActiveChanged(false);
        }
    }

    private boolean isWithinTouchSlop(MotionEvent motionEvent) {
        return Math.abs(motionEvent.getX() - this.mDownX) < this.mTouchSlop && Math.abs(motionEvent.getY() - this.mDownY) < this.mTouchSlop;
    }

    public boolean isWithinDoubleTapSlop(MotionEvent motionEvent) {
        if (this.mActivated) {
            return Math.abs(motionEvent.getX() - this.mActivationX) < this.mDoubleTapSlop && Math.abs(motionEvent.getY() - this.mActivationY) < this.mDoubleTapSlop;
        }
        return true;
    }
}
