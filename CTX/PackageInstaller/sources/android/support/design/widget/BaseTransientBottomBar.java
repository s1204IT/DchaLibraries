package android.support.design.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.animation.AnimationUtils;
import android.support.design.snackbar.ContentViewCallback;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.SnackbarManager;
import android.support.design.widget.SwipeDismissBehavior;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityManager;
import android.widget.FrameLayout;
import java.util.List;

public abstract class BaseTransientBottomBar<B extends BaseTransientBottomBar<B>> {
    private static final boolean USE_OFFSET_API;
    static final Handler handler;
    private final AccessibilityManager accessibilityManager;
    private List<BaseCallback<B>> callbacks;
    private final ContentViewCallback contentViewCallback;
    final SnackbarManager.Callback managerCallback;
    private final ViewGroup targetParent;
    final SnackbarBaseLayout view;

    interface OnAttachStateChangeListener {
        void onViewAttachedToWindow(View view);

        void onViewDetachedFromWindow(View view);
    }

    interface OnLayoutChangeListener {
        void onLayoutChange(View view, int i, int i2, int i3, int i4);
    }

    public static abstract class BaseCallback<B> {
        public void onDismissed(B transientBottomBar, int event) {
        }

        public void onShown(B transientBottomBar) {
        }
    }

    static {
        USE_OFFSET_API = Build.VERSION.SDK_INT >= 16 && Build.VERSION.SDK_INT <= 19;
        handler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                switch (message.what) {
                    case DialogFragment.STYLE_NORMAL:
                        ((BaseTransientBottomBar) message.obj).showView();
                        break;
                    case DialogFragment.STYLE_NO_TITLE:
                        ((BaseTransientBottomBar) message.obj).hideView(message.arg1);
                        break;
                }
                return true;
            }
        });
    }

    void dispatchDismiss(int event) {
        SnackbarManager.getInstance().dismiss(this.managerCallback, event);
    }

    public boolean isShownOrQueued() {
        return SnackbarManager.getInstance().isCurrentOrNext(this.managerCallback);
    }

    final void showView() {
        if (this.view.getParent() == null) {
            ViewGroup.LayoutParams lp = this.view.getLayoutParams();
            if (lp instanceof CoordinatorLayout.LayoutParams) {
                CoordinatorLayout.LayoutParams clp = (CoordinatorLayout.LayoutParams) lp;
                BaseTransientBottomBar<B>.Behavior behavior = new Behavior();
                behavior.setStartAlphaSwipeDistance(0.1f);
                behavior.setEndAlphaSwipeDistance(0.6f);
                behavior.setSwipeDirection(0);
                behavior.setListener(new SwipeDismissBehavior.OnDismissListener() {
                    @Override
                    public void onDismiss(View view) {
                        view.setVisibility(8);
                        BaseTransientBottomBar.this.dispatchDismiss(0);
                    }

                    @Override
                    public void onDragStateChanged(int state) {
                        switch (state) {
                            case DialogFragment.STYLE_NORMAL:
                                SnackbarManager.getInstance().restoreTimeoutIfPaused(BaseTransientBottomBar.this.managerCallback);
                                break;
                            case DialogFragment.STYLE_NO_TITLE:
                            case DialogFragment.STYLE_NO_FRAME:
                                SnackbarManager.getInstance().pauseTimeout(BaseTransientBottomBar.this.managerCallback);
                                break;
                        }
                    }
                });
                clp.setBehavior(behavior);
                clp.insetEdge = 80;
            }
            this.targetParent.addView(this.view);
        }
        this.view.setOnAttachStateChangeListener(new OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                if (BaseTransientBottomBar.this.isShownOrQueued()) {
                    BaseTransientBottomBar.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            BaseTransientBottomBar.this.onViewHidden(3);
                        }
                    });
                }
            }
        });
        if (ViewCompat.isLaidOut(this.view)) {
            if (shouldAnimate()) {
                animateViewIn();
                return;
            } else {
                onViewShown();
                return;
            }
        }
        this.view.setOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int left, int top, int right, int bottom) {
                BaseTransientBottomBar.this.view.setOnLayoutChangeListener(null);
                if (BaseTransientBottomBar.this.shouldAnimate()) {
                    BaseTransientBottomBar.this.animateViewIn();
                } else {
                    BaseTransientBottomBar.this.onViewShown();
                }
            }
        });
    }

    void animateViewIn() {
        final int viewHeight = this.view.getHeight();
        if (USE_OFFSET_API) {
            ViewCompat.offsetTopAndBottom(this.view, viewHeight);
        } else {
            this.view.setTranslationY(viewHeight);
        }
        ValueAnimator animator = new ValueAnimator();
        animator.setIntValues(viewHeight, 0);
        animator.setInterpolator(AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR);
        animator.setDuration(250L);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator2) {
                BaseTransientBottomBar.this.contentViewCallback.animateContentIn(70, 180);
            }

            @Override
            public void onAnimationEnd(Animator animator2) {
                BaseTransientBottomBar.this.onViewShown();
            }
        });
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            private int previousAnimatedIntValue;

            {
                this.previousAnimatedIntValue = viewHeight;
            }

            @Override
            public void onAnimationUpdate(ValueAnimator animator2) {
                int currentAnimatedIntValue = ((Integer) animator2.getAnimatedValue()).intValue();
                if (BaseTransientBottomBar.USE_OFFSET_API) {
                    ViewCompat.offsetTopAndBottom(BaseTransientBottomBar.this.view, currentAnimatedIntValue - this.previousAnimatedIntValue);
                } else {
                    BaseTransientBottomBar.this.view.setTranslationY(currentAnimatedIntValue);
                }
                this.previousAnimatedIntValue = currentAnimatedIntValue;
            }
        });
        animator.start();
    }

    private void animateViewOut(final int event) {
        ValueAnimator animator = new ValueAnimator();
        animator.setIntValues(0, this.view.getHeight());
        animator.setInterpolator(AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR);
        animator.setDuration(250L);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator2) {
                BaseTransientBottomBar.this.contentViewCallback.animateContentOut(0, 180);
            }

            @Override
            public void onAnimationEnd(Animator animator2) {
                BaseTransientBottomBar.this.onViewHidden(event);
            }
        });
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            private int previousAnimatedIntValue = 0;

            @Override
            public void onAnimationUpdate(ValueAnimator animator2) {
                int currentAnimatedIntValue = ((Integer) animator2.getAnimatedValue()).intValue();
                if (BaseTransientBottomBar.USE_OFFSET_API) {
                    ViewCompat.offsetTopAndBottom(BaseTransientBottomBar.this.view, currentAnimatedIntValue - this.previousAnimatedIntValue);
                } else {
                    BaseTransientBottomBar.this.view.setTranslationY(currentAnimatedIntValue);
                }
                this.previousAnimatedIntValue = currentAnimatedIntValue;
            }
        });
        animator.start();
    }

    final void hideView(int event) {
        if (shouldAnimate() && this.view.getVisibility() == 0) {
            animateViewOut(event);
        } else {
            onViewHidden(event);
        }
    }

    void onViewShown() {
        SnackbarManager.getInstance().onShown(this.managerCallback);
        if (this.callbacks != null) {
            int callbackCount = this.callbacks.size();
            for (int i = callbackCount - 1; i >= 0; i--) {
                this.callbacks.get(i).onShown(this);
            }
        }
    }

    void onViewHidden(int event) {
        SnackbarManager.getInstance().onDismissed(this.managerCallback);
        if (this.callbacks != null) {
            int callbackCount = this.callbacks.size();
            for (int i = callbackCount - 1; i >= 0; i--) {
                this.callbacks.get(i).onDismissed(this, event);
            }
        }
        ViewParent parent = this.view.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(this.view);
        }
    }

    boolean shouldAnimate() {
        return !this.accessibilityManager.isEnabled();
    }

    static class SnackbarBaseLayout extends FrameLayout {
        private OnAttachStateChangeListener onAttachStateChangeListener;
        private OnLayoutChangeListener onLayoutChangeListener;

        SnackbarBaseLayout(Context context) {
            this(context, null);
        }

        SnackbarBaseLayout(Context context, AttributeSet attrs) {
            super(context, attrs);
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SnackbarLayout);
            if (a.hasValue(R.styleable.SnackbarLayout_elevation)) {
                ViewCompat.setElevation(this, a.getDimensionPixelSize(R.styleable.SnackbarLayout_elevation, 0));
            }
            a.recycle();
            setClickable(true);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            super.onLayout(changed, l, t, r, b);
            if (this.onLayoutChangeListener != null) {
                this.onLayoutChangeListener.onLayoutChange(this, l, t, r, b);
            }
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            if (this.onAttachStateChangeListener != null) {
                this.onAttachStateChangeListener.onViewAttachedToWindow(this);
            }
            ViewCompat.requestApplyInsets(this);
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            if (this.onAttachStateChangeListener != null) {
                this.onAttachStateChangeListener.onViewDetachedFromWindow(this);
            }
        }

        void setOnLayoutChangeListener(OnLayoutChangeListener onLayoutChangeListener) {
            this.onLayoutChangeListener = onLayoutChangeListener;
        }

        void setOnAttachStateChangeListener(OnAttachStateChangeListener listener) {
            this.onAttachStateChangeListener = listener;
        }
    }

    final class Behavior extends SwipeDismissBehavior<SnackbarBaseLayout> {
        Behavior() {
        }

        @Override
        public boolean canSwipeDismissView(View child) {
            return child instanceof SnackbarBaseLayout;
        }

        @Override
        public boolean onInterceptTouchEvent(CoordinatorLayout parent, SnackbarBaseLayout child, MotionEvent event) {
            int actionMasked = event.getActionMasked();
            if (actionMasked != 3) {
                switch (actionMasked) {
                    case DialogFragment.STYLE_NORMAL:
                        if (parent.isPointInChildBounds(child, (int) event.getX(), (int) event.getY())) {
                            SnackbarManager.getInstance().pauseTimeout(BaseTransientBottomBar.this.managerCallback);
                        }
                        break;
                    case DialogFragment.STYLE_NO_TITLE:
                        SnackbarManager.getInstance().restoreTimeoutIfPaused(BaseTransientBottomBar.this.managerCallback);
                        break;
                }
            }
            return super.onInterceptTouchEvent(parent, child, event);
        }
    }
}
