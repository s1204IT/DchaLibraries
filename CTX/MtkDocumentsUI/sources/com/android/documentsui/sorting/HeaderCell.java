package com.android.documentsui.sorting;

import android.animation.AnimatorInflater;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.documentsui.R;

public class HeaderCell extends LinearLayout {
    private int mCurDirection;

    public HeaderCell(Context context) {
        this(context, null);
    }

    public HeaderCell(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mCurDirection = 0;
        LayoutTransition layoutTransition = getLayoutTransition();
        layoutTransition.setDuration(100L);
        layoutTransition.setStartDelay(0, 0L);
        layoutTransition.setStartDelay(1, 0L);
        layoutTransition.setStartDelay(4, 0L);
    }

    void onBind(SortDimension sortDimension) {
        setVisibility(sortDimension.getVisibility());
        if (sortDimension.getVisibility() == 0) {
            TextView textView = (TextView) findViewById(R.id.label);
            textView.setText(sortDimension.getLabelId());
            switch (sortDimension.getDataType()) {
                case 0:
                    setDataTypeString(textView);
                    break;
                case 1:
                    setDataTypeNumber(textView);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown column data type: " + sortDimension.getDataType() + ".");
            }
            if (this.mCurDirection != sortDimension.getSortDirection()) {
                ImageView imageView = (ImageView) findViewById(R.id.sort_arrow);
                switch (sortDimension.getSortDirection()) {
                    case 0:
                        imageView.setVisibility(8);
                        break;
                    case 1:
                        showArrow(imageView, R.animator.arrow_rotate_up, R.string.sort_direction_ascending);
                        break;
                    case 2:
                        showArrow(imageView, R.animator.arrow_rotate_down, R.string.sort_direction_descending);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown sort direction: " + sortDimension.getSortDirection() + ".");
                }
                this.mCurDirection = sortDimension.getSortDirection();
            }
        }
    }

    private void showArrow(ImageView imageView, int i, int i2) {
        imageView.setVisibility(0);
        imageView.setContentDescription(getContext().getString(i2));
        ObjectAnimator objectAnimator = (ObjectAnimator) AnimatorInflater.loadAnimator(getContext(), i);
        objectAnimator.setTarget(imageView.getDrawable().mutate());
        objectAnimator.start();
    }

    private void setDataTypeNumber(View view) {
        view.setTextAlignment(6);
        setGravity(8388629);
    }

    private void setDataTypeString(View view) {
        view.setTextAlignment(5);
        setGravity(8388627);
    }
}
