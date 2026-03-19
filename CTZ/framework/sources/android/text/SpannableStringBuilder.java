package android.text;

import android.graphics.BaseCanvas;
import android.graphics.Paint;
import android.net.wifi.WifiEnterpriseConfig;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.GrowingArrayUtils;
import java.lang.reflect.Array;
import java.util.IdentityHashMap;
import libcore.util.EmptyArray;

public class SpannableStringBuilder implements CharSequence, GetChars, Spannable, Editable, Appendable, GraphicsOperations {
    private static final int END_MASK = 15;
    private static final int MARK = 1;
    private static final int PARAGRAPH = 3;
    private static final int POINT = 2;
    private static final int SPAN_ADDED = 2048;
    private static final int SPAN_END_AT_END = 32768;
    private static final int SPAN_END_AT_START = 16384;
    private static final int SPAN_START_AT_END = 8192;
    private static final int SPAN_START_AT_START = 4096;
    private static final int SPAN_START_END_MASK = 61440;
    private static final int START_MASK = 240;
    private static final int START_SHIFT = 4;
    private static final String TAG = "SpannableStringBuilder";
    private InputFilter[] mFilters;
    private int mGapLength;
    private int mGapStart;
    private IdentityHashMap<Object, Integer> mIndexOfSpan;
    private int mLowWaterMark;
    private int mSpanCount;
    private int[] mSpanEnds;
    private int[] mSpanFlags;
    private int mSpanInsertCount;
    private int[] mSpanMax;
    private int[] mSpanOrder;
    private int[] mSpanStarts;
    private Object[] mSpans;
    private char[] mText;
    private int mTextWatcherDepth;
    private static final InputFilter[] NO_FILTERS = new InputFilter[0];

    @GuardedBy("sCachedIntBuffer")
    private static final int[][] sCachedIntBuffer = (int[][]) Array.newInstance((Class<?>) int.class, 6, 0);

    public SpannableStringBuilder() {
        this("");
    }

    public SpannableStringBuilder(CharSequence charSequence) {
        this(charSequence, 0, charSequence.length());
    }

    public SpannableStringBuilder(CharSequence charSequence, int i, int i2) {
        this.mFilters = NO_FILTERS;
        int i3 = i2 - i;
        if (i3 < 0) {
            throw new StringIndexOutOfBoundsException();
        }
        this.mText = ArrayUtils.newUnpaddedCharArray(GrowingArrayUtils.growSize(i3));
        this.mGapStart = i3;
        this.mGapLength = this.mText.length - i3;
        TextUtils.getChars(charSequence, i, i2, this.mText, 0);
        this.mSpanCount = 0;
        this.mSpanInsertCount = 0;
        this.mSpans = EmptyArray.OBJECT;
        this.mSpanStarts = EmptyArray.INT;
        this.mSpanEnds = EmptyArray.INT;
        this.mSpanFlags = EmptyArray.INT;
        this.mSpanMax = EmptyArray.INT;
        this.mSpanOrder = EmptyArray.INT;
        if (charSequence instanceof Spanned) {
            Spanned spanned = (Spanned) charSequence;
            Object[] spans = spanned.getSpans(i, i2, Object.class);
            for (int i4 = 0; i4 < spans.length; i4++) {
                if (!(spans[i4] instanceof NoCopySpan)) {
                    int spanStart = spanned.getSpanStart(spans[i4]) - i;
                    int spanEnd = spanned.getSpanEnd(spans[i4]) - i;
                    int spanFlags = spanned.getSpanFlags(spans[i4]);
                    spanStart = spanStart < 0 ? 0 : spanStart;
                    int i5 = spanStart > i3 ? i3 : spanStart;
                    spanEnd = spanEnd < 0 ? 0 : spanEnd;
                    setSpan(false, spans[i4], i5, spanEnd > i3 ? i3 : spanEnd, spanFlags, false);
                }
            }
            restoreInvariants();
        }
    }

    public static SpannableStringBuilder valueOf(CharSequence charSequence) {
        if (charSequence instanceof SpannableStringBuilder) {
            return (SpannableStringBuilder) charSequence;
        }
        return new SpannableStringBuilder(charSequence);
    }

    @Override
    public char charAt(int i) {
        int length = length();
        if (i < 0) {
            throw new IndexOutOfBoundsException("charAt: " + i + " < 0");
        }
        if (i >= length) {
            throw new IndexOutOfBoundsException("charAt: " + i + " >= length " + length);
        }
        if (i >= this.mGapStart) {
            return this.mText[i + this.mGapLength];
        }
        return this.mText[i];
    }

    @Override
    public int length() {
        return this.mText.length - this.mGapLength;
    }

    private void resizeFor(int i) {
        int length = this.mText.length;
        if (i + 1 <= length) {
            return;
        }
        char[] cArrNewUnpaddedCharArray = ArrayUtils.newUnpaddedCharArray(GrowingArrayUtils.growSize(i));
        System.arraycopy(this.mText, 0, cArrNewUnpaddedCharArray, 0, this.mGapStart);
        int length2 = cArrNewUnpaddedCharArray.length;
        int i2 = length2 - length;
        int i3 = length - (this.mGapStart + this.mGapLength);
        System.arraycopy(this.mText, length - i3, cArrNewUnpaddedCharArray, length2 - i3, i3);
        this.mText = cArrNewUnpaddedCharArray;
        this.mGapLength += i2;
        if (this.mGapLength < 1) {
            new Exception("mGapLength < 1").printStackTrace();
        }
        if (this.mSpanCount != 0) {
            for (int i4 = 0; i4 < this.mSpanCount; i4++) {
                if (this.mSpanStarts[i4] > this.mGapStart) {
                    int[] iArr = this.mSpanStarts;
                    iArr[i4] = iArr[i4] + i2;
                }
                if (this.mSpanEnds[i4] > this.mGapStart) {
                    int[] iArr2 = this.mSpanEnds;
                    iArr2[i4] = iArr2[i4] + i2;
                }
            }
            calcMax(treeRoot());
        }
    }

