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

    public RoundRectShape(float[] outerRadii, RectF inset, float[] innerRadii) {
        if (outerRadii != null && outerRadii.length < 8) {
            throw new ArrayIndexOutOfBoundsException("outer radii must have >= 8 values");
        }
        if (innerRadii != null && innerRadii.length < 8) {
            throw new ArrayIndexOutOfBoundsException("inner radii must have >= 8 values");
        }
        this.mOuterRadii = outerRadii;
        this.mInset = inset;
        this.mInnerRadii = innerRadii;
        if (inset != null) {
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
        if (this.mInnerRect == null) {
            float radius = 0.0f;
            if (this.mOuterRadii != null) {
                radius = this.mOuterRadii[0];
                for (int i = 1; i < 8; i++) {
                    if (this.mOuterRadii[i] != radius) {
                        outline.setConvexPath(this.mPath);
                        return;
                    }
                }
            }
            RectF rect = rect();
            outline.setRoundRect((int) Math.ceil(rect.left), (int) Math.ceil(rect.top), (int) Math.floor(rect.right), (int) Math.floor(rect.bottom), radius);
        }
    }

    @Override
    protected void onResize(float w, float h) {
        super.onResize(w, h);
        RectF r = rect();
        this.mPath.reset();
        if (this.mOuterRadii != null) {
            this.mPath.addRoundRect(r, this.mOuterRadii, Path.Direction.CW);
        } else {
            this.mPath.addRect(r, Path.Direction.CW);
        }
        if (this.mInnerRect != null) {
            this.mInnerRect.set(r.left + this.mInset.left, r.top + this.mInset.top, r.right - this.mInset.right, r.bottom - this.mInset.bottom);
            if (this.mInnerRect.width() < w && this.mInnerRect.height() < h) {
                if (this.mInnerRadii != null) {
                    this.mPath.addRoundRect(this.mInnerRect, this.mInnerRadii, Path.Direction.CCW);
                } else {
                    this.mPath.addRect(this.mInnerRect, Path.Direction.CCW);
                }
            }
        }
    }

    @Override
    public RoundRectShape mo15clone() throws CloneNotSupportedException {
        RoundRectShape shape = (RoundRectShape) super.mo15clone();
        shape.mOuterRadii = this.mOuterRadii != null ? (float[]) this.mOuterRadii.clone() : null;
        shape.mInnerRadii = this.mInnerRadii != null ? (float[]) this.mInnerRadii.clone() : null;
        shape.mInset = new RectF(this.mInset);
        shape.mInnerRect = new RectF(this.mInnerRect);
        shape.mPath = new Path(this.mPath);
        return shape;
    }
}
