package com.android.gallery3d.filtershow.ui;

import android.content.Context;
import android.content.res.TypedArray;
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

    public static void setTextSize(int value) {
        mTextSize = value;
    }

    public static void setTrianglePadding(int value) {
        mTrianglePadding = value;
    }

    public static void setTriangleSize(int value) {
        mTriangleSize = value;
    }

    public FramedTextButton(Context context) {
        this(context, null);
    }

    public FramedTextButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mText = null;
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ImageButtonTitle);
            this.mText = a.getString(1);
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        gPaint.setARGB(96, 255, 255, 255);
        gPaint.setStrokeWidth(2.0f);
        gPaint.setStyle(Paint.Style.STROKE);
        int w = getWidth();
        int h = getHeight();
        canvas.drawRect(mTextPadding, mTextPadding, w - mTextPadding, h - mTextPadding, gPaint);
        gPath.reset();
        gPath.moveTo(((w - mTextPadding) - mTrianglePadding) - mTriangleSize, (h - mTextPadding) - mTrianglePadding);
        gPath.lineTo((w - mTextPadding) - mTrianglePadding, ((h - mTextPadding) - mTrianglePadding) - mTriangleSize);
        gPath.lineTo((w - mTextPadding) - mTrianglePadding, (h - mTextPadding) - mTrianglePadding);
        gPath.close();
        gPaint.setARGB(128, 255, 255, 255);
        gPaint.setStrokeWidth(1.0f);
        gPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawPath(gPath, gPaint);
        if (this.mText != null) {
            gPaint.reset();
            gPaint.setARGB(255, 255, 255, 255);
            gPaint.setTextSize(mTextSize);
            float textWidth = gPaint.measureText(this.mText);
            Rect bounds = new Rect();
            gPaint.getTextBounds(this.mText, 0, this.mText.length(), bounds);
            int x = (int) ((w - textWidth) / 2.0f);
            int y = (bounds.height() + h) / 2;
            canvas.drawText(this.mText, x, y, gPaint);
        }
    }
}
