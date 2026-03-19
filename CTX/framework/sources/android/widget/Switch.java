package android.widget;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Insets;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.AllCapsTransformationMethod;
import android.text.method.TransformationMethod2;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.util.MathUtils;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.ViewStructure;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.android.internal.R;

public class Switch extends CompoundButton {
    private static final int MONOSPACE = 3;
    private static final int SANS = 1;
    private static final int SERIF = 2;
    private static final int THUMB_ANIMATION_DURATION = 250;
    private static final int TOUCH_MODE_DOWN = 1;
    private static final int TOUCH_MODE_DRAGGING = 2;
    private static final int TOUCH_MODE_IDLE = 0;
    private boolean mHasThumbTint;
    private boolean mHasThumbTintMode;
    private boolean mHasTrackTint;
    private boolean mHasTrackTintMode;
    private int mMinFlingVelocity;
    private Layout mOffLayout;
    private Layout mOnLayout;
    private ObjectAnimator mPositionAnimator;
    private boolean mShowText;
    private boolean mSplitTrack;
    private int mSwitchBottom;
    private int mSwitchHeight;
    private int mSwitchLeft;
    private int mSwitchMinWidth;
    private int mSwitchPadding;
    private int mSwitchRight;
    private int mSwitchTop;
    private TransformationMethod2 mSwitchTransformationMethod;
    private int mSwitchWidth;
    private final Rect mTempRect;
    private ColorStateList mTextColors;
    private CharSequence mTextOff;
    private CharSequence mTextOn;
    private TextPaint mTextPaint;
    private Drawable mThumbDrawable;
    private float mThumbPosition;
    private int mThumbTextPadding;
    private ColorStateList mThumbTintList;
    private PorterDuff.Mode mThumbTintMode;
    private int mThumbWidth;
    private int mTouchMode;
    private int mTouchSlop;
    private float mTouchX;
    private float mTouchY;
    private Drawable mTrackDrawable;
    private ColorStateList mTrackTintList;
    private PorterDuff.Mode mTrackTintMode;
    private boolean mUseFallbackLineSpacing;
    private VelocityTracker mVelocityTracker;
    private static final int[] CHECKED_STATE_SET = {16842912};
    private static final FloatProperty<Switch> THUMB_POS = new FloatProperty<Switch>("thumbPos") {
        @Override
        public Float get(Switch r1) {
            return Float.valueOf(r1.mThumbPosition);
        }

        @Override
        public void setValue(Switch r1, float f) {
            r1.setThumbPosition(f);
        }
    };

    public Switch(Context context) {
        this(context, null);
    }

