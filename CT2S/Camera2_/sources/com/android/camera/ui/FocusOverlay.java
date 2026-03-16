package com.android.camera.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import com.android.camera.FocusOverlayManager;
import com.android.camera.debug.DebugPropertyHelper;
import com.android.camera.debug.Log;
import com.android.camera2.R;

public class FocusOverlay extends View implements FocusOverlayManager.FocusUI {
    private static final int FOCUS_DURATION_MS = 500;
    private static final int FOCUS_INDICATOR_ROTATION_DEGREES = 50;
    private int mAngle;
    private final Rect mBounds;
    private Paint mDebugCornersPaint;
    private int mDebugFailColor;
    private String mDebugMessage;
    private Paint mDebugSolidPaint;
    private int mDebugStartColor;
    private int mDebugSuccessColor;
    private Paint mDebugTextPaint;
    private final ValueAnimator mFocusAnimation;
    private Rect mFocusDebugCornersRect;
    private Rect mFocusDebugSolidRect;
    private final Drawable mFocusIndicator;
    private final int mFocusIndicatorSize;
    private Drawable mFocusOuterRing;
    private final int mFocusOuterRingSize;
    private boolean mIsPassiveScan;
    private int mPositionX;
    private int mPositionY;
    private boolean mShowIndicator;
    private static final Log.Tag TAG = new Log.Tag("FocusOverlay");
    private static final boolean CAPTURE_DEBUG_UI = DebugPropertyHelper.showCaptureDebugUI();

