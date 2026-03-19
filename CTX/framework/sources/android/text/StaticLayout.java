package android.text;

import android.graphics.Paint;
import android.text.AutoGrowArray;
import android.text.Layout;
import android.text.PrecomputedText;
import android.text.TextUtils;
import android.text.style.LeadingMarginSpan;
import android.text.style.LineHeightSpan;
import android.text.style.TabStopSpan;
import android.util.Log;
import android.util.Pools;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.GrowingArrayUtils;
import dalvik.annotation.optimization.CriticalNative;
import dalvik.annotation.optimization.FastNative;
import java.util.Arrays;

public class StaticLayout extends Layout {
    private static final char CHAR_NEW_LINE = '\n';
    private static final int COLUMNS_ELLIPSIZE = 7;
    private static final int COLUMNS_NORMAL = 5;
    private static final int DEFAULT_MAX_LINE_HEIGHT = -1;
    private static final int DESCENT = 2;
    private static final int DIR = 0;
    private static final int DIR_SHIFT = 30;
    private static final int ELLIPSIS_COUNT = 6;
    private static final int ELLIPSIS_START = 5;
    private static final int EXTRA = 3;
    private static final double EXTRA_ROUNDING = 0.5d;
    private static final int HYPHEN = 4;
    private static final int HYPHEN_MASK = 255;
    private static final int START = 0;
    private static final int START_MASK = 536870911;
    private static final int TAB = 0;
    private static final int TAB_INCREMENT = 20;
    private static final int TAB_MASK = 536870912;
    static final String TAG = "StaticLayout";
    private static final int TOP = 1;
    private int mBottomPadding;
    private int mColumns;
    private boolean mEllipsized;
    private int mEllipsizedWidth;
    private int[] mLeftIndents;
    private int[] mLeftPaddings;
    private int mLineCount;
    private Layout.Directions[] mLineDirections;
    private int[] mLines;
    private int mMaxLineHeight;
    private int mMaximumVisibleLineCount;
    private int[] mRightIndents;
    private int[] mRightPaddings;
    private int mTopPadding;

    private static native int nComputeLineBreaks(long j, char[] cArr, long j2, int i, float f, int i2, float f2, int[] iArr, int i3, int i4, LineBreaks lineBreaks, int i5, int[] iArr2, float[] fArr, float[] fArr2, float[] fArr3, int[] iArr3, float[] fArr4);

    @CriticalNative
    private static native void nFinish(long j);

    @FastNative
    private static native long nInit(int i, int i2, boolean z, int[] iArr, int[] iArr2, int[] iArr3);

    public static final class Builder {
        private static final Pools.SynchronizedPool<Builder> sPool = new Pools.SynchronizedPool<>(3);
        private boolean mAddLastLineLineSpacing;
        private Layout.Alignment mAlignment;
        private int mBreakStrategy;
        private TextUtils.TruncateAt mEllipsize;
        private int mEllipsizedWidth;
        private int mEnd;
        private boolean mFallbackLineSpacing;
        private final Paint.FontMetricsInt mFontMetricsInt = new Paint.FontMetricsInt();
        private int mHyphenationFrequency;
        private boolean mIncludePad;
        private int mJustificationMode;
        private int[] mLeftIndents;
        private int[] mLeftPaddings;
        private int mMaxLines;
        private TextPaint mPaint;
        private int[] mRightIndents;
        private int[] mRightPaddings;
        private float mSpacingAdd;
        private float mSpacingMult;
        private int mStart;
        private CharSequence mText;
        private TextDirectionHeuristic mTextDir;
        private int mWidth;

        private Builder() {
        }

        public static Builder obtain(CharSequence charSequence, int i, int i2, TextPaint textPaint, int i3) {
            Builder builderAcquire = sPool.acquire();
            if (builderAcquire == null) {
                builderAcquire = new Builder();
            }
            builderAcquire.mText = charSequence;
            builderAcquire.mStart = i;
            builderAcquire.mEnd = i2;
            builderAcquire.mPaint = textPaint;
            builderAcquire.mWidth = i3;
            builderAcquire.mAlignment = Layout.Alignment.ALIGN_NORMAL;
            builderAcquire.mTextDir = TextDirectionHeuristics.FIRSTSTRONG_LTR;
            builderAcquire.mSpacingMult = 1.0f;
            builderAcquire.mSpacingAdd = 0.0f;
            builderAcquire.mIncludePad = true;
            builderAcquire.mFallbackLineSpacing = false;
            builderAcquire.mEllipsizedWidth = i3;
            builderAcquire.mEllipsize = null;
            builderAcquire.mMaxLines = Integer.MAX_VALUE;
            builderAcquire.mBreakStrategy = 0;
            builderAcquire.mHyphenationFrequency = 0;
            builderAcquire.mJustificationMode = 0;
            return builderAcquire;
        }

        private static void recycle(Builder builder) {
            builder.mPaint = null;
            builder.mText = null;
            builder.mLeftIndents = null;
            builder.mRightIndents = null;
            builder.mLeftPaddings = null;
            builder.mRightPaddings = null;
            sPool.release(builder);
        }

        void finish() {
            this.mText = null;
            this.mPaint = null;
            this.mLeftIndents = null;
            this.mRightIndents = null;
            this.mLeftPaddings = null;
            this.mRightPaddings = null;
        }

        public Builder setText(CharSequence charSequence) {
            return setText(charSequence, 0, charSequence.length());
        }

        public Builder setText(CharSequence charSequence, int i, int i2) {
            this.mText = charSequence;
            this.mStart = i;
            this.mEnd = i2;
            return this;
        }

        public Builder setPaint(TextPaint textPaint) {
            this.mPaint = textPaint;
            return this;
        }

        public Builder setWidth(int i) {
            this.mWidth = i;
            if (this.mEllipsize == null) {
                this.mEllipsizedWidth = i;
            }
            return this;
        }

        public Builder setAlignment(Layout.Alignment alignment) {
            this.mAlignment = alignment;
            return this;
        }

        public Builder setTextDirection(TextDirectionHeuristic textDirectionHeuristic) {
            this.mTextDir = textDirectionHeuristic;
            return this;
        }

        public Builder setLineSpacing(float f, float f2) {
            this.mSpacingAdd = f;
            this.mSpacingMult = f2;
            return this;
        }

        public Builder setIncludePad(boolean z) {
            this.mIncludePad = z;
            return this;
        }

        public Builder setUseLineSpacingFromFallbacks(boolean z) {
            this.mFallbackLineSpacing = z;
            return this;
        }

        public Builder setEllipsizedWidth(int i) {
            this.mEllipsizedWidth = i;
            return this;
        }

        public Builder setEllipsize(TextUtils.TruncateAt truncateAt) {
            this.mEllipsize = truncateAt;
            return this;
        }

