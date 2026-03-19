package com.android.setupwizardlib.view;

import android.R;
import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.LinearLayout;

public class CheckableLinearLayout extends LinearLayout implements Checkable {
    private boolean mChecked;

    public CheckableLinearLayout(Context context) {
        super(context);
        this.mChecked = false;
        setFocusable(true);
    }

    public CheckableLinearLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mChecked = false;
        setFocusable(true);
    }

    @TargetApi(11)
    public CheckableLinearLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mChecked = false;
        setFocusable(true);
    }

    @TargetApi(21)
    public CheckableLinearLayout(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mChecked = false;
        setFocusable(true);
    }

    @Override
    protected int[] onCreateDrawableState(int i) {
        if (this.mChecked) {
            return mergeDrawableStates(super.onCreateDrawableState(i + 1), new int[]{R.attr.state_checked});
        }
        return super.onCreateDrawableState(i);
    }

    @Override
    public void setChecked(boolean z) {
        this.mChecked = z;
        refreshDrawableState();
    }

    @Override
    public boolean isChecked() {
        return this.mChecked;
    }

    @Override
    public void toggle() {
        setChecked(!isChecked());
    }
}
