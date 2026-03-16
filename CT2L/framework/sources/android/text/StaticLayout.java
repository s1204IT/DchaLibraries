package android.text;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.text.Layout;
import android.text.TextUtils;
import android.text.style.LeadingMarginSpan;
import android.text.style.LineHeightSpan;
import android.text.style.MetricAffectingSpan;
import android.text.style.TabStopSpan;
import android.util.Log;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.GrowingArrayUtils;

public class StaticLayout extends Layout {
    private static final int CHAR_FIRST_HIGH_SURROGATE = 55296;
    private static final int CHAR_LAST_LOW_SURROGATE = 57343;
    private static final char CHAR_NEW_LINE = '\n';
    private static final char CHAR_SPACE = ' ';
    private static final char CHAR_TAB = '\t';
    private static final char CHAR_ZWSP = 8203;
    private static final int COLUMNS_ELLIPSIZE = 5;
    private static final int COLUMNS_NORMAL = 3;
    private static final int DESCENT = 2;
    private static final int DIR = 0;
    private static final int DIR_SHIFT = 30;
    private static final int ELLIPSIS_COUNT = 4;
    private static final int ELLIPSIS_START = 3;
    private static final double EXTRA_ROUNDING = 0.5d;
    private static final int START = 0;
    private static final int START_MASK = 536870911;
    private static final int TAB = 0;
    private static final int TAB_INCREMENT = 20;
    private static final int TAB_MASK = 536870912;
    static final String TAG = "StaticLayout";
    private static final int TOP = 1;
    private int mBottomPadding;
    private int mColumns;
    private int mEllipsizedWidth;
    private Paint.FontMetricsInt mFontMetricsInt;
    private int mLineCount;
    private Layout.Directions[] mLineDirections;
    private int[] mLines;
    private int mMaximumVisibleLineCount;
    private MeasuredText mMeasured;
    private int mTopPadding;

    private static native int[] nLineBreakOpportunities(String str, char[] cArr, int i, int[] iArr);

    public StaticLayout(CharSequence source, TextPaint paint, int width, Layout.Alignment align, float spacingmult, float spacingadd, boolean includepad) {
        this(source, 0, source.length(), paint, width, align, spacingmult, spacingadd, includepad);
    }

    public StaticLayout(CharSequence source, TextPaint paint, int width, Layout.Alignment align, TextDirectionHeuristic textDir, float spacingmult, float spacingadd, boolean includepad) {
        this(source, 0, source.length(), paint, width, align, textDir, spacingmult, spacingadd, includepad);
    }

    public StaticLayout(CharSequence source, int bufstart, int bufend, TextPaint paint, int outerwidth, Layout.Alignment align, float spacingmult, float spacingadd, boolean includepad) {
        this(source, bufstart, bufend, paint, outerwidth, align, spacingmult, spacingadd, includepad, null, 0);
    }

    public StaticLayout(CharSequence source, int bufstart, int bufend, TextPaint paint, int outerwidth, Layout.Alignment align, TextDirectionHeuristic textDir, float spacingmult, float spacingadd, boolean includepad) {
        this(source, bufstart, bufend, paint, outerwidth, align, textDir, spacingmult, spacingadd, includepad, null, 0, Integer.MAX_VALUE);
    }

    public StaticLayout(CharSequence source, int bufstart, int bufend, TextPaint paint, int outerwidth, Layout.Alignment align, float spacingmult, float spacingadd, boolean includepad, TextUtils.TruncateAt ellipsize, int ellipsizedWidth) {
        this(source, bufstart, bufend, paint, outerwidth, align, TextDirectionHeuristics.FIRSTSTRONG_LTR, spacingmult, spacingadd, includepad, ellipsize, ellipsizedWidth, Integer.MAX_VALUE);
    }

