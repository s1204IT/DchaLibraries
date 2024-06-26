package com.android.keyguard;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.ViewHierarchyEncoder;
import android.widget.FrameLayout;
import android.widget.ViewFlipper;
import com.android.internal.widget.LockPatternUtils;
/* loaded from: classes.dex */
public class KeyguardSecurityViewFlipper extends ViewFlipper implements KeyguardSecurityView {
    private static final boolean DEBUG = KeyguardConstants.DEBUG;
    private Rect mTempRect;

    public KeyguardSecurityViewFlipper(Context context) {
        this(context, null);
    }

    public KeyguardSecurityViewFlipper(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mTempRect = new Rect();
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        boolean onTouchEvent = super.onTouchEvent(motionEvent);
        this.mTempRect.set(0, 0, 0, 0);
        boolean z = onTouchEvent;
        for (int i = 0; i < getChildCount(); i++) {
            View childAt = getChildAt(i);
            if (childAt.getVisibility() == 0) {
                offsetRectIntoDescendantCoords(childAt, this.mTempRect);
                motionEvent.offsetLocation(this.mTempRect.left, this.mTempRect.top);
                z = childAt.dispatchTouchEvent(motionEvent) || z;
                motionEvent.offsetLocation(-this.mTempRect.left, -this.mTempRect.top);
            }
        }
        return z;
    }

