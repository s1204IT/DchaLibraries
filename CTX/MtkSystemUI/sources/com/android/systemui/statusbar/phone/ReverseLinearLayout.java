package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import java.util.ArrayList;

public class ReverseLinearLayout extends LinearLayout {
    private boolean mIsAlternativeOrder;
    private boolean mIsLayoutReverse;

    public interface Reversable {
        void reverse(boolean z);
    }

    public ReverseLinearLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        updateOrder();
    }

    @Override
    public void addView(View view) {
        reverseParams(view.getLayoutParams(), view, this.mIsLayoutReverse);
        if (this.mIsLayoutReverse) {
            super.addView(view, 0);
        } else {
            super.addView(view);
        }
    }

    @Override
    public void addView(View view, ViewGroup.LayoutParams layoutParams) {
        reverseParams(layoutParams, view, this.mIsLayoutReverse);
        if (this.mIsLayoutReverse) {
            super.addView(view, 0, layoutParams);
        } else {
            super.addView(view, layoutParams);
        }
    }

    @Override
    public void onRtlPropertiesChanged(int i) {
        super.onRtlPropertiesChanged(i);
        updateOrder();
    }

    public void setAlternativeOrder(boolean z) {
        this.mIsAlternativeOrder = z;
        updateOrder();
    }

    private void updateOrder() {
        boolean z = (getLayoutDirection() == 1) ^ this.mIsAlternativeOrder;
        if (this.mIsLayoutReverse != z) {
            int childCount = getChildCount();
            ArrayList arrayList = new ArrayList(childCount);
            for (int i = 0; i < childCount; i++) {
                arrayList.add(getChildAt(i));
            }
            removeAllViews();
            for (int i2 = childCount - 1; i2 >= 0; i2--) {
                super.addView((View) arrayList.get(i2));
            }
            this.mIsLayoutReverse = z;
        }
    }

    private static void reverseParams(ViewGroup.LayoutParams layoutParams, View view, boolean z) {
        if (view instanceof Reversable) {
            ((Reversable) view).reverse(z);
        }
        if (view.getPaddingLeft() == view.getPaddingRight() && view.getPaddingTop() == view.getPaddingBottom()) {
            view.setPadding(view.getPaddingTop(), view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingLeft());
        }
        if (layoutParams == null) {
            return;
        }
        int i = layoutParams.width;
        layoutParams.width = layoutParams.height;
        layoutParams.height = i;
    }

    public static class ReverseRelativeLayout extends RelativeLayout implements Reversable {
        private int mDefaultGravity;

        public ReverseRelativeLayout(Context context) {
            super(context);
            this.mDefaultGravity = 0;
        }

        @Override
        public void reverse(boolean z) {
            updateGravity(z);
            ReverseLinearLayout.reverseGroup(this, z);
        }

        public void setDefaultGravity(int i) {
            this.mDefaultGravity = i;
        }

        public void updateGravity(boolean z) {
            if (this.mDefaultGravity == 48 || this.mDefaultGravity == 80) {
                int i = this.mDefaultGravity;
                if (z) {
                    i = this.mDefaultGravity == 48 ? 80 : 48;
                }
                if (getGravity() != i) {
                    setGravity(i);
                }
            }
        }
    }

    private static void reverseGroup(ViewGroup viewGroup, boolean z) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View childAt = viewGroup.getChildAt(i);
            reverseParams(childAt.getLayoutParams(), childAt, z);
            if (childAt instanceof ViewGroup) {
                reverseGroup((ViewGroup) childAt, z);
            }
        }
    }
}