    private void moveGapTo(int i) {
        int i2;
        int i3;
        if (i == this.mGapStart) {
            return;
        }
        boolean z = i == length();
        if (i < this.mGapStart) {
            int i4 = this.mGapStart - i;
            System.arraycopy(this.mText, i, this.mText, (this.mGapStart + this.mGapLength) - i4, i4);
        } else {
            int i5 = i - this.mGapStart;
            System.arraycopy(this.mText, (this.mGapLength + i) - i5, this.mText, this.mGapStart, i5);
        }
        if (this.mSpanCount != 0) {
            for (int i6 = 0; i6 < this.mSpanCount; i6++) {
                int i7 = this.mSpanStarts[i6];
                int i8 = this.mSpanEnds[i6];
                if (i7 > this.mGapStart) {
                    i7 -= this.mGapLength;
                }
                if (i7 > i) {
                    i7 += this.mGapLength;
                } else if (i7 == i && ((i2 = (this.mSpanFlags[i6] & 240) >> 4) == 2 || (z && i2 == 3))) {
                    i7 += this.mGapLength;
                }
                if (i8 > this.mGapStart) {
                    i8 -= this.mGapLength;
                }
                if (i8 > i) {
                    i8 += this.mGapLength;
                } else if (i8 == i && ((i3 = this.mSpanFlags[i6] & 15) == 2 || (z && i3 == 3))) {
                    i8 += this.mGapLength;
                }
                this.mSpanStarts[i6] = i7;
                this.mSpanEnds[i6] = i8;
            }
            calcMax(treeRoot());
        }
        this.mGapStart = i;
    }

    @Override
    public SpannableStringBuilder insert(int i, CharSequence charSequence, int i2, int i3) {
        return replace(i, i, charSequence, i2, i3);
    }

    @Override
    public SpannableStringBuilder insert(int i, CharSequence charSequence) {
        return replace(i, i, charSequence, 0, charSequence.length());
    }

    @Override
    public SpannableStringBuilder delete(int i, int i2) {
        SpannableStringBuilder spannableStringBuilderReplace = replace(i, i2, "", 0, 0);
        if (this.mGapLength > 2 * length()) {
            resizeFor(length());
        }
        return spannableStringBuilderReplace;
    }

    @Override
    public void clear() {
        replace(0, length(), "", 0, 0);
        this.mSpanInsertCount = 0;
    }

    @Override
    public void clearSpans() {
        for (int i = this.mSpanCount - 1; i >= 0; i--) {
            Object obj = this.mSpans[i];
            int i2 = this.mSpanStarts[i];
            int i3 = this.mSpanEnds[i];
            if (i2 > this.mGapStart) {
                i2 -= this.mGapLength;
            }
            if (i3 > this.mGapStart) {
                i3 -= this.mGapLength;
            }
            this.mSpanCount = i;
            this.mSpans[i] = null;
            sendSpanRemoved(obj, i2, i3);
        }
        if (this.mIndexOfSpan != null) {
            this.mIndexOfSpan.clear();
        }
        this.mSpanInsertCount = 0;
    }

    @Override
    public SpannableStringBuilder append(CharSequence charSequence) {
        int length = length();
        return replace(length, length, charSequence, 0, charSequence.length());
    }

    public SpannableStringBuilder append(CharSequence charSequence, Object obj, int i) {
        int length = length();
        append(charSequence);
        setSpan(obj, length, length(), i);
        return this;
    }

    @Override
    public SpannableStringBuilder append(CharSequence charSequence, int i, int i2) {
        int length = length();
        return replace(length, length, charSequence, i, i2);
    }

    @Override
    public SpannableStringBuilder append(char c) {
        return append((CharSequence) String.valueOf(c));
    }

    private boolean removeSpansForChange(int i, int i2, boolean z, int i3) {
        int i4 = i3 & 1;
        if (i4 != 0 && resolveGap(this.mSpanMax[i3]) >= i && removeSpansForChange(i, i2, z, leftChild(i3))) {
            return true;
        }
        if (i3 >= this.mSpanCount) {
            return false;
        }
        if ((this.mSpanFlags[i3] & 33) != 33 || this.mSpanStarts[i3] < i || this.mSpanStarts[i3] >= this.mGapStart + this.mGapLength || this.mSpanEnds[i3] < i || this.mSpanEnds[i3] >= this.mGapStart + this.mGapLength || (!z && this.mSpanStarts[i3] <= i && this.mSpanEnds[i3] >= this.mGapStart)) {
            return resolveGap(this.mSpanStarts[i3]) <= i2 && i4 != 0 && removeSpansForChange(i, i2, z, rightChild(i3));
        }
        this.mIndexOfSpan.remove(this.mSpans[i3]);
        removeSpan(i3, 0);
        return true;
    }

