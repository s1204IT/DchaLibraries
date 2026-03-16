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
        RectF rect = rect();
        outline.setRect((int) Math.ceil(rect.left), (int) Math.ceil(rect.top), (int) Math.floor(rect.right), (int) Math.floor(rect.bottom));
    }

    @Override
    protected void onResize(float width, float height) {
        this.mRect.set(0.0f, 0.0f, width, height);
    }

    protected final RectF rect() {
        return this.mRect;
    }

    @Override
    public RectShape mo15clone() throws CloneNotSupportedException {
        RectShape shape = (RectShape) super.mo15clone();
        shape.mRect = new RectF(this.mRect);
        return shape;
    }
}
