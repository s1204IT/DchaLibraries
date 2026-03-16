package android.text.method;

import android.text.Selection;
import android.text.SpannableStringBuilder;
import java.text.BreakIterator;
import java.util.Locale;

public class WordIterator implements Selection.PositionIterator {
    private static final int WINDOW_WIDTH = 50;
    private BreakIterator mIterator;
    private int mOffsetShift;
    private String mString;

    public WordIterator() {
        this(Locale.getDefault());
    }

    public WordIterator(Locale locale) {
        this.mIterator = BreakIterator.getWordInstance(locale);
    }

    public void setCharSequence(CharSequence charSequence, int start, int end) {
        this.mOffsetShift = Math.max(0, start - 50);
        int windowEnd = Math.min(charSequence.length(), end + 50);
        if (charSequence instanceof SpannableStringBuilder) {
            this.mString = ((SpannableStringBuilder) charSequence).substring(this.mOffsetShift, windowEnd);
        } else {
            this.mString = charSequence.subSequence(this.mOffsetShift, windowEnd).toString();
        }
        this.mIterator.setText(this.mString);
    }

    @Override
    public int preceding(int offset) {
        int shiftedOffset = offset - this.mOffsetShift;
        do {
            shiftedOffset = this.mIterator.preceding(shiftedOffset);
            if (shiftedOffset == -1) {
                return -1;
            }
        } while (!isOnLetterOrDigit(shiftedOffset));
        return this.mOffsetShift + shiftedOffset;
    }

    @Override
    public int following(int offset) {
        int shiftedOffset = offset - this.mOffsetShift;
        do {
            shiftedOffset = this.mIterator.following(shiftedOffset);
            if (shiftedOffset == -1) {
                return -1;
            }
        } while (!isAfterLetterOrDigit(shiftedOffset));
        return this.mOffsetShift + shiftedOffset;
    }

    public int getBeginning(int offset) {
        int shiftedOffset = offset - this.mOffsetShift;
        checkOffsetIsValid(shiftedOffset);
        if (isOnLetterOrDigit(shiftedOffset)) {
            if (this.mIterator.isBoundary(shiftedOffset)) {
                return this.mOffsetShift + shiftedOffset;
            }
            return this.mIterator.preceding(shiftedOffset) + this.mOffsetShift;
        }
        if (isAfterLetterOrDigit(shiftedOffset)) {
            return this.mIterator.preceding(shiftedOffset) + this.mOffsetShift;
        }
        return -1;
    }

    public int getEnd(int offset) {
        int shiftedOffset = offset - this.mOffsetShift;
        checkOffsetIsValid(shiftedOffset);
        if (isAfterLetterOrDigit(shiftedOffset)) {
            if (this.mIterator.isBoundary(shiftedOffset)) {
                return this.mOffsetShift + shiftedOffset;
            }
            return this.mIterator.following(shiftedOffset) + this.mOffsetShift;
        }
        if (isOnLetterOrDigit(shiftedOffset)) {
            return this.mIterator.following(shiftedOffset) + this.mOffsetShift;
        }
        return -1;
    }

    private boolean isAfterLetterOrDigit(int shiftedOffset) {
        if (shiftedOffset >= 1 && shiftedOffset <= this.mString.length()) {
            int codePoint = this.mString.codePointBefore(shiftedOffset);
            if (Character.isLetterOrDigit(codePoint)) {
                return true;
            }
        }
        return false;
    }

    private boolean isOnLetterOrDigit(int shiftedOffset) {
        if (shiftedOffset >= 0 && shiftedOffset < this.mString.length()) {
            int codePoint = this.mString.codePointAt(shiftedOffset);
            if (Character.isLetterOrDigit(codePoint)) {
                return true;
            }
        }
        return false;
    }

    private void checkOffsetIsValid(int shiftedOffset) {
        if (shiftedOffset < 0 || shiftedOffset > this.mString.length()) {
            throw new IllegalArgumentException("Invalid offset: " + (this.mOffsetShift + shiftedOffset) + ". Valid range is [" + this.mOffsetShift + ", " + (this.mString.length() + this.mOffsetShift) + "]");
        }
    }
}
