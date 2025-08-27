package com.android.systemui.classifier;

/* loaded from: classes.dex */
public class Point {
    public long timeOffsetNano;
    public float x;
    public float y;

    public Point(float f, float f2) {
        this.x = f;
        this.y = f2;
        this.timeOffsetNano = 0L;
    }

    public Point(float f, float f2, long j) {
        this.x = f;
        this.y = f2;
        this.timeOffsetNano = j;
    }

    public boolean equals(Point point) {
        return this.x == point.x && this.y == point.y;
    }

    public float dist(Point point) {
        return (float) Math.hypot(point.x - this.x, point.y - this.y);
    }

    public float crossProduct(Point point, Point point2) {
        return ((point.x - this.x) * (point2.y - this.y)) - ((point.y - this.y) * (point2.x - this.x));
    }

    public float dotProduct(Point point, Point point2) {
        return ((point.x - this.x) * (point2.x - this.x)) + ((point.y - this.y) * (point2.y - this.y));
    }

    public float getAngle(Point point, Point point2) {
        float fDist = dist(point);
        float fDist2 = dist(point2);
        if (fDist == 0.0f || fDist2 == 0.0f) {
            return 0.0f;
        }
        float fCrossProduct = crossProduct(point, point2);
        float fAcos = (float) Math.acos(Math.min(1.0f, Math.max(-1.0f, (dotProduct(point, point2) / fDist) / fDist2)));
        if (fCrossProduct < 0.0d) {
            return 6.2831855f - fAcos;
        }
        return fAcos;
    }
}
