package com.android.gallery3d.filtershow.crop;

import android.graphics.Matrix;
import android.graphics.RectF;
import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils;
import java.util.Arrays;

public class BoundedRect {
    private RectF inner;
    private float[] innerRotated;
    private RectF outer;
    private float rot;

    public BoundedRect(float f, RectF rectF, RectF rectF2) {
        this.rot = f;
        this.outer = new RectF(rectF);
        this.inner = new RectF(rectF2);
        this.innerRotated = CropMath.getCornersFromRect(this.inner);
        rotateInner();
        if (!isConstrained()) {
            reconstrain();
        }
    }

    public void resetTo(float f, RectF rectF, RectF rectF2) {
        this.rot = f;
        this.outer.set(rectF);
        this.inner.set(rectF2);
        this.innerRotated = CropMath.getCornersFromRect(this.inner);
        rotateInner();
        if (!isConstrained()) {
            reconstrain();
        }
    }

    public void setInner(RectF rectF) {
        if (this.inner.equals(rectF)) {
            return;
        }
        this.inner = rectF;
        this.innerRotated = CropMath.getCornersFromRect(this.inner);
        rotateInner();
        if (!isConstrained()) {
            reconstrain();
        }
    }

    public void setToInner(RectF rectF) {
        rectF.set(this.inner);
    }

    public RectF getInner() {
        return new RectF(this.inner);
    }

    public RectF getOuter() {
        return new RectF(this.outer);
    }

    public void moveInner(float f, float f2) {
        Matrix inverseRotMatrix = getInverseRotMatrix();
        RectF rectF = new RectF(this.inner);
        rectF.offset(f, f2);
        float[] cornersFromRect = CropMath.getCornersFromRect(rectF);
        float[] cornersFromRect2 = CropMath.getCornersFromRect(this.outer);
        inverseRotMatrix.mapPoints(cornersFromRect);
        float[] fArr = {0.0f, 0.0f};
        for (int i = 0; i < cornersFromRect.length; i += 2) {
            float f3 = cornersFromRect[i] + fArr[0];
            float f4 = cornersFromRect[i + 1] + fArr[1];
            if (!CropMath.inclusiveContains(this.outer, f3, f4)) {
                float[] fArr2 = {f3, f4};
                float[] fArrShortestVectorFromPointToLine = GeometryMathUtils.shortestVectorFromPointToLine(fArr2, CropMath.closestSide(fArr2, cornersFromRect2));
                fArr[0] = fArr[0] + fArrShortestVectorFromPointToLine[0];
                fArr[1] = fArr[1] + fArrShortestVectorFromPointToLine[1];
            }
        }
        for (int i2 = 0; i2 < cornersFromRect.length; i2 += 2) {
            float f5 = cornersFromRect[i2] + fArr[0];
            float f6 = cornersFromRect[i2 + 1] + fArr[1];
            if (!CropMath.inclusiveContains(this.outer, f5, f6)) {
                float[] fArr3 = {f5, f6};
                CropMath.getEdgePoints(this.outer, fArr3);
                fArr3[0] = fArr3[0] - f5;
                fArr3[1] = fArr3[1] - f6;
                fArr[0] = fArr[0] + fArr3[0];
                fArr[1] = fArr[1] + fArr3[1];
            }
        }
        for (int i3 = 0; i3 < cornersFromRect.length; i3 += 2) {
            float f7 = cornersFromRect[i3] + fArr[0];
            int i4 = i3 + 1;
            float f8 = cornersFromRect[i4] + fArr[1];
            cornersFromRect[i3] = f7;
            cornersFromRect[i4] = f8;
        }
        this.innerRotated = cornersFromRect;
        reconstrain();
    }

    public void resizeInner(RectF rectF) {
        Matrix rotMatrix = getRotMatrix();
        Matrix inverseRotMatrix = getInverseRotMatrix();
        float[] cornersFromRect = CropMath.getCornersFromRect(this.outer);
        rotMatrix.mapPoints(cornersFromRect);
        float[] cornersFromRect2 = CropMath.getCornersFromRect(this.inner);
        float[] cornersFromRect3 = CropMath.getCornersFromRect(rectF);
        RectF rectF2 = new RectF(rectF);
        for (int i = 0; i < cornersFromRect3.length; i += 2) {
            int i2 = i + 1;
            float[] fArr = {cornersFromRect3[i], cornersFromRect3[i2]};
            float[] fArrCopyOf = Arrays.copyOf(fArr, 2);
            inverseRotMatrix.mapPoints(fArrCopyOf);
            if (!CropMath.inclusiveContains(this.outer, fArrCopyOf[0], fArrCopyOf[1])) {
                float[] fArrLineIntersect = GeometryMathUtils.lineIntersect(new float[]{cornersFromRect3[i], cornersFromRect3[i2], cornersFromRect2[i], cornersFromRect2[i2]}, CropMath.closestSide(fArr, cornersFromRect));
                if (fArrLineIntersect == null) {
                    fArrLineIntersect = new float[]{cornersFromRect2[i], cornersFromRect2[i2]};
                }
                switch (i) {
                    case 0:
                    case 1:
                        rectF2.left = fArrLineIntersect[0] > rectF2.left ? fArrLineIntersect[0] : rectF2.left;
                        rectF2.top = fArrLineIntersect[1] > rectF2.top ? fArrLineIntersect[1] : rectF2.top;
                        break;
                    case 2:
                    case 3:
                        rectF2.right = fArrLineIntersect[0] < rectF2.right ? fArrLineIntersect[0] : rectF2.right;
                        rectF2.top = fArrLineIntersect[1] > rectF2.top ? fArrLineIntersect[1] : rectF2.top;
                        break;
                    case 4:
                    case 5:
                        rectF2.right = fArrLineIntersect[0] < rectF2.right ? fArrLineIntersect[0] : rectF2.right;
                        rectF2.bottom = fArrLineIntersect[1] < rectF2.bottom ? fArrLineIntersect[1] : rectF2.bottom;
                        break;
                    case 6:
                    case 7:
                        rectF2.left = fArrLineIntersect[0] > rectF2.left ? fArrLineIntersect[0] : rectF2.left;
                        rectF2.bottom = fArrLineIntersect[1] < rectF2.bottom ? fArrLineIntersect[1] : rectF2.bottom;
                        break;
                }
            }
        }
        float[] cornersFromRect4 = CropMath.getCornersFromRect(rectF2);
        inverseRotMatrix.mapPoints(cornersFromRect4);
        this.innerRotated = cornersFromRect4;
        reconstrain();
    }

