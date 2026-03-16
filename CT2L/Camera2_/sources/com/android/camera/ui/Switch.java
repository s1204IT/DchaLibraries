package com.android.camera.ui;

import android.R;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.CompoundButton;

public class Switch extends CompoundButton {
    private static final int[] CHECKED_STATE_SET = {R.attr.state_checked};
    private static final int TOUCH_MODE_DOWN = 1;
    private static final int TOUCH_MODE_DRAGGING = 2;
    private static final int TOUCH_MODE_IDLE = 0;
    private int mMinFlingVelocity;
    private Layout mOffLayout;
    private Layout mOnLayout;
    private int mSwitchBottom;
    private int mSwitchHeight;
    private int mSwitchLeft;
    private int mSwitchMinWidth;
    private int mSwitchPadding;
    private int mSwitchRight;
    private int mSwitchTextMaxWidth;
    private int mSwitchTop;
    private int mSwitchWidth;
    private final Rect mTempRect;
    private ColorStateList mTextColors;
    private CharSequence mTextOff;
    private CharSequence mTextOn;
    private TextPaint mTextPaint;
    private Drawable mThumbDrawable;
    private float mThumbPosition;
    private int mThumbTextPadding;
    private int mThumbWidth;
    private int mTouchMode;
    private int mTouchSlop;
    private float mTouchX;
    private float mTouchY;
    private Drawable mTrackDrawable;
    private VelocityTracker mVelocityTracker;

    public Switch(Context context, AttributeSet attrs) {
        this(context, attrs, com.android.camera2.R.attr.switchStyle);
    }

    public Switch(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mVelocityTracker = VelocityTracker.obtain();
        this.mTempRect = new Rect();
        this.mTextPaint = new TextPaint(1);
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        this.mTextPaint.density = dm.density;
        this.mThumbDrawable = res.getDrawable(com.android.camera2.R.drawable.switch_inner_holo_dark);
        this.mTrackDrawable = res.getDrawable(com.android.camera2.R.drawable.switch_track_holo_dark);
        this.mTextOn = res.getString(com.android.camera2.R.string.capital_on);
        this.mTextOff = res.getString(com.android.camera2.R.string.capital_off);
        this.mThumbTextPadding = res.getDimensionPixelSize(com.android.camera2.R.dimen.thumb_text_padding);
        this.mSwitchMinWidth = res.getDimensionPixelSize(com.android.camera2.R.dimen.switch_min_width);
        this.mSwitchTextMaxWidth = res.getDimensionPixelSize(com.android.camera2.R.dimen.switch_text_max_width);
        this.mSwitchPadding = res.getDimensionPixelSize(com.android.camera2.R.dimen.switch_padding);
        setSwitchTextAppearance(context, R.style.TextAppearance.Holo.Small);
        ViewConfiguration config = ViewConfiguration.get(context);
        this.mTouchSlop = config.getScaledTouchSlop();
        this.mMinFlingVelocity = config.getScaledMinimumFlingVelocity();
        refreshDrawableState();
        setChecked(isChecked());
    }

