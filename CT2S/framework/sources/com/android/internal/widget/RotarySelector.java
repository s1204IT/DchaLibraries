package com.android.internal.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.AudioAttributes;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import com.android.internal.R;

public class RotarySelector extends View {
    private static final int ARROW_SCRUNCH_DIP = 6;
    private static final boolean DBG = false;
    private static final int EDGE_PADDING_DIP = 9;
    private static final int EDGE_TRIGGER_DIP = 100;
    public static final int HORIZONTAL = 0;
    public static final int LEFT_HANDLE_GRABBED = 1;
    private static final String LOG_TAG = "RotarySelector";
    public static final int NOTHING_GRABBED = 0;
    static final int OUTER_ROTARY_RADIUS_DIP = 390;
    public static final int RIGHT_HANDLE_GRABBED = 2;
    static final int ROTARY_STROKE_WIDTH_DIP = 83;
    static final int SNAP_BACK_ANIMATION_DURATION_MILLIS = 300;
    static final int SPIN_ANIMATION_DURATION_MILLIS = 800;
    public static final int VERTICAL = 1;
    private static final long VIBRATE_LONG = 20;
    private static final long VIBRATE_SHORT = 20;
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(4).setUsage(13).build();
    private static final boolean VISUAL_DEBUG = false;
    private boolean mAnimating;
    private int mAnimatingDeltaXEnd;
    private int mAnimatingDeltaXStart;
    private long mAnimationDuration;
    private long mAnimationStartTime;
    private Bitmap mArrowLongLeft;
    private Bitmap mArrowLongRight;
    final Matrix mArrowMatrix;
    private Bitmap mArrowShortLeftAndRight;
    private Bitmap mBackground;
    private int mBackgroundHeight;
    private int mBackgroundWidth;
    final Matrix mBgMatrix;
    private float mDensity;
    private Bitmap mDimple;
    private Bitmap mDimpleDim;
    private int mDimpleSpacing;
    private int mDimpleWidth;
    private int mDimplesOfFling;
    private int mEdgeTriggerThresh;
    private int mGrabbedState;
    private final int mInnerRadius;
    private DecelerateInterpolator mInterpolator;
    private Bitmap mLeftHandleIcon;
    private int mLeftHandleX;
    private int mMaximumVelocity;
    private int mMinimumVelocity;
    private OnDialTriggerListener mOnDialTriggerListener;
    private int mOrientation;
    private final int mOuterRadius;
    private Paint mPaint;
    private Bitmap mRightHandleIcon;
    private int mRightHandleX;
    private int mRotaryOffsetX;
    private boolean mTriggered;
    private VelocityTracker mVelocityTracker;
    private Vibrator mVibrator;

    public interface OnDialTriggerListener {
        public static final int LEFT_HANDLE = 1;
        public static final int RIGHT_HANDLE = 2;

        void onDialTrigger(View view, int i);

        void onGrabbedStateChange(View view, int i);
    }

    public RotarySelector(Context context) {
        this(context, null);
    }

    public RotarySelector(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mRotaryOffsetX = 0;
        this.mAnimating = false;
        this.mPaint = new Paint();
        this.mBgMatrix = new Matrix();
        this.mArrowMatrix = new Matrix();
        this.mGrabbedState = 0;
        this.mTriggered = false;
        this.mDimplesOfFling = 0;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RotarySelector);
        this.mOrientation = a.getInt(0, 0);
        a.recycle();
        Resources r = getResources();
        this.mDensity = r.getDisplayMetrics().density;
        this.mBackground = getBitmapFor(R.drawable.jog_dial_bg);
        this.mDimple = getBitmapFor(R.drawable.jog_dial_dimple);
        this.mDimpleDim = getBitmapFor(R.drawable.jog_dial_dimple_dim);
        this.mArrowLongLeft = getBitmapFor(R.drawable.jog_dial_arrow_long_left_green);
        this.mArrowLongRight = getBitmapFor(R.drawable.jog_dial_arrow_long_right_red);
        this.mArrowShortLeftAndRight = getBitmapFor(R.drawable.jog_dial_arrow_short_left_and_right);
        this.mInterpolator = new DecelerateInterpolator(1.0f);
        this.mEdgeTriggerThresh = (int) (this.mDensity * 100.0f);
        this.mDimpleWidth = this.mDimple.getWidth();
        this.mBackgroundWidth = this.mBackground.getWidth();
        this.mBackgroundHeight = this.mBackground.getHeight();
        this.mOuterRadius = (int) (this.mDensity * 390.0f);
        this.mInnerRadius = (int) (307.0f * this.mDensity);
        ViewConfiguration configuration = ViewConfiguration.get(this.mContext);
        this.mMinimumVelocity = configuration.getScaledMinimumFlingVelocity() * 2;
        this.mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    private Bitmap getBitmapFor(int resId) {
        return BitmapFactory.decodeResource(getContext().getResources(), resId);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int edgePadding = (int) (9.0f * this.mDensity);
        this.mLeftHandleX = (this.mDimpleWidth / 2) + edgePadding;
        int length = isHoriz() ? w : h;
        this.mRightHandleX = (length - edgePadding) - (this.mDimpleWidth / 2);
        this.mDimpleSpacing = (length / 2) - this.mLeftHandleX;
        this.mBgMatrix.setTranslate(0.0f, 0.0f);
        if (!isHoriz()) {
            int left = w - this.mBackgroundHeight;
            this.mBgMatrix.preRotate(-90.0f, 0.0f, 0.0f);
            this.mBgMatrix.postTranslate(left, h);
            return;
        }
        this.mBgMatrix.postTranslate(0.0f, h - this.mBackgroundHeight);
    }

