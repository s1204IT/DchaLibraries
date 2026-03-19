package android.text;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.text.Layout;
import android.text.TextUtils;
import android.text.style.ParagraphStyle;

public class BoringLayout extends Layout implements TextUtils.EllipsizeCallback {
    int mBottom;
    private int mBottomPadding;
    int mDesc;
    private String mDirect;
    private int mEllipsizedCount;
    private int mEllipsizedStart;
    private int mEllipsizedWidth;
    private float mMax;
    private Paint mPaint;
    private int mTopPadding;

    public static BoringLayout make(CharSequence charSequence, TextPaint textPaint, int i, Layout.Alignment alignment, float f, float f2, Metrics metrics, boolean z) {
        return new BoringLayout(charSequence, textPaint, i, alignment, f, f2, metrics, z);
    }

    public static BoringLayout make(CharSequence charSequence, TextPaint textPaint, int i, Layout.Alignment alignment, float f, float f2, Metrics metrics, boolean z, TextUtils.TruncateAt truncateAt, int i2) {
        return new BoringLayout(charSequence, textPaint, i, alignment, f, f2, metrics, z, truncateAt, i2);
    }

    public BoringLayout replaceOrMake(CharSequence charSequence, TextPaint textPaint, int i, Layout.Alignment alignment, float f, float f2, Metrics metrics, boolean z) {
        replaceWith(charSequence, textPaint, i, alignment, f, f2);
        this.mEllipsizedWidth = i;
        this.mEllipsizedStart = 0;
        this.mEllipsizedCount = 0;
        init(charSequence, textPaint, alignment, metrics, z, true);
        return this;
    }

    public BoringLayout replaceOrMake(CharSequence charSequence, TextPaint textPaint, int i, Layout.Alignment alignment, float f, float f2, Metrics metrics, boolean z, TextUtils.TruncateAt truncateAt, int i2) {
        boolean z2;
        if (truncateAt == null || truncateAt == TextUtils.TruncateAt.MARQUEE) {
            replaceWith(charSequence, textPaint, i, alignment, f, f2);
            this.mEllipsizedWidth = i;
            this.mEllipsizedStart = 0;
            this.mEllipsizedCount = 0;
            z2 = true;
        } else {
            replaceWith(TextUtils.ellipsize(charSequence, textPaint, i2, truncateAt, true, this), textPaint, i, alignment, f, f2);
            this.mEllipsizedWidth = i2;
            z2 = false;
        }
        init(getText(), textPaint, alignment, metrics, z, z2);
        return this;
    }

    public BoringLayout(CharSequence charSequence, TextPaint textPaint, int i, Layout.Alignment alignment, float f, float f2, Metrics metrics, boolean z) {
        super(charSequence, textPaint, i, alignment, f, f2);
        this.mEllipsizedWidth = i;
        this.mEllipsizedStart = 0;
        this.mEllipsizedCount = 0;
        init(charSequence, textPaint, alignment, metrics, z, true);
    }

    public BoringLayout(CharSequence charSequence, TextPaint textPaint, int i, Layout.Alignment alignment, float f, float f2, Metrics metrics, boolean z, TextUtils.TruncateAt truncateAt, int i2) {
        boolean z2;
        super(charSequence, textPaint, i, alignment, f, f2);
        if (truncateAt == null || truncateAt == TextUtils.TruncateAt.MARQUEE) {
            this.mEllipsizedWidth = i;
            this.mEllipsizedStart = 0;
            this.mEllipsizedCount = 0;
            z2 = true;
        } else {
            replaceWith(TextUtils.ellipsize(charSequence, textPaint, i2, truncateAt, true, this), textPaint, i, alignment, f, f2);
            this.mEllipsizedWidth = i2;
            z2 = false;
        }
        init(getText(), textPaint, alignment, metrics, z, z2);
    }

    void init(CharSequence charSequence, TextPaint textPaint, Layout.Alignment alignment, Metrics metrics, boolean z, boolean z2) {
        int i;
        if ((charSequence instanceof String) && alignment == Layout.Alignment.ALIGN_NORMAL) {
            this.mDirect = charSequence.toString();
        } else {
            this.mDirect = null;
        }
        this.mPaint = textPaint;
        if (z) {
            i = metrics.bottom - metrics.top;
            this.mDesc = metrics.bottom;
        } else {
            i = metrics.descent - metrics.ascent;
            this.mDesc = metrics.descent;
        }
        this.mBottom = i;
        if (z2) {
            this.mMax = metrics.width;
        } else {
            TextLine textLineObtain = TextLine.obtain();
            textLineObtain.set(textPaint, charSequence, 0, charSequence.length(), 1, Layout.DIRS_ALL_LEFT_TO_RIGHT, false, null);
            this.mMax = (int) Math.ceil(textLineObtain.metrics(null));
            TextLine.recycle(textLineObtain);
        }
        if (z) {
            this.mTopPadding = metrics.top - metrics.ascent;
            this.mBottomPadding = metrics.bottom - metrics.descent;
        }
    }

