package com.android.launcher3;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class InsettableFrameLayout extends FrameLayout implements ViewGroup.OnHierarchyChangeListener, Insettable {
    protected Rect mInsets;

    public InsettableFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mInsets = new Rect();
        setOnHierarchyChangeListener(this);
    }

    public void setFrameLayoutChildInsets(View view, Rect newInsets, Rect oldInsets) {
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        if (view instanceof Insettable) {
            ((Insettable) view).setInsets(newInsets);
        } else if (!lp.ignoreInsets) {
            lp.topMargin += newInsets.top - oldInsets.top;
            lp.leftMargin += newInsets.left - oldInsets.left;
            lp.rightMargin += newInsets.right - oldInsets.right;
            lp.bottomMargin += newInsets.bottom - oldInsets.bottom;
        }
        view.setLayoutParams(lp);
    }

    @Override
    public void setInsets(Rect insets) {
        int n = getChildCount();
        for (int i = 0; i < n; i++) {
            View child = getChildAt(i);
            setFrameLayoutChildInsets(child, insets, this.mInsets);
        }
        this.mInsets.set(insets);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    public LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-2, -2);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    public LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    public static class LayoutParams extends FrameLayout.LayoutParams {
        boolean ignoreInsets;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            this.ignoreInsets = false;
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.InsettableFrameLayout_Layout);
            this.ignoreInsets = a.getBoolean(0, false);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
            this.ignoreInsets = false;
        }

        public LayoutParams(ViewGroup.LayoutParams lp) {
            super(lp);
            this.ignoreInsets = false;
        }
    }

    public void onChildViewAdded(View parent, View child) {
        setFrameLayoutChildInsets(child, this.mInsets, new Rect());
    }

    public void onChildViewRemoved(View parent, View child) {
    }
}