    private boolean isHoriz() {
        return this.mOrientation == 0;
    }

    public void setLeftHandleResource(int resId) {
        if (resId != 0) {
            this.mLeftHandleIcon = getBitmapFor(resId);
        }
        invalidate();
    }

    public void setRightHandleResource(int resId) {
        if (resId != 0) {
            this.mRightHandleIcon = getBitmapFor(resId);
        }
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int length = isHoriz() ? View.MeasureSpec.getSize(widthMeasureSpec) : View.MeasureSpec.getSize(heightMeasureSpec);
        int arrowScrunch = (int) (6.0f * this.mDensity);
        int arrowH = this.mArrowShortLeftAndRight.getHeight();
        int height = (this.mBackgroundHeight + arrowH) - arrowScrunch;
        if (isHoriz()) {
            setMeasuredDimension(length, height);
        } else {
            setMeasuredDimension(height, length);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (this.mAnimating) {
            updateAnimation();
        }
        canvas.drawBitmap(this.mBackground, this.mBgMatrix, this.mPaint);
        this.mArrowMatrix.reset();
        switch (this.mGrabbedState) {
            case 0:
                break;
            case 1:
                this.mArrowMatrix.setTranslate(0.0f, 0.0f);
                if (!isHoriz()) {
                    this.mArrowMatrix.preRotate(-90.0f, 0.0f, 0.0f);
                    this.mArrowMatrix.postTranslate(0.0f, height);
                }
                canvas.drawBitmap(this.mArrowLongLeft, this.mArrowMatrix, this.mPaint);
                break;
            case 2:
                this.mArrowMatrix.setTranslate(0.0f, 0.0f);
                if (!isHoriz()) {
                    this.mArrowMatrix.preRotate(-90.0f, 0.0f, 0.0f);
                    this.mArrowMatrix.postTranslate(0.0f, (this.mBackgroundWidth - height) + height);
                }
                canvas.drawBitmap(this.mArrowLongRight, this.mArrowMatrix, this.mPaint);
                break;
            default:
                throw new IllegalStateException("invalid mGrabbedState: " + this.mGrabbedState);
        }
        int bgHeight = this.mBackgroundHeight;
        int bgTop = isHoriz() ? height - bgHeight : width - bgHeight;
        int xOffset = this.mLeftHandleX + this.mRotaryOffsetX;
        int drawableY = getYOnArc(this.mBackgroundWidth, this.mInnerRadius, this.mOuterRadius, xOffset);
        int x = isHoriz() ? xOffset : drawableY + bgTop;
        int y = isHoriz() ? drawableY + bgTop : height - xOffset;
        if (this.mGrabbedState != 2) {
            drawCentered(this.mDimple, canvas, x, y);
            drawCentered(this.mLeftHandleIcon, canvas, x, y);
        } else {
            drawCentered(this.mDimpleDim, canvas, x, y);
        }
        int xOffset2 = isHoriz() ? (width / 2) + this.mRotaryOffsetX : (height / 2) + this.mRotaryOffsetX;
        int drawableY2 = getYOnArc(this.mBackgroundWidth, this.mInnerRadius, this.mOuterRadius, xOffset2);
        if (isHoriz()) {
            drawCentered(this.mDimpleDim, canvas, xOffset2, drawableY2 + bgTop);
        } else {
            drawCentered(this.mDimpleDim, canvas, drawableY2 + bgTop, height - xOffset2);
        }
        int xOffset3 = this.mRightHandleX + this.mRotaryOffsetX;
        int drawableY3 = getYOnArc(this.mBackgroundWidth, this.mInnerRadius, this.mOuterRadius, xOffset3);
        int x2 = isHoriz() ? xOffset3 : drawableY3 + bgTop;
        int y2 = isHoriz() ? drawableY3 + bgTop : height - xOffset3;
        if (this.mGrabbedState != 1) {
            drawCentered(this.mDimple, canvas, x2, y2);
            drawCentered(this.mRightHandleIcon, canvas, x2, y2);
        } else {
            drawCentered(this.mDimpleDim, canvas, x2, y2);
        }
        int dimpleLeft = (this.mRotaryOffsetX + this.mLeftHandleX) - this.mDimpleSpacing;
        int halfdimple = this.mDimpleWidth / 2;
        while (dimpleLeft > (-halfdimple)) {
            int drawableY4 = getYOnArc(this.mBackgroundWidth, this.mInnerRadius, this.mOuterRadius, dimpleLeft);
            if (isHoriz()) {
                drawCentered(this.mDimpleDim, canvas, dimpleLeft, drawableY4 + bgTop);
            } else {
                drawCentered(this.mDimpleDim, canvas, drawableY4 + bgTop, height - dimpleLeft);
            }
            dimpleLeft -= this.mDimpleSpacing;
        }
        int dimpleRight = this.mRotaryOffsetX + this.mRightHandleX + this.mDimpleSpacing;
        int rightThresh = this.mRight + halfdimple;
        while (dimpleRight < rightThresh) {
            int drawableY5 = getYOnArc(this.mBackgroundWidth, this.mInnerRadius, this.mOuterRadius, dimpleRight);
            if (isHoriz()) {
                drawCentered(this.mDimpleDim, canvas, dimpleRight, drawableY5 + bgTop);
            } else {
                drawCentered(this.mDimpleDim, canvas, drawableY5 + bgTop, height - dimpleRight);
            }
            dimpleRight += this.mDimpleSpacing;
        }
    }

    private int getYOnArc(int backgroundWidth, int innerRadius, int outerRadius, int x) {
        int halfWidth = (outerRadius - innerRadius) / 2;
        int middleRadius = innerRadius + halfWidth;
        int triangleBottom = (backgroundWidth / 2) - x;
        int triangleY = (int) Math.sqrt((middleRadius * middleRadius) - (triangleBottom * triangleBottom));
        return (middleRadius - triangleY) + halfWidth;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!this.mAnimating) {
            if (this.mVelocityTracker == null) {
                this.mVelocityTracker = VelocityTracker.obtain();
            }
            this.mVelocityTracker.addMovement(event);
            int height = getHeight();
            int eventX = isHoriz() ? (int) event.getX() : height - ((int) event.getY());
            int hitWindow = this.mDimpleWidth;
            int action = event.getAction();
            switch (action) {
                case 0:
                    this.mTriggered = false;
                    if (this.mGrabbedState != 0) {
                        reset();
                        invalidate();
                    }
                    if (eventX < this.mLeftHandleX + hitWindow) {
                        this.mRotaryOffsetX = eventX - this.mLeftHandleX;
                        setGrabbedState(1);
                        invalidate();
                        vibrate(20L);
                    } else if (eventX > this.mRightHandleX - hitWindow) {
                        this.mRotaryOffsetX = eventX - this.mRightHandleX;
                        setGrabbedState(2);
                        invalidate();
                        vibrate(20L);
                    }
                    break;
                case 1:
                    if (this.mGrabbedState == 1 && Math.abs(eventX - this.mLeftHandleX) > 5) {
                        startAnimation(eventX - this.mLeftHandleX, 0, 300);
                    } else if (this.mGrabbedState == 2 && Math.abs(eventX - this.mRightHandleX) > 5) {
                        startAnimation(eventX - this.mRightHandleX, 0, 300);
                    }
                    this.mRotaryOffsetX = 0;
                    setGrabbedState(0);
                    invalidate();
                    if (this.mVelocityTracker != null) {
                        this.mVelocityTracker.recycle();
                        this.mVelocityTracker = null;
                    }
                    break;
                case 2:
                    if (this.mGrabbedState == 1) {
                        this.mRotaryOffsetX = eventX - this.mLeftHandleX;
                        invalidate();
                        int rightThresh = isHoriz() ? getRight() : height;
                        if (eventX >= rightThresh - this.mEdgeTriggerThresh && !this.mTriggered) {
                            this.mTriggered = true;
                            dispatchTriggerEvent(1);
                            VelocityTracker velocityTracker = this.mVelocityTracker;
                            velocityTracker.computeCurrentVelocity(1000, this.mMaximumVelocity);
                            int rawVelocity = isHoriz() ? (int) velocityTracker.getXVelocity() : -((int) velocityTracker.getYVelocity());
                            int velocity = Math.max(this.mMinimumVelocity, rawVelocity);
                            this.mDimplesOfFling = Math.max(8, Math.abs(velocity / this.mDimpleSpacing));
                            startAnimationWithVelocity(eventX - this.mLeftHandleX, this.mDimplesOfFling * this.mDimpleSpacing, velocity);
                        }
                    } else if (this.mGrabbedState == 2) {
                        this.mRotaryOffsetX = eventX - this.mRightHandleX;
                        invalidate();
                        if (eventX <= this.mEdgeTriggerThresh && !this.mTriggered) {
                            this.mTriggered = true;
                            dispatchTriggerEvent(2);
                            VelocityTracker velocityTracker2 = this.mVelocityTracker;
                            velocityTracker2.computeCurrentVelocity(1000, this.mMaximumVelocity);
                            int rawVelocity2 = isHoriz() ? (int) velocityTracker2.getXVelocity() : -((int) velocityTracker2.getYVelocity());
                            int velocity2 = Math.min(-this.mMinimumVelocity, rawVelocity2);
                            this.mDimplesOfFling = Math.max(8, Math.abs(velocity2 / this.mDimpleSpacing));
                            startAnimationWithVelocity(eventX - this.mRightHandleX, -(this.mDimplesOfFling * this.mDimpleSpacing), velocity2);
                        }
                    }
                    break;
                case 3:
                    reset();
                    invalidate();
                    if (this.mVelocityTracker != null) {
                        this.mVelocityTracker.recycle();
                        this.mVelocityTracker = null;
                    }
                    break;
            }
        }
        return true;
    }

