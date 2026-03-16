package com.android.musicfx.seekbar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import com.android.internal.R;

public abstract class AbsSeekBar extends ProgressBar {
    private float mDisabledAlpha;
    private boolean mIsDragging;
    boolean mIsUserSeekable;
    boolean mIsVertical;
    private int mKeyProgressIncrement;
    private int mScaledTouchSlop;
    private Drawable mThumb;
    private int mThumbOffset;
    private float mTouchDownX;
    private float mTouchDownY;
    float mTouchProgressOffset;

    public AbsSeekBar(Context context) {
        super(context);
        this.mIsUserSeekable = true;
        this.mIsVertical = false;
        this.mKeyProgressIncrement = 1;
    }

    public AbsSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mIsUserSeekable = true;
        this.mIsVertical = false;
        this.mKeyProgressIncrement = 1;
    }

    public AbsSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mIsUserSeekable = true;
        this.mIsVertical = false;
        this.mKeyProgressIncrement = 1;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SeekBar, defStyle, 0);
        Drawable thumb = a.getDrawable(0);
        setThumb(thumb);
        int thumbOffset = a.getDimensionPixelOffset(1, getThumbOffset());
        setThumbOffset(thumbOffset);
        a.recycle();
        TypedArray a2 = context.obtainStyledAttributes(attrs, R.styleable.Theme, 0, 0);
        this.mDisabledAlpha = a2.getFloat(3, 0.5f);
        a2.recycle();
        this.mScaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public void setThumb(Drawable thumb) {
        boolean needUpdate;
        if (this.mThumb != null && thumb != this.mThumb) {
            this.mThumb.setCallback(null);
            needUpdate = true;
        } else {
            needUpdate = false;
        }
        if (thumb != null) {
            thumb.setCallback(this);
            if (this.mIsVertical) {
                this.mThumbOffset = thumb.getIntrinsicHeight() / 2;
            } else {
                this.mThumbOffset = thumb.getIntrinsicWidth() / 2;
            }
            if (needUpdate && (thumb.getIntrinsicWidth() != this.mThumb.getIntrinsicWidth() || thumb.getIntrinsicHeight() != this.mThumb.getIntrinsicHeight())) {
                requestLayout();
            }
        }
        this.mThumb = thumb;
        invalidate();
        if (needUpdate) {
            updateThumbPos(getWidth(), getHeight());
            if (thumb.isStateful()) {
                int[] state = getDrawableState();
                thumb.setState(state);
            }
        }
    }

    public int getThumbOffset() {
        return this.mThumbOffset;
    }

    public void setThumbOffset(int thumbOffset) {
        this.mThumbOffset = thumbOffset;
        invalidate();
    }

    public void setKeyProgressIncrement(int increment) {
        if (increment < 0) {
            increment = -increment;
        }
        this.mKeyProgressIncrement = increment;
    }

    @Override
    public synchronized void setMax(int max) {
        super.setMax(max);
        if (this.mKeyProgressIncrement == 0 || getMax() / this.mKeyProgressIncrement > 20) {
            setKeyProgressIncrement(Math.max(1, Math.round(getMax() / 20.0f)));
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == this.mThumb || super.verifyDrawable(who);
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (this.mThumb != null) {
            this.mThumb.jumpToCurrentState();
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        Drawable progressDrawable = getProgressDrawable();
        if (progressDrawable != null) {
            progressDrawable.setAlpha(isEnabled() ? 255 : (int) (255.0f * this.mDisabledAlpha));
        }
        if (this.mThumb != null && this.mThumb.isStateful()) {
            int[] state = getDrawableState();
            this.mThumb.setState(state);
        }
    }

    @Override
    void onProgressRefresh(float scale, boolean fromUser) {
        super.onProgressRefresh(scale, fromUser);
        Drawable thumb = this.mThumb;
        if (thumb != null) {
            setThumbPos(getWidth(), getHeight(), thumb, scale, Integer.MIN_VALUE);
            invalidate();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        updateThumbPos(w, h);
    }

    private void updateThumbPos(int w, int h) {
        float scale;
        Drawable d = getCurrentDrawable();
        Drawable thumb = this.mThumb;
        if (this.mIsVertical) {
            int thumbWidth = thumb == null ? 0 : thumb.getIntrinsicWidth();
            int trackWidth = Math.min(this.mMaxWidth, (w - this.mPaddingLeft) - this.mPaddingRight);
            int max = getMax();
            scale = max > 0 ? getProgress() / max : 0.0f;
            if (thumbWidth > trackWidth) {
                if (thumb != null) {
                    setThumbPos(w, h, thumb, scale, 0);
                }
                int gapForCenteringTrack = (thumbWidth - trackWidth) / 2;
                if (d != null) {
                    d.setBounds(gapForCenteringTrack, 0, ((w - this.mPaddingRight) - gapForCenteringTrack) - this.mPaddingLeft, (h - this.mPaddingBottom) - this.mPaddingTop);
                    return;
                }
                return;
            }
            if (d != null) {
                d.setBounds(0, 0, (w - this.mPaddingRight) - this.mPaddingLeft, (h - this.mPaddingBottom) - this.mPaddingTop);
            }
            int gap = (trackWidth - thumbWidth) / 2;
            if (thumb != null) {
                setThumbPos(w, h, thumb, scale, gap);
                return;
            }
            return;
        }
        int thumbHeight = thumb == null ? 0 : thumb.getIntrinsicHeight();
        int trackHeight = Math.min(this.mMaxHeight, (h - this.mPaddingTop) - this.mPaddingBottom);
        int max2 = getMax();
        scale = max2 > 0 ? getProgress() / max2 : 0.0f;
        if (thumbHeight > trackHeight) {
            if (thumb != null) {
                setThumbPos(w, h, thumb, scale, 0);
            }
            int gapForCenteringTrack2 = (thumbHeight - trackHeight) / 2;
            if (d != null) {
                d.setBounds(0, gapForCenteringTrack2, (w - this.mPaddingRight) - this.mPaddingLeft, ((h - this.mPaddingBottom) - gapForCenteringTrack2) - this.mPaddingTop);
                return;
            }
            return;
        }
        if (d != null) {
            d.setBounds(0, 0, (w - this.mPaddingRight) - this.mPaddingLeft, (h - this.mPaddingBottom) - this.mPaddingTop);
        }
        int gap2 = (trackHeight - thumbHeight) / 2;
        if (thumb != null) {
            setThumbPos(w, h, thumb, scale, gap2);
        }
    }

    private void setThumbPos(int w, int h, Drawable thumb, float scale, int gap) {
        int available;
        int topBound;
        int bottomBound;
        int leftBound;
        int rightBound;
        int thumbWidth = thumb.getIntrinsicWidth();
        int thumbHeight = thumb.getIntrinsicHeight();
        if (this.mIsVertical) {
            available = ((h - this.mPaddingTop) - this.mPaddingBottom) - thumbHeight;
        } else {
            available = ((w - this.mPaddingLeft) - this.mPaddingRight) - thumbWidth;
        }
        int available2 = available + (this.mThumbOffset * 2);
        if (this.mIsVertical) {
            int thumbPos = (int) ((1.0f - scale) * available2);
            if (gap == Integer.MIN_VALUE) {
                Rect oldBounds = thumb.getBounds();
                leftBound = oldBounds.left;
                rightBound = oldBounds.right;
            } else {
                leftBound = gap;
                rightBound = gap + thumbWidth;
            }
            thumb.setBounds(leftBound, thumbPos, rightBound, thumbPos + thumbHeight);
            return;
        }
        int thumbPos2 = (int) (available2 * scale);
        if (gap == Integer.MIN_VALUE) {
            Rect oldBounds2 = thumb.getBounds();
            topBound = oldBounds2.top;
            bottomBound = oldBounds2.bottom;
        } else {
            topBound = gap;
            bottomBound = gap + thumbHeight;
        }
        thumb.setBounds(thumbPos2, topBound, thumbPos2 + thumbWidth, bottomBound);
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mThumb != null) {
            canvas.save();
            if (this.mIsVertical) {
                canvas.translate(this.mPaddingLeft, this.mPaddingTop - this.mThumbOffset);
                this.mThumb.draw(canvas);
                canvas.restore();
            } else {
                canvas.translate(this.mPaddingLeft - this.mThumbOffset, this.mPaddingTop);
                this.mThumb.draw(canvas);
                canvas.restore();
            }
        }
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        synchronized (this) {
            Drawable d = getCurrentDrawable();
            int thumbHeight = this.mThumb != null ? this.mThumb.getIntrinsicHeight() : 0;
            int dw = 0;
            int dh = 0;
            if (d != null) {
                dw = Math.max(this.mMinWidth, Math.min(this.mMaxWidth, d.getIntrinsicWidth()));
                int dh2 = Math.max(this.mMinHeight, Math.min(this.mMaxHeight, d.getIntrinsicHeight()));
                dh = Math.max(thumbHeight, dh2);
            }
            setMeasuredDimension(resolveSizeAndState(dw + this.mPaddingLeft + this.mPaddingRight, widthMeasureSpec, 0), resolveSizeAndState(dh + this.mPaddingTop + this.mPaddingBottom, heightMeasureSpec, 0));
            if (getMeasuredHeight() > getMeasuredWidth()) {
                this.mIsVertical = true;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!this.mIsUserSeekable || !isEnabled()) {
            return false;
        }
        switch (event.getAction()) {
            case 0:
                if (isInScrollingContainer()) {
                    this.mTouchDownX = event.getX();
                    this.mTouchDownY = event.getY();
                } else {
                    setPressed(true);
                    if (this.mThumb != null) {
                        invalidate(this.mThumb.getBounds());
                    }
                    onStartTrackingTouch();
                    trackTouchEvent(event);
                    attemptClaimDrag();
                }
                break;
            case 1:
                if (this.mIsDragging) {
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                    setPressed(false);
                } else {
                    onStartTrackingTouch();
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                }
                invalidate();
                break;
            case 2:
                if (this.mIsDragging) {
                    trackTouchEvent(event);
                } else {
                    float x = event.getX();
                    float y = event.getX();
                    if (Math.abs(this.mIsVertical ? y - this.mTouchDownY : x - this.mTouchDownX) > this.mScaledTouchSlop) {
                        setPressed(true);
                        if (this.mThumb != null) {
                            invalidate(this.mThumb.getBounds());
                        }
                        onStartTrackingTouch();
                        trackTouchEvent(event);
                        attemptClaimDrag();
                    }
                }
                break;
            case 3:
                if (this.mIsDragging) {
                    onStopTrackingTouch();
                    setPressed(false);
                }
                invalidate();
                break;
        }
        return true;
    }

    private void trackTouchEvent(MotionEvent event) {
        float scale;
        float progress;
        float scale2;
        float progress2 = 0.0f;
        if (this.mIsVertical) {
            int height = getHeight();
            int available = (height - this.mPaddingTop) - this.mPaddingBottom;
            int y = (int) event.getY();
            if (y < this.mPaddingTop) {
                scale2 = 1.0f;
            } else if (y > height - this.mPaddingBottom) {
                scale2 = 0.0f;
            } else {
                scale2 = 1.0f - ((y - this.mPaddingTop) / available);
                progress2 = this.mTouchProgressOffset;
            }
            int max = getMax();
            progress = progress2 + (max * scale2);
        } else {
            int width = getWidth();
            int available2 = (width - this.mPaddingLeft) - this.mPaddingRight;
            int x = (int) event.getX();
            if (x < this.mPaddingLeft) {
                scale = 0.0f;
            } else if (x > width - this.mPaddingRight) {
                scale = 1.0f;
            } else {
                scale = (x - this.mPaddingLeft) / available2;
                progress2 = this.mTouchProgressOffset;
            }
            int max2 = getMax();
            progress = progress2 + (max2 * scale);
        }
        setProgress((int) progress, true);
    }

    private void attemptClaimDrag() {
        if (this.mParent != null) {
            this.mParent.requestDisallowInterceptTouchEvent(true);
        }
    }

    void onStartTrackingTouch() {
        this.mIsDragging = true;
    }

    void onStopTrackingTouch() {
        this.mIsDragging = false;
    }

    void onKeyChange() {
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isEnabled()) {
            int progress = getProgress();
            if ((keyCode == 21 && !this.mIsVertical) || (keyCode == 20 && this.mIsVertical)) {
                if (progress > 0) {
                    setProgress(progress - this.mKeyProgressIncrement, true);
                    onKeyChange();
                    return true;
                }
            } else if (((keyCode == 22 && !this.mIsVertical) || (keyCode == 19 && this.mIsVertical)) && progress < getMax()) {
                setProgress(this.mKeyProgressIncrement + progress, true);
                onKeyChange();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
