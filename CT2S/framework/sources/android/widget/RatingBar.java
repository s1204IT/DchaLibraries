package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.Shape;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.android.internal.R;

public class RatingBar extends AbsSeekBar {
    private int mNumStars;
    private OnRatingBarChangeListener mOnRatingBarChangeListener;
    private int mProgressOnStartTracking;

    public interface OnRatingBarChangeListener {
        void onRatingChanged(RatingBar ratingBar, float f, boolean z);
    }

    public RatingBar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RatingBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mNumStars = 5;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RatingBar, defStyleAttr, defStyleRes);
        int numStars = a.getInt(0, this.mNumStars);
        setIsIndicator(a.getBoolean(3, this.mIsUserSeekable ? false : true));
        float rating = a.getFloat(1, -1.0f);
        float stepSize = a.getFloat(2, -1.0f);
        a.recycle();
        if (numStars > 0 && numStars != this.mNumStars) {
            setNumStars(numStars);
        }
        if (stepSize >= 0.0f) {
            setStepSize(stepSize);
        } else {
            setStepSize(0.5f);
        }
        if (rating >= 0.0f) {
            setRating(rating);
        }
        this.mTouchProgressOffset = 1.1f;
    }

    public RatingBar(Context context, AttributeSet attrs) {
        this(context, attrs, 16842876);
    }

    public RatingBar(Context context) {
        this(context, null);
    }

    public void setOnRatingBarChangeListener(OnRatingBarChangeListener listener) {
        this.mOnRatingBarChangeListener = listener;
    }

    public OnRatingBarChangeListener getOnRatingBarChangeListener() {
        return this.mOnRatingBarChangeListener;
    }

    public void setIsIndicator(boolean isIndicator) {
        this.mIsUserSeekable = !isIndicator;
        setFocusable(isIndicator ? false : true);
    }

    public boolean isIndicator() {
        return !this.mIsUserSeekable;
    }

    public void setNumStars(int numStars) {
        if (numStars > 0) {
            this.mNumStars = numStars;
            requestLayout();
        }
    }

    public int getNumStars() {
        return this.mNumStars;
    }

    public void setRating(float rating) {
        setProgress(Math.round(getProgressPerStar() * rating));
    }

    public float getRating() {
        return getProgress() / getProgressPerStar();
    }

    public void setStepSize(float stepSize) {
        if (stepSize > 0.0f) {
            float newMax = this.mNumStars / stepSize;
            int newProgress = (int) ((newMax / getMax()) * getProgress());
            setMax((int) newMax);
            setProgress(newProgress);
        }
    }

    public float getStepSize() {
        return getNumStars() / getMax();
    }

    private float getProgressPerStar() {
        if (this.mNumStars > 0) {
            return (1.0f * getMax()) / this.mNumStars;
        }
        return 1.0f;
    }

    @Override
    Shape getDrawableShape() {
        return new RectShape();
    }

    @Override
    void onProgressRefresh(float scale, boolean fromUser) {
        super.onProgressRefresh(scale, fromUser);
        updateSecondaryProgress(getProgress());
        if (!fromUser) {
            dispatchRatingChange(false);
        }
    }

    private void updateSecondaryProgress(int progress) {
        float ratio = getProgressPerStar();
        if (ratio > 0.0f) {
            float progressInStars = progress / ratio;
            int secondaryProgress = (int) (Math.ceil(progressInStars) * ((double) ratio));
            setSecondaryProgress(secondaryProgress);
        }
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (this.mSampleTile != null) {
            int width = this.mSampleTile.getWidth() * this.mNumStars;
            setMeasuredDimension(resolveSizeAndState(width, widthMeasureSpec, 0), getMeasuredHeight());
        }
    }

    @Override
    void onStartTrackingTouch() {
        this.mProgressOnStartTracking = getProgress();
        super.onStartTrackingTouch();
    }

    @Override
    void onStopTrackingTouch() {
        super.onStopTrackingTouch();
        if (getProgress() != this.mProgressOnStartTracking) {
            dispatchRatingChange(true);
        }
    }

    @Override
    void onKeyChange() {
        super.onKeyChange();
        dispatchRatingChange(true);
    }

    void dispatchRatingChange(boolean fromUser) {
        if (this.mOnRatingBarChangeListener != null) {
            this.mOnRatingBarChangeListener.onRatingChanged(this, getRating(), fromUser);
        }
    }

    @Override
    public synchronized void setMax(int max) {
        if (max > 0) {
            super.setMax(max);
        }
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(RatingBar.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(RatingBar.class.getName());
    }
}