    public StaticLayout(CharSequence source, int bufstart, int bufend, TextPaint paint, int outerwidth, Layout.Alignment align, TextDirectionHeuristic textDir, float spacingmult, float spacingadd, boolean includepad, TextUtils.TruncateAt ellipsize, int ellipsizedWidth, int maxLines) {
        CharSequence spannedEllipsizer;
        if (ellipsize == null) {
            spannedEllipsizer = source;
        } else {
            spannedEllipsizer = source instanceof Spanned ? new Layout.SpannedEllipsizer(source) : new Layout.Ellipsizer(source);
        }
        super(spannedEllipsizer, paint, outerwidth, align, textDir, spacingmult, spacingadd);
        this.mMaximumVisibleLineCount = Integer.MAX_VALUE;
        this.mFontMetricsInt = new Paint.FontMetricsInt();
        if (ellipsize != null) {
            Layout.Ellipsizer e = (Layout.Ellipsizer) getText();
            e.mLayout = this;
            e.mWidth = ellipsizedWidth;
            e.mMethod = ellipsize;
            this.mEllipsizedWidth = ellipsizedWidth;
            this.mColumns = 5;
        } else {
            this.mColumns = 3;
            this.mEllipsizedWidth = outerwidth;
        }
        this.mLineDirections = (Layout.Directions[]) ArrayUtils.newUnpaddedArray(Layout.Directions.class, this.mColumns * 2);
        this.mLines = new int[this.mLineDirections.length];
        this.mMaximumVisibleLineCount = maxLines;
        this.mMeasured = MeasuredText.obtain();
        generate(source, bufstart, bufend, paint, outerwidth, textDir, spacingmult, spacingadd, includepad, includepad, ellipsizedWidth, ellipsize);
        this.mMeasured = MeasuredText.recycle(this.mMeasured);
        this.mFontMetricsInt = null;
    }

    StaticLayout(CharSequence text) {
        super(text, null, 0, null, 0.0f, 0.0f);
        this.mMaximumVisibleLineCount = Integer.MAX_VALUE;
        this.mFontMetricsInt = new Paint.FontMetricsInt();
        this.mColumns = 5;
        this.mLineDirections = (Layout.Directions[]) ArrayUtils.newUnpaddedArray(Layout.Directions.class, this.mColumns * 2);
        this.mLines = new int[this.mLineDirections.length];
        this.mMeasured = MeasuredText.obtain();
    }

