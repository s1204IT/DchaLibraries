package android.widget;

import android.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;

@Deprecated
public class SlidingDrawer extends ViewGroup {
    private static final int ANIMATION_FRAME_DURATION = 16;
    private static final int COLLAPSED_FULL_CLOSED = -10002;
    private static final int EXPANDED_FULL_OPEN = -10001;
    private static final float MAXIMUM_ACCELERATION = 2000.0f;
    private static final float MAXIMUM_MAJOR_VELOCITY = 200.0f;
    private static final float MAXIMUM_MINOR_VELOCITY = 150.0f;
    private static final float MAXIMUM_TAP_VELOCITY = 100.0f;
    public static final int ORIENTATION_HORIZONTAL = 0;
    public static final int ORIENTATION_VERTICAL = 1;
    private static final int TAP_THRESHOLD = 6;
    private static final int VELOCITY_UNITS = 1000;
    private boolean mAllowSingleTap;
    private boolean mAnimateOnClick;
    private float mAnimatedAcceleration;
    private float mAnimatedVelocity;
    private boolean mAnimating;
    private long mAnimationLastTime;
    private float mAnimationPosition;
    private int mBottomOffset;
    private View mContent;
    private final int mContentId;
    private long mCurrentAnimationTime;
    private boolean mExpanded;
    private final Rect mFrame;
    private View mHandle;
    private int mHandleHeight;
    private final int mHandleId;
    private int mHandleWidth;
    private final Rect mInvalidate;
    private boolean mLocked;
    private final int mMaximumAcceleration;
    private final int mMaximumMajorVelocity;
    private final int mMaximumMinorVelocity;
    private final int mMaximumTapVelocity;
    private OnDrawerCloseListener mOnDrawerCloseListener;
    private OnDrawerOpenListener mOnDrawerOpenListener;
    private OnDrawerScrollListener mOnDrawerScrollListener;
    private final Runnable mSlidingRunnable;
    private final int mTapThreshold;
    private int mTopOffset;
    private int mTouchDelta;
    private boolean mTracking;
    private VelocityTracker mVelocityTracker;
    private final int mVelocityUnits;
    private boolean mVertical;

    public interface OnDrawerCloseListener {
        void onDrawerClosed();
    }

    public interface OnDrawerOpenListener {
        void onDrawerOpened();
    }

    public interface OnDrawerScrollListener {
        void onScrollEnded();

        void onScrollStarted();
    }

