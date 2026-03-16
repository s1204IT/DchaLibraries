package com.android.gallery3d.filtershow.crop;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.support.v4.app.FragmentManagerImpl;
import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils;
import java.util.Arrays;

public class BoundedRect {
    private RectF inner;
    private float[] innerRotated;
    private RectF outer;
    private float rot;

    public BoundedRect(float rotation, RectF outerRect, RectF innerRect) {
        this.rot = rotation;
        this.outer = new RectF(outerRect);
        this.inner = new RectF(innerRect);
        this.innerRotated = CropMath.getCornersFromRect(this.inner);
        rotateInner();
        if (!isConstrained()) {
            reconstrain();
        }
    }

    public void resetTo(float rotation, RectF outerRect, RectF innerRect) {
        this.rot = rotation;
        this.outer.set(outerRect);
        this.inner.set(innerRect);
        this.innerRotated = CropMath.getCornersFromRect(this.inner);
        rotateInner();
        if (!isConstrained()) {
            reconstrain();
        }
    }

    public void setInner(RectF newInner) {
        if (!this.inner.equals(newInner)) {
            this.inner = newInner;
            this.innerRotated = CropMath.getCornersFromRect(this.inner);
            rotateInner();
            if (!isConstrained()) {
                reconstrain();
            }
        }
    }

    public void setToInner(RectF r) {
        r.set(this.inner);
    }

    public RectF getInner() {
        return new RectF(this.inner);
    }

    public RectF getOuter() {
        return new RectF(this.outer);
    }

    public void moveInner(float dx, float dy) {
        Matrix m0 = getInverseRotMatrix();
        RectF translatedInner = new RectF(this.inner);
        translatedInner.offset(dx, dy);
        float[] translatedInnerCorners = CropMath.getCornersFromRect(translatedInner);
        float[] outerCorners = CropMath.getCornersFromRect(this.outer);
        m0.mapPoints(translatedInnerCorners);
        float[] correction = {0.0f, 0.0f};
        for (int i = 0; i < translatedInnerCorners.length; i += 2) {
            float correctedInnerX = translatedInnerCorners[i] + correction[0];
            float correctedInnerY = translatedInnerCorners[i + 1] + correction[1];
            if (!CropMath.inclusiveContains(this.outer, correctedInnerX, correctedInnerY)) {
                float[] badCorner = {correctedInnerX, correctedInnerY};
                float[] nearestSide = CropMath.closestSide(badCorner, outerCorners);
                float[] correctionVec = GeometryMathUtils.shortestVectorFromPointToLine(badCorner, nearestSide);
                correction[0] = correction[0] + correctionVec[0];
                correction[1] = correction[1] + correctionVec[1];
            }
        }
        for (int i2 = 0; i2 < translatedInnerCorners.length; i2 += 2) {
            float correctedInnerX2 = translatedInnerCorners[i2] + correction[0];
            float correctedInnerY2 = translatedInnerCorners[i2 + 1] + correction[1];
            if (!CropMath.inclusiveContains(this.outer, correctedInnerX2, correctedInnerY2)) {
                float[] correctionVec2 = {correctedInnerX2, correctedInnerY2};
                CropMath.getEdgePoints(this.outer, correctionVec2);
                correctionVec2[0] = correctionVec2[0] - correctedInnerX2;
                correctionVec2[1] = correctionVec2[1] - correctedInnerY2;
                correction[0] = correction[0] + correctionVec2[0];
                correction[1] = correction[1] + correctionVec2[1];
            }
        }
        for (int i3 = 0; i3 < translatedInnerCorners.length; i3 += 2) {
            float correctedInnerX3 = translatedInnerCorners[i3] + correction[0];
            float correctedInnerY3 = translatedInnerCorners[i3 + 1] + correction[1];
            translatedInnerCorners[i3] = correctedInnerX3;
            translatedInnerCorners[i3 + 1] = correctedInnerY3;
        }
        this.innerRotated = translatedInnerCorners;
        reconstrain();
    }

    public void resizeInner(RectF newInner) {
        Matrix m = getRotMatrix();
        Matrix m0 = getInverseRotMatrix();
        float[] outerCorners = CropMath.getCornersFromRect(this.outer);
        m.mapPoints(outerCorners);
        float[] oldInnerCorners = CropMath.getCornersFromRect(this.inner);
        float[] newInnerCorners = CropMath.getCornersFromRect(newInner);
        RectF ret = new RectF(newInner);
        for (int i = 0; i < newInnerCorners.length; i += 2) {
            float[] c = {newInnerCorners[i], newInnerCorners[i + 1]};
            float[] c0 = Arrays.copyOf(c, 2);
            m0.mapPoints(c0);
            if (!CropMath.inclusiveContains(this.outer, c0[0], c0[1])) {
                float[] outerSide = CropMath.closestSide(c, outerCorners);
                float[] pathOfCorner = {newInnerCorners[i], newInnerCorners[i + 1], oldInnerCorners[i], oldInnerCorners[i + 1]};
                float[] p = GeometryMathUtils.lineIntersect(pathOfCorner, outerSide);
                if (p == null) {
                    p = new float[]{oldInnerCorners[i], oldInnerCorners[i + 1]};
                }
                switch (i) {
                    case 0:
                    case 1:
                        ret.left = p[0] > ret.left ? p[0] : ret.left;
                        ret.top = p[1] > ret.top ? p[1] : ret.top;
                        break;
                    case 2:
                    case 3:
                        ret.right = p[0] < ret.right ? p[0] : ret.right;
                        ret.top = p[1] > ret.top ? p[1] : ret.top;
                        break;
                    case 4:
                    case 5:
                        ret.right = p[0] < ret.right ? p[0] : ret.right;
                        ret.bottom = p[1] < ret.bottom ? p[1] : ret.bottom;
                        break;
                    case FragmentManagerImpl.ANIM_STYLE_FADE_EXIT:
                    case 7:
                        ret.left = p[0] > ret.left ? p[0] : ret.left;
                        ret.bottom = p[1] < ret.bottom ? p[1] : ret.bottom;
                        break;
                }
            }
        }
        float[] retCorners = CropMath.getCornersFromRect(ret);
        m0.mapPoints(retCorners);
        this.innerRotated = retCorners;
        reconstrain();
    }