    public void fixedAspectResizeInner(RectF rectF) {
        int i;
        Matrix rotMatrix = getRotMatrix();
        Matrix inverseRotMatrix = getInverseRotMatrix();
        float fWidth = this.inner.width() / this.inner.height();
        float[] cornersFromRect = CropMath.getCornersFromRect(this.outer);
        rotMatrix.mapPoints(cornersFromRect);
        float[] cornersFromRect2 = CropMath.getCornersFromRect(this.inner);
        float[] cornersFromRect3 = CropMath.getCornersFromRect(rectF);
        int i2 = 2;
        if (this.inner.top == rectF.top) {
            i = this.inner.left == rectF.left ? 0 : this.inner.right == rectF.right ? 2 : -1;
        } else if (this.inner.bottom == rectF.bottom) {
            if (this.inner.right == rectF.right) {
                i = 4;
            } else if (this.inner.left == rectF.left) {
                i = 6;
            }
        }
        if (i == -1) {
            return;
        }
        float fWidth2 = rectF.width();
        int i3 = 0;
        while (i3 < cornersFromRect3.length) {
            float[] fArr = new float[i2];
            fArr[0] = cornersFromRect3[i3];
            int i4 = i3 + 1;
            fArr[1] = cornersFromRect3[i4];
            float[] fArrCopyOf = Arrays.copyOf(fArr, i2);
            inverseRotMatrix.mapPoints(fArrCopyOf);
            if (!CropMath.inclusiveContains(this.outer, fArrCopyOf[0], fArrCopyOf[1]) && i3 != i) {
                float[] fArrLineIntersect = GeometryMathUtils.lineIntersect(new float[]{cornersFromRect3[i3], cornersFromRect3[i4], cornersFromRect2[i3], cornersFromRect2[i4]}, CropMath.closestSide(fArr, cornersFromRect));
                if (fArrLineIntersect == null) {
                    fArrLineIntersect = new float[]{cornersFromRect2[i3], cornersFromRect2[i4]};
                }
                float fMax = Math.max(Math.abs(cornersFromRect2[i] - fArrLineIntersect[0]), Math.abs(cornersFromRect2[i + 1] - fArrLineIntersect[1]) * fWidth);
                if (fMax < fWidth2) {
                    fWidth2 = fMax;
                }
            }
            i3 += 2;
            i2 = 2;
        }
        float f = fWidth2 / fWidth;
        RectF rectF2 = new RectF(this.inner);
        if (i == 0) {
            rectF2.right = rectF2.left + fWidth2;
            rectF2.bottom = rectF2.top + f;
        } else if (i == 2) {
            rectF2.left = rectF2.right - fWidth2;
            rectF2.bottom = rectF2.top + f;
        } else if (i == 4) {
            rectF2.left = rectF2.right - fWidth2;
            rectF2.top = rectF2.bottom - f;
        } else if (i == 6) {
            rectF2.right = rectF2.left + fWidth2;
            rectF2.top = rectF2.bottom - f;
        }
        float[] cornersFromRect4 = CropMath.getCornersFromRect(rectF2);
        inverseRotMatrix.mapPoints(cornersFromRect4);
        this.innerRotated = cornersFromRect4;
        reconstrain();
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
        Matrix rotMatrix = getRotMatrix();
        float[] fArrCopyOf = Arrays.copyOf(this.innerRotated, 8);
        rotMatrix.mapPoints(fArrCopyOf);
        this.inner = CropMath.trapToRect(fArrCopyOf);
    }

    private void rotateInner() {
        getInverseRotMatrix().mapPoints(this.innerRotated);
    }

    private Matrix getRotMatrix() {
        Matrix matrix = new Matrix();
        matrix.setRotate(this.rot, this.outer.centerX(), this.outer.centerY());
        return matrix;
    }

    private Matrix getInverseRotMatrix() {
        Matrix matrix = new Matrix();
        matrix.setRotate(-this.rot, this.outer.centerX(), this.outer.centerY());
        return matrix;
    }

    public void resetInner(RectF rectF) {
        this.inner.set(rectF);
    }
}
