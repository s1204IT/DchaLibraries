package com.android.server.policy;

import android.os.IBinder;
import android.os.SystemClock;
import android.view.WindowManagerInternal;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;
import com.android.server.LocalServices;
import com.android.server.statusbar.StatusBarManagerInternal;

public class StatusBarController extends BarController {
    private static final long TRANSITION_DURATION = 120;
    private final WindowManagerInternal.AppTransitionListener mAppTransitionListener;

    public StatusBarController() {
        super("StatusBar", 67108864, 268435456, 1073741824, 1, 67108864, 8);
        this.mAppTransitionListener = new WindowManagerInternal.AppTransitionListener() {
            public void onAppTransitionPendingLocked() {
                StatusBarController.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        StatusBarManagerInternal statusbar = StatusBarController.this.getStatusBarInternal();
                        if (statusbar == null) {
                            return;
                        }
                        statusbar.appTransitionPending();
                    }
                });
            }

            public void onAppTransitionStartingLocked(IBinder openToken, IBinder closeToken, final Animation openAnimation, final Animation closeAnimation) {
                StatusBarController.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        StatusBarManagerInternal statusbar = StatusBarController.this.getStatusBarInternal();
                        if (statusbar == null) {
                            return;
                        }
                        long startTime = StatusBarController.calculateStatusBarTransitionStartTime(openAnimation, closeAnimation);
                        long duration = (closeAnimation == null && openAnimation == null) ? 0L : StatusBarController.TRANSITION_DURATION;
                        statusbar.appTransitionStarting(startTime, duration);
                    }
                });
            }

            public void onAppTransitionCancelledLocked() {
                StatusBarController.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        StatusBarManagerInternal statusbar = StatusBarController.this.getStatusBarInternal();
                        if (statusbar == null) {
                            return;
                        }
                        statusbar.appTransitionCancelled();
                    }
                });
            }

            public void onAppTransitionFinishedLocked(IBinder token) {
                StatusBarController.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        StatusBarManagerInternal statusbar = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
                        if (statusbar == null) {
                            return;
                        }
                        statusbar.appTransitionFinished();
                    }
                });
            }
        };
    }

    @Override
    protected boolean skipAnimation() {
        return this.mWin.getAttrs().height == -1;
    }

    public WindowManagerInternal.AppTransitionListener getAppTransitionListener() {
        return this.mAppTransitionListener;
    }

    private static long calculateStatusBarTransitionStartTime(Animation openAnimation, Animation closeAnimation) {
        if (openAnimation == null || closeAnimation == null) {
            return SystemClock.uptimeMillis();
        }
        TranslateAnimation openTranslateAnimation = findTranslateAnimation(openAnimation);
        TranslateAnimation closeTranslateAnimation = findTranslateAnimation(closeAnimation);
        if (openTranslateAnimation == null) {
            return closeTranslateAnimation != null ? SystemClock.uptimeMillis() : SystemClock.uptimeMillis();
        }
        float t = findAlmostThereFraction(openTranslateAnimation.getInterpolator());
        return ((SystemClock.uptimeMillis() + openTranslateAnimation.getStartOffset()) + ((long) (openTranslateAnimation.getDuration() * t))) - TRANSITION_DURATION;
    }

    private static TranslateAnimation findTranslateAnimation(Animation animation) {
        if (animation instanceof TranslateAnimation) {
            return (TranslateAnimation) animation;
        }
        if (animation instanceof AnimationSet) {
            AnimationSet set = (AnimationSet) animation;
            for (int i = 0; i < set.getAnimations().size(); i++) {
                Animation a = set.getAnimations().get(i);
                if (a instanceof TranslateAnimation) {
                    return (TranslateAnimation) a;
                }
            }
            return null;
        }
        return null;
    }

    private static float findAlmostThereFraction(Interpolator interpolator) {
        float val = 0.5f;
        for (float adj = 0.25f; adj >= 0.01f; adj /= 2.0f) {
            if (interpolator.getInterpolation(val) < 0.99f) {
                val += adj;
            } else {
                val -= adj;
            }
        }
        return val;
    }
}