    public SlidingDrawer(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public SlidingDrawer(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public SlidingDrawer(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mFrame = new Rect();
        this.mInvalidate = new Rect();
        this.mSlidingRunnable = new Runnable() {
            @Override
            public void run() {
                SlidingDrawer.this.doAnimation();
            }
        };
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.SlidingDrawer, i, i2);
        this.mVertical = typedArrayObtainStyledAttributes.getInt(0, 1) == 1;
        this.mBottomOffset = (int) typedArrayObtainStyledAttributes.getDimension(1, 0.0f);
        this.mTopOffset = (int) typedArrayObtainStyledAttributes.getDimension(2, 0.0f);
        this.mAllowSingleTap = typedArrayObtainStyledAttributes.getBoolean(3, true);
        this.mAnimateOnClick = typedArrayObtainStyledAttributes.getBoolean(6, true);
        int resourceId = typedArrayObtainStyledAttributes.getResourceId(4, 0);
        if (resourceId != 0) {
            int resourceId2 = typedArrayObtainStyledAttributes.getResourceId(5, 0);
            if (resourceId2 == 0) {
                throw new IllegalArgumentException("The content attribute is required and must refer to a valid child.");
            }
            if (resourceId == resourceId2) {
                throw new IllegalArgumentException("The content and handle attributes must refer to different children.");
            }
            this.mHandleId = resourceId;
            this.mContentId = resourceId2;
            float f = getResources().getDisplayMetrics().density;
            this.mTapThreshold = (int) ((6.0f * f) + 0.5f);
            this.mMaximumTapVelocity = (int) ((100.0f * f) + 0.5f);
            this.mMaximumMinorVelocity = (int) ((MAXIMUM_MINOR_VELOCITY * f) + 0.5f);
            this.mMaximumMajorVelocity = (int) ((MAXIMUM_MAJOR_VELOCITY * f) + 0.5f);
            this.mMaximumAcceleration = (int) ((MAXIMUM_ACCELERATION * f) + 0.5f);
            this.mVelocityUnits = (int) ((1000.0f * f) + 0.5f);
            typedArrayObtainStyledAttributes.recycle();
            setAlwaysDrawnWithCacheEnabled(false);
            return;
        }
        throw new IllegalArgumentException("The handle attribute is required and must refer to a valid child.");
    }

    @Override
    protected void onFinishInflate() {
        this.mHandle = findViewById(this.mHandleId);
        if (this.mHandle == null) {
            throw new IllegalArgumentException("The handle attribute is must refer to an existing child.");
        }
        this.mHandle.setOnClickListener(new DrawerToggler());
        this.mContent = findViewById(this.mContentId);
        if (this.mContent == null) {
            throw new IllegalArgumentException("The content attribute is must refer to an existing child.");
        }
        this.mContent.setVisibility(8);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int mode = View.MeasureSpec.getMode(i);
        int size = View.MeasureSpec.getSize(i);
        int mode2 = View.MeasureSpec.getMode(i2);
        int size2 = View.MeasureSpec.getSize(i2);
        if (mode == 0 || mode2 == 0) {
            throw new RuntimeException("SlidingDrawer cannot have UNSPECIFIED dimensions");
        }
        View view = this.mHandle;
        measureChild(view, i, i2);
        if (this.mVertical) {
            this.mContent.measure(View.MeasureSpec.makeMeasureSpec(size, 1073741824), View.MeasureSpec.makeMeasureSpec((size2 - view.getMeasuredHeight()) - this.mTopOffset, 1073741824));
        } else {
            this.mContent.measure(View.MeasureSpec.makeMeasureSpec((size - view.getMeasuredWidth()) - this.mTopOffset, 1073741824), View.MeasureSpec.makeMeasureSpec(size2, 1073741824));
        }
        setMeasuredDimension(size, size2);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        float left;
        long drawingTime = getDrawingTime();
        View view = this.mHandle;
        boolean z = this.mVertical;
        drawChild(canvas, view, drawingTime);
        if (this.mTracking || this.mAnimating) {
            Bitmap drawingCache = this.mContent.getDrawingCache();
            if (drawingCache != null) {
                if (z) {
                    canvas.drawBitmap(drawingCache, 0.0f, view.getBottom(), (Paint) null);
                    return;
                } else {
                    canvas.drawBitmap(drawingCache, view.getRight(), 0.0f, (Paint) null);
                    return;
                }
            }
            canvas.save();
            if (!z) {
                left = view.getLeft() - this.mTopOffset;
            } else {
                left = 0.0f;
            }
            canvas.translate(left, z ? view.getTop() - this.mTopOffset : 0.0f);
            drawChild(canvas, this.mContent, drawingTime);
            canvas.restore();
            return;
        }
        if (this.mExpanded) {
            drawChild(canvas, this.mContent, drawingTime);
        }
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int i5;
        int i6;
        if (this.mTracking) {
            return;
        }
        int i7 = i3 - i;
        int i8 = i4 - i2;
        View view = this.mHandle;
        int measuredWidth = view.getMeasuredWidth();
        int measuredHeight = view.getMeasuredHeight();
        View view2 = this.mContent;
        if (this.mVertical) {
            i5 = (i7 - measuredWidth) / 2;
            i6 = this.mExpanded ? this.mTopOffset : (i8 - measuredHeight) + this.mBottomOffset;
            view2.layout(0, this.mTopOffset + measuredHeight, view2.getMeasuredWidth(), this.mTopOffset + measuredHeight + view2.getMeasuredHeight());
        } else {
            i5 = this.mExpanded ? this.mTopOffset : (i7 - measuredWidth) + this.mBottomOffset;
            i6 = (i8 - measuredHeight) / 2;
            view2.layout(this.mTopOffset + measuredWidth, 0, this.mTopOffset + measuredWidth + view2.getMeasuredWidth(), view2.getMeasuredHeight());
        }
        view.layout(i5, i6, measuredWidth + i5, measuredHeight + i6);
        this.mHandleHeight = view.getHeight();
        this.mHandleWidth = view.getWidth();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (this.mLocked) {
            return false;
        }
        int action = motionEvent.getAction();
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        Rect rect = this.mFrame;
        View view = this.mHandle;
        view.getHitRect(rect);
        if (!this.mTracking && !rect.contains((int) x, (int) y)) {
            return false;
        }
        if (action == 0) {
            this.mTracking = true;
            view.setPressed(true);
            prepareContent();
            if (this.mOnDrawerScrollListener != null) {
                this.mOnDrawerScrollListener.onScrollStarted();
            }
            if (this.mVertical) {
                int top = this.mHandle.getTop();
                this.mTouchDelta = ((int) y) - top;
                prepareTracking(top);
            } else {
                int left = this.mHandle.getLeft();
                this.mTouchDelta = ((int) x) - left;
                prepareTracking(left);
            }
            this.mVelocityTracker.addMovement(motionEvent);
        }
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        boolean z;
        if (this.mLocked) {
            return true;
        }
        if (this.mTracking) {
            this.mVelocityTracker.addMovement(motionEvent);
            switch (motionEvent.getAction()) {
                case 1:
                case 3:
                    VelocityTracker velocityTracker = this.mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(this.mVelocityUnits);
                    float yVelocity = velocityTracker.getYVelocity();
                    float xVelocity = velocityTracker.getXVelocity();
                    boolean z2 = this.mVertical;
                    if (z2) {
                        z = yVelocity < 0.0f;
                        if (xVelocity < 0.0f) {
                            xVelocity = -xVelocity;
                        }
                        if (xVelocity > this.mMaximumMinorVelocity) {
                            xVelocity = this.mMaximumMinorVelocity;
                        }
                    } else {
                        z = xVelocity < 0.0f;
                        if (yVelocity < 0.0f) {
                            yVelocity = -yVelocity;
                        }
                        if (yVelocity > this.mMaximumMinorVelocity) {
                            yVelocity = this.mMaximumMinorVelocity;
                        }
                    }
                    float fHypot = (float) Math.hypot(xVelocity, yVelocity);
                    if (z) {
                        fHypot = -fHypot;
                    }
                    int top = this.mHandle.getTop();
                    int left = this.mHandle.getLeft();
                    if (Math.abs(fHypot) < this.mMaximumTapVelocity) {
                        if (z2) {
                            if (!z2) {
                                top = left;
                            }
                            performFling(top, fHypot, false, true);
                        } else {
                            if (!z2) {
                            }
                            performFling(top, fHypot, false, true);
                        }
                    } else {
                        if (!z2) {
                            top = left;
                        }
                        performFling(top, fHypot, false, true);
                    }
                    break;
                case 2:
                    moveHandle(((int) (this.mVertical ? motionEvent.getY() : motionEvent.getX())) - this.mTouchDelta);
                    break;
            }
        }
        return this.mTracking || this.mAnimating || super.onTouchEvent(motionEvent);
    }

    private void animateClose(int i, boolean z) {
        prepareTracking(i);
        performFling(i, this.mMaximumAcceleration, true, z);
    }

    private void animateOpen(int i, boolean z) {
        prepareTracking(i);
        performFling(i, -this.mMaximumAcceleration, true, z);
    }

    private void performFling(int i, float f, boolean z, boolean z2) {
        this.mAnimationPosition = i;
        this.mAnimatedVelocity = f;
        if (this.mExpanded) {
            if (z || f > this.mMaximumMajorVelocity) {
                this.mAnimatedAcceleration = this.mMaximumAcceleration;
                if (f < 0.0f) {
                    this.mAnimatedVelocity = 0.0f;
                }
            } else {
                if (i <= this.mTopOffset + (this.mVertical ? this.mHandleHeight : this.mHandleWidth) || f <= (-this.mMaximumMajorVelocity)) {
                    this.mAnimatedAcceleration = -this.mMaximumAcceleration;
                    if (f > 0.0f) {
                        this.mAnimatedVelocity = 0.0f;
                    }
                }
            }
        } else if (!z) {
            if (f <= this.mMaximumMajorVelocity) {
                if (i <= (this.mVertical ? getHeight() : getWidth()) / 2 || f <= (-this.mMaximumMajorVelocity)) {
                }
            }
            this.mAnimatedAcceleration = this.mMaximumAcceleration;
            if (f < 0.0f) {
                this.mAnimatedVelocity = 0.0f;
            }
        } else {
            this.mAnimatedAcceleration = -this.mMaximumAcceleration;
            if (f > 0.0f) {
                this.mAnimatedVelocity = 0.0f;
            }
        }
        long jUptimeMillis = SystemClock.uptimeMillis();
        this.mAnimationLastTime = jUptimeMillis;
        this.mCurrentAnimationTime = jUptimeMillis + 16;
        this.mAnimating = true;
        removeCallbacks(this.mSlidingRunnable);
        postDelayed(this.mSlidingRunnable, 16L);
        stopTracking(z2);
    }

    private void prepareTracking(int i) {
        int width;
        int i2;
        this.mTracking = true;
        this.mVelocityTracker = VelocityTracker.obtain();
        if (!this.mExpanded) {
            this.mAnimatedAcceleration = this.mMaximumAcceleration;
            this.mAnimatedVelocity = this.mMaximumMajorVelocity;
            int i3 = this.mBottomOffset;
            if (this.mVertical) {
                width = getHeight();
                i2 = this.mHandleHeight;
            } else {
                width = getWidth();
                i2 = this.mHandleWidth;
            }
            this.mAnimationPosition = i3 + (width - i2);
            moveHandle((int) this.mAnimationPosition);
            this.mAnimating = true;
            removeCallbacks(this.mSlidingRunnable);
            long jUptimeMillis = SystemClock.uptimeMillis();
            this.mAnimationLastTime = jUptimeMillis;
            this.mCurrentAnimationTime = jUptimeMillis + 16;
            this.mAnimating = true;
            return;
        }
        if (this.mAnimating) {
            this.mAnimating = false;
            removeCallbacks(this.mSlidingRunnable);
        }
        moveHandle(i);
    }

    private void moveHandle(int i) {
        View view = this.mHandle;
        if (this.mVertical) {
            if (i == EXPANDED_FULL_OPEN) {
                view.offsetTopAndBottom(this.mTopOffset - view.getTop());
                invalidate();
                return;
            }
            if (i == COLLAPSED_FULL_CLOSED) {
                view.offsetTopAndBottom((((this.mBottomOffset + this.mBottom) - this.mTop) - this.mHandleHeight) - view.getTop());
                invalidate();
                return;
            }
            int top = view.getTop();
            int i2 = i - top;
            if (i < this.mTopOffset) {
                i2 = this.mTopOffset - top;
            } else if (i2 > (((this.mBottomOffset + this.mBottom) - this.mTop) - this.mHandleHeight) - top) {
                i2 = (((this.mBottomOffset + this.mBottom) - this.mTop) - this.mHandleHeight) - top;
            }
            view.offsetTopAndBottom(i2);
            Rect rect = this.mFrame;
            Rect rect2 = this.mInvalidate;
            view.getHitRect(rect);
            rect2.set(rect);
            rect2.union(rect.left, rect.top - i2, rect.right, rect.bottom - i2);
            rect2.union(0, rect.bottom - i2, getWidth(), (rect.bottom - i2) + this.mContent.getHeight());
            invalidate(rect2);
            return;
        }
        if (i == EXPANDED_FULL_OPEN) {
            view.offsetLeftAndRight(this.mTopOffset - view.getLeft());
            invalidate();
            return;
        }
        if (i == COLLAPSED_FULL_CLOSED) {
            view.offsetLeftAndRight((((this.mBottomOffset + this.mRight) - this.mLeft) - this.mHandleWidth) - view.getLeft());
            invalidate();
            return;
        }
        int left = view.getLeft();
        int i3 = i - left;
        if (i < this.mTopOffset) {
            i3 = this.mTopOffset - left;
        } else if (i3 > (((this.mBottomOffset + this.mRight) - this.mLeft) - this.mHandleWidth) - left) {
            i3 = (((this.mBottomOffset + this.mRight) - this.mLeft) - this.mHandleWidth) - left;
        }
        view.offsetLeftAndRight(i3);
        Rect rect3 = this.mFrame;
        Rect rect4 = this.mInvalidate;
        view.getHitRect(rect3);
        rect4.set(rect3);
        rect4.union(rect3.left - i3, rect3.top, rect3.right - i3, rect3.bottom);
        rect4.union(rect3.right - i3, 0, (rect3.right - i3) + this.mContent.getWidth(), getHeight());
        invalidate(rect4);
    }

    private void prepareContent() {
        if (this.mAnimating) {
            return;
        }
        View view = this.mContent;
        if (view.isLayoutRequested()) {
            if (this.mVertical) {
                int i = this.mHandleHeight;
                view.measure(View.MeasureSpec.makeMeasureSpec(this.mRight - this.mLeft, 1073741824), View.MeasureSpec.makeMeasureSpec(((this.mBottom - this.mTop) - i) - this.mTopOffset, 1073741824));
                view.layout(0, this.mTopOffset + i, view.getMeasuredWidth(), this.mTopOffset + i + view.getMeasuredHeight());
            } else {
                int width = this.mHandle.getWidth();
                view.measure(View.MeasureSpec.makeMeasureSpec(((this.mRight - this.mLeft) - width) - this.mTopOffset, 1073741824), View.MeasureSpec.makeMeasureSpec(this.mBottom - this.mTop, 1073741824));
                view.layout(this.mTopOffset + width, 0, this.mTopOffset + width + view.getMeasuredWidth(), view.getMeasuredHeight());
            }
        }
        view.getViewTreeObserver().dispatchOnPreDraw();
        if (!view.isHardwareAccelerated()) {
            view.buildDrawingCache();
        }
        view.setVisibility(8);
    }

    private void stopTracking(boolean z) {
        this.mHandle.setPressed(false);
        this.mTracking = false;
        if (z && this.mOnDrawerScrollListener != null) {
            this.mOnDrawerScrollListener.onScrollEnded();
        }
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    private void doAnimation() {
        if (this.mAnimating) {
            incrementAnimation();
            if (this.mAnimationPosition >= (this.mBottomOffset + (this.mVertical ? getHeight() : getWidth())) - 1) {
                this.mAnimating = false;
                closeDrawer();
            } else if (this.mAnimationPosition < this.mTopOffset) {
                this.mAnimating = false;
                openDrawer();
            } else {
                moveHandle((int) this.mAnimationPosition);
                this.mCurrentAnimationTime += 16;
                postDelayed(this.mSlidingRunnable, 16L);
            }
        }
    }

    private void incrementAnimation() {
        long jUptimeMillis = SystemClock.uptimeMillis();
        float f = (jUptimeMillis - this.mAnimationLastTime) / 1000.0f;
        float f2 = this.mAnimationPosition;
        float f3 = this.mAnimatedVelocity;
        float f4 = this.mAnimatedAcceleration;
        this.mAnimationPosition = f2 + (f3 * f) + (0.5f * f4 * f * f);
        this.mAnimatedVelocity = f3 + (f4 * f);
        this.mAnimationLastTime = jUptimeMillis;
    }

    public void toggle() {
        if (!this.mExpanded) {
            openDrawer();
        } else {
            closeDrawer();
        }
        invalidate();
        requestLayout();
    }

    public void animateToggle() {
        if (!this.mExpanded) {
            animateOpen();
        } else {
            animateClose();
        }
    }

    public void open() {
        openDrawer();
        invalidate();
        requestLayout();
        sendAccessibilityEvent(32);
    }

    public void close() {
        closeDrawer();
        invalidate();
        requestLayout();
    }

    public void animateClose() {
        prepareContent();
        OnDrawerScrollListener onDrawerScrollListener = this.mOnDrawerScrollListener;
        if (onDrawerScrollListener != null) {
            onDrawerScrollListener.onScrollStarted();
        }
        animateClose(this.mVertical ? this.mHandle.getTop() : this.mHandle.getLeft(), false);
        if (onDrawerScrollListener != null) {
            onDrawerScrollListener.onScrollEnded();
        }
    }

    public void animateOpen() {
        prepareContent();
        OnDrawerScrollListener onDrawerScrollListener = this.mOnDrawerScrollListener;
        if (onDrawerScrollListener != null) {
            onDrawerScrollListener.onScrollStarted();
        }
        animateOpen(this.mVertical ? this.mHandle.getTop() : this.mHandle.getLeft(), false);
        sendAccessibilityEvent(32);
        if (onDrawerScrollListener != null) {
            onDrawerScrollListener.onScrollEnded();
        }
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return SlidingDrawer.class.getName();
    }

    private void closeDrawer() {
        moveHandle(COLLAPSED_FULL_CLOSED);
        this.mContent.setVisibility(8);
        this.mContent.destroyDrawingCache();
        if (!this.mExpanded) {
            return;
        }
        this.mExpanded = false;
        if (this.mOnDrawerCloseListener != null) {
            this.mOnDrawerCloseListener.onDrawerClosed();
        }
    }

    private void openDrawer() {
        moveHandle(EXPANDED_FULL_OPEN);
        this.mContent.setVisibility(0);
        if (this.mExpanded) {
            return;
        }
        this.mExpanded = true;
        if (this.mOnDrawerOpenListener != null) {
            this.mOnDrawerOpenListener.onDrawerOpened();
        }
    }

    public void setOnDrawerOpenListener(OnDrawerOpenListener onDrawerOpenListener) {
        this.mOnDrawerOpenListener = onDrawerOpenListener;
    }

    public void setOnDrawerCloseListener(OnDrawerCloseListener onDrawerCloseListener) {
        this.mOnDrawerCloseListener = onDrawerCloseListener;
    }

    public void setOnDrawerScrollListener(OnDrawerScrollListener onDrawerScrollListener) {
        this.mOnDrawerScrollListener = onDrawerScrollListener;
    }

    public View getHandle() {
        return this.mHandle;
    }

    public View getContent() {
        return this.mContent;
    }

    public void unlock() {
        this.mLocked = false;
    }

    public void lock() {
        this.mLocked = true;
    }

    public boolean isOpened() {
        return this.mExpanded;
    }

    public boolean isMoving() {
        return this.mTracking || this.mAnimating;
    }

    private class DrawerToggler implements View.OnClickListener {
        private DrawerToggler() {
        }

        @Override
        public void onClick(View view) {
            if (!SlidingDrawer.this.mLocked) {
                if (SlidingDrawer.this.mAnimateOnClick) {
                    SlidingDrawer.this.animateToggle();
                } else {
                    SlidingDrawer.this.toggle();
                }
            }
        }
    }
}
