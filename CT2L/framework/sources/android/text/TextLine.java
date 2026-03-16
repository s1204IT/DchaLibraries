package android.text;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.Layout;
import android.text.style.CharacterStyle;
import android.text.style.MetricAffectingSpan;
import android.text.style.ReplacementSpan;
import com.android.internal.util.ArrayUtils;

class TextLine {
    private static final boolean DEBUG = false;
    private static final int TAB_INCREMENT = 20;
    private static final TextLine[] sCached = new TextLine[3];
    private char[] mChars;
    private boolean mCharsValid;
    private int mDir;
    private Layout.Directions mDirections;
    private boolean mHasTabs;
    private int mLen;
    private TextPaint mPaint;
    private Spanned mSpanned;
    private int mStart;
    private Layout.TabStops mTabs;
    private CharSequence mText;
    private final TextPaint mWorkPaint = new TextPaint();
    private final SpanSet<MetricAffectingSpan> mMetricAffectingSpanSpanSet = new SpanSet<>(MetricAffectingSpan.class);
    private final SpanSet<CharacterStyle> mCharacterStyleSpanSet = new SpanSet<>(CharacterStyle.class);
    private final SpanSet<ReplacementSpan> mReplacementSpanSpanSet = new SpanSet<>(ReplacementSpan.class);

    TextLine() {
    }

    static TextLine obtain() {
        synchronized (sCached) {
            int i = sCached.length;
            do {
                i--;
                if (i < 0) {
                    TextLine tl = new TextLine();
                    return tl;
                }
            } while (sCached[i] == null);
            TextLine tl2 = sCached[i];
            sCached[i] = null;
            return tl2;
        }
    }

    static TextLine recycle(TextLine tl) {
        tl.mText = null;
        tl.mPaint = null;
        tl.mDirections = null;
        tl.mSpanned = null;
        tl.mTabs = null;
        tl.mChars = null;
        tl.mMetricAffectingSpanSpanSet.recycle();
        tl.mCharacterStyleSpanSet.recycle();
        tl.mReplacementSpanSpanSet.recycle();
        synchronized (sCached) {
            int i = 0;
            while (true) {
                if (i >= sCached.length) {
                    break;
                }
                if (sCached[i] != null) {
                    i++;
                } else {
                    sCached[i] = tl;
                    break;
                }
            }
        }
        return null;
    }

    void set(TextPaint paint, CharSequence text, int start, int limit, int dir, Layout.Directions directions, boolean hasTabs, Layout.TabStops tabStops) {
        this.mPaint = paint;
        this.mText = text;
        this.mStart = start;
        this.mLen = limit - start;
        this.mDir = dir;
        this.mDirections = directions;
        if (this.mDirections == null) {
            throw new IllegalArgumentException("Directions cannot be null");
        }
        this.mHasTabs = hasTabs;
        this.mSpanned = null;
        boolean hasReplacement = false;
        if (text instanceof Spanned) {
            this.mSpanned = (Spanned) text;
            this.mReplacementSpanSpanSet.init(this.mSpanned, start, limit);
            hasReplacement = this.mReplacementSpanSpanSet.numberOfSpans > 0;
        }
        this.mCharsValid = hasReplacement || hasTabs || directions != Layout.DIRS_ALL_LEFT_TO_RIGHT;
        if (this.mCharsValid) {
            if (this.mChars == null || this.mChars.length < this.mLen) {
                this.mChars = ArrayUtils.newUnpaddedCharArray(this.mLen);
            }
            TextUtils.getChars(text, start, limit, this.mChars, 0);
            if (hasReplacement) {
                char[] chars = this.mChars;
                int i = start;
                while (i < limit) {
                    int inext = this.mReplacementSpanSpanSet.getNextTransition(i, limit);
                    if (this.mReplacementSpanSpanSet.hasSpansIntersecting(i, inext)) {
                        chars[i - start] = 65532;
                        int e = inext - start;
                        for (int j = (i - start) + 1; j < e; j++) {
                            chars[j] = 65279;
                        }
                    }
                    i = inext;
                }
            }
        }
        this.mTabs = tabStops;
    }

