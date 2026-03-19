package com.android.printspooler.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Spinner;

public class ClickInterceptSpinner extends Spinner {
    private View.OnClickListener mListener;

    public ClickInterceptSpinner(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public void setPerformClickListener(View.OnClickListener onClickListener) {
        this.mListener = onClickListener;
    }

    @Override
    public boolean performClick() {
        if (this.mListener != null) {
            this.mListener.onClick(this);
        }
        return super.performClick();
    }
}