        public Builder setMaxLines(int i) {
            this.mMaxLines = i;
            return this;
        }

        public Builder setBreakStrategy(int i) {
            this.mBreakStrategy = i;
            return this;
        }

        public Builder setHyphenationFrequency(int i) {
            this.mHyphenationFrequency = i;
            return this;
        }

        public Builder setIndents(int[] iArr, int[] iArr2) {
            this.mLeftIndents = iArr;
            this.mRightIndents = iArr2;
            return this;
        }

        public Builder setAvailablePaddings(int[] iArr, int[] iArr2) {
            this.mLeftPaddings = iArr;
            this.mRightPaddings = iArr2;
            return this;
        }

        public Builder setJustificationMode(int i) {
            this.mJustificationMode = i;
            return this;
        }

        Builder setAddLastLineLineSpacing(boolean z) {
            this.mAddLastLineLineSpacing = z;
            return this;
        }

        public StaticLayout build() {
            StaticLayout staticLayout = new StaticLayout(this);
            recycle(this);
            return staticLayout;
        }
    }

    @Deprecated
    public StaticLayout(CharSequence charSequence, TextPaint textPaint, int i, Layout.Alignment alignment, float f, float f2, boolean z) {
        this(charSequence, 0, charSequence.length(), textPaint, i, alignment, f, f2, z);
    }

    @Deprecated
    public StaticLayout(CharSequence charSequence, int i, int i2, TextPaint textPaint, int i3, Layout.Alignment alignment, float f, float f2, boolean z) {
        this(charSequence, i, i2, textPaint, i3, alignment, f, f2, z, null, 0);
    }

    @Deprecated
    public StaticLayout(CharSequence charSequence, int i, int i2, TextPaint textPaint, int i3, Layout.Alignment alignment, float f, float f2, boolean z, TextUtils.TruncateAt truncateAt, int i4) {
        this(charSequence, i, i2, textPaint, i3, alignment, TextDirectionHeuristics.FIRSTSTRONG_LTR, f, f2, z, truncateAt, i4, Integer.MAX_VALUE);
    }

    @Deprecated
    public StaticLayout(CharSequence charSequence, int i, int i2, TextPaint textPaint, int i3, Layout.Alignment alignment, TextDirectionHeuristic textDirectionHeuristic, float f, float f2, boolean z, TextUtils.TruncateAt truncateAt, int i4, int i5) {
        CharSequence spannedEllipsizer;
        if (truncateAt == null) {
            spannedEllipsizer = charSequence;
        } else {
            spannedEllipsizer = charSequence instanceof Spanned ? new Layout.SpannedEllipsizer(charSequence) : new Layout.Ellipsizer(charSequence);
        }
        super(spannedEllipsizer, textPaint, i3, alignment, textDirectionHeuristic, f, f2);
        this.mMaxLineHeight = -1;
        this.mMaximumVisibleLineCount = Integer.MAX_VALUE;
        Builder maxLines = Builder.obtain(charSequence, i, i2, textPaint, i3).setAlignment(alignment).setTextDirection(textDirectionHeuristic).setLineSpacing(f2, f).setIncludePad(z).setEllipsizedWidth(i4).setEllipsize(truncateAt).setMaxLines(i5);
        if (truncateAt != null) {
            Layout.Ellipsizer ellipsizer = (Layout.Ellipsizer) getText();
            ellipsizer.mLayout = this;
            ellipsizer.mWidth = i4;
            ellipsizer.mMethod = truncateAt;
            this.mEllipsizedWidth = i4;
            this.mColumns = 7;
        } else {
            this.mColumns = 5;
            this.mEllipsizedWidth = i3;
        }
        this.mLineDirections = (Layout.Directions[]) ArrayUtils.newUnpaddedArray(Layout.Directions.class, 2);
        this.mLines = ArrayUtils.newUnpaddedIntArray(2 * this.mColumns);
        this.mMaximumVisibleLineCount = i5;
        generate(maxLines, maxLines.mIncludePad, maxLines.mIncludePad);
        Builder.recycle(maxLines);
    }

    StaticLayout(CharSequence charSequence) {
        super(charSequence, null, 0, null, 0.0f, 0.0f);
        this.mMaxLineHeight = -1;
        this.mMaximumVisibleLineCount = Integer.MAX_VALUE;
        this.mColumns = 7;
        this.mLineDirections = (Layout.Directions[]) ArrayUtils.newUnpaddedArray(Layout.Directions.class, 2);
        this.mLines = ArrayUtils.newUnpaddedIntArray(2 * this.mColumns);
    }

    private StaticLayout(Builder builder) {
        CharSequence ellipsizer;
        if (builder.mEllipsize == null) {
            ellipsizer = builder.mText;
        } else if (builder.mText instanceof Spanned) {
            ellipsizer = new Layout.SpannedEllipsizer(builder.mText);
        } else {
            ellipsizer = new Layout.Ellipsizer(builder.mText);
        }
        super(ellipsizer, builder.mPaint, builder.mWidth, builder.mAlignment, builder.mTextDir, builder.mSpacingMult, builder.mSpacingAdd);
        this.mMaxLineHeight = -1;
        this.mMaximumVisibleLineCount = Integer.MAX_VALUE;
        if (builder.mEllipsize != null) {
            Layout.Ellipsizer ellipsizer2 = (Layout.Ellipsizer) getText();
            ellipsizer2.mLayout = this;
            ellipsizer2.mWidth = builder.mEllipsizedWidth;
            ellipsizer2.mMethod = builder.mEllipsize;
            this.mEllipsizedWidth = builder.mEllipsizedWidth;
            this.mColumns = 7;
        } else {
            this.mColumns = 5;
            this.mEllipsizedWidth = builder.mWidth;
        }
        this.mLineDirections = (Layout.Directions[]) ArrayUtils.newUnpaddedArray(Layout.Directions.class, 2);
        this.mLines = ArrayUtils.newUnpaddedIntArray(2 * this.mColumns);
        this.mMaximumVisibleLineCount = builder.mMaxLines;
        this.mLeftIndents = builder.mLeftIndents;
        this.mRightIndents = builder.mRightIndents;
        this.mLeftPaddings = builder.mLeftPaddings;
        this.mRightPaddings = builder.mRightPaddings;
        setJustificationMode(builder.mJustificationMode);
        generate(builder, builder.mIncludePad, builder.mIncludePad);
    }

