package android.text;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.ProxyInfo;
import android.util.Log;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.GrowingArrayUtils;
import java.lang.reflect.Array;
import libcore.util.EmptyArray;

public class SpannableStringBuilder implements CharSequence, GetChars, Spannable, Editable, Appendable, GraphicsOperations {
    private static final int END_MASK = 15;
    private static final int MARK = 1;
    private static final InputFilter[] NO_FILTERS = new InputFilter[0];
    private static final int PARAGRAPH = 3;
    private static final int POINT = 2;
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
    private int mSpanCount;
    private int mSpanCountBeforeAdd;
    private int[] mSpanEnds;
    private int[] mSpanFlags;
    private int[] mSpanStarts;
    private Object[] mSpans;
    private char[] mText;

    public SpannableStringBuilder() {
        this(ProxyInfo.LOCAL_EXCL_LIST);
    }

    public SpannableStringBuilder(CharSequence text) {
        this(text, 0, text.length());
    }

    public SpannableStringBuilder(CharSequence text, int start, int end) {
        this.mFilters = NO_FILTERS;
        int srclen = end - start;
        if (srclen < 0) {
            throw new StringIndexOutOfBoundsException();
        }
        this.mText = ArrayUtils.newUnpaddedCharArray(GrowingArrayUtils.growSize(srclen));
        this.mGapStart = srclen;
        this.mGapLength = this.mText.length - srclen;
        TextUtils.getChars(text, start, end, this.mText, 0);
        this.mSpanCount = 0;
        this.mSpans = EmptyArray.OBJECT;
        this.mSpanStarts = EmptyArray.INT;
        this.mSpanEnds = EmptyArray.INT;
        this.mSpanFlags = EmptyArray.INT;
        if (text instanceof Spanned) {
            Spanned sp = (Spanned) text;
            Object[] spans = sp.getSpans(start, end, Object.class);
            for (int i = 0; i < spans.length; i++) {
                if (!(spans[i] instanceof NoCopySpan)) {
                    int st = sp.getSpanStart(spans[i]) - start;
                    int en = sp.getSpanEnd(spans[i]) - start;
                    int fl = sp.getSpanFlags(spans[i]);
                    st = st < 0 ? 0 : st;
                    st = st > end - start ? end - start : st;
                    en = en < 0 ? 0 : en;
                    setSpan(false, spans[i], st, en > end - start ? end - start : en, fl);
                }
            }
        }
    }

    public static SpannableStringBuilder valueOf(CharSequence source) {
        return source instanceof SpannableStringBuilder ? (SpannableStringBuilder) source : new SpannableStringBuilder(source);
    }

    @Override
    public char charAt(int where) {
        int len = length();
        if (where < 0) {
            throw new IndexOutOfBoundsException("charAt: " + where + " < 0");
        }
        if (where >= len) {
            throw new IndexOutOfBoundsException("charAt: " + where + " >= length " + len);
        }
        return where >= this.mGapStart ? this.mText[this.mGapLength + where] : this.mText[where];
    }

    @Override
    public int length() {
        return this.mText.length - this.mGapLength;
    }

    private void resizeFor(int size) {
        int oldLength = this.mText.length;
        if (size + 1 > oldLength) {
            char[] newText = ArrayUtils.newUnpaddedCharArray(GrowingArrayUtils.growSize(size));
            System.arraycopy(this.mText, 0, newText, 0, this.mGapStart);
            int newLength = newText.length;
            int delta = newLength - oldLength;
            int after = oldLength - (this.mGapStart + this.mGapLength);
            System.arraycopy(this.mText, oldLength - after, newText, newLength - after, after);
            this.mText = newText;
            this.mGapLength += delta;
            if (this.mGapLength < 1) {
                new Exception("mGapLength < 1").printStackTrace();
            }
            for (int i = 0; i < this.mSpanCount; i++) {
                if (this.mSpanStarts[i] > this.mGapStart) {
                    int[] iArr = this.mSpanStarts;
                    iArr[i] = iArr[i] + delta;
                }
                if (this.mSpanEnds[i] > this.mGapStart) {
                    int[] iArr2 = this.mSpanEnds;
                    iArr2[i] = iArr2[i] + delta;
                }
            }
        }
    }

