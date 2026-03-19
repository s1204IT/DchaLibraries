package com.android.gallery3d.filtershow.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ImageButton;
import com.android.gallery3d.R;

public class FramedTextButton extends ImageButton {
    private String mText;
    private static int mTextSize = 24;
    private static int mTextPadding = 20;
    private static Paint gPaint = new Paint();
    private static Path gPath = new Path();
    private static int mTrianglePadding = 2;
    private static int mTriangleSize = 30;

    public static void setTextSize(int i) {
        mTextSize = i;
    }

    public static void setTrianglePadding(int i) {
        mTrianglePadding = i;
    }

    public static void setTriangleSize(int i) {
        mTriangleSize = i;
    }

    public FramedTextButton(Context context) {
        this(context, null);
    }

    public FramedTextButton(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mText = null;
        if (attributeSet == null) {
            return;
        }
        this.mText = getContext().obtainStyledAttributes(attributeSet, R.styleable.ImageButtonTitle).getString(1);
    }

    @Override
    public void onDraw(Canvas canvas) {
        gPaint.setARGB(96, 255, 255, 255);
        gPaint.setStrokeWidth(2.0f);
        gPaint.setStyle(Paint.Style.STROKE);
        int width = getWidth();
        int height = getHeight();
        canvas.drawRect(mTextPadding, mTextPadding, width - mTextPadding, height - mTextPadding, gPaint);
        gPath.reset();
        gPath.moveTo(((width - mTextPadding) - mTrianglePadding) - mTriangleSize, (height - mTextPadding) - mTrianglePadding);
        gPath.lineTo((width - mTextPadding) - mTrianglePadding, ((height - mTextPadding) - mTrianglePadding) - mTriangleSize);
        gPath.lineTo((width - mTextPadding) - mTrianglePadding, (height - mTextPadding) - mTrianglePadding);
        gPath.close();
        gPaint.setARGB(128, 255, 255, 255);
        gPaint.setStrokeWidth(1.0f);
        gPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawPath(gPath, gPaint);
        if (this.mText != null) {
            gPaint.reset();
            gPaint.setARGB(255, 255, 255, 255);
            gPaint.setTextSize(mTextSize);
            float fMeasureText = gPaint.measureText(this.mText);
            gPaint.getTextBounds(this.mText, 0, this.mText.length(), new Rect());
            canvas.drawText(this.mText, (int) ((width - fMeasureText) / 2.0f), (height + r4.height()) / 2, gPaint);
        }
    }
}