    void generate(CharSequence source, int bufStart, int bufEnd, TextPaint paint, int outerWidth, TextDirectionHeuristic textDir, float spacingmult, float spacingadd, boolean includepad, boolean trackpad, float ellipsizedWidth, TextUtils.TruncateAt ellipsize) {
        int paraEnd;
        int spanEnd;
        int emoji;
        Bitmap bm;
        Paint whichPaint;
        int endPos;
        int above;
        int below;
        int top;
        int bottom;
        float currentTextWidth;
        int[] breakOpp = null;
        String localeLanguageTag = paint.getTextLocale().toLanguageTag();
        this.mLineCount = 0;
        int v = 0;
        boolean needMultiply = (spacingmult == 1.0f && spacingadd == 0.0f) ? false : true;
        Paint.FontMetricsInt fm = this.mFontMetricsInt;
        int[] chooseHtv = null;
        MeasuredText measured = this.mMeasured;
        Spanned spanned = null;
        if (source instanceof Spanned) {
            spanned = (Spanned) source;
        }
        int paraStart = bufStart;
        while (paraStart <= bufEnd) {
            int paraEnd2 = TextUtils.indexOf(source, CHAR_NEW_LINE, paraStart, bufEnd);
            if (paraEnd2 < 0) {
                paraEnd = bufEnd;
            } else {
                paraEnd = paraEnd2 + 1;
            }
            int firstWidthLineLimit = this.mLineCount + 1;
            int firstWidth = outerWidth;
            int restWidth = outerWidth;
            LineHeightSpan[] chooseHt = null;
            if (spanned != null) {
                LeadingMarginSpan[] sp = (LeadingMarginSpan[]) getParagraphSpans(spanned, paraStart, paraEnd, LeadingMarginSpan.class);
                for (int i = 0; i < sp.length; i++) {
                    LeadingMarginSpan lms = sp[i];
                    firstWidth -= sp[i].getLeadingMargin(true);
                    restWidth -= sp[i].getLeadingMargin(false);
                    if (lms instanceof LeadingMarginSpan.LeadingMarginSpan2) {
                        LeadingMarginSpan.LeadingMarginSpan2 lms2 = (LeadingMarginSpan.LeadingMarginSpan2) lms;
                        int lmsFirstLine = getLineForOffset(spanned.getSpanStart(lms2));
                        firstWidthLineLimit = Math.max(firstWidthLineLimit, lms2.getLeadingMarginLineCount() + lmsFirstLine);
                    }
                }
                chooseHt = (LineHeightSpan[]) getParagraphSpans(spanned, paraStart, paraEnd, LineHeightSpan.class);
                if (chooseHt.length != 0) {
                    if (chooseHtv == null || chooseHtv.length < chooseHt.length) {
                        chooseHtv = ArrayUtils.newUnpaddedIntArray(chooseHt.length);
                    }
                    for (int i2 = 0; i2 < chooseHt.length; i2++) {
                        int o = spanned.getSpanStart(chooseHt[i2]);
                        if (o < paraStart) {
                            chooseHtv[i2] = getLineTop(getLineForOffset(o));
                        } else {
                            chooseHtv[i2] = v;
                        }
                    }
                }
            }
            measured.setPara(source, paraStart, paraEnd, textDir);
            char[] chs = measured.mChars;
            float[] widths = measured.mWidths;
            byte[] chdirs = measured.mLevels;
            int dir = measured.mDir;
            boolean easy = measured.mEasy;
            breakOpp = nLineBreakOpportunities(localeLanguageTag, chs, paraEnd - paraStart, breakOpp);
            int breakOppIndex = 0;
            int width = firstWidth;
            float w = 0.0f;
            int here = paraStart;
            int ok = paraStart;
            float okWidth = 0.0f;
            int okAscent = 0;
            int okDescent = 0;
            int okTop = 0;
            int okBottom = 0;
            int fit = paraStart;
            float fitWidth = 0.0f;
            int fitAscent = 0;
            int fitDescent = 0;
            int fitTop = 0;
            int fitBottom = 0;
            float fitWidthGraphing = 0.0f;
            boolean hasTabOrEmoji = false;
            boolean hasTab = false;
            Layout.TabStops tabStops = null;
            for (int spanStart = paraStart; spanStart < paraEnd; spanStart = spanEnd) {
                if (spanned == null) {
                    spanEnd = paraEnd;
                    int spanLen = spanEnd - spanStart;
                    measured.addStyleRun(paint, spanLen, fm);
                } else {
                    spanEnd = spanned.nextSpanTransition(spanStart, paraEnd, MetricAffectingSpan.class);
                    int spanLen2 = spanEnd - spanStart;
                    measured.addStyleRun(paint, (MetricAffectingSpan[]) TextUtils.removeEmptySpans((MetricAffectingSpan[]) spanned.getSpans(spanStart, spanEnd, MetricAffectingSpan.class), spanned, MetricAffectingSpan.class), spanLen2, fm);
                }
                int fmTop = fm.top;
                int fmBottom = fm.bottom;
                int fmAscent = fm.ascent;
                int fmDescent = fm.descent;
                int j = spanStart;
                while (true) {
                    if (j >= spanEnd) {
                        break;
                    }
                    char c = chs[j - paraStart];
                    if (c != '\n') {
                        if (c == '\t') {
                            if (!hasTab) {
                                hasTab = true;
                                hasTabOrEmoji = true;
                                if (spanned != null) {
                                    TabStopSpan[] spans = (TabStopSpan[]) getParagraphSpans(spanned, paraStart, paraEnd, TabStopSpan.class);
                                    if (spans.length > 0) {
                                        tabStops = new Layout.TabStops(20, spans);
                                    }
                                }
                            }
                            if (tabStops != null) {
                                w = tabStops.nextTab(w);
                            } else {
                                w = Layout.TabStops.nextDefaultStop(w, 20);
                            }
                        } else if (c >= CHAR_FIRST_HIGH_SURROGATE && c <= CHAR_LAST_LOW_SURROGATE && j + 1 < spanEnd && (emoji = Character.codePointAt(chs, j - paraStart)) >= MIN_EMOJI && emoji <= MAX_EMOJI && (bm = EMOJI_FACTORY.getBitmapFromAndroidPua(emoji)) != null) {
                            if (spanned == null) {
                                whichPaint = paint;
                            } else {
                                whichPaint = this.mWorkPaint;
                            }
                            float wid = (bm.getWidth() * (-whichPaint.ascent())) / bm.getHeight();
                            w += wid;
                            hasTabOrEmoji = true;
                            j++;
                        } else {
                            w += widths[j - paraStart];
                        }
                    }
                    boolean isSpaceOrTab = c == ' ' || c == '\t' || c == 8203;
                    if (w <= width || isSpaceOrTab) {
                        fitWidth = w;
                        if (!isSpaceOrTab) {
                            fitWidthGraphing = w;
                        }
                        fit = j + 1;
                        if (fmTop < fitTop) {
                            fitTop = fmTop;
                        }
                        if (fmAscent < fitAscent) {
                            fitAscent = fmAscent;
                        }
                        if (fmDescent > fitDescent) {
                            fitDescent = fmDescent;
                        }
                        if (fmBottom > fitBottom) {
                            fitBottom = fmBottom;
                        }
                        while (breakOpp[breakOppIndex] != -1 && breakOpp[breakOppIndex] < (j - paraStart) + 1) {
                            breakOppIndex++;
                        }
                        boolean isLineBreak = breakOppIndex < breakOpp.length && breakOpp[breakOppIndex] == (j - paraStart) + 1;
                        if (isLineBreak) {
                            okWidth = fitWidthGraphing;
                            ok = j + 1;
                            if (fitTop < okTop) {
                                okTop = fitTop;
                            }
                            if (fitAscent < okAscent) {
                                okAscent = fitAscent;
                            }
                            if (fitDescent > okDescent) {
                                okDescent = fitDescent;
                            }
                            if (fitBottom > okBottom) {
                                okBottom = fitBottom;
                            }
                        }
                    } else {
                        if (ok != here) {
                            endPos = ok;
                            above = okAscent;
                            below = okDescent;
                            top = okTop;
                            bottom = okBottom;
                            currentTextWidth = okWidth;
                        } else if (fit != here) {
                            endPos = fit;
                            above = fitAscent;
                            below = fitDescent;
                            top = fitTop;
                            bottom = fitBottom;
                            currentTextWidth = fitWidth;
                        } else {
                            endPos = here + 1;
                            while (endPos < spanEnd && widths[endPos - paraStart] == 0.0f) {
                                endPos++;
                            }
                            above = fmAscent;
                            below = fmDescent;
                            top = fmTop;
                            bottom = fmBottom;
                            currentTextWidth = widths[here - paraStart];
                        }
                        int ellipseEnd = endPos;
                        if (this.mMaximumVisibleLineCount == 1 && ellipsize == TextUtils.TruncateAt.MIDDLE) {
                            ellipseEnd = paraEnd;
                        }
                        v = out(source, here, ellipseEnd, above, below, top, bottom, v, spacingmult, spacingadd, chooseHt, chooseHtv, fm, hasTabOrEmoji, needMultiply, chdirs, dir, easy, bufEnd, includepad, trackpad, chs, widths, paraStart, ellipsize, ellipsizedWidth, currentTextWidth, paint, true);
                        here = endPos;
                        j = here - 1;
                        fit = here;
                        ok = here;
                        w = 0.0f;
                        fitWidthGraphing = 0.0f;
                        fitBottom = 0;
                        fitTop = 0;
                        fitDescent = 0;
                        fitAscent = 0;
                        okBottom = 0;
                        okTop = 0;
                        okDescent = 0;
                        okAscent = 0;
                        firstWidthLineLimit--;
                        if (firstWidthLineLimit <= 0) {
                            width = restWidth;
                        }
                        if (here < spanStart) {
                            measured.setPos(here);
                            spanEnd = here;
                            break;
                        } else if (this.mLineCount >= this.mMaximumVisibleLineCount) {
                            return;
                        }
                    }
                    j++;
                }
            }
            if (paraEnd != here && this.mLineCount < this.mMaximumVisibleLineCount) {
                if ((fitTop | fitBottom | fitDescent | fitAscent) == 0) {
                    paint.getFontMetricsInt(fm);
                    fitTop = fm.top;
                    fitBottom = fm.bottom;
                    fitAscent = fm.ascent;
                    fitDescent = fm.descent;
                }
                v = out(source, here, paraEnd, fitAscent, fitDescent, fitTop, fitBottom, v, spacingmult, spacingadd, chooseHt, chooseHtv, fm, hasTabOrEmoji, needMultiply, chdirs, dir, easy, bufEnd, includepad, trackpad, chs, widths, paraStart, ellipsize, ellipsizedWidth, w, paint, paraEnd != bufEnd);
            }
            if (paraEnd == bufEnd) {
                break;
            } else {
                paraStart = paraEnd;
            }
        }
        if ((bufEnd == bufStart || source.charAt(bufEnd - 1) == '\n') && this.mLineCount < this.mMaximumVisibleLineCount) {
            measured.setPara(source, bufStart, bufEnd, textDir);
            paint.getFontMetricsInt(fm);
            out(source, bufEnd, bufEnd, fm.ascent, fm.descent, fm.top, fm.bottom, v, spacingmult, spacingadd, null, null, fm, false, needMultiply, measured.mLevels, measured.mDir, measured.mEasy, bufEnd, includepad, trackpad, null, null, bufStart, ellipsize, ellipsizedWidth, 0.0f, paint, false);
        }
    }

