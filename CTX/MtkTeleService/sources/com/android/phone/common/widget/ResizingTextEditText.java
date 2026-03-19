package com.android.phone.common.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.EditText;
import com.android.phone.common.R;
import com.android.phone.common.util.ViewUtil;

public class ResizingTextEditText extends EditText {
    private boolean mIsResizeEnabled;
    private final int mMinTextSize;
    private final int mOriginalTextSize;

    public ResizingTextEditText(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mIsResizeEnabled = true;
        this.mOriginalTextSize = (int) getTextSize();
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.ResizingText);
        this.mMinTextSize = (int) typedArrayObtainStyledAttributes.getDimension(0, this.mOriginalTextSize);
        typedArrayObtainStyledAttributes.recycle();
    }

    @Override
    protected void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        super.onTextChanged(charSequence, i, i2, i3);
        if (this.mIsResizeEnabled) {
            ViewUtil.resizeText(this, this.mOriginalTextSize, this.mMinTextSize);
        }
    }

    @Override
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        if (this.mIsResizeEnabled) {
            ViewUtil.resizeText(this, this.mOriginalTextSize, this.mMinTextSize);
        }
    }

    public void setResizeEnabled(boolean z) {
        this.mIsResizeEnabled = z;
    }
}