    void generate(Builder builder, boolean z, boolean z2) {
        int[] iArr;
        Paint.FontMetricsInt fontMetricsInt;
        long j;
        AutoGrowArray.FloatArray floatArray;
        LineBreaks lineBreaks;
        TextUtils.TruncateAt truncateAt;
        float f;
        boolean z3;
        Spanned spanned;
        PrecomputedText.ParagraphInfo[] paragraphInfo;
        PrecomputedText.ParagraphInfo[] paragraphInfoArr;
        int i;
        ?? r1;
        ?? r2;
        ?? r0;
        long j2;
        ?? r5;
        ?? r72;
        TextDirectionHeuristic textDirectionHeuristic;
        TextPaint textPaint;
        int i2;
        int i3;
        CharSequence charSequence;
        float f2;
        TextUtils.TruncateAt truncateAt2;
        Paint.FontMetricsInt fontMetricsInt2;
        long j3;
        int i4;
        CharSequence charSequence2;
        long j4;
        TextDirectionHeuristic textDirectionHeuristic2;
        int i5;
        ?? r57;
        int i6;
        int i7;
        int i8;
        LineHeightSpan[] lineHeightSpanArr;
        ?? r572;
        int[] iArr2;
        LineBreaks lineBreaks2;
        TextUtils.TruncateAt truncateAt3;
        boolean z4;
        TextUtils.TruncateAt truncateAt4;
        int i9;
        int i10;
        int i11;
        AutoGrowArray.FloatArray floatArray2;
        Spanned spanned2;
        boolean z5;
        PrecomputedText.ParagraphInfo[] paragraphInfoArr2;
        LineBreaks lineBreaks3;
        TextUtils.TruncateAt truncateAt5;
        boolean z6;
        ?? r02 = this;
        CharSequence charSequence3 = builder.mText;
        int i12 = builder.mStart;
        int i13 = builder.mEnd;
        TextPaint textPaint2 = builder.mPaint;
        int i14 = builder.mWidth;
        TextDirectionHeuristic textDirectionHeuristic3 = builder.mTextDir;
        boolean z7 = builder.mFallbackLineSpacing;
        float f3 = builder.mSpacingMult;
        float f4 = builder.mSpacingAdd;
        float f5 = builder.mEllipsizedWidth;
        TextUtils.TruncateAt truncateAt6 = builder.mEllipsize;
        boolean z8 = builder.mAddLastLineLineSpacing;
        LineBreaks lineBreaks4 = new LineBreaks();
        AutoGrowArray.FloatArray floatArray3 = new AutoGrowArray.FloatArray();
        r02.mLineCount = 0;
        r02.mEllipsized = false;
        r02.mMaxLineHeight = r02.mMaximumVisibleLineCount < 1 ? 0 : -1;
        boolean z9 = (f3 == 1.0f && f4 == 0.0f) ? false : true;
        Paint.FontMetricsInt fontMetricsInt3 = builder.mFontMetricsInt;
        if (r02.mLeftIndents == null && r02.mRightIndents == null) {
            iArr = null;
        } else {
            int length = r02.mLeftIndents == null ? 0 : r02.mLeftIndents.length;
            int length2 = r02.mRightIndents == null ? 0 : r02.mRightIndents.length;
            int[] iArr3 = new int[Math.max(length, length2)];
            for (int i15 = 0; i15 < length; i15++) {
                iArr3[i15] = r02.mLeftIndents[i15];
            }
            for (int i16 = 0; i16 < length2; i16++) {
                iArr3[i16] = iArr3[i16] + r02.mRightIndents[i16];
            }
            iArr = iArr3;
        }
        long jNInit = nInit(builder.mBreakStrategy, builder.mHyphenationFrequency, builder.mJustificationMode != 0, iArr, r02.mLeftPaddings, r02.mRightPaddings);
        Spanned spanned3 = charSequence3 instanceof Spanned ? (Spanned) charSequence3 : null;
        if (charSequence3 instanceof PrecomputedText) {
            PrecomputedText precomputedText = (PrecomputedText) charSequence3;
            f = f5;
            truncateAt = truncateAt6;
            spanned = spanned3;
            fontMetricsInt = fontMetricsInt3;
            j = jNInit;
            lineBreaks = lineBreaks4;
            z3 = false;
            floatArray = floatArray3;
            paragraphInfo = precomputedText.canUseMeasuredResult(i12, i13, textDirectionHeuristic3, textPaint2, builder.mBreakStrategy, builder.mHyphenationFrequency) ? precomputedText.getParagraphInfo() : null;
            if (paragraphInfo == null) {
                paragraphInfo = PrecomputedText.createMeasuredParagraphs(charSequence3, new PrecomputedText.Params(textPaint2, textDirectionHeuristic3, builder.mBreakStrategy, builder.mHyphenationFrequency), i12, i13, z3);
            }
            paragraphInfoArr = paragraphInfo;
            boolean z10 = z3;
            i = z10 ? 1 : 0;
            r1 = 0;
            r0 = r02;
            r2 = z10;
            while (true) {
                try {
                } catch (Throwable th) {
                    th = th;
                    j2 = j;
                }
                if (i < paragraphInfoArr.length) {
                    r5 = r0;
                    r72 = r2;
                    textDirectionHeuristic = textDirectionHeuristic3;
                    textPaint = textPaint2;
                    i2 = i13;
                    i3 = i12;
                    charSequence = charSequence3;
                    f2 = f;
                    truncateAt2 = truncateAt;
                    fontMetricsInt2 = fontMetricsInt;
                    j3 = j;
                    break;
                }
                int i17 = i == 0 ? i12 : paragraphInfoArr[i - 1].paragraphEnd;
                int i18 = paragraphInfoArr[i].paragraphEnd;
                if (spanned != null) {
                    LeadingMarginSpan[] leadingMarginSpanArr = (LeadingMarginSpan[]) getParagraphSpans(spanned, i17, i18, LeadingMarginSpan.class);
                    int leadingMargin = i14;
                    int leadingMargin2 = leadingMargin;
                    int iMax = 1;
                    for (?? r4 = z3; r4 < leadingMarginSpanArr.length; r4++) {
                        LeadingMarginSpan leadingMarginSpan = leadingMarginSpanArr[r4];
                        TextDirectionHeuristic textDirectionHeuristic4 = textDirectionHeuristic3;
                        int i19 = i12;
                        leadingMargin -= leadingMarginSpanArr[r4].getLeadingMargin(true);
                        leadingMargin2 -= leadingMarginSpanArr[r4].getLeadingMargin(false);
                        if (leadingMarginSpan instanceof LeadingMarginSpan.LeadingMarginSpan2) {
                            iMax = Math.max(iMax, ((LeadingMarginSpan.LeadingMarginSpan2) leadingMarginSpan).getLeadingMarginLineCount());
                        }
                        textDirectionHeuristic3 = textDirectionHeuristic4;
                        i12 = i19;
                    }
                    textDirectionHeuristic2 = textDirectionHeuristic3;
                    i5 = i12;
                    int i20 = iMax;
                    LineHeightSpan[] lineHeightSpanArr2 = (LineHeightSpan[]) getParagraphSpans(spanned, i17, i18, LineHeightSpan.class);
                    if (lineHeightSpanArr2.length != 0) {
                        if (r1 != 0) {
                            int length3 = r1.length;
                            ?? NewUnpaddedIntArray = r1;
                            if (length3 < lineHeightSpanArr2.length) {
                                NewUnpaddedIntArray = ArrayUtils.newUnpaddedIntArray(lineHeightSpanArr2.length);
                            }
                            for (int i21 = 0; i21 < lineHeightSpanArr2.length; i21++) {
                                int spanStart = spanned.getSpanStart(lineHeightSpanArr2[i21]);
                                if (spanStart < i17) {
                                    NewUnpaddedIntArray[i21] = r0.getLineTop(r0.getLineForOffset(spanStart));
                                } else {
                                    NewUnpaddedIntArray[i21] = r2;
                                }
                            }
                            r572 = NewUnpaddedIntArray;
                            lineHeightSpanArr = lineHeightSpanArr2;
                            i8 = i20;
                            i6 = leadingMargin;
                            i7 = leadingMargin2;
                            if (spanned == null) {
                                TabStopSpan[] tabStopSpanArr = (TabStopSpan[]) getParagraphSpans(spanned, i17, i18, TabStopSpan.class);
                                if (tabStopSpanArr.length > 0) {
                                    int[] iArr4 = new int[tabStopSpanArr.length];
                                    for (int i22 = 0; i22 < tabStopSpanArr.length; i22++) {
                                        iArr4[i22] = tabStopSpanArr[i22].getTabStop();
                                    }
                                    Arrays.sort(iArr4, 0, iArr4.length);
                                    iArr2 = iArr4;
                                } else {
                                    iArr2 = null;
                                }
                                MeasuredParagraph measuredParagraph = paragraphInfoArr[i].measured;
                                char[] chars = measuredParagraph.getChars();
                                int[] rawArray = measuredParagraph.getSpanEndCache().getRawArray();
                                int[] rawArray2 = measuredParagraph.getFontMetrics().getRawArray();
                                AutoGrowArray.FloatArray floatArray4 = floatArray;
                                floatArray4.resize(chars.length);
                                r72 = r2;
                                Spanned spanned4 = spanned;
                                LineBreaks lineBreaks5 = lineBreaks;
                                int i23 = i;
                                PrecomputedText.ParagraphInfo[] paragraphInfoArr3 = paragraphInfoArr;
                                MeasuredParagraph measuredParagraph2 = measuredParagraph;
                                TextPaint textPaint3 = textPaint2;
                                CharSequence charSequence4 = charSequence3;
                                int iNComputeLineBreaks = nComputeLineBreaks(j, chars, measuredParagraph.getNativePtr(), i18 - i17, i6, i8, i7, iArr2, 20, r0.mLineCount, lineBreaks5, lineBreaks5.breaks.length, lineBreaks5.breaks, lineBreaks5.widths, lineBreaks5.ascents, lineBreaks5.descents, lineBreaks5.flags, floatArray4.getRawArray());
                                int[] iArr5 = lineBreaks5.breaks;
                                float[] fArr = lineBreaks5.widths;
                                float[] fArr2 = lineBreaks5.ascents;
                                float[] fArr3 = lineBreaks5.descents;
                                int[] iArr6 = lineBreaks5.flags;
                                int i24 = r0.mMaximumVisibleLineCount - r0.mLineCount;
                                if (truncateAt != null) {
                                    truncateAt3 = truncateAt;
                                    if (truncateAt3 != TextUtils.TruncateAt.END) {
                                        lineBreaks2 = lineBreaks5;
                                        z6 = true;
                                        if (r0.mMaximumVisibleLineCount == 1 && truncateAt3 != TextUtils.TruncateAt.MARQUEE) {
                                        }
                                    } else {
                                        lineBreaks2 = lineBreaks5;
                                        z6 = true;
                                    }
                                    z4 = z6;
                                    if (i24 > 0 || i24 >= iNComputeLineBreaks || !z4) {
                                        truncateAt4 = truncateAt3;
                                        i9 = iNComputeLineBreaks;
                                    } else {
                                        int i25 = i24 - 1;
                                        int i26 = i25;
                                        float f6 = 0.0f;
                                        int i27 = 0;
                                        while (i26 < iNComputeLineBreaks) {
                                            int i28 = i24;
                                            if (i26 == iNComputeLineBreaks - 1) {
                                                f6 += fArr[i26];
                                                truncateAt5 = truncateAt3;
                                            } else {
                                                int i29 = i26 == 0 ? 0 : iArr5[i26 - 1];
                                                while (true) {
                                                    truncateAt5 = truncateAt3;
                                                    if (i29 < iArr5[i26]) {
                                                        f6 += floatArray4.get(i29);
                                                        i29++;
                                                        truncateAt3 = truncateAt5;
                                                    }
                                                }
                                            }
                                            i27 |= iArr6[i26] & 536870912;
                                            i26++;
                                            i24 = i28;
                                            truncateAt3 = truncateAt5;
                                        }
                                        truncateAt4 = truncateAt3;
                                        iArr5[i25] = iArr5[iNComputeLineBreaks - 1];
                                        fArr[i25] = f6;
                                        iArr6[i25] = i27;
                                        i9 = i24;
                                    }
                                    i10 = i17;
                                    int i30 = i10;
                                    char[] cArr = chars;
                                    float[] fArr4 = fArr;
                                    int i31 = 0;
                                    int i32 = 0;
                                    int i33 = 0;
                                    int i34 = 0;
                                    int i35 = 0;
                                    int i36 = 0;
                                    int i37 = 0;
                                    while (i10 < i18) {
                                        int i38 = i32 + 1;
                                        int i39 = rawArray[i32];
                                        int i40 = i33 * 4;
                                        int i41 = i18;
                                        AutoGrowArray.FloatArray floatArray5 = floatArray4;
                                        Paint.FontMetricsInt fontMetricsInt4 = fontMetricsInt;
                                        fontMetricsInt4.top = rawArray2[i40 + 0];
                                        fontMetricsInt4.bottom = rawArray2[i40 + 1];
                                        fontMetricsInt4.ascent = rawArray2[i40 + 2];
                                        fontMetricsInt4.descent = rawArray2[i40 + 3];
                                        int i42 = i33 + 1;
                                        if (fontMetricsInt4.top < i34) {
                                            i34 = fontMetricsInt4.top;
                                        }
                                        if (fontMetricsInt4.ascent < i35) {
                                            i35 = fontMetricsInt4.ascent;
                                        }
                                        if (fontMetricsInt4.descent > i31) {
                                            i31 = fontMetricsInt4.descent;
                                        }
                                        if (fontMetricsInt4.bottom > i36) {
                                            i36 = fontMetricsInt4.bottom;
                                        }
                                        int i43 = i37;
                                        while (i43 < i9 && iArr5[i43] + i17 < i10) {
                                            i43++;
                                        }
                                        int iMin = i35;
                                        int i44 = i34;
                                        int i45 = i43;
                                        int i46 = i31;
                                        int i47 = i45;
                                        ?? r722 = r72;
                                        while (i47 < i9 && iArr5[i47] + i17 <= i39) {
                                            int i48 = i17 + iArr5[i47];
                                            boolean z11 = i48 < i13;
                                            if (z7) {
                                                iMin = Math.min(iMin, Math.round(fArr2[i47]));
                                            }
                                            int i49 = iMin;
                                            int iMax2 = z7 ? Math.max(i46, Math.round(fArr3[i47])) : i46;
                                            AutoGrowArray.FloatArray floatArray6 = floatArray5;
                                            int i50 = i47;
                                            int i51 = i30;
                                            TextUtils.TruncateAt truncateAt7 = truncateAt4;
                                            int i52 = i41;
                                            int i53 = i17;
                                            int[] iArr7 = iArr6;
                                            int i54 = i23;
                                            int i55 = i44;
                                            LineBreaks lineBreaks6 = lineBreaks2;
                                            char[] cArr2 = cArr;
                                            int i56 = i36;
                                            int i57 = i9;
                                            Spanned spanned5 = spanned4;
                                            float[] fArr5 = fArr3;
                                            float f7 = f;
                                            PrecomputedText.ParagraphInfo[] paragraphInfoArr4 = paragraphInfoArr3;
                                            float[] fArr6 = fArr2;
                                            TextDirectionHeuristic textDirectionHeuristic5 = textDirectionHeuristic2;
                                            MeasuredParagraph measuredParagraph3 = measuredParagraph2;
                                            TextPaint textPaint4 = textPaint3;
                                            float[] fArr7 = fArr4;
                                            int i58 = i13;
                                            Paint.FontMetricsInt fontMetricsInt5 = fontMetricsInt4;
                                            int i59 = i5;
                                            int[] iArr8 = iArr5;
                                            CharSequence charSequence5 = charSequence4;
                                            int iOut = out(charSequence4, i51, i48, i49, iMax2, i55, i56, r722 == true ? 1 : 0, f3, f4, lineHeightSpanArr, r572, fontMetricsInt4, iArr6[i47], z9, measuredParagraph3, i58, z, z2, z8, cArr2, floatArray6.getRawArray(), i53, truncateAt7, f7, fArr4[i47], textPaint4, z11);
                                            i39 = i39;
                                            if (i48 < i39) {
                                                fontMetricsInt4 = fontMetricsInt5;
                                                int i60 = fontMetricsInt4.top;
                                                i36 = fontMetricsInt4.bottom;
                                                iMin = fontMetricsInt4.ascent;
                                                i46 = fontMetricsInt4.descent;
                                                i44 = i60;
                                            } else {
                                                fontMetricsInt4 = fontMetricsInt5;
                                                iMin = 0;
                                                i46 = 0;
                                                i44 = 0;
                                                i36 = 0;
                                            }
                                            int i61 = i50 + 1;
                                            if (this.mLineCount >= this.mMaximumVisibleLineCount && this.mEllipsized) {
                                                nFinish(j);
                                                return;
                                            }
                                            i30 = i48;
                                            i47 = i61;
                                            j = j;
                                            i17 = i53;
                                            i23 = i54 == true ? 1 : 0;
                                            iArr6 = iArr7;
                                            lineBreaks2 = lineBreaks6;
                                            cArr = cArr2;
                                            truncateAt4 = truncateAt7;
                                            spanned4 = spanned5;
                                            f = f7;
                                            fArr3 = fArr5;
                                            paragraphInfoArr3 = paragraphInfoArr4;
                                            measuredParagraph2 = measuredParagraph3;
                                            fArr2 = fArr6;
                                            fArr4 = fArr7;
                                            floatArray5 = floatArray6;
                                            iArr5 = iArr8;
                                            i9 = i57;
                                            i41 = i52;
                                            textPaint3 = textPaint4;
                                            textDirectionHeuristic2 = textDirectionHeuristic5;
                                            i13 = i58;
                                            i5 = i59;
                                            charSequence4 = charSequence5;
                                            r722 = iOut;
                                        }
                                        int i62 = i47;
                                        i31 = i46;
                                        i34 = i44;
                                        j = j;
                                        fontMetricsInt = fontMetricsInt4;
                                        i33 = i42;
                                        i17 = i17;
                                        i23 = i23 == true ? 1 : 0;
                                        iArr6 = iArr6;
                                        lineBreaks2 = lineBreaks2;
                                        cArr = cArr;
                                        truncateAt4 = truncateAt4;
                                        spanned4 = spanned4;
                                        f = f;
                                        fArr3 = fArr3;
                                        paragraphInfoArr3 = paragraphInfoArr3;
                                        measuredParagraph2 = measuredParagraph2;
                                        fArr2 = fArr2;
                                        fArr4 = fArr4;
                                        floatArray4 = floatArray5;
                                        iArr5 = iArr5;
                                        i9 = i9;
                                        i37 = i62;
                                        i18 = i41;
                                        textPaint3 = textPaint3;
                                        textDirectionHeuristic2 = textDirectionHeuristic2;
                                        i13 = i13;
                                        i5 = i5;
                                        charSequence4 = charSequence4;
                                        i35 = iMin;
                                        i10 = i39;
                                        i32 = i38;
                                        r72 = r722;
                                    }
                                    i11 = i18;
                                    floatArray2 = floatArray4;
                                    f2 = f;
                                    fontMetricsInt2 = fontMetricsInt;
                                    j3 = j;
                                    textDirectionHeuristic = textDirectionHeuristic2;
                                    i3 = i5;
                                    spanned2 = spanned4;
                                    z5 = i23 == true ? 1 : 0;
                                    paragraphInfoArr2 = paragraphInfoArr3;
                                    textPaint = textPaint3;
                                    charSequence = charSequence4;
                                    lineBreaks3 = lineBreaks2;
                                    truncateAt2 = truncateAt4;
                                    r5 = this;
                                    i2 = i13;
                                    if (i11 != i2) {
                                        break;
                                    }
                                    i = (z5 ? 1 : 0) + 1;
                                    i13 = i2;
                                    r0 = r5;
                                    j = j3;
                                    fontMetricsInt = fontMetricsInt2;
                                    lineBreaks = lineBreaks3;
                                    z3 = false;
                                    truncateAt = truncateAt2;
                                    spanned = spanned2;
                                    f = f2;
                                    paragraphInfoArr = paragraphInfoArr2;
                                    floatArray = floatArray2;
                                    r1 = r572;
                                    r2 = r72;
                                    textPaint2 = textPaint;
                                    textDirectionHeuristic3 = textDirectionHeuristic;
                                    i12 = i3;
                                    charSequence3 = charSequence;
                                } else {
                                    lineBreaks2 = lineBreaks5;
                                    truncateAt3 = truncateAt;
                                }
                                z4 = false;
                                if (i24 > 0) {
                                    truncateAt4 = truncateAt3;
                                    i9 = iNComputeLineBreaks;
                                    i10 = i17;
                                    int i302 = i10;
                                    char[] cArr3 = chars;
                                    float[] fArr42 = fArr;
                                    int i312 = 0;
                                    int i322 = 0;
                                    int i332 = 0;
                                    int i342 = 0;
                                    int i352 = 0;
                                    int i362 = 0;
                                    int i372 = 0;
                                    while (i10 < i18) {
                                    }
                                    i11 = i18;
                                    floatArray2 = floatArray4;
                                    f2 = f;
                                    fontMetricsInt2 = fontMetricsInt;
                                    j3 = j;
                                    textDirectionHeuristic = textDirectionHeuristic2;
                                    i3 = i5;
                                    spanned2 = spanned4;
                                    z5 = i23 == true ? 1 : 0;
                                    paragraphInfoArr2 = paragraphInfoArr3;
                                    textPaint = textPaint3;
                                    charSequence = charSequence4;
                                    lineBreaks3 = lineBreaks2;
                                    truncateAt2 = truncateAt4;
                                    r5 = this;
                                    i2 = i13;
                                    if (i11 != i2) {
                                    }
                                }
                            }
                        }
                        nFinish(j2);
                        throw th;
                    }
                    r57 = r1;
                    i8 = i20;
                    i6 = leadingMargin;
                    i7 = leadingMargin2;
                } else {
                    textDirectionHeuristic2 = textDirectionHeuristic3;
                    i5 = i12;
                    r57 = r1;
                    i6 = i14;
                    i7 = i6;
                    i8 = 1;
                }
                lineHeightSpanArr = null;
                r572 = r57;
                if (spanned == null) {
                }
                nFinish(j2);
                throw th;
            }
            ?? r9 = r72;
            i4 = i3;
            if (i2 == i4) {
                charSequence2 = charSequence;
                try {
                    if (charSequence2.charAt(i2 - 1) == '\n') {
                    }
                    j4 = j3;
                    nFinish(j4);
                } catch (Throwable th2) {
                    th = th2;
                    j2 = j3;
                }
            } else {
                charSequence2 = charSequence;
            }
            if (r5.mLineCount >= r5.mMaximumVisibleLineCount) {
                MeasuredParagraph measuredParagraphBuildForBidi = MeasuredParagraph.buildForBidi(charSequence2, i2, i2, textDirectionHeuristic, null);
                TextPaint textPaint5 = textPaint;
                textPaint5.getFontMetricsInt(fontMetricsInt2);
                j4 = j3;
                try {
                    r5.out(charSequence2, i2, i2, fontMetricsInt2.ascent, fontMetricsInt2.descent, fontMetricsInt2.top, fontMetricsInt2.bottom, r9 == true ? 1 : 0, f3, f4, null, null, fontMetricsInt2, 0, z9, measuredParagraphBuildForBidi, i2, z, z2, z8, null, null, i4, truncateAt2, f2, 0.0f, textPaint5, false);
                } catch (Throwable th3) {
                    th = th3;
                    j2 = j4;
                }
            } else {
                j4 = j3;
            }
            nFinish(j4);
        }
        fontMetricsInt = fontMetricsInt3;
        j = jNInit;
        floatArray = floatArray3;
        lineBreaks = lineBreaks4;
        truncateAt = truncateAt6;
        f = f5;
        z3 = false;
        spanned = spanned3;
        if (paragraphInfo == null) {
        }
        paragraphInfoArr = paragraphInfo;
        boolean z102 = z3;
        i = z102 ? 1 : 0;
        r1 = 0;
        r0 = r02;
        r2 = z102;
        while (true) {
            if (i < paragraphInfoArr.length) {
            }
            i = (z5 ? 1 : 0) + 1;
            i13 = i2;
            r0 = r5;
            j = j3;
            fontMetricsInt = fontMetricsInt2;
            lineBreaks = lineBreaks3;
            z3 = false;
            truncateAt = truncateAt2;
            spanned = spanned2;
            f = f2;
            paragraphInfoArr = paragraphInfoArr2;
            floatArray = floatArray2;
            r1 = r572;
            r2 = r72;
            textPaint2 = textPaint;
            textDirectionHeuristic3 = textDirectionHeuristic;
            i12 = i3;
            charSequence3 = charSequence;
        }
        ?? r92 = r72;
        i4 = i3;
        if (i2 == i4) {
        }
        if (r5.mLineCount >= r5.mMaximumVisibleLineCount) {
        }
        nFinish(j4);
    }