    private int out(CharSequence text, int start, int end, int above, int below, int top, int bottom, int v, float spacingmult, float spacingadd, LineHeightSpan[] chooseHt, int[] chooseHtv, Paint.FontMetricsInt fm, boolean hasTabOrEmoji, boolean needMultiply, byte[] chdirs, int dir, boolean easy, int bufEnd, boolean includePad, boolean trackPad, char[] chs, float[] widths, int widthStart, TextUtils.TruncateAt ellipsize, float ellipsisWidth, float textWidth, TextPaint paint, boolean moreChars) {
        int extra;
        int j = this.mLineCount;
        int off = j * this.mColumns;
        int want = this.mColumns + off + 1;
        int[] lines = this.mLines;
        if (want >= lines.length) {
            Layout.Directions[] grow2 = (Layout.Directions[]) ArrayUtils.newUnpaddedArray(Layout.Directions.class, GrowingArrayUtils.growSize(want));
            System.arraycopy(this.mLineDirections, 0, grow2, 0, this.mLineDirections.length);
            this.mLineDirections = grow2;
            int[] grow = new int[grow2.length];
            System.arraycopy(lines, 0, grow, 0, lines.length);
            this.mLines = grow;
            lines = grow;
        }
        if (chooseHt != null) {
            fm.ascent = above;
            fm.descent = below;
            fm.top = top;
            fm.bottom = bottom;
            for (int i = 0; i < chooseHt.length; i++) {
                if (chooseHt[i] instanceof LineHeightSpan.WithDensity) {
                    ((LineHeightSpan.WithDensity) chooseHt[i]).chooseHeight(text, start, end, chooseHtv[i], v, fm, paint);
                } else {
                    chooseHt[i].chooseHeight(text, start, end, chooseHtv[i], v, fm);
                }
            }
            above = fm.ascent;
            below = fm.descent;
            top = fm.top;
            bottom = fm.bottom;
        }
        boolean firstLine = j == 0;
        boolean currentLineIsTheLastVisibleOne = j + 1 == this.mMaximumVisibleLineCount;
        boolean lastLine = currentLineIsTheLastVisibleOne || end == bufEnd;
        if (firstLine) {
            if (trackPad) {
                this.mTopPadding = top - above;
            }
            if (includePad) {
                above = top;
            }
        }
        if (lastLine) {
            if (trackPad) {
                this.mBottomPadding = bottom - below;
            }
            if (includePad) {
                below = bottom;
            }
        }
        if (needMultiply && !lastLine) {
            double ex = ((below - above) * (spacingmult - 1.0f)) + spacingadd;
            if (ex >= 0.0d) {
                extra = (int) (EXTRA_ROUNDING + ex);
            } else {
                extra = -((int) ((-ex) + EXTRA_ROUNDING));
            }
        } else {
            extra = 0;
        }
        lines[off + 0] = start;
        lines[off + 1] = v;
        lines[off + 2] = below + extra;
        int v2 = v + (below - above) + extra;
        lines[this.mColumns + off + 0] = end;
        lines[this.mColumns + off + 1] = v2;
        if (hasTabOrEmoji) {
            int i2 = off + 0;
            lines[i2] = lines[i2] | 536870912;
        }
        int i3 = off + 0;
        lines[i3] = lines[i3] | (dir << 30);
        Layout.Directions linedirs = DIRS_ALL_LEFT_TO_RIGHT;
        if (easy) {
            this.mLineDirections[j] = linedirs;
        } else {
            this.mLineDirections[j] = AndroidBidi.directions(dir, chdirs, start - widthStart, chs, start - widthStart, end - start);
        }
        if (ellipsize != null) {
            boolean forceEllipsis = moreChars && this.mLineCount + 1 == this.mMaximumVisibleLineCount;
            boolean doEllipsis = (((this.mMaximumVisibleLineCount == 1 && moreChars) || (firstLine && !moreChars)) && ellipsize != TextUtils.TruncateAt.MARQUEE) || (!firstLine && ((currentLineIsTheLastVisibleOne || !moreChars) && ellipsize == TextUtils.TruncateAt.END));
            if (doEllipsis) {
                calculateEllipsis(start, end, widths, widthStart, ellipsisWidth, ellipsize, j, textWidth, paint, forceEllipsis);
            }
        }
        this.mLineCount++;
        return v2;
    }

