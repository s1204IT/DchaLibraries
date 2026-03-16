package android.text;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.CharacterStyle;
import android.text.style.MetricAffectingSpan;
import android.text.style.ReplacementSpan;

public class Styled {
    private static float drawUniformRun(Canvas canvas, Spanned text, int start, int end, int dir, boolean runIsRtl, float x, int top, int y, int bottom, Paint.FontMetricsInt fmi, TextPaint paint, TextPaint workPaint, boolean needWidth) {
        CharSequence tmp;
        int tmpstart;
        int tmpend;
        boolean haveWidth = false;
        float ret = 0.0f;
        CharacterStyle[] spans = (CharacterStyle[]) text.getSpans(start, end, CharacterStyle.class);
        ReplacementSpan replacement = null;
        paint.bgColor = 0;
        paint.baselineShift = 0;
        workPaint.set(paint);
        if (spans.length > 0) {
            for (CharacterStyle span : spans) {
                if (span instanceof ReplacementSpan) {
                    replacement = (ReplacementSpan) span;
                } else {
                    span.updateDrawState(workPaint);
                }
            }
        }
        if (replacement == null) {
            if (runIsRtl) {
                tmp = TextUtils.getReverse(text, start, end);
                tmpstart = 0;
                tmpend = end - start;
            } else {
                tmp = text;
                tmpstart = start;
                tmpend = end;
            }
            if (fmi != null) {
                workPaint.getFontMetricsInt(fmi);
            }
            if (canvas != null) {
                if (workPaint.bgColor != 0) {
                    int c = workPaint.getColor();
                    Paint.Style s = workPaint.getStyle();
                    workPaint.setColor(workPaint.bgColor);
                    workPaint.setStyle(Paint.Style.FILL);
                    if (0 == 0) {
                        ret = workPaint.measureText(tmp, tmpstart, tmpend);
                        haveWidth = true;
                    }
                    if (dir == -1) {
                        canvas.drawRect(x - ret, top, x, bottom, workPaint);
                    } else {
                        canvas.drawRect(x, top, x + ret, bottom, workPaint);
                    }
                    workPaint.setStyle(s);
                    workPaint.setColor(c);
                }
                if (dir == -1) {
                    if (!haveWidth) {
                        ret = workPaint.measureText(tmp, tmpstart, tmpend);
                    }
                    canvas.drawText(tmp, tmpstart, tmpend, x - ret, workPaint.baselineShift + y, workPaint);
                } else {
                    if (needWidth && !haveWidth) {
                        ret = workPaint.measureText(tmp, tmpstart, tmpend);
                    }
                    canvas.drawText(tmp, tmpstart, tmpend, x, workPaint.baselineShift + y, workPaint);
                }
            } else if (needWidth && 0 == 0) {
                ret = workPaint.measureText(tmp, tmpstart, tmpend);
            }
        } else {
            ret = replacement.getSize(workPaint, text, start, end, fmi);
            if (canvas != null) {
                if (dir == -1) {
                    replacement.draw(canvas, text, start, end, x - ret, top, y, bottom, workPaint);
                } else {
                    replacement.draw(canvas, text, start, end, x, top, y, bottom, workPaint);
                }
            }
        }
        if (dir == -1) {
            return -ret;
        }
        return ret;
    }

    public static int getTextWidths(TextPaint paint, TextPaint workPaint, Spanned text, int start, int end, float[] widths, Paint.FontMetricsInt fmi) {
        MetricAffectingSpan[] spans = (MetricAffectingSpan[]) text.getSpans(start, end, MetricAffectingSpan.class);
        ReplacementSpan replacement = null;
        workPaint.set(paint);
        for (MetricAffectingSpan span : spans) {
            if (span instanceof ReplacementSpan) {
                replacement = (ReplacementSpan) span;
            } else {
                span.updateMeasureState(workPaint);
            }
        }
        if (replacement == null) {
            workPaint.getFontMetricsInt(fmi);
            workPaint.getTextWidths(text, start, end, widths);
        } else {
            int wid = replacement.getSize(workPaint, text, start, end, fmi);
            if (end > start) {
                widths[0] = wid;
                for (int i = start + 1; i < end; i++) {
                    widths[i - start] = 0.0f;
                }
            }
        }
        return end - start;
    }