    void draw(Canvas canvas, float f, int i, int i2, int i3) {
        if (!this.mHasTabs) {
            if (this.mDirections == Layout.DIRS_ALL_LEFT_TO_RIGHT) {
                drawRun(canvas, 0, this.mLen, false, f, i, i2, i3, false);
                return;
            } else if (this.mDirections == Layout.DIRS_ALL_RIGHT_TO_LEFT) {
                drawRun(canvas, 0, this.mLen, true, f, i, i2, i3, false);
                return;
            }
        }
        float fDrawRun = 0.0f;
        int[] iArr = this.mDirections.mDirections;
        RectF rectF = null;
        int length = iArr.length - 2;
        int i4 = 0;
        while (i4 < iArr.length) {
            int i5 = iArr[i4];
            int i6 = i5 + (iArr[i4 + 1] & 67108863);
            if (i6 > this.mLen) {
                i6 = this.mLen;
            }
            boolean z = (iArr[i4 + 1] & 67108864) != 0;
            int i7 = i5;
            int i8 = this.mHasTabs ? i5 : i6;
            while (i8 <= i6) {
                int i9 = 0;
                i9 = 0;
                Bitmap bitmapFromAndroidPua = null;
                if (this.mHasTabs && i8 < i6) {
                    char c = this.mChars[i8];
                    i9 = c;
                    if (c >= 55296) {
                        i9 = c;
                        if (c < 56320) {
                            i9 = c;
                            if (i8 + 1 < i6) {
                                int iCodePointAt = Character.codePointAt(this.mChars, i8);
                                if (iCodePointAt >= Layout.MIN_EMOJI && iCodePointAt <= Layout.MAX_EMOJI) {
                                    bitmapFromAndroidPua = Layout.EMOJI_FACTORY.getBitmapFromAndroidPua(iCodePointAt);
                                    i9 = iCodePointAt;
                                } else {
                                    i9 = iCodePointAt;
                                    if (iCodePointAt > 65535) {
                                        i8++;
                                    }
                                }
                                if (i8 != i6) {
                                    if (i4 == length) {
                                        fDrawRun += drawRun(canvas, i7, i8, z, f + fDrawRun, i, i2, i3, i4 == length || i8 != this.mLen);
                                        if (i9 != 9) {
                                        }
                                        i7 = i8 + 1;
                                    }
                                }
                            }
                        }
                    }
                } else if (i8 != i6 || i9 == 9 || bitmapFromAndroidPua != null) {
                    fDrawRun += drawRun(canvas, i7, i8, z, f + fDrawRun, i, i2, i3, i4 == length || i8 != this.mLen);
                    if (i9 != 9) {
                        fDrawRun = this.mDir * nextTab(this.mDir * fDrawRun);
                    } else if (bitmapFromAndroidPua != null) {
                        float fAscent = ascent(i8);
                        float width = bitmapFromAndroidPua.getWidth() * ((-fAscent) / bitmapFromAndroidPua.getHeight());
                        if (rectF == null) {
                            rectF = new RectF();
                        }
                        rectF.set(f + fDrawRun, i2 + fAscent, f + fDrawRun + width, i2);
                        canvas.drawBitmap(bitmapFromAndroidPua, (Rect) null, rectF, this.mPaint);
                        fDrawRun += width;
                        i8++;
                    }
                    i7 = i8 + 1;
                }
                i8++;
            }
            i4 += 2;
        }
    }

    float metrics(Paint.FontMetricsInt fmi) {
        return measure(this.mLen, false, fmi);
    }

