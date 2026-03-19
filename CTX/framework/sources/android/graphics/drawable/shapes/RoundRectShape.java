package android.graphics.drawable.shapes;

import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

public class RoundRectShape extends RectShape {
    private float[] mInnerRadii;
    private RectF mInnerRect;
    private RectF mInset;
    private float[] mOuterRadii;
    private Path mPath;

    public RoundRectShape(float[] fArr, RectF rectF, float[] fArr2) {
        if (fArr != null && fArr.length < 8) {
            throw new ArrayIndexOutOfBoundsException("outer radii must have >= 8 values");
        }
        if (fArr2 != null && fArr2.length < 8) {
            throw new ArrayIndexOutOfBoundsException("inner radii must have >= 8 values");
        }
        this.mOuterRadii = fArr;
        this.mInset = rectF;
        this.mInnerRadii = fArr2;
        if (rectF != null) {
            this.mInnerRect = new RectF();
        }
        this.mPath = new Path();
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        canvas.drawPath(this.mPath, paint);
    }

    @Override
    public void getOutline(Outline outline) {
        if (this.mInnerRect != null) {
            return;
        }
        float f = 0.0f;
        if (this.mOuterRadii != null) {
            f = this.mOuterRadii[0];
            for (int i = 1; i < 8; i++) {
                if (this.mOuterRadii[i] != f) {
                    outline.setConvexPath(this.mPath);
                    return;
                }
            }
        }
        float f2 = f;
        RectF rectFRect = rect();
        outline.setRoundRect((int) Math.ceil(rectFRect.left), (int) Math.ceil(rectFRect.top), (int) Math.floor(rectFRect.right), (int) Math.floor(rectFRect.bottom), f2);
    }

    @Override
    protected void onResize(float f, float f2) {
        super.onResize(f, f2);
        RectF rectFRect = rect();
        this.mPath.reset();
        if (this.mOuterRadii != null) {
            this.mPath.addRoundRect(rectFRect, this.mOuterRadii, Path.Direction.CW);
        } else {
            this.mPath.addRect(rectFRect, Path.Direction.CW);
        }
        if (this.mInnerRect != null) {
            this.mInnerRect.set(rectFRect.left + this.mInset.left, rectFRect.top + this.mInset.top, rectFRect.right - this.mInset.right, rectFRect.bottom - this.mInset.bottom);
            if (this.mInnerRect.width() < f && this.mInnerRect.height() < f2) {
                if (this.mInnerRadii != null) {
                    this.mPath.addRoundRect(this.mInnerRect, this.mInnerRadii, Path.Direction.CCW);
                } else {
                    this.mPath.addRect(this.mInnerRect, Path.Direction.CCW);
                }
            }
        }
    }

    @Override
    public RoundRectShape mo23clone() throws CloneNotSupportedException {
        RoundRectShape roundRectShape = (RoundRectShape) super.mo23clone();
        roundRectShape.mOuterRadii = this.mOuterRadii != null ? (float[]) this.mOuterRadii.clone() : null;
        roundRectShape.mInnerRadii = this.mInnerRadii != null ? (float[]) this.mInnerRadii.clone() : null;
        roundRectShape.mInset = new RectF(this.mInset);
        roundRectShape.mInnerRect = new RectF(this.mInnerRect);
        roundRectShape.mPath = new Path(this.mPath);
        return roundRectShape;
    }
}