    private int out(CharSequence charSequence, int i, int i2, int i3, int i4, int i5, int i6, int i7, float f, float f2, LineHeightSpan[] lineHeightSpanArr, int[] iArr, Paint.FontMetricsInt fontMetricsInt, int i8, boolean z, MeasuredParagraph measuredParagraph, int i9, boolean z2, boolean z3, boolean z4, char[] cArr, float[] fArr, int i10, TextUtils.TruncateAt truncateAt, float f3, float f4, TextPaint textPaint, boolean z5) {
        int[] iArr2;
        int i11;
        TextUtils.TruncateAt truncateAt2;
        int i12;
        int i13;
        int i14;
        int i15;
        int i16;
        boolean z6;
        int i17;
        int i18;
        int i19;
        int i20;
        int i21;
        TextUtils.TruncateAt truncateAt3;
        TextUtils.TruncateAt truncateAt4 = truncateAt;
        int i22 = this.mLineCount;
        int i23 = i22 * this.mColumns;
        boolean z7 = true;
        int i24 = this.mColumns + i23 + 1;
        int[] iArr3 = this.mLines;
        int paragraphDir = measuredParagraph.getParagraphDir();
        if (i24 >= iArr3.length) {
            int[] iArrNewUnpaddedIntArray = ArrayUtils.newUnpaddedIntArray(GrowingArrayUtils.growSize(i24));
            System.arraycopy(iArr3, 0, iArrNewUnpaddedIntArray, 0, iArr3.length);
            this.mLines = iArrNewUnpaddedIntArray;
            iArr2 = iArrNewUnpaddedIntArray;
        } else {
            iArr2 = iArr3;
        }
        if (i22 >= this.mLineDirections.length) {
            Layout.Directions[] directionsArr = (Layout.Directions[]) ArrayUtils.newUnpaddedArray(Layout.Directions.class, GrowingArrayUtils.growSize(i22));
            System.arraycopy(this.mLineDirections, 0, directionsArr, 0, this.mLineDirections.length);
            this.mLineDirections = directionsArr;
        }
        if (lineHeightSpanArr != null) {
            fontMetricsInt.ascent = i3;
            fontMetricsInt.descent = i4;
            fontMetricsInt.top = i5;
            fontMetricsInt.bottom = i6;
            int i25 = 0;
            while (i25 < lineHeightSpanArr.length) {
                if (lineHeightSpanArr[i25] instanceof LineHeightSpan.WithDensity) {
                    i20 = i25;
                    i21 = i22;
                    truncateAt3 = truncateAt4;
                    ((LineHeightSpan.WithDensity) lineHeightSpanArr[i25]).chooseHeight(charSequence, i, i2, iArr[i25], i7, fontMetricsInt, textPaint);
                } else {
                    i20 = i25;
                    i21 = i22;
                    truncateAt3 = truncateAt4;
                    lineHeightSpanArr[i20].chooseHeight(charSequence, i, i2, iArr[i20], i7, fontMetricsInt);
                }
                i25 = i20 + 1;
                truncateAt4 = truncateAt3;
                i22 = i21;
                z7 = true;
            }
            i11 = i22;
            truncateAt2 = truncateAt4;
            i13 = fontMetricsInt.ascent;
            i14 = fontMetricsInt.descent;
            i15 = fontMetricsInt.top;
            i12 = fontMetricsInt.bottom;
        } else {
            i11 = i22;
            truncateAt2 = truncateAt4;
            i12 = i6;
            i13 = i3;
            i14 = i4;
            i15 = i5;
        }
        boolean z8 = i11 == 0;
        boolean z9 = i11 + 1 == this.mMaximumVisibleLineCount;
        if (truncateAt2 != null) {
            if (!z5) {
                i19 = 1;
            } else {
                i19 = 1;
                boolean z10 = this.mLineCount + 1 == this.mMaximumVisibleLineCount;
                if ((((this.mMaximumVisibleLineCount != i19 && z5) || (z8 && !z5)) && truncateAt2 != TextUtils.TruncateAt.MARQUEE) || (!z8 && ((z9 || !z5) && truncateAt2 == TextUtils.TruncateAt.END))) {
                    calculateEllipsis(i, i2, fArr, i10, f3, truncateAt2, i11, f4, textPaint, z10);
                }
            }
            if (this.mMaximumVisibleLineCount != i19) {
                if ((((this.mMaximumVisibleLineCount != i19 && z5) || (z8 && !z5)) && truncateAt2 != TextUtils.TruncateAt.MARQUEE) || (!z8 && ((z9 || !z5) && truncateAt2 == TextUtils.TruncateAt.END))) {
                }
            } else {
                if ((((this.mMaximumVisibleLineCount != i19 && z5) || (z8 && !z5)) && truncateAt2 != TextUtils.TruncateAt.MARQUEE) || (!z8 && ((z9 || !z5) && truncateAt2 == TextUtils.TruncateAt.END))) {
                }
            }
        }
        if (!this.mEllipsized) {
            i16 = i10;
            boolean z11 = i16 != i9 && i9 > 0 && charSequence.charAt(i9 + (-1)) == '\n';
            z6 = (i2 == i9 && !z11) || (i == i9 && z11);
            if (z8) {
                if (z3) {
                    this.mTopPadding = i15 - i13;
                }
                if (z2) {
                    i13 = i15;
                }
            }
            if (z6) {
                if (z3) {
                    this.mBottomPadding = i12 - i14;
                }
                if (z2) {
                    i14 = i12;
                }
            }
            if (!z && (z4 || !z6)) {
                double d = ((i14 - i13) * (f - 1.0f)) + f2;
                if (d >= 0.0d) {
                    i18 = (int) (d + EXTRA_ROUNDING);
                } else {
                    i18 = -((int) ((-d) + EXTRA_ROUNDING));
                }
                i17 = i18;
            } else {
                i17 = 0;
            }
            int i26 = i23 + 0;
            iArr2[i26] = i;
            iArr2[i23 + 1] = i7;
            iArr2[i23 + 2] = i14 + i17;
            iArr2[i23 + 3] = i17;
            if (!this.mEllipsized && z9) {
                if (!z2) {
                    i12 = i14;
                }
                this.mMaxLineHeight = i7 + (i12 - i13);
            }
            int i27 = i7 + (i14 - i13) + i17;
            iArr2[this.mColumns + i23 + 0] = i2;
            iArr2[this.mColumns + i23 + 1] = i27;
            iArr2[i26] = iArr2[i26] | (i8 & 536870912);
            iArr2[i23 + 4] = i8;
            iArr2[i26] = iArr2[i26] | (paragraphDir << 30);
            this.mLineDirections[i11] = measuredParagraph.getDirections(i - i16, i2 - i16);
            this.mLineCount++;
            return i27;
        }
        i16 = i10;
        if (z8) {
        }
        if (z6) {
        }
        if (!z) {
            i17 = 0;
        }
        int i262 = i23 + 0;
        iArr2[i262] = i;
        iArr2[i23 + 1] = i7;
        iArr2[i23 + 2] = i14 + i17;
        iArr2[i23 + 3] = i17;
        if (!this.mEllipsized) {
            if (!z2) {
            }
            this.mMaxLineHeight = i7 + (i12 - i13);
        }
        int i272 = i7 + (i14 - i13) + i17;
        iArr2[this.mColumns + i23 + 0] = i2;
        iArr2[this.mColumns + i23 + 1] = i272;
        iArr2[i262] = iArr2[i262] | (i8 & 536870912);
        iArr2[i23 + 4] = i8;
        iArr2[i262] = iArr2[i262] | (paragraphDir << 30);
        this.mLineDirections[i11] = measuredParagraph.getDirections(i - i16, i2 - i16);
        this.mLineCount++;
        return i272;
    }

