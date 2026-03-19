package android.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Insets;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityNodeInfo;
import com.android.internal.R;

public abstract class AbsSeekBar extends ProgressBar {
    private static final int NO_ALPHA = 255;
    private float mDisabledAlpha;
    private boolean mHasThumbTint;
    private boolean mHasThumbTintMode;
    private boolean mHasTickMarkTint;
    private boolean mHasTickMarkTintMode;
    private boolean mIsDragging;
    boolean mIsUserSeekable;
    private int mKeyProgressIncrement;
    private int mScaledTouchSlop;
    private boolean mSplitTrack;
    private final Rect mTempRect;
    private Drawable mThumb;
    private int mThumbOffset;
    private ColorStateList mThumbTintList;
    private PorterDuff.Mode mThumbTintMode;
    private Drawable mTickMark;
    private ColorStateList mTickMarkTintList;
    private PorterDuff.Mode mTickMarkTintMode;
    private float mTouchDownX;
    float mTouchProgressOffset;

    public AbsSeekBar(Context context) {
        super(context);
        this.mTempRect = new Rect();
        this.mThumbTintList = null;
        this.mThumbTintMode = null;
        this.mHasThumbTint = false;
        this.mHasThumbTintMode = false;
        this.mTickMarkTintList = null;
        this.mTickMarkTintMode = null;
        this.mHasTickMarkTint = false;
        this.mHasTickMarkTintMode = false;
        this.mIsUserSeekable = true;
        this.mKeyProgressIncrement = 1;
    }

    public AbsSeekBar(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mTempRect = new Rect();
        this.mThumbTintList = null;
        this.mThumbTintMode = null;
        this.mHasThumbTint = false;
        this.mHasThumbTintMode = false;
        this.mTickMarkTintList = null;
        this.mTickMarkTintMode = null;
        this.mHasTickMarkTint = false;
        this.mHasTickMarkTintMode = false;
        this.mIsUserSeekable = true;
        this.mKeyProgressIncrement = 1;
    }

