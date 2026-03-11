package com.android.systemui.classifier;

public class Point {
    public long timeOffsetNano;
    public float x;
    public float y;

    public Point(float x, float y) {
        this.x = x;
        this.y = y;
        this.timeOffsetNano = 0L;
    }

    public Point(float x, float y, long timeOffsetNano) {
        this.x = x;
        this.y = y;
        this.timeOffsetNano = timeOffsetNano;
    }

    public boolean equals(Point p) {
        return this.x == p.x && this.y == p.y;
    }

    public float dist(Point a) {
        return (float) Math.hypot(a.x - this.x, a.y - this.y);
    }

    public float crossProduct(Point a, Point b) {
        return ((a.x - this.x) * (b.y - this.y)) - ((a.y - this.y) * (b.x - this.x));
    }

    public float dotProduct(Point a, Point b) {
        return ((a.x - this.x) * (b.x - this.x)) + ((a.y - this.y) * (b.y - this.y));
    }

    public float getAngle(Point a, Point b) {
        float dist1 = dist(a);
        float dist2 = dist(b);
        if (dist1 == 0.0f || dist2 == 0.0f) {
            return 0.0f;
        }
        float crossProduct = crossProduct(a, b);
        float dotProduct = dotProduct(a, b);
        float cos = Math.min(1.0f, Math.max(-1.0f, (dotProduct / dist1) / dist2));
        float angle = (float) Math.acos(cos);
        if (crossProduct < 0.0d) {
            return 6.2831855f - angle;
        }
        return angle;
    }
}
