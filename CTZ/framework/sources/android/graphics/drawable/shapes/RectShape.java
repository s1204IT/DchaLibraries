package android.graphics.drawable.shapes;

import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RectF;

public class RectShape extends Shape {
    private RectF mRect = new RectF();

    @Override
    public void draw(Canvas canvas, Paint paint) {
        canvas.drawRect(this.mRect, paint);
    }

    @Override
    public void getOutline(Outline outline) {
        RectF rectFRect = rect();
        outline.setRect((int) Math.ceil(rectFRect.left), (int) Math.ceil(rectFRect.top), (int) Math.floor(rectFRect.right), (int) Math.floor(rectFRect.bottom));
    }

    @Override
    protected void onResize(float f, float f2) {
        this.mRect.set(0.0f, 0.0f, f, f2);
    }

    protected final RectF rect() {
        return this.mRect;
    }

    @Override
    public RectShape mo23clone() throws CloneNotSupportedException {
        RectShape rectShape = (RectShape) super.mo23clone();
        rectShape.mRect = new RectF(this.mRect);
        return rectShape;
    }
}
