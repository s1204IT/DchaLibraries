package android.text;

import android.emoji.EmojiFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.text.TextUtils;
import android.text.method.TextKeyListener;
import android.text.style.AlignmentSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.LineBackgroundSpan;
import android.text.style.ParagraphStyle;
import android.text.style.ReplacementSpan;
import android.text.style.TabStopSpan;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.GrowingArrayUtils;
import java.util.Arrays;

public abstract class Layout {
    static final Directions DIRS_ALL_LEFT_TO_RIGHT;
    static final Directions DIRS_ALL_RIGHT_TO_LEFT;
    public static final int DIR_LEFT_TO_RIGHT = 1;
    static final int DIR_REQUEST_DEFAULT_LTR = 2;
    static final int DIR_REQUEST_DEFAULT_RTL = -2;
    static final int DIR_REQUEST_LTR = 1;
    static final int DIR_REQUEST_RTL = -1;
    public static final int DIR_RIGHT_TO_LEFT = -1;
    static final int MAX_EMOJI;
    static final int MIN_EMOJI;
    static final int RUN_LENGTH_MASK = 67108863;
    static final int RUN_LEVEL_MASK = 63;
    static final int RUN_LEVEL_SHIFT = 26;
    static final int RUN_RTL_FLAG = 67108864;
    private static final int TAB_INCREMENT = 20;
    private static final Rect sTempRect;
    private Alignment mAlignment;
    private SpanSet<LineBackgroundSpan> mLineBackgroundSpans;
    private TextPaint mPaint;
    private float mSpacingAdd;
    private float mSpacingMult;
    private boolean mSpannedText;
    private CharSequence mText;
    private TextDirectionHeuristic mTextDir;
    private int mWidth;
    TextPaint mWorkPaint;
    private static final ParagraphStyle[] NO_PARA_SPANS = (ParagraphStyle[]) ArrayUtils.emptyArray(ParagraphStyle.class);
    static final EmojiFactory EMOJI_FACTORY = EmojiFactory.newAvailableInstance();

    public enum Alignment {
        ALIGN_NORMAL,
        ALIGN_OPPOSITE,
        ALIGN_CENTER,
        ALIGN_LEFT,
        ALIGN_RIGHT
    }

    public abstract int getBottomPadding();

    public abstract int getEllipsisCount(int i);

    public abstract int getEllipsisStart(int i);

    public abstract boolean getLineContainsTab(int i);

    public abstract int getLineCount();

    public abstract int getLineDescent(int i);

    public abstract Directions getLineDirections(int i);

    public abstract int getLineStart(int i);

    public abstract int getLineTop(int i);

    public abstract int getParagraphDirection(int i);

    public abstract int getTopPadding();

    static {
        if (EMOJI_FACTORY != null) {
            MIN_EMOJI = EMOJI_FACTORY.getMinimumAndroidPua();
            MAX_EMOJI = EMOJI_FACTORY.getMaximumAndroidPua();
        } else {
            MIN_EMOJI = -1;
            MAX_EMOJI = -1;
        }
        sTempRect = new Rect();
        DIRS_ALL_LEFT_TO_RIGHT = new Directions(new int[]{0, RUN_LENGTH_MASK});
        DIRS_ALL_RIGHT_TO_LEFT = new Directions(new int[]{0, 134217727});
    }

    public static float getDesiredWidth(CharSequence source, TextPaint paint) {
        return getDesiredWidth(source, 0, source.length(), paint);
    }

    public static float getDesiredWidth(CharSequence source, int start, int end, TextPaint paint) {
        float need = 0.0f;
        int i = start;
        while (i <= end) {
            int next = TextUtils.indexOf(source, '\n', i, end);
            if (next < 0) {
                next = end;
            }
            float w = measurePara(paint, source, i, next);
            if (w > need) {
                need = w;
            }
            i = next + 1;
        }
        return need;
    }

    protected Layout(CharSequence text, TextPaint paint, int width, Alignment align, float spacingMult, float spacingAdd) {
        this(text, paint, width, align, TextDirectionHeuristics.FIRSTSTRONG_LTR, spacingMult, spacingAdd);
    }

    protected Layout(CharSequence text, TextPaint paint, int width, Alignment align, TextDirectionHeuristic textDir, float spacingMult, float spacingAdd) {
        this.mAlignment = Alignment.ALIGN_NORMAL;
        if (width < 0) {
            throw new IllegalArgumentException("Layout: " + width + " < 0");
        }
        if (paint != null) {
            paint.bgColor = 0;
            paint.baselineShift = 0;
        }
        this.mText = text;
        this.mPaint = paint;
        this.mWorkPaint = new TextPaint();
        this.mWidth = width;
        this.mAlignment = align;
        this.mSpacingMult = spacingMult;
        this.mSpacingAdd = spacingAdd;
        this.mSpannedText = text instanceof Spanned;
        this.mTextDir = textDir;
    }

    void replaceWith(CharSequence text, TextPaint paint, int width, Alignment align, float spacingmult, float spacingadd) {
        if (width < 0) {
            throw new IllegalArgumentException("Layout: " + width + " < 0");
        }
        this.mText = text;
        this.mPaint = paint;
        this.mWidth = width;
        this.mAlignment = align;
        this.mSpacingMult = spacingmult;
        this.mSpacingAdd = spacingadd;
        this.mSpannedText = text instanceof Spanned;
    }

    public void draw(Canvas c) {
        draw(c, null, null, 0);
    }

    public void draw(Canvas canvas, Path highlight, Paint highlightPaint, int cursorOffsetVertical) {
        long lineRange = getLineRangeForDraw(canvas);
        int firstLine = TextUtils.unpackRangeStartFromLong(lineRange);
        int lastLine = TextUtils.unpackRangeEndFromLong(lineRange);
        if (lastLine >= 0) {
            drawBackground(canvas, highlight, highlightPaint, cursorOffsetVertical, firstLine, lastLine);
            drawText(canvas, firstLine, lastLine);
        }
    }