    private void calculateEllipsis(int i, int i2, float[] fArr, int i3, float f, TextUtils.TruncateAt truncateAt, int i4, float f2, TextPaint textPaint, boolean z) {
        int i5;
        float totalInsets = f - getTotalInsets(i4);
        int i6 = 0;
        if (f2 <= totalInsets && !z) {
            this.mLines[(this.mColumns * i4) + 5] = 0;
            this.mLines[(this.mColumns * i4) + 6] = 0;
            return;
        }
        float fMeasureText = textPaint.measureText(TextUtils.getEllipsisString(truncateAt));
        int i7 = i2 - i;
        float f3 = 0.0f;
        if (truncateAt == TextUtils.TruncateAt.START) {
            if (this.mMaximumVisibleLineCount == 1) {
                int i8 = i7;
                float f4 = 0.0f;
                while (true) {
                    if (i8 <= 0) {
                        break;
                    }
                    f4 += fArr[((i8 - 1) + i) - i3];
                    if (f4 + fMeasureText <= totalInsets) {
                        i8--;
                    } else {
                        while (i8 < i7 && fArr[(i8 + i) - i3] == 0.0f) {
                            i8++;
                        }
                    }
                }
                i5 = i8;
            } else {
                if (Log.isLoggable(TAG, 5)) {
                    Log.w(TAG, "Start Ellipsis only supported with one line");
                }
                i5 = 0;
            }
        } else if (truncateAt == TextUtils.TruncateAt.END || truncateAt == TextUtils.TruncateAt.MARQUEE || truncateAt == TextUtils.TruncateAt.END_SMALL) {
            while (i6 < i7) {
                f3 += fArr[(i6 + i) - i3];
                if (f3 + fMeasureText > totalInsets) {
                    break;
                } else {
                    i6++;
                }
            }
            i5 = i7 - i6;
            if (z && i5 == 0 && i7 > 0) {
                i6 = i7 - 1;
                i5 = 1;
            }
        } else if (this.mMaximumVisibleLineCount == 1) {
            float f5 = totalInsets - fMeasureText;
            float f6 = f5 / 2.0f;
            int i9 = i7;
            float f7 = 0.0f;
            while (true) {
                if (i9 <= 0) {
                    break;
                }
                float f8 = fArr[((i9 - 1) + i) - i3] + f7;
                if (f8 <= f6) {
                    i9--;
                    f7 = f8;
                } else {
                    while (i9 < i7 && fArr[(i9 + i) - i3] == 0.0f) {
                        i9++;
                    }
                }
            }
            float f9 = f5 - f7;
            while (i6 < i9) {
                f3 += fArr[(i6 + i) - i3];
                if (f3 > f9) {
                    break;
                } else {
                    i6++;
                }
            }
            i5 = i9 - i6;
        } else {
            if (Log.isLoggable(TAG, 5)) {
                Log.w(TAG, "Middle Ellipsis only supported with one line");
            }
            i5 = 0;
        }
        this.mEllipsized = true;
        this.mLines[(this.mColumns * i4) + 5] = i6;
        this.mLines[(this.mColumns * i4) + 6] = i5;
    }

