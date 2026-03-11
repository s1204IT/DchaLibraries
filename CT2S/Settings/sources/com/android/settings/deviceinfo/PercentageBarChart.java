package com.android.settings.deviceinfo;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import com.android.settings.R;
import java.util.Collection;

public class PercentageBarChart extends View {
    private final Paint mEmptyPaint;
    private Collection<Entry> mEntries;
    private int mMinTickWidth;

    public static class Entry implements Comparable<Entry> {
        public final int order;
        public final Paint paint;
        public final float percentage;

        protected Entry(int order, float percentage, Paint paint) {
            this.order = order;
            this.percentage = percentage;
            this.paint = paint;
        }

        @Override
        public int compareTo(Entry another) {
            return this.order - another.order;
        }
    }

    public PercentageBarChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mEmptyPaint = new Paint();
        this.mMinTickWidth = 1;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PercentageBarChart);
        this.mMinTickWidth = a.getDimensionPixelSize(1, 1);
        int emptyColor = a.getColor(0, -16777216);
        a.recycle();
        this.mEmptyPaint.setColor(emptyColor);
        this.mEmptyPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float entryWidth;
        float entryWidth2;
        super.onDraw(canvas);
        int left = getPaddingLeft();
        int right = getWidth() - getPaddingRight();
        int top = getPaddingTop();
        int bottom = getHeight() - getPaddingBottom();
        int width = right - left;
        boolean isLayoutRtl = isLayoutRtl();
        if (isLayoutRtl) {
            float nextX = right;
            if (this.mEntries != null) {
                for (Entry e : this.mEntries) {
                    if (e.percentage == 0.0f) {
                        entryWidth2 = 0.0f;
                    } else {
                        entryWidth2 = Math.max(this.mMinTickWidth, width * e.percentage);
                    }
                    float lastX = nextX - entryWidth2;
                    if (lastX < left) {
                        canvas.drawRect(left, top, nextX, bottom, e.paint);
                        return;
                    } else {
                        canvas.drawRect(lastX, top, nextX, bottom, e.paint);
                        nextX = lastX;
                    }
                }
            }
            canvas.drawRect(left, top, nextX, bottom, this.mEmptyPaint);
            return;
        }
        float lastX2 = left;
        if (this.mEntries != null) {
            for (Entry e2 : this.mEntries) {
                if (e2.percentage == 0.0f) {
                    entryWidth = 0.0f;
                } else {
                    entryWidth = Math.max(this.mMinTickWidth, width * e2.percentage);
                }
                float nextX2 = lastX2 + entryWidth;
                if (nextX2 > right) {
                    canvas.drawRect(lastX2, top, right, bottom, e2.paint);
                    return;
                } else {
                    canvas.drawRect(lastX2, top, nextX2, bottom, e2.paint);
                    lastX2 = nextX2;
                }
            }
        }
        canvas.drawRect(lastX2, top, right, bottom, this.mEmptyPaint);
    }

    @Override
    public void setBackgroundColor(int color) {
        this.mEmptyPaint.setColor(color);
    }

    public static Entry createEntry(int order, float percentage, int color) {
        Paint p = new Paint();
        p.setColor(color);
        p.setStyle(Paint.Style.FILL);
        return new Entry(order, percentage, p);
    }

    public void setEntries(Collection<Entry> entries) {
        this.mEntries = entries;
    }
}