    private void moveGapTo(int where) {
        int flag;
        int flag2;
        if (where != this.mGapStart) {
            boolean atEnd = where == length();
            if (where < this.mGapStart) {
                int overlap = this.mGapStart - where;
                System.arraycopy(this.mText, where, this.mText, (this.mGapStart + this.mGapLength) - overlap, overlap);
            } else {
                int overlap2 = where - this.mGapStart;
                System.arraycopy(this.mText, (this.mGapLength + where) - overlap2, this.mText, this.mGapStart, overlap2);
            }
            for (int i = 0; i < this.mSpanCount; i++) {
                int start = this.mSpanStarts[i];
                int end = this.mSpanEnds[i];
                if (start > this.mGapStart) {
                    start -= this.mGapLength;
                }
                if (start > where) {
                    start += this.mGapLength;
                } else if (start == where && ((flag = (this.mSpanFlags[i] & 240) >> 4) == 2 || (atEnd && flag == 3))) {
                    start += this.mGapLength;
                }
                if (end > this.mGapStart) {
                    end -= this.mGapLength;
                }
                if (end > where) {
                    end += this.mGapLength;
                } else if (end == where && ((flag2 = this.mSpanFlags[i] & 15) == 2 || (atEnd && flag2 == 3))) {
                    end += this.mGapLength;
                }
                this.mSpanStarts[i] = start;
                this.mSpanEnds[i] = end;
            }
            this.mGapStart = where;
        }
    }

    @Override
    public SpannableStringBuilder insert(int where, CharSequence tb, int start, int end) {
        return replace(where, where, tb, start, end);
    }

    @Override
    public SpannableStringBuilder insert(int where, CharSequence tb) {
        return replace(where, where, tb, 0, tb.length());
    }

    @Override
    public SpannableStringBuilder delete(int start, int end) {
        SpannableStringBuilder ret = replace(start, end, ProxyInfo.LOCAL_EXCL_LIST, 0, 0);
        if (this.mGapLength > length() * 2) {
            resizeFor(length());
        }
        return ret;
    }

    @Override
    public void clear() {
        replace(0, length(), ProxyInfo.LOCAL_EXCL_LIST, 0, 0);
    }

    @Override
    public void clearSpans() {
        for (int i = this.mSpanCount - 1; i >= 0; i--) {
            Object what = this.mSpans[i];
            int ostart = this.mSpanStarts[i];
            int oend = this.mSpanEnds[i];
            if (ostart > this.mGapStart) {
                ostart -= this.mGapLength;
            }
            if (oend > this.mGapStart) {
                oend -= this.mGapLength;
            }
            this.mSpanCount = i;
            this.mSpans[i] = null;
            sendSpanRemoved(what, ostart, oend);
        }
    }

    @Override
    public SpannableStringBuilder append(CharSequence text) {
        int length = length();
        return replace(length, length, text, 0, text.length());
    }

    public SpannableStringBuilder append(CharSequence text, Object what, int flags) {
        int start = length();
        append(text);
        setSpan(what, start, length(), flags);
        return this;
    }

    @Override
    public SpannableStringBuilder append(CharSequence text, int start, int end) {
        int length = length();
        return replace(length, length, text, start, end);
    }

    @Override
    public SpannableStringBuilder append(char text) {
        return append((CharSequence) String.valueOf(text));
    }

