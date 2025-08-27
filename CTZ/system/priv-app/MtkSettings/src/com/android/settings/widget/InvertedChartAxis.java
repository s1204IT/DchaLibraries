package com.android.settings.widget;

import android.content.res.Resources;
import android.text.SpannableStringBuilder;

/* loaded from: classes.dex */
public class InvertedChartAxis implements ChartAxis {
    private float mSize;
    private final ChartAxis mWrapped;

    public InvertedChartAxis(ChartAxis chartAxis) {
        this.mWrapped = chartAxis;
    }

    @Override // com.android.settings.widget.ChartAxis
    public boolean setBounds(long j, long j2) {
        return this.mWrapped.setBounds(j, j2);
    }

    @Override // com.android.settings.widget.ChartAxis
    public boolean setSize(float f) {
        this.mSize = f;
        return this.mWrapped.setSize(f);
    }

    @Override // com.android.settings.widget.ChartAxis
    public float convertToPoint(long j) {
        return this.mSize - this.mWrapped.convertToPoint(j);
    }

    @Override // com.android.settings.widget.ChartAxis
    public long convertToValue(float f) {
        return this.mWrapped.convertToValue(this.mSize - f);
    }

    @Override // com.android.settings.widget.ChartAxis
    public long buildLabel(Resources resources, SpannableStringBuilder spannableStringBuilder, long j) {
        return this.mWrapped.buildLabel(resources, spannableStringBuilder, j);
    }

    @Override // com.android.settings.widget.ChartAxis
    public float[] getTickPoints() {
        float[] tickPoints = this.mWrapped.getTickPoints();
        for (int i = 0; i < tickPoints.length; i++) {
            tickPoints[i] = this.mSize - tickPoints[i];
        }
        return tickPoints;
    }

    @Override // com.android.settings.widget.ChartAxis
    public int shouldAdjustAxis(long j) {
        return this.mWrapped.shouldAdjustAxis(j);
    }
}
