package com.android.keyguard;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ViewFlipper;
import com.android.internal.widget.LockPatternUtils;

public class KeyguardSecurityViewFlipper extends ViewFlipper implements KeyguardSecurityView {
    private Rect mTempRect;

    public KeyguardSecurityViewFlipper(Context context) {
        this(context, null);
    }

    public KeyguardSecurityViewFlipper(Context context, AttributeSet attr) {
        super(context, attr);
        this.mTempRect = new Rect();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean result = super.onTouchEvent(ev);
        this.mTempRect.set(0, 0, 0, 0);
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == 0) {
                offsetRectIntoDescendantCoords(child, this.mTempRect);
                ev.offsetLocation(this.mTempRect.left, this.mTempRect.top);
                result = child.dispatchTouchEvent(ev) || result;
                ev.offsetLocation(-this.mTempRect.left, -this.mTempRect.top);
            }
        }
        return result;
    }

    KeyguardSecurityView getSecurityView() {
        KeyEvent.Callback childAt = getChildAt(getDisplayedChild());
        if (childAt instanceof KeyguardSecurityView) {
            return (KeyguardSecurityView) childAt;
        }
        return null;
    }

    @Override
    public void setKeyguardCallback(KeyguardSecurityCallback callback) {
        KeyguardSecurityView ksv = getSecurityView();
        if (ksv != null) {
            ksv.setKeyguardCallback(callback);
        }
    }

    @Override
    public void setLockPatternUtils(LockPatternUtils utils) {
        KeyguardSecurityView ksv = getSecurityView();
        if (ksv != null) {
            ksv.setLockPatternUtils(utils);
        }
    }

    @Override
    public void onPause() {
        KeyguardSecurityView ksv = getSecurityView();
        if (ksv != null) {
            ksv.onPause();
        }
    }

    @Override
    public void onResume(int reason) {
        KeyguardSecurityView ksv = getSecurityView();
        if (ksv != null) {
            ksv.onResume(reason);
        }
    }

    @Override
    public boolean needsInput() {
        KeyguardSecurityView ksv = getSecurityView();
        if (ksv != null) {
            return ksv.needsInput();
        }
        return false;
    }

    @Override
    public void showUsabilityHint() {
        KeyguardSecurityView ksv = getSecurityView();
        if (ksv != null) {
            ksv.showUsabilityHint();
        }
    }

    @Override
    public void showBouncer(int duration) {
        KeyguardSecurityView active = getSecurityView();
        for (int i = 0; i < getChildCount(); i++) {
            KeyEvent.Callback childAt = getChildAt(i);
            if (childAt instanceof KeyguardSecurityView) {
                KeyguardSecurityView ksv = (KeyguardSecurityView) childAt;
                ksv.showBouncer(ksv == active ? duration : 0);
            }
        }
    }

    @Override
    public void hideBouncer(int duration) {
        KeyguardSecurityView active = getSecurityView();
        for (int i = 0; i < getChildCount(); i++) {
            KeyEvent.Callback childAt = getChildAt(i);
            if (childAt instanceof KeyguardSecurityView) {
                KeyguardSecurityView ksv = (KeyguardSecurityView) childAt;
                ksv.hideBouncer(ksv == active ? duration : 0);
            }
        }
    }

    @Override
    public void startAppearAnimation() {
        KeyguardSecurityView ksv = getSecurityView();
        if (ksv != null) {
            ksv.startAppearAnimation();
        }
    }

    @Override
    public boolean startDisappearAnimation(Runnable finishRunnable) {
        KeyguardSecurityView ksv = getSecurityView();
        if (ksv != null) {
            return ksv.startDisappearAnimation(finishRunnable);
        }
        return false;
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams ? new LayoutParams((LayoutParams) p) : new LayoutParams(p);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int widthMode = View.MeasureSpec.getMode(widthSpec);
        int heightMode = View.MeasureSpec.getMode(heightSpec);
        int widthSize = View.MeasureSpec.getSize(widthSpec);
        int heightSize = View.MeasureSpec.getSize(heightSpec);
        int maxWidth = widthSize;
        int maxHeight = heightSize;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            LayoutParams lp = (LayoutParams) getChildAt(i).getLayoutParams();
            if (lp.maxWidth > 0 && lp.maxWidth < maxWidth) {
                maxWidth = lp.maxWidth;
            }
            if (lp.maxHeight > 0 && lp.maxHeight < maxHeight) {
                maxHeight = lp.maxHeight;
            }
        }
        int wPadding = getPaddingLeft() + getPaddingRight();
        int hPadding = getPaddingTop() + getPaddingBottom();
        int maxWidth2 = maxWidth - wPadding;
        int maxHeight2 = maxHeight - hPadding;
        int width = widthMode == 1073741824 ? widthSize : 0;
        int height = heightMode == 1073741824 ? heightSize : 0;
        for (int i2 = 0; i2 < count; i2++) {
            View child = getChildAt(i2);
            LayoutParams lp2 = (LayoutParams) child.getLayoutParams();
            int childWidthSpec = makeChildMeasureSpec(maxWidth2, lp2.width);
            int childHeightSpec = makeChildMeasureSpec(maxHeight2, lp2.height);
            child.measure(childWidthSpec, childHeightSpec);
            width = Math.max(width, Math.min(child.getMeasuredWidth(), widthSize - wPadding));
            height = Math.max(height, Math.min(child.getMeasuredHeight(), heightSize - hPadding));
        }
        setMeasuredDimension(width + wPadding, height + hPadding);
    }

    private int makeChildMeasureSpec(int maxSize, int childDimen) {
        int mode;
        int size;
        switch (childDimen) {
            case -2:
                mode = Integer.MIN_VALUE;
                size = maxSize;
                break;
            case -1:
                mode = 1073741824;
                size = maxSize;
                break;
            default:
                mode = 1073741824;
                size = Math.min(maxSize, childDimen);
                break;
        }
        return View.MeasureSpec.makeMeasureSpec(size, mode);
    }

    public static class LayoutParams extends FrameLayout.LayoutParams {

        @ViewDebug.ExportedProperty(category = "layout")
        public int maxHeight;

        @ViewDebug.ExportedProperty(category = "layout")
        public int maxWidth;

        public LayoutParams(ViewGroup.LayoutParams other) {
            super(other);
        }

        public LayoutParams(LayoutParams other) {
            super((FrameLayout.LayoutParams) other);
            this.maxWidth = other.maxWidth;
            this.maxHeight = other.maxHeight;
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.KeyguardSecurityViewFlipper_Layout, 0, 0);
            this.maxWidth = a.getDimensionPixelSize(R.styleable.KeyguardSecurityViewFlipper_Layout_layout_maxWidth, 0);
            this.maxHeight = a.getDimensionPixelSize(R.styleable.KeyguardSecurityViewFlipper_Layout_layout_maxHeight, 0);
            a.recycle();
        }
    }
}
