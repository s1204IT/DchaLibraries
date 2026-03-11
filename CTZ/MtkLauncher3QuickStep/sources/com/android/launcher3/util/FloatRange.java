package com.android.launcher3.util;

public class FloatRange {
    public float end;
    public float start;

    public FloatRange() {
    }

    public FloatRange(float f, float f2) {
        set(f, f2);
    }

    public void set(float f, float f2) {
        this.start = f;
        this.end = f2;
    }

    public boolean contains(float f) {
        return f >= this.start && f <= this.end;
    }
}