    public void drawText(Canvas canvas, int firstLine, int lastLine) {
        int x;
        int previousLineBottom = getLineTop(firstLine);
        int previousLineEnd = getLineStart(firstLine);
        ParagraphStyle[] spans = NO_PARA_SPANS;
        int spanEnd = 0;
        TextPaint paint = this.mPaint;
        CharSequence buf = this.mText;
        Alignment paraAlign = this.mAlignment;
        TabStops tabStops = null;
        boolean tabStopsIsInitialized = false;
        TextLine tl = TextLine.obtain();
        int i = firstLine;
        while (true) {
            TabStops tabStops2 = tabStops;
            if (i <= lastLine) {
                int start = previousLineEnd;
                previousLineEnd = getLineStart(i + 1);
                int end = getLineVisibleEnd(i, start, previousLineEnd);
                int ltop = previousLineBottom;
                int lbottom = getLineTop(i + 1);
                previousLineBottom = lbottom;
                int lbaseline = lbottom - getLineDescent(i);
                int dir = getParagraphDirection(i);
                int left = 0;
                int right = this.mWidth;
                if (this.mSpannedText) {
                    Spanned sp = (Spanned) buf;
                    int textLength = buf.length();
                    boolean isFirstParaLine = start == 0 || buf.charAt(start + (-1)) == '\n';
                    if (start >= spanEnd && (i == firstLine || isFirstParaLine)) {
                        spanEnd = sp.nextSpanTransition(start, textLength, ParagraphStyle.class);
                        spans = (ParagraphStyle[]) getParagraphSpans(sp, start, spanEnd, ParagraphStyle.class);
                        paraAlign = this.mAlignment;
                        int n = spans.length - 1;
                        while (true) {
                            if (n < 0) {
                                break;
                            }
                            if (spans[n] instanceof AlignmentSpan) {
                                paraAlign = ((AlignmentSpan) spans[n]).getAlignment();
                                break;
                            }
                            n--;
                        }
                        tabStopsIsInitialized = false;
                    }
                    int length = spans.length;
                    boolean useFirstLineMargin = isFirstParaLine;
                    int n2 = 0;
                    while (true) {
                        if (n2 >= length) {
                            break;
                        }
                        if (spans[n2] instanceof LeadingMarginSpan.LeadingMarginSpan2) {
                            int count = ((LeadingMarginSpan.LeadingMarginSpan2) spans[n2]).getLeadingMarginLineCount();
                            int startLine = getLineForOffset(sp.getSpanStart(spans[n2]));
                            if (i < startLine + count) {
                                useFirstLineMargin = true;
                                break;
                            }
                        }
                        n2++;
                    }
                    for (int n3 = 0; n3 < length; n3++) {
                        if (spans[n3] instanceof LeadingMarginSpan) {
                            LeadingMarginSpan margin = (LeadingMarginSpan) spans[n3];
                            if (dir == -1) {
                                margin.drawLeadingMargin(canvas, paint, right, dir, ltop, lbaseline, lbottom, buf, start, end, isFirstParaLine, this);
                                right -= margin.getLeadingMargin(useFirstLineMargin);
                            } else {
                                margin.drawLeadingMargin(canvas, paint, left, dir, ltop, lbaseline, lbottom, buf, start, end, isFirstParaLine, this);
                                left += margin.getLeadingMargin(useFirstLineMargin);
                            }
                        }
                    }
                }
                boolean hasTabOrEmoji = getLineContainsTab(i);
                if (!hasTabOrEmoji || tabStopsIsInitialized) {
                    tabStops = tabStops2;
                } else {
                    if (tabStops2 == null) {
                        tabStops = new TabStops(20, spans);
                    } else {
                        tabStops2.reset(20, spans);
                        tabStops = tabStops2;
                    }
                    tabStopsIsInitialized = true;
                }
                Alignment align = paraAlign;
                if (align == Alignment.ALIGN_LEFT) {
                    align = dir == 1 ? Alignment.ALIGN_NORMAL : Alignment.ALIGN_OPPOSITE;
                } else if (align == Alignment.ALIGN_RIGHT) {
                    align = dir == 1 ? Alignment.ALIGN_OPPOSITE : Alignment.ALIGN_NORMAL;
                }
                if (align == Alignment.ALIGN_NORMAL) {
                    if (dir == 1) {
                        x = left;
                    } else {
                        x = right;
                    }
                } else {
                    int max = (int) getLineExtent(i, tabStops, false);
                    if (align == Alignment.ALIGN_OPPOSITE) {
                        if (dir == 1) {
                            x = right - max;
                        } else {
                            x = left - max;
                        }
                    } else {
                        x = ((right + left) - (max & (-2))) >> 1;
                    }
                }
                Directions directions = getLineDirections(i);
                if (directions == DIRS_ALL_LEFT_TO_RIGHT && !this.mSpannedText && !hasTabOrEmoji) {
                    canvas.drawText(buf, start, end, x, lbaseline, paint);
                } else {
                    tl.set(paint, buf, start, end, dir, directions, hasTabOrEmoji, tabStops);
                    tl.draw(canvas, x, ltop, lbaseline, lbottom);
                }
                i++;
            } else {
                TextLine.recycle(tl);
                return;
            }
        }
    }

