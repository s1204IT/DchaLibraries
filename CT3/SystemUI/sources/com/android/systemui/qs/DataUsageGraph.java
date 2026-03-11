package com.android.systemui.qs;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import com.android.systemui.R;

public class DataUsageGraph extends View {
    private long mLimitLevel;
    private final int mMarkerWidth;
    private long mMaxLevel;
    private final int mOverlimitColor;
    private final Paint mTmpPaint;
    private final RectF mTmpRect;
    private final int mTrackColor;
    private final int mUsageColor;
    private long mUsageLevel;
    private final int mWarningColor;
    private long mWarningLevel;

    public DataUsageGraph(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mTmpRect = new RectF();
        this.mTmpPaint = new Paint();
        Resources res = context.getResources();
        this.mTrackColor = context.getColor(R.color.data_usage_graph_track);
        this.mUsageColor = context.getColor(R.color.system_accent_color);
        this.mOverlimitColor = context.getColor(R.color.system_warning_color);
        this.mWarningColor = context.getColor(R.color.data_usage_graph_warning);
        this.mMarkerWidth = res.getDimensionPixelSize(R.dimen.data_usage_graph_marker_width);
    }

    public void setLevels(long limitLevel, long warningLevel, long usageLevel) {
        this.mLimitLevel = Math.max(0L, limitLevel);
        this.mWarningLevel = Math.max(0L, warningLevel);
        this.mUsageLevel = Math.max(0L, usageLevel);
        this.mMaxLevel = Math.max(Math.max(Math.max(this.mLimitLevel, this.mWarningLevel), this.mUsageLevel), 1L);
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        RectF r = this.mTmpRect;
        Paint p = this.mTmpPaint;
        int w = getWidth();
        int h = getHeight();
        boolean overLimit = this.mLimitLevel > 0 && this.mUsageLevel > this.mLimitLevel;
        float usageRight = w * (this.mUsageLevel / this.mMaxLevel);
        if (overLimit) {
            float usageRight2 = (w * (this.mLimitLevel / this.mMaxLevel)) - (this.mMarkerWidth / 2);
            usageRight = Math.min(Math.max(usageRight2, this.mMarkerWidth), w - (this.mMarkerWidth * 2));
            r.set(this.mMarkerWidth + usageRight, 0.0f, w, h);
            p.setColor(this.mOverlimitColor);
            canvas.drawRect(r, p);
        } else {
            r.set(0.0f, 0.0f, w, h);
            p.setColor(this.mTrackColor);
            canvas.drawRect(r, p);
        }
        r.set(0.0f, 0.0f, usageRight, h);
        p.setColor(this.mUsageColor);
        canvas.drawRect(r, p);
        float warningLeft = Math.min(Math.max((w * (this.mWarningLevel / this.mMaxLevel)) - (this.mMarkerWidth / 2), 0.0f), w - this.mMarkerWidth);
        r.set(warningLeft, 0.0f, this.mMarkerWidth + warningLeft, h);
        p.setColor(this.mWarningColor);
        canvas.drawRect(r, p);
    }
}
