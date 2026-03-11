package com.android.launcher3.util;

public class CellAndSpan {
    public int cellX;
    public int cellY;
    public int spanX;
    public int spanY;

    public CellAndSpan() {
        this.cellX = -1;
        this.cellY = -1;
        this.spanX = 1;
        this.spanY = 1;
    }

    public void copyFrom(CellAndSpan cellAndSpan) {
        this.cellX = cellAndSpan.cellX;
        this.cellY = cellAndSpan.cellY;
        this.spanX = cellAndSpan.spanX;
        this.spanY = cellAndSpan.spanY;
    }

    public CellAndSpan(int i, int i2, int i3, int i4) {
        this.cellX = -1;
        this.cellY = -1;
        this.spanX = 1;
        this.spanY = 1;
        this.cellX = i;
        this.cellY = i2;
        this.spanX = i3;
        this.spanY = i4;
    }

    public String toString() {
        return "(" + this.cellX + ", " + this.cellY + ": " + this.spanX + ", " + this.spanY + ")";
    }
}