    private void change(int i, int i2, CharSequence charSequence, int i3, int i4) {
        int i5;
        int i6;
        int i7;
        int i8;
        int i9;
        int i10 = i2 - i;
        int i11 = i4 - i3;
        int i12 = i11 - i10;
        int i13 = 1;
        int i14 = this.mSpanCount - 1;
        boolean z = false;
        while (i14 >= 0) {
            int i15 = this.mSpanStarts[i14];
            if (i15 > this.mGapStart) {
                i15 -= this.mGapLength;
            }
            int i16 = this.mSpanEnds[i14];
            if (i16 > this.mGapStart) {
                i16 -= this.mGapLength;
            }
            if ((this.mSpanFlags[i14] & 51) == 51) {
                int length = length();
                if (i15 <= i || i15 > i2) {
                    i7 = i15;
                } else {
                    int i17 = i2;
                    while (i17 < length && (i17 <= i2 || charAt(i17 - 1) != '\n')) {
                        i17++;
                    }
                    i7 = i17;
                }
                if (i16 > i && i16 <= i2) {
                    i8 = i2;
                    while (i8 < length) {
                        if (i8 > i2) {
                            i9 = length;
                            if (charAt(i8 - 1) == '\n') {
                                break;
                            }
                        } else {
                            i9 = length;
                        }
                        i8++;
                        length = i9;
                    }
                } else {
                    i8 = i16;
                }
                if (i7 != i15 || i8 != i16) {
                    i5 = 1;
                    setSpan(false, this.mSpans[i14], i7, i8, this.mSpanFlags[i14], true);
                    z = true;
                    i15 = i7;
                    i16 = i8;
                } else {
                    i16 = i8;
                    i15 = i7;
                    i5 = 1;
                }
            } else {
                i5 = i13;
            }
            if (i15 == i) {
                i6 = 4096;
            } else {
                i6 = i15 == i2 + i12 ? 8192 : 0;
            }
            if (i16 == i) {
                i6 |= 16384;
            } else if (i16 == i2 + i12) {
                i6 |= 32768;
            }
            int[] iArr = this.mSpanFlags;
            iArr[i14] = i6 | iArr[i14];
            i14--;
            i13 = i5;
        }
        int i18 = i13;
        if (z) {
            restoreInvariants();
        }
        moveGapTo(i2);
        if (i12 >= this.mGapLength) {
            resizeFor((this.mText.length + i12) - this.mGapLength);
        }
        boolean z2 = i11 == 0 ? i18 : 0;
        if (i10 > 0) {
            while (this.mSpanCount > 0 && removeSpansForChange(i, i2, z2, treeRoot())) {
            }
        }
        this.mGapStart += i12;
        this.mGapLength -= i12;
        if (this.mGapLength < i18) {
            new Exception("mGapLength < 1").printStackTrace();
        }
        TextUtils.getChars(charSequence, i3, i4, this.mText, i);
        if (i10 > 0) {
            int i19 = this.mGapStart + this.mGapLength == this.mText.length ? i18 : 0;
            for (int i20 = 0; i20 < this.mSpanCount; i20++) {
                boolean z3 = i19;
                this.mSpanStarts[i20] = updatedIntervalBound(this.mSpanStarts[i20], i, i12, (this.mSpanFlags[i20] & 240) >> 4, z3, z2);
                this.mSpanEnds[i20] = updatedIntervalBound(this.mSpanEnds[i20], i, i12, this.mSpanFlags[i20] & 15, z3, z2);
            }
            restoreInvariants();
        }
        if (charSequence instanceof Spanned) {
            Spanned spanned = (Spanned) charSequence;
            Object[] spans = spanned.getSpans(i3, i4, Object.class);
            for (int i21 = 0; i21 < spans.length; i21++) {
                int spanStart = spanned.getSpanStart(spans[i21]);
                int spanEnd = spanned.getSpanEnd(spans[i21]);
                if (spanStart < i3) {
                    spanStart = i3;
                }
                if (spanEnd > i4) {
                    spanEnd = i4;
                }
                if (getSpanStart(spans[i21]) < 0) {
                    setSpan(false, spans[i21], (spanStart - i3) + i, (spanEnd - i3) + i, spanned.getSpanFlags(spans[i21]) | 2048, false);
                }
            }
            restoreInvariants();
        }
    }

    private int updatedIntervalBound(int i, int i2, int i3, int i4, boolean z, boolean z2) {
        if (i >= i2 && i < this.mGapStart + this.mGapLength) {
            if (i4 == 2) {
                if (z2 || i > i2) {
                    return this.mGapStart + this.mGapLength;
                }
            } else if (i4 == 3) {
                if (z) {
                    return this.mGapStart + this.mGapLength;
                }
            } else {
                if (z2 || i < this.mGapStart - i3) {
                    return i2;
                }
                return this.mGapStart;
            }
        }
        return i;
    }

    private void removeSpan(int i, int i2) {
        Object obj = this.mSpans[i];
        int i3 = this.mSpanStarts[i];
        int i4 = this.mSpanEnds[i];
        if (i3 > this.mGapStart) {
            i3 -= this.mGapLength;
        }
        if (i4 > this.mGapStart) {
            i4 -= this.mGapLength;
        }
        int i5 = i + 1;
        int i6 = this.mSpanCount - i5;
        System.arraycopy(this.mSpans, i5, this.mSpans, i, i6);
        System.arraycopy(this.mSpanStarts, i5, this.mSpanStarts, i, i6);
        System.arraycopy(this.mSpanEnds, i5, this.mSpanEnds, i, i6);
        System.arraycopy(this.mSpanFlags, i5, this.mSpanFlags, i, i6);
        System.arraycopy(this.mSpanOrder, i5, this.mSpanOrder, i, i6);
        this.mSpanCount--;
        invalidateIndex(i);
        this.mSpans[this.mSpanCount] = null;
        restoreInvariants();
        if ((i2 & 512) == 0) {
            sendSpanRemoved(obj, i3, i4);
        }
    }

    @Override
    public SpannableStringBuilder replace(int i, int i2, CharSequence charSequence) {
        return replace(i, i2, charSequence, 0, charSequence.length());
    }

    @Override
    public SpannableStringBuilder replace(int i, int i2, CharSequence charSequence, int i3, int i4) {
        int selectionEnd;
        int selectionStart;
        TextWatcher[] textWatcherArr;
        checkRange("replace", i, i2);
        int length = this.mFilters.length;
        boolean z = false;
        CharSequence charSequence2 = charSequence;
        int i5 = i3;
        int length2 = i4;
        for (int i6 = 0; i6 < length; i6++) {
            CharSequence charSequenceFilter = this.mFilters[i6].filter(charSequence2, i5, length2, this, i, i2);
            if (charSequenceFilter != null) {
                charSequence2 = charSequenceFilter;
                length2 = charSequenceFilter.length();
                i5 = 0;
            }
        }
        int i7 = i2 - i;
        int i8 = length2 - i5;
        if (i7 == 0 && i8 == 0 && !hasNonExclusiveExclusiveSpanAt(charSequence2, i5)) {
            return this;
        }
        TextWatcher[] textWatcherArr2 = (TextWatcher[]) getSpans(i, i + i7, TextWatcher.class);
        sendBeforeTextChanged(textWatcherArr2, i, i7, i8);
        boolean z2 = (i7 == 0 || i8 == 0) ? false : true;
        if (!z2) {
            selectionEnd = 0;
            selectionStart = 0;
        } else {
            selectionStart = Selection.getSelectionStart(this);
            selectionEnd = Selection.getSelectionEnd(this);
        }
        CharSequence charSequence3 = charSequence2;
        int i9 = selectionEnd;
        int i10 = i5;
        int i11 = selectionStart;
        change(i, i2, charSequence3, i10, length2);
        if (z2) {
            if (i11 <= i || i11 >= i2) {
                textWatcherArr = textWatcherArr2;
            } else {
                int intExact = i + Math.toIntExact((((long) (i11 - i)) * ((long) i8)) / ((long) i7));
                textWatcherArr = textWatcherArr2;
                setSpan(false, Selection.SELECTION_START, intExact, intExact, 34, true);
                z = true;
            }
            if (i9 > i && i9 < i2) {
                int intExact2 = i + Math.toIntExact((((long) (i9 - i)) * ((long) i8)) / ((long) i7));
                setSpan(false, Selection.SELECTION_END, intExact2, intExact2, 34, true);
                z = true;
            }
            if (z) {
                restoreInvariants();
            }
        } else {
            textWatcherArr = textWatcherArr2;
        }
        sendTextChanged(textWatcherArr, i, i7, i8);
        sendAfterTextChanged(textWatcherArr);
        sendToSpanWatchers(i, i2, i8 - i7);
        return this;
    }