    private void change(int start, int end, CharSequence cs, int csStart, int csEnd) {
        int replacedLength = end - start;
        int replacementLength = csEnd - csStart;
        int nbNewChars = replacementLength - replacedLength;
        for (int i = this.mSpanCount - 1; i >= 0; i--) {
            int spanStart = this.mSpanStarts[i];
            if (spanStart > this.mGapStart) {
                spanStart -= this.mGapLength;
            }
            int spanEnd = this.mSpanEnds[i];
            if (spanEnd > this.mGapStart) {
                spanEnd -= this.mGapLength;
            }
            if ((this.mSpanFlags[i] & 51) == 51) {
                int ost = spanStart;
                int oen = spanEnd;
                int clen = length();
                if (spanStart > start && spanStart <= end) {
                    spanStart = end;
                    while (spanStart < clen && (spanStart <= end || charAt(spanStart - 1) != '\n')) {
                        spanStart++;
                    }
                }
                if (spanEnd > start && spanEnd <= end) {
                    spanEnd = end;
                    while (spanEnd < clen && (spanEnd <= end || charAt(spanEnd - 1) != '\n')) {
                        spanEnd++;
                    }
                }
                if (spanStart != ost || spanEnd != oen) {
                    setSpan(false, this.mSpans[i], spanStart, spanEnd, this.mSpanFlags[i]);
                }
            }
            int flags = 0;
            if (spanStart == start) {
                flags = 0 | 4096;
            } else if (spanStart == end + nbNewChars) {
                flags = 0 | 8192;
            }
            if (spanEnd == start) {
                flags |= 16384;
            } else if (spanEnd == end + nbNewChars) {
                flags |= 32768;
            }
            int[] iArr = this.mSpanFlags;
            iArr[i] = iArr[i] | flags;
        }
        moveGapTo(end);
        if (nbNewChars >= this.mGapLength) {
            resizeFor((this.mText.length + nbNewChars) - this.mGapLength);
        }
        boolean textIsRemoved = replacementLength == 0;
        if (replacedLength > 0) {
            int i2 = 0;
            while (i2 < this.mSpanCount) {
                if ((this.mSpanFlags[i2] & 33) == 33 && this.mSpanStarts[i2] >= start && this.mSpanStarts[i2] < this.mGapStart + this.mGapLength && this.mSpanEnds[i2] >= start && this.mSpanEnds[i2] < this.mGapStart + this.mGapLength && (textIsRemoved || this.mSpanStarts[i2] > start || this.mSpanEnds[i2] < this.mGapStart)) {
                    removeSpan(i2);
                } else {
                    i2++;
                }
            }
        }
        this.mGapStart += nbNewChars;
        this.mGapLength -= nbNewChars;
        if (this.mGapLength < 1) {
            new Exception("mGapLength < 1").printStackTrace();
        }
        TextUtils.getChars(cs, csStart, csEnd, this.mText, start);
        if (replacedLength > 0) {
            boolean atEnd = this.mGapStart + this.mGapLength == this.mText.length;
            for (int i3 = 0; i3 < this.mSpanCount; i3++) {
                int startFlag = (this.mSpanFlags[i3] & 240) >> 4;
                this.mSpanStarts[i3] = updatedIntervalBound(this.mSpanStarts[i3], start, nbNewChars, startFlag, atEnd, textIsRemoved);
                int endFlag = this.mSpanFlags[i3] & 15;
                this.mSpanEnds[i3] = updatedIntervalBound(this.mSpanEnds[i3], start, nbNewChars, endFlag, atEnd, textIsRemoved);
            }
        }
        this.mSpanCountBeforeAdd = this.mSpanCount;
        if (cs instanceof Spanned) {
            Spanned sp = (Spanned) cs;
            Object[] spans = sp.getSpans(csStart, csEnd, Object.class);
            for (int i4 = 0; i4 < spans.length; i4++) {
                int st = sp.getSpanStart(spans[i4]);
                int en = sp.getSpanEnd(spans[i4]);
                if (st < csStart) {
                    st = csStart;
                }
                if (en > csEnd) {
                    en = csEnd;
                }
                if (getSpanStart(spans[i4]) < 0) {
                    setSpan(false, spans[i4], (st - csStart) + start, (en - csStart) + start, sp.getSpanFlags(spans[i4]));
                }
            }
        }
    }

    private int updatedIntervalBound(int offset, int start, int nbNewChars, int flag, boolean atEnd, boolean textIsRemoved) {
        if (offset >= start && offset < this.mGapStart + this.mGapLength) {
            if (flag == 2) {
                if (textIsRemoved || offset > start) {
                    return this.mGapStart + this.mGapLength;
                }
            } else if (flag == 3) {
                if (atEnd) {
                    return this.mGapStart + this.mGapLength;
                }
            } else {
                if (!textIsRemoved && offset >= this.mGapStart - nbNewChars) {
                    return this.mGapStart;
                }
                return start;
            }
        }
        return offset;
    }

    private void removeSpan(int i) {
        Object object = this.mSpans[i];
        int start = this.mSpanStarts[i];
        int end = this.mSpanEnds[i];
        if (start > this.mGapStart) {
            start -= this.mGapLength;
        }
        if (end > this.mGapStart) {
            end -= this.mGapLength;
        }
        int count = this.mSpanCount - (i + 1);
        System.arraycopy(this.mSpans, i + 1, this.mSpans, i, count);
        System.arraycopy(this.mSpanStarts, i + 1, this.mSpanStarts, i, count);
        System.arraycopy(this.mSpanEnds, i + 1, this.mSpanEnds, i, count);
        System.arraycopy(this.mSpanFlags, i + 1, this.mSpanFlags, i, count);
        this.mSpanCount--;
        this.mSpans[this.mSpanCount] = null;
        sendSpanRemoved(object, start, end);
    }