    public FocusOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mBounds = new Rect();
        this.mFocusAnimation = new ValueAnimator();
        this.mFocusIndicator = getResources().getDrawable(R.drawable.focus_ring_touch_inner);
        this.mFocusIndicatorSize = getResources().getDimensionPixelSize(R.dimen.focus_inner_ring_size);
        this.mFocusOuterRing = getResources().getDrawable(R.drawable.focus_ring_touch_outer);
        this.mFocusOuterRingSize = getResources().getDimensionPixelSize(R.dimen.focus_outer_ring_size);
        if (CAPTURE_DEBUG_UI) {
            Resources res = getResources();
            this.mDebugStartColor = res.getColor(R.color.focus_debug);
            this.mDebugSuccessColor = res.getColor(R.color.focus_debug_success);
            this.mDebugFailColor = res.getColor(R.color.focus_debug_fail);
            this.mDebugTextPaint = new Paint();
            this.mDebugTextPaint.setColor(res.getColor(R.color.focus_debug_text));
            this.mDebugTextPaint.setStyle(Paint.Style.FILL);
            this.mDebugSolidPaint = new Paint();
            this.mDebugSolidPaint.setColor(res.getColor(R.color.focus_debug));
            this.mDebugSolidPaint.setAntiAlias(true);
            this.mDebugSolidPaint.setStyle(Paint.Style.STROKE);
            this.mDebugSolidPaint.setStrokeWidth(res.getDimension(R.dimen.focus_debug_stroke));
            this.mDebugCornersPaint = new Paint(this.mDebugSolidPaint);
            this.mDebugCornersPaint.setColor(res.getColor(R.color.focus_debug));
            this.mFocusDebugSolidRect = new Rect();
            this.mFocusDebugCornersRect = new Rect();
        }
    }

    @Override
    public boolean hasFaces() {
        return false;
    }

    @Override
    public void clearFocus() {
        this.mShowIndicator = false;
        if (CAPTURE_DEBUG_UI) {
            setVisibility(4);
        }
        invalidate();
    }

    @Override
    public void setFocusPosition(int x, int y, boolean isPassiveScan) {
        setFocusPosition(x, y, isPassiveScan, 0, 0);
    }

    @Override
    public void setFocusPosition(int x, int y, boolean isPassiveScan, int aFsize, int aEsize) {
        this.mIsPassiveScan = isPassiveScan;
        this.mPositionX = x;
        this.mPositionY = y;
        this.mBounds.set(x - (this.mFocusIndicatorSize / 2), y - (this.mFocusIndicatorSize / 2), (this.mFocusIndicatorSize / 2) + x, (this.mFocusIndicatorSize / 2) + y);
        this.mFocusIndicator.setBounds(this.mBounds);
        this.mFocusOuterRing.setBounds(x - (this.mFocusOuterRingSize / 2), y - (this.mFocusOuterRingSize / 2), (this.mFocusOuterRingSize / 2) + x, (this.mFocusOuterRingSize / 2) + y);
        if (CAPTURE_DEBUG_UI) {
            this.mFocusOuterRing.setBounds(0, 0, 0, 0);
            if (isPassiveScan) {
                this.mFocusDebugSolidRect.setEmpty();
                int avg = (aFsize + aEsize) / 2;
                this.mFocusDebugCornersRect.set(x - (avg / 2), y - (avg / 2), (avg / 2) + x, (avg / 2) + y);
            } else {
                this.mFocusDebugSolidRect.set(x - (aFsize / 2), y - (aFsize / 2), (aFsize / 2) + x, (aFsize / 2) + y);
                if (aFsize != aEsize) {
                    this.mFocusDebugCornersRect.set(x - (aEsize / 2), y - (aEsize / 2), (aEsize / 2) + x, (aEsize / 2) + y);
                } else {
                    this.mFocusDebugCornersRect.setEmpty();
                }
            }
            this.mDebugSolidPaint.setColor(this.mDebugStartColor);
            this.mDebugCornersPaint.setColor(this.mDebugStartColor);
        }
        if (getVisibility() != 0) {
            setVisibility(0);
        }
        invalidate();
    }

    @Override
    public void onFocusStarted() {
        this.mShowIndicator = true;
        this.mFocusAnimation.setIntValues(0, FOCUS_INDICATOR_ROTATION_DEGREES);
        this.mFocusAnimation.setDuration(500L);
        this.mFocusAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                FocusOverlay.this.mAngle = ((Integer) animation.getAnimatedValue()).intValue();
                FocusOverlay.this.invalidate();
            }
        });
        this.mFocusAnimation.start();
        if (CAPTURE_DEBUG_UI) {
            this.mDebugMessage = null;
        }
    }

    @Override
    public void onFocusSucceeded() {
        this.mFocusAnimation.cancel();
        this.mShowIndicator = false;
        if (CAPTURE_DEBUG_UI && !this.mIsPassiveScan) {
            this.mDebugSolidPaint.setColor(this.mDebugSuccessColor);
        }
        invalidate();
    }

    @Override
    public void onFocusFailed() {
        this.mFocusAnimation.cancel();
        this.mShowIndicator = false;
        if (CAPTURE_DEBUG_UI && !this.mIsPassiveScan) {
            this.mDebugSolidPaint.setColor(this.mDebugFailColor);
        }
        invalidate();
    }

    @Override
    public void setPassiveFocusSuccess(boolean success) {
        this.mFocusAnimation.cancel();
        this.mShowIndicator = false;
        if (CAPTURE_DEBUG_UI) {
            this.mDebugCornersPaint.setColor(success ? this.mDebugSuccessColor : this.mDebugFailColor);
        }
        invalidate();
    }

    @Override
    public void showDebugMessage(String message) {
        if (CAPTURE_DEBUG_UI) {
            this.mDebugMessage = message;
        }
    }

    @Override
    public void pauseFaceDetection() {
    }

    @Override
    public void resumeFaceDetection() {
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mShowIndicator) {
            this.mFocusOuterRing.draw(canvas);
            canvas.save();
            canvas.rotate(this.mAngle, this.mPositionX, this.mPositionY);
            this.mFocusIndicator.draw(canvas);
            canvas.restore();
        }
        if (CAPTURE_DEBUG_UI) {
            canvas.drawRect(this.mFocusDebugSolidRect, this.mDebugSolidPaint);
            float delta = 0.1f * this.mFocusDebugCornersRect.width();
            float left = this.mFocusDebugCornersRect.left;
            float top = this.mFocusDebugCornersRect.top;
            float right = this.mFocusDebugCornersRect.right;
            float bot = this.mFocusDebugCornersRect.bottom;
            canvas.drawLines(new float[]{left, top + delta, left, top, left, top, left + delta, top}, this.mDebugCornersPaint);
            canvas.drawLines(new float[]{right, top + delta, right, top, right, top, right - delta, top}, this.mDebugCornersPaint);
            canvas.drawLines(new float[]{left, bot - delta, left, bot, left, bot, left + delta, bot}, this.mDebugCornersPaint);
            canvas.drawLines(new float[]{right, bot - delta, right, bot, right, bot, right - delta, bot}, this.mDebugCornersPaint);
            if (this.mDebugMessage != null) {
                this.mDebugTextPaint.setTextSize(40.0f);
                canvas.drawText(this.mDebugMessage, left - 4.0f, 44.0f + bot, this.mDebugTextPaint);
            }
        }
    }
}
