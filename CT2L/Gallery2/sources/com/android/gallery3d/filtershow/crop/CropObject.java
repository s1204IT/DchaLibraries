package com.android.gallery3d.filtershow.crop;

import android.graphics.RectF;
import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils;

public class CropObject {
    private BoundedRect mBoundedRect;
    private float mAspectWidth = 1.0f;
    private float mAspectHeight = 1.0f;
    private boolean mFixAspectRatio = false;
    private float mRotation = 0.0f;
    private float mTouchTolerance = 45.0f;
    private float mMinSideSize = 20.0f;
    private int mMovingEdges = 0;

    public CropObject(RectF outerBound, RectF innerBound, int outerAngle) {
        this.mBoundedRect = new BoundedRect(outerAngle % 360, outerBound, innerBound);
    }

    public void resetBoundsTo(RectF inner, RectF outer) {
        this.mBoundedRect.resetTo(0.0f, outer, inner);
    }

    public void getInnerBounds(RectF r) {
        this.mBoundedRect.setToInner(r);
    }

    public RectF getInnerBounds() {
        return this.mBoundedRect.getInner();
    }

    public RectF getOuterBounds() {
        return this.mBoundedRect.getOuter();
    }

    public int getSelectState() {
        return this.mMovingEdges;
    }

    public boolean isFixedAspect() {
        return this.mFixAspectRatio;
    }

    public boolean setInnerAspectRatio(float width, float height) {
        if (width <= 0.0f || height <= 0.0f) {
            throw new IllegalArgumentException("Width and Height must be greater than zero");
        }
        RectF inner = this.mBoundedRect.getInner();
        CropMath.fixAspectRatioContained(inner, width, height);
        if (inner.width() < this.mMinSideSize || inner.height() < this.mMinSideSize) {
            return false;
        }
        this.mAspectWidth = width;
        this.mAspectHeight = height;
        this.mFixAspectRatio = true;
        this.mBoundedRect.setInner(inner);
        clearSelectState();
        return true;
    }

    public void setTouchTolerance(float tolerance) {
        if (tolerance <= 0.0f) {
            throw new IllegalArgumentException("Tolerance must be greater than zero");
        }
        this.mTouchTolerance = tolerance;
    }

    public void setMinInnerSideSize(float minSide) {
        if (minSide <= 0.0f) {
            throw new IllegalArgumentException("Min dide must be greater than zero");
        }
        this.mMinSideSize = minSide;
    }

    public void unsetAspectRatio() {
        this.mFixAspectRatio = false;
        clearSelectState();
    }

    public static boolean checkCorner(int selected) {
        return selected == 3 || selected == 6 || selected == 12 || selected == 9;
    }

    public static boolean checkEdge(int selected) {
        return selected == 1 || selected == 2 || selected == 4 || selected == 8;
    }

    public static boolean checkBlock(int selected) {
        return selected == 16;
    }

    public static boolean checkValid(int selected) {
        return selected == 0 || checkBlock(selected) || checkEdge(selected) || checkCorner(selected);
    }

    public void clearSelectState() {
        this.mMovingEdges = 0;
    }

    public boolean selectEdge(int edge) {
        if (!checkValid(edge)) {
            throw new IllegalArgumentException("bad edge selected");
        }
        if (this.mFixAspectRatio && !checkCorner(edge) && !checkBlock(edge) && edge != 0) {
            throw new IllegalArgumentException("bad corner selected");
        }
        this.mMovingEdges = edge;
        return true;
    }

    public boolean selectEdge(float x, float y) {
        int edgeSelected = calculateSelectedEdge(x, y);
        if (this.mFixAspectRatio) {
            edgeSelected = fixEdgeToCorner(edgeSelected);
        }
        if (edgeSelected == 0) {
            return false;
        }
        return selectEdge(edgeSelected);
    }

