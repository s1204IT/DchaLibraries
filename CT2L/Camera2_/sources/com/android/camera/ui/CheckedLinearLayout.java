package com.android.camera.ui;

import android.R;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.LinearLayout;

public class CheckedLinearLayout extends LinearLayout implements Checkable {
    private static final int[] CHECKED_STATE_SET = {R.attr.state_checked};
    private boolean mChecked;

    public CheckedLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean isChecked() {
        return this.mChecked;
    }

    @Override
    public void setChecked(boolean checked) {
        if (this.mChecked != checked) {
            this.mChecked = checked;
            refreshDrawableState();
        }
    }

    @Override
    public void toggle() {
        setChecked(!this.mChecked);
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (this.mChecked) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }
}
