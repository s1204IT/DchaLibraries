package android.text.style;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import java.lang.ref.WeakReference;

public abstract class DynamicDrawableSpan extends ReplacementSpan {
    public static final int ALIGN_BASELINE = 1;
    public static final int ALIGN_BOTTOM = 0;
    private static final String TAG = "DynamicDrawableSpan";
    private WeakReference<Drawable> mDrawableRef;
    protected final int mVerticalAlignment;

    public abstract Drawable getDrawable();

    public DynamicDrawableSpan() {
        this.mVerticalAlignment = 0;
    }

    protected DynamicDrawableSpan(int verticalAlignment) {
        this.mVerticalAlignment = verticalAlignment;
    }

    public int getVerticalAlignment() {
        return this.mVerticalAlignment;
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        Drawable d = getCachedDrawable();
        Rect rect = d.getBounds();
        if (fm != null) {
            fm.ascent = -rect.bottom;
            fm.descent = 0;
            fm.top = fm.ascent;
            fm.bottom = 0;
        }
        return rect.right;
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        Drawable b = getCachedDrawable();
        canvas.save();
        int transY = bottom - b.getBounds().bottom;
        if (this.mVerticalAlignment == 1) {
            transY -= paint.getFontMetricsInt().descent;
        }
        canvas.translate(x, transY);
        b.draw(canvas);
        canvas.restore();
    }

    private Drawable getCachedDrawable() {
        WeakReference<Drawable> wr = this.mDrawableRef;
        Drawable d = null;
        if (wr != null) {
            Drawable d2 = wr.get();
            d = d2;
        }
        if (d == null) {
            Drawable d3 = getDrawable();
            this.mDrawableRef = new WeakReference<>(d3);
            return d3;
        }
        return d;
    }
}