    private static boolean hasNonExclusiveExclusiveSpanAt(CharSequence charSequence, int i) {
        if (charSequence instanceof Spanned) {
            Spanned spanned = (Spanned) charSequence;
            for (Object obj : spanned.getSpans(i, i, Object.class)) {
                if (spanned.getSpanFlags(obj) != 33) {
                    return true;
                }
            }
        }
        return false;
    }

    private void sendToSpanWatchers(int i, int i2, int i3) {
        boolean z;
        int i4;
        int i5;
        for (int i6 = 0; i6 < this.mSpanCount; i6++) {
            int i7 = this.mSpanFlags[i6];
            if ((i7 & 2048) == 0) {
                int i8 = this.mSpanStarts[i6];
                int i9 = this.mSpanEnds[i6];
                if (i8 > this.mGapStart) {
                    i8 -= this.mGapLength;
                }
                int i10 = i8;
                if (i9 > this.mGapStart) {
                    i9 -= this.mGapLength;
                }
                int i11 = i9;
                int i12 = i2 + i3;
                boolean z2 = true;
                if (i10 > i12) {
                    if (i3 != 0) {
                        i4 = i10 - i3;
                        z = true;
                        if (i11 > i12) {
                            if (i3 != 0) {
                                i5 = i11 - i3;
                                if (z2) {
                                    sendSpanChanged(this.mSpans[i6], i4, i5, i10, i11);
                                }
                                int[] iArr = this.mSpanFlags;
                                iArr[i6] = iArr[i6] & (-61441);
                            }
                        } else {
                            if (i11 < i || ((i11 == i && (i7 & 16384) == 16384) || (i11 == i12 && (i7 & 32768) == 32768))) {
                            }
                            i5 = i11;
                            if (z2) {
                            }
                            int[] iArr2 = this.mSpanFlags;
                            iArr2[i6] = iArr2[i6] & (-61441);
                        }
                        z2 = z;
                        i5 = i11;
                        if (z2) {
                        }
                        int[] iArr22 = this.mSpanFlags;
                        iArr22[i6] = iArr22[i6] & (-61441);
                    }
                } else {
                    if (i10 >= i && ((i10 != i || (i7 & 4096) != 4096) && (i10 != i12 || (i7 & 8192) != 8192))) {
                        z = true;
                    }
                    i4 = i10;
                    if (i11 > i12) {
                    }
                    z2 = z;
                    i5 = i11;
                    if (z2) {
                    }
                    int[] iArr222 = this.mSpanFlags;
                    iArr222[i6] = iArr222[i6] & (-61441);
                }
                z = false;
                i4 = i10;
                if (i11 > i12) {
                }
                z2 = z;
                i5 = i11;
                if (z2) {
                }
                int[] iArr2222 = this.mSpanFlags;
                iArr2222[i6] = iArr2222[i6] & (-61441);
            }
        }
        for (int i13 = 0; i13 < this.mSpanCount; i13++) {
            if ((this.mSpanFlags[i13] & 2048) != 0) {
                int[] iArr3 = this.mSpanFlags;
                iArr3[i13] = iArr3[i13] & (-2049);
                int i14 = this.mSpanStarts[i13];
                int i15 = this.mSpanEnds[i13];
                if (i14 > this.mGapStart) {
                    i14 -= this.mGapLength;
                }
                if (i15 > this.mGapStart) {
                    i15 -= this.mGapLength;
                }
                sendSpanAdded(this.mSpans[i13], i14, i15);
            }
        }
    }

    @Override
    public void setSpan(Object obj, int i, int i2, int i3) {
        setSpan(true, obj, i, i2, i3, true);
    }

    private void setSpan(boolean z, Object obj, int i, int i2, int i3, boolean z2) {
        int i4;
        int i5;
        Integer num;
        checkRange("setSpan", i, i2);
        int i6 = (i3 & 240) >> 4;
        if (isInvalidParagraph(i, i6)) {
            if (!z2) {
                return;
            }
            throw new RuntimeException("PARAGRAPH span must start at paragraph boundary (" + i + " follows " + charAt(i - 1) + ")");
        }
        int i7 = i3 & 15;
        if (isInvalidParagraph(i2, i7)) {
            if (!z2) {
                return;
            }
            throw new RuntimeException("PARAGRAPH span must end at paragraph boundary (" + i2 + " follows " + charAt(i2 - 1) + ")");
        }
        if (i6 == 2 && i7 == 1 && i == i2) {
            if (z) {
                Log.e(TAG, "SPAN_EXCLUSIVE_EXCLUSIVE spans cannot have a zero length");
                return;
            }
            return;
        }
        if (i > this.mGapStart) {
            i4 = this.mGapLength + i;
        } else if (i == this.mGapStart && (i6 == 2 || (i6 == 3 && i == length()))) {
            i4 = this.mGapLength + i;
        } else {
            i4 = i;
        }
        if (i2 > this.mGapStart) {
            i5 = this.mGapLength + i2;
        } else if (i2 == this.mGapStart && (i7 == 2 || (i7 == 3 && i2 == length()))) {
            i5 = this.mGapLength + i2;
        } else {
            i5 = i2;
        }
        if (this.mIndexOfSpan != null && (num = this.mIndexOfSpan.get(obj)) != null) {
            int iIntValue = num.intValue();
            int i8 = this.mSpanStarts[iIntValue];
            int i9 = this.mSpanEnds[iIntValue];
            if (i8 > this.mGapStart) {
                i8 -= this.mGapLength;
            }
            if (i9 > this.mGapStart) {
                i9 -= this.mGapLength;
            }
            this.mSpanStarts[iIntValue] = i4;
            this.mSpanEnds[iIntValue] = i5;
            this.mSpanFlags[iIntValue] = i3;
            if (z) {
                restoreInvariants();
                sendSpanChanged(obj, i8, i9, i, i2);
                return;
            }
            return;
        }
        this.mSpans = GrowingArrayUtils.append(this.mSpans, this.mSpanCount, obj);
        this.mSpanStarts = GrowingArrayUtils.append(this.mSpanStarts, this.mSpanCount, i4);
        this.mSpanEnds = GrowingArrayUtils.append(this.mSpanEnds, this.mSpanCount, i5);
        this.mSpanFlags = GrowingArrayUtils.append(this.mSpanFlags, this.mSpanCount, i3);
        this.mSpanOrder = GrowingArrayUtils.append(this.mSpanOrder, this.mSpanCount, this.mSpanInsertCount);
        invalidateIndex(this.mSpanCount);
        this.mSpanCount++;
        this.mSpanInsertCount++;
        int iTreeRoot = (2 * treeRoot()) + 1;
        if (this.mSpanMax.length < iTreeRoot) {
            this.mSpanMax = new int[iTreeRoot];
        }
        if (z) {
            restoreInvariants();
            sendSpanAdded(obj, i, i2);
        }
    }

