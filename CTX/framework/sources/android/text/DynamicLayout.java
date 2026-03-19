package android.text;

import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextUtils;
import android.text.style.ReplacementSpan;
import android.text.style.UpdateLayout;
import android.text.style.WrapTogetherSpan;
import android.util.ArraySet;
import android.util.Pools;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.GrowingArrayUtils;
import java.lang.ref.WeakReference;

public class DynamicLayout extends Layout {
    private static final int BLOCK_MINIMUM_CHARACTER_LENGTH = 400;
    private static final int COLUMNS_ELLIPSIZE = 7;
    private static final int COLUMNS_NORMAL = 5;
    private static final int DESCENT = 2;
    private static final int DIR = 0;
    private static final int DIR_SHIFT = 30;
    private static final int ELLIPSIS_COUNT = 6;
    private static final int ELLIPSIS_START = 5;
    private static final int ELLIPSIS_UNDEFINED = Integer.MIN_VALUE;
    private static final int EXTRA = 3;
    private static final int HYPHEN = 4;
    private static final int HYPHEN_MASK = 255;
    public static final int INVALID_BLOCK_INDEX = -1;
    private static final int MAY_PROTRUDE_FROM_TOP_OR_BOTTOM = 4;
    private static final int MAY_PROTRUDE_FROM_TOP_OR_BOTTOM_MASK = 256;
    private static final int PRIORITY = 128;
    private static final int START = 0;
    private static final int START_MASK = 536870911;
    private static final int TAB = 0;
    private static final int TAB_MASK = 536870912;
    private static final int TOP = 1;
    private CharSequence mBase;
    private int[] mBlockEndLines;
    private int[] mBlockIndices;
    private ArraySet<Integer> mBlocksAlwaysNeedToBeRedrawn;
    private int mBottomPadding;
    private int mBreakStrategy;
    private CharSequence mDisplay;
    private boolean mEllipsize;
    private TextUtils.TruncateAt mEllipsizeAt;
    private int mEllipsizedWidth;
    private boolean mFallbackLineSpacing;
    private int mHyphenationFrequency;
    private boolean mIncludePad;
    private int mIndexFirstChangedBlock;
    private PackedIntVector mInts;
    private int mJustificationMode;
    private int mNumberOfBlocks;
    private PackedObjectVector<Layout.Directions> mObjects;
    private Rect mTempRect;
    private int mTopPadding;
    private ChangeWatcher mWatcher;
    private static StaticLayout sStaticLayout = null;
    private static StaticLayout.Builder sBuilder = null;
    private static final Object[] sLock = new Object[0];

    public static final class Builder {
        private static final Pools.SynchronizedPool<Builder> sPool = new Pools.SynchronizedPool<>(3);
        private Layout.Alignment mAlignment;
        private CharSequence mBase;
        private int mBreakStrategy;
        private CharSequence mDisplay;
        private TextUtils.TruncateAt mEllipsize;
        private int mEllipsizedWidth;
        private boolean mFallbackLineSpacing;
        private final Paint.FontMetricsInt mFontMetricsInt = new Paint.FontMetricsInt();
        private int mHyphenationFrequency;
        private boolean mIncludePad;
        private int mJustificationMode;
        private TextPaint mPaint;
        private float mSpacingAdd;
        private float mSpacingMult;
        private TextDirectionHeuristic mTextDir;
        private int mWidth;

        private Builder() {
        }

        public static Builder obtain(CharSequence charSequence, TextPaint textPaint, int i) {
            Builder builderAcquire = sPool.acquire();
            if (builderAcquire == null) {
                builderAcquire = new Builder();
            }
            builderAcquire.mBase = charSequence;
            builderAcquire.mDisplay = charSequence;
            builderAcquire.mPaint = textPaint;
            builderAcquire.mWidth = i;
            builderAcquire.mAlignment = Layout.Alignment.ALIGN_NORMAL;
            builderAcquire.mTextDir = TextDirectionHeuristics.FIRSTSTRONG_LTR;
            builderAcquire.mSpacingMult = 1.0f;
            builderAcquire.mSpacingAdd = 0.0f;
            builderAcquire.mIncludePad = true;
            builderAcquire.mFallbackLineSpacing = false;
            builderAcquire.mEllipsizedWidth = i;
            builderAcquire.mEllipsize = null;
            builderAcquire.mBreakStrategy = 0;
            builderAcquire.mHyphenationFrequency = 0;
            builderAcquire.mJustificationMode = 0;
            return builderAcquire;
        }

