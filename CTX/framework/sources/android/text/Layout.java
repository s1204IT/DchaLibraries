package android.text;

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
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.GrowingArrayUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

public abstract class Layout {
    public static final int BREAK_STRATEGY_BALANCED = 2;
    public static final int BREAK_STRATEGY_HIGH_QUALITY = 1;
    public static final int BREAK_STRATEGY_SIMPLE = 0;
    public static final float DEFAULT_LINESPACING_ADDITION = 0.0f;
    public static final float DEFAULT_LINESPACING_MULTIPLIER = 1.0f;
    public static final int DIR_LEFT_TO_RIGHT = 1;
    static final int DIR_REQUEST_DEFAULT_LTR = 2;
    static final int DIR_REQUEST_DEFAULT_RTL = -2;
    static final int DIR_REQUEST_LTR = 1;
    static final int DIR_REQUEST_RTL = -1;
    public static final int DIR_RIGHT_TO_LEFT = -1;
    public static final int HYPHENATION_FREQUENCY_FULL = 2;
    public static final int HYPHENATION_FREQUENCY_NONE = 0;
    public static final int HYPHENATION_FREQUENCY_NORMAL = 1;
    public static final int JUSTIFICATION_MODE_INTER_WORD = 1;
    public static final int JUSTIFICATION_MODE_NONE = 0;
    static final int RUN_LEVEL_MASK = 63;
    static final int RUN_LEVEL_SHIFT = 26;
    static final int RUN_RTL_FLAG = 67108864;
    private static final int TAB_INCREMENT = 20;
    public static final int TEXT_SELECTION_LAYOUT_LEFT_TO_RIGHT = 1;
    public static final int TEXT_SELECTION_LAYOUT_RIGHT_TO_LEFT = 0;
    private Alignment mAlignment;
    private int mJustificationMode;
    private SpanSet<LineBackgroundSpan> mLineBackgroundSpans;
    private TextPaint mPaint;
    private float mSpacingAdd;
    private float mSpacingMult;
    private boolean mSpannedText;
    private CharSequence mText;
    private TextDirectionHeuristic mTextDir;
    private int mWidth;
    private TextPaint mWorkPaint;
    private static final ParagraphStyle[] NO_PARA_SPANS = (ParagraphStyle[]) ArrayUtils.emptyArray(ParagraphStyle.class);
    private static final Rect sTempRect = new Rect();
    static final int RUN_LENGTH_MASK = 67108863;

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public static final Directions DIRS_ALL_LEFT_TO_RIGHT = new Directions(new int[]{0, RUN_LENGTH_MASK});

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public static final Directions DIRS_ALL_RIGHT_TO_LEFT = new Directions(new int[]{0, 134217727});