    private boolean isInvalidParagraph(int i, int i2) {
        return (i2 != 3 || i == 0 || i == length() || charAt(i - 1) == '\n') ? false : true;
    }

    @Override
    public void removeSpan(Object obj) {
        removeSpan(obj, 0);
    }

    @Override
    public void removeSpan(Object obj, int i) {
        Integer numRemove;
        if (this.mIndexOfSpan != null && (numRemove = this.mIndexOfSpan.remove(obj)) != null) {
            removeSpan(numRemove.intValue(), i);
        }
    }

    private int resolveGap(int i) {
        return i > this.mGapStart ? i - this.mGapLength : i;
    }

    @Override
    public int getSpanStart(Object obj) {
        Integer num;
        if (this.mIndexOfSpan == null || (num = this.mIndexOfSpan.get(obj)) == null) {
            return -1;
        }
        return resolveGap(this.mSpanStarts[num.intValue()]);
    }

    @Override
    public int getSpanEnd(Object obj) {
        Integer num;
        if (this.mIndexOfSpan == null || (num = this.mIndexOfSpan.get(obj)) == null) {
            return -1;
        }
        return resolveGap(this.mSpanEnds[num.intValue()]);
    }

    @Override
    public int getSpanFlags(Object obj) {
        Integer num;
        if (this.mIndexOfSpan == null || (num = this.mIndexOfSpan.get(obj)) == null) {
            return 0;
        }
        return this.mSpanFlags[num.intValue()];
    }

    @Override
    public <T> T[] getSpans(int i, int i2, Class<T> cls) {
        return (T[]) getSpans(i, i2, cls, true);
    }

    public <T> T[] getSpans(int i, int i2, Class<T> cls, boolean z) {
        if (cls == null) {
            return (T[]) ArrayUtils.emptyArray(Object.class);
        }
        if (this.mSpanCount == 0) {
            return (T[]) ArrayUtils.emptyArray(cls);
        }
        int iCountSpans = countSpans(i, i2, cls, treeRoot());
        if (iCountSpans == 0) {
            return (T[]) ArrayUtils.emptyArray(cls);
        }
        T[] tArr = (T[]) ((Object[]) Array.newInstance((Class<?>) cls, iCountSpans));
        int[] iArrObtain = z ? obtain(iCountSpans) : EmptyArray.INT;
        int[] iArrObtain2 = z ? obtain(iCountSpans) : EmptyArray.INT;
        getSpansRec(i, i2, cls, treeRoot(), tArr, iArrObtain, iArrObtain2, 0, z);
        if (z) {
            sort(tArr, iArrObtain, iArrObtain2);
            recycle(iArrObtain);
            recycle(iArrObtain2);
        }
        return tArr;
    }

    private int countSpans(int i, int i2, Class cls, int i3) {
        int iCountSpans;
        int i4 = i3 & 1;
        if (i4 != 0) {
            int iLeftChild = leftChild(i3);
            int i5 = this.mSpanMax[iLeftChild];
            if (i5 > this.mGapStart) {
                i5 -= this.mGapLength;
            }
            if (i5 >= i) {
                iCountSpans = countSpans(i, i2, cls, iLeftChild);
            }
        } else {
            iCountSpans = 0;
        }
        if (i3 < this.mSpanCount) {
            int i6 = this.mSpanStarts[i3];
            if (i6 > this.mGapStart) {
                i6 -= this.mGapLength;
            }
            if (i6 <= i2) {
                int i7 = this.mSpanEnds[i3];
                if (i7 > this.mGapStart) {
                    i7 -= this.mGapLength;
                }
                if (i7 >= i && ((i6 == i7 || i == i2 || (i6 != i2 && i7 != i)) && (Object.class == cls || cls.isInstance(this.mSpans[i3])))) {
                    iCountSpans++;
                }
                if (i4 != 0) {
                    return iCountSpans + countSpans(i, i2, cls, rightChild(i3));
                }
                return iCountSpans;
            }
            return iCountSpans;
        }
        return iCountSpans;
    }

