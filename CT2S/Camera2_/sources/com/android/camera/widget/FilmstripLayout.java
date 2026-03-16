package com.android.camera.widget;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import com.android.camera.filmstrip.FilmstripContentPanel;
import com.android.camera.filmstrip.FilmstripController;
import com.android.camera.ui.FilmstripGestureRecognizer;
import com.android.camera2.R;

public class FilmstripLayout extends FrameLayout implements FilmstripContentPanel {
    private static final long DEFAULT_DURATION_MS = 200;
    private MyBackgroundDrawable mBackgroundDrawable;
    private final ValueAnimator mFilmstripAnimator;
    private Animator.AnimatorListener mFilmstripAnimatorListener;
    private ValueAnimator.AnimatorUpdateListener mFilmstripAnimatorUpdateListener;
    private FrameLayout mFilmstripContentLayout;
    private float mFilmstripContentTranslationProgress;
    private FilmstripGestureRecognizer.Listener mFilmstripGestureListener;
    private FilmstripView mFilmstripView;
    private FilmstripGestureRecognizer mGestureRecognizer;
    private Handler mHandler;
    private FilmstripContentPanel.Listener mListener;
    private int mSwipeTrend;

    public FilmstripLayout(Context context) {
        super(context);
        this.mFilmstripAnimator = ValueAnimator.ofFloat(null);
        this.mFilmstripAnimatorListener = new Animator.AnimatorListener() {
            private boolean mCanceled;

            @Override
            public void onAnimationStart(Animator animator) {
                this.mCanceled = false;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if (this.mCanceled) {
                    return;
                }
                if (FilmstripLayout.this.mFilmstripContentTranslationProgress != 0.0f) {
                    FilmstripLayout.this.mFilmstripView.getController().goToFilmstrip();
                    FilmstripLayout.this.setVisibility(4);
                } else {
                    FilmstripLayout.this.notifyShown();
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                this.mCanceled = true;
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        };
        this.mFilmstripAnimatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                FilmstripLayout.this.translateContentLayout(((Float) valueAnimator.getAnimatedValue()).floatValue());
                FilmstripLayout.this.mBackgroundDrawable.invalidateSelf();
            }
        };
        init(context);
    }

