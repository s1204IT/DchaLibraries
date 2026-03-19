package com.android.deskclock.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextClock;
import android.widget.TextView;

public class AutoSizingTextClock extends TextClock {
    private boolean mSuppressLayout;
    private final TextSizeHelper mTextSizeHelper;

    public AutoSizingTextClock(Context context) {
        this(context, null);
    }

    public AutoSizingTextClock(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public AutoSizingTextClock(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mSuppressLayout = false;
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
            if (i2 != i3) {
                this.mSuppressLayout = false;
            }
            this.mTextSizeHelper.onTextChanged(i2, i3);
            return;
        }
        requestLayout();
    }

    @Override
    public void setText(CharSequence charSequence, TextView.BufferType bufferType) {
        this.mSuppressLayout = true;
        super.setText(charSequence, bufferType);
        this.mSuppressLayout = false;
    }

    @Override
    public void requestLayout() {
        if ((this.mTextSizeHelper == null || !this.mTextSizeHelper.shouldIgnoreRequestLayout()) && !this.mSuppressLayout) {
            super.requestLayout();
        }
    }
}