    private void calculateEllipsis(int lineStart, int lineEnd, float[] widths, int widthStart, float avail, TextUtils.TruncateAt where, int line, float textWidth, TextPaint paint, boolean forceEllipsis) {
        if (textWidth <= avail && !forceEllipsis) {
            this.mLines[(this.mColumns * line) + 3] = 0;
            this.mLines[(this.mColumns * line) + 4] = 0;
            return;
        }
        float ellipsisWidth = paint.measureText(where == TextUtils.TruncateAt.END_SMALL ? TextUtils.ELLIPSIS_TWO_DOTS : TextUtils.ELLIPSIS_NORMAL, 0, 1);
        int ellipsisStart = 0;
        int ellipsisCount = 0;
        int len = lineEnd - lineStart;
        if (where == TextUtils.TruncateAt.START) {
            if (this.mMaximumVisibleLineCount == 1) {
                float sum = 0.0f;
                int i = len;
                while (i >= 0) {
                    float w = widths[((i - 1) + lineStart) - widthStart];
                    if (w + sum + ellipsisWidth > avail) {
                        break;
                    }
                    sum += w;
                    i--;
                }
                ellipsisStart = 0;
                ellipsisCount = i;
            } else if (Log.isLoggable(TAG, 5)) {
                Log.w(TAG, "Start Ellipsis only supported with one line");
            }
        } else if (where == TextUtils.TruncateAt.END || where == TextUtils.TruncateAt.MARQUEE || where == TextUtils.TruncateAt.END_SMALL) {
            float sum2 = 0.0f;
            int i2 = 0;
            while (i2 < len) {
                float w2 = widths[(i2 + lineStart) - widthStart];
                if (w2 + sum2 + ellipsisWidth > avail) {
                    break;
                }
                sum2 += w2;
                i2++;
            }
            ellipsisStart = i2;
            ellipsisCount = len - i2;
            if (forceEllipsis && ellipsisCount == 0 && len > 0) {
                ellipsisStart = len - 1;
                ellipsisCount = 1;
            }
        } else if (this.mMaximumVisibleLineCount == 1) {
            float lsum = 0.0f;
            float rsum = 0.0f;
            float ravail = (avail - ellipsisWidth) / 2.0f;
            int right = len;
            while (right > 0) {
                float w3 = widths[((right - 1) + lineStart) - widthStart];
                if (w3 + rsum > ravail) {
                    break;
                }
                rsum += w3;
                right--;
            }
            float lavail = (avail - ellipsisWidth) - rsum;
            int left = 0;
            while (left < right) {
                float w4 = widths[(left + lineStart) - widthStart];
                if (w4 + lsum > lavail) {
                    break;
                }
                lsum += w4;
                left++;
            }
            ellipsisStart = left;
            ellipsisCount = right - left;
        } else if (Log.isLoggable(TAG, 5)) {
            Log.w(TAG, "Middle Ellipsis only supported with one line");
        }
        this.mLines[(this.mColumns * line) + 3] = ellipsisStart;
        this.mLines[(this.mColumns * line) + 4] = ellipsisCount;
    }

