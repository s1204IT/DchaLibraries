package com.android.phone.common.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.EditText;
import com.android.phone.common.R;
import com.android.phone.common.util.ViewUtil;

public class ResizingTextEditText extends EditText {
    private final int mMinTextSize;
    private final int mOriginalTextSize;

    public ResizingTextEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mOriginalTextSize = (int) getTextSize();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ResizingText);
        this.mMinTextSize = (int) a.getDimension(0, this.mOriginalTextSize);
        a.recycle();
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        ViewUtil.resizeText(this, this.mOriginalTextSize, this.mMinTextSize);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        ViewUtil.resizeText(this, this.mOriginalTextSize, this.mMinTextSize);
    }
}
