package android.text;

import android.graphics.Paint;
import android.text.style.MetricAffectingSpan;
import android.text.style.ReplacementSpan;
import com.android.internal.util.ArrayUtils;

class MeasuredText {
    private static final boolean localLOGV = false;
    char[] mChars;
    int mDir;
    boolean mEasy;
    int mLen;
    byte[] mLevels;
    private int mPos;
    CharSequence mText;
    int mTextStart;
    float[] mWidths;
    private TextPaint mWorkPaint = new TextPaint();
    private static final Object[] sLock = new Object[0];
    private static final MeasuredText[] sCached = new MeasuredText[3];

    private MeasuredText() {
    }

    static MeasuredText obtain() {
        synchronized (sLock) {
            int i = sCached.length;
            do {
                i--;
                if (i < 0) {
                    MeasuredText mt = new MeasuredText();
                    return mt;
                }
            } while (sCached[i] == null);
            MeasuredText mt2 = sCached[i];
            sCached[i] = null;
            return mt2;
        }
    }

    static MeasuredText recycle(MeasuredText mt) {
        mt.mText = null;
        if (mt.mLen < 1000) {
            synchronized (sLock) {
                int i = 0;
                while (true) {
                    if (i >= sCached.length) {
                        break;
                    }
                    if (sCached[i] != null) {
                        i++;
                    } else {
                        sCached[i] = mt;
                        mt.mText = null;
                        break;
                    }
                }
            }
        }
        return null;
    }

    void setPos(int pos) {
        this.mPos = pos - this.mTextStart;
    }

    void setPara(CharSequence text, int start, int end, TextDirectionHeuristic textDir) {
        int bidiRequest;
        this.mText = text;
        this.mTextStart = start;
        int len = end - start;
        this.mLen = len;
        this.mPos = 0;
        if (this.mWidths == null || this.mWidths.length < len) {
            this.mWidths = ArrayUtils.newUnpaddedFloatArray(len);
        }
        if (this.mChars == null || this.mChars.length < len) {
            this.mChars = ArrayUtils.newUnpaddedCharArray(len);
        }
        TextUtils.getChars(text, start, end, this.mChars, 0);
        if (text instanceof Spanned) {
            Spanned spanned = (Spanned) text;
            ReplacementSpan[] spans = (ReplacementSpan[]) spanned.getSpans(start, end, ReplacementSpan.class);
            for (int i = 0; i < spans.length; i++) {
                int startInPara = spanned.getSpanStart(spans[i]) - start;
                int endInPara = spanned.getSpanEnd(spans[i]) - start;
                if (startInPara < 0) {
                    startInPara = 0;
                }
                if (endInPara > len) {
                    endInPara = len;
                }
                for (int j = startInPara; j < endInPara; j++) {
                    this.mChars[j] = 65532;
                }
            }
        }
        if ((textDir == TextDirectionHeuristics.LTR || textDir == TextDirectionHeuristics.FIRSTSTRONG_LTR || textDir == TextDirectionHeuristics.ANYRTL_LTR) && TextUtils.doesNotNeedBidi(this.mChars, 0, len)) {
            this.mDir = 1;
            this.mEasy = true;
            return;
        }
        if (this.mLevels == null || this.mLevels.length < len) {
            this.mLevels = ArrayUtils.newUnpaddedByteArray(len);
        }
        if (textDir == TextDirectionHeuristics.LTR) {
            bidiRequest = 1;
        } else if (textDir == TextDirectionHeuristics.RTL) {
            bidiRequest = -1;
        } else if (textDir == TextDirectionHeuristics.FIRSTSTRONG_LTR) {
            bidiRequest = 2;
        } else if (textDir == TextDirectionHeuristics.FIRSTSTRONG_RTL) {
            bidiRequest = -2;
        } else {
            boolean isRtl = textDir.isRtl(this.mChars, 0, len);
            bidiRequest = isRtl ? -1 : 1;
        }
        this.mDir = AndroidBidi.bidi(bidiRequest, this.mChars, this.mLevels, len, false);
        this.mEasy = false;
    }

    float addStyleRun(TextPaint paint, int len, Paint.FontMetricsInt fm) {
        if (fm != null) {
            paint.getFontMetricsInt(fm);
        }
        int p = this.mPos;
        this.mPos = p + len;
        if (this.mEasy) {
            boolean isRtl = this.mDir != 1;
            return paint.getTextRunAdvances(this.mChars, p, len, p, len, isRtl, this.mWidths, p);
        }
        float totalAdvance = 0.0f;
        int level = this.mLevels[p];
        int q = p;
        int i = p + 1;
        int e = p + len;
        while (true) {
            if (i == e || this.mLevels[i] != level) {
                boolean isRtl2 = (level & 1) != 0;
                totalAdvance += paint.getTextRunAdvances(this.mChars, q, i - q, q, i - q, isRtl2, this.mWidths, q);
                if (i != e) {
                    q = i;
                    level = this.mLevels[i];
                } else {
                    return totalAdvance;
                }
            }
            i++;
        }
    }

    float addStyleRun(TextPaint paint, MetricAffectingSpan[] spans, int len, Paint.FontMetricsInt fm) {
        float wid;
        TextPaint workPaint = this.mWorkPaint;
        workPaint.set(paint);
        workPaint.baselineShift = 0;
        ReplacementSpan replacement = null;
        for (MetricAffectingSpan span : spans) {
            if (span instanceof ReplacementSpan) {
                replacement = (ReplacementSpan) span;
            } else {
                span.updateMeasureState(workPaint);
            }
        }
        if (replacement == null) {
            wid = addStyleRun(workPaint, len, fm);
        } else {
            wid = replacement.getSize(workPaint, this.mText, this.mTextStart + this.mPos, this.mTextStart + this.mPos + len, fm);
            float[] w = this.mWidths;
            w[this.mPos] = wid;
            int e = this.mPos + len;
            for (int i = this.mPos + 1; i < e; i++) {
                w[i] = 0.0f;
            }
            this.mPos += len;
        }
        if (fm != null) {
            if (workPaint.baselineShift < 0) {
                fm.ascent += workPaint.baselineShift;
                fm.top += workPaint.baselineShift;
            } else {
                fm.descent += workPaint.baselineShift;
                fm.bottom += workPaint.baselineShift;
            }
        }
        return wid;
    }

    int breakText(int limit, boolean forwards, float width) {
        float[] w = this.mWidths;
        if (forwards) {
            int i = 0;
            while (i < limit) {
                width -= w[i];
                if (width < 0.0f) {
                    break;
                }
                i++;
            }
            while (i > 0 && this.mChars[i - 1] == ' ') {
                i--;
            }
            return i;
        }
        int i2 = limit - 1;
        while (i2 >= 0) {
            width -= w[i2];
            if (width < 0.0f) {
                break;
            }
            i2--;
        }
        while (i2 < limit - 1 && this.mChars[i2 + 1] == ' ') {
            i2++;
        }
        return (limit - i2) - 1;
    }

    float measure(int start, int limit) {
        float width = 0.0f;
        float[] w = this.mWidths;
        for (int i = start; i < limit; i++) {
            width += w[i];
        }
        return width;
    }
}