    @Override
    public int getLineForVertical(int vertical) {
        int high = this.mLineCount;
        int low = -1;
        int[] lines = this.mLines;
        while (high - low > 1) {
            int guess = (high + low) >> 1;
            if (lines[(this.mColumns * guess) + 1] > vertical) {
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

    @Override
    public int getLineCount() {
        return this.mLineCount;
    }

    @Override
    public int getLineTop(int line) {
        int top = this.mLines[(this.mColumns * line) + 1];
        if (this.mMaximumVisibleLineCount > 0 && line >= this.mMaximumVisibleLineCount && line != this.mLineCount) {
            return top + getBottomPadding();
        }
        return top;
    }

    @Override
    public int getLineDescent(int line) {
        int descent = this.mLines[(this.mColumns * line) + 2];
        if (this.mMaximumVisibleLineCount > 0 && line >= this.mMaximumVisibleLineCount - 1 && line != this.mLineCount) {
            return descent + getBottomPadding();
        }
        return descent;
    }

    @Override
    public int getLineStart(int line) {
        return this.mLines[(this.mColumns * line) + 0] & 536870911;
    }

    @Override
    public int getParagraphDirection(int line) {
        return this.mLines[(this.mColumns * line) + 0] >> 30;
    }

    @Override
    public boolean getLineContainsTab(int line) {
        return (this.mLines[(this.mColumns * line) + 0] & 536870912) != 0;
    }

    @Override
    public final Layout.Directions getLineDirections(int line) {
        return this.mLineDirections[line];
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
    public int getEllipsisCount(int line) {
        if (this.mColumns < 5) {
            return 0;
        }
        return this.mLines[(this.mColumns * line) + 4];
    }

    @Override
    public int getEllipsisStart(int line) {
        if (this.mColumns < 5) {
            return 0;
        }
        return this.mLines[(this.mColumns * line) + 3];
    }

    @Override
    public int getEllipsizedWidth() {
        return this.mEllipsizedWidth;
    }

    void prepare() {
        this.mMeasured = MeasuredText.obtain();
    }

    void finish() {
        this.mMeasured = MeasuredText.recycle(this.mMeasured);
    }
}