    public void drawBackground(Canvas canvas, Path highlight, Paint highlightPaint, int cursorOffsetVertical, int firstLine, int lastLine) {
        if (this.mSpannedText) {
            if (this.mLineBackgroundSpans == null) {
                this.mLineBackgroundSpans = new SpanSet<>(LineBackgroundSpan.class);
            }
            Spanned buffer = (Spanned) this.mText;
            int textLength = buffer.length();
            this.mLineBackgroundSpans.init(buffer, 0, textLength);
            if (this.mLineBackgroundSpans.numberOfSpans > 0) {
                int previousLineBottom = getLineTop(firstLine);
                int previousLineEnd = getLineStart(firstLine);
                ParagraphStyle[] spans = NO_PARA_SPANS;
                int spansLength = 0;
                TextPaint paint = this.mPaint;
                int spanEnd = 0;
                int width = this.mWidth;
                for (int i = firstLine; i <= lastLine; i++) {
                    int start = previousLineEnd;
                    int end = getLineStart(i + 1);
                    previousLineEnd = end;
                    int ltop = previousLineBottom;
                    int lbottom = getLineTop(i + 1);
                    previousLineBottom = lbottom;
                    int lbaseline = lbottom - getLineDescent(i);
                    if (start >= spanEnd) {
                        spanEnd = this.mLineBackgroundSpans.getNextTransition(start, textLength);
                        spansLength = 0;
                        if (start != end || start == 0) {
                            for (int j = 0; j < this.mLineBackgroundSpans.numberOfSpans; j++) {
                                if (this.mLineBackgroundSpans.spanStarts[j] < end && this.mLineBackgroundSpans.spanEnds[j] > start) {
                                    spans = (ParagraphStyle[]) GrowingArrayUtils.append((LineBackgroundSpan[]) spans, spansLength, this.mLineBackgroundSpans.spans[j]);
                                    spansLength++;
                                }
                            }
                        }
                    }
                    for (int n = 0; n < spansLength; n++) {
                        LineBackgroundSpan lineBackgroundSpan = (LineBackgroundSpan) spans[n];
                        lineBackgroundSpan.drawBackground(canvas, paint, 0, width, ltop, lbaseline, lbottom, buffer, start, end, i);
                    }
                }
            }
            this.mLineBackgroundSpans.recycle();
        }
        if (highlight != null) {
            if (cursorOffsetVertical != 0) {
                canvas.translate(0.0f, cursorOffsetVertical);
            }
            canvas.drawPath(highlight, highlightPaint);
            if (cursorOffsetVertical != 0) {
                canvas.translate(0.0f, -cursorOffsetVertical);
            }
        }
    }

    public long getLineRangeForDraw(Canvas canvas) {
        synchronized (sTempRect) {
            if (!canvas.getClipBounds(sTempRect)) {
                return TextUtils.packRangeInLong(0, -1);
            }
            int dtop = sTempRect.top;
            int dbottom = sTempRect.bottom;
            int top = Math.max(dtop, 0);
            int bottom = Math.min(getLineTop(getLineCount()), dbottom);
            return top >= bottom ? TextUtils.packRangeInLong(0, -1) : TextUtils.packRangeInLong(getLineForVertical(top), getLineForVertical(bottom));
        }
    }

    private int getLineStartPos(int line, int left, int right) {
        Alignment align = getParagraphAlignment(line);
        int dir = getParagraphDirection(line);
        if (align == Alignment.ALIGN_LEFT) {
            align = dir == 1 ? Alignment.ALIGN_NORMAL : Alignment.ALIGN_OPPOSITE;
        } else if (align == Alignment.ALIGN_RIGHT) {
            align = dir == 1 ? Alignment.ALIGN_OPPOSITE : Alignment.ALIGN_NORMAL;
        }
        if (align == Alignment.ALIGN_NORMAL) {
            if (dir == 1) {
                return left;
            }
            return right;
        }
        TabStops tabStops = null;
        if (this.mSpannedText && getLineContainsTab(line)) {
            Spanned spanned = (Spanned) this.mText;
            int start = getLineStart(line);
            int spanEnd = spanned.nextSpanTransition(start, spanned.length(), TabStopSpan.class);
            TabStopSpan[] tabSpans = (TabStopSpan[]) getParagraphSpans(spanned, start, spanEnd, TabStopSpan.class);
            if (tabSpans.length > 0) {
                tabStops = new TabStops(20, tabSpans);
            }
        }
        int max = (int) getLineExtent(line, tabStops, false);
        if (align == Alignment.ALIGN_OPPOSITE) {
            if (dir == 1) {
                int x = right - max;
                return x;
            }
            int x2 = left - max;
            return x2;
        }
        int x3 = ((left + right) - (max & (-2))) >> 1;
        return x3;
    }

    public final CharSequence getText() {
        return this.mText;
    }

    public final TextPaint getPaint() {
        return this.mPaint;
    }

    public final int getWidth() {
        return this.mWidth;
    }

    public int getEllipsizedWidth() {
        return this.mWidth;
    }

    public final void increaseWidthTo(int wid) {
        if (wid < this.mWidth) {
            throw new RuntimeException("attempted to reduce Layout width");
        }
        this.mWidth = wid;
    }

    public int getHeight() {
        return getLineTop(getLineCount());
    }

    public final Alignment getAlignment() {
        return this.mAlignment;
    }

    public final float getSpacingMultiplier() {
        return this.mSpacingMult;
    }

    public final float getSpacingAdd() {
        return this.mSpacingAdd;
    }

    public final TextDirectionHeuristic getTextDirectionHeuristic() {
        return this.mTextDir;
    }

    public int getLineBounds(int line, Rect bounds) {
        if (bounds != null) {
            bounds.left = 0;
            bounds.top = getLineTop(line);
            bounds.right = this.mWidth;
            bounds.bottom = getLineTop(line + 1);
        }
        return getLineBaseline(line);
    }

    public boolean isLevelBoundary(int offset) {
        int line = getLineForOffset(offset);
        Directions dirs = getLineDirections(line);
        if (dirs == DIRS_ALL_LEFT_TO_RIGHT || dirs == DIRS_ALL_RIGHT_TO_LEFT) {
            return false;
        }
        int[] runs = dirs.mDirections;
        int lineStart = getLineStart(line);
        int lineEnd = getLineEnd(line);
        if (offset == lineStart || offset == lineEnd) {
            int paraLevel = getParagraphDirection(line) == 1 ? 0 : 1;
            int runIndex = offset == lineStart ? 0 : runs.length - 2;
            return ((runs[runIndex + 1] >>> 26) & 63) != paraLevel;
        }
        int offset2 = offset - lineStart;
        for (int i = 0; i < runs.length; i += 2) {
            if (offset2 == runs[i]) {
                return true;
            }
        }
        return false;
    }