    public AbsSeekBar(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public AbsSeekBar(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mTempRect = new Rect();
        this.mThumbTintList = null;
        this.mThumbTintMode = null;
        this.mHasThumbTint = false;
        this.mHasThumbTintMode = false;
        this.mTickMarkTintList = null;
        this.mTickMarkTintMode = null;
        this.mHasTickMarkTint = false;
        this.mHasTickMarkTintMode = false;
        this.mIsUserSeekable = true;
        this.mKeyProgressIncrement = 1;
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.SeekBar, i, i2);
        setThumb(typedArrayObtainStyledAttributes.getDrawable(0));
        if (typedArrayObtainStyledAttributes.hasValue(4)) {
            this.mThumbTintMode = Drawable.parseTintMode(typedArrayObtainStyledAttributes.getInt(4, -1), this.mThumbTintMode);
            this.mHasThumbTintMode = true;
        }
        if (typedArrayObtainStyledAttributes.hasValue(3)) {
            this.mThumbTintList = typedArrayObtainStyledAttributes.getColorStateList(3);
            this.mHasThumbTint = true;
        }
        setTickMark(typedArrayObtainStyledAttributes.getDrawable(5));
        if (typedArrayObtainStyledAttributes.hasValue(7)) {
            this.mTickMarkTintMode = Drawable.parseTintMode(typedArrayObtainStyledAttributes.getInt(7, -1), this.mTickMarkTintMode);
            this.mHasTickMarkTintMode = true;
        }
        if (typedArrayObtainStyledAttributes.hasValue(6)) {
            this.mTickMarkTintList = typedArrayObtainStyledAttributes.getColorStateList(6);
            this.mHasTickMarkTint = true;
        }
        this.mSplitTrack = typedArrayObtainStyledAttributes.getBoolean(2, false);
        setThumbOffset(typedArrayObtainStyledAttributes.getDimensionPixelOffset(1, getThumbOffset()));
        boolean z = typedArrayObtainStyledAttributes.getBoolean(8, true);
        typedArrayObtainStyledAttributes.recycle();
        if (z) {
            TypedArray typedArrayObtainStyledAttributes2 = context.obtainStyledAttributes(attributeSet, R.styleable.Theme, 0, 0);
            this.mDisabledAlpha = typedArrayObtainStyledAttributes2.getFloat(3, 0.5f);
            typedArrayObtainStyledAttributes2.recycle();
        } else {
            this.mDisabledAlpha = 1.0f;
        }
        applyThumbTint();
        applyTickMarkTint();
        this.mScaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public void setThumb(Drawable drawable) {
        boolean z;
        if (this.mThumb != null && drawable != this.mThumb) {
            this.mThumb.setCallback(null);
            z = true;
        } else {
            z = false;
        }
        if (drawable != null) {
            drawable.setCallback(this);
            if (canResolveLayoutDirection()) {
                drawable.setLayoutDirection(getLayoutDirection());
            }
            this.mThumbOffset = drawable.getIntrinsicWidth() / 2;
            if (z && (drawable.getIntrinsicWidth() != this.mThumb.getIntrinsicWidth() || drawable.getIntrinsicHeight() != this.mThumb.getIntrinsicHeight())) {
                requestLayout();
            }
        }
        this.mThumb = drawable;
        applyThumbTint();
        invalidate();
        if (z) {
            updateThumbAndTrackPos(getWidth(), getHeight());
            if (drawable != null && drawable.isStateful()) {
                drawable.setState(getDrawableState());
            }
        }
    }

    public Drawable getThumb() {
        return this.mThumb;
    }

    public void setThumbTintList(ColorStateList colorStateList) {
        this.mThumbTintList = colorStateList;
        this.mHasThumbTint = true;
        applyThumbTint();
    }

    public ColorStateList getThumbTintList() {
        return this.mThumbTintList;
    }

    public void setThumbTintMode(PorterDuff.Mode mode) {
        this.mThumbTintMode = mode;
        this.mHasThumbTintMode = true;
        applyThumbTint();
    }

    public PorterDuff.Mode getThumbTintMode() {
        return this.mThumbTintMode;
    }

    private void applyThumbTint() {
        if (this.mThumb != null) {
            if (this.mHasThumbTint || this.mHasThumbTintMode) {
                this.mThumb = this.mThumb.mutate();
                if (this.mHasThumbTint) {
                    this.mThumb.setTintList(this.mThumbTintList);
                }
                if (this.mHasThumbTintMode) {
                    this.mThumb.setTintMode(this.mThumbTintMode);
                }
                if (this.mThumb.isStateful()) {
                    this.mThumb.setState(getDrawableState());
                }
            }
        }
    }

    public int getThumbOffset() {
        return this.mThumbOffset;
    }

    public void setThumbOffset(int i) {
        this.mThumbOffset = i;
        invalidate();
    }

    public void setSplitTrack(boolean z) {
        this.mSplitTrack = z;
        invalidate();
    }

    public boolean getSplitTrack() {
        return this.mSplitTrack;
    }

    public void setTickMark(Drawable drawable) {
        if (this.mTickMark != null) {
            this.mTickMark.setCallback(null);
        }
        this.mTickMark = drawable;
        if (drawable != null) {
            drawable.setCallback(this);
            drawable.setLayoutDirection(getLayoutDirection());
            if (drawable.isStateful()) {
                drawable.setState(getDrawableState());
            }
            applyTickMarkTint();
        }
        invalidate();
    }

    public Drawable getTickMark() {
        return this.mTickMark;
    }

    public void setTickMarkTintList(ColorStateList colorStateList) {
        this.mTickMarkTintList = colorStateList;
        this.mHasTickMarkTint = true;
        applyTickMarkTint();
    }

    public ColorStateList getTickMarkTintList() {
        return this.mTickMarkTintList;
    }

    public void setTickMarkTintMode(PorterDuff.Mode mode) {
        this.mTickMarkTintMode = mode;
        this.mHasTickMarkTintMode = true;
        applyTickMarkTint();
    }

    public PorterDuff.Mode getTickMarkTintMode() {
        return this.mTickMarkTintMode;
    }

    private void applyTickMarkTint() {
        if (this.mTickMark != null) {
            if (this.mHasTickMarkTint || this.mHasTickMarkTintMode) {
                this.mTickMark = this.mTickMark.mutate();
                if (this.mHasTickMarkTint) {
                    this.mTickMark.setTintList(this.mTickMarkTintList);
                }
                if (this.mHasTickMarkTintMode) {
                    this.mTickMark.setTintMode(this.mTickMarkTintMode);
                }
                if (this.mTickMark.isStateful()) {
                    this.mTickMark.setState(getDrawableState());
                }
            }
        }
    }

    public void setKeyProgressIncrement(int i) {
        if (i < 0) {
            i = -i;
        }
        this.mKeyProgressIncrement = i;
    }

    public int getKeyProgressIncrement() {
        return this.mKeyProgressIncrement;
    }

    @Override
    public synchronized void setMin(int i) {
        super.setMin(i);
        int max = getMax() - getMin();
        if (this.mKeyProgressIncrement == 0 || max / this.mKeyProgressIncrement > 20) {
            setKeyProgressIncrement(Math.max(1, Math.round(max / 20.0f)));
        }
    }

    @Override
    public synchronized void setMax(int i) {
        super.setMax(i);
        int max = getMax() - getMin();
        if (this.mKeyProgressIncrement == 0 || max / this.mKeyProgressIncrement > 20) {
            setKeyProgressIncrement(Math.max(1, Math.round(max / 20.0f)));
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable drawable) {
        return drawable == this.mThumb || drawable == this.mTickMark || super.verifyDrawable(drawable);
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (this.mThumb != null) {
            this.mThumb.jumpToCurrentState();
        }
        if (this.mTickMark != null) {
            this.mTickMark.jumpToCurrentState();
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        Drawable progressDrawable = getProgressDrawable();
        if (progressDrawable != null && this.mDisabledAlpha < 1.0f) {
            progressDrawable.setAlpha(isEnabled() ? 255 : (int) (255.0f * this.mDisabledAlpha));
        }
        Drawable drawable = this.mThumb;
        if (drawable != null && drawable.isStateful() && drawable.setState(getDrawableState())) {
            invalidateDrawable(drawable);
        }
        Drawable drawable2 = this.mTickMark;
        if (drawable2 != null && drawable2.isStateful() && drawable2.setState(getDrawableState())) {
            invalidateDrawable(drawable2);
        }
    }

    @Override
    public void drawableHotspotChanged(float f, float f2) {
        super.drawableHotspotChanged(f, f2);
        if (this.mThumb != null) {
            this.mThumb.setHotspot(f, f2);
        }
    }

    @Override
    void onVisualProgressChanged(int i, float f) {
        Drawable drawable;
        super.onVisualProgressChanged(i, f);
        if (i == 16908301 && (drawable = this.mThumb) != null) {
            setThumbPos(getWidth(), drawable, f, Integer.MIN_VALUE);
            invalidate();
        }
    }

    @Override
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        updateThumbAndTrackPos(i, i2);
    }

    private void updateThumbAndTrackPos(int i, int i2) {
        int intrinsicHeight;
        int i3;
        int i4;
        int i5 = (i2 - this.mPaddingTop) - this.mPaddingBottom;
        Drawable currentDrawable = getCurrentDrawable();
        Drawable drawable = this.mThumb;
        int iMin = Math.min(this.mMaxHeight, i5);
        if (drawable != null) {
            intrinsicHeight = drawable.getIntrinsicHeight();
        } else {
            intrinsicHeight = 0;
        }
        if (intrinsicHeight > iMin) {
            int i6 = (i5 - intrinsicHeight) / 2;
            int i7 = ((intrinsicHeight - iMin) / 2) + i6;
            i4 = i6;
            i3 = i7;
        } else {
            i3 = (i5 - iMin) / 2;
            i4 = ((iMin - intrinsicHeight) / 2) + i3;
        }
        if (currentDrawable != null) {
            currentDrawable.setBounds(0, i3, (i - this.mPaddingRight) - this.mPaddingLeft, iMin + i3);
        }
        if (drawable != null) {
            setThumbPos(i, drawable, getScale(), i4);
        }
    }

    private float getScale() {
        int max = getMax() - getMin();
        if (max > 0) {
            return (getProgress() - r0) / max;
        }
        return 0.0f;
    }

    private void setThumbPos(int i, Drawable drawable, float f, int i2) {
        int i3;
        int i4 = (i - this.mPaddingLeft) - this.mPaddingRight;
        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();
        int i5 = (i4 - intrinsicWidth) + (this.mThumbOffset * 2);
        int i6 = (int) ((f * i5) + 0.5f);
        if (i2 == Integer.MIN_VALUE) {
            Rect bounds = drawable.getBounds();
            int i7 = bounds.top;
            i3 = bounds.bottom;
            i2 = i7;
        } else {
            i3 = intrinsicHeight + i2;
        }
        if (isLayoutRtl() && this.mMirrorForRtl) {
            i6 = i5 - i6;
        }
        int i8 = intrinsicWidth + i6;
        Drawable background = getBackground();
        if (background != null) {
            int i9 = this.mPaddingLeft - this.mThumbOffset;
            int i10 = this.mPaddingTop;
            background.setHotspotBounds(i6 + i9, i2 + i10, i9 + i8, i10 + i3);
        }
        drawable.setBounds(i6, i2, i8, i3);
    }

    @Override
    public void onResolveDrawables(int i) {
        super.onResolveDrawables(i);
        if (this.mThumb != null) {
            this.mThumb.setLayoutDirection(i);
        }
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawThumb(canvas);
    }

    @Override
    void drawTrack(Canvas canvas) {
        Drawable drawable = this.mThumb;
        if (drawable != null && this.mSplitTrack) {
            Insets opticalInsets = drawable.getOpticalInsets();
            Rect rect = this.mTempRect;
            drawable.copyBounds(rect);
            rect.offset(this.mPaddingLeft - this.mThumbOffset, this.mPaddingTop);
            rect.left += opticalInsets.left;
            rect.right -= opticalInsets.right;
            int iSave = canvas.save();
            canvas.clipRect(rect, Region.Op.DIFFERENCE);
            super.drawTrack(canvas);
            drawTickMarks(canvas);
            canvas.restoreToCount(iSave);
            return;
        }
        super.drawTrack(canvas);
        drawTickMarks(canvas);
    }

    protected void drawTickMarks(Canvas canvas) {
        if (this.mTickMark != null) {
            int max = getMax() - getMin();
            if (max > 1) {
                int intrinsicWidth = this.mTickMark.getIntrinsicWidth();
                int intrinsicHeight = this.mTickMark.getIntrinsicHeight();
                int i = intrinsicWidth >= 0 ? intrinsicWidth / 2 : 1;
                int i2 = intrinsicHeight >= 0 ? intrinsicHeight / 2 : 1;
                this.mTickMark.setBounds(-i, -i2, i, i2);
                float width = ((getWidth() - this.mPaddingLeft) - this.mPaddingRight) / max;
                int iSave = canvas.save();
                canvas.translate(this.mPaddingLeft, getHeight() / 2);
                for (int i3 = 0; i3 <= max; i3++) {
                    this.mTickMark.draw(canvas);
                    canvas.translate(width, 0.0f);
                }
                canvas.restoreToCount(iSave);
            }
        }
    }

    void drawThumb(Canvas canvas) {
        if (this.mThumb != null) {
            int iSave = canvas.save();
            canvas.translate(this.mPaddingLeft - this.mThumbOffset, this.mPaddingTop);
            this.mThumb.draw(canvas);
            canvas.restoreToCount(iSave);
        }
    }

    @Override
    protected synchronized void onMeasure(int i, int i2) {
        int intrinsicHeight;
        int iMax;
        int iMax2;
        Drawable currentDrawable = getCurrentDrawable();
        if (this.mThumb != null) {
            intrinsicHeight = this.mThumb.getIntrinsicHeight();
        } else {
            intrinsicHeight = 0;
        }
        if (currentDrawable != null) {
            iMax2 = Math.max(this.mMinWidth, Math.min(this.mMaxWidth, currentDrawable.getIntrinsicWidth()));
            iMax = Math.max(intrinsicHeight, Math.max(this.mMinHeight, Math.min(this.mMaxHeight, currentDrawable.getIntrinsicHeight())));
        } else {
            iMax = 0;
            iMax2 = 0;
        }
        setMeasuredDimension(resolveSizeAndState(iMax2 + this.mPaddingLeft + this.mPaddingRight, i, 0), resolveSizeAndState(iMax + this.mPaddingTop + this.mPaddingBottom, i2, 0));
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (!this.mIsUserSeekable || !isEnabled()) {
            return false;
        }
        switch (motionEvent.getAction()) {
            case 0:
                if (isInScrollingContainer()) {
                    this.mTouchDownX = motionEvent.getX();
                    return true;
                }
                startDrag(motionEvent);
                return true;
            case 1:
                if (this.mIsDragging) {
                    trackTouchEvent(motionEvent);
                    onStopTrackingTouch();
                    setPressed(false);
                } else {
                    onStartTrackingTouch();
                    trackTouchEvent(motionEvent);
                    onStopTrackingTouch();
                }
                invalidate();
                return true;
            case 2:
                if (this.mIsDragging) {
                    trackTouchEvent(motionEvent);
                    return true;
                }
                if (Math.abs(motionEvent.getX() - this.mTouchDownX) > this.mScaledTouchSlop) {
                    startDrag(motionEvent);
                    return true;
                }
                return true;
            case 3:
                if (this.mIsDragging) {
                    onStopTrackingTouch();
                    setPressed(false);
                }
                invalidate();
                return true;
            default:
                return true;
        }
    }

    private void startDrag(MotionEvent motionEvent) {
        setPressed(true);
        if (this.mThumb != null) {
            invalidate(this.mThumb.getBounds());
        }
        onStartTrackingTouch();
        trackTouchEvent(motionEvent);
        attemptClaimDrag();
    }

    private void setHotspot(float f, float f2) {
        Drawable background = getBackground();
        if (background != null) {
            background.setHotspot(f, f2);
        }
    }

    private void trackTouchEvent(MotionEvent motionEvent) {
        int iRound = Math.round(motionEvent.getX());
        int iRound2 = Math.round(motionEvent.getY());
        int width = getWidth();
        int i = (width - this.mPaddingLeft) - this.mPaddingRight;
        float f = 1.0f;
        float f2 = 0.0f;
        if (isLayoutRtl() && this.mMirrorForRtl) {
            if (iRound <= width - this.mPaddingRight) {
                if (iRound >= this.mPaddingLeft) {
                    f = ((i - iRound) + this.mPaddingLeft) / i;
                    f2 = this.mTouchProgressOffset;
                }
            }
        } else if (iRound >= this.mPaddingLeft) {
            if (iRound <= width - this.mPaddingRight) {
                f = (iRound - this.mPaddingLeft) / i;
                f2 = this.mTouchProgressOffset;
            }
        } else {
            f = 0.0f;
        }
        setHotspot(iRound, iRound2);
        setProgressInternal(Math.round(f2 + (f * (getMax() - getMin())) + getMin()), true, false);
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
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (isEnabled()) {
            int i2 = this.mKeyProgressIncrement;
            switch (i) {
                case 21:
                case 69:
                    i2 = -i2;
                case 22:
                case 70:
                case 81:
                    if (isLayoutRtl()) {
                        i2 = -i2;
                    }
                    if (setProgressInternal(getProgress() + i2, true, true)) {
                        onKeyChange();
                        return true;
                    }
                default:
                    return super.onKeyDown(i, keyEvent);
            }
        }
        return super.onKeyDown(i, keyEvent);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return AbsSeekBar.class.getName();
    }

    @Override
    public void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfoInternal(accessibilityNodeInfo);
        if (isEnabled()) {
            int progress = getProgress();
            if (progress > getMin()) {
                accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
            }
            if (progress < getMax()) {
                accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
            }
        }
    }

    @Override
    public boolean performAccessibilityActionInternal(int i, Bundle bundle) {
        if (super.performAccessibilityActionInternal(i, bundle)) {
            return true;
        }
        if (!isEnabled()) {
            return false;
        }
        if (i != 4096 && i != 8192) {
            if (i == 16908349 && canUserSetProgress() && bundle != null && bundle.containsKey(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE)) {
                return setProgressInternal((int) bundle.getFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE), true, true);
            }
            return false;
        }
        if (!canUserSetProgress()) {
            return false;
        }
        int iMax = Math.max(1, Math.round((getMax() - getMin()) / 20.0f));
        if (i == 8192) {
            iMax = -iMax;
        }
        if (!setProgressInternal(getProgress() + iMax, true, true)) {
            return false;
        }
        onKeyChange();
        return true;
    }

    boolean canUserSetProgress() {
        return !isIndeterminate() && isEnabled();
    }

    @Override
    public void onRtlPropertiesChanged(int i) {
        super.onRtlPropertiesChanged(i);
        Drawable drawable = this.mThumb;
        if (drawable != null) {
            setThumbPos(getWidth(), drawable, getScale(), Integer.MIN_VALUE);
            invalidate();
        }
    }
}
