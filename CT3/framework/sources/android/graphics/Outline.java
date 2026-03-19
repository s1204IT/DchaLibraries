package android.graphics;

import android.graphics.Path;

public final class Outline {
    public static final int MODE_CONVEX_PATH = 2;
    public static final int MODE_EMPTY = 0;
    public static final int MODE_ROUND_RECT = 1;
    private static final float RADIUS_UNDEFINED = Float.NEGATIVE_INFINITY;
    public float mAlpha;
    public int mMode = 0;
    public final Path mPath = new Path();
    public final Rect mRect = new Rect();
    public float mRadius = RADIUS_UNDEFINED;

    public Outline() {
    }

    public Outline(Outline src) {
        set(src);
    }

    public void setEmpty() {
        this.mMode = 0;
        this.mPath.rewind();
        this.mRect.setEmpty();
        this.mRadius = RADIUS_UNDEFINED;
    }

    public boolean isEmpty() {
        return this.mMode == 0;
    }

    public boolean canClip() {
        return this.mMode != 2;
    }

    public void setAlpha(float alpha) {
        this.mAlpha = alpha;
    }

    public float getAlpha() {
        return this.mAlpha;
    }

    public void set(Outline src) {
        this.mMode = src.mMode;
        this.mPath.set(src.mPath);
        this.mRect.set(src.mRect);
        this.mRadius = src.mRadius;
        this.mAlpha = src.mAlpha;
    }

    public void setRect(int left, int top, int right, int bottom) {
        setRoundRect(left, top, right, bottom, 0.0f);
    }

    public void setRect(Rect rect) {
        setRect(rect.left, rect.top, rect.right, rect.bottom);
    }

    public void setRoundRect(int left, int top, int right, int bottom, float radius) {
        if (left >= right || top >= bottom) {
            setEmpty();
            return;
        }
        this.mMode = 1;
        this.mRect.set(left, top, right, bottom);
        this.mRadius = radius;
        this.mPath.rewind();
    }

    public void setRoundRect(Rect rect, float radius) {
        setRoundRect(rect.left, rect.top, rect.right, rect.bottom, radius);
    }

    public boolean getRect(Rect outRect) {
        if (this.mMode != 1) {
            return false;
        }
        outRect.set(this.mRect);
        return true;
    }

    public float getRadius() {
        return this.mRadius;
    }

    public void setOval(int left, int top, int right, int bottom) {
        if (left >= right || top >= bottom) {
            setEmpty();
            return;
        }
        if (bottom - top == right - left) {
            setRoundRect(left, top, right, bottom, (bottom - top) / 2.0f);
            return;
        }
        this.mMode = 2;
        this.mPath.rewind();
        this.mPath.addOval(left, top, right, bottom, Path.Direction.CW);
        this.mRect.setEmpty();
        this.mRadius = RADIUS_UNDEFINED;
    }

    public void setOval(Rect rect) {
        setOval(rect.left, rect.top, rect.right, rect.bottom);
    }

    public void setConvexPath(Path convexPath) {
        if (convexPath.isEmpty()) {
            setEmpty();
        } else {
            if (!convexPath.isConvex()) {
                throw new IllegalArgumentException("path must be convex");
            }
            this.mMode = 2;
            this.mPath.set(convexPath);
            this.mRect.setEmpty();
            this.mRadius = RADIUS_UNDEFINED;
        }
    }

    public void offset(int dx, int dy) {
        if (this.mMode == 1) {
            this.mRect.offset(dx, dy);
        } else {
            if (this.mMode != 2) {
                return;
            }
            this.mPath.offset(dx, dy);
        }
    }
}
