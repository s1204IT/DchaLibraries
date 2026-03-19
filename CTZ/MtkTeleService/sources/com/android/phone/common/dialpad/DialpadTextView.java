package com.android.phone.common.dialpad;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.widget.TextView;

public class DialpadTextView extends TextView {
    private Rect mTextBounds;
    private String mTextStr;

    public DialpadTextView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mTextBounds = new Rect();
    }

    @Override
    public void draw(Canvas canvas) {
        TextPaint paint = getPaint();
        paint.setColor(getCurrentTextColor());
        canvas.drawText(this.mTextStr, -this.mTextBounds.left, -this.mTextBounds.top, paint);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        this.mTextStr = getText().toString();
        getPaint().getTextBounds(this.mTextStr, 0, this.mTextStr.length(), this.mTextBounds);
        setMeasuredDimension(resolveSize(this.mTextBounds.width(), i), resolveSize(this.mTextBounds.height(), i2));
    }
}