    @Override
    public SpannableStringBuilder replace(int start, int end, CharSequence tb) {
        return replace(start, end, tb, 0, tb.length());
    }

    @Override
    public SpannableStringBuilder replace(int start, int end, CharSequence tb, int tbstart, int tbend) {
        checkRange("replace", start, end);
        int filtercount = this.mFilters.length;
        for (int i = 0; i < filtercount; i++) {
            CharSequence repl = this.mFilters[i].filter(tb, tbstart, tbend, this, start, end);
            if (repl != null) {
                tb = repl;
                tbstart = 0;
                tbend = repl.length();
            }
        }
        int origLen = end - start;
        int newLen = tbend - tbstart;
        if (origLen != 0 || newLen != 0 || hasNonExclusiveExclusiveSpanAt(tb, tbstart)) {
            TextWatcher[] textWatchers = (TextWatcher[]) getSpans(start, start + origLen, TextWatcher.class);
            sendBeforeTextChanged(textWatchers, start, origLen, newLen);
            boolean adjustSelection = (origLen == 0 || newLen == 0) ? false : true;
            int selectionStart = 0;
            int selectionEnd = 0;
            if (adjustSelection) {
                selectionStart = Selection.getSelectionStart(this);
                selectionEnd = Selection.getSelectionEnd(this);
            }
            change(start, end, tb, tbstart, tbend);
            if (adjustSelection) {
                if (selectionStart > start && selectionStart < end) {
                    int offset = ((selectionStart - start) * newLen) / origLen;
                    int selectionStart2 = start + offset;
                    setSpan(false, Selection.SELECTION_START, selectionStart2, selectionStart2, 34);
                }
                if (selectionEnd > start && selectionEnd < end) {
                    int offset2 = ((selectionEnd - start) * newLen) / origLen;
                    int selectionEnd2 = start + offset2;
                    setSpan(false, Selection.SELECTION_END, selectionEnd2, selectionEnd2, 34);
                }
            }
            sendTextChanged(textWatchers, start, origLen, newLen);
            sendAfterTextChanged(textWatchers);
            sendToSpanWatchers(start, end, newLen - origLen);
        }
        return this;
    }

    private static boolean hasNonExclusiveExclusiveSpanAt(CharSequence text, int offset) {
        if (text instanceof Spanned) {
            Spanned spanned = (Spanned) text;
            Object[] spans = spanned.getSpans(offset, offset, Object.class);
            for (Object span : spans) {
                int flags = spanned.getSpanFlags(span);
                if (flags != 33) {
                    return true;
                }
            }
        }
        return false;
    }

    private void sendToSpanWatchers(int replaceStart, int replaceEnd, int nbNewChars) {
        for (int i = 0; i < this.mSpanCountBeforeAdd; i++) {
            int spanStart = this.mSpanStarts[i];
            int spanEnd = this.mSpanEnds[i];
            if (spanStart > this.mGapStart) {
                spanStart -= this.mGapLength;
            }
            if (spanEnd > this.mGapStart) {
                spanEnd -= this.mGapLength;
            }
            int spanFlags = this.mSpanFlags[i];
            int newReplaceEnd = replaceEnd + nbNewChars;
            boolean spanChanged = false;
            int previousSpanStart = spanStart;
            if (spanStart > newReplaceEnd) {
                if (nbNewChars != 0) {
                    previousSpanStart -= nbNewChars;
                    spanChanged = true;
                }
            } else if (spanStart >= replaceStart && ((spanStart != replaceStart || (spanFlags & 4096) != 4096) && (spanStart != newReplaceEnd || (spanFlags & 8192) != 8192))) {
                spanChanged = true;
            }
            int previousSpanEnd = spanEnd;
            if (spanEnd > newReplaceEnd) {
                if (nbNewChars != 0) {
                    previousSpanEnd -= nbNewChars;
                    spanChanged = true;
                }
            } else if (spanEnd >= replaceStart && ((spanEnd != replaceStart || (spanFlags & 16384) != 16384) && (spanEnd != newReplaceEnd || (spanFlags & 32768) != 32768))) {
                spanChanged = true;
            }
            if (spanChanged) {
                sendSpanChanged(this.mSpans[i], previousSpanStart, previousSpanEnd, spanStart, spanEnd);
            }
            int[] iArr = this.mSpanFlags;
            iArr[i] = iArr[i] & (-61441);
        }
        for (int i2 = this.mSpanCountBeforeAdd; i2 < this.mSpanCount; i2++) {
            int spanStart2 = this.mSpanStarts[i2];
            int spanEnd2 = this.mSpanEnds[i2];
            if (spanStart2 > this.mGapStart) {
                spanStart2 -= this.mGapLength;
            }
            if (spanEnd2 > this.mGapStart) {
                spanEnd2 -= this.mGapLength;
            }
            sendSpanAdded(this.mSpans[i2], spanStart2, spanEnd2);
        }
    }

