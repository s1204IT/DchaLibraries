package com.android.phone.common.dialpad;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityManager;
import android.widget.FrameLayout;

public class DialpadKeyButton extends FrameLayout {
    private static final int LONG_HOVER_TIMEOUT = ViewConfiguration.getLongPressTimeout() * 2;
    private AccessibilityManager mAccessibilityManager;
    private CharSequence mBackupContentDesc;
    private Rect mHoverBounds;
    private CharSequence mLongHoverContentDesc;
    private Runnable mLongHoverRunnable;
    private boolean mLongHovered;
    private OnPressedListener mOnPressedListener;
    private boolean mWasClickable;
    private boolean mWasLongClickable;

    public interface OnPressedListener {
        void onPressed(View view, boolean z);
    }

    public void setOnPressedListener(OnPressedListener onPressedListener) {
        this.mOnPressedListener = onPressedListener;
    }

    public DialpadKeyButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mHoverBounds = new Rect();
        initForAccessibility(context);
    }

    public DialpadKeyButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mHoverBounds = new Rect();
        initForAccessibility(context);
    }

    private void initForAccessibility(Context context) {
        this.mAccessibilityManager = (AccessibilityManager) context.getSystemService("accessibility");
    }

    public void setLongHoverContentDescription(CharSequence contentDescription) {
        this.mLongHoverContentDesc = contentDescription;
        if (this.mLongHovered) {
            super.setContentDescription(this.mLongHoverContentDesc);
        }
    }

    @Override
    public void setContentDescription(CharSequence contentDescription) {
        if (this.mLongHovered) {
            this.mBackupContentDesc = contentDescription;
        } else {
            super.setContentDescription(contentDescription);
        }
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        if (this.mOnPressedListener != null) {
            this.mOnPressedListener.onPressed(this, pressed);
        }
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.mHoverBounds.left = getPaddingLeft();
        this.mHoverBounds.right = w - getPaddingRight();
        this.mHoverBounds.top = getPaddingTop();
        this.mHoverBounds.bottom = h - getPaddingBottom();
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (action != 16) {
            return super.performAccessibilityAction(action, arguments);
        }
        simulateClickForAccessibility();
        return true;
    }

    @Override
    public boolean onHoverEvent(MotionEvent event) {
        if (this.mAccessibilityManager.isEnabled() && this.mAccessibilityManager.isTouchExplorationEnabled()) {
            switch (event.getActionMasked()) {
                case 9:
                    this.mWasClickable = isClickable();
                    this.mWasLongClickable = isLongClickable();
                    if (this.mWasLongClickable && this.mLongHoverContentDesc != null) {
                        if (this.mLongHoverRunnable == null) {
                            this.mLongHoverRunnable = new Runnable() {
                                @Override
                                public void run() {
                                    DialpadKeyButton.this.setLongHovered(true);
                                    DialpadKeyButton.this.announceForAccessibility(DialpadKeyButton.this.mLongHoverContentDesc);
                                }
                            };
                        }
                        postDelayed(this.mLongHoverRunnable, LONG_HOVER_TIMEOUT);
                    }
                    setClickable(false);
                    setLongClickable(false);
                    break;
                case 10:
                    if (this.mHoverBounds.contains((int) event.getX(), (int) event.getY())) {
                        if (this.mLongHovered) {
                            performLongClick();
                        } else {
                            simulateClickForAccessibility();
                        }
                    }
                    cancelLongHover();
                    setClickable(this.mWasClickable);
                    setLongClickable(this.mWasLongClickable);
                    break;
            }
        }
        return super.onHoverEvent(event);
    }

    private void simulateClickForAccessibility() {
        if (!isPressed()) {
            setPressed(true);
            sendAccessibilityEvent(1);
            setPressed(false);
        }
    }

    private void setLongHovered(boolean enabled) {
        if (this.mLongHovered != enabled) {
            this.mLongHovered = enabled;
            if (enabled) {
                this.mBackupContentDesc = getContentDescription();
                super.setContentDescription(this.mLongHoverContentDesc);
            } else {
                super.setContentDescription(this.mBackupContentDesc);
            }
        }
    }

    private void cancelLongHover() {
        if (this.mLongHoverRunnable != null) {
            removeCallbacks(this.mLongHoverRunnable);
        }
        setLongHovered(false);
    }
}
