package android.support.v7.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.v4.content.res.ConfigurationHelper;
import android.support.v4.view.ViewCompat;
import android.support.v7.appcompat.R$id;
import android.support.v7.appcompat.R$styleable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

public class ButtonBarLayout extends LinearLayout {
    private boolean mAllowStacking;
    private int mLastWidthSize;

    public ButtonBarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mLastWidthSize = -1;
        boolean allowStackingDefault = ConfigurationHelper.getScreenHeightDp(getResources()) >= 320;
        TypedArray ta = context.obtainStyledAttributes(attrs, R$styleable.ButtonBarLayout);
        this.mAllowStacking = ta.getBoolean(R$styleable.ButtonBarLayout_allowStacking, allowStackingDefault);
        ta.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int initialWidthMeasureSpec;
        boolean stack;
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        if (this.mAllowStacking) {
            if (widthSize > this.mLastWidthSize && isStacked()) {
                setStacked(false);
            }
            this.mLastWidthSize = widthSize;
        }
        boolean needsRemeasure = false;
        if (!isStacked() && View.MeasureSpec.getMode(widthMeasureSpec) == 1073741824) {
            initialWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec(widthSize, Integer.MIN_VALUE);
            needsRemeasure = true;
        } else {
            initialWidthMeasureSpec = widthMeasureSpec;
        }
        super.onMeasure(initialWidthMeasureSpec, heightMeasureSpec);
        if (this.mAllowStacking && !isStacked()) {
            if (Build.VERSION.SDK_INT >= 11) {
                int measuredWidth = ViewCompat.getMeasuredWidthAndState(this);
                int measuredWidthState = measuredWidth & (-16777216);
                stack = measuredWidthState == 16777216;
            } else {
                int childWidthTotal = 0;
                int count = getChildCount();
                for (int i = 0; i < count; i++) {
                    childWidthTotal += getChildAt(i).getMeasuredWidth();
                }
                stack = (getPaddingLeft() + childWidthTotal) + getPaddingRight() > widthSize;
            }
            if (stack) {
                setStacked(true);
                needsRemeasure = true;
            }
        }
        if (!needsRemeasure) {
            return;
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void setStacked(boolean stacked) {
        setOrientation(stacked ? 1 : 0);
        setGravity(stacked ? 5 : 80);
        View spacer = findViewById(R$id.spacer);
        if (spacer != null) {
            spacer.setVisibility(stacked ? 8 : 4);
        }
        int childCount = getChildCount();
        for (int i = childCount - 2; i >= 0; i--) {
            bringChildToFront(getChildAt(i));
        }
    }

    private boolean isStacked() {
        return getOrientation() == 1;
    }
}
