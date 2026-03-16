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

    public EllipsizeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (getOrientation() == 0 && View.MeasureSpec.getMode(widthMeasureSpec) == 1073741824) {
            int totalLength = 0;
            boolean outOfSpec = false;
            TextView ellipsizeView = null;
            int count = getChildCount();
            int parentWidth = View.MeasureSpec.getSize(widthMeasureSpec);
            int queryWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.getSize(widthMeasureSpec), 0);
            for (int ii = 0; ii < count && !outOfSpec; ii++) {
                View child = getChildAt(ii);
                if (child != null && child.getVisibility() != 8) {
                    if (child instanceof TextView) {
                        TextView tv = (TextView) child;
                        if (tv.getEllipsize() != null) {
                            if (ellipsizeView == null) {
                                ellipsizeView = tv;
                                ellipsizeView.setMaxWidth(Integer.MAX_VALUE);
                            } else {
                                outOfSpec = true;
                            }
                        }
                    }
                    measureChildWithMargins(child, queryWidthMeasureSpec, 0, heightMeasureSpec, 0);
                    LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) child.getLayoutParams();
                    if (layoutParams != null) {
                        outOfSpec |= layoutParams.weight > 0.0f;
                        totalLength += child.getMeasuredWidth() + layoutParams.leftMargin + layoutParams.rightMargin;
                    } else {
                        outOfSpec = true;
                    }
                }
            }
            if (!(outOfSpec | (ellipsizeView == null || totalLength == 0)) && totalLength > parentWidth) {
                int maxWidth = ellipsizeView.getMeasuredWidth() - (totalLength - parentWidth);
                if (maxWidth < 0) {
                    maxWidth = 0;
                }
                ellipsizeView.setMaxWidth(maxWidth);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
