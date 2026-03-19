package com.android.phone.common.dialpad;

import android.content.Context;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityManager;
import android.widget.FrameLayout;
import com.android.contacts.model.account.BaseAccountType;

public class DialpadKeyButton extends FrameLayout {
    private static final int LONG_HOVER_TIMEOUT = ViewConfiguration.getLongPressTimeout() * 2;
    private AccessibilityManager mAccessibilityManager;
    private CharSequence mBackupContentDesc;
    private RectF mHoverBounds;
    private CharSequence mLongHoverContentDesc;
    private Runnable mLongHoverRunnable;
    private boolean mLongHovered;
    private OnPressedListener mOnPressedListener;
    private boolean mWasClickable;
    private boolean mWasLongClickable;

    public interface OnPressedListener {
        void onPressed(View view, boolean z);
    }

    public DialpadKeyButton(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mHoverBounds = new RectF();
        initForAccessibility(context);
    }

    public DialpadKeyButton(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mHoverBounds = new RectF();
        initForAccessibility(context);
    }

    private void initForAccessibility(Context context) {
        this.mAccessibilityManager = (AccessibilityManager) context.getSystemService("accessibility");
    }

    public void setLongHoverContentDescription(CharSequence charSequence) {
        this.mLongHoverContentDesc = charSequence;
        if (this.mLongHovered) {
            super.setContentDescription(this.mLongHoverContentDesc);
        }
    }

    @Override
    public void setContentDescription(CharSequence charSequence) {
        if (this.mLongHovered) {
            this.mBackupContentDesc = charSequence;
        } else {
            super.setContentDescription(charSequence);
        }
    }

    @Override
    public void setPressed(boolean z) {
        super.setPressed(z);
        if (this.mOnPressedListener != null) {
            this.mOnPressedListener.onPressed(this, z);
        }
    }

    @Override
    public void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        this.mHoverBounds.left = getPaddingLeft();
        this.mHoverBounds.right = i - getPaddingRight();
        this.mHoverBounds.top = getPaddingTop();
        this.mHoverBounds.bottom = i2 - getPaddingBottom();
    }

    @Override
    public boolean performAccessibilityAction(int i, Bundle bundle) {
        if (i == 16) {
            simulateClickForAccessibility();
            return true;
        }
        return super.performAccessibilityAction(i, bundle);
    }

    @Override
    public boolean onHoverEvent(MotionEvent motionEvent) {
        if (this.mAccessibilityManager.isEnabled() && this.mAccessibilityManager.isTouchExplorationEnabled()) {
            switch (motionEvent.getActionMasked()) {
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
                case BaseAccountType.Weight.PHONE:
                    if (this.mHoverBounds.contains(motionEvent.getX(), motionEvent.getY())) {
                        if (this.mLongHovered) {
                            simulateClickForAccessibility();
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
        return super.onHoverEvent(motionEvent);
    }

    private void simulateClickForAccessibility() {
        if (isPressed()) {
            return;
        }
        setPressed(true);
        sendAccessibilityEvent(1);
        setPressed(false);
    }

    private void setLongHovered(boolean z) {
        if (this.mLongHovered != z) {
            this.mLongHovered = z;
            if (z) {
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