    @Override
    public void setSpan(Object what, int start, int end, int flags) {
        setSpan(true, what, start, end, flags);
    }

    private void setSpan(boolean send, Object what, int start, int end, int flags) {
        checkRange("setSpan", start, end);
        int flagsStart = (flags & 240) >> 4;
        if (flagsStart == 3 && start != 0 && start != length()) {
            char c = charAt(start - 1);
            if (c != '\n') {
                throw new RuntimeException("PARAGRAPH span must start at paragraph boundary");
            }
        }
        int flagsEnd = flags & 15;
        if (flagsEnd == 3 && end != 0 && end != length()) {
            char c2 = charAt(end - 1);
            if (c2 != '\n') {
                throw new RuntimeException("PARAGRAPH span must end at paragraph boundary");
            }
        }
        if (flagsStart == 2 && flagsEnd == 1 && start == end) {
            if (send) {
                Log.e(TAG, "SPAN_EXCLUSIVE_EXCLUSIVE spans cannot have a zero length");
                return;
            }
            return;
        }
        if (start > this.mGapStart) {
            start += this.mGapLength;
        } else if (start == this.mGapStart && (flagsStart == 2 || (flagsStart == 3 && start == length()))) {
            start += this.mGapLength;
        }
        if (end > this.mGapStart) {
            end += this.mGapLength;
        } else if (end == this.mGapStart && (flagsEnd == 2 || (flagsEnd == 3 && end == length()))) {
            end += this.mGapLength;
        }
        int count = this.mSpanCount;
        Object[] spans = this.mSpans;
        for (int i = 0; i < count; i++) {
            if (spans[i] == what) {
                int ostart = this.mSpanStarts[i];
                int oend = this.mSpanEnds[i];
                if (ostart > this.mGapStart) {
                    ostart -= this.mGapLength;
                }
                if (oend > this.mGapStart) {
                    oend -= this.mGapLength;
                }
                this.mSpanStarts[i] = start;
                this.mSpanEnds[i] = end;
                this.mSpanFlags[i] = flags;
                if (send) {
                    sendSpanChanged(what, ostart, oend, start, end);
                    return;
                }
                return;
            }
        }
        this.mSpans = GrowingArrayUtils.append(this.mSpans, this.mSpanCount, what);
        this.mSpanStarts = GrowingArrayUtils.append(this.mSpanStarts, this.mSpanCount, start);
        this.mSpanEnds = GrowingArrayUtils.append(this.mSpanEnds, this.mSpanCount, end);
        this.mSpanFlags = GrowingArrayUtils.append(this.mSpanFlags, this.mSpanCount, flags);
        this.mSpanCount++;
        if (send) {
            sendSpanAdded(what, start, end);
        }
    }

    @Override
    public void removeSpan(Object what) {
        for (int i = this.mSpanCount - 1; i >= 0; i--) {
            if (this.mSpans[i] == what) {
                removeSpan(i);
                return;
            }
        }
    }

    @Override
    public int getSpanStart(Object what) {
        int count = this.mSpanCount;
        Object[] spans = this.mSpans;
        for (int i = count - 1; i >= 0; i--) {
            if (spans[i] == what) {
                int where = this.mSpanStarts[i];
                if (where > this.mGapStart) {
                    return where - this.mGapLength;
                }
                return where;
            }
        }
        return -1;
    }

    @Override
    public int getSpanEnd(Object what) {
        int count = this.mSpanCount;
        Object[] spans = this.mSpans;
        for (int i = count - 1; i >= 0; i--) {
            if (spans[i] == what) {
                int where = this.mSpanEnds[i];
                if (where > this.mGapStart) {
                    return where - this.mGapLength;
                }
                return where;
            }
        }
        return -1;
    }

    @Override
    public int getSpanFlags(Object what) {
        int count = this.mSpanCount;
        Object[] spans = this.mSpans;
        for (int i = count - 1; i >= 0; i--) {
            if (spans[i] == what) {
                return this.mSpanFlags[i];
            }
        }
        return 0;
    }

