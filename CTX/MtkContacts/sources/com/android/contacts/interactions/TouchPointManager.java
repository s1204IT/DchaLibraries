package com.android.contacts.interactions;

import android.graphics.Point;

public class TouchPointManager {
    private static TouchPointManager sInstance = new TouchPointManager();
    private Point mPoint = new Point();

    private TouchPointManager() {
    }

    public static TouchPointManager getInstance() {
        return sInstance;
    }

    public Point getPoint() {
        return this.mPoint;
    }

    public void setPoint(int i, int i2) {
        this.mPoint.set(i, i2);
    }

    public boolean hasValidPoint() {
        return (this.mPoint.x == 0 && this.mPoint.y == 0) ? false : true;
    }
}
