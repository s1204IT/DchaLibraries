package com.android.gallery3d.ui;

public class RelativePosition {
    private float mAbsoluteX;
    private float mAbsoluteY;
    private float mReferenceX;
    private float mReferenceY;

    public void setAbsolutePosition(int i, int i2) {
        this.mAbsoluteX = i;
        this.mAbsoluteY = i2;
    }

    public void setReferencePosition(int i, int i2) {
        this.mReferenceX = i;
        this.mReferenceY = i2;
    }

    public float getX() {
        return this.mAbsoluteX - this.mReferenceX;
    }

    public float getY() {
        return this.mAbsoluteY - this.mReferenceY;
    }
}
