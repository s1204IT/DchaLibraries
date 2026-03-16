package com.android.contacts.common.list;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import com.android.contacts.R;

public class ViewPagerTabStrip extends LinearLayout {
    private int mIndexForSelection;
    private final Paint mSelectedUnderlinePaint;
    private int mSelectedUnderlineThickness;
    private float mSelectionOffset;

    public ViewPagerTabStrip(Context context) {
        this(context, null);
    }

    public ViewPagerTabStrip(Context context, AttributeSet attrs) {
        super(context, attrs);
        Resources res = context.getResources();
        this.mSelectedUnderlineThickness = res.getDimensionPixelSize(R.dimen.tab_selected_underline_height);
        int underlineColor = res.getColor(R.color.tab_selected_underline_color);
        int backgroundColor = res.getColor(R.color.actionbar_background_color);
        this.mSelectedUnderlinePaint = new Paint();
        this.mSelectedUnderlinePaint.setColor(underlineColor);
        setBackgroundColor(backgroundColor);
        setWillNotDraw(false);
    }

    void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        this.mIndexForSelection = position;
        this.mSelectionOffset = positionOffset;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        boolean hasNextTab;
        int childCount = getChildCount();
        if (childCount > 0) {
            View selectedTitle = getChildAt(this.mIndexForSelection);
            int selectedLeft = selectedTitle.getLeft();
            int selectedRight = selectedTitle.getRight();
            boolean isRtl = isRtl();
            if (isRtl) {
                hasNextTab = this.mIndexForSelection > 0;
            } else {
                hasNextTab = this.mIndexForSelection < getChildCount() + (-1);
            }
            if (this.mSelectionOffset > 0.0f && hasNextTab) {
                View nextTitle = getChildAt((isRtl ? -1 : 1) + this.mIndexForSelection);
                int nextLeft = nextTitle.getLeft();
                int nextRight = nextTitle.getRight();
                selectedLeft = (int) ((this.mSelectionOffset * nextLeft) + ((1.0f - this.mSelectionOffset) * selectedLeft));
                selectedRight = (int) ((this.mSelectionOffset * nextRight) + ((1.0f - this.mSelectionOffset) * selectedRight));
            }
            int height = getHeight();
            canvas.drawRect(selectedLeft, height - this.mSelectedUnderlineThickness, selectedRight, height, this.mSelectedUnderlinePaint);
        }
    }

    private boolean isRtl() {
        return getLayoutDirection() == 1;
    }
}
