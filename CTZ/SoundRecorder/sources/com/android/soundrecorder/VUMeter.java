package com.android.soundrecorder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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

    public VUMeter(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }

    void init(Context context) {
        setBackgroundDrawable(context.getResources().getDrawable(R.drawable.vumeter));
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
        float maxAmplitude = this.mRecorder != null ? 0.3926991f + ((2.3561947f * this.mRecorder.getMaxAmplitude()) / 32768.0f) : 0.3926991f;
        if (maxAmplitude > this.mCurrentAngle) {
            this.mCurrentAngle = maxAmplitude;
        } else {
            this.mCurrentAngle = Math.max(maxAmplitude, this.mCurrentAngle - 0.18f);
        }
        this.mCurrentAngle = Math.min(2.7488937f, this.mCurrentAngle);
        float width = getWidth();
        float height = getHeight();
        float f = width / 2.0f;
        float f2 = (height - 3.5f) - 10.0f;
        float f3 = (height * 4.0f) / 5.0f;
        float fSin = (float) Math.sin(this.mCurrentAngle);
        float fCos = f - (((float) Math.cos(this.mCurrentAngle)) * f3);
        float f4 = f2 - (f3 * fSin);
        float f5 = f + 2.0f;
        float f6 = f2 + 2.0f;
        canvas.drawLine(fCos + 2.0f, f4 + 2.0f, f5, f6, this.mShadow);
        canvas.drawCircle(f5, f6, 3.5f, this.mShadow);
        canvas.drawLine(fCos, f4, f, f2, this.mPaint);
        canvas.drawCircle(f, f2, 3.5f, this.mPaint);
        if (this.mRecorder != null && this.mRecorder.state() == 1) {
            postInvalidateDelayed(70L);
        }
    }
}