        private static void recycle(Builder builder) {
            builder.mBase = null;
            builder.mDisplay = null;
            builder.mPaint = null;
            sPool.release(builder);
        }

        public Builder setDisplayText(CharSequence charSequence) {
            this.mDisplay = charSequence;
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

        public Builder setBreakStrategy(int i) {
            this.mBreakStrategy = i;
            return this;
        }

        public Builder setHyphenationFrequency(int i) {
            this.mHyphenationFrequency = i;
            return this;
        }

        public Builder setJustificationMode(int i) {
            this.mJustificationMode = i;
            return this;
        }

        public DynamicLayout build() {
            DynamicLayout dynamicLayout = new DynamicLayout(this);
            recycle(this);
            return dynamicLayout;
        }
    }

    @Deprecated
    public DynamicLayout(CharSequence charSequence, TextPaint textPaint, int i, Layout.Alignment alignment, float f, float f2, boolean z) {
        this(charSequence, charSequence, textPaint, i, alignment, f, f2, z);
    }

    @Deprecated
    public DynamicLayout(CharSequence charSequence, CharSequence charSequence2, TextPaint textPaint, int i, Layout.Alignment alignment, float f, float f2, boolean z) {
        this(charSequence, charSequence2, textPaint, i, alignment, f, f2, z, null, 0);
    }

    @Deprecated
    public DynamicLayout(CharSequence charSequence, CharSequence charSequence2, TextPaint textPaint, int i, Layout.Alignment alignment, float f, float f2, boolean z, TextUtils.TruncateAt truncateAt, int i2) {
        this(charSequence, charSequence2, textPaint, i, alignment, TextDirectionHeuristics.FIRSTSTRONG_LTR, f, f2, z, 0, 0, 0, truncateAt, i2);
    }

    @Deprecated
    public DynamicLayout(CharSequence charSequence, CharSequence charSequence2, TextPaint textPaint, int i, Layout.Alignment alignment, TextDirectionHeuristic textDirectionHeuristic, float f, float f2, boolean z, int i2, int i3, int i4, TextUtils.TruncateAt truncateAt, int i5) {
        super(createEllipsizer(truncateAt, charSequence2), textPaint, i, alignment, textDirectionHeuristic, f, f2);
        this.mTempRect = new Rect();
        Builder ellipsize = Builder.obtain(charSequence, textPaint, i).setAlignment(alignment).setTextDirection(textDirectionHeuristic).setLineSpacing(f2, f).setEllipsizedWidth(i5).setEllipsize(truncateAt);
        this.mDisplay = charSequence2;
        this.mIncludePad = z;
        this.mBreakStrategy = i2;
        this.mJustificationMode = i4;
        this.mHyphenationFrequency = i3;
        generate(ellipsize);
        Builder.recycle(ellipsize);
    }

    private DynamicLayout(Builder builder) {
        super(createEllipsizer(builder.mEllipsize, builder.mDisplay), builder.mPaint, builder.mWidth, builder.mAlignment, builder.mTextDir, builder.mSpacingMult, builder.mSpacingAdd);
        this.mTempRect = new Rect();
        this.mDisplay = builder.mDisplay;
        this.mIncludePad = builder.mIncludePad;
        this.mBreakStrategy = builder.mBreakStrategy;
        this.mJustificationMode = builder.mJustificationMode;
        this.mHyphenationFrequency = builder.mHyphenationFrequency;
        generate(builder);
    }

    private static CharSequence createEllipsizer(TextUtils.TruncateAt truncateAt, CharSequence charSequence) {
        if (truncateAt == null) {
            return charSequence;
        }
        if (charSequence instanceof Spanned) {
            return new Layout.SpannedEllipsizer(charSequence);
        }
        return new Layout.Ellipsizer(charSequence);
    }