    public void fixedAspectResizeInner(RectF newInner) {
        Matrix m = getRotMatrix();
        Matrix m0 = getInverseRotMatrix();
        float aspectW = this.inner.width();
        float aspectH = this.inner.height();
        float aspRatio = aspectW / aspectH;
        float[] corners = CropMath.getCornersFromRect(this.outer);
        m.mapPoints(corners);
        float[] oldInnerCorners = CropMath.getCornersFromRect(this.inner);
        float[] newInnerCorners = CropMath.getCornersFromRect(newInner);
        int fixed = -1;
        if (this.inner.top == newInner.top) {
            if (this.inner.left == newInner.left) {
                fixed = 0;
            } else if (this.inner.right == newInner.right) {
                fixed = 2;
            }
        } else if (this.inner.bottom == newInner.bottom) {
            if (this.inner.right == newInner.right) {
                fixed = 4;
            } else if (this.inner.left == newInner.left) {
                fixed = 6;
            }
        }
        if (fixed != -1) {
            float widthSoFar = newInner.width();
            for (int i = 0; i < newInnerCorners.length; i += 2) {
                float[] c = {newInnerCorners[i], newInnerCorners[i + 1]};
                float[] c0 = Arrays.copyOf(c, 2);
                m0.mapPoints(c0);
                if (!CropMath.inclusiveContains(this.outer, c0[0], c0[1])) {
                    int moved = i;
                    if (moved != fixed) {
                        float[] l2 = CropMath.closestSide(c, corners);
                        float[] l1 = {newInnerCorners[i], newInnerCorners[i + 1], oldInnerCorners[i], oldInnerCorners[i + 1]};
                        float[] p = GeometryMathUtils.lineIntersect(l1, l2);
                        if (p == null) {
                            p = new float[]{oldInnerCorners[i], oldInnerCorners[i + 1]};
                        }
                        float fixed_x = oldInnerCorners[fixed];
                        float fixed_y = oldInnerCorners[fixed + 1];
                        float newWidth = Math.abs(fixed_x - p[0]);
                        float newHeight = Math.abs(fixed_y - p[1]);
                        float newWidth2 = Math.max(newWidth, aspRatio * newHeight);
                        if (newWidth2 < widthSoFar) {
                            widthSoFar = newWidth2;
                        }
                    }
                }
            }
            float heightSoFar = widthSoFar / aspRatio;
            RectF ret = new RectF(this.inner);
            if (fixed == 0) {
                ret.right = ret.left + widthSoFar;
                ret.bottom = ret.top + heightSoFar;
            } else if (fixed == 2) {
                ret.left = ret.right - widthSoFar;
                ret.bottom = ret.top + heightSoFar;
            } else if (fixed == 4) {
                ret.left = ret.right - widthSoFar;
                ret.top = ret.bottom - heightSoFar;
            } else if (fixed == 6) {
                ret.right = ret.left + widthSoFar;
                ret.top = ret.bottom - heightSoFar;
            }
            float[] retCorners = CropMath.getCornersFromRect(ret);
            m0.mapPoints(retCorners);
            this.innerRotated = retCorners;
            reconstrain();
        }
    }

    private boolean isConstrained() {
        for (int i = 0; i < 8; i += 2) {
            if (!CropMath.inclusiveContains(this.outer, this.innerRotated[i], this.innerRotated[i + 1])) {
                return false;
            }
        }
        return true;
    }

    private void reconstrain() {
        CropMath.getEdgePoints(this.outer, this.innerRotated);
        Matrix m = getRotMatrix();
        float[] unrotated = Arrays.copyOf(this.innerRotated, 8);
        m.mapPoints(unrotated);
        this.inner = CropMath.trapToRect(unrotated);
    }

    private void rotateInner() {
        Matrix m = getInverseRotMatrix();
        m.mapPoints(this.innerRotated);
    }

    private Matrix getRotMatrix() {
        Matrix m = new Matrix();
        m.setRotate(this.rot, this.outer.centerX(), this.outer.centerY());
        return m;
    }

    private Matrix getInverseRotMatrix() {
        Matrix m = new Matrix();
        m.setRotate(-this.rot, this.outer.centerX(), this.outer.centerY());
        return m;
    }
}