    @Override
    public <T> T[] getSpans(int i, int i2, Class<T> cls) {
        int i3;
        if (cls == null) {
            return (T[]) ArrayUtils.emptyArray(cls);
        }
        int i4 = this.mSpanCount;
        Object[] objArr = this.mSpans;
        int[] iArr = this.mSpanStarts;
        int[] iArr2 = this.mSpanEnds;
        int[] iArr3 = this.mSpanFlags;
        int i5 = this.mGapStart;
        int i6 = this.mGapLength;
        Object[] objArr2 = (T[]) null;
        Object obj = null;
        int i7 = 0;
        int i8 = 0;
        while (i7 < i4) {
            int i9 = iArr[i7];
            if (i9 > i5) {
                i9 -= i6;
            }
            if (i9 > i2) {
                i3 = i8;
            } else {
                int i10 = iArr2[i7];
                if (i10 > i5) {
                    i10 -= i6;
                }
                if (i10 < i) {
                    i3 = i8;
                } else if (i9 != i10 && i != i2) {
                    if (i9 == i2) {
                        i3 = i8;
                    } else if (i10 == i) {
                        i3 = i8;
                    }
                } else if (!cls.isInstance(objArr[i7])) {
                    i3 = i8;
                } else if (i8 == 0) {
                    obj = objArr[i7];
                    i3 = i8 + 1;
                } else {
                    if (i8 == 1) {
                        objArr2 = (T[]) ((Object[]) Array.newInstance((Class<?>) cls, (i4 - i7) + 1));
                        objArr2[0] = obj;
                    }
                    int i11 = iArr3[i7] & Spanned.SPAN_PRIORITY;
                    if (i11 != 0) {
                        int i12 = 0;
                        while (i12 < i8 && i11 <= (getSpanFlags(objArr2[i12]) & Spanned.SPAN_PRIORITY)) {
                            i12++;
                        }
                        System.arraycopy(objArr2, i12, objArr2, i12 + 1, i8 - i12);
                        objArr2[i12] = objArr[i7];
                        i3 = i8 + 1;
                    } else {
                        i3 = i8 + 1;
                        objArr2[i8] = objArr[i7];
                    }
                }
            }
            i7++;
            i8 = i3;
        }
        if (i8 == 0) {
            return (T[]) ArrayUtils.emptyArray(cls);
        }
        if (i8 == 1) {
            T[] tArr = (T[]) ((Object[]) Array.newInstance((Class<?>) cls, 1));
            tArr[0] = obj;
            return tArr;
        }
        if (i8 == objArr2.length) {
            return (T[]) objArr2;
        }
        T[] tArr2 = (T[]) ((Object[]) Array.newInstance((Class<?>) cls, i8));
        System.arraycopy(objArr2, 0, tArr2, 0, i8);
        return tArr2;
    }

    @Override
    public int nextSpanTransition(int start, int limit, Class kind) {
        int count = this.mSpanCount;
        Object[] spans = this.mSpans;
        int[] starts = this.mSpanStarts;
        int[] ends = this.mSpanEnds;
        int gapstart = this.mGapStart;
        int gaplen = this.mGapLength;
        if (kind == null) {
            kind = Object.class;
        }
        for (int i = 0; i < count; i++) {
            int st = starts[i];
            int en = ends[i];
            if (st > gapstart) {
                st -= gaplen;
            }
            if (en > gapstart) {
                en -= gaplen;
            }
            if (st > start && st < limit && kind.isInstance(spans[i])) {
                limit = st;
            }
            if (en > start && en < limit && kind.isInstance(spans[i])) {
                limit = en;
            }
        }
        return limit;
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new SpannableStringBuilder(this, start, end);
    }

    @Override
    public void getChars(int start, int end, char[] dest, int destoff) {
        checkRange("getChars", start, end);
        if (end <= this.mGapStart) {
            System.arraycopy(this.mText, start, dest, destoff, end - start);
        } else if (start >= this.mGapStart) {
            System.arraycopy(this.mText, this.mGapLength + start, dest, destoff, end - start);
        } else {
            System.arraycopy(this.mText, start, dest, destoff, this.mGapStart - start);
            System.arraycopy(this.mText, this.mGapStart + this.mGapLength, dest, (this.mGapStart - start) + destoff, end - this.mGapStart);
        }
    }

    @Override
    public String toString() {
        int len = length();
        char[] buf = new char[len];
        getChars(0, len, buf, 0);
        return new String(buf);
    }

