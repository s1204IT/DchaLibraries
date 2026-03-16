package com.android.phone.common.dialpad;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.TextView;

public class DialpadTextView extends TextView {
    private Rect mTextBounds;
    private String mTextStr;

    public DialpadTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mTextBounds = new Rect();
    }

    @Override
    public void draw(Canvas canvas) {
        Paint paint = getPaint();
        paint.setColor(getCurrentTextColor());
        canvas.drawText(this.mTextStr, -this.mTextBounds.left, -this.mTextBounds.top, paint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        this.mTextStr = getText().toString();
        getPaint().getTextBounds(this.mTextStr, 0, this.mTextStr.length(), this.mTextBounds);
        int width = resolveSize(this.mTextBounds.width(), widthMeasureSpec);
        int height = resolveSize(this.mTextBounds.height(), heightMeasureSpec);
        setMeasuredDimension(width, height);
    }
}