    private <T> int getSpansRec(int i, int i2, Class<T> cls, int i3, T[] tArr, int[] iArr, int[] iArr2, int i4, boolean z) {
        int spansRec;
        int i5;
        int i6 = i3 & 1;
        if (i6 != 0) {
            int iLeftChild = leftChild(i3);
            int i7 = this.mSpanMax[iLeftChild];
            if (i7 > this.mGapStart) {
                i7 -= this.mGapLength;
            }
            if (i7 >= i) {
                spansRec = getSpansRec(i, i2, cls, iLeftChild, tArr, iArr, iArr2, i4, z);
            }
        } else {
            spansRec = i4;
        }
        if (i3 >= this.mSpanCount) {
            return spansRec;
        }
        int i8 = this.mSpanStarts[i3];
        if (i8 > this.mGapStart) {
            i8 -= this.mGapLength;
        }
        if (i8 > i2) {
            return spansRec;
        }
        int i9 = this.mSpanEnds[i3];
        if (i9 > this.mGapStart) {
            i9 -= this.mGapLength;
        }
        if (i9 >= i && ((i8 == i9 || i == i2 || (i8 != i2 && i9 != i)) && (Object.class == cls || cls.isInstance(this.mSpans[i3])))) {
            int i10 = this.mSpanFlags[i3] & Spanned.SPAN_PRIORITY;
            if (z) {
                iArr[spansRec] = i10;
                iArr2[spansRec] = this.mSpanOrder[i3];
            } else {
                if (i10 != 0) {
                    i5 = 0;
                    while (i5 < spansRec && i10 <= (getSpanFlags(tArr[i5]) & Spanned.SPAN_PRIORITY)) {
                        i5++;
                    }
                    System.arraycopy(tArr, i5, tArr, i5 + 1, spansRec - i5);
                }
                tArr[i5] = this.mSpans[i3];
                spansRec++;
            }
            i5 = spansRec;
            tArr[i5] = this.mSpans[i3];
            spansRec++;
        }
        int i11 = spansRec;
        if (i11 < tArr.length && i6 != 0) {
            return getSpansRec(i, i2, cls, rightChild(i3), tArr, iArr, iArr2, i11, z);
        }
        return i11;
    }

    private static int[] obtain(int i) {
        int[] iArr;
        synchronized (sCachedIntBuffer) {
            int length = sCachedIntBuffer.length - 1;
            int i2 = -1;
            while (true) {
                if (length >= 0) {
                    if (sCachedIntBuffer[length] != null) {
                        if (sCachedIntBuffer[length].length >= i) {
                            break;
                        }
                        if (i2 == -1) {
                            i2 = length;
                        }
                    }
                    length--;
                } else {
                    length = i2;
                    break;
                }
            }
            if (length != -1) {
                iArr = sCachedIntBuffer[length];
                sCachedIntBuffer[length] = null;
            } else {
                iArr = null;
            }
        }
        return checkSortBuffer(iArr, i);
    }

    private static void recycle(int[] iArr) {
        synchronized (sCachedIntBuffer) {
            for (int i = 0; i < sCachedIntBuffer.length; i++) {
                if (sCachedIntBuffer[i] != null && iArr.length <= sCachedIntBuffer[i].length) {
                }
                sCachedIntBuffer[i] = iArr;
            }
        }
    }

    private static int[] checkSortBuffer(int[] iArr, int i) {
        if (iArr == null || i > iArr.length) {
            return ArrayUtils.newUnpaddedIntArray(GrowingArrayUtils.growSize(i));
        }
        return iArr;
    }

    private final <T> void sort(T[] tArr, int[] iArr, int[] iArr2) {
        int length = tArr.length;
        for (int i = (length / 2) - 1; i >= 0; i--) {
            siftDown(i, tArr, length, iArr, iArr2);
        }
        for (int i2 = length - 1; i2 > 0; i2--) {
            T t = tArr[0];
            tArr[0] = tArr[i2];
            tArr[i2] = t;
            int i3 = iArr[0];
            iArr[0] = iArr[i2];
            iArr[i2] = i3;
            int i4 = iArr2[0];
            iArr2[0] = iArr2[i2];
            iArr2[i2] = i4;
            siftDown(0, tArr, i2, iArr, iArr2);
        }
    }

    private final <T> void siftDown(int i, T[] tArr, int i2, int[] iArr, int[] iArr2) {
        int i3 = (2 * i) + 1;
        while (i3 < i2) {
            if (i3 < i2 - 1) {
                int i4 = i3 + 1;
                if (compareSpans(i3, i4, iArr, iArr2) < 0) {
                    i3 = i4;
                }
            }
            if (compareSpans(i, i3, iArr, iArr2) < 0) {
                T t = tArr[i];
                tArr[i] = tArr[i3];
                tArr[i3] = t;
                int i5 = iArr[i];
                iArr[i] = iArr[i3];
                iArr[i3] = i5;
                int i6 = iArr2[i];
                iArr2[i] = iArr2[i3];
                iArr2[i3] = i6;
                int i7 = i3;
                i3 = (2 * i3) + 1;
                i = i7;
            } else {
                return;
            }
        }
    }

    private final int compareSpans(int i, int i2, int[] iArr, int[] iArr2) {
        int i3 = iArr[i];
        int i4 = iArr[i2];
        if (i3 == i4) {
            return Integer.compare(iArr2[i], iArr2[i2]);
        }
        return Integer.compare(i4, i3);
    }

    @Override
    public int nextSpanTransition(int i, int i2, Class cls) {
        if (this.mSpanCount == 0) {
            return i2;
        }
        if (cls == null) {
            cls = Object.class;
        }
        return nextSpanTransitionRec(i, i2, cls, treeRoot());
    }

    private int nextSpanTransitionRec(int i, int i2, Class cls, int i3) {
        int i4 = i3 & 1;
        if (i4 != 0) {
            int iLeftChild = leftChild(i3);
            if (resolveGap(this.mSpanMax[iLeftChild]) > i) {
                i2 = nextSpanTransitionRec(i, i2, cls, iLeftChild);
            }
        }
        if (i3 < this.mSpanCount) {
            int iResolveGap = resolveGap(this.mSpanStarts[i3]);
            int iResolveGap2 = resolveGap(this.mSpanEnds[i3]);
            if (iResolveGap > i && iResolveGap < i2 && cls.isInstance(this.mSpans[i3])) {
                i2 = iResolveGap;
            }
            if (iResolveGap2 > i && iResolveGap2 < i2 && cls.isInstance(this.mSpans[i3])) {
                i2 = iResolveGap2;
            }
            if (iResolveGap < i2 && i4 != 0) {
                return nextSpanTransitionRec(i, i2, cls, rightChild(i3));
            }
            return i2;
        }
        return i2;
    }

    @Override
    public CharSequence subSequence(int i, int i2) {
        return new SpannableStringBuilder(this, i, i2);
    }

    @Override
    public void getChars(int i, int i2, char[] cArr, int i3) {
        checkRange("getChars", i, i2);
        if (i2 <= this.mGapStart) {
            System.arraycopy(this.mText, i, cArr, i3, i2 - i);
        } else if (i >= this.mGapStart) {
            System.arraycopy(this.mText, this.mGapLength + i, cArr, i3, i2 - i);
        } else {
            System.arraycopy(this.mText, i, cArr, i3, this.mGapStart - i);
            System.arraycopy(this.mText, this.mGapStart + this.mGapLength, cArr, i3 + (this.mGapStart - i), i2 - this.mGapStart);
        }
    }