    public void setSwitchTextAppearance(Context context, int resid) {
        Resources res = getResources();
        this.mTextColors = getTextColors();
        int ts = res.getDimensionPixelSize(com.android.camera2.R.dimen.thumb_text_size);
        if (ts != this.mTextPaint.getTextSize()) {
            this.mTextPaint.setTextSize(ts);
            requestLayout();
        }
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.mOnLayout == null) {
            this.mOnLayout = makeLayout(this.mTextOn, this.mSwitchTextMaxWidth);
        }
        if (this.mOffLayout == null) {
            this.mOffLayout = makeLayout(this.mTextOff, this.mSwitchTextMaxWidth);
        }
        this.mTrackDrawable.getPadding(this.mTempRect);
        int maxTextWidth = Math.min(this.mSwitchTextMaxWidth, Math.max(this.mOnLayout.getWidth(), this.mOffLayout.getWidth()));
        int switchWidth = Math.max(this.mSwitchMinWidth, (maxTextWidth * 2) + (this.mThumbTextPadding * 4) + this.mTempRect.left + this.mTempRect.right);
        int switchHeight = this.mTrackDrawable.getIntrinsicHeight();
        this.mThumbWidth = (this.mThumbTextPadding * 2) + maxTextWidth;
        this.mSwitchWidth = switchWidth;
        this.mSwitchHeight = switchHeight;
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int measuredHeight = getMeasuredHeight();
        int measuredWidth = getMeasuredWidth();
        if (measuredHeight < switchHeight) {
            setMeasuredDimension(measuredWidth, switchHeight);
        }
    }

    @Override
    @TargetApi(14)
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        super.onPopulateAccessibilityEvent(event);
        CharSequence text = isChecked() ? this.mOnLayout.getText() : this.mOffLayout.getText();
        if (!TextUtils.isEmpty(text)) {
            event.getText().add(text);
        }
    }

    private Layout makeLayout(CharSequence text, int maxWidth) {
        int actual_width = (int) Math.ceil(Layout.getDesiredWidth(text, this.mTextPaint));
        StaticLayout l = new StaticLayout(text, 0, text.length(), this.mTextPaint, actual_width, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true, TextUtils.TruncateAt.END, Math.min(actual_width, maxWidth));
        return l;
    }

    private boolean hitThumb(float x, float y) {
        this.mThumbDrawable.getPadding(this.mTempRect);
        int thumbTop = this.mSwitchTop - this.mTouchSlop;
        int thumbLeft = (this.mSwitchLeft + ((int) (this.mThumbPosition + 0.5f))) - this.mTouchSlop;
        int thumbRight = this.mThumbWidth + thumbLeft + this.mTempRect.left + this.mTempRect.right + this.mTouchSlop;
        int thumbBottom = this.mSwitchBottom + this.mTouchSlop;
        return x > ((float) thumbLeft) && x < ((float) thumbRight) && y > ((float) thumbTop) && y < ((float) thumbBottom);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        this.mVelocityTracker.addMovement(ev);
        int action = ev.getActionMasked();
        switch (action) {
            case 0:
                float x = ev.getX();
                float y = ev.getY();
                if (isEnabled() && hitThumb(x, y)) {
                    this.mTouchMode = 1;
                    this.mTouchX = x;
                    this.mTouchY = y;
                }
                break;
            case 1:
            case 3:
                if (this.mTouchMode == 2) {
                    stopDrag(ev);
                    return true;
                }
                this.mTouchMode = 0;
                this.mVelocityTracker.clear();
                break;
                break;
            case 2:
                switch (this.mTouchMode) {
                    case 1:
                        float x2 = ev.getX();
                        float y2 = ev.getY();
                        if (Math.abs(x2 - this.mTouchX) > this.mTouchSlop || Math.abs(y2 - this.mTouchY) > this.mTouchSlop) {
                            this.mTouchMode = 2;
                            getParent().requestDisallowInterceptTouchEvent(true);
                            this.mTouchX = x2;
                            this.mTouchY = y2;
                            return true;
                        }
                        break;
                    case 2:
                        float x3 = ev.getX();
                        float dx = x3 - this.mTouchX;
                        float newPos = Math.max(0.0f, Math.min(this.mThumbPosition + dx, getThumbScrollRange()));
                        if (newPos == this.mThumbPosition) {
                            return true;
                        }
                        this.mThumbPosition = newPos;
                        this.mTouchX = x3;
                        invalidate();
                        return true;
                }
                break;
        }
        return super.onTouchEvent(ev);
    }

    private void cancelSuperTouch(MotionEvent ev) {
        MotionEvent cancel = MotionEvent.obtain(ev);
        cancel.setAction(3);
        super.onTouchEvent(cancel);
        cancel.recycle();
    }

    private void stopDrag(MotionEvent ev) {
        boolean newState;
        this.mTouchMode = 0;
        boolean commitChange = ev.getAction() == 1 && isEnabled();
        cancelSuperTouch(ev);
        if (commitChange) {
            this.mVelocityTracker.computeCurrentVelocity(1000);
            float xvel = this.mVelocityTracker.getXVelocity();
            if (Math.abs(xvel) > this.mMinFlingVelocity) {
                newState = xvel > 0.0f;
            } else {
                newState = getTargetCheckedState();
            }
            animateThumbToCheckedState(newState);
            return;
        }
        animateThumbToCheckedState(isChecked());
    }

    private void animateThumbToCheckedState(boolean newCheckedState) {
        setChecked(newCheckedState);
    }

    private boolean getTargetCheckedState() {
        return this.mThumbPosition >= ((float) (getThumbScrollRange() / 2));
    }

    private void setThumbPosition(boolean checked) {
        this.mThumbPosition = checked ? getThumbScrollRange() : 0.0f;
    }

    @Override
    public void setChecked(boolean checked) {
        super.setChecked(checked);
        setThumbPosition(checked);
        invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int switchBottom;
        int switchTop;
        super.onLayout(changed, left, top, right, bottom);
        setThumbPosition(isChecked());
        int switchRight = getWidth() - getPaddingRight();
        int switchLeft = switchRight - this.mSwitchWidth;
        switch (getGravity() & 112) {
            case 16:
                switchTop = (((getPaddingTop() + getHeight()) - getPaddingBottom()) / 2) - (this.mSwitchHeight / 2);
                switchBottom = switchTop + this.mSwitchHeight;
                break;
            case 80:
                switchBottom = getHeight() - getPaddingBottom();
                switchTop = switchBottom - this.mSwitchHeight;
                break;
            default:
                switchTop = getPaddingTop();
                switchBottom = switchTop + this.mSwitchHeight;
                break;
        }
        this.mSwitchLeft = switchLeft;
        this.mSwitchTop = switchTop;
        this.mSwitchBottom = switchBottom;
        this.mSwitchRight = switchRight;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int switchLeft = this.mSwitchLeft;
        int switchTop = this.mSwitchTop;
        int switchRight = this.mSwitchRight;
        int switchBottom = this.mSwitchBottom;
        this.mTrackDrawable.setBounds(switchLeft, switchTop, switchRight, switchBottom);
        this.mTrackDrawable.draw(canvas);
        canvas.save();
        this.mTrackDrawable.getPadding(this.mTempRect);
        int switchInnerLeft = switchLeft + this.mTempRect.left;
        int switchInnerTop = switchTop + this.mTempRect.top;
        int switchInnerRight = switchRight - this.mTempRect.right;
        int switchInnerBottom = switchBottom - this.mTempRect.bottom;
        canvas.clipRect(switchInnerLeft, switchTop, switchInnerRight, switchBottom);
        this.mThumbDrawable.getPadding(this.mTempRect);
        int thumbPos = (int) (this.mThumbPosition + 0.5f);
        int thumbLeft = (switchInnerLeft - this.mTempRect.left) + thumbPos;
        int thumbRight = switchInnerLeft + thumbPos + this.mThumbWidth + this.mTempRect.right;
        this.mThumbDrawable.setBounds(thumbLeft, switchTop, thumbRight, switchBottom);
        this.mThumbDrawable.draw(canvas);
        if (this.mTextColors != null) {
            this.mTextPaint.setColor(this.mTextColors.getColorForState(getDrawableState(), this.mTextColors.getDefaultColor()));
        }
        this.mTextPaint.drawableState = getDrawableState();
        Layout switchText = getTargetCheckedState() ? this.mOnLayout : this.mOffLayout;
        canvas.translate(((thumbLeft + thumbRight) / 2) - (switchText.getEllipsizedWidth() / 2), ((switchInnerTop + switchInnerBottom) / 2) - (switchText.getHeight() / 2));
        switchText.draw(canvas);
        canvas.restore();
    }

    @Override
    public int getCompoundPaddingRight() {
        int padding = super.getCompoundPaddingRight() + this.mSwitchWidth;
        if (!TextUtils.isEmpty(getText())) {
            return padding + this.mSwitchPadding;
        }
        return padding;
    }

    private int getThumbScrollRange() {
        if (this.mTrackDrawable == null) {
            return 0;
        }
        this.mTrackDrawable.getPadding(this.mTempRect);
        return ((this.mSwitchWidth - this.mThumbWidth) - this.mTempRect.left) - this.mTempRect.right;
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        int[] myDrawableState = getDrawableState();
        if (this.mThumbDrawable != null) {
            this.mThumbDrawable.setState(myDrawableState);
        }
        if (this.mTrackDrawable != null) {
            this.mTrackDrawable.setState(myDrawableState);
        }
        invalidate();
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == this.mThumbDrawable || who == this.mTrackDrawable;
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        this.mThumbDrawable.jumpToCurrentState();
        this.mTrackDrawable.jumpToCurrentState();
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(Switch.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(Switch.class.getName());
        CharSequence switchText = isChecked() ? this.mTextOn : this.mTextOff;
        if (!TextUtils.isEmpty(switchText)) {
            CharSequence oldText = info.getText();
            if (TextUtils.isEmpty(oldText)) {
                info.setText(switchText);
                return;
            }
            StringBuilder newText = new StringBuilder();
            newText.append(oldText).append(' ').append(switchText);
            info.setText(newText);
        }
    }
}
