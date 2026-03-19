package com.android.deskclock.widget;

import android.R;
import android.content.Context;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

public class AutoSizingTextView extends AppCompatTextView {
    private final TextSizeHelper mTextSizeHelper;

    public AutoSizingTextView(Context context) {
        this(context, null);
    }

    public AutoSizingTextView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R.attr.textViewStyle);
    }

    public AutoSizingTextView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mTextSizeHelper = new TextSizeHelper(this);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        this.mTextSizeHelper.onMeasure(i, i2);
        super.onMeasure(i, i2);
    }

    @Override
    protected void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        super.onTextChanged(charSequence, i, i2, i3);
        if (this.mTextSizeHelper != null) {
            this.mTextSizeHelper.onTextChanged(i2, i3);
        } else {
            requestLayout();
        }
    }

    @Override
    public void requestLayout() {
        if (this.mTextSizeHelper == null || !this.mTextSizeHelper.shouldIgnoreRequestLayout()) {
            super.requestLayout();
        }
    }
}