    @Override
    public String toString() {
        int length = length();
        char[] cArr = new char[length];
        getChars(0, length, cArr, 0);
        return new String(cArr);
    }

    public String substring(int i, int i2) {
        char[] cArr = new char[i2 - i];
        getChars(i, i2, cArr, 0);
        return new String(cArr);
    }

    public int getTextWatcherDepth() {
        return this.mTextWatcherDepth;
    }

    private void sendBeforeTextChanged(TextWatcher[] textWatcherArr, int i, int i2, int i3) {
        this.mTextWatcherDepth++;
        for (TextWatcher textWatcher : textWatcherArr) {
            textWatcher.beforeTextChanged(this, i, i2, i3);
        }
        this.mTextWatcherDepth--;
    }

    private void sendTextChanged(TextWatcher[] textWatcherArr, int i, int i2, int i3) {
        this.mTextWatcherDepth++;
        for (TextWatcher textWatcher : textWatcherArr) {
            textWatcher.onTextChanged(this, i, i2, i3);
        }
        this.mTextWatcherDepth--;
    }

    private void sendAfterTextChanged(TextWatcher[] textWatcherArr) {
        this.mTextWatcherDepth++;
        for (TextWatcher textWatcher : textWatcherArr) {
            textWatcher.afterTextChanged(this);
        }
        this.mTextWatcherDepth--;
    }

    private void sendSpanAdded(Object obj, int i, int i2) {
        for (SpanWatcher spanWatcher : (SpanWatcher[]) getSpans(i, i2, SpanWatcher.class)) {
            spanWatcher.onSpanAdded(this, obj, i, i2);
        }
    }

    private void sendSpanRemoved(Object obj, int i, int i2) {
        for (SpanWatcher spanWatcher : (SpanWatcher[]) getSpans(i, i2, SpanWatcher.class)) {
            spanWatcher.onSpanRemoved(this, obj, i, i2);
        }
    }

    private void sendSpanChanged(Object obj, int i, int i2, int i3, int i4) {
        for (SpanWatcher spanWatcher : (SpanWatcher[]) getSpans(Math.min(i, i3), Math.min(Math.max(i2, i4), length()), SpanWatcher.class)) {
            spanWatcher.onSpanChanged(this, obj, i, i2, i3, i4);
        }
    }

    private static String region(int i, int i2) {
        return "(" + i + " ... " + i2 + ")";
    }

    private void checkRange(String str, int i, int i2) {
        if (i2 < i) {
            throw new IndexOutOfBoundsException(str + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + region(i, i2) + " has end before start");
        }
        int length = length();
        if (i > length || i2 > length) {
            throw new IndexOutOfBoundsException(str + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + region(i, i2) + " ends beyond length " + length);
        }
        if (i < 0 || i2 < 0) {
            throw new IndexOutOfBoundsException(str + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + region(i, i2) + " starts before 0");
        }
    }

    @Override
    public void drawText(BaseCanvas baseCanvas, int i, int i2, float f, float f2, Paint paint) {
        checkRange("drawText", i, i2);
        if (i2 <= this.mGapStart) {
            baseCanvas.drawText(this.mText, i, i2 - i, f, f2, paint);
            return;
        }
        if (i >= this.mGapStart) {
            baseCanvas.drawText(this.mText, i + this.mGapLength, i2 - i, f, f2, paint);
            return;
        }
        int i3 = i2 - i;
        char[] cArrObtain = TextUtils.obtain(i3);
        getChars(i, i2, cArrObtain, 0);
        baseCanvas.drawText(cArrObtain, 0, i3, f, f2, paint);
        TextUtils.recycle(cArrObtain);
    }

    @Override
    public void drawTextRun(BaseCanvas baseCanvas, int i, int i2, int i3, int i4, float f, float f2, boolean z, Paint paint) {
        checkRange("drawTextRun", i, i2);
        int i5 = i4 - i3;
        int i6 = i2 - i;
        if (i4 <= this.mGapStart) {
            baseCanvas.drawTextRun(this.mText, i, i6, i3, i5, f, f2, z, paint);
            return;
        }
        if (i3 >= this.mGapStart) {
            baseCanvas.drawTextRun(this.mText, i + this.mGapLength, i6, i3 + this.mGapLength, i5, f, f2, z, paint);
            return;
        }
        char[] cArrObtain = TextUtils.obtain(i5);
        getChars(i3, i4, cArrObtain, 0);
        baseCanvas.drawTextRun(cArrObtain, i - i3, i6, 0, i5, f, f2, z, paint);
        TextUtils.recycle(cArrObtain);
    }

    @Override
    public float measureText(int i, int i2, Paint paint) {
        checkRange("measureText", i, i2);
        if (i2 <= this.mGapStart) {
            return paint.measureText(this.mText, i, i2 - i);
        }
        if (i >= this.mGapStart) {
            return paint.measureText(this.mText, this.mGapLength + i, i2 - i);
        }
        int i3 = i2 - i;
        char[] cArrObtain = TextUtils.obtain(i3);
        getChars(i, i2, cArrObtain, 0);
        float fMeasureText = paint.measureText(cArrObtain, 0, i3);
        TextUtils.recycle(cArrObtain);
        return fMeasureText;
    }

    @Override
    public int getTextWidths(int i, int i2, float[] fArr, Paint paint) {
        checkRange("getTextWidths", i, i2);
        if (i2 <= this.mGapStart) {
            return paint.getTextWidths(this.mText, i, i2 - i, fArr);
        }
        if (i >= this.mGapStart) {
            return paint.getTextWidths(this.mText, this.mGapLength + i, i2 - i, fArr);
        }
        int i3 = i2 - i;
        char[] cArrObtain = TextUtils.obtain(i3);
        getChars(i, i2, cArrObtain, 0);
        int textWidths = paint.getTextWidths(cArrObtain, 0, i3, fArr);
        TextUtils.recycle(cArrObtain);
        return textWidths;
    }