    public boolean moveCurrentSelection(float dX, float dY) {
        if (this.mMovingEdges == 0) {
            return false;
        }
        RectF crop = this.mBoundedRect.getInner();
        float minWidthHeight = this.mMinSideSize;
        int movingEdges = this.mMovingEdges;
        if (movingEdges == 16) {
            this.mBoundedRect.moveInner(dX, dY);
            return true;
        }
        float dx = 0.0f;
        float dy = 0.0f;
        if ((movingEdges & 1) != 0) {
            dx = Math.min(crop.left + dX, crop.right - minWidthHeight) - crop.left;
        }
        if ((movingEdges & 2) != 0) {
            dy = Math.min(crop.top + dY, crop.bottom - minWidthHeight) - crop.top;
        }
        if ((movingEdges & 4) != 0) {
            dx = Math.max(crop.right + dX, crop.left + minWidthHeight) - crop.right;
        }
        if ((movingEdges & 8) != 0) {
            dy = Math.max(crop.bottom + dY, crop.top + minWidthHeight) - crop.bottom;
        }
        if (this.mFixAspectRatio) {
            float[] l1 = {crop.left, crop.bottom};
            float[] l2 = {crop.right, crop.top};
            if (movingEdges == 3 || movingEdges == 12) {
                l1[1] = crop.top;
                l2[1] = crop.bottom;
            }
            float[] b = {l1[0] - l2[0], l1[1] - l2[1]};
            float[] disp = {dx, dy};
            float[] bUnit = GeometryMathUtils.normalize(b);
            float sp = GeometryMathUtils.scalarProjection(disp, bUnit);
            float dx2 = sp * bUnit[0];
            float dy2 = sp * bUnit[1];
            RectF newCrop = fixedCornerResize(crop, movingEdges, dx2, dy2);
            this.mBoundedRect.fixedAspectResizeInner(newCrop);
        } else {
            if ((movingEdges & 1) != 0) {
                crop.left += dx;
            }
            if ((movingEdges & 2) != 0) {
                crop.top += dy;
            }
            if ((movingEdges & 4) != 0) {
                crop.right += dx;
            }
            if ((movingEdges & 8) != 0) {
                crop.bottom += dy;
            }
            this.mBoundedRect.resizeInner(crop);
        }
        return true;
    }

    private int calculateSelectedEdge(float x, float y) {
        RectF cropped = this.mBoundedRect.getInner();
        float left = Math.abs(x - cropped.left);
        float right = Math.abs(x - cropped.right);
        float top = Math.abs(y - cropped.top);
        float bottom = Math.abs(y - cropped.bottom);
        int edgeSelected = 0;
        if (left <= this.mTouchTolerance && this.mTouchTolerance + y >= cropped.top && y - this.mTouchTolerance <= cropped.bottom && left < right) {
            edgeSelected = 0 | 1;
        } else if (right <= this.mTouchTolerance && this.mTouchTolerance + y >= cropped.top && y - this.mTouchTolerance <= cropped.bottom) {
            edgeSelected = 0 | 4;
        }
        if (top <= this.mTouchTolerance && this.mTouchTolerance + x >= cropped.left && x - this.mTouchTolerance <= cropped.right && top < bottom) {
            return edgeSelected | 2;
        }
        if (bottom <= this.mTouchTolerance && this.mTouchTolerance + x >= cropped.left && x - this.mTouchTolerance <= cropped.right) {
            return edgeSelected | 8;
        }
        return edgeSelected;
    }

    private static RectF fixedCornerResize(RectF r, int moving_corner, float dx, float dy) {
        if (moving_corner == 12) {
            RectF newCrop = new RectF(r.left, r.top, r.left + r.width() + dx, r.top + r.height() + dy);
            return newCrop;
        }
        if (moving_corner == 9) {
            RectF newCrop2 = new RectF((r.right - r.width()) + dx, r.top, r.right, r.top + r.height() + dy);
            return newCrop2;
        }
        if (moving_corner == 3) {
            RectF newCrop3 = new RectF((r.right - r.width()) + dx, (r.bottom - r.height()) + dy, r.right, r.bottom);
            return newCrop3;
        }
        if (moving_corner != 6) {
            return null;
        }
        RectF newCrop4 = new RectF(r.left, (r.bottom - r.height()) + dy, r.left + r.width() + dx, r.bottom);
        return newCrop4;
    }

    private static int fixEdgeToCorner(int moving_edges) {
        if (moving_edges == 1) {
            moving_edges |= 2;
        }
        if (moving_edges == 2) {
            moving_edges |= 1;
        }
        if (moving_edges == 4) {
            moving_edges |= 8;
        }
        if (moving_edges == 8) {
            return moving_edges | 4;
        }
        return moving_edges;
    }
}