    private void generate(Builder builder) {
        int[] iArr;
        this.mBase = builder.mBase;
        this.mFallbackLineSpacing = builder.mFallbackLineSpacing;
        if (builder.mEllipsize != null) {
            this.mInts = new PackedIntVector(7);
            this.mEllipsizedWidth = builder.mEllipsizedWidth;
            this.mEllipsizeAt = builder.mEllipsize;
            Layout.Ellipsizer ellipsizer = (Layout.Ellipsizer) getText();
            ellipsizer.mLayout = this;
            ellipsizer.mWidth = builder.mEllipsizedWidth;
            ellipsizer.mMethod = builder.mEllipsize;
            this.mEllipsize = true;
        } else {
            this.mInts = new PackedIntVector(5);
            this.mEllipsizedWidth = builder.mWidth;
            this.mEllipsizeAt = null;
        }
        this.mObjects = new PackedObjectVector<>(1);
        if (builder.mEllipsize != null) {
            iArr = new int[7];
            iArr[5] = Integer.MIN_VALUE;
        } else {
            iArr = new int[5];
        }
        Layout.Directions[] directionsArr = {DIRS_ALL_LEFT_TO_RIGHT};
        Paint.FontMetricsInt fontMetricsInt = builder.mFontMetricsInt;
        builder.mPaint.getFontMetricsInt(fontMetricsInt);
        int i = fontMetricsInt.ascent;
        int i2 = fontMetricsInt.descent;
        iArr[0] = 1073741824;
        iArr[1] = 0;
        iArr[2] = i2;
        this.mInts.insertAt(0, iArr);
        iArr[1] = i2 - i;
        this.mInts.insertAt(1, iArr);
        this.mObjects.insertAt(0, directionsArr);
        int length = this.mBase.length();
        reflow(this.mBase, 0, 0, length);
        if (this.mBase instanceof Spannable) {
            if (this.mWatcher == null) {
                this.mWatcher = new ChangeWatcher(this);
            }
            Spannable spannable = (Spannable) this.mBase;
            for (ChangeWatcher changeWatcher : (ChangeWatcher[]) spannable.getSpans(0, length, ChangeWatcher.class)) {
                spannable.removeSpan(changeWatcher);
            }
            spannable.setSpan(this.mWatcher, 0, length, 8388626);
        }
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void reflow(CharSequence charSequence, int i, int i2, int i3) {
        int i4;
        int i5;
        int i6;
        StaticLayout staticLayout;
        StaticLayout.Builder builderObtain;
        int topPadding;
        int bottomPadding;
        int[] iArr;
        int lineStart;
        int i7;
        int i8;
        if (charSequence != this.mBase) {
            return;
        }
        CharSequence charSequence2 = this.mDisplay;
        int length = charSequence2.length();
        int iLastIndexOf = TextUtils.lastIndexOf(charSequence2, '\n', i - 1);
        if (iLastIndexOf >= 0) {
            i4 = iLastIndexOf + 1;
        } else {
            i4 = 0;
        }
        int i9 = i - i4;
        int i10 = i2 + i9;
        int i11 = i3 + i9;
        int i12 = i - i9;
        int i13 = i12 + i11;
        int iIndexOf = TextUtils.indexOf(charSequence2, '\n', i13);
        if (iIndexOf >= 0) {
            i5 = iIndexOf + 1;
        } else {
            i5 = length;
        }
        int i14 = i5 - i13;
        int i15 = i10 + i14;
        int i16 = i11 + i14;
        if (charSequence2 instanceof Spanned) {
            Spanned spanned = (Spanned) charSequence2;
            while (true) {
                Object[] spans = spanned.getSpans(i12, i12 + i16, WrapTogetherSpan.class);
                i6 = i15;
                i7 = i16;
                boolean z = false;
                i8 = i12;
                for (int i17 = 0; i17 < spans.length; i17++) {
                    int spanStart = spanned.getSpanStart(spans[i17]);
                    int spanEnd = spanned.getSpanEnd(spans[i17]);
                    if (spanStart < i8) {
                        int i18 = i8 - spanStart;
                        i6 += i18;
                        i7 += i18;
                        i8 -= i18;
                        z = true;
                    }
                    int i19 = i8 + i7;
                    if (spanEnd > i19) {
                        int i20 = spanEnd - i19;
                        i6 += i20;
                        i7 += i20;
                        z = true;
                    }
                }
                if (!z) {
                    break;
                }
                i12 = i8;
                i16 = i7;
                i15 = i6;
            }
            i12 = i8;
            i16 = i7;
        } else {
            i6 = i15;
        }
        int lineForOffset = getLineForOffset(i12);
        int lineTop = getLineTop(lineForOffset);
        int lineForOffset2 = getLineForOffset(i12 + i6);
        int i21 = i12 + i16;
        if (i21 == length) {
            lineForOffset2 = getLineCount();
        }
        int lineTop2 = getLineTop(lineForOffset2);
        boolean z2 = lineForOffset2 == getLineCount();
        synchronized (sLock) {
            staticLayout = sStaticLayout;
            builderObtain = sBuilder;
            sStaticLayout = null;
            sBuilder = null;
        }
        if (staticLayout == null) {
            staticLayout = new StaticLayout((CharSequence) null);
            builderObtain = StaticLayout.Builder.obtain(charSequence2, i12, i21, getPaint(), getWidth());
        }
        StaticLayout.Builder builder = builderObtain;
        builder.setText(charSequence2, i12, i21).setPaint(getPaint()).setWidth(getWidth()).setTextDirection(getTextDirectionHeuristic()).setLineSpacing(getSpacingAdd(), getSpacingMultiplier()).setUseLineSpacingFromFallbacks(this.mFallbackLineSpacing).setEllipsizedWidth(this.mEllipsizedWidth).setEllipsize(this.mEllipsizeAt).setBreakStrategy(this.mBreakStrategy).setHyphenationFrequency(this.mHyphenationFrequency).setJustificationMode(this.mJustificationMode).setAddLastLineLineSpacing(!z2);
        staticLayout.generate(builder, false, true);
        int lineCount = staticLayout.getLineCount();
        if (i21 != length && staticLayout.getLineStart(lineCount - 1) == i21) {
            lineCount--;
        }
        int i22 = lineForOffset2 - lineForOffset;
        this.mInts.deleteAt(lineForOffset, i22);
        this.mObjects.deleteAt(lineForOffset, i22);
        int lineTop3 = staticLayout.getLineTop(lineCount);
        if (this.mIncludePad && lineForOffset == 0) {
            topPadding = staticLayout.getTopPadding();
            this.mTopPadding = topPadding;
            lineTop3 -= topPadding;
        } else {
            topPadding = 0;
        }
        if (this.mIncludePad && z2) {
            bottomPadding = staticLayout.getBottomPadding();
            this.mBottomPadding = bottomPadding;
            lineTop3 += bottomPadding;
        } else {
            bottomPadding = 0;
        }
        this.mInts.adjustValuesBelow(lineForOffset, 0, i16 - i6);
        this.mInts.adjustValuesBelow(lineForOffset, 1, (lineTop - lineTop2) + lineTop3);
        if (this.mEllipsize) {
            iArr = new int[7];
            iArr[5] = Integer.MIN_VALUE;
        } else {
            iArr = new int[5];
        }
        Layout.Directions[] directionsArr = new Layout.Directions[1];
        int i23 = 0;
        while (i23 < lineCount) {
            int lineStart2 = staticLayout.getLineStart(i23);
            iArr[0] = lineStart2;
            iArr[0] = iArr[0] | (staticLayout.getParagraphDirection(i23) << 30);
            iArr[0] = iArr[0] | (staticLayout.getLineContainsTab(i23) ? 536870912 : 0);
            int lineTop4 = staticLayout.getLineTop(i23) + lineTop;
            if (i23 > 0) {
                lineTop4 -= topPadding;
            }
            iArr[1] = lineTop4;
            int lineDescent = staticLayout.getLineDescent(i23);
            int i24 = lineCount - 1;
            if (i23 == i24) {
                lineDescent += bottomPadding;
            }
            iArr[2] = lineDescent;
            iArr[3] = staticLayout.getLineExtra(i23);
            directionsArr[0] = staticLayout.getLineDirections(i23);
            if (i23 != i24) {
                lineStart = staticLayout.getLineStart(i23 + 1);
            } else {
                lineStart = i21;
            }
            int i25 = lineTop;
            iArr[4] = staticLayout.getHyphen(i23) & 255;
            iArr[4] = iArr[4] | (contentMayProtrudeFromLineTopOrBottom(charSequence2, lineStart2, lineStart) ? 256 : 0);
            if (this.mEllipsize) {
                iArr[5] = staticLayout.getEllipsisStart(i23);
                iArr[6] = staticLayout.getEllipsisCount(i23);
            }
            int i26 = lineForOffset + i23;
            this.mInts.insertAt(i26, iArr);
            this.mObjects.insertAt(i26, directionsArr);
            i23++;
            lineTop = i25;
        }
        updateBlocks(lineForOffset, lineForOffset2 - 1, lineCount);
        builder.finish();
        synchronized (sLock) {
            sStaticLayout = staticLayout;
            sBuilder = builder;
        }
    }

    private boolean contentMayProtrudeFromLineTopOrBottom(CharSequence charSequence, int i, int i2) {
        if ((charSequence instanceof Spanned) && ((ReplacementSpan[]) ((Spanned) charSequence).getSpans(i, i2, ReplacementSpan.class)).length > 0) {
            return true;
        }
        TextPaint paint = getPaint();
        if (charSequence instanceof PrecomputedText) {
            ((PrecomputedText) charSequence).getBounds(i, i2, this.mTempRect);
        } else {
            paint.getTextBounds(charSequence, i, i2, this.mTempRect);
        }
        Paint.FontMetricsInt fontMetricsInt = paint.getFontMetricsInt();
        return this.mTempRect.top < fontMetricsInt.top || this.mTempRect.bottom > fontMetricsInt.bottom;
    }

    private void createBlocks() {
        this.mNumberOfBlocks = 0;
        CharSequence charSequence = this.mDisplay;
        int i = 400;
        while (true) {
            int iIndexOf = TextUtils.indexOf(charSequence, '\n', i);
            if (iIndexOf < 0) {
                break;
            }
            addBlockAtOffset(iIndexOf);
            i = iIndexOf + 400;
        }
        addBlockAtOffset(charSequence.length());
        this.mBlockIndices = new int[this.mBlockEndLines.length];
        for (int i2 = 0; i2 < this.mBlockEndLines.length; i2++) {
            this.mBlockIndices[i2] = -1;
        }
    }

    public ArraySet<Integer> getBlocksAlwaysNeedToBeRedrawn() {
        return this.mBlocksAlwaysNeedToBeRedrawn;
    }

    private void updateAlwaysNeedsToBeRedrawn(int i) {
        int i2 = this.mBlockEndLines[i];
        for (int i3 = i == 0 ? 0 : this.mBlockEndLines[i - 1] + 1; i3 <= i2; i3++) {
            if (getContentMayProtrudeFromTopOrBottom(i3)) {
                if (this.mBlocksAlwaysNeedToBeRedrawn == null) {
                    this.mBlocksAlwaysNeedToBeRedrawn = new ArraySet<>();
                }
                this.mBlocksAlwaysNeedToBeRedrawn.add(Integer.valueOf(i));
                return;
            }
        }
        if (this.mBlocksAlwaysNeedToBeRedrawn != null) {
            this.mBlocksAlwaysNeedToBeRedrawn.remove(Integer.valueOf(i));
        }
    }

    private void addBlockAtOffset(int i) {
        int lineForOffset = getLineForOffset(i);
        if (this.mBlockEndLines == null) {
            this.mBlockEndLines = ArrayUtils.newUnpaddedIntArray(1);
            this.mBlockEndLines[this.mNumberOfBlocks] = lineForOffset;
            updateAlwaysNeedsToBeRedrawn(this.mNumberOfBlocks);
            this.mNumberOfBlocks++;
            return;
        }
        if (lineForOffset > this.mBlockEndLines[this.mNumberOfBlocks - 1]) {
            this.mBlockEndLines = GrowingArrayUtils.append(this.mBlockEndLines, this.mNumberOfBlocks, lineForOffset);
            updateAlwaysNeedsToBeRedrawn(this.mNumberOfBlocks);
            this.mNumberOfBlocks++;
        }
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void updateBlocks(int i, int i2, int i3) {
        int i4;
        int i5;
        int i6;
        boolean z;
        boolean z2;
        int i7;
        if (this.mBlockEndLines == null) {
            createBlocks();
            return;
        }
        int i8 = 0;
        while (true) {
            if (i8 < this.mNumberOfBlocks) {
                if (this.mBlockEndLines[i8] >= i) {
                    break;
                } else {
                    i8++;
                }
            } else {
                i8 = -1;
                break;
            }
        }
        int i9 = i8;
        while (true) {
            if (i9 < this.mNumberOfBlocks) {
                if (this.mBlockEndLines[i9] >= i2) {
                    break;
                } else {
                    i9++;
                }
            } else {
                i9 = -1;
                break;
            }
        }
        int i10 = this.mBlockEndLines[i9];
        if (i8 != 0) {
            i4 = this.mBlockEndLines[i8 - 1] + 1;
        } else {
            i4 = 0;
        }
        boolean z3 = i > i4;
        boolean z4 = i3 > 0;
        boolean z5 = i2 < this.mBlockEndLines[i9];
        if (!z3) {
            i5 = 0;
        } else {
            i5 = 1;
        }
        if (z4) {
            i5++;
        }
        if (z5) {
            i5++;
        }
        int i11 = (i9 - i8) + 1;
        int i12 = (this.mNumberOfBlocks + i5) - i11;
        if (i12 == 0) {
            this.mBlockEndLines[0] = 0;
            this.mBlockIndices[0] = -1;
            this.mNumberOfBlocks = 1;
            return;
        }
        if (i12 > this.mBlockEndLines.length) {
            int[] iArrNewUnpaddedIntArray = ArrayUtils.newUnpaddedIntArray(Math.max(this.mBlockEndLines.length * 2, i12));
            int[] iArr = new int[iArrNewUnpaddedIntArray.length];
            i6 = i10;
            System.arraycopy(this.mBlockEndLines, 0, iArrNewUnpaddedIntArray, 0, i8);
            System.arraycopy(this.mBlockIndices, 0, iArr, 0, i8);
            int i13 = i9 + 1;
            z2 = z5;
            int i14 = i8 + i5;
            z = z4;
            System.arraycopy(this.mBlockEndLines, i13, iArrNewUnpaddedIntArray, i14, (this.mNumberOfBlocks - i9) - 1);
            System.arraycopy(this.mBlockIndices, i13, iArr, i14, (this.mNumberOfBlocks - i9) - 1);
            this.mBlockEndLines = iArrNewUnpaddedIntArray;
            this.mBlockIndices = iArr;
        } else {
            i6 = i10;
            z = z4;
            z2 = z5;
            if (i5 + i11 != 0) {
                int i15 = i9 + 1;
                int i16 = i8 + i5;
                System.arraycopy(this.mBlockEndLines, i15, this.mBlockEndLines, i16, (this.mNumberOfBlocks - i9) - 1);
                System.arraycopy(this.mBlockIndices, i15, this.mBlockIndices, i16, (this.mNumberOfBlocks - i9) - 1);
            }
        }
        if (i5 + i11 != 0 && this.mBlocksAlwaysNeedToBeRedrawn != null) {
            ArraySet<Integer> arraySet = new ArraySet<>();
            int i17 = i5 - i11;
            for (int i18 = 0; i18 < this.mBlocksAlwaysNeedToBeRedrawn.size(); i18++) {
                Integer numValueAt = this.mBlocksAlwaysNeedToBeRedrawn.valueAt(i18);
                if (numValueAt.intValue() < i8) {
                    arraySet.add(numValueAt);
                }
                if (numValueAt.intValue() > i9) {
                    arraySet.add(Integer.valueOf(numValueAt.intValue() + i17));
                }
            }
            this.mBlocksAlwaysNeedToBeRedrawn = arraySet;
        }
        this.mNumberOfBlocks = i12;
        int i19 = i3 - ((i2 - i) + 1);
        if (i19 != 0) {
            i7 = i5 + i8;
            for (int i20 = i7; i20 < this.mNumberOfBlocks; i20++) {
                int[] iArr2 = this.mBlockEndLines;
                iArr2[i20] = iArr2[i20] + i19;
            }
        } else {
            i7 = this.mNumberOfBlocks;
        }
        this.mIndexFirstChangedBlock = Math.min(this.mIndexFirstChangedBlock, i7);
        if (z3) {
            this.mBlockEndLines[i8] = i - 1;
            updateAlwaysNeedsToBeRedrawn(i8);
            this.mBlockIndices[i8] = -1;
            i8++;
        }
        if (z) {
            this.mBlockEndLines[i8] = (i + i3) - 1;
            updateAlwaysNeedsToBeRedrawn(i8);
            this.mBlockIndices[i8] = -1;
            i8++;
        }
        if (z2) {
            this.mBlockEndLines[i8] = i6 + i19;
            updateAlwaysNeedsToBeRedrawn(i8);
            this.mBlockIndices[i8] = -1;
        }
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void setBlocksDataForTest(int[] iArr, int[] iArr2, int i, int i2) {
        this.mBlockEndLines = new int[iArr.length];
        this.mBlockIndices = new int[iArr2.length];
        System.arraycopy(iArr, 0, this.mBlockEndLines, 0, iArr.length);
        System.arraycopy(iArr2, 0, this.mBlockIndices, 0, iArr2.length);
        this.mNumberOfBlocks = i;
        while (this.mInts.size() < i2) {
            this.mInts.insertAt(this.mInts.size(), new int[5]);
        }
    }

    public int[] getBlockEndLines() {
        return this.mBlockEndLines;
    }

    public int[] getBlockIndices() {
        return this.mBlockIndices;
    }

    public int getBlockIndex(int i) {
        return this.mBlockIndices[i];
    }

    public void setBlockIndex(int i, int i2) {
        this.mBlockIndices[i] = i2;
    }

    public int getNumberOfBlocks() {
        return this.mNumberOfBlocks;
    }

    public int getIndexFirstChangedBlock() {
        return this.mIndexFirstChangedBlock;
    }

    public void setIndexFirstChangedBlock(int i) {
        this.mIndexFirstChangedBlock = i;
    }

    @Override
    public int getLineCount() {
        return this.mInts.size() - 1;
    }

    @Override
    public int getLineTop(int i) {
        return this.mInts.getValue(i, 1);
    }

    @Override
    public int getLineDescent(int i) {
        return this.mInts.getValue(i, 2);
    }

    @Override
    public int getLineExtra(int i) {
        return this.mInts.getValue(i, 3);
    }

    @Override
    public int getLineStart(int i) {
        return this.mInts.getValue(i, 0) & 536870911;
    }

    @Override
    public boolean getLineContainsTab(int i) {
        return (this.mInts.getValue(i, 0) & 536870912) != 0;
    }

    @Override
    public int getParagraphDirection(int i) {
        return this.mInts.getValue(i, 0) >> 30;
    }

    @Override
    public final Layout.Directions getLineDirections(int i) {
        return this.mObjects.getValue(i, 0);
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
        return this.mInts.getValue(i, 4) & 255;
    }

    private boolean getContentMayProtrudeFromTopOrBottom(int i) {
        return (this.mInts.getValue(i, 4) & 256) != 0;
    }

    @Override
    public int getEllipsizedWidth() {
        return this.mEllipsizedWidth;
    }

    private static class ChangeWatcher implements TextWatcher, SpanWatcher {
        private WeakReference<DynamicLayout> mLayout;

        public ChangeWatcher(DynamicLayout dynamicLayout) {
            this.mLayout = new WeakReference<>(dynamicLayout);
        }

        private void reflow(CharSequence charSequence, int i, int i2, int i3) {
            DynamicLayout dynamicLayout = this.mLayout.get();
            if (dynamicLayout != null) {
                dynamicLayout.reflow(charSequence, i, i2, i3);
            } else if (charSequence instanceof Spannable) {
                ((Spannable) charSequence).removeSpan(this);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            reflow(charSequence, i, i2, i3);
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }

        @Override
        public void onSpanAdded(Spannable spannable, Object obj, int i, int i2) {
            if (obj instanceof UpdateLayout) {
                int i3 = i2 - i;
                reflow(spannable, i, i3, i3);
            }
        }

        @Override
        public void onSpanRemoved(Spannable spannable, Object obj, int i, int i2) {
            if (obj instanceof UpdateLayout) {
                int i3 = i2 - i;
                reflow(spannable, i, i3, i3);
            }
        }

        @Override
        public void onSpanChanged(Spannable spannable, Object obj, int i, int i2, int i3, int i4) {
            if (obj instanceof UpdateLayout) {
                if (i > i2) {
                    i = 0;
                }
                int i5 = i2 - i;
                reflow(spannable, i, i5, i5);
                int i6 = i4 - i3;
                reflow(spannable, i3, i6, i6);
            }
        }
    }

    @Override
    public int getEllipsisStart(int i) {
        if (this.mEllipsizeAt == null) {
            return 0;
        }
        return this.mInts.getValue(i, 5);
    }

    @Override
    public int getEllipsisCount(int i) {
        if (this.mEllipsizeAt == null) {
            return 0;
        }
        return this.mInts.getValue(i, 6);
    }
}
