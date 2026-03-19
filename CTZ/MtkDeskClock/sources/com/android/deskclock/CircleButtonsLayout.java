package com.android.deskclock;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

public class CircleButtonsLayout extends FrameLayout {
    private View mCircleView;
    private float mDiamOffset;
    private TextView mLabel;
    private Button mResetAddButton;

    public CircleButtonsLayout(Context context) {
        this(context, null);
    }

    public CircleButtonsLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        Resources resources = getContext().getResources();
        this.mDiamOffset = Utils.calculateRadiusOffset(resources.getDimension(R.dimen.circletimer_circle_size), resources.getDimension(R.dimen.circletimer_dot_size), resources.getDimension(R.dimen.circletimer_marker_size)) * 2.0f;
    }

    @Override
    public void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        remeasureViews();
        super.onMeasure(i, i2);
    }

    protected void remeasureViews() {
        if (this.mLabel == null) {
            this.mCircleView = findViewById(R.id.timer_time);
            this.mLabel = (TextView) findViewById(R.id.timer_label);
            this.mResetAddButton = (Button) findViewById(R.id.reset_add);
        }
        int measuredWidth = this.mCircleView.getMeasuredWidth();
        int measuredHeight = this.mCircleView.getMeasuredHeight();
        int iMin = Math.min(measuredWidth, measuredHeight);
        int i = (int) (iMin - this.mDiamOffset);
        if (this.mResetAddButton != null) {
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) this.mResetAddButton.getLayoutParams();
            marginLayoutParams.bottomMargin = i / 6;
            if (iMin == measuredWidth) {
                marginLayoutParams.bottomMargin += (measuredHeight - measuredWidth) / 2;
            }
        }
        if (this.mLabel != null) {
            ViewGroup.MarginLayoutParams marginLayoutParams2 = (ViewGroup.MarginLayoutParams) this.mLabel.getLayoutParams();
            marginLayoutParams2.topMargin = i / 6;
            if (iMin == measuredWidth) {
                marginLayoutParams2.topMargin += (measuredHeight - measuredWidth) / 2;
            }
            int i2 = i / 2;
            int i3 = (measuredHeight / 2) - marginLayoutParams2.topMargin;
            this.mLabel.setMaxWidth((int) (2.0d * Math.sqrt((i2 + i3) * (i2 - i3))));
        }
    }
}
