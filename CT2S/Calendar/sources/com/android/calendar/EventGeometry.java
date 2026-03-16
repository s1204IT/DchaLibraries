package com.android.calendar;

import android.graphics.Rect;

public class EventGeometry {
    private int mCellMargin = 0;
    private float mHourGap;
    private float mMinEventHeight;
    private float mMinuteHeight;

    void setCellMargin(int cellMargin) {
        this.mCellMargin = cellMargin;
    }

    public void setHourGap(float gap) {
        this.mHourGap = gap;
    }

    public void setMinEventHeight(float height) {
        this.mMinEventHeight = height;
    }

    public void setHourHeight(float height) {
        this.mMinuteHeight = height / 60.0f;
    }

    public boolean computeEventRect(int date, int left, int top, int cellWidth, Event event) {
        if (event.drawAsAllday()) {
            return false;
        }
        float cellMinuteHeight = this.mMinuteHeight;
        int startDay = event.startDay;
        int endDay = event.endDay;
        if (startDay > date || endDay < date) {
            return false;
        }
        int startTime = event.startTime;
        int endTime = event.endTime;
        if (startDay < date) {
            startTime = 0;
        }
        if (endDay > date) {
            endTime = 1440;
        }
        int col = event.getColumn();
        int maxCols = event.getMaxColumns();
        int startHour = startTime / 60;
        int endHour = endTime / 60;
        if (endHour * 60 == endTime) {
            endHour--;
        }
        event.top = top;
        event.top += (int) (startTime * cellMinuteHeight);
        event.top += startHour * this.mHourGap;
        event.bottom = top;
        event.bottom += (int) (endTime * cellMinuteHeight);
        event.bottom += (endHour * this.mHourGap) - 1.0f;
        if (event.bottom < event.top + this.mMinEventHeight) {
            event.bottom = event.top + this.mMinEventHeight;
        }
        float colWidth = (cellWidth - ((maxCols + 1) * this.mCellMargin)) / maxCols;
        event.left = left + (col * (this.mCellMargin + colWidth));
        event.right = event.left + colWidth;
        return true;
    }

    boolean eventIntersectsSelection(Event event, Rect selection) {
        return event.left < ((float) selection.right) && event.right >= ((float) selection.left) && event.top < ((float) selection.bottom) && event.bottom >= ((float) selection.top);
    }

    float pointToEvent(float x, float y, Event event) {
        float left = event.left;
        float right = event.right;
        float top = event.top;
        float bottom = event.bottom;
        if (x < left) {
            float dx = left - x;
            if (y < top) {
                float dy = top - y;
                return (float) Math.sqrt((dx * dx) + (dy * dy));
            }
            if (y > bottom) {
                float dy2 = y - bottom;
                return (float) Math.sqrt((dx * dx) + (dy2 * dy2));
            }
            return dx;
        }
        if (x <= right) {
            if (y < top) {
                return top - y;
            }
            if (y <= bottom) {
                return 0.0f;
            }
            return y - bottom;
        }
        float dx2 = x - right;
        if (y < top) {
            float dy3 = top - y;
            return (float) Math.sqrt((dx2 * dx2) + (dy3 * dy3));
        }
        if (y > bottom) {
            float dy4 = y - bottom;
            return (float) Math.sqrt((dx2 * dx2) + (dy4 * dy4));
        }
        return dx2;
    }
}
