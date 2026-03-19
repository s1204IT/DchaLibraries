package com.mediatek.camera.feature.mode.longexposure;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;

public class LongExposureView extends View {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(LongExposureView.class.getSimpleName());
    private ValueAnimator mAnimator;
    private OnCountDownFinishListener mCountDownListener;
    private int mCountdownTime;
    private float mCurrentProgress;
    private OnCaptureAbortedListener mListener;
    private Paint mPaint;
    private int mProgressColor;
    private RectF mRectF;
    private int mRingColor;
    private float mRingWidth;
    private LongExposureViewState mState;

    public enum LongExposureViewState {
        STATE_IDLE,
        STATE_PREVIEW,
        STATE_CAPTURE,
        STATE_ABORT
    }

    public interface OnCaptureAbortedListener {
        void onCaptureAbort();
    }

    public interface OnCountDownFinishListener {
        void countDownFinished(boolean z);
    }

    public void setViewStateChangedListener(OnCaptureAbortedListener onCaptureAbortedListener) {
        this.mListener = onCaptureAbortedListener;
    }

    public void updateViewState(LongExposureViewState longExposureViewState) {
        this.mState = longExposureViewState;
        if (LongExposureViewState.STATE_ABORT == this.mState) {
            cancelAnimate();
            stopCountDown();
        }
    }

    public void setAddCountDownListener(OnCountDownFinishListener onCountDownFinishListener) {
        this.mCountDownListener = onCountDownFinishListener;
    }

    public LongExposureView(Context context) {
        this(context, null);
    }

    public LongExposureView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public LongExposureView(Context context, AttributeSet attributeSet, int i) throws Throwable {
        super(context, attributeSet, i);
        this.mCurrentProgress = 0.0f;
        this.mState = LongExposureViewState.STATE_IDLE;
        initAttrs(context, attributeSet);
        initClickListener();
    }

    public void setCountdownTime(int i) {
        this.mCountdownTime = i;
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();
        this.mRingWidth = measuredWidth / 40.0f;
        float f = 0 + ((measuredWidth * 6) / 79);
        this.mRectF = new RectF((this.mRingWidth / 2.0f) + f, f + (this.mRingWidth / 2.0f), (measuredWidth - r6) - (this.mRingWidth / 2.0f), (measuredHeight - r6) - (this.mRingWidth / 2.0f));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        this.mPaint.setColor(this.mRingColor);
        this.mPaint.setStyle(Paint.Style.STROKE);
        this.mPaint.setStrokeWidth(this.mRingWidth);
        this.mPaint.setColor(this.mProgressColor);
        canvas.drawArc(this.mRectF, -90.0f, this.mCurrentProgress, false, this.mPaint);
    }

    public void startCountDown() {
        LogHelper.d(TAG, "[startCountDown] with time = " + this.mCountdownTime + ",mState = " + this.mState);
        this.mAnimator = getValValueAnimator((long) (this.mCountdownTime * 1000));
        this.mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                LongExposureView.this.mCurrentProgress = (int) (360.0f * (Float.valueOf(String.valueOf(valueAnimator.getAnimatedValue())).floatValue() / 100.0f));
                LongExposureView.this.invalidate();
            }
        });
        this.mAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                super.onAnimationEnd(animator);
                if (LongExposureView.this.mCountDownListener != null) {
                    LogHelper.d(LongExposureView.TAG, "[onAnimationEnd] mCurrentProgress " + LongExposureView.this.mCurrentProgress);
                    if (LongExposureView.this.mCurrentProgress == 360.0f) {
                        LongExposureView.this.mCountDownListener.countDownFinished(true);
                    } else {
                        LongExposureView.this.mCountDownListener.countDownFinished(false);
                    }
                }
            }
        });
        this.mAnimator.start();
    }

    private void initAttrs(Context context, AttributeSet attributeSet) throws Throwable {
        TypedArray typedArrayObtainStyledAttributes;
        try {
            typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.LongExposureView);
            try {
                this.mRingColor = typedArrayObtainStyledAttributes.getColor(1, getResources().getColor(android.R.color.transparent));
                this.mProgressColor = typedArrayObtainStyledAttributes.getColor(1, getResources().getColor(android.R.color.holo_orange_dark));
                this.mRingWidth = 40.0f;
                this.mPaint = new Paint(1);
                this.mPaint.setAntiAlias(true);
                setWillNotDraw(false);
                if (typedArrayObtainStyledAttributes != null) {
                    typedArrayObtainStyledAttributes.recycle();
                }
            } catch (Throwable th) {
                th = th;
                if (typedArrayObtainStyledAttributes != null) {
                    typedArrayObtainStyledAttributes.recycle();
                }
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
            typedArrayObtainStyledAttributes = null;
        }
    }

    private void initClickListener() {
        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LogHelper.d(LongExposureView.TAG, "[onClick] mState " + LongExposureView.this.mState);
                if (LongExposureView.this.mState == LongExposureViewState.STATE_CAPTURE && LongExposureView.this.mListener != null) {
                    LongExposureView.this.mListener.onCaptureAbort();
                }
            }
        });
    }

    private void stopCountDown() {
        LogHelper.d(TAG, "[stopCountDown]");
        this.mCurrentProgress = 0.0f;
        invalidate();
    }

    private ValueAnimator getValValueAnimator(long j) {
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 100.0f);
        valueAnimatorOfFloat.setDuration(j);
        valueAnimatorOfFloat.setInterpolator(new LinearInterpolator());
        valueAnimatorOfFloat.setRepeatCount(0);
        return valueAnimatorOfFloat;
    }

    private void cancelAnimate() {
        if (this.mAnimator != null && this.mAnimator.isRunning()) {
            this.mAnimator.cancel();
        }
    }
}
