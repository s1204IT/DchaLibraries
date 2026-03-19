package android.graphics.drawable.shapes;

import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;

public abstract class Shape implements Cloneable {
    private float mHeight;
    private float mWidth;

    public abstract void draw(Canvas canvas, Paint paint);

    public final float getWidth() {
        return this.mWidth;
    }

    public final float getHeight() {
        return this.mHeight;
    }

    public final void resize(float f, float f2) {
        if (f < 0.0f) {
            f = 0.0f;
        }
        if (f2 < 0.0f) {
            f2 = 0.0f;
        }
        if (this.mWidth != f || this.mHeight != f2) {
            this.mWidth = f;
            this.mHeight = f2;
            onResize(f, f2);
        }
    }

    public boolean hasAlpha() {
        return true;
    }

    protected void onResize(float f, float f2) {
    }

    public void getOutline(Outline outline) {
    }

    @Override
    public Shape mo23clone() throws CloneNotSupportedException {
        return (Shape) super.clone();
    }
}
