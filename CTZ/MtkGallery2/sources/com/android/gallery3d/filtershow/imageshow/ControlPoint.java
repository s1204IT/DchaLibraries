package com.android.gallery3d.filtershow.imageshow;

public class ControlPoint implements Comparable {
    public float x;
    public float y;

    public ControlPoint(float f, float f2) {
        this.x = f;
        this.y = f2;
    }

    public ControlPoint(ControlPoint controlPoint) {
        this.x = controlPoint.x;
        this.y = controlPoint.y;
    }

    public boolean sameValues(ControlPoint controlPoint) {
        if (this == controlPoint) {
            return true;
        }
        if (controlPoint != null && Float.floatToIntBits(this.x) == Float.floatToIntBits(controlPoint.x) && Float.floatToIntBits(this.y) == Float.floatToIntBits(controlPoint.y)) {
            return true;
        }
        return false;
    }

    @Override
    public int compareTo(Object obj) {
        ControlPoint controlPoint = (ControlPoint) obj;
        if (controlPoint.x < this.x) {
            return 1;
        }
        if (controlPoint.x > this.x) {
            return -1;
        }
        return 0;
    }
}
