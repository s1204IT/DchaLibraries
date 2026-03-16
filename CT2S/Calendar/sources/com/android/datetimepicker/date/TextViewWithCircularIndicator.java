package com.android.datetimepicker.date;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.datetimepicker.R;

public class TextViewWithCircularIndicator extends TextView {
    private final int mCircleColor;
    Paint mCirclePaint;
    private boolean mDrawCircle;
    private final String mItemIsSelectedText;
    private final int mRadius;

    public TextViewWithCircularIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mCirclePaint = new Paint();
        Resources res = context.getResources();
        this.mCircleColor = res.getColor(R.color.blue);
        this.mRadius = res.getDimensionPixelOffset(R.dimen.month_select_circle_radius);
        this.mItemIsSelectedText = context.getResources().getString(R.string.item_is_selected);
        init();
    }

    private void init() {
        this.mCirclePaint.setFakeBoldText(true);
        this.mCirclePaint.setAntiAlias(true);
        this.mCirclePaint.setColor(this.mCircleColor);
        this.mCirclePaint.setTextAlign(Paint.Align.CENTER);
        this.mCirclePaint.setStyle(Paint.Style.FILL);
        this.mCirclePaint.setAlpha(60);
    }

    public void drawIndicator(boolean drawCircle) {
        this.mDrawCircle = drawCircle;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mDrawCircle) {
            int width = getWidth();
            int height = getHeight();
            int radius = Math.min(width, height) / 2;
            canvas.drawCircle(width / 2, height / 2, radius, this.mCirclePaint);
        }
    }

    @Override
    public CharSequence getContentDescription() {
        CharSequence itemText = getText();
        return this.mDrawCircle ? String.format(this.mItemIsSelectedText, itemText) : itemText;
    }
}
