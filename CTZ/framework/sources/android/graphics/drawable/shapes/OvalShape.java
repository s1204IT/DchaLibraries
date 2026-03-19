package android.graphics.drawable.shapes;

import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RectF;

public class OvalShape extends RectShape {
    @Override
    public void draw(Canvas canvas, Paint paint) {
        canvas.drawOval(rect(), paint);
    }

    @Override
    public void getOutline(Outline outline) {
        RectF rectFRect = rect();
        outline.setOval((int) Math.ceil(rectFRect.left), (int) Math.ceil(rectFRect.top), (int) Math.floor(rectFRect.right), (int) Math.floor(rectFRect.bottom));
    }

    @Override
    public OvalShape mo23clone() throws CloneNotSupportedException {
        return (OvalShape) super.mo23clone();
    }
}
