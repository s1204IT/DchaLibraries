package com.android.contacts.common.interactions;

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

    public void setPoint(int x, int y) {
        this.mPoint.set(x, y);
    }

    public boolean hasValidPoint() {
        return (this.mPoint.x == 0 && this.mPoint.y == 0) ? false : true;
    }
}