    public String substring(int start, int end) {
        char[] buf = new char[end - start];
        getChars(start, end, buf, 0);
        return new String(buf);
    }

    private void sendBeforeTextChanged(TextWatcher[] watchers, int start, int before, int after) {
        for (TextWatcher textWatcher : watchers) {
            textWatcher.beforeTextChanged(this, start, before, after);
        }
    }

    private void sendTextChanged(TextWatcher[] watchers, int start, int before, int after) {
        for (TextWatcher textWatcher : watchers) {
            textWatcher.onTextChanged(this, start, before, after);
        }
    }

    private void sendAfterTextChanged(TextWatcher[] watchers) {
        for (TextWatcher textWatcher : watchers) {
            textWatcher.afterTextChanged(this);
        }
    }

    private void sendSpanAdded(Object what, int start, int end) {
        SpanWatcher[] recip = (SpanWatcher[]) getSpans(start, end, SpanWatcher.class);
        for (SpanWatcher spanWatcher : recip) {
            spanWatcher.onSpanAdded(this, what, start, end);
        }
    }

    private void sendSpanRemoved(Object what, int start, int end) {
        SpanWatcher[] recip = (SpanWatcher[]) getSpans(start, end, SpanWatcher.class);
        for (SpanWatcher spanWatcher : recip) {
            spanWatcher.onSpanRemoved(this, what, start, end);
        }
    }

    private void sendSpanChanged(Object what, int oldStart, int oldEnd, int start, int end) {
        SpanWatcher[] spanWatchers = (SpanWatcher[]) getSpans(Math.min(oldStart, start), Math.min(Math.max(oldEnd, end), length()), SpanWatcher.class);
        for (SpanWatcher spanWatcher : spanWatchers) {
            spanWatcher.onSpanChanged(this, what, oldStart, oldEnd, start, end);
        }
    }

    private static String region(int start, int end) {
        return "(" + start + " ... " + end + ")";
    }

    private void checkRange(String operation, int start, int end) {
        if (end < start) {
            throw new IndexOutOfBoundsException(operation + " " + region(start, end) + " has end before start");
        }
        int len = length();
        if (start > len || end > len) {
            throw new IndexOutOfBoundsException(operation + " " + region(start, end) + " ends beyond length " + len);
        }
        if (start < 0 || end < 0) {
            throw new IndexOutOfBoundsException(operation + " " + region(start, end) + " starts before 0");
        }
    }

    @Override
    public void drawText(Canvas c, int start, int end, float x, float y, Paint p) {
        checkRange("drawText", start, end);
        if (end <= this.mGapStart) {
            c.drawText(this.mText, start, end - start, x, y, p);
            return;
        }
        if (start >= this.mGapStart) {
            c.drawText(this.mText, start + this.mGapLength, end - start, x, y, p);
            return;
        }
        char[] buf = TextUtils.obtain(end - start);
        getChars(start, end, buf, 0);
        c.drawText(buf, 0, end - start, x, y, p);
        TextUtils.recycle(buf);
    }

    @Override
    public void drawTextRun(Canvas c, int start, int end, int contextStart, int contextEnd, float x, float y, boolean isRtl, Paint p) {
        checkRange("drawTextRun", start, end);
        int contextLen = contextEnd - contextStart;
        int len = end - start;
        if (contextEnd <= this.mGapStart) {
            c.drawTextRun(this.mText, start, len, contextStart, contextLen, x, y, isRtl, p);
            return;
        }
        if (contextStart >= this.mGapStart) {
            c.drawTextRun(this.mText, start + this.mGapLength, len, contextStart + this.mGapLength, contextLen, x, y, isRtl, p);
            return;
        }
        char[] buf = TextUtils.obtain(contextLen);
        getChars(contextStart, contextEnd, buf, 0);
        c.drawTextRun(buf, start - contextStart, len, 0, contextLen, x, y, isRtl, p);
        TextUtils.recycle(buf);
    }

    @Override
    public float measureText(int start, int end, Paint p) {
        checkRange("measureText", start, end);
        if (end <= this.mGapStart) {
            float ret = p.measureText(this.mText, start, end - start);
            return ret;
        }
        if (start >= this.mGapStart) {
            float ret2 = p.measureText(this.mText, this.mGapLength + start, end - start);
            return ret2;
        }
        char[] buf = TextUtils.obtain(end - start);
        getChars(start, end, buf, 0);
        float ret3 = p.measureText(buf, 0, end - start);
        TextUtils.recycle(buf);
        return ret3;
    }

