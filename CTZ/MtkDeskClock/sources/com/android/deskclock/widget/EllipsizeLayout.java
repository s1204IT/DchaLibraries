package com.android.deskclock.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class EllipsizeLayout extends LinearLayout {
    public EllipsizeLayout(Context context) {
        this(context, null);
    }

    public EllipsizeLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        boolean z;
        boolean z2;
        TextView textView;
        if (getOrientation() == 0 && View.MeasureSpec.getMode(i) == 1073741824) {
            int childCount = getChildCount();
            int size = View.MeasureSpec.getSize(i);
            int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.getSize(i), 0);
            TextView textView2 = null;
            int i3 = 0;
            boolean z3 = false;
            int measuredWidth = 0;
            while (true) {
                if (i3 >= childCount || z3) {
                    break;
                }
                View childAt = getChildAt(i3);
                if (childAt != null && childAt.getVisibility() != 8) {
                    if (childAt instanceof TextView) {
                        TextView textView3 = (TextView) childAt;
                        if (textView3.getEllipsize() == null) {
                            z2 = z3;
                            textView = textView2;
                        } else if (textView2 == null) {
                            textView3.setMaxWidth(Integer.MAX_VALUE);
                            z2 = z3;
                            textView = textView3;
                        } else {
                            textView = textView2;
                            z2 = true;
                        }
                        measureChildWithMargins(childAt, iMakeMeasureSpec, 0, i2, 0);
                        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) childAt.getLayoutParams();
                        if (layoutParams == null) {
                            z3 = true;
                        } else {
                            z = layoutParams.weight > 0.0f;
                            measuredWidth += childAt.getMeasuredWidth() + layoutParams.leftMargin + layoutParams.rightMargin;
                            z3 = z2 | z;
                        }
                        textView2 = textView;
                    }
                }
                i3++;
            }
            if (textView2 != null && measuredWidth != 0) {
                z = false;
            }
            if (!(z3 | z) && measuredWidth > size) {
                int measuredWidth2 = textView2.getMeasuredWidth() - (measuredWidth - size);
                if (measuredWidth2 < 0) {
                    measuredWidth2 = 0;
                }
                textView2.setMaxWidth(measuredWidth2);
            }
        }
        super.onMeasure(i, i2);
    }
}