    private static float drawDirectionalRun(Canvas canvas, CharSequence text, int start, int end, int dir, boolean runIsRtl, float x, int top, int y, int bottom, Paint.FontMetricsInt fmi, TextPaint paint, TextPaint workPaint, boolean needWidth) {
        Class<?> division;
        if (!(text instanceof Spanned)) {
            float ret = 0.0f;
            if (runIsRtl) {
                CharSequence tmp = TextUtils.getReverse(text, start, end);
                int tmpend = end - start;
                if (canvas != null || needWidth) {
                    ret = paint.measureText(tmp, 0, tmpend);
                }
                if (canvas != null) {
                    canvas.drawText(tmp, 0, tmpend, x - ret, y, paint);
                }
            } else {
                if (needWidth) {
                    ret = paint.measureText(text, start, end);
                }
                if (canvas != null) {
                    canvas.drawText(text, start, end, x, y, paint);
                }
            }
            if (fmi != null) {
                paint.getFontMetricsInt(fmi);
            }
            return dir * ret;
        }
        int minAscent = 0;
        int maxDescent = 0;
        int minTop = 0;
        int maxBottom = 0;
        Spanned sp = (Spanned) text;
        if (canvas == null) {
            division = MetricAffectingSpan.class;
        } else {
            division = CharacterStyle.class;
        }
        int i = start;
        while (i < end) {
            int next = sp.nextSpanTransition(i, end, division);
            x += drawUniformRun(canvas, sp, i, next, dir, runIsRtl, x, top, y, bottom, fmi, paint, workPaint, needWidth || next != end);
            if (fmi != null) {
                if (fmi.ascent < minAscent) {
                    minAscent = fmi.ascent;
                }
                if (fmi.descent > maxDescent) {
                    maxDescent = fmi.descent;
                }
                if (fmi.top < minTop) {
                    minTop = fmi.top;
                }
                if (fmi.bottom > maxBottom) {
                    maxBottom = fmi.bottom;
                }
            }
            i = next;
        }
        if (fmi != null) {
            if (start == end) {
                paint.getFontMetricsInt(fmi);
            } else {
                fmi.ascent = minAscent;
                fmi.descent = maxDescent;
                fmi.top = minTop;
                fmi.bottom = maxBottom;
            }
        }
        return x - x;
    }

    static float drawText(Canvas canvas, CharSequence text, int start, int end, int dir, boolean runIsRtl, float x, int top, int y, int bottom, TextPaint paint, TextPaint workPaint, boolean needWidth) {
        if ((dir == -1 && !runIsRtl) || (runIsRtl && dir == 1)) {
            float ch = drawDirectionalRun(null, text, start, end, 1, false, 0.0f, 0, 0, 0, null, paint, workPaint, true);
            float ch2 = ch * dir;
            drawDirectionalRun(canvas, text, start, end, -dir, runIsRtl, x + ch2, top, y, bottom, null, paint, workPaint, true);
            return ch2;
        }
        float ch3 = drawDirectionalRun(canvas, text, start, end, dir, runIsRtl, x, top, y, bottom, null, paint, workPaint, needWidth);
        return ch3;
    }

    public static float drawText(Canvas canvas, CharSequence text, int start, int end, int direction, float x, int top, int y, int bottom, TextPaint paint, TextPaint workPaint, boolean needWidth) {
        int direction2 = direction >= 0 ? 1 : -1;
        return drawText(canvas, text, start, end, direction2, false, x, top, y, bottom, paint, workPaint, needWidth);
    }

    public static float measureText(TextPaint paint, TextPaint workPaint, CharSequence text, int start, int end, Paint.FontMetricsInt fmi) {
        return drawDirectionalRun(null, text, start, end, 1, false, 0.0f, 0, 0, 0, fmi, paint, workPaint, true);
    }
}
