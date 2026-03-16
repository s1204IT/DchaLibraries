package com.android.soundrecorder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

public class VUMeter extends View {
    float mCurrentAngle;
    Paint mPaint;
    Recorder mRecorder;
    Paint mShadow;

    public VUMeter(Context context) {
        super(context);
        init(context);
    }

    public VUMeter(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    void init(Context context) {
        Drawable background = context.getResources().getDrawable(R.drawable.vumeter);
        setBackgroundDrawable(background);
        this.mPaint = new Paint(1);
        this.mPaint.setColor(-1);
        this.mShadow = new Paint(1);
        this.mShadow.setColor(Color.argb(60, 0, 0, 0));
        this.mRecorder = null;
        this.mCurrentAngle = 0.0f;
    }

    public void setRecorder(Recorder recorder) {
        this.mRecorder = recorder;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float angle = this.mRecorder != null ? 0.3926991f + ((2.3561947f * this.mRecorder.getMaxAmplitude()) / 32768.0f) : 0.3926991f;
        if (angle > this.mCurrentAngle) {
            this.mCurrentAngle = angle;
        } else {
            this.mCurrentAngle = Math.max(angle, this.mCurrentAngle - 0.18f);
        }
        this.mCurrentAngle = Math.min(2.7488937f, this.mCurrentAngle);
        float w = getWidth();
        float h = getHeight();
        float pivotX = w / 2.0f;
        float pivotY = (h - 3.5f) - 10.0f;
        float l = (4.0f * h) / 5.0f;
        float sin = (float) Math.sin(this.mCurrentAngle);
        float cos = (float) Math.cos(this.mCurrentAngle);
        float x0 = pivotX - (l * cos);
        float y0 = pivotY - (l * sin);
        canvas.drawLine(x0 + 2.0f, y0 + 2.0f, pivotX + 2.0f, pivotY + 2.0f, this.mShadow);
        canvas.drawCircle(2.0f + pivotX, 2.0f + pivotY, 3.5f, this.mShadow);
        canvas.drawLine(x0, y0, pivotX, pivotY, this.mPaint);
        canvas.drawCircle(pivotX, pivotY, 3.5f, this.mPaint);
        if (this.mRecorder != null && this.mRecorder.state() == 1) {
            postInvalidateDelayed(70L);
        }
    }
}
