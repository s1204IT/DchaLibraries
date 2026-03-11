package com.android.setupwizardlib.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import com.android.setupwizardlib.R;

public class ButtonBarLayout extends LinearLayout {
    private int mOriginalPaddingLeft;
    private int mOriginalPaddingRight;
    private boolean mStacked;

    public ButtonBarLayout(Context context) {
        super(context);
        this.mStacked = false;
    }

    public ButtonBarLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mStacked = false;
    }

    @Override
    protected void onMeasure(int i, int i2) {
        boolean z;
        int iMakeMeasureSpec;
        int size = View.MeasureSpec.getSize(i);
        setStacked(false);
        if (View.MeasureSpec.getMode(i) != 1073741824) {
            z = false;
            iMakeMeasureSpec = i;
        } else {
            iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
            z = true;
        }
        super.onMeasure(iMakeMeasureSpec, i2);
        if (getMeasuredWidth() > size) {
            setStacked(true);
            z = true;
        }
        if (z) {
            super.onMeasure(i, i2);
        }
    }

    private void setStacked(boolean z) {
        if (this.mStacked == z) {
            return;
        }
        this.mStacked = z;
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) childAt.getLayoutParams();
            if (z) {
                childAt.setTag(R.id.suw_original_weight, Float.valueOf(layoutParams.weight));
                layoutParams.weight = 0.0f;
            } else {
                Float f = (Float) childAt.getTag(R.id.suw_original_weight);
                if (f != null) {
                    layoutParams.weight = f.floatValue();
                }
            }
            childAt.setLayoutParams(layoutParams);
        }
        setOrientation(z ? 1 : 0);
        for (int i2 = childCount - 1; i2 >= 0; i2--) {
            bringChildToFront(getChildAt(i2));
        }
        if (z) {
            this.mOriginalPaddingLeft = getPaddingLeft();
            this.mOriginalPaddingRight = getPaddingRight();
            int iMax = Math.max(this.mOriginalPaddingLeft, this.mOriginalPaddingRight);
            setPadding(iMax, getPaddingTop(), iMax, getPaddingBottom());
            return;
        }
        setPadding(this.mOriginalPaddingLeft, getPaddingTop(), this.mOriginalPaddingRight, getPaddingBottom());
    }
}
