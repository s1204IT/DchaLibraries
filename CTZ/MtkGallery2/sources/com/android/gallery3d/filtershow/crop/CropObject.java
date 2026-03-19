package com.android.gallery3d.filtershow.crop;

import android.graphics.RectF;
import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils;
import com.mediatek.gallery3d.util.Log;

public class CropObject {
    private BoundedRect mBoundedRect;
    private float mAspectWidth = 1.0f;
    private float mAspectHeight = 1.0f;
    private boolean mFixAspectRatio = false;
    private float mRotation = 0.0f;
    private float mTouchTolerance = 45.0f;
    private float mMinSideSize = 20.0f;
    private int mMovingEdges = 0;
    private RectF mOriginalInnerRect = null;

    public CropObject(RectF rectF, RectF rectF2, int i) {
        this.mBoundedRect = new BoundedRect(i % 360, rectF, rectF2);
    }

    public void resetBoundsTo(RectF rectF, RectF rectF2) {
        this.mBoundedRect.resetTo(0.0f, rectF2, rectF);
    }

    public void getInnerBounds(RectF rectF) {
        this.mBoundedRect.setToInner(rectF);
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

    public boolean setInnerAspectRatio(float f, float f2) {
        if (f <= 0.0f || f2 <= 0.0f) {
            throw new IllegalArgumentException("Width and Height must be greater than zero");
        }
        RectF inner = this.mOriginalInnerRect == null ? this.mBoundedRect.getInner() : new RectF(this.mOriginalInnerRect);
        CropMath.fixAspectRatioContained(inner, f, f2);
        Log.d("Gallery2/CropObject", " inner.width() = " + inner.width() + " inner.height() = " + inner.height() + " mMinSideSize = " + this.mMinSideSize);
        if (inner.width() < this.mMinSideSize || inner.height() < this.mMinSideSize) {
            return false;
        }
        this.mAspectWidth = f;
        this.mAspectHeight = f2;
        this.mFixAspectRatio = true;
        this.mBoundedRect.setInner(inner);
        clearSelectState();
        return true;
    }

    public void setTouchTolerance(float f) {
        if (f <= 0.0f) {
            throw new IllegalArgumentException("Tolerance must be greater than zero");
        }
        this.mTouchTolerance = f;
    }

    public void setMinInnerSideSize(float f) {
        if (f <= 0.0f) {
            throw new IllegalArgumentException("Min dide must be greater than zero");
        }
        this.mMinSideSize = f;
    }

    public void unsetAspectRatio() {
        this.mFixAspectRatio = false;
        clearSelectState();
    }

    public void setAspectRatio() {
        this.mFixAspectRatio = true;
        clearSelectState();
    }

    public static boolean checkCorner(int i) {
        return i == 3 || i == 6 || i == 12 || i == 9;
    }

    public static boolean checkEdge(int i) {
        return i == 1 || i == 2 || i == 4 || i == 8;
    }

    public static boolean checkBlock(int i) {
        return i == 16;
    }

    public static boolean checkValid(int i) {
        return i == 0 || checkBlock(i) || checkEdge(i) || checkCorner(i);
    }

    public void clearSelectState() {
        this.mMovingEdges = 0;
    }

    public boolean selectEdge(int i) {
        if (!checkValid(i)) {
            throw new IllegalArgumentException("bad edge selected");
        }
        if (this.mFixAspectRatio && !checkCorner(i) && !checkBlock(i) && i != 0) {
            throw new IllegalArgumentException("bad corner selected");
        }
        this.mMovingEdges = i;
        return true;
    }

    public boolean selectEdge(float f, float f2) {
        int iCalculateSelectedEdge = calculateSelectedEdge(f, f2);
        if (this.mFixAspectRatio) {
            iCalculateSelectedEdge = fixEdgeToCorner(iCalculateSelectedEdge);
        }
        if (iCalculateSelectedEdge == 0) {
            return false;
        }
        return selectEdge(iCalculateSelectedEdge);
    }

    public boolean moveCurrentSelection(float f, float f2) {
        float fMax;
        float fMax2;
        if (this.mMovingEdges == 0) {
            return false;
        }
        RectF inner = this.mBoundedRect.getInner();
        float f3 = this.mMinSideSize;
        int i = this.mMovingEdges;
        if (i == 16) {
            this.mBoundedRect.moveInner(f, f2);
            return true;
        }
        int i2 = i & 1;
        if (i2 != 0) {
            fMax = Math.min(inner.left + f, inner.right - f3) - inner.left;
        } else {
            fMax = 0.0f;
        }
        int i3 = i & 2;
        if (i3 != 0) {
            fMax2 = Math.min(inner.top + f2, inner.bottom - f3) - inner.top;
        } else {
            fMax2 = 0.0f;
        }
        int i4 = i & 4;
        if (i4 != 0) {
            fMax = Math.max(inner.right + f, inner.left + f3) - inner.right;
        }
        int i5 = i & 8;
        if (i5 != 0) {
            fMax2 = Math.max(inner.bottom + f2, inner.top + f3) - inner.bottom;
        }
        if (this.mFixAspectRatio) {
            float[] fArr = {inner.left, inner.bottom};
            float[] fArr2 = {inner.right, inner.top};
            if (i == 3 || i == 12) {
                fArr[1] = inner.top;
                fArr2[1] = inner.bottom;
            }
            float[] fArrNormalize = GeometryMathUtils.normalize(new float[]{fArr[0] - fArr2[0], fArr[1] - fArr2[1]});
            float fScalarProjection = GeometryMathUtils.scalarProjection(new float[]{fMax, fMax2}, fArrNormalize);
            this.mBoundedRect.fixedAspectResizeInner(fixedCornerResize(inner, i, fArrNormalize[0] * fScalarProjection, fScalarProjection * fArrNormalize[1]));
        } else {
            if (i2 != 0) {
                inner.left += fMax;
            }
            if (i3 != 0) {
                inner.top += fMax2;
            }
            if (i4 != 0) {
                inner.right += fMax;
            }
            if (i5 != 0) {
                inner.bottom += fMax2;
            }
            RectF inner2 = this.mBoundedRect.getInner();
            this.mBoundedRect.resizeInner(inner);
            RectF inner3 = this.mBoundedRect.getInner();
            if ((fMax > 0.0f && inner3.right < inner2.right) || ((fMax < 0.0f && inner3.left > inner2.left) || ((fMax2 > 0.0f && inner3.bottom < inner2.bottom) || (fMax2 < 0.0f && inner3.top > inner2.top)))) {
                this.mBoundedRect.resizeInner(inner2);
            }
        }
        return true;
    }

    private int calculateSelectedEdge(float f, float f2) {
        int i;
        RectF inner = this.mBoundedRect.getInner();
        float fAbs = Math.abs(f - inner.left);
        float fAbs2 = Math.abs(f - inner.right);
        float fAbs3 = Math.abs(f2 - inner.top);
        float fAbs4 = Math.abs(f2 - inner.bottom);
        if (fAbs <= this.mTouchTolerance && this.mTouchTolerance + f2 >= inner.top && f2 - this.mTouchTolerance <= inner.bottom && fAbs < fAbs2) {
            i = 1;
        } else if (fAbs2 <= this.mTouchTolerance && this.mTouchTolerance + f2 >= inner.top && f2 - this.mTouchTolerance <= inner.bottom) {
            i = 4;
        } else {
            i = 0;
        }
        if (fAbs3 <= this.mTouchTolerance && this.mTouchTolerance + f >= inner.left && f - this.mTouchTolerance <= inner.right && fAbs3 < fAbs4) {
            return i | 2;
        }
        if (fAbs4 <= this.mTouchTolerance && this.mTouchTolerance + f >= inner.left && f - this.mTouchTolerance <= inner.right) {
            return i | 8;
        }
        return i;
    }

    private static RectF fixedCornerResize(RectF rectF, int i, float f, float f2) {
        if (i == 12) {
            return new RectF(rectF.left, rectF.top, rectF.left + rectF.width() + f, rectF.top + rectF.height() + f2);
        }
        if (i == 9) {
            return new RectF((rectF.right - rectF.width()) + f, rectF.top, rectF.right, rectF.top + rectF.height() + f2);
        }
        if (i == 3) {
            return new RectF((rectF.right - rectF.width()) + f, (rectF.bottom - rectF.height()) + f2, rectF.right, rectF.bottom);
        }
        if (i == 6) {
            return new RectF(rectF.left, (rectF.bottom - rectF.height()) + f2, rectF.left + rectF.width() + f, rectF.bottom);
        }
        return null;
    }

    private static int fixEdgeToCorner(int i) {
        if (i == 1) {
            i |= 2;
        }
        if (i == 2) {
            i |= 1;
        }
        if (i == 4) {
            i |= 8;
        }
        if (i == 8) {
            return i | 4;
        }
        return i;
    }

    public void setOriginalInnerRect(RectF rectF) {
        this.mOriginalInnerRect = new RectF(rectF);
    }

    public void resetInnerRect() {
        if (this.mOriginalInnerRect != null) {
            this.mBoundedRect.resetInner(this.mOriginalInnerRect);
        }
    }

    public float getMinSideSize() {
        return this.mMinSideSize;
    }
}