    private float getTotalInsets(int i) {
        int i2;
        if (this.mLeftIndents != null) {
            i2 = this.mLeftIndents[Math.min(i, this.mLeftIndents.length - 1)];
        } else {
            i2 = 0;
        }
        if (this.mRightIndents != null) {
            i2 += this.mRightIndents[Math.min(i, this.mRightIndents.length - 1)];
        }
        return i2;
    }

    @Override
    public int getLineForVertical(int i) {
        int i2 = this.mLineCount;
        int[] iArr = this.mLines;
        int i3 = -1;
        while (i2 - i3 > 1) {
            int i4 = (i2 + i3) >> 1;
            if (iArr[(this.mColumns * i4) + 1] > i) {
                i2 = i4;
            } else {
                i3 = i4;
            }
        }
        if (i3 < 0) {
            return 0;
        }
        return i3;
    }

    @Override
    public int getLineCount() {
        return this.mLineCount;
    }

    @Override
    public int getLineTop(int i) {
        return this.mLines[(this.mColumns * i) + 1];
    }

    @Override
    public int getLineExtra(int i) {
        return this.mLines[(this.mColumns * i) + 3];
    }

    @Override
    public int getLineDescent(int i) {
        return this.mLines[(this.mColumns * i) + 2];
    }

    @Override
    public int getLineStart(int i) {
        return this.mLines[(this.mColumns * i) + 0] & 536870911;
    }

