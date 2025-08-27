package com.android.settings.fuelgauge;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.View;

/* loaded from: classes.dex */
public class BatteryActiveView extends View {
    private final Paint mPaint;
    private BatteryActiveProvider mProvider;

    public interface BatteryActiveProvider {
        SparseIntArray getColorArray();

        long getPeriod();

        boolean hasData();
    }

    public BatteryActiveView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mPaint = new Paint();
    }

    public void setProvider(BatteryActiveProvider batteryActiveProvider) {
        this.mProvider = batteryActiveProvider;
        if (getWidth() != 0) {
            postInvalidate();
        }
    }

    @Override // android.view.View
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        if (getWidth() != 0) {
            postInvalidate();
        }
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        if (this.mProvider == null) {
            return;
        }
        SparseIntArray colorArray = this.mProvider.getColorArray();
        float period = this.mProvider.getPeriod();
        int i = 0;
        while (i < colorArray.size() - 1) {
            int i2 = i + 1;
            drawColor(canvas, colorArray.keyAt(i), colorArray.keyAt(i2), colorArray.valueAt(i), period);
            i = i2;
        }
    }

    private void drawColor(Canvas canvas, int i, int i2, int i3, float f) {
        if (i3 == 0) {
            return;
        }
        this.mPaint.setColor(i3);
        canvas.drawRect((i / f) * getWidth(), 0.0f, (i2 / f) * getWidth(), getHeight(), this.mPaint);
    }
}