    public boolean isRtlCharAt(int offset) {
        int line = getLineForOffset(offset);
        Directions dirs = getLineDirections(line);
        if (dirs == DIRS_ALL_LEFT_TO_RIGHT) {
            return false;
        }
        if (dirs == DIRS_ALL_RIGHT_TO_LEFT) {
            return true;
        }
        int[] runs = dirs.mDirections;
        int lineStart = getLineStart(line);
        for (int i = 0; i < runs.length; i += 2) {
            int start = lineStart + runs[i];
            int limit = start + (runs[i + 1] & RUN_LENGTH_MASK);
            if (offset >= start && offset < limit) {
                int level = (runs[i + 1] >>> 26) & 63;
                return (level & 1) != 0;
            }
        }
        return false;
    }

    private boolean primaryIsTrailingPrevious(int offset) {
        int line = getLineForOffset(offset);
        int lineStart = getLineStart(line);
        int lineEnd = getLineEnd(line);
        int[] runs = getLineDirections(line).mDirections;
        int levelAt = -1;
        int i = 0;
        while (true) {
            if (i >= runs.length) {
                break;
            }
            int start = lineStart + runs[i];
            int limit = start + (runs[i + 1] & RUN_LENGTH_MASK);
            if (limit > lineEnd) {
                limit = lineEnd;
            }
            if (offset < start || offset >= limit) {
                i += 2;
            } else {
                if (offset > start) {
                    return false;
                }
                levelAt = (runs[i + 1] >>> 26) & 63;
            }
        }
        if (levelAt == -1) {
            levelAt = getParagraphDirection(line) == 1 ? 0 : 1;
        }
        int levelBefore = -1;
        if (offset == lineStart) {
            levelBefore = getParagraphDirection(line) == 1 ? 0 : 1;
        } else {
            int offset2 = offset - 1;
            int i2 = 0;
            while (true) {
                if (i2 < runs.length) {
                    int start2 = lineStart + runs[i2];
                    int limit2 = start2 + (runs[i2 + 1] & RUN_LENGTH_MASK);
                    if (limit2 > lineEnd) {
                        limit2 = lineEnd;
                    }
                    if (offset2 < start2 || offset2 >= limit2) {
                        i2 += 2;
                    } else {
                        levelBefore = (runs[i2 + 1] >>> 26) & 63;
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        return levelBefore < levelAt;
    }

    public float getPrimaryHorizontal(int offset) {
        return getPrimaryHorizontal(offset, false);
    }

    public float getPrimaryHorizontal(int offset, boolean clamped) {
        boolean trailing = primaryIsTrailingPrevious(offset);
        return getHorizontal(offset, trailing, clamped);
    }

    public float getSecondaryHorizontal(int offset) {
        return getSecondaryHorizontal(offset, false);
    }

    public float getSecondaryHorizontal(int offset, boolean clamped) {
        boolean trailing = primaryIsTrailingPrevious(offset);
        return getHorizontal(offset, !trailing, clamped);
    }

    private float getHorizontal(int offset, boolean trailing, boolean clamped) {
        int line = getLineForOffset(offset);
        return getHorizontal(offset, trailing, line, clamped);
    }

    private float getHorizontal(int offset, boolean trailing, int line, boolean clamped) {
        int start = getLineStart(line);
        int end = getLineEnd(line);
        int dir = getParagraphDirection(line);
        boolean hasTabOrEmoji = getLineContainsTab(line);
        Directions directions = getLineDirections(line);
        TabStops tabStops = null;
        if (hasTabOrEmoji && (this.mText instanceof Spanned)) {
            TabStopSpan[] tabs = (TabStopSpan[]) getParagraphSpans((Spanned) this.mText, start, end, TabStopSpan.class);
            if (tabs.length > 0) {
                tabStops = new TabStops(20, tabs);
            }
        }
        TextLine tl = TextLine.obtain();
        tl.set(this.mPaint, this.mText, start, end, dir, directions, hasTabOrEmoji, tabStops);
        float wid = tl.measure(offset - start, trailing, null);
        TextLine.recycle(tl);
        if (clamped && wid > this.mWidth) {
            wid = this.mWidth;
        }
        int left = getParagraphLeft(line);
        int right = getParagraphRight(line);
        return getLineStartPos(line, left, right) + wid;
    }

    public float getLineLeft(int line) {
        int dir = getParagraphDirection(line);
        Alignment align = getParagraphAlignment(line);
        if (align == Alignment.ALIGN_LEFT) {
            return 0.0f;
        }
        if (align == Alignment.ALIGN_NORMAL) {
            if (dir == -1) {
                return getParagraphRight(line) - getLineMax(line);
            }
            return 0.0f;
        }
        if (align == Alignment.ALIGN_RIGHT) {
            return this.mWidth - getLineMax(line);
        }
        if (align == Alignment.ALIGN_OPPOSITE) {
            if (dir != -1) {
                return this.mWidth - getLineMax(line);
            }
            return 0.0f;
        }
        int left = getParagraphLeft(line);
        int right = getParagraphRight(line);
        int max = ((int) getLineMax(line)) & (-2);
        return (((right - left) - max) / 2) + left;
    }

    public float getLineRight(int line) {
        int dir = getParagraphDirection(line);
        Alignment align = getParagraphAlignment(line);
        if (align == Alignment.ALIGN_LEFT) {
            return getParagraphLeft(line) + getLineMax(line);
        }
        if (align == Alignment.ALIGN_NORMAL) {
            if (dir == -1) {
                return this.mWidth;
            }
            return getParagraphLeft(line) + getLineMax(line);
        }
        if (align == Alignment.ALIGN_RIGHT) {
            return this.mWidth;
        }
        if (align == Alignment.ALIGN_OPPOSITE) {
            if (dir == -1) {
                return getLineMax(line);
            }
            return this.mWidth;
        }
        int left = getParagraphLeft(line);
        int right = getParagraphRight(line);
        int max = ((int) getLineMax(line)) & (-2);
        return right - (((right - left) - max) / 2);
    }

    public float getLineMax(int line) {
        float margin = getParagraphLeadingMargin(line);
        float signedExtent = getLineExtent(line, false);
        if (signedExtent < 0.0f) {
            signedExtent = -signedExtent;
        }
        return margin + signedExtent;
    }

    public float getLineWidth(int line) {
        float margin = getParagraphLeadingMargin(line);
        float signedExtent = getLineExtent(line, true);
        if (signedExtent < 0.0f) {
            signedExtent = -signedExtent;
        }
        return margin + signedExtent;
    }

    private float getLineExtent(int line, boolean full) {
        int start = getLineStart(line);
        int end = full ? getLineEnd(line) : getLineVisibleEnd(line);
        boolean hasTabsOrEmoji = getLineContainsTab(line);
        TabStops tabStops = null;
        if (hasTabsOrEmoji && (this.mText instanceof Spanned)) {
            TabStopSpan[] tabs = (TabStopSpan[]) getParagraphSpans((Spanned) this.mText, start, end, TabStopSpan.class);
            if (tabs.length > 0) {
                tabStops = new TabStops(20, tabs);
            }
        }
        Directions directions = getLineDirections(line);
        if (directions == null) {
            return 0.0f;
        }
        int dir = getParagraphDirection(line);
        TextLine tl = TextLine.obtain();
        tl.set(this.mPaint, this.mText, start, end, dir, directions, hasTabsOrEmoji, tabStops);
        float fMetrics = tl.metrics(null);
        TextLine.recycle(tl);
        return fMetrics;
    }

    private float getLineExtent(int line, TabStops tabStops, boolean full) {
        int start = getLineStart(line);
        int end = full ? getLineEnd(line) : getLineVisibleEnd(line);
        boolean hasTabsOrEmoji = getLineContainsTab(line);
        Directions directions = getLineDirections(line);
        int dir = getParagraphDirection(line);
        TextLine tl = TextLine.obtain();
        tl.set(this.mPaint, this.mText, start, end, dir, directions, hasTabsOrEmoji, tabStops);
        float width = tl.metrics(null);
        TextLine.recycle(tl);
        return width;
    }

    public int getLineForVertical(int vertical) {
        int high = getLineCount();
        int low = -1;
        while (high - low > 1) {
            int guess = (high + low) / 2;
            if (getLineTop(guess) > vertical) {
                high = guess;
            } else {
                low = guess;
            }
        }
        if (low < 0) {
            return 0;
        }
        return low;
    }

    public int getLineForOffset(int offset) {
        int high = getLineCount();
        int low = -1;
        while (high - low > 1) {
            int guess = (high + low) / 2;
            if (getLineStart(guess) > offset) {
                high = guess;
            } else {
                low = guess;
            }
        }
        if (low < 0) {
            return 0;
        }
        return low;
    }

    public int getOffsetForHorizontal(int line, float horiz) {
        int max = getLineEnd(line) - 1;
        int min = getLineStart(line);
        Directions dirs = getLineDirections(line);
        if (line == getLineCount() - 1) {
            max++;
        }
        int best = min;
        float bestdist = Math.abs(getPrimaryHorizontal(best) - horiz);
        for (int i = 0; i < dirs.mDirections.length; i += 2) {
            int here = min + dirs.mDirections[i];
            int there = here + (dirs.mDirections[i + 1] & RUN_LENGTH_MASK);
            int swap = (dirs.mDirections[i + 1] & 67108864) != 0 ? -1 : 1;
            if (there > max) {
                there = max;
            }
            int high = (there - 1) + 1;
            int low = (here + 1) - 1;
            while (high - low > 1) {
                int guess = (high + low) / 2;
                int adguess = getOffsetAtStartOf(guess);
                if (getPrimaryHorizontal(adguess) * swap >= swap * horiz) {
                    high = guess;
                } else {
                    low = guess;
                }
            }
            if (low < here + 1) {
                low = here + 1;
            }
            if (low < there) {
                int low2 = getOffsetAtStartOf(low);
                float dist = Math.abs(getPrimaryHorizontal(low2) - horiz);
                int aft = TextUtils.getOffsetAfter(this.mText, low2);
                if (aft < there) {
                    float other = Math.abs(getPrimaryHorizontal(aft) - horiz);
                    if (other < dist) {
                        dist = other;
                        low2 = aft;
                    }
                }
                if (dist < bestdist) {
                    bestdist = dist;
                    best = low2;
                }
            }
            float dist2 = Math.abs(getPrimaryHorizontal(here) - horiz);
            if (dist2 < bestdist) {
                bestdist = dist2;
                best = here;
            }
        }
        if (Math.abs(getPrimaryHorizontal(max) - horiz) <= bestdist) {
            return max;
        }
        return best;
    }

    public final int getLineEnd(int line) {
        return getLineStart(line + 1);
    }

    public int getLineVisibleEnd(int line) {
        return getLineVisibleEnd(line, getLineStart(line), getLineStart(line + 1));
    }

    private int getLineVisibleEnd(int line, int start, int end) {
        CharSequence text = this.mText;
        if (line == getLineCount() - 1) {
            return end;
        }
        while (end > start) {
            char ch = text.charAt(end - 1);
            if (ch == '\n') {
                return end - 1;
            }
            if (ch != ' ' && ch != '\t') {
                break;
            }
            end--;
        }
        return end;
    }

    public final int getLineBottom(int line) {
        return getLineTop(line + 1);
    }

    public final int getLineBaseline(int line) {
        return getLineTop(line + 1) - getLineDescent(line);
    }

    public final int getLineAscent(int line) {
        return getLineTop(line) - (getLineTop(line + 1) - getLineDescent(line));
    }

    public int getOffsetToLeftOf(int offset) {
        return getOffsetToLeftRightOf(offset, true);
    }

    public int getOffsetToRightOf(int offset) {
        return getOffsetToLeftRightOf(offset, false);
    }

    private int getOffsetToLeftRightOf(int caret, boolean toLeft) {
        int line = getLineForOffset(caret);
        int lineStart = getLineStart(line);
        int lineEnd = getLineEnd(line);
        int lineDir = getParagraphDirection(line);
        boolean lineChanged = false;
        boolean advance = toLeft == (lineDir == -1);
        if (advance) {
            if (caret == lineEnd) {
                if (line >= getLineCount() - 1) {
                    return caret;
                }
                lineChanged = true;
                line++;
            }
        } else if (caret == lineStart) {
            if (line <= 0) {
                return caret;
            }
            lineChanged = true;
            line--;
        }
        if (lineChanged) {
            lineStart = getLineStart(line);
            lineEnd = getLineEnd(line);
            int newDir = getParagraphDirection(line);
            if (newDir != lineDir) {
                toLeft = !toLeft;
                lineDir = newDir;
            }
        }
        Directions directions = getLineDirections(line);
        TextLine tl = TextLine.obtain();
        tl.set(this.mPaint, this.mText, lineStart, lineEnd, lineDir, directions, false, null);
        int caret2 = lineStart + tl.getOffsetToLeftRightOf(caret - lineStart, toLeft);
        TextLine.recycle(tl);
        return caret2;
    }

    private int getOffsetAtStartOf(int offset) {
        char c1;
        if (offset == 0) {
            return 0;
        }
        CharSequence text = this.mText;
        char c = text.charAt(offset);
        if (c >= 56320 && c <= 57343 && (c1 = text.charAt(offset - 1)) >= 55296 && c1 <= 56319) {
            offset--;
        }
        if (this.mSpannedText) {
            ReplacementSpan[] spans = (ReplacementSpan[]) ((Spanned) text).getSpans(offset, offset, ReplacementSpan.class);
            for (int i = 0; i < spans.length; i++) {
                int start = ((Spanned) text).getSpanStart(spans[i]);
                int end = ((Spanned) text).getSpanEnd(spans[i]);
                if (start < offset && end > offset) {
                    offset = start;
                }
            }
        }
        return offset;
    }

    public boolean shouldClampCursor(int line) {
        switch (getParagraphAlignment(line)) {
            case ALIGN_LEFT:
                return true;
            case ALIGN_NORMAL:
                return getParagraphDirection(line) > 0;
            default:
                return false;
        }
    }

    public void getCursorPath(int point, Path dest, CharSequence editingBuffer) {
        dest.reset();
        int line = getLineForOffset(point);
        int top = getLineTop(line);
        int bottom = getLineTop(line + 1);
        boolean clamped = shouldClampCursor(line);
        float h1 = getPrimaryHorizontal(point, clamped) - 0.5f;
        float h2 = isLevelBoundary(point) ? getSecondaryHorizontal(point, clamped) - 0.5f : h1;
        int caps = TextKeyListener.getMetaState(editingBuffer, 1) | TextKeyListener.getMetaState(editingBuffer, 2048);
        int fn = TextKeyListener.getMetaState(editingBuffer, 2);
        int dist = 0;
        if (caps != 0 || fn != 0) {
            dist = (bottom - top) >> 2;
            if (fn != 0) {
                top += dist;
            }
            if (caps != 0) {
                bottom -= dist;
            }
        }
        if (h1 < 0.5f) {
            h1 = 0.5f;
        }
        if (h2 < 0.5f) {
            h2 = 0.5f;
        }
        if (Float.compare(h1, h2) == 0) {
            dest.moveTo(h1, top);
            dest.lineTo(h1, bottom);
        } else {
            dest.moveTo(h1, top);
            dest.lineTo(h1, (top + bottom) >> 1);
            dest.moveTo(h2, (top + bottom) >> 1);
            dest.lineTo(h2, bottom);
        }
        if (caps == 2) {
            dest.moveTo(h2, bottom);
            dest.lineTo(h2 - dist, bottom + dist);
            dest.lineTo(h2, bottom);
            dest.lineTo(dist + h2, bottom + dist);
        } else if (caps == 1) {
            dest.moveTo(h2, bottom);
            dest.lineTo(h2 - dist, bottom + dist);
            dest.moveTo(h2 - dist, (bottom + dist) - 0.5f);
            dest.lineTo(dist + h2, (bottom + dist) - 0.5f);
            dest.moveTo(dist + h2, bottom + dist);
            dest.lineTo(h2, bottom);
        }
        if (fn == 2) {
            dest.moveTo(h1, top);
            dest.lineTo(h1 - dist, top - dist);
            dest.lineTo(h1, top);
            dest.lineTo(dist + h1, top - dist);
            return;
        }
        if (fn == 1) {
            dest.moveTo(h1, top);
            dest.lineTo(h1 - dist, top - dist);
            dest.moveTo(h1 - dist, (top - dist) + 0.5f);
            dest.lineTo(dist + h1, (top - dist) + 0.5f);
            dest.moveTo(dist + h1, top - dist);
            dest.lineTo(h1, top);
        }
    }

    private void addSelection(int line, int start, int end, int top, int bottom, Path dest) {
        int st;
        int en;
        int linestart = getLineStart(line);
        int lineend = getLineEnd(line);
        Directions dirs = getLineDirections(line);
        if (lineend > linestart && this.mText.charAt(lineend - 1) == '\n') {
            lineend--;
        }
        for (int i = 0; i < dirs.mDirections.length; i += 2) {
            int here = linestart + dirs.mDirections[i];
            int there = here + (dirs.mDirections[i + 1] & RUN_LENGTH_MASK);
            if (there > lineend) {
                there = lineend;
            }
            if (start <= there && end >= here && (st = Math.max(start, here)) != (en = Math.min(end, there))) {
                float h1 = getHorizontal(st, false, line, false);
                float h2 = getHorizontal(en, true, line, false);
                float left = Math.min(h1, h2);
                float right = Math.max(h1, h2);
                dest.addRect(left, top, right, bottom, Path.Direction.CW);
            }
        }
    }

    public void getSelectionPath(int start, int end, Path dest) {
        dest.reset();
        if (start != end) {
            if (end < start) {
                end = start;
                start = end;
            }
            int startline = getLineForOffset(start);
            int endline = getLineForOffset(end);
            int top = getLineTop(startline);
            int bottom = getLineBottom(endline);
            if (startline == endline) {
                addSelection(startline, start, end, top, bottom, dest);
                return;
            }
            float width = this.mWidth;
            addSelection(startline, start, getLineEnd(startline), top, getLineBottom(startline), dest);
            if (getParagraphDirection(startline) == -1) {
                dest.addRect(getLineLeft(startline), top, 0.0f, getLineBottom(startline), Path.Direction.CW);
            } else {
                dest.addRect(getLineRight(startline), top, width, getLineBottom(startline), Path.Direction.CW);
            }
            for (int i = startline + 1; i < endline; i++) {
                int top2 = getLineTop(i);
                int bottom2 = getLineBottom(i);
                dest.addRect(0.0f, top2, width, bottom2, Path.Direction.CW);
            }
            int top3 = getLineTop(endline);
            int bottom3 = getLineBottom(endline);
            addSelection(endline, getLineStart(endline), end, top3, bottom3, dest);
            if (getParagraphDirection(endline) == -1) {
                dest.addRect(width, top3, getLineRight(endline), bottom3, Path.Direction.CW);
            } else {
                dest.addRect(0.0f, top3, getLineLeft(endline), bottom3, Path.Direction.CW);
            }
        }
    }

    public final Alignment getParagraphAlignment(int line) {
        Alignment align = this.mAlignment;
        if (this.mSpannedText) {
            Spanned sp = (Spanned) this.mText;
            AlignmentSpan[] spans = (AlignmentSpan[]) getParagraphSpans(sp, getLineStart(line), getLineEnd(line), AlignmentSpan.class);
            int spanLength = spans.length;
            if (spanLength > 0) {
                return spans[spanLength - 1].getAlignment();
            }
            return align;
        }
        return align;
    }

    public final int getParagraphLeft(int line) {
        int dir = getParagraphDirection(line);
        if (dir == -1 || !this.mSpannedText) {
            return 0;
        }
        int left = getParagraphLeadingMargin(line);
        return left;
    }

    public final int getParagraphRight(int line) {
        int right = this.mWidth;
        int dir = getParagraphDirection(line);
        return (dir == 1 || !this.mSpannedText) ? right : right - getParagraphLeadingMargin(line);
    }

    private int getParagraphLeadingMargin(int line) {
        if (!this.mSpannedText) {
            return 0;
        }
        Spanned spanned = (Spanned) this.mText;
        int lineStart = getLineStart(line);
        int lineEnd = getLineEnd(line);
        int spanEnd = spanned.nextSpanTransition(lineStart, lineEnd, LeadingMarginSpan.class);
        LeadingMarginSpan[] spans = (LeadingMarginSpan[]) getParagraphSpans(spanned, lineStart, spanEnd, LeadingMarginSpan.class);
        if (spans.length == 0) {
            return 0;
        }
        int margin = 0;
        boolean isFirstParaLine = lineStart == 0 || spanned.charAt(lineStart + (-1)) == '\n';
        boolean useFirstLineMargin = isFirstParaLine;
        for (int i = 0; i < spans.length; i++) {
            if (spans[i] instanceof LeadingMarginSpan.LeadingMarginSpan2) {
                int spStart = spanned.getSpanStart(spans[i]);
                int spanLine = getLineForOffset(spStart);
                int count = ((LeadingMarginSpan.LeadingMarginSpan2) spans[i]).getLeadingMarginLineCount();
                useFirstLineMargin |= line < spanLine + count;
            }
        }
        for (LeadingMarginSpan span : spans) {
            margin += span.getLeadingMargin(useFirstLineMargin);
        }
        return margin;
    }

    static float measurePara(TextPaint paint, CharSequence text, int start, int end) {
        Directions directions;
        int dir;
        MeasuredText mt = MeasuredText.obtain();
        TextLine tl = TextLine.obtain();
        try {
            mt.setPara(text, start, end, TextDirectionHeuristics.LTR);
            if (mt.mEasy) {
                directions = DIRS_ALL_LEFT_TO_RIGHT;
                dir = 1;
            } else {
                directions = AndroidBidi.directions(mt.mDir, mt.mLevels, 0, mt.mChars, 0, mt.mLen);
                dir = mt.mDir;
            }
            char[] chars = mt.mChars;
            int len = mt.mLen;
            boolean hasTabs = false;
            TabStops tabStops = null;
            int margin = 0;
            if (text instanceof Spanned) {
                LeadingMarginSpan[] spans = (LeadingMarginSpan[]) getParagraphSpans((Spanned) text, start, end, LeadingMarginSpan.class);
                for (LeadingMarginSpan lms : spans) {
                    margin += lms.getLeadingMargin(true);
                }
            }
            int i = 0;
            while (true) {
                if (i >= len) {
                    break;
                }
                if (chars[i] != '\t') {
                    i++;
                } else {
                    hasTabs = true;
                    if (text instanceof Spanned) {
                        Spanned spanned = (Spanned) text;
                        int spanEnd = spanned.nextSpanTransition(start, end, TabStopSpan.class);
                        TabStopSpan[] spans2 = (TabStopSpan[]) getParagraphSpans(spanned, start, spanEnd, TabStopSpan.class);
                        if (spans2.length > 0) {
                            tabStops = new TabStops(20, spans2);
                        }
                    }
                }
            }
            tl.set(paint, text, start, end, dir, directions, hasTabs, tabStops);
            return margin + tl.metrics(null);
        } finally {
            TextLine.recycle(tl);
            MeasuredText.recycle(mt);
        }
    }

    static class TabStops {
        private int mIncrement;
        private int mNumStops;
        private int[] mStops;

        TabStops(int increment, Object[] spans) {
            reset(increment, spans);
        }

        void reset(int increment, Object[] spans) {
            int ns;
            this.mIncrement = increment;
            int ns2 = 0;
            if (spans != null) {
                int[] stops = this.mStops;
                int len$ = spans.length;
                int i$ = 0;
                int ns3 = 0;
                while (i$ < len$) {
                    Object o = spans[i$];
                    if (o instanceof TabStopSpan) {
                        if (stops == null) {
                            stops = new int[10];
                        } else if (ns3 == stops.length) {
                            int[] nstops = new int[ns3 * 2];
                            for (int i = 0; i < ns3; i++) {
                                nstops[i] = stops[i];
                            }
                            stops = nstops;
                        }
                        ns = ns3 + 1;
                        stops[ns3] = ((TabStopSpan) o).getTabStop();
                    } else {
                        ns = ns3;
                    }
                    i$++;
                    ns3 = ns;
                }
                if (ns3 > 1) {
                    Arrays.sort(stops, 0, ns3);
                }
                if (stops != this.mStops) {
                    this.mStops = stops;
                }
                ns2 = ns3;
            }
            this.mNumStops = ns2;
        }

        float nextTab(float h) {
            int ns = this.mNumStops;
            if (ns > 0) {
                int[] stops = this.mStops;
                for (int i = 0; i < ns; i++) {
                    int stop = stops[i];
                    if (stop > h) {
                        return stop;
                    }
                }
            }
            return nextDefaultStop(h, this.mIncrement);
        }

        public static float nextDefaultStop(float h, int inc) {
            return ((int) ((inc + h) / inc)) * inc;
        }
    }

    static float nextTab(CharSequence text, int start, int end, float h, Object[] tabs) {
        float nh = Float.MAX_VALUE;
        boolean alltabs = false;
        if (text instanceof Spanned) {
            if (tabs == null) {
                tabs = getParagraphSpans((Spanned) text, start, end, TabStopSpan.class);
                alltabs = true;
            }
            for (int i = 0; i < tabs.length; i++) {
                if (alltabs || (tabs[i] instanceof TabStopSpan)) {
                    int where = ((TabStopSpan) tabs[i]).getTabStop();
                    if (where < nh && where > h) {
                        nh = where;
                    }
                }
            }
            if (nh != Float.MAX_VALUE) {
                return nh;
            }
        }
        return ((int) ((h + 20.0f) / 20.0f)) * 20;
    }

    protected final boolean isSpanned() {
        return this.mSpannedText;
    }

    static <T> T[] getParagraphSpans(Spanned spanned, int i, int i2, Class<T> cls) {
        return (i != i2 || i <= 0) ? (T[]) spanned.getSpans(i, i2, cls) : (T[]) ArrayUtils.emptyArray(cls);
    }

    private char getEllipsisChar(TextUtils.TruncateAt method) {
        return method == TextUtils.TruncateAt.END_SMALL ? TextUtils.ELLIPSIS_TWO_DOTS[0] : TextUtils.ELLIPSIS_NORMAL[0];
    }

    private void ellipsize(int start, int end, int line, char[] dest, int destoff, TextUtils.TruncateAt method) {
        char c;
        int ellipsisCount = getEllipsisCount(line);
        if (ellipsisCount != 0) {
            int ellipsisStart = getEllipsisStart(line);
            int linestart = getLineStart(line);
            for (int i = ellipsisStart; i < ellipsisStart + ellipsisCount; i++) {
                if (i == ellipsisStart) {
                    c = getEllipsisChar(method);
                } else {
                    c = 65279;
                }
                int a = i + linestart;
                if (a >= start && a < end) {
                    dest[(destoff + a) - start] = c;
                }
            }
        }
    }

    public static class Directions {
        int[] mDirections;

        Directions(int[] dirs) {
            this.mDirections = dirs;
        }
    }

    static class Ellipsizer implements CharSequence, GetChars {
        Layout mLayout;
        TextUtils.TruncateAt mMethod;
        CharSequence mText;
        int mWidth;

        public Ellipsizer(CharSequence s) {
            this.mText = s;
        }

        @Override
        public char charAt(int off) {
            char[] buf = TextUtils.obtain(1);
            getChars(off, off + 1, buf, 0);
            char ret = buf[0];
            TextUtils.recycle(buf);
            return ret;
        }

        @Override
        public void getChars(int start, int end, char[] dest, int destoff) {
            int line1 = this.mLayout.getLineForOffset(start);
            int line2 = this.mLayout.getLineForOffset(end);
            TextUtils.getChars(this.mText, start, end, dest, destoff);
            for (int i = line1; i <= line2; i++) {
                this.mLayout.ellipsize(start, end, i, dest, destoff, this.mMethod);
            }
        }

        @Override
        public int length() {
            return this.mText.length();
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            char[] s = new char[end - start];
            getChars(start, end, s, 0);
            return new String(s);
        }

        @Override
        public String toString() {
            char[] s = new char[length()];
            getChars(0, length(), s, 0);
            return new String(s);
        }
    }

    static class SpannedEllipsizer extends Ellipsizer implements Spanned {
        private Spanned mSpanned;

        public SpannedEllipsizer(CharSequence display) {
            super(display);
            this.mSpanned = (Spanned) display;
        }

        @Override
        public <T> T[] getSpans(int i, int i2, Class<T> cls) {
            return (T[]) this.mSpanned.getSpans(i, i2, cls);
        }

        @Override
        public int getSpanStart(Object tag) {
            return this.mSpanned.getSpanStart(tag);
        }

        @Override
        public int getSpanEnd(Object tag) {
            return this.mSpanned.getSpanEnd(tag);
        }

        @Override
        public int getSpanFlags(Object tag) {
            return this.mSpanned.getSpanFlags(tag);
        }

        @Override
        public int nextSpanTransition(int start, int limit, Class type) {
            return this.mSpanned.nextSpanTransition(start, limit, type);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            char[] s = new char[end - start];
            getChars(start, end, s, 0);
            SpannableString ss = new SpannableString(new String(s));
            TextUtils.copySpansFrom(this.mSpanned, start, end, Object.class, ss, 0);
            return ss;
        }
    }
}