    @Override
    public float getTextRunAdvances(int i, int i2, int i3, int i4, boolean z, float[] fArr, int i5, Paint paint) {
        int i6 = i4 - i3;
        int i7 = i2 - i;
        if (i2 <= this.mGapStart) {
            return paint.getTextRunAdvances(this.mText, i, i7, i3, i6, z, fArr, i5);
        }
        if (i >= this.mGapStart) {
            return paint.getTextRunAdvances(this.mText, i + this.mGapLength, i7, i3 + this.mGapLength, i6, z, fArr, i5);
        }
        char[] cArrObtain = TextUtils.obtain(i6);
        getChars(i3, i4, cArrObtain, 0);
        float textRunAdvances = paint.getTextRunAdvances(cArrObtain, i - i3, i7, 0, i6, z, fArr, i5);
        TextUtils.recycle(cArrObtain);
        return textRunAdvances;
    }

    @Override
    @Deprecated
    public int getTextRunCursor(int i, int i2, int i3, int i4, int i5, Paint paint) {
        int i6 = i2 - i;
        if (i2 <= this.mGapStart) {
            return paint.getTextRunCursor(this.mText, i, i6, i3, i4, i5);
        }
        if (i >= this.mGapStart) {
            return paint.getTextRunCursor(this.mText, i + this.mGapLength, i6, i3, i4 + this.mGapLength, i5) - this.mGapLength;
        }
        char[] cArrObtain = TextUtils.obtain(i6);
        getChars(i, i2, cArrObtain, 0);
        int textRunCursor = i + paint.getTextRunCursor(cArrObtain, 0, i6, i3, i4 - i, i5);
        TextUtils.recycle(cArrObtain);
        return textRunCursor;
    }

    @Override
    public void setFilters(InputFilter[] inputFilterArr) {
        if (inputFilterArr == null) {
            throw new IllegalArgumentException();
        }
        this.mFilters = inputFilterArr;
    }

    @Override
    public InputFilter[] getFilters() {
        return this.mFilters;
    }

    public boolean equals(Object obj) {
        if ((obj instanceof Spanned) && toString().equals(obj.toString())) {
            Spanned spanned = (Spanned) obj;
            Object[] spans = spanned.getSpans(0, spanned.length(), Object.class);
            if (this.mSpanCount == spans.length) {
                for (int i = 0; i < this.mSpanCount; i++) {
                    Object obj2 = this.mSpans[i];
                    Object obj3 = spans[i];
                    if (obj2 == this) {
                        if (spanned != obj3 || getSpanStart(obj2) != spanned.getSpanStart(obj3) || getSpanEnd(obj2) != spanned.getSpanEnd(obj3) || getSpanFlags(obj2) != spanned.getSpanFlags(obj3)) {
                            return false;
                        }
                    } else if (!obj2.equals(obj3) || getSpanStart(obj2) != spanned.getSpanStart(obj3) || getSpanEnd(obj2) != spanned.getSpanEnd(obj3) || getSpanFlags(obj2) != spanned.getSpanFlags(obj3)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    public int hashCode() {
        int iHashCode = (toString().hashCode() * 31) + this.mSpanCount;
        for (int i = 0; i < this.mSpanCount; i++) {
            Object obj = this.mSpans[i];
            if (obj != this) {
                iHashCode = (iHashCode * 31) + obj.hashCode();
            }
            iHashCode = (((((iHashCode * 31) + getSpanStart(obj)) * 31) + getSpanEnd(obj)) * 31) + getSpanFlags(obj);
        }
        return iHashCode;
    }

    private int treeRoot() {
        return Integer.highestOneBit(this.mSpanCount) - 1;
    }

    private static int leftChild(int i) {
        return i - (((i + 1) & (~i)) >> 1);
    }

    private static int rightChild(int i) {
        return i + (((i + 1) & (~i)) >> 1);
    }

    private int calcMax(int i) {
        int iMax;
        int i2 = i & 1;
        if (i2 != 0) {
            iMax = calcMax(leftChild(i));
        } else {
            iMax = 0;
        }
        if (i < this.mSpanCount) {
            iMax = Math.max(iMax, this.mSpanEnds[i]);
            if (i2 != 0) {
                iMax = Math.max(iMax, calcMax(rightChild(i)));
            }
        }
        this.mSpanMax[i] = iMax;
        return iMax;
    }

    private void restoreInvariants() {
        if (this.mSpanCount == 0) {
            return;
        }
        for (int i = 1; i < this.mSpanCount; i++) {
            if (this.mSpanStarts[i] < this.mSpanStarts[i - 1]) {
                Object obj = this.mSpans[i];
                int i2 = this.mSpanStarts[i];
                int i3 = this.mSpanEnds[i];
                int i4 = this.mSpanFlags[i];
                int i5 = this.mSpanOrder[i];
                int i6 = i;
                do {
                    int i7 = i6 - 1;
                    this.mSpans[i6] = this.mSpans[i7];
                    this.mSpanStarts[i6] = this.mSpanStarts[i7];
                    this.mSpanEnds[i6] = this.mSpanEnds[i7];
                    this.mSpanFlags[i6] = this.mSpanFlags[i7];
                    this.mSpanOrder[i6] = this.mSpanOrder[i7];
                    i6--;
                    if (i6 <= 0) {
                        break;
                    }
                } while (i2 < this.mSpanStarts[i6 - 1]);
                this.mSpans[i6] = obj;
                this.mSpanStarts[i6] = i2;
                this.mSpanEnds[i6] = i3;
                this.mSpanFlags[i6] = i4;
                this.mSpanOrder[i6] = i5;
                invalidateIndex(i6);
            }
        }
        calcMax(treeRoot());
        if (this.mIndexOfSpan == null) {
            this.mIndexOfSpan = new IdentityHashMap<>();
        }
        for (int i8 = this.mLowWaterMark; i8 < this.mSpanCount; i8++) {
            Integer num = this.mIndexOfSpan.get(this.mSpans[i8]);
            if (num == null || num.intValue() != i8) {
                this.mIndexOfSpan.put(this.mSpans[i8], Integer.valueOf(i8));
            }
        }
        this.mLowWaterMark = Integer.MAX_VALUE;
    }

    private void invalidateIndex(int i) {
        this.mLowWaterMark = Math.min(i, this.mLowWaterMark);
    }
}