    @Override
    public int getParagraphDirection(int i) {
        return this.mLines[(this.mColumns * i) + 0] >> 30;
    }

    @Override
    public boolean getLineContainsTab(int i) {
        return (this.mLines[(this.mColumns * i) + 0] & 536870912) != 0;
    }

    @Override
    public final Layout.Directions getLineDirections(int i) {
        if (i > getLineCount()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return this.mLineDirections[i];
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
    public int getHyphen(int i) {
        return this.mLines[(this.mColumns * i) + 4] & 255;
    }

    @Override
    public int getIndentAdjust(int i, Layout.Alignment alignment) {
        int i2;
        if (alignment == Layout.Alignment.ALIGN_LEFT) {
            if (this.mLeftIndents == null) {
                return 0;
            }
            return this.mLeftIndents[Math.min(i, this.mLeftIndents.length - 1)];
        }
        if (alignment == Layout.Alignment.ALIGN_RIGHT) {
            if (this.mRightIndents == null) {
                return 0;
            }
            return -this.mRightIndents[Math.min(i, this.mRightIndents.length - 1)];
        }
        if (alignment == Layout.Alignment.ALIGN_CENTER) {
            if (this.mLeftIndents != null) {
                i2 = this.mLeftIndents[Math.min(i, this.mLeftIndents.length - 1)];
            } else {
                i2 = 0;
            }
            return (i2 - (this.mRightIndents != null ? this.mRightIndents[Math.min(i, this.mRightIndents.length - 1)] : 0)) >> 1;
        }
        throw new AssertionError("unhandled alignment " + alignment);
    }

    @Override
    public int getEllipsisCount(int i) {
        if (this.mColumns < 7) {
            return 0;
        }
        return this.mLines[(this.mColumns * i) + 6];
    }

    @Override
    public int getEllipsisStart(int i) {
        if (this.mColumns < 7) {
            return 0;
        }
        return this.mLines[(this.mColumns * i) + 5];
    }

    @Override
    public int getEllipsizedWidth() {
        return this.mEllipsizedWidth;
    }

    @Override
    public int getHeight(boolean z) {
        if (z && this.mLineCount >= this.mMaximumVisibleLineCount && this.mMaxLineHeight == -1 && Log.isLoggable(TAG, 5)) {
            Log.w(TAG, "maxLineHeight should not be -1.  maxLines:" + this.mMaximumVisibleLineCount + " lineCount:" + this.mLineCount);
        }
        return (!z || this.mLineCount < this.mMaximumVisibleLineCount || this.mMaxLineHeight == -1) ? super.getHeight() : this.mMaxLineHeight;
    }

    static class LineBreaks {
        private static final int INITIAL_SIZE = 16;
        public int[] breaks = new int[16];
        public float[] widths = new float[16];
        public float[] ascents = new float[16];
        public float[] descents = new float[16];
        public int[] flags = new int[16];

        LineBreaks() {
        }
    }
}