    float measure(int i, boolean z, Paint.FontMetricsInt fontMetricsInt) {
        boolean z2;
        boolean z3;
        int i2 = z ? i - 1 : i;
        if (i2 < 0) {
            return 0.0f;
        }
        float width = 0.0f;
        if (!this.mHasTabs) {
            if (this.mDirections == Layout.DIRS_ALL_LEFT_TO_RIGHT) {
                return measureRun(0, i, this.mLen, false, fontMetricsInt);
            }
            if (this.mDirections == Layout.DIRS_ALL_RIGHT_TO_LEFT) {
                return measureRun(0, i, this.mLen, true, fontMetricsInt);
            }
        }
        char[] cArr = this.mChars;
        int[] iArr = this.mDirections.mDirections;
        for (int i3 = 0; i3 < iArr.length; i3 += 2) {
            int i4 = iArr[i3];
            int i5 = i4 + (iArr[i3 + 1] & 67108863);
            if (i5 > this.mLen) {
                i5 = this.mLen;
            }
            boolean z4 = (iArr[i3 + 1] & 67108864) != 0;
            int i6 = i4;
            int i7 = this.mHasTabs ? i4 : i5;
            while (i7 <= i5) {
                int i8 = 0;
                i8 = 0;
                Bitmap bitmapFromAndroidPua = null;
                if (this.mHasTabs && i7 < i5) {
                    char c = cArr[i7];
                    i8 = c;
                    if (c >= 55296) {
                        i8 = c;
                        if (c < 56320) {
                            i8 = c;
                            if (i7 + 1 < i5) {
                                int iCodePointAt = Character.codePointAt(cArr, i7);
                                if (iCodePointAt >= Layout.MIN_EMOJI && iCodePointAt <= Layout.MAX_EMOJI) {
                                    bitmapFromAndroidPua = Layout.EMOJI_FACTORY.getBitmapFromAndroidPua(iCodePointAt);
                                    i8 = iCodePointAt;
                                } else {
                                    i8 = iCodePointAt;
                                    if (iCodePointAt > 65535) {
                                        i7++;
                                    }
                                }
                                if (i7 != i5) {
                                    if (i2 < i6) {
                                        if ((this.mDir != -1) != z4) {
                                        }
                                        if (!z2) {
                                        }
                                        float fMeasureRun = measureRun(i6, i7, i7, z4, fontMetricsInt);
                                        if (!z3) {
                                        }
                                        width += fMeasureRun;
                                        if (!z2) {
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (i7 != i5 || i8 == 9 || bitmapFromAndroidPua != null) {
                    z2 = i2 < i6 && i2 < i7;
                    z3 = (this.mDir != -1) != z4;
                    if (!z2 && z3) {
                        return width + measureRun(i6, i, i7, z4, fontMetricsInt);
                    }
                    float fMeasureRun2 = measureRun(i6, i7, i7, z4, fontMetricsInt);
                    if (!z3) {
                        fMeasureRun2 = -fMeasureRun2;
                    }
                    width += fMeasureRun2;
                    if (!z2) {
                        return width + measureRun(i6, i, i7, z4, null);
                    }
                    if (i8 == 9) {
                        if (i != i7) {
                            width = this.mDir * nextTab(this.mDir * width);
                            if (i2 == i7) {
                                return width;
                            }
                        } else {
                            return width;
                        }
                    }
                    if (bitmapFromAndroidPua != null) {
                        width += this.mDir * ((bitmapFromAndroidPua.getWidth() * (-ascent(i7))) / bitmapFromAndroidPua.getHeight());
                        i7++;
                    }
                    i6 = i7 + 1;
                }
                i7++;
            }
        }
        return width;
    }

    private float drawRun(Canvas c, int start, int limit, boolean runIsRtl, float x, int top, int y, int bottom, boolean needWidth) {
        if ((this.mDir == 1) == runIsRtl) {
            float w = -measureRun(start, limit, limit, runIsRtl, null);
            handleRun(start, limit, limit, runIsRtl, c, x + w, top, y, bottom, null, false);
            return w;
        }
        return handleRun(start, limit, limit, runIsRtl, c, x, top, y, bottom, null, needWidth);
    }

    private float measureRun(int start, int offset, int limit, boolean runIsRtl, Paint.FontMetricsInt fmi) {
        return handleRun(start, offset, limit, runIsRtl, null, 0.0f, 0, 0, 0, fmi, true);
    }

    int getOffsetToLeftRightOf(int cursor, boolean toLeft) {
        int runIndex;
        int lineEnd = this.mLen;
        boolean paraIsRtl = this.mDir == -1;
        int[] runs = this.mDirections.mDirections;
        int runLevel = 0;
        int runStart = 0;
        int runLimit = lineEnd;
        int newCaret = -1;
        boolean trailing = false;
        if (cursor == 0) {
            runIndex = -2;
        } else if (cursor == lineEnd) {
            runIndex = runs.length;
        } else {
            runIndex = 0;
            while (true) {
                if (runIndex >= runs.length) {
                    break;
                }
                runStart = 0 + runs[runIndex];
                if (cursor >= runStart) {
                    runLimit = runStart + (runs[runIndex + 1] & 67108863);
                    if (runLimit > lineEnd) {
                        runLimit = lineEnd;
                    }
                    if (cursor < runLimit) {
                        runLevel = (runs[runIndex + 1] >>> 26) & 63;
                        if (cursor == runStart) {
                            int pos = cursor - 1;
                            int prevRunIndex = 0;
                            while (true) {
                                if (prevRunIndex >= runs.length) {
                                    break;
                                }
                                int prevRunStart = 0 + runs[prevRunIndex];
                                if (pos >= prevRunStart) {
                                    int prevRunLimit = prevRunStart + (runs[prevRunIndex + 1] & 67108863);
                                    if (prevRunLimit > lineEnd) {
                                        prevRunLimit = lineEnd;
                                    }
                                    if (pos < prevRunLimit) {
                                        int prevRunLevel = (runs[prevRunIndex + 1] >>> 26) & 63;
                                        if (prevRunLevel < runLevel) {
                                            runIndex = prevRunIndex;
                                            runLevel = prevRunLevel;
                                            runStart = prevRunStart;
                                            runLimit = prevRunLimit;
                                            trailing = true;
                                            break;
                                        }
                                    } else {
                                        continue;
                                    }
                                }
                                prevRunIndex += 2;
                            }
                        }
                    }
                }
                runIndex += 2;
            }
            if (runIndex != runs.length) {
                boolean runIsRtl = (runLevel & 1) != 0;
                boolean advance = toLeft == runIsRtl;
                if (cursor != (advance ? runLimit : runStart) || advance != trailing) {
                    newCaret = getOffsetBeforeAfter(runIndex, runStart, runLimit, runIsRtl, cursor, advance);
                    if (newCaret != (advance ? runLimit : runStart)) {
                        return newCaret;
                    }
                }
            }
        }
        while (true) {
            boolean advance2 = toLeft == paraIsRtl;
            int otherRunIndex = runIndex + (advance2 ? 2 : -2);
            if (otherRunIndex < 0 || otherRunIndex >= runs.length) {
                break;
            }
            int otherRunStart = 0 + runs[otherRunIndex];
            int otherRunLimit = otherRunStart + (runs[otherRunIndex + 1] & 67108863);
            if (otherRunLimit > lineEnd) {
                otherRunLimit = lineEnd;
            }
            int otherRunLevel = (runs[otherRunIndex + 1] >>> 26) & 63;
            boolean otherRunIsRtl = (otherRunLevel & 1) != 0;
            boolean advance3 = toLeft == otherRunIsRtl;
            if (newCaret == -1) {
                newCaret = getOffsetBeforeAfter(otherRunIndex, otherRunStart, otherRunLimit, otherRunIsRtl, advance3 ? otherRunStart : otherRunLimit, advance3);
                if (!advance3) {
                    otherRunLimit = otherRunStart;
                }
                if (newCaret != otherRunLimit) {
                    break;
                }
                runIndex = otherRunIndex;
                runLevel = otherRunLevel;
            } else if (otherRunLevel < runLevel) {
                newCaret = advance3 ? otherRunStart : otherRunLimit;
            }
        }
        return newCaret;
    }

    private int getOffsetBeforeAfter(int runIndex, int runStart, int runLimit, boolean runIsRtl, int offset, boolean after) {
        int spanLimit;
        if (runIndex >= 0) {
            if (offset != (after ? this.mLen : 0)) {
                TextPaint wp = this.mWorkPaint;
                wp.set(this.mPaint);
                int spanStart = runStart;
                if (this.mSpanned == null) {
                    spanLimit = runLimit;
                } else {
                    int target = after ? offset + 1 : offset;
                    int limit = this.mStart + runLimit;
                    while (true) {
                        spanLimit = this.mSpanned.nextSpanTransition(this.mStart + spanStart, limit, MetricAffectingSpan.class) - this.mStart;
                        if (spanLimit >= target) {
                            break;
                        }
                        spanStart = spanLimit;
                    }
                    MetricAffectingSpan[] spans = (MetricAffectingSpan[]) TextUtils.removeEmptySpans((MetricAffectingSpan[]) this.mSpanned.getSpans(this.mStart + spanStart, this.mStart + spanLimit, MetricAffectingSpan.class), this.mSpanned, MetricAffectingSpan.class);
                    if (spans.length > 0) {
                        ReplacementSpan replacement = null;
                        for (MetricAffectingSpan span : spans) {
                            if (span instanceof ReplacementSpan) {
                                replacement = (ReplacementSpan) span;
                            } else {
                                span.updateMeasureState(wp);
                            }
                        }
                        if (replacement != null) {
                            return !after ? spanStart : spanLimit;
                        }
                    }
                }
                int dir = runIsRtl ? 1 : 0;
                int cursorOpt = after ? 0 : 2;
                if (this.mCharsValid) {
                    return wp.getTextRunCursor(this.mChars, spanStart, spanLimit - spanStart, dir, offset, cursorOpt);
                }
                return wp.getTextRunCursor(this.mText, this.mStart + spanStart, this.mStart + spanLimit, dir, this.mStart + offset, cursorOpt) - this.mStart;
            }
        }
        if (after) {
            return TextUtils.getOffsetAfter(this.mText, this.mStart + offset) - this.mStart;
        }
        return TextUtils.getOffsetBefore(this.mText, this.mStart + offset) - this.mStart;
    }

    private static void expandMetricsFromPaint(Paint.FontMetricsInt fmi, TextPaint wp) {
        int previousTop = fmi.top;
        int previousAscent = fmi.ascent;
        int previousDescent = fmi.descent;
        int previousBottom = fmi.bottom;
        int previousLeading = fmi.leading;
        wp.getFontMetricsInt(fmi);
        updateMetrics(fmi, previousTop, previousAscent, previousDescent, previousBottom, previousLeading);
    }

    static void updateMetrics(Paint.FontMetricsInt fmi, int previousTop, int previousAscent, int previousDescent, int previousBottom, int previousLeading) {
        fmi.top = Math.min(fmi.top, previousTop);
        fmi.ascent = Math.min(fmi.ascent, previousAscent);
        fmi.descent = Math.max(fmi.descent, previousDescent);
        fmi.bottom = Math.max(fmi.bottom, previousBottom);
        fmi.leading = Math.max(fmi.leading, previousLeading);
    }

    private float handleText(TextPaint wp, int start, int end, int contextStart, int contextEnd, boolean runIsRtl, Canvas c, float x, int top, int y, int bottom, Paint.FontMetricsInt fmi, boolean needWidth) {
        if (fmi != null) {
            expandMetricsFromPaint(fmi, wp);
        }
        int runLen = end - start;
        if (runLen == 0) {
            return 0.0f;
        }
        float ret = 0.0f;
        int contextLen = contextEnd - contextStart;
        if (needWidth || (c != null && (wp.bgColor != 0 || wp.underlineColor != 0 || runIsRtl))) {
            if (this.mCharsValid) {
                ret = wp.getTextRunAdvances(this.mChars, start, runLen, contextStart, contextLen, runIsRtl, (float[]) null, 0);
            } else {
                int delta = this.mStart;
                ret = wp.getTextRunAdvances(this.mText, delta + start, delta + end, delta + contextStart, delta + contextEnd, runIsRtl, (float[]) null, 0);
            }
        }
        if (c != null) {
            if (runIsRtl) {
                x -= ret;
            }
            if (wp.bgColor != 0) {
                int previousColor = wp.getColor();
                Paint.Style previousStyle = wp.getStyle();
                wp.setColor(wp.bgColor);
                wp.setStyle(Paint.Style.FILL);
                c.drawRect(x, top, x + ret, bottom, wp);
                wp.setStyle(previousStyle);
                wp.setColor(previousColor);
            }
            if (wp.underlineColor != 0) {
                float underlineTop = wp.baselineShift + y + (0.11111111f * wp.getTextSize());
                int previousColor2 = wp.getColor();
                Paint.Style previousStyle2 = wp.getStyle();
                boolean previousAntiAlias = wp.isAntiAlias();
                wp.setStyle(Paint.Style.FILL);
                wp.setAntiAlias(true);
                wp.setColor(wp.underlineColor);
                c.drawRect(x, underlineTop, x + ret, underlineTop + wp.underlineThickness, wp);
                wp.setStyle(previousStyle2);
                wp.setColor(previousColor2);
                wp.setAntiAlias(previousAntiAlias);
            }
            drawTextRun(c, wp, start, end, contextStart, contextEnd, runIsRtl, x, y + wp.baselineShift);
        }
        return runIsRtl ? -ret : ret;
    }

    private float handleReplacement(ReplacementSpan replacement, TextPaint wp, int start, int limit, boolean runIsRtl, Canvas c, float x, int top, int y, int bottom, Paint.FontMetricsInt fmi, boolean needWidth) {
        float ret = 0.0f;
        int textStart = this.mStart + start;
        int textLimit = this.mStart + limit;
        if (needWidth || (c != null && runIsRtl)) {
            int previousTop = 0;
            int previousAscent = 0;
            int previousDescent = 0;
            int previousBottom = 0;
            int previousLeading = 0;
            boolean needUpdateMetrics = fmi != null;
            if (needUpdateMetrics) {
                previousTop = fmi.top;
                previousAscent = fmi.ascent;
                previousDescent = fmi.descent;
                previousBottom = fmi.bottom;
                previousLeading = fmi.leading;
            }
            ret = replacement.getSize(wp, this.mText, textStart, textLimit, fmi);
            if (needUpdateMetrics) {
                updateMetrics(fmi, previousTop, previousAscent, previousDescent, previousBottom, previousLeading);
            }
        }
        if (c != null) {
            if (runIsRtl) {
                x -= ret;
            }
            replacement.draw(c, this.mText, textStart, textLimit, x, top, y, bottom, wp);
        }
        return runIsRtl ? -ret : ret;
    }

    private float handleRun(int start, int measureLimit, int limit, boolean runIsRtl, Canvas c, float x, int top, int y, int bottom, Paint.FontMetricsInt fmi, boolean needWidth) {
        if (start == measureLimit) {
            TextPaint wp = this.mWorkPaint;
            wp.set(this.mPaint);
            if (fmi != null) {
                expandMetricsFromPaint(fmi, wp);
            }
            return 0.0f;
        }
        if (this.mSpanned == null) {
            TextPaint wp2 = this.mWorkPaint;
            wp2.set(this.mPaint);
            return handleText(wp2, start, measureLimit, start, limit, runIsRtl, c, x, top, y, bottom, fmi, needWidth || measureLimit < measureLimit);
        }
        this.mMetricAffectingSpanSpanSet.init(this.mSpanned, this.mStart + start, this.mStart + limit);
        this.mCharacterStyleSpanSet.init(this.mSpanned, this.mStart + start, this.mStart + limit);
        int i = start;
        while (i < measureLimit) {
            TextPaint wp3 = this.mWorkPaint;
            wp3.set(this.mPaint);
            int inext = this.mMetricAffectingSpanSpanSet.getNextTransition(this.mStart + i, this.mStart + limit) - this.mStart;
            int mlimit = Math.min(inext, measureLimit);
            ReplacementSpan replacement = null;
            int j = 0;
            while (true) {
                int j2 = j;
                if (j2 >= this.mMetricAffectingSpanSpanSet.numberOfSpans) {
                    break;
                }
                if (this.mMetricAffectingSpanSpanSet.spanStarts[j2] < this.mStart + mlimit && this.mMetricAffectingSpanSpanSet.spanEnds[j2] > this.mStart + i) {
                    MetricAffectingSpan span = this.mMetricAffectingSpanSpanSet.spans[j2];
                    if (span instanceof ReplacementSpan) {
                        replacement = (ReplacementSpan) span;
                    } else {
                        span.updateDrawState(wp3);
                    }
                }
                j = j2 + 1;
            }
            if (replacement != null) {
                x += handleReplacement(replacement, wp3, i, mlimit, runIsRtl, c, x, top, y, bottom, fmi, needWidth || mlimit < measureLimit);
            } else {
                int j3 = i;
                while (j3 < mlimit) {
                    int jnext = this.mCharacterStyleSpanSet.getNextTransition(this.mStart + j3, this.mStart + mlimit) - this.mStart;
                    wp3.set(this.mPaint);
                    for (int k = 0; k < this.mCharacterStyleSpanSet.numberOfSpans; k++) {
                        if (this.mCharacterStyleSpanSet.spanStarts[k] < this.mStart + jnext && this.mCharacterStyleSpanSet.spanEnds[k] > this.mStart + j3) {
                            this.mCharacterStyleSpanSet.spans[k].updateDrawState(wp3);
                        }
                    }
                    x += handleText(wp3, j3, jnext, i, inext, runIsRtl, c, x, top, y, bottom, fmi, needWidth || jnext < measureLimit);
                    j3 = jnext;
                }
            }
            i = inext;
        }
        return x - x;
    }

    private void drawTextRun(Canvas c, TextPaint wp, int start, int end, int contextStart, int contextEnd, boolean runIsRtl, float x, int y) {
        if (this.mCharsValid) {
            int count = end - start;
            int contextCount = contextEnd - contextStart;
            c.drawTextRun(this.mChars, start, count, contextStart, contextCount, x, y, runIsRtl, wp);
        } else {
            int delta = this.mStart;
            c.drawTextRun(this.mText, delta + start, delta + end, delta + contextStart, delta + contextEnd, x, y, runIsRtl, wp);
        }
    }

    float ascent(int pos) {
        if (this.mSpanned == null) {
            return this.mPaint.ascent();
        }
        int pos2 = pos + this.mStart;
        MetricAffectingSpan[] spans = (MetricAffectingSpan[]) this.mSpanned.getSpans(pos2, pos2 + 1, MetricAffectingSpan.class);
        if (spans.length == 0) {
            return this.mPaint.ascent();
        }
        TextPaint wp = this.mWorkPaint;
        wp.set(this.mPaint);
        for (MetricAffectingSpan span : spans) {
            span.updateMeasureState(wp);
        }
        return wp.ascent();
    }

    float nextTab(float h) {
        return this.mTabs != null ? this.mTabs.nextTab(h) : Layout.TabStops.nextDefaultStop(h, 20);
    }
}
