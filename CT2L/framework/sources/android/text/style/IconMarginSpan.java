package android.text.style;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.Spanned;

public class IconMarginSpan implements LeadingMarginSpan, LineHeightSpan {
    private Bitmap mBitmap;
    private int mPad;

    public IconMarginSpan(Bitmap b) {
        this.mBitmap = b;
    }

    public IconMarginSpan(Bitmap b, int pad) {
        this.mBitmap = b;
        this.mPad = pad;
    }

    @Override
    public int getLeadingMargin(boolean first) {
        return this.mBitmap.getWidth() + this.mPad;
    }

    @Override
    public void drawLeadingMargin(Canvas c, Paint p, int x, int dir, int top, int baseline, int bottom, CharSequence text, int start, int end, boolean first, Layout layout) {
        int st = ((Spanned) text).getSpanStart(this);
        int itop = layout.getLineTop(layout.getLineForOffset(st));
        if (dir < 0) {
            x -= this.mBitmap.getWidth();
        }
        c.drawBitmap(this.mBitmap, x, itop, p);
    }

    @Override
    public void chooseHeight(CharSequence text, int start, int end, int istartv, int v, Paint.FontMetricsInt fm) {
        if (end == ((Spanned) text).getSpanEnd(this)) {
            int ht = this.mBitmap.getHeight();
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