    public FilmstripLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mFilmstripAnimator = ValueAnimator.ofFloat(null);
        this.mFilmstripAnimatorListener = new Animator.AnimatorListener() {
            private boolean mCanceled;

            @Override
            public void onAnimationStart(Animator animator) {
                this.mCanceled = false;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if (this.mCanceled) {
                    return;
                }
                if (FilmstripLayout.this.mFilmstripContentTranslationProgress != 0.0f) {
                    FilmstripLayout.this.mFilmstripView.getController().goToFilmstrip();
                    FilmstripLayout.this.setVisibility(4);
                } else {
                    FilmstripLayout.this.notifyShown();
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                this.mCanceled = true;
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        };
        this.mFilmstripAnimatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                FilmstripLayout.this.translateContentLayout(((Float) valueAnimator.getAnimatedValue()).floatValue());
                FilmstripLayout.this.mBackgroundDrawable.invalidateSelf();
            }
        };
        init(context);
    }

    public FilmstripLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mFilmstripAnimator = ValueAnimator.ofFloat(null);
        this.mFilmstripAnimatorListener = new Animator.AnimatorListener() {
            private boolean mCanceled;

            @Override
            public void onAnimationStart(Animator animator) {
                this.mCanceled = false;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if (this.mCanceled) {
                    return;
                }
                if (FilmstripLayout.this.mFilmstripContentTranslationProgress != 0.0f) {
                    FilmstripLayout.this.mFilmstripView.getController().goToFilmstrip();
                    FilmstripLayout.this.setVisibility(4);
                } else {
                    FilmstripLayout.this.notifyShown();
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                this.mCanceled = true;
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        };
        this.mFilmstripAnimatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                FilmstripLayout.this.translateContentLayout(((Float) valueAnimator.getAnimatedValue()).floatValue());
                FilmstripLayout.this.mBackgroundDrawable.invalidateSelf();
            }
        };
        init(context);
    }

    private void init(Context context) {
        this.mGestureRecognizer = new FilmstripGestureRecognizer(context, new MyGestureListener());
        this.mFilmstripAnimator.setDuration(DEFAULT_DURATION_MS);
        this.mFilmstripAnimator.addUpdateListener(this.mFilmstripAnimatorUpdateListener);
        this.mFilmstripAnimator.addListener(this.mFilmstripAnimatorListener);
        this.mHandler = new Handler(Looper.getMainLooper());
        this.mBackgroundDrawable = new MyBackgroundDrawable();
        this.mBackgroundDrawable.setCallback(new Drawable.Callback() {
            @Override
            public void invalidateDrawable(Drawable drawable) {
                FilmstripLayout.this.invalidate();
            }

            @Override
            public void scheduleDrawable(Drawable drawable, Runnable runnable, long l) {
                FilmstripLayout.this.mHandler.postAtTime(runnable, drawable, l);
            }

            @Override
            public void unscheduleDrawable(Drawable drawable, Runnable runnable) {
                FilmstripLayout.this.mHandler.removeCallbacks(runnable, drawable);
            }
        });
        setBackground(this.mBackgroundDrawable);
    }

    @Override
    public void setFilmstripListener(FilmstripContentPanel.Listener listener) {
        this.mListener = listener;
        if (getVisibility() == 0 && this.mFilmstripContentTranslationProgress == 0.0f) {
            notifyShown();
        } else if (getVisibility() != 0) {
            notifyHidden();
        }
        this.mFilmstripView.getController().setListener(listener);
    }

    @Override
    public void hide() {
        translateContentLayout(1.0f);
        this.mFilmstripAnimatorListener.onAnimationEnd(this.mFilmstripAnimator);
    }

    @Override
    public void show() {
        translateContentLayout(0.0f);
        this.mFilmstripAnimatorListener.onAnimationEnd(this.mFilmstripAnimator);
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility != 0) {
            notifyHidden();
        }
    }

    private void notifyHidden() {
        if (this.mListener != null) {
            this.mListener.onFilmstripHidden();
        }
    }

    private void notifyShown() {
        if (this.mListener != null) {
            this.mListener.onFilmstripShown();
            this.mFilmstripView.zoomAtIndexChanged();
            FilmstripController controller = this.mFilmstripView.getController();
            int currentId = controller.getCurrentId();
            if (controller.inFilmstrip()) {
                this.mListener.onEnterFilmstrip(currentId);
            } else if (controller.inFullScreen()) {
                this.mListener.onEnterFullScreenUiShown(currentId);
            }
        }
    }

    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed && this.mFilmstripView != null && getVisibility() == 4) {
            hide();
        } else {
            translateContentLayout(this.mFilmstripContentTranslationProgress);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return this.mGestureRecognizer.onTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == 0) {
            getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        }
        return false;
    }

    @Override
    public void onFinishInflate() {
        this.mFilmstripView = (FilmstripView) findViewById(R.id.filmstrip_view);
        this.mFilmstripView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                motionEvent.setLocation(motionEvent.getX() + FilmstripLayout.this.mFilmstripContentLayout.getX(), motionEvent.getY() + FilmstripLayout.this.mFilmstripContentLayout.getY());
                FilmstripLayout.this.mGestureRecognizer.onTouchEvent(motionEvent);
                return true;
            }
        });
        this.mFilmstripGestureListener = this.mFilmstripView.getGestureListener();
        this.mFilmstripContentLayout = (FrameLayout) findViewById(R.id.camera_filmstrip_content_layout);
    }

    @Override
    public boolean onBackPressed() {
        return animateHide();
    }

    @Override
    public boolean animateHide() {
        if (getVisibility() != 0) {
            return false;
        }
        if (!this.mFilmstripAnimator.isRunning()) {
            hideFilmstrip();
        }
        return true;
    }

    public void hideFilmstrip() {
        onSwipeOutBegin();
        runAnimation(this.mFilmstripContentTranslationProgress, 1.0f);
    }

    public void showFilmstrip() {
        setVisibility(0);
        runAnimation(this.mFilmstripContentTranslationProgress, 0.0f);
    }

    private void runAnimation(float begin, float end) {
        if (this.mFilmstripAnimator.isRunning()) {
            return;
        }
        if (begin == end) {
            this.mFilmstripAnimatorListener.onAnimationEnd(this.mFilmstripAnimator);
        } else {
            this.mFilmstripAnimator.setFloatValues(begin, end);
            this.mFilmstripAnimator.start();
        }
    }

    private void translateContentLayout(float fraction) {
        this.mFilmstripContentTranslationProgress = fraction;
        this.mFilmstripContentLayout.setTranslationX(getMeasuredWidth() * fraction);
    }

    private void translateContentLayoutByPixel(float pixel) {
        this.mFilmstripContentLayout.setTranslationX(pixel);
        this.mFilmstripContentTranslationProgress = pixel / getMeasuredWidth();
    }

    private void onSwipeOut() {
        if (this.mListener != null) {
            this.mListener.onSwipeOut();
        }
    }

    private void onSwipeOutBegin() {
        if (this.mListener != null) {
            this.mListener.onSwipeOutBegin();
        }
    }

    private class MyGestureListener implements FilmstripGestureRecognizer.Listener {
        private MyGestureListener() {
        }

        @Override
        public boolean onScroll(float x, float y, float dx, float dy) {
            if (FilmstripLayout.this.mFilmstripView.getController().getCurrentId() != -1 && !FilmstripLayout.this.mFilmstripAnimator.isRunning() && (FilmstripLayout.this.mFilmstripContentLayout.getTranslationX() != 0.0f || !FilmstripLayout.this.mFilmstripGestureListener.onScroll(x, y, dx, dy))) {
                FilmstripLayout.this.mSwipeTrend = (((int) dx) >> 1) + (FilmstripLayout.this.mSwipeTrend >> 1);
                if (dx < 0.0f && FilmstripLayout.this.mFilmstripContentLayout.getTranslationX() == 0.0f) {
                    FilmstripLayout.this.mBackgroundDrawable.setOffset(0);
                    FilmstripLayout.this.onSwipeOutBegin();
                }
                if (dx > 0.0f && FilmstripLayout.this.mFilmstripContentLayout.getTranslationX() == FilmstripLayout.this.getMeasuredWidth()) {
                    int currentItemLeft = FilmstripLayout.this.mFilmstripView.getCurrentItemLeft();
                    dx = currentItemLeft;
                    FilmstripLayout.this.mBackgroundDrawable.setOffset(currentItemLeft);
                }
                float translate = FilmstripLayout.this.mFilmstripContentLayout.getTranslationX() - dx;
                if (translate < 0.0f) {
                    translate = 0.0f;
                } else if (translate > FilmstripLayout.this.getMeasuredWidth()) {
                    translate = FilmstripLayout.this.getMeasuredWidth();
                }
                FilmstripLayout.this.translateContentLayoutByPixel(translate);
                if (translate == 0.0f && dx > 0.0f) {
                    FilmstripLayout.this.mFilmstripAnimatorListener.onAnimationEnd(FilmstripLayout.this.mFilmstripAnimator);
                }
                FilmstripLayout.this.mBackgroundDrawable.invalidateSelf();
            }
            return true;
        }

        @Override
        public boolean onSingleTapUp(float x, float y) {
            if (FilmstripLayout.this.mFilmstripContentTranslationProgress == 0.0f) {
                return FilmstripLayout.this.mFilmstripGestureListener.onSingleTapUp(x, y);
            }
            return false;
        }

        @Override
        public boolean onDoubleTap(float x, float y) {
            if (FilmstripLayout.this.mFilmstripContentTranslationProgress == 0.0f) {
                return FilmstripLayout.this.mFilmstripGestureListener.onDoubleTap(x, y);
            }
            return false;
        }

        @Override
        public boolean onFling(float velocityX, float velocityY) {
            if (FilmstripLayout.this.mFilmstripContentTranslationProgress == 0.0f) {
                return FilmstripLayout.this.mFilmstripGestureListener.onFling(velocityX, velocityY);
            }
            return false;
        }

        @Override
        public boolean onScaleBegin(float focusX, float focusY) {
            if (FilmstripLayout.this.mFilmstripContentTranslationProgress == 0.0f) {
                return FilmstripLayout.this.mFilmstripGestureListener.onScaleBegin(focusX, focusY);
            }
            return false;
        }

        @Override
        public boolean onScale(float focusX, float focusY, float scale) {
            if (FilmstripLayout.this.mFilmstripContentTranslationProgress == 0.0f) {
                return FilmstripLayout.this.mFilmstripGestureListener.onScale(focusX, focusY, scale);
            }
            return false;
        }

        @Override
        public boolean onDown(float x, float y) {
            if (FilmstripLayout.this.mFilmstripContentLayout.getTranslationX() == 0.0f) {
                return FilmstripLayout.this.mFilmstripGestureListener.onDown(x, y);
            }
            return false;
        }

        @Override
        public boolean onUp(float x, float y) {
            if (FilmstripLayout.this.mFilmstripContentLayout.getTranslationX() == 0.0f) {
                return FilmstripLayout.this.mFilmstripGestureListener.onUp(x, y);
            }
            if (FilmstripLayout.this.mSwipeTrend >= 0) {
                if (FilmstripLayout.this.mSwipeTrend <= 0 && FilmstripLayout.this.mFilmstripContentLayout.getTranslationX() >= FilmstripLayout.this.getMeasuredWidth() / 2) {
                    FilmstripLayout.this.hideFilmstrip();
                    FilmstripLayout.this.onSwipeOut();
                } else {
                    FilmstripLayout.this.showFilmstrip();
                }
            } else {
                FilmstripLayout.this.hideFilmstrip();
                FilmstripLayout.this.onSwipeOut();
            }
            FilmstripLayout.this.mSwipeTrend = 0;
            return false;
        }

        @Override
        public void onLongPress(float x, float y) {
            FilmstripLayout.this.mFilmstripGestureListener.onLongPress(x, y);
        }

        @Override
        public void onScaleEnd() {
            if (FilmstripLayout.this.mFilmstripContentLayout.getTranslationX() == 0.0f) {
                FilmstripLayout.this.mFilmstripGestureListener.onScaleEnd();
            }
        }
    }

    private class MyBackgroundDrawable extends Drawable {
        private int mOffset;
        private Paint mPaint = new Paint();

        public MyBackgroundDrawable() {
            this.mPaint.setAntiAlias(true);
            this.mPaint.setColor(FilmstripLayout.this.getResources().getColor(R.color.filmstrip_background));
            this.mPaint.setAlpha(MotionEventCompat.ACTION_MASK);
        }

        public void setOffset(int offset) {
            this.mOffset = offset;
        }

        @Override
        public void setAlpha(int i) {
            this.mPaint.setAlpha(i);
        }

        private void setAlpha(float a) {
            setAlpha((int) (255.0f * a));
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            this.mPaint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return -3;
        }

        @Override
        public void draw(Canvas canvas) {
            int width = FilmstripLayout.this.getMeasuredWidth() - this.mOffset;
            float translation = FilmstripLayout.this.mFilmstripContentLayout.getTranslationX() - this.mOffset;
            if (translation != width) {
                setAlpha(1.0f - FilmstripLayout.this.mFilmstripContentTranslationProgress);
                canvas.drawRect(0.0f, 0.0f, FilmstripLayout.this.getMeasuredWidth(), FilmstripLayout.this.getMeasuredHeight(), this.mPaint);
            }
        }
    }
}
