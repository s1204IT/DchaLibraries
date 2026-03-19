package android.support.v7.widget;

import android.R;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;

class FastScroller extends RecyclerView.ItemDecoration implements RecyclerView.OnItemTouchListener {
    float mHorizontalDragX;
    int mHorizontalThumbCenterX;
    private final StateListDrawable mHorizontalThumbDrawable;
    private final int mHorizontalThumbHeight;
    int mHorizontalThumbWidth;
    private final Drawable mHorizontalTrackDrawable;
    private final int mHorizontalTrackHeight;
    private final int mMargin;
    private RecyclerView mRecyclerView;
    private final int mScrollbarMinimumRange;
    float mVerticalDragY;
    int mVerticalThumbCenterY;
    private final StateListDrawable mVerticalThumbDrawable;
    int mVerticalThumbHeight;
    private final int mVerticalThumbWidth;
    private final Drawable mVerticalTrackDrawable;
    private final int mVerticalTrackWidth;
    private static final int[] PRESSED_STATE_SET = {R.attr.state_pressed};
    private static final int[] EMPTY_STATE_SET = new int[0];
    private int mRecyclerViewWidth = 0;
    private int mRecyclerViewHeight = 0;
    private boolean mNeedVerticalScrollbar = false;
    private boolean mNeedHorizontalScrollbar = false;
    private int mState = 0;
    private int mDragState = 0;
    private final int[] mVerticalRange = new int[2];
    private final int[] mHorizontalRange = new int[2];
    private final ValueAnimator mShowHideAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
    private int mAnimationState = 0;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            FastScroller.this.hide(500);
        }
    };
    private final RecyclerView.OnScrollListener mOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            FastScroller.this.updateScrollPosition(recyclerView.computeHorizontalScrollOffset(), recyclerView.computeVerticalScrollOffset());
        }
    };

    FastScroller(RecyclerView recyclerView, StateListDrawable verticalThumbDrawable, Drawable verticalTrackDrawable, StateListDrawable horizontalThumbDrawable, Drawable horizontalTrackDrawable, int defaultWidth, int scrollbarMinimumRange, int margin) {
        this.mVerticalThumbDrawable = verticalThumbDrawable;
        this.mVerticalTrackDrawable = verticalTrackDrawable;
        this.mHorizontalThumbDrawable = horizontalThumbDrawable;
        this.mHorizontalTrackDrawable = horizontalTrackDrawable;
        this.mVerticalThumbWidth = Math.max(defaultWidth, verticalThumbDrawable.getIntrinsicWidth());
        this.mVerticalTrackWidth = Math.max(defaultWidth, verticalTrackDrawable.getIntrinsicWidth());
        this.mHorizontalThumbHeight = Math.max(defaultWidth, horizontalThumbDrawable.getIntrinsicWidth());
        this.mHorizontalTrackHeight = Math.max(defaultWidth, horizontalTrackDrawable.getIntrinsicWidth());
        this.mScrollbarMinimumRange = scrollbarMinimumRange;
        this.mMargin = margin;
        this.mVerticalThumbDrawable.setAlpha(255);
        this.mVerticalTrackDrawable.setAlpha(255);
        this.mShowHideAnimator.addListener(new AnimatorListener());
        this.mShowHideAnimator.addUpdateListener(new AnimatorUpdater());
        attachToRecyclerView(recyclerView);
    }

    public void attachToRecyclerView(RecyclerView recyclerView) {
        if (this.mRecyclerView == recyclerView) {
            return;
        }
        if (this.mRecyclerView != null) {
            destroyCallbacks();
        }
        this.mRecyclerView = recyclerView;
        if (this.mRecyclerView != null) {
            setupCallbacks();
        }
    }

    private void setupCallbacks() {
        this.mRecyclerView.addItemDecoration(this);
        this.mRecyclerView.addOnItemTouchListener(this);
        this.mRecyclerView.addOnScrollListener(this.mOnScrollListener);
    }

    private void destroyCallbacks() {
        this.mRecyclerView.removeItemDecoration(this);
        this.mRecyclerView.removeOnItemTouchListener(this);
        this.mRecyclerView.removeOnScrollListener(this.mOnScrollListener);
        cancelHide();
    }

    private void requestRedraw() {
        this.mRecyclerView.invalidate();
    }

    private void setState(int state) {
        if (state == 2 && this.mState != 2) {
            this.mVerticalThumbDrawable.setState(PRESSED_STATE_SET);
            cancelHide();
        }
        if (state == 0) {
            requestRedraw();
        } else {
            show();
        }
        if (this.mState == 2 && state != 2) {
            this.mVerticalThumbDrawable.setState(EMPTY_STATE_SET);
            resetHideDelay(1200);
        } else if (state == 1) {
            resetHideDelay(1500);
        }
        this.mState = state;
    }

    private boolean isLayoutRTL() {
        return ViewCompat.getLayoutDirection(this.mRecyclerView) == 1;
    }

    boolean isVisible() {
        return this.mState == 1;
    }

    boolean isHidden() {
        return this.mState == 0;
    }

    public void show() {
        int i = this.mAnimationState;
        if (i != 0) {
            if (i == 3) {
                this.mShowHideAnimator.cancel();
            } else {
                return;
            }
        }
        this.mAnimationState = 1;
        this.mShowHideAnimator.setFloatValues(((Float) this.mShowHideAnimator.getAnimatedValue()).floatValue(), 1.0f);
        this.mShowHideAnimator.setDuration(500L);
        this.mShowHideAnimator.setStartDelay(0L);
        this.mShowHideAnimator.start();
    }

    void hide(int duration) {
        switch (this.mAnimationState) {
            case DialogFragment.STYLE_NO_TITLE:
                this.mShowHideAnimator.cancel();
                break;
            case DialogFragment.STYLE_NO_FRAME:
                break;
            default:
                return;
        }
        this.mAnimationState = 3;
        this.mShowHideAnimator.setFloatValues(((Float) this.mShowHideAnimator.getAnimatedValue()).floatValue(), 0.0f);
        this.mShowHideAnimator.setDuration(duration);
        this.mShowHideAnimator.start();
    }

    private void cancelHide() {
        this.mRecyclerView.removeCallbacks(this.mHideRunnable);
    }

    private void resetHideDelay(int delay) {
        cancelHide();
        this.mRecyclerView.postDelayed(this.mHideRunnable, delay);
    }

    @Override
    public void onDrawOver(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        if (this.mRecyclerViewWidth != this.mRecyclerView.getWidth() || this.mRecyclerViewHeight != this.mRecyclerView.getHeight()) {
            this.mRecyclerViewWidth = this.mRecyclerView.getWidth();
            this.mRecyclerViewHeight = this.mRecyclerView.getHeight();
            setState(0);
        } else if (this.mAnimationState != 0) {
            if (this.mNeedVerticalScrollbar) {
                drawVerticalScrollbar(canvas);
            }
            if (this.mNeedHorizontalScrollbar) {
                drawHorizontalScrollbar(canvas);
            }
        }
    }

    private void drawVerticalScrollbar(Canvas canvas) {
        int viewWidth = this.mRecyclerViewWidth;
        int left = viewWidth - this.mVerticalThumbWidth;
        int top = this.mVerticalThumbCenterY - (this.mVerticalThumbHeight / 2);
        this.mVerticalThumbDrawable.setBounds(0, 0, this.mVerticalThumbWidth, this.mVerticalThumbHeight);
        this.mVerticalTrackDrawable.setBounds(0, 0, this.mVerticalTrackWidth, this.mRecyclerViewHeight);
        if (isLayoutRTL()) {
            this.mVerticalTrackDrawable.draw(canvas);
            canvas.translate(this.mVerticalThumbWidth, top);
            canvas.scale(-1.0f, 1.0f);
            this.mVerticalThumbDrawable.draw(canvas);
            canvas.scale(1.0f, 1.0f);
            canvas.translate(-this.mVerticalThumbWidth, -top);
            return;
        }
        canvas.translate(left, 0.0f);
        this.mVerticalTrackDrawable.draw(canvas);
        canvas.translate(0.0f, top);
        this.mVerticalThumbDrawable.draw(canvas);
        canvas.translate(-left, -top);
    }

    private void drawHorizontalScrollbar(Canvas canvas) {
        int viewHeight = this.mRecyclerViewHeight;
        int top = viewHeight - this.mHorizontalThumbHeight;
        int left = this.mHorizontalThumbCenterX - (this.mHorizontalThumbWidth / 2);
        this.mHorizontalThumbDrawable.setBounds(0, 0, this.mHorizontalThumbWidth, this.mHorizontalThumbHeight);
        this.mHorizontalTrackDrawable.setBounds(0, 0, this.mRecyclerViewWidth, this.mHorizontalTrackHeight);
        canvas.translate(0.0f, top);
        this.mHorizontalTrackDrawable.draw(canvas);
        canvas.translate(left, 0.0f);
        this.mHorizontalThumbDrawable.draw(canvas);
        canvas.translate(-left, -top);
    }

    void updateScrollPosition(int offsetX, int offsetY) {
        int verticalContentLength = this.mRecyclerView.computeVerticalScrollRange();
        int verticalVisibleLength = this.mRecyclerViewHeight;
        this.mNeedVerticalScrollbar = verticalContentLength - verticalVisibleLength > 0 && this.mRecyclerViewHeight >= this.mScrollbarMinimumRange;
        int horizontalContentLength = this.mRecyclerView.computeHorizontalScrollRange();
        int horizontalVisibleLength = this.mRecyclerViewWidth;
        this.mNeedHorizontalScrollbar = horizontalContentLength - horizontalVisibleLength > 0 && this.mRecyclerViewWidth >= this.mScrollbarMinimumRange;
        if (!this.mNeedVerticalScrollbar && !this.mNeedHorizontalScrollbar) {
            if (this.mState != 0) {
                setState(0);
                return;
            }
            return;
        }
        if (this.mNeedVerticalScrollbar) {
            float middleScreenPos = offsetY + (verticalVisibleLength / 2.0f);
            this.mVerticalThumbCenterY = (int) ((verticalVisibleLength * middleScreenPos) / verticalContentLength);
            this.mVerticalThumbHeight = Math.min(verticalVisibleLength, (verticalVisibleLength * verticalVisibleLength) / verticalContentLength);
        }
        if (this.mNeedHorizontalScrollbar) {
            float middleScreenPos2 = offsetX + (horizontalVisibleLength / 2.0f);
            this.mHorizontalThumbCenterX = (int) ((horizontalVisibleLength * middleScreenPos2) / horizontalContentLength);
            this.mHorizontalThumbWidth = Math.min(horizontalVisibleLength, (horizontalVisibleLength * horizontalVisibleLength) / horizontalContentLength);
        }
        if (this.mState == 0 || this.mState == 1) {
            setState(1);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent ev) {
        boolean handled = false;
        if (this.mState == 1) {
            boolean insideVerticalThumb = isPointInsideVerticalThumb(ev.getX(), ev.getY());
            boolean insideHorizontalThumb = isPointInsideHorizontalThumb(ev.getX(), ev.getY());
            if (ev.getAction() == 0 && (insideVerticalThumb || insideHorizontalThumb)) {
                if (insideHorizontalThumb) {
                    this.mDragState = 1;
                    this.mHorizontalDragX = (int) ev.getX();
                } else if (insideVerticalThumb) {
                    this.mDragState = 2;
                    this.mVerticalDragY = (int) ev.getY();
                }
                setState(2);
                handled = true;
            }
        } else if (this.mState == 2) {
            handled = true;
        }
        return handled;
    }

    @Override
    public void onTouchEvent(RecyclerView recyclerView, MotionEvent me) {
        if (this.mState == 0) {
            return;
        }
        if (me.getAction() == 0) {
            boolean insideVerticalThumb = isPointInsideVerticalThumb(me.getX(), me.getY());
            boolean insideHorizontalThumb = isPointInsideHorizontalThumb(me.getX(), me.getY());
            if (insideVerticalThumb || insideHorizontalThumb) {
                if (insideHorizontalThumb) {
                    this.mDragState = 1;
                    this.mHorizontalDragX = (int) me.getX();
                } else if (insideVerticalThumb) {
                    this.mDragState = 2;
                    this.mVerticalDragY = (int) me.getY();
                }
                setState(2);
                return;
            }
            return;
        }
        if (me.getAction() == 1 && this.mState == 2) {
            this.mVerticalDragY = 0.0f;
            this.mHorizontalDragX = 0.0f;
            setState(1);
            this.mDragState = 0;
            return;
        }
        if (me.getAction() == 2 && this.mState == 2) {
            show();
            if (this.mDragState == 1) {
                horizontalScrollTo(me.getX());
            }
            if (this.mDragState == 2) {
                verticalScrollTo(me.getY());
            }
        }
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }

    private void verticalScrollTo(float y) {
        int[] scrollbarRange = getVerticalRange();
        float y2 = Math.max(scrollbarRange[0], Math.min(scrollbarRange[1], y));
        if (Math.abs(this.mVerticalThumbCenterY - y2) < 2.0f) {
            return;
        }
        int scrollingBy = scrollTo(this.mVerticalDragY, y2, scrollbarRange, this.mRecyclerView.computeVerticalScrollRange(), this.mRecyclerView.computeVerticalScrollOffset(), this.mRecyclerViewHeight);
        if (scrollingBy != 0) {
            this.mRecyclerView.scrollBy(0, scrollingBy);
        }
        this.mVerticalDragY = y2;
    }

    private void horizontalScrollTo(float x) {
        int[] scrollbarRange = getHorizontalRange();
        float x2 = Math.max(scrollbarRange[0], Math.min(scrollbarRange[1], x));
        if (Math.abs(this.mHorizontalThumbCenterX - x2) < 2.0f) {
            return;
        }
        int scrollingBy = scrollTo(this.mHorizontalDragX, x2, scrollbarRange, this.mRecyclerView.computeHorizontalScrollRange(), this.mRecyclerView.computeHorizontalScrollOffset(), this.mRecyclerViewWidth);
        if (scrollingBy != 0) {
            this.mRecyclerView.scrollBy(scrollingBy, 0);
        }
        this.mHorizontalDragX = x2;
    }

    private int scrollTo(float oldDragPos, float newDragPos, int[] scrollbarRange, int scrollRange, int scrollOffset, int viewLength) {
        int scrollbarLength = scrollbarRange[1] - scrollbarRange[0];
        if (scrollbarLength == 0) {
            return 0;
        }
        float percentage = (newDragPos - oldDragPos) / scrollbarLength;
        int totalPossibleOffset = scrollRange - viewLength;
        int scrollingBy = (int) (totalPossibleOffset * percentage);
        int absoluteOffset = scrollOffset + scrollingBy;
        if (absoluteOffset >= totalPossibleOffset || absoluteOffset < 0) {
            return 0;
        }
        return scrollingBy;
    }

    boolean isPointInsideVerticalThumb(float x, float y) {
        if (!isLayoutRTL() ? x >= this.mRecyclerViewWidth - this.mVerticalThumbWidth : x <= this.mVerticalThumbWidth / 2) {
            if (y >= this.mVerticalThumbCenterY - (this.mVerticalThumbHeight / 2) && y <= this.mVerticalThumbCenterY + (this.mVerticalThumbHeight / 2)) {
                return true;
            }
        }
        return false;
    }

    boolean isPointInsideHorizontalThumb(float x, float y) {
        return y >= ((float) (this.mRecyclerViewHeight - this.mHorizontalThumbHeight)) && x >= ((float) (this.mHorizontalThumbCenterX - (this.mHorizontalThumbWidth / 2))) && x <= ((float) (this.mHorizontalThumbCenterX + (this.mHorizontalThumbWidth / 2)));
    }

    Drawable getHorizontalTrackDrawable() {
        return this.mHorizontalTrackDrawable;
    }

    Drawable getHorizontalThumbDrawable() {
        return this.mHorizontalThumbDrawable;
    }

    Drawable getVerticalTrackDrawable() {
        return this.mVerticalTrackDrawable;
    }

    Drawable getVerticalThumbDrawable() {
        return this.mVerticalThumbDrawable;
    }

    private int[] getVerticalRange() {
        this.mVerticalRange[0] = this.mMargin;
        this.mVerticalRange[1] = this.mRecyclerViewHeight - this.mMargin;
        return this.mVerticalRange;
    }

    private int[] getHorizontalRange() {
        this.mHorizontalRange[0] = this.mMargin;
        this.mHorizontalRange[1] = this.mRecyclerViewWidth - this.mMargin;
        return this.mHorizontalRange;
    }

    private class AnimatorListener extends AnimatorListenerAdapter {
        private boolean mCanceled;

        private AnimatorListener() {
            this.mCanceled = false;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (this.mCanceled) {
                this.mCanceled = false;
            } else if (((Float) FastScroller.this.mShowHideAnimator.getAnimatedValue()).floatValue() == 0.0f) {
                FastScroller.this.mAnimationState = 0;
                FastScroller.this.setState(0);
            } else {
                FastScroller.this.mAnimationState = 2;
                FastScroller.this.requestRedraw();
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            this.mCanceled = true;
        }
    }

    private class AnimatorUpdater implements ValueAnimator.AnimatorUpdateListener {
        private AnimatorUpdater() {
        }

        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            int alpha = (int) (255.0f * ((Float) valueAnimator.getAnimatedValue()).floatValue());
            FastScroller.this.mVerticalThumbDrawable.setAlpha(alpha);
            FastScroller.this.mVerticalTrackDrawable.setAlpha(alpha);
            FastScroller.this.requestRedraw();
        }
    }
}