    public static Metrics isBoring(CharSequence charSequence, TextPaint textPaint) {
        return isBoring(charSequence, textPaint, TextDirectionHeuristics.FIRSTSTRONG_LTR, null);
    }

    public static Metrics isBoring(CharSequence charSequence, TextPaint textPaint, Metrics metrics) {
        return isBoring(charSequence, textPaint, TextDirectionHeuristics.FIRSTSTRONG_LTR, metrics);
    }

    private static boolean hasAnyInterestingChars(CharSequence charSequence, int i) {
        char[] cArrObtain = TextUtils.obtain(500);
        int i2 = 0;
        while (i2 < i) {
            int i3 = i2 + 500;
            try {
                int iMin = Math.min(i3, i);
                TextUtils.getChars(charSequence, i2, iMin, cArrObtain, 0);
                int i4 = iMin - i2;
                for (int i5 = 0; i5 < i4; i5++) {
                    char c = cArrObtain[i5];
                    if (c == '\n' || c == '\t' || TextUtils.couldAffectRtl(c)) {
                        TextUtils.recycle(cArrObtain);
                        return true;
                    }
                }
                i2 = i3;
            } finally {
                TextUtils.recycle(cArrObtain);
            }
        }
        return false;
    }

    public static Metrics isBoring(CharSequence charSequence, TextPaint textPaint, TextDirectionHeuristic textDirectionHeuristic, Metrics metrics) {
        int length = charSequence.length();
        if (hasAnyInterestingChars(charSequence, length)) {
            return null;
        }
        if (textDirectionHeuristic != null && textDirectionHeuristic.isRtl(charSequence, 0, length)) {
            return null;
        }
        if ((charSequence instanceof Spanned) && ((Spanned) charSequence).getSpans(0, length, ParagraphStyle.class).length > 0) {
            return null;
        }
        if (metrics == null) {
            metrics = new Metrics();
        } else {
            metrics.reset();
        }
        TextLine textLineObtain = TextLine.obtain();
        textLineObtain.set(textPaint, charSequence, 0, length, 1, Layout.DIRS_ALL_LEFT_TO_RIGHT, false, null);
        metrics.width = (int) Math.ceil(textLineObtain.metrics(metrics));
        TextLine.recycle(textLineObtain);
        return metrics;
    }

    @Override
    public int getHeight() {
        return this.mBottom;
    }

    @Override
    public int getLineCount() {
        return 1;
    }

    @Override
    public int getLineTop(int i) {
        if (i == 0) {
            return 0;
        }
        return this.mBottom;
    }

    @Override
    public int getLineDescent(int i) {
        return this.mDesc;
    }

    @Override
    public int getLineStart(int i) {
        if (i == 0) {
            return 0;
        }
        return getText().length();
    }

    @Override
    public int getParagraphDirection(int i) {
        return 1;
    }

    @Override
    public boolean getLineContainsTab(int i) {
        return false;
    }

    @Override
    public float getLineMax(int i) {
        return this.mMax;
    }

    @Override
    public float getLineWidth(int i) {
        if (i == 0) {
            return this.mMax;
        }
        return 0.0f;
    }

    @Override
    public final Layout.Directions getLineDirections(int i) {
        return Layout.DIRS_ALL_LEFT_TO_RIGHT;
    }

    @Override
    public int getTopPadding() {
        return this.mTopPadding;
    }

    @Override
    public int getBottomPadding() {
        return this.mBottomPadding;
    }

    @Override
    public int getEllipsisCount(int i) {
        return this.mEllipsizedCount;
    }

    @Override
    public int getEllipsisStart(int i) {
        return this.mEllipsizedStart;
    }

    @Override
    public int getEllipsizedWidth() {
        return this.mEllipsizedWidth;
    }

    @Override
    public void draw(Canvas canvas, Path path, Paint paint, int i) {
        if (this.mDirect != null && path == null) {
            canvas.drawText(this.mDirect, 0.0f, this.mBottom - this.mDesc, this.mPaint);
        } else {
            super.draw(canvas, path, paint, i);
        }
    }

    @Override
    public void ellipsized(int i, int i2) {
        this.mEllipsizedStart = i;
        this.mEllipsizedCount = i2 - i;
    }

    public static class Metrics extends Paint.FontMetricsInt {
        public int width;

        @Override
        public String toString() {
            return super.toString() + " width=" + this.width;
        }

        private void reset() {
            this.top = 0;
            this.bottom = 0;
            this.ascent = 0;
            this.descent = 0;
            this.width = 0;
            this.leading = 0;
        }
    }
}
