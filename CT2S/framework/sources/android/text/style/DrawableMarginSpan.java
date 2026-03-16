package android.text.style;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.Spanned;

public class DrawableMarginSpan implements LeadingMarginSpan, LineHeightSpan {
    private Drawable mDrawable;
    private int mPad;

    public DrawableMarginSpan(Drawable b) {
        this.mDrawable = b;
    }

    public DrawableMarginSpan(Drawable b, int pad) {
        this.mDrawable = b;
        this.mPad = pad;
    }

    @Override
    public int getLeadingMargin(boolean first) {
        return this.mDrawable.getIntrinsicWidth() + this.mPad;
    }

    @Override
    public void drawLeadingMargin(Canvas c, Paint p, int x, int dir, int top, int baseline, int bottom, CharSequence text, int start, int end, boolean first, Layout layout) {
        int st = ((Spanned) text).getSpanStart(this);
        int itop = layout.getLineTop(layout.getLineForOffset(st));
        int dw = this.mDrawable.getIntrinsicWidth();
        int dh = this.mDrawable.getIntrinsicHeight();
        this.mDrawable.setBounds(x, itop, x + dw, itop + dh);
        this.mDrawable.draw(c);
    }

    @Override
    public void chooseHeight(CharSequence text, int start, int end, int istartv, int v, Paint.FontMetricsInt fm) {
        if (end == ((Spanned) text).getSpanEnd(this)) {
            int ht = this.mDrawable.getIntrinsicHeight();
            int need = ht - (((fm.descent + v) - fm.ascent) - istartv);
            if (need > 0) {
                fm.descent += need;
            }
            int need2 = ht - (((fm.bottom + v) - fm.top) - istartv);
            if (need2 > 0) {
                fm.bottom += need2;
            }
        }
    }
}