    public enum Alignment {
        ALIGN_NORMAL,
        ALIGN_OPPOSITE,
        ALIGN_CENTER,
        ALIGN_LEFT,
        ALIGN_RIGHT
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface BreakStrategy {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface Direction {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface HyphenationFrequency {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface JustificationMode {
    }

    @FunctionalInterface
    public interface SelectionRectangleConsumer {
        void accept(float f, float f2, float f3, float f4, int i);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface TextSelectionLayout {
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

    public static float getDesiredWidth(CharSequence charSequence, TextPaint textPaint) {
        return getDesiredWidth(charSequence, 0, charSequence.length(), textPaint);
    }

    public static float getDesiredWidth(CharSequence charSequence, int i, int i2, TextPaint textPaint) {
        return getDesiredWidth(charSequence, i, i2, textPaint, TextDirectionHeuristics.FIRSTSTRONG_LTR);
    }

    public static float getDesiredWidth(CharSequence charSequence, int i, int i2, TextPaint textPaint, TextDirectionHeuristic textDirectionHeuristic) {
        return getDesiredWidthWithLimit(charSequence, i, i2, textPaint, textDirectionHeuristic, Float.MAX_VALUE);
    }

    public static float getDesiredWidthWithLimit(CharSequence charSequence, int i, int i2, TextPaint textPaint, TextDirectionHeuristic textDirectionHeuristic, float f) throws Throwable {
        float f2 = 0.0f;
        while (i <= i2) {
            int iIndexOf = TextUtils.indexOf(charSequence, '\n', i, i2);
            if (iIndexOf < 0) {
                iIndexOf = i2;
            }
            float fMeasurePara = measurePara(textPaint, charSequence, i, iIndexOf, textDirectionHeuristic);
            if (fMeasurePara > f) {
                return f;
            }
            if (fMeasurePara > f2) {
                f2 = fMeasurePara;
            }
            i = iIndexOf + 1;
        }
        return f2;
    }

    protected Layout(CharSequence charSequence, TextPaint textPaint, int i, Alignment alignment, float f, float f2) {
        this(charSequence, textPaint, i, alignment, TextDirectionHeuristics.FIRSTSTRONG_LTR, f, f2);
    }

    protected Layout(CharSequence charSequence, TextPaint textPaint, int i, Alignment alignment, TextDirectionHeuristic textDirectionHeuristic, float f, float f2) {
        this.mWorkPaint = new TextPaint();
        this.mAlignment = Alignment.ALIGN_NORMAL;
        if (i < 0) {
            throw new IllegalArgumentException("Layout: " + i + " < 0");
        }
        if (textPaint != null) {
            textPaint.bgColor = 0;
            textPaint.baselineShift = 0;
        }
        this.mText = charSequence;
        this.mPaint = textPaint;
        this.mWidth = i;
        this.mAlignment = alignment;
        this.mSpacingMult = f;
        this.mSpacingAdd = f2;
        this.mSpannedText = charSequence instanceof Spanned;
        this.mTextDir = textDirectionHeuristic;
    }

    protected void setJustificationMode(int i) {
        this.mJustificationMode = i;
    }

    void replaceWith(CharSequence charSequence, TextPaint textPaint, int i, Alignment alignment, float f, float f2) {
        if (i < 0) {
            throw new IllegalArgumentException("Layout: " + i + " < 0");
        }
        this.mText = charSequence;
        this.mPaint = textPaint;
        this.mWidth = i;
        this.mAlignment = alignment;
        this.mSpacingMult = f;
        this.mSpacingAdd = f2;
        this.mSpannedText = charSequence instanceof Spanned;
    }

    public void draw(Canvas canvas) {
        draw(canvas, null, null, 0);
    }

    public void draw(Canvas canvas, Path path, Paint paint, int i) {
        long lineRangeForDraw = getLineRangeForDraw(canvas);
        int iUnpackRangeStartFromLong = TextUtils.unpackRangeStartFromLong(lineRangeForDraw);
        int iUnpackRangeEndFromLong = TextUtils.unpackRangeEndFromLong(lineRangeForDraw);
        if (iUnpackRangeEndFromLong < 0) {
            return;
        }
        drawBackground(canvas, path, paint, i, iUnpackRangeStartFromLong, iUnpackRangeEndFromLong);
        drawText(canvas, iUnpackRangeStartFromLong, iUnpackRangeEndFromLong);
    }

    private boolean isJustificationRequired(int i) {
        int lineEnd;
        return (this.mJustificationMode == 0 || (lineEnd = getLineEnd(i)) >= this.mText.length() || this.mText.charAt(lineEnd - 1) == '\n') ? false : true;
    }

    private float getJustifyWidth(int i) {
        int leadingMargin;
        int leadingMargin2;
        int indentAdjust;
        Alignment alignment = this.mAlignment;
        int i2 = this.mWidth;
        int paragraphDirection = getParagraphDirection(i);
        ParagraphStyle[] paragraphStyleArr = NO_PARA_SPANS;
        if (this.mSpannedText) {
            Spanned spanned = (Spanned) this.mText;
            int lineStart = getLineStart(i);
            boolean z = lineStart == 0 || this.mText.charAt(lineStart + (-1)) == '\n';
            if (z) {
                paragraphStyleArr = (ParagraphStyle[]) getParagraphSpans(spanned, lineStart, spanned.nextSpanTransition(lineStart, this.mText.length(), ParagraphStyle.class), ParagraphStyle.class);
                int length = paragraphStyleArr.length - 1;
                while (true) {
                    if (length < 0) {
                        break;
                    }
                    if (paragraphStyleArr[length] instanceof AlignmentSpan) {
                        alignment = ((AlignmentSpan) paragraphStyleArr[length]).getAlignment();
                        break;
                    }
                    length--;
                }
            }
            int length2 = paragraphStyleArr.length;
            int i3 = 0;
            while (true) {
                if (i3 >= length2) {
                    break;
                }
                if (paragraphStyleArr[i3] instanceof LeadingMarginSpan.LeadingMarginSpan2) {
                    if (i < getLineForOffset(spanned.getSpanStart(paragraphStyleArr[i3])) + ((LeadingMarginSpan.LeadingMarginSpan2) paragraphStyleArr[i3]).getLeadingMarginLineCount()) {
                        z = true;
                        break;
                    }
                }
                i3++;
            }
            leadingMargin = i2;
            leadingMargin2 = 0;
            for (int i4 = 0; i4 < length2; i4++) {
                if (paragraphStyleArr[i4] instanceof LeadingMarginSpan) {
                    LeadingMarginSpan leadingMarginSpan = (LeadingMarginSpan) paragraphStyleArr[i4];
                    if (paragraphDirection == -1) {
                        leadingMargin -= leadingMarginSpan.getLeadingMargin(z);
                    } else {
                        leadingMargin2 += leadingMarginSpan.getLeadingMargin(z);
                    }
                }
            }
        } else {
            leadingMargin = i2;
            leadingMargin2 = 0;
        }
        if (alignment == Alignment.ALIGN_LEFT) {
            alignment = paragraphDirection == 1 ? Alignment.ALIGN_NORMAL : Alignment.ALIGN_OPPOSITE;
        } else if (alignment == Alignment.ALIGN_RIGHT) {
            alignment = paragraphDirection == 1 ? Alignment.ALIGN_OPPOSITE : Alignment.ALIGN_NORMAL;
        }
        if (alignment == Alignment.ALIGN_NORMAL) {
            if (paragraphDirection == 1) {
                indentAdjust = getIndentAdjust(i, Alignment.ALIGN_LEFT);
            } else {
                indentAdjust = -getIndentAdjust(i, Alignment.ALIGN_RIGHT);
            }
        } else if (alignment == Alignment.ALIGN_OPPOSITE) {
            if (paragraphDirection == 1) {
                indentAdjust = -getIndentAdjust(i, Alignment.ALIGN_RIGHT);
            } else {
                indentAdjust = getIndentAdjust(i, Alignment.ALIGN_LEFT);
            }
        } else {
            indentAdjust = getIndentAdjust(i, Alignment.ALIGN_CENTER);
        }
        return (leadingMargin - leadingMargin2) - indentAdjust;
    }

    public void drawText(Canvas canvas, int i, int i2) {
        int i3;
        int i4;
        int i5;
        int i6;
        int i7;
        int i8;
        CharSequence charSequence;
        TextPaint textPaint;
        TextLine textLine;
        TabStops tabStops;
        int i9;
        Alignment alignment;
        int leadingMargin;
        int leadingMargin2;
        TabStops tabStops2;
        int i10;
        int i11;
        Alignment alignment2;
        int i12;
        int i13;
        int indentAdjust;
        int i14;
        TextLine textLine2;
        int indentAdjust2;
        int i15;
        ParagraphStyle[] paragraphStyleArr;
        Alignment alignment3;
        boolean z;
        boolean z2;
        ParagraphStyle[] paragraphStyleArr2;
        int length;
        int i16;
        boolean z3;
        int i17;
        int i18;
        int i19;
        int i20;
        int i21;
        int i22;
        int i23;
        int i24;
        CharSequence charSequence2;
        TextPaint textPaint2;
        TextLine textLine3;
        TabStops tabStops3;
        boolean z4;
        ParagraphStyle[] paragraphStyleArr3;
        int i25 = i;
        int lineTop = getLineTop(i25);
        int lineStart = getLineStart(i25);
        ParagraphStyle[] paragraphStyleArr4 = NO_PARA_SPANS;
        TextPaint textPaint3 = this.mWorkPaint;
        textPaint3.set(this.mPaint);
        CharSequence charSequence3 = this.mText;
        Alignment alignment4 = this.mAlignment;
        TextLine textLineObtain = TextLine.obtain();
        int i26 = lineTop;
        int i27 = lineStart;
        TabStops tabStops4 = null;
        int i28 = i25;
        int i29 = 0;
        boolean z5 = false;
        while (i28 <= i2) {
            int i30 = i28 + 1;
            int lineStart2 = getLineStart(i30);
            boolean zIsJustificationRequired = isJustificationRequired(i28);
            int lineVisibleEnd = getLineVisibleEnd(i28, i27, lineStart2);
            textPaint3.setHyphenEdit(getHyphen(i28));
            int lineTop2 = getLineTop(i30);
            int lineDescent = lineTop2 - getLineDescent(i28);
            TextLine textLine4 = textLineObtain;
            int paragraphDirection = getParagraphDirection(i28);
            boolean z6 = z5;
            int i31 = this.mWidth;
            TabStops tabStops5 = tabStops4;
            if (!this.mSpannedText) {
                i3 = lineStart2;
                i4 = i30;
                i5 = i27;
                i6 = i28;
                i7 = lineDescent;
                i8 = paragraphDirection;
                charSequence = charSequence3;
                textPaint = textPaint3;
                textLine = textLine4;
                tabStops = tabStops5;
                i9 = i29;
                alignment = alignment4;
                leadingMargin = i31;
                leadingMargin2 = 0;
            } else {
                Spanned spanned = (Spanned) charSequence3;
                int length2 = charSequence3.length();
                if (i27 == 0) {
                    paragraphStyleArr = paragraphStyleArr4;
                    alignment3 = alignment4;
                } else {
                    paragraphStyleArr = paragraphStyleArr4;
                    alignment3 = alignment4;
                    if (charSequence3.charAt(i27 - 1) != '\n') {
                        z = false;
                    }
                    if (i27 >= i29 || !(i28 == i25 || z)) {
                        z2 = true;
                        i9 = i29;
                        paragraphStyleArr2 = paragraphStyleArr;
                    } else {
                        int iNextSpanTransition = spanned.nextSpanTransition(i27, length2, ParagraphStyle.class);
                        ParagraphStyle[] paragraphStyleArr5 = (ParagraphStyle[]) getParagraphSpans(spanned, i27, iNextSpanTransition, ParagraphStyle.class);
                        Alignment alignment5 = this.mAlignment;
                        z2 = true;
                        int length3 = paragraphStyleArr5.length - 1;
                        while (true) {
                            if (length3 >= 0) {
                                i9 = iNextSpanTransition;
                                if (!(paragraphStyleArr5[length3] instanceof AlignmentSpan)) {
                                    length3--;
                                    iNextSpanTransition = i9;
                                } else {
                                    alignment5 = ((AlignmentSpan) paragraphStyleArr5[length3]).getAlignment();
                                    break;
                                }
                            } else {
                                i9 = iNextSpanTransition;
                                break;
                            }
                        }
                        paragraphStyleArr2 = paragraphStyleArr5;
                        alignment3 = alignment5;
                        z6 = false;
                    }
                    length = paragraphStyleArr2.length;
                    i16 = 0;
                    while (true) {
                        if (i16 >= length) {
                            if (!(paragraphStyleArr2[i16] instanceof LeadingMarginSpan.LeadingMarginSpan2)) {
                                i3 = lineStart2;
                            } else {
                                i3 = lineStart2;
                                if (i28 < getLineForOffset(spanned.getSpanStart(paragraphStyleArr2[i16])) + ((LeadingMarginSpan.LeadingMarginSpan2) paragraphStyleArr2[i16]).getLeadingMarginLineCount()) {
                                    z3 = z2;
                                    break;
                                }
                            }
                            i16++;
                            lineStart2 = i3;
                        } else {
                            i3 = lineStart2;
                            z3 = z;
                            break;
                        }
                    }
                    leadingMargin = i31;
                    i17 = 0;
                    leadingMargin2 = 0;
                    while (i17 < length) {
                        if (!(paragraphStyleArr2[i17] instanceof LeadingMarginSpan)) {
                            i18 = i17;
                            i19 = length;
                            i20 = i30;
                            i21 = i27;
                            i22 = i28;
                            i23 = lineDescent;
                            i24 = paragraphDirection;
                            charSequence2 = charSequence3;
                            textPaint2 = textPaint3;
                            textLine3 = textLine4;
                            tabStops3 = tabStops5;
                            z4 = z3;
                            paragraphStyleArr3 = paragraphStyleArr2;
                        } else {
                            LeadingMarginSpan leadingMarginSpan = (LeadingMarginSpan) paragraphStyleArr2[i17];
                            if (paragraphDirection == -1) {
                                i18 = i17;
                                i19 = length;
                                textPaint2 = textPaint3;
                                z4 = z3;
                                i20 = i30;
                                tabStops3 = tabStops5;
                                paragraphStyleArr3 = paragraphStyleArr2;
                                i21 = i27;
                                i22 = i28;
                                i23 = lineDescent;
                                i24 = paragraphDirection;
                                textLine3 = textLine4;
                                charSequence2 = charSequence3;
                                leadingMarginSpan.drawLeadingMargin(canvas, textPaint3, leadingMargin, paragraphDirection, i26, lineDescent, lineTop2, charSequence3, i21, lineVisibleEnd, z, this);
                                leadingMargin -= leadingMarginSpan.getLeadingMargin(z4);
                            } else {
                                i18 = i17;
                                i19 = length;
                                i20 = i30;
                                i21 = i27;
                                i22 = i28;
                                i23 = lineDescent;
                                i24 = paragraphDirection;
                                charSequence2 = charSequence3;
                                textPaint2 = textPaint3;
                                textLine3 = textLine4;
                                tabStops3 = tabStops5;
                                z4 = z3;
                                paragraphStyleArr3 = paragraphStyleArr2;
                                leadingMarginSpan.drawLeadingMargin(canvas, textPaint2, leadingMargin2, i24, i26, i23, lineTop2, charSequence2, i21, lineVisibleEnd, z, this);
                                leadingMargin2 += leadingMarginSpan.getLeadingMargin(z4);
                            }
                        }
                        i17 = i18 + 1;
                        z3 = z4;
                        charSequence3 = charSequence2;
                        paragraphStyleArr2 = paragraphStyleArr3;
                        i27 = i21;
                        length = i19;
                        textPaint3 = textPaint2;
                        i30 = i20;
                        tabStops5 = tabStops3;
                        i28 = i22;
                        lineDescent = i23;
                        textLine4 = textLine3;
                        paragraphDirection = i24;
                    }
                    i4 = i30;
                    i5 = i27;
                    i6 = i28;
                    i7 = lineDescent;
                    i8 = paragraphDirection;
                    charSequence = charSequence3;
                    textPaint = textPaint3;
                    textLine = textLine4;
                    tabStops = tabStops5;
                    paragraphStyleArr4 = paragraphStyleArr2;
                    alignment = alignment3;
                }
                z = true;
                if (i27 >= i29) {
                    z2 = true;
                    i9 = i29;
                    paragraphStyleArr2 = paragraphStyleArr;
                    length = paragraphStyleArr2.length;
                    i16 = 0;
                    while (true) {
                        if (i16 >= length) {
                        }
                        i16++;
                        lineStart2 = i3;
                    }
                    leadingMargin = i31;
                    i17 = 0;
                    leadingMargin2 = 0;
                    while (i17 < length) {
                    }
                    i4 = i30;
                    i5 = i27;
                    i6 = i28;
                    i7 = lineDescent;
                    i8 = paragraphDirection;
                    charSequence = charSequence3;
                    textPaint = textPaint3;
                    textLine = textLine4;
                    tabStops = tabStops5;
                    paragraphStyleArr4 = paragraphStyleArr2;
                    alignment = alignment3;
                }
            }
            int i32 = i6;
            boolean lineContainsTab = getLineContainsTab(i32);
            if (!lineContainsTab || z6) {
                tabStops2 = tabStops;
            } else {
                TabStops tabStops6 = tabStops;
                if (tabStops6 == null) {
                    tabStops6 = new TabStops(20, paragraphStyleArr4);
                } else {
                    tabStops6.reset(20, paragraphStyleArr4);
                }
                tabStops2 = tabStops6;
                z6 = true;
            }
            if (alignment == Alignment.ALIGN_LEFT) {
                i10 = i8;
                i11 = 1;
                alignment2 = i10 == 1 ? Alignment.ALIGN_NORMAL : Alignment.ALIGN_OPPOSITE;
            } else {
                i10 = i8;
                i11 = 1;
                if (alignment == Alignment.ALIGN_RIGHT) {
                    alignment2 = i10 == 1 ? Alignment.ALIGN_OPPOSITE : Alignment.ALIGN_NORMAL;
                } else {
                    alignment2 = alignment;
                }
            }
            if (alignment2 == Alignment.ALIGN_NORMAL) {
                if (i10 == i11) {
                    indentAdjust2 = getIndentAdjust(i32, Alignment.ALIGN_LEFT);
                    i15 = leadingMargin2 + indentAdjust2;
                } else {
                    indentAdjust2 = -getIndentAdjust(i32, Alignment.ALIGN_RIGHT);
                    i15 = leadingMargin - indentAdjust2;
                }
                i13 = indentAdjust2;
                i12 = i15;
            } else {
                int lineExtent = (int) getLineExtent(i32, tabStops2, false);
                if (alignment2 == Alignment.ALIGN_OPPOSITE) {
                    if (i10 == i11) {
                        indentAdjust = -getIndentAdjust(i32, Alignment.ALIGN_RIGHT);
                        i14 = (leadingMargin - lineExtent) - indentAdjust;
                    } else {
                        indentAdjust = getIndentAdjust(i32, Alignment.ALIGN_LEFT);
                        i14 = (leadingMargin2 - lineExtent) + indentAdjust;
                    }
                    i13 = indentAdjust;
                    i12 = i14;
                } else {
                    int indentAdjust3 = getIndentAdjust(i32, Alignment.ALIGN_CENTER);
                    i12 = (((leadingMargin + leadingMargin2) - (lineExtent & (-2))) >> 1) + indentAdjust3;
                    i13 = indentAdjust3;
                }
            }
            Directions lineDirections = getLineDirections(i32);
            if (lineDirections == DIRS_ALL_LEFT_TO_RIGHT && !this.mSpannedText && !lineContainsTab && !zIsJustificationRequired) {
                canvas.drawText(charSequence, i5, lineVisibleEnd, i12, i7, textPaint);
                textLine2 = textLine;
            } else {
                int i33 = i7;
                textLine.set(textPaint, charSequence, i5, lineVisibleEnd, i10, lineDirections, lineContainsTab, tabStops2);
                if (zIsJustificationRequired) {
                    textLine2 = textLine;
                    textLine2.justify((leadingMargin - leadingMargin2) - i13);
                } else {
                    textLine2 = textLine;
                }
                textLine2.draw(canvas, i12, i26, i33, lineTop2);
            }
            alignment4 = alignment;
            tabStops4 = tabStops2;
            textLineObtain = textLine2;
            charSequence3 = charSequence;
            i26 = lineTop2;
            z5 = z6;
            i29 = i9;
            i27 = i3;
            textPaint3 = textPaint;
            i28 = i4;
            i25 = i;
        }
        TextLine.recycle(textLineObtain);
    }

    public void drawBackground(Canvas canvas, Path path, Paint paint, int i, int i2, int i3) {
        int i4;
        ParagraphStyle[] paragraphStyleArr;
        int i5;
        int i6;
        if (this.mSpannedText) {
            if (this.mLineBackgroundSpans == null) {
                this.mLineBackgroundSpans = new SpanSet<>(LineBackgroundSpan.class);
            }
            Spanned spanned = (Spanned) this.mText;
            int length = spanned.length();
            int i7 = 0;
            this.mLineBackgroundSpans.init(spanned, 0, length);
            if (this.mLineBackgroundSpans.numberOfSpans > 0) {
                int lineTop = getLineTop(i2);
                int lineStart = getLineStart(i2);
                ParagraphStyle[] paragraphStyleArr2 = NO_PARA_SPANS;
                TextPaint textPaint = this.mPaint;
                int i8 = this.mWidth;
                int i9 = i2;
                int i10 = lineTop;
                int i11 = lineStart;
                int i12 = 0;
                int i13 = 0;
                while (i9 <= i3) {
                    int i14 = i9 + 1;
                    int lineStart2 = getLineStart(i14);
                    int lineTop2 = getLineTop(i14);
                    int lineDescent = lineTop2 - getLineDescent(i9);
                    if (i11 >= i12) {
                        int nextTransition = this.mLineBackgroundSpans.getNextTransition(i11, length);
                        if (i11 != lineStart2 || i11 == 0) {
                            int i15 = i7;
                            ParagraphStyle[] paragraphStyleArr3 = paragraphStyleArr2;
                            int i16 = i15;
                            while (true) {
                                i6 = nextTransition;
                                if (i15 >= this.mLineBackgroundSpans.numberOfSpans) {
                                    break;
                                }
                                if (this.mLineBackgroundSpans.spanStarts[i15] < lineStart2 && this.mLineBackgroundSpans.spanEnds[i15] > i11) {
                                    ParagraphStyle[] paragraphStyleArr4 = (ParagraphStyle[]) GrowingArrayUtils.append((LineBackgroundSpan[]) paragraphStyleArr3, i16, this.mLineBackgroundSpans.spans[i15]);
                                    i16++;
                                    paragraphStyleArr3 = paragraphStyleArr4;
                                }
                                i15++;
                                nextTransition = i6;
                            }
                            i4 = i6;
                            paragraphStyleArr = paragraphStyleArr3;
                            i7 = i16;
                            i5 = 0;
                            while (i5 < i7) {
                                int i17 = lineStart2;
                                int i18 = i11;
                                int i19 = i9;
                                ((LineBackgroundSpan) paragraphStyleArr[i5]).drawBackground(canvas, textPaint, 0, i8, i10, lineDescent, lineTop2, spanned, i18, i17, i19);
                                i5++;
                                i7 = i7;
                                i14 = i14;
                                lineStart2 = i17;
                                i11 = i18;
                                i9 = i19;
                                i8 = i8;
                                textPaint = textPaint;
                                length = length;
                            }
                            i10 = lineTop2;
                            i13 = i7;
                            paragraphStyleArr2 = paragraphStyleArr;
                            i12 = i4;
                            i9 = i14;
                            i11 = lineStart2;
                            i7 = 0;
                        } else {
                            i4 = nextTransition;
                        }
                    } else {
                        i4 = i12;
                        i7 = i13;
                    }
                    paragraphStyleArr = paragraphStyleArr2;
                    i5 = 0;
                    while (i5 < i7) {
                    }
                    i10 = lineTop2;
                    i13 = i7;
                    paragraphStyleArr2 = paragraphStyleArr;
                    i12 = i4;
                    i9 = i14;
                    i11 = lineStart2;
                    i7 = 0;
                }
            }
            this.mLineBackgroundSpans.recycle();
        }
        if (path != null) {
            if (i != 0) {
                canvas.translate(0.0f, i);
            }
            canvas.drawPath(path, paint);
            if (i != 0) {
                canvas.translate(0.0f, -i);
            }
        }
    }

    public long getLineRangeForDraw(Canvas canvas) {
        synchronized (sTempRect) {
            if (!canvas.getClipBounds(sTempRect)) {
                return TextUtils.packRangeInLong(0, -1);
            }
            int i = sTempRect.top;
            int i2 = sTempRect.bottom;
            int iMax = Math.max(i, 0);
            int iMin = Math.min(getLineTop(getLineCount()), i2);
            return iMax >= iMin ? TextUtils.packRangeInLong(0, -1) : TextUtils.packRangeInLong(getLineForVertical(iMax), getLineForVertical(iMin));
        }
    }

    private int getLineStartPos(int i, int i2, int i3) {
        Alignment paragraphAlignment = getParagraphAlignment(i);
        int paragraphDirection = getParagraphDirection(i);
        if (paragraphAlignment == Alignment.ALIGN_LEFT) {
            paragraphAlignment = paragraphDirection == 1 ? Alignment.ALIGN_NORMAL : Alignment.ALIGN_OPPOSITE;
        } else if (paragraphAlignment == Alignment.ALIGN_RIGHT) {
            paragraphAlignment = paragraphDirection == 1 ? Alignment.ALIGN_OPPOSITE : Alignment.ALIGN_NORMAL;
        }
        if (paragraphAlignment == Alignment.ALIGN_NORMAL) {
            if (paragraphDirection == 1) {
                return i2 + getIndentAdjust(i, Alignment.ALIGN_LEFT);
            }
            return i3 + getIndentAdjust(i, Alignment.ALIGN_RIGHT);
        }
        TabStops tabStops = null;
        if (this.mSpannedText && getLineContainsTab(i)) {
            Spanned spanned = (Spanned) this.mText;
            int lineStart = getLineStart(i);
            TabStopSpan[] tabStopSpanArr = (TabStopSpan[]) getParagraphSpans(spanned, lineStart, spanned.nextSpanTransition(lineStart, spanned.length(), TabStopSpan.class), TabStopSpan.class);
            if (tabStopSpanArr.length > 0) {
                tabStops = new TabStops(20, tabStopSpanArr);
            }
        }
        int lineExtent = (int) getLineExtent(i, tabStops, false);
        if (paragraphAlignment == Alignment.ALIGN_OPPOSITE) {
            if (paragraphDirection == 1) {
                return (i3 - lineExtent) + getIndentAdjust(i, Alignment.ALIGN_RIGHT);
            }
            return (i2 - lineExtent) + getIndentAdjust(i, Alignment.ALIGN_LEFT);
        }
        return ((i2 + i3) - (lineExtent & (-2))) >> (1 + getIndentAdjust(i, Alignment.ALIGN_CENTER));
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

    public final void increaseWidthTo(int i) {
        if (i < this.mWidth) {
            throw new RuntimeException("attempted to reduce Layout width");
        }
        this.mWidth = i;
    }

    public int getHeight() {
        return getLineTop(getLineCount());
    }

    public int getHeight(boolean z) {
        return getHeight();
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

    public int getLineBounds(int i, Rect rect) {
        if (rect != null) {
            rect.left = 0;
            rect.top = getLineTop(i);
            rect.right = this.mWidth;
            rect.bottom = getLineTop(i + 1);
        }
        return getLineBaseline(i);
    }

    public int getHyphen(int i) {
        return 0;
    }

    public int getIndentAdjust(int i, Alignment alignment) {
        return 0;
    }

    public boolean isLevelBoundary(int i) {
        int lineForOffset = getLineForOffset(i);
        Directions lineDirections = getLineDirections(lineForOffset);
        if (lineDirections == DIRS_ALL_LEFT_TO_RIGHT || lineDirections == DIRS_ALL_RIGHT_TO_LEFT) {
            return false;
        }
        int[] iArr = lineDirections.mDirections;
        int lineStart = getLineStart(lineForOffset);
        int lineEnd = getLineEnd(lineForOffset);
        if (i == lineStart || i == lineEnd) {
            int i2 = getParagraphDirection(lineForOffset) == 1 ? 0 : 1;
            return ((iArr[(i != lineStart ? iArr.length - 2 : 0) + 1] >>> 26) & 63) != i2;
        }
        int i3 = i - lineStart;
        for (int i4 = 0; i4 < iArr.length; i4 += 2) {
            if (i3 == iArr[i4]) {
                return true;
            }
        }
        return false;
    }

    public boolean isRtlCharAt(int i) {
        int lineForOffset = getLineForOffset(i);
        Directions lineDirections = getLineDirections(lineForOffset);
        if (lineDirections == DIRS_ALL_LEFT_TO_RIGHT) {
            return false;
        }
        if (lineDirections == DIRS_ALL_RIGHT_TO_LEFT) {
            return true;
        }
        int[] iArr = lineDirections.mDirections;
        int lineStart = getLineStart(lineForOffset);
        for (int i2 = 0; i2 < iArr.length; i2 += 2) {
            int i3 = iArr[i2] + lineStart;
            int i4 = i2 + 1;
            int i5 = (iArr[i4] & RUN_LENGTH_MASK) + i3;
            if (i >= i3 && i < i5) {
                return (((iArr[i4] >>> 26) & 63) & 1) != 0;
            }
        }
        return false;
    }

    public long getRunRange(int i) {
        int lineForOffset = getLineForOffset(i);
        Directions lineDirections = getLineDirections(lineForOffset);
        if (lineDirections == DIRS_ALL_LEFT_TO_RIGHT || lineDirections == DIRS_ALL_RIGHT_TO_LEFT) {
            return TextUtils.packRangeInLong(0, getLineEnd(lineForOffset));
        }
        int[] iArr = lineDirections.mDirections;
        int lineStart = getLineStart(lineForOffset);
        for (int i2 = 0; i2 < iArr.length; i2 += 2) {
            int i3 = iArr[i2] + lineStart;
            int i4 = (iArr[i2 + 1] & RUN_LENGTH_MASK) + i3;
            if (i >= i3 && i < i4) {
                return TextUtils.packRangeInLong(i3, i4);
            }
        }
        return TextUtils.packRangeInLong(0, getLineEnd(lineForOffset));
    }

    private boolean primaryIsTrailingPrevious(int i) {
        int i2;
        int i3;
        int lineForOffset = getLineForOffset(i);
        int lineStart = getLineStart(lineForOffset);
        int lineEnd = getLineEnd(lineForOffset);
        int[] iArr = getLineDirections(lineForOffset).mDirections;
        int i4 = 0;
        while (true) {
            i2 = -1;
            if (i4 < iArr.length) {
                int i5 = iArr[i4] + lineStart;
                int i6 = i4 + 1;
                int i7 = (iArr[i6] & RUN_LENGTH_MASK) + i5;
                if (i7 > lineEnd) {
                    i7 = lineEnd;
                }
                if (i < i5 || i >= i7) {
                    i4 += 2;
                } else {
                    if (i > i5) {
                        return false;
                    }
                    i3 = (iArr[i6] >>> 26) & 63;
                }
            } else {
                i3 = -1;
                break;
            }
        }
        if (i3 == -1) {
            i3 = getParagraphDirection(lineForOffset) == 1 ? 0 : 1;
        }
        if (i != lineStart) {
            int i8 = i - 1;
            int i9 = 0;
            while (true) {
                if (i9 >= iArr.length) {
                    break;
                }
                int i10 = iArr[i9] + lineStart;
                int i11 = i9 + 1;
                int i12 = (iArr[i11] & RUN_LENGTH_MASK) + i10;
                if (i12 > lineEnd) {
                    i12 = lineEnd;
                }
                if (i8 < i10 || i8 >= i12) {
                    i9 += 2;
                } else {
                    i2 = (iArr[i11] >>> 26) & 63;
                    break;
                }
            }
        } else {
            i2 = getParagraphDirection(lineForOffset) == 1 ? 0 : 1;
        }
        return i2 < i3;
    }

    private boolean[] primaryIsTrailingPreviousAllLineOffsets(int i) {
        byte b;
        int lineStart = getLineStart(i);
        int lineEnd = getLineEnd(i);
        int[] iArr = getLineDirections(i).mDirections;
        int i2 = (lineEnd - lineStart) + 1;
        boolean[] zArr = new boolean[i2];
        byte[] bArr = new byte[i2];
        for (int i3 = 0; i3 < iArr.length; i3 += 2) {
            int i4 = iArr[i3] + lineStart;
            int i5 = i3 + 1;
            int i6 = (iArr[i5] & RUN_LENGTH_MASK) + i4;
            if (i6 > lineEnd) {
                i6 = lineEnd;
            }
            if (i6 != i4) {
                bArr[(i6 - lineStart) - 1] = (byte) ((iArr[i5] >>> 26) & 63);
            }
        }
        for (int i7 = 0; i7 < iArr.length; i7 += 2) {
            int i8 = iArr[i7] + lineStart;
            byte b2 = (byte) ((iArr[i7 + 1] >>> 26) & 63);
            int i9 = i8 - lineStart;
            if (i8 == lineStart) {
                b = getParagraphDirection(i) == 1 ? (byte) 0 : (byte) 1;
            } else {
                b = bArr[i9 - 1];
            }
            zArr[i9] = b2 > b;
        }
        return zArr;
    }

    public float getPrimaryHorizontal(int i) {
        return getPrimaryHorizontal(i, false);
    }

    public float getPrimaryHorizontal(int i, boolean z) {
        return getHorizontal(i, primaryIsTrailingPrevious(i), z);
    }

    public float getSecondaryHorizontal(int i) {
        return getSecondaryHorizontal(i, false);
    }

    public float getSecondaryHorizontal(int i, boolean z) {
        return getHorizontal(i, !primaryIsTrailingPrevious(i), z);
    }

    private float getHorizontal(int i, boolean z) {
        return z ? getPrimaryHorizontal(i) : getSecondaryHorizontal(i);
    }

    private float getHorizontal(int i, boolean z, boolean z2) {
        return getHorizontal(i, z, getLineForOffset(i), z2);
    }

    private float getHorizontal(int i, boolean z, int i2, boolean z2) {
        TabStops tabStops;
        int lineStart = getLineStart(i2);
        int lineEnd = getLineEnd(i2);
        int paragraphDirection = getParagraphDirection(i2);
        boolean lineContainsTab = getLineContainsTab(i2);
        Directions lineDirections = getLineDirections(i2);
        if (!lineContainsTab || !(this.mText instanceof Spanned)) {
            tabStops = null;
        } else {
            TabStopSpan[] tabStopSpanArr = (TabStopSpan[]) getParagraphSpans((Spanned) this.mText, lineStart, lineEnd, TabStopSpan.class);
            if (tabStopSpanArr.length > 0) {
                tabStops = new TabStops(20, tabStopSpanArr);
            }
        }
        TextLine textLineObtain = TextLine.obtain();
        textLineObtain.set(this.mPaint, this.mText, lineStart, lineEnd, paragraphDirection, lineDirections, lineContainsTab, tabStops);
        float fMeasure = textLineObtain.measure(i - lineStart, z, null);
        TextLine.recycle(textLineObtain);
        if (z2 && fMeasure > this.mWidth) {
            fMeasure = this.mWidth;
        }
        return getLineStartPos(i2, getParagraphLeft(i2), getParagraphRight(i2)) + fMeasure;
    }

    private float[] getLineHorizontals(int i, boolean z, boolean z2) {
        TabStops tabStops;
        int lineStart = getLineStart(i);
        int lineEnd = getLineEnd(i);
        int paragraphDirection = getParagraphDirection(i);
        boolean lineContainsTab = getLineContainsTab(i);
        Directions lineDirections = getLineDirections(i);
        if (!lineContainsTab || !(this.mText instanceof Spanned)) {
            tabStops = null;
        } else {
            TabStopSpan[] tabStopSpanArr = (TabStopSpan[]) getParagraphSpans((Spanned) this.mText, lineStart, lineEnd, TabStopSpan.class);
            if (tabStopSpanArr.length > 0) {
                tabStops = new TabStops(20, tabStopSpanArr);
            }
        }
        TextLine textLineObtain = TextLine.obtain();
        textLineObtain.set(this.mPaint, this.mText, lineStart, lineEnd, paragraphDirection, lineDirections, lineContainsTab, tabStops);
        boolean[] zArrPrimaryIsTrailingPreviousAllLineOffsets = primaryIsTrailingPreviousAllLineOffsets(i);
        if (!z2) {
            for (int i2 = 0; i2 < zArrPrimaryIsTrailingPreviousAllLineOffsets.length; i2++) {
                zArrPrimaryIsTrailingPreviousAllLineOffsets[i2] = !zArrPrimaryIsTrailingPreviousAllLineOffsets[i2];
            }
        }
        float[] fArrMeasureAllOffsets = textLineObtain.measureAllOffsets(zArrPrimaryIsTrailingPreviousAllLineOffsets, null);
        TextLine.recycle(textLineObtain);
        if (z) {
            for (int i3 = 0; i3 < fArrMeasureAllOffsets.length; i3++) {
                if (fArrMeasureAllOffsets[i3] > this.mWidth) {
                    fArrMeasureAllOffsets[i3] = this.mWidth;
                }
            }
        }
        int lineStartPos = getLineStartPos(i, getParagraphLeft(i), getParagraphRight(i));
        float[] fArr = new float[(lineEnd - lineStart) + 1];
        for (int i4 = 0; i4 < fArr.length; i4++) {
            fArr[i4] = lineStartPos + fArrMeasureAllOffsets[i4];
        }
        return fArr;
    }

    public float getLineLeft(int i) {
        int paragraphDirection = getParagraphDirection(i);
        Alignment paragraphAlignment = getParagraphAlignment(i);
        if (paragraphAlignment == Alignment.ALIGN_LEFT) {
            return 0.0f;
        }
        if (paragraphAlignment == Alignment.ALIGN_NORMAL) {
            if (paragraphDirection == -1) {
                return getParagraphRight(i) - getLineMax(i);
            }
            return 0.0f;
        }
        if (paragraphAlignment == Alignment.ALIGN_RIGHT) {
            return this.mWidth - getLineMax(i);
        }
        if (paragraphAlignment == Alignment.ALIGN_OPPOSITE) {
            if (paragraphDirection == -1) {
                return 0.0f;
            }
            return this.mWidth - getLineMax(i);
        }
        int paragraphLeft = getParagraphLeft(i);
        return paragraphLeft + (((getParagraphRight(i) - paragraphLeft) - (((int) getLineMax(i)) & (-2))) / 2);
    }

    public float getLineRight(int i) {
        int paragraphDirection = getParagraphDirection(i);
        Alignment paragraphAlignment = getParagraphAlignment(i);
        if (paragraphAlignment == Alignment.ALIGN_LEFT) {
            return getParagraphLeft(i) + getLineMax(i);
        }
        if (paragraphAlignment == Alignment.ALIGN_NORMAL) {
            if (paragraphDirection == -1) {
                return this.mWidth;
            }
            return getParagraphLeft(i) + getLineMax(i);
        }
        if (paragraphAlignment == Alignment.ALIGN_RIGHT) {
            return this.mWidth;
        }
        if (paragraphAlignment == Alignment.ALIGN_OPPOSITE) {
            if (paragraphDirection == -1) {
                return getLineMax(i);
            }
            return this.mWidth;
        }
        int paragraphLeft = getParagraphLeft(i);
        int paragraphRight = getParagraphRight(i);
        return paragraphRight - (((paragraphRight - paragraphLeft) - (((int) getLineMax(i)) & (-2))) / 2);
    }

    public float getLineMax(int i) {
        float paragraphLeadingMargin = getParagraphLeadingMargin(i);
        float lineExtent = getLineExtent(i, false);
        if (lineExtent < 0.0f) {
            lineExtent = -lineExtent;
        }
        return paragraphLeadingMargin + lineExtent;
    }

    public float getLineWidth(int i) {
        float paragraphLeadingMargin = getParagraphLeadingMargin(i);
        float lineExtent = getLineExtent(i, true);
        if (lineExtent < 0.0f) {
            lineExtent = -lineExtent;
        }
        return paragraphLeadingMargin + lineExtent;
    }

    private float getLineExtent(int i, boolean z) {
        TabStops tabStops;
        int lineStart = getLineStart(i);
        int lineEnd = z ? getLineEnd(i) : getLineVisibleEnd(i);
        boolean lineContainsTab = getLineContainsTab(i);
        if (!lineContainsTab || !(this.mText instanceof Spanned)) {
            tabStops = null;
        } else {
            TabStopSpan[] tabStopSpanArr = (TabStopSpan[]) getParagraphSpans((Spanned) this.mText, lineStart, lineEnd, TabStopSpan.class);
            if (tabStopSpanArr.length > 0) {
                tabStops = new TabStops(20, tabStopSpanArr);
            }
        }
        Directions lineDirections = getLineDirections(i);
        if (lineDirections == null) {
            return 0.0f;
        }
        int paragraphDirection = getParagraphDirection(i);
        TextLine textLineObtain = TextLine.obtain();
        TextPaint textPaint = this.mWorkPaint;
        textPaint.set(this.mPaint);
        textPaint.setHyphenEdit(getHyphen(i));
        textLineObtain.set(textPaint, this.mText, lineStart, lineEnd, paragraphDirection, lineDirections, lineContainsTab, tabStops);
        if (isJustificationRequired(i)) {
            textLineObtain.justify(getJustifyWidth(i));
        }
        float fMetrics = textLineObtain.metrics(null);
        TextLine.recycle(textLineObtain);
        return fMetrics;
    }

    private float getLineExtent(int i, TabStops tabStops, boolean z) {
        int lineStart = getLineStart(i);
        int lineEnd = z ? getLineEnd(i) : getLineVisibleEnd(i);
        boolean lineContainsTab = getLineContainsTab(i);
        Directions lineDirections = getLineDirections(i);
        int paragraphDirection = getParagraphDirection(i);
        TextLine textLineObtain = TextLine.obtain();
        TextPaint textPaint = this.mWorkPaint;
        textPaint.set(this.mPaint);
        textPaint.setHyphenEdit(getHyphen(i));
        textLineObtain.set(textPaint, this.mText, lineStart, lineEnd, paragraphDirection, lineDirections, lineContainsTab, tabStops);
        if (isJustificationRequired(i)) {
            textLineObtain.justify(getJustifyWidth(i));
        }
        float fMetrics = textLineObtain.metrics(null);
        TextLine.recycle(textLineObtain);
        return fMetrics;
    }

    public int getLineForVertical(int i) {
        int lineCount = getLineCount();
        int i2 = -1;
        while (lineCount - i2 > 1) {
            int i3 = (lineCount + i2) / 2;
            if (getLineTop(i3) > i) {
                lineCount = i3;
            } else {
                i2 = i3;
            }
        }
        if (i2 < 0) {
            return 0;
        }
        return i2;
    }

    public int getLineForOffset(int i) {
        int lineCount = getLineCount();
        int i2 = -1;
        while (lineCount - i2 > 1) {
            int i3 = (lineCount + i2) / 2;
            if (getLineStart(i3) > i) {
                lineCount = i3;
            } else {
                i2 = i3;
            }
        }
        if (i2 < 0) {
            return 0;
        }
        return i2;
    }

    public int getOffsetForHorizontal(int i, float f) {
        return getOffsetForHorizontal(i, f, true);
    }

    public int getOffsetForHorizontal(int i, float f, boolean z) {
        int i2;
        Directions directions;
        float fAbs;
        int i3;
        Layout layout = this;
        int lineEnd = getLineEnd(i);
        int lineStart = getLineStart(i);
        Directions lineDirections = getLineDirections(i);
        TextLine textLineObtain = TextLine.obtain();
        textLineObtain.set(layout.mPaint, layout.mText, lineStart, lineEnd, getParagraphDirection(i), lineDirections, false, null);
        HorizontalMeasurementProvider horizontalMeasurementProvider = layout.new HorizontalMeasurementProvider(i, z);
        int i4 = 1;
        if (i != getLineCount() - 1) {
            lineEnd = textLineObtain.getOffsetToLeftRightOf(lineEnd - lineStart, !layout.isRtlCharAt(lineEnd - 1)) + lineStart;
        }
        float fAbs2 = Math.abs(horizontalMeasurementProvider.get(lineStart) - f);
        int i5 = lineStart;
        int i6 = 0;
        while (i6 < lineDirections.mDirections.length) {
            int i7 = lineDirections.mDirections[i6] + lineStart;
            int i8 = i6 + 1;
            int i9 = (lineDirections.mDirections[i8] & RUN_LENGTH_MASK) + i7;
            boolean z2 = (lineDirections.mDirections[i8] & 67108864) != 0 ? i4 : 0;
            int i10 = z2 != 0 ? -1 : i4;
            if (i9 > lineEnd) {
                i9 = lineEnd;
            }
            int i11 = (i9 - 1) + i4;
            int i12 = i7 + 1;
            int i13 = i12 - 1;
            while (true) {
                i2 = i5;
                directions = lineDirections;
                if (i11 - i13 <= 1) {
                    break;
                }
                int i14 = (i11 + i13) / 2;
                float f2 = horizontalMeasurementProvider.get(layout.getOffsetAtStartOf(i14));
                float f3 = i10;
                if (f2 * f3 >= f3 * f) {
                    i11 = i14;
                } else {
                    i13 = i14;
                }
                i5 = i2;
                lineDirections = directions;
                layout = this;
            }
            if (i13 >= i12) {
                i12 = i13;
            }
            if (i12 < i9) {
                int offsetToLeftRightOf = textLineObtain.getOffsetToLeftRightOf(i12 - lineStart, z2) + lineStart;
                int offsetToLeftRightOf2 = textLineObtain.getOffsetToLeftRightOf(offsetToLeftRightOf - lineStart, !z2) + lineStart;
                if (offsetToLeftRightOf2 < i7 || offsetToLeftRightOf2 >= i9) {
                    fAbs = fAbs2;
                } else {
                    fAbs = Math.abs(horizontalMeasurementProvider.get(offsetToLeftRightOf2) - f);
                    if (offsetToLeftRightOf < i9) {
                        float fAbs3 = Math.abs(horizontalMeasurementProvider.get(offsetToLeftRightOf) - f);
                        if (fAbs3 < fAbs) {
                            fAbs = fAbs3;
                            i3 = offsetToLeftRightOf;
                        } else {
                            i3 = offsetToLeftRightOf2;
                        }
                        if (fAbs < fAbs2) {
                            i2 = i3;
                        }
                    }
                }
            }
            float fAbs4 = Math.abs(horizontalMeasurementProvider.get(i7) - f);
            if (fAbs4 < fAbs) {
                fAbs2 = fAbs4;
                i5 = i7;
            } else {
                fAbs2 = fAbs;
                i5 = i2;
            }
            i6 += 2;
            lineDirections = directions;
            layout = this;
            i4 = 1;
        }
        int i15 = i5;
        if (Math.abs(horizontalMeasurementProvider.get(lineEnd) - f) > fAbs2) {
            lineEnd = i15;
        }
        TextLine.recycle(textLineObtain);
        return lineEnd;
    }

    private class HorizontalMeasurementProvider {
        private float[] mHorizontals;
        private final int mLine;
        private int mLineStartOffset;
        private final boolean mPrimary;

        HorizontalMeasurementProvider(int i, boolean z) {
            this.mLine = i;
            this.mPrimary = z;
            init();
        }

        private void init() {
            if (Layout.this.getLineDirections(this.mLine) != Layout.DIRS_ALL_LEFT_TO_RIGHT) {
                this.mHorizontals = Layout.this.getLineHorizontals(this.mLine, false, this.mPrimary);
                this.mLineStartOffset = Layout.this.getLineStart(this.mLine);
            }
        }

        float get(int i) {
            if (this.mHorizontals == null || i < this.mLineStartOffset || i >= this.mLineStartOffset + this.mHorizontals.length) {
                return Layout.this.getHorizontal(i, this.mPrimary);
            }
            return this.mHorizontals[i - this.mLineStartOffset];
        }
    }

    public final int getLineEnd(int i) {
        return getLineStart(i + 1);
    }

    public int getLineVisibleEnd(int i) {
        return getLineVisibleEnd(i, getLineStart(i), getLineStart(i + 1));
    }

    private int getLineVisibleEnd(int i, int i2, int i3) {
        CharSequence charSequence = this.mText;
        if (i == getLineCount() - 1) {
            return i3;
        }
        while (i3 > i2) {
            int i4 = i3 - 1;
            char cCharAt = charSequence.charAt(i4);
            if (cCharAt == '\n') {
                return i4;
            }
            if (!TextLine.isLineEndSpace(cCharAt)) {
                break;
            }
            i3--;
        }
        return i3;
    }

    public final int getLineBottom(int i) {
        return getLineTop(i + 1);
    }

    public final int getLineBottomWithoutSpacing(int i) {
        return getLineTop(i + 1) - getLineExtra(i);
    }

    public final int getLineBaseline(int i) {
        return getLineTop(i + 1) - getLineDescent(i);
    }

    public final int getLineAscent(int i) {
        return getLineTop(i) - (getLineTop(i + 1) - getLineDescent(i));
    }

    public int getLineExtra(int i) {
        return 0;
    }

    public int getOffsetToLeftOf(int i) {
        return getOffsetToLeftRightOf(i, true);
    }

    public int getOffsetToRightOf(int i) {
        return getOffsetToLeftRightOf(i, false);
    }

    private int getOffsetToLeftRightOf(int i, boolean z) {
        int i2;
        int i3;
        boolean z2 = z;
        int lineForOffset = getLineForOffset(i);
        int lineStart = getLineStart(lineForOffset);
        int lineEnd = getLineEnd(lineForOffset);
        int paragraphDirection = getParagraphDirection(lineForOffset);
        boolean z3 = true;
        if (z2 == (paragraphDirection == -1)) {
            if (i == lineEnd) {
                if (lineForOffset >= getLineCount() - 1) {
                    return i;
                }
                lineForOffset++;
            } else {
                z3 = false;
            }
        } else if (i == lineStart) {
            if (lineForOffset <= 0) {
                return i;
            }
            lineForOffset--;
        }
        if (z3) {
            lineStart = getLineStart(lineForOffset);
            lineEnd = getLineEnd(lineForOffset);
            int paragraphDirection2 = getParagraphDirection(lineForOffset);
            if (paragraphDirection2 != paragraphDirection) {
                z2 = !z2;
                i2 = lineEnd;
                i3 = paragraphDirection2;
            } else {
                i2 = lineEnd;
                i3 = paragraphDirection;
            }
        }
        Directions lineDirections = getLineDirections(lineForOffset);
        TextLine textLineObtain = TextLine.obtain();
        textLineObtain.set(this.mPaint, this.mText, lineStart, i2, i3, lineDirections, false, null);
        int offsetToLeftRightOf = lineStart + textLineObtain.getOffsetToLeftRightOf(i - lineStart, z2);
        TextLine.recycle(textLineObtain);
        return offsetToLeftRightOf;
    }

    private int getOffsetAtStartOf(int i) {
        char cCharAt;
        if (i == 0) {
            return 0;
        }
        CharSequence charSequence = this.mText;
        char cCharAt2 = charSequence.charAt(i);
        if (cCharAt2 >= 56320 && cCharAt2 <= 57343 && (cCharAt = charSequence.charAt(i - 1)) >= 55296 && cCharAt <= 56319) {
            i--;
        }
        if (this.mSpannedText) {
            Spanned spanned = (Spanned) charSequence;
            ReplacementSpan[] replacementSpanArr = (ReplacementSpan[]) spanned.getSpans(i, i, ReplacementSpan.class);
            for (int i2 = 0; i2 < replacementSpanArr.length; i2++) {
                int spanStart = spanned.getSpanStart(replacementSpanArr[i2]);
                int spanEnd = spanned.getSpanEnd(replacementSpanArr[i2]);
                if (spanStart < i && spanEnd > i) {
                    i = spanStart;
                }
            }
        }
        return i;
    }

    public boolean shouldClampCursor(int i) {
        switch (getParagraphAlignment(i)) {
            case ALIGN_NORMAL:
                if (getParagraphDirection(i) <= 0) {
                    break;
                }
                break;
        }
        return false;
    }

    public void getCursorPath(int i, Path path, CharSequence charSequence) {
        path.reset();
        int lineForOffset = getLineForOffset(i);
        int lineTop = getLineTop(lineForOffset);
        int lineBottomWithoutSpacing = getLineBottomWithoutSpacing(lineForOffset);
        boolean zShouldClampCursor = shouldClampCursor(lineForOffset);
        float primaryHorizontal = getPrimaryHorizontal(i, zShouldClampCursor) - 0.5f;
        float secondaryHorizontal = isLevelBoundary(i) ? getSecondaryHorizontal(i, zShouldClampCursor) - 0.5f : primaryHorizontal;
        int metaState = TextKeyListener.getMetaState(charSequence, 1) | TextKeyListener.getMetaState(charSequence, 2048);
        int metaState2 = TextKeyListener.getMetaState(charSequence, 2);
        int i2 = 0;
        if (metaState != 0 || metaState2 != 0) {
            i2 = (lineBottomWithoutSpacing - lineTop) >> 2;
            if (metaState2 != 0) {
                lineTop += i2;
            }
            if (metaState != 0) {
                lineBottomWithoutSpacing -= i2;
            }
        }
        if (primaryHorizontal < 0.5f) {
            primaryHorizontal = 0.5f;
        }
        if (secondaryHorizontal < 0.5f) {
            secondaryHorizontal = 0.5f;
        }
        if (Float.compare(primaryHorizontal, secondaryHorizontal) == 0) {
            path.moveTo(primaryHorizontal, lineTop);
            path.lineTo(primaryHorizontal, lineBottomWithoutSpacing);
        } else {
            path.moveTo(primaryHorizontal, lineTop);
            float f = (lineTop + lineBottomWithoutSpacing) >> 1;
            path.lineTo(primaryHorizontal, f);
            path.moveTo(secondaryHorizontal, f);
            path.lineTo(secondaryHorizontal, lineBottomWithoutSpacing);
        }
        if (metaState == 2) {
            float f2 = lineBottomWithoutSpacing;
            path.moveTo(secondaryHorizontal, f2);
            float f3 = i2;
            float f4 = lineBottomWithoutSpacing + i2;
            path.lineTo(secondaryHorizontal - f3, f4);
            path.lineTo(secondaryHorizontal, f2);
            path.lineTo(secondaryHorizontal + f3, f4);
        } else if (metaState == 1) {
            float f5 = lineBottomWithoutSpacing;
            path.moveTo(secondaryHorizontal, f5);
            float f6 = i2;
            float f7 = secondaryHorizontal - f6;
            float f8 = lineBottomWithoutSpacing + i2;
            path.lineTo(f7, f8);
            float f9 = f8 - 0.5f;
            path.moveTo(f7, f9);
            float f10 = f6 + secondaryHorizontal;
            path.lineTo(f10, f9);
            path.moveTo(f10, f8);
            path.lineTo(secondaryHorizontal, f5);
        }
        if (metaState2 == 2) {
            float f11 = lineTop;
            path.moveTo(primaryHorizontal, f11);
            float f12 = i2;
            float f13 = lineTop - i2;
            path.lineTo(primaryHorizontal - f12, f13);
            path.lineTo(primaryHorizontal, f11);
            path.lineTo(primaryHorizontal + f12, f13);
            return;
        }
        if (metaState2 == 1) {
            float f14 = lineTop;
            path.moveTo(primaryHorizontal, f14);
            float f15 = i2;
            float f16 = primaryHorizontal - f15;
            float f17 = lineTop - i2;
            path.lineTo(f16, f17);
            float f18 = 0.5f + f17;
            path.moveTo(f16, f18);
            float f19 = f15 + primaryHorizontal;
            path.lineTo(f19, f18);
            path.moveTo(f19, f17);
            path.lineTo(primaryHorizontal, f14);
        }
    }

    private void addSelection(int i, int i2, int i3, int i4, int i5, SelectionRectangleConsumer selectionRectangleConsumer) {
        int iMax;
        int iMin;
        int lineStart = getLineStart(i);
        int lineEnd = getLineEnd(i);
        Directions lineDirections = getLineDirections(i);
        if (lineEnd > lineStart && this.mText.charAt(lineEnd - 1) == '\n') {
            lineEnd--;
        }
        for (int i6 = 0; i6 < lineDirections.mDirections.length; i6 += 2) {
            int i7 = lineDirections.mDirections[i6] + lineStart;
            int i8 = i6 + 1;
            int i9 = (lineDirections.mDirections[i8] & RUN_LENGTH_MASK) + i7;
            if (i9 > lineEnd) {
                i9 = lineEnd;
            }
            if (i2 <= i9 && i3 >= i7 && (iMax = Math.max(i2, i7)) != (iMin = Math.min(i3, i9))) {
                float horizontal = getHorizontal(iMax, false, i, false);
                float horizontal2 = getHorizontal(iMin, true, i, false);
                selectionRectangleConsumer.accept(Math.min(horizontal, horizontal2), i4, Math.max(horizontal, horizontal2), i5, (lineDirections.mDirections[i8] & 67108864) != 0 ? 0 : 1);
            }
        }
    }

    public void getSelectionPath(int i, int i2, final Path path) {
        path.reset();
        getSelection(i, i2, new SelectionRectangleConsumer() {
            @Override
            public final void accept(float f, float f2, float f3, float f4, int i3) {
                path.addRect(f, f2, f3, f4, Path.Direction.CW);
            }
        });
    }

    public final void getSelection(int i, int i2, SelectionRectangleConsumer selectionRectangleConsumer) {
        int i3;
        int i4;
        float f;
        if (i == i2) {
            return;
        }
        if (i2 < i) {
            i4 = i;
            i3 = i2;
        } else {
            i3 = i;
            i4 = i2;
        }
        int lineForOffset = getLineForOffset(i3);
        int lineForOffset2 = getLineForOffset(i4);
        int lineTop = getLineTop(lineForOffset);
        int lineBottomWithoutSpacing = getLineBottomWithoutSpacing(lineForOffset2);
        if (lineForOffset == lineForOffset2) {
            addSelection(lineForOffset, i3, i4, lineTop, lineBottomWithoutSpacing, selectionRectangleConsumer);
            return;
        }
        float f2 = this.mWidth;
        addSelection(lineForOffset, i3, getLineEnd(lineForOffset), lineTop, getLineBottom(lineForOffset), selectionRectangleConsumer);
        if (getParagraphDirection(lineForOffset) == -1) {
            selectionRectangleConsumer.accept(getLineLeft(lineForOffset), lineTop, 0.0f, getLineBottom(lineForOffset), 0);
            f = f2;
        } else {
            f = f2;
            selectionRectangleConsumer.accept(getLineRight(lineForOffset), lineTop, f2, getLineBottom(lineForOffset), 1);
        }
        while (true) {
            lineForOffset++;
            if (lineForOffset >= lineForOffset2) {
                break;
            }
            int lineTop2 = getLineTop(lineForOffset);
            int lineBottom = getLineBottom(lineForOffset);
            if (getParagraphDirection(lineForOffset) == -1) {
                selectionRectangleConsumer.accept(0.0f, lineTop2, f, lineBottom, 0);
            } else {
                selectionRectangleConsumer.accept(0.0f, lineTop2, f, lineBottom, 1);
            }
        }
        int lineTop3 = getLineTop(lineForOffset2);
        int lineBottomWithoutSpacing2 = getLineBottomWithoutSpacing(lineForOffset2);
        addSelection(lineForOffset2, getLineStart(lineForOffset2), i4, lineTop3, lineBottomWithoutSpacing2, selectionRectangleConsumer);
        if (getParagraphDirection(lineForOffset2) == -1) {
            selectionRectangleConsumer.accept(f, lineTop3, getLineRight(lineForOffset2), lineBottomWithoutSpacing2, 0);
        } else {
            selectionRectangleConsumer.accept(0.0f, lineTop3, getLineLeft(lineForOffset2), lineBottomWithoutSpacing2, 1);
        }
    }

    public final Alignment getParagraphAlignment(int i) {
        AlignmentSpan[] alignmentSpanArr;
        int length;
        Alignment alignment = this.mAlignment;
        if (this.mSpannedText && (length = (alignmentSpanArr = (AlignmentSpan[]) getParagraphSpans((Spanned) this.mText, getLineStart(i), getLineEnd(i), AlignmentSpan.class)).length) > 0) {
            return alignmentSpanArr[length - 1].getAlignment();
        }
        return alignment;
    }

    public final int getParagraphLeft(int i) {
        if (getParagraphDirection(i) == -1 || !this.mSpannedText) {
            return 0;
        }
        return getParagraphLeadingMargin(i);
    }

    public final int getParagraphRight(int i) {
        int i2 = this.mWidth;
        if (getParagraphDirection(i) == 1 || !this.mSpannedText) {
            return i2;
        }
        return i2 - getParagraphLeadingMargin(i);
    }

    private int getParagraphLeadingMargin(int i) {
        if (!this.mSpannedText) {
            return 0;
        }
        Spanned spanned = (Spanned) this.mText;
        int lineStart = getLineStart(i);
        LeadingMarginSpan[] leadingMarginSpanArr = (LeadingMarginSpan[]) getParagraphSpans(spanned, lineStart, spanned.nextSpanTransition(lineStart, getLineEnd(i), LeadingMarginSpan.class), LeadingMarginSpan.class);
        if (leadingMarginSpanArr.length == 0) {
            return 0;
        }
        boolean z = lineStart == 0 || spanned.charAt(lineStart - 1) == '\n';
        for (int i2 = 0; i2 < leadingMarginSpanArr.length; i2++) {
            if (leadingMarginSpanArr[i2] instanceof LeadingMarginSpan.LeadingMarginSpan2) {
                z |= i < getLineForOffset(spanned.getSpanStart(leadingMarginSpanArr[i2])) + ((LeadingMarginSpan.LeadingMarginSpan2) leadingMarginSpanArr[i2]).getLeadingMarginLineCount();
            }
        }
        int leadingMargin = 0;
        for (LeadingMarginSpan leadingMarginSpan : leadingMarginSpanArr) {
            leadingMargin += leadingMarginSpan.getLeadingMargin(z);
        }
        return leadingMargin;
    }

    private static float measurePara(TextPaint textPaint, CharSequence charSequence, int i, int i2, TextDirectionHeuristic textDirectionHeuristic) throws Throwable {
        MeasuredParagraph measuredParagraphBuildForBidi;
        int leadingMargin;
        TabStops tabStops;
        boolean z;
        TabStops tabStops2;
        TextLine textLineObtain = TextLine.obtain();
        try {
            measuredParagraphBuildForBidi = MeasuredParagraph.buildForBidi(charSequence, i, i2, textDirectionHeuristic, null);
            try {
                char[] chars = measuredParagraphBuildForBidi.getChars();
                int length = chars.length;
                Directions directions = measuredParagraphBuildForBidi.getDirections(0, length);
                int paragraphDir = measuredParagraphBuildForBidi.getParagraphDir();
                if (charSequence instanceof Spanned) {
                    leadingMargin = 0;
                    for (LeadingMarginSpan leadingMarginSpan : (LeadingMarginSpan[]) getParagraphSpans((Spanned) charSequence, i, i2, LeadingMarginSpan.class)) {
                        leadingMargin += leadingMarginSpan.getLeadingMargin(true);
                    }
                } else {
                    leadingMargin = 0;
                }
                int i3 = 0;
                while (true) {
                    if (i3 < length) {
                        if (chars[i3] != '\t') {
                            i3++;
                        } else if (!(charSequence instanceof Spanned)) {
                            z = true;
                            tabStops = null;
                        } else {
                            Spanned spanned = (Spanned) charSequence;
                            TabStopSpan[] tabStopSpanArr = (TabStopSpan[]) getParagraphSpans(spanned, i, spanned.nextSpanTransition(i, i2, TabStopSpan.class), TabStopSpan.class);
                            if (tabStopSpanArr.length > 0) {
                                tabStops2 = new TabStops(20, tabStopSpanArr);
                            } else {
                                tabStops2 = null;
                            }
                            z = true;
                            tabStops = tabStops2;
                        }
                    } else {
                        tabStops = null;
                        z = false;
                        break;
                    }
                }
                textLineObtain.set(textPaint, charSequence, i, i2, paragraphDir, directions, z, tabStops);
                float fAbs = leadingMargin + Math.abs(textLineObtain.metrics(null));
                TextLine.recycle(textLineObtain);
                if (measuredParagraphBuildForBidi != null) {
                    measuredParagraphBuildForBidi.recycle();
                }
                return fAbs;
            } catch (Throwable th) {
                th = th;
                TextLine.recycle(textLineObtain);
                if (measuredParagraphBuildForBidi != null) {
                    measuredParagraphBuildForBidi.recycle();
                }
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
            measuredParagraphBuildForBidi = null;
        }
    }

    static class TabStops {
        private int mIncrement;
        private int mNumStops;
        private int[] mStops;

        TabStops(int i, Object[] objArr) {
            reset(i, objArr);
        }

        void reset(int i, Object[] objArr) {
            this.mIncrement = i;
            int i2 = 0;
            if (objArr != null) {
                int i3 = 0;
                int[] iArr = this.mStops;
                for (Object obj : objArr) {
                    if (obj instanceof TabStopSpan) {
                        if (iArr == null) {
                            iArr = new int[10];
                        } else if (i3 == iArr.length) {
                            int[] iArr2 = new int[i3 * 2];
                            for (int i4 = 0; i4 < i3; i4++) {
                                iArr2[i4] = iArr[i4];
                            }
                            iArr = iArr2;
                        }
                        iArr[i3] = ((TabStopSpan) obj).getTabStop();
                        i3++;
                    }
                }
                if (i3 > 1) {
                    Arrays.sort(iArr, 0, i3);
                }
                if (iArr != this.mStops) {
                    this.mStops = iArr;
                }
                i2 = i3;
            }
            this.mNumStops = i2;
        }

        float nextTab(float f) {
            int i = this.mNumStops;
            if (i > 0) {
                int[] iArr = this.mStops;
                for (int i2 = 0; i2 < i; i2++) {
                    float f2 = iArr[i2];
                    if (f2 > f) {
                        return f2;
                    }
                }
            }
            return nextDefaultStop(f, this.mIncrement);
        }

        public static float nextDefaultStop(float f, int i) {
            float f2 = i;
            return ((int) ((f + f2) / f2)) * i;
        }
    }

    static float nextTab(CharSequence charSequence, int i, int i2, float f, Object[] objArr) {
        boolean z;
        if (charSequence instanceof Spanned) {
            if (objArr != null) {
                z = false;
            } else {
                objArr = getParagraphSpans((Spanned) charSequence, i, i2, TabStopSpan.class);
                z = true;
            }
            float f2 = Float.MAX_VALUE;
            for (int i3 = 0; i3 < objArr.length; i3++) {
                if (z || (objArr[i3] instanceof TabStopSpan)) {
                    float tabStop = ((TabStopSpan) objArr[i3]).getTabStop();
                    if (tabStop < f2 && tabStop > f) {
                        f2 = tabStop;
                    }
                }
            }
            if (f2 != Float.MAX_VALUE) {
                return f2;
            }
        }
        return ((int) ((f + 20.0f) / 20.0f)) * 20;
    }

    protected final boolean isSpanned() {
        return this.mSpannedText;
    }

    static <T> T[] getParagraphSpans(Spanned spanned, int i, int i2, Class<T> cls) {
        if (i == i2 && i > 0) {
            return (T[]) ArrayUtils.emptyArray(cls);
        }
        if (spanned instanceof SpannableStringBuilder) {
            return (T[]) ((SpannableStringBuilder) spanned).getSpans(i, i2, cls, false);
        }
        return (T[]) spanned.getSpans(i, i2, cls);
    }

    private void ellipsize(int i, int i2, int i3, char[] cArr, int i4, TextUtils.TruncateAt truncateAt) {
        char cCharAt;
        int ellipsisCount = getEllipsisCount(i3);
        if (ellipsisCount == 0) {
            return;
        }
        int ellipsisStart = getEllipsisStart(i3);
        int lineStart = getLineStart(i3);
        String ellipsisString = TextUtils.getEllipsisString(truncateAt);
        int length = ellipsisString.length();
        boolean z = ellipsisCount >= length;
        int iMin = Math.min(ellipsisCount, (i2 - ellipsisStart) - lineStart);
        for (int iMax = Math.max(0, (i - ellipsisStart) - lineStart); iMax < iMin; iMax++) {
            if (z && iMax < length) {
                cCharAt = ellipsisString.charAt(iMax);
            } else {
                cCharAt = 65279;
            }
            cArr[(((iMax + ellipsisStart) + lineStart) + i4) - i] = cCharAt;
        }
    }

    public static class Directions {

        @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
        public int[] mDirections;

        @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
        public Directions(int[] iArr) {
            this.mDirections = iArr;
        }
    }

    static class Ellipsizer implements CharSequence, GetChars {
        Layout mLayout;
        TextUtils.TruncateAt mMethod;
        CharSequence mText;
        int mWidth;

        public Ellipsizer(CharSequence charSequence) {
            this.mText = charSequence;
        }

        @Override
        public char charAt(int i) {
            char[] cArrObtain = TextUtils.obtain(1);
            getChars(i, i + 1, cArrObtain, 0);
            char c = cArrObtain[0];
            TextUtils.recycle(cArrObtain);
            return c;
        }

        @Override
        public void getChars(int i, int i2, char[] cArr, int i3) {
            int lineForOffset = this.mLayout.getLineForOffset(i2);
            TextUtils.getChars(this.mText, i, i2, cArr, i3);
            for (int lineForOffset2 = this.mLayout.getLineForOffset(i); lineForOffset2 <= lineForOffset; lineForOffset2++) {
                this.mLayout.ellipsize(i, i2, lineForOffset2, cArr, i3, this.mMethod);
            }
        }

        @Override
        public int length() {
            return this.mText.length();
        }

        @Override
        public CharSequence subSequence(int i, int i2) {
            char[] cArr = new char[i2 - i];
            getChars(i, i2, cArr, 0);
            return new String(cArr);
        }

        @Override
        public String toString() {
            char[] cArr = new char[length()];
            getChars(0, length(), cArr, 0);
            return new String(cArr);
        }
    }

    static class SpannedEllipsizer extends Ellipsizer implements Spanned {
        private Spanned mSpanned;

        public SpannedEllipsizer(CharSequence charSequence) {
            super(charSequence);
            this.mSpanned = (Spanned) charSequence;
        }

        @Override
        public <T> T[] getSpans(int i, int i2, Class<T> cls) {
            return (T[]) this.mSpanned.getSpans(i, i2, cls);
        }

        @Override
        public int getSpanStart(Object obj) {
            return this.mSpanned.getSpanStart(obj);
        }

        @Override
        public int getSpanEnd(Object obj) {
            return this.mSpanned.getSpanEnd(obj);
        }

        @Override
        public int getSpanFlags(Object obj) {
            return this.mSpanned.getSpanFlags(obj);
        }

        @Override
        public int nextSpanTransition(int i, int i2, Class cls) {
            return this.mSpanned.nextSpanTransition(i, i2, cls);
        }

        @Override
        public CharSequence subSequence(int i, int i2) {
            char[] cArr = new char[i2 - i];
            getChars(i, i2, cArr, 0);
            SpannableString spannableString = new SpannableString(new String(cArr));
            TextUtils.copySpansFrom(this.mSpanned, i, i2, Object.class, spannableString, 0);
            return spannableString;
        }
    }
}
