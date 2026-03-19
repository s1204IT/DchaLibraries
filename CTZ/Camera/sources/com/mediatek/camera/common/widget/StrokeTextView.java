package com.mediatek.camera.common.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.TextView;

public class StrokeTextView extends TextView {
    private TextView mBorderText;

    public StrokeTextView(Context context) {
        super(context);
        this.mBorderText = null;
        this.mBorderText = new TextView(context);
        init();
    }

    public StrokeTextView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mBorderText = null;
        this.mBorderText = new TextView(context, attributeSet);
        init();
    }

    public StrokeTextView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mBorderText = null;
        this.mBorderText = new TextView(context, attributeSet, i);
        init();
    }

    private void init() {
        TextPaint paint = this.mBorderText.getPaint();
        paint.setStrokeWidth(1.0f);
        paint.setStyle(Paint.Style.STROKE);
        this.mBorderText.setTextColor(Color.rgb(0, 0, 0));
        this.mBorderText.setAlpha(0.5f);
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams layoutParams) {
        super.setLayoutParams(layoutParams);
        this.mBorderText.setLayoutParams(layoutParams);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        CharSequence text = this.mBorderText.getText();
        if (text == null || !text.equals(getText())) {
            this.mBorderText.setText(getText());
            postInvalidate();
        }
        super.onMeasure(i, i2);
        this.mBorderText.measure(i, i2);
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        this.mBorderText.layout(i, i2, i3, i4);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        this.mBorderText.draw(canvas);
        super.onDraw(canvas);
    }
}
