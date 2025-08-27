package com.android.systemui.recents.views;

/* compiled from: TaskStackLayoutAlgorithm.java */
/* renamed from: com.android.systemui.recents.views.Range, reason: use source file name */
/* loaded from: classes.dex */
class TaskStackLayoutAlgorithm2 {
    float max;
    float min;
    float origin;
    final float relativeMax;
    final float relativeMin;

    public TaskStackLayoutAlgorithm2(float f, float f2) {
        this.relativeMin = f;
        this.min = f;
        this.relativeMax = f2;
        this.max = f2;
    }

    public void offset(float f) {
        this.origin = f;
        this.min = this.relativeMin + f;
        this.max = f + this.relativeMax;
    }

    public float getNormalizedX(float f) {
        if (f < this.origin) {
            return 0.5f + (((f - this.origin) * 0.5f) / (-this.relativeMin));
        }
        return 0.5f + (((f - this.origin) * 0.5f) / this.relativeMax);
    }

    public float getAbsoluteX(float f) {
        return f < 0.5f ? ((f - 0.5f) / 0.5f) * (-this.relativeMin) : ((f - 0.5f) / 0.5f) * this.relativeMax;
    }

    public boolean isInRange(float f) {
        double d = f;
        return d >= Math.floor((double) this.min) && d <= Math.ceil((double) this.max);
    }
}
