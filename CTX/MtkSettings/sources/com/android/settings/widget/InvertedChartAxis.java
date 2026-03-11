package com.android.settings.widget;

import android.content.res.Resources;
import android.text.SpannableStringBuilder;

public class InvertedChartAxis implements ChartAxis {
    private float mSize;
    private final ChartAxis mWrapped;

    public InvertedChartAxis(ChartAxis chartAxis) {
        this.mWrapped = chartAxis;
    }

    @Override
    public boolean setBounds(long j, long j2) {
        return this.mWrapped.setBounds(j, j2);
    }

    @Override
    public boolean setSize(float f) {
        this.mSize = f;
        return this.mWrapped.setSize(f);
    }

    @Override
    public float convertToPoint(long j) {
        return this.mSize - this.mWrapped.convertToPoint(j);
    }

    @Override
    public long convertToValue(float f) {
        return this.mWrapped.convertToValue(this.mSize - f);
    }

    @Override
    public long buildLabel(Resources resources, SpannableStringBuilder spannableStringBuilder, long j) {
        return this.mWrapped.buildLabel(resources, spannableStringBuilder, j);
    }

    @Override
    public float[] getTickPoints() {
        float[] tickPoints = this.mWrapped.getTickPoints();
        for (int i = 0; i < tickPoints.length; i++) {
            tickPoints[i] = this.mSize - tickPoints[i];
        }
        return tickPoints;
    }

    @Override
    public int shouldAdjustAxis(long j) {
        return this.mWrapped.shouldAdjustAxis(j);
    }
}