    private void startAnimation(int startX, int endX, int duration) {
        this.mAnimating = true;
        this.mAnimationStartTime = AnimationUtils.currentAnimationTimeMillis();
        this.mAnimationDuration = duration;
        this.mAnimatingDeltaXStart = startX;
        this.mAnimatingDeltaXEnd = endX;
        setGrabbedState(0);
        this.mDimplesOfFling = 0;
        invalidate();
    }

    private void startAnimationWithVelocity(int startX, int endX, int pixelsPerSecond) {
        this.mAnimating = true;
        this.mAnimationStartTime = AnimationUtils.currentAnimationTimeMillis();
        this.mAnimationDuration = ((endX - startX) * 1000) / pixelsPerSecond;
        this.mAnimatingDeltaXStart = startX;
        this.mAnimatingDeltaXEnd = endX;
        setGrabbedState(0);
        invalidate();
    }

    private void updateAnimation() {
        long millisSoFar = AnimationUtils.currentAnimationTimeMillis() - this.mAnimationStartTime;
        long millisLeft = this.mAnimationDuration - millisSoFar;
        int totalDeltaX = this.mAnimatingDeltaXStart - this.mAnimatingDeltaXEnd;
        boolean goingRight = totalDeltaX < 0;
        if (millisLeft <= 0) {
            reset();
            return;
        }
        float interpolation = this.mInterpolator.getInterpolation(millisSoFar / this.mAnimationDuration);
        int dx = (int) (totalDeltaX * (1.0f - interpolation));
        this.mRotaryOffsetX = this.mAnimatingDeltaXEnd + dx;
        if (this.mDimplesOfFling > 0) {
            if (!goingRight && this.mRotaryOffsetX < this.mDimpleSpacing * (-3)) {
                this.mRotaryOffsetX += this.mDimplesOfFling * this.mDimpleSpacing;
            } else if (goingRight && this.mRotaryOffsetX > this.mDimpleSpacing * 3) {
                this.mRotaryOffsetX -= this.mDimplesOfFling * this.mDimpleSpacing;
            }
        }
        invalidate();
    }

