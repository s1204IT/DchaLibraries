package com.android.gallery3d.filtershow.imageshow;

public class ControlPoint implements Comparable {
    public float x;
    public float y;

    public ControlPoint(float px, float py) {
        this.x = px;
        this.y = py;
    }

    public ControlPoint(ControlPoint point) {
        this.x = point.x;
        this.y = point.y;
    }

    public boolean sameValues(ControlPoint other) {
        if (this == other) {
            return true;
        }
        return other != null && Float.floatToIntBits(this.x) == Float.floatToIntBits(other.x) && Float.floatToIntBits(this.y) == Float.floatToIntBits(other.y);
    }

    @Override
    public int compareTo(Object another) {
        ControlPoint p = (ControlPoint) another;
        if (p.x < this.x) {
            return 1;
        }
        if (p.x > this.x) {
            return -1;
        }
        return 0;
    }
}
