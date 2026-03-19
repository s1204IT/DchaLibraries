package com.android.calendar;

import android.graphics.Rect;

public class EventGeometry {
    private int mCellMargin = 0;
    private float mHourGap;
    private float mMinEventHeight;
    private float mMinuteHeight;

    void setCellMargin(int i) {
        this.mCellMargin = i;
    }

    public void setHourGap(float f) {
        this.mHourGap = f;
    }

    public void setMinEventHeight(float f) {
        this.mMinEventHeight = f;
    }

    public void setHourHeight(float f) {
        this.mMinuteHeight = f / 60.0f;
    }

    public boolean computeEventRect(int i, int i2, int i3, int i4, Event event) {
        if (event.drawAsAllday()) {
            return false;
        }
        float f = this.mMinuteHeight;
        int i5 = event.startDay;
        int i6 = event.endDay;
        if (i5 > i || i6 < i) {
            return false;
        }
        int i7 = event.startTime;
        int i8 = event.endTime;
        int i9 = i5 >= i ? i7 : 0;
        if (i6 > i) {
            i8 = 1440;
        }
        int column = event.getColumn();
        int maxColumns = event.getMaxColumns();
        int i10 = i9 / 60;
        int i11 = i8 / 60;
        if (i11 * 60 == i8) {
            i11--;
        }
        float f2 = i3;
        event.top = f2;
        event.top += (int) (i9 * f);
        event.top += i10 * this.mHourGap;
        event.bottom = f2;
        event.bottom += (int) (i8 * f);
        event.bottom += (i11 * this.mHourGap) - 1.0f;
        if (event.bottom < event.top + this.mMinEventHeight) {
            event.bottom = event.top + this.mMinEventHeight;
        }
        float f3 = (i4 - ((maxColumns + 1) * this.mCellMargin)) / maxColumns;
        event.left = i2 + (column * (this.mCellMargin + f3));
        event.right = event.left + f3;
        return true;
    }

    boolean eventIntersectsSelection(Event event, Rect rect) {
        if (event.left < rect.right && event.right >= rect.left && event.top < rect.bottom && event.bottom >= rect.top) {
            return true;
        }
        return false;
    }

    float pointToEvent(float f, float f2, Event event) {
        float f3 = event.left;
        float f4 = event.right;
        float f5 = event.top;
        float f6 = event.bottom;
        if (f < f3) {
            float f7 = f3 - f;
            if (f2 < f5) {
                float f8 = f5 - f2;
                return (float) Math.sqrt((f7 * f7) + (f8 * f8));
            }
            if (f2 > f6) {
                float f9 = f2 - f6;
                return (float) Math.sqrt((f7 * f7) + (f9 * f9));
            }
            return f7;
        }
        if (f <= f4) {
            if (f2 < f5) {
                return f5 - f2;
            }
            if (f2 <= f6) {
                return 0.0f;
            }
            return f2 - f6;
        }
        float f10 = f - f4;
        if (f2 < f5) {
            float f11 = f5 - f2;
            return (float) Math.sqrt((f10 * f10) + (f11 * f11));
        }
        if (f2 > f6) {
            float f12 = f2 - f6;
            return (float) Math.sqrt((f10 * f10) + (f12 * f12));
        }
        return f10;
    }
}