    private void reset() {
        this.mAnimating = false;
        this.mRotaryOffsetX = 0;
        this.mDimplesOfFling = 0;
        setGrabbedState(0);
        this.mTriggered = false;
    }

    private synchronized void vibrate(long duration) {
        synchronized (this) {
            boolean hapticEnabled = Settings.System.getIntForUser(this.mContext.getContentResolver(), Settings.System.HAPTIC_FEEDBACK_ENABLED, 1, -2) != 0;
            if (hapticEnabled) {
                if (this.mVibrator == null) {
                    this.mVibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                }
                this.mVibrator.vibrate(duration, VIBRATION_ATTRIBUTES);
            }
        }
    }

    private void drawCentered(Bitmap d, Canvas c, int x, int y) {
        int w = d.getWidth();
        int h = d.getHeight();
        c.drawBitmap(d, x - (w / 2), y - (h / 2), this.mPaint);
    }

    public void setOnDialTriggerListener(OnDialTriggerListener l) {
        this.mOnDialTriggerListener = l;
    }

    private void dispatchTriggerEvent(int whichHandle) {
        vibrate(20L);
        if (this.mOnDialTriggerListener != null) {
            this.mOnDialTriggerListener.onDialTrigger(this, whichHandle);
        }
    }

    private void setGrabbedState(int newState) {
        if (newState != this.mGrabbedState) {
            this.mGrabbedState = newState;
            if (this.mOnDialTriggerListener != null) {
                this.mOnDialTriggerListener.onGrabbedStateChange(this, this.mGrabbedState);
            }
        }
    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
