package android.graphics.drawable.shapes;

import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;

public class ArcShape extends RectShape {
    private final float mStartAngle;
    private final float mSweepAngle;

    public ArcShape(float f, float f2) {
        this.mStartAngle = f;
        this.mSweepAngle = f2;
    }

    public final float getStartAngle() {
        return this.mStartAngle;
    }

    public final float getSweepAngle() {
        return this.mSweepAngle;
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        canvas.drawArc(rect(), this.mStartAngle, this.mSweepAngle, true, paint);
    }

    @Override
    public void getOutline(Outline outline) {
    }

    @Override
    public ArcShape mo23clone() throws CloneNotSupportedException {
        return (ArcShape) super.mo23clone();
    }
}
