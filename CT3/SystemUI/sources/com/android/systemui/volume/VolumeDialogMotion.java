package com.android.systemui.volume;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;

public class VolumeDialogMotion {
    private static final String TAG = Util.logTag(VolumeDialogMotion.class);
    private boolean mAnimating;
    private final Callback mCallback;
    private final View mChevron;
    private ValueAnimator mChevronPositionAnimator;
    private final ViewGroup mContents;
    private ValueAnimator mContentsPositionAnimator;
    private final Dialog mDialog;
    private final View mDialogView;
    private boolean mDismissing;
    private final Handler mHandler = new Handler();
    private boolean mShowing;

    public interface Callback {
        void onAnimatingChanged(boolean z);
    }

    public VolumeDialogMotion(Dialog dialog, View dialogView, ViewGroup contents, View chevron, Callback callback) {
        this.mDialog = dialog;
        this.mDialogView = dialogView;
        this.mContents = contents;
        this.mChevron = chevron;
        this.mCallback = callback;
        this.mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog2) {
                if (D.BUG) {
                    Log.d(VolumeDialogMotion.TAG, "mDialog.onDismiss");
                }
            }
        });
        this.mDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog2) {
                if (D.BUG) {
                    Log.d(VolumeDialogMotion.TAG, "mDialog.onShow");
                }
                int h = VolumeDialogMotion.this.mDialogView.getHeight();
                VolumeDialogMotion.this.mDialogView.setTranslationY(-h);
                VolumeDialogMotion.this.startShowAnimation();
            }
        });
    }

    public boolean isAnimating() {
        return this.mAnimating;
    }

    public void setShowing(boolean showing) {
        if (showing == this.mShowing) {
            return;
        }
        this.mShowing = showing;
        if (D.BUG) {
            Log.d(TAG, "mShowing = " + this.mShowing);
        }
        updateAnimating();
    }

    public void setDismissing(boolean dismissing) {
        if (dismissing == this.mDismissing) {
            return;
        }
        this.mDismissing = dismissing;
        if (D.BUG) {
            Log.d(TAG, "mDismissing = " + this.mDismissing);
        }
        updateAnimating();
    }

    private void updateAnimating() {
        boolean animating = !this.mShowing ? this.mDismissing : true;
        if (animating == this.mAnimating) {
            return;
        }
        this.mAnimating = animating;
        if (D.BUG) {
            Log.d(TAG, "mAnimating = " + this.mAnimating);
        }
        if (this.mCallback == null) {
            return;
        }
        this.mCallback.onAnimatingChanged(this.mAnimating);
    }

    public void startShow() {
        if (D.BUG) {
            Log.d(TAG, "startShow");
        }
        if (this.mShowing) {
            return;
        }
        setShowing(true);
        if (this.mDismissing) {
            this.mDialogView.animate().cancel();
            setDismissing(false);
            startShowAnimation();
        } else {
            if (D.BUG) {
                Log.d(TAG, "mDialog.show()");
            }
            this.mDialog.show();
        }
    }

    private int chevronDistance() {
        return this.mChevron.getHeight() / 6;
    }

    public int chevronPosY() {
        Object tag = this.mChevron != null ? this.mChevron.getTag() : null;
        if (tag == null) {
            return 0;
        }
        return ((Integer) tag).intValue();
    }

    public void startShowAnimation() {
        LogDecelerateInterpolator logDecelerateInterpolator = null;
        if (D.BUG) {
            Log.d(TAG, "startShowAnimation");
        }
        this.mDialogView.animate().translationY(0.0f).setDuration(scaledDuration(300)).setInterpolator(new LogDecelerateInterpolator(logDecelerateInterpolator)).setListener(null).setUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (VolumeDialogMotion.this.mChevronPositionAnimator == null) {
                    return;
                }
                float v = ((Float) VolumeDialogMotion.this.mChevronPositionAnimator.getAnimatedValue()).floatValue();
                int posY = VolumeDialogMotion.this.chevronPosY();
                VolumeDialogMotion.this.mChevron.setTranslationY(posY + v + (-VolumeDialogMotion.this.mDialogView.getTranslationY()));
            }
        }).start();
        this.mContentsPositionAnimator = ValueAnimator.ofFloat(-chevronDistance(), 0.0f).setDuration(scaledDuration(400));
        this.mContentsPositionAnimator.addListener(new AnimatorListenerAdapter() {
            private boolean mCancelled;

            @Override
            public void onAnimationEnd(Animator animation) {
                if (this.mCancelled) {
                    return;
                }
                if (D.BUG) {
                    Log.d(VolumeDialogMotion.TAG, "show.onAnimationEnd");
                }
                VolumeDialogMotion.this.setShowing(false);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                if (D.BUG) {
                    Log.d(VolumeDialogMotion.TAG, "show.onAnimationCancel");
                }
                this.mCancelled = true;
            }
        });
        this.mContentsPositionAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float v = ((Float) animation.getAnimatedValue()).floatValue();
                VolumeDialogMotion.this.mContents.setTranslationY((-VolumeDialogMotion.this.mDialogView.getTranslationY()) + v);
            }
        });
        this.mContentsPositionAnimator.setInterpolator(new LogDecelerateInterpolator(logDecelerateInterpolator));
        this.mContentsPositionAnimator.start();
        this.mContents.setAlpha(0.0f);
        this.mContents.animate().alpha(1.0f).setDuration(scaledDuration(150)).setInterpolator(new PathInterpolator(0.0f, 0.0f, 0.2f, 1.0f)).start();
        this.mChevronPositionAnimator = ValueAnimator.ofFloat(-chevronDistance(), 0.0f).setDuration(scaledDuration(250));
        this.mChevronPositionAnimator.setInterpolator(new PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f));
        this.mChevronPositionAnimator.start();
        this.mChevron.setAlpha(0.0f);
        this.mChevron.animate().alpha(1.0f).setStartDelay(scaledDuration(50)).setDuration(scaledDuration(150)).setInterpolator(new PathInterpolator(0.4f, 0.0f, 1.0f, 1.0f)).start();
    }

    public void startDismiss(final Runnable onComplete) {
        LogAccelerateInterpolator logAccelerateInterpolator = null;
        if (D.BUG) {
            Log.d(TAG, "startDismiss");
        }
        if (this.mDismissing) {
            return;
        }
        setDismissing(true);
        if (this.mShowing) {
            this.mDialogView.animate().cancel();
            if (this.mContentsPositionAnimator != null) {
                this.mContentsPositionAnimator.cancel();
            }
            this.mContents.animate().cancel();
            if (this.mChevronPositionAnimator != null) {
                this.mChevronPositionAnimator.cancel();
            }
            this.mChevron.animate().cancel();
            setShowing(false);
        }
        this.mDialogView.animate().translationY(-this.mDialogView.getHeight()).setDuration(scaledDuration(250)).setInterpolator(new LogAccelerateInterpolator(logAccelerateInterpolator)).setUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                VolumeDialogMotion.this.mContents.setTranslationY(-VolumeDialogMotion.this.mDialogView.getTranslationY());
                int posY = VolumeDialogMotion.this.chevronPosY();
                VolumeDialogMotion.this.mChevron.setTranslationY(posY + (-VolumeDialogMotion.this.mDialogView.getTranslationY()));
            }
        }).setListener(new AnimatorListenerAdapter() {
            private boolean mCancelled;

            @Override
            public void onAnimationEnd(Animator animation) {
                if (this.mCancelled) {
                    return;
                }
                if (D.BUG) {
                    Log.d(VolumeDialogMotion.TAG, "dismiss.onAnimationEnd");
                }
                Handler handler = VolumeDialogMotion.this.mHandler;
                final Runnable runnable = onComplete;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (D.BUG) {
                            Log.d(VolumeDialogMotion.TAG, "mDialog.dismiss()");
                        }
                        VolumeDialogMotion.this.mDialog.dismiss();
                        runnable.run();
                        VolumeDialogMotion.this.setDismissing(false);
                    }
                }, 50L);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                if (D.BUG) {
                    Log.d(VolumeDialogMotion.TAG, "dismiss.onAnimationCancel");
                }
                this.mCancelled = true;
            }
        }).start();
    }

    private static int scaledDuration(int base) {
        return (int) (base * 1.0f);
    }

    private static final class LogDecelerateInterpolator implements TimeInterpolator {
        private final float mBase;
        private final float mDrift;
        private final float mOutputScale;
        private final float mTimeScale;

        LogDecelerateInterpolator(LogDecelerateInterpolator logDecelerateInterpolator) {
            this();
        }

        private LogDecelerateInterpolator() {
            this(400.0f, 1.4f, 0.0f);
        }

        private LogDecelerateInterpolator(float base, float timeScale, float drift) {
            this.mBase = base;
            this.mDrift = drift;
            this.mTimeScale = 1.0f / timeScale;
            this.mOutputScale = 1.0f / computeLog(1.0f);
        }

        private float computeLog(float t) {
            return (1.0f - ((float) Math.pow(this.mBase, (-t) * this.mTimeScale))) + (this.mDrift * t);
        }

        @Override
        public float getInterpolation(float t) {
            return computeLog(t) * this.mOutputScale;
        }
    }

    private static final class LogAccelerateInterpolator implements TimeInterpolator {
        private final int mBase;
        private final int mDrift;
        private final float mLogScale;

        LogAccelerateInterpolator(LogAccelerateInterpolator logAccelerateInterpolator) {
            this();
        }

        private LogAccelerateInterpolator() {
            this(100, 0);
        }

        private LogAccelerateInterpolator(int base, int drift) {
            this.mBase = base;
            this.mDrift = drift;
            this.mLogScale = 1.0f / computeLog(1.0f, this.mBase, this.mDrift);
        }

        private static float computeLog(float t, int base, int drift) {
            return ((float) (-Math.pow(base, -t))) + 1.0f + (drift * t);
        }

        @Override
        public float getInterpolation(float t) {
            return 1.0f - (computeLog(1.0f - t, this.mBase, this.mDrift) * this.mLogScale);
        }
    }
}