    public Switch(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 16843839);
    }

    public Switch(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public Switch(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mThumbTintList = null;
        this.mThumbTintMode = null;
        this.mHasThumbTint = false;
        this.mHasThumbTintMode = false;
        this.mTrackTintList = null;
        this.mTrackTintMode = null;
        this.mHasTrackTint = false;
        this.mHasTrackTintMode = false;
        this.mVelocityTracker = VelocityTracker.obtain();
        this.mTempRect = new Rect();
        this.mTextPaint = new TextPaint(1);
        Resources resources = getResources();
        this.mTextPaint.density = resources.getDisplayMetrics().density;
        this.mTextPaint.setCompatibilityScaling(resources.getCompatibilityInfo().applicationScale);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.Switch, i, i2);
        this.mThumbDrawable = typedArrayObtainStyledAttributes.getDrawable(2);
        if (this.mThumbDrawable != null) {
            this.mThumbDrawable.setCallback(this);
        }
        this.mTrackDrawable = typedArrayObtainStyledAttributes.getDrawable(4);
        if (this.mTrackDrawable != null) {
            this.mTrackDrawable.setCallback(this);
        }
        this.mTextOn = typedArrayObtainStyledAttributes.getText(0);
        this.mTextOff = typedArrayObtainStyledAttributes.getText(1);
        this.mShowText = typedArrayObtainStyledAttributes.getBoolean(11, true);
        this.mThumbTextPadding = typedArrayObtainStyledAttributes.getDimensionPixelSize(7, 0);
        this.mSwitchMinWidth = typedArrayObtainStyledAttributes.getDimensionPixelSize(5, 0);
        this.mSwitchPadding = typedArrayObtainStyledAttributes.getDimensionPixelSize(6, 0);
        this.mSplitTrack = typedArrayObtainStyledAttributes.getBoolean(8, false);
        this.mUseFallbackLineSpacing = context.getApplicationInfo().targetSdkVersion >= 28;
        ColorStateList colorStateList = typedArrayObtainStyledAttributes.getColorStateList(9);
        if (colorStateList != null) {
            this.mThumbTintList = colorStateList;
            this.mHasThumbTint = true;
        }
        PorterDuff.Mode tintMode = Drawable.parseTintMode(typedArrayObtainStyledAttributes.getInt(10, -1), null);
        if (this.mThumbTintMode != tintMode) {
            this.mThumbTintMode = tintMode;
            this.mHasThumbTintMode = true;
        }
        if (this.mHasThumbTint || this.mHasThumbTintMode) {
            applyThumbTint();
        }
        ColorStateList colorStateList2 = typedArrayObtainStyledAttributes.getColorStateList(12);
        if (colorStateList2 != null) {
            this.mTrackTintList = colorStateList2;
            this.mHasTrackTint = true;
        }
        PorterDuff.Mode tintMode2 = Drawable.parseTintMode(typedArrayObtainStyledAttributes.getInt(13, -1), null);
        if (this.mTrackTintMode != tintMode2) {
            this.mTrackTintMode = tintMode2;
            this.mHasTrackTintMode = true;
        }
        if (this.mHasTrackTint || this.mHasTrackTintMode) {
            applyTrackTint();
        }
        int resourceId = typedArrayObtainStyledAttributes.getResourceId(3, 0);
        if (resourceId != 0) {
            setSwitchTextAppearance(context, resourceId);
        }
        typedArrayObtainStyledAttributes.recycle();
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        this.mTouchSlop = viewConfiguration.getScaledTouchSlop();
        this.mMinFlingVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        refreshDrawableState();
        setChecked(isChecked());
    }

    public void setSwitchTextAppearance(Context context, int i) {
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(i, R.styleable.TextAppearance);
        ColorStateList colorStateList = typedArrayObtainStyledAttributes.getColorStateList(3);
        if (colorStateList != null) {
            this.mTextColors = colorStateList;
        } else {
            this.mTextColors = getTextColors();
        }
        int dimensionPixelSize = typedArrayObtainStyledAttributes.getDimensionPixelSize(0, 0);
        if (dimensionPixelSize != 0) {
            float f = dimensionPixelSize;
            if (f != this.mTextPaint.getTextSize()) {
                this.mTextPaint.setTextSize(f);
                requestLayout();
            }
        }
        setSwitchTypefaceByIndex(typedArrayObtainStyledAttributes.getInt(1, -1), typedArrayObtainStyledAttributes.getInt(2, -1));
        if (typedArrayObtainStyledAttributes.getBoolean(11, false)) {
            this.mSwitchTransformationMethod = new AllCapsTransformationMethod(getContext());
            this.mSwitchTransformationMethod.setLengthChangesAllowed(true);
        } else {
            this.mSwitchTransformationMethod = null;
        }
        typedArrayObtainStyledAttributes.recycle();
    }

    private void setSwitchTypefaceByIndex(int i, int i2) {
        Typeface typeface;
        switch (i) {
            case 1:
                typeface = Typeface.SANS_SERIF;
                break;
            case 2:
                typeface = Typeface.SERIF;
                break;
            case 3:
                typeface = Typeface.MONOSPACE;
                break;
            default:
                typeface = null;
                break;
        }
        setSwitchTypeface(typeface, i2);
    }

    public void setSwitchTypeface(Typeface typeface, int i) {
        Typeface typefaceCreate;
        if (i > 0) {
            if (typeface == null) {
                typefaceCreate = Typeface.defaultFromStyle(i);
            } else {
                typefaceCreate = Typeface.create(typeface, i);
            }
            setSwitchTypeface(typefaceCreate);
            int i2 = (~(typefaceCreate != null ? typefaceCreate.getStyle() : 0)) & i;
            this.mTextPaint.setFakeBoldText((i2 & 1) != 0);
            this.mTextPaint.setTextSkewX((i2 & 2) != 0 ? -0.25f : 0.0f);
            return;
        }
        this.mTextPaint.setFakeBoldText(false);
        this.mTextPaint.setTextSkewX(0.0f);
        setSwitchTypeface(typeface);
    }

    public void setSwitchTypeface(Typeface typeface) {
        if (this.mTextPaint.getTypeface() != typeface) {
            this.mTextPaint.setTypeface(typeface);
            requestLayout();
            invalidate();
        }
    }

    public void setSwitchPadding(int i) {
        this.mSwitchPadding = i;
        requestLayout();
    }

    public int getSwitchPadding() {
        return this.mSwitchPadding;
    }

    public void setSwitchMinWidth(int i) {
        this.mSwitchMinWidth = i;
        requestLayout();
    }

    public int getSwitchMinWidth() {
        return this.mSwitchMinWidth;
    }

    public void setThumbTextPadding(int i) {
        this.mThumbTextPadding = i;
        requestLayout();
    }

    public int getThumbTextPadding() {
        return this.mThumbTextPadding;
    }

    public void setTrackDrawable(Drawable drawable) {
        if (this.mTrackDrawable != null) {
            this.mTrackDrawable.setCallback(null);
        }
        this.mTrackDrawable = drawable;
        if (drawable != null) {
            drawable.setCallback(this);
        }
        requestLayout();
    }

    public void setTrackResource(int i) {
        setTrackDrawable(getContext().getDrawable(i));
    }

    public Drawable getTrackDrawable() {
        return this.mTrackDrawable;
    }

    public void setTrackTintList(ColorStateList colorStateList) {
        this.mTrackTintList = colorStateList;
        this.mHasTrackTint = true;
        applyTrackTint();
    }

    public ColorStateList getTrackTintList() {
        return this.mTrackTintList;
    }

    public void setTrackTintMode(PorterDuff.Mode mode) {
        this.mTrackTintMode = mode;
        this.mHasTrackTintMode = true;
        applyTrackTint();
    }

    public PorterDuff.Mode getTrackTintMode() {
        return this.mTrackTintMode;
    }

    private void applyTrackTint() {
        if (this.mTrackDrawable != null) {
            if (this.mHasTrackTint || this.mHasTrackTintMode) {
                this.mTrackDrawable = this.mTrackDrawable.mutate();
                if (this.mHasTrackTint) {
                    this.mTrackDrawable.setTintList(this.mTrackTintList);
                }
                if (this.mHasTrackTintMode) {
                    this.mTrackDrawable.setTintMode(this.mTrackTintMode);
                }
                if (this.mTrackDrawable.isStateful()) {
                    this.mTrackDrawable.setState(getDrawableState());
                }
            }
        }
    }

    public void setThumbDrawable(Drawable drawable) {
        if (this.mThumbDrawable != null) {
            this.mThumbDrawable.setCallback(null);
        }
        this.mThumbDrawable = drawable;
        if (drawable != null) {
            drawable.setCallback(this);
        }
        requestLayout();
    }

    public void setThumbResource(int i) {
        setThumbDrawable(getContext().getDrawable(i));
    }

    public Drawable getThumbDrawable() {
        return this.mThumbDrawable;
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
        if (this.mThumbDrawable != null) {
            if (this.mHasThumbTint || this.mHasThumbTintMode) {
                this.mThumbDrawable = this.mThumbDrawable.mutate();
                if (this.mHasThumbTint) {
                    this.mThumbDrawable.setTintList(this.mThumbTintList);
                }
                if (this.mHasThumbTintMode) {
                    this.mThumbDrawable.setTintMode(this.mThumbTintMode);
                }
                if (this.mThumbDrawable.isStateful()) {
                    this.mThumbDrawable.setState(getDrawableState());
                }
            }
        }
    }

    public void setSplitTrack(boolean z) {
        this.mSplitTrack = z;
        invalidate();
    }

    public boolean getSplitTrack() {
        return this.mSplitTrack;
    }

    public CharSequence getTextOn() {
        return this.mTextOn;
    }

    public void setTextOn(CharSequence charSequence) {
        this.mTextOn = charSequence;
        requestLayout();
    }

    public CharSequence getTextOff() {
        return this.mTextOff;
    }

    public void setTextOff(CharSequence charSequence) {
        this.mTextOff = charSequence;
        requestLayout();
    }

    public void setShowText(boolean z) {
        if (this.mShowText != z) {
            this.mShowText = z;
            requestLayout();
        }
    }

    public boolean getShowText() {
        return this.mShowText;
    }

    @Override
    public void onMeasure(int i, int i2) {
        int intrinsicWidth;
        int intrinsicHeight;
        int iMax;
        if (this.mShowText) {
            if (this.mOnLayout == null) {
                this.mOnLayout = makeLayout(this.mTextOn);
            }
            if (this.mOffLayout == null) {
                this.mOffLayout = makeLayout(this.mTextOff);
            }
        }
        Rect rect = this.mTempRect;
        int intrinsicHeight2 = 0;
        if (this.mThumbDrawable != null) {
            this.mThumbDrawable.getPadding(rect);
            intrinsicWidth = (this.mThumbDrawable.getIntrinsicWidth() - rect.left) - rect.right;
            intrinsicHeight = this.mThumbDrawable.getIntrinsicHeight();
        } else {
            intrinsicWidth = 0;
            intrinsicHeight = 0;
        }
        if (this.mShowText) {
            iMax = Math.max(this.mOnLayout.getWidth(), this.mOffLayout.getWidth()) + (this.mThumbTextPadding * 2);
        } else {
            iMax = 0;
        }
        this.mThumbWidth = Math.max(iMax, intrinsicWidth);
        if (this.mTrackDrawable != null) {
            this.mTrackDrawable.getPadding(rect);
            intrinsicHeight2 = this.mTrackDrawable.getIntrinsicHeight();
        } else {
            rect.setEmpty();
        }
        int iMax2 = rect.left;
        int iMax3 = rect.right;
        if (this.mThumbDrawable != null) {
            Insets opticalInsets = this.mThumbDrawable.getOpticalInsets();
            iMax2 = Math.max(iMax2, opticalInsets.left);
            iMax3 = Math.max(iMax3, opticalInsets.right);
        }
        int iMax4 = Math.max(this.mSwitchMinWidth, (2 * this.mThumbWidth) + iMax2 + iMax3);
        int iMax5 = Math.max(intrinsicHeight2, intrinsicHeight);
        this.mSwitchWidth = iMax4;
        this.mSwitchHeight = iMax5;
        super.onMeasure(i, i2);
        if (getMeasuredHeight() < iMax5) {
            setMeasuredDimension(getMeasuredWidthAndState(), iMax5);
        }
    }

    @Override
    public void onPopulateAccessibilityEventInternal(AccessibilityEvent accessibilityEvent) {
        super.onPopulateAccessibilityEventInternal(accessibilityEvent);
        CharSequence charSequence = isChecked() ? this.mTextOn : this.mTextOff;
        if (charSequence != null) {
            accessibilityEvent.getText().add(charSequence);
        }
    }

    private Layout makeLayout(CharSequence charSequence) {
        if (this.mSwitchTransformationMethod != null) {
            charSequence = this.mSwitchTransformationMethod.getTransformation(charSequence, this);
        }
        return StaticLayout.Builder.obtain(charSequence, 0, charSequence.length(), this.mTextPaint, (int) Math.ceil(Layout.getDesiredWidth(charSequence, 0, charSequence.length(), this.mTextPaint, getTextDirectionHeuristic()))).setUseLineSpacingFromFallbacks(this.mUseFallbackLineSpacing).build();
    }

    private boolean hitThumb(float f, float f2) {
        if (this.mThumbDrawable == null) {
            return false;
        }
        int thumbOffset = getThumbOffset();
        this.mThumbDrawable.getPadding(this.mTempRect);
        int i = this.mSwitchTop - this.mTouchSlop;
        int i2 = (this.mSwitchLeft + thumbOffset) - this.mTouchSlop;
        return f > ((float) i2) && f < ((float) ((((this.mThumbWidth + i2) + this.mTempRect.left) + this.mTempRect.right) + this.mTouchSlop)) && f2 > ((float) i) && f2 < ((float) (this.mSwitchBottom + this.mTouchSlop));
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        float f;
        this.mVelocityTracker.addMovement(motionEvent);
        switch (motionEvent.getActionMasked()) {
            case 0:
                float x = motionEvent.getX();
                float y = motionEvent.getY();
                if (isEnabled() && hitThumb(x, y)) {
                    this.mTouchMode = 1;
                    this.mTouchX = x;
                    this.mTouchY = y;
                }
                break;
            case 1:
            case 3:
                if (this.mTouchMode == 2) {
                    stopDrag(motionEvent);
                    super.onTouchEvent(motionEvent);
                    return true;
                }
                this.mTouchMode = 0;
                this.mVelocityTracker.clear();
                break;
                break;
            case 2:
                switch (this.mTouchMode) {
                    case 1:
                        float x2 = motionEvent.getX();
                        float y2 = motionEvent.getY();
                        if (Math.abs(x2 - this.mTouchX) > this.mTouchSlop || Math.abs(y2 - this.mTouchY) > this.mTouchSlop) {
                            this.mTouchMode = 2;
                            getParent().requestDisallowInterceptTouchEvent(true);
                            this.mTouchX = x2;
                            this.mTouchY = y2;
                            return true;
                        }
                        break;
                    case 2:
                        float x3 = motionEvent.getX();
                        int thumbScrollRange = getThumbScrollRange();
                        float f2 = x3 - this.mTouchX;
                        if (thumbScrollRange != 0) {
                            f = f2 / thumbScrollRange;
                        } else {
                            f = f2 > 0.0f ? 1.0f : -1.0f;
                        }
                        if (isLayoutRtl()) {
                            f = -f;
                        }
                        float fConstrain = MathUtils.constrain(this.mThumbPosition + f, 0.0f, 1.0f);
                        if (fConstrain != this.mThumbPosition) {
                            this.mTouchX = x3;
                            setThumbPosition(fConstrain);
                        }
                        return true;
                }
                break;
        }
        return super.onTouchEvent(motionEvent);
    }

    private void cancelSuperTouch(MotionEvent motionEvent) {
        MotionEvent motionEventObtain = MotionEvent.obtain(motionEvent);
        motionEventObtain.setAction(3);
        super.onTouchEvent(motionEventObtain);
        motionEventObtain.recycle();
    }

    private void stopDrag(MotionEvent motionEvent) {
        boolean targetCheckedState;
        this.mTouchMode = 0;
        boolean z = true;
        boolean z2 = motionEvent.getAction() == 1 && isEnabled();
        boolean zIsChecked = isChecked();
        if (z2) {
            this.mVelocityTracker.computeCurrentVelocity(1000);
            float xVelocity = this.mVelocityTracker.getXVelocity();
            if (Math.abs(xVelocity) > this.mMinFlingVelocity) {
                if (!isLayoutRtl() ? xVelocity <= 0.0f : xVelocity >= 0.0f) {
                    z = false;
                }
                targetCheckedState = z;
            } else {
                targetCheckedState = getTargetCheckedState();
            }
        } else {
            targetCheckedState = zIsChecked;
        }
        if (targetCheckedState != zIsChecked) {
            playSoundEffect(0);
        }
        setChecked(targetCheckedState);
        cancelSuperTouch(motionEvent);
    }

    private void animateThumbToCheckedState(boolean z) {
        this.mPositionAnimator = ObjectAnimator.ofFloat(this, THUMB_POS, z ? 1.0f : 0.0f);
        this.mPositionAnimator.setDuration(250L);
        this.mPositionAnimator.setAutoCancel(true);
        this.mPositionAnimator.start();
    }

    private void cancelPositionAnimator() {
        if (this.mPositionAnimator != null) {
            this.mPositionAnimator.cancel();
        }
    }

    private boolean getTargetCheckedState() {
        return this.mThumbPosition > 0.5f;
    }

    private void setThumbPosition(float f) {
        this.mThumbPosition = f;
        invalidate();
    }

    @Override
    public void toggle() {
        setChecked(!isChecked());
    }

    @Override
    public void setChecked(boolean z) {
        super.setChecked(z);
        boolean zIsChecked = isChecked();
        if (isAttachedToWindow() && isLaidOut()) {
            animateThumbToCheckedState(zIsChecked);
        } else {
            cancelPositionAnimator();
            setThumbPosition(zIsChecked ? 1.0f : 0.0f);
        }
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int iMax;
        int width;
        int paddingLeft;
        int paddingTop;
        int height;
        super.onLayout(z, i, i2, i3, i4);
        int i5 = 0;
        if (this.mThumbDrawable != null) {
            Rect rect = this.mTempRect;
            if (this.mTrackDrawable != null) {
                this.mTrackDrawable.getPadding(rect);
            } else {
                rect.setEmpty();
            }
            Insets opticalInsets = this.mThumbDrawable.getOpticalInsets();
            int iMax2 = Math.max(0, opticalInsets.left - rect.left);
            iMax = Math.max(0, opticalInsets.right - rect.right);
            i5 = iMax2;
        } else {
            iMax = 0;
        }
        if (isLayoutRtl()) {
            paddingLeft = getPaddingLeft() + i5;
            width = ((this.mSwitchWidth + paddingLeft) - i5) - iMax;
        } else {
            width = (getWidth() - getPaddingRight()) - iMax;
            paddingLeft = (width - this.mSwitchWidth) + i5 + iMax;
        }
        int gravity = getGravity() & 112;
        if (gravity == 16) {
            paddingTop = (((getPaddingTop() + getHeight()) - getPaddingBottom()) / 2) - (this.mSwitchHeight / 2);
            height = this.mSwitchHeight + paddingTop;
        } else if (gravity != 80) {
            paddingTop = getPaddingTop();
            height = this.mSwitchHeight + paddingTop;
        } else {
            height = getHeight() - getPaddingBottom();
            paddingTop = height - this.mSwitchHeight;
        }
        this.mSwitchLeft = paddingLeft;
        this.mSwitchTop = paddingTop;
        this.mSwitchBottom = height;
        this.mSwitchRight = width;
    }

    @Override
    public void draw(Canvas canvas) {
        Insets opticalInsets;
        int i;
        int i2;
        Rect rect = this.mTempRect;
        int i3 = this.mSwitchLeft;
        int i4 = this.mSwitchTop;
        int i5 = this.mSwitchRight;
        int i6 = this.mSwitchBottom;
        int thumbOffset = getThumbOffset() + i3;
        if (this.mThumbDrawable != null) {
            opticalInsets = this.mThumbDrawable.getOpticalInsets();
        } else {
            opticalInsets = Insets.NONE;
        }
        if (this.mTrackDrawable != null) {
            this.mTrackDrawable.getPadding(rect);
            thumbOffset += rect.left;
            if (opticalInsets != Insets.NONE) {
                if (opticalInsets.left > rect.left) {
                    i3 += opticalInsets.left - rect.left;
                }
                if (opticalInsets.top > rect.top) {
                    i = (opticalInsets.top - rect.top) + i4;
                } else {
                    i = i4;
                }
                if (opticalInsets.right > rect.right) {
                    i5 -= opticalInsets.right - rect.right;
                }
                if (opticalInsets.bottom > rect.bottom) {
                    i2 = i6 - (opticalInsets.bottom - rect.bottom);
                }
                this.mTrackDrawable.setBounds(i3, i, i5, i2);
            } else {
                i = i4;
            }
            i2 = i6;
            this.mTrackDrawable.setBounds(i3, i, i5, i2);
        }
        if (this.mThumbDrawable != null) {
            this.mThumbDrawable.getPadding(rect);
            int i7 = thumbOffset - rect.left;
            int i8 = thumbOffset + this.mThumbWidth + rect.right;
            this.mThumbDrawable.setBounds(i7, i4, i8, i6);
            Drawable background = getBackground();
            if (background != null) {
                background.setHotspotBounds(i7, i4, i8, i6);
            }
        }
        super.draw(canvas);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width;
        super.onDraw(canvas);
        Rect rect = this.mTempRect;
        Drawable drawable = this.mTrackDrawable;
        if (drawable != null) {
            drawable.getPadding(rect);
        } else {
            rect.setEmpty();
        }
        int i = this.mSwitchTop;
        int i2 = this.mSwitchBottom;
        int i3 = i + rect.top;
        int i4 = i2 - rect.bottom;
        Drawable drawable2 = this.mThumbDrawable;
        if (drawable != null) {
            if (this.mSplitTrack && drawable2 != null) {
                Insets opticalInsets = drawable2.getOpticalInsets();
                drawable2.copyBounds(rect);
                rect.left += opticalInsets.left;
                rect.right -= opticalInsets.right;
                int iSave = canvas.save();
                canvas.clipRect(rect, Region.Op.DIFFERENCE);
                drawable.draw(canvas);
                canvas.restoreToCount(iSave);
            } else {
                drawable.draw(canvas);
            }
        }
        int iSave2 = canvas.save();
        if (drawable2 != null) {
            drawable2.draw(canvas);
        }
        Layout layout = getTargetCheckedState() ? this.mOnLayout : this.mOffLayout;
        if (layout != null) {
            int[] drawableState = getDrawableState();
            if (this.mTextColors != null) {
                this.mTextPaint.setColor(this.mTextColors.getColorForState(drawableState, 0));
            }
            this.mTextPaint.drawableState = drawableState;
            if (drawable2 != null) {
                Rect bounds = drawable2.getBounds();
                width = bounds.left + bounds.right;
            } else {
                width = getWidth();
            }
            canvas.translate((width / 2) - (layout.getWidth() / 2), ((i3 + i4) / 2) - (layout.getHeight() / 2));
            layout.draw(canvas);
        }
        canvas.restoreToCount(iSave2);
    }

    @Override
    public int getCompoundPaddingLeft() {
        if (!isLayoutRtl()) {
            return super.getCompoundPaddingLeft();
        }
        int compoundPaddingLeft = super.getCompoundPaddingLeft() + this.mSwitchWidth;
        if (!TextUtils.isEmpty(getText())) {
            return compoundPaddingLeft + this.mSwitchPadding;
        }
        return compoundPaddingLeft;
    }

    @Override
    public int getCompoundPaddingRight() {
        if (isLayoutRtl()) {
            return super.getCompoundPaddingRight();
        }
        int compoundPaddingRight = super.getCompoundPaddingRight() + this.mSwitchWidth;
        if (!TextUtils.isEmpty(getText())) {
            return compoundPaddingRight + this.mSwitchPadding;
        }
        return compoundPaddingRight;
    }

    private int getThumbOffset() {
        float f;
        if (isLayoutRtl()) {
            f = 1.0f - this.mThumbPosition;
        } else {
            f = this.mThumbPosition;
        }
        return (int) ((f * getThumbScrollRange()) + 0.5f);
    }

    private int getThumbScrollRange() {
        Insets opticalInsets;
        if (this.mTrackDrawable != null) {
            Rect rect = this.mTempRect;
            this.mTrackDrawable.getPadding(rect);
            if (this.mThumbDrawable != null) {
                opticalInsets = this.mThumbDrawable.getOpticalInsets();
            } else {
                opticalInsets = Insets.NONE;
            }
            return ((((this.mSwitchWidth - this.mThumbWidth) - rect.left) - rect.right) - opticalInsets.left) - opticalInsets.right;
        }
        return 0;
    }

    @Override
    protected int[] onCreateDrawableState(int i) {
        int[] iArrOnCreateDrawableState = super.onCreateDrawableState(i + 1);
        if (isChecked()) {
            mergeDrawableStates(iArrOnCreateDrawableState, CHECKED_STATE_SET);
        }
        return iArrOnCreateDrawableState;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        int[] drawableState = getDrawableState();
        Drawable drawable = this.mThumbDrawable;
        boolean state = false;
        if (drawable != null && drawable.isStateful()) {
            state = false | drawable.setState(drawableState);
        }
        Drawable drawable2 = this.mTrackDrawable;
        if (drawable2 != null && drawable2.isStateful()) {
            state |= drawable2.setState(drawableState);
        }
        if (state) {
            invalidate();
        }
    }

    @Override
    public void drawableHotspotChanged(float f, float f2) {
        super.drawableHotspotChanged(f, f2);
        if (this.mThumbDrawable != null) {
            this.mThumbDrawable.setHotspot(f, f2);
        }
        if (this.mTrackDrawable != null) {
            this.mTrackDrawable.setHotspot(f, f2);
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable drawable) {
        return super.verifyDrawable(drawable) || drawable == this.mThumbDrawable || drawable == this.mTrackDrawable;
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (this.mThumbDrawable != null) {
            this.mThumbDrawable.jumpToCurrentState();
        }
        if (this.mTrackDrawable != null) {
            this.mTrackDrawable.jumpToCurrentState();
        }
        if (this.mPositionAnimator != null && this.mPositionAnimator.isStarted()) {
            this.mPositionAnimator.end();
            this.mPositionAnimator = null;
        }
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return Switch.class.getName();
    }

    @Override
    public void onProvideStructure(ViewStructure viewStructure) {
        super.onProvideStructure(viewStructure);
        onProvideAutoFillStructureForAssistOrAutofill(viewStructure);
    }

    @Override
    public void onProvideAutofillStructure(ViewStructure viewStructure, int i) {
        super.onProvideAutofillStructure(viewStructure, i);
        onProvideAutoFillStructureForAssistOrAutofill(viewStructure);
    }

    private void onProvideAutoFillStructureForAssistOrAutofill(ViewStructure viewStructure) {
        CharSequence charSequence = isChecked() ? this.mTextOn : this.mTextOff;
        if (!TextUtils.isEmpty(charSequence)) {
            CharSequence text = viewStructure.getText();
            if (TextUtils.isEmpty(text)) {
                viewStructure.setText(charSequence);
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(text);
            sb.append(' ');
            sb.append(charSequence);
            viewStructure.setText(sb);
        }
    }

    @Override
    public void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfoInternal(accessibilityNodeInfo);
        CharSequence charSequence = isChecked() ? this.mTextOn : this.mTextOff;
        if (!TextUtils.isEmpty(charSequence)) {
            CharSequence text = accessibilityNodeInfo.getText();
            if (TextUtils.isEmpty(text)) {
                accessibilityNodeInfo.setText(charSequence);
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(text);
            sb.append(' ');
            sb.append(charSequence);
            accessibilityNodeInfo.setText(sb);
        }
    }
}