    KeyguardSecurityView getSecurityView() {
        View childAt = getChildAt(getDisplayedChild());
        if (childAt instanceof KeyguardSecurityView) {
            return (KeyguardSecurityView) childAt;
        }
        return null;
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void setKeyguardCallback(KeyguardSecurityCallback keyguardSecurityCallback) {
        KeyguardSecurityView securityView = getSecurityView();
        if (securityView != null) {
            securityView.setKeyguardCallback(keyguardSecurityCallback);
        }
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void setLockPatternUtils(LockPatternUtils lockPatternUtils) {
        KeyguardSecurityView securityView = getSecurityView();
        if (securityView != null) {
            securityView.setLockPatternUtils(lockPatternUtils);
        }
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void reset() {
        KeyguardSecurityView securityView = getSecurityView();
        if (securityView != null) {
            securityView.reset();
        }
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void onPause() {
        KeyguardSecurityView securityView = getSecurityView();
        if (securityView != null) {
            securityView.onPause();
        }
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void onResume(int i) {
        KeyguardSecurityView securityView = getSecurityView();
        if (securityView != null) {
            securityView.onResume(i);
        }
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public boolean needsInput() {
        KeyguardSecurityView securityView = getSecurityView();
        if (securityView != null) {
            return securityView.needsInput();
        }
        return false;
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void showPromptReason(int i) {
        KeyguardSecurityView securityView = getSecurityView();
        if (securityView != null) {
            securityView.showPromptReason(i);
        }
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void showMessage(CharSequence charSequence, int i) {
        KeyguardSecurityView securityView = getSecurityView();
        if (securityView != null) {
            securityView.showMessage(charSequence, i);
        }
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void startAppearAnimation() {
        KeyguardSecurityView securityView = getSecurityView();
        if (securityView != null) {
            securityView.startAppearAnimation();
        }
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public boolean startDisappearAnimation(Runnable runnable) {
        KeyguardSecurityView securityView = getSecurityView();
        if (securityView != null) {
            return securityView.startDisappearAnimation(runnable);
        }
        return false;
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public CharSequence getTitle() {
        KeyguardSecurityView securityView = getSecurityView();
        if (securityView != null) {
            return securityView.getTitle();
        }
        return "";
    }

    @Override // android.widget.FrameLayout, android.view.ViewGroup
    protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return layoutParams instanceof LayoutParams;
    }

    @Override // android.widget.FrameLayout, android.view.ViewGroup
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return layoutParams instanceof LayoutParams ? new LayoutParams((LayoutParams) layoutParams) : new LayoutParams(layoutParams);
    }

    @Override // android.widget.FrameLayout, android.view.ViewGroup
    public LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new LayoutParams(getContext(), attributeSet);
    }

    @Override // android.widget.FrameLayout, android.view.View
    protected void onMeasure(int i, int i2) {
        int mode = View.MeasureSpec.getMode(i);
        int mode2 = View.MeasureSpec.getMode(i2);
        if (DEBUG && mode != Integer.MIN_VALUE) {
            Log.w("KeyguardSecurityViewFlipper", "onMeasure: widthSpec " + View.MeasureSpec.toString(i) + " should be AT_MOST");
        }
        if (DEBUG && mode2 != Integer.MIN_VALUE) {
            Log.w("KeyguardSecurityViewFlipper", "onMeasure: heightSpec " + View.MeasureSpec.toString(i2) + " should be AT_MOST");
        }
        int size = View.MeasureSpec.getSize(i);
        int size2 = View.MeasureSpec.getSize(i2);
        int childCount = getChildCount();
        int i3 = size;
        int i4 = size2;
        for (int i5 = 0; i5 < childCount; i5++) {
            LayoutParams layoutParams = (LayoutParams) getChildAt(i5).getLayoutParams();
            if (layoutParams.maxWidth > 0 && layoutParams.maxWidth < i3) {
                i3 = layoutParams.maxWidth;
            }
            if (layoutParams.maxHeight > 0 && layoutParams.maxHeight < i4) {
                i4 = layoutParams.maxHeight;
            }
        }
        int paddingLeft = getPaddingLeft() + getPaddingRight();
        int paddingTop = getPaddingTop() + getPaddingBottom();
        int max = Math.max(0, i3 - paddingLeft);
        int max2 = Math.max(0, i4 - paddingTop);
        int i6 = mode == 1073741824 ? size : 0;
        int i7 = mode2 == 1073741824 ? size2 : 0;
        for (int i8 = 0; i8 < childCount; i8++) {
            View childAt = getChildAt(i8);
            LayoutParams layoutParams2 = (LayoutParams) childAt.getLayoutParams();
            childAt.measure(makeChildMeasureSpec(max, layoutParams2.width), makeChildMeasureSpec(max2, layoutParams2.height));
            i6 = Math.max(i6, Math.min(childAt.getMeasuredWidth(), size - paddingLeft));
            i7 = Math.max(i7, Math.min(childAt.getMeasuredHeight(), size2 - paddingTop));
        }
        setMeasuredDimension(i6 + paddingLeft, i7 + paddingTop);
    }

    private int makeChildMeasureSpec(int i, int i2) {
        int i3 = 1073741824;
        switch (i2) {
            case -2:
                i3 = Integer.MIN_VALUE;
                break;
            case -1:
                break;
            default:
                i = Math.min(i, i2);
                break;
        }
        return View.MeasureSpec.makeMeasureSpec(i, i3);
    }

    /* loaded from: classes.dex */
    public static class LayoutParams extends FrameLayout.LayoutParams {
        @ViewDebug.ExportedProperty(category = "layout")
        public int maxHeight;
        @ViewDebug.ExportedProperty(category = "layout")
        public int maxWidth;

        public LayoutParams(ViewGroup.LayoutParams layoutParams) {
            super(layoutParams);
        }

        public LayoutParams(LayoutParams layoutParams) {
            super((FrameLayout.LayoutParams) layoutParams);
            this.maxWidth = layoutParams.maxWidth;
            this.maxHeight = layoutParams.maxHeight;
        }

        public LayoutParams(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.KeyguardSecurityViewFlipper_Layout, 0, 0);
            this.maxWidth = obtainStyledAttributes.getDimensionPixelSize(1, 0);
            this.maxHeight = obtainStyledAttributes.getDimensionPixelSize(0, 0);
            obtainStyledAttributes.recycle();
        }

        protected void encodeProperties(ViewHierarchyEncoder viewHierarchyEncoder) {
            super.encodeProperties(viewHierarchyEncoder);
            viewHierarchyEncoder.addProperty("layout:maxWidth", this.maxWidth);
            viewHierarchyEncoder.addProperty("layout:maxHeight", this.maxHeight);
        }
    }
}