    @Override
    public int getTextWidths(int start, int end, float[] widths, Paint p) {
        checkRange("getTextWidths", start, end);
        if (end <= this.mGapStart) {
            int ret = p.getTextWidths(this.mText, start, end - start, widths);
            return ret;
        }
        if (start >= this.mGapStart) {
            int ret2 = p.getTextWidths(this.mText, this.mGapLength + start, end - start, widths);
            return ret2;
        }
        char[] buf = TextUtils.obtain(end - start);
        getChars(start, end, buf, 0);
        int ret3 = p.getTextWidths(buf, 0, end - start, widths);
        TextUtils.recycle(buf);
        return ret3;
    }

    @Override
    public float getTextRunAdvances(int start, int end, int contextStart, int contextEnd, boolean isRtl, float[] advances, int advancesPos, Paint p) {
        int contextLen = contextEnd - contextStart;
        int len = end - start;
        if (end <= this.mGapStart) {
            float ret = p.getTextRunAdvances(this.mText, start, len, contextStart, contextLen, isRtl, advances, advancesPos);
            return ret;
        }
        if (start >= this.mGapStart) {
            float ret2 = p.getTextRunAdvances(this.mText, start + this.mGapLength, len, contextStart + this.mGapLength, contextLen, isRtl, advances, advancesPos);
            return ret2;
        }
        char[] buf = TextUtils.obtain(contextLen);
        getChars(contextStart, contextEnd, buf, 0);
        float ret3 = p.getTextRunAdvances(buf, start - contextStart, len, 0, contextLen, isRtl, advances, advancesPos);
        TextUtils.recycle(buf);
        return ret3;
    }

    @Override
    @Deprecated
    public int getTextRunCursor(int contextStart, int contextEnd, int dir, int offset, int cursorOpt, Paint p) {
        int contextLen = contextEnd - contextStart;
        if (contextEnd <= this.mGapStart) {
            int ret = p.getTextRunCursor(this.mText, contextStart, contextLen, dir, offset, cursorOpt);
            return ret;
        }
        if (contextStart >= this.mGapStart) {
            int ret2 = p.getTextRunCursor(this.mText, contextStart + this.mGapLength, contextLen, dir, offset + this.mGapLength, cursorOpt) - this.mGapLength;
            return ret2;
        }
        char[] buf = TextUtils.obtain(contextLen);
        getChars(contextStart, contextEnd, buf, 0);
        int ret3 = p.getTextRunCursor(buf, 0, contextLen, dir, offset - contextStart, cursorOpt) + contextStart;
        TextUtils.recycle(buf);
        return ret3;
    }

    @Override
    public void setFilters(InputFilter[] filters) {
        if (filters == null) {
            throw new IllegalArgumentException();
        }
        this.mFilters = filters;
    }

    @Override
    public InputFilter[] getFilters() {
        return this.mFilters;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Spanned) || !toString().equals(o.toString())) {
            return false;
        }
        Spanned other = (Spanned) o;
        Object[] otherSpans = other.getSpans(0, other.length(), Object.class);
        if (this.mSpanCount != otherSpans.length) {
            return false;
        }
        for (int i = 0; i < this.mSpanCount; i++) {
            Object thisSpan = this.mSpans[i];
            Object otherSpan = otherSpans[i];
            if (thisSpan == this) {
                if (other != otherSpan || getSpanStart(thisSpan) != other.getSpanStart(otherSpan) || getSpanEnd(thisSpan) != other.getSpanEnd(otherSpan) || getSpanFlags(thisSpan) != other.getSpanFlags(otherSpan)) {
                    return false;
                }
            } else if (!thisSpan.equals(otherSpan) || getSpanStart(thisSpan) != other.getSpanStart(otherSpan) || getSpanEnd(thisSpan) != other.getSpanEnd(otherSpan) || getSpanFlags(thisSpan) != other.getSpanFlags(otherSpan)) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int hash = toString().hashCode();
        int hash2 = (hash * 31) + this.mSpanCount;
        for (int i = 0; i < this.mSpanCount; i++) {
            Object span = this.mSpans[i];
            if (span != this) {
                hash2 = (hash2 * 31) + span.hashCode();
            }
            hash2 = (((((hash2 * 31) + getSpanStart(span)) * 31) + getSpanEnd(span)) * 31) + getSpanFlags(span);
        }
        return hash2;
    }
}
