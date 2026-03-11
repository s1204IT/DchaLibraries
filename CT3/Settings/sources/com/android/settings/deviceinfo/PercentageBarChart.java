package com.android.settings.deviceinfo;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import java.util.Collection;

public class PercentageBarChart extends View {
    private final Paint mEmptyPaint;
    private Collection<Entry> mEntries;
    private int mMinTickWidth;

    public static class Entry implements Comparable<Entry> {
        public final int order;
        public final Paint paint;
        public final float percentage;

        @Override
        public int compareTo(Entry another) {
            return this.order - another.order;
        }
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
}
